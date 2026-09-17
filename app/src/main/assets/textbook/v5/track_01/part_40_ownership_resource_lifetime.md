# PART 40 · Ownership and Resource Lifetime — RAII, borrowing, refcount, pinning

resource bug의 핵심은 allocate/free API 이름이 아니라 **누가 현재 소유자인가, 누가 임시로 빌려 쓰는가, 언제 lifetime이 끝나는가, 비동기·예외·FFI 경계에서 누가 cleanup 책임을 이어받는가**다. memory뿐 아니라 fd, lock, callback, device buffer도 같은 질문을 가진다. 이 PART는 lifetime을 코드 구조와 runtime evidence로 증명하는 방법을 다룬다.

---

## CHAPTER 01 · resource lifetime은 생성과 파괴 사이의 유효한 사용 구간이다

resource가 존재하는 동안에는 handle이 실제 object를 가리키고 필요한 invariant가 유지되어야 한다. lifetime이 끝난 뒤 같은 address나 integer ID가 재사용될 수 있으므로 값이 같다는 사실만으로 같은 resource identity를 의미하지 않는다.

memory, file descriptor, socket, lock, GPU buffer는 서로 다른 cleanup primitive를 사용하지만 stale handle과 double release라는 공통 failure를 가진다. logical operation이 끝났다고 resource lifetime이 자동으로 끝나는 것도 아니다.

디버깅에서는 allocate/open, transfer, use, close/free event를 generation과 함께 기록한다. 마지막 crash point보다 첫 ownership 위반을 찾는다.

---

## CHAPTER 02 · single-owner 모델은 mutation과 destruction 책임을 한 곳에 모은다

resource에 명확한 단일 owner가 있으면 누가 파괴할 수 있는지와 언제 다른 code가 접근을 중단해야 하는지가 단순해진다. shared global reference가 많아질수록 lifetime proof가 어려워진다.

owner를 function scope, object, request context 중 어디에 둘지는 resource가 실제로 필요한 기간과 맞춰야 한다. 너무 긴 owner는 leak을 만들고 너무 짧은 owner는 borrow가 dangling 된다.

code review에서는 “누가 free하나”보다 transfer 전후 owner identity를 확인한다. error path에서도 같은 owner rule이 유지되는지 본다.

---

## CHAPTER 03 · move transfer는 resource identity를 복사하지 않고 ownership을 넘긴다

unique resource를 다른 object나 scope로 넘길 때 원래 owner가 계속 release 권한을 가지면 double free가 생긴다. move semantics는 ownership을 새 위치로 이전하고 old handle을 invalid 또는 empty state로 만드는 모델이다.

OS handle이나 C pointer는 language가 move를 강제하지 않을 수 있어 convention과 wrapper가 필요하다. vector/container reallocation처럼 object 위치가 바뀔 때 external pointer가 stale 되는 문제도 구분한다.

transfer API는 success/failure 시 owner가 누구인지 명시한다. fault injection으로 중간 실패를 넣어 release가 정확히 한 번인지 확인한다.

---

## CHAPTER 04 · borrowing은 ownership 없이 제한된 기간 접근 권한만 제공한다

borrower는 resource를 사용할 수 있지만 lifetime을 종료할 권한은 없다. 안전 조건은 borrow가 owner보다 오래 살아남지 않고 mutable access 규칙이 invariant를 깨지 않는 것이다. C pointer나 callback reference에서는 compiler가 이를 자동 증명하지 못할 수 있다.

async task가 local object를 capture해 owner scope 이후 실행되면 borrow가 dangling 된다. FFI가 pointer를 저장하는지 즉시 사용하는지 모르면 호출자는 lifetime을 결정할 수 없다.

API 문서에 borrowed/retained semantics를 명시한다. sanitizer와 delayed callback test로 owner destruction 뒤 접근을 찾는다.

---

## CHAPTER 05 · mutable alias는 동시에 같은 state를 바꾸는 경로를 늘린다

하나의 object를 여러 mutable reference가 공유하면 lifetime뿐 아니라 ordering과 invariant 문제가 생긴다. data race가 없더라도 logical update가 서로 덮어쓸 수 있다. unique mutable access 또는 synchronization이 필요하다.

immutable alias는 reasoning을 단순화하지만 내부 cache처럼 hidden mutation이 있다면 thread-safety contract가 다시 필요하다. copy-on-write는 공유 상태를 mutation 시점에 분리하는 한 방법이다.

ownership graph와 mutation entry point를 함께 그린다. bug fix로 lock만 추가하기 전에 mutable alias 자체를 줄일 수 있는지 검토한다.

---

## CHAPTER 06 · scope lifetime은 lexical block과 실제 resource 필요 기간을 맞추는 도구다

local variable이 scope를 벗어날 때 cleanup되는 구조는 resource release를 control-flow와 결합해 누락을 줄인다. 하지만 callback, returned reference, global registry가 object를 scope 밖으로 탈출시키면 lexical lifetime과 runtime lifetime이 달라진다.

large object를 unnecessarily outer scope에 두면 memory peak가 증가하고 lock guard를 오래 유지하면 contention이 커진다. 반대로 inner scope에서 만든 buffer를 async operation에 넘기면 use-after-free가 생길 수 있다.

scope shortening은 readability뿐 아니라 resource pressure 최적화다. profiler에서 lifetime histogram과 peak concurrency를 본다.

---

## CHAPTER 07 · RAII는 acquisition과 deterministic cleanup을 object lifetime에 묶는다

RAII 계열은 object construction에서 resource를 획득하고 destructor/drop에서 release해 return·exception 경로에서도 cleanup이 실행되게 한다. manual `open→...→close`보다 control-flow 누락이 줄어든다.

constructor가 부분 성공한 뒤 실패할 때 이미 획득한 subresource를 정리해야 한다. destructor가 exception을 던지거나 blocking work를 오래 하면 또 다른 문제가 생긴다.

wrapper가 실제 ownership을 갖는지 단순 view인지 이름과 type으로 구분한다. error injection으로 모든 early return에서 release 여부를 검사한다.

---

## CHAPTER 08 · destructor order는 dependency가 있는 resource의 teardown 순서를 결정한다

여러 member가 서로 의존하면 파괴 순서가 correctness에 영향을 준다. queue가 worker보다 먼저 파괴되거나 logger가 thread보다 먼저 종료되면 cleanup 중 use-after-free가 생길 수 있다.

static/global destruction은 translation unit과 runtime 종료 순서가 복잡해 특히 위험하다. explicit shutdown phase가 더 안전한 경우가 많다.

shutdown trace에 component stop, drain, close 시각을 남긴다. normal exit뿐 아니라 partial initialization 실패도 같은 order를 따라야 한다.

---

## CHAPTER 09 · exception safety는 중간 실패 뒤 resource와 invariant가 어떤 상태인지 정의한다

operation 도중 exception이나 error가 발생해도 leak 없이 object invariant를 유지해야 한다. strong guarantee는 실패 시 이전 state를 보존하고 basic guarantee는 valid하지만 변경된 state를 허용할 수 있다.

side effect가 여러 system에 걸치면 language exception만으로 rollback이 되지 않는다. transaction이나 compensation이 필요하다.

각 failure point에 fault injection을 넣어 owner count와 external state를 검사한다. catch에서 error를 삼키는 것보다 state guarantee를 명시한다.

---

## CHAPTER 10 · unique owner type은 release 권한의 중복을 type 수준에서 줄인다

unique pointer/handle wrapper는 복사 대신 move만 허용해 double owner를 방지할 수 있다. raw pointer를 반환하더라도 ownership이 아니라 borrow임을 API로 구분해야 한다.

unique wrapper 안의 resource가 thread로 이동할 때 thread-safety requirement도 함께 이동한다. custom deleter는 어떤 allocator/domain에 반환해야 하는지 identity를 보존해야 한다.

type 변환에서 ownership 정보가 `void*`나 integer handle로 사라지는 지점을 audit한다. FFI boundary가 대표적인 위험점이다.

---

## CHAPTER 11 · reference counting은 owner 수를 runtime state로 추적한다

refcount는 strong reference가 생길 때 증가하고 사라질 때 감소해 count가 zero가 되면 object를 reclaim한다. tracing GC 없이 deterministic lifetime을 얻을 수 있지만 각 reference update에 비용이 있고 cycle을 자동 해결하지 못한다.

누가 count를 올리는지 명확하지 않으면 leak이 생기고 raw pointer가 count 없이 탈출하면 premature free가 가능하다. borrowed reference와 owning reference를 구분해야 한다.

refcount leak은 snapshot count보다 retain/release callsite를 추적한다. hot object의 atomic refcount contention도 performance issue가 될 수 있다.

---

## CHAPTER 12 · atomic refcount는 thread-safe lifetime과 coherence 비용을 교환한다

여러 thread가 같은 object의 owner를 추가·제거하면 refcount update가 atomic이어야 할 수 있다. single cache line에 높은 빈도로 RMW가 발생하면 core 사이 ownership traffic이 커진다.

atomic refcount가 object 내부 state까지 thread-safe하게 만드는 것은 아니다. 마지막 decrement와 destruction 사이 memory ordering도 구현 contract를 따라야 한다.

profile에서 refcount operation hotspot과 cache-to-cache traffic을 확인한다. unnecessary shared ownership을 local/borrowed reference로 줄일 수 있는지 본다.

---

## CHAPTER 13 · strong cycle은 refcount가 zero에 도달하지 못하게 한다

A와 B가 서로 strong reference를 가지면 외부 owner가 사라져도 count가 남아 object가 reclaim되지 않는다. listener, parent-child graph, closure capture에서 흔하다.

cycle을 weak edge로 끊거나 explicit lifecycle을 도입해야 한다. 모든 edge를 weak로 바꾸면 필요한 lifetime이 부족해질 수 있으므로 graph ownership 의미를 먼저 정의한다.

heap graph에서 root 없는 refcount cycle을 찾는다. 종료 시 leak count만 보지 않고 object relation을 본다.

---

## CHAPTER 14 · weak reference는 object lifetime을 연장하지 않는 관찰 handle이다

weak reference는 target이 살아 있을 때만 strong reference로 upgrade해 안전하게 사용해야 한다. existence check와 use 사이 target이 파괴될 수 있으므로 atomic upgrade primitive가 중요하다.

cache와 observer registry에서 weak reference는 leak을 줄이지만 target disappearance를 정상 상태로 처리해야 한다. callback queue에 raw target을 넣으면 weak semantics가 사라진다.

upgrade 실패 path를 테스트한다. UI/lifecycle code에서 target이 이미 사라진 상황을 error로 보지 말고 contract로 다룬다.

---

## CHAPTER 15 · ARC 계열은 compile/runtime이 reference counting을 자동 삽입해도 ownership reasoning은 남는다

automatic reference counting은 retain/release 삽입을 도와 manual error를 줄이지만 strong cycle과 callback capture 문제는 해결되지 않는다. compiler optimization이 redundant count를 제거해 성능을 개선할 수 있어 source와 runtime count가 직접 대응하지 않을 수 있다.

unowned/weak reference 선택은 target lifetime assumption을 코드에 박는다. 잘못된 unowned assumption은 dangling access로 이어진다.

leak profiler와 ownership graph를 함께 사용한다. automation을 lifetime proof의 대체물로 보지 않는다.

---

## CHAPTER 16 · copy-on-write는 shared immutable state를 mutation 시점에 분리한다

여러 owner가 같은 backing storage를 읽기만 할 때 공유하고 한 쪽이 수정하려 하면 private copy를 만드는 방식이다. 메모리와 copy 비용을 줄일 수 있지만 hidden alias가 있어 uniqueness check와 synchronization이 필요하다.

large buffer를 작은 수정마다 copy하면 latency spike가 생긴다. slice/view가 backing storage 전체 lifetime을 유지하는 경우 메모리 retention도 증가한다.

copy 발생 count와 bytes를 profile한다. API 사용자가 value semantics를 기대하는지 명확히 보장한다.

---

## CHAPTER 17 · interior mutability는 외부 immutable view 안에 통제된 mutation을 허용한다

cache, lazy initialization, synchronization primitive는 logical value가 immutable해 보여도 내부 state를 변경할 수 있다. 이를 안전하게 제공하려면 mutation을 lock/atomic/runtime borrow rule로 보호해야 한다.

“const reference니까 thread-safe”라고 가정하면 hidden mutation에서 race가 생긴다. observably immutable과 physically immutable을 구분한다.

API contract에 concurrent access 가능 여부를 적는다. race detector로 lazy-init과 cache path를 별도 검사한다.

---

## CHAPTER 18 · pinning은 object address가 lifetime 중 이동하지 않는다는 추가 보장이다

self-reference, DMA, async kernel operation처럼 address stability가 필요한 경우 object를 move하지 못하게 pinning할 수 있다. 이는 allocator와 compactor 자유도를 줄이고 memory fragmentation을 늘릴 수 있다.

pointer를 외부에 노출한 뒤 object를 vector reallocation이나 moving GC가 이동하면 stale pointer가 된다. pin은 필요 기간만 유지해야 한다.

pinned bytes와 duration을 관찰한다. 장기 pin이 memory pressure와 GC pause에 미치는 영향도 측정한다.

---

## CHAPTER 19 · self-reference는 object 이동과 partial initialization에 민감하다

object 내부 field가 같은 object의 다른 field 주소를 저장하면 construction 완료 전에 이동되거나 copy될 때 pointer가 깨질 수 있다. 일반 move/copy semantics와 상충한다.

self-referential async state machine은 pinning이나 indirection으로 stable address를 제공해야 한다. destructor에서 내부 pointer가 여전히 valid한지도 확인한다.

construction, move, destruction test를 의도적으로 수행한다. code review에서 raw interior pointer를 특별 취급한다.

---

## CHAPTER 20 · generation handle은 ID 재사용으로 생기는 stale reference를 막는다

slot index나 integer ID는 object가 파괴된 뒤 새 object에 재사용될 수 있다. handle에 generation을 함께 넣으면 old reference가 같은 slot의 새 object를 잘못 가리키는 것을 detect할 수 있다.

게임 entity, connection table, descriptor cache에서 address/ID reuse가 빠르면 ABA와 유사한 문제가 생긴다. generation wraparound와 concurrency도 고려한다.

lookup 실패를 crash보다 정상 stale-handle error로 처리한다. 로그에 index와 generation을 함께 남긴다.

---

## CHAPTER 21 · typestate는 lifetime phase별 허용 operation을 type으로 나눈다

resource가 Created→Connected→Closed 같은 state를 가진다면 모든 method에서 runtime flag를 검사하기보다 state별 type/API를 분리할 수 있다. invalid operation을 compile time에 줄이고 transition에서 ownership을 넘긴다.

state explosion이 큰 protocol에 과도한 type을 만들면 usability가 떨어진다. security-sensitive key, transaction, socket handshake처럼 invalid sequence 비용이 큰 곳에 우선 적용한다.

test는 transition graph를 기준으로 작성한다. closed object가 raw escape hatch로 다시 사용되지 않는지 확인한다.

---

## CHAPTER 22 · file descriptor ownership은 작은 integer와 kernel object lifetime을 분리한다

fd 숫자는 process table의 slot이며 close 뒤 다른 file/socket에 빠르게 재사용될 수 있다. stale async callback이 old fd number를 사용하면 전혀 다른 resource에 operation을 수행할 수 있다.

dup/fork는 같은 open file description을 공유할 수 있어 offset과 status flag semantics가 연결된다. “fd가 다르다=독립 resource”도 항상 참이 아니다.

wrapper에 generation/ownership을 두고 close 이후 raw fd 사용을 제한한다. trace에는 inode/socket identity를 함께 남긴다.

---

## CHAPTER 23 · lock guard는 acquire와 release를 lexical lifetime에 묶는다

lock guard는 critical section을 scope로 표현해 early return과 exception에서도 unlock을 보장한다. 하지만 guard scope가 너무 넓으면 blocking I/O와 callback까지 lock을 보유해 contention·deadlock이 증가한다.

guard object를 다른 scope로 move하거나 condition wait에서 일시 release하는 semantics도 정확히 알아야 한다. lock lifetime과 protected invariant boundary를 맞춘다.

hold-time distribution과 guard callsite를 측정한다. 단순 lock count보다 longest hold를 본다.

---

## CHAPTER 24 · async lifetime은 logical task가 physical stack보다 오래 살아갈 수 있게 한다

coroutine이나 callback은 caller stack이 반환된 뒤에도 실행될 수 있어 local reference를 capture하면 dangling lifetime이 생긴다. task가 owner object보다 오래 살지, owner가 task를 cancel/drain할지 명시해야 한다.

UI lifecycle·request scope가 끝났는데 background result가 old state를 수정하면 memory-safe해도 logical lifetime bug다. generation과 structured concurrency가 필요하다.

pending task와 owner generation을 trace한다. shutdown에서 모든 child task가 수렴하는지 검증한다.

---

## CHAPTER 25 · cancellation cleanup은 partially completed work의 resource를 회수해야 한다

cancel signal은 execution 어느 지점에서든 관찰될 수 있어 lock, temporary file, network request가 이미 일부 생성됐을 수 있다. cleanup을 exception handler 하나에만 맡기면 non-cancellable region과 late completion에서 leak이 생길 수 있다.

commit phase와 cancellable phase를 나누고 resource release를 idempotent하게 만든다. cancellation 자체가 cleanup을 다시 interrupt하지 않게 보호할 필요도 있다.

cancel fuzzing으로 모든 suspension point를 흔든다. open handle과 allocated bytes가 baseline으로 돌아오는지 확인한다.

---

## CHAPTER 26 · callback ownership은 등록자, executor, target의 lifetime을 연결한다

callback registry가 closure를 strong하게 보관하면 target object가 예상보다 오래 살아 leak이 생길 수 있다. 반대로 raw target pointer만 저장하면 target destruction 뒤 callback이 use-after-free를 만든다.

unsubscribe token, weak capture, scoped registration 중 하나로 관계를 명시한다. executor queue에 이미 들어간 callback은 unsubscribe 뒤에도 실행될 수 있는지 contract가 필요하다.

register→enqueue→unregister→execute 순서를 바꾼 stress test를 만든다. callback generation을 로그에 남긴다.

---

## CHAPTER 27 · FFI ownership은 language runtime 밖으로 나가며 자동 규칙이 사라지는 지점이다

C/C++/JNI boundary에서는 누가 allocate하고 어느 allocator로 free하는지, pointer를 callee가 retain하는지 명시해야 한다. GC-managed object pointer를 native code가 장기간 보관하면 moving collector와 충돌할 수 있다.

allocator mismatch, encoding buffer lifetime, callback trampoline destruction이 대표 failure다. function signature만 맞다고 ownership semantics까지 맞는 것은 아니다.

FFI wrapper에서 raw pointer를 최소화하고 ownership annotation을 문서화한다. cross-language sanitizer와 stress test를 사용한다.

---

## CHAPTER 28 · region과 arena는 많은 object의 lifetime을 하나의 bulk boundary로 묶는다

개별 free 대신 arena 전체를 한 번에 release하면 allocation이 빠르고 fragmentation 관리가 단순해질 수 있다. 대신 하나의 작은 object가 오래 필요하면 arena 전체 memory가 retention된다.

object가 arena 밖으로 pointer를 escape하면 arena destruction 뒤 dangling reference가 된다. request-scoped temporary graph처럼 lifetime이 자연스럽게 묶인 workload에 적합하다.

arena별 allocated/live bytes와 age를 측정한다. giant arena를 convenience global allocator로 사용하지 않는다.

---

## CHAPTER 29 · lifetime failure는 leak, double free, use-after-free, stale identity로 분류한다

leak는 release가 없거나 owner cycle 때문에 lifetime이 너무 길어진 문제고, double free는 release 권한이 중복된 문제며, use-after-free는 borrow가 owner보다 오래 산 문제다. stale identity는 address/ID 재사용 때문에 old handle이 새 object를 잘못 가리키는 문제다.

증상이 memory corruption 하나로 보여도 failure class마다 수정 방법이 다르다. allocator hardening만 추가하면 root ownership bug가 남는다.

sanitizer, heap graph, generation log를 함께 사용한다. first invalid lifetime transition을 찾는다.

---

## CHAPTER 30 · ownership contract는 create, transfer, borrow, destroy 전 과정을 명시한다

resource API는 생성자가 누구인지, ownership transfer가 어느 call에서 일어나는지, borrowed reference가 얼마나 유효한지, release가 blocking인지, cancellation·error에서 누가 cleanup하는지를 정의해야 한다. raw pointer나 integer handle만으로는 이 정보가 보이지 않는다.

type wrapper와 scope를 사용해 invalid state를 줄이고, FFI/async처럼 type proof가 약해지는 경계에는 generation과 runtime check를 추가한다.

최종 검증은 normal path뿐 아니라 timeout, exception, owner destruction, concurrent callback을 주입해 **resource가 정확히 한 번 release되고 lifetime 밖 접근이 없음을 증명하는 것**이다.
