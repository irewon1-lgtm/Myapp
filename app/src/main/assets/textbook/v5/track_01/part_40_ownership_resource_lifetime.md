# PART 40 · Ownership and Resource Lifetime — RAII, borrowing, refcount, pinning

Memory management은 `언제 free할 것인가`만의 문제가 아니다. File descriptor, socket, lock, transaction, GPU buffer, process handle처럼 외부 resource도 생성·공유·이전·종료 시점이 존재한다. Lifetime design은 누가 resource를 소유하고, 누가 빌려 쓰며, ownership transfer가 언제 일어나고, 실패·취소·exception에서도 어떤 cleanup이 보장되는지를 타입과 API 계약으로 표현하는 문제다.

## CHAPTER 01 · resource lifetime과 heap allocation lifetime을 분리한다

Heap memory는 resource lifetime의 한 종류일 뿐이다. File descriptor는 kernel object에 대한 process-local reference이고, mutex guard는 critical section 진입 권한이며, database transaction은 remote/local state transition의 범위다. 이들은 GC가 object를 회수한다고 자동으로 올바르게 종료되는 것이 아니다. Resource가 scarce하거나 외부 side effect를 가진다면 deterministic release가 필요할 수 있다. API 설계에서 `객체가 더 이상 reachable하지 않음`과 `resource 사용을 끝냈음`을 같은 사건으로 취급하지 않는다.

## CHAPTER 02 · single ownership은 destruction authority를 한 곳에 둔다

하나의 resource를 정확히 한 owner가 관리하면 누가 release해야 하는지가 명확하다. Owner가 lifetime을 끝내거나 ownership을 다른 object로 transfer할 때만 destruction authority가 이동한다. 여러 pointer가 같은 address를 볼 수 있어도 ownership은 하나일 수 있다. 이 구분이 없으면 `모두가 close할 수 있음`과 `아무도 close하지 않음`이라는 두 반대 failure가 생긴다. API signature와 type이 owner와 observer를 구분하면 cleanup 책임을 code review에서 추적하기 쉬워진다.

## CHAPTER 03 · move는 값 복사와 ownership transfer를 구분한다

Resource handle을 bitwise copy하면 두 object가 같은 underlying resource를 자신이 소유한다고 착각할 수 있다. Move semantics는 source의 ownership을 destination으로 이전하고 source를 non-owning/empty state로 바꾸는 계약이다. Move 이후 source object가 어떤 operation을 허용하는지 type invariant로 정의해야 한다. File/socket/mutex-like resource는 copy를 금지하고 move만 허용하는 설계가 destruction authority를 명확하게 유지한다.

## CHAPTER 04 · borrowing은 ownership을 넘기지 않고 제한된 접근 권한만 전달한다

Borrowed reference는 callee가 resource lifetime을 연장하거나 destroy할 권한이 없다는 의미를 가질 수 있다. Borrower가 owner보다 오래 살아남으면 dangling reference가 되므로 lifetime relation이 핵심이다. Rust의 borrow model처럼 compile time에 alias/lifetime 제약을 강하게 검증할 수도 있고, 다른 언어에서는 convention·type wrapper·runtime check에 의존할 수 있다. Borrow API는 `주소를 전달한다`가 아니라 permission과 lifetime boundary를 전달한다.

## CHAPTER 05 · mutable alias는 lifetime보다 더 강한 correctness 제약을 만든다

두 곳이 동시에 같은 state를 변경할 수 있으면 data race뿐 아니라 logical invariant violation이 발생한다. `한 시점에 unique mutable access`라는 규칙은 mutation 권한을 독점시켜 reasoning scope를 줄인다. 여러 immutable observer와 하나의 mutable owner를 구분하는 모델은 compiler optimization에도 alias 정보를 제공할 수 있다. Ownership system의 가치는 free 시점뿐 아니라 누가 state를 바꿀 수 있는지를 제한하는 데 있다.

## CHAPTER 06 · lexical scope와 actual resource lifetime은 반드시 같지 않다

Stack variable이 scope를 벗어날 때 destructor가 호출되는 언어에서는 lexical scope가 cleanup boundary가 될 수 있다. 하지만 ownership을 heap object, closure, async task로 이동하면 resource lifetime은 원래 scope보다 길어진다. Conversely early release를 원하면 nested scope나 explicit ownership reset이 필요할 수 있다. Scope와 resource lifetime을 무조건 동일시하지 말고 capture, move, shared ownership이 lifetime을 어디까지 확장하는지 추적한다.

## CHAPTER 07 · RAII는 acquisition 성공과 cleanup guarantee를 object invariant에 묶는다

RAII object는 construction이 성공한 순간부터 resource를 유효하게 소유하고 destruction에서 release한다. 중간 operation이 exception이나 early return으로 빠져도 stack unwinding 과정에서 이미 완성된 owner object가 정리된다. 핵심은 destructor가 있다는 사실이 아니라 `partially acquired resource가 외부에 노출되지 않고, fully constructed owner는 항상 cleanup responsibility를 가진다`는 invariant다. Lock guard, file wrapper, transaction guard가 대표적인 적용이다.

## CHAPTER 08 · destructor order는 dependency graph와 일치해야 한다

여러 resource가 서로 의존하면 destruction 순서가 correctness를 결정한다. B가 A를 사용한다면 보통 B가 먼저 종료되고 A가 나중에 release되어야 한다. C++ member/base destruction order처럼 언어가 정한 규칙과 field declaration order를 모르면 shutdown 시 use-after-destroy가 생길 수 있다. Composite owner는 acquisition order와 reverse cleanup order를 함께 설계한다. Global/static object 사이 implicit dependency는 shutdown-order bug를 만들기 쉬워 가능한 한 명시적 owner graph로 바꾼다.

## CHAPTER 09 · exception safety는 leak 방지보다 state transition atomicity까지 포함한다

Operation 도중 exception이 발생했을 때 resource만 정리돼도 object invariant가 깨져 있으면 안전하지 않다. Basic guarantee는 leak 없이 valid state를 유지하고, strong guarantee는 실패 시 observable state가 operation 전과 같도록 rollback하는 수준을 목표로 할 수 있다. Commit point 이전에 temporary owner가 resource를 보관하고 성공 시에만 main state로 swap하는 pattern은 partial update를 줄인다. Exception-safety 수준을 API contract에 맞춰 선택한다.

## CHAPTER 10 · unique owner pointer는 nullability와 destruction policy까지 가진 타입이다

Unique ownership wrapper는 copy를 금지하고 move만 허용해 하나의 destruction authority를 유지한다. Custom deleter를 통해 heap memory 외 resource도 관리할 수 있다. Raw pointer를 별도로 노출할 때는 non-owning view인지 ownership escape인지 문서화한다. `pointer가 하나뿐이다`가 unique ownership의 전부가 아니라, type system이 duplicate destruction authority 생성을 막고 cleanup policy를 owner object 안에 보관하는 것이 핵심이다.

## CHAPTER 11 · reference counting은 owner 수를 runtime state로 유지한다

Shared ownership은 여러 owner가 같은 object를 독립적으로 보유할 수 있게 하고 마지막 strong reference가 사라질 때 object를 destroy한다. 이 방식은 lifetime을 local scope에서 추론하기 어렵게 만드는 대신 deterministic한 마지막-release 시점을 제공할 수 있다. Count update 자체가 metadata access이며 multi-threaded shared pointer는 atomic operation을 필요로 할 수 있다. Hot path에서 작은 object를 대량 복사하면 refcount traffic이 성능 병목이 될 수 있다.

## CHAPTER 12 · atomic refcount는 cache coherence traffic을 만든다

여러 core가 같은 control block의 count를 증가·감소시키면 atomic read-modify-write와 cache-line ownership transfer가 반복된다. Object payload를 거의 읽지 않아도 reference 복사만으로 coherence bottleneck이 생길 수 있다. Per-thread batching이나 ownership model 변경이 필요한 경우가 있다. Refcount benchmark는 payload operation뿐 아니라 retain/release count, core migration, shared control-block cache line을 함께 측정해야 한다.

## CHAPTER 13 · strong-reference cycle은 count가 0이 되지 않는 구조적 leak이다

A가 B를 strong-reference하고 B가 A를 strong-reference하면 외부 owner가 사라져도 두 count는 남는다. 이는 `collector가 늦게 돈다`가 아니라 reference-count reachability model이 cycle을 자동 해제하지 못하는 구조다. Graph에서 어떤 edge가 lifetime을 소유하고 어떤 edge가 observation/relation만 표현하는지 구분해야 한다. Parent-child, callback-owner, delegate graph는 cycle이 생기기 쉬운 영역이다.

## CHAPTER 14 · weak reference는 lifetime을 연장하지 않는 graph edge다

Weak reference는 대상 object를 가리킬 수 있지만 strong ownership count에 포함되지 않는다. Target destruction 이후 weak access가 null/expired로 안전하게 판정되도록 control metadata가 필요하다. Weak reference는 cycle breaker지만 target이 사라질 수 있다는 branch를 모든 access에서 처리해야 한다. `weak로 바꾸면 leak 해결`이 아니라 domain상 그 edge가 실제로 ownership이 아닌지 먼저 확인한다.

## CHAPTER 15 · ARC도 reference counting이므로 ownership annotation과 cycle discipline이 필요하다

Automatic Reference Counting은 compiler/runtime가 retain/release를 삽입해 manual count update를 줄이지만 graph semantics 자체는 사라지지 않는다. Strong/weak/unowned 같은 reference category가 object lifetime relation을 표현한다. Capturing closure가 owner를 strong하게 잡으면 예상보다 lifetime이 길어질 수 있다. Compiler automation은 bookkeeping을 자동화할 뿐 ownership graph 설계를 대신하지 않는다.

## CHAPTER 16 · copy-on-write는 shared immutable state와 mutation ownership을 분리한다

여러 logical owner가 같은 backing storage를 읽는 동안 공유하고, mutation이 발생할 때 unique copy를 만드는 방식은 cheap copy semantics와 value isolation을 동시에 노릴 수 있다. 그러나 uniqueness check가 atomic/refcount metadata와 연결될 수 있고, hidden copy가 large object에서 latency spike를 만들 수 있다. API가 value type처럼 보여도 mutation 시 O(n) copy가 발생할 수 있으므로 performance contract에 이를 포함한다.

## CHAPTER 17 · interior mutability는 immutable reference 아래 mutation을 허용하는 별도 synchronization contract다

Public reference가 immutable해 보여도 내부 cache, lazy initialization, refcount처럼 mutable metadata가 존재할 수 있다. 이런 mutation이 thread-safe한지, runtime borrow check를 사용하는지, atomic/lock으로 보호하는지 명시해야 한다. `const/immutable view`가 physical bit immutability를 항상 뜻하지 않는다. Logical constness와 internal synchronization을 분리하면 API semantics를 유지하면서 implementation optimization을 적용할 수 있다.

## CHAPTER 18 · pinning은 object address가 움직이지 않아야 하는 lifetime condition을 표현한다

Self-referential structure, async state machine, DMA registration처럼 object가 이동하면 내부 pointer/외부 registration이 깨지는 경우가 있다. Pinning은 단순 `heap에 둔다`보다 강한 contract로, 특정 lifetime 동안 storage location을 안정적으로 유지한다. Moving GC나 container relocation과 충돌할 수 있으므로 pin 수명과 범위를 최소화한다. Pinning이 많으면 compaction/allocator 자유도가 줄어 fragmentation을 키울 수 있다.

## CHAPTER 19 · self-referential object는 move 가능성과 borrow lifetime을 동시에 제한한다

Object 내부 pointer가 같은 object의 field를 가리키면 object relocation 후 pointer가 old address를 참조할 수 있다. Construction 중 address가 최종 위치인지, move operation을 금지할지, offset 기반 reference를 사용할지 선택해야 한다. Async coroutine state처럼 compiler가 self-reference를 만들 수 있는 구현에서는 pinning이나 stable storage가 correctness 조건이 된다. Self-reference는 ordinary struct보다 lifetime proof가 복잡하다.

## CHAPTER 20 · generation handle은 dangling raw pointer 대신 identity version을 검증한다

Resource table에서 slot index만 handle로 사용하면 slot이 free된 뒤 새 object에 재사용될 때 stale handle이 새 object를 잘못 가리킬 수 있다. Index와 generation counter를 함께 저장하면 access 시 version mismatch를 검출할 수 있다. Game engine, kernel-like table, async operation registry에서 유용하다. ABA 문제와 유사하게 `주소/번호가 같음`과 `같은 lifetime의 object임`을 분리한다.

## CHAPTER 21 · typestate는 resource lifecycle의 허용 operation을 타입으로 나눈다

Socket이 `Created → Bound → Listening`, transaction이 `Active → Committed/RolledBack`처럼 state에 따라 가능한 operation이 다르면 하나의 mutable enum보다 별도 type/state parameter로 표현할 수 있다. 잘못된 순서를 compile time에 금지할 수 있고 state transition이 ownership transfer와 함께 일어난다. Typestate가 지나치게 세분되면 API 복잡성이 커지므로 safety-critical transition에 집중한다.

## CHAPTER 22 · file/socket ownership은 descriptor duplication까지 포함한다

File descriptor를 dup하거나 process fork로 상속하면 서로 다른 integer descriptor가 같은 kernel open-file description을 reference할 수 있다. 한 descriptor close가 underlying object를 즉시 destroy하지 않을 수 있다. User-space owner model은 kernel reference model과 일치해야 한다. `fd 숫자 하나 = resource 하나`로 단순화하면 offset sharing, shutdown, inheritance bug를 설명하지 못한다.

## CHAPTER 23 · lock guard는 mutual exclusion과 lifetime을 결합한다

Mutex lock을 수동 acquire/release하면 early return·exception·cancellation에서 unlock 누락이 생길 수 있다. Guard owner가 scope lifetime 동안 lock ownership을 보유하고 destruction에서 release하면 control-flow path와 cleanup을 분리할 수 있다. Guard를 async suspension을 넘겨 유지하면 long critical section과 priority inversion을 만들 수 있으므로 lock lifetime이 await boundary를 지나지 않는지 별도 검토한다.

## CHAPTER 24 · async task는 captured resource의 lifetime을 원래 caller보다 길게 만들 수 있다

Closure가 file/socket/UI object를 capture한 채 background task로 이동하면 caller scope가 끝나도 captured owner가 resource를 유지한다. 반대로 raw/non-owning reference를 capture하면 task가 실행될 때 owner가 이미 사라졌을 수 있다. Async API는 `task completion/cancellation까지 무엇을 소유하는가`를 명시해야 한다. Structured concurrency는 child task lifetime을 parent scope에 묶어 이런 ownership graph를 단순화한다.

## CHAPTER 25 · cancellation은 cleanup path이지 단순 boolean flag가 아니다

Operation이 resource를 부분적으로 획득한 뒤 cancellation되면 open handle, temporary file, lock, reservation, remote request를 정리해야 한다. Cancellation point마다 invariant를 유지하고 cleanup이 idempotent해야 race에서 double release를 막을 수 있다. Cancel signal을 받은 순간과 operation이 실제로 종료된 순간을 구분한다. Caller가 completion을 기다리지 않고 owner를 파괴하면 use-after-free가 발생할 수 있다.

## CHAPTER 26 · callback registration은 callback과 target의 양방향 lifetime 관계를 만든다

Event source가 callback closure를 strong하게 보관하고 closure가 target object를 capture하면 cycle이 생길 수 있다. 반대로 source가 raw callback pointer만 보관하면 target destruction 후 dangling invocation 위험이 있다. Registration token/RAII subscription, weak capture, explicit unregister를 사용해 ownership 방향을 명확히 한다. Callback API는 invocation thread와 unsubscribe synchronization도 함께 정의해야 한다.

## CHAPTER 27 · FFI boundary는 allocator와 ownership convention을 명시적으로 합의해야 한다

한 language/runtime에서 allocate한 memory를 다른 allocator로 free하면 corruption이 발생할 수 있다. C ABI를 넘길 때 누가 allocate하고 누가 release하는지, pointer가 borrowed인지 transferred인지, callback user-data lifetime이 얼마인지 문서화한다. GC-managed object를 native pointer로 넘기면 movement/pinning과 collection reachability 문제도 생긴다. FFI wrapper는 raw pointer를 high-level safe owner type으로 즉시 감싸는 것이 좋다.

## CHAPTER 28 · region/arena ownership은 많은 object의 lifetime을 하나로 묶는다

개별 object free를 추적하지 않고 request/phase 단위 arena가 모든 allocation을 소유한 뒤 한 번에 해제하면 allocation metadata와 fragmentation을 줄일 수 있다. 대신 일부 object만 더 오래 살아야 하면 arena 전체 lifetime이 늘어 memory retention이 커진다. Region boundary가 실제 workload lifetime과 일치하는지 확인한다. Arena pointer가 region 밖으로 escape하지 않는 invariant가 핵심이다.

## CHAPTER 29 · leak과 premature free는 반대 방향의 lifetime failure다

Resource를 오래 유지하면 leak/retention이고 너무 일찍 release하면 dangling access/use-after-free다. Refcount를 무조건 올리면 후자는 줄지만 전자가 늘고, raw pointer를 남발하면 overhead는 낮지만 proof burden이 커진다. Lifetime design은 `안전하면 오래 잡아도 됨`이 아니라 memory/resource budget과 latency를 포함한 균형 문제다. Heap profile, fd count, retain graph, sanitizer evidence를 함께 본다.

## CHAPTER 30 · ownership 계약은 create·borrow·transfer·release 네 사건을 추적 가능하게 만든다

API마다 resource를 누가 생성하고, borrowed access가 어느 범위에서 유효하며, transfer 뒤 source가 어떤 상태가 되고, release를 누가 한 번만 수행하는지 문서와 type으로 고정한다. Async/FFI/callback처럼 lifetime이 비국소적인 경계에는 regression test와 sanitizer를 붙인다. Reference graph가 복잡해질수록 strong edge를 기본으로 늘리는 대신 실제 domain ownership을 먼저 그린다. 수명 correctness는 GC 여부와 무관하게 모든 시스템의 기본 invariant다.