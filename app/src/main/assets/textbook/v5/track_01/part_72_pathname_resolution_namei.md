# PART 72 · Pathname Resolution Internals — dentries, RCU-walk, symlinks, mounts, rename races, openat2

Path string은 file object가 아니다. Kernel은 start directory와 root context에서 component를 하나씩 해석해 **dentry cache, inode, mount tree, symbolic link, permission, rename/mount sequence**를 통과한 뒤 최종 object reference를 만든다. 공격자가 directory tree를 동시에 바꿀 수 있는 환경에서는 pathname resolution 자체가 concurrent state machine이며, userspace가 `검사 후 open`으로 재현하려 하면 TOCTOU가 생긴다.

## CHAPTER 01 · Path lookup은 starting path와 component stream의 결합이다

`a/b/c` 같은 relative path는 current working directory 또는 dirfd에서 시작하고 `/a/b` 같은 absolute path는 effective root에서 시작한다. 동일 string도 start path와 mount namespace가 다르면 다른 object로 resolve된다. 따라서 pathname identity를 string만으로 logging하거나 authorization cache key로 쓰면 namespace/context가 빠진다. Secure resolution은 start directory reference, effective root, lookup flags를 함께 state로 관리해야 한다.

## CHAPTER 02 · Dentry는 이름과 inode object 사이의 cached association이다

Dentry는 parent directory 안의 component name과 inode association을 cache한다. File이 rename되어도 dentry와 inode가 함께 이동할 수 있으므로 pathname text와 inode identity는 분리된다. Open file은 unlink 후에도 inode reference 때문에 계속 사용 가능하며 dentry가 path namespace에서 사라지는 것과 object lifetime이 동시에 끝나지 않는다. VFS lookup은 `string→inode` 직접 변환이 아니라 dentry graph를 따라간다.

## CHAPTER 03 · Negative dentry는 존재하지 않는 이름도 cache한다

Lookup 결과 해당 name이 없으면 kernel은 inode가 없는 negative dentry를 cache할 수 있다. 반복된 miss를 filesystem까지 내려보내지 않는 성능 최적화지만, 이후 file creation/rename이 그 dentry state를 positive로 바꿔야 한다. `dcache entry가 있음=파일이 있음`이 아니다. Debugger와 filesystem code는 dentry 존재와 `d_inode != NULL`를 구분해야 하며 negative cache invalidation이 틀리면 새로 생긴 file을 못 찾는 stale miss가 된다.

## CHAPTER 04 · d_lock은 하나의 dentry name/parent/hash membership을 보호한다

Rename/unlink는 dentry의 parent, name, hash-table 위치를 바꿀 수 있다. REF-walk가 candidate dentry를 검증할 때 `d_lock`으로 그 component의 local state를 안정화한다. Parent directory 전체를 lock하지 않고 child dentry 단위로 검증할 수 있어 cache hit path의 contention을 줄인다. 그러나 directory mutation serialization과 local dentry stability는 다른 lock domains이므로 둘을 하나로 생각하면 race를 놓친다.

## CHAPTER 05 · Directory i_rwsem은 directory namespace mutation을 serialize한다

Create/unlink/rename처럼 한 directory의 set of names를 바꾸는 operation은 directory inode의 rwsem을 통해 coordination한다. Cache miss에서 filesystem lookup을 호출할 때도 shared lock이 필요할 수 있다. `d_lock`은 한 dentry의 local fields를 보호하지만 `i_rwsem`은 directory 전체 namespace operation의 ordering을 제공한다. Lookup fast path와 slow filesystem lookup이 다른 lock cost를 가지는 이유다.

## CHAPTER 06 · lookup_fast는 dcache hit를 filesystem round trip 없이 해결한다

현재 parent+name에 대한 valid dentry가 dcache에 있고 revalidation이 필요 없으면 lookup은 filesystem implementation까지 내려가지 않고 다음 component로 진행할 수 있다. 이 fast path가 hot-path pathname performance를 만든다. 하지만 cache hit가 stale remote filesystem metadata를 의미할 수 있어 filesystem은 revalidation hook과 validity policy를 제공할 수 있다. Fast path는 correctness rule을 생략하는 것이 아니라 cached proof가 충분할 때만 사용된다.

## CHAPTER 07 · lookup_slow는 cache miss에서 filesystem authority에 질문한다

Dcache에서 name을 찾지 못하면 REF-walk는 directory lock을 잡고 다시 확인한 뒤 filesystem lookup method를 호출한다. Concurrent thread가 사이에 dentry를 만들 수 있기 때문에 `miss 한 번`을 final truth로 취급하지 않는다. Slow lookup 결과가 존재하지 않아도 negative dentry가 cache될 수 있다. Storage/network filesystem latency가 path resolution syscall latency로 직접 들어오는 경계다.

## CHAPTER 08 · REF-walk는 counted reference로 현재 path lifetime을 고정한다

REF-walk는 current dentry와 mount에 reference를 유지한 채 next component reference를 획득한 뒤 old reference를 놓는 hand-over-hand 방식으로 path object lifetime을 보장한다. 그래서 lookup 도중 unlink/rename이 발생해도 memory가 다른 object로 재사용되는 위험을 피한다. 비용은 refcount traffic과 locks다. 안정성은 높지만 high-frequency lookup에서 shared cache lines와 atomic operations이 bottleneck이 될 수 있다.

## CHAPTER 09 · RCU-walk는 reference를 잡지 않고 변화를 감지해 fast path를 만든다

RCU-walk는 counted dentry/mount references를 매 component마다 증가시키지 않고 RCU read-side protection과 sequence validation을 사용한다. Mutation을 막지 않고 `lookup하는 동안 바뀌었는가`를 검출한다. 충분히 안정적인 cache-hit path에서는 훨씬 싼 비용으로 진행하지만 sleep이 필요한 operation이나 uncertain state를 만나면 RCU mode를 유지할 수 없다. Optimistic concurrency control과 같은 구조다.

## CHAPTER 10 · RCU-walk는 REF-walk가 만들 수 없는 결정을 내려서는 안 된다

RCU mode가 빠르다고 해서 weaker semantic을 허용하면 안 된다. New dentry를 읽은 뒤 old dentry sequence를 재검증하는 hand-over-hand validation은 counted-reference REF-walk와 동일한 logical path를 보장하기 위한 것이다. Sequence mismatch가 나면 optimistic result를 버리고 retry/fallback한다. 성능 fast path의 핵심 invariant는 `틀릴 수도 있으니 빨리 반환`이 아니라 `변화가 있으면 반드시 알아채고 강한 path로 전환`이다.

## CHAPTER 11 · unlazy_walk는 RCU state를 stable references로 승격한다

RCU-walk가 cache miss, permission check, automount, complex symlink 등 sleep/lock이 필요한 지점을 만나면 현재 dentry/mount/symlink references를 실제 counted references로 변환하고 sequence가 여전히 valid한지 확인한다. 성공하면 REF-walk로 계속하고, mutation을 감지하면 처음부터 다시 lookup한다. Optimistic read에서 pessimistic locking으로 넘어가는 순간 자체가 race-sensitive transition이다.

## CHAPTER 12 · -ECHILD retry는 lookup failure가 아니라 fast-path validation 실패일 수 있다

RCU path가 seqlock change나 non-RCU-safe operation을 만나 `-ECHILD`를 올려보내면 syscall layer가 REF-walk로 재시도할 수 있다. 내부적으로 한 번 실패했어도 userspace에는 정상 lookup 결과가 나올 수 있다. Performance tracing에서 internal retry를 filesystem error로 세면 안 된다. RCU fallback rate가 높다면 namespace churn이나 filesystem revalidation 때문에 fast path가 이득을 못 얻는 신호다.

## CHAPTER 13 · rename_lock은 dcache hash search 중 rename을 감지하는 seqlock이다

Dentry rename은 name/parent가 바뀌며 hash bucket도 이동할 수 있다. Lookup thread가 old bucket chain을 scan하던 중 dentry가 이동하면 실제 entry를 놓칠 수 있다. Global rename seqlock sequence를 lookup 전후로 비교해 concurrent rename이 있었고 search가 실패했다면 retry한다. Lookup이 rename을 막는 게 아니라 **miss가 진짜 miss였는지 검증**하는 방식이다.

## CHAPTER 14 · rename_lock은 RESOLVE_BENEATH/IN_ROOT escape 공격 방어에도 쓰인다

`..`을 처리하는 동안 attacker가 parent directory를 starting root 밖으로 rename하면 단순 `path_equal(root)` 검사 사이를 비집고 escape할 수 있다. Kernel은 rename sequence 변화와 dotdot traversal을 결합해 잠재 attack을 감지하면 `EAGAIN`으로 lookup을 실패시킬 수 있다. Path containment는 pathname prefix 비교가 아니라 concurrent namespace mutation까지 포함한 security proof다.

## CHAPTER 15 · mount_lock은 mount tree 변화가 path crossing을 오염시키는지 감지한다

Component lookup은 mountpoint에 도달하면 다른 vfsmount root로 넘어갈 수 있다. Concurrent mount/unmount가 발생하면 current path의 mount relationship이 바뀔 수 있으므로 sequence validation이 필요하다. `mnt_count` reference는 mount structure memory lifetime을 보장하지만 namespace에 계속 연결돼 있다는 보장과는 다르다. Structure lifetime과 namespace membership을 분리해야 한다.

## CHAPTER 16 · mount crossing은 directory hierarchy가 아니라 filesystem graph traversal이다

Path component가 mountpoint를 만나면 dentry parent-child만 따라가는 게 아니라 mounted filesystem root로 jump한다. Bind mount는 같은 underlying subtree를 다른 namespace 위치에 노출할 수 있다. `..` 처리도 mount root를 만나면 mounted-on path로 돌아가야 한다. Path traversal security는 dentry tree뿐 아니라 mount graph를 함께 제한해야 한다.

## CHAPTER 17 · chroot는 path root를 바꾸지만 열린 descriptor capability를 없애지 않는다

Process root를 바꿔 absolute path resolution 범위를 제한해도 chroot 밖 object에 이미 열린 fd가 있거나 working directory/mount configuration이 잘못되면 authority가 남을 수 있다. chroot는 full sandbox가 아니다. `openat2 RESOLVE_IN_ROOT`처럼 특정 operation의 root를 explicit하게 제한하는 방식과 process-wide root mutation은 threat model이 다르다.

## CHAPTER 18 · dotdot 처리는 root boundary에서 특별한 state transition이다

일반 directory에서 `..`은 parent를 따라가지만 effective root에서는 더 위로 나가면 안 된다. Mount root에서는 mounted-on dentry로 이동하는 추가 규칙도 있다. Concurrent rename/mount 때문에 parent relationship이 바뀔 수 있어 `..`은 단순 string stack pop이 아니다. Containment-aware lookup은 dotdot를 가장 위험한 escape component로 취급한다.

## CHAPTER 19 · Symlink는 component stream 자체를 동적으로 바꾼다

Symlink body가 상대경로면 symlink가 있던 directory를 기준으로 새 component stream을 이어가고 absolute path면 effective root에서 다시 시작할 수 있다. Nested links는 원래 pathname과 link body를 stack으로 관리해야 한다. Userspace에서 string substitution만 흉내내면 mount/root/magic-link semantics를 재현하기 어렵다. Symlink resolution은 lookup state machine 내부 operation이다.

## CHAPTER 20 · Symlink depth/budget은 cycle과 resource exhaustion을 제한한다

`a→b`, `b→a` 같은 cycle이나 매우 긴 chain을 무한히 따라가면 kernel stack/CPU를 소모한다. Lookup은 follow count/budget을 제한해 `ELOOP` 같은 failure로 종료한다. Security parser는 `path length만 제한`해서 충분하지 않으며 expansion 후 traversal work의 상한을 둬야 한다. Magic link와 ordinary symlink를 별도로 금지할 수 있는 이유도 capability가 다르기 때문이다.

## CHAPTER 21 · Magic link는 pathname string이 아니라 existing kernel object로 jump할 수 있다

`/proc/<pid>/fd/<n>` 같은 entry는 읽으면 path처럼 보이지만 follow 시 단순 string target을 다시 lookup하는 대신 open file object/path reference로 jump할 수 있다. Target이 이미 unlink되었거나 다른 mount에 있어도 reference가 존재할 수 있다. Sandbox가 symlink만 막고 magic link를 허용하면 pathname root 밖 capability로 이동할 수 있어 별도 `NO_MAGICLINKS` 정책이 필요하다.

## CHAPTER 22 · Final component는 create/unlink/rename 때문에 intermediate components와 다르다

Path의 마지막 name은 `open O_CREAT`, unlink, rename target처럼 namespace mutation 대상이 될 수 있어 parent directory를 exclusive하게 lock하고 `존재/부재` 상태를 atomic하게 검사해야 할 수 있다. Intermediate traversal과 final operation을 분리하지 않으면 `검사 후 생성` race가 생긴다. VFS의 lookup flags가 parent/final component를 다르게 다루는 이유다.

## CHAPTER 23 · O_NOFOLLOW는 trailing symlink만 막을 수 있어 전체-path restriction이 아니다

마지막 component symlink follow를 금지해도 앞쪽 `a/b` 중 `a`가 symlink면 다른 subtree로 이동할 수 있다. `RESOLVE_NO_SYMLINKS`는 모든 components의 symlink traversal을 막아 더 강한 guarantee를 제공한다. Security option 이름이 비슷하다고 threat coverage가 같지 않다. 전체 path policy인지 final component policy인지 명확히 구분해야 한다.

## CHAPTER 24 · RESOLVE_BENEATH는 dirfd 아래 descendant만 허용하는 one-shot containment다

Untrusted relative path를 trusted dirfd 아래에서 열 때 absolute path와 root escape, magic-link jump, escaping `..`을 막을 수 있다. 중요한 점은 userspace가 components를 미리 검사하는 것이 아니라 kernel lookup 과정 자체가 restriction을 유지한다는 것이다. Concurrent rename 공격까지 sequence lock으로 감지하기 때문에 TOCTOU defense가 pathname walker 안에 있다.

## CHAPTER 25 · RESOLVE_IN_ROOT는 dirfd를 operation-local virtual root처럼 사용한다

`IN_ROOT`는 absolute path와 absolute symlink도 dirfd를 root로 다시 해석하고 root에서 `..`을 no-op처럼 처리한다. Process-global chroot를 변경하지 않고 한 syscall의 resolution root만 제한할 수 있다. Multithread server에서 tenant별 filesystem view를 처리할 때 global cwd/root mutation 없이 explicit path root를 사용할 수 있다.

## CHAPTER 26 · RESOLVE_NO_XDEV는 mount와 bind-mount crossing까지 제한한다

Path가 descendant directory에 있어도 그 안에 attacker-controlled bind mount가 있으면 다른 filesystem/subtree로 이동할 수 있다. `NO_XDEV`는 ordinary mount와 bind mount crossing을 막아 containment를 더 강하게 만든다. 다만 legitimate mount layout을 사용하는 application에서는 policy와 usability가 충돌하므로 allowed mount topology를 threat model에 맞춰 선택해야 한다.

## CHAPTER 27 · RESOLVE_CACHED는 lookup을 cache-only latency contract로 제한한다

All components가 dcache에서 RCU-compatible fast path로 해결되지 않고 I/O/revalidation/slow lookup이 필요하면 `EAGAIN`으로 빠질 수 있다. Event-loop thread가 potentially blocking filesystem lookup을 피하고 slow worker로 fallback하는 구조에 사용할 수 있다. 이것은 존재 여부를 cache에서 대충 추측하는 게 아니라 **blocking 가능성이 생기면 operation 자체를 중단**하는 latency-control contract다.

## CHAPTER 28 · Path resolution observability는 string보다 lookup mode와 retry cause가 중요하다

동일 pathname이 어떤 요청에서는 microseconds, 다른 요청에서는 milliseconds가 걸리는 이유는 dcache hit, RCU→REF fallback, filesystem slow lookup, automount, revalidation, symlink, mount crossing에 따라 다르다. Trace에는 start dir/mount namespace, RCU retry, component count, cache miss, lookup_slow, filesystem callback latency를 기록해야 한다. `open 느림`만으로 storage 문제라고 결론내리면 안 된다.

## CHAPTER 29 · Path-cache benchmark는 rename/unlink churn과 cold misses를 포함해야 한다

Hot loop에서 같은 existing path만 open하면 dcache/RCU-walk 최고 성능만 측정한다. Production에는 newly created files, negative lookups, rename-heavy deployment, container mount changes, remote filesystem revalidation이 섞인다. Benchmark는 cache-hot, cache-cold, negative, concurrent rename, symlink-heavy 시나리오를 분리해 lookup fast-path coverage와 fallback cost를 측정해야 한다.

## CHAPTER 30 · Pathname resolution은 concurrent namespace에서 object identity를 안전하게 획득하는 알고리즘이다

Robust pathname handling은 **start/root path reference, dentry/inode lifetime, negative cache, RCU/REF transition, directory/dentry locks, rename/mount seqlocks, symlink/magic-link stack, mount crossing, final-component mutation, openat2 containment**을 하나의 algorithm으로 본다. Path string은 요청일 뿐 결과 object identity가 아니다. Security와 correctness는 lookup이 끝나는 순간까지 namespace가 바뀔 수 있음을 전제로 kernel-level resolution policy로 보장해야 한다.
