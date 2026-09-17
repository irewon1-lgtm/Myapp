# PART 33 · PCIe, DMA and IOMMU — BAR, MSI-X, IOVA, PASID, SR-IOV

CPU와 장치가 통신한다는 말은 실제로는 여러 주소 공간과 transaction domain이 만나는 과정이다. PCIe device는 configuration space와 BAR로 resource를 선언하고, driver는 MMIO register와 DMA queue를 설정하며, device는 CPU load/store와 독립적으로 host memory에 접근할 수 있다. IOMMU는 device-visible I/O address를 system physical memory에 mapping해 DMA isolation을 만들고, PASID/SVA는 더 세밀한 process address-space 공유를 가능하게 한다. **장치가 빠르게 데이터를 옮긴다는 사실과 장치가 어떤 memory를 건드릴 권한이 있는지는 별개의 문제**다.

---

## CHAPTER 01 · PCIe device는 bus addressable function으로 열거된다

Firmware와 operating system은 PCI/PCIe hierarchy를 탐색해 bus-device-function identity를 가진 function을 발견하고 configuration space를 읽는다. Vendor/device ID, class code, capability list, BAR, interrupt capability를 바탕으로 적절한 driver를 bind한다. Device discovery와 driver initialization은 별도 단계이므로 `lspci에 보임`과 `driver가 정상 동작함`은 같은 증거가 아니다.

Hotplug·reset·power transition은 enumeration 뒤에도 function lifecycle을 바꿀 수 있다. Driver는 probe뿐 아니라 remove/shutdown/error-recovery 경로를 갖는다.

---

## CHAPTER 02 · Configuration space는 device가 지원하는 capability의 control plane이다

PCI configuration space에는 command/status, BAR, capability pointer와 PCIe extended capability가 있다. MSI/MSI-X, SR-IOV 같은 기능은 capability structure로 발견된다. Driver가 register offset을 임의 가정하지 않고 capability traversal을 통해 지원 여부를 확인해야 하는 이유다.

Capability 존재와 실제 platform support는 다를 수 있다. IOMMU·interrupt remapping·firmware policy가 기능 사용을 제한할 수 있다.

---

## CHAPTER 03 · BAR는 device register/memory window를 system address space에 배치한다

Base Address Register는 device가 필요한 MMIO/I/O resource 크기와 type을 나타내고 firmware/kernel이 system address range를 할당한다. Driver는 BAR resource를 map해 device control/status register 또는 onboard memory에 접근한다.

BAR address는 ordinary RAM pointer가 아니다. MMIO access는 architecture와 device ordering rule을 따르고 speculative/cacheable access를 일반 memory처럼 사용하면 안 된다.

---

## CHAPTER 04 · MMIO read/write는 compiler access와 device transaction을 연결한다

Driver가 `readl/writel`류 I/O accessor를 사용하는 이유는 volatile-like compiler behavior뿐 아니라 architecture-specific ordering/endian/access width를 맞추기 위해서다. Raw pointer dereference로 device register를 읽고 쓰면 compiler reorder나 unsupported access width 문제를 만들 수 있다.

Device programming manual이 요구하는 register sequence와 memory barrier를 함께 지켜야 한다.

---

## CHAPTER 05 · Posted write는 CPU가 device completion을 기다리지 않고 진행하게 한다

PCIe/MMIO write는 posted transaction으로 buffer되어 caller가 write completion을 직접 기다리지 않을 수 있다. 그래서 `register에 write 함수가 반환됨`과 `device가 그 write를 실제 처리함`은 다르다. 일부 driver는 특정 register read를 통해 posted write를 flush하거나 ordering point를 만든다.

Reset/disable 직전 register write가 device에 도달했는지 보장해야 하는 path에서 이 차이가 중요하다. Linux PCI driver 문서도 MMIO write posting을 별도 주제로 다룬다. citeturn243976search2turn243976search6

---

## CHAPTER 06 · Doorbell register는 queue state publication의 마지막 단계다

High-performance device는 command descriptor를 host memory queue에 두고 MMIO doorbell write로 `새 descriptor가 있음`을 알리는 구조를 많이 사용한다. CPU가 descriptor field를 memory에 채운 뒤 doorbell을 먼저 device가 관찰하면 incomplete descriptor를 읽을 수 있으므로 DMA memory ordering primitive가 필요하다.

Queue publication은 `descriptor writes → DMA visibility barrier → doorbell`의 순서를 갖는다. PART 27 weak-memory proof가 device와 CPU 사이에도 적용된다.

---

## CHAPTER 07 · Legacy INTx와 MSI/MSI-X는 interrupt delivery architecture가 다르다

Legacy pin-based interrupt는 shared line과 level-triggered semantics를 사용할 수 있다. MSI/MSI-X는 device가 memory-write transaction 형태로 interrupt message를 보내고 vector를 더 세밀하게 분배할 수 있다. MSI-X는 여러 queue가 서로 다른 vector/CPU에 completion을 전달하도록 구성하는 데 유리하다.

Interrupt vector 수를 늘리면 parallelism이 늘 수 있지만 CPU/IRQ affinity가 잘못되면 cache locality와 fairness가 나빠진다. SR-IOV VF마다 MSI-X vector allocation도 resource가 된다. citeturn243976search0turn243976search5

---

## CHAPTER 08 · Interrupt remapping은 device interrupt도 isolation 대상임을 보여 준다

DMA만 IOMMU로 막아도 malicious/misconfigured device가 arbitrary interrupt message를 만들 수 있다면 isolation이 불완전하다. Platform interrupt-remapping support는 device interrupt를 허용된 destination/vector로 제한하는 데 사용된다.

Direct device assignment의 trust model은 memory translation과 interrupt routing을 함께 본다.

---

## CHAPTER 09 · DMA는 CPU가 byte를 복사하는 것과 다른 execution agent다

Device DMA engine은 CPU instruction stream과 독립적으로 host memory read/write transaction을 발생시킨다. CPU는 descriptor와 buffer를 준비하고 device를 시작한 뒤 다른 일을 할 수 있다. Completion interrupt/polling이 data ownership 반환 지점이 된다.

Buffer를 device가 사용하는 동안 CPU allocator가 free/reuse하면 data corruption이 발생한다. DMA ownership은 lifetime invariant다.

---

## CHAPTER 10 · DMA API는 CPU pointer를 device address로 자동 동일시하지 않는다

CPU virtual address, physical address, device-visible DMA address는 서로 다를 수 있다. Driver는 DMA mapping API를 통해 buffer를 device가 접근 가능한 address로 map하고 IOMMU/architecture-specific translation을 runtime에 맡긴다. `virt_to_phys`류 직접 계산은 일반 portable DMA contract가 아니다.

Linux x86 IOMMU 문서도 well-behaved driver가 `dma_map_*` 후 IOVA를 device에 제공하고 완료 뒤 `dma_unmap_*`하는 경로를 설명한다. citeturn243976search9

---

## CHAPTER 11 · Coherent DMA와 streaming DMA는 cache-maintenance contract가 다르다

Control descriptor처럼 CPU와 device가 자주 공유하는 small structure는 coherent DMA allocation을 사용할 수 있고, large payload streaming은 map/unmap 또는 sync API로 ownership transition을 표시할 수 있다. Noncoherent architecture에서는 CPU cache clean/invalidate가 필요할 수 있다.

`DMA coherent`는 thread synchronization이 필요 없다는 뜻이 아니다. Device/CPU visibility와 여러 CPU thread의 logical synchronization은 별도 문제다.

---

## CHAPTER 12 · IOMMU는 device request의 address translation과 protection을 담당한다

IOMMU는 device가 발생시킨 I/O virtual address(IOVA)를 physical memory로 변환하고 page permission/domain을 적용한다. Device마다 또는 group/domain마다 허용 memory range를 제한해 buggy/malicious DMA가 kernel 전체 memory를 덮는 것을 막을 수 있다.

IOMMU가 비활성화되면 direct assignment의 security assumption이 크게 달라진다. VFIO가 IOMMU-protected environment를 강조하는 이유다. citeturn243976search4

---

## CHAPTER 13 · IOVA는 process virtual address와 다른 address domain이다

Traditional DMA mapping에서 device가 보는 IOVA는 CPU process virtual address와 같을 필요가 없다. Driver는 buffer page를 IOMMU domain에 map하고 device descriptor에는 IOVA를 넣는다. 같은 physical page가 CPU와 device에서 다른 address로 보일 수 있다.

Crash dump에서 DMA address를 userspace pointer처럼 역참조하면 안 된다. Mapping table과 domain identity가 필요하다.

---

## CHAPTER 14 · IOTLB는 IOMMU translation에도 cache hierarchy가 있음을 뜻한다

Device request마다 multi-level IOMMU page table을 걸으면 expensive하므로 IOMMU는 translation cache(IOTLB)를 사용할 수 있다. Mapping churn이 많거나 working set이 IOTLB reach를 넘으면 DMA latency가 증가할 수 있다.

Huge I/O page, persistent mapping, batched unmap은 IOTLB pressure와 memory pinning을 교환한다.

---

## CHAPTER 15 · IOTLB invalidation은 CPU TLB shootdown과 유사한 ordering 문제다

Driver가 IOVA mapping을 제거/변경한 뒤 device가 stale translation을 사용하면 freed page에 DMA할 수 있다. IOMMU invalidation completion과 device queue drain/order를 맞춰야 mapping을 안전하게 재사용할 수 있다.

Unmap 함수 반환이 어떤 invalidation guarantee를 제공하는지는 DMA/IOMMU API contract를 따른다.

---

## CHAPTER 16 · IOMMU group은 PCI topology가 완전한 isolation을 허용하는 범위를 표현한다

PCI bridge나 multi-function device 구조 때문에 IOMMU가 서로 다른 function transaction을 구분하지 못하는 topology가 있을 수 있다. VFIO는 isolation 가능한 device group 개념을 사용해 unsafe partial assignment를 막는다. Linux VFIO 문서는 동일 bridge 아래 function의 transaction이 IOMMU 관점에서 구분되지 않을 수 있음을 명시한다. citeturn243976search4

`장치 하나만 VM에 넘겼다`가 isolation의 충분조건이 아니다. Group topology와 ACS/bridge behavior를 본다.

---

## CHAPTER 17 · VFIO는 userspace가 device를 직접 제어하면서 kernel isolation을 유지하게 한다

VFIO framework는 IOMMU-protected device access를 userspace에 노출해 VM monitor나 userspace driver가 BAR, interrupt, DMA mapping을 제어할 수 있게 한다. Kernel device-specific driver를 우회하므로 userspace가 reset/error/lifecycle을 책임져야 한다.

VFIO fd 자체가 강한 capability다. File permission과 container mount로 무분별하게 노출하면 host DMA authority를 넘길 수 있다.

---

## CHAPTER 18 · Device reset은 assignment lifecycle의 correctness boundary다

이전 owner가 남긴 DMA queue, register, secret state가 다음 VM/process에 보이면 isolation이 깨진다. Device assignment 전후 function-level reset 또는 vendor-specific reset을 통해 known state로 되돌릴 수 있어야 한다.

Reset capability가 없는 device는 safe reassignment/live migration을 제한할 수 있다. VFIO bind 성공만으로 reset isolation을 증명하지 않는다.

---

## CHAPTER 19 · SR-IOV는 하나의 Physical Function에서 여러 Virtual Function을 hardware로 expose한다

SR-IOV capable PF는 여러 VF PCI function을 생성해 각 VM/tenant에 독립 assignment할 수 있다. PF driver가 VF resource와 policy를 관리하고 VF는 제한된 queue/BAR/interrupt resource를 갖는다. Linux sysfs는 `sriov_numvfs`, total VF, VF MSI-X allocation을 expose한다. citeturn243976search0turn243976search5

VF 수를 늘리면 isolation unit은 늘지만 queue/vector/cache resource가 분할된다.

---

## CHAPTER 20 · PF와 VF 사이에는 control-plane trust relation이 남는다

VF가 독립 PCI function처럼 보여도 PF firmware/driver가 switch policy, rate limit, spoof check, resource allocation을 제어할 수 있다. PF compromise나 firmware bug가 여러 VF에 영향을 줄 수 있다.

Multi-tenant isolation review는 IOMMU domain뿐 아니라 device-internal PF/VF isolation과 embedded switch policy까지 포함한다.

---

## CHAPTER 21 · SR-IOV network isolation은 MAC/VLAN 설정만으로 끝나지 않는다

VF spoofing, trust mode, rate limit, switchdev/offload policy가 tenant traffic isolation에 영향을 준다. Linux networking 문서는 legacy VF netlink API보다 switchdev model을 새로운 driver에서 권장한다. citeturn243976search1

Hardware offload된 forwarding rule이 software firewall path를 우회하지 않는지 end-to-end packet path를 검증한다.

---

## CHAPTER 22 · PASID는 한 device request에 process/address-space identity를 추가한다

PASID(Process Address Space ID)는 device request가 어느 process/address space에 속하는지 구분하는 identifier로 사용될 수 있다. 한 device가 여러 process의 translation context를 동시에 사용하게 해 accelerator sharing과 SVA를 가능하게 한다. Linux VFIO 문서도 PASID가 SVA와 scalable I/O virtualization의 전제임을 설명한다. citeturn243976search4

PASID reuse/lifetime은 process mm lifetime과 동기화돼야 한다.

---

## CHAPTER 23 · Shared Virtual Addressing은 CPU와 accelerator가 같은 virtual pointer를 공유하게 한다

SVA에서는 IOMMU가 process CPU page table과 연계되어 device가 userspace virtual address를 직접 사용할 수 있다. Pointer-rich data structure를 device-specific IOVA로 다시 serialize하지 않아도 되는 장점이 있다. UACCE 문서는 accelerator가 main CPU와 동일 virtual address를 사용할 수 있도록 IOMMU SVA를 활용한다고 설명한다. citeturn243976search3

Convenience 대신 page-fault, mm teardown, PASID binding 같은 lifetime complexity가 추가된다.

---

## CHAPTER 24 · ATS는 device가 address translation을 cache하게 한다

Address Translation Services를 지원하는 device는 IOMMU translation result를 device-side cache에 저장해 반복 IOMMU lookup을 줄일 수 있다. Mapping 변경 시 device ATS cache invalidation까지 일관되게 처리해야 한다.

CPU TLB, IOTLB, device ATC가 서로 다른 cache layer가 되는 셈이다. Stale translation bug는 freed memory DMA로 이어질 수 있다.

---

## CHAPTER 25 · Page Request Interface는 device page fault를 CPU VM과 연결한다

SVA workload에서 device가 아직 resident/mapped되지 않은 virtual page에 접근하면 PRI류 mechanism을 통해 page request를 host에 전달하고 OS가 fault를 resolve한 뒤 device가 retry하도록 할 수 있다. Device가 pageable process memory를 사용하는 데 필요한 핵심 기능이다. UACCE 문서도 SVA capability에 PASID와 device page-fault support를 함께 언급한다. citeturn243976search3

Page-fault storm은 CPU thread가 아니라 accelerator access pattern 때문에 생길 수 있다.

---

## CHAPTER 26 · Peer-to-peer DMA는 host memory를 거치지 않는 대신 topology 제약을 가진다

PCIe device끼리 직접 data를 주고받는 P2P DMA는 host DRAM bandwidth/copy를 줄일 수 있지만 PCIe switch/root-port topology와 IOMMU routing이 지원해야 한다. Linux x86 IOMMU 문서도 peer-to-peer transaction이 일반 IOVA translation과 다르게 취급되는 영역이 있음을 언급한다. citeturn243976search9

GPU↔NVMe direct path 성능은 topology-aware하게 배치해야 한다.

---

## CHAPTER 27 · PCIe error recovery는 link/device failure를 driver lifecycle과 연결한다

AER/error-recovery path는 correctable/nonfatal/fatal PCIe error를 report하고 driver에 recovery callback을 요청할 수 있다. Device channel이 frozen되면 normal I/O를 계속 보내는 것이 아니라 reset/reinit state machine으로 들어가야 한다.

Error counter만 clear하고 계속 동작시키면 corrupted queue state가 남을 수 있다. Recovery 후 device configuration·DMA mapping·interrupt를 재검증한다.

---

## CHAPTER 28 · Hot unplug는 모든 outstanding DMA와 MMIO pointer를 invalid하게 만든다

Device가 제거되면 driver/userspace가 보유하던 BAR mapping과 queue state를 더 이상 사용하면 안 된다. Surprise removal과 graceful remove는 timing이 다르다. Async worker가 stale device pointer를 참조하면 kernel crash가 발생할 수 있다.

Device object lifetime은 fd/process lifetime보다 짧을 수 있음을 API에 반영한다.

---

## CHAPTER 29 · PCIe/DMA observability는 CPU와 device queue를 동시에 본다

성능 incident에서 CPU submission rate, MMIO doorbell, DMA map/unmap, IOMMU fault/IOTLB miss, MSI-X interrupt, device queue depth를 하나의 timeline에 연결한다. `GPU가 느림`, `NIC가 느림`이라는 device-level metric만으로는 host-side bottleneck을 분리할 수 없다.

IOMMU fault log에는 device requester identity와 IOVA가 중요하고 PCIe AER에는 BDF/topology가 중요하다.

---

## CHAPTER 30 · Device-assignment 설계의 최종 계약은 address·ownership·interrupt·reset·isolation이다

1. **Address** — CPU VA/PA, IOVA, device PASID/SVA가 어떤 translation chain으로 연결되는가.
2. **Ownership** — DMA buffer와 queue descriptor를 CPU/device 중 누가 언제 사용할 수 있는가.
3. **Interrupt** — completion/error signal이 어느 vector/CPU/guest로 안전하게 routing되는가.
4. **Reset** — owner 전환·failure 뒤 device가 known clean state로 돌아오는가.
5. **Isolation** — IOMMU group, SR-IOV function, PF/VF policy가 다른 tenant memory/device state를 차단하는가.

PCIe 성능과 virtualization은 이 다섯 조건이 맞을 때만 안전하게 최적화할 수 있다.
