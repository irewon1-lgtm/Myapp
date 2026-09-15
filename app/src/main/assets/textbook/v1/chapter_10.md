## V1-C10. 성능 병목과 장애를 계층별로 진단하기

**원본 Lesson 매핑:** V1-26, V1-27

### 1. 현실 문제에서 시작하기

“앱이 느리다”는 진단이 아니다. 어떤 화면이, 어떤 동작에서, 얼마나 느리고, CPU·메모리·디스크·네트워크·서버 중 어디에서 시간을 쓰는지 측정해야 한다. 사용자는 모두 ‘버벅임’으로 느끼지만 원인은 전혀 다르다.

이 장의 핵심 문장은 하나다. **측정 전 추측 금지.** 성능 문제와 기능 오류를 모두 계층별로 분리하고, 증상→가설→측정→최소수정→재측정의 반복으로 해결한다.

### 2. Mental Model

```text
사용자 체감 지연
  ↓
UI/main thread?
  ↓
CPU 계산?
  ↓
RAM/GC/메모리 압박?
  ↓
disk/DB I/O?
  ↓
DNS/network?
  ↓
server/API?
```

한 요청의 전체 2초 중 앱 계산 50ms, 네트워크 100ms, 서버 처리 1.8초라면 UI 코드를 최적화해도 체감 개선이 거의 없다. 시간을 구간별로 나누는 것이 출발점이다.

### 3. 핵심 개념 해부

#### latency를 구간으로 나누기

총 지연시간만 보면 병목을 알 수 없다. 화면 클릭 시각, 요청 시작/종료, 서버 응답 시작, parsing 완료, UI 반영 시각을 기록하면 어느 구간이 긴지 보인다. 이를 trace나 structured log로 남길 수 있다.

평균값만 보면 드문 심각한 지연이 숨을 수 있으므로 분포와 상위 지연도 본다. 초보 단계에서는 p95 같은 개념을 “100번 중 느린 쪽 5개 수준” 정도로 이해해도 충분하다.

#### CPU·RAM·disk·network·server

CPU가 포화되면 계산시간이 늘고, RAM 압박은 GC나 process kill로 이어질 수 있다. disk/DB I/O가 느리면 저장·조회에서 대기한다. network는 대역폭뿐 아니라 latency, packet loss, DNS가 영향을 준다. server가 느리면 클라이언트는 기다릴 수밖에 없다.

각 자원에는 다른 측정법이 필요하다. 하나의 숫자로 전체 성능을 설명하려 하지 않는다.

#### freeze·ANR·crash·timeout 구분

freeze는 화면이 반응하지 않는 체감이고, Android의 ANR은 main thread가 일정 조건에서 응답하지 않을 때 시스템이 탐지하는 상태다. crash는 프로세스가 예외 등으로 종료되는 것이고, timeout은 어떤 작업이 정해진 시간 안에 완료되지 않은 것이다.

이 네 개를 섞으면 잘못된 처방이 나온다. crash에 timeout 재시도를 넣거나, network timeout에 UI 렌더링 최적화를 하는 식의 오류를 피한다.

#### auth·offline·storage full

네트워크 기능 실패라도 `offline`, `DNS`, `TLS`, `401 auth`, `403 permission`, `429 rate limit`, `5xx server`는 서로 다르다. 저장기능 실패도 권한, 경로, storage full, DB constraint가 다르다.

상태코드와 OS 오류, 사용자 환경을 함께 기록해 증상 카드를 계층에 배치한다.

#### thermal/battery와 모바일 특성

모바일은 지속 부하에서 발열과 배터리 정책의 영향을 크게 받는다. 처음에는 빠른 작업이 시간이 지나며 느려질 수 있다. 백그라운드 제한과 절전모드가 스케줄 작업에 영향을 줄 수도 있다.

그래서 데스크톱 에뮬레이터에서 한 번 빠르게 동작했다고 실제 Galaxy 장시간 사용 성능이 보장되지는 않는다.

### 3A. 개념을 연결해서 생각하기 — 성능은 “느낌”을 수치와 경로로 바꾸는 일

성능 최적화의 가장 큰 위험은 결과가 즉시 보이는 작은 수정에 끌리는 것이다. 이미지가 느리니 cache를 넣고, 목록이 느리니 pagination을 넣고, API가 느리니 retry를 추가한다. 그러나 병목이 아닌 곳을 고치면 복잡성만 늘어난다. 따라서 baseline을 먼저 만든다.

baseline에는 대표 시나리오, 데이터 규모, 기기, OS, 네트워크, cold/warm start 여부가 포함되어야 한다. 같은 작업을 서로 다른 조건으로 측정하면 수정 전후 비교가 무의미하다. 실기기 성능을 볼 때는 발열과 background app 상태도 영향을 준다.

성능을 **queueing** 관점으로 보면 이해가 쉬워진다. 서버가 초당 10건 처리 가능한데 100건이 동시에 들어오면 개별 처리 자체가 100ms여도 대기열 때문에 사용자가 오래 기다린다. thread를 늘리거나 retry를 과도하게 하면 오히려 queue를 악화할 수 있다. rate limit, backpressure, batching이 필요한 이유다.

cache는 latency를 낮추지만 freshness trade-off가 생긴다. stock price처럼 최신성이 중요한 데이터에 긴 cache TTL을 쓰면 빠르지만 틀린 앱이 된다. 반대로 매번 원격 fetch하면 느리고 비용이 늘 수 있다. 데이터 성격에 따라 freshness SLA를 정한다.

mobile 성능에서는 battery와 thermal이 기능요건이다. background polling을 너무 자주 하면 배터리를 소모하고 OS가 제한할 수 있다. “매초 확인해야 빨리 알림” 같은 요구는 실제 필요성과 platform constraint를 비교해야 한다.

장애와 성능의 경계도 흐리다. timeout은 느림이 임계치를 넘어서 기능 실패로 바뀐 것이다. 30초 후 성공하는 API도 사용자 입장에서는 실패일 수 있다. 따라서 performance budget과 timeout 정책을 business requirement와 연결한다.

### 4. Worked Examples

#### Worked Example A — 느린 종목 상세화면

측정 결과:
- 버튼→API 요청 시작 30ms
- DNS+connect 70ms
- server response 대기 1,500ms
- JSON parse 20ms
- DB save 40ms
- UI render 25ms

병목은 서버 대기다. UI 리스트 최적화부터 하는 것은 우선순위가 틀렸다. 서버 query/cache/API 설계 또는 사용자에게 loading/partial data를 보여주는 UX를 검토한다.

#### Worked Example B — ANR

앱이 로컬 DB 전체를 main thread에서 동기 조회해 6초간 화면을 막는다. 네트워크가 없어도 ANR 가능성이 있다. DB 작업을 적절한 background context로 옮기고, 필요한 데이터만 query하고, UI에는 loading/error state를 제공한다. 수정 후 실제 데이터 규모로 다시 측정한다.

### 4A. 미니 사례집

**사례 1 — 800종목 화면이 버벅임**  
모든 데이터를 한 번에 render하는지, DB query가 느린지, 이미지/포맷팅이 비싼지 trace로 분리한다. 무조건 하드웨어 탓을 하지 않는다.

**사례 2 — API retry 후 더 느려짐**  
server가 503인데 수백 client가 즉시 retry해 부하가 커졌다. exponential backoff + jitter와 max retry가 필요하다.

**사례 3 — cache 도입 후 빠르지만 가격이 오래됨**  
cache hit만 성공지표로 보지 않고 data age/freshness를 같이 측정한다.

**사례 4 — 충전 중엔 빠르고 이동 중엔 느림**  
network type, battery saver, thermal, background restriction 조건을 비교한다.

### 5. 그럴듯하지만 틀린 판단

1. 느리면 RAM 청소 앱부터 쓴다.
2. CPU 사용률이 낮으니 서버도 빠를 것이라고 생각한다.
3. 평균 응답시간만 좋으면 사용자 경험도 좋다고 생각한다.
4. 에뮬레이터에서 빠르면 실제 휴대폰에서도 동일하다고 생각한다.

### 5A. 최적화 승인 조건

- 수정 전 baseline이 있는가?
- 병목 구간이 측정으로 확인됐는가?
- 기대 개선량을 적었는가?
- freshness/메모리/배터리 같은 부작용을 평가했는가?
- 같은 조건으로 수정 후 재측정했는가?
- 평균뿐 아니라 느린 케이스도 개선됐는가?
- 기능 회귀 테스트가 통과했는가?

한 항목이라도 NO라면 “최적화 완료”보다 “가설 검증 중”으로 표시한다.

### 5B. 측정치를 잘못 읽으면 최적화도 틀린다

한 번의 실행시간은 noise가 크다. 앱 cold start, disk cache, network variability, background process에 따라 값이 달라질 수 있다. 같은 조건에서 여러 번 측정하고 median과 느린 값도 본다. 작은 차이 1~2%는 측정 noise일 수 있다.

성능 테스트 데이터도 실제 규모와 비슷해야 한다. 개발 DB 20행에서 빠른 query가 운영 50만행에서 느릴 수 있다. 반대로 artificial benchmark에서 느린 작은 함수가 전체 앱 시간의 0.1%만 차지하면 최적화 가치가 낮다. 이를 Amdahl의 법칙까지 수학적으로 깊게 배울 필요는 없지만 **전체 시간에서 차지하는 비중**은 항상 본다.

network는 bandwidth와 latency를 구분한다. 대용량 파일 다운로드는 bandwidth 영향을 크게 받고, 작은 API 요청을 여러 번 순차 호출하는 구조는 latency 영향을 크게 받을 수 있다. “기가 인터넷인데 API가 느림”이 가능한 이유다.

모바일 앱에서는 smoothness도 단순 평균 FPS 하나로 보지 않는다. 특정 순간 긴 frame이 생기면 사용자는 끊김을 느낀다. Android tooling의 frame timing/trace를 활용해 어떤 작업이 main thread를 길게 막았는지 본다.

성능 개선에는 비용이 있다. cache는 memory/storage를 쓰고, preload는 startup/network를 늘리며, concurrency는 코드복잡성과 race risk를 늘린다. 따라서 최적화는 **정확성·단순성·배터리·데이터 최신성**을 함께 고려한 trade-off다.

### 6. Guided Lab

가상의 앱 시나리오 5개를 받아 각 증상을 CPU/RAM/storage/network/server/UI 후보로 분류한다. 각 분류에는 “확인할 지표”를 반드시 붙인다. 지표 없는 원인 단정은 오답이다.

실제 사용할 수 있는 앱 하나에서 화면 로딩 시간을 손으로라도 측정하고, 네트워크가 필요한 단계와 로컬 단계로 나눠 기록한다.

### 7. Independent Lab

자신이 만드는 앱에서 가장 느릴 수 있는 작업 세 가지를 골라 측정계획을 작성한다. 시작/끝 timestamp, input size, success/failure, device/network 조건을 포함한다. 최적화 코드는 작성하지 않는다. 먼저 무엇을 측정할지만 설계한다.

### 8. Debug Challenge

**상황:** 앱이 “가끔” 10초 멈춘다. 로그상 서버 응답은 200ms다. main thread long task, DB lock, 대용량 file I/O, GC/memory pressure 후보를 만들고 각각을 배제할 증거를 정한다. 마지막으로 수정 전후 동일 시나리오를 반복해 회귀검증한다.

### 9. AI Audit

AI에게 “앱을 빠르게 해줘”라고 요청했더니 캐시·스레드·DB index를 한꺼번에 추가했다면 승인하지 않는다. baseline 측정, bottleneck 증거, 변경 하나당 기대효과, 부작용, 재측정값을 요구한다.

### 9A. 진단 미니드릴 — 증상을 보면 첫 질문부터 고른다

**드릴 1. 평균 300ms인데 일부 사용자는 8초**  
첫 판단: tail latency/p95와 조건별 분포를 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 2. 기가 Wi-Fi인데 작은 API 연속호출이 느림**  
첫 판단: bandwidth보다 round-trip latency와 순차 호출구조를 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 3. cache 후 빨라졌지만 값이 오래됨**  
첫 판단: freshness/TTL을 성능지표와 함께 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 4. retry 추가 후 장애가 악화**  
첫 판단: retry storm, backoff, jitter, max retry를 본다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 5. 에뮬레이터는 빠르고 실기기는 느림**  
첫 판단: thermal, device class, storage, network 조건을 실기에서 측정한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

**드릴 6. UI 최적화했지만 전체시간 거의 동일**  
첫 판단: 병목 비중이 작았는지 전체 trace로 확인한다.  
증거 없이 바로 수정하지 말고, 이 판단을 반박할 수 있는 확인 항목도 하나 적는다.

이 미니드릴의 목표는 정답 암기가 아니다. **증상 → 계층 → 확인할 증거**를 30초 안에 연결하는 습관을 만드는 것이다.

### 9B. 성능 수치에 반드시 붙여야 하는 조건

`로딩 1.2초`라는 숫자만 적으면 재현성이 낮다. 최소한 앱 버전, 기기, OS, 네트워크, 데이터 건수, cold/warm 여부, 측정시각을 함께 남긴다. 서버 성능이라면 요청종류와 payload 크기도 적는다.

수정 전후 비교에서는 이 조건을 최대한 동일하게 유지한다. 조건이 달라졌다면 개선값을 그대로 성능향상으로 주장하지 않는다. 성능 보고서의 신뢰도는 숫자의 소수점 자릿수보다 **비교 조건의 통제**에서 나온다.

성능 최적화에서 가장 비싼 실수는 병목이 아닌 곳을 정교하게 고치는 것이다. 측정이 없는 최적화는 기능을 복잡하게 만들면서도 사용자가 느끼는 속도를 거의 바꾸지 못할 수 있다. 그래서 모든 개선안에는 `어느 구간을 몇 ms 줄이려는가`라는 구체적 가설을 붙인다.

재측정 없는 최적화는 완료가 아니다.

### 10. 회상 문제

1. 성능 문제에서 ‘측정 전 추측 금지’가 중요한 이유는?
2. latency를 구간별로 나누면 무엇을 알 수 있는가?
3. CPU-bound와 network/server wait의 차이는?
4. freeze와 crash는 어떻게 다른가?
5. ANR은 어떤 종류의 문제와 연결되는가?
6. timeout은 원인인가 증상인가?
7. 모바일 thermal throttling이 반복실험에 어떤 영향을 줄 수 있는가?
8. 최적화 후 무엇을 반드시 다시 해야 하는가?

### 11. 전이 문제

1. API가 150ms인데 화면은 3초 걸린다. 어디부터 측정할지 계획하라.
2. 처음 5분은 빠르고 이후 느려진다. 메모리/발열/캐시 후보를 구분할 실험을 설계하라.
3. 부모님 폰에서만 앱이 느리다. 기기 성능·OS·네트워크·데이터 규모를 비교하는 표를 설계하라.

### 12. Chapter 완료 증거

- Guided Lab의 실행/관찰 결과를 남긴다.
- 정상 케이스뿐 아니라 실패·경계 케이스를 최소 1개 보존한다.
- “무엇을 바꿨는가 / 왜 바꿨는가 / 무엇으로 맞음을 확인했는가”를 5문장 이내로 적는다.
- AI를 사용했다면 AI 답의 오류·누락·과잉변경 여부를 체크한 기록을 남긴다.
- 결과가 예상과 다르면 PASS라고 쓰지 않고, 재현 조건과 다음 실험을 기록한다.

### 13. 참고 렌즈

- Systems Performance, 2e — Brendan Gregg
- Android Developers — app performance and ANR guidance
- Release It!, 2e
- Site Reliability Engineering
- Computer Systems: A Programmer’s Perspective

---
