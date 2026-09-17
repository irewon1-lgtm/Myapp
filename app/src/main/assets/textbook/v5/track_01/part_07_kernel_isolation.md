# PART 07 · kernel boundary와 격리 — identity, policy, resource ownership

운영체제 보안은 `관리자/일반 사용자` 두 단계가 아니다. 실제 허용 여부는 **요청 주체의 identity, 대상 resource, operation, namespace, capability, mandatory policy, resource limit**의 조합으로 결정된다. 디버깅과 보안 검토는 이 조합을 분해해야 한다.

---

## CHAPTER 01 · syscall boundary는 untrusted user state가 kernel로 들어오는 검증 지점이다

application API와 system call은 동일한 층이 아니다. runtime/library는 user space에서 buffering·validation·state management를 수행한 뒤 하나 이상의 syscall을 호출할 수 있다. kernel entry에서는 user pointer, length, flag, descriptor가 모두 신뢰되지 않은 입력이다.

kernel은 user address가 접근 가능한지, length arithmetic이 overflow하지 않는지, descriptor가 현재 process에 유효한지, operation 권한이 있는지 확인해야 한다. syscall attack surface가 큰 이유는 잘못된 pointer 하나가 privileged memory에 직접 영향을 줄 수 있기 때문이다.

검증은 pointer가 현재 읽힌다는 사실만 확인하는 것으로 끝나지 않는다. user memory는 다른 thread에 의해 바뀔 수 있으므로 kernel이 같은 pointer를 여러 번 해석하면 check와 use 사이 값이 달라질 수 있다. 길이×개수 계산, nested pointer, variable-length structure는 overflow와 inconsistent snapshot을 특히 조심해야 한다. robust boundary는 필요한 metadata를 한 번 복사해 kernel-owned representation으로 정규화한 뒤 그 사본을 검증한다. syscall fuzzing에서는 정상 입력뿐 아니라 boundary length, unmapped tail page, invalid flag 조합, interrupted copy를 넣어 partial side effect가 남는지 확인한다.

---

## CHAPTER 02 · syscall ABI는 register와 error convention까지 binary contract다

syscall number, argument register, return convention은 architecture와 OS ABI가 정한다. 고수준 언어는 wrapper 뒤에 이를 숨기지만 debugger/trace에서는 raw syscall ABI가 드러난다.

library wrapper가 retry, cancellation point, errno 변환을 추가할 수 있으므로 `library return`과 `kernel raw return`을 구분한다. interrupted syscall이 자동 재시작되는지, partial result가 반환되는지는 API별 계약을 확인한다.

64-bit process 안에서도 compat layer나 structure layout이 개입하면 user-visible type과 kernel ABI width가 달라질 수 있다. trace에서 syscall number만 보고 이름을 붙일 때도 architecture table을 맞춰야 한다. `EINTR` 뒤 library가 자동 재호출하면 application log에는 한 번의 API call처럼 보이지만 kernel에는 여러 entry가 남는다. 반대로 partial read/write는 이미 side effect가 있으므로 무조건 재호출하면 byte를 중복 처리할 수 있다. raw return value, restart 여부, consumed byte count, signal delivery 시점을 함께 기록하면 wrapper semantics와 kernel semantics를 분리할 수 있다.

---

## CHAPTER 03 · credentials는 process가 아니라 operation 시점의 security context로 본다

Unix 계열에는 real/effective/saved user ID, group ID, supplementary group 같은 identity state가 존재할 수 있다. access check가 어느 credential을 사용하는지는 operation과 API에 따라 다를 수 있다.

privilege drop를 수행할 때 일부 credential이나 supplementary group을 남기면 예상보다 큰 권한이 유지될 수 있다. child process spawn 시 credential·environment·descriptor 상속도 security boundary에 포함한다.

authorization evidence에는 process 이름이나 시작 UID보다 **결정 순간의 effective credential snapshot**이 필요하다. long-lived daemon이 startup 때 privilege를 갖고 resource를 연 뒤 권한을 낮추면 이미 열린 descriptor는 계속 강한 access path가 될 수 있고, setuid/setgid 계열 transition은 saved ID와 group state 때문에 되돌릴 수 있는 권한을 남길 수 있다. multi-threaded process에서는 credential 변경 API의 thread/process semantics도 확인한다. 감사 로그에는 effective IDs, supplementary groups, capability set, executable identity와 대상 object를 함께 남겨 “누가 요청했는가”를 실제 access check와 연결한다.

---

## CHAPTER 04 · capability는 root 권한을 세분화하지만 privilege 조합 위험은 남는다

Linux capability 모델은 전통적인 all-powerful root privilege를 여러 권한 단위로 나눈다. network configuration, raw socket, process tracing 같은 operation을 별도 capability로 통제할 수 있다.

세분화는 least privilege를 가능하게 하지만 capability 하나가 다른 resource와 결합되어 더 큰 권한을 얻을 수 있는지 검토해야 한다. executable file capability, inheritable/ambient set처럼 propagation rule도 존재한다. `root가 아니다`가 안전성의 충분조건은 아니다.

실제 권한은 permitted/effective/inheritable/ambient/bounding set의 교집합과 transition 규칙으로 판단한다. effective set에서 빠졌더라도 permitted에 남은 capability를 process가 다시 활성화할 수 있는지, exec 뒤 ambient가 유지되는지, bounding set이 복구 불가능한 상한을 제대로 낮췄는지 확인한다. `no_new_privs` 같은 정책은 exec를 통한 권한 상승 경로를 제한하는 데 의미가 있다. container manifest나 service unit을 검토할 때 capability 이름 목록만 보지 말고 어떤 executable·namespace·mounted resource와 결합되는지까지 threat model에 포함한다.

---

## CHAPTER 05 · filesystem permission은 path component 전체와 object metadata를 확인한다

파일 접근은 마지막 파일의 mode bit만 보는 문제가 아니다. path traversal 과정에서 directory execute/search permission이 필요하고 ACL, mount option, MAC policy가 추가될 수 있다.

symbolic link와 rename이 존재하므로 path 문자열을 validation한 뒤 다시 path로 open하는 패턴은 TOCTOU에 취약할 수 있다. 가능한 경우 이미 검증된 directory descriptor를 기준으로 relative open을 수행하고 no-follow 같은 kernel primitive를 활용한다.

path resolution은 mount namespace와 bind mount, symlink, `..` 처리 때문에 문자열 prefix 검사보다 복잡하다. security-sensitive open은 trusted dirfd를 anchor로 삼고 가능한 플랫폼에서는 resolution constraint를 제공하는 primitive를 사용해 symlink·mount escape를 제한한다. open 뒤에는 `fstat` 계열로 실제 object type과 owner를 확인해 validation한 object와 사용 object가 같은지 증명할 수 있다. race test는 공격 thread가 symlink/rename/mount 변화를 반복하는 동안 open을 수행해 path check가 우회되는지 검사한다. permission denial은 각 path component와 최종 inode policy를 별도로 확인한다.

---

## CHAPTER 06 · descriptor limit은 leak을 availability failure로 바꾼다

file/socket descriptor는 kernel resource와 process table entry를 소비한다. close가 누락되면 처음에는 정상 동작하다 descriptor limit에 도달한 순간 unrelated file open과 network accept까지 실패할 수 있다.

leak 진단에서는 단순 count뿐 아니라 descriptor type, 생성 stack, lifetime distribution을 본다. server는 process limit과 system-wide limit을 동시에 고려하고 admission control을 통해 overload 시 descriptor 폭증을 막는다.

`EMFILE`은 process 한도, `ENFILE` 계열은 system-wide resource pressure처럼 failure domain이 다를 수 있다. accept loop가 descriptor exhaustion 뒤 오류를 무제한 반복하면 CPU까지 포화시킬 수 있으므로 backoff와 reserve strategy를 설계한다. epoll/poll registry에 close된 fd의 stale registration이나 재사용된 번호가 남는지도 lifetime 관점에서 확인한다. 운영에서는 total open count 외에 socket state, file type, oldest age, open rate/close rate를 추적해 누적 leak과 정상 traffic burst를 구분하고, 한도에 도달하기 전 admission을 줄일 수 있는 headroom 경보를 둔다.

---

## CHAPTER 07 · memory limit은 heap 크기와 다르다

process/resource memory limit은 managed heap 하나가 아니라 anonymous memory, mmap, page cache accounting, shared/private page, native allocation과 연결될 수 있다. container/cgroup limit 아래에서는 host에 free RAM이 남아도 allocation이 실패하거나 OOM kill이 발생할 수 있다.

memory policy를 설계할 때 working set, reclaimable cache, peak allocation, fragmentation을 구분한다. limit을 크게 올리는 것은 leak이나 unbounded queue를 가리는 해결책이 될 수 있다.

cgroup 계층에서는 현재 사용량뿐 아니라 `high` 수준의 throttling/reclaim과 `max` 수준의 hard failure, OOM event를 구분해야 한다. 같은 RSS에서도 reclaimable file cache 비율과 anonymous working set이 다르면 pressure 반응이 달라진다. kill이 발생하면 host 전체 OOM인지 해당 cgroup 내부 OOM인지, 어떤 process가 victim이었는지 event counter와 kernel log를 연결한다. load test는 steady state만 보지 말고 startup burst, compaction/GC 동시 발생, queue spike처럼 peak memory가 겹치는 시나리오에서 limit headroom과 reclaim latency가 SLO를 지키는지 확인한다.

---

## CHAPTER 08 · cgroup은 resource accounting과 control boundary를 만든다

cgroup v2는 process 집합에 CPU, memory, I/O 같은 resource policy를 적용할 수 있다. CPU quota/weight, memory limit, I/O controller는 application 내부 thread scheduler와 별도의 상위 제약이다.

latency 문제에서 application CPU utilization이 낮아도 cgroup throttling 때문에 runnable task가 실행되지 못할 수 있다. container 환경에서는 host metric과 cgroup-local metric을 함께 본다.

CPU quota는 period 안에서 사용 가능한 runtime을 소진하면 runnable task가 있어도 다음 replenishment까지 throttling을 만들 수 있고, weight는 경쟁 cgroup 사이 상대적 배분에 영향을 준다. I/O controller도 application queue보다 아래에서 bandwidth/latency를 제한할 수 있다. 따라서 diagnosis에는 `cpu.stat`류의 throttled time, pressure stall, memory event, I/O statistics와 service latency를 같은 시간축으로 놓는다. autoscaling이 process CPU percentage만 보고 판단하면 quota-bound workload를 저부하로 오인할 수 있으므로 effective CPU capacity와 controller setting을 배포 metadata에 포함한다.

---

## CHAPTER 09 · namespace는 global resource view를 분리한다

mount, PID, network, IPC, UTS 같은 namespace는 process가 보는 system resource view를 격리한다. 격리는 resource 자체의 완전한 독립 복사라기보다 name lookup과 visibility boundary를 만드는 방식일 수 있다.

container가 VM과 같은 isolation을 제공한다고 단정하지 않는다. 같은 kernel을 공유하는 구조에서는 kernel vulnerability가 isolation boundary를 넘을 수 있다. namespace와 cgroup, capability, seccomp, MAC를 조합한다.

namespace는 process lifetime과 완전히 같지 않다. namespace file descriptor나 bind-mounted namespace handle이 남아 있으면 원래 process가 종료돼도 namespace가 유지될 수 있고, `setns` 같은 operation은 caller를 기존 namespace에 연결할 수 있다. mount propagation 설정이 잘못되면 한 namespace의 mount 변화가 예상 밖의 peer group에 전달될 수도 있다. incident 조사에서는 PID 숫자만 기록하지 말고 namespace inode/identifier, host PID와 namespace-local PID, network/mount namespace를 함께 캡처해 서로 다른 view의 로그를 같은 주체로 매칭한다.

---

## CHAPTER 10 · user namespace는 identity mapping을 분리한다

user namespace는 namespace 내부 UID/GID와 외부 host identity를 mapping할 수 있다. 내부에서 UID 0처럼 보이는 process가 host 전체의 root 권한을 갖는 것은 아니다.

그러나 user namespace가 허용하는 kernel attack surface와 capability semantics가 복잡하므로 배포 환경의 정책을 확인한다. identity를 로그로 남길 때 namespace-local ID만 기록하면 host 관점에서 주체를 식별하기 어려울 수 있다.

UID/GID map은 어느 host identity 범위를 namespace 안에 표현할 수 있는지를 정하며, filesystem object ownership은 이 mapping과 mount semantics의 영향을 받는다. `setgroups` 제한과 subordinate ID allocation 같은 주변 정책도 group-based access 결과를 바꿀 수 있다. rootless container 문제를 분석할 때 내부 `id` 출력만 보지 말고 host-side mapped ID, file owner, capability scope를 함께 본다. mapping 밖의 owner가 overflow ID로 보이는 상황이나 volume mount에서 write permission이 달라지는 상황을 test하면 “내부 root인데 왜 못 쓰는가”를 권한 매핑 문제로 분리할 수 있다.

---

## CHAPTER 11 · sandbox는 하나의 기술이 아니라 허용 surface의 교집합이다

실제 sandbox는 filesystem view, syscall filter, capability 제거, namespace, MAC policy, network restriction을 겹쳐 만든다. 한 층이 허용하더라도 다른 층이 거부할 수 있다.

sandbox 설계는 `무엇을 막을지`보다 **정상 workload가 실제로 필요한 최소 operation 집합**을 먼저 정의한다. allowlist가 명확할수록 unexpected new dependency가 정책 위반으로 드러난다.

allowlist는 software version이 바뀌면 함께 진화해야 한다. 새 runtime이나 library가 다른 syscall을 사용하기 시작했을 때 filter를 무조건 넓히기보다 trace로 call site와 필요성을 확인한다. seccomp 같은 syscall filter의 action도 kill, errno, trap, notify처럼 failure semantics가 다르므로 production behavior에 맞게 고른다. sandbox regression test에는 정상 workload 전체와 의도적으로 금지된 operation을 모두 넣어 “기능은 된다”와 “차단도 유지된다”를 별도 검증한다. escape risk는 개별 layer가 아니라 허용된 syscall·mounted object·capability·IPC endpoint의 조합으로 평가한다.

---

## CHAPTER 12 · MAC는 object owner보다 system policy를 우선할 수 있다

DAC에서는 resource owner가 permission을 조정할 수 있지만 mandatory access control은 system-wide policy가 subject/object 관계를 추가 통제한다. SELinux 같은 체계에서는 label/domain과 policy rule이 access 결정에 관여한다.

permission denied를 mode bit만 수정해 해결하려 하면 MAC denial 원인을 놓친다. audit log에서 source domain, target type, class, requested permission을 확인한다. policy를 넓게 허용하는 대신 필요한 transition과 operation만 추가한다.

label이 예상과 다르면 rule 자체가 맞아도 denial이 발생하므로 file context와 process domain transition을 함께 확인한다. temporary workaround로 enforcement를 끄거나 broad allow rule을 추가하면 원인이 사라지는 대신 isolation 전체가 약해진다. AVC/audit event를 request timeline과 연결하고 source context, target context, object class, permission을 최소 단위로 추출한 뒤 정상 실행에 꼭 필요한 edge만 policy에 반영한다. 배포 후에는 새 allow가 다른 domain에도 의도치 않은 access path를 열지 않는지 negative test로 검증한다.

---

## CHAPTER 13 · Android application sandbox는 UID와 platform policy를 함께 사용한다

Android는 application을 distinct UID로 실행해 기본 filesystem/process boundary를 만든다. runtime permission, app component export rule, SELinux policy, Binder permission check가 추가된다.

shared storage, exported component, ContentProvider URI permission처럼 의도적인 data-sharing path는 sandbox 예외 경로가 된다. 보안 검토에서는 application 내부 코드뿐 아니라 외부에서 들어올 수 있는 모든 component entry point를 inventory한다.

manifest의 exported 여부와 intent filter는 설치 시점의 attack surface를 만들고, runtime permission이 있어도 Binder/service 쪽에서 caller를 다시 검증해야 할 수 있다. URI permission은 전체 provider 권한보다 좁은 temporary delegation이 될 수 있으므로 grant 범위와 lifetime을 추적한다. AppOps나 platform policy가 runtime permission 위에 추가 결정을 적용하는 경우도 있어 단순 permission 문자열만으로 결과를 예측할 수 없다. 테스트는 같은 앱 내부 호출뿐 아니라 다른 UID의 test client가 exported component, provider URI, bound service를 호출하는 경로를 포함해야 한다.

---

## CHAPTER 14 · Binder는 caller identity가 remote call과 함께 전달되는 IPC다

Binder transaction에서는 callee가 caller UID/PID 같은 identity를 확인해 authorization을 수행할 수 있다. local method call처럼 보이는 framework API가 process boundary를 넘을 수 있으므로 identity와 failure semantics를 보존해야 한다.

service가 privileged identity로 downstream operation을 대신 수행할 때 confused-deputy 문제가 생길 수 있다. caller가 요청할 권한과 service 자체가 가진 권한을 분리해 검증한다.

privileged service가 내부 작업을 수행하려고 caller identity를 잠시 clear하는 API를 사용한다면 restore 경로가 exception과 early return에서도 보장돼야 한다. nested Binder call에서는 어느 지점의 identity가 authorization 대상인지 명확히 해야 한다. remote process death는 local exception/failed transaction으로 나타날 수 있으므로 cached proxy가 살아 있다는 사실을 peer lifetime과 동일시하지 않는다. transaction log에는 interface/method, caller UID, request ID, delegated operation, downstream authorization 결과를 연결해 service 권한이 원래 caller를 대신해 과도하게 사용되지 않았는지 감사할 수 있게 한다.

---

## CHAPTER 15 · request identity는 authentication 이후 authorization까지 유지되어야 한다

사용자가 누구인지 확인한 뒤 내부 service call에서 identity가 사라지면 downstream은 모든 요청을 trusted service 자체의 권한으로 처리할 수 있다. correlation ID와 authentication identity는 목적이 다르며, authorization에 필요한 principal·scope·tenant context를 명시적으로 전달한다.

identity forwarding은 원본 credential을 무조건 복사하는 것이 아니다. delegation token, capability-style scope, signed assertion처럼 필요한 권한만 제한해 전달하는 설계가 더 안전하다.

delegation에는 subject뿐 아니라 audience, scope, tenant, expiry, issuer를 함께 제한해야 한다. upstream token을 모든 downstream에 재사용하면 한 service가 필요 이상으로 넓은 credential을 보유하게 되고 compromise blast radius가 커진다. authorization decision log에는 caller principal과 acting service를 둘 다 남겨 “누구를 위해 누가 실행했는가”를 복원한다. retry·async queue를 거칠 때 identity context가 유실되거나 stale token이 재사용되지 않는지 확인하고, background job은 원래 user delegation이 만료됐을 때 어떤 service authority로 계속할지 명시한다.

---

## CHAPTER 16 · TOCTOU는 check와 use 사이 state 변화가 가능한 모든 resource에 적용된다

file existence/permission을 먼저 확인한 뒤 나중에 다시 path로 open하면 그 사이 다른 process가 symlink나 file을 바꿀 수 있다. check 결과가 use 대상과 동일 object라는 보장이 없기 때문이다.

해결은 `더 빨리 open`이 아니라 operation을 atomic kernel primitive에 가깝게 만든다. descriptor 기반 operation, atomic create, rename, compare-and-swap semantics를 사용해 check/use gap을 제거한다.

TOCTOU는 filesystem에만 한정되지 않는다. process ID 재사용, fd 번호 재사용, shared-state version 확인 뒤 update, authorization check 뒤 mutable resource lookup에서도 같은 구조가 나타난다. 해결 패턴은 이름을 다시 조회하는 대신 stable handle을 확보하거나, generation/version을 operation에 포함하거나, check와 mutation을 하나의 transaction으로 묶는 것이다. race harness는 check 직후 target을 반복 교체해 window를 넓히고, 성공한 operation이 실제로 validation한 object generation에 적용됐는지 기록한다. sleep으로 window를 줄이는 방식은 correctness proof가 아니다.

---

## CHAPTER 17 · resource ownership은 생성자와 해제 책임을 연결한다

fd, lock, mmap, temporary file, child process처럼 explicit cleanup이 필요한 resource는 ownership을 정해야 한다. error path와 cancellation path에서 해제가 누락되면 steady-state leak으로 축적된다.

RAII/context manager/structured concurrency는 cleanup을 lexical lifetime에 연결한다. ownership transfer가 있으면 원래 owner가 더 이상 해제하지 않는다는 계약과 recipient의 종료 책임을 명시한다.

cleanup은 가능한 한 idempotent하게 만들어 partial construction과 중복 cancellation에서도 안전하게 한다. 여러 resource가 dependency를 가지면 해제 순서도 ownership graph의 일부다. 예를 들어 DMA buffer는 device operation을 drain한 뒤 unmap/free해야 하고, child process pipe는 reader/writer 종료 순서가 EOF semantics에 영향을 줄 수 있다. leak test는 정상 성공 path뿐 아니라 각 acquisition 단계 직후 fault를 주입해 이미 획득한 resource가 역순으로 정리되는지 검사한다. ownership transfer event와 close/free event를 ID로 연결하면 “누가 마지막 책임자였는가”를 사후에 추적할 수 있다.

---

## CHAPTER 18 · signal handler는 일반 함수와 같은 안전한 실행 환경이 아니다

POSIX signal은 asynchronous하게 thread execution을 끊고 handler를 실행할 수 있다. handler에서 lock을 잡거나 malloc처럼 async-signal-safe가 아닌 함수를 호출하면 interrupted code와 재진입 충돌이 생길 수 있다.

복잡한 작업은 signal handler에서 직접 수행하지 않고 flag/self-pipe 같은 안전한 notification으로 main control loop에 넘긴다. crash signal 처리도 가능한 operation이 매우 제한적이므로 사후 진단용 raw state 보존을 우선한다.

handler가 실행되는 stack 자체가 손상됐을 수 있는 crash 상황에서는 alternate signal stack 같은 별도 실행 공간이 진단 가능성을 높일 수 있지만, 그 안에서도 일반 allocator·logger의 안전성이 생기는 것은 아니다. signal mask와 handler 설치 시점도 race를 만들 수 있으므로 초기화 전에 signal이 들어오는 경로를 고려한다. handler에서는 최소한의 async-signal-safe write나 atomic flag만 수행하고 symbolization·upload는 별도 process 또는 다음 startup 단계로 넘긴다. 테스트에서는 lock 보유 중 signal, nested signal, stack overflow 근처 signal처럼 재진입이 위험한 경로를 주입한다.

---

## CHAPTER 19 · graceful shutdown은 새로운 work를 막고 기존 ownership을 drain한다

server 종료는 process kill 한 번이 아니다. 신규 request admission 중단, in-flight deadline 설정, queue drain, transaction/offset checkpoint, resource close 순서를 설계해야 한다.

shutdown deadline을 넘긴 작업을 무한히 기다리면 deploy가 멈추고, 즉시 강제 종료하면 data loss가 생길 수 있다. workload별 `grace period 이후 중단 가능 상태`를 정의하고 restart 후 중복 처리 가능성까지 포함한다.

readiness endpoint를 먼저 내려 새 traffic 유입을 막고, 이미 load balancer나 queue에 들어간 work가 사라질 시간을 확보한 뒤 drain을 시작하는 식으로 외부 admission과 내부 ownership을 연결한다. 각 in-flight request에는 shutdown generation과 deadline을 부여해 새 background work를 다시 생성하지 못하게 한다. checkpoint가 durable해진 뒤 resource를 close해야 restart가 동일 work를 중복 처리하더라도 idempotency rule로 복구할 수 있다. 운영 검증은 graceful signal, deadline 초과 강제 kill, process crash 세 경우를 나눠 데이터 손실·중복·재기동 시간을 측정한다.

---

## CHAPTER 20 · 권한 문제는 denial point와 effective identity를 증거로 찾는다

보안/격리 오류를 조사할 때 다음을 함께 수집한다.

```text
effective credentials / capability
namespace and cgroup membership
resource identifier and owner
DAC/ACL result
MAC/SELinux audit event
syscall + errno
Binder/IPC caller identity
resource limit counters
```

`permission denied`를 catch해 더 강한 권한으로 재시도하는 방식은 원인을 숨기고 privilege를 확대한다. 어떤 policy layer가 어떤 rule로 거부했는지 확인한 뒤 최소 권한 변경으로 수정한다.

조사 순서는 identity→object identity→namespace/view→DAC/ACL→capability→MAC→resource controller→IPC delegation처럼 layer별 evidence를 쌓는 방식이 재현성이 높다. 같은 `EACCES`나 `EPERM`이라도 kernel subsystem과 policy에 따라 denial point가 다르므로 errno 하나로 결론내리지 않는다. 정상 요청과 실패 요청의 effective credential, namespace ID, target inode/socket/service ID를 비교해 차이를 좁히고 audit event를 correlation ID로 연결한다. 수정 후에는 원래 실패 case가 성공하는지뿐 아니라 권한이 없어야 하는 negative principal이 계속 거부되는지도 반드시 확인한다.