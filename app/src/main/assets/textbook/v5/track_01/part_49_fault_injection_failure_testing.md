# PART 49 · Fault Injection and Failure Testing — errors, crashes, resource exhaustion, invariants

정상 경로가 잘 동작한다는 사실만으로 시스템의 신뢰성을 설명할 수 없다. 실제 장애는 allocation 실패, disk full, short write, fsync 오류, process crash, timeout, OOM kill, permission 변경처럼 **경계 중간에서 실패가 끼어드는 형태**로 나타난다. Fault injection은 무작위로 망가뜨리는 행위가 아니라 failure model을 명시하고, 정확한 injection point와 oracle을 두어 불변식이 유지되는지 검증하는 실험이다.

---

## CHAPTER 01 · fault model이 없으면 failure test는 사건 목록 나열에 그친다

Fault injection을 시작하기 전에 어떤 실패를 시스템이 견뎌야 하는지 범위를 정한다. Process crash, host power loss, disk write error, timeout, memory exhaustion은 서로 다른 보장을 깨뜨린다. 예를 들어 process crash는 kernel page cache가 남을 수 있지만 전원 손실은 volatile state 전체를 잃는다.

Failure model은 “동시에 몇 개가 실패할 수 있는가”, “storage가 reorder할 수 있는가”, “network가 duplicate/reorder할 수 있는가”까지 포함한다. 모델 밖의 장애를 테스트하지 않는 것이 아니라, 어떤 결과를 요구하지 않는지 분명히 하는 것이다.

Test report에는 사용한 fault model을 함께 남긴다. 같은 crash test라도 가정이 다르면 PASS의 의미가 완전히 달라진다.

## CHAPTER 02 · injection point는 실패 전후의 state transition을 정확히 가르는 위치다

`save()`가 실패한다고만 테스트하면 어느 내부 단계에서 실패했는지 알 수 없다. 임시 파일 생성 전, payload write 중간, fsync 직전, rename 직후처럼 state transition 사이에 injection point를 둬야 recovery invariant를 확인할 수 있다.

좋은 injection point는 코드 구조와 persistent state 경계를 모두 반영한다. API entry에만 오류를 넣으면 깊은 cleanup path와 partial progress를 놓친다. 반대로 모든 instruction에 fault를 넣는 방식은 결과 해석이 어려워진다.

각 point에는 이름과 expected state를 연결한다. “P3에서 crash하면 old 또는 new generation 중 하나만 보여야 한다”처럼 oracle이 명확해야 자동 회귀 검사가 가능하다.

## CHAPTER 03 · deterministic trigger는 동일한 실패를 정확히 재현하게 만든다

Random chaos는 넓은 공간을 탐색하는 데 유용하지만 실패 후 같은 순간을 재현하기 어렵다. Counter 기반 `N번째 allocation`, 특정 file path의 `두 번째 fsync`, 특정 request id 같은 deterministic trigger를 두면 사건을 다시 만들 수 있다.

Trigger는 production 로직에 영향을 최소화해야 한다. Injection 기능이 timing을 크게 바꾸면 원래 race가 사라지거나 새로운 race가 생길 수 있다. Test hook 자체의 overhead와 synchronization을 문서화한다.

실패가 발견되면 trigger configuration을 regression artifact로 저장한다. Random seed만으로 충분하지 않은 경우 fault sequence와 operation index까지 함께 기록한다.

## CHAPTER 04 · operation 전 실패와 후 실패는 caller가 보는 의미가 다르다

외부 API가 timeout을 반환했다고 해서 server가 작업을 수행하지 않았다는 뜻은 아니다. 요청 처리 전 실패, side effect 후 response 전 실패, response 전송 중 실패를 구분해야 retry 안전성을 판단할 수 있다. Storage도 write 이전 error와 write 완료 후 acknowledgement 손실이 다르다.

Failure test는 상태 변화와 관찰 결과를 별도로 기록한다. Caller가 error를 받았는데 side effect가 이미 commit된 case를 의도적으로 만든다. 이때 idempotency key나 read-after-retry 정책이 제대로 동작하는지 확인한다.

Exactly-once처럼 들리는 보장은 대개 여러 계층의 protocol에 의존한다. Injection으로 ambiguous outcome을 만들고 application이 어떻게 reconcile하는지 검증한다.

## CHAPTER 05 · allocation failure는 cleanup과 partial construction을 검증하는 강한 도구다

Heap allocation은 정상 환경에서 거의 항상 성공해 error path가 오래 실행되지 않을 수 있다. `N번째 allocation 실패`를 주입하면 object construction 중간에 만들어진 subresource가 모두 해제되는지 확인할 수 있다. 단순 OOM stress보다 위치를 통제하기 쉽다.

C/C++에서는 null/error propagation과 ownership 이전 지점이 핵심이고, managed runtime에서도 native allocation·large object reserve·buffer creation이 실패할 수 있다. Allocation 실패를 catch한 뒤 더 많은 memory를 할당하는 logging path도 문제를 키울 수 있다.

검증은 leak, double free, partially initialized object publication을 본다. 실패 후 재시도에서 global state가 정상 baseline으로 돌아오는지도 확인한다.

## CHAPTER 06 · disk full은 write API와 metadata operation 모두를 실패시킬 수 있다

디스크 여유가 없으면 payload append뿐 아니라 directory entry 생성, journal metadata, temporary file 생성도 실패할 수 있다. “파일 크기가 작으니 저장 가능하다”는 판단은 filesystem metadata와 reserved space를 무시한다.

Fault test에서는 data partition과 log partition을 분리해 어느 쪽 고갈이 서비스에 어떤 영향을 주는지 본다. Error logging이 같은 가득 찬 filesystem을 사용하면 원인 기록조차 실패할 수 있다.

Recovery 후 space를 확보했을 때 application이 자동으로 정상화되는지도 중요하다. 반복 실패로 queue가 무한 성장하거나 corrupted temp file을 계속 재사용하지 않는지 확인한다.

## CHAPTER 07 · short write는 일부 byte만 기록된 뒤 성공한 양을 반환하는 정상 API semantics다

`write(fd, buf, n)`이 항상 n byte를 기록한다는 가정은 잘못이다. Signal, nonblocking I/O, resource 상황에 따라 일부만 진행될 수 있으므로 caller는 반환된 byte 수만큼 pointer를 이동해 나머지를 반복해야 한다. Zero progress와 error도 별도로 처리한다.

Fault injection으로 1 byte, 절반, 마지막 1 byte를 제외한 길이처럼 다양한 short write를 강제한다. Loop가 offset을 잘못 갱신하거나 동일 데이터를 중복 기록하는 bug를 찾을 수 있다.

Persistent format이라면 partial record가 crash 후 어떻게 감지되는지도 본다. Length와 checksum이 recovery scanner의 stop condition으로 올바르게 작동해야 한다.

## CHAPTER 08 · fsync failure는 durability가 확보되지 않았다는 사실을 상위 계층에 전달해야 한다

Write가 성공해 page cache에 들어갔다고 durable한 것은 아니다. `fsync`가 error를 반환하면 저장 장치나 filesystem이 데이터를 안정적으로 기록하지 못했을 수 있다. 이 오류를 로그만 남기고 success로 반환하면 application의 commit 의미가 깨진다.

Fault test는 fsync error 직후 process crash를 결합해 caller가 어떤 상태를 보게 되는지 확인한다. Retry가 안전한지, file descriptor를 계속 사용할 수 있는지 platform semantics도 검토해야 한다.

Durability-sensitive API는 “memory에 수락됨”과 “stable storage에 commit됨”을 구분한다. Injection 결과를 통해 실제 return point가 어느 수준의 보장을 제공하는지 문서화한다.

## CHAPTER 09 · rename failure는 atomic publish protocol의 마지막 단계를 끊는다

`temp 작성 → fsync → rename` 패턴에서 rename이 실패하면 새 파일이 publish되지 않아야 한다. 그러나 temp file은 남을 수 있고 retry가 기존 temp와 충돌할 수 있다. Permission, target 존재, filesystem boundary 등 여러 이유로 rename은 실패한다.

Injection은 rename 직전·중간·직후 관찰 지점을 만든다. 실패했다고 응답했는데 target이 이미 교체된 ambiguous outcome도 caller가 처리할 수 있어야 한다.

Startup cleanup은 오래된 temp와 현재 generation을 구분해야 한다. 이름 pattern만 보고 무조건 삭제하면 아직 사용 중인 writer의 파일을 제거할 수 있다.

## CHAPTER 10 · corrupt read는 checksum과 parser의 fail-closed 동작을 검증한다

Storage가 성공적으로 읽혔더라도 bit corruption, stale block, memory error로 content가 틀릴 수 있다. Fault injection은 특정 byte를 뒤집거나 length field를 변형해 integrity check가 실제로 corruption을 잡는지 확인한다.

Checksum mismatch 후 parser가 “가능한 만큼 계속 읽기”로 진행하면 더 큰 state corruption을 만들 수 있다. 어떤 format은 record 단위로 skip 가능하지만, 그 경계가 신뢰할 수 있을 때만 안전하다.

Corruption test는 payload뿐 아니라 metadata를 포함한다. Header length, version, checksum 자체가 손상된 경우에도 bounded failure를 보장해야 한다.

## CHAPTER 11 · process crash는 userspace cleanup을 모두 건너뛰는 failure다

`kill -9`나 abrupt termination은 destructor, finally, buffer flush를 보장하지 않는다. 따라서 crash consistency는 정상 shutdown 코드를 실행해 확인할 수 없다. Persistent invariant는 process가 임의의 instruction 경계에서 사라져도 recovery 가능한 형태로 설계해야 한다.

Crash injection은 operation 단계별로 process를 죽이고 다시 시작해 state를 검사한다. Restart script가 test state를 수정하지 않도록 recovery 시작 전 raw artifact를 보존하는 것이 좋다.

“재시작이 됐다”만 PASS 조건으로 두지 않는다. committed transaction 보존, uncommitted state 비노출, resource index consistency 같은 불변식을 자동 검사한다.

## CHAPTER 12 · crash matrix는 operation 단계와 crash 위치의 조합을 체계적으로 덮는다

복구 프로토콜이 8단계라면 각 단계 전후 crash를 모두 만들어 matrix로 관리할 수 있다. 일부 지점만 수동 테스트하면 가장 취약한 window를 우연히 건너뛸 수 있다. 단계가 바뀌면 matrix도 code와 함께 업데이트한다.

각 cell에는 expected visible generation과 허용 가능한 temp/log 상태를 정의한다. Outcome이 여러 개 허용되는 지점도 명시해 false failure를 피한다. “어떤 상태든 열리면 성공” 같은 느슨한 oracle은 피한다.

Matrix 실행은 deterministic trigger와 결합한다. 실패한 cell을 단독 재생할 수 있어야 debugging이 빠르다.

## CHAPTER 13 · power loss는 process crash보다 더 넓은 volatile state를 없앤다

Process crash 후에는 kernel page cache와 storage controller state가 계속 살아 있을 수 있다. 실제 power loss에서는 이 volatile state도 사라질 수 있으므로 write ordering과 flush/FUA 보장이 중요해진다. VM 강제 종료가 physical power loss를 완전히 재현한다고 가정하면 안 된다.

실험 환경에 따라 block device fault emulator, VM snapshot, 실제 power-cut rig 등 fidelity가 다르다. 어떤 계층까지 state를 제거하는지 문서화한다.

Power-loss test는 rename만 보지 않고 directory durability와 metadata ordering까지 포함한다. File 내용은 durable하지만 이름이 사라지는 경우도 설계상 중요하다.

## CHAPTER 14 · clock jump는 wall-clock 기반 timeout과 ordering 가정을 깨뜨린다

NTP adjustment, manual clock change, VM suspend/resume으로 wall clock이 앞뒤로 움직일 수 있다. Duration 측정과 deadline에 realtime clock을 사용하면 timeout이 즉시 만료되거나 지나치게 늘어날 수 있다. Monotonic clock이 필요한 이유다.

Fault injection으로 시간을 전진·후퇴시키고 token expiry, lease, retry backoff가 어떤 영향을 받는지 본다. Distributed system에서는 각 node clock skew도 별도 failure dimension이다.

Persistent timestamp가 business ordering에 사용된다면 동일하거나 역전된 값도 처리할 수 있어야 한다. Clock을 sequence number처럼 사용하지 않는다.

## CHAPTER 15 · timeout은 remote side가 실패했다는 증거가 아니라 관찰 기한 초과다

Client timeout은 response를 제때 받지 못했다는 뜻일 뿐 server가 처리하지 않았다는 뜻이 아니다. Network delay, queueing, server stall, response loss가 모두 같은 timeout으로 보일 수 있다. 따라서 retry는 duplicate side effect 가능성을 고려해야 한다.

Injection은 server가 commit한 직후 response를 지연시키는 case를 포함한다. Idempotency key, deduplication, status query가 ambiguous outcome을 해결하는지 확인한다.

Timeout 값을 줄이는 것만으로 resilience가 좋아지지 않는다. Too-short timeout은 정상 느린 요청을 실패로 바꾸고 retry storm을 만들 수 있다.

## CHAPTER 16 · cancellation은 실행 중인 작업의 ownership 정리를 시험한다

사용자가 화면을 닫거나 upstream deadline이 만료되면 작업이 취소될 수 있다. Cancellation point에서 file, lock, transaction, temporary buffer가 올바르게 정리되는지 검증해야 한다. 일부 side effect가 이미 commit됐다면 취소가 rollback을 의미하지 않을 수도 있다.

Injection은 각 await/blocking point에서 cancellation을 발생시킨다. `finally`가 실행되더라도 cleanup 자체가 blocking되거나 실패할 수 있음을 고려한다.

취소된 작업이 background에서 계속 실행되는 orphan 상태도 관찰한다. CPU·I/O가 계속 소비되면 load shedding 효과가 사라진다.

## CHAPTER 17 · OOM kill은 application cleanup 기회를 주지 않고 process를 제거할 수 있다

Memory pressure가 심하면 kernel 또는 cgroup이 process를 kill할 수 있다. Application-level allocation exception과 달리 destructor나 shutdown hook이 실행되지 않을 수 있다. 따라서 persistent correctness는 OOM kill을 process crash와 같은 범주로 견뎌야 한다.

Test에서는 memory.max 같은 제한을 낮춰 실제 kill 경로를 만들고 supervisor가 restart를 어떻게 처리하는지 본다. Restart loop가 cache warmup과 재할당으로 다시 OOM을 만드는지 확인한다.

OOM 원인 분석에는 peak RSS뿐 아니라 cgroup event와 memory pressure를 기록한다. “프로세스가 그냥 사라졌다”는 증상을 명확한 fault로 연결한다.

## CHAPTER 18 · CPU starvation은 error 없이 progress만 멈추는 failure다

Thread가 runnable이지만 CPU quota, 높은 우선순위 경쟁자, oversubscription 때문에 거의 실행되지 못할 수 있다. 이 경우 API error는 없지만 heartbeat와 deadline은 실패한다. 일반 fault injection이 return code만 바꾸면 이 종류를 놓친다.

CPU quota를 극단적으로 낮추거나 competing workload를 배치해 starvation을 재현한다. Lock holder가 starvation되면 다른 thread 전체가 멈추는 우선순위 역전과 비슷한 증상도 나타난다.

Watchdog는 process 생존이 아니라 forward progress를 측정해야 한다. Queue age, completed operations, scheduler delay를 함께 본다.

## CHAPTER 19 · lock contention injection은 timing-sensitive failure를 안정적으로 확대한다

Race와 deadlock은 짧은 critical section에서는 드물게 나타난다. Test hook으로 특정 lock을 오래 잡거나 barrier에서 thread를 정렬하면 문제 window를 의도적으로 넓힐 수 있다. 단순 sleep을 무작위로 넣는 것보다 재현성이 높다.

목표는 production보다 느리게 만드는 것이 아니라 ordering 가정을 깨뜨리는 것이다. Lock A 획득 후 B 직전 정지처럼 dependency edge를 명확히 선택한다.

실패 후 thread dump에서 lock graph를 확인하고 regression test에 같은 schedule constraint를 보존한다. Race seed만 남겨서는 scheduler 변화로 재현이 사라질 수 있다.

## CHAPTER 20 · EINTR은 syscall이 signal에 의해 중단될 수 있다는 정상 경계 조건이다

Blocking syscall은 signal delivery로 중단되어 EINTR을 반환할 수 있다. 어떤 syscall은 자동 restart되고 어떤 것은 partial progress를 반환할 수 있으므로 무조건 “EINTR이면 동일 호출 반복” 규칙을 적용하면 안 된다.

Fault test는 signal을 정확한 blocking 구간에 전달해 wrapper가 offset, timeout, cancellation state를 보존하는지 본다. Deadline 기반 호출은 restart할 때 남은 시간을 다시 계산해야 한다.

EINTR path는 평소 거의 실행되지 않아 stale bug가 숨어 있기 쉽다. Platform/library가 restart semantics를 바꾸는 버전에서도 regression을 유지한다.

## CHAPTER 21 · file descriptor exhaustion은 unrelated 기능까지 연쇄적으로 실패시킨다

Process가 fd limit에 도달하면 socket accept, file open, pipe 생성, logging까지 동시에 실패할 수 있다. Error를 기록하려고 새 file을 여는 코드가 실패해 원인 정보가 사라질 수도 있다. Reserve fd 같은 전략을 사용하는 이유다.

Injection은 leak로 천천히 limit에 도달하는 경우와 갑자기 limit을 낮추는 경우를 모두 본다. 기존 connection을 유지하면서 신규 accept를 어떻게 제한하는지도 중요하다.

Recovery 후 fd 수가 baseline으로 돌아오는지 확인한다. 단순히 limit을 크게 올려 test를 통과시키면 leak 원인을 숨길 수 있다.

## CHAPTER 22 · permission change는 실행 중에도 resource 접근 권한을 바꿀 수 있다

파일 mode, credential, sandbox policy가 deployment 중 바뀌면 이전에 열 수 있던 resource가 갑자기 거부될 수 있다. Startup check가 성공했다는 이유로 runtime access가 영구 보장되는 것은 아니다.

Fault injection으로 read/write 사이에 permission을 바꾸고 error가 상위 계층에 정확히 전달되는지 본다. 권한 오류를 `not found`처럼 뭉뚱그리면 운영자가 설정 문제를 진단하기 어렵다.

Security boundary에서는 fallback path가 더 넓은 권한을 사용하지 않는지 검증한다. Permission failure를 피하려고 privileged helper로 자동 우회하면 정책을 무력화할 수 있다.

## CHAPTER 23 · config corruption은 parser뿐 아니라 safe default 정책을 검증한다

설정 파일 일부가 잘리거나 type이 바뀌거나 checksum이 맞지 않을 수 있다. Parser가 crash하지 않는 것만으로 충분하지 않다. 어떤 값이 유효하지 않을 때 이전 known-good config를 유지할지, startup을 중단할지 명확해야 한다.

Security-sensitive 설정이 깨졌을 때 permissive default로 돌아가면 위험하다. 반대로 optional tuning 하나 때문에 전체 서비스가 시작하지 못하면 availability 문제가 된다. Field를 risk class로 나누어 정책을 정한다.

Corruption test는 syntax error뿐 아니라 semantic invalid value, duplicate key, unknown version을 포함한다. Error message에 secret config value를 그대로 출력하지 않는다.

## CHAPTER 24 · upgrade 중 crash는 두 버전이 공유하는 persistent state를 시험한다

Schema migration, file format 변경, binary 교체 중 process가 죽으면 old와 new artifact가 섞일 수 있다. 새 버전이 halfway migration을 감지하고 재개하거나 rollback할 수 있어야 한다. 단일 clean upgrade test로는 이 상태를 만들 수 없다.

각 migration step 사이에 crash를 넣고 old binary와 new binary 각각이 state를 읽을 수 있는지 compatibility policy에 맞춰 확인한다. Irreversible step은 명시적 cutover point로 관리한다.

Upgrade test artifact에는 version pair와 migration generation을 기록한다. 최신→최신만 테스트하면 실제 rolling environment를 반영하지 못한다.

## CHAPTER 25 · chaos test의 scope는 검증하려는 invariant보다 넓어지지 않아야 한다

Production chaos에서 여러 장애를 동시에 무작위로 넣으면 실제 resilience를 볼 수 있지만 원인과 coverage를 해석하기 어렵다. 먼저 lab에서 단일 fault와 명확한 oracle로 검증한 뒤 범위를 넓히는 편이 효과적이다.

Scope에는 대상 service, tenant, fault duration, 최대 동시 실패 수를 명시한다. “전체 cluster에서 random kill” 같은 실험은 blast radius가 너무 커 학습 대비 위험이 높을 수 있다.

실험 종료 조건도 필요하다. Error budget 소진, data integrity alarm, rollback failure가 발생하면 자동 중단하도록 guardrail을 둔다.

## CHAPTER 26 · blast radius는 failure injection 자체의 실패까지 제한해야 한다

Fault tool이 버그를 내거나 잘못된 selector를 적용할 수 있다. 따라서 target을 label·instance count로 제한하고, production 전체에 적용할 권한을 기본값으로 주지 않는다. 안전한 chaos system은 자기 자신도 untrusted actuator로 취급한다.

실험 전 대상 목록을 snapshot으로 확인하고 최대 비율을 강제한다. Control plane과 recovery path는 실험 대상에서 제외할 수 있다.

Rollback command가 같은 장애로 막히는 상황도 고려한다. Fault duration에 TTL을 두면 orchestrator가 사라져도 자동 복구될 수 있다.

## CHAPTER 27 · injection evidence가 없으면 실패가 실제로 주입됐는지 증명할 수 없다

Test가 통과했어도 fault trigger가 실행되지 않았다면 아무 것도 검증하지 않은 것이다. 각 injection point는 hit count, timestamp, target operation을 기록해 test artifact에 남겨야 한다. 이 정보가 CLEAN PASS의 근거가 된다.

단, 민감한 payload 전체를 log할 필요는 없다. Operation id와 fault type, sequence number면 충분한 경우가 많다. High-frequency test에서 logging이 timing을 크게 바꾸지 않도록 bounded buffer를 사용할 수 있다.

Oracle 결과와 injection evidence를 같은 run id로 묶는다. “에러가 안 났으니 성공”이 아니라 “정확히 fault가 발생했고 invariant가 유지됐다”를 증명한다.

## CHAPTER 28 · invariant oracle은 결과가 정상인지 자동 판정하는 핵심이다

Fault test의 가치가 사람이 로그를 보고 느낌으로 판단하는 데 의존하면 coverage를 늘릴 수 없다. Database라면 uniqueness·foreign key·committed visibility, file format이라면 generation·checksum·index consistency처럼 machine-checkable invariant를 정의한다.

Oracle은 implementation detail보다 외부 보장에 가깝게 만든다. 내부 temp file 개수는 버전마다 달라질 수 있지만 “마지막 committed state가 읽힌다”는 보장은 더 안정적이다.

Recovery 후 oracle뿐 아니라 다음 write가 정상 동작하는지도 본다. 읽기만 가능한 손상 상태를 PASS로 오인하지 않는다.

## CHAPTER 29 · failure seed는 발견한 사건을 영구 회귀 시험으로 바꾼다

Fuzz·chaos에서 rare failure를 찾았으면 seed와 fault schedule을 저장해 deterministic regression으로 승격한다. 단순 incident ticket만 남기면 scheduler, hardware, timing이 바뀐 뒤 같은 bug가 다시 나타날 수 있다.

Regression에는 최소 재현 sequence를 만들면 좋다. 여러 fault 중 실제 필요한 것만 남기면 test가 빨라지고 원인이 선명해진다. 실패한 artifact의 hash와 software version도 기록한다.

새 구현이 들어가도 기존 seed corpus를 계속 돌린다. 과거 버그 클래스가 다른 code path에서 재발하는 것을 막는 안전망이 된다.

## CHAPTER 30 · fault injection contract는 ‘실패를 만들었다’가 아니라 ‘불변식을 검증했다’다

완성된 failure test는 세 요소를 갖는다. 어떤 fault가 가능한지 정의한 model, 그 fault가 실제로 발생했음을 보여주는 injection evidence, 그리고 결과 state를 판정하는 invariant oracle이다. 셋 중 하나가 없으면 PASS라는 말의 의미가 약해진다.

테스트 순서는 정상 path → deterministic single fault → crash matrix → 제한된 복합 fault로 넓힌다. 매 PART나 매 operation마다 전체 chaos를 반복하는 대신 변경된 failure surface를 중심으로 증분 검증하는 것이 효율적이다.

운영 환경에서 chaos를 수행할 때는 blast radius와 자동 복구를 먼저 제한한다. 신뢰성은 장애를 많이 만드는 것이 아니라 **예상한 장애에서 시스템이 어떤 상태로 남는지 증명하는 능력**이다.
