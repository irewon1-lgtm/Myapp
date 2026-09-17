# PART 09 · BLOCK 01 · LESSON 09 · 오류를 실패 원인·HTTP 표현·운영 증거로 분리하기

backend에서 `error`는 하나의 종류가 아니다. client가 잘못된 JSON을 보냈을 수도 있고, 존재하지 않는 resource를 요청했을 수도 있고, 현재 상태에서 command가 허용되지 않을 수도 있으며, 외부 결제 API가 timeout됐거나 programmer bug가 발생했을 수도 있다. 모두 500 하나로 보내면 client 행동과 monitoring이 망가지고, 반대로 내부 exception을 그대로 노출하면 구현 detail과 민감정보가 새어 나간다. 이 PART는 오류를 **application 의미, transport 표현, 내부 진단 증거**의 세 층으로 나눈다.

---

## CHAPTER 01 · expected failure와 unexpected failure는 운영 의미가 다르다

사용자가 존재하지 않는 order ID를 요청하는 것은 시스템이 예상 가능한 상태다. invalid input이나 permission denied, out-of-stock도 application이 정상적으로 거부할 수 있는 결과다. 반면 null dereference, invariant가 깨진 impossible state, library bug는 개발자가 예상하지 못한 결함일 수 있다. 둘을 같은 error counter로 세면 reliability 신호가 왜곡된다.

expected failure도 무시할 사건은 아니다. 갑자기 authorization denial이 폭증하면 공격 또는 client bug일 수 있고, out-of-stock 비율 증가는 business signal이다. 그러나 pager를 울릴 server crash와 같은 severity로 취급할 필요는 없다. error taxonomy가 monitoring routing에 직접 연결된다.

실습에서는 `ValidationError`, `NotFound`, `Conflict`, `DependencyTimeout`, 일반 `Error`를 분리하고 각각 API status, log level, metric category를 표로 만든다. 이 표가 없는 상태에서 catch block부터 작성하지 않는다.

---

## CHAPTER 02 · parsing error는 application service 이전의 request contract 실패다

malformed JSON이나 unsupported media type은 business service가 처리할 데이터로 변환되지 못한 상태다. handler/service가 호출되지 않았음을 보장하고 4xx 계열로 표현할 수 있다. parser library stack을 client body에 그대로 넣지 않고 stable error type을 제공한다.

body size 초과는 syntax와도 다르다. client가 너무 큰 valid JSON을 보냈을 수 있으므로 `payload too large`와 `malformed`를 구분한다. resource protection metric에서는 size limit rejection을 별도 count하는 것이 abuse와 정상 client regression을 찾는 데 유용하다.

실습에서 malformed, unsupported content type, too large 세 request를 보내 service call count가 모두 0인지 확인하고 response error code가 서로 다른지 test한다.

---

## CHAPTER 03 · validation error는 어느 field가 contract를 만족하지 않았는지 구조화한다

runtime schema가 type, length, required, format rule을 거부하면 client가 요청을 수정해 다시 보낼 수 있다. error response에는 stable code와 field path를 제공할 수 있다. 사람용 message는 localization되더라도 code는 program logic에 사용할 수 있게 유지한다.

모든 validation detail을 노출해야 하는 것은 아니다. 로그인 credential처럼 어떤 부분이 틀렸는지 자세히 말하면 account enumeration에 도움을 줄 수 있다. 일반 business form과 security-sensitive endpoint의 disclosure 수준을 다르게 설계한다.

실습으로 `items[2].quantity` error를 만들고 nested path가 정확히 전달되는지 확인한다. UI fixture가 field를 강조할 수 있도록 contract test를 만든다.

---

## CHAPTER 04 · not found는 `없는 것`과 `볼 수 없는 것` 사이의 보안 정책을 포함한다

resource가 실제 없을 때 404를 사용하는 것은 자연스럽다. 그러나 resource가 존재하지만 caller에게 권한이 없을 때 403을 줄지 404처럼 존재 자체를 숨길지는 threat model과 UX에 따라 달라진다. 중요한 것은 repository의 `null` 하나로 모든 경우를 섞지 않고 authorization 판단 위치를 명확히 하는 것이다.

관리자 support 도구는 존재 여부를 알아야 할 수 있고 public user API는 concealment가 필요할 수 있다. 같은 domain error라도 transport adapter별 representation이 다를 수 있다. client contract와 audit log에는 실제 내부 원인을 별도로 남길 수 있다.

실습에서 `missing`, `exists-but-not-owned`, `owned` 세 state를 fake repository와 policy로 만들고 external status와 internal audit event가 어떻게 달라지는지 test한다.

---

## CHAPTER 05 · conflict는 현재 resource state와 command가 충돌하는 실패를 표현한다

이미 취소된 order를 다시 취소하거나 stale version으로 update하는 경우 request syntax는 맞지만 현재 상태와 양립하지 않는다. 이런 실패는 validation 400이나 server 500과 다르다. HTTP 409 같은 status를 사용할 수 있으며 response에는 current state나 retry strategy를 안전한 범위에서 알려 줄 수 있다.

unique constraint violation도 application 의미상 conflict로 번역될 수 있다. DB driver code를 client에 노출하지 않고 `EmailAlreadyUsed` 같은 stable error를 만든다. 어떤 conflict는 client가 latest data를 다시 읽고 재시도할 수 있고, 어떤 것은 business choice를 바꿔야 한다.

실습으로 stale version update와 duplicate email을 서로 다른 application error code로 만들되 HTTP status는 둘 다 409가 될 수 있게 한다. client가 code를 기준으로 다른 UI를 보이도록 test한다.

---

## CHAPTER 06 · dependency timeout은 `실패함`보다 `결과를 제한 시간 안에 확인하지 못함`에 가깝다

외부 payment API에 charge를 보낸 뒤 timeout이 발생하면 상대가 처리하지 않았다고 단정할 수 없다. network response만 잃었을 수 있다. 이 상태를 generic `PaymentFailed`로 바꾸고 즉시 retry하면 이중 결제 위험이 있다. `OutcomeUnknown` 또는 reconciliation-needed state를 별도로 모델링할 수 있다.

read-only dependency timeout은 제한적 retry가 더 안전할 수 있지만 전체 request deadline과 load amplification을 고려한다. error taxonomy에 retryable hint가 있더라도 최종 policy는 operation idempotency와 budget을 함께 본다.

실습에서는 fake payment가 success response, explicit decline, timeout-after-processing 세 mode를 갖게 한다. timeout을 decline과 같은 state로 저장하지 않는지 확인하고 status 조회/reconciliation flow를 설계한다.

---

## CHAPTER 07 · programmer bug는 client가 고칠 수 있는 오류처럼 포장하지 않는다

`undefined.property`나 impossible branch 도달 같은 bug를 `잘못된 요청입니다`로 400 처리하면 server 결함이 숨는다. client는 아무리 request를 바꿔도 해결하지 못하고 운영 metric에는 실패가 사라진다. unexpected error는 500 계열 generic response를 주고 internal exception tracking과 alert에 연결한다.

반대로 모든 exception을 fatal bug라고 볼 수도 없다. library가 known timeout을 exception으로 표현할 수 있다. exception class가 application taxonomy로 번역되기 전까지는 implementation detail일 뿐이다. boundary adapter에서 expected infrastructure error를 의미 있는 application failure로 매핑한다.

실습에서는 일반 Error를 던져 500 generic response, internal stack log, error metric이 모두 생성되는지 확인한다. client body에는 stack이나 filesystem path가 없는지 assertion을 둔다.

---

## CHAPTER 08 · error cause chain은 번역 뒤에도 내부 진단을 위해 보존한다

DB driver가 connection timeout을 던졌고 repository가 `RepositoryUnavailable`, service가 `CreateOrderTemporarilyUnavailable`로 번역할 수 있다. 각 layer가 새 Error만 만들고 original cause를 버리면 root cause stack과 driver code를 잃는다. language의 error cause 기능이나 structured wrapper로 내부 chain을 보존한다.

client에게 cause chain을 전부 노출할 필요는 없다. public response에는 stable code와 safe detail만 보내고 logging/tracing에는 sanitized cause class와 stack을 기록한다. 같은 error가 여러 layer에서 중복 log되어 noisy alert를 만들지 않도록 ownership도 정한다.

실습에서 nested cause를 만든 뒤 logger가 top-level type과 root cause type을 모두 기록하는지 확인한다. response serializer는 root message를 포함하지 않도록 분리한다.

---

## CHAPTER 09 · machine-readable error body는 status 외의 안정적인 분기 기준을 제공한다

같은 409에서도 `VERSION_CONFLICT`, `EMAIL_ALREADY_USED`, `ORDER_ALREADY_CANCELLED`처럼 client 행동이 다를 수 있다. response에 stable application error code 또는 problem type URI를 제공하면 client가 한국어 message 문자열을 parsing하지 않아도 된다. human-readable title/detail과 machine-readable identifier를 분리한다.

error schema도 API contract이므로 field 이름과 type을 OpenAPI에 정의하고 version evolution을 관리한다. debug-only field를 production response에 우연히 추가하지 않는다. request/correlation ID를 넣으면 support가 internal log를 찾는 데 도움된다.

실습으로 공통 error envelope를 만들고 400/404/409/500 모두 같은 top-level shape를 따르게 한다. 각 operation의 documented error code 목록과 실제 response를 contract test로 비교한다.

---

## CHAPTER 10 · HTTP status는 client retry·cache·monitoring behavior에 영향을 준다

status code는 단순 화면 문구 번호가 아니다. 4xx/5xx class를 기준으로 client library나 proxy가 retry 정책을 적용할 수 있고, monitoring이 success rate를 계산한다. 429에는 rate-limit 의미와 retry timing을 전달할 수 있고 503은 일시적인 service unavailable을 표현할 수 있다. 잘못된 status 선택은 자동화된 행동을 잘못 유도한다.

모든 business decline을 500으로 보내면 retry storm을 만들 수 있고, server bug를 200+`ok:false`로 보내면 load balancer와 metric은 정상으로 볼 수 있다. API contract와 operational semantics를 일치시킨다.

실습에서 client가 400에서는 retry하지 않고 503에서 제한적으로 retry하도록 만들고 server status를 바꿨을 때 call count가 어떻게 달라지는지 관찰한다.

---

## CHAPTER 11 · error response에는 secret·SQL·stack·내부 topology를 노출하지 않는다

DB error message에는 table/column 이름과 query fragment가 포함될 수 있고, stack에는 file path와 library version이 들어간다. authentication error에는 credential detail이 있을 수 있다. 개발 환경에서 유용한 정보를 production client response에 그대로 보내면 공격자에게 내부 구조를 제공하고 개인정보가 유출될 수 있다.

safe response와 internal diagnostic event를 별도로 만든다. logger도 token과 password를 redaction한다. error object 전체를 JSON으로 덤프하기 전에 library가 어떤 property를 포함하는지 확인한다.

실습에서 의도적으로 SQL-like error와 token을 가진 custom Error를 만들고 public serializer 결과에 금지 문자열이 없는지 snapshot test한다. internal test logger에는 필요한 cause class만 남는지 본다.

---

## CHAPTER 12 · retryable flag 하나보다 operation semantics와 deadline을 함께 본다

`DependencyUnavailable`이 transient라고 해서 모든 caller가 retry해야 하는 것은 아니다. 이미 response deadline이 100ms 남았다면 새 500ms attempt는 의미가 없고, side effect operation이 idempotent하지 않으면 duplicate 위험이 있다. retry decision은 error class, operation type, attempts, elapsed deadline을 입력으로 사용한다.

error object에 `retryAfter`나 category를 제공할 수 있지만 policy를 central resilience layer에 둘 수도 있다. 서비스마다 무한 retry loop를 직접 작성하면 multiplicative retry가 생긴다. 전체 call graph에서 retry ownership을 정한다.

실습으로 동일 timeout error를 read query와 payment charge에 넣고 retry policy 결과가 다르게 나오게 한다. remaining deadline이 부족한 case도 포함한다.

---

## CHAPTER 13 · error metric은 원인·operation·outcome을 낮은 cardinality로 분류한다

`error.message` 전체를 metric label로 사용하면 user-specific string과 stack fragment 때문에 cardinality가 폭발한다. 대신 `error_type=validation|conflict|dependency_timeout|internal`, `route=/orders/:id`처럼 bounded dimension을 사용한다. 개별 request detail은 log와 trace에 둔다.

expected validation error가 많다고 service availability SLO를 그대로 깎을지 별도 지표로 볼지도 정의한다. valid request population을 denominator로 잡는 방법이 있다. 보안 denial과 business decline도 reliability failure와 분리할 수 있다.

실습에서 오류 100개를 생성하고 message는 모두 달라도 metric series 수가 제한되는지 확인한다. request ID는 log에만 존재하는지 검사한다.

---

## CHAPTER 14 · error budget과 incident triage는 unexpected failure를 빠르게 찾게 한다

production에서 500 비율이 상승하면 먼저 route/region/version별 분포를 보고, trace에서 slow/failing dependency를 찾고, log cause chain으로 좁힌다. validation 400 증가가 동시에 있어도 원인이 같은지 별도로 검증한다. deploy 시점과 error onset을 같은 시간축에 놓으면 regression 가설을 만들 수 있다.

error handling code가 error를 삼켜 200을 반환하면 이런 관측이 불가능해진다. 반대로 동일 root failure를 API layer, service layer, repository layer에서 세 번 error log하면 incident volume이 과장된다. 한 request에서 root cause를 추적할 correlation과 logging ownership을 정한다.

연습으로 `DB pool exhausted` incident timeline을 구성하고 user 500, repository timeout, pool wait metric, deploy change를 연결한다. 어떤 증거만으로는 root cause를 확정할 수 없는지도 적는다.

---

## CHAPTER 15 · 오류 실습은 같은 실패를 status만 바꾸는 것이 아니라 후속 행동까지 검증한다

주문 API에 malformed JSON, validation failure, missing order, ownership denial, stale version conflict, payment decline, payment timeout, unexpected bug를 주입한다. 각 case에 application error type, external status/code, client retry 가능 여부, log severity, metric class를 미리 정의한다. 실행 후 실제 값이 표와 일치하는지 확인한다.

특히 payment timeout은 outcome unknown으로 남기고 자동 charge retry를 하지 않으며 reconciliation job을 등록한다. unexpected bug response에는 stack이 없어야 하고 internal trace에는 cause가 있어야 한다. invalid request에서는 service가 호출되지 않는지 spy로 확인한다.

AI는 error class boilerplate와 mapping table 초안을 생성할 수 있다. 사람은 `누가 이 실패를 고칠 수 있는가`, `재시도하면 안전한가`, `결과가 확정됐는가`, `client에게 어디까지 공개할 것인가`를 결정한다. error contract가 명확하면 실패가 발생했을 때 system은 단순히 멈추는 대신 **다음 행동을 결정할 수 있는 정보**를 제공한다.
