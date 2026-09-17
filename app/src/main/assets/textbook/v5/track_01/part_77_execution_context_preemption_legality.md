# PART 77 · Execution Context and Preemption Legality — process, IRQ, NMI, atomic regions, PREEMPT_RT

Kernel code의 합법성은 함수 이름만으로 결정되지 않는다. 같은 함수라도 **process context인지, hard IRQ인지, softirq인지, NMI인지, preemption/interrupt가 disabled인지, 특정 lock을 잡고 있는지**에 따라 sleep·allocation·lock acquisition·CPU migration·userspace access의 허용 여부가 달라진다. 가장 위험한 버그는 데이터 값이 틀린 버그가 아니라 **현재 execution context가 허용하지 않는 연산을 호출해 scheduler·interrupt·locking invariant를 깨는 버그**다. 이 영역은 API 암기보다 context state와 전이 규칙을 정확히 추적해야 한다.

## CHAPTER 01 · Execution context는 call stack보다 강한 계약이다

함수 `f()`가 process context에서는 정상이어도 hard IRQ나 atomic region에서 호출되면 즉시 잘못된 코드가 될 수 있다. 이유는 함수가 요구하는 자원이 CPU를 양보하거나 다른 execution context의 progress를 필요로 할 수 있기 때문이다. Context contract를 읽을 때는 `누가 호출했는가`보다 **현재 CPU가 schedule될 수 있는가, interrupt가 들어올 수 있는가, 현재 thread identity가 의미 있는가, 같은 resource를 다른 context가 재진입할 수 있는가**를 봐야 한다. Call chain이 길어질수록 이 조건은 숨겨지므로 low-level API는 가능한 context를 문서와 annotation으로 명시해야 한다.

## CHAPTER 02 · preemption disable은 CPU 소유권을 잠시 고정하지만 scheduler를 멈추는 비용을 만든다

`preempt_disable()` 계열은 현재 task가 다른 task로 preempt되는 것을 막아 CPU-local state를 안정적으로 사용할 수 있게 하지만, 그 시간 동안 더 높은 priority task가 runnable이 되어도 즉시 실행하지 못한다. 따라서 critical section의 길이는 단순 성능 문제가 아니라 **scheduler response latency의 상한을 직접 늘리는 값**이다. Preemption-disabled 영역 안에서 page fault, blocking allocation, sleeping lock처럼 schedule을 요구하는 연산을 호출하면 context invariant가 깨진다. 이런 영역은 짧고 bounded해야 하며, 긴 계산을 넣는 대신 필요한 CPU-local state만 snapshot한 뒤 preemption을 다시 허용하는 설계가 낫다.

## CHAPTER 03 · preempt count는 단일 boolean이 아니라 nested execution state를 표현한다

Kernel은 preemption과 interrupt nesting을 추적하기 위해 단순 `disabled=true` 이상의 state를 유지한다. Lock helper, interrupt entry, softirq processing, explicit preempt-disable가 중첩될 수 있으므로 마지막 exit에서만 원래 schedulable state로 돌아가야 한다. 잘못된 enable/disable pairing은 당장 crash하지 않고 **나중에 전혀 관계없는 지점에서 `scheduling while atomic`이나 장시간 latency**로 나타날 수 있다. 따라서 instrumentation은 현재 preemptibility만 보지 말고 어떤 nesting path가 count를 올렸는지 stack과 함께 봐야 한다.

## CHAPTER 04 · interrupt disable은 preemption disable보다 더 강한 local progress 제한이다

Local IRQ disable은 현재 CPU에서 maskable interrupt delivery를 막는다. 이 상태가 길어지면 timer tick, device completion, network receive, scheduler-related interrupt 등 다른 subsystem의 progress가 함께 지연된다. 특히 현재 코드가 기다리는 completion을 같은 CPU interrupt가 만들어야 한다면 **IRQ를 끈 채 기다리는 순간 self-deadlock**이 된다. IRQ-off latency는 평균 실행시간이 아니라 최악 구간을 추적해야 하며, rare error path나 debug print가 임계구역을 길게 만드는지도 포함해야 한다.

## CHAPTER 05 · irqsave/irqrestore는 이전 interrupt state를 보존하는 nested protocol이다

`local_irq_save(flags)` 또는 `_irqsave` lock variant의 핵심은 단순히 interrupt를 끄는 것이 아니라 **호출 전 상태를 저장했다가 정확히 복원하는 것**이다. 이미 IRQ-off context에서 진입했는데 무조건 enable해버리면 outer caller의 invariant를 깨고, 반대로 restore 누락은 CPU를 장시간 interrupt-disabled 상태로 남긴다. `flags`는 다른 critical section과 섞어 재사용하면 안 되며, acquire/release 경로마다 동일한 state token을 사용해야 한다. Error return이 여러 개인 함수는 모든 exit가 restore를 거치는지 구조적으로 검증해야 한다.

## CHAPTER 06 · hard IRQ context에는 ordinary task identity와 sleep 권한이 없다

Hard interrupt handler는 현재 task를 중단하고 실행되지만 그 task의 정상 call flow 일부가 아니다. Handler가 sleep하면 scheduler가 `어떤 task가 무엇을 기다리는가`라는 정상 blocking 모델을 유지할 수 없고, interrupt source를 처리하지 못한 채 재진입 위험도 생긴다. 따라서 hard IRQ에서는 최소 상태를 acknowledge/capture하고, 오래 걸리거나 sleep이 필요한 처리는 threaded IRQ·workqueue·NAPI 같은 schedulable context로 넘겨야 한다. Handler에서 호출한 helper가 내부적으로 mutex나 reclaim allocation을 쓰는지까지 확인해야 한다.

## CHAPTER 07 · softirq는 hard IRQ보다 늦게 실행되지만 여전히 ordinary process context가 아니다

Softirq/BH 처리는 hard IRQ의 긴 작업을 미루기 위한 deferred context지만, 기본 non-RT 환경에서는 여전히 sleeping을 자유롭게 허용하는 thread context가 아니다. 한 번에 너무 많은 softirq work를 처리하면 user task latency가 폭증하고, budget을 넘으면 `ksoftirqd`로 밀려 scheduling delay가 커질 수 있다. 따라서 softirq-safe data structure는 process-context locking 규칙과 다를 수 있으며, 같은 object를 process와 softirq가 공유하면 `_bh` locking이나 명확한 ownership handoff가 필요하다. `interrupt에서 안 하니까 안전하다`는 판단은 insufficient하다.

## CHAPTER 08 · threaded IRQ는 interrupt work를 scheduler의 통제 아래 옮긴다

Threaded IRQ는 primary hard-IRQ handler가 source를 최소한으로 다룬 뒤 전용 thread를 깨워 나머지 처리를 process-like context에서 수행한다. 이렇게 하면 scheduler priority와 affinity로 latency를 제어할 수 있고 sleep 가능한 API 사용 범위도 넓어진다. 하지만 interrupt source masking/unmasking과 thread wakeup 사이 ordering이 잘못되면 interrupt loss 또는 storm이 발생한다. PREEMPT_RT에서는 많은 interrupt가 강제로 threaded되어 non-RT kernel에서 암묵적으로 맞던 spin/atomic assumptions가 바뀔 수 있으므로 configuration별 context를 실제로 확인해야 한다.

## CHAPTER 09 · NMI context는 hard IRQ보다도 dependency budget이 작다

NMI는 ordinary interrupt masking을 우회할 수 있으므로 lockup detection과 low-level diagnostics에 유용하지만, 그만큼 이미 같은 CPU가 어떤 lock을 잡은 상태인지 예측하기 어렵다. NMI handler가 ordinary spinlock, allocator, console lock 등 interrupted context가 보유할 수 있는 자원을 기다리면 deadlock 가능성이 매우 높다. NMI-safe code는 preallocated/per-CPU storage, lock-free or specially designed atomic path, bounded execution을 요구한다. `interrupt-safe`라는 라벨이 자동으로 `NMI-safe`를 의미하지 않는다.

## CHAPTER 10 · process context의 핵심 특권은 blocking을 scheduler에 표현할 수 있다는 점이다

Ordinary task context에서는 mutex, waitqueue, blocking I/O처럼 현재 task를 wait state로 만들고 CPU를 다른 runnable task에 넘길 수 있다. 이 모델이 성립하려면 기다리는 조건을 만드는 producer가 현재 task와 독립적으로 실행 가능해야 하고, lock ordering이 그 producer를 막지 않아야 한다. `sleep 가능`은 `아무 데서나 오래 기다려도 된다`는 뜻이 아니다. Deadline·signal interruption·cancellation·shutdown을 포함해 wait lifetime과 wake condition을 명시해야 한다.

## CHAPTER 11 · atomic context의 본질은 “원자적 연산”이 아니라 schedule 불가능성이다

Kernel에서 `atomic context`라는 표현은 CPU instruction 하나가 atomic하다는 뜻과 다르다. 보통 preemption/IRQ가 disabled되었거나 non-sleeping lock을 잡아 **현재 코드가 schedule out되면 안 되는 상태**를 가리킨다. 이때 sleeping allocation, blocking wait, mutex acquisition은 불법이 될 수 있다. API 리뷰에서는 함수 이름에 `atomic`이 있는지보다 호출 시점의 context와 내부 호출이 schedule point를 만들 수 있는지를 추적해야 한다.

## CHAPTER 12 · spinlock은 mutual exclusion과 preemption/CPU behavior를 함께 바꾼다

Non-RT kernel의 `spinlock_t`는 짧은 critical section을 위해 busy-wait하고, 보통 lock을 보유하는 동안 preemption behavior도 제한한다. Lock owner가 sleep하면 waiter가 CPU를 태우며 영원히 progress하지 못할 수 있으므로 sleeping operation을 critical section에 넣으면 안 된다. 또한 process와 IRQ가 같은 lock을 공유하면 local IRQ가 lock owner로 재진입하지 못하도록 `_irqsave` variant 같은 추가 protocol이 필요하다. Lock type 선택은 data race만 막는 문제가 아니라 **어떤 context에서 누가 owner가 될 수 있는가**를 결정하는 문제다.

## CHAPTER 13 · PREEMPT_RT의 spinlock_t는 non-RT와 같은 latency semantics가 아니다

PREEMPT_RT는 많은 `spinlock_t`를 priority-inheritance 가능한 sleeping rtmutex 기반으로 바꾸어 kernel preemptibility를 높인다. 이때 lock acquisition 중 task가 schedule될 수 있고, 전통적으로 `spinlock을 잡으면 preemption이 꺼진다`는 가정이 성립하지 않는다. 반면 `raw_spinlock_t`는 low-level genuinely atomic context를 위해 non-sleeping semantics를 유지한다. Cross-configuration code는 lock 이름만 보고 timing/atomicity를 추정하지 말고 RT 변환 규칙과 호출 가능한 API를 확인해야 한다.

## CHAPTER 14 · raw_spinlock_t는 정말 schedule 불가능해야 하는 작은 하드 경계를 표시한다

`raw_spinlock_t`는 PREEMPT_RT에서도 sleeping lock으로 변환되지 않는 low-level primitive다. Interrupt controller, scheduler 내부, architecture entry code처럼 scheduler 자체를 이용할 수 없는 영역에 필요하지만, 사용 범위를 넓히면 RT latency를 직접 악화시킨다. Raw lock 안에서 수행하는 작업은 bounded하고 architecture-independent side effect까지 포함해 짧아야 한다. `RT에서 느리니까 raw로 바꾸자`는 수정은 correctness와 latency model을 동시에 망칠 수 있다.

## CHAPTER 15 · mutex는 sleep 가능한 대신 owner progress와 priority relation을 고려해야 한다

Mutex contention은 waiter를 sleep시켜 CPU waste를 줄이지만, lock owner가 낮은 priority에서 밀리면 높은 priority waiter가 간접적으로 오래 block될 수 있다. RT mutex나 PI mechanism은 owner priority를 올려 inversion을 완화하지만 dependency chain이 길수록 propagation cost와 복잡도가 커진다. Mutex를 잡은 상태에서 외부 I/O, callback, unknown code를 호출하면 critical section upper bound를 잃는다. 따라서 lock 보호 범위를 data invariant에 필요한 최소 상태변경으로 제한하고 slow work는 lock 밖으로 이동해야 한다.

## CHAPTER 16 · migration_disable은 preemption_disable과 다른 계약이다

CPU-local resource를 쓰기 위해 필요한 것이 `다른 task가 못 뛰게 하기`가 아니라 **현재 task가 다른 CPU로 이동하지 않게 하기**뿐인 경우가 있다. PREEMPT_RT의 sleeping spinlock처럼 task는 preempt될 수 있어도 같은 CPU에 다시 돌아와야 하는 경로에서는 migration disable이 더 적합할 수 있다. 하지만 migration-disabled task가 장시간 block하면 scheduler placement 자유도가 줄고 hotplug/CPU isolation과 충돌할 수 있다. 어떤 invariant가 CPU identity를 요구하는지 명확히 해야 과도한 preemption disable을 피할 수 있다.

## CHAPTER 17 · per-CPU data는 접근 순간의 CPU identity 안정성이 전제다

Per-CPU counter나 queue는 lock contention을 줄이지만, pointer를 얻은 뒤 task가 다른 CPU로 migration되면 **다른 CPU의 shard를 현재 CPU local이라고 착각**할 수 있다. Preemption/migration control 없이 raw per-CPU pointer를 장시간 보관하는 패턴은 위험하다. 필요한 값만 즉시 읽거나 update하고, CPU identity가 보장되는 scope를 작게 유지해야 한다. rseq 같은 userspace mechanism도 결국 같은 CPU-identity invariant를 명시적으로 관리한다.

## CHAPTER 18 · RCU read-side critical section은 reader progress와 quiescent-state 정의에 묶인다

RCU read-side는 ordinary mutex와 달리 reader가 매우 싸게 object를 참조하는 대신 updater가 grace period 뒤에 reclamation한다. Classic kernel RCU read-side에서 arbitrary blocking을 허용하면 updater가 기다리는 quiescent state가 오지 않아 reclamation progress가 깨질 수 있다. PREEMPT_RCU와 PREEMPT_RT의 세부 허용 범위는 다르지만, explicit sleep이 가능한지 여부를 implementation/configuration에 맞춰 판단해야 한다. Reader에서 호출하는 helper 하나가 sleep 가능해도 RCU lifetime proof 전체가 무너질 수 있다.

## CHAPTER 19 · SRCU는 sleep 가능한 reader를 위해 다른 tracking cost를 지불한다

Sleepable RCU는 read-side에서 blocking이 필요한 subsystem을 위해 별도 per-instance tracking을 사용한다. Reader 자유도가 늘어나는 대신 bookkeeping과 grace-period 비용이 일반 RCU와 다르고, 어떤 SRCU domain의 read lock을 들고 있는지 lifetime을 명시해야 한다. Classic RCU를 SRCU로 무작정 바꾸면 performance characteristic이 바뀌고, 반대로 SRCU가 필요한 path를 classic RCU로 단순화하면 sleep-in-reader bug가 생긴다. Primitive 선택은 API 취향이 아니라 reader execution-context requirement에서 출발해야 한다.

## CHAPTER 20 · memory allocation flag는 allocator가 기다릴 수 있는 context를 선언한다

Kernel allocation은 크기만 넘기는 API가 아니라 reclaim·I/O·filesystem recursion을 허용할지 `gfp_t` flag로 context constraint를 전달한다. Atomic/IRQ path에서 blocking reclaim을 허용하면 current context가 sleep할 수 없어 deadlock이나 warning이 발생한다. 반대로 항상 nonblocking allocation만 쓰면 memory pressure에서 불필요한 allocation failure를 만들 수 있다. Allocation site는 **현재 context가 reclaim을 기다릴 수 있는지, failure를 처리할 수 있는지, emergency reserve 사용이 정당한지**를 함께 설계해야 한다.

## CHAPTER 21 · waitqueue/completion은 sleep 가능한 context에서만 blocking primitive가 된다

`wait_for_completion()` 같은 API는 task를 sleep시키므로 hard IRQ, atomic region, IRQ-off, preemption-disabled context에서 호출할 수 없다. 특히 `_irqsave` lock을 잡은 채 completion을 기다리고 producer가 같은 IRQ/lock을 필요로 하면 textbook self-deadlock이 된다. Completion object lifetime도 waiter/waker가 끝날 때까지 유지되어야 하며 stack-allocated completion을 asynchronous producer가 늦게 참조하면 use-after-return이 된다. Wait primitive는 context legality와 lifetime proof를 같이 요구한다.

## CHAPTER 22 · `might_sleep()` 계열 경고는 symptom이 아니라 context proof 실패를 가리킨다

Debug configuration에서 kernel은 sleep 가능한 지점이 atomic context에서 호출되는지 검사해 warning을 낼 수 있다. 이런 warning을 `debug kernel이라 시끄럽다`며 무시하면 production에서 rare deadlock/latency spike로 바뀐다. Stack trace에서 실제 sleep point만 고치지 말고, **어느 caller가 preempt/IRQ/lock state를 들고 여기까지 왔는지**를 역추적해야 한다. Root cause는 보통 context contract가 함수 signature 밖에 숨겨진 구조다.

## CHAPTER 23 · static context analysis는 context precondition을 코드의 타입 비슷한 계약으로 만든다

최신 kernel tooling은 특정 lock/context가 active 또는 inactive여야 한다는 조건을 annotation으로 표현하고 compiler가 call graph에서 검사할 수 있다. 이는 runtime lockdep와 다른 장점을 가진다. 실행되지 않은 error path도 정적으로 검사할 수 있지만, inline/복잡한 control flow처럼 분석 한계도 있다. 중요한 효과는 `이 함수는 어떤 context에서 합법인가`라는 지식을 사람 머릿속 convention이 아니라 machine-checkable contract로 이동시키는 데 있다.

## CHAPTER 24 · lockdep은 lock 순서뿐 아니라 IRQ-context 사용 관계도 검증한다

어떤 lock이 process context와 IRQ context 양쪽에서 사용되는데 process side가 IRQ를 켠 채 획득하면, 같은 CPU interrupt가 들어와 동일 lock을 기다리는 재진입 deadlock이 가능하다. Lockdep은 observed locking class와 context relation을 이용해 이런 cycle을 경고할 수 있다. 따라서 warning의 두 lock만 순서를 뒤집는 식으로 고치지 말고 각 lock의 **가능한 acquisition context graph**를 다시 그려야 한다. Dynamic checker는 실제 실행 coverage에 의존하므로 stress/fault path도 포함해 돌려야 한다.

## CHAPTER 25 · priority inversion은 lock이 아니라 execution-context latency와 결합된다

높은 priority task가 낮은 priority lock owner를 기다릴 때 중간 priority work가 owner를 계속 preempt하면 inversion이 생긴다. PI mutex는 owner를 boost할 수 있지만 owner가 IRQ-off/raw-spin section, non-preemptible code, firmware call 안에 있으면 scheduler가 해결할 수 없다. 따라서 RT latency 분석은 mutex graph뿐 아니라 **preemption-disabled/IRQ-disabled 구간의 worst-case duration**을 같이 측정해야 한다. 높은 priority만 설정한다고 deterministic latency가 생기지 않는다.

## CHAPTER 26 · context latency는 평균이 아니라 longest non-preemptible chain으로 결정된다

평균 IRQ-off 2µs라도 드물게 20ms path가 존재하면 real-time response upper bound는 그 20ms에 지배된다. Critical path는 nested raw locks, interrupt masking, softirq backlog, scheduler-disabled region을 이어서 계산해야 한다. Trace에서는 각 구간의 duration과 caller를 기록하고 percentile만이 아니라 worst outlier를 release regression으로 추적한다. Performance optimization보다 먼저 **boundedness를 증명**하는 것이 latency-sensitive kernel path의 핵심이다.

## CHAPTER 27 · callback boundary는 execution context를 바꿀 수 있으므로 API 이름보다 호출 주체를 봐야 한다

Timer callback, workqueue item, IRQ thread, softirq, RCU callback은 모두 `나중에 호출되는 함수`처럼 보이지만 context legality가 서로 다르다. 동일 helper를 여러 callback에서 재사용하면 가장 강한 context constraint가 helper 전체를 지배할 수 있다. 안전한 설계는 callback entry에서 context-specific state를 짧게 처리하고 공통 slow path는 명시적 handoff로 process context에 보내는 것이다. Context transition이 코드 구조에 보이지 않으면 유지보수자가 sleep 가능한 helper를 잘못 끼워 넣기 쉽다.

## CHAPTER 28 · teardown synchronization은 현재 context와 target context의 dependency를 함께 봐야 한다

`synchronize_irq()`나 work flush처럼 `다른 execution context가 완전히 끝날 때까지 기다리는` API는 shutdown에 유용하지만, target handler가 현재 thread가 보유한 lock/resource를 필요로 하면 즉시 deadlock한다. IRQ가 disabled된 현재 CPU에서 같은 CPU interrupt completion을 기다리는 것도 같은 구조다. Teardown은 먼저 새 work 발생을 차단하고, producer가 필요한 locks를 풀고, in-flight work를 drain하고, 마지막에 resource를 free하는 순서를 가져야 한다. `free 전에 synchronize`라는 한 줄 규칙만으로는 부족하다.

## CHAPTER 29 · PREEMPT_RT는 동일 source code의 context semantics를 바꿀 수 있다

Forced-threaded interrupts, sleeping `spinlock_t`, local locks, allocator 내부 sleeping lock 때문에 non-RT에서 accidental하게 맞던 assumption이 RT에서 깨질 수 있다. 반대로 RT에서는 더 많은 path가 preemptible해져 latency가 줄지만 raw/entry path 같은 truly atomic region은 더 명확히 드러난다. Portable kernel code는 `#ifdef RT`를 곳곳에 흩뿌리는 대신 어떤 operation이 **sleep requirement, CPU-local requirement, hard-IRQ requirement**를 갖는지 primitive 수준에서 표현해야 한다. 두 configuration 모두에서 context checker와 latency trace를 검증해야 한다.

## CHAPTER 30 · execution-context correctness는 상태표로 증명해야 한다

Kernel helper 하나를 승인하려면 최소한 `process/hardirq/softirq/NMI 가능 여부`, `preemption 상태`, `IRQ 상태`, `migration 가능 여부`, `held lock`, `sleep 가능 여부`, `allocation policy`, `CPU-local dependency`, `teardown synchronization`을 표로 만들 수 있어야 한다. 호출경로별 state가 하나라도 모순되면 context bug다. 좋은 low-level API는 이 표를 작게 만들고, 나쁜 API는 내부에서 context를 암묵적으로 바꾸거나 caller에게 숨은 조건을 요구한다. **실행 context는 부가정보가 아니라 correctness proof의 일부**다.
