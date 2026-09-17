# PART 22 · Hardware Reliability — ECC, scrubbing, silent corruption, end-to-end integrity

정상적인 code와 정상적인 synchronization이 있어도 bit가 뒤집히거나, controller가 잘못된 data를 돌려주거나, I/O path가 sector를 잘못된 위치에 기록하면 결과는 틀릴 수 있다. Reliability engineering은 `hardware는 항상 정확하다`를 기본 가정으로 두지 않는다. 오류를 **검출할 수 있는가, 수정할 수 있는가, 어느 component까지 격리할 수 있는가, 손상된 상태가 더 넓게 전파되기 전에 멈출 수 있는가**를 계층별로 설계한다.

---

## CHAPTER 01 · Failure, error, fault를 구분하면 조사 범위가 선명해진다

Hardware reliability 문맥에서는 원인이 되는 fault, 내부 state가 잘못된 error, 외부 service가 specification을 위반한 failure를 분리하면 causal chain을 추적하기 쉽다. DRAM cell의 bit flip은 fault에서 시작할 수 있고 ECC로 수정되면 application-visible failure까지 가지 않는다. 반대로 수정되지 않은 bit가 pointer나 filesystem metadata에 들어가면 더 큰 corruption으로 증폭될 수 있다.

따라서 `서버가 죽었다`는 incident description만으로는 부족하다. 어떤 physical fault가 있었는지, 어느 계층이 error를 감지했는지, recovery가 성공했는지, data integrity가 유지됐는지까지 기록해야 한다.

---

## CHAPTER 02 · Parity는 검출 능력과 수정 능력이 다르다

Parity bit는 data bits의 특정 parity 조건을 유지해 일부 bit error를 탐지할 수 있다. 그러나 어느 bit가 잘못됐는지 위치를 알아내지 못하면 스스로 수정할 수 없다. Error-detecting code와 error-correcting code는 목적이 다르다.

ECC scheme은 추가 redundancy를 사용해 syndrome을 계산하고 특정 error pattern을 detect/correct할 수 있다. `ECC memory`라는 이름만으로 capability를 단정하지 말고 controller와 memory가 어떤 error class를 correct/detect하는지 확인해야 한다.

---

## CHAPTER 03 · SECDED는 대표적인 correction/detection contract다

Single Error Correction, Double Error Detection 계열은 한 codeword에서 single-bit error를 수정하고 double-bit error를 검출하는 대표적 구조다. 중요한 점은 `ECC가 있으면 모든 memory error가 해결된다`가 아니라 **보장하는 error model의 범위가 명시적**이라는 것이다.

여러 bit가 특정 pattern으로 동시에 손상되면 correction capability를 넘을 수 있다. Chip-level failure, burst error, row fault를 다루기 위해 더 강한 coding/organization을 사용할 수 있고 platform마다 실제 보호 수준이 다르다.

---

## CHAPTER 04 · Corrected error가 많아지는 것은 failure가 아니라 warning signal일 수 있다

EDAC subsystem은 corrected와 uncorrected memory error를 구분해 reporting할 수 있다. Corrected error는 application data가 이미 틀렸다는 뜻은 아닐 수 있지만 특정 DIMM/channel/row에서 error rate가 증가하면 hardware degradation의 선행 신호가 될 수 있다.

운영에서는 `corrected니까 무시`와 `한 번 나왔으니 즉시 교체` 사이에 정책이 필요하다. Error rate, location concentration, time trend, workload correlation을 수집하고 threshold를 정해 predictive maintenance를 할 수 있다.

---

## CHAPTER 05 · Uncorrected error도 항상 즉시 process crash와 같은 의미는 아니다

Uncorrected error가 발견된 physical page가 아직 allocation되지 않았거나 recovery 가능한 경로에 있다면 OS가 page를 격리할 수 있다. 반대로 kernel-critical data나 active process memory에 영향을 주면 machine check, process termination, system reset 같은 강한 대응이 필요할 수 있다.

Error severity와 containment 범위가 중요하다. Monitoring system은 corrected/uncorrected/fatal을 같은 counter에 합치지 말고 page/address/component와 함께 기록해야 한다.

---

## CHAPTER 06 · Memory scrubbing은 사용하기 전에 latent error를 발견하려는 전략이다

ECC가 memory access 때만 검사된다면 오랫동안 읽지 않은 location의 error는 잠복할 수 있다. Scrubbing은 memory media를 주기적으로 읽어 ECC를 검사하고 correction 가능한 error를 다시 써서 정상 codeword로 복구할 수 있다. 이는 correctable error가 추가 fault와 겹쳐 uncorrectable state가 되는 확률을 줄인다.

Scrub rate를 높이면 detection latency는 줄지만 memory bandwidth와 power를 소비한다. Reliability와 performance 사이의 trade-off를 workload와 error rate에 맞춰 조정해야 한다.

---

## CHAPTER 07 · Patrol scrub와 demand scrub는 관찰 시점이 다르다

Demand scrub는 normal access 중 발견한 corrected error를 해당 location에 다시 써 복구하는 방식과 연결되고, patrol/background scrub는 application access와 무관하게 memory 영역을 순회한다. Platform은 memory controller나 device level에서 다른 scrub mechanism을 제공할 수 있다.

어떤 scrub이 활성화됐는지 모르면 `ECC가 알아서 고친다`는 가정이 잘못될 수 있다. Firmware/kernel configuration과 실제 scrub counter를 확인해야 한다.

---

## CHAPTER 08 · Error syndrome과 physical location은 교체 가능한 부품으로 mapping되어야 한다

ECC syndrome은 error pattern을 설명하지만 운영자는 실제 FRU—DIMM, channel, rank, bank—를 찾아야 한다. EDAC는 memory controller hierarchy와 error location 정보를 userspace에 전달할 수 있다. Machine topology가 복잡하면 physical address에서 DIMM label까지 mapping이 잘못되어 엉뚱한 부품을 교체할 수 있다.

Reliability tool은 logical address, syndrome, channel/rank, board slot labeling을 일관되게 관리해야 한다.

---

## CHAPTER 09 · Memory repair는 replacement보다 작은 단위에서 failure를 격리할 수 있다

일부 platform은 post-package repair, row repair, sparing 같은 mechanism으로 특정 failing region을 다른 physical resource로 대체할 수 있다. 이런 repair는 media의 물리적 degradation을 software-visible address space에서 숨긴다.

하지만 repair resource도 유한하다. Repaired row 수와 spare consumption trend를 monitoring하면 device가 얼마나 degradation됐는지 판단하는 근거가 된다. Repair success가 underlying failure 원인을 없앤 것은 아니다.

---

## CHAPTER 10 · Page offlining은 bad physical page를 allocator에서 제거하는 containment다

OS가 특정 physical page에 반복 error가 발생한다고 판단하면 그 page를 future allocation에서 제외할 수 있다. 이미 사용 중인 page는 migration이나 process impact가 필요할 수 있다. 이 접근은 whole-machine shutdown보다 작은 blast radius로 fault를 containment한다.

Page offline count가 늘어나는 시스템은 usable memory capacity와 fragmentation에도 영향을 받는다. Reliability metric과 capacity metric을 따로 보지 말아야 한다.

---

## CHAPTER 11 · CPU machine-check와 memory EDAC는 같은 error path가 아닐 수 있다

Processor는 cache, interconnect, execution unit, memory controller 등 다양한 hardware error를 machine-check architecture를 통해 보고할 수 있다. EDAC는 memory/IO error handling의 한 subsystem이고 architecture-specific RAS report가 별도 path로 올라올 수 있다.

Incident response는 kernel panic log 한 줄이 아니라 hardware error record를 decode해 bank/status/address를 분석해야 한다. 동일 symptom의 reboot라도 power loss, watchdog, machine check는 완전히 다른 failure class다.

---

## CHAPTER 12 · Cache parity/ECC가 있어도 end-to-end data integrity가 자동 보장되지는 않는다

CPU cache가 error를 detect/correct하고 DRAM도 ECC를 사용해도 DMA engine, interconnect, storage controller, cable/path, device firmware에서 corruption이 생길 수 있다. 각 segment가 보호돼 있어도 보호 domain 사이 handoff가 검증되지 않으면 gap이 남는다.

End-to-end integrity는 data가 producer에서 consumer까지 이동하는 전체 path에 tag/checksum이 어떻게 유지되는지 본다. `각 장치에 ECC 있음`은 end-to-end proof가 아니다.

---

## CHAPTER 13 · Checksum은 accidental corruption detection과 malicious integrity를 구분해야 한다

CRC는 random bit error를 탐지하는 데 효율적이지만 공격자가 content와 checksum을 함께 변경할 수 있는 threat model에서는 authentication이 아니다. HMAC이나 digital signature는 secret/private key와 결합해 malicious modification 탐지를 목표로 한다.

Integrity mechanism 선택은 `checksum이 있다`가 아니라 **어떤 adversary/error model을 막는가**로 결정한다. Storage bit rot와 hostile disk modification은 다른 threat model이다.

---

## CHAPTER 14 · Collision probability는 checksum width와 data volume에 따라 누적된다

짧은 checksum은 우연히 같은 digest가 나올 가능성이 있다. 대규모 storage에서 billions of blocks를 장기간 읽고 쓰면 per-block error probability가 작아도 fleet-level event는 현실적인 문제가 된다. Hash/checksum width와 detection strength는 data volume과 required reliability에 맞춰 선택해야 한다.

Reliability SLO는 per-operation error rate와 전체 fleet exposure를 함께 본다. `확률이 매우 낮다`는 설명만으로 충분하지 않다.

---

## CHAPTER 15 · Block integrity metadata는 data와 위치/순서까지 보호할 수 있다

Linux block integrity framework는 sector data에 checksum/protection information을 연결해 device가 실제로 application이 의도한 block을 올바른 순서·위치에 저장했는지 검증하는 end-to-end protection을 지원할 수 있다. T10 protection information류는 data checksum 외에 reference/guard 정보로 misdirected write를 탐지할 수 있다.

Data가 손상되지 않았어도 wrong LBA에 정상 data가 기록되면 filesystem 관점에서는 corruption이다. Integrity metadata는 content뿐 아니라 placement semantics를 보호해야 한다.

---

## CHAPTER 16 · dm-integrity는 data와 integrity tag의 crash consistency까지 다룬다

Sector data와 checksum tag를 따로 쓰면 crash가 그 사이에 발생해 둘이 서로 다른 generation을 나타낼 수 있다. dm-integrity는 journal mode에서 data와 tag update를 atomic하게 복구 가능하도록 관리한다. Integrity metadata도 data이므로 자체 crash-consistency protocol이 필요하다.

Direct/bitmap/journal mode는 performance와 reliability 보장이 다르다. `checksum을 추가하면 끝`이 아니라 checksum state의 durable ordering까지 설계해야 한다.

---

## CHAPTER 17 · dm-verity는 read-only Merkle tree로 immutable block integrity를 검증한다

Verity target은 data block의 cryptographic hash를 tree 구조로 연결하고 trusted root hash에 도달할 때까지 verification한다. Read-only image가 expected bytes와 다른 경우 I/O failure로 surface할 수 있다. 이는 writable data correction mechanism과 다르다.

Verified boot chain에서는 root hash 자체의 authenticity가 signature/trust anchor로 보호돼야 한다. Hash tree만 있고 root를 공격자가 바꿀 수 있다면 integrity guarantee가 없다.

---

## CHAPTER 18 · Detection without redundancy는 recovery를 제공하지 않는다

Checksum이 corruption을 발견해도 올바른 원본이 없으면 복구할 수 없다. Replication, erasure coding, backup, mirror 같은 redundancy가 repair source를 제공해야 한다. Detection과 recovery는 별도 capability다.

반대로 redundancy만 있고 checksum이 없으면 corrupted replica를 정상 data로 오인해 다른 replica에 전파할 수 있다. Reliable storage는 **detect → identify good copy → repair → verify** cycle을 갖춰야 한다.

---

## CHAPTER 19 · Replication은 software bug와 correlated corruption에 약할 수 있다

동일 application bug가 모든 replica에 같은 잘못된 data를 쓰면 replication은 오류를 충실하게 복제한다. Firmware bug, power event, operator command처럼 correlated failure가 여러 copy를 동시에 손상할 수도 있다.

Redundancy design은 failure independence를 평가해야 한다. 다른 availability zone, 다른 power domain, immutable backup, delayed replica는 서로 다른 correlated failure를 줄이는 전략이다.

---

## CHAPTER 20 · Erasure coding은 storage overhead와 repair cost를 교환한다

Erasure code는 data를 여러 fragment와 parity fragment로 변환해 일부 fragment 손실에서도 원본을 복원할 수 있게 한다. Full replication보다 storage overhead를 줄일 수 있지만 encoding/decoding CPU와 network repair traffic이 증가한다.

Failure가 발생한 뒤 rebuild 중에는 remaining redundancy가 줄고 background repair가 production I/O와 경쟁할 수 있다. Capacity planning은 steady state뿐 아니라 degraded rebuild state를 포함해야 한다.

---

## CHAPTER 21 · Scrub는 storage에서도 latent corruption을 조기에 발견한다

Cold data는 몇 달 동안 읽히지 않을 수 있다. Read-time checksum verification만 있으면 corruption 발견이 너무 늦어 backup/replica도 이미 같은 damage를 가질 수 있다. Background scrub는 data를 주기적으로 읽고 checksum을 검증해 latent error를 조기에 찾는다.

Scrub I/O가 production workload를 방해하지 않도록 rate limit과 priority가 필요하다. Error risk가 높은 device는 dynamic scrub rate를 높이는 policy도 가능하다.

---

## CHAPTER 22 · Bit rot라는 용어는 실제 failure mechanism을 가리지 않도록 사용해야 한다

장기간 저장된 data가 손상되는 현상을 관행적으로 bit rot라고 부르지만 underlying cause는 media wear, charge leakage, controller bug, cosmic/particle upset, firmware, path corruption 등 다양하다. 원인을 특정하지 않고 `bit rot`로 뭉개면 mitigation을 잘못 선택한다.

Error location과 device health telemetry를 보존해야 동일 pattern이 반복되는지 판단할 수 있다.

---

## CHAPTER 23 · NAND flash reliability는 erase/program cycle과 controller policy에 강하게 의존한다

Flash media는 program/erase cycle이 쌓이면서 raw bit error rate가 증가할 수 있고 controller는 ECC, wear leveling, bad-block management, read-retry를 사용해 logical block abstraction을 유지한다. Application은 raw NAND state를 직접 보지 않는 경우가 많다.

Drive health metric만 보고 data integrity를 보장할 수는 없다. Controller가 correction margin을 소진하기 전에 media replacement/refresh policy가 필요하다.

---

## CHAPTER 24 · Write amplification은 reliability와 endurance에도 영향을 준다

Small random writes가 flash translation layer와 garbage collection에서 더 큰 physical write를 만들면 media wear가 application write volume보다 빠르게 증가한다. Filesystem journaling, database WAL, dm-integrity journal이 각각 추가 writes를 만들 수 있다.

Durability layer를 여러 겹 쌓을 때 write amplification을 측정해야 한다. Reliability mechanism 자체가 endurance budget을 소비하므로 workload에 맞는 configuration이 필요하다.

---

## CHAPTER 25 · Silent data corruption은 crash보다 위험할 수 있다

Crash는 즉시 감지되지만 silent corruption은 정상 response처럼 전파될 수 있다. Wrong result가 database, backup, analytics model에 저장되면 시간이 지난 뒤 root cause와 repair point를 찾기 어려워진다.

Critical computation은 input checksum, invariant, redundant calculation, range validation 같은 semantic detection을 hardware integrity 위에 추가할 수 있다. Hardware ECC가 application logic correctness를 검증하는 것은 아니다.

---

## CHAPTER 26 · Semantic invariant는 checksum이 잡지 못하는 corruption을 잡을 수 있다

Byte가 정확히 저장됐어도 값 자체가 domain rule을 위반할 수 있다. Account balance total, B-tree ordering, object reference graph처럼 구조적 invariant를 주기적으로 검증하면 software bug나 misdirected logical update를 발견할 수 있다.

Integrity hierarchy는 physical bit → block checksum → filesystem metadata → database constraint → business invariant로 올라간다. 한 층의 success가 위 층 correctness를 증명하지 않는다.

---

## CHAPTER 27 · Repair는 bad copy를 good copy로 덮는 순간까지 검증해야 한다

Corruption을 탐지한 뒤 replica B가 정상이라고 가정해 replica A를 덮어썼는데 B도 오래된 data라면 correctness가 회복되지 않는다. Repair source를 선택할 때 version/epoch/checksum/quorum 정보를 사용해야 한다.

Repair 완료 후 read-back verification과 audit record를 남겨야 한다. `복사 성공`은 integrity recovery 완료와 동일하지 않다.

---

## CHAPTER 28 · Reliability telemetry는 corrected error trend와 data-integrity mismatch를 분리한다

Memory corrected ECC, uncorrected ECC, PCIe AER, storage media error, checksum mismatch, verity failure는 서로 다른 failure domain이다. 하나의 `hardware error count`로 합치면 predictive signal을 잃는다.

Metric에는 component identity, physical location, severity, corrected 여부, first/last timestamp, count rate, repair action을 포함해야 한다. Fleet 분석은 특정 model/firmware에 error가 집중되는지 찾아야 한다.

---

## CHAPTER 29 · Fault injection으로 detection path 자체를 검증한다

Reliability mechanism은 실제 error가 발생하기 전까지 code path가 실행되지 않을 수 있다. Test environment에서 memory/error injection, corrupted block, checksum mismatch, partial write를 의도적으로 만들어 alert와 containment가 작동하는지 검증해야 한다.

Test는 data loss를 만들 수 있으므로 production과 격리된 환경에서 수행하고, injection point·expected alarm·repair result를 명시한다. `ECC enabled`라는 configuration 확인만으로 operational readiness를 증명하지 않는다.

---

## CHAPTER 30 · End-to-end reliability는 detect·contain·recover·prove의 연속 계약이다

완전한 integrity path는 네 단계로 평가한다.

1. **Detect** — error가 silent하게 지나가지 않는가.
2. **Contain** — 손상된 component/page/block/replica가 더 넓은 state를 오염시키기 전에 격리되는가.
3. **Recover** — 신뢰할 수 있는 redundant source나 repair mechanism으로 correctness를 회복하는가.
4. **Prove** — repair 후 checksum·invariant·read-back evidence로 정상 상태를 다시 증명하는가.

ECC, checksum, replication, backup은 각각 이 네 단계의 일부만 담당한다. 수석 개발자는 `무슨 보호 기능이 켜져 있는가`가 아니라 **어느 failure model에서 어느 단계까지 보장되는가**를 문서와 실행 증거로 설명해야 한다.
