# PART 13 · BLOCK 01 · LESSON 13 · config와 secret을 실행 계약으로 관리하기

같은 server code라도 port, database endpoint, feature switch, external API credential이 달라지면 전혀 다른 behavior를 가진다. 이 값을 source code 곳곳에 박아 두면 환경을 바꿀 때 code 자체를 수정해야 하고, secret을 repository에 넣으면 유출 범위가 커진다. 반대로 `환경변수로 뺐으니 끝`이라고 생각해도 부족하다. type·required 여부·precedence·rotation·logging·재현성을 함께 관리해야 한다. 이 PART는 config를 **process가 어떤 조건으로 실행되는지 정의하는 입력 계약**으로, secret을 그중 노출 시 권한을 주는 민감한 값으로 다룬다.

---

## CHAPTER 01 · config는 build artifact와 runtime environment를 분리한다

server binary 또는 JavaScript bundle을 한 번 만들고 development, staging, production에서 서로 다른 database host와 port를 사용할 수 있다. runtime config를 외부에서 주입하면 code artifact를 환경마다 다시 수정하지 않아도 된다. 이 구조는 `어떤 code version`과 `어떤 runtime setting`을 별도로 추적하게 만든다.

모든 상수를 config로 만들 필요는 없다. 알고리즘 내부 상수나 protocol invariant까지 environment variable로 노출하면 운영자가 잘못 바꿀 수 있는 surface가 늘어난다. 환경에 따라 달라져야 하고 운영 중 조정할 가치가 있는 값만 config로 분리한다.

실습에서는 PORT, DB_URL, LOG_LEVEL만 config object로 만들고 pagination hard max 같은 invariant는 code constant로 남긴다. 각 값이 왜 runtime-configurable인지 설명한다.

---

## CHAPTER 02 · startup에서 raw string을 typed config로 변환한다

environment variable은 기본적으로 문자열이다. `PORT="3000"`, `FEATURE_X="false"` 같은 값을 application type으로 변환해야 한다. `Boolean("false")`가 true가 되는 JavaScript coercion처럼 일반 변환 함수만 쓰면 의도와 다른 값이 생길 수 있다.

startup에서 한 번 parse/validate해 `AppConfig` object를 만들고 code 곳곳에서는 `process.env`를 직접 읽지 않는 방식이 추적하기 쉽다. required field가 없거나 invalid하면 request를 받기 전에 fail-fast할 수 있다.

실습에서 strict boolean parser와 integer range parser를 만들고 `false`, `0`, empty, invalid 문자열을 test한다. config object가 생성된 뒤 raw env 접근을 lint/code review rule로 금지한다.

---

## CHAPTER 03 · missing config와 잘못된 config는 startup failure로 빠르게 드러내는 편이 안전할 수 있다

DB_URL이 없는데 server가 200으로 readiness를 올린 뒤 첫 요청에서야 crash하면 traffic이 이미 들어온다. 필수 config와 external client initialization을 startup에서 검증하고 준비가 끝난 뒤 readiness를 true로 올리면 failure boundary가 선명하다.

모든 external dependency에 실제 network request를 startup에서 보내야 하는 것은 아니다. dependency outage 때문에 모든 instance가 동시에 boot 실패하는 위험도 있다. syntax/credential presence는 local 검증하고 connection은 readiness 또는 lazy init으로 다루는 등 dependency 성격에 따라 결정한다.

실습에서는 invalid PORT, missing DB_URL, unsupported LOG_LEVEL에서 process가 startup error code와 함께 종료되게 한다. optional config는 명시적 default를 갖는다.

---

## CHAPTER 04 · config precedence는 여러 출처가 같은 key를 제공할 때 누가 이기는지 정한다

command-line flag, environment variable, config file, secret manager, default가 동시에 같은 설정을 제공할 수 있다. precedence가 문서화되지 않으면 개발 PC와 production에서 다른 값이 선택되는 문제를 찾기 어렵다. `default < file < env < explicit CLI`처럼 한 규칙을 고정하고 loader가 적용한 최종 source를 기록할 수 있다.

단 secret value 자체를 log하지 않고 `PAYMENT_KEY source=secret-manager version=7`처럼 metadata만 남긴다. precedence를 너무 복잡하게 만들면 어떤 값이 적용됐는지 추적이 어려워진다. 필요한 source 수를 줄이는 것도 좋은 설계다.

실습에서 세 source가 PORT를 동시에 제공하게 하고 expected winner를 test한다. config dump는 secret field를 `***`로 표시한다.

---

## CHAPTER 05 · secret은 possession 자체가 권한이 될 수 있는 config다

DB password, API token, private key, webhook signing secret은 노출되면 시스템 접근이나 위조에 사용될 수 있다. 일반 config와 같은 string type이어도 위험도가 다르다. repository, client bundle, error response, telemetry에 남지 않게 취급한다.

environment variable은 전달 수단일 뿐 secret vault가 아니다. process inspection, crash dump, CI log, debug endpoint에서 노출될 수 있다. production에서는 platform secret store 또는 external secret manager를 사용하고 application에는 필요한 시점에 최소 권한으로 전달한다.

실습에서 config logger가 일반 field만 출력하고 secret field는 redaction하는지 test한다. thrown error message에도 secret 원문이 포함되지 않게 한다.

---

## CHAPTER 06 · source control에 들어간 secret은 파일 삭제만으로 끝나지 않는다

secret을 commit한 뒤 다음 commit에서 지워도 Git history와 clone, cache, artifact에 남아 있을 수 있다. 노출된 credential은 `삭제했으니 안전`이 아니라 compromised로 가정하고 provider에서 revoke/rotate해야 한다. history rewrite는 복제본 전체를 회수하지 못하므로 credential lifecycle 조치가 핵심이다.

pre-commit secret scanner와 server-side scanning은 실수를 줄일 수 있지만 false negative가 존재한다. secret을 애초에 repository에 넣지 않는 workflow와 short-lived credential을 선호한다.

실습에서는 fake token pattern을 scanner fixture로 넣고 detection test를 만든다. 실제 secret은 사용하지 않는다. incident checklist에는 rotate, access log review, history cleanup을 별도 항목으로 둔다.

---

## CHAPTER 07 · secret rotation은 old/new credential이 겹치는 전환 기간을 설계한다

signing key나 API key를 바꿀 때 모든 instance가 정확히 같은 millisecond에 새 값을 쓰기 어렵다. verifier는 일정 기간 old/new key를 모두 받아들이고 signer는 new key만 사용하는 방식으로 단계적 전환할 수 있다. key ID/version을 token 또는 signature metadata에 포함하면 어느 key를 사용할지 선택하기 쉽다.

외부 provider API key는 새 key 발급 → application rollout → old key usage가 0인지 관찰 → old key revoke 순서로 진행할 수 있다. 자동 rotation이 있다면 application client가 refresh를 지원하는지 확인한다.

실습에서 key version 1과 2 fake를 만들고 verification overlap을 test한다. old key revoke 후 old signature가 거부되는지 확인한다.

---

## CHAPTER 08 · dynamic config는 consistency와 rollout 문제를 새로 만든다

process restart 없이 feature threshold를 바꿀 수 있는 dynamic config는 빠르지만 instance마다 update 시점이 달라질 수 있다. request A는 old value, B는 new value를 사용하면 business behavior가 흔들릴 수 있다. config version을 context/log에 남기고, atomic snapshot을 사용해 한 request 안에서는 같은 version을 유지할 수 있다.

critical financial rule을 remote flag 하나로 즉시 바꾸는 것은 audit와 rollback 요구를 검토해야 한다. code review가 필요한 invariant와 운영 tuning parameter를 구분한다.

실습에서 config version을 1→2로 업데이트하는 동안 concurrent request가 어떤 version을 사용하는지 기록하고 request 중간에 값이 바뀌지 않게 snapshot한다.

---

## CHAPTER 09 · feature flag는 배포와 기능 노출을 분리하지만 영구 조건문이 되기 쉽다

새 code를 먼저 배포하고 특정 tenant 1%에만 feature를 켤 수 있다. 문제가 있으면 flag를 끄며 rollback할 수 있다. 하지만 flag가 오래 남으면 old/new path 둘 다 유지해야 해 test matrix가 커진다. flag에는 owner와 제거 날짜를 두고 rollout이 끝나면 cleanup한다.

permission이나 entitlement를 feature flag로 대체하지 않는다. client가 flag 이름을 안다고 관리자 기능을 쓸 수 있어서는 안 된다. flag는 behavior rollout 도구이고 authorization은 별도 정책이다.

실습에서 deterministic user bucketing으로 10% rollout을 만들고 같은 user가 request마다 그룹이 바뀌지 않는지 test한다. flag off/on 두 path에 contract test를 유지한다.

---

## CHAPTER 10 · environment 이름보다 capability와 endpoint를 직접 config로 표현한다

`if (ENV === "production") useRealPayment()`처럼 environment 이름에 behavior를 많이 묶으면 staging-like 환경이나 region 추가 때 조건이 복잡해진다. `PAYMENT_BASE_URL`, `PAYMENT_MODE`, `FEATURE_X`처럼 필요한 capability를 직접 설정하면 조합을 더 명확히 표현할 수 있다.

production/staging label은 telemetry와 safety guard에 유용하지만 모든 결정을 하나의 string에 의존시키지 않는다. destructive test endpoint는 build/runtime guard와 authorization을 함께 사용한다.

실습에서는 `ENV` 하나로 DB/payment/log를 결정하던 code를 각각 typed config field로 분리하고 test matrix를 단순화한다.

---

## CHAPTER 11 · config 변경도 관측 가능한 deployment event로 기록한다

code deploy 없이 rate limit threshold나 timeout이 바뀌면 latency/error 그래프가 변할 수 있다. incident 분석에서 code SHA만 보면 원인을 놓친다. config version, feature flag revision, secret key ID 같은 non-sensitive metadata를 deployment timeline에 남긴다.

누가 언제 어떤 값 범주를 바꿨는지 audit가 필요한 system도 있다. 모든 config value를 log에 출력하지 않고 change ID와 diff category를 기록한다. rollback도 version 단위로 할 수 있게 한다.

실습에서 timeout config를 500→100ms로 바꾸고 error spike simulation과 같은 timeline에 version event를 기록한다. root-cause hypothesis가 더 빨리 좁혀지는지 본다.

---

## CHAPTER 12 · secret과 config를 client response·health endpoint에 노출하지 않는다

`/debug/config`가 전체 process.env를 반환하거나 health endpoint가 DB URL에 password를 포함하면 internal endpoint라고 해도 위험하다. production debug route는 기본 비활성, 강한 authorization, redaction을 적용한다. stack trace에서 connection string이 노출되는지도 확인한다.

config metadata를 support에 보여 줘야 한다면 allowlist된 version, region, feature set 정도만 제공한다. client가 알아야 할 public config와 server-only secret을 separate type으로 관리한다.

실습에서 debug JSON serializer에 fake secret field를 넣고 golden test가 노출을 막는지 확인한다.

---

## CHAPTER 13 · 재현 가능한 장애 분석에는 code SHA와 config snapshot metadata가 함께 필요하다

동일 commit으로 실행해도 timeout, feature flag, dependency endpoint가 다르면 bug가 재현되지 않을 수 있다. incident event에 application version, config revision, schema version을 기록하면 당시 실행 조건을 복원하기 쉽다. secret value는 저장하지 않고 secret version만 기록한다.

테스트에서도 production config를 그대로 복사하지 않고 typed config fixture를 사용한다. required field가 새로 추가되면 fixture compile/test가 실패해 누락을 빨리 찾을 수 있다.

실습으로 `RuntimeIdentity { codeVersion, configVersion, region }`을 모든 startup log와 trace resource attribute에 넣는다. 두 instance가 다른 config version을 쓰면 dashboard에서 식별 가능하게 한다.

---

## CHAPTER 14 · config failure는 startup·runtime·rotation 세 종류로 주입해 검증한다

startup에는 missing/invalid value, runtime에는 remote config fetch failure와 stale cache, rotation에는 old credential revoke를 주입한다. 각 failure에서 service가 fail-fast, last-known-good, fail-open/closed 중 어떤 정책을 사용하는지 endpoint 위험도에 맞춰 결정한다.

예를 들어 optional recommendation threshold는 remote config outage에서 last-known-good를 쓸 수 있지만 payment signing key를 얻지 못하면 transaction을 막아야 할 수 있다. generic fallback 하나로 모두 처리하지 않는다.

실습에서는 config provider timeout, malformed update, secret rotation mismatch를 fake로 만들고 expected readiness/status를 test한다.

---

## CHAPTER 15 · config 실습은 `실행 가능한 하나의 snapshot`을 만드는 것으로 마무리한다

server startup에서 raw environment와 secret provider를 읽어 typed `AppConfig`를 만든다. required/optional/default/secret field를 구분하고, validation 실패 시 port를 열기 전에 종료한다. startup log에는 codeVersion, configVersion, region, enabled feature 이름만 남기고 secret value는 redaction한다.

검증에는 invalid integer, misleading boolean string, missing secret, precedence 충돌, feature flag 변경, secret rotation, debug endpoint leak을 포함한다. readiness가 config initialization 전에는 false이고 성공 후 true가 되는지도 확인한다.

AI는 config schema와 deployment environment 변수 목록을 생성할 수 있다. 사람은 무엇을 runtime 변경 가능하게 열어 둘지, 어떤 값은 secret인지, 변경이 incident에 어떤 영향을 줄지 결정한다. config를 실행 계약으로 관리하면 `같은 코드인데 왜 여기서만 다르지`라는 문제를 증거로 좁힐 수 있다.
