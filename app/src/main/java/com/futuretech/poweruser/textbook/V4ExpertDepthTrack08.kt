package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack08 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C08-S01",
            depthTitle("DB를 표가 아니라 page·row version·migration이 있는 저장 엔진으로 본다"),
            depthParagraph("""
V3에서 table, row, column, schema, type, constraint를 이미 배웠다면 이제 “DB가 row를 어디에 어떻게 저장하고 동시에 읽고 쓰는가”로 내려간다. 관계형 DB의 논리 모델은 표지만 저장 엔진은 보통 fixed-size page 단위로 디스크와 buffer cache를 관리한다. 한 row를 읽어도 page 전체를 메모리로 가져올 수 있고, page 안의 빈 공간·row 위치·version 정보가 성능에 영향을 준다.
"""),
            depthHeading("1. page와 buffer cache"),
            depthParagraph("""
DB는 매 SELECT마다 storage에서 직접 한 row만 읽는 것이 아니라 필요한 page를 buffer pool/cache에 올리고 이후 접근을 메모리에서 처리한다. working set이 memory보다 크거나 full scan이 반복되면 hot page가 밀려 cache miss가 늘 수 있다. “RAM을 늘렸더니 DB가 빨라짐”이 단순 CPU 성능 때문이 아니라 cache hit 증가 때문일 수 있다.
"""),
            depthHeading("2. MVCC는 reader와 writer 충돌을 줄이기 위한 version 모델이다"),
            depthParagraph("""
PostgreSQL 같은 DB는 row를 수정할 때 기존 version을 즉시 덮어쓰기보다 새 version을 만들고 transaction snapshot에 따라 서로 다른 version을 보게 하는 MVCC를 사용한다. reader가 writer lock을 오래 기다리지 않는 장점이 있지만 오래된 version 정리(vacuum 등)와 transaction id 관리 비용이 있다. “UPDATE하면 row 한 칸이 바로 바뀐다”는 단순 모델보다 실제 동시성 동작을 이해하게 해 준다.
"""),
            depthHeading("3. surrogate key와 natural key를 구분한다"),
            depthParagraph("""
email처럼 업무 의미가 있는 값은 바뀔 수 있고 대소문자/normalization 정책도 있다. 내부 식별자는 immutable surrogate key를 쓰고 email에는 separate UNIQUE constraint를 둘 수 있다. 반대로 국가코드처럼 안정적이고 의미가 분명한 natural key가 적합할 수도 있다. 모든 table에 auto-increment id를 기계적으로 붙이는 것이 정답은 아니다.
"""),
            depthHeading("4. soft delete는 unique와 relation 문제를 만든다"),
            depthParagraph("""
deleted_at만 채우면 row는 남으므로 email UNIQUE가 이미 삭제된 계정을 계속 막을 수 있다. partial unique index, active flag를 포함한 key, 계정 복구 정책 중 무엇이 맞는지 설계해야 한다. 모든 query가 deleted_at IS NULL을 잊지 않아야 하므로 view/repository scope를 사용할 수도 있다. soft delete는 “복구 쉬움” 대신 데이터 생명주기 복잡성을 산다.
"""),
            depthHeading("5. schema migration은 code deploy와 순서를 맞춘다"),
            depthParagraph("""
운영 DB에 NOT NULL column을 한 번에 추가하면 기존 row와 old app이 깨질 수 있다. expand-and-contract는 먼저 nullable/new column 추가, old/new 둘 다 읽는 code 배포, backfill, new write 전환, 충분한 검증 후 old column 제거처럼 단계적으로 바꾼다. migration 자체도 lock과 rewrite를 만들 수 있으므로 큰 table에서는 실행 계획과 영향 시간을 확인한다.
"""),
            depthCode("""
안전한 column 교체 예
1) new_email nullable 추가
2) app: old_email/new_email 둘 다 읽기
3) 새 write는 두 column 동시 기록
4) background backfill
5) 누락 0건 검증
6) app: new_email만 읽기
7) 충분한 기간 후 old_email 제거

code rollback 가능 구간을 최대한 길게 유지한다.
"""),
            depthBullets(
                "schema 변경은 SQL 한 줄이 아니라 old app/new app 동시 존재 기간까지 포함한다.",
                "soft delete를 넣으면 unique, FK, 통계, 개인정보 보관 정책을 다시 검토한다.",
                "DB memory 문제를 볼 때 buffer hit ratio와 working set을 함께 본다."
            )
        ),
        expertPack(
            sectionId = "V1-C08-S02",
            depthTitle("집계·무결성 다음 단계: constraint timing·window function·pre-aggregation"),
            depthHeading("constraint는 언제 검사되는지도 중요하다"),
            depthParagraph("""
FOREIGN KEY나 UNIQUE를 statement마다 즉시 확인하는 DB가 많지만 일부 constraint는 transaction commit까지 지연(deferrable)할 수 있다. 두 row가 서로 참조하거나 key swap처럼 중간 상태만 보면 위반이지만 최종 transaction 상태는 유효한 작업에서 도움이 된다. 다만 지연 검증은 transaction 끝에서 한꺼번에 실패할 수 있으므로 사용 위치를 좁힌다.
"""),
            depthHeading("1. aggregate와 window function은 결과 row 수가 다르다"),
            depthParagraph("""
GROUP BY는 여러 row를 그룹당 한 row로 접지만 window function은 원래 row를 유지한 채 그룹 통계를 옆에 붙인다. 사용자별 누적 매출, 순위, 이전/다음 row 비교를 self join 없이 표현할 수 있다. SUM(amount) OVER(PARTITION BY user_id ORDER BY created_at) 같은 식은 각 주문 row에 누적값을 계산한다.
"""),
            depthHeading("2. HAVING과 WHERE의 실행 의미"),
            depthParagraph("""
WHERE는 grouping 전에 개별 row를 줄이고 HAVING은 aggregate 결과 그룹을 거른다. status='PAID'를 WHERE가 아니라 HAVING에서 억지로 처리하면 불필요한 row를 먼저 group할 수 있고 의미도 달라질 수 있다. 실제 optimizer가 재배치할 수 있어도 query를 작성할 때 논리 단계가 선명해야 한다.
"""),
            depthHeading("3. materialized view와 summary table"),
            depthParagraph("""
수억 건 주문에서 dashboard를 열 때마다 일별 매출 GROUP BY를 전체 scan하면 비싸다. 일정 주기나 event 기반으로 daily_sales summary를 미리 계산하면 read latency를 낮출 수 있다. 대신 원본과 summary 사이 freshness 지연, 재계산/backfill, late-arriving event 처리 규칙이 필요하다. 빠른 read는 consistency 비용을 산다.
"""),
            depthHeading("4. constraint와 application check의 race"),
            depthParagraph("""
“email이 있는지 SELECT→없으면 INSERT”만 하면 두 request가 동시에 없다고 보고 둘 다 insert할 수 있다. 최종 중복 방지는 DB UNIQUE constraint가 맡고 application은 unique violation을 domain conflict로 변환한다. 사전 check는 사용자 친화적 메시지/빠른 실패용일 뿐 concurrency 안전성을 보장하지 않는다.
"""),
            depthHeading("5. 여러 row invariant는 transaction에서 다시 본다"),
            depthParagraph("""
주문 total이 items 합과 같다는 규칙을 application에서 계산해 저장해도 다른 transaction이 item을 바꾸면 어긋날 수 있다. 어떤 데이터가 source of truth인지 정하고, total을 계산값으로 매번 구할지 snapshot으로 저장할지, 변경 경로를 transaction으로 묶을지 결정한다. 중복 저장은 read 성능과 일관성 사이 trade-off다.
"""),
            depthCode("""
user별 주문과 누적 합
SELECT
  user_id,
  created_at,
  amount,
  SUM(amount) OVER (
    PARTITION BY user_id
    ORDER BY created_at
  ) AS running_total
FROM orders
WHERE status = 'PAID';
""", "sql"),
            depthBullets(
                "사전 SELECT 검사를 uniqueness 보장으로 착각하지 않고 DB constraint를 최종 안전망으로 둔다.",
                "실시간 aggregate가 느리면 summary freshness 요구를 먼저 정한다.",
                "derived data를 저장하면 재계산과 drift 탐지 절차를 함께 만든다."
            )
        ),
        expertPack(
            sectionId = "V1-C08-S03",
            depthTitle("JOIN을 문법에서 실행 전략과 cardinality 추정 문제로 올린다"),
            depthHeading("같은 JOIN도 nested loop·hash join·merge join으로 실행될 수 있다"),
            depthParagraph("""
optimizer는 table 크기, index, 통계, 조건에 따라 join algorithm을 선택한다. 작은 outer table의 각 row마다 index로 inner를 찾는 nested loop가 빠를 수도 있고, 큰 unsorted set에서는 hash table을 만들어 match하는 hash join이 유리할 수 있다. 이미 정렬된 두 입력은 merge join이 효율적일 수 있다. SQL 문장이 같아도 data distribution이 바뀌면 plan이 달라진다.
"""),
            depthHeading("1. cardinality estimate가 틀리면 좋은 index가 있어도 나쁜 plan을 고를 수 있다"),
            depthParagraph("""
optimizer는 “이 조건으로 몇 row가 남을까”를 통계로 추정한다. 특정 tenant에 data가 몰렸거나 column 간 상관관계가 강한데 통계가 이를 못 잡으면 10건으로 예상한 결과가 실제 100만 건일 수 있다. EXPLAIN의 estimated rows와 EXPLAIN ANALYZE의 actual rows 차이를 보면 통계 문제를 의심할 수 있다.
"""),
            depthHeading("2. N+1은 latency 누적 문제다"),
            depthParagraph("""
DB가 같은 기기 안에 있어도 query parsing/round trip/lock 비용이 있고 remote DB면 network RTT가 추가된다. 1000개 order를 한 번 조회한 뒤 user를 1000번 따로 조회하면 각 query가 2ms만 걸려도 전체가 크게 늘어난다. join/eager load/batch IN query로 round trip 수를 줄이되 한 번에 거대한 cartesian result를 만드는 문제도 피한다.
"""),
            depthHeading("3. many-to-many junction table은 relation 자체의 데이터를 담는다"),
            depthParagraph("""
student_course에 student_id, course_id만 두는 것이 아니라 enrolled_at, role, grade처럼 관계에 속한 속성을 둘 수 있다. 이 순간 junction table은 단순 연결용 숨은 table이 아니라 독립 domain entity가 된다. unique(student_id,course_id)로 중복 enrollment를 막는 식으로 relation invariant도 둔다.
"""),
            depthHeading("4. denormalization에는 갱신 권한자를 정한다"),
            depthParagraph("""
order에 user_name snapshot을 저장하면 과거 주문서 이름을 유지하고 join을 줄일 수 있지만 user profile이 바뀌어도 자동 갱신할지 말지 결정해야 한다. 일부 denormalized field는 “주문 당시 값”이라 의도적으로 stale해야 한다. 중복 데이터는 무조건 나쁜 것이 아니라 의미와 갱신 source가 불명확할 때 위험하다.
"""),
            depthHeading("5. CTE는 가독성과 optimizer 동작을 둘 다 본다"),
            depthParagraph("""
복잡 query를 WITH 절로 단계별 표현하면 읽기 쉬워지지만 DB/version에 따라 CTE가 materialize되거나 inline되어 성능 특성이 달라질 수 있다. “CTE면 항상 느림/빠름”으로 외우지 말고 실제 plan을 확인한다. recursive CTE는 tree/graph 계층 조회에도 사용할 수 있다.
"""),
            depthCode("""
EXPLAIN ANALYZE에서 볼 것

Estimated rows: 100
Actual rows:    1,200,000

이 차이가 크면 optimizer가 join 순서/algorithm을 잘못 고를 수 있다.
index 추가 전에
- statistics freshness
- skewed data
- correlated columns
- predicate 형태
를 먼저 확인한다.
"""),
            depthBullets(
                "JOIN 중복 row를 DISTINCT로 숨기기 전에 relation cardinality를 먼저 그린다.",
                "ORM 화면 성능 문제는 실제 SQL과 query count를 측정한다.",
                "estimated vs actual row 차이가 큰 query는 통계와 data skew를 조사한다."
            )
        ),
        expertPack(
            sectionId = "V1-C08-S04",
            depthTitle("인덱스와 transaction의 핵심 심화: covering index·WAL·MVCC anomaly·deadlock"),
            depthHeading("복합 index는 left-prefix 규칙만 외우지 말고 query를 cover할 수 있는지 본다"),
            depthParagraph("""
(user_id, created_at) index가 WHERE user_id=? ORDER BY created_at에 잘 맞는 이유는 key 순서와 query가 정렬돼 있기 때문이다. 여기에 SELECT에 필요한 amount까지 index가 포함돼 table heap을 다시 읽지 않아도 되면 covering/index-only scan이 가능할 수 있다. read는 빨라지지만 index 크기와 write 비용이 늘므로 hot query에 선택적으로 사용한다.
"""),
            depthHeading("1. selectivity가 낮은 index는 효과가 작을 수 있다"),
            depthParagraph("""
boolean is_active가 99% true라면 true query에 index를 써도 거의 전체 row를 읽어야 해서 sequential scan이 더 나을 수 있다. 반대로 rare false를 찾는 query에는 도움이 될 수 있다. index 존재 여부가 아니라 predicate가 전체 중 몇 %를 줄이는지, clustering/correlation이 어떤지 본다.
"""),
            depthHeading("2. WAL은 durability와 crash recovery의 핵심 구조다"),
            depthParagraph("""
많은 DB는 data page를 즉시 모두 storage에 쓰기 전에 변경 내용을 write-ahead log에 순서대로 기록한다. commit을 성공으로 알리기 전에 필요한 WAL이 durable하게 기록되면 crash 후 log를 replay해 data page를 복구할 수 있다. WAL은 backup/replication에도 사용될 수 있다. “commit=table file 즉시 수정”이라는 단순 모델을 확장해야 한다.
"""),
            depthHeading("3. isolation level과 write skew"),
            depthParagraph("""
두 의사가 on-call이어야 최소 한 명 남는 규칙이 있다고 하자. snapshot isolation에서 두 transaction이 각각 “다른 의사가 아직 on-call”임을 보고 자기 row를 off로 바꾸면 서로 다른 row를 수정했기 때문에 conflict 없이 둘 다 commit될 수 있고 invariant가 깨진다. serializable isolation이나 explicit locking/constraint 모델이 필요한 사례다.
"""),
            depthHeading("4. deadlock은 DB가 감지해 한쪽을 죽일 수 있다"),
            depthParagraph("""
T1: account 1 lock→account 2 대기, T2: account 2 lock→account 1 대기면 cycle이다. DB는 보통 하나를 victim으로 abort한다. application은 deadlock/serialization failure를 transient error로 분류해 transaction 전체를 제한적으로 retry할 수 있어야 한다. partial logic만 재실행하면 side effect가 중복될 수 있다.
"""),
            depthHeading("5. EXPLAIN ANALYZE는 실제 실행을 하므로 조심한다"),
            depthParagraph("""
SELECT는 실제 실행 통계를 얻는 데 유용하지만 UPDATE/DELETE에 ANALYZE를 붙이면 실제 변경이 일어날 수 있다. production에서는 read-only transaction, replica, safe query, rollback 등을 고려한다. plan에서 loops, actual time, rows, buffers를 보고 “느린 node 하나”뿐 아니라 반복 횟수까지 곱해 총 비용을 이해한다.
"""),
            depthCode("""
write skew 예
규칙: 의사 A/B 중 최소 1명은 ON

T1 snapshot: A=ON, B=ON -> A를 OFF
T2 snapshot: A=ON, B=ON -> B를 OFF

서로 다른 row update라 충돌이 안 날 수 있음
최종: A=OFF, B=OFF -> 업무 invariant 위반

transaction 존재만으로 충분하지 않다.
"""),
            depthBullets(
                "index는 query pattern, selectivity, write overhead를 함께 측정한다.",
                "WAL/fsync 설정은 성능과 durability trade-off이므로 의미를 모르고 끄지 않는다.",
                "serialization/deadlock retry는 transaction 전체와 외부 side effect 경계를 함께 설계한다."
            )
        )
    )
}
