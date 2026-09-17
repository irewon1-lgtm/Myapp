# PART 28 · Container Internals — namespaces, cgroups, rootfs, seccomp, OCI lifecycle

Container는 작은 가상머신이 아니다. Linux container는 host kernel을 공유하면서 process가 **다른 PID·mount·network·user·UTS·IPC view를 보도록 namespace를 구성하고, cgroup으로 resource accounting/control을 적용하며, capability·seccomp·LSM으로 privilege를 줄이고, 별도 root filesystem view를 제공**하는 process isolation 조합이다. Isolation은 하나의 feature가 아니라 여러 kernel boundary가 동시에 맞아야 성립한다.

---

## CHAPTER 01 · Container boundary는 kernel boundary를 공유한다

VM guest는 별도 kernel을 실행할 수 있지만 일반 Linux container process는 host kernel에 system call을 직접 보낸다. 따라서 kernel vulnerability는 container isolation boundary를 넘어설 수 있고 seccomp/capability/LSM으로 reachable attack surface를 줄이는 것이 중요하다.

Container security를 `namespace가 있으니 격리됨`으로 설명하지 않는다. Shared-kernel trust model을 먼저 명시한다.

---

## CHAPTER 02 · OCI bundle은 rootfs와 config.json으로 runtime contract를 표현한다

OCI runtime specification은 container root filesystem과 process/environment/namespace/resource/hook configuration을 bundle로 기술한다. High-level orchestrator가 어떤 UX를 제공하든 low-level runtime은 이 contract를 실제 process state로 변환한다.

Image metadata와 runtime configuration은 같은 것이 아니다. Image가 immutable artifact여도 runtime은 mount, credential, cgroup, environment를 deployment마다 다르게 설정할 수 있다.

---

## CHAPTER 03 · Runtime lifecycle은 create와 start를 분리한다

Container create 단계에서는 namespace, rootfs, cgroup, hooks 같은 execution environment를 준비할 수 있고 start에서 init process execution을 진행한다. 준비와 실행을 분리하면 network/device setup hook이 process 실행 전에 필요한 작업을 수행할 수 있다.

Crash/debugging에서는 `container create 실패`, `start 실패`, `process exit`를 같은 failure로 묶지 않는다. Lifecycle phase별 evidence가 다르다.

---

## CHAPTER 04 · PID namespace는 process ID view와 parenthood를 재정의한다

Container 내부 PID 1은 host에서 다른 PID를 가진 동일 task일 수 있다. PID namespace는 process visibility와 ID mapping을 격리하며 nested namespace도 가능하다. Host monitor가 보는 PID와 container log의 PID를 correlation할 때 namespace identity가 필요하다.

Container 내부 PID 1은 signal handling과 orphan reaping에서 특별한 역할을 가진다. 일반 application process를 PID 1로 두면 zombie reaping과 shutdown semantics를 명시해야 한다.

---

## CHAPTER 05 · Mount namespace는 filesystem tree view를 분리한다

각 process group은 다른 mount table을 볼 수 있다. 같은 underlying filesystem object라도 container에서는 다른 path에 mount되거나 숨겨질 수 있다. `컨테이너 안에 파일이 없다`는 말이 host filesystem에 파일이 없다는 뜻은 아니다.

Mount namespace와 chroot는 다르다. Root path를 바꾸는 것만으로 mount topology, device, proc/sys view까지 안전하게 격리되는 것은 아니다.

---

## CHAPTER 06 · Mount propagation은 namespace 사이 mount event 전파를 결정한다

shared/private/slave propagation property는 한 namespace의 mount/unmount event가 다른 namespace에 전달되는 방식을 제어한다. Container volume mount가 host 또는 sibling container에 예상치 못하게 전파되면 isolation 문제가 생길 수 있다.

Mount propagation은 path permission과 다른 축이다. Rootfs tree가 같아 보여도 event propagation topology가 다를 수 있다.

---

## CHAPTER 07 · User namespace는 UID/GID를 namespace별로 mapping한다

Container 내부 UID 0을 host UID 0과 동일한 credential로 만들지 않고 subordinate UID range에 mapping할 수 있다. Rootless container는 이 mechanism으로 host root privilege 없이 내부 root-like environment를 구성한다.

그러나 모든 kernel operation이 user namespace 안의 root를 host root처럼 허용하는 것은 아니다. Capability도 user namespace 범위와 연결된다. `container root`라는 이름 하나로 privilege를 판단하지 않는다.

---

## CHAPTER 08 · Network namespace는 interface·route·socket namespace를 분리한다

Network namespace마다 interface, routing table, firewall context, port namespace를 분리할 수 있다. veth pair는 두 namespace를 연결하는 virtual link로 사용할 수 있고 bridge/NAT를 통해 외부 network와 연결할 수 있다.

Container에서 localhost는 해당 network namespace의 loopback이다. Host localhost와 동일하지 않다. Network debugging은 namespace를 명시하고 route/interface/socket을 조사해야 한다.

---

## CHAPTER 09 · IPC와 UTS namespace도 process-visible global state를 분리한다

IPC namespace는 System V IPC/POSIX message queue 같은 일부 IPC resource view를 분리하고 UTS namespace는 hostname/domainname view를 분리한다. Isolation 대상은 filesystem과 network만이 아니다.

어떤 global resource가 namespace되지 않았는지 확인하는 것이 더 중요하다. Kernel version, some devices, certain sysctls는 shared or differently namespaced semantics를 가질 수 있다.

---

## CHAPTER 10 · Cgroup v2는 hierarchy 하나에서 resource domain을 구성한다

cgroup v2는 process를 hierarchy에 배치하고 CPU, memory, I/O, PIDs 등의 controller를 통해 usage를 account/control한다. Namespace는 `무엇을 보느냐`, cgroup은 `얼마나 사용할 수 있느냐`에 더 가깝다.

Container OOM, CPU throttling, I/O latency를 분석할 때 host free resource만 보지 말고 해당 cgroup의 effective limit과 pressure counter를 확인한다.

---

## CHAPTER 11 · memory.max와 memory.high는 hard limit과 pressure policy가 다르다

memory.max는 cgroup memory usage의 hard boundary와 연결되고 memory.high는 reclaim/throttling pressure를 주는 soft control로 사용할 수 있다. Application은 hard OOM에 도달하기 전에 memory.high pressure로 latency가 증가할 수 있다.

`OOM이 없으니 memory 문제 아님`은 틀릴 수 있다. reclaim stall, PSI, memory.events를 함께 본다.

---

## CHAPTER 12 · CPU quota는 runnable task를 policy로 멈출 수 있다

CPU quota를 소진한 cgroup task는 host에 idle CPU가 있어도 period replenishment까지 throttled될 수 있다. PART 23의 scheduler analysis에서 runnable wait와 quota throttle을 분리해야 하는 이유다.

Container p99 latency가 주기적인 spike를 보이면 cpu.stat throttled time과 period를 correlation한다.

---

## CHAPTER 13 · PIDs controller는 fork bomb의 process count를 제한한다

Memory/CPU limit만으로는 process/thread creation storm을 충분히 containment하지 못할 수 있다. pids controller는 hierarchy에서 생성 가능한 task 수를 제한한다. Limit 도달 시 fork/clone이 실패할 수 있다.

Application은 thread creation failure를 OOM으로 잘못 해석하지 말고 errno/resource event를 구분한다.

---

## CHAPTER 14 · Capability는 root privilege를 세분화한다

Traditional root privilege를 network administration, raw socket, ptrace 등 여러 capability bit로 분리해 필요한 권한만 부여할 수 있다. Container runtime은 bounding/permitted/effective/ambient set을 구성한다.

불필요한 capability 하나가 container escape primitive가 될 수 있다. `root user 아님`보다 실제 capability set과 device/mount access를 검토한다.

---

## CHAPTER 15 · no_new_privs는 exec를 통한 privilege 상승을 제한한다

Process가 `no_new_privs`를 설정하면 이후 execve에서 setuid/file capability 같은 mechanism을 통해 새로운 privilege를 얻지 못하도록 제한할 수 있다. Seccomp filter 사용과도 결합되는 중요한 safety property다.

Privilege lifecycle은 현재 UID만 보지 말고 exec 이후 변화 가능성을 포함해야 한다.

---

## CHAPTER 16 · Seccomp filter는 syscall 허용목록이 아니라 kernel entry policy다

Seccomp BPF는 syscall number와 argument metadata를 기반으로 allow, errno, trap, kill, user-notify 같은 action을 결정할 수 있다. Container workload에 불필요한 syscall을 차단하면 kernel attack surface를 줄일 수 있다.

하지만 application/library/kernel version이 새 syscall을 사용하면 overly strict profile이 정상 동작을 깨뜨릴 수 있다. Profile은 observed syscall list를 그대로 복사하지 말고 required semantic과 fallback을 검증한다.

---

## CHAPTER 17 · Device access는 namespace/cgroup만으로 끝나지 않는다

Container가 `/dev` node를 볼 수 있는지, corresponding device cgroup/LSM policy가 access를 허용하는지, capability가 필요한지 모두 확인해야 한다. GPU, accelerator, block device를 container에 노출하면 DMA/IOMMU와 driver attack surface가 isolation model에 추가된다.

Privileged container로 모든 device를 열어 문제를 해결하면 격리 guarantee를 크게 낮춘다.

---

## CHAPTER 18 · Root filesystem layer는 immutable image와 writable layer를 합성할 수 있다

OverlayFS 계열은 lower read-only layer와 upper writable layer를 하나의 merged tree처럼 보여 줄 수 있다. Container image layer가 재사용되고 container별 변경은 upper layer에 기록된다.

Application이 `/etc/config`를 수정했을 때 original image layer가 변한 것이 아니다. Debugging/backup에서 merged view와 underlying layer를 구분한다.

---

## CHAPTER 19 · Copy-up은 첫 write에 예상치 못한 latency와 storage cost를 만든다

Lower layer file을 수정하려면 OverlayFS가 upper layer에 copy-up해야 할 수 있다. Large file의 첫 small write가 전체 metadata/data copy 비용을 유발할 수 있다. Rename, hardlink, extended attribute semantics도 ordinary filesystem과 차이가 생길 수 있다.

Write-heavy database를 overlay writable layer에 둘 때 direct volume과 성능·durability behavior를 benchmark한다.

---

## CHAPTER 20 · Whiteout은 lower file 삭제를 merged view에서 표현한다

Read-only lower layer 파일을 실제로 삭제할 수 없으므로 upper layer에 whiteout metadata를 두어 merged view에서 숨길 수 있다. Image layer diff에서 `파일이 없어졌다`는 것은 lower data가 제거됐다는 뜻이 아닐 수 있다.

Container image forensic/storage size 분석은 merged tree만 보면 unused lower bytes를 놓친다.

---

## CHAPTER 21 · Bind mount는 image immutability와 runtime data를 연결한다

Host path나 volume을 container path에 bind mount하면 image의 해당 path 내용이 runtime view에서 가려질 수 있다. 동일 image가 deployment마다 다른 file을 읽을 수 있는 이유다.

Configuration provenance는 image digest뿐 아니라 mount source/options까지 포함해야 재현 가능하다.

---

## CHAPTER 22 · Read-only rootfs는 write path를 명시하게 만든다

Root filesystem을 read-only로 두면 application이 암묵적으로 local file에 state를 쓰는 문제를 조기에 발견하고 writable volume/tmpfs를 명시적으로 분리할 수 있다. 이는 immutability와 least privilege를 강화한다.

Log/cache/temp file이 필요한 path를 따로 설계하지 않으면 runtime failure가 발생한다. Read-only 설정은 security flag 하나가 아니라 state architecture contract다.

---

## CHAPTER 23 · Container init는 signal forwarding과 zombie reaping을 책임져야 한다

Container PID 1이 shell wrapper나 application이면 SIGTERM handling과 child reaping behavior가 일반 process와 다를 수 있다. Orchestrator가 stop signal을 보내도 child process까지 전달되지 않으면 graceful shutdown이 실패한다.

Init process는 shutdown deadline, signal forwarding, orphan reaping을 명확히 처리해야 한다.

---

## CHAPTER 24 · OCI hook은 lifecycle에 host-side privileged code를 삽입한다

Runtime hook은 create/start/poststop 등 특정 lifecycle phase에 external executable을 실행할 수 있다. Device/network/security setup에 유용하지만 untrusted bundle이 privileged hook을 통제하면 host attack surface가 된다.

Hook source와 arguments를 runtime policy로 제한하고 timeout/failure semantics를 정의한다.

---

## CHAPTER 25 · Rootless container도 모든 host attack surface를 제거하지 않는다

User namespace mapping으로 host root privilege 없이 container를 실행해 blast radius를 줄일 수 있지만 shared kernel syscall attack surface와 unprivileged namespace/kernel feature bug는 남는다. Network/storage helper를 위해 setuid helper나 privileged daemon을 사용하면 새로운 trust boundary가 생긴다.

Rootless는 privilege reduction이고 VM-level kernel separation과 동일하지 않다.

---

## CHAPTER 26 · Container image digest는 runtime state 전체를 식별하지 않는다

Image digest가 같아도 environment variable, secret, bind mount, network policy, cgroup limit, seccomp profile이 다르면 execution environment가 다르다. Incident artifact에는 image digest와 함께 OCI config/runtime policy를 남겨야 한다.

`동일 image니까 재현된다`는 결론은 deployment state가 동결됐을 때만 성립한다.

---

## CHAPTER 27 · Checkpoint/restore는 process state와 namespace resource를 함께 복원해야 한다

Container checkpoint는 memory/register뿐 아니라 open fd, socket, timer, namespace object, cgroup state와 external resource dependency를 다뤄야 한다. 모든 kernel object가 portable하게 restore 가능한 것은 아니다.

Migration/checkpoint capability는 workload가 사용하는 resource set의 교집합으로 결정된다. Unsupported fd 하나가 전체 restore를 막을 수 있다.

---

## CHAPTER 28 · Container observability는 host PID와 namespace PID를 연결해야 한다

Host eBPF/ftrace는 host task identity를 보지만 application log는 container PID/hostname을 기록할 수 있다. Cgroup ID, namespace inode, container runtime metadata를 이용해 event를 workload identity에 mapping한다.

PID 숫자만 join하면 PID reuse와 namespace collision 때문에 잘못된 attribution이 생길 수 있다.

---

## CHAPTER 29 · Container escape risk는 isolation layer의 합집합에서 생긴다

Attack surface에는 syscall, mounted filesystem, device, capability, network, runtime socket, container management API가 모두 포함된다. 한 layer가 강해도 privileged Docker/socket mount 같은 다른 boundary가 host control을 노출할 수 있다.

Security review는 namespace 목록보다 **container에서 host privilege로 이어지는 모든 edge**를 graph로 만든다.

---

## CHAPTER 30 · Container correctness의 최종 계약은 view·quota·privilege·filesystem·lifecycle이다

Container runtime을 검토할 때 다섯 축을 동시에 확인한다.

1. **View isolation** — namespace가 process-visible global state를 필요한 범위로 분리하는가.
2. **Resource control** — cgroup이 CPU/memory/I/O/PID budget을 실제로 제한하는가.
3. **Privilege reduction** — user namespace, capability, seccomp, LSM이 불필요한 kernel power를 제거하는가.
4. **Filesystem state** — rootfs/layer/mount가 immutable artifact와 runtime state를 명확히 분리하는가.
5. **Lifecycle** — create/start/signal/stop/cleanup 과정에서 resource가 leak되거나 privilege가 남지 않는가.

`컨테이너로 실행했다`는 사실은 이 다섯 조건의 통과를 자동으로 보장하지 않는다.
