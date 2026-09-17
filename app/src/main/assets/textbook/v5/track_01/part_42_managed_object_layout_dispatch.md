# PART 42 · Managed Object Layout and Dispatch — headers, fields, vtables, barriers

managed runtime의 object는 source field 목록만으로 구성되지 않는다. header, alignment, class metadata, GC barrier, monitor state와 dispatch cache가 함께 runtime representation을 만든다. 이 PART는 **object layout, allocation, virtual/interface dispatch, class lifecycle, GC와 heap locality**를 하나의 실행 모델로 연결한다.

---

## CHAPTER 01 · object header는 runtime identity와 관리 metadata를 담는다

managed object 앞에는 class pointer, mark/lock state, GC 관련 bit 같은 runtime metadata가 포함될 수 있다. source에 field가 하나도 없어도 object가 zero bytes가 아닌 이유다. header layout은 runtime version과 architecture에 따라 달라질 수 있어 application이 raw offset에 의존하면 안 된다.

identity hash나 monitor 사용으로 header state가 변하거나 별도 side structure가 생길 수 있다. object count가 많으면 작은 header overhead도 heap 전체에서 큰 비중이 된다.

heap 분석에서는 logical payload와 runtime overhead를 분리한다. memory regression이 field 증가인지 object count 증가인지 확인한다.

---

## CHAPTER 02 · alignment와 padding은 field size 합보다 object footprint를 키운다

CPU가 특정 type을 자연 alignment에서 접근하도록 runtime은 field와 object 끝에 padding을 넣을 수 있다. field order가 바뀌면 같은 logical data라도 total size가 달라질 수 있다.

small object가 millions 단위라면 8 bytes padding 차이가 큰 heap 차이를 만든다. 하지만 manual field reordering이 ABI/serialization/reflection semantics를 바꾸는지 확인해야 한다.

runtime object-size tool로 실제 footprint를 측정한다. source type size 추정만으로 capacity planning을 하지 않는다.

---

## CHAPTER 03 · reference width는 heap size와 pointer compression 정책에 따라 달라질 수 있다

64-bit process라도 managed reference를 compressed form으로 저장해 object field 크기를 줄일 수 있다. 가능한 heap address range와 alignment를 이용해 decode할 수 있지만 heap configuration이 커지면 compression 조건이 달라질 수 있다.

reference width 변화는 object size와 cache locality, GC metadata에 연쇄 영향을 준다. 같은 app이 device/VM heap setting에 따라 footprint가 달라질 수 있다.

runtime configuration과 heap limit을 benchmark 결과에 포함한다. memory regression을 build diff로만 설명하지 않는다.

---

## CHAPTER 04 · field layout은 type alignment와 runtime policy의 결과다

runtime은 superclass field, primitive width, reference field를 alignment와 access efficiency에 맞게 배치한다. source declaration order가 physical order와 항상 동일하다고 가정하면 unsafe/FFI code가 깨질 수 있다.

reflection은 logical field identity를 제공하지만 raw memory serialization은 runtime-specific layout에 묶인다. persistent format에 managed object bytes를 그대로 쓰지 않는다.

field offset이 필요한 low-level tool은 runtime-supported API를 사용한다. version upgrade에서 layout diff를 자동 확인한다.

---

## CHAPTER 05 · inheritance layout은 superclass와 subclass state를 하나의 object에 구성한다

subclass object는 inherited field와 자신의 field, runtime header를 함께 포함한다. hierarchy가 깊어지면 logical abstraction과 physical locality가 다르게 보일 수 있다. base field가 모든 subclass object에 반복되므로 작은 change도 전체 heap에 큰 영향을 준다.

class evolution에서 field 추가가 serialized native layout처럼 fixed ABI라고 가정하면 안 된다. managed runtime은 internal layout을 변경할 자유를 가질 수 있다.

heap histogram을 class hierarchy별로 본다. common base field의 memory cost를 object count와 곱해 평가한다.

---

## CHAPTER 06 · array layout은 header, length, element region을 연속 구조로 가진다

managed array는 class/header와 length metadata 뒤 element storage가 이어질 수 있다. primitive array는 element가 inline되어 locality가 좋고 object-reference array는 pointer만 연속되고 target은 heap에 흩어질 수 있다.

large array allocation은 contiguous virtual region과 GC policy에 부담을 줄 수 있다. bounds check elimination과 vectorization은 length와 access pattern을 이용한다.

array length distribution과 retained size를 측정한다. many small object graph와 primitive flat array의 locality를 benchmark한다.

---

## CHAPTER 07 · multidimensional array는 flat matrix가 아니라 array graph일 수 있다

언어의 `T[][]` 형태는 각 row가 별도 array object인 jagged representation일 수 있다. row마다 header와 reference가 있고 memory가 연속이라는 보장이 없다. numerical kernel에서 flat contiguous buffer보다 cache와 vectorization이 불리할 수 있다.

ragged shape가 필요한 domain에서는 이 유연성이 장점이다. 무조건 flat으로 바꾸면 indexing과 resize cost가 달라진다.

memory footprint와 row locality를 실제 object graph로 확인한다. matrix benchmark에서 representation을 결과와 함께 명시한다.

---

## CHAPTER 08 · string layout은 character encoding과 immutability policy의 결합이다

managed string은 length, encoding representation, backing storage를 가지며 runtime이 Latin-1/UTF-16 같은 compact representation을 선택할 수 있다. logical Unicode character 수와 storage bytes, indexing unit은 다를 수 있다.

substring이 backing buffer를 공유하는 runtime이라면 작은 slice가 큰 original storage lifetime을 유지할 수 있고, copy semantics라면 allocation cost가 생긴다. runtime version에 따라 정책이 달라질 수 있다.

string-heavy memory issue에서 average length와 backing bytes를 측정한다. user-visible character와 code unit을 혼동하지 않는다.

---

## CHAPTER 09 · slice retention은 작은 view가 큰 backing object lifetime을 연장할 수 있다

array/string view가 원본 storage를 참조하면 copy를 피할 수 있지만 view 하나가 살아 있는 동안 전체 backing object가 reclaim되지 않는다. parser가 giant input의 작은 token만 cache하는 경우 예상보다 큰 retention이 생긴다.

copy와 view 사이 선택은 object size와 lifetime 차이를 기준으로 한다. short-lived slice는 view가 유리하고 long-lived tiny slice는 copy가 더 작을 수 있다.

heap dominator에서 retained size를 확인한다. logical size가 작은 object가 큰 backing을 소유하는지 본다.

---

## CHAPTER 10 · boxing은 primitive value를 object identity와 allocation으로 바꾼다

primitive를 generic collection이나 reflective API에 넣을 때 wrapper object가 필요하면 allocation과 indirection이 생긴다. runtime cache가 일부 common value를 재사용할 수 있지만 모든 value에 적용된다고 가정하면 안 된다.

hot loop에서 boxing은 GC pressure와 cache miss를 늘릴 수 있다. equality도 value와 identity semantics를 혼동하면 bug가 생긴다.

allocation profile에서 wrapper class를 찾는다. primitive-specialized container와 성능·API complexity를 비교한다.

---

## CHAPTER 11 · allocation fast path는 common object 생성을 thread-local bump로 단순화한다

managed runtime은 thread-local allocation buffer 같은 영역에서 pointer를 증가시키는 빠른 allocation path를 사용할 수 있다. object 생성 자체가 항상 global lock이나 syscall을 의미하지 않는다.

buffer refill과 large object는 slow path를 타고 GC pressure에 따라 allocation stall이 생길 수 있다. microbenchmark가 fast path만 측정하면 real heap pressure를 대표하지 못한다.

allocation rate, refill, GC pause를 같은 시간축에 둔다. throughput보다 retained live set도 함께 본다.

---

## CHAPTER 12 · zero initialization은 language safety contract와 allocation cost에 포함된다

managed language는 새 object field가 정의된 default value에서 시작한다고 보장할 수 있다. runtime/OS는 freshly zeroed page, bulk clear 등을 활용해 비용을 줄인다. 이 보장을 생략하면 이전 tenant/object data가 노출되는 security 문제가 된다.

large array를 만들고 곧 모든 bytes를 overwrite해도 zero-init을 자동 제거할 수 있는지는 optimizer proof에 달려 있다. unsafe path는 별도 책임을 가진다.

allocation benchmark에서 zeroing bandwidth를 고려한다. security를 이유 없이 performance option으로 끄지 않는다.

---

## CHAPTER 13 · virtual dispatch는 runtime type에서 method target을 선택한다

virtual method call은 receiver class metadata와 dispatch table 등을 이용해 실제 implementation을 찾는다. source call 한 번이 indirect branch와 null/type check를 포함할 수 있다.

call site가 monomorphic하면 JIT/AOT compiler가 direct call로 devirtualize할 수 있다. highly polymorphic site는 predictor와 inline cache 효율이 낮아진다.

type profile과 target distribution을 본다. abstraction을 제거하기 전에 optimizer가 이미 direct call을 만드는지 확인한다.

---

## CHAPTER 14 · interface dispatch는 class hierarchy 밖의 capability lookup을 처리한다

interface call은 receiver가 어떤 concrete class인지와 해당 interface method mapping을 runtime에서 해결해야 한다. vtable index가 고정된 simple virtual call보다 lookup 구조가 복잡할 수 있다.

runtime은 interface table, hash, inline cache를 사용해 common case를 빠르게 만들 수 있다. reflection/proxy가 target diversity를 늘릴 수 있다.

hot interface call의 miss rate와 devirtualization 여부를 profile한다. source abstraction cost를 추측하지 않는다.

---

## CHAPTER 15 · inline cache는 call site에서 최근 receiver type과 target을 기억한다

JIT runtime은 call site가 반복해서 같은 type을 볼 때 type guard와 direct target을 cache해 lookup을 줄일 수 있다. monomorphic, polymorphic cache가 workload의 type diversity를 반영한다.

class loading이나 dynamic proxy로 새로운 type이 나타나면 cache miss와 recompilation이 생길 수 있다. stale target을 사용하지 않도록 class assumption invalidation이 필요하다.

inline-cache state와 type profile을 telemetry로 본다. startup과 steady-state를 분리한다.

---

## CHAPTER 16 · polymorphism은 call target entropy와 optimization freedom을 줄인다

하나의 call site가 많은 receiver type을 동일 빈도로 보면 branch predictor와 inline cache가 안정된 target을 학습하기 어렵다. compiler도 특정 implementation을 inline하기 어렵다.

하지만 abstraction을 없애기 위해 huge switch를 만드는 것은 maintainability와 code size를 악화할 수 있다. hot path에서만 representation을 특화하는 방법이 있다.

call-site target histogram과 code footprint를 함께 본다. megamorphic site를 실제로 특정한 뒤 최적화한다.

---

## CHAPTER 17 · devirtualization은 type proof를 direct call과 inlining 기회로 바꾼다

final class, class hierarchy analysis, runtime profile로 target이 하나라고 증명하면 indirect dispatch를 direct call로 바꿀 수 있다. 이후 constant propagation과 inlining이 이어져 더 큰 최적화가 가능하다.

JIT speculative proof는 새 subclass가 load되면 깨질 수 있어 guard와 deoptimization path가 필요하다. AOT는 dynamic loading possibility 때문에 더 보수적일 수 있다.

generated code와 guard를 확인한다. benchmark에서 class loading phase를 포함한다.

---

## CHAPTER 18 · class loading은 code와 metadata를 runtime namespace에 등록한다

class loader는 bytecode/dex를 찾아 verify·link하고 type identity를 runtime에 만든다. 같은 class name이라도 loader가 다르면 다른 type identity가 될 수 있는 환경이 있다.

dynamic module/plugin은 class unloading과 metadata lifetime 문제를 추가한다. 잘못된 loader reference가 module 전체를 leak시킬 수 있다.

loaded class count와 loader graph를 heap에서 본다. plugin reload stress test로 old loader가 reclaim되는지 확인한다.

---

## CHAPTER 19 · class initialization은 static state를 한 번 만드는 synchronization event다

class init은 static field와 initializer를 실행하며 여러 thread가 동시에 처음 접근하면 runtime이 initialization ordering을 보장해야 한다. initializer가 blocking I/O나 다른 class init을 호출하면 startup deadlock이나 long stall이 생길 수 있다.

init 실패가 subsequent access에서 어떤 exception state로 남는지 language semantics를 확인한다. hidden global work를 static initializer에 넣지 않는다.

startup trace에서 class-init duration과 dependency를 측정한다. cycle이 있는지 graph로 본다.

---

## CHAPTER 20 · class metadata도 object와 별도 lifetime을 가진다

method table, field descriptor, constant pool 같은 metadata는 class loader와 함께 유지되며 heap object가 없어도 loader가 살아 있으면 memory를 소비할 수 있다. JIT code와 reflection cache가 metadata를 추가로 참조한다.

hot reload 환경에서 loader leak은 native/metaspace 성격의 memory 증가로 나타날 수 있다. Java heap만 보면 원인을 놓친다.

loader별 metadata usage와 weak/reference chain을 확인한다. unload 가능 조건을 runtime 기준으로 검증한다.

---

## CHAPTER 21 · reflection은 dynamic access를 제공하지만 static optimization과 security 경계를 약하게 한다

reflection은 runtime name/type metadata로 field·method에 접근해 flexible framework를 만들지만 compiler가 target과 access pattern을 미리 알기 어렵게 한다. access-control bypass API가 있으면 security 검토도 필요하다.

reflection cache가 class loader를 strong하게 잡아 leak을 만들 수 있다. repeated lookup이 hot path면 generated adapter나 cached handle이 낫다.

reflective call count와 target을 profile한다. 외부 input으로 arbitrary member를 선택하게 하지 않는다.

---

## CHAPTER 22 · generated code는 runtime metadata와 executable memory lifetime을 연결한다

proxy, serializer, JIT는 runtime에 새 class/code를 생성할 수 있다. code cache와 class loader가 generation identity를 공유해야 old code가 unload된 metadata를 참조하지 않는다.

W^X와 signing/provenance도 generated executable에 적용된다. debug symbol이 없으면 crash 분석이 어렵다.

code generation ID와 loader lifetime을 trace한다. 반복 generation 뒤 memory가 수렴하는지 확인한다.

---

## CHAPTER 23 · monitor state는 object identity에 synchronization metadata를 연결한다

managed `synchronized`/monitor는 object별 lock state와 waiter queue를 관리한다. uncontended fast path는 header bit나 lightweight structure를 사용하고 contention이 생기면 heavier monitor로 inflate될 수 있다.

lock 사용이 object layout과 identity hash와 상호작용할 수 있다. hot object 하나를 monitor로 공유하면 coherence와 scheduler contention이 커진다.

monitor contention과 inflation count를 profile한다. lock object granularity를 invariant에 맞춘다.

---

## CHAPTER 24 · identity hash는 object address와 같은 개념이 아니다

moving GC가 object를 이동할 수 있으므로 stable identity hash를 raw address에 단순 의존할 수 없다. runtime은 header 또는 side metadata로 identity를 유지할 수 있다.

identity hash 요청이 lightweight header optimization을 바꿀 수 있고 monitor inflation과 상호작용할 수도 있다. performance-sensitive code가 object identity semantics를 남용하지 않게 한다.

hash distribution과 allocation을 따로 본다. logical key에는 domain value hash를 우선 사용한다.

---

## CHAPTER 25 · GC barrier는 object reference mutation을 collector invariants와 연결한다

write/read barrier는 application이 reference graph를 바꾸는 동안 concurrent/generational collector가 필요한 metadata를 유지하게 한다. field assignment 하나가 card mark나 remembered-set update를 추가할 수 있다.

barrier cost는 reference write rate와 collector mode에 따라 달라진다. unsafe/native write가 barrier를 우회하면 collector가 live object를 놓칠 수 있다.

barrier-heavy workload를 profile하고 runtime-approved reference API를 사용한다. barrier 제거를 수동 micro-optimization으로 시도하지 않는다.

---

## CHAPTER 26 · moving GC는 reference를 업데이트할 수 있어 raw address lifetime을 제한한다

compacting/copying collector는 fragmentation을 줄이기 위해 live object를 새 address로 이동한다. managed reference는 runtime이 업데이트하지만 native raw pointer는 pin/global-handle 같은 protocol 없이는 stale가 될 수 있다.

long-term pin은 compaction freedom을 낮춰 fragmentation과 pause에 영향을 준다. JNI critical region을 짧게 유지해야 한다.

native boundary에서 pointer acquire/release event를 기록한다. moving-GC configuration에서도 test한다.

---

## CHAPTER 27 · special reference는 reachability와 cleanup semantics를 세분한다

weak/soft/phantom reference 계열은 ordinary strong reference와 다른 GC 처리 규칙을 제공한다. cache eviction이나 cleanup notification에 쓸 수 있지만 collection timing을 business correctness에 의존하면 안 된다.

finalizer와 reference queue가 느리면 resource release가 지연될 수 있다. OS handle은 deterministic close가 더 안전하다.

reference queue backlog와 retained graph를 관찰한다. GC timing을 synchronization mechanism으로 사용하지 않는다.

---

## CHAPTER 28 · object graph locality는 pointer chasing과 cache miss를 결정한다

서로 연결된 object가 heap 곳곳에 흩어지면 traversal이 많은 cache miss와 TLB miss를 만들 수 있다. flat array/struct representation은 locality를 개선할 수 있지만 abstraction과 update cost가 달라진다.

GC compaction이 locality를 일부 개선할 수 있어도 allocation chronology와 logical relation이 항상 일치하지 않는다.

pointer-chasing hotspot의 memory stall을 profile한다. representation 변경 전후 object count와 bytes까지 비교한다.

---

## CHAPTER 29 · heap retention은 live object count보다 root와 retained size를 봐야 한다

작은 root 하나가 큰 object graph를 붙잡으면 shallow size는 작아도 retained size가 매우 크다. cache, listener, coroutine capture, class loader가 대표적인 root가 될 수 있다.

GC가 자주 돌아도 strong reachability가 있으면 memory는 줄지 않는다. allocation rate와 leak을 구분해야 한다.

heap dominator tree와 object age를 사용한다. growth path를 snapshot diff로 추적한다.

---

## CHAPTER 30 · managed-object contract는 layout, dispatch, lifetime, GC를 함께 정의한다

managed code의 성능과 correctness를 설명하려면 source field와 method만 보지 말고 runtime header, alignment, class metadata, dispatch cache, GC barrier, monitor state를 포함해야 한다. 이 내부 구조는 runtime version에 따라 바뀔 수 있으므로 application이 raw layout에 의존하지 않게 한다.

optimization은 boxing·reflection·polymorphism을 실제 profile로 확인한 뒤 수행한다. memory issue는 shallow size가 아니라 retained graph와 loader lifetime을 본다.

release 검증은 runtime/GC mode 변경에서 **동일한 language-level semantics를 유지하면서 object lifetime과 dispatch가 예상 가능한 비용으로 동작하는지** 확인하는 것이다.
