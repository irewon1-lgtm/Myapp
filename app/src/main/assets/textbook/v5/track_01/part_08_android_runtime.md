# PART 08 · Android runtime — package, ART, Zygote, Binder, lifecycle

Android application의 실행은 APK 내부 code를 CPU가 직접 읽는 과정이 아니다. package 설치 상태, DEX, ART compilation mode, Zygote process creation, framework component lifecycle, Binder IPC, process importance가 결합된 platform execution model이다.

---

## CHAPTER 01 · APK는 설치 가능한 package format이고 runtime image와 동일하지 않다

APK에는 DEX code, compiled resource, manifest, asset, native library, signing metadata가 들어갈 수 있다. package manager는 설치 시 package identity·signature·component declaration·permission 정보를 system state에 반영한다.

설치된 package가 하나여도 실행 중 process는 여러 개일 수 있고 process가 종료되어도 package와 durable app data는 남는다. `앱이 설치됨`, `process가 존재함`, `Activity가 화면에 있음`은 독립 상태다.

---

## CHAPTER 02 · DEX는 managed runtime이 소비하는 bytecode representation이다

Kotlin/Java source는 build pipeline을 거쳐 DEX representation으로 변환된다. DEX는 source syntax가 아니라 register-based bytecode와 type/method metadata를 가진 runtime input이다.

minification/optimization 과정에서 class·method 이름이 바뀔 수 있으므로 production stack trace를 source로 복원하려면 mapping artifact를 보존해야 한다. build artifact identity와 symbol/mapping identity를 함께 관리한다.

---

## CHAPTER 03 · ART는 interpretation, JIT, AOT를 상황에 맞게 조합한다

ART는 DEX를 실행하며 platform version과 compilation policy에 따라 interpreter, JIT, ahead-of-time compilation을 조합할 수 있다. 따라서 `Android는 전부 JIT` 또는 `전부 AOT`라는 설명은 정확하지 않다.

startup, storage footprint, installation/update cost, steady-state performance는 서로 다른 목표다. compilation policy가 바뀌면 동일 APK도 실행 특성이 달라질 수 있으므로 runtime/platform version을 performance evidence에 포함한다.

---

## CHAPTER 04 · JIT는 runtime profile을 이용해 hot code에 compilation budget을 집중한다

JIT compiler는 실행 중 관찰한 code path를 기반으로 native code를 생성할 수 있다. hotness 판단, code cache, profile collection 자체가 runtime resource를 사용한다.

benchmark 초반과 장시간 실행 결과가 다른 이유 중 하나가 warmup과 compilation state다. microbenchmark에서 JIT warmup을 통제하지 않으면 source 변경보다 runtime compilation timing을 측정할 수 있다.

---

## CHAPTER 05 · AOT compilation은 설치된 code 전체를 무조건 native로 만드는 과정이 아니다

ART의 dex optimization/AOT 정책은 platform release와 device condition에 따라 달라질 수 있다. Android 14 이후에는 ART Service가 dexopt 관련 역할을 담당한다. profile-guided compilation은 자주 실행되는 영역에 최적화 비용을 집중할 수 있다.

성능 문제에서 `AOT가 안 됐다`고 추정하기 전에 실제 compilation state와 startup trace를 확인한다. package update, OS update, profile 상태가 compilation artifact를 바꿀 수 있다.

---

## CHAPTER 06 · Zygote는 공통 runtime state를 준비한 뒤 app process 생성 비용을 줄인다

Zygote 계열 process는 framework/runtime class와 공통 state를 미리 준비하고 application process 생성에 사용된다. fork 계열 생성은 copy-on-write를 활용해 초기 memory sharing 이점을 얻을 수 있다.

process 생성 이후 app-specific initialization과 class loading, Application/component startup이 이어진다. startup memory가 공유 상태에서 private page로 변하는 write pattern은 COW와 연결된다.

---

## CHAPTER 07 · cold startup latency는 process creation부터 first frame까지 이어진다

cold start에서는 process가 없기 때문에 process creation, runtime/class initialization, Application, component creation, UI composition/layout/render가 모두 경로에 들어갈 수 있다.

한 phase를 줄여도 critical path 밖이면 first-frame latency가 줄지 않는다. startup optimization은 trace에서 critical path를 찾고 main-thread blocking I/O, eager initialization, class loading, heavy DI graph 같은 실제 span을 줄인다.

---

## CHAPTER 08 · warm/hot start 구분은 존재하는 state의 차이다

process와 Activity/task state가 얼마나 남아 있는지에 따라 startup path가 달라진다. 이미 process가 살아 있는 warm path와 process까지 새로 필요한 cold path의 비용을 평균 하나로 섞으면 regression을 놓친다.

성능 지표는 startup type과 device state를 분리해 수집한다. cache가 따뜻한 반복 실행만 측정하면 실제 사용자의 cold-start 문제를 숨길 수 있다.

---

## CHAPTER 09 · component lifecycle은 process lifetime과 같은 것이 아니다

Activity, Service, Provider 등의 lifecycle callback은 framework가 component state를 관리하기 위한 계약이다. process 자체는 background importance와 system pressure에 따라 component가 눈에 보이지 않는 동안 종료될 수 있다.

메모리에 있는 singleton이나 static object를 영구 source of truth로 사용하면 process recreation 후 상태가 사라진다. UI state, durable state, server-derived cache를 서로 다른 lifetime으로 설계한다.

---

## CHAPTER 10 · process death는 exception이 아니라 platform lifecycle의 정상 가능성이다

background process는 memory pressure나 platform policy에 따라 종료될 수 있다. application이 다음 실행에서 이전 heap을 그대로 복원받는 것은 아니다.

복원 설계는 `현재 화면을 재구성하는 최소 state`, `다시 fetch 가능한 state`, `반드시 durable해야 하는 user transaction`을 구분한다. process kill test 없이 rotation/recomposition만 테스트하면 recovery contract가 검증되지 않는다.

---

## CHAPTER 11 · main Looper/MessageQueue는 UI thread의 scheduling contract다

main thread는 Looper와 MessageQueue를 통해 callback/message를 순차 처리한다. 한 callback이 오래 실행되면 뒤의 input, lifecycle callback, render-related work가 지연된다.

ANR/jank 원인은 CPU 전체가 느린 것이 아니라 main thread가 deadline 동안 blocked 또는 busy인 경우가 많다. thread trace에서 runnable/on-CPU, monitor wait, Binder wait, file/network I/O를 분리한다.

---

## CHAPTER 12 · coroutine은 thread를 없애지 않고 suspension과 continuation을 추상화한다

Kotlin coroutine은 suspend point에서 execution을 중단하고 continuation을 나중에 resume할 수 있게 한다. 실제 work는 dispatcher가 선택한 thread/executor에서 실행된다.

`suspend` 함수라고 CPU-bound work가 자동으로 background thread로 이동하지 않는다. dispatcher 선택, structured cancellation, lifecycle scope를 확인한다. blocking call을 limited thread pool에 몰아넣으면 coroutine 수가 많아도 underlying pool saturation이 발생한다.

---

## CHAPTER 13 · Binder transaction은 process boundary와 scheduler boundary를 동시에 넘는다

Binder proxy call은 local method syntax처럼 보일 수 있지만 remote service process의 Binder thread에서 실행될 수 있다. serialization, kernel transaction, remote scheduling, reply가 latency에 포함된다.

large transaction, nested synchronous Binder call, Binder thread pool exhaustion은 deadlock/latency chain을 만들 수 있다. trace에서 caller→Binder→callee 관계를 이어서 본다.

---

## CHAPTER 14 · Binder authorization은 caller identity를 기준으로 수행해야 한다

privileged service가 Binder request를 받을 때 caller UID/permission을 검증하지 않으면 service 권한이 confused deputy로 악용될 수 있다. input validation과 authorization은 별도다.

identity를 임시로 clear/restore하는 API를 사용할 경우 scope를 정확히 제한한다. privileged identity로 수행해야 하는 최소 operation만 분리하고 예외 path에서도 identity restoration을 보장한다.

---

## CHAPTER 15 · ContentProvider는 structured data IPC이면서 permission boundary다

ContentProvider는 URI 기반 query/insert/update/delete interface를 다른 component/process에 노출할 수 있다. exported 여부, read/write permission, URI grant가 attack surface를 결정한다.

selection/URI/path를 외부 입력으로 받을 때 provider 내부 DB/file access와 연결되는 경로를 검증한다. caller가 허용된 row/resource 범위를 넘어설 수 없는지 authorization을 query 조건에 반영한다.

---

## CHAPTER 16 · Service는 background thread가 아니다

Service component는 lifecycle과 process importance 의미를 제공하지만 callback이 자동으로 별도 worker thread에서 실행되는 것은 아니다. blocking work를 어디에서 수행할지는 별도로 설계한다.

foreground service는 사용자에게 지속 작업을 알리고 platform restriction 아래 특정 long-running work를 수행하는 mechanism이지 arbitrary background execution bypass가 아니다. platform version별 restriction을 확인한다.

---

## CHAPTER 17 · background work는 deadline, persistence, constraint에 따라 mechanism을 선택한다

즉시 UI와 연결된 coroutine work, process가 죽어도 재시도해야 하는 durable background task, exact alarm 요구는 서로 다른 문제다. 하나의 executor에 모두 넣으면 lifecycle과 reliability requirement가 충돌한다.

작업마다 `process death 후 재실행 필요 여부`, `network/charging constraint`, `maximum delay`, `duplicate execution 허용 여부`를 먼저 정의한다. retry 가능한 작업은 idempotent state transition으로 만든다.

---

## CHAPTER 18 · class loading은 code availability와 initialization을 분리한다

class loader는 DEX/native path에서 class definition을 찾고 runtime type을 만든다. class가 load 가능한 것과 static initialization이 완료된 것은 별개다.

startup에서 예상치 못한 class initialization이 disk access나 heavy graph construction을 유발할 수 있다. trace와 method profile로 first-use initialization을 확인한다. dynamic feature/split package는 code availability 시점도 달라질 수 있다.

---

## CHAPTER 19 · ART GC는 allocation rate와 live-set topology에 반응한다

managed heap allocation은 GC policy와 연결된다. short-lived allocation이 많으면 collection frequency가 올라갈 수 있고, long-lived object graph가 크면 marking/compaction 비용이 커진다.

GC pause 하나만 보지 않고 allocation rate, live set, native memory, concurrent GC CPU를 함께 본다. jank가 GC와 시간상 겹쳐도 root cause가 main-thread allocation burst인지 background pressure인지 확인한다.

---

## CHAPTER 20 · JNI/native boundary는 managed safety를 벗어나는 ownership 계약이다

JNI를 통해 native code로 넘어가면 raw pointer, manual lifetime, native thread, ABI 규칙이 적용된다. managed reference를 native code가 장기간 보유하려면 local/global reference lifetime을 구분해야 한다.

native crash는 Java exception으로 안전하게 변환되지 않을 수 있다. tombstone, native symbol, build ID, ABI별 shared library를 보존해야 재현 가능하다. JNI boundary에서 exception pending 상태와 thread attach/detach 규칙도 지켜야 한다.

---

## CHAPTER 21 · ABI별 native library는 package compatibility를 결정한다

native `.so`는 target CPU ABI와 calling convention에 묶인다. package가 device ABI에 맞는 library를 포함하지 않으면 load 실패가 발생할 수 있다.

모든 ABI를 빌드한다고 correctness가 보장되는 것도 아니다. alignment, SIMD instruction, pointer width assumption, native dependency chain을 ABI별로 검증한다. crash report에는 architecture와 exact library build ID를 포함한다.

---

## CHAPTER 22 · resource는 source filename이 아니라 compiled resource identity로 접근된다

Android build는 resource를 compile/package하고 resource ID와 configuration qualifier를 이용해 device 조건에 맞는 값을 선택한다. locale, density, night mode, screen size에 따라 동일 resource reference가 다른 asset/value를 resolve할 수 있다.

resource-not-found 문제에서 파일 존재 여부만 확인하지 않는다. merged resource, build variant, qualifier, package namespace를 확인한다. shrinker가 사용 분석에 실패해 필요한 resource를 제거하는 경우도 release build에서만 나타날 수 있다.

---

## CHAPTER 23 · application update는 code 교체와 persistent state migration을 함께 다룬다

새 APK가 설치되어도 기존 app data가 유지되는 update path가 일반적이다. 따라서 code version과 DB/preferences/file schema version이 달라질 수 있다.

migration은 forward path뿐 아니라 rollback compatibility와 interrupted migration을 고려한다. signing identity, version policy, native/data format compatibility를 release contract에 포함한다. update 후 process restart와 old background artifact가 섞이는 경계도 테스트한다.

---

## CHAPTER 24 · startup trace는 first frame critical path를 증거로 분해한다

startup trace에서 process creation, main thread scheduling, class initialization, Binder call, disk I/O, layout/render span을 시간축으로 본다. wall-clock 한 숫자는 최적화 지점을 알려 주지 않는다.

instrumentation 자체의 overhead와 debug build distortion을 피하고 representative release-like build/device에서 측정한다. startup benchmark는 cold/warm state를 명시하고 여러 반복의 분포를 기록한다.

---

## CHAPTER 25 · restore test는 process를 실제로 제거한 뒤 durable state만으로 재구성한다

화면 rotate나 navigation back-stack test는 process death를 대체하지 않는다. test는 process memory가 사라졌다고 가정하고 saved state, database, file, server state에서 UI와 transaction을 복원한다.

복원 중 network unavailable, schema migration, partial user input이 동시에 발생할 수 있다. `복원 성공`을 화면 표시 하나로 판단하지 않고 duplicate transaction, lost edit, stale authorization을 확인한다.

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