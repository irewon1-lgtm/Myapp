# PART 43 · JIT and Deoptimization Runtime — tiering, profiling, OSR, code cache

JIT runtime은 실행 중 관찰한 hotness와 type profile을 이용해 더 빠른 machine code를 만들지만, 그 최적화는 “현재 assumption이 맞다”는 조건 위에 세워진다. assumption이 깨지면 deoptimization으로 안전한 state로 돌아가야 한다. 이 PART는 **profiling, speculative optimization, safepoint, code cache, recompilation과 운영 warmup**을 하나의 runtime 계약으로 다룬다.

---

## CHAPTER 01 · tiered execution은 startup과 peak performance를 서로 다른 code tier로 나눈다

처음부터 모든 method를 최고 수준으로 compile하면 startup CPU와 latency가 커진다. runtime은 interpreter 또는 baseline code로 빠르게 시작하고 hot method만 optimizing tier로 올리는 전략을 사용할 수 있다. 각 tier는 compilation cost와 execution speed가 다르다.

workload가 짧으면 optimization cost를 회수하기 전에 process가 끝날 수 있고, 장기 service는 peak tier가 중요하다. benchmark가 어느 tier를 측정하는지 모르면 cold-start와 steady-state 성능을 혼동한다.

method별 tier transition과 compile time을 trace한다. 성능 비교는 process age와 profile state를 결과에 포함한다.

---

## CHAPTER 02 · hotness는 execution frequency와 cost를 최적화 우선순위로 바꾼다

JIT는 invocation count, loop backedge, sampling 등으로 어떤 code가 자주 실행되는지 추정한다. hot threshold는 compilation budget을 어디에 쓸지 정하는 정책이다. 모든 method를 optimize하는 것이 항상 최선은 아니다.

burst workload는 잠깐 hot했다가 사라질 수 있고 background task가 threshold를 채워 interactive code보다 먼저 compile될 수 있다. profile pollution이 생기면 compile queue가 실제 SLO와 다른 곳에 자원을 쓴다.

hotness counter와 request class를 연결한다. threshold tuning은 compile CPU, code-cache size, latency를 함께 본다.

---

## CHAPTER 03 · type profile은 dynamic call의 실제 receiver distribution을 기록한다

managed dynamic dispatch는 runtime type에 따라 target이 달라지지만 production에서 한 call site가 대부분 하나의 type만 볼 수 있다. JIT는 receiver profile을 이용해 guard+direct call로 speculative devirtualization을 할 수 있다.

새 type이 나타나면 guard가 실패해 slow path나 deoptimization으로 가야 한다. benchmark가 한 type만 사용하면 production의 polymorphic cost를 과소평가한다.

call-site별 type histogram을 보존한다. plugin·feature rollout 뒤 profile entropy가 늘면서 regression이 생기는지 확인한다.

---

## CHAPTER 04 · profiling information은 optimizer IR에 추가 facts를 제공한다

execution count, branch probability, type feedback가 IR analysis에 들어가면 inlining, block layout, guard placement가 더 현실적인 cost model을 사용한다. profile은 semantics를 바꾸는 것이 아니라 어떤 legal optimization을 우선할지 돕는다.

stale profile이나 다른 workload의 profile을 적용하면 cold path를 과도하게 inline하거나 rare branch를 잘못 optimize할 수 있다. source와 profile version compatibility도 관리해야 한다.

profile provenance를 build/runtime artifact로 남긴다. optimization decision을 재현할 때 code revision뿐 아니라 feedback data가 필요하다.

---

## CHAPTER 05 · speculation guard는 aggressive optimization assumption을 runtime check로 보호한다

JIT는 “receiver가 이 type이다”, “array length가 충분하다” 같은 현재 관찰을 가정해 check와 dispatch를 제거할 수 있다. 대신 entry나 critical point에 guard를 두고 assumption이 틀리면 slow path/deopt로 돌아간다.

guard가 너무 많으면 branch와 metadata cost가 커지고, 너무 약하면 correctness가 깨진다. guard condition과 optimized code의 dependency를 정확히 연결해야 한다.

deoptimization log에서 어떤 guard가 자주 실패하는지 본다. frequent failure는 speculation이 workload와 맞지 않는 신호다.

---

## CHAPTER 06 · class hierarchy assumption은 dynamic loading 때문에 invalidation될 수 있다

현재 loaded class만 보면 virtual target이 하나라도 나중에 subclass가 load되면 assumption이 깨질 수 있다. runtime은 dependency를 추적해 관련 compiled code를 invalidate하거나 guard해야 한다.

class unloading과 redefinition, instrumentation도 hierarchy state를 바꿀 수 있다. code cache가 old metadata pointer를 계속 사용하면 crash가 생긴다.

class-load event와 invalidated method를 correlation한다. rollout 뒤 deopt spike가 새 plugin/class 때문인지 확인한다.

---

## CHAPTER 07 · OSR은 이미 실행 중인 hot loop를 optimized code로 옮긴다

On-Stack Replacement는 method entry로 다시 시작하지 않고 현재 loop iteration의 live state를 optimized frame representation으로 변환해 실행을 이어간다. long-running loop가 baseline tier에 갇히지 않게 한다.

local value와 monitor, exception state를 정확히 mapping해야 한다. OSR entry가 잘못되면 rare input에서 state mismatch가 생길 수 있다.

OSR compile/transition timestamp와 loop duration을 profile한다. benchmark warmup이 OSR 이후 steady-state만 측정하는지 명시한다.

---

## CHAPTER 08 · deoptimization은 optimized frame을 일반 runtime state로 재구성한다

speculative assumption이 깨지면 runtime은 register와 optimized stack slot에서 logical local/object state를 복원해 interpreter나 lower tier로 돌아갈 수 있어야 한다. 이를 위해 compiler는 deopt metadata와 virtual object materialization 정보를 남긴다.

scalar replacement로 실제 allocation되지 않았던 object를 deopt 시점에 materialize해야 할 수도 있다. metadata가 code version과 맞지 않으면 correctness가 무너진다.

deopt reason, code ID, reconstructed frame을 debug log에 연결한다. frequent deopt는 performance issue지만 deopt 자체는 correctness mechanism이다.

---

## CHAPTER 09 · uncommon trap은 rare path를 optimized fast path 밖으로 밀어낼 수 있다

거의 발생하지 않는 branch를 compiled code에서 작게 유지하고 실제로 발생하면 runtime slow path나 deoptimization으로 처리할 수 있다. common case code footprint가 줄어드는 이점이 있다.

운영에서 rare event가 빈번해지면 trap storm이 발생해 repeated deopt/recompile로 성능이 무너질 수 있다. input distribution 변화가 compiler bug처럼 보일 수 있다.

uncommon-trap rate와 request type을 함께 본다. 새로운 feature가 rare path를 normal path로 바꾸면 optimization policy를 재평가한다.

---

## CHAPTER 10 · safepoint는 runtime이 thread state를 일관되게 관찰할 수 있는 rendezvous다

GC, deoptimization, stack walking을 위해 runtime은 thread가 register/stack state를 설명 가능한 지점에 도달하도록 할 수 있다. safepoint poll과 stop protocol은 pause latency에 영향을 준다.

긴 native call이나 poll 없는 loop가 safepoint 도달을 지연시키면 모든 thread가 기다릴 수 있다. 너무 잦은 poll은 hot loop overhead가 된다.

safepoint request→all-thread-stop latency를 측정한다. longest-to-stop thread와 current code를 기록한다.

---

## CHAPTER 11 · stack map은 safepoint에서 reference와 value location을 설명한다

moving GC와 deoptimizer는 특정 PC에서 어느 register/stack slot이 object reference인지 알아야 한다. compiler가 stack map을 생성해 precise root scanning과 state reconstruction을 가능하게 한다.

wrong code version의 map을 사용하면 integer를 pointer로 보거나 live reference를 놓칠 수 있다. JIT code relocation과 reclamation이 metadata lifetime과 맞아야 한다.

code ID와 stack-map ID를 함께 검증한다. crash symbolization처럼 metadata mismatch를 first-class failure로 다룬다.

---

## CHAPTER 12 · liveness는 deopt와 GC가 보존해야 할 state 범위를 결정한다

optimized code는 source local 중 이후 사용되지 않는 값을 제거할 수 있다. 하지만 debugger나 deoptimization semantics가 특정 value를 요구하면 compiler가 materialization 정보를 유지해야 할 수 있다.

live range를 줄이면 register pressure가 낮아지지만 safepoint metadata와 object lifetime에도 영향을 준다. source scope와 machine liveness는 다르다.

optimization issue를 분석할 때 IR liveness와 deopt requirement를 함께 본다. “변수가 source에 있다”는 이유만으로 register에 존재한다고 가정하지 않는다.

---

## CHAPTER 13 · code cache는 generated machine code의 bounded executable memory다

JIT code는 executable memory에 저장되며 code cache가 제한되어 있으면 오래되거나 low-value code를 reclaim해야 한다. cache가 포화되면 새로운 optimization이 제한되고 performance가 steady-state에서 악화될 수 있다.

large inlining과 many specialized versions가 cache pressure를 높인다. W^X transition과 instruction-cache maintenance도 lifecycle에 포함된다.

used/free code-cache bytes와 compilation failure를 monitor한다. process uptime에 따라 cache pressure가 증가하는지 본다.

---

## CHAPTER 14 · code reclaim은 executing thread와 metadata lifetime을 안전하게 끝내야 한다

obsolete compiled code를 reclaim하려면 어떤 thread도 해당 instruction range를 실행하거나 return address로 보유하지 않는다는 보장이 필요하다. epoch/safepoint 같은 mechanism으로 quiescence를 확인할 수 있다.

code bytes만 free하고 inline cache·stack map이 남거나 반대 순서면 stale reference가 생긴다. code address 재사용은 crash analysis와 generation 관리도 어렵게 한다.

reclaim generation과 active frame count를 trace한다. stress에서 rapid compile/deopt/reclaim을 반복한다.

---

## CHAPTER 15 · compile queue는 optimization work 자체가 CPU resource를 소비한다

hot method가 많이 생기면 compiler thread queue가 쌓이고 application과 CPU를 경쟁할 수 있다. startup이나 traffic burst에서 compile work가 foreground latency를 악화시키는 경우가 있다.

compiler thread 수를 늘리면 warmup은 빨라져도 application capacity가 줄 수 있다. thermal/power 제한 device에서는 더 민감하다.

queue length, compile CPU, foreground latency를 함께 측정한다. background compilation policy를 workload SLO와 맞춘다.

---

## CHAPTER 16 · recompile thrash는 assumption이 반복해서 깨질 때 발생한다

method가 optimize→deopt→다시 optimize를 반복하면 compilation CPU와 code-cache churn이 useful execution을 압도할 수 있다. unstable type profile이나 phase-changing workload가 원인일 수 있다.

threshold를 올리거나 speculation을 덜 공격적으로 만드는 것이 유리할 수 있다. 단순 deopt 횟수 제한으로 correctness path를 막으면 안 된다.

method별 compilation generation과 deopt reason을 count한다. oscillation pattern을 자동 탐지한다.

---

## CHAPTER 17 · profile pollution은 training workload와 real workload가 다를 때 생긴다

startup/background task가 profile을 먼저 채우거나 test data가 production type distribution과 다르면 optimizer가 잘못된 path를 hot으로 판단할 수 있다. persisted profile을 다음 실행에 사용한다면 더 오래 영향을 준다.

profile reset 후 성능이 좋아지는 현상은 pollution 가설의 단서다. 하지만 warmup 손실도 함께 고려한다.

profile source와 age를 version으로 관리한다. deployment cohort별 hotness 차이를 비교한다.

---

## CHAPTER 18 · baseline tier는 compilation cost를 낮춰 빠른 startup을 제공한다

baseline compiler는 복잡한 global analysis보다 빠른 code generation을 선택해 interpreter보다 빠르면서 optimizing tier보다 저렴한 중간점을 제공할 수 있다. short-lived method에는 baseline만으로 충분할 수 있다.

baseline code가 너무 크면 code-cache를 소비하고, 너무 느리면 hot threshold 도달 전 user latency가 나빠진다. tier policy는 workload length에 맞춰야 한다.

startup first-use와 steady-state를 분리해 benchmark한다. tier별 code size도 기록한다.

---

## CHAPTER 19 · JIT inlining은 runtime profile을 이용해 dynamic call을 specialization한다

JIT는 actual receiver type과 call frequency를 알고 있어 hot monomorphic call을 공격적으로 inline할 수 있다. 이후 constant propagation과 escape analysis가 이어진다.

inlining body가 커지면 code-cache와 I-cache pressure가 증가하고 assumption invalidation 범위도 넓어진다. hotness가 바뀌면 old specialized code가 낭비가 된다.

inlining decision log와 code size를 비교한다. always-inline hint보다 runtime evidence를 우선한다.

---

## CHAPTER 20 · escape analysis는 runtime-observed context에서 allocation을 제거할 수 있다

object가 current method/thread 밖으로 escape하지 않는다고 증명하면 stack-like scalar state로 분해하거나 allocation을 제거할 수 있다. JIT는 inlined caller까지 보며 더 정밀한 proof를 할 수 있다.

reflection/native call/unknown virtual target은 escape proof를 막는다. deoptimization 시 virtual object를 materialize할 metadata가 필요하다.

allocation profile과 deopt materialization을 함께 본다. source `new` count로 heap pressure를 추정하지 않는다.

---

## CHAPTER 21 · check elimination은 proven invariant를 이용해 반복 검사를 줄인다

array bounds, null, type check가 loop invariant나 dominating proof로 이미 안전하다고 알 수 있으면 optimizer가 중복 검사를 제거할 수 있다. proof가 깨지는 path에는 guard가 남아야 한다.

unsafe annotation이나 incorrect range info가 있으면 memory safety를 위협할 수 있다. compiler bug를 줄이기 위해 verifier와 fuzzing이 중요하다.

optimized IR/disassembly에서 eliminated check를 확인한다. adversarial boundary input으로 semantics를 검증한다.

---

## CHAPTER 22 · code patching은 live executable state를 바꾸므로 publication protocol이 필요하다

inline cache target이나 entry stub을 runtime에 patch할 때 다른 thread가 동시에 실행할 수 있다. patch가 architecture atomic write보다 크면 intermediate byte sequence를 실행하지 않도록 safe point나 trap protocol이 필요하다.

instruction-cache coherence와 W^X permission transition도 포함된다. patch metadata와 code version이 분리되면 stale jump가 생긴다.

patch generation과 executing PC를 trace한다. high concurrency에서 repeatedly invalidate/patch하는 stress를 수행한다.

---

## CHAPTER 23 · W^X는 JIT가 writable code와 executable code phase를 분리하게 한다

JIT는 machine code 생성 동안 memory를 write해야 하지만 완료 후 executable permission을 부여한다. 동일 page를 지속적으로 W+X로 유지하면 memory corruption이 code injection으로 이어질 위험이 커진다.

dual mapping이나 batched permission transition을 사용해 performance와 security를 절충할 수 있다. mprotect와 TLB shootdown 비용도 고려한다.

code-cache mapping permission을 release gate에서 검사한다. 성능 문제를 이유로 W^X를 해제하지 않는다.

---

## CHAPTER 24 · profiler는 JIT hotness와 code generation 자체를 바꿀 수 있다

sampling·instrumentation profiler가 실행 frequency와 timing을 바꾸면 hot threshold, inlining profile, lock behavior가 달라질 수 있다. heavy tracing에서만 regression이 사라지는 현상은 observer effect일 수 있다.

JIT code symbolization도 generation마다 address가 바뀌므로 sample을 correct code ID와 매칭해야 한다.

low-overhead profile과 targeted instrumentation을 단계적으로 사용한다. profile mode를 benchmark artifact에 기록한다.

---

## CHAPTER 25 · warmup은 cache뿐 아니라 runtime code tier가 안정되는 과정이다

JIT workload의 warmup에는 class loading, profile 수집, compilation, code-cache fill이 포함된다. 몇 회 반복 후 latency가 낮아지는 것을 단순 CPU cache warming으로 해석하면 안 된다.

production service는 deploy 직후 traffic을 warmup 상태에서 처리해야 할 수 있다. benchmark가 안정된 peak tier만 보고 capacity를 정하면 rollout 순간 SLO가 깨진다.

iteration별 tier와 latency를 함께 그래프로 본다. cold-start, warmed, long-run 세 구간을 별도로 평가한다.

---

## CHAPTER 26 · cold peak는 startup compilation과 user traffic이 동시에 몰릴 때 나타난다

process restart 직후 class init, JIT compilation, cache miss, request processing이 같은 CPU/IO budget을 경쟁하면 transient peak가 생긴다. autoscaling이 동시에 많은 instance를 올리면 fleet 수준으로 확대된다.

pre-warm, profile-guided AOT, gradual traffic ramp가 완화책이 될 수 있다. warmup request가 real external side effect를 만들지 않게 한다.

rollout telemetry에서 process age별 latency와 compile CPU를 본다. average fleet metric에 cold instance가 숨지 않게 한다.

---

## CHAPTER 27 · AOT와 JIT hybrid는 startup과 specialization을 서로 다른 단계에 배분한다

ahead-of-time code는 startup에 바로 사용할 수 있고 JIT는 runtime profile에 맞춰 hot code를 더 specialize할 수 있다. profile-guided AOT는 두 접근을 연결한다.

AOT artifact가 runtime/ISA와 compatible해야 하고 JIT가 새 version으로 patch할 때 code identity가 달라진다. deployment cache invalidation과 signing도 고려한다.

tier별 실행 비율을 telemetry로 기록한다. runtime upgrade 후 AOT invalidation이 cold regression을 만드는지 확인한다.

---

## CHAPTER 28 · deopt storm은 rare correctness path가 sustained performance failure로 바뀐 상태다

많은 thread가 같은 assumption 실패로 동시에 deopt하면 interpreter/baseline fallback과 recompilation이 겹쳐 CPU와 latency가 급증한다. class loading이나 input phase change가 trigger일 수 있다.

재컴파일 전에 profile을 안정화하거나 speculation policy를 낮춰야 한다. deopt 자체를 비활성화하면 correctness가 깨질 수 있다.

deopt reason별 rate와 affected request를 실시간 monitor한다. threshold를 넘어가면 diagnostic snapshot을 자동 수집한다.

---

## CHAPTER 29 · runtime upgrade는 optimizer behavior와 metadata format까지 바꿀 수 있다

ART/JVM/runtime version이 바뀌면 tier threshold, GC interaction, JIT heuristic, generated code가 달라질 수 있다. application source가 같아도 startup과 peak performance가 바뀌는 이유다.

persisted profile이나 code cache가 새 runtime과 호환되지 않으면 재생성 비용이 발생한다. security fix가 code layout을 바꿔 performance counter도 달라질 수 있다.

runtime version을 release artifact와 benchmark result에 포함한다. upgrade canary에서 cold/steady/deopt metric을 모두 비교한다.

---

## CHAPTER 30 · JIT contract는 assumption, guard, deopt, code lifetime을 연결한다

speculative optimization은 어떤 profile fact를 가정했고 어떤 guard가 그것을 보호하며, 실패 시 어떻게 logical state를 복원하는지 명확해야 한다. stack map·code cache·class metadata의 generation도 동일 lifecycle에 속한다.

performance 운영에서는 warmup과 compile budget, deopt rate를 request SLO와 같이 본다. fastest steady-state code만 목표로 하면 startup과 phase change에서 실패한다.

최종 검증은 class loading·type 변화·code-cache pressure·deopt를 실제로 주입해 **assumption이 깨져도 semantics를 보존하고 runtime이 안정된 tier로 다시 수렴하는지** 확인하는 것이다.
