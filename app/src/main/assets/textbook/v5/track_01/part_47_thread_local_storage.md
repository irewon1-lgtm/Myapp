# PART 47 · Thread-Local Storage — TLS models, loader, thread pools, context leakage

Thread-local storage는 global variable의 이름에 `thread별` 속성을 붙이는 기능이 아니다. Loader와 runtime은 각 thread에 module별 TLS block을 만들고 code가 현재 thread의 storage를 빠르게 찾는 ABI를 제공해야 한다. Thread가 오래 살아 있는 pool, coroutine이 thread를 이동하는 runtime, dynamically loaded library, destructor 순서는 TLS의 lifetime을 예상보다 복잡하게 만든다. TLS는 synchronization을 줄이는 대신 memory duplication과 hidden context dependency를 만든다.

## CHAPTER 01 · TLS는 동일 symbol이 thread마다 다른 storage instance를 가리키게 한다

Ordinary global variable은 process 안에서 하나의 address/storage를 공유하지만 thread-local variable은 각 thread에 독립 instance가 존재한다. Source code가 같은 symbol을 읽어도 current thread identity에 따라 다른 address가 계산된다. 따라서 synchronization 없이 mutable scratch state를 저장할 수 있지만 thread 간 공유가 필요한 state에는 적합하지 않다. Thread-local pointer를 다른 thread에 넘기면 원래 thread instance의 lifetime과 의미가 깨질 수 있다. API가 TLS를 암묵적으로 사용하면 함수 argument만 보고 dependency를 알기 어렵다.

## CHAPTER 02 · compiler는 TLS access를 ordinary absolute address load와 다르게 생성한다

Global variable은 relocation 후 fixed/process-relative address로 접근할 수 있지만 TLS variable offset은 current thread의 TLS base와 결합해야 한다. Compiler와 linker는 target platform의 TLS ABI에 맞는 instruction sequence와 relocation을 생성한다. Executable에 정의된 local TLS인지 dynamically loaded module인지에 따라 더 짧거나 일반적인 access model을 선택할 수 있다. `thread_local read는 register access 하나` 같은 일반화는 linkage model과 architecture를 확인하지 않으면 틀린다.

## CHAPTER 03 · thread pointer는 current thread metadata와 TLS 영역의 anchor 역할을 할 수 있다

Architecture/runtime은 dedicated register 또는 OS-managed mechanism으로 현재 thread의 control block/TLS base를 빠르게 찾게 할 수 있다. TLS access는 이 base에 compile/link-time offset을 더해 variable address를 구하거나 loader-maintained vector를 따라갈 수 있다. Context switch 시 scheduler/runtime은 새 thread에 맞는 thread-pointer state를 복원해야 한다. TLS performance와 context switch correctness가 ABI와 연결되는 이유다.

## CHAPTER 04 · ELF PT_TLS류 metadata는 initial TLS image와 zero-fill 영역을 기술한다

Native executable/shared object가 TLS initialized data와 zero-initialized TLS를 포함하면 binary format은 loader가 각 thread instance를 만들 수 있도록 size, alignment, initial bytes를 제공해야 한다. Process 시작 시 main thread의 TLS block을 만들고 이후 thread 생성 시 같은 template을 복제/초기화한다. TLS section의 file size와 memory size가 다를 수 있어 `.tdata/.tbss`류 concept이 나온다. Binary-size report와 per-thread runtime memory를 구분한다.

## CHAPTER 05 · static TLS는 process 시작 시 알려진 module을 빠른 offset으로 배치할 수 있다

Executable과 startup shared library의 TLS 요구가 미리 알려져 있으면 loader가 하나의 static TLS area 안에 module별 block을 배치하고 fixed offset access를 가능하게 할 수 있다. Fast access의 대가는 reserved static TLS capacity와 dynamic module 제약이다. Runtime이 너무 많은 static TLS를 요구하는 library를 late-load하면 allocation model이 달라질 수 있다. Deployment에서 library load order가 TLS access model에 영향을 줄 수 있다.

## CHAPTER 06 · dynamic TLS는 dlopen 이후 추가된 module을 existing thread에 제공해야 한다

Process가 이미 여러 thread를 실행 중인 상태에서 TLS variable을 가진 shared library를 load하면 기존 thread에도 해당 module의 TLS instance가 필요해진다. Loader는 module ID와 dynamic thread vector 같은 metadata를 사용해 lazy/eager allocation을 관리할 수 있다. 모든 thread를 즉시 멈춰 giant block을 재배치하는 대신 indirection을 사용하는 이유다. Dlopen-heavy plugin architecture는 TLS lookup과 loader synchronization을 고려해야 한다.

## CHAPTER 07 · local-exec 같은 TLS model은 빠른 대신 binary deployment 조건이 강하다

Linker가 variable이 main executable 내부에 있고 module layout이 고정됨을 안다면 current thread base에서 constant offset으로 매우 짧게 접근할 수 있다. Shared library가 arbitrary process에 load될 수 있으면 이런 가정을 할 수 없어 general-dynamic류 더 유연한 sequence가 필요할 수 있다. Compiler option으로 강한 TLS model을 강제하기 전에 실제 linkage/loader invariant가 보장되는지 확인한다. 잘못된 model은 성능 문제가 아니라 relocation/load failure를 만든다.

## CHAPTER 08 · general dynamic access는 module identity와 offset resolution을 runtime에 맡긴다

Code가 어느 final TLS layout에 들어갈지 모르면 module ID/offset을 descriptor로 전달해 loader helper가 current thread의 address를 찾는 방식이 가능하다. First access에 allocation/lookup이 발생하고 이후 cache될 수 있다. Hot inner loop에서 general TLS lookup이 많으면 ordinary local보다 눈에 띄는 overhead가 될 수 있다. Profiling 시 source variable이 `thread_local`인지와 generated TLS sequence를 확인한다.

## CHAPTER 09 · thread 생성은 stack뿐 아니라 TLS image와 runtime control block을 초기화한다

New native thread를 만들 때 OS/runtime은 stack, guard page, scheduling state 외에 current executable/module의 TLS storage를 준비해야 한다. TLS initial value가 large array라면 thread 생성마다 memory footprint와 initialization copy/zero 비용이 늘어난다. `1000 threads`의 비용은 stack만 합산해서 끝나지 않는다. Per-thread runtime allocator cache, locale, errno, tracing state까지 포함하면 실제 overhead가 훨씬 커질 수 있다.

## CHAPTER 10 · per-thread large buffer는 N×memory amplification을 만든다

1MB scratch buffer를 TLS에 넣으면 thread 4개에서는 작아 보여도 500-thread service에서는 500MB logical reservation/commit pressure가 된다. 일부 thread가 해당 path를 한 번도 사용하지 않아도 eager TLS initialization이면 memory를 소비할 수 있다. Lazy thread-local allocation은 unused thread 비용을 줄이는 대신 first-use latency와 cleanup 복잡성을 만든다. Thread-count distribution과 TLS shallow/retained size를 함께 계산한다.

## CHAPTER 11 · errno는 TLS-like per-thread state가 필요한 이유를 보여 준다

두 thread가 동시에 system/library call을 수행할 때 하나의 global error variable을 공유하면 서로의 error code를 덮어쓴다. Per-thread errno representation을 사용하면 API가 legacy global-like syntax를 유지하면서 thread isolation을 얻을 수 있다. 그러나 async/coroutine code가 다른 thread로 resume하면 thread-local errno를 operation-local context처럼 장기간 보관하면 안 된다. Error value는 필요한 시점에 즉시 capture한다.

## CHAPTER 12 · TLS destructor는 thread 종료 시점에 resource cleanup을 수행할 수 있다

Thread-local object가 nontrivial destructor를 가지면 thread exit path가 각 TLS instance를 정리해야 한다. Destruction order와 다른 global/TLS object dependency가 shutdown bug를 만들 수 있다. Process 강제 종료에서는 destructor가 실행되지 않을 수 있으므로 critical persistent commit을 TLS destructor에만 의존하지 않는다. Thread pool worker가 process lifetime 내내 종료되지 않으면 destructor도 호출되지 않아 resource retention이 장기화될 수 있다.

## CHAPTER 13 · destructor가 새 TLS state를 다시 만들면 종료 protocol이 반복될 수 있다

일부 thread-specific data API는 destructor 실행 중 key value를 다시 설정하면 정해진 횟수만큼 destructor pass를 반복할 수 있다. Cleanup callback끼리 dependency가 있거나 resource를 다시 등록하면 thread exit가 예상보다 복잡해진다. Destructor를 ordinary request cleanup mechanism으로 사용하지 않고 thread-lifetime resource에 제한한다. Termination test에서 destructor order와 iteration limit을 확인한다.

## CHAPTER 14 · thread pool에서는 TLS가 request lifetime보다 훨씬 오래 산다

Worker thread가 수천 request를 재사용하면 한 request가 TLS에 넣은 user id, large buffer, security context가 다음 request까지 남을 수 있다. Request 끝에서 explicit reset을 하지 않으면 data leakage와 memory retention이 발생한다. `thread가 끝나면 자동 삭제`는 pool 환경에서 의미가 없다. Per-request context를 TLS에 넣을 때 scope guard로 install/restore하고 stale value test를 작성한다.

## CHAPTER 15 · coroutine은 logical task와 physical thread를 분리하므로 TLS를 request context로 쓰기 어렵다

Coroutine A가 thread 1에서 시작해 suspend 후 thread 2에서 resume하면 ordinary ThreadLocal lookup 결과가 달라진다. Runtime은 coroutine context element를 thread-local에 install/restore하는 bridge를 제공할 수 있지만 context switch마다 비용이 든다. Auth/trace/request state는 logical task context에 보관하고 thread-local은 actual thread resource에만 사용한다. P41 context propagation과 직접 연결되는 경계다.

## CHAPTER 16 · async callback이 foreign thread에서 들어오면 TLS assumption이 깨진다

Native library, Binder, executor가 callback을 어떤 thread에서 호출하는지 API contract를 확인해야 한다. Caller thread에 설정한 TLS가 callback thread에도 있을 것이라고 가정하면 locale/security/transaction context가 사라질 수 있다. Callback parameter 또는 explicit context token으로 필요한 state를 전달한다. FFI bridge가 managed thread attach를 수행할 때 runtime TLS 초기화도 필요할 수 있다.

## CHAPTER 17 · ThreadLocal key와 value graph는 managed runtime에서 GC root가 될 수 있다

Long-lived thread object가 ThreadLocal map을 보유하면 value가 다른 large object graph를 retain할 수 있다. Key를 weak하게 저장해도 stale entry cleanup이 늦으면 value가 남을 수 있는 runtime design이 존재한다. Heap dump에서 worker thread가 dominator가 되어 memory leak처럼 보일 수 있다. Library가 숨겨진 ThreadLocal cache를 사용하는지 retained path를 따라 확인한다.

## CHAPTER 18 · class loader와 TLS가 결합하면 hot-reload/module unload를 막을 수 있다

Application server/plugin 환경에서 system worker thread의 ThreadLocal value가 plugin class instance를 잡고 있으면 plugin class loader 전체가 collect/unload되지 않을 수 있다. Module을 redeploy할 때 old version metadata와 heap가 계속 남는다. Plugin shutdown 시 모든 worker에서 thread-local state를 제거하기 어려우므로 global shared worker에 plugin-owned TLS를 남기지 않는 설계가 중요하다.

## CHAPTER 19 · TLS cache는 lock contention을 줄이지만 memory centralization을 깨뜨린다

Allocator per-thread cache처럼 global free list를 매번 lock하지 않고 thread-local object를 재사용하면 hot-path contention을 낮출 수 있다. 대신 idle thread가 free object를 품고 있어 다른 thread가 memory shortage인데도 중앙 pool로 돌아오지 않을 수 있다. Thread exit 또는 high-watermark에서 cache를 drain하는 policy가 필요하다. Throughput 이득과 retained memory를 함께 측정한다.

## CHAPTER 20 · per-CPU와 per-thread storage는 migration semantics가 다르다

Thread-local state는 task가 CPU를 이동해도 thread를 따라가지만 per-CPU state는 scheduler migration 시 다른 instance를 보게 된다. Preemption/migration disabled section에서만 안정적으로 per-CPU data를 쓸 수 있는 kernel pattern이 존재한다. User-space CPU-id cache와 TLS를 섞을 때 migration race를 고려한다. P23 scheduler와 P44 restartable sequence의 차이를 연결한다.

## CHAPTER 21 · TLS는 false sharing을 줄일 수 있지만 allocator alignment가 locality를 결정한다

Thread별 counter를 하나의 shared array에 두면 서로 다른 counter라도 같은 cache line을 공유해 false sharing이 생길 수 있다. TLS로 각 thread의 private cache line에 배치하면 coherence traffic을 줄일 수 있다. 그러나 TLS block 내 여러 variable alignment와 adjacent metadata가 어떻게 배치되는지는 ABI/runtime에 달려 있다. Hot counter는 cache-line padding이 필요한지 PMU coherence evidence로 판단한다.

## CHAPTER 22 · NUMA에서 thread-local allocation은 first-touch placement와 결합된다

Thread가 자기 node에서 TLS-adjacent heap buffer를 처음 touch하면 local NUMA memory에 배치될 수 있지만 thread가 다른 node로 migration하면 remote access가 된다. Long-lived pinned worker와 per-thread state는 locality가 좋을 수 있고, freely migrating pool은 imbalance가 생길 수 있다. Thread affinity를 memory policy와 독립적으로 설정하지 않는다. TLS 자체보다 TLS가 소유한 large buffer placement가 더 큰 영향을 줄 수 있다.

## CHAPTER 23 · FFI는 TLS model과 runtime thread attach requirement를 맞춰야 한다

Native shared library가 TLS variable을 사용하면 dynamic loader가 해당 native thread에 TLS storage를 제공하지만 managed runtime metadata는 별도 attach가 필요할 수 있다. Foreign-created thread가 JVM/ART/Python runtime API를 호출하려면 runtime attach/GC safepoint contract를 따라야 한다. `OS thread가 존재함`과 `managed runtime이 이 thread를 알고 있음`은 같은 조건이 아니다.

## CHAPTER 24 · signal handler에서 TLS access는 async-signal-safety를 따로 검토해야 한다

TLS address 계산 자체가 platform에 따라 loader helper나 lazy allocation을 호출할 수 있는 model이라면 signal handler에서 안전하지 않을 수 있다. 이미 resolved된 simple TLS라도 variable access 후 호출하는 library가 signal-safe인지 별도 문제다. Signal path에서는 최소한의 preallocated state와 async-signal-safe operation을 사용한다. `thread-local이라 race 없음`이 signal safety를 의미하지 않는다.

## CHAPTER 25 · tracing context를 TLS에만 넣으면 async boundary에서 causal chain이 끊긴다

Synchronous thread-per-request server에서는 trace id를 ThreadLocal에 두는 방식이 동작할 수 있지만 executor/coroutine에서 thread가 바뀌면 다른 request context와 섞일 수 있다. Observability library는 logical context propagation API를 사용하고 thread-local은 implementation cache로 제한한다. Trace leak은 단순 로그 오류가 아니라 다른 user/request identity를 연결하는 privacy 문제도 만들 수 있다.

## CHAPTER 26 · security credential을 TLS에 넣을 때 install/restore가 예외·취소에도 보장돼야 한다

Impersonation context를 current thread에 설정한 뒤 operation이 exception으로 빠지면서 restore를 놓치면 다음 work가 높은 권한으로 실행될 수 있다. Scope guard/finally를 사용해 previous value 복원을 deterministic하게 만든다. Thread pool에서는 credential TLS를 `set(new)`만 하지 않고 previous state snapshot과 restoration을 한 operation으로 관리한다. Cancellation path도 동일 invariant를 통과해야 한다.

## CHAPTER 27 · test runner의 thread reuse는 TLS test isolation을 깨뜨릴 수 있다

테스트 A가 thread-local 값을 남긴 뒤 같은 worker에서 테스트 B가 실행되면 순서 의존 failure가 생긴다. Test framework가 병렬/worker pool을 사용하는 경우 각 test setup/teardown에서 context를 clear하고 leak assertion을 둘 수 있다. Random test order에서만 실패한다면 global static뿐 아니라 TLS residue를 조사한다. Production request isolation bug의 작은 재현이 될 수 있다.

## CHAPTER 28 · thread dump는 TLS memory를 직접 보여 주지 않아 heap/profile correlation이 필요하다

Thread count가 급증하면서 RSS가 늘어도 thread stack만으로 증가량을 설명하지 못하면 per-thread allocator cache, TLS object, native runtime state를 조사한다. Managed heap dump에서 worker thread dominator, native allocator per-thread arena, `/proc` thread count를 같은 timestamp로 비교한다. `thread 하나 비용`을 stack size 상수로 계산하지 않는다.

## CHAPTER 29 · TLS microbenchmark는 access model과 compiler optimization을 확인해야 한다

Compiler가 TLS address를 loop 밖에서 hoist하거나 constant local-exec offset을 사용하면 source-level 매 access 비용이 실제보다 작게 보일 수 있다. Dynamic library/general-dynamic model을 production이 사용하는데 executable local-exec benchmark로 측정하면 의미가 없다. Disassembly와 relocation type을 확인하고 warm/cold first-access를 분리한다. Dlopen 이후 lazy allocation cost도 별도 측정한다.

## CHAPTER 30 · TLS 계약은 thread lifetime·loader lifetime·logical task lifetime을 혼동하지 않는다

Thread-local에 넣을 state는 실제 OS/runtime thread와 같은 lifetime을 가져야 한다. Request/coroutine identity는 logical context로 전달하고, plugin/module state는 unload 가능성을 보존하며, large buffer/cache는 N×thread memory budget을 계산한다. Dlopen, thread creation, pool reuse, destructor, async migration을 regression test에 포함한다. TLS는 lock을 제거하는 shortcut이 아니라 storage identity를 thread에 결합하는 ABI/runtime mechanism이다.