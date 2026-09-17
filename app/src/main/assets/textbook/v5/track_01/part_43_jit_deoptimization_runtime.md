# PART 43 · JIT and Deoptimization Runtime — tiering, profiling, OSR, code cache

JIT runtime은 source 또는 bytecode를 한 번 machine code로 바꾸고 끝나는 compiler가 아니다. 실행 중 관측한 type·branch·call-frequency 정보를 바탕으로 어떤 method를 어떤 수준으로 compile할지 결정하고, speculative assumption을 guard하며, assumption이 깨지면 optimized frame을 interpreter/baseline state로 복원해야 한다. JIT 성능은 peak machine code quality뿐 아니라 warmup, profiling overhead, compile queue, code-cache pressure, deoptimization 빈도까지 포함한 폐루프 시스템이다.

## CHAPTER 01 · tiered execution은 startup latency와 peak performance를 교환한다

모든 method를 시작 전에 최고 optimization level로 compile하면 startup이 길어지고 실제로 한 번도 실행되지 않는 code에 compile 자원을 낭비한다. Tiered runtime은 interpreter 또는 baseline compiler로 빠르게 시작하고 hotness가 확인된 method만 더 비싼 optimizing compiler로 올릴 수 있다. Tier 전환 정책은 method invocation count, loop backedge, profile confidence, compile budget에 의해 결정될 수 있다. Startup-sensitive mobile app과 장시간 server workload는 동일한 threshold가 최적이 아닐 수 있다. Benchmark는 cold, warmup, steady-state를 분리해 어떤 tier에서 측정했는지 기록한다.

## CHAPTER 02 · hotness counter는 compile candidate를 고르기 위한 runtime signal이다

Method call이나 loop backedge에 counter를 두면 어떤 code가 실제 workload에서 자주 실행되는지 알 수 있다. Counter update 자체도 overhead이므로 sampling, saturating counter, profile buffer 같은 방식으로 비용을 줄일 수 있다. Short-lived process에서는 hot threshold에 도달하기 전에 종료될 수 있고, burst traffic은 특정 path를 일시적으로 hot하게 만들 수 있다. `hot=중요`를 영구 truth로 두지 않고 profile age와 workload phase를 고려한다.

## CHAPTER 03 · profile에는 branch frequency뿐 아니라 type distribution이 들어갈 수 있다

Dynamic call site에서 receiver type이 거의 하나인지, 두세 개인지, 수십 개인지 기록하면 devirtualization과 inlining 결정을 개선할 수 있다. Branch taken ratio, array length distribution, null frequency 같은 profile도 optimization profitability를 바꾼다. Profile slot capacity가 제한되면 드문 type가 `other` bucket으로 합쳐질 수 있다. JIT가 보는 profile은 application의 전체 truth가 아니라 limited instrumentation에서 수집한 근사치이므로 confidence와 invalidation policy가 필요하다.

## CHAPTER 04 · optimizing compiler는 runtime profile을 static IR에 annotation한다

Bytecode/IR를 SSA-like representation으로 바꾸고 profile에서 얻은 branch weight·type feedback을 붙이면 inlining, block layout, range check 제거, speculative specialization을 실행할 수 있다. Profile-guided IR transformation은 P19의 ahead-of-time optimizer와 유사하지만 runtime class hierarchy와 현재 heap/type behavior를 추가로 이용한다. Compile time 자체가 user latency에 영향을 줄 수 있으므로 optimization pass budget도 제한된다. JIT optimizer는 code quality와 compile latency를 동시에 최적화한다.

## CHAPTER 05 · speculative optimization은 guard가 참인 동안만 빠른 path를 허용한다

Call site가 현재까지 `Dog` type만 봤다면 JIT는 `receiver is Dog` guard 뒤에 direct/inlined method를 만들 수 있다. 새로운 subclass instance가 들어오면 guard가 실패하고 generic dispatch 또는 deoptimization path로 가야 한다. Guard 없는 speculation은 correctness bug가 된다. 따라서 optimized code는 빠른 machine code 외에 어떤 assumption에 의존하는지 metadata로 보관해야 한다. Assumption 종류가 많을수록 invalidation graph와 deopt metadata가 커진다.

## CHAPTER 06 · class hierarchy assumption은 dynamic loading으로 깨질 수 있다

Compile 시점에 특정 method의 override가 하나뿐이어도 나중에 새로운 class가 load되면 devirtualization assumption이 더 이상 참이 아닐 수 있다. Runtime은 class loading 시 compiled code dependency를 확인하고 affected code를 invalidate하거나 guard를 사용한다. Plugin/reflection-heavy application은 hierarchy stability가 낮아 speculative optimization 수명이 짧을 수 있다. JIT regression을 볼 때 deoptimization count와 class-loading event를 같은 timeline에 놓는다.

## CHAPTER 07 · on-stack replacement는 이미 실행 중인 loop를 optimized code로 갈아탄다

Long-running loop가 interpreter/baseline tier에서 hot threshold에 도달했을 때 method가 return할 때까지 기다리면 optimization 이득을 늦게 얻는다. OSR은 loop의 현재 local/stack state를 optimized frame layout으로 변환하고 loop 중간 entry point로 전환한다. 이를 위해 compiler는 OSR entry와 state mapping을 생성해야 한다. OSR 자체도 transition cost가 있으므로 짧은 loop에는 부적합하다. Benchmark 초반의 sudden speedup은 OSR 시점을 반영할 수 있다.

## CHAPTER 08 · deoptimization은 optimized frame을 더 일반적인 execution state로 복원한다

Speculative guard failure나 invalidated dependency가 발생하면 runtime은 현재 machine register/stack에 흩어진 값으로 interpreter/baseline frame의 logical local/operand stack을 재구성해야 한다. Optimizer가 scalar replacement로 object를 실제 heap에 만들지 않았다면 deopt 시 materialization이 필요할 수 있다. Deoptimization metadata가 빠지면 optimized code에서 원래 program state를 복구할 수 없다. Deopt는 예외가 아니라 speculative JIT의 correctness mechanism이다.

## CHAPTER 09 · uncommon trap은 드문 path를 optimized code에서 제거하는 전략이다

Profile에서 거의 발생하지 않는 null/type/error path를 hot machine code에 모두 유지하면 instruction footprint와 branch가 늘어난다. JIT는 드문 조건에서 deopt하거나 slow stub로 빠지게 하고 common path를 직선화할 수 있다. 실제 production input이 바뀌어 그 `uncommon` path가 자주 발생하면 repeated deoptimization과 recompilation이 생겨 성능이 붕괴할 수 있다. Trap count가 workload drift signal이 될 수 있다.

## CHAPTER 10 · safepoint는 runtime이 thread state를 신뢰할 수 있는 위치를 정의한다

Moving GC, deoptimization, stack walking, debugger suspension을 위해 runtime은 thread의 managed reference와 register state를 정확히 알아야 한다. 모든 instruction에서 이를 보장하는 대신 compiler가 safepoint를 지정하고 stack map을 생성할 수 있다. Thread가 long-running native/non-safepoint code에 있으면 stop-the-world 요청 응답이 늦어질 수 있다. Safepoint latency는 GC pause 시작까지의 time-to-safepoint와 실제 GC work를 분리해 측정한다.

## CHAPTER 11 · stack map은 특정 PC에서 어느 register/stack slot이 reference인지 알려 준다

Optimized code는 local variable을 register, stack slot, constant, eliminated value로 자유롭게 배치한다. GC가 heap object를 이동하려면 현재 live reference 위치를 알아야 하고 deopt는 logical value를 복원해야 한다. Compiler는 safepoint/exception point별 stack map과 value location metadata를 만든다. 이 metadata는 code size와 compile time을 증가시키지만 runtime introspection correctness에 필수다.

## CHAPTER 12 · precise GC와 optimizer는 liveness information을 공유한다

Reference가 source scope 안에 있어도 optimizer가 더 이상 사용되지 않는다고 판단하면 GC root에서 제외할 수 있고, 반대로 native call까지 살아 있어야 하면 keep-alive barrier가 필요할 수 있다. Liveness가 잘못되면 object가 너무 오래 유지되어 retention이 생기거나 너무 일찍 collect되어 native pointer가 dangling될 수 있다. Language/runtime의 reachability fence/keep-alive API는 optimizer liveness와 resource lifetime의 경계를 보정한다.

## CHAPTER 13 · code cache는 JIT machine code를 위한 제한된 executable memory다

JIT output은 executable mapping에 저장되어야 하고 method metadata·unwind·stack map과 연결된다. Code cache가 무한히 커지면 memory footprint와 I-cache/ITLB pressure가 증가하므로 size budget과 eviction/reclamation 정책이 필요하다. Cache가 가득 차면 compile을 중단하거나 old/cold code를 버리고 재compile할 수 있다. Peak performance가 시간이 지나 떨어지는 현상이 code-cache pressure와 연결될 수 있다.

## CHAPTER 14 · compiled code reclamation은 active frame과 code pointer safety를 보장해야 한다

Cold compiled method를 code cache에서 제거하려 해도 어떤 thread가 현재 그 code 안에서 실행 중이거나 return address를 stack에 보유할 수 있다. Runtime은 safepoint, epoch, stack scan 등을 사용해 code가 더 이상 실행 reference를 갖지 않을 때 reclaim해야 한다. Inline cache나 function pointer가 old code address를 보관하는지도 invalidation해야 한다. Executable memory reclamation은 ordinary heap free보다 control-flow safety requirement가 강하다.

## CHAPTER 15 · JIT compilation queue도 overload될 수 있다

많은 method가 동시에 hot해지면 compiler thread queue가 길어져 optimal tier 전환이 늦어진다. Compiler가 CPU를 많이 쓰면 application worker와 core를 경쟁할 수도 있다. Compile thread priority, concurrency, background-only policy가 startup/throughput에 영향을 준다. Runtime metric에는 queued compile count, compile time, generated code bytes, failed/aborted compile를 포함해 `왜 hot code가 아직 baseline인가`를 설명한다.

## CHAPTER 16 · recompilation thrash는 profile이 불안정할 때 생긴다

Method가 type A에 맞춰 optimize→type B 등장으로 deopt→다시 optimize→또 다른 type으로 deopt를 반복하면 compile CPU와 code cache를 낭비한다. Runtime은 deopt history를 보고 더 generic한 optimization으로 후퇴하거나 recompilation threshold를 높일 수 있다. Megamorphic workload를 monomorphic benchmark로 학습시킨 profile을 사용하면 이런 thrash가 커질 수 있다. Stability가 peak specialization보다 중요할 때가 있다.

## CHAPTER 17 · profile pollution은 training workload가 production과 다를 때 발생한다

Startup/profile collection 과정에서 test/debug path가 과도하게 실행되면 JIT/AOT profile이 실제 user traffic과 다른 hotness를 기록한다. 잘못된 profile은 code layout, inlining, compilation priority를 왜곡한다. Profile-guided artifact는 수집 workload와 version을 추적하고 stale profile을 자동 승격하지 않는다. Android profile-guided compilation에서도 representative startup/navigation path가 중요하다.

## CHAPTER 18 · baseline compiler는 빠른 compile time과 충분한 execution speed를 목표로 한다

Interpreter보다 빠르면서 optimizing compiler보다 훨씬 짧은 compile latency를 가진 baseline tier는 warmup bridge 역할을 한다. Complex global analysis를 생략하고 direct bytecode-to-machine translation에 가까운 방식을 사용할 수 있다. Baseline code quality를 peak code와 비교해 낮다고 실패로 보지 않는다. Runtime objective는 process lifetime 전체 CPU/time/energy를 최소화하는 것이지 모든 method를 최고 tier로 만드는 것이 아니다.

## CHAPTER 19 · JIT inlining은 dynamic type profile과 code-size budget을 동시에 사용한다

Hot call site를 inline하면 dispatch와 call overhead를 없애고 caller context에서 constant/type propagation이 가능하다. 하지만 large callee를 여러 caller에 복제하면 code cache와 I-cache가 커진다. Runtime은 call frequency, callee size, recursion, polymorphism을 조합해 threshold를 정한다. P39 instruction footprint와 JIT inlining decision을 함께 측정하면 `더 많은 inline=더 빠름`이라는 단순 규칙을 피할 수 있다.

## CHAPTER 20 · escape analysis는 allocation을 stack/scalar state로 제거할 수 있다

새 object가 method/thread 밖으로 escape하지 않는다고 증명되면 JIT가 heap allocation을 제거하고 field를 scalar local로 분해할 수 있다. 이때 heap profiler에 source-level object가 나타나지 않을 수 있다. Deopt가 발생하면 logical object state를 materialize할 metadata가 필요하다. Object allocation benchmark는 optimization을 막는 black-box 사용과 실제 production optimized path를 구분한다.

## CHAPTER 21 · range/null check elimination은 guard dominance와 deopt에 의존한다

Loop 전에 array bounds와 null condition을 증명하면 반복 내부 check를 제거할 수 있다. Dynamic assumption이 필요한 경우 guard failure 시 slow path/deopt로 보낼 수 있다. Check 제거는 safety contract 삭제가 아니라 check를 더 적은 위치에 집중시키는 transformation이다. 잘못된 unsafe annotation으로 compiler proof를 속이는 것과 runtime guard 기반 최적화를 구분한다.

## CHAPTER 22 · code patching은 inline cache와 entry point를 실행 중 갱신한다

Call site cache가 새로운 target을 학습하거나 method가 higher tier로 compile되면 기존 caller entry를 새 code로 연결해야 한다. Runtime은 atomic patch, indirection cell, safepoint patching 같은 방식으로 thread가 partial instruction을 보지 않게 해야 한다. Patch 후 instruction cache synchronization이 필요한 architecture도 있다. JIT performance mechanism이 동시에 concurrency와 memory-protection mechanism이다.

## CHAPTER 23 · W^X는 JIT code generation의 permission transition을 제한한다

Security hardening은 memory page가 동시에 writable/executable인 상태를 최소화하거나 금지한다. JIT는 writable buffer에 code를 생성한 뒤 executable로 전환하거나 dual-mapping/OS API를 사용할 수 있다. Permission transition과 I-cache synchronization이 compile latency에 포함된다. JIT 최적화를 위해 W^X를 무력화하는 것은 performance solution이 아니라 attack surface 증가다.

## CHAPTER 24 · JIT와 profiler가 서로 간섭할 수 있다

Instrumentation profiler가 method entry에 probe를 넣거나 sampling signal이 자주 발생하면 hotness counter, inlining, code cache layout이 원래 workload와 달라질 수 있다. Debug build와 production optimized build의 JIT tier behavior도 다를 수 있다. `프로파일링했을 때만 느림/빠름`은 Heisenberg effect 후보다. Low-overhead sampling과 runtime-native compilation metric을 조합한다.

## CHAPTER 25 · warmup benchmark는 steady state를 자동 보장하지 않는다

N회 반복 후 측정한다는 규칙만으로 JIT tier·GC·code cache가 안정됐다고 보장할 수 없다. Compile event와 deopt event가 계속 발생하는지, frequency/thermal state가 안정됐는지, allocation/GC cycle이 representative한지 확인해야 한다. Measurement window 안에서 slope가 남아 있으면 결과가 warmup 과정 일부다. Iteration count가 아니라 runtime state evidence로 steady state를 판정한다.

## CHAPTER 26 · cold-start 성능과 peak JIT 성능은 반대 방향 trade-off를 가질 수 있다

Aggressive profile instrumentation과 early compile은 peak 도달을 앞당길 수 있지만 startup CPU/energy를 늘린다. AOT/profile-guided precompile을 사용하면 startup을 개선하지만 binary/code-cache footprint가 증가할 수 있다. Mobile app은 first-frame latency와 battery, server는 long-run throughput을 더 중시할 수 있다. Runtime policy를 workload lifetime에 맞춘다.

## CHAPTER 27 · AOT와 JIT는 상호 배타적이지 않다

Runtime은 baseline AOT code로 시작하고 execution profile에 따라 hot method를 JIT recompile할 수 있다. 반대로 profile을 다음 install/build의 AOT input으로 사용해 JIT warmup을 줄일 수 있다. `AOT 언어`, `JIT 언어`라는 이분법보다 어떤 code가 어느 시점에 어떤 tier로 실행되는지 확인한다. Android ART의 profile-guided compilation이 이런 hybrid model의 예다.

## CHAPTER 28 · deoptimization storm은 latency spike의 독립 failure mode다

Config/class loading/input shape 변화 직후 많은 optimized method assumption이 동시에 깨지면 deopt, recompilation, code-cache mutation이 몰릴 수 있다. CPU utilization과 p99 latency가 함께 튀지만 application business code profile만 보면 원인이 보이지 않을 수 있다. Deopt reason histogram, compile queue, class-load event, code-cache stats를 incident timeline에 넣는다.

## CHAPTER 29 · runtime upgrade는 optimization policy와 generated code를 바꾼다

Same source와 same app version이어도 ART/JVM/runtime update가 tier threshold, compiler pass, GC barrier, code-cache policy를 바꾸면 performance가 달라질 수 있다. Regression baseline에는 OS/runtime version을 포함한다. 특정 generated assembly에 의존한 micro-optimization을 public invariant로 만들지 않는다. Runtime upgrade 테스트는 correctness뿐 아니라 cold/warm/steady-state performance matrix를 다시 측정한다.

## CHAPTER 30 · JIT runtime의 품질은 peak speed가 아니라 lifetime cost와 deopt correctness로 판정한다

검증 가능한 JIT 시스템은 interpreter/baseline/optimized tier별 실행 시간, compile CPU, generated code bytes, deopt reason, safepoint latency, code-cache occupancy를 기록한다. Speculative optimization마다 guard와 recoverable state mapping이 있어야 하고 assumption invalidation을 stress test한다. Benchmark는 cold start·warmup·steady state를 분리하며 representative type/profile distribution을 사용한다. 최고 tier의 한 microbenchmark 점수보다 process lifetime 전체 latency·energy·memory가 최종 기준이다.