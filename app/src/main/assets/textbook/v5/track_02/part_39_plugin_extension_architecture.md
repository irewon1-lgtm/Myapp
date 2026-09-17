# PART 39 · Plugin과 extension architecture — core contract를 고정하고 외부 behavior를 안전하게 확장하기

프로그램 기능을 core release 없이 추가하려면 plugin, hook, registry 같은 extension point를 만들 수 있다. 하지만 “아무 module이나 import해 실행”하는 구조는 API compatibility, startup failure, dependency collision, security trust를 모두 plugin 시스템으로 끌어들인다. 확장성의 핵심은 동적 import 자체가 아니라 **core가 어떤 capability를 공개하고 extension이 어느 lifetime과 권한 안에서 동작하는지 제한하는 것**이다.

---

## CHAPTER 01 · extension point는 core가 외부 구현에게 허용하는 작은 계약이다

Plugin interface를 설계할 때 core 내부 object 전체를 넘기면 plugin이 private field와 database connection, cache에 직접 의존하게 된다. 처음에는 자유롭지만 core refactor가 거의 불가능해진다. Plugin이 실제로 필요한 operation만 protocol/context로 제공한다.

예를 들어 formatter plugin은 `format(record) -> str`만 필요할 수 있다. Repository와 network client까지 context에 넣을 이유가 없다. Capability가 작을수록 compatibility와 security boundary가 선명하다.

Extension point는 input/output뿐 아니라 thread/async context, error behavior, timeout, 호출 횟수를 포함한다. Plugin이 한 request마다 호출되는지 startup에서 한 번 호출되는지에 따라 허용 비용이 다르다.

Core가 제공하는 contract를 versioned public API로 취급한다. Internal helper를 plugin 문서에 노출하면 사실상 public compatibility burden이 된다.

---

## CHAPTER 02 · registry는 “어떤 이름이 어떤 implementation을 선택하는가”를 중앙화한다

`if kind == "csv"`, `elif kind == "json"`가 여러 곳에 흩어지는 대신 registry가 이름→factory mapping을 관리할 수 있다. Core는 이름으로 implementation을 찾고 factory를 통해 instance를 만든다. 새 plugin은 registry entry를 추가하는 방식으로 확장된다.

Registry key collision 정책을 정한다. 같은 이름을 두 plugin이 등록하면 first wins, last wins, error 중 어떤 semantics를 사용할지 명시한다. Silent override는 공급망과 debugging 문제를 만들 수 있다.

Global mutable registry는 import order에 따라 내용이 달라질 수 있다. Application startup에서 명시적으로 plugin list를 load하고 immutable registry snapshot을 만드는 방식이 더 재현 가능할 수 있다.

Test에서는 작은 local registry를 만들어 global plugin set과 격리한다. Registry가 singleton이면 test order와 plugin unload 문제가 생기기 쉽다.

---

## CHAPTER 03 · discovery는 설치된 후보를 찾는 단계이고 load는 code를 실행하는 단계다

Package metadata나 entry point를 사용하면 설치된 plugin 후보를 import하기 전에 이름과 version을 발견할 수 있다. Discovery와 import를 분리하면 목록 표시와 compatibility filtering을 code execution 없이 수행할 수 있다.

Plugin module을 import하면 top-level code가 실행된다. 따라서 “목록을 읽는 것”처럼 보이는 operation이 실제로 network와 file access를 일으키지 않게 discovery 단계에서 import를 최소화한다.

Lazy load는 startup을 빠르게 만들지만 실제 feature 첫 사용 시 import error가 발생할 수 있다. Critical plugin은 startup validation으로 미리 load하고 optional plugin은 lazy하게 처리하는 혼합 정책을 사용할 수 있다.

Discovery 결과에는 distribution name, plugin ID, version, requested capability를 기록해 운영 중 어떤 plugin이 선택됐는지 진단할 수 있게 한다.

---

## CHAPTER 04 · plugin compatibility는 version 숫자보다 capability negotiation이 강할 수 있다

Core version 3과 plugin version 7이라는 숫자만으로 실제 호환 operation을 알기 어렵다. Plugin이 `api_version=2`, `features={"streaming","batch"}`처럼 지원 contract를 선언하면 core가 필요한 capability를 확인해 load할 수 있다.

Major version이 다르면 무조건 거부하는 정책이 간단할 수 있지만 backward-compatible subset을 활용하지 못한다. 반대로 “일단 호출해 보고 실패하면 처리”하면 production runtime에서 늦게 incompatibility가 나타난다.

Startup negotiation에서 required method와 schema version을 확인하고 stable adapter를 만들 수 있다. Old plugin을 일정 기간 compatibility wrapper로 지원할 수 있지만 제거 일정을 관리한다.

Capability 이름도 public protocol이다. 의미를 변경하지 않고 새 capability를 추가하는 방식으로 evolution한다.

---

## CHAPTER 05 · hook ordering은 여러 extension이 같은 사건에 참여할 때 semantics가 된다

Request before/after hook, compiler pass, validation plugin처럼 여러 extension이 순서대로 실행되면 ordering이 최종 결과에 영향을 준다. Registration order에 우연히 의존하지 말고 priority와 dependency를 명시할 수 있다.

Plugin A가 Plugin B보다 먼저 실행되어야 한다는 dependency가 cycle을 만들 수 있다. Topological sort로 order를 계산하고 cycle이면 startup에서 실패시킨다. 모든 plugin에 arbitrary priority number를 주면 숫자 경쟁이 생길 수 있어 명시적 before/after relation이 더 설명력이 있을 수 있다.

Hook 중 하나가 실패하면 다음 plugin을 계속 실행할지 전체 operation을 중단할지 결정한다. Audit hook은 best-effort일 수 없고 optional metrics hook은 실패를 격리할 수 있다.

After hook은 앞의 plugin이 만든 state를 볼 수 있으므로 mutation contract를 정의한다. Immutable event를 전달하고 별도 result를 모으면 extension끼리 숨은 coupling이 줄어든다.

---

## CHAPTER 06 · plugin failure는 core process의 failure와 분리할 필요가 있다

같은 process에 import된 Python plugin은 exception뿐 아니라 memory leak, blocking loop, native extension crash로 core 전체에 영향을 줄 수 있다. 신뢰된 internal plugin에는 충분할 수 있지만 third-party code를 hostile/untrusted로 본다면 process boundary가 필요하다.

Subprocess plugin host를 사용하면 crash isolation과 resource limit을 강화할 수 있다. 대신 IPC schema, timeout, serialization 비용이 생긴다. Extension의 trust level과 latency requirement에 따라 boundary를 선택한다.

In-process plugin도 timeout을 쉽게 강제하기 어려운 sync CPU loop가 있을 수 있다. Cooperative async plugin은 cancellation contract를 지키도록 요구하고 long-running work는 worker process로 보낼 수 있다.

Plugin failure log에는 plugin identity/version을 포함해 core defect와 구분한다. 동일 plugin이 반복 crash하면 circuit breaker처럼 disable하는 정책도 가능하다.

---

## CHAPTER 07 · plugin configuration은 namespace와 validation을 분리한다

여러 plugin이 `timeout`, `path` 같은 일반 key를 사용하면 root configuration에서 충돌한다. Plugin ID 아래에 configuration namespace를 두고 각 plugin이 자신의 schema를 validation하게 할 수 있다.

Core는 unknown plugin config를 어떻게 처리할지 정한다. 설치되지 않은 plugin 설정을 error로 볼지 future deployment를 위해 보존할지 운영 workflow에 따라 다르다.

Secret을 plugin config에 전달할 때 plugin이 실제로 필요한 credential만 제공한다. Core master token을 모든 plugin에 노출하지 않고 scoped credential을 생성할 수 있다.

Dynamic config reload에서 plugin instance가 새 설정을 적용할 수 있는지, restart가 필요한지 capability로 명시한다. Partial update 중 plugin state가 inconsistent하지 않게 immutable config snapshot을 교체한다.

---

## CHAPTER 08 · extension API test는 core와 plugin 양쪽이 공유하는 contract suite로 만든다

Plugin author가 core repository 내부 test를 모두 실행할 수 없어도 public contract suite를 package로 제공하면 자신의 implementation이 required behavior를 만족하는지 확인할 수 있다. Input edge case, exception, lifecycle, concurrency requirement를 같은 test로 검증한다.

Core CI에서는 reference plugin과 known third-party compatibility matrix를 실행할 수 있다. API 변경 전에 어떤 plugin이 깨지는지 확인한다.

Contract test가 implementation detail을 강제하지 않게 한다. Plugin이 어떤 library를 쓰는지보다 observable output과 error/lifetime semantics를 본다.

Security-sensitive extension에는 malformed input과 resource budget test를 추가한다. “method가 존재한다”는 structural check보다 behavior contract가 중요하다.

---

## CHAPTER 09 · plugin uninstall·upgrade는 runtime state와 artifact state를 함께 바꾼다

Plugin package를 upgrade하는 순간 현재 process에 이미 import된 old module이 자동으로 교체되는 것은 아니다. Running instance가 어떤 version을 사용 중인지와 disk에 어떤 version이 설치됐는지 다를 수 있다. 일반적으로 process restart로 clean load를 보장하는 편이 단순하다.

Hot plugin reload는 old callback unregister, thread/task 종료, resource close, module reference 제거가 필요하고 완전한 unloading이 어려울 수 있다. Development convenience와 production reliability를 구분한다.

Plugin이 자체 persistent schema를 가진다면 upgrade 전에 migration이 필요하다. Core rollback과 plugin data migration compatibility를 함께 계획한다.

Uninstall 후 configuration와 data를 보존할지 삭제할지도 별도 policy다. Package 제거를 데이터 삭제와 자동으로 묶지 않는다.

---

## CHAPTER 10 · extensibility는 열어 두는 것이 아니라 안전한 변경 지점을 의도적으로 좁히는 것이다

모든 internal function을 override 가능하게 만들면 plugin freedom은 커지지만 core invariant를 보장할 수 없다. Extension architecture는 변화 가능성이 실제로 필요한 지점을 선택하고 그 지점에 stable contract를 제공하는 것이다.

Formatter, storage adapter, authentication provider처럼 독립 변화 축이 있는 영역은 좋은 extension 후보가 될 수 있다. Transaction 내부 step처럼 invariant가 강하게 연결된 곳을 arbitrary hook으로 열면 correctness가 깨지기 쉽다.

Plugin 수가 늘수록 compatibility matrix와 security surface도 커진다. Extension API의 최소성은 장기 maintenance 비용을 직접 줄인다.

Plugin 설계의 핵심은 **동적으로 code를 불러오는 기술이 아니라 capability·version·lifetime·failure·trust boundary를 정의해 core의 불변조건을 깨지 않고 필요한 변화만 외부 구현에 위임하는 것**이다.