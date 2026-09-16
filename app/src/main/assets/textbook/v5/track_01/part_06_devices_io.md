# PART 06 · 장치는 CPU 옆에 붙은 단순 부품이 아니다 — I/O, interrupt, DMA, driver

키보드를 누르면 문자가 나타나고, SSD에서 파일을 읽으면 byte가 메모리에 들어오고, network packet이 도착하면 socket에서 data를 읽을 수 있다. 사용자에게는 즉시 일어난 한 동작처럼 보이지만 실제로는 **device, controller, bus, interrupt, DMA, kernel driver, buffer, process**가 역할을 나눠 가진다.

이 PART의 목표는 하드웨어 명칭을 외우는 것이 아니라 `I/O가 느리다`, `driver가 멈췄다`, `CPU 사용률은 낮은데 응답이 늦다` 같은 현상을 데이터 이동 경로로 설명하는 것이다.

---

## CHAPTER 01 · CPU와 device 사이에는 제어 경계가 있다

### CPU가 SSD cell을 직접 읽는 것은 아니다

application이 파일을 읽는 경로를 단순화하면:

```text
application
↓ system call
kernel filesystem
↓
block layer / driver
↓
storage controller
↓
device media
```

CPU instruction이 NAND flash cell 주소를 application pointer처럼 직접 읽는 구조가 아니다.

각 계층은 더 높은 계층에 abstraction을 제공한다.

### device controller

장치는 command, status, data transfer를 처리하는 controller/interface를 가진다. CPU/kernel은 특정 register나 queue를 통해 command를 제출하고 완료 상태를 받는다.

정확한 interface는 PCIe/NVMe, USB, display, network 등 device 종류에 따라 완전히 다르다.

그래서 `I/O 장치는 전부 같은 방식`으로 설명하면 안 된다.

---

## CHAPTER 02 · memory-mapped I/O는 RAM과 같은 주소 문법으로 device register를 접근할 수 있게 한다

### 주소처럼 보여도 일반 memory가 아닐 수 있다

일부 architecture/system에서는 device register가 physical address space의 특정 range에 mapping된다.

CPU는 load/store instruction 형태로 접근하지만 target은 DRAM이 아니라 device controller일 수 있다.

```text
CPU store
↓ physical address decode
├─ DRAM range → memory
└─ MMIO range → device register
```

### MMIO access는 일반 memory optimization을 그대로 적용하면 위험하다

compiler/CPU가 ordinary RAM access에 허용하는 reorder/cache behavior가 device register access에 그대로 허용되면 command ordering이 깨질 수 있다.

그래서 architecture와 OS는 device memory attribute, barrier, volatile-like access primitive 등을 사용한다.

application 개발자가 보통 MMIO를 직접 다루지는 않지만 driver code에서 `그냥 pointer write`가 아닌 이유를 이해해야 한다.

---

## CHAPTER 03 · polling은 단순하지만 CPU 시간을 계속 사용할 수 있다

### 완료됐는지 반복 확인

```text
while device_status != DONE:
    check again
```

이 방식은 매우 짧은 대기나 특수 low-latency path에서 유용할 수 있다.

하지만 장치 작업이 milliseconds 걸리는데 CPU가 계속 상태 register를 읽으면 그동안 다른 유용한 일을 할 CPU cycle을 소비한다.

### busy wait와 blocking wait

```text
busy wait
CPU가 계속 조건 확인

blocking wait
thread를 runnable에서 제외
event가 오면 다시 깨움
```

둘의 trade-off는 wait duration, wake latency, power, contention에 따라 다르다.

mobile device에서는 전력 비용도 중요하다.

---

## CHAPTER 04 · interrupt는 device가 CPU attention을 요청하는 방법이다

### CPU가 계속 묻지 않아도 된다

device work가 끝났을 때 interrupt를 발생시키면 CPU/kernel은 완료 사건을 처리할 수 있다.

```text
CPU submits I/O
↓
CPU does other work
↓
device completes
↓ interrupt
kernel handler
↓
waiting task may become runnable
```

### interrupt handler에서는 모든 일을 오래 하지 않는다

interrupt context에서 긴 작업을 하면 다른 latency-sensitive work를 방해할 수 있다.

OS/driver는 긴 처리를 deferred work, worker context, softirq-like mechanism 등으로 나누기도 한다.

정확한 mechanism은 OS마다 다르지만 원리는 같다.

> 빠르게 device event를 acknowledge하고, 오래 걸리는 처리는 적절한 execution context로 미룬다.

### interrupt storm

잘못된 device/driver나 지나치게 많은 event가 interrupt를 폭발적으로 만들면 CPU가 application보다 interrupt 처리에 많은 시간을 쓸 수 있다.

따라서 `CPU 사용률 100%`를 user process만 보고 분석하면 놓칠 수 있다.

---

## CHAPTER 05 · DMA는 큰 data를 CPU load/store loop 없이 옮기는 경로를 만든다

### CPU가 byte 하나씩 복사하면 비싸다

network packet이나 disk block처럼 큰 data를 device와 RAM 사이에서 옮길 때 CPU가 모든 byte를 직접 load/store하면 cycle을 많이 쓴다.

Direct Memory Access 계열 mechanism은 controller가 memory transfer를 수행하도록 한다.

단순 모델:

```text
CPU: descriptor/command 준비
↓
DMA engine/device: data transfer
↓
RAM buffer
↓
interrupt/completion notification
```

### DMA가 CPU를 완전히 배제하는 것은 아니다

CPU는 buffer allocation, descriptor setup, queue management, completion handling을 수행한다.

또 cache coherence/IOMMU/security 문제가 있다.

`DMA = CPU가 아무 일도 안 함`이 아니다.

---

## CHAPTER 06 · IOMMU는 device DMA에도 주소 translation과 isolation을 제공할 수 있다

### device가 아무 physical memory나 쓰게 두면 위험하다

buggy/malicious device가 DMA address를 마음대로 지정할 수 있다면 kernel이나 다른 process memory를 덮어쓸 수 있다.

IOMMU는 device-visible address를 physical memory로 translation하고 allowed mapping을 제한하는 역할을 할 수 있다.

```text
device DMA address
↓ IOMMU translation / permission
physical memory
```

CPU의 MMU가 process isolation을 돕는 것과 비슷한 목적이 있지만 대상과 세부 동작은 다르다.

---

## CHAPTER 07 · device buffer에는 ownership 전환이 있다

### network receive buffer 예

```text
NIC owns descriptor
↓ packet arrives
NIC/DMA writes buffer
↓ completion
kernel owns buffer
↓ protocol stack
socket receive queue
↓ read/copy/map
application owns data view
```

각 단계에서 누가 buffer를 수정할 수 있는지 명확해야 race와 corruption을 막는다.

### zero-copy의 진짜 질문

`zero-copy`라는 마케팅 단어보다 실제 copy count와 ownership transition을 본다.

```text
몇 번 CPU memcpy가 발생하는가?
page를 재사용하는가?
reference만 넘기는가?
checksum/encryption 때문에 다시 touch하는가?
user/kernel boundary copy가 있는가?
```

copy 하나를 줄이는 대신 pinning과 lifetime 관리 비용이 커질 수도 있다.

---

## CHAPTER 08 · file descriptor는 파일만 가리키는 숫자가 아니다

Unix 계열의 file descriptor abstraction에서는 regular file뿐 아니라 pipe, socket, device endpoint 등 여러 I/O object를 비슷한 read/write/poll interface로 다룰 수 있다.

```text
fd 3 → regular file
fd 4 → TCP socket
fd 5 → pipe
```

이 통일된 interface 덕분에 event multiplexing과 shell redirection 같은 기능을 일반화할 수 있다.

### descriptor와 underlying object를 구분한다

`dup`이나 fork-like operation으로 여러 descriptor가 같은 open file description/resource를 가리킬 수 있다.

그래서 descriptor number 하나를 닫았다고 underlying resource가 항상 즉시 사라지는지 여부는 reference 관계에 따라 달라질 수 있다.

---

## CHAPTER 09 · buffering은 syscall 수와 device operation 수를 바꾼다

### 작은 write를 매번 system call

```text
write 1 byte
write 1 byte
write 1 byte
...
```

system call overhead와 storage operation scheduling에 불리할 수 있다.

user-space buffer에 모아 큰 chunk로 보내면 syscall 수를 줄일 수 있다.

```text
append to user buffer
...
flush 64KB
```

### 너무 큰 buffer도 공짜가 아니다

큰 buffer는 memory footprint와 latency를 늘릴 수 있다.

real-time log를 10MB buffer에만 모으면 crash 때 많은 log가 사라질 수 있다.

buffer size는 throughput, latency, durability 사이 trade-off다.

---

## CHAPTER 10 · storage device의 논리 block과 실제 media를 같은 것으로 보지 않는다

### SSD 내부에는 controller mapping이 있다

application이 logical block address에 write해도 flash cell에 1:1 고정 위치로 쓰는 것은 아니다.

SSD controller는 flash translation layer, wear leveling, garbage collection 등을 수행한다.

이 때문에 logical overwrite가 내부적으로 다른 physical page에 write된 뒤 mapping이 갱신되는 식으로 동작할 수 있다.

### write amplification

application이 작은 data를 바꿨는데 SSD 내부에서는 erase block과 garbage collection 때문에 더 많은 data movement가 발생할 수 있다.

DB workload에서 random small write pattern이 storage endurance/performance에 영향을 줄 수 있는 이유다.

### TRIM/discard 개념

filesystem이 더 이상 사용하지 않는 logical block을 device에 알려 주면 SSD controller가 future garbage collection을 더 효율적으로 수행할 수 있다.

정확한 지원과 정책은 OS/filesystem/device 조합에 따라 다르다.

---

## CHAPTER 11 · queue depth가 storage throughput과 latency를 함께 바꾼다

### 여러 request를 동시에 제출

modern storage protocol은 여러 outstanding I/O를 queue에 둘 수 있다.

device controller가 request를 병렬/재정렬해 throughput을 높일 수 있다.

하지만 queue가 너무 길면 개별 request가 뒤에서 오래 기다려 tail latency가 커진다.

```text
queue depth 1
→ 낮은 concurrency

queue depth 64
→ 높은 device utilization 가능
→ 하지만 saturation 시 latency 증가 가능
```

benchmark 결과를 볼 때 queue depth를 반드시 확인해야 한다.

---

## CHAPTER 12 · sequential/random이라는 말도 storage 종류에 따라 의미가 바뀐다

HDD에서는 mechanical seek가 random I/O에 큰 penalty를 준다.

SSD에는 mechanical head가 없지만 random access가 완전히 공짜는 아니다. controller mapping, flash page/block 특성, queueing, write amplification이 있다.

따라서 옛 HDD 경험을 그대로 NVMe SSD에 적용하거나 `SSD니까 random=sequential`이라고 반대로 과장하면 안 된다.

---

## CHAPTER 13 · network NIC도 queue와 interrupt를 가진 I/O device다

### receive path 단순 모델

```text
wire packet
↓
NIC receive queue
↓ DMA
RAM buffer
↓ interrupt/poll
kernel network stack
↓
socket receive buffer
↓
application read
```

network latency를 분석할 때 remote server만 볼 수 없는 이유다.

local host에서도 queueing, packet processing, copy, scheduling이 있다.

### interrupt coalescing

packet마다 interrupt를 발생시키면 high packet rate에서 interrupt overhead가 커질 수 있다.

NIC는 여러 completion을 묶어 interrupt 수를 줄일 수 있다.

trade-off:

```text
more coalescing
→ lower interrupt overhead
→ potentially higher batching latency
```

throughput과 latency 목표에 따라 설정이 달라질 수 있다.

---

## CHAPTER 14 · display와 GPU도 command queue를 가진다

### CPU가 pixel 하나씩 화면에 직접 쓰는 모델은 부족하다

modern UI에서는 CPU가 draw command/data를 만들고 GPU가 rendering pipeline을 수행하는 구조가 일반적이다.

```text
app UI state
↓
layout/draw preparation
↓
render commands
↓
GPU queue
↓
framebuffer/compositor
↓
display
```

### GPU/CPU synchronization

CPU가 GPU가 아직 사용하는 resource를 수정하거나 GPU 결과를 CPU가 너무 일찍 읽으려 하면 synchronization이 필요하다.

강제 sync가 자주 발생하면 CPU와 GPU가 서로 기다려 pipeline이 깨진다.

그래서 graphics performance에서는 CPU time과 GPU time을 분리해 본다.

---

## CHAPTER 15 · frame pacing은 평균 FPS보다 중요할 수 있다

60fps 목표에서 평균이 60이어도 frame time이 다음과 같으면 체감이 나쁘다.

```text
8ms, 9ms, 10ms, 45ms, 8ms, 9ms ...
```

평균만 보면 괜찮아 보이지만 45ms frame에서 끊김이 보인다.

따라서:

```text
median frame time
p90/p95/p99
jank count
missed deadline
```

같은 분포를 본다.

이 원리는 network latency와 DB latency에도 그대로 적용된다.

---

## CHAPTER 16 · driver는 hardware-specific policy와 kernel interface를 연결한다

### application이 device datasheet를 직접 다루지 않는다

kernel driver는 device register/queue/interrupt/DMA 세부를 다루고 상위 subsystem에 표준 interface를 제공한다.

```text
application
↓ generic API
kernel subsystem
↓ driver API
specific driver
↓
hardware
```

### driver bug의 영향이 큰 이유

많은 driver는 높은 privilege에서 동작한다.

memory safety bug나 invalid DMA programming이 process 하나를 넘어서 system crash/corruption으로 이어질 수 있다.

그래서 driver correctness와 isolation은 시스템 안정성 핵심이다.

---

## CHAPTER 17 · synchronous와 asynchronous I/O를 API 모양보다 완료 시점으로 구분한다

### synchronous call

call이 완료돼 return할 때 operation 결과가 준비돼 있는 model을 생각할 수 있다.

하지만 내부 device는 DMA/interrupt를 비동기적으로 사용할 수 있다.

즉 application API가 synchronous라고 hardware가 polling으로 동작한다는 뜻은 아니다.

### asynchronous API

operation을 제출하고 completion을 나중에 받는다.

```text
submit read
↓ immediate return/task suspension
other work
↓ completion event
resume/callback
```

이 model은 thread를 I/O wait에 묶어 두는 비용을 줄일 수 있지만 completion ordering과 cancellation이 복잡해진다.

---

## CHAPTER 18 · timeout은 device failure를 의미하지 않는다

I/O timeout이 나왔다고 SSD/NIC hardware가 반드시 고장난 것은 아니다.

가능한 원인:

```text
queue saturation
scheduler delay
lock contention
driver stall
remote endpoint delay
packet loss/retransmission
filesystem writeback congestion
thermal throttling
actual hardware fault
```

그래서 timeout log 하나를 보고 부품 교체부터 하지 않는다.

end-to-end trace로 어느 queue에서 시간이 쌓였는지 본다.

---

## CHAPTER 19 · backpressure는 I/O pipeline 전체에 필요하다

### producer가 consumer보다 빠른 경우

```text
camera produces 120 frames/s
encoder processes 30 frames/s
```

무한 queue에 계속 쌓으면 memory가 증가하고 latency가 계속 늦어진다.

정책이 필요하다.

```text
drop old frames
drop new frames
block producer
reduce capture rate
scale quality down
```

정답은 제품 요구에 따라 다르다.

video call에서는 오래된 frame을 처리하는 것보다 drop하고 최신 frame을 보내는 편이 낫기도 한다.

file backup에서는 drop이 허용되지 않을 수 있다.

---

## CHAPTER 20 · I/O 진단은 queue를 따라간다

`파일 저장이 가끔 3초 걸린다`를 조사한다고 하자.

잘못된 방식:

```text
SSD가 느린가 보다
```

더 나은 분해:

```text
application serialization time
↓
user buffer wait
↓
system call duration
↓
filesystem lock/journal wait
↓
page cache dirty throttling
↓
block queue latency
↓
device service time
↓
flush completion
```

각 단계의 timestamp/metric을 얻으면 `3초`가 어디에서 생겼는지 좁힐 수 있다.

### CPU 낮음 + latency 높음

이 경우 thread가 I/O/lock/queue를 기다리는지 조사한다.

### CPU 높음 + I/O 낮음

serialization/compression/encryption 같은 CPU work를 본다.

### device utilization 높음 + queue depth 증가

storage saturation 가능성을 본다.

`한 metric`이 아니라 pipeline correlation이 중요하다.

---

## PART 06 종료 점검

1. application이 SSD/NIC register를 직접 다루지 않는 이유는 무엇인가?
2. polling과 interrupt의 CPU 비용 차이는 무엇인가?
3. DMA가 data transfer에서 CPU 역할을 완전히 제거하지 않는 이유는 무엇인가?
4. IOMMU가 DMA security/isolation에 어떻게 기여할 수 있는가?
5. buffer ownership protocol이 필요한 이유는 무엇인가?
6. file descriptor가 regular file만을 뜻하지 않는 이유는 무엇인가?
7. buffering이 throughput과 latency/durability를 동시에 바꾸는 이유는 무엇인가?
8. SSD logical write와 physical flash write가 1:1이 아닐 수 있는 이유는 무엇인가?
9. queue depth가 throughput과 tail latency를 어떻게 바꾸는가?
10. network receive path에서 NIC부터 application까지 어떤 queue가 존재할 수 있는가?
11. GPU rendering에서 CPU time과 GPU time을 왜 분리해야 하는가?
12. timeout이 곧 hardware failure라는 결론이 아닌 이유는 무엇인가?
13. producer-consumer I/O pipeline에서 backpressure가 없으면 무엇이 무너지는가?

이제 I/O를 `CPU가 장치에 명령한다` 한 문장으로 끝내지 않는다. **command, queue, DMA, interrupt, driver, buffer ownership, completion, backpressure**를 연결해 설명할 수 있어야 한다.