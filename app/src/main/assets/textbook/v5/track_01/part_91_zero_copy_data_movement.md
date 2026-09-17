# PART 91 · Zero-Copy Data Movement — page ownership, partial progress, backpressure, truncation races, socket queue lifetime

Zero-copy는 `read()`로 userspace buffer에 올렸다가 `write()`로 다시 kernel에 넣는 왕복 복사를 피하는 설계이지, 데이터가 어디에도 복사되지 않는다는 약속이 아니다. Linux의 `sendfile()`, `splice()`, `tee()`, `vmsplice()`는 page cache·pipe buffer·socket queue가 같은 backing memory를 **참조로 이어 붙이는 경로**를 만들 수 있지만, 그 순간 핵심 문제는 bandwidth보다 ownership이 된다. 어느 객체가 page를 붙잡고 있는지, syscall이 일부만 전진했을 때 어느 범위가 이미 소비됐는지, downstream이 막혔을 때 어디까지 메모리가 묶이는지, 원본 file이 truncate·overwrite될 때 참조 중인 bytes의 의미가 무엇인지, socket이 언제 그 참조를 놓는지를 모르면 zero-copy 코드는 빠른 코드가 아니라 데이터 손상과 무한 정체를 숨긴 코드가 된다.

## CHAPTER 01 · Zero-copy의 정확한 뜻은 copy count가 아니라 ownership boundary를 줄이는 것이다

일반적인 file→socket 전송은 `read()`가 page cache의 bytes를 userspace buffer로 복사하고, `write()`가 그 buffer를 socket send path가 관리하는 memory로 다시 복사하는 형태가 될 수 있다. Zero-copy 계열은 이 두 copy 중 하나 이상을 page reference, pipe buffer descriptor, socket fragment 같은 metadata 연결로 대체하려 한다. 그러나 syscall 진입·페이지 조회·checksum·protocol header 생성·DMA mapping·NIC 전송 같은 작업은 그대로 남는다. 따라서 “CPU가 bytes를 두 번 복사하지 않았다”와 “CPU 비용이 0이다”는 전혀 다른 주장이다. 더 중요한 변화는 **bytes의 소유권이 즉시 한 곳으로 끝나지 않는다는 점**이다. Caller가 syscall return을 보고 source를 곧바로 재사용해도 되는지, kernel이 아직 같은 backing page를 downstream queue에 붙잡고 있는지, fallback copy가 발생했는지를 API별 contract로 확인해야 한다. Zero-copy 성능을 평가할 때도 throughput만 보지 말고 CPU cycles, cache pollution, page retention, completion latency, blocked time을 같이 측정해야 실제 비용 이동을 볼 수 있다.

## CHAPTER 02 · Data path를 buffer가 아니라 reference graph로 그리면 lifetime이 보인다

`file → page cache page → pipe buffer → socket queue → NIC`를 단순 화살표로만 보면 각 단계가 bytes를 새로 복사하는 것처럼 오해하기 쉽다. 실제 zero-copy 경로에서는 pipe가 page 자체를 소유하는 대신 “이 page의 이 offset부터 이 length까지”를 가리키는 descriptor를 보관하고, 다음 단계가 같은 backing storage에 대한 추가 reference를 취할 수 있다. 이때 physical page의 lifetime과 logical data의 lifetime을 분리해야 한다. Reference count가 남아 있으면 page object가 즉시 재사용되는 것을 막을 수 있지만, 이것이 file contents에 대한 transaction snapshot을 자동으로 제공한다는 뜻은 아니다. 반대로 fd를 닫아도 이미 downstream object가 확보한 reference가 있다면 그 in-flight data는 fd table entry와 별개 lifetime을 가진다. 디버깅할 때는 “누가 fd를 갖고 있나”만 보지 말고 **어떤 page/pipe slot/socket skb가 어느 byte range를 아직 참조하는가**를 추적해야 한다. Zero-copy의 핵심 자료구조는 buffer 하나가 아니라 reference graph이며, correctness는 graph의 생성·이동·해제 순서를 증명하는 일이다.

## CHAPTER 03 · sendfile()은 file offset과 전송 progress를 동시에 다루는 stateful operation이다

`sendfile(out_fd, in_fd, offset, count)`는 성공 시 실제로 `out_fd`에 넘긴 byte 수를 반환하며, 요청한 `count`보다 적게 끝나는 것이 정상이다. `offset == NULL`이면 input open-file description의 현재 file offset을 사용하고 실제 전진량만큼 그 offset이 바뀐다. 별도 `off_t *offset`을 주면 input fd의 shared file offset은 건드리지 않고 pointed value만 전진한다. 이 차이는 여러 workers가 같은 open-file description을 공유할 때 매우 크다. 공용 offset을 사용하면 한 worker의 progress가 다른 worker의 시작점을 바꿀 수 있고, explicit offset을 쓰면 caller가 range partition과 retry state를 직접 소유한다. Linux는 한 호출에서 전송하는 양에도 상한이 있으므로 giant `count` 한 번으로 전체 file을 보냈다고 가정하면 안 된다. 올바른 loop는 `requested`, `completed`, `remaining`, `next_offset`을 별도 변수로 보존하고 **return value만큼만 commit**한다. Error가 나더라도 이전 호출에서 성공한 범위까지 되돌리는 자동 transaction은 없으므로, progress ledger가 곧 정확성의 기준이 된다.

## CHAPTER 04 · Partial success는 예외가 아니라 streaming I/O의 정상 상태다

Zero-copy API를 `n == requested`일 때만 성공으로 보는 wrapper는 현실의 socket·pipe backpressure를 견디지 못한다. `sendfile()`과 `splice()`는 양수의 짧은 길이를 반환할 수 있고, 이는 앞부분이 이미 downstream으로 넘어갔다는 뜻이다. 다음 호출은 반드시 그 길이 뒤에서 이어져야 한다. 반대로 `-1/EAGAIN`은 이번 호출에서 새 progress가 없었다는 의미로 취급해야 하며, 이전에 성공한 bytes까지 다시 보내면 duplication이 된다. `EINTR`도 “전체 operation이 취소됐다”는 고수준 의미로 단순화하면 위험하다. syscall boundary에서 실제 progress가 반환되었는지와 error만 반환되었는지를 구분해야 한다. File→pipe→socket처럼 두 단계 pipeline이면 더 복잡하다. Input에서 pipe로 64 KiB를 넣었는데 socket으로 16 KiB만 빠졌다면 source offset은 이미 64 KiB 전진했지만 end-to-end committed progress는 16 KiB다. 남은 48 KiB는 pipe가 소유한다. **source progress, staging occupancy, sink progress를 한 숫자로 합치지 않는 것**이 partial-transfer 버그를 막는 첫 원칙이다.

## CHAPTER 05 · Retry loop는 “다시 호출”이 아니라 세 개의 cursor를 일관되게 전진시키는 state machine이다

File→pipe→socket splice loop에는 최소 `source_cursor`, `pipe_occupancy`, `sink_cursor`가 존재한다. Source에서 pipe로 `a` bytes를 splice하면 source cursor는 `a`만큼 전진하고 pipe occupancy도 `a`만큼 늘어난다. 이어 pipe에서 socket으로 `b` bytes만 빠지면 occupancy는 `b`만큼 감소하고 sink cursor가 `b`만큼 전진한다. 여기서 `b < a`라고 source를 `a-b`만큼 되감아 다시 읽으면 동일 bytes가 pipe에 이미 남아 있으므로 중복 전송이 생긴다. 반대로 socket이 막혔다고 pipe를 버리고 source cursor만 유지하면 buffered tail이 유실될 수 있다. State machine은 “pipe가 비었으면 source refill”, “pipe에 data가 있으면 sink drain 우선”, “sink가 EAGAIN이면 writable readiness까지 대기”, “source EOF는 pipe가 완전히 drain된 뒤 end-to-end EOF로 승격” 같은 transition을 명시해야 한다. 이 구조는 구현 언어와 무관하다. **진행 상태를 syscall 호출 횟수가 아니라 byte ownership의 이동으로 모델링**해야 retry가 idempotent해진다.

## CHAPTER 06 · EAGAIN은 실패가 아니라 downstream credit이 0이라는 backpressure 신호다

Nonblocking socket이나 pipe에서 `EAGAIN`은 data가 잘못됐다는 뜻이 아니라 현재 즉시 수용할 capacity가 없다는 뜻이다. 이때 busy loop로 같은 `splice()`를 수천 번 재호출하면 CPU를 태우면서 queue state는 바꾸지 못한다. Readiness mechanism과 결합해 socket writable, pipe readable/writable 같은 조건이 다시 성립할 때까지 잠들어야 한다. 다만 `SPLICE_F_NONBLOCK` 하나만 켰다고 모든 endpoint가 완전 nonblocking이 되는 것도 아니다. Linux manual이 명시하듯 이 flag는 splice의 pipe operation을 nonblocking으로 만들지만, 상대 file descriptor 자체가 block할 수 있으면 전체 call이 여전히 block할 수 있다. 따라서 fd의 `O_NONBLOCK`, API flag, underlying filesystem/device behavior를 별도로 확인해야 한다. Backpressure 대응의 목표는 “절대 block하지 않기”가 아니라 **어디에서 기다릴지 명시하고, 기다리는 동안 이미 확보한 page references와 pipe occupancy를 bounded하게 유지하는 것**이다.

## CHAPTER 07 · Pipe는 byte array가 아니라 제한된 descriptor ring과 backing pages의 결합이다

Pipe capacity를 단순히 “64 KiB짜리 buffer”라고 외우면 zero-copy behavior를 놓친다. Linux pipe는 내부적으로 여러 buffer entries가 backing pages 또는 page ranges를 가리키는 구조를 사용하며, capacity는 kernel/version/resource limit에 따라 달라질 수 있고 `F_GETPIPE_SZ`로 조회할 수 있다. 같은 byte 수라도 fragmentation과 buffer entry 사용 패턴에 따라 descriptor pressure가 달라질 수 있다. 큰 pipe를 만든다고 throughput이 자동으로 오르지도 않는다. Pipe가 커질수록 producer가 consumer보다 앞서 달릴 수 있는 거리와 동시에 retained memory/reference의 상한도 커진다. 특히 `tee()`로 같은 backing data를 여러 branches가 참조하면 느린 branch 하나가 page release를 오래 지연시킬 수 있다. 운영 설계에서는 pipe size를 magic constant로 고정하지 말고 **burst 흡수량, consumer latency, memory budget, fairness**의 함수로 정해야 한다. Capacity는 성능 knob이면서 동시에 얼마나 많은 in-flight ownership을 허용할지 정하는 safety limit다.

## CHAPTER 08 · splice()의 핵심 제약은 적어도 한쪽 endpoint가 pipe여야 한다는 것이다

`splice(fd_in, ..., fd_out, ..., size, flags)`는 arbitrary fd 두 개 사이를 언제나 직접 연결하는 범용 “kernel memcpy”가 아니다. Linux에서는 적어도 한 endpoint가 pipe여야 하며, file→socket 전송을 구성하려면 흔히 file→pipe와 pipe→socket의 두 단계가 필요하다. 이 제약 덕분에 pipe가 reference handoff의 명시적 staging object가 된다. Source가 seekable file인지 pipe인지에 따라 `off_in` 규칙도 달라진다. Pipe endpoint에는 offset pointer를 주면 안 되고, seekable file에 explicit offset pointer를 주면 underlying fd offset을 그대로 둔 채 pointed value만 전진한다. `SPLICE_F_MOVE`라는 이름 때문에 page ownership이 항상 물리적으로 “이동”한다고 믿어서도 안 된다. 현재 manual contract상 이 flag는 성공적인 move를 보장하는 기능이 아니라 hint이며 오래전부터 실질적으로 no-op으로 취급돼 왔다. Correctness는 flag 이름이 아니라 **return value와 documented ownership/lifetime contract**에 기대야 한다.

## CHAPTER 09 · file→pipe splice에서 page cache hit는 bytes 대신 page reference를 staging할 수 있다

Buffered file data가 page cache에 존재할 때 zero-copy path의 이점은 userspace bounce buffer를 만들지 않고 pipe가 해당 cached data를 나타내는 page range를 참조할 수 있다는 데 있다. 여기서 page cache lookup과 readahead, page fault, storage I/O가 사라지는 것은 아니다. Cache miss라면 backing storage에서 page를 채워야 하고, 그 latency는 그대로 source side critical path에 들어간다. 또한 reference가 살아 있는 동안 reclaim이나 invalidation이 원하는 시점에 즉시 끝나지 못할 수 있어 “copy를 안 했으니 memory를 안 쓴다”는 계산도 틀린다. Pipe가 보관하는 것은 원본 file이라는 추상 객체가 아니라 특정 시점에 얻은 backing memory에 대한 kernel-managed reference다. 하지만 application-level consistency는 별도 문제다. 같은 file range를 다른 writer가 수정하거나 truncate할 수 있다면 API contract가 snapshot isolation을 주는지 확인해야 하며, 그렇지 않다면 mutation을 serialization해야 한다. **Page lifetime safety와 file-version consistency를 같은 것으로 취급하지 않는 것**이 핵심이다.

## CHAPTER 10 · pipe→socket splice가 끝나도 syscall return 시점에 page lifetime이 끝났다고 볼 수 없다

Pipe에서 socket으로 data를 넘길 때 kernel은 socket send queue가 backing page를 참조하도록 연결할 수 있다. `splice()`가 양수로 반환됐다는 사실은 pipe 관점에서 그 bytes가 소비됐다는 뜻이지, network stack이 그 bytes에 대한 모든 reference를 이미 해제했다는 뜻은 아니다. TCP는 segmentation, qdisc/NIC queueing, retransmission 가능성, ACK 처리 같은 후속 lifetime을 갖는다. 구현은 상황에 따라 copy fallback을 선택할 수도 있으므로 모든 전송이 동일한 reference lifetime을 갖는다고 가정할 수 없다. Caller에게 중요한 contract는 **syscall completion과 source-memory reuse permission을 분리**하는 것이다. 특히 원본 file region을 동시에 rewrite/truncate하는 writer가 있다면 “socket으로 넘겼으니 이제 안전하다”는 판단은 위험하다. Linux `sendfile()` 문서도 zero-copy를 지원하는 socket/pipe로 보낼 때 transferred file portion을 downstream이 소비할 때까지 수정하지 말라고 경고한다. 성능 최적화가 data-versioning protocol을 대신해주지 않는다.

## CHAPTER 11 · Socket send queue는 ownership을 syscall stack 밖으로 연장한다

보통의 blocking write를 배운 뒤에는 `write()`가 반환되면 kernel이 buffer 내용을 독립적으로 소유한다고 생각하기 쉽다. Copy 기반 send라면 user buffer를 즉시 재사용해도 되는 이유가 kernel이 필요한 bytes를 복사했기 때문이다. 그러나 zero-copy 경로는 바로 그 copy를 피하므로 source backing memory와 network queue의 lifetime이 겹칠 수 있다. TCP send queue는 아직 전송되지 않은 data뿐 아니라 재전송 가능성을 위해 보존해야 하는 data를 포함할 수 있고, skb/fragments가 page reference를 쥔 채 syscall 이후에도 살아 있을 수 있다. Local send completion, NIC DMA completion, peer ACK, peer application read는 모두 다른 사건이다. 어느 사건이 source reference release 조건인지는 API와 구현 path에 따라 다르므로 임의로 하나를 선택하면 안 된다. **Queue lifetime을 external completion contract 없이 추측하지 말고, 해당 API가 제공하는 immutability 규칙과 completion signal을 따르는 것**이 안전하다.

## CHAPTER 12 · ACK는 remote application consumption과 같지 않고, zero-copy completion도 transmit completion과 같지 않다

TCP ACK는 peer TCP stack이 bytes를 수신해 sequence space에서 인정했다는 transport-level 신호이지 remote application이 `read()`로 처리했다는 증거가 아니다. 반대로 Linux `MSG_ZEROCOPY` completion notification은 kernel이 caller의 user pages에 대한 hold를 놓아 buffer를 다시 수정해도 된다는 ownership 신호이지 packet이 wire에서 완전히 전송됐거나 peer가 ACK했다는 보장이 아니다. Kernel documentation은 fallback copy가 발생한 경우 completion이 실제 transmission보다 먼저 올 수도 있음을 명시한다. 이 구분은 `sendfile()`/`splice()`를 이해할 때도 유용하다. “system call이 끝남”, “source memory가 재사용 가능함”, “local network stack이 전송 완료함”, “peer transport가 수신함”, “peer application이 처리함”을 서로 다른 milestones로 모델링해야 한다. **Zero-copy는 completion의 의미를 더 세분화하는 기술**이며, 단일 `done` boolean으로 압축하면 ownership bug와 delivery-assumption bug가 동시에 생긴다.

## CHAPTER 13 · Backpressure chain은 socket에서 pipe를 거쳐 file reader까지 역방향으로 전파되어야 한다

네트워크가 느려 socket send queue가 차면 pipe→socket drain이 멈춘다. Pipe가 계속 차오르면 결국 file→pipe refill도 멈춰야 한다. 이 역방향 전파가 없으면 application이 별도의 pipe를 계속 만들거나 source ranges를 무제한 예약해 memory/reference retention을 확대한다. Proper bounded pipeline은 downstream credit이 생길 때만 upstream을 전진시킨다. 예를 들어 pipe에 미전송 data가 남아 있으면 socket drain을 우선하고, high-water mark 이상에서는 file refill을 중단하며, low-water mark 아래로 내려간 뒤에만 다시 source를 읽는다. 여러 connection을 한 event loop에서 처리한다면 한 socket의 큰 writable burst가 다른 connection을 굶기지 않도록 per-iteration byte budget도 필요하다. Backpressure는 단순 성능 최적화가 아니라 **in-flight page ownership의 총량을 제한하는 memory-safety mechanism**이다. “copy가 없으니 buffer memory가 없다”는 오해가 가장 위험한 이유가 바로 여기에 있다.

## CHAPTER 14 · Nonblocking pipeline은 readiness와 progress를 함께 보지 않으면 livelock에 빠진다

`epoll`에서 EPOLLOUT이 왔다고 socket이 원하는 전체 chunk를 받아준다는 보장은 없다. Writable은 “어느 정도 progress 가능”이라는 힌트일 뿐이며, 그 사이 다른 sender나 congestion state가 바뀌면 `splice()`가 다시 짧게 끝나거나 EAGAIN을 반환할 수 있다. 반대로 edge-triggered readiness를 쓰면서 한 번만 drain하고 남은 pipe data를 방치하면 새 edge가 오지 않아 stall할 수 있다. Loop는 매 호출 후 실제 bytes를 반영하고, progress가 계속되는 동안 bounded하게 drain한 뒤, EAGAIN에서만 readiness wait로 돌아가야 한다. Source side도 동일하다. Regular file은 pollability가 socket과 다르고 storage fault가 내부적으로 block할 수 있으므로 “event-driven이면 어떤 I/O도 block하지 않는다”는 전제가 성립하지 않는다. **Readiness는 ownership transfer의 가능성을 알려주는 신호이지 transfer 자체가 아니다.** State machine의 진실은 언제나 return byte count와 현재 queue occupancy다.

## CHAPTER 15 · tee()는 bytes를 복제하지 않고 references를 복제하므로 fan-out의 느린 소비자가 lifetime을 지배한다

`tee(pipeA, pipeB, n, flags)`는 input pipe의 data를 소비하지 않은 채 output pipe에도 같은 data를 보이게 할 수 있다. Manual이 설명하듯 실제 data copy 대신 backing buffer에 대한 reference를 추가하는 방식이 가능하다. 이 특성은 logging, hashing, replication 같은 fan-out을 값싸게 만들지만, ownership graph에는 branch가 생긴다. Fast network branch가 이미 data를 다 보냈어도 slow audit branch가 같은 backing page reference를 유지하면 page lifetime은 끝나지 않는다. Branch 수가 늘어날수록 byte copy cost는 낮아도 descriptor/reference accounting과 backpressure coupling이 커질 수 있다. 한 branch가 영구 정체되면 다른 branch의 source cleanup까지 늦춰질 수 있으므로 timeout, branch drop policy, bounded pipe capacity가 필요하다. **Zero-copy fan-out의 비용은 bytes × branches가 아니라 references × lifetime**으로 봐야 하며, 느린 branch의 tail latency가 retained-memory peak를 결정한다.

## CHAPTER 16 · vmsplice()는 userspace memory를 pipe에 연결하므로 buffer reuse 규칙이 가장 직접적으로 드러난다

`vmsplice()`는 iovec으로 지정한 userspace ranges를 pipe와 연결하는 인터페이스다. User→pipe 방향에서 진짜 splice semantics를 제공할 수 있고, pipe→user 방향은 현재 실제로 copy가 수행된다는 점도 문서에 명시돼 있다. 핵심 위험은 syscall이 반환됐다고 원래 userspace buffer가 자동으로 private copy가 됐다고 생각하는 것이다. Pipe와 downstream이 아직 해당 pages를 참조할 수 있는 동안 caller가 같은 memory를 수정하면 전송 중인 data가 바뀌는 형태의 self-corruption을 만들 수 있다. 따라서 application은 각 iovec range에 “FREE → IN_FLIGHT → REUSABLE” 같은 ownership state를 두고 downstream release condition 전에는 allocator/free-list에 돌려보내지 않아야 한다. 이 discipline은 buffer pool을 쓸 때 특히 중요하다. **Zero-copy user buffer는 함수 인자가 아니라 일시적으로 공유되는 storage object**이며, reuse 시점을 call stack 종료와 연결하면 안 된다.

## CHAPTER 17 · SPLICE_F_GIFT는 optimization flag가 아니라 ownership contract를 더 강하게 바꾸는 요청이다

`vmsplice()`의 `SPLICE_F_GIFT`는 user pages를 kernel에 gift한다는 의미를 갖고, manual은 application이 이후 그 memory를 수정하면 안 된다고 매우 강하게 규정한다. 또한 page alignment와 length alignment 같은 조건이 요구된다. 이 flag를 “복사 한 번 더 줄이는 turbo option”처럼 켜는 것은 위험하다. Allocator가 동일 virtual range를 재사용하거나, object destructor가 memory를 scrub하거나, 다른 thread가 bookkeeping field 하나를 바꾸는 것만으로도 kernel이 참조하는 data와 application이 생각하는 data가 갈라질 수 있다. Gift 여부와 별개로 zero-copy design은 buffer ownership을 코드 구조에 드러내야 하지만, GIFT는 특히 **caller's mutation right 자체를 포기하는 contract**로 다뤄야 한다. Library wrapper는 raw pointer만 받기보다 move-only token, in-flight handle, explicit completion callback처럼 재사용을 구조적으로 막는 API를 제공하는 편이 안전하다.

## CHAPTER 18 · Page alignment와 segment boundaries는 성능뿐 아니라 “무엇을 참조하는가”를 결정한다

Page-based zero-copy는 logical message boundary와 physical page boundary가 일치하지 않을 수 있다. 한 page의 중간 2 KiB만 전송하면 descriptor는 page reference에 offset/length를 함께 보관해야 하고, 같은 page의 나머지 bytes는 다른 logical object가 사용할 수 있다. 이때 application이 “내가 보낸 2 KiB만 immutable하면 된다”고 생각해도 underlying mechanism이나 optimization 조건은 full-page alignment를 선호할 수 있으며, `SPLICE_F_GIFT`처럼 아예 aligned page 단위를 요구하는 경우도 있다. Small fragments가 많으면 page reference 수, pipe buffer entries, scatter-gather segments가 늘어 NIC/device limits나 metadata overhead에 부딪힐 수 있다. 따라서 allocator는 payload size만 맞추는 것이 아니라 **alignment, lifetime cohort, fragmentation**을 함께 고려해야 한다. 같은 completion 시점에 풀릴 data를 같은 pages에 묶으면 ownership tracking이 단순해지고, 서로 다른 lifetime의 objects를 한 page에 섞으면 한 작은 tail 때문에 page 전체 retention이 길어질 수 있다.

## CHAPTER 19 · Truncation race에서 page가 살아 있다는 사실은 file snapshot이 보존된다는 뜻이 아니다

Zero-copy source file을 다른 thread/process가 `truncate()`하는 상황은 page lifetime과 file semantic state가 충돌하는 대표 사례다. Pipe나 socket이 기존 backing page에 reference를 갖고 있다면 physical storage object가 즉시 재사용되지 않을 수 있지만, application이 기대하는 “전송 시작 시점의 file bytes 전체가 그대로 전달된다”는 snapshot guarantee는 별도 문제다. Truncate는 inode size와 page-cache invalidation 범위를 바꾸고, 아직 source range를 읽지 않은 subsequent splice/sendfile 호출의 EOF 위치도 바꿀 수 있다. 이미 staged된 bytes와 아직 staged되지 않은 bytes가 서로 다른 file generation을 반영하면 한 logical response 안에 세대가 섞일 수 있다. 따라서 static asset server가 zero-copy를 쓴다면 **open fd만 잡아두는 것으로 content immutability를 증명하지 말고**, rename-based immutable generations, content-addressed files, writer serialization 같은 publish protocol로 source version을 고정해야 한다.

## CHAPTER 20 · Concurrent overwrite는 같은 file size에서도 전송 세대를 섞을 수 있다

Truncate가 없어도 in-place overwrite는 위험하다. File size가 그대로라서 EOF는 변하지 않지만, 아직 page cache에서 읽지 않은 range와 이미 pipe/socket에 연결된 range가 서로 다른 시점의 bytes를 볼 수 있다. 특히 large file을 여러 splice chunk로 전송하면 앞 chunk는 old version, 뒤 chunk는 new version이 되는 torn response가 가능하다. Reference counting은 use-after-free를 막는 memory-lifetime mechanism이지 application-level multi-page atomicity protocol이 아니다. “page가 reference됐으니 그 page 내용도 불변”이라는 보장은 API 문서가 주지 않는 한 만들어내면 안 된다. Safe publishing에서는 writer가 temporary file에 새 version을 완성하고 durability 조건을 만족한 뒤 atomic namespace switch로 새 readers만 새 inode/generation을 열게 만드는 방식이 더 명확하다. **Zero-copy reader와 in-place mutable writer를 동시에 허용하려면 별도 version/locking protocol이 필요**하고, 그 비용까지 포함해 최적화 가치를 계산해야 한다.

## CHAPTER 21 · Hole punching과 sparse file 변화도 source range의 의미를 바꾼다

Sparse file에서 logical range는 physical block이 없어서 zero로 읽힐 수도 있고, filesystem operation이 hole을 만들거나 제거할 수도 있다. Zero-copy API는 caller가 “이 offset부터 n bytes”를 요청했다는 사실만으로 그 range가 영원히 같은 physical blocks에 묶여 있다고 약속하지 않는다. Concurrent `fallocate()` hole-punch, extent remap, reflink/COW 동작과 겹치면 아직 staging되지 않은 range의 backing semantics가 달라질 수 있다. Filesystem은 내부 locking과 reference counting으로 memory safety와 metadata consistency를 지키지만, application response가 한 immutable version이어야 한다는 요구까지 대신 구현해주지는 않는다. 그래서 checksum이나 ETag를 전송 전후에 계산해도 중간 세대 혼합을 놓칠 수 있다. Correct design은 **source generation을 먼저 고정하고 그 generation 안에서 zero-copy를 적용**한다. Zero-copy를 먼저 적용한 뒤 race가 생기면 checksum으로 감지하겠다는 방식은 이미 잘못된 bytes를 네트워크에 보낸 뒤의 사후 검출에 불과하다.

## CHAPTER 22 · sendfile()의 “source를 수정하지 말라”는 제약은 성능 팁이 아니라 data integrity contract다

최신 Linux man-pages는 output이 zero-copy를 지원하는 socket 또는 pipe일 때 transferred portion의 source file을 downstream이 소비할 때까지 수정하지 않도록 caller에게 책임을 둔다. 이 문장은 kernel이 source mutation을 자동 snapshot/copy-on-write로 격리해줄 것이라고 기대하면 안 된다는 뜻이다. Static file server에서 deployment가 같은 pathname의 file을 in-place rewrite하면 old connection이 아직 전송 중인 bytes를 영향을 받을 수 있으므로, 새 file을 다른 inode로 만든 뒤 rename하고 기존 open fd generation은 자연스럽게 drain시키는 배포 패턴이 더 안전하다. Cache layer도 마찬가지다. “hot file이니까 sendfile”을 선택하기 전에 **immutability window가 socket lifetime보다 길게 유지되는가**를 확인해야 한다. 성능 API가 요구하는 data contract를 application architecture가 지킬 수 없으면, copy 기반 경로가 오히려 정확성과 격리를 사는 합리적인 선택일 수 있다.

## CHAPTER 23 · close(fd)는 이미 시작된 zero-copy transfer를 취소하거나 reference를 회수하는 명령이 아니다

File descriptor는 process fd table의 handle일 뿐이고, in-flight kernel operation이나 pipe/socket object가 별도 reference를 확보했다면 `close()`는 그 reference graph 전체를 즉시 파괴하지 않는다. P71에서 다룬 것처럼 close와 fd number 재사용은 특히 비동기·멀티스레드 코드에서 ABA 혼동을 만든다. Zero-copy pipeline에서도 source fd를 닫았다고 pipe에 이미 들어간 data가 사라지는 것이 아니며, socket fd를 다른 thread가 닫는 것과 이미 queue된 data의 release timing은 별개다. 더 위험한 패턴은 close 후 같은 integer fd 번호가 새 file/socket에 재사용됐는데 stale retry state가 그 번호로 다음 `splice()`를 호출하는 경우다. 그러면 old transfer의 cursor와 new object가 결합될 수 있다. **Transfer state는 raw fd integer가 아니라 object generation과 함께 관리**해야 하며, cancellation은 “fd를 닫았다”가 아니라 어떤 in-flight references를 더 이상 생성하지 않고 existing references가 drain됐는지까지 확인하는 protocol이어야 한다.

## CHAPTER 24 · Socket teardown은 성공적으로 queue된 bytes의 fate와 page release를 분리해서 봐야 한다

Peer reset, local shutdown, timeout, process cancellation이 발생하면 socket queue의 data가 끝까지 전달되지 못할 수 있다. 그렇다고 syscall에서 과거에 성공한 bytes를 application이 자동으로 “미전송”으로 되돌릴 수 있는 것은 아니다. TCP API가 caller에게 알려주는 것은 local enqueue progress와 후속 error이며, 정확히 어느 application-level message가 peer에서 처리됐는지는 별도 acknowledgment protocol 없이는 알 수 없다. Page lifetime 관점에서는 teardown이 socket-owned references를 결국 release해야 하지만, 그 시점과 caller가 delivery outcome을 확정하는 시점은 다르다. Retry를 다른 connection에서 수행할 경우 message idempotency/sequence를 사용해야 중복을 막을 수 있다. **Memory ownership completion과 business delivery completion을 한 이벤트로 합치면 안 된다.** Zero-copy는 page release를 더 명시적으로 고민하게 만들 뿐, distributed delivery ambiguity를 해결하지 않는다.

## CHAPTER 25 · Retransmission은 같은 logical bytes의 network lifetime을 syscall보다 훨씬 길게 만든다

TCP는 loss가 발생하면 이미 한 번 NIC로 내려간 data를 다시 전송할 수 있어야 한다. 그래서 local syscall이 끝난 뒤에도 protocol stack이 data representation을 보존할 이유가 있다. Zero-copy path가 backing page references를 활용한다면 이 lifetime 동안 source data stability가 중요해지고, copy fallback path라면 kernel-owned copy가 그 역할을 할 수 있다. Congestion, RTO/RACK recovery, receiver window, qdisc backlog 같은 요소가 길어지면 retained data lifetime도 tail에서 크게 늘어날 수 있다. 평균 RTT가 짧다는 이유로 “100 ms 뒤면 buffer 재사용 가능” 같은 timer-based ownership release를 만들면 rare loss에서 corruption이 발생한다. Buffer release는 시간 추정이 아니라 **documented completion condition 또는 reference-holding subsystem의 확정 신호**에 묶어야 한다. 성능 테스트도 clean LAN만 쓰지 말고 loss·reordering·small receive window·peer stall을 넣어 retention tail을 확인해야 한다.

## CHAPTER 26 · MSG_ZEROCOPY는 sendfile/splice와 다른 source를 쓰지만 ownership 문제를 더 명시적으로 보여준다

`MSG_ZEROCOPY`는 userspace buffer에서 socket으로 보내는 send 계열 호출에서 copy avoidance를 요청하는 기능이며, `SO_ZEROCOPY` opt-in과 error queue completion protocol을 사용한다. Kernel documentation은 page pin/accounting과 completion notification overhead 때문에 작은 write에서는 copy가 더 쌀 수 있다고 설명한다. 더 중요한 점은 flag가 “반드시 zero copy”를 보장하지 않는다는 것이다. Device scatter-gather 제약이나 checksum 등의 이유로 deferred copy가 일어날 수 있고, 그 경우에도 kernel은 user pages에 대한 hold를 놓았다는 completion을 보낸다. Completion은 call sequence range로 coalesce될 수 있고 retransmission/socket teardown에서 순서가 달라질 수도 있다. 이 사례는 `sendfile()`/`splice()`에도 적용할 사고방식을 준다. **zero-copy는 구현 경로의 가능성이고, caller가 의존해야 하는 것은 ownership/completion contract**다. “이 API 이름에 zero가 있으니 copy가 없었다”는 관측 없는 단정은 금물이다.

## CHAPTER 27 · Zero-copy가 느려지는 경우는 copy 비용보다 reference 관리 비용이 커질 때다

Copy를 없애면 항상 빨라진다는 결론은 workload size를 무시한다. Small payload에서는 syscall/setup, page lookup, descriptor 생성, reference accounting, completion 처리, cache miss가 memcpy보다 비쌀 수 있다. `MSG_ZEROCOPY` 문서도 대략 큰 write에서 이득이 나타난다고 경고하며 threshold는 하드웨어와 workload에 따라 달라진다. `splice()` pipeline은 두 syscall과 pipe bookkeeping이 필요할 수 있고, TLS/userspace transformation처럼 어차피 bytes를 읽고 바꿔야 하는 stage가 있다면 reference-only path의 이점이 줄어든다. 반대로 multi-GB static file, high-throughput proxy, large storage stream처럼 copy bandwidth와 cache pollution이 병목이면 이득이 커질 수 있다. 평가는 payload size bucket별 CPU cycles/byte, syscalls/GB, cache misses, throughput, p99 latency, retained pages, context switches를 비교해야 한다. **Zero-copy는 API 선택이 아니라 측정으로 증명해야 하는 workload-specific optimization**이다.

## CHAPTER 28 · Transform이 필요한 data는 zero-copy chain이 자연스럽게 끊길 수 있다

Compression, application-level encryption, content rewriting, framing 변경처럼 bytes 자체를 변환해야 하면 CPU가 source를 읽고 destination representation을 생성해야 한다. 이때 “zero-copy를 끝까지 유지”하려고 복잡한 kernel feature를 억지로 조합하면 correctness와 portability 비용이 더 커질 수 있다. TLS도 구현 위치에 따라 userspace plaintext→ciphertext transformation 때문에 copy/새 buffer가 필요할 수 있고, kernel TLS나 device offload 같은 별도 기능이 있을 때만 다른 경로가 가능하다. 중요한 것은 pipeline을 `source acquisition`, `transformation`, `transport` 단계로 나눠 어느 경계에서 immutable representation이 생기는지 정하는 것이다. Transformation 이후 buffer가 immutable하고 large enough라면 그 이후 구간만 zero-copy-like handoff를 최적화할 수 있다. **전체 요청을 zero-copy/아님으로 이분법화하지 말고 구간별 copy graph를 그려야** 실제 병목과 ownership contract를 동시에 파악할 수 있다.

## CHAPTER 29 · Error handling은 “몇 bytes가 누구에게 남아 있는가”를 보존해야 한다

File→pipe 단계가 성공한 뒤 pipe→socket에서 `EPIPE`, `ECONNRESET` 같은 fatal error가 나면 pipe에 아직 data가 남을 수 있다. 이 tail은 source에서 이미 소비된 bytes이지만 sink에는 commit되지 않은 bytes다. 단순 cleanup에서 pipe fd를 닫으면 memory references는 release되겠지만 application delivery state는 실패로 기록해야 한다. 반대로 error가 transient EAGAIN이면 tail을 버리면 안 되고 readiness 후 이어서 drain해야 한다. Error taxonomy는 `retry same state`, `abort and release staging`, `restart on new sink with application deduplication`, `source fatal`처럼 ownership action과 결합돼야 한다. Metrics에도 errno count만 남기지 말고 `source_bytes_acquired`, `sink_bytes_accepted`, `staged_bytes_dropped`, `retry_bytes`를 남기면 사건 후 유실/중복 가능성을 계산할 수 있다. **Zero-copy error path의 핵심 데이터는 error code보다 outstanding byte ledger**다.

## CHAPTER 30 · Cancellation은 source read 중단과 in-flight reference drain을 두 단계로 나눠야 한다

사용자가 download를 취소하거나 timeout이 나면 가장 먼저 해야 할 일은 upstream에서 새 pages를 더 가져오지 않는 것이다. 그러나 이미 pipe/socket에 들어간 references는 별도 cleanup path를 통해 release돼야 한다. Immediate fd close를 cancellation primitive로 사용할 수는 있지만, multi-threaded code에서 어떤 thread가 해당 fd를 공유하는지, close 후 number reuse가 가능한지, completion callback이 뒤늦게 도착하는지까지 고려해야 한다. `MSG_ZEROCOPY`처럼 completion queue가 있는 API는 teardown 시 notification order가 평상시와 다를 수 있으므로 outstanding ranges를 generation별로 정리해야 한다. Splice pipeline은 pipe occupancy가 cancellation 당시 얼마였는지 기록하고 pipe object를 폐기하면서 source/sink cursors를 terminal state로 전환하는 편이 명확하다. **Cancellation success는 syscall을 멈췄다는 뜻이 아니라 더 이상 참조를 생성하지 않고 기존 참조가 회수될 경로가 확정됐다는 뜻**으로 정의해야 resource leak을 막을 수 있다.

## CHAPTER 31 · Observability는 throughput보다 queue와 ownership age를 보여줘야 한다

Zero-copy 장애는 “CPU는 낮은데 요청이 멈춤”처럼 보일 수 있다. Socket send queue가 막히고 pipe occupancy가 high-water에 붙어 있으며 source refill은 중단됐는데, 단순 request latency와 bytes/sec만 보면 원인이 network인지 source인지 구분하기 어렵다. 최소한 connection별 staged bytes, pipe capacity/occupancy, source/sink cursor gap, EAGAIN count, writable wait time, socket send-queue depth, zero-copy completion lag, fallback-copy count를 관측할 수 있어야 한다. Page references가 오래 살아 reclaim pressure를 만들면 system memory/reclaim metrics와도 연결해야 한다. 특히 p50보다 “가장 오래 잡힌 in-flight range age”가 leak/stall을 빠르게 드러낸다. Tracing을 넣을 때는 payload 자체를 기록해 secret을 복제하지 말고 range id, file generation, offset, length, state transition, timestamp를 남긴다. **Zero-copy telemetry는 bytes의 내용보다 ownership transition의 시간축을 기록하는 것이 핵심**이다.

## CHAPTER 32 · File generation identity가 없으면 같은 pathname이 다른 bytes를 가리키는 순간을 잡지 못한다

운영 중 deployment가 `/var/www/app.bin`을 교체할 때 pathname은 같아도 inode/content generation은 달라질 수 있다. 이미 open한 fd는 old object를 가리키고 새 open은 new object를 가리킬 수 있어, 이것 자체는 안전한 immutable-generation 패턴에 도움이 된다. 문제는 logging에 pathname만 남기면 어떤 generation을 전송했는지 나중에 구분할 수 없다는 점이다. Size, inode identity, mtime만으로도 collision/변경을 완전히 배제하지 못할 수 있으므로 content hash나 deployment build id를 함께 기록하는 편이 강하다. Source mutation을 허용해야 한다면 transfer 시작 시 generation/version token을 확보하고 모든 chunk가 동일 generation 조건을 만족하는지 검증해야 한다. **Zero-copy correctness의 source identity는 “파일 이름”이 아니라 전송 동안 불변으로 약속된 content generation**이다. 그래야 truncation/overwrite race가 발생했을 때 어느 contract가 깨졌는지 증명할 수 있다.

## CHAPTER 33 · Security 관점에서는 immutable source와 range authorization을 별도로 검증해야 한다

Zero-copy API가 path parsing이나 authorization을 우회해주지는 않는다. Request가 허용된 file range를 결정한 뒤 fd를 얻고, 그 fd와 offset/count가 실제 authorized object와 일치하는지 고정해야 한다. Path를 검증한 후 다시 pathname으로 열면 rename/symlink race가 생길 수 있고, range 계산 overflow가 있으면 의도하지 않은 bytes를 전송할 수 있다. Long-lived fd cache를 쓰면 file generation이 policy 변경 이후에도 남을 수 있으므로 cache invalidation과 access revocation semantics를 정해야 한다. `sendfile()`처럼 kernel이 직접 bytes를 socket으로 넘기면 application이 payload inspection을 거치지 않을 수 있어 DLP/redaction 요구가 있는 데이터에는 부적합할 수도 있다. **Copy avoidance는 security checks를 생략하는 권한이 아니라, checks가 끝난 immutable object/range를 더 효율적으로 이동하는 방법**이다. Authorization identity와 content generation identity를 같은 transfer record에 묶어야 감사가 가능하다.

## CHAPTER 34 · Fault injection은 happy-path throughput보다 partial progress와 lifetime bug를 더 잘 찾는다

Zero-copy test를 localhost에서 큰 file 하나 보내고 checksum만 맞추는 것으로 끝내면 가장 위험한 state가 드러나지 않는다. Pipe capacity를 작게 만들고, receiver를 의도적으로 늦추고, socket을 nonblocking으로 바꾸고, 중간에 peer reset을 넣고, source를 truncate/overwrite하려는 writer를 경쟁시키고, small/unaligned segments를 섞어야 한다. `tee()` branch 하나를 멈춰 fan-out retention도 확인해야 한다. `vmsplice()` buffer pool은 completion 전 재사용을 일부러 시도해 guard/assert가 잡는지 본다. Network loss/reordering을 넣어 socket queue lifetime tail이 늘어날 때 timer-based release가 없는지도 검증한다. 각 fault에서 expected invariant는 “유실/중복 없음”, “source generation 혼합 없음”, “outstanding range가 종료 후 0”, “fatal error 후 references가 bounded time 안에 회수 경로로 들어감”처럼 정의한다. **성능 최적화 검증은 throughput benchmark와 adversarial ownership test를 분리해서 둘 다 통과해야 한다.**

## CHAPTER 35 · API 선택은 copy 횟수가 아니라 source type·mutation·completion 요구로 결정한다

Immutable regular file을 socket으로 보내고 transformation이 없다면 `sendfile()`이 가장 단순한 후보가 될 수 있다. Arbitrary producer/consumer를 pipe staging으로 연결하거나 fan-out이 필요하면 `splice()`/`tee()`가 더 적합할 수 있다. Userspace-generated large immutable pages를 pipe에 직접 연결하려면 `vmsplice()`를 검토할 수 있지만 buffer ownership discipline이 훨씬 엄격해진다. User buffer를 socket으로 직접 보내면서 completion notification을 처리할 수 있다면 `MSG_ZEROCOPY`가 별도 선택지다. 반대로 payload가 작거나 자주 수정되거나 transform이 필수이거나 source immutability를 보장할 수 없다면 plain `read/write` 또는 `sendmsg` copy path가 더 단순하고 빠를 수도 있다. Portability 요구가 높으면 Linux-specific splice family의 운영 비용도 고려해야 한다. **“zero-copy를 최대화”가 목표가 아니라 요구되는 correctness contract를 가장 단순하게 만족하면서 측정된 병목을 줄이는 API를 선택**해야 한다.

## CHAPTER 36 · Zero-copy correctness의 최종 proof는 byte range와 reference lifetime을 함께 닫는 것이다

한 zero-copy pipeline을 승인하려면 최소한 다음 질문에 답할 수 있어야 한다. Source content generation은 transfer 동안 불변인가. Source cursor, pipe occupancy, sink cursor는 partial return마다 정확히 갱신되는가. EAGAIN에서 busy-loop하지 않고 readiness로 backpressure가 전파되는가. Pipe/fan-out capacity가 retained references를 bounded하게 제한하는가. `vmsplice()` user pages는 downstream release 전에 재사용되지 않는가. Truncate·overwrite·hole-punch와 같은 source mutation이 serialization되는가. Socket queue가 syscall 이후 pages를 붙잡을 수 있음을 코드가 전제로 삼는가. Cancellation/teardown에서 outstanding ranges가 결국 release되는가. Completion signal을 remote delivery와 혼동하지 않는가. Fault injection 후 duplicate/loss/generation-mix가 0인지 측정했는가. **Zero-copy는 “복사를 없앤 코드”가 아니라, bytes를 복사하는 대신 ownership transfer를 증명하는 코드다.** 성능 이득은 그 proof 위에서만 안전하게 유지된다.
