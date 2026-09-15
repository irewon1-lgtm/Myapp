package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.LessonContent

private data class ModuleGuide(
    val bigPicture: String,
    val analogy: String,
    val flow: String,
    val compare: String,
    val realWorld: String,
    val mistakes: String,
    val glossary: Map<String, String>
)

object LectureContentRepository {
    private const val NOVICE_ASSUMPTION =
        "일반적인 스마트폰·인터넷 사용만 할 수 있고 코딩 용어는 처음 듣는 학습자를 기준으로 설명합니다. 아는 척하고 넘어가지 않고, 새 용어는 먼저 쉬운 말로 정의한 뒤 정확한 의미를 붙입니다."

    fun forLesson(lesson: LessonContent): LessonLecture {
        val guide = moduleGuides["${lesson.curriculumType}:${lesson.moduleNumber}"] ?: genericGuide(lesson)
        val glossary = lesson.explainKeywords.distinct().map { keyword ->
            GlossaryEntry(
                term = keyword,
                plainDefinition = guide.glossary[keyword] ?: fallbackDefinition(keyword, lesson),
                memoryHook = memoryHook(keyword, lesson)
            )
        }
        val workedLines = explainCode(lesson.codeSample, lesson.practiceLanguage)
        val secondLines = explainCode(lesson.initialPracticeCode, lesson.practiceLanguage)
        val keywordText = glossary.joinToString("\n\n") { "• ${it.term}: ${it.plainDefinition}\n  기억법: ${it.memoryHook}" }
        val brokenFixed = "잘못된 예\n${lesson.brokenCode}\n\n수정 예\n${lesson.brokenCodeFix}"

        val sections = listOf(
            LectureSection(
                id = "${lesson.lessonId}-why",
                kind = LectureSectionKind.WHY,
                title = "1. 왜 이걸 배우는가",
                body = "${lesson.title}은(는) 외우기 위한 지식이 아니라, 실제 앱과 AI 작업을 이해하기 위한 도구입니다. 지금 단계에서는 전문 개발자가 되는 것이 목표가 아닙니다. 먼저 ‘이 개념이 어디에 쓰이고, 없으면 무엇이 불편한가’를 잡습니다.\n\n이번 레슨의 최종 목표는 다음입니다. ${lesson.expectedOutcome}\n\n처음 듣는 단어가 있어도 정상입니다. 이 강의에서는 문제를 먼저 풀게 하지 않고, 의미와 예시를 충분히 본 다음 실습으로 넘어갑니다.",
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "목표: ${lesson.expectedOutcome}"
            ),
            LectureSection(
                id = "${lesson.lessonId}-foundation",
                kind = LectureSectionKind.FOUNDATION,
                title = "2. 먼저 큰 그림부터",
                body = guide.bigPicture,
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "세부 문법보다 먼저 전체 흐름에서 이 개념의 자리를 잡으세요."
            ),
            LectureSection(
                id = "${lesson.lessonId}-definition",
                kind = LectureSectionKind.DEFINITION,
                title = "3. 쉬운 설명 → 정확한 설명",
                body = "쉬운 말로 먼저 보면 이렇습니다. ${lesson.explanation}\n\n정확한 관점에서는 ‘${lesson.title}’을(를) 입력, 상태, 처리, 결과 중 어디에 놓아야 하는지 구분할 수 있어야 합니다. 이름을 외우는 것보다 ‘무슨 역할을 하고 무엇과 다른지’를 설명할 수 있으면 제대로 이해한 것입니다.",
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "정의만 외우지 말고 역할과 경계를 같이 기억합니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-analogy",
                kind = LectureSectionKind.ANALOGY,
                title = "4. 현실 비유로 감 잡기",
                body = guide.analogy,
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "비유는 이해를 돕는 도구일 뿐 실제 구조와 완전히 같지는 않습니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-flow",
                kind = LectureSectionKind.FLOW,
                title = "5. 흐름으로 보기",
                body = "개념을 단독으로 외우면 금방 잊습니다. 실제 작업에서는 앞 단계의 결과가 다음 단계의 입력이 됩니다.\n\n${guide.flow}\n\n이 레슨에서 확인할 핵심은 ‘어디서 시작해서 어디로 전달되고, 실패하면 어느 구간을 먼저 볼 것인가’입니다.",
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "항상 입력 → 처리 → 결과, 또는 요청 → 응답처럼 흐름으로 추적합니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-vocabulary",
                kind = LectureSectionKind.VOCABULARY,
                title = "6. 오늘 처음 나오는 말 정리",
                body = keywordText,
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "모르는 용어가 남아 있으면 문제로 넘어가기 전에 이 페이지를 다시 봅니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-worked",
                kind = LectureSectionKind.WORKED_EXAMPLE,
                title = "7. 첫 번째 예제 — 한 줄씩 읽기",
                body = "이제 완성된 예제를 먼저 봅니다. 아직 직접 쓰려고 하지 않아도 됩니다. 각 줄이 어떤 역할을 하는지만 읽습니다.\n\n${workedLines.joinToString("\n") { "${it.lineNumber}줄: ${it.explanation}" }}",
                importance = LearningImportance.MUST_PRACTICE,
                code = lesson.codeSample,
                codeLineExplanations = workedLines,
                takeaway = "코드를 통째로 외우지 말고 각 줄의 역할을 말로 설명해 봅니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-second",
                kind = LectureSectionKind.SECOND_EXAMPLE,
                title = "8. 두 번째 예제 — 맥락을 바꿔 보기",
                body = "첫 예제와 비슷한 구조지만 값이나 상황을 바꾼 예제입니다. 같은 개념이 다른 상황에서도 반복해서 쓰인다는 점을 보세요.\n\n${secondLines.joinToString("\n") { "${it.lineNumber}줄: ${it.explanation}" }}\n\n두 예제를 비교해 ‘바뀐 부분’과 ‘그대로인 구조’를 나눌 수 있으면 암기보다 훨씬 오래 남습니다.",
                importance = LearningImportance.MUST_PRACTICE,
                code = lesson.initialPracticeCode,
                codeLineExplanations = secondLines,
                takeaway = "구조는 유지되고 값·조건·데이터만 바뀌는 부분을 찾아보세요."
            ),
            LectureSection(
                id = "${lesson.lessonId}-compare",
                kind = LectureSectionKind.COMPARE,
                title = "9. 헷갈리는 개념과 구분하기",
                body = guide.compare,
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "비슷해 보이는 용어는 ‘역할, 입력, 결과, 실행 위치’ 중 하나를 기준으로 구분합니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-mistakes",
                kind = LectureSectionKind.COMMON_MISTAKES,
                title = "10. 초보자가 자주 틀리는 지점",
                body = "${guide.mistakes}\n\n이 레슨의 고장난 예제도 같이 보세요. 정답을 외우기보다 무엇이 달라졌는지 비교합니다.",
                importance = LearningImportance.MUST_PRACTICE,
                code = brokenFixed,
                takeaway = "오류는 실패가 아니라 ‘어떤 규칙을 아직 구분하지 못했는지’ 알려주는 자료입니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-real",
                kind = LectureSectionKind.REAL_WORLD,
                title = "11. 실제 앱에서는 어디에 쓰이나",
                body = "${guide.realWorld}\n\n이 레슨을 마치면 실제 작업에서 다음을 할 수 있어야 합니다. ${lesson.expectedOutcome}",
                importance = LearningImportance.MUST_PRACTICE,
                takeaway = "배운 개념을 실제 앱·AI 작업 한 장면과 연결해서 기억합니다."
            ),
            LectureSection(
                id = "${lesson.lessonId}-recap",
                kind = LectureSectionKind.RECAP,
                title = "12. 핵심 정리",
                body = buildString {
                    append("오늘 반드시 기억할 것은 많지 않습니다. 아래 항목을 자기 말로 설명할 수 있으면 충분합니다.\n\n")
                    glossary.forEach { append("• ${it.term}: ${it.plainDefinition}\n") }
                    append("\nAI에게 맡겨도 되는 부분은 긴 코드 초안이나 반복 작업입니다. 하지만 구조, 결과 확인, 오류 판단은 사람이 이해해야 합니다.")
                },
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "핵심 용어 ${glossary.joinToString(", ") { it.term }}"
            ),
            LectureSection(
                id = "${lesson.lessonId}-ready",
                kind = LectureSectionKind.READINESS,
                title = "13. 문제로 넘어가기 전 준비 확인",
                body = "다음 질문에 머릿속으로 답해보세요.\n\n1) ${lesson.title}은(는) 무엇인가?\n2) 왜 필요한가?\n3) 위 예제에서 핵심 줄은 어디인가?\n4) 초보자가 자주 틀리는 지점은 무엇인가?\n5) 실제 앱에서는 어디에 쓰이는가?\n\n모두 완벽히 말할 필요는 없습니다. 최소한 ‘아, 이건 이런 역할이구나’가 잡혔다면 문제와 실습으로 넘어가도 됩니다. 이해가 흐릿하면 이전 강의 카드로 돌아가세요.",
                importance = LearningImportance.MUST_UNDERSTAND,
                takeaway = "강의 완료 후에만 문제·실습이 열립니다."
            )
        )

        return LessonLecture(
            lessonId = lesson.lessonId,
            title = lesson.title,
            beginnerAssumption = NOVICE_ASSUMPTION,
            estimatedLectureMinutes = maxOf(20, lesson.estimatedMinutes),
            sections = sections,
            glossary = glossary,
            mustRemember = glossary.take(7).map { it.term }
        )
    }

    private fun explainCode(code: String, language: String): List<CodeLineExplanation> =
        code.lines().filter { it.isNotBlank() }.mapIndexed { index, raw ->
            CodeLineExplanation(index + 1, raw, explainLine(raw.trim(), language))
        }

    private fun explainLine(line: String, language: String): String = when {
        line.startsWith("#") || line.startsWith("//") -> "사람에게 설명하는 주석입니다. 실행 결과보다 코드 의도를 남기는 데 씁니다."
        line.startsWith("def ") -> "반복해서 사용할 작업을 함수로 정의합니다. 함수 이름과 입력값을 먼저 확인합니다."
        line.startsWith("if ") -> "조건을 검사합니다. 조건이 참인지 거짓인지에 따라 다음 행동이 달라집니다."
        line.startsWith("elif ") || line.startsWith("else") -> "앞 조건이 맞지 않았을 때 다른 경우를 처리하는 분기입니다."
        line.startsWith("for ") -> "여러 값에 같은 작업을 반복합니다. 무엇을 순회하는지 확인합니다."
        line.startsWith("while ") -> "조건이 참인 동안 반복합니다. 종료 조건이 없으면 무한 반복이 될 수 있습니다."
        line.contains("print(") || line.contains("console.log") -> "현재 값이나 결과를 화면/콘솔에 출력해서 눈으로 확인합니다."
        line.startsWith("SELECT", ignoreCase = true) -> "데이터베이스에서 필요한 데이터를 읽어오는 SQL 명령입니다. 어떤 열과 표를 읽는지 봅니다."
        line.startsWith("INSERT", ignoreCase = true) -> "데이터베이스에 새 행을 추가하는 SQL 명령입니다."
        line.startsWith("UPDATE", ignoreCase = true) -> "이미 있는 데이터를 수정하는 SQL 명령입니다. WHERE 조건을 특히 주의합니다."
        line.startsWith("DELETE", ignoreCase = true) -> "데이터를 삭제하는 SQL 명령입니다. 범위를 제한하는 조건이 중요합니다."
        line.startsWith("interface ") || line.startsWith("type ") -> "TypeScript에서 데이터 모양이나 타입 규칙을 설명하는 선언입니다."
        line.startsWith("const ") || line.startsWith("let ") -> "값에 이름을 붙여 저장합니다. 이후 코드가 이 이름으로 값을 사용합니다."
        line.contains("fetch(") -> "웹/API 주소에 요청을 보내는 지점입니다. 주소, 메서드, 응답 처리를 함께 확인합니다."
        line.startsWith("<") -> "HTML 구조를 만드는 태그입니다. 화면에서 어떤 요소가 되는지 연결해서 봅니다."
        line.contains("=") && !line.contains("==") && language != "SQL" -> "오른쪽의 값을 계산하거나 가져와 왼쪽 이름에 저장하는 대입입니다."
        else -> "이 줄이 앞 단계의 값을 어떻게 사용하고 다음 단계로 무엇을 넘기는지 확인합니다."
    }

    private fun fallbackDefinition(keyword: String, lesson: LessonContent): String =
        "$keyword 는 '${lesson.title}'을 이해할 때 구분해야 하는 핵심 용어입니다. 이 레슨에서는 ${lesson.expectedOutcome}라는 목표 안에서 어떤 역할을 하는지 중심으로 이해하면 됩니다. 처음부터 전문 정의를 외우기보다 예제에서 위치와 역할을 먼저 찾으세요."

    private fun memoryHook(keyword: String, lesson: LessonContent): String =
        "'$keyword'라는 말을 보면 '${lesson.moduleTitle}에서 ${lesson.title}과 연결되는 역할'을 먼저 떠올리세요."

    private fun genericGuide(lesson: LessonContent) = ModuleGuide(
        bigPicture = "새 기술을 배울 때는 이름부터 외우지 않습니다. 사용자가 무언가를 입력하고, 프로그램이 규칙에 따라 처리하고, 결과를 보여주는 전체 흐름에서 '${lesson.title}'의 위치를 먼저 잡습니다. 이렇게 보면 처음 보는 용어도 기존 구조에 끼워 넣어 이해할 수 있습니다.",
        analogy = "처음 가는 건물에서도 입구, 안내데스크, 작업 공간, 창고, 출구를 구분하면 길을 찾을 수 있습니다. 프로그램도 화면, 처리 로직, 데이터, 외부 연결을 역할별로 나누면 훨씬 이해하기 쉽습니다.",
        flow = "사용자 행동 → 입력값 → 처리 규칙 → 데이터/외부 연결 → 결과 → 화면 확인 순서로 따라가세요.",
        compare = "비슷한 개념을 구분할 때는 이름보다 ‘어디에서 실행되는가’, ‘무엇을 입력받는가’, ‘무엇을 결과로 내는가’를 비교합니다.",
        realWorld = "실제 앱에서는 여러 기술이 한 화면에 섞여 보입니다. 하나씩 떼어 역할을 확인하면 AI가 만든 코드도 어디를 수정해야 하는지 판단하기 쉬워집니다.",
        mistakes = "초보자는 용어를 외운 뒤 실제 예제에서 찾지 못하거나, 한 번 성공한 코드를 이해했다고 착각하기 쉽습니다. 반드시 예제의 값을 바꿔보고 결과가 왜 달라지는지 확인하세요.",
        glossary = emptyMap()
    )

    private val moduleGuides = mapOf(
        "BEGINNER:1" to ModuleGuide(
            bigPicture = "컴퓨터와 스마트폰을 쓰는 것과 프로그램의 구조를 이해하는 것은 다릅니다. 여기서는 파일이 어디에 있고, 앱이 어디에서 실행되고, 인터넷을 통해 서버와 어떻게 통신하며, 데이터가 어디에 저장되는지를 하나의 지도처럼 연결합니다. 이 지도를 알면 ‘화면 문제인지, 인터넷 문제인지, 서버 문제인지’를 분리할 수 있습니다.",
            analogy = "택배를 생각하면 쉽습니다. 파일은 물건, 폴더는 상자, 경로는 주소입니다. 앱은 주문서를 작성하는 가게, 인터넷은 도로, 서버는 주문을 처리하는 창고, 데이터베이스는 물건이 정리된 보관소와 비슷합니다. 비유의 목적은 역할을 나누는 것입니다.",
            flow = "파일/설치 → 운영체제 권한 → 앱 실행 → 사용자 입력 → 네트워크 요청 → 서버/API → 데이터베이스 → 응답 → 화면 표시 순서로 봅니다.",
            compare = "앱과 웹은 보이는 화면이 비슷해도 실행 위치가 다를 수 있습니다. API는 데이터베이스가 아니고, JSON도 데이터베이스가 아닙니다. API는 연결 규칙, DB는 저장소, JSON은 데이터를 표현하는 형식이라고 구분하세요.",
            realWorld = "앱이 열리는데 최신 데이터만 안 나오면 화면 전체를 다시 만들기보다 네트워크 요청과 서버 응답부터 확인합니다. 설치가 안 되면 API가 아니라 APK, 권한, Android 버전 같은 앞단을 먼저 봅니다.",
            mistakes = "ZIP과 APK를 같은 설치 파일로 생각하기, 브라우저에서 되는 것을 네이티브 앱에서도 당연히 된다고 생각하기, 화면이 비면 곧바로 DB 문제라고 단정하기가 대표적인 실수입니다.",
            glossary = mapOf("파일" to "이름을 가진 데이터 단위입니다.", "폴더" to "파일을 묶어 정리하는 공간입니다.", "경로" to "파일이나 폴더의 위치를 단계별 이름으로 표현한 것입니다.", "API" to "앱과 서버가 정해진 방식으로 기능이나 데이터를 주고받는 접점입니다.", "JSON" to "데이터를 key와 value 구조로 표현해 전달하는 형식입니다.", "DB" to "데이터를 지속적으로 저장·검색·수정하는 시스템입니다.", "서버" to "네트워크를 통해 요청을 받아 처리하고 응답하는 컴퓨터나 프로그램입니다." )
        ),
        "BEGINNER:2" to ModuleGuide(
            bigPicture = "생성형 AI는 ‘정답 데이터베이스’가 아니라 입력된 문맥을 바탕으로 다음에 올 가능성이 높은 내용을 만들어내는 모델입니다. 그래서 매우 자연스럽게 말하면서도 틀릴 수 있습니다. 모델, 검색, 외부 도구, 권한을 분리해서 이해하면 AI의 강점과 한계를 훨씬 정확히 판단할 수 있습니다.",
            analogy = "매우 많은 글을 읽은 유능한 조수와 비슷하지만, 기억이 완벽한 사서와 같지는 않습니다. 조수는 그럴듯한 답을 만들어낼 수 있고, 최신 문서가 필요하면 별도로 검색하거나 도구를 사용해야 합니다.",
            flow = "사용자 입력 → 토큰화 → 현재 컨텍스트 반영 → 모델 추론 → 출력 생성 → 필요 시 검색/도구 호출 → 결과 검증 순서로 봅니다.",
            compare = "학습(training)은 모델 자체를 바꾸는 과정, 추론(inference)은 이미 학습된 모델로 답을 만드는 과정, 검색은 외부의 현재 자료를 가져오는 별도 과정입니다. 자연스러운 문장과 사실 정확도도 별개입니다.",
            realWorld = "AI가 존재하지 않는 함수나 오래된 API 사용법을 제시할 수 있습니다. 그래서 중요한 작업은 공식 문서, 실제 실행 결과, 원문 자료로 다시 확인해야 합니다.",
            mistakes = "AI가 자신 있게 말하면 사실이라고 생각하기, 검색과 모델 기억을 같은 것으로 보기, 긴 답변이 더 정확하다고 생각하기가 흔한 오류입니다.",
            glossary = mapOf("토큰" to "모델이 텍스트를 처리할 때 사용하는 작은 단위입니다.", "컨텍스트" to "현재 답변을 만들 때 모델이 참고하는 입력 범위입니다.", "학습" to "데이터를 이용해 모델 파라미터를 바꾸는 과정입니다.", "추론" to "학습된 모델이 입력을 받아 출력을 만드는 과정입니다.", "검색" to "외부 자료에서 현재 정보를 가져오는 동작입니다.", "환각" to "AI가 사실이 아닌 내용을 그럴듯하게 만들어내는 현상입니다." )
        ),
        "BEGINNER:3" to ModuleGuide(
            bigPicture = "AI에게 일을 잘 맡기는 능력은 질문을 길게 쓰는 기술이 아닙니다. 목표, 현재 자료, 지켜야 할 제약, 원하는 출력형식, 성공조건을 분명하게 전달하고 큰 작업을 검증 가능한 작은 단계로 나누는 능력입니다.",
            analogy = "직원에게 ‘잘 해줘’라고 말하는 것과 ‘이 자료만 사용해서, 표 형식으로, 5개 항목을, 오늘 기준으로 정리하고 출처를 붙여줘’라고 말하는 차이와 같습니다.",
            flow = "목표 정의 → 입력자료 제공 → 제약/비목표 지정 → 작업 분해 → 출력 형식 지정 → 성공조건 정의 → 결과 검토 → 필요한 부분만 재지시합니다.",
            compare = "질문은 정보를 묻는 데 가깝고 작업 지시는 결과물을 만들게 하는 데 가깝습니다. 제약조건은 하면 안 되는 것, 성공조건은 끝났다고 판단할 기준입니다.",
            realWorld = "앱 수정, 자료조사, 문서 작성, 데이터 정리 모두 같은 구조를 사용할 수 있습니다. 특히 기존 기능을 보존해야 하는 코딩 작업에서는 수정범위와 테스트조건이 중요합니다.",
            mistakes = "목표 없이 ‘좋게 만들어줘’라고 하기, 입력자료를 빠뜨리기, 결과 확인 기준 없이 완료라고 믿기, 한 번에 너무 큰 작업을 맡기기가 대표적인 실수입니다.",
            glossary = mapOf("목표" to "이번 작업에서 최종적으로 얻고 싶은 결과입니다.", "제약조건" to "작업 중 반드시 지켜야 할 제한입니다.", "성공조건" to "작업이 끝났다고 판정할 수 있는 검사 기준입니다.", "출력형식" to "결과를 어떤 구조와 모양으로 받을지 정한 규칙입니다.", "작업 분해" to "큰 일을 검증 가능한 작은 단계로 나누는 것입니다." )
        ),
        "BEGINNER:4" to ModuleGuide(
            bigPicture = "검색은 많은 결과를 찾는 일이 아니라 신뢰할 수 있는 근거를 골라 현재 질문에 맞게 판단하는 과정입니다. 출처의 종류, 날짜, 원문 여부, 서로 충돌하는 주장, 사실과 해석의 차이를 분리하는 습관이 중요합니다.",
            analogy = "소문을 듣고 결정하는 대신 계약서 원본, 공식 발표, 여러 사람의 증언을 비교하는 것과 같습니다. 정보의 양보다 증거의 질이 중요합니다.",
            flow = "질문 정의 → 1차 출처 찾기 → 날짜/버전 확인 → 보조 출처 비교 → 주장과 근거 분리 → 충돌 확인 → 결론과 불확실성 기록 순서입니다.",
            compare = "공식 문서와 블로그, 뉴스와 커뮤니티, 원문과 요약은 역할이 다릅니다. 요약은 빠르게 이해하기 좋지만 중요한 결정은 가능한 한 원문으로 되돌아가야 합니다.",
            realWorld = "API 사용법, 앱 정책, 투자 뉴스, 제품 사양처럼 시간이 지나면 바뀌는 정보는 특히 기준일과 공식 출처가 중요합니다.",
            mistakes = "검색결과 첫 줄만 믿기, 날짜를 확인하지 않기, 같은 기사를 복제한 여러 사이트를 서로 독립된 근거로 세기, AI 요약만 보고 원문을 보지 않기가 흔한 실수입니다.",
            glossary = mapOf("원문" to "정보가 처음 공개된 직접 자료입니다.", "공식 출처" to "책임 주체가 직접 제공하는 문서나 발표입니다.", "최신성" to "정보가 현재 시점에도 유효한지에 관한 성질입니다.", "근거" to "주장을 뒷받침하는 확인 가능한 자료입니다." )
        ),
        "BEGINNER:5" to ModuleGuide(
            bigPicture = "프로그램은 복잡해 보여도 결국 값을 저장하고, 조건을 판단하고, 반복하고, 작업을 묶고, 입력을 결과로 바꾸는 규칙의 조합입니다. 문법을 외우기 전에 값이 어떻게 움직이고 상태가 어떻게 변하는지 추적하는 습관을 만듭니다.",
            analogy = "요리 레시피와 비슷합니다. 재료는 값, 재료 이름표는 변수, ‘물이 끓으면’은 조건, ‘10번 저어라’는 반복, ‘소스 만들기’라는 묶음은 함수와 비슷합니다.",
            flow = "입력값 → 변수에 저장 → 조건 확인 → 필요한 반복 → 함수 호출 → 상태 변화 → 출력 결과 순서로 코드의 흐름을 따라갑니다.",
            compare = "변수는 값을 담는 이름, 조건문은 선택, 반복문은 반복, 함수는 작업 묶음입니다. 이 네 가지 역할을 섞지 않는 것이 첫 번째 목표입니다.",
            realWorld = "로그인 여부에 따른 화면 변경, 여러 종목의 점수 계산, 버튼 클릭 후 데이터 처리 등 대부분의 앱 기능은 이 기본 요소들의 조합입니다.",
            mistakes = "코드를 위에서 아래로 무조건 한 번만 실행된다고 생각하기, 변수의 값이 중간에 바뀔 수 있다는 점을 놓치기, 조건과 반복을 혼동하기가 흔합니다.",
            glossary = mapOf("변수" to "값에 이름을 붙여 저장하고 다시 사용할 수 있게 한 것입니다.", "값" to "숫자, 문자, 참/거짓처럼 프로그램이 다루는 실제 데이터입니다.", "조건문" to "조건에 따라 다른 코드를 실행하는 구조입니다.", "반복문" to "같은 작업을 여러 번 수행하는 구조입니다.", "함수" to "하나의 목적을 가진 작업을 이름 붙여 묶은 것입니다.", "상태" to "프로그램이 현재 기억하고 있는 값이나 상황입니다." )
        ),
        "BEGINNER:6" to ModuleGuide(
            bigPicture = "Python은 사람이 읽기 쉬운 문법으로 데이터를 다루고 자동화를 만들기 좋은 언어입니다. 초급에서는 문법 전체를 외우지 않고 숫자·문자열·리스트·딕셔너리·조건·반복·함수만으로 작은 문제를 직접 해결하는 데 집중합니다.",
            analogy = "계산기와 메모장과 반복 작업 도구를 하나로 합친 것처럼 생각하면 됩니다. 숫자를 계산하고, 문장을 바꾸고, 여러 값을 묶고, 같은 작업을 반복하게 할 수 있습니다.",
            flow = "값 만들기 → 변수에 저장 → 자료구조에 묶기 → 조건/반복으로 처리 → 함수로 정리 → print로 결과 확인 순서로 익힙니다.",
            compare = "문자열은 글자 데이터, 리스트는 순서 있는 여러 값, 딕셔너리는 이름표(key)와 값(value)의 쌍입니다. if는 선택, for/while은 반복, def는 함수를 만드는 문법입니다.",
            realWorld = "CSV 정리, API 응답 처리, 점수 계산, 파일 이름 변경, 반복 보고서 생성 같은 자동화 작업에 Python이 자주 쓰입니다.",
            mistakes = "따옴표 누락, 들여쓰기 오류, 숫자와 문자열을 섞어 계산하기, 리스트 인덱스 범위를 넘기기, >=를 =>로 쓰기 등이 초보자에게 매우 흔합니다.",
            glossary = mapOf("문자열" to "글자를 순서대로 묶은 데이터입니다.", "리스트" to "여러 값을 순서대로 담는 자료구조입니다.", "딕셔너리" to "key와 value 쌍으로 값을 저장하는 자료구조입니다.", "인덱스" to "리스트 안에서 값의 위치를 나타내는 번호입니다.", "함수" to "반복해서 사용할 작업을 이름 붙여 묶은 것입니다.", "오류" to "코드가 규칙에 맞지 않거나 실행 중 문제가 생긴 상태입니다." )
        ),
        "BEGINNER:7" to ModuleGuide(
            bigPicture = "데이터는 화면에 보이는 값 그 자체가 아니라 구조를 가지고 저장·전달됩니다. 표, CSV, JSON, 데이터베이스가 각각 어떤 모양으로 데이터를 표현하고 어떤 상황에서 쓰이는지 구분하는 것이 목표입니다.",
            analogy = "주소록을 생각해보면 한 사람은 한 행, 이름·전화번호는 열입니다. JSON은 한 사람 정보를 꼬리표와 함께 포장한 묶음이고, 데이터베이스는 많은 사람 정보를 검색하고 수정할 수 있게 관리하는 시스템과 비슷합니다.",
            flow = "원본 데이터 → 구조 확인 → 타입/NULL 확인 → 필요한 값 선택 → 변환/검증 → 저장 또는 전달 → 화면 표시 순서로 봅니다.",
            compare = "CSV는 단순한 표 형태 파일, JSON은 중첩 가능한 key/value 표현, 데이터베이스는 지속적 저장과 검색을 담당합니다. Excel 화면과 데이터베이스는 겉보기 표가 비슷해도 역할이 다릅니다.",
            realWorld = "앱에서 사용자 목록, 주식 데이터, 일정, 설정값을 저장하거나 API로 주고받을 때 데이터 구조를 읽을 수 있어야 오류 원인을 찾을 수 있습니다.",
            mistakes = "행과 열을 바꾸어 이해하기, 숫자가 문자열로 들어온 것을 놓치기, NULL을 0과 같다고 생각하기, JSON 괄호·콤마 구조를 무시하기가 대표적입니다.",
            glossary = mapOf("행" to "표에서 하나의 기록을 가로 방향으로 묶은 단위입니다.", "열" to "같은 종류의 값을 세로 방향으로 모은 항목입니다.", "CSV" to "쉼표 등 구분자로 표 형태 데이터를 저장하는 텍스트 형식입니다.", "JSON" to "key/value와 배열을 이용해 구조화된 데이터를 표현하는 형식입니다.", "NULL" to "값이 없거나 정해지지 않았음을 나타내는 상태입니다.", "CRUD" to "생성, 읽기, 수정, 삭제의 기본 데이터 작업입니다." )
        ),
        "BEGINNER:8" to ModuleGuide(
            bigPicture = "보안은 특별한 해킹 기술보다 계정, 권한, 링크, 개인정보, 백업을 안전하게 다루는 습관에서 시작합니다. 목표는 모든 공격을 막는 전문가가 아니라 위험 신호를 알아보고 멈출 수 있는 사용자가 되는 것입니다.",
            analogy = "집 보안과 비슷합니다. 비밀번호는 열쇠, MFA는 두 번째 잠금장치, 앱 권한은 방마다 출입권을 주는 것, 백업은 중요한 물건의 복사본을 다른 곳에 보관하는 것과 비슷합니다.",
            flow = "요청/링크 확인 → 발신자·도메인 확인 → 요구하는 권한/정보 확인 → 최소 권한 부여 → 중요한 변경은 추가 인증 → 기록/백업 → 이상 시 차단과 복구 순서입니다.",
            compare = "비밀번호와 Passkey, 인증과 권한, 백업과 동기화는 서로 다릅니다. 로그인했다고 모든 권한이 필요한 것도 아니고, 클라우드 동기화가 항상 안전한 백업을 뜻하지도 않습니다.",
            realWorld = "택배 문자, 금융 사칭, 앱 권한 요청, API Key 공유, 기기 분실 같은 상황에서 즉시 판단해야 합니다. AI에게 민감정보를 붙여 넣는 것도 별도의 보안 판단이 필요합니다.",
            mistakes = "같은 비밀번호 재사용, 문자 링크 바로 클릭, 필요 이상 권한 허용, API Key를 코드에 그대로 넣기, 백업이 있다고 확인하지 않고 믿기가 흔한 실수입니다.",
            glossary = mapOf("MFA" to "비밀번호 외에 추가 인증 수단을 요구하는 다중 요소 인증입니다.", "Passkey" to "기기와 공개키 암호 방식을 이용해 비밀번호 대신 로그인하는 방식입니다.", "권한" to "앱이나 계정이 특정 자원에 접근할 수 있는 허용 범위입니다.", "피싱" to "가짜 메시지나 사이트로 사용자의 정보나 행동을 유도하는 공격입니다.", "API Key" to "API 사용 권한을 식별하는 비밀 값으로 외부에 노출하면 안 됩니다." )
        ),
        "INTERMEDIATE:1" to ModuleGuide(
            bigPicture = "웹 화면은 보통 HTML이 구조를 만들고 CSS가 배치와 모양을 정하며 JavaScript가 사용자 행동과 데이터 변화를 처리합니다. 세 기술이 한 파일에 섞일 수 있어도 역할을 나눠서 읽으면 AI가 만든 웹 코드를 훨씬 쉽게 검토할 수 있습니다.",
            analogy = "건물에 비유하면 HTML은 벽과 방의 구조, CSS는 색·가구·배치, JavaScript는 문이 열리고 버튼이 작동하는 동작 규칙입니다.",
            flow = "HTML 요소 생성 → CSS로 배치/표현 → 사용자 이벤트 발생 → JavaScript가 상태/데이터 처리 → DOM 변경 → 화면 갱신 순서로 봅니다.",
            compare = "HTML은 프로그래밍 로직보다 문서 구조에 가깝고, CSS는 스타일 규칙, JavaScript는 실행되는 코드입니다. DOM은 브라우저가 HTML을 객체 구조로 표현한 것입니다.",
            realWorld = "버튼이 안 눌리면 CSS보다 이벤트 연결을, 요소가 사라지면 HTML/DOM을, 모양만 이상하면 CSS를 먼저 보는 식으로 오류 범위를 좁힐 수 있습니다.",
            mistakes = "HTML 태그 닫기 누락, CSS 선택자 혼동, 이벤트 함수를 호출하지 않고 참조만 하거나 반대로 즉시 실행하기, DOM 요소가 생기기 전에 접근하기가 흔합니다.",
            glossary = mapOf("HTML" to "웹 문서의 구조와 의미를 표현하는 마크업입니다.", "CSS" to "웹 요소의 모양과 배치를 정하는 스타일 규칙입니다.", "JavaScript" to "브라우저에서 동작과 데이터 처리를 구현하는 언어입니다.", "DOM" to "브라우저가 HTML 문서를 객체 트리로 표현한 구조입니다.", "이벤트" to "클릭, 입력, 로딩처럼 프로그램이 반응할 수 있는 사건입니다." )
        ),
        "INTERMEDIATE:2" to ModuleGuide(
            bigPicture = "TypeScript는 JavaScript에 타입 정보를 더해 실수를 실행 전에 발견하기 쉽게 만든 언어입니다. 목표는 모든 타입 문법을 외우는 것이 아니라 AI가 만든 프론트엔드 코드에서 데이터 모양, 함수 입력/출력, null 가능성, 비동기 흐름을 읽는 것입니다.",
            analogy = "택배 상자에 ‘깨지기 쉬움’, ‘냉장’, ‘무게 5kg’ 같은 라벨을 붙이는 것과 비슷합니다. 타입은 값의 모양에 대한 약속이라 잘못된 값을 넣을 때 일찍 경고합니다.",
            flow = "데이터 타입 선언 → 변수/객체 생성 → 함수에 전달 → 타입 검사 → 비동기 작업 await → 결과를 화면 상태에 반영하는 흐름으로 봅니다.",
            compare = "JavaScript는 실행 중에 타입 문제를 발견할 수 있지만 TypeScript는 개발 단계에서 많은 오류를 미리 잡습니다. interface와 type은 비슷하지만 용도와 확장 방식에 차이가 있습니다.",
            realWorld = "API 응답 객체의 필드 이름이 바뀌었거나 null이 들어오는 경우 TypeScript 타입 검사가 문제 위치를 좁히는 데 도움을 줍니다.",
            mistakes = "any를 남용해 타입 검사를 무력화하기, null/undefined를 무시하기, Promise와 실제 결과값을 혼동하기, async 함수에서 await를 빠뜨리기가 흔합니다.",
            glossary = mapOf("타입" to "값이 어떤 종류와 모양인지 나타내는 규칙입니다.", "interface" to "객체가 가져야 할 필드와 타입의 모양을 설명하는 선언입니다.", "null" to "의도적으로 값이 없음을 나타내는 값입니다.", "undefined" to "값이 아직 정의되지 않았음을 나타내는 JavaScript 값입니다.", "async" to "비동기 작업을 다루는 함수를 표시하는 키워드입니다.", "await" to "Promise 결과가 준비될 때까지 해당 async 흐름에서 기다리는 키워드입니다." )
        ),
        "INTERMEDIATE:3" to ModuleGuide(
            bigPicture = "API와 HTTP는 앱이 외부 시스템과 통신하는 규칙입니다. 주소(URL), 메서드(GET/POST 등), 헤더, 쿼리, 본문, 상태코드, 인증을 분리해서 읽을 수 있어야 요청이 왜 실패했는지 찾을 수 있습니다.",
            analogy = "식당 주문과 비슷합니다. URL은 식당 주소, 메서드는 주문 종류, 헤더는 예약/신분 정보, body는 실제 주문 내용, 상태코드는 주문이 접수됐는지 알려주는 번호와 비슷합니다.",
            flow = "URL 구성 → 메서드 선택 → 인증/헤더 설정 → Query/Body 작성 → 요청 전송 → 상태코드 확인 → 응답 JSON 파싱 → 실패 시 재시도/오류 처리 순서입니다.",
            compare = "GET은 주로 조회, POST는 새 작업/데이터 전송에 쓰이지만 실제 의미는 API 문서를 따라야 합니다. 401은 인증 문제, 403은 권한 문제, 404는 대상을 찾지 못함, 429는 요청 제한 초과를 뜻합니다.",
            realWorld = "주식 시세, 날씨, 로그인, AI 모델 호출 등 많은 앱 기능이 API를 사용합니다. 화면 오류처럼 보여도 실제 원인은 401, 잘못된 URL, Rate Limit일 수 있습니다.",
            mistakes = "API Key를 코드에 노출하기, GET/POST를 감으로 바꾸기, 상태코드를 무시하고 응답 본문만 보기, 429에서 무한 재시도하기가 흔합니다.",
            glossary = mapOf("HTTP" to "웹에서 요청과 응답을 주고받기 위한 대표적인 통신 규약입니다.", "GET" to "주로 데이터를 조회할 때 쓰는 HTTP 메서드입니다.", "POST" to "주로 데이터를 보내거나 새 작업을 요청할 때 쓰는 HTTP 메서드입니다.", "Header" to "요청/응답의 부가 정보를 담는 영역입니다.", "Status Code" to "요청 처리 결과를 숫자로 나타낸 HTTP 상태값입니다.", "Rate Limit" to "일정 시간에 허용되는 요청 횟수 제한입니다." )
        ),
        "INTERMEDIATE:4" to ModuleGuide(
            bigPicture = "SQL은 관계형 데이터베이스에서 데이터를 읽고 추가하고 수정하고 삭제하는 언어입니다. 중급에서는 복잡한 최적화보다 SELECT, WHERE, INSERT, UPDATE, DELETE와 안전한 조건 사용을 정확히 이해하는 것이 중요합니다.",
            analogy = "큰 엑셀 표에서 ‘이 조건에 맞는 행만 골라 보여줘’, ‘새 행 추가해줘’, ‘이 사람의 전화번호만 바꿔줘’라고 명령하는 언어라고 생각할 수 있습니다.",
            flow = "어떤 테이블인지 확인 → 필요한 열 선택 → WHERE로 범위 제한 → 정렬/개수 제한 → 결과 확인 → 수정 쿼리는 영향 범위를 다시 확인 → 실행 순서입니다.",
            compare = "SELECT는 읽기, INSERT는 추가, UPDATE는 수정, DELETE는 삭제입니다. WHERE가 없는 UPDATE/DELETE는 많은 행을 바꿀 수 있어 특히 위험합니다. JOIN은 여러 테이블의 관련 행을 연결합니다.",
            realWorld = "사용자, 일정, 주문, CRM 활동처럼 구조화된 데이터를 저장하는 앱에서 SQL과 DB 개념을 알면 데이터가 왜 중복되거나 누락됐는지 추적할 수 있습니다.",
            mistakes = "WHERE 없는 UPDATE/DELETE, 문자열 따옴표 누락, NULL을 = NULL로 비교하기, 사용자 입력을 문자열로 그대로 이어붙여 SQL Injection 위험을 만드는 것이 대표적입니다.",
            glossary = mapOf("Table" to "같은 구조의 기록을 행과 열로 저장하는 데이터베이스 단위입니다.", "SELECT" to "데이터를 조회하는 SQL 명령입니다.", "WHERE" to "조건에 맞는 행만 선택하도록 범위를 제한하는 절입니다.", "JOIN" to "관련 키를 기준으로 여러 테이블의 행을 연결하는 연산입니다.", "Transaction" to "여러 DB 작업을 하나의 논리적 단위로 묶어 모두 성공하거나 되돌릴 수 있게 하는 기능입니다." )
        ),
        "INTERMEDIATE:5" to ModuleGuide(
            bigPicture = "Python 자동화는 파일, API, 데이터 변환, 반복 작업을 한 흐름으로 연결해 사람이 매번 손으로 하던 일을 재현 가능하게 만드는 것입니다. 핵심은 코드 길이가 아니라 입력, 처리, 출력, 실패 처리, 로그가 분명한 파이프라인을 만드는 것입니다.",
            analogy = "공장 컨베이어벨트처럼 원료가 들어오고 여러 공정을 지나 결과물이 나옵니다. 중간 공정 하나가 실패하면 어디서 멈췄는지 기록이 있어야 다시 시작할 수 있습니다.",
            flow = "입력 파일/API → 형식 검증 → 데이터 변환 → 필터/집계 → 결과 저장 → 로그 기록 → 실패 시 재시도 또는 중단 → 재실행 시 중복 방지 순서입니다.",
            compare = "단순 스크립트는 한 번 실행하고 끝날 수 있지만 운영 자동화는 실패, 재실행, 중복, 로그까지 생각해야 합니다. 예외(exception)는 예상 가능한 실패를 처리하는 구조입니다.",
            realWorld = "매일 CSV 정리, API 데이터 수집, 보고서 생성, 파일 이름 변경, 데이터베이스 업데이트 등에 Python 자동화가 자주 쓰입니다.",
            mistakes = "파일 경로를 고정해 다른 기기에서 깨지기, 오류를 전부 무시하기, 재실행 때 중복 데이터 만들기, 로그를 남기지 않아 실패 위치를 못 찾기가 흔합니다.",
            glossary = mapOf("파이프라인" to "여러 처리 단계를 순서대로 연결한 작업 흐름입니다.", "예외" to "실행 중 발생할 수 있는 오류 상황을 코드에서 다루는 방식입니다.", "로그" to "프로그램이 언제 무엇을 했고 어디서 실패했는지 남긴 기록입니다.", "멱등성" to "같은 작업을 여러 번 실행해도 최종 결과가 불필요하게 달라지지 않는 성질입니다.", "재시도" to "일시적 실패 후 같은 작업을 다시 시도하는 전략입니다." )
        ),
        "INTERMEDIATE:6" to ModuleGuide(
            bigPicture = "Git은 파일을 저장하는 도구라기보다 변경 이력을 기록하고 비교하고 되돌리는 버전관리 시스템입니다. AI와 코딩할수록 ‘무엇이 바뀌었는지’와 ‘정상 상태로 어떻게 돌아갈지’를 아는 것이 중요합니다.",
            analogy = "문서의 자동 저장이 아니라 의미 있는 시점마다 스냅샷을 찍고, 서로 다른 작업용 복사 경로를 만들고, 차이를 비교하는 기록 시스템이라고 보면 됩니다.",
            flow = "변경 확인 → diff 검토 → 필요한 파일 stage → commit → branch에서 실험 → 테스트 → 문제가 있으면 restore/revert → 검증 후 병합 순서로 봅니다.",
            compare = "Repository는 프로젝트와 이력을 담는 공간, commit은 한 시점의 기록, branch는 독립 작업 흐름, diff는 변경점 비교입니다. restore와 revert는 되돌리는 방식이 다릅니다.",
            realWorld = "AI가 여러 파일을 한꺼번에 고쳤는데 앱이 깨졌을 때 Git 이력이 있으면 어떤 변경이 원인인지 좁히고 정상 버전으로 복구할 수 있습니다.",
            mistakes = "테스트하지 않은 큰 변경을 한 commit에 넣기, diff를 안 보고 병합하기, force push를 이해 없이 사용하기, branch와 폴더 복사를 같은 것으로 생각하기가 흔합니다.",
            glossary = mapOf("Repository" to "프로젝트 파일과 변경 이력을 관리하는 Git 저장소입니다.", "Commit" to "특정 시점의 변경 내용을 메시지와 함께 기록한 단위입니다.", "Diff" to "두 상태 사이에서 무엇이 추가·삭제·수정됐는지 보여주는 비교입니다.", "Branch" to "기존 이력에서 갈라져 독립적으로 작업할 수 있는 흐름입니다.", "Revert" to "기존 commit의 효과를 되돌리는 새 commit을 만드는 방식입니다." )
        ),
        "INTERMEDIATE:7" to ModuleGuide(
            bigPicture = "디버깅은 감으로 코드를 고치는 일이 아니라 문제를 재현하고, 정상 구간과 실패 구간의 경계를 찾고, 가장 작은 가설을 시험하는 과정입니다. 테스트는 수정이 실제로 문제를 고쳤고 다른 기능을 깨지 않았는지 확인하는 증거입니다.",
            analogy = "집 전기가 안 들어올 때 집 전체 배선을 뜯는 대신 차단기, 특정 방, 콘센트, 기기 순서로 범위를 좁히는 것과 같습니다.",
            flow = "문제 재현 → 에러/증상 기록 → 계층 분리 → 가장 가능성 높은 가설 → 최소 수정 → 같은 조건 재시험 → 경계값/회귀시험 → 증거 저장 순서입니다.",
            compare = "증상은 사용자가 보는 문제, 원인은 실제 결함, 가설은 아직 검증되지 않은 추정입니다. Unit test는 작은 단위, integration test는 여러 단위 연결, regression test는 기존 기능이 다시 깨지지 않았는지 확인합니다.",
            realWorld = "화면은 열리는데 데이터가 없거나, 특정 입력에서만 앱이 꺼지거나, 업데이트 후 예전 기능이 깨지는 상황에서 체계적 디버깅이 필요합니다.",
            mistakes = "재현 없이 바로 수정하기, 여러 파일을 동시에 바꾸기, 에러 메시지의 마지막 한 줄만 보기, 한 번 성공했다고 회귀시험을 생략하기가 흔합니다.",
            glossary = mapOf("재현" to "같은 조건에서 같은 문제가 다시 발생하도록 만드는 것입니다.", "가설" to "원인에 대한 검증 전 추정입니다.", "경계값" to "정상과 오류가 갈릴 가능성이 높은 최소·최대·빈 값 같은 입력입니다.", "회귀시험" to "수정 후 기존 기능이 다시 깨지지 않았는지 확인하는 테스트입니다." )
        ),
        "INTERMEDIATE:8" to ModuleGuide(
            bigPicture = "자연어 코딩은 AI에게 코드를 대신 써달라고 하는 것이 아니라 요구사항을 테스트 가능한 계약으로 바꾸고, AI의 변경 범위를 통제하고, 결과를 diff와 테스트로 검증하는 작업 방식입니다.",
            analogy = "공사 업체에게 ‘예쁘게 고쳐주세요’가 아니라 ‘이 벽만 철거하고 배관은 보존하며, 완료 후 누수 테스트를 통과해야 한다’고 계약하는 것과 같습니다.",
            flow = "목표/비목표 → 현재 상태 → 수정범위 → 보존할 기능 → 성공조건 → 테스트조건 → 작은 변경 → diff 검토 → 실행 검증 → HANDOFF 기록 순서입니다.",
            compare = "Prompt는 한 번의 지시일 수 있지만 요구사항은 구현이 지켜야 할 조건 집합입니다. Acceptance Test는 ‘완료’를 판단하는 사용자 관점의 기준입니다.",
            realWorld = "앱 기능 추가, 버그 수정, 데이터 연결, UI 변경을 AI에게 맡길 때 기존 기능을 깨뜨리지 않고 반복 가능한 방식으로 진행하는 데 필요합니다.",
            mistakes = "‘전부 알아서’라고만 지시하기, 기존 기능 보존 조건을 안 쓰기, 테스트 없이 완료를 믿기, 여러 목표를 한 번에 바꾸기가 흔합니다.",
            glossary = mapOf("요구사항" to "결과물이 반드시 만족해야 하는 기능과 제약 조건입니다.", "Acceptance Test" to "사용자 요구가 충족됐는지 판정하는 완료 기준 테스트입니다.", "수정범위" to "이번 작업에서 바꿔도 되는 파일·기능·영역의 범위입니다.", "HANDOFF" to "다음 작업자가 현재 상태와 남은 일을 이어받을 수 있게 남기는 기록입니다." )
        ),
        "INTERMEDIATE:9" to ModuleGuide(
            bigPicture = "AI 앱은 단순 채팅창이 아니라 시스템 지시, 사용자 입력, 컨텍스트, 모델 호출, 구조화된 출력, 도구 실행, 실패 처리, 비용과 개인정보 관리가 연결된 시스템입니다. 각 층을 분리해야 안전하게 수정할 수 있습니다.",
            analogy = "콜센터를 생각하면 시스템 지시는 상담원 규정, 사용자 메시지는 고객 요청, 컨텍스트는 고객 기록, Tool은 외부 업무 시스템, Structured Output은 정해진 양식의 업무 결과와 비슷합니다.",
            flow = "System 규칙 → User 요청 → 필요한 Context → 모델 호출 → Structured Output 검사 → 필요 시 Tool 실행 → 결과 검증 → 사용자 표시 → 실패 시 fallback 순서입니다.",
            compare = "Prompt와 Context는 다릅니다. Prompt는 지시와 입력, Context는 답변에 참고시키는 주변 정보입니다. Tool Calling은 모델이 실제 외부 기능을 직접 실행한다기보다 정해진 도구 호출을 요청하고 앱이 실행하는 구조입니다.",
            realWorld = "AI 요약, 데이터 분석, 문서 검색, 자동 분류, 코딩 도우미 같은 기능을 만들 때 모델 답변만 믿지 않고 스키마 검증과 실패 처리가 필요합니다.",
            mistakes = "API Key 노출, 모델 출력 문자열을 그대로 실행하기, JSON 스키마 검증 생략, 민감정보를 무조건 컨텍스트에 넣기, 실패 시 가짜 성공을 보여주기가 위험합니다.",
            glossary = mapOf("System Instruction" to "AI가 따라야 할 상위 행동 규칙입니다.", "Prompt" to "모델에 전달하는 지시와 입력입니다.", "Context" to "현재 응답 생성에 참고시키는 주변 정보입니다.", "Structured Output" to "미리 정한 스키마나 형식에 맞춘 모델 출력입니다.", "Tool Calling" to "모델이 앱에 등록된 외부 기능 사용을 요청하는 구조입니다.", "Fallback" to "주요 기능이 실패했을 때 사용할 대체 경로입니다." )
        ),
        "INTERMEDIATE:10" to ModuleGuide(
            bigPicture = "자동화는 ‘버튼 없이 알아서 실행’되는 마법이 아니라 Trigger가 발생하면 조건을 검사하고 Action을 실행하며, 실패하면 재시도하거나 사람 승인을 요청하도록 설계한 흐름입니다. 운영에서는 성공보다 실패 처리와 기록이 더 중요할 때가 많습니다.",
            analogy = "알람시계처럼 시간이라는 Trigger가 오면 벨이라는 Action이 실행됩니다. 현관 센서는 문이 열렸다는 Trigger 뒤에 밤인지 조건을 확인하고 조명을 켤 수 있습니다.",
            flow = "Trigger 발생 → Condition 검사 → Action 실행 → 결과 확인 → 실패 분류 → Retry/Backoff 또는 사람 승인 → 로그 기록 → 다음 실행 대기 순서입니다.",
            compare = "Schedule은 시간 기반 Trigger, Webhook은 외부 이벤트가 보내는 Trigger입니다. Retry는 다시 시도, Backoff는 재시도 간격을 점점 늘리는 전략입니다. 자동화와 배치 작업도 실행 조건에 따라 다릅니다.",
            realWorld = "매일 보고서 생성, 새 이메일 분류, 주식 데이터 갱신, 일정 알림, 실패 시 관리자 승인 같은 업무를 자동화할 수 있습니다.",
            mistakes = "실패해도 무한 재시도, 중복 실행 방지 없음, 사람이 확인해야 할 위험 작업까지 자동 승인, 로그 없이 조용히 실패하기가 대표적인 문제입니다.",
            glossary = mapOf("Trigger" to "자동화를 시작하게 하는 사건이나 조건입니다.", "Action" to "Trigger 이후 실제로 수행하는 작업입니다.", "Condition" to "Action을 실행할지 판단하는 조건입니다.", "Schedule" to "정해진 시간이나 주기에 실행되도록 하는 규칙입니다.", "Webhook" to "외부 시스템이 이벤트 발생을 HTTP 요청으로 알려주는 방식입니다.", "Backoff" to "반복 실패 시 재시도 간격을 점점 늘리는 전략입니다." )
        )
    )
}
