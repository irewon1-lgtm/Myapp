# PART 06 · 장치와 I/O — MMIO, DMA, queue, completion

I/O는 CPU가 장치에 명령을 내리고 결과를 받는 **비동기 상태 전이**다. 성능과 correctness는 syscall 횟수보다 buffer ownership, DMA visibility, queue depth, completion ordering, timeout semantics에 의해 결정되는 경우가 많다.

---

## CHAPTER 01 · device boundary에는 bus, register, driver, queue가 있다

application이 storage나 network를 사용할 때 device register를 직접 만지지 않는다. syscall과 kernel subsystem을 거쳐 driver가 device-specific command와 descriptor를 구성한다. PCIe 같은 interconnect, platform bus, firmware interface가 device discovery와 resource mapping에 관여할 수 있다.

장치 문제를 진단할 때 application API, kernel subsystem, driver, transport, physical device를 분리한다. 같은 `I/O error`도 permission, queue timeout, device reset, media failure, link error에서 올 수 있다. kernel log와 device-specific telemetry가 필요한 이유다.

---

## CHAPTER 02 · MMIO register는 일반 RAM과 같은 memory가 아니다

memory-mapped I/O는 device register를 address space에 mapping해 load/store instruction으로 접근하게 한다. 그러나 register access는 side effect를 가질 수 있고 caching/reordering 규칙도 normal memory와 다르다. read가 interrupt status를 clear하거나 write 순서가 command protocol 의미를 가질 수 있다.

compiler optimization과 CPU ordering 때문에 device register access에는 architecture/OS가 제공하는 accessor와 barrier를 사용해야 한다. raw pointer dereference로 동일한 semantics를 얻는다고 가정하면 안 된다. device specification의 read/write width, ordering, reserved bit 규칙을 따른다.

---

## CHAPTER 03 · polling은 latency와 CPU consumption을 교환한다

polling은 device completion을 반복 확인하므로 interrupt delivery latency를 피할 수 있지만 CPU cycle을 소비한다. very-low-latency workload에서는 busy polling이 합리적일 수 있고, 대부분의 general-purpose workload에서는 sleep/interrupt/event completion이 효율적이다.

adaptive polling은 처음 짧게 spin하고 완료되지 않으면 sleep하는 식으로 두 비용을 절충할 수 있다. 선택은 expected service time, core budget, power constraint, tail latency 목표에 근거한다.

polling loop의 비용은 iteration 수만 재면 불완전하다. 전용 core를 고정했는지, SMT sibling에 어떤 workload가 있는지, status register 또는 shared completion memory가 어느 cache domain에 있는지에 따라 같은 poll rate라도 시스템 비용이 달라진다. 진단에서는 poll duration, successful-poll 비율, empty-poll cycle, CPU residency, wakeup latency를 같이 기록한다. 장치 서비스 시간이 길어졌는데 spin budget을 그대로 유지하면 CPU를 태우면서도 tail latency는 개선되지 않을 수 있으므로, adaptive threshold가 실제 service-time distribution을 따라가는지 확인해야 한다.

---

## CHAPTER 04 · interrupt는 장치 완료를 알리지만 처리량이 높으면 자체 병목이 된다

장치는 completion/error event를 interrupt로 CPU에 알릴 수 있다. 높은 packet/I/O rate에서 event 하나마다 interrupt가 발생하면 interrupt handling과 context disruption 비용이 커진다. interrupt coalescing/moderation은 여러 completion을 묶어 처리량을 높이는 대신 개별 event latency를 늘릴 수 있다.

modern network stack은 interrupt 후 deferred/budgeted polling을 조합하기도 한다. 따라서 `interrupt 수가 적다=좋다`도 아니다. event rate, batch size, CPU locality, queue latency를 같이 본다.

interrupt affinity가 queue owner나 application consumer와 어긋나면 completion 처리 뒤 데이터가 다시 다른 CPU로 이동하면서 cache locality와 scheduler 비용이 악화될 수 있다. 반대로 한 CPU에 너무 많은 vector를 몰면 hardirq/deferred work가 그 core의 user task를 밀어낸다. `/proc/interrupts` 계열의 per-CPU 분포, softirq 시간, queue별 packet/request 수, coalescing 설정을 동일 시간축에서 비교하면 “장치가 느린지”와 “완료 전달 경로가 포화됐는지”를 나눌 수 있다. moderation 값을 조정할 때는 throughput뿐 아니라 p99 completion latency와 CPU steal-away 시간을 함께 검증한다.

---

## CHAPTER 05 · DMA는 data copy를 없애는 것이 아니라 ownership을 장치와 공유한다

Direct Memory Access를 사용하면 device가 CPU instruction마다 byte를 옮기지 않고 memory buffer와 직접 data transfer를 수행할 수 있다. CPU는 descriptor를 준비하고 device에 주소/길이를 알려 준 뒤 completion을 기다린다.

문제는 CPU와 device가 같은 buffer를 언제 읽고 써도 되는지다. descriptor publish 전에 content가 visible해야 하고 completion 전에 CPU가 buffer를 재사용하면 corruption이 생긴다. architecture/platform에 따라 cache coherency와 DMA mapping API가 이 visibility를 관리한다.

DMA API에는 방향과 lifetime이 의미를 가진다. device가 읽는 buffer와 device가 쓰는 buffer는 필요한 synchronization이 다를 수 있고, non-coherent platform에서는 map/unmap 또는 sync operation이 cache maintenance와 결합된다. scatter-gather list를 만들었다면 descriptor가 참조하는 segment가 모두 completion까지 살아 있어야 한다. 장애 재현에서는 request ID, DMA address, mapping generation, CPU ownership 전환, device completion을 함께 남겨 stale mapping이나 early reuse를 식별한다. 단순히 memcpy가 없다는 이유만으로 zero-copy가 안전하거나 빠르다고 결론내리지 않는다.

---

## CHAPTER 06 · IOMMU는 device DMA address space에도 translation과 isolation을 제공한다

IOMMU는 device가 사용하는 I/O virtual address를 physical memory로 translate하고 접근 범위를 제한할 수 있다. 잘못되거나 공격받은 device가 임의의 system RAM에 DMA하는 위험을 줄이고 scatter-gather buffer를 연속된 I/O address처럼 보이게 할 수 있다.

DMA mapping lifetime은 CPU virtual mapping과 다르다. buffer를 free하기 전에 device operation이 끝났는지, IOMMU mapping을 해제했는지 확인해야 한다. use-after-free는 CPU thread 사이에서만 생기는 문제가 아니다.

IOMMU fault는 단순 “장치 오류”가 아니라 device identity와 translation state를 연결하는 증거다. faulting requester ID/PASID, IOVA, access type, domain attachment, mapping generation을 기록하면 잘못된 descriptor와 잘못된 isolation configuration을 구분할 수 있다. teardown 시에는 새 submission 차단→in-flight drain/reset→DMA unmap→memory free 순서를 보장해야 한다. 순서를 뒤집으면 장치가 이미 다른 용도로 재사용된 physical page에 late DMA를 수행할 수 있다. hot-unplug·reset 같은 비정상 경로에서도 동일 lifetime 규칙이 유지되는지 별도로 검증한다.

---

## CHAPTER 07 · ring buffer에서 ownership bit가 correctness를 만든다

NIC, storage controller는 descriptor ring을 통해 producer/consumer state를 공유할 수 있다. CPU가 descriptor를 채우고 ownership을 device에 넘기면 device가 DMA 후 completion state를 갱신한다.

ring correctness는 index arithmetic보다 **slot ownership transition**으로 본다.

```text
FREE → CPU_PREPARED → DEVICE_OWNED → COMPLETED → FREE
```

state 전이가 atomic하지 않거나 wrap-around 계산이 잘못되면 descriptor overwrite, duplicate completion, stale DMA가 발생한다. queue full/empty 판정과 generation counter를 명확히 한다.

producer index가 먼저 보이고 descriptor payload가 늦게 보이면 device는 반쯤 준비된 command를 읽을 수 있으므로 doorbell/ownership publish 전 memory ordering이 필요하다. completion 쪽에서도 status를 관찰한 뒤 DMA-written payload를 읽는 순서가 맞아야 한다. wrap-around가 일어난 장시간 테스트에서는 index 값만으로 old/new slot을 구분하기 어려워 generation bit나 monotonic counter가 유용하다. 검증 시 submit sequence, ring slot, producer/consumer index, ownership generation, completion code를 로그에 남기면 duplicate·lost·stale completion을 같은 ledger에서 대조할 수 있다.

---

## CHAPTER 08 · file descriptor는 path가 아니라 open resource reference다

fd는 process-local handle이고 kernel의 open state를 참조한다. path lookup은 open 시점에 namespace object를 찾는 과정이고, 이후 read/write는 열린 object reference를 사용한다. rename/unlink 후에도 open fd가 계속 접근 가능한 경우가 생기는 이유다.

`dup`, `fork`, descriptor passing으로 여러 handle이 underlying open state를 공유할 수 있다. close lifetime을 단순 변수 scope와 일치시키지 않으면 descriptor leak과 unexpected shared offset이 생긴다. close-on-exec도 process spawn boundary에서 필수 검토 항목이다.

동일한 정수 fd 번호는 close 뒤 다른 open에서 재사용될 수 있으므로 비동기 callback에 숫자만 저장하면 ABA 형태의 lifetime 버그가 생길 수 있다. 오래된 completion이 도착했을 때 같은 번호가 새 socket/file을 가리키면 잘못된 resource에 작업을 적용할 수 있다. ownership wrapper, generation token, request-local handle을 사용해 identity를 분리하고, `/proc/.../fd` snapshot이나 open/close trace로 leak과 reuse를 확인한다. 특히 multi-thread close와 I/O가 겹치는 API는 OS별 semantics를 확인해 “close했으니 즉시 모든 operation이 취소됐다”는 가정을 피해야 한다.

---

## CHAPTER 09 · buffering은 user, kernel, device 각 계층에 존재한다

language stream buffer, libc buffer, socket send buffer, page cache, device queue/cache는 서로 다른 목적을 가진다. 한 계층의 flush가 다음 모든 계층의 durability/completion을 의미하지 않는다.

성능 문제에서는 batching 이득과 latency 비용을 함께 본다. 작은 write를 모으면 syscall/packet overhead가 줄 수 있지만 interactive latency가 증가한다. buffer가 가득 찼을 때 block할지 drop할지 reject할지 역시 backpressure 정책이다.

각 buffer에는 capacity, admission rule, drain trigger가 따로 있다. user-space queue가 비어 있어도 kernel socket buffer가 포화될 수 있고, page cache write가 빨리 반환돼도 device queue에는 오래 남아 있을 수 있다. 그래서 queue length를 한 지점에서만 보면 병목이 다음 계층으로 이동한 것을 놓친다. 진단에서는 application pending bytes, socket/page-cache 상태, block/NIC queue depth, drain rate를 동시에 관찰한다. flush 주기를 줄였을 때 syscall 수와 write amplification이 증가하는지, 늘렸을 때 latency와 memory footprint가 증가하는지를 함께 측정해야 batching 정책의 실제 trade-off를 판단할 수 있다.

---

## CHAPTER 10 · storage path는 filesystem request에서 flash translation까지 이어진다

file write는 filesystem/page cache에서 block request로 변환되고 storage controller queue를 거쳐 device에 도달한다. SSD 내부에서는 logical block address가 NAND physical location과 직접 같지 않으며 FTL이 wear leveling, garbage collection, mapping을 수행한다.

따라서 SSD latency spike는 application request size만으로 설명되지 않는다. write amplification, internal garbage collection, queue saturation, thermal condition, firmware behavior가 tail latency를 만들 수 있다. storage benchmark는 sustained state와 fresh-drive state를 구분한다.

---

## CHAPTER 11 · queue depth는 throughput을 올리다가 saturation 이후 latency를 폭발시킨다

parallel device는 여러 outstanding request를 받아 내부적으로 병렬 처리할 수 있다. queue depth가 너무 낮으면 device parallelism을 활용하지 못하고, 너무 높으면 throughput 증가 없이 wait time만 커진다.

Little's Law 관점에서 in-flight request 수는 arrival rate와 residence time의 곱과 연결된다. 목표는 최대 queue depth가 아니라 workload의 latency SLO 안에서 device를 충분히 활용하는 operating point를 찾는 것이다. average보다 p95/p99를 본다.

queue depth를 실험할 때 host submission queue의 길이와 device 내부 병렬성을 혼동하지 않는다. 여러 software queue가 하나의 hardware resource에 합쳐지거나, priority class별로 dispatch rule이 달라질 수 있다. depth를 단계적으로 늘리면서 throughput, queue wait, service time, timeout/retry, CPU submission cost를 동시에 기록하면 saturation knee를 찾을 수 있다. p99가 급증했는데 throughput이 거의 늘지 않는 구간은 추가 in-flight가 병렬성보다 대기만 늘리는 영역이다. admission control은 이 knee 이전에서 workload별 budget을 유지하도록 설계하는 편이 안정적이다.

---

## CHAPTER 12 · sequential/random이라는 분류보다 request locality와 merge 가능성이 중요하다

rotational disk에서는 seek cost 때문에 sequential access 이점이 매우 컸다. SSD에서도 sequential request는 controller/FTL과 host stack에서 merge·prefetch·large transfer 이점을 가질 수 있지만 device 특성에 따라 차이가 달라진다.

random access는 queue parallelism으로 일부 숨길 수 있고, 매우 작은 sequential request는 syscall/metadata overhead 때문에 비효율적일 수 있다. block size, alignment, concurrency, read-ahead, cache hit ratio를 함께 측정한다.

관측 단위를 file-level pattern에만 두면 실제 block layout을 놓칠 수 있다. 논리적으로 연속된 file offset도 fragmentation이나 copy-on-write allocation 때문에 여러 extent로 흩어질 수 있고, 여러 thread의 순차 stream이 block layer에서 interleave되면 device에는 random-like workload로 보일 수 있다. 반대로 adjacent request는 merge되어 더 큰 transfer가 될 수 있다. trace에서 offset, size, issue order, merge 여부와 completion latency를 연결하고 cache를 cold/warm으로 분리하면 “sequential이라 빨라야 한다”는 추정을 실제 device-visible access pattern으로 바꿀 수 있다.

---

## CHAPTER 13 · NIC path는 packet copy보다 queue와 CPU distribution 문제를 포함한다

receive packet은 NIC RX ring, DMA buffer, driver/network stack을 거쳐 socket receive queue에 도달한다. transmit은 반대 방향으로 descriptor와 completion을 사용한다. checksum/segmentation offload는 일부 protocol work를 NIC에 넘길 수 있다.

multi-queue NIC는 traffic을 여러 CPU queue에 분산할 수 있지만 affinity가 잘못되면 packet processing과 application thread 사이 cache locality가 나빠질 수 있다. packet loss를 network 자체 문제로 단정하지 않고 NIC ring drop, kernel backlog, socket queue overflow를 구분한다.

---

## CHAPTER 14 · GPU 작업은 command submission과 fence synchronization으로 본다

GPU는 CPU와 별도 execution pipeline과 memory system을 가진다. CPU가 draw/compute command를 제출한 시점과 GPU가 실제 완료한 시점은 다르다. shared buffer를 재사용하려면 fence/semaphore 같은 synchronization이 필요하다.

CPU가 GPU completion을 동기적으로 기다리면 pipeline parallelism이 사라지고, GPU가 CPU-produced resource를 기다리면 반대 방향 stall이 생긴다. frame performance는 CPU time과 GPU time을 분리해 측정하고 queue depth와 synchronization point를 trace한다.

resource lifetime은 frame 번호보다 fence value와 연결하는 편이 안전하다. command buffer에 참조된 texture/buffer를 CPU가 다음 frame 준비 때문에 덮어쓰기 전에 해당 GPU work가 끝났는지 확인해야 하며, staging/upload memory도 같은 규칙을 따른다. 과도한 wait-idle은 correctness는 지키지만 CPU/GPU overlap을 제거하므로 per-resource 또는 timeline synchronization으로 필요한 dependency만 표현한다. GPU capture나 frame trace에서는 submit ID, queue, wait/signal fence, resource generation, GPU start/end timestamp를 연결해 CPU-bound와 GPU-bound, synchronization bubble을 구분한다.

---

## CHAPTER 15 · frame pacing은 평균 FPS보다 deadline scheduling 문제다

60Hz display에서 frame budget은 대략 refresh interval 안에 CPU 준비와 GPU render가 완료되어야 한다. 평균 rendering time이 budget 이하여도 특정 frame이 deadline을 넘으면 jank가 보인다.

frame queue가 너무 길면 throughput은 유지되어도 input-to-display latency가 늘어난다. UI 성능에서는 average FPS 대신 frame-time distribution, missed deadline, main/render/GPU stage를 분리한다. Android trace에서 frame timeline과 CPU scheduling을 함께 본다.

---

## CHAPTER 16 · driver는 kernel privilege에서 untrusted device/input을 처리한다

driver는 높은 privilege에서 device state와 memory mapping을 다루므로 length, descriptor, firmware response를 엄격하게 검증해야 한다. user input이 ioctl 같은 interface를 통해 driver까지 내려오면 validation bug가 kernel memory corruption으로 이어질 수 있다.

reset/error recovery path는 정상 path만큼 중요하다. device timeout 후 outstanding DMA가 정말 중단되었는지 확인하지 않고 buffer를 free하면 late completion이 freed memory를 건드릴 수 있다. reset generation을 두어 stale completion을 거부하는 설계가 필요할 수 있다.

---

## CHAPTER 17 · async I/O는 submission과 completion 사이 state machine을 만든다

asynchronous API는 caller가 operation을 submit한 뒤 다른 일을 수행하고 completion event에서 결과를 받게 한다. 이 구조에서는 request object lifetime, buffer lifetime, cancellation, partial completion을 명시해야 한다.

completion order가 submission order와 같다고 가정해서는 안 된다. 여러 request가 독립적으로 진행되면 result를 request ID에 연결해야 하고, stateful protocol이면 required ordering을 별도로 enforce한다. callback/future가 편리해도 underlying I/O semantics는 사라지지 않는다.

---

## CHAPTER 18 · timeout은 operation 결과를 모른다는 상태를 만들 수 있다

client timeout이 발생한 순간 remote/device operation이 실행되지 않았다는 뜻은 아니다. request가 이미 queue를 떠났거나 device가 completion 직전일 수 있다. 따라서 timeout 후 즉시 같은 non-idempotent operation을 retry하면 duplicate side effect가 생길 수 있다.

cancel API가 successful return을 해도 hardware/remote peer까지 cancellation이 전달되었는지 semantics를 확인한다. ambiguous completion을 다루려면 idempotency key, status query, generation/request ID가 필요하다.

---

## CHAPTER 19 · backpressure는 producer가 downstream capacity를 초과하지 못하게 한다

producer rate가 device/network/consumer service rate보다 크면 queue가 계속 증가한다. unbounded queue는 overload를 즉시 실패 대신 memory growth와 tail latency로 바꾼다.

bounded buffer는 full일 때 block, reject, drop, shed, coalesce 중 하나의 정책이 필요하다. telemetry/logging처럼 newest/oldest drop이 허용되는 데이터와 transaction처럼 loss가 허용되지 않는 데이터는 정책이 다르다. backpressure는 성능 옵션이 아니라 overload correctness다.

---

## CHAPTER 20 · I/O 진단은 request lifetime을 end-to-end로 추적한다

한 request에 correlation ID를 부여하고 다음 timestamp를 연결한다.

```text
application enqueue
syscall/submission
kernel queue enter
hardware dispatch
hardware completion
kernel completion
application callback
```

latency가 어느 구간에서 늘어나는지 확인하면 application queue, scheduler, driver, device를 분리할 수 있다. throughput, queue depth, error/reset count, CPU interrupt/softirq load를 함께 기록한다. I/O 최적화는 buffer 크기를 임의 조정하는 작업이 아니라 **병목 queue와 ownership transition을 증거로 찾는 작업**이다.