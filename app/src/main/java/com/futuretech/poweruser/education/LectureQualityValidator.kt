package com.futuretech.poweruser.education

object LectureQualityValidator {
    const val CRITERIA_COUNT = 25

    fun validate(lecture: LessonLecture): List<LectureQualityCheck> {
        val kinds = lecture.sections.map { it.kind }.toSet()
        val allText = lecture.sections.joinToString("\n") { "${it.title}\n${it.body}\n${it.takeaway}\n${it.code}" }
        val glossaryTerms = lecture.glossary.map { it.term }.toSet()
        val sectionIds = lecture.sections.map { it.id }
        val worked = lecture.sections.firstOrNull { it.kind == LectureSectionKind.WORKED_EXAMPLE }
        val second = lecture.sections.firstOrNull { it.kind == LectureSectionKind.SECOND_EXAMPLE }

        return listOf(
            check(1, "초보자 가정 명시", lecture.beginnerAssumption.contains("코딩") && lecture.beginnerAssumption.length >= 40, lecture.beginnerAssumption),
            check(2, "왜 배우는지 설명", LectureSectionKind.WHY in kinds, "WHY section"),
            check(3, "큰 그림 선행", LectureSectionKind.FOUNDATION in kinds, "FOUNDATION section"),
            check(4, "쉬운 설명과 정확한 설명", LectureSectionKind.DEFINITION in kinds, "DEFINITION section"),
            check(5, "현실 비유", LectureSectionKind.ANALOGY in kinds, "ANALOGY section"),
            check(6, "흐름 설명", LectureSectionKind.FLOW in kinds, "FLOW section"),
            check(7, "신규 용어 정리", LectureSectionKind.VOCABULARY in kinds && lecture.glossary.isNotEmpty(), "glossary=${lecture.glossary.size}"),
            check(8, "모든 핵심어 정의", lecture.mustRemember.all { it in glossaryTerms }, "mustRemember=${lecture.mustRemember}"),
            check(9, "첫 완성 예제", worked != null && worked.code.isNotBlank(), "worked example"),
            check(10, "코드 줄별 해설", worked != null && worked.codeLineExplanations.isNotEmpty() && worked.codeLineExplanations.size == worked.code.lines().count { it.isNotBlank() }, "worked line explanations"),
            check(11, "두 번째 변형 예제", second != null && second.code.isNotBlank(), "second example"),
            check(12, "두 번째 예제 줄별 해설", second != null && second.codeLineExplanations.isNotEmpty(), "second line explanations"),
            check(13, "헷갈리는 개념 비교", LectureSectionKind.COMPARE in kinds, "COMPARE section"),
            check(14, "흔한 실수", LectureSectionKind.COMMON_MISTAKES in kinds, "COMMON_MISTAKES section"),
            check(15, "잘못된 예와 수정 예", allText.contains("잘못된 예") && allText.contains("수정 예"), "broken/fixed example"),
            check(16, "실제 앱 연결", LectureSectionKind.REAL_WORLD in kinds, "REAL_WORLD section"),
            check(17, "핵심 정리", LectureSectionKind.RECAP in kinds, "RECAP section"),
            check(18, "문제 전 준비 확인", lecture.sections.lastOrNull()?.kind == LectureSectionKind.READINESS, "last=${lecture.sections.lastOrNull()?.kind}"),
            check(19, "강의 카드 충분한 분량", lecture.sections.size >= 12, "sections=${lecture.sections.size}"),
            check(20, "전체 설명 밀도", lecture.totalCharacters >= 2200, "chars=${lecture.totalCharacters}"),
            check(21, "짧은 빈 강의 금지", lecture.sections.all { it.body.trim().length >= 70 }, "minBody=${lecture.sections.minOfOrNull { it.body.trim().length }}"),
            check(22, "섹션 ID 중복 금지", sectionIds.size == sectionIds.distinct().size, "ids=${sectionIds.size}"),
            check(23, "강의 예상시간 20분 이상", lecture.estimatedLectureMinutes >= 20, "minutes=${lecture.estimatedLectureMinutes}"),
            check(24, "핵심 기억 항목 3~7개", lecture.mustRemember.size in 3..7, "mustRemember=${lecture.mustRemember.size}"),
            check(25, "미완성 placeholder 금지", listOf("TODO", "TBD", "LOREM", "준비중", "나중에 작성").none { allText.uppercase().contains(it.uppercase()) }, "placeholder scan")
        )
    }

    fun allPass(lecture: LessonLecture): Boolean = validate(lecture).all { it.passed }

    private fun check(id: Int, name: String, passed: Boolean, detail: String) =
        LectureQualityCheck(id, name, passed, detail)
}
