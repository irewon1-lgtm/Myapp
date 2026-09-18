package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson01 {
    private Track2Lesson01() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 1;
        final String l = "조건에 따라 다른 코드를 실행하는 if 이해하기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 조건·불리언·참과 거짓",
                "조건(condition)\n"
                        + "프로그램이 ‘지금 어떤 경우인가?’를 판단하기 위해 확인하는 기준입니다. 예를 들어 나이가 20세 이상인지, 비밀번호가 맞는지, 장바구니 금액이 5만원 이상인지 같은 질문이 조건이 됩니다. 조건은 보통 참 또는 거짓으로 평가되고, 그 결과에 따라 다음에 실행할 코드가 달라집니다.\n\n"
                        + "불리언(boolean / bool)\n"
                        + "참과 거짓 두 상태를 표현하는 데이터 타입입니다. Python에서는 True와 False를 사용합니다. 조건문의 핵심은 결국 어떤 식을 계산해서 True인지 False인지 확인하는 것입니다. type(True)를 실행하면 bool이라고 나옵니다.\n\n"
                        + "참(True)\n"
                        + "조건이 맞는 상태입니다. age >= 20에서 age가 25라면 결과는 True입니다. if는 조건이 True일 때 들여쓰기된 코드 블록을 실행합니다.\n\n"
                        + "거짓(False)\n"
                        + "조건이 맞지 않는 상태입니다. age >= 20에서 age가 17이라면 False입니다. False가 나오면 해당 if 블록은 건너뛰고, elif나 else가 있다면 다음 경로를 확인합니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — 비교 연산자와 ==",
                "비교(comparison)\n"
                        + "두 값을 비교해 True 또는 False를 만드는 작업입니다. 숫자 크기뿐 아니라 문자열이 같은지도 비교할 수 있습니다. 비교 결과 자체가 bool 값이므로 print(age >= 20)처럼 바로 출력해서 확인할 수도 있습니다.\n\n"
                        + "비교 연산자(comparison operator)\n"
                        + "값을 비교할 때 사용하는 기호입니다. ==는 같은지, !=는 다른지, >와 <는 큰지 작은지, >=와 <=는 이상·이하를 확인합니다. 수학 기호와 비슷하지만 Python에서 정확한 모양을 지켜야 합니다.\n\n"
                        + "== 같음 비교\n"
                        + "= 하나는 변수에 값을 넣는 대입이고, == 두 개는 양쪽 값이 같은지 비교하는 연산자입니다. password = \"1234\"는 저장이고 password == \"1234\"는 ‘같은가?’라는 질문입니다. 초보자가 가장 자주 헷갈리는 부분입니다.\n\n"
                        + "!= 다름 비교\n"
                        + "두 값이 서로 다른지 확인합니다. 예를 들어 status != \"done\"은 status가 done이 아닐 때 True가 됩니다. ‘not equal’이라는 뜻으로 읽으면 기억하기 쉽습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — if·콜론·블록·들여쓰기",
                "if 문(if statement)\n"
                        + "조건이 True일 때만 특정 코드를 실행하게 만드는 제어문입니다. if age >= 20:처럼 조건을 적고, 다음 줄에서 들여쓰기해 실행할 코드를 작성합니다. 프로그램이 처음으로 ‘선택’을 하게 만드는 문법입니다.\n\n"
                        + "콜론(colon, :)\n"
                        + "if, elif, else, 함수 정의, 반복문처럼 ‘이제 아래에 하나의 코드 블록이 이어진다’는 것을 나타내는 기호입니다. if 조건 뒤에 :를 빼먹으면 SyntaxError가 납니다.\n\n"
                        + "코드 블록(block)\n"
                        + "같은 제어 구조에 속하는 코드 묶음입니다. Python에서는 중괄호 대신 들여쓰기로 블록을 표시합니다. if 아래에서 같은 칸만큼 들어간 여러 줄은 같은 조건이 참일 때 함께 실행됩니다.\n\n"
                        + "들여쓰기(indentation)\n"
                        + "줄 앞에 공백을 넣어 코드의 소속을 나타내는 방식입니다. Python 문법의 일부이므로 보기 좋게 꾸미는 용도만이 아닙니다. 같은 블록은 같은 수준으로 들여써야 하며, 보통 공백 4칸을 사용합니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — elif·else·분기·논리 연산자",
                "elif\n"
                        + "앞의 if 조건이 False였을 때 추가 조건을 검사하는 문법입니다. ‘else if’를 줄여 쓴 형태라고 생각하면 됩니다. 여러 범위를 나눌 때 if → elif → elif → else 순서로 사용할 수 있습니다.\n\n"
                        + "else\n"
                        + "앞의 if와 elif가 모두 False일 때 마지막으로 실행하는 경로입니다. else 자체에는 조건을 적지 않습니다. ‘그 밖의 모든 경우’라고 생각하면 됩니다.\n\n"
                        + "분기(branch)\n"
                        + "조건에 따라 프로그램 실행 경로가 여러 갈래로 나뉘는 것을 말합니다. if 문을 배우면 코드는 더 이상 단순히 위에서 아래로 모든 줄을 실행하지 않고, 조건에 맞는 branch만 선택합니다.\n\n"
                        + "논리 연산자(logical operator)\n"
                        + "여러 조건을 하나로 묶는 and, or, not을 말합니다. and는 양쪽이 모두 True여야 True, or는 하나라도 True면 True, not은 True와 False를 반대로 뒤집습니다. 예: age >= 20 and has_id == True."));

        out.add(Track2Content.page(lesson, l, "시작", "프로그램도 상황에 따라 다른 선택을 해야 한다",
                "TRACK 01에서는 코드가 기본적으로 위에서 아래로 실행된다고 배웠습니다. 하지만 실제 프로그램은 모든 상황에서 똑같이 움직이지 않습니다. 로그인에 성공했을 때와 실패했을 때 화면이 다르고, 결제 금액이 일정 기준을 넘으면 배송비가 무료가 되며, 점수가 기준보다 높으면 합격 문구를 보여 줍니다. 이런 ‘상황에 따른 선택’을 만드는 가장 기본적인 도구가 if입니다.\n\n"
                        + "if를 배우면 코드의 흐름이 처음으로 갈라집니다. 중요한 것은 문법을 외우는 것보다 ‘어떤 조건을 확인하고, True일 때 무엇을 실행하고, False일 때 어디로 가는가’를 순서대로 읽는 습관입니다. 앞으로 코드를 볼 때는 조건식의 결과를 먼저 True 또는 False로 예상한 뒤 어느 블록이 실행될지 따라가세요.",
                "신호등을 생각하면 쉽습니다. ‘신호가 초록색인가?’라는 질문이 조건이고, 맞으면 건너가고 아니면 기다립니다. if는 컴퓨터에게 이런 판단 규칙을 적어 주는 문법입니다.",
                null,
                null,
                null,
                null,
                "condition · bool · True · False · branch"));

        out.add(Track2Content.page(lesson, l, "이론", "비교식은 True 또는 False라는 값을 만든다",
                "조건문 안에는 보통 비교식이 들어갑니다. age >= 20을 계산하면 숫자가 나오는 것이 아니라 True 또는 False가 나옵니다. score == 100, password != \"0000\", temperature < 0 같은 식도 모두 bool 값을 만듭니다. 즉 if는 특별한 마법을 하는 것이 아니라 먼저 조건식을 계산하고 그 결과가 True인지 확인합니다.\n\n"
                        + "대입과 비교는 반드시 구분해야 합니다. age = 20은 20을 age에 저장하는 코드이고 age == 20은 현재 age가 20인지 묻는 코드입니다. 오류를 줄이려면 처음에는 조건식을 print로 따로 출력해 보세요. print(age >= 20)을 실행해 True나 False가 예상과 같은지 확인한 다음 if 안에 넣으면 흐름을 이해하기 쉽습니다.",
                "비교식은 ‘예/아니오 질문’입니다. age >= 20은 ‘나이가 20 이상인가?’라고 묻는 것과 같고, Python은 대답을 True 또는 False로 돌려줍니다.",
                "age = 17\nprint(age >= 20)\nprint(age == 17)\nprint(age != 17)",
                null,
                null,
                null,
                "comparison · == · != · >= · <= · bool"));

        out.add(Track2Content.page(lesson, l, "이론", "if·elif·else와 들여쓰기로 실행 경로를 만든다",
                "가장 단순한 if는 조건이 True일 때만 한 블록을 실행합니다. 두 갈래가 필요하면 else를 붙이고, 세 갈래 이상이면 중간에 elif를 사용합니다. Python은 들여쓰기로 어느 코드가 어느 조건에 속하는지 구분하므로 줄 앞의 공백이 실제 문법입니다.\n\n"
                        + "여러 범위를 나눌 때는 위에서부터 조건을 확인합니다. 첫 번째로 True가 된 branch를 실행하면 같은 if-elif-else 묶음의 아래 branch는 더 이상 검사하지 않습니다. 그래서 조건 순서가 중요합니다. 예를 들어 age < 20을 먼저 검사한 뒤 age < 13을 적으면 10살도 첫 조건에서 이미 True가 되어 청소년 경로로 가 버립니다. 더 좁은 범위를 먼저 적는 습관을 들이세요.",
                "문이 세 개 있다고 생각하세요. 첫 문 if를 열 수 있으면 그 문으로 들어가고 끝입니다. 못 열면 다음 elif 문을 보고, 그것도 아니면 마지막 else 문으로 갑니다.",
                "age = 15\n\nif age < 13:\n    print(\"어린이\")\nelif age < 20:\n    print(\"청소년\")\nelse:\n    print(\"성인\")",
                null,
                null,
                null,
                "if · elif · else · indentation · block · order"));

        out.add(Track2Content.page(lesson, l, "예제", "나이에 따라 이용 구분을 바꾸는 프로그램",
                "아래 프로그램은 사용자에게 나이를 입력받고, 숫자로 변환한 다음 세 구간으로 나눕니다. 첫 번째 조건 age < 13이 True면 ‘어린이’를 출력합니다. 첫 조건이 False일 때만 age < 20을 검사하고, 그것도 False면 20세 이상이므로 else가 실행됩니다.\n\n"
                        + "이 코드를 읽을 때 8세, 16세, 25세를 각각 넣었다고 가정하고 한 줄씩 따라가 보세요. 조건문의 실력은 문법을 많이 외우는 것보다 서로 다른 입력을 넣었을 때 어느 경로가 선택되는지 정확히 추적하는 능력에서 생깁니다.",
                "8살이면 첫 질문에서 바로 True, 16살이면 첫 질문은 False이고 두 번째가 True, 25살이면 둘 다 False라서 else로 갑니다.",
                "age = int(input(\"나이: \"))\n\nif age < 13:\n    category = \"어린이\"\nelif age < 20:\n    category = \"청소년\"\nelse:\n    category = \"성인\"\n\nprint(f\"이용 구분: {category}\")",
                null,
                null,
                null,
                "category · branch · range · input · int"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — 점수에 따라 결과 문구 바꾸기",
                "이번에는 숫자 하나를 입력받아 서로 다른 결과를 보여 주는 프로그램을 직접 만듭니다. 한 번에 완성하려 하지 말고 먼저 if/else 두 갈래를 만든 뒤 elif를 추가하세요. 경계값인 60점과 90점을 직접 넣어 보는 것이 중요합니다.\n\n"
                        + "조건을 만든 뒤에는 ‘59, 60, 89, 90을 넣으면 각각 어떤 branch가 실행되는가?’를 코드 실행 전에 먼저 적어 보세요. 기준 바로 앞과 바로 뒤를 검사하는 습관은 실제 프로그램 테스트에서도 매우 중요합니다.",
                "기준점 바로 옆 숫자를 넣어 보면 조건을 잘못 썼는지 쉽게 찾을 수 있습니다. 60 이상인지 확인한다면 59와 60을 꼭 비교해 보세요.",
                "score = int(input(\"점수: \"))\n\n# 여기에 if / elif / else를 작성하세요",
                "1. 90점 이상이면 ‘A’, 60점 이상이면 ‘통과’, 그보다 낮으면 ‘재도전’을 출력하기\n2. 59, 60, 89, 90을 각각 넣어 예상 결과와 실제 결과 비교하기\n3. 조건 순서를 바꾸면 어떤 문제가 생기는지 직접 확인하기\n4. 결과를 result 변수에 저장한 뒤 마지막 print는 한 번만 사용하기",
                null,
                null,
                "boundary · >= · elif · result"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — 점수에 따라 결과 문구 바꾸기",
                "예시 답안\n"
                        + "90점 이상 조건을 가장 먼저 둡니다. score가 95라면 첫 조건에서 True가 되어 A가 저장되고 아래 elif는 검사하지 않습니다. 70이라면 첫 조건은 False, 두 번째 score >= 60은 True라서 통과가 됩니다. 50은 두 조건이 모두 False라서 else로 갑니다.\n\n"
                        + "경계값을 확인하면 59는 재도전, 60은 통과, 89도 통과, 90은 A입니다. 만약 score >= 60을 맨 위에 두면 95도 이미 첫 조건에서 True가 되어 ‘통과’가 나오므로 A 조건까지 도달하지 못합니다. 조건 순서는 프로그램의 의미를 바꿀 수 있습니다.",
                "score = int(input(\"점수: \"))\n\nif score >= 90:\n    result = \"A\"\nelif score >= 60:\n    result = \"통과\"\nelse:\n    result = \"재도전\"\n\nprint(result)"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — 여러 조건을 and와 or로 묶기",
                "실제 조건은 한 가지 값만 보는 경우보다 여러 기준을 함께 확인하는 경우가 많습니다. and는 모든 조건이 맞아야 하고, or는 하나만 맞아도 됩니다. 이번 실습에서는 주문 금액과 멤버십 여부를 함께 사용합니다. 문자열 입력은 정확한 글자를 비교해야 하므로 먼저 어떤 값이 들어오는지도 확인하세요.\n\n"
                        + "복잡한 조건은 한 줄로 길게 쓰기 전에 각 비교식을 따로 print해 보는 것이 좋습니다. total >= 50000이 어떤 bool 값을 만드는지, member == \"yes\"가 어떤 bool 값인지 확인한 뒤 and로 합치면 오류를 찾기 훨씬 쉽습니다.",
                "and는 ‘둘 다’, or는 ‘둘 중 하나라도’입니다. 두 개의 체크박스를 모두 통과해야 하는지, 하나만 통과해도 되는지 생각하면 됩니다.",
                "total = int(input(\"주문 금액: \"))\nmember = input(\"멤버인가요? yes/no: \")\n\n# 조건을 만들어 보세요",
                "1. 주문 금액이 50,000원 이상이고 member가 yes면 ‘VIP 무료배송’ 출력하기\n2. 둘 중 하나만 맞으면 ‘일반 배송’ 출력하기\n3. total >= 100000 또는 member == \"yes\"이면 coupon = True가 되게 만들기\n4. not을 사용해 member가 yes가 아닌 경우를 한 번 표현해 보기",
                null,
                null,
                "and · or · not · membership · bool"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — 여러 조건을 and와 or로 묶기",
                "예시 답안\n"
                        + "VIP 무료배송은 두 조건을 동시에 만족해야 하므로 and를 사용합니다. 주문 금액이 충분해도 member가 no면 전체 조건은 False입니다. coupon 조건은 둘 중 하나만 만족해도 되므로 or가 맞습니다. not은 bool을 뒤집는 연산자이지만 문자열 비교에서는 member != \"yes\"가 더 읽기 쉬운 경우도 많습니다.\n\n"
                        + "조건이 길어지면 괄호를 사용해 의미를 분명히 해도 좋습니다. 프로그램에서 중요한 것은 짧게 쓰는 것이 아니라 나중에 다시 읽었을 때 규칙이 정확히 보이는 것입니다.",
                "total = int(input(\"주문 금액: \"))\nmember = input(\"멤버인가요? yes/no: \")\n\nif total >= 50000 and member == \"yes\":\n    print(\"VIP 무료배송\")\nelse:\n    print(\"일반 배송\")\n\ncoupon = total >= 100000 or member == \"yes\"\nprint(f\"쿠폰 대상: {coupon}\")\nprint(member != \"yes\")"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — 조건문의 실행 경로 읽기",
                "조건문을 제대로 이해했는지는 코드를 보지 않고 문법을 외우는 것보다, 주어진 값에서 어떤 branch가 실행되는지 설명할 수 있는지로 확인하는 편이 좋습니다. 아래 문제는 대입과 비교, 조건 순서, 논리 연산자를 한 번에 점검합니다.",
                "각 문제에서 먼저 조건식이 True인지 False인지 적고 그 다음 실행되는 줄을 고르세요. 머릿속에서 바로 답이 안 나면 비교식만 따로 떼어 계산해도 됩니다.",
                null,
                null,
                "1. x = 10일 때 x == 10의 결과는 무엇인가요?\n2. if x > 5: 아래에 들여쓴 코드는 x가 10일 때 실행될까요?\n3. age = 15일 때 if age < 13 / elif age < 20 / else 중 어느 branch가 실행될까요?\n4. True and False의 결과와 True or False의 결과는 각각 무엇인가요?",
                "1. True\n2. 실행됩니다.\n3. elif age < 20 branch가 실행됩니다.\n4. False, True",
                "== · if · elif · and · or · branch"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — 조건문의 실행 경로 읽기",
                "1. True\n2. 실행됩니다.\n3. elif age < 20 branch\n4. False, True",
                "x == 10은 저장이 아니라 비교이므로 현재 x 값 10과 오른쪽 10이 같은지 확인해 True가 됩니다. x > 5도 True이므로 if 블록이 실행됩니다. age가 15라면 age < 13은 False이고 다음 age < 20이 True라서 두 번째 branch가 선택됩니다. and는 양쪽이 모두 True여야 하므로 True and False는 False이고, or는 하나라도 True면 되므로 True or False는 True입니다.",
                "x 값을 3, 5, 6으로 바꾸고 x > 5가 언제 True가 되는지 적어 보세요. 그 다음 조건을 x >= 5로 바꿔 경계값 5의 결과가 어떻게 달라지는지 확인하세요.",
                "=와 ==를 섞어 쓰는 것, 좁은 범위보다 넓은 범위를 먼저 적는 것, 들여쓰기를 맞추지 않는 것이 초보 단계에서 가장 흔한 실수입니다."));

        out.add(Track2Content.page(lesson, l, "정리", "챕터 1 정리 — 조건을 bool로 만들고 한 경로를 선택한다",
                "if를 읽을 때는 세 단계만 기억하면 됩니다. 첫째, 조건식을 계산해 True 또는 False를 만든다. 둘째, 위에서부터 조건을 확인한다. 셋째, 처음 True가 된 branch의 들여쓰기 블록을 실행한다. elif는 추가 질문이고 else는 앞의 질문이 모두 False일 때의 마지막 경로입니다.\n\n"
                        + "비교 연산자 ==, !=, >, <, >=, <=와 논리 연산자 and, or, not을 이용하면 현실의 규칙을 코드로 표현할 수 있습니다. 다음 챕터에서는 같은 코드를 여러 번 복사하지 않고 반복해서 실행하는 for와 while을 배웁니다.",
                "조건문은 ‘질문 → 참/거짓 → 길 선택’입니다. 이 흐름을 말로 설명할 수 있으면 문법 기호를 잠깐 잊어도 다시 코드를 읽을 수 있습니다.",
                null,
                null,
                null,
                null,
                "condition · comparison · bool · if · elif · else · indentation · and · or · not"));
    }
}
