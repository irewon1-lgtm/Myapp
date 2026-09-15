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
    hintLevel1 = "정답을 바로 찾지 말고 학습 목표와 현재 코드의 차이를 한 문장으로 설명해 보세요.",
    hintLevel2 = "문제가 생기는 줄 하나만 좁혀서 변수·조건·구조·데이터 흐름 중 무엇이 잘못됐는지 확인하세요.",
    hintLevel3 = "수정 방향: ${fix.lines().firstOrNull().orEmpty()}",
    explainPrompt = "${title}을(를) 처음 듣는 사람에게 3문장 이상으로 설명하세요. 무엇인지, 왜 필요한지, 실제 예시를 포함하세요.",
    explainKeywords = keywords,
    aiHallucinationQuestion = aiQuestion,
    aiHallucinationOptions = aiOptions,
    correctOptionIndex = aiAnswer
)

object CurriculumDataRepository {
    val beginnerLessons: List<LessonContent> = BeginnerCurriculum.seeds.map { it.toLesson() }
    val intermediateLessons: List<LessonContent> = IntermediateCurriculum.seeds.map { it.toLesson() }
    val v1TextbookPracticeLessons: List<LessonContent> = V1TextbookPracticeData.lessons

    // Preserve the original 90-lesson contract for existing regression tests and legacy screens.
    val allLessons: List<LessonContent> = beginnerLessons + intermediateLessons

    fun lessonById(id: String): LessonContent? =
        v1TextbookPracticeLessons.find { it.lessonId == id } ?: allLessons.find { it.lessonId == id }
}
