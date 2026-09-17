# PART 85 · PCIe Error Recovery — AER, DPC, isolation, reset, driver state reconstruction

PCIe error recovery는 `에러 로그를 찍고 reset한다`는 한 동작이 아니다. Link가 신뢰 가능한지, device MMIO가 접근 가능한지, DMA가 차단됐는지, 같은 hierarchy의 다른 functions가 어떤 recovery 요구를 냈는지에 따라 단계가 달라진다. Recovery 중 driver는 normal I/O를 멈춘 채 software ownership ledger를 유지하고, platform은 필요하면 faulty hierarchy를 격리하고 reset한 뒤 configuration과 device-private state를 다시 만든다. 핵심은 **오류 이전 generation의 DMA/interrupt/request가 reset 이후 새 generation에 섞이지 않도록 끊고, 모든 participant가 같은 recovery generation으로 수렴하게 하는 것**이다.

## CHAPTER 01 · PCIe error severity는 transaction failure와 link failure를 구분한다

Correctable error는 protocol/hardware가 기능 손실 없이 스스로 복구할 수 있어 software는 주로 telemetry와 rate monitoring을 담당한다. Uncorrectable nonfatal error는 특정 transaction이 실패할 수 있지만 link 자체는 계속 신뢰 가능할 수 있고, fatal error는 link/hierarchy가 더 이상 reliable하지 않다고 본다. 같은 AER interrupt라는 이유로 세 severity를 동일 reset policy로 처리하면 recoverable workload를 불필요하게 끊거나 fatal link에서 위험한 추가 I/O를 수행한다. Recovery decision은 **어느 scope의 state를 더 이상 신뢰할 수 없는가**에서 출발해야 한다.

## CHAPTER 02 · AER ownership은 firmware와 OS 중 하나가 명확히 가져야 한다

PCIe AER capability가 있어도 platform firmware가 error handling을 소유하는 시스템과 OS가 소유하는 시스템이 있다. Linux가 firmware와 동시에 같은 error status/reset을 다루면 duplicate clear, conflicting reset, lost diagnostics 같은 예측 불가능한 결과가 생길 수 있다. ACPI `_OSC` 같은 handoff가 OS에 control을 부여한 경우에만 Linux AER root driver가 정상 owner가 된다. Error recovery도 ordinary device control과 마찬가지로 **single authority가 state machine을 소유해야 하는 protocol**이다.

## CHAPTER 03 · Correctable error는 recovery보다 rate와 degradation signal이 중요하다

Correctable error는 해당 transaction이나 link 기능을 hardware가 복구했으므로 driver reset이 필요하지 않을 수 있다. 하지만 correctable error count가 급증하면 cable/link quality, signal integrity, thermal/power 문제의 early warning일 수 있고 error reporting 자체가 interrupt/log storm을 만들 수 있다. 운영에서는 개별 occurrence보다 rate, BDF/port, lane/link speed, firmware change와 상관해야 한다. `자동 복구됐다`는 사실은 **hardware health degradation이 없다는 의미가 아니다.**

## CHAPTER 04 · Error containment의 첫 목표는 wild DMA가 memory corruption으로 번지는 것을 막는 것이다

Fatal bus/link error에서 platform이 affected device/hierarchy를 isolate하거나 DPC로 downstream link를 차단하는 이유는 bad transaction을 계속 흘리지 않기 위해서다. Driver가 firmware hang 상태에서 stale descriptor를 처리하며 arbitrary DMA address를 만들 수 있다면 recovery보다 먼저 system memory를 손상시킬 수 있다. Isolation 이후 MMIO read가 all-ones처럼 invalid value를 반환하거나 write가 drop될 수 있으므로 driver는 정상 register semantics를 가정해서는 안 된다. **격리는 availability를 희생해 integrity blast radius를 줄이는 단계**다.

## CHAPTER 05 · error_detected는 hardware를 고치는 callback이 아니라 software quiesce synchronization point다

Platform이 `error_detected()`를 호출할 때 device는 이미 MMIO 접근 불가능하거나 channel이 frozen일 수 있다. Driver는 새 request submission을 차단하고 pending timers/work를 정리하며 upper layer에 backpressure/error를 준비하되, device register를 만져 recovery하려 해서는 안 된다. 이 callback은 sleep 가능한 task context일 수 있어 software state를 안정화하는 데 사용할 수 있지만 hierarchy의 다른 drivers도 recovery에 참여한다. **새 I/O를 멈추고 outstanding ownership을 동결하는 것**이 첫 책임이다.

## CHAPTER 06 · pci_channel_state는 driver가 어느 하드웨어 가정을 버려야 하는지 알려준다

`io_normal`, `io_frozen`, `io_perm_failure` 같은 channel state는 recovery API가 단순 errno 대신 device accessibility level을 전달하는 이유를 보여준다. Frozen 상태에서는 MMIO/DMA가 blocked되었다고 가정해 normal polling loop를 중단해야 하고, permanent failure에서는 recovery attempt보다 detach/upper-layer failure propagation으로 전환해야 한다. Driver가 state를 무시하고 동일 path를 타면 inaccessible register를 무한 polling하거나 reset 불가능 device에 request를 계속 쌓게 된다. State enum은 **허용 operation 집합을 결정하는 execution-context 계약**이다.

## CHAPTER 07 · Recovery result code는 driver 하나의 희망이 아니라 hierarchy 전체 reset 수준 결정에 참여한다

각 affected driver는 `RECOVERED`, `CAN_RECOVER`, `NEED_RESET`, `DISCONNECT` 같은 결과를 반환할 수 있고 platform은 hierarchy 전체의 결과를 합쳐 다음 단계로 이동한다. Multi-function card에서 한 function만 reset 필요를 선언해도 shared silicon 때문에 slot reset이 전체 functions에 영향을 줄 수 있다. 다른 function이 `CAN_RECOVER`라고 해도 가장 강한 recovery requirement가 우선될 수 있다. **Recovery scope는 driver instance가 아니라 shared failure domain의 최대 요구 수준**으로 결정된다.

## CHAPTER 08 · mmio_enabled는 normal service resume가 아니라 제한된 early diagnostic/recovery window다

Platform이 MMIO를 다시 허용해 `mmio_enabled()`를 호출하더라도 DMA와 ordinary request processing이 모두 안전하다는 뜻은 아니다. Driver는 register health를 확인하거나 diagnostic state를 읽고 device-local reset을 시도할 수 있지만 normal queue를 재가동해서는 안 된다. 다른 function이 reset을 요구하면 이후 slot reset이 여전히 발생할 수 있기 때문이다. Early recovery code는 **hardware 접근 가능성 확인과 final service publication을 분리**해야 한다.

## CHAPTER 09 · Fatal error에서는 driver가 CAN_RECOVER를 반환해도 reset requirement가 사라지지 않는다

Fatal AER는 link 자체가 unreliable하다고 분류되므로 software가 register 몇 개를 읽어 정상처럼 보여도 transport state를 그대로 신뢰할 수 없다. Linux AER flow에서는 fatal hierarchy에 reset이 필수 경로가 될 수 있으며 driver의 early result는 진단/준비에 영향을 줄 뿐 final reset을 취소하지 못한다. 이 원칙은 `지금 MMIO가 읽힌다`보다 **오류 severity가 정의한 trust boundary**를 우선한다. Recovery code가 transient successful read로 fatal classification을 덮어쓰면 stale link/device state를 재사용하게 된다.

## CHAPTER 10 · Link reset과 slot/device reset은 서로 다른 state를 초기화한다

PCIe link state machine만 깨졌다면 link reset으로 transport를 복구할 수 있지만 endpoint internal state까지 손상됐다면 function/slot/fundamental reset이 필요할 수 있다. Reset type마다 configuration, firmware, queues, SRAM contents가 얼마나 보존되는지 다르다. Driver가 `reset`이라는 단어 하나로 모든 state가 power-on default로 돌아왔다고 가정하면 partial reset 뒤 stale firmware/queue를 재사용할 수 있다. **Reset primitive마다 postcondition을 문서화하고 그 postcondition부터 software reconstruction을 시작**해야 한다.

## CHAPTER 11 · DPC는 downstream fault를 빠르게 containment해 hierarchy 손상을 제한한다

Downstream Port Containment이 동작하면 faulting downstream hierarchy의 link를 disable해 error propagation과 wild traffic를 차단한다. 그 순간 하위 devices는 MMIO로 접근 불가능해질 수 있고, recovery가 link를 다시 enable할 때까지 driver가 hardware를 만지면 안 된다. DPC는 reset mechanism과 같지 않고 **error propagation을 끊는 isolation mechanism**이다. Containment event를 본 driver는 device-dead와 device-temporarily-isolated를 구분하면서도 normal traffic는 즉시 차단해야 한다.

## CHAPTER 12 · slot_reset callback은 fresh hardware state를 driver software state에 다시 맞추는 단계다

Platform reset 뒤 `slot_reset()`이 호출되면 PCI config space와 interrupt/DMA capability를 다시 준비하고 device-specific firmware/queue를 재초기화해야 한다. Reset 직전 software request table에는 old hardware generation의 descriptors가 남아 있을 수 있으므로 그대로 queue head만 재program하면 duplicate completion 또는 stale DMA가 생긴다. Driver는 **hardware generation을 새로 시작하고 old in-flight work를 error/abort로 종결**한 뒤 새 descriptors만 publish해야 한다.

## CHAPTER 13 · pci_save_state/restore_state는 config-space reconstruction baseline을 관리한다

PCI core는 enumeration/suspend 등에서 configuration state를 저장하고 reset 후 restore할 수 있다. Driver가 probe 이후 BAR-related config, MSI/MSI-X, device-specific PCI config를 변경한다면 recovery에 적합한 saved state가 실제 current intended configuration을 반영하는지 확인해야 한다. Suspend 때 저장된 state가 later error recovery에 부적절한 rare case도 고려해야 한다. Saved config는 dump가 아니라 **reset 후 다시 만들고 싶은 known-good software intent snapshot**이다.

## CHAPTER 14 · Firmware reload는 reset recovery critical path가 될 수 있다

일부 device는 reset 후 microcode/firmware를 다시 load해야 기능을 회복한다. Firmware image lookup이 filesystem/userspace에 의존하는데 root/storage path가 같은 failed PCI hierarchy에 있다면 recovery dependency cycle이 생길 수 있다. 필요한 artifact를 memory에 유지하거나 independent path를 제공해야 할 수 있다. Error recovery는 정상 probe보다 dependency budget이 작으므로 **recovery path가 자신의 고장난 device에 필요한 resource를 다시 요청하지 않는지** 검토해야 한다.

## CHAPTER 15 · Multi-function device는 global reset work의 단일 owner를 정해야 한다

하나의 card에 여러 PCI functions와 driver instances가 있으면 slot reset은 공유 silicon을 동시에 초기화한다. 모든 function이 global firmware reset이나 shared bus setup을 독립적으로 수행하면 race와 double initialization이 발생한다. Function 0, shared core object, PF 같은 designated owner가 one-shot work를 수행하고 다른 functions는 synchronization barrier 뒤 local state를 복원하도록 설계해야 한다. **Physical device ownership과 PCI function ownership을 구분**해야 한다.

## CHAPTER 16 · Interrupt state는 error event부터 slot_reset 완료까지 정상 전달을 가정할 수 없다

Error handling 중 interrupt가 완전히 멈춘다고도, 계속 정상적으로 온다고도 보장하기 어려울 수 있다. Inaccessible device가 interrupt source를 acknowledge하지 못하면 shared IRQ storm이 생길 수 있고 platform이 source를 mask할 수 있다. Driver interrupt handler는 error generation state를 확인해 device register access를 피하고 `IRQ_NOTHANDLED` 또는 safe path로 빠져야 한다. Recovery flow가 interrupt availability를 필요로 하는 completion을 기다리지 않도록 설계해야 한다.

## CHAPTER 17 · DMA quiescence는 software queue stop보다 더 강한 hardware postcondition이다

Upper layer request submission을 막고 software descriptor list를 비웠어도 device가 bus mastering을 계속하며 old transaction을 발행할 수 있다. Isolation/IOMMU/reset가 실제 DMA를 차단했다는 보장 전에는 old mappings/pages를 재사용하면 안 된다. 특히 reset 직전 completion event가 유실될 수 있어 normal reference release path가 실행되지 않을 수 있다. Recovery는 **new submission stop→device/bus DMA quiesce→old mappings release→new generation mapping** 순서를 가져야 한다.

## CHAPTER 18 · IOMMU는 wild DMA blast radius를 줄이지만 stale mapping lifetime을 자동 정리하지 않는다

IOMMU domain이 device DMA를 허용된 pages로 제한하면 firmware bug가 arbitrary system memory를 덮는 위험을 줄일 수 있다. 그러나 reset 후 old IOVA mapping을 그대로 남겨두면 new device generation이 recycled descriptors를 통해 stale buffer에 접근할 수 있다. Driver recovery는 request ownership과 IOMMU mapping ledger를 함께 정리해야 한다. Protection hardware가 있다는 이유로 **mapping lifecycle correctness requirement가 사라지는 것은 아니다.**

## CHAPTER 19 · Permanent failure는 retry loop가 아니라 device removal semantics로 전환해야 한다

Platform이 hierarchy를 복구할 수 없거나 driver가 `DISCONNECT`를 반환하면 정상 I/O를 재시도하는 것은 availability가 아니라 resource leak와 hung task를 만든다. Pending requests를 상위 layer에 `-EIO` 등으로 종결하고 new open/submission을 거부하며 timer/work/IRQ/DMA references를 제거해야 한다. Hotplug replacement가 가능하다면 old instance가 완전히 detached된 뒤 새 enumeration generation을 시작해야 한다. **죽은 hardware를 느린 hardware처럼 취급하면 안 된다.**

## CHAPTER 20 · AER logging은 first-error evidence와 secondary cascade를 구분해야 한다

하나의 root cause가 upstream/downstream에서 여러 AER records를 연쇄 생성할 수 있다. 모든 message를 independent incident로 세면 alert storm이 되고, 마지막 log만 남기면 최초 bad TLP/device identity를 잃는다. Root port/RCEC source, requester ID, severity, uncorrectable/correctable status, first-error pointer와 timestamp를 같은 incident generation으로 묶어야 한다. Recovery 중 발생한 secondary error도 **original failure와 recovery failure**를 구분해 기록해야 한다.

## CHAPTER 21 · Correctable error rate limiting은 visibility를 유지하면서 log storm을 막아야 한다

Correctable errors는 기능을 즉시 깨뜨리지 않아 burst가 발생해도 device service가 계속될 수 있지만, per-event printk가 CPU/storage를 압도하면 관측 시스템이 새로운 장애를 만든다. Rate limit/aggregation을 적용하되 device/port별 count와 시간 추세를 잃지 않아야 predictive hardware maintenance에 사용할 수 있다. Log suppression은 error 제거가 아니라 **event cardinality를 metric으로 압축하는 것**이어야 한다.

## CHAPTER 22 · Recovery callback 자체가 실패하면 state machine은 더 강한 reset 또는 disconnect로 escalation한다

`mmio_enabled()`에서 device-local reset이 실패하거나 `slot_reset()`에서 firmware init이 실패하면 같은 단계를 무한 반복하면 안 된다. Driver result code는 platform이 next stronger recovery action 또는 permanent failure로 이동하게 한다. Error path에서도 locks, requests, reference counts가 일관된 상태여야 다음 stage가 안전하게 실행된다. Recovery function은 `성공하면 정상`만 구현하는 게 아니라 **실패 후 다음 recovery stage가 시작 가능한 checkpoint**를 반환해야 한다.

## CHAPTER 23 · Recovery와 system suspend가 겹치면 둘 중 하나가 hardware generation ownership을 가져야 한다

AER reset이 진행 중인데 PM core가 suspend callback을 호출하거나, suspended device에 fatal error가 보고되면 두 state machine이 configuration/reset/interrupt state를 동시에 바꿀 수 있다. PCI core/driver는 locking와 device state를 통해 concurrent transitions를 serialize하고, system sleep을 abort하거나 recovery 완료 뒤 다시 시도하는 policy를 가져야 한다. `둘 다 device를 멈추는 코드`라는 이유로 함께 실행해도 안전한 것이 아니다. **Reset generation owner를 하나만 허용**해야 한다.

## CHAPTER 24 · Hot-remove와 error recovery가 겹치면 object lifetime부터 해결해야 한다

Surprise removal은 AER/DPC error처럼 보일 수 있지만 device가 물리적으로 돌아오지 않을 수 있다. Recovery worker가 `pci_dev`와 driver-private data를 참조하는 동안 remove path가 free하면 use-after-free가 생긴다. 반대로 recovery가 module/device reference를 영원히 잡으면 remove가 hang한다. Device core reference와 recovery work cancellation/drain을 이용해 **hardware presence lifetime과 recovery task lifetime을 rendezvous**시켜야 한다.

## CHAPTER 25 · Virtualized PCI passthrough에서는 host recovery가 guest-visible device semantics를 바꾼다

VFIO/passthrough device가 AER/reset을 겪으면 host는 physical function을 복구하면서 guest에 virtual error/reset event를 전달해야 할 수 있다. Guest가 old DMA queue를 여전히 유효하다고 생각하는 동안 host가 IOMMU mapping을 바꾸면 state divergence가 생긴다. SR-IOV PF reset이 여러 VFs에 영향을 주면 tenant isolation과 reset scope도 함께 고려해야 한다. Physical recovery와 virtual device lifecycle은 **generation mapping protocol**로 연결되어야 한다.

## CHAPTER 26 · Recovery latency budget은 upper-layer timeout과 경쟁한다

Storage/network device가 수초 동안 AER recovery를 수행하면 filesystem, TCP, database, cluster health checker가 먼저 timeout해 failover를 시작할 수 있다. Device가 결국 돌아와도 upper layer는 이미 request를 취소하거나 connection을 재구성했을 수 있어 late completion을 그대로 전달하면 duplicate effect가 된다. Driver는 outstanding request generation과 timeout/cancel 상태를 확인해 completion을 reconcile해야 한다. **Hardware recovery time과 software retry/failover deadline을 같은 timeline에 설계**해야 한다.

## CHAPTER 27 · Error injection은 real hardware failure가 없어도 recovery graph를 실제로 실행시키는 필수 테스트다

PCIe AER injection facility를 사용하면 correctable/nonfatal/fatal error를 controlled environment에서 발생시켜 callbacks와 reset path를 검증할 수 있다. Test는 단순 `error_detected가 호출됐다`가 아니라 in-flight DMA, multi-function card, runtime suspend, hotplug work, heavy IRQ load를 조합해 state transitions를 흔들어야 한다. Recovery 이후 old request가 double-complete하지 않고 mapping/refcount가 baseline으로 돌아오는지도 확인한다. 최초 production AER가 **처음 실행되는 recovery path**가 되어서는 안 된다.

## CHAPTER 28 · Recovery observability는 callback stage와 device generation을 함께 기록해야 한다

Incident trace에는 AER source/severity, channel state, 각 driver return code, MMIO enable, reset type, slot_reset duration, firmware reload, resume 시점, outstanding request count를 동일 recovery ID로 묶어야 한다. 단순 dmesg 순서만으로는 parallel/multi-function callback의 causal graph를 복원하기 어렵다. Reset 횟수와 device generation을 counter로 넣으면 late IRQ/completion이 어느 generation에서 왔는지도 판정할 수 있다. **복구 성공 여부보다 어떤 state를 거쳐 성공했는지**가 재발 방지에 더 중요하다.

## CHAPTER 29 · Recovery 이후 health check는 register access 성공보다 end-to-end I/O를 검증해야 한다

Slot reset 뒤 PCI config와 MMIO가 정상이어도 firmware queue, DMA, interrupt, media/link path가 여전히 손상됐을 수 있다. Driver `resume()` 직전 또는 upper subsystem에서 bounded self-test/first transaction으로 actual data path가 정상인지 확인하는 것이 안전하다. 너무 무거운 test는 recovery latency를 늘리므로 device-specific minimum health check를 정의해야 한다. **Control plane alive와 data plane usable을 분리**해야 false recovery를 막을 수 있다.

## CHAPTER 30 · PCIe recovery correctness는 old generation 격리와 new generation reconstruction으로 증명해야 한다

한 error recovery를 승인하려면 `오류 severity가 어느 trust boundary를 깼는가`, `new I/O는 어디서 차단됐는가`, `old DMA/IRQ/request가 언제 quiesce됐는가`, `어떤 reset postcondition을 가정하는가`, `config/firmware/queue를 어떤 source of truth에서 재구성했는가`, `multi-function/guest/upper-layer timeout을 어떻게 동기화했는가`, `old generation object가 new generation에 late completion을 전달할 수 없는가`를 답해야 한다. **Recovery는 device를 다시 켜는 행위가 아니라 손상된 execution generation을 폐기하고 검증된 새 generation으로 갈아타는 transaction**이다.
