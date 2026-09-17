# PART 49 · Fault Injection and Failure Testing — errors, crashes, resource exhaustion, invariants

정상 입력에서 기능이 동작하는지 확인하는 테스트만으로 시스템의 신뢰성을 설명할 수 없다. 운영 장애는 allocation 실패, short write, storage error, timeout, cancellation, process death, disk full, clock discontinuity, resource exhaustion처럼 **정상 control flow를 중간에서 끊거나 일부 side effect만 남기는 사건**으로 나타난다. Fault injection은 이런 사건을 재현 가능한 위치에 의도적으로 넣어 recovery invariant와 evidence를 검증하는 방법이다.

## CHAPTER 01 · fault model은 어떤 failure를 시스템이 견뎌야 하는지 먼저 정의한다

`장애에도 안전하다`는 문장은 범위가 없다. Process crash는 견디지만 disk corruption은 못 견딜 수 있고, single allocation failure는 처리하지만 global OOM kill은 복구 불가능할 수 있다. Component마다 fail-stop, omission, partial result, corruption, delay, resource exhaustion 중 어떤 fault를 contract에 포함할지 정한다. Fault model이 없으면 테스트 case를 무한히 늘려도 어떤 보장을 얻었는지 말할 수 없다. Recovery requirement와 data-loss allowance를 먼저 고정한다.

## CHAPTER 02 · fault injection point는 실제 side effect 경계와 맞아야 한다

함수 시작에서 무조건 error를 반환하는 테스트는 state가 전혀 바뀌지 않은 trivial failure만 검증할 수 있다. 실제 위험은 file 일부를 쓴 뒤, transaction log는 기록했지만 metadata 전환 전, resource 두 개 중 하나만 획득한 뒤 실패하는 지점이다. Operation을 prepare→side effect→commit 단계로 나누고 각 boundary에 injection point를 둔다. Injection point가 실제 production call path와 같은 code를 통과해야 mock-only false confidence를 줄일 수 있다.

## CHAPTER 03 · deterministic trigger는 동일 failure를 반복 재현할 수 있게 한다

`10% 확률로 실패`만 사용하면 같은 bug를 다시 만들기 어렵고 test flakiness가 생긴다. N번째 allocation, 특정 file offset, 특정 state transition, named failpoint처럼 deterministic condition을 제공하면 실패 위치를 artifact로 저장할 수 있다. Random fault schedule은 탐색 범위를 넓히는 용도로 별도 사용하고 seed를 기록한다. 발견된 random failure는 deterministic regression failpoint로 축소한다.

## CHAPTER 04 · fail-before와 fail-after-side-effect를 구분한다

Operation이 side effect를 전혀 만들기 전에 실패하는 경우와 side effect는 완료됐지만 acknowledgement만 잃는 경우는 recovery가 다르다. 후자는 caller가 retry할 때 duplicate effect를 만들 수 있다. File rename, payment-like command, queue publish, remote commit 모두 이 distinction이 중요하다. Fault injector는 `호출 자체 실패` 하나로 끝내지 않고 pre-commit/post-commit uncertainty를 재현해야 idempotency와 recovery log를 검증할 수 있다.

## CHAPTER 05 · allocation failure는 error path가 또 allocation하는지 드러낸다

Heap allocation 실패를 처리하는 catch/error-reporting path가 log formatting, exception object, fallback buffer를 다시 allocate하면 failure가 recursive하게 악화될 수 있다. Critical low-memory path는 preallocated reserve나 allocation-free diagnostic을 사용할 수 있다. Fault injection은 특정 allocation site에서 null/exception을 발생시키고 already-acquired resource가 release되는지 확인한다. Managed runtime에서도 explicit OOM catch가 system-wide recovery를 보장하지 않는다는 점을 구분한다.

## CHAPTER 06 · disk full은 write 실패뿐 아니라 metadata update 실패를 만든다

Data block 공간이 남아도 inode/metadata/journal 공간이 부족할 수 있고, temporary file 생성·rename·database WAL append가 서로 다른 지점에서 ENOSPC를 만날 수 있다. Application이 `save 성공`을 보고한 뒤 후속 metadata write가 실패하면 durability contract가 깨진다. 제한된 filesystem/loop image를 사용해 실제 free-space exhaustion을 재현하고 cleanup 이후 재시도가 가능한지 검증한다. Disk full test가 host 전체 disk를 위험하게 채우지 않도록 격리한다.

## CHAPTER 07 · short write는 write()가 요청 byte 전부를 처리한다는 가정을 깨뜨린다

Stream/file API는 성공 return이 항상 full-length를 의미하지 않을 수 있다. Signal, resource limit, device condition에서 일부 byte만 처리되고 positive count를 반환할 수 있다. Loop가 remaining range를 정확히 갱신하는지, partial record가 reader에게 노출되는지 테스트한다. Fault wrapper가 항상 `-1 error`만 반환하면 short-write bug를 못 찾는다. P44의 EINTR/partial semantics와 연결한다.

## CHAPTER 08 · fsync 실패는 write 성공과 durable 성공을 분리한다

Page cache에 write가 성공해도 backing storage에 persist하는 과정에서 I/O error가 나올 수 있다. Application이 fsync/fdatasync return을 무시하면 durable commit으로 보고한 data가 crash 후 사라질 수 있다. Injection은 data write 성공→sync failure 순서를 만들어 caller가 transaction을 실패로 처리하는지 확인한다. Database/file format은 sync 실패 후 같은 file을 계속 신뢰할 수 있는지도 명시해야 한다.

## CHAPTER 09 · rename failure는 atomic replace protocol의 마지막 전환을 검증한다

Temporary file에 새 data를 완전히 쓰고 sync했어도 target rename이 permission, cross-filesystem, I/O error로 실패할 수 있다. 이때 old target은 유지되어야 하고 temp file cleanup/retry policy가 필요하다. Rename 성공 후 directory durability까지 요구한다면 directory fsync 실패도 별도 fault다. Crash-consistent update test는 각 commit step을 개별적으로 끊는다.

## CHAPTER 10 · corrupt read는 `I/O 성공 = data 정상` 가정을 깨뜨린다

Storage/controller/filesystem이 error를 검출하면 read 자체가 실패할 수 있지만 silent corruption은 정상 길이 byte를 반환할 수 있다. End-to-end checksum/version/schema validation이 이를 검출해야 한다. Test fixture가 file bit를 뒤집거나 checksum mismatch를 만들어 parser가 fail-closed하는지 확인한다. Corrupt input에서 huge length/offset으로 resource exhaustion이 발생하지 않도록 parser bounds도 함께 검증한다.

## CHAPTER 11 · process crash는 destructor/finally가 실행되지 않는 종료를 재현한다

Graceful exception test는 RAII/finally cleanup을 검증하지만 kill/power-loss류 crash는 user-space cleanup 없이 process가 사라진다. Persistent correctness는 OS가 fd를 닫아 주는 것만으로 충분하지 않고 disk state가 commit protocol의 어느 단계였는지에 달려 있다. Test harness는 child process를 특정 failpoint에서 강제 종료하고 재시작 후 state invariant를 검사한다. In-process exception으로 crash consistency를 대신하지 않는다.

## CHAPTER 12 · crash matrix는 persistent update의 모든 prefix를 검사한다

한 operation이 A→B→C→D write/flush step을 가진다면 각 step 전후에서 crash시켜 recovery 결과를 비교한다. 허용 결과가 old state 또는 new state 둘 중 하나인지, torn hybrid state가 나오는지 정의한다. SQLite atomic-commit 설명처럼 filesystem/storage assumption이 protocol correctness에 영향을 준다. Crash point를 exhaustively 순회하면 특정 timing에만 생기는 durability bug를 deterministic하게 찾을 수 있다.

## CHAPTER 13 · power-loss test는 process kill보다 lower-layer volatile cache를 포함한다

Process kill은 kernel/page cache가 계속 살아 있어 dirty data를 나중에 flush할 수 있다. 실제 전원 손실은 OS page cache, device volatile write cache가 동시에 사라지는 더 강한 fault다. Hardware power-cut rig 또는 storage emulator가 필요할 수 있고 비용/위험이 높으므로 별도 승인된 환경에서 수행한다. 무료 시뮬레이션은 실제 power-loss PASS라고 부르지 않는다. Fault model 차이를 명시한다.

## CHAPTER 14 · clock jump는 wall-clock 기반 timeout·ordering assumption을 깨뜨린다

System time이 NTP/manual change로 앞으로/뒤로 이동하면 `now - start` duration이 음수/폭증할 수 있다. Timeout은 monotonic clock을 사용하고 timestamp ordering은 distributed clock uncertainty를 고려한다. Test clock abstraction으로 forward/backward jump, leap-like discontinuity를 deterministic하게 주입한다. P16 timekeeping invariant를 application timeout code가 실제로 따르는지 확인한다.

## CHAPTER 15 · timeout injection은 느림과 실패를 같은 path로 처리하는지 검증한다

Dependency가 즉시 error를 반환하는 경우와 deadline까지 아무 응답도 없는 경우 resource occupancy가 다르다. Timeout 동안 connection/thread/permit가 묶이고 retry가 추가 load를 만들 수 있다. Test는 response delay를 deadline 바로 전·후로 조절하고 late completion이 state를 덮어쓰지 않는지 확인한다. Timeout 후 cancellation이 실제 underlying operation까지 전파되는지도 검증한다.

## CHAPTER 16 · cancellation injection은 cleanup과 idempotency race를 드러낸다

Task가 lock 획득 후, file temp 생성 후, remote request 전송 후 등 여러 지점에서 cancel될 수 있다. Cancellation handler가 resource를 release하고 partially committed state를 rollback/mark하는지 확인한다. Completion과 cancel이 동시에 race할 때 둘 중 하나만 terminal transition을 소유해야 한다. P41 task-state contract를 failpoint로 검증한다.

## CHAPTER 17 · OOM killer는 allocation error를 application에 전달하지 않고 process를 제거할 수 있다

Memory pressure가 심하면 kernel/cgroup policy가 process를 kill해 user-space recovery code를 실행할 기회를 주지 않을 수 있다. Persistent state는 abrupt death를 견뎌야 하고 supervisor가 restart loop를 제어해야 한다. Cgroup memory limit을 작은 isolated test process에 적용해 kill/restart behavior를 검증할 수 있다. Host 전체 memory를 고갈시키는 방식은 안전하지 않다.

## CHAPTER 18 · CPU starvation은 timeout을 dependency failure처럼 보이게 만들 수 있다

Process가 runnable인데 CPU quota/throttling, high-priority competitor 때문에 실행되지 못하면 timer callback과 heartbeat가 늦어진다. Fault test는 cgroup quota 또는 controlled busy worker로 scheduling delay를 만들고 application이 network/storage failure로 오진하지 않는지 본다. Queue/wakeup latency metric이 있어야 원인을 분리할 수 있다. Timeout budget에 local scheduling delay가 포함된다는 사실을 검증한다.

## CHAPTER 19 · lock contention injection은 rare critical-section expansion을 재현한다

특정 thread를 lock 보유 상태에서 의도적으로 pause하면 waiter queue, priority inversion, timeout path를 관찰할 수 있다. Production code에 sleep을 넣는 대신 test hook/barrier로 exact point를 제어한다. Lock owner가 cancel/crash했을 때 primitive가 자동 release되는지, robust mutex/recovery semantics가 필요한지 확인한다. Deadlock detector는 단순 timeout과 cycle을 구분해야 한다.

## CHAPTER 20 · signal interruption은 blocking syscall error handling을 검증한다

Signal을 특정 blocking read/wait 시점에 전달해 EINTR, automatic restart, partial result handling이 올바른지 확인할 수 있다. Caller가 error만 보고 buffer offset을 초기화하면 duplicate/corrupt data가 생길 수 있다. Test는 small/large request와 signal handler policy를 나눠 수행한다. Signal injection은 handler async-signal-safety도 동시에 검증할 수 있다.

## CHAPTER 21 · file-descriptor exhaustion은 정상 open 실패와 cleanup leak을 함께 드러낸다

Process RLIMIT_NOFILE을 작게 설정해 socket/file open이 EMFILE에 도달하도록 만들면 error path가 기존 descriptor를 정리하는지 확인할 수 있다. Retry loop가 busy-spin하거나 log file까지 열지 못해 diagnostic이 사라질 수 있다. Reserved emergency descriptor pattern이 필요한 system도 있다. Test 종료 후 fd count가 baseline으로 돌아오는지 leak assertion을 둔다.

## CHAPTER 22 · permission change는 long-lived assumption을 깨뜨린다

Startup에서 file access가 성공했어도 credential, mount mode, policy가 runtime에 바뀌면 후속 operation이 EACCES/EPERM으로 실패할 수 있다. Application이 permission error를 corruption/absence로 오인해 data를 overwrite하지 않는지 확인한다. Android/SELinux 같은 MAC denial은 Unix mode bit와 다른 evidence를 남긴다. Fault test는 expected denial을 명확히 분류한다.

## CHAPTER 23 · configuration corruption은 parser와 fallback policy를 동시에 검증한다

Config file truncation, invalid enum, incompatible version, duplicate field를 주입해 startup이 fail-closed하는지 또는 validated default로 복구하는지 확인한다. `파싱 실패면 빈 config`처럼 silent fallback하면 security/availability invariant가 깨질 수 있다. Last-known-good snapshot을 사용하는 경우 rollback provenance를 기록한다. Corrupt config를 자동으로 overwrite하면 forensic evidence를 잃을 수 있다.

## CHAPTER 24 · upgrade/restart fault는 old/new schema와 binary 호환성을 검증한다

Deployment 중 process가 일부 instance만 새 version인 상태, migration 중 crash, rollback 후 old binary가 new data를 읽는 상황을 재현한다. Versioned file/DB/protocol은 forward/backward compatibility window를 명시해야 한다. Migration은 idempotent하거나 progress marker를 가져 재시작 가능해야 한다. Build success와 upgrade safety는 별개 gate다.

## CHAPTER 25 · chaos test는 deterministic component test를 대체하지 않는다

Production-like environment에서 random instance kill, resource degradation을 주입하면 emergent behavior를 찾을 수 있지만 원인 재현과 coverage 측정이 어렵다. 먼저 component failpoint로 known invariant를 검증하고, integration/staging chaos로 상호작용을 확인한다. Production chaos는 blast radius·abort condition·승인 절차가 필요하다. 무작위 장애를 많이 넣었다는 사실 자체가 reliability 증거는 아니다.

## CHAPTER 26 · blast radius를 제한하지 않은 fault injection은 테스트가 아니라 사고가 될 수 있다

Disk fill, packet drop, CPU burn, kill fault는 shared environment의 다른 workload에 영향을 줄 수 있다. Namespace/cgroup/test account와 explicit target selector로 scope를 격리한다. Time limit과 automatic cleanup/rollback을 둔다. 운영환경·외부시스템 변경을 수반하는 실제 chaos 실행은 사전 승인과 안전장치가 필요하다. Local simulation과 production fault injection 결과를 같은 PASS로 부르지 않는다.

## CHAPTER 27 · observability가 없으면 injected fault가 실제로 발생했는지 증명할 수 없다

Failpoint trigger count, fault location, injected errno/delay, process kill timestamp를 test artifact에 남긴다. Application log/metric/trace가 expected recovery path를 통과했는지 correlation id로 연결한다. Fault가 trigger되지 않았는데 test가 정상 완료되면 PASS가 아니라 invalid test다. Injection mechanism 자체를 assertion해서 false PASS를 막는다.

## CHAPTER 28 · invariant oracle은 최종 화면보다 내부 state를 검증한다

Crash 후 UI가 열리는지만 보면 lost record, duplicate counter, leaked file을 놓칠 수 있다. Domain invariant, checksum, reference count, transaction state, fd/memory count를 검사하는 oracle을 만든다. Failure가 허용하는 결과 집합을 명시해 old/new state 둘 다 valid한 경우 false failure를 피한다. Recovery test는 `예외가 안 났다`가 아니라 invariant가 유지됐음을 증명한다.

## CHAPTER 29 · 발견된 random failure는 seed·schedule·failpoint를 고정한 regression으로 승격한다

Stress/chaos에서 rare bug를 찾았으면 당시 seed, input, thread schedule hint, fault event sequence를 보존한다. 가능한 가장 작은 deterministic reproduction으로 줄여 normal CI/unit suite에 넣는다. Random campaign만 다시 돌려 `이번엔 안 나옴`을 수정 증거로 쓰지 않는다. Regression이 실제 old code에서 실패하고 fix에서 통과하는지 확인한다.

## CHAPTER 30 · fault-testing 계약은 주입 증거·recovery invariant·미검증 범위를 함께 남긴다

각 fault case는 대상 failure model, injection point, 실제 trigger evidence, expected invariant, observed result를 기록한다. Simulated disk error가 physical power-loss를 증명하지 않고 process kill이 kernel crash를 증명하지 않는다. 서로 다른 failure domain을 별도 CLEAN으로 유지한다. Reliability PASS는 실행한 fault에 대해서만 선언하고, 더 강한 fault는 미검증으로 명시한다.