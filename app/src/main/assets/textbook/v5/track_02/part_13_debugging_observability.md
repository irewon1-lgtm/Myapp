# PART 13 · Debugging과 observability — 추측을 실행 증거로 바꾸기

디버깅은 오류 메시지를 보고 눈에 띄는 코드를 수정하는 작업이 아니다. 관찰된 현상에서 가능한 원인을 세우고, 실행 상태를 측정해 가설을 하나씩 제거하는 조사 과정이다. Stack trace, log, debugger, profiler, metric은 서로 다른 질문에 답한다. 좋은 디버깅은 도구를 많이 쓰는 것이 아니라 **어떤 증거가 어떤 가설을 반증하는지**를 명확히 한다.

---

## CHAPTER 01 · 재현 조건을 고정해야 원인과 우연을 구분할 수 있다

버그를 처음 만났을 때 가장 먼저 필요한 것은 수정이 아니라 재현 가능한 조건이다. 어떤 입력, 환경, 데이터 상태, 호출 순서에서 실패했는지 고정하지 못하면 코드를 바꾼 뒤 문제가 사라져도 실제 원인이 해결됐는지 알 수 없다. Random timing이나 외부 서비스 상태가 우연히 달라졌을 수도 있다.

재현 절차는 가능한 한 작게 만든다. 전체 application에서만 보이던 문제를 특정 API request, 함수 호출, 데이터 row 조합으로 줄이면 관찰해야 할 변수도 줄어든다. 최소 재현은 버그를 단순화하면서 동시에 어떤 조건이 꼭 필요한지 알려 준다. 조건 하나를 제거했을 때 실패가 사라지면 그 조건은 원인 공간을 좁히는 증거다.

환경 정보도 재현의 일부다. Python version, dependency version, OS, timezone, configuration, seed, database schema version을 기록한다. 같은 source commit이라도 이 입력들이 다르면 behavior가 달라질 수 있다. 특히 “내 PC에서는 안 난다”는 말은 환경 차이가 있다는 정보일 뿐 결함이 없다는 증거가 아니다.

재현할 수 없는 production incident는 로그와 trace, snapshot을 통해 당시 상태를 복원해야 한다. 그래서 observability는 문제가 생긴 뒤 붙이는 부가기능이 아니라 재현 불가능한 사건을 조사하기 위한 사전 증거 설계다.

---

## CHAPTER 02 · traceback은 마지막 줄보다 호출 경로 전체를 읽는다

Exception traceback은 실패한 최종 줄뿐 아니라 어떤 함수 호출을 거쳐 그 지점에 도달했는지 frame sequence를 보여 준다. 마지막 `KeyError`만 보고 dict 접근을 수정하면 실제로는 그 이전 parser가 필드를 누락시킨 원인을 놓칠 수 있다. Stack을 아래에서 위로 또는 경계 기준으로 읽으며 “어디서 잘못된 상태가 처음 만들어졌는가”를 찾는다.

각 frame에는 local variable과 입력 context가 있다. Debugger나 crash report가 이를 보여 줄 수 있지만 secret과 개인정보가 포함될 수 있으므로 수집 정책이 필요하다. 필요한 식별자와 값 범위는 남기되 credential과 민감 데이터는 제거한다.

Exception chaining이 보존되어 있다면 domain error 뒤의 원래 storage/network cause를 추적할 수 있다. `raise NewError(...) from exc`와 같은 연결은 상위 계층에 의미 있는 오류 타입을 제공하면서 기술 원인도 유지한다. Chain을 끊으면 incident 분석에서 중요한 증거를 잃을 수 있다.

비동기 코드에서는 task boundary 때문에 stack이 동기 호출처럼 단순히 이어지지 않을 수 있다. 어느 task가 생성되었고 어떤 await point에서 exception이 전달됐는지 봐야 한다. Stack trace는 실행 모델과 함께 읽을 때 의미가 있다.

---

## CHAPTER 03 · 로그는 사건의 시간축을 만들되 상태 덤프가 되어서는 안 된다

Log는 “여기까지 실행됨”을 무작정 출력하는 도구가 아니다. 중요한 상태 전이, 외부 요청, retry, 권한 결정, 실패를 시간 순서로 연결해 incident timeline을 복원할 수 있게 해야 한다. 한 request나 job에 correlation identifier를 붙이면 여러 module의 기록을 같은 사건으로 묶을 수 있다.

Structured logging은 message 문자열 안에 모든 정보를 섞기보다 event name, user/resource ID, duration, result, error category 같은 field로 남겨 검색과 집계를 쉽게 한다. 하지만 가능한 field를 전부 기록하면 비용과 privacy 문제가 커진다. 조사에 필요한 최소 context와 보존 기간을 설계한다.

Log level도 의미가 있어야 한다. Expected validation failure를 모두 error로 기록하면 운영자는 실제 장애를 찾기 어렵고, 치명적 invariant violation을 debug로 남기면 경고를 놓친다. Severity는 기술 exception 종류보다 서비스 영향과 대응 필요성을 기준으로 정한다.

같은 exception을 여러 계층에서 중복 기록하면 하나의 사건이 여러 error로 집계될 수 있다. 보통 request boundary나 job boundary에서 충분한 context와 함께 한 번 기록하고, 내부 계층은 error를 보강해 전파하는 방식이 더 선명하다.

---

## CHAPTER 04 · debugger는 실행을 멈추는 도구가 아니라 가설을 검사하는 현미경이다

Breakpoint를 아무 곳에나 많이 설치하면 실행 흐름을 따라가는 데 시간만 늘어난다. 먼저 “이 함수에 들어올 때 `state`가 이미 잘못됐을 것이다” 같은 가설을 세우고 그 지점 직전과 직후를 관찰한다. 예상과 실제가 다르면 원인 범위를 앞뒤로 줄여 나간다.

Step over, step into, step out은 각각 현재 함수의 세부 실행을 얼마나 볼지 선택하는 도구다. Library 내부까지 무조건 들어가기보다 contract가 깨지는 경계를 찾는다. Watch expression과 conditional breakpoint를 사용하면 특정 ID나 index에서만 멈춰 대규모 loop의 한 실패 사례를 조사할 수 있다.

Debugger가 behavior 자체를 바꾸는 경우도 있다. Timing-sensitive race는 실행을 멈추면 재현되지 않을 수 있고, property를 평가하기 위해 inspector가 attribute access를 하면서 side effect를 일으킬 수도 있다. 이때는 trace나 deterministic test, concurrency instrumentation 같은 다른 증거가 필요하다.

Debugger 관찰값도 한 시점의 snapshot이다. “여기서는 맞았다”가 이전과 이후 모든 시점에 맞았다는 뜻은 아니다. 상태 변화 경로를 시간축으로 보려면 breakpoint와 log/trace를 조합한다.

---

## CHAPTER 05 · binary search debugging은 원인 위치를 구간으로 줄인다

큰 pipeline에서 결과가 잘못됐을 때 처음부터 모든 줄을 읽는 대신 중간 지점의 invariant를 확인해 원인 범위를 절반씩 줄일 수 있다. 입력은 정상인데 최종 출력이 틀렸다면 parser 출력, transformation 중간값, persistence 직전 값을 차례로 비교한다. 어느 경계부터 값이 달라지는지 찾는 방식이다.

Commit history에서도 같은 사고를 사용할 수 있다. Known-good revision과 known-bad revision 사이에서 중간 commit을 테스트해 regression을 처음 만든 변경을 찾는다. Git bisect 같은 도구는 이를 자동화하지만 중요한 전제는 각 revision을 판별할 deterministic test가 있다는 것이다.

Feature flag나 configuration을 하나씩 제거해 원인을 좁히는 것도 같은 원리다. 여러 변수를 동시에 바꾸면 어느 변화가 결과에 영향을 줬는지 알 수 없다. 한 번에 하나의 가설 변수를 변경하고 결과를 기록한다.

Binary search 방식은 원인이 한 축을 따라 구간화될 수 있을 때 강하다. 두 조건이 함께 있어야 발생하는 interaction bug에서는 단순 절반 나누기만으로 부족하므로 최소 재현과 조합 테스트를 함께 사용한다.

---

## CHAPTER 06 · profiler는 느린 줄이 아니라 시간과 자원이 어디에 소비되는지 측정한다

성능 문제가 보이면 먼저 측정한다. 체감상 느려 보이는 함수가 실제 bottleneck이 아닐 수 있다. CPU profiler는 실행 시간이 어떤 call stack과 function에 분포하는지 보여 주고, sampling profiler는 낮은 overhead로 반복적으로 stack을 관찰해 hot path를 추정할 수 있다.

Wall-clock time과 CPU time을 구분한다. 함수가 1초 걸렸지만 CPU는 10ms만 사용했다면 나머지는 network, lock, disk, sleep을 기다렸을 수 있다. 이 경우 계산 최적화보다 I/O concurrency나 외부 latency가 원인일 가능성이 높다.

Line profiler처럼 세밀한 도구는 좁혀진 hot function에서 유용하지만 전체 application에 항상 켜면 overhead가 크다. 먼저 coarse metric과 profile로 범위를 좁힌 뒤 필요한 해상도로 내려간다. 관측 도구 자체가 system behavior를 얼마나 바꾸는지도 고려한다.

최적화 후에는 동일 workload로 다시 측정한다. 한 함수가 빨라져도 memory allocation이나 downstream load가 증가해 전체 latency가 나빠질 수 있다. Performance change는 local speedup이 아니라 end-to-end 결과로 검증한다.

---

## CHAPTER 07 · memory debugging은 “얼마나 쓰는가”와 “왜 계속 살아 있는가”를 분리한다

Memory 사용량이 증가한다고 모두 leak은 아니다. Cache가 의도적으로 커지거나 batch가 큰 순간 peak가 생길 수 있다. Leak은 더 이상 필요하지 않은 object가 reference 관계 때문에 회수되지 않고 계속 남는 문제다. 따라서 allocation rate, live object set, reference graph를 구분해 본다.

Python에서는 garbage collection이 있어도 global collection, cache, callback registry, closure, bound method가 object reference를 붙잡고 있으면 해제되지 않는다. File/socket 같은 외부 resource는 memory object가 언젠가 회수되는 것과 별개로 명시적 close가 필요할 수 있다.

Snapshot을 두 시점에서 비교해 어떤 type의 instance가 계속 증가하는지 찾고, 그 object를 누가 reference하는지 역으로 추적한다. 단순히 `del x`를 추가하는 것은 다른 reference가 남아 있다면 해결되지 않는다. Ownership model을 고쳐야 한다.

Peak memory 문제는 streaming, chunking, data representation 변경으로 해결할 수 있다. Leak과 peak는 증상이 비슷해도 처방이 다르므로 시간에 따른 memory curve와 workload를 함께 기록한다.

---

## CHAPTER 08 · metric은 한 요청의 원인보다 전체 분포와 추세를 보여 준다

Log와 trace가 개별 사건의 세부 경로를 보여 준다면 metric은 요청 수, 오류율, latency 분포, queue depth, resource usage 같은 전체 상태를 압축한다. “어제보다 느려졌다”거나 “특정 배포 후 error rate가 상승했다”는 질문에는 metric이 강하다.

평균 latency 하나는 tail 문제를 숨길 수 있다. 대부분 20ms지만 일부가 5초라면 평균만 보고 사용자 경험을 판단하기 어렵다. Percentile과 histogram을 사용해 p50, p95, p99 같은 분포 위치를 본다. 표본 수가 적거나 aggregation window가 다르면 해석도 달라진다.

Metric label을 지나치게 세분화하면 사용자 ID처럼 cardinality가 큰 값 때문에 저장 비용과 query 비용이 폭증할 수 있다. 개별 식별자는 log/trace에 두고 metric은 집계 가능한 차원으로 제한하는 식으로 도구 역할을 나눈다.

Alert는 metric이 임계값을 넘었다는 사실보다 사람이 행동해야 하는 상태를 알려야 한다. 일시적인 spike마다 호출되는 alert는 곧 무시된다. Error budget이나 지속 시간 조건과 결합해 실제 서비스 영향에 맞춘다.

---

## CHAPTER 09 · distributed trace는 서비스 경계를 건너 latency와 실패를 연결한다

한 요청이 여러 service와 database를 거치면 한 process의 stack trace만으로 전체 경로를 볼 수 없다. Trace는 request context를 서비스 사이에 전달하고 각 작업을 span으로 기록해 end-to-end critical path를 구성한다. 어느 서비스가 지연을 만들었고 retry가 몇 번 있었는지 시간축으로 볼 수 있다.

Trace context propagation이 빠지면 일부 span이 별도 trace로 분리되어 사건 연결이 끊긴다. HTTP header, message metadata처럼 경계를 건널 때 correlation 정보를 전달해야 한다. Async queue에서는 producer가 보낸 시점과 consumer가 처리한 시점 사이의 queue delay도 중요한 span이 된다.

Sampling은 비용과 진단력 사이의 trade-off다. 모든 request를 상세 trace하면 고부하 서비스에서 저장량이 커진다. 정상 요청은 일부만 sampling하고 오류나 느린 요청은 더 높은 비율로 보존하는 정책을 사용할 수 있다. Sampling 때문에 특정 rare incident 증거가 없을 가능성도 인식한다.

Trace가 있어도 application의 domain state가 없으면 원인을 완전히 설명하지 못할 수 있다. Correlation ID와 안전한 business context를 log에 연결해 서로 보완한다.

---

## CHAPTER 10 · 디버깅 종료 조건은 “증상이 사라짐”이 아니라 원인 가설의 검증이다

코드를 한 줄 바꾼 뒤 오류가 더 이상 보이지 않는 것은 시작일 뿐이다. 어떤 원인이 있었고, 수정이 왜 그 원인을 제거하며, 같은 failure class가 다시 들어오면 어떤 test가 잡을지를 설명할 수 있어야 한다. 그렇지 않으면 우연히 timing을 바꾸거나 증상을 다른 경로로 밀어냈을 수 있다.

수정 전 최소 재현이 실패하고 수정 후 같은 재현이 통과하는 regression test를 남긴다. Race나 production-only 문제라면 직접 재현 test가 어려울 수 있으므로 invariant, instrumentation, stress condition을 보강한다. 중요한 것은 발견한 지식을 코드와 검증층에 남기는 것이다.

Incident review에서는 개인의 실수보다 왜 기존 guardrail이 잡지 못했는지 본다. Type check, validation, test, deployment gate, monitoring 중 어느 층이 빈 공간이었는지 찾으면 동일한 종류의 결함을 넓게 줄일 수 있다.

Debugging의 최종 산출물은 수정 commit만이 아니다. **재현 조건, 원인, 증거, 수정 원리, 회귀 방지 장치**가 연결된 설명이다. 이 구조를 반복하면 로그를 많이 찍는 사람보다 훨씬 빠르게 복잡한 시스템의 원인 범위를 좁힐 수 있다.