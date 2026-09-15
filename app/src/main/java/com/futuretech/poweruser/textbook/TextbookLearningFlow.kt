package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.LessonContent

enum class LearningProblemType(val displayName: String) {
    CONCEPT_CHOICE("개념 선택"),
    OUTPUT_PREDICTION("실행결과 예상"),
    ORDERING("순서배치"),
    FILL_CODE("빈칸코드"),
    ONE_LINE_FIX("한 줄 수정"),
    DIRECT_WRITE("직접작성"),
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

data class LearningSection(
    val id: String,
    val title: String,
    val blocks: List<TextbookBlock>,
    val problem: InlineLearningProblem
)

object TextbookLearningFlow {
    const val MAX_CONTENT_BLOCKS_PER_SECTION = 6

    fun buildSections(
        chapter: TextbookChapter,
        blocks: List<TextbookBlock>,
        lesson: LessonContent
    ): List<LearningSection> {
        val rawSections = splitIntoConceptSections(blocks)
        val bounded = rawSections.flatMap { chunkSection(it.first, it.second) }
        val safeSections = if (bounded.isEmpty()) listOf(chapter.title to blocks.take(MAX_CONTENT_BLOCKS_PER_SECTION)) else bounded

        return safeSections.mapIndexed { index, (title, sectionBlocks) ->
            val type = LearningProblemType.entries[(chapter.number - 1 + index) % LearningProblemType.entries.size]
            LearningSection(
                id = "${chapter.id}-S%02d".format(index + 1),
                title = title.ifBlank { chapter.title },
                blocks = sectionBlocks,
                problem = problemFor(chapter, lesson, index, type)
            )
        }
    }

    private fun splitIntoConceptSections(blocks: List<TextbookBlock>): List<Pair<String, List<TextbookBlock>>> {
        val result = mutableListOf<Pair<String, List<TextbookBlock>>>()
        var title = "핵심 개념"
        var current = mutableListOf<TextbookBlock>()

        fun flush() {
            if (current.isNotEmpty()) {
                result += title to current.toList()
                current = mutableListOf()
            }
        }

        blocks.forEachIndexed { index, block ->
            if (block is TextbookBlock.Heading && block.level == 2 && index == 0) {
                return@forEachIndexed
            }
            if (block is TextbookBlock.Heading && block.level in 3..4) {
                flush()
                title = block.text
            } else {
                current += block
            }
        }
        flush()
        return result
    }

    private fun chunkSection(title: String, blocks: List<TextbookBlock>): List<Pair<String, List<TextbookBlock>>> {
        if (blocks.size <= MAX_CONTENT_BLOCKS_PER_SECTION) return listOf(title to blocks)
        return blocks.chunked(MAX_CONTENT_BLOCKS_PER_SECTION).mapIndexed { index, chunk ->
            val suffix = if (index == 0) "" else " · 계속 ${index + 1}"
            "$title$suffix" to chunk
        }
    }

    private fun problemFor(
        chapter: TextbookChapter,
        lesson: LessonContent,
        index: Int,
        type: LearningProblemType
    ): InlineLearningProblem {
        val id = "${chapter.id}-P%02d".format(index + 1)
        return when (type) {
            LearningProblemType.CONCEPT_CHOICE -> InlineLearningProblem(
                id = id,
                type = type,
                prompt = lesson.aiHallucinationQuestion,
                options = lesson.aiHallucinationOptions,
                correctOptionIndex = lesson.correctOptionIndex
            )

            LearningProblemType.OUTPUT_PREDICTION -> InlineLearningProblem(
                id = id,
                type = type,
                prompt = "코드를 실행하기 전에 무엇을 출력하거나 수행할지 한두 문장으로 예상하세요.",
                code = lesson.codeSample,
                referenceAnswer = lesson.expectedOutcome
            )

            LearningProblemType.ORDERING -> {
                val correct = listOf(
                    "증상을 구체적으로 기록한다",
                    "관련 경계와 후보를 나눈다",
                    "가장 싼 증거부터 확인한다",
                    "가설 하나만 바꿔 다시 검증한다"
                )
                val shuffled = listOf(correct[2], correct[0], correct[3], correct[1])
                InlineLearningProblem(
                    id = id,
                    type = type,
                    prompt = "문제를 추측으로 고치지 않도록 진단 순서를 올바르게 배열하세요.",
                    correctOrder = correct,
                    shuffledOrder = shuffled,
                    referenceAnswer = correct.joinToString(" → ")
                )
            }

            LearningProblemType.FILL_CODE -> {
                val parsed = parseFillBlank(lesson.fillInBlankPrompt)
                InlineLearningProblem(
                    id = id,
                    type = type,
                    prompt = parsed.first,
                    acceptedAnswers = parsed.second,
                    referenceAnswer = parsed.second.firstOrNull().orEmpty()
                )
            }

            LearningProblemType.ONE_LINE_FIX -> InlineLearningProblem(
                id = id,
                type = type,
                prompt = "고장난 코드에서 가장 먼저 고칠 한 줄을 작성하세요.",
                code = lesson.brokenCode,
                acceptedAnswers = firstChangedLine(lesson.brokenCode, lesson.brokenCodeFix).let { if (it.isBlank()) emptyList() else listOf(it) },
                referenceAnswer = firstChangedLine(lesson.brokenCode, lesson.brokenCodeFix)
            )

            LearningProblemType.DIRECT_WRITE -> InlineLearningProblem(
                id = id,
                type = type,
                prompt = "예제를 보지 않고 이번 개념의 최소 동작 코드를 직접 작성해 보세요. 이 미니문제에서는 점수화하지 않고 전체 실습에서 실제 실행으로 검증합니다.",
                referenceAnswer = lesson.initialPracticeCode
            )

            LearningProblemType.DEBUGGING -> InlineLearningProblem(
                id = id,
                type = type,
                prompt = "고장난 코드를 직접 수정해 보세요. 여기서는 비교만 하고 최종 통과는 전체 실습의 실제 실행 결과로 판정합니다.",
                code = lesson.brokenCode,
                referenceAnswer = lesson.brokenCodeFix
            )

            LearningProblemType.AI_ANSWER_AUDIT -> InlineLearningProblem(
                id = id,
                type = type,
                prompt = "AI가 제시했다고 가정하고, 아래 선택지 중 근거에 맞는 답을 고르세요.\n\n${lesson.aiHallucinationQuestion}",
                options = lesson.aiHallucinationOptions,
                correctOptionIndex = lesson.correctOptionIndex
            )
        }
    }

    private fun parseFillBlank(prompt: String): Pair<String, List<String>> {
        val match = Regex("\\[([^]]+)]").find(prompt)
        if (match == null) return prompt to emptyList()
        val answers = match.groupValues[1]
            .split("/", "|", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return prompt.replaceRange(match.range, "[          ]") to answers
    }

    private fun firstChangedLine(before: String, after: String): String {
        val beforeLines = before.lines()
        val afterLines = after.lines()
        val max = maxOf(beforeLines.size, afterLines.size)
        for (i in 0 until max) {
            if (beforeLines.getOrNull(i)?.trim() != afterLines.getOrNull(i)?.trim()) {
                return afterLines.getOrNull(i)?.trim().orEmpty()
            }
        }
        return ""
    }

    fun normalizeAnswer(value: String): String = value
        .trim()
        .replace("\\s+".toRegex(), " ")
        .lowercase()
}
