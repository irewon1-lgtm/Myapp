# PART 03 · 메모리와 저장 — locality, lifetime, durability의 경계

프로그램의 데이터는 register·cache·DRAM·page cache·storage를 같은 속도와 같은 수명으로 이동하지 않는다. 성능 문제는 **어디에 데이터가 있는가**보다 **어떤 접근 패턴으로 어느 계층을 왕복하는가**에 좌우되고, 데이터 유실 문제는 `write()` 호출 여부보다 **어느 durability 경계까지 도달했는가**에 좌우된다.

---

## CHAPTER 01 · memory hierarchy는 locality를 전제로 성능을 만든다

CPU와 DRAM 사이의 latency 차이는 cache hierarchy로 완화된다. cache는 개별 변수의 의미를 알지 못하고 cache line 단위로 데이터를 이동한다. 따라서 알고리즘의 Big-O가 같아도 address access pattern이 다르면 실행시간이 크게 달라질 수 있다.

spatial locality는 인접 주소를 사용할 가능성이고 temporal locality는 최근 사용한 데이터를 다시 사용할 가능성이다. contiguous array scan은 두 성질을 활용하기 쉽지만 pointer-chasing structure는 다음 주소를 이전 load 결과로 알아야 해서 memory-level parallelism과 prefetch가 제한될 수 있다.

working set이 cache capacity를 넘으면 conflict/capacity miss가 늘고, 여러 core가 같은 cache line을 write하면 coherence traffic이 증가한다. 성능 분석에서 `cache miss` 한 숫자만 보는 대신 level별 miss, line sharing, access stride, working-set size를 함께 본다.

---

## CHAPTER 02 · virtual address translation은 실행마다 발생하는 독립 비용이다

process가 사용하는 pointer는 일반적으로 virtual address다. page table은 virtual page를 physical frame과 protection metadata에 연결한다. multi-level page table은 전체 address space를 평평한 거대한 table로 유지하는 비용을 줄이지만 translation 자체에 여러 memory access가 필요할 수 있다.

TLB는 최근 translation을 cache한다. data cache hit인데 TLB miss가 발생할 수도 있고, 반대로 TLB hit인데 data가 lower cache/DRAM에 있을 수도 있다. 큰 sparse working set, 특정 stride, 많은 process/context 변화는 translation behavior를 성능 변수로 만든다.

page table entry에는 present 상태뿐 아니라 read/write/execute 권한, dirty/accessed 관련 상태가 포함될 수 있다. protection fault를 memory shortage와 동일시하면 안 된다. **주소가 mapping되어 있는가, 권한이 맞는가, backing page가 resident한가**는 별도 질문이다.

---

## CHAPTER 03 · page fault는 오류가 아니라 lazy state transition일 수 있다

page fault는 현재 translation state만으로 memory access를 완료할 수 없음을 CPU가 kernel에 알리는 사건이다. kernel은 anonymous page를 새로 준비하거나, file-backed page를 page cache에 가져오거나, copy-on-write를 처리한 뒤 instruction을 재시도할 수 있다.

반대로 unmapped address, permission violation, backing I/O failure는 process-visible fault나 종료로 이어질 수 있다. 동일한 `page fault`라는 event가 demand allocation과 fatal invalid access를 모두 포함할 수 있으므로 fault reason과 후속 kernel action을 확인해야 한다.

성능에서는 major/minor 분류 자체보다 storage I/O, reclaim, page-table work와 연결된 실제 latency를 본다. page fault count가 많아도 zero-fill anonymous page의 비용과 storage-backed fault의 비용은 다르다.

---

## CHAPTER 04 · copy-on-write는 sharing의 비용을 write 시점으로 미룬다

copy-on-write는 두 address space가 동일 physical page를 공유하다 어느 쪽이 수정할 때 private copy를 만드는 전략이다. fork, snapshot, runtime process creation에서 초기 복사를 줄일 수 있지만 write-heavy workload에서는 page copy와 memory growth가 뒤늦게 발생한다.

COW를 사용할 때 `virtual size`, `RSS`, proportional/shared memory를 구분해야 한다. shared page를 process별로 단순 합산하면 실제 physical consumption을 과대평가할 수 있고, 반대로 write가 늘어 private copy가 생성되면 처음 측정한 sharing 비율이 유지되지 않는다.

COW fault는 correctness를 위한 정상 경로이지만 latency spike를 만들 수 있다. fork 후 large heap을 수정하는 workload, snapshot 직후 write burst처럼 access pattern이 바뀌는 시점을 trace와 함께 본다.

---

## CHAPTER 05 · mmap은 file I/O를 address-space access로 바꾸지만 비용을 제거하지 않는다

memory mapping은 file offset range를 virtual address range에 연결한다. 이후 load/store는 ordinary memory access처럼 보이지만 missing page는 fault를 통해 page cache와 storage I/O를 유발할 수 있다.

mmap의 이점은 API call 수를 줄이고 kernel page cache와 직접 연결되는 access model을 제공할 수 있다는 점이다. 그러나 random fault storm, truncation race, address-space pressure, writeback timing, synchronization 문제가 추가된다.

`mmap = zero-copy = faster`라는 등식은 성립하지 않는다. workload의 access pattern, file size, fault frequency, reuse, memory pressure, write durability를 측정해야 한다. mapping lifetime과 file descriptor lifetime도 분리된다. descriptor close가 mapping lifetime과 동일한 것은 아니다.

---

## CHAPTER 06 · allocator는 object request와 OS page allocation 사이의 정책 계층이다

application의 작은 object allocation마다 kernel syscall이 발생하는 것은 아니다. allocator/runtime는 큰 region을 확보해 size class, arena, free list 같은 구조로 나누고 재사용한다. 따라서 object count와 syscall count를 직접 대응시키면 안 된다.

fragmentation은 `free bytes` 총량만으로 설명되지 않는다. allocator metadata, size-class rounding, page utilization, thread-local cache, arena 분할 때문에 사용자가 요청한 logical bytes보다 훨씬 많은 RSS가 유지될 수 있다.

free된 object가 allocator에 반환돼도 allocator가 그 page를 즉시 OS에 돌려주지 않으면 RSS는 감소하지 않을 수 있다. leak을 판단할 때 live object graph, allocator retained memory, mapped but unused region을 구분한다.

---

## CHAPTER 07 · garbage collector는 reachability와 reclamation policy를 분리한다

managed runtime에서 object가 더 이상 필요하지 않다는 사실과 memory가 즉시 반환된다는 사실은 다르다. GC는 root set에서 reachability를 추적해 live object를 판정하고 collection policy에 따라 reclaim/compact한다.

generational collector는 많은 object가 짧게 산다는 관찰을 이용해 young generation을 더 자주 수집할 수 있다. concurrent collector는 application thread와 일부 GC work를 겹치지만 barrier, remembered set, background CPU 비용을 추가한다.

GC 문제는 `heap size` 하나가 아니다.

```text
allocation rate
live-set size
promotion rate
pause distribution
concurrent GC CPU
fragmentation / compaction
reference retention
```

이 지표를 함께 봐야 allocation churn, genuine leak, heap sizing 문제를 구분할 수 있다.

---

## CHAPTER 08 · memory pressure는 application heap 밖에서도 발생한다

process memory에는 managed heap 외에 native heap, thread stacks, mmap, graphics buffers, code/JIT cache, shared libraries, page-table overhead가 포함될 수 있다. JVM/ART heap graph만 보고 process RSS 증가를 설명할 수 없는 이유다.

system-wide pressure가 커지면 kernel은 clean page cache를 reclaim하고, dirty page writeback을 촉진하며, platform에 따라 compressed memory/swap 계층을 활용하거나 process termination 정책을 적용할 수 있다. reclaim 과정 자체가 latency를 만든다.

OOM은 단순히 `free RAM=0`일 때만 발생하는 사건이 아니다. allocation constraint, cgroup/process limit, virtual mapping, contiguous requirement, overcommit policy처럼 여러 조건이 관여할 수 있다. 실제 kill reason과 memory accounting을 확인한다.

---

## CHAPTER 09 · Android memory는 managed heap만 측정하면 불완전하다

Android application process는 ART heap, native allocation, graphics, code, shared mappings 등 여러 영역을 사용한다. system은 process importance와 memory pressure에 따라 background process를 종료할 수 있으므로 process lifetime 자체를 영구 상태로 가정하면 안 된다.

Activity/ViewModel/Compose state와 process persistence를 혼동하면 복원 불가능한 상태가 생긴다. 중요한 user state는 process memory 외부의 durable source of truth와 복원 계약을 가져야 한다.

성능 도구는 Java/Kotlin allocation과 native/graphics memory를 서로 다른 방식으로 관찰할 수 있다. `Profiler에서 heap이 안정적이니 memory leak이 없다`는 결론은 측정 범위를 먼저 확인해야 한다.

---

## CHAPTER 10 · write path는 application buffer에서 persistent media까지 여러 단계를 가진다

application이 write API를 호출했을 때 데이터는 user-space buffer, kernel page cache, filesystem journal/data block, device cache를 거쳐 persistent media에 도달할 수 있다. 어느 지점에서 성공을 반환하는지는 API와 filesystem/device 정책에 따라 다르다.

따라서 다음 상태는 동일하지 않다.

```text
application buffer에 반영됨
kernel이 write를 수락함
filesystem metadata/data가 ordering 규칙을 만족함
device가 flush를 완료함
power loss 후 복구 가능한 상태가 됨
```

`flush()`라는 함수명만 보고 durability를 추정하지 않는다. language buffer flush, OS fsync 계열, database transaction commit은 서로 다른 경계를 가진다.

---

## CHAPTER 11 · crash consistency는 정상 실행 결과가 아니라 중간 실패 상태를 설계한다

파일 두 개를 순서대로 갱신하는 작업이 정상적으로 끝나면 일관되어 보여도 첫 번째 write 후 전원이 꺼지면 불변조건이 깨질 수 있다. crash consistency는 **어느 instruction/I/O 뒤에서 중단돼도 복구 가능한 상태를 정의하는 문제**다.

atomic rename, temporary file + fsync + rename, journal, copy-on-write filesystem, database transaction은 서로 다른 계층에서 partial update를 다룬다. 중요한 것은 API 이름보다 보장 범위를 확인하는 것이다.

metadata durability와 file-content durability도 분리될 수 있다. 파일 내용이 기록됐지만 directory entry가 durable하지 않거나 그 반대의 조합을 고려해야 한다. crash test는 kill signal만으로 power-loss semantics 전체를 재현하지 못할 수 있다.

---

## CHAPTER 12 · WAL은 data page보다 먼저 redo/intent 정보를 durable하게 만든다

write-ahead logging의 핵심은 변경된 data page 자체보다 **복구에 필요한 log record를 먼저 durable하게 만드는 ordering**이다. crash 후 log를 사용해 committed change를 redo하거나 incomplete operation을 식별할 수 있다.

WAL은 `로그 파일 하나 더 쓴다`는 기능이 아니라 다음 invariant를 요구한다.

```text
recovery에 필요한 log가 data page보다 먼저 durable해야 한다
commit 응답 전에 필요한 commit record가 durability 조건을 만족해야 한다
checkpoint는 log와 data page의 복구 경계를 이동시킨다
```

SQLite와 PostgreSQL 같은 DB engine은 세부 구현과 isolation model이 다르므로 WAL이라는 이름만으로 동일한 semantics를 가정하지 않는다. transaction durability는 DB engine의 공식 문서를 기준으로 판단한다.

---

## CHAPTER 13 · memory/storage 문제는 계층별 evidence를 연결한다

메모리 문제에서 최소 증거는 다음 범주다.

```text
allocation profile
live-set / retained object
RSS / PSS / mapped regions
page fault / reclaim
GC pause and allocation rate
native / graphics allocation
```

storage 문제에서는 다음을 분리한다.

```text
application write timing
syscall latency
page-cache dirty state
filesystem writeback
block-device latency
fsync/commit latency
crash-recovery result
```

latency가 application에서 보인다고 application code가 원인이라고 단정하지 않는다. trace에서 wait chain을 따라 kernel reclaim, storage queue, GC, lock contention까지 연결해야 한다.

---

## CHAPTER 14 · lifetime과 durability는 서로 다른 축이다

메모리·파일·DB를 하나의 저장 문제로 묶으면 판단이 흐려진다. 먼저 두 축으로 나눈다.

```text
lifetime: 값이 언제까지 접근 가능해야 하는가

durability: crash/power loss 이후에도 복구되어야 하는가
```

cache는 lifetime이 짧아도 재생성 가능하면 충분할 수 있고, 금융 transaction은 process lifetime과 무관하게 durable해야 한다. immutable artifact는 memory에 resident하지 않아도 storage에서 다시 mapping할 수 있다.

설계 시 각 상태에 대해 **source of truth, rebuild 가능성, 최대 허용 손실 범위, commit point, 복구 절차**를 명시한다. 이 다섯 항목이 없으면 `저장했다`는 말은 운영 보장으로 사용할 수 없다.