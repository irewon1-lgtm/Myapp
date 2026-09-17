# PART 20 · Virtualization — privilege, address translation, vCPU, device model

Virtual machine은 process isolation을 한 단계 더 크게 만든 기능이 아니다. guest operating system이 자신이 CPU·memory·device를 직접 소유한다고 가정하도록 **privileged execution, address translation, interrupt, device access의 일부를 hardware와 hypervisor가 중재**하는 실행 구조다. 성능과 정확성은 guest instruction 자체보다 VM exit 빈도, nested translation, vCPU scheduling, device emulation·paravirtualization, dirty-page tracking 같은 경계 비용에서 결정되는 경우가 많다.

---

## CHAPTER 01 · virtualization의 핵심은 privileged state를 누가 최종 소유하는가다

Guest kernel도 page table을 만들고 interrupt를 제어하며 device register를 다룬다고 가정한다. 그러나 실제 physical machine의 최종 privileged state는 host kernel·hypervisor가 통제해야 한다. Hardware virtualization은 guest가 제한된 execution mode에서 대부분의 ordinary instruction을 직접 실행하도록 두고, 특정 privileged event나 configured condition에서 host로 control을 넘긴다. 따라서 virtualization 비용을 `모든 instruction을 software가 흉내 낸다`로 이해하면 틀린다. Native-speed에 가까운 구간과 exit·emulation이 필요한 구간을 분리해야 한다. KVM 계열에서는 Linux kernel이 virtualization extension을 사용해 VM/vCPU execution을 제공하고 userspace VMM이 device model과 lifecycle 일부를 담당한다.

## CHAPTER 02 · VM entry와 VM exit는 실행 주체가 바뀌는 경계다

vCPU가 guest code를 실행하려면 host가 guest register state·control state를 준비하고 virtualization hardware에 진입한다. Guest 실행 중 특정 event가 발생하면 hardware는 exit reason과 guest state를 저장하고 host context로 돌아온다. Exit 원인은 architecture와 configuration에 따라 privileged instruction, I/O access, exception, interrupt window, page-translation fault 등 다양하다. 중요한 성능 지표는 단순 CPU utilization이 아니라 **어떤 exit가 얼마나 자주 발생하고 한 exit 처리에 얼마나 많은 host work가 필요한가**다. Exit-heavy workload는 guest 내부 instruction count가 작아도 host transition과 emulation 비용 때문에 latency가 커질 수 있다.

## CHAPTER 03 · KVM은 kernel과 userspace VMM 사이의 책임을 나눈다

Linux KVM API에서 `/dev/kvm`은 userspace VMM이 VM object와 vCPU를 만들고 memory region을 등록하며 execution loop를 제어하는 interface다. KVM kernel component는 CPU virtualization extension, interrupt injection, memory virtualization과 같은 privileged mechanism을 제공한다. QEMU 같은 VMM은 firmware, device model, virtual PCI topology, disk/network backend, migration protocol 같은 더 큰 machine model을 구성한다. 이 분리를 알아야 `KVM 성능`, `QEMU 성능`, `guest 성능`을 하나의 층으로 뭉개지 않는다. 같은 guest slowdown도 vCPU scheduling, exit handling, emulated device, host I/O backend 중 다른 원인에서 발생할 수 있다.

## CHAPTER 04 · vCPU는 physical CPU와 동일한 자원이 아니다

Guest가 8개의 CPU를 본다고 해서 8개의 physical core가 항상 전용으로 배정된다는 뜻은 아니다. vCPU는 host scheduler가 실행시키는 schedulable execution context다. Host가 더 많은 vCPU를 physical CPU에 overcommit하면 각 vCPU는 run queue에서 기다릴 수 있다. Guest 내부에서는 runnable thread가 있는데 CPU를 못 받은 시간으로 보이고, host에서는 vCPU thread가 descheduled된 상태다. 따라서 guest CPU 사용률만 보면 `CPU가 바쁘지 않다`고 오판할 수 있다. vCPU steal time, host run queue, CPU affinity, NUMA placement를 함께 봐야 virtualization scheduling delay를 분리할 수 있다.

## CHAPTER 05 · CPU pinning은 locality와 isolation을 얻는 대신 flexibility를 줄인다

vCPU thread를 특정 physical CPU set에 pinning하면 cache locality와 latency predictability를 높일 수 있고 noisy-neighbor 영향을 줄일 수 있다. 그러나 physical core 일부가 idle이어도 pinned vCPU가 다른 core를 사용할 수 없고, SMT sibling sharing이나 NUMA node 배치를 잘못하면 기대와 반대로 성능이 나빠질 수 있다. Pinning은 `고정하면 빠르다`가 아니라 **scheduler freedom, cache/NUMA locality, isolation, capacity utilization의 trade-off**다. latency-sensitive VM에서는 host interrupt·I/O thread까지 같은 core를 경쟁하는지 확인해야 한다.

## CHAPTER 06 · guest virtual address는 host physical address로 한 번에 변환되지 않는다

Guest process가 사용하는 guest virtual address는 먼저 guest page table을 통해 guest physical address로 변환된다. 그 guest physical address는 hypervisor가 관리하는 second-stage translation을 통해 host physical memory로 연결된다. Architecture에 따라 EPT, NPT, stage-2 translation 같은 이름이 사용된다. 결과적으로 memory access에는 **guest translation과 host-level translation이라는 두 계층**이 존재할 수 있다. Hardware가 nested page walk를 가속하지만 TLB miss가 발생하면 page-walk 비용이 커질 수 있다. VM memory 성능을 볼 때 guest page size와 host backing page size, nested-TLB reach를 함께 봐야 한다.

## CHAPTER 07 · nested page fault는 guest page fault와 다른 경계다

Guest virtual address가 guest page table에서 valid하지 않으면 guest OS가 처리할 page fault가 된다. 반면 guest physical address에 대한 second-stage mapping이 없거나 protection condition을 위반하면 hypervisor 측 fault/exit가 발생할 수 있다. 두 fault는 ownership이 다르다. Guest가 memory를 demand-allocate하는 과정, host가 backing page를 준비하는 과정, live migration dirty tracking이 write-protect를 거는 과정이 서로 다른 fault path를 만들 수 있다. `page fault 증가`라는 한 숫자만 보면 어느 translation layer에서 발생했는지 알 수 없다.

## CHAPTER 08 · huge page는 guest와 host 두 계층에서 각각 판단해야 한다

Guest가 huge page를 사용해도 host backing mapping이 작은 page로 쪼개져 있으면 second-stage translation의 TLB reach 이득이 제한될 수 있다. 반대로 host에서 large page를 쓰더라도 guest access pattern이 sparse하거나 memory compaction 비용이 크면 전체 성능이 좋아지지 않을 수 있다. Virtualization에서는 page-size decision이 두 translation layer와 연결되므로 `huge page=TLB miss 감소` 한 문장으로 끝나지 않는다. memory footprint, fragmentation, migration granularity, deduplication·ballooning 같은 기능과 충돌하는지도 확인한다.

## CHAPTER 09 · memory overcommit은 예약량과 resident backing을 분리한다

Host는 VM에 선언된 virtual RAM 총합보다 적은 physical RAM으로 여러 VM을 운용할 수 있다. 이 구조는 모든 guest가 peak memory를 동시에 쓰지 않는다는 가정에 의존한다. Host memory pressure가 높아지면 reclaim, swap, ballooning, VM OOM, host OOM 같은 여러 메커니즘이 실제 latency와 availability를 결정한다. Guest 내부 `free memory`와 host가 그 VM에 실제로 유지하는 resident memory는 같은 숫자가 아니다. 운영자는 guest-visible capacity와 host physical commitment를 별도로 추적해야 한다.

## CHAPTER 10 · ballooning은 guest가 host에 page를 돌려주는 협력 protocol이다

Memory balloon driver는 guest 안에서 page를 확보해 guest workload가 그 page를 사용하지 못하게 하고, hypervisor가 대응 host backing을 회수할 수 있도록 한다. Balloon이 커지면 guest 입장에서는 usable memory가 줄어 reclaim·swap pressure가 증가할 수 있다. Ballooning은 host가 guest address space를 무작정 빼앗는 것이 아니라 guest driver와 협력해 memory availability를 조절하는 paravirtual mechanism이다. Aggressive ballooning은 host consolidation 효율을 높이지만 guest tail latency와 OOM risk를 키울 수 있다.

## CHAPTER 11 · device virtualization은 trap-and-emulate만 있는 것이 아니다

Legacy device register를 software가 모두 emulation하면 guest access가 exit를 자주 유발하고 VMM이 device semantics를 재현해야 한다. Paravirtualized device는 guest가 virtualization-aware protocol을 사용해 더 적은 trap과 더 큰 batch로 요청을 전달하도록 설계할 수 있다. Virtio는 이 목적을 위한 표준화된 virtual device interface다. 성능 분석에서는 device type 이름보다 **request 제출 경로, notification 횟수, queue layout, copy path, backend I/O**를 확인해야 한다.

## CHAPTER 12 · virtqueue는 producer-consumer ownership protocol이다

Virtio device는 descriptor와 queue 구조를 통해 guest buffer를 device/backend에 전달한다. Split virtqueue와 packed virtqueue는 layout이 다르지만 공통적으로 descriptor ownership, availability, used/completion 상태를 명확히 관리해야 한다. Queue entry를 publish한 뒤 notification을 보내고 device가 처리한 뒤 completion을 관찰하는 과정에는 memory ordering 요구도 존재한다. Virtual I/O를 `가상 디스크 함수 호출`로 이해하면 queue full, descriptor exhaustion, notification storm, ordering bug를 설명할 수 없다.

## CHAPTER 13 · notification suppression은 interrupt storm을 줄이는 대신 latency trade-off를 만든다

매 request마다 guest↔host notification을 발생시키면 high-IOPS workload에서 transition cost가 커진다. Virtio와 backend는 batching·event suppression 같은 방식으로 notification 빈도를 줄일 수 있다. 그러나 completion을 너무 오래 batch하면 latency가 늘어난다. Polling을 도입하면 interrupt overhead를 줄일 수 있지만 CPU를 계속 소비한다. 따라서 I/O path는 throughput·latency·CPU budget 사이의 정책 문제이며, workload queue depth와 service time에 맞춰 조정해야 한다.

## CHAPTER 14 · virtio-blk와 virtio-scsi는 guest-visible contract가 다르다

Virtual block device를 제공하는 방식에도 여러 interface가 있다. 단순 block request model과 richer SCSI command model은 기능 범위와 queue semantics가 다르다. Guest filesystem이 보는 logical block device 성능은 virtio frontend뿐 아니라 host page cache, host filesystem, image format, storage backend, physical media queue까지 이어진다. `VM 디스크가 느리다`는 증상을 해결하려면 guest block queue와 host backend queue를 같은 timeline에 놓아야 한다.

## CHAPTER 15 · network virtualization은 packet copy와 scheduling boundary를 만든다

Guest network packet은 virtio-net queue, host networking stack, bridge/tap, firewall, physical NIC 같은 여러 layer를 통과할 수 있다. 각 layer에서 batching, segmentation offload, checksum offload, copy, queueing이 일어날 수 있다. Guest p99 network latency가 나빠졌을 때 guest TCP stack만 보면 host queueing이나 vCPU deschedule을 놓칠 수 있다. Packet timestamp를 guest와 host 양쪽에서 수집하고 clock domain 차이까지 보정해야 causal path를 복원할 수 있다.

## CHAPTER 16 · IOMMU는 device DMA에도 address isolation을 적용한다

Device passthrough나 DMA capable virtual I/O에서는 device가 host memory 어디든 임의로 접근하도록 둘 수 없다. IOMMU는 device-visible address를 physical memory mapping과 연결하고 access scope를 제한한다. Virtualization에서는 guest DMA address→IOMMU translation→host physical memory 경로가 추가될 수 있다. IOMMU mapping update와 TLB invalidation도 비용이 있다. Security isolation과 DMA performance는 같은 mechanism의 두 측면이다.

## CHAPTER 17 · device passthrough는 emulation을 줄이는 대신 mobility를 잃을 수 있다

Physical device나 SR-IOV virtual function을 guest에 직접에 가깝게 할당하면 software device model과 copy를 줄여 latency·throughput을 개선할 수 있다. 그러나 device state가 physical hardware에 더 강하게 묶이면서 snapshot, live migration, oversubscription, centralized policy가 어려워질 수 있다. Passthrough는 `가장 빠른 방식`이 아니라 **performance와 operational mobility의 교환**이다. 장애 복구 전략과 hardware replacement까지 포함해 선택해야 한다.

## CHAPTER 18 · interrupt virtualization은 guest와 host scheduling을 연결한다

Physical interrupt를 guest에 전달하려면 host가 event를 받아 target vCPU에 virtual interrupt state를 반영해야 한다. vCPU가 현재 다른 host CPU에서 실행 중이거나 descheduled되어 있으면 delivery timing이 달라진다. Modern hardware는 interrupt remapping과 posted interrupt 같은 acceleration을 제공할 수 있지만 architecture·configuration에 의존한다. I/O completion latency를 분석할 때 device service time뿐 아니라 interrupt injection과 target vCPU scheduling delay를 확인해야 한다.

## CHAPTER 19 · VM snapshot은 memory·device·storage consistency를 동시에 맞춰야 한다

VM memory만 복사하면 CPU register와 virtual device state, disk state와 일치하지 않을 수 있다. Snapshot은 어느 시점의 machine state를 복원 가능한 형태로 정의해야 한다. Crash-consistent snapshot은 guest가 갑자기 전원을 잃은 상태와 비슷한 보장을 제공할 수 있고, application-consistent snapshot은 guest application flush/quiesce 협력이 필요할 수 있다. `snapshot 성공`은 파일 생성 성공이 아니라 restore 후 cross-layer invariant가 유지되는지까지 검증해야 한다.

## CHAPTER 20 · live migration은 실행 중 dirty memory가 계속 생기는 문제다

Pre-copy migration은 VM이 실행되는 동안 memory page를 destination으로 복사하고, copy 중 다시 변경된 dirty page를 반복 전송한 뒤 짧은 stop-and-copy 단계로 전환할 수 있다. Dirty rate가 network copy capacity에 가까워지면 convergence가 느려지거나 실패할 수 있다. Post-copy 계열 전략은 destination 실행을 먼저 시작하고 필요한 page를 demand-fetch하는 대신 source/network failure risk가 달라진다. Migration 정책은 downtime, total migration time, bandwidth, dirty rate를 함께 본다.

## CHAPTER 21 · migration stream은 RAM만이 아니라 device state contract다

QEMU migration protocol은 RAM state 외에도 device section과 configuration state를 전송한다. Source와 destination의 machine type, device version, CPU feature compatibility가 맞지 않으면 byte transfer가 성공해도 guest를 올바르게 재개할 수 없다. Migration compatibility는 binary format compatibility와 유사하게 versioned contract다. Fleet upgrade에서 `새 QEMU에서도 예전 VM이 옮겨지는가`를 실제 migration matrix로 검증해야 한다.

## CHAPTER 22 · CPU model compatibility는 instruction availability와 state layout을 제한한다

Guest가 source host의 CPU feature를 사용한 뒤 destination이 그 feature를 제공하지 못하면 live migration 후 execution을 이어갈 수 없다. Cloud/cluster에서는 physical CPU의 모든 feature를 그대로 expose하기보다 migration domain 내 공통 CPU model을 제공할 수 있다. 그 대가로 최신 instruction과 performance feature 일부를 사용하지 못할 수 있다. CPU feature exposure는 performance·compatibility·security patchability의 정책이다.

## CHAPTER 23 · nested virtualization은 translation과 exit layer를 한 번 더 추가한다

L0 hypervisor 위의 L1 guest hypervisor가 L2 VM을 실행하는 nested virtualization에서는 privilege transition과 memory translation 계층이 더 복잡해진다. Hardware assist가 있더라도 L2 event가 L1 또는 L0 처리를 요구할 수 있고 nested page table composition 비용이 발생한다. `VM 안의 VM`이라는 기능 설명보다 **어느 layer가 exit를 소유하고 어떤 translation state를 shadow/combine하는가**가 성능과 correctness를 결정한다.

## CHAPTER 24 · confidential VM은 host trust model 자체를 바꾼다

일반 VM에서는 hypervisor가 guest memory를 inspect할 수 있다는 trust model이 흔하다. Hardware-backed confidential computing은 guest memory confidentiality/integrity를 host보다 더 강하게 보호하려는 기능을 제공할 수 있다. 그 결과 device I/O, attestation, debugging, live migration에 새로운 protocol과 제한이 생긴다. `VM이라 안전하다`와 `host도 guest plaintext를 볼 수 없다`는 전혀 다른 보안 보장이다. Threat model에서 host operator를 trusted principal로 포함할지 명시해야 한다.

## CHAPTER 25 · side-channel은 isolation boundary 밖의 shared hardware에서 발생할 수 있다

VM별 page table과 CPU privilege가 분리돼도 shared cache, branch predictor, memory bandwidth, SMT resource, power/thermal domain 같은 microarchitectural resource는 공유될 수 있다. Timing information을 통해 다른 workload activity를 추론하는 side-channel은 logical access control과 다른 문제다. Mitigation은 isolation level, scheduling, microcode/OS defense, performance cost를 함께 고려한다. Multi-tenant design에서는 `메모리 접근 불가`만으로 confidentiality를 완전히 설명할 수 없다.

## CHAPTER 26 · overcommit은 CPU·memory·I/O에서 각각 다른 failure mode를 만든다

CPU overcommit은 runnable vCPU queue와 steal time을 늘린다. Memory overcommit은 reclaim·swap·balloon·OOM pressure를 만든다. Storage/network overcommit은 backend queue와 tail latency를 증폭시킨다. 하나의 `oversubscription ratio`로 세 자원을 동시에 설명할 수 없다. Capacity planning은 vCPU:pCPU, committed/resident memory, IOPS/bandwidth, burst correlation을 분리하고 worst-case co-tenancy를 포함해야 한다.

## CHAPTER 27 · NUMA-aware VM placement는 vCPU와 memory와 device locality를 동시에 본다

Multi-socket host에서 VM memory가 한 NUMA node에 있고 vCPU가 다른 node에서 실행되면 remote memory access가 증가할 수 있다. Passthrough device도 특정 NUMA node와 더 가까울 수 있다. vCPU pinning만 하고 memory policy를 맞추지 않으면 locality가 깨진다. VM topology를 guest에 어떻게 expose할지도 guest scheduler와 allocator decision에 영향을 준다. Performance tuning은 host NUMA topology와 guest topology를 함께 모델링해야 한다.

## CHAPTER 28 · VM 관측은 guest와 host 두 timeline을 연결해야 한다

Guest metric은 guest가 본 runnable thread, page fault, I/O completion을 보여 주지만 host deschedule·backend queue·VM exit를 숨길 수 있다. Host metric은 vCPU thread와 backend를 보여 주지만 guest application semantic을 모른다. 장애 분석은 guest request ID·guest trace·host vCPU schedule·KVM exit·QEMU I/O·physical device completion을 가능한 공통 clock 기준으로 연결해야 한다. Virtualization은 관측 boundary를 하나 더 만든다.

## CHAPTER 29 · VM 성능 회귀는 exit·translation·queue·scheduling을 분해해 검증한다

Benchmark가 느려졌을 때 먼저 guest IPC만 비교하면 원인을 놓칠 수 있다. 최소 증거는 vCPU run/steal, VM-exit count와 reason, nested page/TLB behavior, virtqueue depth, host backend latency, CPU frequency·NUMA placement를 포함해야 한다. 동일 workload에서 hypervisor/version/device-model 설정을 한 변수씩 바꾸고 counter 변화와 latency distribution을 함께 본다. Virtualization tuning도 hypothesis→measurement→regression gate 순서로 관리해야 한다.

## CHAPTER 30 · virtualization 설계 검토는 실행·memory·device·mobility·trust를 동시에 고정한다

운영 VM은 다섯 계약을 동시에 만족해야 한다. **Execution**: vCPU scheduling과 feature exposure가 workload 요구를 만족한다. **Memory**: nested translation과 overcommit 정책이 latency·OOM 경계를 정의한다. **Device**: emulation/paravirtualization/passthrough 선택이 queue와 isolation을 정의한다. **Mobility**: snapshot·migration compatibility가 장애·maintenance 전략을 보장한다. **Trust**: hypervisor·host operator·shared hardware를 어디까지 신뢰하는지 명시한다. 이 다섯 축 중 하나를 문서화하지 않으면 성능 최적화가 availability나 security를 깨뜨릴 수 있다.
