# PART 81 · Page Cache Writeback Engine — dirty state, throttling, flusher policy, error delivery, durability

Buffered file I/O의 핵심은 `write()`가 storage device에 바로 데이터를 기록하지 않는다는 사실이다. CPU가 page cache의 folio를 수정한 시점, kernel이 그 folio를 dirty로 회계한 시점, writeback I/O를 제출한 시점, device가 completion을 반환한 시점, volatile cache가 실제 nonvolatile media에 반영된 시점은 서로 다르다. 이 시간축을 하나의 `저장 완료`로 뭉개면 memory pressure, writeback stall, delayed `EIO`, `fsync()` 의미를 동시에 오해하게 된다. Page-cache writeback은 **memory state를 backing-store state로 수렴시키면서 writer 속도를 조절하고 오류를 뒤늦게라도 정확히 전달하는 상태기계**다.

## CHAPTER 01 · address_space는 파일의 page-cache 상태를 소유하는 핵심 객체다

Linux의 `struct address_space`는 이름과 달리 process virtual address space가 아니라 inode나 block device가 가진 cacheable object의 page-cache state를 표현한다. `i_pages`에는 cached folio들이, `writeback_index`에는 background writeback의 진행 위치가, `wb_err`에는 뒤늦게 발견된 writeback 오류가 들어간다. `a_ops`는 filesystem이 read/writeback/invalidate 동작을 구현하는 함수 집합을 연결한다. 따라서 buffered I/O를 추적할 때는 `struct file` 하나만 보지 말고 **file → inode → address_space → folio → backing block/I/O**의 ownership chain을 따라가야 한다. 같은 inode를 여러 file description이 열어도 page cache는 address_space 단위로 공유되므로 writer별 메모리 복사본이 따로 존재하는 것이 아니다.

## CHAPTER 02 · Page-cache hit는 storage access를 없애는 대신 freshness contract를 page cache로 옮긴다

Read가 이미 cached된 folio를 만나면 storage I/O 없이 memory에서 데이터를 반환할 수 있다. 이 성능 이득은 page cache가 backing store와 어느 시점까지 일치하는지를 kernel이 관리한다는 전제 위에 성립한다. Local filesystem에서는 inode mapping과 page-cache invalidation 규칙이 freshness를 통제하지만, network/distributed filesystem은 server-side mutation과 local cache coherency까지 추가로 해결해야 한다. `read()`가 빠르다는 사실은 storage가 빠르다는 뜻이 아니라 **요청 시점에 kernel이 유효하다고 판단한 cache state가 있었다는 뜻**이다. 성능 분석에서 cache hit와 actual device latency를 분리하지 않으면 hardware 병목을 잘못 추정한다.

## CHAPTER 03 · Cache miss의 read path는 folio allocation과 filesystem mapping을 함께 요구한다

Page cache에 원하는 offset이 없으면 kernel은 새 folio를 확보하고 해당 file offset을 backing storage의 block/extent와 연결한 뒤 I/O를 시작한다. Allocation이 memory pressure에서 stall할 수 있고, filesystem mapping lookup도 metadata lock이나 remote request를 요구할 수 있으므로 cache miss latency는 device service time만으로 설명되지 않는다. I/O가 끝나기 전에 같은 offset을 다른 thread가 읽으면 동일 folio에 합류해 completion을 기다릴 수 있어 duplicate I/O를 피한다. 따라서 read latency는 **cache lookup → allocation → mapping lookup → I/O submission → completion → folio uptodate transition**으로 분해해야 하며, 어느 단계가 tail을 만드는지 trace로 확인해야 한다.

## CHAPTER 04 · Readahead는 순차 접근을 예측해 demand fault보다 먼저 cache를 채운다

Kernel의 readahead state는 최근 access 위치와 window size를 보고 앞으로 필요할 가능성이 높은 file range를 미리 읽는다. 예측이 맞으면 application이 다음 read를 호출할 때 storage latency가 이미 숨겨져 throughput이 높아진다. 그러나 random access나 working set보다 큰 speculative window에서는 필요 없는 folio가 memory와 I/O bandwidth를 소비해 useful cache를 밀어낼 수 있다. Readahead는 단순히 `크게 할수록 빠른` 옵션이 아니라 **sequentiality confidence와 storage queue depth, memory pressure를 교환하는 speculative prefetch**다. 성능 튜닝은 hit ratio뿐 아니라 wasted readahead bytes와 reclaim cost를 함께 봐야 한다.

## CHAPTER 05 · mmap sequential fault도 readahead를 만들지만 access signal은 read syscall과 다르다

Memory-mapped file은 application이 `read()`를 호출하지 않고 CPU page fault를 통해 page cache를 채울 수 있다. Fault handler는 주변 access pattern과 mapping state를 사용해 readahead를 시도할 수 있지만, sparse/random fault pattern에서는 prediction이 쉽게 빗나간다. Userspace pointer traversal이 실제 file-I/O call stack을 거치지 않으므로 syscall profile만 보면 page-cache miss 원인을 놓칠 수 있다. mmap 성능 문제는 **major/minor fault, file readahead, page-cache hit, reclaim/refault**를 같이 관측해야 하며, pointer-level locality가 storage-level sequentiality로 이어지는지 확인해야 한다.

## CHAPTER 06 · Buffered write의 첫 핵심 전이는 clean folio를 dirty folio로 바꾸는 것이다

Buffered `write()`는 대개 user data를 page cache에 복사하고 해당 folio가 backing store보다 새 내용을 가진다는 사실을 dirty state로 기록한다. 이 시점에 application-visible write는 성공할 수 있지만 storage device에 I/O가 제출되지 않았을 수도 있다. Dirty bit는 단순 boolean이 아니라 `memory copy가 authoritative이며 backing store가 뒤처졌다`는 상태 표지다. Filesystem은 delayed allocation이나 metadata reservation을 함께 만들 수 있으므로 dirty data가 많아질수록 아직 물리 block이 완전히 확정되지 않은 state도 커질 수 있다. 따라서 `write() 성공 = durable`이라는 해석은 writeback engine의 존재 자체와 모순된다.

## CHAPTER 07 · Dirty accounting은 memory consumption과 storage debt를 동시에 계량한다

Dirty folio는 clean cache와 달리 즉시 버릴 수 없다. Reclaim하려면 먼저 backing store에 writeback하거나 data를 잃어야 하므로 dirty memory는 **미지급 storage I/O 부채**다. Kernel은 global/node/backing-device 또는 cgroup 관련 accounting을 통해 dirty volume을 추적하고 writer 속도를 제한한다. Dirty bytes만 보면 workload의 write amplification이나 slow device queue를 놓칠 수 있으므로 dirty generation rate와 writeback completion rate를 함께 봐야 한다. 생성률이 지속적으로 completion률보다 크면 dirty memory가 한계까지 증가한 뒤 writer throttling이 시작되며 latency가 갑자기 튄다.

## CHAPTER 08 · dirty_background threshold는 background flusher가 따라잡기 시작하는 선이다

Dirty memory가 background threshold 아래일 때는 writer가 빠르게 page cache만 갱신하고 device write를 나중으로 미룰 수 있다. Threshold를 넘으면 background writeback이 적극적으로 dirty folio를 storage로 내보내기 시작한다. 이 선은 application을 즉시 block시키는 hard limit가 아니라 **device가 backlog를 소비하기 시작하는 control-loop trigger**에 가깝다. Background threshold가 지나치게 높으면 burst absorption은 커지지만 crash 시 잃을 수 있는 volatile dirty set과 later flush burst가 커지고, 너무 낮으면 작은 burst도 즉시 storage traffic을 만들어 throughput이 흔들릴 수 있다.

## CHAPTER 09 · dirty_ratio 계열의 foreground limit는 writer 자신에게 backpressure를 건다

Dirty memory가 더 높은 limit에 도달하면 계속 memory-speed로 쓰게 둘 수 없다. `balance_dirty_pages()` 계열은 dirtying task의 속도를 backing device가 감당 가능한 writeback rate에 맞추도록 pause/throttle한다. 이 순간 `write()` latency가 storage 속도와 직접 연결되기 시작하므로 application은 갑자기 수 ms~수백 ms tail을 볼 수 있다. 원인을 찾을 때 syscall 자체 CPU cost를 의심하기보다 **dirty threshold, backing-device bandwidth, writeback backlog, throttling sleep**을 확인해야 한다. Memory가 많다고 무한 write burst를 흡수하는 설계는 결국 더 큰 stall로 되돌아온다.

## CHAPTER 10 · balance_dirty_pages는 단순 sleep이 아니라 feedback controller로 이해해야 한다

Writer throttling의 목적은 dirty pages를 일정 고정값 아래 두는 것뿐 아니라 현재 dirty position과 device writeout rate를 바탕으로 생성 속도를 안정화하는 것이다. Fast SSD와 slow network/block device가 같은 static pause를 사용하면 한쪽은 throughput을 버리고 다른 쪽은 backlog가 폭발한다. Writeback control은 measured completion rate와 dirty position error를 이용해 task별 pause를 조절하는 feedback system으로 보는 편이 정확하다. Control loop가 흔들리면 **짧은 burst→과도한 sleep→dirty 감소→다시 burst** 같은 latency oscillation이 생길 수 있으므로 평균 throughput만으로 건강성을 판단하면 안 된다.

## CHAPTER 11 · Dirty expiry는 오래된 data가 무기한 memory에 머무르지 않게 한다

`dirty_expire_centisecs` 같은 정책은 dirty state가 일정 시간 이상 오래되면 periodic flusher가 writeout 대상으로 삼도록 한다. 이는 application이 fsync를 전혀 호출하지 않더라도 dirty data가 영원히 RAM에만 머무르지 않게 하는 background policy다. 다만 `eligible for writeout`은 즉시 durable completion을 의미하지 않고 flusher scheduling, queue congestion, filesystem ordering을 더 거쳐야 한다. Crash-loss window를 reasoning할 때는 expiry 값 하나만 보지 말고 **dirty age→writeback submission→device completion→cache flush/durability** 전체 시간을 고려해야 한다.

## CHAPTER 12 · dirty_writeback interval은 flusher wakeup cadence이지 durability SLA가 아니다

Periodic flusher thread가 얼마나 자주 깨는지를 조절하는 값은 dirty data의 최대 persistence latency를 직접 보장하지 않는다. Flusher가 깨어도 dirty set이 크거나 device가 느리면 일부 folio는 여러 cycle 뒤에야 처리될 수 있고, filesystem이 metadata ordering을 추가로 요구할 수도 있다. 반대로 memory pressure나 explicit sync가 있으면 periodic interval보다 훨씬 일찍 writeback이 시작된다. 이 설정을 `5초면 디스크 저장` 같은 SLA로 해석하면 잘못이다. Durability가 필요하면 application-level sync contract를 사용해야 한다.

## CHAPTER 13 · Backing-device writeback domain은 서로 다른 storage 속도를 분리해 제어한다

한 시스템에 fast NVMe와 slow USB/network-backed storage가 함께 있으면 global dirty count만으로 writer를 제어할 경우 느린 device의 backlog가 빠른 device writer까지 과도하게 막을 수 있다. Writeback accounting은 backing device/domain을 구분해 각 storage의 dirty debt와 bandwidth를 반영하려 한다. File이 실제 어느 backing path에 연결됐는지, device mapper/remote filesystem 같은 중간 layer가 latency를 어떻게 추가하는지까지 추적해야 한다. `전체 dirty pages가 많다`보다 **어느 writeback domain이 debt를 만들고 있는가**가 진단에 더 중요하다.

## CHAPTER 14 · writeback_index는 huge cache에서 같은 앞부분만 반복 스캔하지 않게 한다

Address_space에는 background writeback 진행 위치를 기억하는 index가 있어 반복 writeback이 매번 file offset 0부터 시작하지 않도록 한다. 큰 sparse file에서 dirty folio가 넓게 분산돼 있으면 scan position과 clustering 정책이 I/O locality와 fairness에 영향을 준다. 특정 workload가 계속 앞부분만 dirty하게 만든다고 해서 다른 offset의 오래된 dirty data가 무한히 굶으면 안 된다. Writeback traversal은 **locality를 얻으면서 starvation을 방지하는 scheduling problem**이며, file offset order와 physical block order가 항상 같지도 않다.

## CHAPTER 15 · writeback_control은 한 번의 writeback 요청이 무엇을 얼마나 밀어낼지 전달한다

Filesystem의 `writepages`/iomap writeback path는 `struct writeback_control`을 통해 sync mode, 대상 range, `nr_to_write` 같은 정책 입력을 받는다. Background flusher와 fsync, reclaim-triggered writeback은 같은 dirty folio를 다뤄도 urgency와 blocking 허용 범위가 다르다. Filesystem이 이 intent를 무시해 항상 전체 file을 synchronous하게 밀면 tail latency가 폭증하고, 반대로 너무 적게 제출하면 explicit sync가 필요한 범위를 끝내지 못한다. Writeback API는 단순 callback이 아니라 **policy caller와 filesystem mapper 사이의 scheduling contract**다.

## CHAPTER 16 · Folio lock과 PageWriteback 상태는 data mutation과 I/O completion의 race를 분리한다

Writeback을 시작할 때 kernel은 folio state를 조정하고 해당 range에 I/O가 진행 중임을 표시한다. CPU writer가 동시에 같은 bytes를 다시 수정할 수 있으므로 `I/O 중이다`와 `새 dirty data가 없다`는 상태를 구분해야 한다. Writeback submission 직후 dirty bit를 clear해도 completion 전에 다시 write가 발생하면 folio는 재dirty되어 다음 writeback 대상이 된다. 이 state machine이 정확해야 **old snapshot을 쓰는 I/O와 new memory mutation**이 서로의 update를 지우지 않는다. Lock과 flags를 단순 `page 잠금`으로 보면 재dirty race를 이해할 수 없다.

## CHAPTER 17 · Writeback I/O submission과 completion은 다른 execution context에서 일어날 수 있다

Filesystem은 dirty folio를 bio/request로 변환해 block/network layer에 제출하고 즉시 다음 folio를 처리할 수 있다. 실제 completion은 interrupt, softirq, workqueue 또는 process context에서 나중에 도착한다. Completion path는 PageWriteback state를 해제하고 waiter를 깨우며 오류가 있으면 mapping에 기록해야 한다. Submission stack이 끝났다는 사실은 storage write가 끝났다는 뜻이 아니므로 profiler에서 `writepages가 빨리 return했다`만 보고 writeback이 빠르다고 판단하면 안 된다. Outstanding I/O depth와 completion latency가 별도 지표다.

## CHAPTER 18 · Iomap은 writeback 실패 시 dirty bit를 영원히 붙잡지 않고 오류를 기록한다

Storage failure가 발생한 folio를 계속 dirty 상태로 남겨 두면 flusher가 동일 실패를 무한 반복하며 dirty folio clot를 만들 수 있다. Iomap writeback path는 실패한 folio의 dirty state를 정리하면서 `-EIO`를 page cache/address_space error state에 기록해 userspace가 후속 `fsync()`에서 오류를 받게 한다. 이 설계는 **retry state와 error-reporting state를 분리**한다. `dirty bit가 사라졌다 = 데이터가 성공적으로 저장됐다`가 아니며, durability 판정에는 writeback error cursor를 반드시 포함해야 한다.

## CHAPTER 19 · errseq_t는 asynchronous writeback 오류를 시간 순서가 있는 stream으로 보존한다

Buffered `write()`가 성공한 뒤 수초 후 device에서 EIO가 발생하면 원래 syscall에 오류를 되돌려줄 수 없다. Address_space의 `wb_err`는 이런 delayed error가 발생했다는 사실과 sequence를 기록한다. File description은 자신이 마지막으로 확인한 error cursor를 보유해 `이전에 보고한 오류를 매 fsync마다 영원히 반복`하지 않으면서 이후 새 오류는 놓치지 않는다. Errseq_t는 단순 errno slot보다 **비동기 실패를 consumer별 checkpoint로 읽는 event stream**에 가깝다.

## CHAPTER 20 · fsync error는 특정 write syscall 하나에 정확히 귀속되지 않을 수 있다

Generic page-cache writeback은 각 dirty folio를 어느 file description이 변경했는지 완벽히 추적하지 않는다. 따라서 writeback 오류가 발생하면 당시 열려 있던 여러 file description이 다음 `fsync()`에서 동일 오류를 볼 수 있으며, 해당 fd가 직접 문제의 bytes를 쓰지 않았더라도 error가 전달될 수 있다. 반대로 error를 한 번 소비한 fd는 새 오류가 없으면 다음 fsync에서 0을 받을 수 있다. Application은 `fsync EIO = 직전 write 한 건 실패`처럼 좁게 해석하지 말고 **file/backing-store writeback stream에서 persistence failure가 있었다**고 처리해야 한다.

## CHAPTER 21 · fsync는 dirty data writeback과 metadata ordering을 파일시스템 계약에 따라 묶는다

`fsync()`는 page-cache data를 writeout하고 관련 metadata가 crash 후 파일을 올바르게 복원할 수 있는 상태가 되도록 filesystem-specific ordering을 수행한다. Data I/O completion만 기다리고 inode size, extent mapping, journal commit이 stable하지 않으면 reboot 뒤 bytes를 찾지 못할 수 있다. 반대로 filesystem journaling이 metadata를 commit했다고 user data가 반드시 durable한 것도 아니다. Durability proof는 **data blocks + metadata reachability + lower-device cache flush semantics**를 함께 포함해야 하며, fsync는 그 조합을 제공하는 filesystem API다.

## CHAPTER 22 · Device completion과 durable media commit은 volatile write cache 때문에 분리될 수 있다

Block device가 write request completion을 반환해도 controller volatile cache에만 data가 존재할 수 있다. Power loss까지 견뎌야 하는 filesystem ordering은 flush/FUA 같은 lower-layer primitive를 사용해 device cache와 media persistence를 제어한다. 따라서 PageWriteback bit가 clear된 시점과 application durability point가 다를 수 있다. Writeback engine은 memory pressure 해소를 위해 `device accepted data`만 필요할 수 있지만 fsync는 더 강한 persistence semantics를 요구한다. **Memory cleanliness와 crash durability는 동일 상태가 아니다.**

## CHAPTER 23 · mmap shared write는 page fault를 통해 dirty state를 만들므로 syscall write 추적만으로 부족하다

`MAP_SHARED` file mapping에서 CPU store가 처음 write-protected folio를 수정하면 page_mkwrite 계열 fault path가 filesystem reservation과 dirty transition을 준비한다. 이후 application이 ordinary memory instruction으로 계속 수정하므로 `write()` syscall count와 dirty generation rate가 일치하지 않는다. Database나 mmap-heavy workload를 분석할 때 major/minor fault, dirty folio growth, msync/fsync 호출을 같이 봐야 한다. File writeback은 syscall API가 아니라 **memory mapping의 modification history**도 입력으로 받는다.

## CHAPTER 24 · truncate/invalidate는 page cache와 backing mapping을 원자적으로 정리해야 한다

File size 축소나 hole punch가 진행되는 동안 writeback이나 mmap fault가 같은 range를 다시 채우면 삭제한 data가 되살아나거나 stale block에 I/O가 갈 수 있다. `invalidate_lock`, folio lock, filesystem mapping locks가 존재하는 이유는 page-cache contents와 file offset→storage mapping을 같은 coherency protocol 안에서 바꾸기 위해서다. Teardown은 새 fault/write를 차단하고 in-flight writeback을 조정한 뒤 cache entry를 제거해야 한다. 단순 `xarray에서 page 삭제`는 storage ownership 변경과 동기화되지 않는다.

## CHAPTER 25 · Direct I/O와 buffered I/O를 섞으면 page-cache coherency protocol이 필요하다

Direct write는 page cache를 우회해 storage를 갱신하므로 동일 range에 clean/stale cached folio가 남으면 이후 buffered read가 old data를 반환할 수 있다. Iomap direct-write path는 dirty page cache를 먼저 flush하고 write 전후 invalidate 같은 절차로 두 경로를 동기화한다. Concurrent mmap/buffered access가 있으면 더 복잡한 locking이 필요하다. Direct I/O는 `cache를 안 쓴다`가 아니라 **cache를 사용하는 다른 path와 충돌하지 않도록 명시적으로 cache state를 정리하는 I/O mode**다.

## CHAPTER 26 · Memory pressure에서 dirty folio는 clean cache처럼 즉시 reclaim할 수 없다

Clean file-cache folio는 backing store에 동일 data가 있으므로 reference가 없다면 버리고 나중에 다시 읽을 수 있다. Dirty folio는 최신 data가 memory에만 있으므로 reclaim 전에 writeback을 유도해야 한다. Storage가 느리거나 실패하면 reclaim path가 I/O debt에 묶이고 allocator latency가 길어진다. 그래서 memory pressure와 storage pressure는 독립 자원이 아니며, dirty-heavy workload에서 OOM 직전 CPU가 reclaim/writeback에 오래 머무를 수 있다. Memory metric만 보면 root cause가 storage saturation이라는 사실을 놓칠 수 있다.

## CHAPTER 27 · cgroup writeback은 dirty cost를 실제 producer에게 귀속시키려는 자원 격리 문제다

Multi-tenant system에서 한 cgroup이大量 dirty data를 만들고 global flusher가 처리하면 I/O와 memory pressure 비용이 다른 workload에 전가될 수 있다. Cgroup-aware writeback은 inode/backing state와 dirtying task attribution을 연결해 throttling/accounting을 가능한 producer domain에 귀속하려 한다. Shared inode를 여러 cgroup이 쓰는 경우 ownership이 이동하거나 mixed attribution이 필요해 단순하지 않다. Resource isolation은 CPU/memory limit만이 아니라 **dirty debt와 writeback bandwidth의 책임 주체**까지 포함해야 한다.

## CHAPTER 28 · Writeback 관측은 Dirty·Writeback 양과 throughput·latency를 같은 timeline에 놓아야 한다

`/proc/meminfo`의 Dirty/Writeback, vmstat counters, writeback tracepoints, block I/O latency를 따로 보면 causal relation을 놓친다. 건강한 steady state에서는 dirty generation과 completion이 장기적으로 균형을 이루고, backlog age가 bounded해야 한다. Dirty가 상승하는데 device throughput도 max라면 storage bottleneck, throughput은 낮은데 writer throttle이 크면 filesystem lock/error/congestion 같은 다른 제약을 의심한다. **양, 나이, 생성률, 완료률, task stall**을 같이 기록해야 control-loop failure를 설명할 수 있다.

## CHAPTER 29 · Failure injection은 ENOSPC와 EIO가 언제 application에 보이는지 검증해야 한다

Buffered write path에서는 metadata reservation 단계의 ENOSPC가 즉시 나타날 수도 있고, delayed allocation/writeback 단계의 space/I/O failure가 나중 fsync에서 나타날 수도 있다. Test는 `write 성공→background writeback 실패→fsync error`, `partial buffered write`, `device error 후 dirty state 정리`, `process가 fsync 없이 종료` 같은 timing matrix를 만들어야 한다. Error code 하나만 assert하지 말고 **어느 syscall이 오류를 소비했고 wb_err cursor가 다음 sync에서 어떻게 이동했는지**까지 확인해야 persistence contract가 검증된다.

## CHAPTER 30 · Page-cache writeback correctness는 memory state에서 durability까지 연속 ledger로 증명해야 한다

한 buffered write를 승인하려면 `어느 folio가 dirty가 되었는가`, `dirty debt는 어느 domain에 회계되는가`, `누가 언제 throttling되는가`, `어떤 writeback request가 어느 storage range로 제출됐는가`, `completion/error가 address_space에 어떻게 반영됐는가`, `fsync가 어떤 error cursor와 metadata order를 소비하는가`, `device volatile cache까지 포함한 durability point가 어디인가`를 연결할 수 있어야 한다. 이 chain 중 하나라도 관측되지 않으면 `write 성공`, `PageWriteback clear`, `fsync return` 같은 단일 신호만으로 저장 안전성을 주장할 수 없다. **Page cache는 단순 캐시가 아니라 memory-speed write와 storage durability 사이의 비동기 transaction engine**이다.
