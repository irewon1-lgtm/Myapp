# PART 83 · System Suspend and Resume State Machine — device phases, wakeup races, runtime-PM reconciliation

System suspend는 `CPU를 재운다`는 단일 operation이 아니다. Userspace execution을 멈추고, device dependency graph를 따라 여러 callback phase를 통과시키고, runtime-PM state와 system-sleep state를 정렬하고, wakeup source만 남긴 채 interrupt/timekeeping/CPU 상태를 축소한 뒤 platform sleep에 진입한다. Resume는 이 순서를 단순 역재생하는 것이 아니라 **hardware가 다시 접근 가능한 시점, IRQ가 다시 허용되는 시점, child/parent dependency가 복구되는 시점**을 맞춰야 한다. Suspend bug는 평상시에는 드러나지 않고 rare wakeup timing, device removal, runtime-suspended state, failed callback에서만 나타나므로 전체 transition을 transaction으로 봐야 한다.

## CHAPTER 01 · System sleep은 runtime PM과 다른 global state transition이다

Runtime PM은 working system 안에서 idle device 하나를 opportunistically 저전력 상태로 보내지만, system suspend는 userspace·scheduler·devices·interrupts·CPUs·platform 전체를 하나의 전환에 참여시킨다. 이미 runtime-suspended device가 system suspend에 들어올 수도 있고 active device가 직전에 runtime resume될 수도 있다. Driver가 두 경로를 별개 함수로 구현하더라도 shared hardware state와 reference count를 동시에 만지므로 race 가능성이 있다. Correctness는 `runtime suspend 성공`과 `system suspend 성공`을 따로 보는 게 아니라 **두 state machine이 교차할 때 일관된 device state를 만드는가**로 판단해야 한다.

## CHAPTER 02 · Suspend 요청은 먼저 새 activity가 계속 생기는 경로를 통제해야 한다

Userspace task가 계속 I/O를 제출하고 service가 계속 device를 깨우는 상태에서 device를 suspend하려 하면 quiescence가 끝나지 않는다. System sleep 진입은 freezer/notifier/subsystem policy를 통해 새 workload를 줄이고, driver는 suspend callback 시작 전에 queueing entry를 막거나 in-flight work를 drain해야 한다. `callback이 호출됐으니 이제 아무도 장치를 안 쓴다`는 가정은 위험하다. Suspend protocol은 **new work admission 차단→existing work drain→hardware state save/power transition** 순서를 가져야 한다.

## CHAPTER 03 · Userspace freeze는 application thread와 kernel worker를 동일하게 다루지 않는다

System suspend는 ordinary userspace가 계속 state를 변경하지 않도록 freeze할 수 있지만 모든 kernel thread를 무조건 같은 방식으로 멈출 수는 없다. Suspend 진행 자체, storage flush, device callbacks, filesystem work를 수행해야 하는 kernel execution context가 남아 있어야 한다. Freezable worker는 freezer contract에 맞춰 safe point에서 멈추고, non-freezable infrastructure는 suspend 단계 동안 필요한 progress를 보장한다. 잘못된 freezer dependency는 **A를 freeze했는데 B의 suspend가 A의 completion을 기다리는 deadlock**으로 나타난다.

## CHAPTER 04 · PM dependency graph는 parent/child와 supplier/consumer ordering을 보존해야 한다

Device가 bus controller, regulator, clock, power domain, IOMMU 같은 supplier에 의존하면 consumer가 먼저 suspend되고 supplier가 나중에 꺼져야 한다. Resume에서는 반대로 supplier가 먼저 살아야 consumer가 register와 DMA path를 복구할 수 있다. Device links와 PM core ordering은 이 dependency를 표현한다. Driver가 hidden dependency를 global pointer나 out-of-band firmware call로 만들면 PM core가 order를 알 수 없어 intermittent resume failure가 생긴다. **Dependency는 코드 호출관계가 아니라 suspend/resume ordering graph에도 등록**되어야 한다.

## CHAPTER 05 · prepare phase는 본격 suspend 전에 transition 가능성을 확인하는 경계다

Prepare callback은 device/subsystem이 system-wide transition에 들어가기 전 선행 조건을 확인하고 필요한 high-level coordination을 할 기회를 준다. 이 단계에서 runtime-suspended device를 active로 되돌려 system suspend callback이 known state에서 시작하도록 하는 subsystem도 있다. Prepare 실패는 이후 더 깊은 phase가 시작되기 전에 transition을 abort할 수 있어 rollback surface를 줄인다. Driver가 hardware power-off를 prepare에서 너무 일찍 수행하면 아직 다른 device/caller가 정상 working-state access를 할 수 있어 ordering을 깨뜨린다.

## CHAPTER 06 · suspend phase는 ordinary interrupt와 sleep 가능한 작업이 아직 가능한 마지막 주요 단계다

Device suspend callback에서는 request queue를 멈추고 DMA를 drain하며 runtime state를 save하는 등 비교적 긴 작업을 수행할 수 있다. 하지만 모든 device가 이 phase를 순서대로/병렬로 통과하므로 한 driver의 unbounded wait가 system suspend 전체를 지연시킨다. Callback이 userspace daemon, frozen worker, 이미 suspend된 child에 의존하면 deadlock한다. Suspend code는 정상 I/O path보다 dependency budget이 작고, **필요한 모든 producer가 아직 살아 있는지**를 명시적으로 검토해야 한다.

## CHAPTER 07 · late suspend는 runtime PM을 더 이상 자유롭게 움직이지 못하게 하는 전환점이다

System suspend 후반에는 PM core가 runtime PM activity를 제한해 device가 system-sleep state로 내려가는 동안 background runtime resume/suspend가 끼어들지 않게 한다. Driver가 late phase 이후에도 runtime-PM work를 새로 queue하면 state machine 두 개가 동일 register/power-domain을 경쟁할 수 있다. Late callback은 ordinary suspend에서 미리 끝낼 수 없었던 마지막 ordering-sensitive 작업에만 사용하고, 대규모 I/O drain을 뒤로 미루지 않는 편이 안전하다. **Transition이 깊어질수록 사용할 수 있는 subsystem과 execution freedom이 줄어든다.**

## CHAPTER 08 · suspend_noirq는 high-level device interrupt handler가 더 이상 실행되지 않는 단계다

Noirq phase 전에 PM core는 device IRQ action 처리를 막아 driver interrupt handler가 suspend state와 race하지 않게 한다. 이때 hardware register snapshot, final interrupt mask, wake source arm처럼 handler와 경쟁하면 위험한 작업을 수행할 수 있다. 반대로 completion을 기다리는데 그 completion을 interrupt handler가 만들어야 한다면 noirq에서 영원히 끝나지 않는다. Noirq callback은 `IRQ race가 없다`는 장점과 **IRQ-driven progress를 사용할 수 없다**는 제한을 동시에 가진다.

## CHAPTER 09 · Wakeup-capable IRQ는 ordinary device interrupt와 다른 역할로 재구성된다

System이 잠들 때 대부분의 ordinary device activity는 중단되지만 일부 event는 system을 다시 깨워야 한다. PM core/subsystem은 wakeup source가 될 수 있는 IRQ를 arm하고 나머지 activity는 working-state handler를 실행하지 않도록 제어한다. Driver가 interrupt pending bit를 잘못 clear하거나 wake enable 순서를 뒤집으면 suspend 직전 event가 유실되거나 즉시 resume loop가 생길 수 있다. Wakeup configuration은 **normal interrupt handling과 system-resume trigger를 분리하는 protocol**이다.

## CHAPTER 10 · Suspend-to-idle은 platform power-off보다 scheduler/timekeeping quiescence가 중심이다

S2idle에서는 memory와 많은 platform state가 유지되고 CPUs가 deepest idle로 들어가며 scheduler tick과 timekeeping이 suspend 흐름에 맞춰 멈춘다. Non-timer hardware interrupt가 CPU를 깨울 수 있지만 실제 system resume는 wakeup으로 arm된 event인지 판정한 뒤 시작된다. Platform firmware가 deep sleep을 제공하지 않아도 사용할 수 있어 resume latency는 낮을 수 있지만 idle leakage/power savings는 hardware에 따라 다르다. `freeze`와 `deep`을 같은 suspend 상태로 보면 power·wake semantics를 잘못 튜닝한다.

## CHAPTER 11 · Suspend-to-RAM은 CPU/device state뿐 아니라 platform firmware contract까지 포함한다

Deep suspend에서는 nonboot CPUs offline, low-level platform functions suspend, DRAM self-refresh 같은 더 강한 power transition이 들어갈 수 있다. Kernel driver state가 올바라도 firmware가 wake vector, interrupt routing, power rail sequence를 잘못 복구하면 resume가 실패한다. 따라서 suspend 문제 분석은 kernel trace만이 아니라 ACPI/firmware event와 hardware reset reason을 함께 봐야 한다. Deep sleep은 **kernel state machine이 platform state machine과 handoff하는 cross-layer transaction**이다.

## CHAPTER 12 · Wakeup race는 “잠들기 직전 event”를 잃지 않는 것이 핵심이다

Event가 suspend 준비 중 발생하면 세 가지 위험이 있다. Device가 아직 active여서 event를 정상 처리했는데 이후 무조건 sleep해 response가 늦어지거나, wakeup이 arm되기 전 event가 clear되어 유실되거나, wakeup이 pending인데 platform sleep에 들어가 즉시 다시 깨는 race가 생긴다. PM core의 wakeup-count와 driver wake state는 **event 발생 여부 확인과 sleep commit 사이의 atomicity gap**을 줄이기 위한 mechanism이다. Suspend가 가끔 즉시 되돌아오는 문제는 이 race의 증거일 수 있다.

## CHAPTER 13 · Device state save는 register dump보다 semantic state reconstruction이 중요하다

Power loss로 register contents가 사라지는 device는 resume에서 queue pointers, feature bits, interrupt config, firmware state를 다시 만들 수 있어야 한다. 모든 register를 blind save/restore하면 write-only, clear-on-read, reset-dependent field를 잘못 건드릴 수 있다. Driver는 software authoritative state를 유지하고 resume에서 hardware를 known reset state부터 재program하는 방식이 더 견고할 수 있다. **Hardware snapshot과 software intent 중 어느 것이 source of truth인지**를 register group별로 결정해야 한다.

## CHAPTER 14 · In-flight DMA는 suspend 전에 completion 또는 abort가 증명되어야 한다

Device power를 끈 뒤 DMA가 host memory에 계속 접근하면 memory corruption이나 IOMMU fault가 발생한다. Suspend는 new submission을 막고 queue를 drain하거나 hardware abort/reset을 수행한 뒤 bus mastering과 DMA mapping을 안전하게 정리해야 한다. Timeout 시 `그냥 계속 suspend`하면 stale DMA risk를 남기고, 무한 기다리면 system sleep이 불가능하다. Driver는 **drain success / abort success / fatal failure and suspend abort**를 명확히 나눠야 한다.

## CHAPTER 15 · Suspend callback failure는 이미 suspend된 device들을 역순으로 복구해야 한다

중간 device의 callback이 error를 반환하면 PM core는 system sleep 전체를 포기하고 앞서 suspend된 device들을 working state로 되돌려야 한다. Driver는 resume callback이 `실제로 sleep을 거쳤다`는 전제만 가져서는 안 되고 partial-suspend rollback에서도 안전해야 한다. Init/teardown과 마찬가지로 각 phase는 idempotent에 가까운 state check와 ownership ledger가 필요하다. Error injection으로 각 device order 위치에서 suspend failure를 넣어 **rollback graph가 정상 working state로 수렴하는지** 검증해야 한다.

## CHAPTER 16 · noirq resume는 interrupt handler보다 먼저 hardware invariants를 복원한다

Wake 후 noirq resume phase에서는 ordinary device interrupt handler가 아직 다시 실행되지 않으므로, driver는 handler가 기대하는 register/queue/lock state를 먼저 복원할 수 있다. Interrupt enable bit를 hardware에서 너무 일찍 열고 software data structure가 아직 준비되지 않았다면 pending event가 resume 중 state를 공격한다. 반대로 필요한 acknowledge/clear를 늦추면 stale interrupt storm이 시작될 수 있다. **IRQ handler publication 전에 handler precondition을 모두 복원**하는 것이 noirq resume의 핵심이다.

## CHAPTER 17 · early/resume phase는 dependency graph를 따라 ordinary device service를 재개한다

Lower-level power/bus 상태가 올라온 뒤 device resume callback이 queue, firmware, network/media pipeline 같은 service를 다시 시작한다. 독립 device는 병렬 resume될 수 있어 global static order에 기대는 driver는 race가 난다. Consumer가 supplier completion을 필요로 한다면 device link나 explicit synchronization으로 dependency를 표현해야 한다. Resume가 fast-path처럼 보이더라도 **parallel initialization + late external event arrival**을 고려한 concurrency code로 작성해야 한다.

## CHAPTER 18 · complete phase는 suspend transaction에서 빠져나와 normal policy로 돌아가는 경계다

Complete callback은 device가 working state로 돌아온 뒤 transition-specific bookkeeping을 정리하고 runtime PM 같은 normal policy를 다시 허용하는 단계다. Suspend 중 임시 reference, wakeup marker, debug timestamp를 이 시점까지 유지했다가 정리할 수 있다. Complete 이전에 userspace가 full service를 시작하도록 signal하면 아직 PM core가 transition state일 수 있어 race가 생긴다. System resume 완료는 **hardware active + dependency restored + policy reenabled**가 함께 만족되어야 한다.

## CHAPTER 19 · Runtime-suspended device를 system suspend에서 다시 깨울지 남겨둘지는 명시적 정책이다

이미 runtime suspend된 device를 system suspend 준비에서 굳이 resume하면 불필요한 latency와 power transition이 생긴다. `SMART_SUSPEND` 같은 flag는 driver 관점에서 runtime-suspended 상태를 system suspend에도 안전하게 유지할 수 있음을 subsystem에 알릴 수 있다. 하지만 device wakeup capability, PCI config save, parent state 때문에 bus layer가 실제로는 resume를 요구할 수도 있다. Optimization은 **driver-local 판단보다 전체 bus/power-domain dependency**를 우선해야 한다.

## CHAPTER 20 · Resume 후 runtime PM status는 physical hardware state와 다시 일치해야 한다

System resume path가 hardware를 D0/active로 만들었는데 runtime-PM core는 여전히 suspended라고 생각하면 다음 get/put에서 reference state가 어긋난다. 반대로 hardware는 low-power인데 core가 active라고 생각하면 register access가 timeout날 수 있다. Generic PM callbacks와 bus-specific logic이 runtime state를 재설정하는 이유가 여기에 있다. **Software PM state와 physical power state의 divergence**는 즉시 crash보다 다음 idle/resume cycle에서 드러나기 쉬워 장시간 stress가 필요하다.

## CHAPTER 21 · Wakeup source reference는 suspend entry를 막는 logical veto다

Kernel subsystem은 처리 중인 event가 끝나기 전에 system이 sleep하면 안 되는 경우 wakeup-source accounting을 이용해 suspend를 지연시킬 수 있다. Reference를 놓치면 event 처리 중 sleep해 data loss가 생기고, release를 누락하면 system이 영원히 suspend하지 못한다. Wakeup source는 user-visible `화면 켜기` 같은 기능만 뜻하지 않고 **현재 progress가 system sleep과 양립할 수 없는 critical work**를 표시한다. Long-held source는 owner stack과 hold duration을 관측해야 한다.

## CHAPTER 22 · Suspend latency는 가장 느린 phase/device의 critical path로 결정된다

수백 device callback의 평균이 빠르더라도 한 network/storage driver가 queue drain에서 3초를 쓰면 전체 suspend latency가 그 경로에 지배된다. PM dependency 때문에 일부 callback은 직렬화되고 독립 device는 병렬화될 수 있으므로 총 시간은 단순 합도 평균도 아니다. Phase별 timestamp, device callback duration, async dependency wait를 trace해 critical path를 재구성해야 한다. Power-saving feature가 UX를 망가뜨리는지 판단하려면 **energy benefit과 transition tail latency**를 함께 측정한다.

## CHAPTER 23 · Resume latency는 cold hardware initialization과 firmware readiness에 크게 의존한다

Device가 power-gated되면 register restore만으로 끝나지 않고 firmware load, link training, calibration, network renegotiation이 필요할 수 있다. System은 resume 완료로 보이는데 user workload 첫 access가 hidden deferred initialization을 맞아 latency spike를 경험할 수도 있다. Driver가 일부 initialization을 asynchronous하게 미룬다면 readiness state를 API에 명확히 반영해야 한다. Resume benchmark는 `kernel resume end`뿐 아니라 **첫 실제 transaction이 정상 latency로 성공하는 시점**까지 측정해야 한다.

## CHAPTER 24 · Filesystem/storage suspend는 dirty writeback과 journal state를 먼저 안정화해야 한다

Storage controller를 suspend하기 전에 filesystem이 필요한 writeback, journal commit, flush를 완료하지 않았다면 later I/O가 이미 잠든 device를 필요로 하게 된다. 반대로 global sync를 무조건 과도하게 수행하면 suspend latency와 flash write amplification이 커진다. Storage stack의 freeze/suspend ordering은 upper filesystem state와 lower block/device PM dependency를 연결해야 한다. **Dirty data lifetime과 device power lifetime이 교차하는 지점**이므로 P81 writeback state를 함께 추적해야 한다.

## CHAPTER 25 · Network suspend는 packet queue와 remote protocol timeout을 동시에 고려한다

NIC queue를 drain해도 remote peer는 local system이 자는 동안 connection timeout/retransmission을 진행할 수 있다. Wake-on-LAN 같은 feature는 일부 packet pattern만 wakeup trigger로 처리하고 ordinary traffic은 버릴 수 있다. Resume 후 driver link-up과 protocol stack state가 회복될 때 stale carrier/queue state가 남으면 connection은 살아 있어 보이지만 실제 packet이 안 간다. Suspend transparency는 **local device state + remote time passage**를 같이 고려해야 한다.

## CHAPTER 26 · Clock/timekeeping suspension은 monotonic·boottime·wall clock 의미 차이를 드러낸다

Deep sleep 동안 CPU counter가 멈추거나 scheduler tick이 중단될 수 있어 elapsed time을 어느 clock domain에 반영할지 kernel timekeeping이 처리한다. Timeout을 suspend 시간을 포함해야 하는지 제외해야 하는지에 따라 MONOTONIC/BOOTTIME 계열 선택이 달라질 수 있다. Resume 직후 timer storm이나 immediate timeout이 발생하는 시스템은 clock semantics를 잘못 선택했을 가능성이 있다. Power transition은 **시간도 하나의 subsystem state**라는 사실을 드러낸다.

## CHAPTER 27 · Suspend/resume 중 hotplug·remove가 겹치면 device lifetime과 PM lifetime이 충돌한다

Device removal이 진행 중인데 PM core가 callback pointer를 호출하거나, suspend된 device를 resume하려는 순간 driver object가 teardown되면 use-after-free가 된다. Driver core와 PM core는 device registration/refcount를 통해 transition 동안 object lifetime을 보호해야 한다. External hotplug event는 suspend 중 queue되었다가 resume 후 처리될 수도 있다. **Device existence state와 power state를 동일 enum으로 합치면 안 된다.**

## CHAPTER 28 · Suspend failure observability는 마지막 callback 하나가 아니라 phase graph를 남겨야 한다

Hard hang이나 reboot가 suspend 중 발생하면 ordinary logs가 storage에 남지 않을 수 있다. `pm_trace`, pstore/ramoops, tracepoint, phase/device timestamp를 이용해 마지막으로 진입/완료한 callback을 복구할 수 있어야 한다. `마지막 로그가 GPU였다`는 사실이 GPU가 원인이라는 뜻은 아니며 그 다음 dependency에서 lockup했을 수도 있다. Evidence는 **phase entry/exit + dependency + wake event + error rollback**을 같은 generation ID로 연결해야 한다.

## CHAPTER 29 · Suspend fault injection은 모든 phase의 partial rollback을 시험해야 한다

Prepare, suspend, late, noirq 각 단계에서 artificial error/timeout을 주고 이미 transition된 device가 working state로 정확히 복구되는지 검증해야 한다. Wakeup event를 arm 직전/직후에 넣고 immediate-resume/lost-wakeup race도 시험한다. Runtime-suspended device, active device, hotplug pending, heavy dirty writeback 같은 initial state를 조합해야 평상시 happy path 밖의 오류가 나온다. System PM test는 한 번 `echo mem`이 성공하는지보다 **transition state-space를 고의로 흔드는 것**이 중요하다.

## CHAPTER 30 · Suspend/resume correctness는 system-wide phase ledger로 증명해야 한다

한 sleep cycle을 승인하려면 `new work를 언제 차단했는가`, `각 device의 dependency order는 무엇인가`, `in-flight DMA/I/O가 어디서 drain됐는가`, `wake source가 어느 순간 arm됐는가`, `noirq 전후 interrupt precondition은 무엇인가`, `runtime PM state와 physical state를 어떻게 재동기화했는가`, `failure 시 어느 phase까지 rollback했는가`를 답할 수 있어야 한다. 평균 전력 절감만 보고 이 ledger를 무시하면 rare resume failure가 필연적으로 남는다. **System sleep은 power feature가 아니라 전체 kernel state를 quiescent generation으로 이동했다가 복원하는 transaction**이다.
