# 8장 실전 훈련편 — DNS·연결·TLS·HTTP·재시도

이 훈련편은 네트워크 용어 암기가 아니라 **어느 단계까지 성공했는지 증거로 나누는 연습**을 한다.

---

## 훈련 A. 네트워크 실패를 단계로 분류

다음 메시지를 가장 가까운 단계에 배치한다.

```text
UnknownHostException
Connection refused
TLS handshake failed
HTTP 401
HTTP 429
JSON parse error
```

예상 분류:

```text
DNS/이름해석
TCP/연결
TLS
HTTP 인증
HTTP rate limit
응답 body 처리
```

같은 “API 실패”라도 수정 지점이 다르다.

---

## 훈련 B. DNS가 정상이라는 증거

상황:

```text
host=api.example.com
DNS result=203.0.113.10
connect timeout
```

DNS는 성공했다. 다음 우선 후보는:

- 대상 IP/port 접근성
- 방화벽
- 서버 listen 상태
- 네트워크 경로

DNS 설정을 계속 바꾸는 것은 정보가 적다.

---

## 훈련 C. Connection refused와 timeout 구분

두 오류는 같은 의미가 아니다.

```text
connection refused
```

보통 대상에 도달했지만 해당 port가 연결을 받지 않는 상황을 포함할 수 있다.

```text
timeout
```

정해진 시간 안에 응답이 오지 않았다는 뜻이다.

질문:

- 서버 process가 죽어 port가 닫혔는가?
- firewall이 packet을 버리는가?
- 잘못된 IP/port인가?

오류 표현만으로 100% 확정하지 않되 후보를 달리한다.

---

## 훈련 D. URL 해부

URL:

```text
https://api.example.com:8443/v2/stocks/AAPL?currency=USD
```

각 부분을 찾는다.

```text
scheme = https
host = api.example.com
port = 8443
path = /v2/stocks/AAPL
query = currency=USD
```

포트가 생략됐을 때 scheme의 기본 포트가 사용될 수 있다는 점도 확인한다.

---

## 훈련 E. TLS 인증서 오류

로그:

```text
DNS success
connect success
certificate hostname mismatch
```

문제는 HTTP status code가 아니다. HTTP 요청 전에 TLS 검증에서 멈췄다.

가능한 후보:

- 잘못된 인증서
- 접속 host와 certificate 이름 불일치
- 프록시/중간자 설정

“서버가 500을 반환했다”고 보고하면 틀린 계층이다.

---

## 훈련 F. 401과 403 실제 판정

상황 1:

```text
Authorization header 없음
response 401
```

인증 정보 누락 후보가 강하다.

상황 2:

```text
정상 token
response 403
body="admin role required"
```

인증은 됐지만 권한 부족 후보가 강하다.

둘을 모두 로그인 화면으로 돌리는 UI가 적절한지 제품 요구사항 관점에서 검토한다.

---

## 훈련 G. 404가 서버 다운이라는 오해

```text
GET /api/v1/stcoks/AAPL
HTTP 404
```

`stocks` 철자가 `stcoks`로 잘못됐다.

서버는 응답하고 있다. 전체 서버 다운보다 **path/route 문제**가 강하다.

---

## 훈련 H. 500과 잘못된 요청

클라이언트가 잘못된 값을 보내도 서버가 버그 때문에 500을 반환할 수 있다.

즉:

```text
5xx = 서버에서 실패가 발생했다는 단서
```

이지 “클라이언트 입력은 무조건 정상”이라는 보장은 아니다.

원본 request와 server log를 같이 본다.

---

## 훈련 I. 429 대응

응답:

```text
HTTP 429
Retry-After: 60
```

잘못된 코드:

```python
while response.status == 429:
    response = request_again_immediately()
```

더 나은 방향:

- Retry-After 존중
- 요청 합치기
- 캐시
- 사용자별/작업별 rate control
- exponential backoff + jitter 필요 여부 검토

---

## 훈련 J. timeout 구간 측정

측정:

```text
DNS 15ms
connect 40ms
TLS 70ms
request upload 5ms
server wait 4900ms
response download 30ms
```

5초 timeout이 난다면 server wait가 핵심 후보다.

네트워크 전체를 “느림”으로 묶지 않는다.

---

## 훈련 K. retry가 중복 결제를 만드는 사고

흐름:

```text
결제 요청 A 전송
서버 결제 성공
응답이 네트워크에서 유실
클라이언트 timeout
클라이언트 요청 B 재시도
서버가 B도 새 결제로 처리
```

단순 retry가 위험한 이유다.

필요한 설계 후보:

- idempotency key
- client transaction ID
- 서버 중복 검증
- 결제 상태 조회

---

## 훈련 L. GET/POST만 외우지 말고 의도를 본다

대표적인 HTTP method의 흔한 의미:

```text
GET    읽기
POST   생성/작업 요청
PUT    전체 교체 성격
PATCH  일부 변경
DELETE 삭제
```

하지만 실제 API 계약이 최종 기준이다.

질문:

- GET 요청이 서버 데이터를 바꾸도록 설계하면 cache/proxy/retry 가정과 충돌할 수 있는 이유는?

HTTP 의미와 시스템 동작을 맞추는 것이 중요하다.

---

## 훈련 M. 헤더와 body 구분

예:

```text
Authorization: Bearer ***
Content-Type: application/json
Accept: application/json
```

body:

```json
{"symbol":"AAPL"}
```

인증 token과 비즈니스 데이터 역할을 구분한다.

로그에 Authorization header 전체를 남기지 않는다.

---

## 훈련 N. Content-Type 불일치

응답 헤더:

```text
Content-Type: text/html
```

클라이언트는 JSON을 기대한다.

body:

```html
<html><body>502 Bad Gateway</body></html>
```

클라이언트가 바로 JSON parser를 실행하면 parse error만 보일 수 있다.

진단 순서:

```text
status code
Content-Type
raw body 일부(민감정보 제외)
그 다음 parser
```

---

## 훈련 O. CORS를 서버 전체 장애로 착각하지 않기

브라우저 웹앱에서는 같은 API가 curl/native app에서는 되는데 브라우저에서 CORS 정책 때문에 차단될 수 있다.

```text
curl 200
native app 200
browser CORS error
```

서버가 완전히 다운된 것이 아니다.

CORS는 브라우저 보안 정책과 서버 응답 헤더가 관련된다. 깊은 내용은 후속 HTTP/API 권에서 다룬다.

---

## 훈련 P. 실제 네트워크 경로 표 만들기

기능 하나를 선택해 다음을 채운다.

```text
user action:
URL:
host:
DNS result:
port:
TLS:
method:
status:
content-type:
body schema:
parse result:
UI state:
```

빈 항목은 `미확인`으로 남긴다.

---

## AI 답 검증 1

AI 답:

> ping이 성공했으므로 HTTPS API도 정상입니다.

판정: 틀림.

ping과 TCP/TLS/HTTP는 다른 단계다. 서버가 ICMP에 응답해도 HTTPS가 실패할 수 있다.

---

## AI 답 검증 2

AI 답:

> 500 오류이므로 앱 코드는 볼 필요가 없습니다.

판정: 과도한 결론.

server-side failure가 맞더라도 잘못된 client request가 trigger했을 수 있고, 클라이언트의 오류 처리도 검토해야 한다.

---

## 디버깅 미션 1 — 웹은 되고 앱은 안 됨

증거:

```text
브라우저 웹: 정상
앱 API: 401
앱 token exp: 어제
```

전체 인터넷/서버 장애보다 앱 인증 token 만료가 강한 후보다.

다음 확인:

- refresh token 흐름
- 기기 시간
- token storage
- auth header 생성

---

## 디버깅 미션 2 — 특정 지역에서만 timeout

관찰:

```text
서울: 정상
해외 일부 ISP: timeout
DNS answer 다름
```

후보:

- CDN/Geo DNS
- 특정 경로 routing
- 지역별 firewall
- IPv4/IPv6 차이

클라이언트 UI 수정만으로 해결되지 않을 수 있다.

---

## 독립 프로젝트 — 견고한 API 요청 정책

가상의 조회 API를 설계한다.

### 정상 계약

```text
GET /stocks/{symbol}
```

### 실패 정책 작성

| 상황 | 클라이언트 행동 |
|---|---|
| DNS 실패 | 오류 표시/제한적 retry |
| connect timeout | retry 정책 적용 |
| TLS 오류 | 무작정 retry 금지, 인증 문제 확인 |
| 401 | 인증 갱신/로그인 정책 |
| 403 | 권한 안내 |
| 404 | 리소스 없음 처리 |
| 429 | Retry-After/backoff |
| 5xx | 제한된 retry/상태 표시 |
| 200 + invalid schema | validation FAIL, 저장 금지 |

### 마지막 단계

실행 로그에 secret 없이 어떤 필드를 남길지 설계한다.

---

# 8장 훈련 완료 기준

- DNS/connection/TLS/HTTP/parse 단계를 구분한다.
- URL의 scheme/host/port/path/query를 찾는다.
- 상태코드를 원인 확정이 아니라 방향 정보로 사용한다.
- timeout 시간을 세부 구간으로 측정한다.
- 429에 즉시 무한 retry하지 않는다.
- 재시도가 중복 작업을 만들 수 있음을 이해한다.
- Content-Type/raw body를 parser 오류 전에 확인한다.
- 브라우저 CORS와 서버 전체 장애를 구분한다.
- API 실패 정책을 상태별로 설계한다.
