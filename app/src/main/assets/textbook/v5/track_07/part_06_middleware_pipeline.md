# PART 06 · BLOCK 01 · LESSON 06 · middleware를 순서가 있는 request pipeline으로 이해하기

route마다 request ID 생성, logging, body parsing, authentication, rate limit, error translation을 반복해서 작성하면 누락과 불일치가 생긴다. middleware는 이런 공통 처리를 request와 handler 사이의 pipeline으로 구성하는 방법이다. 그러나 middleware를 많이 넣는 것 자체가 좋은 설계는 아니다. 순서·조기 종료·비동기 오류·context ownership이 불명확하면 request가 멈추거나 두 번 진행되거나 이미 끝난 response를 다시 수정하는 버그가 생긴다. 이 PART는 middleware를 **앞 단계가 다음 단계에 계약을 제공하는 ordered control flow**로 다룬다.

---

## CHAPTER 01 · middleware는 request 전후의 공통 관심사를 한 실행 경로에 배치한다

request가 route handler에 도착하기 전에 JSON parsing, request ID, authentication 같은 단계를 실행하고, handler 이후 response duration이나 error를 기록할 수 있다. framework마다 signature와 lifecycle은 다르지만 핵심은 `현재 단계가 request를 처리하고 다음 단계로 넘길지, 여기서 response를 끝낼지` 결정한다는 것이다.

```text
network request
→ correlation
→ parser
→ authentication
→ route-specific validation
→ handler
→ error translation
→ response completion telemetry
```

이 흐름을 그림으로 먼저 그리면 middleware file 수가 많아져도 어느 단계가 어떤 data를 보장하는지 알 수 있다. 반대로 `app.use(...)` 목록만 늘어놓고 계약을 정의하지 않으면 순서를 바꿀 때 예상하지 못한 failure가 생긴다.

실습에서는 각 middleware가 자신의 이름을 배열에 push하고 마지막 handler가 배열을 response로 보내게 한다. 등록 순서를 바꿔 실제 실행 순서가 달라지는 것을 확인한다. 그 뒤 error path에서도 어떤 단계가 실행되는지 별도 기록한다.

---

## CHAPTER 02 · 순서는 앞 단계가 만든 값을 뒤 단계가 사용하는 dependency graph다

body validation middleware가 `req.body`를 사용하려면 JSON parser가 먼저 body를 만들어야 한다. authorization이 `principal`을 필요로 하면 authentication이 먼저 identity를 검증해야 한다. request ID가 모든 log에 포함되길 원하면 logger보다 앞에서 ID를 생성해야 한다. middleware ordering은 style이 아니라 data dependency를 표현한다.

잘못된 순서는 단순 undefined 오류뿐 아니라 security gap을 만들 수 있다. rate limit key가 authenticated user ID를 기준으로 해야 하는데 auth보다 먼저 실행하면 IP 기준으로만 제한될 수 있고, 반대로 expensive authentication을 abuse protection 전에 실행하면 credential verifier 자체가 공격 자원이 될 수 있다. 어떤 protection을 먼저 둘지는 cost와 identity requirement를 함께 본다.

각 middleware에 `requires`와 `provides`를 적는 연습을 한다. 예: parser는 `raw body`를 요구하고 `parsed body`를 제공, auth는 `credential`을 요구하고 `principal`을 제공한다. 이 표에서 ordering을 도출하면 단순 기억보다 안정적이다.

---

## CHAPTER 03 · next는 제어권을 넘기는 호출이며 정확히 한 번의 흐름을 만들어야 한다

Express류 middleware에서 `next()`는 다음 middleware 또는 route로 실행을 진행한다. 호출하지 않고 response도 끝내지 않으면 client가 계속 기다리는 hang가 생긴다. 두 번 호출하거나 비동기 callback 여러 곳에서 호출하면 downstream handler가 중복 실행되는 위험이 있다. `next()`는 단순 문법 장식이 아니라 control-flow ownership을 넘기는 사건이다.

```ts
function middleware(req, res, next) {
  if (condition) {
    next();
    return;
  }
  res.status(403).end();
}
```

branch마다 `다음 단계로 넘김`과 `response 완료` 중 하나가 명확하도록 구성한다. Promise-based middleware framework는 explicit next 대신 return/throw 규칙을 사용할 수 있으므로 해당 framework contract를 따른다.

실습에서 next를 빼 hang를 재현하고, 한 request에 1초 timeout을 둔 client가 어떻게 실패하는지 본다. 다음에는 next를 두 번 호출하는 잘못된 middleware를 만들고 downstream call count를 검사한다. control flow를 실제 counter로 검증한다.

---

## CHAPTER 04 · early response는 pipeline을 중단하지만 함수 실행까지 자동 중단하지 않을 수 있다

인증 실패나 rate limit 초과에서 middleware가 response를 보냈다면 뒤 handler를 실행하면 안 된다. 그런데 response API를 호출한 뒤 code가 계속 실행되어 `next()`까지 도달하면 이미 끝난 response 뒤로 business logic이 실행될 수 있다. 그래서 `return res.status(...).json(...)`처럼 control-flow 종료를 명시하는 패턴이 사용된다.

response를 보냈다는 사실과 side effect가 없다는 사실은 다르다. validation failure 뒤에 audit write를 일부러 수행할 수도 있지만 그런 post-processing은 의도적 구조로 분리해야 한다. accidental fallthrough와 deliberate cleanup을 혼동하지 않는다.

실습에서는 auth failure에서 401을 보낸 뒤에도 fake `charge()`가 호출되는 버그를 일부러 만든다. `return`과 branch 구조를 수정해 call count가 0이 되는지 검증한다. status만 401이면 안전하다고 판단하지 않고 downstream side effect까지 확인한다.

---

## CHAPTER 05 · synchronous throw와 rejected Promise가 error pipeline에 들어오는 방식은 framework마다 확인한다

동기 middleware에서 `throw new Error()`가 중앙 error handler로 전달되는지, async function의 rejected Promise를 framework가 자동 catch하는지는 버전과 framework에 따라 다르다. Express의 세대 차이처럼 behavior가 바뀔 수 있으므로 `async면 알아서 처리`라는 일반화 대신 공식 문서를 확인한다.

catch해서 `next(error)`를 호출하는 wrapper를 사용할 수도 있고 framework-native async handler를 사용할 수도 있다. 중요한 것은 rejection이 unhandled 상태로 process에 남지 않고 request-specific error response와 telemetry로 연결되는 것이다. fire-and-forget Promise는 request pipeline 밖으로 벗어나 별도 ownership이 필요하다.

실습에서 동기 throw, `await` 중 reject, await하지 않은 Promise reject 세 case를 만든다. 중앙 error handler가 어떤 case를 받는지 실제 framework version에서 확인하고, 받지 못한 case는 명시적 wrapper 또는 background job 구조로 수정한다.

---

## CHAPTER 06 · authentication middleware는 credential을 검증된 principal로 변환한다

credential 문자열을 route마다 직접 decode하면 검증 옵션과 error response가 흩어진다. authentication middleware는 credential 위치를 읽고 verifier를 호출한 뒤, 성공하면 `Principal`이라는 좁은 context를 다음 단계에 제공한다. principal에는 user ID, tenant, 인증 강도 같은 server-validated 정보가 들어갈 수 있다.

이 middleware가 모든 authorization rule까지 판단할 필요는 없다. 특정 order를 취소할 수 있는지는 resource state와 business permission을 알아야 하므로 route/service policy에서 검사할 수 있다. authentication은 `누구인가`를 확립하고 authorization은 `이 행동을 해도 되는가`를 판단한다.

실습에서는 credential 없음, malformed, expired, valid 네 결과를 fake verifier로 만든다. valid case에서만 principal이 context에 존재하고, 다른 세 case에서는 handler call count가 0인지 확인한다. error message에 token 원문이 포함되지 않게 한다.

---

## CHAPTER 07 · validation middleware는 raw input을 validated application input으로 바꾸는 경계를 만든다

route schema를 middleware로 적용하면 handler는 반복적인 type/range 검사보다 use case 호출에 집중할 수 있다. parser와 마찬가지로 schema가 성공한 값을 별도 property에 두면 raw input과 trusted value를 구분하기 쉽다. 예를 들어 `req.validated.body` 또는 framework가 제공하는 typed result를 사용한다.

validation을 global middleware 하나로 모든 route에 적용하기는 어렵다. 각 operation마다 body/query/params schema가 다르므로 route metadata에 schema를 연결하거나 route-specific middleware를 둔다. unknown field 정책과 coercion setting도 operation contract에 맞춘다.

실습에서는 `/orders` route에 body schema와 query schema를 각각 붙이고, handler에서 raw `req.body` 접근을 금지하는 coding rule을 만든다. wrong type request가 service에 전달되지 않는지 spy로 검증한다.

---

## CHAPTER 08 · request context middleware는 비동기 호출 전체에 correlation 정보를 연결한다

request ID, trace context, authenticated actor 같은 정보는 service와 repository log에서도 필요할 수 있다. 모든 함수 signature에 context를 넘기는 방식은 명시적이지만 plumbing이 많고, Node.js의 AsyncLocalStorage 같은 async context mechanism을 사용할 수도 있다. 어느 방식을 쓰든 message queue나 worker thread처럼 실행 경계를 넘을 때 context가 자동 보존되는지 확인해야 한다.

context store에는 작은 immutable metadata만 두는 편이 안전하다. request body 전체나 mutable service object를 넣으면 lifetime과 memory retention이 복잡해진다. user email 같은 개인정보 대신 internal opaque ID를 사용하는 것도 telemetry privacy에 유리하다.

실습에서 request ID를 AsyncLocalStorage에 넣고 nested async function과 setTimeout callback log에 같은 ID가 보이는지 확인한다. 그다음 background task를 request 종료 후 실행해 context lifetime을 관찰하고, durable job에는 필요한 correlation 값을 명시적으로 payload에 넣는 방식과 비교한다.

---

## CHAPTER 09 · timing middleware는 response 완료 event를 기준으로 실제 duration을 측정한다

handler 호출 직후 `Date.now()-start`를 계산하면 async DB 작업이 끝나기 전에 duration을 기록할 수 있다. response의 `finish` 또는 framework lifecycle hook에서 final status와 종료 시각을 기록해야 user-visible request duration에 가까운 값을 얻는다. client abort는 finish와 다른 event로 나타날 수 있어 별도 outcome으로 구분한다.

wall clock은 시스템 시간 조정의 영향을 받을 수 있어 정밀 elapsed time에는 monotonic clock API를 사용하는 것이 낫다. Node에서 high-resolution monotonic timer를 활용할 수 있다. duration metric은 route template 기준으로 aggregation하고 raw URL을 label로 넣지 않는다.

실습에서는 handler가 200ms await한 뒤 response를 보내게 하고 `handler enter`, `next returned`, `finish` 세 시각을 비교한다. middleware의 before/after code가 framework에서 어떤 실행 순서를 갖는지 직접 확인한다.

---

## CHAPTER 10 · timeout middleware는 request deadline을 만들지만 하위 작업 취소와 별개다

middleware가 2초 후 504를 보내는 것만으로 DB query와 external HTTP call이 자동 중지되지는 않는다. response는 끝났는데 expensive work가 계속되면 overload 상황에서 hidden work가 쌓인다. deadline을 AbortSignal이나 dependency-specific timeout으로 전달하고, side effect operation은 결과 미확정 상태를 별도로 다뤄야 한다.

timeout 위치도 중요하다. parser까지 포함한 전체 request budget인지, handler business processing만 2초인지에 따라 측정이 달라진다. reverse proxy timeout이 3초인데 application timeout이 10초면 client는 이미 사라진 뒤 server가 7초 더 일할 수 있다. outer/inner deadline hierarchy를 정한다.

실습에서 3초 fake dependency와 1초 response timeout을 만들고 dependency 함수가 실제로 몇 초 실행되는지 log한다. cancellation을 전달한 버전과 비교해 resource 사용 차이를 확인한다.

---

## CHAPTER 11 · rate-limit middleware는 key와 비용 모델에 따라 배치 위치가 달라진다

IP 기반 coarse limit은 authentication 전에 값싼 abuse를 줄이는 데 유용할 수 있다. user/tenant quota는 principal이 필요하므로 authentication 뒤에 적용한다. password verification 자체가 CPU 비용이 큰 login endpoint에서는 auth 전에 별도 IP/device limit을 두고 계정 기준 limit을 추가할 수 있다. 하나의 global middleware로 모든 abuse pattern을 해결하지 않는다.

rate limiter가 distributed store를 사용하면 그 store의 latency와 장애가 모든 request에 영향을 줄 수 있다. limiter 장애 시 fail-open 또는 fail-closed 중 무엇을 할지 endpoint 위험도에 따라 선택한다. 결제/관리 기능과 public content 조회의 정책이 같을 필요는 없다.

실습에서는 IP coarse limit과 authenticated user limit 두 단계를 만들고 각 단계가 어떤 key를 사용하는지 log한다. limiter store timeout을 주입해 chosen failure policy가 실제로 적용되는지 test한다.

---

## CHAPTER 12 · CORS middleware는 browser response access 정책이며 authorization을 대신하지 않는다

CORS header를 설정하면 browser가 cross-origin script에 response를 노출할지 결정하는 데 사용한다. 허용 origin, method, header, credential 설정이 필요할 수 있고 preflight OPTIONS request가 handler 전에 처리된다. native app이나 server-to-server client는 browser CORS enforcement를 따르지 않는다.

따라서 `CORS에서 origin을 막았으니 endpoint가 보호된다`고 생각하면 안 된다. attacker는 browser 외 client로 직접 API를 호출할 수 있으므로 authentication과 authorization은 별도로 필요하다. credentialed CORS에서는 wildcard origin과 cookie policy를 특히 주의한다.

실습에서는 preflight request와 실제 GET을 따로 보내 어떤 middleware가 실행되는지 본다. CORS 허용 origin이 아니어도 curl은 response를 받을 수 있다는 사실을 확인해 browser policy와 server permission을 분리한다.

---

## CHAPTER 13 · error middleware는 known failure와 unexpected failure를 한 HTTP contract로 번역한다

handler와 service가 여러 error type을 던질 때 각 route가 제각각 status/body를 만들면 API가 일관되지 않는다. 중앙 error middleware는 ValidationError, NotFound, Conflict 같은 known application failure를 정해진 response schema로 변환하고, 예상하지 못한 error는 generic 500과 internal logging으로 처리한다.

error middleware 자체가 실패하지 않도록 serialization 가능한 최소 field를 사용하고, 이미 response가 시작된 streaming case는 별도 처리한다. stack trace, SQL, credential을 client에게 보내지 않는다. request ID를 response problem detail에 포함할 수 있지만 공격자가 내부 log를 직접 조회할 수 있는 정보는 피한다.

실습에서는 세 종류 application error와 일반 Error를 throw하고 status/type이 mapping table과 맞는지 contract test한다. mapper에 없는 error가 accidental 200으로 처리되지 않는지 확인한다.

---

## CHAPTER 14 · business rule을 middleware chain에 과도하게 넣으면 흐름이 숨겨진다

`ensureOrderPaid`, `applyCoupon`, `reserveInventory`, `chargePayment`를 모두 middleware로 연결하면 route definition만 봐서는 use case가 여러 side effect를 가진 pipeline이 된다. middleware는 cross-cutting transport concern에 적합하지만 domain transaction과 compensation은 application service에서 명시적으로 orchestration하는 편이 이해하기 쉽다.

공통이라는 이유만으로 모든 함수를 middleware로 만들 필요는 없다. 여러 use case가 사용하는 pure validation rule은 domain 함수로 재사용할 수 있고, authorization policy는 명시적 service call이나 route policy object로 둘 수 있다. 선택 기준은 `HTTP pipeline에 결합되어야 하는 책임인가`다.

연습으로 현재 middleware 목록을 transport concern, security edge, domain rule, infrastructure side effect 네 범주로 분류한다. domain/infrastructure side effect가 지나치게 섞여 있다면 service로 이동시켜 request pipeline을 얇게 만든다.

---

## CHAPTER 15 · middleware 실습은 순서·중단·오류·context 네 failure를 따로 재현한다

`POST /orders` 앞에 request ID, body parser, coarse rate limit, authentication, user quota, validation, timing middleware를 배치하고 마지막에 handler를 둔다. 각 단계는 자신의 `requires/provides`를 문서화한다. 중앙 error middleware는 validation과 unexpected error를 다른 contract로 변환한다.

검증은 네 종류로 나눈다. 첫째 parser와 validation 순서를 뒤집어 failure를 확인한다. 둘째 auth failure 후 next를 잘못 호출해 handler가 실행되는 버그를 재현하고 수정한다. 셋째 async rejection이 error handler에 전달되는지 확인한다. 넷째 concurrent request 20개에서 request ID context가 섞이지 않는지 로그로 검증한다. 같은 정상 요청을 여러 번 보내는 것을 별도 PASS로 세지 않는다.

AI는 framework별 middleware syntax와 error handler signature를 찾아 주는 데 사용할 수 있다. 사람은 `어떤 단계가 어떤 값을 신뢰할 수 있게 만드는지`, `response를 끝낸 뒤 누가 제어권을 소유하는지`, `deadline과 cancellation이 실제 하위 작업에 전달되는지`를 확인한다. 이 세 질문이 답해져야 middleware 수가 늘어도 request 흐름을 추적할 수 있다.
