# PART 54 · Crash Atomicity and Recovery — WAL, shadow state, commit records, replay

Persistent state를 바꾸는 operation은 정상 실행 중 invariant뿐 아니라 **임의 instruction·write·flush 사이에서 전원이 꺼졌을 때 남는 prefix state**까지 안전해야 한다. Crash consistency는 `파일이 안 깨짐`보다 강한 문제다. 여러 block/record가 하나의 logical transaction을 이룰 때 old state 또는 new state 중 허용된 결과로만 복구되도록 write ordering, commit marker, checksum, log replay, copy-on-write protocol을 설계해야 한다.

## CHAPTER 01 · atomicity는 logical operation과 physical write 개수가 다를 때 문제로 드러난다

`balance A 감소 + balance B 증가`처럼 한 operation이 여러 persistent location을 바꾸면 storage는 그 둘을 한 번에 원자적으로 쓰지 않을 수 있다. 첫 write 후 crash하면 money가 사라지고 두 번째만 남으면 생긴다. File metadata와 data, index와 row, manifest와 segment도 같은 구조다. Logical atomicity를 얻으려면 physical write sequence 중 어느 prefix가 남아도 recovery가 old/new invariant로 수렴하도록 protocol을 둔다. Application transaction과 device sector atomicity를 같은 것으로 취급하지 않는다.

## CHAPTER 02 · torn write는 한 logical block이 old/new byte가 섞인 상태로 남을 가능성이다

Storage가 특정 write unit의 atomicity를 보장하지 않거나 failure가 controller/media 중간에서 발생하면 page/block 일부만 새 data가 될 수 있다. Filesystem과 DB는 checksum, page LSN, double-write buffer 같은 기법으로 torn/corrupt page를 검출·복구할 수 있다. `write()가 한 번 호출됨`은 media write atomicity 크기를 보장하지 않는다. Hardware/filesystem guarantee를 문서로 확인하고 더 큰 application page를 한 write로 썼다는 사실을 원자성 근거로 쓰지 않는다.

## CHAPTER 03 · write ordering은 crash 후 어떤 prefix 조합이 가능한지를 제한한다

A를 persist한 뒤 B를 persist해야 한다는 protocol에서 CPU/kernel/device가 B를 먼저 media에 반영할 수 있으면 recovery assumption이 깨진다. Program order, kernel dirty-page order, device queue completion order, persistence order는 같은 개념이 아니다. Barrier/flush/FUA/fsync 같은 mechanism은 특정 계층의 ordering/durability를 강화한다. Protocol proof는 `A persist-before B` 같은 relation을 명시하고 각 relation을 어떤 primitive가 보장하는지 매핑해야 한다.

## CHAPTER 04 · commit record는 `변경이 완전히 준비됨`을 작은 durable fact로 압축한다

Large transaction의 모든 data page를 먼저 준비·persist한 뒤 작은 commit marker를 마지막에 durable하게 만들면 recovery는 marker 존재 여부로 old/new 선택을 할 수 있다. Commit record 자체가 torn/corrupt되지 않는 보장 또는 checksum/duplicate strategy가 필요하다. Marker를 쓰기 전에 모든 dependency가 stable해야 하고 marker 이후 cleanup은 crash해도 semantic result를 바꾸지 않아야 한다. Commit point를 명확하게 정의하면 acknowledgement와 durability의 경계를 설명할 수 있다.

## CHAPTER 05 · WAL은 data page보다 redo/undo information을 먼저 durable하게 만든다

Write-Ahead Logging의 핵심은 modified data page가 persistent state에 나타나기 전에 그 변경을 복구할 충분한 log가 stable해야 한다는 순서다. Crash 후 log를 replay해 committed change를 redo하거나 uncommitted change를 undo할 수 있다. Log record에 transaction id, page/record identity, before/after info, sequence/LSN을 어떻게 담는지는 system마다 다르다. WAL file이 존재한다고 write-ahead rule이 지켜지는 것은 아니며 page flush와 log flush ordering이 실제 invariant다.

## CHAPTER 06 · redo logging은 stable old page와 committed log에서 new state를 재구성한다

Redo-oriented protocol은 data page가 아직 old여도 committed log record를 다시 적용해 new state로 만들 수 있다. Replay operation은 같은 record를 여러 번 적용해도 결과가 변하지 않도록 page LSN/version으로 중복 적용을 막거나 idempotent하게 설계한다. Recovery 중 다시 crash할 수 있으므로 recovery 자체도 restartable해야 한다. Log replay 성공 후 checkpoint/truncation을 언제 해도 안전한지 separate invariant가 필요하다.

## CHAPTER 07 · undo logging은 uncommitted modification을 old state로 되돌릴 정보를 보존한다

Data page를 commit 전에 persistent하게 쓸 수 있는 steal policy에서는 uncommitted value가 media에 남을 수 있어 before-image 또는 inverse operation이 필요하다. Crash recovery는 transaction commit status를 보고 uncommitted change를 undo한다. Undo record가 data write보다 먼저 durable하지 않으면 old value를 잃는다. Logical undo가 side-effect-free인지, index/secondary structure까지 되돌릴 수 있는지 확인한다. External side effect는 database undo만으로 되돌릴 수 없다.

## CHAPTER 08 · ARIES류 recovery는 analysis·redo·undo를 분리해 complex buffer policy를 다룬다

Buffer manager가 dirty page를 자유롭게 flush하고 committed page를 즉시 쓰지 않는 steal/no-force policy를 사용하면 recovery는 어떤 transaction/page가 어느 상태였는지 reconstruct해야 한다. Analysis phase로 dirty page/transaction table을 복원하고 redo로 history를 repeat한 뒤 loser transaction을 undo하는 구조가 가능하다. Compensation log record는 undo 자체가 crash 후 재개 가능하도록 progress를 기록한다. 이름 암기보다 buffer policy가 recovery algorithm을 왜 복잡하게 만드는지 연결한다.

## CHAPTER 09 · checkpoint는 log 전체 replay 비용을 줄이되 recovery truth를 잃지 않아야 한다

Checkpoint가 현재 dirty page와 transaction state를 기록하면 recovery가 log beginning부터 읽지 않고 더 최근 위치에서 시작할 수 있다. Checkpoint 생성 중에도 transaction과 page write가 계속될 수 있어 fuzzy checkpoint는 완전 stop-the-world snapshot이 아니다. Checkpoint record와 page LSN이 함께 recovery start point를 결정한다. Log truncation은 checkpoint가 필요한 history를 완전히 대체한 뒤에만 가능하다.

## CHAPTER 10 · log truncation은 disk-space cleanup이 아니라 recovery horizon 변경이다

Old WAL segment를 삭제하면 그 시점 이전 state를 redo/replication/backup recovery에 사용할 수 없게 된다. Standby replica, incremental backup, snapshot이 old LSN을 요구하면 truncation을 늦춰야 한다. Retention leak은 disk full을 만들고 aggressive deletion은 recovery chain을 끊는다. Minimum required LSN을 여러 consumer 중 최솟값으로 계산하고 consumer progress를 observability에 노출한다.

## CHAPTER 11 · shadow paging은 old page를 덮어쓰지 않고 새 page tree를 만든다

Copy-on-write persistent structure는 modification을 새 block/page에 기록하고 모든 child가 준비된 뒤 root pointer를 atomically 새 tree로 전환한다. Crash가 root switch 전에 나면 old tree, 이후면 new tree를 사용한다. In-place undo log 없이 atomicity를 얻을 수 있지만 unchanged page reference 관리, space reclamation, fragmentation이 비용이다. Root/metadata update의 atomicity와 durability가 전체 protocol의 commit point가 된다.

## CHAPTER 12 · copy-on-write filesystem은 block graph versioning과 free-space accounting을 함께 해결해야 한다

새 extent/tree node를 만들고 parent pointer를 새 version으로 연결하면 old block을 보존할 수 있다. 그러나 crash 후 어느 block이 reachable인지, allocation metadata가 double allocate되지 않는지, old snapshot이 reference하는 block을 언제 free할지가 복잡해진다. Reference count/space map도 자체 crash-consistency protocol이 필요하다. `데이터를 덮어쓰지 않는다`만으로 metadata corruption이 사라지는 것은 아니다.

## CHAPTER 13 · double-write는 torn page를 detection+replacement 가능한 duplicate로 바꾼다

Main location에 large page를 쓰기 전에 별도 contiguous/stable area에 page copy를 먼저 durable하게 쓰고, main write가 crash로 torn되면 recovery가 double-write copy에서 복원할 수 있다. 추가 write bandwidth와 space를 비용으로 내고 hardware atomic-write unit보다 큰 page를 보호한다. Double-write area 자체의 record boundary, checksum, generation을 검증해야 wrong page를 복원하지 않는다.

## CHAPTER 14 · checksum은 partial/corrupt state를 검출하지만 어느 copy가 최신인지 알려 주지 않는다

두 page copy 모두 checksum이 valid해도 하나는 old generation이고 하나는 new generation일 수 있다. Version/LSN/generation counter를 함께 저장해 recency를 판단해야 한다. Checksum field 자체가 data와 같은 failure domain에 있어 attacker protection도 제공하지 않는다. Recovery는 checksum valid + identity/version valid를 모두 확인한다. Silent corruption과 crash partial write를 같은 detector로 잡을 수 있어도 repair policy는 다를 수 있다.

## CHAPTER 15 · generation counter는 ABA와 stale metadata를 persistent state에서도 구분한다

Slot/page id가 재사용되면 old pointer가 같은 physical location을 다시 가리키며 stale reference가 새 object를 valid하게 보일 수 있다. Generation을 identity에 포함하면 recovery가 old reference를 거부할 수 있다. Counter wraparound 가능성과 serialized width를 고려한다. Free-list/allocator metadata에서도 page number만으로 ownership을 판단하지 않고 epoch/generation과 commit state를 함께 둔다.

## CHAPTER 16 · manifest/index 파일은 immutable segment 집합의 commit pointer 역할을 할 수 있다

LSM/archive/search index는 data segment를 immutable하게 생성한 뒤 작은 manifest에 현재 active segment list를 기록할 수 있다. 새 segment가 완전히 durable하기 전 manifest가 참조하면 crash 후 missing data가 되고, old segment를 manifest switch 전에 삭제하면 rollback이 불가능하다. Sequence는 create→sync segment→write/sync new manifest→atomic replace→sync directory→old cleanup처럼 구성될 수 있다. Cleanup은 commit 이후 crash해도 correctness가 유지되어야 한다.

## CHAPTER 17 · atomic rename은 namespace 전환에 유용하지만 content durability까지 포함하지 않는다

동일 filesystem 안에서 rename이 atomic namespace switch를 제공해 reader가 old/new filename 중 하나만 보게 할 수 있다. 그러나 temp file content가 stable하지 않거나 directory update가 crash 후 사라질 수 있으면 persistent commit은 완성되지 않는다. Rename semantic과 fsync ordering을 filesystem contract로 확인한다. Cross-filesystem rename은 같은 guarantee를 갖지 않을 수 있어 staging path를 target filesystem 안에 둔다.

## CHAPTER 18 · directory durability는 file content durability와 별도 metadata commit이다

새 file을 create하거나 rename으로 name→inode mapping을 바꾸면 directory metadata가 persistent해야 crash 후 name이 존재한다. File 자체를 fsync해도 containing directory entry가 durable하다는 보장이 별도일 수 있다. Atomic-replace protocol에서 directory fsync를 빠뜨리면 test machine에서 대부분 성공해도 sudden power loss에서 target name이 old/missing 상태가 될 수 있다. Platform guarantee와 required durability level을 명확히 한다.

## CHAPTER 19 · append log도 sector boundary와 length prefix 때문에 torn tail을 처리해야 한다

Log record를 sequential append하면 overwrite보다 단순하지만 crash가 record 중간에 나면 tail에 partial header/body가 남을 수 있다. Length + checksum + sequence를 사용해 recovery scanner가 마지막 완전 record까지만 인정하고 partial tail을 truncate할 수 있다. Length field가 corrupt해 huge allocation/seek를 유도하지 않도록 maximum을 검증한다. `EOF까지 읽다가 parse error면 끝` 같은 느슨한 rule은 중간 corruption을 tail로 오인할 수 있다.

## CHAPTER 20 · group commit은 여러 transaction의 durability flush를 하나로 batch한다

각 transaction마다 storage flush를 기다리면 latency와 device command overhead가 크다. 여러 commit record를 log에 append하고 한 번의 fsync/flush로 모두 durable하게 만든 뒤 각 waiter를 깨우면 throughput을 높일 수 있다. Batch wait time이 latency를 추가하므로 workload arrival에 따라 adaptive policy가 필요하다. Acknowledgement는 shared flush가 실제 성공한 뒤에만 나가야 하며 flush failure는 batch의 모든 transaction에 전달된다.

## CHAPTER 21 · asynchronous commit은 acknowledged durability 수준을 명확히 낮춘다

Client에게 commit 성공을 먼저 응답하고 WAL/data flush를 background로 미루면 latency를 줄일 수 있지만 crash 시 이미 성공 응답한 최근 transaction을 잃을 수 있다. 이는 bug가 아니라 선택된 durability contract일 수 있다. API/SLA가 `process crash`와 `power loss`에서 허용하는 data-loss window를 명시해야 한다. Sync/async commit mode를 혼용하면 transaction별 guarantee를 telemetry에 남긴다.

## CHAPTER 22 · idempotent recovery는 같은 log/action을 여러 번 재생해도 안전해야 한다

Recovery 중 다시 crash하면 다음 boot에서 같은 record를 재처리할 수 있다. Page LSN이 already-applied record를 건너뛰거나 operation 자체를 idempotent하게 설계해야 한다. External effect replay는 duplicate email/payment처럼 되돌리기 어려우므로 durable outbox + consumer idempotency key가 필요하다. Recovery algorithm을 `한 번만 실행됨` 가정으로 설계하지 않는다.

## CHAPTER 23 · write-ahead outbox는 database commit과 message publish 사이 gap을 persistent하게 만든다

DB row update와 queue publish를 두 독립 system에 수행하면 하나만 성공할 수 있다. 같은 DB transaction에 outbox record를 함께 commit하고 별도 publisher가 outbox를 읽어 message를 반복 publish하면 DB state와 publish intent의 atomicity를 얻을 수 있다. Consumer는 duplicate publish를 견뎌야 하고 outbox cleanup은 delivery checkpoint 뒤에 이루어진다. Exactly-once라는 표면 목표보다 at-least-once + idempotent processing의 실제 invariant를 정의한다.

## CHAPTER 24 · recovery는 schema/version evolution도 함께 처리해야 한다

Old log/page가 previous binary format인데 new recovery code가 current struct만 이해하면 upgrade 직후 crash에서 복구가 실패할 수 있다. Persistent log record에 version을 두고 support window 동안 old decoder를 유지하거나 upgrade 전에 checkpoint/format migration을 완료한다. Rollback 가능성을 요구하면 new writer가 old reader가 이해 가능한 log를 쓰는 기간이 필요하다. P52 compatibility가 recovery path에도 적용된다.

## CHAPTER 25 · backup은 crash recovery와 다른 failure domain을 보호한다

WAL/replica가 같은 corruption/bug를 그대로 복제하면 application bug에 의한 data 삭제를 되돌릴 수 없다. Backup은 더 긴 retention과 independent storage/failure domain을 제공하며 restore 가능한 snapshot + required log chain을 관리한다. Backup 성공 log만으로 충분하지 않고 정기 restore test가 필요하다. Recovery point objective와 recovery time objective를 log retention/checkpoint 주기와 연결한다.

## CHAPTER 26 · replica acknowledgement는 durability quorum의 의미를 명확히 해야 한다

Distributed store에서 leader local memory에만 쓴 상태, leader disk에 fsync한 상태, follower memory/disk까지 복제된 상태는 failure tolerance가 다르다. `replicated`라는 말이 어느 단계인지 protocol contract를 확인한다. Quorum acknowledgement는 node failure를 견딜 수 있지만 correlated power/storage/network failure와 stale replica selection 문제를 별도 다룬다. DDIA의 replication/consensus 관점과 local crash atomicity를 연결한다.

## CHAPTER 27 · recovery correctness는 invariant와 allowed outcomes를 먼저 정의해야 한다

Crash 후 정확히 new state 하나만 요구할 수도 있고 old/new 둘 중 하나는 허용하지만 hybrid는 금지할 수도 있다. Counter가 중복 증가하지 않음, index와 row가 일치함, allocation block이 두 owner에게 속하지 않음 같은 invariant를 machine-checkable oracle로 만든다. `DB가 열림`이나 `파일 parse됨`은 충분한 recovery proof가 아니다. P49 crash matrix가 이 invariant를 각 prefix에서 검증한다.

## CHAPTER 28 · fault injection은 persistence layer 실제 ordering을 충분히 흉내 내지 못할 수 있다

Application function 사이에서 process를 kill하는 simulation은 kernel/device reorder와 volatile cache loss를 완전히 재현하지 않는다. Filesystem emulator, power-cut hardware, storage fault injection은 더 강한 evidence지만 비용과 운영 위험이 크다. Test 수준별 failure model을 명시하고 weaker simulation 결과를 physical power-loss PASS로 과장하지 않는다. 서로 다른 CLEAN은 실제로 다른 failure type을 실행해야 한다.

## CHAPTER 29 · recovery performance도 availability contract의 일부다

Log가 수 TB 쌓인 뒤 crash recovery가 수시간 걸리면 data는 안전해도 service RTO를 만족하지 못한다. Checkpoint frequency, parallel redo, index rebuild, lazy recovery가 startup time을 바꾼다. Normal operation에서 checkpoint 비용을 아끼면 recovery time이 커지는 trade-off가 있다. Production-size dataset으로 restart/recovery benchmark를 수행하고 progress metric을 제공한다.

## CHAPTER 30 · crash-consistency 계약은 persist-before relation과 recovery algorithm을 함께 증명한다

Persistent update를 설계할 때 logical invariant, physical write set, required ordering, commit point, crash prefix별 allowed state, recovery procedure를 문서화한다. 각 `A persist-before B` relation이 fsync/flush/FUA/transaction 중 어떤 primitive로 보장되는지 연결한다. Recovery는 반복 실행 가능하고 corrupt/torn record를 fail-closed해야 한다. 정상-path test만으로 완료하지 않고 P49 deterministic crash matrix와 실제 storage guarantee 범위에서 검증한다.