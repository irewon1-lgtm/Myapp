# PART 21 · Power and Thermal — DVFS, idle state, throttling, energy

전력과 열은 CPU frequency 하나의 문제가 아니다. workload demand, scheduler placement, idle residency, thermal sensor, battery·PMIC 상태가 feedback loop를 만든다. 짧은 benchmark의 peak 성능보다 sustained performance, energy per task, thermal safety를 함께 봐야 실제 모바일·서버 동작을 설명할 수 있다.

---

## CHAPTER 01 · performance state

performance state는 CPU가 사용할 수 있는 frequency·voltage 조합과 그 주변 operating point를 뜻한다. software가 원하는 frequency를 요청해도 firmware, silicon bin, temperature, power limit가 실제 선택 가능한 state를 제한할 수 있다.

같은 utilization에서도 선택 state가 다르면 task latency와 energy가 달라진다. nominal GHz만 보고 성능을 비교하면 boost residency와 throttling을 놓치기 쉽다. state transition 자체에도 latency와 energy가 들기 때문에 짧은 burst에서는 전환 비용이 지배적일 수 있다.

frequency residency, voltage domain, runnable load를 같은 시간축에 놓아 workload가 어느 state에서 처리됐는지 확인한다. tuning은 최고 state 체류를 늘리는 것이 아니라 latency 목표 안에서 energy와 thermal headroom을 유지하는 방향으로 평가한다.

---

## CHAPTER 02 · DVFS

DVFS는 dynamic voltage and frequency scaling으로 workload demand에 따라 전압과 주파수를 바꿔 performance와 energy를 조절한다. dynamic power는 voltage와 frequency의 영향을 크게 받으므로 작은 state 변경도 energy curve를 비선형적으로 바꿀 수 있다.

frequency를 높이면 일을 빨리 끝낼 수 있지만 voltage 상승과 leakage, thermal rise가 따라올 수 있다. 반대로 너무 낮은 state를 오래 유지하면 task가 길게 실행되어 총 energy가 오히려 늘 수 있다. 따라서 DVFS는 단순 저전력 스위치가 아니다.

trace에서 requested frequency, actual residency, task completion time, package power를 함께 본다. 짧은 benchmark 하나보다 여러 load level에서 energy-per-task curve를 만들어 operating point를 찾는 편이 강하다.

---

## CHAPTER 03 · governor

governor는 utilization, scheduler signal, policy를 바탕으로 어느 performance state를 선택할지 결정한다. 동일한 application binary도 governor가 다르면 burst response와 steady-state frequency가 달라져 latency distribution이 변할 수 있다.

반응이 너무 느리면 interactive task가 낮은 frequency에서 시작해 deadline을 놓치고, 너무 공격적이면 작은 burst마다 boost해 energy와 heat를 낭비한다. feedback delay와 sampling interval은 governor stability에 직접 영향을 준다.

실험에는 governor 이름만 적지 말고 tunable, update interval, utilization trace를 같이 남긴다. code 변경 전후 benchmark에서 governor state가 달랐다면 성능 차이를 application effect로 단정하지 않는다.

---

## CHAPTER 04 · boost

boost는 짧은 고부하 구간에서 nominal보다 높은 performance state를 허용해 response time을 줄이는 mechanism이다. available boost는 temperature, current, shared power budget, active core 수에 따라 달라질 수 있다.

초기 몇 초 benchmark가 빠르다가 이후 느려지는 현상은 algorithm 변화가 아니라 boost budget 소진일 수 있다. boost가 길어질수록 thermal headroom을 쓰고 이후 throttling을 유발해 전체 session의 평균 latency를 악화시킬 수도 있다.

boost residency와 temperature 상승, workload completion을 같은 그래프에 놓는다. peak score와 sustained score를 분리하고 실제 product workload가 어느 duration에 가까운지 기준을 정한다.

---

## CHAPTER 05 · thermal throttling

thermal throttling은 sensor가 허용 범위에 접근할 때 frequency, voltage, core availability를 낮춰 hardware를 보호하는 제어다. 사용자에게는 동일 workload가 갑자기 느려지는 것처럼 보이지만 실제 원인은 thermal policy일 수 있다.

CPU만 뜨거운 것이 아니라 GPU, modem, charger, display가 같은 enclosure thermal budget을 공유한다. charging 중 game이나 camera workload에서 CPU code가 변하지 않았는데도 throttle point가 앞당겨질 수 있다.

thermal event, temperature, frequency residency, frame/request latency를 함께 기록한다. threshold를 임의로 높여 성능을 얻는 방식은 safety contract를 깨므로 optimization으로 취급하지 않는다.

---

## CHAPTER 06 · thermal sensors

thermal sensor는 CPU cluster, GPU, battery, skin, PMIC처럼 서로 다른 위치를 측정한다. 각 sensor의 sampling interval과 calibration, physical lag가 다르므로 한 temperature 값으로 device 전체 열 상태를 대표할 수 없다.

hotspot sensor가 급격히 오르지만 skin temperature는 늦게 반응할 수 있고 반대 상황도 가능하다. policy가 어떤 sensor를 어떤 weight로 사용하는지 모르면 throttling 원인을 잘못 해석한다.

trace에는 sensor ID와 zone, trip point, sampling time을 보존한다. 서로 다른 firmware version에서 sensor mapping이 바뀌는 경우도 있으므로 model과 build identity를 같이 기록한다.

---

## CHAPTER 07 · hysteresis

hysteresis는 threshold 근처에서 throttle과 unthrottle이 빠르게 반복되는 oscillation을 줄이기 위해 진입점과 해제점을 다르게 두는 방식이다. feedback loop에 delay가 있을 때 특히 중요하다.

해제 threshold가 너무 가깝다면 frequency가 계속 출렁이고 latency variance가 커질 수 있다. 반대로 너무 멀면 workload가 식은 뒤에도 낮은 state에 오래 머물러 unnecessary performance loss가 생긴다.

state transition frequency와 temperature slope를 함께 본다. 정책 변경은 평균 temperature보다 oscillation count와 p99 latency가 개선되는지 확인해 평가한다.

---

## CHAPTER 08 · idle state

idle state는 runnable work가 없을 때 CPU 일부 회로를 끄거나 clock을 줄여 energy를 아끼는 상태다. 깊은 idle일수록 static power가 줄지만 entry/exit latency와 state restore 비용이 커질 수 있다.

짧은 timer가 자주 발생하면 CPU가 deep idle에 도달하지 못하고 shallow state만 반복한다. 평균 CPU utilization이 낮아도 battery가 빨리 줄 수 있는 이유다.

idle residency, wakeup reason, next timer를 함께 수집한다. idle 최적화는 deep state 비율을 최대화하는 것이 아니라 application latency constraint 안에서 불필요한 wakeup을 줄이는 작업이다.

---

## CHAPTER 09 · idle governor

idle governor는 예상 idle duration과 latency requirement를 보고 어떤 idle state에 들어갈지 선택한다. 미래 wakeup까지 시간이 짧다면 deep state의 exit cost가 절감 energy보다 클 수 있다.

예상 duration이 반복적으로 틀리면 governor는 잘못된 state를 선택하고 energy 또는 latency를 잃는다. periodic background task와 bursty interactive workload는 서로 다른 prediction challenge를 만든다.

state selection, predicted idle, actual residency, wakeup latency를 비교한다. tuning은 특정 state를 강제하는 대신 prediction error와 workload pattern을 이해하는 데서 시작한다.

---

## CHAPTER 10 · tickless

tickless 동작은 CPU가 idle일 때 불필요한 periodic scheduler tick을 줄여 긴 sleep을 가능하게 한다. 하지만 pending timer, RCU, housekeeping work가 남아 있으면 실제 deep idle은 제한된다.

periodic tick이 사라졌다고 wakeup이 모두 없어지는 것은 아니다. driver polling이나 application alarm이 더 자주 CPU를 깨울 수도 있다. system-wide wakeup source를 함께 봐야 한다.

per-CPU timer event와 idle residency를 correlation한다. tickless configuration만 바꾸고 battery가 좋아졌다고 결론내리지 말고 실제 wakeup frequency 감소를 증명한다.

---

## CHAPTER 11 · wakeup cost

wakeup cost는 idle state에서 CPU를 깨우는 시간뿐 아니라 clock/power domain restore, cache refill, scheduler dispatch까지 포함한다. 매우 짧은 task에서는 useful work보다 wakeup overhead가 더 클 수 있다.

여러 작은 작업을 batch하면 wakeup 횟수를 줄여 energy를 절약할 수 있지만 interactive response delay가 늘 수 있다. background telemetry와 user input은 같은 batching policy를 사용할 수 없다.

wakeup reason, latency, task duration, energy를 함께 측정한다. optimization은 wakeup count와 user-visible deadline을 동시에 만족하는 지점을 찾는다.

---

## CHAPTER 12 · transition latency

performance/idle state transition은 즉시 일어나지 않는다. clock source 변경, voltage settle, firmware coordination 같은 단계가 필요해 요청 시점과 실제 state 도달 시점 사이에 delay가 있다.

state를 너무 자주 바꾸면 transition 자체가 workload를 방해할 수 있다. 짧은 task마다 deep idle과 high boost를 왕복하면 energy와 latency 모두 나빠질 수 있다.

requested state와 actual residency timestamp를 분리해 기록한다. control loop의 update interval보다 transition latency가 길면 oscillation 가능성을 우선 검토한다.

---

## CHAPTER 13 · heterogeneous CPU

heterogeneous CPU는 서로 다른 performance/energy 특성을 가진 core를 한 system에 배치한다. 같은 thread가 어느 core에서 실행되는지에 따라 execution time, cache behavior, thermal effect가 달라진다.

큰 core는 burst latency에 유리하지만 energy와 thermal cost가 크고 작은 core는 sustained efficiency에 유리할 수 있다. 모든 runnable task를 큰 core로 보내는 정책은 short benchmark는 좋아도 battery와 thermal stability를 해칠 수 있다.

core type별 residency, migration, IPC, energy를 기록한다. task class와 deadline에 맞춘 placement가 실제 end-to-end SLO를 개선하는지 확인한다.

---

## CHAPTER 14 · energy-aware scheduling

energy-aware scheduling은 CPU capacity와 energy model을 사용해 runnable task를 적절한 core에 배치하려는 정책이다. load balance만 보는 scheduler와 달리 같은 throughput에서 energy가 낮은 placement를 찾으려 한다.

model이 실제 silicon이나 thermal state와 어긋나면 선택이 잘못될 수 있다. thermal pressure, uclamp, affinity가 동시에 적용되면 simple energy model만으로 결과를 예측하기 어렵다.

scheduler placement와 actual power를 함께 검증한다. model 변경 후 task migration과 latency가 예상대로 변했는지 workload class별로 확인한다.

---

## CHAPTER 15 · thermal pressure

thermal pressure는 nominal CPU capacity와 thermal limit 때문에 실제 사용 가능한 capacity의 차이를 scheduler에 알려 주는 신호다. 과열된 big core를 여전히 full-capacity로 계산하면 task placement가 잘못될 수 있다.

pressure가 높을 때 runnable task가 다른 CPU로 이동하면서 cache/NUMA cost가 생길 수 있다. application은 CPU utilization만 보고 '여유가 있다'고 판단하면 실제 capacity 부족을 놓친다.

thermal pressure, runqueue delay, migration, frequency를 같은 timeline에 둔다. temperature만 낮추는 수정이 아니라 deadline miss가 줄었는지까지 확인한다.

---

## CHAPTER 16 · shared power budget

CPU, GPU, memory, modem은 device 전체의 power/thermal budget을 공유할 수 있다. 한 subsystem이 boost하면 다른 subsystem의 available headroom이 줄어 전체 application 성능이 달라진다.

GPU-heavy frame에서 CPU frequency가 낮아진다고 scheduler 문제라고 단정하면 안 된다. charging과 radio transmission도 같은 budget을 소비할 수 있다.

subsystem별 power state와 workload phase를 함께 측정한다. optimization은 한 component 점수를 최대화하는 대신 end-to-end latency와 energy를 기준으로 한다.

---

## CHAPTER 17 · memory/interconnect

memory controller와 interconnect도 energy를 사용하고 bandwidth saturation이 CPU execution을 제한한다. memory-bound workload는 CPU frequency를 올려도 성능이 거의 늘지 않으면서 power만 증가할 수 있다.

cache miss와 DRAM traffic이 높은 상태에서 big core boost를 강제하면 thermal budget만 소모할 수 있다. data layout과 locality 개선이 더 큰 energy win이 될 수 있다.

bandwidth, cache miss, CPU residency, package energy를 같이 본다. workload가 compute-bound인지 memory-bound인지 분류한 뒤 power policy를 적용한다.

---

## CHAPTER 18 · race to idle

race-to-idle은 높은 performance state로 일을 빨리 끝내고 더 오래 깊은 idle에 머무르는 전략이다. 그러나 high state의 energy efficiency가 나쁘거나 task가 memory-bound라면 총 energy가 줄지 않을 수 있다.

짧은 user interaction과 장시간 batch 작업은 최적 전략이 다르다. background job을 무조건 최고 frequency로 실행하면 thermal rise 때문에 이후 foreground latency를 악화시킬 수 있다.

operation completion time과 이후 idle residency를 합쳐 energy per task를 계산한다. active phase만 재면 race-to-idle의 전체 효과를 놓친다.

---

## CHAPTER 19 · energy per task

energy per task는 한 operation을 완료하는 데 소비된 총 energy를 측정해 power와 performance를 하나의 지표로 연결한다. 평균 power가 높아도 task가 훨씬 빨리 끝나면 총 energy는 낮을 수 있다.

반대로 power가 낮아졌지만 task duration이 크게 늘면 battery 관점의 이득이 없을 수 있다. queue wait와 idle interval까지 어떤 범위에 포함할지 측정 window를 명확히 해야 한다.

동일 input과 state에서 energy와 latency distribution을 같이 기록한다. optimization은 energy와 SLO 중 하나를 숨기지 않고 trade-off curve로 제시한다.

---

## CHAPTER 20 · battery profiler

battery profiler는 CPU, screen, radio, wakelock 같은 activity를 energy model과 결합해 attribution한다. model 기반 추정은 useful하지만 실제 wall power와 차이가 날 수 있다.

high-cardinality attribution이나 background shared work는 어느 app에 비용을 귀속할지 애매하다. device vendor model과 OS version이 바뀌면 같은 workload의 추정값이 달라질 수도 있다.

hardware counter, fuel gauge, profiler estimate를 가능한 범위에서 교차검증한다. 절대값보다 동일 조건의 상대 변화에 더 강한 도구인지 이해하고 사용한다.

---

## CHAPTER 21 · charging thermal

charging은 battery와 PMIC에 추가 열을 만들고 thermal headroom을 줄일 수 있다. 같은 benchmark라도 충전 중과 battery mode에서 sustained performance가 달라지는 이유다.

fast charging phase와 battery level에 따라 current와 heat가 변하므로 '충전기 연결됨' 하나의 flag로 충분하지 않다. 케이스와 ambient temperature도 skin cooling을 바꾼다.

benchmark protocol에 charge state, battery level, charger type, ambient temperature를 포함한다. code change 효과와 thermal environment 효과를 분리해야 한다.

---

## CHAPTER 22 · steady state

steady state는 initial boost, cold cache, background compilation, temperature ramp가 지나 system state가 비교적 안정된 측정 구간을 뜻한다. workload 목적에 따라 steady state와 cold-start 성능을 별도로 봐야 한다.

짧은 test만 반복하면 boost를 측정하고 실제 장시간 사용을 대표하지 못할 수 있다. 반대로 warmup을 지나치게 길게 하면 사용자 startup 문제를 숨긴다.

frequency와 temperature가 안정되는 조건을 사전에 정의한다. 측정 window와 제외 구간을 결과와 함께 기록해 재현성을 확보한다.

---

## CHAPTER 23 · thermal safety

thermal safety는 performance 목표보다 우선하는 hardware·사용자 보호 제약이다. sensor failure, fan/cooling 문제, runaway workload에서도 안전 state로 이동할 fail-safe가 필요하다.

threshold를 올리거나 throttle을 비활성화해 benchmark score를 얻는 방식은 reliability contract를 파괴한다. safety margin은 worst-case ambient와 component tolerance를 고려해야 한다.

fault injection으로 sensor 이상이나 cooling degradation을 재현하고 safe shutdown/throttle이 작동하는지 확인한다. 성능 tuning과 safety policy 변경은 별도 review 경계를 둔다.

---

## CHAPTER 24 · PM QoS

PM QoS는 latency 또는 throughput 요구를 kernel power management에 전달해 너무 깊은 idle이나 느린 transition을 제한할 수 있다. 특정 device나 workload가 response constraint를 가질 때 유용하다.

constraint가 오래 남으면 system 전체가 high-power state에 머무를 수 있다. owner가 종료됐는데 request가 해제되지 않는 leak은 battery 문제로 이어진다.

활성 QoS request와 owner, duration을 추적한다. latency win과 idle residency loss를 같이 측정해 필요한 범위에서만 constraint를 유지한다.

---

## CHAPTER 25 · wake lock

wake lock은 중요한 작업 중 system suspend를 막아 progress를 보장한다. 그러나 scope가 넓거나 release가 누락되면 deep sleep이 사라져 background battery drain이 발생한다.

network retry나 error path에서 lock이 남는 경우가 흔하다. long-running task는 indefinite lock보다 timeout과 persisted state를 사용해 restart 가능하게 만드는 편이 안전하다.

owner, acquisition stack, hold time, wakeup count를 추적한다. 정상 작업 완료뿐 아니라 cancellation·crash 경로에서도 release되는지 테스트한다.

---

## CHAPTER 26 · timer storm

timer storm은 많은 component가 짧은 간격으로 독립 timer를 예약해 CPU를 반복 깨우는 상태다. 개별 timer는 작아 보여도 system-wide로 합치면 deep idle residency를 파괴한다.

periodic polling, retry, telemetry가 서로 다른 주기로 실행되면 wakeup이 겹치지 않아 radio와 CPU가 계속 깨어 있을 수 있다. batching과 flex window가 효과적일 수 있다.

per-timer owner와 wakeup histogram을 수집한다. timer 개수보다 실제 wakeup과 idle break를 만드는 timer를 우선 줄인다.

---

## CHAPTER 27 · power trace

power trace는 scheduler, frequency, idle state, thermal event, wakelock을 하나의 timeline에 연결한다. battery percentage만으로는 어느 subsystem이 언제 비용을 만들었는지 알 수 없다.

trace 자체가 너무 무거우면 observer effect로 state residency가 바뀔 수 있다. 필요한 category와 sampling rate를 선택해 overhead를 제한한다.

user-visible latency span과 power state를 correlation한다. 특정 optimization이 CPU time은 줄였지만 wakeup이나 GPU cost를 늘렸는지 end-to-end로 확인한다.

---

## CHAPTER 28 · benchmark protocol

power/thermal benchmark는 ambient temperature, battery level, charging, brightness, network, background task, warmup을 통제해야 한다. 하나라도 다르면 같은 binary의 결과가 크게 달라질 수 있다.

반복 횟수와 cooling interval도 중요하다. 연속 run은 앞 run의 heat를 다음 run에 전달해 independent sample이 아니게 된다.

protocol을 자동화하고 raw temperature·frequency trace를 함께 보존한다. 평균 score만 저장하지 말고 variance와 throttle 발생 시점을 기록한다.

---

## CHAPTER 29 · energy SLO

energy SLO는 operation당 energy, background wakeup, sustained thermal state를 product 품질 목표에 포함하는 방식이다. latency만 만족하면서 battery를 과도하게 소비하는 구현을 성공으로 보지 않는다.

workload class별 허용 budget이 다를 수 있다. foreground gesture와 overnight sync를 같은 energy/latency 목표로 평가하면 잘못된 최적화를 유도한다.

release gate에는 latency와 energy regression을 함께 둔다. noise가 큰 환경에서는 threshold와 sample size를 명시해 flaky gate를 피한다.

---

## CHAPTER 30 · closed loop

power management는 demand가 state selection을 바꾸고, 그 state가 temperature와 power를 바꾸며, 다시 available capacity와 scheduler signal을 바꾸는 closed loop다. 각 controller를 독립적으로 tune하면 상호작용 때문에 oscillation이 생길 수 있다.

feedback delay, gain, hysteresis를 이해해야 한다. boost governor와 thermal controller가 서로 반대 방향으로 빠르게 반응하면 frequency가 출렁이고 tail latency가 커질 수 있다.

최종 검증은 workload→frequency→temperature→throttle→latency의 전체 loop를 trace한다. 안정성, energy, performance 세 축이 함께 만족될 때만 tuning을 완료로 본다.
