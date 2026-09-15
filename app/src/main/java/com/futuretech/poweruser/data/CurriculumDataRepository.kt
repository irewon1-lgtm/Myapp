package com.futuretech.poweruser.data

data class LessonContent(
    val lessonId: String,
    val curriculumType: String, // BEGINNER, INTERMEDIATE or TEXTBOOK_V1
    val unitNumber: Int,
    val moduleNumber: Int,
    val moduleTitle: String,
    val stepNumber: Int,
    val stepTotal: Int = 5,
    val estimatedMinutes: Int = 25,
    val practiceLanguage: String = "PYTHON",
    val title: String,
    val explanation: String,
    val expectedOutcome: String,
    val codeSample: String,
    val initialPracticeCode: String,
    val fillInBlankPrompt: String,
    val brokenCode: String,
    val brokenCodeFix: String,
    val hintLevel1: String,
    val hintLevel2: String,
    val hintLevel3: String,
    val explainPrompt: String,
    val explainKeywords: List<String>,
    val aiHallucinationQuestion: String,
    val aiHallucinationOptions: List<String>,
    val correctOptionIndex: Int
)

internal data class LessonSeed(
    val id: String,
    val type: String,
    val sequence: Int,
    val module: Int,
    val moduleTitle: String,
    val step: Int,
    val title: String,
    val explanation: String,
    val outcome: String,
    val language: String,
    val code: String,
    val practice: String,
    val fill: String,
    val broken: String,
    val fix: String,
    val aiQuestion: String,
    val aiOptions: List<String>,
    val aiAnswer: Int,
    val keywords: List<String>,
    val minutes: Int = 25,
    val stepTotal: Int = 5
)

private fun buildLevel3Hint(broken: String, fix: String): String {
    val brokenLines = broken.lines()
    val fixedLines = fix.lines()
    val maxLines = maxOf(brokenLines.size, fixedLines.size)
    val firstChanged = (0 until maxLines).firstOrNull { index ->
        brokenLines.getOrNull(index)?.trimEnd() != fixedLines.getOrNull(index)?.trimEnd()
    }
    return if (firstChanged != null) {
        "힌트 3 · 거의 해결방법: ${firstChanged + 1}번째 줄을 중심으로 고장난 코드와 목표 동작의 차이를 최소 수정하세요. 정답 코드를 그대로 보여주지는 않습니다. 수정 후 직접 실행해 확인하세요."
    } else {
        "힌트 3 · 거의 해결방법: 입력→처리→출력 중 결과가 처음 달라지는 지점을 최소 수정하세요. 정답 코드를 그대로 보여주지는 않습니다."
    }
}

internal fun LessonSeed.toLesson(): LessonContent = LessonContent(
    lessonId = id,
    curriculumType = type,
    unitNumber = sequence,
    moduleNumber = module,
    moduleTitle = moduleTitle,
    stepNumber = step,
    stepTotal = stepTotal,
    estimatedMinutes = minutes,
    practiceLanguage = language,
    title = title,
    explanation = explanation,
    expectedOutcome = outcome,
    codeSample = code,
    initialPracticeCode = practice,
    fillInBlankPrompt = fill,
    brokenCode = broken,
    brokenCodeFix = fix,
    hintLevel1 = "힌트 1 · 방향: 정답을 바로 찾지 말고 학습 목표와 현재 코드의 차이를 한 문장으로 설명해 보세요.",
    hintLevel2 = "힌트 2 · 문제 위치: 문제가 생기는 줄 하나만 좁혀서 변수·조건·구조·데이터 흐름 중 무엇이 잘못됐는지 확인하세요.",
    hintLevel3 = buildLevel3Hint(broken, fix),
    explainPrompt = "${title}을(를) 처음 듣는 사람에게 3문장 이상으로 설명하세요. 무엇인지, 왜 필요한지, 실제 예시를 포함하세요.",
    explainKeywords = keywords,
    aiHallucinationQuestion = aiQuestion,
    aiHallucinationOptions = aiOptions,
    correctOptionIndex = aiAnswer
)

object CurriculumDataRepository {
    val beginnerLessons: List<LessonContent> = BeginnerCurriculum.seeds.map { it.toLesson() }

    // Items 31-35 keep their stable IDs/progress keys but use the rebuilt local-first lessons.
    val intermediateLessons: List<LessonContent> = IntermediateCurriculum.seeds
        .map { IntermediateCurriculum31To35.replace(it) }
        .map { it.toLesson() }

    val v1TextbookPracticeLessons: List<LessonContent> = V1TextbookPracticeData.lessons

    // Preserve the original 90-lesson contract for existing regression tests and legacy screens.
    val allLessons: List<LessonContent> = beginnerLessons + intermediateLessons

    fun lessonById(id: String): LessonContent? =
        v1TextbookPracticeLessons.find { it.lessonId == id } ?: allLessons.find { it.lessonId == id }
}
