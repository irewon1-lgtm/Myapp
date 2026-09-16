package com.futuretech.poweruser.education

data class PracticeSplit(
    val problemWeight: Float,
    val workspaceWeight: Float
)

enum class GuidancePhase(val label: String, val description: String) {
    WORKED_EXAMPLE("1 · 예제 관찰", "먼저 완성된 예제와 실행 흐름을 보고 결과를 예상합니다."),
    NEAR_TRANSFER("2 · 비슷한 문제", "같은 구조에서 값·조건을 직접 바꾸고 실행합니다."),
    PARTIAL_SUPPORT("3 · 도움 줄이기", "핵심만 회상하고 필요한 경우에만 단계형 힌트를 씁니다."),
    INDEPENDENT("4 · 혼자 작성", "예제를 보지 않고 직접 작성하고 실제 테스트로 검증합니다."),
    VARIATION("5 · 변형·디버깅", "고장난 코드와 다른 문맥의 문제에서 개념을 다시 적용합니다.")
}

object LearningUi21To25Policy {
    const val TABLET_BREAKPOINT_DP = 600
    const val TABLET_PROBLEM_WEIGHT = 0.40f
    const val TABLET_WORKSPACE_WEIGHT = 0.60f
    // E-book reader remains a centered single column even on wide tablets.
    const val READER_MAX_WIDTH_DP = 820

    val guidanceOrder: List<GuidancePhase> = GuidancePhase.entries
    val phonePracticeOrder: List<String> = listOf("문제", "코드", "결과")

    fun practiceSplit(
        smallestScreenWidthDp: Int,
        isLandscape: Boolean
    ): PracticeSplit? = if (smallestScreenWidthDp >= TABLET_BREAKPOINT_DP && isLandscape) {
        PracticeSplit(TABLET_PROBLEM_WEIGHT, TABLET_WORKSPACE_WEIGHT)
    } else {
        null
    }
}
