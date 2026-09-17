# PART 11 · 부트 체인 — reset에서 Android 서비스 가동까지

부팅은 전원 인가 뒤 코드가 순서대로 실행되는 단순 목록이 아니다. 각 단계는 **다음 단계에 제어권을 넘기기 전에 실행 이미지의 신뢰성, 메모리 배치, 하드웨어 상태, rollback 가능성**을 확정한다. boot failure는 어느 handoff invariant가 깨졌는지 찾아야 한다.

---

## CHAPTER 01 · reset은 CPU architectural state를 정의된 출발점으로 되돌린다

reset 이후 processor는 architecture와 SoC가 정한 초기 privilege level, register state, exception configuration, instruction fetch location에서 실행을 시작한다. 이 시점에는 일반 application runtime, filesystem, Android framework가 존재하지 않는다. DRAM조차 platform 초기화가 끝나기 전에는 일반 memory처럼 사용할 수 없을 수 있다.

부트 문제를 분석할 때 `전원이 들어왔다`와 `CPU가 executable instruction을 fetch했다`를 구분한다. power rail, clock, reset deassertion, boot ROM entry 중 어디까지 도달했는지 확인해야 한다. serial console, hardware trace, boot stage marker 같은 가장 이른 evidence가 중요하다. OS log가 없다는 사실만으로 kernel이 원인이라고 결론내릴 수 없다.

---

## CHAPTER 02 · 초기 firmware는 DRAM과 platform resource를 다음 stage가 사용할 수 있게 만든다

boot ROM 또는 초기 firmware는 SoC/board-specific clock, power domain, memory controller, security state를 설정할 수 있다. DRAM training과 controller initialization이 실패하면 더 높은 단계의 bootloader가 정상 image를 가지고 있어도 실행 공간을 만들지 못한다.

firmware가 수행하는 구체 작업은 platform마다 다르므로 일반화된 명령 sequence를 외우지 않는다. 대신 다음 stage에 넘기는 **memory map, initialized device, boot parameter, security state**가 무엇인지 확인한다. firmware update 이후 boot가 깨졌다면 kernel image보다 먼저 memory initialization과 handoff structure의 version compatibility를 검토한다.

DRAM training 문제는 단순 “메모리 불량”과 firmware algorithm/version 문제를 구분해야 한다. training retry 수, selected frequency/timing, memory vendor/revision, ECC 또는 controller error status를 가능한 범위에서 persistent marker에 남기면 cold boot·warm reset 차이를 비교할 수 있다. 동일 board에서 firmware만 바꿨을 때 실패한다면 전압·clock·training parameter가 어느 handoff에서 달라졌는지 확인하고, 실패 직전 stage code와 firmware build ID를 함께 보존한다.

---

## CHAPTER 03 · bootloader는 image 선택, 검증, 배치, handoff를 수행한다

Android bootloader는 boot partition 계열 image를 선택하고 검증한 뒤 kernel과 ramdisk, device-tree 관련 정보를 메모리에 배치해 kernel entry로 제어권을 넘긴다. active slot, boot reason, command line/bootconfig 같은 metadata가 image 선택과 kernel 동작에 영향을 준다.

bootloader correctness는 `image를 읽었다`로 끝나지 않는다. image format version, load address, alignment, verified-boot metadata, rollback index, selected slot이 일관되어야 한다. 업데이트 후 특정 slot만 부팅 실패한다면 동일 kernel source라도 slot metadata나 partition content가 다를 수 있다. bootloader log와 partition digest를 함께 비교한다.

---

## CHAPTER 04 · chain of trust는 다음 단계의 code identity를 cryptographically 연결한다

Verified Boot 계열은 현재 신뢰된 stage가 다음 stage image의 signature/hash metadata를 검증해 신뢰를 이어 간다. 핵심 invariant는 **실행되는 code가 허가된 key와 version policy에 의해 검증되었는가**다. 단순 checksum은 accidental corruption을 찾을 수 있어도 공격자가 image와 checksum을 함께 바꾸는 것을 막지 못한다.

chain이 중간에서 끊기면 이후 stage가 정상이어도 trusted boot로 간주할 수 없다. root of trust가 어떤 key/material에 고정되는지, chained partition의 public key가 어떤 descriptor로 연결되는지 확인한다. 개발용 unlock 상태와 production locked state는 허용 정책이 다르므로 boot log에서 device state를 같이 기록한다.

검증 실패를 조사할 때는 단순 pass/fail보다 어느 descriptor와 partition digest, 어떤 public-key identity에서 chain이 끊겼는지 남긴다. vbmeta 계층이 여러 개라면 top-level trust anchor부터 chained vbmeta와 실제 partition hash까지 경로를 재구성한다. unlock/relock이나 key rotation 이후에는 device에 저장된 trust state가 release pipeline의 signer와 일치하는지 확인한다. 동일 image가 일부 device에서만 거부된다면 key provisioning과 rollback state 차이도 비교 대상이다.

---

## CHAPTER 05 · signature 검증은 key 신뢰와 image 내용의 결합이다

signature가 유효하다는 사실은 `누가 서명했는가`와 `무엇에 서명했는가`가 모두 맞을 때 의미가 있다. 잘못된 key를 trust store에 넣으면 cryptographic 검증 자체가 완벽해도 잘못된 publisher를 신뢰한다. 반대로 올바른 key라도 metadata coverage가 빠진 영역은 보호되지 않는다.

release process에서는 signing key custody, key rotation, revoked key 처리, reproducible artifact identity를 운영 절차로 관리한다. build artifact SHA와 signing metadata를 release record에 보존하면 device-side verification failure를 CI/build output과 연결할 수 있다. signature error를 `파일 깨짐` 한 문장으로 처리하지 않는다.

key rotation은 새 signer만 준비한다고 끝나지 않는다. 이전 device가 어떤 key set을 신뢰하는지, 새 image가 어느 transition window에서 old/new key 모두와 호환되는지, revoke 이후 downgrade path가 막히는지 확인해야 한다. incident record에는 certificate/public-key digest, signed artifact digest, signing tool/version을 남겨 “서명은 됐다”가 아니라 정확히 어떤 identity가 어떤 bytes를 승인했는지 재현한다. signing service 장애와 artifact corruption도 별도 failure domain으로 분리한다.

---

## CHAPTER 06 · rollback protection은 유효하게 서명된 과거 취약 image도 거부한다

signature만 확인하면 공격자가 과거에 정상 서명되었지만 알려진 취약점이 있는 image로 downgrade할 수 있다. rollback index/version policy는 현재 허용된 최소 version보다 오래된 image의 실행을 차단한다.

업데이트 실패 뒤 이전 slot로 돌아가는 기능과 security rollback protection은 충돌 가능성이 있다. fallback 가능한 image가 현재 rollback policy를 만족해야 한다. OTA 설계에서는 새 slot activation 시점, rollback index 확정 시점, boot-success mark 시점을 함께 검토해 update 중 전원 손실이 device brick으로 이어지지 않게 한다.

rollback index를 너무 일찍 올리면 새 slot이 실제로 healthy한지 확인하기 전에 이전 정상 slot까지 실행 불가가 될 수 있다. 반대로 너무 늦게 확정하면 취약 image로 되돌릴 window가 남는다. power-loss injection을 slot write 완료 전후, first boot, boot-success mark 전후에 수행해 어느 조합에서도 최소 하나의 허용 가능한 boot path가 남는지 검증한다. device에 저장된 index와 각 slot metadata를 incident 시 함께 캡처하면 fallback 실패 원인을 구체화할 수 있다.

---

## CHAPTER 07 · A/B slot은 update와 active boot image를 분리한다

seamless update 구조에서는 현재 실행 중인 slot과 비활성 slot을 분리해 새 image를 비활성 쪽에 준비한 뒤 다음 boot에서 전환할 수 있다. 실패 시 다른 slot로 되돌릴 수 있지만 두 slot의 metadata와 user data compatibility가 유지되어야 한다.

slot switch 자체가 rollback을 보장하지 않는다. database/data schema가 새 version에서 irreversible하게 변경되면 code slot을 되돌려도 user state가 구버전과 호환되지 않을 수 있다. OTA 전체를 **boot partition rollback + persistent data compatibility**라는 두 축으로 설계해야 한다.

slot metadata의 retry counter, active/bootable/successful 상태는 recovery state machine의 일부다. 새 slot을 한 번 실패했다고 즉시 영구 불량으로 볼지, 일정 횟수 재시도할지 bootloader policy를 알아야 한다. 정상 부팅 후 successful mark가 너무 늦으면 transient reboot가 불필요한 rollback을 만들고, 너무 빠르면 framework가 아직 불안정한 image를 정상으로 확정할 수 있다. test에서는 boot success marker 이전의 watchdog reset과 이후의 app/data migration failure를 따로 주입한다.

---

## CHAPTER 08 · boot image는 kernel과 early userspace를 연결하는 배포 단위다

Android release에 따라 boot, vendor_boot, init_boot 등 image 역할과 header 구조가 달라질 수 있다. kernel binary, generic/vendor ramdisk, boot metadata가 어떤 partition에 있는지 platform version에 맞춰 확인해야 한다.

image unpack/repack 도구가 header version이나 alignment를 잘못 처리하면 contents가 맞아 보여도 bootloader가 거부할 수 있다. custom kernel/debug build를 만들 때 source 코드뿐 아니라 image construction pipeline과 AVB footer까지 같은 release contract의 일부로 본다.

header version이 달라지면 field 위치와 ramdisk 분리 규칙, page alignment, bootconfig 결합 방식이 바뀔 수 있으므로 다른 generation의 repack tool을 섞지 않는다. 검증 시 image 전체 hash뿐 아니라 parsed header의 kernel/ramdisk size, load parameter, compression format, footer 위치를 release manifest와 비교한다. bootloader가 image를 읽었지만 kernel entry에 도달하지 못한다면 decompression·load address·memory overlap 문제도 포함해 image construction 단계와 handoff memory map을 함께 본다.

---

## CHAPTER 09 · bootconfig와 kernel command line은 early-boot configuration contract다

kernel과 early userspace는 bootloader가 전달한 parameter를 통해 root device, console, hardware mode, Android-specific configuration을 받을 수 있다. Android 12+ 계열에서는 bootconfig가 command-line parameter 일부를 구조적으로 전달하는 데 사용된다.

configuration mismatch는 binary corruption 없이도 boot를 바꾼다. 같은 kernel image가 다른 bootconfig로 다른 filesystem, debug mode, device state를 선택할 수 있다. 문제 재현 시 image hash만 비교하지 말고 effective command line/bootconfig를 capture한다.

parameter provenance도 중요하다. bootloader default, slot metadata, device-specific overlay가 같은 key를 정의하면 최종 effective value가 어디서 왔는지 알아야 한다. 정상/실패 device에서 raw source와 kernel이 실제 파싱한 `/proc/cmdline` 또는 bootconfig view를 함께 비교하면 생성 단계와 소비 단계 차이를 찾을 수 있다. 민감한 debug flag나 security mode가 production image에 우연히 활성화되지 않도록 allowlist와 release-time validation을 둔다.

---

## CHAPTER 10 · kernel early init은 scheduler·memory·interrupt·filesystem 기반을 만든다

kernel은 architecture initialization, page allocator, scheduler, interrupt subsystem, device model, VFS 같은 핵심 subsystem을 단계적으로 초기화한다. 여기서 실패하면 Android framework까지 도달하지 못한다.

panic 메시지의 마지막 줄만 보지 말고 earliest warning과 call trace, loaded driver, mount state를 본다. early boot에서 console driver가 늦게 초기화되면 실제 실패보다 log가 늦게 보일 수 있으므로 persistent ramoops/pstore 같은 crash evidence가 유용하다. kernel stage와 userspace init stage를 명확히 나눈다.

initcall tracing이나 boot-time marker를 사용하면 어느 subsystem 초기화가 마지막으로 완료됐는지와 긴 지연이 어디서 발생했는지 찾을 수 있다. early panic이 serial console 준비 전 발생하는 platform에서는 pstore/ramoops 영역과 reset 후 보존 조건을 사전에 검증해야 실제 장애 때 증거를 얻을 수 있다. kernel build ID, config, command line, DT digest를 함께 보존하면 같은 source라도 configuration 차이로 발생한 early-boot failure를 분리할 수 있다.

---

## CHAPTER 11 · device tree는 hardware topology와 resource를 kernel에 전달한다

Device Tree는 discoverable bus만으로 충분히 알기 어려운 hardware instance, register range, interrupt, clock, GPIO, compatibility 정보를 기술할 수 있다. kernel driver는 compatible binding과 resource description을 이용해 device를 probe한다.

DT mismatch는 driver code가 정상이어도 probe failure, wrong interrupt, invalid MMIO mapping을 만들 수 있다. board revision과 DTB/DTBO 조합을 release artifact로 관리하고 schema/binding validation을 자동화한다. firmware와 kernel이 같은 hardware description contract를 공유하는지 확인한다.

DT overlay를 사용하는 device에서는 base DT와 board/SKU별 overlay 조합 자체가 artifact identity다. property type과 unit, phandle dependency, reserved-memory overlap을 schema validation으로 잡고 실제 boot에서는 선택된 DTB/DTBO digest를 기록한다. 특정 board revision만 실패한다면 driver binary보다 compatible string, GPIO polarity, regulator/clock reference 차이를 우선 비교한다. 잘못된 resource address는 probe 단계뿐 아니라 뒤늦은 DMA/interrupt corruption으로도 나타날 수 있다.

---

## CHAPTER 12 · driver probe는 hardware resource ownership을 kernel object로 전환한다

probe 시 driver는 device resource를 map하고 interrupt/DMA/clock을 준비하며 subsystem에 device를 등록한다. probe가 성공했다는 것은 device가 완전 정상이라는 뜻이 아니라 kernel이 operation을 시작할 최소 state를 만들었다는 뜻이다.

probe defer는 dependency가 아직 준비되지 않았을 때 정상적인 retry mechanism일 수 있다. 반면 반복 defer나 timeout은 dependency graph 문제를 나타낼 수 있다. boot latency 분석에서 특정 driver probe가 critical path를 막는지, parallel initialization이 가능한지 trace로 확인한다.

probe가 중간 단계에서 실패하면 이미 enable한 clock/regulator, IRQ, DMA mapping을 역순으로 정리해야 이후 retry나 다른 driver bind가 깨지지 않는다. repeated defer의 dependency 이름과 provider readiness를 기록하면 단순 횟수보다 graph cycle을 찾기 쉽다. device가 register된 직후 userspace가 접근할 수 있다면 initialization 완료 전에 observable state를 노출하지 않도록 publish point를 명확히 한다. reset/resume path도 probe에서 만든 ownership contract를 동일하게 복원해야 한다.

---

## CHAPTER 13 · init은 kernel 이후 user-space dependency graph를 시작한다

kernel이 첫 user-space process를 시작하면 service/process/mount/permission initialization이 user space에서 이어진다. Linux 일반론에서는 init/system manager가 역할을 담당하고 Android는 자체 init language와 service definition을 사용한다.

서비스 시작 순서는 단순 파일 순서가 아니라 trigger, property, class, dependency에 의해 결정될 수 있다. boot hang을 분석할 때 process가 생성됐는지뿐 아니라 어떤 property/condition을 기다리는지 본다. user-space deadlock과 kernel hang을 log timestamp로 분리한다.

user-space startup graph를 조사할 때 service PID가 존재한다는 사실과 readiness가 충족됐다는 사실을 분리한다. socket/file/property 같은 dependency를 아직 제공하지 못한 service가 살아 있기만 하면 후속 단계는 계속 기다릴 수 있다. trigger firing 시각, service start/exit code, restart count, required mount/property를 graph 형태로 기록하면 “init에서 멈췄다”를 실제 missing edge로 바꿀 수 있다. crash-loop가 critical dependency를 반복 생성·삭제하는 경우도 별도 표시한다.

---

## CHAPTER 14 · Android init은 mount·property·service를 platform startup graph로 엮는다

Android init은 rc configuration을 읽어 filesystem mount, property action, daemon/service start를 수행한다. vold, servicemanager, hwservicemanager, Zygote 등 핵심 process가 준비되어야 framework startup이 진행된다.

property trigger가 잘못되거나 service가 crash-loop하면 후속 action이 실행되지 않을 수 있다. `bootanimation이 멈췄다`는 UI symptom보다 init service state, restart count, property progression을 확인한다. service restart policy가 무한 bootloop를 확대하는지도 본다.

rc action의 trigger 조건이 여러 property에 걸치면 하나만 누락돼도 action이 영원히 실행되지 않을 수 있다. 정상 device와 실패 device의 property transition timeline을 비교하고 service class 시작 시각, disabled/oneshot/critical flag를 함께 본다. mount namespace와 file label이 준비되기 전에 daemon을 시작하면 재시작 후에만 우연히 성공하는 race가 생길 수 있다. cold-boot 반복 테스트로 startup ordering이 deterministic하게 readiness 조건을 만족하는지 검증한다.

---

## CHAPTER 15 · SELinux enforcing state는 부팅 초기에 policy boundary를 만든다

Android는 SELinux mandatory access control을 사용해 process domain과 object type 사이 access를 제한한다. early boot에서 policy load와 domain transition이 맞지 않으면 service가 실행되더라도 필요한 file/device/Binder access를 거부당할 수 있다.

`avc: denied`를 전부 allow rule로 해결하지 않는다. 요청 주체 domain이 의도한 domain인지, target type이 올바르게 label되었는지 먼저 본다. 잘못된 domain transition을 broad allow로 덮으면 boot는 되더라도 sandbox boundary가 약화된다.

policy 변경은 allow rule 하나가 아니라 type transition과 file context까지 함께 검토한다. executable label이 틀리면 service가 예상 domain으로 전환되지 않고 이후 여러 denial이 연쇄적으로 발생할 수 있다. audit event의 source context, target context, class, permission을 startup service ID와 연결하고 정상 device와 비교한다. 수정 후에는 boot가 진행되는지뿐 아니라 원래 금지돼야 하는 cross-domain access가 계속 거부되는 negative test도 수행해야 한다.

---

## CHAPTER 16 · SystemServer는 framework service graph의 핵심 coordinator다

SystemServer는 ActivityManager, PackageManager 등 다수 framework service를 초기화한다. 특정 service 초기화가 synchronous critical path에 있으면 boot complete와 app launch 가능 시점이 늦어질 수 있다.

SystemServer crash는 framework 전체를 재시작시키고 bootloop로 이어질 수 있다. Java stack, watchdog trace, native Binder dependency를 함께 본다. service start order와 dependency를 바꾸는 patch는 기능이 떠도 race를 만들 수 있으므로 boot stress/restart test가 필요하다.

framework service가 startup 중 synchronous Binder나 disk/database 작업을 수행하면 실제 병목은 SystemServer 코드 밖에 있을 수 있다. trace에서 service init span, Binder callee, lock owner를 연결하고 watchdog monitored thread가 어디서 기다리는지 본다. 새로운 service를 critical phase에 추가했다면 cold boot p50뿐 아니라 tail과 failure recovery를 측정한다. SystemServer 재시작 뒤 service state가 중복 등록되거나 stale native daemon state와 충돌하지 않는지도 반복 restart test에서 확인한다.

---

## CHAPTER 17 · Zygote는 runtime warm state를 app process 생성에 재사용한다

Zygote는 ART runtime과 framework class 일부를 preload한 뒤 application process 생성의 parent 역할을 한다. fork 계열 semantics와 copy-on-write 덕분에 초기 memory를 공유할 수 있다.

preload를 늘리면 app startup은 빨라질 수 있지만 boot time과 system memory footprint가 증가할 수 있다. app process에서 shared page를 write해 private COW page로 바꾸면 sharing 이점이 줄어든다. preload decision을 boot/app-start 양쪽 metric으로 평가한다.

preload 후보는 단순히 많이 쓰이는 class가 아니라 여러 app process가 read-mostly로 재사용할 수 있는지까지 봐야 한다. preload 후 각 child가 page를 수정하면 PSS 이득이 사라지고 boot CPU/I/O만 늘어난다. Zygote preload time, shared/private PSS, cold-start class-loading span을 전후 비교하면 비용 이동을 확인할 수 있다. fork 이후 process-specific native handle이나 thread state는 안전하게 재초기화돼야 하므로 preload 가능한 state와 child-owned state의 경계를 명확히 한다.

---

## CHAPTER 18 · package state는 설치된 artifact와 framework registry를 연결한다

PackageManager는 installed package, version, signature, component, permission, user-specific enabled state를 관리한다. APK file이 존재한다고 component가 launchable한 것은 아니다. disabled state, user/profile, signature/permission relation이 실제 launch path에 영향을 준다.

OTA나 restore 이후 package metadata와 data directory ownership이 어긋나면 app crash가 발생할 수 있다. package scan log, uid assignment, data migration result를 함께 확인한다. app 문제처럼 보여도 framework package state corruption일 수 있다.

package scan에서는 code path와 signature가 같아도 user별 enabled/suspended 상태, shared UID history, permission grant가 달라질 수 있다. 정상/실패 user profile에서 package registry와 data directory owner/SELinux label을 비교한다. system package replace 뒤 old oat/dex artifact나 stale component state가 남는 문제도 고려한다. repair가 필요할 때 단순 data clear로 증거를 지우기 전에 registry snapshot과 scan error를 보존해 설치 metadata와 app-owned data 중 어느 쪽이 깨졌는지 구분한다.

---

## CHAPTER 19 · post-boot optimization은 boot complete 이후에도 실행 특성을 바꾼다

ART profile collection, dexopt/background compilation, cache warmup 같은 작업은 UI가 보인 뒤에도 진행될 수 있다. OTA 직후 첫 사용과 며칠 사용한 device의 performance가 다른 이유가 여기 있다.

성능 regression을 평가할 때 OTA 직후 state와 steady-state를 구분한다. background optimization이 CPU/thermal budget을 사용해 foreground app latency에 영향을 주는 경우도 있으므로 trace와 scheduler metric을 같이 본다.

post-boot job은 charging/idle/storage 같은 constraint에 따라 실행 시점이 달라질 수 있어 동일 OTA라도 device별로 warmness가 다르다. performance test에서는 package compilation state, profile presence, job 실행 여부를 기록하고 fresh-update와 steady-state cohort를 분리한다. dexopt가 foreground workload와 겹치면 CPU·I/O contention과 thermal throttling이 나타날 수 있으므로 job span과 app latency를 동일 trace에 놓는다. 최적화 완료 후 개선되는 문제를 code regression으로 오해하지 않는다.

---

## CHAPTER 20 · bootloop는 동일 실패가 restart policy를 통해 반복되는 상태다

bootloop는 kernel panic 반복, critical userspace service crash, SystemServer crash, verified boot slot fallback 실패 등 여러 계층에서 발생할 수 있다. 화면 로고만으로 원인을 정할 수 없다.

반복 횟수마다 가장 이른 persistent log와 boot reason을 보존한다. 동일 stack인지, slot이 바뀌는지, watchdog/reset source가 달라지는지 비교한다. 자동 recovery가 원본 failure evidence를 덮어쓰지 않도록 crash counter와 persistent storage를 설계한다.

각 반복을 boot generation으로 번호 매기고 selected slot, reset reason, 마지막 progress marker, failure stack을 함께 저장하면 같은 failure의 재시도인지 recovery path가 바뀐 새 failure인지 구분할 수 있다. log buffer가 reboot마다 초기화되면 pstore나 작은 persistent record에 핵심 marker만 남긴다. recovery 정책이 일정 횟수 후 slot 변경·safe mode·factory recovery로 이동한다면 state transition을 명시해 loop 자체가 evidence를 파괴하거나 무한 reset으로 battery를 소모하지 않게 한다.

---

## CHAPTER 21 · watchdog은 progress invariant가 깨졌을 때 강제 복구를 시작한다

watchdog은 특정 thread/service가 정해진 시간 안에 heartbeat/progress를 만들지 못하면 stack dump, restart, reboot를 수행할 수 있다. timeout은 root cause가 아니라 system이 `더 기다려도 안전하지 않다`고 판단한 결과다.

watchdog dump에서 monitored lock과 thread state를 찾아야 한다. timeout 값을 늘려 증상을 없애면 deadlock이나 unbounded blocking을 숨길 수 있다. 정상 worst-case latency와 실제 hang을 구분해 threshold를 정한다.

heartbeat가 살아 있어도 실제 업무 progress가 멈출 수 있으므로 단순 timer tick보다 queue drain, transaction sequence, service milestone처럼 의미 있는 progress signal을 감시하는 편이 강하다. watchdog firing 시 monitored thread뿐 아니라 lock owner, Binder callee, runqueue state를 함께 캡처하면 timeout 직전의 dependency chain을 복원할 수 있다. threshold 변경은 정상 p99 stall과 recovery objective를 근거로 하고 fault injection으로 실제 deadlock에서 지정된 시간 안에 dump와 recovery가 수행되는지 검증한다.

---

## CHAPTER 22 · boot reason은 이전 종료 원인을 다음 부팅에서 전달한다

watchdog reset, kernel panic, brownout, user reboot처럼 reset source가 다르면 조사 방향이 달라진다. bootloader/kernel/platform이 boot reason을 persistent register나 storage에 남길 수 있다.

boot reason string 하나를 신뢰하지 말고 hardware reset status, pstore/tombstone, battery/power telemetry와 교차검증한다. power loss는 software log를 flush하지 못할 수 있으므로 마지막 log 부재가 software fault 부재를 의미하지 않는다.

일부 reset register는 다음 boot에서 읽은 뒤 clear되므로 수집 순서가 중요하다. early boot agent가 raw reset-status bit와 decoded reason을 둘 다 보존하면 decoder bug나 firmware version 차이를 나중에 검증할 수 있다. watchdog이 결국 hardware reset을 유발한 경우 최종 reset source만 보면 원래 software hang을 놓칠 수 있으므로 watchdog persistent marker와 함께 본다. brownout 의심 시 전압/PMIC event와 reset timestamp를 맞춰 software panic과 전원 문제를 분리한다.

---

## CHAPTER 23 · update rollback은 code slot보다 data migration까지 포함한다

A/B update가 old slot로 돌아갈 수 있어도 새 version이 persistent database를 irreversible하게 변경했다면 실질 rollback은 실패한다. migration은 backward-compatible expansion, dual-read/write, finalize 단계로 나누는 전략을 사용할 수 있다.

update package, rollback index, data schema version, package state의 compatibility matrix를 release 전 검증한다. `부팅 성공`만 OTA acceptance criteria로 두면 app-level data corruption을 놓친다.

migration completion marker는 실제 durable schema/data 변경보다 먼저 기록돼서는 안 된다. power loss와 rollback을 각 migration phase에 주입해 old code가 new/intermediate state를 읽을 때도 안전한지 확인한다. irreversible finalize는 rollout이 안정적이고 rollback window가 종료된 뒤 별도 단계로 수행하는 편이 복구 여지를 높인다. system service와 user app schema가 서로 다른 시점에 migration된다면 version handshake와 minimum/maximum compatible version을 명시한다.

---

## CHAPTER 24 · startup timeline은 stage별 timestamp로 critical path를 찾는다

boot duration을 한 숫자로만 측정하면 firmware, kernel, init, framework 중 어디가 느려졌는지 알 수 없다. bootloader marker, kernel timestamp, init event, SystemServer milestone, boot-complete property, first app frame을 하나의 timeline으로 만든다.

clock source가 stage마다 다를 수 있으므로 raw timestamp origin을 확인한다. regression은 stage delta로 비교하고, parallel stage의 합을 단순히 total에 더하지 않는다. critical path에 있는 blocking dependency를 찾아야 실제 boot time이 줄어든다.

firmware counter, kernel monotonic clock, Android trace clock이 서로 다른 origin/frequency를 가질 수 있어 handoff marker를 이용한 clock alignment가 필요하다. 각 stage의 시작·완료만 나열하지 말고 dependency edge를 표시해 병렬 init 중 실제 boot-complete를 지배한 path를 계산한다. optimization 전후에는 total뿐 아니라 critical stage wait와 service 시간을 비교한다. 한 stage를 빠르게 했는데 다음 dependency가 즉시 병목으로 이동했다면 기대한 total 개선이 나오지 않을 수 있다.

---

## CHAPTER 25 · boot invariant는 각 handoff에서 검증 가능한 조건으로 적는다

boot chain을 안정적으로 운영하려면 각 stage의 완료를 명시한다.

```text
firmware: DRAM과 다음-stage storage 접근 가능
bootloader: 선택 image 검증/배치 완료
kernel: root/early userspace 실행 가능
init: 필수 daemon과 mount 준비
framework: core system service healthy
package: user app launch prerequisites 충족
```

하나의 `booted=true` 상태로 합치지 않는다. health marker가 너무 늦으면 rollback 판단이 지연되고 너무 이르면 이후 critical failure를 놓친다. stage-specific readiness를 release test와 watchdog 기준에 연결한다.

각 readiness marker는 **누가 쓰고, 어디에 durable하게 남고, 어떤 failure에서 무효화되는지**도 정의해야 한다. 예를 들어 framework-ready를 기록한 직후 SystemServer가 반복 crash한다면 해당 marker를 최종 boot success로 사용할 수 없다. marker generation과 slot/version identity를 함께 저장해 이전 boot의 정상 표시가 새 image에 재사용되지 않게 한다. release test는 각 invariant 직전/직후 fault를 주입해 recovery policy가 예상 stage로 정확히 돌아가는지 검증한다.

---

## CHAPTER 26 · OTA 장애 분석은 artifact identity에서 사용자 state까지 한 chain으로 추적한다

OTA 후 bootloop가 발생하면 `업데이트가 문제`라고 끝내지 않는다. current/previous slot, partition hash, AVB result, rollback index, boot reason, kernel build ID, init service crash, SystemServer trace, data migration version을 한 incident record에 묶는다.

정상 device와 실패 device의 차이를 같은 구조로 비교하면 failure stage가 좁아진다. 수정안은 그 stage invariant를 직접 검증하는 regression test를 포함해야 한다. boot chain의 핵심은 순서를 외우는 것이 아니라 **각 handoff가 무엇을 보장하고 어떤 증거로 그 보장을 확인하는가**다.

비교표에는 device/board revision, firmware/bootloader version, selected slot metadata, effective bootconfig, DT/partition digest, package/data schema까지 넣어 binary 차이와 persistent-state 차이를 분리한다. 최초 실패 boot의 evidence를 recovery 시도 전에 보존하고, 이후 boot generation마다 어떤 자동 조치가 적용됐는지 기록한다. 수정 후에는 정상 OTA 한 번만 확인하지 말고 download/write 중단, first-boot crash, migration 중단, watchdog reset, rollback을 반복해 원래 깨졌던 handoff invariant가 모든 recovery path에서 유지되는지 확인한다.