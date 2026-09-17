# PART 27 · Weak Memory Models — happens-before, litmus tests, fences, publication proofs

멀티코어 correctness는 source code의 위→아래 순서만으로 증명되지 않는다. Compiler는 as-if 규칙 아래 memory operation을 재배치할 수 있고 CPU는 pipeline·store buffer·cache coherence 때문에 다른 core가 write를 서로 다른 시점에 관찰하게 만들 수 있다. Memory model은 **어떤 execution이 language와 architecture에서 허용되는지**를 정의한다. Synchronization은 `동시에 접근하지 않는다` 수준이 아니라 허용 execution set을 필요한 ordering으로 줄이는 작업이다.

---

## CHAPTER 01 · Program order와 global observation order는 같은 것이 아니다

한 thread가 `x=1; y=1`을 순서대로 실행했더라도 다른 CPU가 y의 write를 먼저 관찰할 수 있는 architecture가 존재한다. Compiler가 independent store를 재배치하거나 CPU store buffer가 visibility timing을 바꿀 수 있기 때문이다. Single-thread 결과를 바꾸지 않는 optimization도 concurrent observer가 있을 때는 observable difference를 만들 수 있다.

Concurrency proof는 source statement order, compiler order, architectural memory order를 구분한다.

---

## CHAPTER 02 · Data race는 단순 timing bug가 아니라 language semantics 경계다

Non-atomic memory location을 synchronization 없이 여러 thread가 access하고 적어도 하나가 write한다면 language에 따라 data race가 undefined/invalid execution을 만들 수 있다. `실제 CPU에서 우연히 값이 맞았다`는 실행은 correctness 증거가 아니다.

Data-race-free program이 강한 semantics를 얻도록 설계된 language memory model이 많다. Shared mutable state를 atomic/synchronization primitive 밖에서 읽는 code는 compiler optimization까지 고려해야 한다.

---

## CHAPTER 03 · Atomicity와 ordering은 분리된 속성이다

Atomic load/store는 tearing 없이 하나의 value를 읽고 쓸 수 있게 하지만 다른 memory operation과의 ordering을 자동으로 모두 보장하지 않는다. Relaxed atomic은 modification atomicity를 제공하면서 cross-location ordering은 최소화할 수 있다.

`atomic 변수 하나 사용 = 주변 data도 안전 publication`이라는 결론은 틀릴 수 있다. Publication protocol은 release/acquire 또는 더 강한 relation이 필요한지 검토한다.

---

## CHAPTER 04 · Modification order는 atomic object별 write 순서를 제공한다

한 atomic object에 대한 modification은 일관된 total order를 가진다고 모델링할 수 있다. 그러나 서로 다른 atomic object x와 y의 modification order 사이에는 자동 global total order가 생기지 않는다.

두 counter가 각각 atomic이어도 `(x,y)` pair invariant를 snapshot처럼 읽을 수 있는 것은 아니다. Cross-object consistency에는 추가 protocol이 필요하다.

---

## CHAPTER 05 · Sequenced-before는 한 thread 내부의 language-level order다

Expression evaluation과 statement execution 사이에는 language가 정한 sequenced-before relation이 있다. 이는 thread 내부 dependency를 정의하지만 다른 thread의 observation을 직접 보장하지 않는다. Inter-thread relation을 만들려면 synchronization relation과 결합해야 한다.

Happens-before proof는 local sequenced-before edge와 cross-thread synchronization edge를 그래프로 연결한다.

---

## CHAPTER 06 · Synchronizes-with edge가 thread 사이 ordering을 만든다

Release operation이 publish한 atomic value를 acquire operation이 적절히 관찰하면 두 thread 사이 synchronizes-with relation이 형성될 수 있다. Release 이전의 write는 acquire 이후 code가 happens-before 관계를 통해 관찰 가능해진다.

이 edge가 없는 flag polling은 CPU에서 `대충 순서대로 보일 것`에 의존한다. Correct publication은 어떤 write가 어떤 read와 synchronize되는지 명시해야 한다.

---

## CHAPTER 07 · Release는 이전 memory effect를 publication 지점보다 뒤로 넘기지 않도록 한다

Producer가 object fields를 초기화한 뒤 release store로 pointer/ready flag를 publish하면 initialization write들이 publication 이후로 밀려 observer가 half-initialized object를 보는 execution을 막는 데 사용된다. Release는 이후 operation을 모두 막는 full fence와 동일한 개념이 아니다.

필요한 방향의 ordering만 사용하면 cost를 줄일 수 있지만 proof가 정확해야 한다.

---

## CHAPTER 08 · Acquire는 publication 관찰 이후의 access를 앞당기지 않도록 한다

Consumer가 acquire load로 ready flag/pointer를 읽은 뒤 object fields를 access하면 해당 acquire와 producer release가 연결될 때 initialization을 안전하게 관찰할 수 있다. Acquire는 이전 operation과 이후 operation 전체를 global total order로 만드는 기능이 아니다.

Release/acquire pair는 ownership handoff·immutable object publication에 핵심 pattern이다.

---

## CHAPTER 09 · Release sequence와 RMW는 단순 flag보다 복잡한 synchronization chain을 만든다

Atomic object에 release store 이후 read-modify-write가 연속될 때 memory model은 특정 release sequence를 통해 synchronization relation을 확장할 수 있다. Counter/refcount algorithm에서 intermediate RMW가 publication chain을 끊는지 여부가 중요하다.

Memory order를 조합할 때 개별 instruction이 아니라 modification history를 봐야 한다.

---

## CHAPTER 10 · Sequential consistency는 atomic operation 사이 강한 global order를 추가한다

Seq_cst atomic은 모든 seq_cst operation이 하나의 total order에 들어가는 더 강한 reasoning model을 제공한다. Debugging과 correctness에는 편하지만 architecture에 따라 fence/serialization cost가 더 클 수 있다.

Performance 이유로 relaxed/acquire/release로 낮출 때는 기존 seq_cst proof에서 어떤 order edge를 제거해도 invariant가 유지되는지 재증명해야 한다.

---

## CHAPTER 11 · Compiler barrier와 CPU barrier는 다른 reorder source를 막는다

Compiler barrier는 compiler가 memory access를 barrier 너머로 code-motion하지 못하게 하지만 CPU hardware의 memory visibility ordering을 강제하지 않을 수 있다. Hardware memory barrier는 architecture execution ordering을 제어한다. Kernel primitive는 둘을 적절히 조합한다.

Inline assembly `barrier()` 하나를 넣고 멀티코어 ordering이 해결됐다고 생각하면 안 된다.

---

## CHAPTER 12 · Store buffer는 다른 CPU가 write를 늦게 보게 만들 수 있다

CPU는 store completion을 기다리지 않고 다음 instruction을 진행하기 위해 store buffer를 사용할 수 있다. Local core는 자신의 pending write를 forwarding해 즉시 읽지만 다른 core에는 cache-coherence propagation 이후 보일 수 있다.

이 때문에 두 core가 각각 write 후 상대 variable을 read하는 Store Buffering litmus에서 직관과 다른 outcome이 architecture/memory order에 따라 허용될 수 있다.

---

## CHAPTER 13 · SB litmus는 `각자 먼저 썼으니 적어도 하나는 보겠지`라는 직관을 깨뜨린다

초기 `x=0, y=0`에서 CPU0이 x=1 후 y를 읽고 CPU1이 y=1 후 x를 읽을 때 두 read가 모두 0을 볼 수 있는지 여부는 memory model의 대표 테스트다. Strong ordering architecture와 weak ordering architecture의 허용 outcome이 다를 수 있다.

Litmus test는 재현 확률을 보는 benchmark가 아니라 memory model이 허용하는 execution을 formal하게 질문하는 도구다.

---

## CHAPTER 14 · Message Passing litmus는 release/acquire publication을 검증한다

Producer가 data를 쓴 뒤 flag를 publish하고 consumer가 flag를 본 뒤 data를 읽는 pattern은 대표적 message-passing test다. Flag load/store가 적절한 release/acquire ordering을 갖지 않으면 consumer가 new flag와 old data를 동시에 보는 execution이 허용될 수 있다.

Protocol의 핵심은 flag value가 아니라 data write→release→acquire→data read로 이어지는 happens-before chain이다.

---

## CHAPTER 15 · Load Buffering은 load→store reorder의 허용 범위를 드러낸다

두 thread가 상대 variable을 읽은 뒤 자신의 variable에 write하는 LB pattern은 architecture/compiler가 load와 later store를 어떻게 order하는지 시험한다. Litmus outcome은 특정 architecture memory model을 확인해야 한다.

한 CPU의 경험을 다른 architecture에 일반화하면 portability bug가 된다.

---

## CHAPTER 16 · IRIW는 여러 observer가 write order를 다르게 볼 수 있는지 시험한다

Independent Reads of Independent Writes는 서로 다른 두 writer와 두 reader를 두고 reader가 write order를 서로 다르게 관찰할 수 있는지 묻는다. Multi-copy atomicity와 global order property를 이해하는 데 사용된다.

`cache coherence가 있으면 모든 CPU가 모든 write를 같은 순간/순서로 본다`는 과도한 해석을 피해야 한다.

---

## CHAPTER 17 · Dependency ordering은 acquire와 같은 것으로 취급하면 안 된다

Address/data/control dependency가 architecture에서 일부 ordering을 만들 수 있지만 compiler가 dependency를 최적화하거나 architecture가 control dependency를 약하게 처리할 수 있다. Consume ordering이 language에서 복잡한 이유도 이 때문이다.

Portable application code는 obscure dependency trick보다 well-defined acquire/release primitive를 선호한다.

---

## CHAPTER 18 · READ_ONCE/WRITE_ONCE는 compiler visibility를 제한하지만 full synchronization은 아니다

Linux kernel의 READ_ONCE/WRITE_ONCE는 compiler가 access를 합치거나 제거·invent하는 것을 막고 특정 access를 한 번 수행하도록 돕는다. 하지만 이 자체가 full barrier는 아니다. 필요한 ordering은 smp_load_acquire, smp_store_release, smp_mb 같은 primitive로 표현해야 한다.

Kernel lockless code는 access atomicity와 ordering primitive를 각각 검토한다.

---

## CHAPTER 19 · Fence는 특정 memory operation 사이에 order edge를 삽입한다

Full/read/write fence는 앞뒤 operation의 일부 또는 전체를 architecture가 정한 방식으로 order한다. Fence를 무조건 많이 넣으면 correctness는 쉬워질 수 있지만 pipeline/store buffer optimization을 제한해 성능 비용이 커질 수 있다.

Fence 최적화는 제거 전후 litmus outcome을 비교해 필요한 edge가 유지되는지 증명해야 한다.

---

## CHAPTER 20 · Lock acquire/release도 memory-order primitive다

Mutex/spinlock은 mutual exclusion뿐 아니라 protected data의 visibility ordering을 제공한다. Critical section write가 unlock 이전에 publish되고 다음 locker가 lock 획득 후 이를 관찰할 수 있도록 memory semantics가 결합된다.

Custom atomic protocol로 lock을 대체할 때 mutual exclusion만 재현하고 memory order를 빠뜨리면 rare corruption이 발생한다.

---

## CHAPTER 21 · Double-checked locking은 publication ordering이 없으면 half-initialized object를 노출할 수 있다

`if (ptr==null) lock; initialize; ptr=obj` pattern에서 outer lock-free read가 synchronization되지 않으면 compiler/CPU가 object initialization과 pointer publication을 reader에게 잘못된 순서로 보이게 할 수 있다. Language가 제공하는 atomic/volatile semantics에 맞는 pattern을 사용해야 한다.

Algorithm 이름을 복사하지 말고 publication edge를 증명한다.

---

## CHAPTER 22 · Seqlock reader는 consistent snapshot을 retry로 얻는다

Writer가 sequence counter를 odd/even으로 변화시키고 data를 수정하면 reader는 counter→data→counter를 읽어 중간 write가 있었는지 검증한다. Counter access와 data access 사이 ordering이 정확하지 않으면 stale/mixed snapshot을 valid로 오판할 수 있다.

Seqlock correctness는 retry loop와 memory barrier가 함께 만든다.

---

## CHAPTER 23 · RCU publication과 reclamation은 ordering과 lifetime 두 문제를 동시에 푼다

Writer가 새 structure를 초기화해 pointer를 publish할 때 reader가 fully initialized state를 보도록 ordering이 필요하다. Old structure를 free하려면 pre-existing reader가 모두 빠져나온 grace period도 필요하다. Visibility와 lifetime은 서로 다른 invariant다.

RCU API는 architecture별 memory barrier guarantee를 제공하므로 hand-written equivalent를 만들기보다 primitive contract를 따른다.

---

## CHAPTER 24 · Device memory ordering은 normal cacheable memory와 다를 수 있다

MMIO register access는 device protocol과 architecture ordering rule을 따른다. SMP memory barrier가 device access ordering을 충분히 보장하지 않는 경우가 있어 device I/O primitive와 mb()/wmb()/rmb()류를 사용해야 한다.

DMA descriptor를 memory에 채운 뒤 device doorbell을 쓰는 protocol에서는 descriptor visibility→doorbell order가 핵심이다. CPU끼리의 atomic pattern만으로 reasoning하지 않는다.

---

## CHAPTER 25 · DMA coherence와 ownership transfer도 memory model 문제다

CPU가 buffer를 채운 뒤 device에 ownership을 넘기고 device completion 후 CPU가 다시 읽는 path는 cache coherence/IOMMU/DMA API가 보장하는 synchronization을 따른다. Noncoherent architecture에서는 explicit cache maintenance가 필요할 수 있다.

Buffer lifecycle에 `CPU-owned`, `device-owned` state를 두고 transition마다 required barrier/sync operation을 명시한다.

---

## CHAPTER 26 · JIT compiler도 atomic semantics를 보존해야 한다

Runtime JIT가 source/bytecode atomic operation을 machine instruction으로 lower할 때 target architecture memory model에 맞는 instruction/fence를 생성해야 한다. Deoptimization과 code patching도 running thread가 inconsistent instruction/data를 보지 않도록 synchronization해야 한다.

Language-level memory model과 hardware-level primitive 사이에는 compiler/runtime가 correctness bridge 역할을 한다.

---

## CHAPTER 27 · Model checker와 herd-style litmus engine은 희귀 execution을 enumeration한다

Concurrency test를 수백만 번 실행해도 특정 reorder outcome이 안 나올 수 있다. Formal memory-model tooling은 small program의 event graph와 relation을 분석해 어떤 outcome이 허용되는지 enumerate할 수 있다.

Proof-critical lock-free code는 stress test뿐 아니라 litmus/model analysis를 사용해 architecture portability를 검증한다.

---

## CHAPTER 28 · Thread sanitizer는 happens-before 추적을 이용해 race를 찾지만 모든 ordering bug를 증명하지 않는다

Dynamic race detector는 instrumented memory access와 synchronization event를 추적해 data race를 발견할 수 있다. 실행되지 않은 path는 분석하지 못하고 intentional lock-free algorithm은 false positive/unsupported primitive 문제가 있을 수 있다.

Race detector PASS는 weak-memory proof 완료가 아니다. Static/litmus reasoning과 함께 사용한다.

---

## CHAPTER 29 · Memory-order 최적화는 benchmark 전에 proof delta를 작성한다

seq_cst를 acquire/release나 relaxed로 낮추려면 먼저 기존 ordering graph에서 제거되는 edge를 적고 invariant가 여전히 성립하는지 확인한다. 그 다음 performance counter/benchmark로 실제 이득을 측정한다.

`relaxed가 빠르다`는 이유만으로 변경하면 correctness risk를 performance 근거 없이 떠안게 된다. 많은 architecture에서 차이가 거의 없는 path도 있다.

---

## CHAPTER 30 · Weak-memory proof의 최종 형식은 event·relation·invariant다

Concurrent algorithm을 검토할 때 narrative 대신 다음 세 층으로 표현한다.

1. **Events** — 각 thread/device가 어떤 load/store/RMW/fence를 수행하는가.
2. **Relations** — sequenced-before, reads-from, modification order, synchronizes-with, happens-before가 어떻게 연결되는가.
3. **Invariant** — 어떤 forbidden state/outcome이 이 relation 아래 도달 불가능해야 하는가.

이 proof가 없으면 `테스트에서 한 번도 안 깨졌다`는 결과는 scheduling/reorder space의 극히 일부만 본 것이다. Senior-level concurrency review는 code style이 아니라 **허용 execution 집합을 필요한 범위로 줄였는지** 판정한다.
