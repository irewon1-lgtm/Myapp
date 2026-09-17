# PART 16 · 시간 시스템 — clocksource, timer, deadline, synchronization

컴퓨터의 `시간`은 하나의 숫자가 아니다. **경과시간을 재는 monotonic timeline, 사람이 보는 civil time, scheduler가 쓰는 execution clock, future event를 발생시키는 timer, 외부 기준과 동기화하는 clock discipline**이 서로 다른 계약을 가진다. 잘못된 clock 선택은 timeout, cache expiry, certificate validation, distributed ordering을 모두 깨뜨릴 수 있다.

---

## CHAPTER 01 · wall clock과 elapsed-time clock은 요구사항이 다르다

wall clock은 달력 날짜와 시각을 표현하고 외부 표준시와 동기화되어야 한다. 운영 중 NTP correction이나 관리자 조정으로 앞으로 또는 뒤로 움직일 수 있다. 반면 duration과 deadline 계산에는 시간이 역행하지 않는 monotonic clock이 필요하다.

`endWall - startWall`로 latency를 측정하면 wall-clock correction 때문에 음수 또는 비정상 duration이 나올 수 있다. duration은 monotonic source를 사용하고 사용자에게 표시할 event time은 wall clock을 사용한다. timestamp와 duration을 같은 primitive로 취급하지 않는다.

---

## CHAPTER 02 · hardware counter는 time unit이 아니라 증가하는 cycle domain이다

clocksource의 기반 hardware counter는 일정 frequency로 증가하는 정수일 수 있다. raw counter 값을 nanosecond로 바꾸려면 frequency와 scaling rule이 필요하다. CPU frequency가 동적으로 바뀌는 counter를 안정 clock으로 사용하면 elapsed time이 왜곡될 수 있다.

좋은 clocksource는 monotonicity, sufficient resolution, stability, cross-CPU consistency를 가져야 한다. Linux는 architecture별 candidate 중 rating/availability에 따라 clocksource를 선택할 수 있다. performance issue에서 timestamp 이상이 보이면 active clocksource와 suspend/frequency behavior를 확인한다.

---

## CHAPTER 03 · counter wraparound는 modular arithmetic으로 보정한다

finite-width hardware counter는 최대값 뒤 0으로 wrap한다. 32-bit 100MHz counter처럼 wrap interval이 짧으면 raw value 비교만으로 시간 순서를 결정할 수 없다.

kernel timekeeping은 valid-bit mask와 previous sample 차이를 modular arithmetic으로 처리해 continuous timeline을 만든다. custom firmware/driver가 raw counter를 직접 사용할 때 wrap-safe subtraction을 구현해야 한다. uptime이 특정 기간을 넘을 때만 발생하는 bug는 wrap interval과 연결해 본다.

---

## CHAPTER 04 · clocksource와 clock-event device는 반대 방향의 abstraction이다

clocksource는 `지금 timeline의 어디인가`를 읽는 장치이고 clock-event device는 `미래의 특정 시점에 interrupt를 발생시켜라`를 programming하는 장치다. 같은 hardware timer block을 사용할 수 있어도 API 역할은 다르다.

clocksource 정확도가 좋아도 event timer resolution/interrupt latency가 나쁘면 timer callback이 늦을 수 있다. `clock_gettime이 정확하다`와 `sleep이 정확히 원하는 시점에 깨어난다`는 별개의 성질이다. deadline miss를 time-read error로 오진하지 않는다.

---

## CHAPTER 05 · timer expiration과 task execution 시점은 같지 않다

timer hardware가 deadline에 interrupt를 발생시켜도 target thread가 즉시 CPU를 얻는 것은 아니다. interrupt handling, timer queue processing, scheduler run queue, higher-priority task 때문에 callback execution은 뒤로 밀릴 수 있다.

따라서 timeout 정확성은 timer resolution과 scheduler latency를 함께 본다. real-time workload는 wakeup latency distribution과 worst case를 측정한다. application sleep을 microsecond 단위로 요청한다고 동일 precision으로 execution이 재개된다는 보장은 없다.

---

## CHAPTER 06 · periodic tick과 tickless scheduling은 wakeup 비용 구조를 바꾼다

고정 주기 timer tick은 scheduler accounting과 timer processing을 단순화하지만 idle CPU도 주기적으로 깨워 energy를 소비한다. tickless/nohz 방식은 다음 필요한 event까지 timer를 program해 불필요한 wakeup을 줄인다.

mobile/server power optimization에서 timer coalescing과 tick suppression이 중요하다. application이 짧은 periodic timer를 수백 개 만들면 kernel이 tickless여도 wakeup을 계속 발생시킨다. timer frequency를 energy budget과 함께 본다.

---

## CHAPTER 07 · high-resolution timer는 더 작은 단위를 제공하지만 scheduling guarantee는 아니다

고해상도 timer infrastructure는 jiffy보다 미세한 expiration을 지원할 수 있다. 그러나 callback latency는 interrupt masking, scheduler, CPU sleep state의 영향을 받는다.

benchmark에서 nanosecond API를 사용했다는 이유로 nanosecond 정확도라고 주장하지 않는다. clock resolution, measurement overhead, minimum timer granularity를 calibration한다. very-short duration은 measurement tool 자체 비용이 signal보다 커질 수 있다.

---

## CHAPTER 08 · sched_clock은 정확한 wall time보다 빠른 scheduler timestamp를 우선한다

kernel scheduler/tracing에는 매우 자주 호출되는 fast timestamp source가 필요하다. architecture-specific `sched_clock`은 speed와 monotonicity를 중시하며 civil time 정확성과 목적이 다르다.

CPU 간 counter skew가 존재하면 cross-CPU trace timestamp ordering이 이상하게 보일 수 있다. trace tool이 어떤 clock domain을 사용하는지 확인하고 migration 전후 event를 절대적인 nanosecond order로 과신하지 않는다.

---

## CHAPTER 09 · CPU time과 wall time은 resource consumption과 latency를 분리한다

wall time은 request가 시작해 끝날 때까지의 전체 시간이고 CPU time은 task가 실제 CPU에서 실행한 시간이다. wall 1초, CPU 20ms인 operation은 대부분 wait 상태일 가능성이 높다.

performance diagnosis에서 user CPU, system CPU, runnable wait, I/O wait를 분리하면 optimization 방향이 달라진다. algorithm 최적화는 on-CPU가 병목일 때 의미가 있다. off-CPU request에 assembly optimization을 적용해도 latency는 거의 변하지 않는다.

---

## CHAPTER 10 · process/thread CPU clock은 scheduler accounting semantics를 가진다

process CPU time은 여러 thread가 소비한 CPU를 합산할 수 있고 thread CPU clock은 해당 execution context만 측정한다. multicore에서 두 thread가 동시에 1초 실행하면 process CPU time이 wall 1초보다 크게 증가할 수 있다.

CPU budget/quota를 평가할 때 wall duration과 혼동하지 않는다. profiler sample 비율을 wall percentage로 바로 해석하기 전 sampling target이 process 전체인지 thread인지 확인한다.

---

## CHAPTER 11 · realtime clock adjustment는 step과 slew를 구분한다

clock offset이 클 때 system은 wall clock을 즉시 점프(step)시키거나 frequency를 조금 조정해 서서히 맞출(slew) 수 있다. step은 calendar timestamp를 급격히 바꾸고 slew는 일정 기간 clock rate를 변경한다.

wall-clock 기반 expiry/order logic은 두 방식 모두 영향받는다. database record ordering과 timeout은 monotonic sequence/clock을 별도로 사용한다. audit log의 civil timestamp가 역순처럼 보이면 NTP correction history를 확인한다.

---

## CHAPTER 12 · oscillator drift는 동기화가 없어도 시간이 서서히 어긋나게 한다

physical oscillator frequency는 온도, 제조 편차, aging의 영향을 받아 nominal frequency와 차이가 난다. clock synchronization은 offset뿐 아니라 frequency error를 추정해 discipline한다.

서버가 network에서 단절되면 마지막 frequency estimate로 holdover할 수 있지만 오차는 시간에 따라 누적된다. timestamp correctness requirement가 엄격하면 external reference loss 시 허용 drift와 fail-safe policy를 정의한다.

---

## CHAPTER 13 · NTP offset 계산은 network delay의 대칭성 가정에 영향을 받는다

NTP는 client/server의 transmit/receive timestamp를 이용해 round-trip delay와 clock offset을 추정한다. forward/reverse network path delay가 비대칭이면 offset estimate에 bias가 생길 수 있다.

따라서 RTT가 짧다고 time offset이 정확하다는 보장은 없다. timestamp capture 위치가 user space, kernel, NIC hardware 중 어디인지도 error budget을 바꾼다. high-accuracy system은 hardware timestamp와 network topology를 함께 관리한다. RFC 9769의 interleaved mode도 transmit timestamp accuracy 개선을 다룬다.

---

## CHAPTER 14 · NTP는 한 sample을 믿지 않고 peer/filter/discipline 상태를 관리한다

network jitter와 outlier 때문에 하나의 response로 clock을 즉시 맞추지 않는다. time synchronization implementation은 여러 sample과 peer quality를 평가하고 clock discipline algorithm으로 correction을 적용한다.

monitoring에는 offset 하나뿐 아니라 frequency error, selected source, stratum/reference, reachability, delay/jitter를 포함한다. NTP daemon이 실행 중이라는 사실과 system time이 healthy하다는 사실을 동일시하지 않는다.

---

## CHAPTER 15 · leap second와 civil-time rule은 monotonic timeline과 분리해야 한다

UTC civil time은 leap-second policy 같은 calendar rule의 영향을 받을 수 있다. system/library가 leap second를 step, smear, explicit second로 처리하는 방식도 다를 수 있다.

business timestamp가 global ordering을 요구하면 civil time alone에 의존하지 않는다. monotonic local sequence, database commit order, logical clock을 함께 사용한다. external timestamp interchange에는 timezone와 UTC normalization policy를 명시한다.

---

## CHAPTER 16 · timezone은 offset 숫자가 아니라 변경 가능한 지역 규칙 database다

`Asia/Seoul` 같은 zone ID는 역사/정책 rule을 통해 특정 instant의 UTC offset을 결정한다. `+09:00`은 한 시점의 offset만 표현하며 지역 규칙 자체를 담지 않는다.

future appointment를 저장할 때 `instant`인지 `local date/time + zone`인지 business 의미를 결정한다. 정부가 timezone/DST rule을 변경하면 future instant 변환 결과가 달라질 수 있다. tz database version도 reproducibility 조건이 될 수 있다.

---

## CHAPTER 17 · DST가 있는 지역에서는 local time이 존재하지 않거나 두 번 존재할 수 있다

spring-forward 전환에서 특정 local time interval이 건너뛰어지고 fall-back에서는 동일 local clock time이 두 UTC instant에 대응할 수 있다. local datetime만 저장하면 ambiguity가 생긴다.

scheduler는 nonexistent/ambiguous time 처리 rule을 정의한다. `매일 02:30` job이 DST 전환 날 어떻게 동작할지 명시해야 한다. offset/zone/occurrence policy를 함께 보존한다.

---

## CHAPTER 18 · deadline은 absolute wall timestamp보다 monotonic remaining budget으로 전달한다

분산 request가 `현재시각+5초` wall timestamp를 각 host에서 재계산하면 clock skew가 timeout budget을 왜곡할 수 있다. 가능한 범위에서는 monotonic local deadline을 사용하고 process/service boundary를 넘을 때 remaining duration을 전달하는 방식이 더 안전할 수 있다.

protocol이 absolute deadline timestamp를 요구하면 clock synchronization error budget을 포함한다. downstream은 이미 만료된 work를 즉시 reject해 resource를 회수하고, timeout 후 결과가 도착하는 ambiguous completion을 처리한다.

---

## CHAPTER 19 · timer wheel과 heap은 대량 timer 관리의 다른 cost profile을 가진다

priority heap은 다음 expiry를 빠르게 찾고 insert/remove에 logarithmic cost를 갖는 전형적 구조다. hierarchical timer wheel은 time bucket으로 많은 timer를 거의 O(1)에 가까운 방식으로 관리할 수 있지만 resolution과 cascade가 설계 변수다.

runtime/kernel은 workload에 맞는 timer data structure를 선택한다. application이 수백만 per-item timer를 만들기 전에 coarse deadline grouping이나 shared expiry queue로 timer cardinality를 줄일 수 있는지 검토한다.

---

## CHAPTER 20 · timer cancellation에는 이미 firing 중인 callback과의 race가 있다

cancel이 성공했다고 callback body가 절대 실행되지 않는지, queue에서만 제거되는지 API semantics를 확인한다. callback이 이미 CPU에서 실행 중이면 cancellation과 resource cleanup이 race할 수 있다.

timer target object lifetime은 callback completion까지 보존하거나 generation/token으로 stale callback을 무시한다. UI/navigation에서 이전 screen timer가 새 state를 수정하는 logical race도 같은 구조다.

---

## CHAPTER 21 · timeout과 deadline을 여러 layer에서 중복 적용하면 실제 budget이 왜곡된다

client 2초, proxy 3초, service 5초, DB 10초처럼 downstream timeout이 더 길면 caller가 포기한 뒤에도 expensive work가 계속된다. 반대로 각 layer가 독립적으로 짧은 timeout을 빼면 정상 request가 불필요하게 실패한다.

end-to-end budget을 critical path stage에 배분하고 retry cost를 포함한다. timeout metric은 어느 layer가 먼저 만료되었는지 tag해 cascade failure를 분석한다.

---

## CHAPTER 22 · cache TTL은 correctness guarantee가 아니라 stale-data policy다

TTL은 entry를 일정 시간 뒤 만료시키지만 source data가 TTL 동안 변하지 않는다는 뜻은 아니다. TTL 선택은 허용 stale window와 cache churn의 trade-off다.

wall-clock correction이 TTL 계산에 영향을 주지 않도록 runtime cache는 monotonic elapsed time을 사용할 수 있다. persistent cache expiry는 process restart를 넘으므로 wall/absolute instant와 schema version을 사용하되 clock-skew policy를 둔다.

---

## CHAPTER 23 · distributed log timestamp는 causal order를 보장하지 않는다

서로 다른 host의 clock이 수 ms만 어긋나도 request A가 B를 발생시켰는데 B log timestamp가 더 이르게 보일 수 있다. NTP가 healthy해도 network/clock error 범위 내에서 이런 inversion은 가능하다.

trace parent-child, sequence number, message offset이 causal evidence다. cross-host timestamp는 approximate alignment에 사용하고 ordering proof에는 protocol relation을 사용한다.

---

## CHAPTER 24 · Lamport clock은 causality를 scalar counter에 반영한다

각 process가 local event마다 counter를 증가시키고 message에 값을 싣고, 수신 시 `max(local, received)+1`로 갱신하면 happens-before인 event는 증가하는 logical timestamp를 갖는다.

반대는 성립하지 않는다. Lamport timestamp가 작다고 반드시 causal predecessor인 것은 아니다. total-order tie-break에는 process ID 등을 추가할 수 있지만 real elapsed time 의미는 없다. logical clock과 physical clock의 목적을 분리한다.

---

## CHAPTER 25 · vector clock은 concurrency와 causality를 더 직접 표현하지만 metadata가 커진다

participant별 counter vector를 비교하면 한 event가 다른 event를 causally precede하는지, 서로 concurrent한지 구분할 수 있다. participant 수가 커지고 membership이 변하면 vector size 관리가 복잡해진다.

version vector, dotted version vector 같은 변형은 distributed datastore conflict detection에 쓰인다. 모든 system에 vector clock을 넣기보다 conflict semantics가 필요한 state에서 선택한다.

---

## CHAPTER 26 · hybrid logical clock은 physical time과 logical ordering을 결합한다

HLC 계열은 physical clock estimate를 사용하면서 동일/역행 timestamp 상황을 logical component로 보정해 causality-friendly timestamp를 만든다. 순수 logical clock보다 wall time에 가까운 값을 유지할 수 있다.

clock skew bound와 persistence semantics를 이해해야 한다. timestamp를 primary key/TTL로 사용할 때 node restart와 clock jump가 invariant를 깨뜨리지 않는지 검증한다.

---

## CHAPTER 27 · scheduler timer와 real-time guarantee는 worst-case interference까지 포함한다

periodic real-time task가 deadline을 지키려면 execution time뿐 아니라 higher-priority interference, interrupt, lock blocking을 포함한 response-time bound가 필요하다. 평균 execution이 deadline보다 짧다는 사실은 충분하지 않다.

priority inversion protocol과 CPU reservation을 함께 설계한다. hard real-time과 soft real-time을 구분하고 missed deadline이 safety failure인지 quality degradation인지 정의한다.

---

## CHAPTER 28 · mobile timer는 power-management wakeup contract와 연결된다

Android/mobile platform은 device sleep 상태에서 timer를 정확히 깨울지 batching할지 API별 policy를 가진다. exact wakeup은 battery 비용이 크므로 platform restriction과 permission이 있을 수 있다.

사용자 알람처럼 exact timing이 필요한 작업과 analytics sync처럼 batching 가능한 작업을 구분한다. background timer loop로 platform scheduler를 우회하면 energy와 reliability 모두 악화될 수 있다.

---

## CHAPTER 29 · time bug는 boundary condition을 simulation해야 재현된다

일반 현재시각으로만 테스트하면 midnight, month/year rollover, leap day, DST transition, clock rollback, suspend/resume를 놓친다. clock interface를 dependency로 주입해 deterministic fake time으로 boundary를 이동한다.

monotonic과 wall clock을 각각 fake할 수 있어야 한다. production code가 global time API를 직접 호출하면 test가 어려워지고 hidden clock dependency가 많아진다. time source를 abstraction으로 명시한다.

---

## CHAPTER 30 · 시간 설계는 각 값의 clock domain을 타입 수준에서 드러낸다

`Long timestamp` 하나로 wall epoch millis, monotonic nanos, timeout duration을 모두 표현하면 단위와 domain을 쉽게 섞는다. `Instant`, `Duration`, `MonotonicDeadline`, `LocalDateTime+Zone`처럼 의미가 다른 타입을 분리한다.

로그/DB/protocol에는 unit과 epoch를 명시한다. duration 계산은 같은 clock domain 안에서만 수행한다. timekeeping의 최종 불변조건은 **표시 시간, 경과 시간, 실행 deadline, causal order를 서로 다른 문제로 유지하는 것**이다.