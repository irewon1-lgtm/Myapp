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

data class LearningConcept(
    val id: String,
    val index: Int,
    val title: String,
    val blocks: List<TextbookBlock>,
    val problem: InlineLearningProblem
)

/**
 * Item 4-6 presentation layer.
 *
 * The 4-7 minute Section produced by [TextbookSectioner] remains intact as the curriculum unit.
 * This class only divides that Section into focused concept pages and attaches one low-stakes
 * retrieval problem to each page. Source markdown and the existing runtime/practice engine are
 * deliberately left untouched.
 */
object TextbookLearningFlow {
    const val MAX_BLOCKS_PER_CONCEPT = 6

    fun buildConcepts(
        chapterId: String,
        section: TextbookSection,
        lesson: LessonContent
    ): List<LearningConcept> {
        val rawConcepts = splitAtConceptHeadings(section)
        val bounded = rawConcepts.flatMap { (title, blocks) ->
            blocks.chunked(MAX_BLOCKS_PER_CONCEPT).mapIndexed { chunkIndex, chunk ->
                val suffix = if (chunkIndex == 0) "" else " · 계속 ${chunkIndex + 1}"
                "$title$suffix" to chunk
            }
        }.filter { it.second.isNotEmpty() }

        val safeConcepts = if (bounded.isEmpty()) {
            listOf(section.title to section.blocks.take(MAX_BLOCKS_PER_CONCEPT))
        } else {
            bounded
        }

        return safeConcepts.mapIndexed { index, (title, blocks) ->
            val typeIndex = (section.index * 3 + index) % LearningProblemType.entries.size
            val type = LearningProblemType.entries[typeIndex]
            LearningConcept(
                id = "${section.id}-C${(index + 1).toString().padStart(2, '0')}",
                index = index,
                title = title.ifBlank { section.title },
                blocks = blocks,
                problem = problemFor(
                    problemId = "$chapterId-${section.id}-P${(index + 1).toString().padStart(2, '0')}",
                    lesson = lesson,
                    type = type
                )
            )
        }
    }

    private fun splitAtConceptHeadings(section: TextbookSection): List<Pair<String, List<TextbookBlock>>> {
        val result = mutableListOf<Pair<String, List<TextbookBlock>>>()
        var title = section.title
        var current = mutableListOf<TextbookBlock>()

        fun hasSubstance(): Boolean = current.any { block ->
            block !is TextbookBlock.Heading && block != TextbookBlock.Divider
        }

        fun flush() {
            if (current.isNotEmpty()) {
                result += title to current.toList()
                current = mutableListOf()
            }
        }

        section.blocks.forEach { block ->
            val isConceptHeading = block is TextbookBlock.Heading && block.level in 3..4
            if (isConceptHeading && hasSubstance()) {
                flush()
                title = (block as TextbookBlock.Heading).text
            }
            current += block
        }
        flush()
        return result
    }

    private fun problemFor(
        problemId: String,
        lesson: LessonContent,
        type: LearningProblemType
    ): InlineLearningProblem = when (type) {
        LearningProblemType.CONCEPT_CHOICE -> InlineLearningProblem(
            id = problemId,
            type = type,
            prompt = lesson.aiHallucinationQuestion,
            options = lesson.aiHallucinationOptions,
            correctOptionIndex = lesson.correctOptionIndex
        )

        LearningProblemType.OUTPUT_PREDICTION -> InlineLearningProblem(
            id = problemId,
            type = type,
            prompt = "실행하기 전에 이 코드가 무엇을 출력하거나 수행할지 예상해 보세요.",
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
            InlineLearningProblem(
                id = problemId,
                type = type,
                prompt = "추측으로 고치지 않도록 진단 순서를 올바르게 배열하세요.",
                correctOrder = correct,
                shuffledOrder = listOf(correct[2], correct[0], correct[3], correct[1]),
                referenceAnswer = correct.joinToString(" → ")
            )
        }

        LearningProblemType.FILL_CODE -> {
            val parsed = parseFillBlank(lesson.fillInBlankPrompt)
            InlineLearningProblem(
                id = problemId,
                type = type,
                prompt = parsed.first,
                acceptedAnswers = parsed.second,
                referenceAnswer = parsed.second.firstOrNull().orEmpty()
            )
        }

        LearningProblemType.ONE_LINE_FIX -> {
            val changedLine = firstChangedLine(lesson.brokenCode, lesson.brokenCodeFix)
            InlineLearningProblem(
                id = problemId,
                type = type,
                prompt = "고장난 코드에서 가장 먼저 고칠 한 줄을 작성하세요.",
                code = lesson.brokenCode,
                acceptedAnswers = changedLine.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty(),
                referenceAnswer = changedLine
            )
        }

        LearningProblemType.DIRECT_WRITE -> InlineLearningProblem(
            id = problemId,
            type = type,
            prompt = "예제를 보지 않고 이번 개념의 최소 동작 코드를 직접 작성해 보세요.",
            referenceAnswer = lesson.initialPracticeCode
        )

        LearningProblemType.DEBUGGING -> InlineLearningProblem(
            id = problemId,
            type = type,
            prompt = "고장난 코드를 직접 수정해 보세요. 최종 통과 판정은 전체 실습의 실제 실행으로 확인합니다.",
            code = lesson.brokenCode,
            referenceAnswer = lesson.brokenCodeFix
        )

        LearningProblemType.AI_ANSWER_AUDIT -> InlineLearningProblem(
            id = problemId,
            type = type,
            prompt = "AI가 아래 답을 제시했다고 가정하고, 근거에 맞는 선택지를 고르세요.\n\n${lesson.aiHallucinationQuestion}",
            options = lesson.aiHallucinationOptions,
            correctOptionIndex = lesson.correctOptionIndex
        )
    }

    private fun parseFillBlank(prompt: String): Pair<String, List<String>> {
        val match = Regex("\\[([^]]+)]").find(prompt) ?: return prompt to emptyList()
        val answers = match.groupValues[1]
            .split("/", "|", ",")
            .map(String::trim)
            .filter(String::isNotBlank)
        return prompt.replaceRange(match.range, "[          ]") to answers
    }

    private fun firstChangedLine(before: String, after: String): String {
        val beforeLines = before.lines()
        val afterLines = after.lines()
        val count = maxOf(beforeLines.size, afterLines.size)
        for (index in 0 until count) {
            if (beforeLines.getOrNull(index)?.trim() != afterLines.getOrNull(index)?.trim()) {
                return afterLines.getOrNull(index)?.trim().orEmpty()
            }
        }
        return ""
    }

    fun normalizeAnswer(value: String): String = value
        .trim()
        .replace("\\s+".toRegex(), " ")
        .lowercase()
}
