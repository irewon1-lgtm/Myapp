# PART 26 · Tracing Internals — perf events, ftrace, probes, eBPF, ring buffers

관측 도구는 시스템을 투명하게 보는 창이 아니다. 어디에 instrumentation을 삽입하는지, event를 어느 clock으로 timestamp하는지, buffer가 가득 찼을 때 무엇을 잃는지, sampling frequency가 어떤 bias를 만드는지에 따라 관측 결과가 달라진다. 수석 개발자는 flame graph와 trace UI를 보는 데서 끝나지 않고 **관측 데이터가 어떤 kernel path에서 생성·버퍼링·전달·symbolize됐는지** 설명할 수 있어야 한다.

---

## CHAPTER 01 · Instrumentation과 sampling은 데이터를 만드는 방식이 다르다

Instrumentation은 특정 event 발생 지점에서 명시적으로 record를 생성한다. Sampling은 timer/performance-counter overflow 같은 trigger마다 현재 execution state를 일부 관찰한다. Instrumentation은 event semantics가 명확하지만 hot path overhead가 커질 수 있고, sampling은 overhead를 제한하면서 statistical profile을 만들지만 짧은 event를 놓칠 수 있다.

`관측됐다`와 `발생했다`를 구분해야 한다. Sampling에서 보이지 않았다는 사실은 event가 없었다는 증명이 아니다.

---

## CHAPTER 02 · Static tracepoint는 kernel code가 유지하는 stable observation site다

Tracepoint는 kernel source의 특정 semantic event에 instrumentation hook을 둔다. Scheduler switch, block I/O, network event처럼 event meaning이 code address보다 중요할 때 유리하다. Function implementation이 바뀌어도 tracepoint semantic contract가 유지되면 tooling이 덜 깨진다.

반면 tracepoint field도 kernel version에 따라 변할 수 있으므로 decoder는 version/format metadata를 확인한다. Function name을 probe하는 것보다 안정적이라는 사실을 `영원히 ABI 고정`으로 확대하지 않는다.

---

## CHAPTER 03 · Dynamic probe는 source를 수정하지 않고 observation point를 추가한다

Kprobe는 kernel instruction/function 위치에 동적으로 probe를 걸 수 있고 uprobe는 userspace executable/library 위치에 probe를 둘 수 있다. Static tracepoint가 없는 내부 함수를 조사할 수 있지만 implementation detail에 결합된다.

Kernel update로 function이 inline되거나 이름·signature가 바뀌면 probe가 깨질 수 있다. Dynamic probe script는 target build ID/kernel version과 함께 관리한다.

---

## CHAPTER 04 · Entry와 return probe는 같은 function의 다른 state를 본다

Function entry에서는 argument를 관찰하기 쉽고 return probe에서는 return value와 elapsed time을 계산할 수 있다. 그러나 recursive function, tail call, exception/unwind, high concurrency에서는 entry↔return correlation이 단순 stack 하나로 해결되지 않을 수 있다.

Probe framework의 maxactive/missed counter와 context lifetime을 확인해야 한다. Missed return probe가 있으면 latency histogram이 biased될 수 있다.

---

## CHAPTER 05 · ftrace는 function instrumentation과 ring buffer를 결합한다

ftrace는 kernel function tracing, function graph, trace events, filters를 제공하는 tracing framework다. Function tracer를 전체 kernel에 무차별 활성화하면 event rate와 overhead가 급격히 증가할 수 있으므로 filter를 좁혀야 한다.

관측 범위는 hypothesis에서 역산한다. Scheduler delay를 조사한다면 관련 wake/switch path와 특정 process를 좁히지, 모든 function을 기록하지 않는다.

---

## CHAPTER 06 · Function graph는 call duration을 보여 주지만 wall-time 해석에 주의한다

Function graph tracer는 entry/exit를 연결해 call tree와 duration을 만들 수 있다. 그러나 function 안에서 preempt되거나 interrupt가 실행되면 elapsed duration에 다른 execution이 포함될 수 있다. `함수 자체 CPU cost`와 `함수 entry부터 exit까지 wall duration`은 다르다.

CPU-time을 알고 싶으면 scheduler/irq event와 결합해 off-CPU interval을 제거해야 한다.

---

## CHAPTER 07 · Trace buffer는 생산 속도가 소비 속도보다 빠르면 data를 잃을 수 있다

Kernel tracing은 per-CPU 또는 shared ring buffer에 event를 기록한다. Buffer가 finite하므로 producer가 너무 빠르면 old event overwrite, reservation failure, dropped-event counter가 발생할 수 있다. Trace에 gap이 있는데 `그 시간엔 아무 일도 없었다`고 해석하면 안 된다.

항상 lost/overrun counter와 buffer size를 evidence에 포함한다. Event filter를 좁히고 duration을 제한하는 것이 신뢰도를 높일 수 있다.

---

## CHAPTER 08 · Per-CPU buffer는 lock contention을 줄이지만 global ordering을 어렵게 한다

각 CPU가 local ring buffer에 기록하면 hot-path synchronization을 줄일 수 있다. 하지만 CPU별 timestamp stream을 합칠 때 clock synchronization과 merge ordering이 필요하다. 서로 다른 CPU에서 거의 동시에 발생한 event의 causal order가 timestamp precision보다 촘촘하면 단순 sort로 확정할 수 없다.

Causal ID나 task migration state를 이용해 ordering을 보강한다.

---

## CHAPTER 09 · BPF ring buffer는 cross-CPU ordering과 memory sharing을 다르게 설계한다

BPF ringbuf는 multi-producer single-consumer 구조로 하나의 shared ring을 사용할 수 있어 per-CPU buffer보다 memory를 유연하게 사용하고 sequential event ordering을 보존하기 쉽게 만든다. Reserve/commit API는 event payload를 ring 안에 직접 채우도록 해 copy를 줄일 수 있다.

Reservation failure는 nonblocking하게 발생할 수 있으므로 BPF program은 buffer full 때 event loss를 예상해야 한다. Consumer lag metric을 추적한다.

---

## CHAPTER 10 · eBPF verifier는 arbitrary kernel code execution을 막는 안전 경계다

BPF program은 kernel context에서 실행되므로 unchecked pointer access나 unbounded loop가 system safety를 위협할 수 있다. Verifier는 control flow, register type, pointer bounds, helper-call contract 등을 분석해 program을 허용하거나 거부한다.

Verifier acceptance는 business logic correctness의 증명이 아니다. 안전하게 실행 가능한 program이라는 뜻이며 event semantics와 filtering bug는 별도로 검증해야 한다.

---

## CHAPTER 11 · BPF map은 probe 간 state와 userspace 통신을 제공한다

Hash, array, per-CPU map, ringbuf 등 map type은 서로 다른 concurrency/memory semantics를 가진다. Per-CPU map은 update contention을 줄이지만 userspace에서 CPU별 값을 aggregate해야 한다. Global hash map은 shared state를 제공하지만 update cost와 memory limit이 있다.

Map 선택은 convenience가 아니라 access pattern과 aggregation requirement에 따른다.

---

## CHAPTER 12 · Attach point가 바뀌면 동일 BPF code도 관찰 의미가 달라진다

Kprobe에 붙은 program은 implementation function call을 관찰하고 tracepoint는 semantic event를 관찰하며 network hook은 packet path의 특정 stage를 본다. 동일 field 이름이라도 hook 시점이 다르면 아직 validation/NAT/routing 전후인지 의미가 다르다.

Observation point를 architecture diagram에 표시하고 upstream/downstream transformation을 명시한다.

---

## CHAPTER 13 · perf_event_open은 PMU와 software events를 unified event source로 노출한다

Linux perf event API는 hardware performance counter, software counter, tracepoint 등을 event fd로 다룰 수 있다. Event는 counting mode와 sampling mode로 사용할 수 있고 group을 통해 여러 counter를 동시에 enable/disable할 수 있다.

PMU counter 수가 제한돼 event가 multiplex되면 raw count를 그대로 비교할 수 없다. enabled/running time으로 scaling된 value와 multiplex ratio를 확인한다.

---

## CHAPTER 14 · Hardware counter는 microarchitectural event 정의를 확인해야 한다

Cycles, instructions, cache miss, branch miss 같은 이름은 CPU architecture와 PMU event definition에 따라 세부 의미가 다를 수 있다. Speculative event를 세는지 retired event를 세는지도 중요하다. 다른 CPU generation의 raw event code를 그대로 비교하지 않는다.

Derived metric IPC = instructions/cycles도 counter semantics와 frequency/idle 상태를 고려해야 한다.

---

## CHAPTER 15 · Sampling frequency가 너무 높으면 profiler가 workload를 바꾼다

높은 sample rate는 resolution을 높이지만 interrupt/NMI overhead, stack unwind cost, buffer traffic을 증가시킨다. Profiling overhead가 scheduler와 cache behavior를 바꾸면 Heisenberg effect가 발생한다.

Frequency를 단계적으로 올리며 result stability와 overhead를 같이 측정한다. Production profiling은 failure budget 안에서 설정한다.

---

## CHAPTER 16 · Period-based sampling과 frequency-based sampling은 의미가 다르다

Event period를 고정하면 특정 event N번마다 sample이 발생하고 event rate가 높을수록 sample frequency가 증가한다. Target frequency mode는 kernel이 period를 조절해 approximate sample rate를 유지할 수 있다.

Workload phase가 크게 변하는 benchmark에서는 sampling mode가 result distribution에 영향을 줄 수 있다. Reproducible profile은 exact event configuration을 기록한다.

---

## CHAPTER 17 · Stack unwinding은 profiler accuracy의 별도 subsystem이다

Sample에서 call stack을 복원하려면 frame pointer, DWARF CFI, unwind table, language runtime metadata 같은 정보가 필요하다. Optimizer가 inline/tail-call/frame-pointer omission을 적용하면 physical stack과 source-level call tree가 다르게 보일 수 있다.

`unknown` frame이 많을 때 hotspot이 없는 것이 아니라 symbol/unwind quality가 낮은 것일 수 있다. Debug symbol과 build ID를 보존한다.

---

## CHAPTER 18 · Inline frame과 JIT code는 symbolization pipeline을 복잡하게 만든다

Compiler inlining은 여러 source function을 한 machine-code region에 합친다. JIT runtime은 실행 중 code address를 생성·폐기할 수 있다. Profiler가 JIT symbol map이나 runtime event를 수집하지 않으면 address가 unknown으로 남거나 오래된 symbol에 잘못 매핑될 수 있다.

ART/JVM/V8 같은 runtime profiling은 native symbolizer만으로 충분하지 않다.

---

## CHAPTER 19 · Flame graph는 sample aggregation이고 timeline이 아니다

Flame graph의 가로 폭은 sample count 비율을 나타내고 시간의 좌→우 진행을 의미하지 않는다. 특정 순간의 causal ordering을 찾을 때 flame graph만 사용하면 안 된다. Timeline trace와 statistical profile은 서로 다른 질문에 답한다.

CPU hotspot은 flame graph, scheduler gap은 trace, memory retention은 heap graph처럼 evidence type을 문제에 맞춘다.

---

## CHAPTER 20 · Off-CPU profile은 waiting reason을 hotspot으로 만든다

CPU profile은 실행 중인 stack만 표본화하므로 lock wait, I/O sleep, scheduler wait가 긴 서비스는 병목이 사라져 보일 수 있다. Off-CPU profiling은 block 시작 stack과 wakeup/return을 연결해 기다린 시간을 call path에 귀속한다.

Latency = on-CPU + off-CPU라는 분해를 유지하면 `CPU가 낮은데 느림` 문제를 설명할 수 있다.

---

## CHAPTER 21 · Tracepoint field는 event schema로 다뤄야 한다

Tracefs는 event format과 field offset/type 정보를 제공한다. Binary decoder가 kernel struct layout을 임의 추정하지 않고 event schema를 따라야 한다. Kernel version 변경 시 field addition/removal를 tolerant하게 처리한다.

관측 pipeline도 schema evolution 문제를 가진다. Old collector가 new event를 읽을 수 있는지 compatibility test를 둔다.

---

## CHAPTER 22 · Clock source와 timestamp mode가 다르면 trace merge가 틀릴 수 있다

Kernel trace, userspace log, device trace가 서로 다른 clock domain을 쓰면 timestamp 숫자를 그대로 merge할 수 없다. PART 16의 monotonic/wall clock 구분을 관측 시스템에도 적용한다. Suspend 포함 여부와 CPU-local counter synchronization도 확인한다.

Cross-system trace는 clock offset/uncertainty를 metadata로 보존한다.

---

## CHAPTER 23 · Probe handler는 atomic/interrupt/NMI context constraint를 따라야 한다

Instrumentation callback이 어떤 context에서 실행되는지에 따라 sleep, allocation, lock 사용 가능성이 다르다. NMI-safe하지 않은 operation을 NMI context에서 수행하면 deadlock이나 crash를 만들 수 있다. Probe code는 application callback보다 훨씬 엄격한 constraint를 가진다.

관측 기능이 production failure를 만드는 것을 막기 위해 handler work를 최소화하고 event buffer로 넘긴다.

---

## CHAPTER 24 · Recursion protection은 tracer가 자기 자신을 다시 trace하는 문제를 막는다

Tracing handler 안에서 실행한 kernel function이 다시 같은 probe를 trigger하면 infinite recursion이나 corrupted state가 생길 수 있다. ftrace/kprobe/fprobe는 서로 다른 recursion protection mechanism과 context rule을 가진다.

Custom probe가 공통 handler를 공유할 때 framework별 recursion semantics를 확인한다. `callback은 짧으니까 안전`이라는 가정으로 충분하지 않다.

---

## CHAPTER 25 · Filtering은 overhead 최적화이면서 evidence definition이다

PID, CPU, function, event field로 filter를 적용하면 event volume을 줄일 수 있지만 관측 범위 밖 causal event를 잃을 수 있다. Filter가 너무 넓으면 dropped event가 늘고 너무 좁으면 원인 chain이 잘린다.

Hypothesis마다 필요한 boundary event를 먼저 나열한 뒤 filter를 설계한다. Filter configuration을 trace artifact와 같이 저장한다.

---

## CHAPTER 26 · Lost event counter는 trace 품질의 일부다

Buffer overflow나 probe missed counter가 0이 아닌 trace는 incomplete evidence다. Event loss가 random하지 않고 high-load 구간에 집중되면 바로 가장 중요한 incident 순간이 빠질 수 있다.

Report에는 total events뿐 아니라 lost count, buffer size, consumer lag, sample multiplex ratio를 포함한다. Trace가 깨졌다면 `관찰되지 않았다`를 사실로 보고하지 않는다.

---

## CHAPTER 27 · Correlation ID 없이 multi-layer trace를 합치면 우연한 시간 근접성을 causality로 오인한다

동시에 수천 request가 흐르는 시스템에서 timestamp가 가깝다는 이유만으로 kernel I/O와 application request를 연결하면 잘못된 causal chain을 만들 수 있다. Request ID, socket inode, fd/file identity, thread/task ID, block request tag처럼 layer를 연결할 key가 필요하다.

관측 설계 단계에서 correlation field가 어느 boundary에서 생성·전달·소멸하는지 정의한다.

---

## CHAPTER 28 · Instrumentation overhead 자체를 baseline으로 측정한다

Trace off/on 상태의 throughput, p99, CPU, cache miss를 비교해 observer effect를 정량화한다. Production trace가 5% latency를 추가한다면 incident 원인과 instrument overhead를 분리하기 어렵다.

Dynamic sampling, selective probe, shorter capture window를 사용해 overhead budget을 관리한다.

---

## CHAPTER 29 · Observability security는 kernel data 노출을 통제한다

Probe는 function argument, memory address, credential, packet payload 같은 민감정보를 읽을 수 있다. Root-capable tracing tool이 production secret을 export하지 않도록 capability, BPF privilege, data redaction, access control을 설계해야 한다.

관측 데이터 저장소 자체도 보안 boundary다. Debugging 편의 때문에 least privilege를 무시하지 않는다.

---

## CHAPTER 30 · Tracing evidence의 최종 계약은 coverage·ordering·loss·overhead·identity다

Trace를 증거로 인정하기 전에 다섯 질문을 통과해야 한다.

1. **Coverage** — 필요한 causal boundary가 instrument됐는가.
2. **Ordering** — clock과 cross-CPU merge로 event 순서를 신뢰할 수 있는가.
3. **Loss** — buffer overflow·missed probe·sampling omission을 알고 있는가.
4. **Overhead** — instrumentation이 workload를 의미 있게 바꾸지 않았는가.
5. **Identity** — event를 실제 request/task/resource에 연결할 correlation key가 있는가.

이 조건을 기록하지 않은 trace는 시각적으로 정교해도 완전한 증거가 아니다.
