package com.futuretech.poweruser.education

enum class UserFlowArea(val expectedCount: Int) {
    CURRICULUM_NAVIGATION(8),
    READING(8),
    PRACTICE(10),
    RUN_SUBMIT(10),
    FEEDBACK_HINT(8),
    MASTERY_REVIEW(6),
    DEVICE_STATE(6),
    OFFLINE_RUNTIME_PROJECT(4)
}

data class UserFlowScenario(
    val id: String,
    val area: UserFlowArea,
    val action: String,
    val successCriterion: String
)

data class VisualQualityGate(
    val id: String,
    val requirement: String
)

/**
 * Items 33-35 quality contract.
 *
 * This is deliberately separate from the older content-structure Extreme60 suites.
 * These cases describe what a learner actually does in the product. CI can validate
 * deterministic contracts cheaply; real-device-only claims must still be labelled as
 * device tests and must never be inferred from this JVM contract alone.
 */
object Items31To35QualityContract {
    const val EXTREME_SCENARIO_COUNT = 60
    const val CLEAN_GATE_COUNT = 25
    const val MIN_TOUCH_TARGET_DP = 48
    const val PHONE_WIDTH_GATE_DP = 360
    const val TABLET_TEXT_MAX_WIDTH_DP = 780
    const val TABLET_PROBLEM_WEIGHT_MIN = 0.38f
    const val TABLET_PROBLEM_WEIGHT_MAX = 0.42f

    private fun scenarios(
        area: UserFlowArea,
        start: Int,
        entries: List<Pair<String, String>>
    ): List<UserFlowScenario> = entries.mapIndexed { index, entry ->
        UserFlowScenario(
            id = "UF%02d".format(start + index),
            area = area,
            action = entry.first,
            successCriterion = entry.second
        )
    }

    val extreme60: List<UserFlowScenario> = buildList {
        addAll(scenarios(UserFlowArea.CURRICULUM_NAVIGATION, 1, listOf(
            "전체 과정에서 9권을 찾는다" to "9권 구조가 먼저 보이고 내부 엔진명은 전면에 나오지 않는다",
            "1권을 연다" to "권 안의 Chapter 목록으로 이동한다",
            "Chapter를 연다" to "읽기 화면으로 이동한다",
            "목차를 연다" to "임시 drawer로 나타나고 본문 폭을 영구 점유하지 않는다",
            "다른 Chapter를 고른다" to "선택한 Chapter로 이동하고 drawer가 닫힌다",
            "뒤로 간다" to "이전 학습 위치로 자연스럽게 돌아간다",
            "학습 홈으로 간다" to "오늘 할 행동만 보이는 단순 홈이 열린다",
            "전체 과정으로 돌아간다" to "과정 탐색 화면으로 복귀한다"
        )))
        addAll(scenarios(UserFlowArea.READING, 9, listOf(
            "20분 동안 본문을 읽는다" to "중앙 단일 컬럼과 제한된 본문 폭을 유지한다",
            "긴 Chapter를 스크롤한다" to "가로 스크롤 없이 세로 읽기가 유지된다",
            "코드 예제를 읽는다" to "본문과 코드 영역이 시각적으로 분리된다",
            "핵심 문장을 확인한다" to "한 화면의 핵심이 과도한 배지 없이 구분된다",
            "태블릿 세로로 읽는다" to "IDE식 분할 없이 책 형태를 유지한다",
            "태블릿 가로로 읽는다" to "읽기 화면은 여전히 단일 본문 컬럼이다",
            "목차를 닫고 계속 읽는다" to "본문 위치를 방해하는 영구 사이드바가 없다",
            "앱을 나갔다 돌아온다" to "저장된 Chapter/Section 위치에서 이어 읽는다"
        )))
        addAll(scenarios(UserFlowArea.PRACTICE, 17, listOf(
            "설명 직후 짧은 문제를 푼다" to "읽기와 문제 사이의 학습 루프가 끊기지 않는다",
            "실행 결과를 먼저 예상한다" to "실행 전 예측을 입력할 수 있다",
            "코드 한 부분을 수정한다" to "편집한 코드가 그대로 유지된다",
            "빈칸을 채운다" to "핵심 개념을 회상한 뒤 제출할 수 있다",
            "보지 않고 짧은 코드를 작성한다" to "실제 runtime 검증 단계로 이어진다",
            "고장난 코드를 디버깅한다" to "증상-원인-수정 흐름을 거친다",
            "AI가 만든 답을 검증한다" to "AI 자체가 채점자가 되지 않는다",
            "연습에서 여러 번 실패한다" to "재시도 횟수 때문에 감점되지 않는다",
            "연습에서 힌트를 쓴다" to "완료율은 유지되고 숙련 증거만 약해진다",
            "Challenge로 전환한다" to "힌트·AI·정답 보기가 잠긴다"
        )))
        addAll(scenarios(UserFlowArea.RUN_SUBMIT, 27, listOf(
            "정상 코드를 실행한다" to "실행 결과는 보이지만 숙련도는 변하지 않는다",
            "syntax error를 일부러 실행한다" to "실패 기록 없이 오류 증거를 확인한다",
            "logic error를 일부러 실행한다" to "자유 실험 후 코드를 계속 수정할 수 있다",
            "테스트 값을 직접 바꿔 실행한다" to "편집 코드 원본은 훼손되지 않는다",
            "수정한 코드를 제출한다" to "제출에서만 deterministic 판정이 발생한다",
            "공개 테스트를 실패한다" to "어떤 요구사항이 실패했는지 증거가 나온다",
            "숨은 테스트를 실패한다" to "정답 입력을 노출하지 않고 경계 실패를 알려준다",
            "실패 후 코드를 고친다" to "작성 코드가 사라지지 않고 재제출할 수 있다",
            "모든 테스트를 통과한다" to "통과 상태가 실제 테스트 결과로 결정된다",
            "같은 문제를 다시 실행한다" to "Run과 Submit 상태가 혼동되지 않는다"
        )))
        addAll(scenarios(UserFlowArea.FEEDBACK_HINT, 37, listOf(
            "오답을 제출한다" to "무엇이 실패했나-증거-이유-다음 확인점 순으로 설명한다",
            "힌트1을 연다" to "방향만 알려주고 정답은 주지 않는다",
            "힌트2를 연다" to "문제 위치를 지목하되 완성 답은 주지 않는다",
            "힌트3을 연다" to "거의 해결 방향까지 주되 사용자가 직접 수정한다",
            "힌트 후 다시 실행한다" to "힌트 사용 자체는 실패로 기록되지 않는다",
            "힌트 후 제출에 성공한다" to "숙련 단계는 독립 성공보다 낮게 기록된다",
            "관련 강의를 다시 연다" to "오답에서 학습 내용으로 돌아갈 수 있다",
            "같은 실수를 반복한다" to "내 오류와 복습 대상으로 연결된다"
        )))
        addAll(scenarios(UserFlowArea.MASTERY_REVIEW, 45, listOf(
            "무힌트 연습 6문제를 모두 통과한다" to "검증 가능이 아니라 AI 협업 단계까지 올라간다",
            "힌트1 또는 2를 쓰고 연습을 통과한다" to "응용 단계로 기록된다",
            "힌트3을 쓰고 연습을 통과한다" to "실행 단계와 복습 필요 증거가 남는다",
            "도움 없이 Challenge를 통과한다" to "검증 가능 단계로 올라간다",
            "며칠 뒤 변형 복습을 푼다" to "복습 예정 항목에서 다시 문제를 만난다",
            "이미 강한 숙련 뒤 안내형 연습을 한다" to "기존 강한 숙련 단계가 하향되지 않는다"
        )))
        addAll(scenarios(UserFlowArea.DEVICE_STATE, 51, listOf(
            "360dp 폰에서 실습한다" to "가로 넘침 없이 문제-코드-결과 세로 흐름을 사용한다",
            "태블릿 가로에서 실습한다" to "문제 약 40%, 작업공간 약 60%로 분할한다",
            "태블릿 세로로 돌린다" to "강제 분할을 해제하고 세로 흐름으로 전환한다",
            "화면을 회전한다" to "읽기 위치와 저장 가능한 학습 상태가 유지된다",
            "앱 프로세스가 재생성된다" to "영구 저장된 진행/읽기 위치를 다시 불러온다",
            "제출 실패 뒤 다시 수정한다" to "전체 화면 번쩍임 없이 같은 위치에서 결과를 확인한다"
        )))
        addAll(scenarios(UserFlowArea.OFFLINE_RUNTIME_PROJECT, 57, listOf(
            "오프라인에서 기존 학습을 연다" to "로컬 교재와 deterministic 실습은 가능한 범위에서 계속된다",
            "Python/TypeScript 코드를 로컬 실행한다" to "timeout과 sandbox 제한 안에서 결과가 반환된다",
            "프로젝트를 연다" to "배운 내용을 단계형 미션으로 연결한다",
            "홈에서 프로젝트를 선택한다" to "내부 테스트 엔진 설명 없이 프로젝트로 이동한다"
        )))
    }

    val clean25: List<VisualQualityGate> = listOf(
        VisualQualityGate("C01", "360dp 가로 넘침 0"),
        VisualQualityGate("C02", "태블릿 읽기 본문 폭 과도함 0"),
        VisualQualityGate("C03", "버튼 겹침 0"),
        VisualQualityGate("C04", "형광 강조 남용 0"),
        VisualQualityGate("C05", "본문 장문 벽 형태 블록 0"),
        VisualQualityGate("C06", "주요 터치 영역 48dp 미만 0"),
        VisualQualityGate("C07", "제출 시 전체 화면 흰색/검정 플래시 0"),
        VisualQualityGate("C08", "실습 중 작성 상태의 불필요한 소실 0"),
        VisualQualityGate("C09", "폰 실습은 영구 좌우분할 0"),
        VisualQualityGate("C10", "태블릿 가로 실습 문제영역 38~42%"),
        VisualQualityGate("C11", "태블릿 읽기는 단일 컬럼"),
        VisualQualityGate("C12", "읽기 목차는 임시 drawer"),
        VisualQualityGate("C13", "영구 Chapter 사이드바 0"),
        VisualQualityGate("C14", "정답/오답을 색상만으로 표현 0"),
        VisualQualityGate("C15", "실행과 제출 버튼의 역할이 텍스트로 구분됨"),
        VisualQualityGate("C16", "제출 실패 후 편집 내용을 즉시 계속 수정 가능"),
        VisualQualityGate("C17", "결과 패널이 현재 화면 맥락을 유지"),
        VisualQualityGate("C18", "코드 편집 영역 최소 실사용 높이 확보"),
        VisualQualityGate("C19", "본문/보조텍스트/코드의 시각 역할 분리"),
        VisualQualityGate("C20", "홈에서 문제엔진·hidden test 내부용어 노출 0"),
        VisualQualityGate("C21", "홈 핵심 행동은 이어서/복습/프로젝트/전체과정 중심"),
        VisualQualityGate("C22", "숙련도를 단일 숫자 점수로 표시하지 않음"),
        VisualQualityGate("C23", "Challenge와 연습의 도움 정책이 명확히 다름"),
        VisualQualityGate("C24", "읽기 위치는 앱 재생성 후 복원 가능"),
        VisualQualityGate("C25", "오프라인/실패 상태가 학습 전체를 불필요하게 막지 않음")
    )
}
