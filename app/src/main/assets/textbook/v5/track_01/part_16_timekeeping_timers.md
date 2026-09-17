# PART 16 · 시간 시스템 — clocksource, timer, deadline, synchronization

컴퓨터의 `시간`은 하나의 숫자가 아니다. **경과시간을 재는 monotonic timeline, 사람이 보는 civil time, scheduler가 쓰는 execution clock, future event를 발생시키는 timer, 외부 기준과 동기화하는 clock discipline**이 서로 다른 계약을 가진다. 잘못된 clock 선택은 timeout, cache expiry, certificate validation, distributed ordering을 모두 깨뜨릴 수 있다.

---

## CHAPTER 01 · clock domain은 같은 숫자여도 의미가 다른 시간 축을 구분한다

wall clock, monotonic clock, boot-time clock, process CPU clock처럼 운영체제가 노출하는 시간 축은 서로 다른 질문에 답한다. wall clock은 현실 세계의 시각에 맞아야 하지만 조정될 수 있고, monotonic clock은 경과시간 측정에서 역행하면 안 된다. suspend 중에도 흐르는 clock과 멈추는 clock도 구분해야 한다. 같은 `timestamp` 타입에 이 값을 섞으면 비교가 가능해 보여도 의미는 깨진다.

API 설계에서는 값과 함께 **어느 clock domain에서 왔는지**가 계약에 포함되어야 한다. timeout deadline을 wall clock으로 만들면 NTP step이나 관리자 조정 때문에 갑자기 만료하거나 반대로 오래 남을 수 있다. 반면 사용자에게 영수증 발행시각을 보여 줄 때 monotonic tick은 재부팅 뒤 의미가 없다. duration, civil timestamp, scheduler accounting을 별도 type으로 나누면 이런 혼합을 컴파일 단계에서 줄일 수 있다.

장애 분석에서는 start/end 값만 보지 말고 clock ID, boot ID, timezone, synchronization 상태를 함께 남긴다. 두 host의 wall timestamp가 가까워도 인과 순서를 증명하지 못하고, 한 process의 monotonic value를 다른 machine과 직접 비교할 수도 없다. 시간 버그는 숫자 오차보다 domain 혼동에서 더 자주 생긴다.

---

## CHAPTER 02 · hardware counter는 시간 그 자체가 아니라 증가하는 측정 기반이다

CPU나 SoC의 counter는 일정 주파수로 증가하는 숫자를 제공하고 kernel은 이를 실제 time unit으로 변환한다. counter frequency가 고정인지, power state에 따라 달라지는지, 모든 CPU에서 동기화되어 있는지에 따라 안정성이 달라진다. 하드웨어가 제공하는 raw cycle 수와 운영체제가 보정한 monotonic time을 동일시하면 안 된다.

시간계층은 counter read cost, resolution, stability를 함께 고려해 clocksource를 선택한다. 매우 빠른 counter라도 core마다 offset이 다르면 thread migration 뒤 시간이 뒤로 간 것처럼 보일 수 있고, 안정적이지만 읽기 비용이 큰 source는 hot path에 부담이 된다. kernel이 conversion multiplier와 offset을 유지하는 이유는 raw tick을 application에 그대로 노출하지 않기 위해서다.

성능 측정에서는 counter가 실제 elapsed time과 어떤 관계인지 확인한다. CPU frequency counter를 시간으로 착각하면 DVFS에서 잘못된 duration이 나오며 virtual machine에서는 hypervisor가 synthetic clock을 제공할 수 있다. microbenchmark는 timer overhead와 measurement granularity를 baseline으로 측정해 실제 코드 비용보다 clock read 비용이 커지는 구간을 피해야 한다.

---

## CHAPTER 03 · counter wraparound는 unsigned arithmetic과 비교 규칙을 요구한다

유한 비트 counter는 충분히 오래 실행되면 최대값을 넘어 0으로 돌아간다. 이 사건은 장치 오류가 아니라 표현 범위의 정상 동작이며, timer subsystem은 wraparound를 고려한 비교를 사용해야 한다. 단순히 `now > deadline`으로 비교하면 wrap 경계 근처에서 아직 미래인 deadline을 과거로 판단하거나 반대 상황이 생길 수 있다.

안전한 설계는 허용 가능한 최대 interval을 counter range의 일부로 제한하고 modulo arithmetic을 이용한다. 예를 들어 signed difference가 의미 있게 유지되는 범위 안에서 상대 순서를 판정하면 wrap을 자연스럽게 처리할 수 있다. 장기간 uptime에서만 나타나는 timer bug는 이런 전제가 깨졌을 가능성이 높다.

테스트에서는 정상 범위 값만 쓰지 않고 counter를 최대값 직전으로 이동시킨 fake clock을 사용한다. schedule, cancel, retry, cache expiry를 wrap 전후에 실행해 ordering이 유지되는지 확인한다. 32-bit millisecond tick처럼 wrap 주기가 현실적으로 짧은 환경은 특히 실제 uptime과 무관하게 경계 테스트가 release gate에 포함되어야 한다.

---

## CHAPTER 04 · clockevent는 시간을 읽는 장치가 아니라 미래 interrupt를 예약하는 장치다

clocksource가 `지금 몇 시인가`를 제공한다면 clockevent device는 `얼마 뒤 interrupt를 발생시킬 것인가`를 담당한다. kernel은 다음 timer deadline을 계산하고 hardware comparator나 timer register를 programming해 CPU가 그 시점에 깨어나도록 한다. 이 둘을 분리하면 시간 측정 정확도와 event delivery 정책을 독립적으로 선택할 수 있다.

periodic tick 방식은 일정 간격마다 interrupt를 발생시키지만 idle 구간에도 wakeup 비용을 낸다. one-shot clockevent는 가장 가까운 deadline에만 interrupt를 예약해 불필요한 tick을 줄일 수 있다. 다만 interrupt latency, minimum programmable delta, device resolution 때문에 요청한 시각과 실제 callback 시각 사이 오차가 생길 수 있다.

진단에서는 timer 설정 시각, requested deadline, hardware interrupt 도착, callback 실행 시각을 나눈다. deadline이 늦었다고 hardware timer가 느린 것은 아니다. interrupt가 제때 와도 scheduler가 task를 늦게 실행할 수 있고 CPU idle exit가 추가 latency를 만들 수 있다. event delivery chain 전체를 trace해야 한다.

---

## CHAPTER 05 · timer expiration scheduling은 정확도와 batch 효율을 동시에 다룬다

timer가 만료됐다는 사실과 callback이 즉시 실행된다는 사실은 다르다. kernel은 여러 timer를 자료구조에 넣고 가장 이른 expiration을 찾으며, 만료된 callback을 어떤 context에서 어떤 순서로 실행할지 결정한다. 높은 timer rate에서 작은 bookkeeping 비용도 CPU와 lock contention을 크게 만들 수 있다.

정확한 deadline이 필요 없는 작업은 비슷한 expiration을 묶어 wakeup을 줄일 수 있다. background telemetry나 maintenance timer를 모두 서로 다른 millisecond에 예약하면 CPU가 깊은 idle에 들어가지 못한다. 반면 network retransmission, audio deadline처럼 지연 허용치가 좁은 작업을 과도하게 coalesce하면 latency와 protocol behavior가 나빠진다.

운영 metric은 등록 timer 수, expiration batch 크기, callback 지연, wakeup 횟수를 같이 본다. timer 자체가 병목인지 callback이 길어서 backlog가 생기는지 분리해야 한다. callback 내부에서 blocking work를 수행하면 다음 timer까지 밀릴 수 있으므로 실제 heavy work는 별도 execution context로 넘기는 설계가 안전하다.

---

## CHAPTER 06 · tickless mode는 idle과 low-activity 구간의 불필요한 periodic interrupt를 줄인다

고정 periodic tick은 scheduler accounting과 timer 처리에 단순하지만 CPU가 할 일이 없는 동안에도 계속 깨어나게 한다. tickless 설계는 다음 의미 있는 event까지 periodic interrupt를 생략하거나 주기를 늘려 power와 virtualization overhead를 줄인다. mobile과 large server 모두 idle residency 개선 효과가 크다.

하지만 tick 제거는 timekeeping과 scheduler accounting을 없애는 것이 아니다. elapsed time은 stable clocksource로 계산하고, 다음 event는 one-shot timer에 다시 programming한다. long idle 뒤 깨어날 때 누적 accounting과 만료 timer를 올바르게 처리해야 하며, CPU마다 독립적으로 tick 상태가 달라질 수 있다.

성능 분석에서 tickless가 켜졌다는 설정값만 믿지 않는다. 실제 wakeup source, interrupt rate, deepest idle-state residency를 관찰한다. stray timer, polling thread, 높은-frequency daemon이 있으면 tickless kernel에서도 CPU가 계속 깨어난다. power 문제는 scheduler와 application timer 사용까지 함께 추적해야 한다.

---

## CHAPTER 07 · high-resolution timer는 정밀한 요청을 가능하게 하지만 실행 deadline을 보장하지 않는다

high-resolution timer는 coarse tick보다 세밀한 expiration을 표현하고 hardware one-shot timer를 이용해 가까운 deadline을 예약할 수 있다. 이는 microsecond 수준의 timeout이나 media scheduling에 유리하지만 요청한 시각에 application code가 즉시 실행된다는 real-time guarantee는 아니다.

expiration interrupt 뒤에는 interrupt handling, softirq 또는 callback dispatch, runnable queue, scheduler 선택이 이어진다. CPU가 non-preemptible 구간에 있거나 higher-priority task가 실행 중이면 callback은 늦어질 수 있다. 따라서 timer resolution과 scheduling latency를 같은 의미로 말하면 안 된다.

검증은 requested deadline과 actual callback timestamp의 distribution을 측정한다. 평균 오차가 작아도 p99가 deadline을 넘으면 latency-sensitive workload에는 실패다. CPU isolation, priority policy, power-state exit, interrupt affinity를 바꿨을 때 tail이 어떻게 달라지는지 비교해야 원인을 특정할 수 있다.

---

## CHAPTER 08 · scheduler clock은 공정성과 runtime accounting을 위한 시간 축이다

scheduler는 task가 얼마나 실행됐는지, 언제 runnable이 되었는지, deadline이나 virtual runtime이 어떻게 변하는지를 계산하기 위해 빠르고 일관된 clock이 필요하다. 이 clock은 사람이 보는 시각을 맞출 필요가 없고 scheduling decision에 충분한 monotonic성과 해상도가 중요하다.

CPU migration이 잦은 system에서는 서로 다른 core에서 읽은 scheduler clock이 일관되어야 한다. 하드웨어 counter가 core별 offset을 가지면 kernel이 보정하거나 더 안정적인 source를 선택해야 한다. accounting 오차는 단순 통계 문제를 넘어 fairness와 deadline 계산을 왜곡할 수 있다.

scheduler latency 분석에서는 task의 enqueue, runnable, actual on-CPU 구간을 같은 scheduling clock으로 연결한다. wall-clock 로그와 섞으면 NTP correction이나 cross-host skew 때문에 queueing time이 잘못 계산될 수 있다. runqueue wait를 증명하려면 scheduler trace의 native timestamp를 우선한다.

---

## CHAPTER 09 · CPU time과 wall time은 계산과 기다림을 분리하는 기본 증거다

wall duration은 operation 시작부터 종료까지 사용자가 기다린 전체 시간이고 CPU time은 process나 thread가 실제 CPU에서 실행한 시간을 근사한다. wall 5초에 CPU 100ms라면 병목은 계산량보다 I/O, lock, scheduler wait, sleep 같은 off-CPU 구간에 있을 가능성이 높다.

반대로 wall과 CPU가 거의 같고 한 core가 지속적으로 바쁘다면 CPU-bound path를 의심할 수 있다. multi-thread workload에서는 process CPU time이 wall보다 훨씬 클 수 있으므로 단순 비율을 100% utilization처럼 해석하면 안 된다. thread별 CPU와 system-wide core capacity를 함께 본다.

프로파일링에서는 wall-clock trace와 CPU sample을 연결한다. CPU profiler가 hotspot을 보여도 전체 latency에서 차지하는 비율이 작으면 최적화 효과가 제한된다. performance regression을 고칠 때 먼저 wall/CPU 차이를 계산하면 조사 계층을 빠르게 좁힐 수 있다.

---

## CHAPTER 10 · process와 thread CPU clock은 execution accounting을 wall time과 분리한다

운영체제는 process 전체 또는 특정 thread가 실제로 소비한 CPU 시간을 별도 clock으로 제공할 수 있다. sleep 중에는 증가하지 않으므로 algorithmic cost와 scheduler delay를 구분하는 데 유용하다. 동일 request의 elapsed 2초 중 CPU가 1.8초라면 I/O wait보다 계산 또는 spin 가능성이 높다.

thread CPU clock은 multi-thread application에서 특히 중요하다. 한 worker가 spin하면서 다른 worker를 기다리면 process CPU는 크게 증가하지만 productive throughput은 늘지 않을 수 있다. request latency만 보면 external dependency 문제처럼 보일 수 있어 per-thread accounting이 필요하다.

테스트에서는 CPU-time threshold와 elapsed-time threshold를 별도로 둔다. 기능이 동일해도 busy-wait로 변경되면 latency는 비슷하면서 battery와 server capacity가 악화될 수 있다. CPU budget regression을 독립 gate로 관리하면 이런 변화가 사용자 지연으로 나타나기 전에 탐지할 수 있다.

---

## CHAPTER 11 · wall-clock step과 slew는 시각 보정 방식이 서로 다르다

외부 기준과 local wall clock 사이 차이가 발견되면 시스템은 시간을 즉시 뛰어넘는 step이나 clock rate를 조금 조정하는 slew를 사용할 수 있다. 큰 초기 오차는 step이 필요할 수 있지만 running application 입장에서는 시각이 갑자기 앞으로 또는 뒤로 이동한 것처럼 보인다.

slew는 continuity를 유지하는 대신 correction이 끝날 때까지 clock rate가 미세하게 달라진다. calendar event에는 자연스럽지만 elapsed-time 계산에 wall clock을 쓰면 두 방식 모두 오류를 만든다. timeout은 monotonic deadline으로 두고 wall-clock correction과 분리해야 한다.

운영 로그에서 timestamp discontinuity가 보이면 application bug로 단정하지 않는다. NTP daemon 상태, offset, step event, boot 직후 synchronization 여부를 확인한다. audit trail처럼 wall time이 중요한 데이터는 monotonic sequence나 event ID를 함께 저장해 동일 시각 반복과 역행에서도 순서를 복원할 수 있게 한다.

---

## CHAPTER 12 · clock drift는 완전히 정상인 oscillator 오차가 시간이 지나며 누적되는 현상이다

물리 oscillator는 온도, 전압, 제조 편차 때문에 nominal frequency와 정확히 같지 않다. 작은 ppm 차이도 긴 시간에는 눈에 띄는 offset으로 누적된다. 그래서 네트워크가 끊긴 장치라도 local clock이 완벽히 유지될 것이라고 가정하면 안 된다.

시간 동기화 시스템은 순간 offset뿐 아니라 frequency error를 추정해 local clock의 rate를 조정한다. 불안정한 hardware clock이나 급격한 thermal 변화가 있으면 correction이 자주 필요해질 수 있다. mobile device와 VM은 suspend, migration 같은 추가 변수를 가진다.

진단에서는 offset time-series와 drift rate를 함께 본다. 특정 host만 꾸준히 한 방향으로 벗어나면 network delay보다 oscillator 또는 virtualization source를 의심할 수 있다. certificate, token expiry, distributed lease가 wall clock에 의존한다면 허용 skew와 monitoring threshold를 명시해야 한다.

---

## CHAPTER 13 · NTP offset은 한 번의 timestamp 차이가 아니라 왕복 지연을 고려한 추정치다

서버와 클라이언트 clock 차이를 알아내려면 메시지가 네트워크를 오가는 동안 소비한 시간을 고려해야 한다. NTP 계열 프로토콜은 여러 timestamp를 이용해 round-trip delay와 offset을 추정한다. 경로가 비대칭이면 추정 오차가 커질 수 있으므로 단일 sample을 절대적인 truth로 보지 않는다.

여러 server와 여러 sample을 사용하면 outlier와 falseticker를 줄일 수 있다. packet loss, queueing spike, VM pause는 순간 offset 관측을 오염시킨다. synchronization 품질은 현재 offset뿐 아니라 dispersion, jitter, selected source 상태까지 봐야 한다.

분산 로그의 시각 정렬에서는 NTP가 켜져 있다는 사실만으로 microsecond-level ordering을 주장하지 않는다. host별 observed offset bound를 저장하고 인과성은 trace parent, sequence, message relation으로 확인한다. wall timestamp는 편리한 근사 축이지 distributed total order가 아니다.

---

## CHAPTER 14 · clock discipline은 noisy measurement를 즉시 적용하지 않고 안정적으로 추종한다

외부 시간 source에서 매 sample마다 다른 offset이 들어오므로 local clock은 측정값을 그대로 따라가면 흔들린다. discipline algorithm은 offset과 frequency estimate를 filtering하고 안정적으로 correction한다. 너무 빠른 반응은 network jitter를 clock jitter로 만들고 너무 느린 반응은 실제 drift를 오래 남긴다.

source selection도 discipline의 일부다. 여러 upstream이 서로 다른 값을 줄 때 신뢰 가능한 집합을 찾고, 큰 deviation을 보이는 source를 제외해야 한다. 단일 서버 의존은 장애나 잘못된 시각이 전체 fleet으로 전파될 위험을 키운다.

운영에서는 synchronization state change 자체를 event로 남긴다. source switch, unsynchronized transition, large correction이 발생한 구간의 authentication failure나 cache expiry 이상을 같이 보면 시간 원인을 찾기 쉽다. clock service는 인프라 dependency로 취급하고 SLO와 alarm을 갖는 편이 안전하다.

---

## CHAPTER 15 · civil time은 날짜·달력 규칙과 결합된 사람이 이해하는 시간이다

Unix timestamp 같은 instant와 `2026-09-17 09:00` 같은 civil representation은 다르다. civil time은 calendar system, timezone, daylight-saving 규칙을 적용해 사람이 보는 날짜와 시각으로 변환된다. 저장할 때 timezone 정보가 빠지면 같은 문자열이 여러 instant를 의미할 수 있다.

예약 시스템은 사용자가 입력한 지역 시각과 실제 실행 instant를 분리해서 보관하는 것이 안전하다. 장기간 반복 일정은 timezone rule이 미래에 바뀔 수 있어 생성 시점의 offset만 저장하면 후속 occurrence가 틀릴 수 있다. 지역 ID와 원래 의도를 함께 보존해야 한다.

버그 재현에는 입력 문자열, timezone database version, locale, parsing policy를 기록한다. `2026-11-01 01:30`처럼 어떤 지역에서는 두 번 존재하거나 아예 존재하지 않는 local time이 있을 수 있다. civil time은 단순 정수 arithmetic으로 다루기 어려운 domain이다.

---

## CHAPTER 16 · timezone은 고정 offset이 아니라 지역별 역사와 정책을 포함한다

`UTC+9` 같은 offset은 특정 instant의 차이만 표현하지만 timezone은 과거와 미래의 offset 규칙을 포함한다. 국가가 정책을 바꾸면 같은 지역도 날짜에 따라 offset이 달라질 수 있다. 장기간 보관되는 일정은 numeric offset만으로 사용자의 지역 의도를 복원할 수 없다.

서버와 클라이언트가 서로 다른 timezone database version을 사용하면 미래 일정 계산이 달라질 수 있다. 모바일 앱이 offline에서 occurrence를 생성하고 backend가 재계산할 때 이런 차이가 나타난다. protocol은 instant 전송과 local scheduling 의미를 분리해야 한다.

운영 장애에서는 OS timezone 설정만 보지 말고 application runtime이 실제 어떤 tzdb를 사용하는지 확인한다. container image가 오래되었거나 library가 별도 database를 포함할 수도 있다. timezone update는 데이터 migration 없이도 결과를 바꾸는 dependency update다.

---

## CHAPTER 17 · daylight-saving 전환은 local time에 gap과 overlap을 만든다

DST 시작 시 clock이 앞으로 뛰면 특정 local time range가 존재하지 않을 수 있고, 종료 시 뒤로 돌아가면 같은 local time이 두 번 나타날 수 있다. 단순히 `하루=24시간`으로 계산하면 이 경계에서 일정이 한 시간 밀리거나 중복될 수 있다.

`매일 오전 8시`와 `24시간마다`는 서로 다른 요구다. 전자는 civil schedule이고 후자는 elapsed interval이다. 사용자가 기대하는 의미를 API에서 구분해야 한다. calendar arithmetic은 timezone-aware library를 사용하고 ambiguous/nonexistent time 처리 정책을 명시한다.

테스트는 평범한 날짜뿐 아니라 DST 직전·직후, overlap 구간, leap-year와 함께 수행한다. 운영 데이터에 원본 timezone과 resolved instant를 둘 다 남기면 예상 시각과 실제 실행 시각 차이를 분석할 수 있다. 시간 버그는 대부분 정상일 때가 아니라 경계에서 드러난다.

---

## CHAPTER 18 · deadline은 duration보다 강한 의미를 가진 절대 종료 경계다

`5초 timeout`은 operation을 시작할 때 남은 예산을 의미하지만 nested call마다 새 5초를 부여하면 전체 request는 훨씬 오래 걸릴 수 있다. end-to-end deadline을 monotonic time으로 계산하고 downstream에 남은 budget을 전달해야 chain 전체의 latency bound가 유지된다.

queue에 오래 머문 request는 실행을 시작하기 전에 이미 deadline을 넘었을 수 있다. 이런 work를 그대로 DB나 external API에 보내면 사용자에게는 실패했는데 resource는 계속 소비한다. admission과 dequeue 시점에서 deadline을 검사해 expired work를 제거하는 정책이 필요하다.

trace에는 original deadline, 각 span 시작 시 remaining budget, timeout 원인을 기록한다. `timeout`이라는 한 error code만으로는 어느 계층이 예산을 소진했는지 알 수 없다. deadline propagation은 latency뿐 아니라 overload protection에도 직접 연결된다.

---

## CHAPTER 19 · timer data structure는 expiration 탐색 비용과 insertion 비용을 교환한다

수많은 timer를 관리할 때 매 tick마다 전체 목록을 스캔할 수 없다. heap, balanced tree, timer wheel 같은 자료구조는 expiration 순서, range, resolution에 따라 서로 다른 비용을 가진다. workload의 timer 분포가 선택을 좌우한다.

priority heap은 가장 가까운 deadline을 빠르게 찾지만 insert/remove가 log N이고 arbitrary cancellation에 index 관리가 필요하다. timer wheel은 넓은 범위를 bucket으로 나눠 대량 timer를 효율적으로 처리할 수 있지만 resolution과 cascade 정책을 신중히 정해야 한다. 한 구조가 모든 환경에서 우월하지 않다.

진단에서는 등록 timer 수뿐 아니라 insert/cancel rate, expiration distance distribution, batch size를 본다. request마다 짧은 timer를 생성했다가 대부분 cancel하는 서비스는 expiration보다 cancellation cost가 병목일 수 있다. data structure를 바꾸기 전 실제 operation mix를 측정한다.

---

## CHAPTER 20 · timer cancellation은 callback과 경쟁하는 state transition이다

cancel 호출이 성공했다는 말이 callback이 절대 실행되지 않는다는 뜻인지, 아직 시작되지 않은 callback만 막는다는 뜻인지 API마다 다르다. expiration 직전에 cancel하면 callback dispatch와 cancellation이 race할 수 있다. cleanup code는 이런 동시성을 명시적으로 처리해야 한다.

안전한 설계는 timer state를 scheduled, firing, completed, cancelled처럼 구분하고 callback이 stale generation인지 검사하게 만든다. object를 destroy한 뒤 늦은 callback이 접근하면 use-after-free나 오래된 UI update가 생길 수 있다. callback lifetime은 owner lifetime과 연결되어야 한다.

테스트에서는 cancel 직전·직후, callback 시작과 cancel 동시 실행, reschedule generation 변경을 반복한다. 단순히 sleep 후 cancel하는 deterministic하지 않은 test로는 race를 안정적으로 재현하기 어렵다. fake scheduler와 barrier를 사용해 interleaving을 강제해야 한다.

---

## CHAPTER 21 · timeout budget은 여러 dependency에 배분되는 제한 자원이다

request 전체 deadline이 1초라면 DNS, connection, authentication, DB, rendering이 각각 독립적으로 1초를 사용해서는 안 된다. critical path와 병렬 가능성을 고려해 budget을 나누고 각 dependency가 남은 시간을 초과하지 않게 해야 한다.

budget을 너무 촘촘히 고정하면 정상 variance에도 실패가 증가하고, 너무 느슨하면 한 dependency가 전체 SLO를 소모한다. historical latency distribution과 retry 정책을 같이 보며 threshold를 정한다. retry attempt는 새로운 full timeout이 아니라 남은 budget 안에서만 실행하는 것이 안전하다.

운영에서는 timeout rate와 함께 `remaining budget at call`을 수집한다. dependency가 빠른데 항상 적은 budget만 받는다면 upstream queue가 문제일 수 있다. timeout을 service별 숫자 조정으로만 다루지 말고 request lifetime 전체의 자원 배분 문제로 본다.

---

## CHAPTER 22 · cache TTL은 freshness policy이며 clock semantics에 의존한다

TTL은 entry가 생성된 뒤 얼마 동안 유효한지를 표현하는 duration이다. local in-memory cache는 monotonic elapsed time으로 만료를 계산하는 편이 안전하고, distributed cache의 absolute expiry는 server clock synchronization 상태를 고려해야 한다. wall-clock jump가 대량 simultaneous expiry를 만들 수 있다.

TTL이 짧으면 freshness는 좋아지지만 backend load가 늘고, 길면 stale data window가 커진다. 모든 entry에 같은 TTL을 주면 특정 시각에 cache miss가 집중되는 herd가 생길 수 있어 jittered expiry가 유용하다. 그러나 보안 credential처럼 정확한 만료가 필요한 데이터에는 임의 jitter를 적용하면 안 된다.

진단에는 cache hit/miss뿐 아니라 age distribution, expiry reason, refresh latency를 포함한다. clock correction 뒤 miss rate가 급증했다면 application load 변화가 아니라 expiry semantics가 원인일 수 있다. cache policy는 timekeeping dependency를 명시적으로 가져야 한다.

---

## CHAPTER 23 · distributed timestamp는 서로 다른 host의 사건 순서를 완전히 결정하지 못한다

각 machine이 NTP로 동기화되어도 clock skew와 network delay는 0이 아니다. 두 로그의 wall timestamp가 2ms 차이라는 사실만으로 어느 사건이 원인인지 단정하면 안 된다. 특히 VM pause나 synchronization loss가 있으면 순서가 뒤집혀 보일 수 있다.

분산 tracing은 parent-child span relation, message ID, sequence number 같은 논리적 관계를 사용해 causality를 보존한다. timestamp는 latency를 근사하고 시각화하는 축으로 쓰되 causal proof는 protocol metadata에서 얻는 것이 안전하다.

incident 분석 시 host별 synchronization 상태와 uncertainty bound를 함께 본다. 장애 구간에 한 node만 offset이 커졌다면 apparent negative latency가 생길 수 있다. 단순 로그 정렬 대신 request lineage를 구축하면 clock quality가 나빠도 사건 순서를 복원할 수 있다.

---

## CHAPTER 24 · Lamport clock은 인과 관계를 보존하는 scalar logical clock이다

Lamport clock은 local event마다 counter를 증가시키고 message에 값을 실어 보내며 수신 시 더 큰 값 다음으로 진행한다. `a happens-before b`라면 Lamport(a) < Lamport(b)를 보장할 수 있어 wall clock 없이 causality-compatible ordering을 만든다.

반대는 성립하지 않는다. counter가 작다고 반드시 causal predecessor는 아니며 concurrent event도 임의 순서가 붙을 수 있다. 따라서 Lamport timestamp만으로 concurrency 여부를 판정할 수 없다. total-order tie-breaker와 결합할 수 있지만 그것은 실제 시간 순서가 아니라 deterministic order다.

분산 시스템에서 필요한 질문이 `누가 먼저 일어났는가`인지 `서로 인과적으로 연결됐는가`인지 구분해야 한다. audit ordering이나 replicated command serialization에는 Lamport-style sequence가 유용하지만 사용자에게 표시하는 실제 시각은 별도 wall timestamp가 필요하다.

---

## CHAPTER 25 · vector clock은 동시성과 인과성을 더 정확히 표현하는 대신 metadata가 커진다

vector clock은 participant별 logical counter를 유지해 한 event가 다른 event를 causally precede하는지, 서로 concurrent인지 비교할 수 있다. 두 vector가 component-wise로 한쪽이 작거나 같으면 causal order를 얻고, 서로 일부 component가 앞서면 concurrent 관계를 알 수 있다.

이 정보는 conflict detection에 유용하지만 participant 수가 늘면 metadata가 커진다. dynamic membership과 garbage collection도 복잡해져 대규모 시스템에서는 다른 compact causal metadata를 선택할 수 있다. 정확한 causality와 storage overhead 사이 trade-off가 있다.

운영에서 vector clock을 단순 version number처럼 표시하면 의미를 잃는다. sibling state가 왜 생겼는지, merge가 어떤 causal frontier를 포함하는지 설명할 수 있어야 한다. conflict resolution policy는 logical clock이 아니라 application semantics가 최종 결정을 내린다.

---

## CHAPTER 26 · hybrid logical clock은 physical time과 logical ordering을 결합한다

physical clock은 사람이 해석하기 쉽고 범위 query에 편리하지만 skew가 있고, logical clock은 causality를 표현하지만 현실 시간과 거리가 있다. hybrid logical clock 계열은 physical component를 중심으로 하되 clock이 뒤로 가거나 같은 시각에 causal event가 연속될 때 logical counter를 사용해 단조 증가를 유지한다.

이 구조도 clock synchronization 요구를 완전히 없애지 않는다. physical component skew가 너무 크면 retention, snapshot, conflict window 같은 정책이 잘못될 수 있다. HLC가 제공하는 ordering guarantee와 실제 wall-time accuracy를 별도 문서화해야 한다.

분산 저장소에서 HLC를 사용한다면 serialization format, overflow, restart persistence, node ID tie-break를 함께 설계한다. timestamp 하나를 보고 external consistency를 자동으로 가정하면 안 된다. transaction protocol이 어떤 ordering guarantee를 추가하는지 확인해야 한다.

---

## CHAPTER 27 · real-time workload는 평균 latency보다 bounded worst-case behavior가 중요하다

real-time scheduling은 단순히 빠른 CPU를 쓰는 문제가 아니다. task period, execution budget, deadline, priority inversion, interrupt latency 같은 상한을 관리해야 한다. 평균 1ms라도 가끔 100ms stall이 나면 10ms deadline system에는 실패다.

memory allocation, page fault, lock contention, frequency scaling은 tail unpredictability를 키울 수 있다. critical path에서 dynamic allocation을 제한하거나 page를 prefault하고 priority inheritance를 사용하는 이유가 여기 있다. hard/soft real-time 요구를 구분해 필요한 보장 수준을 정해야 한다.

검증은 p99만으로 끝내지 않고 worst observed latency와 missed-deadline count를 workload envelope 안에서 측정한다. CPU isolation, IRQ affinity, thermal state, overload scenario를 포함해야 production 조건의 bound를 추정할 수 있다. real-time property는 source code만으로 증명되지 않는다.

---

## CHAPTER 28 · mobile timer는 wakeup, battery, background policy와 함께 설계해야 한다

모바일 장치에서 timer 하나는 callback뿐 아니라 CPU wakeup과 radio activity를 유발할 수 있다. 수많은 앱이 정확한 시각에 반복 alarm을 걸면 deep sleep이 깨지고 battery drain이 커진다. platform이 inexact alarm이나 batching을 제공하는 이유는 전체 시스템의 wakeup을 줄이기 위해서다.

사용자-visible exact reminder와 background refresh는 요구가 다르다. 전자는 정확도가 중요하고 후자는 수분 지연되어도 괜찮을 수 있다. exact alarm 권한이나 foreground execution을 단순 우회 수단으로 쓰면 정책 위반과 battery regression을 만든다.

운영에서는 timer 수보다 actual wakeup count, wake lock duration, background execution time을 본다. 앱이 callback에서 network를 시작하면 radio tail energy까지 연결될 수 있다. mobile scheduling은 latency, reliability, energy를 하나의 contract로 다뤄야 한다.

---

## CHAPTER 29 · time-dependent test는 실제 sleep 대신 controllable clock을 사용한다

테스트에서 `sleep(1000)`을 사용하면 실행 환경의 scheduler와 load에 따라 flaky해지고 suite도 느려진다. business logic이 clock interface를 주입받게 만들면 fake clock을 원하는 시각으로 이동시켜 expiry, retry, daily schedule을 deterministic하게 검증할 수 있다.

fake clock은 단순 now 값만 바꾸는 데 그치지 않는다. monotonic advance와 wall-clock step을 독립적으로 표현해야 clock-domain 혼동을 찾을 수 있다. DST overlap, NTP backward step, timer cancellation race 같은 경계도 별도 scenario로 만든다.

실제 integration test에서는 kernel timer와 scheduler behavior를 검증하되 logic test와 분리한다. unit layer는 시간이 빨리 지나도록 시뮬레이션하고 platform layer는 실제 callback jitter를 측정하면 속도와 현실성을 모두 얻을 수 있다. flaky time test를 재시도로 숨기지 않는다.

---

## CHAPTER 30 · time type은 instant, duration, deadline, civil time을 코드 수준에서 분리해야 한다

시간 버그를 줄이는 가장 강한 방법 중 하나는 서로 다른 의미를 같은 integer나 string으로 표현하지 않는 것이다. instant는 특정 timeline의 점, duration은 두 점 사이 길이, deadline은 미래 완료 경계, civil time은 timezone 규칙이 적용된 인간 친화 표현이다. 서로 허용되는 연산이 다르다.

예를 들어 instant + duration은 의미가 있지만 wall timestamp 두 개의 차이를 latency로 쓰는 것은 clock adjustment 때문에 위험할 수 있다. local date-time을 UTC instant로 바꾸려면 timezone과 ambiguity policy가 필요하다. type system이 이런 전제를 드러내면 리뷰와 테스트가 쉬워진다.

serialization에서도 domain을 보존한다. `expiresAt`이 UTC instant인지 monotonic deadline인지 명시하고 unit, epoch, timezone을 schema에 포함한다. 여러 서비스가 같은 필드를 다르게 해석하는 순간 timeout과 만료 문제는 재현하기 어려운 분산 버그가 된다.