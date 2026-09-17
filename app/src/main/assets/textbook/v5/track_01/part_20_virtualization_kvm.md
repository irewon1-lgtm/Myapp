# PART 20 · Virtualization — KVM, nested translation, virtio, migration

가상화는 guest가 독립 machine처럼 보이게 만드는 기술이지만 실제 실행은 host scheduler, nested translation, device virtualization, migration state가 함께 만든다. 성능과 correctness를 이해하려면 guest 내부 수치만 보지 않고 guest↔hypervisor↔host↔device의 state transition을 연결해야 한다.

---

## CHAPTER 01 · privileged state

virtualization은 guest가 privileged state를 직접 소유하는 것처럼 보이게 하면서 실제 control은 hypervisor와 hardware virtualization extension이 분담한다. guest가 보는 control register, interrupt state, page-table root가 실제 hardware state와 언제 동기화되는지가 correctness의 핵심이다.

privileged-state 전환이 잦아지면 trap과 emulation 비용이 커질 수 있다. guest 내부에서는 평범한 instruction처럼 보여도 host에서는 exit를 유발할 수 있으므로 단순 instruction count만으로 비용을 설명하면 안 된다. capability와 privilege state가 migration 뒤에도 동일하게 재현되는지도 확인한다.

진단은 exit reason, guest register snapshot, host KVM trace를 같은 시점에 연결한다. 수정 후 exit frequency나 emulation path가 예상대로 줄었는지 확인하고, isolation과 compatibility가 유지되는지 regression test로 검증한다.

---

## CHAPTER 02 · VM entry/exit

VM entry는 host에서 guest execution으로 들어가고 VM exit는 특정 event 때문에 다시 host control로 돌아오는 전환이다. privileged instruction, interrupt, page fault, device access가 exit 원인이 될 수 있으며 한 번의 exit에는 state save/restore와 handler 실행이 포함된다.

exit가 너무 많으면 guest CPU utilization은 높지 않은데 wall latency가 커질 수 있다. 특히 timer, virtual interrupt, MMIO emulation이 반복되면 context boundary 자체가 bottleneck이 된다. exit 원인을 합계 하나로 보지 말고 class별 frequency와 duration을 분리해야 한다.

KVM trace와 host scheduler를 같이 보면 guest가 실제로 실행한 시간과 exit 처리·host wait 시간을 분리할 수 있다. optimization은 exit count만 낮추는 것이 아니라 guest-visible semantics와 interrupt ordering을 보존해야 한다.

---

## CHAPTER 03 · KVM/VMM boundary

KVM은 CPU·memory virtualization의 kernel path를 제공하고 userspace VMM은 device model, firmware, migration orchestration을 맡는 경우가 많다. 같은 VM failure라도 kernel accelerator와 userspace emulator 중 어느 boundary에서 state가 어긋났는지 구분해야 한다.

VMM이 device command를 늦게 처리하면 guest에서는 device latency로 보이고, KVM에서 vCPU가 오래 runnable 대기하면 guest에서는 CPU stall처럼 보일 수 있다. 두 층의 metric을 따로 보면 원인이 사라진다.

incident에는 VMM version, KVM/kernel version, VM config, vCPU thread ID, device backend를 함께 남긴다. 수정은 kernel/userspace 양쪽 contract를 깨지 않는 최소 boundary에 적용한다.

---

## CHAPTER 04 · vCPU scheduling

vCPU는 guest 안에서는 processor처럼 보이지만 host에서는 scheduler가 실행시키는 thread다. guest runnable task가 있어도 vCPU thread가 host runqueue에서 기다리면 guest는 CPU가 느린 것으로 관찰한다. oversubscription과 noisy neighbor가 이 지연을 확대한다.

steal-like time, host runnable delay, vCPU migration을 구분해야 한다. guest CPU 사용률만으로 capacity를 판단하면 host contention을 놓칠 수 있다. RT나 latency-sensitive VM은 평균 CPU share보다 worst-case scheduling delay가 더 중요하다.

host scheduler trace와 guest workload timestamp를 correlation한다. vCPU count를 늘렸을 때 throughput이 늘지 않고 runqueue delay만 커지면 실제 physical capacity가 한계라는 증거다.

---

## CHAPTER 05 · CPU pinning

CPU pinning은 vCPU thread가 실행될 host CPU 집합을 제한해 migration과 cache locality 변동을 줄인다. NUMA placement와 interrupt affinity까지 맞추면 latency jitter를 낮출 수 있지만 잘못된 pinning은 idle CPU가 있어도 특정 core를 포화시킨다.

pinning은 isolation이 아니라 placement policy다. sibling SMT를 공유하거나 같은 NUMA node에 다른 heavy task가 있으면 기대한 전용 capacity가 나오지 않을 수 있다. reserved CPU와 housekeeping task의 관계도 확인해야 한다.

benchmark에는 affinity map, sibling topology, NUMA node, background load를 기록한다. pinning 전후의 migration, cache miss, runqueue delay가 함께 개선되는지 확인한다.

---

## CHAPTER 06 · nested translation

guest virtual address는 guest page table을 거쳐 guest physical address가 되고 다시 host translation을 거쳐 실제 machine frame으로 연결된다. hardware nested paging은 이 두 단계를 결합하지만 TLB miss와 page walk 비용은 여전히 커질 수 있다.

large working set이나 fragmented mapping은 nested walk를 자주 유발한다. guest 안의 TLB metric만 보면 host EPT/NPT miss 비용을 놓칠 수 있다. page size와 host backing layout도 translation cost에 직접 영향을 준다.

PMU/KVM fault data와 host page-table state를 함께 확인한다. huge page 적용 시 TLB miss가 줄어도 compaction·fragmentation·NUMA 문제가 새로 생기지 않는지 같이 측정한다.

---

## CHAPTER 07 · nested faults

nested fault는 guest page table fault와 host backing fault를 분리해야 한다. 같은 guest address가 invalid해 보여도 실제로는 host가 backing page를 준비하거나 protection을 변경하는 중일 수 있다.

fault handler가 page를 resolve할 때 vCPU는 멈추므로 fault burst는 tail latency를 만든다. memory overcommit, swap, post-copy migration이 있으면 nested fault cost가 훨씬 커질 수 있다.

fault address, access type, guest PTE, host mapping, backing state를 한 사건으로 기록한다. fault count보다 fault service time과 refault 패턴을 보는 편이 원인 분석에 강하다.

---

## CHAPTER 08 · huge pages

huge page는 적은 TLB entry로 큰 guest memory range를 cover해 nested translation pressure를 줄일 수 있다. 하지만 host에서 큰 contiguous physical region을 확보해야 하므로 fragmentation과 compaction cost가 증가할 수 있다.

VM startup 때 확보는 잘 되지만 장시간 uptime 뒤 allocation이 실패하는 경우도 있다. NUMA locality가 틀리면 TLB 이득보다 remote memory cost가 더 커질 수 있다.

huge-page ratio, TLB miss, compaction stall, NUMA traffic을 함께 측정한다. page size 하나만 바꾸고 latency가 좋아졌다는 결과를 전체 workload에 일반화하지 않는다.

---

## CHAPTER 09 · memory overcommit

memory overcommit은 여러 VM이 peak를 동시에 사용하지 않는다는 가정으로 physical capacity보다 큰 virtual memory를 제공한다. 평상시 density는 높아지지만 correlated peak에서는 host reclaim, swap, OOM이 동시에 발생할 수 있다.

VM별 configured memory 합계보다 실제 working set과 reclaimability가 중요하다. ballooning이나 host swap이 동작해도 latency-sensitive guest에서는 SLA를 이미 위반했을 수 있다.

host free memory, PSI/reclaim, guest major fault, balloon size를 같은 시간축에 놓는다. capacity 계획에는 normal peak뿐 아니라 replica loss나 burst가 겹치는 failure scenario를 포함한다.

---

## CHAPTER 10 · ballooning

balloon driver는 guest 내부 page를 확보해 host가 그 backing을 회수할 수 있게 한다. host가 직접 guest의 중요 page를 모르는 문제를 guest allocator와 협력해 해결하지만 guest 안에서는 memory pressure와 reclaim이 발생한다.

balloon을 빠르게 키우면 guest page cache가 급격히 줄고 swap이나 GC pressure가 커질 수 있다. 반대로 너무 늦게 반응하면 host가 먼저 critical pressure에 들어간다.

balloon target, actual balloon size, guest reclaim, host pressure를 함께 본다. policy 변경은 guest latency와 host density를 동시에 측정해 판단한다.

---

## CHAPTER 11 · device virtualization

device virtualization은 full emulation, paravirtual device, passthrough처럼 다른 구조를 가진다. full emulation은 compatibility가 넓지만 trap 비용이 크고, virtio 계열은 guest-driver와 host-backend가 shared queue protocol을 사용한다.

passthrough는 빠르지만 migration과 isolation, reset ownership이 복잡해진다. 따라서 device choice는 단순 throughput 순위가 아니라 lifecycle과 failure domain까지 포함해 결정한다.

device path를 guest driver→virtqueue/MMIO→host backend→physical device로 추적한다. 어느 단계에서 queue가 쌓였는지 확인하면 같은 I/O latency도 원인을 분리할 수 있다.

---

## CHAPTER 12 · virtqueue

virtqueue는 descriptor와 available/used ring으로 guest와 host 사이 buffer ownership을 교환한다. descriptor를 publish하기 전에 buffer content가 visible해야 하고 completion 전에는 guest가 해당 buffer를 재사용하면 안 된다.

ring index wrap, stale descriptor, notification race는 data corruption이나 duplicate completion을 만들 수 있다. queue full 상태를 무한 retry로 처리하면 host CPU만 소비하고 progress는 없을 수 있다.

queue depth, kick/interrupt 수, completion latency, descriptor generation을 기록한다. correctness는 index arithmetic보다 ownership transition으로 증명한다.

---

## CHAPTER 13 · notification

virtio notification은 guest kick과 host interrupt를 통해 queue state 변화가 있음을 알린다. suppression이나 batching은 event 수를 줄이지만 너무 공격적이면 completion이 늦어져 tail latency가 증가한다.

notification은 data 자체가 아니라 state 변화 신호다. signal을 놓쳐도 shared ring state를 다시 확인해 progress할 수 있는 protocol이 필요하다.

kick/interrupt rate, batch size, queue residence time을 함께 측정한다. 최적 설정은 최대 batching이 아니라 latency SLO 안에서 event overhead를 줄이는 지점이다.

---

## CHAPTER 14 · block virtualization

virtual block path는 guest filesystem, virtual disk, host file/block layer, physical storage를 연속으로 지난다. 각 층에 queue와 cache가 생길 수 있어 guest의 한 write가 host에서는 여러 request로 변환될 수 있다.

fsync 같은 durability 요청도 guest virtual device와 host backing storage까지 실제로 전달되어야 의미가 있다. host page cache에서 끝난 completion을 guest가 durable commit으로 오해하면 crash consistency가 깨진다.

guest request ID와 host block request를 연결해 queue latency와 flush를 추적한다. benchmark는 backing type과 cache mode를 반드시 함께 기록한다.

---

## CHAPTER 15 · network virtualization

virtual network path는 guest NIC queue, vhost/tap, bridge 또는 routing, host NIC로 이어진다. packet copy보다 CPU placement, batching, offload, queue mapping이 성능을 크게 좌우할 수 있다.

vCPU와 vhost thread가 다른 NUMA node에 있으면 packet마다 remote memory traffic이 늘어난다. GRO/TSO 같은 offload가 켜지면 packet count metric 해석도 달라진다.

RX/TX queue, vhost CPU, drop location, socket queue를 같은 flow에 연결한다. guest packet loss를 곧바로 physical network 문제로 단정하지 않는다.

---

## CHAPTER 16 · IOMMU

IOMMU는 device DMA address space를 host physical memory와 분리해 passthrough device가 허용된 page에만 접근하게 한다. translation과 isolation을 동시에 제공하므로 mapping lifecycle이 security boundary다.

DMA가 끝나기 전에 mapping을 해제하거나 다른 guest에 page를 재사용하면 stale DMA가 잘못된 memory를 건드릴 수 있다. device reset과 outstanding request drain도 같은 lifetime에 포함한다.

IOMMU fault log, device BDF, IOVA mapping, guest ownership generation을 연결해 조사한다. 성능 최적화로 isolation을 우회하지 않는다.

---

## CHAPTER 17 · passthrough

passthrough는 guest가 physical device에 가까운 경로를 사용해 virtualization overhead를 줄인다. 대신 device를 한 VM이 독점하거나 SR-IOV 같은 hardware partition에 의존하며 migration과 reset이 어렵다.

device firmware bug나 DMA error가 guest boundary를 넘어 host reliability에 영향을 줄 수도 있다. hot-unplug 전에 guest driver와 host IOMMU state가 안전하게 정리되어야 한다.

성능 측정에는 host interrupt, DMA fault, NUMA placement를 포함한다. throughput 이득만 보고 operability와 recovery 비용을 무시하지 않는다.

---

## CHAPTER 18 · interrupt virtualization

physical interrupt는 host에서 처리된 뒤 guest-visible virtual interrupt로 전달된다. posted interrupt나 virtual APIC 같은 mechanism은 VM exit를 줄일 수 있지만 delivery ordering과 target vCPU state를 맞춰야 한다.

vCPU가 descheduled된 상태에서 interrupt가 쌓이면 guest service latency가 늘 수 있다. interrupt coalescing이 지나치면 guest는 device가 느린 것으로 관찰한다.

interrupt injection timestamp, vCPU runnable/run state, guest handler 시작 시각을 연결한다. raw interrupt count보다 delivery latency를 본다.

---

## CHAPTER 19 · snapshot consistency

VM snapshot은 disk image만 복사하는 작업이 아니다. CPU register, RAM, virtual device state가 동일 logical instant를 나타내야 resume 후 protocol과 filesystem invariant가 유지된다.

application-level transaction이 진행 중이면 crash-consistent snapshot과 application-consistent snapshot의 의미도 다르다. quiesce 없이 메모리와 disk를 서로 다른 시점에 잡으면 정상 restore가 안 될 수 있다.

snapshot generation, device state, guest filesystem freeze 여부를 기록한다. restore test는 실제 snapshot artifact로 수행해 metadata만 검증하는 실수를 피한다.

---

## CHAPTER 20 · live migration

live migration은 VM이 실행되는 동안 memory와 device state를 source에서 destination으로 옮긴다. pre-copy에서는 dirty page rate가 transfer bandwidth보다 높으면 수렴하지 못하고 downtime이 길어진다.

network jitter, huge page, device passthrough, pinned memory가 migration 가능성과 속도를 바꾼다. destination CPU feature가 부족하면 migration 후 illegal instruction이나 behavior 차이가 생길 수 있다.

round별 transferred bytes, dirty rate, stop-and-copy duration, destination resume latency를 측정한다. 성공 여부뿐 아니라 service interruption을 SLO로 관리한다.

---

## CHAPTER 21 · migration stream

migration stream은 VM state를 versioned field와 section으로 직렬화한 protocol이다. source와 destination VMM이 같은 field semantics를 이해해야 하며 optional feature의 negotiation이 필요하다.

stream 일부가 손상되거나 순서가 어긋나면 restore가 중간에 실패할 수 있다. compatibility를 filename이나 VMM version string만으로 추정하면 안 된다.

stream version, machine type, device model, CPU feature를 artifact metadata로 보존한다. rolling upgrade 환경에서는 old→new와 new→old 방향을 분리해 테스트한다.

---

## CHAPTER 22 · CPU model compatibility

migration 가능한 VM은 source에서 노출된 instruction feature가 destination에서도 유지되어야 한다. destination CPU가 더 최신이어도 source guest가 보던 exact model contract가 자동 보장되는 것은 아니다.

host-passthrough는 성능에는 유리할 수 있지만 heterogeneous cluster migration 범위를 줄인다. baseline CPU model은 mobility와 feature access 사이 trade-off다.

CPUID/feature bitmap을 migration artifact와 함께 기록한다. cluster upgrade 전에는 실제 guest workload로 instruction compatibility를 확인한다.

---

## CHAPTER 23 · nested virtualization

nested virtualization은 L1 guest가 다시 hypervisor가 되어 L2 guest를 실행하는 구조다. VM exit와 nested translation이 한 단계 더 겹쳐 performance와 state machine이 복잡해진다.

L0가 어떤 virtualization feature를 L1에 노출할지 정확해야 하고, L1이 설정한 control state를 L0가 합성해야 한다. corner case는 단순 guest workload에서 드러나지 않을 수 있다.

L0/L1/L2 exit reason과 feature set을 분리해 기록한다. 기능이 동작한다는 것과 production latency가 감당 가능한 것은 별개다.

---

## CHAPTER 24 · confidential VM

confidential VM은 guest memory를 host administrator나 hypervisor로부터 더 강하게 보호하는 방향으로 trust boundary를 바꾼다. encryption과 attestation이 추가되지만 device I/O와 debugging은 더 복잡해진다.

host가 plaintext memory를 직접 보지 못하면 기존 dump와 introspection 도구가 제한될 수 있다. attestation report가 어떤 firmware·measurement를 증명하는지도 명확히 해야 한다.

security 설계에는 key lifecycle, attestation verifier, shared buffer 경계를 포함한다. confidential이라는 이름만으로 side channel과 availability 문제가 해결되지는 않는다.

---

## CHAPTER 25 · side channel

VM isolation은 architectural memory access를 막아도 cache, branch predictor, shared execution unit 같은 microarchitectural state를 완전히 분리하지 못할 수 있다. timing 차이를 이용한 side channel이 이 경계를 노린다.

SMT sharing, co-location, predictor state가 threat model에 들어가는지 판단해야 한다. mitigation은 isolation을 강화하지만 throughput과 density를 낮출 수 있다.

security test에는 secret-dependent timing과 co-tenant placement를 포함한다. mitigation 적용 후에도 residual leakage와 성능 비용을 각각 측정한다.

---

## CHAPTER 26 · overcommit modes

CPU, memory, I/O overcommit은 서로 다른 failure behavior를 가진다. CPU는 runqueue delay로, memory는 reclaim/OOM으로, I/O는 queue latency로 나타나므로 하나의 oversubscription ratio로 설명할 수 없다.

각 resource의 burst와 correlated peak를 고려해야 한다. CPU headroom이 충분해도 storage가 포화되면 VM 전체 tail latency가 무너질 수 있다.

capacity model에는 resource별 utilization, saturation, recovery time을 넣는다. 정상 평균이 아니라 failure scenario에서 headroom이 유지되는지 본다.

---

## CHAPTER 27 · NUMA placement

vCPU와 guest memory backing이 같은 NUMA node에 가까우면 remote memory access를 줄일 수 있다. 큰 VM을 여러 node에 걸쳐 배치할 때는 vCPU pinning과 memory policy를 함께 설계해야 한다.

first-touch나 host migration으로 page가 예상과 다른 node에 몰릴 수 있다. virtual NUMA topology를 guest에 노출할 때 실제 host placement와 지나치게 어긋나면 guest 내부 allocator도 잘못된 선택을 한다.

node별 memory bandwidth, remote access, vCPU migration을 측정한다. NUMA tuning은 CPU affinity 한 줄로 끝나지 않는다.

---

## CHAPTER 28 · cross-layer observability

virtualized workload의 symptom은 guest, KVM, VMM, host scheduler, physical device 중 어느 층에서도 시작될 수 있다. 각 층의 timestamp와 request identity를 연결해야 causal chain을 복원할 수 있다.

guest log만 보면 host deschedule이나 migration pause가 application stall처럼 보인다. 반대로 host CPU spike가 guest 내부 lock contention 때문일 수도 있다.

관측 도구의 clock origin과 sampling loss를 기록한다. cross-layer trace는 데이터 양보다 동일 사건을 정확히 correlation하는 것이 중요하다.

---

## CHAPTER 29 · virtualization regression

virtualization regression은 guest software, host kernel, VMM, firmware, microcode 변경을 각각 분리해 봐야 한다. 같은 VM image라도 host stack 변화만으로 latency와 instruction behavior가 달라질 수 있다.

benchmark는 host type과 exact build를 고정하고 한 변수씩 바꾼다. warm cache나 migration history가 결과를 오염시키지 않게 초기 상태도 통제한다.

regression이 발견되면 guest metric과 host counter가 함께 변했는지 확인한다. 단순 version rollback보다 어느 boundary에서 cost가 추가됐는지 증명하는 편이 재발 방지에 강하다.

---

## CHAPTER 30 · design contract

virtualization contract는 CPU feature, memory mapping, device ownership, migration compatibility, isolation을 각각 명시해야 한다. hidden sharing이 많을수록 성능과 failure가 다른 VM으로 전파된다.

설계 시 정상 실행뿐 아니라 host reboot, device reset, migration 중 failure, memory pressure를 포함한다. 어떤 state가 durable하고 어떤 state는 재생성 가능한지 구분해야 recovery가 가능하다.

운영 gate는 guest-visible semantics와 host resource accounting을 함께 검증한다. density를 높이는 최적화가 isolation이나 rollback을 약화시키면 전체 contract 관점에서 실패다.
