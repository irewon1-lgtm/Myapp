# PART 28 · Container Internals — namespaces, cgroups, seccomp, OCI lifecycle

container는 작은 VM이 아니라 **같은 kernel을 공유하면서 process가 보는 namespace, resource budget, privilege, filesystem layer를 조합한 실행 환경**이다. isolation은 한 기능이 아니라 여러 boundary의 교집합이며, runtime lifecycle과 image provenance까지 포함해야 운영 가능한 security contract가 된다.

---

## CHAPTER 01 · shared kernel은 container isolation의 가장 중요한 전제다

container process는 host와 같은 kernel code를 실행한다. namespace가 PID와 mount view를 분리하고 cgroup이 resource를 제한해도 kernel vulnerability는 여러 container가 공유하는 공통 failure domain이 될 수 있다. VM과 동일한 hardware virtualization boundary로 이해하면 threat model이 틀어진다.

root inside container도 namespace와 capability mapping에 따라 host root와 의미가 다를 수 있지만, privileged container나 wide device access는 boundary를 크게 약화시킬 수 있다.

security review에서는 namespace, capability, seccomp, LSM, device exposure를 함께 inventory한다. kernel patch level과 runtime config를 workload artifact와 연결해 공통 kernel risk를 추적한다.

---

## CHAPTER 02 · OCI bundle은 runtime이 소비하는 executable deployment contract다

OCI runtime bundle은 root filesystem과 runtime configuration을 함께 제공해 어떤 process를 어떤 namespace·mount·capability·cgroup 조건에서 실행할지 정의한다. image layer가 같아도 runtime spec이 다르면 실제 execution boundary가 달라진다.

config가 environment, bind mount, capability를 잘못 포함하면 image 자체가 안전해도 deployment에서 privilege가 넓어질 수 있다. reproducibility는 image digest만으로 충분하지 않다.

bundle config와 image digest를 release record에 같이 보존한다. production incident에서 실제 runtime config를 desired manifest와 비교해 drift를 확인한다.

---

## CHAPTER 03 · runtime lifecycle은 create와 start를 분리한다

OCI-style lifecycle은 bundle을 검증하고 container state를 준비하는 create 단계와 process execution을 시작하는 start 단계를 분리한다. namespace, mount, cgroup 설정이 완료되기 전에 application code가 실행되면 isolation invariant가 깨질 수 있다.

partial create failure에서는 mount, cgroup, shim process 같은 resource가 남을 수 있다. delete가 idempotent하지 않으면 orphan resource가 축적된다.

state transition을 `creating→created→running→stopped→deleted`로 기록한다. 각 실패 지점에서 cleanup이 완료되는지 fault injection으로 검증한다.

---

## CHAPTER 04 · PID namespace는 process identifier와 visibility를 분리한다

PID namespace 안의 process는 host와 다른 PID를 볼 수 있고 namespace의 PID 1은 특별한 lifecycle 역할을 가진다. host PID와 container PID를 같은 identity로 로그하면 incident correlation이 어려워진다.

namespace 내부에서 process가 보이지 않는다고 host resource가 존재하지 않는 것은 아니다. host monitor는 전체 process를 볼 수 있고 signal target resolution도 namespace context를 따라야 한다.

로그에 container identity, namespace-local PID, host task identity를 함께 남긴다. process supervision과 debugging tool이 어느 namespace view를 사용했는지 명시한다.

---

## CHAPTER 05 · mount namespace는 pathname의 의미를 process별로 바꾼다

mount namespace는 process가 보는 filesystem tree와 mount table을 분리한다. 같은 `/data` 문자열이 host와 container에서 다른 backing filesystem을 가리킬 수 있다. path string만으로 storage identity를 판단하면 안 된다.

bind mount와 overlay, read-only remount가 섞이면 file permission과 durability 특성도 달라진다. host에서 file이 보인다는 사실이 container path의 object identity를 보장하지 않는다.

mount ID, device/inode, namespace를 함께 기록한다. incident 재현에는 mount table과 propagation state를 포함한다.

---

## CHAPTER 06 · mount propagation은 namespace 사이 mount event 전달 규칙이다

shared, slave, private 같은 propagation 설정은 한 namespace의 mount/unmount 변화가 다른 namespace에 전파되는 방식을 결정한다. bind mount만 확인하고 propagation을 무시하면 host mount가 container에 예상치 않게 나타나거나 필요한 mount가 전달되지 않을 수 있다.

runtime과 systemd가 서로 다른 propagation policy를 적용하면 환경별 차이가 생긴다. security boundary에서는 unintended mount visibility가 중요하다.

`mountinfo`와 propagation flag를 보존한다. hot-plug나 nested mount를 포함한 lifecycle test로 intended visibility를 검증한다.

---

## CHAPTER 07 · user namespace는 UID/GID를 다른 identity domain으로 매핑한다

user namespace는 내부 UID 0을 host의 unprivileged UID에 매핑할 수 있어 rootless execution과 privilege reduction에 사용된다. 내부에서 root처럼 보인다는 사실만으로 host 전체 권한을 가진 것은 아니다.

하지만 capability semantics가 namespace context에 따라 달라지고 filesystem ownership mapping도 복잡해진다. unmapped ID와 shared volume은 permission anomaly를 만들 수 있다.

uid/gid map과 effective capability를 함께 수집한다. host와 container 양쪽에서 file ownership과 privileged operation을 검증한다.

---

## CHAPTER 08 · network namespace는 interface, route, port space를 격리한다

network namespace는 interface, routing table, firewall state, socket port namespace를 분리한다. veth, bridge, NAT를 통해 host/network와 연결되며 실제 packet path는 namespace boundary를 여러 번 넘을 수 있다.

container 안에서 `localhost`는 host localhost가 아니다. port publish와 service mesh가 추가되면 connection path가 더 복잡해진다.

packet drop을 조사할 때 source/destination namespace, veth pair, route, conntrack을 연결한다. namespace 내부 socket metric만으로 physical network 문제를 결론내리지 않는다.

---

## CHAPTER 09 · IPC와 UTS namespace는 다른 global view를 분리한다

IPC namespace는 SysV IPC·POSIX message queue 같은 object visibility를 분리하고 UTS namespace는 hostname/domainname view를 바꾼다. 이들은 security isolation과 application identity에 다른 영향을 준다.

shared IPC가 필요한 workload에서 namespace를 분리하면 component가 서로 보지 못하고, 반대로 불필요하게 host IPC를 공유하면 data exposure가 생길 수 있다.

runtime spec에서 각 namespace의 존재 여부를 명시한다. application이 hostname을 stable identity로 사용하지 않는지도 검토한다.

---

## CHAPTER 10 · cgroup v2는 resource accounting과 hierarchical policy를 결합한다

cgroup v2는 process group에 CPU, memory, I/O, pids 같은 controller를 계층적으로 적용한다. container 안에서 CPU가 idle해 보여도 상위 cgroup quota나 memory limit 때문에 progress가 제한될 수 있다.

parent limit이 child 설정보다 강하면 child가 넉넉한 값으로 보이더라도 실제 capacity는 작다. metrics도 host와 cgroup-local 값을 구분해야 한다.

current cgroup path, effective controller values, pressure signal을 incident에 포함한다. hierarchy 변경 후 inherited limit가 예상과 같은지 test한다.

---

## CHAPTER 11 · memory controls는 OOM 이전부터 latency를 바꾼다

memory.max 같은 hard limit뿐 아니라 reclaim pressure와 high threshold가 application allocation, page fault, writeback latency에 영향을 줄 수 있다. host free RAM이 남아도 container cgroup 내부에서는 reclaim과 OOM이 발생할 수 있다.

page cache와 anonymous memory가 같은 budget을 경쟁하면 workload 변화에 따라 effective working set이 급격히 줄 수 있다.

memory.current, events, PSI, reclaim, OOM kill을 같은 timeline에 둔다. limit을 늘려 증상을 숨기기 전에 leak·queue·cache policy를 분리한다.

---

## CHAPTER 12 · CPU quota는 wall time 안에 사용할 CPU budget을 제한한다

CPU quota는 일정 period 동안 cgroup이 소비할 runtime을 제한해 multi-tenant fairness를 만든다. budget을 소진하면 host CPU가 남아 있어도 task가 다음 replenishment까지 throttled될 수 있다.

burst workload는 평균 CPU utilization이 낮아도 period boundary에서 p99 latency가 튈 수 있다. thread 수를 늘려도 quota 자체는 늘지 않는다.

throttled time, period, quota consumption, request latency를 연결한다. application CPU profile과 cgroup accounting을 함께 본다.

---

## CHAPTER 13 · pids controller는 process/thread 폭증을 availability failure 전에 막는다

pids controller는 cgroup 안에서 생성할 task 수에 상한을 둬 fork bomb이나 runaway thread creation을 제한한다. limit 도달 시 새 process/thread 생성이 실패하지만 기존 task는 계속 실행될 수 있다.

thread pool leak가 처음에는 정상처럼 보이다 limit 근처에서 unrelated spawn까지 실패할 수 있다. limit을 높이는 것만으로 leak는 해결되지 않는다.

pids.current, creation rate, owner stack을 모니터링한다. stress test에서 limit 도달 시 application이 bounded failure로 처리하는지 확인한다.

---

## CHAPTER 14 · capability는 root privilege를 operation 단위로 나눈다

Linux capability는 raw socket, mount 관련 operation, process tracing 같은 privilege를 여러 bit로 나눠 least privilege를 가능하게 한다. container runtime은 bounding/permitted/effective/ambient set을 조정해 process 권한을 제한한다.

불필요한 capability 하나가 namespace나 writable host mount와 결합되면 큰 privilege escalation surface가 될 수 있다. `non-root UID`만으로 안전성을 판단하면 안 된다.

실제 effective capability와 workload 필요 operation을 대조한다. default allow보다 명시적 최소 set을 유지하고 regression test로 제거 가능성을 확인한다.

---

## CHAPTER 15 · no_new_privs는 exec를 통한 privilege 획득 경로를 제한한다

no_new_privs가 설정되면 exec를 통해 setuid나 file capability 등으로 새로운 privilege를 얻는 경로를 제한할 수 있다. seccomp filter 적용 같은 sandbox 구성의 안전한 전제조건으로 사용되기도 한다.

flag는 이미 가진 privilege를 자동 제거하지 않는다. 넓은 capability나 privileged fd가 남아 있다면 여전히 강한 권한을 행사할 수 있다.

process status와 exec behavior를 검증한다. sandbox review에서 `no_new_privs=true`를 전체 least privilege의 대체물로 취급하지 않는다.

---

## CHAPTER 16 · seccomp는 syscall surface를 allow/deny policy로 줄인다

seccomp filter는 process가 호출할 수 있는 syscall과 argument pattern을 제한해 kernel attack surface를 줄인다. 정상 workload가 필요로 하는 최소 syscall 집합을 정의해야 하며 unknown dependency가 있으면 runtime failure로 드러난다.

너무 넓은 allowlist는 효과가 없고 너무 좁으면 library/OS update 뒤 정상 기능이 깨질 수 있다. architecture별 syscall 번호 차이도 고려한다.

filter version과 denied syscall telemetry를 보존한다. representative workload와 error path를 실행해 policy가 기능과 security 요구를 동시에 만족하는지 확인한다.

---

## CHAPTER 17 · device access는 namespace보다 강한 hardware boundary를 건드린다

container에 `/dev` node를 노출하거나 device cgroup permission을 주면 process가 driver ioctl, DMA-capable hardware와 직접 상호작용할 수 있다. driver vulnerability는 shared kernel boundary를 공격하는 경로가 될 수 있다.

GPU, accelerator passthrough는 performance 이점이 있지만 reset, firmware, multi-tenant isolation을 더 복잡하게 만든다.

device allowlist와 ioctl surface를 inventory한다. 필요하지 않은 raw device access를 제거하고 hot-unplug/reset failure를 별도 test한다.

---

## CHAPTER 18 · OverlayFS는 여러 filesystem layer를 하나의 view로 합친다

container image는 read-only lower layer와 writable upper layer를 overlay해 하나의 root filesystem처럼 보일 수 있다. pathname 하나가 어느 layer에서 온 object인지에 따라 write와 delete semantics가 달라진다.

first write에서 copy-up이 발생하면 예상치 못한 latency와 disk usage가 생길 수 있다. inode identity가 layer transition으로 바뀌어 open fd와 path observation이 달라질 수도 있다.

upper/lower path와 copy-up event를 추적한다. write-heavy workload는 overlay cost를 실제 deployment filesystem에서 benchmark한다.

---

## CHAPTER 19 · copy-up은 첫 mutation에서 lower object를 upper layer로 복제한다

lower read-only file을 수정하려면 OverlayFS가 upper layer에 새 copy를 만들고 이후 mutation을 그 copy에 적용한다. large file의 작은 변경도 copy-up 비용이 클 수 있다.

concurrent reader가 old lower object를 이미 열고 있으면 path가 새 upper file을 가리킨 뒤에도 old handle을 계속 사용할 수 있다. path identity와 open object lifetime을 구분해야 한다.

copy-up bytes, duration, upper storage pressure를 모니터링한다. configuration file hot rewrite 같은 pattern을 실제 overlay에서 테스트한다.

---

## CHAPTER 20 · whiteout은 lower entry를 삭제한 것처럼 숨기는 metadata다

read-only lower layer의 file을 실제로 삭제할 수 없기 때문에 upper layer에 whiteout을 기록해 merged view에서 보이지 않게 할 수 있다. layer를 직접 검사하면 file이 존재하지만 container view에서는 사라져 보여 혼란이 생길 수 있다.

backup/export tool이 whiteout semantics를 보존하지 않으면 reconstructed image에서 삭제된 file이 다시 나타날 수 있다.

layer tar와 merged view를 모두 검증한다. image build/rebase 과정에서 whiteout handling을 regression test로 유지한다.

---

## CHAPTER 21 · bind mount는 host object를 container namespace에 직접 노출한다

bind mount는 host directory/file을 container path에 연결해 data sharing을 단순하게 하지만 isolation 경계를 의도적으로 연다. read-write host mount는 container compromise의 blast radius를 크게 넓힐 수 있다.

path traversal, symlink, mount propagation과 결합되면 예상보다 넓은 host tree가 노출될 수 있다. mount source의 ownership과 label도 중요하다.

source/destination, read-only flag, propagation, SELinux context를 release config에서 검증한다. secret과 socket mount는 별도 security review를 둔다.

---

## CHAPTER 22 · read-only rootfs는 mutable state 위치를 명시하게 만든다

root filesystem을 read-only로 두면 application이 code/image layer를 runtime에 수정하는 것을 막고 mutable state를 volume/tmpfs 같은 명시적 위치로 분리하게 한다. compromise 후 persistence surface도 줄일 수 있다.

application이 예상하지 않은 cache, PID file, temp file을 rootfs에 쓰면 runtime에서만 실패할 수 있다. startup뿐 아니라 rare error path가 write를 시도하는지도 봐야 한다.

write audit와 representative test로 필요한 writable path를 inventory한다. 예외 writable mount를 최소 범위로 제한한다.

---

## CHAPTER 23 · PID 1은 signal과 child reaping에서 특별한 책임을 가진다

PID namespace의 init process는 orphan child를 reap하고 signal handling에서 일반 process와 다른 고려가 필요하다. application binary를 그대로 PID 1로 실행하면 zombie accumulation이나 graceful shutdown 실패가 생길 수 있다.

container stop이 TERM을 보냈는데 application이 signal을 처리하지 않으면 timeout 뒤 KILL되어 transaction drain을 잃을 수 있다.

child count, signal delivery, shutdown duration을 테스트한다. 필요하면 작은 init/supervisor를 사용해 lifecycle 책임을 분리한다.

---

## CHAPTER 24 · runtime hook은 lifecycle 중 host-side privileged extension point다

OCI hook은 create/start 등 특정 시점에 external program을 실행해 network, device, policy 설정을 확장할 수 있다. privileged host context에서 실행될 수 있어 supply-chain과 input validation surface가 크다.

hook failure가 container create를 중간 상태에 남기거나 cleanup을 방해할 수 있다. 여러 hook의 ordering도 중요하다.

hook binary digest, arguments, exit status를 감사 로그에 남긴다. failure injection으로 partial lifecycle cleanup을 확인한다.

---

## CHAPTER 25 · rootless는 host privilege를 줄이지만 모든 isolation 문제를 제거하지 않는다

rootless runtime은 user namespace와 unprivileged mechanism을 사용해 daemon/container가 host root를 요구하지 않게 한다. compromise 시 direct host-root 권한을 줄이는 중요한 defense다.

network, cgroup, filesystem 기능이 privileged runtime과 다르게 동작할 수 있고 kernel attack surface는 여전히 공유한다. 기능 차이를 숨기기 위해 broad helper privilege를 추가하면 목적이 약해진다.

rootless mode의 actual UID mapping과 helper binary 권한을 검토한다. workload requirement와 unsupported feature를 명확히 문서화한다.

---

## CHAPTER 26 · runtime provenance는 image뿐 아니라 실행 binary와 config의 출처를 증명한다

container supply chain에서 image digest가 같아도 runtime, shim, hook, seccomp profile이 다르면 실제 execution environment가 달라진다. provenance는 어떤 source/build가 어떤 artifact와 config를 만들었는지 연결해야 한다.

mutable tag와 unpinned runtime dependency는 rollback과 incident 재현을 어렵게 한다.

image digest, runtime version, config hash, policy bundle을 deployment record에 저장한다. release gate에서 intended provenance와 live node state를 비교한다.

---

## CHAPTER 27 · checkpoint/restore는 kernel object state까지 일관되게 포착해야 한다

container checkpoint는 process memory뿐 아니라 open fd, socket, namespace, timer 같은 kernel-visible state를 복원 가능한 형태로 저장해야 한다. 외부 peer나 device state는 snapshot과 독립적으로 변할 수 있어 restore가 어려울 수 있다.

checkpoint 중 network request가 진행 중이면 duplicate 또는 broken connection이 생길 수 있다. application quiesce와 protocol-level recovery가 필요할 수 있다.

checkpoint generation과 external dependency를 기록한다. 실제 restore를 반복해 state continuity와 idempotency를 검증한다.

---

## CHAPTER 28 · container observability는 namespace와 cgroup identity를 보존해야 한다

host에서 보는 PID, interface, path가 container 내부 값과 다르므로 telemetry가 어느 identity domain에서 수집됐는지 알아야 한다. container restart 뒤 같은 name이 다른 cgroup/process generation을 가리킬 수 있다.

host metric과 container metric을 단순 합치면 double counting이나 wrong correlation이 생길 수 있다.

container ID, cgroup path, namespace inode, pod/workload generation을 event에 포함한다. restart와 reschedule을 포함한 trace correlation을 테스트한다.

---

## CHAPTER 29 · escape graph는 단일 취약점보다 privilege 조합을 본다

container escape는 kernel bug 하나뿐 아니라 writable host mount, powerful capability, device access, exposed daemon socket 같은 여러 privilege edge를 조합할 수 있다. 각 설정이 개별적으로 필요해 보여도 결합하면 host control path가 생길 수 있다.

Docker/container runtime socket을 mount한 workload처럼 API capability 자체가 사실상 host privilege인 경우도 있다.

attack graph를 resource와 authority edge로 작성한다. least privilege review는 설정 항목을 하나씩 보지 말고 가능한 chain을 제거하는 방향으로 한다.

---

## CHAPTER 30 · container contract는 isolation, resource, lifecycle, provenance를 함께 고정한다

안전한 container 정의에는 어떤 namespace를 분리하고, 어떤 capability와 syscall을 허용하며, CPU/memory/pids limit가 무엇이고, 어떤 mount/device를 공유하는지 명시돼야 한다. create→run→stop→delete 실패 경로와 image/runtime provenance도 같은 contract의 일부다.

'container니까 격리됨' 같은 추상 표현은 검증할 수 없다. 실제 kernel 공유와 host integration surface를 기준으로 threat model을 만든다.

CLEAN 검증은 namespace visibility, resource saturation, seccomp denial, mount exposure, shutdown cleanup을 서로 다른 failure mode로 확인한다. 모든 layer가 기대한 boundary를 유지할 때만 isolation을 주장할 수 있다.
