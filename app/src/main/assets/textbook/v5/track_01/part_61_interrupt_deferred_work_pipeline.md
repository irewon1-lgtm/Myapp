# PART 61 · Interrupt and Deferred Work Pipeline — hard IRQ, softirq, threaded IRQ, workqueue

Interrupt handling의 핵심은 `장치가 CPU를 깨운다`가 아니다. **누가 interrupt source를 acknowledge/mask하고, 최소한의 hard-IRQ work 뒤 어떤 execution context로 나머지를 넘기며, 그 deferred work가 sleep·priority·CPU locality·forward progress를 어떤 규칙으로 갖는지**가 실제 설계다.

## CHAPTER 01 · Interrupt controller와 device source는 서로 다른 state를 가진다

CPU가 interrupt vector를 받았다고 device가 event를 완전히 처리한 것은 아니다. Interrupt controller pending state와 device status register/queue state는 별도다. Handler는 어느 layer의 pending bit를 언제 clear/ack해야 재전송·loss 없이 다음 event를 받을 수 있는지 알아야 한다.

## CHAPTER 02 · Mask와 acknowledge는 의미가 다르다

Mask는 새 delivery를 막고, acknowledge는 현재 interrupt event를 받아들였음을 표시한다. 일부 controller/device는 mask+ack를 결합하거나 EOI가 따로 필요하다. 순서를 잘못 잡으면 level-triggered interrupt가 계속 재발하거나 edge event를 놓칠 수 있다.

## CHAPTER 03 · Edge-triggered와 level-triggered는 재검사 규칙이 다르다

Edge는 상태 변화 순간을 알리고 level은 조건이 유지되는 동안 asserted될 수 있다. Level source를 완전히 drain하지 않고 unmask하면 즉시 interrupt가 다시 들어올 수 있다. Edge source는 mask된 동안 추가 edge가 hardware에 어떻게 latched되는지 controller semantics를 확인해야 한다.

## CHAPTER 04 · Hard IRQ context는 latency budget이 매우 짧다

Hard interrupt handler가 오래 CPU를 점유하면 일반 task뿐 아니라 다른 interrupt 처리도 지연될 수 있다. Handler는 device state를 안정화하고 필요한 work를 기록한 뒤 가능한 한 빠르게 deferred context로 넘겨야 한다. Heavy parsing, blocking allocation, filesystem I/O를 hard IRQ에 넣는 것은 execution-context contract를 위반한다.

## CHAPTER 05 · IRQ-disabled 구간은 system-wide tail latency를 만든다

Local CPU에서 interrupt를 오래 disable하면 timer, network, device completion이 해당 CPU에 전달되지 못한다. 평균 execution time이 작아도 드문 긴 irq-off section은 p99 scheduling latency를 크게 만들 수 있다. IRQ latency 분석에는 irq handler 시간뿐 아니라 irq-disabled interval 자체를 추적해야 한다.

## CHAPTER 06 · Top half와 bottom half는 urgency와 flexibility를 분리한다

Top-half 성격의 hard IRQ는 source 안정화와 최소 bookkeeping을 하고, 나머지는 softirq/thread/workqueue 같은 deferred context로 넘긴다. 이 분리의 기준은 code size가 아니라 **즉시 해야 event를 잃지 않는 일과 나중에 해도 되는 일**이다.

## CHAPTER 07 · Softirq는 process context가 아니므로 sleep할 수 없다

Softirq/BH context는 blocking mutex나 sleep 가능한 allocation을 일반 process thread처럼 사용할 수 없다. 작업이 potentially blocking이면 threaded context로 넘겨야 한다. `나중에 실행되니까 sleep 가능`이라는 일반화는 틀린다.

## CHAPTER 08 · Softirq backlog는 ksoftirqd로 밀려날 수 있다

Softirq work가 많아져 inline 처리 budget을 넘기면 kernel thread가 나머지를 처리할 수 있다. 이때 같은 network workload도 interrupt/softirq와 schedulable kernel-thread execution 사이에서 latency profile이 달라진다. CPU saturation 시 ksoftirqd scheduling delay가 packet processing tail을 키울 수 있다.

## CHAPTER 09 · Threaded IRQ는 handler를 scheduler가 관리하는 thread context로 옮긴다

Threaded interrupt handler는 sleeping operation을 허용하고 scheduler priority/affinity를 적용할 수 있다. 그러나 wakeup과 scheduling latency가 추가된다. Real-time system에서는 hard IRQ 최소 handler와 high-priority IRQ thread를 조합해 interrupt work를 priority-aware scheduling domain으로 옮길 수 있다.

## CHAPTER 10 · IRQ affinity는 interrupt work의 CPU placement를 결정한다

어떤 CPU가 interrupt를 받는지는 cache locality, NUMA locality, application CPU budget에 영향을 준다. Device queue와 CPU affinity가 잘 맞으면 completion data와 consumer thread가 가까워질 수 있지만 모든 interrupts를 한 core에 몰면 saturation된다. Affinity는 throughput과 isolation을 함께 본다.

## CHAPTER 11 · MSI-X는 queue별 interrupt vector mapping을 가능하게 한다

Multi-queue device가 각 queue에 별도 MSI-X vector를 사용하면 interrupt를 여러 CPU에 분배할 수 있다. Queue count, vector count, CPU count가 다르면 mapping이 필요하다. Interrupt parallelism을 늘려도 shared device lock이나 single completion structure가 남아 있으면 scaling이 제한된다.

## CHAPTER 12 · Interrupt coalescing은 handler invocation 수를 줄이는 batching이다

Device가 completion 여러 개를 모아 한 번의 interrupt로 알리면 per-interrupt overhead를 줄일 수 있다. 대신 첫 completion이 batch를 기다리는 시간이 생긴다. Coalescing threshold/time은 throughput, CPU utilization, tail latency를 교환하는 control parameter다.

## CHAPTER 13 · NAPI는 network IRQ와 polling 사이의 hybrid deferred model이다

Network load가 높을 때 매 packet interrupt를 받는 대신 interrupt로 NAPI work를 schedule하고 poll budget 안에서 여러 packet을 처리한다. Queue를 충분히 drain하면 interrupt를 다시 enable한다. NAPI는 `polling이 interrupt보다 빠르다`가 아니라 **load에 따라 notification cost와 batch processing을 전환하는 protocol**이다.

## CHAPTER 14 · Workqueue는 sleep 가능한 deferred process context를 제공한다

Work item을 worker thread에서 실행하면 blocking mutex, sleepable allocation, I/O wait 같은 process-context operation을 사용할 수 있다. 대신 workqueue backlog와 worker availability가 latency를 결정한다. Hard IRQ에서 workqueue에 enqueue한 순간 device state ownership을 work item lifetime까지 안전하게 넘겨야 한다.

## CHAPTER 15 · Per-CPU workqueue는 locality를 얻지만 hot CPU backlog를 만들 수 있다

Work를 queue한 CPU와 같은 worker pool에서 실행하면 cache locality를 얻을 수 있다. 그러나 특정 CPU에 interrupts가 몰리면 deferred work도 같은 CPU에 쌓여 imbalance가 심해질 수 있다. Unbound workqueue는 더 넓은 CPU pool로 분산하는 대신 locality를 희생한다.

## CHAPTER 16 · Workqueue max_active는 concurrency bound이자 backpressure parameter다

동시에 실행 가능한 work가 너무 많으면 subsystem 내부 lock과 memory pressure가 커지고, 너무 적으면 independent work가 직렬화된다. max_active는 thread 수 설정이 아니라 해당 workqueue가 downstream resource에 가하는 concurrency를 제한하는 값이다.

## CHAPTER 17 · WQ_MEM_RECLAIM은 memory pressure에서도 forward progress를 예약한다

Memory reclaim path가 workqueue completion을 기다리는데 모든 workers가 memory allocation에서 막히면 deadlock이 생길 수 있다. Reclaim-sensitive workqueue는 reserved execution context 같은 forward-progress mechanism이 필요하다. Deferred work 설계에는 정상 부하뿐 아니라 low-memory execution graph를 포함해야 한다.

## CHAPTER 18 · Flush는 과거 queueing instance와 현재 requeue를 구분해야 한다

Work가 자기 자신을 다시 queue하거나 다른 thread가 동시에 enqueue할 수 있으므로 `flush가 끝났다`의 범위를 정의해야 한다. 특정 queueing instance가 끝난 것인지 work가 완전히 idle한 것인지 API contract를 확인해야 한다. Shutdown code가 이를 틀리게 이해하면 resource가 파괴된 뒤 work가 다시 실행될 수 있다.

## CHAPTER 19 · Cancel은 callback이 실행 중일 가능성을 별도로 다뤄야 한다

Pending work를 queue에서 제거하는 것과 이미 CPU에서 실행 중인 callback 종료를 기다리는 것은 다르다. Async cancel과 cancel_sync semantics를 구분하지 않으면 use-after-free가 생긴다. Resource teardown은 **new queue 금지→pending 제거→running drain→resource free** 순서를 가져야 한다.

## CHAPTER 20 · Timer callback도 execution context에 따라 허용 operation이 달라진다

High-resolution timer, timer softirq, delayed work는 모두 `시간 뒤 실행`처럼 보이지만 callback context가 다르다. Sleep이 필요한 timer-triggered work는 timer callback에서 직접 처리하지 말고 workqueue/thread로 넘겨야 할 수 있다. Delay mechanism과 execution mechanism을 분리해서 설계해야 한다.

## CHAPTER 21 · Deferred work queue는 unbounded producer에서 memory pressure가 된다

Interrupt/event producer가 consumer worker보다 빠르면 work item queue가 계속 늘어날 수 있다. 각 item이 buffer reference를 보유하면 backlog가 memory retention으로 변한다. Queue limit, coalescing, event deduplication, upstream masking 같은 backpressure policy가 필요하다.

## CHAPTER 22 · Duplicate work suppression은 state-machine correctness를 요구한다

같은 work item이 이미 pending일 때 `queue_work`가 중복 enqueue를 거부하는 semantics를 이용할 수 있지만, event count를 단순 boolean pending에 압축하면 여러 hardware event를 잃을 수 있다. Work callback이 실제 device queue를 완전히 drain하는지 또는 event counter를 별도로 유지하는지 invariant가 필요하다.

## CHAPTER 23 · CPU hotplug는 per-CPU deferred state의 ownership 이동 문제다

CPU가 offline될 때 그 CPU에 pinned된 work, timer, interrupt affinity, per-CPU queue를 다른 execution context로 옮기거나 drain해야 한다. `CPU는 항상 존재한다`는 가정이 work lifetime보다 짧아질 수 있다. Hotplug-safe subsystem은 queueing과 teardown을 동기화해야 한다.

## CHAPTER 24 · Suspend/resume은 interrupt masking과 pending work ordering을 바꾼다

Device suspend 중 interrupt source를 어떻게 mask하고 queued work를 drain할지, resume 뒤 hardware state와 software queue를 어떤 순서로 복구할지 명확해야 한다. Resume 직후 stale completion이 들어오면 이전 generation descriptor를 현재 request로 오인할 수 있다. Device generation/state validation이 필요하다.

## CHAPTER 25 · Interrupt storm은 source bug와 legitimate overload를 구분해야 한다

Handler가 source condition을 clear하지 못해 같은 level IRQ가 반복되는 경우와 실제 높은 event rate는 다른 문제다. IRQ count만 보면 구분하기 어렵다. Handler당 processed completions, queue depth, device status, mask/unmask frequency를 함께 관찰해야 한다.

## CHAPTER 26 · Lost interrupt는 polling/recovery path가 없으면 영구 stall이 된다

Hardware/driver race로 notification을 놓쳤는데 queue에 completion이 남으면 waiter가 영원히 잠들 수 있다. 일부 subsystem은 periodic poll, timeout, doorbell recheck 같은 recovery mechanism을 둔다. Event notification은 optimization일 수 있지만 **state 자체는 queue를 재검사해 복원 가능**해야 robust하다.

## CHAPTER 27 · IRQ handler와 worker 사이 memory ordering이 ownership handoff를 보장한다

Interrupt handler가 shared state를 update한 뒤 work를 queue하면 worker는 그 state를 일관되게 봐야 한다. Workqueue API가 제공하는 ordering guarantee와 driver lock/barrier를 조합해야 한다. CPU가 다르면 compiler/CPU reorder를 무시할 수 없다.

## CHAPTER 28 · Deferred-work latency는 enqueue→start와 execution을 분리해 측정한다

Work callback이 20µs여도 queue에서 5ms 기다렸다면 user-visible latency의 원인은 callback cost가 아니다. Enqueue timestamp, worker start, finish를 분리하고 worker pool saturation과 CPU scheduling delay를 함께 측정해야 한다. Queue depth만으로 latency를 추정하면 service-time distribution을 놓친다.

## CHAPTER 29 · Priority inversion은 deferred work에도 존재한다

High-priority task가 device completion을 기다리는데 completion work가 low-priority worker pool에 묶여 있으면 indirect priority inversion이 생긴다. IRQ thread priority, workqueue priority, CPU affinity, cgroup scheduling policy를 dependency chain으로 봐야 한다. Critical completion path를 generic background queue에 넣으면 priority model이 깨질 수 있다.

## CHAPTER 30 · Deferred execution은 context 계약과 forward progress를 설계하는 일이다

Interrupt pipeline을 안정적으로 만들려면 **ack/mask/EOI, hard-IRQ budget, softirq/thread/workqueue 선택, sleep 가능성, queue bound, cancellation/drain, CPU locality, priority, memory-order handoff, low-memory forward progress**를 한 protocol로 정의해야 한다. `나중에 처리한다`는 추상화만으로는 overload·shutdown·suspend·CPU hotplug에서 correctness를 지킬 수 없다.
