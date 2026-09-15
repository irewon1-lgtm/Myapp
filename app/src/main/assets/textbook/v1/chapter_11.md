## V1-C11. 종합 시스템 장애 진단 프로젝트

**원본 Lesson 매핑:** V1-28

### 1. 현실 문제에서 시작하기

앞 장까지 개념을 따로 배웠다면 이제 한 장애를 처음부터 끝까지 해결한다. 실제 장애는 “DNS 문제”, “메모리 문제”처럼 친절하게 이름표를 달고 오지 않는다. 사용자는 “어제부터 안 돼요”, “업데이트 후 이상해요”, “와이프 폰만 알림이 안 와요”처럼 증상을 말한다.

이 장에서는 정답보다 **진단 과정의 품질**을 평가한다. 무엇을 재현했고, 어떤 계층을 분리했고, 어떤 증거로 후보를 제거했으며, 어떤 최소변경을 했고, 어떻게 회귀검증했는지를 보고서로 남긴다.

### 2. Mental Model

```text
1. 증상 정의
2. 재현 조건 고정
3. 계층 분리
4. 증거 수집
5. 가설 3개 이상
6. 가장 싼 실험부터
7. 최소 수정
8. 동일 조건 재검증
9. 경계/실패 케이스
10. 복구/rollback
11. 기록/인수
```

“해결됨”은 다시 켜보니 한 번 됐다는 뜻이 아니다. 원인을 설명할 수 있고, 동일 조건에서 반복 검증하고, 이전 정상 기능이 깨지지 않았음을 확인해야 한다.

### 3. 핵심 개념 해부

#### 증상을 검증 가능한 문장으로 바꾸기

“앱이 이상하다”를 `Galaxy A에서 Wi-Fi 사용 시 09:00 알림이 3회 중 3회 오지 않지만 수동 테스트 알림은 수신된다`처럼 바꾼다. 기기, 계정, 네트워크, 시간, 빈도, 기대결과를 명시하면 재현과 비교가 가능해진다.

좋은 문제정의는 원인을 포함하지 않는다. “FCM 버그 때문에 알림이 안 온다”는 이미 결론을 섞은 문장이다.

#### 계층분리와 비교군

앱/UI, local state, OS permission/background, network, backend scheduler, push provider, account/device token 같은 계층을 나눈다. 정상 기기와 실패 기기를 비교하면 많은 변수를 빠르게 제거할 수 있다.

비교군이 없으면 모든 설정이 의심스럽지만, 엄마 폰은 되고 배우자 폰만 안 된다면 공통 서버 코드보다 기기별 token/permission/account 차이가 우선 후보가 될 수 있다.

#### 가설·실험·증거

가설은 최소 세 개 만든다. 가장 가능성 높은 것 하나에 집착하면 confirmation bias가 생긴다. 각 가설에는 “맞다면 어떤 증거가 보여야 하는가”를 적는다.

실험은 한 번에 한 변수를 바꾼다. 비용과 위험이 낮고 정보량이 큰 실험부터 한다. 전체 재설치나 DB 초기화는 정보와 데이터를 지워버릴 수 있어 보통 초반 실험으로 부적절하다.

#### 최소수정과 rollback

원인이 확인되면 관련된 최소 범위만 수정한다. 여러 기능을 동시에 리팩터링하면 새 오류가 섞여 원인 추적이 어려워진다. 변경 전 상태를 Git commit, 백업, 설정 snapshot 등으로 보존해 rollback 가능하게 한다.

AI 코딩에서는 특히 “고쳐줘” 한 마디에 AI가 파일 20개를 바꾸지 않게 acceptance criteria와 보존조건을 준다.

#### 회귀검증과 증거 패키지

수정된 실패 케이스만 통과하면 충분하지 않다. 정상 경로, 경계조건, 관련 기능을 다시 검증한다. 자동화 테스트가 있으면 실행하고, 실제 기기 조건도 필요한 만큼 확인한다.

최종 보고서에는 재현 절차, 원인, 수정 diff, 테스트 결과, 남은 위험, rollback 방법을 남긴다. 다음 사람이 같은 장애를 처음부터 다시 조사하지 않게 하는 것이 목표다.

### 3A. 종합 방법론 — 좋은 디버깅은 과학실험과 닮아 있다

장애 대응을 잘하는 사람은 오류메시지를 많이 외운 사람이 아니라 **가설을 싸게 틀리게 만드는 사람**이다. 즉, 큰 변경을 하기 전에 작은 실험으로 후보를 제거한다. 예를 들어 알림이 안 올 때 앱을 재설치하기 전에 notification permission, device token 등록시각, provider response를 확인하면 데이터를 지우지 않고 더 많은 정보를 얻을 수 있다.

첫 단계는 reproducibility다. 10번 중 10번 실패하는지, 특정 계정/기기/네트워크/시간에만 실패하는지 확인한다. 재현이 불안정하면 조건을 하나씩 고정한다. “가끔”이라는 단어를 빈도와 조건으로 바꾼다.

둘째는 **관찰과 추론 분리**다. `provider response=200`은 관찰이고 “따라서 기기에 도착했다”는 추론이다. push provider가 accepted했다고 실제 notification이 사용자에게 표시됐다는 뜻은 아닐 수 있다. 보고서에서 사실과 해석을 문장 수준으로 구분하면 AI가 만든 과잉결론도 잡기 쉽다.

셋째는 binary search식 범위축소다. 전체 흐름이 10단계면 중간 지점의 증거를 확인해 앞/뒤 절반을 나눈다. API response가 앱까지 정상 도착했다면 DNS/server보다 parse/state/UI 쪽을 본다. 이렇게 계층을 절반씩 제거하면 “다 해보기”보다 훨씬 빠르다.

넷째는 minimal change다. 버그를 고치면서 코드 정리, UI 변경, dependency update까지 한꺼번에 하면 원인과 효과를 분리할 수 없다. 장애수정 commit은 가능한 작게 유지하고, 개선/refactor는 별도 작업으로 분리한다.

다섯째는 regression이다. 버그가 다시 생기지 않게 실패 케이스를 테스트로 고정한다. 자동화하기 어려운 실제 기기 문제라면 재현 runbook과 smoke checklist라도 만든다. 중요한 것은 다음번 사람이 같은 조사비용을 다시 치르지 않게 하는 것이다.

마지막은 **복구 우선순위**다. 운영 장애에서는 완벽한 근본수정보다 사용자 영향 완화가 먼저일 수 있다. feature flag로 문제 기능을 끄고, 안전한 이전 버전으로 rollback하고, 이후 root cause를 분석하는 순서가 적절할 수 있다. 단, 임시조치와 영구수정을 명확히 기록한다.

### 4. Worked Examples

#### Worked Example A — 가족 알림 한 기기만 실패

증상: 엄마 폰은 일정 변경 push를 받고 배우자 폰은 못 받는다.

분리:
- 서버가 일정 이벤트를 생성했는가?
- 수신자 목록에 배우자 계정/기기가 포함됐는가?
- device token이 최신인가?
- push provider 요청은 성공했는가?
- 기기 notification permission/channel이 켜졌는가?
- 앱이 force-stop/배터리 제한 상태인가?

정상 기기와 실패 기기의 token 등록 로그·permission·provider response를 비교한다. 서버를 무작정 재배포하지 않는다.

#### Worked Example B — 업데이트 후 데이터 사라짐

신규 설치는 정상, 기존 업데이트 사용자만 데이터가 빈다. DB migration/계정 선택/sync restore 후보를 세운다. 구버전 샘플 DB를 복사해 신버전 upgrade test를 재현한다. migration 전후 row count와 schema version을 기록한다. 수정 후 실제 구버전→신버전 경로를 반복한다.

### 4A. 종합 사례 4개

**사례 A — 가족앱 배우자만 알림 실패**  
정상폰과 실패폰을 비교한다. server event와 provider send가 공통 정상이라면 device token, permission, notification channel, battery restriction 쪽 정보가 더 값지다.

**사례 B — 국내주식 PER 결측률 높음**  
UI가 빈 칸을 보인다는 사실에서 data source availability → parser → normalization → DB 저장 → API → UI mapping 순으로 trace한다. 다른 지표가 정상이라는 비교군을 활용한다.

**사례 C — APK 업데이트가 일부 기기에서 실패**  
파일 corruption, signing, version, minSdk/ABI, 저장공간, 기존 설치 package identity를 분리한다. 실패 기기 로그/installer message를 확보한다.

**사례 D — 자동화가 중복 실행**  
scheduler가 두 번 trigger됐는지, worker가 retry됐는지, lock/idempotency가 없는지 job id로 추적한다. 중복 결과만 삭제하는 것은 재발방지가 아니다.

### 5. 그럴듯하지만 틀린 판단

1. 한 번 재실행해서 되면 해결됐다고 생각한다.
2. 가장 마지막에 수정한 코드가 원인이라고 단정한다.
3. 재설치·초기화를 첫 단계로 실행해 증거를 지운다.
4. AI가 PASS라고 적어준 로그를 실제 테스트 실행 증거로 착각한다.

### 5A. 최종 장애 보고서 승인 질문

1. 문제를 재현 가능한 문장으로 정의했는가?
2. 영향을 받는/받지 않는 비교군이 있는가?
3. 시간축이 있는가?
4. 가설을 최소 3개 만들었는가?
5. 각 가설을 기각/지지한 증거가 있는가?
6. root cause와 symptom fix를 구분했는가?
7. 수정 diff가 최소화됐는가?
8. 원래 실패케이스가 실제로 다시 통과했는가?
9. 정상기능 회귀검증을 했는가?
10. rollback/복구 경로가 있는가?
11. 실행하지 않은 검증을 PASS라고 표시하지 않았는가?
12. 다음 사람이 재현할 수 있는 기록이 남았는가?

12개 중 하나라도 근거가 없다면 최종 COMPLETE가 아니라 **미검증/부분완료**로 표시한다.

### 5B. 실제 장애에서 자주 실패하는 행동 패턴

**1. 해결책부터 실행한다.**  
검색결과나 AI가 제시한 첫 방법을 바로 실행하면 원인 후보를 검증할 기회를 잃는다. 먼저 증거를 저장한다.

**2. 여러 변경을 동시에 한다.**  
앱 업데이트, cache 삭제, 계정 재로그인, 공유기 재부팅을 한꺼번에 하면 문제가 사라져도 무엇이 원인이었는지 모른다. 다음에 재발한다.

**3. 정상 비교군을 쓰지 않는다.**  
실패폰 한 대만 들여다보는 것보다 정상폰과 앱 버전, 권한, token, account, network를 표로 비교하는 편이 빠르다.

**4. 실행하지 않은 테스트를 보고서에 통과로 적는다.**  
코드가 이론상 맞아 보여도 build, install, actual run은 별도 증거다. 환경이 없으면 `미실행`이라고 적는다.

**5. root cause와 resilience를 혼동한다.**  
서버 timeout이 원인이더라도 앱이 crash하지 않고 오류를 보여주는 방어코드는 필요하다. 반대로 방어코드만 넣었다고 서버 timeout 원인이 해결된 것은 아니다.

**6. 데이터 파괴적 조치를 너무 일찍 한다.**  
재설치·DB 초기화는 증거와 사용자 데이터를 지울 수 있다. backup/snapshot 후 마지막 수단으로 쓴다.

**7. 완료기준이 없다.**  
“되는 것 같음” 대신 재현 N회, 자동테스트, 실제 기기 smoke, 관련기능 회귀 같은 acceptance criteria를 미리 정한다.

이 장을 통과한 학습자는 모든 문제를 혼자 즉시 고칠 필요는 없다. 대신 **모르는 문제를 안전하게 좁히고, AI나 전문가에게 전달할 증거를 만들 수 있어야 한다.**

### 6. Guided Lab

제공된 장애 시나리오를 사용해 1쪽 보고서를 작성한다.

- 제목/영향
- 재현 조건
- 기대 vs 실제
- 시간축
- 계층별 확인
- 가설 3개
- 실험과 결과
- root cause
- 최소 수정
- 회귀시험
- rollback
- 남은 위험

각 주장 옆에 로그·스크린샷·테스트·설정값 중 하나의 증거를 붙인다.

### 7. Independent Lab

자신의 실제 앱에서 최근 겪은 문제 하나를 고른다. 해결된 문제여도 좋다. 당시 행동을 다시 보면서 ‘증거 없이 했던 조치’를 표시하고, 같은 문제가 다시 생기면 사용할 진단 runbook을 작성한다.

### 8. Debug Challenge

**상황 — 종합 시나리오:** 앱 업데이트 이후 일부 Galaxy에서 첫 실행은 정상이나 두 번째 실행부터 빈 화면. Wi-Fi/모바일 데이터 모두 동일. 서버는 200, API JSON 정상. 앱 로그에는 로컬 cache deserialize 오류가 있다.

재설치하면 일시적으로 정상인 이유까지 설명해야 한다. 캐시 포맷/version migration을 후보로 확인하고, 기존 cache 데이터 재현fixture를 만들어 수정 전 실패/수정 후 성공을 증명한다.

### 9. AI Audit

AI를 진단 파트너로 쓰되 다음 규칙을 강제한다.

1. 사실/추론/가설을 구분해서 쓰게 한다.
2. 아직 확인하지 않은 사실을 ‘확인됨’이라고 쓰지 못하게 한다.
3. 수정 전에 최소 재현을 요구한다.
4. 수정 diff를 작게 제한한다.
5. 실제 실행한 테스트만 PASS로 쓰게 한다.
6. 실패 로그와 남은 위험도 결과물에 포함한다.

AI의 자신감이 아니라 **검증 가능한 증거**가 승인 기준이다.

### 9A. 진단 미니드릴 — 증상을 보면 첫 질문부터 고른다

**드릴 1. 한 번 성공 후 COMPLETE**  
첫 판단: 동일 조건 반복과 회귀검증이 없으면 미검증이다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 2. AI가 20파일 수정**  
첫 판단: root cause와 직접 관련된 최소 diff로 줄인다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 3. 재설치 후 정상**  
첫 판단: 원인이 제거된 것이 아니라 local state가 지워졌을 수 있다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 4. 정상폰과 실패폰 비교 안 함**  
첫 판단: 비교표로 device/account/token/permission/network 차이를 먼저 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 5. 서버 200이니 push 성공 단정**  
첫 판단: provider accepted와 device displayed를 구분한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 6. 테스트 환경이 없어 실행 못 함**  
첫 판단: PASS가 아니라 NOT RUN으로 기록하고 필요한 환경을 명시한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

이 미니드릴의 목표는 정답 암기가 아니다. **증상 → 계층 → 확인할 증거**를 30초 안에 연결하는 습관을 만드는 것이다.

### 10. 회상 문제

1. 좋은 증상정의에 포함할 요소는?
2. 문제정의에 원인 추정을 섞으면 왜 위험한가?
3. 정상 비교군이 주는 이점은?
4. 가설을 여러 개 만드는 이유는?
5. 한 번에 한 변수만 바꾸는 이유는?
6. 재설치를 초반에 피해야 하는 경우는?
7. 최소수정이 디버깅에 유리한 이유는?
8. 회귀검증은 수정된 케이스 외에 무엇을 보는가?
9. rollback 경로가 필요한 이유는?
10. 최종 장애보고서에 필요한 증거는?

### 11. 전이 문제

1. 국내주식 앱의 PER만 대량 결측이다. 데이터소스/API/parser/UI를 계층분리해 진단계획을 작성하라.
2. APK가 어떤 기기에서는 설치되고 다른 기기에서는 실패한다. version/ABI/signing/storage/permission 후보를 비교하라.
3. 자동화가 09:00에 가끔 실행되지 않는다. scheduler→server→provider→device를 시간축으로 추적할 로그 설계를 작성하라.

### 12. Chapter 완료 증거

- Guided Lab의 실행/관찰 결과를 남긴다.
- 정상 케이스뿐 아니라 실패·경계 케이스를 최소 1개 보존한다.
- “무엇을 바꿨는가 / 왜 바꿨는가 / 무엇으로 맞음을 확인했는가”를 5문장 이내로 적는다.
- AI를 사용했다면 AI 답의 오류·누락·과잉변경 여부를 체크한 기록을 남긴다.
- 결과가 예상과 다르면 PASS라고 쓰지 않고, 재현 조건과 다음 실험을 기록한다.

### 13. 참고 렌즈

- Release It!, 2e
- Systems Performance, 2e
- Software Engineering at Google
- Site Reliability Engineering
- Android Developers — testing, performance, processes

---

# V1 최종 체크

이 책을 끝냈다면 다음 질문에 도구 없이 자기 말로 답해 본다.

- 앱이 안 될 때 왜 재설치부터 하면 안 되는가?
- 파일 이름·경로·MIME·내용·해시는 각각 무엇을 말하는가?
- process와 thread, UI freeze와 crash는 어떻게 다른가?
- RAM 부족과 storage full은 어떻게 구분하는가?
- 터미널 명령에서 실행 전 반드시 확인해야 할 것은 무엇인가?
- update가 신규 install보다 더 어려운 이유는 무엇인가?
- 로그에서 root cause와 후속 오류를 어떻게 구분하는가?
- localhost·DNS·port·server는 어떻게 연결되는가?
- Android permission·storage·sync·backup은 어떻게 다른가?
- 성능 문제에서 왜 측정이 수정보다 먼저인가?
- AI가 “고쳤다”고 했을 때 무엇을 실제 증거로 요구해야 하는가?

10개 이상을 실제 사례와 함께 설명할 수 있고, 마지막 종합 장애 프로젝트를 증거 기반으로 수행했다면 V1을 통과한 것으로 본다.

# 주요 참고 기반

## 실전·스테디셀러
- Code, 2nd Edition — Charles Petzold
- Computer Systems: A Programmer’s Perspective, 3rd Edition — Randal Bryant, David O’Hallaron
- How Linux Works, 3rd Edition — Brian Ward
- The Linux Command Line, 2nd Edition — William Shotts
- Systems Performance, 2nd Edition — Brendan Gregg
- Release It!, 2nd Edition — Michael Nygard
- Software Engineering at Google
- Site Reliability Engineering

## 최신/대학 커리큘럼
- Computer Networking: A Top-Down Approach, 9th Edition (2025)
- Harvard CS50x 2026
- CMU 15-213 / CSAPP 계열
- MIT The Missing Semester

## 공식문서
- Android Developers: Processes and threads
- Android Developers: Permissions on Android
- Android Developers: App data and file storage
- Android Developers: Testing / performance / ANR guidance
- Unicode Standard / UTF-8 technical references
- MDN: How the Web works

> 참고자료는 문장을 복제하기 위한 출처가 아니라, 범위·정확성·실전패턴·학습순서를 교차검증하기 위한 렌즈로 사용했다.
