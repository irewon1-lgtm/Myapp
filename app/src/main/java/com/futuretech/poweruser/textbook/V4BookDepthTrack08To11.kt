package com.futuretech.poweruser.textbook

internal object V4BookDepthTrack08To11 {
    val packs: List<BookDepthPack> = listOf(
        depthPack(
            sectionId = "V1-C08-S01",
            topics = listOf(
                depthTopic("table과 row를 엑셀처럼만 생각하면 놓치는 것", "관계형 DB table은 단순 표 모양보다 schema와 constraint가 중요하다. 각 column의 타입과 null 허용 여부, key 관계를 DB가 알고 있기 때문에 잘못된 값을 저장 단계에서 막을 수 있다. 애플리케이션 코드가 실수해도 데이터 규칙을 DB가 한 번 더 지킬 수 있다."),
                depthTopic("PRIMARY KEY는 단순 일련번호보다 더 큰 역할을 한다", "한 row를 다른 row와 확실히 구분할 안정적인 식별자가 있어야 update, delete, relation을 안전하게 수행할 수 있다. 사람이 보는 이름이나 이메일은 변경되거나 중복될 수 있으므로 업무상 의미와 식별자 역할을 분리하는 경우가 많다."),
                depthTopic("CRUD를 SQL 문법 네 개로만 외우면 부족하다", "실제 코드는 create 전에 validation, read 때 filter와 pagination, update 때 concurrent change, delete 때 relation과 soft delete 정책을 함께 고려한다. SQL 한 줄은 시스템 전체 데이터 흐름 중 한 단계다."),
                depthTopic("SELECT *가 편해도 항상 좋은 선택은 아니다", "필요하지 않은 큰 column까지 읽으면 DB I/O와 network 전송량이 늘고 schema 변경 영향도 커진다. 어떤 화면과 API에 어떤 field가 필요한지 명시하면 성능과 contract가 더 분명해진다."),
                depthTopic("NULL은 빈 문자열이나 0과 다른 상태다", "NULL은 값이 없거나 알려지지 않았음을 표현한다. 빈 문자열은 길이 0인 실제 문자열이고 0은 실제 숫자다. SQL에서는 NULL 비교 규칙도 다르므로 값 없음의 의미를 명확히 설계해야 한다.")
            ),
            workedExample = """
                users table
                id          PRIMARY KEY
                email       UNIQUE NOT NULL
                name        NOT NULL
                deleted_at  NULL 가능

                INSERT user
                → email 중복이면 DB가 거부

                DELETE 대신 deleted_at 기록
                → 실제 row는 남고 조회 조건에서 제외

                schema가 업무 규칙 일부를 지킴
            """,
            mistakes = listOf(
                "table을 단순 엑셀 파일처럼 생각하고 constraint를 전부 앱 코드에만 둔다.",
                "사람 이름처럼 바뀔 수 있는 값을 무조건 primary key로 사용한다.",
                "모든 조회에서 SELECT *를 습관적으로 사용한다.",
                "NULL과 빈 문자열과 0을 같은 값 없음으로 취급한다.",
                "delete가 다른 table relation에 어떤 영향을 주는지 확인하지 않는다."
            ),
            questions = listOf(
                "natural key와 surrogate key는 언제 선택할까?",
                "soft delete 데이터는 unique constraint와 어떻게 충돌할 수 있을까?",
                "DB type과 애플리케이션 type이 다르면 변환 오류가 어디서 생길까?",
                "schema migration은 운영 중 table을 어떻게 안전하게 바꿀까?"
            )
        ),
        depthPack(
            sectionId = "V1-C08-S02",
            topics = listOf(
                depthTopic("집계는 row를 읽는 것과 다른 질문을 한다", "COUNT, SUM, AVG 같은 aggregate는 개별 row보다 집단의 통계를 만든다. GROUP BY를 쓰면 category나 날짜처럼 같은 key끼리 묶어 통계를 낼 수 있다. 어떤 row를 먼저 filter하고 어떤 기준으로 group할지에 따라 결과가 크게 달라진다."),
                depthTopic("constraint는 데이터 무결성을 마지막에 지키는 안전망이다", "NOT NULL, UNIQUE, CHECK, FOREIGN KEY 같은 constraint는 애플리케이션 여러 경로에서 같은 규칙을 깜빡해도 DB 상태가 깨지는 것을 막아 준다. 하지만 오류 메시지와 사용자 경험까지 DB에 맡길 수는 없으므로 app validation과 함께 사용한다."),
                depthTopic("FOREIGN KEY는 id를 저장하는 것보다 관계를 보장하는 장치다", "orders.user_id가 999를 가리키는데 users에 999가 없다면 관계가 깨진다. foreign key는 이런 참조를 막고 delete/update 시 관계를 어떻게 처리할지 규칙을 둘 수 있다. referential integrity가 바로 이런 연결의 일관성을 의미한다."),
                depthTopic("데이터 무결성은 값 범위뿐 아니라 여러 row 관계까지 포함한다", "잔액이 음수가 되면 안 된다는 단일 값 규칙도 있고, 주문 총액이 주문 항목 합계와 맞아야 한다는 여러 row 규칙도 있다. 복잡한 규칙은 transaction과 application service까지 함께 설계해야 한다."),
                depthTopic("통계 query는 작은 table에서 맞아도 큰 table에서 느릴 수 있다", "수백 row에서는 GROUP BY와 ORDER BY 비용이 눈에 띄지 않지만 수천만 row에서는 full scan, sort, temporary data가 커진다. index, pre-aggregation, materialized view 같은 전략이 필요한 이유다.")
            ),
            workedExample = """
                orders
                id | user_id | amount | status

                질문: 사용자별 결제 완료 총액

                1) WHERE status = 'PAID'
                2) GROUP BY user_id
                3) SUM(amount)

                무결성
                user_id는 users.id를 FOREIGN KEY로 참조
                amount는 0 이상 CHECK

                query와 constraint는 서로 다른 역할
            """,
            mistakes = listOf(
                "GROUP BY 전에 어떤 row가 filter되는지 생각하지 않는다.",
                "앱 validation이 있으니 DB constraint는 필요 없다고 생각한다.",
                "foreign key를 단순 id column 이름 규칙으로만 이해한다.",
                "복잡한 무결성 규칙을 서로 다른 request가 따로 갱신하게 둔다.",
                "작은 개발 DB에서 빠른 aggregate query가 운영에서도 항상 빠르다고 가정한다."
            ),
            questions = listOf(
                "HAVING은 WHERE와 어떤 단계가 다를까?",
                "ON DELETE CASCADE를 잘못 쓰면 어떤 위험이 있을까?",
                "unique constraint와 transaction이 동시에 들어오는 중복 요청을 어떻게 막을까?",
                "대규모 통계를 실시간 계산하지 않고 미리 준비하는 방법은 무엇일까?"
            )
        ),
        depthPack(
            sectionId = "V1-C08-S03",
            topics = listOf(
                depthTopic("JOIN은 table을 물리적으로 붙이는 작업이 아니다", "query 실행 시 관계 key를 기준으로 여러 table의 row를 조합해 결과를 만든다. 원본 table 구조가 합쳐져 하나가 되는 것은 아니다. INNER JOIN과 LEFT JOIN은 관계가 없는 row를 결과에 남길지에서 차이가 난다."),
                depthTopic("정규화는 table을 많이 쪼개는 대회가 아니다", "같은 사용자 주소를 주문 row마다 반복 저장하면 주소 수정 때 여러 곳이 다르게 남을 수 있다. 정규화는 중복 때문에 update anomaly가 생기는 구조를 줄이는 사고법이다. 반대로 읽기 성능을 위해 일부 중복을 의도적으로 허용하는 denormalization도 있다."),
                depthTopic("N+1 query는 ORM을 쓰면 자동으로 사라지는 문제가 아니다", "주문 100개를 조회한 뒤 각 주문마다 user query를 한 번씩 보내면 1+100개의 query가 생길 수 있다. join, eager loading, batch query로 필요한 관계를 묶어 가져와야 한다. 화면은 한 번 열렸는데 DB round trip이 수백 번 생기는 대표 성능 문제다."),
                depthTopic("many-to-many 관계에는 중간 table이 필요하다", "학생과 수업처럼 한 학생이 여러 수업을 듣고 한 수업에도 여러 학생이 참여하면 한쪽 table에 id 하나만 저장할 수 없다. enrollment 같은 junction table에 두 key와 추가 속성을 저장하면 관계 자체를 row로 표현할 수 있다."),
                depthTopic("조회 결과 중복은 JOIN 조건 문제일 수도 있고 정상일 수도 있다", "한 주문에 항목이 세 개 있으면 orders와 items를 join한 결과에서 주문 정보가 세 줄 반복될 수 있다. 이것은 관계의 cardinality 때문에 자연스러운 결과일 수 있다. DISTINCT를 무조건 붙이기 전에 어떤 관계가 row를 늘렸는지 이해해야 한다.")
            ),
            workedExample = """
                users
                1 | Kim

                orders
                10 | user_id=1
                11 | user_id=1

                items
                100 | order_id=10
                101 | order_id=10

                users JOIN orders
                → Kim 정보가 주문 수만큼 반복

                orders JOIN items
                → order 10이 item 수만큼 반복

                반복 row가 생긴 이유는 관계 수에 있음
            """,
            mistakes = listOf(
                "JOIN하면 원본 table 자체가 하나로 합쳐진다고 생각한다.",
                "정규화를 column을 무조건 최대한 잘게 나누는 규칙으로 외운다.",
                "ORM을 사용하니 N+1 query가 생길 수 없다고 생각한다.",
                "many-to-many 관계를 한 column의 쉼표 문자열로 저장한다.",
                "중복 row가 보이면 원인을 보지 않고 DISTINCT부터 붙인다."
            ),
            questions = listOf(
                "LEFT JOIN 뒤 WHERE 조건 때문에 사실상 INNER JOIN처럼 될 수 있는 이유는 무엇일까?",
                "denormalization은 어떤 읽기 성능 문제를 해결하려고 할까?",
                "junction table에 created_at 같은 속성을 두는 이유는 무엇일까?",
                "ORM이 만든 실제 SQL을 확인하는 습관이 왜 중요할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C08-S04",
            topics = listOf(
                depthTopic("index는 책의 색인처럼 별도 구조를 유지하는 비용이 있다", "특정 column 값을 빠르게 찾기 위해 정렬된 보조 구조를 만들면 read는 빨라질 수 있지만 insert/update 때 index도 함께 갱신해야 한다. index를 많이 만들수록 쓰기 비용과 저장공간이 늘 수 있으므로 실제 query pattern을 보고 선택한다."),
                depthTopic("복합 index는 column 순서가 중요하다", "index가 (user_id, created_at) 순서라면 user_id로 먼저 범위를 좁힌 뒤 created_at을 활용하기 좋다. created_at만 검색할 때 같은 효과를 못 낼 수 있다. index column 목록만 보지 말고 query 조건과 정렬 순서를 함께 본다."),
                depthTopic("EXPLAIN은 DB가 어떤 길을 선택했는지 보는 도구다", "query optimizer는 table scan, index scan, join 순서 같은 여러 계획의 비용을 추정해 하나를 고른다. EXPLAIN 결과로 실제 어떤 index를 쓰는지, 예상 row 수가 큰지 확인할 수 있다. 느린 query를 감으로 고치기 전에 실행 계획을 보는 이유다."),
                depthTopic("transaction은 여러 문장을 한 덩어리로 묶는 것 이상이다", "원자성 때문에 중간 실패 시 전체 변경을 되돌릴 수 있고, isolation은 동시에 실행되는 transaction이 서로의 중간 상태를 어떻게 볼지 정한다. 잔액 차감과 주문 생성처럼 함께 성공하거나 함께 실패해야 하는 작업에 중요하다."),
                depthTopic("ACID는 주문처럼 외우기보다 실패 상황으로 기억한다", "Atomicity는 반만 저장되는 문제, Consistency는 규칙이 깨지는 문제, Isolation은 동시 실행 충돌, Durability는 성공했다고 답한 변경이 장애 뒤 사라지는 문제와 연결해 이해하면 쉽다. 실제 DB는 성능과 isolation 수준 사이에서 선택을 제공한다.")
            ),
            workedExample = """
                계좌 A → B 10,000원 이체

                transaction 시작
                1) A 잔액 -10000
                2) 중간에 오류 발생
                3) B 잔액 +10000 실행 못함

                transaction이 없다면 A만 차감될 위험
                transaction rollback
                → A 차감도 취소

                동시에 두 이체가 들어오면 isolation 문제도 추가됨
            """,
            mistakes = listOf(
                "index는 많을수록 무조건 좋다고 생각한다.",
                "복합 index의 column 순서를 query와 무관하게 정한다.",
                "느린 query를 EXPLAIN 없이 SELECT 문 모양만 바꾼다.",
                "transaction을 여러 SQL을 묶는 문법 정도로만 이해한다.",
                "isolation level을 높이면 비용 없이 모든 동시성 문제가 사라진다고 생각한다."
            ),
            questions = listOf(
                "covering index는 table row 접근을 어떻게 줄일까?",
                "optimizer 통계가 오래되면 잘못된 plan을 고를 수 있을까?",
                "deadlock은 두 transaction이 어떤 순서로 lock을 잡을 때 생길까?",
                "WAL은 durability와 crash recovery에 어떤 역할을 할까?"
            )
        ),

        depthPack(
            sectionId = "V1-C09-S01",
            topics = listOf(
                depthTopic("보안은 공격 이름을 외우기 전에 자산과 경계를 찾는 일이다", "사용자 개인정보, 결제 권한, API token, 관리자 기능처럼 지켜야 할 자산을 먼저 찾고 누가 어느 경계를 넘어 접근하는지 본다. browser→server, server→DB, service→외부 API는 각각 trust boundary가 될 수 있다. 경계마다 인증·검증·권한 확인이 필요하다."),
                depthTopic("authentication과 session 수명은 함께 설계해야 한다", "로그인에 성공한 뒤 session이 영원히 살아 있다면 탈취된 credential 위험도 오래 간다. 만료, refresh, logout, 기기별 revoke, 중요 작업 재인증 같은 정책이 필요하다. 편리함과 보안 사이에서 수명을 정한다."),
                depthTopic("authorization은 역할 이름 하나보다 실제 자원 관계를 본다", "admin/user 같은 role도 유용하지만 ‘이 주문의 owner인가’, ‘이 문서에 공유 권한이 있는가’처럼 resource별 조건이 더 중요할 수 있다. endpoint URL을 숨기는 것은 권한 검사가 아니다. server가 매 요청에서 실제 권한을 확인해야 한다."),
                depthTopic("least privilege는 사고가 났을 때 피해 범위를 줄인다", "앱이 필요한 DB table만 접근하고 worker가 필요한 queue만 쓰도록 권한을 줄이면 credential 하나가 유출돼도 모든 시스템을 장악하기 어렵다. 개발 편의를 위해 모든 권한을 주는 습관은 운영 위험을 크게 키운다."),
                depthTopic("threat model은 완벽한 문서보다 질문 습관에서 시작한다", "누가 공격할 수 있는가, 무엇을 얻으려 하는가, 어떤 입력을 조작할 수 있는가, 실패했을 때 어떤 피해가 생기는가를 기능 설계 때 묻는다. 로그인, 파일 업로드, 결제, 관리자 기능처럼 위험이 큰 경계부터 구체적으로 본다.")
            ),
            workedExample = """
                기능: 사용자가 자기 영수증 PDF 다운로드

                asset: 영수증 개인정보
                boundary: browser → API
                authentication: 로그인 사용자인가
                authorization: receipt.ownerId == currentUser.id 인가
                validation: receipt id 형식 정상인가
                logging: 다운로드 성공/실패 기록

                URL을 추측하기 어렵게 만드는 것만으로는 보호되지 않음
            """,
            mistakes = listOf(
                "로그인만 했으면 모든 API 권한 문제가 해결됐다고 생각한다.",
                "관리자 URL을 숨기면 authorization이 필요 없다고 생각한다.",
                "개발·운영 service account에 항상 관리자 권한을 준다.",
                "token 만료와 revoke 정책 없이 영구 credential을 사용한다.",
                "보안 검토를 개발이 끝난 뒤 공격 이름 체크리스트로만 수행한다."
            ),
            questions = listOf(
                "RBAC와 resource-based authorization은 어떻게 조합할까?",
                "중요 작업에서 MFA를 다시 요구하는 이유는 무엇일까?",
                "secret rotation은 유출 대응에 어떤 도움을 줄까?",
                "audit log는 일반 application log와 무엇이 달라야 할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C09-S02",
            topics = listOf(
                depthTopic("hash와 encryption은 되돌릴 수 있는지가 핵심 차이다", "암호화는 key를 가진 사람이 원문을 복구할 수 있게 설계하고 hash는 일반적으로 원문 복구가 목적이 아닌 단방향 요약값을 만든다. password 저장은 복호화할 필요가 없으므로 느린 password hashing algorithm과 salt를 사용한다."),
                depthTopic("password에 일반 SHA hash만 쓰면 왜 부족한가", "빠른 hash는 공격자가 수십억 후보를 빠르게 시험하기 좋다. password hashing은 계산과 메모리 비용을 일부러 높여 brute force 속도를 늦춘다. Argon2, scrypt, bcrypt 같은 계열이 등장한 이유다. 직접 암호 알고리즘을 설계하지 않는다."),
                depthTopic("salt는 password를 더 복잡하게 만드는 문자열이 아니다", "각 password마다 무작위 salt를 함께 사용하면 같은 password라도 저장 hash가 달라진다. 미리 계산한 rainbow table 재사용을 어렵게 하고 대량 공격 비용을 높인다. salt는 비밀일 필요가 없지만 사용자별로 충분히 무작위여야 한다."),
                depthTopic("digital signature는 내용을 숨기지 않는다", "서명은 private key로 만든 검증 정보와 public key를 이용해 누가 만들었는지와 내용이 바뀌지 않았는지 확인하는 목적이다. 서명된 문서의 본문이 공개 상태일 수도 있다. confidentiality와 authenticity를 구분해야 한다."),
                depthTopic("encoding은 보안 장치가 아니다", "URL encoding, Base64, UTF-8은 표현 방식이다. 누구나 규칙을 알면 되돌릴 수 있으므로 secret을 보호하지 않는다. 보안 문제에서 ‘읽기 어려워 보인다’와 ‘암호학적으로 보호된다’를 구분해야 한다.")
            ),
            workedExample = """
                회원가입 password
                plain password
                + random salt
                ↓ password hashing (예: Argon2 계열)
                stored hash + salt

                로그인
                입력 password + 저장 salt
                같은 hashing 수행
                저장 hash와 안전하게 비교

                원래 password를 복호화해 비교하는 구조가 아님
            """,
            mistakes = listOf(
                "Base64로 바꾸면 password가 암호화됐다고 생각한다.",
                "password를 빠른 일반 hash 한 번으로 저장한다.",
                "모든 사용자에 같은 고정 salt를 사용한다.",
                "digital signature가 파일 내용을 숨겨 준다고 생각한다.",
                "암호 알고리즘과 key 관리 방식을 직접 새로 설계한다."
            ),
            questions = listOf(
                "pepper는 salt와 어떤 차이가 있을까?",
                "key derivation function은 password에서 key를 만들 때 왜 필요할까?",
                "TLS certificate의 signature는 무엇을 검증할까?",
                "encryption key rotation은 이미 저장된 데이터와 어떻게 함께 처리할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C09-S03",
            topics = listOf(
                depthTopic("SQL injection은 문자열 따옴표를 조심하는 문제보다 구조 분리 문제다", "사용자 입력을 SQL 문자열에 직접 이어 붙이면 입력이 data가 아니라 SQL 문법 일부로 해석될 수 있다. parameterized query는 SQL 구조와 값을 분리해 DB driver가 안전하게 전달하도록 한다. 입력에서 특정 문자를 지우는 blacklist보다 구조적 해결이 중요하다."),
                depthTopic("XSS는 server만의 문제도 frontend만의 문제도 아니다", "사용자 입력이 HTML이나 JavaScript 문맥에 맞는 escaping 없이 출력되면 공격 code가 다른 사용자 browser에서 실행될 수 있다. framework의 기본 escaping을 우회하는 raw HTML 기능, URL, attribute 문맥을 특히 조심한다. CSP는 추가 방어층이지 올바른 output encoding을 대신하지 않는다."),
                depthTopic("CSRF는 browser가 credential을 자동으로 붙이는 성질을 이용한다", "cookie 기반 로그인처럼 browser가 다른 사이트에서 보낸 요청에도 credential을 자동 전송할 수 있으면 공격 사이트가 사용자의 권한으로 state-changing request를 유도할 수 있다. SameSite cookie, CSRF token, origin 검증 등으로 요청 의도를 확인한다."),
                depthTopic("SSRF는 server를 공격자의 proxy처럼 쓰게 만든다", "사용자가 입력한 URL을 server가 그대로 요청하면 외부에서는 접근할 수 없는 내부 service, metadata endpoint에 server 권한으로 접근할 수 있다. 허용 host 목록, scheme 제한, DNS/IP 검증, network egress 정책을 함께 고려한다."),
                depthTopic("파일 업로드는 여러 공격 표면이 겹치는 기능이다", "파일 크기 폭탄, 악성 형식, 경로 조작, public 실행, metadata 개인정보 등 다양한 위험이 있다. server에서 임의 이름으로 격리 저장하고 형식과 크기를 검증하며 필요한 경우 별도 scanner와 object storage를 사용하는 이유다.")
            ),
            workedExample = """
                위험한 SQL
                "SELECT * FROM users WHERE email = '" + input + "'"

                안전한 방향
                SQL: SELECT * FROM users WHERE email = ?
                params: [input]

                핵심
                query 구조는 코드가 정함
                사용자 값은 data로만 전달
                입력이 SQL 문법이 되지 않게 경계를 분리
            """,
            mistakes = listOf(
                "SQL injection을 막으려고 따옴표 몇 개만 직접 제거한다.",
                "React/Vue 같은 framework를 쓰면 어떤 raw HTML 사용도 안전하다고 생각한다.",
                "GET만 아니면 CSRF가 자동으로 막힌다고 생각한다.",
                "server가 요청할 URL을 사용자에게 제한 없이 받는다.",
                "업로드 파일을 원래 이름 그대로 web에서 실행 가능한 위치에 저장한다."
            ),
            questions = listOf(
                "stored XSS와 reflected XSS는 공격 데이터가 어디에 남는지가 어떻게 다를까?",
                "SameSite=Lax와 Strict는 사용자 흐름에 어떤 차이를 만들까?",
                "SSRF 방어에서 DNS rebinding을 왜 생각해야 할까?",
                "secret을 코드 저장소에 commit했을 때 단순 삭제만으로 충분하지 않은 이유는 무엇일까?"
            )
        ),

        depthPack(
            sectionId = "V1-C10-S01",
            topics = listOf(
                depthTopic("디버깅은 수정 기술보다 관찰 기술이다", "버그를 고치기 전에 재현 조건, 기대 결과, 실제 결과를 정확히 기록해야 한다. 같은 입력에서 다시 발생하지 않으면 수정이 맞았는지 비교하기 어렵다. 먼저 증상을 안정적으로 재현하고 관찰 지점을 늘리는 것이 핵심이다."),
                depthTopic("minimal reproduction이 강력한 이유", "수천 줄 앱에서만 보이는 문제를 20줄 예제로 줄이면 DB, network, UI 같은 불필요한 변수를 제거할 수 있다. 문제를 줄이는 과정에서 어느 구성요소를 뺐을 때 버그가 사라지는지 자체가 원인 힌트가 된다."),
                depthTopic("binary search식 원인 좁히기는 코드에도 적용된다", "긴 pipeline에서 어디서 값이 잘못됐는지 처음부터 한 줄씩 보기보다 중간 지점 값을 확인해 앞쪽인지 뒤쪽인지 범위를 절반씩 줄일 수 있다. log, assertion, breakpoint를 전략적으로 놓는 방식이다."),
                depthTopic("flaky bug는 재현 횟수와 환경을 기록해야 한다", "항상 실패하지 않는 버그는 timing, race, 외부 dependency, 공유 state에 의존할 수 있다. 발생률, 기기, OS, network, 시간, 이전 동작을 함께 기록하고 반복 실행해 패턴을 찾는다."),
                depthTopic("가설 없이 수정하면 우연히 증상만 숨길 수 있다", "sleep을 1초 넣었더니 race가 사라져도 원인이 해결된 것은 아닐 수 있다. ‘response가 늦게 와 최신 state를 덮는다’ 같은 가설을 세우고 그 가설을 확인할 관찰과 최소 수정으로 검증해야 한다.")
            ),
            workedExample = """
                증상: 저장 버튼을 두 번 누르면 주문이 가끔 두 개 생성됨

                재현
                같은 계정, 같은 상품, 빠르게 두 번 클릭

                관찰
                browser Network: POST가 2번 나감
                server log: 두 request 모두 처리

                가설
                client 중복 클릭 + server idempotency 없음

                수정 후보
                버튼 중복 제출 방지 + idempotency key
                같은 재현 절차로 다시 검증
            """,
            mistakes = listOf(
                "재현 절차 없이 코드부터 바꾼다.",
                "여러 수정안을 한 번에 적용해 어떤 변화가 효과였는지 모른다.",
                "증상이 사라졌다는 이유로 원인 가설 검증을 끝낸다.",
                "flaky bug를 한 번 재현 실패했다고 존재하지 않는다고 판단한다.",
                "log에 필요한 식별자 없이 일반 문장만 남긴다."
            ),
            questions = listOf(
                "structured logging은 문자열 log보다 검색에 어떤 도움이 될까?",
                "heap dump나 profiler는 어떤 종류의 버그에서 필요할까?",
                "git bisect는 언제 원인 commit을 빠르게 찾을 수 있을까?",
                "production bug를 안전하게 재현할 test fixture는 어떻게 만들까?"
            )
        ),
        depthPack(
            sectionId = "V1-C10-S02",
            topics = listOf(
                depthTopic("테스트는 코드가 맞다는 증명서가 아니라 특정 가설의 자동 검사다", "unit test 하나가 통과해도 테스트하지 않은 입력과 환경에서는 버그가 있을 수 있다. 좋은 테스트는 요구사항과 위험한 경계값을 명시하고 실패했을 때 무엇이 깨졌는지 알려 준다. 테스트 개수보다 의미 있는 시나리오가 중요하다."),
                depthTopic("unit·integration·end-to-end는 실패 범위와 비용이 다르다", "unit test는 작은 로직을 빠르게 검증하고 integration test는 DB나 여러 component 연결을 확인하며 end-to-end는 사용자 흐름 전체를 본다. 모든 것을 E2E로만 검사하면 느리고 원인 파악이 어렵고, unit만 있으면 연결 문제를 놓칠 수 있다."),
                depthTopic("경계값 테스트는 정상값 여러 개보다 더 많은 버그를 찾기도 한다", "나이 19 이상 규칙이라면 18, 19, 20을 보고, 배열 index라면 비어 있음·첫 항목·마지막 항목·범위 밖을 본다. 비교 연산자 하나 차이와 off-by-one 오류는 경계에서 드러난다."),
                depthTopic("mock이 많으면 실제 연결 버그를 숨길 수 있다", "모든 dependency를 mock하면 test는 빠르지만 mock이 실제 API와 다른 행동을 해도 알기 어렵다. 중요한 DB query나 serialization, network contract는 integration test로 실제 구현과 맞는지 확인해야 한다."),
                depthTopic("테스트도 유지보수 대상이다", "구현 세부를 너무 자세히 검사하면 내부 refactoring만 해도 test가 대량으로 깨진다. 사용자에게 보이는 behavior와 공개 contract를 중심으로 검사하면 구현을 바꿔도 의미 있는 보호가 남는다.")
            ),
            workedExample = """
                요구사항: 19세 이상 가입 가능

                약한 test
                age=30 → success

                경계 test
                age=18 → reject
                age=19 → success
                age=20 → success
                age=-1 → invalid
                age=null → invalid

                한 규칙을 정상값 + 경계 + 잘못된 입력으로 나눠 검증
            """,
            mistakes = listOf(
                "테스트가 통과하면 프로그램 전체가 완전히 맞다고 생각한다.",
                "정상 입력만 여러 개 넣고 경계값을 테스트하지 않는다.",
                "모든 dependency를 mock하고 실제 DB/API 연결 검증을 하지 않는다.",
                "private 함수 호출 횟수 같은 구현 세부를 과도하게 검사한다.",
                "flaky test를 원인 조사 없이 retry로만 숨긴다."
            ),
            questions = listOf(
                "property-based testing은 예시 몇 개와 어떻게 다를까?",
                "contract test는 service 사이 API 변경을 어떻게 잡을까?",
                "test fixture가 너무 크면 어떤 유지보수 문제가 생길까?",
                "mutation testing은 테스트 품질을 어떤 방식으로 평가할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C10-S03",
            topics = listOf(
                depthTopic("Git은 파일 백업보다 변경 그래프를 관리한다", "commit은 특정 시점의 project 상태와 parent 관계를 기록한다. branch는 commit을 가리키는 이름이고 merge/rebase는 서로 다른 변경 흐름을 연결하는 방법이다. 파일을 복사해 final2, final3로 저장하는 방식과 달리 누가 왜 어떤 변경을 했는지 추적할 수 있다."),
                depthTopic("좋은 commit은 작고 한 이유로 묶인다", "UI 색 변경과 DB schema 변경과 버그 수정을 한 commit에 섞으면 review와 rollback이 어려워진다. 하나의 의도와 검증 근거를 가진 작은 commit은 문제 발생 시 원인을 찾고 일부만 되돌리기 쉽다."),
                depthTopic("build artifact와 source code를 구분한다", "APK, JAR, bundle 같은 artifact는 source를 특정 compiler, dependency, 설정으로 변환한 배포 결과물이다. 같은 source라도 build environment나 dependency가 달라지면 artifact가 달라질 수 있다. reproducible build와 version pinning이 중요한 이유다."),
                depthTopic("deploy와 release는 같은 단어처럼 보여도 운영에서는 구분할 수 있다", "server에 새 code를 올려도 feature flag로 사용자에게 아직 노출하지 않을 수 있고, mobile app release는 store 배포와 사용자 update 시점이 다를 수 있다. 배포 기술 단계와 사용자 기능 활성화를 분리하면 위험을 줄일 수 있다."),
                depthTopic("rollback도 변경 종류에 따라 난이도가 다르다", "application binary는 이전 버전으로 돌아가기 쉬울 수 있지만 DB migration으로 column을 삭제했거나 data format을 바꿨다면 단순 code rollback이 실패할 수 있다. backward-compatible migration, expand-and-contract 전략을 생각하는 이유다.")
            ),
            workedExample = """
                feature 개발
                commit A: DB에 nullable new_column 추가
                commit B: app이 old/new 둘 다 읽도록 수정
                deploy
                data backfill
                commit C: app이 new만 사용
                충분히 안정화 후 old column 제거

                한 번에 column 삭제 + app 변경보다 rollback 가능성이 높음
            """,
            mistakes = listOf(
                "branch를 파일 복사본과 같은 개념으로 생각한다.",
                "서로 무관한 변경을 큰 commit 하나에 몰아 넣는다.",
                "source가 같으면 어느 기기에서 build해도 artifact가 반드시 같다고 생각한다.",
                "deploy 성공을 사용자 기능 정상과 같은 의미로 본다.",
                "DB migration이 있는 release도 code만 이전 버전으로 돌리면 된다고 생각한다."
            ),
            questions = listOf(
                "merge와 rebase는 commit graph를 어떻게 다르게 만들까?",
                "semantic versioning은 어떤 contract 변경을 표현하려 할까?",
                "canary release와 blue-green deployment는 위험을 어떻게 줄일까?",
                "feature flag를 오래 남겨 두면 어떤 기술 부채가 생길까?"
            )
        ),

        depthPack(
            sectionId = "V1-C11-S01",
            topics = listOf(
                depthTopic("아키텍처는 멋진 폴더 이름보다 변경을 어디에 가두는가의 문제다", "결제 provider를 바꿨을 때 결제 모듈만 주로 수정되고 주문·회원 전체가 함께 바뀌지 않도록 경계를 만드는 것이 좋은 설계의 한 예다. cohesion은 같은 책임을 모으고 coupling은 서로의 변경 영향이 얼마나 큰지 보는 관점이다."),
                depthTopic("요구사항을 기능 목록만으로 받으면 비기능 요구를 놓친다", "동시에 몇 명이 쓰는지, 응답 시간 목표, 장애 허용 수준, 개인정보 보관 규칙, 비용 제한도 architecture에 영향을 준다. 초당 1건 service와 초당 10만 건 service가 같은 구조일 필요는 없다."),
                depthTopic("interface는 모든 클래스 앞에 붙이는 장식이 아니다", "변경 가능성이 크거나 외부 dependency와 경계를 만들고 싶을 때 추상 계약이 유용하다. 실제 구현이 하나뿐이고 바뀔 이유도 없는 작은 내부 helper까지 interface로 감싸면 이해 비용만 늘 수 있다."),
                depthTopic("상태를 어디에 둘지 결정하는 것이 설계의 큰 부분이다", "사용자 session을 process memory에만 두면 server가 여러 대일 때 다른 instance에서 찾지 못할 수 있다. DB, distributed cache, client 등 상태 위치를 정하면 consistency, latency, 장애 복구 방식이 함께 달라진다."),
                depthTopic("설계 문서는 결론보다 trade-off를 남겨야 한다", "왜 queue를 넣었는지, 왜 strong consistency 대신 eventual consistency를 허용했는지 같은 이유가 남아야 미래 변경자가 같은 논쟁을 반복하지 않는다. ADR처럼 선택지, 결정, 장단점을 짧게 기록하는 습관이 유용하다.")
            ),
            workedExample = """
                요구사항: 이미지 업로드 후 썸네일 생성

                질문
                업로드 response를 썸네일 완료까지 기다려야 하나?
                초당 업로드 몇 건인가?
                원본 이미지는 어디 보관하나?
                처리 실패 시 재시도 가능한가?

                선택 예
                API → object storage 저장 → queue → image worker

                장점: request 빠름, worker 확장 가능
                비용: queue·상태·retry·관측성 추가
            """,
            mistakes = listOf(
                "폴더와 class를 많이 나누면 자동으로 좋은 architecture라고 생각한다.",
                "기능 요구만 보고 성능·보안·비용·장애 요구를 묻지 않는다.",
                "모든 구체 class에 interface를 만들어 추상화 수만 늘린다.",
                "state 위치를 명시하지 않고 server 수를 늘린다.",
                "설계 결론만 남기고 왜 그 선택을 했는지 기록하지 않는다."
            ),
            questions = listOf(
                "modular monolith와 microservices는 경계를 어떻게 다르게 배치할까?",
                "bounded context 같은 domain 경계는 언제 도움이 될까?",
                "stateful service를 scale out할 때 어떤 문제가 생길까?",
                "architecture decision을 다시 평가해야 하는 신호는 무엇일까?"
            )
        ),
        depthPack(
            sectionId = "V1-C11-S02",
            topics = listOf(
                depthTopic("cache와 queue는 모두 중간에 두는 기술이지만 목적이 다르다", "cache는 이미 얻은 결과를 재사용해 읽기 latency와 원본 부하를 줄이고, queue는 처리할 일을 저장해 producer와 consumer의 속도와 시간을 분리한다. 하나는 결과 재사용, 다른 하나는 작업 전달이라는 차이를 먼저 잡는다."),
                depthTopic("eventual consistency는 틀린 데이터를 허용한다는 말이 아니다", "여러 복제본이나 service 상태가 즉시 같지 않아도 일정 시간이 지나면 일치하도록 설계하는 모델이다. 사용자에게 어느 정도 지연을 허용할지, 충돌을 어떻게 해결할지, 아직 반영 중임을 어떻게 보여 줄지가 중요하다."),
                depthTopic("backpressure가 없으면 빠른 producer가 느린 consumer를 압도한다", "초당 10만 message가 들어오는데 worker가 1만 개밖에 처리하지 못하면 queue backlog가 계속 늘어난다. queue 길이, 처리율, 지연을 측정하고 producer rate limit, worker 확장, batch 처리, load shedding을 고려한다."),
                depthTopic("retry는 중복 side effect와 연결된다", "message 처리 중 DB 저장은 성공했지만 acknowledgement 전에 consumer가 죽으면 같은 message가 다시 전달될 수 있다. idempotent handler, deduplication key, transaction boundary를 설계해야 중복 결제나 중복 이메일 문제를 줄일 수 있다."),
                depthTopic("distributed lock은 마지막 선택일 수 있다", "여러 process가 같은 자원을 동시에 바꾸지 못하게 lock을 둘 수 있지만 lock service 장애, 만료, network partition, deadlock 같은 복잡성이 생긴다. DB constraint, atomic update, optimistic concurrency처럼 더 단순한 방법으로 해결 가능한지 먼저 본다.")
            ),
            workedExample = """
                producer: 주문 event 초당 20,000개 생성
                consumer: 초당 12,000개 처리

                1분 뒤 backlog 약 480,000개 증가 가능

                관찰할 값
                queue depth
                oldest message age
                consumer error rate
                processing throughput

                해결 후보
                worker 확장 / batch / producer 제한 / 불필요 event 제거
            """,
            mistakes = listOf(
                "cache와 queue를 둘 다 속도를 높이는 같은 기술로 생각한다.",
                "eventual consistency를 데이터 정확성을 포기하는 것으로 이해한다.",
                "queue가 있으니 producer가 얼마나 빨리 보내도 괜찮다고 생각한다.",
                "message가 정확히 한 번만 전달된다고 가정해 side effect를 중복 실행한다.",
                "동시성 문제를 보면 무조건 distributed lock부터 도입한다."
            ),
            questions = listOf(
                "consumer group은 message를 여러 worker에 어떻게 나눌까?",
                "exactly-once라는 표현은 어떤 범위에서만 의미가 있을까?",
                "cache consistency와 database transaction consistency는 어떻게 다를까?",
                "optimistic concurrency control은 version column을 어떻게 사용할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C11-S03",
            topics = listOf(
                depthTopic("scale up과 scale out은 비용 구조와 실패 구조가 다르다", "scale up은 한 장비의 CPU·RAM을 키워 단순하지만 상한과 단일 장비 장애가 있고, scale out은 여러 instance로 나눠 처리량과 장애 분산이 가능하지만 load balancing, shared state, 배포 복잡성이 생긴다. 무조건 분산부터 시작할 이유는 없다."),
                depthTopic("bottleneck을 측정하지 않고 server 수부터 늘리면 해결이 안 될 수 있다", "문제가 느린 DB query, 외부 API, lock contention이라면 web server를 두 배로 늘려도 같은 병목에 요청이 더 몰릴 수 있다. CPU, memory, DB connections, latency trace로 가장 제한적인 자원을 먼저 찾는다."),
                depthTopic("circuit breaker는 retry와 반대 방향의 보호를 한다", "retry는 실패한 요청을 다시 시도하지만 circuit breaker는 일정 실패율을 넘으면 잠시 호출 자체를 막아 외부 장애에 계속 매달리지 않게 한다. timeout, 제한된 retry, circuit breaker를 함께 설계하면 장애 전파를 줄일 수 있다."),
                depthTopic("SLO는 monitoring 숫자를 사용자 약속과 연결한다", "latency 123ms라는 metric 자체보다 ‘정상 요청의 99.9%를 500ms 안에 처리한다’처럼 서비스 목표를 정하면 어떤 장애를 우선 해결할지 판단하기 쉽다. SLI는 실제 측정값, SLO는 목표 수준으로 생각할 수 있다."),
                depthTopic("종합 프로젝트에서는 코드보다 검증 경로를 설계해야 한다", "요구사항→데이터 모델→API→권한→실패 시나리오→테스트→배포→관측성까지 연결해야 실제 운영 가능한 시스템이 된다. AI가 코드를 빠르게 만들어도 race, permission, rollback, production evidence는 사람이 검증 기준을 세워야 한다.")
            ),
            workedExample = """
                서비스가 갑자기 느려짐

                metric
                CPU 35% 정상
                DB connection 98% 사용
                p99 query latency 급증

                trace
                request 4초 중 DB query 3.6초

                결론 후보
                web server 부족이 아니라 DB 병목

                먼저 query/index/connection 사용 원인을 해결
                측정 없이 server 수만 늘리지 않음
            """,
            mistakes = listOf(
                "traffic이 늘면 원인 측정 없이 instance부터 늘린다.",
                "분산 시스템이 단일 service보다 항상 더 고급이고 더 좋다고 생각한다.",
                "retry 횟수를 늘리면 외부 service 장애가 해결된다고 생각한다.",
                "dashboard 숫자를 많이 만들고 사용자 관점의 SLO는 정하지 않는다.",
                "AI가 만든 코드가 테스트를 통과하면 운영 실패 시나리오 검증도 끝났다고 생각한다."
            ),
            questions = listOf(
                "load balancer는 unhealthy instance를 어떻게 제외할까?",
                "bulkhead는 connection pool과 worker pool을 어떻게 분리할 수 있을까?",
                "error budget은 기능 개발과 안정성 투자를 어떻게 조정할까?",
                "chaos test는 실제 장애 전에 어떤 복구 능력을 확인하려 할까?"
            )
        )
    )
}
