# PART 105 · Extended Attributes and POSIX ACL State — xattr namespaces, ACL masks, inheritance, mutation races, metadata durability

Extended attribute와 POSIX ACL을 “파일에 붙은 추가 메모” 정도로 보면 permission 계산과 metadata lifetime을 놓친다. Xattr은 inode에 결합된 name/value metadata이고 namespace마다 누가 읽고 쓸 수 있는지가 다르다. POSIX ACL은 owner/group/other mode bit보다 세밀한 named user/group 권한을 표현하지만, `ACL_MASK`가 effective permission을 다시 제한한다. Pathname 기반 API는 rename·symlink race를 만나고, xattr 한 개의 update와 여러 metadata의 transaction은 다르며, syscall 성공과 crash durability도 같은 보장이 아니다. Backup·copy·hard link·exec credential transition까지 포함해 inode metadata를 하나의 state domain으로 봐야 한다.

## CHAPTER 01 · Extended attribute는 file contents가 아니라 inode에 결합된 별도 metadata key/value state다

Xattr은 일반 file byte stream 뒤에 이어 붙는 숨은 문자열이 아니다. Filesystem은 inode와 연결된 metadata namespace에 name과 value를 저장하고, `getxattr()`·`setxattr()` 계열 API로 이를 다룬다. 같은 pathname이 hard link 여러 개를 통해 보이더라도 동일 inode를 가리키면 xattr state도 공유된다. 반대로 file contents를 복사해 새 inode를 만들었다고 해서 xattr이 자동으로 동일해지는 것은 tool과 filesystem semantics에 달려 있다. 따라서 “파일 데이터가 같다”는 hash 증거만으로 security metadata까지 동일하다고 말할 수 없다. 운영 검증에서는 **content identity와 inode metadata identity를 분리**하고 ACL, capabilities, labels 같은 xattr-backed state를 별도로 확인해야 한다.

## CHAPTER 02 · Xattr 이름의 namespace prefix는 단순 naming convention이 아니라 access-control boundary다

Linux xattr 이름은 `user.`, `trusted.`, `security.`, `system.`처럼 namespace prefix를 가진다. 이 prefix는 사람이 보기 좋은 분류가 아니라 kernel과 filesystem이 permission·semantics를 다르게 적용하는 경계다. Application이 arbitrary metadata를 `security.*`에 넣는다고 보안 속성이 생기는 것도 아니고, ordinary user metadata를 privileged namespace에 쓰려 하면 거절될 수 있다. Backup 도구가 prefix를 무시하고 value만 복사하면 destination에서 의미가 달라질 수 있다. **Name string 전체가 xattr type의 일부**라고 생각해야 하며, namespace별 owner, writer, reader, consumer를 문서화해야 한다.

## CHAPTER 03 · Xattr value 크기와 전체 metadata 공간에는 filesystem별 한계가 있어 arbitrary blob storage로 쓰면 안 된다

Xattr API는 byte array를 저장할 수 있지만 무제한 object store가 아니다. 개별 value size, name length, inode에 붙일 수 있는 전체 xattr 공간은 filesystem 구현과 block layout에 따라 제한된다. 작은 token 몇 개는 잘 저장되다가 production에서 certificate chain이나 large JSON을 넣는 순간 `E2BIG`·`ENOSPC` 계열 실패가 발생할 수 있다. 이 실패를 disk free space만 보고 설명할 수 없는 경우도 있다. Xattr에는 **작고 구조가 명확한 metadata**를 두고 large payload는 별도 storage object로 분리하는 것이 안정적이다. Capacity test는 평균 value가 아니라 maximum expected count와 size 조합으로 해야 한다.

## CHAPTER 04 · getxattr()의 size-probe와 실제 read 사이에는 value change race가 존재한다

많은 caller는 먼저 size 0 buffer로 필요한 길이를 확인한 뒤 그 크기만큼 buffer를 할당하고 다시 `getxattr()`을 호출한다. 그러나 두 호출 사이에 다른 thread나 process가 value를 더 큰 값으로 교체할 수 있다. 첫 번째 length가 이후 read의 고정 snapshot을 보장하지 않기 때문에 두 번째 호출이 `ERANGE`로 실패하거나 재시도가 필요할 수 있다. 반대로 value가 작아지거나 삭제되면 `ENODATA`류 결과가 나올 수 있다. Robust code는 **probe→allocate→read를 lock-free optimistic sequence**로 보고 bounded retry를 구현해야 한다. Length를 신뢰해 unchecked copy를 하면 memory-safety bug로 이어질 수 있다.

## CHAPTER 05 · listxattr()도 이름 목록의 point-in-time snapshot을 보장하는 transaction이 아니다

Xattr names를 먼저 길이 query한 뒤 list buffer를 읽는 동안 attribute가 추가·삭제될 수 있다. 그래서 list 결과를 authoritative inventory로 사용해 “없는 attribute를 삭제했다”거나 “정책 metadata가 완전하다”고 단정하면 race를 만난다. Security scanner가 여러 xattr을 읽어 policy state를 재구성할 때는 list 시점과 개별 get 시점이 다를 수 있다. 필요한 attribute names를 이미 알고 있다면 직접 get하고 expected absence를 명시적으로 처리하는 편이 더 강한 contract가 될 수 있다. **Metadata enumeration과 metadata snapshot은 동일하지 않다**는 점이 중요하다. Required capability가 없는 환경에서 service를 계속 띄워야 한다면 해당 기능을 비활성화했다는 degraded-state metric을 노출하고, security-sensitive workflow는 별도 gate로 차단해야 한다.

## CHAPTER 06 · XATTR_CREATE와 XATTR_REPLACE는 application-level compare-and-intent를 kernel operation에 일부 반영한다

`setxattr()` flags는 attribute가 없어야 성공하는 create-only와 이미 있어야 성공하는 replace-only semantics를 제공할 수 있다. 이 기능을 쓰지 않고 `getxattr() → 판단 → setxattr()` 순서로 존재 여부를 확인하면 두 syscall 사이에 경쟁자가 state를 바꾸는 TOCTOU가 생긴다. Create-only는 duplicate initializer를 잡고, replace-only는 accidental first-create를 막는 데 유용하다. 다만 둘 다 value generation까지 비교하는 compare-and-swap은 아니다. **Existence precondition과 content-version precondition을 구분**하고, 강한 optimistic concurrency가 필요하면 별도 generation metadata나 external lock을 설계해야 한다.

## CHAPTER 07 · setxattr() 한 번의 replacement와 여러 xattr·mode·owner 변경을 묶은 transaction은 전혀 다르다

하나의 xattr value를 교체하는 syscall이 성공했다고 해서 ACL, file mode, owner, security label을 함께 원자적으로 전환했다는 뜻은 아니다. Application이 여러 metadata를 순차 수정하는 동안 observer는 중간 상태를 볼 수 있고, process crash가 나면 일부만 적용된 상태가 남을 수 있다. 예를 들어 새 ACL을 쓰고 security marker를 나중에 쓰는 설계는 두 operation 사이에 authorization window를 만든다. Metadata migration은 **순서 invariant, idempotent recovery, partial-commit detection**을 가져야 한다. 필요하면 새 inode를 준비한 뒤 rename으로 publish하는 방식이 더 강한 atomicity boundary를 줄 수 있다.

## CHAPTER 08 · Path 기반 xattr syscall은 pathname resolution race를 가지므로 stable FD 기반 API와 구분해야 한다

`setxattr(path, ...)`는 호출 시점에 pathname을 다시 resolve한다. 다른 process가 parent directory를 rename하거나 symlink target을 바꾸면 caller가 의도한 object와 다른 inode에 metadata를 쓸 위험이 있다. 권한이 중요한 code에서는 이미 검증해 연 file descriptor를 사용한 `fsetxattr()` 계열이 object identity를 더 안정적으로 고정한다. 이때도 FD가 어떤 inode를 가리키는지 open 시점 evidence가 필요하다. **Path text와 object handle을 분리**하면 privileged metadata updater의 TOCTOU surface를 줄일 수 있다. 같은 directory 안에서도 read-file과 execute만 허용한 worker와 create/remove까지 허용한 maintenance worker는 전혀 다른 blast radius를 가지므로 역할별 policy profile을 분리하는 편이 좋다.

## CHAPTER 09 · setxattr()와 lsetxattr()의 차이는 symlink 자체와 symlink target 중 어느 inode를 수정하는지 결정한다

Symlink path에 xattr을 설정할 때 follow 여부를 명확히 하지 않으면 전혀 다른 security object를 바꿀 수 있다. 일반 path API가 symbolic link를 따라 target에 적용되는 경우와 `l*` 계열이 link object 자체를 대상으로 하는 경우를 구분해야 한다. Attack-controlled directory에서 privileged helper가 path를 받아 label이나 ACL을 쓰는 구조라면 이 차이는 직접적인 confused-deputy 문제가 된다. **“이 문자열에 metadata를 쓴다”가 아니라 “어느 resolved inode에 metadata를 쓴다”**를 contract로 적어야 한다. Symlink restrictions와 filesystem support도 함께 확인해야 한다.

## CHAPTER 10 · security.* namespace는 LSM과 kernel security consumer가 의미를 부여하므로 application-private storage로 취급하면 안 된다

`security.*` xattr은 SELinux label, file capability, integrity metadata 등 kernel security subsystem이 해석하는 값에 사용될 수 있다. Value format은 해당 consumer의 ABI와 정책에 의해 결정되며 arbitrary application string을 넣는 용도가 아니다. 잘못된 value를 쓰면 syscall이 거절되거나 file이 예상과 다른 security context로 동작할 수 있다. Backup/restore에서도 raw bytes를 복원하는 것만으로 destination policy가 같은 의미를 가진다고 보장할 수 없다. **Security xattr은 data format과 enforcement engine을 함께 versioning**해야 한다.

## CHAPTER 11 · security.capability xattr은 exec credential transition과 연결되므로 일반 metadata보다 위험도가 높다

File capabilities는 executable inode에 capability metadata를 붙여 exec 시 process credential 계산에 영향을 줄 수 있다. 따라서 해당 xattr을 보존·변경할 권한은 단순한 “파일 태그 편집”이 아니라 privilege boundary에 가깝다. Copy tool이 source의 `security.capability`를 destination에 무조건 유지하면 예상하지 못한 executable authority가 전파될 수 있고, 반대로 누락되면 service startup이 실패할 수 있다. Deployment pipeline은 file owner, mode, capability xattr을 하나의 artifact security manifest로 검증해야 한다. **Content hash만 같은 binary와 executable authority가 같은 binary는 동의어가 아니다.**

## CHAPTER 12 · trusted.* namespace는 privileged administration metadata와 ordinary user metadata를 분리하는 데 쓰인다

`trusted.*` 계열은 일반 unprivileged user가 자유롭게 읽고 쓰는 `user.*`와 다른 permission model을 가진다. Filesystem이나 system component가 내부 coordination marker를 여기에 둘 수 있으므로 application이 root 권한으로 arbitrary keys를 남발하면 다른 tooling과 충돌할 수 있다. Namespace access가 capability context에 의존하면 container 내부 root와 host root를 동일하게 보면 안 된다. **Privileged xattr은 누가 쓸 수 있는지뿐 아니라 누가 의미를 소비하는지**까지 확인해야 한다. 운영 스크립트가 privileged namespace를 cleanup할 때 unknown attribute를 무조건 삭제하는 방식은 위험하다.

## CHAPTER 13 · user.* xattr도 file type·permission·mount policy의 영향을 받으므로 모든 inode에 자유롭게 저장되는 것은 아니다

`user.*` namespace는 application metadata에 적합하지만 caller가 write permission을 가졌다는 이유만으로 모든 filesystem object에 동일하게 사용할 수 있다고 가정하면 안 된다. Filesystem별 지원 여부, object type, owner/permission, mount configuration이 결과에 영향을 줄 수 있다. Portable application은 xattr을 primary truth로 사용할 때 unsupported filesystem에서의 fallback을 정의해야 한다. **Xattr availability는 deployment precondition**으로 검사하고, 미지원 환경에서 silently metadata를 버리면 security나 correctness state가 유실될 수 있다.

## CHAPTER 14 · POSIX ACL은 inode access metadata이며 단순히 mode bit를 더 길게 쓴 표현이 아니다

POSIX access ACL은 owner, named users, owning group, named groups, mask, other 같은 entries를 이용해 mode bit보다 세밀한 주체별 권한을 표현한다. Named user나 group entry가 존재하면 permission decision은 `rwx` 세 글자만 봐서는 복원되지 않는다. `ls -l`의 group class bits도 ACL mask와 연결되어 보일 수 있기 때문에 mode만 수집하는 inventory는 effective access를 놓친다. Security audit은 **ACL entries와 mode bits를 함께 읽고 effective permission을 계산**해야 한다. “chmod 640이니까 이 사용자 접근 불가” 같은 결론은 extended ACL이 있으면 틀릴 수 있다.

## CHAPTER 15 · ACL_MASK는 named user와 group class의 effective permission을 상한선으로 자른다

ACL에서 가장 자주 오해되는 entry가 mask다. Named user entry에 `rwx`가 적혀 있어도 mask가 `r-x`라면 effective permission은 write가 제거된 `r-x`가 된다. Owning group과 named groups도 group class mask의 영향을 받는다. 반면 owner entry와 other entry는 같은 방식으로 mask되지 않는다. Tool output이 raw permission과 effective permission을 함께 보여주는 이유다. **Stored ACL entry와 실제 authorization 결과를 구분**하지 않으면 권한을 과대평가하게 된다. Policy generator는 named entry를 추가·삭제할 때 mask가 어떻게 계산되는지도 명시해야 한다.

## CHAPTER 16 · Named user ACL entry는 UID identity에 직접 결합되므로 계정 재사용과 namespace mapping이 장기 위험이 된다

특정 사용자에게 별도 권한을 주기 위해 named user ACL을 사용할 수 있지만 저장되는 identity는 이름 문자열 자체가 아니라 numeric identity semantics와 연결된다. 계정을 삭제한 뒤 UID가 재사용되거나, backup을 다른 identity namespace로 restore하면 같은 number가 다른 principal을 가리킬 수 있다. 사용자 이름을 display한 audit만 남기면 이 drift를 놓칠 수 있다. **ACL backup에는 numeric ID와 intended principal mapping evidence**가 필요하고, cross-host restore에서는 identity remapping 정책을 먼저 결정해야 한다. 장기 보관 ACL은 계정 lifecycle database와 주기적으로 대조해 orphan numeric ID를 찾아야 하며, orphan을 발견했다고 즉시 다른 사용자에게 재매핑해서는 안 된다.

## CHAPTER 17 · Named group entries가 여러 개 match되면 group class 계산과 mask를 함께 적용해야 한다

Process credentials가 supplementary groups를 여러 개 가질 수 있으므로 ACL access check에서 하나 이상의 named/owning group entry가 match할 수 있다. 구현과 표준 semantics에 따라 해당 group class permissions를 결합한 뒤 mask의 상한을 적용하는 흐름을 이해해야 한다. “첫 번째 matching group만 본다” 같은 hand-written authorization logic은 kernel 결과와 달라질 수 있다. Application이 kernel ACL을 별도 policy engine으로 복제하지 말고 실제 access syscall을 authoritative check로 사용하는 이유다. **Credential group set 전체가 authorization input**이며 single GID 비교로 축약하면 안 된다.

## CHAPTER 18 · Default ACL은 directory에 붙은 template이고 그 directory 자체의 access ACL과 역할이 다르다

Directory의 default ACL은 현재 directory를 접근할 때 직접 사용하는 permission table이 아니라 그 아래 새 object가 생성될 때 access ACL을 유도하는 inheritance template이다. 그래서 default ACL을 바꿨다고 기존 child files의 ACL이 자동으로 모두 변경된다고 생각하면 안 된다. 반대로 directory access ACL을 바꿔도 future child inheritance template은 별도로 남을 수 있다. 운영 도구는 **directory access ACL과 default ACL을 별도 state**로 보여줘야 한다. Migration 시 둘 중 하나만 복사하면 현재 접근과 미래 생성물의 권한이 서로 다른 방향으로 drift할 수 있다. 새 child가 예상한 ACL로 만들어지는지는 실제 create 후 `acl` state를 읽어 검증해야 하며, parent template 변경과 기존 child migration은 별도 deployment 단계로 다뤄야 한다.

## CHAPTER 19 · 새 file 생성 시 default ACL과 mode argument가 결합되므로 umask만으로 최종 permission을 설명할 수 없다

프로그램이 `open(..., mode)`이나 `mkdir(..., mode)`로 object를 만들 때 parent directory에 default ACL이 있으면 최종 access ACL 계산은 단순한 `mode & ~umask` 모델과 달라질 수 있다. Default ACL entries가 상속되고 요청 mode가 허용하는 범위와 결합되어 effective bits가 정해진다. 그래서 같은 binary와 같은 umask라도 parent directory policy에 따라 새 file permission이 달라질 수 있다. Incident 분석에서는 process umask만 수집하지 말고 **parent default ACL과 requested creation mode**를 함께 확인해야 한다. 특히 shared directory에서 default ACL이 협업 권한을 만들고 umask는 application 작성자가 통제하는 경우, 두 policy owner가 서로의 효과를 모르면 신규 파일 접근권한이 지속적으로 흔들릴 수 있다.

## CHAPTER 20 · chmod는 ACL이 존재하는 inode에서 group class와 mask 의미를 바꿀 수 있어 별도 channel이 아니다

Extended ACL이 설정된 file에 `chmod()`를 수행할 때 mode bits와 ACL representation은 서로 독립적으로 남아 있지 않는다. Group-class mode 변화가 ACL mask와 연동될 수 있어, application이 ACL을 설정한 뒤 다른 maintenance script가 chmod를 실행하면 named entries의 effective permissions가 달라질 수 있다. “ACL 설정은 보안팀, chmod는 배포팀”처럼 ownership을 나눠도 실제 state는 하나의 permission model이다. **Mode mutation과 ACL mutation을 같은 configuration domain으로 관리**하고 둘 중 어느 tool이 마지막으로 썼는지를 추적해야 한다. 자동화 도구가 chmod와 setfacl을 서로 다른 순서로 실행하면 최종 effective 권한이 달라질 수 있으므로 deployment는 최종 ACL dump를 acceptance evidence로 남겨야 한다.

## CHAPTER 21 · Kernel access check는 owner→named user→group class→other 같은 주체 분기와 mask를 조합한다

ACL authorization은 모든 entries를 단순 OR하는 방식이 아니다. Caller가 inode owner인지, named user entry가 있는지, 어떤 groups가 match하는지에 따라 branch가 달라지고 group class에는 mask가 적용된다. 이 순서를 잘못 이해하면 “other에 read가 있으니 누구나 읽는다”거나 “named user deny entry를 만들 수 있다” 같은 잘못된 모델을 만들 수 있다. POSIX ACL은 일반 deny ACE 모델이 아니라 permission classes의 허용 범위를 표현한다. **Authorization algorithm 자체가 invariant**이므로 custom policy simulator를 만들면 kernel 결과와 corpus test로 계속 비교해야 한다.

## CHAPTER 22 · stat() mode bits는 ACL 전체를 직렬화한 것이 아니라 축약된 permission class view다

Extended ACL이 있는 file에서도 `stat()`은 전통적인 mode field를 반환한다. 그러나 이 bits만으로 named user/group entries를 복원할 수 없다. Group-class bits는 ACL mask의 의미와 연결되어 보여 ACL 내부 permissions와 1:1 대응하지 않는다. Monitoring agent가 stat만 수집하면 “mode 변화 없음”인데 실제 named entry가 추가된 사건을 놓칠 수 있다. 반대로 ACL mask 변경이 mode의 group bits 변화로 나타날 수 있다. **Mode와 ACL은 서로 연결되어 있지만 정보량이 다르다**는 점을 audit schema에 반영해야 한다. 따라서 compliance scanner는 mode-only snapshot과 full ACL snapshot을 구분해 저장하고, 둘 사이 drift가 생기면 어느 metadata channel에서 변경됐는지 추적할 수 있어야 한다.

## CHAPTER 23 · Hard link와 rename은 inode를 유지하므로 xattr과 ACL도 pathname이 아니라 object와 함께 이동한다

Rename은 같은 filesystem 안에서 directory entry topology를 바꾸지만 inode 자체를 새로 만드는 operation이 아니다. Hard link도 하나의 inode에 여러 names를 추가한다. 따라서 xattr·ACL은 각 pathname copy가 아니라 inode metadata로 공유된다. 한 hard link를 통해 ACL을 바꾸면 다른 hard link name에서도 같은 object permission이 달라진다. Security tool이 pathname별 독립 policy로 생각하면 duplicate entries를 서로 다른 files로 오판할 수 있다. **Device+inode identity와 link topology를 함께 보아야 metadata mutation의 blast radius를 계산**할 수 있다.

## CHAPTER 24 · File copy는 새 inode 생성이므로 xattr·ACL 보존 여부가 copy tool의 명시적 policy가 된다

`read()`와 `write()`로 contents만 복제하면 destination은 새 inode이고 source xattrs나 ACL을 자동 상속하지 않는다. 일부 high-level copy 도구는 옵션과 권한에 따라 ACL/xattrs를 보존하지만, failure를 warning으로만 처리할 수도 있다. Deployment artifact를 “복사 성공”으로 판단하기 전에 security metadata preservation 결과를 별도로 검증해야 한다. 특히 backup restore, container image extraction, cross-filesystem migration에서는 namespace support 차이가 생긴다. **Content copy success와 metadata fidelity success를 두 개의 gate로 분리**해야 한다.

## CHAPTER 25 · Reflink나 copy-on-write clone도 “같은 data blocks”와 “같은 inode metadata object”를 구분해야 한다

Filesystem clone/reflink은 data extent를 공유해 빠른 copy를 만들 수 있지만 source와 destination이 독립 inode라면 이후 xattr·ACL mutation은 독립 metadata state가 될 수 있다. “block을 공유하니 ACL도 공유된다”는 가정은 잘못이다. 반대로 clone 시점에 어떤 metadata가 초기 복제되는지는 filesystem과 API semantics를 확인해야 한다. Snapshot·clone 기반 deployment에서는 **data sharing lifetime과 metadata independence**를 별도 관찰해야 한다. 이후 source ACL을 고쳤다고 destination clone에 자동 반영될 것이라고 기대하면 stale security state가 남는다.

## CHAPTER 26 · 여러 xattr의 logical consistency는 application이 generation이나 external lock으로 만들어야 한다

Application이 `user.schema`, `user.owner`, `user.checksum` 세 attribute를 하나의 logical record처럼 사용해도 kernel은 이를 multi-key transaction으로 묶어주지 않는다. Reader가 세 값을 순서대로 읽는 동안 writer가 중간 두 개만 갱신하면 mixed generation을 볼 수 있다. 해결책은 single encoded xattr에 immutable record를 넣거나, generation field를 앞뒤로 검증하거나, inode-level external coordination을 두는 것이다. **Logical record boundary를 storage API boundary와 일치시키는 것**이 가장 단순하다. 여러 independent xattr을 DB row처럼 취급하면 consistency proof가 필요하다.

## CHAPTER 27 · ACL과 security xattr 변경은 권한 상승 표면이므로 metadata writer 자체가 least privilege 대상이다

Privileged daemon이 사용자 요청을 받아 arbitrary path의 ACL, owner, capability xattr을 바꾸는 API를 제공하면 그 daemon이 사실상 authorization broker가 된다. Path traversal, symlink race, namespace confusion, unchecked requested rights가 있으면 caller가 직접 갖지 못한 authority를 file에 심을 수 있다. Metadata service는 stable FD/openat2 resolution, allow-listed root, bounded rights, caller identity를 결합해야 한다. **“내용은 안 쓰고 metadata만 쓴다”는 이유로 낮은 위험 작업으로 분류하면 안 된다.** File capability나 ACL은 실행 권한과 데이터 접근권한을 직접 바꿀 수 있다.

## CHAPTER 28 · ENODATA·ERANGE·E2BIG·ENOSPC·EDQUOT·EOPNOTSUPP는 서로 다른 복구 경로를 요구한다

Xattr operation failure를 하나의 generic I/O error로 처리하면 retry가 잘못된다. Attribute 부재는 create path일 수 있고, `ERANGE`는 probe/read race나 buffer 부족, `E2BIG`은 value 한계, `ENOSPC`·`EDQUOT`는 metadata capacity/accounting, `EOPNOTSUPP`는 filesystem capability 문제를 나타낼 수 있다. Unsupported filesystem에 무한 retry를 해도 해결되지 않고, quota failure를 attribute absence로 취급하면 policy marker를 잃을 수 있다. **Errno를 state category로 보존하고 operation intent와 함께 기록**해야 recovery가 정확해진다.

## CHAPTER 29 · Crash consistency는 syscall success와 durable metadata persistence를 구분해서 검증해야 한다

`setxattr()`이나 ACL mutation syscall이 성공했다는 것은 running kernel state에서 operation이 수용되었다는 의미이지, 전원 손실 직후 반드시 같은 metadata가 영구 media에 남는다는 일반 보장으로 확대하면 안 된다. Filesystem journaling, writeback, fsync semantics가 metadata durability에 관여한다. Security-critical provisioning이 crash 직후에도 capability/ACL과 content version의 일관성을 요구한다면 해당 filesystem의 durability contract를 확인하고 file 또는 directory sync strategy를 설계해야 한다. **Runtime authorization correctness와 crash-recovery correctness는 별도 gate**다.

## CHAPTER 30 · Xattr/ACL 운영 계약은 inode identity·namespace·effective permission·generation·durability evidence를 함께 남겨야 한다

재현 가능한 metadata incident에는 pathname 하나와 `ls -l` 출력만으로 부족하다. Device/inode identity, link count와 hard-link paths, xattr namespace/name/value hash 또는 safe representation, ACL access/default entries, mask와 effective permissions, owner/group, writer credential, user namespace, mutation syscall/flags, errno, generation marker, copy/restore source, durability step를 수집해야 한다. 이 evidence가 있어야 **“파일 내용은 같지만 권한이 다르다”, “mode는 같지만 named ACL이 다르다”, “path는 바뀌었지만 같은 inode다”** 같은 실제 failure를 설명할 수 있다. Metadata는 부가 정보가 아니라 authorization과 system behavior를 결정하는 primary state다.
