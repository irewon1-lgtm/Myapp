package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.LessonContent

enum class LearningProblemType(val displayName: String) {
    CONCEPT_CHOICE("개념 선택"),
    OUTPUT_PREDICTION("실행결과 예상"),
    ORDERING("순서배치"),
    FILL_CODE("빈칸코드"),
    ONE_LINE_FIX("한 줄 수정"),
    DIRECT_WRITE("직접 설명/작성"),
    DEBUGGING("디버깅"),
    AI_ANSWER_AUDIT("AI가 만든 답 검증")
}

data class InlineLearningProblem(
    val id: String,
    val type: LearningProblemType,
    val prompt: String,
    val code: String? = null,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val referenceAnswer: String = "",
    val correctOrder: List<String> = emptyList(),
    val shuffledOrder: List<String> = emptyList()
)

data class LearningConcept(
    val id: String,
    val index: Int,
    val title: String,
    val blocks: List<TextbookBlock>,
    val problem: InlineLearningProblem
)

/**
 * Textbook-first presentation layer.
 *
 * Each learner-facing LESSON keeps the complete authored explanation/examples/code together.
 * Beginner guidance is added around the authored body without replacing it. Synthetic/non-V3
 * callers stay legacy-safe so parser and practice tests can still construct arbitrary sections.
 */
object TextbookLearningFlow {
    const val MAX_BLOCKS_PER_CONCEPT = Int.MAX_VALUE

    fun buildConcepts(
        chapterId: String,
        section: TextbookSection,
        @Suppress("UNUSED_PARAMETER") practiceLesson: LessonContent
    ): List<LearningConcept> {
        if (section.blocks.isEmpty()) return emptyList()

        val isV3LearnerSection = chapterId.matches(Regex("V1-C\\d{2}")) &&
            section.id.startsWith("$chapterId-S")
        val guide = if (isV3LearnerSection) {
            // Real V3 sections must resolve a guide. Synthetic tests can intentionally invent S99.
            V3BeginnerGuidanceResolver.find(section.id)
        } else {
            null
        }
        val title = if (guide != null) {
            V3BeginnerGuidanceResolver.decoratedTitle(section.id, section.title)
        } else {
            section.title
        }
        val recall = InlineLearningProblem(
            id = "$chapterId-${section.id}-P01",
            type = LearningProblemType.DIRECT_WRITE,
            prompt = if (guide != null) {
                "방금 읽은 ‘${section.title}’의 본문과 정답·해설을 확인했습니다. 이제 화면을 위로 보지 말고 자기 말로 다시 설명하세요. ‘무엇인지 → 왜 필요한지 → 실제 코드·앱에서 어떻게 쓰이는지’ 중 최소 2가지를 포함하세요. 여기서는 점수를 깎지 않으며, 실행 채점은 TRACK 실전에서 합니다."
            } else {
                "방금 읽은 ‘${section.title}’을 책을 보지 않고 자기 말로 설명하세요. 무엇인지, 왜 필요한지, 실제 코드·앱에서 어디에 쓰이는지 중 최소 2가지를 포함하세요. 여기서는 점수를 깎지 않으며, 실행 채점은 TRACK 실전에서 합니다."
            },
            referenceAnswer = guide?.answerPoints?.joinToString("\n").orEmpty()
        )

        return listOf(
            LearningConcept(
                id = "${section.id}-C01",
                index = 0,
                title = title,
                blocks = if (guide != null) {
                    V3BeginnerGuidanceResolver.decorateBlocks(section.id, section.blocks)
                } else {
                    section.blocks
                },
                problem = recall
            )
        )
    }

    fun normalizeAnswer(value: String): String = value
        .trim()
        .replace("\\s+".toRegex(), " ")
        .lowercase()
}
