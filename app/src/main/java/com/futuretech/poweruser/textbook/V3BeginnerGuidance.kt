package com.futuretech.poweruser.textbook

enum class BeginnerLessonFocus(val label: String) {
    CORE("필수"),
    CORE_WITH_DEEP_DIVE("필수 + 심화 포함")
}

data class BeginnerTermHint(val term: String, val plainMeaning: String)

data class BeginnerLessonGuide(
    val sectionId: String,
    val focus: BeginnerLessonFocus,
    val mustUnderstand: List<String>,
    val termHints: List<BeginnerTermHint>,
    val canDefer: List<String>,
    val answerPoints: List<String>
)

/** Non-destructive beginner layer added after V3 sectioning. Authored markdown is never replaced. */
object V3BeginnerGuidance {
    private val sectionIds = listOf(
        "V1-C01-S01", "V1-C01-S02",
        "V1-C02-S01", "V1-C02-S02", "V1-C02-S03", "V1-C02-S04",
        "V1-C03-S01", "V1-C03-S02",
        "V1-C04-S01", "V1-C04-S02", "V1-C04-S03",
        "V1-C05-S01", "V1-C05-S02", "V1-C05-S03", "V1-C05-S04",
        "V1-C06-S01", "V1-C06-S02",
        "V1-C07-S01", "V1-C07-S02",
        "V1-C08-S01", "V1-C08-S02",
        "V1-C09-S01",
        "V1-C10-S01", "V1-C10-S02",
        "V1-C11-S01", "V1-C11-S02"
    )

    private val deepDiveSections = setOf(
        "V1-C03-S01", "V1-C03-S02", "V1-C05-S01", "V1-C05-S04",
        "V1-C06-S01", "V1-C06-S02", "V1-C07-S02", "V1-C08-S02",
        "V1-C09-S01", "V1-C11-S01", "V1-C11-S02"
    )

    private val termHints = mapOf(
        "V1-C01-S01" to listOf(
            h("encoding", "문자나 정보를 저장·전송 가능한 표현으로 바꾸는 규칙"),
            h("Unicode", "문자마다 공통 번호를 정해 둔 큰 문자 표준"),
            h("UTF-8", "Unicode 문자를 실제 byte로 표현하는 대표적인 방식")
        ),
        "V1-C01-S02" to listOf(
            h("kernel", "운영체제 안에서 CPU·메모리·장치 같은 핵심 자원을 관리하는 중심 부분"),
            h("process", "저장된 프로그램이 실제 실행되고 있는 한 실행 단위")
        ),
        "V1-C02-S01" to listOf(
            h("decomposition", "큰 문제를 컴퓨터가 처리할 수 있는 작은 작업으로 나누는 것"),
            h("pseudocode", "정확한 언어 문법 전, 사람말에 가깝게 실행 순서를 적는 것")
        ),
        "V1-C02-S03" to listOf(
            h("scope", "변수 이름을 볼 수 있고 사용할 수 있는 범위"),
            h("parameter / argument", "함수 정의의 입력 자리 / 실제 호출할 때 넘기는 값")
        ),
        "V1-C03-S01" to listOf(
            h("amortized cost", "가끔 비싼 작업이 있어도 여러 작업 전체 비용을 평균해서 보는 방식"),
            h("BST", "작은 값은 왼쪽, 큰 값은 오른쪽에 두는 검색용 이진 트리"),
            h("heap", "최소값·최댓값 같은 우선 항목을 빠르게 꺼내기 위한 트리 기반 구조"),
            h("trie", "문자열의 공통 앞부분을 경로처럼 공유하는 트리 구조")
        ),
        "V1-C03-S02" to listOf(
            h("invariant", "알고리즘 진행 중 계속 참이어야 하는 성질"),
            h("memoization", "계산 결과를 저장해 같은 계산을 반복하지 않는 방법"),
            h("DP", "겹치는 작은 문제의 결과를 저장·조합해 큰 문제를 푸는 접근")
        ),
        "V1-C04-S01" to listOf(
            h("semantic HTML", "모양보다 내용의 역할이 코드에 드러나도록 HTML 요소를 고르는 것"),
            h("accessibility", "다양한 능력·환경의 사용자가 제품을 사용할 수 있게 만드는 품질")
        ),
        "V1-C04-S03" to listOf(
            h("DOM", "브라우저가 HTML을 프로그램에서 다룰 수 있게 만든 객체 tree"),
            h("event bubbling", "자식에서 발생한 event가 부모 방향으로 전달될 수 있는 동작")
        ),
        "V1-C05-S01" to listOf(
            h("lexical scope", "함수가 어디에서 정의됐는지를 기준으로 바깥 변수를 찾는 규칙"),
            h("closure", "함수가 만들어질 때 접근하던 바깥 변수 환경을 나중에도 사용하는 동작"),
            h("prototype", "객체에 없는 기능을 연결된 원형 객체 쪽에서 찾아가는 구조")
        ),
        "V1-C05-S02" to listOf(
            h("blocking", "현재 실행 흐름을 오래 붙잡아 다음 작업 진행을 막는 상태"),
            h("callback", "사건이나 작업 완료 뒤 나중에 실행하도록 전달해 둔 함수")
        ),
        "V1-C05-S03" to listOf(
            h("microtask", "현재 JS 실행 뒤, 다음 일반 task보다 먼저 처리되는 Promise 후속 작업 계열"),
            h("event loop", "현재 실행과 대기 작업을 보고 다음 JS 실행 기회를 조정하는 구조")
        ),
        "V1-C05-S04" to listOf(
            h("value is User", "검사가 true면 value를 User로 좁혀도 된다고 TypeScript에 알려 주는 타입 가드 표기"),
            h("Record<string, unknown>", "문자열 key를 가지지만 값 타입은 아직 모른다고 표현하는 객체 타입"),
            h("race condition", "여러 작업의 완료 순서에 따라 최종 결과가 달라지는 문제")
        ),
        "V1-C06-S01" to listOf(
            h("NAT", "내부 private IP 통신을 외부 public IP 통신과 연결하는 주소 변환 방식"),
            h("latency / bandwidth", "응답 지연 시간 / 일정 시간에 보낼 수 있는 데이터 양")
        ),
        "V1-C06-S02" to listOf(
            h("MIME type", "데이터가 HTML·JSON·PNG처럼 어떤 종류인지 나타내는 표준 이름"),
            h("boundary", "multipart body 안에서 여러 part를 구분하는 경계 문자열"),
            h("preflight", "일부 cross-origin 요청 전에 OPTIONS로 허용 여부를 먼저 묻는 절차")
        ),
        "V1-C07-S01" to listOf(
            h("DTO", "계층이나 시스템 경계에서 데이터를 전달하려고 정한 입력·출력 모양"),
            h("dependency injection", "필요한 실제 구현을 내부에서 만들지 않고 밖에서 전달받는 방식")
        ),
        "V1-C07-S02" to listOf(
            h("IDOR/BOLA", "ID만 바꿔 다른 사람 object에 접근할 수 있게 되는 권한 누락 취약점"),
            h("single flight", "같은 비싼 작업이 동시에 겹칠 때 대표 한 작업만 수행해 결과를 함께 쓰는 방식"),
            h("TTL jitter", "cache 만료 시점을 조금씩 흩뜨려 동시 만료 폭주를 줄이는 방식"),
            h("DLQ", "여러 번 실패한 message를 정상 queue에서 분리해 조사하는 dead-letter queue"),
            h("outbox", "DB 변경과 후속 event 기록을 같은 transaction에 남겨 event 유실을 줄이는 패턴"),
            h("saga/compensation", "분산 작업 일부 실패 시 보상 동작으로 이전 side effect를 맞추는 방식")
        ),
        "V1-C08-S01" to listOf(
            h("data integrity", "데이터가 정한 규칙과 관계를 깨지 않고 올바른 상태를 유지하는 성질"),
            h("referential integrity", "foreign key 관계가 실제 존재하는 row를 가리키도록 유지되는 성질")
        ),
        "V1-C08-S02" to listOf(
            h("selectivity", "조건이 전체 row 중 얼마나 적은 후보로 잘 좁혀 주는지 보는 정도"),
            h("optimizer", "DB가 여러 실행 방법의 비용을 추정해 query plan을 고르는 구성요소"),
            h("WAL", "commit된 변경을 장애 뒤 복구할 수 있도록 먼저 기록하는 write-ahead log 계열 메커니즘")
        ),
        "V1-C09-S01" to listOf(
            h("trust boundary", "이 선을 넘어온 값은 그대로 믿지 않고 다시 검증한다고 정한 경계"),
            h("digital signature", "내용을 숨기기보다 제작자와 변조 여부를 검증하는 서명"),
            h("SSRF", "사용자 주소를 서버가 대신 요청하게 해 내부 주소까지 접근시키는 공격")
        ),
        "V1-C10-S01" to listOf(
            h("minimal reproduction", "큰 앱 문제를 가장 작은 코드·입력으로 줄여 같은 실패를 재현하는 것"),
            h("flaky test", "같은 코드인데 timing·공유상태 때문에 PASS/FAIL이 불안정한 테스트")
        ),
        "V1-C10-S02" to listOf(
            h("artifact", "build가 만든 APK·AAB·bundle처럼 테스트·배포할 수 있는 결과물"),
            h("migration / rollback", "DB 구조 변경 절차 / 문제 배포를 이전 안정 상태로 되돌리는 절차")
        ),
        "V1-C11-S01" to listOf(
            h("cohesion", "한 모듈의 코드가 같은 목적과 책임에 얼마나 잘 모여 있는지 보는 관점"),
            h("coupling", "한 모듈 변경이 다른 모듈 변경까지 얼마나 강하게 요구하는지 보는 관점"),
            h("dependency inversion", "핵심 규칙이 구체 DB·SDK보다 추상 계약에 의존하도록 만드는 방향")
        ),
        "V1-C11-S02" to listOf(
            h("circuit breaker", "계속 실패하는 외부 dependency 호출을 잠시 차단해 장애 전파를 줄이는 패턴"),
            h("bulkhead", "한 기능 과부하가 전체 자원을 먹지 않게 worker·connection을 칸막이처럼 나누는 방식"),
            h("SLI/SLO", "실제 운영 품질 지표 / 그 지표에 대해 정한 목표 수준")
        )
    )

    private val defer = mapOf(
        "V1-C03-S01" to listOf("AVL/Red-Black tree 회전 구현", "hash 충돌 처리 알고리즘 세부", "trie 메모리 최적화"),
        "V1-C03-S02" to listOf("정렬 알고리즘 증명", "DP 점화식 심화", "고급 최단경로 알고리즘"),
        "V1-C05-S01" to listOf("prototype chain 세부", "this의 모든 호출 형태", "class 상속 설계 심화"),
        "V1-C05-S04" to listOf("never exhaustiveness 패턴", "generic 고급 제약", "AbortSignal 조합 패턴"),
        "V1-C06-S01" to listOf("DNS root/TLD 세부", "TCP 혼잡제어 알고리즘", "QUIC 내부 구현"),
        "V1-C06-S02" to listOf("HTTP cache directive 전체", "JWT 표준 세부", "WebSocket framing"),
        "V1-C07-S02" to listOf("cache stampede 구현", "분산 saga orchestrator", "관측성 표준 protocol"),
        "V1-C08-S02" to listOf("B-tree 페이지 내부 구조", "optimizer cost model", "isolation anomaly 세부 조합"),
        "V1-C09-S01" to listOf("암호 알고리즘 수학", "PKI chain 세부", "공격 bypass 기법"),
        "V1-C11-S01" to listOf("Clean Architecture 계층 이름 암기", "DDD 전술 패턴", "DI container 프레임워크"),
        "V1-C11-S02" to listOf("분산 consistency 이론 심화", "서비스 메시/고급 load balancing", "SRE error budget 운영")
    )

    private val answers = mapOf(
        "V1-C01-S01" to listOf("RAM은 실행 중 값을 빠르게 두고 저장장치는 파일을 오래 보관한다.", "경로는 파일의 실제 위치를 나타낸다.", "확장자는 이름의 힌트이고 파일 형식은 실제 byte 해석 규칙이다.", "Unicode는 문자 체계이고 UTF-8은 byte 표현 방식이다."),
        "V1-C01-S02" to listOf("소스 저장과 실행은 다르다.", "운영체제는 CPU·메모리·파일·장치 사용을 관리한다.", "실행 성공은 결과 정확성을 뜻하지 않는다.", "문법 오류와 논리 오류는 원인이 다르다."),
        "V1-C02-S01" to listOf("목표·입력·처리·출력을 먼저 정하면 필요한 코드가 보인다.", "타입은 값의 종류와 가능한 동작을 구분한다.", "변수는 값에 의미 있는 이름을 붙여 기억한다.", "의사코드는 언어 문법 전에 실행 생각을 적는 도구다."),
        "V1-C02-S02" to listOf("input 값은 문자열일 수 있어 숫자 계산 전 변환이 필요하다.", "if는 True/False에 따라 실행 경로를 나눈다.", "elif 순서는 결과를 바꿀 수 있다.", "while 상태가 종료 조건 쪽으로 변하지 않으면 무한 반복이 생길 수 있다."),
        "V1-C02-S03" to listOf("list·tuple·dict·set은 목적에 따라 고른다.", "parameter는 정의 자리, argument는 실제 전달 값이다.", "print와 return은 화면 표시와 결과 반환으로 역할이 다르다.", "scope 때문에 모든 변수가 어디서나 보이지 않는다."),
        "V1-C02-S04" to listOf("module은 관련 책임을 나눠 관리·재사용한다.", "파일은 process 종료 뒤에도 남을 수 있지만 일반 변수 상태는 사라질 수 있다.", "try/except는 예상 가능한 실패를 처리한다.", "모든 오류를 except: pass로 숨기면 실제 버그가 묻힌다."),
        "V1-C03-S01" to listOf("배열은 index 접근에 강하고 linked list는 연결 변경 삽입에 유리한 상황이 있다.", "node는 값과 연결 정보를 가진다.", "B→X→C 삽입은 X가 먼저 C를 가리켜 기존 연결을 보존한다.", "stack은 LIFO, queue는 FIFO다.", "binary tree와 BST는 같은 말이 아니다."),
        "V1-C03-S02" to listOf("binary search에는 정렬이라는 전제조건이 필요하다.", "Big-O는 입력 증가에 따른 작업량 증가 형태를 본다.", "cycle graph에서 visited가 없으면 같은 node를 반복 방문할 수 있다.", "BFS는 queue, DFS는 stack/recursion과 자연스럽게 연결된다.", "greedy는 전체 최적을 보장하는 근거가 필요하다."),
        "V1-C04-S01" to listOf("HTML은 구조·의미, CSS는 모양, JavaScript는 동작을 주로 담당한다.", "link는 이동, button은 기능 실행이 기본 역할이다.", "label은 input 의미와 focus를 연결한다.", "semantic HTML은 내용 역할을 구조에 드러낸다."),
        "V1-C04-S02" to listOf("padding은 내부, margin은 바깥 간격이다.", "box-sizing: border-box는 width 계산을 단순하게 한다.", "flex는 한 방향, grid는 행·열 배치에 자연스럽다.", "responsive는 단순 축소가 아니라 구조 재배치다."),
        "V1-C04-S03" to listOf("DOM은 HTML을 프로그램에서 다루는 tree다.", "event listener는 사건이 생겼을 때 실행할 함수를 등록한다.", "state와 화면이 어긋나면 UI 버그가 된다.", "Elements·Console·Network로 실제 상태를 확인한다."),
        "V1-C05-S01" to listOf("브라우저/Node.js는 JavaScript 실행 환경이다.", "const는 객체 내부 불변까지 자동 보장하지 않는다.", "객체 reference를 공유하면 한쪽 변경이 다른 쪽에서도 보일 수 있다.", "closure는 함수가 바깥 변수 환경을 나중에도 사용할 수 있게 한다.", "this는 호출 방식의 영향을 받는다."),
        "V1-C05-S02" to listOf("map은 변환, filter는 선별, reduce는 누적이다.", "spread는 deep clone이 아니다.", "throw는 오류를 위쪽 처리 지점으로 전달한다.", "callback은 나중 사건/완료 시점에 실행하도록 넘겨 둔 함수다.", "비동기와 병렬은 같은 말이 아니다."),
        "V1-C05-S03" to listOf("Promise는 pending→fulfilled/rejected 중 하나로 완료된다.", "async 함수는 Promise를 반환한다.", "await는 해당 async 함수의 이어지는 부분을 기다리게 한다.", "현재 script 뒤 microtask가 일반 task보다 먼저 처리될 수 있다.", "setTimeout 0은 즉시 실행 보장이 아니다."),
        "V1-C05-S04" to listOf("독립 작업은 Promise.all로 같이 시작·대기할 수 있다.", "race condition은 완료 순서 때문에 최신 결과가 오래된 결과로 덮이는 식의 문제다.", "debounce와 throttle은 실행 시점 정책이 다르다.", "TypeScript as User는 runtime JSON 검증이 아니다."),
        "V1-C06-S01" to listOf("Wi-Fi 연결은 외부 인터넷 전체 정상과 같은 뜻이 아니다.", "DNS는 domain에서 IP를 찾고 port는 서비스 끝점을 구분한다.", "NAT는 내부 private IP와 외부 통신을 연결한다.", "latency와 bandwidth는 서로 다른 성능 문제다."),
        "V1-C06-S02" to listOf("Content-Type은 실제 body 종류, Accept는 원하는 response 종류다.", "multipart/form-data는 여러 part를 한 body에 담고 boundary로 나눈다.", "FormData Content-Type을 수동 지정하면 boundary가 빠질 수 있다.", "HTTP·API·REST·JSON은 서로 다른 개념이다.", "CORS는 인증/권한 방화벽이 아니다."),
        "V1-C07-S01" to listOf("parsing은 형식을 읽는 일, validation은 시스템 규칙에 맞는지 확인하는 일이다.", "middleware 순서는 실제 동작에 영향을 준다.", "handler는 HTTP, service는 업무 규칙, repository는 저장소 접근에 집중할 수 있다.", "DI는 실제 DB/결제 대신 fake를 넣어 테스트하기 쉽게 한다."),
        "V1-C07-S02" to listOf("인증은 누구인지, 권한은 무엇을 할 수 있는지 확인한다.", "UI 버튼 숨김은 server authorization을 대체하지 못한다.", "queue는 중복·retry·DLQ 같은 새 실패 문제도 만든다.", "retry에는 idempotency가 중요하다.", "logs·metrics·traces는 보는 질문이 다르다."),
        "V1-C08-S01" to listOf("DB는 구조·제약·검색·동시성 기능과 함께 데이터를 관리한다.", "WHERE 없는 UPDATE/DELETE는 전체 row를 바꿀 수 있다.", "PRIMARY KEY는 대표 고유 식별자이고 UNIQUE는 다른 중복도 막는다.", "CHECK는 타입만으로 막지 못하는 값 범위를 지킨다.", "FOREIGN KEY는 참조 무결성을 지키는 데 도움을 준다."),
        "V1-C08-S02" to listOf("N:M은 junction table로 풀 수 있다.", "INNER JOIN은 matching row만, LEFT JOIN은 왼쪽 row를 모두 남긴다.", "index는 read를 빠르게 할 수 있지만 write/storage 비용을 만든다.", "EXPLAIN은 query plan을 보여 준다.", "transaction은 여러 변경을 하나의 commit/rollback 단위로 묶는다.", "긴 lock은 대기와 deadlock 위험을 키운다."),
        "V1-C09-S01" to listOf("기밀성·무결성·가용성은 각각 읽기·변경·사용 가능 상태를 보호한다.", "encoding·hash·encryption은 목적이 다르다.", "비밀번호는 전용 password hashing과 salt를 사용한다.", "digital signature는 내용을 숨기는 기능이 아니다.", "SQLi/XSS는 외부 데이터가 명령 문법으로 해석될 때 위험해진다.", "server에서 validation·authentication·authorization을 다시 적용한다."),
        "V1-C10-S01" to listOf("증상과 원인을 구분한다.", "FACT와 HYPOTHESIS를 분리한다.", "한 가설에 한 실험을 하면 원인 추적이 쉽다.", "unit·integration·E2E는 검증 범위가 다르다.", "PASS는 실행한 조건의 증거이지 버그 0개의 증명이 아니다.", "고친 버그는 regression test로 남긴다."),
        "V1-C10-S02" to listOf("commit은 변경 기록, branch는 별도 변경 흐름이다.", "merge conflict는 사람이 최종 의도를 정해야 하는 상황이다.", "main merge와 production 배포는 다른 단계다.", "artifact는 commit·설정·테스트와 연결해야 한다.", "CI PASS는 정의된 검사 성공 범위만 뜻한다.", "미실행 검증을 PASS라고 보고하지 않는다."),
        "V1-C11-S01" to listOf("functional requirement는 기능, non-functional은 품질 조건이다.", "acceptance criteria는 완료를 테스트 가능한 조건으로 만든다.", "responsibility와 module은 변경 이유를 기준으로 경계를 만든다.", "interface와 implementation은 계약과 구현을 나눈다.", "높은 cohesion과 낮은 불필요 coupling은 수정 범위를 줄인다.", "DI는 핵심 규칙을 구체 기술에서 떼어 테스트하기 쉽게 한다."),
        "V1-C11-S02" to listOf("state transition을 명시하면 잘못된 상태 이동을 막기 쉽다.", "cache는 source of truth와 invalidation 정책이 필요하다.", "retry는 idempotency와 함께 설계한다.", "scale out 뒤 DB가 새 bottleneck이 될 수 있다.", "timeout과 circuit breaker는 장애 전파를 줄이는 역할이 다르다.", "AI가 구현을 도와도 요구사항·권한·불변조건·실행 증거는 사람이 검증한다.")
    )

    val guides: List<BeginnerLessonGuide> = sectionIds.map { id ->
        BeginnerLessonGuide(
            sectionId = id,
            focus = if (id in deepDiveSections) BeginnerLessonFocus.CORE_WITH_DEEP_DIVE else BeginnerLessonFocus.CORE,
            mustUnderstand = listOf(
                "이번 LESSON의 핵심 흐름을 자기 말로 설명한다.",
                "본문 예시에서 ‘왜 필요한가 → 어떻게 동작하는가 → 어디서 실패하는가’를 따라간다.",
                "용어를 외우기보다 실제 코드·앱 상황과 연결해 설명한다."
            ),
            termHints = termHints[id].orEmpty(),
            canDefer = defer[id].orEmpty(),
            answerPoints = requireNotNull(answers[id]) { "Missing answer guide for $id" }
        )
    }

    private val byId = guides.associateBy { it.sectionId }

    fun forSection(sectionId: String): BeginnerLessonGuide = requireNotNull(byId[sectionId]) {
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

    private fun h(term: String, meaning: String) = BeginnerTermHint(term, meaning)
}
