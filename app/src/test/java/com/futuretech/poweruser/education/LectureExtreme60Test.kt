package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LectureExtreme60Test(
    private val scenarioId: Int,
    private val scenarioName: String
) {
    @Test
    fun extremeScenarioPasses() {
        assertTrue("Scenario $scenarioId failed: $scenarioName", evaluateScenario(scenarioId))
    }

    private fun evaluateScenario(id: Int): Boolean {
        val lessons = CurriculumDataRepository.allLessons
        val lectures = lessons.associate { it.lessonId to LectureContentRepository.forLesson(it) }
        val modules = lessons.groupBy { "${it.curriculumType}:${it.moduleNumber}" }.toSortedMap()

        return when (id) {
            in 1..18 -> {
                val module = modules.entries.elementAt(id - 1)
                module.value.all { lesson ->
                    val lecture = lectures.getValue(lesson.lessonId)
                    lecture.sections.size >= 12 && lecture.totalCharacters >= 2200
                }
            }
            in 19..28 -> {
                val bucket = id - 19
                lessons.filterIndexed { index, _ -> index % 10 == bucket }.all { lesson ->
                    val lecture = lectures.getValue(lesson.lessonId)
                    val terms = lecture.glossary.map { it.term }.toSet()
                    lesson.explainKeywords.all { it in terms }
                }
            }
            in 29..38 -> {
                val bucket = id - 29
                val required = setOf(
                    LectureSectionKind.WHY,
                    LectureSectionKind.FOUNDATION,
                    LectureSectionKind.DEFINITION,
                    LectureSectionKind.ANALOGY,
                    LectureSectionKind.FLOW,
                    LectureSectionKind.VOCABULARY,
                    LectureSectionKind.WORKED_EXAMPLE,
                    LectureSectionKind.SECOND_EXAMPLE,
                    LectureSectionKind.COMPARE,
                    LectureSectionKind.COMMON_MISTAKES,
                    LectureSectionKind.REAL_WORLD,
                    LectureSectionKind.RECAP,
                    LectureSectionKind.READINESS
                )
                lessons.filterIndexed { index, _ -> index % 10 == bucket }.all { lesson ->
                    lectures.getValue(lesson.lessonId).sections.map { it.kind }.toSet().containsAll(required)
                }
            }
            in 39..48 -> {
                val bucket = id - 39
                lessons.filterIndexed { index, _ -> index % 10 == bucket }.all { lesson ->
                    val lecture = lectures.getValue(lesson.lessonId)
                    lecture.sections.filter { it.kind in setOf(LectureSectionKind.WORKED_EXAMPLE, LectureSectionKind.SECOND_EXAMPLE) }
                        .all { section -> section.code.isNotBlank() && section.codeLineExplanations.isNotEmpty() }
                }
            }
            in 49..58 -> {
                val bucket = id - 49
                lessons.filterIndexed { index, _ -> index % 10 == bucket }.all { lesson ->
                    val lecture = lectures.getValue(lesson.lessonId)
                    lecture.estimatedLectureMinutes >= 20 &&
                        lecture.mustRemember.size in 3..7 &&
                        LectureQualityValidator.allPass(lecture)
                }
            }
            59 -> CurriculumDataRepository.beginnerLessons.all { lesson ->
                val lecture = lectures.getValue(lesson.lessonId)
                lecture.beginnerAssumption.contains("코딩") && lecture.beginnerAssumption.contains("처음")
            }
            60 -> lessons.all { lesson ->
                val lecture = lectures.getValue(lesson.lessonId)
                lecture.sections.first().kind == LectureSectionKind.WHY &&
                    lecture.sections.last().kind == LectureSectionKind.READINESS &&
                    lecture.sections.last().body.contains("문제")
            }
            else -> false
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Extreme {0}: {1}")
        fun data(): Collection<Array<Any>> {
            val names = buildList {
                repeat(18) { add("모듈 전체 강의 분량·구조 검증 ${it + 1}") }
                repeat(10) { add("핵심 용어 정의 누락 검증 ${it + 1}") }
                repeat(10) { add("필수 강의 섹션 누락 검증 ${it + 1}") }
                repeat(10) { add("코드 예제·줄별 해설 검증 ${it + 1}") }
                repeat(10) { add("CLEAN25·학습시간·핵심기억 검증 ${it + 1}") }
                add("완전 초보자 출발선 문구 검증")
                add("강의 완료 전 문제 차단 구조 데이터 검증")
            }
            require(names.size == 60)
            return names.mapIndexed { index, name -> arrayOf(index + 1, name) }
        }
    }
}
