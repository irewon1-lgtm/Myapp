# PART 75 · Credentials, Capabilities and LSM Authorization — identity, privilege sets, exec transitions, policy hooks

`root냐 아니냐`만으로 Linux authorization을 설명할 수 없다. 한 task의 security state에는 **real/effective/saved/fs UID/GID, supplementary groups, capability sets, securebits, user namespace, no_new_privs, LSM security blob**이 결합되고, `execve`는 이 state를 다시 계산한다. Filesystem·process-control·network·BPF 같은 operation은 서로 다른 credential fields와 LSM hooks를 사용한다. 권한 bug를 잡으려면 `누구인가`보다 **어떤 operation이 어떤 credential snapshot과 policy layer를 평가했는가**를 추적해야 한다.

## CHAPTER 01 · Credential은 task identity의 immutable snapshot처럼 다뤄진다

Kernel은 task가 사용하는 `struct cred`를 reference-counted object로 관리하고, credential 변경 시 기존 object를 field-by-field 수정하기보다 prepare/commit 형태로 새 credentials를 publish하는 model을 사용한다. 여러 kernel paths가 같은 credential pointer를 lock 없이 읽을 수 있기 때문이다. Authorization 도중 identity가 반쯤 바뀌는 상태를 노출하지 않는 것이 중요하다. Userspace에서 `setuid 호출 중 다른 thread가 어떤 uid를 봤나` 같은 질문도 process/thread credential synchronization semantics를 확인해야 한다.

## CHAPTER 02 · Real UID는 provenance와 일부 signal/accounting semantics에 사용된다

Real UID는 process를 누가 시작했는지에 가까운 identity이고 login/accounting, 일부 permission transition에서 기준이 될 수 있다. 하지만 일반 filesystem access가 항상 real UID로 평가되는 것은 아니다. Security log가 real UID만 기록하면 실제 authorization에 사용된 effective/fs credential을 놓칠 수 있다. Incident evidence에는 real/effective/fs IDs를 구분해 남겨야 한다.

## CHAPTER 03 · Effective UID는 많은 permission checks의 active identity다

Traditional Unix model에서 effective UID가 filesystem ownership checks와 privileged-operation 판단의 주요 identity로 사용된다. Set-user-ID executable이나 credential-changing syscall이 effective ID를 바꿀 수 있다. Real UID와 effective UID가 다른 process를 단순 `user X process`로 표시하면 privilege transition을 숨긴다. Authorization debugging은 operation 시점의 effective identity를 캡처해야 한다.

## CHAPTER 04 · Saved set-user-ID는 privilege를 잠시 내렸다 되찾는 transition state다

Privileged program이 effective UID를 unprivileged value로 바꿔도 saved ID를 유지하면 이후 다시 privilege를 얻을 수 있는 경우가 있다. `지금 euid가 non-root`라는 관찰만으로 future privilege regain 가능성을 판단할 수 없다. Secure daemon은 privilege dropping이 temporary인지 permanent인지 명시하고 saved IDs와 capabilities까지 함께 정리해야 한다.

## CHAPTER 05 · fsuid/fsgid는 filesystem access identity를 별도로 조정할 수 있다

Linux에는 filesystem permission check를 위해 fsuid/fsgid를 별도 state로 사용할 수 있는 역사적 interface가 있다. 대부분의 modern code는 effective IDs와 동일하게 유지하지만 network filesystem server 같은 특수 code는 의미가 달라질 수 있다. `/proc`이나 audit에서 euid만 보고 `왜 file open이 거부됐나`를 분석하면 fs credential 차이를 놓칠 수 있다.

## CHAPTER 06 · Supplementary groups는 group authorization surface를 확장한다

Primary GID 하나 외에도 process는 여러 supplementary group IDs를 갖고 file group permission과 IPC/resource access에 영향을 줄 수 있다. Container/user-namespace 환경에서는 host와 namespace group mapping이 달라질 수 있다. Group membership cache가 authentication backend와 stale하면 새 login/session 이전까지 privilege가 달라질 수 있으므로 process credential 생성 시점을 추적해야 한다.

## CHAPTER 07 · Capability는 root privilege를 operation classes로 분해한다

Linux capabilities는 `CAP_NET_ADMIN`, `CAP_SYS_PTRACE`처럼 전통적 root 권한을 여러 bits로 분리해 필요한 privilege만 부여할 수 있게 한다. 그러나 capability 하나가 매우 넓은 kernel surface를 열 수 있고 namespace scope에 따라 의미가 달라진다. `root가 아니므로 안전` 또는 `cap 하나뿐이라 최소권한`이라는 평가는 각 capability가 허용하는 syscalls/ioctls/namespaces를 실제 threat model과 대조해야 한다.

## CHAPTER 08 · Permitted set은 process가 현재/향후 effective로 올릴 수 있는 상한이다

Permitted capabilities에 없는 bit를 ordinary userspace가 effective set에 새로 추가할 수 없다. 따라서 permitted set은 process privilege reservoir와 비슷한 역할을 한다. 현재 effective set이 작아도 permitted set에 강한 capability가 남아 있으면 code path가 다시 활성화할 수 있다. Permanent privilege drop은 effective만 지우는 것으로 끝나지 않는다.

## CHAPTER 09 · Effective set은 현재 capability checks에 실제 사용되는 bits다

Kernel capability check는 보통 task의 effective set에서 요구 capability를 확인한다. Program이 startup에 필요한 privileged resource를 열고 effective bit를 내리면 steady state attack surface를 줄일 수 있다. 하지만 inherited open fds와 privileged kernel objects는 capability drop 이후에도 authority를 유지할 수 있으므로 credential state와 object capability lifetime을 함께 정리해야 한다.

## CHAPTER 10 · Inheritable set은 exec를 통한 capability 전달 규칙의 한 입력이다

Inheritable capabilities는 file inheritable mask와 결합해 new executable의 permitted set 형성에 영향을 줄 수 있다. 단순 child-process fork inheritance와 다르게 exec transition에서 file metadata가 개입한다. Build/deploy artifact의 file capability와 process inheritable state를 둘 다 모르면 exec 후 실제 privilege를 예측할 수 없다.

## CHAPTER 11 · Bounding set은 exec로 획득 가능한 file capability의 system/process 상한을 줄인다

Capability bounding set에서 bit를 제거하면 이후 file capability를 통해 그 bit를 permitted로 획득하는 경로를 제한할 수 있다. 이 reduction은 one-way 성격을 가져 sandbox hardening에 유용하다. Effective/permitted를 잠시 0으로 만들었다 다시 privileged exec로 회복하는 경로까지 막으려면 bounding set과 securebits/no_new_privs를 함께 설계해야 한다.

## CHAPTER 12 · Ambient capabilities는 ordinary non-privileged exec에서도 유지될 수 있는 별도 set이다

Ambient set은 permitted와 inheritable에 포함된 capabilities를 exec 후에도 유지하는 mechanism을 제공한다. Privileged file exec에서는 규칙이 달라지고 ambient가 clear될 수 있다. Service launcher가 ambient cap을 사용하면 child binary가 file capabilities를 갖지 않아도 privilege가 이어질 수 있으므로 unit/container manifest와 process status를 함께 감사해야 한다.

## CHAPTER 13 · File capabilities는 executable inode에 privilege transition metadata를 붙인다

Setuid root 대신 file extended attributes로 permitted/effective capability mask를 지정할 수 있다. Exec 시 process capability state와 file capability, bounding set, user namespace relationship을 조합해 new sets가 계산된다. Binary contents만 hash 검증하고 file xattr을 release evidence에서 빼면 동일 binary가 서로 다른 privilege로 실행될 수 있다.

## CHAPTER 14 · setuid/setgid executable도 exec credential recomputation의 일부다

File mode의 set-user-ID/set-group-ID bits는 조건이 맞으면 effective/saved IDs를 file owner/group으로 전환한다. `no_new_privs`, mount nosuid, tracing 상태 같은 조건은 이 transition을 제한할 수 있다. Permission bit가 보인다고 실제 privilege elevation이 반드시 발생하는 것은 아니며, exec-time context가 최종 result를 결정한다.

## CHAPTER 15 · no_new_privs는 exec를 통한 새 privilege 획득을 막는 강한 one-way flag다

`PR_SET_NO_NEW_PRIVS`를 설정하면 이후 exec에서 setuid/setgid/file capabilities 때문에 현재보다 privilege가 상승하는 것을 제한한다. Seccomp filter를 unprivileged process가 설치할 때도 중요한 전제다. 이 flag는 이미 가진 open fds/capabilities를 자동 제거하지 않으므로 **새 privilege 획득 차단**과 **현재 authority 제거**를 분리해야 한다.

## CHAPTER 16 · securebits는 UID 0과 capability의 전통적 연결 규칙을 조정한다

Root UID로 바뀔 때 capabilities를 자동 획득하거나 UID 0에서 내려올 때 capability를 지우는 legacy semantics를 securebits로 조절할 수 있다. Keep-caps 같은 설정은 credential transition을 세밀하게 만들지만 복잡성도 높인다. Privilege drop code는 uid/cap API 호출 순서와 securebits를 함께 검증해야 하며 테스트는 exec 전후 state를 실제로 관찰해야 한다.

## CHAPTER 17 · User namespace는 UID/GID와 capability scope를 새 identity domain으로 만든다

User namespace 안의 UID 0은 host initial namespace의 root와 동일하지 않다. Namespace별 UID/GID mapping이 host IDs와 container-visible IDs를 연결하고 capability는 특정 user namespace와 그 descendant scope에서 평가될 수 있다. `container root`라는 label만으로 host privilege를 추론하면 안 된다. Operation이 소유하는 kernel object가 어느 user namespace와 연결되는지 확인해야 한다.

## CHAPTER 18 · UID/GID map 작성 자체가 privilege-controlled transition이다

새 user namespace의 identity mapping을 누가 어떤 host IDs로 설정할 수 있는지는 `/proc/<pid>/uid_map`, `gid_map`, setgroups policy와 privilege rule에 제한된다. Arbitrary host root mapping을 허용하면 namespace isolation이 무너진다. Container runtime은 mapping 설정을 setup phase authority로 취급하고 child가 untrusted code를 실행하기 전에 완료/lockdown해야 한다.

## CHAPTER 19 · Namespace capability check는 target object의 owning user namespace를 기준으로 해석한다

Capability bit가 effective set에 있어도 target inode/network namespace/mount가 initial 또는 다른 user namespace에 속하면 권한이 충분하지 않을 수 있다. 반대로 namespace 내부 object에는 host unprivileged process가 namespace-root capability로 관리할 수 있다. Capability 이름만 로그에 남기지 말고 credential user namespace와 target namespace identity를 함께 기록해야 한다.

## CHAPTER 20 · Kernel은 syscall 안에서 stable credential reference를 사용해야 한다

Authorization check 도중 다른 thread의 credential transition 때문에 identity fields가 반쯤 변하면 check와 use가 다른 principal을 적용할 수 있다. Immutable/refcounted credential object와 RCU-style replacement는 syscall path가 stable snapshot을 참조하게 한다. Security-sensitive kernel code는 current credentials와 explicitly passed override credentials의 lifetime을 명확히 해야 한다.

## CHAPTER 21 · override_creds는 kernel worker가 다른 principal의 권한으로 제한된 작업을 수행하게 한다

Filesystem/network subsystem은 background worker가 original request principal을 대신해 operation을 수행해야 할 수 있다. Temporary credential override를 사용하면 current kernel thread identity와 request authority를 분리할 수 있지만 restore가 누락되면 이후 unrelated work가 잘못된 privilege로 실행된다. Scope-based save/restore와 error-path coverage가 필수다.

## CHAPTER 22 · File permission check는 DAC mode bits와 capabilities만으로 끝나지 않는다

Unix owner/group/mode/ACL DAC를 통과한 뒤 LSM hook이 SELinux/AppArmor 같은 mandatory policy를 추가로 거부할 수 있다. 반대로 capability가 DAC 일부를 우회해도 LSM policy는 독립적으로 제한할 수 있다. `chmod 777인데 Permission denied` 또는 `root인데 denied` 같은 사건은 authorization pipeline의 어느 layer가 거부했는지 분리해야 한다.

## CHAPTER 23 · LSM은 security-critical kernel operation에 policy hooks를 삽입한다

LSM framework는 inode, file open, task, socket, BPF 등 여러 kernel object lifecycle 지점에 hooks를 제공한다. Security module은 credential/object security blob과 policy를 사용해 allow/deny를 결정한다. Hook이 호출되는 시점은 `userspace syscall 이름`과 항상 1:1이 아니므로 audit는 object operation 관점에서 봐야 한다.

## CHAPTER 24 · SELinux type enforcement는 UID와 별도의 label domain을 사용한다

Process/domain label과 file/socket object type 사이의 allowed class/permission을 policy가 정의한다. 같은 UID로 실행하는 두 services도 domain이 다르면 접근 권한이 달라진다. DAC가 허용해도 TE가 deny할 수 있고 domain transition은 exec file label과 policy에 의해 일어날 수 있다. Incident log에는 UID뿐 아니라 security context가 필요하다.

## CHAPTER 25 · Authorization check와 object use 사이 TOCTOU는 credential뿐 아니라 object identity에도 존재한다

Path permission을 검사한 뒤 pathname을 다시 open하면 namespace가 바뀔 수 있고, fd permission을 확인한 뒤 fd number가 재사용될 수 있다. Security decision은 가능한 한 stable inode/file/socket reference에 묶어 수행해야 한다. P71/P72의 fd/path identity 문제와 credential snapshot을 동시에 지켜야 `누가 무엇에 권한이 있었는가`가 실제 use와 일치한다.

## CHAPTER 26 · Privilege drop 뒤에도 inherited object capabilities가 남을 수 있다

Root/capabilities를 버려도 이미 open한 raw device, listening socket, namespace fd, BPF map/program, directory fd가 privileged operation을 계속 허용할 수 있다. Least-privilege design은 credentials를 낮추는 단계와 privileged resources를 close/restrict/pass하는 단계를 모두 포함한다. `setuid(nobody) 성공`만으로 sandbox가 완성됐다고 판단하면 안 된다.

## CHAPTER 27 · Exec transition audit는 before/after credential vector를 비교해야 한다

Setuid bit, file capabilities, no_new_privs, ambient/inheritable/bounding sets, user namespace, securebits가 결합되므로 exec 후 privilege는 직관만으로 예측하기 어렵다. Test harness는 exec 전 `/proc/self/status`와 after-exec helper의 UID/GID/cap state를 캡처해 expected transition과 비교해야 한다. Packaging metadata까지 reproducible artifact에 포함해야 한다.

## CHAPTER 28 · Authorization failure observability는 거부 layer와 required permission을 기록해야 한다

`EPERM/EACCES`만 application log에 남기면 DAC, capability, seccomp, LSM, namespace mapping 중 무엇이 거부했는지 모른다. Audit/LSM denial log, effective credentials, capability sets, target inode/socket labels, mount/user namespace를 correlation해야 한다. 하지만 secret paths/labels를 과도하게 노출하지 않도록 production logging policy도 필요하다.

## CHAPTER 29 · Credential confusion test는 setuid/fork/exec/namespace race를 조합해야 한다

Single-thread happy-path test로는 privilege transition bug를 잡기 어렵다. Multiple threads, fd inheritance, user namespace creation, no_new_privs, file capabilities, LSM enforcing mode를 조합해 child가 예상보다 높은 privilege를 얻거나 필요한 authority를 잃는지 검증해야 한다. Security invariant는 `exec 뒤 effective cap set ⊆ allowlist`, `untrusted child CLOEXEC leaks=0`처럼 machine-checkable하게 만든다.

## CHAPTER 30 · Linux authorization은 principal state와 object policy가 만나는 pipeline이다

정확한 권한 모델은 **UID/GID variants, supplementary groups, capability five-set/bounding logic, file capabilities, securebits, no_new_privs, user namespace mappings, immutable credential snapshots, object identity, LSM/SELinux policy, inherited fd authority**를 한 pipeline으로 본다. `root`, `user`, `permission denied` 같은 label은 결과만 요약한다. 실제 설계와 진단은 어떤 credential vector가 어떤 stable object에 어떤 policy layer를 거쳐 operation을 허용/거부했는지를 증명해야 한다.
