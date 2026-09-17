# PART 25 · Async I/O Engines — readiness, completion, io_uring, backpressure

비동기 I/O는 thread를 없애는 기술이 아니라 **submission, readiness/completion, buffer ownership, timeout, cancellation, backpressure**를 분리하는 실행 모델이다. API가 달라도 request가 언제 시작되고, 누가 resource를 소유하며, 어떤 event가 완료를 뜻하는지 명확하지 않으면 race와 overload가 생긴다.

---

## CHAPTER 01 · blocking과 async는 기다림을 어디에 둘지 결정한다

blocking I/O에서는 호출 thread가 operation 진행을 기다리고, async 모델에서는 request를 제출한 뒤 completion을 다른 event에서 받는다. 둘의 핵심 차이는 syscall 수가 아니라 **대기 상태를 thread stack에 둘지 explicit request state에 둘지**다. async request에는 buffer, descriptor, deadline, correlation ID가 completion까지 살아 있어야 한다.

비동기화가 service time 자체를 줄이지는 않는다. storage나 network가 이미 saturation이면 더 많은 in-flight request가 queue wait만 늘릴 수 있다. blocking thread shortage를 해결하면서 downstream overload를 악화시키는 경우도 있다.

진단에서는 submission→queue→device/network→completion→callback timestamp를 연결한다. thread count 감소만 보지 않고 in-flight 수, queue age, p99 latency를 함께 측정한다. cancellation 후 resource가 실제로 회수되는지도 같은 lifecycle에서 확인한다.

---

## CHAPTER 02 · readiness는 operation 완료와 다른 신호다

readiness API는 descriptor가 현재 block 없이 어느 정도 진행될 가능성을 알려 준다. readable event가 왔다고 application message 하나가 완전히 준비됐다는 뜻은 아니며, writable event도 원하는 전체 payload를 한 번에 쓸 수 있다는 보장이 아니다. transport byte stream과 application framing을 분리해야 한다.

여러 consumer가 같은 descriptor를 다루거나 event와 실제 syscall 사이 state가 변하면 readiness 뒤에도 short read, EAGAIN이 나올 수 있다. readiness를 completion으로 오해하면 parser state와 buffer ownership이 꼬인다.

event timestamp와 실제 read/write 결과를 함께 기록한다. readiness 이후 drain loop가 얼마나 진행했는지, EAGAIN에서 올바르게 멈췄는지 확인한다. protocol parser는 partial input을 정상 상태로 모델링해야 한다.

---

## CHAPTER 03 · epoll state는 interest와 ready 집합을 분리한다

epoll 계열 모델은 감시하려는 descriptor의 interest와 현재 event를 전달할 ready 상태를 내부적으로 관리한다. descriptor 등록은 I/O를 실행하는 행위가 아니며, event 반환은 해당 descriptor의 state가 관찰 조건을 만족했음을 뜻한다. descriptor lifetime과 epoll registration lifetime도 별개다.

close·dup·fd reuse가 섞이면 숫자가 같아도 다른 open file description을 가리킬 수 있다. stale registration이나 handler가 old generation을 처리하면 unrelated connection에 결과를 적용할 위험이 있다.

fd 숫자만 로그에 남기지 말고 connection/request generation과 함께 기록한다. add/mod/del event, close 시각, callback 시각을 연결해 stale event를 찾는다. descriptor 재사용이 빠른 stress workload를 regression test에 포함한다.

---

## CHAPTER 04 · edge와 level trigger는 drain 책임이 다르다

level-triggered mode는 조건이 계속 참이면 event를 반복 관찰할 수 있고, edge-triggered mode는 state transition을 중심으로 알림을 줄여 event overhead를 낮출 수 있다. edge mode에서는 handler가 nonblocking I/O를 EAGAIN까지 충분히 drain하지 않으면 남은 data가 있는데 새 edge가 오지 않아 stall처럼 보일 수 있다.

반대로 한 handler가 너무 오래 drain하면 event loop fairness가 나빠져 다른 connection latency가 증가한다. correctness와 fairness를 동시에 고려해야 한다.

handler당 처리량, EAGAIN 도달 여부, loop lag를 계측한다. edge mode로 전환한 뒤 event 수만 줄었는지, starvation이나 stuck connection이 생기지 않았는지 adversarial burst에서 확인한다.

---

## CHAPTER 05 · one-shot은 handler ownership을 명시적으로 만든다

one-shot event는 한 번 전달된 뒤 다시 arm하기 전까지 같은 registration의 추가 event 전달을 억제해 여러 worker가 동시에 동일 descriptor를 처리하는 위험을 줄일 수 있다. 대신 rearm이 protocol의 필수 state transition이 된다. handler 완료와 rearm 순서가 틀리면 event를 잃거나 stale state를 다시 활성화할 수 있다.

error path나 cancellation에서 rearm 여부가 불명확하면 일부 connection만 영구 정지하는 희귀 장애가 생긴다. success path만 테스트해서는 잡히지 않는다.

registration generation, handler owner, rearm timestamp를 기록한다. callback이 중복 실행되지 않는지와 rearm 누락이 없는지를 fault injection으로 검증한다. ownership transfer를 lock-free한 우연에 맡기지 않는다.

---

## CHAPTER 06 · thundering herd는 한 event가 과도한 waiter를 깨우는 문제다

동일 resource를 기다리는 많은 thread/process가 한 event에 동시에 깨어나면 실제로 일을 얻는 소수 외에는 다시 sleep하면서 scheduler와 cache 비용만 만든다. listen socket accept나 shared queue가 대표적인 contention 지점이다. wakeup 수가 work 수보다 훨씬 많으면 herd가 발생한다.

worker 수를 늘릴수록 throughput이 증가하지 않고 context switch와 runnable queue가 커질 수 있다. herd를 retry loop로 덮으면 CPU storm으로 바뀐다.

wakeup count, successful accept/consume count, context switch를 함께 측정한다. exclusive wakeup, sharding, per-worker queue 같은 구조 변경 전후 useful-wakeup ratio가 개선되는지 본다.

---

## CHAPTER 07 · accept loop는 connection admission과 overload policy를 포함한다

listen socket이 readable해졌을 때 accept를 한 번만 하고 돌아가면 backlog에 여러 connection이 남을 수 있고, 반대로 무제한 accept하면 event loop가 새 connection만 처리하며 기존 request를 굶길 수 있다. accept loop에는 drain 정책과 fairness budget이 필요하다.

파일 descriptor 한도, TLS handshake capacity, worker queue가 이미 포화인데 계속 accept하면 overload를 내부 memory와 tail latency로 옮길 뿐이다. admission control은 socket layer부터 시작될 수 있다.

listen backlog, accept rate, rejected/closed connection, handshake queue를 같은 시간축에 둔다. burst test에서 기존 session SLO와 new-connection success를 함께 본다.

---

## CHAPTER 08 · completion correlation은 결과를 원래 request와 정확히 연결한다

completion 기반 API에서는 제출 순서와 완료 순서가 다를 수 있다. user data, request ID, generation 같은 correlation 정보가 없으면 completion을 wrong buffer나 logical operation에 적용할 수 있다. 특히 동일 descriptor에서 여러 request가 동시에 진행될 때 순서 가정은 위험하다.

request가 timeout되어 caller state가 사라진 뒤 late completion이 도착하는 경우도 있다. ID가 재사용되면 stale completion이 새 request와 충돌할 수 있다.

submit ID와 completion ID, generation, buffer owner를 기록한다. out-of-order completion과 timeout/retry를 강제로 섞는 test에서 결과가 정확한 operation에 귀속되는지 확인한다.

---

## CHAPTER 09 · io_uring ring은 submission과 completion을 shared queue로 연결한다

io_uring 계열은 userspace와 kernel이 submission/completion ring을 공유해 syscall과 handoff 비용을 줄일 수 있다. ring index와 entry ownership은 producer/consumer protocol이며, head/tail update와 entry content visibility가 올바른 ordering을 가져야 한다. ring이 있다고 I/O 자체가 무한히 빨라지는 것은 아니다.

SQ가 가득 차거나 CQ를 늦게 소비하면 backpressure가 생긴다. completion을 처리하지 않고 submission만 늘리면 ring overflow나 memory retention으로 이어질 수 있다.

SQ/CQ occupancy, submit batch, completion drain rate를 측정한다. ring 크기를 키우는 것보다 steady-state producer/consumer rate가 맞는지 먼저 확인한다.

---

## CHAPTER 10 · SQE는 operation의 immutable snapshot이어야 한다

Submission Queue Entry는 opcode, descriptor, buffer, offset, flags 같은 request 정보를 kernel이 소비할 수 있게 표현한다. submit 뒤 해당 memory나 참조 대상의 의미가 임의로 바뀌면 request semantics가 깨질 수 있다. linked operation이나 fixed resource를 사용할 때 lifetime 규칙이 더 중요하다.

buffer address가 valid해 보여도 completion 전에 free/reuse되면 다른 object가 overwrite될 수 있다. descriptor close와 fd reuse도 동일한 위험을 만든다.

SQE 생성 시 request generation과 resource reference를 추적한다. sanitizer/fault test로 early free·close를 넣어 completion 경계가 실제 lifetime을 보호하는지 검증한다.

---

## CHAPTER 11 · CQ backpressure는 완료 결과를 소비하지 못할 때 생긴다

device가 빠르게 completion을 만들어도 application이 CQ를 drain하지 못하면 completion queue가 포화될 수 있다. 이때 새 submission의 진행이 제한되거나 overflow 처리 비용이 생긴다. I/O engine 전체는 submission rate뿐 아니라 completion processing capacity에 제한된다.

callback 내부 heavy work를 직접 수행하면 CQ drain이 늦어지고, 결국 downstream 결과 처리 때문에 upstream I/O까지 정체될 수 있다. completion thread와 business work를 분리하는 이유다.

CQ depth, oldest completion age, handler duration을 측정한다. queue 크기 확대 후 latency가 단순히 뒤로 밀린 것은 아닌지 확인한다.

---

## CHAPTER 12 · registered files는 lookup 비용과 lifetime을 교환한다

registered file table은 반복 request에서 descriptor lookup·reference acquisition 비용을 줄일 수 있지만 등록된 resource의 lifetime을 별도로 관리해야 한다. application의 fd close 의미와 ring registration의 reference가 다를 수 있다.

config reload에서 old file을 닫았다고 생각했지만 ring registration이 reference를 유지하면 resource leak이나 stale target 사용이 생길 수 있다. update 시 index generation을 관리해야 한다.

registered slot, backing file identity, update/remove event를 로그에 남긴다. hot reload와 cancellation이 동시에 일어나는 test로 stale slot을 확인한다.

---

## CHAPTER 13 · registered buffers는 pinning과 reuse 규칙을 강화한다

registered buffer는 repeated mapping 비용을 줄이는 대신 memory를 장기간 pin하거나 특별한 lifecycle로 묶을 수 있다. pinned page는 reclaim과 migration을 제한해 system memory pressure에 영향을 줄 수 있다. 작은 latency win이 전체 memory flexibility를 해칠 수 있다.

buffer pool이 request 수보다 작으면 reuse 경쟁이 생기고, completion 전 slot을 다시 배정하면 data corruption이 발생한다.

pin count, buffer occupancy, completion ownership을 관찰한다. memory pressure와 I/O latency를 함께 benchmark해 registration의 실제 trade-off를 확인한다.

---

## CHAPTER 14 · provided buffers는 receive buffer ownership을 pool로 관리한다

provided buffer mechanism은 kernel이 준비된 buffer pool에서 적절한 buffer를 선택해 receive completion과 함께 ID를 반환하게 할 수 있다. application은 completion 후 buffer를 처리하고 다시 pool에 반환해야 한다. ID는 pointer가 아니라 generation-aware ownership token처럼 다루는 편이 안전하다.

consumer가 buffer를 오래 잡으면 pool이 고갈되어 receive가 멈출 수 있다. 오류 path에서 반환 누락이 있으면 시간이 지나며 capacity가 감소한다.

available buffer count, hold duration, return rate를 모니터링한다. cancellation·parse error에서도 buffer가 정확히 pool로 복귀하는지 테스트한다.

---

## CHAPTER 15 · multishot은 한 submission에서 여러 completion을 만들 수 있다

multishot operation은 accept/receive처럼 반복 event를 하나의 request lifetime 아래 여러 completion으로 전달할 수 있다. request가 첫 completion 뒤 끝났다는 기존 가정을 그대로 사용하면 resource를 너무 일찍 free할 수 있다.

각 completion이 final인지 continuation 가능한지 확인해야 하며 cancellation과 concurrent completion race도 고려한다. one-request-one-result 모델에서 generation이 달라진다.

shot count, final flag, cancellation 시각을 기록한다. 마지막 completion 이전 resource release가 없는지 stress test로 확인한다.

---

## CHAPTER 16 · linked requests는 dependency와 failure propagation을 명시한다

linked request는 operation A 성공 뒤 B를 실행하거나 timeout을 request chain에 묶는 식으로 여러 I/O 사이 ordering을 표현한다. 한 request 실패가 뒤 request를 취소하는지 계속하는지 semantics를 정확히 알아야 한다.

부분 성공이 external side effect를 남긴 상태에서 chain이 중단되면 application-level rollback이 필요할 수 있다. kernel link는 business transaction을 자동 보장하지 않는다.

각 link의 result와 cancel reason을 저장한다. mid-chain failure를 주입해 최종 external state가 정의된 recovery contract를 만족하는지 확인한다.

---

## CHAPTER 17 · timeout race는 완료와 시간 만료가 거의 동시에 일어날 때 발생한다

timeout event와 I/O completion이 경쟁하면 caller는 timeout을 관찰했지만 operation은 이미 성공했을 수 있다. non-idempotent write를 즉시 retry하면 duplicate side effect가 생길 수 있다. timeout은 '실패했다'보다 '결과를 제때 확인하지 못했다'는 상태일 수 있다.

kernel-level cancel 성공 여부와 remote/device side effect 여부도 다르다. ambiguous outcome을 application protocol이 처리해야 한다.

request ID, timeout fire, completion, retry를 같은 timeline에 둔다. boundary time을 의도적으로 흔드는 test로 duplicate execution을 검증한다.

---

## CHAPTER 18 · cancellation은 resource 회수 protocol이다

async cancellation은 caller intent를 전달하지만 이미 실행 중인 device/remote operation을 항상 즉시 중단시키는 것은 아니다. cancel request와 original completion이 경쟁할 수 있으며 둘 중 어느 결과를 authoritative하게 볼지 API contract가 필요하다.

caller state를 먼저 free하면 late completion이 use-after-free를 만들 수 있다. cancellation 이후에도 completion을 drain하거나 generation check로 무시해야 할 수 있다.

cancel latency, late completion count, cleanup completion을 모니터링한다. success/error/timeout/cancel 모든 경로에서 동일 resource가 한 번만 해제되는지 검증한다.

---

## CHAPTER 19 · SQPOLL은 syscall을 줄이는 대신 dedicated CPU를 소비한다

SQ polling thread는 submission ring을 지속 관찰해 userspace가 매번 syscall하지 않아도 kernel이 request를 발견하게 한다. high-rate workload에서 handoff latency를 줄일 수 있지만 idle에서도 polling CPU와 power 비용이 발생할 수 있다.

poll thread가 잘못된 CPU에 배치되면 application과 cache를 경쟁하거나 power policy를 방해한다. low-rate workload에서는 이득보다 비용이 클 수 있다.

poll CPU utilization, submission latency, energy를 rate sweep으로 측정한다. maximum throughput만 보고 mode를 상시 활성화하지 않는다.

---

## CHAPTER 20 · I/O polling은 device completion latency와 CPU budget을 교환한다

busy I/O polling은 interrupt를 기다리지 않고 completion state를 직접 반복 확인해 매우 낮은 latency를 얻을 수 있다. 대신 CPU를 지속 소비하고 같은 core의 다른 work를 밀어낼 수 있다. NVMe 같은 fast device에서도 workload 특성에 따라 이득이 달라진다.

polling thread가 descheduled되면 기대한 latency advantage가 사라지고 CPU cost만 남을 수 있다. power/thermal 환경에서도 지속 polling은 불리하다.

p50뿐 아니라 p99 latency와 CPU-cycle-per-I/O를 함께 본다. polling on/off를 동일 queue depth에서 비교한다.

---

## CHAPTER 21 · mixed event model은 여러 completion source를 하나의 state machine으로 묶어야 한다

network readiness, io_uring completion, timer, signal, worker future가 한 application에 동시에 존재하면 event source마다 ordering과 cancellation semantics가 다르다. 단일 event loop에 넣는 것만으로 의미가 통일되지는 않는다.

한 request가 network event와 timeout, worker completion을 동시에 받을 수 있어 generation과 terminal-state transition이 필요하다. terminal 상태 뒤 들어온 event를 안전하게 무시해야 한다.

request state transition을 trace하고 illegal transition을 assertion으로 잡는다. source별 event ID를 logical operation ID와 연결한다.

---

## CHAPTER 22 · zero-copy는 copy를 없애기보다 ownership boundary를 바꾼다

zero-copy 계열 API는 user↔kernel 또는 subsystem 사이 data copy를 줄여 CPU와 memory bandwidth를 아낄 수 있다. 대신 buffer가 device/network stack 사용을 마칠 때까지 application이 수정·재사용하지 못하는 ownership 제약이 생긴다.

작은 payload에서는 setup/pinning 비용이 copy보다 클 수 있다. completion notification을 무시하고 buffer를 재사용하면 silent corruption이 가능하다.

bytes copied, pinned duration, completion latency를 함께 측정한다. payload size sweep으로 break-even point를 찾는다.

---

## CHAPTER 23 · zero-copy receive는 packet lifetime을 application까지 연장한다

zero-copy RX는 NIC/stack buffer를 application이 직접 참조하는 경로를 만들 수 있어 copy를 줄이지만 packet buffer release가 application 처리 속도에 의존한다. consumer가 늦으면 RX resource가 고갈될 수 있다.

buffer ownership이 user space로 넘어간 동안 NIC ring refill과 memory pressure가 영향을 받는다. 단순 throughput 이득만 보면 packet loss 위험을 놓친다.

RX pool occupancy, hold time, drop count를 같은 timeline에 둔다. slow consumer를 주입해 backpressure와 buffer return이 정상인지 검증한다.

---

## CHAPTER 24 · direct I/O는 page cache를 우회하지만 alignment와 lifetime 제약을 만든다

direct I/O는 일반 buffered page cache path를 줄여 DB처럼 자체 cache를 가진 application에서 double caching을 피할 수 있다. 하지만 buffer alignment, offset, block size 제약이 있고 device completion까지 buffer lifetime을 유지해야 한다.

small random I/O에서 direct path가 항상 빠른 것은 아니다. read-ahead와 cache hit를 잃을 수 있으며 queue depth 관리 책임도 application 쪽으로 이동한다.

buffered/direct를 동일 workload와 durability 조건에서 비교한다. page cache hit ratio와 device I/O 양까지 측정한다.

---

## CHAPTER 25 · short I/O는 정상 결과로 처리해야 한다

read/write가 요청한 길이보다 적은 byte만 처리하고 성공할 수 있는 API가 존재한다. signal, socket buffer capacity, file/device 특성 때문에 short result는 반드시 error가 아니다. protocol은 남은 byte를 정확히 이어서 처리해야 한다.

'한 번 write면 message 하나가 전송된다'는 가정은 stream transport에서 깨진다. retry offset을 잘못 계산하면 duplicate 또는 data gap이 생긴다.

requested/actual length와 offset을 기록한다. artificially small buffer와 interruption을 넣어 framing이 보존되는지 테스트한다.

---

## CHAPTER 26 · backpressure는 in-flight work에 상한을 둔다

producer가 completion capacity보다 빠르면 submission queue, memory buffer, downstream service 중 어딘가에 backlog가 쌓인다. unbounded async는 blocking thread를 없애는 대신 무한 queue를 만들 수 있다. semaphore, bounded channel, queue depth limit이 overload를 명시적으로 드러낸다.

reject/drop/block 중 정책은 data semantics에 따라 다르다. retry 가능한 request를 즉시 reject하면 upstream retry storm이 생길 수도 있다.

in-flight count, oldest age, rejection, downstream saturation을 함께 본다. limit을 latency SLO와 service capacity에서 도출한다.

---

## CHAPTER 27 · event fairness는 hot source가 loop를 독점하지 못하게 한다

한 socket이나 CQ가 계속 event를 만들어도 event loop가 그것만 drain하면 다른 timer·connection이 굶을 수 있다. per-source work budget과 round-robin scheduling은 tail starvation을 줄이는 방법이다.

fairness를 너무 강하게 적용하면 cache locality와 throughput이 낮아질 수 있다. priority가 필요한 control-plane event와 bulk data를 같은 queue에 두는 것도 위험하다.

source별 service time과 max wait를 측정한다. average loop latency뿐 아니라 가장 오래 기다린 event age를 본다.

---

## CHAPTER 28 · async tracing은 request의 suspension과 resume를 연결해야 한다

async request는 하나의 physical stack에 전체 call chain이 남지 않는다. submission span, kernel/device event, completion callback이 서로 다른 thread와 시간에 실행될 수 있어 correlation ID가 없으면 causal path가 끊긴다.

trace context propagation이 빠지면 slow request의 가장 긴 wait가 보이지 않을 수 있다. instrumentation 자체가 event loop를 느리게 만들 수도 있다.

logical operation ID와 attempt ID를 분리해 기록한다. trace sampling 시 timeout/error request가 보존되는지 확인한다.

---

## CHAPTER 29 · async benchmark는 concurrency와 service capacity를 분리한다

async engine benchmark는 단순 QPS뿐 아니라 in-flight 수, queue depth, payload, device/network latency를 명시해야 한다. concurrency를 높이면 throughput이 늘다가 saturation 이후 p99만 급격히 증가할 수 있다.

synthetic no-op completion은 실제 buffer ownership과 downstream cost를 대표하지 못한다. warm cache와 local loopback만으로 production behavior를 추정하지 않는다.

concurrency sweep으로 throughput-latency curve를 만든다. CPU cost per operation과 memory footprint도 함께 기록한다.

---

## CHAPTER 30 · async contract는 submission, completion, cancellation, ownership을 함께 정의한다

안전한 async API는 request가 언제 accepted되는지, completion이 한 번인지 여러 번인지, buffer·descriptor를 언제 재사용할 수 있는지, timeout/cancel 뒤 late result를 어떻게 처리하는지 명시해야 한다. 구현 세부보다 이 contract가 호출자 correctness를 결정한다.

API를 바꿀 때 success path만 호환되면 충분하지 않다. queue full, partial completion, cancel race, process shutdown에서 observable behavior가 유지되어야 한다.

contract test는 event ordering을 강제로 뒤집고 resource leak과 duplicate side effect를 확인한다. async 설계의 목표는 callback 수를 줄이는 것이 아니라 모든 intermediate state의 ownership을 증명하는 것이다.
