# PART 64 · Userspace Paging and userfaultfd — missing pages, write protection, post-copy, fault ownership

Page fault는 항상 kernel이 backing page를 즉시 결정하는 사건일 필요가 없다. `userfaultfd`는 등록한 virtual-memory range의 fault를 **userspace handler가 관측하고 page content·protection·wake 시점을 결정**할 수 있게 한다. 이 기능은 post-copy migration, snapshot/dirty tracking, userspace paging을 가능하게 하지만 fault handler 자체가 memory·I/O·scheduler dependency에 묶이면 새로운 deadlock과 tail-latency source가 된다.

## CHAPTER 01 · userfaultfd는 fault handling ownership을 userspace로 넘긴다

Process는 userfaultfd object를 만들고 virtual address range를 등록한다. 그 range에서 해당 mode의 page fault가 발생하면 kernel은 faulting thread를 block시키고 event를 file descriptor로 전달할 수 있다. Userspace handler가 fault를 해결할 때까지 original thread는 memory access를 완료하지 못한다. 즉 virtual-memory access가 userspace protocol round trip으로 바뀐다.

## CHAPTER 02 · API feature negotiation은 kernel version 추측보다 정확하다

`UFFDIO_API` handshake는 현재 kernel이 지원하는 feature와 ioctl capability를 알려준다. Version number만 보고 WP, minor, poison, async mode가 있다고 가정하면 distro backport/configuration 차이에서 실패한다. Handler는 요구 feature를 probe하고 지원되지 않으면 명시적 fallback 또는 startup failure를 선택해야 한다.

## CHAPTER 03 · Registered range와 VMA lifetime은 별도 state다

Virtual address range를 userfaultfd에 등록했다고 mapping이 영구히 존재하는 것은 아니다. `munmap`, remap, truncate, process lifecycle이 VMA를 바꿀 수 있다. Handler가 stale address event를 처리하지 않도록 range generation과 unregister/teardown ordering을 관리해야 한다.

## CHAPTER 04 · MISSING mode는 page content 결정 자체를 userspace로 이동한다

등록 range에 PTE/backing page가 없을 때 fault event가 발생하면 handler는 remote storage, snapshot image, compressed cache 등에서 원하는 content를 준비할 수 있다. 이 구조는 demand paging policy를 kernel 밖으로 꺼낸다. 대신 fault latency가 storage/network/userspace scheduling latency를 모두 포함하게 된다.

## CHAPTER 05 · UFFDIO_COPY는 half-populated page를 노출하지 않는 atomic install을 제공한다

Handler가 page content를 userspace buffer에서 target range로 복사하는 동안 faulting thread가 중간 byte state를 보게 해서는 안 된다. `UFFDIO_COPY`는 page population과 mapping publication을 fault-resolution operation으로 묶어 readers가 완료된 page만 관측하게 한다. Copy buffer lifetime은 ioctl completion까지 유지되어야 한다.

## CHAPTER 06 · ZEROPAGE는 zero-filled demand page를 별도 fast path로 해결한다

Snapshot/migration source에서 page가 모두 zero임을 알고 있다면 payload를 network/storage에서 운반할 필요가 없다. `UFFDIO_ZEROPAGE`는 zero page resolution을 명시할 수 있다. Zero-page detection은 bandwidth를 줄이지만 scanning/hash cost가 savings보다 커지는지 workload별 측정이 필요하다.

## CHAPTER 07 · MINOR fault는 content가 cache에 있지만 mapping completion을 userspace가 제어한다

Minor mode에서는 backing content가 이미 page cache 등 kernel memory에 존재할 수 있지만 PTE mapping을 userspace decision 뒤에 완료한다. Handler는 `UFFDIO_CONTINUE`로 기존 page를 mapping할 수 있다. MISSING과 달리 page data를 다시 공급하는 문제가 아니라 **existing backing을 언제 visible하게 할지**가 핵심이다.

## CHAPTER 08 · Write-protect mode는 write first-touch를 event로 바꾼다

Range를 UFFD write-protect 상태로 두면 first write에서 event를 받아 dirty tracking, snapshot COW bookkeeping을 할 수 있다. Handler가 protection을 해제하기 전 faulting writer는 진행하지 못한다. 따라서 write-protect tracking latency는 write critical path에 직접 들어간다.

## CHAPTER 09 · mprotect+SIGSEGV와 userfaultfd WP는 control plane이 다르다

Signal handler 방식은 faulting thread context에서 asynchronous signal constraints를 다뤄야 하지만 userfaultfd는 별도 polling/handler thread에서 event를 처리할 수 있다. 이 분리는 복잡한 bookkeeping을 쉽게 하지만 file descriptor queue, handler scheduling, wakeup protocol이라는 새 dependency를 만든다.

## CHAPTER 10 · Missing과 WP를 함께 쓰면 fault reason 순서를 정확히 해석해야 한다

Page가 아직 존재하지 않으면서 write access가 발생하면 `missing write`와 `write-protected present page`는 다른 상태다. Handler는 event flag와 registered mode에 따라 먼저 content를 공급하고 protection state를 유지할지, 동시에 WP로 install할지 결정해야 한다. 두 state machine을 하나의 `write fault`로 뭉개면 dirty tracking gap이 생긴다.

## CHAPTER 11 · Resolve와 wake를 분리하면 batch 처리할 수 있다

일부 resolution ioctl은 DONTWAKE mode를 사용해 page state를 먼저 준비하고 waiter wakeup을 나중에 별도로 수행할 수 있다. 여러 page를 batch로 준비하거나 ordering barrier 뒤에 동시에 release할 수 있지만 wake를 잊으면 thread가 영구히 block된다. Prepared와 runnable state를 분리한 protocol이 필요하다.

## CHAPTER 12 · Fault handler는 자기 자신이 관리하는 range에 의존하면 recursion할 수 있다

Handler thread가 page를 공급하기 위해 allocation, logging, parsing을 하면서 그 작업이 같은 userfaultfd-managed memory를 access하면 handler 자신이 새 fault를 발생시킬 수 있다. Fault-resolution stack의 code/data는 관리 대상 range 밖에서 progress 가능해야 한다. Pager가 자기 page를 page-in해야 하는 순환 dependency를 피해야 한다.

## CHAPTER 13 · Handler thread scheduling은 memory access latency가 된다

Fault event가 queue에 도착해도 handler가 CPU를 못 받으면 faulting application thread가 계속 block된다. Handler priority, CPU affinity, cgroup quota, runqueue saturation이 page-fault p99에 직접 영향을 준다. Userspace pager는 storage daemon이 아니라 latency-critical scheduler participant다.

## CHAPTER 14 · 하나의 handler thread는 head-of-line blocking을 만들 수 있다

여러 fault가 동시에 발생할 때 handler 하나가 slow remote page를 기다리면 뒤의 cache-hit fault까지 지연될 수 있다. Worker concurrency를 늘리면 throughput은 개선되지만 same-page duplicate requests와 ordering coordination이 필요하다. Fault type과 backing source에 따라 queue를 분리하는 설계도 고려할 수 있다.

## CHAPTER 15 · 같은 page에 여러 fault가 몰리면 request coalescing이 필요하다

여러 threads가 동일 missing page를 동시에 access할 수 있다. 각 event마다 remote fetch를 따로 시작하면 bandwidth를 낭비하고 conflicting install이 생긴다. Page key별 in-flight state를 두고 한 fetch가 완료되면 모든 waiters를 깨우는 coalescing이 필요하다.

## CHAPTER 16 · Post-copy migration은 execution을 memory transfer보다 먼저 시작한다

Destination VM을 먼저 실행하고 missing guest page가 access될 때 source에서 가져오면 migration downtime을 줄일 수 있다. 그러나 source/network가 page server 역할을 계속 수행해야 하고 fault rate가 높으면 guest execution이 network RTT에 묶인다. Post-copy는 downtime과 failure exposure를 교환한다.

## CHAPTER 17 · Pre-copy 한 번은 post-copy readonly fault volume을 줄일 수 있다

실행 전 대부분의 stable/read-only pages를 미리 보내고 hot/remaining pages만 post-copy로 처리하면 initial userfault burst를 줄일 수 있다. Migration strategy는 pre/post 중 하나의 선택이 아니라 dirty rate, network bandwidth, downtime target에 따라 hybrid로 구성될 수 있다.

## CHAPTER 18 · Urgent fault request와 background transfer는 bandwidth arbitration이 필요하다

Post-copy source가 round-robin으로 remaining pages를 보내는 동안 destination fault가 특정 page를 긴급 요청할 수 있다. Urgent page가 bulk stream 뒤에 막히면 fault latency가 커진다. Control request, priority fetch, network queue size를 설계해 fault-driven traffic이 background copy에 묻히지 않게 해야 한다.

## CHAPTER 19 · Duplicate delivery 방지는 source-of-truth bitmap/state가 필요하다

Background transfer 직전 fault request가 도착하면 같은 page를 두 번 보낼 race가 생길 수 있다. Source는 page state를 unsent/in-flight/sent 같은 generation-aware state로 관리해 중복 전송과 missing gap을 막아야 한다. Network success와 destination install completion도 구분할 필요가 있다.

## CHAPTER 20 · Migration 중 source failure는 아직 오지 않은 memory를 잃을 수 있다

Pre-copy는 destination에 대부분 state가 존재하지만 post-copy 초기에 많은 pages가 source에만 남을 수 있다. Source가 죽거나 network가 장기간 단절되면 destination execution이 복구 불가능해질 수 있다. Availability 요구에 따라 replication, checkpoint, abort boundary를 설계해야 한다.

## CHAPTER 21 · Write-protect dirty tracking은 snapshot delta를 만들 수 있다

Snapshot 기준점에서 pages를 WP하고 first write event에서 dirty bit를 기록한 뒤 write를 허용하면 이후 delta set을 추적할 수 있다. Tracking은 page granularity이므로 작은 field write도 전체 page를 dirty로 만든다. Delta size와 fault overhead의 trade-off가 있다.

## CHAPTER 22 · Async WP는 blocking notification 없이 dirty state를 기록하는 다른 mode다

지원 kernel에서는 async write-protect tracking이 fault message를 userspace에 전달하지 않고 kernel이 protection을 자동 해제하면서 PTE state로 dirty 여부를 남길 수 있다. Per-write notification overhead는 줄지만 event stream이 사라지므로 tracking semantics와 collection 방법이 달라진다.

## CHAPTER 23 · PTE가 사라지는 operation은 protection metadata semantics를 확인해야 한다

`MADV_DONTNEED`, hole punch, VMA 변화처럼 PTE 자체가 제거되는 operation에서 WP/RWP tracking bit가 어떻게 유지되거나 사라지는지는 mode마다 다를 수 있다. Dirty/snapshot system은 memory-management operation을 평범한 access와 동일하게 취급하면 tracking gap이 생긴다.

## CHAPTER 24 · Huge pages는 fault resolution granularity와 copy cost를 바꾼다

Hugetlbfs/THP-backed mapping에서 supported userfaultfd mode와 page-size granularity를 확인해야 한다. 큰 page를 한 번에 공급하면 event 수는 줄지만 remote fetch/copy latency와 overfetch가 커질 수 있다. Hot subset이 작다면 huge-page split/placement 정책과 충돌한다.

## CHAPTER 25 · UFFDIO_POISON은 hardware memory error semantics를 이식할 수 있다

지원 kernel에서는 fault resolution 대신 해당 range를 poison으로 표시해 이후 access에 SIGBUS 또는 guest machine-check 같은 failure를 전달할 수 있다. VM migration에서 source host의 poisoned guest page를 destination에서도 동일하게 fault시키는 용도로 사용할 수 있다. Correctness는 data만 복사하는 것이 아니라 failure state도 복제하는 것이다.

## CHAPTER 26 · Security policy는 kernel-mode fault interception 권한을 제한한다

Unprivileged userfaultfd가 kernel-context fault까지 다룰 수 있으면 attack surface가 커질 수 있다. Linux는 unprivileged use를 user-mode fault에 제한하는 정책과 `/dev/userfaultfd` permission model을 제공한다. Pager design은 기능 probe뿐 아니라 deployment privilege model을 명시해야 한다.

## CHAPTER 27 · unregister 전에 in-flight fault를 drain해야 한다

Range를 unregister/unmap하면서 handler가 그 address를 해결 중이면 stale ioctl, wrong mapping, use-after-free가 생길 수 있다. Teardown은 new fault generation 차단, event queue drain, in-flight workers cancel/join, unregister, unmap 순서의 state machine이 필요하다.

## CHAPTER 28 · Pager observability는 fault service time을 backing source별로 분해해야 한다

Fault count만으로 부족하다. Event enqueue→handler start→source fetch→copy/continue ioctl→wake→faulting thread scheduled까지 각 구간을 측정해야 한다. Zero-page/cache-hit/network/disk backing을 분리하면 pager 자체 bottleneck과 backing bottleneck을 구분할 수 있다.

## CHAPTER 29 · Fault storm은 ordinary request overload와 다른 positive feedback을 만들 수 있다

Handler가 느려지면 blocked threads가 많아지고, resumed threads가 한꺼번에 더 많은 missing pages를 touch해 새 fault burst를 만들 수 있다. Queue bound, fetch concurrency, prefetch, admission policy 없이 pager가 backing service를 과부하시킬 수 있다. Fault rate를 completion capacity에 맞춰 제어해야 한다.

## CHAPTER 30 · Userspace paging은 memory access를 distributed protocol로 바꾸는 기능이다

Robust 설계는 **feature negotiation, range lifetime, missing/minor/WP semantics, atomic page install, duplicate coalescing, handler scheduling, post-copy failure model, dirty tracking, security permission, teardown drain**을 하나의 protocol로 다룬다. `page fault를 userspace가 처리한다`는 유연성의 대가는 ordinary load/store가 userspace queue와 외부 backing service의 correctness에 의존하게 된다는 점이다.
