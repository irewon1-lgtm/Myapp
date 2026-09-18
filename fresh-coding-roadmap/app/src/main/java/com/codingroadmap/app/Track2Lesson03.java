package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson03 {
    private Track2Lesson03() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 3;
        final String l = "함수로 코드를 묶고 다시 사용하기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 함수·정의·호출",
                "함수(function)\n"
                        + "하나의 목적을 수행하는 코드 묶음에 이름을 붙인 것입니다. 같은 계산이나 처리를 여러 곳에서 다시 사용하고 싶을 때 함수를 만들면 코드를 복사하지 않아도 됩니다. print, input, len처럼 지금까지 사용한 기능들도 함수의 예입니다.\n\n"
                        + "함수 정의(function definition)\n"
                        + "새 함수를 만드는 코드입니다. Python에서는 def로 시작합니다. def greet():라고 적고 아래에 들여쓴 코드가 함수 본문이 됩니다. 함수를 정의했다고 바로 실행되는 것은 아니고, 나중에 이름으로 호출해야 합니다.\n\n"
                        + "def\n"
                        + "‘define’에서 온 Python 키워드로 함수를 정의할 때 사용합니다. def 뒤에는 함수 이름과 괄호, 콜론이 옵니다. 예: def say_hello():. 변수 이름처럼 함수 이름도 역할이 보이게 짓는 것이 좋습니다.\n\n"
                        + "호출(call / function call)\n"
                        + "이미 정의된 함수를 실제로 실행하는 것입니다. greet()처럼 함수 이름 뒤에 괄호를 붙이면 호출됩니다. 같은 함수를 여러 번 호출하면 함수 본문도 그때마다 다시 실행됩니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — 매개변수·인자·입력값",
                "매개변수(parameter)\n"
                        + "함수를 정의할 때 외부에서 값을 받을 자리를 나타내는 이름입니다. def greet(name):에서 name이 parameter입니다. 함수 안에서는 일반 변수처럼 사용할 수 있으며 호출할 때 전달된 값이 연결됩니다.\n\n"
                        + "인자(argument)\n"
                        + "함수를 호출하면서 실제로 전달하는 값입니다. greet(\"민수\")에서 \"민수\"가 argument입니다. parameter는 함수 설명서에 적힌 ‘자리 이름’, argument는 실제 호출 때 그 자리에 넣는 ‘값’이라고 구분하면 쉽습니다.\n\n"
                        + "위치 인자(positional argument)\n"
                        + "전달한 순서에 따라 parameter에 연결되는 값입니다. def add(a, b):를 add(10, 20)으로 호출하면 a는 10, b는 20을 받습니다. 순서가 의미를 가지므로 서로 다른 역할의 값은 위치를 주의해야 합니다.\n\n"
                        + "기본값(default value / default parameter)\n"
                        + "argument를 생략했을 때 사용할 값을 함수 정의에 미리 정할 수 있습니다. def greet(name, prefix=\"안녕하세요\")처럼 작성합니다. 기본값이 있는 parameter는 선택 입력처럼 사용할 수 있습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — return·반환값·None",
                "return\n"
                        + "함수가 계산한 결과를 호출한 곳으로 돌려보내는 Python 키워드입니다. return을 만나면 그 함수 실행은 그 자리에서 끝나고 값이 바깥으로 전달됩니다. 계산 함수는 print보다 return을 사용하면 결과를 다른 계산에 다시 이용하기 쉽습니다.\n\n"
                        + "반환값(return value)\n"
                        + "함수가 호출한 곳으로 돌려주는 결과입니다. total = add(2, 3)에서 add 함수가 5를 return하면 total에 5가 저장됩니다. input()이 사용자가 입력한 문자열을 돌려주는 것도 반환값의 예입니다.\n\n"
                        + "None\n"
                        + "Python에서 ‘특별한 값이 없음’을 나타내는 값입니다. return을 쓰지 않은 함수도 실제로는 None을 반환합니다. print만 하고 결과를 return하지 않은 함수를 변수에 저장했을 때 None이 보일 수 있는 이유입니다.\n\n"
                        + "출력(print)과 반환(return)의 차이\n"
                        + "print는 사람이 화면에서 보도록 보여 주는 동작이고, return은 프로그램의 다른 코드가 그 값을 계속 사용하도록 전달하는 동작입니다. 둘은 목적이 다르며 계산 결과를 재사용하려면 보통 return이 필요합니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — 지역 변수·범위·재사용·책임",
                "지역 변수(local variable)\n"
                        + "함수 안에서 만든 변수로, 기본적으로 그 함수 안에서 사용됩니다. 함수가 자기 계산에 필요한 중간 값을 독립적으로 관리할 수 있게 해 줍니다. 바깥 코드와 이름이 우연히 같아도 서로 다른 범위일 수 있습니다.\n\n"
                        + "범위(scope)\n"
                        + "변수 이름을 사용할 수 있는 영역입니다. 함수 안에서 만든 local variable은 함수 밖에서 바로 사용할 수 없습니다. scope를 알면 ‘왜 이 변수를 찾을 수 없지?’ 같은 NameError를 이해하는 데 도움이 됩니다.\n\n"
                        + "재사용(reuse)\n"
                        + "한 번 만든 코드를 여러 곳에서 다시 사용하는 것입니다. 함수의 중요한 장점입니다. 할인 계산 규칙을 함수 하나에 두면 여러 주문에서 같은 규칙을 호출할 수 있고, 규칙이 바뀌어도 함수 한 곳만 고치면 됩니다.\n\n"
                        + "단일 책임(single responsibility)\n"
                        + "함수 하나가 너무 많은 일을 하기보다 한 가지 분명한 목적을 갖게 만드는 설계 생각입니다. 예를 들어 ‘가격 계산’, ‘출력’, ‘파일 저장’을 각각 나누면 읽고 테스트하고 고치기 쉬워집니다."));

        out.add(Track2Content.page(lesson, l, "시작", "함수는 자주 쓰는 절차에 이름을 붙이는 방법이다",
                "프로그램이 길어지면 같은 계산이나 출력 형식을 여러 번 사용하게 됩니다. 코드를 복사해서 붙여 넣으면 처음에는 빠르지만, 규칙이 바뀔 때 모든 복사본을 찾아 고쳐야 하고 하나를 빠뜨릴 수도 있습니다. 함수는 관련 코드를 하나의 이름 아래 묶어 필요한 곳에서 호출하게 해 줍니다.\n\n"
                        + "함수를 읽을 때는 먼저 ‘이 함수가 어떤 입력을 받고 어떤 결과를 만드는가?’를 찾으세요. 내부 구현을 한 줄씩 보기 전에 입출력 계약을 이해하면 긴 함수도 훨씬 쉽게 읽힙니다. 함수 이름도 calculate_total처럼 목적을 드러내면 코드 자체가 설명서 역할을 합니다.",
                "자주 만드는 요리에 레시피 이름을 붙이는 것과 비슷합니다. 매번 모든 조리 단계를 다시 설명하지 않고 ‘김치볶음밥 만들기’를 호출하면 정해 둔 순서가 실행됩니다.",
                null, null, null, null,
                "function · def · call · reuse"));

        out.add(Track2Content.page(lesson, l, "이론", "parameter는 받을 자리, argument는 실제로 넣는 값이다",
                "함수마다 똑같은 결과만 만든다면 활용 범위가 좁습니다. parameter를 두면 호출할 때마다 다른 값을 받아 같은 규칙을 적용할 수 있습니다. def greet(name):에서 name은 함수가 받을 값의 자리입니다. greet(\"민수\")와 greet(\"영희\")처럼 호출하면 같은 본문이 서로 다른 값으로 실행됩니다.\n\n"
                        + "parameter가 여러 개면 기본적으로 위치 순서대로 argument가 연결됩니다. calculate_total(price, count)를 calculate_total(3000, 4)로 호출하면 price=3000, count=4입니다. 함수 이름과 parameter 이름을 읽으면 어떤 값을 어떤 순서로 넣어야 하는지 이해할 수 있어야 합니다.",
                "빈칸이 있는 신청서를 생각하면 됩니다. name이라는 빈칸이 parameter이고, 실제로 적는 ‘민수’가 argument입니다.",
                "def greet(name):\n    print(f\"{name}님, 안녕하세요!\")\n\ngreet(\"민수\")\ngreet(\"영희\")",
                null, null, null,
                "parameter · argument · positional argument · function body"));

        out.add(Track2Content.page(lesson, l, "이론", "계산 결과를 다시 쓰려면 print보다 return을 구분한다",
                "함수 안에서 print(total)을 실행하면 사람 눈에는 결과가 보이지만, 그 숫자가 호출한 코드에 전달되는 것은 아닙니다. 계산 결과를 변수에 저장하거나 다른 계산에 이어 쓰려면 return total처럼 반환해야 합니다. 반환값은 함수 호출식 자체의 값이 됩니다.\n\n"
                        + "예를 들어 result = multiply(3, 4)에서 multiply가 12를 return하면 함수 호출 부분이 12라는 값으로 바뀐 것처럼 생각할 수 있습니다. 그래서 result에 12가 저장되고 result + 10 같은 계산도 가능합니다. return을 만나면 함수가 즉시 끝난다는 점도 기억하세요.",
                "print는 전광판에 숫자를 보여 주는 것이고 return은 계산한 숫자를 다음 사람 손에 건네주는 것입니다. 보여 주기와 전달하기는 다릅니다.",
                "def multiply(a, b):\n    result = a * b\n    return result\n\nvalue = multiply(3, 4)\nprint(value)\nprint(value + 10)",
                null, null, null,
                "return · return value · None · expression"));

        out.add(Track2Content.page(lesson, l, "예제", "가격과 개수를 받아 총액을 반환하는 함수",
                "아래 calculate_total 함수는 가격과 개수를 입력으로 받고 총액을 반환합니다. 함수 안에서 계산 규칙을 한 곳에 모아 두었기 때문에 서로 다른 상품에도 같은 함수를 재사용할 수 있습니다. 반환된 값은 apple_total, banana_total처럼 다른 변수에 저장해 이후 계산에 다시 사용합니다.\n\n"
                        + "함수는 ‘입력 → 처리 → 반환’이라는 작은 프로그램처럼 볼 수 있습니다. TRACK 01에서 배운 입력·처리·출력 구조가 함수 내부에도 그대로 적용됩니다.",
                "계산기 버튼 하나를 직접 만든다고 생각하세요. 두 숫자를 넣으면 정해 둔 규칙으로 계산하고 결과 하나를 돌려줍니다.",
                "def calculate_total(price, count):\n    total = price * count\n    return total\n\napple_total = calculate_total(1500, 4)\nbanana_total = calculate_total(2000, 2)\n\nprint(apple_total)\nprint(banana_total)\nprint(apple_total + banana_total)",
                null, null, null,
                "calculate · parameter · return · reuse"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — 인사 함수를 만들고 여러 값으로 호출하기",
                "먼저 가장 작은 함수부터 직접 정의합니다. 같은 print 문을 여러 번 쓰는 대신 name을 parameter로 받아 호출할 때마다 다른 사람에게 인사하게 만듭니다. 그 다음 prefix에 기본값을 넣어 선택적으로 인사말을 바꿔 봅니다.\n\n"
                        + "실습 중에는 함수 정의만 하고 호출을 빼먹었을 때 아무 출력이 없는 이유도 확인하세요. 정의는 레시피를 적는 것이고 호출이 실제 실행입니다.",
                "def 아래의 코드가 있다고 바로 실행되는 것이 아닙니다. greet(...)처럼 괄호를 붙여 호출해야 함수 본문이 움직입니다.",
                "def greet(name):\n    print(f\"{name}님, 반갑습니다.\")\n\n# 아래에서 여러 번 호출해 보세요",
                "1. 자기 이름과 가족 이름 두 개로 greet 호출하기\n2. greet를 세 번 호출하면 본문이 몇 번 실행되는지 확인하기\n3. def greet(name, prefix=\"안녕하세요\") 형태로 바꾸기\n4. prefix를 생략한 호출과 직접 넣은 호출의 차이 확인하기",
                null, null,
                "def · call · parameter · argument · default value"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — 인사 함수를 만들고 여러 값으로 호출하기",
                "예시 답안\n"
                        + "함수는 한 번 정의하고 여러 번 호출할 수 있습니다. prefix에 기본값을 지정하면 두 번째 argument를 생략했을 때 ‘안녕하세요’를 사용하고, 직접 전달하면 그 값으로 바뀝니다. 함수 정의만 실행하면 Python은 함수가 있다는 사실만 준비하고 본문은 아직 실행하지 않습니다.\n\n"
                        + "기본값 parameter는 선택 옵션처럼 사용할 수 있습니다. 하지만 처음에는 복잡한 인자 문법보다 ‘입력값을 받아 같은 규칙을 재사용한다’는 핵심을 먼저 익히면 충분합니다.",
                "def greet(name, prefix=\"안녕하세요\"):\n    print(f\"{prefix}, {name}님!\")\n\ngreet(\"민수\")\ngreet(\"영희\", \"반갑습니다\")\ngreet(\"지수\")"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — 할인 계산을 return하는 함수로 만들기",
                "이번에는 출력 함수가 아니라 값을 반환하는 계산 함수를 만듭니다. price와 discount_rate를 받아 할인 후 가격을 계산하고 return하세요. 반환값을 변수에 저장한 뒤 배송비를 더해 최종 금액을 계산합니다.\n\n"
                        + "함수 안에서 print만 사용한 버전과 return을 사용한 버전을 비교해 보면 둘의 차이가 분명해집니다. 계산 함수는 결과를 재사용할 수 있게 return하는 방식이 보통 더 유연합니다.",
                "함수가 가격을 화면에 보여 주는 데서 끝나면 다음 계산이 어렵습니다. 값을 return하면 배송비 더하기, 세금 계산 등 다음 단계로 연결할 수 있습니다.",
                "def discounted_price(price, discount_rate):\n    # 할인 후 가격을 계산하고 return하세요\n    pass",
                "1. price * (1 - discount_rate)를 계산해 반환하기\n2. discounted_price(10000, 0.1)의 결과가 9000인지 확인하기\n3. 반환값을 saved_price에 저장하고 배송비 3000원 더하기\n4. 함수 안에서 print만 했을 때 saved_price에 무엇이 들어가는지 비교하기",
                null, null,
                "return · return value · discount_rate · reuse"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — 할인 계산을 return하는 함수로 만들기",
                "예시 답안\n"
                        + "10% 할인은 원래 가격의 90%를 내는 것이므로 price * (1 - 0.1)로 계산할 수 있습니다. 함수가 이 값을 return하면 호출식 전체가 그 결과값으로 사용됩니다. 그래서 saved_price에 저장하고 다시 배송비를 더할 수 있습니다.\n\n"
                        + "반대로 함수가 print만 하고 return하지 않으면 호출 결과는 None입니다. 화면에 9000이 보였더라도 program이 9000을 반환받은 것은 아닙니다. 이 구분은 이후 API, 파일 처리, 데이터 가공 함수를 만들 때 계속 중요합니다.",
                "def discounted_price(price, discount_rate):\n    result = price * (1 - discount_rate)\n    return result\n\nsaved_price = discounted_price(10000, 0.1)\nfinal_price = saved_price + 3000\n\nprint(saved_price)\nprint(final_price)"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — 함수의 입력과 반환 흐름 읽기",
                "함수 정의와 호출을 구분하고, parameter와 argument, print와 return의 역할을 정확히 구별할 수 있는지 확인합니다. 답을 고르기 전에 함수 호출 한 번을 ‘argument 전달 → 본문 실행 → return 값 전달’ 순서로 따라가 보세요.",
                "함수 안으로 들어갔다가 return을 만나 다시 호출한 자리로 돌아오는 화살표를 그려 보면 이해가 쉽습니다.",
                null, null,
                "1. def add(a, b):에서 a와 b를 무엇이라고 하나요?\n2. add(2, 3)에서 2와 3은 무엇인가요?\n3. 함수에서 계산 결과를 다른 코드가 다시 사용하게 하려면 주로 print와 return 중 무엇을 사용하나요?\n4. return이 없는 함수가 기본적으로 반환하는 특별한 값은 무엇인가요?",
                "1. parameter(매개변수)\n2. argument(인자)\n3. return\n4. None",
                "function · parameter · argument · return · None"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — 함수의 입력과 반환 흐름 읽기",
                "1. parameter\n2. argument\n3. return\n4. None",
                "parameter는 함수 정의에 적힌 입력 자리 이름이고 argument는 호출할 때 넣는 실제 값입니다. return은 계산한 값을 호출한 곳으로 전달하므로 다른 변수에 저장하거나 다음 계산에 사용할 수 있습니다. return을 명시하지 않은 Python 함수도 호출이 끝나면 None이라는 특별한 값을 반환합니다.",
                "def square(number): return number * number 함수를 만들고 square(5), square(10)을 호출해 보세요. 반환값 두 개를 더해 새로운 계산에 사용할 수 있는지도 확인하세요.",
                "함수를 정의했지만 호출하지 않는 것, parameter 순서를 잘못 넣는 것, print가 return과 같은 역할을 한다고 착각하는 것이 흔한 실수입니다."));

        out.add(Track2Content.page(lesson, l, "정리", "챕터 3 정리 — 함수는 입력을 받아 한 일을 하고 결과를 돌려준다",
                "함수는 관련 코드를 이름 아래 묶어 재사용하는 도구입니다. def로 정의하고 괄호를 붙여 호출합니다. parameter는 받을 자리, argument는 실제 전달값입니다. 계산 결과를 프로그램이 다시 사용해야 한다면 return으로 반환합니다.\n\n"
                        + "좋은 함수는 한 가지 목적이 분명하고 이름만 봐도 역할을 예상할 수 있습니다. 함수 안의 local variable과 scope 개념도 함께 기억해 두세요. 다음 챕터에서는 한두 개의 값이 아니라 여러 값을 묶어 저장하고 반복 처리할 수 있는 리스트와 딕셔너리를 배웁니다.",
                "함수를 ‘작은 기계’라고 생각하세요. 입력을 넣고, 안에서 정해진 일을 하고, 필요하면 결과를 return합니다.",
                null, null, null, null,
                "function · def · call · parameter · argument · return · local variable · scope · reuse"));
    }
}
