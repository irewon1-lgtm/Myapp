package com.futuretech.poweruser.textbook

/** Resolves audited beginner guidance after the learner-facing V3 grouping is finalized. */
object V3BeginnerGuidanceResolver {
    private val genericMustUnderstand = listOf(
        "이번 LESSON의 핵심 흐름을 자기 말로 설명한다.",
        "본문 예시에서 ‘왜 필요한가 → 어떻게 동작하는가 → 어디서 실패하는가’를 따라간다.",
        "용어를 외우기보다 실제 코드·앱 상황과 연결해 설명한다."
    )

    private fun h(term: String, meaning: String) = BeginnerTermHint(term, meaning)

    private fun g(
        id: String,
        terms: List<BeginnerTermHint> = emptyList(),
        answers: List<String>,
        deep: Boolean = false,
        defer: List<String> = emptyList()
    ) = BeginnerLessonGuide(
        sectionId = id,
        focus = if (deep) BeginnerLessonFocus.CORE_WITH_DEEP_DIVE else BeginnerLessonFocus.CORE,
        mustUnderstand = genericMustUnderstand,
        termHints = terms,
        canDefer = if (deep) defer.ifEmpty { listOf("세부 구현과 예외 규칙은 중급 단계에서 다시 학습") } else emptyList(),
        answerPoints = answers
    )

    private val overrides = listOf(
        g(
            "V1-C01-S01",
            terms = listOf(h("CPU", "프로그램 명령을 계산하고 처리하는 핵심 장치"), h("RAM", "실행 중 필요한 값을 빠르게 두는 작업 공간")),
            answers = listOf("컴퓨터 동작은 입력→처리→출력으로 나눠 볼 수 있다.", "CPU는 명령을 처리하고 RAM은 실행 중 값을 빠르게 둔다.", "저장장치는 파일과 프로그램을 전원이 꺼져도 남도록 보관한다.", "코드가 실행됐다는 사실과 결과가 맞다는 사실은 다르다.")
        ),
        g(
            "V1-C01-S02",
            terms = listOf(h("path", "파일이나 폴더가 저장된 위치를 나타내는 경로"), h("bit / byte", "0·1 한 자리 / 보통 8bit를 묶은 데이터 단위")),
            answers = listOf("파일은 이름만이 아니라 실제 byte와 형식으로 저장된다.", "폴더는 파일을 계층적으로 정리하고 path는 그 위치를 나타낸다.", "확장자는 형식의 힌트이지 파일 내용 자체를 바꾸지 않는다.", "bit와 byte는 저장 용량과 실제 데이터 표현의 기본 단위다.")
        ),
        g(
            "V1-C01-S03",
            terms = listOf(h("encoding", "문자를 저장·전송 가능한 byte 표현으로 바꾸는 규칙"), h("Unicode", "문자에 공통 번호를 부여한 문자 표준"), h("UTF-8", "Unicode 문자를 byte로 표현하는 대표적인 encoding")),
            answers = listOf("컴퓨터는 글자 모양 자체가 아니라 정해진 byte 표현을 저장한다.", "Unicode는 문자 번호 체계이고 UTF-8은 그 문자를 byte로 표현하는 방식이다.", "저장할 때와 읽을 때 encoding이 다르면 글자가 깨질 수 있다.", "encoding은 encryption과 목적이 다르다."),
            deep = true,
            defer = listOf("Unicode normalization 세부", "UTF-8 byte 패턴 암기")
        ),
        g(
            "V1-C01-S04",
            terms = listOf(h("kernel", "운영체제에서 CPU·메모리·장치 같은 핵심 자원을 관리하는 중심 부분"), h("process", "저장된 프로그램이 실제 실행 중인 한 실행 단위")),
            answers = listOf("프로그램 파일이 저장돼 있는 것과 process로 실행되는 것은 다르다.", "운영체제는 프로그램이 하드웨어 자원을 안전하게 쓰도록 중간에서 관리한다.", "소스 코드는 사람이 작성한 표현이고 실행 환경은 그것을 실제 동작으로 연결한다.", "문법 오류·실행 중 오류·논리 오류는 서로 다른 문제다."),
            deep = true,
            defer = listOf("kernel 내부 scheduler", "compiler·interpreter 구현 세부")
        ),
        g(
            "V1-C01-S05",
            terms = listOf(h("syntax error", "언어 문법 규칙에 맞지 않아 코드 해석 단계에서 생기는 오류"), h("logic error", "실행은 되지만 사람이 원한 규칙과 결과가 다른 오류")),
            answers = listOf("코드는 한 줄씩 입력→계산→출력 흐름으로 읽는다.", "일부 값을 바꿔 결과가 어떻게 달라지는지 직접 실행해 본다.", "오류 메시지는 실패 위치와 원인을 좁히는 정보다.", "실행→관찰→수정→재실행이 첫 디버깅 루프다.")
        ),

        g(
            "V1-C03-S01",
            terms = listOf(h("node", "값과 다른 데이터로 이어지는 연결 정보를 함께 가진 한 조각"), h("amortized cost", "가끔 비싼 작업을 포함해 여러 작업 전체 비용을 평균해서 보는 방식"), h("hash collision", "서로 다른 key가 같은 hash 위치를 원하게 되는 상황")),
            answers = listOf("array 계열은 index 접근에 강하고 linked list는 연결 변경이 중요한 상황에 유리할 수 있다.", "node는 값과 연결 정보를 가진다.", "stack은 LIFO, queue는 FIFO다.", "hash table은 key를 빠르게 찾도록 hash를 이용하지만 collision 처리도 필요하다."),
            deep = true,
            defer = listOf("dynamic array resize 정책", "hash collision 구현 세부")
        ),
        g(
            "V1-C03-S02",
            terms = listOf(h("BST", "작은 값은 왼쪽, 큰 값은 오른쪽에 배치하는 검색용 이진 트리"), h("heap", "최소·최대 같은 우선 항목을 빠르게 꺼내기 위한 트리 기반 구조"), h("trie", "문자열의 공통 앞부분을 경로로 공유하는 트리 구조")),
            answers = listOf("tree는 부모-자식 계층을 표현한다.", "binary tree와 BST는 같은 말이 아니다.", "BST는 한쪽으로 치우치면 검색 장점이 줄어든다.", "heap은 전체 정렬보다 우선순위가 높은 값을 빠르게 꺼내는 데 초점이 있다.", "trie는 접두사 검색과 자동완성 같은 문제에 연결된다."),
            deep = true,
            defer = listOf("AVL/Red-Black 회전", "heapify 구현", "trie 메모리 최적화")
        ),
        g(
            "V1-C03-S03",
            terms = listOf(h("adjacency list", "각 graph node마다 연결된 이웃 목록을 저장하는 방식"), h("binary search", "정렬된 범위에서 가운데를 비교해 탐색 범위를 절반씩 줄이는 방법"), h("divide and conquer", "큰 문제를 작은 문제로 나누어 해결한 뒤 합치는 전략")),
            answers = listOf("graph는 vertex와 edge로 복잡한 연결 관계를 표현한다.", "cycle이 있으면 visited 없이 같은 node를 반복할 수 있다.", "binary search는 데이터가 정렬돼 있다는 전제가 필요하다.", "정렬 알고리즘은 같은 결과를 만들어도 작업 과정과 비용이 다르다."),
            deep = false
        ),
        g(
            "V1-C03-S04",
            terms = listOf(h("Big-O", "입력이 커질 때 작업량이 어떤 속도로 늘어나는지 나타내는 표기"), h("memoization", "한 번 계산한 결과를 저장해 같은 계산을 반복하지 않는 방법"), h("invariant", "알고리즘 진행 중 계속 참이어야 하는 성질")),
            answers = listOf("O(1)·O(log n)·O(n)·O(n²)는 입력 증가에 따른 작업량 증가 형태가 다르다.", "재귀에는 반드시 종료 조건과 종료 쪽으로 가까워지는 변화가 필요하다.", "BFS는 queue, DFS는 stack 또는 recursion과 자연스럽게 연결된다.", "greedy는 현재 최선이 전체 최선으로 이어진다는 근거가 필요하다.", "memoization은 반복 계산을 줄인다."),
            deep = true,
            defer = listOf("복잡도 수학적 증명", "DP 점화식 심화", "고급 graph 알고리즘")
        ),

        g(
            "V1-C06-S01",
            terms = listOf(h("NAT", "내부 private IP 통신을 외부 public IP 통신과 연결하는 주소 변환 방식"), h("latency / bandwidth", "응답 지연 시간 / 일정 시간에 보낼 수 있는 데이터 양")),
            answers = listOf("Wi-Fi 연결은 외부 Internet 전체 정상과 같은 뜻이 아니다.", "DNS는 domain 이름을 IP 정보와 연결한다.", "IP는 목적지 장치를, port는 그 장치의 network service를 구분한다.", "TCP와 UDP는 신뢰성·연결·지연 요구에서 역할이 다르다.", "latency와 bandwidth는 서로 다른 성능 문제다."),
            deep = true,
            defer = listOf("DNS root/TLD 세부", "TCP 혼잡제어", "QUIC 내부 구현")
        ),
        g(
            "V1-C06-S02",
            terms = listOf(h("TLS", "HTTP 내용을 보내기 전에 암호화된 통신 채널을 만들고 상대 확인을 돕는 보안 프로토콜"), h("certificate", "접속한 domain과 public key 신원을 연결하는 디지털 증명서"), h("idempotency", "같은 요청을 반복해도 최종 상태 효과가 중복되지 않는 성질")),
            answers = listOf("HTTPS는 HTTP를 TLS로 보호해 사용한다.", "certificate는 접속한 domain과 서버 신원을 확인하는 과정에 쓰인다.", "URL은 scheme·host·port·path·query·fragment로 역할을 나눠 읽는다.", "HTTP request/response는 method·status·header·body를 구분해 읽는다.", "network 실패와 HTTP 오류 응답은 다른 단계다."),
            deep = true,
            defer = listOf("TLS handshake 암호학 세부", "HTTP version별 framing")
        ),
        g(
            "V1-C06-S03",
            terms = listOf(h("MIME type", "데이터가 HTML·JSON·PNG처럼 어떤 종류인지 나타내는 표준 이름"), h("boundary", "multipart body 안에서 여러 part가 어디서 나뉘는지 표시하는 경계 문자열")),
            answers = listOf("MIME type은 byte 데이터가 어떤 종류인지 알려 준다.", "Content-Type은 현재 HTTP body의 실제 종류를 알려 준다.", "Accept는 client가 response로 받을 수 있거나 원하는 형식을 표현한다.", "multipart/form-data는 text와 file을 여러 part로 담고 boundary로 구분한다.", "FormData에서는 browser가 boundary를 포함한 Content-Type을 만들도록 두는 것이 일반적이다."),
            deep = false
        ),
        g(
            "V1-C06-S04",
            terms = listOf(h("endpoint", "method와 path로 구분되는 구체적인 API 호출 지점"), h("authentication / authorization", "사용자가 누구인지 확인 / 그 사용자가 무엇을 할 수 있는지 확인"), h("Bearer token", "그 값을 가진 사람이 권한을 행사할 수 있으므로 노출을 막아야 하는 인증 증표")),
            answers = listOf("API는 프로그램이 다른 프로그램의 기능과 데이터를 사용하는 계약이다.", "REST와 JSON은 같은 개념이 아니다.", "pagination은 너무 많은 데이터를 한 번에 보내는 비용을 줄인다.", "authentication과 authorization은 서로 다른 질문이다.", "cookie/session과 bearer token은 로그인 상태를 전달하는 방식이 다르다."),
            deep = true,
            defer = listOf("JWT 표준 세부", "OAuth/OIDC 심화")
        ),
        g(
            "V1-C06-S05",
            terms = listOf(h("preflight", "일부 cross-origin 요청 전에 OPTIONS로 허용 여부를 먼저 묻는 절차"), h("ETag", "client가 가진 자원 버전과 server 자원 변경 여부를 비교할 수 있게 하는 식별값"), h("SSE / WebSocket", "server→client event stream / 양방향 지속 message 연결")),
            answers = listOf("origin은 scheme+host+port 조합으로 구분한다.", "CORS는 browser의 cross-origin response 접근 정책이지 API 권한 방화벽이 아니다.", "cache는 빠르지만 오래된 데이터를 보여 줄 수 있어 갱신 정책이 필요하다.", "SSE와 WebSocket은 실시간 통신 방향과 요구가 다르다.", "Network 패널에서는 DNS/TLS/HTTP/status/body/CORS/cache 단계를 나눠 진단한다."),
            deep = true,
            defer = listOf("HTTP cache directive 전체", "WebSocket framing")
        ),

        g(
            "V1-C07-S01",
            terms = listOf(h("middleware", "request가 handler에 도달하기 전후에 공통 처리를 연결하는 단계"), h("parsing / validation", "데이터 형식을 읽기 / 우리 시스템 규칙에 맞는지 검사하기")),
            answers = listOf("server process는 port에서 request를 기다리고 route가 알맞은 handler로 보낸다.", "localhost는 현재 실행 중인 그 device 자신을 뜻한다.", "route parameter·query·body는 모두 외부 입력이므로 검증해야 한다.", "parsing과 validation은 서로 다른 일이다.", "middleware는 순서가 실제 동작에 영향을 준다.")
        ),
        g(
            "V1-C07-S02",
            terms = listOf(h("DTO", "계층 경계에서 전달할 데이터를 정한 입력·출력 모양"), h("dependency injection", "필요한 실제 구현을 내부에서 만들지 않고 밖에서 전달받는 방식"), h("repository", "업무 로직이 저장소 세부 구현을 직접 알지 않게 만드는 데이터 접근 경계")),
            answers = listOf("handler는 HTTP 입력·출력에 집중하고 service는 업무 규칙에 집중할 수 있다.", "repository는 DB 접근 세부를 경계 뒤로 숨길 수 있다.", "모든 작은 앱에 layer를 무조건 늘리는 것은 좋은 설계가 아니다.", "DI를 쓰면 실제 DB·결제 대신 fake를 넣어 핵심 규칙을 테스트하기 쉽다.")
        ),
        g(
            "V1-C07-S03",
            terms = listOf(h("IDOR/BOLA", "ID만 바꿔 다른 사람 object에 접근할 수 있게 되는 권한 누락 취약점"), h("DLQ", "여러 번 실패한 message를 정상 queue에서 분리해 조사하는 dead-letter queue"), h("outbox", "DB 변경과 후속 event 기록을 같은 transaction에 남겨 event 유실을 줄이는 패턴")),
            answers = listOf("인증은 누구인지, 권한은 무엇을 할 수 있는지 확인한다.", "UI에서 버튼을 숨기는 것은 server authorization을 대체하지 못한다.", "cache에는 invalidation과 source of truth 문제가 있다.", "queue 작업은 중복 전달될 수 있어 idempotency가 중요하다.", "retry 가능한 오류와 반복해도 해결되지 않는 오류를 구분해야 한다."),
            deep = true,
            defer = listOf("cache stampede 구현", "분산 saga/compensation", "outbox relay 세부")
        ),
        g(
            "V1-C07-S04",
            terms = listOf(h("observability", "logs·metrics·traces 등으로 시스템 내부 상태를 추측이 아니라 증거로 보는 능력"), h("p95", "전체 요청 중 95%가 그 시간 이하로 끝나는 지연시간 지표"), h("graceful shutdown", "새 요청을 멈추고 진행 중 작업과 연결을 정리한 뒤 process를 종료하는 절차")),
            answers = listOf("동시 요청이 shared state를 바꾸면 race condition이 생길 수 있다.", "DB connection pool과 rate limit은 무한 자원 가정을 막는다.", "log는 개별 사건, metric은 전체 추세, trace는 한 요청의 경로를 보여 준다.", "health check는 process 생존과 traffic 준비 상태를 구분할 수 있다.", "graceful shutdown은 배포 중 진행 중인 작업을 갑자기 끊지 않게 한다."),
            deep = true,
            defer = listOf("OpenTelemetry protocol 세부", "고급 backpressure 알고리즘")
        ),

        g(
            "V1-C08-S01",
            terms = listOf(h("schema", "table·column·type 등 데이터 구조와 규칙을 정한 설계"), h("parameterized query", "SQL 문법과 사용자 값을 분리해 전달하는 query 방식")),
            answers = listOf("DB는 많은 데이터를 구조와 규칙 아래 저장·검색·수정한다.", "table은 같은 종류의 row를 모으고 column은 속성 종류를 나타낸다.", "schema는 데이터 모양과 규칙의 설계다.", "SQL CRUD는 INSERT·SELECT·UPDATE·DELETE와 연결된다.", "UPDATE/DELETE에서는 WHERE 누락을 특히 조심해야 한다.")
        ),
        g(
            "V1-C08-S02",
            terms = listOf(h("data integrity", "데이터가 시스템 규칙과 관계를 깨지 않고 올바른 상태를 유지하는 성질"), h("referential integrity", "foreign key가 실제 존재하는 row를 가리키도록 유지되는 성질"), h("constraint", "DB가 잘못된 값이나 관계를 저장하지 못하게 두는 규칙")),
            answers = listOf("GROUP BY는 같은 기준의 row를 묶고 aggregate를 계산한다.", "WHERE는 group 전 row를, HAVING은 group 결과를 필터한다.", "PRIMARY KEY는 대표 고유 식별자이고 UNIQUE는 다른 중복도 막는다.", "CHECK는 type만으로 막지 못하는 값 범위를 지킬 수 있다.", "FOREIGN KEY는 참조 무결성을 지키는 데 도움을 준다.")
        ),
        g(
            "V1-C08-S03",
            terms = listOf(h("junction table", "N:M 관계를 두 개의 1:N 관계로 풀면서 관계 자체 데이터를 저장하는 table"), h("normalization", "중복과 갱신 이상을 줄이도록 데이터를 역할별 table로 나누는 설계 원칙"), h("N+1", "목록 1번 조회 뒤 각 항목마다 추가 query를 반복해 query 수가 폭증하는 문제")),
            answers = listOf("1:N과 N:M 관계는 table 구조가 다르다.", "N:M은 junction table로 표현할 수 있다.", "과거 주문 가격처럼 역사 값은 현재 product 값과 분리해 snapshot할 수 있다.", "INNER JOIN은 matching row만, LEFT JOIN은 왼쪽 row를 모두 남긴다.", "ORM을 사용해도 실제 SQL과 N+1 여부를 확인해야 한다.")
        ),
        g(
            "V1-C08-S04",
            terms = listOf(h("selectivity", "조건이 전체 row를 얼마나 적은 후보로 잘 좁히는지 보는 정도"), h("optimizer", "DB가 여러 실행 방법의 비용을 추정해 query plan을 고르는 구성요소"), h("WAL", "commit된 변경을 장애 뒤 복구할 수 있도록 먼저 기록하는 write-ahead log 계열 메커니즘")),
            answers = listOf("index는 read를 빠르게 할 수 있지만 storage와 write 비용을 만든다.", "복합 index는 column 순서가 중요하다.", "EXPLAIN은 DB가 선택한 query plan을 보여 준다.", "transaction은 여러 변경을 commit/rollback 한 단위로 묶는다.", "ACID는 atomicity·consistency·isolation·durability를 뜻한다.", "lock을 오래 잡으면 대기와 deadlock 위험이 커진다."),
            deep = true,
            defer = listOf("B-tree page 내부 구조", "optimizer cost model", "isolation level 구현 세부")
        ),

        g(
            "V1-C09-S01",
            terms = listOf(h("trust boundary", "이 선을 넘어온 값은 그대로 믿지 않고 다시 검증한다고 정한 경계"), h("CIA triad", "기밀성·무결성·가용성이라는 세 가지 큰 보안 목표"), h("least privilege", "작업에 필요한 최소 권한만 부여해 침해 피해 범위를 줄이는 원칙")),
            answers = listOf("asset은 노출·변조·중단되면 문제가 되는 보호 대상이다.", "기밀성·무결성·가용성은 각각 읽기·변경·사용 가능 상태를 보호한다.", "authentication과 authorization은 다른 질문이다.", "browser→server 같은 trust boundary에서는 입력을 다시 검증한다.", "최소 권한은 계정이 탈취돼도 가능한 피해를 줄인다.")
        ),
        g(
            "V1-C09-S02",
            terms = listOf(h("salt", "같은 비밀번호라도 저장 결과가 달라지게 해 미리 계산한 공격표 재사용을 어렵게 하는 무작위 값"), h("digital signature", "내용을 숨기기보다 제작자와 변조 여부를 검증하는 서명"), h("certificate", "public key와 domain 신원을 연결해 검증할 수 있게 하는 증명서")),
            answers = listOf("encoding은 표현 변경이고 encryption은 key로 내용을 보호한다.", "cryptographic hash는 원본을 복호화하기 위한 변환이 아니다.", "비밀번호에는 빠른 SHA-256 한 번보다 전용 password hashing을 사용한다.", "salt는 숨겨야 하는 두 번째 비밀번호가 아니다.", "digital signature는 encryption과 목적이 다르다.", "certificate는 TLS에서 public key와 신원을 연결하는 데 도움을 준다."),
            deep = true,
            defer = listOf("암호 알고리즘 수학", "PKI chain 세부")
        ),
        g(
            "V1-C09-S03",
            terms = listOf(h("SSRF", "사용자가 준 주소를 server가 대신 요청하게 해 내부 주소까지 접근시키는 공격"), h("path traversal", "../ 같은 경로 표현으로 허용 폴더 밖 파일에 접근하려는 공격"), h("defense in depth", "하나의 방어에만 의존하지 않고 여러 층의 검증과 제한을 겹쳐 두는 방식")),
            answers = listOf("SQL injection은 사용자 데이터가 SQL 문법으로 해석될 때 위험해진다.", "XSS는 사용자 데이터가 browser의 실행 가능한 HTML/JS로 해석될 때 발생할 수 있다.", "CSRF와 CORS는 같은 문제를 해결하지 않는다.", "파일 이름과 Content-Type은 client가 조작할 수 있으므로 upload를 별도로 검증한다.", "secret은 source·log·client 앱에 그대로 넣지 않는다.", "보안은 validation·인증·권한·rate limit·secret 관리 같은 여러 층으로 만든다."),
            deep = true,
            defer = listOf("공격 bypass 기법", "고급 exploit chain")
        ),

        g(
            "V1-C10-S01",
            terms = listOf(h("minimal reproduction", "큰 앱 문제를 가장 작은 코드·입력으로 줄여 같은 실패를 재현하는 것"), h("stack trace", "오류까지 어떤 함수 호출 경로를 거쳤는지 보여 주는 기록")),
            answers = listOf("증상과 원인을 구분하고 expected와 actual을 먼저 적는다.", "같은 조건에서 재현할 수 있어야 원인 후보를 좁힐 수 있다.", "FACT와 HYPOTHESIS를 분리하고 한 가설에 한 실험을 한다.", "로그·debugger·stack trace는 실제 실행 경로와 상태를 확인하는 증거다.", "minimal reproduction은 큰 앱 문제를 작은 실험으로 줄여 원인을 찾기 쉽게 한다.")
        ),
        g(
            "V1-C10-S02",
            terms = listOf(h("regression test", "한번 고친 버그가 다시 생기지 않는지 같은 실패 조건을 반복 검사하는 테스트"), h("test double", "실제 DB·결제·네트워크 대신 테스트에서 역할을 대신하는 fake·stub·mock 같은 대체물"), h("flaky test", "같은 코드인데 timing·공유상태 때문에 PASS와 FAIL이 불안정하게 바뀌는 테스트")),
            answers = listOf("unit·integration·E2E는 검증하는 범위와 비용이 다르다.", "boundary와 실패 입력까지 확인해야 정상 예제 한두 개보다 강한 증거가 된다.", "고친 버그는 같은 조건을 자동 재현하는 regression test로 남긴다.", "test double은 외부 의존성을 통제해 핵심 규칙을 안정적으로 검증하게 한다.", "flaky test는 무시할 대상이 아니라 원인을 찾아 안정화해야 할 신뢰성 문제다.", "테스트 PASS는 실행한 조건의 성공 증거이지 버그 0개의 증명이 아니다.")
        ),
        g(
            "V1-C10-S03",
            terms = listOf(h("artifact", "build가 만든 APK·AAB·bundle처럼 테스트·배포할 수 있는 결과물"), h("migration / rollback", "DB 구조 변경 절차 / 문제 배포를 이전 안정 상태로 되돌리는 절차"), h("CI", "정해 둔 build·test·검사를 자동으로 반복 실행해 변경을 검증하는 절차")),
            answers = listOf("repository·working tree·commit·branch는 변경 상태와 역사를 서로 다른 역할로 관리한다.", "merge conflict는 자동 정답이 없어 사람이 최종 의도를 결정해야 하는 상황이다.", "main merge와 production 배포는 서로 다른 단계다.", "build 결과 artifact는 어떤 commit·설정·테스트에서 만들어졌는지 추적 가능해야 한다.", "CI PASS는 정의된 검사 범위가 성공했다는 뜻이지 미실행 검증까지 성공했다는 뜻이 아니다.", "문제 배포에는 migration 호환성과 rollback 경로를 함께 준비해야 한다.", "실제로 실행하지 않은 검증을 PASS라고 보고하지 않는다.")
        ),

        g(
            "V1-C11-S01",
            terms = listOf(h("cohesion", "한 모듈의 코드가 같은 목적과 책임에 얼마나 잘 모여 있는지 보는 관점"), h("coupling", "한 모듈 변경이 다른 모듈 변경까지 얼마나 강하게 요구하는지 보는 관점"), h("dependency inversion", "핵심 규칙이 구체 DB·SDK보다 추상 계약에 의존하도록 만드는 방향")),
            answers = listOf("functional requirement는 기능, non-functional requirement는 품질 조건이다.", "acceptance criteria는 완료를 실제 테스트 가능한 문장으로 만든다.", "responsibility와 module은 변경 이유를 기준으로 경계를 나눈다.", "interface는 사용할 수 있는 계약이고 implementation은 실제 동작 방식이다.", "높은 cohesion과 낮은 불필요한 coupling은 수정·테스트 범위를 줄인다.", "DI는 핵심 규칙을 구체 기술에서 분리하기 쉽게 한다."),
            deep = true,
            defer = listOf("Clean Architecture 계층 이름 암기", "DDD 전술 패턴")
        ),
        g(
            "V1-C11-S02",
            terms = listOf(h("source of truth", "같은 상태가 여러 곳에 있을 때 최종 기준으로 삼는 원본"), h("eventual consistency", "여러 저장소가 즉시 같지 않아도 일정 시간이 지나면 같은 상태로 수렴하는 모델"), h("idempotency", "같은 논리 작업을 반복해도 중복 효과가 생기지 않게 하는 성질")),
            answers = listOf("state transition을 명확히 적으면 허용되지 않는 상태 이동을 찾기 쉽다.", "cache에는 source of truth와 invalidation 정책이 필요하다.", "queue는 retry·중복·실패 보관 정책을 함께 설계해야 한다.", "결제 retry에는 idempotency가 중요하다.", "동시 재고 차감 같은 race condition은 transaction·atomic operation·lock 등으로 제어한다."),
            deep = true,
            defer = listOf("분산 consistency 이론", "고급 message delivery semantics")
        ),
        g(
            "V1-C11-S03",
            terms = listOf(h("circuit breaker", "계속 실패하는 외부 dependency 호출을 잠시 차단해 장애 전파를 줄이는 패턴"), h("bulkhead", "한 기능 과부하가 전체 자원을 먹지 않게 자원을 칸막이처럼 나누는 방식"), h("SLI/SLO", "실제 운영 품질 지표 / 그 지표에 대해 정한 목표 수준")),
            answers = listOf("bottleneck을 측정하기 전에 무작정 server를 늘리지 않는다.", "scale up은 한 장비를 키우고 scale out은 여러 instance로 나눈다.", "timeout·retry·circuit breaker는 장애 대응에서 역할이 다르다.", "logs·metrics·traces는 운영 문제를 서로 다른 관점에서 보여 준다.", "종합 프로젝트에서는 요구사항→데이터→API→보안→테스트→실패 시뮬레이션→release 검증을 연결한다.", "AI가 구현을 도와도 권한·불변조건·실행 증거는 사람이 검증한다."),
            deep = true,
            defer = listOf("서비스 메시", "고급 load balancing", "SRE error budget 운영")
        )
    )

    private val replacedTrackPrefixes = setOf(
        "V1-C01-", "V1-C03-", "V1-C06-", "V1-C07-", "V1-C08-", "V1-C09-", "V1-C10-", "V1-C11-"
    )

    val guides: List<BeginnerLessonGuide> = V3BeginnerGuidance.guides
        .filterNot { guide -> replacedTrackPrefixes.any { guide.sectionId.startsWith(it) } }
        .plus(overrides)

    private val byId = guides.associateBy { it.sectionId }

    fun find(sectionId: String): BeginnerLessonGuide? = byId[sectionId]

    fun forSection(sectionId: String): BeginnerLessonGuide = requireNotNull(find(sectionId)) {
        "Missing beginner guidance for $sectionId"
    }

    fun decoratedTitle(sectionId: String, title: String): String =
        "${forSection(sectionId).focus.label} · $title"

    fun decorateBlocks(sectionId: String, authoredBlocks: List<TextbookBlock>): List<TextbookBlock> {
        val guide = forSection(sectionId)
        val before = buildList {
            add(TextbookBlock.Heading(4, "이번 LESSON 학습 가이드 · ${guide.focus.label}"))
            add(TextbookBlock.BulletList(guide.mustUnderstand, ordered = false))
            if (guide.termHints.isNotEmpty()) {
                add(TextbookBlock.Heading(4, "먼저 읽는 어려운 용어"))
                add(TextbookBlock.BulletList(guide.termHints.map { "${it.term} = ${it.plainMeaning}" }, ordered = false))
            }
            if (guide.canDefer.isNotEmpty()) {
                add(TextbookBlock.Heading(4, "지금은 외우지 않아도 됩니다"))
                add(TextbookBlock.Paragraph("아래 내용은 존재 이유만 이해하고 세부 구현·공식은 중급 단계에서 다시 봐도 됩니다."))
                add(TextbookBlock.BulletList(guide.canDefer, ordered = false))
            }
            add(TextbookBlock.Divider)
        }
        val after = buildList {
            add(TextbookBlock.Divider)
            add(TextbookBlock.Heading(4, "자가점검 정답·해설 · 먼저 스스로 답한 뒤 비교"))
            add(TextbookBlock.Paragraph("위의 ‘책을 덮고 확인’ 질문에 먼저 자기 말로 답한 뒤 비교하세요. 문장을 외우지 말고 빠진 이유·조건·실행 순서를 찾는 용도입니다."))
            add(TextbookBlock.BulletList(guide.answerPoints, ordered = true))
            add(TextbookBlock.Paragraph("해설을 확인했으면 바로 아래 짧은 확인에서 화면을 위로 보지 않고 다시 설명해 기억을 꺼내 봅니다."))
        }
        return before + authoredBlocks + after
    }
}
