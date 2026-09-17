# PART 58 · Kernel Network Data Path — socket, skb, NAPI, queues, offload, CPU placement

네트워크 I/O는 `send()`와 `recv()` 사이의 black box가 아니다. Socket buffer에서 `sk_buff`, qdisc, NIC queue와 descriptor ring으로 내려가고, 수신은 interrupt/NAPI, GRO, protocol stack을 거쳐 socket queue로 올라온다. GSO/TSO/GRO, checksum offload, RSS/RPS/XPS는 packet 수와 CPU placement 자체를 바꾸므로 관측 숫자를 그대로 wire packet으로 해석하면 틀릴 수 있다. 이 PART는 **한 byte stream이 kernel과 NIC의 queue·CPU·buffer를 통과하는 실제 data path**를 추적한다.

---

## CHAPTER 01 · send 완료는 network peer 수신이 아니라 local stack이 data를 받아들였다는 의미부터 구분한다

Blocking `send()`가 bytes를 반환하면 일반적으로 그 양만큼 data가 local socket send buffer에 수락되었다는 뜻이다. NIC 전송, remote host 도착, application read까지 완료됐다는 의미가 아니다. TCP에서는 kernel이 재전송과 congestion control을 계속 수행하며, process가 이미 다음 일을 하고 있을 수 있다.

따라서 durability처럼 network에도 completion level이 있다. Userspace buffer copy 완료, local queue enqueue, NIC descriptor completion, transport ACK, application-level ACK가 서로 다른 지점이다. 어떤 수준이 business success인지 protocol에서 정해야 한다.

Timeout과 retry를 설계할 때 `send success`를 remote side effect 증거로 사용하지 않는다. Application acknowledgement나 idempotency가 필요한 이유다.

## CHAPTER 02 · socket backpressure는 downstream이 느릴 때 sender가 무한히 buffer하지 않게 한다

Peer나 network가 느리면 TCP send buffer와 qdisc/NIC queue가 차고 결국 `send()`가 block하거나 nonblocking mode에서 progress하지 못한다. 이 backpressure를 무시하고 application queue에 계속 복사하면 kernel 밖에 또 다른 unbounded backlog가 생긴다.

Event-driven server는 writable readiness가 항상 “전체 message를 보낼 수 있다”는 뜻이 아님을 이해해야 한다. Partial send 후 offset을 유지하고, high-water mark에서 upstream producer를 늦춘다.

Metrics에는 application queue와 socket send queue를 같이 둔다. Userspace queue만 비어 있는데 socket backlog가 계속 큰 경우 downstream congestion이 이미 kernel에 숨어 있을 수 있다.

## CHAPTER 03 · sk_buff는 packet data와 protocol metadata를 함께 운반하는 kernel network object다

Linux networking은 흔히 `sk_buff`를 중심으로 packet header, data pointer, checksum/offload state, device/protocol metadata를 전달한다. 매 layer가 raw bytes만 다시 복사하는 것이 아니라 같은 object를 수정·참조하며 pipeline을 통과한다. 그래서 skb allocation, clone, linearization 비용이 packet rate 성능에 직접 영향을 준다.

하나의 logical large packet이 GSO 때문에 wire 상의 여러 frame을 대표할 수도 있다. skb count를 wire packet count와 동일시하면 관측을 잘못 해석할 수 있다.

Memory pressure 사건에서는 payload bytes뿐 아니라 skb metadata와 queue에 머무는 object 수를 본다. Small packet workload는 byte rate가 낮아도 object rate가 매우 높을 수 있다.

## CHAPTER 04 · skb clone은 payload를 공유하며 metadata view를 복제해 copy를 줄인다

Packet을 여러 path에서 참조해야 할 때 전체 payload를 복사하는 대신 skb metadata만 clone하고 underlying data를 reference-counted 공유할 수 있다. 이 최적화는 copy bandwidth를 줄이지만 어느 path가 data를 수정할 수 있는지 copy-on-write 규칙을 필요로 한다.

Header 수정만 필요한 경우 head 영역을 분리하고 payload는 공유할 수 있다. 잘못된 mutation은 다른 clone이 보는 packet까지 바꾸므로 helper가 writable 여부를 확인한다.

Performance profile에서 clone 수가 많다면 routing/tap/filter path가 예상보다 packet fan-out을 만들고 있는지 본다. Clone 자체보다 이후 linearization/COW가 더 큰 비용일 수 있다.

## CHAPTER 05 · skb headroom과 tailroom은 header 추가를 위한 미리 확보된 공간이다

Protocol stack은 packet이 내려가며 Ethernet/IP/TCP header를 앞쪽에 추가하거나 metadata를 뒤쪽에 붙일 수 있다. 충분한 headroom이 있으면 새 buffer copy 없이 pointer만 이동해 header를 쓸 수 있다. 부족하면 reallocation과 data move가 필요해진다.

Tunnel encapsulation은 더 많은 outer header를 요구하므로 원래 예상보다 큰 headroom이 필요할 수 있다. MTU와 headroom은 다른 개념이지만 encapsulation 비용에서 함께 나타난다.

Custom network module은 필요한 headroom을 명시하고 helper를 사용한다. Raw pointer arithmetic으로 공간이 있다고 가정하면 memory corruption이 생긴다.

## CHAPTER 06 · GSO는 큰 logical packet을 kernel 안에서 늦게까지 유지해 per-packet 비용을 줄인다

Generic Segmentation Offload는 transport가 여러 MSS-sized packet으로 나눠야 할 data를 큰 skb로 유지하고 device에 가까운 지점에서 segmentation하게 한다. Protocol processing과 qdisc 일부가 더 적은 object를 다뤄 CPU overhead를 줄일 수 있다.

하지만 capture point에 따라 수십 KiB짜리 “packet”이 보일 수 있어 wire MTU 위반으로 오해할 수 있다. Offload 전후 관측 지점을 구분해야 한다.

GSO size와 segment count가 지나치게 크면 queue burst와 latency에 영향을 줄 수 있다. Throughput 이득과 microburst를 함께 본다.

## CHAPTER 07 · TSO는 NIC가 TCP segmentation을 수행해 host CPU의 packetization 비용을 줄인다

TCP Segmentation Offload를 지원하는 NIC는 큰 descriptor의 payload를 MSS 단위 wire packet으로 나누고 각 header/checksum을 생성할 수 있다. Host는 적은 descriptor와 protocol work로 높은 bandwidth를 낼 수 있다.

NIC feature가 꺼지거나 virtual path가 지원하지 않으면 segmentation이 software로 돌아와 CPU 사용량이 급증할 수 있다. 같은 traffic인데 host CPU가 갑자기 오른다면 offload state를 확인한다.

Benchmark에는 NIC feature flags와 packet rate를 기록한다. Byte throughput만 같아도 packets/s와 CPU cost는 크게 다를 수 있다.

## CHAPTER 08 · GRO는 여러 수신 packet을 큰 skb로 합쳐 upper stack 호출 횟수를 줄인다

Generic Receive Offload는 같은 flow의 연속 segment를 조건에 맞게 합쳐 protocol stack 위쪽이 더 큰 단위로 처리하게 한다. Per-packet routing, socket processing 비용을 줄여 throughput을 높인다. TSO의 수신측 대응처럼 보이지만 정확한 merge 조건은 protocol semantics에 의존한다.

Packet capture가 GRO 이후라면 wire보다 큰 packet과 적은 count가 보일 수 있다. IDS나 telemetry가 packet boundary에 의미를 두면 capture 위치를 명확히 해야 한다.

Low-latency workload에서는 coalescing이 batching delay를 만들 수 있는지 측정한다. Throughput 최적화가 항상 p99 latency 최적화는 아니다.

## CHAPTER 09 · checksum offload는 checksum 계산 책임과 packet metadata를 host와 NIC 사이에서 이동시킨다

Transmit path에서 kernel은 checksum이 아직 계산되지 않았음을 metadata로 표시하고 NIC가 final checksum을 채울 수 있다. Receive에서도 NIC가 검증 결과를 전달해 host 계산을 줄인다. 이 때문에 host capture에서 아직 wire에 나가지 않은 packet의 checksum이 “틀려 보이는” 현상이 생긴다.

Offload state를 모르는 diagnostic tool은 false corruption을 보고할 수 있다. 실제 wire capture나 NIC metadata와 교차 확인한다.

Driver는 partial checksum start/offset을 정확히 전달해야 한다. Tunnel과 segmentation이 결합되면 inner/outer checksum 책임을 구분한다.

## CHAPTER 10 · qdisc는 socket과 NIC 사이에서 scheduling·shaping·drop 정책을 적용한다

Queueing discipline은 packet을 어떤 순서로 device queue에 보낼지 정하고 rate shaping이나 fairness를 구현할 수 있다. Device가 busy하면 qdisc backlog가 늘어나 network latency의 상당 부분이 host 내부 queue에서 발생할 수 있다.

`ping`이 느리다고 wire만 조사하지 말고 qdisc delay와 drops를 확인한다. Buffer가 너무 크면 loss는 줄어도 bufferbloat로 tail latency가 커질 수 있다.

Traffic class별 정책을 도입하면 throughput과 latency workload를 분리할 수 있지만 classification 오류가 새 장애를 만든다. Queue별 backlog를 관측한다.

## CHAPTER 11 · multiqueue NIC는 하나의 device를 여러 TX/RX queue로 나눠 CPU parallelism을 만든다

고속 NIC는 여러 descriptor ring과 interrupt vector를 제공해 packet processing을 여러 CPU로 분산한다. Flow가 적절히 hash되어 queue에 나뉘면 single-core packet-rate bottleneck을 피할 수 있다. 그러나 한 elephant flow는 여전히 한 queue/CPU에 집중될 수 있다.

Queue 수를 CPU 수와 무조건 같게 만들면 cache와 IRQ overhead가 늘 수 있다. NUMA topology, expected flow count, RSS table을 함께 튜닝한다.

Per-queue bytes/packets/drops를 확인해 skew를 찾는다. Device 전체 평균만 보면 hot queue가 가려진다.

## CHAPTER 12 · descriptor ring은 host와 NIC가 packet buffer ownership을 주고받는 bounded queue다

TX ring에는 host가 NIC에 보낼 buffer descriptor를 넣고, RX ring에는 NIC가 채울 빈 buffer를 준비한다. Producer/consumer index와 completion을 통해 ownership이 이동한다. Ring이 가득 차면 추가 submit이 멈추고, RX ring이 비면 incoming packet을 drop할 수 있다.

DMA가 끝나기 전 buffer를 재사용하면 corruption이 생긴다. Memory ordering과 device doorbell semantics도 architecture에 따라 중요하다.

Ring occupancy와 refill failure는 driver-level backpressure의 핵심 지표다. Socket queue만 보고 NIC starvation을 놓치지 않는다.

## CHAPTER 13 · doorbell write는 준비된 descriptor를 NIC에 알리는 MMIO 경계다

Host가 ring memory에 descriptor를 써도 NIC가 즉시 새 work를 알지 못할 수 있다. Doorbell register에 producer index를 기록해 device가 처리할 범위를 알린다. 매 packet마다 doorbell을 치면 MMIO 비용이 커져 batching이 사용될 수 있다.

반대로 batching을 너무 오래 기다리면 latency가 증가한다. Descriptor memory write가 device에 보이기 전에 doorbell이 관찰되지 않도록 ordering rule을 지켜야 한다.

Driver 성능 분석에서는 packets/doorbell과 batch size를 본다. CPU instruction 수만으로 device notification overhead를 설명하기 어렵다.

## CHAPTER 14 · RX refill은 NIC가 쓸 빈 buffer를 계속 공급하는 data-path 생존 조건이다

수신 packet을 처리한 뒤 driver는 consumed descriptor에 새 page/buffer를 연결해 ring을 채워야 한다. Memory pressure나 allocation failure로 refill이 늦으면 NIC가 받을 공간이 없어 packet drop이 발생한다.

Page-pool 같은 recycling mechanism은 allocation cost와 cache locality를 개선할 수 있다. 하지만 buffer가 upper stack에 오래 붙잡히면 pool이 고갈될 수 있다.

RX no-buffer drop과 allocator pressure를 함께 본다. Network loss를 remote congestion으로 오해하지 않는다.

## CHAPTER 15 · NAPI는 interrupt storm을 줄이고 수신 처리를 polling budget으로 전환한다

Packet이 도착할 때마다 hardware interrupt를 계속 받으면 높은 packet rate에서 interrupt overhead가 과도해진다. NAPI는 초기 interrupt 후 queue를 polling mode로 처리하고 일정 budget만큼 packet을 소화한 뒤 필요하면 다음 softirq cycle로 넘긴다.

Low load에서는 interrupt가 빠른 반응을 제공하고 high load에서는 polling이 batching 효율을 제공한다. 이 hybrid model이 throughput과 latency를 절충한다.

CPU별 NAPI/softirq 시간을 관측하면 userspace가 CPU를 못 받는 원인을 찾을 수 있다. Application CPU utilization만 보면 kernel packet work가 빠질 수 있다.

## CHAPTER 16 · NAPI budget은 한 poll이 처리할 work와 다른 task에 양보할 시점을 정한다

Budget이 너무 작으면 poll 재스케줄과 overhead가 늘고 backlog가 쌓일 수 있다. 너무 크면 한 queue가 CPU를 오래 점유해 다른 network queue와 userspace task latency를 악화시킬 수 있다. Fairness와 throughput의 조절점이다.

Device queue 수와 traffic skew에 따라 적절한 budget이 달라진다. Hot queue 하나가 대부분 budget을 소비하는 경우 RSS 조정이 더 근본 해결일 수 있다.

Tuning 전후에 softirq runtime, backlog, drops, application p99를 같이 본다. Packet 처리량만 최대화하지 않는다.

## CHAPTER 17 · busy polling은 syscall sleep 대신 짧게 RX queue를 직접 확인해 latency를 줄일 수 있다

Latency-sensitive socket은 packet 도착을 기다리며 일정 시간 busy poll해 interrupt/scheduler wakeup 지연을 줄일 수 있다. 대신 CPU를 계속 소비하고 다른 workload와 power budget을 압박한다. 낮은 request rate에서도 core가 바빠질 수 있다.

Busy-poll thread와 NIC queue를 같은 CPU/NUMA domain에 배치해야 locality 이득이 커진다. Oversubscribed 환경에서는 오히려 tail이 나빠질 수 있다.

Latency 개선을 p50뿐 아니라 CPU%, energy, p99와 함께 평가한다. 전용 core가 없는 서비스에서 무조건 켜지 않는다.

## CHAPTER 18 · RSS는 packet header hash를 사용해 RX flow를 hardware queue에 분산한다

Receive Side Scaling은 NIC가 flow tuple을 hash하고 indirection table을 통해 RX queue를 선택하게 한다. 같은 flow를 같은 queue에 유지하면 packet ordering과 cache locality를 보존하면서 여러 flow는 여러 CPU에 분산할 수 있다.

Hash key와 indirection table이 불균형하면 특정 queue가 hot해질 수 있다. Flow 수가 적으면 queue가 많아도 parallelism이 나오지 않는다.

Per-flow/queue distribution을 측정하고 CPU affinity와 함께 튜닝한다. RSS queue와 application worker가 멀리 있으면 이후 cross-CPU transfer가 이득을 상쇄한다.

## CHAPTER 19 · RPS는 hardware queue 이후 software에서 packet processing CPU를 다시 분산한다

NIC queue 수가 부족하거나 IRQ가 일부 CPU에만 묶인 경우 Receive Packet Steering이 skb를 다른 CPU의 backlog로 enqueue해 protocol processing을 분산할 수 있다. 추가 inter-CPU queueing과 cache transfer 비용을 지불한다.

Hardware RSS가 충분히 잘 배치되어 있다면 RPS가 불필요할 수 있다. 반대로 virtual NIC처럼 hardware queue가 제한된 환경에서는 유용하다.

RPS 설정은 mask 크기보다 실제 CPU load와 NUMA locality로 평가한다. 분산 자체가 목표가 아니라 end-to-end 처리 비용 감소가 목표다.

## CHAPTER 20 · RFS는 flow를 실제 consuming application CPU 근처로 보내 cache locality를 높이려 한다

Receive Flow Steering은 단순 hash 분산을 넘어 해당 flow의 socket을 처리하는 application thread 위치를 참고해 packet processing CPU를 맞추려 한다. Socket data와 process working set이 같은 CPU cache에 있을 가능성을 높인다.

Thread가 자주 migration하면 steering target도 계속 바뀌어 이득이 줄 수 있다. Long-lived flow와 worker affinity가 있는 workload에서 효과가 더 명확할 수 있다.

측정에서는 packet processing CPU와 userspace consume CPU 사이 migration/cross-call을 본다. Throughput만으로 locality 개선 여부를 판단하지 않는다.

## CHAPTER 21 · XPS는 transmit queue와 CPU mapping을 조정해 TX lock/cache contention을 줄인다

Transmit Packet Steering은 CPU 또는 flow가 어느 TX queue를 사용할지 선택해 여러 sender가 같은 queue를 과도하게 경쟁하지 않도록 한다. Queue와 NIC completion CPU locality도 고려할 수 있다.

잘못된 mapping은 특정 queue만 포화시키거나 packet reorder 가능성을 높일 수 있다. Flow consistency와 qdisc semantics를 함께 본다.

TX queue별 stop/wake, bytes, completion CPU를 관찰해 tuning 효과를 확인한다. CPU mask를 설정했다는 사실만으로 균형이 보장되지 않는다.

## CHAPTER 22 · softirq는 network 처리 시간을 userspace scheduler time과 경쟁시키는 실행 context다

NAPI poll과 여러 protocol work는 softirq context에서 수행될 수 있다. Packet rate가 매우 높으면 특정 CPU가 softirq에 많은 시간을 써 application thread가 runnable인데도 실행 기회를 적게 받을 수 있다. 이는 userspace CPU profile만으로는 잘 보이지 않는다.

Work가 한 번에 끝나지 않으면 kernel thread로 넘겨져 처리될 수도 있어 latency pattern이 바뀐다. IRQ/NAPI affinity와 userspace pinning을 함께 설계한다.

Incident에서 `softirq%`, per-CPU packet rate, run queue를 같은 timeline에 둔다. Application code regression으로 오해하지 않는다.

## CHAPTER 23 · drop domain을 구분해야 packet loss의 실제 위치를 찾을 수 있다

Packet은 NIC RX buffer 부족, driver ring, qdisc, IP backlog, socket receive buffer, firewall 등 여러 지점에서 drop될 수 있다. 단일 `packet loss` metric은 어디서 버려졌는지 알려주지 않는다.

각 계층 counter의 의미와 증가 조건을 매핑하고, 동일 packet이 여러 counter에 중복 집계되는지도 확인한다. Hardware drop은 host stack에 packet 자체가 들어오지 않아 tcpdump에도 안 보일 수 있다.

Loss incident는 top-down 추측보다 queue별 counter를 좁혀간다. Remote sender의 retransmission과 local drop evidence를 맞추면 위치를 더 정확히 찾을 수 있다.

## CHAPTER 24 · RX socket buffer는 application 소비 속도와 network arrival burst 사이를 흡수한다

Kernel socket receive queue가 크면 일시적인 userspace stall을 견딜 수 있지만 queue에 오래 머문 packet의 latency가 증가하고 memory를 소비한다. 너무 작으면 burst에서 drop 또는 TCP window 축소가 자주 발생한다.

Auto-tuning은 traffic 특성에 따라 buffer를 늘릴 수 있으므로 connection 수가 많은 서버의 총 memory budget을 확인한다. Application queue까지 별도로 있으면 double buffering이 된다.

Queue occupancy와 read rate를 관측해 소비자가 느린지 network burst가 큰지 구분한다. Buffer size만 크게 올리는 것을 해결책으로 삼지 않는다.

## CHAPTER 25 · network zero-copy는 copy 비용을 줄이지만 completion과 buffer lifetime을 더 복잡하게 만든다

송신 zero-copy API는 userspace page를 kernel/NIC가 직접 참조하게 해 memory bandwidth를 절약할 수 있다. 대신 send syscall이 반환된 뒤에도 device가 buffer를 사용 중일 수 있어 별도 completion notification 전까지 mutation/reuse하면 안 된다.

Small message에서는 pinning과 completion bookkeeping이 copy보다 비쌀 수 있다. Large throughput transfer에서 이득이 더 잘 나타난다.

Buffer pool은 in-flight generation을 추적하고 error/cancellation에서도 completion을 회수해야 한다. CPU 감소만 보고 lifetime correctness를 희생하지 않는다.

## CHAPTER 26 · NUMA network locality는 NIC·IRQ·packet buffer·application CPU를 같은 domain에 맞추는 문제다

NIC가 socket 0 PCIe root에 연결됐는데 packet worker가 socket 1에서 실행되면 DMA buffer와 skb가 remote memory가 되어 interconnect traffic이 커질 수 있다. RSS queue와 IRQ를 NIC 가까운 CPU에 두고 application worker도 locality를 맞추면 cache와 memory latency를 줄일 수 있다.

하지만 load가 한 socket capacity를 넘으면 분산이 필요하다. Locality와 load balance 사이에서 실제 throughput curve를 측정한다.

Device NUMA node, page allocation node, CPU placement를 함께 기록한다. CPU affinity 하나만으로 locality를 증명하지 않는다.

## CHAPTER 27 · interrupt coalescing은 여러 packet을 묶어 interrupt 수를 줄이는 대신 대기 시간을 추가한다

NIC는 packet마다 즉시 interrupt하지 않고 일정 packet 수나 시간 동안 모아 한 번에 알릴 수 있다. High throughput에서는 interrupt overhead를 크게 줄이지만 sparse latency-sensitive traffic은 첫 packet이 timer 만료까지 기다릴 수 있다.

Adaptive coalescing은 load에 따라 값을 바꿀 수 있어 benchmark run마다 behavior가 달라질 수 있다. NIC 설정과 실제 interrupt rate를 함께 기록한다.

Tuning은 packets/interrupt, CPU%, p50/p99 latency를 동시에 본다. 최소 latency와 최대 throughput은 서로 다른 setting을 요구할 수 있다.

## CHAPTER 28 · packet timestamp는 어느 계층의 시간을 찍었는지 알아야 latency를 분해할 수 있다

Software timestamp는 kernel stack의 특정 지점 시간을 기록하고 hardware timestamp는 NIC 가까운 transmit/receive 순간을 제공할 수 있다. Application send time과 wire time 사이에는 queueing과 batching이 있어 서로 다른 값이다.

PTP 같은 clock synchronization이 없으면 두 host의 hardware timestamp를 직접 빼는 것도 부정확하다. Clock domain과 conversion을 명시한다.

Latency 분석에서는 timestamp source와 point를 metadata에 포함한다. 숫자 해상도가 높다는 이유만으로 end-to-end 의미가 정확해지는 것은 아니다.

## CHAPTER 29 · offload가 켜진 환경에서는 관측 지점에 따라 packet 수와 checksum·size가 달라진다

Tcpdump, interface counter, NIC hardware counter, application byte count는 서로 다른 stage를 본다. GSO 전 capture는 큰 skb 하나를, wire는 여러 segment를 볼 수 있고 GRO 후 capture는 여러 수신 packet이 합쳐져 있을 수 있다. Checksum도 device가 아직 채우기 전일 수 있다.

따라서 서로 다른 tool 숫자가 맞지 않는 것을 즉시 data loss로 해석하지 않는다. 각 counter가 counting하는 unit을 문서화한다.

Troubleshooting playbook에 offload on/off 비교를 포함하되 offload를 끄는 것 자체가 CPU와 timing을 크게 바꿀 수 있음을 표시한다.

## CHAPTER 30 · network data-path contract는 queue·ownership·CPU placement를 끝까지 연결하는 것이다

한 packet은 socket buffer, skb, qdisc, NIC ring을 지나며 여러 번 queue에 들어가고 ownership이 이동한다. Offload는 packet granularity를 바꾸고 RSS/NAPI는 어떤 CPU가 work를 수행하는지를 결정한다. 어느 한 계층만 보면 latency·drop·CPU 원인을 잘못 찾기 쉽다.

운영에서는 per-queue backlog/drop, softirq, IRQ/RSS mapping, socket memory, NIC offload state를 함께 수집한다. Byte throughput만으로 data path 건강성을 표현하지 않는다.

최종 검증은 representative traffic에서 wire-level 결과와 host-level counters를 교차 확인하는 것이다. `send()`가 성공했다거나 interface가 up이라는 사실만으로 end-to-end network path를 PASS라고 부를 수 없다.
