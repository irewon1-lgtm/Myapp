package com.futuretech.poweruser.textbook

data class CurriculumChapterRef(
    val id: String,
    val number: Int,
    val title: String,
    val contentAvailable: Boolean
)

data class CurriculumBook(
    val id: String,
    val number: Int,
    val title: String,
    val chapters: List<CurriculumChapterRef>
)

object PowerUserCurriculumCatalog {
    const val TOTAL_BOOKS = 9
    const val TOTAL_CHAPTERS = 134

    val books: List<CurriculumBook> = listOf(
        CurriculumBook("V1", 1, "컴퓨터·인터넷·디지털 시스템", listOf(
            CurriculumChapterRef("V1-C01", 1, "시스템 전체 그림과 디지털 표현", true),
            CurriculumChapterRef("V1-C02", 2, "파일·경로·형식·무결성", true),
            CurriculumChapterRef("V1-C03", 3, "프로세스와 동시성", true),
            CurriculumChapterRef("V1-C04", 4, "메모리·연산장치·커널 경계", true),
            CurriculumChapterRef("V1-C05", 5, "권한·터미널·환경변수", true),
            CurriculumChapterRef("V1-C06", 6, "설치·버전·설정과 의존성", true),
            CurriculumChapterRef("V1-C07", 7, "로그를 읽고 시간축으로 사건 재구성하기", true),
            CurriculumChapterRef("V1-C08", 8, "포트·DNS·앱/웹/서버 경계", true),
            CurriculumChapterRef("V1-C09", 9, "Android 권한·동기화·백업", true),
            CurriculumChapterRef("V1-C10", 10, "성능 병목과 장애를 계층별로 진단하기", true),
            CurriculumChapterRef("V1-C11", 11, "종합 시스템 장애 진단 프로젝트", true)
        )),
        CurriculumBook("V2", 2, "프로그래밍 사고 + Python", listOf(
            CurriculumChapterRef("V2-C01", 1, "문제를 코드로 바꾸기 전의 사고법", false),
            CurriculumChapterRef("V2-C02", 2, "Python 실행·오류·값의 기초", false),
            CurriculumChapterRef("V2-C03", 3, "객체·참조·표현식 mental model", false),
            CurriculumChapterRef("V2-C04", 4, "문자열·입출력과 사용자 데이터", false),
            CurriculumChapterRef("V2-C05", 5, "조건·경계값·의사결정", false),
            CurriculumChapterRef("V2-C06", 6, "반복과 데이터 처리 패턴", false),
            CurriculumChapterRef("V2-C07", 7, "리스트·튜플·슬라이스와 가독성", false),
            CurriculumChapterRef("V2-C08", 8, "딕셔너리·집합·중첩 데이터", false),
            CurriculumChapterRef("V2-C09", 9, "함수의 설계: 입력·출력·책임", false),
            CurriculumChapterRef("V2-C10", 10, "scope·mutability·함수를 값으로 다루기", false),
            CurriculumChapterRef("V2-C11", 11, "예외·traceback·좋은 오류 메시지", false),
            CurriculumChapterRef("V2-C12", 12, "파일시스템을 안전하게 다루기", false),
            CurriculumChapterRef("V2-C13", 13, "CSV·JSON·날짜시간 실전 데이터 처리", false),
            CurriculumChapterRef("V2-C14", 14, "module·venv·의존성·프로젝트 구조", false),
            CurriculumChapterRef("V2-C15", 15, "typing·dataclass·class로 데이터 모델링", false),
            CurriculumChapterRef("V2-C16", 16, "iterator·context manager·async의 실행 흐름", false),
            CurriculumChapterRef("V2-C17", 17, "logging과 작은 테스트로 관찰 가능하게 만들기", false),
            CurriculumChapterRef("V2-C18", 18, "HTTP API 호출과 견고한 자동화 스크립트", false),
            CurriculumChapterRef("V2-C19", 19, "AI가 만든 Python 검증과 종합 프로젝트", false)
        )),
        CurriculumBook("V3", 3, "Web + JavaScript + TypeScript", listOf(
            CurriculumChapterRef("V3-C01", 1, "웹 문서의 구조와 의미", false),
            CurriculumChapterRef("V3-C02", 2, "콘텐츠·링크·자산·미디어", false),
            CurriculumChapterRef("V3-C03", 3, "폼·검증·접근성·키보드 사용성", false),
            CurriculumChapterRef("V3-C04", 4, "CSS cascade와 box model", false),
            CurriculumChapterRef("V3-C05", 5, "크기·타이포·배치·stacking", false),
            CurriculumChapterRef("V3-C06", 6, "Flexbox·Grid·반응형 레이아웃", false),
            CurriculumChapterRef("V3-C07", 7, "유지보수 가능한 CSS component 규칙", false),
            CurriculumChapterRef("V3-C08", 8, "JavaScript 실행모델·값·제어흐름", false),
            CurriculumChapterRef("V3-C09", 9, "함수·scope·array·object로 로직 구성하기", false),
            CurriculumChapterRef("V3-C10", 10, "module·DOM·event로 화면 동작시키기", false),
            CurriculumChapterRef("V3-C11", 11, "상태·비동기·fetch·브라우저 오류처리", false),
            CurriculumChapterRef("V3-C12", 12, "Storage·DevTools로 웹앱 관찰하기", false),
            CurriculumChapterRef("V3-C13", 13, "TypeScript로 데이터 계약을 강제하기", false),
            CurriculumChapterRef("V3-C14", 14, "접근 가능한 반응형 웹앱 종합 프로젝트", false)
        )),
        CurriculumBook("V4", 4, "데이터 + SQL + Database", listOf(
            CurriculumChapterRef("V4-C01", 1, "데이터를 파일이 아니라 계약으로 보기", false),
            CurriculumChapterRef("V4-C02", 2, "타입·NULL·품질규칙", false),
            CurriculumChapterRef("V4-C03", 3, "관계형 모델·entity·key", false),
            CurriculumChapterRef("V4-C04", 4, "normalization과 안전한 SQL 연습환경", false),
            CurriculumChapterRef("V4-C05", 5, "SELECT·WHERE·정렬·pagination", false),
            CurriculumChapterRef("V4-C06", 6, "집계·GROUP BY로 요약하기", false),
            CurriculumChapterRef("V4-C07", 7, "JOIN과 관계 오류를 다루기", false),
            CurriculumChapterRef("V4-C08", 8, "CTE·DML·constraint로 데이터 변경 통제하기", false),
            CurriculumChapterRef("V4-C09", 9, "transaction·ACID·동시성", false),
            CurriculumChapterRef("V4-C10", 10, "index·EXPLAIN·view로 성능을 판단하기", false),
            CurriculumChapterRef("V4-C11", 11, "migration과 SQL injection 방지", false),
            CurriculumChapterRef("V4-C12", 12, "backup·restore·DB engine 선택·데이터 이동", false),
            CurriculumChapterRef("V4-C13", 13, "데이터 validation과 DB observability", false),
            CurriculumChapterRef("V4-C14", 14, "AI가 만든 schema/SQL 검증과 종합 프로젝트", false)
        )),
        CurriculumBook("V5", 5, "HTTP + API + 네트워크", listOf(
            CurriculumChapterRef("V5-C01", 1, "네트워크 경로·IP·DNS", false),
            CurriculumChapterRef("V5-C02", 2, "TCP/UDP와 TLS로 연결의 성질 이해하기", false),
            CurriculumChapterRef("V5-C03", 3, "HTTP 요청·응답·method", false),
            CurriculumChapterRef("V5-C04", 4, "URL·header·cookie·JSON payload", false),
            CurriculumChapterRef("V5-C05", 5, "curl 재현·REST·idempotency", false),
            CurriculumChapterRef("V5-C06", 6, "pagination·filter·cache·ETag", false),
            CurriculumChapterRef("V5-C07", 7, "timeout·retry·backoff·rate limit", false),
            CurriculumChapterRef("V5-C08", 8, "API key·OAuth/OIDC·session/token", false),
            CurriculumChapterRef("V5-C09", 9, "CORS·same-origin·CSRF", false),
            CurriculumChapterRef("V5-C10", 10, "OpenAPI·오류모델·버전 호환성", false),
            CurriculumChapterRef("V5-C11", 11, "Webhook을 안전하게 운영하기", false),
            CurriculumChapterRef("V5-C12", 12, "WebSocket·SSE·RPC 선택", false),
            CurriculumChapterRef("V5-C13", 13, "네트워크 디버깅·trace·견고한 client", false),
            CurriculumChapterRef("V5-C14", 14, "실전 API client/server 종합 프로젝트", false)
        )),
        CurriculumBook("V6", 6, "Git + 디버깅 + 테스트 + 소프트웨어 엔지니어링", listOf(
            CurriculumChapterRef("V6-C01", 1, "Git mental model: repository·working tree·index", false),
            CurriculumChapterRef("V6-C02", 2, "diff·commit·history를 증거로 읽기", false),
            CurriculumChapterRef("V6-C03", 3, "branch·merge·conflict·rebase", false),
            CurriculumChapterRef("V6-C04", 4, "restore·revert·reset과 disaster recovery", false),
            CurriculumChapterRef("V6-C05", 5, "remote·PR·code review", false),
            CurriculumChapterRef("V6-C06", 6, "디버깅 1: 재현과 관찰", false),
            CurriculumChapterRef("V6-C07", 7, "디버깅 2: 격리·가설·최소수정", false),
            CurriculumChapterRef("V6-C08", 8, "디버깅 3: trace·상태조합·회귀범위", false),
            CurriculumChapterRef("V6-C09", 9, "unit·integration·E2E test", false),
            CurriculumChapterRef("V6-C10", 10, "test double·결정성·regression test", false),
            CurriculumChapterRef("V6-C11", 11, "property·coverage·위험기반 테스트", false),
            CurriculumChapterRef("V6-C12", 12, "명세·acceptance criteria·invariant", false),
            CurriculumChapterRef("V6-C13", 13, "refactoring·legacy code·module boundary", false),
            CurriculumChapterRef("V6-C14", 14, "CI·artifact·signing·provenance", false),
            CurriculumChapterRef("V6-C15", 15, "점진배포·observability·incident·rollback", false),
            CurriculumChapterRef("V6-C16", 16, "AI 변경 통제 종합 프로젝트", false)
        )),
        CurriculumBook("V7", 7, "실전 사이버보안", listOf(
            CurriculumChapterRef("V7-C01", 1, "asset·threat·risk·trust boundary", false),
            CurriculumChapterRef("V7-C02", 2, "인증·비밀번호·Passkey·MFA·session", false),
            CurriculumChapterRef("V7-C03", 3, "least privilege와 secret 관리", false),
            CurriculumChapterRef("V7-C04", 4, "암호화·hash·signature·TLS", false),
            CurriculumChapterRef("V7-C05", 5, "입력검증·canonicalization·Injection", false),
            CurriculumChapterRef("V7-C06", 6, "XSS·CSRF·Broken Access Control", false),
            CurriculumChapterRef("V7-C07", 7, "SSRF·파일 업로드·path traversal", false),
            CurriculumChapterRef("V7-C08", 8, "misconfiguration·supply chain·업데이트", false),
            CurriculumChapterRef("V7-C09", 9, "로그·예외·민감정보 저장", false),
            CurriculumChapterRef("V7-C10", 10, "Android 앱 보안과 privacy·data minimization", false),
            CurriculumChapterRef("V7-C11", 11, "피싱·사칭·ransomware·계정복구", false),
            CurriculumChapterRef("V7-C12", 12, "API security 종합", false),
            CurriculumChapterRef("V7-C13", 13, "prompt injection과 agent/tool 권한보안", false),
            CurriculumChapterRef("V7-C14", 14, "secure review·OWASP/ASVS·threat modeling", false),
            CurriculumChapterRef("V7-C15", 15, "incident response와 최종 보안감사", false)
        )),
        CurriculumBook("V8", 8, "AI·LLM·Prompt·Context·검증", listOf(
            CurriculumChapterRef("V8-C01", 1, "AI·ML·LLM과 token의 기초", false),
            CurriculumChapterRef("V8-C02", 2, "embedding과 Transformer mental model", false),
            CurriculumChapterRef("V8-C03", 3, "학습·추론·context window", false),
            CurriculumChapterRef("V8-C04", 4, "hallucination·불확실성·검증 태도", false),
            CurriculumChapterRef("V8-C05", 5, "prompt·instruction hierarchy·few-shot", false),
            CurriculumChapterRef("V8-C06", 6, "structured output과 prompt를 spec으로 관리하기", false),
            CurriculumChapterRef("V8-C07", 7, "context engineering과 압축·handoff", false),
            CurriculumChapterRef("V8-C08", 8, "grounding과 RAG 기본 구조", false),
            CurriculumChapterRef("V8-C09", 9, "chunking·retrieval·RAG 품질", false),
            CurriculumChapterRef("V8-C10", 10, "tool calling과 도구 설계", false),
            CurriculumChapterRef("V8-C11", 11, "multimodal·reasoning model·모델 선택", false),
            CurriculumChapterRef("V8-C12", 12, "비용·latency 최적화", false),
            CurriculumChapterRef("V8-C13", 13, "privacy·prompt injection·output validation", false),
            CurriculumChapterRef("V8-C14", 14, "eval dataset·rubric·human/LLM judge", false),
            CurriculumChapterRef("V8-C15", 15, "error taxonomy·regression eval·AI 종합 프로젝트", false)
        )),
        CurriculumBook("V9", 9, "AI 코딩·Agent·자동화·최종 프로젝트", listOf(
            CurriculumChapterRef("V9-C01", 1, "AI 코딩을 명세와 acceptance test로 시작하기", false),
            CurriculumChapterRef("V9-C02", 2, "repo 지도와 context selection", false),
            CurriculumChapterRef("V9-C03", 3, "변경계획·위험범위·baseline", false),
            CurriculumChapterRef("V9-C04", 4, "작은 변경·diff·AI 코드 이해", false),
            CurriculumChapterRef("V9-C05", 5, "의존성·테스트·AI code review", false),
            CurriculumChapterRef("V9-C06", 6, "AI 디버깅·가설경쟁·refactoring 통제", false),
            CurriculumChapterRef("V9-C07", 7, "migration·문서·handoff·재개 가능성", false),
            CurriculumChapterRef("V9-C08", 8, "tool calling·workflow·agent loop", false),
            CurriculumChapterRef("V9-C09", 9, "memory·retrieval·planning", false),
            CurriculumChapterRef("V9-C10", 10, "subagent·MCP·connector", false),
            CurriculumChapterRef("V9-C11", 11, "권한·human approval·sandbox", false),
            CurriculumChapterRef("V9-C12", 12, "idempotency·retry·scheduler·queue·webhook", false),
            CurriculumChapterRef("V9-C13", 13, "trace·agent eval·external evaluator", false),
            CurriculumChapterRef("V9-C14", 14, "failure recovery·CI/CD·artifact/signing", false),
            CurriculumChapterRef("V9-C15", 15, "monitoring·rollback·kill switch", false),
            CurriculumChapterRef("V9-C16", 16, "Capstone 3종: 자동화·AI 앱·AI 개발 검증", false)
        ))
    )

    val chapters: List<CurriculumChapterRef> = books.flatMap { it.chapters }

    fun bookById(id: String): CurriculumBook? = books.find { it.id == id }
    fun chapterById(id: String): CurriculumChapterRef? = chapters.find { it.id == id }
    fun bookForChapter(chapterId: String): CurriculumBook? = books.find { book -> book.chapters.any { it.id == chapterId } }
}
