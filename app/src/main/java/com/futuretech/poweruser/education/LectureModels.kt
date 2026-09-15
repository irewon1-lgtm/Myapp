package com.futuretech.poweruser.education

enum class LectureSectionKind {
    WHY,
    FOUNDATION,
    DEFINITION,
    ANALOGY,
    FLOW,
    VOCABULARY,
    WORKED_EXAMPLE,
    SECOND_EXAMPLE,
    COMPARE,
    COMMON_MISTAKES,
    REAL_WORLD,
    RECAP,
    READINESS
}

enum class LearningImportance(val label: String) {
    MUST_UNDERSTAND("반드시 이해"),
    MUST_PRACTICE("직접 할 수 있어야 함"),
    AI_CAN_HELP("AI에게 맡겨도 됨"),
    REFERENCE("참고")
}

data class GlossaryEntry(
    val term: String,
    val plainDefinition: String,
    val memoryHook: String
)

data class CodeLineExplanation(
    val lineNumber: Int,
    val code: String,
    val explanation: String
)

data class LectureSection(
    val id: String,
    val kind: LectureSectionKind,
    val title: String,
    val body: String,
    val importance: LearningImportance,
    val code: String = "",
    val codeLineExplanations: List<CodeLineExplanation> = emptyList(),
    val takeaway: String = ""
)

data class LessonLecture(
    val lessonId: String,
    val title: String,
    val beginnerAssumption: String,
    val estimatedLectureMinutes: Int,
    val sections: List<LectureSection>,
    val glossary: List<GlossaryEntry>,
    val mustRemember: List<String>
) {
    val totalCharacters: Int
        get() = sections.sumOf { it.body.length + it.takeaway.length + it.code.length }
}

data class LectureQualityCheck(
    val id: Int,
    val name: String,
    val passed: Boolean,
    val detail: String
)
