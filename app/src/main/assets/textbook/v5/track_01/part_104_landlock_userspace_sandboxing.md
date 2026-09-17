# PART 104 · Landlock Userspace Sandboxing — ruleset FDs, domains, path-beneath policy, inheritance, layering, compatibility

Landlock를 “권한 없는 프로세스도 쓰는 파일 접근 차단 기능” 정도로만 이해하면 실제 contract가 흐려진다. Landlock은 userspace가 ruleset object를 만들고, 어떤 access-right class를 다룰지 선언하고, filesystem object를 anchor로 rule을 추가한 뒤, current thread를 새 security domain 안으로 제한하는 **one-way policy installation pipeline**이다. Policy는 UID를 새로 부여하지 않고 기존 DAC·capability·다른 LSM 권한을 추가로 깎는다. Ruleset FD lifetime, `O_PATH` parent FD, ABI negotiation, handled rights, rule layering, fork/exec inheritance, 이미 열린 file descriptor의 authority, mount/rename topology 같은 경계가 함께 맞아야 sandbox가 예상대로 작동한다.

## CHAPTER 01 · Landlock는 권한을 부여하는 ACL이 아니라 기존 권한 위에 추가 제한을 쌓는 unprivileged LSM interface다

Landlock policy는 process가 원래 없던 filesystem 권한을 새로 만들어 주지 않는다. DAC, capabilities, 다른 LSM 정책이 이미 허용하는 operation 중 Landlock ruleset이 다루도록 선언한 access class를 다시 제한한다. 그래서 Landlock rule에 path를 “허용”했다고 해도 Unix mode bit나 SELinux가 거절하면 접근은 계속 실패한다. 반대로 Landlock가 다루지 않는 access right는 그 ruleset layer가 판단하지 않는다. 이 성질을 모르면 sandbox test에서 성공을 “Landlock가 grant했다”고 잘못 해석할 수 있다. 정확한 모델은 **baseline authority ∩ Landlock layer 1 ∩ Landlock layer 2 …**처럼 권한 집합이 좁아지는 방향이다. 따라서 policy 설계는 무엇을 허용할지뿐 아니라 어떤 access class를 Landlock가 책임지게 할지부터 정의해야 한다.

## CHAPTER 02 · ABI version query는 기능 탐지가 아니라 userspace와 running kernel의 정책 언어 범위를 맞추는 첫 단계다

Landlock은 kernel이 발전하면서 새로운 access right와 rule type을 추가할 수 있으므로 compile-time header만 보고 running kernel이 같은 기능을 지원한다고 가정하면 안 된다. Userspace는 실행 중 kernel이 이해하는 Landlock ABI 범위를 질의하고, 자신이 요구하는 right가 그 범위에 들어오는지 결정해야 한다. 최신 header로 빌드한 binary가 오래된 kernel에서 unknown right를 전달하면 ruleset 생성 단계에서 실패할 수 있다. 반대로 오래된 binary가 새 kernel에서 동작할 때는 자신이 모르는 새 restriction을 자동으로 얻는다고 기대하면 안 된다. **Policy compatibility는 syscall 존재 여부가 아니라 handled access vocabulary의 교집합**으로 계산해야 한다. 운영 로그에는 detected ABI와 실제 활성화한 rights 집합을 남겨야 동일 binary의 환경별 차이를 설명할 수 있다.

## CHAPTER 03 · landlock_create_ruleset()이 돌려주는 FD는 policy source object의 lifetime handle이다

Ruleset 생성이 성공하면 userspace는 file descriptor 형태의 ruleset handle을 받는다. 이 FD는 일반 data file이 아니지만 close, duplication, inheritance 같은 file-descriptor lifetime 법칙을 따른다. Ruleset을 조립하는 동안 FD를 잘못 close하거나 다른 thread가 숫자만 재사용하면 이후 `landlock_add_rule()`가 전혀 다른 object를 대상으로 실패할 수 있다. 따라서 FD 번호 자체를 영구 identity로 보지 말고 creation point와 ownership scope를 관리해야 한다. 또한 enforcement를 끝낸 뒤 policy construction FD가 계속 필요하지 않다면 명시적으로 close해 leak을 막아야 한다. **Ruleset object lifetime과 enforced domain lifetime은 같지 않다**는 점도 중요하다. Domain restriction은 ruleset FD를 닫는다고 되돌아가지 않는다.

## CHAPTER 04 · handled_access_fs는 “이 ruleset이 판정할 filesystem 권한 집합”을 선언하는 schema다

Ruleset 생성 시 filesystem access mask를 지정하는 것은 단순 metadata 설정이 아니다. 이 mask가 이후 layer에서 통제되는 operation class를 고정한다. 어떤 right를 handled set에 포함했는데 대응되는 allow rule을 만들지 않으면 그 domain에서 해당 operation이 거절될 수 있다. 반대로 mask에 넣지 않은 right는 그 layer가 제한하지 않는다. 그래서 “모든 것을 막고 일부만 열자”는 정책은 kernel ABI가 지원하는 rights를 정확히 구성해야 하고, “read-only sandbox”도 execute, read-file, read-dir, remove, make, refer, truncate 같은 서로 다른 operation을 구분해야 한다. **Handled set은 policy의 type system**이며 rule contents보다 먼저 검증해야 한다.

## CHAPTER 05 · Unknown access right는 무시 대상이 아니라 compatibility failure로 다뤄야 한다

Security API에서 모르는 bit를 조용히 무시하면 application은 자신이 제한했다고 믿는 operation을 실제로는 제한하지 못할 수 있다. Landlock userspace는 running kernel의 ABI를 확인하고 그 kernel이 이해하는 access rights만 전달해야 한다. Required right가 지원되지 않는다면 두 가지 선택이 있다. Sandbox 기능 자체를 fail closed로 중단하거나, 명시적으로 더 약한 compatibility profile로 내려가며 그 사실을 기록하는 것이다. 어느 쪽이든 silently continue하면 안 된다. **Security degradation은 control flow에 드러나야 한다.** 테스트도 최신 kernel에서의 정상 path뿐 아니라 일부 right가 없는 환경을 시뮬레이션해 application이 예상한 fallback 또는 거절을 수행하는지 확인해야 한다.

## CHAPTER 06 · PATH_BENEATH rule은 문자열 prefix가 아니라 열린 filesystem object를 parent anchor로 삼는다

Filesystem rule은 `/srv/app` 같은 문자열을 kernel에 넘겨 prefix match를 수행하는 방식이 아니다. Userspace는 제한 기준이 될 directory나 file을 적절한 FD로 열고, 그 object를 parent로 하는 path-beneath rule을 추가한다. 이 덕분에 policy construction 시점의 path resolution과 enforcement 시점의 access check가 분리된다. 문자열 재해석에 의존하지 않으므로 symlink spelling만으로 rule 의미가 바뀌지는 않지만, 이후 rename·mount topology 변화가 object reachability에 어떤 영향을 주는지는 별도 문제다. **Rule anchor는 pathname text가 아니라 kernel filesystem object reference**라는 점을 이해해야 debugging에서 “설정 파일에는 이 문자열이 맞다” 이상의 증거를 수집할 수 있다.

## CHAPTER 07 · O_PATH FD는 rule anchor를 잡는 capability-like reference이며 ordinary read 권한과 동일하지 않다

Landlock rule을 만들 때 parent path를 `O_PATH` 계열 FD로 참조하는 패턴이 중요하다. `O_PATH` descriptor는 file contents를 읽기 위한 일반 open handle과 다르고 pathname object를 안정적으로 지칭하는 데 적합하다. 하지만 “O_PATH니까 권한이 없다”고 단순화하면 안 된다. 그 FD는 후속 *at 계열 resolution이나 metadata operation에 사용할 수 있는 reference이며 전달되면 capability-like authority가 생길 수 있다. Sandbox builder가 privileged setup phase에서 O_PATH FD를 열고 worker로 넘길 때는 그 descriptor 자체의 lifetime과 close-on-exec 정책도 관리해야 한다. **Path anchor와 data access handle을 분리**하면 policy construction과 runtime authority를 더 명확히 격리할 수 있다.

## CHAPTER 08 · Rule이 허용하는 것은 parent subtree 전체가 아니라 선언된 access mask와 object topology의 교차다

Path-beneath rule 하나를 추가했다고 해서 해당 subtree의 모든 filesystem operation이 허용되는 것은 아니다. 그 rule은 특정 parent 아래에서 어떤 handled rights를 허용할지 mask로 표현한다. Read-file은 허용하지만 remove-file이나 make-reg는 거절할 수 있고, directory listing과 file read도 별개일 수 있다. Sandbox가 “이 디렉터리는 허용”이라는 UI만 제공하면 내부적으로 어떤 rights를 줬는지 보이지 않아 policy review가 불가능해진다. 따라서 policy artifact에는 **anchor identity + allowed access set**을 함께 직렬화하거나 log로 남겨야 한다. 테스트도 directory별 allow/deny가 아니라 operation matrix로 수행해야 한다.

## CHAPTER 09 · Directory read와 regular-file read는 다른 access class이므로 데이터 노출 경로가 다르다

디렉터리 내용을 열람해 이름을 나열하는 행위와 파일 payload를 읽는 행위는 같은 read가 아니다. Sandbox가 file read만 제한하고 directory enumeration을 허용하면 민감한 object 이름이나 존재 여부가 metadata channel로 노출될 수 있다. 반대로 directory listing을 막아도 이미 정확한 path를 아는 process가 file read 권한을 별도로 가지고 있으면 payload 접근이 가능할 수 있다. 이 차이는 secret directory, plugin discovery, per-user spool 같은 구조에서 중요하다. **Confidentiality threat model은 content와 namespace metadata를 분리**해야 한다. Evidence도 `open/read` 실패와 `getdents` 계열 enumeration 실패를 따로 확인해야 한다.

## CHAPTER 10 · Creation·removal rights는 대상 object type별로 나뉘어 mutation surface를 세밀하게 제한한다

Filesystem write를 하나의 boolean으로 다루면 sandbox가 실제로 허용하는 namespace mutation을 설명하기 어렵다. Directory, regular file, symlink, socket, FIFO, device node 같은 object 생성과 file/directory 제거는 서로 다른 rights로 모델링될 수 있다. Application이 cache file만 만들어야 한다면 필요한 creation class만 허용하고 symlink나 device node creation은 막는 식의 축소가 가능하다. 이는 단순 read-only/read-write 구분보다 attack surface를 크게 줄인다. 특히 writable directory에서 symlink creation을 허용하면 privileged component와의 path confusion이 생길 수 있으므로 **object-kind별 mutation authority**를 명시해야 한다.

## CHAPTER 11 · REFER right는 rename과 link가 directory hierarchy를 가로지를 때 생기는 별도 authority를 다룬다

파일을 읽거나 쓰는 권한과 namespace entry를 다른 directory hierarchy로 이동·연결하는 권한은 동일하지 않다. Cross-directory rename이나 hard-link는 object contents를 바꾸지 않고도 어느 subtree에서 object가 reachable한지를 변경한다. Landlock의 refer 관련 제한은 이런 topology mutation을 별도 security boundary로 취급한다. Policy가 source와 destination 양쪽 subtree에 적절한 권한을 주지 않으면 rename이 거절될 수 있으며, 단순 write permission만 보고 성공을 예상하면 안 된다. **Namespace topology 변경은 payload mutation과 다른 권한 축**이라는 점이 container root, upload staging, build sandbox에서 중요하다.

## CHAPTER 12 · TRUNCATE 같은 후속 access right는 “write 허용”과 file length mutation을 분리해 compatibility 문제를 만든다

Kernel ABI가 확장되면서 기존 broad write 모델에서 별도 access right가 추가될 수 있다. File truncate처럼 data length를 바꾸는 operation이 독립 right로 다뤄지면 오래된 policy가 새 kernel에서 자동으로 더 강한 제한을 얻는다고 기대할 수 없다. Userspace는 자신이 의도한 security property를 어떤 rights 조합으로 표현하는지 ABI별 table을 가져야 한다. 예를 들어 “파일 내용은 append/write 가능하지만 arbitrary truncate는 금지” 같은 정책은 관련 right가 지원되는 환경에서만 정확히 표현될 수 있다. **Security intent와 kernel access-bit vocabulary 사이의 mapping을 versioned contract로 관리**해야 한다.

## CHAPTER 13 · Network access rights가 지원되는 환경에서도 bind와 connect는 filesystem policy와 독립된 class다

Landlock는 발전 과정에서 filesystem 외의 network action을 제한할 수 있는 policy class를 확장해 왔다. TCP port bind나 outbound connect 같은 operation을 다룰 때는 path rule과 다른 rule type·object identity를 사용한다. “Landlock sandbox니까 네트워크도 자동 격리된다”는 가정은 위험하다. Running kernel ABI와 ruleset configuration이 해당 network rights를 실제로 handled set에 포함했는지 확인해야 한다. 또한 port number restriction은 peer identity, DNS, protocol payload까지 검증하는 firewall과 동일하지 않다. **Network Landlock는 특정 socket operation authority를 줄이는 층**이지 full network policy stack의 대체물이 아니다.

## CHAPTER 14 · no_new_privs는 unprivileged task가 자신과 descendants에 제한을 설치할 수 있게 만드는 핵심 전제다

Unprivileged process가 security policy를 설치하는 API는 그 policy를 이용해 다른 security model을 우회하거나 privilege gain path를 만들 수 없어야 한다. Landlock self-restriction에서 `no_new_privs`는 이후 exec를 통한 privilege acquisition을 차단하는 전제와 결합된다. Setup 코드가 ruleset 생성만 하고 `landlock_restrict_self()` 전에 필요한 precondition을 세우지 않으면 enforcement syscall이 실패할 수 있다. 더 중요한 것은 순서다. Worker thread가 실행을 시작한 뒤 늦게 `no_new_privs`와 Landlock을 설정하면 이미 privileged action이 일어났을 수 있다. **Privilege ceiling을 먼저 고정하고 domain restriction을 설치**하는 startup sequence를 하나의 transaction처럼 설계해야 한다.

## CHAPTER 15 · restrict_self는 current thread를 새 Landlock domain에 넣는 irreversible authority reduction이다

Landlock enforcement는 “잠깐 켰다가 끄는 mode flag”가 아니다. Current thread가 ruleset을 적용하면 이후 그 thread가 속한 security domain은 더 제한된 상태가 되며 일반적인 userspace API로 원래 넓은 권한으로 되돌아갈 수 없다. 그래서 initialization 중 잘못된 path rule을 적용하면 process가 필요한 config, locale, shared library, socket file에 접근하지 못해 스스로 복구할 수 없을 수 있다. 적용 전 dry-run이 없다고 가정하고 **필요 resource open → policy build → validation → restrict_self → post-enforcement smoke check** 순서를 명확히 해야 한다. Rollback은 domain 해제가 아니라 process 재시작으로 설계하는 편이 안전하다.

## CHAPTER 16 · Thread 단위 적용과 process 내부 concurrency를 혼동하면 일부 thread만 sandbox 밖에 남을 수 있다

Security state가 current thread 기준으로 설치되는 API에서는 multithreaded process가 특히 위험하다. 한 thread가 Landlock domain에 들어갔다고 해서 이미 존재하는 다른 sibling thread가 자동으로 동일 순간 제한된다고 가정하면 안 된다. Sandbox 적용 시점 전에 worker pool을 생성했다면 각 thread의 domain inheritance와 실제 restriction 상태를 확인해야 한다. 가장 단순한 전략은 **single-threaded bootstrap에서 제한을 설치한 뒤 descendants를 생성**하는 것이다. 이미 multithreaded인 daemon을 부분적으로 제한하려면 thread lifecycle과 synchronization을 별도 설계해야 한다. Testing에서는 각 thread가 금지 path에 실제로 접근해 동일 결과를 보이는지 확인해야 한다.

## CHAPTER 17 · fork/clone으로 생성되는 descendants는 parent security domain inheritance를 threat model에 포함해야 한다

Landlock를 적용한 thread가 이후 child를 만들면 descendants가 어떤 domain을 이어받는지가 sandbox persistence의 핵심이다. 만약 child가 아무 제약 없이 원래 namespace 권한으로 돌아갈 수 있다면 self-restriction은 의미가 없어진다. 따라서 process tree에서 제한이 어떻게 상속되는지를 전제로 worker model을 설계하고, fork 후 child에서 forbidden operation을 직접 검증해야 한다. 동시에 parent가 제한 전에 fork한 helper는 더 넓은 권한을 유지할 수 있으므로 intentional broker architecture인지 accidental escape path인지 구분해야 한다. **Sandbox boundary는 PID 하나가 아니라 creation ordering을 포함한 process lineage**로 정의해야 한다.

## CHAPTER 18 · exec는 새 program image로 바뀌어도 Landlock domain을 privilege reset처럼 제거하지 않는다

Sandboxed process가 다른 binary를 exec할 수 있다면 새 program이 같은 filesystem authority 범위 안에서 실행되는지 이해해야 한다. Exec가 domain을 제거한다면 helper binary 하나로 escape할 수 있으므로 security model은 restriction persistence와 `no_new_privs` 전제를 함께 본다. Application은 “exec 후 초기화 코드가 다시 Landlock를 건다”에만 의존하지 말고 parent가 만든 domain이 descendants의 executable transition에도 적용되는지 검증해야 한다. 반대로 sandbox 안에서 package manager나 compiler를 실행해야 한다면 필요한 paths가 모두 ruleset에 포함되어 있어야 한다. **Program image replacement와 security domain lifetime은 별개**다.

## CHAPTER 19 · Layering은 새 ruleset을 적용할수록 권한을 넓히지 못하고 추가 intersection을 만든다

Landlock domain에 이미 들어간 task가 추가 ruleset을 적용하면 새 layer는 기존 허용 범위를 다시 제한한다. 후속 layer에서 더 넓은 directory를 허용한다고 해서 이전 layer가 막은 path가 다시 열리는 것이 아니다. 이 monotonic property는 library나 plugin이 자신에게 필요한 범위를 추가로 줄일 수 있게 하지만, “configuration reload로 정책을 완화”하는 기능은 같은 process 안에서 구현하기 어렵게 만든다. Policy update가 완화를 필요로 한다면 새 process generation을 띄우고 traffic을 전환하는 방식이 자연스럽다. **Layer count와 각 layer의 handled rights를 기록**해야 왜 특정 operation이 최종적으로 거절됐는지 설명할 수 있다.

## CHAPTER 20 · Rule 없음은 access grant가 아니라 handled right에 대해 deny-by-default가 될 수 있다

Ruleset이 특정 right를 handled 대상으로 선언하면 그 right에 대한 접근은 rules로 허용된 scope 안에서만 가능해지는 모델을 가져야 한다. 그래서 root directory 전체에 broad allow rule을 깜빡했다고 해서 “rule이 없으니 원래 DAC대로 동작”한다고 생각하면 startup이 전부 실패할 수 있다. 반대로 handled set에서 빠진 right는 Landlock layer가 제한하지 않으므로 partial policy가 생긴다. 이 두 경우는 겉으로 모두 “rule이 없다”지만 결과가 반대다. **Handled 여부와 allow-rule 존재 여부를 두 축으로 디버깅**해야 한다. Policy compiler는 각 handled right에 최소 하나의 intentional allow path가 있는지 정적 검사할 수 있다.

## CHAPTER 21 · 이미 열린 file descriptor는 path-based future open restriction과 다른 authority lifetime을 가진다

Sandbox 적용 전에 sensitive file을 열어 일반 read/write FD를 보유하고 있다면 이후 Landlock가 그 pathname에 대한 새 open을 막더라도 이미 획득한 descriptor authority가 자동으로 사라진다고 기대하면 안 된다. 이것은 capability-style handle 전반의 공통 lifetime 문제다. 따라서 bootstrap 단계에서 privileged configuration을 읽기 위해 연 FD, directory FD, socket, memfd 등을 worker에 남길지 명시적으로 결정해야 한다. Landlock만 설치하고 FD leak을 점검하지 않으면 path namespace는 좁아졌지만 실제 I/O authority는 넓게 남을 수 있다. **Sandbox entry 전에 descriptor inventory와 CLOEXEC/close policy를 닫는 것**이 필수다.

## CHAPTER 22 · rename 이후 pathname이 바뀌어도 rule 판단은 단순 문자열 policy reload 문제가 아니다

Rule anchor와 target은 kernel filesystem object topology에 기반하므로 policy를 “설정 시점 path 문자열의 영구 prefix”로 설명하면 rename 상황에서 오해가 생긴다. Directory가 다른 위치로 이동하거나 bind mount가 추가되면 동일 inode/subtree가 다른 pathname으로 reachable할 수 있다. Security property가 “이 object subtree만 허용”인지 “이 textual path만 허용”인지 먼저 정해야 한다. Landlock만으로 pathname spelling 자체를 immutable하게 만들 수 있다고 가정하면 안 된다. **Mount namespace와 VFS topology mutation을 함께 고려**하고, sandbox 적용 후 누가 rename/mount를 수행할 권한을 가지는지 별도 제한해야 한다.

## CHAPTER 23 · Bind mount와 mount namespace 변화는 동일 object에 새로운 reachability path를 만들 수 있다

Filesystem sandbox는 object graph와 mount topology가 만나므로 privileged 외부 actor가 sandbox가 보는 mount namespace를 바꿀 수 있다면 policy 의미도 영향을 받을 수 있다. 특정 directory object를 rule anchor로 허용했더라도 다른 mount가 그 아래를 덮거나, 같은 filesystem tree가 다른 위치에 bind되어 보일 수 있다. Unprivileged sandbox process 자체가 mount mutation 권한이 없더라도 supervisor나 container runtime의 lifecycle과 race가 존재할 수 있다. 따라서 production evidence에는 Landlock ruleset뿐 아니라 **적용 시점 mount namespace identity와 중요한 mount topology**를 같이 보존하는 것이 좋다.

## CHAPTER 24 · Path resolution race를 줄여도 symlink와 magic-link 정책을 자동으로 모두 해결하는 것은 아니다

Landlock check는 kernel path walk와 결합되지만 application의 모든 pathname-security 문제를 대신 해결하지 않는다. Sandbox 안에서도 허용된 subtree 내부에서 symlink를 따라 예상치 못한 target에 도달하려는 시도가 있을 수 있고, open 시점에 요구하는 resolution invariant는 `openat2()` 같은 API와 함께 설계할 수 있다. 즉 Landlock는 “이 task가 어느 object 범위에 접근 가능한가”를 줄이고, openat2 resolve flags는 “이 pathname traversal이 어떤 형태를 허용하는가”를 더 세밀하게 고정한다. **Authorization boundary와 path-resolution invariant를 중첩**하면 TOCTOU와 escape surface를 더 줄일 수 있다.

## CHAPTER 25 · Policy builder와 sandboxed worker를 분리하면 privileged setup authority를 더 명확히 폐기할 수 있다

복잡한 서비스는 config, certificate, plugin, runtime directory를 모두 연 뒤 worker를 제한해야 할 수 있다. 이때 하나의 process가 평생 policy builder와 worker 역할을 같이 가지면 setup용 FDs와 credentials가 남기 쉽다. Builder가 필요한 O_PATH anchors와 resources를 준비하고, child가 최소 descriptor set만 상속받은 상태에서 Landlock domain에 들어가는 구조는 ownership을 명확히 한다. 단, builder가 IPC broker로 계속 남아 있으면 sandbox가 금지한 파일을 대신 열어 전달하는 confused deputy가 될 수 있다. **Broker API가 Landlock policy보다 넓은 authority를 재노출하지 않는지** 별도로 검증해야 한다.

## CHAPTER 26 · Error code는 unsupported ABI, malformed rule, permission precondition, denied runtime access를 구분하는 상태 증거다

Landlock setup과 runtime failure를 하나의 “sandbox error”로 묶으면 원인을 찾기 어렵다. Ruleset create 단계의 invalid mask, add-rule 단계의 bad FD나 unsupported rule, restrict-self 단계의 precondition failure, enforcement 이후 ordinary `open()`의 `EACCES`는 서로 다른 state transition에서 발생한다. Retry 가능한 것도 다르다. Malformed policy는 재시도해도 낫지 않고, missing feature는 compatibility branch가 필요하며, runtime denial은 정상 security outcome일 수 있다. **Syscall name + errno + ruleset generation + target operation**을 함께 기록해야 자동 recovery가 잘못된 정책을 무한 반복하지 않는다.

## CHAPTER 27 · Audit log가 빈약하다는 이유로 deny 원인을 pathname guess로 재구성하면 증거가 왜곡된다

Landlock denial을 운영에서 분석할 때 항상 풍부한 per-rule audit event가 제공된다고 가정하면 안 된다. Application은 자신이 설치한 ruleset을 deterministic artifact로 남기고, denied operation의 target path, access intent, process identity, mount namespace와 policy generation을 자체 observability에 포함시키는 편이 안전하다. 단, 민감 path와 secret name을 과도하게 log하면 새로운 정보 노출이 된다. 따라서 **security evidence와 confidentiality를 함께 설계**해 hash/ID 기반 policy rule mapping을 사용할 수 있다. “EACCES가 났으니 아마 Landlock” 같은 추정은 DAC·SELinux·AppArmor 실패와 구분되지 않는다.

## CHAPTER 28 · Policy 규모와 layer 수는 correctness뿐 아니라 path-check overhead와 운영 복잡도에 영향을 준다

Landlock는 syscall마다 관련 security check에 참여하므로 rule cardinality와 layer 구조를 무한히 늘려도 공짜라고 가정하면 안 된다. 실제 성능 영향은 workload의 path access pattern, kernel 구현, rule tree 구조에 따라 측정해야 한다. 그러나 optimization을 위해 여러 독립 trust boundary를 하나의 broad allow subtree로 합치면 security precision을 잃는다. 먼저 최소 권한 모델을 만들고, representative workload에서 open/stat/create/rename latency와 CPU cost를 측정한 뒤 병목이 확인될 때만 policy topology를 조정해야 한다. **성능 최적화가 allow surface 확대를 정당화하지 않도록 benchmark와 threat model을 분리**해야 한다.

## CHAPTER 29 · 좋은 test matrix는 allow case보다 escape attempt와 startup-order failure를 더 많이 포함한다

Sandbox test가 “허용된 config를 읽었다” 한 건으로 끝나면 실제 boundary를 거의 검증하지 못한다. Forbidden sibling directory open, symlink traversal, cross-directory rename, file creation 종류, pre-opened FD 사용, fork child, exec child, multithreaded startup, unsupported ABI fallback, mount topology change 같은 negative case를 조합해야 한다. 각 test는 단순 errno뿐 아니라 **어느 security layer가 기대한 denial을 만들었는지** 설명 가능해야 한다. 특히 policy가 너무 약해 operation이 성공하는 false negative가 가장 위험하므로, escape attempt가 성공하면 test harness가 즉시 실패하도록 해야 한다.

## CHAPTER 30 · Landlock 운영 계약은 ABI·ruleset·anchor·domain·descriptor·topology evidence를 함께 보존해야 한다

재현 가능한 Landlock sandbox에는 source code의 allow-list만으로 부족하다. Running kernel에서 확인한 ABI, handled filesystem/network rights, ruleset generation ID, 각 rule의 anchor object와 allowed mask, `no_new_privs` 적용 시점, restrict_self 성공 시점, process/thread lineage, sandbox entry 전후 FD inventory, mount namespace identity, 핵심 mount topology, post-enforcement smoke result를 묶어야 한다. 이 evidence가 있어야 “sandbox enabled=true” 같은 boolean 대신 **어떤 authority가 실제로 제거되었고 어떤 authority가 pre-opened handle이나 broker를 통해 남았는지** 검증할 수 있다. Security boundary의 품질은 설정 파일의 의도가 아니라 실제 state와 failure proof로 판단해야 한다.
