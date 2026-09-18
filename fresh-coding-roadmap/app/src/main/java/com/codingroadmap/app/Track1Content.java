package com.codingroadmap.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Track1Content {
    private Track1Content() {}

    static final class Page {
        final int lessonNumber;
        final String lessonTitle;
        final String kind;
        final String title;
        final String body;
        final String code;
        final String practice;
        final String question;
        final String answer;
        final String keywords;

        Page(
                int lessonNumber,
                String lessonTitle,
                String kind,
                String title,
                String body,
                String code,
                String practice,
                String question,
                String answer,
                String keywords
        ) {
            this.lessonNumber = lessonNumber;
            this.lessonTitle = lessonTitle;
            this.kind = kind;
            this.title = title;
            this.body = body;
            this.code = code;
            this.practice = practice;
            this.question = question;
            this.answer = answer;
            this.keywords = keywords;
        }

        boolean hasCode() { return code != null && !code.isEmpty(); }
        boolean hasPractice() { return practice != null && !practice.isEmpty(); }
        boolean hasQuestion() { return question != null && !question.isEmpty(); }
        boolean hasKeywords() { return keywords != null && !keywords.isEmpty(); }
    }

    private static Page p(
            int lesson,
            String lessonTitle,
            String kind,
            String title,
            String body,
            String code,
            String practice,
            String question,
            String answer,
            String keywords
    ) {
        return new Page(lesson, lessonTitle, kind, title, body, code, practice, question, answer, keywords);
    }

    static List<Page> pages() {
        List<Page> out = new ArrayList<>();

        // LESSON 01
        final String l1 = "코딩이 무엇인지 알고 첫 코드를 실행하기";
        out.add(p(1, l1, "시작", "코딩은 컴퓨터에게 일을 시키는 방법이다",
                "컴퓨터는 사람의 말을 그대로 이해하지 못합니다. 그래서 사람이 원하는 일을 일정한 규칙으로 적어 줘야 합니다. 그 규칙에 맞춰 적은 명령들의 모음을 ‘프로그램’이라고 하고, 그 명령을 작성하는 일을 ‘프로그래밍’ 또는 ‘코딩’이라고 부릅니다.\n\n처음부터 어려운 원리를 외울 필요는 없습니다. 오늘의 핵심은 ‘내가 적은 코드가 실제 결과를 만든다’는 경험입니다.",
                null, null, null, null,
                "프로그램(program) · 프로그래밍(programming) · 코드(code)"));
        out.add(p(1, l1, "이론", "소스 코드와 프로그래밍 언어",
                "사람이 읽을 수 있게 작성한 프로그램의 원문을 ‘소스 코드(source code)’라고 합니다. Python, JavaScript, Java 같은 것은 소스 코드를 적을 때 사용하는 ‘프로그래밍 언어’입니다.\n\n언어마다 문법은 다르지만 목적은 같습니다. 사람이 의도한 일을 컴퓨터가 실행할 수 있는 형태로 표현하는 것입니다. 이 앱에서는 처음 배우기 쉬운 Python 문법으로 개념을 익힙니다.",
                null, null, null, null,
                "소스 코드(source code) · 프로그래밍 언어(programming language) · Python"));
        out.add(p(1, l1, "이론", "Python 코드는 누가 읽고 실행할까?",
                "Python 코드를 실행하면 Python 실행 환경이 코드를 읽고 실제 동작으로 바꿉니다. 초보 단계에서는 이것을 ‘Python이 내 코드를 읽어서 실행한다’고 이해하면 충분합니다. 이런 방식으로 코드를 읽어 실행하는 프로그램을 흔히 ‘인터프리터(interpreter)’라고 부릅니다.\n\n인터넷 글에서 interpreter, runtime, execute 같은 단어를 보면 ‘코드를 실제로 돌아가게 만드는 쪽’이라는 큰 그림부터 떠올리면 됩니다.",
                null, null, null, null,
                "인터프리터(interpreter) · 실행(execute) · 실행 환경(runtime)"));
        out.add(p(1, l1, "예제", "첫 명령: print로 결과 보여주기",
                "print는 괄호 안의 값을 화면에 보여주는 Python 기능입니다. 따옴표 안의 글자는 그대로 출력하고, 숫자 계산식은 먼저 계산한 뒤 결과를 출력합니다.",
                "print(\"안녕, 코딩!\")\nprint(2 + 3)",
                null, null, null,
                "print · 출력(output) · 문자열(string)"));
        out.add(p(1, l1, "실습", "실습 A — 코드 한 글자 바꾸면 결과도 바뀐다",
                "코딩은 눈으로 읽는 것보다 직접 바꾸고 실행하면서 배우는 편이 빠릅니다. 아래 과제는 정답을 외우는 것이 아니라 ‘코드 변경 → 실행 → 결과 확인’의 흐름을 익히는 연습입니다.",
                "print(\"안녕, 코딩!\")\nprint(2 + 3)",
                "1. ‘안녕, 코딩!’을 자기 이름으로 바꿔 보기\n2. 2 + 3을 10 + 7로 바꾸기\n3. print(100) 한 줄을 새로 추가하기\n4. 각 변경 뒤에 결과가 어떻게 달라졌는지 말로 설명하기",
                null, null,
                "수정(edit) · 실행(run) · 결과(result)"));
        out.add(p(1, l1, "실습", "실습 B — 예상하고 실행하기",
                "좋은 습관은 실행 버튼을 누르기 전에 결과를 먼저 예상하는 것입니다. 예상과 실제가 같으면 이해한 것이고, 다르면 왜 다른지 찾아보면 됩니다.",
                "print(10 - 4)\nprint(3 * 5)\nprint(\"3 + 5\")",
                "1. 실행 전에 세 줄의 결과를 종이에 적기\n2. 실제 실행 결과와 비교하기\n3. 마지막 줄의 따옴표를 지우면 무엇이 달라질지 예상하기",
                null, null,
                "예측(prediction) · 계산(expression) · 따옴표(quote)"));
        out.add(p(1, l1, "확인", "확인 문제 — 코딩의 큰 그림",
                "아래 세 문제는 용어를 외우는 시험이 아니라, 앞으로 인터넷 글을 읽을 때 필요한 최소한의 단어를 확인하는 문제입니다.",
                null, null,
                "1. 사람이 읽을 수 있게 작성한 프로그램 원문을 무엇이라고 할까요?\n2. Python 같은 것은 무엇의 한 종류일까요?\n3. print(2 + 3)의 결과는 무엇일까요?",
                "1. 소스 코드(source code)\n2. 프로그래밍 언어(programming language)\n3. 5",
                "source code · programming language · interpreter · output"));
        out.add(p(1, l1, "정리", "챕터 1 정리 — 지금 알아야 할 것",
                "코딩은 컴퓨터에게 할 일을 명령으로 적는 일입니다. 그 원문이 소스 코드이고, Python은 그 코드를 적는 언어 중 하나입니다. 코드는 실행되어야 실제 결과가 생깁니다.\n\n여기까지 이해했다면 컴파일러·CPU·메모리 같은 깊은 내용은 아직 몰라도 됩니다. 다음에는 여러 줄의 코드가 어떤 순서로 움직이는지 배웁니다.",
                null, null, null, null,
                "프로그램 · 소스 코드 · Python · interpreter · run · output"));

        // LESSON 02
        final String l2 = "코드가 위에서 아래로 실행되는 흐름 이해하기";
        out.add(p(2, l2, "시작", "여러 줄의 코드는 ‘순서’가 중요하다",
                "프로그램은 여러 명령이 모인 것입니다. Python은 특별한 지시가 없다면 위에 적힌 줄부터 아래 줄까지 차례로 실행합니다. 이 실행 순서를 이해하면 ‘왜 이 결과가 먼저 나오지?’ 같은 혼란이 크게 줄어듭니다.",
                null, null, null, null,
                "실행 순서(execution order) · 흐름(flow)"));
        out.add(p(2, l2, "이론", "문장(statement)과 표현식(expression)",
                "프로그래밍 글에서 자주 만나는 두 단어가 있습니다. ‘문장(statement)’은 컴퓨터에게 어떤 일을 하라고 적은 한 단위의 명령이고, ‘표현식(expression)’은 계산되어 하나의 값이 되는 부분입니다.\n\n예를 들어 print(2 + 3) 전체는 실행할 명령이고, 그 안의 2 + 3은 계산되어 5라는 값이 되는 표현식입니다.",
                null, null, null, null,
                "문장(statement) · 표현식(expression) · 값(value)"));
        out.add(p(2, l2, "이론", "주석(comment)은 컴퓨터가 아닌 사람을 위한 메모",
                "코드에 #을 쓰면 그 뒤의 내용은 실행하지 않는 ‘주석(comment)’이 됩니다. 주석은 코드가 왜 존재하는지 설명하거나 나중에 기억할 내용을 적을 때 사용합니다.\n\n좋은 주석은 코드에 이미 보이는 내용을 반복하기보다 ‘왜 이렇게 했는지’를 짧게 남깁니다.",
                "# 이 프로그램은 인사 순서를 확인합니다\nprint(\"첫 번째\")\nprint(\"두 번째\")",
                null, null, null,
                "주석(comment) · #"));
        out.add(p(2, l2, "예제", "실행 순서를 눈으로 확인하기",
                "아래 코드는 세 개의 print 문장이 적힌 순서대로 실행됩니다. 줄의 위치를 바꾸면 출력 순서도 바뀝니다.",
                "print(\"첫 번째\")\nprint(\"두 번째\")\nprint(\"세 번째\")",
                null, null, null,
                "순차 실행(sequential execution)"));
        out.add(p(2, l2, "실습", "실습 A — 순서를 바꿔 결과를 통제하기",
                "코드 줄의 위치를 직접 움직여 보면서 실행 순서를 익힙니다.",
                "print(\"A\")\nprint(\"B\")\nprint(\"C\")",
                "1. C가 가장 먼저 나오도록 줄 순서를 바꾸기\n2. B → A → C 순서가 나오도록 바꾸기\n3. 한 줄을 삭제하고 남은 결과 확인하기\n4. #을 붙여 한 줄만 실행되지 않게 만들기",
                null, null,
                "순차 실행 · 주석 · 코드 순서"));
        out.add(p(2, l2, "실습", "실습 B — 표현식은 먼저 계산된다",
                "print 안에 계산식을 넣으면 계산식이 먼저 값이 되고, 그 다음 print가 그 값을 보여줍니다.",
                "print(2 + 3)\nprint(10 - 4)\nprint(3 * 2 + 1)",
                "1. 각 줄의 결과를 실행 전에 적기\n2. 3 * 2 + 1의 숫자를 바꿔 새 식 만들기\n3. print 안에 자기만의 계산식 두 개 추가하기",
                null, null,
                "표현식(expression) · 평가(evaluate)"));
        out.add(p(2, l2, "확인", "확인 문제 — 코드 흐름 읽기",
                "코드를 위에서 아래로 읽는 습관이 생겼는지 확인합니다.",
                null, null,
                "1. statement와 expression의 차이를 한 문장으로 말해 보세요.\n2. # 뒤의 문장은 실행될까요?\n3. print(4 + 1)에서는 무엇이 먼저 일어날까요?",
                "1. statement는 실행할 명령 단위이고 expression은 계산되어 값이 되는 부분입니다.\n2. 실행되지 않습니다. 주석입니다.\n3. 4 + 1이 5로 계산된 뒤 print가 5를 보여줍니다.",
                "statement · expression · comment · execution order"));
        out.add(p(2, l2, "정리", "챕터 2 정리 — 코드를 읽는 첫 번째 방법",
                "코드를 만나면 먼저 위에서 아래로 읽고, 각 줄이 ‘무슨 명령인지’, 줄 안의 계산식이 ‘어떤 값이 되는지’를 나눠 보면 됩니다. # 주석은 실행용이 아니라 사람을 위한 설명입니다.\n\n다음에는 값을 저장하고 다시 사용하는 변수로 넘어갑니다.",
                null, null, null, null,
                "sequential execution · statement · expression · comment"));

        // LESSON 03
        final String l3 = "값에 이름을 붙이는 변수 이해하기";
        out.add(p(3, l3, "시작", "변수는 값을 다시 쓰기 위한 이름이다",
                "프로그램에서 같은 숫자나 글자를 여러 번 직접 적으면 수정하기 어렵습니다. 그래서 값에 이름을 붙여 저장해 두고 그 이름을 다시 사용합니다. 이 이름이 ‘변수(variable)’입니다.",
                null, null, null, null,
                "변수(variable) · 값(value)"));
        out.add(p(3, l3, "이론", "‘상자’ 비유보다 조금 더 정확한 설명",
                "처음에는 변수를 ‘값을 넣는 상자’라고 생각해도 됩니다. 하지만 인터넷 글을 읽을 때는 ‘이름이 값에 연결된다(binding)’는 표현도 자주 봅니다.\n\nname = \"민수\"는 name이라는 이름을 \"민수\"라는 값에 연결하는 것입니다. 이 정도만 알아도 assignment, binding 같은 단어를 만났을 때 길을 잃지 않습니다.",
                null, null, null, null,
                "변수(variable) · 대입(assignment) · 연결(binding)"));
        out.add(p(3, l3, "이론", "= 는 ‘같다’가 아니라 대입이다",
                "수학에서는 =가 ‘왼쪽과 오른쪽이 같다’는 뜻이지만, Python의 변수 대입에서는 오른쪽 값을 왼쪽 이름에 저장한다는 뜻입니다.\n\nage = 37을 읽을 때는 ‘age는 37과 같다’보다 ‘37을 age에 넣는다’라고 읽는 편이 정확합니다. 같은 변수에 새 값을 넣으면 이후에는 새 값이 사용됩니다.",
                "age = 37\nage = 38\nprint(age)",
                null, null, null,
                "대입 연산자(assignment operator) · 재대입(reassignment)"));
        out.add(p(3, l3, "예제", "변수를 만들고 다시 사용하기",
                "한 번 만든 변수는 뒤의 코드에서 이름으로 사용할 수 있습니다. 계산식에도 넣을 수 있습니다.",
                "name = \"승원\"\nage = 37\nnext_age = age + 1\n\nprint(name)\nprint(next_age)",
                null, null, null,
                "식별자(identifier) · 변수 이름(variable name)"));
        out.add(p(3, l3, "실습", "실습 A — 내 정보로 변수 바꾸기",
                "변수의 이름과 값이 각각 어떤 역할인지 구분해 봅니다.",
                "name = \"승원\"\nage = 37\ncity = \"서울\"",
                "1. 세 값을 자기 정보로 바꾸기\n2. print(name), print(age), print(city) 추가하기\n3. age = age + 1을 넣고 결과 확인하기\n4. city 변수 이름을 home_city로 바꾸고 모든 사용 위치도 함께 수정하기",
                null, null,
                "identifier · reassignment · 변수명"));
        out.add(p(3, l3, "실습", "실습 B — 좋은 변수 이름 만들기",
                "변수 이름은 나중에 읽었을 때 무엇을 담는지 알 수 있어야 합니다. Python에서는 보통 소문자와 밑줄을 사용합니다.",
                "price = 1200\nitem_count = 3\ntotal_price = price * item_count",
                "1. a, b, c 같은 이름 대신 의미가 드러나는 이름으로 바꾸기\n2. ‘오늘 걸음 수’를 저장할 변수 이름 만들기\n3. ‘사용자 이름’을 저장할 변수 이름 만들기\n4. 숫자로 시작하는 이름은 왜 피해야 하는지 Python에서 직접 확인해 보기",
                null, null,
                "naming · snake_case · identifier"));
        out.add(p(3, l3, "확인", "확인 문제 — 변수와 대입",
                "변수 용어를 이해했는지 확인합니다.",
                null, null,
                "1. x = 10 다음에 x = 20을 실행하면 print(x)는 무엇일까요?\n2. = 기호는 여기서 어떤 뜻일까요?\n3. 변수 이름은 왜 a보다 total_price가 더 좋은 경우가 많을까요?",
                "1. 20\n2. 오른쪽 값을 왼쪽 변수에 대입한다는 뜻입니다.\n3. 이름만 보고도 값의 의미를 짐작할 수 있어 코드를 읽고 수정하기 쉽기 때문입니다.",
                "variable · value · assignment · identifier · reassignment"));
        out.add(p(3, l3, "정리", "챕터 3 정리 — 이름과 값의 관계",
                "변수는 값을 다시 사용하기 위한 이름입니다. =는 대입이고, 같은 변수에 새 값을 넣는 것을 재대입이라고 합니다. 변수 이름은 의미가 드러나게 짓는 것이 좋습니다.\n\n다음에는 변수 안에 들어갈 수 있는 값의 종류, 즉 데이터 타입을 배웁니다.",
                null, null, null, null,
                "variable · assignment · binding · identifier"));

        // LESSON 04
        final String l4 = "숫자·문자열·참거짓과 데이터 타입 이해하기";
        out.add(p(4, l4, "시작", "값에는 종류가 있다",
                "컴퓨터는 10이라는 숫자와 \"10\"이라는 글자를 다르게 다룹니다. 값의 종류를 ‘데이터 타입(data type)’ 또는 줄여서 ‘타입(type)’이라고 합니다. 타입을 알면 어떤 연산이 가능한지 예측할 수 있습니다.",
                null, null, null, null,
                "데이터 타입(data type) · 타입(type)"));
        out.add(p(4, l4, "이론", "처음 알아야 할 네 가지 타입",
                "Python 입문에서 가장 자주 만나는 타입은 네 가지입니다. int는 정수, float는 소수점이 있는 숫자, str은 문자열, bool은 참/거짓입니다.\n\nint: 10, -3\nfloat: 3.14, 0.5\nstr: \"안녕\", \"10\"\nbool: True, False",
                null, null, null, null,
                "int · float · str · bool"));
        out.add(p(4, l4, "이론", "타입이 다르면 같은 +도 의미가 달라진다",
                "숫자끼리 +를 쓰면 덧셈을 하고, 문자열끼리 +를 쓰면 글자를 이어 붙입니다. 타입이 맞지 않으면 오류가 날 수 있습니다.\n\n이 때문에 인터넷에서 ‘type mismatch’, ‘type error’라는 말을 자주 봅니다. ‘값의 종류가 기대한 것과 다르다’는 뜻으로 이해하면 됩니다.",
                "print(2 + 3)\nprint(\"안녕\" + \"하세요\")",
                null, null, null,
                "연산자(operator) · 타입 불일치(type mismatch)"));
        out.add(p(4, l4, "예제", "type()으로 값의 종류 확인하기",
                "Python의 type()을 사용하면 값이나 변수의 타입을 확인할 수 있습니다. 타입이 헷갈릴 때 직접 확인하는 습관이 좋습니다.",
                "count = 3\nprice = 12.5\nname = \"민수\"\nis_ready = True\n\nprint(type(count))\nprint(type(price))\nprint(type(name))\nprint(type(is_ready))",
                null, null, null,
                "type() · int · float · str · bool"));
        out.add(p(4, l4, "실습", "실습 A — 같은 모양, 다른 타입",
                "눈으로 비슷해 보여도 타입이 다른 값들을 비교해 봅니다.",
                "a = 10\nb = \"10\"\nprint(type(a))\nprint(type(b))",
                "1. a + a의 결과 예상 후 실행\n2. b + b의 결과 예상 후 실행\n3. 3.0과 3의 type() 결과 비교\n4. True와 \"True\"의 타입 비교",
                null, null,
                "숫자(number) · 문자열(string) · bool"));
        out.add(p(4, l4, "실습", "실습 B — 타입에 맞는 연산 고르기",
                "각 값의 종류에 맞는 연산을 선택하는 연습입니다.",
                "price = 2500\ncount = 4\nproduct = \"사과\"",
                "1. 총 가격을 계산하는 식 만들기\n2. product를 두 번 이어 붙여 보기\n3. price + product를 실행하면 어떤 일이 생기는지 확인하기\n4. 오류 메시지에 TypeError라는 단어가 있는지 찾아보기",
                null, null,
                "operator · TypeError · type mismatch"));
        out.add(p(4, l4, "확인", "확인 문제 — 타입 읽기",
                "타입 이름을 외우는 것보다 값의 성격을 구분하는 것이 중요합니다.",
                null, null,
                "1. \"25\"는 int일까요 str일까요?\n2. 25.0은 어떤 타입일까요?\n3. True는 글자일까요 참거짓 값일까요?",
                "1. str\n2. float\n3. bool 타입의 참거짓 값입니다.",
                "data type · int · float · str · bool · TypeError"));
        out.add(p(4, l4, "정리", "챕터 4 정리 — 값의 종류를 먼저 확인하자",
                "타입은 값의 종류입니다. int, float, str, bool 네 가지를 구분하면 입문 단계의 많은 문제를 해결할 수 있습니다. 같은 연산자라도 타입에 따라 동작이 달라질 수 있습니다.\n\n다음에는 사용자가 직접 값을 입력하게 만들고, 문자열 입력을 숫자로 바꾸는 방법을 배웁니다.",
                null, null, null, null,
                "type · int · float · str · bool · operator"));

        // LESSON 05
        final String l5 = "입력과 출력, 형 변환 이해하기";
        out.add(p(5, l5, "시작", "프로그램은 입력을 받아 결과를 내놓는다",
                "많은 프로그램을 ‘입력(input) → 처리(process) → 출력(output)’ 세 단계로 볼 수 있습니다. 계산기라면 숫자를 입력받고 계산한 뒤 결과를 출력합니다. 검색 앱이라면 검색어를 입력받고 검색한 뒤 결과 목록을 보여줍니다.",
                null, null, null, null,
                "입력(input) · 처리(process) · 출력(output) · I/O"));
        out.add(p(5, l5, "이론", "input()이 돌려주는 값은 문자열이다",
                "Python의 input()은 사용자가 키보드로 입력한 내용을 문자열(str)로 돌려줍니다. 화면에 10을 입력해도 처음에는 숫자 10이 아니라 글자 \"10\"입니다.\n\n그래서 숫자 계산을 하려면 int()나 float()를 사용해 타입을 바꾸는 ‘형 변환(type conversion)’이 필요할 수 있습니다.",
                null, null, null, null,
                "input() · 반환값(return value) · 형 변환(type conversion)"));
        out.add(p(5, l5, "이론", "int(), float(), str()로 타입 바꾸기",
                "int(\"10\")은 문자열 \"10\"을 정수 10으로 바꿉니다. float(\"3.5\")는 소수 숫자로 바꿉니다. str(10)은 숫자 10을 문자열 \"10\"으로 바꿉니다.\n\n바꿀 수 없는 문자열을 숫자로 바꾸려 하면 ValueError가 납니다. 예: int(\"사과\").",
                "age_text = \"37\"\nage = int(age_text)\nprint(age + 1)",
                null, null, null,
                "int() · float() · str() · ValueError"));
        out.add(p(5, l5, "예제", "입력받아 계산하는 프로그램",
                "input으로 받은 문자열을 int로 바꾼 뒤 계산합니다. f-string은 문자열 안에 변수 값을 보기 좋게 넣는 방법입니다.",
                "age = int(input(\"나이: \"))\nnext_age = age + 1\nprint(f\"내년에는 {next_age}살입니다.\")",
                null, null, null,
                "f-string · input · conversion · output"));
        out.add(p(5, l5, "실습", "실습 A — 내 이름과 나이 입력받기",
                "사용자가 입력한 값을 저장하고 다시 출력해 봅니다.",
                "name = input(\"이름: \")\nage = int(input(\"나이: \"))",
                "1. name과 age를 출력하기\n2. ‘OOO님은 37살입니다’ 형태로 출력하기\n3. 내년 나이를 계산해 함께 출력하기\n4. 나이에 글자를 입력했을 때 오류 이름 확인하기",
                null, null,
                "input · int · f-string · ValueError"));
        out.add(p(5, l5, "실습", "실습 B — 두 숫자 더하기",
                "input 결과를 바로 계산하려 하면 문자열 연결이 될 수 있으므로 숫자로 변환하는 과정을 넣습니다.",
                "a = int(input(\"첫 번째 숫자: \"))\nb = int(input(\"두 번째 숫자: \"))\nprint(a + b)",
                "1. 10과 20을 입력해 30 확인\n2. 덧셈을 곱셈으로 바꾸기\n3. 소수 입력을 받도록 int를 float로 바꾸기\n4. 변수 이름 a, b를 더 설명적인 이름으로 바꾸기",
                null, null,
                "I/O · conversion · float()"));
        out.add(p(5, l5, "확인", "확인 문제 — 입력과 형 변환",
                "입력값의 타입을 이해하면 초보 단계의 많은 오류를 피할 수 있습니다.",
                null, null,
                "1. input()의 기본 반환 타입은 무엇일까요?\n2. \"25\"를 숫자 25로 바꾸려면 무엇을 사용할까요?\n3. int(\"hello\")는 정상 실행될까요?",
                "1. str\n2. int(\"25\")\n3. 아니요. 숫자로 바꿀 수 없는 문자열이라 ValueError가 발생합니다.",
                "input · output · type conversion · return value · ValueError"));
        out.add(p(5, l5, "정리", "챕터 5 정리 — 입력은 타입까지 확인한다",
                "프로그램은 입력을 받고, 처리하고, 결과를 출력합니다. input()의 결과는 문자열이므로 숫자 계산이 필요하면 int()나 float()로 변환합니다. f-string을 사용하면 변수 값을 문장 안에 넣기 쉽습니다.\n\n다음에는 오류 메시지를 읽고 스스로 고치는 기본 디버깅 방법을 배웁니다.",
                null, null, null, null,
                "I/O · input · output · conversion · f-string"));

        // LESSON 06
        final String l6 = "오류 메시지와 디버깅의 기본 이해하기";
        out.add(p(6, l6, "시작", "오류는 ‘틀렸다’가 아니라 단서다",
                "코딩에서는 오류가 자주 발생합니다. 오류 메시지는 프로그램이 왜 멈췄는지 알려주는 보고서입니다. 숙련된 사람도 오류를 없애는 것이 아니라, 오류를 빠르게 읽고 원인을 좁히는 능력을 사용합니다. 이 과정을 ‘디버깅(debugging)’이라고 합니다.",
                null, null, null, null,
                "오류(error) · 예외(exception) · 디버깅(debugging)"));
        out.add(p(6, l6, "이론", "초보가 먼저 알아둘 오류 네 가지",
                "SyntaxError: 문법 자체가 잘못됨\nNameError: 없는 이름을 사용함\nTypeError: 맞지 않는 타입으로 연산함\nValueError: 타입 변환 형식에 맞지 않는 값이 들어옴\n\n오류 이름을 보면 문제의 범위를 빠르게 좁힐 수 있습니다.",
                null, null, null, null,
                "SyntaxError · NameError · TypeError · ValueError"));
        out.add(p(6, l6, "이론", "traceback에서 무엇부터 볼까?",
                "Python 오류에는 보통 ‘어느 파일의 몇 번째 줄에서 문제가 났는지’와 마지막에 오류 종류·설명이 표시됩니다. 이런 실행 경로 정보를 traceback이라고 부릅니다.\n\n초보자는 ① 마지막 오류 이름 ② 표시된 줄 번호 ③ 방금 수정한 부분 순서로 확인하면 됩니다. 메시지 전체를 한 번에 이해하려고 할 필요는 없습니다.",
                null, null, null, null,
                "traceback · line number · exception message"));
        out.add(p(6, l6, "예제", "네 가지 오류를 구분해 보기",
                "아래 예시는 각각 다른 이유로 실패합니다. 한 번에 모두 실행하지 말고 한 줄씩 바꿔 확인하는 것이 좋습니다.",
                "# SyntaxError 예: print(\"안녕)\n# NameError 예: print(user_name)\n# TypeError 예: print(10 + \"5\")\n# ValueError 예: print(int(\"사과\"))",
                null, null, null,
                "error class · traceback · debug"));
        out.add(p(6, l6, "실습", "실습 A — 일부러 오류 만들고 복구하기",
                "오류를 일부러 만들어 보면 실제로 발생했을 때 덜 당황하게 됩니다.",
                "name = \"민수\"\nprint(name)",
                "1. 마지막 따옴표를 지워 SyntaxError 만들기\n2. print(nam)으로 바꿔 NameError 만들기\n3. print(10 + \"5\")로 TypeError 확인하기\n4. 각 오류를 하나씩 원상복구하기",
                null, null,
                "reproduce · fix · SyntaxError · NameError · TypeError"));
        out.add(p(6, l6, "실습", "실습 B — 디버깅 4단계 습관",
                "문제가 생기면 무작정 전체 코드를 다시 쓰지 않습니다. 범위를 좁혀야 합니다.",
                "price = \"1200\"\ncount = 3\nprint(price * count)",
                "1. 실제 결과가 의도와 같은지 확인하기\n2. 변수의 type()을 출력해 보기\n3. price를 int로 바꿔 숫자 계산으로 수정하기\n4. ‘증상 → 원인 → 수정 → 재실행’ 순서로 메모하기",
                null, null,
                "debugging loop · reproduce · inspect · fix · verify"));
        out.add(p(6, l6, "확인", "확인 문제 — 오류 이름으로 범위 좁히기",
                "오류 이름과 대표 원인을 연결할 수 있는지 확인합니다.",
                null, null,
                "1. 정의하지 않은 변수 이름을 쓰면 어떤 오류가 흔할까요?\n2. int(\"hello\")는 어떤 오류가 흔할까요?\n3. 오류가 난 직후 전체 코드를 다시 쓰는 것보다 먼저 할 일은 무엇일까요?",
                "1. NameError\n2. ValueError\n3. 오류 종류·줄 번호·방금 바꾼 부분을 확인해 원인 범위를 좁힙니다.",
                "debugging · traceback · SyntaxError · NameError · TypeError · ValueError"));
        out.add(p(6, l6, "정리", "챕터 6 정리 — 오류를 읽는 순서",
                "오류는 디버깅을 위한 단서입니다. 마지막 오류 이름, 줄 번호, 최근 변경 내용을 먼저 확인합니다. SyntaxError·NameError·TypeError·ValueError 네 가지를 알아두면 검색할 때도 훨씬 정확한 질문을 만들 수 있습니다.\n\n다음에는 변수와 연산자를 조합해 실제 계산 도구를 만듭니다.",
                null, null, null, null,
                "error · exception · traceback · debugging"));

        // LESSON 07
        final String l7 = "연산자와 계산 순서로 작은 계산기 만들기";
        out.add(p(7, l7, "시작", "코드는 실제 계산 도구가 될 수 있다",
                "지금까지 배운 변수와 타입을 조합하면 반복 계산을 프로그램에 맡길 수 있습니다. 계산기 프로그램은 ‘값 저장 → 계산 → 출력’이라는 가장 기본적인 프로그램 구조를 보여주는 좋은 연습입니다.",
                null, null, null, null,
                "연산자(operator) · 계산(expression)"));
        out.add(p(7, l7, "이론", "기본 산술 연산자",
                "+ 는 더하기, - 는 빼기, * 는 곱하기, / 는 나누기입니다. Python에는 // 정수 나눗셈, % 나머지, ** 거듭제곱도 있지만 입문에서는 우선 + - * / 네 가지를 자유롭게 쓰는 것이 중요합니다.\n\n연산자는 값을 이용해 새로운 값을 만드는 기호입니다.",
                null, null, null, null,
                "산술 연산자(arithmetic operator) · + · - · * · /"));
        out.add(p(7, l7, "이론", "계산 순서와 괄호",
                "Python도 일반적인 수학 규칙처럼 곱셈·나눗셈을 덧셈·뺄셈보다 먼저 계산합니다. 헷갈릴 때는 괄호를 사용해 의도를 분명하게 만들 수 있습니다.\n\n2 + 3 * 4는 14이고, (2 + 3) * 4는 20입니다. 코드를 읽는 사람에게 계산 의도를 보여주기 위해 괄호를 쓰는 것도 좋은 습관입니다.",
                null, null, null, null,
                "연산 우선순위(operator precedence) · 괄호(parentheses)"));
        out.add(p(7, l7, "예제", "가격 × 개수 = 총액",
                "변수에 단가와 수량을 저장하면 값을 바꾸기만 해도 총액을 다시 계산할 수 있습니다.",
                "price = 1200\ncount = 3\ntotal = price * count\n\nprint(f\"총 금액: {total}원\")",
                null, null, null,
                "price · count · total · calculation"));
        out.add(p(7, l7, "실습", "실습 A — 장보기 계산기",
                "현실 숫자를 넣어 계산 결과를 확인합니다.",
                "apple_price = 1500\napple_count = 4\nbanana_price = 2000\nbanana_count = 2",
                "1. 사과 총액 계산하기\n2. 바나나 총액 계산하기\n3. 두 상품의 전체 총액 계산하기\n4. 10% 할인이라고 가정하고 total * 0.9도 계산해 보기",
                null, null,
                "total · discount · arithmetic"));
        out.add(p(7, l7, "실습", "실습 B — 입력받는 계산기로 확장하기",
                "고정 숫자 대신 사용자가 값을 입력하도록 바꾸면 재사용 가능한 작은 도구가 됩니다.",
                "price = int(input(\"가격: \"))\ncount = int(input(\"개수: \"))\ntotal = price * count\nprint(f\"총액: {total}원\")",
                "1. 가격 2500, 개수 4 입력해 결과 확인\n2. 배송비 변수를 추가해 총액에 더하기\n3. 괄호를 사용해 ‘상품 총액 + 배송비’를 분명하게 표현하기\n4. 소수 가격을 받을 경우 int 대신 무엇을 쓸지 생각하기",
                null, null,
                "input · arithmetic · precedence · float"));
        out.add(p(7, l7, "확인", "확인 문제 — 연산과 계산 순서",
                "결과를 실행 전에 예측해 보는 문제입니다.",
                null, null,
                "1. 2 + 3 * 4의 결과는?\n2. (2 + 3) * 4의 결과는?\n3. price=3000, count=2일 때 price * count는?",
                "1. 14\n2. 20\n3. 6000",
                "operator · precedence · parentheses · expression"));
        out.add(p(7, l7, "정리", "챕터 7 정리 — 계산을 코드로 옮기는 법",
                "연산자는 값으로 새 값을 만듭니다. 계산 순서가 헷갈리면 괄호로 의도를 명확히 합니다. 변수와 입력을 함께 사용하면 같은 계산을 여러 상황에서 재사용할 수 있습니다.\n\n마지막 챕터에서는 지금까지 배운 것만 사용해 작은 프로그램을 처음부터 설계하고 테스트합니다.",
                null, null, null, null,
                "arithmetic operator · expression · precedence · parentheses"));

        // LESSON 08
        final String l8 = "첫 미니 프로젝트를 설계하고 테스트하기";
        out.add(p(8, l8, "시작", "프로젝트는 코드를 쓰기 전에 요구사항부터 정한다",
                "작은 프로그램도 먼저 ‘무엇을 만들 것인지’를 한 문장으로 정하면 훨씬 쉽습니다. 이것을 요구사항(requirement) 또는 명세(specification)의 아주 간단한 형태라고 생각할 수 있습니다.\n\n이번 프로젝트 목표: ‘이름과 세 가지 지출 금액을 입력받아 오늘의 총 지출을 보여준다.’",
                null, null, null, null,
                "요구사항(requirement) · 명세(specification)"));
        out.add(p(8, l8, "이론", "입력 → 처리 → 출력으로 문제 나누기",
                "문제를 세 칸으로 나누면 코드로 옮기기 쉽습니다.\n\n입력: 이름, 커피값, 점심값, 교통비\n처리: 세 금액을 모두 더함\n출력: 이름과 총 지출 금액\n\n이렇게 해결 절차를 단계로 정리한 생각을 넓은 의미에서 알고리즘(algorithm)이라고 부릅니다.",
                null, null, null, null,
                "input-process-output · 알고리즘(algorithm)"));
        out.add(p(8, l8, "이론", "테스트 케이스와 경계값",
                "코드가 한 번 실행됐다고 끝이 아닙니다. 특정 입력을 넣었을 때 기대한 결과가 나오는지 확인해야 합니다. 이런 입력과 기대 결과의 조합을 ‘테스트 케이스(test case)’라고 합니다.\n\n0원, 큰 금액, 잘못된 글자 입력처럼 평소와 다른 값도 생각해 보면 좋습니다. 이런 가장자리 상황을 ‘경계값’ 또는 ‘edge case’라고 부릅니다.",
                null, null, null, null,
                "테스트 케이스(test case) · 경계값(edge case) · 검증(verify)"));
        out.add(p(8, l8, "예제", "오늘 지출 계산기 기본 버전",
                "지금까지 배운 input, int, 변수, 덧셈, f-string만 사용합니다. 새 문법을 억지로 추가하지 않는 것이 중요합니다.",
                "name = input(\"이름: \")\ncoffee = int(input(\"커피: \"))\nlunch = int(input(\"점심: \"))\ntransport = int(input(\"교통: \"))\n\ntotal = coffee + lunch + transport\nprint(f\"{name}님의 오늘 지출은 {total}원입니다.\")",
                null, null, null,
                "requirement · input · process · output"));
        out.add(p(8, l8, "실습", "실습 A — 내 지출로 완성하기",
                "예제 코드를 그대로 보는 데서 끝내지 않고 자기 상황에 맞게 바꿉니다.",
                null,
                "1. 실제 또는 가상의 금액 세 개를 입력해 실행하기\n2. snack 변수를 추가해 간식비도 입력받기\n3. total 계산식에 snack을 포함하기\n4. 출력 문장을 자기 스타일로 바꾸기\n5. 실행 전에 손계산한 값과 프로그램 결과 비교하기",
                null, null,
                "변형(modify) · 검산(check) · 재실행"));
        out.add(p(8, l8, "실습", "실습 B — 테스트 케이스 세 개 만들기",
                "테스트는 ‘대충 잘 되겠지’가 아니라 입력과 기대 결과를 미리 정해 확인하는 것입니다.",
                null,
                "테스트 1: coffee=0, lunch=9000, transport=1500 → 기대값 10500\n테스트 2: 모두 0 → 기대값 0\n테스트 3: 큰 금액을 넣어 계산 확인\n\n추가: 숫자 대신 ‘천원’을 입력하면 어떤 오류가 나는지 확인하고 오류 이름 기록하기",
                null, null,
                "test case · expected result · edge case · ValueError"));
        out.add(p(8, l8, "확인", "확인 문제 — 프로그램을 설계하는 순서",
                "코드 문법보다 더 중요한 ‘문제 나누기’가 이해됐는지 확인합니다.",
                null, null,
                "1. 프로그램을 입력·처리·출력으로 나누는 이유는 무엇일까요?\n2. test case는 무엇과 무엇의 조합일까요?\n3. edge case는 어떤 상황을 뜻할까요?",
                "1. 큰 문제를 작은 단계로 나눠 설계와 디버깅을 쉽게 하기 위해서입니다.\n2. 테스트 입력과 기대 결과의 조합입니다.\n3. 0, 매우 큰 값, 잘못된 형식처럼 평소 범위의 가장자리나 특이한 상황입니다.",
                "algorithm · requirement · test case · edge case"));
        out.add(p(8, l8, "완료", "TRACK 01 완료 — 이제 무엇을 할 수 있나?",
                "이제 코딩이 무엇인지 설명하고, Python 코드의 기본 실행 흐름을 읽고, 변수와 타입을 구분하고, 입력값을 숫자로 바꾸고, 오류 메시지의 핵심을 읽고, 간단한 계산 프로그램을 설계할 수 있습니다.\n\n아직 조건문·반복문·함수는 배우지 않았습니다. 그 내용은 TRACK 02에서 다룹니다. 지금 단계에서 모르는 글을 검색할 때는 아래 키워드를 사용하면 다른 책이나 인터넷 설명을 이해할 기반이 생겼습니다.",
                null, null, null, null,
                "program · source code · interpreter · statement · expression · variable · type · I/O · debugging · test case"));

        return Collections.unmodifiableList(out);
    }

    static int firstPageIndexOfLesson(List<Page> pages, int lessonNumber) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).lessonNumber == lessonNumber) return i;
        }
        return 0;
    }
}
