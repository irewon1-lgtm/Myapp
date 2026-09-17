# PART 103 · Linux Keyrings and Credential-bound Secret Lifetime — possession, linkage, request_key, revocation, quotas, trust boundaries

Linux keyring을 단순한 “커널 안의 비밀번호 저장소”로 보면 실제 failure domain을 놓친다. Key는 serial number를 가진 kernel object이고, keyring은 그 object들을 link로 가리키는 별도 container다. 접근 권한은 UID/GID만으로 결정되지 않고 current credentials가 어떤 keyring을 소유·참조·possession하고 있는지, 해당 key의 permission mask가 무엇인지, key가 revoked·expired·invalidated 상태인지가 함께 결정한다. `request_key()`는 cache lookup처럼 보이지만 miss 시 userspace construction helper까지 이어지는 비동기 경계가 생기며, unlink와 object destruction도 같은 사건이 아니다. 따라서 secret lifetime을 안전하게 설계하려면 object identity, link graph, credential association, construction state, quota, revocation, garbage collection, evidence를 하나의 state machine으로 봐야 한다.

## CHAPTER 01 · Key는 이름이 아니라 serial number와 type·description·payload를 가진 커널 object다

Linux key subsystem에서 key는 단순 문자열 이름으로 식별되는 환경변수 같은 값이 아니다. 각 key object에는 kernel이 부여한 serial number가 있고, key type과 description, 소유 UID/GID, permission mask, payload, 상태 플래그와 lifetime 정보가 결합된다. 같은 description을 가진 key가 여러 keyring에 존재할 수 있으므로 description만 log에 남기면 어떤 object를 읽거나 revoke했는지 재현할 수 없다. 반대로 serial은 현재 부팅 인스턴스와 object lifetime 안에서 유용한 handle이지 영구 business identity가 아니다. 따라서 운영 코드가 key를 캐시할 때는 **serial은 runtime handle, type+description은 lookup selector, payload는 보호 대상 state**로 분리해야 한다. 실패 분석에서도 “이 이름의 key가 있었다”가 아니라 어떤 serial이 어느 시점에 어떤 keyring에 link되어 있었고 상태가 무엇이었는지를 확인해야 stale handle과 name collision을 구분할 수 있다.

## CHAPTER 02 · Keyring은 payload 저장 슬롯이 아니라 다른 key object를 link로 소유하는 검색 그래프다

Keyring 역시 하나의 key type이지만 핵심 역할은 다른 key들을 link로 참조해 검색 가능한 graph를 만드는 것이다. 한 key는 여러 keyring에 동시에 link될 수 있고, 어떤 keyring에서 unlink했다고 해서 즉시 key object 자체가 파괴되는 것은 아니다. 다른 keyring link나 open reference가 남아 있으면 object lifetime은 계속된다. 또한 keyring 안에 다른 keyring을 link할 수 있어 search path는 flat list가 아니라 graph traversal이 된다. 이 구조 때문에 “secret을 삭제했다”는 표현은 위험하다. 실제로는 특정 link를 제거했는지, 모든 reachable link가 사라졌는지, object reference count가 0으로 수렴했는지, revoke나 invalidate로 future use를 막았는지를 구분해야 한다. Security incident 대응에서 필요한 것은 file delete와 같은 단일 operation이 아니라 **reachability와 authority를 끊는 순서**다.

## CHAPTER 03 · Thread·process·session·user keyring은 서로 다른 credential scope에 결합된다

프로세스가 key를 사용할 수 있는 이유는 key subsystem이 current credentials와 여러 special keyring을 연결하기 때문이다. Thread-specific, process-specific, session, user, user-session keyring은 이름이 비슷해도 공유 범위와 inheritance가 다르다. Thread keyring에 놓은 key를 sibling thread가 당연히 볼 것이라 가정하면 동작이 깨질 수 있고, 반대로 session keyring에 secret을 넣으면 생각보다 넓은 process tree가 possession을 얻을 수 있다. Scope 선택은 편의 문제가 아니라 authority boundary다. 특히 service manager나 login session처럼 process tree가 길게 이어지는 환경에서는 session keyring이 예상보다 오래 살아남을 수 있다. 설계 시 “누가 이 key를 생성했는가”보다 **어느 credential scope에 link했고 그 scope가 fork/exec/session transition에서 어떻게 이어지는가**를 먼저 적어야 한다.

## CHAPTER 04 · Possession은 owner UID와 다른 독립적인 접근 조건이다

Key permission에는 possessor, user, group, other에 대한 권한 비트가 구분되어 있으며, possession은 단순히 key의 owner UID와 같다는 뜻이 아니다. Current task가 자신의 credential-associated keyring search path를 통해 해당 key에 도달할 수 있을 때 possessor 권한이 적용될 수 있다. 그래서 같은 UID의 두 process라도 서로 다른 session keyring topology를 가지면 접근 결과가 달라질 수 있고, 반대로 owner가 아닌 process도 적절한 possession path를 통해 허용된 operation을 수행할 수 있다. UID만 audit하면 “왜 한 프로세스는 되고 다른 프로세스는 안 되는가”를 설명하지 못하는 이유다. Authorization evidence에는 effective credential, special keyring association, reachability, permission class를 함께 기록해야 한다. **Ownership과 possession을 같은 축으로 합치지 않는 것**이 keyring 보안 모델의 첫 번째 invariant다.

## CHAPTER 05 · VIEW·READ·WRITE·SEARCH·LINK·SETATTR 권한은 서로 다른 state transition을 허용한다

Key permission mask는 하나의 allow/deny bit가 아니라 operation별 권한을 분리한다. VIEW는 metadata 관찰, READ는 payload read, WRITE는 payload update, SEARCH는 keyring traversal, LINK는 다른 keyring에 link하는 행위, SETATTR은 ownership·permission·timeout 같은 attribute 변경과 연결된다. SEARCH가 없으면 keyring 안에 존재하는 key도 lookup path에서 발견되지 않을 수 있고, READ가 없어도 특정 kernel subsystem이 key payload를 내부적으로 사용할 수 있는 경우가 있어 userspace read 가능성과 service usability를 동일시하면 안 된다. LINK 권한을 과도하게 주면 secret payload를 직접 읽지 못하더라도 더 넓은 possession graph로 authority가 전파될 수 있다. 따라서 최소권한 설계는 “read 금지” 하나로 끝나지 않고 **graph traversal, reference propagation, mutation, metadata exposure를 operation별로 분리**해야 한다.

## CHAPTER 06 · UID/GID ownership은 user namespace mapping과 함께 해석해야 한다

Key에는 owner UID/GID가 있고 permission evaluation에 사용되지만 container 환경에서는 numeric ID를 host 전역 identity로 단순 해석할 수 없다. User namespace는 credential이 바라보는 ID mapping과 capability scope를 바꿀 수 있고, key 관련 operation이 어느 namespace의 credential 기준으로 승인되는지 이해해야 한다. 동일한 숫자 `1000`이 서로 다른 namespace context에서 같은 principal을 의미한다고 가정하면 audit와 policy가 어긋난다. 또한 privileged helper가 host credential로 key를 생성한 뒤 container process에 link할 때는 생성자와 최종 possessor의 권한 축이 달라진다. Evidence에는 raw numeric UID뿐 아니라 **요청 task의 user namespace, mapped identity, capability context, key owner metadata**를 같이 남겨야 한다. Namespace boundary를 무시한 key sharing은 secret isolation을 예상보다 약하게 만들 수 있다.

## CHAPTER 07 · add_key()는 object 생성과 link를 하나의 요청에서 수행하지만 두 효과를 구분해 검증해야 한다

`add_key()`는 type, description, payload, destination keyring을 받아 key를 생성하거나 적절한 semantics에 따라 key를 확보한 뒤 destination에 link하는 사용자 API다. 호출이 성공해 serial을 받았다는 사실은 “원하는 장기 보관 구조가 완성됐다”와 동일하지 않다. Destination keyring 선택이 잘못되면 secret은 예상보다 짧게 살아남거나 반대로 넓은 session에서 오래 reachable할 수 있다. 동일 description 처리와 update 가능 여부도 key type에 따라 다르므로 application이 이름 중복을 자체 uniqueness contract로 가정하면 race가 생긴다. 생성 직후에는 serial, owner, permission, destination keyring, timeout을 읽어 **object 생성 결과와 graph link 결과를 분리해 확인**하는 것이 안전하다. 이 검증이 없으면 나중에 lookup failure가 payload 문제인지 topology 문제인지 구분하기 어렵다.

## CHAPTER 08 · keyctl update는 같은 serial의 payload generation을 바꾸므로 reader와 writer의 시점을 분리해야 한다

업데이트 가능한 key type에서는 기존 key serial을 유지한 채 payload를 교체할 수 있다. 이때 application이 serial을 immutable secret version으로 취급하면 audit trail이 틀어진다. 같은 serial을 두고 시간 t1과 t2에 payload 의미가 달라질 수 있기 때문이다. Concurrent reader가 update 전후 어느 payload를 사용했는지는 syscall 성공 여부만으로 복원되지 않을 수 있으며, key type 내부 locking과 consumer semantics를 고려해야 한다. Secret rotation을 구현할 때 “기존 serial update”와 “새 key 생성 후 link switch”는 rollback·observability·grace period 특성이 다르다. 강한 version evidence가 필요하면 새 object를 만들고 link topology를 전환하는 방식이 더 명확할 수 있다. 핵심은 **object identity와 payload version을 같은 것으로 취급하지 않는 것**이다.

## CHAPTER 09 · request_key()는 cache lookup이 아니라 miss 시 construction protocol을 시작할 수 있다

`request_key()`는 원하는 type과 description을 현재 search path에서 찾고, 없으면 policy에 따라 userspace helper를 호출해 새 key construction을 시도할 수 있다. 그래서 latency와 failure mode가 단순 in-kernel lookup보다 훨씬 크다. Helper 실행, credential 전달, external secret source 접근, timeout, negative result가 하나의 request path에 들어온다. Hot path에서 `request_key()`를 호출하는 서비스는 cache hit 비율만 보고 성능을 추정하면 안 되고 miss가 helper spawn과 원격 lookup으로 증폭될 가능성을 계산해야 한다. Security 측면에서도 helper는 trust boundary를 넘으므로 요청자가 전달한 description을 command argument나 configuration selector로 안전하게 처리해야 한다. **Lookup miss가 새로운 authority acquisition workflow를 열 수 있다는 점**이 핵심이다.

## CHAPTER 10 · Construction 중인 key에는 별도 상태가 있으며 같은 요청이 동시에 몰릴 때 deduplication 문제가 생긴다

여러 thread가 거의 동시에 같은 description의 key를 요청하면 모두 독립적으로 secret backend를 호출하도록 만들면 stampede와 inconsistent result가 생길 수 있다. Key subsystem은 construction in-progress 상태를 통해 요청을 조정하지만 application은 그 동안 발생하는 wait, interruption, timeout을 정상 cache hit와 구분해야 한다. Construction owner가 죽거나 helper가 실패했을 때 waiter가 어떤 오류를 받는지도 recovery policy에 영향을 준다. “key가 없다”와 “누군가 지금 만들고 있다”는 전혀 다른 상태다. 부하 테스트에서는 cold cache 상태에서 동시 `request_key()`를 집중시켜 helper invocation 수, waiter latency, duplicate backend fetch 여부를 관찰해야 한다. **Construction state를 보이지 않는 내부 세부사항으로 취급하면 thundering herd를 설명할 수 없다.**

## CHAPTER 11 · Negative instantiation은 실패를 캐시해 반복적인 construction 폭주를 막지만 오류를 오래 고정할 수 있다

Secret backend가 해당 principal이나 description에 대한 key를 제공할 수 없을 때 request construction은 negative result로 끝날 수 있다. 이 상태는 즉시 object 부재로 되돌아가는 것과 다르며 일정 기간 같은 lookup에 실패 결과를 재사용해 반복 helper invocation을 막을 수 있다. 이 메커니즘은 outage 동안 backend를 보호하지만, 잘못된 policy나 일시적 network failure를 너무 긴 negative timeout으로 저장하면 복구 후에도 계속 실패해 보일 수 있다. 운영자는 `ENOKEY`류 결과만 보고 backend가 현재도 실패 중이라고 단정하면 안 된다. Negative key의 timeout과 생성 시점, helper result를 함께 확인해야 한다. **Failure caching도 하나의 stateful control plane**이며 retry budget과 TTL을 맞춰야 한다.

## CHAPTER 12 · Timeout과 expiry는 object를 즉시 free하는 사건이 아니라 future usability를 바꾸는 lifetime transition이다

Key에 timeout을 설정하면 시간이 지난 뒤 expired 상태가 되어 정상 use가 거절될 수 있다. 하지만 expiration 순간과 메모리 회수 순간은 동일하지 않다. Link나 reference가 남아 있을 수 있고 garbage collection은 별도 조건과 시점에서 진행된다. 따라서 secret rotation 시스템이 “TTL이 지났으니 object는 사라졌다”고 가정하면 memory residency와 reachability를 과대평가 또는 과소평가할 수 있다. 반대로 expired key를 search path에 오래 남겨두면 lookup 결과가 원하는 새 key로 자연스럽게 전환되는지 key type과 search semantics를 확인해야 한다. Monitoring에서는 expiry timestamp, observed error, link count/topology, replacement key serial을 함께 보아야 한다. **Logical validity 종료와 physical destruction을 분리**하는 것이 lifetime 분석의 기본이다.

## CHAPTER 13 · revoke는 key를 더 이상 정상적으로 사용하지 못하게 만드는 security transition이다

Credential compromise나 secret rotation에서 중요한 operation은 단순 unlink가 아니라 revoke일 수 있다. Revoke된 key는 이미 획득한 reference가 있더라도 정상 operation이 제한되고 `EKEYREVOKED` 같은 명시적 실패로 드러날 수 있다. 이는 특정 keyring에서만 link를 제거하는 것보다 강한 incident containment primitive다. 다만 revoke가 payload bytes를 그 순간 물리적으로 zeroize하고 object를 free한다는 뜻으로 확대 해석하면 안 된다. Key type의 destroy path와 reference lifetime, garbage collection이 뒤따른다. Incident procedure는 **authority disable → link graph 정리 → reference drain → replacement provisioning**을 각각 증거로 확인해야 한다. 그래야 “노출된 secret을 못 쓰게 했다”와 “메모리에서 완전히 사라졌다”를 혼동하지 않는다.

## CHAPTER 14 · invalidate는 revoke와 같은 이름의 삭제가 아니며 garbage collection eligibility와 연결된다

Invalidation은 key를 사용 불가능한 상태로 만들고 garbage collector가 처리할 수 있는 대상으로 전환하는 데 사용될 수 있지만 revoke와 API semantics가 동일하지 않다. Application이 모든 종료 상태를 boolean `deleted=true` 하나로 기록하면 `EKEYREVOKED`, `EKEYEXPIRED`, invalidated lookup miss를 구분할 수 없다. 이 구분은 자동 복구 정책에도 중요하다. Expired key는 refresh 대상일 수 있고, revoked key는 보안 정책상 동일 identity를 즉시 재발급하면 안 될 수 있으며, invalidation은 cache coherency 목적일 수 있다. 상태 전이를 명시적으로 기록하고 operation별 errno를 보존하면 recovery가 원인에 맞게 달라진다. **Key lifecycle에는 여러 terminal-like state가 있으며 하나의 delete 개념으로 축약하면 안 된다.**

## CHAPTER 15 · unlink는 graph edge를 지우는 operation이고 key object의 마지막 reference 여부는 별도 문제다

`keyctl_unlink()`나 keyring clear로 link를 제거하면 특정 parent keyring에서 child가 더 이상 reachable하지 않게 된다. 그러나 다른 keyring link, process-held reference, kernel consumer reference가 남아 있으면 object는 계속 존재한다. 이 때문에 cleanup code가 unlink 성공을 secret destruction 증거로 기록하면 false assurance가 생긴다. 반대로 shared key를 한 keyring에서 unlink해도 다른 서비스가 계속 사용할 수 있으므로 shared ownership 계약이 필요하다. 삭제 작업 전에는 어느 keyring들이 해당 serial을 link하고 있는지, unlink 후 어떤 search path에서 더 이상 발견되지 않는지를 검증해야 한다. **Edge lifetime과 node lifetime은 독립적**이며, keyring은 이 차이를 직접 드러내는 좋은 ownership 예다.

## CHAPTER 16 · clear는 keyring의 여러 link를 한꺼번에 제거하지만 child object의 fate를 일괄 보장하지 않는다

Keyring clear는 container 안의 links를 제거하는 강한 topology mutation이지만, child key 각각의 object lifetime과 다른 parent links까지 없애지는 않는다. Session teardown에서 convenience cleanup으로 clear를 쓰면 한 번에 많은 authority edge가 사라지므로 concurrent consumer가 갑자기 lookup failure를 경험할 수 있다. Shared keyring에 대해 clear를 실행하는 admin tool은 삭제 대상 cardinality를 먼저 snapshot하고, 예상하지 못한 foreign link가 섞여 있는지 검증해야 한다. 특히 nested keyring을 clear할 때 child keyring 내부의 links까지 recursive delete된다고 가정하면 안 된다. **Container mutation의 범위를 parent edge set으로 한정해서 이해**해야 blast radius를 정확히 계산할 수 있다.

## CHAPTER 17 · join_session_keyring()은 process가 바라보는 session authority graph 자체를 바꾸는 transition이다

Session keyring을 새로 만들거나 이름 있는 session keyring에 join하는 동작은 단순 handle 조회가 아니다. 이후 current task와 descendant들이 possession하게 되는 search graph가 바뀔 수 있으므로 authorization 결과가 달라진다. 서비스 초기화 코드가 session join을 늦게 수행하면 그 이전에 생성한 children과 이후 children의 key visibility가 달라질 수 있다. 이름 있는 session keyring을 여러 process가 공유할 때는 누가 같은 namespace에 join할 수 있는지도 threat model에 포함해야 한다. Debugging에서는 key serial 하나만 보지 말고 session keyring serial의 변경 시점과 process tree를 함께 추적해야 한다. **Session keyring switch는 credential-associated root pointer의 변경**으로 보는 편이 정확하다.

## CHAPTER 18 · fork/clone에서 keyring association의 상속 규칙을 잘못 가정하면 child privilege가 달라진다

새 task 생성 시 모든 keyring scope가 같은 방식으로 복제되는 것은 아니다. Thread-specific state, process-specific state, session association은 각각 다른 공유·복사·상속 semantics를 가지며 clone flags와 credential lifetime과 결합된다. Parent가 child에게 secret을 전달하려고 “fork했으니 보일 것”이라고 가정하거나, 반대로 isolation을 기대하며 “새 process니까 안 보일 것”이라고 가정하면 둘 다 틀릴 수 있다. Privilege separation 전에 어떤 keyring을 detach/replace해야 하는지 명확히 해야 한다. 테스트는 parent/child 각각에서 동일 description lookup을 수행하고 실제 serial과 permission 결과를 비교해야 한다. **Process tree와 authority graph가 같은 모양으로 복제된다는 가정을 버리는 것**이 안전하다.

## CHAPTER 19 · exec는 address space를 바꾸지만 session keyring 같은 external credential state를 자동으로 초기화하지 않는다

`execve()`는 program image를 교체하지만 모든 kernel-side authority object가 새 프로그램과 함께 사라지는 것은 아니다. UID/GID, capability transition과 마찬가지로 keyring association도 exec 전후 contract를 확인해야 한다. Wrapper process가 secret을 얻은 뒤 less-trusted binary를 exec하는 구조라면 그 binary가 동일 session possession을 이어받는지 검토해야 한다. File descriptor leakage만 막고 keyring authority를 잊으면 secret exposure path가 남는다. 반대로 exec 후 필요한 key가 사라질 것이라 가정한 프로그램은 예상치 못한 stale authority를 계속 사용할 수 있다. **Program image lifetime과 credential-bound key authority lifetime은 별개**이므로 privilege boundary에서 둘을 함께 audit해야 한다.

## CHAPTER 20 · Persistent keyring은 로그인 session보다 긴 user-scoped lifetime을 의도적으로 제공한다

일반 session keyring은 login/session teardown과 강하게 연결될 수 있지만 persistent keyring은 user별로 더 긴 기간 credential material을 유지해야 하는 사용 사례를 지원한다. 이 편의성은 곧 더 긴 attack window와 quota consumption을 의미한다. Service가 재시작 후 secret 재조회 비용을 줄이려고 persistent scope를 선택하면, 사용자 logout 뒤에도 얼마나 남는지와 timeout 정책을 명시해야 한다. Persistent keyring을 process-local cache처럼 취급하면 운영자가 service restart로 secret이 사라질 것이라 잘못 기대할 수 있다. 관찰 항목에는 user identity, persistent keyring serial, timeout, 마지막 refresh 시점이 포함되어야 한다. **Persistence는 성능 최적화가 아니라 lifetime policy 선택**이다.

## CHAPTER 21 · Key quota는 메모리 보호 장치이자 실패를 외부화하는 resource accounting boundary다

사용자별 key 수와 payload bytes에는 quota가 적용될 수 있고, 한도를 넘으면 새 key 생성이나 link operation이 `EDQUOT` 등으로 실패할 수 있다. 이 실패를 secret backend outage로 오인하면 불필요한 retry가 더 많은 pressure를 만든다. Leak된 keyring links나 과도한 negative cache가 quota를 점유하면 unrelated authentication flow까지 실패할 수 있어 blast radius가 커진다. 운영자는 `/proc/key-users` 같은 관측 지점과 application serial inventory를 비교해 user별 key count와 quota usage를 추적해야 한다. Cleanup은 임의로 오래된 key를 지우기보다 owner, scope, timeout, active consumers를 확인한 뒤 수행해야 한다. **Quota exhaustion은 capacity fault이면서 authority graph leak의 증거**일 수 있다.

## CHAPTER 22 · Key type은 payload validation·update·read·destroy semantics를 결정하는 실행 계약이다

`user`, `logon`, `trusted`, `encrypted`, `asymmetric` 같은 key type은 이름만 다른 storage class가 아니다. 어떤 payload를 허용하고 userspace가 읽을 수 있는지, update가 가능한지, kernel consumer가 어떻게 해석하는지, destroy 시 어떤 정리가 필요한지가 type별로 다르다. Application이 모든 key를 opaque byte array로 다루면 type-specific security guarantee를 잃는다. 예를 들어 userspace read를 허용하지 않는 type은 secret material을 application address space에 노출하지 않는 설계를 가능하게 한다. 반대로 type을 바꾸면서 같은 description을 재사용하면 search 결과가 의도한 object인지 type까지 검증해야 한다. **Type은 payload format과 authority semantics를 동시에 고정하는 part of identity**다.

## CHAPTER 23 · trusted와 encrypted key는 plaintext secret을 userspace에 덜 노출시키기 위한 다른 trust chain을 가진다

Trusted key 계열은 TPM 같은 trust anchor를 활용해 key material을 보호할 수 있고, encrypted key는 다른 kernel key를 master로 사용해 암호화된 형태의 payload를 관리할 수 있다. 둘을 단순히 “암호화된 keyring 값”으로 묶으면 root of trust와 recovery 특성을 놓친다. Hardware-backed path는 TPM availability, PCR/policy, boot state 같은 외부 조건에 의존할 수 있고, encrypted key는 master key lifetime과 dependency graph가 중요하다. Master가 revoke되거나 unavailable해지면 dependent secret 복호화가 연쇄 실패할 수 있다. 따라서 rotation과 backup을 설계할 때 child payload만 저장할 것이 아니라 **master identity, trust source, sealing 조건, dependency failure**를 함께 문서화해야 한다.

## CHAPTER 24 · Asymmetric key는 private payload 저장보다 certificate와 signature verification object lifecycle에 가깝다

Asymmetric key type은 X.509 certificate, public key, signature verification 같은 kernel trust decisions에 사용될 수 있다. 이 영역에서는 description collision보다 key identifier, certificate subject, issuer, fingerprint, trust chain이 더 중요한 evidence가 된다. Kernel module verification이나 integrity subsystem이 keyring을 trust store로 사용할 때 user application의 ordinary secret cache와 같은 운영 정책을 적용하면 안 된다. 새 certificate를 link하는 것은 즉시 기존 signer를 불신하는 것과 다르고, revoke/blacklist 정책은 별도 state transition이다. 검증 실패 시 단순 `EACCES`보다 어떤 keyring에서 어떤 signer를 찾았는지와 certificate validity를 확인해야 한다. **Keyring이 authorization data structure이자 trust anchor registry로도 동작할 수 있음**을 구분해야 한다.

## CHAPTER 25 · Keyring restriction은 future link를 제한해 trust store가 임의 확장되는 것을 막는다

특정 keyring은 어떤 key가 새로 link될 수 있는지 restriction policy를 가질 수 있다. 이는 현재 들어 있는 key의 read permission을 제한하는 기능과 다르며, **미래 graph mutation의 admissibility**를 제어한다. Trust store에서 “현재 contents가 안전하다”만 검사하고 누구나 새 signer를 link할 수 있게 두면 시간이 지나며 보안 경계가 붕괴한다. Restriction을 설정한 뒤에는 허용된 signer chain이나 key type 조건에 맞지 않는 link가 실제로 실패하는지 negative test가 필요하다. 운영 중 restriction을 바꾸거나 제거할 수 있는 주체도 별도 privileged path로 관리해야 한다. Policy evidence에는 keyring serial, restriction kind, attempted child identity, errno를 남겨야 한다.

## CHAPTER 26 · request-key userspace helper는 kernel과 external secret source 사이의 정책 경계다

`request_key()` miss가 userspace helper로 넘어가면 kernel은 더 이상 secret source의 인증·network retry·format validation을 직접 책임지지 않는다. Helper configuration은 어떤 key type/description/caller context를 어떤 program에 전달할지 결정하는 policy table이 되며, 잘못된 wildcard는 예상보다 넓은 요청을 privileged helper에 노출할 수 있다. Description 같은 caller-controlled data를 shell command 구성에 직접 삽입하면 injection 위험도 생긴다. Helper는 최소 privilege로 실행하고 external backend credential을 별도 보호하며, 성공 전에 payload 크기와 type contract를 검증해야 한다. **Kernel keyring의 보안은 helper 경계를 넘는 순간 userspace policy와 함께 평가**해야 한다.

## CHAPTER 27 · Concurrent search와 link/unlink는 “검색 직후에도 같은 graph”라는 보장을 주지 않는다

한 thread가 keyring을 search해 serial을 얻은 직후 다른 thread가 그 link를 제거하거나 key를 revoke할 수 있다. Search 성공은 이후 operation까지 topology가 frozen된다는 lock guarantee가 아니다. Serial reference를 실제로 획득했는지, subsequent syscall이 어떤 state를 다시 검증하는지에 따라 race 결과가 달라진다. Application이 `search → permission assumed → use`를 userspace transaction처럼 생각하면 TOCTOU가 생긴다. 안전한 설계는 각 privileged operation이 kernel에서 현재 key state와 permission을 다시 확인하도록 하고, failure를 정상 race outcome으로 처리한다. 테스트에서는 반복적인 link/unlink/revoke와 concurrent lookup/use를 섞어 stale serial, unexpected success, livelock이 없는지 확인해야 한다.

## CHAPTER 28 · /proc/keys와 /proc/key-users는 debugging evidence이지만 visibility 자체가 credential policy의 영향을 받는다

Key subsystem은 `/proc/keys`, `/proc/key-users` 같은 관측 지점을 제공해 serial, flags, usage, expiry, owner, description, quota 정보를 확인하는 데 도움을 준다. 하지만 이 출력이 시스템 전체 secret graph의 완전한 inventory라고 가정하면 안 된다. Current reader credentials와 permission policy에 따라 보이는 key가 달라질 수 있고, payload를 직접 노출하지 않는 것이 정상이다. 장애 분석 스크립트는 proc snapshot만 저장하지 말고 실행 UID/GID, namespace, session keyring identity를 같이 기록해야 비교 가능하다. 또한 description에 민감정보를 넣으면 payload를 숨겨도 metadata leak이 될 수 있다. **Observability channel도 authorization boundary 안에 있다**는 점을 유지해야 한다.

## CHAPTER 29 · ENOKEY·EKEYREVOKED·EKEYEXPIRED·EACCES·EDQUOT는 서로 다른 recovery를 요구한다

Key operation failure를 모두 “secret 없음”으로 매핑하면 recovery loop가 위험해진다. `ENOKEY`는 lookup 실패나 unavailable state를 시사할 수 있고, `EKEYREVOKED`는 보안상 사용 중지된 object, `EKEYEXPIRED`는 lifetime 만료, `EACCES`는 permission/possession 문제, `EDQUOT`는 resource accounting 문제를 가리킨다. Revoked key에 자동 재생성을 수행하면 incident response를 우회할 수 있고, quota failure에 retry를 반복하면 더 큰 부하를 만든다. Error class마다 refresh, re-authentication, topology repair, operator alert, cleanup 중 어느 path로 갈지 명시해야 한다. **Errno는 단순 로그 문자열이 아니라 state machine transition selector**로 취급할 가치가 있다.

## CHAPTER 30 · 신뢰할 수 있는 keyring 운영은 object·graph·credential·time·quota evidence를 함께 닫아야 한다

Keyring 문제를 재현하려면 “keyctl 명령이 성공했다”는 기록만으로 부족하다. 최소 evidence set은 key serial/type/description, owner와 permission, parent keyring serial과 link 관계, current thread/process/session association, relevant UID/GID와 namespace, timeout/expiry, revoked/invalidated state, quota usage, helper construction 결과, 최종 errno다. Secret rotation 실험에서는 old key revoke, replacement create, link switch, old reachability 제거, consumer convergence를 순서대로 증명해야 한다. Stress test는 concurrent request/update/unlink, helper failure, quota exhaustion, process exec, session switch를 조합해야 한다. 이 증거가 있어야 **“secret이 존재한다”가 아니라 “누가 언제 어떤 경로로 어떤 상태의 key를 사용할 수 있는가”**라는 실제 보안 계약을 검증할 수 있다.
