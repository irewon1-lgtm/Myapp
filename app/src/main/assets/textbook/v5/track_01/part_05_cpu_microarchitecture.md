# PART 05 · CPU 실행 — ISA, speculation, cache, ordering

source-level 연산 수만으로 CPU 성능을 설명할 수 없다. 실제 실행은 instruction dependency, pipeline occupancy, branch prediction, cache miss, coherence traffic, register pressure, scheduler interference에 의해 결정된다. architecture-level semantics와 microarchitecture-level cost를 분리해 본다.

---

## CHAPTER 01 · ISA는 software와 CPU 사이의 architecture contract다

ISA는 instruction encoding, visible register, address calculation, privilege transition, exception semantics 같은 software-visible 규칙을 정의한다. 같은 ISA를 구현한 서로 다른 CPU는 pipeline 깊이, cache 크기, execution unit 수, branch predictor가 달라도 동일한 architectural result를 제공해야 한다.

따라서 `ARM이 빠르다`, `x86이 느리다` 같은 문장은 정보가 부족하다. 성능은 concrete microarchitecture와 compiler output, workload의 결합으로 결정된다. binary compatibility는 ISA만으로 끝나지 않고 ABI, OS syscall convention, object format이 함께 맞아야 한다.

---

## CHAPTER 02 · pipeline은 instruction latency를 없애지 않고 overlap한다

pipeline은 instruction 처리 단계를 분할해 서로 다른 instruction이 동시에 다른 stage에 존재하도록 한다. fetch/decode/rename/issue/execute/retire 같은 구체 stage 수와 이름은 CPU마다 다르지만 핵심은 **throughput을 높이기 위해 여러 instruction의 작업을 겹친다**는 것이다.

hazard가 생기면 pipeline의 일부가 기다리거나 잘못 가져온 instruction을 폐기해야 한다. data dependency, unavailable execution unit, instruction-cache miss, branch misprediction은 서로 다른 stall 원인이다. IPC가 낮다는 사실만으로 pipeline 깊이를 원인으로 단정하지 않고 stall breakdown과 cache/branch counter를 함께 본다.

---

## CHAPTER 03 · dependency graph가 instruction-level parallelism의 상한을 만든다

RAW(read-after-write) dependency는 후속 instruction이 선행 결과를 필요로 하는 진짜 data dependency다. WAR/WAW는 architectural register name 재사용 때문에 생기는 false dependency일 수 있고 register renaming이 이를 제거할 수 있다.

critical dependency chain이 길면 execution unit이 많아도 latency를 병렬화할 수 없다. 반대로 independent instruction이 충분하면 out-of-order core가 여러 operation을 동시에 issue할 수 있다. optimization에서 instruction count만 줄이는 것보다 dependency chain을 끊거나 memory latency와 계산을 overlap하는 것이 더 중요할 수 있다.

---

## CHAPTER 04 · branch prediction은 control dependency를 speculation으로 숨긴다

conditional branch의 실제 방향을 알기 전까지 fetch를 멈추면 pipeline throughput이 크게 떨어진다. modern CPU는 history 기반 predictor와 target predictor를 사용해 다음 instruction stream을 추측하고 speculative execution을 진행한다.

prediction이 맞으면 숨긴 latency가 이득이 되고 틀리면 speculative work를 폐기하고 correct path를 다시 채워야 한다. branch misprediction cost는 pipeline structure와 workload에 따라 다르다. `if문을 없애면 빠르다`가 아니라 branch predictability, alternative instruction cost, vectorization 가능성을 측정한다.

---

## CHAPTER 05 · out-of-order execution은 실행 순서를 바꿔도 retirement semantics를 보존한다

out-of-order core는 operand가 준비된 instruction을 program order와 다른 순서로 실행할 수 있다. 그러나 exception과 architectural state는 software가 기대하는 precise order를 보존해야 한다. reorder buffer 같은 구조가 completed result를 추적하고 retirement 시점에 architectural state를 commit한다.

speculative load나 execution이 architectural state에 commit되지 않아도 cache 같은 microarchitectural state를 바꿀 수 있다는 사실은 side-channel security와 연결된다. correctness 관점과 information-leak 관점의 observable state가 다를 수 있다는 점을 분리한다.

---

## CHAPTER 06 · superscalar core에서는 latency와 reciprocal throughput을 구분한다

한 instruction의 latency는 결과가 dependent consumer에 사용 가능해질 때까지의 지연이고, throughput은 independent instruction을 얼마나 자주 시작할 수 있는지와 관련된다. execution port와 functional unit 수가 제한되어 같은 종류 operation이 resource contention을 만들 수 있다.

instruction 하나가 싸더라도 같은 port에 집중되면 backend bound가 될 수 있고, instruction 수가 많아도 여러 unit에 분산되어 병렬 실행되면 wall time 영향이 작을 수 있다. assembly를 볼 때 opcode 개수보다 dependency, port pressure, load/store bandwidth를 함께 해석한다.

---

## CHAPTER 07 · register pressure는 spill을 통해 memory traffic으로 변환된다

compiler는 virtual value를 제한된 physical register에 배치한다. 동시에 live한 값이 많으면 일부를 stack slot로 spill하고 나중에 reload해야 한다. source에서 local variable이 하나 늘어난 변화가 register allocation 임계점을 넘으면 unexpected memory traffic을 만들 수 있다.

inline expansion과 loop unrolling은 branch/call overhead를 줄일 수 있지만 code size와 register pressure를 늘린다. optimization은 한 지표를 최대화하는 작업이 아니라 instruction cache, register file, execution resource 간 trade-off다.

---

## CHAPTER 08 · ABI는 함수 경계에서 register와 stack 책임을 나눈다

calling convention은 argument/return register, caller-saved/callee-saved register, stack alignment, varargs 규칙을 정의한다. 서로 다른 object module이 같은 ABI를 따라야 binary-level function call이 성립한다.

stack alignment가 깨지면 특정 SIMD instruction이나 library code에서 crash할 수 있다. FFI/JNI 경계에서 function signature만 맞추고 struct layout·calling convention·ownership을 무시하면 silent corruption이 생길 수 있다. binary boundary는 source type보다 ABI document가 최종 기준이다.

---

## CHAPTER 09 · recursion cost는 call depth와 frame state에 비례한다

재귀 함수는 각 active invocation에 필요한 state를 유지해야 한다. compiler가 tail-call optimization을 적용하지 않는다면 깊은 recursion은 stack consumption을 증가시키고 stack overflow 위험을 만든다. frame size가 큰 함수는 같은 call depth에서도 더 많은 stack을 사용한다.

recursion과 iteration의 성능 비교는 문법 형태로 결정되지 않는다. compiler optimization, branch behavior, locality, algorithmic structure를 본다. stack trace depth가 실제 source call count와 다를 수 있는 이유도 inlining·tail call·optimized unwind와 연결된다.

---

## CHAPTER 10 · SIMD는 같은 operation을 여러 lane에 적용하지만 dependency와 layout 제약을 받는다

vector instruction은 여러 element를 한 register에서 병렬 처리한다. compiler auto-vectorization은 loop-carried dependency, alias uncertainty, alignment, branch structure 때문에 제한될 수 있다. source loop가 단순해 보여도 pointer가 서로 overlap할 가능성을 배제할 수 없으면 안전한 vectorization을 포기할 수 있다.

vector width가 넓어졌다고 항상 linear speedup이 나오지 않는다. memory bandwidth, gather/scatter cost, tail handling, frequency throttling이 병목이 될 수 있다. vectorization report와 generated assembly, hardware counter로 실제 적용 여부를 확인한다.

---

## CHAPTER 11 · cache coherence는 core마다 가진 cache view를 일관되게 유지한다

multi-core system에서 같은 physical cache line이 여러 core cache에 존재할 수 있다. coherence protocol은 write ownership과 invalidation/update를 조정해 하나의 memory location에 대한 core 간 일관성을 유지한다. 구체 protocol state는 CPU family마다 다르지만 핵심 cost는 **shared writable line ownership이 core 사이를 이동할 때 traffic이 발생한다**는 점이다.

coherence는 language memory model의 synchronization을 대체하지 않는다. hardware가 cache line 값을 일관되게 유지해도 compiler/CPU ordering 규칙 때문에 unsynchronized program이 올바른 happens-before를 얻는 것은 아니다.

---

## CHAPTER 12 · false sharing은 독립 변수도 같은 cache line이면 경쟁하게 만든다

서로 다른 thread가 서로 다른 counter를 수정해 logical data sharing이 없더라도 두 값이 같은 cache line에 있으면 write ownership이 core 사이를 왕복할 수 있다. 이 현상이 false sharing이다.

진단은 lock contention이 없는데도 coherence-related counter와 CPU 사용량이 높고 scaling이 나빠지는 패턴에서 시작할 수 있다. 해결은 무조건 padding을 넣는 것이 아니다. data layout, per-thread aggregation, update frequency를 바꿔 shared write traffic 자체를 줄이는 것이 우선이다.

---

## CHAPTER 13 · memory ordering은 coherence와 별도의 관찰 순서 규칙이다

CPU는 store buffer, speculative load, out-of-order execution을 사용하면서 ISA가 허용하는 범위에서 memory operation 관찰 순서를 재배치할 수 있다. compiler도 language memory model 안에서 source order를 transformation할 수 있다.

atomic acquire/release와 fence는 필요한 ordering edge를 만든다. sequential consistency를 무조건 사용하면 reasoning은 단순해지지만 비용이 커질 수 있고, 지나치게 weak ordering은 correctness proof를 어렵게 만든다. lock-free algorithm의 memory order는 benchmark가 아니라 happens-before proof에서 선택한다.

---

## CHAPTER 14 · aliasing 정보는 compiler optimization 가능성을 결정한다

두 pointer가 같은 memory를 가리킬 가능성이 있으면 compiler는 한 store가 다른 load 결과를 바꿀 수 있다고 보수적으로 가정해야 한다. alias analysis가 independent memory임을 증명하면 load hoisting, vectorization, reordering 같은 최적화가 가능해진다.

반대로 programmer가 non-aliasing 계약을 잘못 주장하면 compiler는 실제 overlap을 고려하지 않고 최적화해 undefined behavior나 corruption을 만들 수 있다. performance hint는 correctness contract가 먼저 참일 때만 안전하다.

---

## CHAPTER 15 · interrupt와 exception은 current instruction stream을 privilege handler로 전환한다

interrupt는 device/timer 같은 asynchronous event와 연결되고 exception은 page fault, illegal instruction, divide fault처럼 current execution과 직접 연결될 수 있다. CPU는 architecture가 정의한 state를 저장하고 privilege handler로 control을 넘긴다.

interrupt rate가 매우 높으면 application instruction 실행 시간이 줄고 cache locality가 깨질 수 있다. 그러나 softirq/deferred work, driver polling, interrupt coalescing이 실제 비용을 분산할 수 있으므로 raw interrupt count만으로 원인을 결정하지 않는다.

---

## CHAPTER 16 · privilege level은 instruction과 memory access 가능 범위를 제한한다

user mode code는 privileged instruction과 kernel address space에 직접 접근할 수 없다. syscall/exception transition이 통제된 entry point를 제공한다. privilege boundary는 성능 overhead가 아니라 isolation의 핵심이다.

speculative execution vulnerability는 privilege check가 architectural commit을 막아도 microarchitectural trace가 남을 수 있다는 점을 악용했다. 방어는 software fence, predictor isolation, microcode/CPU design 등 여러 계층에 걸칠 수 있다. security mitigation의 성능 영향도 workload에 따라 측정해야 한다.

---

## CHAPTER 17 · PMU counter는 추정 대신 microarchitectural event를 관찰하게 한다

performance monitoring unit은 cycle, instruction, branch miss, cache/TLB event 같은 hardware event를 셀 수 있다. IPC, miss rate, cycles per event를 계산하면 CPU-bound 원인을 더 구체적으로 분해할 수 있다.

counter는 sampling multiplexing, privilege filter, event definition 차이의 영향을 받는다. 서로 다른 CPU model의 event 이름을 직접 비교하거나 derived metric 하나로 결론내리지 않는다. wall-clock, profile, trace와 counter를 상호 검증한다.

---

## CHAPTER 18 · microbenchmark는 측정 대상보다 benchmark harness를 재기 쉽다

짧은 function을 반복 호출하면 JIT warmup, dead-code elimination, constant folding, CPU frequency scaling, cache warm state, timer resolution이 결과를 왜곡할 수 있다. compiler가 결과를 사용하지 않는 계산을 제거하면 benchmark는 실제 operation 비용을 측정하지 않는다.

신뢰할 수 있는 microbenchmark는 warmup, blackhole/result consumption, iteration independence, statistical distribution, CPU environment를 통제한다. 최종 판단은 production trace와 연결한다. microbenchmark win이 end-to-end latency 개선을 보장하지 않는다.

---

## CHAPTER 19 · mobile CPU는 DVFS와 heterogeneous core 때문에 고정 성능 장치가 아니다

Android device는 workload, battery, thermal state에 따라 CPU frequency와 core selection을 바꿀 수 있다. big/LITTLE 계열 heterogeneous topology에서는 같은 thread도 시간에 따라 다른 performance class core에서 실행될 수 있다.

장시간 benchmark에서 처음 몇 초는 빠르고 이후 느려지는 현상은 algorithm 변화가 아니라 thermal throttling일 수 있다. device performance를 비교할 때 temperature, power mode, background activity, charging state를 기록한다. frame jank는 평균 CPU time보다 deadline miss distribution을 본다.

---

## CHAPTER 20 · CPU optimization은 bottleneck classification에서 시작한다

최적화 순서는 `코드를 짧게`가 아니다.

```text
wall-time regression을 재현한다
→ on-CPU 비중을 확인한다
→ frontend/backend/memory/branch stall을 분해한다
→ hotspot의 dependency와 access pattern을 본다
→ 한 가지 hypothesis를 변경한다
→ 동일 workload에서 counter와 latency를 재측정한다
```

CPU optimization이 I/O wait나 lock contention 문제를 해결하지는 않는다. microarchitecture 지식의 목적은 모든 코드를 assembly로 바꾸는 것이 아니라 **측정 결과가 어느 자원 제약을 가리키는지 정확히 해석하는 것**이다.