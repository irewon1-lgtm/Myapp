# PART 12 · BLOCK 01 · LESSON 12 · request context와 correlation을 동시 요청 속에서 보존하기

한 server process에는 많은 request가 동시에 겹친다. `load user`, `save order`, `payment timeout` 같은 log만 남기면 어느 줄이 같은 요청인지 알기 어렵다. request ID와 trace context, authenticated principal, deadline처럼 한 요청을 따라가야 하는 metadata를 context로 관리할 수 있다. 그러나 global variable에 넣으면 동시 요청이 섞이고, context에 개인정보와 mutable object를 과도하게 넣으면 lifetime과 보안 문제가 생긴다. 이 PART는 context를 **관측과 정책을 연결하는 작은 immutable metadata 묶음**으로 설계한다.

---

## CHAPTER 01 · request ID는 한 요청의 log event를 묶는 correlation key다

두 요청이 동시에 실행되면 log가 interleave된다. request 시작 시 unique ID를 만들고 모든 log에 넣으면 같은 요청의 event를 검색할 수 있다. client가 support ticket에 response header의 request ID를 제공하면 운영자는 해당 request timeline을 빠르게 찾을 수 있다.

ID는 business user identifier와 다르다. random/opaque 값을 사용해 개인정보를 피하고 길이와 format을 제한한다. client가 보낸 `X-Request-Id`를 그대로 내부 primary correlation key로 사용할 경우 너무 긴 값, control character, 중복을 허용하지 않도록 검증한다. 외부 ID와 내부 ID를 둘 다 기록할 수도 있다.

실습에서 concurrent 50 request를 보내 각 response에 unique ID가 있고 해당 ID의 start/finish log가 정확히 한 쌍인지 확인한다.

---

## CHAPTER 02 · global variable은 현재 request 정보를 저장하는 장소가 될 수 없다

```ts
let currentRequestId: string;
```

request 시작마다 이 값을 바꾸면 A가 DB를 기다리는 사이 B가 ID를 덮어쓸 수 있다. A가 돌아와 log를 남길 때 B ID를 사용하게 된다. single-threaded event loop에서도 await 사이 interleaving이 있기 때문에 발생한다.

request-scoped value는 함수 인자로 전달하거나 async context mechanism을 사용한다. framework dependency injection scope를 쓸 수도 있다. 중요한 것은 process-global mutable state와 request-local state를 분리하는 것이다.

실습에서 global variable 버전을 concurrent delay로 깨뜨리고 wrong ID log 수를 센다. 이후 explicit context 버전으로 수정해 0이 되는지 확인한다.

---

## CHAPTER 03 · explicit context parameter는 흐름이 보이는 가장 단순한 방식이다

```ts
service.createOrder(ctx, input)
repository.save(ctx, order)
```

함수 signature에 context가 보이므로 dependency가 명확하고 test에서 fixed context를 넣기 쉽다. 단점은 많은 function이 context를 전달만 하는 plumbing을 갖는다는 것이다. 그래도 중요한 deadline이나 actor를 숨기지 않는 장점이 있다.

context object는 request ID, trace handle, deadline, principal처럼 cross-cutting metadata를 담고 business input과 구분한다. context가 너무 커져 모든 service가 수십 field에 접근하면 global object처럼 변하므로 field usage를 제한한다.

실습에서 requestId와 deadline만 가진 immutable context를 service/repository에 전달하고 compiler가 context 없이 호출하지 못하게 한다.

---

## CHAPTER 04 · AsyncLocalStorage는 async execution chain에 context를 연결한다

Node.js의 AsyncLocalStorage는 callback/Promise chain을 따라 현재 store를 조회할 수 있게 해 logger가 매번 request ID parameter를 받지 않아도 된다. request 시작에서 `run(store, handler)` 형태로 scope를 만들고 내부 log가 store를 읽는다.

편리하지만 hidden dependency가 된다. 함수 signature만 보면 context 사용 여부가 보이지 않고, 특정 async library나 execution boundary에서 propagation을 확인해야 한다. context가 없을 때 logger가 어떤 fallback을 하는지 정한다.

실습에서 nested Promise, timer, DB fake callback에서 같은 ID가 유지되는지 test한다. concurrent requests가 서로 다른 store를 갖는지도 검증한다.

---

## CHAPTER 05 · context store는 mutable shared object보다 immutable metadata로 유지한다

store에 `{ user, cart, dbTransaction, hugeBody }`를 넣고 여러 layer가 수정하게 하면 request-global mutable state가 되어 dependency가 숨는다. request가 오래 유지되면 큰 object도 memory에 붙잡힌다. context는 작은 identifier와 read-only metadata 중심으로 둔다.

principal도 전체 ORM User entity보다 userId, tenantId, permissions snapshot처럼 필요한 claim만 저장한다. 권한이 처리 중 바뀔 수 있는 system에서는 snapshot validity와 revalidation rule을 별도로 정의한다.

실습에서 store object를 `Object.freeze` 또는 readonly type으로 만들고 mutation attempt를 compiler/test가 잡는지 확인한다. large body를 넣은 버전과 memory retention 차이를 profiling한다.

---

## CHAPTER 06 · trace context는 request ID보다 넓게 여러 service의 작업을 연결한다

한 client request가 API server에서 payment service, inventory service를 호출하면 각 service의 local request ID만으로 end-to-end 흐름을 묶기 어렵다. distributed tracing은 trace ID와 span parent 관계를 전파해 여러 process의 operation을 하나의 trace로 구성한다.

trace ID를 request ID와 같게 쓸 수도 있지만 개념은 다르다. 한 trace에 여러 server/client span이 있고 message queue를 거쳐 시간이 길게 이어질 수 있다. local support ID와 standardized trace context를 별도로 유지할 수 있다.

실습에서는 service A가 B를 호출할 때 traceparent 같은 context header를 전달하는 instrumentation을 관찰하고 B span이 같은 trace에 속하는지 확인한다.

---

## CHAPTER 07 · propagation header는 internet client가 만든 값과 trusted instrumentation을 구분한다

trace context header는 외부 client도 조작할 수 있다. 길이·format을 검증하고 sampling flag나 baggage를 무제한 신뢰하지 않는다. 일부 system은 external trace를 이어받되 internal correlation ID는 새로 만든다. trust boundary에 따라 trace를 새로 시작할 수도 있다.

malformed header 때문에 request 전체가 실패해야 하는지, 새 trace로 fallback할지 observability policy를 정한다. 공격자가 매우 큰 baggage를 보내 storage와 log를 키우지 못하도록 limit을 둔다.

실습에서 invalid trace header와 oversized baggage를 보내 instrumentation이 안전하게 처리하는지 test한다.

---

## CHAPTER 08 · baggage는 business data를 아무렇게나 실어 나르는 통로가 아니다

distributed context의 baggage mechanism은 key-value를 downstream에 전파할 수 있어 편리하지만 모든 service와 telemetry exporter를 지나며 비용과 privacy 위험이 커질 수 있다. user email, token, 전체 permission list를 넣지 않는다. low-cardinality 또는 필요한 routing metadata만 엄격히 선택한다.

tenant ID를 baggage로 전파하더라도 authorization의 유일한 근거로 삼지 않는다. downstream service는 authenticated service identity와 trusted token/claim을 별도로 검증할 수 있다. observability propagation과 security credential을 같은 것으로 취급하지 않는다.

실습으로 safe correlation field와 unsafe personal field 목록을 만들고 baggage allowlist middleware를 구현한다.

---

## CHAPTER 09 · authenticated principal은 context에 들어가도 object-level authorization을 대신하지 않는다

authentication middleware가 userId=7을 context에 넣었다고 `GET /orders/10`을 자동 허용할 수 없다. service가 order owner 또는 permission policy를 확인해야 한다. context는 identity evidence를 전달할 뿐 resource-specific decision을 완료하지 않는다.

tenant ID도 principal claim에서 얻고 repository query scope에 사용한다. client body의 tenantId가 context를 덮어쓰지 못하게 한다. background job에서는 HTTP principal이 없으므로 job actor/service identity를 별도 context type으로 만들 수 있다.

실습에서 principal은 동일하지만 order owner가 다른 두 case를 만들어 authorization 결과가 달라지는지 확인한다.

---

## CHAPTER 10 · deadline context는 남은 시간 budget을 하위 layer에 전달한다

request 시작 시 absolute deadline을 context에 저장하면 repository와 HTTP adapter가 remaining time을 계산해 자신의 timeout을 설정할 수 있다. 각 layer가 독립적으로 5초 timeout을 갖는 것보다 전체 user budget을 보존하기 쉽다.

monotonic elapsed time과 wall-clock deadline 표현을 조심한다. distributed service에 absolute time을 전파할 때 clock skew가 영향을 줄 수 있으므로 standardized timeout/deadline mechanism을 사용하거나 충분한 margin을 둔다.

실습에서 1000ms deadline으로 시작해 300ms 작업 후 downstream timeout이 약 700ms 이하가 되는지 fake clock으로 test한다.

---

## CHAPTER 11 · queue boundary에서는 필요한 context를 message metadata로 명시적으로 넘긴다

HTTP request가 outbox/queue로 넘어가면 worker는 나중에 다른 process에서 실행된다. AsyncLocalStorage는 그 경계를 자동 통과하지 않는다. eventId, trace link, tenantId, initiating actor ID처럼 필요한 정보를 message schema에 넣고 consumer가 새 context를 만든다.

모든 HTTP header를 message에 복사하지 않는다. credential은 만료될 수 있고 secret 노출 범위가 커진다. worker가 필요한 authorization은 service identity와 durable business data를 기준으로 다시 판단한다.

실습에서 enqueue 시 request context 전체를 JSON serialize하려는 위험한 버전과 explicit metadata mapper를 비교한다. message schema가 작은지 확인한다.

---

## CHAPTER 12 · context의 개인정보는 log·trace exporter를 통해 외부 시스템으로 복제될 수 있다

context field는 logger와 tracing instrumentation이 자동으로 모든 event에 붙일 수 있다. email이나 phone을 넣으면 수천 log/span에 복제되어 삭제·retention 관리가 어려워진다. opaque user ID조차 개인정보 정책상 취급이 필요할 수 있다.

식별이 필요하면 internal surrogate ID, hash/pseudonymization, access-controlled log를 고려한다. incident에 필요한 최소 field만 넣는다. secret과 password는 context 금지 목록으로 둔다.

실습에서 context serializer가 allowed field만 출력하도록 하고 password/token/email fixture가 telemetry에 나타나지 않는지 test한다.

---

## CHAPTER 13 · nested internal calls에서도 correlation hierarchy를 보존한다

한 request가 `createOrder → reserveInventory → payment`를 호출할 때 모두 같은 request/trace context를 공유하되 각 operation은 별도 span과 operation name을 갖는다. logger가 current span ID를 포함하면 어느 내부 단계의 event인지 더 정확히 연결할 수 있다.

retry는 새 span을 만들되 같은 logical operation ID를 유지할 수 있다. duplicate attempt를 하나의 성공처럼 숨기지 않는다. dependency retry count와 attempt number를 trace attribute로 기록하면 latency가 왜 길었는지 보인다.

실습에서 payment 첫 attempt timeout, 두 번째 success를 만들고 같은 trace 안에 두 client span이 생기는지 확인한다.

---

## CHAPTER 14 · test runner에서도 context가 이전 테스트에서 새 테스트로 새지 않는지 확인한다

async context가 cleanup되지 않으면 test A의 request ID가 test B log에 남아 flaky assertion을 만들 수 있다. test마다 `run` scope를 새로 만들고 parallel test에서도 store isolation을 확인한다. global singleton logger가 context provider를 참조하는 경우 reset behavior를 명확히 한다.

production server에서도 long-lived timer나 event listener가 request context를 캡처해 memory leak을 만들 수 있다. request 종료 후 지속되는 background callback은 필요한 값만 copy하고 request store reference를 오래 보존하지 않는다.

실습에서 parallel test 100개가 각각 고유 context를 만들고 wrong-ID event가 0인지 검사한다. 종료 후 timer가 store를 참조하는 case도 profiling한다.

---

## CHAPTER 15 · context 실습은 correlation·identity·deadline·privacy를 하나의 request에서 검증한다

`POST /orders` request가 들어오면 internal request ID를 생성하고 incoming trace context를 검증하며 authenticated principal과 deadline을 context에 넣는다. service와 repository logger는 explicit body 없이 context metadata를 자동 포함한다. 외부 payment call에는 trace context와 remaining timeout만 전달하고, queue event에는 eventId·tenantId·trace link만 넣는다.

동시 요청 100개, malformed external request ID, oversized trace baggage, 다른 tenant body, deadline expiry, queue handoff를 각각 test한다. request ID가 섞이지 않고, client tenant 값이 principal을 덮지 않으며, secret/email이 telemetry와 message에 포함되지 않는지 확인한다.

AI는 AsyncLocalStorage boilerplate와 tracing SDK setup을 생성할 수 있다. 사람은 context에 어떤 field를 넣을지, 무엇을 security credential로 신뢰할지, process 경계를 넘을 때 무엇을 명시적으로 복사할지 결정한다. context가 작고 계약이 선명하면 동시 요청과 분산 호출에서도 **한 요청의 증거와 권한 경계를 잃지 않는다**.
