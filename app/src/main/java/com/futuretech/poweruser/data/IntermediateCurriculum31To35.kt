package com.futuretech.poweruser.data

/**
 * Local-first replacements for intermediate curriculum items 31-35.
 *
 * Design goals:
 * - beginner-readable debugging lessons with enough teaching content
 * - deterministic Python practice instead of paid AI grading
 * - one debugging idea per lesson
 * - concrete evidence, boundary and regression habits
 *
 * These seeds intentionally reuse I07-01..I07-05 so existing progress IDs remain stable.
 */
internal object IntermediateCurriculum31To35 {
    val seeds: List<LessonSeed> = listOf(
        LessonSeed(
            id = "I07-01",
            type = "INTERMEDIATE",
            sequence = 31,
            module = 7,
            moduleTitle = "오류 찾기와 테스트",
            step = 1,
            title = "재현부터 한다: 버그를 잡을 수 있는 문장으로 바꾸기",
            explanation = """
                버그를 고치기 전에 먼저 해야 할 일은 '다시 똑같이 실패하게 만들기'입니다. 이걸 재현이라고 합니다.

                나쁜 보고: "검색이 가끔 안 돼요."
                좋은 보고: "검색창을 비운 상태에서 검색 버튼을 누르면 결과 대신 crash가 난다. 기대 결과는 '검색어를 입력하세요' 안내다."

                재현 기록은 네 칸이면 충분합니다.
                1) 시작 상태: 앱이 어떤 화면/데이터 상태였나
                2) 입력과 행동: 무엇을 입력하고 어떤 버튼을 눌렀나
                3) 실제 결과: 실제로 무엇이 일어났나
                4) 기대 결과: 원래 무엇이 일어나야 하나

                에러 메시지나 로그가 있으면 그대로 붙이고, 없으면 화면에서 마지막으로 정상 동작한 지점을 적습니다. 중요한 것은 원인을 추측하는 게 아니라 '누가 해도 같은 실패를 다시 만들 수 있는 증거'를 먼저 만드는 것입니다.

                이번 실습은 네트워크나 유료 AI를 쓰지 않습니다. 로컬 Python으로 재현 기록을 만들고, 같은 입력에서 같은 실패가 나는지 확인합니다.
            """.trimIndent(),
            outcome = "막연한 오류 설명을 시작 상태·입력·실제 결과·기대 결과가 있는 재현 기록으로 바꾼다.",
            language = "PYTHON",
            code = """
                bug = {
                    "start": "search screen",
                    "input": "",
                    "action": "tap submit",
                    "actual": "crash",
                    "expected": "show validation message"
                }
                for key, value in bug.items():
                    print(key, "=", value)
            """.trimIndent(),
            practice = """
                start = "search screen"
                user_input = ""
                action = "tap submit"
                actual = "crash"
                expected = "show validation message"

                print("START:", start)
                print("INPUT:", repr(user_input))
                print("ACTION:", action)
                print("ACTUAL:", actual)
                print("EXPECTED:", expected)
            """.trimIndent(),
            fill = "버그를 다른 사람이 같은 조건에서 다시 발생시킬 수 있게 만드는 것을 [ 재현 / reproduce ]이라고 합니다.",
            broken = """
                bug = "search broken"
                steps = ""
                print(bug, steps)
            """.trimIndent(),
            fix = """
                bug = "empty search crashes"
                steps = "open search -> leave empty -> tap submit"
                expected = "validation message"
                actual = "crash"
                print(bug, steps, expected, actual)
            """.trimIndent(),
            aiQuestion = "버그 수정 전에 가장 먼저 확보해야 할 것은 무엇입니까?",
            aiOptions = listOf(
                "재현 절차와 기대 결과/실제 결과",
                "원인으로 보이는 코드를 전부 한꺼번에 수정",
                "AI에게 원인을 맞혀 달라고만 요청"
            ),
            aiAnswer = 0,
            keywords = listOf("재현", "입력", "기대결과", "실제결과", "증거"),
            minutes = 32
        ),
        LessonSeed(
            id = "I07-02",
            type = "INTERMEDIATE",
            sequence = 32,
            module = 7,
            moduleTitle = "오류 찾기와 테스트",
            step = 2,
            title = "마지막 정상 지점 찾기: UI·네트워크·API·DB 분리",
            explanation = """
                화면에 데이터가 안 보인다고 해서 '화면 문제'라고 단정하면 안 됩니다. 앱은 보통 UI → 네트워크 → API → DB 같은 여러 층을 지나갑니다. 디버깅은 각 층을 전부 뜯는 일이 아니라 '어디까지 정상이고 어디부터 비정상인지' 경계를 찾는 일입니다.

                예를 들어 종목 목록이 비어 있을 때 다음 순서로 봅니다.
                1) UI: 로딩/에러 상태가 무엇인가
                2) 네트워크: 요청 자체가 나갔는가
                3) API: 상태코드와 응답 본문이 정상인가
                4) DB: 필요한 값이 실제 저장되어 있는가

                API 응답에 데이터가 이미 정상적으로 들어 있는데 화면만 비어 있다면, 서버 전체를 다시 만드는 것보다 UI 상태 연결·파싱·렌더링을 먼저 의심하는 게 합리적입니다. 반대로 요청 자체가 나가지 않았다면 DB부터 고칠 이유가 없습니다.

                핵심 질문은 하나입니다. "마지막으로 정상임을 증명할 수 있는 지점은 어디인가?" 그 다음 한 칸만 조사합니다. 이렇게 하면 수정 범위가 작아지고 기존 기능을 망가뜨릴 가능성도 줄어듭니다.
            """.trimIndent(),
            outcome = "하나의 증상을 UI·네트워크·API·DB 층으로 나누고 마지막 정상 지점 다음 층부터 조사한다.",
            language = "PYTHON",
            code = """
                layers = [
                    ("UI", False),
                    ("network", True),
                    ("API", True),
                    ("DB", True),
                ]
                for name, ok in layers:
                    print(name, "OK" if ok else "FAIL")
            """.trimIndent(),
            practice = """
                evidence = {
                    "network": "request sent",
                    "API": "200 + expected JSON",
                    "DB": "row exists",
                    "UI": "empty list"
                }
                for layer, result in evidence.items():
                    print(layer, "->", result)

                # 어느 층부터 조사해야 하는지 target에 직접 적어보세요.
                target = "UI"
                print("NEXT CHECK:", target)
            """.trimIndent(),
            fill = "여러 층 중 문제가 시작되기 직전까지 정상이라고 확인된 위치를 [ 마지막 정상 지점 ]이라고 부를 수 있습니다.",
            broken = """
                api = "200 + expected JSON"
                ui = "empty"
                next_action = "rewrite DB and API together"
                print(next_action)
            """.trimIndent(),
            fix = """
                api = "200 + expected JSON"
                ui = "empty"
                next_action = "inspect UI state/parsing first"
                print(next_action)
            """.trimIndent(),
            aiQuestion = "API 응답이 200이고 필요한 JSON도 확인됐는데 화면만 비어 있습니다. 가장 먼저 볼 후보는?",
            aiOptions = listOf(
                "UI 상태·파싱·렌더링 연결",
                "DB와 서버를 전부 새로 작성",
                "인터넷 회사를 바꿈"
            ),
            aiAnswer = 0,
            keywords = listOf("UI", "network", "API", "DB", "마지막 정상 지점", "격리"),
            minutes = 34
        ),
        LessonSeed(
            id = "I07-03",
            type = "INTERMEDIATE",
            sequence = 33,
            module = 7,
            moduleTitle = "오류 찾기와 테스트",
            step = 3,
            title = "가설 하나만 검증한다: 최소 수정과 전후 비교",
            explanation = """
                원인을 좁혔다면 다음은 가설을 세웁니다. 가설은 '왜 실패했는지에 대한 시험 가능한 설명'입니다. 예: "빈 검색어 검증이 없어서 crash가 난다."

                여기서 가장 흔한 실수는 UI·API·DB를 한꺼번에 고치는 것입니다. 여러 곳을 동시에 바꾸면 문제가 사라져도 어떤 변경이 효과가 있었는지 알 수 없고, 새 버그가 생겨도 원인을 찾기 어려워집니다.

                안전한 순서는 다음입니다.
                1) 가설 한 문장
                2) 그 가설만 확인하는 가장 작은 수정
                3) 수정 전과 같은 입력으로 다시 실행
                4) 전/후 결과 비교
                5) 가설이 틀리면 변경을 되돌리고 다음 가설

                '작게 바꾸고 같은 조건에서 다시 본다'는 습관은 AI 코딩에서도 특히 중요합니다. AI가 열 파일 10개를 한꺼번에 바꾸게 하기보다 수정 범위를 한두 군데로 제한하고 diff를 확인하면 롤백도 쉽습니다.
            """.trimIndent(),
            outcome = "시험 가능한 가설을 하나 세우고 최소 수정 뒤 동일 입력으로 전후를 비교한다.",
            language = "PYTHON",
            code = """
                before = "crash"
                hypothesis = "empty input is not validated"
                change = "add empty-input guard"
                after = "validation message"

                print("HYPOTHESIS:", hypothesis)
                print("CHANGE:", change)
                print("BEFORE:", before)
                print("AFTER:", after)
            """.trimIndent(),
            practice = """
                price = None

                # 가설: price가 None인데 숫자 계산을 해서 실패한다.
                if price is None:
                    result = "price missing"
                else:
                    result = price * 2

                print(result)
            """.trimIndent(),
            fill = "원인이라고 예상하고 실제로 시험해 볼 수 있는 설명을 [ 가설 / hypothesis ]이라고 합니다.",
            broken = """
                changes = ["UI rewrite", "API rewrite", "DB rewrite", "validation"]
                print("changed all:", changes)
            """.trimIndent(),
            fix = """
                hypothesis = "empty input not validated"
                one_change = "add validation"
                print("test one hypothesis:", hypothesis, one_change)
            """.trimIndent(),
            aiQuestion = "버그 수정 때 여러 층을 동시에 크게 바꾸는 가장 큰 단점은?",
            aiOptions = listOf(
                "어떤 변경이 원인을 해결했는지 분리하기 어렵다.",
                "코드 줄 수가 무조건 줄어든다.",
                "회귀시험이 필요 없어지는 장점이 있다."
            ),
            aiAnswer = 0,
            keywords = listOf("가설", "최소 수정", "전후 비교", "diff", "rollback"),
            minutes = 34
        ),
        LessonSeed(
            id = "I07-04",
            type = "INTERMEDIATE",
            sequence = 34,
            module = 7,
            moduleTitle = "오류 찾기와 테스트",
            step = 4,
            title = "경계에서 깨지는지 본다: 테스트 케이스 설계",
            explanation = """
                정상 입력 하나가 통과했다고 기능이 안전한 것은 아닙니다. 버그는 경계에서 많이 생깁니다. 기준값 바로 아래·같음·바로 위, 빈 값, 아주 긴 값, 중복 값처럼 '조건이 바뀌는 자리'를 의도적으로 시험해야 합니다.

                예를 들어 80점 이상 합격이면 100점만 시험할 게 아니라 79 / 80 / 81을 먼저 봅니다. 80에서 조건이 바뀌기 때문입니다. 입력칸이라면 빈 문자열, 공백만 있는 문자열, 최대 길이, 최대 길이+1도 후보입니다.

                좋은 테스트 세트는 최소 세 종류를 포함합니다.
                - 정상: 평범한 입력이 잘 되는가
                - 경계: 조건이 바뀌는 정확한 지점이 맞는가
                - 실패/예외: 비어 있거나 잘못된 값에서 안전하게 실패하는가

                이번에는 테스트를 AI에게 채점시키지 않습니다. 기대값을 코드에 명시하고 실제 결과와 정확히 비교합니다. 같은 입력은 언제 실행해도 같은 PASS/FAIL을 내므로 비용이 없고 판정도 흔들리지 않습니다.
            """.trimIndent(),
            outcome = "정상·경계·실패 케이스를 만들고 기대값과 실제값을 deterministic하게 비교한다.",
            language = "PYTHON",
            code = """
                def passed(score):
                    return score >= 80

                cases = [
                    (79, False),
                    (80, True),
                    (81, True),
                ]

                for score, expected in cases:
                    actual = passed(score)
                    print(score, "PASS" if actual == expected else "FAIL")
            """.trimIndent(),
            practice = """
                def passed(score):
                    return score >= 80

                cases = [
                    (79, False),
                    (80, True),
                    (81, True),
                    (0, False),
                    (100, True),
                ]

                for value, expected in cases:
                    actual = passed(value)
                    print(value, expected, actual)
            """.trimIndent(),
            fill = "기준값 바로 아래·같음·바로 위를 시험하는 것을 [ 경계값 테스트 / boundary test ]라고 합니다.",
            broken = """
                def passed(score):
                    return score > 80

                print(passed(100))
            """.trimIndent(),
            fix = """
                def passed(score):
                    return score >= 80

                for score in [79, 80, 81]:
                    print(score, passed(score))
            """.trimIndent(),
            aiQuestion = "'80 이상 합격' 조건에서 가장 중요한 첫 경계 테스트 묶음은?",
            aiOptions = listOf(
                "79 / 80 / 81",
                "100만 한 번",
                "무작위 숫자 하나만"
            ),
            aiAnswer = 0,
            keywords = listOf("테스트", "경계값", "정상", "실패", "기대값", "deterministic"),
            minutes = 36
        ),
        LessonSeed(
            id = "I07-05",
            type = "INTERMEDIATE",
            sequence = 35,
            module = 7,
            moduleTitle = "오류 찾기와 테스트",
            step = 5,
            title = "고친 뒤 끝이 아니다: 회귀시험과 증거 남기기",
            explanation = """
                버그 하나가 사라졌다고 수정이 끝난 것은 아닙니다. 새 수정 때문에 원래 잘 되던 기능이 깨질 수 있습니다. 수정 전 정상 기능을 다시 확인하는 것을 회귀시험이라고 합니다.

                회귀시험의 핵심은 '이번 버그용 테스트'와 '기존 정상 기능 테스트'를 같이 돌리는 것입니다. 예를 들어 빈 검색어 crash를 고쳤다면 빈 입력 테스트만 PASS시키는 게 아니라 일반 검색, 한글 검색, 이전 화면 복귀처럼 원래 되던 동작도 다시 확인합니다.

                그리고 PASS라는 말에는 증거가 붙어야 합니다.
                - 어떤 테스트를 실행했는가
                - 몇 개가 통과/실패했는가
                - 실패가 있다면 무엇인가
                - 어떤 코드/버전을 시험했는가

                자동화된 테스트 결과·로그·commit SHA 같은 재현 가능한 증거가 가장 좋습니다. 스크린샷은 UI 증거에는 유용하지만 내부 로직 전체를 증명하지는 못합니다. 실행하지 않은 테스트를 PASS라고 적는 것은 금지합니다.

                이 레슨도 로컬 deterministic test만으로 끝낼 수 있습니다. AI 설명은 선택 기능일 뿐 채점 근거가 아닙니다.
            """.trimIndent(),
            outcome = "수정한 버그 테스트와 기존 기능 테스트를 함께 실행하고 버전·결과가 포함된 증거를 남긴다.",
            language = "PYTHON",
            code = """
                def normalize(text):
                    return text.strip().lower()

                tests = [
                    (" AAPL ", "aapl"),
                    ("NVDA", "nvda"),
                    ("", ""),
                ]

                passed = 0
                for raw, expected in tests:
                    actual = normalize(raw)
                    ok = actual == expected
                    passed += int(ok)
                    print(raw, "PASS" if ok else "FAIL")

                print("SUMMARY:", passed, "/", len(tests))
            """.trimIndent(),
            practice = """
                def normalize(text):
                    return text.strip().lower()

                regression = [
                    (" AAPL ", "aapl"),
                    ("NVDA", "nvda"),
                    ("  msft", "msft"),
                    ("", ""),
                ]

                results = []
                for raw, expected in regression:
                    actual = normalize(raw)
                    results.append(actual == expected)

                print("PASS:", sum(results))
                print("TOTAL:", len(results))
                print("ALL PASS:", all(results))
            """.trimIndent(),
            fill = "수정 후 기존에 정상 동작하던 기능이 깨지지 않았는지 다시 확인하는 테스트를 [ 회귀시험 / regression test ]이라고 합니다.",
            broken = """
                result = "PASS"
                evidence = "not executed"
                print(result, evidence)
            """.trimIndent(),
            fix = """
                executed = True
                passed = 4
                total = 4
                evidence = f"executed={executed}, result={passed}/{total}"
                print(evidence)
            """.trimIndent(),
            aiQuestion = "테스트를 실제로 실행하지 않았지만 코드가 맞아 보여서 보고서에 PASS라고 적어도 됩니까?",
            aiOptions = listOf(
                "아니오. 실행 증거가 없으면 PASS라고 쓰지 않는다.",
                "예. 코드가 짧으면 실행은 생략한다.",
                "AI가 괜찮다고 하면 PASS로 기록한다."
            ),
            aiAnswer = 0,
            keywords = listOf("회귀시험", "증거", "로그", "PASS", "commit", "재현성"),
            minutes = 38
        )
    )

    private val byId = seeds.associateBy { it.id }

    fun replace(seed: LessonSeed): LessonSeed = byId[seed.id] ?: seed
}
