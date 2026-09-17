# PART 99 · Mount Namespace and VFS Topology Mutation — propagation, detached mounts, idmaps, lazy unmount, pivot-root lifetime

Mount는 “device를 directory에 붙이는 명령”을 넘어 process가 보는 pathname graph 자체를 바꾸는 operation이다. Mount namespace가 생기면 같은 inode/filesystem도 namespace마다 다른 topology에 보일 수 있고, shared/slave/private propagation은 한 namespace의 mount·unmount event가 다른 namespace로 흘러가는 방향을 결정한다. 최근 Linux의 `open_tree()`, `move_mount()`, `mount_setattr()` 계열은 mount object를 pathname에서 떼어 file descriptor처럼 조작한 뒤 attach할 수 있게 해 topology mutation의 race boundary를 바꿨다. 여기에 idmapped mount, lazy unmount, `pivot_root()`가 들어가면 정확성은 **어떤 mount object가 어느 namespace graph에 붙어 있는지, 어떤 propagation relation을 갖는지, 누가 reference를 유지하며 detach 이후 언제 실제로 소멸하는지**를 추적하는 문제로 바뀐다.

## CHAPTER 01 · Mount namespace는 file contents가 아니라 mount topology view를 격리한다

Mount namespace를 새로 만들면 process가 보는 mount list가 다른 namespace와 분리될 수 있다. 같은 underlying filesystem과 inode를 공유하더라도 `/proc/self/mountinfo`에 나타나는 attachment graph가 namespace마다 달라질 수 있다는 뜻이다. 따라서 “파일이 같은가”와 “같은 pathname으로 도달 가능한가”는 다른 질문이다. Namespace 생성 시 기존 mount list가 초기 view로 복제되지만 이후 mutation이 완전히 독립적인지는 propagation type에 달려 있다. Container가 host root filesystem의 일부를 bind mount로 보더라도 host와 동일 mount namespace를 쓰는 것은 아니다. **Mount namespace isolation은 storage copy가 아니라 pathname-to-mount graph isolation**이다. Incident에서는 process PID뿐 아니라 그 process가 속한 mount namespace와 mountinfo snapshot을 함께 확보해야 경로 문제를 재현할 수 있다.

## CHAPTER 02 · Mount object와 mount point pathname을 분리하면 rename·bind·detach semantics가 보인다

Filesystem object를 특정 directory entry에 attach한 상태를 mount라고 생각하면 쉽지만 kernel 관점에서는 mount object와 namespace topology 안의 attachment point를 분리해서 보는 편이 정확하다. 하나의 filesystem이 여러 mount objects를 통해 여러 위치에 보일 수 있고 bind mount는 새로운 view를 만든다. Pathname은 현재 topology를 통해 mount object에 도달하는 locator일 뿐 identity 자체가 아니다. `open_tree()`가 detached mount object를 fd로 잡을 수 있다는 사실은 이 구분을 더 직접적으로 보여준다. **Mount identity를 pathname 문자열과 동일시하면 move·pivot·lazy detach 뒤 stale bookkeeping이 생긴다.** Runtime은 mount generation과 parent relation을 기록하고 pathname은 그 generation이 현재 graph 어디에 붙어 있는지 나타내는 derived metadata로 취급하는 편이 안전하다.

## CHAPTER 03 · clone/unshare로 만든 새 mount namespace는 초기 graph를 복제하지만 propagation link까지 단순 복사본은 아니다

`clone(CLONE_NEWNS)`나 `unshare(CLONE_NEWNS)`로 새 mount namespace를 만들면 caller는 기존 mount hierarchy를 기반으로 새 view를 얻는다. 그러나 shared mounts는 peer group 관계와 event propagation semantics 때문에 두 namespace가 이후 완전히 고립된다고 단정할 수 없다. Util-linux `unshare`는 일반적인 isolation 목적에 맞추려고 새 namespace에서 mounts를 recursive private로 바꾸는 동작을 제공하지만, 직접 syscall로 namespace를 만든 application은 원하는 propagation policy를 스스로 설정해야 한다. **Namespace boundary를 만들었다는 사실과 mount-event boundary를 만들었다는 사실은 다르다.** Container runtime은 namespace 생성 직후 root propagation state를 명시적으로 설정해 host와 guest 사이 accidental mount injection이나 unmount propagation을 막아야 한다.

## CHAPTER 04 · Shared mount는 peer group에 속하고 child mount·unmount events를 양방향으로 전파한다

`MS_SHARED` mount는 peer group의 다른 members와 propagation relationship을 갖는다. Shared parent 바로 아래에 새 mount가 생기면 해당 event가 peer mounts 아래에도 복제될 수 있고, peer 쪽에서 발생한 event도 되돌아온다. 이 기능은 removable media나 service-managed mounts를 여러 namespace view에 동기화할 때 유용하지만 container root에 무심코 남으면 guest topology mutation이 host 쪽으로 새어나갈 위험을 만든다. Peer group ID는 `/proc/<pid>/mountinfo`의 `shared:X` optional field로 관찰할 수 있다. **Shared는 “같은 filesystem”이라는 뜻이 아니라 mount-event replication membership**이다. Policy review에서는 device/inode보다 peer-group graph를 봐야 어떤 event가 어디까지 확산되는지 알 수 있다.

## CHAPTER 05 · Private mount는 event propagation을 양방향 차단하는 topology isolation point다

`MS_PRIVATE` mount는 peer group이 없고 mount/unmount events를 밖으로 보내지도, 다른 peer에서 받지도 않는다. Container root를 recursive private로 바꾸는 패턴은 host와 container 사이 topology mutation을 강하게 분리하는 데 쓰인다. 다만 private로 바꾸는 순간 host에서 새로 attach한 storage가 container에 자동으로 들어오기를 기대할 수 없으므로 operational requirement와 trade-off가 있다. Recursive operation을 빠뜨리면 root는 private여도 descendant mount 일부가 shared로 남아 unexpected propagation path를 유지할 수 있다. **Propagation policy는 한 mount point flag가 아니라 subtree graph property**다. 검증에서는 mountinfo 전체 subtree를 탐색해 남은 shared/master tags가 없는지 확인해야 한다.

## CHAPTER 06 · Slave mount는 upstream events를 받지만 downstream mutation을 되돌려 보내지 않는 단방향 relation이다

`MS_SLAVE`는 master shared peer group에서 오는 mount/unmount events를 받아들이지만 slave 아래에서 생긴 events를 master 쪽으로 전파하지 않는다. Host에서 새 storage를 container 안에 보여주고 싶지만 container가 host topology를 바꾸게 하고 싶지 않은 경우 같은 비대칭 요구에 맞는다. Slave가 다시 peer group을 형성해 slave+shared 상태가 될 수도 있어 propagation graph는 단순 tree가 아니라 directed relations까지 포함한다. Mountinfo의 `master:X`, 때로는 `propagate_from:X`가 이 관계를 보여준다. **Slave는 private의 약한 버전이 아니라 명시적인 one-way replication channel**이다. Security model은 어느 방향의 event를 신뢰하는지와 upstream compromise가 downstream topology를 바꿀 수 있음을 함께 적어야 한다.

## CHAPTER 07 · Unbindable mount는 private semantics에 recursive bind 복제 방지까지 추가한다

`MS_UNBINDABLE`은 event propagation 관점에서는 private과 비슷하지만 해당 mount 자체를 bind mount source로 사용할 수 없게 한다. Recursive bind가 큰 subtree를 다른 곳에 복제할 때 unbindable descendants는 target tree에서 prune된다. 이 기능은 상위 directory를 하위로 반복 bind해 mount tree가 폭발적으로 복제되는 “mount explosion” 류 문제를 제한하는 데 유용하다. 하지만 application이 subtree clone에 모든 descendants가 포함된다고 가정하면 unbindable mount 때문에 view가 달라질 수 있다. **Recursive topology copy는 ordinary directory recursion과 다르며 mount propagation attributes가 filtering rule로 작동**한다. Runtime이 rbind 결과를 사용하기 전에 expected mount set을 mountinfo로 검증할 이유가 여기에 있다.

## CHAPTER 08 · Bind mount는 새 storage copy가 아니라 기존 subtree를 다른 attachment point에서 보이게 한다

`MS_BIND`는 source path가 가리키는 object/subtree를 target에 다시 attach해 별도 pathname view를 만든다. Data를 복사하지 않으므로 한쪽에서 file contents를 수정하면 다른 view에서도 underlying object change를 볼 수 있다. Bind mount 자체의 mount attributes와 source filesystem의 properties를 구분해야 하며 read-only bind를 구성할 때도 operation 순서와 modern `mount_setattr()` capabilities를 이해해야 한다. Source pathname이 나중에 rename되어도 이미 생성된 mount object는 pathname 문자열보다 강한 reference를 갖는다. **Bind mount는 aliasing을 mount graph 수준으로 확장**한다. Security review에서는 “container 안 다른 path이니 다른 데이터”라고 가정하지 말고 underlying superblock/inode identity와 mount-specific restrictions를 함께 본다.

## CHAPTER 09 · Recursive bind는 subtree의 nested mounts까지 복제하므로 cardinality와 propagation을 함께 고려해야 한다

`MS_BIND|MS_REC` 또는 rbind는 directory files만 보이는 것이 아니라 source 아래 nested mount topology까지 target 아래에 복제할 수 있다. Nested mount가 많을수록 operation cost와 mount count가 증가하고, shared/slave/unbindable attributes에 따라 resulting graph도 달라진다. Unbindable descendants는 recursive bind에서 자동으로 빠질 수 있고 shared sources의 peer relationships도 결과에 영향을 준다. Container root preparation에서 rbind를 편의 기능으로 쓰면서 mount count limit이나 cleanup plan을 고려하지 않으면 teardown이 복잡해진다. **Recursive bind는 filesystem tree copy가 아니라 mount graph clone operation**이므로 before/after mountinfo diff를 통해 실제 생성된 mounts를 검증하고 cleanup 대상 generation을 기록해야 한다.

## CHAPTER 10 · Propagation type은 child event가 어디로 흐르는지 결정하고 mount 자체 제거는 parent propagation state의 영향을 받는다

Shared/private/slave flags를 “이 mount가 복제되나?”라는 단일 boolean으로 이해하면 세부가 틀어진다. Mount namespace 문서는 propagation type이 바로 아래 child mount/unmount events의 propagation을 결정한다고 설명하며, 특정 mount 자체가 unmount될 때 어떤 peers에 영향이 가는지는 그 mount의 parent relation과 propagation context가 중요하다. Grandchild events도 중간 mount들의 state를 따라 단계적으로 해석해야 한다. **Topology event propagation은 edge-local rule을 graph 전체에 적용한 결과**다. Complex container/storage environment에서는 shared tag 하나만 보고 영향 범위를 추정하지 말고 parent mount IDs와 peer/master IDs를 포함한 graph를 재구성하는 편이 정확하다.

## CHAPTER 11 · /proc/<pid>/mountinfo는 mount graph와 propagation metadata를 동시에 보여주는 핵심 증거다

`/proc/<pid>/mounts`는 사람이 보기 쉬운 mount list를 주지만 mount topology와 propagation 분석에는 `mountinfo`가 더 강하다. 각 record에는 mount ID와 parent mount ID, root, mount point, options, filesystem 정보가 있고 optional fields에 `shared:X`, `master:X`, `propagate_from:X`, `unbindable` 등이 나타난다. Mount IDs와 peer-group IDs는 lifetime이 끝난 뒤 재사용될 수 있으므로 장기 stable global identity로 저장하면 안 된다. Incident snapshot에서는 namespace identity와 timestamp를 함께 저장한다. **Mountinfo는 current graph snapshot이지 append-only history가 아니다.** High-churn runtime에서는 topology mutations 자체를 application telemetry와 연결해야 어떤 generation이 언제 attach/detach됐는지 복원할 수 있다.

## CHAPTER 12 · Less-privileged mount namespace는 user namespace ownership 때문에 propagation과 attribute 변경 권한이 제한된다

각 mount namespace는 owner user namespace를 가지며, 서로 다른 user namespace 사이에서 복제된 mount topology는 privilege escalation을 막기 위한 추가 restrictions를 받는다. Less-privileged namespace를 만들 때 shared mounts가 slave로 낮아지는 behavior처럼 kernel은 unprivileged side의 mutation이 privileged namespace로 역전파되지 않도록 한다. Mount attributes가 user-namespace boundary를 지나면서 locked되어 이후 child namespace가 `nodev`, `nosuid`, `noexec`, read-only 같은 안전 restriction을 임의로 풀지 못하는 경우도 있다. **Mount namespace isolation과 user-namespace privilege model은 결합되어 있다.** Rootless container에서 “namespace 안 uid 0”만 보고 host mount authority와 동일하다고 해석하면 안 된다.

## CHAPTER 13 · Legacy mount(2)는 source/target pathname과 여러 operation modes를 한 syscall interface에 겹쳐 놓았다

전통적인 `mount(2)`는 new filesystem attach, bind, remount, propagation change, move 같은 서로 다른 operations를 flags 조합으로 표현한다. 충분히 강력하지만 pathname resolution과 object creation·attribute mutation·attachment가 한 interface에 겹쳐 있어 complex runtime에서 TOCTOU와 rollback reasoning이 어렵다. Propagation flags는 다른 mount operations와 함께 자유롭게 조합할 수 없는 제약도 있다. Modern fd-based mount API가 나온 배경은 mount object를 detached 상태에서 만들고 검증한 뒤 attach할 수 있게 boundary를 분리하기 위함이다. **Legacy mount는 나쁜 API라기보다 state transition 단계가 덜 분해된 API**다. Runtime은 target kernel compatibility와 필요 atomicity에 따라 둘을 명확히 선택해야 한다.

## CHAPTER 14 · open_tree()는 existing subtree를 mount fd로 잡거나 detached clone을 만들어 pathname race를 줄인다

`open_tree()`는 path가 가리키는 mount를 fd-like handle로 얻을 수 있고 `OPEN_TREE_CLONE`을 쓰면 bind-mount에 해당하는 detached mount object를 만든다. Detached object는 아직 caller의 visible filesystem hierarchy에 attach되지 않았으므로 `move_mount()` 전까지 pathname collision 없이 attributes를 준비할 수 있다. `AT_RECURSIVE`와 함께 subtree clone도 가능하다. 결과 mount fd는 `*at()` operations의 dirfd처럼 사용할 수도 있어 global pathname 재해석을 줄인다. **Mount preparation과 publication을 분리**하면 실패 중간상태가 namespace에 노출되는 시간을 줄일 수 있다. 다만 detached object도 fd reference가 살아 있는 resource이므로 cleanup에서 close를 빼먹으면 mount object lifetime이 예상보다 늘어난다.

## CHAPTER 15 · Detached mount는 attach되기 전 propagation history를 소급해서 받지 않는다

`open_tree(OPEN_TREE_CLONE)`로 만든 detached mount object는 caller의 normal mount namespace graph에 바로 속하지 않고 anonymous mount namespace 쪽에 존재하는 semantics를 가진다. 그래서 detach 상태 동안 original shared tree에서 발생한 propagation events가 clone에 계속 동기화된다고 가정하면 안 된다. 문서가 설명하듯 attach 후 propagation이 다시 적용될 수 있지만 attach 이전 history가 retroactive하게 채워지지는 않는다. Long preparation window 동안 source topology가 바뀌면 detached clone과 live source가 diverge할 수 있다. **Detached mount는 transaction snapshot과 동일하지 않다.** 필요한 consistency가 있다면 source generation을 고정하거나 clone 직후 빠르게 attributes를 설정하고 publication하는 protocol이 필요하다.

## CHAPTER 16 · move_mount()는 detached mount를 destination graph에 publish하는 attach primitive다

`move_mount()`는 existing attached mount를 다른 location으로 이동할 수도 있고 `open_tree()`/`fsmount()`로 얻은 detached mount object를 destination에 attach할 수도 있다. Empty-path flags를 사용하면 source fd 자체를 대상으로 삼아 pathname source lookup을 피할 수 있다. Destination은 dirfd-relative path로 지정할 수 있어 trusted directory capability를 기준으로 placement할 수 있다. Caller는 CAP_SYS_ADMIN 등 필요한 권한과 search permissions를 가져야 한다. **Detached preparation → move_mount publication** 구조는 complex mount tree를 userspace에서 준비한 뒤 한 단계로 visible graph에 넣는 mental model을 제공한다. Publication 실패 시 detached fd를 닫아 rollback할 수 있다는 점도 legacy multi-step mount보다 lifetime reasoning을 단순하게 만든다.

## CHAPTER 17 · move_mount의 shared-parent restrictions는 topology move가 propagation graph를 깨뜨리지 않도록 한다

Mount를 임의로 move할 수 있는 것은 아니다. Source mount의 parent가 shared propagation을 가지는 경우처럼 move가 peer propagation invariants를 깨뜨릴 상황은 EINVAL로 거부될 수 있다. Unbindable children과 destination shared topology의 조합도 제한을 받는다. 따라서 “path A의 mount를 path B로 옮긴다”를 rename과 동일한 namespace-local operation으로 생각하면 안 된다. **Move는 mount object의 parent edge를 바꾸면서 propagation graph에도 참여하는 mutation**이다. Runtime은 operation 전에 source/destination parent의 propagation tags를 점검하고 failure를 topology mismatch로 분류해야 한다. 무조건 retry하면 state가 바뀌지 않는 한 같은 EINVAL을 반복할 뿐이다.

## CHAPTER 18 · mount_setattr()는 mount flags와 propagation을 path 또는 mount fd 기준으로 바꾸는 modern mutation API다

`mount_setattr()`는 `mount_attr`의 `attr_set`, `attr_clr`, `propagation` 등을 통해 read-only, nosuid, nodev, noexec, nosymfollow, atime policy 같은 mount-specific attributes와 propagation type을 조정할 수 있다. `AT_RECURSIVE`를 사용하면 subtree 전체에 적용할 수 있어 여러 legacy remount calls를 줄일 수 있다. Kernel은 clear 후 set 순서로 attributes를 계산하므로 동시에 어떤 제한을 제거하고 추가하는지 명시적으로 읽을 수 있다. **Attribute mutation은 file mode 변경과 다르게 mount view에 적용**된다. Same filesystem의 다른 mount object가 다른 `noexec`/readonly policy를 가질 수 있으므로 security policy는 superblock이 아니라 mount generation 단위로 검증해야 한다.

## CHAPTER 19 · mount restrictions가 locked되면 child namespace가 안전 flag를 되돌리는 것을 kernel이 거부한다

User-namespace boundary를 가로질러 mount가 복제되거나 new user+mount namespace pair가 만들어질 때 kernel은 readonly, nosuid, nodev, noexec 같은 security-sensitive properties를 locked state로 만들 수 있다. 그 뒤 less-privileged side가 `mount_setattr()`이나 remount로 restriction을 제거하려 하면 EPERM이 날 수 있다. 이것은 rootless environment에서 container-root가 host가 강제한 mount restrictions를 풀지 못하게 하는 defense다. **Observed EPERM을 단순 capability 누락으로만 분류하지 말고 attribute-lock provenance를 확인**해야 한다. Runtime diagnostics는 owner user namespace와 current mountinfo, requested attr changes를 함께 기록해야 정책 원인을 설명할 수 있다.

## CHAPTER 20 · Idmapped mount는 filesystem inode ownership을 rewrite하지 않고 한 mount view에서 UID/GID 해석을 변환한다

Idmapped mount는 on-disk inode uid/gid를 대량 `chown`하지 않고 특정 mount에서 credentials와 filesystem IDs 사이 mapping을 다르게 해석할 수 있게 한다. User namespace fd가 mapping definition의 anchor로 사용되며 supported filesystem과 kernel restrictions가 있다. 이 기능은 container/user namespace와 host filesystem ownership을 효율적으로 맞추는 데 유용하지만 “모든 filesystem에서 임의로 된다”는 generic guarantee가 아니다. **ID mapping은 data mutation이 아니라 mount-view credential translation**이다. Backup, security audit, inode metadata를 읽는 host tool과 container 내부 view가 다른 uid 숫자를 볼 수 있으므로 logs에는 raw inode identity와 mapping context를 함께 기록해야 한다.

## CHAPTER 21 · Existing attached mount의 idmap을 임의로 바꾸는 대신 detached mount에서 mapping을 설정하는 제약을 이해해야 한다

`mount_setattr()`의 idmapped-mount 규칙은 ordinary flags보다 엄격하다. Mount가 이미 idmapped인지, writers가 존재하는지, detached 상태인지, filesystem이 mapping을 지원하는지 등에 따라 operation이 거부된다. 문서상 idmap 설정은 detached `open_tree(OPEN_TREE_CLONE)` object에서 수행하는 workflow가 핵심이다. 이미 production graph에 attach된 mount의 identity translation을 순간적으로 바꾸면 running processes의 permission semantics가 뒤집힐 수 있기 때문에 제한이 강한 것이 자연스럽다. **Idmap은 publication 전 immutable-ish configuration으로 취급하는 편이 안전**하다. Runtime은 clone→idmap→other restrictions→attach 순서를 명시하고 실패 시 detached object를 폐기해야 한다.

## CHAPTER 22 · open_tree_attr()는 clone과 attribute application을 더 가깝게 결합하지만 kernel version contract를 확인해야 한다

최근 Linux에는 `open_tree_attr()`처럼 tree clone/open과 mount attributes 적용을 결합한 interface가 추가되어 detached mount에 idmap을 포함한 attributes를 더 직접적으로 설정할 수 있다. 이 interface는 전통적인 `open_tree()`보다 새 kernel에서 도입됐고 libc wrapper availability도 다를 수 있어 runtime compatibility probe가 필요하다. 최신 기능을 compile-time headers만 보고 사용하면 older production kernel에서 ENOSYS를 만날 수 있다. **Mount API capability는 source code version이 아니라 running kernel feature set으로 결정**된다. Library는 new path와 fallback path를 분리하고 semantics가 동일하지 않은 부분은 feature flag와 tests로 관리해야 한다.

## CHAPTER 23 · Lazy unmount는 pathname graph에서 즉시 detach하지만 active references가 사라질 때까지 object lifetime을 연장한다

`umount2(..., MNT_DETACH)`는 mount와 그 submounts를 현재 mount table/pathname access에서 떼어 새 lookup이 들어오지 못하게 하지만, 이미 open fd·cwd·mapped object 같은 active references가 있으면 실제 filesystem teardown은 나중까지 지연된다. 따라서 `umount -l` 성공을 “모든 users가 끊겼다” 또는 “device를 안전하게 제거해도 된다”와 동일시하면 안 된다. **Topology visibility와 resource lifetime은 별도 milestone**이다. Incident에서 detached mount의 storage I/O가 계속되는 것처럼 보이면 old references를 가진 processes를 찾아야 한다. Graceful teardown은 new access 차단, holder drain, final release를 단계적으로 관측해야 한다. Detach success 뒤 device shutdown이나 credential revocation 같은 후속 operation을 실행한다면 reference-drain confirmation을 별도 barrier로 두어야 한다.

## CHAPTER 24 · Normal unmount의 EBUSY는 mount point 아래 pathname이 아니라 살아 있는 references를 찾으라는 신호다

Unmount가 EBUSY로 실패하는 이유는 current working directory, open files, submounts, mappings 등 mount를 busy하게 만드는 references가 남아 있기 때문일 수 있다. 단순히 directory listing이 비어 있는지 확인하는 것으로는 해결되지 않는다. Process cwd/root, fd tables, namespace별 mount topology를 조사해야 holder를 찾을 수 있다. FUSE/NFS처럼 filesystem-specific activity도 고려한다. MNT_FORCE는 일부 filesystems에만 의미가 있고 data-loss 위험까지 있으므로 generic “강제 해결 버튼”으로 쓰면 안 된다. **EBUSY는 cleanup protocol이 아직 끝나지 않았다는 evidence**다. Runtime은 timeout 뒤 바로 lazy detach할지, holder를 terminate할지, operation을 실패시킬지 workload durability 요구에 맞춰 정책화해야 한다.

## CHAPTER 25 · Shared parent 아래 unmount는 다른 peers로 propagation될 수 있어 teardown blast radius를 확인해야 한다

Shared mount propagation은 attach뿐 아니라 unmount events에도 적용될 수 있다. Namespace 하나에서 cleanup한다고 생각한 unmount가 peer group이나 slave chain을 통해 다른 namespace view에 영향을 줄 가능성이 있으므로 destructive topology operation 전에 parent propagation state를 확인해야 한다. `mount_namespaces(7)`가 unmount behavior를 parent shared state와 함께 설명하는 이유다. **Unmount target만 보고 영향 범위를 결정할 수 없다.** Container runtime이 host-shared path 아래 임시 mount를 만들고 지우는 경우 예상치 못한 propagation을 막기 위해 namespace root를 private/slave로 정규화하거나 isolated subtree를 사용해야 한다. Fault test에서는 실제 peer namespaces를 만들어 propagation 방향을 검증해야 한다.

## CHAPTER 26 · pivot_root()는 pathname prefix 교체가 아니라 process namespace의 root mount topology를 재배치한다

`pivot_root(new_root, put_old)`는 caller mount namespace의 root filesystem을 `new_root`로 바꾸고 기존 root를 `put_old` 아래로 이동시키는 primitive다. `chroot()`처럼 path resolution root만 바꾸는 것과 달리 mount graph 자체를 재배치한다. Container/initramfs 전환에서 old root를 더 이상 reachable하지 않게 만들려면 pivot 후 cwd를 새 `/`로 바꾸고 old root를 unmount하는 cleanup이 뒤따른다. Preconditions도 존재해 new root가 mount point여야 하는 등 topology 준비가 필요하다. **Root switch는 mount graph transaction**이며, old root references가 남으면 격리/teardown 기대가 깨질 수 있다. Child processes가 pivot 전 cwd/fd를 들고 있지 않은지도 검증해야 한다.

## CHAPTER 27 · pivot_root('.', '.') 패턴은 mount stack을 이용해 temporary put_old directory 없이 old root를 detach할 수 있다

Linux 문서에는 new root로 chdir한 뒤 `pivot_root(".", ".")`를 하고 `umount2(".", MNT_DETACH)`를 사용하는 pattern이 설명되어 있다. Pivot이 old root를 new root 위 mount stack에 올리는 semantics를 이용해 다음 unmount resolution에서 old layer를 제거한다. Compact하지만 이 pattern을 magic incantation으로 외우면 current directory와 mount stack ordering을 놓치기 쉽다. **`.`의 의미는 syscall 사이에 topology가 바뀌면서 달라진다.** Runtime implementation은 각 단계 후 root/cwd identity를 검증하고, error path에서 어느 graph state까지 변했는지 기록해야 한다. Root transition 실패는 ordinary file-open 실패보다 복구가 어렵기 때문에 disposable child namespace에서 준비하는 편이 안전하다.

## CHAPTER 28 · chroot()는 mount namespace isolation이나 open-reference revocation을 제공하지 않는다

`chroot()`는 process의 pathname resolution root를 바꾸는 기능이지 mount topology 자체를 독립 namespace로 만드는 기능이 아니다. Open fd, cwd, other processes, mount propagation을 자동 정리하지 않으므로 container filesystem isolation primitive 하나로 사용하면 escape/visibility assumptions가 약하다. `pivot_root()`도 단독 보안 sandbox는 아니며 user/mount namespace, fd cleanup, capabilities 등과 결합해야 한다. **Path root 제한과 mount graph isolation을 구분**하면 왜 modern container runtime이 여러 primitives를 조합하는지 이해할 수 있다. Security review는 “root path가 바뀌었는가”가 아니라 old namespace objects로 이어지는 references가 모두 의도대로 끊겼는가를 검사해야 한다.

## CHAPTER 29 · O_PATH·dirfd·mount fd를 이용한 topology mutation은 global pathname 재해석을 줄여 race surface를 낮춘다

Modern mount APIs는 `open_tree()` mount fd와 destination dirfd를 이용해 source/destination을 object capability에 가깝게 고정할 수 있다. 이는 privileged runtime이 untrusted process가 rename/symlink로 바꿀 수 있는 global pathname을 여러 syscall에 걸쳐 반복 해석하는 위험을 줄인다. `move_mount()`의 empty-path flags는 이미 열린 object 자체를 대상으로 삼을 수 있다. **Path-based authorization을 한 번 통과한 뒤 stable fd reference로 operation을 이어가는 설계**는 P71의 openat/openat2 원칙과 같다. 그래도 fd가 올바른 namespace generation을 가리키는지와 CLOEXEC cleanup은 별도 문제이므로 capability lifetime audit가 필요하다. 권한 확인과 object acquisition을 같은 trusted dirfd lineage에 묶으면 pathname namespace가 바뀌는 동안의 ambiguity도 줄일 수 있다.

## CHAPTER 30 · Mount fd는 namespace attachment와 독립적으로 object lifetime을 연장할 수 있으므로 close discipline이 필요하다

Detached mount fd는 아직 pathname graph에 보이지 않더라도 kernel mount object를 살아 있게 하는 reference다. Attached mount를 가리키는 fd도 topology move 이후 stable handle로 남아 object 접근에 영향을 줄 수 있다. Runtime이 error path에서 mount fd를 leak하면 visible mount count와 별개로 resources가 예상보다 오래 남을 수 있다. 반대로 fd를 닫아도 이미 graph에 attach된 mount가 자동 unmount되는 것은 아니다. **Reference lifetime과 graph membership은 양방향으로 독립적**이다. Mount manager는 `PREPARED(detached) → CONFIGURED → ATTACHED → DETACHED → RELEASED` 같은 state machine을 두고 fd와 namespace edge를 각각 정리해야 한다. Namespace teardown 전에는 mount-fd registry를 먼저 닫고, 그 뒤 graph detach를 수행하는 식으로 ownership order를 고정하면 leak 원인을 좁히기 쉽다.

## CHAPTER 31 · Overlay/container root 구성은 mount topology와 propagation policy를 함께 고정해야 한다

Container rootfs는 overlayfs, bind mounts, proc/sys/dev mounts, secret/config mounts 등 여러 mount objects를 조합해 구성될 수 있다. Overlay copy-up/whiteout semantics만 맞아도 root parent가 shared로 남아 있으면 host/container 사이 unexpected mount propagation이 생길 수 있다. 반대로 지나치게 private로 만들면 host에서 제공하려는 dynamic mounts가 들어오지 않는다. OCI runtime 설정의 mount list와 실제 kernel mountinfo가 일치하는지 검증해야 한다. **Container filesystem correctness는 image layers뿐 아니라 runtime mount graph의 generation**으로 정의해야 한다. Startup 후 topology hash/snapshot을 남기면 incident에서 unexpected injection이나 missing bind를 빠르게 비교할 수 있다.

## CHAPTER 32 · Mount count와 namespace proliferation은 kernel memory·IDs·cleanup work를 소비하는 resource budget이다

Mount objects, peer group IDs, namespaces는 무료 metadata가 아니다. Recursive bind와 short-lived container churn이 많으면 mount count와 cleanup work가 커지고, bug로 teardown이 누락되면 mount namespace references가 누적될 수 있다. Mountinfo lines가 급증하거나 namespace file descriptors가 오래 살아 있으면 process가 종료된 뒤에도 namespace lifetime이 연장될 수 있다. **Mount topology도 cardinality budget을 가져야 한다.** Per-workload expected mounts, namespace count, longest detached lifetime, lazy-unmount pending age를 metrics로 두고 abnormal growth를 감지해야 한다. 단순 disk usage monitoring으로는 이 resource leak을 볼 수 없다.

## CHAPTER 33 · Mount observability는 pathname list가 아니라 namespace별 parent/peer/master graph와 generation을 기록해야 한다

`findmnt`, `/proc/<pid>/mountinfo`, namespace symlinks는 현재 상태를 조사하는 출발점이다. 필요한 정보는 mount point 문자열뿐 아니라 mount ID, parent ID, filesystem type, source/root, read-only/noexec/nosuid/nodev flags, shared/master tags다. Container incident에서는 host PID와 container PID가 다른 mount namespace에 있으므로 양쪽 mountinfo를 함께 비교한다. IDs는 재사용될 수 있어 장기 correlation에는 boot/session generation과 timestamp가 필요하다. **Topology debugging은 tree diff 문제**다. 정상 baseline graph와 incident graph를 비교하면 unexpected propagation, stale lazy-detach, wrong bind source를 application log보다 직접적으로 찾을 수 있다.

## CHAPTER 34 · Mount fault injection은 propagation, busy references, detached publication, idmap failure를 서로 다른 실패로 검증해야 한다

테스트는 tmpfs 하나 mount/unmount하는 happy path로 부족하다. 두 mount namespaces에 shared/slave/private combinations를 만들어 child mount와 unmount가 어느 방향으로 전파되는지 확인한다. Open fd와 cwd를 남겨 EBUSY를 만들고 MNT_DETACH 후 old reference가 계속 동작하는지도 본다. Detached `open_tree` object에 attributes를 적용한 뒤 `move_mount` 실패를 주입해 visible partial state가 없는지 검사한다. Idmap은 unsupported filesystem, writer 존재, invalid userns fd 같은 failure를 구분한다. **각 topology failure class에서 mount graph와 leaked fds/namespaces가 baseline으로 수렴하는지 실제로 확인**해야 CLEAN이라 부를 수 있다. Test 종료 시 각 namespace의 mountinfo를 baseline과 diff하고 namespace fd count까지 확인해야 invisible detached objects를 놓치지 않는다.

## CHAPTER 35 · Mount API 선택은 compatibility보다 먼저 publication atomicity와 trust boundary를 기준으로 정한다

Simple host administration에서는 mature `mount(2)`/mount(8) workflow가 충분할 수 있다. Privileged container runtime처럼 untrusted pathname races를 줄이고 complex subtree를 visibility 전에 준비해야 하면 fd-based `open_tree`/`mount_setattr`/`move_mount`가 더 강한 model을 제공한다. Idmapped mount나 newer attributes가 필요하면 running kernel feature availability가 선택을 결정한다. **새 API를 쓴다는 이유만으로 더 안전한 것이 아니라 object handles, error cleanup, namespace ownership을 올바르게 관리할 때 장점이 생긴다.** Fallback legacy path가 있다면 두 paths가 동일 security restrictions와 propagation policy를 실제로 만족하는지 별도로 테스트해야 한다.

## CHAPTER 36 · Mount-topology correctness는 namespace graph, propagation edges, references가 모두 닫히는 proof다

안전한 mount subsystem은 다음을 답할 수 있어야 한다. Process가 어느 mount namespace view를 사용하는가. Shared/slave/private/unbindable edges가 의도한 방향인가. Recursive bind가 어떤 descendants를 복제·prune하는가. Detached mount가 live source와 diverge할 수 있음을 고려하는가. Attribute locks와 user-namespace 권한을 존중하는가. Idmap은 publication 전 올바른 userns mapping으로 고정되는가. Lazy unmount 뒤 stale references가 bounded하게 drain되는가. Pivot 뒤 old root에 도달하는 fd/cwd가 남지 않는가. Fault injection 후 mounts, namespace references, detached fds가 baseline으로 돌아오는가. **Mount는 pathname 명령이 아니라 VFS topology generation을 생성·연결·전파·분리·폐기하는 graph transaction**이다.
