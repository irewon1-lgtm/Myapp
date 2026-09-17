# PART 24 · Storage Media and NVMe — queue, FTL, garbage collection, persistence

Filesystem이 logical block을 관리한다고 해서 storage device가 그 block을 같은 physical 위치에 그대로 저장하는 것은 아니다. NVMe controller는 command queue를 처리하고, flash translation layer는 logical address를 NAND page로 mapping하며, erase-before-write 제약 때문에 garbage collection과 wear leveling을 수행한다. Durability는 `write syscall 성공`에서 끝나지 않고 host cache, controller cache, media program, power-loss protection의 경계를 통과해야 한다.

---

## CHAPTER 01 · Block device abstraction은 media geometry를 숨긴다

Operating system은 storage를 logical block address 범위로 다루지만 underlying media는 sector, NAND page, erase block, zone 같은 다른 physical granularity를 가진다. Device controller가 logical-to-physical translation을 제공하므로 filesystem은 raw NAND erase rule을 직접 다루지 않는 경우가 많다.

이 추상화는 portability를 높이지만 성능과 endurance 비용을 숨긴다. 4KiB logical write 하나가 내부에서는 더 큰 page program·metadata update·garbage collection을 유발할 수 있다. Application-visible I/O size와 media write size를 동일하게 취급하지 않는다.

---

## CHAPTER 02 · NVMe는 host-memory queue를 중심으로 command를 교환한다

NVMe는 submission queue와 completion queue를 host memory에 두고 controller가 DMA로 entry를 읽고 completion을 기록하는 구조를 사용한다. Host는 command를 submission queue에 넣고 doorbell register를 갱신해 새 entry를 알린다. Controller는 command를 처리한 뒤 completion queue에 result를 기록하고 interrupt 또는 polling 경로로 host에 completion을 알린다.

이 구조는 register-by-register legacy protocol보다 높은 parallelism에 적합하다. Performance tuning은 command latency뿐 아니라 queue count, queue depth, interrupt moderation, CPU affinity를 함께 본다.

---

## CHAPTER 03 · Admin queue와 I/O queue는 control plane과 data plane을 분리한다

Controller initialization, feature configuration, namespace management 같은 administrative command는 admin queue를 사용하고 normal read/write는 I/O queue pair를 사용한다. Control path와 data path를 분리하면 frequent I/O가 management command와 같은 lock/queue를 경쟁하지 않게 설계할 수 있다.

문제 분석에서도 admin failure와 I/O queue stall을 분리한다. Device reset, firmware activation, namespace change는 normal data request latency와 다른 state machine을 가진다.

---

## CHAPTER 04 · Queue pair 수가 많다고 항상 throughput이 증가하지 않는다

여러 CPU가 독립 I/O queue를 가지면 shared lock contention을 줄이고 completion locality를 높일 수 있다. 그러나 controller internal channel 수, NAND die parallelism, PCIe bandwidth가 이미 포화됐다면 queue를 더 늘려도 throughput은 증가하지 않고 tail latency만 커질 수 있다.

Queue count는 CPU-side scalability parameter이고 queue depth는 outstanding work 양이다. 둘을 분리해 조정한다.

---

## CHAPTER 05 · Queue depth는 parallelism과 waiting time을 동시에 만든다

Outstanding command가 너무 적으면 controller가 internal parallelism을 충분히 활용하지 못할 수 있다. 반대로 queue depth가 service capacity보다 훨씬 크면 request가 device queue에서 오래 기다려 p99 latency가 증가한다. Throughput benchmark는 깊은 queue에서 높은 숫자를 보여도 interactive workload에는 부적합할 수 있다.

Little’s Law는 storage에도 적용된다. Average outstanding I/O = throughput × average latency 관계를 이용하면 queue가 왜 늘어났는지 추적할 수 있다.

---

## CHAPTER 06 · Completion interrupt와 polling은 CPU cost와 latency를 교환한다

Interrupt-driven completion은 CPU가 다른 일을 하거나 idle할 수 있게 하지만 high-IOPS에서는 interrupt overhead가 커질 수 있다. Polling은 completion을 빠르게 확인할 수 있으나 CPU를 계속 소비한다. Hybrid polling이나 interrupt coalescing은 그 사이의 trade-off를 조정한다.

Storage benchmark에서 CPU utilization을 제외하면 polling mode가 `더 빠르다`는 결론만 남을 수 있다. Throughput, latency, CPU per I/O, energy를 같이 본다.

---

## CHAPTER 07 · PRP와 SGL은 host buffer를 controller에 기술하는 방식이다

NVMe command는 data buffer가 host physical memory 어디에 있는지 controller가 알 수 있어야 한다. Physical Region Page(PRP)와 Scatter-Gather List(SGL)는 non-contiguous memory를 descriptor로 표현하는 mechanism이다. Large I/O가 여러 page에 걸쳐 있으면 descriptor chain이 필요할 수 있다.

IOMMU가 사용되면 device-visible address와 physical memory 사이에 추가 translation이 존재한다. DMA mapping cost와 IOTLB behavior도 high-IOPS workload에 영향을 준다.

---

## CHAPTER 08 · Namespace는 controller 전체와 동일한 단위가 아니다

NVMe subsystem은 하나 이상의 namespace를 expose할 수 있고 각 namespace는 logical block address space와 format을 가진다. Application이 보는 `/dev/nvme0n1` 같은 device가 controller hardware 전체와 1:1이라고 가정하면 management boundary를 잘못 이해할 수 있다.

Namespace resize/format과 controller reset은 scope가 다르다. Operational tooling은 controller, subsystem, namespace identity를 별도로 기록해야 한다.

---

## CHAPTER 09 · NAND flash는 overwrite보다 erase가 더 큰 단위에서 일어난다

Flash는 page 단위 program/read와 더 큰 erase block 단위 erase를 사용한다. 이미 programmed된 page를 임의로 같은 위치에 덮어쓰는 것이 DRAM store처럼 단순하지 않다. Controller는 update를 새 physical page에 기록하고 old version을 invalid로 표시한 뒤 나중에 block 단위 garbage collection을 수행할 수 있다.

이 비대칭이 FTL과 write amplification의 근본 원인이다.

---

## CHAPTER 10 · FTL은 logical block address를 physical flash location으로 변환한다

Flash Translation Layer는 host가 보내는 LBA를 physical page에 mapping한다. Mapping granularity가 fine하면 metadata가 커지고 coarse하면 small random update가 복잡해질 수 있다. Controller는 SRAM/DRAM cache와 flash-resident mapping metadata를 조합할 수 있다.

Power loss 시 mapping state를 복구할 수 있어야 하므로 FTL metadata durability도 data durability의 일부다. Host는 이 내부 protocol을 직접 보지 못한다.

---

## CHAPTER 11 · Out-of-place update가 stale page를 만든다

Logical block이 수정될 때 controller가 새 physical page에 data를 쓰고 mapping을 갱신하면 이전 physical page는 stale/invalid가 된다. 시간이 지나면 erase block 안에 valid와 invalid page가 섞인다. Free page가 부족해지면 garbage collector가 valid page를 다른 block으로 이동시키고 block을 erase해야 한다.

Application write 외에 GC copy가 추가 physical write를 만들면서 write amplification이 생긴다.

---

## CHAPTER 12 · Garbage collection은 foreground latency를 폭발시킬 수 있다

Free block이 충분할 때 write latency는 낮다가 device가 가득 차고 free space가 줄면 GC가 foreground write와 경쟁해 latency가 급격히 증가할 수 있다. 평균 throughput은 비슷해도 p99/p999가 크게 나빠지는 write cliff가 발생한다.

Sustained benchmark는 preconditioning 없이 empty drive에서만 측정하면 production steady-state를 과대평가한다. Device fill level과 prior write history를 통제해야 한다.

---

## CHAPTER 13 · Over-provisioning은 GC와 wear leveling에 숨은 여유 공간을 제공한다

Host에 expose하지 않은 physical capacity는 controller가 free block pool과 replacement resource로 사용할 수 있다. Over-provisioning이 많으면 GC가 선택할 여지가 커지고 write amplification이 줄 수 있지만 usable capacity는 감소한다.

Capacity를 끝까지 채우는 policy는 cost 효율만 보는 것이 아니다. Performance consistency와 endurance를 같이 고려해야 한다.

---

## CHAPTER 14 · Wear leveling은 erase cycle을 physical block에 분산한다

특정 logical region이 자주 업데이트되면 동일 physical block만 반복 erase되지 않도록 FTL이 data placement를 바꾼다. Dynamic wear leveling은 active write를 분산하고 static wear leveling은 오래 변하지 않은 cold data도 이동시켜 전체 erase count를 균등화할 수 있다.

Wear leveling 자체도 data movement를 만들므로 endurance와 write amplification 사이 policy가 필요하다. SMART-like health metric은 average/max erase count나 remaining life estimate를 제공할 수 있다.

---

## CHAPTER 15 · Read disturb와 retention은 write endurance와 다른 failure mode다

Flash cell은 시간이 지나며 stored charge margin이 줄거나 반복 read가 주변 cell에 영향을 줄 수 있다. Controller는 ECC margin, read-retry, refresh를 사용해 data를 유지할 수 있다. Device가 거의 write되지 않는다고 reliability issue가 없는 것은 아니다.

Cold archive workload는 retention과 periodic refresh/scrub policy를 고려해야 한다.

---

## CHAPTER 16 · Internal ECC는 host checksum을 대체하지 않는다

SSD controller의 media ECC는 NAND raw error를 수정하지만 firmware bug, DMA/path error, misdirected write는 ECC 보호 domain 밖일 수 있다. Host filesystem/database checksum은 다른 계층의 corruption을 탐지한다. PART 22의 end-to-end integrity 원칙이 storage device 내부에도 적용된다.

각 ECC/checksum이 어떤 boundary에서 생성되고 검증되는지 문서화해야 protection gap을 찾을 수 있다.

---

## CHAPTER 17 · TRIM/Discard는 logical free 정보를 device에 전달한다

Filesystem이 block을 free해도 device는 host가 그 LBA data를 더 이상 필요로 하지 않는지 알지 못할 수 있다. Discard/TRIM은 unused logical range 정보를 controller에 전달해 GC와 over-provisioning 효율을 높일 수 있다.

Discard timing은 latency에 영향을 줄 수 있고 encryption/integrity layer와 semantics가 결합될 수 있다. Continuous discard와 batched discard는 workload에 따라 trade-off가 다르다.

---

## CHAPTER 18 · Write cache는 completion과 persistence를 분리한다

Controller가 volatile cache에 data를 받아 놓고 command completion을 반환하면 host는 write가 끝났다고 보지만 power loss 후 media에 남지 않을 수 있다. Device가 volatile write cache를 사용할 때 flush/FUA semantics가 durability protocol의 핵심이다.

Application의 fsync가 실제 durable guarantee가 되려면 filesystem, block layer, controller가 flush ordering을 올바르게 전달해야 한다. 한 계층이 flush를 무시하면 전체 guarantee가 깨진다.

---

## CHAPTER 19 · FUA는 특정 write가 stable storage까지 도달해야 함을 표현한다

Force Unit Access는 write를 cache에만 머물게 하지 않고 stable storage 요구와 연결할 수 있다. Device/stack support에 따라 FUA를 직접 사용하거나 flush sequence와 조합한다. Linux write-cache layer도 FUA와 flush 선택에 따라 performance가 달라질 수 있다.

Durability benchmark는 throughput만 보지 않고 power-failure test로 실제 persistence contract를 검증해야 한다.

---

## CHAPTER 20 · Ordering과 durability는 별개의 조건이다

Write A가 media에 남고 Write B가 media에 남았다는 사실만으로 A가 B보다 먼저 persistent해졌다는 보장은 없다. Filesystem/database recovery는 write ordering을 요구할 수 있다. Barrier/flush/FUA protocol은 특정 dependency를 storage stack에 전달한다.

`두 write 모두 성공`과 `crash 후 허용된 순서로 남음`을 구분해야 한다.

---

## CHAPTER 21 · Power-loss protection은 volatile state를 media에 commit할 시간을 산다

Enterprise SSD는 capacitor 등 energy reserve를 사용해 sudden power loss 시 controller RAM/metadata를 NAND에 flush하는 protection을 제공할 수 있다. 그러나 모든 consumer device가 동일 guarantee를 제공하는 것은 아니다.

Device datasheet의 power-loss protection scope가 user data와 mapping metadata를 어디까지 포함하는지 확인해야 한다. PLP가 있다고 filesystem ordering bug를 해결해 주는 것도 아니다.

---

## CHAPTER 22 · Atomic write granularity는 application transaction과 동일하지 않다

Device가 특정 block size의 atomic write를 지원해도 application record가 더 크거나 여러 LBA에 걸치면 partial update가 가능하다. Database/page format은 device atomicity와 자체 checksum/WAL을 조합해 crash recovery를 설계한다.

Storage atomicity를 과대평가하면 torn write에 취약해진다. 실제 guarantee granularity를 문서에서 확인한다.

---

## CHAPTER 23 · Zoned storage는 host와 device의 write-placement contract를 바꾼다

Zoned Namespace 같은 model은 zone에 sequential write constraint를 적용해 FTL mapping/GC overhead를 줄이는 방향을 제공한다. 대신 host software가 write pointer와 zone lifecycle을 관리해야 한다.

General block device와 동일한 random-write assumption을 유지할 수 없으므로 filesystem/database가 zoned semantics를 인식해야 한다. Storage abstraction이 성능 최적화를 위해 다시 얇아지는 사례다.

---

## CHAPTER 24 · I/O scheduler와 NVMe multi-queue의 역할을 구분한다

Fast NVMe에서는 device 자체 parallel queue가 많아 legacy rotating-disk seek optimization과 다른 scheduling strategy가 필요하다. Block layer는 per-CPU software queue와 hardware dispatch queue를 사용해 scalability를 높일 수 있다. Scheduler는 fairness/latency control을 제공할 수 있지만 device queue depth와 중복 control이 될 수도 있다.

Workload마다 scheduler choice와 queue setting을 benchmark해야 한다.

---

## CHAPTER 25 · Read-ahead는 sequential prediction이고 random workload에는 waste가 될 수 있다

OS/file layer가 다음 block을 미리 읽으면 sequential scan latency를 숨길 수 있지만 random access에서는 unused data로 bandwidth와 cache를 소비한다. Storage device 자체에도 prefetch/read cache가 있을 수 있어 여러 계층이 같은 예측을 중복할 수 있다.

Read-ahead 효율은 hit ratio와 extra I/O를 함께 측정한다. `더 많이 미리 읽기`는 universal optimization이 아니다.

---

## CHAPTER 26 · Small synchronous I/O는 queue parallelism을 사용하지 못할 수 있다

Thread가 4KiB write를 하나 제출하고 completion까지 blocking한 뒤 다음 write를 보내면 queue depth는 1에 머문다. Device가 수십 command를 병렬 처리할 수 있어도 application submission pattern이 이를 사용하지 못한다.

Async I/O와 batching은 outstanding depth를 늘릴 수 있지만 latency/ordering complexity가 증가한다. Application concurrency와 device concurrency를 구분해야 한다.

---

## CHAPTER 27 · Large I/O는 command overhead를 줄이지만 tail과 fairness를 바꾼다

여러 small request를 merge해 large I/O로 보내면 per-command overhead가 줄고 sequential throughput이 좋아질 수 있다. 반면 한 large request가 device service time을 오래 차지하면 다른 latency-sensitive I/O가 기다릴 수 있다.

Block size tuning은 throughput, queue fairness, memory copy, filesystem page size를 함께 본다.

---

## CHAPTER 28 · Storage benchmark는 cache state와 preconditioning을 통제해야 한다

Page cache가 warm한 read는 device를 거의 건드리지 않을 수 있고 controller cache가 warm한 결과도 cold media latency를 대표하지 않는다. SSD write benchmark는 fresh empty drive와 steady-state full drive가 다르다.

Benchmark protocol에 filesystem cache drop 여부, direct I/O, device fill ratio, preconditioning, queue depth, block size, read/write mix를 명시한다. 숫자만 비교하면 재현되지 않는다.

---

## CHAPTER 29 · Device telemetry는 latency와 endurance를 연결한다

NVMe health/error log와 controller telemetry는 temperature, media error, available spare, endurance usage, error status 같은 signal을 제공할 수 있다. Application p99 latency가 나빠질 때 device thermal throttling, background GC, media error retry가 원인일 수 있다.

Host I/O trace와 device health timeline을 correlation하면 storage stack 어느 층에서 시간이 늘었는지 좁힐 수 있다.

---

## CHAPTER 30 · Storage correctness는 submission→completion→persistence→recoverability를 분리한다

수석 개발자가 storage write를 설명할 때 네 단계를 섞지 않는다.

1. **Submission** — host가 request를 queue에 올렸는가.
2. **Completion** — device/controller가 command 완료를 보고했는가.
3. **Persistence** — power loss 뒤에도 required bytes와 ordering이 남는가.
4. **Recoverability** — crash 후 filesystem/database가 invariant를 복원할 수 있는가.

NVMe queue, controller cache, FTL, NAND media, filesystem journal, database WAL은 각 단계의 다른 부분을 담당한다. `write 성공`이라는 단어 하나로 이 모든 보장을 묶지 않는 것이 storage engineering의 출발점이다.
