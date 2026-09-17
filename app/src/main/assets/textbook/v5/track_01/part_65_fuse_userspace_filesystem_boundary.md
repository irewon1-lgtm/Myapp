# PART 65 · FUSE and Userspace Filesystem Boundary — request queues, caching, invalidation, interrupts, daemon failure

FUSE는 filesystem logic을 userspace로 옮기지만 VFS semantics까지 userspace 마음대로 바꾸는 기술이 아니다. Kernel client와 daemon은 **request identity, lookup lifetime, page cache, attribute validity, interrupt race, background queue pressure, daemon death, unmount**를 protocol로 합의해야 한다. 한 file operation이 process→kernel→daemon→backing service→kernel→process를 왕복하므로 queueing과 lifetime error가 곧 filesystem correctness error가 된다.

## CHAPTER 01 · FUSE mount는 kernel client와 userspace server를 연결한다

Application은 일반 VFS syscall을 사용하지만 FUSE filesystem operation은 kernel FUSE client가 request를 만들어 `/dev/fuse` 같은 transport로 userspace daemon에 전달한다. Daemon은 request를 처리해 reply를 돌려주고 kernel이 VFS operation을 완료한다. 따라서 filesystem latency에는 application scheduling뿐 아니라 daemon scheduling과 IPC queueing이 포함된다.

## CHAPTER 02 · INIT handshake는 protocol version과 capability를 협상한다

Kernel과 daemon이 지원하는 FUSE protocol major/minor, max write, readahead, feature flags를 handshake로 맞춘다. Struct layout과 feature semantics가 protocol version에 따라 달라질 수 있으므로 compile-time header version을 remote kernel capability로 가정하면 안 된다. Mount마다 negotiated state를 보존해야 한다.

## CHAPTER 03 · Request unique ID는 async reply를 원 operation과 결합한다

여러 threads의 filesystem operations가 동시에 daemon으로 전달되므로 reply order가 request order와 같을 필요가 없다. Kernel은 request identity로 response를 matching한다. Daemon이 wrong ID나 stale generation에 reply하면 다른 VFS operation의 state를 오염시킬 수 있으므로 request ID lifetime을 명확히 관리해야 한다.

## CHAPTER 04 · Lookup은 pathname string을 node identity로 변환하는 protocol이다

VFS pathname component lookup에서 daemon은 entry와 node identifier, attributes, validity time을 반환할 수 있다. Kernel은 이후 operation에서 pathname 대신 node ID를 사용할 수 있다. 따라서 daemon 내부 inode/object identity는 rename과 hard link 뒤에도 protocol lifetime 동안 일관돼야 한다.

## CHAPTER 05 · FORGET은 reply가 아니라 lookup reference 감소 notification이다

Kernel이 cached lookup reference를 더 이상 필요로 하지 않으면 FORGET을 보내 daemon이 node bookkeeping을 줄일 수 있다. 일반 request처럼 response를 기대하는 operation과 다르다. FORGET 처리가 늦거나 누락돼도 kernel request completion을 기다리지 않지만 daemon object retention이 커질 수 있다.

## CHAPTER 06 · Lookup count와 open handle lifetime은 서로 다르다

Directory entry cache에서 reference가 사라져도 file이 open되어 있으면 file handle state가 남을 수 있다. 반대로 open handle이 없어도 inode lookup reference가 존재할 수 있다. Path identity, node identity, open-file state를 하나의 refcount로 합치면 unlink/open semantics가 깨진다.

## CHAPTER 07 · getattr cache는 metadata freshness와 syscall latency를 교환한다

Kernel이 attribute validity timeout 동안 size/mode/timestamp를 cache하면 매 stat마다 daemon round trip을 피할 수 있다. 그러나 backing store가 다른 client에 의해 바뀌면 stale metadata가 노출될 수 있다. Cache timeout은 performance knob인 동시에 consistency contract다.

## CHAPTER 08 · Entry cache와 attribute cache는 invalidation 대상이 다르다

`name→node` mapping이 바뀌는 rename/unlink와 동일 node의 size/mtime이 바뀌는 write는 다른 cache를 무효화한다. Daemon이 external change를 알고도 적절한 invalidation notification을 보내지 않으면 kernel은 존재하지 않는 name이나 오래된 size를 계속 사용할 수 있다.

## CHAPTER 09 · Page cache를 사용할지 direct I/O를 사용할지는 coherence boundary를 바꾼다

Cached FUSE I/O에서는 kernel page cache가 read/write를 흡수하고 daemon request가 readahead/writeback 단위로 발생할 수 있다. Direct I/O는 cache를 우회해 daemon과 더 직접적으로 상호작용한다. 동일 file에 external writer가 있으면 어느 layer가 authoritative cache인지 명시해야 한다.

## CHAPTER 10 · Writeback cache는 write syscall completion과 daemon persistence를 분리한다

Kernel writeback cache가 enabled이면 user write가 page cache dirty state로 끝나고 daemon write request는 나중에 발생할 수 있다. `write returned=daemon received`가 아니다. fsync/flush/release semantics가 durability와 error reporting boundary를 정의해야 한다.

## CHAPTER 11 · Delayed write error는 이후 operation으로 전달될 수 있다

Async writeback에서 daemon/storage error가 original write syscall 뒤에 발생할 수 있다. Kernel/filesystem이 그 error를 fsync, close, later write 등에 어떻게 surface하는지 중요하다. Application이 close/fsync result를 무시하면 userspace filesystem의 delayed failure를 놓칠 수 있다.

## CHAPTER 12 · max_background는 outstanding async request capacity다

Daemon이 처리 중인 background request가 configured limit에 도달하면 kernel은 추가 requests를 block할 수 있다. 이 값은 `worker thread 수`가 아니라 kernel↔daemon pipeline의 in-flight admission limit이다. 너무 낮으면 device/network parallelism을 못 쓰고 너무 높으면 daemon queue와 memory retention이 커진다.

## CHAPTER 13 · congestion_threshold는 cache optimization을 줄이는 overload signal이다

Outstanding background request가 threshold를 넘으면 kernel은 async readahead나 non-synchronous writeback 같은 optional work를 줄일 수 있다. 이는 essential I/O를 보호하기 위한 backpressure다. Daemon overload가 page-cache behavior까지 바꾸므로 FUSE queue pressure는 application access pattern에 feedback을 준다.

## CHAPTER 14 · waiting counter는 idle 상태에서 daemon hang을 드러낼 수 있다

FUSE control filesystem의 waiting 값은 kernel에서 userspace로 전달 대기 중이거나 daemon 처리 중인 requests를 나타낸다. Filesystem activity가 없는데 값이 계속 남아 있으면 daemon deadlock/hang 가능성이 있다. Request count를 latency histogram과 결합하면 queue buildup과 stuck request를 구분할 수 있다.

## CHAPTER 15 · INTERRUPT는 original request와 별도 prioritized request다

Filesystem syscall이 signal로 중단되면 kernel은 original request가 daemon에 이미 전달되었는지에 따라 INTERRUPT를 queue할 수 있다. Daemon은 original operation을 취소하거나 무시할 수 있다. Cancellation은 local thread flag가 아니라 두 request identity 사이의 race protocol이다.

## CHAPTER 16 · INTERRUPT가 original request보다 먼저 도착할 수 있다

Priority 때문에 interrupt request가 daemon queue에서 original request보다 먼저 처리될 수 있다. Daemon이 original ID를 아직 모른다면 잠시 기다리거나 EAGAIN으로 interrupt를 다시 queue하게 해야 할 수 있다. `cancel target이 없으니 취소 완료`로 처리하면 original operation이 나중에 실행될 수 있다.

## CHAPTER 17 · Original reply가 먼저 끝나면 늦은 INTERRUPT는 stale control message다

Operation이 이미 완료되고 reply가 kernel로 돌아간 뒤 interrupt가 daemon에 도착할 수 있다. Daemon은 completed-request state를 잠깐 보존하거나 unknown interrupt를 safe하게 처리해야 한다. Cancellation state를 request object free와 동시에 없애면 late message race가 생긴다.

## CHAPTER 18 · Daemon worker pool은 filesystem-wide head-of-line blocking을 만들 수 있다

Slow remote metadata request 하나가 single-thread daemon을 막으면 unrelated local reads까지 지연된다. Worker concurrency를 늘리면 throughput은 좋아지지만 same inode mutation ordering과 directory serialization이 필요하다. Operation class별 queue와 bounded parallelism을 설계해야 한다.

## CHAPTER 19 · Daemon 자체가 mounted FUSE filesystem에 의존하면 dependency loop가 생긴다

Daemon executable/log/config/backing store가 자기 FUSE mount 위에 있고 fault/reclaim 시 해당 file을 access해야 하면 request를 처리하려고 다시 자신에게 request를 보내는 deadlock이 생길 수 있다. Daemon progress-critical resources는 filesystem dependency graph 밖에 두어야 한다.

## CHAPTER 20 · Memory reclaim과 FUSE writeback은 forward-progress deadlock을 만들 수 있다

Kernel이 memory reclaim을 위해 dirty FUSE page를 writeback하려는데 daemon이 memory allocation/reclaim에 막히면 cycle이 생길 수 있다. Background request reserve, daemon memory budget, swap/backing dependency를 포함해 low-memory progress를 설계해야 한다.

## CHAPTER 21 · Connection abort는 outstanding requests 전체의 failure boundary다

Daemon crash/hang 또는 administrator abort 시 kernel은 waiting requests를 error로 종료하고 새로운 requests도 실패시킬 수 있다. 각 syscall이 어떤 errno로 끝나는지보다 중요한 것은 partial operations와 cached state가 어떤 상태로 남는가다. Daemon restart를 지원하려면 session identity와 recovery protocol이 별도로 필요하다.

## CHAPTER 22 · Mount teardown은 new VFS entry를 막고 daemon work를 drain해야 한다

Unmount 과정에서 새 operations를 차단하고 outstanding requests, open handles, page writeback, notifications를 정리한 뒤 connection을 파괴해야 한다. Force/lazy unmount는 application-visible semantics가 다를 수 있다. Daemon exit와 unmount 순서를 명확히 하지 않으면 orphan requests가 남는다.

## CHAPTER 23 · Passthrough는 userspace policy와 kernel data path를 분리한다

FUSE passthrough는 daemon이 backing file을 등록하고 특정 open file의 read/write/mmap 등을 kernel이 backing file로 직접 처리하게 할 수 있다. Control plane은 daemon이 결정하지만 data plane round trip을 줄인다. 대신 backing-file lifetime/accounting과 filesystem stacking security가 새로운 invariant가 된다.

## CHAPTER 24 · Passthrough backing reference는 daemon FD table 밖에 존재할 수 있다

Daemon이 backing FD를 등록한 뒤 자신의 FD를 닫아도 kernel reference가 남을 수 있다. Resource가 process `lsof`/RLIMIT 관측에서 숨겨질 수 있어 privilege와 accounting 문제가 생긴다. Kernel-held reference도 tenant resource budget에 포함하는 정책이 필요하다.

## CHAPTER 25 · Filesystem stacking은 recursion depth와 teardown dependency를 만든다

FUSE passthrough backing file이 다른 stacked filesystem을 거쳐 다시 FUSE로 돌아오면 cycle/deep recursion이 생길 수 있다. Stack depth validation과 privilege restriction은 성능 문제가 아니라 kernel stack/teardown safety를 위한 boundary다.

## CHAPTER 26 · FUSE-over-io_uring은 transport queue를 바꾸지만 protocol semantics는 남는다

io_uring transport는 long-lived queue entries와 buffer pools로 kernel↔daemon communication overhead를 줄일 수 있다. 그러나 모든 request type이 같은 transport를 쓰는 것은 아닐 수 있고 interrupts/notifications는 legacy path가 남을 수 있다. Transport optimization과 filesystem protocol correctness를 분리해야 한다.

## CHAPTER 27 · Zero-copy FUSE는 payload copy 대신 buffer-pool lifetime을 관리한다

Registered buffer pool을 kernel과 daemon이 공유하면 request payload 복사를 줄일 수 있지만 slot ownership과 commit ID를 정확히 넘겨야 한다. Buffer를 너무 일찍 재사용하면 서로 다른 filesystem request payload가 섞인다. Zero-copy는 copy cost를 ownership protocol로 교환한다.

## CHAPTER 28 · virtio-fs는 FUSE protocol을 VM boundary까지 확장한다

Guest kernel이 FUSE client, host daemon이 server가 되어 virtqueue로 request/reply를 전달할 수 있다. Virtqueue는 enqueue 후 priority 재정렬이 어렵기 때문에 high-priority queue를 별도로 두는 등 `/dev/fuse`와 다른 queue semantics를 해결해야 한다. Same protocol도 transport topology에 따라 scheduling design이 달라진다.

## CHAPTER 29 · FUSE observability는 VFS wait와 daemon service를 분리해야 한다

Application syscall duration, kernel queue wait, daemon dequeue, backing-store service, reply transport, kernel completion을 각각 측정해야 한다. `daemon handler 1ms`인데 syscall 100ms라면 worker queue나 kernel throttling이 원인일 수 있다. Unique request ID를 end-to-end trace correlation key로 사용해야 한다.

## CHAPTER 30 · Userspace filesystem은 protocol server이자 cache-coherence system이다

안정적인 FUSE 설계는 **version negotiation, node/open lifetime, metadata/page cache validity, writeback errors, background admission, interrupt races, daemon forward progress, abort/unmount, passthrough/zero-copy ownership, transport observability**를 하나의 filesystem protocol로 다룬다. Userspace 구현의 장점은 개발 자유도지만 kernel VFS contract와 failure semantics를 더 명시적으로 책임져야 한다는 뜻이기도 하다.
