# PART 54 · Crash Atomicity and Recovery — WAL, shadow state, commit records, replay

Crash recovery의 목표는 “재시작하면 열린다”가 아니다. Operation 중간 어느 시점에 전원이 끊겨도 **committed state는 보존되고 uncommitted state는 노출되지 않으며, recovery를 반복해도 같은 결과**가 나와야 한다. 이를 위해 write ordering, log, commit record, generation, checksum, rename, checkpoint가 서로 맞물린다. 이 PART는 logical atomicity에서 시작해 WAL·shadow paging·backup·replica durability까지 복구 프로토콜의 경계를 추적한다.

---

## CHAPTER 01 · logical atomicity는 여러 physical write를 하나의 관찰 가능한 변화로 묶는다

Application operation 하나가 data page, index, metadata 여러 곳을 수정해도 외부 reader는 old state 또는 new state 중 하나만 보아야 할 수 있다. Storage는 이 여러 write를 자동으로 하나의 atomic unit으로 만들지 않으므로 log나 copy-on-write 같은 protocol이 필요하다.

Atomicity는 durability와 다르다. Operation이 all-or-nothing으로 보이더라도 crash 후 사라질 수 있고, durable하더라도 partial state가 노출될 수 있다. 두 축을 분리해 설계한다.

Recovery invariant는 구체적으로 적는다. 예를 들어 “commit record가 durable하지 않으면 old generation만 유효하다”처럼 crash 후 선택 규칙이 있어야 test oracle을 만들 수 있다.

## CHAPTER 02 · torn write는 하나의 논리 block이 부분적으로만 바뀌는 failure다

Storage sector나 filesystem block보다 큰 write는 crash 시 앞부분은 새 data, 뒷부분은 old data가 남는 torn state가 될 수 있다. Hardware atomic write unit을 가정하려면 실제 device guarantee를 확인해야 한다. 단순 `write(4096)` 호출 크기가 atomicity를 보장하지 않는다.

Page header에 checksum과 generation을 두면 torn/corrupt page를 탐지할 수 있다. 하지만 탐지만으로 복구할 copy가 생기는 것은 아니므로 log, duplicate page, parity 같은 redundancy가 필요하다.

Fault injection은 record 중간을 잘라 읽는 case를 포함한다. Parser가 일부 field를 그럴듯하게 읽고 진행하지 않도록 integrity check를 먼저 적용한다.

## CHAPTER 03 · write ordering은 crash 후 먼저 보이면 안 되는 state를 제어한다

Data를 쓰고 pointer를 갱신하는 protocol에서 pointer가 먼저 durable해지면 crash 후 미완성 data를 가리킬 수 있다. Source code의 syscall 순서가 device persistence 순서와 같다고 가정하지 않는다. Buffering, filesystem, storage cache가 reorder할 수 있다.

필요한 ordering point에서 fsync/flush를 사용하고 다음 단계로 넘어간다. Ordering barrier 수는 latency와 직접 연결되므로 batching과 group commit이 중요한 최적화가 된다.

Crash matrix에서 각 persistence boundary 전후를 끊어 recovery 결과를 확인한다. 문서상 순서보다 실제 fault test가 최종 증거다.

## CHAPTER 04 · commit record는 ‘이 transaction의 새 state가 완성됐다’를 표시하는 durable marker다

여러 변경을 먼저 기록하고 마지막에 작은 commit marker를 durable하게 쓰면 recovery는 marker 존재 여부로 transaction을 redo하거나 무시할 수 있다. Commit record가 atomic하게 판별 가능하도록 checksum·length·transaction id를 포함할 수 있다.

중요한 것은 commit marker가 data보다 먼저 durable해지지 않게 하는 것이다. Log flush ordering이 여기서 핵심이다.

Application이 success를 반환하는 시점도 commit durability와 맞춰야 한다. Marker가 memory에만 있는 상태에서 success를 반환하면 process crash 후 acknowledged transaction이 사라질 수 있다.

## CHAPTER 05 · WAL은 data page보다 먼저 변경 의도를 durable log에 기록한다

Write-Ahead Logging에서는 data page를 제자리에서 바꾸기 전에 해당 변경을 재현할 log record를 안정적으로 기록한다. Crash 후 data page가 old/new/partial 어느 상태든 log를 기준으로 일관된 state를 재구성할 수 있다.

WAL은 append 성격 덕분에 sequential I/O와 group commit에 유리하지만 log space 관리와 checkpoint가 필요하다. Log 자체도 torn/corrupt record를 처리해야 한다.

LSN과 page generation 같은 ordering metadata를 사용하면 page가 어느 log 위치까지 반영했는지 판단할 수 있다. Recovery가 무작정 전체 log를 재적용하지 않게 한다.

## CHAPTER 06 · redo는 committed 변경을 다시 적용해 missing data page write를 보완한다

Commit된 transaction의 data page가 crash 전에 disk에 내려가지 않았더라도 redo log가 durable하면 recovery가 변경을 다시 적용할 수 있다. Redo operation은 가능하면 idempotent하거나 page LSN 비교로 중복 적용을 막는다.

Redo가 logical operation인지 physical byte change인지에 따라 recovery가 필요한 context가 다르다. Logical redo는 schema/index state에 의존할 수 있고 physical redo는 format version과 page layout에 강하게 묶인다.

Recovery test는 일부 page만 write된 상태에서 redo가 최종 committed image를 만드는지 검증한다. Redo 중 다시 crash한 뒤 재시작하는 case도 포함한다.

## CHAPTER 07 · undo는 아직 commit되지 않은 변경이 data page에 노출된 경우 되돌린다

Steal policy처럼 uncommitted dirty page가 disk에 기록될 수 있으면 crash recovery가 해당 변경을 취소할 정보가 필요하다. Undo log는 이전 값이나 inverse operation을 보존해 rollback을 가능하게 한다.

Undo 자체가 crash 중 중단될 수 있으므로 recovery를 반복해도 안전해야 한다. Compensation record 같은 mechanism은 “이 undo가 이미 수행됐다”는 정보를 남길 수 있다.

Transaction abort path와 crash recovery path가 같은 undo logic을 공유하더라도 failure model은 다르다. Recovery에는 application object와 thread state가 없다는 점을 고려한다.

## CHAPTER 08 · ARIES류 recovery는 analysis·redo·undo를 분리해 복잡한 WAL 상태를 재구성한다

전형적인 recovery algorithm은 먼저 log를 분석해 dirty page와 active transaction을 찾고, 필요한 변경을 redo한 뒤 loser transaction을 undo한다. 핵심은 crash 당시 buffer pool의 정확한 memory state가 사라졌어도 log metadata만으로 안전한 시작점을 계산하는 것이다.

Checkpoint는 analysis 범위를 줄이지만 crash 순간 checkpoint 자체가 완성되지 않았을 수 있다. Recovery가 incomplete checkpoint를 구분해야 한다.

알고리즘을 구현할 때 단계 이름을 복제하는 것보다 각 record type의 invariant와 LSN ordering을 정확히 지키는 것이 중요하다.

## CHAPTER 09 · checkpoint는 log 전체를 매번 재생하지 않도록 recovery 시작점을 앞당긴다

System이 오래 실행되면 WAL이 계속 커져 startup recovery 시간이 늘 수 있다. Checkpoint는 어느 시점까지의 dirty state와 transaction 정보를 정리해 그 이전 log에 대한 의존성을 줄인다.

Checkpoint를 위해 모든 writer를 완전히 멈출 필요는 없다. Fuzzy checkpoint는 동시 변경을 허용하되 recovery가 필요한 metadata를 기록한다. 대신 reasoning이 더 복잡해진다.

Checkpoint frequency는 normal I/O overhead, log space, recovery time objective의 trade-off다. 평균 throughput만 보고 너무 드물게 만들지 않는다.

## CHAPTER 10 · log truncation은 더 이상 recovery에 필요하지 않은 prefix만 제거해야 한다

Checkpoint가 있다고 바로 이전 WAL을 삭제할 수 있는 것은 아니다. Replica, backup, long transaction이 오래된 log를 아직 필요로 할 수 있다. 가장 느린 consumer와 recovery horizon을 기준으로 안전한 truncation point를 계산한다.

잘못된 truncation은 평상시에는 드러나지 않다가 특정 crash 또는 restore에서 치명적으로 나타난다. “디스크가 찼다”는 이유로 오래된 log를 임의 삭제하면 안 된다.

Retention metric에 oldest required LSN과 consumer별 lag을 포함한다. 누가 log reclamation을 막는지 알 수 있어야 한다.

## CHAPTER 11 · shadow paging은 기존 page를 덮지 않고 새 copy와 root 전환으로 commit한다

변경된 page를 새 위치에 쓰고 마지막에 root pointer를 새 tree/generation으로 원자적으로 전환하면 uncommitted state가 old root를 손상시키지 않는다. Commit 전 crash면 old root를, 이후 crash면 new root를 선택한다.

장점은 undo log가 필요 없을 수 있다는 점이지만 page copy와 fragmentation, garbage collection 비용이 생긴다. Root metadata 자체의 torn write도 보호해야 한다.

Recovery는 여러 root copy의 generation과 checksum을 비교해 최신 valid one을 선택할 수 있다. 단순 timestamp보다 monotonic generation이 안전하다.

## CHAPTER 12 · copy-on-write filesystem도 old/new tree를 유지하지만 application transaction과 동일하지 않다

CoW filesystem은 metadata/data block을 새 위치에 쓰고 tree pointer를 갱신해 crash consistency를 제공할 수 있다. 하지만 application이 여러 file에 걸친 business transaction을 수행할 때 그 전체를 하나의 atomic commit으로 묶어준다고 가정하면 안 된다.

Filesystem atomicity unit과 application invariant는 계층이 다르다. Database는 CoW filesystem 위에서도 자체 WAL을 사용할 수 있다.

Storage stack의 lower-layer guarantee를 활용하되 중복되지 않는 application-level requirement를 별도로 정의한다.

## CHAPTER 13 · double-write는 torn page를 복구할 두 번째 copy를 먼저 확보한다

Data page를 final location에 덮기 전에 별도 double-write area에 완전한 copy를 durable하게 기록하면 crash 후 final page가 torn되었을 때 복구 source를 얻을 수 있다. 이는 checksum이 탐지만 제공하는 한계를 보완한다.

추가 write 때문에 amplification이 생기므로 hardware atomic write나 filesystem feature와의 관계를 평가한다. 하지만 실제 failure model을 제거하지 않은 채 성능 이유로 생략하면 corruption risk가 돌아온다.

Recovery는 어느 copy가 더 최신이며 valid한지 generation/LSN으로 판정해야 한다. 두 copy 모두 존재한다는 사실만으로 충분하지 않다.

## CHAPTER 14 · checksum은 corruption 검출과 format version validation을 함께 돕는다

Page/record checksum은 torn write와 bit corruption을 찾는 데 유용하다. Version과 generation을 checksum 범위에 포함하면 header 일부가 잘못 해석되는 위험도 줄일 수 있다.

Checksum algorithm을 바꾸는 format evolution에서는 reader가 어느 algorithm을 적용할지 version metadata가 필요하다. Unknown checksum type을 단순 skip하면 integrity guarantee가 약해진다.

Recovery 과정에서도 checksum failure를 무시하지 않는다. 가능한 replica/backup copy로 복구하거나 명시적 corruption error를 내야 한다.

## CHAPTER 15 · generation number는 old valid copy와 new valid copy의 순서를 결정한다

CoW root, manifest, snapshot이 여러 copy로 남아 있을 때 checksum만으로는 어느 것이 최신인지 알 수 없다. Monotonic generation을 함께 기록하면 crash 후 가장 높은 valid generation을 선택할 수 있다.

Generation wraparound와 duplicate write를 고려해 충분한 width와 비교 규칙을 정한다. Distributed writer가 여러 개라면 단순 local counter보다 stronger coordination이 필요할 수 있다.

Recovery test는 new generation header만 write되고 payload가 invalid한 case에서 old valid generation으로 fallback하는지 확인한다.

## CHAPTER 16 · manifest는 여러 data file의 현재 generation을 작은 metadata로 묶을 수 있다

LSM, archive, dataset format은 data file을 immutable하게 만들고 작은 manifest가 현재 active set을 가리키도록 설계할 수 있다. 새 data를 모두 durable하게 만든 뒤 manifest를 원자적으로 교체하면 commit point가 작아진다.

Manifest 자체가 손상될 수 있으므로 checksum, previous copy, generation을 둔다. Reference된 file이 실제 존재하고 hash가 맞는지 startup validation이 필요하다.

Garbage collection은 old manifest가 더 이상 참조하지 않는 file만 제거해야 한다. Reader가 old generation을 사용 중인지 lifetime을 고려한다.

## CHAPTER 17 · atomic rename은 namespace publish에는 강력하지만 file content durability 전체를 대신하지 않는다

같은 filesystem 안에서 rename이 atomic하게 보일 수 있어 temp file을 final name으로 publish하는 데 유용하다. Reader는 old 또는 new path target 중 하나를 본다. 그러나 temp file content가 stable storage에 있는지와 directory entry가 power loss 후 남는지는 별도 sync가 필요할 수 있다.

`write temp → fsync temp → rename → fsync directory` 같은 pattern은 이 계층 차이를 반영한다. 정확한 필요 단계는 filesystem semantics를 확인한다.

Crash test에서 rename 직후 power loss를 넣어 이름과 내용이 함께 기대대로 복구되는지 검증한다.

## CHAPTER 18 · directory durability는 file data durability와 다른 metadata 경계다

새 file을 생성하거나 rename하면 directory entry가 바뀐다. File 자체를 fsync해도 directory metadata가 crash 후 보존된다는 보장이 자동으로 따라오지 않을 수 있다. Application이 파일 존재 자체를 commit indicator로 사용한다면 중요하다.

Directory fsync 지원과 semantics는 filesystem/platform마다 차이가 있을 수 있다. Portable code는 지원 범위를 명시한다.

Fault test는 “파일 내용은 device에 있지만 이름이 old state”인 case를 고려한다. Recovery가 orphan temp를 어떻게 처리할지도 정한다.

## CHAPTER 19 · log tail은 crash로 잘린 마지막 record를 정상적으로 무시할 수 있어야 한다

Append 중 crash하면 마지막 record의 header만 있거나 payload 일부만 있을 수 있다. Recovery scanner는 length, checksum, sequence를 이용해 마지막 완전한 record까지 읽고 이후 tail을 버릴 수 있어야 한다.

Malformed length가 log file 밖을 가리키면 parser가 거대한 allocation을 하지 않게 range-check한다. Tail corruption과 중간 corruption은 의미가 다를 수 있다. 중간 record 손상은 이후 log 신뢰성을 더 크게 깨뜨린다.

Replay 후 안전한 offset에서 log를 truncate/rollover할 수 있다. Partial record를 다음 append와 이어 붙이지 않는다.

## CHAPTER 20 · group commit은 여러 transaction이 하나의 flush 비용을 공유한다

Storage flush는 비쌀 수 있으므로 여러 transaction의 log record를 모아 한 번의 durable flush로 commit하면 throughput이 크게 늘 수 있다. 대신 첫 transaction이 batch가 채워질 때까지 기다려 latency가 증가할 수 있다.

Batch size와 max wait time을 함께 설정해 throughput과 tail latency를 조절한다. Load가 낮을 때도 지나치게 기다리지 않게 한다.

Commit acknowledgement는 해당 transaction record가 포함된 flush 완료 후에만 보낸다. Queue에 들어갔다는 사실과 durable group commit을 구분한다.

## CHAPTER 21 · async commit은 latency를 줄이는 대신 최근 acknowledged data 손실 가능성을 받아들일 수 있다

일부 system은 transaction을 logical commit으로 처리하고 WAL flush를 뒤로 미뤄 response를 빠르게 할 수 있다. Process crash는 견딜 수 있어도 host power loss에서는 최근 commit이 사라질 수 있다. 이 trade-off를 사용자가 이해할 수 있는 durability tier로 표현해야 한다.

모든 data에 같은 policy를 적용할 필요는 없다. Telemetry와 financial transaction의 durability 요구가 다를 수 있다.

Metric에는 durable LSN과 acknowledged LSN gap을 두어 risk window를 관측한다. Async라는 이름 뒤에 데이터 손실 범위를 숨기지 않는다.

## CHAPTER 22 · idempotent recovery는 recovery 중 다시 crash해도 같은 절차를 반복할 수 있게 한다

Recovery 자체도 완벽한 환경에서 실행되는 것이 아니다. Redo 중 host가 다시 꺼질 수 있으므로 다음 부팅에서 같은 log를 다시 적용해도 state가 더 망가지지 않아야 한다. Page LSN, operation id, compensation record가 이를 돕는다.

Recovery가 side effect로 외부 message를 보내면 중복 발생 위험이 있다. Persistent state 복구와 external effect replay를 분리하거나 idempotency key를 사용한다.

Test는 `crash → recovery 절반 → crash → recovery` sequence를 포함한다. 한 번의 clean restart만으로 idempotence를 증명하지 않는다.

## CHAPTER 23 · transactional outbox는 local commit과 message publish 사이의 gap을 줄인다

Database state를 commit한 뒤 message broker publish가 실패하면 두 system이 불일치한다. Outbox pattern은 business change와 “보낼 message” record를 같은 local transaction에 저장하고 별도 publisher가 idempotently 전달한다.

Publisher crash로 같은 event를 다시 보낼 수 있으므로 consumer deduplication 또는 idempotent handling이 필요하다. Outbox는 exactly-once network를 만드는 것이 아니라 ambiguity를 durable queue로 바꾼다.

Backlog age와 publish retry를 metric으로 둔다. Database commit은 성공했는데 외부 effect가 오래 지연되는 상태를 관측할 수 있어야 한다.

## CHAPTER 24 · recovery format도 versioning이 필요하다

새 software가 WAL record나 manifest format을 바꾸면 crash 직전 old version이 쓴 log를 new version이 읽어야 할 수 있다. Rolling upgrade 중에는 반대 방향 rollback도 고려해야 한다.

Record에 version을 두고 old/new parser compatibility를 테스트한다. Recovery code는 평소 실행 빈도가 낮아 version regression이 숨어 있기 쉽다.

Upgrade 전 checkpoint/log drain을 요구하는 전략도 가능하지만 operation cost와 failure mode를 명확히 한다.

## CHAPTER 25 · backup은 live state copy가 아니라 recoverable point를 만들어야 한다

Database file을 실행 중 단순 복사하면 여러 page가 서로 다른 시점의 state를 담을 수 있다. Snapshot, backup API, WAL position을 이용해 consistent point를 만든다. Backup 완료 후 restore test가 실제 품질을 결정한다.

Backup artifact에는 schema/version, base snapshot, 필요한 log range를 함께 기록한다. Log retention이 backup보다 먼저 잘리면 복구가 불가능해질 수 있다.

정기적으로 isolated environment에서 restore하고 application invariant를 검사한다. “백업 파일이 존재한다”는 사실을 recovery PASS로 보지 않는다.

## CHAPTER 26 · replica durability는 commit acknowledgement가 몇 copy의 어떤 상태를 의미하는지 정의한다

Leader local flush 후 응답하는지, replica memory에 도착해야 하는지, replica disk flush까지 기다리는지에 따라 failover 시 데이터 손실 가능성이 달라진다. Replica count만으로 durability를 말할 수 없다.

Synchronous replication은 latency를 늘리고 availability와 trade-off가 있다. Network partition 시 commit을 멈출지 degraded mode를 허용할지 policy를 정한다.

Metric에 quorum commit index와 replica durable index를 구분한다. Failover test로 acknowledged data가 실제 남는지 확인한다.

## CHAPTER 27 · recovery invariant는 구조적 consistency와 business consistency를 모두 포함한다

B-tree pointer가 모두 valid해도 account balance 총합 같은 business invariant가 깨질 수 있다. Storage recovery는 page-level integrity를 보장하고 application transaction은 domain invariant를 보장해야 한다.

Automated checker를 startup 또는 offline tool로 제공하면 fault test oracle로 사용할 수 있다. Full scan이 비싸면 sampling과 background validation을 조합한다.

Corruption을 발견했을 때 자동 repair가 더 위험할 수 있다. 증거를 보존하고 어떤 invariant를 복구할 수 있는지 구분한다.

## CHAPTER 28 · recovery fault model은 어떤 계층까지 동시에 실패하는지 명시해야 한다

Process crash, kernel crash, power loss, device write loss, bit corruption은 서로 다른 recovery path를 요구한다. “crash-safe”라는 표현만으로는 어떤 model인지 알 수 없다.

Test environment가 실제로 제거하는 state 범위를 문서화한다. VM process kill은 physical power-loss cache를 재현하지 않을 수 있다.

Guarantee 문서와 test model이 일치해야 한다. 검증하지 않은 더 강한 durability를 marketing 문구처럼 주장하지 않는다.

## CHAPTER 29 · recovery performance는 장애 후 service availability를 결정한다

정확한 recovery라도 WAL replay에 몇 시간이 걸리면 RTO를 만족하지 못할 수 있다. Checkpoint frequency, parallel redo, log volume이 restart 시간을 결정한다.

Recovery benchmark는 clean startup과 분리해 측정한다. Dirty page 수, log size, number of files를 실제 worst-case에 가깝게 만든다.

Progress metric을 제공해 watchdog가 “hang”으로 오인해 recovery process를 반복 재시작하지 않게 한다. Recovery loop가 스스로 availability를 악화시키는 경우를 막는다.

## CHAPTER 30 · crash-recovery contract는 commit point와 replay rule을 한 세트로 정의한다

안전한 persistent system은 `어느 순간부터 commit인가`, `그 이전 crash는 무엇을 버리는가`, `그 이후 crash는 무엇을 반드시 복원하는가`를 명확히 한다. WAL, CoW, rename은 이 계약을 구현하는 서로 다른 도구다.

Ordering·checksum·generation·fsync가 맞물려야 하고 recovery 자체도 idempotent해야 한다. Backup과 replica도 같은 commit 의미를 다른 copy로 확장한 것이다.

최종 검증은 단계별 crash matrix와 invariant checker로 수행한다. 정상 종료 후 file이 열리는지 확인하는 것만으로 crash atomicity나 durability를 PASS라고 부를 수 없다.
