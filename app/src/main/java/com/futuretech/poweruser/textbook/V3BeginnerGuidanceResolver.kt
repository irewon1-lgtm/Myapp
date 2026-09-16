package com.futuretech.poweruser.textbook

/**
 * Resolves learner guidance after V3 grouping. Most lessons reuse the original audited guidance.
 * TRACK 06 is intentionally split into three learner lessons after the first real Gradle run exposed
 * a >60 minute page, so its later two guides are defined here without rewriting the stable table.
 */
object V3BeginnerGuidanceResolver {
    private val genericMustUnderstand = listOf(
        "이번 LESSON의 핵심 흐름을 자기 말로 설명한다.",
        "본문 예시에서 ‘왜 필요한가 → 어떻게 동작하는가 → 어디서 실패하는가’를 따라간다.",
        "용어를 외우기보다 실제 코드·앱 상황과 연결해 설명한다."
    )

    private val c06s02 = BeginnerLessonGuide(
        sectionId = "V1-C06-S02",
        focus = BeginnerLessonFocus.CORE_WITH_DEEP_DIVE,
        mustUnderstand = genericMustUnderstand,
        termHints = listOf(
            BeginnerTermHint("TLS", "HTTP 내용을 보내기 전에 암호화된 통신 채널을 만들고 상대 확인을 돕는 보안 프로토콜"),
            BeginnerTermHint("certificate", "접속한 도메인과 서버의 public key 신원을 검증할 수 있게 연결하는 디지털 증명서"),
            BeginnerTermHint("idempotency", "같은 요청을 여러 번 반복해도 최종 상태 효과가 중복되지 않는 성질")
        ),
        canDefer = listOf(
            "TLS handshake의 암호학 세부",
            "HTTP version별 framing 차이",
            "safe/idempotent 표준의 세부 예외"
        ),
        answerPoints = listOf(
            "HTTPS는 HTTP 요청을 TLS로 보호해 전송한다.",
            "certificate 검증에서는 접속한 domain과 인증서가 맞는지 확인하는 것이 중요하다.",
            "URL은 scheme·host·port·path·query·fragment로 역할을 나눠 읽는다.",
            "HTTP request/response는 method·status·header·body를 구분해 읽는다.",
            "idempotent는 응답 문장이 같다는 뜻이 아니라 반복 요청의 최종 상태 효과를 본다.",
            "network 실패와 HTTP 404/500 응답은 서로 다른 단계의 실패다."
        )
    )

    private val c06s03 = BeginnerLessonGuide(
        sectionId = "V1-C06-S03",
        focus = BeginnerLessonFocus.CORE_WITH_DEEP_DIVE,
        mustUnderstand = genericMustUnderstand,
        termHints = listOf(
            BeginnerTermHint("MIME type", "데이터가 HTML·JSON·PNG처럼 어떤 종류인지 나타내는 표준 이름"),
            BeginnerTermHint("boundary", "multipart body 안에서 여러 part가 어디서 나뉘는지 표시하는 경계 문자열"),
            BeginnerTermHint("preflight", "일부 cross-origin 요청 전에 OPTIONS로 실제 요청을 허용할지 먼저 확인하는 절차")
        ),
        canDefer = listOf(
            "HTTP cache directive 전체",
            "JWT 표준 세부",
            "WebSocket framing"
        ),
        answerPoints = listOf(
            "Content-Type은 현재 body 종류이고 Accept는 client가 받고 싶은 response 종류를 나타낸다.",
            "multipart/form-data는 text와 file 같은 여러 part를 한 body에 담고 boundary로 나눈다.",
            "FormData를 보낼 때 Content-Type을 무심코 수동 지정하면 browser가 만든 boundary가 빠질 수 있다.",
            "HTTP·API·REST·JSON은 서로 다른 역할의 개념이다.",
            "authentication은 누구인지, authorization은 무엇을 할 수 있는지 확인한다.",
            "CORS는 browser의 cross-origin response 접근 정책이지 server 인증·권한 방화벽이 아니다."
        )
    )

    val guides: List<BeginnerLessonGuide> = V3BeginnerGuidance.guides
        .filterNot { it.sectionId == c06s02.sectionId }
        .plus(c06s02)
        .plus(c06s03)

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
