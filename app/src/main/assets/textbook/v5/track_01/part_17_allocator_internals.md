# PART 17 · allocator 내부 — page allocator에서 object bin까지

`malloc/new`는 memory를 만드는 함수가 아니다. allocator는 이미 확보한 address range와 page를 **어떤 크기로 나누고, 어떤 thread에 배분하며, 언제 합치고, 언제 OS에 반환할지** 결정한다. latency, fragmentation, locality, security가 이 정책에서 동시에 나온다.

---

## CHAPTER 01 · physical page allocator와 object allocator는 다른 문제를 푼다

kernel page allocator는 physical page/frame 단위의 큰 memory resource를 관리하고 user/kernel object allocator는 그 page를 더 작은 object로 나눈다. 작은 object 하나를 만들 때마다 page allocator를 호출하면 metadata와 synchronization 비용이 너무 크다.

따라서 allocation path를 `physical page 확보 → arena/slab에 page 공급 → size class/free block 배분 → object 반환`으로 분해한다. 장애를 볼 때 어느 계층에서 실패했는지 구분한다. free object가 많아도 해당 allocator가 OS page를 더 확보하지 못하면 allocation은 실패할 수 있다.

---

## CHAPTER 02 · buddy allocator는 power-of-two block 분할과 병합으로 contiguous page를 관리한다

buddy system은 큰 block을 절반씩 쪼개 원하는 order를 만들고, free 시 주소 관계로 buddy block을 찾아 같은 order끼리 병합할 수 있다. metadata와 merge가 단순하지만 요청 크기를 power-of-two 단위로 맞추면서 internal fragmentation이 생긴다.

고-order allocation은 total free page가 충분해도 contiguous block이 없으면 실패할 수 있다. order별 free list, compaction, pinned page를 함께 본다. `free memory가 2GB인데 allocation이 왜 실패했나`는 total byte가 아니라 required order가 핵심일 수 있다.

---

## CHAPTER 03 · zone은 모든 physical page가 동일하게 사용 가능하다는 가정을 깨뜨린다

architecture/device 제약 때문에 DMA 가능한 address range, normal memory 등 page zone이 나뉠 수 있다. allocation flag는 어느 zone과 reclaim policy를 사용할지 결정한다.

잘못된 allocation context에서 scarce low-address/DMA zone을 소비하면 다른 driver가 실패할 수 있다. kernel memory debugging에서는 total free만 보지 않고 zone별 watermark와 reserve를 확인한다. resource class가 다르면 같은 page count라도 대체 가능하지 않다.

---

## CHAPTER 04 · per-CPU page cache는 global lock contention을 줄인다

모든 small page allocation이 global buddy structure를 잠그면 multi-core에서 lock contention이 커진다. per-CPU free list는 자주 쓰는 page를 local CPU에서 빠르게 allocate/free하고 batch 단위로 global allocator와 교환한다.

local cache가 너무 크면 한 CPU에 free page가 쌓여 다른 CPU/zone에서 pressure가 보일 수 있다. balance watermark와 drain mechanism이 필요하다. CPU hotplug에서도 per-CPU ownership을 정리해야 한다.

---

## CHAPTER 05 · slab allocator는 동일 크기 kernel object를 page 위에 캐시한다

inode, dentry, task structure처럼 반복 생성되는 fixed-layout object는 slab cache로 관리하면 initialization/layout 정보를 재사용하고 fragmentation을 줄일 수 있다. slab은 여러 object slot과 free-state metadata를 가진다.

object cache는 constructor/destructor와 debug poison/redzone을 적용할 수 있다. 특정 cache의 object size, slab occupancy, partial slab 수를 보면 kernel memory leak과 fragmentation을 구분할 수 있다. Linux 최신 slab 문서가 `struct slab`/folio relation을 별도로 다루는 이유도 이 object/page 계층 분리 때문이다. citeturn729512search0

---

## CHAPTER 06 · partial/full/empty slab 상태가 reclaimability를 결정한다

slab에 일부 object만 live하면 해당 page는 다른 용도로 반환하기 어렵다. live object 몇 개가 많은 slab을 붙잡으면 total used bytes는 작아도 page footprint가 커질 수 있다.

object lifetime을 비슷한 cache로 묶거나 compaction 가능한 allocator를 사용하면 fragmentation을 줄일 수 있다. slab occupancy histogram과 object age를 본다. `leak 없음`과 `memory efficiency 좋음`은 같은 판정이 아니다.

---

## CHAPTER 07 · kmalloc과 virtually contiguous allocation은 contiguous 의미가 다르다

kernel API는 physically contiguous page가 필요한 allocation과 virtual address만 contiguous하면 되는 allocation을 구분한다. 큰 physically contiguous allocation은 fragmentation에 민감하고, virtual-contiguous mapping은 page table overhead와 TLB pressure를 추가한다.

DMA buffer, large table, temporary workspace가 어떤 contiguity를 실제로 요구하는지 확인한다. 요구하지 않는 physical contiguity를 사용하면 long-uptime failure가 생기고, 너무 많은 virtual mapping은 mapping metadata와 shootdown 비용을 늘린다.

---

## CHAPTER 08 · user-space allocator는 virtual memory region을 자체 block graph로 관리한다

`malloc` 구현은 brk/sbrk 또는 mmap 계열로 큰 region을 확보한 뒤 block header와 free structure를 관리할 수 있다. 구체 구현은 allocator마다 다르지만 공통 질문은 `free block을 어떻게 찾는가, split/coalesce는 언제 하는가, OS 반환은 언제 하는가`다.

application object allocation latency는 syscall latency와 직접 같지 않다. fast path는 thread/local cache에서 pointer pop 하나로 끝날 수 있고 slow path에서만 arena growth와 system call이 발생한다. profile은 fast/slow path를 구분한다.

---

## CHAPTER 09 · free-list placement policy는 search cost와 fragmentation을 교환한다

first-fit, best-fit, segregated free list 같은 전략은 요청에 맞는 block을 찾는 비용과 남는 fragment 모양이 다르다. best-fit이 항상 fragmentation을 최소화하는 것도 아니다. 작은 unusable fragment가 많이 남을 수 있다.

실전 allocator는 exact theoretical policy 하나보다 size class와 bin, locality, coalescing을 조합한다. workload의 allocation size distribution이 allocator 설계의 입력이다. 평균 object size보다 histogram과 lifetime correlation을 본다.

---

## CHAPTER 10 · size class는 search를 줄이지만 rounding waste를 만든다

요청 크기를 미리 정한 size class로 올림하면 해당 class의 free slot을 빠르게 배분할 수 있다. 대신 `requested bytes < allocated bytes` 차이가 internal fragmentation이 된다.

class 간격을 촘촘하게 하면 waste는 줄지만 bin metadata와 management cost가 늘고, 너무 넓으면 memory waste가 커진다. allocator stats에서 requested/allocated/resident를 구분해 class policy의 실제 비용을 본다.

---

## CHAPTER 11 · split은 큰 free block을 작은 allocation과 remainder로 나눈다

큰 block을 요청보다 훨씬 크게 그대로 주면 waste가 크므로 allocator는 tail을 split해 free list로 돌려놓을 수 있다. 그러나 remainder가 metadata/align을 만족하지 못할 만큼 작다면 split하지 않는 것이 낫다.

split threshold와 minimum block size는 fragmentation 패턴에 영향을 준다. 반복되는 `large→small` allocation이 free space를 잘게 쪼개는 workload에서는 segregated class/arena 구조가 더 안정적일 수 있다.

---

## CHAPTER 12 · coalescing은 인접 free block을 합쳐 future large allocation을 준비한다

free block 양쪽의 neighbor가 free인지 빠르게 알려면 boundary tag나 side metadata가 필요하다. 즉시 coalescing은 fragmentation을 줄일 수 있지만 free path cost가 증가하고, deferred coalescing은 fast free 대신 later scan 비용을 낸다.

thread-local cache가 block을 보유하면 central arena 관점에서는 아직 free가 아니어서 coalescing이 지연될 수 있다. application이 idle해도 RSS가 바로 줄지 않는 이유와 연결된다.

---

## CHAPTER 13 · arena는 lock contention을 줄이는 대신 memory duplication을 만든다

multi-thread allocator는 여러 arena를 두어 서로 다른 thread가 independent metadata를 잠그게 할 수 있다. contention은 줄지만 free memory가 arena 사이에 분산되어 한 arena의 large allocation에 다른 arena의 free block을 바로 쓰지 못할 수 있다.

thread 수가 급증하면 arena 수도 늘어 resident footprint가 커질 수 있다. service가 thread-per-request model에서 pool model로 바뀌었을 때 allocator RSS 특성이 달라질 수 있다. arena count와 active thread topology를 함께 본다.

---

## CHAPTER 14 · thread cache는 allocation fast path와 memory retention을 교환한다

thread-local cache/tcache는 small object를 lock 없이 allocate/free해 latency를 줄인다. 하지만 종료하지 않는 thread가 많은 free object를 local cache에 보유하면 global allocator가 memory를 재사용하거나 OS에 반환하기 어렵다.

cache flush threshold와 thread lifetime을 workload에 맞춘다. burst workload 뒤 RSS가 내려오지 않는다고 즉시 leak으로 판정하지 않고 tcache/arena retained memory를 측정한다.

---

## CHAPTER 15 · remote free는 allocation thread와 free thread가 다른 경우 locality를 깨뜨린다

producer thread가 allocate한 object를 consumer thread가 free하면 owner arena/thread cache로 반환하는 과정에 cross-thread queue와 synchronization이 필요할 수 있다. high-rate remote free는 contention과 cache-line transfer를 만든다.

object ownership architecture가 allocator 성능에도 영향을 준다. per-thread queue에서 allocate/free를 같은 worker가 수행하게 하거나 batch remote-free를 사용할 수 있다. application message passing과 memory ownership을 함께 설계한다.

---

## CHAPTER 16 · large allocation은 small-object path와 별도 정책을 사용하는 경우가 많다

매우 큰 block을 arena 내부에서 carve하면 주변 free space가 fragment될 수 있으므로 allocator는 direct mmap 같은 별도 path를 사용할 수 있다. free 시 mapping을 즉시 OS에 반환할 수 있지만 mmap/munmap과 page-table/shootdown 비용이 늘어난다.

large threshold는 allocation size distribution과 lifetime에 따라 tuning된다. 이미지/압축 buffer처럼 수 MB temporary allocation이 반복되면 direct mapping churn이 latency를 만들 수 있다. buffer reuse/pool이 더 적합한지 측정한다.

---

## CHAPTER 17 · alignment는 vector/DMA/ABI 요구를 만족시키지만 padding을 증가시킨다

특정 object가 cache line, SIMD, page boundary에 맞춰야 하면 allocator가 더 큰 region에서 aligned address를 선택하고 앞/뒤 padding을 남길 수 있다. over-aligned small object가 많으면 memory waste가 커진다.

alignment requirement가 실제 hardware/API 요구인지 검증한다. false sharing 방지용 cache-line alignment와 SIMD load alignment는 목적이 다르다. 구조체 크기와 array stride가 cache footprint에 미치는 영향도 계산한다.

---

## CHAPTER 18 · realloc은 pointer 안정성을 보장하지 않는다

block 뒤에 충분한 free space가 있으면 in-place 확장이 가능하지만 그렇지 않으면 새 block allocate→copy→old free가 필요하다. 따라서 realloc 뒤 old pointer/alias를 계속 사용하면 use-after-free가 된다.

large buffer growth를 반복하면 copy cost가 누적된다. geometric capacity growth는 realloc 횟수를 줄이지만 unused capacity를 보유한다. dynamic array growth factor도 allocator와 cache trade-off다.

---

## CHAPTER 19 · zero-size와 overflow corner case는 API contract를 정확히 따라야 한다

`malloc(0)`, `realloc(p,0)`, `calloc(n,size)`의 semantics는 language/library standard와 implementation을 확인한다. 특히 `n*size` overflow를 caller가 계산한 뒤 malloc에 넘기면 작은 buffer가 생성될 수 있다.

size multiplication을 내부에서 checked하는 API를 사용하거나 explicit overflow check를 둔다. allocation failure와 zero-size sentinel을 같은 null return으로 혼동하지 않게 error contract를 정한다.

---

## CHAPTER 20 · double free는 free-list metadata를 공격 표면으로 바꿀 수 있다

같은 block을 두 번 free하면 allocator internal list/bin에 동일 address가 중복 삽입되거나 metadata invariant가 깨질 수 있다. hardened allocator는 ownership state, safe-linking, quarantine, consistency check로 탐지/악용을 어렵게 한다.

근본 해결은 ownership을 단일화하는 것이다. pointer를 free한 뒤 invalid state로 만들고 transfer semantics를 type/API로 표현한다. reference counting에서도 마지막 owner 판정 race가 double free를 만들 수 있다.

---

## CHAPTER 21 · use-after-free는 allocator reuse가 빨라질수록 증상이 비결정적이다

free된 block이 아직 같은 bytes를 유지하면 stale pointer access가 우연히 정상처럼 보이다 다음 allocation이 같은 block을 재사용하면 다른 object를 corrupt한다. 그래서 bug 발생 시점과 crash 시점이 멀어질 수 있다.

quarantine은 free block을 즉시 재사용하지 않아 UAF detectability를 높이고 sanitizer redzone과 결합될 수 있다. production allocator의 reuse policy가 바뀌면 잠복 bug가 새 release에서 갑자기 나타날 수 있다.

---

## CHAPTER 22 · metadata corruption은 crash 위치보다 이전 write를 찾아야 한다

allocator가 free-list pointer나 size header를 읽다 crash했어도 실제 corruption은 이전 buffer overflow가 metadata를 덮은 시점에 발생했을 수 있다. allocator function을 root cause로 오인하지 않는다.

redzone, guard page, ASan, hardware memory tagging을 사용해 corruption point에 가까운 위치에서 fault를 발생시킨다. crash dump에서 damaged block 주변 allocation history를 추적한다.

---

## CHAPTER 23 · memory tagging은 pointer와 allocation generation/region을 연결한다

architecture/runtime의 memory tagging mechanism은 pointer에 tag를 포함하고 memory granule의 tag와 비교해 stale/wrong pointer access를 탐지할 수 있다. tag space가 유한하므로 완전한 temporal safety proof는 아니지만 UAF/overflow detectability를 높인다.

Android/Arm 환경에서 MTE 같은 mechanism을 사용할 때 synchronous/asynchronous fault mode와 performance cost를 이해한다. production sampling과 test full-check mode를 분리할 수 있다.

---

## CHAPTER 24 · allocator hardening은 exploit reliability를 낮추지만 memory safety를 대체하지 않는다

free-list pointer encoding, randomization, guard, canary, quarantine는 heap corruption exploit을 어렵게 한다. 그러나 legitimate pointer로 out-of-bounds write가 발생하는 source bug 자체를 제거하지 않는다.

hardening flag가 final binary/runtime allocator에 적용되는지 검사한다. performance cost 때문에 일부 feature를 끌 경우 threat model과 compensating control을 기록한다.

---

## CHAPTER 25 · fragmentation은 requested, allocated, active, resident의 차이로 측정한다

allocator stats에서 application이 요청한 bytes(requested), size-class가 배분한 bytes(allocated), page에 active한 region, OS에서 resident한 page는 서로 다르다. 이 차이를 분해해야 internal fragmentation과 retained page를 구분할 수 있다.

RSS-requested gap이 크다고 전부 allocator 탓은 아니다. native library, mmap, stack, page cache를 process map으로 분리한다. allocator-specific telemetry와 OS memory map을 같은 시점에 수집한다.

---

## CHAPTER 26 · scavenging/decay는 free page를 OS에 언제 반환할지 결정한다

free block이 늘었을 때 allocator가 즉시 `madvise`/unmap으로 page를 반환하면 RSS는 줄지만 이후 allocation에서 page fault/zeroing 비용이 다시 발생한다. 일정 시간 보유하면 burst 재사용은 빠르지만 idle RSS가 높다.

decay time은 workload burst period와 memory pressure에 맞춘다. container limit이 빡빡하면 aggressive reclaim이 필요할 수 있고 low-latency server는 resident cache를 더 유지할 수 있다.

---

## CHAPTER 27 · memory zeroing은 security와 bandwidth 비용을 동시에 가진다

새 process/user에게 이전 content가 노출되지 않도록 OS는 새 page를 zero 상태로 제공해야 한다. large allocation first-touch에서 zeroing bandwidth가 latency로 나타날 수 있다.

allocator가 이미 zeroed block인지 추적하면 redundant memset을 줄일 수 있지만 correctness/security invariant를 유지해야 한다. `calloc`이 항상 malloc+memset보다 느리다고 가정하지 않는다. lazy-zero page와 optimization이 있을 수 있다.

---

## CHAPTER 28 · allocator profiling은 allocation site와 retained lifetime을 연결한다

sampled allocation profile은 어떤 call site가 bytes/object를 생성하는지 보여 주고 heap snapshot은 무엇이 남아 있는지 보여 준다. 생성량과 retention을 함께 보면 churn과 leak를 분리할 수 있다.

sampling rate와 overhead를 이해한다. 매우 작은 object가 초고빈도로 생성되면 byte-weighted sample에서 count 문제가 작게 보일 수 있다. count/bytes/lifetime을 모두 본다.

---

## CHAPTER 29 · Android native memory는 ART heap 밖 allocator behavior를 가진다

JNI/native library, graphics, codec는 native allocator를 사용하며 Java heap limit/GC만으로 관리되지 않는다. managed object가 small wrapper만 보유하고 native buffer가 큰 경우 heap dump에서는 실제 footprint가 작게 보일 수 있다.

native allocation tag/site와 process maps, graphics memory를 함께 수집한다. native resource를 managed finalizer에만 의존하면 reclamation이 늦어질 수 있으므로 explicit close/ownership을 사용한다.

---

## CHAPTER 30 · allocator 선택은 latency, footprint, concurrency, security 네 축으로 평가한다

allocator benchmark는 단일-thread alloc/free loop만으로 부족하다. 실제 size/lifetime distribution, cross-thread free, peak concurrency, idle decay를 재현해야 한다. p99 allocation latency와 RSS/fragmentation을 동시에 측정한다.

custom pool/arena는 workload가 명확할 때 강력하지만 lifetime과 max capacity가 틀리면 memory를 과도하게 붙잡는다. general allocator를 바꾸기 전에 application ownership과 allocation rate를 줄이는 것이 더 큰 효과인지 검증한다. allocator engineering의 목표는 빠른 `malloc` 한 번이 아니라 **전체 object lifetime 동안 predictable memory cost를 유지하는 것**이다.