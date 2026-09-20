package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson01 {
    private Track2Lesson01() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 1;
        final String l = "조건에 따라 다른 코드를 실행하는 if 이해하기";

        out.add(Track2Content.page(
                lesson, l,
                "시작",
                "P01 · Python과 if를 오늘 처음 본 사람에게",
                "Python은 사람이 쓴 명령을 컴퓨터가 실행할 수 있게 표현하는 프로그래밍 언어입니다. 이 챕터에서 가장 먼저 익힐 것은 문법 암기가 아니라 ‘조건을 확인하고 행동을 바꾼다’는 사고입니다. 비가 오면 우산을 챙기고, 재고가 0이면 품절을 보여 주는 것처럼 프로그램도 상황에 따라 다른 길을 선택합니다.\n\nif는 그 선택을 만드는 가장 기본적인 문법입니다. 조건을 계산한 결과가 True이면 안쪽 코드를 실행하고 False이면 건너뜁니다. 챕터 마지막에는 if·elif·else를 읽고, 경계값과 복합 조건을 검증하고, 고장 난 조건을 고쳐 작은 판단 로직을 직접 설계하는 단계까지 갑니다.",
                "쉽게 말하면 if는 컴퓨터에게 ‘이 질문이 맞으면 이것을 해’라고 가르치는 문법입니다.",
                "age = 20\n\nif age >= 19:\n    print(\"성인입니다\")",
                "코드를 외우지 말고 먼저 age >= 19가 True인지 False인지 말해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · Python과 if를 오늘 처음 본 사람에게"));

        out.add(Track2Content.page(
                lesson, l,
                "용어집",
                "P02 · Python / 프로그램 / 코드 / 문법",
                "Python은 프로그래밍 언어, 프로그램은 컴퓨터가 어떤 일을 하도록 만든 명령들의 묶음, 코드는 그 명령을 실제 언어 문법으로 적은 글입니다. 문법(syntax)은 Python이 코드를 읽기 위해 요구하는 약속입니다.\n\n중요한 구분은 두 가지입니다. 문법이 틀리면 Python이 실행 자체를 못 하는 경우가 많고, 문법은 맞지만 생각이 틀리면 실행은 되면서 잘못된 결과가 나옵니다. 뒤의 것이 논리 오류이며 실제 프로그램에서 더 위험할 수 있습니다.",
                "한국어에도 문장 규칙이 있듯 Python에도 ‘이 모양으로 써야 읽을 수 있다’는 규칙이 있습니다.",
                "print(\"안녕하세요\")\n\nif age >= 19:\n    print(\"성인\")",
                "‘문법 오류’와 ‘논리 오류’를 자기 말로 각각 한 문장씩 설명해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · Python / 프로그램 / 코드 / 문법"));

        out.add(Track2Content.page(
                lesson, l,
                "용어집",
                "P03 · 값 / 변수 / 대입",
                "값(value)은 프로그램이 다루는 실제 데이터입니다. 10, 3.14, \"서울\", True 같은 것이 모두 값입니다. 변수(variable)는 그 값을 다시 사용하기 위해 붙이는 이름입니다. age = 20은 age라는 이름으로 20을 기억하겠다는 뜻입니다.\n\nPython에서 =는 수학의 등호가 아니라 대입입니다. 오른쪽 값을 왼쪽 이름에 연결합니다. 반대로 ==는 두 값이 같은지 비교해서 True 또는 False를 만드는 비교 연산자입니다. 조건문을 배우기 전에 이 두 기호를 확실히 분리해야 합니다.",
                "=는 ‘넣는다’, ==는 ‘같은가?’라고 소리 내서 읽으면 덜 헷갈립니다.",
                "age = 20\nscore = 85\nname = \"민수\"\n\nprint(age == 20)",
                "age = 19와 age == 19가 각각 무엇을 하는지 설명해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 값 / 변수 / 대입"));

        out.add(Track2Content.page(
                lesson, l,
                "용어집",
                "P04 · True / False / Boolean",
                "조건문은 결국 ‘이 조건이 참인가 거짓인가?’를 판단합니다. Python은 참을 True, 거짓을 False라고 쓰며 이 두 값을 다루는 타입을 bool 또는 Boolean이라고 합니다.\n\n10 > 3은 True, 10 < 3은 False가 됩니다. if 뒤에 오는 식도 결국 True 또는 False로 평가됩니다. 따라서 긴 조건을 만나도 먼저 작은 비교식으로 쪼개 각각 True인지 False인지 확인하면 흐름을 읽기 쉬워집니다.",
                "if 뒤의 문장은 어려운 주문이 아니라 결국 예/아니오로 답할 수 있는 질문입니다.",
                "print(10 > 3)   # True\nprint(10 < 3)   # False\n\nis_member = True",
                "age = 18일 때 age >= 19의 결과를 실행 전에 예상해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · True / False / Boolean"));

        out.add(Track2Content.page(
                lesson, l,
                "용어집",
                "P05 · 비교 연산자와 경계의 첫 이해",
                "비교 연산자는 값을 비교해 bool을 만듭니다. == 같음, != 다름, > 초과, < 미만, >= 이상, <= 이하입니다. 특히 >와 >=, <와 <=는 경계값에서 결과가 달라집니다.\n\n예를 들어 ‘19세 이상 허용’은 age >= 19입니다. age > 19라고 쓰면 정확히 19인 사람이 빠집니다. 이렇게 규칙이 바뀌는 딱 걸치는 값을 경계값이라고 부릅니다. 조건문 실력은 정상값보다 경계값을 정확히 다루는 데서 차이가 납니다.",
                "‘이상’에는 같은 값이 포함되고 ‘초과’에는 포함되지 않습니다.",
                "age = 19\nprint(age > 19)   # False\nprint(age >= 19)  # True",
                "18, 19, 20을 넣었을 때 age >= 19가 각각 어떤 결과인지 적어 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 비교 연산자와 경계의 첫 이해"));

        out.add(Track2Content.page(
                lesson, l,
                "용어집",
                "P06 · if / elif / else / 들여쓰기",
                "if는 첫 조건을 묻고, elif는 앞 조건이 False였을 때 추가 질문을 하며, else는 앞의 어떤 조건에도 해당하지 않을 때 실행되는 마지막 경로입니다. if와 elif 뒤에는 조건과 콜론(:)이 오고 else에는 조건을 적지 않습니다.\n\nPython은 중괄호 대신 들여쓰기로 코드 블록을 표시합니다. 같은 수준으로 들여쓴 줄들은 같은 조건에 속합니다. 따라서 들여쓰기는 보기 좋게 만드는 장식이 아니라 프로그램 구조 자체입니다.",
                "오른쪽으로 들어간 줄은 ‘이 조건 안에 속한다’고 읽으면 됩니다.",
                "if score >= 90:\n    print(\"A\")\nelif score >= 80:\n    print(\"B\")\nelse:\n    print(\"C\")",
                "각 줄을 가리키며 어떤 줄이 if 블록, elif 블록, else 블록인지 말해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · if / elif / else / 들여쓰기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P07 · 첫 번째 if를 한 줄씩 읽는 법",
                "age = 20을 먼저 실행하면 age가 20을 가리킵니다. 다음 줄 if age >= 19:에서 20 >= 19를 계산하므로 True입니다. 따라서 들여쓴 print가 실행됩니다. 그 뒤 들여쓰기가 끝난 줄은 조건과 관계없이 다시 정상적으로 실행됩니다.\n\n조건문을 읽을 때는 ‘현재 변수 값 → 조건식 계산 → True/False → 실행되는 블록’ 네 단계로 추적하세요. 이 습관이 생기면 코드가 길어져도 길을 잃지 않습니다.",
                "컴퓨터가 한 줄씩 읽는 장면을 머릿속으로 슬로모션 재생한다고 생각하세요.",
                "age = 20\n\nif age >= 19:\n    print(\"성인입니다\")\n\nprint(\"확인 끝\")",
                "age를 18로 바꾸면 어떤 줄이 출력되고 어떤 줄이 건너뛰는지 먼저 예상하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 첫 번째 if를 한 줄씩 읽는 법"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P08 · 조건문의 실행 흐름",
                "if는 프로그램 전체를 멈추는 문법이 아닙니다. 조건이 False이면 그 if 블록만 건너뛰고 그 다음 코드로 계속 진행합니다. money가 5000이고 조건이 money >= 10000이라면 구매 가능 문구만 생략되고 뒤의 ‘프로그램 계속’은 실행됩니다.\n\n디버깅할 때는 ① 현재 값 ② 조건식의 결과 ③ 선택된 블록을 따로 적으세요. 한 문장으로 뭉쳐 읽는 것보다 훨씬 정확합니다.",
                "False는 ‘프로그램 종료’가 아니라 ‘이 블록은 건너뛴다’라는 뜻입니다.",
                "money = 5000\n\nif money >= 10000:\n    print(\"구매 가능\")\n\nprint(\"프로그램 계속\")",
                "money가 12000일 때와 5000일 때 출력 차이를 비교하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 조건문의 실행 흐름"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P09 · 콜론과 들여쓰기 오류",
                "if 조건 뒤의 콜론(:)은 아래에 코드 블록이 이어진다는 표시입니다. 콜론이 빠지거나 if 안의 줄이 들여쓰기되지 않으면 Python이 구조를 이해하지 못해 문법 오류가 납니다.\n\n처음에는 한 단계 들여쓰기를 공백 4칸으로 통일하세요. 같은 블록에 속하는 줄은 같은 깊이를 유지합니다. 오류가 나면 콜론, 따옴표·괄호 닫힘, 들여쓰기 순서로 확인하면 초보 단계의 많은 오류를 빠르게 찾을 수 있습니다.",
                "들여쓰기는 ‘이 줄이 누구 소속인가’를 표시하는 표지판입니다.",
                "age = 20\n\nif age >= 19:\n    print(\"성인\")\n    print(\"입장 가능\")\n\nprint(\"끝\")",
                "콜론을 지운 버전과 들여쓰기를 지운 버전을 직접 비교해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 콜론과 들여쓰기 오류"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P10 · 숫자 비교식을 한국어에서 코드로 바꾸기",
                "조건문을 잘 쓰려면 기호보다 먼저 규칙의 뜻을 정확히 번역해야 합니다. ‘19세 이상’은 age >= 19, ‘100점 초과’는 score > 100, ‘재고가 0’은 stock == 0, ‘비밀번호가 다름’은 password != saved_password입니다.\n\n문장을 코드로 바꾼 뒤에는 반드시 경계값을 넣어 확인하세요. ‘이상/이하’인지 ‘초과/미만’인지 한 글자 차이가 실제 사용자를 다른 경로로 보낼 수 있습니다.",
                "한국어 조건을 먼저 정확히 쓰고 그 다음 기호로 번역하세요.",
                "temperature = 30\nprint(temperature > 30)\nprint(temperature >= 30)\nprint(temperature == 30)",
                "‘수량이 1 이상’, ‘가격이 0보다 큼’, ‘상태가 done이 아님’을 각각 비교식으로 적으세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 숫자 비교식을 한국어에서 코드로 바꾸기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P11 · =와 ==를 완전히 분리하기",
                "score = 90은 90을 score에 저장하는 대입입니다. score == 90은 현재 score가 90인지 묻는 비교이며 결과는 True 또는 False입니다. 조건문에서는 대부분 ‘질문’이 필요하므로 ==를 사용합니다.\n\n기호를 볼 때 =는 ‘넣는다’, ==는 ‘같은가?’라고 읽는 습관을 들이면 좋습니다. 실행되는 코드를 보면서 오른쪽에서 값이 만들어지고 왼쪽 이름에 저장되는지, 아니면 양쪽을 비교하는지 구분하세요.",
                "=는 행동이고 ==는 질문입니다.",
                "score = 90\n\nif score == 90:\n    print(\"정확히 90점\")",
                "score = 80 다음 print(score == 90)의 출력이 무엇인지 예상하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · =와 ==를 완전히 분리하기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P12 · 숫자와 문자열은 다르다",
                "숫자 19와 문자열 \"19\"는 화면에는 비슷해 보여도 다른 타입입니다. 19 == \"19\"는 False입니다. 특히 input()으로 받은 값은 기본적으로 문자열이므로 숫자 크기를 비교하려면 보통 int() 등으로 변환합니다.\n\n조건이 이상하게 동작할 때는 비교 연산자만 보지 말고 양쪽 값의 타입도 확인하세요. ‘내가 숫자를 비교하는가, 글자를 비교하는가?’라는 질문은 초보 단계에서 매우 중요합니다.",
                "겉모양이 같아도 숫자와 글자는 컴퓨터에게 서로 다른 종류입니다.",
                "age_text = input(\"나이: \")\nage = int(age_text)\n\nif age >= 19:\n    print(\"성인\")",
                "19 == \"19\"와 int(\"19\") == 19의 차이를 설명하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 숫자와 문자열은 다르다"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P13 · if와 else로 둘 중 하나 선택하기",
                "if/else는 결과가 정확히 두 갈래일 때 가장 이해하기 쉽습니다. 먼저 if 조건을 계산하고 True이면 if 블록을 실행합니다. False이면 else 블록을 실행합니다. 같은 if/else 묶음에서는 두 블록이 동시에 실행되지 않습니다.\n\n로그인 성공/실패, 재고 있음/없음, 기준 충족/미충족처럼 서로 배타적인 두 경로를 표현할 때 자주 사용합니다.",
                "if가 첫 문이고 else는 ‘그 문이 아니면 들어가는 두 번째 문’입니다.",
                "age = 18\n\nif age >= 19:\n    print(\"성인\")\nelse:\n    print(\"미성년자\")",
                "age를 19로 바꾸면 어느 블록이 실행되는지 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · if와 else로 둘 중 하나 선택하기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P14 · elif로 세 가지 이상 나누기",
                "elif는 앞의 조건이 False였을 때만 검사되는 추가 질문입니다. if/elif/else 묶음은 위에서 아래로 검사하다가 처음 True가 된 한 갈래를 실행한 뒤 나머지를 건너뜁니다.\n\nscore가 85라면 score >= 90은 False, score >= 80은 True이므로 B 경로가 선택됩니다. 이 시점에는 첫 조건이 이미 False였다는 정보가 있으므로 ‘80 이상 90 미만’을 굳이 두 조건으로 반복하지 않아도 됩니다.",
                "위에서부터 문을 하나씩 두드리고 처음 열린 문으로 들어가면 그 묶음은 끝입니다.",
                "score = 85\n\nif score >= 90:\n    print(\"A\")\nelif score >= 80:\n    print(\"B\")\nelif score >= 70:\n    print(\"C\")\nelse:\n    print(\"D\")",
                "score를 95, 85, 75, 65로 바꿔 각각 어느 갈래인지 추적하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · elif로 세 가지 이상 나누기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P15 · 조건 순서가 결과를 바꾼다",
                "문법이 맞아도 조건 순서가 잘못되면 결과는 틀릴 수 있습니다. score >= 70을 먼저 두면 95점도 첫 조건에서 이미 True가 되어 뒤의 90점 조건에 도달하지 않습니다.\n\n범위가 겹치는 elif 체인에서는 보통 더 구체적이고 엄격한 조건을 먼저 두고 넓은 조건을 뒤에 둡니다. 조건을 작성한 뒤 ‘위 조건이 아래 조건을 먹어버리지는 않는가?’를 반드시 확인하세요.",
                "큰 그물부터 던지면 작은 기준까지 갈 기회가 없어집니다.",
                "score = 95\n\nif score >= 90:\n    grade = \"A\"\nelif score >= 80:\n    grade = \"B\"\nelif score >= 70:\n    grade = \"C\"\nelse:\n    grade = \"D\"",
                "일부러 70 조건을 맨 위로 옮겨 95점 결과가 왜 틀리는지 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 조건 순서가 결과를 바꾼다"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P16 · and — 두 조건이 모두 필요할 때",
                "and는 양쪽 조건이 모두 True일 때만 전체가 True입니다. ‘19세 이상이고 티켓이 있어야 입장’처럼 두 조건을 동시에 만족해야 하는 규칙에 사용합니다.\n\n조건 A와 B가 있으면 True/True만 통과하고 나머지 세 조합은 False입니다. bool 변수는 이미 True/False이므로 has_ticket == True 대신 has_ticket이라고 써도 됩니다.",
                "and는 한국어의 ‘그리고, 둘 다’입니다.",
                "age = 25\nhas_ticket = True\n\nif age >= 19 and has_ticket:\n    print(\"입장 가능\")",
                "성인/미성년 × 티켓 있음/없음 네 조합을 표로 만들어 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · and — 두 조건이 모두 필요할 때"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P17 · or — 하나만 맞아도 될 때",
                "or는 두 조건 중 하나라도 True이면 전체가 True입니다. VIP이거나 직원이면 입장 가능, 토요일이거나 일요일이면 주말 같은 규칙에 사용합니다.\n\n각 비교는 완전한 식으로 적는 습관이 안전합니다. day == \"토요일\" or \"일요일\"처럼 두 번째 비교를 생략하면 의도와 다른 동작이 생길 수 있습니다. day == \"토요일\" or day == \"일요일\"처럼 씁니다.",
                "or는 ‘둘 중 하나라도’입니다.",
                "day = \"일요일\"\n\nif day == \"토요일\" or day == \"일요일\":\n    print(\"주말\")",
                "VIP=False, staff=True인 경우 전체 조건이 왜 True인지 설명하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · or — 하나만 맞아도 될 때"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P18 · not — 참과 거짓 뒤집기",
                "not은 bool 결과를 반대로 만듭니다. not True는 False, not False는 True입니다. is_blocked가 False일 때 not is_blocked는 True가 되어 ‘차단되지 않은 사용자’라는 의미를 자연스럽게 표현할 수 있습니다.\n\n다만 not을 여러 번 중첩하거나 부정적인 변수 이름과 섞으면 읽기 어려워집니다. 짧게 쓰는 것보다 사람이 뜻을 바로 이해할 수 있는 조건을 우선하세요.",
                "not은 예/아니오 답을 뒤집는 스위치입니다.",
                "is_blocked = False\n\nif not is_blocked:\n    print(\"입장 가능\")",
                "not True, not False, not (10 > 3)의 결과를 적어 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · not — 참과 거짓 뒤집기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P19 · 괄호로 복합 조건의 뜻 고정하기",
                "‘회원이면서 5만원 이상 구매했거나 VIP이면 무료배송’은 (is_member and price >= 50000) or is_vip처럼 묶을 수 있습니다. 괄호는 계산 우선순위뿐 아니라 사람이 규칙 덩어리를 눈으로 구분하게 해 줍니다.\n\n복합 조건이 길어지면 먼저 한국어 규칙을 괄호 단위로 나눈 뒤 코드로 번역하세요. 이는 문법 오류보다 찾기 어려운 논리 오류를 줄이는 강력한 습관입니다.",
                "괄호는 조건 두세 개를 하나의 ‘생각 덩어리’로 묶는 표시입니다.",
                "is_member = True\nprice = 60000\nis_vip = False\n\nif (is_member and price >= 50000) or is_vip:\n    print(\"무료배송\")",
                "회원=False, 가격=10000, VIP=True일 때 어느 부분 때문에 True인지 말해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 괄호로 복합 조건의 뜻 고정하기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P20 · 중첩 if — 조건 안에 다시 조건 넣기",
                "if 안에 또 if를 넣는 것을 중첩 조건이라고 합니다. 첫 질문이 True일 때만 두 번째 질문이 의미가 있을 경우 자연스럽습니다. 예를 들어 회원인지 확인한 다음 회원에게만 VIP 등급을 확인할 수 있습니다.\n\n하지만 중첩이 3~4단계 이상 깊어지면 현재 어느 조건 안에 있는지 추적하기 어려워집니다. 그때는 조건을 합치거나 실패 조건을 먼저 처리하는 방식으로 구조를 평평하게 만들 수 있습니다.",
                "큰 문을 통과한 사람에게만 안쪽의 두 번째 문을 보여주는 구조입니다.",
                "is_member = True\ngrade = \"VIP\"\n\nif is_member:\n    if grade == \"VIP\":\n        print(\"VIP 혜택\")\n    else:\n        print(\"일반 회원 혜택\")\nelse:\n    print(\"비회원\")",
                "중첩된 두 조건을 각각 어떤 질문인지 한국어로 바꿔 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 중첩 if — 조건 안에 다시 조건 넣기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P21 · 중첩을 줄이는 방법 — 조건 합치기",
                "두 조건이 모두 참일 때만 같은 행동을 하고 중간에 다른 처리가 없다면 중첩 if를 and로 합칠 수 있습니다. if is_member: 안에 if price >= 50000:만 있는 구조는 if is_member and price >= 50000:으로 평평하게 만들 수 있습니다.\n\n다만 중첩마다 서로 다른 else 의미가 있거나 단계별 처리가 필요하면 무조건 합치면 안 됩니다. ‘두 질문을 합쳐도 원래의 각 경로 의미가 그대로 유지되는가?’를 먼저 확인하세요.",
                "같은 목적의 두 문을 연속 통과해야 한다면 한 문장으로 합칠 수 있는지 살펴보세요.",
                "if is_member and price >= 50000:\n    print(\"회원 할인\")",
                "중첩 버전과 and 버전을 각각 써 보고 결과가 같은 입력 세 개를 테스트하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 중첩을 줄이는 방법 — 조건 합치기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P22 · Guard 방식 — 실패를 먼저 걸러내기",
                "코드가 깊게 중첩되면 정상 흐름을 찾기 어려워집니다. Guard 방식은 잘못된 입력이나 진행 불가능한 조건을 먼저 처리하고 정상 로직을 더 평평하게 읽게 만드는 사고법입니다.\n\n예를 들어 함수 안에서 age < 0 같은 명백한 오류를 먼저 검사해 반환한 뒤 정상 나이 처리로 넘어갈 수 있습니다. 이 챕터에서는 함수의 return을 깊게 배우지 않아도 ‘예외적인 실패 조건을 먼저 분리하면 핵심 경로가 선명해진다’는 구조를 이해하면 됩니다.",
                "입구에서 들어오면 안 되는 경우를 먼저 돌려보내면 안쪽 길이 단순해집니다.",
                "age = -1\n\nif age < 0:\n    print(\"잘못된 나이\")\nelif age < 19:\n    print(\"미성년자\")\nelse:\n    print(\"성인\")",
                "정상값보다 먼저 검사할 ‘잘못된 값’이 무엇인지 한 가지 더 만들어 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · Guard 방식 — 실패를 먼저 걸러내기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P23 · 경계값 — 조건문 테스트의 핵심",
                "경계값은 규칙이 바뀌는 바로 그 지점입니다. ‘19세 이상’이면 18, 19, 20이 핵심 테스트이고 ‘100점 이상 보너스’면 99, 100, 101이 핵심입니다. 멀리 떨어진 정상값만 넣으면 >와 >= 실수를 발견하지 못할 수 있습니다.\n\n조건을 만들 때마다 기준값 바로 아래, 정확한 기준값, 바로 위 값을 세트로 확인하는 습관을 들이세요. 실제 버그가 경계에서 자주 발생하는 이유는 사람이 ‘대충 이 정도’로 읽기 쉽기 때문입니다.",
                "문이 열리는 정확한 선의 바로 앞·딱 그 선·바로 뒤를 확인합니다.",
                "minimum_age = 19\nfor_test = \"18 / 19 / 20을 각각 대입해 본다\"",
                "점수 80 이상 통과 규칙의 최소 경계 테스트 세트를 적어 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 경계값 — 조건문 테스트의 핵심"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P24 · 범위 조건 — 사이 값을 표현하기",
                "‘13세 이상 19세 미만’처럼 한 값이 범위 안에 있는지 확인하려면 age >= 13 and age < 19처럼 두 비교를 and로 연결할 수 있습니다. Python은 13 <= age < 19처럼 연속 비교도 지원하지만 처음에는 두 조건의 뜻을 분리해 이해하는 편이 좋습니다.\n\n범위에는 시작과 끝을 포함하는지 반드시 명확히 해야 합니다. >=와 >, <=와 < 중 무엇을 쓰는지가 정책의 일부입니다.",
                "범위는 ‘왼쪽 문도 통과하고 오른쪽 문도 통과해야 한다’고 생각하세요.",
                "age = 15\n\nif age >= 13 and age < 19:\n    print(\"청소년\")\n\n# Python에서는 다음도 가능\nif 13 <= age < 19:\n    print(\"청소년\")",
                "12, 13, 18, 19를 넣어 범위의 시작과 끝을 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 범위 조건 — 사이 값을 표현하기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P25 · 여러 if와 if/elif의 차이",
                "독립된 if 여러 개는 각각 따로 검사하므로 여러 블록이 동시에 실행될 수 있습니다. 반대로 if/elif/else 체인은 처음 True가 된 한 갈래만 실행합니다.\n\n‘70점 이상 문구도, 80점 이상 문구도, 90점 이상 문구도 모두 표시’하려면 독립 if가 맞을 수 있고, ‘A/B/C 중 등급 하나만 선택’하려면 if/elif/else가 맞습니다. 선택 구조를 결정하기 전에 ‘여러 결과가 동시에 가능한가?’를 물어보세요.",
                "if 여러 개는 체크박스 여러 개, if/elif/else는 라디오 버튼 하나 선택과 비슷합니다.",
                "score = 95\n\nif score >= 70:\n    print(\"70점 이상\")\nif score >= 80:\n    print(\"80점 이상\")\nif score >= 90:\n    print(\"90점 이상\")",
                "같은 score=95를 if/elif/else로 바꿨을 때 출력 개수가 왜 달라지는지 설명하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 여러 if와 if/elif의 차이"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P26 · Decision Table — 코드 전에 규칙 표 만들기",
                "조건이 여러 개면 바로 if부터 쓰지 말고 가능한 조합을 표로 펼쳐 보는 것이 좋습니다. VIP 여부와 구매금액 5만원 이상 여부가 있다면 True/False 조합은 네 개입니다. 각 조합에서 배송비가 얼마인지 먼저 결정하면 빠진 경우를 발견하기 쉽습니다.\n\nDecision Table은 코드보다 정책 자체가 맞는지 검토하게 해 주고, 나중에는 각 행을 테스트 케이스로 그대로 사용할 수 있습니다. 복잡한 조건일수록 ‘코드 작성’보다 ‘규칙 구조화’가 먼저입니다.",
                "조건 조합을 표에 다 적으면 ‘생각하지 못한 경우’를 눈으로 찾을 수 있습니다.",
                "is_vip = False\nprice = 42000\n\nif is_vip or price >= 50000:\n    shipping_fee = 0\nelse:\n    shipping_fee = 3000",
                "VIP/비VIP × 5만원 이상/미만 네 경우의 기대 배송비를 표로 적으세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · Decision Table — 코드 전에 규칙 표 만들기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P27 · 실제 할인 규칙을 구조로 설계하기",
                "규칙이 ‘VIP 20%, VIP가 아니고 회원이며 5만원 이상 10%, 그 외 0%’라면 우선순위가 있습니다. 가장 특별한 VIP 규칙을 먼저 검사하고, 그 다음 회원 조건, 마지막 else를 둡니다.\n\n중첩 if로도 만들 수 있지만 if/elif/else로 평평하게 쓰면 우선순위가 한눈에 보입니다. 좋은 조건문은 줄 수가 적은 코드가 아니라 규칙의 순서와 예외가 명확한 코드입니다.",
                "가장 강한 예외 규칙부터 아래로 내려오며 한 가지 결과를 고릅니다.",
                "is_vip = False\nis_member = True\nprice = 60000\n\nif is_vip:\n    discount = 20\nelif is_member and price >= 50000:\n    discount = 10\nelse:\n    discount = 0",
                "VIP=True, 회원=False, 가격=1000일 때 왜 20이 되는지 추적하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 실제 할인 규칙을 구조로 설계하기"));

        out.add(Track2Content.page(
                lesson, l,
                "이론",
                "P28 · 사용자 입력을 조건에 쓰기",
                "input()으로 받은 글자는 사용자가 예상과 다르게 입력할 수 있습니다. y를 기대했는데 Y, 앞뒤 공백, 전혀 다른 글자가 들어올 수 있습니다. 따라서 실제 프로그램에서는 조건을 검사하기 전에 문자열을 정리하거나 허용 입력을 명확히 검증합니다.\n\n또 else를 무조건 ‘아니오’로 취급하지 마세요. y와 n만 허용한다면 y, n, 그 외 잘못된 입력을 세 갈래로 나누는 편이 안전합니다.",
                "사용자가 정확히 우리가 기대한 모양으로만 입력한다고 믿지 않는 것이 중요합니다.",
                "answer = input(\"계속할까요? (y/n): \").strip().lower()\n\nif answer == \"y\":\n    print(\"계속\")\nelif answer == \"n\":\n    print(\"종료\")\nelse:\n    print(\"y 또는 n을 입력하세요\")",
                "Y, n, 공백이 붙은 y, hello 네 입력이 각각 어디로 가는지 생각해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 사용자 입력을 조건에 쓰기"));

        out.add(Track2Content.page(
                lesson, l,
                "디버깅",
                "P29 · 고장 코드 1 — 문법 오류 찾기",
                "문법 오류는 Python이 코드를 정상 구조로 읽지 못하는 문제입니다. if 조건 뒤 콜론이 빠지거나, 들여쓰기가 없거나, 따옴표·괄호가 닫히지 않았을 때 자주 발생합니다.\n\n오류가 나면 무작정 전체 코드를 고치지 말고 오류가 가리키는 줄 주변에서 ① 콜론 ② 괄호·따옴표 ③ 들여쓰기 ④ if/elif/else 순서를 확인하세요. 오류 메시지는 실패 통보가 아니라 위치를 좁히는 힌트입니다.",
                "컴퓨터가 ‘이 문장을 문법대로 읽을 수 없다’고 알려주는 상황입니다.",
                "age = 20\n\n# 잘못된 예\n# if age >= 19\n# print(\"성인\")\n\n# 올바른 예\nif age >= 19:\n    print(\"성인\")",
                "콜론을 일부러 지우고 오류를 본 뒤 다시 복원해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 고장 코드 1 — 문법 오류 찾기"));

        out.add(Track2Content.page(
                lesson, l,
                "디버깅",
                "P30 · 고장 코드 2 — 논리 오류 찾기",
                "논리 오류는 코드가 실행되지만 결과가 요구사항과 다른 문제입니다. ‘19세 이상이 성인’인데 age > 19라고 쓰면 19세가 미성년자로 처리됩니다. Python 문법은 맞기 때문에 프로그램은 멈추지 않습니다.\n\n이런 오류를 잡는 가장 강력한 방법이 경계 테스트입니다. 18, 19, 20을 넣으면 정확히 기준값에서 문제가 드러납니다. 실행된다는 사실과 올바르다는 사실은 다릅니다.",
                "논리 오류는 계산기는 켜지지만 답을 잘못 계산하는 것과 비슷합니다.",
                "age = 19\n\nif age >= 19:\n    print(\"성인\")\nelse:\n    print(\"미성년자\")",
                "일부러 >로 바꾼 뒤 18/19/20 결과를 기록해 오류를 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 고장 코드 2 — 논리 오류 찾기"));

        out.add(Track2Content.page(
                lesson, l,
                "디버깅",
                "P31 · 고장 코드 3 — and와 or 혼동",
                "규칙이 ‘19세 이상이고 티켓이 있어야 입장’인데 or를 쓰면 미성년자라도 티켓만 있으면 통과할 수 있습니다. 한국어의 ‘그리고/둘 다’는 and, ‘또는/하나라도’는 or에 대응합니다.\n\n두 bool 조건이 있으면 가능한 조합은 네 가지이므로 작은 진리표를 만들어 기대 결과와 코드 결과를 비교하세요. 복합 조건 버그는 머리로만 보면 놓치기 쉽습니다.",
                "and는 둘 다 통과해야 하는 두 개의 검사대, or는 하나만 통과해도 되는 두 개의 입구입니다.",
                "age = 17\nhas_ticket = True\n\nif age >= 19 and has_ticket:\n    print(\"입장 가능\")\nelse:\n    print(\"입장 불가\")",
                "성인 여부 × 티켓 여부 4개 조합을 모두 테스트하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 고장 코드 3 — and와 or 혼동"));

        out.add(Track2Content.page(
                lesson, l,
                "실습",
                "P32 · 실습 A — 영화관 입장 판정",
                "요구사항을 코드로 바꿉니다. 19세 이상이어야 하고 티켓도 있어야 입장 가능합니다. 나이가 부족하면 ‘나이 제한’, 나이는 되지만 티켓이 없으면 ‘티켓 필요’, 둘 다 충족하면 ‘입장 가능’을 출력하세요.\n\n바로 코드를 쓰기 전에 먼저 세 경우의 우선순위를 말로 적으세요. 실패 이유를 먼저 분리하면 조건 체인이 단순해집니다.",
                "나이 제한 → 티켓 확인 → 최종 입장 순서로 생각하면 됩니다.",
                "age = 20\nhas_ticket = False\n\n# 아래에 if / elif / else를 작성하세요.",
                "반드시 age=18/ticket=True, age=19/ticket=False, age=19/ticket=True를 테스트하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 실습 A — 영화관 입장 판정"));

        out.add(Track2Content.page(
                lesson, l,
                "실습 정답·해설",
                "P33 · 실습 A 정답과 응용",
                "먼저 age < 19를 검사하면 나이가 부족한 경우를 바로 분리할 수 있습니다. 다음 elif not has_ticket까지 왔다는 것은 이미 19세 이상이라는 뜻이므로 티켓 여부만 보면 됩니다. 마지막 else까지 왔다면 ‘성인이고 티켓 있음’ 두 조건이 모두 만족된 상태입니다.\n\n앞 단계에서 이미 확인한 사실을 뒤에서 반복하지 않는 것이 깔끔한 조건 체인의 핵심입니다.",
                "elif까지 내려왔다는 사실 자체가 이전 조건이 False였다는 정보를 줍니다.",
                "age = 20\nhas_ticket = False\n\nif age < 19:\n    print(\"나이 제한\")\nelif not has_ticket:\n    print(\"티켓 필요\")\nelse:\n    print(\"입장 가능\")",
                "응용: VIP이면 티켓이 없어도 입장 가능하도록 우선순위를 설계해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 실습 A 정답과 응용"));

        out.add(Track2Content.page(
                lesson, l,
                "실습",
                "P34 · 실습 B — 배송비 규칙 설계",
                "규칙은 VIP이면 무료배송, VIP가 아니어도 5만원 이상이면 무료배송, 나머지는 3000원입니다. 먼저 VIP True/False와 5만원 이상 True/False를 조합한 4칸 Decision Table을 완성한 뒤 코드를 쓰세요.\n\n표를 만든 뒤 보면 무료배송은 두 조건 중 하나만 만족해도 되므로 or가 자연스럽다는 것을 확인할 수 있습니다.",
                "코드보다 먼저 네 가지 경우의 정답을 정하면 연산자 선택이 쉬워집니다.",
                "is_vip = False\nprice = 42000\n\n# shipping_fee를 조건문으로 결정하세요.",
                "True/10000, False/50000, False/49999 세 입력을 반드시 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 실습 B — 배송비 규칙 설계"));

        out.add(Track2Content.page(
                lesson, l,
                "실습 정답·해설",
                "P35 · 실습 B 정답과 두 방식 비교",
                "무료배송은 VIP이거나 5만원 이상이라는 두 조건 중 하나만 맞아도 되므로 or를 사용합니다. 각각 별도 if/elif로 작성해도 동작할 수 있지만 결과가 같은 규칙을 반복하게 됩니다.\n\n조건을 합쳤을 때 의미가 더 직접적이고 중복이 줄어든다면 합친 버전이 읽기 쉽습니다. 단, 규칙이 달라질 가능성이 있으면 분리된 구조가 더 명확한 경우도 있습니다.",
                "같은 결과를 만드는 두 길이라면 하나의 조건으로 묶을 수 있는지 봅니다.",
                "is_vip = False\nprice = 42000\n\nif is_vip or price >= 50000:\n    shipping_fee = 0\nelse:\n    shipping_fee = 3000\n\nprint(shipping_fee)",
                "49999와 50000을 넣어 경계가 정확히 바뀌는지 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 실습 B 정답과 두 방식 비교"));

        out.add(Track2Content.page(
                lesson, l,
                "실습",
                "P36 · 실습 C — 등급 판정과 경계값",
                "90 이상 A, 80 이상 B, 70 이상 C, 그 미만 D 규칙을 if/elif/else로 작성합니다. 겹치는 범위이므로 높은 기준부터 내려오는 순서가 중요합니다.\n\n작성 후 69, 70, 79, 80, 89, 90, 100을 테스트하세요. 각 등급 경계의 바로 아래와 정확한 기준값을 확인하면 비교 연산자 오류를 빠르게 잡을 수 있습니다.",
                "90 → 80 → 70처럼 가장 높은 문턱부터 내려오세요.",
                "score = 80\n\n# grade를 A/B/C/D 중 하나로 정하세요.",
                "69,70,79,80,89,90,100의 기대 등급을 먼저 적고 실행 결과와 비교하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 실습 C — 등급 판정과 경계값"));

        out.add(Track2Content.page(
                lesson, l,
                "실습 정답·해설",
                "P37 · 실습 C 정답과 조건 순서",
                "score >= 90부터 검사하고, 실패했을 때만 80, 다시 실패했을 때만 70을 검사합니다. 따라서 score >= 80까지 왔다면 이미 90 미만이라는 정보가 포함되어 있습니다.\n\n반대로 70 이상을 먼저 쓰면 90점도 첫 조건에서 잡혀 C가 되어 버립니다. 겹치는 범위의 elif 체인에서는 더 엄격한 조건을 먼저 두는 이유입니다.",
                "앞에서 넓게 잡아버리면 뒤의 정교한 기준까지 도달하지 못합니다.",
                "score = 80\n\nif score >= 90:\n    grade = \"A\"\nelif score >= 80:\n    grade = \"B\"\nelif score >= 70:\n    grade = \"C\"\nelse:\n    grade = \"D\"\n\nprint(grade)",
                "조건 순서를 70→80→90으로 바꿔 95점이 왜 잘못 분류되는지 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 실습 C 정답과 조건 순서"));

        out.add(Track2Content.page(
                lesson, l,
                "확인 문제",
                "P38 · 확인문제 1 — 실행 결과 예측",
                "코드를 실행하기 전에 결과를 예측하세요. ① age=19에서 age > 19 ② score=85에서 90/80/else 체인 ③ 회원=True, price=30000에서 and 조건 ④ False or True의 결과를 각각 판단합니다.\n\n답을 바로 찍지 말고 각 비교식을 먼저 True/False로 바꾼 뒤 어떤 블록으로 이동하는지 한 단계씩 쓰세요.",
                "정답보다 ‘True/False를 거쳐 경로를 찾는 과정’이 더 중요합니다.",
                "age = 19\nscore = 85\nis_member = True\nprice = 30000\n\nprint(age > 19)\nprint(score >= 90)\nprint(score >= 80)\nprint(is_member and price >= 50000)",
                "종이에 결과를 먼저 적은 뒤 실행해 비교하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 확인문제 1 — 실행 결과 예측"));

        out.add(Track2Content.page(
                lesson, l,
                "문제 정답·해설",
                "P39 · 확인문제 1 정답과 이유",
                "age > 19는 19 자체를 포함하지 않으므로 False입니다. score=85는 90 이상이 False이고 80 이상이 True이므로 B 경로입니다. is_member는 True지만 price >= 50000은 False이므로 True and False는 False입니다. False or True는 하나가 True이므로 True입니다.\n\n이 문제의 핵심은 복합 조건을 한 번에 감으로 읽지 않고 작은 bool 값으로 환산하는 것입니다.",
                "복잡해 보여도 True와 False 두 값으로 바꾸면 계산이 단순해집니다.",
                "print(19 > 19)             # False\nprint(85 >= 90)            # False\nprint(85 >= 80)            # True\nprint(True and False)       # False\nprint(False or True)        # True",
                "각 결과를 말로 설명할 수 있는지 확인하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 확인문제 1 정답과 이유"));

        out.add(Track2Content.page(
                lesson, l,
                "확인 문제",
                "P40 · 확인문제 2 — 틀린 코드 고치기",
                "다음 네 종류의 오류를 직접 고칩니다. ① ‘19세 이상’인데 >를 사용한 경계 오류 ② ‘회원이고 5만원 이상’인데 or를 사용한 논리 연산 오류 ③ 70→80→90 순서로 적은 등급 오류 ④ y가 아니면 무조건 n으로 처리하는 입력 검증 오류입니다.\n\n각 코드를 고친 뒤 ‘무엇을 바꿨는가’보다 ‘왜 원래 코드가 틀렸는가’를 한 문장으로 설명하세요.",
                "수정 이유를 설명할 수 있어야 같은 종류의 버그를 다음에 스스로 잡을 수 있습니다.",
                "# 1\nif age > 19:\n    print(\"성인\")\n\n# 2\nif is_member or price >= 50000:\n    print(\"할인\")\n\n# 3\nif score >= 70:\n    grade = \"C\"\nelif score >= 80:\n    grade = \"B\"",
                "네 문제를 각각 수정하고 경계값/조합 테스트를 하나 이상 붙이세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 확인문제 2 — 틀린 코드 고치기"));

        out.add(Track2Content.page(
                lesson, l,
                "문제 정답·해설",
                "P41 · 확인문제 2 정답과 수정 이유",
                "① 19를 포함해야 하므로 age >= 19. ② 두 조건을 모두 만족해야 하므로 and. ③ 등급은 90→80→70처럼 높은 기준부터. ④ y/n 외 입력을 구분하려면 if y, elif n, else 잘못된 입력으로 나눕니다.\n\n이 네 오류는 조건문에서 매우 자주 반복됩니다. 비교 연산자 경계, 논리 연산자 의미, 조건 순서, 예상하지 못한 입력을 각각 별도의 체크 항목으로 기억하세요.",
                "조건문 검토 체크리스트 네 개: 경계·and/or·순서·예상 밖 입력입니다.",
                "if age >= 19:\n    print(\"성인\")\n\nif is_member and price >= 50000:\n    print(\"할인\")\n\nif score >= 90:\n    grade = \"A\"\nelif score >= 80:\n    grade = \"B\"\nelif score >= 70:\n    grade = \"C\"",
                "자기 코드 하나를 골라 네 체크 항목으로 다시 검토하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 확인문제 2 정답과 수정 이유"));

        out.add(Track2Content.page(
                lesson, l,
                "미니 프로젝트",
                "P42 · 주문 할인 판정기",
                "상품 가격과 고객 상태로 할인율을 결정합니다. 가격이 0 이하이면 잘못된 가격, VIP면 20%, VIP가 아니고 회원이며 10만원 이상이면 15%, 회원이며 5만원 이상이면 10%, 비회원이며 20만원 이상이면 5%, 그 외 0%입니다.\n\n바로 if부터 쓰지 말고 ‘잘못된 입력 → 가장 강한 VIP 규칙 → 회원 고액 → 회원 일반 → 비회원 고액 → 그 외’ 순서로 우선순위를 먼저 적으세요. 이 프로젝트는 지금까지 배운 경계값, and/not, elif 순서를 한 번에 사용합니다.",
                "복잡한 조건문은 문법보다 ‘어떤 규칙을 먼저 적용할지’ 결정하는 일이 먼저입니다.",
                "price = 120000\nis_member = True\nis_vip = False\n\ndiscount = 0\n\n# 여기에 조건문을 작성하세요.\n\nprint(\"할인율:\", discount)",
                "0원, VIP, 회원 10만원, 회원 5만원, 비회원 20만원, 아무 조건 없음 최소 6가지를 테스트하세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 주문 할인 판정기"));

        out.add(Track2Content.page(
                lesson, l,
                "최종 정리",
                "P43 · 프로젝트 정답 + 테스트 + Chapter 1 마무리",
                "정답은 우선순위대로 elif 체인을 구성합니다. 가격 오류를 먼저 걸러낸 뒤 VIP, 회원 10만원, 회원 5만원, 비회원 20만원, 그 외를 처리합니다. 테스트는 0, VIP 저가, 100000 회원, 50000 회원, 200000 비회원, 49999 회원처럼 규칙 경계와 조합을 포함해야 합니다.\n\n이 챕터를 끝냈다면 if가 True/False로 분기한다는 원리, =와 == 차이, >와 >= 경계, if/elif/else의 첫 True 선택, 여러 독립 if의 차이, and/or/not, 중첩 단순화, Decision Table, 문법 오류와 논리 오류를 자기 말로 설명할 수 있어야 합니다. 이 정도면 조건문을 ‘본 적 있는 초보’가 아니라 조건 로직을 설계하고 테스트하는 초중급 진입 단계입니다.",
                "마지막 목표는 코드를 외우는 것이 아니라 규칙을 설계하고 경계값으로 스스로 검증하는 것입니다.",
                "price = 120000\nis_member = True\nis_vip = False\n\ndiscount = 0\n\nif price <= 0:\n    print(\"잘못된 가격\")\nelif is_vip:\n    discount = 20\nelif is_member and price >= 100000:\n    discount = 15\nelif is_member and price >= 50000:\n    discount = 10\nelif not is_member and price >= 200000:\n    discount = 5\nelse:\n    discount = 0\n\nprint(\"할인율:\", discount)",
                "코드 없이 ① 19세 이상 조건 ② and/or 차이 ③ 높은 점수부터 검사하는 이유 ④ 경계값 테스트 이유를 설명해 보세요.",
                null,
                null,
                "Python · if · 조건문 · True/False · 프로젝트 정답 + 테스트 + Chapter 1 마무리"));

    }
}
