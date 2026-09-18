# PART 51 · Resource Limits and Accounting — rlimit, cgroups, quotas, pressure, admission

리소스 제한은 “얼마나 많이 쓸 수 있는가”를 정하는 숫자 모음이 아니다. Process rlimit, system-wide table, cgroup memory·CPU·I/O·PID controller, kernel memory, socket buffer, application queue가 서로 다른 계층에서 같은 workload를 제약한다. 한 계층의 limit만 올리면 병목이 다른 곳으로 이동할 수 있다. 이 PART는 **resource budget → accounting → pressure signal → admission/degradation**을 하나의 control loop로 다룬다.

---

## CHAPTER 01 · resource budget은 최대값이 아니라 정상 운영 범위를 먼저 정의한다

시스템이 사용할 수 있는 memory, CPU, file descriptor, thread, I/O bandwidth에는 모두 상한이 있다. 하지만 설계에서 중요한 것은 limit 직전까지 쓰는 것이 아니라 burst와 장애 복구를 위한 headroom을 남기는 것이다. 정상 steady state, 예상 peak, hard limit을 세 구간으로 나누면 alert와 admission policy를 설계하기 쉽다.

Budget은 resource별로 독립적이지 않다. Connection 하나가 fd, socket memory, thread/task, application buffer를 동시에 소비할 수 있다. 따라서 “최대 10만 connection”은 각 resource의 per-connection cost를 곱해 전체 budget과 맞는지 확인해야 한다.

Capacity 문서에는 단순 maximum보다 unit cost를 적는다. `bytes/request`, `fds/connection`, `cpu-ms/job`처럼 소비 모델이 있어야 workload 증가를 다른 machine size에 적용할 수 있다.

## CHAPTER 02 · rlimit은 process와 credential context에 적용되는 전통적인 userspace 경계다

Resource limit은 process가 열 수 있는 fd, 생성할 core dump, 사용할 stack 등 여러 항목의 soft/hard ceiling을 제공한다. Soft limit은 process가 낮추거나 허용 범위에서 조정할 수 있지만 hard limit 변경에는 더 높은 권한이 필요할 수 있다. Inheritance 때문에 parent launcher의 설정이 child service에 그대로 전달되기도 한다.

문제는 application config와 실제 kernel limit이 다를 수 있다는 점이다. 서비스가 “max_connections=50000”이어도 nofile soft limit이 1024면 먼저 실패한다. Startup에서 effective limit을 읽어 configuration과 모순을 검증하는 것이 좋다.

Container 환경에서는 rlimit과 cgroup limit이 동시에 존재한다. 하나가 넉넉하다고 다른 제약이 사라지는 것은 아니다.

## CHAPTER 03 · RLIMIT_NOFILE은 fd 숫자 범위와 동시에 open resource 수를 제한한다

Socket, regular file, pipe, eventfd 등은 모두 file descriptor table을 사용한다. fd limit에 도달하면 신규 connection accept나 log file open이 실패해 unrelated 기능이 동시에 영향을 받을 수 있다. Leak이 있으면 평균 traffic이 같아도 시간이 지날수록 limit에 접근한다.

단순 limit 상향은 임시 완화일 뿐이다. `open fd count / request rate`와 lifetime 분포를 관찰해 leak인지 정상 concurrency 증가인지 구분한다. Keep-alive와 connection pool 설정도 fd budget에 직접 영향을 준다.

운영에서는 현재 사용량과 hard limit의 비율을 alert로 두되, failure path가 로그 fd를 새로 요구하지 않게 한다. Emergency diagnostic에 사용할 reserved descriptor 전략도 고려할 수 있다.

## CHAPTER 04 · process fd table과 system-wide file table은 다른 병목이다

각 process가 nofile limit 아래여도 kernel 전체의 open file structure나 inode/dentry cache가 압박받을 수 있다. 여러 container가 같은 host에서 각각 합리적인 수의 fd를 열면 system-wide limit에 도달할 수 있다. Tenant 단위 제한과 host capacity를 같이 봐야 한다.

Host-wide exhaustion은 한 service의 문제처럼 보이지 않을 수 있다. 여러 process에서 동시에 `EMFILE` 또는 `ENFILE` 계열 오류가 나타나는지 구분하고 system metric을 수집한다.

Capacity 계획에서는 process당 최대 fd를 모두 더한 값이 host가 실제 감당 가능한지 계산한다. 모든 tenant가 동시에 hard max까지 쓰는 worst case를 허용할 것인지 admission 정책으로 제한할 것인지 결정한다.

## CHAPTER 05 · process와 thread 수 제한은 scheduler와 memory budget을 동시에 보호한다

새 process/thread는 task structure, stack, TLS, scheduler queue를 소비한다. PID/task limit이 없으면 fork bomb이나 runaway thread creation이 host 전체 availability를 해칠 수 있다. Per-user limit과 cgroup pids controller는 서로 다른 범위에서 이 폭발을 막는다.

Thread pool을 자동 확장할 때 task limit에 가까워지면 단순 create retry를 반복하지 않는다. Queueing과 backpressure로 전환하거나 work를 reject해야 한다. 실패 후 half-created runtime state가 남지 않는지도 확인한다.

Thread count alert는 absolute 값뿐 아니라 증가 slope를 본다. Slow leak은 hard limit까지 시간이 오래 걸려 순간 threshold만으로 놓치기 쉽다.

## CHAPTER 06 · virtual address limit과 RSS는 같은 memory 사용을 설명하지 않는다

큰 address space를 reserve해도 실제 physical page가 모두 resident한 것은 아니다. Memory-mapped file, sparse arena, guard region 때문에 virtual size는 클 수 있지만 RSS는 작을 수 있다. 반대로 shared page와 reclaimable cache 때문에 RSS만으로 실제 pressure를 완전히 설명하지 못한다.

Resource limit이 address space에 적용되는지 resident memory에 적용되는지 확인해야 한다. 64-bit process의 큰 reservation을 무조건 leak으로 판단하면 allocator 설계를 오해할 수 있다.

Incident에서는 VSS/RSS/PSS, anonymous/file-backed 분류를 함께 본다. “memory 10GB”라는 한 숫자 대신 어떤 종류의 page가 실제로 reclaim 가능한지까지 추적한다.

## CHAPTER 07 · stack limit은 recursion depth와 per-thread reservation에 영향을 준다

각 thread stack은 finite resource다. Recursive algorithm, 큰 local array, deep callback chain은 stack overflow를 만들 수 있다. Stack limit을 무작정 키우면 thread 수가 많은 서비스에서 address-space와 committed memory 부담이 늘 수 있다.

Worker thread는 main thread와 다른 default stack size를 가질 수 있으므로 platform/runtime 설정을 확인한다. Guard page가 overflow를 조기에 잡는지도 중요하다.

테스트는 정상 깊이뿐 아니라 adversarial nesting을 포함한다. Parser의 input nesting depth를 제한하면 stack limit에 의존한 crash를 application-level error로 바꿀 수 있다.

## CHAPTER 08 · locked memory는 reclaim되지 않는다는 점 때문에 강한 budget이 필요하다

`mlock` 계열로 page를 memory에 고정하면 swap/reclaim에서 제외되어 latency와 secret handling에는 유리할 수 있지만 system memory flexibility를 줄인다. 많은 process가 무제한으로 lock하면 reclaim 가능한 page가 부족해진다.

그래서 locked-memory limit은 privilege와 함께 관리된다. Crypto key처럼 작은 민감 데이터와 대규모 cache를 같은 정책으로 다루지 않는다.

Lock 실패 시 application이 insecure fallback을 할지, 기능을 중단할지 목적에 따라 정한다. Security-sensitive buffer라면 “잠금 실패했지만 계속 실행”이 threat model에 맞는지 명시한다.

## CHAPTER 09 · core dump limit은 진단 가능성과 secret 노출 위험을 동시에 조절한다

Crash dump는 stack과 heap을 포함해 강력한 debugging 자료를 제공하지만 credential, key, personal data도 포함할 수 있다. Core size limit과 dumpable policy는 단순 disk 절약 설정이 아니라 security control이다.

Production에서는 dump 저장 위치, 접근 권한, retention을 제한하고 필요하면 sensitive process의 dump를 비활성화한다. 반대로 아무 dump도 없으면 rare crash 원인 분석이 불가능해질 수 있으므로 redaction 가능한 crash reporter를 고려한다.

설정 검증은 실제 crash를 일으켜 dump 생성 여부와 permission을 확인한다. Config file 값만 보고 PASS라고 하지 않는다.

## CHAPTER 10 · cgroup hierarchy는 resource control을 tree 형태의 ownership으로 만든다

Cgroup v2에서는 process를 hierarchy에 배치하고 memory, CPU, I/O 같은 controller를 subtree 단위로 적용한다. Parent의 제약은 child가 더 넓은 resource를 얻지 못하게 하므로 effective capacity는 여러 ancestor 설정의 결과다.

Service가 자기 cgroup 파일만 읽고 parent limit을 무시하면 실제 usable budget을 과대평가할 수 있다. Container runtime, orchestrator, system manager가 어느 level에 값을 쓰는지 확인한다.

Hierarchy 설계는 조직 구조와도 연결된다. Host→tenant→service처럼 책임 범위를 나누면 usage accounting과 admission이 명확해진다.

## CHAPTER 11 · memory.max는 cgroup이 넘어설 수 없는 hard ceiling이다

Memory hard limit에 도달하면 reclaim을 시도하고 progress가 없으면 cgroup 내부에서 OOM 처리가 발생할 수 있다. Host 전체에 여유 memory가 있어도 해당 cgroup은 더 사용할 수 없다. 그래서 container 안의 OOM을 host free memory만 보고 설명할 수 없다.

Limit은 workload의 peak live set과 kernel/socket overhead까지 고려해야 한다. 평균 RSS만 기준으로 잡으면 burst에서 반복 OOM이 발생한다.

OOM event와 limit hit를 metric으로 수집하고 restart loop를 감시한다. Cache warmup이 매 restart마다 같은 peak를 만들어 영구 장애로 이어질 수 있다.

## CHAPTER 12 · memory.high는 hard kill 전에 pressure와 throttling을 주는 control point다

Memory high boundary는 넘어갈 수 있지만 reclaim과 allocation slowdown을 유발해 cgroup이 스스로 사용량을 낮추도록 pressure를 건다. Hard max보다 먼저 반응하므로 graceful degradation과 load shedding의 신호로 활용할 수 있다.

하지만 high를 너무 낮게 잡으면 정상 workload가 계속 direct reclaim에 들어가 latency가 악화된다. 너무 높으면 OOM 직전까지 아무 신호가 없다. Working set과 reclaimability를 측정해 설정한다.

Memory usage만 보지 말고 PSI와 reclaim stall을 함께 본다. 같은 90% usage라도 cache가 쉽게 회수되는 경우와 anonymous working set이 꽉 찬 경우의 위험이 다르다.

## CHAPTER 13 · cgroup OOM은 host OOM과 다른 failure scope를 만든다

Container가 memory.max를 넘으면 해당 cgroup 안의 task가 OOM victim이 될 수 있다. 다른 tenant는 정상일 수 있어 isolation에는 도움이 되지만, service 내부에서는 일부 worker만 죽어 partial failure가 될 수 있다.

Supervisor가 child process만 재시작할지 전체 unit을 재시작할지 정책을 정한다. Shared memory와 lock owner가 죽었을 때 consistency도 고려한다.

OOM score와 victim selection에 의존해 중요한 process가 항상 살아남을 것이라 가정하지 않는다. Essential component는 별도 cgroup으로 분리하거나 resource reservation을 둘 수 있다.

## CHAPTER 14 · cpu.max는 일정 기간 동안 사용할 수 있는 CPU time을 quota로 제한한다

Cgroup CPU quota는 여러 CPU에 동시에 실행될 수 있어도 period당 총 CPU time이 quota에 도달하면 throttling한다. 네 개 CPU가 보이는 container가 실제로는 한 CPU 분량만 사용할 수도 있다. Thread pool을 logical CPU count만으로 만들면 과도한 runnable task가 생긴다.

Throttling은 application 내부에서는 scheduler stall처럼 보인다. CPU utilization이 quota 대비 100%인데 host는 idle할 수도 있다.

Metrics에 throttled periods와 throttled time을 포함한다. Latency spike와 quota event가 맞물리면 code optimization보다 resource allocation이 해결책일 수 있다.

## CHAPTER 15 · cpu.weight는 hard quota가 아니라 contention 시 상대적 share를 조절한다

CPU weight는 host에 여유가 있을 때 사용을 제한하지 않고, 여러 cgroup이 경쟁할 때 scheduler가 상대적 비중을 나누는 데 사용된다. 따라서 “weight 50이면 CPU 50%”처럼 절대 quota로 해석하면 안 된다.

Burst가 드문 background job에는 quota보다 weight가 throughput을 덜 제한하면서 foreground를 보호할 수 있다. 반대로 strict tenant billing에는 quota가 필요할 수 있다.

실험은 idle host와 saturated host 두 조건에서 한다. Weight 효과는 경쟁이 있을 때 나타나므로 단독 benchmark만 보면 차이가 없을 수 있다.

## CHAPTER 16 · pids.max는 fork/thread 폭발의 blast radius를 cgroup 안에 가둔다

PID controller는 cgroup 안의 task 수를 제한해 fork bomb이나 runaway thread creation이 host 전체 task table을 소모하지 않게 한다. Limit에 도달하면 새 task 생성이 실패하므로 application은 이 경로를 처리해야 한다.

Service가 worker를 동적으로 늘리는 구조라면 peak task 수에 helper process와 runtime thread까지 포함한다. 예상 worker 수와 pids.max를 같은 숫자로 두면 headroom이 없다.

Limit hit metric과 task count slope를 관찰한다. Thread leak을 limit 상향으로 숨기지 않는다.

## CHAPTER 17 · I/O controller는 storage bandwidth와 latency contention을 조절한다

여러 service가 같은 block device를 공유하면 한 tenant의 sequential write가 다른 tenant의 latency를 악화시킬 수 있다. Cgroup I/O controller는 weight나 rate limit을 통해 device별 resource share를 조정할 수 있다.

하지만 filesystem cache 때문에 userspace write 순간과 실제 block I/O 시점이 다를 수 있다. Buffered I/O workload는 limit 효과가 writeback 시점에 나타날 수 있다.

Device별 metric과 cgroup별 bytes/latency를 함께 본다. Logical file throughput만으로는 shared storage contention을 정확히 설명하기 어렵다.

## CHAPTER 18 · cpuset은 CPU와 memory node의 사용 가능한 위치를 제한한다

Cpuset은 task가 실행 가능한 CPU와 memory allocation node를 지정해 isolation과 NUMA locality를 만든다. CPU quota와 달리 “어디에서” 실행할지 통제한다. 잘못된 cpuset은 충분한 quota가 있어도 parallelism을 막을 수 있다.

Parent/child hierarchy의 effective mask가 intersection으로 줄어드는지 확인한다. 설정 파일에 CPU 0-7이 적혀 있어도 ancestor가 0-3만 허용하면 실제 usable set은 더 작다.

Topology change와 hotplug 후 effective cpuset을 다시 확인한다. Offline CPU를 포함한 stale mask는 예상과 다른 scheduler behavior를 만들 수 있다.

## CHAPTER 19 · PSI는 resource가 부족해 task가 실제로 멈춘 시간을 pressure로 표현한다

Pressure Stall Information은 CPU, memory, I/O 때문에 runnable 또는 작업 가능한 task가 얼마나 stall됐는지를 보여준다. 단순 usage percentage보다 사용자 영향에 가까운 신호가 될 수 있다. Memory 95%라도 reclaim stall이 없으면 안정적일 수 있고, 70%에서도 thrashing이 심하면 PSI가 높을 수 있다.

PSI의 `some`과 `full` 의미를 구분해 읽는다. 일부 task만 막힌 상태와 모든 non-idle task가 동시에 progress하지 못한 상태는 severity가 다르다.

Admission control은 PSI의 짧은 spike에 과민 반응하지 않도록 window와 hysteresis를 둔다. Usage와 pressure를 함께 봐야 한다.

## CHAPTER 20 · quota와 reservation은 최대 사용량과 최소 보장을 각각 표현한다

Quota만 있으면 tenant가 너무 많이 쓰는 것을 막을 수 있지만, host가 과도하게 overcommit되면 모든 tenant가 동시에 필요한 최소 resource를 받지 못할 수 있다. Reservation은 중요 workload에 최소 capacity를 확보하는 다른 방향의 control이다.

Memory와 CPU는 reservation semantics가 다를 수 있다. Scheduler weight, guaranteed QoS, memory.low 같은 mechanism을 platform에 맞게 이해한다.

Capacity planning에서는 hard max 합계뿐 아니라 guaranteed reservation 합계가 physical capacity를 넘지 않는지 확인한다. Overcommit 비율은 의도적으로 결정한다.

## CHAPTER 21 · container 내부에서 보이는 resource는 host 전체가 아니라 effective limit이어야 한다

Runtime API가 host CPU count와 memory total을 그대로 반환하면 containerized application이 너무 큰 thread pool과 cache를 만들 수 있다. Modern runtime이 cgroup 정보를 읽어 usable capacity를 계산하는 이유다.

하지만 nested container나 오래된 runtime에서는 인식이 불완전할 수 있다. Startup에서 runtime-reported 값과 cgroup effective 값이 일치하는지 확인한다.

Auto-sizing formula에는 hard limit만 쓰지 말고 다른 process의 reserve와 headroom을 제외한다. “메모리의 80% cache” 같은 설정이 container 전체를 압박하지 않게 한다.

## CHAPTER 22 · kernel memory도 container workload의 실제 memory cost에 포함된다

Page table, socket buffer, slab object, filesystem metadata 같은 kernel memory는 application heap 밖에서 증가한다. High-connection service는 user heap이 안정적이어도 socket과 network metadata로 memory pressure를 만들 수 있다.

Memory accounting이 어떤 kernel component를 cgroup에 포함하는지는 kernel version과 controller semantics를 확인한다. RSS 하나로 container memory를 설명하지 않는다.

Incident에서 anonymous/file/slab/socket 분류를 본다. Heap dump가 작다는 이유로 memory leak이 없다고 결론내리지 않는다.

## CHAPTER 23 · socket memory는 connection 수와 traffic burst에 따라 크게 증폭될 수 있다

각 socket에는 send/receive buffer와 protocol state가 필요하다. Auto-tuning이 buffer를 키우면 고대역폭 connection에서는 효율적이지만 수만 connection이 동시에 큰 buffer를 가지면 memory가 크게 늘 수 있다.

Application-level queue가 socket buffer 뒤에 또 있으면 같은 data를 여러 번 buffer할 수 있다. Backpressure를 위 계층으로 전달하지 않으면 memory budget이 쉽게 무너진다.

Per-socket 평균과 tail buffer 크기를 connection 수와 곱해 capacity를 추정한다. Idle connection과 active connection을 분리한다.

## CHAPTER 24 · 등록된 kernel resource는 fd가 없어도 별도 lifetime과 limit을 가질 수 있다

Pinned buffer, io_uring registration, shared memory, device context처럼 kernel에 등록한 resource는 application object와 다른 lifetime을 가질 수 있다. File descriptor를 닫았다고 모든 관련 resource가 즉시 해제된다고 가정하면 안 된다.

Registration API는 실패 시 partial success를 남길 수 있는지, unregister가 blocking인지 확인한다. Limit hit가 발생하면 어떤 errno와 metric으로 보이는지도 문서화한다.

Long-running service에서는 registered count와 bytes를 별도 계측한다. Heap leak detector로는 보이지 않는 resource leak을 찾는 데 필요하다.

## CHAPTER 25 · per-thread cache는 contention을 줄이는 대신 memory budget을 thread 수에 곱한다

Allocator와 library가 thread-local cache를 쓰면 global lock을 줄여 성능이 좋아질 수 있다. 하지만 worker가 많을수록 idle cache가 누적되고 cgroup memory limit에 더 빨리 접근한다. Burst 후 cache가 오래 남으면 RSS가 내려오지 않을 수 있다.

Cache 크기와 thread pool 크기를 함께 튜닝한다. 둘을 독립 설정으로 보면 `cache_per_thread × max_threads` worst case를 놓친다.

Metrics에 active thread와 cached bytes를 같이 두면 leak과 정상 cache retention을 구분할 수 있다. Memory pressure 시 cache trim hook이 실제 동작하는지도 테스트한다.

## CHAPTER 26 · leak은 절대 사용량보다 시간에 따른 증가 slope로 더 잘 보일 수 있다

Traffic과 cache warmup 때문에 memory가 올라가는 것은 정상일 수 있다. 문제는 workload가 안정된 뒤에도 fd, thread, heap, registered buffer가 계속 증가하는 경우다. 그래서 leak detection은 usage level과 함께 `resource/time` slope를 본다.

Long soak test에서 request count로 normalize하면 workload 변화의 영향을 줄일 수 있다. 예를 들어 `fds per million requests`가 계속 증가하는지 본다.

Restart가 leak을 숨길 수 있으므로 uptime이 긴 canary의 trend를 보존한다. Hard limit 직전 alert만으로는 원인을 분석할 시간이 부족하다.

## CHAPTER 27 · admission control은 resource가 고갈되기 전에 새 work를 거절하는 정책이다

Queue가 이미 memory와 CPU를 모두 소모한 뒤 OOM으로 죽는 것보다, 안전한 threshold에서 신규 request를 빠르게 reject하는 편이 전체 system availability를 지킬 수 있다. Admission 신호는 queue depth, memory pressure, CPU saturation, downstream health를 조합할 수 있다.

단순 usage 90% 같은 한 조건은 burst에 과민할 수 있다. Hysteresis와 최소 지속 시간을 두어 flap을 줄인다. Priority가 있다면 중요 request에 reserve capacity를 남긴다.

Reject response는 caller가 retry storm을 만들지 않도록 backoff 정보를 줄 수 있다. Admission은 local resource control과 distributed retry policy가 만나는 지점이다.

## CHAPTER 28 · graceful degradation은 hard failure 전에 기능 비용을 낮춘다

Resource pressure가 높을 때 optional cache, expensive analysis, large image quality를 낮추면 핵심 기능을 유지할 수 있다. 하지만 degradation path가 평소 실행되지 않으면 오히려 장애 시 bug를 드러낼 수 있다.

각 기능의 resource cost와 사용자 영향을 분류해 단계별 degradation ladder를 만든다. Memory pressure인데 CPU-heavy 기능만 끄는 등 신호와 조치가 어긋나지 않게 한다.

Fault test로 단계 전환과 복귀를 검증한다. Pressure가 사라졌을 때 feature가 정상 상태로 돌아오는지, oscillation이 없는지도 본다.

## CHAPTER 29 · headroom alert는 limit hit보다 앞선 위험 구간을 알려야 한다

Hard limit 도달 이벤트는 이미 incident일 수 있다. 예상 growth rate와 peak burst를 고려해 충분히 앞에서 경고해야 한다. `remaining = limit - usage`를 절대값과 시간-to-exhaustion 관점으로 본다.

Memory와 fd처럼 leak 가능 resource는 slope 기반 ETA가 유용하고, CPU처럼 순간 변동이 큰 resource는 saturation window와 queue delay가 더 적합하다.

Alert threshold는 service마다 다르게 정한다. 동일 80%라도 reclaimable cache가 많은 system과 PID limit이 80%인 system의 위험이 다르다.

## CHAPTER 30 · resource control의 최종 contract는 limit·pressure·admission이 연결된 폐루프다

좋은 resource policy는 hard ceiling만 두지 않는다. 실제 usage를 정확히 accounting하고, pressure가 사용자 latency에 영향을 주기 시작하는 지점을 감지하며, limit 전에 admission 또는 degradation을 수행한다. 이후 pressure가 낮아지면 안전하게 정상 상태로 복귀한다.

Rlimit, cgroup, kernel table, application queue는 서로 다른 계층이므로 effective limit을 계산해야 한다. 한 숫자만 올려 문제를 해결하려 하면 다음 병목으로 이동한다.

운영에서는 usage, pressure, rejection, OOM/throttle event를 같은 timeline에 둔다. 이 evidence가 있어야 resource exhaustion을 단순 capacity 부족과 leak, 잘못된 autosizing, noisy neighbor로 구분할 수 있다.
