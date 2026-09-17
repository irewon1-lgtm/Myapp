# PART 21 · Power and Thermal — DVFS, idle state, throttling, energy accounting

같은 binary가 같은 입력을 처리해도 실행시간이 항상 일정하지 않은 이유 중 하나는 hardware가 고정된 frequency·voltage·power state로 동작하지 않기 때문이다. 현대 CPU·GPU·memory·interconnect는 workload, temperature, battery, policy에 따라 동작점을 바꾸고, operating system은 scheduler·frequency governor·idle governor·thermal policy를 통해 성능과 energy를 동시에 조절한다. 이 계층을 무시하면 `처음엔 빠른데 5분 뒤 느려짐`, `벤치마크를 반복할수록 결과가 달라짐`, `CPU 사용률은 같은데 latency가 변함` 같은 현상을 설명하지 못한다.

---

## CHAPTER 01 · Performance state는 단순 clock frequency 하나가 아니다

CPU performance는 instruction retirement rate, microarchitecture utilization, memory stalls, operating frequency, voltage, thermal headroom의 함수다. Frequency가 높아지면 동일 cycle 수의 code는 더 빨리 끝날 가능성이 있지만 실제 실행시간은 memory latency·cache miss·parallel bottleneck에 의해 제한될 수 있다. Hardware-managed performance state에서는 software가 특정 MHz를 직접 고르는 대신 performance range나 energy-performance hint를 주고 processor가 내부 policy로 동작점을 선택할 수 있다.

Linux CPU performance scaling subsystem은 policy object와 scaling driver/governor를 분리한다. `scaling_cur_freq` 같은 숫자를 곧바로 실제 instruction rate로 해석하지 않고 driver semantics와 sampling 방식까지 확인해야 한다. Performance 분석은 frequency, IPC, stall, utilization을 같이 봐야 한다.

---

## CHAPTER 02 · DVFS는 frequency를 낮추는 기능이 아니라 voltage-frequency operating point 선택 문제다

Dynamic Voltage and Frequency Scaling은 workload demand에 따라 operating point를 변경해 energy와 performance를 조절한다. Dynamic power는 대략 capacitance·voltage²·frequency와 연관되므로 voltage 변화는 energy에 큰 영향을 줄 수 있다. 하지만 frequency를 낮추면 작업이 오래 실행되어 static/leakage energy와 wake duration이 늘어날 수 있다.

따라서 `낮은 frequency가 항상 적은 energy`라는 결론은 성립하지 않는다. 일부 workload는 짧게 높은 performance로 끝내고 깊은 idle state에 더 오래 머무는 race-to-idle 전략이 유리할 수 있고, 다른 workload는 높은 voltage/frequency의 비효율 때문에 낮은 operating point가 더 낫다. Energy optimization은 workload duty cycle과 idle opportunity를 포함해야 한다.

---

## CHAPTER 03 · Governor는 utilization을 performance request로 변환하는 policy다

CPUFreq governor는 scheduler나 utilization signal을 보고 desired performance를 선택하는 policy layer다. `performance`, `powersave`, `schedutil` 같은 이름은 특정 목적과 implementation을 가진다. 특히 scheduler-integrated governor는 runnable workload와 CPU capacity 정보를 이용해 scaling request를 만들 수 있다.

Governor tuning은 min/max frequency만 조절하는 것과 다르다. Ramp-up latency가 너무 느리면 burst request가 낮은 frequency에서 시작해 tail latency가 커질 수 있고, 지나치게 aggressive하면 short idle gap에도 높은 power state를 유지해 energy와 thermal pressure를 높일 수 있다. workload burst length와 response-time target을 같이 측정해야 한다.

---

## CHAPTER 04 · Boost와 turbo는 base frequency 위의 무제한 성능이 아니다

Processor는 thermal·electrical·current·power budget이 허용될 때 base operating point보다 높은 performance state를 잠시 사용할 수 있다. 가능한 boost duration과 frequency는 active core 수, package temperature, power delivery, previous workload history에 영향을 받을 수 있다. 그래서 cold-start benchmark의 첫 몇 초와 sustained benchmark의 10분 후 결과가 다를 수 있다.

Benchmark를 비교할 때 `CPU model이 같다`만으로 조건이 같다고 볼 수 없다. cooling state, battery/AC, ambient temperature, governor, boost permission, background load를 기록해야 한다. 특히 mobile SoC에서는 thermal envelope가 작아 sustained performance와 burst performance 차이가 크다.

---

## CHAPTER 05 · Thermal throttling은 고장 상태가 아니라 보호 control loop다

열은 workload가 소비한 electrical power가 silicon과 package에 축적된 결과다. Temperature가 policy threshold에 접근하면 system은 frequency/voltage 제한, scheduler placement 변경, GPU/CPU cap, display/charging 제한, background work throttling 같은 mitigation을 적용할 수 있다. Android Thermal framework는 severity level을 통해 이런 thermal stress를 platform과 app에 전달한다.

Throttle이 발생했다는 사실만으로 hardware fault를 의미하지 않는다. 문제는 workload가 요구하는 sustained performance와 device thermal design이 맞지 않는 경우다. 성능 튜닝은 peak benchmark보다 **thermal steady state에서 목표 latency를 유지하는가**를 봐야 한다.

---

## CHAPTER 06 · Temperature sensor는 하나의 절대 진실이 아니라 control input이다

SoC에는 CPU, GPU, battery, skin, USB 등 여러 thermal sensor가 있을 수 있고 각 sensor는 서로 다른 physical location과 time constant를 가진다. Android Thermal HAL은 sensor reading과 severity를 framework에 제공하지만 제품 policy는 raw temperature 하나보다 사용자 안전·surface temperature·component limit를 종합할 수 있다.

성능 incident에서 `CPU 70°C`만 보고 throttling 여부를 단정하면 안 된다. 실제 limit은 device별로 다르고 skin temperature나 battery constraint가 먼저 성능을 제한할 수 있다. thermal status, frequency cap, cooling device state, workload timeline을 같이 기록해야 한다.

---

## CHAPTER 07 · Hysteresis가 없으면 thermal control이 oscillation할 수 있다

Threshold 하나에서 즉시 throttle을 켜고 같은 threshold에서 바로 해제하면 temperature가 경계 근처에서 오르내릴 때 frequency cap이 빠르게 토글될 수 있다. Hysteresis는 진입 threshold와 해제 조건을 분리해 control instability를 줄인다. Thermal control은 feedback system이므로 sensor delay, thermal inertia, mitigation strength가 모두 loop dynamics에 영향을 준다.

성능 trace에서 frequency가 주기적으로 내려갔다 올라오는 패턴이 보이면 workload phase뿐 아니라 thermal feedback oscillation을 의심할 수 있다. 단일 시점의 sensor value가 아니라 시간축을 봐야 한다.

---

## CHAPTER 08 · CPU idle state는 scheduler idle과 동일한 의미가 아니다

Scheduler 관점에서 runnable task가 없어 CPU가 idle이 되면 kernel idle loop가 hardware low-power state를 선택할 수 있다. CPUIdle subsystem은 governor와 driver를 분리하고, idle state마다 target residency와 exit latency가 다르다. 깊은 state는 더 많은 energy를 절약할 수 있지만 진입·복귀 비용이 크다.

따라서 latency-sensitive workload는 너무 깊은 idle state가 wakeup latency를 늘릴 수 있고, 너무 얕은 state만 쓰면 energy를 낭비한다. PM QoS 같은 constraint는 허용 가능한 resume latency를 제한해 idle-state 선택에 영향을 준다.

---

## CHAPTER 09 · Idle governor는 다음 wakeup을 예측해 state를 고른다

Kernel은 가장 가까운 timer event 시점을 알 수 있지만 external interrupt가 언제 올지는 완전히 예측할 수 없다. CPUIdle governor는 과거 idle duration과 timer 정보를 바탕으로 어떤 state에 들어갈지 선택한다. 잘못 예측해 너무 깊은 state에 들어가면 짧은 idle interval에서 exit cost를 낭비하고, 너무 얕게 들어가면 긴 idle opportunity의 energy saving을 놓친다.

이 결정은 microseconds 수준 latency와 battery life에 모두 영향을 준다. Realtime workload와 mobile battery workload가 같은 idle policy를 선호하지 않을 수 있다.

---

## CHAPTER 10 · Tickless idle은 불필요한 wakeup 자체를 줄인다

Periodic scheduler tick이 계속 CPU를 깨우면 깊은 idle state residency가 짧아진다. Tickless configuration은 idle CPU에서 periodic tick을 중지하고 다음 실제 event까지 sleep할 수 있게 해 energy efficiency를 높인다. 하지만 timer density와 background housekeeping이 많으면 tick을 꺼도 CPU가 자주 깨어난다.

Energy debugging에서는 `CPU utilization 1%`보다 wakeup rate와 idle residency가 더 중요한 경우가 있다. 짧은 task가 초당 수백 번 CPU를 깨우면 average utilization은 낮아도 deep idle 진입이 차단될 수 있다.

---

## CHAPTER 11 · Wakeup은 실행시간보다 큰 energy cost를 만들 수 있다

작은 telemetry task 하나가 200µs만 CPU를 쓰더라도 CPU cluster, memory, interconnect가 low-power state에서 깨어나고 다시 idle로 돌아가는 transition cost가 발생한다. 여러 background component가 서로 다른 timer를 사용하면 wakeup이 분산되어 energy가 증가한다.

Timer coalescing과 batching은 여러 작업을 같은 wake window에 모아 idle residency를 늘린다. 다만 batching이 deadline을 침해하면 UX가 나빠진다. Energy optimization은 `작업량 감소`뿐 아니라 **작업 시점 정렬** 문제다.

---

## CHAPTER 12 · Frequency transition latency도 workload에 보인다

Performance request를 바꿔도 hardware가 즉시 target state에 도달하는 것은 아니다. Transition latency가 존재하고 일부 platform은 firmware/hardware가 내부적으로 scaling한다. 수백 microsecond burst가 transition보다 짧다면 governor가 높은 performance를 요청할 때 이미 workload가 끝날 수 있다.

따라서 burst workload의 latency 개선은 frequency max를 올리는 것보다 request prediction, sustained minimum, scheduler hint가 더 중요할 수 있다. Trace에서 frequency request 시점과 actual execution interval을 맞춰야 한다.

---

## CHAPTER 13 · Heterogeneous CPU는 core capacity 자체가 다르다

Mobile/modern CPU는 서로 다른 performance·efficiency 특성을 가진 core cluster를 가질 수 있다. Scheduler는 task utilization, CPU capacity, energy model, affinity 등을 이용해 placement를 결정한다. 작은 core에 배치된 task가 max frequency여도 큰 core의 낮은 frequency보다 느릴 수 있다.

`CPU frequency`만 비교하면 heterogeneous system에서 의미가 약하다. Scheduler capacity normalization, task migration, cluster wake cost를 함께 봐야 한다. Latency-sensitive thread를 무조건 big core에 고정하면 battery와 thermal pressure를 높여 전체 sustained performance를 악화시킬 수 있다.

---

## CHAPTER 14 · Energy-Aware Scheduling은 가장 빠른 CPU가 아니라 효율적인 placement를 찾는다

Energy model을 사용할 수 있는 scheduler는 expected utilization과 CPU energy cost를 기준으로 task placement를 평가할 수 있다. 목표는 모든 task를 최강 core에 넣는 것이 아니라 performance requirement를 만족하면서 energy를 줄이는 것이다. 이 판단은 CPU capacity와 frequency scaling policy에 의존한다.

잘못된 utilization estimate나 affinity restriction은 scheduler의 선택 범위를 제한한다. 따라서 application이 thread affinity를 직접 강제할 때는 OS energy policy를 우회하는 비용까지 고려해야 한다.

---

## CHAPTER 15 · Thermal pressure는 scheduler가 보는 usable capacity를 줄인다

CPU가 thermal throttling으로 최대 performance를 내지 못하면 scheduler가 nominal capacity를 그대로 믿을 경우 task placement가 잘못될 수 있다. Modern scheduler는 thermal pressure를 capacity에 반영할 수 있다. 즉 같은 logical CPU라도 현재 thermal state에 따라 effective capacity가 달라진다.

이 메커니즘은 sustained workload에서 중요하다. 처음에는 high-capacity core에 task를 몰았지만 heat가 올라 capacity가 줄고 task가 다른 cluster로 이동하면서 latency pattern이 바뀔 수 있다. Thread trace와 thermal pressure를 함께 봐야 한다.

---

## CHAPTER 16 · GPU·NPU·memory bandwidth도 동일한 power budget을 경쟁할 수 있다

SoC package의 total power/thermal envelope는 CPU만 사용하는 것이 아니다. GPU, NPU, ISP, modem, memory controller가 동시에 active하면 각 subsystem에 허용되는 performance budget이 달라질 수 있다. AI inference와 UI rendering을 동시에 수행할 때 CPU code가 변하지 않았는데 CPU frequency가 낮아질 수 있다.

System optimization은 component 하나의 benchmark를 합산해서 예측할 수 없다. Concurrent workload에서 shared power rail과 thermal coupling을 측정해야 한다.

---

## CHAPTER 17 · Memory frequency와 interconnect scaling은 CPU stall을 바꾼다

Memory-bound workload에서 CPU frequency만 높여도 DRAM/interconnect가 bottleneck이면 성능이 거의 늘지 않는다. 반대로 memory controller가 low-power state나 낮은 frequency에 머물면 CPU IPC가 떨어질 수 있다. SoC는 memory bandwidth demand에 따라 interconnect/DRAM operating point도 scaling할 수 있다.

Performance counter의 memory-stall 증가와 frequency trace를 같이 보면 CPU compute bottleneck인지 memory service bottleneck인지 구분할 수 있다. Energy 최적화 역시 CPU와 memory subsystem을 분리해 측정해야 한다.

---

## CHAPTER 18 · Race-to-idle은 workload shape에 따라 유효성이 달라진다

작업을 높은 performance state에서 빨리 끝내고 deep idle에 오래 머무르는 전략은 bursty workload에서 유리할 수 있다. 그러나 지속 workload에서는 높은 voltage/frequency가 계속 유지되어 thermal throttling을 앞당길 수 있다. 반대로 너무 낮은 performance state는 작업 시간을 늘려 display/modem/memory 같은 다른 subsystem을 더 오래 active하게 만들 수 있다.

Energy per operation을 측정해야지 instantaneous power만 비교하면 안 된다. `5W로 1초`와 `3W로 2초`는 total energy가 다르며 주변 subsystem residency까지 포함하면 더 복잡해진다.

---

## CHAPTER 19 · Joule per task가 Watt보다 application efficiency를 더 잘 표현할 수 있다

Power는 순간적인 energy rate이고 energy는 시간 적분이다. 앱 기능 하나가 사용하는 battery impact를 비교하려면 operation 당 energy, frame 당 energy, request 당 energy 같은 단위가 유용하다. Peak Watt가 낮아도 시간이 길어 total Joule이 더 클 수 있다.

측정 시스템은 rail-level power, subsystem residency, battery discharge estimate 등 서로 다른 정확도를 가진다. Android PowerStats는 platform power entity 상태와 energy data를 수집할 수 있고 Perfetto·statsd 같은 client가 이를 연계할 수 있다. Measurement source의 sampling cadence와 attribution model을 문서화해야 한다.

---

## CHAPTER 20 · Battery percentage는 fine-grained power profiler가 아니다

Battery state-of-charge는 chemistry, voltage curve, fuel-gauge model, temperature에 영향을 받으며 작은 code change의 energy 차이를 즉시 보여 주기에는 coarse하다. 앱 최적화 검증은 rail/power monitor, subsystem residency, repeatable workload를 사용해야 한다.

실험은 화면 밝기, radio state, battery temperature, charger 상태, background sync를 통제해야 한다. 그렇지 않으면 code change보다 환경 variance가 더 커질 수 있다.

---

## CHAPTER 21 · Charging은 thermal envelope를 줄일 수 있다

배터리 charging 자체가 열을 만들고 power-management policy를 바꿀 수 있다. 같은 workload를 충전 중과 battery-only 상태에서 실행하면 available thermal headroom과 boost behavior가 달라질 수 있다. Mobile benchmark에서 charger 연결 여부는 단순 편의 조건이 아니라 실험 변수다.

장시간 performance test는 battery percentage를 유지하기 위해 충전하면서 돌리기 쉽지만, 그 결과가 실제 battery-use scenario를 대표하지 않을 수 있다. 목표 사용자 상황에 맞는 power source 조건을 명시해야 한다.

---

## CHAPTER 22 · Thermal steady state 이전의 benchmark는 sustained capacity를 과대평가할 수 있다

Cold device에서 시작한 짧은 benchmark는 silicon과 chassis에 축적된 heat가 적어 높은 boost state를 오래 유지할 수 있다. 실제 사용자 workload가 20분 지속된다면 첫 30초 결과는 대표값이 아니다. Thermal mass와 cooling path 때문에 temperature response는 workload보다 늦게 따라온다.

Sustained test는 latency/frequency/temperature가 일정 범위로 안정되는 시점을 관찰하고 steady-state section을 따로 분석해야 한다. Warm-up이 필요한 JIT benchmark와 thermal warm-up은 서로 다른 현상이므로 분리해야 한다.

---

## CHAPTER 23 · Thermal shutdown은 performance tuning보다 safety invariant가 우선한다

Android thermal framework의 severe/critical/emergency/shutdown 단계는 단순 UI 경고가 아니다. platform은 component 보호를 위해 workload 제한·기능 비활성화·최종 shutdown까지 수행할 수 있다. Application이 thermal mitigation을 우회하려 하거나 지속적인 boost를 강제하면 안전 policy와 충돌한다.

앱은 thermal status callback을 활용해 background work, rendering quality, inference rate를 낮출 수 있다. `최대한 빠르게 유지`보다 **기기 안전과 핵심 UX를 유지하는 degradation policy**가 중요하다.

---

## CHAPTER 24 · Power QoS는 latency constraint가 energy policy를 제한하는 방식이다

일부 workload는 deep idle의 resume latency나 낮은 performance state를 허용할 수 없다. Linux PM QoS는 system component가 latency constraint를 전달해 power-saving mechanism 선택을 제한할 수 있게 한다. Constraint가 너무 엄격하면 CPU가 깊은 idle state에 들어가지 못해 system-wide energy가 증가한다.

Driver가 필요 이상으로 QoS request를 오래 유지하면 application이 CPU를 쓰지 않아도 battery drain이 발생할 수 있다. Power issue를 디버깅할 때 active task만 보지 말고 lingering latency constraint도 확인해야 한다.

---

## CHAPTER 25 · Wake lock은 CPU utilization이 아니라 suspend opportunity를 제어한다

Android와 mobile platform의 wake mechanism은 system suspend/deep idle 진입을 막거나 특정 work가 완료될 때까지 active state를 유지하게 할 수 있다. Wake lock leak은 CPU 100%가 아니어도 device가 깊은 sleep으로 못 들어가 battery를 크게 소모하게 만든다.

Correctness 관점에서는 필요한 작업 중 sleep으로 들어가면 안 되고, energy 관점에서는 작업 종료 즉시 constraint를 release해야 한다. Acquisition/release lifetime을 resource invariant로 관리해야 한다.

---

## CHAPTER 26 · Timer storm과 background polling은 deep sleep을 파괴한다

수십 component가 각자 1초 polling timer를 두면 실제 work가 거의 없어도 CPU와 subsystem이 지속적으로 깨어난다. Polling은 event-driven notification보다 state freshness를 단순하게 만들지만 wakeup budget을 소비한다. Background work scheduler가 batching window를 제공하는 이유가 여기에 있다.

Energy bug를 찾을 때 timer source, wakeup source, frequency transition, idle-state residency를 같은 trace에서 확인하면 `어떤 component가 deep idle을 막는가`를 찾을 수 있다.

---

## CHAPTER 27 · Power trace는 frequency 숫자보다 state transition timeline이 중요하다

Linux power tracepoint는 cpu_idle, cpu_frequency, frequency limit 같은 event를 기록할 수 있다. 이를 scheduler switch, IRQ, workqueue event와 함께 보면 workload가 어느 순간 CPU를 깨우고 performance state를 올리는지 추적할 수 있다. Android에서도 Perfetto와 PowerStats를 결합해 CPU activity와 energy event를 상관시킬 수 있다.

Average frequency만 보면 짧은 boost burst와 긴 low-frequency period가 같은 평균을 만들 수 있다. Root cause 분석에는 event timeline이 필요하다.

---

## CHAPTER 28 · Benchmark 반복성은 thermal·power state를 실험 protocol에 넣어야 확보된다

성능 회귀 gate는 code version만 같으면 재현된다고 가정하면 안 된다. 시작 battery temperature, ambient condition, governor/power mode, charger state, display state, network radio, background load를 기록하고 warm-up protocol을 고정해야 한다. 실험 사이 cooling time이 부족하면 뒤 test가 불리해진다.

Randomized test order나 separate-device comparison도 필요할 수 있다. Benchmark noise를 code noise로 오판하지 않도록 environment metadata를 artifact로 남겨야 한다.

---

## CHAPTER 29 · Energy optimization은 latency SLO와 함께 검증해야 한다

Battery consumption을 줄였지만 interaction latency가 SLO를 넘으면 제품 최적화라고 할 수 없다. 반대로 frame rate를 높였지만 device가 빠르게 thermal-throttle되어 장시간 UX가 악화되면 peak performance 개선일 뿐이다. Energy, latency, throughput, thermal severity를 동시에 acceptance criteria로 둬야 한다.

모바일 성능 변경은 `평균 frame time`, `sustained p95 frame time`, `energy per minute`, `thermal severity transition`처럼 여러 metric으로 gate를 구성할 수 있다.

---

## CHAPTER 30 · Power-performance 분석은 workload→state transition→heat→policy feedback을 닫힌 loop로 본다

전력 문제를 정확히 설명하려면 다음 causal loop를 유지한다.

```text
workload demand
→ scheduler placement / performance request
→ voltage-frequency-power state
→ execution time and energy
→ temperature rise
→ thermal/power limit
→ reduced available capacity
→ workload latency changes
```

이 loop에서 한 node만 최적화하면 다른 node가 보상하며 결과가 달라질 수 있다. 높은 frequency는 latency를 줄이지만 heat를 늘리고, thermal cap은 다시 frequency를 낮춘다. 깊은 idle은 energy를 줄이지만 wake latency를 늘린다. 최종 설계 기준은 단일 benchmark 최고점이 아니라 **목표 workload가 장시간 반복될 때 latency·energy·temperature·safety invariant를 동시에 만족하는가**다.
