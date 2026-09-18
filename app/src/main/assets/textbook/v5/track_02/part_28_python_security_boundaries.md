# PART 28 · Python security boundary — 입력·명령·파일·직렬화에서 코드와 데이터를 분리하기

대부분의 애플리케이션 보안 문제는 언어 문법 자체보다 신뢰하지 않은 데이터가 코드·경로·쿼리·객체 생성처럼 더 강한 의미로 해석되는 경계에서 생긴다. Python에서도 문자열 하나가 shell command, SQL, template, path, pickle payload로 승격되는 순간 공격면이 열린다. 안전한 기본 원칙은 **데이터를 가능한 오래 데이터로 유지하고, 코드나 권한이 필요한 해석은 좁은 경계에서만 허용하는 것**이다.

---

## CHAPTER 01 · 신뢰 경계는 입력의 출처보다 공격자가 영향을 줄 수 있는지를 본다

### 시작 전 용어집

#### 1. 경계

- **뜻:** 경계마다 입력 크기, 허용 문자, schema, 권한, lifetime을 검증한다.
- **왜 중요한가:** Client에서 이미 검사했다는 이유로 server validation을 생략하지 않는다.
- **예시:** 경계마다 입력 크기, 허용 문자, schema, 권한, lifetime을 …

#### 2. 입력

- **뜻:** HTTP parameter, CLI argument, 환경 변수, 업로드 파일, 데이터베이스 row는 모두 외부에서 왔다는 이유만으로 동일하지 않다.
- **왜 중요한가:** 내부 DB 값도 과거 취약점을 통해 오염되었거나 다른 서비스가 쓴 값일 수 있다.
- **예시:** HTTP parameter, CLI argument, 환경 변수, 업로드 파일, …

#### 3. HTTP

- **뜻:** 어떤 데이터가 현재 보안 결정에 사용되는지 기준으로 trust를 판단한다.
- **왜 중요한가:** 공격자는 UI를 우회해 API를 직접 호출할 수 있다.
- **예시:** 어떤 데이터가 현재 보안 결정에 사용되는지 기준으로 trust를 …

#### 4. parameter

- **뜻:** 보안 validation과 business validation을 구분한다.
- **왜 중요한가:** 사용자 이름 길이 제한은 업무 규칙일 수 있고 path traversal 차단은 resource boundary 보호다.
- **예시:** 보안 validation과 business validation을 구분한다.

둘을 한 함수에 섞으면 정책 변경 때 위험하다.

---

## CHAPTER 02 · `eval`과 dynamic code execution은 데이터 입력을 프로그램으로 바꾼다

### 시작 전 용어집

#### 1. eval

- **뜻:** 문자열을 `eval`이나 `exec`로 실행하면 입력이 Python 코드의 권한을 얻게 된다.
- **왜 중요한가:** 사용자 수식이나 configuration을 편하게 처리하려고 이 기능을 사용하면 file/network/environment 접근까지 가능해질 수 있다.
- **예시:** 문자열을 `eval`이나 `exec`로 실행하면 입력이 Python 코드의 권한을 …

#### 2. dynamic code

- **뜻:** 제한된 expression이 필요하다면 parser와 AST를 만들어 허용 operation만 평가하는 편이 안전하다.
- **왜 중요한가:** `literal_eval` 같은 더 제한된 도구도 허용하는 데이터 크기와 nesting에 resource limit이 필요할 수 있다.
- **예시:** 제한된 expression이 필요하다면 parser와 AST를 만들어 허용 operation만 …

#### 3. execution

- **뜻:** 안전성은 함수 이름 하나가 아니라 어떤 문법과 객체 생성을 허용하는지에 달려 있다.
- **왜 중요한가:** Plugin처럼 코드 실행 자체가 요구사항이면 trust model을 명시한다.
- **예시:** 안전성은 함수 이름 하나가 아니라 어떤 문법과 객체 …

#### 4. 입력

- **뜻:** 같은 process에서 plugin을 실행한다는 것은 일반적으로 application과 같은 권한을 공유한다는 뜻이다.
- **왜 중요한가:** Untrusted code에는 process/container 수준 격리가 필요할 수 있다.
- **예시:** 같은 process에서 plugin을 실행한다는 것은 일반적으로 application과 같은 …

---

## CHAPTER 03 · shell injection은 문자열 결합이 명령 문법으로 재해석될 때 생긴다

### 시작 전 용어집

#### 1. shell injection

- **뜻:** `subprocess`에 사용자 입력을 포함한 shell command 문자열을 넘기면 `;`, `&&`, redirect 같은 shell metacharacter가 command 구조를 바꿀 수 있다.
- **왜 중요한가:** 가능한 경우 shell을 사용하지 않고 executable과 argv를 별도 list로 전달한다.
- **예시:** `subprocess`에 사용자 입력을 포함한 shell command 문자열을 넘기면 …

#### 2. subprocess

- **뜻:** Quoting을 직접 만드는 것은 platform별 shell rule 때문에 어렵다.
- **왜 중요한가:** Shell feature가 꼭 필요하지 않다면 제거하는 것이 가장 단순한 방어다.
- **예시:** Quoting을 직접 만드는 것은 platform별 shell rule 때문에 …

#### 3. 입력

- **뜻:** PATH lookup과 executable identity도 통제한다.
- **왜 중요한가:** Command 결과의 stdout/stderr에도 secret이 포함될 수 있다.
- **예시:** PATH lookup과 executable identity도 통제한다.

#### 4. list

- **뜻:** 실패 log에 전체 command line을 남길 때 token과 password argument를 redact한다.
- **예시:** 실패 log에 전체 command line을 남길 때 token과 …

---

## CHAPTER 04 · path traversal은 문자열 검사보다 filesystem boundary 문제다

### 시작 전 용어집

#### 1. path

- **뜻:** /`나 absolute path를 통해 허용 root 밖에 접근할 수 있다.
- **왜 중요한가:** Canonical path를 계산하고 허용 root 내부인지 검사하며, symlink와 race가 중요한 환경에서는 open 방식 자체를 안전하게 설계한다.
- **예시:** /`나 absolute path를 통해 허용 root 밖에 접근할 …

#### 2. filesystem boundary

- **뜻:** 사용자가 제공한 파일명을 base directory와 단순히 이어 붙이면 `.
- **왜 중요한가:** 업로드 파일 이름을 그대로 저장하는 대신 application이 새 ID 기반 이름을 생성하면 collision과 traversal 위험을 줄일 수 있다.
- **예시:** 사용자가 제공한 파일명을 base directory와 단순히 이어 붙이면 …

#### 3. process

- **뜻:** Application process가 읽을 필요 없는 directory까지 OS 권한을 가진다면 path bug 하나의 피해 범위가 커진다.
- **왜 중요한가:** Least privilege는 코드 validation 실패를 보완하는 방어층이다.
- **예시:** Application process가 읽을 필요 없는 directory까지 OS 권한을 …

. 

 원래 이름은 metadata로 보존한다.

Permission도 함께 본다.  

---

## CHAPTER 05 · 안전하지 않은 deserialization은 object construction과 code execution을 연결할 수 있다

### 시작 전 용어집

#### 1. serialization

- **뜻:** 모델 파일과 cache artifact도 serialization library의 security advisory를 추적한다.
- **왜 중요한가:** Pickle 계열처럼 Python object graph를 복원하는 format은 신뢰된 데이터 교환에는 편할 수 있지만 untrusted payload에서는 arbitrary code execution 위험이 있을 수 있다.
- **예시:** 모델 파일과 cache artifact도 serialization library의 security advisory를 …

#### 2. object construction

- **뜻:** 외부 API와 업로드에는 JSON처럼 data-oriented format과 schema validation을 선호한다.
- **왜 중요한가:** “우리 서버가 만든 pickle만 읽는다”면 저장 위치와 signature를 공격자가 바꿀 수 없는지 확인한다.
- **예시:** 외부 API와 업로드에는 JSON처럼 data-oriented format과 schema validation을 …

#### 3. code execution

- **뜻:** Internal network나 object storage가 자동으로 trusted가 되는 것은 아니다.
- **왜 중요한가:** Format 선택은 convenience가 아니라 threat model이다.
- **예시:** Internal network나 object storage가 자동으로 trusted가 되는 것은 …

---

## CHAPTER 06 · secret은 문자열 타입보다 더 제한된 lifecycle을 가져야 한다

### 시작 전 용어집

#### 1. secret

- **뜻:** API key와 password를 source repository에 넣지 않고 secret store나 deployment mechanism에서 주입한다.
- **왜 중요한가:** 그러나 환경 변수로 옮겼다고 모든 문제가 끝나는 것은 아니다.
- **예시:** API key와 password를 source repository에 넣지 않고 secret …

#### 2. 타입

- **뜻:** Log, exception, child process inheritance, crash dump에서 노출될 수 있다.
- **왜 중요한가:** Secret object를 출력할 때 기본 `repr`이 실제 값을 드러내지 않게 하고 config dump에서 redact한다.
- **예시:** Log, exception, child process inheritance, crash dump에서 노출될 …

#### 3. lifecycle

- **뜻:** 가능한 한 필요한 client를 생성한 뒤 raw secret을 application 전체에 전달하지 않는다.
- **왜 중요한가:** Rotation을 고려하면 secret value가 바뀔 수 있어야 한다.
- **예시:** 가능한 한 필요한 client를 생성한 뒤 raw secret을 …

#### 4. API

- **뜻:** Long-lived connection이 old credential을 계속 쓰는지, reload가 필요한지 운영 contract를 둔다.
- **예시:** Long-lived connection이 old credential을 계속 쓰는지, reload가 필요한지 …

---

## CHAPTER 07 · random은 simulation과 security에서 요구되는 품질이 다르다

### 시작 전 용어집

#### 1. random

- **뜻:** 일반 pseudo-random generator는 simulation, test fixture, game logic에 적합하지만 token과 password reset code에는 예측 저항성이 필요한 cryptographic random source를 사용한다.
- **왜 중요한가:** Seed를 알면 sequence를 재현할 수 있는 generator를 보안 secret에 쓰지 않는다.
- **예시:** 일반 pseudo-random generator는 simulation, test fixture, game logic에 …

#### 2. simulation

- **뜻:** Test에서는 deterministic seed가 장점이지만 production security에서는 반대다.
- **왜 중요한가:** 같은 `random`이라는 단어 아래 두 요구를 분리한다.
- **예시:** Test에서는 deterministic seed가 장점이지만 production security에서는 반대다.

#### 3. security

- **뜻:** Token 길이도 entropy budget과 encoding을 함께 본다.
- **왜 중요한가:** 사람이 읽기 쉬운 짧은 code는 online rate limit과 expiration 같은 추가 방어가 필요하다.
- **예시:** Token 길이도 entropy budget과 encoding을 함께 본다.

---

## CHAPTER 08 · authorization은 객체 존재 확인과 분리해 우회 경로를 줄인다

### 시작 전 용어집

#### 1. authorization

- **뜻:** UI에서 버튼을 숨기는 것은 authorization이 아니다.
- **왜 중요한가:** Service/API boundary에서 resource와 principal 관계를 검증한다.
- **예시:** UI에서 버튼을 숨기는 것은 authorization이 아니다.

#### 2. 객체

- **뜻:** 사용자 입력 ID로 객체를 찾은 뒤 실제로 그 사용자가 해당 객체를 읽거나 수정할 권한이 있는지 검사해야 한다.
- **왜 중요한가:** Object-level authorization을 매 endpoint에서 ad-hoc 조건으로 쓰면 한 경로에서 빠뜨리기 쉽다.
- **예시:** 사용자 입력 ID로 객체를 찾은 뒤 실제로 그 …

#### 3. 입력

- **뜻:** Policy function이나 repository query에 ownership 조건을 포함하는 방식으로 반복 가능한 경계를 만든다.
- **왜 중요한가:** Error message가 “존재하지만 권한 없음”과 “존재하지 않음”을 구분해 민감 resource existence를 노출하는지 threat model에 따라 판단한다.
- **예시:** Policy function이나 repository query에 ownership 조건을 포함하는 방식으로 …

---

## CHAPTER 09 · dependency와 build도 실행되는 코드의 신뢰 경계다

### 시작 전 용어집

#### 1. dependency

- **뜻:** Package 설치 시 setup/build script와 dependency code가 개발·CI 환경에서 실행될 수 있다.
- **왜 중요한가:** 이름이 비슷한 악성 package, dependency confusion, compromised release를 고려해 source와 version을 통제한다.
- **예시:** Package 설치 시 setup/build script와 dependency code가 개발·CI …

#### 2. build

- **뜻:** Build log에 secret이 노출되지 않게 한다.
- **왜 중요한가:** Lock, hash, registry policy는 artifact identity를 강화하지만 취약점 자체를 검사하는 단계도 필요하다.
- **예시:** Build log에 secret이 노출되지 않게 한다.

#### 3. 경계

- **뜻:** Update를 자동으로 받기만 하거나 영구 pinning만 하는 대신 advisory와 test를 연결한 upgrade process를 둔다.
- **왜 중요한가:** CI token과 package publish credential은 최소 권한과 짧은 lifetime을 선호한다.
- **예시:** Update를 자동으로 받기만 하거나 영구 pinning만 하는 대신 …

---

## CHAPTER 10 · 안전한 Python 설계는 해석 권한이 커지는 지점을 줄이는 것이다

### 시작 전 용어집

#### 1. Python

- **뜻:** Python security의 핵심은 **신뢰하지 않은 데이터를 강한 interpreter에 직접 넘기지 않고, 필요한 의미만 단계적으로 부여하며 각 경계에서 권한과 resource budget을 다시 확인하는 것**이다.
- **왜 중요한가:** Raw string이 domain value로, path로, shell command로, query로, object로, code로 바뀌는 순간마다 의미와 권한이 커진다.
- **예시:** Python security의 핵심은 **신뢰하지 않은 데이터를 강한 interpreter에 …

#### 2. value

- **뜻:** 이 변환을 adapter 하나에 모으고 allowlist/schema/parameterization을 사용하면 검증 지점이 선명해진다.
- **왜 중요한가:** 보안은 input sanitation 함수 하나가 아니다.
- **예시:** 이 변환을 adapter 하나에 모으고 allowlist/schema/parameterization을 사용하면 검증 …

#### 3. path

- **뜻:** OS permission, process isolation, timeout, size limit, dependency provenance, logging policy가 서로 다른 failure를 막는다.
- **왜 중요한가:** 한 층이 실패해도 피해 범위를 제한하도록 defense in depth를 만든다.
- **예시:** OS permission, process isolation, timeout, size limit, dependency …
