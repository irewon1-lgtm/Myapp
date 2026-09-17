# PART 53 · Buffered, Direct and Memory-Mapped I/O — page cache, alignment, coherence

파일 I/O API는 같은 bytes를 읽고 쓰더라도 **page cache를 경유하는가, application buffer와 device 사이를 직접 연결하는가, file page를 process address space에 mapping하는가**에 따라 ownership·copy·alignment·durability·coherence가 달라진다. 성능 차이는 syscall 횟수보다 cache hit, readahead, writeback, DMA alignment, invalidation, fault path에 의해 결정된다. 동일 file에 여러 I/O path를 섞을 때 stale-data와 ordering을 명시적으로 관리해야 한다.

## CHAPTER 01 · buffered read는 file data를 page cache와 application buffer라는 두 계층으로 본다

일반 read path에서 kernel은 file offset에 해당하는 page가 page cache에 있는지 확인하고, miss면 storage I/O를 통해 page를 채운 뒤 user buffer로 copy할 수 있다. 이후 같은 data를 다른 process가 읽으면 cache hit로 device 접근을 피할 수 있다. Application이 read한 byte를 수정해도 page cache 원본이 바뀌는 것은 아니다. 따라서 buffered read 비용은 storage latency + page-cache lookup + user copy의 조합이며, warm-cache benchmark와 cold-cache benchmark를 분리해야 실제 workload를 설명할 수 있다.

## CHAPTER 02 · buffered write는 write() return과 persistent media commit 사이를 분리한다

Application이 write를 호출하면 kernel이 user buffer를 page cache에 복사하고 page를 dirty로 표시한 뒤 빠르게 return할 수 있다. Dirty page는 writeback thread·memory pressure·fsync 등에 의해 나중에 storage로 내려간다. 따라서 write latency가 짧다고 device가 빠른 것이 아니며, crash durability도 보장되지 않는다. Dirty-page accumulation은 burst를 흡수하지만 writeback 시점에 latency cliff를 만들 수 있다. Application이 durability를 요구하면 sync contract를 별도로 호출하고 결과를 확인해야 한다.

## CHAPTER 03 · page cache는 file content와 virtual memory mapping을 연결하는 공통 object가 될 수 있다

Buffered read와 mmap file mapping은 같은 page-cache page를 바라볼 수 있어 한 path의 write가 다른 path에 어떻게 보이는지 filesystem/OS coherence rule이 중요하다. Mapped page가 dirty되면 writeback 대상이 되고 buffered read는 갱신된 cache page를 볼 수 있다. 그러나 direct I/O가 page cache를 우회하면 invalidation/synchronization이 필요해진다. `같은 file`이라는 논리 identity가 여러 caching path에서 자동으로 완벽한 instantaneous coherence를 보장한다고 가정하지 않는다.

## CHAPTER 04 · readahead는 sequential pattern을 예측해 요청 전에 page를 가져온다

Kernel은 file offset이 연속적으로 증가하는 access를 관찰해 다음 page를 미리 읽을 수 있다. Readahead가 맞으면 application read가 storage completion을 기다리지 않고 cache hit로 진행한다. Random seek workload에서는 불필요한 page를 읽어 bandwidth와 cache를 낭비할 수 있다. Benchmark에서 한 번 sequential scan한 뒤 두 번째 run을 측정하면 readahead와 page cache가 모든 storage 비용을 숨길 수 있다. Access pattern과 readahead state를 결과 metadata로 기록한다.

## CHAPTER 05 · writeback은 dirty page 수·age·memory pressure와 storage queue를 조정한다

Dirty data를 너무 오래 memory에 쌓으면 crash-loss window와 memory pressure가 커지고, 너무 자주 작은 write로 flush하면 storage efficiency가 나빠진다. Kernel은 dirty threshold와 background writeback policy로 batching을 조절한다. Device가 느리거나 write amplification이 높아 dirty production rate를 못 따라가면 application writer가 throttling될 수 있다. `write가 갑자기 느려짐`은 application lock이 아니라 dirty limit에 걸린 writeback backpressure일 수 있다.

## CHAPTER 06 · fsync는 application→filesystem→device persistence chain의 요청이다

Fsync는 dirty file data와 필요한 metadata를 stable storage contract까지 밀어내도록 요청하지만 정확한 보장은 filesystem, device cache, hardware power-loss property에 의존한다. Application은 return error를 확인해야 하며 successful fsync와 directory-entry durability를 구분해야 한다. Target file을 rename해 atomic replace한 경우 directory fsync가 필요한 protocol도 있다. P13/P24의 crash ordering과 연결해 write→fsync→rename→dir fsync 순서를 실제 fault test로 검증한다.

## CHAPTER 07 · direct I/O는 `cache 없음`보다 page-cache data path를 우회하는 계약이다

O_DIRECT류 interface는 application buffer와 storage I/O를 더 직접 연결해 page cache copy와 pollution을 줄이려는 목적이 있지만 exact semantics와 alignment requirement는 filesystem/device/kernel에 따라 다를 수 있다. Direct I/O도 device controller cache, DMA mapping, CPU cache와 무관한 것은 아니다. `zero-copy`라고 일반화하지 않고 실제 copy path와 registered/pinned page를 확인한다. Database처럼 자체 buffer cache를 가진 application이 double caching을 줄이는 데 사용할 수 있다.

## CHAPTER 08 · direct I/O alignment는 buffer address·length·file offset 모두에 적용될 수 있다

Storage logical/physical block과 DMA constraint 때문에 O_DIRECT는 memory buffer alignment, request length, file offset을 특정 단위에 맞출 것을 요구할 수 있다. Misalignment가 EINVAL로 실패하거나 buffered fallback되는지는 platform/filesystem contract를 확인해야 한다. Aligned allocator와 block-size query를 사용하며 hard-coded 4096을 universal rule로 쓰지 않는다. Tail bytes가 block boundary에 맞지 않는 variable-length record는 bounce buffer나 buffered path가 필요할 수 있다.

## CHAPTER 09 · direct I/O buffer는 I/O completion 전까지 lifetime과 mutability가 제한된다

Async direct I/O가 application page를 DMA/source로 사용하는 동안 buffer를 free/reuse/modify하면 device가 잘못된 data를 읽거나 destination을 덮어쓸 수 있다. Submission 이후 completion까지 buffer ownership을 I/O operation이 가진다는 contract가 필요하다. Garbage-collected/moving runtime에서는 pinning 또는 native stable buffer가 필요할 수 있다. P25 async I/O와 P40 ownership을 storage buffer lifetime에 연결한다.

## CHAPTER 10 · direct I/O와 buffered I/O를 같은 file region에 섞으면 coherence 작업이 생긴다

Page cache에 old data가 남아 있는데 direct write가 device를 갱신하면 buffered reader가 stale page를 보는 문제가 생길 수 있다. Kernel/filesystem은 direct I/O 전후 page invalidation/flush를 수행할 수 있지만 race와 concurrent mmap access는 복잡하다. Application이 path를 섞어야 한다면 exclusive range ownership, fsync/invalidate, documented filesystem semantics를 사용한다. 하나의 file descriptor flag만 바꾸면 consistency가 자동 해결된다고 가정하지 않는다.

## CHAPTER 11 · mmap read는 file byte를 user address-space page로 demand-map한다

Mmap은 read syscall마다 user buffer copy를 요청하는 대신 file page를 virtual address range에 mapping한다. 첫 access에서 page fault가 나면 page cache/storage에서 page를 채우고 이후 load instruction으로 접근한다. Small random access에서 syscall overhead를 줄일 수 있지만 page fault와 TLB behavior가 performance를 지배할 수 있다. Mapping 전체가 즉시 RAM을 차지한다는 뜻이 아니며 resident page는 working set과 pressure에 따라 달라진다.

## CHAPTER 12 · mmap write는 ordinary store를 filesystem dirty page로 바꾼다

Writable shared mapping에 store하면 CPU가 memory page를 수정하고 kernel은 이를 dirty file page로 추적해 나중에 writeback할 수 있다. Application이 durable commit을 요구하면 msync/fsync 등 platform contract가 필요하다. Pointer store가 return했다고 persistent storage에 반영된 것은 아니다. Structured persistent data를 mmap으로 수정하면 field update ordering과 crash-consistency protocol을 application이 직접 설계해야 한다.

## CHAPTER 13 · MAP_PRIVATE는 file-backed initial data와 process-private modification을 분리한다

Private mapping은 file page를 읽을 수 있지만 write 시 COW private page를 만들어 underlying file에 변경을 반영하지 않는다. 따라서 `mmap했으니 파일 수정`이라는 가정은 mapping flag에 따라 틀린다. Large file parser가 private mapping을 사용하면 pointer-like access를 얻으면서 accidental write가 file corruption으로 이어지지 않을 수 있다. 그러나 COW write가 많으면 resident anonymous memory가 증가한다.

## CHAPTER 14 · mapped file truncation은 address가 남아 있어도 backing range를 없앨 수 있다

한 process가 file을 mmap한 동안 다른 process가 file을 shrink하면 mapping의 일부 virtual address가 더 이상 valid backing을 갖지 않을 수 있다. 이후 access가 SIGBUS류 fault로 이어질 수 있다. Mapping lifetime과 file-size lifetime을 별도 invariant로 관리해야 한다. Append/grow도 reader가 새 length를 어떻게 발견하는지 protocol이 필요하다. Mutable shared file을 raw mmap으로 협업할 때 size/epoch를 synchronization한다.

## CHAPTER 15 · mmap은 pointer lifetime 때문에 file replacement와 충돌한다

Atomic rename으로 file path가 새 inode를 가리키게 해도 이미 mmap된 process는 old file object mapping을 계속 사용할 수 있다. Path identity와 mapping identity가 분리된다. Configuration/index file을 replace한 뒤 모든 reader가 즉시 새 bytes를 본다고 가정하면 안 된다. Reader는 versioned file open+map lifecycle을 관리하고 old mapping release 후 new mapping으로 전환해야 한다.

## CHAPTER 16 · page fault batching과 sequential fault-around가 mmap scan 성능을 바꾼다

Sequential mmap access에서 매 page마다 독립 storage request를 내면 비효율적이므로 kernel은 fault-around/readahead로 주변 page를 미리 준비할 수 있다. Random sparse access는 이 예측이 맞지 않아 major fault가 많아질 수 있다. Read() sequential scan과 mmap scan의 성능 비교는 page-cache warmness와 fault count를 함께 봐야 한다. `mmap이 copy가 적으니 항상 빠름`이라는 결론은 틀리다.

## CHAPTER 17 · madvise/fadvise는 access-pattern hint이며 correctness를 바꾸면 안 된다

Application은 sequential/random/willneed/dontneed 같은 hint를 kernel에 제공해 readahead/reclaim policy를 돕는다. Hint가 무시되어도 program 결과는 같아야 한다. 잘못된 hint가 correctness 전제가 되면 portability가 깨진다. Large one-pass scan 후 cache pollution을 줄이기 위해 dontneed류 hint를 사용할 수 있지만 concurrent reader의 shared page-cache effect를 고려한다. 효과는 target kernel/filesystem에서 측정한다.

## CHAPTER 18 · direct I/O는 application-side buffer cache를 직접 설계하게 만든다

Database가 page cache를 우회하면 어떤 page를 memory에 유지·evict할지 자체 buffer manager가 책임진다. DB page size, dirty eviction, prefetch, checkpoint가 storage I/O pattern을 결정한다. 이중 cache를 줄이는 대신 OS의 일반 readahead/reclaim 기능을 직접 대체해야 한다. Memory budget을 DB cache와 process heap 사이에 명시적으로 배분한다. Direct I/O 선택은 syscall micro-optimization이 아니라 cache ownership 이전이다.

## CHAPTER 19 · buffered I/O의 page cache는 여러 process 사이 data를 공유하는 이점이 있다

같은 shared library/data file을 여러 process가 읽으면 page cache page를 공동 재사용해 physical memory와 disk I/O를 줄일 수 있다. 각 process가 private user-space cache를 만들면 동일 data가 중복된다. Direct I/O가 항상 memory-efficient한 것이 아니다. Workload가 cross-process sharing을 가지는지와 application cache hit ratio를 함께 평가한다.

## CHAPTER 20 · double buffering은 page cache와 application cache가 동일 bytes를 중복 보관하는 현상이다

Database/cache server가 자체 buffer pool에 100GB data를 두면서 OS page cache에도 같은 100GB가 남으면 host memory 효율이 나빠질 수 있다. 하지만 OS cache가 metadata/executable/other file과 함께 adaptive하게 memory를 사용하고 application cache가 fixed budget이라 trade-off가 복잡하다. RSS만으로 page-cache duplication을 계산하지 않고 cgroup/file cache stat과 application buffer metric을 비교한다.

## CHAPTER 21 · DAX는 block/page-cache 경로를 줄여 persistent byte-addressable memory를 mapping할 수 있다

Direct Access 계열은 filesystem page cache를 우회하고 storage/persistent-memory mapping을 CPU address space에 더 직접 연결할 수 있다. 그러나 ordinary DRAM mmap과 동일한 durability가 아니다. CPU cache에 있는 store가 persistence domain까지 도달하려면 architecture-specific flush/order가 필요할 수 있다. DAX availability와 guarantee는 filesystem/device/platform에 의존한다. Persistent-memory semantics를 일반 SSD mmap에 대입하지 않는다.

## CHAPTER 22 · direct access는 CPU cache coherence와 persistence ordering을 분리한다

다른 CPU가 같은 cache-coherent memory를 최신 값으로 볼 수 있다는 사실과 power failure 후 media에 남는다는 사실은 다른 ordering domain이다. Persistent memory는 cache line writeback, fence, platform persistence domain을 고려해야 한다. `memory barrier`와 `durability barrier`를 같은 것으로 취급하지 않는다. Log/transaction protocol은 visibility order와 persist order를 각각 증명해야 한다.

## CHAPTER 23 · FUA와 flush는 storage write-cache ordering을 표현하는 도구다

Device가 volatile write cache를 사용하면 command completion이 NAND/media persistence보다 앞설 수 있다. Flush command는 이전 write를 stable storage로 내리도록 요청하고 FUA는 특정 write의 persistence semantics를 강화할 수 있다. Filesystem/block layer가 이를 적절히 변환해 fsync contract를 구현한다. Application이 raw block/NVMe를 다루는 경우 command ordering과 power-loss protection을 직접 이해해야 한다.

## CHAPTER 24 · read-modify-write amplification은 unaligned/small direct write에서 커질 수 있다

Application write가 device physical block/page보다 작거나 alignment가 맞지 않으면 lower layer가 기존 block을 읽어 merge한 뒤 다시 쓰는 작업이 필요할 수 있다. Filesystem/journal/FTL까지 겹치면 write amplification이 더 커진다. Logical bytes written만 보고 storage wear와 bandwidth를 판단하지 않는다. Record/page size를 storage geometry와 workload update pattern에 맞춰 benchmark한다.

## CHAPTER 25 · scatter-gather I/O는 여러 user buffer를 하나의 logical operation으로 묶는다

readv/writev류 vectored I/O는 header와 body처럼 non-contiguous memory를 별도 memcpy로 합치지 않고 하나의 syscall로 제출할 수 있다. Syscall count와 temporary buffer allocation을 줄이지만 iovec count/total length validation이 필요하다. Async/DMA backend가 scatter-gather list를 얼마나 효율적으로 처리하는지 target별로 다르다. Very many tiny segments는 descriptor processing overhead를 키울 수 있다.

## CHAPTER 26 · sendfile/splice류 zero-copy path는 page ownership을 subsystem 사이 전달한다

File→socket transfer에서 user-space read buffer를 거치지 않고 page/cache reference를 network stack에 전달하면 copy와 context transition을 줄일 수 있다. 하지만 TLS encryption, content transform, checksumming이 user-space bytes를 필요로 하면 path가 달라질 수 있다. `zero copy`는 bytes가 절대 복사되지 않는다는 보장보다 특정 user copy를 제거하는 목표로 이해한다. Buffer lifetime과 backpressure는 여전히 존재한다.

## CHAPTER 27 · io_uring registered buffers는 direct/async path의 반복 pin·lookup 비용을 줄인다

Buffer를 미리 registration하면 submission마다 user page lookup/pin을 반복하지 않고 kernel이 stable mapping을 재사용할 수 있다. 대신 registered memory가 long-lived pinned/resource budget을 차지하고 buffer pool ownership이 복잡해진다. Fixed buffer index reuse는 completion 전 금지해야 한다. P25 async engine과 P51 memlock/resource limit을 함께 검토한다.

## CHAPTER 28 · I/O path benchmark는 cache 상태·dataset size·sync policy를 고정해야 한다

Buffered read benchmark가 RAM보다 작은 file을 반복 읽으면 storage가 아니라 memcpy/page-cache bandwidth를 측정한다. Direct I/O benchmark는 alignment와 queue depth가 다르면 비교가 불공정하다. Write benchmark가 fsync를 제외하면 durability workload를 대변하지 않는다. Cold buffered, warm buffered, direct sequential/random, mmap fault, durable write를 별도 scenario로 나누고 actual device bytes/latency를 수집한다.

## CHAPTER 29 · path 선택은 latency 하나가 아니라 memory ownership과 failure semantics를 바꾼다

Buffered I/O는 OS cache/readahead/writeback을 활용하고 direct I/O는 application이 cache/buffer ownership을 더 많이 가진다. Mmap은 pointer access와 VM fault semantics를, DAX는 persistence-order 문제를 추가한다. 같은 API workload를 네 방식으로 바꾸는 것은 implementation detail 변경이 아니라 resource/recovery model 변경이다. Code review에서 buffer lifetime, crash durability, cache coherence, memory budget을 함께 재검토한다.

## CHAPTER 30 · I/O contract는 cache domain·completion·durability·coherence를 따로 정의한다

각 path에서 operation completion이 `user buffer 복사 완료`, `page cache dirty`, `device command 완료`, `persistent media 도달` 중 어디까지 의미하는지 문서화한다. 동일 file에 buffered/direct/mmap path를 섞을 때 invalidate/flush/exclusive-range rule을 정한다. Async buffer는 completion까지 lifetime을 보장하고 storage error는 sync 단계까지 확인한다. 성능 최적화는 representative cache state와 durability requirement를 유지한 end-to-end 측정으로만 승격한다.