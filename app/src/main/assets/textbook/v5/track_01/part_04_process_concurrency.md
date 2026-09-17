# PART 04 · 프로세스와 동시성 — state ownership, ordering, progress

동시성의 핵심 문제는 “여러 일을 동시에 한다”가 아니라 **누가 어떤 상태를 소유하고, 어떤 순서가 보장되며, 경쟁이 생겨도 시스템이 계속 진행하는가**다. process·thread·scheduler·synchronization을 이 세 질문으로 연결한다.

---

## CHAPTER 01 · process는 실행 상태와 보호 경계를 함께 가진다

process는 executable file의 복사본이 아니다. virtual address space, credentials, open resource, signal state, thread 집합, scheduling/accounting 정보를 묶은 kernel 관리 단위다. 같은 program image에서 시작한 두 process는 code page를 공유할 수 있어도 heap·stack·descriptor table·runtime state는 독립적으로 진화한다.

process isolation의 목적은 주소 충돌을 피하는 것 이상이다. 다른 process가 임의의 memory를 읽거나 쓰지 못하게 하고, resource access를 credentials와 kernel policy로 통제한다. crash가 process boundary를 넘어 직접 memory corruption으로 전파되지 않는 것도 이 격리의 결과다. IPC는 이 보호 경계를 의도적으로 통과하는 계약이다.

---

## CHAPTER 02 · fork와 exec는 address space 복제와 program replacement를 분리한다

Unix 계열의 `fork()`는 호출 시점의 process state를 기반으로 child를 만들고, memory page는 copy-on-write로 공유될 수 있다. parent와 child는 동일 virtual content에서 시작해도 write 이후 독립 state로 갈라진다. open file description처럼 kernel object는 reference를 공유할 수 있어 file offset·lock semantics에 영향을 줄 수 있다.

`exec` 계열은 현재 process identity의 많은 kernel state를 유지하면서 address space를 새 program image로 교체한다. 따라서 `fork + exec`는 “process 하나 더 만들고 그 안에서 다른 프로그램을 시작한다”는 조합이다. descriptor close-on-exec 설정을 놓치면 child program이 의도하지 않은 file/socket handle을 상속해 resource leak이나 privilege boundary 문제를 만들 수 있다.

---

## CHAPTER 03 · thread는 shared memory 위의 독립 execution context다

같은 process의 thread는 address space와 대부분의 process resource를 공유하지만 각자 register state와 stack, scheduling state를 갖는다. 함수 local variable이 stack에 있다는 이유만으로 항상 thread-safe한 것은 아니다. local이 가리키는 object가 shared heap에 있거나 callback을 통해 다른 thread로 전달될 수 있다.

thread 간 communication이 shared memory load/store로 가능하다는 점은 비용을 낮추지만 correctness burden을 높인다. 한 thread가 만든 object를 다른 thread가 읽는 순간부터 **publication, visibility, lifetime** 계약이 필요하다. data structure가 논리적으로 immutable이어도 안전하게 publish되지 않으면 다른 thread가 초기화 중간 상태를 관찰할 수 있는 언어/runtime가 존재한다.

---

## CHAPTER 04 · scheduler는 CPU 시간을 배분하고 latency 분포를 만든다

runnable thread가 CPU core보다 많으면 scheduler가 실행 시간을 분할한다. thread가 실행되지 않는 이유는 `blocked` 하나가 아니다. sleep, I/O wait, lock wait, timer, runnable queue 대기처럼 상태가 다르다. wall-clock latency를 분석할 때 on-CPU time과 runnable-but-not-running 시간을 분리해야 한다.

priority, affinity, cgroup/resource policy, CPU topology가 scheduling 결과에 영향을 줄 수 있다. context switch 자체의 비용보다 더 큰 문제는 cache/TLB working set이 바뀌는 간접 비용일 수 있다. thread 수를 늘려 throughput이 떨어질 때 scheduler queue, cache locality, lock contention을 함께 측정한다.

---

## CHAPTER 05 · atomicity는 operation 경계를 정의한다

`x++`처럼 source에서 한 줄인 표현이 read-modify-write 여러 단계로 실행될 수 있다. 두 thread가 같은 old value를 읽고 각각 새 값을 쓰면 update 하나가 사라질 수 있다. atomicity는 source line의 모양이 아니라 **관찰자에게 하나의 indivisible state transition처럼 보이는 범위**를 뜻한다.

atomic variable 하나를 사용한다고 여러 field의 invariant가 자동으로 원자적이 되지 않는다. `balance`와 `version` 두 atomic 값을 별도 갱신하면 reader가 서로 다른 transaction의 조합을 볼 수 있다. invariant가 여러 값에 걸치면 lock, immutable snapshot, combined atomic state, transaction 같은 더 큰 synchronization boundary가 필요하다.

---

## CHAPTER 06 · mutex의 목적은 critical section의 불변조건 보호다

mutex를 붙일 위치는 “공유 변수를 쓰는 모든 줄”이 아니라 **함께 원자적으로 유지되어야 하는 invariant 경계**에서 결정한다. lock을 너무 작게 쪼개면 invariant가 중간 상태로 노출되고, 너무 크게 잡으면 unrelated work까지 serialize되어 contention이 커진다.

critical section 안에서 blocking I/O, long computation, callback을 실행하면 lock hold time이 예측 불가능해진다. callback이 같은 lock을 재진입하거나 다른 lock을 획득하면 deadlock graph가 복잡해진다. lock 설계에서는 owner, protected state, acquisition order, 최대 hold time, blocking call 허용 여부를 문서화한다.

검증에서는 lock 존재 여부보다 invariant가 깨지는 모든 경로가 같은 보호 규칙을 따르는지 확인한다. fast path 하나가 lock을 우회하거나 error path가 중간 상태를 남기면 정상 경로의 mutex는 충분하지 않다. lock contention trace에는 acquisition 시각, owner, hold duration, call site를 남겨 장기 보유가 실제로 어느 작업에서 발생했는지 연결한다. 이렇게 해야 coarse lock을 무작정 쪼개는 대신 보호 범위를 유지하면서 병목 구간만 밖으로 이동할 수 있다.

---

## CHAPTER 07 · condition variable은 event flag가 아니라 predicate wait를 구현한다

condition wait는 “signal이 오면 계속 실행”으로 설계하면 race가 생긴다. 올바른 모델은 shared state predicate를 mutex 아래에서 검사하고, predicate가 false인 동안 wait하며, 깨어난 뒤 다시 검사하는 것이다.

```text
lock
while (!predicate):
    wait(condition, lock)
consume state
unlock
```

wake-up과 실제 scheduling 사이에 다른 thread가 state를 바꿀 수 있고 spurious wakeup을 허용하는 API도 있다. 따라서 signal count 자체를 업무 상태로 사용하지 않는다. queue가 비어 있지 않다는 predicate, shutdown flag, capacity available 같은 **실제 state condition**이 기준이다.

lost wakeup을 피하려면 predicate 변경과 wait 등록 사이의 원자적 관계를 API가 어떻게 보장하는지 이해해야 한다. producer가 state를 바꾼 뒤 signal했는데 consumer가 아직 wait queue에 들어가기 전이었다면, consumer는 signal 자체가 아니라 이미 true가 된 predicate를 보고 진행해야 한다. 반대로 여러 consumer가 한 signal에 경쟁할 수 있으므로 깨어난 thread가 조건을 소비한 뒤 나머지가 다시 잠드는 동작도 정상이다. 이 때문에 signal 기록과 업무 상태를 별도 ledger로 취급한다.

---

## CHAPTER 08 · logical race와 data race를 구분한다

data race는 memory model이 정의하는 unsynchronized conflicting access 문제이고, logical race는 개별 access가 thread-safe해도 operation 순서 때문에 업무 결과가 틀리는 문제다. concurrent map의 `contains(key)`와 `put(key)`가 각각 thread-safe여도 둘 사이에 다른 thread가 끼어들 수 있다.

check-then-act, read-modify-write, reserve-then-commit은 하나의 atomic operation이 필요한 대표 구조다. API가 제공하는 `putIfAbsent`, transaction, compare-and-set 같은 compound primitive를 사용하거나 lock으로 전체 invariant를 묶는다. “collection이 concurrent니까 로직도 안전하다”는 결론은 성립하지 않는다.

---

## CHAPTER 09 · deadlock은 wait-for graph의 cycle이다

두 execution context가 서로 상대가 가진 resource를 기다리면 progress가 멈춘다. lock A→B와 B→A처럼 acquisition order가 뒤집히는 패턴이 대표적이지만 thread join, bounded queue, transaction lock, IPC response wait도 wait graph edge가 될 수 있다.

예방은 global lock order, lock hierarchy, try-lock+rollback, timeout, ownership redesign으로 접근한다. timeout은 deadlock을 제거하는 것이 아니라 무한 wait를 bounded failure로 바꾸는 경우가 많다. livelock은 서로 양보하면서 state가 변하지만 완료하지 못하는 문제이고 starvation은 일부 participant가 계속 기회를 얻지 못하는 문제다. 세 문제의 progress property가 다르다.

---

## CHAPTER 10 · memory model은 compiler와 CPU가 허용받은 재배치 범위를 정한다

single-thread semantics를 보존하는 transformation이 multi-thread observer에게는 다른 순서처럼 보일 수 있다. compiler reordering, store buffer, cache coherence, out-of-order execution을 모두 개별 하드웨어 현상으로 외우기보다 language memory model이 어떤 synchronization 없이 무엇을 보장하지 않는지부터 본다.

`volatile`/atomic acquire-release, lock/unlock 같은 operation은 특정 ordering과 visibility relation을 만든다. plain variable의 source order가 다른 core에서 같은 순서로 관찰된다고 가정하면 안 된다. low-level lock-free code는 atomicity뿐 아니라 memory ordering을 맞춰야 하며, 지나치게 약한 ordering은 희귀한 production-only bug를 만들 수 있다.

---

## CHAPTER 11 · visibility는 happens-before relation으로 증명한다

writer가 object field를 모두 설정한 뒤 reference를 전달했다고 해도 reader가 그 초기화를 볼 수 있는 synchronization relation이 필요할 수 있다. safe publication은 lock release→acquire, atomic release→acquire, thread start/join 등 language/runtime이 정의한 happens-before edge를 이용한다.

visibility bug는 값이 “늦게 전파되는 cache 문제”라는 표현만으로 충분하지 않다. compiler와 runtime도 관여하므로 **해당 언어 memory model에서 두 access 사이에 어떤 happens-before가 존재하는지**를 확인한다. 없으면 timing delay를 추가해 증상을 줄이는 것은 수정이 아니다.

증명은 source line 순서가 아니라 synchronization graph로 한다. writer의 initialization write에서 release edge로, reader의 acquire edge를 거쳐 실제 read까지 경로가 이어지는지 확인하면 된다. immutable object도 reference publication이 안전하지 않으면 생성 직후의 field visibility가 계약되지 않을 수 있다. 반대로 올바른 publication 뒤에는 매 field마다 별도 fence를 추가할 필요가 없다. 이 graph를 test와 code review에 명시하면 우연한 scheduler 순서에 의존한 수정과 실제 memory-ordering 수정을 구분할 수 있다.

---

## CHAPTER 12 · IPC는 serialization, ownership, failure boundary를 추가한다

pipe, socket, shared memory, message queue, Binder는 모두 process boundary를 넘지만 semantics가 다르다. message 기반 IPC는 data를 serialize/copy하거나 kernel buffer를 통해 전달하며, shared memory는 copy를 줄이는 대신 synchronization과 lifetime 계약을 직접 관리해야 한다.

IPC call은 local function보다 실패 모드가 많다. peer crash, timeout, partial write, protocol version mismatch, permission denial, backpressure가 존재한다. request ID, idempotency, deadline, cancellation, message size limit을 protocol 설계에 포함한다. local API처럼 보이는 Binder proxy도 remote process scheduling과 transaction limit의 영향을 받을 수 있다.

---

## CHAPTER 13 · non-blocking I/O는 readiness와 completion을 구분한다

non-blocking descriptor에서 read가 즉시 완료되지 않는다고 thread가 반드시 busy-wait해야 하는 것은 아니다. readiness API는 “현재 operation이 block 없이 진행될 가능성”을 알려 주고, completion 기반 API는 submitted operation의 완료 event를 전달하는 다른 모델을 사용한다.

partial read/write는 정상 동작일 수 있다. application message boundary와 transport byte stream boundary를 동일시하면 protocol framing이 깨진다. send buffer가 가득 찼을 때 계속 생산하면 memory queue가 무한히 늘 수 있으므로 high-water mark와 backpressure가 필요하다.

readiness notification을 받았다고 다음 호출이 반드시 원하는 전체 길이를 처리한다는 보장은 없다. 다른 consumer가 먼저 데이터를 소비하거나 edge-triggered API에서 drain 규칙을 어기면 event를 놓친 것처럼 보일 수 있다. completion 모델도 제출 성공과 실제 operation 성공을 구분해야 하며, completion queue가 포화되면 새 backpressure 지점이 생긴다. 따라서 descriptor state, pending operation 수, queue depth, short I/O 결과를 함께 기록해야 event engine의 stall을 재현할 수 있다.

---

## CHAPTER 14 · event loop의 correctness는 handler가 오래 점유하지 않는다는 가정에 의존한다

single event-loop thread는 shared mutable state를 한 thread에서 serialize해 lock 필요를 줄일 수 있다. 대신 한 handler가 CPU-bound work나 blocking I/O를 오래 수행하면 뒤의 모든 event latency가 증가한다. 따라서 event loop에는 **non-blocking handler budget**이라는 암묵적 scheduling 계약이 있다.

offload한 worker 결과가 돌아올 때 original request가 이미 timeout/cancel되었을 수 있다. event-loop architecture도 cancellation token, request generation, stale result suppression이 필요하다. queue depth와 loop lag를 측정하면 CPU utilization만으로 보이지 않는 event saturation을 찾을 수 있다.

---

## CHAPTER 15 · cancellation은 control signal이지 강제 중단 명령이 아니다

cooperative cancellation에서는 task가 cancellation state를 관찰하고 안전한 지점에서 cleanup 후 종료한다. cancellation이 lock hold 중, transaction 중간, partial file write 뒤 들어올 수 있으므로 cancellation-safe boundary를 설계해야 한다.

timeout은 caller의 기다림 종료와 underlying work 종료가 다를 수 있다. caller가 timeout됐는데 server/database 작업은 계속될 수 있다. deadline을 계층 전체로 전달하고, 취소되지 않는 작업은 결과 discard와 resource accounting을 설계한다. cleanup은 idempotent해야 중복 cancellation과 retry에서 안전하다.

취소 확인 지점이 너무 드물면 API는 취소 가능해 보여도 실제 자원 회수가 늦고, 너무 세밀하면 invariant를 유지하기 어려울 수 있다. 예를 들어 file replacement나 transaction commit의 특정 구간은 취소를 지연시켜 atomic boundary를 끝낸 뒤 결과를 폐기하는 편이 안전하다. 반대로 network fan-out처럼 독립 subtask가 많다면 parent cancellation을 즉시 전파해 downstream quota를 회수해야 한다. 취소 latency와 cleanup completion을 별도 metric으로 두면 “caller는 끝났는데 작업이 남는” 누수를 찾을 수 있다.

---

## CHAPTER 16 · Android main thread는 Looper/MessageQueue deadline을 가진다

Android UI process의 main thread는 framework callback과 message processing을 수행한다. main thread에서 network/file I/O나 긴 계산을 실행하면 event queue가 정체되고 input/render deadline을 놓친다. 문제는 “thread를 하나 더 만들면 된다”가 아니라 work의 ownership과 lifecycle을 맞추는 것이다.

background work가 Activity/Composable보다 오래 살아남으면 stale reference와 lifecycle bug가 생길 수 있다. UI state update는 main-safe boundary로 돌아와야 하고, screen이 사라졌을 때 결과를 적용할지 취소할지 정책이 필요하다. process death까지 고려하면 중요한 작업 상태는 memory callback chain에만 존재해서는 안 된다.

---

## CHAPTER 17 · thread pool은 concurrency limit이자 queueing system이다

worker 수가 많을수록 throughput이 계속 증가하지 않는다. CPU-bound workload는 runnable competition과 cache pressure가 커지고, I/O-bound workload도 downstream connection pool·DB pool보다 worker가 많으면 queue 위치만 이동한다.

pool 설계는 worker count, queue capacity, rejection/backpressure policy, task priority, deadline을 함께 정한다. unbounded queue는 overload를 memory growth와 tail latency로 숨긴다. bounded queue의 reject는 실패처럼 보이지만 시스템 전체를 보호하는 admission control이 될 수 있다.

pool tuning은 평균 service time만으로 결정하지 않는다. arrival burst, task duration distribution, downstream concurrency ceiling을 함께 놓고 active worker·queued task·rejection·queue wait를 같은 시간축에서 본다. worker가 모두 busy인데 CPU가 낮다면 blocking dependency가 원인일 수 있고, CPU가 포화인데 queue가 증가한다면 worker 추가가 오히려 context switch만 늘릴 수 있다. queue wait가 deadline 대부분을 소비하는 시점에는 실행 전에 expired task를 버리는 정책도 필요하다.

---

## CHAPTER 18 · contention은 평균 lock time보다 분포와 queue 길이로 본다

lock이 느린지 판단할 때 acquisition count만으로 부족하다. hold time, wait time, waiter count, owner scheduling, critical-section code path를 함께 본다. 짧은 lock이라도 매우 높은 frequency와 cache-line bouncing이 있으면 병목이 된다.

contention을 줄이는 방법은 lock implementation 변경만이 아니다. state partitioning, sharding, immutable snapshot, batching, message passing, read-copy-update, reducing shared writes가 더 큰 효과를 낼 수 있다. 먼저 어떤 invariant 때문에 공유가 필요한지 확인하고 공유 자체를 제거할 수 있는지 검토한다.

특히 tail wait는 lock holder의 on-CPU 시간보다 scheduler preemption이나 page fault 때문에 길어질 수 있다. owner가 lock을 쥔 채 deschedule되면 다른 waiter는 critical section 자체가 짧아도 긴 지연을 본다. 따라서 flame profile만으로는 충분하지 않고 lock event와 scheduler trace를 연결해야 한다. NUMA나 cache-line 이동이 많은 경우에는 wait-free 시간에도 coherence 비용이 증가할 수 있으므로 lock count, ownership migration, remote-memory traffic을 함께 비교한다.

---

## CHAPTER 19 · 동시성 버그는 timeline evidence가 필요하다

thread dump 한 장은 특정 순간의 wait state를 보여 주지만 race의 이전 순서를 설명하지 못할 수 있다. trace, lock event, request ID, sequence number를 결합해 timeline을 만든다. deadlock이면 wait-for graph를, starvation이면 runnable/ownership history를, race이면 conflicting access와 happens-before 부재를 증명한다.

재현 테스트는 sleep으로 timing을 맞추기보다 barrier/latch/fake scheduler로 interleaving을 통제한다. 같은 interleaving을 반복할 수 있어야 regression test가 된다. stress test는 발생 확률을 높이지만 원인을 증명하는 deterministic test와 역할이 다르다.

좋은 timeline은 단순 timestamp 목록이 아니라 causality edge를 포함한다. enqueue→dequeue, lock request→grant, request→response, publish→consume 관계를 ID와 sequence로 연결하면 host clock 오차가 있어도 사건 순서를 복원할 수 있다. 반대로 로그 시각만 정렬하면 서로 다른 core나 process의 clock 차이 때문에 원인과 결과가 뒤집혀 보일 수 있다. 재현 harness에서도 어떤 barrier가 어떤 interleaving을 강제했는지 기록해야 수정 후 동일한 경쟁 조건을 다시 검사할 수 있다.

---

## CHAPTER 20 · concurrency architecture는 shared state를 최소화하는 방향에서 시작한다

가장 안전한 synchronization primitive는 필요 없는 synchronization이다. state ownership을 한 execution context로 제한하거나 immutable value를 전달하거나 actor/message-passing으로 mutation 위치를 줄이면 possible interleaving 수가 크게 감소한다.

설계 문서에는 최소한 다음을 명시한다.

```text
state owner
mutation entry points
cross-thread/process transfer rule
ordering guarantee
cancellation/deadline rule
backpressure limit
failure/retry semantics
```

이 계약을 코드 리뷰 기준으로 사용하면 “lock이 있으니 안전하다”가 아니라 **불변조건과 progress가 실제로 증명되는가**를 검토할 수 있다.

ownership boundary가 바뀌는 지점은 API로 드러내는 편이 좋다. mutable object reference를 여러 worker에게 그대로 전달하는 대신 immutable command, snapshot, versioned handle처럼 transfer semantics가 명확한 표현을 사용하면 누가 언제 수정할 수 있는지가 줄어든다. 또한 shutdown 시에는 새 work admission 중지, in-flight drain 또는 cancel, resource close의 순서를 정해야 한다. 평상시 race가 없더라도 종료 경로가 이 순서를 깨면 use-after-close나 lost work가 생기므로 lifecycle 전체를 같은 concurrency contract로 검증한다.