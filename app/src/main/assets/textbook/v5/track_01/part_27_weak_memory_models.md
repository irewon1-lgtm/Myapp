# PART 27 · Weak Memory Models — happens-before, fences, litmus tests

동시성 correctness는 source line 순서만으로 증명할 수 없다. compiler와 CPU는 language/ISA가 허용하는 범위에서 load·store를 재배치하고, cache coherence가 있어도 다른 thread가 같은 순서를 관찰한다는 보장은 없다. atomicity, visibility, ordering을 분리하고 **happens-before graph와 허용 가능한 execution history**로 증명해야 한다.

---

## CHAPTER 01 · program order와 observation order는 같지 않을 수 있다

한 thread 안에서 source가 A 다음 B를 실행한다고 보여도 compiler와 CPU는 single-thread semantics를 보존하는 범위에서 실제 memory operation의 issue·visibility 순서를 바꿀 수 있다. 다른 core가 관찰하는 order는 language memory model과 synchronization edge가 결정한다.

debug build나 x86 한 machine에서 항상 A→B로 보였다는 경험은 portability proof가 아니다. optimization level이나 ISA가 바뀌면 허용 execution 집합이 달라질 수 있다.

증명은 source 줄 번호가 아니라 event graph로 작성한다. 어떤 write가 어느 release/acquire 또는 lock edge를 통해 reader의 read보다 선행하는지 명시하고, edge가 없으면 우연한 timing에 의존한다고 판단한다.

---

## CHAPTER 02 · data race는 단순 동시 접근보다 강한 개념이다

data race는 서로 다른 execution context가 같은 memory location에 conflicting access를 하고 이를 정당화하는 synchronization relation이 없는 상태다. 언어에 따라 data race가 undefined behavior를 만들거나 제한된 결과 집합을 허용할 수 있다.

논리적 race와도 구분해야 한다. 두 atomic operation이 각각 race-free여도 check-then-act 전체가 원자적이지 않으면 business invariant가 깨질 수 있다.

race detector report와 code ownership model을 함께 본다. report가 없다는 사실은 실행되지 않은 interleaving까지 안전하다는 증거가 아니다. deterministic interleaving test와 memory-model reasoning을 병행한다.

---

## CHAPTER 03 · atomicity와 ordering은 별도 보장이다

atomic load/store는 해당 memory operation이 torn value 없이 하나의 operation처럼 관찰되게 할 수 있지만, 주변 ordinary access의 순서까지 자동으로 보장하지 않을 수 있다. relaxed atomic은 atomicity만 필요하고 cross-variable ordering이 필요 없을 때 사용할 수 있다.

모든 atomic을 sequentially consistent로 바꾸면 reasoning은 쉬워질 수 있지만 비용과 scalability가 달라질 수 있다. 반대로 ordering을 과하게 약화하면 correctness proof가 무너진다.

각 atomic variable의 역할을 state, counter, publication flag 등으로 분류한다. 필요한 synchronization edge를 먼저 정의한 뒤 가장 약하지만 충분한 ordering을 선택하고 litmus test로 허용 결과를 확인한다.

---

## CHAPTER 04 · modification order는 한 atomic object의 write history를 정렬한다

atomic object에는 그 object에 대한 modification order가 존재해 서로 다른 write 사이 일관된 순서를 제공한다. 하지만 두 atomic object의 modification order를 합쳐 전체 program의 하나의 total order가 자동으로 생기지는 않는다.

flag A와 counter B가 각각 원자적이어도 reader가 서로 다른 logical generation의 값을 조합할 수 있다. multi-field invariant를 별도 synchronization 없이 atomic variable 모음으로 표현하는 실수가 여기서 나온다.

history test에서는 각 object의 observed version과 logical generation을 기록한다. cross-object snapshot consistency가 필요한 경우 combined state, lock, seqlock 같은 더 강한 protocol을 사용한다.

---

## CHAPTER 05 · sequenced-before는 한 thread 내부의 language-level relation이다

sequenced-before는 같은 thread evaluation 사이의 ordering relation을 표현한다. 이 relation은 다른 thread로 자동 전파되지 않으며 synchronizes-with 같은 cross-thread edge와 결합될 때 happens-before를 만든다.

source expression 안에서도 evaluation order가 명시되지 않은 언어 규칙이 있을 수 있다. side effect 순서를 문법 직관으로 추정하면 optimizer와 runtime에서 예상이 달라질 수 있다.

언어 specification의 sequencing 규칙을 기준으로 한다. low-level concurrent code는 compound expression을 단순 event로 나누어 graph를 그리면 undefined/unspecified behavior를 찾기 쉽다.

---

## CHAPTER 06 · synchronizes-with는 thread 사이의 visibility 다리를 만든다

release store와 이를 관찰한 acquire load, mutex unlock과 subsequent lock 같은 operation은 memory model이 정의한 synchronizes-with relation을 만들 수 있다. 이 edge가 writer의 선행 write를 reader의 후속 read까지 연결한다.

같은 atomic variable을 읽었다고 항상 edge가 만들어지는 것은 아니다. 어떤 value를 관찰했는지와 memory order가 조건을 만족해야 한다.

publication test에서는 writer data write→release→acquire→reader data read 경로를 명시한다. assertion은 final value만 확인하지 말고 forbidden intermediate state가 관찰되지 않는지 반복 검증한다.

---

## CHAPTER 07 · release는 이전 write를 publication boundary에 묶는다

release operation은 같은 thread에서 그보다 앞선 relevant memory effect가 다른 thread의 matching acquire를 통해 관찰될 수 있게 ordering edge를 만든다. release를 'cache flush 명령' 한 문장으로 설명하면 compiler와 language semantics를 놓친다.

release flag를 data initialization보다 먼저 수행하면 reader가 flag를 보고 incomplete object를 읽을 수 있다. publication point는 초기화 완료 뒤에 위치해야 한다.

IR/assembly와 source memory order를 함께 검토한다. producer-consumer litmus test에서 flag 관찰 후 payload의 모든 invariant가 유지되는지 확인한다.

---

## CHAPTER 08 · acquire는 이후 access가 publication 이전으로 넘어가지 못하게 한다

acquire operation이 matching release의 value chain을 관찰하면 그 이후 read가 writer의 선행 write를 볼 수 있는 happens-before 관계를 만든다. acquire는 모든 system memory를 최신화하는 일반 명령이 아니라 특정 synchronization protocol의 일부다.

acquire 전에 payload를 미리 읽어 두고 flag만 나중에 확인하는 구조는 intended ordering을 우회할 수 있다. compiler가 값을 재사용하지 않는지도 memory model에 따라 봐야 한다.

consumer code는 acquire 이후에 protected state를 읽는 구조로 만든다. litmus와 sanitizer를 병행하고, generated code에서 필요한 barrier가 target ISA에 맞게 생성되는지 확인한다.

---

## CHAPTER 09 · release sequence는 synchronization chain을 중간 RMW까지 확장할 수 있다

release sequence는 release write 뒤 같은 atomic object에 이어지는 특정 modification을 통해 acquire가 원래 release와 연결될 수 있게 하는 memory-model 규칙이다. 복잡한 lock-free algorithm에서 직접 producer를 읽지 않아도 synchronization이 이어지는 이유가 될 수 있다.

규칙을 잘못 기억하면 relaxed RMW가 예상과 다른 visibility를 만들 수 있다. language version에 따라 세부 정의가 바뀌는지 specification을 확인해야 한다.

algorithm proof에는 acquire가 어떤 modification을 읽는지 명시한다. 값만 같다고 같은 release chain으로 간주하지 않고 modification identity를 history model에 포함한다.

---

## CHAPTER 10 · sequential consistency는 강한 공통 order를 제공하지만 만능은 아니다

sequentially consistent atomic은 관련 SC operation에 단일 total order를 제공해 reasoning을 단순화한다. 그러나 ordinary non-atomic data race를 자동으로 고치거나 여러 operation을 하나의 transaction으로 만들지는 않는다.

SC를 사용해도 check-then-act가 여러 atomic으로 분리되면 다른 thread가 중간에 개입할 수 있다. correctness requirement를 먼저 정의해야 한다.

SC 버전으로 reference implementation을 만든 뒤 weaker ordering 최적화와 differential test를 수행하는 방법이 유용하다. 성능 차이가 실제 hotspot인지 측정하기 전에는 약한 ordering으로 복잡성을 늘리지 않는다.

---

## CHAPTER 11 · compiler barrier와 hardware barrier는 역할이 다르다

compiler barrier는 optimizer가 memory operation을 특정 경계 넘어 재배치하지 못하게 하고 hardware fence는 CPU가 memory transaction을 관찰 가능하게 하는 순서를 제한한다. language atomic primitive는 필요한 두 층을 적절히 결합하도록 설계된 경우가 많다.

inline assembly로 hardware fence만 넣고 compiler ordering을 빠뜨리거나 그 반대면 protocol이 깨질 수 있다. architecture마다 fence instruction의 범위도 다르다.

가능하면 language/library atomic을 사용한다. low-level barrier를 직접 쓸 때는 compiler IR과 disassembly, ISA manual을 함께 검토하고 litmus test를 여러 target에서 실행한다.

---

## CHAPTER 12 · store buffer는 다른 core가 write를 늦게 보는 결과를 만들 수 있다

CPU는 store를 local buffer에 임시 보관해 execution을 계속하고 cache coherence로 나중에 visibility를 전파할 수 있다. 두 core가 서로 다른 location에 store한 뒤 상대 location을 load하면 source 직관과 다른 결과가 허용되는 memory model이 존재한다.

이 현상을 'cache가 늦다'라고만 부르면 정확한 ordering requirement를 놓친다. compiler reordering과 hardware buffering을 분리해야 한다.

store-buffer litmus를 사용해 target ISA에서 허용 결과를 확인한다. 필요한 synchronization edge가 있으면 fence나 release/acquire로 표현한다.

---

## CHAPTER 13 · SB litmus는 직관과 memory model의 차이를 드러낸다

Store Buffering litmus는 두 thread가 각각 자신의 variable을 쓰고 상대 variable을 읽는 작은 program으로 약한 ordering에서 가능한 결과를 관찰한다. 작은 state space라서 memory model 비교와 barrier 효과 검증에 적합하다.

실제 hardware에서 forbidden 결과가 한 번도 나오지 않았다고 proof가 되는 것은 아니다. frequency가 매우 낮거나 구현이 specification보다 강할 수 있다.

model checker와 반복 stress를 함께 사용한다. barrier 변경 전후 allowed outcome set이 원하는 방향으로 줄었는지 확인한다.

---

## CHAPTER 14 · message passing은 payload와 publication flag의 순서를 증명한다

producer가 payload를 쓴 뒤 flag를 release store하고 consumer가 flag를 acquire load한 뒤 payload를 읽는 pattern은 대표적인 message-passing synchronization이다. 핵심은 flag 자체보다 payload write가 flag publication 앞에 happens-before로 연결되는 것이다.

flag를 plain variable로 바꾸거나 acquire를 빼면 consumer가 flag=true와 stale payload 조합을 볼 수 있는 모델이 생길 수 있다.

payload를 여러 field로 구성해 initialization invariant를 test한다. weak-memory simulator와 real hardware stress를 사용해 forbidden partial state를 찾는다.

---

## CHAPTER 15 · Load Buffering litmus는 load와 후속 store ordering을 시험한다

Load Buffering pattern은 각 thread가 다른 location을 읽고 자신의 location에 쓰는 구조를 통해 load-store 재배치와 causality를 시험한다. architecture와 language model에 따라 허용 결과가 다를 수 있다.

특정 ISA 경험을 다른 target에 복사하면 portability bug가 생긴다. JIT가 source를 다른 instruction sequence로 바꾸는 경우도 있다.

litmus source, compiler flags, disassembly, observed outcome을 한 artifact로 보존한다. memory-order 변경이 outcome set에 어떤 영향을 주는지 비교한다.

---

## CHAPTER 16 · IRIW는 여러 reader의 write 관찰 순서가 같다는 가정을 시험한다

Independent Reads of Independent Writes pattern은 서로 다른 writer와 reader를 사용해 모든 observer가 independent write를 같은 순서로 본다고 가정할 수 있는지 시험한다. multi-copy atomicity와 memory model 차이를 이해하는 데 유용하다.

일부 platform에서 나오기 어려운 결과라도 portable algorithm은 target model의 허용 범위를 따라야 한다.

cross-platform litmus suite를 유지한다. distributed-like multi-reader state를 lock-free로 설계할 때 global order가 정말 필요한지 먼저 결정한다.

---

## CHAPTER 17 · dependency ordering을 일반 acquire와 혼동하지 않는다

address/data/control dependency가 일부 architecture에서 ordering 효과를 가질 수 있지만 language compiler가 dependency를 보존하는 방식과 hardware guarantee는 별개다. fragile dependency trick은 optimization으로 끊길 수 있다.

portable code에서 dependency에 암묵적으로 의존하면 compiler upgrade 뒤 희귀 race가 나타날 수 있다. consume 계열 semantics가 복잡한 이유다.

명시적 acquire가 허용되는지 먼저 본다. dependency를 직접 사용할 때는 source→IR→assembly에서 chain이 유지되는지 확인한다.

---

## CHAPTER 18 · READ_ONCE/WRITE_ONCE류 primitive는 compiler access를 제한하지만 전체 synchronization은 아니다

kernel-style single-access primitive는 compiler가 load/store를 merge하거나 제거하는 것을 제한해 shared state observation을 안정화할 수 있다. 하지만 이것만으로 cross-CPU happens-before나 full barrier가 생기는 것은 아니다.

flag access를 once primitive로 바꿨다고 payload visibility가 자동 보장된다고 생각하면 안 된다. 필요한 memory barrier와 lock protocol을 별도 사용해야 한다.

code review에서 'atomic access'와 'ordering edge'를 다른 체크 항목으로 둔다. data race report 억제를 synchronization 대체로 사용하지 않는다.

---

## CHAPTER 19 · fence는 ordering edge를 만들지만 대상 operation과 범위를 이해해야 한다

fence는 특정 종류의 memory operation이 경계를 넘어 관찰되는 것을 제한한다. acquire/release fence와 full fence는 범위가 다르고 architecture instruction으로 lowering되는 방식도 다르다.

fence를 추가하면 symptom이 사라질 수 있지만 어떤 edge가 필요했는지 설명하지 못하면 과잉 serialization이나 다른 target bug를 남긴다.

happens-before graph에 fence가 연결하는 read/write를 표시한다. fence 전후 benchmark와 litmus outcome을 함께 비교한다.

---

## CHAPTER 20 · lock은 mutual exclusion과 memory ordering을 함께 제공한다

정상적으로 구현된 mutex의 unlock/lock relation은 critical section 사이 visibility와 ordering을 제공한다. lock으로 보호된 data에 별도 ad-hoc fence를 추가할 필요가 없는 경우가 많다.

하지만 일부 path가 lock을 우회하면 전체 invariant가 깨진다. read-mostly fast path가 plain access를 사용하고 writer만 lock을 잡는 패턴은 안전하지 않을 수 있다.

protected field inventory와 lock ownership을 문서화한다. sanitizer와 stress에서 모든 mutation/read path가 동일 protocol을 따르는지 검증한다.

---

## CHAPTER 21 · double-checked locking은 safe publication이 핵심이다

`if null → lock → create` 패턴에서 object construction과 reference publication 사이 ordering이 없으면 reader가 non-null reference와 partially initialized fields를 함께 볼 수 있다. 두 번째 null check만으로 해결되지 않는다.

언어가 제공하는 thread-safe lazy primitive나 atomic/volatile publication을 사용하는 편이 안전하다. 직접 구현하면 memory model proof가 필요하다.

constructor field invariant를 포함한 stress test를 만든다. timing sleep을 넣어 증상을 숨기는 방식은 regression test가 아니다.

---

## CHAPTER 22 · seqlock reader는 data read와 sequence read의 ordering이 맞아야 한다

seqlock은 writer가 sequence를 변경하고 data를 갱신하며 reader가 시작/종료 sequence를 비교해 중간 write와 겹친 snapshot을 재시도한다. sequence access ordering이 약하면 reader가 data를 엉뚱한 위치에서 읽어 false-stable snapshot을 만들 수 있다.

pointer lifetime이 바뀌는 data는 seqlock만으로 UAF를 해결하지 못한다. snapshot consistency와 reclamation을 분리해야 한다.

reader retry와 writer frequency를 측정한다. memory barrier placement를 model/litmus로 검증한다.

---

## CHAPTER 23 · RCU publication과 reclamation에는 서로 다른 ordering 요구가 있다

RCU writer는 새 object를 완전히 초기화한 뒤 pointer를 publish해야 하고 reader는 올바른 dereference primitive로 initialized state를 관찰해야 한다. old object free는 grace period 뒤에만 가능하다.

publication ordering이 맞아도 grace period 전 free하면 UAF이고, reclamation이 맞아도 partial initialization publication은 data corruption이다.

publish event, reader critical section, grace period를 model로 분리한다. fault/stall reader를 넣어 reclamation backlog와 safety를 함께 검증한다.

---

## CHAPTER 24 · device ordering은 normal memory와 같은 규칙으로 가정하면 안 된다

MMIO register와 device descriptor는 architecture가 normal cacheable memory와 다른 ordering rule을 적용할 수 있다. descriptor content를 memory에 쓴 뒤 doorbell register를 먼저 보이게 하면 device가 incomplete descriptor를 읽을 수 있다.

read/write accessor와 DMA barrier는 platform API를 따라야 한다. raw pointer와 generic atomic만으로 device protocol을 안전하게 구현할 수 있다고 가정하지 않는다.

driver trace에서 descriptor generation과 doorbell, completion을 연결한다. architecture별 stress와 IOMMU/device simulator를 사용한다.

---

## CHAPTER 25 · DMA ownership transfer에는 CPU와 device 사이 visibility 경계가 필요하다

CPU가 buffer를 준비하고 device ownership으로 넘길 때 content와 descriptor가 device에 보이는 순서가 맞아야 한다. device completion 전 CPU가 buffer를 재사용하면 DMA가 stale/new data를 섞을 수 있다.

coherent platform에서도 ownership protocol은 필요하고 non-coherent platform에서는 cache maintenance까지 추가될 수 있다.

CPU_PREPARED→DEVICE_OWNED→COMPLETED 상태를 명시한다. late completion과 reset을 주입해 freed buffer 접근이 없는지 확인한다.

---

## CHAPTER 26 · JIT는 language atomic semantics를 target instruction으로 정확히 보존해야 한다

JIT compiler는 high-level atomic acquire/release/SC operation을 target ISA의 적절한 instruction과 fence로 lowering해야 한다. speculative optimization이나 deoptimization이 synchronization semantics를 바꾸면 runtime 전체 correctness가 깨진다.

hot code와 cold code, deopt 전후에 같은 atomic behavior가 유지되어야 한다. JIT tier 변경에서만 나타나는 race가 가능하다.

generated code와 tier transition을 기록한다. memory litmus를 interpreter/baseline/optimized tier 각각에서 실행해 outcome set을 비교한다.

---

## CHAPTER 27 · litmus와 model checking은 희귀 interleaving을 systematic하게 탐색한다

작은 concurrent program을 memory-model tool에 넣으면 random stress로 만나기 어려운 allowed execution을 체계적으로 확인할 수 있다. 실제 hardware test와 specification model을 연결하는 bridge 역할을 한다.

model이 실제 source compiler semantics를 반영하지 않으면 false confidence가 생긴다. compiler lowering과 target architecture 두 단계를 분리해야 한다.

critical synchronization primitive마다 minimal litmus를 유지한다. code 변경 시 allowed/forbidden outcome set이 의도대로 유지되는지 자동 비교한다.

---

## CHAPTER 28 · race detector는 happens-before 후보를 찾는 동적 도구다

race detector는 instrumented execution에서 conflicting access와 synchronization event를 추적해 happens-before가 없는 pair를 보고한다. 실제 실행된 path만 관찰하므로 report 없음은 전체 state space의 안전성을 증명하지 않는다.

custom synchronization을 detector가 이해하지 못하면 false positive가 생길 수 있고 instrumentation이 timing을 바꿔 race를 숨길 수도 있다.

report는 ownership model과 대조한다. 발견한 race는 deterministic interleaving test와 명시적 synchronization으로 고친다.

---

## CHAPTER 29 · ordering optimization은 proof 이후에만 한다

sequential consistency나 mutex를 relaxed atomic으로 바꾸면 fence와 cache traffic을 줄일 수 있지만 correctness proof가 훨씬 어려워진다. benchmark에서 차이가 작다면 복잡성 비용이 이득보다 클 수 있다.

weak ordering은 architecture별 비용이 다르다. 한 CPU에서 빨라진 최적화를 전체 target에 일반화하면 안 된다.

reference strong-order implementation과 weaker version을 differential test한다. performance counter와 latency가 실제로 개선될 때만 최적화를 유지한다.

---

## CHAPTER 30 · memory proof는 value가 아니라 edge와 lifetime을 증명한다

완전한 concurrent memory proof에는 atomicity, happens-before, object lifetime, progress가 모두 필요하다. 값이 최종적으로 맞았다는 test만으로 중간 UAF나 stale observation을 배제할 수 없다.

각 shared object에 owner, mutation path, publication edge, reclamation condition을 적는다. synchronization primitive를 바꾸면 이 proof도 함께 갱신해야 한다.

CLEAN 검증은 litmus/model, sanitizer, deterministic history test, real hardware stress를 서로 다른 failure mode로 사용한다. 여러 증거가 같은 invariant를 지지할 때만 low-level ordering 변경을 안전하다고 판단한다.
