## V1-C08. 포트·DNS·앱/웹/서버 경계

**원본 Lesson 매핑:** V1-21, V1-22, V1-23

### 1. 현실 문제에서 시작하기

브라우저에서 사이트가 안 열릴 때 “인터넷이 안 된다”고 말하지만 실제로는 Wi-Fi 연결, DNS, 특정 서버, 포트, TLS, 인증, 앱 자체 중 하나만 실패했을 수 있다. 또 웹앱과 네이티브 앱이 같은 서비스를 보여줘도 내부 경로는 다를 수 있다.

이 장에서는 네트워크를 패킷 계산 문제로 배우지 않는다. **이름을 주소로 찾고(DNS), 주소의 특정 서비스에 연결하고(port/socket), 클라이언트가 서버와 요청/응답을 주고받는 경계**를 진단용 mental model로 배운다.

### 2. Mental Model

```text
앱/브라우저(client)
  ↓  example.com 이름 사용
DNS resolver
  ↓  IP 주소
client → IP:port 연결
  ↓
server process가 해당 port에서 listen
  ↓
요청 처리 → 응답
```

`localhost`는 보통 현재 기기 자신을 가리킨다. 휴대폰에서 `localhost:8000`은 PC의 개발 서버가 아니라 휴대폰 자신이다. 이 한 가지 오해만으로도 많은 초보 네트워크 오류가 생긴다.

### 3. 핵심 개념 해부

#### IP·port·socket

IP 주소는 네트워크에서 호스트/인터페이스를 찾는 데 쓰이고, port는 한 호스트 안의 특정 네트워크 서비스를 구분하는 데 쓰인다. 서버 프로세스가 어떤 port에서 listen하고 클라이언트가 그 주소로 connect한다.

port conflict는 두 프로세스가 같은 주소/port를 동시에 사용하려 할 때 생길 수 있다. 개발 서버가 안 뜨면 코드보다 먼저 “프로세스가 실제 실행 중인가, 어느 port에서 listen 중인가”를 확인한다.

#### localhost의 의미

`127.0.0.1`/`localhost`는 현재 장치의 loopback을 가리키는 대표적 방식이다. PC에서 서버를 띄우고 Galaxy 앱에서 `localhost`로 접속하면 휴대폰 자신을 찾게 되어 실패할 수 있다. 같은 Wi-Fi의 PC LAN IP나 개발 도구의 포트 포워딩 등 실제 연결 경로가 필요하다.

이 차이는 에뮬레이터에서도 별도 규칙이 있을 수 있으므로 플랫폼 문서를 확인해야 한다.

#### DNS

사람은 도메인 이름을 쓰지만 네트워크 연결에는 IP 주소가 필요하다. DNS resolver가 이름을 주소 레코드로 변환한다. DNS cache 때문에 변경 직후 오래된 주소가 보이거나, 특정 네트워크에서만 해석이 실패할 수 있다.

도메인 실패와 서버 실패를 구분하려면 이름 해석 결과와 직접 IP 접근 가능성, 다른 resolver/네트워크 결과를 비교한다. 다만 HTTPS 서비스는 IP 직접 접속 시 인증서/Host 문제로 별도 실패할 수 있어 단순 판정은 금물이다.

#### 네이티브 앱·웹앱·브라우저·backend

브라우저는 웹 콘텐츠를 해석·실행하는 클라이언트다. 웹앱은 주로 브라우저에서 동작하고, 네이티브 앱은 Android 플랫폼 API를 직접 활용할 수 있다. 둘 모두 같은 backend API를 사용할 수 있다.

따라서 웹은 되는데 앱만 안 되면 backend 전체 장애 가능성이 낮아지고 앱 버전·인증·네트워크 보안 설정 같은 차이를 본다. 반대로 웹과 앱이 동시에 같은 기능만 실패하면 공통 backend나 계정 문제를 의심할 수 있다.

#### cloud·CDN·서버 경계

클라우드는 여러 인프라 서비스를 묶는 운영방식이고, CDN은 사용자와 가까운 위치에 정적 콘텐츠나 캐시된 데이터를 전달해 지연을 줄이는 역할을 한다. 사용자는 ‘서버 하나’로 느끼지만 실제 요청은 load balancer, API server, DB, CDN 등을 거칠 수 있다.

Power User 단계에서는 세부 라우팅 알고리즘보다 “어느 구성요소가 공통 원인인지”를 파악하는 수준이 중요하다.

### 3A. 개념을 연결해서 생각하기 — 주소·이름·서비스·애플리케이션을 분리한다

네트워크 오류가 어려운 이유는 사용자가 도메인 한 줄만 보지만 내부에서는 여러 단계가 연속되기 때문이다. 브라우저가 `example.com`을 받으면 이름을 IP로 해석하고, 적절한 transport 연결을 만들고, TLS가 있다면 인증서와 암호협상을 거친 뒤, HTTP 요청을 서버 애플리케이션에 보낸다. HTTP 500이 왔다면 적어도 DNS·연결·TLS·요청 전달의 상당 부분은 성공했다는 중요한 단서다.

DNS는 phone book 비유가 유용하지만 완벽하지 않다. 하나의 도메인이 여러 IP를 가질 수 있고, 지역·CDN·load balancing에 따라 답이 달라질 수 있으며, TTL/cache 때문에 변경이 즉시 모든 사용자에게 반영되지 않는다. 그러므로 DNS 변경 후 일부 사용자만 실패하는 현상은 자연스럽게 생길 수 있다.

port는 “건물의 방 번호” 비유를 쓸 수 있지만, 실제로는 transport endpoint를 구분하는 숫자다. 서버 process가 특정 interface/address와 port에 bind/listen한다. `127.0.0.1`에만 bind하면 같은 PC에서는 되지만 외부 기기에서는 접근하지 못할 수 있다. `0.0.0.0` 같은 listen 설정의 의미는 플랫폼 문서를 보고 안전하게 다룬다.

앱과 웹의 경계도 중요하다. Android 앱은 OS network security config, certificate store, VPN, Private DNS, app permission의 영향을 받고 브라우저는 별도 정책과 cache를 가질 수 있다. “웹에서 됨”은 서버가 완전히 정상이라는 강한 단서지만 Android 앱 경로의 모든 조건을 보장하지 않는다.

localhost를 제대로 이해하면 개발 연결 문제가 크게 줄어든다. **localhost는 요청을 보내는 그 장치 자신**이다. Galaxy에서 PC server를 보려면 PC의 네트워크 주소와 server listen 범위, firewall, 같은 네트워크 여부를 맞춰야 한다. Android emulator는 host access에 특수 주소를 제공할 수 있으므로 현재 개발환경 문서를 확인한다.

CDN과 cache는 성능을 높이지만 “나는 새 파일을 배포했는데 일부 사용자에게 옛 파일” 같은 일관성 문제를 만든다. cache key, TTL, invalidation, versioned asset name을 이해하면 이런 문제를 더 정확하게 진단할 수 있다.

### 4. Worked Examples

#### Worked Example A — 로컬 서버가 안 열림

개발 서버를 `localhost:8000`에 띄웠는데 브라우저도 접속 불가다.

1. 서버 프로세스가 실행 중인지 확인.
2. 실제 listen 주소/port 확인.
3. 다른 프로세스가 port를 점유했는지 확인.
4. `localhost`와 LAN 접근 요구를 구분.
5. firewall/OS permission 확인.
6. 같은 PC 브라우저 → 다른 기기 순서로 범위를 넓힌다.

처음부터 공유기 설정을 바꾸지 않는다.

#### Worked Example B — DNS vs 서버

`example.com`이 안 열릴 때 먼저 DNS가 주소를 반환하는지 본다. 주소가 전혀 안 나오면 이름해석 문제 후보가 커진다. 주소는 나오지만 연결 timeout이면 서버/라우팅/firewall 후보를 본다. HTTP 응답 500이 오면 네트워크 연결 자체는 상당 부분 성공했고 애플리케이션 서버 처리가 실패한 것이다.

### 4A. 미니 사례집

**사례 1 — 모바일 데이터에서는 되고 회사 Wi-Fi에서 안 됨**  
앱 코드 공통성보다 DNS filtering, firewall/proxy, captive portal, VPN 차이를 먼저 비교한다.

**사례 2 — 도메인은 ping/resolve되는데 앱 timeout**  
DNS 단계는 통과했다. port/firewall/server listen/TLS/application latency로 이동한다.

**사례 3 — 웹 최신 이미지, 앱은 구버전**  
CDN/cache key 또는 앱 자체 image cache를 비교한다. origin 파일만 확인하고 끝내지 않는다.

**사례 4 — PC localhost server를 Galaxy에서 못 봄**  
Galaxy의 localhost가 Galaxy를 가리킨다는 사실을 먼저 확인한다.

### 5. 그럴듯하지만 틀린 판단

1. localhost는 항상 내 PC를 의미한다고 생각한다.
2. DNS가 성공하면 서버도 반드시 정상이라고 생각한다.
3. 웹이 되면 네이티브 앱도 같은 인증/네트워크 조건이라고 생각한다.
4. HTTP 500을 인터넷 연결 실패로 분류한다.

### 5A. 네트워크 6단계 Gate

1. **이름:** DNS가 기대 주소를 반환하는가?
2. **경로:** 해당 네트워크에서 주소로 갈 수 있는가?
3. **port:** 서버가 실제 listen 중인가?
4. **보안:** TLS/certificate/policy가 통과하는가?
5. **프로토콜:** HTTP status와 header는 무엇인가?
6. **애플리케이션:** server logic/DB가 정상인가?

앞 단계가 실패했는데 뒷단계 코드를 수정하지 않는다. 반대로 500 응답을 받았는데 DNS cache를 지우는 식의 층 혼동을 피한다.

### 5B. 상태코드와 연결오류를 같은 언어로 읽기

네트워크 디버깅에서는 **응답이 왔는가**가 첫 분기다. `DNS resolution failed`, `connection refused`, `timeout`처럼 HTTP 응답 자체가 없는 오류와 `401`, `404`, `500`처럼 서버가 HTTP 응답을 준 오류는 단계가 다르다. 후자는 적어도 서버 애플리케이션 경계까지 상당 부분 도달했다는 뜻이다.

`connection refused`는 보통 대상 host까지 가서 해당 port에 listen하는 서비스가 없거나 즉시 거절된 상황을 의심하고, `timeout`은 경로 중간 drop, firewall, server hang, 네트워크 품질 등 후보가 더 넓다. 오류 문자열 하나만으로 확정하지 않고 환경별 비교와 server log를 본다.

HTTPS에서는 certificate name과 도메인도 중요하다. DNS 진단을 위해 IP 주소를 브라우저에 직접 넣었는데 TLS 오류가 났다고 서버가 죽었다고 결론내리면 안 된다. 인증서는 특정 hostname에 발급되는 경우가 일반적이므로 테스트 방법 자체가 새로운 실패를 만들 수 있다.

VPN과 Private DNS, 광고차단 DNS, 보안앱은 네트워크 경로를 바꿀 수 있다. 특정 사용자만 실패하는 경우 앱 버전만 비교하지 말고 이런 기기별 네트워크 설정을 함께 본다.

API base URL은 환경설정 문제와 네트워크 문제의 경계다. production 앱이 staging 주소를 보고 있다면 DNS와 서버는 모두 “정상”인데 사용자는 잘못된 데이터를 보게 된다. 실제 요청 URL을 로그로 남기되 secret query는 제거한다.

### 6. Guided Lab

자신이 사용하는 서비스 하나를 `client → DNS → network → server/API → DB/storage → response`로 분해한다. 각 경계에서 관찰 가능한 증거를 한 개씩 적는다. 가능하면 브라우저 개발자도구나 안전한 네트워크 진단 도구로 도메인, 상태코드, 응답시간을 확인한다.

### 7. Independent Lab

“PC의 로컬 웹서버를 같은 Wi-Fi의 Galaxy에서 열기” 시나리오의 네트워크 경로를 설계하라. localhost가 왜 실패할 수 있는지, 필요한 PC 주소·listen 설정·firewall 후보를 설명한다. 실제 설정 변경은 안전한 테스트 환경에서만 한다.

### 8. Debug Challenge

**상황:** 웹페이지는 정상인데 Android 앱은 `unknown host` 오류. DNS 설정, 앱 network permission, VPN/Private DNS, 도메인 오타, 환경별 base URL을 후보로 만든다. 웹이 된다는 사실만으로 앱의 DNS 경로가 동일하다고 가정하지 않는다.

### 9. AI Audit

AI가 네트워크 오류 해결책으로 “방화벽을 완전히 끄라”고 제안하면 최소한의 진단을 하지 않은 것이다. 어떤 port/주소가 필요한지, 로컬/외부 접근 중 무엇인지, 잠깐의 테스트라면 어떤 안전한 범위로 제한할지 검토한다.

### 9A. 진단 미니드릴 — 증상을 보면 첫 질문부터 고른다

**드릴 1. unknown host**  
첫 판단: DNS/base URL/VPN/Private DNS부터 확인한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 2. connection refused**  
첫 판단: 대상 IP:port에 process가 listen하는지 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 3. HTTP 401**  
첫 판단: 네트워크 연결보다 인증 token/credential scope를 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 4. HTTP 500**  
첫 판단: DNS 지우기보다 server application/log를 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 5. PC localhost는 되는데 Galaxy는 안 됨**  
첫 판단: Galaxy localhost가 Galaxy 자신이라는 점과 PC listen/firewall을 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 6. 도메인 변경 후 일부만 옛 서버**  
첫 판단: TTL/cache/CDN resolver 차이를 확인한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

이 미니드릴의 목표는 정답 암기가 아니다. **증상 → 계층 → 확인할 증거**를 30초 안에 연결하는 습관을 만드는 것이다.

### 10. 회상 문제

1. IP와 port의 역할을 구분하라.
2. server가 port에서 listen한다는 말은?
3. localhost가 현재 장치를 가리킨다는 것이 왜 중요한가?
4. DNS는 무엇을 해결하는가?
5. DNS 성공과 HTTP 성공은 왜 다른가?
6. 웹앱과 네이티브 앱이 같은 backend를 쓸 수 있다는 의미는?
7. HTTP 500이 왔을 때 이미 성공했다고 볼 수 있는 계층은?
8. CDN의 실용적 역할은?

### 11. 전이 문제

1. PC 웹에서는 되지만 Galaxy 앱에서만 API가 안 된다. 공통/비공통 계층을 나눠 진단하라.
2. 도메인 변경 직후 일부 사용자만 옛 서버로 간다. DNS cache 관점의 확인사항을 적어라.
3. 앱이 특정 Wi-Fi에서만 실패한다. DNS/VPN/firewall 후보를 구분할 실험을 설계하라.

### 12. Chapter 완료 증거

- Guided Lab의 실행/관찰 결과를 남긴다.
- 정상 케이스뿐 아니라 실패·경계 케이스를 최소 1개 보존한다.
- “무엇을 바꿨는가 / 왜 바꿨는가 / 무엇으로 맞음을 확인했는가”를 5문장 이내로 적는다.
- AI를 사용했다면 AI 답의 오류·누락·과잉변경 여부를 체크한 기록을 남긴다.
- 결과가 예상과 다르면 PASS라고 쓰지 않고, 재현 조건과 다음 실험을 기록한다.

### 13. 참고 렌즈

- Computer Networking: A Top-Down Approach, 9e (2025)
- MDN — How the Web works
- Android Developers — networking guidance
- Harvard CS50 networking/web materials
- Cloud provider networking fundamentals

---
