## V1-C06. 설치·버전·설정과 의존성

**원본 Lesson 매핑:** V1-17, V1-18, V1-19

### 1. 현실 문제에서 시작하기

앱을 업데이트할 때마다 APK를 다시 받아야 하는지, 어떤 앱은 자동으로 업데이트되는지, 업데이트 후 왜 갑자기 깨지는지 이해하려면 “설치파일 하나”가 아니라 패키지·버전·의존성·설정·데이터 migration을 함께 봐야 한다. AI 코딩에서도 “코드만 고쳤는데 빌드가 안 됨”의 상당수는 코드 자체가 아니라 dependency나 환경 차이다.

이 장은 패키지 관리 도구를 외우는 것이 아니라 **재현 가능한 설치와 안전한 업데이트**를 이해하는 장이다.

### 2. Mental Model

```text
소스코드
 + dependency 목록/lock
 + build 설정
 + secret 제외 설정
        ↓ build
패키지(APK 등)
        ↓ install/update
앱 바이너리 + 사용자 데이터 + OS 권한/설정
        ↓
실행
```

업데이트는 단순 파일 교체가 아니다. 기존 데이터와 새 코드의 호환성, 최소 OS 버전, 라이브러리 버전, 서명, 설정 schema가 함께 맞아야 한다.

### 3. 핵심 개념 해부

#### 패키지와 package manager

패키지는 배포를 위해 코드와 자원을 정해진 형태로 묶은 것이다. Android의 APK, Python package, OS 패키지는 목적은 비슷하지만 규칙이 다르다. package manager는 설치·업데이트·의존성 해결을 도와준다.

어떤 도구를 설치할 때 본체만 보는 것이 아니라 그 도구가 요구하는 runtime, native library, 권한, OS version이 있는지 확인해야 한다.

#### dependency와 lock

현대 앱은 모든 코드를 직접 쓰지 않고 라이브러리를 조합한다. dependency가 자동 업데이트되면 새 버전의 breaking change가 내 코드와 충돌할 수 있다. 반대로 너무 오래 고정하면 보안패치와 호환성 개선을 놓친다.

lockfile은 설치되는 구체적 버전을 고정해 재현성을 높이는 수단이다. “내 컴퓨터에서는 됨”을 줄이는 데 매우 중요하다.

#### 버전과 호환성

semantic versioning의 `major.minor.patch`는 널리 쓰이는 의사소통 규칙이지만 모든 프로젝트가 완벽히 따르는 것은 아니다. 그래서 숫자만 믿지 않고 release note, minimum version, deprecation, migration guide를 확인한다.

Android 앱 업데이트에서는 앱 서명과 versionCode 같은 배포 규칙도 작동한다. 사용자는 단순히 이름이 같은 APK라고 같은 앱의 정상 업데이트로 인식되지 않을 수 있다.

#### migration과 rollback

새 버전이 데이터 구조를 바꿀 때 기존 사용자 데이터는 새 구조로 변환되어야 한다. 이것이 migration이다. migration이 실패하면 앱이 시작되지 않거나 데이터가 누락될 수 있다. 그래서 업데이트 전 백업, 단계적 rollout, rollback 경로를 고려한다.

rollback도 무조건 쉬운 것이 아니다. 새 버전이 데이터를 비가역적으로 바꿨다면 구버전 앱이 새 데이터를 읽지 못할 수 있다. 코드와 데이터의 버전관계를 같이 설계해야 한다.

#### 설정과 secret 분리

환경별 API URL, 기능 플래그, 사용자 설정, 빌드 설정, 비밀키는 성격이 다르다. 모두 한 파일에 넣으면 배포 실수와 보안사고가 생긴다. 일반 설정은 버전관리할 수 있지만 secret은 공개 저장소에 넣지 않는다.

좋은 설정은 기본값과 필수값, 유효성 검사를 명확히 한다. 설정이 없을 때 조용히 이상한 동작을 하기보다 시작 단계에서 명확한 오류를 내는 편이 안전할 수 있다.

### 3A. 개념을 연결해서 생각하기 — “업데이트”는 코드가 아니라 시스템 상태 전환

사용자는 업데이트를 새 APK 설치 정도로 보지만 실제로는 **old state → migration → new state** 전환이다. old state에는 코드뿐 아니라 사용자 DB, cache, 설정, 권한, 로그인 token, OS version, dependency가 포함된다. 그래서 신규설치 테스트만으로 업데이트 안전성을 증명할 수 없다.

dependency 관리는 신뢰와 재현성 문제다. 내 프로젝트 소스가 동일해도 외부 라이브러리가 다른 버전으로 설치되면 실행결과가 달라질 수 있다. lockfile이나 version catalog가 특정 dependency graph를 재현하도록 돕는 이유다. 하지만 lock 자체가 최신성/보안을 보장하지는 않는다. 주기적으로 advisory와 release note를 검토해야 한다.

버전전략은 사용자에게 의미를 전달한다. major/minor/patch 같은 SemVer는 유용한 약속이지만 법칙이 아니다. Android의 versionCode처럼 플랫폼이 업데이트 순서를 판단하는 값과 사용자에게 보여주는 versionName도 구분한다. 앱 이름만 같다고 업데이트가 가능한 것이 아니며, 서명키와 package identity가 맞아야 한다.

migration은 schema change만 뜻하지 않는다. 파일포맷, 설정 key, API authentication 방식, cache structure가 바뀌어도 migration이 필요할 수 있다. 특히 “기존 사용자만 실패”는 old data와 new code의 만남에서 생기는 문제일 가능성이 높다. 따라서 테스트 fixture에 **구버전 상태 샘플**을 포함해야 한다.

rollback은 update의 반대방향이라고 단순화할 수 없다. 새 버전이 DB를 irreversible하게 바꿨다면 구버전 코드가 새 schema를 이해하지 못할 수 있다. 안전한 rollout은 feature flag, backward-compatible schema, staged migration, backup을 조합한다. 모든 프로젝트가 이 복잡성을 필요로 하지는 않지만 “구버전 APK 재설치=완전 rollback”이라는 생각은 버린다.

설정 분리도 deployment 품질과 연결된다. development/staging/production URL이 코드 여러 곳에 하드코딩되어 있으면 환경 전환 중 일부만 바뀌는 사고가 생긴다. configuration source를 명확히 하고 앱 시작 시 validation을 수행하면 잘못된 서버에 조용히 연결되는 위험을 줄일 수 있다.

### 4. Worked Examples

#### Worked Example A — 업데이트 후 앱 시작 실패

새 버전 배포 후 기존 사용자만 crash한다. 신규 설치 사용자는 정상이다. 이 패턴은 기존 데이터 migration을 강하게 의심하게 한다.

진단:
1. 신규/기존 설치 차이를 재현한다.
2. DB schema/config version을 비교한다.
3. migration 로그를 본다.
4. 데이터 백업본으로 반복 테스트한다.
5. 수정 후 구버전 데이터 → 신버전 업그레이드 회귀시험을 추가한다.

#### Worked Example B — dependency 차이

개발자 A의 빌드는 성공하고 CI는 실패한다. A의 환경에는 전역 설치된 라이브러리가 있고 CI에는 없다. 의존성을 명시적 파일과 lock으로 선언하고 깨끗한 환경에서 재빌드해야 한다. 숨은 dependency를 제거하는 것이 핵심이다.

### 4A. 미니 사례집

**사례 1 — 업데이트 직후 기존 DB만 crash**  
fresh install은 정상. migration path를 재현한다. 구버전 DB fixture가 없었다면 테스트 공백이다.

**사례 2 — CI가 갑자기 깨짐**  
dependency range가 넓어 새 patch가 내려왔다. lock diff와 package registry release를 확인한다.

**사례 3 — APK가 설치는 되지만 기존 앱 위에 update 안 됨**  
application/package identity, signing certificate, version code/order를 확인한다.

**사례 4 — 개발 앱이 운영 DB에 접속**  
base URL/config injection이 환경마다 명확히 분리되지 않았다. build-time/runtime config boundary를 재설계한다.

### 5. 그럴듯하지만 틀린 판단

1. 코드가 같으면 어느 컴퓨터에서도 반드시 같은 빌드가 나온다고 생각한다.
2. minor 업데이트는 절대 breaking change가 없다고 단정한다.
3. rollback은 예전 APK만 다시 설치하면 끝난다고 생각한다.
4. .env 파일이면 자동으로 안전하다고 생각한다.

### 5A. 배포 전 최소 Gate

1. clean build 성공
2. dependency lock/변경 목록 확인
3. secret scan
4. 신규 설치 테스트
5. **구버전→신버전 update 테스트**
6. schema/config migration 확인
7. 서명/버전 검증
8. 실제 target Android 기기 smoke
9. crash/log 확인
10. rollback 또는 recovery 경로 기록

업데이트 성공은 “APK가 생성됨”이 아니라 사용자의 기존 상태가 안전하게 새 버전으로 전환되는 것까지 포함한다.

### 5B. 업데이트 사고를 줄이는 배포 전략

**staged rollout**은 모든 사용자에게 한 번에 배포하지 않고 일부 사용자나 테스트군부터 새 버전을 적용해 문제를 조기에 발견하는 방식이다. 개인 배포 앱에서도 가족 한 명의 테스트폰 → 사용자 본인 보조폰 → 전체 가족 순서처럼 작은 단계적 배포를 할 수 있다.

**feature flag**는 앱 버전을 되돌리지 않고 문제가 있는 기능만 끌 수 있는 안전장치다. 하지만 flag가 많아지면 오래된 분기와 테스트 조합이 늘어난다. 임시 flag에는 제거일정을 둔다.

**backward-compatible migration**은 새 코드와 구버전 코드가 잠시 같은 데이터 구조를 이해할 수 있게 변경을 여러 단계로 나누는 전략이다. 예를 들어 새 column을 먼저 추가하고 읽기 코드를 배포한 뒤, 나중에 old column 의존성을 제거한다. 작은 개인 앱에서는 과할 수 있지만 운영 중 사용자 데이터를 지키는 사고방식은 유용하다.

dependency update는 “최신이 좋다”와 “안 바꾸는 게 안전하다” 사이의 선택이 아니다. security advisory, 지원 종료, 필요한 기능, breaking risk를 평가하고 작은 batch로 올린다. 변경 후 unit/integration/build/smoke를 다시 실행한다.

배포 artifact는 version name만 기록하지 말고 commit SHA와 SHA-256을 연결한다. 그러면 “이 APK가 정확히 어느 코드에서 나온 것인가?”를 나중에 증명할 수 있다. AI가 빌드했다고 말하는 것보다 실제 artifact hash와 CI run이 더 강한 증거다.

### 6. Guided Lab

가상의 앱 배포표를 만든다: 앱 버전, 최소 OS, DB schema version, dependency lock hash, build date, APK SHA-256. 두 버전을 비교해 어떤 사용자가 업데이트에 실패할 수 있는지 예측한다. 설정 파일 샘플에서는 일반 설정과 secret 후보를 색으로 나눈다.

### 7. Independent Lab

업데이트 체크리스트를 작성하라: 백업, release note, dependency, migration, signing, install test, rollback, 실제 기기 smoke test, 로그 관찰. 자신의 앱에 필요한 항목과 불필요한 항목을 구분한다.

### 8. Debug Challenge

**상황:** 특정 기기에서만 신버전 설치 후 실행 즉시 종료. 원인은 아직 모른다. OS version, ABI/native dependency, 기존 데이터, 권한, 서명, config 차이를 비교표로 만든 뒤 하나씩 제거한다. APK를 반복 다운로드하는 행동 자체를 진단으로 착각하지 않는다.

### 9. AI Audit

AI가 dependency 문제에 대해 “모든 라이브러리를 최신으로 업데이트”라고 제안하면 반려할 가능성이 높다. 현재 lock, 변경되는 버전 수, breaking change, 보안 필요성, rollback, 테스트 범위를 확인한 뒤 **최소 변경**으로 해결한다.

### 9A. 진단 미니드릴 — 증상을 보면 첫 질문부터 고른다

**드릴 1. 신규설치는 정상, 업데이트만 crash**  
첫 판단: 구버전 데이터/config migration 경로를 최우선 비교한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 2. 같은 commit인데 빌드 artifact가 다름**  
첫 판단: dependency lock, build tool, environment, timestamped/nonreproducible inputs를 확인한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 3. APK 이름은 같은데 update 거부**  
첫 판단: package identity, signing certificate, versionCode를 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 4. dependency 하나 올렸는데 30개가 변경**  
첫 판단: transitive dependency와 lock diff를 검토한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 5. rollback했는데 DB를 못 읽음**  
첫 판단: 신버전 migration이 backward incompatible했는지 확인한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 6. production URL이 staging으로 바뀜**  
첫 판단: config source와 build variant injection을 추적한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

이 미니드릴의 목표는 정답 암기가 아니다. **증상 → 계층 → 확인할 증거**를 30초 안에 연결하는 습관을 만드는 것이다.

### 10. 회상 문제

1. 패키지와 dependency의 차이는?
2. lockfile이 재현성을 높이는 이유는?
3. 버전 숫자만 보고 호환성을 확정하면 안 되는 이유는?
4. migration이 필요한 대표 상황은?
5. rollback이 데이터 때문에 어려울 수 있는 이유는?
6. 일반 설정과 secret을 왜 분리해야 하는가?
7. 신규 설치는 정상인데 기존 사용자만 실패할 때 무엇을 의심할 수 있는가?
8. 깨끗한 환경 빌드가 왜 중요한가?

### 11. 전이 문제

1. 가족 앱을 수동 APK 배포 중이다. 자동 업데이트 구조를 넣기 전 확인할 배포·서명·버전 요소를 적어라.
2. AI가 라이브러리 20개를 한 번에 올렸다. 안전하게 검토할 순서를 설계하라.
3. 설정값 하나가 빠지면 앱이 조용히 잘못된 서버를 본다. 더 안전한 fail-fast 설계를 제안하라.

### 12. Chapter 완료 증거

- Guided Lab의 실행/관찰 결과를 남긴다.
- 정상 케이스뿐 아니라 실패·경계 케이스를 최소 1개 보존한다.
- “무엇을 바꿨는가 / 왜 바꿨는가 / 무엇으로 맞음을 확인했는가”를 5문장 이내로 적는다.
- AI를 사용했다면 AI 답의 오류·누락·과잉변경 여부를 체크한 기록을 남긴다.
- 결과가 예상과 다르면 PASS라고 쓰지 않고, 재현 조건과 다음 실험을 기록한다.

### 13. 참고 렌즈

- Release It!, 2e — Michael Nygard
- Semantic Versioning specification
- Android Developers — app signing, versioning, build/deployment docs
- MIT The Missing Semester
- Software Engineering at Google

---
