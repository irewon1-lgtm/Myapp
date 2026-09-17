# PART 66 · Restartable Sequences and Per-CPU Fast Paths — CPU identity, abort, migration, local updates

Per-CPU data는 shared atomic contention을 줄이는 강력한 방법이지만 userspace thread는 scheduler에 의해 언제든 다른 CPU로 이동할 수 있다. Restartable Sequences(rseq)는 **현재 CPU identity와 짧은 per-CPU critical section을 kernel scheduler state와 연결**해, migration·preemption·signal이 일어나면 unsafe partial sequence가 commit되지 않도록 abort시킨다. 이 기능은 lock-free 만능 도구가 아니라 extremely short CPU-local update에 특화된 ABI다.

## CHAPTER 01 · Per-CPU data는 contention을 partitioning으로 없앤다

모든 threads가 하나의 global counter/cache를 atomic하게 갱신하면 cache line이 cores 사이를 이동한다. CPU별 shard를 두면 각 CPU가 local state를 갱신하고 가끔 aggregate할 수 있다. 이 구조는 coherence traffic을 줄이지만 CPU migration과 global aggregation이라는 새 문제를 만든다.

## CHAPTER 02 · Current CPU를 읽는 값은 scheduler migration과 race할 수 있다

Thread가 `나는 CPU 3에 있다`고 읽은 직후 scheduler가 CPU 7로 옮기면 CPU 3의 local data를 수정하는 것은 더 이상 CPU-local operation이 아니다. 단순 `getcpu` 결과를 cached pointer로 오래 보관하면 migration race가 생긴다. CPU identity와 update commit을 하나의 critical protocol로 묶어야 한다.

## CHAPTER 03 · rseq는 thread별 shared ABI area를 kernel에 등록한다

Userspace는 thread마다 rseq structure의 address를 kernel에 등록하고 kernel은 scheduling/migration/signal delivery 경계에서 필요한 fields와 critical-section state를 관리한다. 이 memory는 ordinary application variable가 아니라 kernel/userspace ABI다. Field ownership 규칙을 어기면 optimized mode에서 process termination 같은 강한 failure가 발생할 수 있다.

## CHAPTER 04 · cpu_id_start와 cpu_id는 critical sequence의 migration detection에 쓰인다

Critical section 시작 시 CPU identity를 읽고 per-CPU pointer를 계산한 뒤 commit 직전에 scheduler migration이 없었는지 protocol이 보장해야 한다. Rseq는 context-switch return path에서 registered critical section을 검사해 unsafe continuation을 abort할 수 있다. CPU ID 읽기만 빠른 것이 아니라 migration validation까지 포함된다.

## CHAPTER 05 · rseq_cs는 userspace code range와 abort target을 기술한다

Userspace는 현재 restartable critical section의 start/post-commit/abort 정보를 kernel이 해석할 수 있게 제공한다. Preemption이나 migration이 commit point 이전에 발생하면 return PC를 abort handler로 바꿔 partial local update를 버리고 다시 시도할 수 있다. Critical section metadata와 actual instruction layout이 일치해야 한다.

## CHAPTER 06 · Commit instruction 전의 side effect는 abort 후 안전해야 한다

Abort가 발생하면 userspace가 sequence를 다시 실행하므로 commit 전 외부-visible irreversible side effect가 있으면 중복 실행될 수 있다. Critical section은 보통 local scratch register와 final single-store commit 같은 구조로 설계한다. Syscall, I/O, allocation처럼 restart하기 어려운 operation을 넣으면 rseq model과 맞지 않는다.

## CHAPTER 07 · Post-commit point는 abort가 더 이상 허용되지 않는 경계다

Critical update를 실제 per-CPU state에 publish한 뒤에는 scheduler가 thread를 이동시키더라도 이미 완료된 operation을 abort해서 다시 실행하면 안 된다. 따라서 commit point와 post-commit IP를 정확히 정의해야 한다. Compiler가 instruction을 재배치하면 metadata와 실제 commit 경계가 어긋날 수 있다.

## CHAPTER 08 · Compiler barrier는 source 순서를 machine protocol에 고정하는 일부다

Rseq critical section은 instruction address와 commit ordering에 의존하므로 compiler가 load/store를 section 밖으로 옮기거나 merge하면 protocol이 깨질 수 있다. Inline assembly/compiler annotation/barrier를 사용해 code shape를 통제해야 한다. CPU memory barrier와 compiler barrier는 역할이 다르다.

## CHAPTER 09 · Signal delivery는 critical section을 abort시킬 수 있다

Signal handler가 same thread에서 실행되기 전에 kernel은 registered rseq critical section 상태를 확인한다. Handler가 per-CPU state를 만질 수도 있으므로 interrupted section을 그대로 이어가면 reentrancy와 migration assumptions가 깨질 수 있다. Signal boundary가 restart point인 이유다.

## CHAPTER 10 · Voluntary scheduling point도 CPU-local assumption을 끝낼 수 있다

Critical section 안에서 syscall/sleep을 하면 scheduler가 thread를 다른 CPU에서 재개할 수 있다. Rseq sequence는 extremely short nonblocking code를 전제로 한다. Blocking operation을 section에 넣는 설계는 time-slice extension이 있더라도 적합하지 않다.

## CHAPTER 11 · Rseq abort는 transaction rollback이 아니다

Kernel이 arbitrary memory write를 undo하는 것이 아니다. Userspace code가 commit 전 partial state를 외부에 publish하지 않도록 작성하기 때문에 abort handler가 다시 시작할 수 있는 것이다. 여러 addresses에 이미 write한 뒤 자동 rollback을 기대하면 안 된다.

## CHAPTER 12 · Per-CPU allocator fast path는 rseq의 대표적 사용처다

Thread가 current CPU의 freelist/cache에서 object를 pop/push할 때 global atomic을 피할 수 있다. Commit 전에 migration되면 abort하고 새 CPU cache에서 재시도한다. Global refill/drain은 별도 slow path와 locking을 사용할 수 있다. Fast path와 global consistency path를 분리하는 pattern이다.

## CHAPTER 13 · Per-CPU counters는 local update와 global read 정확도 사이를 교환한다

CPU-local counter를 rseq로 빠르게 증가시키면 hot-path atomic contention을 줄인다. 하지만 global sum은 여러 CPU shard를 읽어 합쳐야 하고 snapshot 동안 concurrent updates가 있을 수 있다. Exact linearizable total이 필요한지 approximate metric이면 되는지 먼저 정의해야 한다.

## CHAPTER 14 · CPU-local queue는 producer identity가 이동하면 ownership이 바뀐다

Thread가 current CPU queue에 enqueue할 때 migration되면 wrong queue ownership과 wake target이 생길 수 있다. Rseq commit으로 enqueue를 CPU-local하게 만들더라도 consumer/offline path가 queue lifetime을 관리해야 한다. Per-CPU 구조는 CPU hotplug와도 연결된다.

## CHAPTER 15 · Memory allocation failure path는 rseq section 밖으로 빠져야 한다

Local cache가 비었을 때 global allocator에서 refill하려면 lock, syscall, page fault가 필요할 수 있다. 이런 slow work를 restartable section 안에 넣지 말고 abort/branch 후 slow path에서 처리한 뒤 다시 fast path를 시도해야 한다. Rseq는 slow path를 없애는 기술이 아니다.

## CHAPTER 16 · CPU hotplug는 per-CPU data lifetime을 scheduler topology와 연결한다

CPU가 offline되면 해당 shard에 남은 objects/counters/queue를 다른 global state로 drain해야 할 수 있다. Userspace가 offline CPU ID의 data structure를 계속 참조하지 않도록 topology change를 감지해야 한다. Rseq가 migration race를 막아도 offline resource reclamation을 자동 해결하지는 않는다.

## CHAPTER 17 · Node ID fast access는 NUMA policy hot path를 줄일 수 있다

Rseq ABI가 current NUMA node ID를 제공하면 allocation/cache policy가 syscall 없이 locality를 선택할 수 있다. 그러나 memory tier/NUMA topology가 바뀌는 system에서는 node ID를 long-lived semantic identity로 저장하면 안 된다. 현재 scheduling location을 나타내는 hint로 사용해야 한다.

## CHAPTER 18 · mm_cid는 address-space-local concurrency identity로 활용될 수 있다

Modern rseq ABI는 memory map context에서 concurrency ID 같은 scheduler-provided identity를 노출할 수 있다. 이는 CPU ID보다 dense한 shard index를 제공해 per-thread/per-concurrency data structure footprint를 줄이는 데 활용될 수 있다. ABI feature availability를 probe해야 한다.

## CHAPTER 19 · Legacy rseq behavior와 optimized V2는 update cost가 다르다

Legacy mode는 context switch/signal boundary에서 fields와 critical-section check를 더 unconditional하게 수행하는 compatibility behavior를 가진다. Optimized V2는 실제 migration/state change가 있을 때 필요한 update만 수행해 kernel return-path overhead를 줄인다. Binary가 등록하는 structure size/feature contract가 mode 선택에 영향을 준다.

## CHAPTER 20 · Read-only ABI field를 application scratch로 쓰면 optimization contract를 깨뜨린다

과거 observed behavior에 의존해 kernel-owned field를 userspace가 덮어쓰는 code는 optimized mode의 stricter validation과 충돌한다. 최신 ABI는 read-only field를 수정하는 misuse를 강하게 검출할 수 있다. ABI memory layout을 `내 struct니까 내 것`으로 취급하면 안 된다.

## CHAPTER 21 · 여러 libraries가 한 thread의 rseq ABI를 공유할 수 있어야 한다

Allocator, tracing library, runtime이 모두 rseq를 사용하려 할 수 있다. 한 library가 registration/fields를 독점하거나 규약 밖으로 수정하면 다른 library를 깨뜨린다. Process-wide runtime은 registration ownership과 compatible ABI usage를 조정해야 한다.

## CHAPTER 22 · membarrier와 rseq는 cross-thread state transition에서 연결될 수 있다

Global operation이 모든 running threads의 rseq critical section이 abort/complete되었음을 확인해야 할 때 membarrier RSEQ command 같은 coordination을 사용할 수 있다. 이는 per-CPU fast path와 global reconfiguration 사이의 synchronization boundary다. Fast local update와 rare global barrier를 조합하는 design이다.

## CHAPTER 23 · Scheduler time-slice extension은 rseq critical section preemption risk를 줄일 수 있다

Optimized V2가 지원하는 환경에서 thread는 짧은 critical section 진입 시 time-slice extension을 요청할 수 있다. Reschedule point에서 kernel이 짧은 추가 실행 시간을 허용하면 shared resource를 잡은 채 deschedule되는 contention을 줄일 수 있다. 이것은 무제한 nonpreemptible userspace를 허용하는 기능이 아니다.

## CHAPTER 24 · Time-slice extension은 엄격한 최대 시간과 yield protocol을 가진다

Extension duration은 제한되며 userspace는 granted 상태에서 section을 끝낸 뒤 kernel에 CPU를 양보해야 할 수 있다. Grant 중 arbitrary syscall을 수행하면 grant가 revoke되고 scheduling이 발생할 수 있다. Critical section이 길거나 unpredictable하면 extension에 의존하면 안 된다.

## CHAPTER 25 · Rseq failure를 재현하려면 forced migration과 signals를 주입해야 한다

Normal benchmark에서 thread가 한 CPU에 오래 머물면 abort path가 거의 실행되지 않아 bug가 숨을 수 있다. Test는 affinity 변경, scheduler load, frequent signals, preemption을 사용해 commit 직전 migration을 강제해야 한다. Abort counter와 retry progress를 관찰해야 한다.

## CHAPTER 26 · Critical-section code layout은 LTO/inlining에도 보존돼야 한다

Compiler/linker optimization이 section label, branch, commit instruction 위치를 바꾸면 rseq metadata와 실제 code range가 어긋날 수 있다. Toolchain-supported rseq primitives/asm macros를 사용하고 disassembly/selftest로 generated layout을 검증해야 한다. Hand-written clever assembly는 maintenance risk가 크다.

## CHAPTER 27 · Sanitizer/instrumentation은 critical sequence 안에 hidden call을 넣을 수 있다

Address sanitizer, coverage, profiling instrumentation이 memory access 주변에 helper call이나 extra load/store를 삽입하면 rseq sequence assumptions가 깨질 수 있다. Critical code를 instrumentation 제외하거나 toolchain이 rseq-aware하게 처리하는지 확인해야 한다. Debug build가 release보다 더 쉽게 실패할 수도 있다.

## CHAPTER 28 · Per-CPU fast path는 memory footprint를 대가로 scalability를 얻는다

CPU마다 independent cache/counter/buffer를 복제하면 global contention은 줄지만 CPU 수에 비례해 memory 사용이 늘어난다. 수백 CPU system에서는 shard capacity × CPU count가 큰 hidden memory가 된다. Local cache size와 global reclaim/drain policy를 함께 설계해야 한다.

## CHAPTER 29 · Observability는 abort/retry와 slow-path 비율을 포함해야 한다

Rseq fast path success rate가 낮으면 code는 복잡한데 실제 성능은 slow path에 지배될 수 있다. CPU migration abort, signal abort, local cache miss, global refill, retry count를 측정해야 한다. Aggregate throughput만으로 abort storm이나 scheduler interaction을 알 수 없다.

## CHAPTER 30 · rseq는 CPU-local state machine을 scheduler와 합의하는 ABI다

올바른 사용은 **thread registration, CPU identity, code-range metadata, single commit, abort safety, compiler layout, migration/signal behavior, slow path separation, CPU hotplug, global barrier, instrumentation compatibility**를 모두 지켜야 한다. 핵심 이득은 atomics를 없애는 것이 아니라 CPU-local ownership이 유지된 짧은 구간에서 expensive global synchronization을 피하는 것이다.
