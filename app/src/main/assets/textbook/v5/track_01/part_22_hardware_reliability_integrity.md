# PART 22 · Hardware Reliability — ECC, integrity, corruption, repair

신뢰성은 오류가 절대 생기지 않는 상태가 아니라 오류를 검출하고 격리하며 필요한 경우 수정하고, 수정 결과를 다시 검증하는 체계다. ECC, checksum, redundancy, scrub, repair는 서로 다른 failure domain을 다루므로 한 기술의 성공을 전체 end-to-end integrity로 확대하면 안 된다.

---

## CHAPTER 01 · fault, error, failure

fault는 잘못의 원인, error는 내부 state의 잘못, failure는 외부 service가 계약을 지키지 못한 결과로 구분할 수 있다. 이 셋을 분리하면 detection과 containment 위치가 명확해진다.

같은 hardware fault가 ECC로 수정되면 user-visible failure가 없을 수 있고, 반대로 silent corruption은 error가 존재하지만 외부 failure가 늦게 드러날 수 있다. incident를 failure 하나로 기록하면 early warning을 잃는다.

telemetry에는 cause, detected error, correction action, final outcome을 separate field로 남긴다. reliability review는 어떤 fault가 어떤 error로 변하고 어느 boundary에서 failure가 되는지 chain으로 본다.

---

## CHAPTER 02 · parity

parity는 bit 집합의 홀짝 정보를 추가해 특정 bit error를 감지하는 단순한 redundancy다. 한 bit flip은 검출하기 쉽지만 위치를 특정하거나 모든 multi-bit error를 잡는 것은 아니다.

parity가 맞았다는 사실은 data가 반드시 옳다는 뜻이 아니다. 짝수 개 bit가 바뀌면 parity가 유지될 수 있으므로 threat model과 expected error pattern을 이해해야 한다.

error rate와 protected width를 기준으로 detection strength를 평가한다. stronger ECC나 end-to-end checksum이 필요한 경계를 parity 하나로 대체하지 않는다.

---

## CHAPTER 03 · SECDED

SECDED ECC는 single-error correction과 double-error detection을 제공하는 대표 code다. syndrome을 계산해 single-bit error의 위치를 찾고 수정할 수 있으며 double-bit error는 수정하지 않고 detect한다.

correctable error가 반복되면 component가 열화되고 있다는 신호일 수 있다. 자동 수정이 user symptom을 없애더라도 telemetry를 버리면 predictive maintenance 기회를 잃는다.

syndrome, DIMM topology, corrected count를 장기 추세로 본다. firmware나 memory controller가 correction을 숨기는 경우 OS-visible counter 범위를 확인한다.

---

## CHAPTER 04 · corrected errors

corrected error는 ECC가 data를 정상 상태로 복원해 immediate failure를 막은 사건이다. 그렇다고 reliability risk가 0이 되는 것은 아니다. 특정 DIMM이나 channel에서 correction이 증가하면 latent hardware degradation을 의심해야 한다.

단발 event와 지속 trend를 구분한다. temperature와 workload가 error rate에 영향을 줄 수 있으므로 count를 단순 누적 숫자로만 보지 않는다.

corrected error rate, physical location, temperature, workload를 correlation한다. replacement threshold는 user failure가 난 뒤가 아니라 trend와 vendor policy를 기준으로 정한다.

---

## CHAPTER 05 · uncorrected errors

uncorrected error는 available redundancy로 원래 data를 신뢰성 있게 복원할 수 없는 상태다. OS는 machine check, page poison, process kill, panic 같은 containment action을 선택할 수 있다.

가장 위험한 선택은 corrupted data를 정상처럼 계속 사용하는 것이다. recoverability가 없는 오류는 fail-stop이 silent propagation보다 낫다.

error context, affected physical address, owning process, storage/writeback 여부를 보존한다. incident review는 containment가 충분했는지와 같은 component에서 재발했는지 확인한다.

---

## CHAPTER 06 · memory scrub

memory scrubbing은 background에서 memory를 읽고 ECC correction을 수행해 latent single-bit error가 두 번째 error와 겹치기 전에 발견하는 전략이다. cold page도 주기적으로 확인할 수 있다는 점이 중요하다.

scrub rate가 너무 낮으면 exposure window가 길고, 너무 높으면 memory bandwidth와 energy를 소비한다. workload와 같은 channel을 경쟁하면 application latency에 영향을 줄 수 있다.

scrub coverage와 bandwidth impact를 측정한다. corrected error trend가 줄어드는지, throughput loss가 허용 범위인지 함께 본다.

---

## CHAPTER 07 · scrub modes

scrub은 demand read 중 correction하는 방식, background patrol scrub처럼 여러 mode를 가질 수 있다. 각 mode는 coverage timing과 performance cost가 다르다.

demand scrub만으로는 오랫동안 읽히지 않는 memory의 latent error를 늦게 발견할 수 있다. patrol scrub은 그 gap을 줄이지만 bandwidth를 계속 사용한다.

system role과 failure tolerance에 맞춰 mode와 rate를 정한다. configuration 변경 후 corrected/uncorrected error와 memory bandwidth를 같이 비교한다.

---

## CHAPTER 08 · syndrome location

ECC syndrome은 단순 error flag보다 어느 bit pattern이 깨졌는지 정보를 줄 수 있다. memory topology와 결합하면 DIMM, rank, channel, page 수준으로 recurring fault를 좁힐 수 있다.

physical address interleaving 때문에 software address와 DIMM location이 직관적으로 일치하지 않을 수 있다. vendor mapping과 controller topology가 필요하다.

incident에는 syndrome raw value와 decoded topology를 모두 보존한다. decoding tool version도 함께 기록해 사후 분석에서 같은 결과를 재현할 수 있게 한다.

---

## CHAPTER 09 · memory repair

memory repair는 spare row 사용, bad page retirement, DIMM 교체처럼 여러 수준에서 수행된다. repair action이 성공했다고 해서 원인이 제거됐다는 보장은 없다.

잘못된 component를 교체하거나 bad page만 숨기면 upstream controller fault가 계속될 수 있다. repair 뒤 같은 syndrome pattern이 재발하는지 확인해야 한다.

repair ID, replaced component, pre/post error rate를 기록한다. recovery runbook에 verification window를 포함해 '조치 완료'와 '문제 해결'을 구분한다.

---

## CHAPTER 10 · page offline

page offline은 반복 오류가 난 physical page를 allocator에서 제외해 새 allocation이 들어가지 않게 한다. 이미 mapping된 page나 DMA pin이 있으면 단순히 free list에서 빼는 것보다 복잡하다.

critical kernel page나 unmovable page는 migration이 어렵고 offline이 실패할 수 있다. page retirement가 늘면 usable memory capacity도 줄어든다.

offline attempt, migration result, owning subsystem을 추적한다. 반복되는 bad page가 동일 DIMM에 몰리는지 topology와 함께 본다.

---

## CHAPTER 11 · machine check

machine check는 CPU 또는 platform hardware가 detected error를 software에 전달하는 경로다. corrected, recoverable, fatal severity를 구분하고 context를 보존해야 한다.

machine check handler 자체가 system을 불안정하게 만들 수 있으므로 가능한 최소 작업으로 containment하고 persistent log를 남기는 것이 중요하다.

bank/status register, instruction context, physical address, recent corrected error를 같이 수집한다. reboot 뒤에도 원인을 잃지 않도록 pstore류 persistent evidence를 활용한다.

---

## CHAPTER 12 · end-to-end integrity

end-to-end integrity는 memory, interconnect, controller, storage를 거치는 전체 경로에서 corruption을 감지할 수 있게 보호 정보를 전달한다. 한 계층의 ECC가 다른 구간의 오류까지 보장하지는 않는다.

예를 들어 RAM ECC가 있어도 DMA path나 storage firmware에서 data가 바뀔 수 있다. application checksum이 있으면 더 높은 층에서 이런 corruption을 검출할 수 있다.

각 보호층의 coverage와 gap을 문서화한다. integrity failure가 발생했을 때 어느 hop에서 처음 mismatch가 생겼는지 추적 가능한 metadata를 유지한다.

---

## CHAPTER 13 · checksum threat model

checksum은 accidental corruption을 찾는 데 유용하지만 adversary가 data와 checksum을 함께 바꿀 수 있는 threat model에서는 authenticity를 제공하지 않는다. 보안 요구가 있으면 cryptographic MAC/signature가 필요하다.

빠른 non-cryptographic checksum과 cryptographic hash는 목적이 다르다. 무조건 더 강한 algorithm을 쓰는 대신 error model과 공격 모델을 분리한다.

설계 문서에 무엇을 검출하려는지 적는다. collision tolerance, performance, key management 요구를 함께 평가해 잘못된 primitive 선택을 막는다.

---

## CHAPTER 14 · collision probability

checksum이나 hash는 유한한 output 공간을 사용하므로 서로 다른 input이 같은 value를 가질 가능성이 있다. 데이터 규모가 커질수록 누적 collision risk도 커질 수 있다.

'확률이 매우 낮다'는 문장만으로 충분하지 않다. object 수, checksum bit width, expected lifetime을 넣어 system scale에서 risk를 계산해야 한다.

critical data에는 independent redundancy나 semantic validation을 추가할 수 있다. collision probability와 operational consequence를 함께 보고 algorithm을 고른다.

---

## CHAPTER 15 · block integrity metadata

block integrity metadata는 data block과 함께 guard/reference tag 같은 정보를 전달해 host memory에서 device media까지 data identity를 보호할 수 있다. block layer가 이 metadata를 잃으면 end-to-end protection이 끊긴다.

stack 중간의 mapper나 virtual device가 integrity field를 지원하지 않으면 silent gap이 생길 수 있다. 단순 filesystem checksum과 동일한 기능으로 보지 않는다.

request path별 metadata support를 검증한다. fault injection으로 data/tag mismatch를 만들고 실제 error가 올바른 layer까지 전파되는지 확인한다.

---

## CHAPTER 16 · dm-integrity

dm-integrity는 block device 위에 per-sector integrity tag와 journaling을 제공해 data와 tag의 update ordering을 관리한다. data만 새롭고 tag가 옛 상태인 crash를 피하려면 atomicity가 필요하다.

integrity tag 저장은 capacity와 write amplification을 늘릴 수 있다. journal mode와 bitmap mode는 recovery behavior와 overhead가 다르다.

crash test에서 data/tag pair가 일관되게 recovery되는지 확인한다. throughput benchmark만으로 integrity configuration을 결정하지 않는다.

---

## CHAPTER 17 · dm-verity

dm-verity는 read-only block tree를 cryptographic hash로 검증해 runtime read가 trusted root hash와 일치하는지 확인한다. Android verified boot 같은 immutable system image protection에 활용될 수 있다.

verity는 write-time corruption을 수정하는 기술이 아니라 mismatch를 검출하는 기술이다. root of trust가 안전하지 않으면 tree 전체가 의미를 잃는다.

root hash provenance와 image build identity를 release record에 연결한다. intentional update와 corruption을 구분하도록 versioned artifact contract를 유지한다.

---

## CHAPTER 18 · detection and redundancy

replication이나 RAID 같은 redundancy는 component loss를 견디게 하지만 corruption detection을 자동 보장하지는 않는다. 잘못된 data가 모든 replica로 복제되면 redundancy는 corruption을 더 넓게 보존할 수 있다.

따라서 checksum과 version, semantic invariant가 필요하다. repair source를 고를 때도 replica가 healthy하다는 증거가 있어야 한다.

redundancy policy와 integrity policy를 별도 문서화한다. failover test와 corruption test를 서로 다른 scenario로 유지한다.

---

## CHAPTER 19 · correlated corruption

correlated corruption은 같은 firmware bug, power event, operator action이 여러 replica나 device를 동시에 손상시키는 상황이다. 독립 failure 가정에 기반한 probability 계산이 무너진다.

동일 hardware batch와 동일 software version에 replica를 모두 두면 공통 원인이 더 커질 수 있다. geo redundancy도 동일 deploy artifact corruption을 막지 못할 수 있다.

failure domain을 hardware, software, operator, power 기준으로 나눈다. backup과 replica가 같은 공통 원인에 노출되는지 정기적으로 검토한다.

---

## CHAPTER 20 · erasure coding

erasure coding은 data를 여러 shard와 parity shard로 나눠 일부 shard 손실을 복구한다. storage efficiency는 replication보다 좋을 수 있지만 reconstruction에 CPU와 network bandwidth가 필요하다.

동시에 잃을 수 있는 shard 수를 넘으면 복구 불가능하다. correlated rack failure나 maintenance 중 추가 failure를 capacity model에 포함해야 한다.

rebuild duration과 degraded read latency를 측정한다. 정상 storage cost만 비교하지 말고 failure recovery window를 함께 본다.

---

## CHAPTER 21 · storage scrub

storage scrub은 background에서 block을 읽고 checksum을 검증해 cold data의 latent corruption을 발견한다. 읽히지 않는 data는 user request만으로는 오류를 영원히 발견하지 못할 수 있다.

scrub이 replica mismatch를 찾으면 어느 copy가 옳은지 결정할 evidence가 필요하다. 가장 최신이라는 이유만으로 healthy하다고 단정할 수 없다.

scrub coverage, mismatch count, repair result를 추적한다. scrub interval은 media reliability와 dataset size를 기준으로 결정한다.

---

## CHAPTER 22 · bit rot

bit rot는 장기 보존 중 media state가 변해 data가 손상되는 현상을 통칭한다. HDD magnetic decay, flash retention 같은 mechanism은 다르지만 cold data 검증이라는 운영 요구는 비슷하다.

backup도 오랫동안 검증하지 않으면 같은 문제를 가질 수 있다. 복구 시점에 처음 corruption을 발견하면 healthy copy가 이미 사라졌을 수 있다.

periodic checksum scan과 restore drill을 수행한다. backup age와 verification date를 함께 관리해 '존재하는 backup'과 '복구 가능한 backup'을 구분한다.

---

## CHAPTER 23 · flash reliability

flash reliability는 program/erase cycle, retention, read disturb, temperature, controller ECC에 의해 달라진다. device health percentage 하나가 모든 mechanism을 대표하지는 않는다.

wear가 진행될수록 raw bit error rate가 올라가고 controller가 더 강한 ECC를 사용할 수 있다. correction margin이 줄면 latency가 변할 수도 있다.

SMART/NVMe health log와 media error trend를 장기 저장한다. capacity planning에 write endurance와 workload write amplification을 포함한다.

---

## CHAPTER 24 · write amplification

write amplification은 host logical write보다 media 내부 physical write가 더 많아지는 현상이다. FTL garbage collection, filesystem journaling, database WAL이 서로 겹치면 전체 amplification이 커질 수 있다.

amplification은 endurance를 줄이고 background GC를 늘려 tail latency를 악화시킨다. 단순 host bytes written만으로 media wear를 추정하면 부족하다.

logical/physical write 비율과 device wear를 함께 본다. workload batching이나 overprovisioning 변경이 실제 media write를 줄였는지 검증한다.

---

## CHAPTER 25 · silent corruption

silent corruption은 error signal 없이 잘못된 data가 정상처럼 전달되는 상태다. 즉시 crash보다 발견이 늦어 downstream cache, replica, backup까지 오염시킬 수 있어 더 위험하다.

storage checksum이 맞아도 application logic이 잘못된 value를 저장하면 semantic corruption이 남는다. integrity는 여러 layer에서 필요하다.

source-of-truth version, checksum, semantic invariant를 결합한다. corruption incident에서는 최초 잘못된 write 시점과 propagation 범위를 추적한다.

---

## CHAPTER 26 · semantic invariant

semantic invariant는 데이터가 checksum상 정상이어도 domain rule을 만족하는지 확인한다. 예를 들어 음수가 될 수 없는 balance나 monotonic sequence가 깨지면 storage bytes가 온전해도 system state는 잘못된 것이다.

모든 invariant를 runtime에 강제하면 비용이 클 수 있어 critical boundary와 asynchronous audit를 나눌 수 있다. repair 후에도 semantic check가 필요하다.

constraint violation과 repair history를 감사 가능하게 남긴다. low-level integrity와 business correctness를 같은 지표로 합치지 않는다.

---

## CHAPTER 27 · repair verification

repair는 data를 다른 replica에서 복사하는 순간 끝나지 않는다. source가 healthy했는지, target write가 정확했는지, metadata와 version이 일치하는지 다시 검증해야 한다.

repair 중 concurrent update가 발생하면 stale data로 되돌릴 수 있다. generation이나 compare-and-swap semantics로 repair 대상 version을 고정한다.

post-repair checksum과 semantic validation을 수행한다. repair event가 많아지는 추세는 underlying failure가 해결되지 않았다는 신호일 수 있다.

---

## CHAPTER 28 · reliability telemetry

reliability telemetry는 corrected error, uncorrected error, media wear, checksum mismatch, repair count를 장기 추세로 저장해야 한다. incident 때만 수집하면 degradation pattern을 볼 수 없다.

raw count는 uptime과 workload에 따라 달라지므로 rate와 topology를 함께 본다. 동일 DIMM, rack, firmware에서 cluster가 형성되는지 중요하다.

alert는 user failure 이전의 leading indicator를 포함한다. telemetry pipeline 자체가 data loss를 겪지 않는지도 확인한다.

---

## CHAPTER 29 · fault injection

fault injection은 bit flip, I/O error, checksum mismatch, partial repair를 의도적으로 만들어 detection·containment·recovery가 실제로 작동하는지 확인한다. 문서에 적힌 recovery가 실행 가능한지 검증하는 방법이다.

production과 다른 failure mode만 주입하면 false confidence가 생긴다. 실제 hardware/firmware error와 유사한 timing과 scope를 설계해야 한다.

실험에는 blast radius와 abort condition을 둔다. injection 후 모든 state가 원래 invariant로 수렴하는지 확인하고 증거를 보존한다.

---

## CHAPTER 30 · reliability contract

reliability contract는 어떤 오류를 검출하고, 어디까지 자동 수정하며, 어느 경우 fail-stop하고, repair 후 무엇을 재검증하는지 명시해야 한다. '고가용성' 같은 단어만으로는 부족하다.

ECC, checksum, replica, backup은 각각 다른 layer의 guarantee를 제공한다. gap이 존재하면 어느 상위 layer가 책임지는지 정해야 한다.

release와 operations gate는 fault injection, restore drill, telemetry review를 포함한다. 최종 목표는 오류를 숨기는 것이 아니라 오류가 있어도 silent corruption 없이 예측 가능한 방식으로 회복하는 것이다.
