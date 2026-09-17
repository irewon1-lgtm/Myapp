# PART 63 · Device Driver Lifecycle — match, probe, dependencies, PM, reset, hot-unplug, teardown

Driver correctness는 read/write callback만으로 결정되지 않는다. Device는 **발견·match·probe·resource acquisition·dependency binding·runtime suspend/resume·error reset·hot unplug·remove**를 거치며, 각 전이마다 hardware와 software state의 ownership이 바뀐다. 실패 path가 정상 path보다 덜 설계되어 있으면 production 장애는 대부분 teardown에서 난다.

## CHAPTER 01 · Device object와 driver object는 독립적으로 먼저 존재할 수 있다

Bus가 device를 발견해 device object를 등록하는 시점과 해당 hardware를 지원하는 driver가 등록되는 시점은 다를 수 있다. Driver model은 나중에 두 객체를 match해 binding을 시도한다. `driver load=hardware 존재` 또는 `device enumerate=driver 준비 완료`라는 가정은 틀린다.

## CHAPTER 02 · Match는 지원 가능성을 고르고 probe는 실제 usable state를 검증한다

Bus match는 vendor/device ID, compatible string 같은 identity 기준으로 후보 driver를 선택한다. Probe는 실제 revision, resource, dependency, firmware state를 확인하고 driver-private state를 초기화한다. Match 성공 뒤 probe 실패는 정상적으로 가능한 lifecycle branch다.

## CHAPTER 03 · Probe는 partial initialization rollback을 전제로 설계해야 한다

Clock은 얻었지만 IRQ 등록 전에 실패하거나, DMA buffer를 만들었지만 device reset이 실패할 수 있다. 각 단계가 성공할 때마다 cleanup responsibility가 늘어난다. Probe error path는 획득 역순으로 resource를 해제하고 hardware를 안전 state로 돌려야 한다.

## CHAPTER 04 · Managed resource는 cleanup ordering을 자동화하지만 semantic dependency를 없애지 않는다

devres류 managed allocation은 device detach 시 resource release를 자동화해 error path를 단순화할 수 있다. 그러나 hardware interrupt를 끄기 전에 DMA buffer가 해제되면 안 되는 것처럼 semantic ordering이 필요한 resource는 여전히 driver가 lifecycle을 설계해야 한다. 자동 free와 safe teardown은 같은 개념이 아니다.

## CHAPTER 05 · Deferred probe는 dependency가 아직 준비되지 않았음을 표현한다

Regulator, clock, IOMMU, GPIO, firmware supplier가 아직 bind되지 않았으면 probe가 `나중에 다시 시도`를 요청할 수 있다. 이때 expensive initialization을 많이 수행한 뒤 defer하면 반복 rollback 비용이 커진다. Dependency 확인은 가능한 한 probe 초기에 끝내야 한다.

## CHAPTER 06 · Deferred probe는 child device 생성 뒤 사용하면 dependency loop를 만들 수 있다

Probe 중 child device를 등록한 뒤 다시 defer하면 parent probe 재시도와 child registration이 순환할 수 있다. Lifecycle state가 외부에 publish되기 전에 defer 가능성을 닫아야 한다. **visibility를 만든 뒤 rollback**은 단순 memory cleanup보다 훨씬 어려운 문제다.

## CHAPTER 07 · Device link는 supplier-consumer ordering을 명시한다

Consumer가 supplier resource에 의존하면 probe 순서뿐 아니라 remove, suspend/resume, runtime PM도 dependency order를 지켜야 한다. Device link는 이 관계를 driver core가 알게 해준다. Hidden dependency를 callback 내부 pointer만으로 유지하면 hot-unplug나 system suspend에서 ordering bug가 난다.

## CHAPTER 08 · sync_state는 bootloader state에서 kernel-owned state로 전환하는 경계다

Bootloader가 regulator/clock/IOMMU 등을 켜 둔 상태에서 kernel consumer들이 아직 probe되지 않았으면 supplier가 즉시 unused resource를 꺼서는 안 된다. 모든 known consumers가 bind된 뒤 software aggregate state에 맞춰 hardware를 정리하는 시점이 필요하다. 이 handoff는 boot-time ownership transfer다.

## CHAPTER 09 · Probe 성공은 user-visible service readiness와 같지 않을 수 있다

Driver binding이 끝나도 firmware download, calibration, link training, asynchronous initialization이 남을 수 있다. Device node가 존재한다는 사실만으로 first I/O가 성공한다고 가정하면 race가 난다. Ready state를 별도 state machine과 completion/event로 표현해야 한다.

## CHAPTER 10 · Firmware loading은 driver와 device code version을 연결한다

일부 hardware는 runtime firmware blob이 필요하다. Firmware version이 driver가 기대하는 command ABI와 맞지 않으면 probe는 성공해도 later I/O가 깨질 수 있다. Driver binary, device firmware, configuration을 하나의 compatibility set으로 release evidence에 기록해야 한다.

## CHAPTER 11 · Interrupt enable은 handler와 data structures가 완전히 준비된 뒤여야 한다

IRQ를 등록/enable한 순간 hardware event가 들어올 수 있다. Handler가 참조할 queue, lock, memory, device state가 아직 초기화 중이면 startup race가 생긴다. Probe ordering은 `resource allocate→state publish→interrupt enable` 같은 happens-before relation을 가져야 한다.

## CHAPTER 12 · DMA enable은 device에 memory ownership 권한을 부여한다

Bus mastering/DMA를 켜면 device가 host memory를 읽고 쓸 수 있다. 따라서 DMA descriptors와 IOMMU mapping, buffer lifetime이 완성되기 전에는 enable하면 안 된다. Teardown에서는 반대로 device DMA를 확실히 멈춘 뒤 mapping/buffer를 해제해야 한다.

## CHAPTER 13 · Runtime PM은 사용하지 않는 device를 suspend하지만 logical binding은 유지한다

Runtime suspend 상태에서도 driver는 device와 bound되어 있고 user request가 오면 resume할 수 있다. `suspended=removed`가 아니다. Driver state는 bound/unbound와 powered/unpowered를 독립 축으로 표현해야 한다.

## CHAPTER 14 · Runtime PM software state와 실제 hardware state는 초기에는 다를 수 있다

Boot firmware가 device를 active로 남겼는데 PM core는 초기 상태를 suspended로 가정할 수 있다. Probe는 실제 state를 확인해 runtime PM status를 맞추거나 device를 명시적으로 resume/suspend해야 한다. State bookkeeping과 physical power state 불일치는 double-disable, lost register state를 만든다.

## CHAPTER 15 · Autosuspend delay는 energy와 wake latency를 교환한다

I/O가 끝날 때마다 즉시 suspend하면 짧은 burst 사이에서 power transition이 반복되고 latency가 커진다. 너무 늦게 suspend하면 idle power가 낭비된다. Autosuspend policy는 request inter-arrival distribution과 resume cost를 근거로 해야 한다.

## CHAPTER 16 · System suspend는 runtime PM과 다른 global ordering을 갖는다

System sleep에서는 전체 device tree가 dependency order에 따라 quiesce되어야 한다. Runtime suspend된 device라도 system-suspend callback에서 additional platform state를 처리해야 할 수 있다. `runtime suspend 구현했으니 system suspend도 같다`는 가정은 위험하다.

## CHAPTER 17 · Resume은 register restore보다 dependency readiness가 먼저다

Device가 clock/regulator/parent bus/IOMMU에 의존하면 supplier가 먼저 active여야 consumer resume이 안전하다. Resume callback이 I/O를 재개하기 전에 link training, queue restore, interrupt state, firmware context가 모두 준비됐는지 확인해야 한다.

## CHAPTER 18 · Reset은 device를 초기 상태로 돌리지만 host state와 generation을 동기화해야 한다

Device reset 뒤 old descriptors, completion, firmware state가 사라질 수 있다. Host가 pre-reset outstanding request를 계속 current queue와 동일하게 취급하면 stale completion과 buffer ownership bug가 생긴다. Reset generation을 증가시키고 old work를 fail/retire해야 한다.

## CHAPTER 19 · Function-level reset과 bus-level reset은 blast radius가 다르다

개별 function reset으로 recovery할 수 있는 fault에 상위 bus reset을 사용하면 sibling devices까지 영향받을 수 있다. 반대로 local reset으로 shared controller state가 복구되지 않으면 반복 fault가 난다. Recovery escalation은 최소 범위부터 시작해 dependency graph를 고려해야 한다.

## CHAPTER 20 · Error recovery는 정상 I/O submission을 먼저 차단해야 한다

Reset/recovery 중 새 request가 들어오면 queue state와 hardware generation이 섞인다. Driver는 error state를 publish해 new I/O를 reject/queue하고, outstanding work를 drain/abort한 뒤 hardware reset과 reinit을 수행해야 한다. Recovery와 submission path의 serialization이 핵심이다.

## CHAPTER 21 · Hot-unplug은 hardware가 언제든 사라질 수 있다는 failure model이다

External bus 또는 virtualized device는 process가 사용 중일 때도 제거될 수 있다. MMIO read가 실패하고 DMA completion이 오지 않으며 IRQ가 사라질 수 있다. Driver API가 device pointer를 가진다고 physical hardware existence가 보장되는 것은 아니다.

## CHAPTER 22 · Remove는 user entry point를 닫고 새 reference 생성을 먼저 막아야 한다

Teardown 순서는 보통 user-visible interface unregister, new I/O 차단, callbacks/work drain, IRQ/DMA stop, hardware disable, memory/resource release처럼 **새 접근을 먼저 막고 기존 접근을 소진**하는 방향이어야 한다. Free부터 하면 race window가 생긴다.

## CHAPTER 23 · Reference counting은 callback lifetime과 device lifetime을 연결한다

Open file, async request, work item, timer가 device-private state를 참조할 수 있다. Remove callback이 끝났다고 모든 external reference가 사라졌다는 보장은 없다. Kref/RCU/flush 같은 lifetime mechanism으로 final free 시점을 증명해야 한다.

## CHAPTER 24 · Workqueue/timer cancellation은 remove path의 필수 단계다

Deferred callback이 remove 후 실행되면 freed state를 접근한다. `cancel`이 pending만 지우는지 running callback까지 기다리는지 API semantics를 구분해야 한다. Requeue 가능한 work는 new queue를 금지한 뒤 sync cancel해야 drain이 끝난다.

## CHAPTER 25 · IRQ synchronize는 handler가 완전히 빠져나왔는지 확인하는 barrier다

Interrupt source를 mask하고 free_irq를 호출하기 전에 다른 CPU에서 handler가 이미 실행 중일 수 있다. Handler completion과 resource free 사이에 synchronization point가 필요하다. IRQ line disable만으로 in-flight handler가 즉시 사라지지 않는다.

## CHAPTER 26 · DMA unmap 전에 device ownership을 회수해야 한다

Device가 descriptor를 아직 읽거나 buffer에 write할 가능성이 있는데 IOMMU mapping/page를 해제하면 memory corruption이 생긴다. Queue stop, device quiesce, completion drain/reset을 통해 DMA ownership이 host로 돌아왔음을 보장한 뒤 unmap해야 한다.

## CHAPTER 27 · Probe/remove 반복 시험은 정상 boot 한 번보다 더 많은 bug를 찾는다

Module load/unload, bind/unbind, hotplug 반복은 leaked resource, stale sysfs entry, double free, async callback lifetime bug를 드러낸다. Driver test는 `한 번 성공적으로 probe`가 아니라 lifecycle transition을 여러 번 순환해 state가 원점으로 돌아오는지 검증해야 한다.

## CHAPTER 28 · Fault injection은 probe의 모든 acquisition 단계에 적용해야 한다

N번째 allocation failure, IRQ request failure, firmware load timeout, PM resume error를 주입해 cleanup path를 확인하면 정상 환경에서 거의 실행되지 않는 rollback branch를 검증할 수 있다. 각 failure point 뒤 resource count와 hardware state가 probe 전과 동일해야 한다.

## CHAPTER 29 · Driver observability는 lifecycle generation을 포함해야 한다

같은 device가 reset/reprobe된 뒤 old async log가 늦게 도착하면 현재 generation event처럼 보일 수 있다. Device ID뿐 아니라 probe/reset generation, firmware version, queue generation을 log/trace에 포함하면 stale event를 구분할 수 있다.

## CHAPTER 30 · Driver는 hardware 기능보다 state transition을 증명해야 한다

Robust driver의 기준은 benchmark I/O 성공이 아니라 **match/probe rollback, dependency order, runtime/system PM, reset recovery, hot-unplug, async drain, IRQ/DMA ownership, final free**가 모든 interleaving에서 안전한지다. Hardware를 제어하는 code보다 hardware가 사라지고 실패하고 다시 돌아오는 lifecycle protocol이 더 어려운 부분이다.
