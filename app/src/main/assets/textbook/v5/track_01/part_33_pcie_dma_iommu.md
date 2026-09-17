# PART 33 · PCIe, DMA and IOMMU — device discovery, address translation, isolation

PCIe device를 붙인다는 것은 단순히 bus에 hardware를 보이는 일이 아니다. enumeration으로 identity와 resource를 확정하고, MMIO와 interrupt 경계를 열며, DMA가 어떤 address space를 사용할지 정의하고, IOMMU와 reset 정책으로 failure domain을 제한해야 한다. device assignment와 SR-IOV까지 가면 **성능, isolation, ownership, recovery**가 하나의 계약으로 묶인다.

---

## CHAPTER 01 · PCIe enumeration은 존재하는 function을 software resource graph로 바꾼다

PCIe hierarchy에서 host는 bus·device·function을 탐색하고 vendor/device identity와 capability를 읽어 software-visible device를 만든다. enumeration은 이름표를 붙이는 절차가 아니라 이후 MMIO window, interrupt vector, driver binding이 의존할 topology를 확정하는 단계다. hot-plug나 firmware 설정에 따라 동일한 물리 장치도 다른 bus number를 받을 수 있으므로 숫자를 영구 identity로 취급하면 안 된다.

장애 분석에서는 “device가 안 보인다”를 한 단계로 묶지 않는다. link가 올라왔는지, config space 접근이 되는지, function이 enumerated됐는지, driver가 bind됐는지를 순서대로 나눈다. 동일 모델 두 장이 있을 때는 BDF만 아니라 slot·serial·firmware identity까지 연결해야 교체와 재부팅 뒤에도 같은 장치를 추적할 수 있다.

관측 증거는 boot log의 enumeration, config header, negotiated link width/speed, driver probe 시각을 같은 timeline에 놓는다. firmware update 뒤 장치 수가 달라졌다면 application부터 조사하지 않고 bus topology와 resource assignment 차이를 먼저 비교한다.

---

## CHAPTER 02 · configuration space는 device capability와 resource 요구를 노출한다

PCIe configuration space에는 device identity, command/status, BAR, capability와 extended capability가 들어 있다. 운영체제와 driver는 이 정보를 읽어 장치가 어떤 address window와 interrupt mechanism을 지원하는지 판단한다. config 값은 단순 진단 표가 아니라 실제 enable sequence와 보안 경계의 입력이다.

bus mastering을 너무 일찍 허용하면 IOMMU domain이나 buffer 준비 전에 장치가 DMA를 시작할 수 있다. 반대로 memory-space enable이 빠져 있으면 BAR가 배치돼도 MMIO가 동작하지 않는다. capability chain이 손상되거나 firmware가 잘못된 size를 보고하면 resource allocator가 전체 topology를 비정상 배치할 수 있다.

문제 재현 시 vendor/device ID, command register, BAR size, MSI-X/IOMMU 관련 capability를 snapshot으로 보존한다. 정상 장치와 실패 장치의 config dump를 동일 firmware·kernel 조건에서 비교하면 driver 내부를 보기 전에 hardware description 차이를 좁힐 수 있다.

---

## CHAPTER 03 · BAR는 device register window를 CPU address space에 배치한다

Base Address Register는 device가 필요로 하는 MMIO 또는 I/O resource 범위를 표현하고 host가 system address에 배치할 수 있게 한다. BAR에 보이는 address는 일반 DRAM pointer와 같은 memory semantics를 갖지 않는다. register read/write는 device state machine과 연결되며 width, alignment, ordering 규칙을 따라야 한다.

잘못된 BAR size나 overlapping window는 다른 device resource와 충돌하고, hot-plug 뒤 재배치가 일어나면 hard-coded address를 사용하는 code가 즉시 깨진다. 32-bit와 64-bit BAR 조합, prefetchable 속성도 mapping 정책에 영향을 준다. userspace passthrough에서는 해당 window를 누구에게 노출할지 isolation 문제가 추가된다.

진단은 resource tree와 BAR mapping, page protection을 함께 확인한다. MMIO fault가 나면 pointer arithmetic부터 보기보다 device가 실제로 enabled되어 있고 해당 physical range가 올바른 function에 배정됐는지 검증한다.

---

## CHAPTER 04 · MMIO access는 ordinary memory access와 ordering 계약이 다르다

memory-mapped register는 load/store instruction으로 접근할 수 있어도 compiler와 CPU가 일반 cacheable memory처럼 자유롭게 다뤄도 된다는 뜻은 아니다. architecture와 OS accessor가 제공하는 ordering, width, endianness, barrier semantics를 따라야 한다. register read가 side effect를 갖거나 write-one-to-clear 같은 규칙을 가지면 단순한 변수 대입 모델이 깨진다.

직접 volatile pointer만 사용하면 compiler reorder 일부를 막더라도 device ordering과 interconnect semantics가 충분히 보장되지 않을 수 있다. descriptor를 DRAM에 기록한 뒤 doorbell을 울리는 경로에서는 descriptor visibility가 먼저 보장되어야 한다. read-modify-write가 금지된 register에 generic bit update helper를 쓰는 것도 위험하다.

trace에는 register access 주소만 아니라 operation 전후의 descriptor state와 barrier 위치를 남긴다. rare hang은 CPU 값은 맞는데 device가 old descriptor를 본 ordering bug로 나타날 수 있으므로 data publication과 MMIO write를 한 사건으로 분석한다.

---

## CHAPTER 05 · posted write는 CPU 완료와 device 관찰 시점을 분리한다

PCIe write는 CPU가 transaction을 interconnect에 넘긴 뒤 target device의 최종 처리까지 기다리지 않고 진행할 수 있다. posted write는 throughput에 유리하지만 “store instruction이 끝났다”와 “device가 register를 반영했다”를 동일하게 만들지 않는다. 특정 register sequence가 device-visible ordering을 요구하면 platform 규칙에 따른 flush나 read-back이 필요할 수 있다.

reset command 직후 상태 register를 읽거나, doorbell 뒤 buffer 재사용을 하는 code에서 이 차이를 놓치면 race가 생긴다. 특히 bridge와 virtualization layer가 사이에 있으면 지연 구간이 늘어나며 timing에 의존한 sleep은 correctness proof가 아니다.

관측에서는 host timestamp와 device completion 또는 status transition을 분리한다. write 직후 강제 read-back을 넣었을 때만 문제가 사라진다면 posted-write visibility 가설을 세우고 문서화된 ordering primitive로 교정한다.

---

## CHAPTER 06 · doorbell publication은 descriptor 준비와 device 통지를 하나의 protocol로 묶는다

queue 기반 장치는 host memory에 descriptor를 쓰고 MMIO doorbell로 새 work를 알리는 구조를 자주 사용한다. correctness 조건은 descriptor의 모든 field와 referenced buffer가 device에 보이기 전에 doorbell이 먼저 관찰되지 않는 것이다. CPU cache coherency가 있더라도 memory ordering과 DMA ownership 전환은 별도 문제다.

producer index만 먼저 업데이트하거나 buffer contents가 아직 수정 중인데 doorbell을 울리면 device가 partial request를 읽을 수 있다. 반대로 completion 전에 descriptor slot을 재사용하면 old DMA와 new request가 동일 memory를 공유한다. queue wraparound에서는 generation bit나 producer/consumer index의 범위를 정확히 관리해야 한다.

검증은 descriptor sequence, doorbell value, completion index를 함께 기록한다. fault injection으로 barrier 제거·지연을 흉내 내면 ordering 의존성을 드러낼 수 있고, 정상 path뿐 아니라 reset 후 재개 시 queue generation도 확인해야 한다.

---

## CHAPTER 07 · MSI-X는 interrupt vector를 queue와 CPU topology에 맞게 분산한다

MSI-X는 device가 여러 message-signaled interrupt vector를 사용하게 해 multi-queue 장치의 completion을 서로 다른 CPU에 배치할 수 있게 한다. vector 수가 많다고 자동으로 빠른 것은 아니다. queue affinity, NUMA locality, interrupt moderation이 맞지 않으면 cache migration과 scheduler overhead가 증가한다.

모든 vector를 한 CPU에 몰면 multi-queue의 병렬성이 사라지고, 너무 세분하면 interrupt rate와 bookkeeping이 커질 수 있다. vector table programming과 device enable 순서가 틀리면 interrupt가 누락되거나 예상하지 않은 handler로 갈 수 있다.

운영에서는 vector별 interrupt count, handler CPU, queue completion rate를 연결한다. throughput imbalance가 있으면 application thread affinity와 IRQ affinity를 같이 보고, 단순히 IRQ 수만 늘리는 조정은 피한다.

---

## CHAPTER 08 · interrupt remapping은 device가 발생시키는 interrupt의 target을 격리한다

device passthrough 환경에서는 DMA뿐 아니라 interrupt도 guest나 process boundary를 넘어 잘못 전달되지 않게 제어해야 한다. interrupt remapping은 device identity와 허용 target 사이의 mapping을 관리해 arbitrary vector injection 위험을 줄이는 역할을 한다. 이 경계가 없다면 untrusted device가 다른 execution context에 영향을 줄 수 있다.

IOMMU만 켰다고 interrupt 경계까지 자동 해결된다고 가정하면 안 된다. platform capability와 virtualization stack 설정이 함께 맞아야 한다. reset이나 hot-unplug 중 stale mapping이 남으면 제거된 device의 event가 새 owner에게 도달할 수 있다.

진단에서는 remapping enable 상태, source identity, assigned vector를 보존한다. passthrough VM에서 이상한 interrupt storm이 보이면 guest driver뿐 아니라 host remapping table과 device reset sequence를 함께 확인한다.

---

## CHAPTER 09 · DMA agent는 CPU 밖의 독립적인 memory 접근 주체다

DMA-capable device는 CPU instruction stream을 거치지 않고 system memory를 읽고 쓸 수 있다. 따라서 CPU page permission만으로 device access를 통제할 수 없다. buffer를 DMA에 넘기는 순간부터 device가 언제 읽고 쓰는지, CPU가 언제 다시 소유권을 회수하는지 별도 protocol이 필요하다.

CPU가 buffer를 수정하는 동안 device가 동시에 읽거나, completion 전에 free된 page가 다른 object로 재사용되면 silent corruption이 가능하다. non-coherent architecture에서는 cache maintenance까지 필요할 수 있다. long-term pin은 VM reclaim과 migration에도 영향을 준다.

관측은 DMA mapping lifetime, request ID, completion과 buffer allocator generation을 연결한다. use-after-free가 의심되면 crash stack만 보지 말고 device가 마지막으로 해당 IOVA를 사용한 시점을 추적한다.

---

## CHAPTER 10 · DMA address는 CPU virtual address와 동일하지 않다

driver가 가진 userspace/kernel virtual address, physical address, device-visible DMA address는 서로 다른 namespace일 수 있다. IOMMU가 있으면 device는 IOVA를 사용하고, mapping API가 이를 physical page와 연결한다. CPU pointer를 그대로 device descriptor에 넣는 코드는 특정 단순 환경에서는 우연히 동작해도 이식성과 isolation을 잃는다.

32-bit DMA mask 장치가 high memory를 직접 못 가리키면 bounce buffer나 제한된 allocation이 필요할 수 있다. mapping 실패를 무시하면 truncated address로 다른 memory를 덮을 수 있다. scatter-gather list도 segment 수와 alignment 제한을 가진다.

디버깅에서는 CPU VA, physical page, IOVA, device DMA mask를 한 표로 연결한다. I/O error가 특정 memory size 이후에만 발생한다면 addressability와 segment boundary를 먼저 검사한다.

---

## CHAPTER 11 · DMA coherency는 CPU cache와 device가 같은 bytes를 보는 규칙이다

coherent DMA에서는 platform이 CPU cache와 device access 사이 일관성을 제공하지만 ordering과 ownership 규칙은 여전히 필요하다. non-coherent DMA에서는 map/unmap 또는 explicit sync가 cache clean/invalidate 역할을 포함할 수 있다. coherent라는 단어를 “barrier 불필요”로 해석하면 안 된다.

CPU가 device write completion 전에 cache line을 읽어 old data를 유지하거나, CPU write가 device read 전에 외부로 밀려나지 않으면 stale content가 보인다. partial cache-line sharing은 unrelated object까지 maintenance 영향을 받을 수 있다.

재현 시 buffer allocation type, cacheability, sync call과 completion을 기록한다. architecture별로만 발생하는 corruption은 source logic보다 DMA coherency contract 차이를 의심할 근거가 된다.

---

## CHAPTER 12 · IOMMU는 device DMA에 address translation과 protection을 추가한다

IOMMU는 device-visible IOVA를 system physical memory로 translation하며 device 또는 domain별 허용 mapping을 제한할 수 있다. 이는 CPU MMU와 목적이 비슷하지만 주체가 device DMA라는 점이 다르다. virtualization에서는 guest-owned buffer만 passthrough device가 접근하게 하는 핵심 isolation layer다.

identity mapping으로 켜져 있어도 isolation 효과가 제한될 수 있고, domain grouping이 잘못되면 서로 신뢰하지 않는 device가 같은 address space를 공유한다. mapping update 후 IOTLB invalidation이 늦으면 stale translation이 남을 수 있다.

관측은 domain membership, IOVA map/unmap, fault log를 사용한다. IOMMU fault address를 application pointer와 직접 비교하지 말고 당시 request의 IOVA mapping과 대조해야 한다.

---

## CHAPTER 13 · IOVA는 device용 virtual address space를 제공한다

IOVA는 device가 descriptor에서 사용하는 address와 실제 physical page 배치를 분리한다. scatter된 page를 연속 IOVA range로 보이게 할 수 있고 guest physical address model과 host physical memory 사이 translation에도 활용된다. 이 추상화는 flexible mapping을 주지만 allocator와 invalidation 비용을 추가한다.

IOVA space가 fragmentation되거나 address limit을 소진하면 physical memory가 남아 있어도 mapping이 실패할 수 있다. 같은 IOVA가 unmap 직후 새 request에 빠르게 재사용되면 stale DMA나 IOTLB entry와 충돌 위험이 커진다.

운영에서는 IOVA allocation 실패와 free-space geometry를 본다. 장시간 실행 뒤만 발생하는 mapping 실패는 memory leak과 별도로 IOVA allocator leak도 조사해야 한다.

---

## CHAPTER 14 · IOTLB는 IOMMU translation을 cache해 성능과 invalidation 비용을 만든다

device가 매 DMA마다 page table을 완전히 walk하면 비용이 크므로 IOTLB가 최근 translation을 cache할 수 있다. 작은 random buffer를 많은 mapping으로 만들면 IOTLB miss가 증가하고 device throughput이 translation overhead에 제한될 수 있다. huge mapping과 batching은 reach를 늘리는 선택지가 된다.

mapping을 변경하거나 page를 다른 owner에게 넘길 때 stale entry를 제거해야 한다. invalidation 범위가 너무 넓으면 global stall이 커지고, 너무 좁아 잘못 남으면 isolation violation이 된다.

성능 분석에서 device bandwidth만 보지 말고 IOMMU fault/miss와 mapping churn을 함께 본다. buffer pool을 재사용했을 때 throughput이 개선되면 IOTLB와 map/unmap 비용을 분리해 측정한다.

---

## CHAPTER 15 · IOTLB invalidation은 mapping lifetime 종료를 hardware에 확정한다

software page table에서 entry를 지웠다는 사실만으로 device가 즉시 old translation을 잊는 것은 아니다. IOMMU cache와 device-side translation cache가 있을 수 있어 documented invalidation sequence와 completion을 기다려야 한다. 그 전에 physical page를 다른 tenant에게 재사용하면 stale DMA가 새 data에 접근할 수 있다.

unmap storm은 invalidation queue와 synchronization 비용을 증가시켜 I/O latency를 흔들 수 있다. lazy invalidation 전략은 throughput을 높일 수 있지만 reuse boundary를 더 엄격히 설계해야 한다.

검증은 unmap timestamp, invalidation completion, page reuse 시각을 연결한다. 보안 incident에서는 stale access 가능 window를 수치로 확인해야 한다.

---

## CHAPTER 16 · IOMMU group은 독립적으로 격리 가능한 device 단위를 나타낸다

물리 topology와 bridge 특성 때문에 개별 PCI function이 완전히 독립된 DMA requester로 분리되지 않을 수 있다. IOMMU group은 안전한 assignment 경계를 판단할 때 사용되는 단위다. 같은 group의 device를 서로 다른 trust domain에 나누면 isolation assumption이 깨질 수 있다.

SR-IOV가 있다고 모든 VF가 무조건 독립적이라는 뜻도 아니다. platform ACS, bridge routing, reset scope를 함께 봐야 한다. group 구성은 firmware와 kernel 설정에 따라 달라질 수 있다.

passthrough 배포 전 group topology를 artifact로 보존한다. hardware 교체나 BIOS 변경 후 group이 달라졌다면 기존 security policy를 그대로 재사용하지 않는다.

---

## CHAPTER 17 · VFIO는 userspace device ownership을 kernel isolation primitive와 연결한다

VFIO 계열은 userspace VMM이나 application이 device register와 DMA mapping을 제어할 수 있게 하면서 IOMMU·interrupt 같은 kernel protection을 함께 사용한다. 단순 `/dev/mem` 접근과 달리 ownership과 mapping lifecycle을 명시적으로 관리한다.

process가 죽거나 VM이 reset될 때 device를 안전한 상태로 되돌리지 못하면 다음 owner가 이전 DMA queue나 key material을 상속할 수 있다. fd가 열려 있는 동안 device reference가 유지되는 것도 lifecycle에 포함된다.

운영에서는 container/VM identity, VFIO group, IOMMU domain, reset event를 연결한다. device 재할당 직후만 발생하는 corruption은 previous owner cleanup을 먼저 본다.

---

## CHAPTER 18 · device reset은 이전 owner의 실행 상태를 제거해야 한다

reset의 목적은 link를 잠깐 내리는 것이 아니라 queue, DMA engine, interrupt, internal cache 같은 runtime state를 정의된 초기 상태로 돌리는 것이다. function-level reset이 모든 shared component를 초기화하지 않을 수 있고 일부 device는 bus-level reset이 필요할 수 있다.

reset 중 DMA가 완전히 멈췄는지 확인하기 전에 buffer를 free하면 old transaction이 memory를 건드릴 수 있다. 반대로 reset scope가 너무 넓으면 같은 device group의 다른 tenant까지 중단된다.

reset 시작·완료, pending DMA, interrupt disable, reinitialization을 state machine으로 기록한다. timeout 후 강제 재사용하지 말고 quarantine 여부를 명확히 정한다.

---

## CHAPTER 19 · SR-IOV는 하나의 physical function에서 여러 virtual function을 노출한다

SR-IOV는 PF가 resource를 관리하고 여러 VF가 비교적 독립된 PCI function처럼 guest나 process에 할당될 수 있게 한다. hardware queue와 DMA context를 분리해 virtualization overhead를 줄일 수 있지만 PF firmware와 shared resource가 여전히 공통 failure domain이 될 수 있다.

VF 수를 늘리면 queue, MSI-X vector, bandwidth가 무한히 늘지 않는다. physical port와 internal scheduler를 공유하므로 noisy neighbor가 생길 수 있다. VF reset이 PF 전체 상태에 미치는 영향도 장치마다 다르다.

관측은 PF/VF별 traffic, queue, error counter를 함께 본다. guest 내부 metric만으로 physical saturation을 설명하려 하지 않는다.

---

## CHAPTER 20 · PF와 VF 사이 trust boundary는 configuration 권한을 제한해야 한다

PF는 VF 생성과 resource policy를 관리하므로 control-plane 권한이 크다. untrusted VF가 MAC, VLAN, spoofing 관련 설정이나 privileged feature를 임의 변경하지 못하게 hardware와 driver policy가 제한해야 한다. PF compromise는 여러 VF에 동시에 영향을 줄 수 있다.

“VF니까 격리됐다”는 결론 대신 DMA domain, interrupt, configuration, reset, shared firmware 각각의 경계를 검토한다. admin tool이 PF에 broad access를 노출하면 tenant isolation이 우회될 수 있다.

보안 검증에는 VF spoofing attempt, reset, malformed descriptor를 포함한다. PF audit log와 VF identity를 연결해 누가 어떤 설정을 바꿨는지 추적 가능하게 만든다.

---

## CHAPTER 21 · VF network isolation은 data path와 control plane을 함께 다룬다

VF를 network tenant에 할당할 때 MAC/VLAN filtering, anti-spoofing, rate limit과 switch policy가 일관되어야 한다. guest OS firewall만으로 physical NIC의 switching behavior를 모두 통제할 수 없다. offload가 host network stack을 우회하면 관측·보안 경계도 달라진다.

misconfigured VF가 다른 tenant frame을 수신하거나 source address를 위조하면 isolation failure다. hardware offload 업데이트가 지연되면 control plane에서는 정책이 적용된 것으로 보여도 data path는 old rule을 사용할 수 있다.

packet capture 위치와 NIC hardware counter를 함께 활용한다. 정책 change timestamp와 actual traffic behavior를 correlation해 convergence 시간을 확인한다.

---

## CHAPTER 22 · PASID는 한 device가 여러 address space를 식별하게 한다

Process Address Space ID 같은 mechanism은 하나의 requester가 여러 translation context를 구분해 process별 또는 workload별 address space를 사용할 수 있게 한다. accelerator가 여러 process memory를 직접 접근하는 모델에서 유용하지만 requester ID만으로는 부족한 세밀한 isolation이 필요해진다.

PASID 재사용이 너무 빠르거나 teardown 후 stale request가 남으면 새 process context에 old DMA가 들어갈 수 있다. fault attribution도 requester와 PASID를 함께 봐야 정확하다.

진단 로그에 device, PASID, process generation, IOVA/VA를 함께 남긴다. process exit와 mapping teardown ordering을 stress test한다.

---

## CHAPTER 23 · shared virtual addressing은 CPU와 device가 address model을 더 가깝게 공유한다

SVA는 accelerator가 process virtual address를 사용하도록 해 explicit buffer translation 관리 부담을 줄일 수 있다. 하지만 page fault, migration, permission change가 device execution과 연결되므로 CPU MMU lifecycle과 device invalidation protocol이 더 밀접해진다.

pointer를 그대로 device에 넘길 수 있다는 편의 때문에 lifetime이 자동 보장되는 것은 아니다. process unmap, fork, exec, exit와 concurrent device access를 정확히 정의해야 한다.

관측에서는 process page fault와 device fault를 같은 address-generation으로 연결한다. memory manager와 driver가 서로 다른 lifetime assumption을 갖지 않게 API contract를 문서화한다.

---

## CHAPTER 24 · ATS는 device가 translation을 cache할 수 있게 해 invalidation 책임을 넓힌다

Address Translation Services를 사용하는 device는 host translation 결과를 자체 cache에 보관할 수 있어 DMA translation latency를 줄인다. 대신 host mapping이 바뀔 때 device-side cache까지 정확히 invalidate해야 한다. cache hierarchy가 하나 더 생기는 셈이다.

stale ATS entry는 단순 성능 문제가 아니라 잘못된 physical page 접근으로 이어질 수 있다. invalidation completion 전에 page를 재사용하면 tenant boundary가 깨질 수 있다.

ATS 활성 여부와 device cache invalidation event를 기록한다. 기능을 끄면 문제가 사라지는 경우 성능 차이와 correctness issue를 분리해 분석한다.

---

## CHAPTER 25 · PRI는 device page request를 memory manager와 연결한다

Page Request Interface 계열은 device가 translation fault를 만나 host에 page 제공을 요청하고 처리를 기다리는 모델을 지원한다. accelerator가 pageable memory를 다룰 수 있게 하지만 device work가 memory pressure와 page-fault latency에 직접 노출된다.

request가 폭주하면 fault handling queue가 병목이 되고 device가 stall할 수 있다. process가 memory를 unmap하는 동안 pending PRI request를 어떻게 종료할지도 lifecycle 문제다.

page request rate, service latency, migration/reclaim event를 함께 측정한다. OOM이나 cgroup limit이 device timeout으로만 보이지 않도록 cross-layer trace를 만든다.

---

## CHAPTER 26 · peer DMA는 device 사이 data path의 ownership을 재정의한다

peer-to-peer DMA는 한 device가 다른 device resource나 memory에 CPU copy 없이 접근해 latency와 bandwidth를 개선할 수 있다. 하지만 root complex topology, ACS/IOMMU policy, BAR addressability에 따라 지원 범위가 제한된다. host DRAM을 거치지 않는 path는 기존 관측 지점도 우회한다.

두 device 중 하나가 reset되거나 hot-unplug될 때 상대가 여전히 peer address를 사용하면 bus error나 corruption이 발생할 수 있다. authorization도 양쪽 device trust를 고려해야 한다.

peer mapping lifecycle과 topology를 기록한다. benchmark에서는 CPU usage 감소뿐 아니라 failure recovery와 isolation까지 검증한다.

---

## CHAPTER 27 · PCIe recovery는 link와 device state를 단계적으로 복구한다

PCIe error가 발생했을 때 단순 재부팅 대신 error severity에 따라 link recovery, function reset, driver reinitialization 같은 절차가 가능할 수 있다. recovery 중 upper-layer I/O가 어떤 오류를 받는지, outstanding DMA를 어떻게 정리하는지가 중요하다.

recovery가 성공해도 queue sequence와 data integrity가 자동 보장되는 것은 아니다. application이 같은 request를 재시도할 때 duplicate write가 생길 수 있으므로 idempotency와 generation 관리가 필요하다.

AER/error log, reset stage, request outcome을 같은 incident record에 넣는다. 반복되는 correctable error가 결국 fatal failure로 발전하는 패턴도 trend로 본다.

---

## CHAPTER 28 · hot-unplug은 device pointer보다 resource lifetime 문제다

device가 물리적 또는 논리적으로 사라질 수 있는 환경에서는 driver가 보유한 MMIO, DMA buffer, workqueue, userspace handle이 모두 teardown 순서를 따라야 한다. device object가 제거됐는데 callback이 뒤늦게 실행되면 use-after-free가 발생한다.

new work admission을 먼저 막고, in-flight I/O를 cancel/drain한 뒤 interrupt와 DMA를 중단하고 mapping을 해제하는 순서를 명시한다. surprise removal은 정상 shutdown callback을 모두 실행할 기회를 주지 않을 수 있다.

stress test에서 I/O 중 hot-unplug과 process exit를 겹친다. refcount와 outstanding request가 0으로 수렴하는지 확인한다.

---

## CHAPTER 29 · PCIe observability는 software queue와 hardware link를 연결해야 한다

application latency만 보면 queue stall, IOMMU fault, link retrain, device internal error를 구분하기 어렵다. driver queue depth, completion latency, PCIe error, negotiated width/speed, IOMMU fault를 같은 timestamp domain에서 수집해야 한다.

metric 이름이 같아도 reset 전후 counter가 초기화될 수 있고 device firmware가 다른 의미로 report할 수 있다. absolute value보다 interval delta와 generation을 관리한다.

incident timeline에 BDF·serial·firmware·driver version을 함께 보존한다. hardware 교체 뒤 주소가 바뀌어도 같은 physical lineage를 추적할 수 있어야 한다.

---

## CHAPTER 30 · device assignment contract는 identity, DMA, interrupt, reset, recovery를 함께 묶는다

안전한 passthrough나 userspace device 사용은 “장치를 보였다”로 끝나지 않는다. 어떤 function이 owner에게 배정됐는지, 어느 IOMMU domain과 interrupt target을 사용하는지, buffer lifetime과 invalidation 경계가 무엇인지, owner 종료 후 어떤 reset으로 clean state를 보장하는지 정의해야 한다.

성능을 위해 isolation 기능을 끄면 얻는 이득과 새 failure domain을 함께 기록해야 한다. SR-IOV·SVA·ATS 같은 기능은 각각 resource sharing과 cache layer를 늘리므로 enable 조건을 명확히 둔다.

release gate에서는 hot-unplug, reset, malformed DMA, VM/process crash를 실제로 주입해 다음 owner가 old state를 관찰하지 않는지 검증한다. 최종 목표는 빠른 I/O가 아니라 **device가 허용된 memory와 execution context에만 영향을 주도록 증명하는 것**이다.
