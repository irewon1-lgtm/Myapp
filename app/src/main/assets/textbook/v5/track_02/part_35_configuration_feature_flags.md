# PART 35 · Configuration과 feature flag — 기본값·파일·환경·실행 인자를 하나의 검증된 상태로 합치기

프로그램 behavior는 source code만으로 결정되지 않는다. Timeout, endpoint, file path, feature enablement처럼 배포 환경에 따라 달라지는 값이 configuration으로 들어온다. 설정 source가 늘어나면 어떤 값이 최종적으로 선택됐는지, 잘못된 값이 언제 발견되는지, 변경이 현재 process에 언제 반영되는지가 중요한 계약이 된다. 핵심은 **여러 문자열 source를 startup에서 하나의 typed·validated configuration으로 정규화하는 것**이다.

---

## CHAPTER 01 · configuration은 code 밖의 입력이며 schema가 필요하다

Environment variable, config file, CLI option은 모두 외부 입력이다. 개발자가 직접 작성했다는 이유로 항상 유효하다고 가정하지 않는다. Port는 1..65535 범위인지, timeout은 양수인지, endpoint scheme은 허용되는지, 필수 secret reference가 존재하는지 startup에서 검증한다.

모든 값이 문자열 source에서 시작하더라도 내부에서는 `int`, duration, enum, path 같은 domain type으로 변환한다. 함수 깊숙한 곳에서 매번 `int(os.environ["PORT"])`를 호출하면 parsing과 failure가 여러 위치에 흩어진다.

Typed configuration object를 한번 만든 뒤 immutable하게 공유하면 process가 같은 설정 snapshot을 사용한다. Dynamic reload가 필요하지 않은 application에서 runtime 중 environment를 계속 읽는 것보다 추론하기 쉽다.

Config schema에는 default뿐 아니라 값의 provenance도 중요할 수 있다. 문제를 조사할 때 `timeout=3s`가 default인지 config file override인지 알면 원인을 빠르게 찾을 수 있다.

---

## CHAPTER 02 · precedence는 여러 source가 같은 key를 제공할 때의 정책이다

Default, file, environment, CLI가 모두 `timeout`을 정의할 수 있다면 어느 source가 우선하는지 고정해야 한다. 일반적인 layering은 base default 위에 environment-specific file, environment variable, explicit CLI override를 덮는 식일 수 있지만 application 요구에 따라 다르다.

Merge를 dict update 몇 번으로 구현할 수 있어도 nested structure에서는 deep merge와 replace 의미가 달라진다. `database.pool.max=20`만 override하려 했는데 database section 전체가 교체되어 host가 사라질 수 있다. Schema-aware merge policy가 필요할 수 있다.

List 설정도 append인지 replace인지 명확하게 한다. Allowed origins, plugin path 같은 list를 layer마다 합칠 때 중복과 순서가 behavior에 영향을 줄 수 있다.

최종 configuration을 만들 때 source별 값을 보존하면 `explain_config("timeout")`처럼 precedence 결과를 진단할 수 있다. 단, secret 값은 provenance만 보여 주고 실제 content는 노출하지 않는다.

---

## CHAPTER 03 · default는 편의가 아니라 값이 생략됐을 때의 의미를 결정한다

Default timeout 30초를 넣으면 사용자가 설정을 생략했을 때도 application은 30초라는 정책을 선택한다. 이것은 단순 개발 편의가 아니라 public behavior다. Version upgrade에서 default를 5초로 바꾸면 configuration file을 수정하지 않은 사용자 behavior가 바뀐다.

안전한 default가 없는 값은 필수로 요구한다. Production database URL을 localhost로 default해 버리면 잘못된 환경에서 엉뚱한 DB에 연결할 수 있다. Secret을 `"changeme"`로 제공하는 것도 위험하다.

Boolean feature의 default는 rollout policy와 연결된다. 새 기능을 opt-in으로 둘지 기본 enable할지 결정하고 config migration에서 기존 사용자 behavior가 유지되는지 확인한다.

Default가 여러 계층에 중복되면 어느 값이 진짜인지 drift가 생긴다. Schema definition 한 곳에서 default를 소유하고 documentation과 generated config example이 이를 참조하게 하는 편이 좋다.

---

## CHAPTER 04 · environment variable은 flat 문자열 namespace이므로 명시적 변환이 필요하다

환경 변수 `"false"`는 Python 문자열로는 truthy다. `bool(os.getenv("DEBUG"))`처럼 처리하면 `"false"`도 True가 된다. 허용 값을 `true/false`, `1/0` 등으로 명시하고 parser가 그 밖의 값을 거부한다.

Duration `"500"`이 seconds인지 milliseconds인지 이름과 schema에 unit을 명시한다. CSV-like list를 comma로 split할 때 값 자체에 comma가 들어갈 수 있는지 고려한다. 복잡한 structure를 env var 하나에 넣기보다 JSON 또는 config file source가 더 적합할 수 있다.

Variable 이름은 application prefix를 사용해 충돌을 줄인다. `APP_HTTP_TIMEOUT`처럼 scope가 드러나면 shared host에서 generic `TIMEOUT`보다 관리하기 쉽다.

Environment 전체를 diagnostic에 출력하지 않는다. Cloud credential과 token이 같이 들어 있는 경우가 많다. 허용된 non-secret key만 redacted format으로 보여 준다.

---

## CHAPTER 05 · secret은 configuration과 비슷하게 읽혀도 보안 lifetime이 다르다

Database password와 API token은 endpoint URL과 같은 config object에 들어올 수 있지만 저장·출력·회전 정책은 다르다. Secret value를 dataclass `repr`에 포함하거나 exception message에 넣으면 log에 유출될 수 있다. Sensitive field는 redaction을 기본으로 한다.

Source repository에 secret을 넣고 나중에 file에서 삭제해도 Git history에 남을 수 있다. Secret manager나 deployment secret mechanism에서 runtime에 주입하고 code에는 reference/key 이름만 둔다.

Rotation 가능한 credential은 process restart 없이 reload해야 하는 요구가 있을 수 있다. 이때 immutable startup config와 secret provider를 분리해 secret만 짧은 lifetime으로 fetch하는 설계를 고려한다.

Test에서는 production-shaped fake secret을 사용하되 실제 credential을 fixture에 복사하지 않는다. CI log와 snapshot에도 secret이 포함되지 않는지 검증한다.

---

## CHAPTER 06 · feature flag는 boolean variable보다 rollout 상태 머신에 가깝다

단순 `NEW_UI=true` 외에도 사용자 비율, tenant allowlist, region, minimum version, experiment cohort에 따라 기능을 활성화할 수 있다. Flag evaluation은 context를 입력으로 받아 decision을 반환하는 policy가 된다.

Flag를 application 전역에서 직접 조회하면 한 request 안에서도 다른 시점에 값이 바뀌어 behavior가 섞일 수 있다. Request 시작 시 evaluation result를 snapshot해 use case에 전달하거나 flag 변화가 중간에 반영되어도 안전한지 contract를 정한다.

Feature flag는 오래 남으면 영구 분기와 test matrix를 늘린다. Rollout이 끝난 release flag는 제거하고 코드 path를 하나로 합친다. Operational kill switch처럼 장기 유지할 flag와 temporary experiment flag를 구분한다.

Flag off path도 계속 테스트해야 한다. 새 path만 개발하면서 old fallback이 decay하면 incident 때 flag를 꺼도 복구되지 않는다.

---

## CHAPTER 07 · configuration validation은 가능한 한 side effect 전에 완료한다

Server가 port를 bind하고 worker를 시작한 뒤 database config가 잘못됐음을 발견하면 partial startup cleanup이 필요하다. 가능한 setting은 startup 초기에 parse하고 cross-field invariant까지 확인한 뒤 external resource를 연다.

`tls_enabled=true`인데 certificate path가 없거나 `mode=production`인데 debug flag가 켜진 모순은 field별 type check만으로 잡히지 않는다. Config object-level validation이 필요하다.

모든 외부 resource 존재 여부를 startup에서 확인하는 것이 항상 좋은 것은 아니다. Optional service가 일시적으로 down됐다고 main application을 시작할 수 없는지 product requirement를 본다. Syntax/shape validation과 connectivity health check를 구분한다.

Validation error는 key path와 안전한 값 표현, 기대 범위를 포함해 operator가 수정할 수 있게 한다. Secret은 실제 값을 출력하지 않는다.

---

## CHAPTER 08 · hot reload는 설정 변경을 동시성 문제로 바꾼다

Process가 실행 중인 동안 config file을 다시 읽어 새 설정을 적용하면 여러 request가 old/new snapshot을 동시에 볼 수 있다. Mutable config object의 field를 하나씩 바꾸면 중간에 일부만 새 값인 inconsistent state가 생길 수 있다.

새 config 전체를 parse·validate한 뒤 immutable snapshot reference를 원자적으로 교체하면 reader가 old 또는 new 중 하나의 일관된 set을 보게 만들기 쉽다. Long-running operation이 시작 시점 snapshot을 계속 사용할지 매 단계 latest를 읽을지도 정한다.

변경 불가능한 setting도 있다. Listen port나 process worker count는 reload보다 restart가 안전할 수 있다. Config key마다 dynamic/restart-required 속성을 문서화한다.

Reload 실패 시 기존 valid config를 유지하고 error를 알리는 정책이 일반적이다. Invalid file을 읽었다고 running service의 현재 setting까지 지우지 않는다.

---

## CHAPTER 09 · config change는 code change처럼 version·review·rollback 대상이 될 수 있다

Production behavior를 크게 바꾸는 feature flag나 timeout 변경은 source commit 없이도 incident를 만들 수 있다. 누가 언제 어떤 값을 바꿨는지 audit trail을 남기고 이전 version으로 되돌릴 수 있어야 한다.

Infrastructure repository의 config도 pull request와 test를 통해 review할 수 있다. Schema validation을 CI에서 실행해 typo를 deployment 전에 잡는다. Environment별 diff를 machine-readable하게 만들면 production만의 예외를 찾기 쉽다.

Config rollout을 단계적으로 적용해 일부 instance에서 metric을 확인한 뒤 확대할 수 있다. Code deployment와 flag enable을 분리하면 risk를 제어할 수 있지만 두 version 조합을 compatibility matrix로 생각해야 한다.

Rollback할 때 schema가 이미 migration된 상태와 old config가 호환되는지도 확인한다. Config versioning은 data/code versioning과 연결된다.

---

## CHAPTER 10 · configuration boundary는 raw source를 typed snapshot으로 변환하는 adapter다

좋은 application core는 environment variable 이름과 YAML/JSON file path를 직접 알 필요가 없다. Startup adapter가 여러 source를 읽고 precedence를 적용하고 schema를 검증해 `AppConfig`를 만든 뒤 필요한 component에 전달한다.

Component는 전체 config object를 모두 받기보다 자신에게 필요한 작은 setting group을 받으면 coupling이 줄어든다. Email client가 database password까지 가진 config root를 받을 이유는 없다.

Feature flag와 secret은 lifetime과 security가 달라 별도 provider/interface로 분리할 수 있다. 모든 “설정”을 한 거대한 global dict에 넣는 방식보다 역할이 선명하다.

Configuration 설계의 핵심은 **값을 어디에 적을지보다 source precedence·type·validation·lifetime·provenance를 고정해 같은 배포 입력에서 같은 실행 상태를 재현할 수 있게 만드는 것**이다.