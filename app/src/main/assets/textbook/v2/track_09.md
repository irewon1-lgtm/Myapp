# TRACK 09 · 보안과 데이터 보호

보안은 `해커가 어려운 암호를 푸는 것`만을 뜻하지 않는다.

실제 앱 보안의 많은 문제는 아주 단순한 질문에서 시작한다.

```text
누가 이 데이터를 볼 수 있는가?
누가 이 기능을 실행할 수 있는가?
사용자가 보낸 값을 어디까지 믿을 수 있는가?
비밀번호와 key를 어디에 저장하는가?
파일이 바뀌지 않았다는 것을 어떻게 확인하는가?
```

이 TRACK에서는 encoding, hash, encryption을 서로 섞지 않고 하나씩 만든다.

---

## BLOCK 01 · 보안이란 무엇인가

### LESSON 01 · 지켜야 할 것이 있다

앱에는 지켜야 할 자산이 있다.

```text
사용자 개인정보
비밀번호
결제 정보
회사 내부 데이터
서버 권한
서비스 가용성
```

이런 보호 대상을 **자산(asset)**이라고 부른다.

### LESSON 02 · 공격자

보호 규칙을 우회해 자산에 접근하거나 서비스를 방해하려는 사람·프로그램을 **공격자(attacker)**라고 부른다.

공격자는 외부인만이 아니라 권한을 오용하는 내부 사용자일 수도 있다.

---

## BLOCK 02 · CIA triad

### LESSON 01 · Confidentiality

허가되지 않은 사람이 정보를 읽지 못하게 하는 성질을 **기밀성(confidentiality)**이라고 부른다.

### LESSON 02 · Integrity

데이터가 허가 없이 바뀌거나 망가지지 않고 올바른 상태를 유지하는 성질을 **무결성(integrity)**이라고 부른다.

### LESSON 03 · Availability

필요할 때 정상 사용자가 시스템을 사용할 수 있는 성질을 **가용성(availability)**이라고 부른다.

이 세 관점을 함께 **CIA triad**라고 부른다.

---

## BLOCK 03 · threat

### LESSON 01 · 무엇이 잘못될 수 있는가

자산에 피해를 줄 수 있는 가능성을 **위협(threat)**이라고 부른다.

예:

```text
비밀번호 탈취
DB 변조
서비스 마비
파일 업로드 악용
```

### LESSON 02 · vulnerability

공격자가 이용할 수 있는 시스템의 약점을 **취약점(vulnerability)**이라고 부른다.

```text
SQL 문자열 결합
약한 비밀번호 저장
권한 검사 누락
```

같은 것이 취약점이 될 수 있다.

---

## BLOCK 04 · risk

### LESSON 01 · 가능성과 피해를 함께 본다

모든 취약점이 같은 위험도를 가지는 것은 아니다.

공격 가능성과 피해 크기를 함께 고려한 위험을 **risk**라고 부른다.

```text
발생 가능성 × 영향
```

처럼 생각할 수 있지만 실제 risk 평가는 더 많은 요소를 본다.

---

## BLOCK 05 · trust boundary

### LESSON 01 · 신뢰 수준이 바뀌는 경계

내 서버 코드와 인터넷에서 들어온 사용자 입력은 같은 수준으로 신뢰할 수 없다.

신뢰 수준이 바뀌는 지점을 **신뢰 경계(trust boundary)**라고 부른다.

예:

```text
브라우저 → 서버
서버 → 외부 결제 API
앱 → 로컬 파일
일반 사용자 → 관리자 기능
```

### LESSON 02 · 경계에서 검증한다

외부에서 내부로 들어오는 데이터는 경계를 통과할 때 검증해야 한다.

`프론트엔드에서 이미 검사했다`는 이유로 서버 검증을 생략하지 않는다.

---

## BLOCK 06 · threat modeling

### LESSON 01 · 공격이 일어난 뒤만 생각하지 않는다

시스템 설계 단계에서:

```text
무엇을 지켜야 하나?
누가 공격할 수 있나?
어떤 경로로 들어오나?
가장 큰 피해는 무엇인가?
어떤 방어가 있나?
```

를 미리 분석하는 것을 **위협 모델링(threat modeling)**이라고 부른다.

---

## BLOCK 07 · authentication

### LESSON 01 · 누구인지 확인

**authentication(인증)**은 사용자가 누구인지 확인하는 과정이다.

예:

```text
password
passkey
생체 인증
일회용 코드
```

### LESSON 02 · identifier와 authenticator

이메일 주소는 `누구라고 주장하는지` 알려주는 식별자다.

비밀번호는 그 주장을 증명하는 인증 수단이다.

이메일을 안다고 로그인할 권한이 생기는 것은 아니다.

---

## BLOCK 08 · authorization

### LESSON 01 · 무엇을 할 수 있는가

인증된 사용자가 어떤 기능과 데이터에 접근할 수 있는지 판단하는 것을 **authorization(인가)**이라고 부른다.

### LESSON 02 · UI를 숨긴다고 권한 보호가 아니다

관리자 버튼을 화면에서 숨겨도 공격자는 API를 직접 호출할 수 있다.

권한 검사는 서버에서 실행해야 한다.

---

## BLOCK 09 · least privilege

### LESSON 01 · 필요한 최소 권한만

사용자, 프로그램, DB 계정에 필요한 기능만 수행할 수 있는 최소 권한을 주는 원칙을 **최소 권한 원칙(principle of least privilege)**이라고 부른다.

### LESSON 02 · 왜 중요한가

웹 서버 계정이 DB 모든 table 삭제 권한까지 가지고 있다면 서버 하나가 뚫렸을 때 피해가 커진다.

필요한 table과 작업만 허용하면 피해 범위를 줄일 수 있다.

---

## BLOCK 10 · encoding 다시 보기

### LESSON 01 · 표현을 바꾸는 규칙

**encoding**은 정보를 정해진 규칙에 따라 다른 표현으로 바꾸는 것이다.

예:

```text
문자 → UTF-8 bytes
binary → Base64 text
```

### LESSON 02 · encoding은 보안이 아니다

Base64로 바꾼 문자열은 쉽게 원래 bytes로 되돌릴 수 있다.

```text
encoding 목적 = 표현/전송 방식 변경
encryption 목적 = 비밀 보호
```

둘을 섞지 않는다.

---

## BLOCK 11 · Base64

### LESSON 01 · binary를 글자로 표현

**Base64**는 binary 데이터를 제한된 ASCII 문자 집합으로 표현하는 encoding 방식이다.

이미지 bytes를 JSON이나 텍스트 채널에 넣어야 할 때 사용할 수 있다.

### LESSON 02 · Base64 password는 암호화가 아니다

```text
password → Base64
```

는 비밀번호 보호가 아니다.

누구나 쉽게 decode할 수 있다.

---

## BLOCK 12 · hash를 배우기 전에

### LESSON 01 · 데이터가 바뀌었는지 빠르게 비교하고 싶다

파일 10GB 전체를 매번 사람 눈으로 비교할 수 없다.

데이터에서 짧은 대표값을 계산해 비교하는 방법이 필요하다.

### LESSON 02 · 한 방향 계산

입력을 일정 길이의 값으로 계산하지만 그 결과에서 원본을 다시 복구하기 어렵게 설계된 함수가 있다.

이런 함수가 암호학적 hash function이다.

---

## BLOCK 13 · cryptographic hash

### LESSON 01 · 한 줄 뜻

**암호학적 해시 함수(cryptographic hash function)**는 임의 길이 데이터를 받아 고정 길이의 hash 값을 만드는 함수다.

예:

```text
hello
↓ SHA-256
2cf24dba5f...
```

### LESSON 02 · 같은 입력은 같은 hash

같은 bytes를 넣으면 같은 hash 결과가 나온다.

파일이 한 bit라도 바뀌면 보통 매우 다른 결과가 나온다.

---

## BLOCK 14 · SHA-256

### LESSON 01 · 대표적인 cryptographic hash

**SHA-256**은 SHA-2 계열의 암호학적 hash 함수다.

256 bit 길이의 hash 값을 만든다.

### LESSON 02 · 어디에 쓰나

```text
파일 무결성 확인
디지털 서명 과정
Git과 별개 용도의 식별/검증
```

등에 사용될 수 있다.

비밀번호 저장에는 단순 SHA-256만 사용하는 것이 적절하지 않다.

---

## BLOCK 15 · collision

### LESSON 01 · 입력이 다른데 hash가 같은 경우

hash 출력 가능한 값의 수는 유한하기 때문에 이론적으로 서로 다른 입력이 같은 hash를 가질 수 있다.

이를 **collision(충돌)**이라고 부른다.

### LESSON 02 · collision resistance

공격자가 같은 hash를 만드는 다른 입력을 찾기 매우 어렵게 만드는 성질을 **collision resistance**라고 부른다.

암호학적 hash에서 중요한 성질 중 하나다.

---

## BLOCK 16 · preimage resistance

### LESSON 01 · hash만 보고 원본 찾기 어렵게

hash 값 하나가 주어졌을 때 그 hash를 만드는 입력을 찾기 매우 어려운 성질을 **preimage resistance**라고 부른다.

### LESSON 02 · hash = 암호화가 아니다

암호화는 올바른 key가 있으면 복호화하도록 설계된다.

hash는 원래 입력을 되돌리는 복호화 절차를 제공하지 않는다.

---

## BLOCK 17 · 파일 무결성

### LESSON 01 · 다운로드 파일 확인

공식 사이트가 APK SHA-256을 제공했다고 하자.

```text
공식 hash = ABC...
내가 받은 APK hash = ABC...
```

같다면 다운로드 과정에서 bytes가 달라지지 않았다는 강한 증거가 된다.

### LESSON 02 · hash만으로 누가 만들었는지 증명되나

공격자가 파일과 hash 값을 모두 바꿀 수 있다면 둘을 함께 속일 수 있다.

hash는 데이터 비교에는 좋지만 `누가 이 값을 제공했는가`까지 혼자 해결하지 않는다.

그 문제에 디지털 서명이 필요하다.

---

## BLOCK 18 · password를 hash하는 이유

### LESSON 01 · 원래 password를 저장하지 않는다

서버 DB에 비밀번호 원문을 저장하면 DB가 유출될 때 모든 password가 바로 노출된다.

비밀번호는 복호화할 필요가 없다.

로그인할 때 입력 password를 다시 계산해 저장된 값과 비교하면 된다.

### LESSON 02 · 빠른 SHA-256만 쓰면 왜 위험한가

공격자는 수십억 개의 후보 password를 매우 빠르게 계산해 비교할 수 있다.

비밀번호 저장에는 의도적으로 느리고 메모리를 많이 쓰게 만든 전용 password hashing algorithm을 사용한다.

---

## BLOCK 19 · salt

### LESSON 01 · 같은 password라도 다른 결과

**salt**는 password를 hash할 때 함께 사용하는 무작위 값이다.

```text
password + random salt
↓ password hash
```

### LESSON 02 · salt는 비밀일 필요가 없다

salt는 hash와 함께 저장할 수 있다.

목적은 같은 password를 쓰는 사용자들의 hash가 똑같아지는 것을 막고 미리 계산한 대규모 공격을 어렵게 만드는 것이다.

---

## BLOCK 20 · password hashing algorithm

### LESSON 01 · bcrypt

**bcrypt**는 password 저장을 위해 널리 사용된 느린 hash 알고리즘이다.

작업 비용을 조정할 수 있다.

### LESSON 02 · Argon2

**Argon2**는 password hashing을 위해 설계된 메모리-하드 함수다.

현대 시스템에서 강한 선택지로 사용된다.

### LESSON 03 · 직접 암호 알고리즘 만들지 않는다

`나만의 특별한 hash`를 직접 만드는 것은 위험하다.

검증된 라이브러리와 최신 권장 설정을 사용한다.

---

## BLOCK 21 · encryption

### LESSON 01 · key가 있으면 원본으로 되돌릴 수 있게 숨긴다

**암호화(encryption)**는 데이터를 key를 사용해 읽을 수 없는 형태로 바꾸고, 올바른 key를 가진 쪽이 다시 원본으로 복호화할 수 있게 만드는 과정이다.

```text
plaintext
↓ encryption + key
ciphertext
↓ decryption + key
plaintext
```

### LESSON 02 · plaintext와 ciphertext

```text
plaintext = 암호화 전 원본 데이터
ciphertext = 암호화 후 읽기 어려운 데이터
```

---

## BLOCK 22 · symmetric encryption

### LESSON 01 · 같은 비밀 key를 공유

암호화와 복호화에 같은 secret key를 사용하는 방식을 **대칭키 암호화(symmetric encryption)**라고 부른다.

대표 알고리즘으로 AES가 있다.

### LESSON 02 · key 전달 문제

양쪽이 같은 secret key를 알아야 한다.

인터넷에서 처음 만난 상대에게 이 key를 안전하게 전달하는 문제가 있다.

---

## BLOCK 23 · AES

### LESSON 01 · 현대 대칭키 암호화 표준

**AES(Advanced Encryption Standard)**는 널리 사용하는 대칭키 암호 알고리즘이다.

### LESSON 02 · mode와 nonce가 중요하다

AES라는 이름만 쓰면 충분하지 않다.

GCM 같은 안전한 mode와 nonce 관리가 중요하다.

현대 라이브러리의 authenticated encryption API를 사용한다.

---

## BLOCK 24 · nonce

### LESSON 01 · 한 번만 사용해야 하는 값

암호화에서 특정 key와 함께 재사용하면 안 되는 값을 **nonce**라고 부른다.

`number used once`에서 이름이 왔다.

### LESSON 02 · nonce가 꼭 비밀은 아니다

nonce는 암호문과 함께 저장·전송할 수 있다.

중요한 것은 알고리즘 요구에 맞게 유일하거나 예측 불가하게 만드는 것이다.

---

## BLOCK 25 · authenticated encryption

### LESSON 01 · 숨기는 것과 변조 확인을 함께

암호문이 중간에서 바뀌어도 알아차려야 한다.

기밀성과 무결성을 함께 제공하는 방식을 **authenticated encryption**이라고 부른다.

AES-GCM, ChaCha20-Poly1305 등이 대표적인 방식이다.

### LESSON 02 · encryption만 하고 integrity를 빼면

일부 암호화 방식은 암호문 변조를 제대로 탐지하지 못할 수 있다.

현대 앱에서는 검증된 AEAD 방식을 우선 사용한다.

---

## BLOCK 26 · asymmetric cryptography

### LESSON 01 · key 두 개를 쓴다

서로 다른 두 key를 한 쌍으로 사용하는 방식을 **비대칭키 암호(asymmetric cryptography)**라고 부른다.

```text
public key = 공개 가능
private key = 소유자가 비밀로 보호
```

### LESSON 02 · 공개키로 무엇을 하나

알고리즘과 용도에 따라:

```text
암호화 key 합의
디지털 서명 검증
```

등에 사용한다.

`public key로 암호화하면 private key로 복호화`라는 단순 공식만으로 모든 현대 프로토콜을 설명하지 않는다.

---

## BLOCK 27 · digital signature

### LESSON 01 · 누가 만들었고 변조되지 않았는지 확인

**디지털 서명(digital signature)**은 private key 소유자가 데이터에 서명하고 다른 사람이 public key로 그 서명을 검증할 수 있게 하는 암호 기술이다.

### LESSON 02 · 서명이 제공하는 것

올바른 검증이 성공하면:

```text
해당 private key 소유자가 서명했음
서명 후 데이터가 변조되지 않았음
```

을 확인하는 데 사용할 수 있다.

내용을 숨기는 암호화와 목적이 다르다.

---

## BLOCK 28 · certificate

### LESSON 01 · public key와 이름을 연결

서버 public key가 진짜 `example.com`의 것인지 어떻게 알까?

도메인과 public key 정보 등을 신뢰 체계에 연결한 문서를 **디지털 인증서(certificate)**라고 부른다.

### LESSON 02 · CA

인증서를 발급·서명하는 신뢰 기관을 **CA(Certificate Authority)**라고 부른다.

브라우저는 신뢰할 CA 목록을 이용해 certificate chain을 검증한다.

---

## BLOCK 29 · PKI

### LESSON 01 · 인증서 전체 신뢰 체계

public key, certificate, CA, 발급·폐기·검증 절차를 포함한 체계를 **PKI(Public Key Infrastructure)**라고 부른다.

TLS 웹 인증서가 이 체계를 사용한다.

---

## BLOCK 30 · TLS 다시 보기

### LESSON 01 · 서버 인증과 안전한 session key 합의

TLS handshake에서 client와 server는 certificate를 검증하고 안전한 암호 key를 합의한다.

그 뒤 실제 HTTP 데이터는 효율적인 대칭키 암호로 보호된다.

### LESSON 02 · HTTPS가 주는 것

정상 검증된 HTTPS 연결은:

```text
전송 중 기밀성
전송 중 무결성
서버 인증
```

을 제공한다.

사용자가 사기 사이트에 직접 접속했다면 HTTPS라도 사이트 내용이 선하다는 뜻은 아니다.

---

## BLOCK 31 · secret 관리

### LESSON 01 · secret이란 무엇인가

다른 사람이 알면 인증이나 권한을 얻을 수 있는 값을 **secret**이라고 부른다.

```text
DB password
API key
private key
JWT signing key
```

### LESSON 02 · 코드 저장소에 넣지 않는다

```javascript
const API_KEY = "real-secret-key";
```

를 Git에 commit하면 나중에 코드에서 삭제해도 과거 history에 남을 수 있다.

secret manager나 안전한 환경 설정 시스템을 사용한다.

---

## BLOCK 32 · 환경변수도 완벽한 금고는 아니다

### LESSON 01 · code와 secret 분리에는 도움

환경변수는 secret을 소스 코드에서 분리하는 데 사용할 수 있다.

### LESSON 02 · process가 읽을 수 있다

환경변수를 읽을 권한이 있는 process와 관리자는 값을 볼 수 있다.

로그에 전체 환경변수를 출력하면 secret이 노출될 수 있다.

---

## BLOCK 33 · SQL injection을 배우기 전에

### LESSON 01 · 사용자 입력을 SQL 문법에 붙인다

나쁜 코드:

```javascript
const sql = "SELECT * FROM users WHERE name = '" + name + "'";
```

사용자가 SQL 특수문자를 입력하면 원래 query 구조를 바꿀 수 있다.

### LESSON 02 · injection

사용자 데이터가 원래는 `값`이어야 하는데 프로그램의 `명령 문법`으로 해석되는 공격을 **injection**이라고 부른다.

---

## BLOCK 34 · SQL injection

### LESSON 01 · 데이터와 명령을 분리한다

SQL injection을 막는 핵심은 사용자 입력을 SQL 문자열에 직접 붙이지 않는 것이다.

parameterized query를 사용한다.

```javascript
await db.query(
  "SELECT * FROM users WHERE id = $1",
  [userId]
);
```

### LESSON 02 · escape를 직접 만들지 않는다

복잡한 SQL 문법을 개발자가 직접 문자열 치환으로 안전하게 escape하려 하면 실수하기 쉽다.

DB driver의 parameter 기능을 사용한다.

---

## BLOCK 35 · XSS를 배우기 전에

### LESSON 01 · 사용자 글이 HTML/JavaScript로 실행되면

댓글에 사용자가 넣은 글을 그대로 `innerHTML`로 넣는다고 하자.

공격자가 HTML script를 넣어 다른 사용자의 브라우저에서 실행시킬 수 있다.

### LESSON 02 · XSS

다른 사용자의 브라우저에서 공격자가 만든 script가 실행되는 웹 취약점을 **XSS(Cross-Site Scripting)**라고 부른다.

---

## BLOCK 36 · XSS 방어

### LESSON 01 · output encoding

사용자 문자열을 HTML에 표시할 때 `<`, `>` 같은 문자가 태그가 아니라 글자로 보이도록 안전하게 변환한다.

framework의 자동 escaping 기능을 활용한다.

### LESSON 02 · innerHTML 주의

```javascript
element.innerHTML = userInput;
```

처럼 신뢰하지 않는 입력을 직접 HTML로 넣지 않는다.

필요하면 검증된 HTML sanitizer를 사용한다.

### LESSON 03 · CSP

**Content Security Policy(CSP)**는 브라우저가 어떤 script와 resource를 실행·로드할 수 있는지 제한하는 HTTP 보안 정책이다.

XSS 피해를 줄이는 추가 방어층이 될 수 있다.

---

## BLOCK 37 · CSRF를 배우기 전에

### LESSON 01 · 브라우저는 cookie를 자동으로 보낼 수 있다

사용자가 bank.example에 로그인해 session cookie를 가지고 있다.

악성 사이트가 은행 송금 요청을 만들면 브라우저가 조건에 따라 은행 cookie를 자동으로 포함할 수 있다.

### LESSON 02 · CSRF

사용자가 의도하지 않은 요청을 인증된 브라우저를 통해 실행시키는 공격을 **CSRF(Cross-Site Request Forgery)**라고 부른다.

---

## BLOCK 38 · CSRF 방어

### LESSON 01 · CSRF token

서버가 정상 페이지에만 예측하기 어려운 token을 넣고 상태 변경 요청에 그 값을 요구할 수 있다.

악성 사이트는 이 token을 알기 어려워진다.

### LESSON 02 · SameSite cookie

Cookie의 `SameSite` 속성은 cross-site 요청에서 cookie 전송을 제한해 CSRF 위험을 줄이는 데 도움을 준다.

CORS는 CSRF를 자동으로 막아 주는 기능이 아니다.

---

## BLOCK 39 · SSRF

### LESSON 01 · 서버에게 URL을 대신 열게 한다

사용자가 URL을 입력하면 서버가 그 URL을 가져오는 기능이 있다고 하자.

공격자가:

```text
http://127.0.0.1:...
http://internal-service/...
```

같은 내부 주소를 넣어 서버 내부 자원에 접근하게 할 수 있다.

### LESSON 02 · SSRF

서버가 공격자가 지정한 네트워크 위치로 요청을 보내도록 악용하는 취약점을 **SSRF(Server-Side Request Forgery)**라고 부른다.

허용할 destination을 제한하고 내부 네트워크 접근을 막는 방어가 필요하다.

---

## BLOCK 40 · path traversal

### LESSON 01 · 파일 이름으로 상위 폴더 이동

사용자가 다운로드 파일 이름으로:

```text
../../secret.txt
```

를 넣었다.

서버가 경로에 그대로 붙이면 허용된 폴더 밖 파일을 읽을 수 있다.

### LESSON 02 · 이름 붙이기

`../` 등을 이용해 허용된 디렉터리 밖의 파일에 접근하려는 공격을 **path traversal**이라고 부른다.

서버가 허용된 파일 ID와 안전한 저장 경로를 사용해야 한다.

---

## BLOCK 41 · command injection

### LESSON 01 · 사용자 입력이 shell 명령이 된다

나쁜 코드:

```javascript
exec("convert " + filename + " output.png");
```

공격자가 shell 특수문자를 filename에 넣으면 다른 명령이 실행될 수 있다.

### LESSON 02 · 해결 방향

가능하면 shell 문자열을 조합하지 않는다.

프로세스 실행 API에 명령과 argument를 분리해 전달하고 입력을 허용 목록으로 제한한다.

---

## BLOCK 42 · 파일 업로드 보안

### LESSON 01 · 파일 이름을 믿지 않는다

`photo.png.exe`처럼 이름만 바꿀 수 있다.

확장자만 검사하면 부족하다.

### LESSON 02 · MIME을 믿지 않는다

client가 `Content-Type: image/png`라고 적어도 거짓일 수 있다.

서버는 파일 signature를 검사하거나 안전한 image decoder로 실제 내용을 검증할 수 있다.

### LESSON 03 · 업로드 위치

사용자가 업로드한 파일을 웹서버가 바로 실행 가능한 경로에 저장하면 위험하다.

실행 불가능한 별도 저장소에 보관하고 안전한 이름을 생성한다.

---

## BLOCK 43 · file signature

### LESSON 01 · 파일 앞의 특정 bytes

많은 파일 형식은 시작 부분에 특정 byte 패턴을 가진다.

이를 **file signature** 또는 magic bytes라고 부른다.

PNG는 특정 시작 bytes를 가진다.

### LESSON 02 · 완벽한 보안 검사는 아니다

공격자는 정상 이미지 안에 악성 데이터를 숨길 수 있고 parser 자체 취약점도 있을 수 있다.

필요하면 재인코딩, antivirus scanning, sandbox 처리 같은 추가 방어를 사용한다.

---

## BLOCK 44 · CORS 오해

### LESSON 01 · CORS는 서버 자체 접근 차단이 아니다

CORS는 브라우저 JavaScript가 cross-origin response를 읽을 수 있는지 제어하는 정책이다.

공격자는 브라우저가 아닌 자신의 HTTP client로 서버를 직접 호출할 수 있다.

### LESSON 02 · authorization을 CORS로 대신하지 않는다

`Origin이 우리 사이트니까 허용`만으로 중요한 데이터 접근 권한을 판단하면 안 된다.

서버의 authentication/authorization이 따로 필요하다.

---

## BLOCK 45 · session hijacking

### LESSON 01 · session ID를 훔치면

공격자가 사용자의 session cookie를 얻으면 서버가 공격자를 해당 사용자로 착각할 수 있다.

이를 **session hijacking**이라고 부른다.

### LESSON 02 · 방어

```text
HTTPS
Secure cookie
HttpOnly
SameSite
짧고 적절한 session 수명
로그인 시 session ID 재발급
의심 상황 재인증
```

등을 조합한다.

---

## BLOCK 46 · JWT 보안

### LESSON 01 · payload는 암호화가 아니다

JWT payload는 누구나 읽을 수 있는 경우가 일반적이다.

비밀번호나 secret을 넣지 않는다.

### LESSON 02 · signature 검증

서버는 JWT의 서명을 반드시 검증하고 허용한 알고리즘, issuer, audience, expiration 등을 확인해야 한다.

### LESSON 03 · 로그아웃 문제

자체 서명 JWT가 긴 시간 유효하면 서버에서 즉시 취소하기 어려울 수 있다.

짧은 access token, refresh token, revocation 정책 등을 설계한다.

---

## BLOCK 47 · MFA

### LESSON 01 · 여러 종류의 인증 증거

password 하나만이 아니라 다른 인증 요소를 함께 요구하는 것을 **MFA(Multi-Factor Authentication)**라고 부른다.

### LESSON 02 · factor 종류

```text
아는 것 = password/PIN
가진 것 = 보안키/휴대폰
본인 특성 = 생체 정보
```

서로 다른 factor 종류를 결합해야 의미가 크다.

---

## BLOCK 48 · passkey

### LESSON 01 · password 대신 공개키 기반 인증

**passkey**는 WebAuthn/FIDO 기술을 이용해 서비스마다 공개키 기반 자격증명을 사용하는 로그인 방식이다.

private key는 사용자 기기 또는 안전한 credential provider에 보호된다.

### LESSON 02 · phishing 저항

credential이 특정 웹사이트 origin과 연결되기 때문에 가짜 사이트에 password를 입력하게 만드는 전통적 phishing에 더 강하다.

---

## BLOCK 49 · brute force

### LESSON 01 · 가능한 값을 계속 시도

password 후보를 매우 많이 반복해서 시험하는 공격을 **brute-force attack**이라고 부른다.

### LESSON 02 · 방어

```text
강한 password hashing
rate limiting
MFA
credential stuffing 탐지
```

등을 조합한다.

단순 계정 잠금만 사용하면 공격자가 다른 사용자의 계정을 일부러 잠글 수도 있다.

---

## BLOCK 50 · credential stuffing

### LESSON 01 · 다른 사이트에서 유출된 password 재사용

공격자가 다른 서비스에서 유출된 email/password 조합을 우리 서비스에 자동으로 시험하는 것을 **credential stuffing**이라고 부른다.

### LESSON 02 · 사용자의 password 재사용 문제

서버가 안전하게 hash해도 사용자가 다른 사이트와 같은 password를 쓰면 피해가 이어질 수 있다.

MFA와 유출 password 차단이 도움이 된다.

---

## BLOCK 51 · dependency security

### LESSON 01 · 내가 작성하지 않은 코드도 앱의 일부다

npm package, Python library, Android dependency에 취약점이 있을 수 있다.

### LESSON 02 · supply chain

개발·빌드·배포 과정에 들어오는 외부 도구와 패키지를 공격하는 것을 **소프트웨어 공급망(supply chain) 공격**이라고 부른다.

패키지 이름 typo, 탈취된 maintainer 계정, 악성 update 등이 위험이 될 수 있다.

---

## BLOCK 52 · lock file과 integrity

### LESSON 01 · 어떤 dependency를 실제로 설치했는지 고정

lock file은 실제 dependency version과 hash 정보를 기록할 수 있다.

### LESSON 02 · hash 검증

package manager가 내려받은 package가 lock file에 기록된 integrity hash와 같은지 확인하면 전송 중 변조를 탐지하는 데 도움을 준다.

---

## BLOCK 53 · secret scanning

### LESSON 01 · Git에 key가 들어가는 실수 찾기

GitHub 등은 repository 안의 알려진 secret 형식을 탐지하는 **secret scanning** 기능을 제공할 수 있다.

### LESSON 02 · 이미 노출되면 삭제만으로 부족

secret이 commit되었다면:

```text
1. key 폐기/회전
2. 새 key 발급
3. code/history에서 제거 검토
4. 노출 기간 로그 조사
```

가 필요하다.

`git에서 삭제했으니 안전`이 아니다.

---

## BLOCK 54 · logging security

### LESSON 01 · 로그도 데이터 저장소다

로그에:

```text
password
access token
Authorization header
card number
private key
```

를 남기면 로그 시스템이 새로운 공격 대상이 된다.

### LESSON 02 · masking과 redaction

민감 값 일부를 가리는 것을 **masking**이라고 부른다.

완전히 제거하거나 `[REDACTED]`로 바꾸는 것을 **redaction**이라고 부른다.

---

## BLOCK 55 · error message leakage

### LESSON 01 · 내부 stack trace를 사용자에게 보내지 않는다

```text
/var/app/db/passwords.yml
SQL connection string
internal IP
framework version
```

같은 정보가 오류 화면에 노출되면 공격자에게 힌트를 줄 수 있다.

### LESSON 02 · 외부 메시지와 내부 로그 분리

사용자에게는 안전하고 이해하기 쉬운 오류 code/message를 보낸다.

상세 stack trace는 접근 통제된 내부 로그에 남긴다.

---

## BLOCK 56 · security header

### LESSON 01 · 브라우저 보안 정책 전달

서버는 HTTP header로 브라우저에게 추가 보안 정책을 전달할 수 있다.

예:

```text
Content-Security-Policy
Strict-Transport-Security
X-Content-Type-Options
```

### LESSON 02 · X-Content-Type-Options

```http
X-Content-Type-Options: nosniff
```

은 브라우저가 선언된 MIME type을 무시하고 임의로 다른 타입으로 추측하는 행동을 제한한다.

파일 업로드와 MIME 보안에 도움이 될 수 있다.

---

## BLOCK 57 · secure by default

### LESSON 01 · 기본값이 안전해야 한다

사용자가 아무 설정도 하지 않았을 때 가장 안전한 상태가 되도록 만드는 원칙을 **secure by default**라고 부른다.

```text
권한 기본 거부
HTTPS 기본
debug mode 운영에서 off
공개 bucket 기본 금지
```

### LESSON 02 · fail closed

보안 확인 과정에서 오류가 났을 때 `일단 허용`보다 `일단 거부`하는 방향을 **fail closed**라고 부른다.

인증 서버 장애를 `모두 관리자 처리`로 우회하면 안 된다.

---

## BLOCK 58 · defense in depth

### LESSON 01 · 방어 하나가 뚫려도 다음 방어가 있다

한 가지 보호 장치만 믿지 않고 여러 방어층을 겹치는 원칙을 **defense in depth(심층 방어)**라고 부른다.

예:

```text
input validation
+ parameterized SQL
+ least privilege DB account
+ monitoring
```

하나가 실패해도 전체 피해를 줄인다.

---

## BLOCK 59 · 업데이트와 보안 패치

### LESSON 01 · 오래된 dependency는 알려진 취약점을 가질 수 있다

보안 취약점이 공개되면 공격자도 내용을 알게 된다.

필요한 보안 patch를 적용하지 않고 오래된 version을 계속 사용하면 위험이 커진다.

### LESSON 02 · 무조건 최신 major로 즉시 올리는 것도 위험

breaking change 때문에 서비스가 중단될 수 있다.

보안 update는:

```text
취약점 영향 확인
패치 version 확인
테스트
단계적 배포
모니터링
```

순으로 안전하게 진행한다.

---

## BLOCK 60 · 보안 테스트

### LESSON 01 · 정상 입력만 테스트하면 부족

```text
빈 값
매우 긴 값
특수문자
권한 없는 사용자
만료 token
잘못된 file type
중복 요청
```

을 시험한다.

### LESSON 02 · SAST와 DAST 맛보기

코드를 실행하지 않고 취약 패턴을 찾는 분석을 **SAST**라고 부른다.

실행 중인 앱을 외부에서 시험하는 분석을 **DAST**라고 부른다.

둘은 서로 다른 문제를 찾을 수 있다.

---

## BLOCK 61 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| asset | 보안에서 지켜야 할 데이터·기능·서비스 |
| threat | 자산에 피해를 줄 수 있는 가능성 |
| vulnerability | 공격자가 이용할 수 있는 약점 |
| risk | 공격 가능성과 피해를 함께 본 위험 |
| trust boundary | 신뢰 수준이 바뀌는 시스템 경계 |
| threat modeling | 공격 경로와 방어를 설계 단계에서 분석하는 것 |
| confidentiality | 허가 없는 사람이 정보를 읽지 못하게 하는 성질 |
| integrity | 데이터가 허가 없이 바뀌지 않게 하는 성질 |
| availability | 필요할 때 서비스를 사용할 수 있는 성질 |
| authentication | 사용자가 누구인지 확인하는 것 |
| authorization | 사용자가 무엇을 할 수 있는지 판단하는 것 |
| least privilege | 필요한 최소 권한만 주는 원칙 |
| encoding | 정보를 다른 표현으로 바꾸는 규칙 |
| cryptographic hash | 데이터를 고정 길이 hash 값으로 만드는 보안용 함수 |
| SHA-256 | 대표적인 암호학적 hash 함수 |
| salt | password hash에 함께 넣는 사용자별 무작위 값 |
| encryption | key로 데이터를 숨기고 다시 복호화할 수 있게 만드는 것 |
| symmetric encryption | 같은 secret key를 사용하는 암호화 방식 |
| asymmetric cryptography | public/private key 쌍을 사용하는 암호 기술 |
| digital signature | private key로 서명하고 public key로 검증하는 기술 |
| certificate | 이름과 public key 정보를 신뢰 체계에 연결한 문서 |
| PKI | certificate와 CA를 포함한 공개키 신뢰 체계 |
| injection | 사용자 데이터가 명령 문법으로 해석되는 공격 |
| XSS | 공격자 script가 다른 사용자의 browser에서 실행되는 취약점 |
| CSRF | 인증된 사용자의 browser로 의도하지 않은 요청을 실행시키는 공격 |
| SSRF | 서버가 공격자가 고른 네트워크 위치로 요청하게 만드는 공격 |
| path traversal | 파일 경로 조작으로 허용 폴더 밖에 접근하는 공격 |
| session hijacking | session 식별값을 훔쳐 사용자로 가장하는 공격 |
| MFA | 서로 다른 인증 요소를 함께 요구하는 방식 |
| supply chain | 개발·빌드·dependency 경로를 통한 공격 영역 |
| defense in depth | 여러 보안 방어층을 겹치는 원칙 |

---

## BLOCK 62 · TRACK 09 완료 기준

다음을 직접 설명·적용할 수 있어야 한다.

- asset/threat/vulnerability/risk를 구분한다.
- trust boundary를 시스템 그림에 표시한다.
- authentication과 authorization을 구분한다.
- encoding/hash/encryption을 서로 다른 목적이라고 설명한다.
- SHA-256의 무결성 용도와 password 저장 용도를 구분한다.
- salt가 필요한 이유를 설명한다.
- bcrypt/Argon2 같은 password hashing을 사용한다.
- symmetric/asymmetric cryptography의 큰 차이를 설명한다.
- digital signature와 encryption을 구분한다.
- certificate/CA/TLS 연결을 설명한다.
- parameterized query로 SQL injection을 막는다.
- XSS가 발생하는 위험한 DOM 코드를 찾는다.
- CSRF와 CORS가 다른 문제임을 설명한다.
- SSRF/path traversal/command injection의 입력 경계를 찾는다.
- file upload에서 extension/MIME/signature를 모두 구분한다.
- secret을 source/log에 남기지 않는다.
- least privilege와 defense in depth를 설계에 적용한다.

### TRACK 프로젝트 · 취약한 메모 앱 보안 개선

일부러 취약한 앱을 만든다.

초기 취약점:

```text
password 평문 저장
SQL 문자열 결합
댓글 innerHTML 출력
관리자 API 권한 확인 없음
upload 파일 이름 그대로 저장
API key source code 포함
Authorization header 전체 logging
```

수정 순서:

1. password를 Argon2 또는 검증된 password hash library로 저장한다.
2. 사용자별 salt와 비용 설정을 확인한다.
3. SQL을 parameterized query로 바꾼다.
4. 사용자 댓글을 text로 렌더링하고 필요하면 sanitizer를 사용한다.
5. 모든 관리자 route에서 서버 authorization을 적용한다.
6. upload 파일의 크기·허용 type·실제 bytes를 검사한다.
7. 서버가 만든 안전한 파일 이름을 사용한다.
8. API key를 secret store/환경 설정으로 이동한다.
9. 로그의 token을 redaction한다.
10. CSP와 안전한 cookie 속성을 적용한다.
11. 각 취약점에 실패 테스트를 만든다.
12. 수정 전에는 공격이 성공하고 수정 후에는 실패하는 증거를 남긴다.

보안은 `보안 라이브러리 설치`로 끝나지 않는다. **어떤 공격 경로를 어떤 방어가 막는지 연결해서 설명할 수 있어야** 통과다.
