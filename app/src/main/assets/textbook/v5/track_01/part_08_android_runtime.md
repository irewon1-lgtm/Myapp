# PART 08 · Android runtime — package, ART, Zygote, Binder, lifecycle

Android application의 실행은 APK 내부 code를 CPU가 직접 읽는 과정이 아니다. package 설치 상태, DEX, ART compilation mode, Zygote process creation, framework component lifecycle, Binder IPC, process importance가 결합된 platform execution model이다.

---

## CHAPTER 01 · APK는 설치 가능한 package format이고 runtime image와 동일하지 않다

APK에는 DEX code, compiled resource, manifest, asset, native library, signing metadata가 들어갈 수 있다. package manager는 설치 시 package identity·signature·component declaration·permission 정보를 system state에 반영한다.

설치된 package가 하나여도 실행 중 process는 여러 개일 수 있고 process가 종료되어도 package와 durable app data는 남는다. `앱이 설치됨`, `process가 존재함`, `Activity가 화면에 있음`은 독립 상태다.

설치 문제를 실행 문제와 구분하려면 package version, signing identity, split 구성, install source와 실제 process build를 연결한다. 같은 application ID라도 update 전 process가 잠시 살아 있거나 multi-process component가 서로 다른 lifecycle에 있을 수 있으므로 PID만으로 배포 version을 추정하지 않는다. 설치 후 code/resource artifact가 어느 path에 배치됐는지와 data directory가 어떤 version에서 만들어졌는지를 기록하면 “APK는 새 버전인데 상태는 옛 버전” 같은 migration 문제를 분리할 수 있다. package replace, uninstall/reinstall, data-clear는 durable state 보존 범위가 서로 다르다.

---

## CHAPTER 02 · DEX는 managed runtime이 소비하는 bytecode representation이다

Kotlin/Java source는 build pipeline을 거쳐 DEX representation으로 변환된다. DEX는 source syntax가 아니라 register-based bytecode와 type/method metadata를 가진 runtime input이다.

minification/optimization 과정에서 class·method 이름이 바뀔 수 있으므로 production stack trace를 source로 복원하려면 mapping artifact를 보존해야 한다. build artifact identity와 symbol/mapping identity를 함께 관리한다.

DEX 단계에서는 desugaring, shrinking, inlining, dead-code elimination 때문에 source와 runtime method 구조가 크게 달라질 수 있다. 따라서 production crash line을 source filename 하나로 역추적하지 않고 APK checksum/version과 정확히 대응하는 mapping·native symbols를 사용한다. multidex나 split module이 있으면 class가 어느 dex/split에서 왔는지도 확인한다. verifier나 class-resolution failure가 발생했을 때는 source compiler 성공 여부보다 최종 packaged bytecode의 type reference, method signature, minSdk 변환 결과를 봐야 한다. release artifact를 보존해야 이런 문제를 재현할 수 있다.

---

## CHAPTER 03 · ART는 interpretation, JIT, AOT를 상황에 맞게 조합한다

ART는 DEX를 실행하며 platform version과 compilation policy에 따라 interpreter, JIT, ahead-of-time compilation을 조합할 수 있다. 따라서 `Android는 전부 JIT` 또는 `전부 AOT`라는 설명은 정확하지 않다.

startup, storage footprint, installation/update cost, steady-state performance는 서로 다른 목표다. compilation policy가 바뀌면 동일 APK도 실행 특성이 달라질 수 있으므로 runtime/platform version을 performance evidence에 포함한다.

runtime 실행 상태를 분석할 때는 method가 interpreter/JIT/AOT 중 어느 경로에 있었는지와 profile 상태를 가능한 범위에서 함께 본다. code가 다시 compile되거나 deopt되면 동일 method의 CPU profile이 시간에 따라 달라질 수 있다. device idle/charging 같은 maintenance 조건과 package update 이후 artifact invalidation도 성능 변동의 원인이 될 수 있다. benchmark에는 OS build, ART module/runtime version, app version, profile warmness를 기록해 code change와 runtime compilation state를 분리한다. 한 번의 빠른 실행을 steady-state 성능으로 일반화하지 않는다.

---

## CHAPTER 04 · JIT는 runtime profile을 이용해 hot code에 compilation budget을 집중한다

JIT compiler는 실행 중 관찰한 code path를 기반으로 native code를 생성할 수 있다. hotness 판단, code cache, profile collection 자체가 runtime resource를 사용한다.

benchmark 초반과 장시간 실행 결과가 다른 이유 중 하나가 warmup과 compilation state다. microbenchmark에서 JIT warmup을 통제하지 않으면 source 변경보다 runtime compilation timing을 측정할 수 있다.

warmup을 정량화하려면 iteration별 latency와 compilation event를 같이 관찰하고, 분포가 안정화되는 지점을 미리 정한 규칙으로 판단한다. 첫 실행을 버리는 것만으로 충분하지 않을 수 있다. code cache pressure나 새로운 call target 출현으로 이미 compiled된 code가 다시 최적화·deopt될 수 있기 때문이다. profile-guided branch가 실제 사용자 workload와 다르면 benchmark에서만 빠른 code가 생성될 수도 있다. 성능 비교는 동일한 profile preparation 절차와 같은 device thermal state에서 수행하고 cold, warming, steady 구간을 별도 결과로 남긴다.

---

## CHAPTER 05 · AOT compilation은 설치된 code 전체를 무조건 native로 만드는 과정이 아니다

ART의 dex optimization/AOT 정책은 platform release와 device condition에 따라 달라질 수 있다. Android 14 이후에는 ART Service가 dexopt 관련 역할을 담당한다. profile-guided compilation은 자주 실행되는 영역에 최적화 비용을 집중할 수 있다.

성능 문제에서 `AOT가 안 됐다`고 추정하기 전에 실제 compilation state와 startup trace를 확인한다. package update, OS update, profile 상태가 compilation artifact를 바꿀 수 있다.

AOT artifact는 app binary와 독립적인 영구 진실이 아니라 package·runtime·profile 조건에 종속된 cache로 본다. update에서 dex checksum이나 runtime contract가 바뀌면 기존 artifact를 재사용할 수 없고, profile이 아직 충분히 수집되지 않은 새 설치는 오래 사용한 device와 다른 startup 특성을 가질 수 있다. 성능 회귀를 조사할 때 fresh install, update install, 오래 사용한 install을 분리하고 compilation 준비 상태를 기록한다. compile 비용을 앞당겨 startup을 줄이면 installation/maintenance CPU·storage 비용이 늘 수 있으므로 전체 lifecycle cost도 함께 비교한다.

---

## CHAPTER 06 · Zygote는 공통 runtime state를 준비한 뒤 app process 생성 비용을 줄인다

Zygote 계열 process는 framework/runtime class와 공통 state를 미리 준비하고 application process 생성에 사용된다. fork 계열 생성은 copy-on-write를 활용해 초기 memory sharing 이점을 얻을 수 있다.

process 생성 이후 app-specific initialization과 class loading, Application/component startup이 이어진다. startup memory가 공유 상태에서 private page로 변하는 write pattern은 COW와 연결된다.

preloaded page가 공유된다는 사실과 모든 child가 같은 physical cost를 계속 공유한다는 사실은 다르다. child가 해당 page를 수정하면 private copy가 생기고, 여러 process가 startup 직후 동일 영역을 많이 dirty하면 기대한 sharing 이득이 빠르게 줄 수 있다. memory 분석에서는 process별 RSS 합보다 PSS/shared-private 구성을 보고, startup trace에서는 fork 완료 이후 app-specific initialization이 어느 page와 class를 touch하는지 본다. Zygote 단계에서 준비할 수 없는 native resource나 process-specific handle은 child에서 다시 초기화돼야 하므로 preload와 per-process init 경계를 명시한다.

---

## CHAPTER 07 · cold startup latency는 process creation부터 first frame까지 이어진다

cold start에서는 process가 없기 때문에 process creation, runtime/class initialization, Application, component creation, UI composition/layout/render가 모두 경로에 들어갈 수 있다.

한 phase를 줄여도 critical path 밖이면 first-frame latency가 줄지 않는다. startup optimization은 trace에서 critical path를 찾고 main-thread blocking I/O, eager initialization, class loading, heavy DI graph 같은 실제 span을 줄인다.

first frame 이전 work를 전부 “startup”이라고 합치지 말고 dependency graph를 만든다. 예를 들어 analytics 초기화가 background에서 진행되며 UI render와 dependency가 없다면 그 시간을 줄여도 first-frame metric은 변하지 않는다. 반대로 content provider 초기화나 synchronous Binder call은 Application 이전에도 critical path에 들어올 수 있다. trace에서 process start, bind application, component creation, measure/layout/draw, frame present를 연결하고 각 span의 on-CPU와 blocked time을 분리하면 단순 method profile보다 실제 wall-clock blocker를 찾을 수 있다.

---

## CHAPTER 08 · warm/hot start 구분은 존재하는 state의 차이다

process와 Activity/task state가 얼마나 남아 있는지에 따라 startup path가 달라진다. 이미 process가 살아 있는 warm path와 process까지 새로 필요한 cold path의 비용을 평균 하나로 섞으면 regression을 놓친다.

성능 지표는 startup type과 device state를 분리해 수집한다. cache가 따뜻한 반복 실행만 측정하면 실제 사용자의 cold-start 문제를 숨길 수 있다.

실험 harness가 앱을 단순 background/foreground만 반복하면 hot path 비중이 과도하게 높아질 수 있다. cold 측정에서는 process와 관련 cache 준비 상태를 명시하고, warm 측정에서는 process는 있지만 target Activity state가 없는 상황처럼 조건을 고정한다. task/back-stack 상태도 launch path를 바꾸므로 intent flag와 launch mode를 함께 기록한다. 결과를 p50 하나로 합치지 말고 startup class별 분포와 발생 비율을 보고, release 전후에 어떤 class에서 regression이 생겼는지 비교한다. 그래야 “전체 평균은 그대로지만 cold start가 악화”된 변화를 놓치지 않는다.

---

## CHAPTER 09 · component lifecycle은 process lifetime과 같은 것이 아니다

Activity, Service, Provider 등의 lifecycle callback은 framework가 component state를 관리하기 위한 계약이다. process 자체는 background importance와 system pressure에 따라 component가 눈에 보이지 않는 동안 종료될 수 있다.

메모리에 있는 singleton이나 static object를 영구 source of truth로 사용하면 process recreation 후 상태가 사라진다. UI state, durable state, server-derived cache를 서로 다른 lifetime으로 설계한다.

resource ownership도 lifecycle callback 이름에 기계적으로 맞추지 않는다. 화면 visibility보다 오래 살아야 하는 subscription과 화면이 사라지면 즉시 끊어야 하는 camera/location handle은 owner가 다르다. configuration change는 Activity instance를 바꿔도 process와 다른 scoped state를 유지할 수 있어 leak을 숨기기도 한다. 각 resource에 owner scope, acquire point, release point, process-death 복구 방식을 적고 lifecycle test에서 create/start/resume/pause/stop/destroy 순서뿐 아니라 recreate와 background kill을 조합해 double registration과 stale callback을 검사한다.

---

## CHAPTER 10 · process death는 exception이 아니라 platform lifecycle의 정상 가능성이다

background process는 memory pressure나 platform policy에 따라 종료될 수 있다. application이 다음 실행에서 이전 heap을 그대로 복원받는 것은 아니다.

복원 설계는 `현재 화면을 재구성하는 최소 state`, `다시 fetch 가능한 state`, `반드시 durable해야 하는 user transaction`을 구분한다. process kill test 없이 rotation/recomposition만 테스트하면 recovery contract가 검증되지 않는다.

process가 강제 종료될 때 application callback을 받을 것이라고 가정하지 않는다. 따라서 “종료 직전에 저장”하는 설계보다 state가 의미 있게 바뀌는 시점에 durable boundary를 갱신해야 한다. 서버로 전송 중인 transaction은 local pending record와 idempotency key를 남겨 재시작 뒤 완료/재시도 여부를 판별한다. restore test는 임의의 screen state에서 process를 제거한 뒤 fresh process가 saved state와 database만으로 동일 logical state를 구성하는지 검사한다. memory-only token이나 singleton cache가 없으면 실패하는 경로를 의도적으로 찾아야 한다.

---

## CHAPTER 11 · main Looper/MessageQueue는 UI thread의 scheduling contract다

main thread는 Looper와 MessageQueue를 통해 callback/message를 순차 처리한다. 한 callback이 오래 실행되면 뒤의 input, lifecycle callback, render-related work가 지연된다.

ANR/jank 원인은 CPU 전체가 느린 것이 아니라 main thread가 deadline 동안 blocked 또는 busy인 경우가 많다. thread trace에서 runnable/on-CPU, monitor wait, Binder wait, file/network I/O를 분리한다.

queue 지연은 callback 실행시간과 별개로 측정한다. message가 enqueue된 뒤 dispatch 시작까지 오래 기다리면 앞선 작업이나 synchronization barrier가 원인일 수 있고, dispatch 자체가 길면 handler 내부 work가 원인이다. main-thread trace에 message/Choreographer frame span과 Binder/disk span을 겹치면 input이 왜 deadline을 놓쳤는지 causal chain을 만들 수 있다. background thread가 main thread lock을 오래 보유하는 경우 CPU profile만 보면 main은 idle처럼 보일 수 있으므로 owner thread까지 따라간다. queue backlog와 frame miss를 함께 계측한다.

---

## CHAPTER 12 · coroutine은 thread를 없애지 않고 suspension과 continuation을 추상화한다

Kotlin coroutine은 suspend point에서 execution을 중단하고 continuation을 나중에 resume할 수 있게 한다. 실제 work는 dispatcher가 선택한 thread/executor에서 실행된다.

`suspend` 함수라고 CPU-bound work가 자동으로 background thread로 이동하지 않는다. dispatcher 선택, structured cancellation, lifecycle scope를 확인한다. blocking call을 limited thread pool에 몰아넣으면 coroutine 수가 많아도 underlying pool saturation이 발생한다.

coroutine dump나 trace를 해석할 때 logical coroutine과 physical thread를 분리한다. 하나의 coroutine이 여러 thread에서 이어질 수 있고 한 thread가 시간에 따라 여러 coroutine을 실행할 수 있다. dispatcher queue wait가 길어지면 suspend/resume 자체는 정상이어도 user latency가 증가한다. parent cancellation이 child로 전파되는 범위와 `NonCancellable` cleanup 같은 예외 구간을 점검하고, blocking bridge를 사용할 때 pool size와 downstream connection pool의 상한이 어떻게 결합되는지 본다. request ID를 coroutine context에 전달하면 resume 뒤에도 trace correlation을 유지할 수 있다.

---

## CHAPTER 13 · Binder transaction은 process boundary와 scheduler boundary를 동시에 넘는다

Binder proxy call은 local method syntax처럼 보일 수 있지만 remote service process의 Binder thread에서 실행될 수 있다. serialization, kernel transaction, remote scheduling, reply가 latency에 포함된다.

large transaction, nested synchronous Binder call, Binder thread pool exhaustion은 deadlock/latency chain을 만들 수 있다. trace에서 caller→Binder→callee 관계를 이어서 본다.

동기 transaction이 연쇄적으로 A→B→C를 호출하면 A의 main thread가 C의 queue와 execution까지 간접적으로 기다릴 수 있다. 각 service의 Binder thread pool이 모두 다른 downstream synchronous call을 기다리면 CPU가 남아 있어도 progress가 멈출 수 있다. payload가 커지면 serialization/copy 비용뿐 아니라 transaction buffer limit 실패도 고려한다. Binder trace에서는 transaction ID, caller thread, target process/thread, queue wait, callee execution, reply를 연결하고, one-way call은 reply가 없으므로 receiver backlog와 drop/failure semantics를 별도로 관찰한다.

---

## CHAPTER 14 · Binder authorization은 caller identity를 기준으로 수행해야 한다

privileged service가 Binder request를 받을 때 caller UID/permission을 검증하지 않으면 service 권한이 confused deputy로 악용될 수 있다. input validation과 authorization은 별도다.

identity를 임시로 clear/restore하는 API를 사용할 경우 scope를 정확히 제한한다. privileged identity로 수행해야 하는 최소 operation만 분리하고 예외 path에서도 identity restoration을 보장한다.

caller identity를 local helper로 넘기는 동안 단순 PID/UID integer만 오래 저장하면 process 재사용이나 asynchronous 처리에서 원래 principal과 분리될 수 있다. 필요한 경우 request 시점의 authorization 결과와 stable app/package identity를 함께 캡처하고, deferred work에는 제한된 delegation을 전달한다. `clearCallingIdentity` 이후 nested call이 service 자신의 강한 권한으로 실행된다는 점을 code review에서 표시한다. negative test client를 다른 UID로 실행해 허용되지 않은 method와 object ID가 실제 service boundary에서 거부되는지 확인한다.

---

## CHAPTER 15 · ContentProvider는 structured data IPC이면서 permission boundary다

ContentProvider는 URI 기반 query/insert/update/delete interface를 다른 component/process에 노출할 수 있다. exported 여부, read/write permission, URI grant가 attack surface를 결정한다.

selection/URI/path를 외부 입력으로 받을 때 provider 내부 DB/file access와 연결되는 경로를 검증한다. caller가 허용된 row/resource 범위를 넘어설 수 없는지 authorization을 query 조건에 반영한다.

projection, sort order, selection argument와 URI path segment는 모두 외부 입력으로 취급한다. 문자열을 직접 SQL이나 file path에 합치지 않고 parameterization과 canonical object lookup을 사용한다. per-URI grant가 있으면 grant된 subtree보다 상위 resource로 `..`나 encoding 변형을 통해 탈출할 수 없는지 검사한다. query 결과 cursor가 민감한 column을 과도하게 노출하지 않는지도 projection allowlist로 통제한다. provider audit에는 caller UID, normalized URI, operation, affected row/object count를 남겨 대량 enumeration이나 범위 우회를 탐지할 수 있게 한다.

---

## CHAPTER 16 · Service는 background thread가 아니다

Service component는 lifecycle과 process importance 의미를 제공하지만 callback이 자동으로 별도 worker thread에서 실행되는 것은 아니다. blocking work를 어디에서 수행할지는 별도로 설계한다.

foreground service는 사용자에게 지속 작업을 알리고 platform restriction 아래 특정 long-running work를 수행하는 mechanism이지 arbitrary background execution bypass가 아니다. platform version별 restriction을 확인한다.

Service callback에서 worker를 시작했다면 component가 stop될 때 worker도 자동 종료된다고 가정하지 않는다. task ownership과 cancellation scope를 연결하고, restart policy가 같은 logical work를 중복 실행할 수 있는지 확인한다. foreground 전환·notification·start request 사이에는 platform deadline과 상태 규칙이 있으므로 startup path가 막히면 별도 failure가 될 수 있다. background 제한 회피를 위해 long-lived Service를 유지하는 대신 작업의 사용자 가시성, 지속성, deadline에 맞는 scheduler를 선택하고 실제 process kill 뒤 재개 semantics를 테스트한다.

---

## CHAPTER 17 · background work는 deadline, persistence, constraint에 따라 mechanism을 선택한다

즉시 UI와 연결된 coroutine work, process가 죽어도 재시도해야 하는 durable background task, exact alarm 요구는 서로 다른 문제다. 하나의 executor에 모두 넣으면 lifecycle과 reliability requirement가 충돌한다.

작업마다 `process death 후 재실행 필요 여부`, `network/charging constraint`, `maximum delay`, `duplicate execution 허용 여부`를 먼저 정의한다. retry 가능한 작업은 idempotent state transition으로 만든다.

persistent scheduler가 “정확히 한 번” 실행을 보장한다고 가정하기보다 at-least-once 가능성을 전제로 durable job ID와 commit state를 둔다. constraint가 실행 중 바뀌거나 process가 commit 직전 죽으면 동일 job이 다시 실행될 수 있다. 서버 side effect에는 idempotency key를 사용하고 local DB에서는 pending→running→committed 같은 state를 transaction으로 기록한다. deadline이 짧은 user-visible work와 maintenance work를 같은 queue에 넣지 않고 priority·constraint·battery cost를 분리한다. test는 enqueue 후 즉시 process kill, network loss, reboot/upgrade 같은 recovery boundary를 포함한다.

---

## CHAPTER 18 · class loading은 code availability와 initialization을 분리한다

class loader는 DEX/native path에서 class definition을 찾고 runtime type을 만든다. class가 load 가능한 것과 static initialization이 완료된 것은 별개다.

startup에서 예상치 못한 class initialization이 disk access나 heavy graph construction을 유발할 수 있다. trace와 method profile로 first-use initialization을 확인한다. dynamic feature/split package는 code availability 시점도 달라질 수 있다.

class initialization은 동기화 지점을 만들 수 있어 여러 thread가 같은 class를 처음 사용할 때 한 thread의 initializer를 기다릴 수 있다. initializer가 Binder나 disk I/O를 호출하면 unrelated thread가 type access만으로도 긴 stall을 겪을 수 있다. circular initialization과 failed initializer는 이후 접근 semantics도 복잡하게 만든다. startup trace에서 `<clinit>`과 class-load span을 분리하고, lazy initialization을 도입했다면 첫 사용이 latency-critical path로 이동하지 않았는지 확인한다. dynamic code가 준비되기 전 reference를 resolve하는 race도 별도 failure로 테스트한다.

---

## CHAPTER 19 · ART GC는 allocation rate와 live-set topology에 반응한다

managed heap allocation은 GC policy와 연결된다. short-lived allocation이 많으면 collection frequency가 올라갈 수 있고, long-lived object graph가 크면 marking/compaction 비용이 커진다.

GC pause 하나만 보지 않고 allocation rate, live set, native memory, concurrent GC CPU를 함께 본다. jank가 GC와 시간상 겹쳐도 root cause가 main-thread allocation burst인지 background pressure인지 확인한다.

GC log나 profiler에서는 collection cause, reclaimed bytes, pause phase, concurrent duration을 frame/request timeline과 맞춘다. allocation rate가 급증해 young collection이 잦은 경우와 retained graph가 커서 full collection 비용이 커진 경우의 수정 전략은 다르다. weak/reference queue, bitmap/native buffer처럼 managed object가 native resource를 간접 소유하는 패턴도 heap size만으로는 실제 pressure를 설명하지 못한다. 성능 변경 후 pause가 줄었더라도 concurrent GC CPU가 늘어 battery나 background throughput이 악화되지 않았는지 전체 resource cost를 본다.

---

## CHAPTER 20 · JNI/native boundary는 managed safety를 벗어나는 ownership 계약이다

JNI를 통해 native code로 넘어가면 raw pointer, manual lifetime, native thread, ABI 규칙이 적용된다. managed reference를 native code가 장기간 보유하려면 local/global reference lifetime을 구분해야 한다.

native crash는 Java exception으로 안전하게 변환되지 않을 수 있다. tombstone, native symbol, build ID, ABI별 shared library를 보존해야 재현 가능하다. JNI boundary에서 exception pending 상태와 thread attach/detach 규칙도 지켜야 한다.

managed array/string을 native에서 pin하거나 critical access로 오래 잡으면 collector의 이동/정지 정책에 영향을 줄 수 있으므로 native work duration을 짧게 유지하고 필요한 경우 별도 native buffer로 복사한다. native-created thread는 VM attach 상태와 TLS cleanup을 명시적으로 관리해야 한다. Java exception이 pending인 상태에서 다른 JNI call을 계속 수행하면 원래 오류를 덮거나 잘못된 state를 만들 수 있다. boundary마다 input length, ownership transfer, reference kind, exception contract를 문서화하고 sanitizer/native crash test로 use-after-free와 buffer overflow를 검증한다.

---

## CHAPTER 21 · ABI별 native library는 package compatibility를 결정한다

native `.so`는 target CPU ABI와 calling convention에 묶인다. package가 device ABI에 맞는 library를 포함하지 않으면 load 실패가 발생할 수 있다.

모든 ABI를 빌드한다고 correctness가 보장되는 것도 아니다. alignment, SIMD instruction, pointer width assumption, native dependency chain을 ABI별로 검증한다. crash report에는 architecture와 exact library build ID를 포함한다.

split APK나 App Bundle 배포에서는 device에 실제 설치된 ABI split이 무엇인지 확인해야 한다. top-level `.so`가 존재해도 transitive dependency가 같은 ABI로 패키징되지 않으면 load 단계에서 실패한다. `dlopen` failure는 library name만 보지 말고 search path, ELF class, required symbol/version, dependent soname을 추적한다. 32/64-bit 포인터 truncation이나 structure packing 문제는 특정 ABI에서만 나타날 수 있으므로 동일 native test vector를 각 supported ABI에서 실행하고 symbol file을 build ID 기준으로 보관한다.

---

## CHAPTER 22 · resource는 source filename이 아니라 compiled resource identity로 접근된다

Android build는 resource를 compile/package하고 resource ID와 configuration qualifier를 이용해 device 조건에 맞는 값을 선택한다. locale, density, night mode, screen size에 따라 동일 resource reference가 다른 asset/value를 resolve할 수 있다.

resource-not-found 문제에서 파일 존재 여부만 확인하지 않는다. merged resource, build variant, qualifier, package namespace를 확인한다. shrinker가 사용 분석에 실패해 필요한 resource를 제거하는 경우도 release build에서만 나타날 수 있다.

qualifier resolution은 여러 후보 중 device configuration에 가장 적합한 entry를 고르므로 fallback resource가 빠지면 특정 locale/density에서만 crash가 날 수 있다. dynamic lookup이나 reflection으로 resource name을 만들면 shrinker가 사용을 추론하지 못할 수 있어 keep rule이 필요하다. merge conflict를 조사할 때는 library와 app source filename보다 최종 merged table에서 어느 package/ID가 선택됐는지 본다. screenshot test도 기본 configuration 하나가 아니라 locale, font scale, night mode, density 조합을 골라 missing/incorrect resource와 layout overflow를 찾는다.

---

## CHAPTER 23 · application update는 code 교체와 persistent state migration을 함께 다룬다

새 APK가 설치되어도 기존 app data가 유지되는 update path가 일반적이다. 따라서 code version과 DB/preferences/file schema version이 달라질 수 있다.

migration은 forward path뿐 아니라 rollback compatibility와 interrupted migration을 고려한다. signing identity, version policy, native/data format compatibility를 release contract에 포함한다. update 후 process restart와 old background artifact가 섞이는 경계도 테스트한다.

migration은 version 숫자를 올리는 작업이 아니라 old durable state를 new invariant로 원자적으로 옮기는 과정이다. 큰 migration이 중간에 process death나 storage-full로 끊겨도 다음 시작에서 재실행 가능해야 하며, 완료 marker가 실제 data commit보다 먼저 기록되면 복구 불가능한 half-migrated state가 남는다. rollback을 지원한다면 새 code가 쓴 format을 old code가 읽을 수 있는 기간과 destructive migration 시점을 정한다. upgrade test matrix는 N-1뿐 아니라 오래된 supported version, interrupted migration, background worker가 old schema를 잡은 상황을 포함한다.

---

## CHAPTER 24 · startup trace는 first frame critical path를 증거로 분해한다

startup trace에서 process creation, main thread scheduling, class initialization, Binder call, disk I/O, layout/render span을 시간축으로 본다. wall-clock 한 숫자는 최적화 지점을 알려 주지 않는다.

instrumentation 자체의 overhead와 debug build distortion을 피하고 representative release-like build/device에서 측정한다. startup benchmark는 cold/warm state를 명시하고 여러 반복의 분포를 기록한다.

trace clock과 benchmark wall clock을 맞춰 launch request부터 first frame present까지 동일 iteration을 식별한다. main thread span만 보지 말고 Binder callee, RenderThread, I/O worker가 critical dependency인지 연결해야 한다. sampling profiler는 짧은 blocking event를 놓칠 수 있으므로 scheduler/binder/disk event를 포함한 system trace와 보완한다. regression 전후 trace는 같은 OS/device/thermal 조건에서 비교하고, total duration뿐 아니라 각 critical span의 시작 지연과 실행시간이 어떻게 달라졌는지 표로 만들면 optimization이 다른 stage로 병목을 이동시켰는지 확인할 수 있다.

---

## CHAPTER 25 · restore test는 process를 실제로 제거한 뒤 durable state만으로 재구성한다

화면 rotate나 navigation back-stack test는 process death를 대체하지 않는다. test는 process memory가 사라졌다고 가정하고 saved state, database, file, server state에서 UI와 transaction을 복원한다.

복원 중 network unavailable, schema migration, partial user input이 동시에 발생할 수 있다. `복원 성공`을 화면 표시 하나로 판단하지 않고 duplicate transaction, lost edit, stale authorization을 확인한다.

복원 harness는 kill 직전 화면의 logical state와 durable checkpoint를 기록하고, 새 process에서 어느 source로 각 field를 재구성했는지 검증한다. 서버 cache와 local pending edit가 충돌하면 version/merge rule이 필요하고, authentication이 만료됐으면 민감한 화면을 stale memory 없이 다시 gate해야 한다. upload/payment 같은 side effect는 pending operation ID로 이전 시도 상태를 조회해 중복 실행을 피한다. restore 완료 뒤 navigation back stack, scroll/form state, committed business data를 서로 다른 중요도로 검증하면 UI 편의 state와 transaction correctness를 혼동하지 않는다.

---

## CHAPTER 26 · Android 진단은 Java stack 하나보다 여러 evidence plane을 결합한다

문제 유형별 증거가 다르다.

```text
ANR            → main/Binder thread stack, system ANR reason, trace
native crash   → tombstone, signal, registers, native backtrace, build ID
jank           → frame timeline, main/render/GPU scheduling
memory         → heap allocation, RSS/PSS, native/graphics, GC
startup        → process/class/Binder/disk trace
IPC latency    → caller/callee Binder spans
```

logcat은 사건 설명에 유용하지만 timing과 resource usage 전체를 증명하지 않는다. Perfetto/system trace, dumpsys, tombstone, profiler를 같은 build/device timestamp로 연결해 원인 chain을 만든다.

증거 묶음에는 app version, OS build, device model, process start time, PID/TID, native build ID와 trace clock 기준을 포함한다. process가 재시작되면 같은 PID가 나중에 재사용될 수 있으므로 timestamp와 process generation을 같이 사용한다. ANR stack 한 장과 jank trace가 서로 다른 실행에서 수집됐다면 억지로 causal chain으로 합치지 않는다. incident마다 동일 reproduction run에서 나온 trace·log·memory snapshot을 우선 연결하고, 개인정보가 포함될 수 있는 payload는 최소화한다. 수정 검증은 원래 evidence signature가 사라졌는지와 새로운 bottleneck이 생기지 않았는지를 같은 계측 세트로 비교한다.