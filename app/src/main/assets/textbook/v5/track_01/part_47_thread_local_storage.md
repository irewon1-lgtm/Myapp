# PART 47 · Thread-Local Storage — TLS models, loader, thread pools, context leakage

Thread-Local Storage(TLS)는 `global처럼 접근하지만 thread마다 다른 instance`를 제공한다. 편리해 보이지만 compiler code generation, ELF relocation, loader의 TLS block 배치, thread 생성·종료, runtime attach, GC root, thread pool reuse까지 연결된다. 특히 논리적인 request·coroutine lifetime과 OS thread lifetime을 혼동하면 context leakage와 memory retention이 생긴다. 이 PART는 ABI 수준의 TLS 접근 비용부터 운영 중 진단과 보안 경계까지 추적한다.

---

## CHAPTER 01 · thread-local 변수는 이름 하나에 thread 수만큼의 storage instance를 만든다

일반 global은 process 안에서 하나의 주소를 공유하지만 thread-local 변수는 각 thread가 자기 instance를 가진다. Source code에서 같은 식별자를 읽어도 실제 주소는 현재 thread의 TLS base와 변수 offset에 의해 달라진다. 따라서 TLS는 synchronization을 없애는 마법이 아니라 **공유 상태를 per-thread 상태로 분할하는 storage 정책**이다.

이 특성은 counter나 scratch buffer처럼 thread에 고정된 임시 상태에 유용하다. 반면 request identity처럼 thread를 이동할 수 있는 논리적 context를 저장하면 의미가 깨진다. Thread pool에서는 다음 작업이 같은 thread를 재사용하므로 이전 값이 남아 있으면 다른 요청으로 누출될 수 있다.

설계 시 먼저 값의 lifetime을 적는다. 정말 OS thread 생존 기간과 같다면 TLS가 자연스럽지만 request·transaction·coroutine lifetime이라면 명시적인 context 전달이 더 안전하다.

## CHAPTER 02 · TLS access는 compiler가 ABI별 주소 계산 sequence로 낮춘다

Source의 `thread_local x` 접근은 보통 단순 absolute address load가 아니다. Compiler는 실행 파일인지 shared object인지, symbol이 link 시점에 확정되는지에 따라 TLS model을 선택하고 thread pointer에서 offset을 계산하거나 runtime resolver를 호출하는 instruction sequence를 만든다.

이 때문에 같은 변수 접근도 local-exec와 general-dynamic에서 cost가 다를 수 있다. Hot loop의 TLS load가 예상보다 비싸다면 source line만 보지 말고 생성된 relocation과 instruction sequence를 확인해야 한다. LTO와 visibility 설정이 model 선택에 영향을 줄 수도 있다.

ABI는 register 사용, relocation type, alignment를 정의한다. 직접 assembly를 작성하거나 JIT가 TLS를 접근한다면 language runtime의 추측이 아니라 해당 target ABI 규칙을 따라야 한다.

## CHAPTER 03 · thread pointer는 현재 thread의 TLS 영역을 찾는 anchor다

많은 ABI는 각 thread마다 특별한 thread pointer 값을 유지하고, TLS 변수 주소를 이 pointer를 기준으로 계산한다. Context switch가 일어나면 kernel/runtime은 새 thread에 맞는 base를 복원한다. 그래서 같은 machine instruction이라도 실행 thread가 달라지면 다른 storage를 참조한다.

Thread pointer 주변에는 TLS 외에도 thread control block 같은 runtime metadata가 배치될 수 있다. 임의 offset을 가정해 접근하면 libc나 ABI 버전 변화에 취약하다. 공식 intrinsic과 compiler-generated access를 사용하는 이유다.

Debugger에서 TLS 주소를 비교할 때는 변수 symbol만 보지 말고 thread identity와 base도 함께 기록한다. “주소가 매번 달라진다”는 현상이 allocator bug가 아니라 TLS semantics일 수 있다.

## CHAPTER 04 · PT_TLS 같은 image metadata가 초기 TLS template를 정의한다

Executable과 shared object는 TLS에 들어갈 초기화 데이터와 zero-filled 영역의 크기·정렬 정보를 binary metadata로 제공한다. Loader는 이를 읽어 각 thread의 TLS block에 module별 template를 배치한다. 초기 값이 있는 thread-local 변수도 thread마다 같은 초기 pattern에서 시작한다.

이 metadata는 file offset과 memory size가 다를 수 있으며 alignment 조건을 만족해야 한다. Loader 구현은 overflow와 malformed size를 검증해야 한다. Binary parser라는 점에서 일반 segment mapping과 같은 보안 원칙이 적용된다.

TLS 문제를 진단할 때 source 선언만 보지 말고 binary에 실제 TLS segment가 생겼는지, module load 순서가 어떠한지 확인한다. Optimization으로 변수가 사라지거나 storage class가 바뀐 경우도 구분할 수 있다.

## CHAPTER 05 · static TLS는 thread 생성 시 미리 배치되어 빠른 offset 접근을 가능하게 한다

프로그램 시작 시 이미 알려진 module들의 TLS를 하나의 static block에 배치하면 각 변수의 offset을 빠르게 계산할 수 있다. 실행 파일과 초기 shared library의 TLS가 대표적이다. 이 경우 thread 생성 시 필요한 전체 크기를 알고 한 번에 storage를 준비할 수 있다.

장점은 접근 경로가 짧고 allocation이 예측 가능하다는 것이다. 단점은 나중에 동적으로 load되는 module의 TLS까지 무한히 예약할 수 없다는 점이다. Platform은 일부 surplus 영역이나 dynamic TLS 구조를 조합해 이 문제를 해결한다.

많은 thread를 만드는 서비스에서는 static TLS 크기도 memory amplification에 포함한다. 변수 하나가 작아 보여도 `per-thread bytes × thread count`로 계산해야 한다.

## CHAPTER 06 · dynamic TLS는 실행 중 추가된 module의 storage를 thread별로 확장한다

`dlopen`처럼 process 시작 후 shared object가 들어오면 이미 존재하는 모든 thread에 새 module의 TLS 공간이 필요할 수 있다. Loader는 module ID와 per-thread vector 같은 indirection을 사용해 dynamic TLS를 찾고 필요할 때 storage를 준비한다.

이 경로는 static offset 접근보다 resolver와 pointer chasing이 늘 수 있다. Plugin architecture에서 TLS-heavy module을 자주 load/unload하면 접근 비용과 lifetime 복잡도가 커진다. `dlclose`가 곧 모든 thread의 관련 storage가 즉시 안전하게 사라진다는 단순 가정도 위험하다.

테스트에서는 module을 load한 뒤 기존 thread와 새 thread 모두 변수 초기값이 올바른지 확인한다. Thread 종료 destructor와 module unload가 겹치는 race도 failure case에 포함한다.

## CHAPTER 07 · local-exec TLS model은 link 시 offset을 확정할 수 있을 때 가장 직접적이다

Local-exec model은 보통 main executable 내부처럼 symbol 위치가 최종 link에서 고정된 경우 사용한다. Compiler는 thread pointer에 상수 offset을 더하는 형태로 주소를 계산할 수 있어 runtime lookup이 거의 없다. 그래서 hot TLS access에는 유리하다.

하지만 shared library처럼 load address와 module 배치가 동적인 상황에 이 model을 강제로 사용하면 relocation 가능성을 깨뜨린다. Build flag 하나로 성능을 얻는 대신 배포 형태와 ABI 제약이 생기는 셈이다.

성능 실험에서는 source-level 변수 종류만 비교하지 말고 실제 relocation/model을 확인한다. Static linking, PIE, shared library 여부가 달라지면 결과를 그대로 옮길 수 없다.

## CHAPTER 08 · general-dynamic model은 범용성을 위해 resolver 비용을 지불한다

General-dynamic 방식은 symbol이 어느 module의 몇 번째 TLS object인지 runtime에 해결할 수 있어 shared object에 가장 일반적으로 적용할 수 있다. 대신 address 계산에 descriptor나 resolver 호출이 필요할 수 있어 local-exec보다 instruction과 memory access가 많다.

Compiler와 linker는 visibility나 whole-program 정보를 이용해 더 싼 model로 relaxation할 수 있다. 따라서 assembly 한 번만 보고 언어 차이를 단정하지 말고 최종 linked binary를 본다. Link-time 최적화가 TLS path를 바꾸는 경우도 있다.

TLS access가 실제 병목인지 확인하려면 microbenchmark만으로 부족하다. Cache miss, function call, surrounding work를 포함한 production profile에서 비중을 측정한 뒤 model 변경의 가치를 판단한다.

## CHAPTER 09 · 새 thread 생성은 stack뿐 아니라 TLS와 runtime state를 함께 만든다

Thread creation은 kernel task만 추가하는 일이 아니다. Userspace runtime은 stack, thread control block, static/dynamic TLS bookkeeping, cancellation state 등을 준비한다. Thread-local constructor가 있다면 초기화 시점도 이 lifecycle에 포함된다.

따라서 thread를 대량 생성·폐기하면 TLS 초기화 비용과 memory churn도 발생한다. Thread pool이 성능에 유리한 이유 중 하나는 이 per-thread 초기화 비용을 amortize하기 때문이다. 하지만 pool reuse는 TLS cleanup 책임을 application으로 되돌린다.

Thread creation failure를 테스트할 때 단순 `pthread_create` 오류만 보지 말고 limit, memory pressure, TLS allocation failure가 상위 runtime에 어떻게 전파되는지 확인한다.

## CHAPTER 10 · 작은 TLS 변수도 thread 수가 많으면 큰 memory amplification이 된다

64 KiB짜리 per-thread buffer는 하나만 보면 작아 보이지만 2,000 thread면 128 MiB 이상이 된다. Alignment와 allocator overhead까지 더하면 실제 RSS는 더 커질 수 있다. 특히 library가 내부적으로 TLS cache를 추가하면 application 개발자는 증가 원인을 놓치기 쉽다.

Memory budget은 `static TLS + dynamic TLS + thread cache + stack`을 thread 수와 곱해 계산한다. Thread count가 workload에 따라 늘어나는 서버라면 최악값과 steady state를 분리한다. Lazy allocation 여부도 RSS 해석에 중요하다.

OOM 사건에서는 heap histogram만 보면 TLS와 stack을 놓칠 수 있다. Thread count time series와 per-thread mapping을 함께 조사해야 “heap leak이 아닌 thread proliferation”을 구분할 수 있다.

## CHAPTER 11 · errno 같은 thread-local 상태는 동시 syscall 오류를 서로 격리한다

여러 thread가 동시에 system call을 실행하면 하나의 global error slot으로는 각 thread의 최근 오류를 표현할 수 없다. 그래서 `errno` 같은 상태는 thread-local semantics를 사용한다. Source에서는 변수처럼 보여도 실제로는 accessor macro/function을 통해 TLS를 참조할 수 있다.

중요한 점은 값의 유효 범위다. 성공한 함수가 errno를 반드시 0으로 만들지는 않으며, 다른 library call이 값을 덮을 수 있다. Error를 판단한 직후 필요한 값을 복사해야 한다. TLS라는 저장 방식이 API semantics까지 자동으로 안전하게 만드는 것은 아니다.

Callback이나 logging 함수가 errno를 바꾸는 환경에서는 원래 오류를 보존해야 한다. Diagnostic helper가 관찰 대상 상태를 변형하지 않는지 테스트한다.

## CHAPTER 12 · TLS destructor는 thread 종료 시 per-thread resource 정리를 시도한다

Thread-local object가 heap buffer나 handle을 소유하면 thread 종료 시 destructor를 실행해 정리할 수 있다. 하지만 process 강제 종료, fatal signal, runtime detach 같은 모든 경로에서 destructor가 보장되는 것은 아니다. 따라서 영속 correctness를 destructor 하나에 의존하면 안 된다.

Destructor 순서도 module 간 의존성을 만들 수 있다. 한 TLS object의 destructor가 이미 파괴된 다른 thread-local object를 참조하면 shutdown bug가 생긴다. 가능한 한 destructor는 자기 resource만 정리하고 복잡한 global dependency를 피한다.

Thread pool에서는 thread가 종료되지 않으므로 request마다 destructor가 호출되지 않는다. Request-scoped cleanup과 thread-exit cleanup을 구분해야 한다.

## CHAPTER 13 · destructor가 다시 TLS 값을 만들면 여러 pass가 필요할 수 있다

일부 thread-specific data API는 destructor 실행 중 key에 다시 non-null 값을 설정하는 패턴을 허용해 종료 시 destructor pass를 반복할 수 있다. 구현은 무한 반복을 막기 위해 pass 수를 제한한다. 이 semantics를 모르면 “destructor가 반드시 한 번만 실행된다”는 가정이 깨진다.

복잡한 cleanup chain은 종료 latency를 길게 만들고 순서 의존성을 숨긴다. Destructor 안에서 새로운 resource를 획득하는 코드는 특히 위험하다. 종료 경로에서는 allocation과 외부 호출을 최소화하는 편이 낫다.

테스트에서는 destructor가 재등록되는 case와 제한 도달 case를 포함한다. Resource가 누수될 수 있는 경로를 명확히 정의하고 process-level cleanup에 기대지 않는다.

## CHAPTER 14 · thread pool은 TLS lifetime을 request lifetime보다 훨씬 길게 만든다

Worker thread가 수천 request를 순차 처리하면 TLS 값은 worker가 살아 있는 동안 유지된다. 요청 A가 trace id, user context, parser buffer를 넣고 지우지 않으면 요청 B가 같은 thread에서 이전 상태를 볼 수 있다. 이 문제는 data race가 없어도 발생한다.

안전한 패턴은 작업 시작 시 context를 설정하고 `finally` 성격의 cleanup에서 원래 값을 복원하는 것이다. 단순히 새 값으로 덮는 것만으로는 exception·cancellation 경로에서 cleanup이 빠질 수 있다. Nested context라면 stack semantics가 필요할 수 있다.

Pool reuse test에서는 서로 다른 tenant/request를 같은 worker에 강제로 배치해 leakage를 검사한다. Thread 수를 늘려 우연히 문제가 숨는 테스트는 피한다.

## CHAPTER 15 · coroutine은 thread를 이동할 수 있으므로 TLS가 logical context를 따라가지 않는다

Coroutine이 suspension 후 다른 worker thread에서 resume되면 TLS는 새 thread의 값을 보게 된다. 따라서 request id, transaction, security principal 같은 logical context를 plain TLS에 저장하면 resume 순간 값이 바뀔 수 있다. 반대로 thread에 고정된 native resource라면 이동 자체가 문제가 될 수 있다.

Modern runtime은 coroutine-local 또는 explicit context propagation mechanism을 제공하기도 한다. 핵심은 scheduler가 suspension/resume 경계에서 어떤 값을 capture하고 restore하는지 이해하는 것이다. 모든 thread-local library가 이를 자동 지원하지 않는다.

Migration 테스트는 single-thread executor로는 드러나지 않는다. 여러 worker에서 강제로 suspend/resume시키고 context가 유지되는지 확인해야 한다.

## CHAPTER 16 · foreign callback은 예상하지 못한 thread에서 runtime 코드를 실행시킬 수 있다

Native library가 자기 thread를 만들고 managed/runtime callback을 호출하면 그 thread에는 언어 runtime이 기대하는 TLS state가 아직 없을 수 있다. JNI 같은 interface는 thread attach 절차를 요구할 수 있고, callback 종료 시 detach 책임도 생긴다.

반대로 runtime thread가 C library로 들어갔다가 callback을 받는 경우에는 기존 TLS가 존재한다. 두 경우를 같은 것으로 취급하면 thread identity와 exception handling이 꼬일 수 있다. Callback API 문서에서 호출 thread 보장을 확인한다.

테스트에서는 application이 만든 thread, foreign library thread, thread pool worker 각각에서 callback을 실행한다. Crash가 드물게 나타나면 TLS initialization과 runtime attach 상태를 먼저 확인한다.

## CHAPTER 17 · thread-local reference는 GC 관점에서 thread가 가진 장기 root가 될 수 있다

Managed runtime에서 ThreadLocal이 object를 참조하면 그 thread가 살아 있는 동안 reference chain이 GC root에서 이어질 수 있다. Worker thread가 process lifetime만큼 오래 산다면 request 하나의 큰 object graph가 cleanup 누락으로 계속 살아남을 수 있다.

이 누수는 “할당은 끝났는데 회수가 안 되는” 형태로 보인다. Heap dump에서는 object 자체보다 root path가 Thread/ThreadLocal 구조로 이어지는지 확인해야 한다. 단순 retained size 순위만 보면 원인을 놓칠 수 있다.

Scope 종료 시 remove/restore를 강제하고, library가 내부 ThreadLocal cache를 쓰는지도 검토한다. Memory test는 여러 request cycle 후 retained object 수가 baseline으로 돌아오는지 본다.

## CHAPTER 18 · class loader와 ThreadLocal이 결합하면 module unload를 막을 수 있다

Plugin이나 application container에서는 장수 thread가 ThreadLocal을 통해 plugin class instance를 참조하면 해당 class loader 전체가 reachable 상태로 남을 수 있다. 코드 자체는 unload됐다고 생각해도 metadata와 static graph가 계속 유지되어 redeploy마다 memory가 증가한다.

이 문제는 value뿐 아니라 custom ThreadLocal key의 class도 영향을 줄 수 있다. Weak reference가 일부 사용되더라도 stale entry cleanup 시점에 따라 retention이 길어질 수 있다. Runtime 구현의 실제 semantics를 확인해야 한다.

Redeploy stress test에서 class loader instance 수와 thread-local root를 추적한다. 단일 배포 후 heap이 정상인 것만으로는 장기 운영 누수를 찾기 어렵다.

## CHAPTER 19 · per-thread cache는 contention을 줄이지만 memory와 freshness를 희생한다

Allocator, codec, parser가 thread-local cache를 두면 global lock과 allocation을 줄여 throughput을 높일 수 있다. 대신 idle thread에도 cache가 남고, thread 수가 많으면 전체 cache footprint가 커진다. Cache에 들어간 object의 generation이나 configuration이 오래될 수도 있다.

Cache size는 per-thread hit rate와 총 memory의 함수로 정한다. 큰 cache가 hit rate를 거의 개선하지 않으면 전체 thread 수에 의해 비용만 증폭된다. Thread pool resize로 thread가 줄 때 cache가 실제로 회수되는지도 본다.

관측에는 thread count, cache bytes/thread, hit/miss를 함께 둔다. Global cache와 비교할 때 lock cost만 보지 말고 memory amplification과 locality까지 포함한다.

## CHAPTER 20 · per-CPU와 per-thread는 비슷해 보여도 scheduler migration에서 의미가 갈린다

Per-thread state는 task identity를 따라가지만 per-CPU state는 현재 실행 중인 CPU에 귀속된다. Scheduler가 thread를 CPU 3에서 CPU 7로 이동시키면 TLS 값은 그대로지만 per-CPU counter나 buffer는 다른 instance를 사용한다. 두 개념을 섞으면 통계와 lifetime 해석이 틀어진다.

Kernel의 per-CPU data는 preemption/migration 제어와 결합해 안전하게 접근하는 경우가 많다. Userspace에서 CPU id를 읽고 배열에 접근하는 단순 구현은 접근 도중 migration될 수 있어 별도 보장이 필요하다.

성능 최적화에서 무엇을 분할하려는지 먼저 정한다. Thread ownership을 줄이려는지, CPU cache locality를 얻으려는지에 따라 storage primitive가 달라진다.

## CHAPTER 21 · TLS 자체가 false sharing을 완전히 제거해 주는 것은 아니다

각 thread가 서로 다른 variable instance를 가지면 논리적 공유는 줄지만, allocator나 TLS layout 때문에 두 thread의 자주 쓰는 data가 같은 cache line에 위치할 가능성은 구현에 따라 달라질 수 있다. 또한 TLS가 가리키는 heap object가 공유되면 false sharing은 그대로 남는다.

Counter array를 TLS pointer로 나눴더라도 최종 집계 구조를 자주 갱신하면 coherence traffic이 발생한다. 진짜 write ownership이 어디에 있는지 cache-line 수준에서 봐야 한다.

Hardware counter와 address sampling으로 invalidation hotspot을 확인한다. `thread_local` 키워드가 있다는 이유만으로 coherence 문제가 해결됐다고 결론내리지 않는다.

## CHAPTER 22 · NUMA에서는 TLS가 가리키는 memory의 물리적 위치도 성능을 좌우한다

Thread-local object가 thread 생성 시 특정 NUMA node에서 first-touch되면 이후 thread가 다른 node로 이동했을 때 remote memory access가 발생할 수 있다. TLS descriptor 자체보다 그 안에서 참조하는 큰 scratch buffer나 cache가 더 큰 영향을 준다.

Long-lived worker를 CPU affinity로 고정하면 locality가 안정될 수 있지만 load balancing 유연성을 잃는다. 반대로 자유 migration을 허용하면 memory placement 정책이 필요하다. Workload의 migration 빈도와 buffer 크기를 같이 본다.

NUMA 문제를 진단할 때 per-thread latency와 CPU/node placement를 함께 기록한다. 평균 값만 보면 일부 migrated worker의 tail slowdown이 가려질 수 있다.

## CHAPTER 23 · FFI thread attach는 runtime TLS를 초기화하는 lifecycle operation이다

Managed runtime은 각 native thread에 GC state, exception state, safepoint metadata 같은 per-thread runtime 정보를 요구할 수 있다. 외부 thread가 FFI로 들어올 때 attach를 수행하면 이러한 구조와 TLS가 준비된다. Attach 없이 runtime API를 호출하면 crash나 GC corruption으로 이어질 수 있다.

Attach된 thread는 runtime이 scan해야 할 root와 scheduling participant가 되므로 detach 시점도 중요하다. Thread가 종료되는데 runtime registry에 남아 있으면 resource leak이 생긴다. 반대로 callback 중 너무 일찍 detach하면 live reference가 무효가 된다.

Boundary wrapper에서 attach 여부를 확인하고 ownership 규칙을 한 곳에 둔다. 각 callback 구현이 제각각 lifecycle을 처리하게 만들지 않는다.

## CHAPTER 24 · signal handler에서는 TLS 접근도 async-signal-safety를 따져야 한다

Signal은 thread의 임의 instruction 지점에서 handler를 실행할 수 있다. TLS address 계산 자체가 단순 offset이면 가능해 보여도 accessor가 loader resolver나 allocation을 호출하는 경로라면 signal-safe하지 않을 수 있다. Handler에서 사용할 수 있는 API 집합을 엄격히 제한해야 한다.

특히 lazy initialization되는 thread-local object를 handler에서 처음 접근하면 lock이나 heap allocation이 발생할 수 있다. 정상 경로에서 한 번 초기화됐다는 경험에 의존하지 않는다. Platform ABI와 runtime 구현을 확인한다.

Crash handler는 가능한 한 preallocated buffer와 최소 syscall만 사용한다. 복잡한 TLS logging context를 읽으려다 deadlock을 만드는 것보다 raw thread id와 signal info를 안전하게 남기는 편이 낫다.

## CHAPTER 25 · tracing context는 thread-local convenience보다 propagation correctness가 중요하다

Trace/span id를 TLS에 두면 동기 호출 stack에서는 편리하지만 async task가 다른 worker로 이동하면 context가 끊어진다. 반대로 pool worker를 재사용하면서 cleanup을 놓치면 다른 request의 span으로 잘못 연결될 수 있다. 잘못된 trace는 없는 trace보다 incident 분석을 더 혼란스럽게 만든다.

Tracing framework는 task 생성 시 context를 capture하고 실행 시 restore하는 wrapper를 제공할 수 있다. Queue, callback, future 같은 모든 async boundary가 propagation point다. 일부 boundary만 지원하면 trace graph가 부분적으로 잘못된다.

테스트에서는 parent-child span 관계를 검증하고 worker thread id가 바뀌는 상황을 포함한다. Context 값 자체뿐 아니라 cleanup 후 이전 값이 복원되는지도 확인한다.

## CHAPTER 26 · security principal을 TLS에 보관하면 cleanup 누락이 권한 누출로 이어질 수 있다

Authentication/authorization context가 thread-local이면 request 종료 후 반드시 제거되어야 한다. Thread pool의 다음 요청이 이전 principal을 상속하면 tenant isolation이 깨질 수 있다. 이 문제는 race가 아니라 deterministic lifetime bug다.

안전한 구조는 default-deny 상태에서 request 시작 시 명시적으로 principal을 설치하고 종료 시 이전 상태를 복원하는 것이다. Exception, timeout, cancellation에서도 같은 cleanup path가 실행되어야 한다. Nested impersonation이 있다면 push/pop 규칙을 둔다.

Security test는 익명 요청을 privileged 요청 직후 같은 worker에 배치해 stale principal을 확인한다. 단순 정상 로그인 flow만으로는 이 종류의 누출을 찾기 어렵다.

## CHAPTER 27 · TLS를 쓰는 test는 process 순서와 worker reuse에 의존하지 않게 격리해야 한다

한 test가 thread-local 값을 설정하고 지우지 않으면 다음 test가 같은 test runner thread에서 영향을 받을 수 있다. 개별 test만 실행하면 통과하지만 suite 전체에서는 순서에 따라 실패하는 전형적인 hidden state가 된다.

Fixture는 시작 전에 known baseline을 만들고 종료 후 cleanup을 보장해야 한다. Parallel test에서는 thread 수와 scheduler가 달라지므로 TLS leakage가 더 불규칙하게 나타날 수 있다. Random test order가 문제 탐지에 도움이 된다.

Leak regression은 값뿐 아니라 retained object도 본다. Test process가 장시간 살아 있는 환경에서는 stale ThreadLocal이 suite memory를 계속 키울 수 있다.

## CHAPTER 28 · TLS memory 문제는 heap snapshot과 thread inventory를 함께 봐야 한다

메모리가 증가할 때 heap object type만 보면 원인이 “많은 buffer”로 보일 수 있다. 각 buffer가 어떤 thread-local root에서 유지되는지, 그 thread가 왜 살아 있는지까지 연결해야 근본 원인을 찾는다. Thread dump와 heap dominator tree를 같은 시점에 수집하는 이유다.

Native TLS는 managed heap에 잡히지 않을 수 있다. `/proc` mapping, native allocator profile, thread count, library-specific stats를 함께 사용해야 한다. Static TLS와 thread stack을 heap leak으로 오해하지 않는다.

진단 절차는 thread 수 증가와 bytes/thread 증가를 분리한다. 전자는 thread lifecycle 문제이고 후자는 TLS/cache payload 문제일 가능성이 높다.

## CHAPTER 29 · TLS benchmark는 access model과 surrounding work를 분리해 해석한다

단순 loop에서 TLS load만 반복하면 compiler가 값을 register에 hoist하거나 dead-code elimination할 수 있다. 반대로 resolver를 의도하지 않게 포함하면 실제 hot path보다 과장된 결과가 나온다. Assembly를 확인하고 결과가 실제 access를 측정하는지 검증한다.

Local-exec, initial-exec, general-dynamic 같은 model을 비교하려면 build/link 조건을 고정해야 한다. Shared library 여부와 symbol visibility가 달라지면 다른 것을 측정하게 된다. CPU affinity와 warm cache 조건도 명시한다.

Microbenchmark에서 몇 ns 차이가 나더라도 application에서 TLS가 전체 CPU의 0.1%라면 가치가 작다. Production profile과 결합해 최적화 우선순위를 정한다.

## CHAPTER 30 · TLS 선택의 contract는 storage identity·lifetime·migration 세 축으로 정의한다

Thread-local state를 도입하기 전에 세 질문에 답한다. 값은 어떤 thread에 소속되는가, 언제 생성·정리되어야 하는가, 실행이 다른 thread로 이동할 수 있는가. 이 셋 중 하나라도 thread lifetime과 맞지 않으면 plain TLS는 잘못된 abstraction일 가능성이 높다.

ABI 수준에서는 access model, loader가 만든 storage, destructor 규칙을 확인한다. Runtime 수준에서는 pool reuse, coroutine migration, foreign thread attach를 다룬다. 운영 수준에서는 per-thread memory, stale context, root retention을 관측한다.

TLS는 lock을 줄이고 API를 단순하게 만들 수 있지만 숨은 전역 상태를 thread 단위로 복제한다. 따라서 사용 지점마다 ownership과 cleanup을 문서화하고, leakage와 migration을 의도적으로 시험하는 것이 최종 안전 장치다.
