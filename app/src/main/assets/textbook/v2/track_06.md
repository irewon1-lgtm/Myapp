# TRACK 06 · 인터넷·네트워크·API

앱이 서버와 데이터를 주고받는 순간부터 많은 개발자가 `네트워크가 문제인가 봐`라고 한 덩어리로 생각한다.

이 TRACK에서는 통신을 층으로 나눈다.

```text
내 기기
↓
네트워크 연결
↓
주소 찾기
↓
서버까지 연결
↓
암호화된 통신
↓
HTTP 요청
↓
데이터 형식
↓
API 규칙
↓
브라우저 보안 정책
```

특히 MIME, Content-Type, Accept, multipart/form-data, boundary는 먼저 **왜 데이터 종류를 알려줘야 하는가**부터 이해한다.

---

## BLOCK 01 · 네트워크

### LESSON 01 · 장치끼리 데이터를 주고받기

여러 컴퓨터와 장치가 서로 데이터를 주고받을 수 있게 연결된 구조를 **네트워크(network)**라고 부른다.

집의 휴대폰과 공유기도 작은 네트워크를 만든다.

### LESSON 02 · 인터넷

전 세계의 수많은 네트워크를 서로 연결한 거대한 연결망을 **인터넷(Internet)**이라고 부른다.

웹은 인터넷을 사용하는 서비스 중 하나다.

---

## BLOCK 02 · Wi-Fi와 인터넷

### LESSON 01 · Wi-Fi

Wi-Fi는 기기가 가까운 무선 네트워크에 연결되는 기술이다.

휴대폰의 Wi-Fi 아이콘이 켜졌다는 것은 보통 공유기까지 연결되었다는 뜻이다.

### LESSON 02 · Wi-Fi가 되어도 인터넷은 안 될 수 있다

```text
휴대폰 → 공유기 : 정상
공유기 → 외부 인터넷 : 장애
```

이면 Wi-Fi 아이콘은 있어도 웹사이트가 열리지 않을 수 있다.

`Wi-Fi 연결 = 인터넷 전체 정상`은 아니다.

---

## BLOCK 03 · client와 server

### LESSON 01 · client

다른 프로그램에 데이터나 기능을 요청하는 쪽을 **클라이언트(client)**라고 부른다.

브라우저나 모바일 앱이 서버 API에 요청할 때 클라이언트 역할을 한다.

### LESSON 02 · server

요청을 받아 처리하고 결과를 돌려주는 쪽을 **서버(server)**라고 부른다.

서버는 물리 컴퓨터 한 대뿐 아니라 요청을 처리하는 프로그램과 시스템을 가리키기도 한다.

---

## BLOCK 04 · 주소가 필요한 이유

### LESSON 01 · 어디로 보낼지 알아야 한다

택배가 목적지 주소 없이 갈 수 없듯 네트워크 데이터도 목적지를 찾을 정보가 필요하다.

그 역할에서 중요한 것이 **IP 주소**다.

---

## BLOCK 05 · IP 주소

### LESSON 01 · 네트워크의 숫자 주소

**IP 주소(IP address)**는 IP 네트워크에서 장치 또는 네트워크 인터페이스를 식별하고 데이터를 전달할 위치를 찾는 데 사용하는 주소다.

예시 IPv4 주소:

```text
192.0.2.10
```

### LESSON 02 · IPv4와 IPv6

IPv4는 32비트 주소 공간을 사용한다.

IPv6는 훨씬 큰 128비트 주소 공간을 사용한다.

IPv6 예시:

```text
2001:db8::1
```

지금 주소를 외우는 것이 아니라 `서버를 찾는 숫자 주소가 필요하다`는 구조를 이해한다.

---

## BLOCK 06 · private IP와 public IP

### LESSON 01 · 집 안에서 쓰는 주소

집이나 회사 내부 네트워크에서는 사설 IP 주소를 사용할 수 있다.

예:

```text
192.168.x.x
10.x.x.x
```

### LESSON 02 · public IP

외부 인터넷에서 라우팅 가능한 주소를 **공인 IP(public IP)**라고 부른다.

한 공유기 뒤의 여러 기기가 하나의 공인 IP를 공유할 수도 있다.

---

## BLOCK 07 · NAT

### LESSON 01 · 내부 주소와 외부 주소 사이를 연결한다

가정용 공유기는 여러 내부 기기의 사설 IP 통신을 하나의 공인 IP를 통해 외부로 연결하는 **NAT(Network Address Translation)** 기능을 사용할 수 있다.

아주 쉽게:

> NAT = 내부 여러 장치의 주소와 외부 인터넷 주소 사이를 변환·연결하는 기능

### LESSON 02 · 밖에서 내 PC로 바로 들어오기 어려운 이유

NAT 뒤의 내부 장치는 외부 인터넷에서 바로 접근할 수 없는 경우가 많다.

서버를 집에서 열 때 포트 포워딩 같은 별도 설정이 필요한 이유 중 하나다.

---

## BLOCK 08 · domain

### LESSON 01 · 숫자 주소를 외우기 어렵다

사람은 `142.250...` 같은 숫자보다 `example.com` 같은 이름을 기억하기 쉽다.

이런 인터넷 이름을 **도메인 이름(domain name)**이라고 부른다.

### LESSON 02 · domain이 IP 자체는 아니다

도메인은 사람이 사용하는 이름이고 실제 통신에는 IP 주소 정보가 필요하다.

도메인을 IP로 연결해 찾는 시스템이 DNS다.

---

## BLOCK 09 · DNS

### LESSON 01 · 이름을 주소 정보로 찾기

**DNS(Domain Name System)**는 도메인 이름과 관련된 IP 주소 정보를 찾을 수 있게 하는 분산 이름 시스템이다.

비유:

```text
사람 이름 → 전화번호
도메인 이름 → IP 주소 정보
```

완전히 같은 시스템은 아니고 역할을 이해하기 위한 비유다.

### LESSON 02 · DNS lookup

브라우저가 `example.com`을 열 때 먼저 DNS를 통해 서버 IP를 찾을 수 있다.

이 조회를 **DNS lookup**이라고 부른다.

---

## BLOCK 10 · DNS record

### LESSON 01 · A record

도메인을 IPv4 주소와 연결하는 DNS 레코드를 **A record**라고 부른다.

### LESSON 02 · AAAA record

IPv6 주소와 연결하는 레코드는 **AAAA record**다.

### LESSON 03 · CNAME

한 도메인 이름을 다른 도메인 이름의 별칭으로 연결하는 레코드를 **CNAME**이라고 부른다.

---

## BLOCK 11 · port

### LESSON 01 · 한 컴퓨터 안의 어느 서비스인가

IP 주소로 컴퓨터까지 찾아왔어도 그 컴퓨터에서 여러 서버 프로그램이 실행될 수 있다.

어느 네트워크 서비스와 통신할지를 구분하는 번호가 **포트(port)**다.

비유:

```text
IP = 건물 주소
port = 건물 안 서비스 번호
```

### LESSON 02 · 대표 포트

HTTP는 80, HTTPS는 443을 기본 포트로 많이 사용한다.

하지만 다른 포트를 사용할 수도 있다.

---

## BLOCK 12 · protocol

### LESSON 01 · 통신 규칙

컴퓨터끼리 데이터를 주고받으려면 양쪽이 같은 규칙을 알아야 한다.

이런 통신 규칙을 **프로토콜(protocol)**이라고 부른다.

### LESSON 02 · 여러 층의 protocol

웹 통신 한 번에도:

```text
IP
TCP 또는 QUIC
TLS
HTTP
```

같은 여러 규칙이 역할을 나눠 사용될 수 있다.

---

## BLOCK 13 · packet

### LESSON 01 · 큰 데이터를 작은 단위로 나누어 보낸다

네트워크에서는 데이터를 작은 단위로 나누어 전달한다.

이런 전송 단위를 넓게 **패킷(packet)**이라고 부른다.

### LESSON 02 · 한 파일이 한 번에 통째로 날아가는 것은 아니다

100MB 파일도 네트워크에서는 많은 작은 데이터 조각으로 나뉘어 이동한다.

각 계층마다 헤더를 붙여 목적지와 순서 같은 정보를 전달한다.

---

## BLOCK 14 · TCP

### LESSON 01 · 연결을 만든다

**TCP(Transmission Control Protocol)**는 두 프로그램 사이에 신뢰성 있는 바이트 스트림 연결을 제공하는 전송 프로토콜이다.

초보용 뜻:

> TCP = 데이터를 빠뜨리지 않고 순서대로 전달하도록 확인하는 연결 방식

### LESSON 02 · 순서와 재전송

네트워크에서 일부 데이터가 사라지면 TCP는 누락된 데이터를 다시 보내도록 처리할 수 있다.

받는 쪽에는 원래 순서대로 전달한다.

---

## BLOCK 15 · TCP handshake

### LESSON 01 · 연결하기 전에 서로 준비 확인

TCP 연결을 시작할 때 대표적으로 세 단계의 **3-way handshake**를 사용한다.

```text
SYN →
← SYN-ACK
ACK →
```

### LESSON 02 · 세 글자를 외우는 것이 목적이 아니다

핵심은:

> HTTP 데이터를 보내기 전에 TCP 연결 자체를 만드는 과정이 필요할 수 있다.

이다.

연결 단계에서 실패하면 HTTP 상태 코드조차 받을 수 없다.

---

## BLOCK 16 · UDP

### LESSON 01 · 연결 확인을 단순화한 전송

**UDP(User Datagram Protocol)**는 TCP처럼 연결과 재전송·순서 보장을 제공하지 않는 더 단순한 데이터그램 전송 프로토콜이다.

### LESSON 02 · 나쁜 프로토콜이 아니다

실시간 음성·게임처럼 약간의 데이터 손실보다 지연시간이 더 중요한 경우 UDP 계열 접근이 적합할 수 있다.

목적에 따라 선택한다.

---

## BLOCK 17 · latency와 bandwidth

### LESSON 01 · latency

요청을 보낸 뒤 응답의 첫 결과가 오기까지 걸리는 시간을 **지연시간(latency)**이라고 부른다.

### LESSON 02 · bandwidth

일정 시간 동안 얼마나 많은 데이터를 전송할 수 있는지를 **대역폭(bandwidth)**이라고 부른다.

비유:

```text
latency = 첫 차가 도착하는 시간
bandwidth = 한 시간 동안 지나갈 수 있는 차의 수
```

---

## BLOCK 18 · TLS가 필요한 이유

### LESSON 01 · 중간에서 내용을 보면 안 된다

비밀번호가 인터넷을 지나갈 때 누군가 중간에서 그대로 읽을 수 있으면 위험하다.

통신 내용을 암호화하고 상대 서버를 확인하는 보안 계층이 필요하다.

### LESSON 02 · TLS

**TLS(Transport Layer Security)**는 인터넷 통신을 암호화하고 상대의 신원을 인증하는 데 사용하는 보안 프로토콜이다.

HTTPS는 HTTP를 TLS 위에서 사용한다.

---

## BLOCK 19 · certificate 맛보기

### LESSON 01 · 서버가 누구인지 확인

TLS 연결에서 서버는 **디지털 인증서(certificate)**를 제시할 수 있다.

인증서에는 도메인과 공개키 정보 등이 들어 있다.

### LESSON 02 · 브라우저가 인증서를 검증한다

브라우저는 인증서가 신뢰할 수 있는 인증기관 체인으로 서명되었는지, 도메인이 맞는지, 유효기간이 남았는지 등을 확인한다.

정확한 암호학 원리는 TRACK 09에서 배운다.

---

## BLOCK 20 · HTTP

### LESSON 01 · 웹 요청과 응답의 규칙

**HTTP(Hypertext Transfer Protocol)**는 클라이언트와 서버가 웹 자원에 대한 요청과 응답을 주고받는 규칙이다.

### LESSON 02 · HTTPS

**HTTPS**는 HTTP 통신을 TLS로 보호해 사용하는 형태다.

HTTPS라고 해서 사이트 내용 자체가 무조건 안전하거나 선하다는 뜻은 아니다.

---

## BLOCK 21 · URL

### LESSON 01 · 자원의 위치 표현

```text
https://api.example.com:443/users/10?active=true#profile
```

이런 자원 위치 표현을 **URL**이라고 부른다.

### LESSON 02 · 부분 나누기

```text
https        = scheme
api.example.com = host
443          = port
/users/10    = path
active=true  = query string
profile      = fragment
```

각 부분이 역할을 가진다.

---

## BLOCK 22 · scheme과 host

### LESSON 01 · scheme

URL 앞의 `https`처럼 어떤 방식으로 자원에 접근할지 나타내는 부분을 **scheme**이라고 부른다.

### LESSON 02 · host

요청을 보낼 서버의 이름이나 주소 부분을 **host**라고 부른다.

도메인 이름이 들어갈 수도 있고 IP 주소가 들어갈 수도 있다.

---

## BLOCK 23 · query string

### LESSON 01 · URL에 추가 조건 보내기

```text
/products?page=2&sort=price
```

`?` 뒤의 부분을 **query string**이라고 부른다.

```text
page=2
sort=price
```

처럼 이름과 값을 보낼 수 있다.

### LESSON 02 · 비밀값을 query에 넣지 않기

URL은 브라우저 기록, 서버 로그, 프록시 등에 남을 수 있다.

비밀번호나 secret token을 query string에 넣지 않는다.

---

## BLOCK 24 · request

### LESSON 01 · 서버에게 보내는 메시지

클라이언트가 서버에게 `무엇을 해 달라`고 보내는 HTTP 메시지를 **request(요청)**라고 부른다.

요청에는:

```text
method
URL/path
headers
body
```

등이 들어갈 수 있다.

---

## BLOCK 25 · response

### LESSON 01 · 서버가 돌려주는 결과

서버가 요청을 처리한 뒤 클라이언트에 보내는 HTTP 메시지를 **response(응답)**라고 부른다.

응답에는:

```text
status code
headers
body
```

가 들어갈 수 있다.

---

## BLOCK 26 · HTTP method

### LESSON 01 · GET

주로 데이터를 조회할 때 `GET`을 사용한다.

```http
GET /users/10
```

### LESSON 02 · POST

새 작업이나 자원 생성을 요청할 때 `POST`를 많이 사용한다.

### LESSON 03 · PUT과 PATCH

`PUT`은 자원 전체 교체 의미로, `PATCH`는 일부 변경 의미로 사용하는 API 설계가 많다.

### LESSON 04 · DELETE

자원을 삭제해 달라는 의도를 나타낼 때 사용한다.

메서드 이름만 적었다고 서버가 자동으로 그 의미를 지키는 것은 아니다. API 구현이 규칙을 따라야 한다.

---

## BLOCK 27 · safe와 idempotent

### LESSON 01 · safe method

서버 상태를 바꾸지 않는 조회 목적의 메서드를 HTTP에서 **safe**하다고 분류한다.

GET이 대표적이다.

### LESSON 02 · idempotent

같은 요청을 여러 번 수행해도 최종 상태 효과가 한 번 수행한 것과 같은 성질을 **멱등성(idempotency)**이라고 부른다.

예:

```text
PUT /users/1 name=민수
```

를 같은 내용으로 여러 번 보내도 최종 이름은 `민수`다.

결제 POST처럼 반복하면 돈이 여러 번 빠질 수 있는 작업은 별도 idempotency key 설계가 필요하다.

---

## BLOCK 28 · status code

### LESSON 01 · 2xx

요청이 성공적으로 처리된 범주다.

```text
200 OK
201 Created
204 No Content
```

### LESSON 02 · 3xx

다른 위치로 이동하거나 캐시와 관련된 응답에 사용되는 범주다.

### LESSON 03 · 4xx

클라이언트 요청에 문제가 있음을 나타내는 범주다.

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
429 Too Many Requests
```

### LESSON 04 · 5xx

서버가 요청 처리 중 실패했음을 나타내는 범주다.

```text
500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
```

---

## BLOCK 29 · 401과 403

### LESSON 01 · 401

인증 정보가 없거나 유효하지 않아 누군지 확인되지 않은 경우 `401`을 사용할 수 있다.

### LESSON 02 · 403

누군지는 알지만 해당 작업 권한이 없는 경우 `403`을 사용할 수 있다.

```text
401 = 누구인지 확인되지 않음
403 = 누구인지 알지만 이 작업은 허용되지 않음
```

---

## BLOCK 30 · header

### LESSON 01 · 메시지에 대한 추가 정보

HTTP 메시지의 성격과 처리 방법에 관한 추가 정보를 **header**에 넣는다.

예:

```http
Content-Type: application/json
Authorization: Bearer ...
Cache-Control: no-cache
```

### LESSON 02 · body

실제 주요 데이터 내용은 **body**에 넣을 수 있다.

모든 요청과 응답이 body를 가지는 것은 아니다.

---

## BLOCK 31 · 데이터 종류를 왜 알려줘야 하나

### LESSON 01 · 받은 바이트가 무엇인지 알아야 한다

서버가 데이터 덩어리를 받았다.

이것이:

```text
HTML 문서인가?
JSON인가?
PNG 사진인가?
PDF인가?
```

알아야 올바르게 해석할 수 있다.

### LESSON 02 · 종류표

그래서 인터넷으로 주고받는 데이터에 `이 데이터는 이런 종류입니다`라고 알려주는 표준 이름을 사용한다.

그것이 MIME type이다.

---

## BLOCK 32 · MIME type

### LESSON 01 · 한 줄 뜻

**MIME type**은 데이터의 종류와 형식을 나타내는 표준 이름이다.

> MIME type = 인터넷 데이터에 붙이는 종류표

### LESSON 02 · text/html

```text
text/html
```

```text
text = 글자 계열
html = HTML 형식
```

즉 HTML 문서다.

### LESSON 03 · image/png

```text
image/png
```

PNG 이미지 데이터라는 뜻이다.

### LESSON 04 · application/json

```text
application/json
```

JSON 형식의 프로그램용 데이터라는 뜻이다.

---

## BLOCK 33 · JSON

### LESSON 01 · 구조화된 데이터를 글자로 표현

**JSON(JavaScript Object Notation)**은 객체와 배열 같은 데이터를 정해진 문자열 형식으로 표현하는 데이터 포맷이다.

```json
{
  "name": "민수",
  "age": 20,
  "skills": ["HTML", "CSS"]
}
```

### LESSON 02 · JSON과 JavaScript 객체는 다르다

JSON은 문자열 데이터 형식이다.

JavaScript object는 실행 중 메모리에 존재하는 객체다.

```javascript
JSON.parse(text)
```

로 JSON 문자열을 객체로 바꿀 수 있다.

---

## BLOCK 34 · Content-Type

### LESSON 01 · 앞에서 배운 header와 MIME을 연결한다

**Content-Type**은 HTTP 메시지 body의 데이터가 어떤 MIME type인지 알려주는 header다.

```http
Content-Type: application/json
```

사람말:

```text
이 메시지 body는 JSON 데이터입니다.
```

### LESSON 02 · 잘못된 Content-Type

실제 body는 JSON인데:

```http
Content-Type: image/png
```

라고 보내면 받는 프로그램이 잘못 해석하거나 거부할 수 있다.

---

## BLOCK 35 · Accept

### LESSON 01 · 어떤 응답을 받고 싶은가

**Accept**는 클라이언트가 어떤 MIME type의 응답을 받을 수 있거나 선호하는지 서버에 알려주는 header다.

```http
Accept: application/json
```

### LESSON 02 · Content-Type과 구분

```text
Content-Type = 내가 지금 보내는 실제 body 종류
Accept       = 내가 받고 싶은 응답 종류
```

둘을 뒤집지 않는다.

---

## BLOCK 36 · charset

### LESSON 01 · 문자열 인코딩 정보

텍스트 MIME type에는 문자 인코딩 정보를 함께 표시할 수 있다.

```http
Content-Type: text/html; charset=utf-8
```

사람말:

```text
HTML 글자 데이터이고 문자 인코딩은 UTF-8입니다.
```

TRACK 01의 인코딩이 실제 HTTP에서 연결된다.

---

## BLOCK 37 · form data

### LESSON 01 · HTML form 값을 보내기

사용자가:

```text
이름 = 민수
나이 = 20
```

를 입력했다.

이런 폼 데이터를 HTTP body에 담아 서버로 보낼 수 있다.

### LESSON 02 · application/x-www-form-urlencoded

간단한 폼 값은:

```text
name=%EB%AF%BC%EC%88%98&age=20
```

같은 URL encoded 형식으로 보낼 수 있다.

---

## BLOCK 38 · 파일 업로드 문제

### LESSON 01 · 글자와 파일을 한 요청에 같이 보내고 싶다

프로필 등록:

```text
nickname = minsu
photo = me.png
```

글자 필드와 바이너리 파일을 함께 보낼 방법이 필요하다.

### LESSON 02 · 여러 부분으로 나눈다

HTTP body를 여러 조각으로 나누고 각 조각에 이름과 데이터를 담는 방식이 있다.

그 대표 MIME type이 `multipart/form-data`다.

---

## BLOCK 39 · multipart/form-data

### LESSON 01 · 단어부터 쪼갠다

```text
multi = 여러
part  = 부분
form-data = 폼 데이터
```

즉:

> 하나의 body를 여러 부분으로 나누어 폼 값과 파일을 함께 담는 형식

이다.

### LESSON 02 · 조각마다 정보가 있다

각 부분은:

```text
필드 이름
파일 이름
Content-Type
실제 데이터
```

등을 가질 수 있다.

---

## BLOCK 40 · boundary

### LESSON 01 · 조각 사이 경계선

여러 부분이 하나의 body 안에 붙어 있으면 어디서 하나가 끝나는지 표시해야 한다.

그 경계 문자열을 **boundary**라고 부른다.

### LESSON 02 · 실제 Content-Type

```http
Content-Type: multipart/form-data; boundary=----ABC
```

사람말:

```text
body는 multipart/form-data 형식이고
각 데이터 조각은 ----ABC라는 경계로 나눕니다.
```

### LESSON 03 · 브라우저 FormData

브라우저의 `FormData`를 사용하면 browser가 boundary를 포함한 Content-Type을 자동으로 설정하는 경우가 많다.

개발자가 Content-Type만 수동으로 덮어써 boundary를 빼면 서버가 body를 해석하지 못할 수 있다.

---

## BLOCK 41 · API

### LESSON 01 · 프로그램끼리 사용하는 접점

**API(Application Programming Interface)**는 한 프로그램이 다른 프로그램의 기능이나 데이터를 정해진 방법으로 사용할 수 있게 만든 접점과 규칙이다.

### LESSON 02 · HTTP API

웹에서 HTTP를 이용해 API를 제공하는 경우가 많다.

```http
GET /users/10
```

을 `10번 사용자 정보를 가져오는 API`로 정의할 수 있다.

```text
HTTP = 통신 규칙
API = 기능을 어떻게 사용할지 정한 규칙
```

---

## BLOCK 42 · REST

### LESSON 01 · 자원을 중심으로 HTTP를 사용하는 설계 방식

**REST**는 웹의 자원과 HTTP 의미를 이용해 API를 설계하는 아키텍처 스타일이다.

API를:

```text
/users
/orders
/products
```

같은 자원 중심으로 표현하고 HTTP method를 활용한다.

### LESSON 02 · REST = JSON은 아니다

REST API가 JSON을 많이 사용하지만 REST 자체가 JSON을 뜻하는 것은 아니다.

REST는 API 설계 스타일이고 JSON은 데이터 형식이다.

---

## BLOCK 43 · endpoint

### LESSON 01 · API를 호출하는 구체적인 주소

API에서 특정 기능을 호출할 수 있는 URL과 method 조합을 **endpoint**라고 부른다.

예:

```text
GET /users/10
POST /users
```

같은 path라도 method가 다르면 다른 동작이 될 수 있다.

---

## BLOCK 44 · pagination

### LESSON 01 · 100만 개를 한 번에 보내지 않기

데이터가 매우 많으면 한 번에 모두 보내는 대신 페이지로 나눈다.

이를 **페이지네이션(pagination)**이라고 부른다.

```text
?page=1&size=50
```

### LESSON 02 · offset 방식

```text
offset=100&limit=50
```

처럼 몇 개를 건너뛴 뒤 일정 개수를 가져올 수 있다.

큰 데이터에서 뒤 페이지로 갈수록 DB 비용이 커질 수 있다.

### LESSON 03 · cursor 방식

마지막으로 본 항목의 식별값을 다음 요청 기준으로 보내는 **cursor pagination**도 있다.

대량 데이터와 실시간 추가에 유리한 경우가 많다.

---

## BLOCK 45 · 인증과 권한

### LESSON 01 · authentication

사용자가 누구인지 확인하는 과정을 **인증(authentication)**이라고 부른다.

```text
비밀번호 확인
생체인증
인증 토큰 확인
```

### LESSON 02 · authorization

확인된 사용자가 어떤 작업을 할 수 있는지 판단하는 것을 **인가/권한 확인(authorization)**이라고 부른다.

```text
인증 = 누구인가?
인가 = 무엇을 할 수 있는가?
```

---

## BLOCK 46 · cookie

### LESSON 01 · 브라우저가 사이트 정보를 저장하고 보낸다

**cookie**는 브라우저가 사이트와 관련된 작은 값을 저장하고 조건에 맞는 HTTP 요청에 자동으로 포함할 수 있는 기능이다.

### LESSON 02 · cookie 속성

```text
Secure
HttpOnly
SameSite
Expires/Max-Age
```

같은 설정으로 전송과 접근 범위를 제한한다.

보안 의미는 TRACK 09에서 깊게 다룬다.

---

## BLOCK 47 · session

### LESSON 01 · 서버에서 로그인 상태 관리

**session**은 서버가 사용자별 상태를 일정 기간 저장해 두는 방식이다.

브라우저 cookie에는 session ID만 저장하고 실제 로그인 정보는 서버에 둘 수 있다.

### LESSON 02 · session ID

session ID는 서버의 특정 session을 찾기 위한 식별값이다.

다른 사람이 훔치면 사용자를 가장할 수 있으므로 안전하게 보호해야 한다.

---

## BLOCK 48 · token

### LESSON 01 · 인증 증표

서버가 사용자의 인증 상태나 권한을 확인할 수 있도록 발급하는 값을 **token**이라고 부를 수 있다.

### LESSON 02 · Bearer token

```http
Authorization: Bearer abc123...
```

처럼 `Bearer` 방식으로 token을 보낼 수 있다.

Bearer token은 **가진 사람이 권한을 행사할 수 있는 증표**에 가깝기 때문에 노출되면 위험하다.

---

## BLOCK 49 · JWT 맛보기

### LESSON 01 · token 표현 방식 중 하나

**JWT(JSON Web Token)**는 JSON 기반 정보를 서명 가능한 형태로 표현하는 표준이다.

`token = JWT`는 아니다. JWT는 여러 token 형식 중 하나다.

### LESSON 02 · JWT payload는 기본적으로 비밀이 아니다

JWT의 payload는 보통 base64url 인코딩되어 있어 누구나 decode할 수 있다.

서명은 변조 여부를 확인하는 데 사용하지 내용을 숨기지는 않는다.

민감정보를 무심코 payload에 넣지 않는다.

---

## BLOCK 50 · CORS 전에 origin

### LESSON 01 · origin

웹에서 **origin**은 보통 다음 세 가지 조합이다.

```text
scheme + host + port
```

예:

```text
https://example.com:443
```

### LESSON 02 · 다른 origin

```text
https://example.com
https://api.example.com
```

host가 다르므로 서로 다른 origin이다.

브라우저는 다른 origin 사이의 접근에 보안 제한을 적용한다.

---

## BLOCK 51 · same-origin policy

### LESSON 01 · 다른 사이트 데이터를 마음대로 읽지 못하게

악성 사이트가 사용자가 로그인한 은행 사이트 내용을 마음대로 읽을 수 있다면 위험하다.

브라우저는 기본적으로 다른 origin의 자원 접근을 제한한다.

이 보안 규칙을 **same-origin policy**라고 부른다.

---

## BLOCK 52 · CORS

### LESSON 01 · 서버가 허용할 다른 origin을 알려준다

**CORS(Cross-Origin Resource Sharing)**는 서버가 HTTP header를 통해 브라우저에게 어떤 다른 origin의 요청을 허용할지 알려주는 규칙이다.

### LESSON 02 · CORS 오류 = 서버 다운이 아니다

서버가 정상 응답했어도 브라우저가 JavaScript에서 그 응답을 읽지 못하게 차단할 수 있다.

```text
서버 연결 실패
≠
브라우저 CORS 차단
```

---

## BLOCK 53 · preflight

### LESSON 01 · 실제 요청 전에 허용 여부 확인

브라우저는 일부 cross-origin 요청 전에 `OPTIONS` 요청을 먼저 보내 서버가 허용하는지 확인한다.

이를 **preflight request**라고 부른다.

### LESSON 02 · 왜 갑자기 OPTIONS가 보이나

개발자가 POST만 보냈다고 생각해도 브라우저가 먼저 OPTIONS를 보낼 수 있다.

Network 패널에서 이 요청이 실패하면 실제 API 요청이 보내지지 않을 수 있다.

---

## BLOCK 54 · cache

### LESSON 01 · 같은 데이터를 매번 받지 않기

브라우저와 중간 서버는 같은 자원을 다시 사용하기 위해 **cache**에 저장할 수 있다.

### LESSON 02 · Cache-Control

```http
Cache-Control: max-age=3600
```

같은 header로 캐시 사용 규칙을 전달할 수 있다.

### LESSON 03 · 오래된 데이터 문제

서버는 바뀌었는데 브라우저가 오래된 cache를 보여 줄 수 있다.

`서버 코드가 배포됐는데 화면이 예전이다`라는 증상에서 cache도 확인한다.

---

## BLOCK 55 · ETag

### LESSON 01 · 자원이 바뀌었는지 식별

서버는 응답에 **ETag**라는 식별값을 붙일 수 있다.

클라이언트가 다음 요청에 이전 ETag를 보내 `바뀌었나요?`라고 물을 수 있다.

### LESSON 02 · 304

자원이 바뀌지 않았다면 서버는 `304 Not Modified`를 보내 body를 다시 보내지 않을 수 있다.

네트워크 사용량을 줄인다.

---

## BLOCK 56 · redirect

### LESSON 01 · 다른 위치로 가라고 알려주기

서버가 `이 자원은 다른 URL로 이동했습니다`라고 응답할 수 있다.

이것을 **redirect**라고 부른다.

```text
301 Moved Permanently
302 Found
307 Temporary Redirect
308 Permanent Redirect
```

메서드 유지 규칙에 차이가 있어 무조건 같은 것으로 보지 않는다.

---

## BLOCK 57 · WebSocket

### LESSON 01 · 서버와 지속 연결

HTTP 요청마다 요청→응답을 반복하는 대신 하나의 연결을 유지하며 양쪽이 데이터를 주고받을 수 있는 프로토콜이 **WebSocket**이다.

### LESSON 02 · 어디에 쓰나

```text
실시간 채팅
주가 스트리밍
온라인 게임 상태
협업 문서
```

처럼 실시간 양방향 통신에 유용하다.

---

## BLOCK 58 · polling과 SSE

### LESSON 01 · polling

클라이언트가 일정 시간마다 서버에 `새 데이터 있나요?`라고 요청하는 방식을 **polling**이라고 부른다.

### LESSON 02 · Server-Sent Events

서버가 하나의 HTTP 연결을 유지하며 클라이언트 방향으로 이벤트를 계속 보내는 기술을 **SSE(Server-Sent Events)**라고 부른다.

양방향이 꼭 필요하지 않다면 WebSocket보다 단순할 수 있다.

---

## BLOCK 59 · network error를 층으로 나누기

### LESSON 01 · HTTP status가 없을 수 있다

DNS 실패나 TCP 연결 실패라면 HTTP 서버까지 도달하지 못했다.

따라서 `500인가?`를 물을 수 없다.

### LESSON 02 · 진단 순서

```text
1. 기기 네트워크 연결?
2. DNS lookup 성공?
3. IP/port 연결?
4. TLS handshake 성공?
5. HTTP 요청 전송?
6. status code?
7. response headers/body?
8. 브라우저라면 CORS?
```

층을 하나씩 확인한다.

---

## BLOCK 60 · fetch로 API 호출하기

### LESSON 01 · GET

```javascript
const response = await fetch("/api/users/10");
```

이 단계에서 `response`는 HTTP 응답 정보를 가진 객체다.

### LESSON 02 · JSON body 읽기

```javascript
const data = await response.json();
```

응답 body를 JSON으로 해석한다.

### LESSON 03 · status 확인

fetch는 404나 500에서도 Promise가 자동 reject되지 않는다.

따라서:

```javascript
if (!response.ok) {
  throw new Error(`HTTP ${response.status}`);
}
```

처럼 상태를 직접 확인한다.

---

## BLOCK 61 · API 오류 응답 설계

### LESSON 01 · 사람에게도 프로그램에게도 이해 가능한 오류

나쁜 응답:

```json
{"error":"fail"}
```

좋은 방향:

```json
{
  "code": "INVALID_EMAIL",
  "message": "이메일 형식이 올바르지 않습니다",
  "field": "email"
}
```

### LESSON 02 · 내부 정보 노출 금지

사용자에게 DB password, stack trace, 내부 서버 경로를 그대로 보내면 보안 정보가 노출될 수 있다.

외부 오류와 내부 로그를 구분한다.

---

## BLOCK 62 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| network | 장치들이 데이터를 주고받을 수 있게 연결된 구조 |
| Internet | 전 세계 네트워크를 연결한 거대한 연결망 |
| client | 기능이나 데이터를 요청하는 쪽 |
| server | 요청을 처리하고 결과를 제공하는 쪽 |
| IP address | 네트워크에서 목적지를 찾는 숫자 주소 |
| DNS | 도메인 이름을 IP 주소 정보와 연결해 찾는 시스템 |
| port | 한 장치 안에서 네트워크 서비스를 구분하는 번호 |
| protocol | 통신을 위한 규칙 |
| packet | 네트워크로 전달되는 데이터 단위 |
| TCP | 순서와 전달 확인을 제공하는 연결형 전송 프로토콜 |
| UDP | 연결·재전송을 단순화한 데이터그램 전송 프로토콜 |
| latency | 요청 후 결과가 오기까지 걸리는 시간 |
| bandwidth | 일정 시간에 전송할 수 있는 데이터 양 |
| TLS | 통신 암호화와 상대 인증에 쓰는 보안 프로토콜 |
| HTTP | 웹 요청과 응답의 규칙 |
| URL | 인터넷 자원의 위치 표현 |
| request | 클라이언트가 서버에 보내는 메시지 |
| response | 서버가 돌려주는 결과 메시지 |
| status code | HTTP 처리 결과를 나타내는 숫자 |
| header | HTTP 메시지의 추가 정보 |
| body | HTTP 메시지의 실제 내용 데이터 |
| MIME type | 데이터 종류를 나타내는 표준 종류표 |
| JSON | 구조화된 데이터를 문자열로 표현하는 형식 |
| Content-Type | body의 MIME type을 알려주는 header |
| Accept | 받고 싶은 response MIME type을 알려주는 header |
| multipart/form-data | body를 여러 조각으로 나누어 폼 값과 파일을 보내는 형식 |
| boundary | multipart의 각 조각을 나누는 경계 문자열 |
| API | 프로그램이 다른 기능·데이터를 사용하는 접점과 규칙 |
| REST | HTTP와 자원 개념을 이용하는 API 설계 스타일 |
| authentication | 사용자가 누구인지 확인하는 것 |
| authorization | 확인된 사용자가 어떤 작업을 할 수 있는지 판단하는 것 |
| cookie | 브라우저가 사이트 값을 저장하고 요청에 보낼 수 있는 기능 |
| session | 서버에서 사용자 상태를 일정 기간 관리하는 방식 |
| token | 인증이나 권한 확인에 사용하는 증표 값 |
| origin | scheme+host+port로 구분하는 웹 출처 |
| CORS | 다른 origin 요청을 허용할 범위를 알리는 HTTP 규칙 |
| cache | 다시 사용할 데이터를 임시 저장해 비용을 줄이는 것 |
| WebSocket | 하나의 연결로 양방향 실시간 데이터를 주고받는 프로토콜 |

---

## BLOCK 63 · TRACK 06 완료 기준

다음을 직접 설명·실행할 수 있어야 한다.

- Wi-Fi와 인터넷 연결을 구분한다.
- IP/domain/DNS/port 역할을 설명한다.
- TCP와 UDP의 큰 차이를 설명한다.
- latency와 bandwidth를 구분한다.
- TLS와 HTTPS 관계를 설명한다.
- URL을 scheme/host/port/path/query로 나눈다.
- HTTP request/response를 읽는다.
- GET/POST/PUT/PATCH/DELETE 의도를 설명한다.
- status code 2xx/4xx/5xx 범주를 구분한다.
- header와 body를 구분한다.
- MIME type이 왜 필요한지 어린 학생에게 설명한다.
- Content-Type과 Accept를 구분한다.
- multipart/form-data와 boundary를 실제 요청에서 읽는다.
- API와 HTTP, REST, JSON을 서로 다른 개념으로 설명한다.
- authentication과 authorization을 구분한다.
- cookie/session/token의 역할을 구분한다.
- origin과 CORS를 설명한다.
- Network 패널에서 DNS 이후 HTTP 요청·응답을 확인한다.

### TRACK 프로젝트 · 파일 업로드 API 조사기

프론트엔드에서 프로필 사진과 닉네임을 서버로 보내는 기능을 만든다.

필수 과정:

1. `FormData`에 nickname과 image file을 넣는다.
2. fetch로 POST 요청을 보낸다.
3. Network 패널에서 실제 request method와 URL을 확인한다.
4. `Content-Type: multipart/form-data; boundary=...`를 찾는다.
5. request body가 여러 part로 나뉘는지 확인한다.
6. 서버가 JSON으로 응답하게 한다.
7. response `Content-Type: application/json`을 확인한다.
8. 이미지가 아닌 파일을 보내 실패시키고 status code와 오류 body를 기록한다.
9. CORS 설정을 일부러 틀리게 만들어 브라우저 차단을 재현한다.
10. 실제 서버 장애와 CORS 차단을 구분해 설명한다.

이 프로젝트를 끝낸 뒤 `MIME`, `Content-Type`, `boundary`가 더 이상 외계어처럼 보이지 않아야 한다.
