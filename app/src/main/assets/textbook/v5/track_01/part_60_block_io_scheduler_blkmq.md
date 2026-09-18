# PART 60 · Block I/O Internals — bio, blk-mq, merging, scheduling, tags, completion

파일 시스템 아래의 block layer는 logical I/O를 device가 처리할 request로 바꾸고, 여러 CPU의 submission을 hardware queue로 연결하며, merge·scheduler·tag·timeout·completion을 관리한다. NVMe처럼 queue가 많은 장치에서는 단일 전역 queue 모델보다 blk-mq의 CPU/queue topology가 성능을 좌우한다. 이 PART는 **bio 생성에서 hardware completion이 원래 caller로 돌아올 때까지의 request lifecycle**을 추적한다.

---

## CHAPTER 01 · bio는 block 범위와 memory segment를 묶어 block layer에 전달하는 I/O description이다

Filesystem과 memory subsystem은 page나 buffer를 특정 block range에 읽고 쓰기 위해 bio 형태의 작업을 만든다. 하나의 bio는 연속 block 범위와 여러 memory vector를 포함할 수 있어 scatter/gather DMA로 이어질 수 있다. Bio는 file이나 inode 의미를 대부분 잃은 채 block device address와 operation semantics를 전달하는 경계다.

상위 계층의 큰 write가 여러 bio로 나뉠 수 있고, 반대로 인접 bio가 아래에서 merge될 수 있다. 따라서 application I/O count와 block request count는 일치하지 않는다.

Tracing에서는 bio submit 시점과 completion을 request id/sector로 연결한다. File-level latency만으로 block layer에서 split/merge가 어떻게 일어났는지 알기 어렵다.

## CHAPTER 02 · request는 하나 이상의 bio를 device queue가 처리하기 좋은 단위로 묶는다

Block layer는 compatible하고 인접한 bio를 request로 결합할 수 있다. Request에는 operation, sector range, flags, deadline/scheduler metadata가 붙고 driver가 device command로 변환한다. Device가 지원하는 maximum sectors, segment 수, alignment 때문에 큰 bio가 다시 split될 수도 있다.

Request 생성은 단순 포장 단계가 아니다. Merge가 잘 되면 command 수가 줄지만 너무 오래 모으면 latency가 늘 수 있다. Random workload에서는 merge 기회가 적다.

Metrics에서 bios/request, average request size, sectors/request를 보면 upper workload와 device command granularity의 차이를 이해할 수 있다.

## CHAPTER 03 · blk-mq는 여러 CPU의 submission을 software·hardware queue로 분리해 global lock 병목을 줄인다

Multi-queue block layer는 CPU별 또는 CPU group별 software submission context와 device hardware dispatch queue를 연결한다. NVMe처럼 여러 hardware queue를 제공하는 장치에서 parallel submission이 단일 lock에 몰리지 않게 한다. CPU→hardware queue mapping이 locality와 fairness에 영향을 준다.

Queue 수가 많다고 무조건 성능이 높아지는 것은 아니다. Device 내부 controller resource, interrupt vector, NUMA topology가 실제 parallelism을 제한한다.

Scaling benchmark는 thread 수와 queue 수를 함께 바꿔 saturation point를 찾는다. `num_queues = num_cpus` 같은 단순 규칙을 모든 장치에 적용하지 않는다.

## CHAPTER 04 · software queue는 submitting CPU의 request를 모아 contention과 cache bouncing을 줄인다

각 CPU가 자기 submission context에서 request를 준비하면 shared global queue lock을 덜 경쟁할 수 있다. 이후 mapping된 hardware context로 dispatch된다. CPU migration이나 cpuset 변화가 이 mapping의 locality를 바꿀 수 있다.

Per-CPU batching은 throughput에 유리하지만 특정 CPU workload가 hot하면 해당 software queue에 backlog가 몰릴 수 있다. Scheduler가 task를 옮겨도 이미 enqueue된 I/O의 경로는 남을 수 있다.

CPU별 submit rate와 hardware queue별 depth를 같이 본다. Device 전체 queue depth만으로 불균형을 찾기 어렵다.

## CHAPTER 05 · hardware queue는 실제 device command queue와 tag namespace에 가까운 dispatch 단위다

Driver는 block request를 NVMe submission queue 같은 device-specific queue에 올린다. 각 hardware queue는 제한된 outstanding command/tag를 가지며 completion queue/interrupt와 연결될 수 있다. Queue affinity가 CPU와 device NUMA locality를 결정한다.

하나의 queue가 가득 찬 동안 다른 queue가 여유일 수 있으므로 mapping policy가 중요하다. 반대로 너무 많은 queue는 controller cache와 interrupt overhead를 늘린다.

Queue별 outstanding, completion rate, latency를 수집한다. 전체 평균 queue depth가 정상이어도 한 hot queue의 tail은 매우 나쁠 수 있다.

## CHAPTER 06 · direct issue는 scheduler 대기 없이 request를 곧바로 driver에 넘겨 latency를 줄일 수 있다

조건이 맞고 queue에 여유가 있으면 block layer는 request를 별도 dispatch list에 오래 두지 않고 바로 hardware queue로 issue할 수 있다. Small latency-sensitive I/O에는 유리하지만 batching·merge·fairness 기회가 줄어들 수 있다.

장치가 이미 saturated라면 direct issue가 의미 있는 이득을 주지 못하고 tag contention만 빨리 만날 수 있다. Scheduler 정책과 workload 혼합에 따라 선택이 달라진다.

Trace에서 submit→dispatch gap을 측정해 실제 queueing 위치를 확인한다. API가 direct I/O라는 이름이라고 block layer direct issue가 자동 보장되는 것은 아니다.

## CHAPTER 07 · queue depth는 outstanding I/O로 device parallelism을 채우지만 latency와 resource를 증가시킨다

SSD/NVMe는 여러 command를 동시에 받아 내부 channel과 flash die를 병렬 활용하므로 일정 queue depth까지 throughput이 증가한다. 그러나 saturation 이후 depth를 더 늘리면 throughput은 거의 그대로인데 대기 시간만 길어진다.

Little’s Law 관점에서 높은 throughput과 latency는 queue 내 work 수와 연결된다. Peak MB/s를 얻기 위해 지나치게 큰 depth를 쓰면 interactive workload p99가 악화될 수 있다.

Benchmark는 QD1, QD4, QD32처럼 curve를 그린다. Production operating point는 throughput 최대점보다 SLO를 만족하는 지점일 수 있다.

## CHAPTER 08 · tag는 outstanding request와 device command slot을 연결하는 제한된 identifier다

Blk-mq와 driver는 동시에 진행 중인 request에 tag를 할당해 completion이 돌아왔을 때 원래 request를 찾는다. Tag 수는 hardware queue depth와 비슷한 scarce resource다. Tag가 없으면 새 I/O는 기다리거나 requeue되어야 한다.

Reserved tag가 recovery/flush 같은 critical command에 사용될 수 있다. 일반 traffic이 모든 slot을 소비해 deadlock을 만드는 것을 막는 장치다.

Tag utilization과 wait time을 관측하면 storage가 bandwidth보다 command-slot에서 먼저 막히는지 알 수 있다.

## CHAPTER 09 · tag exhaustion은 submit thread를 device service time에 직접 묶는 backpressure다

모든 tag가 사용 중이면 새 request는 즉시 hardware에 들어갈 수 없다. Software queue에서 기다리거나 caller가 resource availability를 기다린다. Device latency가 올라가면 tag가 더 오래 점유되어 추가 queueing이 생기는 positive feedback이 발생한다.

Application에서 I/O concurrency를 무한히 늘리면 tag wait가 커질 뿐 throughput은 증가하지 않을 수 있다. Upper layer semaphore나 admission으로 queue depth를 제한하는 이유다.

Incident에서는 device utilization뿐 아니라 tag wait와 outstanding histogram을 본다. `disk 100%` 한 숫자보다 병목 위치가 명확하다.

## CHAPTER 10 · plugging은 짧은 기간 여러 bio를 모아 merge와 batch dispatch 기회를 만든다

한 code path가 연속 I/O를 제출할 때 각 bio를 즉시 device에 보내기보다 잠깐 local plug list에 모으면 인접 request를 merge하고 lock/doorbell overhead를 줄일 수 있다. Scope가 끝나거나 조건이 되면 한꺼번에 flush한다.

너무 오래 plug하면 first I/O latency가 늘어나므로 bounded scope가 중요하다. Filesystem이 알고 있는 locality를 block layer가 활용하는 batching mechanism으로 볼 수 있다.

Trace에서 application submit timestamp와 actual dispatch 사이에 짧은 burst pattern이 보일 수 있다. 이를 device stall로 오해하지 않는다.

## CHAPTER 11 · request merge는 인접 block I/O를 합쳐 command 수를 줄인다

앞뒤 sector가 연속되고 operation/flag가 compatible하면 bio/request를 하나로 합칠 수 있다. Sequential write에서 command overhead와 device mapping 비용을 줄인다. 하지만 maximum sectors/segments와 boundary 제한을 넘을 수 없다.

Random I/O에서는 merge율이 낮다. Application이 이미 큰 I/O를 제출한다면 추가 이득도 작다.

`merges/sec`가 높은 것이 항상 좋은 것은 아니다. Merge를 기다리는 queue delay와 command 감소 이득을 함께 측정한다.

## CHAPTER 12 · merge 때문에 상위 I/O count와 device request count가 달라져 관측이 왜곡될 수 있다

Application이 100개의 4 KiB write를 했더라도 block layer가 10개의 40 KiB request로 merge하면 device command count는 10이다. 반대로 device limit 때문에 하나의 큰 application I/O가 여러 command로 split될 수도 있다.

따라서 iops를 비교할 때 어느 계층 count인지 명시한다. Filesystem trace와 device telemetry의 숫자가 다르다고 lost I/O라고 결론내리지 않는다.

Bytes와 latency를 함께 사용하고, correlation이 필요하면 sector range와 request id를 추적한다. Count 하나는 data path 변환을 숨긴다.

## CHAPTER 13 · I/O scheduler는 request 순서와 fairness를 조절해 workload 목표를 반영한다

Block scheduler는 deadline, process/cgroup identity, sector locality 등을 이용해 어떤 request를 먼저 dispatch할지 결정한다. Rotational disk에서는 seek 최소화가 중요했고 SSD에서도 latency isolation과 fairness가 의미가 있다.

Database처럼 자체 queueing과 NVMe parallelism을 적극 사용하는 workload는 scheduler overhead가 불필요할 수 있고, desktop mixed workload는 latency-oriented scheduler가 유리할 수 있다.

Scheduler 선택은 device type 이름보다 실제 workload의 read/write mix와 SLO로 검증한다. Queue depth와 cgroup policy도 함께 본다.

## CHAPTER 14 · `none` scheduler는 별도 재정렬을 최소화하지만 block layer의 모든 queueing이 사라지는 것은 아니다

`none`을 선택해도 blk-mq software queue, hardware queue, tags, device 내부 queue는 여전히 존재한다. 즉 “scheduler 없음 = zero queue latency”가 아니다. 추가 scheduler policy를 최소화한다는 의미에 가깝다.

Fast NVMe와 application-controlled I/O에서 좋은 baseline이 될 수 있지만 multi-tenant fairness는 약해질 수 있다.

Scheduler 변경 전후에 dispatch latency와 tenant별 p99를 비교한다. 평균 throughput만 좋아지고 한 workload가 starvation되는지 확인한다.

## CHAPTER 15 · storage fairness는 bytes가 아니라 latency·IOPS·request size 차이까지 고려해야 한다

한 tenant가 큰 sequential write를 지속하면 작은 random read가 queue 뒤에서 오래 기다릴 수 있다. 동일 bytes share가 동일 latency share를 의미하지 않는다. Scheduler와 cgroup controller가 workload별 weight/rate를 적용할 수 있다.

High-priority read를 무조건 앞세우면 background writeback이 굶어 dirty memory가 쌓이는 다른 문제가 생길 수 있다. 전체 system progress를 유지해야 한다.

Tenant별 queueing delay와 device service time을 분리한다. Fairness 정책이 device 자체 latency를 바꾸는지 확인한다.

## CHAPTER 16 · completion order는 submission order와 같지 않을 수 있다

여러 hardware queue와 device 내부 parallelism 때문에 나중에 제출한 request가 먼저 완료될 수 있다. 서로 독립인 read/write는 이 out-of-order completion을 허용해 성능을 얻는다. Application이 callback 순서를 submit 순서로 가정하면 race가 생긴다.

Durability dependency가 있는 write는 flush/FUA와 filesystem ordering protocol로 명시해야 한다. “먼저 submit했다”는 사실만으로 crash ordering을 보장하지 않는다.

Async I/O test는 completion을 의도적으로 reorder해 state machine이 안전한지 확인한다.

## CHAPTER 17 · completion path는 device interrupt/polling에서 request·bio callback까지 ownership을 되돌린다

NIC와 마찬가지로 storage도 device completion queue를 처리해 tag로 request를 찾고 status를 해석한 뒤 상위 bio completion을 호출한다. Buffer와 request memory는 이 시점 전까지 재사용하면 안 된다.

Completion이 어떤 CPU에서 실행되는지에 따라 cache locality와 softirq/interrupt load가 달라진다. High IOPS에서는 completion CPU가 병목이 될 수 있다.

Trace는 submit/dispatch/device-complete/endio 네 시점을 구분해 queue time과 service time을 계산한다.

## CHAPTER 18 · I/O timeout은 device가 실패했다고 단정하는 순간이 아니라 progress가 기대보다 늦다는 감지다

Request가 설정된 시간 안에 completion되지 않으면 block layer/driver가 timeout handler를 호출할 수 있다. 원인은 device hang, lost interrupt, controller reset, fabric 지연 등 다양하다. Timeout 처리 중 request가 늦게 완료되는 race도 고려해야 한다.

무조건 재시도하면 이미 완료된 write를 중복하거나 failure를 장시간 숨길 수 있다. Driver와 protocol의 command idempotency를 이해해야 한다.

Timeout event, controller state, reset log를 함께 보존한다. Application timeout만으로 storage hardware failure를 단정하지 않는다.

## CHAPTER 19 · retry는 transient failure를 흡수하지만 latency budget과 duplicate semantics를 소비한다

일부 block error는 path failover나 controller reset 후 재시도할 수 있다. 그러나 retry마다 request가 tag와 queue를 다시 소비하고 상위 deadline을 잠식한다. Persistent media error를 무한 retry하면 system 전체 I/O가 막힐 수 있다.

Read retry와 write retry는 side effect semantics가 다를 수 있다. Device/protocol이 command completion ambiguity를 어떻게 정의하는지 따른다.

Retry count와 cumulative delay를 metric으로 노출한다. Success rate만 보면 심한 recovery 지연을 정상으로 오해할 수 있다.

## CHAPTER 20 · read/write mix는 device 내부 scheduling과 cache behavior를 바꾼다

100% sequential read benchmark와 production의 random write 혼합은 전혀 다른 device latency를 만든다. Write는 flash translation layer와 garbage collection을 유발할 수 있고, read latency가 함께 악화될 수 있다.

Block scheduler도 mixed workload에서 read priority와 write batching을 다르게 적용한다. Queue depth 하나로 workload를 표현하기 부족하다.

Benchmark에는 read%, block size, randomness, fsync frequency를 명시한다. Vendor 최고 throughput 숫자를 application capacity로 직접 사용하지 않는다.

## CHAPTER 21 · flush와 FUA는 volatile write cache를 통과한 durability ordering을 표현한다

Filesystem이 commit을 보장하려면 앞선 write가 stable media에 도달했는지 제어해야 한다. Flush는 device cache의 앞선 write를 내리고, FUA는 특정 command가 stable media에 기록되는 semantics를 제공할 수 있다. 실제 지원과 implementation은 device/protocol에 따라 다르다.

Flush가 비싸면 group commit으로 여러 transaction이 비용을 공유한다. 하지만 flush를 생략해 benchmark가 빨라졌다면 durability가 같은지 확인해야 한다.

Power-loss test로 hardware가 advertised semantics를 실제 지키는지 검증한다. Completion latency만으로 durability를 추론하지 않는다.

## CHAPTER 22 · discard/TRIM은 더 이상 사용하지 않는 logical block을 device에 알려 내부 관리를 돕는다

Filesystem이 block을 free해도 SSD는 그 사실을 알지 못하면 모든 physical mapping을 유효하게 취급할 수 있다. Discard는 해당 logical range가 불필요함을 알려 garbage collection과 write amplification을 줄일 수 있다.

실시간 discard가 latency를 만들 수 있어 batch/fstrim 방식과 trade-off가 있다. Thin provisioning에서는 space reclamation semantics도 연결된다.

Discard 후 data confidentiality를 자동 보장한다고 가정하지 않는다. Secure erase는 다른 계약이다.

## CHAPTER 23 · zoned storage는 임의 위치 overwrite 대신 zone write pointer 규칙을 block interface에 노출한다

Zoned device는 일부/전체 영역에서 순차 write를 요구하고 zone reset 후 다시 사용할 수 있다. Traditional filesystem의 random overwrite assumption과 충돌하므로 zone-aware layout이나 translation layer가 필요하다.

Zone open/active limit도 scarce resource가 될 수 있다. 여러 writer가 무작위 zone을 열면 command가 실패할 수 있다.

Recovery는 zone write pointer와 application metadata를 reconcile해야 한다. 단순 sector address interface처럼 다루지 않는다.

## CHAPTER 24 · blk-cgroup은 block I/O usage와 scheduling을 cgroup hierarchy에 연결한다

Container/tenant별 storage contention을 제어하기 위해 block I/O를 cgroup에 charge하고 weight/rate policy를 적용할 수 있다. Buffered write는 submit 시점과 실제 writeback 시점이 다르므로 attribution semantics를 이해해야 한다.

하나의 shared filesystem에서 여러 cgroup이 같은 inode/page를 사용할 때 accounting이 단순하지 않을 수 있다. Kernel version과 controller behavior를 확인한다.

Per-cgroup bytes, delay, throttle time을 수집해 noisy neighbor를 찾는다. Device 전체 latency 하나만으로 tenant 영향도를 알 수 없다.

## CHAPTER 25 · I/O throttling은 bandwidth/IOPS를 제한하지만 upper queue가 무한히 쌓이지 않게 해야 한다

Cgroup rate limit에 걸린 request는 dispatch가 늦어지므로 application이 계속 생성하면 dirty page나 userspace queue가 커질 수 있다. Throttle은 downstream 보호 장치이지 upstream admission을 자동 제공하지 않는다.

Rate limit을 너무 낮추면 memory writeback과 sync path가 장시간 stall해 system-wide pressure로 번질 수 있다.

Throttle delay와 queue depth를 관찰하고 producer concurrency를 맞춘다. 제한된 tenant가 host memory를 backlog로 소비하지 않게 한다.

## CHAPTER 26 · NUMA queue mapping은 submission·DMA buffer·interrupt CPU를 device 근처에 맞추는 문제다

Multi-queue NVMe가 특정 NUMA node에 연결돼 있으면 가까운 CPU에서 submission하고 memory buffer도 같은 node에 두는 것이 interconnect traffic을 줄일 수 있다. Blk-mq queue mapping과 IRQ affinity가 이를 지원한다.

Thread가 다른 node로 migration하면 remote submission/completion이 늘 수 있다. 반대로 한 node에 workload가 과도하면 locality보다 load balance가 중요해진다.

Device NUMA node, request submit CPU, completion CPU를 trace해 실제 path를 확인한다. Hardware topology만 보고 자동으로 최적이라고 가정하지 않는다.

## CHAPTER 27 · completion CPU는 high-IOPS workload에서 별도의 processing bottleneck이 될 수 있다

Device가 빠를수록 command completion 처리 자체의 interrupt, queue polling, callback 비용이 커진다. 특정 CPU에 completion이 몰리면 device bandwidth에 여유가 있어도 그 CPU가 packet/network softirq처럼 포화될 수 있다.

Interrupt affinity와 polling mode를 조정해 분산할 수 있지만 application worker와 cache locality trade-off가 있다.

CPU별 IOPS completion count와 utilization을 본다. User CPU가 낮다는 이유로 storage path CPU bottleneck을 배제하지 않는다.

## CHAPTER 28 · block trace는 submit·merge·issue·complete event를 이어 queueing 위치를 보여준다

Application latency 하나로는 filesystem, block scheduler, device 중 어디서 시간이 쓰였는지 알 수 없다. Block tracing은 bio/request가 언제 queue에 들어가고 merge되고 driver에 issue되고 completion됐는지 timestamp를 제공한다.

Instrumentation이 고 IOPS에서 많은 event를 생성하므로 sampling 또는 짧은 capture를 사용한다. Sector/address 같은 정보가 민감한 workload metadata를 드러낼 수 있는지도 고려한다.

Trace를 filesystem/fsync event와 같은 clock domain으로 연결하면 end-to-end storage timeline을 재구성할 수 있다.

## CHAPTER 29 · block benchmark는 device cache와 filesystem을 분리하고 queue-depth curve를 측정해야 한다

Raw device `fio` 결과와 application filesystem 성능은 metadata, page cache, journaling 때문에 다르다. Benchmark 목적에 따라 raw block, direct file, buffered file을 구분한다. Data destructive test는 production device에 실행하지 않는다.

Block size, read/write mix, randomness, queue depth, job count, sync/flush policy를 모두 기록한다. 짧은 SSD benchmark는 SLC cache와 thermal state만 측정할 수 있어 sustained run도 필요하다.

평균 MB/s뿐 아니라 p99 completion latency와 error를 본다. 가장 높은 QD 숫자가 실제 서비스 operating point가 아닐 수 있다.

## CHAPTER 30 · request lifecycle contract는 submit에서 completion까지 queue와 ownership을 설명할 수 있어야 한다

Block I/O는 bio가 request로 묶이고 software queue와 scheduler를 지나 hardware queue/tag를 얻고 device에서 실행된 뒤 completion callback으로 돌아온다. Merge와 split 때문에 count가 변하고, multi-queue 때문에 순서와 CPU도 변한다.

성능 분석은 submit→dispatch queue time, device service time, completion processing을 분리한다. Correctness는 buffer lifetime, out-of-order completion, flush/FUA durability를 함께 본다.

최종 검증은 실제 trace와 workload benchmark로 이 lifecycle이 예상대로 동작하는지 확인하는 것이다. Device가 높은 MB/s를 냈다는 사실만으로 block layer의 latency·fairness·durability를 PASS라고 부를 수 없다.
