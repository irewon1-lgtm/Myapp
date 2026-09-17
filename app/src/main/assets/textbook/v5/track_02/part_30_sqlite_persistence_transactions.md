# PART 30 · SQLite persistence와 transaction — 메모리 상태를 영구 데이터 계약으로 바꾸기

메모리 안의 list와 dict는 process가 끝나면 사라진다. 영구 저장을 시작하면 값의 타입뿐 아니라 schema, transaction, 동시 writer, crash recovery, migration이라는 새로운 계약이 생긴다. SQLite는 하나의 파일 안에 relational database engine을 제공해 작은 프로그램에서도 이 문제를 실제로 경험하게 한다. 핵심은 SQL 문법을 많이 외우는 것이 아니라 **어떤 상태 변경을 하나의 원자적 단위로 묶고, 실패 뒤 어떤 데이터가 남아야 하는지**를 설계하는 것이다.

---

## CHAPTER 01 · persistence는 객체를 저장하는 일이 아니라 장기 schema를 만드는 일이다

Python object는 class field와 reference를 자유롭게 바꿀 수 있지만 database row는 시간이 지나도 이전 version의 application이 만든 데이터를 보존한다. `Task` 객체를 저장할 때 object 전체를 그대로 dump하기보다 ID, title, status, timestamps처럼 장기적으로 의미가 있는 field를 table schema로 선택한다.

Schema에는 type만 아니라 `NOT NULL`, `UNIQUE`, foreign key 같은 invariant를 둘 수 있다. Application validation만 믿으면 다른 import script나 bug가 잘못된 row를 넣을 수 있지만 database constraint는 저장 경계에서 마지막 방어선이 된다. 반대로 모든 업무 정책을 constraint 하나에 억지로 넣으면 migration과 error message가 어려워질 수 있으므로 책임을 나눈다.

저장 model과 domain model을 분리하면 내부 class를 리팩터링해도 schema를 즉시 바꿀 필요가 없다. Repository adapter가 row와 domain object 사이를 변환한다. 이 mapping이 persistence boundary다.

Database schema는 code보다 lifetime이 길 수 있다. Column 이름 하나를 바꾸는 것도 기존 data migration과 rollback 계획을 요구한다는 점이 메모리 객체와 가장 큰 차이다.

---

## CHAPTER 02 · connection은 파일 handle처럼 lifetime과 ownership을 가진 resource다

Database connection은 단순 함수 호출이 아니라 transaction state, lock, cache를 가진 resource다. 요청마다 connection을 열고 닫을지, process 동안 재사용할지, pool을 사용할지는 workload에 따라 달라진다. SQLite에서는 file access와 thread/process model도 함께 고려해야 한다.

Connection을 global mutable singleton으로 숨기면 test isolation과 transaction boundary가 불명확해질 수 있다. Repository나 unit-of-work가 connection을 명시적으로 소유하고 context manager로 lifetime을 제한하면 누가 commit/rollback을 책임지는지 보인다.

Exception이 발생했을 때 cursor와 transaction을 정리해야 하므로 `try/finally`와 context manager가 중요하다. Garbage collection이 언젠가 connection을 닫아 주겠지라는 기대에 durability를 맡기지 않는다.

Read-only 작업과 write 작업의 lifetime도 다를 수 있다. 긴 read transaction이 writer와 lock behavior에 영향을 줄 수 있으므로 결과 전체를 오래 iterator로 노출할 때 connection이 얼마나 오래 유지되는지 확인한다.

---

## CHAPTER 03 · parameter binding은 SQL 문자열 조립과 데이터 전달을 분리한다

사용자 입력을 `"SELECT ... WHERE name='" + value + "'"`처럼 SQL 문자열에 직접 합치면 quote와 SQL syntax가 data가 아니라 code로 해석될 수 있다. Parameterized query는 SQL 구조와 값 전달을 분리해 injection 위험을 줄이고 type conversion도 driver가 담당하게 한다.

Placeholder syntax는 DB API와 driver마다 다를 수 있으므로 직접 quote 함수를 만들지 않는다. Table/column 이름처럼 SQL identifier는 일반 value parameter로 bind할 수 없는 경우가 많다. 동적 identifier가 필요하다면 허용 목록에서 선택한다.

Parameterization은 authorization을 대신하지 않는다. 안전하게 `user_id=123`을 query에 넣어도 현재 사용자가 그 row를 읽을 권한이 있는지는 별도 정책이다. Injection 방지와 object authorization은 다른 failure class다.

Log에도 전체 SQL과 secret parameter를 무조건 남기지 않는다. Query template, duration, row count와 안전한 identifier 중심으로 관측한다.

---

## CHAPTER 04 · transaction은 여러 statement를 하나의 상태 전이로 묶는다

계좌 A 차감과 B 증가처럼 둘이 함께 성공해야 invariant가 유지되는 작업은 하나의 transaction에 묶는다. 중간 statement가 실패하면 rollback해 이전 일관된 상태로 돌아가고, 모두 성공하면 commit해 외부에서 결과가 보이게 한다.

Transaction boundary는 함수 크기와 같지 않다. 여러 helper가 하나의 business operation을 구현하더라도 commit은 use case 전체가 성공한 뒤 한 번이어야 할 수 있다. Repository method마다 자동 commit하면 상위 service가 여러 변경을 atomic하게 묶기 어려워진다.

반대로 transaction 안에서 network 요청을 오래 기다리면 database lock과 snapshot을 불필요하게 오래 유지할 수 있다. 필요한 외부 데이터를 먼저 준비하고 critical database section을 좁히는 설계를 고려한다. 하지만 외부 요청 결과와 DB write 사이의 race가 생길 수 있으므로 version check나 idempotency도 필요할 수 있다.

Transaction은 오류 처리 문법이 아니라 **어떤 상태 집합을 하나의 성공/실패 단위로 볼 것인지**라는 domain 결정이다.

---

## CHAPTER 05 · isolation은 동시에 실행되는 transaction이 서로 무엇을 볼지 결정한다

두 transaction이 동시에 같은 row를 읽고 수정하면 lost update나 stale decision이 생길 수 있다. SQLite의 isolation과 locking behavior를 이해해야 “내 transaction 안에서는 맞았다”가 concurrent execution에서도 맞는지 판단할 수 있다.

Read snapshot이 시작된 뒤 다른 writer가 commit한 값을 현재 transaction이 즉시 보는지 여부는 isolation mode와 transaction lifecycle에 따라 다를 수 있다. Application이 read→compute→write를 수행할 때 version column과 conditional update를 사용해 다른 writer가 intervened했는지 확인할 수 있다.

Database가 single-writer 성격을 가진 환경에서도 여러 process가 write를 시도하면 lock wait와 `busy` failure가 생길 수 있다. Retry를 한다면 transaction 전체를 안전하게 다시 실행할 수 있는지와 total timeout budget을 정한다.

Process-local lock만으로 여러 process의 database coordination을 해결했다고 생각하지 않는다. 실제 shared state를 소유한 database의 concurrency contract를 기준으로 한다.

---

## CHAPTER 06 · atomic commit은 crash 중에도 old 또는 new 상태 중 하나를 보게 하는 것이 목표다

Database transaction의 강점은 process가 중간에 crash해도 부분 write가 committed state로 노출되지 않도록 storage engine이 journal/WAL과 filesystem operation을 조합한다는 점이다. Application이 직접 여러 JSON 파일을 교체하는 것보다 복잡한 durability protocol을 engine에 맡길 수 있다.

그러나 `commit()`이 반환했다는 사실이 어느 수준의 power-loss durability를 의미하는지는 database mode와 storage configuration에 따라 달라질 수 있다. Performance를 위해 synchronous setting을 낮추면 durability guarantee가 달라질 수 있다. 설정 변경은 측정과 데이터 손실 허용도를 근거로 한다.

WAL mode는 read/write concurrency와 checkpoint behavior에 다른 특성을 제공한다. 무조건 더 빠른 switch가 아니라 workload와 filesystem 조건을 확인한다.

Crash recovery는 정상 shutdown test와 다른 failure class다. 중요한 data라면 process kill, disk-full, interrupted write 같은 fault scenario를 별도로 검증한다.

---

## CHAPTER 07 · index는 read를 빠르게 하지만 write와 storage 비용을 추가한다

`WHERE user_id = ?`를 자주 조회하는데 적절한 index가 없다면 table row가 늘수록 많은 row를 검사할 수 있다. Index는 key를 별도 구조로 유지해 lookup을 줄이지만 insert/update/delete 때 index도 갱신해야 하고 disk 공간을 사용한다.

모든 column에 index를 추가하는 것은 정답이 아니다. 실제 query pattern과 selectivity를 보고 선택한다. Composite index는 column 순서가 query 조건과 어떤 관계인지 이해해야 한다.

Query planner가 어떤 index를 선택하는지 explain 도구로 확인할 수 있다. “index가 있으니 사용될 것”이라고 가정하지 않는다. 데이터 분포와 통계에 따라 planner 선택이 달라질 수 있다.

Performance test는 빈 개발 DB가 아니라 현실적인 row count와 분포에서 수행한다. 100행에서의 full scan은 빠르지만 100만행에서는 전혀 다른 병목이 된다.

---

## CHAPTER 08 · migration은 application code와 schema version 사이의 호환성 작업이다

새 column을 추가하거나 status representation을 바꾸면 기존 database를 새 schema로 변환해야 한다. Migration은 한 번 실행되는 maintenance code가 아니라 사용자 데이터를 바꾸는 production code다. Backup, retry, rollback 또는 forward-fix 전략이 필요하다.

Application update와 migration이 동시에 배포되지 않는 환경에서는 old code와 new schema가 잠시 공존할 수 있다. 먼저 nullable/new column을 추가하고 양쪽 format을 지원한 뒤 data를 backfill하고 마지막에 old field를 제거하는 expand-contract 방식이 downtime과 compatibility 위험을 줄일 수 있다.

Large migration은 transaction 크기와 lock duration을 고려한다. Batch migration이 중간에 멈췄을 때 어디까지 완료됐는지 알 수 있고 재실행해도 안전하도록 idempotent checkpoint를 설계한다.

Migration 성공 후 row count, invariant, aggregate를 reconciliation해 의미가 보존됐는지 확인한다. Exception이 없었다는 사실만으로 data correctness를 증명하지 않는다.

---

## CHAPTER 09 · repository test는 in-memory fake와 실제 SQLite를 서로 다른 목적으로 사용한다

In-memory fake repository는 service의 domain flow를 빠르게 test하는 데 좋지만 SQL syntax, constraint, transaction isolation을 재현하지 못한다. 실제 SQLite integration test를 별도로 두어 schema와 query contract를 검증한다.

Temporary database file을 test마다 새로 만들면 test isolation이 좋고 실제 file behavior도 확인할 수 있다. In-memory SQLite는 빠르지만 connection별 database lifetime과 file locking이 production과 다를 수 있으므로 어떤 failure class를 검증하는지 명확히 한다.

Repository contract test를 같은 test suite로 fake와 SQLite implementation에 적용하면 `get`, duplicate handling, transaction behavior가 대체 가능한지 확인할 수 있다. Concrete implementation-specific test는 WAL, lock, migration 같은 별도 특성을 다룬다.

Concurrency test에서는 두 connection을 사용해 실제 interleaving을 만든다. 하나의 connection만 공유하는 test로 여러 writer 문제를 검증했다고 하지 않는다.

---

## CHAPTER 10 · database 도입은 persistence 책임을 더 명시적으로 만드는 단계다

SQLite를 쓰기 시작했다고 domain model이 SQL row가 되어야 하는 것은 아니다. CLI/API는 raw query를 몰라도 되고 service는 transaction boundary를 orchestration하며 repository adapter가 SQL과 mapping을 담당한다. Schema와 domain invariant가 겹치는 부분은 양쪽에서 적절히 보호한다.

성능 문제는 query count, transaction duration, index usage, connection lifetime을 측정한다. SQL 문자열 한 줄을 미세하게 고치기 전에 N+1 query와 불필요한 round trip 같은 구조적 비용을 찾는다.

오류도 계층별로 번역한다. Unique constraint violation을 domain의 `DuplicateTask`로 바꿀 수 있고 disk-full이나 corruption은 정상 업무 실패와 구분해 상위에 전달한다.

Persistence를 배우는 핵심은 database API를 호출하는 법이 아니라 **메모리의 순간 상태를 crash와 concurrency를 견디는 장기 상태로 만들기 위해 schema·transaction·isolation·migration 계약을 설계하는 것**이다.