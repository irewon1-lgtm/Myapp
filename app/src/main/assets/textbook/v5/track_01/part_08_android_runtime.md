# PART 08 · Android 앱은 APK 한 파일이 바로 CPU에서 실행되는 구조가 아니다

Android 앱 개발에서 `빌드 → 설치 → 실행`은 버튼 몇 번으로 끝나 보인다. 하지만 실제로는 package가 설치되고, DEX bytecode가 ART runtime의 해석/JIT/AOT 전략과 결합되고, Zygote 계열 process에서 app process가 생성되며, framework component와 Binder service가 lifecycle을 관리한다.

이 PART에서는 특정 Android API 사용법을 외우기보다 **앱 package가 process가 되고, code가 실행되고, system service와 통신하며, process가 죽었다 다시 만들어지는 전체 경로**를 이해한다.

---

## CHAPTER 01 · APK는 실행 파일 하나라기보다 배포 container다

Android Package에는 code와 resource, manifest, signing 관련 정보 등이 들어간다.

단순 구조:

```text
APK
├─ AndroidManifest.xml
├─ classes*.dex
├─ resources / compiled resources
├─ assets
├─ native libraries (.so) if any
└─ signing metadata
```

실제 packaging 구조와 compression은 build 설정에 따라 달라질 수 있다.

### DEX와 native library를 구분한다

Kotlin/Java source는 Android build chain을 통해 DEX bytecode 형태로 package에 들어가는 것이 일반적이다.

C/C++ native code는 ABI별 `.so` library로 들어갈 수 있다.

```text
Kotlin/Java source
↓ compilation/desugaring/dexing
DEX

C/C++ source
↓ native compiler/linker
arm64-v8a .so 등
```

둘은 runtime loading과 portability가 다르다.

---

## CHAPTER 02 · DEX는 CPU machine code 그 자체가 아니다

Dalvik Executable format의 bytecode는 ART가 실행하는 입력 형식이다.

CPU는 DEX opcode를 hardware instruction으로 직접 실행하지 않는다.

ART가 interpreter, JIT compiled code, AOT compiled artifact 등을 이용해 target CPU에서 실행한다.

이 구조 때문에:

```text
DEX size
≠ native code size
≠ runtime memory footprint
```

이다.

---

## CHAPTER 03 · ART는 interpreted 대 compiled의 단순 이분법이 아니다

현재 Android runtime은 상황에 따라 해석, JIT, AOT를 조합할 수 있다.

개념적으로:

```text
method not compiled
→ interpreter execution possible

hot method
→ JIT compile possible

profile-selected method
→ AOT compiled artifact possible
```

정확한 compile filter와 artifact 구성은 Android release/device configuration에 따라 달라질 수 있다.

### 왜 전부 AOT하지 않는가

모든 method를 installation 순간 최고 optimization으로 compile하면:

```text
install/update time 증가
compiled code storage 증가
실제로 한 번도 안 쓰는 code까지 compile
```

비용이 생긴다.

반대로 전부 interpreter/JIT에 맡기면 초기 execution cost와 runtime compiler overhead가 생긴다.

ART는 profile-guided compilation 등으로 trade-off를 조정한다.

---

## CHAPTER 04 · JIT는 실행 중 얻은 정보를 사용할 수 있다

JIT compiler는 실제 실행을 관찰해 자주 실행되는 method나 runtime type information을 최적화에 활용할 수 있다.

```text
method executes repeatedly
↓
hotness/profile data
↓
JIT compile
↓
subsequent call uses compiled code
```

### warm-up 효과

benchmark 첫 실행과 100번째 실행의 성능이 다를 수 있다.

JIT, class loading, cache warm-up이 섞이기 때문이다.

Android microbenchmark에서 warm-up과 compilation mode를 통제해야 하는 이유다.

### code cache도 memory다

JIT-compiled native code는 code cache 같은 runtime memory를 사용한다.

`JIT로 빨라졌다`는 것은 memory/storage/compile CPU 비용이 공짜라는 뜻이 아니다.

---

## CHAPTER 05 · AOT도 고정된 한 번의 installation compiler가 아니다

Android의 AOT compilation은 profile과 device idle/charging 상태, system policy에 따라 나중에 수행될 수 있다.

Android 14 이후 on-device dexopt 관리에는 ART Service가 사용된다.

따라서 `APK 설치 순간 모든 code가 native로 완성된다`는 오래된 단순 모델은 현재 Android를 정확히 설명하지 못한다.

### compilation artifact는 release에 따라 바뀔 수 있다

`.odex`, `.vdex`, `.art` 같은 artifact 용어를 볼 수 있지만 파일 구성과 의미를 app logic에서 hard-code해서 의존하면 안 된다.

AOSP 내부 구현 세부는 release별로 달라질 수 있다.

---

## CHAPTER 06 · Zygote는 app process creation cost를 줄이는 기반이다

Android boot 과정에서 Zygote process가 준비되고 app/system process 생성의 root 역할을 한다.

공통 class/resource를 미리 load한 상태에서 process를 fork-like 방식으로 만들면 copy-on-write를 활용해 startup cost와 memory sharing에 이점이 생길 수 있다.

```text
Zygote
common runtime/classes loaded
↓
fork/specialize
↓
app process
```

PART 03의 copy-on-write가 Android process startup과 연결된다.

### USAP

현대 Android에는 Unspecialized App Process를 미리 준비했다가 app launch 시 specialize하는 방식도 있을 수 있다.

중요한 것은 `앱 실행 = executable file 새로 처음부터 읽기` 한 단계가 아니라 **system이 process creation pipeline 자체를 최적화한다**는 점이다.

---

## CHAPTER 07 · app process가 생겼다고 Activity가 곧 화면에 그려지는 것은 아니다

startup path에는 여러 작업이 있다.

```text
process create/specialize
↓
runtime init
↓
Application creation
↓
component/activity creation
↓
class loading / DI / data init
↓
UI composition/layout/draw
↓
first frame
```

각 단계가 startup latency를 만든다.

### Application.onCreate에 모든 초기화를 몰지 않는다

사용자가 첫 화면에서 필요하지 않은 SDK, DB migration, network config, large object graph를 synchronously 초기화하면 first frame이 늦어진다.

lazy/deferred initialization은 필요 시점을 뒤로 미룰 수 있지만 thread safety와 first-use latency를 고려해야 한다.

---

## CHAPTER 08 · cold, warm, hot start를 구분한다

### cold start

app process 자체가 없는 상태에서 process creation부터 필요하다.

### warm/hot start

process가 살아 있거나 Activity/task 상태 일부가 남아 있어 더 적은 단계를 거칠 수 있다.

정확한 Android 성능 도구의 분류 정의를 사용해야 하지만 핵심은 **startup benchmark 전에 process 상태를 고정해야 한다**는 것이다.

cold start와 hot start 숫자를 섞어 평균 내면 의미가 없다.

---

## CHAPTER 09 · lifecycle callback은 process 생존 보장이 아니다

Activity lifecycle은 UI component 상태 전환을 표현한다.

```text
created
started
resumed
paused
stopped
```

하지만 `stopped Activity object가 메모리에 있으니 process가 계속 살아 있다`는 보장은 없다.

Android system은 memory pressure와 process importance에 따라 background process를 종료할 수 있다.

### onDestroy를 영구 저장 시점으로 믿지 않는다

process kill에서 모든 callback이 실행될 것이라 가정하면 user data를 잃을 수 있다.

중요한 data는 적절한 시점에 durable state로 저장해야 한다.

---

## CHAPTER 10 · configuration change와 process death는 다른 문제다

화면 회전 같은 configuration change는 Activity recreation을 일으킬 수 있다.

process death는 runtime memory 전체가 사라지는 더 큰 사건이다.

ViewModel은 configuration recreation에는 state 보존에 도움을 줄 수 있지만 process death 뒤 모든 in-memory state를 자동 복구해 주는 persistent database가 아니다.

### state 계층을 구분한다

```text
UI ephemeral state
→ 현재 frame/interaction

saved instance state
→ 작은 복원 정보

ViewModel/process memory
→ process 살아 있는 동안의 screen state

durable repository/DB/file
→ process/device restart 뒤에도 필요한 state
```

모든 state를 하나의 bucket에 넣지 않는다.

---

## CHAPTER 11 · Android main thread와 Looper/MessageQueue

Android main thread는 event를 MessageQueue/Looper 계열 mechanism으로 처리한다.

```text
message/callback posted
↓
queue
↓
Looper dispatch
↓
handler executes
```

main thread handler가 오래 걸리면 뒤 message가 지연된다.

### `비동기로 호출했다`가 main thread free를 보장하지 않는다

callback 결과를 main thread에서 받아 heavy JSON parse나 bitmap transform을 수행하면 network 자체는 background였어도 UI는 멈출 수 있다.

작업의 **어느 구간이 어느 dispatcher/thread에서 실행되는지** 추적해야 한다.

---

## CHAPTER 12 · coroutine은 thread와 같은 것이 아니다

Kotlin coroutine은 suspend/resume 가능한 computation abstraction이다.

여러 coroutine이 적은 수의 thread에서 실행될 수 있다.

```text
coroutine A running on worker 1
A suspends for I/O
worker 1 runs B
A resumes later, possibly another worker
```

### thread-local 가정에 주의

coroutine이 suspension 전후 같은 physical thread에서 실행된다고 임의로 가정하면 안 된다.

ThreadLocal, transaction context, MDC 같은 thread-bound state는 coroutine-aware propagation mechanism이 필요할 수 있다.

### blocking과 suspending

`suspend` 함수라고 내부에서 blocking I/O를 하지 않는다는 보장은 없다.

implementation이 blocking API를 그대로 호출하면 underlying thread는 여전히 block된다.

API 이름보다 실제 behavior를 본다.

---

## CHAPTER 13 · Binder는 framework 호출의 process boundary를 숨길 수 있다

app이 system service method를 호출할 때 실제 구현이 system_server 같은 다른 process에 있을 수 있다.

proxy/stub layer가 parcel serialization과 Binder transaction을 처리한다.

```text
app object call
↓ proxy
Parcel encoding
↓ Binder driver
service thread
↓ implementation
reply Parcel
↓
app
```

### Binder transaction에는 size/serialization cost가 있다

큰 bitmap/list를 Binder argument로 계속 보내는 것은 shared process memory reference 전달과 다르다.

data를 parcelable representation으로 옮기는 비용과 transaction buffer 제약이 있다.

큰 data는 file descriptor/shared memory/content provider 등 다른 mechanism이 적합할 수 있다.

---

## CHAPTER 14 · app sandbox와 Binder identity가 authorization을 만든다

system service는 caller UID/PID 같은 identity를 얻어 permission을 검사할 수 있다.

service가 자기 높은 권한으로 모든 caller 요청을 실행하면 confused deputy 취약점이 될 수 있다.

IPC boundary는 network API와 마찬가지로 **trust boundary**다.

```text
validate input
check caller identity
check permission
perform narrow operation
return sanitized result
```

---

## CHAPTER 15 · ContentProvider는 DB wrapper만이 아니다

ContentProvider는 process 간 structured data access interface를 제공할 수 있다.

호출은 provider process의 Binder thread pool에서 들어올 수 있으므로 query/insert/update implementation이 concurrent call을 받을 수 있다는 점을 고려해야 한다.

`UI thread에서만 호출될 것`이라는 가정은 위험하다.

### URI permission

특정 URI에 일시적으로 read/write permission을 grant하는 mechanism을 이용해 파일/data를 다른 app과 공유할 수 있다.

전체 filesystem path를 공개하는 것보다 좁은 capability를 전달하는 설계다.

---

## CHAPTER 16 · Service는 background thread의 동의어가 아니다

Android Service component가 별도 process/thread에서 자동 실행된다고 생각하면 틀린다.

기본적으로 같은 app process/main thread에서 callback이 실행될 수 있다.

따라서 service 안에서 blocking 작업을 바로 수행하면 main thread를 막을 수 있다.

### foreground service

foreground service는 사용자에게 인지되는 지속 작업을 위한 특별한 lifecycle/notification 정책과 연결된다.

`백그라운드에서 오래 돌리고 싶다`는 이유만으로 무조건 foreground service를 쓰는 것이 아니라 Android release의 background execution policy를 따라야 한다.

---

## CHAPTER 17 · WorkManager와 job scheduling은 process death를 전제로 한다

지연 가능한 persistent background work는 process memory의 thread 하나로만 표현하면 app process가 죽을 때 사라진다.

system-backed scheduler는 work intent/state를 durable하게 관리하고 적절한 조건에서 process를 다시 시작해 작업할 수 있다.

### exactly-once 환상

process crash/retry가 가능한 background job은 동일 work가 재실행될 수 있다고 가정하는 편이 안전하다.

external side effect가 있다면 idempotency가 필요하다.

```text
job starts
server payment request sent
app crashes before local completion mark
job retries
```

server/client가 duplicate를 안전하게 처리하지 않으면 이중 결제가 가능하다.

---

## CHAPTER 18 · class loading은 code를 메모리에 준비하는 과정이다

DEX에 class가 존재한다고 모든 class가 startup에서 즉시 initialized되는 것은 아니다.

class loading, verification, initialization은 필요 시점에 발생할 수 있다.

### static initializer

무거운 static initialization은 class first-use latency를 크게 만들 수 있다.

```text
class referenced first time
↓
load/verify
↓
static init
↓
actual method
```

startup trace에서 예상하지 못한 class initialization cost가 나올 수 있다.

---

## CHAPTER 19 · GC pause와 allocation churn

ART GC는 많은 작업을 concurrent하게 수행하도록 발전했지만 application allocation pattern은 여전히 성능에 영향을 준다.

짧은 frame마다 많은 temporary object를 만들면 allocation/collection work가 증가할 수 있다.

### `객체 생성 = 느림`으로 과도하게 단순화하지 않는다

modern allocator의 short-lived allocation은 매우 빠를 수 있다.

무조건 object pooling을 적용하면:

```text
code complexity
stale state bug
larger retained memory
synchronization cost
```

가 생길 수 있다.

실제 allocation profile과 GC pause를 측정한다.

---

## CHAPTER 20 · native code는 managed runtime 밖의 문제를 추가한다

JNI를 통해 C/C++ code를 호출하면 native memory safety와 ABI 문제가 생긴다.

```text
Kotlin/Java managed world
↓ JNI boundary
C/C++ native code
```

### ownership

native allocation을 누가 free하는지 명확해야 한다.

managed object finalizer에 cleanup을 의존하면 release 시점이 늦거나 불확실할 수 있다.

### JNI call overhead

작은 operation마다 JNI boundary를 반복 왕복하면 marshalling과 transition 비용이 커질 수 있다.

큰 batch로 처리할 수 있는지 본다.

---

## CHAPTER 21 · ABI와 native library packaging

Android device CPU architecture와 native `.so` ABI가 맞아야 한다.

예:

```text
arm64-v8a
x86_64
```

지원하지 않는 ABI만 package하면 install/load 문제가 생길 수 있다.

### fat package와 split

여러 ABI native library를 모두 하나의 package에 넣으면 size가 커질 수 있다.

modern distribution은 device에 필요한 split만 전달하는 방식으로 다운로드 크기를 줄일 수 있다.

---

## CHAPTER 22 · resource는 code와 다른 loading path를 가진다

layout, drawable, string 같은 Android resource는 build 과정에서 compile/link되어 resource table과 package data로 들어간다.

runtime에서 resource ID를 통해 조회한다.

### resource qualifier

```text
locale
screen density
screen size
night mode
```

같은 configuration에 따라 다른 resource variant가 선택될 수 있다.

`파일 하나를 읽는다`보다 복잡한 selection logic이다.

---

## CHAPTER 23 · app update는 running process와 installed package state를 바꾼다

새 APK가 설치되면 code/resource version이 바뀐다.

process가 restart되고 persistent data는 이전 schema일 수 있다.

그래서 update compatibility에는 다음이 포함된다.

```text
DB migration
preferences migration
serialized file version
server API compatibility
saved state compatibility
native library ABI
```

빌드가 성공했다고 update가 안전한 것은 아니다.

---

## CHAPTER 24 · 앱 시작 성능을 실제 trace로 분해한다

상황: cold start가 2.5초.

먼저 단계별 trace를 본다.

```text
process creation
runtime/class loading
ContentProvider auto init
Application.onCreate
DI graph
DB open/migration
first Activity create
compose/layout
first frame
```

각 구간의 duration을 측정한다.

### 흔한 잘못된 최적화

logo animation 100ms 줄이는 동안 Application.onCreate가 1.2초 걸리는 것을 무시하는 경우다.

가장 큰 span부터 줄인다.

### startup metric

사용자가 실제 내용을 볼 수 있는 시점을 정의해야 한다.

process start timestamp 하나로 UX를 설명하기 어렵다.

first frame, fully drawn, interactive-ready 등 목표를 구분한다.

---

## CHAPTER 25 · process death 복원 시나리오를 테스트한다

화면 회전만 테스트하고 `앱을 background에 오래 뒀다 돌아오기`를 process가 살아 있는 상황에서만 테스트하면 복원 bug를 놓친다.

검증 시나리오:

```text
1. form에 값 입력
2. navigation stack 이동
3. process kill 조건 재현
4. app/task 복원
5. user-critical state 확인
6. duplicate network request 여부 확인
```

state ownership을 계층별로 테스트해야 한다.

---

## CHAPTER 26 · Android 장애를 계층으로 분류한다

```text
build/package failure
install/signature failure
process startup failure
class loading/runtime failure
main-thread ANR
managed exception crash
native crash
Binder/service failure
resource/configuration bug
memory pressure/process death
persistent state migration bug
```

모든 것을 `앱이 안 됨`으로 묶지 않는다.

### 증거

```text
logcat
ANR trace
Java/Kotlin stack trace
native tombstone
perfetto/system trace
dumpsys meminfo
package manager state
DB/file evidence
```

증상에 맞는 evidence source를 선택한다.

---

## PART 08 종료 점검

1. APK와 DEX와 native machine code는 무엇이 다른가?
2. ART가 interpreter/JIT/AOT를 조합하는 이유는 무엇인가?
3. profile-guided compilation이 install time·storage·runtime trade-off를 어떻게 바꾸는가?
4. Zygote와 copy-on-write는 app startup에서 어떻게 연결되는가?
5. cold start와 hot start benchmark를 섞으면 왜 안 되는가?
6. Activity lifecycle과 process lifetime이 왜 같은 것이 아닌가?
7. ViewModel이 process-death persistent storage가 아닌 이유는 무엇인가?
8. coroutine과 thread가 같은 것이 아닌 이유는 무엇인가?
9. Binder call이 local call과 다른 실패 모드를 갖는 이유는 무엇인가?
10. Service가 자동 background thread가 아닌 이유는 무엇인가?
11. persistent background job에서 idempotency가 필요한 이유는 무엇인가?
12. class/static initialization이 startup latency에 어떻게 숨어들 수 있는가?
13. JNI가 memory ownership과 ABI 문제를 추가하는 이유는 무엇인가?
14. update 검증에 migration이 포함돼야 하는 이유는 무엇인가?
15. Android 문제를 build/runtime/ANR/native/process-death 층으로 나누면 무엇이 좋아지는가?

Android 앱을 `Kotlin 코드가 APK가 되어 실행된다`라고만 설명하지 않는다. **package → DEX/native → ART → Zygote/process → framework/main loop → Binder/system service → lifecycle/persistence**의 전체 경로를 연결할 수 있어야 한다.