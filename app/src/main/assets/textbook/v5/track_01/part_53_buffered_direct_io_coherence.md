# PART 53 · Buffered, Direct and Memory-Mapped I/O — page cache, alignment, coherence

파일 I/O API는 같은 storage를 서로 다른 경로로 본다. Buffered read/write는 page cache를 거치고, direct I/O는 cache를 우회하려 하며, `mmap`은 file page를 virtual memory처럼 접근하게 한다. 이 경로들을 섞으면 alignment, coherency, persistence ordering, buffer lifetime이 복잡해진다. 이 PART는 syscall 이름보다 **data가 application buffer에서 page cache와 block device를 거쳐 durable state가 되는 경로**를 추적한다.

---

## CHAPTER 01 · buffered read는 file data를 page cache와 사용자 buffer 사이로 전달한다

일반 `read()`는 요청한 file offset의 page가 cache에 있으면 그 data를 사용자 buffer로 복사하고, 없으면 storage I/O를 발생시켜 page cache를 채운 뒤 반환한다. 따라서 두 번째 read가 빠른 것은 device가 빨라진 것이 아니라 cache hit일 수 있다. File benchmark에서 cold/warm 상태를 분리해야 하는 이유다.

Sequential access에서는 kernel readahead가 뒤 page를 미리 가져와 syscall이 직접 요청하지 않은 I/O도 발생시킬 수 있다. Random workload에서는 같은 정책이 낭비가 될 수 있다.

Read latency를 분석할 때 syscall 시간만 보지 말고 major page miss, cache hit ratio, underlying device latency를 함께 본다. Page cache가 storage behavior를 가리는 중간 계층이다.

## CHAPTER 02 · buffered write는 먼저 dirty page를 만들고 실제 device write를 뒤로 미룰 수 있다

`write()`가 성공하면 보통 data가 kernel page cache에 복사되었다는 뜻이지 stable media에 기록됐다는 뜻은 아니다. Dirty page는 background writeback이나 explicit sync에 의해 나중에 device로 내려간다. 그래서 write syscall latency가 낮아도 storage backlog가 쌓일 수 있다.

Dirty data가 너무 많아지면 writer가 writeback을 기다리며 throttling될 수 있다. Burst 초반은 빠르고 뒤쪽에서 latency가 급증하는 패턴이 나타난다.

Application이 durability를 요구하면 `fsync`/`fdatasync` 같은 별도 경계를 사용해야 한다. “write success”와 “commit success”를 API에서 혼동하지 않는다.

## CHAPTER 03 · page cache coherence는 여러 access path가 같은 file page를 본다는 규칙이다

Buffered I/O와 `mmap`이 같은 file을 접근하면 kernel은 가능한 한 동일 page cache page를 통해 data를 일관되게 보이게 한다. 하지만 direct I/O나 device DMA가 섞이면 coherency 조건이 더 복잡해진다. Filesystem과 kernel 버전에 따라 synchronization requirement를 확인해야 한다.

같은 process 안에서도 stdio user buffer, application cache, page cache가 겹칠 수 있다. 한 layer를 flush했다고 다른 layer까지 자동 commit되는 것은 아니다.

Coherence bug는 stale read처럼 보이기 쉽다. 어느 cache에 최신 copy가 있는지 경로를 그려서 조사한다.

## CHAPTER 04 · readahead는 sequential access를 예측해 future I/O를 선행한다

Kernel은 연속된 page를 읽는 pattern을 감지하면 뒤쪽 page를 미리 요청해 application이 기다리는 시간을 줄인다. Device queue를 활용해 throughput을 높일 수 있지만 실제로 읽지 않을 page까지 가져오면 bandwidth와 cache를 낭비한다.

Database처럼 자체 buffer manager가 access pattern을 알고 있다면 readahead가 중복되거나 eviction을 유발할 수 있다. 반대로 large sequential scan에는 큰 이득이 있다.

Benchmark에서 readahead를 끄고 켠 결과를 비교하면 device raw latency와 kernel prediction 효과를 분리할 수 있다. Production tuning은 실제 scan 길이 분포를 기반으로 한다.

## CHAPTER 05 · writeback은 dirty page를 device request로 변환하는 비동기 단계다

Dirty page는 일정 threshold, age, memory pressure에 따라 writeback된다. Background worker가 처리하는 동안 application은 계속 write할 수 있지만 dirty budget을 넘으면 foreground thread가 직접 기다릴 수 있다. 이 지점에서 tail latency가 급증한다.

Filesystem은 여러 page를 모아 큰 I/O로 만들고 metadata ordering을 관리할 수 있다. 따라서 application write 순서와 block device에 도달하는 순서는 동일하지 않을 수 있다.

Writeback queue, dirty bytes, device utilization을 같은 timeline에 보면 “write syscall이 갑자기 느려졌다”는 현상을 설명할 수 있다.

## CHAPTER 06 · fsync는 file의 durability 경계를 요청하지만 storage stack 전체 보장을 확인해야 한다

`fsync`는 해당 file의 변경을 stable storage에 반영하도록 요구하는 핵심 API다. 하지만 정확히 어떤 metadata가 포함되는지, directory entry의 durability는 별도 directory fsync가 필요한지 filesystem semantics를 확인해야 한다.

Device write cache가 volatile하다면 filesystem은 flush/FUA 같은 command를 사용해 ordering과 persistence를 보장해야 한다. Hardware가 명령을 거짓으로 보고하면 software만으로 해결할 수 없다.

Crash test는 fsync return 이후 power-loss model에서 data가 남는지 확인해야 한다. 호출했다는 사실만으로 durability gate를 PASS라고 말하지 않는다.

## CHAPTER 07 · direct I/O는 page cache 우회를 목표로 하지만 ‘device에 즉시 durable’과 같은 뜻이 아니다

Direct I/O는 application buffer와 storage 사이의 data copy/cache pollution을 줄이기 위해 page cache를 우회하는 경로를 제공할 수 있다. Database buffer manager처럼 자체 cache를 가진 system에 유용하다. 하지만 device controller cache와 filesystem metadata는 여전히 존재할 수 있다.

Direct라는 이름 때문에 sync write와 혼동하면 안 된다. Completion은 I/O transfer 완료를 의미할 수 있어도 persistence는 flush policy에 달려 있다.

효과는 workload와 device에 따라 다르므로 buffered I/O보다 항상 빠르다는 가정은 피한다. Small unaligned request에서는 오히려 비효율적일 수 있다.

## CHAPTER 08 · direct I/O alignment는 buffer·offset·length에 제약을 줄 수 있다

Block/page boundary에 맞지 않는 direct I/O는 error가 나거나 내부 fallback/추가 처리가 필요할 수 있다. 요구 alignment는 filesystem, kernel, device에 따라 달라질 수 있다. Hard-coded 4096만 믿지 말고 실제 interface에서 확인한다.

Allocator는 aligned buffer를 제공해야 하고 slice offset을 만들 때 alignment가 깨지지 않게 한다. Async I/O에서는 buffer lifetime까지 completion 후까지 유지해야 한다.

Test는 aligned success path뿐 아니라 offset+1, length 비배수 같은 boundary를 넣어 error handling을 검증한다. Silent buffered fallback이 성능 측정을 왜곡하지 않는지도 확인한다.

## CHAPTER 09 · async/direct I/O의 buffer lifetime은 syscall return보다 길 수 있다

Kernel이나 device가 application buffer를 직접 참조하는 동안 그 memory를 free하거나 reuse하면 data corruption이 생긴다. Submission과 completion이 분리된 API에서는 buffer ownership이 completion까지 kernel/operation에 묶인다.

Registered buffer는 lifetime을 더 길게 만들어 mapping 비용을 줄일 수 있지만 해제 순서가 복잡해진다. Cancellation이 곧 DMA 중단 완료를 의미하는지 API semantics를 확인한다.

Ownership을 type/state로 표현하고 completion 전 reuse를 막는다. Load test에서 rare corruption이 보이면 buffer pool의 generation과 in-flight count를 확인한다.

## CHAPTER 10 · buffered I/O와 direct I/O를 같은 file에 섞으면 coherency를 명시적으로 다뤄야 한다

한 path가 page cache를 수정하고 다른 path가 cache를 우회하면 어느 data가 최신인지 동기화가 필요하다. Kernel이 필요한 invalidation을 제공하더라도 alignment와 overlap 조건에 따라 비용과 semantics가 복잡하다.

Database가 direct data file과 buffered metadata file을 분리하는 식으로 access mode를 명확히 나누면 reasoning이 쉬워진다. 같은 byte range를 여러 mode로 자주 교차 접근하지 않는 것이 좋다.

Mixed-path test에서는 write→read 순서를 양방향으로 실행해 stale data가 없는지 확인한다. Performance만 보고 correctness를 가정하지 않는다.

## CHAPTER 11 · mmap read는 page fault를 통해 file page를 address space에 가져온다

`mmap`은 read syscall을 반복하는 대신 file offset을 virtual address로 매핑한다. 처음 접근한 page가 resident하지 않으면 page fault가 발생해 storage I/O를 기다릴 수 있다. Source code에 syscall이 없어도 latency가 memory load instruction에서 발생할 수 있다.

Sequential access에서는 fault readahead가 작동할 수 있고, random access에서는 많은 minor/major fault가 발생할 수 있다. `mmap이 zero-copy라 항상 빠르다`는 단순 결론은 위험하다.

Profile에서 page fault와 blocked time을 함께 본다. CPU stack만 보면 load instruction이 느린 이유를 알기 어렵다.

## CHAPTER 12 · mmap write는 memory store를 dirty file page로 바꾸지만 persistence는 별도다

Shared mapping에 write하면 page가 dirty해지고 나중에 writeback된다. Store instruction 완료가 file durability를 뜻하지 않는다. `msync`, `fsync`, filesystem semantics를 통해 persistence 경계를 설계해야 한다.

Crash-consistent data structure를 mmap 위에 직접 만들면 CPU memory ordering과 storage persistence ordering을 동시에 고려해야 할 수 있다. 단순 pointer update를 transaction처럼 사용하면 torn state가 생길 수 있다.

Persistent format은 checksum/generation/log 같은 복구 구조를 둔다. Memory mapping은 I/O API를 바꿀 뿐 atomicity를 자동 제공하지 않는다.

## CHAPTER 13 · MAP_PRIVATE는 copy-on-write view이지 file update channel이 아니다

Private mapping에서 write하면 process가 private COW page를 가지며 원본 file에는 반영되지 않는다. 다른 process와 공유하려는 목적에 쓰면 기대와 다른 결과가 나온다.

Fork 후 private mapping은 parent/child가 초기 page를 공유하다 write 시 분리될 수 있다. Memory usage는 mapping size보다 실제 dirty private page 수에 따라 증가한다.

API 선택 시 `read-only view`, `shared modification`, `private workspace`를 구분한다. Flag 이름보다 원하는 visibility semantics를 먼저 적는다.

## CHAPTER 14 · mmap된 file을 truncate하면 기존 virtual address의 유효성이 깨질 수 있다

다른 thread/process가 file을 줄이면 mapping의 일부 page가 더 이상 backing storage를 갖지 않을 수 있고 접근 시 signal/fault가 발생할 수 있다. Mapping pointer가 non-null이라는 이유로 계속 안전한 것이 아니다.

Concurrent truncate가 가능한 file format은 lock/version protocol을 두거나 immutable generation을 사용한다. Reader가 mapping lifetime 동안 file size가 안정적이라는 보장을 필요로 하는지 문서화한다.

Fault test에서 truncate와 reader access를 겹쳐 rare crash를 재현한다. Filesystem API error만 처리하고 memory fault를 놓치지 않는다.

## CHAPTER 15 · mapping identity는 file descriptor가 닫혀도 mapping lifetime과 별개일 수 있다

`mmap`이 성공한 뒤 원래 fd를 닫아도 mapping은 유지되는 platform semantics가 일반적이다. 반대로 file name이 rename/unlink되어도 mapping이 참조하는 underlying object는 계속 존재할 수 있다. Path identity와 inode/object identity를 분리해야 한다.

Hot reload에서 file path를 새 generation으로 교체해도 기존 reader의 mapping은 old object를 볼 수 있다. 이는 안전한 immutable generation 전략으로 활용할 수 있다.

Diagnostic에서는 path 문자열보다 device/inode 또는 generation id를 기록해 실제 object를 구분한다.

## CHAPTER 16 · fault readahead는 mmap access pattern을 추측해 page를 미리 가져온다

Page fault handler는 한 page만 읽는 대신 주변 page를 함께 가져와 sequential access latency를 줄일 수 있다. 그래서 첫 fault는 비싸고 이어지는 access는 빠른 pattern이 나타난다.

Random index lookup에서는 prefetched page가 사용되지 않아 cache pollution이 생길 수 있다. `madvise` 같은 hint가 kernel policy에 도움을 줄 수 있지만 hint는 correctness 보장이 아니다.

Benchmark는 access stride와 dataset size를 바꿔 readahead 영향 범위를 확인한다. 한 scan pattern으로 모든 mmap workload를 대표하지 않는다.

## CHAPTER 17 · I/O hint는 kernel에 의도를 전달하지만 application correctness를 맡기지 않는다

Sequential/random, will-need/don’t-need 같은 hint는 cache와 readahead 정책을 조절하는 데 도움을 줄 수 있다. Kernel은 hint를 무시하거나 상황에 맞게 해석할 수 있으므로 기능의 correctness가 hint 적용 여부에 의존하면 안 된다.

Long scan 후 cache pollution을 줄이기 위해 page를 버리도록 hint할 수 있지만 다른 process와 공유하는 page cache 영향도 고려한다.

Tuning은 실제 cache miss와 I/O량을 측정해 효과를 확인한다. Hint를 넣었다는 사실만으로 최적화가 됐다고 말하지 않는다.

## CHAPTER 18 · application cache와 page cache가 겹치면 double caching이 될 수 있다

Database나 media server가 자체 buffer cache를 유지하면서 buffered I/O를 사용하면 같은 data가 application memory와 kernel page cache에 두 번 존재할 수 있다. 이중 cache는 memory pressure를 높이지만 kernel readahead와 sharing 이득도 제공한다.

Direct I/O로 page cache를 우회하면 application이 replacement와 prefetch 책임을 더 많이 가져간다. 어느 쪽이 낫다는 보편 규칙은 없다.

RSS와 page cache를 합친 system memory 관점에서 cache 효율을 본다. Process heap만 최적화해 host가 reclaim pressure에 빠지는 상황을 피한다.

## CHAPTER 19 · page cache는 process 간에 file data를 공유하는 암묵적 공동 cache다

여러 process가 같은 file을 읽으면 physical page가 page cache에서 공유될 수 있어 전체 memory와 I/O를 줄인다. 각 process RSS 합을 단순 더하면 실제 physical 사용량을 과대평가할 수 있다.

반대로 한 process의 large scan이 shared cache를 밀어 다른 process의 hit rate를 낮출 수 있다. Multi-tenant host에서 cache는 공유 자원이 된다.

Performance incident에서 process 단위만 보지 말고 host page cache와 workload interaction을 본다. Restart 후 느려지는 cold-cache effect도 여기서 나온다.

## CHAPTER 20 · double buffering은 copy 횟수뿐 아니라 backpressure 위치를 바꾼다

Application buffer→page cache→device처럼 여러 buffer가 있으면 producer가 downstream보다 빨라도 잠시 흡수할 수 있다. 하지만 각 buffer가 꽉 차는 시점이 다르고 총 memory가 커진다. Queue가 많을수록 latency가 숨겨진 채 backlog가 누적될 수 있다.

Zero-copy 최적화는 copy cost를 줄이지만 buffer ownership과 lifetime 제약을 강화한다. Copy 제거 자체가 목표가 아니라 CPU·memory·latency의 전체 비용을 본다.

Metrics에는 각 queue/buffer occupancy를 둔다. 가장 앞단 queue만 보면 downstream congestion을 늦게 발견할 수 있다.

## CHAPTER 21 · DAX는 page cache를 우회해 persistent memory를 직접 매핑하는 다른 persistence model을 만든다

Direct Access 계열은 storage를 memory-like address로 접근해 page cache와 block I/O 경로를 줄일 수 있다. 그러나 CPU cache가 volatile할 수 있어 store 완료와 persistence가 동일하지 않을 수 있다. Platform이 제공하는 flush와 ordering primitive를 따라야 한다.

Crash consistency를 pointer update만으로 해결할 수 없다는 점은 동일하다. Cache line tear, ordering, metadata atomicity를 고려한 format이 필요하다.

DAX 여부에 따라 기존 buffered I/O assumption이 깨질 수 있으므로 deployment 환경을 명시적으로 탐지한다.

## CHAPTER 22 · persistence ordering은 여러 write 중 crash 후 어떤 순서가 보장되는지를 정의한다

Application이 data block을 쓰고 metadata pointer를 갱신할 때 pointer가 먼저 durable해지면 crash 후 미완성 data를 가리킬 수 있다. `write()` 호출 순서만으로 device persistence 순서를 보장할 수 없으므로 sync/flush protocol이 필요하다.

Filesystem journaling이 모든 application-level dependency를 알아서 해결해 주는 것은 아니다. Application format의 commit point를 설계해야 한다.

Crash injection으로 각 ordering boundary를 검증한다. Recovery가 old 또는 new state 중 하나만 보게 하는 것이 목표다.

## CHAPTER 23 · FUA와 flush는 volatile device cache와 ordering을 제어하는 block-layer primitive다

Storage device는 성능을 위해 write를 volatile cache에 받아 completion을 줄 수 있다. Flush는 앞선 write를 stable media로 밀어내고, FUA 계열은 특정 write의 persistence 성질을 강화한다. Filesystem이 이를 조합해 fsync semantics를 구현한다.

Application은 일반적으로 raw command보다 filesystem API를 사용하지만, durability 문제를 이해하려면 아래 계층의 의미를 알아야 한다. Device가 cache policy를 잘못 보고하면 예상이 깨질 수 있다.

Power-loss validation은 실제 hardware/virtualization stack이 flush를 올바르게 전달하는지 확인하는 최종 증거다.

## CHAPTER 24 · write amplification은 application byte보다 더 많은 physical write를 만들 수 있다

작은 random update가 filesystem metadata, journal, storage FTL garbage collection과 결합하면 physical media에 훨씬 많은 bytes가 기록될 수 있다. Application throughput만 보면 endurance와 device bandwidth 비용을 놓친다.

Batching, append-only layout, larger sequential write가 amplification을 줄일 수 있지만 recovery와 latency trade-off가 있다.

Device telemetry가 가능하면 host writes와 media writes를 비교한다. Database compaction과 filesystem writeback이 겹치는 시간대도 관찰한다.

## CHAPTER 25 · vectored I/O는 여러 buffer를 한 syscall의 logical operation으로 묶는다

`readv/writev` 계열은 흩어진 buffer를 하나의 I/O vector로 전달해 syscall 수와 intermediate copy를 줄일 수 있다. Protocol header와 payload가 별도 buffer에 있을 때 유용하다.

하지만 partial write가 vector 중간에서 끝날 수 있어 남은 iovec를 정확히 조정해야 한다. 단순히 처음부터 다시 보내면 이미 기록한 data를 중복할 수 있다.

Async API와 결합하면 각 vector element의 lifetime도 completion까지 유지해야 한다. Scatter/gather는 성능 최적화와 ownership 복잡성을 함께 가져온다.

## CHAPTER 26 · zero-copy는 data 이동을 줄이는 대신 page lifetime과 mutation 규칙을 강화한다

`sendfile`, splice, memory sharing 같은 경로는 userspace copy를 줄여 CPU와 memory bandwidth를 절약할 수 있다. 대신 kernel이 source page를 사용하는 동안 application이 content를 변경하거나 해제하면 안 되는 제약이 생길 수 있다.

Small payload에서는 setup overhead가 copy cost보다 클 수 있다. Large sequential transfer에서 이득이 더 잘 나타날 수 있다.

Benchmark는 CPU cycles, throughput, latency를 함께 본다. “copy 횟수 0” 자체를 사용자 가치와 동일시하지 않는다.

## CHAPTER 27 · registered buffer는 반복 I/O의 mapping 비용을 줄이지만 장기 pinned resource가 된다

Async engine이 buffer를 미리 등록하면 매 operation마다 address validation/pinning 비용을 줄일 수 있다. 하지만 등록된 memory는 일반 allocation보다 이동/reclaim이 어렵고 별도 limit을 소비할 수 있다.

Buffer pool 크기를 queue depth와 맞추고 unused registration을 해제한다. Registration failure에서 일반 buffer path로 fallback할지 명확히 정한다.

Metrics에 registered bytes와 in-flight usage를 둔다. Memory leak처럼 보이는 pinned region을 설명하는 데 필요하다.

## CHAPTER 28 · I/O benchmark는 cache state·queue depth·alignment를 고정해야 비교 가능하다

Sequential 1 MiB buffered read와 4 KiB random direct read는 전혀 다른 workload다. Block size, access pattern, read/write ratio, sync policy, queue depth를 명시한다. Cold device 성능을 재려면 page cache와 device cache의 상태도 고려한다.

Filesystem과 raw device benchmark를 같은 것으로 비교하지 않는다. Metadata, journaling, readahead가 추가된다.

Latency distribution과 throughput을 동시에 보고 saturation 지점을 찾는다. 단일 최고 MB/s만으로 application 성능을 예측하지 않는다.

## CHAPTER 29 · path semantics는 file 이름보다 open된 object와 generation을 구분해야 한다

Rename/unlink는 directory namespace를 바꾸지만 이미 open된 fd와 mapping은 underlying file object를 계속 참조할 수 있다. Hot-swap에서 writer가 새 file을 rename해도 old reader는 old generation을 안전하게 볼 수 있다.

반대로 path를 매번 reopen하는 reader는 어느 순간 새 generation으로 전환된다. 같은 application 안에서도 object identity가 달라질 수 있다.

로그에 path만 남기지 말고 generation/hash를 기록하면 “같은 파일 이름인데 내용이 다르다”는 사건을 설명하기 쉽다.

## CHAPTER 30 · I/O contract는 visibility와 durability를 분리해 access path마다 정의한다

Buffered I/O, direct I/O, mmap은 data를 보는 방식과 cache interaction이 다르다. 하지만 모두에서 핵심 질문은 같다. 다른 reader에게 언제 보이는가, crash 후 언제 남는가, in-flight buffer는 누가 소유하는가, resource 상한은 무엇인가.

Performance 최적화로 path를 바꾸기 전에 이 correctness contract를 먼저 맞춘다. Page cache를 우회해 latency가 줄어도 durability가 깨지면 성공이 아니다.

최종 검증은 representative I/O benchmark와 crash/coherency test를 조합한다. Cache hit 숫자와 MB/s만으로는 storage path가 안전하다는 것을 증명할 수 없다.
