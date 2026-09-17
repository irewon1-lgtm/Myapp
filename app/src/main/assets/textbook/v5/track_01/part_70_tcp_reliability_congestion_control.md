# PART 70 · TCP Reliability and Congestion Control — sequence space, ACK clock, RACK/TLP, cwnd, pacing, ECN

TCP는 `연결된 신뢰성 있는 스트림`이라는 API 뒤에서 **byte sequence space, acknowledgement, retransmission, receive flow control, congestion control, timers, reordering inference**를 지속적으로 갱신한다. Packet이 사라졌다는 사실과 network가 혼잡하다는 판단은 같은 사건이 아니다. 구현은 loss detection과 congestion response를 분리하면서도 sender flight를 안전한 범위로 제한해야 한다.

## CHAPTER 01 · TCP sequence number는 packet 번호가 아니라 byte-stream 위치다

TCP sequence space는 payload byte position을 나타낸다. 하나의 segment 크기가 바뀌거나 retransmission에서 segment boundary가 달라져도 동일 byte range는 동일 logical data다. Packet count 중심으로 상태를 추적하면 SACK, retransmission, coalescing에서 identity가 깨진다.

## CHAPTER 02 · SYN과 FIN도 sequence space를 소비한다

Connection establishment와 graceful close control flags는 sequence-number accounting에 포함된다. 따라서 first payload sequence와 final ACK 계산은 단순 payload byte count만 더한 값이 아니다. State machine transition과 sequence validity를 함께 봐야 RST/duplicate segment 처리도 정확해진다.

## CHAPTER 03 · Send state는 acknowledged, sent-unacked, unsent data를 분리한다

Application send buffer의 bytes가 모두 wire에 나간 것은 아니다. Congestion window, receiver window, pacing, NIC queue 때문에 일부만 outstanding flight가 된다. Sender는 `SND.UNA`류 acknowledgement frontier와 next sequence를 구분해 retransmission과 new-data scheduling을 결정한다.

## CHAPTER 04 · Cumulative ACK는 앞선 연속 byte range를 한 번에 확정한다

ACK number는 해당 sequence 이전의 모든 bytes를 연속적으로 받았음을 뜻한다. 중간 hole이 있으면 뒤 data를 받아도 cumulative frontier는 hole 앞에 머문다. 이 특성이 duplicate ACK와 SACK-based loss inference의 배경이다.

## CHAPTER 05 · SACK는 cumulative ACK 뒤의 received islands를 표현한다

Selective Acknowledgment option은 receiver가 out-of-order로 받은 byte ranges를 sender에 알려준다. Sender는 scoreboard를 사용해 어떤 ranges가 이미 receiver에 있는지 추적하고 필요한 holes만 retransmit할 수 있다. SACK 정보 자체는 cumulative delivery guarantee를 바꾸지 않는다.

## CHAPTER 06 · Reordering과 loss를 구분하지 못하면 spurious retransmission이 생긴다

Packet이 network에서 순서가 뒤바뀐 것뿐인데 sender가 loss로 판단해 retransmit하면 bandwidth와 congestion state를 불필요하게 소비한다. Loss detector는 reorder tolerance를 가져야 한다. 반대로 tolerance가 너무 크면 실제 loss recovery가 늦어진다.

## CHAPTER 07 · RTT sample은 retransmission ambiguity를 피해야 한다

ACK가 original transmission을 확인한 것인지 retransmission을 확인한 것인지 모르면 RTT sample이 잘못될 수 있다. RTT estimator는 valid samples를 선택하고 smoothed RTT와 variation을 유지한다. Timer는 single last-RTT 숫자로 설정하면 jitter network에서 불안정해진다.

## CHAPTER 08 · RTO는 RTT estimate에 safety margin을 더한 last-resort recovery timer다

Retransmission timeout은 ACK feedback 자체가 끊겼을 때 sender가 progress를 회복하는 최후 수단이다. RTO가 너무 짧으면 congestion/reordering을 loss로 오인하고, 너무 길면 dead connection recovery가 늦다. Repeated timeout은 exponential backoff로 network load를 줄인다.

## CHAPTER 09 · RTO recovery는 congestion state를 강하게 축소한다

Entire flight나 ACK clock이 사라졌을 가능성이 큰 RTO는 sender가 매우 보수적으로 다시 시작하게 만든다. Tail loss 하나가 RTO까지 가면 short request latency가 크게 늘어나는 이유다. Modern loss recovery는 가능한 한 ACK feedback을 이용해 RTO 전에 복구하려 한다.

## CHAPTER 10 · RACK는 sequence distance보다 transmit-time order로 loss를 추론한다

Recent ACKnowledgment loss detection은 최근 ACK된 segment보다 충분히 오래 전에 전송된 unacked segment를 loss 후보로 본다. Per-segment transmit timestamp와 SACK feedback을 사용하므로 application-limited traffic과 reordering에서 fixed duplicate-ACK threshold보다 유연하다.

## CHAPTER 11 · Reordering window는 RACK의 false-positive/slow-recovery trade-off다

RACK가 `얼마나 오래됐으면 lost`로 볼지 정하는 reordering window가 너무 작으면 reordered segment를 loss로 오인하고 너무 크면 recovery가 늦어진다. Network path의 reorder behavior와 min RTT를 반영해야 한다.

## CHAPTER 12 · TLP는 tail loss에서 ACK feedback을 다시 만들기 위한 probe다

Flight 마지막 packet이 사라지면 뒤 packet이 없어 duplicate ACK가 생기지 않을 수 있다. Tail Loss Probe는 RTO보다 앞서 probe/new data를 보내 ACK response를 유도하고 RACK recovery를 시작하게 한다. TLP는 loss를 독립적으로 판정하는 congestion algorithm이 아니라 feedback generator다.

## CHAPTER 13 · Loss detection과 congestion response는 분리되어야 한다

RACK가 segment를 lost로 표시했다고 즉시 아무 제한 없이 retransmit할 수 있는 것은 아니다. Congestion-control state가 허용하는 sending rate/window 안에서 repair해야 한다. Loss detector는 evidence를 만들고 congestion algorithm은 network load response를 결정한다.

## CHAPTER 14 · Congestion window는 outstanding network data 상한의 핵심 상태다

`cwnd`는 receiver capacity와 별개로 sender가 network congestion 관점에서 허용하는 flight size를 제한한다. Application buffer가 크고 receiver window가 넓어도 cwnd가 작으면 sender는 더 보내지 않는다. Throughput diagnosis에서 sender buffer, rwnd, cwnd를 각각 확인해야 한다.

## CHAPTER 15 · Slow start는 path capacity를 exponential하게 탐색한다

초기/회복 상태에서 ACK가 도착할 때마다 cwnd를 빠르게 늘려 available bandwidth를 탐색한다. 너무 보수적인 시작은 high-BDP path utilization을 늦추고 너무 공격적인 burst는 queue overflow를 만들 수 있다. Initial window와 pacing이 함께 중요하다.

## CHAPTER 16 · Congestion avoidance는 steady-state capacity probing을 완만하게 한다

Slow-start threshold 이후에는 cwnd growth를 줄여 loss/ECN signal 없이도 무한 exponential increase가 일어나지 않게 한다. 실제 algorithm은 Reno/CUBIC/BBR류마다 probing model이 다를 수 있지만 모두 network feedback에 따라 send permission을 조절한다.

## CHAPTER 17 · Fast recovery는 ACK clock을 유지한 채 lost data를 복구하려 한다

일부 loss가 있어도 later packets의 ACK/SACK이 계속 오면 sender는 전체 connection을 RTO state로 되돌리지 않고 missing ranges를 retransmit한다. Recovery 동안 pipe/flight estimate와 cwnd reduction을 조정해 network에 너무 많은 data를 추가하지 않는다.

## CHAPTER 18 · PRR은 recovery 중 실제 delivered data에 맞춰 transmission을 조절한다

Proportional Rate Reduction류 mechanism은 recovery 동안 ACK로 확인된 delivered amount에 비례해 repair/new data sending을 제한한다. 목표는 cwnd를 줄이는 순간 한꺼번에 idle되거나 반대로 excessive burst를 보내는 것을 피하는 것이다.

## CHAPTER 19 · Receiver window는 network congestion이 아니라 receiver memory/application speed를 표현한다

Advertised receive window는 peer receive buffer에서 수용 가능한 byte range를 sender에 알린다. Receiver application이 늦게 읽으면 rwnd가 줄어 sender를 멈출 수 있다. cwnd-limited와 rwnd-limited는 해결책이 전혀 다르다.

## CHAPTER 20 · Window scaling은 high-BDP connection에서 receive limit 표현 범위를 늘린다

16-bit 기본 window field만으로는 large bandwidth-delay product path를 충분히 채우기 어렵다. Connection setup에서 scale factor를 협상해 더 큰 logical window를 표현한다. 그러나 실제 socket memory/autotuning limit이 작으면 scale option만 켜도 throughput이 늘지 않는다.

## CHAPTER 21 · ACK clock은 data delivery rate를 sender scheduling signal로 바꾼다

ACK 도착은 network가 이전 data를 어느 속도로 전달했는지 나타내는 feedback이다. Sender가 ACK batch에 맞춰 큰 burst를 즉시 보내면 network queue가 흔들릴 수 있다. Modern stack은 pacing과 delivery-rate estimate로 burst를 평탄화하려 한다.

## CHAPTER 22 · Pacing은 cwnd라는 byte budget을 시간축 rate로 펼친다

cwnd가 1MB라고 해서 1MB를 한 순간에 NIC queue에 넣어야 하는 것은 아니다. Pacing rate는 allowed flight를 time interval에 나눠 전송해 queue burst를 줄인다. CPU/NIC TSO batching과 pacing granularity가 함께 작동하므로 packet timestamp 위치를 구분해야 한다.

## CHAPTER 23 · TSO는 pacing unit과 wire packet unit을 다르게 만든다

Sender는 큰 TSO skb를 stack/NIC에 넘기고 hardware가 여러 segments로 나눌 수 있다. Pacing implementation은 너무 큰 TSO batch가 wire에 burst로 나오지 않도록 size/time을 조절할 수 있다. `send packet count`와 wire segment spacing은 동일하지 않다.

## CHAPTER 24 · ECN은 packet drop 전에 congestion을 signal할 수 있다

ECN-capable endpoints와 network는 queue congestion에서 packet을 버리는 대신 CE marking을 사용할 수 있고 receiver가 이를 sender에 feedback한다. Sender는 loss가 없어도 congestion response를 해야 한다. `retransmission 없음=congestion 없음`은 틀린 판단이다.

## CHAPTER 25 · ECN negotiation과 feedback variant는 connection state다

양 endpoint가 지원을 합의해야 ECN semantics를 사용할 수 있고 classic/accurate feedback 등 capability에 따라 information granularity가 다를 수 있다. Middlebox/path compatibility 때문에 per-connection fallback을 고려해야 한다.

## CHAPTER 26 · Application-limited flow는 cwnd가 커도 throughput이 낮을 수 있다

Request/response application이 data를 충분히 공급하지 않으면 sender가 congestion window를 다 사용하지 못한다. 이 상태에서 `cwnd가 작아서 느리다`고 tuning하면 원인을 못 고친다. Send queue empty time과 app write timing을 함께 봐야 한다.

## CHAPTER 27 · Delayed ACK와 ACK aggregation은 sender feedback timing을 바꾼다

Receiver가 모든 segment마다 즉시 ACK하지 않거나 network가 ACK packets을 batch하면 RTT/delivery-rate sample과 sender burst pattern이 달라질 수 있다. ACK compression은 실제 forward delivery보다 빠른 feedback burst를 만들어 rate estimator를 왜곡할 수 있다.

## CHAPTER 28 · TIME-WAIT은 old duplicate segment가 new connection에 섞이는 것을 막는 state다

Active close 후 connection tuple을 일정 시간 보존하는 것은 resource 낭비가 아니라 sequence-space safety와 delayed packet isolation을 위한 것이다. High connection-rate server는 TIME-WAIT count를 capacity로 계획해야 하며 무작정 줄이면 protocol safety assumptions를 바꾼다.

## CHAPTER 29 · TCP observability는 state machine과 limit reason을 함께 기록해야 한다

Retransmission count만으로 부족하다. SRTT/RTTVAR, RTO, cwnd, ssthresh, rwnd, bytes in flight, delivery rate, pacing rate, SACK/RACK recovery, ECN signal, app-limited flag를 같은 timeline에서 봐야 한다. Linux SNMP/connection metrics는 aggregate와 per-flow 증거를 서로 보완한다.

## CHAPTER 30 · TCP performance는 reliability proof와 shared-network control의 합성 결과다

안정적인 transport 진단은 **byte sequence, ACK/SACK scoreboard, RTT/RTO, RACK/TLP loss inference, recovery, cwnd/rwnd, pacing, application supply, ECN, close-state lifetime**을 하나의 state machine으로 본다. `packet loss가 있다`거나 `network가 느리다`는 한 문장으로는 어느 control loop가 throughput과 latency를 제한하는지 알 수 없다.
