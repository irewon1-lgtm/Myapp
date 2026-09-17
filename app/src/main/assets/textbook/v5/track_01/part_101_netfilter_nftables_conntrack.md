# PART 101 · Netfilter, nftables, and Conntrack State — hooks, transactional rulesets, tuple identity, NAT bindings, flow offload

Linux firewalling을 단순히 “packet마다 rule 목록을 위에서 아래로 검사한다”로 설명하면 실제 failure domain의 절반이 사라진다. Netfilter는 packet traversal의 여러 hook, nftables가 관리하는 generation 단위 ruleset, conntrack의 bidirectional flow identity, NAT binding, expectation/helper state, flowtable fast path를 결합한다. Packet 하나의 verdict는 현재 packet bytes만이 아니라 **어느 namespace의 어떤 ruleset generation이 활성인지, 그 packet이 어떤 conntrack entry에 귀속되는지, NAT와 offload state가 이미 만들어졌는지**에 의해 달라진다. 운영에서 `nft list ruleset`만 정상이어도 기존 flows가 오래된 state를 들고 있거나 offload가 classic hooks를 우회하면 관측 결과가 달라질 수 있다. 따라서 config state와 live flow state를 별도 ledger로 봐야 한다.

## CHAPTER 01 · Netfilter hook은 firewall 프로그램이 아니라 packet traversal의 경계점이다

Netfilter는 하나의 중앙 함수가 모든 packet을 소유하는 구조가 아니라 network stack의 의미 있는 traversal 지점에 hook을 제공한다. Incoming packet, locally delivered packet, forwarded packet, locally generated packet, outgoing packet은 서로 다른 hook sequence를 통과할 수 있다. 따라서 같은 rule expression을 어느 hook에 붙였는지가 verdict 의미를 바꾼다. 예를 들어 route decision 이전에 주소를 바꾸는 것과 이후에 바꾸는 것은 lookup 결과와 egress interface까지 달라질 수 있다. Base chain은 특정 hook과 priority에 등록되고 packet이 그 지점에 도달해야만 평가된다. Debugging에서 “rule이 있는데 왜 안 맞았나”를 묻기 전에 packet이 실제로 그 hook을 통과했는지 확인해야 한다. **Traversal path가 evaluation eligibility를 결정한다**는 것이 첫 invariant다.

## CHAPTER 02 · Hook priority는 여러 subsystem의 실행 순서를 만드는 shared ordering contract다

하나의 hook에 nftables chain만 존재하는 것이 아니다. Conntrack attach, DNAT/SNAT, security processing, flowtable ingress 같은 subsystem이 서로 다른 priority에 걸릴 수 있다. 숫자 ordering을 잘못 잡으면 rule 문법은 맞아도 원하는 metadata가 아직 생성되기 전이거나 이미 변환된 뒤일 수 있다. 예를 들어 conntrack state를 참조하는 chain은 conntrack lookup이 수행된 뒤여야 의미가 있고, `notrack`은 tracking이 붙기 전에 적용되어야 한다. 같은 hook name만 비교해서는 충분하지 않다. 장애 재현 시 chain hook, priority, family, device scope를 함께 기록해야 ordering을 복원할 수 있다. **Rule correctness는 expression 자체와 실행 시점의 prerequisite state가 동시에 맞아야 성립**한다.

## CHAPTER 03 · nftables expression은 register와 verdict를 통해 작은 packet-evaluation machine을 구성한다

Nftables rule은 고정된 필드 비교 문장 하나가 아니라 packet/meta data를 load하고 비교하고 lookup하고 state를 갱신한 뒤 verdict를 내리는 expression sequence다. Intermediate register는 payload, metadata, set lookup 결과 같은 값을 전달하고 verdict register는 continue, jump, goto, accept, drop, return 같은 control-flow 결과를 표현한다. 이 구조에서는 한 expression의 side effect가 뒤 expression의 input이 될 수 있으므로 단순 텍스트 순서가 execution semantics를 가진다. Set/map lookup 실패가 곧 drop인지, 다음 expression으로 가지 않는지, jump 후 return이 어느 chain으로 돌아오는지까지 봐야 한다. Trace evidence는 최종 verdict만 남기는 것보다 **어느 rule handle과 expression에서 control flow가 바뀌었는지**를 남겨야 재현 가능하다.

## CHAPTER 04 · Table, chain, rule의 이름과 handle은 서로 다른 identity layer다

운영자는 보통 사람이 읽는 table/chain name으로 정책을 관리하지만 kernel netlink API는 object handle과 transaction-local ID를 함께 사용한다. Rule 위치를 line number처럼 생각하면 앞 rule 추가·삭제 후 동일 숫자가 다른 object를 가리킬 수 있다. Handle은 live ruleset에서 특정 object를 식별하는 데 유용하지만 ruleset을 통째로 재생성하면 새 generation에서 identity가 달라질 수 있다. 따라서 자동화가 `세 번째 rule을 삭제` 같은 positional assumption에 의존하면 drift가 생긴다. Config-as-code에서는 semantic key와 expected generation을 유지하고, 변경 후 dump에서 handle을 다시 확인하는 편이 안전하다. **Human-readable identity, kernel object identity, deployment generation을 한 값으로 합치지 않는 것**이 update race를 줄인다.

## CHAPTER 05 · Base chain과 regular chain은 packet entry ownership이 다르다

Base chain은 hook에 직접 연결되어 packet evaluation의 entry point가 되지만 regular chain은 jump/goto 같은 control flow로 호출되어야 실행된다. Regular chain이 존재한다는 사실만으로 packet이 그 chain을 방문하는 것은 아니다. Jump는 caller로 return 가능한 call-like semantics를 만들고 goto는 return path가 다르므로 nested policy에서 control flow를 잘못 해석하기 쉽다. Chain policy 역시 base chain에서 end-of-chain에 도달했을 때 의미가 있으며 모든 intermediate chain에 같은 방식으로 적용되는 것이 아니다. Debugging에서는 chain graph를 함수 call graph처럼 그려 어떤 packet class가 어느 branch로 진입하는지 확인하는 것이 유리하다. **Reachability가 없는 rule은 문법상 정상이어도 operationally dead state**다.

## CHAPTER 06 · nftables batch는 여러 mutation을 하나의 generation transition으로 묶는다

Kernel nftables netlink API는 batch begin/end를 통해 여러 object mutation을 transaction처럼 제출할 수 있다. 새 table, chain, set, rule을 순차적으로 만들어도 batch가 성공하면 readers에게 일관된 새 generation이 보이고, 중간 command가 실패하면 부분 적용을 피할 수 있다. 반대로 shell에서 개별 명령을 따로 실행하면 command 사이 순간에 old/new policy가 섞인 state가 존재할 수 있다. Firewall 변경에서 그 짧은 window도 실제 packet을 잘못 허용하거나 차단할 수 있다. Batch generation ID는 stale writer가 예상하지 못한 ruleset 위에 변경을 덮는 race를 검출하는 단서가 된다. **Policy rollout의 atomicity 단위는 text file이 아니라 kernel이 commit한 ruleset generation**이다.

## CHAPTER 07 · Atomic replacement는 syntax 성공보다 stronger한 all-or-nothing invariant를 제공한다

새 ruleset을 검증한 뒤 atomic하게 교체하면 deployment 중 partially configured firewall이 노출되는 시간을 제거할 수 있다. 다만 `nft -f` 사용 자체가 원하는 최종 state를 보장하는 것은 아니다. Existing objects를 flush/delete하지 않고 같은 declarations를 추가하면 reload마다 duplicate rules가 쌓일 수 있고, dynamic set elements나 stateful objects를 유지하려는 의도와 전체 flush가 충돌할 수 있다. 안전한 replacement는 desired-state 정의, preserve해야 할 runtime state, destructive reset 범위를 분리한다. Rollback도 이전 파일을 다시 실행하는 수준이 아니라 이전 generation이 참조하던 object와 새 live conntrack state의 상호작용을 확인해야 한다. **Atomic commit은 partial config를 막지만 semantic drift까지 자동으로 막지는 않는다**.

## CHAPTER 08 · Set과 map은 rule count를 줄이는 자료구조이면서 별도 lifetime을 가진 stateful object다

많은 address/port를 개별 rule로 늘어놓는 대신 set과 map을 사용하면 lookup complexity와 policy 관리가 달라진다. Set element는 interval, timeout, concatenated key 같은 semantics를 가질 수 있고 map은 key lookup 결과로 verdict나 변환 값을 제공할 수 있다. Dynamic element를 runtime에 추가하면 ruleset text가 같아도 live state는 달라진다. Set object를 교체할 때 rule이 여전히 reference 중이면 deletion ordering이 제약되고, timeout element가 만료되는 순간 packet verdict가 바뀔 수 있다. 관측에서는 ruleset version과 함께 element count, timeout/expiration, failed insert를 기록해야 한다. **Rule graph와 referenced data structure의 lifetime은 별개**이며 둘 중 하나만 snapshot하면 정책을 완전히 복원할 수 없다.

## CHAPTER 09 · Stateful counter, quota, limit은 policy object에 mutable runtime state를 붙인다

Counter는 packets/bytes를 누적하고 quota는 누적 사용량이 threshold를 넘는 순간 verdict 의미를 바꿀 수 있으며 rate limit은 time window에 따라 허용 여부가 달라진다. 동일한 ruleset generation이라도 runtime state가 달라지면 결과가 달라질 수 있다는 뜻이다. Config reload가 stateful object를 preserve하는지 재생성하는지에 따라 counter continuity와 quota enforcement가 달라진다. Monitoring이 handle만 따라가면 object recreation 뒤 counter reset을 traffic 감소로 오인할 수 있다. 반대로 named object를 유지하면 새 policy가 old usage debt를 물려받는다. **Configuration state와 accumulated state를 분리해 migration policy를 명시**해야 deployment 전후 의미가 유지된다.

## CHAPTER 10 · Conntrack entry의 핵심 identity는 original tuple과 reply tuple의 양방향 관계다

Connection tracking은 packet 하나를 독립 record로 저장하지 않고 양방향 flow를 하나의 entry로 묶는다. Original direction tuple은 처음 관찰한 source/destination address, protocol, port 같은 key를 담고 reply tuple은 반대 방향 packet이 같은 connection으로 귀속되도록 lookup identity를 제공한다. NAT가 있으면 wire에서 보이는 tuple과 conntrack이 유지하는 original/reply relation이 단순 대칭이 아닐 수 있다. 그래서 packet capture 한 방향만 보고 `다른 tuple이니 다른 connection`이라고 결론 내리면 오류가 난다. Evidence에는 original/reply tuple, conntrack ID, zone, status를 함께 남겨야 한다. **Connection identity는 directional packet key 두 개를 하나의 lifetime으로 결합한 object**다.

## CHAPTER 11 · Conntrack zone은 동일 tuple의 충돌을 막는 별도 identity namespace다

Virtualization, overlapping tenant networks, bridge/VRF 구성에서는 서로 다른 logical domain이 같은 5-tuple을 사용할 수 있다. Zone을 사용하면 tuple이 같아도 별도 conntrack entries로 분리해 state collision을 막을 수 있다. 반대로 ingress path에서 zone assignment가 일관되지 않으면 forward packet과 reply packet이 서로 다른 zone에서 lookup되어 NEW/INVALID처럼 보일 수 있다. Flowtable lookup도 input interface나 zone topology와 연결될 수 있어 offload까지 영향을 받는다. Debugging에서는 address/port만 기록하지 말고 zone을 conntrack identity의 일부로 취급해야 한다. **Tuple uniqueness의 scope가 host 전체인지 zone 내부인지 명확하지 않으면 state association proof가 성립하지 않는다**.

## CHAPTER 12 · Unconfirmed에서 confirmed로의 전이는 conntrack object가 global lookup에 공개되는 commit boundary다

새 flow의 첫 packet을 처리하는 동안 conntrack object가 할당됐다고 해서 즉시 모든 packet이 그 entry를 lookup할 수 있는 것은 아니다. Packet이 필요한 hook과 validation을 통과한 뒤 entry가 confirmed state로 들어가야 stable lookup identity가 된다. Drop되거나 실패한 first packet의 tentative state를 무조건 global table에 남기면 공격자가 미완성 entries로 table을 오염시키거나 잘못된 policy를 지속시킬 수 있다. Confirmation 이전/이후 failure cleanup은 reference lifetime이 다르다. Race 분석에서는 allocation, tuple insertion, confirmation, timeout activation, destruction을 분리해야 한다. **Object allocation과 externally visible connection commitment는 서로 다른 transition**이다.

## CHAPTER 13 · NEW, ESTABLISHED, RELATED, INVALID는 TCP finite-state-machine 이름이 아니다

Conntrack state는 protocol transport FSM을 그대로 복제하지 않는다. `NEW`는 conntrack 관점에서 아직 reply를 본 적 없는 flow를 포함할 수 있고, `ESTABLISHED`는 양방향 traffic을 관찰한 tracking relation을 의미한다. `RELATED`는 helper/expectation 또는 protocol-specific relation으로 기존 master connection과 연결된 새 flow일 수 있다. `INVALID`는 packet이 기존 entry에 정상적으로 귀속되지 못하거나 checksum/protocol validation 같은 이유로 tracking할 수 없는 경우를 나타낸다. 이를 TCP `SYN_SENT`, `ESTABLISHED`, `FIN_WAIT`와 1:1 대응시키면 firewall 정책과 transport debugging을 혼동한다. **Transport protocol state와 firewall connection state는 서로 다른 state machine이며 교차 관찰만 가능**하다.

## CHAPTER 14 · Protocol tracker와 timeout은 conntrack lifetime을 traffic semantics에 맞춰 조절한다

UDP처럼 transport-level connection teardown이 없는 protocol과 TCP처럼 lifecycle signal이 있는 protocol은 같은 timeout을 사용할 수 없다. Conntrack은 protocol-specific state와 configurable timeout을 이용해 entry를 얼마나 오래 유지할지 결정한다. Timeout이 너무 짧으면 long-idle but valid flow가 만료되어 다음 packet이 NEW로 보이고 NAT mapping이 바뀔 수 있다. 너무 길면 dead flows가 table capacity를 점유해 새 connection allocation을 방해한다. ASSURED 같은 status는 bidirectional progress를 보고 GC policy에서 보존 우선순위에 영향을 줄 수 있다. Capacity tuning은 max entries만 키우는 것이 아니라 **arrival rate × average lifetime × protocol mix**를 기준으로 해야 한다.

## CHAPTER 15 · Expectation과 helper는 future flow를 master connection에 연결하므로 parser가 security boundary가 된다

일부 application protocol은 control connection 안에서 future data connection의 address/port를 협상한다. Conntrack helper는 control payload를 해석해 expectation object를 만들고, 이후 matching flow를 RELATED로 master에 연결할 수 있다. 이 기능은 편리하지만 untrusted payload parser가 kernel policy state를 생성하는 경계가 된다. 과도하게 넓은 expectation이나 자동 helper assignment는 공격자가 예상 밖 port를 RELATED로 허용하도록 만들 수 있다. 현대 구성에서 helper를 명시적으로 붙이고 필요한 protocol/zone으로 scope를 좁히는 이유가 여기에 있다. Evidence에는 master conntrack ID, helper name, expectation tuple, timeout을 함께 기록해야 한다. **RELATED trust는 packet label이 아니라 누가 어떤 expectation을 생성했는지에서 출발**한다.

## CHAPTER 16 · Conntrack hash lookup은 양방향 key 때문에 entry 수와 lookup node 수를 구분해서 봐야 한다

Conntrack table은 packet마다 빠르게 original/reply direction을 찾기 위해 hash-based lookup을 사용한다. 하나의 logical connection이 양방향 tuple lookup을 제공하므로 내부 hash representation과 사용자에게 보이는 connection count를 단순히 같은 숫자로 생각하면 capacity 추정이 어긋날 수 있다. Kernel 문서가 `nf_conntrack_count`와 `nf_conntrack_max`를 별도로 노출하는 이유도 운영자가 현재 allocation pressure를 확인할 수 있게 하기 위해서다. Hash bucket이 지나치게 작거나 공격자가 collision을 유도하면 lookup CPU cost가 증가할 수 있다. **Logical flow cardinality, hash occupancy, lookup latency를 분리**해야 table이 꽉 차지 않았는데도 packet processing CPU가 치솟는 상황을 설명할 수 있다.

## CHAPTER 17 · nf_conntrack_max 도달은 단순 metric saturation이 아니라 새 flow state 생성 실패다

Conntrack allocation이 capacity limit에 도달하면 새 flow의 tracking state를 정상적으로 만들지 못할 수 있다. Firewall이 `ct state established,related accept`에 의존하고 있다면 tracking 실패는 단순 observability 손실이 아니라 packet verdict와 NAT 가능성까지 바꾼다. 공격자가 짧은 flows를 대량 생성해 table을 채우면 기존 service의 new connections가 영향을 받을 수 있으므로 capacity는 availability/security boundary다. `nf_conntrack_max`만 크게 올리면 entry memory footprint와 hash lookup cost가 늘고 OOM pressure를 다른 subsystem으로 전가할 수 있다. Alert는 utilization percentage뿐 아니라 insert failure/drop log, creation rate, timeout distribution을 포함해야 한다. **State-table exhaustion은 control-plane limit이 data-plane failure로 전파되는 경로**다.

## CHAPTER 18 · Garbage collection은 dead entry 제거뿐 아니라 pressure 아래에서 보존 우선순위를 결정한다

Timeout이 지난 entry는 reclaim 대상이지만 high pressure에서는 어떤 connection을 먼저 지울지에 따라 user-visible failure가 달라진다. Reply가 오고 traffic이 안정적으로 진행된 ASSURED flow를 쉽게 버리면 active session이 다음 packet에서 새 state로 재해석될 수 있다. 반대로 미완성 NEW entries를 오래 보존하면 SYN/UDP flood가 capacity를 잠식한다. GC는 entry state, timeout, protocol semantics, pressure를 함께 고려하는 policy다. Tune 후에는 단순 count 감소보다 evicted flow가 곧 다시 생성되는지, NAT mapping churn이 늘었는지, request error가 증가했는지 확인해야 한다. **Reclaim success는 memory를 회수한 사실이 아니라 useful live state를 보존하면서 pressure를 낮췄는지로 평가**해야 한다.

## CHAPTER 19 · Checksum 실패와 INVALID state는 malformed traffic을 policy에서 분리하는 evidence다

Conntrack은 protocol/packet validation 과정에서 checksum이 잘못되거나 tuple/state association이 성립하지 않는 packet을 INVALID로 분류할 수 있다. 운영자가 모든 INVALID를 공격으로 간주하면 NIC checksum offload나 capture 위치 때문에 생기는 관측 차이를 오판할 수 있고, 반대로 무조건 accept하면 evasion surface를 넓힐 수 있다. Packet capture가 host ingress 이전인지 이후인지, hardware offload가 checksum을 언제 완성하는지와 firewall hook 위치를 함께 봐야 한다. Invalid counter가 증가할 때 interface error, checksum offload setting, fragment 상태, conntrack log를 상관하는 이유다. **Packet validity는 bytes뿐 아니라 관측 지점과 tracking context에 의존**하므로 evidence provenance가 필요하다.

## CHAPTER 20 · Fragment는 transport tuple이 아직 보이지 않을 수 있어 state association 순서를 바꾼다

IPv4/IPv6 fragmentation에서는 모든 fragment가 L4 port를 포함하지 않으므로 transport tuple을 직접 계산할 수 없는 packet이 존재한다. Reassembly가 필요한 지점보다 먼저 conntrack/NAT decision을 강제하면 non-first fragment를 올바른 flow에 귀속시키기 어렵다. Flowtable fast path도 transport selectors가 없는 fragmented traffic을 classic path로 돌릴 수 있다. 공격자는 overlapping/odd fragment pattern으로 middlebox와 endpoint 해석 차이를 노릴 수 있으므로 normalization과 reassembly ownership이 security boundary다. Debugging에서는 packet 하나의 fragment offset만 보지 말고 fragment ID, reassembly queue lifetime, timeout, resulting conntrack entry를 연결해야 한다. **Tuple-based state machine은 tuple을 복원할 수 있는 시점 이후에만 완전한 identity를 가진다**.

## CHAPTER 21 · NAT는 매 packet에서 rule을 다시 선택하는 것이 아니라 connection에 binding을 만든다

Stateful NAT에서는 새 connection의 초기 packet이 NAT rule을 통해 translation decision을 만들고 그 결과가 conntrack/NAT extension에 binding으로 저장된다. 이후 packets는 매번 ruleset을 처음부터 재해석해 새 address/port를 선택하는 것이 아니라 established binding을 따라 양방향 변환된다. 따라서 NAT rule을 변경해도 이미 생성된 connections가 즉시 새 mapping으로 바뀌지 않을 수 있다. Deployment 직후 old/new translation이 공존하는 이유다. 특정 client만 계속 old backend로 가는 문제를 ruleset dump만 보고 찾지 못하는 경우 conntrack lifetime을 확인해야 한다. **NAT policy generation과 NAT binding generation은 독립적인 lifetime**을 가진다.

## CHAPTER 22 · Reply 방향 NAT는 original/reply tuple relation을 역변환하는 stateful operation이다

DNAT로 destination이 바뀐 request에 대해 reply packet은 backend의 실제 source에서 출발하지만 client에게는 원래 요청한 destination에서 온 것처럼 보여야 한다. Conntrack은 original/reply tuple과 NAT state를 이용해 reverse translation을 적용한다. 이 과정에서 asymmetric routing으로 reply가 conntrack state가 없는 다른 host/namespace를 통과하면 reverse mapping을 찾지 못해 connection이 깨질 수 있다. Stateful firewall/NAT cluster가 단순 stateless route ECMP와 충돌하는 대표 이유다. High availability 설계에서는 flow affinity 또는 state synchronization을 검토해야 한다. **Forward translation이 성공했다는 사실만으로 reply path correctness가 보장되지 않으며 동일 state owner로 돌아오는 경로가 필요**하다.

## CHAPTER 23 · SNAT port allocation은 shared namespace에서 collision과 exhaustion을 다루는 allocator 문제다

여러 internal flows가 하나의 public address를 공유하면 source port가 external tuple uniqueness를 유지하도록 선택돼야 한다. Original source port를 유지할 수 없으면 allocator가 다른 port를 선택하고, 동시에 생성되는 flows와 collision 없이 conntrack/NAT binding을 commit해야 한다. Ephemeral port space가 좁거나 특정 destination으로 fan-out이 집중되면 available tuple 조합이 고갈될 수 있다. 이 failure는 conntrack table total capacity와 별개다. `nf_conntrack_count`가 여유 있어도 NAT source tuple allocation이 실패할 수 있다. 운영에서는 translated address별 active bindings와 allocation errors를 분리해 봐야 한다. **NAT capacity는 entries 수뿐 아니라 usable external tuple cardinality에 의해 제한**된다.

## CHAPTER 24 · Conntrack mark, label, helper metadata는 packet bytes 밖의 policy state를 운반한다

Conntrack entry에는 mark, label, helper association, security context 같은 metadata가 붙을 수 있고 nftables rule이나 routing/QoS policy가 이를 참조할 수 있다. Packet mark와 connection mark를 같은 것으로 취급하면 per-packet mutation과 persistent per-flow state를 혼동한다. Connection mark를 첫 packet에서 설정한 뒤 후속 packets에 restore하는 pattern은 order가 바뀌면 예상과 다른 routing table을 선택할 수 있다. Metadata schema를 여러 teams가 공유하면 bit allocation 충돌도 생긴다. Evidence에는 numeric value뿐 아니라 mask/label 의미와 write rule handle을 남겨야 한다. **Out-of-band metadata도 connection identity의 일부처럼 lifetime과 ownership을 명시**해야 drift를 막을 수 있다.

## CHAPTER 25 · ctnetlink event stream은 authoritative database가 아니라 loss 가능한 change notification이다

Userspace conntrack monitor는 create/update/destroy event를 받아 local cache를 만들 수 있지만 event delivery가 끊기거나 socket buffer가 overflow되면 일부 transition을 놓칠 수 있다. Event가 없었다는 사실을 `state가 없었다`로 해석하면 local mirror가 kernel table과 영구적으로 어긋날 수 있다. Robust consumer는 sequence/loss signal을 감지하고 필요할 때 full dump로 reconciliation해야 한다. Dump 중에도 live changes가 계속되므로 snapshot과 event stream 사이 ordering gap을 처리하는 설계가 필요하다. Monitoring system도 conntrack table과 같은 truth owner가 아니다. **Event channel은 freshness mechanism이고 authoritative state는 kernel의 current dump/lookup**이라는 구분이 중요하다.

## CHAPTER 26 · Network namespace는 ruleset과 conntrack table의 ownership boundary를 나눈다

각 network namespace는 interface, route, Netfilter/conntrack 관련 state를 독립적으로 가질 수 있으므로 host root namespace의 ruleset만 보고 container traffic을 설명할 수 없다. Veth pair를 건너는 packet은 namespace 경계를 이동하면서 서로 다른 hook/ruleset을 통과할 수 있다. 동일 tuple도 namespace가 다르면 별도 conntrack domain에 존재할 수 있고 namespace teardown은 그 namespace가 소유한 connection state lifetime을 끝낸다. Debugging command를 어느 namespace에서 실행했는지가 evidence의 일부다. Container incident에서 `nft list ruleset`, `conntrack -L`, route dump는 반드시 namespace identity와 함께 보존해야 한다. **Namespace는 단순 이름 격리가 아니라 network state owner를 분할하는 lifecycle boundary**다.

## CHAPTER 27 · Flowtable은 established flow의 후속 packets를 classic forwarding hooks 밖의 fast path로 보낼 수 있다

Netfilter flowtable은 policy가 선택한 flow를 별도 lookup table에 넣고 이후 hit packet을 빠르게 egress로 보낼 수 있다. 첫 packets는 classic path를 통과해 conntrack/NAT state를 형성하지만 offload 이후 일부 packets는 ingress 뒤의 일반 forwarding hooks와 counters를 우회한다. 그래서 firewall counter가 갑자기 증가하지 않아도 traffic이 사라진 것이 아닐 수 있다. Flowtable entry에는 NAT configuration도 연계되어 fast path에서 translation을 유지한다. FIN/RST, fragment, MTU exception 같은 traffic은 classic path로 돌아올 수 있어 한 connection 안에서도 경로가 달라질 수 있다. **Fast path는 semantic bypass가 아니라 cached forwarding state를 다른 execution path에서 적용하는 것**이다.

## CHAPTER 28 · Hardware offload는 kernel policy state와 NIC/switch programmed state 사이에 두 번째 commit domain을 만든다

Flow를 hardware로 offload하면 userspace→kernel ruleset commit만으로 끝나지 않고 driver와 device table에 state를 program하는 과정이 추가된다. Kernel이 flow를 offloaded로 표시한 시점과 device가 실제 모든 packets를 새 rule로 처리하는 시점 사이에는 asynchronous boundary가 있을 수 있다. Device reset, table exhaustion, unsupported action은 일부 flows만 software path로 fallback하게 만들 수 있다. 따라서 software counter와 hardware counter가 다르게 보이고 packet capture 지점도 달라진다. Rollback 시 device entries가 제거됐는지까지 확인하지 않으면 stale forwarding이 남을 수 있다. **Offload는 performance optimization이면서 별도 state replica와 reconciliation failure를 도입**한다.

## CHAPTER 29 · Ruleset rollout 증거는 config diff, trace, counters, conntrack state를 같은 generation에 묶어야 한다

Firewall 변경 후 `명령이 성공했다`는 로그만으로 packet policy가 기대대로 동작했음을 증명할 수 없다. 배포 전 desired ruleset hash, commit 후 kernel dump, key rule handles, trace sample, counters, conntrack/NAT entries, offload status를 함께 수집해야 config와 live state를 연결할 수 있다. Counter가 0이면 rule이 잘못된 것인지 packet이 다른 hook/fast path를 탔는지 구분해야 하고, trace가 한 packet에서 성공해도 existing conntrack bindings이 모두 새 policy를 따르는 것은 아니다. Canary traffic은 NEW flow와 ESTABLISHED flow를 분리해 검증하는 것이 유리하다. **Evidence bundle의 핵심은 “어느 generation에서 어느 flow identity가 어느 path를 탔는가”를 재구성할 수 있는지**다.

## CHAPTER 30 · Netfilter correctness는 ruleset, connection, translation, execution-path 네 ledger를 동시에 닫아야 한다

Ruleset ledger는 활성 generation과 table/chain/rule/set object graph를 기록한다. Connection ledger는 namespace·zone·original/reply tuple·status·timeout으로 conntrack lifetime을 추적한다. Translation ledger는 어떤 first-packet decision이 NAT binding을 만들었고 언제 소멸하는지 기록한다. Execution-path ledger는 packet이 classic hooks, flowtable software fast path, hardware offload 중 어디에서 처리됐는지를 나타낸다. 장애 시 한 ledger만 보면 모순처럼 보이는 현상이 다른 ledger에서 설명된다. Ruleset을 바꿨는데 기존 NAT flow가 유지되거나, counter가 멈췄는데 traffic은 계속되거나, 같은 tuple이 tenant별로 다른 state를 갖는 현상이 그 예다. **안전한 firewall 운영은 네 ledger의 owner, generation, lifetime, reconciliation evidence를 함께 증명하는 작업**이다.
