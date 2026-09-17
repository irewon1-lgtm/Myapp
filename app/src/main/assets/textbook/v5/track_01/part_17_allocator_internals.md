# PART 17 · allocator 내부 — page allocator에서 object bin까지

`malloc/new`는 memory를 만드는 함수가 아니다. allocator는 이미 확보한 address range와 page를 **어떤 크기로 나누고, 어떤 thread에 배분하며, 언제 합치고, 언제 OS에 반환할지** 결정한다. latency, fragmentation, locality, security가 이 정책에서 동시에 나온다.

---

## CHAPTER 01 · physical page allocator와 object allocator는 서로 다른 단위의 자원을 관리한다

kernel page allocator는 physical frame과 page order를 다루고, user-space allocator나 slab 계층은 그 page를 더 작은 object나 block으로 잘게 나눈다. 작은 allocation마다 kernel로 내려가면 syscall, page-table update, zeroing 비용이 너무 커지므로 일반 allocator는 큰 region을 확보한 뒤 내부 metadata로 object를 재사용한다. 따라서 `object allocation 실패`와 `system free page 부족`은 같은 사건이 아닐 수 있다.

진단은 요청 크기, allocator bin, arena/slab 상태, backing page 확보 여부를 분리한다. free block이 남아 있어도 alignment나 contiguous requirement 때문에 요청을 만족하지 못할 수 있고, 반대로 process RSS가 높아도 많은 page가 allocator cache에 남아 재사용 가능할 수 있다. leak 판단에서 logical live object와 allocator-reserved page를 구분해야 한다.

운영 metric은 requested bytes, resident bytes, mapped bytes, page fault, allocation latency를 같이 본다. object layer와 page layer의 숫자가 일치하지 않는다는 사실 자체가 버그는 아니다. 두 계층의 정책 차이가 footprint와 tail latency를 만든다.

---

## CHAPTER 02 · buddy allocator는 power-of-two page block을 split과 merge로 관리한다

buddy system은 큰 page block을 절반씩 나눠 작은 order를 만들고, 인접한 buddy가 모두 free면 다시 상위 order로 합칠 수 있게 설계된다. 빠른 allocation과 coalescing이 장점이지만 요청 크기가 power-of-two 경계와 맞지 않으면 internal waste가 생길 수 있다. 장시간 uptime에서 다양한 크기의 allocation과 pinning이 섞이면 큰 contiguous block을 얻기 어려워질 수 있다.

high-order allocation 실패는 total free memory만 보면 설명되지 않는다. free page가 충분해도 여러 작은 block으로 흩어져 있으면 큰 order를 만들기 위해 compaction이 필요하고, pinned page가 있으면 compaction조차 실패할 수 있다. 그래서 buddy order distribution과 compaction stall을 함께 본다.

driver나 huge page처럼 contiguous memory가 필요한 path는 average allocation보다 worst-case fragmentation에 민감하다. regression test에서는 clean boot 직후뿐 아니라 churn과 long-lived allocation이 누적된 상태에서 high-order request 성공률과 latency를 측정해야 한다.

---

## CHAPTER 03 · memory zone은 모든 physical page가 같은 제약을 갖지 않는다는 사실을 반영한다

DMA addressability, architecture limitation, movable 여부 같은 이유로 kernel은 physical memory를 zone이나 migration type으로 구분할 수 있다. 어떤 allocation은 특정 zone에서만 성공할 수 있으므로 system 전체 free memory가 남아 있어도 해당 zone이 고갈되면 실패한다. 이 현상은 `메모리가 남는데 왜 allocation이 실패하나`라는 질문의 대표 원인이다.

zone pressure는 workload와 device configuration에 따라 달라진다. 많은 low-address DMA buffer, non-movable kernel allocation, long-term pin이 특정 영역을 잠식하면 다른 page는 충분해도 요구를 만족하지 못한다. allocator는 reclaim이나 compaction을 시도하지만 제약이 강하면 latency만 증가하고 결국 실패할 수 있다.

incident에서는 total free, zone별 free/order 분포, allocation flags와 caller를 같이 본다. 단순히 RAM을 추가하는 해결책이 해당 constrained zone을 늘리지 않으면 효과가 없을 수 있다. resource constraint의 종류를 먼저 확인해야 한다.

---

## CHAPTER 04 · per-CPU page cache는 global allocator contention을 줄이는 locality 계층이다

모든 small page allocation이 global lock이나 shared free list를 건드리면 multi-core system에서 coherence와 lock contention이 커진다. per-CPU page cache는 자주 사용하는 작은 order page를 각 CPU 가까이에 보관해 fast path를 만든다. allocation과 free가 같은 CPU에서 반복되면 global synchronization을 크게 줄일 수 있다.

하지만 per-CPU cache가 많아지면 free memory가 여러 CPU에 분산되어 global 관점에서 즉시 사용하기 어려울 수 있다. 특정 CPU가 많이 비우고 다른 CPU가 부족하면 refill/drain이 발생하고, CPU hotplug나 memory pressure에서는 cache를 회수해야 한다. locality 이득과 stranded capacity 사이 trade-off가 있다.

진단에서는 global allocator cost만 보지 않고 per-CPU hit/refill, migration, remote free를 본다. thread affinity 변화로 allocation/free CPU가 갈라지면 예상했던 local reuse가 깨질 수 있다. allocator scaling은 CPU topology와 scheduling pattern에 의존한다.

---

## CHAPTER 05 · slab allocator는 동일한 kernel object를 반복 생성하는 비용을 줄인다

kernel은 inode, dentry, socket 같은 반복되는 object를 일정 size와 initialization pattern으로 생성한다. slab 계열 allocator는 page를 object slot로 나누고 이미 초기화된 cache를 재사용해 allocation latency와 metadata overhead를 줄인다. object size와 alignment가 고정되어 있기 때문에 generic variable-size allocator보다 빠른 path를 만들 수 있다.

slab cache는 free object가 많아도 backing page가 process나 subsystem에 전혀 필요 없다는 뜻은 아니다. partial slab이 많이 남으면 low occupancy 때문에 page를 다른 용도로 반환하기 어렵다. object count, slab count, per-slab occupancy를 함께 봐야 memory pressure를 설명할 수 있다.

문제가 특정 object cache에서 생기면 call-site별 allocation rate와 lifetime distribution을 확인한다. cache 이름만 보고 leak이라고 결론내리지 않고 active object가 실제로 계속 증가하는지, reclaimable empty slab이 줄지 않는지, reference owner가 누구인지 연결해야 한다.

---

## CHAPTER 06 · slab occupancy는 free object 총량보다 page 회수 가능성을 더 잘 설명한다

slab page 하나에 object 100개가 들어가는데 1개만 살아 있어도 그 page 전체를 즉시 반환하지 못할 수 있다. 여러 slab에 live object가 하나씩 흩어지면 logical live bytes는 적어도 physical footprint는 높게 남는다. 이것이 allocator-level fragmentation의 전형적인 형태다.

occupancy 개선은 object를 무조건 compact할 수 없는 kernel 환경에서는 어렵다. 대신 lifetime이 비슷한 object를 같은 cache나 slab에 모으고, long-lived와 short-lived allocation을 분리하면 empty slab이 생길 가능성을 높일 수 있다. cache design이 memory reclaimability에 영향을 준다.

운영에서 평균 occupancy만 보면 tail을 놓칠 수 있다. partial slab 수, empty/full 비율, object lifetime을 함께 봐야 한다. 특정 feature를 끈 뒤 object count는 줄었는데 RSS가 그대로라면 slab sparsity가 남았는지 확인한다.

---

## CHAPTER 07 · contiguous allocation은 byte 수보다 physical geometry에 제약된다

같은 2MiB 요청이라도 가상주소가 연속인 것과 physical page가 연속인 것은 다르다. 일반 user-space memory는 page table로 흩어진 physical frame을 연속 virtual range처럼 보이게 할 수 있지만 DMA나 huge page처럼 physical contiguity를 요구하는 경우는 free-space geometry가 중요하다.

장시간 allocation/free churn은 작은 hole을 많이 만들 수 있고 pinned page는 compaction 이동을 막는다. total free 30%인데 high-order allocation이 실패하는 상황이 생기는 이유다. reboot가 증상을 없애면 physical fragmentation이나 pinning이 원인 후보가 된다.

진단에는 buddy order distribution, compaction duration, pinned page, high-order failure를 기록한다. 연속성이 필요 없는 workload라면 scatter-gather나 IOMMU를 활용해 요구 자체를 줄이는 것이 allocator tuning보다 강한 해결책일 수 있다.

---

## CHAPTER 08 · user-space arena는 syscall 비용을 줄이는 대신 process 내부 정책을 만든다

일반 allocator는 OS에서 큰 virtual region을 확보하고 내부 arena에서 block을 나눠 준다. 작은 `malloc`이 매번 `mmap`이나 `brk`를 호출하지 않는 이유다. 이 계층에서 free list, bin, thread cache, arena 선택 정책이 생기며 application의 allocation pattern이 실제 RSS와 latency를 결정한다.

arena가 너무 적으면 multi-thread contention이 커지고, 너무 많으면 free block이 arena별로 분산되어 fragmentation과 footprint가 늘 수 있다. 한 arena에 충분한 free memory가 있어도 다른 arena의 요청이 그것을 즉시 재사용하지 못하는 구현이 있을 수 있다. thread 수 증가와 RSS 증가가 함께 나타나는 원인 중 하나다.

측정은 allocation throughput뿐 아니라 arena 수, per-arena resident/free bytes, remote free를 본다. allocator configuration을 바꾸기 전 object lifetime과 concurrency를 관찰해야 한다. magic arena count로 모든 workload를 해결할 수는 없다.

---

## CHAPTER 09 · free list는 reusable block을 찾는 검색 정책을 결정한다

free된 block을 어떤 순서와 자료구조로 저장하는지에 따라 allocation latency와 fragmentation이 달라진다. first-fit은 빠를 수 있지만 작은 hole을 남길 수 있고, best-fit은 waste를 줄이려다 검색 비용과 unusable fragment를 늘릴 수 있다. segregated list는 size class별 후보를 제한해 빠른 lookup을 제공한다.

free list metadata가 block 내부에 저장되면 memory corruption이 allocator control structure를 덮을 위험이 있다. modern allocator는 pointer encoding, quarantine, consistency check 같은 hardening을 추가할 수 있다. 성능과 attack resistance 사이 비용이 존재한다.

진단에서는 free bytes 총량보다 requested size가 어떤 list를 탐색했고 얼마나 많은 candidate를 확인했는지 본다. fragmentation workload에서 allocation latency가 점차 늘어난다면 free-list search와 split pattern을 profile해야 한다.

---

## CHAPTER 10 · size class는 allocation을 빠르게 만드는 대신 rounding waste를 만든다

allocator는 비슷한 요청 크기를 동일 class로 묶어 fixed-size slot에서 빠르게 배분할 수 있다. 33-byte 요청이 40 또는 48-byte class로 올라가면 차이는 internal fragmentation이 된다. 작은 차이는 개별 object에서는 작지만 수백만 개가 존재하면 memory footprint를 크게 바꾼다.

size class 간격을 촘촘히 하면 waste는 줄지만 metadata와 free list가 늘고 cache locality가 나빠질 수 있다. 반대로 class 수를 줄이면 fast path는 단순해지지만 rounding overhead가 커진다. allocator는 대상 workload의 size distribution을 전제로 절충한다.

application 최적화에서는 logical payload size만 줄이지 말고 class boundary를 확인한다. object를 65에서 64 byte로 줄였을 때 실제 allocated size가 한 class 내려가면 효과가 크지만 70에서 68로 줄여 같은 class에 남으면 RSS 변화가 거의 없을 수 있다.

---

## CHAPTER 11 · block split은 큰 free block을 작은 요청에 맞추며 future geometry를 바꾼다

요청보다 큰 free block을 선택하면 allocator는 필요한 부분과 remainder를 나눌 수 있다. 이 split은 현재 요청을 해결하지만 남은 조각이 이후 size distribution과 맞지 않으면 external fragmentation을 만든다. 너무 작은 remainder는 metadata를 유지할 가치가 없어 원래 allocation에 흡수될 수 있다.

split threshold는 latency와 waste를 동시에 결정한다. 공격적인 split은 다양한 free block을 만들지만 metadata와 coalescing 부담이 늘고, 보수적인 split은 internal waste가 커진다. workload가 특정 size로 반복되는지 다양한 size가 섞이는지에 따라 결과가 달라진다.

fragmentation 분석에서 free block histogram을 시간축으로 보면 split policy의 흔적을 찾을 수 있다. 특정 작은 크기의 unusable fragment가 계속 증가한다면 object layout이나 allocation batching을 바꾸는 쪽이 allocator flag 조정보다 효과적일 수 있다.

---

## CHAPTER 12 · coalescing은 인접 free block을 다시 큰 block으로 복구한다

두 인접 block이 모두 free라면 합쳐 더 큰 요청을 받을 수 있게 만드는 것이 coalescing이다. 즉시 합치면 fragmentation을 줄이지만 free path 비용이 늘고, deferred coalescing은 fast free를 제공하지만 큰 요청 직전에 추가 work가 필요할 수 있다.

coalescing 가능 여부를 알려면 인접 block의 상태를 효율적으로 알아야 하므로 boundary tag나 metadata가 필요하다. 이 metadata가 손상되면 잘못된 merge가 다른 live block까지 free 영역으로 포함해 severe corruption으로 이어질 수 있다.

운영에서 large allocation failure와 small free bytes가 동시에 보이면 coalescing 상태를 확인한다. reclaim pressure 순간에 deferred coalescing이 몰리면 tail latency가 튈 수 있으므로 allocation profile과 free-path cost를 함께 측정한다.

---

## CHAPTER 13 · multiple arena는 lock contention을 줄이지만 메모리를 shard한다

multi-thread allocator가 global heap lock 하나만 쓰면 allocation-heavy workload에서 심한 contention이 생긴다. 여러 arena를 두면 thread 집합을 분산해 병렬성을 높일 수 있다. 대신 각 arena가 자신의 free block과 top chunk를 가지면 unused memory가 서로 공유되지 못해 process footprint가 커질 수 있다.

thread가 자주 생성·종료되거나 CPU migration이 많은 workload에서는 arena assignment가 예상과 달라질 수 있다. 특정 arena에 long-lived object가 남아 page 반환을 막는 반면 다른 arena는 계속 새 page를 확보할 수 있다. 평균 free ratio만으로는 이런 imbalance를 볼 수 없다.

allocator tuning은 throughput, arena count, resident memory를 함께 비교한다. arena 제한을 줄였을 때 lock wait가 늘어 latency가 나빠질 수 있으므로 RSS 절감 하나만 보고 선택하면 안 된다.

---

## CHAPTER 14 · thread cache는 hot allocation을 빠르게 만들지만 free memory를 thread별로 가둔다

thread-local cache는 작은 size class의 object를 local list에 보관해 shared lock과 cache-line traffic을 피한다. allocate/free가 같은 thread에서 반복되면 매우 빠르다. 하지만 많은 thread가 각각 cache를 채우면 실제 live object보다 훨씬 많은 memory가 process 안에 남을 수 있다.

thread pool이 크거나 workload가 bursty하면 idle thread cache가 오랫동안 memory를 잡을 수 있다. cache refill/drain threshold가 너무 크면 footprint가 증가하고 너무 작으면 shared allocator와 왕복이 늘어 throughput이 떨어진다. thread lifetime과 cache 정책이 연결된다.

incident에서는 thread 수 변화와 allocator resident 증가를 같은 그래프에 놓는다. 기능 traffic이 줄었는데 RSS가 안 내려가는 경우 live heap이 아니라 per-thread cache가 원인일 수 있다. cache flush 실험으로 가설을 검증하되 production 설정은 latency까지 재측정한다.

---

## CHAPTER 15 · remote free는 allocation CPU와 free CPU가 다를 때 ownership 경계를 넘는다

object를 thread A가 allocate하고 thread B가 free하면 B의 local cache에 바로 넣는 것이 항상 최선은 아니다. 원래 arena나 owner CPU의 accounting과 locality를 유지하려면 remote-free queue에 넣었다가 owner가 회수할 수 있다. 이 경로는 shared synchronization과 delayed reclamation을 추가한다.

producer-consumer architecture나 task migration이 심한 system은 remote free 비율이 높아질 수 있다. allocation은 빠른데 free backlog가 쌓이면 RSS가 증가하고 owner thread가 나중에 대량 drain하면서 latency spike를 만들 수 있다. 단순 allocation profile만 봐서는 원인을 놓친다.

운영 metric은 alloc/free thread relation, remote queue depth, drain cost를 포함한다. object ownership을 request나 shard에 맞춰 설계하면 allocator 수준의 cross-thread traffic을 줄일 수 있다.

---

## CHAPTER 16 · large allocation은 small-bin fast path와 다른 경로를 사용할 수 있다

매우 큰 object는 arena 내부에서 나누기보다 별도 `mmap` 영역으로 확보하는 allocator가 많다. 이런 allocation은 page table, virtual mapping, zeroing, kernel interaction 비용이 크지만 free 후 OS에 직접 반환하기 쉽다. threshold 근처의 크기는 구현 policy에 따라 전혀 다른 성능을 보일 수 있다.

large object가 자주 생성·해제되면 VMA churn과 TLB shootdown, page fault가 증가할 수 있다. 반대로 arena에 유지하면 mapping overhead는 줄지만 high-water RSS가 내려가지 않을 수 있다. long-lived large buffer와 ephemeral scratch buffer는 요구가 다르다.

profile에서 size histogram을 보고 threshold 근처 allocation이 집중되는지 확인한다. object pooling이 항상 답은 아니다. pool이 peak-sized buffer를 영구 보유하면 memory capacity를 악화시킬 수 있으므로 reuse probability와 peak concurrency를 함께 계산한다.

---

## CHAPTER 17 · alignment는 ABI와 SIMD 요구를 만족시키지만 padding cost를 만든다

object 주소는 type과 instruction이 요구하는 alignment를 만족해야 한다. 자연 alignment를 어기면 일부 architecture에서 fault가 나거나 여러 memory transaction으로 느려질 수 있다. allocator는 header와 requested alignment를 고려해 block 시작점을 조정하고 그 사이 padding을 소모한다.

과도한 alignment 요청은 작은 object에서도 큰 waste를 만든다. 64-byte alignment가 cache line isolation에 필요할 수 있지만 모든 object에 적용하면 footprint와 cache density가 나빠진다. SIMD와 DMA처럼 실제 requirement가 있는지 확인해야 한다.

FFI와 shared-memory 구조에서는 allocator alignment뿐 아니라 struct layout과 ABI를 함께 본다. 같은 pointer가 충분히 aligned되어도 내부 field offset이 잘못되면 SIMD load나 native code에서 failure가 생길 수 있다.

---

## CHAPTER 18 · realloc은 resize가 아니라 object identity와 copy 가능성을 바꾸는 operation이다

`realloc`은 기존 block 뒤에 충분한 free space가 있으면 제자리에서 확장할 수 있지만, 그렇지 않으면 새 block을 allocate하고 데이터를 copy한 뒤 old block을 free할 수 있다. 따라서 성공 뒤 pointer가 바뀔 수 있고 기존 alias는 invalid해진다. 실패 시 original pointer 처리 규칙도 정확히 따라야 한다.

큰 buffer가 점진적으로 조금씩 증가하면 반복 copy가 누적되어 O(n²)에 가까운 비용을 만들 수 있다. dynamic array가 capacity를 geometric하게 늘리는 이유는 resize 횟수를 줄이기 위해서다. shrink도 항상 physical memory 반환을 의미하지 않는다.

profiling에서는 realloc count, moved bytes, in-place success ratio를 본다. latency spike가 memcpy 때문인지 kernel mapping 때문인지 분리해야 한다. external pointer가 존재하는 API에서는 movable storage 대신 stable handle을 사용하는 것이 안전할 수 있다.

---

## CHAPTER 19 · allocation size overflow는 작은 buffer를 만든 뒤 큰 write를 허용하는 보안 버그가 된다

`count * elementSize + header` 같은 size 계산이 integer 범위를 넘으면 실제 필요한 크기보다 작은 값으로 wrap될 수 있다. allocator는 전달받은 작은 size만큼 정상적으로 memory를 주지만 caller는 원래 count를 믿고 더 많은 bytes를 써 heap corruption을 만든다. allocator 자체가 잘못한 것이 아니다.

size arithmetic은 allocation 전에 checked multiplication/addition으로 검증해야 한다. signed/unsigned conversion과 alignment rounding도 overflow 지점이 될 수 있다. untrusted length가 들어오는 parser, image, network path는 특히 위험하다.

fuzzing에서는 최대값 근처 count와 조합을 넣어 size 계산을 공격한다. sanitizer가 overwrite를 잡아도 root cause는 preceding arithmetic일 수 있으므로 report에서 allocation size와 intended logical size를 함께 기록한다.

---

## CHAPTER 20 · double free는 free list와 allocator metadata를 두 번 변경하는 lifetime violation이다

같은 block을 두 번 free하면 allocator는 이미 free 상태인 memory를 다시 free list에 넣거나 metadata를 중복 갱신할 수 있다. 결과는 즉시 abort일 수도 있고 나중 allocation에서 같은 address가 두 owner에게 반환되는 silent corruption일 수도 있다. modern hardening이 이를 탐지해도 application lifetime bug 자체는 남는다.

원인은 cleanup path 중복, ownership transfer 불명확, error handling에서 흔하다. `free 후 null`은 단일 alias에는 도움이 되지만 다른 alias가 있으면 충분하지 않다. resource owner를 명확히 하고 move/RAII 같은 구조로 release 권한을 한 곳에 모으는 편이 강하다.

incident에서는 최초 free와 두 번째 free stack을 둘 다 확보해야 한다. allocator가 마지막 crash 지점만 보여 주면 원래 ownership transfer를 놓칠 수 있다. deterministic lifetime test와 sanitizer를 병행한다.

---

## CHAPTER 21 · use-after-free는 memory가 반환된 뒤 stale reference가 계속 사용되는 문제다

free는 pointer 값을 지우는 것이 아니라 해당 storage의 ownership을 allocator에 돌려준다. stale pointer가 남으면 이후 read/write가 allocator metadata나 다른 object를 건드릴 수 있다. 같은 address가 곧 재사용되면 bug가 정상처럼 보였다가 특정 timing에서만 터질 수 있다.

quarantine allocator는 freed block을 즉시 재사용하지 않아 UAF 탐지 확률을 높일 수 있고 memory tagging은 stale pointer와 새 allocation tag mismatch를 잡을 수 있다. 하지만 이런 도구는 lifetime contract를 대체하지 않는다. asynchronous callback과 remote free가 섞이면 owner 종료 시점을 명시해야 한다.

재현에서는 allocation/free sequence, address reuse, thread interleaving을 기록한다. crash instruction보다 object가 언제 logical lifetime을 끝냈는지 찾는 것이 핵심이다. generation handle은 stale reference를 값 수준에서 거부하는 데 유용하다.

---

## CHAPTER 22 · allocator metadata corruption은 나중의 unrelated allocation에서 폭발할 수 있다

heap allocator는 block size, state, free-list link 같은 metadata를 사용한다. out-of-bounds write가 이 정보를 덮으면 즉시 crash하지 않고 다음 `malloc/free`에서 invariant check가 실패할 수 있다. 그래서 allocator 내부에서 abort가 발생해도 실제 overwrite는 훨씬 이전 code에 있을 수 있다.

hardening은 checksum, encoded pointer, redzone, invariant validation으로 corruption을 일찍 탐지한다. 이런 검사는 overhead가 있지만 root-cause 거리와 exploitability를 줄인다. release에서 완전히 끄기보다 위험도에 맞는 protection을 유지하는 이유다.

조사에서는 실패한 allocator operation만 수정하지 않는다. ASan, hardware tagging, canary를 사용해 최초 invalid write를 찾고 corrupted address의 object history를 추적한다. heap dump와 allocation stack이 도움이 된다.

---

## CHAPTER 23 · memory tagging은 pointer와 allocation generation을 연결해 stale access를 탐지한다

tagging mechanism은 pointer와 memory granule에 작은 tag를 연결하고 access 시 일치 여부를 검사한다. free 후 새 allocation에 다른 tag가 배정되면 오래된 pointer가 같은 address를 가리켜도 mismatch로 fault를 만들 수 있다. address reuse만 보는 UAF보다 더 직접적인 generation signal을 제공한다.

tag bit 수가 제한되면 충돌 가능성이 있고 모든 native code가 tagging contract를 이해해야 한다. pointer bit manipulation이나 serialization이 tag를 손상할 수 있어 ABI와 runtime 지원이 필요하다. 성능 모드와 synchronous/asynchronous fault mode도 진단 경험을 바꾼다.

보안 test에서는 tagging이 켜졌다는 설정이 아니라 실제 fault report와 symbolization을 확인한다. bug가 탐지되지 않았다고 memory-safe하다는 뜻은 아니며, allocator hardening과 ownership discipline을 함께 유지해야 한다.

---

## CHAPTER 24 · allocator hardening은 exploitation cost를 높이지만 correctness 수정은 아니다

quarantine, randomization, guard page, pointer encoding, consistency check는 heap corruption이 deterministic exploit로 이어지는 것을 어렵게 하고 조기 탐지를 돕는다. 그러나 application이 double free나 overflow를 일으키는 원인은 그대로다. protection은 defense-in-depth이며 bug fix의 대체물이 아니다.

hardening level이 높으면 memory overhead와 allocation latency가 증가할 수 있다. production에서 어느 기능을 유지할지 threat model과 performance budget으로 결정한다. debug build에서만 protection을 켜면 field-only corruption을 놓칠 수 있어 일부 기능은 release에도 필요하다.

변경 전후에는 throughput뿐 아니라 exploit-relevant invariant와 crash detectability를 확인한다. protection을 끄고 benchmark가 빨라졌다는 이유로 제거하면 incident 비용과 보안 위험을 숨기는 셈이다.

---

## CHAPTER 25 · fragmentation metric은 live bytes와 reserved bytes의 차이를 구조적으로 설명해야 한다

fragmentation은 하나의 퍼센트가 아니다. size-class rounding으로 생기는 internal waste, free block이 흩어져 large request를 못 받는 external fragmentation, sparse slab/arena 때문에 page를 OS에 반환하지 못하는 retention을 분리해야 한다. 각각 해결책이 다르다.

`RSS - live heap`만 계산하면 file mapping, thread stack, allocator metadata까지 섞여 잘못된 결론을 낼 수 있다. allocator가 제공하는 active/allocated/resident/retained 통계를 이해하고 object size histogram과 함께 본다.

시간축도 중요하다. peak 후 resident가 천천히 내려오는 정상 cache behavior인지, 동일 traffic에서 high-water mark가 계속 증가하는 leak인지 구분한다. fragmentation은 snapshot보다 workload history와 관련된다.

---

## CHAPTER 26 · scavenging은 unused page를 OS에 돌려주며 latency와 footprint를 교환한다

allocator가 free block을 보유하면 future allocation은 빠르지만 process RSS가 높게 남는다. scavenger는 충분히 비어 있는 page를 decommit하거나 `madvise`해 OS가 physical memory를 회수할 수 있게 한다. aggressive scavenging은 memory footprint를 줄이지만 곧 다시 page fault와 zeroing 비용을 만들 수 있다.

interactive app와 batch server는 적절한 decay time이 다르다. burst 이후 긴 idle이 있다면 빠른 반환이 유리하고 steady high-throughput service는 cache 유지가 좋을 수 있다. system memory pressure signal에 반응해 정책을 조정하는 구현도 있다.

측정은 RSS뿐 아니라 allocation p99, minor fault, page reclaim을 같이 본다. `메모리 줄이기`만 목표로 scavenging을 세게 하면 latency가 악화될 수 있다. footprint와 reuse interval을 함께 최적화해야 한다.

---

## CHAPTER 27 · zeroing은 security invariant이며 allocation latency의 실질 비용이다

다른 process나 보안 domain이 쓰던 physical page를 새 owner에게 그대로 주면 이전 data가 노출될 수 있다. 운영체제는 user-visible page를 적절히 zeroing해 confidentiality를 보장한다. 이 비용은 large first-touch allocation에서 눈에 띄는 CPU/memory bandwidth를 사용할 수 있다.

allocator 내부에서 같은 security domain의 free object를 재사용할 때는 모든 bytes를 즉시 zero할 필요가 없을 수 있지만 language/runtime가 초기값을 요구하면 별도 initialization이 필요하다. lazy zero, background zeroing 같은 최적화는 보안 boundary를 깨지 않는 범위에서만 가능하다.

performance trace에서 page fault와 zeroing function이 hotspot이면 단순 memcpy 문제로 보지 않는다. allocation size와 first-touch pattern을 줄이거나 reuse를 늘리는 구조적 개선이 필요하다. security clear를 제거하는 최적화는 허용되지 않는다.

---

## CHAPTER 28 · allocation profiling은 크기보다 call site와 lifetime을 연결할 때 가치가 커진다

현재 heap snapshot은 어떤 object가 살아 있는지 보여 주지만 allocation churn을 설명하지 못한다. sampling profiler는 call site별 allocated bytes와 count를 보여 주어 short-lived hot allocation을 찾을 수 있다. retained heap과 allocation rate는 서로 다른 문제다.

높은 allocation rate가 항상 bad인 것도 아니다. collector나 allocator가 빠르게 처리하고 latency SLO를 만족하면 acceptable할 수 있다. 반대로 적은 수의 매우 큰 long-lived allocation은 fragmentation과 peak RSS를 좌우할 수 있다. size와 lifetime distribution을 같이 본다.

optimization 전후에는 allocation site가 사라졌는지뿐 아니라 CPU, GC, RSS가 실제로 개선됐는지 확인한다. object pooling이 allocation count를 줄여도 stale state와 memory retention을 늘릴 수 있으므로 end-to-end metric이 필요하다.

---

## CHAPTER 29 · Android native memory는 managed heap 밖의 allocator behavior를 별도로 추적해야 한다

Android process의 RSS에는 ART heap뿐 아니라 native heap, graphics buffer, mmap, code, stack이 포함된다. Java/Kotlin heap이 안정적인데 process memory가 계속 증가한다면 JNI library나 native allocator, graphics resource를 조사해야 한다. managed profiler 하나로 전체 leak을 부정할 수 없다.

native allocation은 process importance와 LMK/OOM에 똑같이 영향을 줄 수 있다. large bitmap, codec buffer, C++ cache가 managed heap limit 밖에 있다고 안전한 것은 아니다. allocation call site와 native heap dump, `/proc` mapping, graphics accounting을 같은 시점에 본다.

JNI ownership은 managed object lifetime과 native buffer lifetime을 연결해야 한다. finalizer에만 의존하면 release가 늦어질 수 있고 process pressure가 먼저 올라간다. explicit close와 lifecycle contract가 필요하다.

---

## CHAPTER 30 · allocator 선택은 throughput보다 workload의 lifetime·size·threading 구조에 맞춰야 한다

allocator마다 small-object fast path, thread cache, arena policy, scavenging, hardening의 절충이 다르다. benchmark 하나에서 가장 빠른 allocator가 production에서도 최선이라는 보장은 없다. object size distribution, cross-thread free, peak concurrency, memory limit을 실제 workload로 재현해야 한다.

선택 기준에는 allocation p50/p99, RSS high-water, fragmentation, CPU, security protection, observability를 포함한다. container memory limit이 빡빡한 서비스와 desktop application은 같은 정책을 원하지 않는다. release 운영팀이 allocator statistics와 crash report를 해석할 수 있는지도 중요하다.

교체 전후에는 동일 failure injection과 long-duration soak test를 실행한다. allocator 변경은 memory layout과 timing을 크게 바꿔 숨어 있던 UAF를 감추거나 드러낼 수 있다. 성능 개선과 correctness regression을 동시에 검증해야 한다.