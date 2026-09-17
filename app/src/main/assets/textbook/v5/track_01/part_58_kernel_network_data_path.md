# PART 58 · Kernel Network Data Path — socket, skb, NAPI, queues, offload, CPU placement

Network I/O는 `send()`와 `recv()` 사이에 존재하는 black box가 아니다. 한 packet은 **socket buffer ownership, protocol stack, queue discipline, NIC descriptor ring, interrupt/NAPI, CPU steering, checksum/segmentation offload**를 통과한다. Latency와 packet loss를 설명하려면 어느 queue에서 누가 packet을 소유했고 언제 다음 stage로 넘겼는지 복원해야 한다.

## CHAPTER 01 · Socket send success는 wire transmission completion이 아니다

`send` 계열 호출이 반환했다는 사실은 userspace buffer에서 kernel socket state로 data ownership이 넘어갔음을 뜻할 수 있지만 NIC가 이미 frame을 전송했다는 보장은 아니다. Socket send buffer, transport queue, qdisc, driver ring, NIC hardware queue가 뒤에 남아 있다. Completion 의미를 API boundary별로 구분하지 않으면 application latency와 network service time을 섞는다.

## CHAPTER 02 · Socket buffer limit은 application backpressure의 첫 경계다

송신 socket buffer가 가득 차면 blocking socket은 wait할 수 있고 nonblocking socket은 capacity 부족을 반환할 수 있다. 이 pressure는 NIC queue saturation뿐 아니라 transport congestion control, peer receive window, qdisc backlog 등 여러 downstream 원인에서 올라온다. 따라서 `send가 막혔다`는 증상은 network path 전체의 backpressure signal이다.

## CHAPTER 03 · sk_buff는 packet bytes가 아니라 packet state의 중심 metadata다

Linux `sk_buff`는 protocol header 위치, device, checksum state, segmentation metadata, fragments, ownership/refcount 같은 정보를 담는다. Packet payload는 linear head와 page fragments 등 별도 buffer에 존재할 수 있다. `packet 하나=연속 byte 배열 하나`라고 가정하면 clone, zero-copy fragment, GSO packet의 실제 memory ownership을 설명할 수 없다.

## CHAPTER 04 · skb clone은 metadata 복제와 payload 공유를 분리한다

Transport가 retransmission을 위해 payload를 보존하면서 lower layer가 header를 조작하려면 metadata clone과 shared data reference가 필요할 수 있다. Clone된 skb가 같은 payload를 가리키면 write-before-copy 규칙이 중요해진다. Refcount와 header mutability가 틀리면 packet corruption이나 use-after-free로 이어진다.

## CHAPTER 05 · Headroom은 protocol layer가 header를 prepend할 공간이다

Upper layer payload 앞에 TCP/IP/Ethernet header를 붙일 때 매번 전체 packet을 새 buffer로 복사하면 비용이 크다. Packet buffer는 앞쪽 headroom을 예약해 lower layer가 header를 prepend할 수 있다. Headroom 부족은 reallocation/copy path를 만들 수 있으므로 buffer geometry가 data-path 비용에 직접 영향을 준다.

## CHAPTER 06 · GSO는 큰 logical packet을 늦게 segmentation한다

Generic Segmentation Offload는 upper stack이 큰 skb를 유지하고 하위 stack/NIC 가까운 곳에서 MTU 크기의 packet으로 나누게 한다. 이렇게 하면 per-packet protocol processing 횟수를 줄일 수 있다. 그러나 capture point에 따라 실제 wire packet보다 큰 skb가 관측되므로 packet analyzer 결과를 무조건 wire reality로 해석하면 안 된다.

## CHAPTER 07 · TSO는 segmentation work 일부를 NIC로 이동한다

TCP Segmentation Offload를 지원하는 NIC는 큰 TCP payload와 metadata를 받아 hardware에서 여러 frame으로 나눌 수 있다. CPU work는 줄지만 NIC descriptor, DMA, checksum capability와 결합된다. Offload가 꺼지거나 fallback되면 같은 application load에서 CPU usage와 packet rate가 급변할 수 있다.

## CHAPTER 08 · GRO는 receive packet 여러 개를 stack 상단에서 합친다

Generic Receive Offload는 동일 flow의 packet을 더 큰 logical skb로 합쳐 upper protocol processing 횟수를 줄일 수 있다. Throughput은 개선되지만 batching delay와 capture granularity가 변한다. Low-latency workload에서는 GRO flush timing이 p99에 영향을 줄 수 있으므로 throughput 최적화와 latency 목표를 분리해야 한다.

## CHAPTER 09 · Checksum offload는 checksum value와 checksum state를 분리한다

TX path에서 kernel이 final checksum을 계산하지 않고 NIC에 계산 responsibility를 넘길 수 있고, RX path에서는 NIC가 검증 결과를 metadata로 전달할 수 있다. Packet capture에서 checksum이 `틀려 보인다`고 실제 wire corruption을 바로 결론내리면 안 된다. Capture point가 offload 전인지 후인지 확인해야 한다.

## CHAPTER 10 · qdisc는 NIC 앞의 scheduling/backpressure layer다

Transmit packet은 queue discipline을 통해 classifying, ordering, shaping, dropping될 수 있다. NIC ring이 비어 있어도 qdisc policy가 packet을 delay할 수 있고, qdisc backlog가 늘어도 transport layer와 application은 다른 시간축으로 pressure를 느낄 수 있다. TX latency 분석에는 socket queue와 qdisc queue를 분리한 timestamp가 필요하다.

## CHAPTER 11 · Multi-queue NIC는 하나의 device 안에 여러 independent queue를 만든다

현대 NIC는 여러 TX/RX descriptor queue를 제공해 parallel packet processing을 가능하게 한다. Queue mapping이 잘못되면 많은 CPU가 있어도 특정 queue만 포화되고 head-of-line blocking이 생길 수 있다. `NIC utilization 40%` 같은 aggregate 값은 queue imbalance를 숨길 수 있다.

## CHAPTER 12 · Descriptor ring은 CPU와 NIC가 ownership을 교환하는 shared queue다

Driver는 descriptor에 DMA buffer address와 length를 기록하고 ownership을 device에 넘긴다. NIC가 처리 후 completion state를 기록하면 driver가 buffer를 reclaim한다. Producer/consumer index, memory barrier, DMA visibility가 맞지 않으면 descriptor를 너무 일찍 재사용하거나 device가 stale metadata를 읽을 수 있다.

## CHAPTER 13 · Doorbell write는 queue state가 준비된 뒤 device에 알려야 한다

CPU가 descriptor contents를 memory에 준비하고 tail pointer/doorbell MMIO를 갱신해 NIC에 새 work를 알리는 구조에서는 ordering이 중요하다. Device notification이 descriptor visibility보다 앞서면 NIC가 incomplete state를 관측할 수 있다. DMA memory ordering과 MMIO ordering은 ordinary heap write와 같은 규칙으로 가정하면 안 된다.

## CHAPTER 14 · RX path는 빈 buffer를 NIC에 미리 공급해야 한다

Receive queue에는 NIC가 packet을 DMA할 destination buffer가 준비돼 있어야 한다. Driver가 refill을 제때 못 하면 ring starvation과 drop이 생길 수 있다. Packet drop을 protocol stack에서만 찾으면 hardware ring capacity 부족이나 memory allocation failure를 놓칠 수 있다.

## CHAPTER 15 · Hard IRQ에서 모든 packet을 처리하면 interrupt storm이 된다

매 packet arrival마다 interrupt context에서 전체 protocol processing을 하면 high packet rate에서 CPU가 interrupt handling에 잠식될 수 있다. Linux NAPI는 interrupt를 event trigger로 사용하고 일정 budget 안에서 polling processing으로 전환해 interrupt rate를 제어한다. 이것은 interrupt 제거가 아니라 **event notification과 bulk processing의 분리**다.

## CHAPTER 16 · NAPI poll budget은 fairness와 throughput을 동시에 조절한다

한 NAPI instance가 한 번에 너무 많은 packet을 처리하면 다른 device/flow/task가 CPU를 못 받을 수 있고, 너무 적게 처리하면 scheduling overhead가 커진다. Budget은 packet processing의 batch size이자 CPU fairness parameter다. High-throughput tuning은 NAPI budget과 application scheduler latency를 함께 봐야 한다.

## CHAPTER 17 · Busy polling은 interrupt latency를 CPU consumption으로 바꾼다

Application/kernel이 packet arrival을 기다리며 적극적으로 NAPI poll을 수행하면 interrupt delivery와 scheduler wakeup 시간을 줄일 수 있다. 그러나 idle 때도 CPU를 소비하고 power/thermal budget을 사용한다. Busy poll은 `빠른 네트워크 옵션`이 아니라 **latency와 energy/capacity의 trade-off**다.

## CHAPTER 18 · RSS는 NIC가 flow를 hardware RX queue에 분산한다

Receive Side Scaling은 flow hash 등을 사용해 incoming packet을 여러 receive queue로 분배한다. Queue별 interrupt affinity를 다르게 두면 여러 CPU가 병렬 처리할 수 있다. 하지만 flow 하나는 ordering과 cache locality 때문에 특정 queue에 고정되는 경우가 많아 elephant flow 하나가 queue hotspot을 만들 수 있다.

## CHAPTER 19 · RPS는 hardware queue 뒤에서 software CPU steering을 한다

Receive Packet Steering은 packet이 driver를 통과한 뒤 protocol processing target CPU를 software로 선택할 수 있다. NIC queue 수가 적을 때 parallelism을 늘릴 수 있지만 cross-CPU enqueue와 IPI 비용이 추가된다. NUMA remote CPU로 보내면 packet data가 있는 memory node와 processing CPU가 멀어질 수도 있다.

## CHAPTER 20 · RFS는 flow locality를 application consumer와 맞추려 한다

Receive Flow Steering은 단순 hash 분산보다 해당 flow를 소비하는 application thread CPU와 protocol processing을 가깝게 두는 것을 목표로 한다. 이득은 cache locality에서 오지만 scheduler migration이 잦으면 mapping이 stale해질 수 있다. Network steering과 process affinity는 독립 설정이 아니다.

## CHAPTER 21 · XPS는 transmit queue와 CPU placement를 연결한다

Transmit Packet Steering은 송신 packet을 어느 TX queue로 보낼지 CPU/flow 기준으로 조정해 queue lock contention과 cache bouncing을 줄일 수 있다. 잘못된 mapping은 특정 queue saturation이나 remote NUMA access를 만든다. TX queue mapping은 NIC hardware queue count만 보고 정하면 안 된다.

## CHAPTER 22 · Softirq processing은 application thread와 같은 CPU를 경쟁할 수 있다

Network receive work가 softirq context에서 많이 실행되면 user thread CPU time이 줄고 scheduler latency가 늘 수 있다. CPU utilization 100%만 보면 application computation과 kernel packet processing을 구분할 수 없다. softirq time, NAPI poll, user time을 같은 CPU timeline에 놓아야 한다.

## CHAPTER 23 · Packet drop은 하나의 counter가 아니라 여러 queue의 failure result다

NIC hardware ring overflow, driver allocation failure, backlog overflow, qdisc drop, socket receive buffer full, protocol validation failure는 서로 다른 drop이다. Interface-level dropped counter 하나로 root cause를 찾을 수 없다. Drop location마다 ownership과 recovery semantics가 다르므로 stage-specific counter와 tracepoint가 필요하다.

## CHAPTER 24 · Receive socket buffer는 application read rate와 kernel arrival rate의 경계다

Application이 `recv`를 충분히 빨리 호출하지 못하면 socket receive queue가 쌓이고 limit에 도달할 수 있다. TCP는 advertised window 등을 통해 sender를 늦출 수 있지만 UDP 같은 datagram path는 drop으로 끝날 수 있다. 같은 `앱이 늦게 읽음`이 transport에 따라 다른 failure semantics를 만든다.

## CHAPTER 25 · Zero-copy는 copy를 없애는 대신 buffer lifetime을 길게 만든다

Userspace page를 pin하거나 shared buffer를 NIC/stack과 공유하면 memcpy를 줄일 수 있지만 completion 전까지 buffer를 재사용할 수 없다. Cancellation·process exit·device reset 때 ownership 회수가 복잡해진다. Zero-copy 최적화는 byte-copy 비용을 **lifetime protocol과 pinned-memory pressure**로 교환한다.

## CHAPTER 26 · NUMA locality는 NIC queue, memory, CPU 세 위치를 함께 맞춰야 한다

NIC가 특정 NUMA node의 PCIe root에 연결되고 RX buffer가 그 node에서 할당되는데 protocol processing을 remote node CPU에서 하면 interconnect traffic이 늘어난다. RSS/RPS/XPS와 irq affinity, application pinning을 별도로 최적화하면 서로 충돌할 수 있다. Network topology tuning은 device-memory-CPU 삼각형으로 봐야 한다.

## CHAPTER 27 · Interrupt coalescing은 packet latency와 interrupt rate를 교환한다

NIC가 여러 completion을 모아 interrupt를 덜 발생시키면 CPU overhead는 줄지만 첫 packet이 interrupt를 기다리는 시간이 늘 수 있다. Adaptive coalescing은 traffic rate에 따라 정책을 바꿀 수 있지만 latency distribution을 더 동적으로 만든다. Throughput benchmark와 latency-sensitive production 설정이 같을 이유가 없다.

## CHAPTER 28 · Packet timestamp는 어느 layer에서 찍혔는지 알아야 의미가 있다

Userspace send time, kernel enqueue time, NIC hardware TX timestamp, peer receive timestamp는 서로 다른 event다. Clock domain도 software realtime과 PHC hardware clock이 다를 수 있다. Network latency 분해는 timestamp value만 수집하는 게 아니라 **event point와 clock source를 함께 기록**해야 한다.

## CHAPTER 29 · Network observability도 offload와 batching 때문에 원본 event를 왜곡할 수 있다

GRO가 여러 wire packet을 하나의 skb로 합치고 GSO가 큰 skb를 여러 wire frame으로 나누므로 kernel trace/capture 지점마다 packet count와 size가 달라진다. Sampling이나 eBPF hook도 특정 layer만 관측한다. 서로 다른 관측 도구의 packet 수가 다르다고 바로 data loss를 결론내리지 말고 hook 위치를 먼저 맞춰야 한다.

## CHAPTER 30 · Packet path 성능은 queue ownership과 CPU placement의 합성 문제다

Network throughput/latency를 안정적으로 설계하려면 **socket buffer, skb lifetime, qdisc, descriptor ring, NAPI budget, offload, RSS/RPS/XPS, IRQ affinity, NUMA locality, application read/write rate**를 하나의 pipeline으로 봐야 한다. 어느 한 layer의 utilization이 낮아도 앞뒤 queue imbalance가 p99와 drop을 지배할 수 있다. 최종 진단 단위는 `NIC가 느리다`가 아니라 packet이 어느 stage에서 얼마나 머물렀는가다.
