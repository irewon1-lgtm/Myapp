package com.futuretech.poweruser.textbook

data class TextbookChapter(
    val id: String, val number: Int, val title: String, val assetPath: String, val practiceLessonId: String,
    val sourceLessonIds: List<String>, val summary: String, val keyConcepts: List<String>,
    val humanMustKnow: String, val aiCanHelp: String, val estimatedReadMinutes: Int
)

object V1TextbookCatalog {
    const val BOOK_TITLE = "코딩 완전과정"
    const val BOOK_SUBTITLE = "완전 초보 출발 · 관련 BLOCK을 큰 LESSON으로 묶어 책처럼 깊게 학습 · 직접 실습"
    const val TRACK_COUNT = 11
    const val TRACK_LABEL = "TRACK"
    const val BLOCK_LABEL = "BLOCK"
    const val LESSON_LABEL = "LESSON"

    val chapters: List<TextbookChapter> = listOf(
        c(
            "V1-C01", 1, "컴퓨터와 프로그래밍의 언어", "V2-T01", listOf("V1-01", "V1-02", "V1-03"),
            "컴퓨터가 무엇인지부터 시작한다. 입력·처리·출력, CPU·RAM·저장장치, 파일·인코딩, 프로그램·코드·실행을 모르는 용어 없이 쌓고 첫 코드를 직접 실행한다.",
            listOf("컴퓨터", "CPU·RAM·저장장치", "파일·인코딩", "프로그램·코드", "실행과 오류"),
            "컴퓨터와 프로그램이 실제로 무엇인지 자기 말로 설명하고, 처음 보는 코드를 작은 조각으로 나누어 읽을 수 있어야 한다.",
            "AI는 예제와 설명을 도울 수 있지만 이해하지 못한 용어와 실행하지 않은 코드를 그대로 믿지 않는다.",
            1200
        ),
        c(
            "V1-C02", 2, "프로그래밍 사고와 문법", "V2-T02", listOf("V1-04", "V1-05", "V1-06"),
            "값·자료형·변수에서 시작해 조건·반복·함수·스코프·모듈·오류 처리까지 직접 코드를 쓰며 익힌다. 문법보다 실행 순서와 문제를 작은 단계로 나누는 사고를 먼저 만든다.",
            listOf("값과 자료형", "변수와 상태", "조건과 반복", "함수와 스코프", "모듈과 오류 처리"),
            "짧은 프로그램을 스스로 설계·작성·실행·수정하고, 각 줄이 왜 필요한지 설명할 수 있어야 한다.",
            "AI가 코드를 써 줄 수 있어도 변수 상태와 실행 흐름을 사람이 추적해 틀린 결과를 찾아야 한다.",
            1500
        ),
        c(
            "V1-C03", 3, "자료구조와 알고리즘", "V2-T03", listOf("V1-07", "V1-08"),
            "배열·리스트·스택·큐·해시 테이블·트리·그래프를 아주 쉬운 상황에서 시작해 검색·정렬·재귀·시간복잡도·공간복잡도와 문제 해결 전략까지 연결한다.",
            listOf("배열·리스트", "스택·큐", "해시 테이블", "트리·그래프", "알고리즘·복잡도"),
            "자료구조를 이름으로 외우지 않고 어떤 문제에 왜 선택하는지, 더 느린 방법과 비교해 설명할 수 있어야 한다.",
            "AI가 알고리즘 답을 내도 시간복잡도, 경계값, 자료구조 선택을 사람이 검증한다.",
            1800
        ),
        c(
            "V1-C04", 4, "웹 화면과 브라우저", "V2-T04", listOf("V1-09", "V1-10", "V1-11", "V1-12"),
            "웹페이지가 무엇인지부터 HTML·CSS·DOM·이벤트·레이아웃·반응형·접근성·브라우저 렌더링까지 배우고 실제 앱 화면을 만든다.",
            listOf("HTML", "CSS", "DOM", "이벤트", "반응형·접근성"),
            "화면을 구조·스타일·동작으로 나누고, 원하는 UI를 직접 만들어 문제를 브라우저 개발도구로 찾을 수 있어야 한다.",
            "AI가 UI 코드를 만들어도 DOM 구조, 반응형 깨짐, 접근성, 이벤트 흐름을 사람이 확인한다.",
            1700
        ),
        c(
            "V1-C05", 5, "JavaScript와 TypeScript 깊게", "V2-T05", listOf("V1-13", "V1-14", "V1-15", "V1-16"),
            "JavaScript 실행 방식, 값과 참조, 함수·클로저, 객체·프로토타입, 모듈, Promise·async/await·이벤트 루프·race condition을 익힌 뒤 TypeScript 타입 시스템으로 확장한다.",
            listOf("실행 컨텍스트", "클로저·객체", "모듈", "비동기·이벤트 루프", "TypeScript"),
            "비동기 코드의 시작·완료·실패·상태 변경 순서를 추적하고 TypeScript가 잡는 오류와 못 잡는 오류를 구분할 수 있어야 한다.",
            "AI가 비동기 코드를 작성해도 경쟁 상태와 오류 경로, 타입 경계를 사람이 직접 검토한다.",
            1900
        ),
        c(
            "V1-C06", 6, "인터넷·네트워크·API", "V2-T06", listOf("V1-17", "V1-18", "V1-19"),
            "네트워크·IP·DNS·포트에서 시작해 TCP·TLS·HTTP·URL·요청·응답·MIME·Content-Type·JSON·API·인증·CORS와 파일 업로드까지 통신 순서대로 배운다.",
            listOf("IP·DNS·포트", "TCP·TLS", "HTTP", "MIME·JSON", "API·인증·CORS"),
            "네트워크 문제를 연결·암호화·HTTP·데이터 형식·브라우저 정책 단계로 나눠 실제 요청과 응답을 읽을 수 있어야 한다.",
            "AI가 API 코드를 만들어도 실제 status code, headers, body, 인증정보와 오류 경계를 사람이 확인한다.",
            1800
        ),
        c(
            "V1-C07", 7, "서버와 백엔드", "V2-T07", listOf("V1-20"),
            "서버가 요청을 받는 순간부터 route·middleware·validation·인증·권한·파일·캐시·queue·background job·rate limit·동시성·관측성까지 실제 백엔드를 만든다.",
            listOf("서버·route", "validation", "인증·권한", "cache·queue", "동시성·관측성"),
            "요청 하나가 서버 안을 통과하는 경로를 설명하고 실패·재시도·중복 처리·성능 문제를 설계 단계에서 고려할 수 있어야 한다.",
            "AI가 서버 코드를 작성해도 입력 검증, 권한, transaction 경계, 재시도 안전성을 사람이 검토한다.",
            1900
        ),
        c(
            "V1-C08", 8, "데이터베이스", "V2-T08", listOf("V1-21", "V1-22", "V1-23"),
            "데이터를 왜 DB에 저장하는지부터 table·row·column·key·SQL·JOIN·index·정규화·무결성·transaction·ACID·lock·deadlock·성능까지 배운다.",
            listOf("table·key", "SQL·JOIN", "index", "무결성·정규화", "transaction·ACID"),
            "데이터 모델을 직접 설계하고 무결성 제약, transaction 경계와 query 성능을 근거로 설명할 수 있어야 한다.",
            "AI가 SQL을 만들어도 잘못된 JOIN, N+1, index 부족, transaction·무결성 문제를 사람이 확인한다.",
            1900
        ),
        c(
            "V1-C09", 9, "보안과 데이터 보호", "V2-T09", listOf("V1-24", "V1-25"),
            "위협과 신뢰 경계부터 encoding·hash·encryption 차이, password hashing·salt, 대칭/비대칭키, signature·certificate·HTTPS, SQL injection·XSS·CSRF·secret·파일 업로드 보안까지 배운다.",
            listOf("위협 모델", "hash·encryption", "키·서명·인증서", "웹 공격", "secret·파일 보안"),
            "보안 기능을 이름으로 붙이는 것이 아니라 공격자가 어떤 경로로 무엇을 할 수 있는지 생각하고 방어를 검증할 수 있어야 한다.",
            "AI가 보안 코드를 제안해도 최신 안전한 기본값과 신뢰 경계, secret 노출 여부는 사람이 검증한다.",
            1900
        ),
        c(
            "V1-C10", 10, "오류·테스트·Git·빌드·배포", "V2-T10", listOf("V1-26", "V1-27"),
            "재현·디버거·단위/통합/E2E·회귀시험을 익히고 Git·branch·merge·CI·build·artifact·서명·버전·dependency·migration·rollback·배포까지 한 흐름으로 연결한다.",
            listOf("디버깅", "테스트", "Git", "빌드·CI", "배포·rollback"),
            "수정 전후를 실제 실행으로 검증하고, 코드 변경이 어떤 commit·artifact·배포본에 들어갔는지 추적할 수 있어야 한다.",
            "AI가 수정안을 만들 수 있지만 PASS·완료 판정은 실제 테스트와 배포 증거로만 한다.",
            2000
        ),
        c(
            "V1-C11", 11, "소프트웨어 설계와 종합 프로젝트", "V2-T11", listOf("V1-28"),
            "요구사항·추상화·interface·coupling·cohesion·layer·architecture·refactoring·state management·cache·queue·scaling·fault tolerance를 배우고 실제 서비스를 설계·구현·검증한다.",
            listOf("요구사항", "모듈 설계", "아키텍처", "성능·확장", "종합 프로젝트"),
            "AI에게 일을 맡기기 전에 구조와 불변조건을 설계하고 결과 코드의 데이터 흐름·오류 경계·테스트·운영 위험을 독립적으로 검토할 수 있어야 한다.",
            "AI는 구현 속도를 높이는 도구다. 최종 시스템 경계·안전성·데이터 정합성·실행 증거에 대한 책임은 사람이 가진다.",
            2400
        )
    )

    val practiceLessonIds: Set<String> = chapters.map { it.practiceLessonId }.toSet()
    val allSourceLessonIds: List<String> = chapters.flatMap { it.sourceLessonIds }
    fun chapterById(id: String): TextbookChapter? = chapters.find { it.id == id }
    fun chapterByPracticeLessonId(id: String): TextbookChapter? = chapters.find { it.practiceLessonId == id }

    private fun c(
        id: String,
        number: Int,
        title: String,
        practice: String,
        source: List<String>,
        summary: String,
        keyConcepts: List<String>,
        human: String,
        ai: String,
        minutes: Int
    ) = TextbookChapter(
        id,
        number,
        title,
        "textbook/v3/track_${number.toString().padStart(2, '0')}.md",
        practice,
        source,
        summary,
        keyConcepts,
        human,
        ai,
        minutes
    )
}
