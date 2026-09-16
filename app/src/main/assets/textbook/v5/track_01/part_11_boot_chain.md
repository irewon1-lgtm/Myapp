# PART 11 · 전원 버튼에서 앱 첫 화면까지 — boot chain을 끝까지 추적한다

앱이 실행되는 세계는 이미 수많은 단계가 성공한 뒤다. 전원이 켜진 직후 CPU가 어디서 instruction을 가져오는지, 누가 DRAM을 사용할 수 있게 만드는지, 누가 kernel image를 검증하고 로드하는지, kernel이 어떻게 첫 user-space process를 시작하는지, Android가 어떻게 Zygote와 system service를 올리는지 이해하면 `부팅이 안 된다`, `업데이트 후 bootloop`, `앱 첫 실행만 유난히 느리다` 같은 문제를 층별로 나눌 수 있다.

이 PART의 목표는 특정 제조사 bootloader 명령을 암기하는 것이 아니다. **신뢰·제어권·memory·실행 주체가 한 단계씩 다음 단계로 넘어가는 chain**을 이해하는 것이다.

---

## CHAPTER 01 · reset 뒤 CPU도 아무 코드나 자동으로 아는 것은 아니다

processor는 reset 뒤 architecture가 정한 초기 상태와 entry location에서 instruction fetch를 시작한다.

그 위치에는 platform firmware/boot ROM이 실행할 code가 준비돼 있어야 한다.

```text
power/reset
↓
architectural reset state
↓
boot ROM / firmware entry
↓
platform initialization
```

### reset vector

CPU가 reset 직후 처음 instruction을 가져오는 위치/규칙을 일반적으로 reset vector라는 개념으로 설명한다.

구체 주소와 mode는 architecture/platform마다 다르다.

중요한 것은 `OS가 맨 처음 실행된다`가 아니라 **OS보다 앞선 trusted code가 존재한다**는 점이다.

---

## CHAPTER 02 · firmware는 운영체제보다 먼저 hardware를 usable state로 만든다

초기 firmware는 board와 SoC에 따라 다음 종류의 일을 할 수 있다.

```text
clock/power domain setup
DRAM controller initialization
basic security state
boot device selection
hardware description preparation
next-stage loader verification/loading
```

DRAM controller가 준비되기 전에는 우리가 평소 생각하는 큰 main memory를 자유롭게 쓸 수 없다.

따라서 early boot code는 제한된 on-chip memory를 사용하다가 DRAM initialization 뒤 더 넓은 memory로 이동할 수 있다.

---

## CHAPTER 03 · bootloader는 kernel file을 그냥 찾아 실행하는 프로그램이 아니다

Android AOSP의 현재 bootloader 문서는 bootloader가 memory를 초기화하고, device state를 보호하며, Verified Boot 흐름에 따라 boot 관련 partition을 검증하고, kernel과 ramdisk를 memory에 준비한 뒤 control을 kernel로 넘기는 역할을 설명한다.

개념:

```text
firmware / early stages
↓
bootloader
├─ select boot slot/mode
├─ verify boot material
├─ load kernel
├─ load ramdisk/bootconfig
└─ transfer control
↓
kernel
```

기기별로 여러 loader stage가 존재할 수 있다.

---

## CHAPTER 04 · chain of trust는 다음 code를 실행하기 전에 검증한다

secure/verified boot의 핵심은 각 단계가 다음 단계의 integrity/authenticity를 확인하는 chain이다.

```text
hardware-protected root of trust
↓ verifies
bootloader stage
↓ verifies
boot image / metadata
↓ verifies
verified partitions
```

모든 byte를 항상 한 번에 hash한다는 뜻은 아니다. 큰 filesystem partition은 hash tree/dm-verity 같은 mechanism으로 runtime verification이 연결될 수 있다.

### 왜 시작점이 hardware-protected여야 하는가

검증을 수행하는 첫 verifier 자체를 attacker가 쉽게 바꿀 수 있으면 그 verifier가 악성 image를 `정상`이라고 승인할 수 있다.

그래서 trust chain에는 바꾸기 어려운 root가 필요하다.

---

## CHAPTER 05 · signature와 hash의 역할을 구분한다

hash만 있으면 `내용이 바뀌었는지` 비교할 수 있지만 attacker가 image와 hash를 함께 교체할 수 있다면 trusted origin을 보장하지 못한다.

digital signature는 trusted public key에 대응하는 private key로 승인된 metadata/image인지 검증하는 데 사용된다.

단순 모델:

```text
image
↓ hash
expected digest / signed metadata
↓ signature verification with trusted key
accept or reject
```

실제 AVB metadata/vbmeta 구조는 더 복잡하며 공식 format을 따른다.

---

## CHAPTER 06 · rollback protection은 `유효하게 서명된 옛 취약 버전`도 막아야 한다

공격자는 서명이 올바른 과거 firmware가 가진 알려진 취약점을 이용하려 할 수 있다.

따라서 signature validity만 확인하면 충분하지 않을 수 있다.

rollback index/version metadata를 tamper-resistant state와 비교해 허용된 최소 version보다 낮은 image를 거부하는 mechanism이 필요하다.

```text
image signature valid = yes
but version < rollback floor
→ reject
```

보안 update가 install된 뒤 다시 vulnerable version으로 내려가는 공격을 막는 목적이다.

---

## CHAPTER 07 · A/B slot은 update 실패에서 부팅 가능한 경로를 남긴다

A/B update를 사용하는 device는 system partition set을 두 slot으로 관리할 수 있다.

개념:

```text
currently booted: slot A
update writes:    slot B
↓ reboot
try slot B
↓ success mark
future boots B
```

새 slot이 boot 성공 조건을 만족하지 못하면 fallback policy로 이전 slot을 선택할 수 있다.

### update 완료와 boot 성공은 다른 사건이다

bytes를 B slot에 썼다고 update가 성공한 것이 아니다.

새 system이 실제로 boot되고 health condition을 통과해 successful로 표시되는 단계가 필요하다.

---

## CHAPTER 08 · boot image에는 kernel만 있는 것이 아니다

Android release/device 구조에 따라 `boot.img`, `vendor_boot.img`, `init_boot.img` 등 여러 boot image가 관여할 수 있다.

AOSP bootloader 문서는 kernel과 ramdisk, bootconfig를 준비하는 흐름을 설명한다.

### ramdisk/initramfs

kernel이 root filesystem의 최종 형태를 바로 사용할 수 없는 early boot에서 초기 userspace 파일들을 memory-backed filesystem으로 제공할 수 있다.

초기 `init`와 mount 준비에 필요하다.

---

## CHAPTER 09 · kernel command line과 bootconfig는 bootloader에서 kernel/userspace로 정보를 전달한다

boot option에는 hardware/platform state와 boot reason 같은 값이 포함될 수 있다.

Android 12 이상에서는 `androidboot.*` 중 userspace에 전달할 정보를 bootconfig로 전달하는 구조가 사용된다.

이것은 단순 text 설정이 아니라 **boot stage 사이 contract**다.

잘못된 parameter는 driver probe, mount, security policy, Android property 결과에 영향을 줄 수 있다.

---

## CHAPTER 10 · kernel entry 뒤에는 memory·interrupt·scheduler·driver initialization이 이어진다

kernel은 CPU mode와 memory management를 초기화하고 subsystem을 준비한다.

일반적인 개념:

```text
architecture setup
memory allocator/page tables
interrupt/timer
scheduler primitives
core subsystems
built-in drivers
filesystem/root preparation
userspace entry
```

실제 순서는 kernel version/architecture/config에 따라 복잡하다.

### early boot log

정상 userspace logger가 뜨기 전 failure는 kernel console/serial/pstore 같은 더 이른 evidence가 필요할 수 있다.

`logcat이 없다`고 evidence가 없는 것이 아니다.

---

## CHAPTER 11 · device tree는 hardware description을 kernel에 전달할 수 있다

embedded/ARM platform에서는 device tree가 hardware topology와 register/interrupt/resource 정보를 기술하는 데 널리 쓰인다.

```text
CPU
memory range
bus
UART
I2C device
interrupt controller
reserved memory
```

kernel driver는 compatible property와 resource 정보를 사용해 해당 hardware를 probe할 수 있다.

### DTB/DTBO

device tree source는 binary blob으로 compile될 수 있고 overlay로 board variant 차이를 적용할 수 있다.

Android bootloader가 `dtbo` partition 등을 검증하는 이유와 연결된다.

---

## CHAPTER 12 · driver probe 실패는 hardware가 물리적으로 고장났다는 뜻이 아니다

가능한 원인:

```text
wrong device tree resource
clock/regulator not enabled
dependency driver not ready
firmware file missing
permission/security policy
incompatible driver/device revision
actual hardware fault
```

boot log에서 probe error와 dependency ordering을 본다.

### deferred probe

어떤 driver는 필요한 supplier가 아직 준비되지 않았으면 나중에 다시 probe할 수 있다.

`첫 probe 실패 log`만 보고 최종 failure라고 판단하지 않는다.

---

## CHAPTER 13 · kernel은 첫 user-space process로 control의 범위를 넓힌다

Linux 계열 boot에서는 kernel이 초기화 뒤 PID 1 역할의 초기 userspace process를 시작한다.

Android에서는 `init`이 system bring-up의 핵심 역할을 한다.

```text
kernel
↓
first userspace / init
↓
mount/property/service setup
↓
Android native daemons/framework services
```

PID 1은 ordinary process와 shutdown/orphan handling 등에서 특별한 역할을 가질 수 있다.

---

## CHAPTER 14 · Android init은 service dependency와 property event를 이용한다

Android init language/configuration은 boot phase에 따라 command와 service를 시작한다.

개념:

```text
on early-init
on init
on late-init
property trigger
service definitions
```

정확한 event 이름과 built-in command는 release의 AOSP 문서를 확인한다.

### 서비스 하나가 늦으면 boot 전체가 늦어질 수 있다

critical dependency가 blocking operation을 하면 이후 framework readiness가 밀릴 수 있다.

boot performance도 end-to-end trace로 분석해야 한다.

---

## CHAPTER 15 · SELinux policy는 boot 중 enforcement 경계를 만든다

Android는 SELinux MAC을 이용해 process/domain 사이 access를 제한한다.

boot 때 policy가 load되고 process가 security domain에 배치된다.

### denial을 권한 오류와 구분한다

service가 file/socket에 접근하지 못할 때 Unix mode bit가 맞아도 SELinux policy가 거부할 수 있다.

`setenforce 0`로 문제를 숨기는 대신 denial log와 domain/type rule을 분석한다.

---

## CHAPTER 16 · system_server는 Android framework의 많은 system service를 호스팅한다

Activity/Package/Window 등 framework 기능의 많은 부분은 privileged system process/service와 연결된다.

app process는 Binder를 통해 이 service에 요청한다.

따라서 앱 한 줄:

```text
systemService.doSomething()
```

뒤에 process boundary, Binder scheduling, permission check, system state가 있을 수 있다.

---

## CHAPTER 17 · Zygote가 준비된 뒤 app process를 빠르게 만든다

boot 중 ART/runtime common state를 가진 Zygote가 올라온다.

새 app launch 요청:

```text
system request
↓
Zygote/USAP process creation path
↓
UID/GID/security specialization
↓
app runtime entry
```

common page를 copy-on-write로 공유할 수 있어 memory/startup 이점이 생긴다.

PART 03, 08의 COW/Zygote가 boot chain 안에서 연결된다.

---

## CHAPTER 18 · package scan과 persistent metadata가 앱 실행 가능 상태를 만든다

Package Manager는 설치된 package, signature, component declaration, permission 등 metadata를 관리한다.

boot/update 후 package scan과 optimization state가 startup에 영향을 줄 수 있다.

### package가 설치돼 있다는 것과 process가 실행 중이라는 것은 다르다

persistent package metadata는 storage에 있고 app process는 필요할 때 생성된다.

설치된 300개 앱이 전부 300개 process로 항상 떠 있는 것이 아니다.

---

## CHAPTER 19 · boot completed는 모든 background 일이 끝났다는 뜻이 아니다

사용자가 launcher를 볼 수 있게 된 뒤에도 optimization, index, cloud sync 등 background work가 계속될 수 있다.

그래서 reboot 직후 benchmark와 몇 분 뒤 benchmark가 다를 수 있다.

### post-boot contention

```text
app startup
competes with
package optimization
media scan
sync
thermal recovery
```

실험할 때 reboot 후 안정화 조건을 명확히 한다.

---

## CHAPTER 20 · bootloop를 `Android가 깨졌다` 한 문장으로 끝내지 않는다

bootloop 지점을 구분한다.

```text
bootloader 이전
bootloader verification
kernel early boot
kernel panic
init/service crash loop
Zygote crash
system_server crash
launcher/framework failure
```

각 층에서 evidence가 다르다.

### 화면 logo 위치로만 단계 판단하지 않는다

vendor splash/boot animation과 실제 internal stage가 정확히 1:1 대응한다고 가정하면 안 된다.

serial log, boot reason, pstore, tombstone, logcat availability 등으로 stage를 확인한다.

---

## CHAPTER 21 · watchdog은 멈춘 critical component를 감지한다

kernel/userspace/platform에는 서로 다른 watchdog mechanism이 있을 수 있다.

중요한 service가 heartbeat/progress를 내지 못하면 system reset/restart가 발생할 수 있다.

### watchdog reset은 root cause가 아니라 결과일 수 있다

watchdog이 device를 reboot시켰더라도 원인은:

```text
deadlock
interrupt disabled too long
I/O hang
scheduler starvation
hardware failure
```

일 수 있다.

reset 직전 evidence를 보존해야 한다.

---

## CHAPTER 22 · boot reason을 다음 장애 조사로 전달한다

reset cause를 다음 boot에서 읽을 수 있으면 `사용자가 전원 버튼으로 재부팅`과 `kernel panic/watchdog`을 구분할 수 있다.

Android AOSP는 canonical boot reason을 bootloader→bootconfig/kernel parameters→Android property로 전달하는 규칙을 정의한다.

이 정보는 crash recovery telemetry의 시작점이다.

---

## CHAPTER 23 · update 후 boot failure에는 rollback 전략이 필요하다

system update가 boot-critical component를 바꾼다면 failure가 application update보다 치명적이다.

필요한 방어:

```text
A/B slot
boot success mark
rollback protection과 호환되는 fallback
recovery environment
signed images
migration compatibility
```

### data migration과 OS rollback 충돌

새 OS가 persistent data format을 irreversible하게 바꾼 뒤 old slot로 rollback하면 old version이 data를 읽지 못할 수 있다.

boot slot rollback만으로 update safety가 완성되는 것이 아니다.

---

## CHAPTER 24 · full-stack startup latency를 하나의 timeline으로 만든다

예:

```text
0ms      reset
120ms    early firmware
450ms    bootloader verification complete
900ms    kernel entry
1600ms   init userspace
3300ms   Zygote/framework ready
5000ms   launcher visible
6200ms   user taps app
6400ms   app process ready
6900ms   first frame
```

이 timeline이 있어야 `부팅 7초`를 실제 구간으로 나눌 수 있다.

### 서로 다른 clock source

early firmware, kernel, Android userspace가 서로 다른 timestamp origin을 사용할 수 있다.

trace를 합칠 때 기준점을 정렬해야 한다.

---

## CHAPTER 25 · 전원부터 앱 화면까지의 불변조건

각 단계가 다음 단계로 넘길 때 최소 네 가지를 묻는다.

```text
1. control은 누구에게 넘어가는가?
2. 실행할 code/data의 authenticity/integrity는 누가 보장하는가?
3. memory/resource는 어떤 상태로 전달되는가?
4. 실패하면 어디에 evidence가 남는가?
```

이 질문은 boot뿐 아니라 application architecture에도 그대로 적용된다.

---

## CHAPTER 26 · 종합 사고 실험: OTA 후 특정 기기만 bootloop

### 관찰

```text
model A: 0.2% bootloop
model B: 정상
update package signature 정상
```

### 가설 분해

```text
boot slot selection?
verified boot metadata?
kernel/vendor module compatibility?
device tree variant?
userspace service migration?
data schema?
thermal/power/hardware coincidence?
```

### evidence

```text
bootloader state
AVB error
kernel/pstore
init service logs
tombstone
boot reason
model-specific build fingerprint
```

`Android 업데이트 문제`를 hardware variant와 boot stage로 쪼갠다.

---

## PART 11 종료 점검

1. reset 직후 OS보다 먼저 실행되는 code가 왜 필요한가?
2. firmware와 bootloader의 역할을 어떻게 구분할 수 있는가?
3. Verified Boot chain of trust의 시작점이 왜 보호돼야 하는가?
4. signature가 유효해도 rollback protection이 필요한 이유는 무엇인가?
5. A/B update에서 `bytes written`과 `boot successful`이 다른 이유는 무엇인가?
6. kernel image와 ramdisk/bootconfig는 어떤 역할 차이가 있는가?
7. device tree가 driver probe에 어떤 정보를 제공하는가?
8. kernel에서 userspace init으로 control이 넘어가는 의미는 무엇인가?
9. SELinux denial이 Unix permission과 별개일 수 있는 이유는 무엇인가?
10. Zygote가 boot chain과 app startup chain을 어떻게 연결하는가?
11. boot completed 뒤에도 benchmark가 불안정할 수 있는 이유는 무엇인가?
12. watchdog reset을 root cause라고 바로 결론 내리면 안 되는 이유는 무엇인가?
13. OS rollback과 persistent data migration이 충돌할 수 있는 이유는 무엇인가?
14. boot latency를 하나의 숫자가 아니라 timeline으로 기록해야 하는 이유는 무엇인가?

이제 전원 버튼에서 앱 화면까지를 `기기가 켜진다`라고 압축하지 않는다. **root of trust → bootloader → kernel → init → framework → Zygote → app process → first frame**의 control chain으로 설명할 수 있어야 한다.