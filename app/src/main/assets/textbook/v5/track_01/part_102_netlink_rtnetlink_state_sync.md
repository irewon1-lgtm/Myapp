# PART 102 · Netlink Control Plane and rtnetlink State Synchronization — sequence, ACK, multipart dump, notification loss, reconciliation

Netlink를 “kernel과 통신하는 socket” 정도로만 이해하면 control plane에서 가장 위험한 부분인 **request correlation, partial dump, multicast loss, object identity drift, namespace ownership, desired-state reconciliation**이 빠진다. Userspace daemon은 link·address·route·neighbor·rule 같은 kernel network state를 Netlink message로 읽고 바꾸지만, request 성공과 최종 operational state가 항상 같은 순간에 확정되는 것은 아니다. Multicast notification은 authoritative log가 아니고, dump도 진행 중 concurrent mutation을 만날 수 있다. 따라서 신뢰할 수 있는 controller는 message parser보다 더 큰 state machine을 가져야 한다. 어느 socket/namespace에서 어떤 sequence를 보냈고, ACK와 multipart terminator를 받았는지, notification gap 뒤 무엇을 다시 dump했는지를 증거로 남겨야 한다.

## CHAPTER 01 · Netlink는 syscall 하나의 request/response가 아니라 asynchronous message transport다

`ioctl()`처럼 호출 하나가 kernel object 하나를 즉시 바꾼다고 생각하면 Netlink의 concurrency를 놓친다. Userspace는 datagram-oriented socket으로 message를 보내고, kernel은 ACK, error, multipart reply, multicast notification을 서로 다른 시점에 전달할 수 있다. 같은 socket에 여러 outstanding request가 있으면 receive 순서만으로 어느 reply가 어느 request에 대응하는지 판단할 수 없다. 또한 notification은 내가 보낸 request와 무관하게 끼어들 수 있다. 따라서 client는 **message header의 sequence, sender identity, message type을 기준으로 correlation**하고 application state를 갱신해야 한다. Blocking `recvmsg()`가 한 번 성공했다는 사실은 transaction completion을 의미하지 않으며, expected terminal condition을 별도로 추적해야 한다.

## CHAPTER 02 · Port ID는 process PID와 동일한 개념이 아니라 Netlink endpoint identity다

Netlink socket은 userspace endpoint를 식별하기 위해 port ID를 사용한다. 흔히 첫 socket에서 process PID와 같은 값이 배정될 수 있어 둘을 동일한 identity로 오해하기 쉽지만, 한 process가 여러 Netlink socket을 열면 모든 socket이 같은 PID를 endpoint ID로 쓸 수 없다. Kernel 또는 library가 고유 port ID를 배정할 수 있고, socket lifetime이 끝나면 그 identity도 종료된다. Request log에 OS process PID만 남기면 어느 socket에서 message가 나갔는지 복원하기 어렵다. **Process identity와 Netlink endpoint identity를 분리**하고 socket create/close, local port ID, protocol family를 함께 기록해야 stale reply나 reused endpoint 문제를 추적할 수 있다.

## CHAPTER 03 · Sequence number는 reply correlation key이지 global ordering clock이 아니다

Netlink request header의 sequence는 userspace가 outstanding request를 식별하기 위해 사용하는 값이다. 같은 socket에서 monotonically 증가시키면 debugging이 쉬워지지만 sequence 자체가 kernel 전체의 serialization order나 object generation을 보장하지 않는다. 서로 다른 sockets와 processes는 독립 sequence space를 가질 수 있고 multicast notifications에는 request correlation과 다른 semantics가 적용된다. Wraparound도 장기 실행 daemon에서 고려해야 한다. Client가 sequence만 보고 `더 큰 값이 더 최신 kernel state`라고 판단하면 잘못된 ordering을 만든다. **Sequence는 local request/reply conversation을 묶는 token**이고, state freshness는 dump generation, object attributes, notification reconciliation 같은 별도 evidence로 판단해야 한다.

## CHAPTER 04 · ACK는 kernel이 request를 수용한 결과를 말하지만 모든 비동기 효과의 종결점은 아니다

Mutation request에 ACK를 요구하면 kernel은 성공 또는 error를 `NLMSG_ERROR` 형식으로 돌려줄 수 있다. Error field가 0인 ACK는 request 처리 성공을 뜻하지만, network device의 carrier 변화, neighbor resolution, IPv6 DAD 같은 후속 state machine이 이미 원하는 operational state에 도달했다는 뜻은 아니다. Controller가 ACK 직후 external readiness를 선언하면 transient state를 놓칠 수 있다. 반대로 ACK를 요청하지 않으면 성공 path에는 reply가 없어 timeout을 실패로 오해할 수 있다. Mutation마다 **kernel configuration commit condition과 operational convergence condition을 구분**하고, 후자는 object dump/notification에서 검증해야 한다.

## CHAPTER 05 · NLMSG_ERROR는 실패 packet뿐 아니라 성공 ACK의 envelope로도 사용된다

이름만 보면 `NLMSG_ERROR`가 항상 error라고 생각하기 쉽지만 Netlink ACK도 같은 message type으로 전달되고 embedded error code가 0이면 성공을 나타낸다. Parser가 message type만 보고 실패 처리하면 정상 mutation을 전부 error로 기록할 수 있다. 실제 실패에서는 negative errno와 원래 request header가 포함되어 어떤 operation이 거절됐는지 correlation할 수 있다. Robust client는 payload length와 flags를 검증하고, error code를 errno domain으로 해석하며, original request metadata와 결합한다. **Envelope type과 semantic outcome을 분리**하는 것이 기본 parsing invariant다. Truncated error payload나 예상하지 못한 sender를 그대로 신뢰하지 않는 것도 중요하다.

## CHAPTER 06 · Extended ACK는 errno 하나로는 부족한 validation failure 위치를 전달한다

복잡한 nested attributes를 가진 request가 `EINVAL`로 실패하면 errno만으로 어느 field가 잘못됐는지 찾기 어렵다. Netlink extended ACK는 human-readable message, offending attribute offset 같은 추가 diagnostic information을 제공할 수 있다. 이를 활성화하고 log에 보존하면 controller가 generic retry loop로 같은 malformed request를 반복하는 대신 schema/version 문제를 구분할 수 있다. 다만 extack text는 stable machine contract로 취급하기보다 diagnostic evidence로 사용하는 편이 안전하다. 자동 분기는 numeric errno와 validated attribute metadata를 중심으로 하고 text는 운영자 설명에 붙인다. **Error observability는 retry policy의 input**이므로 풍부한 ACK 정보가 reliability에 직접 연결된다.

## CHAPTER 07 · Multipart dump는 여러 datagram을 모아 NLMSG_DONE까지 받아야 하나의 logical operation이 끝난다

Route나 link 전체를 조회하는 dump는 결과 cardinality가 커서 하나의 datagram에 담기지 않는다. Kernel은 `NLM_F_MULTI`가 붙은 여러 messages를 보내고 terminal `NLMSG_DONE`으로 logical dump 종료를 알린다. 첫 `recvmsg()`에서 유효한 entries를 받았다고 cache를 replace하면 뒤 messages에 있는 objects가 누락된다. 반대로 terminal message를 놓치고 무한 대기하면 reconciliation loop가 멈춘다. Client는 sequence가 맞는 multipart set만 수집하고 DONE 또는 explicit error에서 transaction을 닫아야 한다. **Receive call boundary와 dump transaction boundary는 다르다**는 점이 대규모 state sync의 핵심이다.

## CHAPTER 08 · Dump 중 concurrent mutation이 있으면 결과는 단순한 시점 snapshot이 아닐 수 있다

Userspace가 수천 개 routes를 dump하는 동안 다른 process가 link나 route를 추가·삭제할 수 있다. Kernel API와 family가 제공하는 consistency 수준에 따라 dump는 시작 순간의 완전한 snapshot이 아니라 traversal 중 변화가 섞인 view가 될 수 있다. 일부 상황에서는 interrupted/inconsistent dump를 flags로 알려 재시도를 요구한다. Controller가 이를 무시하고 결과를 authoritative cache로 승격하면 존재하는 object를 삭제 대상으로 오판하거나 이미 사라진 object를 유지할 수 있다. **Dump completion과 dump consistency는 별도 조건**이다. High-churn 시스템에서는 retry backoff, generation check, event replay를 결합해 stable reconciliation point를 만들어야 한다.

## CHAPTER 09 · Multicast notification은 change hint이지 durable event log가 아니다

Rtnetlink multicast group을 subscribe하면 link/address/route/neighbor 변화 notifications를 받을 수 있어 polling을 줄일 수 있다. 하지만 socket receive buffer overflow, scheduler starvation, process restart 동안 events가 손실될 수 있고, 구독 이전에 일어난 변화는 받을 수 없다. 그래서 event stream만 누적해 local mirror를 영원히 유지하는 설계는 eventually divergent하다. Notification은 cache를 빠르게 갱신하는 freshness path로 사용하고, loss signal이나 reconnect가 발생하면 authoritative dump로 resync해야 한다. **No event observed ≠ no state change**라는 invariant를 controller 설계에 넣어야 silent drift를 막을 수 있다.

## CHAPTER 10 · ENOBUFS와 receive-queue pressure는 control-plane observability loss를 의미한다

Kernel 또는 socket receive queue가 userspace consumer보다 빠르게 notifications를 생성하면 message drop이 발생할 수 있다. Netlink client가 `ENOBUFS` 같은 overrun signal을 단순 transient read error로 처리하고 계속 진행하면 local cache의 어느 구간이 빠졌는지 알 수 없다. 이 시점부터 incremental state는 신뢰할 수 없으므로 dirty flag를 세우고 full dump/reconciliation으로 복구해야 한다. Receive buffer를 키우는 것은 burst tolerance를 높이지만 loss-proof를 만들지는 않는다. Event processing latency, queue depth, dropped notification, resync 횟수를 함께 측정해야 한다. **Buffer overflow는 성능 문제이자 state-consistency fault**다.

## CHAPTER 11 · Initial dump와 live events 사이의 gap은 subscribe-before-dump 패턴으로도 완전히 사라지지 않는다

Controller 시작 시 흔한 전략은 multicast를 먼저 subscribe하고 full dump를 수행한 뒤 queued events를 적용하는 것이다. 이 방식은 dump 시작 이후 변화를 포착하는 데 유리하지만 dump traversal 중 이미 포함된 object에 대한 event가 다시 도착하거나, object create/delete/create가 빠르게 일어나 duplicate/stale transitions가 생길 수 있다. Event에 global total order가 없는 경우 단순 arrival order만으로 snapshot과 완벽히 merge하기 어렵다. 그래서 controller는 object identity와 final state를 idempotent하게 적용하고 필요하면 second verification dump를 사용한다. **Startup synchronization은 event replay가 아니라 snapshot과 delta의 reconciliation 문제**다.

## CHAPTER 12 · Netlink attribute는 TLV이므로 length와 alignment를 검증하지 않으면 parser boundary가 무너진다

Netlink attribute는 type과 length를 가진 TLV 구조이며 message 안에서 정렬 규칙을 따른다. Userspace parser가 declared length를 신뢰해 buffer 끝을 넘어 읽거나 padding을 payload로 해석하면 malformed kernel/user input에서 memory-safety 문제가 생길 수 있다. Nested attributes도 outer length 안에 완전히 들어와야 한다. 64-bit value가 항상 자연 정렬되어 있다고 가정해서 직접 pointer cast하는 코드도 portability 문제를 만든다. Robust decoder는 header 최소 길이, attribute length, aligned advance, message remaining bytes를 단계별로 확인한다. **Wire length가 memory access authority를 결정하며 C structure size가 packet truth가 아니다**.

## CHAPTER 13 · Unknown attribute를 무조건 error로 처리하면 forward compatibility가 깨진다

Kernel은 새 기능을 추가하면서 기존 message에 optional attribute를 확장할 수 있다. Client가 자신이 모르는 attribute를 발견할 때마다 전체 message를 reject하면 새 kernel에서 오래된 userspace가 불필요하게 고장난다. 반대로 mandatory semantics를 모른 채 무시하면 잘못된 state를 만들 수 있다. API family의 compatibility contract에 따라 unknown optional fields는 skip하고, operation 수행에 필수인 feature는 capability/policy query로 확인하는 전략이 필요하다. Attribute type number와 raw length를 debug evidence로 보존하면 새 kernel에서 drift를 분석하기 쉽다. **Extensibility는 “모든 것을 이해한다”가 아니라 이해하지 못하는 data를 안전하게 경계 짓는 능력**이다.

## CHAPTER 14 · Nested attribute는 별도 schema space를 가지므로 outer type만 알아서는 충분하지 않다

Link info, route metrics, address flags 같은 객체는 nested attributes로 복합 구조를 표현할 수 있다. Outer attribute가 nest라는 사실만 확인하고 inner payload를 같은 flat type table로 해석하면 type collision과 length 오류가 생긴다. 각 nest는 별도 attribute set과 validation rule을 가질 수 있고, family/version에 따라 지원 범위가 다르다. Decoder는 parser context를 stack처럼 유지해 현재 어느 nested schema를 해석 중인지 알아야 한다. Encoder도 child attributes를 완성한 뒤 parent length를 닫아야 한다. **Nested TLV는 단순 재귀 형식이 아니라 namespace가 바뀌는 schema transition**이다.

## CHAPTER 15 · Generic Netlink family ID는 이름에서 runtime에 resolve되는 dynamic identifier다

Generic Netlink family는 사람이 아는 family name과 runtime numeric ID를 구분한다. Numeric ID가 boot나 module lifecycle을 넘어 영구적으로 고정된다고 가정하면 다른 environment에서 잘못된 family로 request를 보낼 수 있다. Userspace는 control family를 통해 name→ID와 multicast group 정보를 resolve하고 cache lifetime을 관리해야 한다. Module unload/reload나 namespace별 availability가 바뀌면 cached capability가 stale해질 수 있다. Protocol constants처럼 source code에 numeric family ID를 hard-code하지 않는 이유다. **Stable semantic name과 ephemeral runtime handle을 분리**하는 것은 kernel object handle 전반에 반복되는 패턴이다.

## CHAPTER 16 · Kernel-advertised policy와 machine-readable specs는 parser를 handwritten assumption에서 분리한다

최근 Netlink documentation은 YAML 기반 family specification으로 operations, attributes, nested sets, validation constraints를 기계가 읽을 수 있게 표현한다. 이 정보는 code generation과 input policy에 활용되어 userspace가 C header layout을 임의로 복제하는 오류를 줄인다. 그렇다고 schema file 하나만 믿고 running kernel capability를 추정하면 안 된다. 실제 kernel version/config/module에 따라 operation 지원이 다를 수 있으므로 introspection과 runtime error handling이 필요하다. Generated decoder도 unknown attrs와 length failure를 처리해야 한다. **Specification은 serialization contract를 강화하지만 live capability negotiation을 대체하지 않는다**.

## CHAPTER 17 · rtnetlink의 ifindex는 interface name보다 강한 runtime identity지만 lifetime 밖에서는 재사용될 수 있다

Network interface name은 rename될 수 있으므로 controller가 `eth0` 같은 이름만 key로 사용하면 rename을 delete+create로 오인하거나 잘못된 device를 수정할 수 있다. ifindex는 live namespace 안에서 interface를 식별하는 더 강한 handle이지만 device 삭제 후 숫자가 영원히 reserve되는 것은 아니므로 장기 persistence key로 무조건 안전하지 않다. Event cache는 namespace + ifindex + observed lifetime을 묶고, human-readable name은 attribute로 취급하는 편이 낫다. Delete event 뒤 같은 ifindex가 새 device에 재사용되면 old metadata를 상속시키면 안 된다. **Runtime handle과 durable identity를 구분**해야 device churn에서 ABA형 오류를 막는다.

## CHAPTER 18 · Link create/delete는 interface object뿐 아니라 dependent address, route, neighbor state를 연쇄 변경한다

Interface가 사라지면 그 ifindex에 붙은 addresses, connected routes, neighbor entries와 qdisc 같은 dependent objects도 영향을 받는다. Userspace가 RTM_DELLINK 하나만 local cache에서 제거하고 children을 그대로 두면 dangling state가 남는다. 반대로 child delete notifications가 먼저/나중에 도착할 수 있어 특정 arrival order에 의존하면 race가 생긴다. Controller는 link lifetime을 parent ownership boundary로 보고 link deletion 시 dependent cache를 invalidate하거나 authoritative dump로 재확인해야 한다. **Object graph의 root lifetime이 끝나면 child state의 validity도 재평가**해야 한다.

## CHAPTER 19 · Address configured와 address usable은 IPv6 DAD 같은 후속 state 때문에 분리된다

Address add request가 ACK됐다고 해서 그 주소가 즉시 source address로 안전하게 사용 가능한 것은 아니다. IPv6에서는 Duplicate Address Detection 동안 tentative state를 거칠 수 있고 conflict가 발견되면 dadfailed 같은 결과로 전환될 수 있다. Preferred/deprecated lifetime도 시간이 지나며 usability를 바꾼다. Controller가 `RTM_NEWADDR ACK == ready`라고 선언하면 service bind 또는 route advertisement가 너무 일찍 시작될 수 있다. Address flags와 lifetime notifications를 관측해 operational readiness를 별도로 판정해야 한다. **Configuration object existence와 protocol-level readiness는 별도 state machine**이다.

## CHAPTER 20 · Route identity는 prefix 하나가 아니라 table, priority, protocol, nexthop 같은 selector 조합이다

같은 destination prefix에 여러 routing tables와 metrics, source constraints, multipath nexthops가 공존할 수 있다. Userspace cache가 prefix만 key로 쓰면 valid parallel routes를 서로 덮어쓴다. Delete request도 selector를 충분히 구체적으로 주지 않으면 의도보다 넓거나 다른 route를 건드릴 수 있다. Kernel dump에서 table ID가 main/default 외 확장 attribute로 표현되는 경우 parser가 legacy field만 읽으면 identity를 잃을 수 있다. **Route는 prefix object가 아니라 lookup policy 안의 structured key/value record**이며 desired-state diff도 그 전체 identity를 기준으로 해야 한다.

## CHAPTER 21 · Multipath route는 route 하나 안에 여러 nexthop lifetime과 weight를 가진 복합 object다

ECMP/multipath는 동일 prefix에 단순 duplicate routes를 넣는 것과 다르게 한 route가 여러 nexthops를 포함할 수 있다. 각 nexthop의 interface, gateway, weight가 바뀌면 traffic distribution이 변하며 일부 path failure가 전체 route deletion을 의미하지 않을 수 있다. Nested rtnetlink attributes를 잘못 flatten하면 nexthop grouping이 사라져 config round-trip이 깨진다. Controller는 unordered set인지 ordered list인지 family semantics를 따라 canonicalization해야 false diff를 줄일 수 있다. **Composite network object를 text line 단위로 비교하지 말고 nested identity와 membership 변화로 비교**해야 한다.

## CHAPTER 22 · Neighbor entry는 단순 ARP/NDP cache가 아니라 reachability state machine의 관측면이다

Neighbor object는 IP→link-layer address mapping과 함께 INCOMPLETE, REACHABLE, STALE, DELAY, PROBE, FAILED 같은 reachability state를 가질 수 있다. Route가 존재해도 next-hop neighbor가 FAILED이면 packet delivery는 진행되지 않는다. Notification burst를 모두 durable truth로 저장하면 transient probes까지 incident처럼 보일 수 있고, 반대로 final FAILED를 놓치면 blackhole 원인을 route 문제로 오판한다. Controller와 observability pipeline은 mapping identity와 reachability state, timer-driven transitions를 분리해 기록해야 한다. **Control-plane configuration과 liveness evidence가 한 object에 함께 존재**하는 대표 사례다.

## CHAPTER 23 · Policy routing rule은 route보다 먼저 lookup domain을 선택하므로 ordering이 semantics다

Routing policy database rule은 source/destination, mark, interface 같은 selector에 따라 어느 table을 조회할지 결정하고 priority ordering을 가진다. Rule set은 같은 routes를 두고도 전혀 다른 forwarding 결과를 만들 수 있다. Controller가 routes만 snapshot하고 rules를 누락하면 실제 lookup path를 재현할 수 없다. 동일 priority 충돌이나 insertion order dependency를 허용하는 구성은 재배포 때 결과가 달라질 수 있어 canonical explicit priorities가 안전하다. **Forwarding proof는 rule traversal → selected table → route/nexthop lookup의 chain을 보존**해야 한다.

## CHAPTER 24 · Netlink message ordering은 socket queue ordering이지 multi-message atomic transaction 보장이 아니다

한 socket에서 여러 mutation requests를 연속 send했다고 해서 모든 rtnetlink families가 그 묶음을 하나의 atomic configuration transaction으로 commit하는 것은 아니다. 앞 request가 성공하고 뒤 request가 실패하면 partial desired state가 남을 수 있다. Nftables처럼 explicit batch transaction을 제공하는 subsystem과 일반 rtnetlink mutation을 같은 모델로 취급하면 rollback 가정이 틀어진다. Multi-object change가 원자적이어야 한다면 family가 제공하는 transactional primitive를 확인하거나 controller가 compensating operation과 reconciliation을 구현해야 한다. **Transport ordering과 state commit atomicity는 독립 속성**이다.

## CHAPTER 25 · Request retry는 idempotency가 확인되지 않으면 duplicate object나 destructive side effect를 만들 수 있다

ACK가 timeout됐다고 해서 kernel이 request를 실행하지 않았다는 뜻은 아니다. Reply만 유실됐을 수 있으므로 동일 create request를 그대로 재전송하면 `EEXIST`가 오거나 duplicate semantics를 가진 family에서는 추가 object가 생길 수 있다. Delete retry도 object identity가 재사용된 뒤라면 새 object를 지우는 ABA 위험이 있다. Reliable controller는 timeout 뒤 먼저 current state를 조회해 intended effect가 이미 적용됐는지 확인하고, create/replace/exclusive flags를 의도에 맞게 사용한다. **At-least-once transport retry를 exactly-once state mutation처럼 취급하지 않는 것**이 control-plane reliability의 핵심이다.

## CHAPTER 26 · Capability와 namespace는 Netlink mutation authorization의 두 축이다

Network configuration mutation은 적절한 capability가 필요하고 network namespace마다 권한·object set이 다르다. Host에서 `CAP_NET_ADMIN`을 가졌다고 모든 target namespace의 state를 같은 socket으로 자동 제어하는 것은 아니다. Socket이 어느 namespace에서 생성되었는지, process가 namespace를 언제 전환했는지, user namespace capability mapping이 무엇인지가 authorization과 visible state를 결정한다. Privilege error를 단순 `permission denied`로 기록하면 namespace mismatch를 찾기 어렵다. **Credentials와 state-owner namespace를 함께 기록**해야 권한 failure와 object-not-found를 구분할 수 있다.

## CHAPTER 27 · Netlink socket은 생성 시점의 network namespace context와 연결되므로 setns ordering이 중요하다

Daemon이 여러 container namespace를 관리할 때 한 번 연 Netlink socket을 모든 namespace에 재사용할 수 있다고 가정하면 잘못된 state를 읽고 쓸 수 있다. 일반적으로 target namespace context에서 socket을 열거나 namespace-aware API pattern을 사용해 ownership을 명확히 해야 한다. Thread가 `setns()`를 호출한 뒤 다른 작업과 공유되면 process/thread context race도 생길 수 있다. Namespace별 dedicated worker/socket 모델은 isolation을 단순화한다. Evidence에는 netns inode/cookie 같은 stable-enough runtime identifier와 socket port ID를 같이 남기는 편이 좋다. **Socket lifetime과 namespace lifetime을 연결하지 않으면 cross-tenant mutation 위험**이 생긴다.

## CHAPTER 28 · Desired-state controller는 event consumer보다 diff/reconcile loop로 설계해야 self-healing이 된다

신뢰할 수 있는 network agent는 notifications를 받아 imperative command를 연속 실행하는 구조보다, desired state와 authoritative observed state를 비교해 convergence시키는 loop를 가진다. Event loss, daemon restart, manual operator change가 발생해도 다음 reconciliation에서 drift를 발견할 수 있기 때문이다. Diff는 object identity와 kernel-normalized defaults를 이해해야 하고, no-op update를 반복해 event storm을 만들지 않아야 한다. Failed mutation은 retry budget과 backoff를 갖되 current state를 다시 읽어 precondition을 갱신한다. **Correctness goal은 event를 하나도 놓치지 않는 것이 아니라 놓쳐도 최종 state를 다시 증명할 수 있는 것**이다.

## CHAPTER 29 · Netlink debugging evidence는 raw message보다 correlation context를 함께 보존해야 가치가 있다

Hex dump만 저장하면 sequence가 어느 operation인지, 어느 namespace/socket에서 왔는지, dump의 몇 번째 fragment인지 알 수 없어 재현성이 낮다. 최소 evidence는 monotonic timestamp, protocol/family, local port ID, sequence, message type/flags, object key, ACK errno/extack, multipart completion, namespace identity, receive-loss signal을 포함해야 한다. Sensitive address와 identifiers는 보존 정책에 맞춰 redact하되 correlation 가능성은 유지해야 한다. Trace와 controller log의 clock source도 맞춰야 packet/control-plane 변화 순서를 비교할 수 있다. **Evidence schema가 message schema만큼 중요**하며 incident 전에 설계되어야 한다.

## CHAPTER 30 · Netlink correctness는 conversation, snapshot, event, object-lifetime 네 ledger를 동시에 닫아야 한다

Conversation ledger는 socket/port ID와 request sequence, ACK/error, multipart DONE을 추적한다. Snapshot ledger는 dump가 어느 시점과 consistency 조건에서 완성됐는지 기록한다. Event ledger는 multicast subscription, loss/ENOBUFS, replay/reconciliation 여부를 관리한다. Object-lifetime ledger는 namespace 안에서 ifindex, route key, neighbor identity가 언제 생성·변경·삭제·재사용됐는지 연결한다. 장애 복구는 네 ledger 중 하나를 추측으로 채우는 대신 authoritative dump로 다시 닫는 과정이어야 한다. **Netlink는 message API가 아니라 distributed control-plane state를 userspace와 kernel 사이에서 수렴시키는 synchronization protocol**로 다뤄야 한다.
