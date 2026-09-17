# PART 37 · Firmware and Hardware Description — UEFI, ACPI, Device Tree, handoff

운영체제가 hardware를 다 안다고 시작하는 것은 아니다. firmware는 memory map, device identity, interrupt routing, power topology, clock·regulator dependency 같은 정보를 다음 stage에 전달한다. 이 description이 실제 board와 다르면 driver code가 완벽해도 system은 잘못된 resource를 잡는다. 이 PART는 **firmware handoff, ACPI/Device Tree, dependency graph, validation과 debug**를 한 계약으로 다룬다.

---

## CHAPTER 01 · firmware handoff는 초기 hardware state를 OS가 이해할 수 있는 계약으로 바꾼다

boot firmware는 DRAM, CPU mode, 일부 device와 security state를 준비한 뒤 kernel에 제어권을 넘긴다. 이 순간 kernel은 단순 binary entry address만 받는 것이 아니라 memory map, boot parameter, hardware description, reserved region 같은 정보를 함께 사용한다. handoff structure가 version이나 address에서 틀리면 kernel early boot가 전혀 다른 증상으로 실패할 수 있다.

firmware가 남긴 device state와 kernel driver가 기대하는 reset state가 다르면 첫 probe부터 timeout이 생긴다. bootloader가 이미 clock이나 IOMMU를 설정했는지, kernel이 이를 재초기화하는지 ownership을 명확히 해야 한다.

초기 boot incident에서는 firmware version, handoff pointer, effective boot parameter를 보존한다. kernel log가 시작되기 전 failure는 serial marker와 persistent firmware code를 사용해 마지막 완료 stage를 좁힌다.

---

## CHAPTER 02 · UEFI service는 boot-time 기능과 runtime 기능의 lifetime을 구분한다

UEFI 계열 환경에서는 firmware가 file access, memory allocation, device discovery 같은 boot service를 제공하고 OS가 ExitBootServices 이후 자원 ownership을 가져간다. 그 경계를 넘은 뒤 boot-service pointer를 계속 사용하면 firmware memory가 다른 용도로 재사용될 수 있다.

runtime service는 별도의 mapping과 calling requirement를 가질 수 있어 kernel virtual-memory 전환과 함께 조정해야 한다. firmware bug가 runtime call에서만 드러나면 일반 driver crash처럼 보일 수 있다.

handoff 직전 memory-map key와 ExitBootServices 결과를 기록한다. firmware update 뒤 runtime service fault가 생기면 OS source보다 먼저 firmware version과 mapping 정책을 비교한다.

---

## CHAPTER 03 · firmware memory map은 usable RAM과 reserved region을 분리한다

모든 physical address가 DRAM이라고 가정할 수 없다. firmware code/data, ACPI table, MMIO window, crash log, secure region 등이 physical address space 일부를 차지한다. kernel은 handoff memory map을 기반으로 allocator가 사용할 page와 절대 건드리지 않을 영역을 구분한다.

reserved range를 usable RAM으로 잘못 표시하면 시간이 지난 뒤 page allocator가 device register나 firmware state를 덮어 silent corruption을 만들 수 있다. 반대로 실제 RAM을 reserved로 남기면 capacity가 줄어든다.

boot 시 effective physical map을 artifact로 보존한다. “RAM이 사라졌다”는 문제에서는 DIMM size보다 먼저 usable/reserved classification과 kernel reservation을 비교한다.

---

## CHAPTER 04 · ACPI AML은 firmware policy를 executable description으로 전달한다

ACPI table은 단순 key-value 목록만이 아니라 AML method를 통해 device discovery, power, thermal, routing 관련 동작을 표현할 수 있다. OS의 AML interpreter가 firmware-defined method를 실행하므로 firmware code 품질이 runtime behavior에 직접 들어온다.

method가 hardware state를 잘못 가정하거나 infinite loop·invalid region access를 하면 suspend/resume이나 hotplug 같은 특정 path에서만 장애가 날 수 있다. OS quirk가 필요한 이유도 vendor firmware 현실과 specification 사이 차이 때문이다.

ACPI error에서는 method path, table signature, firmware version을 기록한다. broad kernel workaround를 넣기 전에 table dump와 interpreter error를 재현 가능한 artifact로 만든다.

---

## CHAPTER 05 · DSDT와 SSDT는 hardware description을 여러 table로 구성한다

DSDT는 base system description을 제공하고 SSDT는 추가 device나 platform variation을 보완할 수 있다. 실제 namespace는 여러 table이 합쳐진 결과이므로 한 파일만 보고 device definition 전체를 판단하면 안 된다. firmware revision이나 OEM option이 table set을 바꿀 수도 있다.

중복 object, 잘못된 override, load order 문제는 동일 device가 다른 resource를 갖는 것처럼 보이게 한다. VM에서 host가 synthetic ACPI table을 생성하는 경우 physical firmware와 완전히 다른 input이 된다.

boot 시 loaded table list와 checksum을 저장한다. 정상/실패 machine을 비교할 때 BIOS 설정과 table SHA까지 맞춰야 의미 있는 diff가 된다.

---

## CHAPTER 06 · ACPI identity는 driver binding 전에 device를 구분하는 정보다

ACPI namespace object의 HID, CID, UID 같은 identity는 OS가 어떤 driver와 policy를 적용할지 결정하는 입력이 된다. 같은 human-readable 이름을 가진 두 object라도 underlying identity와 resource가 다를 수 있다.

firmware가 잘못된 ID를 보고하면 엉뚱한 driver가 bind되거나 generic fallback만 사용될 수 있다. device revision 차이를 하나의 ID로 뭉치면 특정 board에서만 quirk가 필요한 문제가 생긴다.

probe failure에서는 kernel driver match table과 firmware identity를 대조한다. product name만으로 driver compatibility를 추정하지 않는다.

---

## CHAPTER 07 · ACPI resource는 MMIO, IRQ, DMA 같은 ownership boundary를 선언한다

device description에는 register window, interrupt, GPIO, DMA relation 같은 resource가 포함된다. OS resource manager는 충돌을 막고 driver가 허용된 영역만 접근하게 해야 한다. firmware가 overlapping range를 주면 두 driver가 같은 hardware를 소유하는 위험이 생긴다.

resource length나 polarity가 틀리면 probe는 성공해도 실제 I/O가 오동작할 수 있다. IRQ trigger mode mismatch는 interrupt storm이나 lost interrupt로 나타날 수 있다.

resource dump와 driver-requested range를 함께 기록한다. hardware access fault가 나면 driver pointer보다 먼저 description의 address·size를 검증한다.

---

## CHAPTER 08 · interrupt routing description은 source와 controller input을 연결한다

platform에는 여러 interrupt controller와 routing bridge가 있을 수 있다. firmware description은 device interrupt source가 어느 controller line과 polarity/trigger semantics를 사용하는지 OS에 알려 준다. routing이 틀리면 handler가 전혀 호출되지 않거나 다른 device event가 섞인다.

shared interrupt와 MSI transition도 platform 정책과 driver capability에 영향을 받는다. suspend/resume 뒤 controller state가 초기화되면 routing을 다시 복원해야 할 수 있다.

IRQ number만 로그에 남기지 말고 source device와 controller mapping을 보존한다. storm incident에서는 line level과 acknowledge sequence까지 확인한다.

---

## CHAPTER 09 · firmware power description은 device dependency와 state transition을 정의한다

ACPI power resource와 platform-specific table은 CPU/device power state, wake capability, thermal relation을 OS policy와 연결한다. device를 off하기 전에 clock·bus·dependency가 안전한 상태인지 확인해야 한다. 잘못된 dependency는 suspend에서만 data corruption을 만든다.

runtime PM과 system sleep이 같은 transition을 공유할 수도 있지만 trigger와 ordering은 다르다. firmware method가 latency를 오래 잡으면 resume tail이 커진다.

power transition trace에 firmware method duration과 device resume 순서를 기록한다. “절전 후만 고장”이면 normal boot path와 다른 description-driven sequence를 비교한다.

---

## CHAPTER 10 · Device Tree는 discoverable하지 않은 hardware topology를 명시한다

Device Tree는 SoC처럼 bus enumeration만으로 device와 resource를 완전히 알기 어려운 platform에서 node/property graph로 hardware를 기술한다. kernel은 compatible string과 reg, interrupt, clocks 같은 property를 사용해 driver와 resource를 연결한다.

DT는 source code가 아니라 deployed hardware contract다. board revision과 맞지 않는 DTB를 사용하면 driver는 정상이어도 wrong GPIO, MMIO, regulator를 잡는다. overlay가 적용되면 final tree가 build artifact의 base와 달라진다.

running system의 effective tree를 dump해 release DTB와 비교한다. filename만 맞다고 같은 내용이라 가정하지 않는다.

---

## CHAPTER 11 · compatible string은 hardware behavior contract를 driver match와 연결한다

Device Tree `compatible`은 단순 제품명이 아니라 driver가 기대할 register layout과 behavior revision을 선택하는 key다. generic compatible과 specific compatible을 함께 나열해 fallback을 제공할 수 있지만 순서와 semantics가 중요하다.

새 silicon revision을 old compatible로만 표시하면 driver가 unsupported workaround를 적용하지 못할 수 있다. 반대로 너무 specific한 string만 넣으면 old kernel이 bind하지 못한다.

binding 문서와 driver match table을 대조한다. 신규 board bring-up에서 probe가 안 되면 node status와 compatible부터 확인한다.

---

## CHAPTER 12 · reg property는 address와 size를 parent bus 규칙에 따라 해석한다

Device Tree의 `reg` 값은 node가 속한 parent의 address/size-cell 규칙에 따라 여러 cell로 구성된다. raw 숫자만 보고 physical address라고 읽으면 bus address translation과 width를 놓칠 수 있다.

32-bit/64-bit 혼합, multiple range, endian 표현을 잘못 작성하면 driver가 완전히 다른 register를 map한다. compiler가 DTB를 만들었다고 semantic correctness가 검증된 것은 아니다.

schema validation과 runtime resource dump를 함께 사용한다. MMIO read가 all-ones 또는 bus fault라면 effective address 계산부터 다시 본다.

---

## CHAPTER 13 · ranges는 child bus address를 parent address space로 변환한다

bus node의 `ranges`는 child-visible address와 parent system address 사이 mapping을 표현한다. PCI host bridge나 SoC internal bus처럼 여러 address domain이 있는 platform에서 필수다. empty ranges의 의미와 property 부재의 의미도 binding에 따라 구분해야 한다.

translation window가 잘못되면 모든 child device가 일정 offset만큼 틀린 address를 사용한다. 개별 driver를 각각 수정하는 것은 root cause를 숨긴다.

resource tree에서 child address→translated physical range를 확인한다. 동일 board의 known-good DT와 parent bus node를 우선 비교한다.

---

## CHAPTER 14 · phandle은 hardware dependency를 graph edge로 표현한다

clock, regulator, interrupt parent, GPIO provider 같은 relation은 phandle reference로 다른 node를 가리킨다. 이 reference는 단순 pointer가 아니라 provider와 consumer의 binding-specific argument를 함께 전달한다.

잘못된 phandle이나 cell count는 consumer probe를 실패시키고 dependency가 준비되지 않은 것처럼 보이게 한다. provider removal이나 overlay 적용 시 dangling relation도 고려해야 한다.

dt-schema와 kernel probe log에서 provider lookup failure를 확인한다. dependency graph를 시각화하면 반복 defer의 cycle을 찾는 데 도움이 된다.

---

## CHAPTER 15 · deferred probe는 dependency가 아직 준비되지 않았다는 정상 상태일 수 있다

driver가 필요한 regulator, clock, bus controller를 아직 찾지 못하면 probe를 나중에 재시도할 수 있다. 따라서 한 번의 defer는 bug가 아니다. 문제는 dependency가 영원히 생기지 않거나 cycle 때문에 모든 consumer가 기다리는 경우다.

부팅 지연을 줄이려고 defer를 무조건 error로 바꾸면 정상 parallel initialization이 깨진다. 반대로 silent retry만 하면 missing DT property가 boot 끝까지 숨는다.

probe defer reason과 retry count를 기록한다. boot complete 시 남은 deferred device 목록을 health signal로 사용한다.

---

## CHAPTER 16 · clock tree는 device frequency와 enable dependency를 계층적으로 연결한다

SoC clock은 root PLL에서 divider/gate를 거쳐 여러 device에 공급될 수 있다. 한 clock 변경이 sibling에 영향을 줄 수 있어 consumer별 requested rate와 shared parent policy를 함께 봐야 한다. clock enable reference가 틀리면 power consumption 또는 device hang이 생긴다.

DVFS와 driver local clock request가 충돌하면 performance가 예상과 달라진다. suspend 시 clock을 너무 일찍 끄면 outstanding DMA가 중단될 수 있다.

clock tree state, enable count, rate change를 trace한다. device timeout 직전 clock gating이 있었는지 확인한다.

---

## CHAPTER 17 · regulator graph는 전원 rail dependency와 voltage constraint를 관리한다

여러 device가 같은 regulator를 공유할 수 있고 consumer마다 minimum/maximum voltage requirement가 다르다. OS regulator framework는 reference와 constraint를 합쳐 실제 rail state를 결정한다. 한 consumer가 disable했다고 shared rail을 바로 끌 수 없는 이유다.

DT/ACPI constraint가 잘못되면 정상 frequency에서만 brownout이나 thermal issue가 나타날 수 있다. always-on rail과 controllable rail도 구분해야 한다.

voltage transition과 device state를 기록한다. 특정 load에서만 reset되는 board는 software exception보다 power rail telemetry를 함께 본다.

---

## CHAPTER 18 · reset controller는 hardware block을 정의된 초기 state로 돌린다

SoC의 여러 peripheral은 shared reset line이나 hierarchy를 사용할 수 있다. reset assert/deassert timing과 clock 조건이 맞지 않으면 block이 half-initialized 상태에 남을 수 있다. reset scope가 넓으면 다른 active consumer를 함께 중단시킬 수 있다.

probe 실패 후 retry를 위해 reset을 반복할 때 outstanding transaction이 완전히 사라졌는지 확인해야 한다. security-sensitive accelerator는 previous context가 reset 뒤 남지 않는지도 중요하다.

reset line state와 consumer list를 추적한다. hot recovery test에서 sibling device가 영향을 받지 않는지 확인한다.

---

## CHAPTER 19 · pinctrl과 GPIO description은 pad electrical state와 logical function을 연결한다

한 physical pin은 GPIO, UART, I2C 같은 여러 function으로 multiplex될 수 있고 pull-up, drive strength 같은 electrical 설정도 필요하다. logical driver가 맞아도 pinmux가 틀리면 bus가 보이지 않는다.

bootloader가 설정한 pin state가 kernel transition 중 바뀌면 early console이 사라지는 현상도 생긴다. suspend용 pin state와 active state를 별도로 정의할 수 있다.

문제 분석에서는 GPIO 번호만 보지 말고 pin group, mux function, active level을 확인한다. board schematic과 effective pinctrl state를 함께 본다.

---

## CHAPTER 20 · I2C와 SPI description은 bus topology와 chip-select/address contract를 정한다

non-enumerable serial bus device는 firmware가 slave address, chip-select, frequency, interrupt를 기술해야 driver가 존재를 알 수 있다. 같은 address를 두 node가 공유하거나 wrong mode를 지정하면 probe가 성공해도 통신이 깨질 수 있다.

I2C mux나 SPI bridge가 있으면 parent-child topology가 실제 signal path를 반영해야 한다. bus recovery와 power sequencing dependency도 고려한다.

logic analyzer evidence와 kernel transaction error를 연결한다. software timeout만으로 device failure를 단정하지 않는다.

---

## CHAPTER 21 · reserved-memory는 일반 allocator가 사용하면 안 되는 physical region을 선언한다

firmware, secure world, framebuffer, DMA pool이 특정 physical region을 계속 소유해야 한다면 kernel page allocator에서 제외해야 한다. reserved-memory description은 단순 capacity 손실이 아니라 cross-component ownership 계약이다.

size와 alignment가 실제 firmware usage보다 작으면 kernel이 일부를 재사용해 rare corruption을 만들 수 있다. 반대로 불필요하게 큰 reserve는 available RAM을 줄인다.

boot map과 DT reserved region을 대조한다. firmware log가 사용하는 address와 kernel allocator 범위가 겹치지 않는지 자동 검사한다.

---

## CHAPTER 22 · dma-ranges는 device bus address와 CPU physical address 관계를 표현한다

일부 bus에서 device가 보는 DMA address와 CPU physical address가 직접 같지 않다. `dma-ranges`는 parent hierarchy를 통해 address translation window를 기술할 수 있다. 이를 무시하면 device가 허용되지 않은 physical range를 가리킬 수 있다.

DMA mask와 window가 맞지 않으면 large-memory configuration에서만 I/O failure가 생길 수 있다. IOMMU가 있는 system에서는 firmware description과 IOMMU mapping이 함께 적용된다.

DMA fault에서 descriptor address, translated physical address, dma-ranges를 한 번에 확인한다. small-memory test success를 portability 증거로 삼지 않는다.

---

## CHAPTER 23 · IOMMU binding은 device requester와 translation domain을 연결한다

firmware description은 어떤 device stream/requester ID가 어느 IOMMU controller와 연결되는지 OS에 알려 줘야 한다. binding이 틀리면 IOMMU fault attribution이 잘못되거나 device가 translation을 우회할 수 있다.

multi-function device, display/GPU처럼 여러 stream ID를 쓰는 hardware는 relation이 복잡하다. passthrough 보안은 이 description이 정확하다는 전제 위에 세워진다.

boot 시 requester→IOMMU mapping을 dump하고 known hardware topology와 대조한다. isolation test에서 다른 device memory 접근을 실제로 차단하는지 확인한다.

---

## CHAPTER 24 · NUMA firmware description은 CPU와 memory locality graph의 출발점이다

server firmware는 CPU와 memory node, distance 정보를 제공해 OS가 scheduler와 allocator locality policy를 세우게 한다. 잘못된 NUMA distance는 thread와 page를 비효율적으로 배치해 성능을 크게 낮출 수 있다.

memory hotplug나 VM에서는 topology가 synthetic할 수 있어 physical hardware와 동일하지 않다. application의 numa tuning도 OS가 노출한 topology를 기준으로 하므로 firmware 오류가 상위 전체에 전파된다.

NUMA node와 distance matrix를 boot artifact로 저장한다. firmware upgrade 전후 bandwidth/latency topology를 비교한다.

---

## CHAPTER 25 · SMBIOS는 inventory를 제공하지만 operational identity와 검증 수준을 구분한다

SMBIOS/DMI 정보는 vendor, product, serial, memory slot 같은 inventory를 제공해 quirk와 자산 관리에 사용된다. 그러나 firmware가 제공하는 문자열이 항상 신뢰 가능한 cryptographic identity는 아니다.

cloud VM이나 vendor tool이 값을 가상화할 수 있고 board replacement에서 serial semantics가 달라질 수 있다. security authorization을 단순 DMI string에 의존하면 안 된다.

incident record에는 SMBIOS snapshot을 넣되 TPM/secure identity와 역할을 분리한다. quirk match가 너무 broad해 다른 모델에 적용되지 않게 한다.

---

## CHAPTER 26 · Device Tree overlay는 runtime·deployment variation을 base tree에 적용한다

overlay는 base DT에 node/property를 추가·수정해 add-on board나 SKU variation을 표현할 수 있다. 편리하지만 apply order와 target identity가 틀리면 다른 node를 변경하거나 duplicate resource를 만들 수 있다.

live overlay removal은 이미 driver가 resource를 소유한 상태와 충돌할 수 있다. boot-time overlay와 runtime overlay의 lifecycle을 구분해야 한다.

release artifact에 base DTB와 적용 overlay 목록·hash를 함께 저장한다. running effective tree를 재구성할 수 있어야 한다.

---

## CHAPTER 27 · firmware validation은 구조뿐 아니라 hardware contract를 검사해야 한다

ACPI compiler나 dt-schema가 syntax·binding을 검사해도 실제 board wiring과 모든 semantic relation을 완전히 증명하지는 못한다. release pipeline은 schema validation에 더해 resource overlap, missing dependency, secure boot signature 같은 checks를 결합해야 한다.

firmware blob이 변조되면 kernel이 trusted description이라고 가정한 resource 경계가 공격자에게 유리하게 바뀔 수 있다. verified boot chain에 firmware와 hardware-description artifact를 포함하는 이유다.

CI에서 table/DT diff를 review 가능한 형태로 출력한다. binary blob만 업데이트하고 의미 변화가 보이지 않는 프로세스는 피한다.

---

## CHAPTER 28 · firmware quirk는 specification보다 현실 hardware의 예외를 좁게 보정한다

시장에 배포된 firmware 오류를 OS가 전부 고칠 수 없을 때 특정 vendor/model/version에 조건부 workaround를 적용한다. quirk는 필요하지만 match 범위가 넓으면 정상 장치까지 비표준 path로 보내 새로운 bug를 만든다.

firmware가 수정된 뒤 old quirk가 계속 적용되면 double workaround가 될 수 있다. quirk 조건에는 version upper/lower bound와 제거 계획이 필요하다.

incident에서 active quirk 목록을 기록한다. hardware refresh 후 performance 차이가 code 변경이 아닌 quirk match 변화인지 확인한다.

---

## CHAPTER 29 · hardware-description debug는 driver보다 먼저 effective input을 확인한다

probe failure가 보이면 source code만 읽기 전에 실제 kernel이 받은 ACPI namespace나 Device Tree를 확인한다. build repository의 DTS와 running DT는 bootloader overlay나 firmware generation 때문에 다를 수 있다.

resource address, compatible/HID, clock/regulator relation을 단계별로 대조하면 driver가 wrong input을 받은 문제와 driver implementation bug를 빠르게 분리할 수 있다.

debug report에 effective node/table excerpt, driver match, probe error를 같이 남긴다. “firmware 문제”라는 추상 결론 대신 어떤 property가 contract를 위반했는지 특정한다.

---

## CHAPTER 30 · firmware contract는 description, ownership, validation, handoff를 끝까지 연결한다

안정적인 platform은 firmware가 어떤 hardware state를 준비하고 어떤 table/tree로 그것을 설명하며 kernel이 언제 ownership을 넘겨받는지 명시해야 한다. address, interrupt, clock, reset, IOMMU relation은 서로 독립 파일이 아니라 하나의 hardware graph다.

release에서는 firmware·bootloader·DT/ACPI·kernel 조합을 version matrix로 검증한다. 하나만 교체해도 handoff assumption이 바뀔 수 있으므로 component 단독 성공을 system success로 보지 않는다.

fault test와 schema validation, effective-runtime dump를 결합해 **실제 hardware와 software description이 같은 계약을 공유하는지** 증명한다. 그래야 driver debugging이 추측이 아니라 검증 가능한 입력에서 시작된다.
