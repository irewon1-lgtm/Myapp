# PART 42 · Managed Object Layout and Dispatch — headers, fields, vtables, barriers

Managed runtime의 객체는 source code에 적힌 field 목록 그대로 heap에 놓인 단순 record가 아니다. Runtime은 object identity, class/type metadata, synchronization, GC 이동·mark 상태, alignment, reference width를 표현해야 하고, method call은 class hierarchy·interface·inline cache·JIT optimization과 연결된다. 객체 하나의 byte layout과 dispatch path를 이해하면 allocation pressure, cache locality, boxing, reflection, JNI, GC barrier 비용을 같은 모델에서 분석할 수 있다.

## CHAPTER 01 · object header는 payload 밖의 runtime 상태를 담는 공간이다

Managed object는 application field 외에 runtime이 object를 식별하고 관리하는 metadata를 필요로 할 수 있다. Class/type pointer, mark/lock state, identity-hash 관련 상태 같은 정보가 header 또는 side metadata에 위치할 수 있으며 구체 layout은 runtime·version·architecture마다 다르다. Header가 8~16byte 이상인 환경에서는 두세 개 작은 field만 가진 object도 payload보다 metadata 비중이 커질 수 있다. Millions-of-small-objects workload에서 object count 자체가 heap footprint와 cache traffic을 지배하는 이유를 payload 합계만으로는 설명할 수 없다. Runtime layout은 public language ABI가 아닐 수 있으므로 특정 version에서 관찰한 offset을 application contract로 고정하지 않는다.

## CHAPTER 02 · alignment와 padding은 field 합계보다 object size를 크게 만든다

CPU와 runtime은 pointer·scalar access alignment 요구와 allocator granularity 때문에 object 시작 주소와 field offset을 일정 단위에 맞출 수 있다. 1byte boolean 뒤 8byte field가 오면 중간 padding이 생길 수 있고, object 전체 크기도 alignment boundary로 round-up될 수 있다. Field 선언 순서를 바꿔 padding을 줄이는 최적화는 serialization order나 reflection semantics와 독립적으로 판단해야 하며 runtime이 자체 field reordering을 허용하는지도 확인해야 한다. Heap profiler의 shallow size가 source type 크기 예상보다 큰 경우 header, alignment, padding, reference width를 분해해서 계산한다.

## CHAPTER 03 · reference는 반드시 native pointer와 같은 크기·표현일 필요가 없다

64-bit process에서도 heap reference를 32-bit compressed form으로 저장한 뒤 heap base와 scaling rule을 사용해 address를 복원할 수 있다. Reference compression은 object graph의 memory bandwidth와 cache footprint를 줄이는 대신 heap size/addressing 범위와 decode operation에 제약을 만든다. Runtime option이나 heap 규모가 threshold를 넘으면서 compressed mode가 꺼지면 같은 object model의 memory footprint가 갑자기 증가할 수 있다. `64-bit 앱이니까 reference는 모두 8byte` 같은 계산은 runtime configuration을 확인하지 않으면 틀릴 수 있다.

## CHAPTER 04 · field layout은 language semantics와 runtime implementation의 교차점이다

Instance field는 inheritance, alignment, reference/non-reference 분류에 따라 offset이 정해지고 superclass 영역 뒤 subclass field가 붙는 식의 규칙을 사용할 수 있다. GC는 object를 scan할 때 어느 offset이 managed reference인지 알아야 하므로 class metadata에 reference map을 유지할 수 있다. Field offset을 직접 가정하는 unsafe/FFI code는 runtime version과 architecture 변화에 취약하다. Performance 관점에서는 함께 읽히는 hot field를 좁은 working set에 모으고 거의 쓰지 않는 cold field를 별도 object로 분리하면 cache line utilization을 개선할 수 있지만 additional indirection이라는 비용도 생긴다.

## CHAPTER 05 · inheritance는 object payload와 dispatch metadata를 동시에 확장한다

Subclass instance는 superclass state를 포함하면서 추가 field와 method override relation을 가진다. Object layout에는 inherited field 공간이 포함되고 method dispatch는 dynamic type을 기준으로 override target을 찾아야 한다. Deep hierarchy 자체가 항상 느린 것은 아니지만 class metadata, constructor chain, virtual dispatch target diversity, instanceof/type-check graph를 복잡하게 만든다. Composition으로 바꿀지 inheritance를 유지할지는 code style보다 state ownership, dispatch polymorphism, memory layout, extension requirement를 함께 비교한다.

## CHAPTER 06 · array는 element storage를 한 object에 연속 배치하는 특별한 layout을 가진다

Managed array는 object header와 length metadata 뒤에 element region이 이어지는 형태를 사용할 수 있다. Primitive array는 element 값을 연속 byte로 저장해 object-per-element overhead를 피하지만 reference array는 pointer/reference sequence를 저장하고 각 target object는 별도 위치에 있을 수 있다. Bounds check가 language safety를 제공하고 JIT가 loop invariant를 증명하면 일부 check를 제거할 수 있다. Large primitive array는 locality가 좋지만 single huge allocation·copy 비용이 있고, nested object array는 indirection과 GC scanning cost가 늘어난다. 데이터 구조를 선택할 때 logical collection size가 아니라 실제 object count와 reference edge 수를 계산한다.

## CHAPTER 07 · multidimensional array 표현은 rectangular contiguous matrix와 다를 수 있다

언어의 `T[][]`가 실제로 array-of-references-to-arrays라면 각 row는 독립 object이며 길이가 다를 수도 있다. Row마다 header·alignment overhead가 생기고 row 전환마다 pointer chasing이 발생한다. 반면 flat one-dimensional buffer에 index를 계산하면 contiguous layout을 얻지만 bounds/index arithmetic과 API readability 비용이 생긴다. Numerical/image workload에서 cache locality와 vectorization이 중요하면 logical 2D API와 physical flat storage를 분리할 수 있다. `2차원 배열`이라는 source syntax만으로 memory layout을 추론하지 않는다.

## CHAPTER 08 · string은 문자 sequence 외에 encoding·length·hash cache 같은 상태를 가질 수 있다

Runtime은 문자열을 UTF-16 code unit, compact Latin-1/UTF-16 dual form, UTF-8-like internal form 등 다양한 방식으로 구현할 수 있다. 동일한 사용자-visible text라도 internal byte footprint가 content에 따라 달라질 수 있고 cached hash나 slice representation 여부도 runtime별로 다르다. String concatenation이 immutable object를 계속 만들면 intermediate allocation이 증가하므로 builder/rope 같은 구조가 필요할 수 있다. 문자열 성능을 분석할 때 code point count가 아니라 internal representation, allocation count, copy byte, normalization requirement를 분리한다.

## CHAPTER 09 · substring/slice가 backing storage를 공유하는지 복사하는지는 retention을 바꾼다

Slice object가 원본 large buffer를 reference하며 offset/length만 저장하면 substring 생성은 저렴하지만 작은 slice 하나가 수백 MB backing을 오래 retain할 수 있다. 반대로 substring 생성 시 필요한 구간을 복사하면 retention은 줄지만 O(n) copy와 새 allocation이 발생한다. Runtime/library version에 따라 이 정책이 바뀔 수 있으므로 historical behavior를 현재 구현에 대입하면 안 된다. Heap dump에서 작은 logical string이 큰 array를 retain하는지 retained-size graph로 확인한다.

## CHAPTER 10 · boxing은 primitive value를 object identity 세계로 올리는 비용이다

Generic container나 interface가 primitive를 직접 표현하지 못하는 환경에서는 integer/boolean 같은 값을 wrapper object로 boxing할 수 있다. Boxing은 header·alignment·reference edge·allocation/GC cost를 추가하고 unboxing은 type/null check를 포함할 수 있다. Runtime이 small-value cache나 escape analysis로 allocation을 제거할 수 있어 source에서 boxing 문법이 보여도 실제 heap allocation이 없을 수 있다. Performance 판단은 bytecode/IR·allocation profile로 확인하며, hot collection이 millions of boxed numbers를 보유한다면 primitive-specialized representation의 이득이 커질 수 있다.

## CHAPTER 11 · allocation fast path는 bump pointer와 thread-local region으로 매우 짧아질 수 있다

Moving/generational heap에서는 thread가 자신의 allocation region에서 pointer를 size만큼 증가시키는 방식으로 object allocation을 처리할 수 있다. Thread-local allocation buffer는 global allocator lock을 피하고 contiguous young objects를 만들어 locality를 높인다. Fast path가 싸다고 allocation이 공짜인 것은 아니다. Region refill, zeroing, object header init, GC scan/copy, cache bandwidth가 뒤에서 비용을 만든다. `new 한 번의 benchmark`보다 allocation rate(bytes/sec), object lifetime distribution, GC work를 함께 본다.

## CHAPTER 12 · zero initialization은 language safety와 allocation bandwidth를 연결한다

Managed language가 새 object/array field를 0/null로 보장하면 runtime은 memory가 이전 data를 노출하지 않도록 초기화해야 한다. OS에서 zeroed page를 공급받거나 allocator가 bulk zeroing을 수행하며 compiler/runtime가 이후 field store가 전체를 덮는다고 증명하면 redundant zeroing 일부를 줄일 수 있다. Large array allocation에서 initialization memory bandwidth가 실제 application computation보다 클 수 있다. 민감한 data는 object free 후 즉시 zeroed된다고 가정하지 않고 별도 secure-erasure contract를 확인한다.

## CHAPTER 13 · virtual method dispatch는 dynamic class에서 target code를 찾는 과정이다

Virtual call site가 receiver의 runtime class에 따라 다른 method implementation을 호출해야 한다면 compiler는 direct absolute call로 고정할 수 없다. Vtable-like table index, class metadata lookup, inline cache 등 runtime별 mechanism을 사용할 수 있다. Stable monomorphic call site는 JIT가 target을 guard한 뒤 direct/inlined path로 바꿀 수 있고 class assumption이 깨지면 deoptimization할 수 있다. Virtual dispatch 비용은 indirect jump 몇 cycle만이 아니라 inlining 기회를 잃는 비용까지 포함한다.

## CHAPTER 14 · interface dispatch는 class hierarchy와 별도 mapping을 필요로 할 수 있다

하나의 class가 여러 interface를 구현하고 interface method slot이 class vtable 위치와 직접 일치하지 않으면 interface→implementation mapping이 필요하다. Runtime은 interface method table, hash/search, inline cache 같은 구조를 사용할 수 있다. Polymorphic interface call이 hot하면 receiver type distribution을 profile해 devirtualization 가능성을 평가한다. `interface는 느리다` 같은 일반화보다 실제 call site가 monomorphic인지 megamorphic인지, JIT가 어떤 code를 생성했는지 본다.

## CHAPTER 15 · monomorphic inline cache는 최근 receiver type과 target을 call site에 기억한다

동일 call site에서 거의 항상 같은 concrete type이 오면 type guard 하나와 cached target으로 dynamic lookup을 줄일 수 있다. JIT는 guard success path를 inline하고 miss 시 generic resolver로 보낼 수 있다. Type distribution이 바뀌면 cache state가 polymorphic/megamorphic으로 전환될 수 있다. Production traffic의 polymorphism이 benchmark보다 넓으면 lab에서 잘 inlined된 code가 실제 환경에서는 generic dispatch path를 탈 수 있다.

## CHAPTER 16 · polymorphic/megamorphic call site는 code size와 prediction을 함께 압박한다

몇 개 type을 별도 guard chain으로 처리하는 polymorphic inline cache는 lookup을 줄이지만 guard 수와 generated code가 늘어난다. Target type이 매우 많으면 runtime이 megamorphic shared dispatch structure로 되돌아갈 수 있다. 이는 branch predictor target entropy와 I-cache footprint에도 영향을 준다. Object-oriented abstraction 비용을 분석할 때 hierarchy 자체보다 hot call site별 receiver histogram을 수집한다.

## CHAPTER 17 · devirtualization은 type proof를 direct call과 inlining 기회로 바꾼다

Compiler/JIT가 receiver의 concrete type이 하나뿐임을 증명하거나 speculative guard를 세울 수 있으면 virtual/interface call을 direct target으로 바꿀 수 있다. Direct call은 body inlining, constant propagation, escape analysis를 연쇄적으로 가능하게 한다. 그러나 dynamic class loading이나 subclass 등장으로 assumption이 깨질 수 있는 runtime은 invalidation/deoptimization mechanism이 필요하다. 최적화는 type system과 runtime dependency tracking이 함께 만든다.

## CHAPTER 18 · class loading은 bytecode를 읽는 것보다 namespace·verification·linking 문제다

Runtime은 class/module bytes를 찾고 parse하며 type safety를 verify하고 superclass/interface relation과 field/method metadata를 구성한다. 같은 binary name이라도 다른 class loader namespace에서 로드되면 서로 다른 runtime type으로 취급될 수 있다. Plugin/container 환경에서 `이름은 같은데 cast가 실패함`은 class-loader identity 문제일 수 있다. Class loading latency는 file I/O, verification, dex/bytecode processing, initialization을 단계별로 측정한다.

## CHAPTER 19 · class initialization은 최초 active use에 side effect를 실행할 수 있다

Static field initializer나 class initializer가 lazy하게 실행되면 처음 method 호출에서 예상치 못한 lock, I/O, allocation이 발생할 수 있다. 여러 thread가 동시에 initialization을 요청하면 runtime은 한 번만 실행되도록 synchronization하고 실패 state도 관리해야 한다. Cold-start trace에서 특정 class initialization이 긴 경우 단순 method body profile로는 원인을 찾기 어렵다. Static initialization에 network/disk/heavy computation을 넣지 않는 이유는 startup critical path와 hidden synchronization 때문이다.

## CHAPTER 20 · class metadata 자체도 memory를 사용하며 unloadability는 loader lifetime에 달린다

Method table, field descriptor, constant pool, JIT metadata, reflection object가 native/managed memory를 소비할 수 있다. Dynamic module을 반복 load하고 class loader를 strong-reference하면 instance가 없어도 metadata가 unload되지 않을 수 있다. Heap만 보고 native/runtime metadata leak을 놓치지 않는다. Plugin system은 loader ownership과 thread/context/class cache가 loader를 retain하지 않는지 검증해야 한다.

## CHAPTER 21 · reflection은 metadata lookup·access check·boxing을 runtime path로 이동시킨다

Reflection API는 이름/descriptor를 사용해 field/method를 동적으로 찾고 invoke한다. Runtime이 accessor를 cache하거나 generated stub를 만들 수 있지만 direct static call보다 compile-time optimization 정보가 적다. Arguments를 object array로 만들고 primitive를 boxing하는 구현도 있을 수 있다. Reflection-heavy serialization/DI framework는 metadata cache와 generated-code path를 활용해 hot invocation 비용을 줄일 수 있다. 보안상 private access 우회 규칙도 runtime/version에 따라 확인한다.

## CHAPTER 22 · dynamic proxy와 generated class는 code/data footprint를 runtime에 추가한다

Proxy framework가 interface implementation class를 동적으로 생성하면 class loading, verification, JIT compilation, metadata memory가 추가된다. 수천 개 unique proxy shape가 생기면 code cache와 class metadata pressure가 커질 수 있다. Reusable proxy class와 per-instance handler data를 분리하면 shape explosion을 줄일 수 있다. Runtime-generated artifact도 build-time code와 마찬가지로 versioning·debug symbol·profiling strategy가 필요하다.

## CHAPTER 23 · monitor/lock state는 object header 또는 side structure와 연결될 수 있다

Managed object의 synchronization primitive가 object identity에 붙어 있으면 uncontended fast lock state를 header bit로 표현하고 contention 시 heavyweight monitor structure로 inflate하는 설계를 사용할 수 있다. 구체 mechanism은 runtime마다 다르지만 lock을 한 번 사용했다는 사실이 object metadata state와 native resource를 바꿀 수 있다. Identity hash와 header bit 사용이 충돌해 side metadata가 필요해질 수도 있다. Synchronization overhead를 object size와 별개로 추적한다.

## CHAPTER 24 · identity hash는 value hash와 다르며 object movement와 호환되어야 한다

Object가 moving GC로 주소를 바꾸더라도 identity hash가 주소 기반으로 변하면 language contract가 깨질 수 있다. Runtime은 hash를 header/side table에 저장하거나 stable derivation을 사용해야 한다. Identity-based map은 value equality와 다른 semantics를 가지며 object lifetime 동안 stable해야 한다. Address를 object identity로 외부에 노출하는 unsafe/native code는 moving runtime과 충돌한다.

## CHAPTER 25 · GC write/read barrier는 object field access에 hidden runtime work를 추가한다

Concurrent/generational collector는 reference store 시 card marking, SATB logging 같은 write barrier를 삽입하거나 load 시 read barrier를 사용할 수 있다. Source에 단순 assignment 하나만 보여도 machine code에는 barrier check와 metadata store가 포함될 수 있다. Pointer-heavy graph mutation이 primitive arithmetic보다 비싼 이유 중 하나다. Collector 선택·heap phase에 따라 barrier cost가 달라지므로 allocation benchmark와 mutation benchmark를 분리한다.

## CHAPTER 26 · moving GC는 object address 안정성과 handle design에 영향을 준다

Collector가 compaction/copying으로 object를 이동하면 모든 managed reference를 update하거나 forwarding mechanism을 사용해야 한다. Native code가 raw address를 오래 보관하면 relocation과 충돌하므로 pinning, handle, critical section 제한이 필요하다. Pinning된 object가 많으면 collector의 compaction freedom과 region evacuation 성공률이 떨어질 수 있다. FFI API는 raw pointer lifetime을 가능한 짧게 하고 managed owner를 keep-alive하도록 한다.

## CHAPTER 27 · weak/soft/phantom reference는 reachability level과 cleanup semantics가 다르다

GC runtime은 strong reference 외에 collector가 object lifetime을 줄일 수 있는 특수 reference category를 제공할 수 있다. Weak reference는 canonical map/cache에 유용하지만 collection timing을 business logic synchronization으로 사용하면 nondeterministic하다. Finalization/phantom cleanup은 external resource의 primary release mechanism으로 삼지 않고 deterministic owner를 우선한다. Collector queue 처리 지연과 reference processing pause도 관측 대상이다.

## CHAPTER 28 · object graph locality는 logical relation과 physical proximity가 다를 수 있다

Parent가 child reference를 가진다고 해서 두 object가 같은 cache line/page에 있는 것은 아니다. Allocation timing과 GC compaction policy가 physical placement를 결정한다. Pointer-rich tree/graph traversal은 각 edge마다 cache miss와 TLB miss를 유발할 수 있다. Hot immutable data를 flat array/packed struct에 옮기거나 object pool에서 근접 배치하면 locality를 높일 수 있지만 mutation flexibility와 GC simplicity를 교환한다.

## CHAPTER 29 · heap dump에서 shallow size와 retained size를 구분한다

Shallow size는 object 자체 header+field 공간이고 retained size는 그 object가 사라지면 함께 unreachable될 object graph 크기다. 작은 manager object 하나가 large cache를 retain하면 shallow size만 보고 원인을 놓친다. Dominator tree는 어떤 object가 memory retention을 지배하는지 보여 준다. Runtime metadata/native allocation은 heap dump 밖에 있을 수 있으므로 RSS/PSS/native allocator/GC heap을 함께 본다.

## CHAPTER 30 · managed object 성능 계약은 layout·dispatch·GC·runtime version을 함께 고정한다

Object-heavy hotspot을 최적화할 때 shallow size, object count, reference count, allocation rate, dispatch polymorphism, barrier traffic, cache miss를 한 번에 수집한다. Runtime upgrade로 header/reference compression/JIT dispatch policy가 바뀔 수 있으므로 layout assumption을 regression gate에 직접 하드코딩하지 않는다. Public API는 value semantics와 ownership을 표현하고 implementation layout은 profiler evidence에 따라 바꿀 수 있게 둔다. 최적화 성공은 source class 수가 줄었다는 사실이 아니라 sustained heap footprint·GC time·CPU/cache behavior 개선으로 판정한다.