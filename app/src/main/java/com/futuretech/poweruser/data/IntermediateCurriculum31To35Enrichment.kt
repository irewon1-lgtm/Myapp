package com.futuretech.poweruser.data

/** Extra depth discovered by the items 31-35 quality gate. */
internal fun enrichIntermediate31To35(seed: LessonSeed): LessonSeed {
    if (seed.id != "I07-03") return seed

    val extra = """

        최소 수정은 단순히 코드 줄 수를 적게 바꾸라는 뜻이 아닙니다. '내 가설이 맞는지 확인하는 데 필요한 범위만 바꾼다'는 뜻입니다. 예를 들어 빈 입력 검증이 원인이라고 의심한다면 먼저 입력 검증 한 곳만 추가하고 같은 재현 절차를 실행합니다. 그 상태에서 문제가 사라지면 가설의 신뢰도가 올라갑니다. 문제가 그대로면 그 수정은 되돌리고 다음 가설로 넘어갑니다.

        전후 비교에는 관찰 기준도 미리 정해야 합니다. "느낌상 좋아졌다"가 아니라 같은 입력에서 crash가 사라졌는지, 기대 메시지가 나오는지, 기존 정상 입력도 그대로 처리되는지를 봅니다. 이렇게 해야 수정이 실제 원인을 해결했는지와 우연히 증상만 가렸는지를 구분할 수 있습니다.
    """.trimIndent()

    return seed.copy(explanation = seed.explanation + extra)
}
