# PART 73 · PMU and perf_event Measurement — counters, sampling, multiplexing, skid, ring buffers, attribution

Hardware performance counter는 `CPU cycles 숫자`를 읽는 단순 register가 아니다. 실제 측정은 **어떤 PMU event를 어떤 execution context에 attach하고, counter slot 부족을 어떻게 multiplex하며, overflow sample의 skid를 어떻게 해석하고, lost records와 frequency scaling을 어떻게 보정할지**까지 포함한다. Counter value를 근거로 최적화하려면 event definition과 attribution error를 먼저 증명해야 한다.

## CHAPTER 01 · PMU event는 microarchitecture가 노출한 특정 hardware condition의 count다

Cycles, instructions, cache miss, branch miss처럼 portable-looking events도 정확한 hardware meaning은 CPU implementation에 따라 다를 수 있다. Cache reference가 어느 cache level을 뜻하는지, prefetch/coherence traffic을 포함하는지 event specification을 확인해야 한다. Event 이름만 같다고 서로 다른 CPU 세대의 값이 직접 비교 가능한 것은 아니다. Measurement report에는 CPU model, kernel/perf version, event encoding을 함께 보존해야 재현 가능하다.

## CHAPTER 02 · Architectural generic event와 raw event는 portability 수준이 다르다

Linux가 generic hardware events로 매핑하는 cycle/instruction 같은 항목은 여러 architectures에서 비슷한 목적을 갖지만 exact semantics는 여전히 다를 수 있다. Raw event는 PMU-specific config bits를 직접 지정하므로 더 세밀하지만 해당 CPU family를 벗어나면 의미가 없거나 다른 event가 된다. Benchmark automation은 event availability를 probe하고 unsupported event를 0으로 처리하지 말고 `미지원`으로 분리해야 한다.

## CHAPTER 03 · perf_event_open fd 하나는 하나의 measurement object다

`perf_event_open()`은 event configuration과 target task/CPU를 결합한 kernel object를 만들고 fd를 반환한다. 이후 enable/disable/read/mmap/ioctl은 이 measurement object를 조작한다. fd가 살아 있는 동안 event state와 count가 유지될 수 있으므로 profiling lifetime도 descriptor lifetime protocol을 따른다. Measurement setup/teardown이 workload warmup 구간과 겹치지 않도록 명시해야 한다.

## CHAPTER 04 · Per-thread와 per-CPU mode는 attribution 질문 자체가 다르다

특정 pid/task에 attach하면 그 task의 scheduling migration을 따라 count할 수 있고, cpu 지정 system-wide mode는 해당 CPU에서 실행된 여러 tasks의 events를 본다. `이 함수가 느리다`와 `이 CPU가 어떤 work로 포화됐나`는 다른 측정 mode가 필요하다. Per-thread만 보면 interrupt/kernel/background noise를 놓칠 수 있고 per-CPU만 보면 workload ownership이 흐려질 수 있다.

## CHAPTER 05 · exclude_user/kernel/hv는 count domain을 명시한다

User code만 측정할지 kernel execution도 포함할지 hypervisor time을 제외할지 attr flags로 정할 수 있다. Syscall-heavy workload에서 user cycles만 보고 instruction efficiency를 비교하면 kernel cost가 사라진다. 반대로 algorithm microbenchmark에서 kernel interrupts까지 포함하면 run-to-run noise가 커질 수 있다. Domain exclusion은 post-processing filter가 아니라 counter가 실제로 enable되는 privilege contexts를 결정하는 실험 설계다.

## CHAPTER 06 · Counting mode는 aggregate total을 제공하지만 시간 구조를 잃는다

Counter를 workload 전후에 read하면 전체 cycles/misses를 얻을 수 있으나 어느 phase에서 발생했는지는 알 수 없다. Average miss rate가 같아도 startup에 miss가 몰린 경우와 steady-state에 지속되는 경우의 해결책은 다르다. Aggregate counter는 hypothesis screening에 좋지만 phase/locality 원인에는 sampling·trace와 결합해야 한다.

## CHAPTER 07 · Sampling mode는 counter overflow를 observation event로 바꾼다

`sample_period=N`이면 event가 약 N회 발생할 때 overflow interrupt/sample record가 생성된다. Sampled IP/stack/register/data-source로 event 위치를 추정할 수 있지만 every event를 기록하는 것이 아니다. Sample frequency가 낮으면 rare hotspot을 놓치고 높으면 interrupt/record overhead가 workload를 바꾼다. Statistical profile은 sample-count uncertainty와 observer effect를 함께 가진다.

## CHAPTER 08 · sample_freq mode는 kernel이 period를 조절해 목표 sample rate를 추종한다

Workload event rate가 변할 때 fixed period는 sample rate를 크게 흔들 수 있다. Frequency mode는 timer tick 기반으로 period를 조절해 목표 samples/sec에 가깝게 유지한다. 그러나 control loop가 즉시 반응하는 것이 아니므로 phase transition에서는 일시적 oversampling/undersampling이 생길 수 있다. Exact event periodicity가 필요한 실험과 adaptive-rate profiling을 구분해야 한다.

## CHAPTER 09 · Hardware counter slot 수보다 event 수가 많으면 multiplexing이 일어난다

CPU에는 동시에 program 가능한 PMU counters가 제한되어 있다. 더 많은 events를 요청하면 kernel이 시간 분할로 event sets를 rotate할 수 있다. 이때 raw count는 event가 실제 CPU에서 running된 시간만 포함한다. 같은 wall-clock 동안 항상 측정된 event와 20%만 scheduled된 event를 raw count로 비교하면 틀린다.

## CHAPTER 10 · time_enabled/time_running scaling은 multiplexed count의 추정치다

Perf는 event가 enable된 총 시간과 실제 hardware에 scheduled된 시간을 제공해 `count * enabled/running` 같은 scaling을 가능하게 한다. 하지만 workload phase가 event scheduling window와 상관되면 uniform-rate assumption이 깨진다. 예를 들어 cache miss burst 구간에 event가 off-counter였다면 scaling은 실제 total을 복원하지 못한다. Multiplex ratio가 낮을수록 추정 불확실성을 명시해야 한다.

## CHAPTER 11 · Event group은 여러 counters를 동시에 schedule해 비율 해석을 안정화한다

Cycles와 instructions를 서로 다른 시간 window에 multiplex하면 IPC 계산이 phase mismatch 때문에 왜곡될 수 있다. Event group은 group members가 함께 on/off PMU되도록 요청해 numerator/denominator를 같은 execution interval에서 측정할 수 있게 한다. 하지만 hardware slot이 부족해 group 전체를 동시에 배치할 수 없으면 group이 아예 scheduled되지 않을 수도 있다. Group feasibility를 먼저 확인해야 한다.

## CHAPTER 12 · Pinned event는 반드시 counter에 상주하려는 강한 요구다

Group leader를 pinned로 설정하면 가능한 한 항상 hardware PMU에 유지하려고 한다. Slot/conflict 때문에 배치 불가능하면 error state가 될 수 있다. `pinned=더 정확`을 남발하면 다른 monitoring tool/NMI watchdog과 resource conflict를 일으킨다. Production observability는 PMU가 shared scarce resource라는 점을 고려해야 한다.

## CHAPTER 13 · Exclusive event는 다른 PMU users와 공존하지 못할 수 있다

특정 PMU 기능은 혼자 counter set을 사용해야 의미가 맞을 수 있다. Exclusive measurement는 system-wide perf session이나 kernel watchdog 때문에 실행되지 않을 수 있다. Lab에서는 성공하고 production에서 silently no data가 되는 위험이 있으므로 time_running과 open/enable error를 반드시 검증해야 한다.

## CHAPTER 14 · Sample ring buffer는 producer kernel과 consumer userspace의 bounded queue다

Sampling records는 mmap된 perf ring buffer에 기록되고 userspace reader가 head/tail protocol로 소비한다. Consumer가 느리면 buffer가 차고 samples가 drop된다. `perf file에 1M samples가 있다`가 workload event의 complete set을 뜻하지 않는다. Lost record/count를 함께 보고 sampling loss가 hotspot distribution을 편향시키는지 판단해야 한다.

## CHAPTER 15 · Lost samples는 uniform random loss가 아닐 수 있다

Buffer overflow는 event rate가 가장 높은 burst에서 발생하기 쉬워 hotspot phase를 선택적으로 잃을 수 있다. 그래서 `2% samples lost`가 단순 2% 오차가 아니다. Burst-heavy lock contention이나 interrupt storm을 정확히 보려면 larger buffer, lower sample rate, per-CPU consumption capacity를 조정해 loss를 줄여야 한다.

## CHAPTER 16 · Sampling skid는 event 발생 instruction과 recorded IP 사이의 거리다

PMU overflow가 발생한 순간 CPU가 즉시 정확한 instruction에서 멈추지 못해 몇 instructions 뒤 IP를 기록할 수 있다. Out-of-order execution과 interrupt delivery가 skid를 만든다. Hotspot을 source line 하나에 정확히 귀속하려면 event의 skid 특성을 알아야 한다. 일반 sampling에서 인접 instruction/function에 sample이 퍼지는 것을 code bug라고 오판하면 안 된다.

## CHAPTER 17 · precise_ip는 lower-skid hardware sampling capability를 요청한다

Supported CPU에서 PEBS류 precise mechanism을 사용하면 event에 가까운 retired instruction state를 capture할 수 있다. `precise_ip=2/3`는 더 정확한 IP를 요구하지만 모든 event/CPU가 지원하지 않는다. Unsupported precise request를 fallback 없이 무시하거나 generic event로 대체하면 정확한 실험이라는 전제가 무너진다. Exact-IP marker를 record에서 확인해야 한다.

## CHAPTER 18 · precise sample의 register state도 architectural snapshot 의미를 확인해야 한다

Precise mechanism이 captured registers를 제공해도 speculative microarchitectural state 전체를 보여주는 것은 아니다. 어떤 event는 retirement 기준, 어떤 data source는 load latency/physical address와 결합될 수 있다. Sample register를 `그 순간 모든 CPU 내부 상태`로 해석하지 말고 PMU manual이 보장하는 architectural fields로 제한해야 한다.

## CHAPTER 19 · Branch stack은 최근 control-flow history를 bounded buffer로 제공한다

LBR류 hardware는 sample 시점 이전의 recent branches를 기록해 misprediction/hot call path를 분석할 수 있다. Buffer depth가 제한되어 전체 stack/call graph가 아니며 branch filter와 privilege mode에 따라 content가 달라진다. Tail call/inlining과 exception control flow도 conventional call stack과 다르므로 branch trace와 unwind trace를 보완적으로 사용해야 한다.

## CHAPTER 20 · User stack sample은 raw bytes이고 unwind 품질은 별도 문제다

`PERF_SAMPLE_STACK_USER`는 userspace stack bytes를 record할 수 있지만 이를 caller frames로 바꾸려면 register context, unwind metadata, frame pointer 정책이 필요하다. P56의 CFI/artifact 문제가 그대로 적용된다. Stack sample을 수집했다는 사실과 정확한 callchain을 얻었다는 사실은 다르다.

## CHAPTER 21 · Data-source sample은 memory hierarchy 원인을 instruction에 연결한다

Supported PMU는 load가 L1/L2/LLC/remote memory 등 어디서 served되었는지, latency weight나 physical address를 sample에 제공할 수 있다. 단순 cache-miss count보다 `어떤 load가 어디까지 갔나`를 보여주지만 hardware-specific encoding과 sampling bias가 있다. NUMA remote access 진단에는 CPU/node mapping과 함께 봐야 한다.

## CHAPTER 22 · PMU event도 speculative execution과 retirement semantics를 구분해야 한다

일부 events는 speculatively issued work를 세고 다른 events는 retired operations만 센다. Branch misprediction처럼 나중에 squash된 work가 microarchitectural resource는 소비했지만 retired instructions에는 나타나지 않을 수 있다. IPC와 execution-port pressure를 함께 해석할 때 event semantics를 확인하지 않으면 `instructions는 적은데 cycles가 많음` 원인을 잘못 설명한다.

## CHAPTER 23 · Counter overflow interrupt 자체가 workload에 관측 가능한 영향을 준다

높은 sampling rate는 NMI/interrupt handling, ring-buffer writes, stack copying, branch-stack capture 같은 overhead를 추가한다. 특히 low-latency microbenchmark에서 profiler overhead가 측정 대상보다 클 수 있다. No-profiler baseline과 여러 sample-rate runs를 비교해 observer effect curve를 확인해야 한다.

## CHAPTER 24 · Frequency scaling에서 CPU cycles와 wall time은 같은 clock이 아니다

Core frequency가 DVFS/thermal 때문에 바뀌면 hardware cycle count와 elapsed nanoseconds relationship이 변한다. `cycles 감소=더 빠름`이 성립하려면 frequency state가 비교 가능해야 한다. Reference cycles, aperf/mperf류 capability, wall-clock latency와 CPUFreq trace를 함께 사용해 execution efficiency와 clock-speed 변화를 분리해야 한다.

## CHAPTER 25 · Instructions retired count도 interrupt·assist·architecture effect를 완전 제거하지 못한다

`instructions`가 source-level operation count가 아니다. Compiler optimization, vectorization, microcode assist, exception, interrupt context inclusion 설정에 따라 값이 달라진다. 동일 algorithm 비교에서는 binary/build와 privilege filters를 고정하고, 다른 ISA 사이에는 instruction count 자체를 performance-normalized metric으로 쓰지 않는 편이 낫다.

## CHAPTER 26 · Uncore PMU는 process ownership이 아니라 shared fabric activity를 본다

Memory controller, interconnect, PCIe 같은 uncore counters는 CPU core의 current task와 직접 1:1 attribution되지 않을 수 있다. System-wide bandwidth를 알려주지만 어떤 process가 원인인지 찾으려면 cgroup/task metrics, address samples, workload isolation을 함께 사용해야 한다. Shared-resource event를 per-process truth처럼 표시하면 false attribution이 생긴다.

## CHAPTER 27 · Guest/host virtualization은 PMU ownership과 event visibility를 분리한다

VM 내부 perf가 virtual PMU만 보거나 host가 counters를 multiplex할 수 있고, host profile에서 guest execution을 포함/제외할 수도 있다. Nested virtualization에서는 더 복잡하다. Performance regression이 guest code 때문인지 host steal/PMU scheduling 때문인지 판단하려면 exclude_guest/host와 vCPU scheduling evidence를 함께 확인해야 한다.

## CHAPTER 28 · perf_event 권한은 observability와 data leakage 사이의 security boundary다

Hardware samples는 kernel/user IP, register, address, callchain 등 sensitive execution context를 노출할 수 있다. `perf_event_paranoid`, CAP_PERFMON 등의 정책은 누가 어떤 scope의 PMU data를 볼 수 있는지 제한한다. Production profiler를 privileged daemon으로 운영한다면 collected profile artifact 자체도 secrets에 가까운 access control과 retention policy가 필요하다.

## CHAPTER 29 · PMU experiment는 event set과 scheduling quality를 함께 기록해야 재현된다

Report에는 event names/raw codes, grouping, pinned/exclusive, sample period/freq, precise_ip, exclude flags, CPU affinity, time_enabled/time_running, lost samples, kernel/CPU model, DVFS state를 포함해야 한다. 숫자만 저장하면 later regression에서 counter가 20%만 running됐는지 precise event가 fallback됐는지 알 수 없다. Measurement configuration은 result와 동급의 evidence artifact다.

## CHAPTER 30 · PMU 수치는 hardware truth가 아니라 제한된 counter에서 만든 측정 추정치다

신뢰할 수 있는 분석은 **event semantics, target scope, group simultaneity, multiplex scaling, sampling rate, skid/precision, ring-buffer loss, callchain quality, frequency state, uncore attribution, virtualization, permission**을 확인한 뒤에만 counter를 원인 증거로 사용한다. `cache miss가 많다`는 결론보다 먼저 그 miss event가 무엇을 세었고 언제 counter에 올라와 있었으며 어느 instruction/CPU/workload에 귀속 가능한지 증명해야 한다.
