# PART 37 · Firmware and Hardware Description — UEFI, ACPI, Device Tree, driver binding

Kernel이 부팅됐다고 해서 board의 모든 장치를 자동으로 아는 것은 아니다. CPU/memory topology, interrupt controller, PCI host bridge, GPIO, clock, regulator, thermal sensor처럼 **discoverable하지 않은 hardware relation**은 firmware table이나 Device Tree 같은 description contract로 전달된다. Driver는 이 description에서 resource를 받아 MMIO·IRQ·DMA·power dependency를 구성한다. 잘못된 firmware metadata는 driver code가 맞아도 probe 실패·resource collision·power bug를 만든다.

---

## CHAPTER 01 · Firmware handoff는 boot code에서 OS ownership으로 state를 넘기는 계약이다

Firmware는 memory map, hardware table, boot service state를 준비하고 kernel/bootloader에 control을 넘긴다. OS가 ownership을 받은 뒤 firmware와 kernel이 동일 device register를 동시에 관리하면 race가 생길 수 있다. Handoff point와 runtime service scope를 명확히 해야 한다.

`firmware가 초기화했으니 kernel driver가 아무것도 안 해도 된다`는 가정도 위험하다. Driver는 reset 이후 runtime state를 자신의 invariant에 맞게 재구성해야 한다.

---

## CHAPTER 02 · UEFI는 boot service와 runtime service를 분리한다

UEFI environment는 image loading, filesystem/network boot, memory allocation 같은 boot service를 제공하고 ExitBootServices 이후 OS가 system resource 관리를 인수한다. 일부 runtime service는 OS 실행 중에도 남을 수 있다.

Boot service pointer를 ExitBootServices 뒤 사용하거나 stale memory map key로 exit하면 boot failure가 발생할 수 있다. Firmware interface에도 lifecycle이 있다.

---

## CHAPTER 03 · Firmware memory map은 usable RAM과 reserved region을 분리한다

Physical address range에는 ordinary RAM뿐 아니라 firmware runtime region, MMIO aperture, ACPI data, reserved memory가 섞여 있다. Kernel allocator가 reserved region을 일반 page로 사용하면 firmware/device state를 덮을 수 있다.

Early boot memory allocator는 firmware map을 검증해 usable range만 관리한다. Later hotplug/reserved-memory mechanism과 일관성을 유지해야 한다.

---

## CHAPTER 04 · ACPI는 table과 executable AML namespace를 함께 사용한다

ACPI는 static table header/data뿐 아니라 AML(ACPI Machine Language) method를 통해 device/resource/power logic을 표현할 수 있다. OS는 AML interpreter로 namespace object와 control method를 평가한다.

Firmware bug가 단순 잘못된 숫자가 아니라 AML method side effect/timing으로 나타날 수 있다. Table dump와 method evaluation trace를 함께 볼 수 있어야 한다.

---

## CHAPTER 05 · DSDT와 SSDT는 system/device description namespace를 구성한다

Differentiated/System Definition Table에는 device node, method, resource가 정의될 수 있고 SSDT가 추가 namespace definition을 제공할 수 있다. Multiple table이 하나의 ACPI namespace를 합성하므로 특정 device property가 어느 table에서 왔는지 provenance를 추적해야 한다.

Firmware update가 SSDT 하나만 바꿔도 driver probe behavior가 달라질 수 있다.

---

## CHAPTER 06 · _HID/_CID는 hardware identity와 driver match에 사용된다

ACPI device는 Hardware ID/Compatible ID로 OS driver와 match될 수 있다. ID string이 맞아도 resource/power method가 잘못되면 probe 이후 failure가 난다.

Driver matching success는 resource correctness의 증거가 아니다. ID, resource, dependency를 별도 검증한다.

---

## CHAPTER 07 · _CRS는 device가 사용할 current resource set을 기술한다

MMIO address, I/O port, IRQ, DMA resource 등이 _CRS를 통해 OS에 전달될 수 있다. Resource overlap이나 wrong polarity/trigger 정보는 boot/probe failure를 만들 수 있다.

Kernel resource tree와 firmware _CRS를 비교하면 `driver가 잘못된 address를 쓴다`는 증상을 firmware metadata까지 역추적할 수 있다.

---

## CHAPTER 08 · Interrupt routing은 device IRQ number 하나보다 복잡하다

ACPI/firmware는 interrupt controller topology, GSI, PCI routing, trigger/polarity 정보를 제공한다. OS는 이를 architecture interrupt domain과 연결해 device-visible IRQ resource를 만든다.

Wrong trigger mode는 interrupt storm/missed interrupt를 만들 수 있다. `/proc/interrupts` 숫자만 보고 source hardware route를 단정하지 않는다.

---

## CHAPTER 09 · ACPI power method는 device state transition과 dependency를 표현한다

Device power resource, sleep state, wake capability가 firmware method/table로 기술될 수 있다. Suspend/resume에서 `_PSx`, wake method, resource dependency가 잘못되면 cold boot는 정상인데 resume만 실패할 수 있다.

Power bug는 driver callback과 firmware method invocation 순서를 하나의 timeline으로 본다.

---

## CHAPTER 10 · Device Tree는 node/property tree로 hardware를 선언적으로 기술한다

Device Tree는 bus/device를 node hierarchy로 표현하고 `compatible`, `reg`, `interrupts`, clock/reset/regulator reference 같은 property를 통해 resource/dependency를 연결한다. Kernel은 board-specific hardcoded address 대신 tree data를 읽어 generic driver를 bind할 수 있다.

DT는 runtime discovery protocol이 아니라 firmware-provided description data라는 점이 핵심이다.

---

## CHAPTER 11 · compatible string은 driver binding contract다

`compatible = "vendor,device"` 같은 string list는 specific implementation과 backward-compatible generic implementation을 표현할 수 있다. Driver match table은 이 string을 사용해 probe code를 선택한다.

Compatible string을 firmware version label처럼 임의 변경하면 stable binding ABI가 깨진다. 실제 hardware programming model compatibility를 나타내야 한다.

---

## CHAPTER 12 · reg property는 parent bus address-cell contract를 따른다

`reg`는 device register/resource address와 size를 기술하지만 cell width/encoding은 parent의 `#address-cells`, `#size-cells` 등에 의존한다. 64-bit address를 32-bit cell 하나처럼 읽으면 완전히 다른 MMIO range를 얻을 수 있다.

DT binary parser뿐 아니라 bus translation rule을 따라 address를 해석한다.

---

## CHAPTER 13 · ranges는 child bus address를 parent address domain으로 변환한다

SoC 내부 bus는 child device address space와 CPU physical address가 다를 수 있다. `ranges` property는 이 translation window를 표현한다. PCIe host bridge, simple-bus 등 bus type에 따라 address interpretation rule이 달라진다.

PART 33의 BAR/IOVA와 마찬가지로 `주소 숫자`에는 항상 address domain이 붙는다.

---

## CHAPTER 14 · phandle은 node 사이 dependency를 graph로 만든다

Clock, regulator, reset controller, GPIO provider 같은 shared provider를 consumer node가 phandle로 reference한다. Device Tree는 tree처럼 보이지만 reference edge 때문에 실제 dependency graph를 형성한다.

Probe order도 tree order가 아니라 dependency availability에 따라 달라질 수 있다.

---

## CHAPTER 15 · Deferred probe는 dependency가 아직 준비되지 않았음을 표현한다

Consumer driver가 regulator/clock/IOMMU provider를 요청했지만 provider driver가 아직 probe되지 않았다면 kernel이 probe를 defer하고 나중에 재시도할 수 있다. 이 상태를 hard failure로 오해하면 boot log를 잘못 읽는다.

반대로 missing firmware node 때문에 provider가 영원히 나오지 않는다면 perpetual deferred probe가 된다. Dependency graph를 확인한다.

---

## CHAPTER 16 · Clock tree는 frequency source와 gate/mux/divider dependency를 표현한다

SoC peripheral은 단순 `클럭 하나`가 아니라 parent oscillator/PLL, mux, divider, gate를 거칠 수 있다. Driver가 frequency를 요청하면 common clock framework가 shared clock tree와 다른 consumer requirement를 조정할 수 있다.

잘못된 parent/frequency 설정은 data corruption이나 timing protocol failure까지 만들 수 있다. Power/clock constraint는 성능 옵션이 아니다.

---

## CHAPTER 17 · Regulator framework는 voltage rail 공유를 조정한다

여러 device가 같은 regulator를 공유하면 한 driver가 disable/voltage change를 독단적으로 수행할 수 없다. Framework는 enable reference와 voltage constraint를 관리한다. Firmware description은 consumer-supply relation과 min/max voltage를 제공한다.

Brownout/reset-like bug는 software resource lifetime과 physical rail dependency를 함께 조사한다.

---

## CHAPTER 18 · Reset controller는 logical driver reset을 physical line/protocol에 연결한다

Device reset line이 shared인지 exclusive인지, assert/deassert delay가 얼마인지 firmware binding에 따라 driver behavior가 달라진다. Reset 직후 clock/power가 안정되기 전에 register access하면 intermittent probe failure가 날 수 있다.

Reset sequencing은 firmware metadata와 datasheet timing을 함께 따라야 한다.

---

## CHAPTER 19 · Pinctrl과 GPIO는 동일 pin의 mux/electrical state를 관리한다

SoC pin은 GPIO, UART, I2C 등 여러 function으로 mux될 수 있고 pull-up/drive-strength/slew rate 설정이 필요할 수 있다. Device Tree/ACPI는 default/sleep pin state를 기술해 driver/power management와 연결한다.

잘못된 pinmux는 driver가 probe 성공해도 physical signal이 나오지 않는 전형적 board-integration bug다.

---

## CHAPTER 20 · I2C/SPI child device는 bus enumeration이 firmware description에 의존할 수 있다

I2C/SPI 장치는 PCI처럼 self-enumerating ID space가 없거나 제한적이므로 firmware node가 address/chip-select/interrupt를 알려줘야 driver가 생성될 수 있다. 잘못된 bus address는 다른 device와 collision을 만들 수 있다.

Probe log가 없을 때 driver bug보다 `device object 자체가 만들어졌는가`부터 확인한다.

---

## CHAPTER 21 · reserved-memory는 일반 allocator에서 제외할 physical region을 선언한다

Firmware/DT는 DMA shared pool, framebuffer, secure firmware buffer처럼 일반 Linux page allocator가 사용하면 안 되는 memory region을 reserved로 선언할 수 있다. Driver는 해당 region을 dedicated allocator/mapping으로 사용할 수 있다.

Overlap/size 오류는 boot memory corruption으로 이어질 수 있다. P31 DRAM capacity와 OS usable memory가 다른 이유 중 하나다.

---

## CHAPTER 22 · dma-ranges는 device DMA address domain과 CPU physical domain을 연결한다

Bus/device가 보는 DMA address가 CPU physical address와 동일하지 않을 수 있다. DT의 dma-ranges류 property와 IOMMU configuration이 device DMA mapping path에 영향을 준다.

PART 33 DMA API가 firmware-provided topology 위에서 동작한다는 뜻이다. IOMMU가 있어도 firmware가 stream/device identity를 잘못 기술하면 translation domain이 틀릴 수 있다.

---

## CHAPTER 23 · IOMMU firmware binding은 device stream identity를 translation domain에 연결한다

SoC IOMMU는 PCI BDF 외에 stream ID/requester ID를 사용해 device transaction을 구분할 수 있다. Firmware description이 어떤 device가 어떤 IOMMU/master ID를 사용하는지 알려준다.

Wrong stream ID는 DMA fault 또는 더 위험하게 잘못된 domain access를 만들 수 있어 security-critical metadata다.

---

## CHAPTER 24 · NUMA/SRAT류 firmware table은 memory locality topology를 OS에 알려준다

Multi-socket system에서 CPU와 memory node proximity가 firmware table로 전달돼 scheduler/allocator가 NUMA topology를 구성한다. 잘못된 distance/topology 정보는 OS가 remote memory를 local로 오판해 placement를 악화시킬 수 있다.

NUMA performance investigation은 kernel-reported topology가 hardware expectation과 맞는지 확인한다.

---

## CHAPTER 25 · SMBIOS/DMI는 inventory metadata이며 resource programming contract와 다르다

Firmware는 system/vendor/product/version, memory device 정보 같은 inventory table을 제공할 수 있다. Kernel/driver가 quirk selection에 DMI 정보를 사용하기도 하지만 SMBIOS entry가 MMIO/IRQ resource를 직접 정의하는 ACPI/DT와 동일한 역할은 아니다.

Diagnostic tool은 inventory metadata와 live hardware state를 구분한다.

---

## CHAPTER 26 · Device Tree overlay는 running/base tree에 hardware description을 동적으로 추가할 수 있다

Overlay는 base tree node/property를 확장해 add-on board/device를 표현할 수 있다. Apply/remove 시 이미 bound된 driver와 dependency/resource lifetime을 관리해야 한다. Reference가 남은 node를 무리하게 제거하면 use-after-free/undefined device state가 생길 수 있다.

Dynamic hardware description도 transaction-like lifecycle이 필요하다.

---

## CHAPTER 27 · Firmware table은 untrusted external input처럼 검증해야 한다

Length, checksum, pointer/reference, address range가 잘못된 firmware data를 kernel이 그대로 믿으면 out-of-bounds parsing이나 resource corruption이 발생할 수 있다. Parser는 bounds와 overlap, version을 검증해야 한다.

Firmware가 vendor-signed라고 해도 bug가 없다는 뜻은 아니다. Kernel defensive parsing은 security boundary다.

---

## CHAPTER 28 · Firmware bug workaround는 machine identity와 정확히 scope해야 한다

특정 hardware revision의 ACPI/DT bug를 kernel quirk로 보정할 수 있지만 broad match를 사용하면 정상 machine에 잘못 적용될 수 있다. Vendor/product/BIOS version 등 최소 조건으로 scope하고 firmware update로 해결되면 workaround lifecycle을 관리한다.

Quirk는 영구 design이 아니라 compatibility debt다.

---

## CHAPTER 29 · Hardware-description debugging은 table→resource→driver probe→live register 순서로 간다

Device가 동작하지 않을 때 firmware source/decompiled table, kernel resource tree, driver match/probe log, clock/regulator/reset state, MMIO register를 순서대로 비교한다. `driver source는 맞다`만 확인하면 upstream metadata bug를 놓친다.

Boot-before/after firmware update diff는 table-level regression을 찾는 강력한 evidence다.

---

## CHAPTER 30 · Firmware-description 설계의 최종 계약은 identity·resource·dependency·power·ownership이다

1. **Identity** — 어떤 hardware implementation에 어떤 driver/binding이 적용되는가.
2. **Resource** — MMIO/IRQ/DMA/memory range가 충돌 없이 정확한 address domain으로 기술됐는가.
3. **Dependency** — clock/regulator/reset/IOMMU/provider graph가 완전한가.
4. **Power** — boot/runtime/suspend state transition에 필요한 method와 state가 일관적인가.
5. **Ownership** — firmware와 OS 중 누가 어느 시점부터 device state를 제어하는가.

OS driver correctness는 code만의 문제가 아니라 **firmware가 전달한 hardware contract까지 포함한 end-to-end property**다.
