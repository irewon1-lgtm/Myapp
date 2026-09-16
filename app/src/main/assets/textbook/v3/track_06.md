# TRACK 06 · 인터넷·네트워크·API

웹이나 앱에서 데이터가 안 오면 초보자는 흔히 이렇게 생각한다.

> 서버가 고장 났나?

하지만 실제 통신은 여러 단계를 지난다.

```text
내 기기
↓
Wi-Fi/모바일 네트워크
↓
도메인 이름을 주소로 찾기
↓
서버의 IP와 port로 연결
↓
필요하면 TLS로 안전한 통신 준비
↓
HTTP 요청
↓
서버의 HTTP 응답
↓
body 데이터 형식 해석
↓
브라우저 보안 정책 확인
↓
앱 화면에 사용
```

어느 단계가 실패했는지 구분할 수 있어야 `네트워크가 안 돼요`를 실제 문제로 좁힐 수 있다.

이번 TRACK에서 MIME, Content-Type, boundary 같은 말도 정의부터 외우지 않는다. 먼저 **받은 데이터가 무엇인지 어떻게 알까?**, **사진과 글자를 한 요청에 어떻게 같이 보낼까?** 같은 실제 문제에서 출발한다.

---

## BLOCK 01 · 내 휴대폰에서 인터넷 서버까지는 어떤 길이 있는가

### LESSON 01 · network·client/server·IP·private/public IP·NAT를 집 공유기에서 시작한다

#### 1. 네트워크는 장치끼리 데이터를 주고받을 수 있게 연결한 구조다

집에 다음 장치가 있다고 하자.

```text
휴대폰
태블릿
노트북
TV
공유기
```

각 장치가 공유기와 연결되어 데이터를 주고받을 수 있다.

이것도 하나의 작은 **network(네트워크)**다.

네트워크라고 하면 인터넷 전체부터 생각하기 쉽지만, 회사 내부망이나 집 안의 Wi-Fi도 네트워크다.

#### 2. Wi-Fi 아이콘이 있다고 인터넷 전체가 정상인 것은 아니다

휴대폰과 공유기 연결은 정상인데 공유기가 외부 인터넷과 연결되지 못할 수 있다.

```text
휴대폰 ── 정상 ── 공유기 ── 장애 ── 인터넷
```

이 경우 휴대폰에는 Wi-Fi 표시가 있어도 웹사이트가 안 열릴 수 있다.

따라서:

```text
Wi-Fi 연결됨
=
인터넷의 모든 단계 정상
```

이 아니다.

문제를 층별로 보는 첫 예다.

#### 3. client와 server는 요청하는 쪽과 제공하는 쪽의 역할이다

브라우저가 사용자 정보를 요청한다.

```text
브라우저
→ 사용자 데이터 주세요
서버
→ 여기 있습니다
```

요청하는 쪽을 **client(클라이언트)**, 요청을 받아 처리하고 결과를 주는 쪽을 **server(서버)**라고 부른다.

휴대폰 자체가 항상 client이고 큰 컴퓨터가 항상 server라는 뜻은 아니다.

역할의 이름이다.

같은 컴퓨터에서도 프로그램 A가 다른 프로그램 B에게 요청하면 문맥에 따라 client/server 역할을 나눌 수 있다.

#### 4. 서버까지 데이터를 보내려면 주소가 필요하다

택배를 보내려면 목적지 정보가 필요하다.

네트워크에서도 데이터를 어디로 보낼지 식별할 주소가 필요하다.

여기서 **IP address(IP 주소)**가 나온다.

예시 IPv4 주소:

```text
192.0.2.10
```

초급에서는:

> IP 주소 = IP 네트워크에서 데이터를 어느 목적지로 전달할지 찾는 데 사용하는 숫자 주소

라고 이해한다.

#### 5. IPv4와 IPv6가 왜 두 종류인가

IPv4는 32bit 주소 공간을 사용한다.

인터넷 장치가 폭발적으로 늘면서 더 큰 주소 공간이 필요해졌다.

IPv6는 128bit 주소 공간을 사용한다.

예시:

```text
2001:db8::1
```

지금 주소 모양을 외울 필요는 없다.

```text
IPv4 = 오래되고 매우 널리 쓰이는 작은 주소 공간
IPv6 = 훨씬 큰 주소 공간을 가진 새 세대 IP
```

정도로 역할을 잡는다.

#### 6. 집 안 IP와 인터넷에서 보이는 IP가 다를 수 있다

집 Wi-Fi에 연결한 휴대폰이:

```text
192.168.0.20
```

같은 주소를 가질 수 있다.

이런 주소 대역은 내부 네트워크에서 쓰는 **private IP(사설 IP)**다.

인터넷 전체에서 직접 라우팅되는 **public IP(공인 IP)**와 역할이 다르다.

집 안 여러 장치는 각자 사설 IP를 가지고, 공유기는 외부로 나갈 때 하나의 공인 IP를 사용할 수 있다.

#### 7. NAT는 내부 주소와 외부 통신 사이를 연결한다

공유기 뒤에:

```text
휴대폰 192.168.0.10
노트북 192.168.0.11
TV     192.168.0.12
```

가 있다.

외부 인터넷에서는 하나의 공인 IP를 공유할 수 있다.

공유기는 어떤 내부 장치가 시작한 통신인지 기억하고 외부 응답을 알맞은 내부 장치로 돌려준다.

이런 주소 변환과 연결에 **NAT(Network Address Translation)**가 사용된다.

아주 단순화하면:

```text
내부 여러 private IP
↓ 공유기 NAT
외부 public IP 통신
```

이다.

#### 8. 왜 집 PC 서버가 밖에서 바로 안 보일 수 있을까

내 PC가 `192.168.0.11`이고 서버를 3000 port로 열었다고 하자.

집 안 다른 장치에서는 접근 가능할 수 있다.

하지만 외부 인터넷의 누군가가 `192.168.0.11`로 직접 접근할 수는 없다. private 주소는 인터넷 전체에서 직접 라우팅하는 주소가 아니기 때문이다.

NAT 뒤의 내부 서버에 외부에서 접근하려면 port forwarding이나 별도 터널/공인 서버 같은 구성이 필요할 수 있다.

#### 9. IP 주소는 사람의 정확한 위치 주소와 같은 것이 아니다

IP를 `집 주소`라고 비유할 수 있지만 실제로는 차이가 많다.

```text
공유 NAT
통신사 네트워크
VPN
프록시
동적 IP
모바일 네트워크
```

등이 있으므로 IP 하나만 보고 개인의 정확한 물리적 위치를 항상 알 수 있는 것은 아니다.

#### 10. 실제 오류를 층으로 나눈다

증상:

```text
앱에서 서버 연결 실패
```

질문:

```text
휴대폰은 공유기에 연결됐나?
다른 사이트는 열리나?
서버 IP로 도달 가능한가?
서버가 실제 실행 중인가?
올바른 port를 듣고 있나?
방화벽이 막고 있나?
```

`서버 코드 버그`를 보기 전에 그 아래 연결부터 확인할 수 있다.

#### 11. 책을 덮고 확인한다

1. network와 Internet은 어떤 관계인가?
2. client와 server를 앱 API 예로 설명하라.
3. IP address가 필요한 이유는 무엇인가?
4. private IP와 public IP가 다른 이유를 집 공유기 예로 설명하라.
5. NAT가 어떤 문제를 해결하는지 그림으로 설명하라.
6. Wi-Fi 아이콘이 있어도 인터넷이 안 될 수 있는 이유는 무엇인가?

---

## BLOCK 02 · 사람이 쓰는 이름에서 실제 서버의 서비스까지 찾아간다

### LESSON 01 · domain·DNS·port·protocol·packet·TCP/UDP·latency/bandwidth를 한 요청의 이동 경로로 배운다

#### 1. 사람은 숫자 IP보다 이름을 기억하기 쉽다

웹사이트를 열 때 우리는 보통:

```text
example.com
```

같은 이름을 입력한다.

이런 이름을 **domain name(도메인 이름)**이라고 부른다.

하지만 실제 IP 네트워크에서는 목적지 IP 정보가 필요하다.

도메인 이름을 IP 정보와 연결해 찾는 시스템이 필요하다.

#### 2. DNS는 인터넷의 이름 찾기 시스템이다

**DNS(Domain Name System)**는 도메인 이름과 관련된 IP 주소 등 정보를 찾을 수 있게 하는 분산된 이름 시스템이다.

비유:

```text
사람 이름 → 전화번호부 → 전화번호
도메인 → DNS → IP 정보
```

비유가 완전히 동일한 시스템이라는 뜻은 아니다.

#### 3. DNS lookup의 큰 흐름을 본다

브라우저에:

```text
https://api.example.com/users
```

를 입력한다.

대략:

```text
api.example.com의 주소 정보가 이미 cache에 있나?
↓ 없으면 DNS 질의
DNS resolver가 필요한 서버들을 통해 조회
↓
A/AAAA 등 record 결과
↓
IP 주소를 얻음
```

실제 DNS에는 resolver, root, TLD, authoritative server, caching 같은 더 많은 요소가 있다.

초급에서는 `이름을 주소 정보로 바꾸는 단계가 HTTP보다 앞에 있을 수 있다`는 것이 중요하다.

DNS가 실패하면 HTTP 500조차 받을 수 없다. HTTP 서버에 아직 도달하지 못했기 때문이다.

#### 4. A, AAAA, CNAME을 역할로 본다

대표 DNS record:

```text
A
→ 이름을 IPv4 주소와 연결

AAAA
→ 이름을 IPv6 주소와 연결

CNAME
→ 한 이름을 다른 도메인 이름의 별칭으로 연결
```

예:

```text
www.example.com → example-host.provider.com
```

같은 별칭 관계를 만들 수 있다.

#### 5. IP로 컴퓨터까지 왔는데 어느 프로그램에 전달할까

서버 한 대에서 여러 서비스가 실행될 수 있다.

```text
웹 서버
DB
관리 도구
게임 서버
```

어느 네트워크 서비스와 통신할지 구분하는 번호가 **port(포트)**다.

초보 비유:

```text
IP = 건물 주소
port = 건물 안의 창구 번호
```

HTTP는 기본 80, HTTPS는 기본 443 port를 많이 사용한다.

Node.js 개발 서버는 3000 같은 다른 port를 자주 사용한다.

#### 6. port 번호가 열려 있다는 것과 앱이 정상이라는 것은 다르다

`443 port 연결 성공`은 그 지점에서 서버가 통신을 받을 수 있다는 증거다.

그 뒤 HTTP endpoint가 500을 보낼 수도 있다.

반대로 port 연결 자체가 실패하면 HTTP 응답을 받을 단계까지 못 간 것이다.

따라서:

```text
TCP connection refused
```

와:

```text
HTTP 500
```

은 완전히 다른 층의 문제다.

#### 7. protocol은 서로 통신하기 위한 규칙이다

컴퓨터 둘이 데이터를 주고받으려면:

```text
어떻게 연결할지
데이터를 어떤 단위로 보낼지
누락을 어떻게 다룰지
요청/응답을 어떤 모양으로 쓸지
```

등에 대한 공통 규칙이 필요하다.

이런 통신 규칙을 **protocol(프로토콜)**이라고 부른다.

웹 한 번에도 여러 protocol이 층으로 협력한다.

```text
IP
TCP 또는 QUIC
TLS
HTTP
```

서로 같은 일을 하는 것이 아니다.

#### 8. 네트워크 데이터는 큰 파일 하나가 통째로 순간이동하지 않는다

100MB 영상을 보낸다고 해서 하나의 거대한 덩어리가 선로를 통째로 차지하는 것은 아니다.

네트워크 계층에서는 데이터를 작은 단위로 나눠 전달한다.

넓은 의미에서 이런 전송 단위를 **packet(패킷)**이라고 부른다.

각 계층에서는 header 같은 정보를 붙여 목적지, 순서, protocol 정보를 전달한다.

#### 9. TCP는 신뢰성 있는 byte stream을 제공한다

**TCP(Transmission Control Protocol)**는 두 endpoint 사이에 신뢰성 있는 byte stream 통신을 제공하는 전송 protocol이다.

초급 감각:

```text
순서대로 전달
누락되면 재전송 시도
중복/순서 문제 처리
흐름 제어
혼잡 제어
```

같은 기능을 제공한다.

`TCP면 절대 데이터 손실이 없다`라는 식으로 물리 네트워크를 과장해서 이해하지 않는다. 애플리케이션에게 신뢰성 있는 stream을 제공하려고 재전송과 순서 제어 등을 수행한다.

#### 10. TCP connection을 만들 때 handshake가 있다

대표적인 3-way handshake:

```text
client → SYN
server → SYN-ACK
client → ACK
```

세 문자를 외우는 것이 목표가 아니다.

중요한 것은:

> HTTP 데이터를 보내기 전에 전송 계층 연결 자체를 만드는 과정이 필요할 수 있다.

는 것이다.

#### 11. UDP는 TCP보다 기능이 적다고 "나쁜 protocol"이 아니다

**UDP(User Datagram Protocol)**는 TCP처럼 연결 상태, 순서 보장, 재전송을 기본 제공하지 않는 더 단순한 datagram protocol이다.

그 대신 overhead가 작고 application이 필요한 방식을 직접 설계할 수 있다.

실시간 게임, 음성/영상 통신 같은 환경에서는 일부 packet 손실보다 지연이 더 중요할 수 있다.

목적에 따라 선택한다.

#### 12. HTTP/3에서 QUIC 같은 말이 나오는 이유

현대 HTTP/3는 TCP 대신 UDP 위에서 동작하는 QUIC을 사용한다.

이것은 `UDP는 웹에서 절대 쓰지 않는다` 같은 단순 암기를 피해야 하는 좋은 예다.

기술 계층은 발전한다.

우리는 각 protocol의 역할을 이해하고 현재 표준을 확인하는 습관을 가져야 한다.

#### 13. latency와 bandwidth는 서로 다른 문제다

**latency(지연시간)**는 요청 후 첫 응답을 받기까지의 지연과 관련된 개념이다.

**bandwidth(대역폭)**는 일정 시간 동안 얼마나 많은 데이터를 전송할 수 있는지에 관한 능력이다.

비유:

```text
latency = 첫 트럭이 목적지까지 가는 시간
bandwidth = 한 시간에 몇 대의 트럭이 지나갈 수 있는가
```

고속 인터넷인데 해외 서버 첫 반응이 늦을 수 있다. bandwidth는 큰데 latency가 클 수 있기 때문이다.

#### 14. 작은 API 요청과 대용량 다운로드는 병목이 다를 수 있다

작은 JSON API:

```text
파일 크기 2KB
서버까지 왕복 지연 300ms
```

이라면 bandwidth보다 latency가 체감에 더 중요할 수 있다.

10GB 파일 다운로드라면 충분한 bandwidth가 중요하다.

#### 15. packet loss가 생기면 TCP에서도 체감 지연이 커질 수 있다

packet이 손실돼 재전송이 필요하면 데이터 전달이 늦어진다.

따라서 단순히 `다운로드 속도 숫자`만으로 실제 앱 응답성을 설명할 수 없다.

#### 16. 진단 예제를 층으로 분해한다

증상:

```text
https://api.example.com 접속 안 됨
```

확인 순서 후보:

```text
1. DNS가 api.example.com을 주소로 찾는가?
2. 해당 IP/port 연결이 가능한가?
3. TCP/QUIC 단계가 성공하는가?
4. TLS가 성공하는가?
5. HTTP 응답이 오는가?
```

서버 404를 받았다면 DNS와 연결/TLS는 이미 상당 부분 성공한 것이다.

#### 17. 책을 덮고 확인한다

1. domain과 IP는 어떤 관계인가?
2. DNS 실패와 HTTP 500은 왜 다른 단계인가?
3. port가 필요한 이유를 한 서버에 여러 프로그램이 있는 예로 설명하라.
4. TCP와 UDP의 큰 차이를 설명하라.
5. latency와 bandwidth를 작은 API/대용량 파일 예로 구분하라.
6. HTTPS 요청 전에 어떤 네트워크 단계가 있을 수 있는지 큰 순서로 말하라.

---

## BLOCK 03 · 서버와 안전하게 연결하고 HTTP 요청을 시작한다

### LESSON 01 · TLS·certificate·HTTP/HTTPS·URL을 "주소창에 URL 입력" 한 번으로 따라간다

#### 1. 인터넷 중간에서 비밀번호가 보이면 안 된다

로그인 요청:

```text
email=user@example.com
password=my-secret
```

이 데이터가 인터넷을 지나간다.

누군가 중간에서 평문을 읽거나 바꿀 수 있다면 매우 위험하다.

그래서 통신 내용을 암호화하고 상대 서버의 신원을 확인하는 보안 계층이 필요하다.

#### 2. TLS는 안전한 통신 채널을 만드는 protocol이다

**TLS(Transport Layer Security)**는 인터넷 통신의 기밀성과 무결성을 보호하고 상대를 인증하는 데 사용하는 보안 protocol이다.

초급에서는:

```text
중간에서 내용을 쉽게 읽지 못하도록 암호화
전송 중 바뀌었는지 보호
내가 연결하려는 서버인지 확인하는 과정 제공
```

을 핵심으로 이해한다.

암호학 세부는 TRACK 09에서 다시 배운다.

#### 3. certificate는 서버가 "나는 이 도메인의 서버다"를 증명하는 과정에 사용된다

TLS 연결에서 서버는 **digital certificate(디지털 인증서)**를 제시한다.

인증서에는:

```text
도메인 정보
공개키 정보
유효기간
발급/서명 관련 정보
```

등이 들어 있다.

브라우저는:

```text
인증서가 신뢰 가능한 chain인가?
접속한 domain과 인증서가 맞는가?
유효기간이 지나지 않았는가?
```

등을 검사한다.

#### 4. 자가서명 인증서 경고를 무조건 "해킹"이라고 단정하지 않는다

개발 환경에서는 self-signed certificate를 사용할 수 있다.

브라우저는 기본적으로 신뢰 chain을 확인할 수 없어 경고할 수 있다.

반대로 운영 서비스에서 인증서가 잘못됐는데 경고를 무시하는 것도 위험하다.

문맥을 보고 판단한다.

#### 5. HTTP는 웹 요청과 응답의 규칙이다

**HTTP(Hypertext Transfer Protocol)**는 client와 server가 웹 자원에 대한 request와 response를 주고받는 규칙이다.

HTTP가 하는 질문:

```text
어떤 자원을 요청하는가?
어떤 method인가?
추가 header는 무엇인가?
body가 있는가?
처리 결과 status는 무엇인가?
```

#### 6. HTTPS는 HTTP를 TLS로 보호해 사용하는 형태다

```text
HTTP
+
TLS 보안 채널
=
HTTPS 통신
```

하지만 HTTPS 주소라고 사이트 내용 자체가 무조건 믿을 만하다는 뜻은 아니다.

공격자도 자신의 domain에 정상 인증서를 받을 수 있다.

HTTPS가 증명하는 것은 주로 **그 domain과의 통신 보호 및 인증**이지 사이트 사업자가 선하다는 보증이 아니다.

#### 7. URL을 하나씩 분해한다

```text
https://api.example.com:443/users/10?active=true#profile
```

나눈다.

```text
https
→ scheme

api.example.com
→ host

443
→ port

/users/10
→ path

active=true
→ query string

profile
→ fragment
```

각 부분이 역할이 다르다.

#### 8. scheme은 어떤 방식으로 접근할지 나타낸다

```text
https://
```

의 `https`가 scheme이다.

브라우저는 scheme을 보고 어떤 방식으로 자원에 접근할지 판단한다.

`http`, `https`, `file` 같은 scheme이 있다.

#### 9. host는 요청할 서버 위치를 나타낸다

```text
api.example.com
```

이 host를 DNS가 IP 정보로 찾는다.

host에는 domain name이나 IP 주소가 사용될 수 있다.

#### 10. path는 서버 안의 자원/endpoint 경로로 사용된다

```text
/users/10
```

API에서 10번 사용자와 관련된 자원 경로로 설계할 수 있다.

하지만 path 자체가 자동으로 파일 시스템 경로라는 뜻은 아니다. 서버 routing 규칙에 따라 코드가 처리한다.

#### 11. query string은 추가 조건을 URL에 표현한다

```text
/products?page=2&sort=price
```

`?` 뒤가 query string이다.

```text
page=2
sort=price
```

같은 parameter를 전달한다.

페이지 번호, 검색어, 필터 등에 많이 사용한다.

#### 12. 비밀 정보를 URL query에 넣지 않는다

URL은:

```text
브라우저 방문 기록
서버 access log
프록시 log
분석 도구
Referer 관련 노출 가능성
```

등 여러 곳에 남을 수 있다.

비밀번호, secret token 같은 민감값을 query에 넣는 설계는 피한다.

#### 13. fragment는 보통 HTTP request로 server에 전송되지 않는다

```text
#profile
```

부분은 client 쪽 문서 위치나 SPA routing 등에 사용할 수 있다.

일반적인 HTTP request에서 server로 보내는 URL에는 fragment가 포함되지 않는다.

#### 14. 주소창 입력부터 HTTP 전까지 연결한다

```text
URL 입력
↓
host 확인
↓
DNS 조회
↓
IP/port 연결
↓
TLS handshake (HTTPS)
↓
HTTP request 전송
↓
HTTP response
```

이 큰 흐름을 그릴 수 있어야 문제를 층별로 진단할 수 있다.

#### 15. 인증서 오류와 HTTP status를 구분한다

TLS handshake 단계에서 인증서 검증이 실패하면 HTTP request 자체가 보내지지 않을 수 있다.

따라서 `인증서 오류인데 HTTP 500이 뭐지?`라고 묻기 전에 어느 단계에서 실패했는지 본다.

#### 16. 책을 덮고 확인한다

1. TLS가 해결하려는 큰 문제 세 가지를 말하라.
2. certificate 검증에서 domain이 왜 중요한가?
3. HTTP와 HTTPS의 관계를 설명하라.
4. URL을 scheme/host/port/path/query/fragment로 나눠 보라.
5. 비밀번호를 query string에 넣으면 왜 위험할 수 있는가?
6. URL 입력 후 HTTP request가 나가기 전 큰 단계를 순서대로 말하라.

---

## BLOCK 04 · HTTP 요청과 응답을 실제 메시지처럼 읽는다

### LESSON 01 · request/response·method·status·header·body·safe/idempotent를 한 번의 주문 API로 이해한다

#### 1. HTTP는 "요청"과 "응답"의 대화다

client:

```text
10번 사용자의 정보를 주세요.
```

server:

```text
성공했습니다. 여기 데이터입니다.
```

HTTP에서는 이 대화를 정해진 메시지 형식으로 표현한다.

client가 보내는 것을 **request**, server가 돌려주는 것을 **response**라고 부른다.

#### 2. request에는 무엇이 들어갈까

단순화:

```text
method
path/URL
headers
body(필요한 경우)
```

예:

```http
GET /users/10 HTTP/1.1
Host: api.example.com
Accept: application/json
```

body가 없는 GET 요청의 예다.

#### 3. response에는 무엇이 들어갈까

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"id":10,"name":"민수"}
```

나눈다.

```text
200 OK
→ 처리 결과 status

Content-Type
→ body 형식 정보

{"id":...}
→ 실제 body 데이터
```

#### 4. HTTP method는 요청 의도를 표현한다

대표:

```text
GET
→ 자원 조회

POST
→ 새 작업/자원 생성에 많이 사용

PUT
→ 자원 전체 교체 의미로 설계하는 경우가 많음

PATCH
→ 일부 변경 의미로 많이 사용

DELETE
→ 삭제 요청 의도
```

method 이름을 썼다고 server가 자동으로 올바른 의미를 지키는 것은 아니다. API 구현이 규칙을 지켜야 한다.

#### 5. GET에 삭제 기능을 넣으면 왜 좋지 않을까

```text
GET /delete-user?id=10
```

같은 설계를 하면 browser prefetch, crawler, cache 등 GET을 `안전한 조회`라고 기대하는 시스템과 충돌할 수 있다.

HTTP 의미를 맞추는 것은 단순 취향 문제가 아니다.

#### 6. status code는 response 처리 결과의 큰 종류를 알려 준다

2xx:

```text
200 OK
201 Created
204 No Content
```

성공 범주.

3xx:

```text
301
302
307
308
```

redirect와 caching 관련 상태 등.

4xx:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
429 Too Many Requests
```

client request나 권한/상태 등과 관련한 문제 범주.

5xx:

```text
500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
```

server가 처리 과정에서 요청을 정상 완료하지 못한 범주.

#### 7. 401과 403을 구분한다

초급용:

```text
401
→ 인증 정보가 없거나 유효하지 않아 사용자를 확인할 수 없음

403
→ 사용자가 누군지는 알지만 이 작업을 할 권한이 없음
```

예:

```text
로그인 안 한 사용자가 관리자 API 호출 → 401
로그인한 일반 사용자가 관리자 API 호출 → 403
```

실제 API의 인증 방식에 따라 응답 설계 세부는 달라질 수 있다.

#### 8. header는 메시지에 대한 추가 정보다

예:

```http
Content-Type: application/json
Authorization: Bearer abc...
Cache-Control: no-cache
Accept-Language: ko
```

header에는 body 형식, 인증, cache 정책, 언어 선호 등 메시지 처리에 필요한 메타정보가 들어간다.

#### 9. body는 실제 주요 데이터 내용이다

POST:

```http
POST /users HTTP/1.1
Content-Type: application/json

{"name":"민수"}
```

아래 JSON이 body다.

모든 request/response가 body를 가져야 하는 것은 아니다.

204 No Content처럼 body가 없는 성공 응답도 있다.

#### 10. safe method는 server 상태 변경을 의도하지 않는다

HTTP에서 GET, HEAD 같은 method는 **safe**한 method로 분류된다.

즉 본래 의미상 server의 상태를 변경하는 작업을 요청하는 것이 아니다.

로그 기록 같은 부수 효과가 전혀 없다는 뜻은 아니지만 사용자가 자원 상태를 변경하는 의미는 아니다.

#### 11. idempotent는 같은 요청을 반복했을 때 최종 상태 효과를 본다

```http
PUT /users/1

{"name":"민수"}
```

같은 요청을 여러 번 보내도 최종 이름은 `민수`다.

이런 성질을 **idempotency(멱등성)**라고 부른다.

DELETE도 같은 자원을 여러 번 삭제 요청해도 `삭제된 상태`라는 최종 효과 관점에서 idempotent로 분류된다. 응답 status가 매번 같아야 한다는 뜻은 아니다.

#### 12. 결제 POST는 반복되면 위험할 수 있다

```http
POST /payments
```

을 네트워크 재시도로 두 번 보내면 실제 결제가 두 번 발생할 수 있다.

이런 작업에서는 별도의 **idempotency key**를 설계해 같은 논리 요청의 중복 처리를 막는 방식이 자주 사용된다.

#### 13. fetch는 404/500에서 자동 reject되지 않는다

JavaScript:

```javascript
const response = await fetch("/api/users/999");
```

서버가 404를 보내도 fetch Promise는 network level에서 정상 응답을 받았으므로 fulfilled될 수 있다.

따라서:

```javascript
if (!response.ok) {
  throw new Error(`HTTP ${response.status}`);
}
```

처럼 status를 확인한다.

`network error`와 `HTTP error response`를 구분한다.

#### 14. Network 패널에서 실제 메시지를 읽는다

DevTools Network에서 API 요청을 클릭한다.

확인:

```text
Request URL
Request Method
Status Code
Request Headers
Request Payload
Response Headers
Response body
Timing
```

추측 대신 실제 값을 본다.

#### 15. 오류를 층으로 분리한다

```text
DNS 실패
→ HTTP status 없음

TCP/TLS 실패
→ HTTP status 없음

HTTP 404
→ server와 HTTP 대화까지 성공했지만 자원을 못 찾음

HTTP 500
→ server가 request를 받았으나 처리 중 실패
```

이 구분이 실제 디버깅의 핵심이다.

#### 16. 책을 덮고 확인한다

1. request와 response에 각각 어떤 정보가 있을 수 있는가?
2. GET/POST/PATCH/DELETE의 대표적인 의도를 말하라.
3. 401과 403을 구분하라.
4. header와 body의 차이는 무엇인가?
5. idempotent가 `응답이 매번 완전히 같음`이라는 뜻이 아닌 이유는 무엇인가?
6. fetch에서 404를 직접 확인해야 할 수 있는 이유는 무엇인가?

---

## BLOCK 05 · 데이터가 무엇인지 알려 주고 여러 종류를 한 요청에 담는다

### LESSON 01 · MIME·JSON·Content-Type·Accept·form·multipart/form-data·boundary를 처음부터 연결한다

#### 1. 데이터 덩어리만 받으면 무엇인지 어떻게 알까

서버가 byte를 보냈다.

그 데이터가:

```text
HTML 문서인가?
JSON 데이터인가?
PNG 이미지인가?
PDF인가?
MP4 영상인가?
ZIP 파일인가?
```

받는 프로그램은 알아야 올바르게 해석할 수 있다.

예를 들어 같은 byte를 HTML parser에 넣을지 이미지 decoder에 넣을지 결정해야 한다.

그래서 인터넷 데이터에는 `이 데이터는 어떤 종류입니다`라는 표준 종류표가 필요하다.

#### 2. MIME type은 데이터의 종류를 나타내는 표준 이름이다

**MIME type**은 데이터의 종류와 형식을 나타내는 표준 이름이다.

아주 쉽게:

> MIME type = 인터넷으로 주고받는 데이터에 붙이는 종류표

대표 예:

```text
text/html
text/plain
image/png
image/jpeg
application/json
application/pdf
```

처음에는 `/` 기준으로 두 덩어리를 본다.

```text
큰 종류 / 세부 형식
```

#### 3. text/html을 글자 그대로 해석한다

```text
text/html
```

```text
text
→ 글자 기반 계열

html
→ HTML 형식
```

즉 `HTML 문서 형식의 텍스트`라는 뜻이다.

#### 4. image/png는 PNG 이미지다

```text
image/png
```

```text
image
→ 이미지 계열

png
→ PNG 형식
```

확장자 `.png`와 관련은 있지만 MIME type과 파일 이름 확장자는 같은 개념은 아니다.

파일 이름을 `.txt`로 바꿔도 실제 PNG byte가 텍스트로 변하지 않는다.

#### 5. application/json은 프로그램용 JSON 데이터에 사용한다

JSON:

```json
{
  "name": "민수",
  "age": 20,
  "skills": ["HTML", "CSS"]
}
```

이 형식을 HTTP에서 표현하는 대표 MIME type이:

```text
application/json
```

이다.

`application`이라는 큰 종류 이름이 처음에는 직관적이지 않아도 된다. `JSON 데이터의 표준 MIME type은 application/json`이라고 실제 요청에서 계속 보며 익힌다.

#### 6. JSON과 JavaScript object는 같은 것이 아니다

JavaScript object:

```javascript
const user = {
  name: "민수"
};
```

JSON text:

```json
{"name":"민수"}
```

JSON은 **문자열 데이터 형식**이다.

JavaScript object는 runtime memory에 존재하는 객체다.

변환:

```javascript
const text = JSON.stringify(user);
const object = JSON.parse(text);
```

`stringify`와 `parse`를 이용해 서로 바꿀 수 있다.

#### 7. MIME type을 HTTP 메시지에 어떻게 붙일까 — Content-Type

body에 JSON을 보낸다.

```http
POST /users HTTP/1.1
Content-Type: application/json

{"name":"민수"}
```

**Content-Type**은 현재 HTTP message body가 어떤 MIME type인지 알려주는 header다.

사람말:

```text
Content-Type: application/json
=
이 body는 JSON 데이터입니다.
```

#### 8. body와 Content-Type이 모순되면 문제가 생긴다

실제 body:

```json
{"name":"민수"}
```

header:

```http
Content-Type: image/png
```

받는 server는 `PNG라고 했는데 내용이 JSON 같은데?`라는 상황이 된다.

framework가 올바른 parser를 선택하지 못하거나 415 Unsupported Media Type 같은 응답을 보낼 수 있다.

Content-Type은 장식이 아니다. **body를 어떻게 해석할지 알려 주는 정보**다.

#### 9. fetch로 JSON을 보낼 때 두 작업이 필요하다

```javascript
await fetch("/users", {
  method: "POST",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    name: "민수"
  })
});
```

나눈다.

```text
JSON.stringify
→ JavaScript object를 JSON text로 바꿈

Content-Type: application/json
→ 이 body가 JSON이라고 server에 알림
```

둘은 서로 다른 역할이다.

#### 10. Accept는 "내가 받고 싶은 response 종류"다

```http
Accept: application/json
```

뜻:

> 나는 response를 application/json 형식으로 받을 수 있거나 선호합니다.

Content-Type과 구분한다.

```text
Content-Type
→ 내가 지금 보내는/받은 실제 body의 종류

Accept
→ client가 response로 받을 수 있거나 원하는 종류
```

#### 11. response에서도 Content-Type을 본다

server:

```http
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8

{"ok":true}
```

client는 response body가 JSON임을 알 수 있다.

`charset=utf-8`은 문자 encoding 정보다. TRACK 01의 UTF-8이 다시 연결된다.

#### 12. 단순 form 데이터는 x-www-form-urlencoded로 보낼 수 있다

HTML form:

```text
name = 민수
age = 20
```

대표적인 form encoding 중 하나:

```text
name=%EB%AF%BC%EC%88%98&age=20
```

MIME type:

```text
application/x-www-form-urlencoded
```

작은 텍스트 form 필드를 보내는 데 사용할 수 있다.

#### 13. 파일과 글자를 한 요청에 같이 보내려면 문제가 생긴다

프로필 등록:

```text
nickname = minsu
photo = me.png
```

nickname은 짧은 text다.

photo는 binary file이다.

body 하나에 여러 종류의 필드와 파일을 나눠 담아야 한다.

이 문제를 해결하는 대표 형식이:

```text
multipart/form-data
```

다.

#### 14. multipart/form-data를 단어부터 나눈다

```text
multi = 여러
part = 부분
form-data = form 데이터
```

즉:

> 하나의 HTTP body를 여러 part로 나눠 text field와 file 등을 함께 담는 형식

이다.

#### 15. 실제 body는 여러 조각으로 나뉜다

단순화한 예:

```text
------ABC
Content-Disposition: form-data; name="nickname"

minsu
------ABC
Content-Disposition: form-data; name="photo"; filename="me.png"
Content-Type: image/png

[PNG binary data]
------ABC--
```

각 part마다 이름, 파일 이름, Content-Type 같은 정보가 있을 수 있다.

#### 16. 그러면 어디서 한 part가 끝나는지 어떻게 알까

body에 여러 데이터 조각이 연속으로 붙어 있다.

server는:

```text
여기까지 nickname
여기부터 photo
```

를 구분해야 한다.

그래서 각 part 사이에 특별한 경계 문자열을 둔다.

이 문자열이 **boundary**다.

#### 17. Content-Type에 boundary도 같이 전달한다

```http
Content-Type: multipart/form-data; boundary=----ABC
```

사람말:

```text
body는 multipart/form-data이고
part들은 ----ABC라는 경계 문자열로 나뉩니다.
```

server는 이 정보를 보고 body를 파싱한다.

#### 18. FormData를 쓸 때 Content-Type을 수동으로 덮어쓰면 왜 문제가 될까

브라우저:

```javascript
const formData = new FormData();
formData.append("nickname", "minsu");
formData.append("photo", file);

await fetch("/profile", {
  method: "POST",
  body: formData
});
```

이 경우 브라우저가 적절한 boundary를 만들고 Content-Type에 포함시킬 수 있다.

초보자가 이렇게 수동 설정한다.

```javascript
headers: {
  "Content-Type": "multipart/form-data"
}
```

그러면 boundary 정보가 빠질 수 있다.

server는 각 part를 어디서 나눌지 모른다.

따라서 FormData를 사용할 때 browser가 Content-Type을 자동 설정하도록 두는 것이 일반적인 패턴이다.

#### 19. Network 패널에서 실제 boundary를 확인한다

파일 업로드 요청을 보내고 DevTools Network에서 request headers를 본다.

```text
Content-Type:
multipart/form-data; boundary=----WebKitFormBoundary...
```

을 찾는다.

Payload 영역에서 field와 file이 실제로 나뉘는지도 본다.

단어를 외우는 것보다 실제 요청에서 보는 것이 훨씬 오래 기억된다.

#### 20. 파일 MIME을 믿기만 하면 보안 문제가 될 수 있다

사용자가 filename을 `cat.png`, Content-Type을 `image/png`라고 보냈다고 해서 실제 파일이 안전한 PNG라는 보장은 없다.

client가 보내는 값은 조작할 수 있다.

server는 파일 크기, 실제 형식, 허용 extension, 저장 위치, 실행 가능성 등을 별도로 검증해야 한다.

보안 TRACK에서 자세히 배운다.

#### 21. 한 번에 연결한다

```text
byte 데이터가 온다
↓
무슨 종류인지 알아야 함
↓
MIME type
↓
HTTP body 종류는 Content-Type header로 전달
↓
JSON이면 application/json
↓
글자와 파일을 함께 보내야 함
↓
multipart/form-data
↓
여러 part를 나눌 기준 필요
↓
boundary
```

이제 MIME, Content-Type, multipart, boundary가 서로 떨어진 암기 단어가 아니다.

#### 22. 책을 덮고 확인한다

1. MIME type이 필요한 이유를 `받은 byte가 PNG인지 JSON인지` 예로 설명하라.
2. `text/html`, `image/png`, `application/json`을 각각 읽어 보라.
3. JSON text와 JavaScript object는 무엇이 다른가?
4. Content-Type과 Accept를 구분하라.
5. multipart/form-data가 필요한 실제 상황을 하나 말하라.
6. boundary가 없으면 server가 무엇을 구분하기 어려운가?
7. FormData에서 Content-Type을 무심코 수동 설정하면 왜 boundary 문제가 생길 수 있는가?

---

## BLOCK 06 · API는 프로그램이 다른 프로그램의 기능을 쓰는 약속이다

### LESSON 01 · API·REST·endpoint·pagination·authentication·cookie/session/token을 사용자 목록 API로 연결한다

#### 1. API를 URL 한 개라고 생각하지 않는다

프론트엔드 앱이 server에게:

```text
사용자 목록 주세요.
10번 사용자 주세요.
새 사용자 만들어 주세요.
```

라고 요청할 수 있다.

server가 이런 기능을 어떤 형식으로 사용할 수 있는지 정한 접점과 규칙을 **API(Application Programming Interface)**라고 부른다.

웹에서는 HTTP를 이용한 API가 매우 흔하다.

#### 2. HTTP와 API는 같은 말이 아니다

```text
HTTP
→ request/response를 주고받는 통신 규칙

API
→ 프로그램의 기능과 데이터를 어떤 방법으로 사용할지 정한 접점/계약
```

HTTP 위에 API를 설계할 수 있다.

API가 반드시 HTTP만 사용해야 하는 것도 아니다.

#### 3. endpoint는 구체적인 호출 지점이다

```text
GET /users
GET /users/10
POST /users
```

method와 path를 조합한 구체적인 API 호출 지점을 **endpoint**라고 부른다.

같은 `/users`라도 GET과 POST는 다른 동작을 가질 수 있다.

#### 4. REST는 자원과 HTTP 의미를 활용하는 API 설계 스타일이다

**REST**는 웹의 resource와 HTTP 의미를 이용하는 architecture style이다.

예:

```text
GET /users/10
→ 사용자 10 조회

POST /users
→ 사용자 생성

PATCH /users/10
→ 사용자 일부 수정

DELETE /users/10
→ 사용자 삭제
```

`REST = JSON`은 아니다.

REST는 설계 스타일이고 JSON은 데이터 형식이다.

#### 5. URL을 동사로 가득 채우지 않는 이유를 본다

```text
/getUser
/createUser
/deleteUser
```

처럼 동사를 path에 넣을 수도 있지만 RESTful 설계에서는:

```text
/users
/users/10
```

같은 resource와 HTTP method를 이용해 의도를 표현하는 방식을 많이 쓴다.

모든 API가 반드시 REST여야 하는 것은 아니다. GraphQL, RPC 등 다른 접근도 있다.

#### 6. 100만 사용자를 한 번에 보내면 문제가 생긴다

```http
GET /users
```

가 100만 row를 모두 JSON으로 보내면:

```text
DB 조회 비용
server memory
network 전송량
client parsing
화면 rendering
```

모두 부담이 커진다.

그래서 데이터를 여러 페이지로 나누는 **pagination**을 사용한다.

#### 7. page/size 방식

```text
GET /users?page=2&size=50
```

한 페이지에 50명씩, 두 번째 페이지를 요청한다.

사용자가 이해하기 쉽지만 내부 DB에서 뒤 페이지가 매우 깊어지면 비용이 커질 수 있다.

#### 8. offset/limit 방식

```text
GET /users?offset=100&limit=50
```

앞에서 100개를 건너뛰고 50개 가져온다.

큰 offset은 DB가 많은 row를 건너뛰어야 해서 느릴 수 있다.

#### 9. cursor pagination은 마지막 위치를 기준으로 다음을 가져온다

response:

```json
{
  "items": [...],
  "nextCursor": "abc123"
}
```

다음 요청:

```text
GET /users?cursor=abc123
```

실시간으로 항목이 계속 추가되는 큰 목록에서 안정적인 경우가 많다.

#### 10. API에 누구나 접근하게 둘 수 없는 기능이 있다

```text
내 주문 조회
결제 취소
관리자 사용자 삭제
```

사용자가 누구인지 확인해야 한다.

이것이 **authentication(인증)**이다.

```text
인증 = 너는 누구인가?
```

#### 11. 인증됐다고 모든 것을 할 수 있는 것은 아니다

일반 사용자와 관리자가 있다.

둘 다 로그인했지만 권한이 다르다.

```text
authorization(인가/권한 확인)
= 이 사용자가 이 작업을 할 수 있는가?
```

TRACK 09에서 더 깊게 배운다.

#### 12. cookie는 browser가 저장하고 조건에 맞는 request에 보낼 수 있는 작은 값이다

server response:

```http
Set-Cookie: sessionId=abc123; HttpOnly; Secure
```

browser가 cookie를 저장하고 같은 site 조건에 맞는 request에 자동으로 보낼 수 있다.

cookie에는:

```text
Secure
HttpOnly
SameSite
Expires/Max-Age
Domain
Path
```

같은 속성이 있다.

#### 13. session은 server 쪽에 로그인 상태를 저장하는 방식이다

server:

```text
session abc123
→ userId 10
→ 로그인됨
```

browser cookie에는 `abc123` 같은 session ID만 있을 수 있다.

request:

```text
cookie의 session ID
↓
server가 session 저장소 조회
↓
user 10임을 확인
```

#### 14. session ID가 유출되면 위험하다

공격자가 유효한 session ID를 훔치면 사용자를 가장할 수 있다.

그래서 cookie의 Secure/HttpOnly/SameSite 설정, session 만료, 재발급 정책 등이 중요하다.

#### 15. token은 인증/권한 증표로 사용할 수 있는 값이다

예:

```http
Authorization: Bearer eyJ...
```

**Bearer token**은 `이 값을 가진 사람이 권한을 행사할 수 있다`는 성격이 강하다.

따라서 노출되면 위험하다.

브라우저 어디에 저장할지, 어떻게 만료할지, 탈취에 어떻게 대응할지 설계해야 한다.

#### 16. JWT는 token 전체를 뜻하지 않는다

**JWT(JSON Web Token)**는 정보를 JSON 기반으로 표현하고 서명할 수 있는 표준 형식이다.

```text
token
└─ 여러 형식 가능
   └─ JWT는 그중 하나
```

`token = JWT`라고 생각하면 안 된다.

#### 17. JWT payload는 기본적으로 비밀이 아니다

JWT payload는 base64url encoding된 데이터를 포함할 수 있어 누구나 decode할 수 있다.

서명은 변조 여부 검증과 관련되지만 내용을 숨기는 encryption과 다르다.

비밀번호나 민감한 비밀정보를 payload에 넣지 않는다.

#### 18. API 오류 response도 계약의 일부다

나쁜 오류:

```json
{"error":"fail"}
```

조금 더 구조적인 예:

```json
{
  "code": "INVALID_EMAIL",
  "message": "이메일 형식이 올바르지 않습니다",
  "field": "email"
}
```

client가 code를 보고 처리할 수 있고, 사람에게 message를 보여 줄 수 있다.

server의 stack trace, DB password, 내부 경로를 그대로 외부에 보내면 안 된다.

#### 19. 책을 덮고 확인한다

1. HTTP와 API의 차이는 무엇인가?
2. endpoint를 method+path 예로 설명하라.
3. REST와 JSON이 같은 말이 아닌 이유는 무엇인가?
4. pagination이 필요한 이유를 100만 사용자 예로 설명하라.
5. authentication과 authorization을 구분하라.
6. cookie/session 구조를 로그인 예로 설명하라.
7. bearer token이 노출되면 왜 위험한가?
8. JWT payload가 encryption된 비밀이라고 보면 안 되는 이유는 무엇인가?

---

## BLOCK 07 · 브라우저 보안 정책과 cache, 실시간 통신, 실제 네트워크 디버깅을 연결한다

### LESSON 01 · origin·CORS·cache·redirect·WebSocket/SSE·fetch·Network 진단을 하나의 장애 시나리오로 배운다

#### 1. 프론트엔드와 API domain이 다르면 갑자기 CORS 오류를 볼 수 있다

프론트:

```text
https://app.example.com
```

API:

```text
https://api.example.com
```

JavaScript가 API를 호출했는데 browser Console에 CORS 관련 오류가 나온다.

server가 완전히 꺼진 것일까?

반드시 그렇지는 않다.

브라우저 보안 정책이 response 접근을 막은 것일 수 있다.

#### 2. origin부터 정의한다

웹에서 **origin**은 일반적으로:

```text
scheme + host + port
```

조합으로 구분한다.

```text
https://example.com:443
```

다음 둘은 host가 다르다.

```text
https://app.example.com
https://api.example.com
```

따라서 다른 origin이다.

#### 3. same-origin policy는 다른 site 데이터를 마음대로 읽지 못하게 한다

악성 사이트를 연 사용자가 은행에도 로그인돼 있다고 하자.

악성 JavaScript가 은행 site의 민감한 response를 마음대로 읽을 수 있다면 위험하다.

브라우저는 기본적으로 다른 origin의 자원 접근에 제한을 둔다.

이것이 **same-origin policy**의 큰 목적이다.

#### 4. CORS는 server가 허용할 cross-origin 접근을 알려 주는 규칙이다

**CORS(Cross-Origin Resource Sharing)**는 server가 HTTP header를 사용해 브라우저에게 어떤 다른 origin의 접근을 허용할지 알려 주는 방식이다.

예:

```http
Access-Control-Allow-Origin: https://app.example.com
```

#### 5. CORS는 server를 보호하는 방화벽 전체가 아니다

CORS는 주로 **브라우저**가 cross-origin response를 JavaScript에 공개할지 판단하는 정책이다.

curl이나 server-to-server request는 같은 CORS 정책을 그대로 적용하지 않는다.

따라서:

```text
CORS 설정했으니 아무도 API를 직접 호출 못 한다
```

는 틀린 생각이다.

실제 인증/권한 검사가 필요하다.

#### 6. preflight는 실제 request 전에 허용 여부를 묻는다

일부 cross-origin request에서는 browser가 먼저 `OPTIONS` request를 보낸다.

```text
OPTIONS /api/users
```

server가:

```text
이 origin 허용
이 method 허용
이 header 허용
```

정보를 response한다.

이 사전 확인을 **preflight request**라고 부른다.

#### 7. 개발자는 POST 하나만 보냈는데 OPTIONS가 생길 수 있다

코드:

```javascript
fetch("https://api.example.com/users", {
  method: "POST",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify(data)
});
```

Network 패널에 OPTIONS가 먼저 보일 수 있다.

preflight가 실패하면 실제 POST가 보내지지 않을 수 있다.

따라서 `POST가 server log에 없다`면 OPTIONS부터 확인한다.

#### 8. cache는 같은 데이터를 매번 새로 받지 않도록 저장한다

브라우저와 중간 cache server는 자원을 다시 사용하기 위해 response를 저장할 수 있다.

이것이 **cache**다.

장점:

```text
network 전송 감소
server 부하 감소
화면 표시 빠름
```

단점:

```text
오래된 데이터가 보일 수 있음
```

#### 9. Cache-Control로 cache 정책을 전달한다

```http
Cache-Control: max-age=3600
```

일정 시간 response를 새로 요청하지 않고 사용할 수 있다는 정책을 전달할 수 있다.

민감 정보에는 `no-store` 같은 정책을 고려할 수 있다.

정확한 directive 의미는 HTTP 표준과 현재 browser 동작을 확인한다.

#### 10. ETag로 자원이 바뀌었는지 확인할 수 있다

server response:

```http
ETag: "abc123"
```

다음 request에서 client가:

```http
If-None-Match: "abc123"
```

을 보내 변경 여부를 물을 수 있다.

변경되지 않았다면:

```text
304 Not Modified
```

response로 body 전송을 줄일 수 있다.

#### 11. "배포했는데 옛날 화면" 문제에서 cache도 본다

원인 후보:

```text
배포 자체가 옛 버전
CDN cache
browser cache
service worker cache
API response cache
```

`새 코드를 올렸으니 browser도 무조건 새 파일을 받았다`고 가정하지 않는다.

Network에서 실제 response와 cache 상태를 본다.

#### 12. redirect는 다른 위치로 이동하라고 알려 준다

대표:

```text
301 Moved Permanently
302 Found
307 Temporary Redirect
308 Permanent Redirect
```

302와 307/308은 method 유지 규칙에서 차이가 있을 수 있다.

POST request redirect에서 method가 어떻게 처리되는지 중요할 수 있으므로 status 의미를 구분한다.

#### 13. 실시간 데이터는 매번 새 HTTP request만 보내야 할까

채팅, 주가, 협업 문서처럼 server에서 새 데이터가 자주 생긴다.

방법 1 — polling:

```text
5초마다 "새 데이터 있어요?" request
```

구현은 단순하지만 새 데이터가 없어도 계속 요청한다.

#### 14. SSE는 server에서 client 방향 이벤트 stream을 보낼 수 있다

**Server-Sent Events(SSE)**는 하나의 HTTP 연결을 유지하며 server가 client로 이벤트를 계속 보낼 수 있게 한다.

server→client 방향 실시간 업데이트가 주목적일 때 WebSocket보다 단순한 선택이 될 수 있다.

#### 15. WebSocket은 양방향 지속 연결을 제공한다

**WebSocket**은 연결을 유지하면서 client와 server가 양쪽에서 message를 보낼 수 있는 protocol이다.

```text
chat
온라인 게임 상태
협업 편집
실시간 dashboard
```

같은 양방향 통신에 유용하다.

그렇다고 `실시간 = 무조건 WebSocket`은 아니다. update 방향, 규모, proxy 지원, 재연결, 운영 난이도를 보고 선택한다.

#### 16. fetch request를 처음부터 끝까지 읽는다

```javascript
async function loadUser(id) {
  const response = await fetch(`/api/users/${id}`, {
    headers: {
      Accept: "application/json"
    }
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const data = await response.json();
  return data;
}
```

순서:

```text
URL 생성
↓ fetch
network/TLS/HTTP
↓ response 도착
status 확인
↓ body를 JSON으로 parsing
data 반환
```

#### 17. `response.json()`도 비동기다

response body가 stream으로 도착하고 parsing이 필요하다.

```javascript
const data = await response.json();
```

은 body를 읽고 JSON으로 해석하는 비동기 작업이다.

#### 18. network error와 HTTP error를 코드에서 구분한다

```javascript
try {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
} catch (error) {
  ...
}
```

catch에는 DNS/TLS/connection 실패와 우리가 throw한 HTTP 오류가 모두 들어올 수 있다.

실제 앱에서는 error 종류나 status를 구조화해서 사용자 메시지와 retry 정책을 다르게 할 수 있다.

#### 19. 장애 진단 순서를 고정한다

```text
1. 기기 network 연결은 정상인가?
2. DNS가 domain을 찾는가?
3. IP/port에 연결 가능한가?
4. TLS handshake가 성공하는가?
5. HTTP request가 실제 전송됐는가?
6. status code는 무엇인가?
7. response headers는 무엇인가?
8. body가 어떤 형식인가?
9. browser CORS가 막았는가?
10. cache 때문에 오래된 response를 보고 있나?
11. JavaScript parsing/rendering에서 실패했나?
```

모든 상황에 항상 이 순서 그대로만 써야 한다는 뜻은 아니지만 초보자가 막연히 추측하는 것을 줄인다.

#### 20. 최종 프로젝트 · 파일 업로드 API 조사기

프론트 HTML:

```html
<form id="profile-form">
  <label for="nickname">닉네임</label>
  <input id="nickname" name="nickname">

  <label for="photo">사진</label>
  <input id="photo" name="photo" type="file" accept="image/*">

  <button type="submit">업로드</button>
</form>
```

JavaScript:

```javascript
form.addEventListener("submit", async event => {
  event.preventDefault();

  const formData = new FormData(form);

  const response = await fetch("/api/profile", {
    method: "POST",
    body: formData
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const result = await response.json();
  console.log(result);
});
```

실제로 확인한다.

```text
Request Method가 POST인가?
Content-Type에 multipart/form-data와 boundary가 있는가?
nickname part가 있는가?
photo part가 있는가?
photo part의 MIME type은 무엇인가?
response Content-Type은 application/json인가?
status code는 무엇인가?
```

#### 21. 오류를 일부러 만든다

```text
1. 잘못된 endpoint → 404 확인
2. server에서 일부러 500 → HTTP error 확인
3. API server 종료 → connection/network error 확인
4. CORS 허용 origin 제거 → browser CORS 차단 확인
5. FormData에 Content-Type을 잘못 수동 지정 → boundary parsing 오류 관찰
6. 오래된 response에 cache header 부여 → cache 현상 확인
```

각 실패가 **어느 층인지** 기록한다.

#### 22. TRACK 06 완료 기준

책을 보지 않고 다음을 설명할 수 있어야 한다.

- Wi-Fi, local network, Internet을 구분한다.
- client/server 역할을 설명한다.
- private/public IP와 NAT를 집 공유기 예로 설명한다.
- domain/DNS/IP/port가 어떻게 연결되는지 설명한다.
- TCP/UDP의 큰 차이와 latency/bandwidth 차이를 설명한다.
- TLS/certificate/HTTPS 관계를 설명한다.
- URL을 scheme/host/port/path/query/fragment로 나눈다.
- HTTP request/response와 method/status/header/body를 읽는다.
- safe/idempotent의 의미를 실제 요청으로 설명한다.
- MIME type이 왜 필요한지 설명한다.
- JSON object와 JSON text를 구분한다.
- Content-Type과 Accept를 구분한다.
- multipart/form-data와 boundary를 실제 request에서 찾는다.
- API/HTTP/REST/JSON을 서로 다른 개념으로 설명한다.
- authentication/authorization/cookie/session/token을 구분한다.
- origin/same-origin/CORS/preflight를 설명한다.
- cache/ETag/304의 큰 흐름을 설명한다.
- polling/SSE/WebSocket의 목적 차이를 설명한다.
- Network 패널에서 실패 단계가 DNS인지 HTTP인지 CORS인지 구분한다.

이 TRACK의 통과 기준은 `MIME = application/json` 같은 단어 암기가 아니다.

**주소창에 URL을 넣은 순간부터 화면이 데이터를 사용할 때까지 어떤 단계가 있고, 문제가 생기면 어느 층을 먼저 확인할지 설명할 수 있어야 한다.**
