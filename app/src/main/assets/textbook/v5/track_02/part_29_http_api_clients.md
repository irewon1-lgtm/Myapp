# PART 29 · HTTP API client — 외부 서비스 호출을 timeout·retry·schema·idempotency 계약으로 다루기

Python에서 외부 API를 호출하는 코드는 몇 줄이면 만들 수 있지만 실제 안정성은 그 몇 줄 밖에 있다. DNS, TLS, connection pool, timeout, HTTP status, malformed body, rate limit, partial failure, retry가 모두 호출 결과에 영향을 준다. API client를 단순 `requests.get()` 래퍼가 아니라 **외부 실패를 내부 의미로 번역하는 adapter**로 보면 구조가 선명해진다.

---

## CHAPTER 01 · HTTP status와 application success는 같은 층이 아니다

2xx status는 HTTP 요청이 성공적으로 처리되었다는 뜻이지만 body의 업무 결과까지 항상 성공이라는 뜻은 아니다. 반대로 일부 API는 4xx에 구조화된 validation error를 제공한다. Client는 transport status와 domain response를 단계적으로 해석한다.

Expected status set을 명시하고 unknown status를 조용히 성공으로 취급하지 않는다. 204처럼 body가 없는 응답에 무조건 JSON parser를 적용하면 정상 응답을 오류로 만들 수 있다.

Redirect를 자동으로 따를지, authentication header가 다른 host로 전달되는지 library 정책을 확인한다. HTTP semantics를 client library default에 전부 맡기지 않는다.

---

## CHAPTER 02 · timeout은 connect와 read, 전체 deadline을 구분한다

외부 호출이 영원히 기다리지 않게 timeout을 둔다. 연결 자체가 안 되는 경우와 연결 후 server가 response bytes를 주지 않는 경우는 다른 timeout 단계다. Library가 하나의 숫자로 모든 단계를 처리하는지 별도 값을 제공하는지 확인한다.

Retry가 있는 경우 각 시도 timeout을 단순히 5초로 두고 세 번 재시도하면 전체 request가 15초 이상 걸릴 수 있다. Parent deadline에서 남은 budget을 계산해 child call에 전달한다.

Timeout 후 remote side가 실제 operation을 완료했을 수 있다는 점은 side-effectful POST에서 중요하다. Local timeout은 remote cancellation 증거가 아니다.

---

## CHAPTER 03 · retry는 status·method·idempotency에 따라 선택한다

Connection reset과 503은 일시적일 수 있지만 400 validation error는 재시도로 바뀌지 않는다. Retry 가능한 오류 목록을 명시하고 exponential backoff와 jitter를 사용해 장애 중인 server에 동시에 재요청하는 현상을 줄인다.

GET 같은 읽기 operation은 일반적으로 재시도 의미가 단순하지만 POST가 결제나 주문 생성이라면 duplicate side effect가 생길 수 있다. Server가 idempotency key를 지원하면 같은 logical request를 안전하게 재전송할 수 있다.

429 rate limit에서는 `Retry-After`와 quota policy를 존중한다. 무조건 즉시 retry하면 제한을 더 악화시킨다.

---

## CHAPTER 04 · response parsing은 content type과 schema를 검증한다

Status 200이라고 body가 항상 예상 JSON schema라는 보장은 없다. Proxy error page가 HTML을 반환하거나 server bug로 field type이 바뀔 수 있다. Content-Type과 body size를 확인하고 parser·schema validation을 거친 뒤 typed model로 변환한다.

Unknown field를 무시할지 거부할지 compatibility policy를 정한다. 외부 provider가 새 field를 추가하는 것이 정상이라면 strict rejection이 불필요한 outage를 만들 수 있다. 반대로 security-sensitive command payload는 closed schema가 적합할 수 있다.

Schema mismatch를 `None`으로 숨기지 않는다. Provider contract drift라는 별도 오류로 분류해 alert와 fallback을 결정할 수 있게 한다.

---

## CHAPTER 05 · authentication header와 token refresh는 concurrency를 가진다

Bearer token을 header에 넣는 것 자체는 간단하지만 token expiration과 refresh가 여러 task/thread에서 동시에 발생할 수 있다. 만료 직전 요청 수십 개가 모두 refresh를 시작하면 token endpoint에 burst를 만들 수 있다.

Refresh를 single-flight로 묶거나 credential manager가 한 번만 갱신하도록 coordination한다. 새 token을 얻는 데 실패했을 때 기존 token을 계속 쓸지 즉시 인증 실패로 처리할지 정책을 정한다.

Secret은 request/response log와 exception에 남기지 않는다. Header 전체 dump보다 allowlist field logging을 사용한다.

---

## CHAPTER 06 · connection pooling은 latency를 줄이지만 client lifetime을 요구한다

요청마다 새 HTTP client와 TCP/TLS connection을 만들면 handshake 비용이 반복된다. Long-lived client가 connection pool을 재사용하면 latency와 resource 효율이 좋아질 수 있다. 하지만 pool size와 DNS/TLS lifetime, shutdown cleanup을 관리해야 한다.

Client object를 함수마다 만들고 즉시 버리는 패턴은 pool 이점을 잃고 socket churn을 만들 수 있다. Application lifetime 또는 service lifetime에 맞춰 client를 소유한다.

Pool이 가득 찼을 때 대기 시간이 timeout budget에 포함되는지 확인한다. Downstream가 느려지면 connection queue가 새로운 bottleneck이 된다.

---

## CHAPTER 07 · pagination은 전체 목록이 아니라 cursor state machine이다

API가 page token을 반환한다면 client는 current token, received items, termination condition을 가진 반복 상태 머신이 된다. 같은 token이 반복되거나 empty page인데 next token이 존재하는 provider bug에 대비해 page budget과 duplicate-token detection을 둘 수 있다.

Offset pagination은 데이터가 동시에 변경되면 중복/누락이 생길 수 있고 cursor pagination은 snapshot semantics가 provider에 따라 다르다. “모든 항목을 정확히 한 번” 요구가 있다면 API contract를 확인한다.

대량 목록은 generator로 page/item을 streaming할 수 있지만 consumer가 늦으면 connection/resource lifetime이 길어질 수 있다. Eager page fetch와 lazy item iteration의 경계를 설계한다.

---

## CHAPTER 08 · rate limit은 client 전체의 shared resource budget이다

한 request가 quota를 초과하지 않아도 여러 worker가 동시에 같은 API key를 사용하면 전체 rate limit을 넘을 수 있다. Process-local limiter 하나로 distributed worker 전체 quota를 보장할 수 있는지 확인한다.

Token bucket이나 semaphore로 concurrency/rate를 제한하고 priority가 다른 workload를 분리할 수 있다. Background sync가 interactive request quota를 모두 소비하지 않게 한다.

Response header로 남은 quota와 reset time을 제공한다면 metric으로 관찰해 limit 직전 behavior를 조절할 수 있다.

---

## CHAPTER 09 · fallback은 실패를 숨기지 않고 품질 저하를 계약으로 만든다

Primary provider가 실패했을 때 stale cache나 secondary provider를 사용하는 fallback은 availability를 높일 수 있지만 데이터 freshness와 의미가 달라진다. User에게 어떤 품질의 결과인지 표시할지, 어떤 endpoint에서 fallback을 허용하지 않을지 정한다.

Fallback이 primary failure를 모두 숨기면 장애가 장기간 발견되지 않을 수 있다. Metric과 log에는 primary error를 남기되 사용자 request는 degrade된 결과를 받을 수 있다.

Secondary provider의 schema와 의미가 정확히 같은지 확인한다. 가격 source가 다르면 timestamp와 currency, rounding이 달라질 수 있다.

---

## CHAPTER 10 · API client는 transport detail을 domain error로 번역하는 anti-corruption layer다

Application core가 HTTP status, JSON key, token refresh를 직접 알게 하면 provider 변경이 전체 codebase로 퍼진다. Client adapter가 외부 계약을 내부 typed result와 exception으로 변환하면 service는 `ProductUnavailable`, `RateLimited`, `ProviderTemporaryFailure` 같은 의미를 다룰 수 있다.

이 경계에는 timeout, retry, logging, schema validation을 모으되 business decision까지 넣지 않는다. Provider가 404를 반환했을 때 이것이 “상품 없음”인지 “endpoint 잘못됨”인지는 endpoint contract를 기준으로 번역한다.

API client의 품질은 요청 코드를 얼마나 짧게 썼는지가 아니라 **외부 실패와 변화가 내부 domain으로 무질서하게 새어 들어오지 않도록 얼마나 정확한 경계를 만들었는가**로 평가한다.