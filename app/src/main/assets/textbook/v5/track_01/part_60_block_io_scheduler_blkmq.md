# PART 60 · Block I/O Internals — bio, blk-mq, merging, scheduling, tags, completion

Storage request는 filesystem에서 device command로 바로 내려가지 않는다. Linux block layer는 **bio aggregation, request allocation, software staging queue, hardware dispatch queue, merging, I/O scheduling, tag allocation, timeout/completion**을 통해 filesystem과 driver 사이의 concurrency를 조절한다. NVMe queue가 빠르더라도 block layer ownership과 queueing이 병목이면 application은 device 성능을 얻지 못한다.

## CHAPTER 01 · bio는 block I/O의 logical extent와 memory vector를 표현한다

Filesystem과 upper block layer는 block address range와 memory pages/fragments를 `bio` 형태로 전달할 수 있다. 하나의 bio가 반드시 하나의 device command가 되는 것은 아니다. Block layer는 인접 bio를 merge하거나 여러 bio를 하나의 request로 묶을 수 있으므로 application I/O count와 device command count는 다를 수 있다.

## CHAPTER 02 · Request는 driver에 전달할 scheduling unit이다

`struct request`는 하나 이상의 bio를 포함하며 queue position, tag, timeout, operation type 같은 block-layer state를 가진다. bio가 data mapping 중심이라면 request는 **dispatch/completion lifecycle**의 중심이다. Tracing에서 bio와 request event를 섞으면 merge 때문에 latency attribution이 틀릴 수 있다.

## CHAPTER 03 · blk-mq는 single global queue bottleneck을 여러 queue domain으로 분해한다

고속 SSD/NVM과 multicore 환경에서는 모든 CPU가 하나의 locked request queue를 공유하면 cache-line contention이 device보다 먼저 병목이 된다. blk-mq는 software queue와 hardware context를 여러 개 두어 submission path를 분산한다. 목표는 `queue를 많이 만든다`가 아니라 **submission ownership을 CPU locality에 맞게 분할**하는 것이다.

## CHAPTER 04 · Software staging queue는 merge와 scheduler가 개입하는 공간이다

Request가 driver로 즉시 내려갈 수 있으면 direct issue path를 탈 수 있지만 merge 가능성을 확인하거나 I/O scheduler가 활성화되어 있으면 software staging queue에서 대기한다. 따라서 block latency는 device service time 외에 scheduler/merge delay를 포함할 수 있다.

## CHAPTER 05 · Hardware dispatch queue는 device queue와 대응되는 driver-facing context다

Hardware context는 driver submission queue 또는 DMA ring과 연결되는 마지막 block-layer dispatch domain이다. 여러 software queue가 하나의 hardware context로 mapping될 수 있다. CPU 수와 device hardware queue 수가 다르면 mapping policy가 locality와 contention을 결정한다.

## CHAPTER 06 · Direct issue는 가장 짧은 경로지만 항상 가능한 것은 아니다

Scheduler와 merge가 필요 없고 hardware resource가 있으면 request를 software queue에서 오래 보관하지 않고 driver로 직접 보낼 수 있다. 그러나 device tag가 부족하거나 queue가 busy하면 request는 dispatch list로 되돌아갈 수 있다. `direct path 지원`과 `항상 direct`는 다르다.

## CHAPTER 07 · Queue depth는 device parallelism과 latency amplification을 동시에 바꾼다

Outstanding request가 너무 적으면 device internal parallelism을 활용하지 못하고, 너무 많으면 queueing delay와 tail latency가 커진다. Optimal depth는 media latency, controller parallelism, read/write mix, SLO에 따라 달라진다. Queue depth를 최대값으로 설정하는 것은 throughput benchmark에는 유리해도 interactive latency에는 불리할 수 있다.

## CHAPTER 08 · Tag는 outstanding request identity와 resource reservation이다

blk-mq는 request에 tag를 할당해 driver/device completion을 원래 request와 빠르게 연결한다. Tag pool이 소진되면 새 request가 기다려야 하므로 tag count는 사실상 queue capacity다. Shared tag set을 쓰는 여러 queue/device는 서로의 outstanding capacity를 간접적으로 경쟁할 수 있다.

## CHAPTER 09 · Tag exhaustion은 storage가 느린 것이 아니라 dispatch admission이 막힌 상태다

Device completion이 늦거나 queue depth가 과도하게 높으면 tag 반환 속도가 submission 속도를 못 따라간다. 이때 upper layer는 request allocation/dispatch에서 stall할 수 있다. Device latency와 tag wait를 분리해 측정해야 controller service 문제인지 block-layer admission 문제인지 알 수 있다.

## CHAPTER 10 · Plugging은 짧은 시간 request를 모아 merge 기회를 만든다

Thread가 연속 block request를 발생시킬 때 block layer가 잠깐 모아 인접 request를 합치면 command 수를 줄일 수 있다. 그러나 delay를 너무 길게 두면 merge 이득보다 latency가 커진다. Plugging은 batching과 동일하게 throughput/latency trade-off를 만든다.

## CHAPTER 11 · Back merge와 front merge는 logical adjacency를 device request로 합친다

새 bio가 기존 request 뒤나 앞의 contiguous sector와 연결될 수 있으면 하나의 더 큰 request로 합칠 수 있다. Merge는 command overhead를 줄이지만 max segment, boundary, device limit, operation compatibility를 지켜야 한다. 모든 sequential user I/O가 자동으로 하나의 request가 되는 것은 아니다.

## CHAPTER 12 · Merge는 observability count를 바꾼다

Application이 100번 write를 호출해도 bio merging과 request merging으로 device에는 더 적은 command가 갈 수 있다. 반대로 큰 I/O가 device limit 때문에 여러 request로 split될 수 있다. Performance 분석은 syscall count→bio count→request count→device command count를 각각 구분해야 한다.

## CHAPTER 13 · I/O scheduler는 media seek만 최적화하는 legacy 기능이 아니다

SSD에서도 scheduler는 fairness, latency class, read/write prioritization, workload isolation 같은 policy를 제공할 수 있다. Mechanical seek 최적화가 중요하지 않아도 queue competition은 남는다. Scheduler 선택은 device type 하나로 결정하지 말고 latency target과 multi-tenant behavior를 봐야 한다.

## CHAPTER 14 · NONE scheduler도 scheduling이 전혀 없다는 뜻은 아니다

I/O scheduler layer에서 reordering을 하지 않아도 blk-mq queue mapping, hardware dispatch resource, driver/device queueing은 존재한다. `none`을 선택했다고 application request가 device에 즉시 도달하는 것은 아니다. 다른 queue stage의 latency를 계속 측정해야 한다.

## CHAPTER 15 · Scheduler fairness는 throughput optimum과 충돌할 수 있다

Large sequential writer 하나가 queue를 계속 채우면 throughput은 높지만 small latency-sensitive read가 뒤에 묻힐 수 있다. Scheduler가 read latency나 per-cgroup fairness를 위해 reordering하면 aggregate throughput이 약간 낮아질 수 있다. Storage policy는 service class 목표를 명시해야 한다.

## CHAPTER 16 · Completion order는 submission order와 다를 수 있다

여러 hardware queue와 device internal parallelism에서는 뒤에 제출된 request가 먼저 끝날 수 있다. Block layer도 protocol도 일반적으로 global completion order를 보장하지 않는다. Filesystem/database가 ordering을 필요로 한다면 barrier, flush, FUA, dependency protocol을 명시해야 한다.

## CHAPTER 17 · Request completion은 device success와 upper-layer semantic completion을 연결한다

Driver가 request 완료를 block layer에 알리면 bio end_io callback과 upper layer wakeup이 이어질 수 있다. Partial/error status가 bio chain에 어떻게 전파되는지 중요하다. `device command 끝남`과 `user request가 관측 가능한 완료 상태` 사이에는 callback/scheduler delay가 남을 수 있다.

## CHAPTER 18 · Timeout은 service-time threshold가 아니라 ownership recovery trigger다

Request timeout이 발생하면 driver가 reset, abort, retry 같은 recovery를 수행할 수 있다. Timeout이 너무 짧으면 정상 tail request를 failure로 오판하고, 너무 길면 stuck command가 tag와 queue resource를 오래 점유한다. Timeout policy는 device recovery time과 upper SLO를 함께 고려해야 한다.

## CHAPTER 19 · Retry는 queue pressure를 증폭할 수 있다

Transient error에 모든 request를 즉시 재제출하면 이미 포화된 device queue에 추가 load를 만든다. Retry backoff와 max attempts, idempotency를 명시해야 한다. Storage retry storm은 application retry storm과 결합될 때 load amplification이 더 커진다.

## CHAPTER 20 · Read/write mix는 scheduler와 device resource를 다르게 소비한다

Writeback이 background라고 해도 device queue와 tags를 점유해 foreground read latency에 영향을 줄 수 있다. Write-heavy workload에서 p99 read가 악화되면 read code path가 아니라 dirty writeback과 queue occupancy를 확인해야 한다. Separate class/priority가 필요한 이유다.

## CHAPTER 21 · Flush와 FUA는 ordering/durability command를 queue에 삽입한다

Filesystem/database의 persistence contract를 지키려면 data write와 cache flush/FUA ordering이 device까지 전달돼야 한다. Flush는 단순 느린 write가 아니라 앞선 writes의 persistence boundary를 만든다. Queue reordering이 있어도 이 dependency를 보존해야 한다.

## CHAPTER 22 · Discard/TRIM도 block queue resource를 소비한다

Filesystem free-space 회수 후 discard를 발행하면 SSD FTL에 unused range를 알릴 수 있지만 large discard가 foreground I/O와 경쟁할 수 있다. Batch size, async execution, rate limiting 없이 discard를 과도하게 보내면 latency spike를 만들 수 있다.

## CHAPTER 23 · Zoned device는 random-write queue assumption을 깨뜨린다

Host-managed zoned storage는 zone write pointer와 sequential write constraint를 갖는다. Block layer와 filesystem이 zone state를 지키지 않으면 request가 device에서 거부될 수 있다. Queue scheduler도 arbitrary reordering이 zone ordering constraint를 위반하지 않도록 해야 한다.

## CHAPTER 24 · blk-cgroup은 I/O cost를 workload boundary에 귀속한다

여러 container/process group이 같은 block device를 공유하면 cgroup I/O controller가 bandwidth/IOPS/weight policy를 적용할 수 있다. Host device가 idle해 보여도 특정 cgroup quota 때문에 workload가 throttle될 수 있다. Application latency와 host-level utilization을 같이 봐야 한다.

## CHAPTER 25 · I/O throttling은 queue에 의도적 delay를 추가한다

Bandwidth/IOPS limit은 device protection과 tenant fairness를 제공하지만 request를 software layer에 대기시킨다. Throttle wait를 device latency에 포함시키면 hardware가 느리다고 오판한다. Storage SLO는 policy-induced wait와 physical service time을 구분해야 한다.

## CHAPTER 26 · CPU/NUMA mapping은 submission queue locality를 바꾼다

Software queue와 hardware context가 CPU/node 기준으로 mapping되므로 submitting thread가 이동하거나 storage adapter가 remote NUMA node에 있으면 metadata와 completion이 cross-node traffic을 만들 수 있다. Block I/O tuning도 CPU topology와 device topology를 함께 봐야 한다.

## CHAPTER 27 · Completion CPU가 application wakeup locality를 바꾼다

Device interrupt/completion이 특정 CPU에서 처리되고 waiting thread가 다른 CPU에서 깨어나면 scheduler migration과 cache warmup 비용이 추가될 수 있다. Storage latency의 마지막 구간은 device completion timestamp 이후에도 존재한다. IRQ affinity와 application pinning을 함께 분석해야 한다.

## CHAPTER 28 · Block trace는 bio/request/device stage를 분리해야 한다

한 request의 total latency를 submission, merge/scheduler wait, dispatch, device service, completion callback, task wakeup으로 쪼개야 한다. Device p99만 보면 software queue stall을 놓치고, syscall p99만 보면 device 원인을 알 수 없다. Stage별 timestamp와 request identity를 연결해야 한다.

## CHAPTER 29 · Synthetic block benchmark는 filesystem/application reality와 다를 수 있다

Direct raw-device benchmark는 page cache, filesystem metadata, writeback, journaling을 우회할 수 있다. 반대로 buffered benchmark는 RAM/cache가 device보다 더 큰 영향을 줄 수 있다. Queue-depth와 read/write distribution도 production과 다르면 blk-mq scheduler behavior가 달라진다.

## CHAPTER 30 · Block layer 성능은 request lifecycle 전체의 ownership 문제다

안정적인 storage path는 **bio 생성, request merge, software queue, scheduler, hardware dispatch, tag capacity, device completion, flush ordering, cgroup throttling, CPU locality**를 하나의 lifecycle로 본다. `SSD가 빠르다`는 hardware 특성만으로 application latency를 설명할 수 없으며, 어느 queue가 request ownership을 얼마나 오래 보유했는지가 최종 진단 기준이다.
