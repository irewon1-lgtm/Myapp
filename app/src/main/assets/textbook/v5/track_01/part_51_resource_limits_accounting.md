# PART 51 · Resource Limits and Accounting — rlimit, cgroups, quotas, pressure, admission

운영체제 자원은 CPU와 RAM 두 개가 아니다. File descriptor, process/thread slot, virtual address space, stack, locked memory, cgroup memory, CPU time budget, I/O bandwidth, PID count가 각각 다른 exhaustion path를 가진다. Limit은 단순 안전장치가 아니라 overload 시 **누가 먼저 실패하고 어떤 error/kill/throttle로 나타나는지**를 결정한다. Capacity planning과 failure handling은 자원별 accounting 단위와 enforcement semantics를 알아야 한다.

## CHAPTER 01 · resource budget은 평균 사용량이 아니라 허용 가능한 최대 동시 점유를 정의한다

평균 메모리 2GB인 service도 burst에서 8GB를 쓰면 4GB limit 환경에서 죽는다. 평균 fd 100개라도 connection storm에서 50,000개가 필요할 수 있다. Capacity는 arrival burst, request lifetime, concurrency와 연결해 peak/percentile을 본다. Limit은 정상 peak보다 충분한 headroom을 주되 unbounded leak/overload가 host 전체를 망가뜨리지 않도록 설정한다. Budget을 정하지 않은 resource는 incident 때 default/system-wide limit이 임의 정책이 된다.

## CHAPTER 02 · RLIMIT은 process 또는 credential 범위의 전통적 resource ceiling을 제공한다

Unix-like system의 rlimit은 open file, address space, stack, core dump 등 여러 resource에 soft/hard limit을 둘 수 있다. Soft limit은 process가 일정 범위에서 조정 가능하고 hard limit은 더 강한 ceiling 역할을 할 수 있다. Container cgroup limit과 rlimit은 서로 대체가 아니라 다른 계층이다. Effective limit을 확인할 때 shell ulimit 설정, service manager, container runtime, application 자체 setrlimit을 모두 추적한다.

## CHAPTER 03 · RLIMIT_NOFILE은 integer fd 번호가 아니라 process fd table capacity와 연결된다

File/socket/pipe/eventfd/inotify 등 많은 kernel object가 file descriptor를 사용한다. Limit에 도달하면 open/socket/accept가 EMFILE로 실패할 수 있다. Listener가 accept하지 못하면 connection backlog가 차고 monitoring/log file open도 실패할 수 있다. Per-request fd 사용량×concurrency와 background fd를 합산해 budget을 정한다. Leak test는 request 종료 후 fd count가 baseline으로 돌아오는지 확인한다.

## CHAPTER 04 · system-wide file table exhaustion과 per-process fd exhaustion은 다른 failure다

Process limit에 여유가 있어도 kernel 전체 open-file resource가 부족하면 ENFILE류 system-level failure가 발생할 수 있다. Shared host에서 한 tenant leak이 다른 process에 영향을 줄 수 있다. Container isolation이 모든 global kernel table을 자동 partition하지 않는 경우가 있다. Host metric과 process metric을 둘 다 수집하고 global exhaustion에 대한 admission/tenant limit을 둔다.

## CHAPTER 05 · process/thread count limit은 fork/clone 실패와 service capacity를 연결한다

Thread-per-request architecture는 thread stack/memory뿐 아니라 task/PID resource를 소비한다. RLIMIT_NPROC, cgroup pids.max, system PID space 같은 limit에 도달하면 새 thread/process 생성이 실패할 수 있다. Error path가 worker를 더 생성하려고 반복하면 busy failure loop가 된다. Thread pool은 bounded size와 queue/rejection policy를 가져야 하며 child-process supervisor도 fork failure를 처리해야 한다.

## CHAPTER 06 · virtual address space limit과 resident memory limit은 같은 것이 아니다

Large mmap reservation은 virtual address를 많이 차지하지만 실제 page를 touch하기 전 RSS는 작을 수 있다. 반대로 shared page는 여러 process RSS에 보이지만 physical memory는 공유될 수 있다. RLIMIT_AS류 virtual ceiling과 cgroup physical/accounted memory limit을 구분한다. 32-bit process는 address space fragmentation 자체가 allocation failure를 만들 수 있다. Memory dashboard에서 VIRT/RSS/PSS/commit을 같은 `메모리 사용량`으로 합치지 않는다.

## CHAPTER 07 · stack limit은 recursion과 thread count 두 방향으로 작동한다

Main/process stack rlimit과 pthread stack size는 플랫폼 API가 다를 수 있다. 한 thread에 큰 stack reserve를 주면 deep recursion 여유는 늘지만 수천 thread에서 virtual/committed memory 비용이 커진다. Guard page와 actual committed growth를 구분한다. Stack overflow test는 production stack size와 같은 조건에서 수행하며, recursive algorithm을 heap-based explicit stack으로 바꿀지 complexity와 input depth를 기준으로 판단한다.

## CHAPTER 08 · locked/pinned memory limit은 DMA·real-time·crypto workload에 별도 ceiling을 만든다

Page를 mlock/pin하면 reclaim/swap할 수 없어 system flexibility가 줄어든다. RDMA/DMA registered buffer, realtime application, secret page가 pinned memory를 사용할 수 있다. RLIMIT_MEMLOCK 또는 device/kernel policy가 실패를 만들 수 있다. `RAM이 충분한데 registration 실패`는 ordinary heap 부족과 다른 원인이다. Long-lived pinning을 최소화하고 cleanup 누락을 별도 leak으로 감시한다.

## CHAPTER 09 · core dump limit은 debugging evidence와 sensitive-data exposure를 동시에 제어한다

Crash core는 register, stack, heap 일부를 포함해 원인 분석에 유용하지만 secret/token/user data를 담을 수 있다. RLIMIT_CORE와 dump policy가 0이면 production crash에서 core가 생성되지 않을 수 있다. 무작정 unlimited로 켜는 것도 위험하다. Encrypted/restricted crash storage, size limit, symbol server를 설계하고 incident requirement와 privacy/security를 함께 맞춘다.

## CHAPTER 10 · cgroup v2는 resource controller를 hierarchy에 적용한다

Cgroup tree에서 parent가 가진 resource capacity 안에서 child group을 제한·가중할 수 있다. Process는 membership을 통해 CPU/memory/pids/io controller 영향을 받는다. Container memory limit은 VM처럼 별도 physical RAM을 할당하는 것이 아니라 host kernel accounting/enforcement policy다. Parent limit이 더 작으면 child가 큰 limit을 요청해도 실제 usable resource는 상위 ceiling을 넘지 못한다.

## CHAPTER 11 · memory.current와 memory.max는 사용량과 hard ceiling을 분리한다

Cgroup memory.current는 현재 accounted usage를 보여 주고 memory.max는 hard limit 역할을 한다. Limit 근처에서 reclaim이 발생해 latency가 먼저 악화될 수 있으며, max를 넘는 allocation이 항상 application malloc error로 돌아오는 것은 아니다. OOM selection/kill이 발생할 수 있다. Headroom alert는 kill event만 기다리지 않고 memory pressure와 reclaim stall을 함께 본다.

## CHAPTER 12 · memory.high는 즉시 kill보다 reclaim/throttle pressure를 유도하는 control이 될 수 있다

Hard max에 닿기 전 high boundary를 사용하면 workload가 과도한 memory를 쓰는 동안 allocation/reclaim path에서 pressure를 받게 할 수 있다. 이는 graceful backpressure를 유도하지만 latency가 증가한다. Service가 memory.high에서 이미 SLO를 잃는다면 max 숫자만 capacity로 보고 worker를 채우면 안 된다. Memory pressure point를 load test에서 찾는다.

## CHAPTER 13 · cgroup OOM event는 process-local OutOfMemory exception과 다른 failure다

Cgroup memory limit에서 kernel이 victim process를 kill하면 managed runtime catch block이 실행될 기회가 없을 수 있다. Multi-process service에서 어떤 process가 죽는지, oom.group 정책으로 group을 함께 kill할지 운영 contract가 필요하다. Supervisor restart와 persistent state recovery를 abrupt-crash 기준으로 검증한다. OOM event counter와 exit reason을 alert에 포함한다.

## CHAPTER 14 · cpu.max는 CPU time을 period budget으로 제한해 throttling을 만든다

Container가 4 logical CPU를 볼 수 있어도 cpu.max가 1 CPU equivalent budget이면 worker 4개가 동시에 돌다 period budget을 빨리 소진하고 remainder 동안 throttled될 수 있다. CPU utilization만 보면 각 worker가 바쁘지만 wall latency는 quota stall 때문에 커진다. nr_throttled/throttled time을 측정하고 worker concurrency를 effective quota에 맞춘다.

## CHAPTER 15 · cpu.weight는 hard quota가 아니라 competing groups 사이 relative share다

Host가 idle하면 낮은 weight group도 충분한 CPU를 사용할 수 있지만 contention이 생기면 relative weight가 distribution에 영향을 준다. 따라서 staging idle host에서 성능이 좋다가 production contention에서 느려질 수 있다. Weight를 absolute core guarantee로 설명하지 않는다. Critical service에는 reservation/quota/placement policy를 함께 고려한다.

## CHAPTER 16 · pids.max는 fork bomb뿐 아니라 thread explosion을 격리한다

Linux task 단위 accounting 때문에 thread도 pids controller에 포함될 수 있다. Application bug가 unbounded thread를 만들 때 group pids.max가 host 전체를 보호하지만 service는 thread creation failure를 경험한다. Rejection/health check가 이를 정확히 보고해야 한다. Limit hit 후 recovery를 위해 runaway producer를 멈추고 existing task가 drain될 수 있어야 한다.

## CHAPTER 17 · I/O controller는 bandwidth/IOPS/weight를 device 단위로 제한할 수 있다

CPU와 memory가 여유 있어도 cgroup io.max/io.weight 또는 storage QoS 때문에 read/write latency가 늘 수 있다. Logical filesystem 아래 실제 block device mapping을 알아야 controller가 어디에 적용되는지 이해할 수 있다. Buffered write는 page cache에 빨리 끝난 뒤 writeback에서 throttling될 수 있어 application write latency만 보면 놓칠 수 있다. Device queue와 cgroup stats를 함께 본다.

## CHAPTER 18 · cpuset은 허용 CPU와 NUMA memory node를 동시에 제한할 수 있다

Cpuset controller로 workload가 실행 가능한 CPU와 allocation 가능한 memory node를 지정할 수 있다. CPU affinity와 cpuset effective mask의 교집합이 실제 placement를 결정한다. Memory node 제한과 first-touch가 결합돼 remote/locality behavior가 달라진다. Container 내부 `/proc/cpuinfo` 숫자보다 effective cpuset을 worker sizing에 사용한다.

## CHAPTER 19 · Pressure Stall Information류 metric은 `자원이 부족해 얼마나 기다렸는가`를 본다

Usage 90%라는 숫자보다 task가 CPU/memory/I/O resource 때문에 실행 progress를 못 한 시간 비율이 SLO와 더 직접 연결될 수 있다. Memory reclaim로 task가 stall되면 RSS가 limit 아래여도 pressure가 높다. CPU runqueue contention, I/O wait도 동일한 관점으로 볼 수 있다. Resource dashboard에 usage와 stall을 함께 배치한다.

## CHAPTER 20 · quota와 reservation은 `최대 사용량`과 `보장 용량`을 분리한다

Hard max만 설정하면 여러 tenant가 동시에 peak에 도달했을 때 host capacity를 초과할 수 있다. Reservation/request는 scheduler placement와 capacity planning에 최소 필요 자원을 전달하고 limit은 폭주 upper bound를 정한다. Overcommit을 허용하면 statistical multiplexing 이득과 simultaneous-peak risk를 교환한다. SLO-critical service는 reservation을 명시하고 burst service는 overcommit policy를 별도로 둔다.

## CHAPTER 21 · container에서 host-visible resource와 container-effective resource가 다를 수 있다

Host에는 64 CPU/256GB가 있어도 container는 cpuset 4 CPU, cpu quota 2 core, memory.max 8GB일 수 있다. Runtime/library가 host total을 읽어 cache size와 worker count를 잡으면 limit을 초과한다. Container-aware API와 cgroup filesystem을 사용한다. Kubernetes/other orchestrator request/limit도 실제 kernel controller로 어떻게 반영되는지 확인한다.

## CHAPTER 22 · kernel memory와 page cache도 cgroup memory accounting에 포함될 수 있다

Application heap만 줄였는데 memory.current가 높다면 page cache, socket buffer, kernel object가 accounted될 수 있다. Workload 특성에 따라 file cache가 large memory를 사용하지만 reclaimable할 수 있다. `heap 2GB인데 container 4GB OOM`을 GC tuning만으로 해결하지 않는다. Memory stat breakdown과 fd/socket/page-cache를 조사한다.

## CHAPTER 23 · socket buffer와 connection count도 memory budget을 소비한다

각 TCP/socket는 kernel send/receive buffer와 protocol state를 갖는다. Connection 수가 수십만이면 heap보다 kernel network memory가 커질 수 있다. Backpressure 없이 receive buffer가 쌓이면 cgroup memory pressure에도 영향을 준다. Network Track에서 protocol을 배우더라도 system resource 관점에서는 connection=fd+kernel memory+timer state라는 budget을 계산해야 한다.

## CHAPTER 24 · timer/watch registration도 kernel object와 queue entry를 소비한다

수백만 timer, epoll watch, inotify watch를 만들면 user object뿐 아니라 kernel data structure가 증가한다. Limit이 sysctl/namespace 단위일 수 있고 error가 memory 부족처럼 나타나지 않을 수 있다. Long-lived watch를 unregister하지 않는 leak은 fd count가 일정해도 발생할 수 있다. Resource inventory에 invisible registration count를 포함한다.

## CHAPTER 25 · per-thread allocator/cache가 resource accounting을 분산시킨다

Global free memory가 있어도 thread-local allocator cache가 idle thread에 memory를 보유해 RSS가 높게 유지될 수 있다. Cgroup max에서는 이 retained cache도 capacity를 소비한다. Thread count 감소, cache scavenging, arena tuning을 heap object leak과 구분한다. P17 allocator와 P47 TLS를 resource budget 관점에서 다시 연결한다.

## CHAPTER 26 · resource leak은 slope로 보는 것이 단일 snapshot보다 강하다

Fd 5,000개가 많아 보여도 stable pool일 수 있고 100개가 매 request 1개씩 증가하면 leak이다. Load 단계별로 request count 대비 resource slope를 측정하고 idle drain 후 baseline으로 돌아오는지 본다. Heap, fd, threads, timers, native memory를 같은 soak test에서 수집한다. Leak detector는 warmup cache growth를 stable plateau와 구분한다.

## CHAPTER 27 · admission control은 limit hit 전에 new work를 거절하는 정책이다

Memory/connection/thread pool이 hard ceiling에 닿은 뒤 실패하면 이미 in-flight work가 함께 위험해진다. Queue length, active connection, memory headroom을 기준으로 새 request를 429/busy/retry-later류로 거절하면 기존 request의 성공률을 지킬 수 있다. Admission threshold는 capacity test와 SLO에서 정하고 hysteresis로 oscillation을 줄인다.

## CHAPTER 28 · graceful degradation은 scarce resource에서 기능 priority를 명시한다

Memory pressure에서 thumbnail/cache를 버리고 core transaction을 유지하거나 CPU overload에서 optional analytics를 중단하는 식으로 기능 tier를 정할 수 있다. Degradation path 자체가 allocation/logging을 많이 하면 위기에서 실패한다. Fault injection P49로 hard limit 직전 behavior를 검증한다. `일단 모든 요청 받기`보다 business priority를 resource policy에 반영한다.

## CHAPTER 29 · resource alert는 limit 값이 아니라 time-to-exhaust와 stall을 본다

Memory 80%가 stable하면 안전할 수 있고 fd가 분당 100개씩 증가하면 50%에서도 incident가 예측된다. Current usage, configured limit, derivative/slope, pressure stall, rejection/throttle event를 조합한다. Alert가 너무 늦어 hard kill 후 울리지 않게 headroom burn rate를 계산한다. Autoscaling이 해결 가능한 resource와 node-local hard limit을 구분한다.

## CHAPTER 30 · resource contract는 단위·accounting scope·enforcement 결과를 명시한다

각 자원마다 무엇을 세는지(RSS, tasks, fds, IOPS), 어느 scope(process/cgroup/host)인지, soft/high/max에서 어떤 behavior(throttle, error, kill)가 발생하는지 기록한다. Capacity test는 limit에 접근하며 latency와 pressure를 측정하고 P49 fault injection으로 실제 enforcement path를 실행한다. Dashboard와 application error는 같은 resource identity를 공유한다. `자원 부족`이라는 하나의 상태 대신 exhaustion domain별 recovery를 설계한다.