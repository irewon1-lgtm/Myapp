# PART 24 · Storage Media and NVMe — queue, FTL, persistence

스토리지 성능과 durability는 filesystem API 한 줄로 설명되지 않는다. host block request, NVMe queue, controller cache, FTL, NAND media가 서로 다른 queue와 ordering을 가진다. completion, persistence, atomicity를 분리하고 device 내부 GC와 wear까지 포함해야 실제 tail latency와 crash behavior를 이해할 수 있다.

---

## CHAPTER 01 · block abstraction

block layer는 filesystem이나 database의 logical request를 fixed-size addressable block operation으로 바꿔 device에 전달한다. 이 abstraction은 NAND page나 erase block 같은 media 내부 geometry를 숨긴다.

logical block이 연속이라고 physical media도 연속이라는 보장은 없다. controller FTL, RAID, mapper가 중간에 존재할 수 있어 sequential workload와 actual media behavior가 달라진다.

request size, offset, queue path를 device telemetry와 연결한다. application optimization을 평가할 때 logical I/O와 physical work를 구분한다.

---

## CHAPTER 02 · NVMe queues

NVMe는 host memory의 submission queue와 completion queue를 사용해 여러 command를 병렬 처리한다. CPU별 queue pair를 사용하면 lock contention을 줄이고 device parallelism을 활용할 수 있다.

queue 수가 많다고 무조건 빠른 것은 아니다. controller 내부 channel과 interrupt placement가 병목이면 software queue만 늘어난다.

queue별 depth, completion latency, CPU affinity를 측정한다. 특정 queue에 load가 몰리는지와 device 전체 saturation을 함께 본다.

---

## CHAPTER 03 · admin and I/O queues

admin queue는 identify, configuration, firmware, namespace 같은 management command를 담당하고 I/O queue는 read/write data path를 주로 처리한다. 두 path의 latency와 failure 영향이 다르다.

admin command가 막혀도 기존 I/O가 일정 시간 진행할 수 있고, 반대로 controller reset은 모든 queue에 영향을 줄 수 있다. 한 queue timeout을 device 전체 failure로 단정하지 않는다.

log에는 command class, queue ID, controller state를 보존한다. recovery는 admin path와 data path를 분리해 검증한다.

---

## CHAPTER 04 · queue count

queue count는 CPU parallelism과 interrupt distribution을 늘릴 수 있다. 하지만 controller가 처리할 수 있는 실제 hardware queue와 channel 수를 넘으면 memory·interrupt overhead만 증가할 수 있다.

너무 적은 queue는 여러 core가 shared queue를 경쟁하게 하고, 너무 많은 queue는 load imbalance를 만들 수 있다. NUMA topology도 queue placement에 영향을 준다.

queue count 변경 전후 lock contention, CPU utilization, p99 latency를 비교한다. 최대값보다 workload에 맞는 operating point를 찾는다.

---

## CHAPTER 05 · queue depth

queue depth는 outstanding command 수를 나타내며 device가 내부 parallelism을 활용하게 한다. 낮으면 device가 idle하고, 너무 높으면 throughput 증가 없이 wait time만 커질 수 있다.

saturation 이후 추가 request는 service capacity를 만들지 않는다. synchronous latency-sensitive workload와 batch throughput workload의 적정 depth가 다르다.

throughput, average, p95/p99 latency를 depth sweep으로 측정한다. queue depth를 benchmark score 하나로 고정하지 않는다.

---

## CHAPTER 06 · completion mode

NVMe completion은 interrupt, polling, hybrid 방식으로 처리할 수 있다. interrupt는 CPU를 양보할 수 있지만 delivery overhead가 있고 polling은 latency를 줄이는 대신 CPU를 계속 소비한다.

높은 IOPS에서 interrupt coalescing이 유리할 수 있지만 individual request latency는 늘어날 수 있다. idle workload에서 polling은 energy를 낭비한다.

completion latency와 CPU cost를 함께 측정한다. workload class별 mode 선택이 SLO와 energy budget을 만족하는지 확인한다.

---

## CHAPTER 07 · PRP and SGL

PRP와 SGL은 host buffer가 physical memory에서 여러 page/segment로 흩어져 있을 때 device가 DMA할 address 목록을 표현한다. I/O length와 alignment에 따라 descriptor 구조가 달라진다.

잘못된 address나 lifetime은 device DMA가 unrelated memory를 건드리는 심각한 corruption으로 이어질 수 있다. command completion 전 buffer와 mapping을 재사용하면 안 된다.

DMA mapping, segment count, completion generation을 추적한다. large I/O가 descriptor overhead와 IOMMU pressure를 얼마나 늘리는지도 본다.

---

## CHAPTER 08 · namespace

NVMe namespace는 controller 아래의 logical block address space를 분리한다. filesystem mount namespace와는 다른 개념이며 capacity, LBA format, feature가 namespace별로 달라질 수 있다.

같은 controller에 여러 namespace가 있어도 physical resource를 공유할 수 있다. 한 namespace의 heavy workload가 다른 namespace latency에 영향을 줄 수 있다.

namespace ID, controller, queue telemetry를 함께 기록한다. logical isolation과 physical performance isolation을 구분한다.

---

## CHAPTER 09 · NAND asymmetry

NAND flash는 read, program, erase가 서로 다른 latency와 granularity를 가진다. page는 write할 수 있지만 erase는 더 큰 block 단위로 수행되는 구조가 일반적이다.

in-place overwrite가 어렵기 때문에 controller는 새 page에 write하고 old page를 invalid 처리한다. 이 비대칭이 FTL과 garbage collection을 필요하게 만든다.

read/write latency를 단순 media speed 한 숫자로 보지 않는다. sustained write에서 erase와 GC가 개입하는 시점을 측정한다.

---

## CHAPTER 10 · FTL

Flash Translation Layer는 host logical block address를 physical NAND location으로 매핑한다. mapping, garbage collection, wear leveling, overprovisioning을 controller 내부에서 관리한다.

host는 같은 LBA를 overwrite하지만 device는 다른 physical page에 write할 수 있다. 따라서 logical access pattern만으로 physical write와 wear를 정확히 알 수 없다.

host bytes written과 device wear/physical write telemetry를 함께 본다. FTL behavior는 controller firmware와 fill level에 따라 달라질 수 있다.

---

## CHAPTER 11 · out-of-place update

out-of-place update는 기존 NAND page를 직접 덮지 않고 새 page에 data를 쓴 뒤 mapping을 바꾼다. old page는 stale 상태가 되어 이후 garbage collection 대상이 된다.

random overwrite가 많으면 valid/stale page가 섞여 GC copy 비용이 증가할 수 있다. 같은 host write rate라도 physical amplification이 달라진다.

write pattern과 device amplification을 비교한다. workload batching이나 log-structured layout이 실제 media write를 줄이는지 검증한다.

---

## CHAPTER 12 · garbage collection

flash garbage collection은 erase block의 valid page를 다른 위치로 이동하고 block을 erase해 free space를 만든다. foreground write와 겹치면 controller 내부 copy가 latency spike를 만들 수 있다.

free block reserve가 줄어든 full device에서 GC가 더 공격적으로 동작할 수 있다. fresh-drive benchmark가 장기 사용 state보다 훨씬 좋은 이유다.

fill level, internal GC activity, p99 write latency를 함께 측정한다. sustained workload를 충분히 길게 실행해 steady state를 본다.

---

## CHAPTER 13 · overprovisioning

overprovisioning은 host에 노출하지 않는 spare flash capacity를 확보해 FTL이 GC와 wear leveling에 사용할 여유를 만든다. 여유가 많으면 valid page 이동 없이 free block을 확보하기 쉬워진다.

usable capacity와 sustained performance/endurance 사이 trade-off가 있다. 단순히 logical capacity를 최대화하면 long-term write latency가 악화될 수 있다.

device fill level과 write amplification을 비교한다. workload의 write intensity에 맞는 spare capacity를 선택한다.

---

## CHAPTER 14 · wear leveling

wear leveling은 program/erase cycle이 특정 block에 몰리지 않게 data를 이동해 flash 수명을 늘린다. dynamic hot data와 static cold data까지 고려하는 정책이 있을 수 있다.

wear balancing 자체도 internal copy를 만들어 write amplification과 latency를 늘릴 수 있다. endurance와 performance를 분리해서 평가해야 한다.

wear distribution과 remaining life telemetry를 장기 추적한다. 특정 workload가 일부 region에 wear를 집중시키는지 확인한다.

---

## CHAPTER 15 · retention and read disturb

retention error는 시간이 지나며 cell charge가 변해 bit margin이 줄어드는 문제고 read disturb는 반복 read가 주변 cell state에 영향을 줄 수 있는 현상이다. 둘 다 cold data reliability와 연결된다.

controller ECC가 많은 오류를 수정하는 동안 host는 정상처럼 볼 수 있다. correction margin이 줄어들면 eventual uncorrectable error 가능성이 커진다.

media error와 read-retry telemetry를 장기 저장한다. backup/scrub 정책은 cold data의 retention risk를 포함한다.

---

## CHAPTER 16 · internal ECC

flash controller 내부 ECC는 NAND raw bit error를 수정해 host에 clean block을 제공한다. 하지만 controller 이후 DMA, memory, software path corruption까지 보호하는 end-to-end mechanism은 아니다.

ECC correction이 증가하면 media aging signal이 될 수 있다. host가 correctable event를 볼 수 있는 범위는 device마다 다르다.

device health와 block-level checksum을 함께 사용한다. internal ECC 존재를 이유로 application integrity 검증을 제거하지 않는다.

---

## CHAPTER 17 · discard

discard/TRIM은 filesystem이 더 이상 필요한 data가 없는 LBA를 device에 알려 FTL이 해당 page를 live data로 취급하지 않게 한다. GC 선택 폭을 넓혀 sustained write behavior를 개선할 수 있다.

너무 빈번한 synchronous discard는 foreground latency를 만들 수 있다. queued/batched discard와 security erase semantics도 구분해야 한다.

discard rate와 latency, device GC를 함께 측정한다. 데이터 삭제 보안 요구와 performance hint를 같은 의미로 사용하지 않는다.

---

## CHAPTER 18 · write cache

controller write cache는 command를 빠르게 완료한 뒤 media write를 나중에 수행할 수 있다. volatile cache라면 command completion이 power-loss durability를 의미하지 않는다.

filesystem이나 DB가 cache semantics를 잘못 이해하면 fsync 후에도 data가 사라질 수 있다. cache enable/disable과 flush command support를 확인해야 한다.

completion, flush, device cache state를 trace한다. benchmark에서 buffered completion throughput과 durable throughput을 분리한다.

---

## CHAPTER 19 · FUA

Force Unit Access는 특정 write가 volatile cache에만 남지 않도록 persistence semantics를 요청하는 mechanism이다. 실제 지원과 implementation은 device protocol contract를 확인해야 한다.

모든 write에 FUA를 사용하면 latency가 커질 수 있고 group commit 기회를 줄일 수 있다. durable transaction boundary에 맞춰 사용해야 한다.

FUA command latency와 media behavior를 fault test로 검증한다. 이름만 보고 power-loss guarantee를 추정하지 않는다.

---

## CHAPTER 20 · ordering

스토리지 command의 submission 순서, controller completion 순서, durable media 반영 순서는 동일하지 않을 수 있다. crash consistency는 필요한 ordering edge를 flush/barrier/FUA로 표현해야 한다.

A data write 뒤 B metadata write를 보냈다는 사실만으로 power loss 후 A가 먼저 남는다는 보장은 없다. queue reordering과 cache가 개입할 수 있다.

transaction protocol에서 각 persistence point를 명시한다. power-cut test로 allowed crash states가 recovery invariant와 일치하는지 확인한다.

---

## CHAPTER 21 · power-loss protection

power-loss protection은 capacitor 같은 energy reserve를 사용해 전원 손실 시 volatile cache와 controller metadata를 safe media state로 내릴 수 있게 한다. device마다 보호 범위가 다르다.

PLP가 있다고 모든 host memory write가 durable한 것은 아니다. command가 controller에 도달하기 전 software queue에 남아 있을 수 있다.

vendor guarantee와 실제 flush path를 확인한다. fault injection에서 sudden power loss 후 committed data set이 protocol과 맞는지 검증한다.

---

## CHAPTER 22 · atomic write granularity

atomic write granularity는 power loss나 crash에서 torn state 없이 한 단위로 기록될 수 있는 범위를 의미한다. database page가 이보다 크면 partial sector/page update를 recovery protocol이 처리해야 한다.

logical block size와 atomic guarantee가 동일하다고 가정하지 않는다. alignment와 command boundary도 영향을 줄 수 있다.

DB/filesystem이 기대하는 atomicity와 device property를 비교한다. torn-write injection으로 recovery가 실제로 가능한지 확인한다.

---

## CHAPTER 23 · zoned storage

zoned storage는 address space를 zone으로 나누고 일부 zone에서 sequential write pointer 규칙을 요구한다. host가 placement를 더 직접 관리해 device 내부 FTL/GC 부담을 줄일 수 있다.

random overwrite가 필요한 application은 zone reset과 log-structured data management를 설계해야 한다. host software complexity가 증가한다.

zone write pointer, open zone count, reset frequency를 모니터링한다. device 특성에 맞지 않는 generic block assumption을 피한다.

---

## CHAPTER 24 · multi-queue scheduler

multi-queue block layer는 CPU별 software submission과 여러 hardware queue를 연결한다. scheduler는 merging, fairness, dispatch ordering을 조절해 device utilization과 latency를 절충한다.

fast NVMe에서 과도한 software scheduling은 overhead가 될 수 있지만 multi-tenant workload에서는 fairness가 필요할 수 있다. scheduler choice는 workload에 따라 달라진다.

queue별 latency와 merge rate를 비교한다. benchmark는 single-job뿐 아니라 contention workload를 포함한다.

---

## CHAPTER 25 · read-ahead

read-ahead는 sequential access를 예측해 요청 전에 다음 block을 읽어 storage latency를 숨긴다. prediction이 맞으면 throughput이 올라가지만 random access에서는 bandwidth와 cache를 낭비한다.

대규모 scan이 다른 hot data를 page cache에서 밀어낼 수 있다. application 자체 prefetch와 kernel read-ahead가 중복될 수도 있다.

hit/useful-prefetch 비율과 cache pressure를 함께 본다. 파일 크기와 access stride에 맞춰 정책을 평가한다.

---

## CHAPTER 26 · small synchronous I/O

작은 synchronous I/O는 command 하나가 끝나야 다음 work가 진행되어 device queue parallelism을 활용하기 어렵다. syscall, interrupt, flush 같은 fixed overhead 비중도 커진다.

latency SLO 때문에 synchronous semantics가 필요한 경우 batching이 제한될 수 있다. thread 수만 늘리면 queue wait가 늘고 tail이 악화될 수 있다.

request size, sync ratio, queue depth를 측정한다. throughput 개선이 durability나 request boundary를 바꾸지 않는지 확인한다.

---

## CHAPTER 27 · large I/O

large I/O는 per-command overhead를 amortize하고 sequential bandwidth를 활용하기 쉽다. 하지만 하나의 request가 queue를 오래 점유하면 small latency-sensitive I/O fairness를 해칠 수 있다.

buffer pinning과 DMA segment 수도 커질 수 있다. device optimal transfer size를 지나치게 넘으면 추가 이득이 없을 수 있다.

size sweep으로 bandwidth와 p99 latency를 비교한다. mixed workload에서 head-of-line blocking이 생기는지 확인한다.

---

## CHAPTER 28 · storage benchmark

storage benchmark는 read/write mix, block size, queue depth, cache state, sync semantics, dataset size, fill level을 명시해야 한다. 이 조건이 다르면 같은 device도 전혀 다른 결과가 나온다.

fresh device의 short run은 FTL steady state를 대표하지 않는다. host page cache가 hit하면 실제 device 성능을 측정하지 않을 수도 있다.

warmup과 steady-state 조건을 문서화한다. average throughput뿐 아니라 latency distribution과 device telemetry를 같이 저장한다.

---

## CHAPTER 29 · device telemetry

NVMe health/log page는 temperature, media error, wear, spare capacity, reset, command error 같은 device 내부 상태를 제공한다. host errno만으로는 controller degradation을 충분히 볼 수 없다.

raw counter는 uptime과 workload를 고려해 rate로 해석해야 한다. firmware update 후 field 의미가 바뀔 수도 있다.

telemetry를 incident timeline과 연결한다. threshold alert뿐 아니라 장기 trend로 pre-failure 상태를 찾는다.

---

## CHAPTER 30 · storage contract

storage contract는 command completion, persistence, atomicity, ordering, retry 가능성을 서로 다른 보장으로 정의해야 한다. filesystem과 database는 device가 실제 제공하는 guarantee 위에서 recovery protocol을 설계한다.

'write 성공'이라는 한 상태로 모든 경계를 합치면 power loss에서 data가 사라지는 이유를 설명할 수 없다. volatile cache와 controller reset도 failure model에 포함한다.

release 검증은 sustained load와 power-cut/fault test를 함께 사용한다. 목표는 최대 IOPS가 아니라 crash를 포함한 모든 중간 상태에서 data invariant를 유지하는 것이다.
