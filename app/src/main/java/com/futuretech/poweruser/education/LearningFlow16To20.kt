package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.PersonalErrorNoteEntity
import com.futuretech.poweruser.data.SpacedRepetitionItemEntity

object ErrorDrivenPracticeEngine {
    const val CHALLENGE_PROBLEM_COUNT = 10

    private val baseTypes = listOf(
        LessonProblemType.PREDICT_OUTPUT,
        LessonProblemType.MODIFY_AND_RUN,
        LessonProblemType.FILL_BLANK,
        LessonProblemType.WRITE_FROM_MEMORY,
        LessonProblemType.DEBUG,
        LessonProblemType.VERIFY_AI_ANSWER
    )

    private val defaultExtras = listOf(
        LessonProblemType.DEBUG,
        LessonProblemType.MODIFY_AND_RUN,
        LessonProblemType.PREDICT_OUTPUT,
        LessonProblemType.VERIFY_AI_ANSWER
    )

    fun problemTypeFor(note: PersonalErrorNoteEntity): LessonProblemType {
        val text = listOf(note.errorType, note.errorMessage, note.correctionGuide, note.codeSnippet)
            .joinToString(" ")
            .lowercase()
        return when {
            listOf("fill", "blank", "빈칸", "json comma", "comma", "쉼표").any { it in text } -> LessonProblemType.FILL_BLANK
            listOf("debug", "syntax", "runtime", "exception", "indent", "오류", "에러", ">=").any { it in text } -> LessonProblemType.DEBUG
            listOf("write", "memory", "직접 작성", "빈 배열", "empty array").any { it in text } -> LessonProblemType.WRITE_FROM_MEMORY
            listOf("modify", "run", "수정", "get/post", "get ", "post ").any { it in text } -> LessonProblemType.MODIFY_AND_RUN
            listOf("ai_verify", "ai 답", "검증 오답", "hallucination").any { it in text } -> LessonProblemType.VERIFY_AI_ANSWER
            else -> LessonProblemType.PREDICT_OUTPUT
        }
    }

    fun weightFor(note: PersonalErrorNoteEntity): Int = 1 + note.occurrenceCount.coerceIn(1, 8)

    fun challengeFlow(errorNotes: List<PersonalErrorNoteEntity>): List<LessonProblemType> {
        val weightedPool = errorNotes
            .sortedWith(compareByDescending<PersonalErrorNoteEntity> { weightFor(it) }
                .thenByDescending { it.lastOccurredTimestamp }
                .thenBy { it.errorType })
            .flatMap { note -> List(weightFor(note)) { problemTypeFor(note) } }

        val extras = mutableListOf<LessonProblemType>()
        val candidates = if (weightedPool.isNotEmpty()) weightedPool else defaultExtras
        var cursor = 0
        while (extras.size < CHALLENGE_PROBLEM_COUNT - baseTypes.size) {
            var candidate = candidates[cursor % candidates.size]
            cursor++
            val previous = (baseTypes + extras).lastOrNull()
            if (candidate == previous) {
                candidate = baseTypes.firstOrNull { it != previous && it !in extras.takeLast(1) }
                    ?: defaultExtras.first { it != previous }
            }
            extras += candidate
        }

        val flow = (baseTypes + extras).toMutableList()
        for (index in 1 until flow.size) {
            if (flow[index] == flow[index - 1]) {
                val swapIndex = (index + 1 until flow.size).firstOrNull { flow[it] != flow[index - 1] }
                if (swapIndex != null) {
                    val temp = flow[index]
                    flow[index] = flow[swapIndex]
                    flow[swapIndex] = temp
                } else {
                    flow[index] = baseTypes.first { it != flow[index - 1] }
                }
            }
        }
        return flow.take(CHALLENGE_PROBLEM_COUNT)
    }

    fun priorityLabel(errorNotes: List<PersonalErrorNoteEntity>): String {
        val top = errorNotes.maxByOrNull { weightFor(it) } ?: return "기본 균형 출제"
        return "내 오류 우선: ${top.errorType} · ${top.occurrenceCount}회"
    }
}

data class ReviewVariation(val prompt: String, val changedExample: String?, val focus: String)

object ReviewVariationEngine {
    fun forItem(item: SpacedRepetitionItemEntity): ReviewVariation {
        val stage = item.reviewCount.coerceAtLeast(0) % 4
        return when (stage) {
            0 -> ReviewVariation(
                "정의를 보지 않고 이 개념을 본인 말로 설명하세요. 무엇인지와 언제 쓰는지를 함께 말하세요.",
                null,
                "단순 문장 암기가 아니라 개념의 역할을 회상합니다."
            )
            1 -> ReviewVariation(
                "비슷한 개념과 무엇이 다른지 먼저 말한 뒤, 실제 상황 하나를 새로 만들어 설명하세요.",
                item.comparison.takeIf { it.isNotBlank() },
                "같은 답을 외우지 않고 구분 기준을 재구성합니다."
            )
            2 -> ReviewVariation(
                "아래 예시는 원래 학습 때와 입력을 바꾼 변형입니다. 실행 결과나 의미가 어떻게 달라지는지 먼저 예상하세요.",
                varyExample(item.example, item.reviewCount),
                "입력·문맥이 바뀌어도 같은 개념을 적용할 수 있는지 확인합니다."
            )
            else -> ReviewVariation(
                "이 개념을 처음 보는 다른 문제에 적용한다고 가정하세요. 어떤 순서로 판단할지 2~3단계로 말하세요.",
                item.analogy.takeIf { it.isNotBlank() },
                "회상이 아니라 전이(transfer)를 확인합니다."
            )
        }
    }

    private fun varyExample(example: String, seed: Int): String? {
        if (example.isBlank()) return null
        val number = Regex("-?\\d+(?:\\.\\d+)?").find(example)
        if (number != null) {
            val raw = number.value
            val replacement = raw.toDoubleOrNull()?.let { value ->
                val changed = value + (seed.coerceAtLeast(1) % 5 + 1)
                if (raw.contains('.')) changed.toString() else changed.toLong().toString()
            }
            if (replacement != null) return example.replaceRange(number.range, replacement)
        }
        val quoted = Regex("(['\"])(.+?)\\1").find(example)
        if (quoted != null) {
            val quote = quoted.groupValues[1]
            val body = quoted.groupValues[2]
            return example.replaceRange(quoted.range, "$quote${body}_변형$quote")
        }
        return "$example\n# 같은 개념을 다른 입력/문맥에 적용해 보세요."
    }
}

data class ProjectStage(
    val stage: Int,
    val title: String,
    val mission: String,
    val requiredLessonIds: Set<String>,
    val unlocked: Boolean
)

object ProjectStageEngine {
    fun beginnerStages(completedLessonIds: Set<String>): List<ProjectStage> = listOf(
        stage(1, "입력 화면 만들기", "종목명과 두 개의 숫자 입력을 받아 화면 상태로 보관합니다.", emptySet(), completedLessonIds),
        stage(2, "계산 로직 연결", "조건문과 숫자 변환으로 점수 계산식을 연결합니다.", setOf("B03-01"), completedLessonIds),
        stage(3, "등급 분기 만들기", "계산 결과를 A/B/C 등급으로 분기하고 예외 입력을 처리합니다.", setOf("B03-01", "B04-01"), completedLessonIds),
        stage(4, "완성·검증", "여러 입력으로 직접 시험하고 틀린 경우를 고쳐 프로젝트를 완성합니다.", setOf("B03-01", "B04-01", "B05-01"), completedLessonIds)
    )

    fun intermediateStages(completedLessonIds: Set<String>): List<ProjectStage> = listOf(
        stage(1, "검색 입력", "연구할 종목을 입력하고 요청 단계를 확인합니다.", emptySet(), completedLessonIds),
        stage(2, "데이터 저장", "SQL 연습 DB에 저장·조회 흐름을 연결합니다.", setOf("I01-01"), completedLessonIds),
        stage(3, "계산 파이프라인", "Python 계산 단계를 연결하고 실제 실행 결과를 확인합니다.", setOf("I01-01", "I02-01"), completedLessonIds),
        stage(4, "보고서 조립", "검색→저장→계산→검증 결과를 한 화면에 조립합니다.", setOf("I01-01", "I02-01", "I03-01"), completedLessonIds)
    )

    private fun stage(number: Int, title: String, mission: String, required: Set<String>, completed: Set<String>) = ProjectStage(
        number, title, mission, required, required.all { it in completed }
    )
}
