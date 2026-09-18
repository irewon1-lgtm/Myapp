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
        final String easyExplanation;
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
            this(
                    lessonNumber,
                    lessonTitle,
                    kind,
                    title,
                    body,
                    "",
                    code,
                    practice,
                    question,
                    answer,
                    keywords
            );
        }

        Page(
                int lessonNumber,
                String lessonTitle,
                String kind,
                String title,
                String body,
                String easyExplanation,
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
            this.easyExplanation = easyExplanation == null ? "" : easyExplanation;
            this.code = code;
            this.practice = practice;
            this.question = question;
            this.answer = answer;
            this.keywords = keywords;
        }

        boolean hasCode() { return code != null && !code.isEmpty(); }
        boolean hasEasyExplanation() { return easyExplanation != null && !easyExplanation.isEmpty(); }
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

        return Collections.unmodifiableList(enrich(out));
    }

    private static List<Page> enrich(List<Page> raw) {
        List<Page> expanded = new ArrayList<>();
        int lastLesson = -1;

        for (Page source : raw) {
            if (source.lessonNumber != lastLesson) {
                expanded.addAll(vocabularyPages(source.lessonNumber, source.lessonTitle));
                lastLesson = source.lessonNumber;
            }

            Page explained = new Page(
                    source.lessonNumber,
                    source.lessonTitle,
                    source.kind,
                    source.title,
                    expandedBodyFor(source),
                    easyExplanationFor(source),
                    source.code,
                    source.practice,
                    source.question,
                    source.answer,
                    source.keywords
            );
            expanded.add(explained);

            if (source.hasPractice()) {
                expanded.add(practiceAnswerPage(source));
            }
            if (source.hasQuestion()) {
                expanded.add(questionAnswerPage(source));
            }
        }

        return expanded;
    }

    private static String expandedBodyFor(Page source) {
        String deeper = deepDiveFor(source);
        String fill = fullPageExpansionFor(source);

        StringBuilder body = new StringBuilder(source.body);
        if (deeper != null && !deeper.isEmpty()) {
            body.append("\n\n조금 더 깊게 이해하기\n").append(deeper);
        }
        if (fill != null && !fill.isEmpty()) {
            body.append("\n\n실제로 연결해서 이해하기\n").append(fill);
        }
        return body.toString();
    }

    private static String fullPageExpansionFor(Page page) {
        switch (page.title) {
            case "코딩은 컴퓨터에게 일을 시키는 방법이다":
                return "예를 들어 휴대폰 계산기에서 2와 +와 3을 누르면 프로그램은 ‘사용자가 숫자 2를 입력했다’, ‘더하기를 선택했다’, ‘숫자 3을 입력했다’는 정보를 순서대로 받아 5라는 결과를 만듭니다. 우리가 앞으로 쓰는 짧은 Python 코드도 구조는 같습니다. 입력이나 값이 있고, 코드가 그것을 처리하고, 마지막에 결과가 나옵니다. 따라서 처음 코드를 볼 때는 문법부터 외우기보다 ‘이 코드는 어떤 일을 시키는가?’를 한 문장으로 말해 보는 연습이 좋습니다.";
            case "소스 코드와 프로그래밍 언어":
                return "실제 개발자는 소스 코드를 편집기에서 열어 수정하고 저장한 뒤 실행합니다. 같은 ‘안녕하세요 출력하기’도 Python에서는 print(\"안녕하세요\")라고 쓰고 다른 언어에서는 문법이 달라질 수 있습니다. 그래서 특정 문법을 외우는 것과 ‘출력한다’는 개념을 이해하는 것은 다릅니다. 인터넷 글을 읽을 때 Python 문법을 몰라도 ‘이 부분은 출력 코드구나’라고 구조를 파악할 수 있으면 다음 내용을 배우기가 훨씬 쉽습니다.";
            case "Python 코드는 누가 읽고 실행할까?":
                return "사용자가 Run 버튼을 누르면 편집기가 Python 실행 프로그램을 불러 코드를 전달하고, Python은 위에서부터 코드를 해석하면서 필요한 동작을 수행합니다. 실행 환경이 없거나 버전이 맞지 않으면 소스 코드가 올바르더라도 실행되지 않을 수 있습니다. 그래서 설치 안내에서 ‘Python 3.x 필요’, ‘runtime 설치’ 같은 문장을 보게 됩니다. 이때 runtime은 코드를 쓰는 문법이 아니라 ‘그 코드를 실제로 움직일 수 있게 해 주는 환경’이라는 점을 구분하세요.";
            case "첫 명령: print로 결과 보여주기":
                return "print는 학습 초기에 특히 중요합니다. 변수 안에 무엇이 들어 있는지, 계산 결과가 예상과 맞는지, 코드가 어느 지점까지 실행됐는지를 눈으로 확인할 수 있기 때문입니다. 예를 들어 total = 1200 * 3 다음에 print(total)을 넣으면 3600이 저장됐는지 즉시 확인할 수 있습니다. 나중에는 전문적인 로그 도구도 사용하지만, print로 값을 확인하는 습관은 디버깅의 출발점으로 계속 도움이 됩니다.";
            case "실습 A — 코드 한 글자 바꾸면 결과도 바뀐다":
                return "실습을 할 때는 여러 줄을 한꺼번에 바꾸지 말고 한 번에 한 가지씩 바꿔 보세요. 이름만 바꾸고 실행, 숫자만 바꾸고 실행, 새 print 한 줄을 추가하고 실행하는 식입니다. 이렇게 하면 어떤 수정이 어떤 결과를 만들었는지 정확히 연결할 수 있습니다. 실제 개발에서도 문제가 생겼을 때 최근에 바꾼 부분을 작게 나눠 확인하는 것이 오류 원인을 빠르게 찾는 방법입니다.";
            case "실습 B — 예상하고 실행하기":
                return "예상할 때는 머릿속으로 Python의 역할을 흉내 내면 됩니다. 첫 줄을 보고 ‘이 줄이 계산되는가, 아니면 글자인가’를 판단하고, 예상 값을 적은 다음 다음 줄로 넘어갑니다. 실행 결과와 예상이 다르면 바로 정답만 보지 말고 어느 규칙을 잘못 적용했는지 찾아보세요. 이렇게 하면 단순히 문제 하나를 맞히는 것이 아니라 처음 보는 코드도 스스로 읽는 힘이 생깁니다.";
            case "확인 문제 — 코딩의 큰 그림":
                return "문제를 풀고 나서는 답을 단어 하나로만 외우지 말고 서로 연결해 보세요. ‘Python이라는 programming language로 source code를 작성하고, interpreter가 그것을 execute해서 output을 만든다’라고 한 문장으로 말할 수 있으면 개념들이 따로 흩어져 있지 않은 것입니다. 앞으로 새로운 용어를 만나도 ‘코드를 쓰는 쪽인가, 실행하는 쪽인가, 결과 쪽인가’를 먼저 분류하면 훨씬 이해하기 쉽습니다.";
            case "챕터 1 정리 — 지금 알아야 할 것":
                return "지금 단계에서 코드를 완벽히 쓰는 것보다 용어를 보고 질문할 수 있는 상태가 되는 것이 중요합니다. 예를 들어 ‘Python interpreter가 뭔가요?’, ‘source code와 실행 파일의 차이가 뭔가요?’처럼 정확한 단어로 질문하면 인터넷 검색이나 AI 답변의 품질도 좋아집니다. 다음 챕터에서는 이 큰 흐름 안에서 여러 줄의 코드가 어떤 순서로 움직이는지 더 구체적으로 읽게 됩니다.";

            case "여러 줄의 코드는 ‘순서’가 중요하다":
                return "예를 들어 name = \"민수\"라는 줄보다 print(name)이 먼저 실행되면 Python은 아직 name이 무엇인지 모를 수 있습니다. 즉 코드의 내용뿐 아니라 위치와 순서도 의미를 가집니다. 나중에 프로그램이 길어지면 ‘값 준비 → 계산 → 출력’처럼 자연스러운 순서를 만들고, 관련된 코드를 가까이 두는 것이 읽기와 디버깅에 큰 도움이 됩니다.";
            case "문장(statement)과 표현식(expression)":
                return "코드를 읽을 때 statement와 expression을 완벽히 문법적으로 분류할 필요는 없습니다. 대신 ‘이 부분은 값을 만들어 내는가?’와 ‘이 줄은 그 값을 가지고 어떤 일을 하는가?’를 물어보세요. total = price * count라면 price * count가 값을 만드는 expression이고, 그 결과를 total에 넣는 전체 줄은 대입 동작입니다. 이런 식으로 큰 줄을 작은 역할로 나누면 코드가 덜 복잡해 보입니다.";
            case "주석(comment)은 컴퓨터가 아닌 사람을 위한 메모":
                return "좋은 주석의 예는 ‘# 세금 계산 전 금액’처럼 변수의 의미나 계산 이유를 알려 주는 것입니다. 나쁜 주석의 예는 total = price * count 옆에 ‘# 가격과 개수를 곱한다’처럼 코드만 읽어도 알 수 있는 내용을 반복하는 것입니다. 주석은 코드가 설명하기 어려운 배경이나 이유를 보충할 때 가장 가치가 있습니다.";
            case "실행 순서를 눈으로 확인하기":
                return "실제 프로그램에서는 print를 여러 위치에 넣어 ‘A 지점까지 왔다’, ‘B 지점까지 왔다’처럼 실행 경로를 확인하기도 합니다. 예를 들어 특정 화면이 열리지 않을 때 함수 시작과 끝에 임시 출력문을 넣으면 어느 지점에서 실행이 멈췄는지 알 수 있습니다. 지금 배우는 단순한 순서 확인이 나중의 디버깅 습관과 직접 연결됩니다.";
            case "실습 A — 순서를 바꿔 결과를 통제하기":
                return "줄 순서를 바꾼 뒤에는 단순히 결과만 보지 말고 ‘왜 이 순서가 됐는지’를 말로 설명해 보세요. C를 맨 위로 올렸기 때문에 C가 먼저 출력되고, #이 붙은 줄은 실행 대상에서 빠지기 때문에 결과에 나타나지 않는다고 설명할 수 있어야 합니다. 설명할 수 있다면 같은 규칙을 처음 보는 코드에도 적용할 수 있습니다.";
            case "실습 B — 표현식은 먼저 계산된다":
                return "하나의 print 안에 계산이 여러 단계 들어가면 작은 부분부터 값을 만들어 간다고 생각하면 됩니다. 3 * 2 + 1에서는 먼저 3 * 2가 6이 되고, 그 결과에 1을 더해 7을 만듭니다. 나중에 함수 호출이나 복잡한 식을 읽을 때도 ‘안쪽에서 값이 만들어지고 바깥쪽 코드가 그 값을 사용한다’는 방식으로 따라가면 됩니다.";
            case "확인 문제 — 코드 흐름 읽기":
                return "답을 확인한 뒤에는 코드를 보지 않고 실행 순서를 말로 설명해 보세요. ‘첫 줄의 expression을 계산하고 출력한다. 둘째 줄은 주석이라 건너뛴다. 셋째 줄을 계산하고 출력한다’처럼 설명하면 됩니다. 코드를 소리 내어 순서대로 설명하는 연습은 초보자가 프로그램의 흐름을 이해하는 데 매우 효과적입니다.";
            case "챕터 2 정리 — 코드를 읽는 첫 번째 방법":
                return "처음 보는 짧은 코드를 읽을 때는 줄마다 번호를 매긴다고 생각하고 1번부터 따라가세요. 각 줄에서 ‘입력되는 값’, ‘계산되는 값’, ‘출력되거나 저장되는 값’을 표시하면 흐름이 보입니다. 이후 if나 반복문이 등장하면 단순한 1→2→3 흐름이 달라지지만, 현재 위치를 추적하는 습관은 그대로 사용됩니다.";

            case "변수는 값을 다시 쓰기 위한 이름이다":
                return "실제 프로그램에서는 숫자를 코드 곳곳에 직접 적기보다 의미 있는 변수로 저장합니다. 예를 들어 배송비가 3000원이라면 여러 계산식에 3000을 반복하기보다 shipping_fee = 3000으로 만들어 둡니다. 나중에 배송비가 3500원으로 바뀌어도 변수 값을 한 곳에서 수정할 수 있고, 코드를 읽는 사람도 3000이 무엇을 뜻하는지 바로 알 수 있습니다.";
            case "‘상자’ 비유보다 조금 더 정확한 설명":
                return "‘상자’ 비유는 처음 이해하기 쉽지만 Python에서는 이름과 값의 연결이 더 중요한 개념입니다. name = \"민수\"라고 했을 때 name이라는 글자 자체가 민수라는 뜻은 아닙니다. 실행 중에 name이라는 이름이 문자열 \"민수\"를 가리키도록 연결된 것입니다. 이후 name = \"영희\"를 실행하면 같은 이름이 새로운 값을 가리키게 됩니다.";
            case "= 는 ‘같다’가 아니라 대입이다":
                return "대입문의 오른쪽에는 단순한 값뿐 아니라 계산식도 올 수 있습니다. total = price * count에서는 Python이 오른쪽 price * count를 먼저 계산하고 그 결과를 total에 연결합니다. 따라서 대입문을 읽을 때는 오른쪽부터 ‘어떤 값이 만들어지는가’를 본 다음 왼쪽 변수에 그 결과가 들어간다고 생각하면 이해하기 쉽습니다.";
            case "변수를 만들고 다시 사용하기":
                return "중간 결과를 변수에 저장하면 복잡한 계산도 단계별로 나눌 수 있습니다. subtotal = price * count, tax = subtotal * 0.1, total = subtotal + tax처럼 각 단계에 이름을 붙이면 계산 규칙이 눈에 보입니다. 실제 업무 코드에서도 중간값을 적절한 변수로 나누면 오류를 찾고 수정하기 쉬워집니다.";
            case "실습 A — 내 정보로 변수 바꾸기":
                return "값을 자기 정보로 바꿨을 때 코드 구조는 그대로인데 결과만 달라지는지 확인하세요. 이것이 ‘같은 프로그램이 다른 데이터로 동작한다’는 기본 개념입니다. 실제 앱도 사용자마다 코드를 새로 만드는 것이 아니라 같은 코드가 각 사용자의 다른 데이터를 받아 처리합니다.";
            case "실습 B — 좋은 변수 이름 만들기":
                return "좋은 변수 이름은 너무 짧지도, 지나치게 길지도 않아야 합니다. user_phone_number는 의미가 분명하지만 user_phone_number_for_current_logged_in_person처럼 너무 길면 읽기 어렵습니다. 프로젝트 안에서 같은 의미의 값에는 같은 스타일의 이름을 쓰는 것도 중요합니다. Python에서는 snake_case가 일반적인 관례입니다.";
            case "확인 문제 — 변수와 대입":
                return "변수 문제를 풀 때는 종이에 현재 값을 적어 가며 따라가도 좋습니다. x = 10이면 x 옆에 10, 다음에 x = 20이면 10을 지우고 20을 적는 식입니다. 나중에 변수가 여러 개가 되면 이런 상태 추적이 코드 결과를 예측하는 데 도움이 됩니다.";
            case "챕터 3 정리 — 이름과 값의 관계":
                return "변수를 이해했다는 것은 단순히 ‘상자’라는 비유를 외운 것이 아니라, 프로그램이 실행되는 동안 값이 이름을 통해 저장되고 바뀐다는 사실을 이해한 것입니다. 이후 조건문은 변수 값을 보고 다른 행동을 선택하고, 반복문은 변수 값을 여러 번 바꾸기도 합니다. 그래서 변수는 다음 단계 문법의 중심이 됩니다.";

            case "값에는 종류가 있다":
                return "타입을 구분하는 이유는 컴퓨터가 같은 모양의 데이터라도 다른 방식으로 처리하기 때문입니다. 사용자 ID가 00123처럼 앞의 0을 유지해야 한다면 숫자가 아니라 문자열로 보관하는 것이 더 적절할 수 있습니다. 즉 어떤 타입을 선택할지는 값의 생김새뿐 아니라 그 값을 앞으로 어떻게 사용할지에 따라 결정됩니다.";
            case "처음 알아야 할 네 가지 타입":
                return "int와 float는 계산용 숫자, str은 글자, bool은 참·거짓이라는 큰 분류부터 확실히 잡으세요. 예를 들어 사람의 이름은 str, 상품 개수는 int, 할인율은 float, 로그인 여부는 bool로 표현할 수 있습니다. 실제 프로그램에서는 데이터의 의미를 보고 알맞은 타입을 선택합니다.";
            case "타입이 다르면 같은 +도 의미가 달라진다":
                return "+ 하나만 봐서는 결과를 확정할 수 없습니다. 왼쪽과 오른쪽 값의 타입을 함께 봐야 합니다. 2 + 3은 숫자 5가 되고, \"2\" + \"3\"은 문자열 \"23\"이 됩니다. 오류 없이 실행됐다고 해서 원하는 결과라는 보장은 없으므로 값과 타입을 함께 확인하는 습관이 중요합니다.";
            case "type()으로 값의 종류 확인하기":
                return "type() 결과는 보통 <class 'int'>처럼 보일 수 있습니다. 처음에는 class라는 단어까지 깊게 들어갈 필요 없이 ‘Python이 이 값을 int 타입으로 보고 있구나’라고 읽으면 충분합니다. 나중에 객체와 클래스를 배우면 이 표기가 왜 그렇게 나오는지 더 정확히 이해하게 됩니다.";
            case "실습 A — 같은 모양, 다른 타입":
                return "실습에서는 결과만 확인하지 말고 각 줄 옆에 타입을 적어 보세요. a는 int, b는 str이라고 표시한 뒤 연산 결과가 왜 다른지 연결합니다. 이런 습관을 들이면 API나 CSV 파일에서 숫자가 문자열로 들어왔을 때도 문제를 더 빨리 발견할 수 있습니다.";
            case "실습 B — 타입에 맞는 연산 고르기":
                return "TypeError가 나지 않더라도 타입 때문에 결과가 이상할 수 있습니다. 문자열 \"5\" * 3은 오류가 아니라 \"555\"가 됩니다. 따라서 ‘오류가 없으니 맞다’가 아니라 기대한 타입과 값이 맞는지 확인해야 합니다. 프로그램 검증에서는 이 두 가지를 모두 봅니다.";
            case "확인 문제 — 타입 읽기":
                return "타입을 판단한 다음에는 ‘이 값으로 어떤 연산을 할 수 있는가’를 한 번 더 생각해 보세요. str은 연결이나 반복에 사용할 수 있고, 숫자는 산술 계산에 적합하며, bool은 조건 판단에 사용됩니다. 타입은 단순한 이름표가 아니라 가능한 동작을 결정하는 규칙과 연결됩니다.";
            case "챕터 4 정리 — 값의 종류를 먼저 확인하자":
                return "실제 개발에서 데이터 문제를 만나면 값 자체와 타입을 함께 기록하는 것이 좋습니다. ‘price가 1000이다’보다 ‘price가 문자열 \"1000\"이다’가 훨씬 정확한 정보입니다. 이렇게 말할 수 있으면 검색이나 질문도 구체적이 되어 문제 해결 속도가 빨라집니다.";

            case "프로그램은 입력을 받아 결과를 내놓는다":
                return "로그인 화면을 예로 들면 사용자가 아이디와 비밀번호를 입력하고(input), 프로그램이 저장된 정보와 비교한 뒤(process), 로그인 성공이나 실패 화면을 보여 줍니다(output). 복잡한 기능도 이 세 단계로 단순화할 수 있습니다. 새로운 앱 기능을 볼 때 입력·처리·출력을 찾아보는 습관을 들이면 구조가 빠르게 보입니다.";
            case "input()이 돌려주는 값은 문자열이다":
                return "input()이 문자열을 반환하는 것은 개발자가 그 입력의 의미를 결정할 수 있게 해 줍니다. 사용자가 01012345678을 입력했다면 숫자 계산용이 아니라 전화번호 문자열로 쓰는 것이 맞을 수 있습니다. 반대로 수량 10이라면 int로 바꿔 계산합니다. 즉 같은 숫자 모양의 입력도 목적에 따라 타입 선택이 달라집니다.";
            case "int(), float(), str()로 타입 바꾸기":
                return "형 변환을 할 때는 변환 후 무엇을 할지 생각해야 합니다. 개수는 int가 자연스럽고, 소수 비율은 float가 필요할 수 있으며, 화면 메시지 조합에서는 str이 필요할 수 있습니다. 변환 과정은 데이터 준비 단계이므로 입력을 받자마자 필요한 형식으로 정리해 두면 이후 코드가 단순해집니다.";
            case "입력받아 계산하는 프로그램":
                return "코드를 한 줄씩 입력·처리·출력으로 표시해 보세요. age = int(input(...))는 입력과 변환, next_age = age + 1은 처리, print(...)는 출력입니다. 이렇게 역할을 나누면 기능을 수정할 때 어느 줄을 바꿔야 하는지 쉽게 찾을 수 있습니다.";
            case "실습 A — 내 이름과 나이 입력받기":
                return "입력값을 여러 번 사용할 계획이라면 변수에 저장하는 것이 중요합니다. name을 한 번 입력받아 여러 문장에서 사용하면 사용자가 이름을 반복 입력할 필요가 없습니다. 실제 앱에서도 로그인한 사용자 이름을 한 번 받아 여러 화면에서 다시 표시하는 것과 비슷한 생각입니다.";
            case "실습 B — 두 숫자 더하기":
                return "덧셈 프로그램을 곱셈, 평균, 할인 계산으로 조금씩 바꾸면 같은 입력 구조를 재사용할 수 있습니다. 이런 작은 변형 연습은 문법 문제를 많이 푸는 것보다 ‘내가 원하는 기능으로 코드를 바꾸는 힘’을 키우는 데 도움이 됩니다.";
            case "확인 문제 — 입력과 형 변환":
                return "정답을 본 뒤 input → str → conversion → calculation의 흐름을 화살표로 그려 보세요. 값이 어느 단계에서 어떤 타입인지 표시하면 형 변환이 왜 필요한지 더 명확해집니다. 프로그램이 길어져도 데이터가 어디서 들어와 어떻게 바뀌는지 추적하는 방식은 같습니다.";
            case "챕터 5 정리 — 입력은 타입까지 확인한다":
                return "사용자 입력은 예상과 다를 수 있다는 생각을 항상 가지고 있어야 합니다. 빈 문자열, 글자, 너무 큰 숫자처럼 다양한 값이 들어올 수 있습니다. 지금은 변환 오류를 직접 경험하는 단계이고, 다음에는 조건과 예외 처리를 통해 잘못된 입력에 대응하는 방법을 배우게 됩니다.";

            case "오류는 ‘틀렸다’가 아니라 단서다":
                return "오류가 발생했을 때 가장 피해야 할 행동은 이유를 확인하지 않고 코드를 여러 군데 동시에 바꾸는 것입니다. 그러면 무엇 때문에 해결됐는지 알 수 없고 새로운 문제가 생길 수 있습니다. 오류 메시지와 최근 변경을 보고 한 가지 가설을 세운 뒤 작은 수정으로 확인하는 방식이 좋습니다.";
            case "초보가 먼저 알아둘 오류 네 가지":
                return "네 오류 이름은 문제를 찾는 출발점입니다. SyntaxError면 괄호·따옴표·콜론 같은 문법을, NameError면 이름 철자와 정의 위치를, TypeError면 타입을, ValueError면 실제 값 형식을 먼저 봅니다. 이 분류만 알아도 무작정 검색하는 것보다 훨씬 빠르게 원인을 좁힐 수 있습니다.";
            case "traceback에서 무엇부터 볼까?":
                return "traceback의 위쪽에는 여러 파일과 함수가 보일 수 있어서 처음에는 겁날 수 있습니다. 우선 가장 아래쪽의 오류 종류와 메시지를 읽고, 그 위에서 내가 작성한 파일 이름과 줄 번호를 찾으세요. 외부 라이브러리 내부 줄보다 내 코드에서 어떤 값을 넘겼는지를 먼저 확인하는 경우가 많습니다.";
            case "네 가지 오류를 구분해 보기":
                return "오류 예제를 직접 실행할 때는 한 번에 하나만 활성화하세요. 여러 오류가 동시에 있으면 Python은 보통 먼저 만난 문제에서 멈추므로 뒤쪽 오류는 보이지 않을 수 있습니다. 하나를 고친 뒤 다시 실행하면서 다음 오류를 보는 과정도 실제 디버깅에서 자주 경험합니다.";
            case "실습 A — 일부러 오류 만들고 복구하기":
                return "복구한 뒤에는 정상 결과까지 확인해야 실습이 끝납니다. 단순히 오류 메시지가 사라진 것만으로는 충분하지 않습니다. 프로그램이 원래 기대한 값을 출력하는지까지 봐야 합니다. 이것이 fix와 verify가 서로 다른 단계인 이유입니다.";
            case "실습 B — 디버깅 4단계 습관":
                return "디버깅 메모를 짧게 남기는 습관도 도움이 됩니다. ‘증상: 120012001200 출력 → 확인: price가 str → 수정: int(price) → 결과: 3600’처럼 적으면 문제 해결 과정을 스스로 정리할 수 있습니다. 나중에는 이런 기록이 팀원에게 버그 원인을 설명하는 데도 사용됩니다.";
            case "확인 문제 — 오류 이름으로 범위 좁히기":
                return "오류 문제에서 답을 맞힌 뒤에는 검색 문장을 직접 만들어 보세요. 예를 들어 ‘Python ValueError int 문자열 변환’처럼 오류 이름과 상황을 함께 적습니다. 개발자는 모든 오류 해결법을 외우기보다 정확한 정보를 가지고 검색하고 검증하는 능력을 많이 사용합니다.";
            case "챕터 6 정리 — 오류를 읽는 순서":
                return "오류를 읽는 순서를 습관화하세요. ① 오류 이름 ② 메시지 ③ 내 코드 줄 번호 ④ 그 줄의 실제 값과 타입 ⑤ 최근 변경 순서로 보면 좋습니다. 이 순서는 Python뿐 아니라 다른 프로그래밍 언어에서 오류를 조사할 때도 비슷하게 적용할 수 있습니다.";

            case "코드는 실제 계산 도구가 될 수 있다":
                return "계산식을 코드로 만들면 같은 규칙을 수백 번 반복해도 사람이 다시 계산할 필요가 없습니다. 예를 들어 영업 매출 합계, 할인 가격, 평균 방문 횟수도 변수와 연산자의 조합으로 표현할 수 있습니다. 정확한 계산 규칙을 한 번 코드로 표현하고 다양한 데이터에 반복 적용하는 것이 프로그래밍의 큰 장점입니다.";
            case "기본 산술 연산자":
                return "연산자를 사용할 때는 결과 타입도 생각해 보세요. Python에서 / 나눗셈은 보통 float 결과를 만듭니다. 10 / 2의 결과도 5.0처럼 보일 수 있습니다. 반면 //는 몫을 구합니다. 지금은 모든 세부 규칙을 외우지 말고 결과가 궁금하면 짧은 코드를 직접 실행해 확인하는 습관을 가지면 됩니다.";
            case "계산 순서와 괄호":
                return "실제 코드에서는 ‘수학적으로 맞다’뿐 아니라 ‘다른 사람이 바로 이해할 수 있다’도 중요합니다. 복잡한 할인식이나 세금 계산식은 괄호와 중간 변수를 사용해 단계를 나누는 편이 좋습니다. 한 줄을 짧게 만드는 것보다 계산 규칙이 분명하게 보이는 코드가 유지보수에 유리합니다.";
            case "가격 × 개수 = 총액":
                return "가격과 개수를 각각 변수로 두면 상품마다 다른 데이터를 넣어 같은 계산을 사용할 수 있습니다. total 변수에 결과를 저장해 두면 이후 할인, 세금, 출력 등 다음 단계에서 다시 사용할 수 있습니다. 즉 하나의 계산 결과가 다음 처리 단계의 입력이 되는 흐름을 경험하게 됩니다.";
            case "실습 A — 장보기 계산기":
                return "상품이 세 개, 네 개로 늘어나면 각 상품의 금액을 따로 계산하고 마지막에 합치는 방식이 이해하기 쉽습니다. 아직 list나 반복문을 배우지 않았으므로 지금은 변수를 여러 개 사용하지만, 다음 단계에서는 반복되는 구조를 더 간단하게 만드는 방법을 배우게 됩니다.";
            case "실습 B — 입력받는 계산기로 확장하기":
                return "입력 기능이 붙으면 프로그램을 매번 수정하지 않고도 다른 가격과 수량을 계산할 수 있습니다. 이 차이는 매우 중요합니다. 하드코딩된 값은 코드에 고정되어 있지만 사용자 입력은 실행할 때마다 달라집니다. 실제 앱이 다양한 사용자를 처리할 수 있는 이유도 이런 데이터 분리와 연결됩니다.";
            case "확인 문제 — 연산과 계산 순서":
                return "응용 문제를 풀 때는 한 번에 전체 식을 계산하지 말고 괄호와 우선순위에 따라 중간값을 적으세요. 2 + 3 * 4라면 먼저 3 * 4 = 12, 다음 2 + 12 = 14라고 나눕니다. 복잡한 코드에서도 중간값을 추적하는 습관이 오류를 줄여 줍니다.";
            case "챕터 7 정리 — 계산을 코드로 옮기는 법":
                return "계산을 코드로 옮길 때는 ① 필요한 값에 이름 붙이기 ② 계산식을 작은 단계로 나누기 ③ 중간 결과 확인하기 ④ 최종 결과 출력하기 순서를 사용하세요. 복잡한 업무 계산도 이 과정을 반복하면 프로그램으로 옮길 수 있습니다.";

            case "프로젝트는 코드를 쓰기 전에 요구사항부터 정한다":
                return "요구사항은 프로그램의 ‘완성 조건’과 연결됩니다. 지출 계산기라면 단순히 실행되는 것이 아니라 이름과 세 금액을 입력받고 정확한 합계를 보여 줘야 완성입니다. 요구사항이 명확하면 기능이 빠졌는지, 테스트가 무엇을 확인해야 하는지도 자연스럽게 정할 수 있습니다.";
            case "입력 → 처리 → 출력으로 문제 나누기":
                return "코드를 쓰기 전에 종이에 세 칸을 그려 input, process, output을 적어 보세요. 입력 칸에는 필요한 데이터, 처리 칸에는 계산 규칙, 출력 칸에는 사용자에게 보여 줄 결과를 적습니다. 이렇게 하면 아직 문법을 몰라도 프로그램 구조를 먼저 설계할 수 있습니다.";
            case "테스트 케이스와 경계값":
                return "좋은 테스트 케이스는 ‘왜 이 값을 넣는지’ 목적이 있습니다. 정상적인 금액은 기본 기능, 0원은 경계 상황, 문자열 입력은 잘못된 형식, 큰 숫자는 범위 문제를 확인합니다. 테스트는 단순히 많이 실행하는 것이 아니라 서로 다른 위험을 확인하도록 설계하는 것이 중요합니다.";
            case "오늘 지출 계산기 기본 버전":
                return "이 코드를 읽을 때 변수마다 역할을 표시해 보세요. name은 사용자 정보, coffee·lunch·transport는 입력 데이터, total은 처리 결과입니다. 마지막 print는 output입니다. 이렇게 역할을 붙이면 프로그램 전체가 한눈에 보이고, 새로운 기능을 추가할 위치도 찾기 쉬워집니다.";
            case "실습 A — 내 지출로 완성하기":
                return "기능 추가는 한 줄만 늘리는 일이 아닐 수 있습니다. snack이라는 항목을 추가하면 입력받는 줄, total 계산식, 필요하면 출력 문장까지 여러 곳이 함께 바뀝니다. 실제 개발에서도 기능 하나가 데이터·처리·화면 여러 부분에 영향을 줄 수 있으므로 변경 범위를 생각하는 습관이 중요합니다.";
            case "실습 B — 테스트 케이스 세 개 만들기":
                return "테스트 결과를 기록할 때 입력, expected result, actual result 세 칸으로 적어 보세요. 둘이 같으면 통과, 다르면 실패입니다. 실패했을 때는 무조건 코드가 틀렸다고 단정하지 말고 expected result 자체가 잘못 계산된 것은 아닌지도 확인해야 합니다.";
            case "확인 문제 — 프로그램을 설계하는 순서":
                return "설계가 익숙해지면 코드를 보기 전에 필요한 변수와 계산이 떠오릅니다. 예를 들어 평균 계산 프로그램이라면 숫자 입력 여러 개, 합계 계산, 개수로 나누기, 결과 출력이 필요하다는 구조를 먼저 생각할 수 있습니다. 문법은 그 구조를 실제 코드로 표현하는 수단입니다.";
            case "TRACK 01 완료 — 이제 무엇을 할 수 있나?":
                return "트랙 1을 끝낸 뒤에는 처음 보는 짧은 Python 코드에서 print, 변수, 타입, input, 계산식, 오류 이름을 구분해 보세요. 완벽히 이해하지 못하는 부분이 있어도 정확한 용어로 ‘이 expression이 왜 str인가요?’, ‘이 ValueError는 왜 나나요?’라고 질문할 수 있다면 충분히 좋은 출발점입니다. 다음 트랙에서는 조건문·반복문·함수를 배우며 프로그램이 훨씬 더 실제 도구처럼 움직이기 시작합니다.";
            default:
                return "";
        }
    }

    private static String deepDiveFor(Page page) {
        switch (page.title) {
            case "코딩은 컴퓨터에게 일을 시키는 방법이다":
                return "프로그램은 단순히 코드를 많이 적은 것이 아니라, 입력을 받고 어떤 규칙으로 처리한 뒤 결과를 만드는 절차를 가진 작업물입니다. 우리가 휴대폰에서 버튼을 누르면 화면이 바뀌는 것도 미리 작성된 프로그램이 그 입력을 받아 정해진 동작을 실행하기 때문입니다. 앞으로 코드를 볼 때는 ‘이 줄이 컴퓨터에게 무슨 일을 시키는가?’를 먼저 생각하면 됩니다.";
            case "소스 코드와 프로그래밍 언어":
                return "소스 코드는 사람이 수정하는 원본이고, 프로그래밍 언어는 그 원본을 적는 문법 체계입니다. 같은 기능도 Python, JavaScript, Java 등 서로 다른 언어로 만들 수 있습니다. 중요한 것은 언어 이름을 많이 외우는 것이 아니라 ‘언어마다 문법은 다르지만 변수·조건·반복·함수처럼 공통되는 생각이 있다’는 점입니다.";
            case "Python 코드는 누가 읽고 실행할까?":
                return "Python 파일에 적힌 글자는 그 자체로 화면을 움직이지 않습니다. Python 실행 환경이 코드를 읽고 의미를 해석한 다음 운영체제에 필요한 일을 요청합니다. 예를 들어 print는 Python이 처리한 결과를 화면 출력 기능과 연결합니다. 그래서 인터넷에서 ‘Python이 설치되어 있지 않아 실행되지 않는다’는 말을 보면, 코드를 읽어 줄 실행 환경이 없다는 뜻으로 이해할 수 있습니다.";
            case "첫 명령: print로 결과 보여주기":
                return "print는 학습용 장난감이 아니라 실제 프로그램에서도 로그를 확인하거나 계산 결과를 빠르게 검사할 때 자주 사용합니다. 괄호 안에는 글자뿐 아니라 숫자, 변수, 계산식도 넣을 수 있습니다. 따라서 print는 ‘지금 프로그램 안에 어떤 값이 들어 있는지 눈으로 확인하는 창’처럼 사용할 수 있습니다.";
            case "실습 A — 코드 한 글자 바꾸면 결과도 바뀐다":
                return "프로그램은 사용자의 의도를 추측하지 않고 적힌 코드 그대로 움직입니다. 따라서 작은 수정 하나가 결과를 어떻게 바꾸는지 확인하는 습관이 중요합니다. 실제 개발에서도 한 번에 많은 줄을 바꾸기보다 작은 단위로 수정하고 실행해 결과를 확인하면 오류 원인을 찾기 쉬워집니다.";
            case "실습 B — 예상하고 실행하기":
                return "실행 전 예측은 코드를 읽는 능력을 키우는 가장 좋은 방법 중 하나입니다. 예상이 틀렸다면 실패가 아니라 ‘내가 이해하지 못한 규칙이 어디 있는지’ 알려 주는 신호입니다. 특히 따옴표 유무처럼 작은 문법 차이가 결과를 어떻게 바꾸는지 비교하면 Python 문법이 빠르게 익숙해집니다.";
            case "확인 문제 — 코딩의 큰 그림":
                return "이 문제에서 중요한 것은 용어를 정확히 말하는 것과 실제 동작을 연결하는 것입니다. source code는 사람이 고치는 원본, programming language는 그 원본을 적는 규칙, output은 프로그램이 밖으로 보여 주는 결과입니다. 이 세 단어를 구분할 수 있으면 입문서나 인터넷 설명의 첫 문단을 읽을 수 있는 기반이 생깁니다.";
            case "챕터 1 정리 — 지금 알아야 할 것":
                return "챕터 1의 개념은 앞으로 모든 챕터의 바탕이 됩니다. 코드를 작성하고, 실행 환경이 읽고, 결과가 나온다는 흐름을 먼저 기억하세요. 이후 변수나 조건문을 배워도 결국 이 흐름 안에서 더 복잡한 명령을 표현하는 것입니다.";

            case "여러 줄의 코드는 ‘순서’가 중요하다":
                return "실행 순서는 프로그램의 결과를 결정합니다. 예를 들어 값을 먼저 만든 뒤 출력해야 하는데 출력부터 시도하면 아직 필요한 값이 준비되지 않았을 수 있습니다. 이후 조건문과 반복문을 배우면 실행 순서가 갈라지거나 되돌아오기도 하지만, 그때도 기본 기준은 ‘현재 어느 줄을 실행하고 있는가’입니다.";
            case "문장(statement)과 표현식(expression)":
                return "statement와 expression을 구분하면 긴 코드의 구조를 읽기 쉬워집니다. expression은 값을 만들어 내고, statement는 그 값을 저장하거나 출력하거나 다른 동작에 사용합니다. 실제 코드에서는 하나의 statement 안에 여러 expression이 들어갈 수 있으므로 ‘어떤 값이 먼저 만들어지는지’를 보는 습관이 중요합니다.";
            case "주석(comment)은 컴퓨터가 아닌 사람을 위한 메모":
                return "주석은 실행 결과에 영향을 주지 않지만 협업과 유지보수에서는 매우 중요합니다. 특히 ‘왜 이 방식을 선택했는지’, ‘이 값은 어떤 단위인지’, ‘나중에 무엇을 수정해야 하는지’를 남길 때 유용합니다. 반대로 코드와 똑같은 말을 반복하는 주석은 금방 낡아 오히려 혼란을 줄 수 있습니다.";
            case "실행 순서를 눈으로 확인하기":
                return "출력 순서를 바꾸는 실험은 프로그램 흐름을 눈으로 확인하는 가장 단순한 방법입니다. 실제 디버깅에서도 여러 위치에 임시 print를 넣어 ‘어디까지 실행됐는지’를 확인하기도 합니다. 지금은 세 줄짜리 예제지만 같은 원리는 수백 줄 프로그램에도 그대로 적용됩니다.";
            case "실습 A — 순서를 바꿔 결과를 통제하기":
                return "코드 줄을 이동하거나 주석 처리하면 실행 결과가 달라집니다. 이 연습을 통해 ‘코드의 위치도 의미의 일부’라는 점을 배웁니다. 실제로 설정을 먼저 해야 하는 코드, 값을 계산한 뒤 저장해야 하는 코드처럼 순서가 바뀌면 동작하지 않는 경우가 많습니다.";
            case "실습 B — 표현식은 먼저 계산된다":
                return "표현식은 큰 식 안에서도 일정한 계산 규칙을 따릅니다. 3 * 2 + 1처럼 연산자가 여러 개 있으면 우선순위에 따라 값을 만든 뒤 print가 최종값을 출력합니다. 나중에 함수 호출이나 조건식에서도 ‘먼저 값이 만들어지고 그 값이 사용된다’는 생각이 계속 등장합니다.";
            case "확인 문제 — 코드 흐름 읽기":
                return "코드 흐름을 읽는 목적은 외우지 않고도 결과를 추론하는 것입니다. 위에서 아래로 진행되는 순서와 한 줄 안에서 expression이 먼저 계산된다는 두 규칙만으로도 짧은 프로그램의 동작을 상당 부분 예측할 수 있습니다.";
            case "챕터 2 정리 — 코드를 읽는 첫 번째 방법":
                return "코드를 읽을 때 첫째 실행 순서를 보고, 둘째 각 줄이 어떤 statement인지 보고, 셋째 그 안에서 어떤 expression이 값으로 계산되는지 확인하세요. 이 세 단계는 이후 복잡한 코드를 읽을 때도 그대로 사용할 수 있는 기본 독해법입니다.";

            case "변수는 값을 다시 쓰기 위한 이름이다":
                return "변수의 핵심 목적은 값을 기억하고 의미 있는 이름으로 다시 사용하는 것입니다. 예를 들어 1200이라는 숫자만 보면 무엇인지 모르지만 price라는 이름이 붙으면 가격이라는 의미가 생깁니다. 변수는 단순 저장 기능이면서 동시에 코드에 의미를 붙이는 설명 도구이기도 합니다.";
            case "‘상자’ 비유보다 조금 더 정확한 설명":
                return "Python에서는 변수 이름과 값이 연결된다고 생각하면 이후 개념을 이해하기 쉽습니다. 같은 값에 여러 이름이 연결될 수도 있고, 같은 이름이 나중에 다른 값에 다시 연결될 수도 있습니다. 지금 당장 메모리 주소까지 알 필요는 없지만 ‘변수 이름 자체가 값은 아니다’라는 점은 기억해 두면 좋습니다.";
            case "= 는 ‘같다’가 아니라 대입이다":
                return "대입은 오른쪽 expression을 먼저 계산한 뒤 그 결과를 왼쪽 이름에 연결합니다. 그래서 total = price * count라면 먼저 price * count가 계산되고 그 결과가 total에 들어갑니다. 이 순서를 이해하면 복잡한 대입문도 오른쪽부터 읽어 나갈 수 있습니다.";
            case "변수를 만들고 다시 사용하기":
                return "변수를 사용하면 같은 값을 여러 곳에서 일관되게 쓸 수 있습니다. 값이 바뀌면 변수에 새 값을 넣는 한 곳만 수정해도 이후 계산에서 새 값이 사용됩니다. 설정값, 사용자 입력, 계산 중간 결과 등 실제 프로그램의 거의 모든 데이터가 이런 방식으로 이름을 갖고 움직입니다.";
            case "실습 A — 내 정보로 변수 바꾸기":
                return "개인 정보를 변수로 바꾸는 연습은 ‘변수 이름과 실제 값은 별개’라는 점을 확인하기 좋습니다. name이라는 변수에는 어떤 사람의 이름이든 들어갈 수 있습니다. 즉 변수 이름은 역할을 나타내고 실제 값은 실행할 때 달라질 수 있습니다.";
            case "실습 B — 좋은 변수 이름 만들기":
                return "좋은 변수 이름은 코드를 읽는 시간을 줄입니다. today_steps는 오늘 걸음 수라는 의미를 바로 전달하지만 x는 코드를 앞뒤로 읽어야 의미를 알 수 있습니다. 실제 협업에서는 변수 이름만 잘 지어도 주석을 줄이고 실수를 예방할 수 있습니다.";
            case "확인 문제 — 변수와 대입":
                return "변수 문제를 풀 때는 항상 ‘현재 이 이름에 마지막으로 대입된 값이 무엇인가?’를 확인하세요. 재대입이 여러 번 있으면 가장 최근 대입 이후의 값을 사용합니다. 이 습관이 나중에 프로그램 상태가 변하는 과정을 이해하는 기초가 됩니다.";
            case "챕터 3 정리 — 이름과 값의 관계":
                return "변수는 데이터를 저장하는 기능뿐 아니라 프로그램의 상태를 표현하는 방법입니다. 사용자 이름, 현재 점수, 총 금액처럼 프로그램이 기억해야 하는 정보를 변수로 표현합니다. 값을 바꾸면 프로그램의 상태도 바뀐다고 생각하면 다음 단계 학습에 도움이 됩니다.";

            case "값에는 종류가 있다":
                return "타입은 컴퓨터가 값을 어떻게 다룰지 결정합니다. 숫자는 산술 계산에 쓰고, 문자열은 글자를 다루며, bool은 참과 거짓을 표현합니다. 타입을 모르고 값을 섞으면 예상하지 못한 결과나 오류가 생기므로 데이터가 들어오는 순간부터 타입을 의식하는 습관이 중요합니다.";
            case "처음 알아야 할 네 가지 타입":
                return "int와 float는 둘 다 숫자지만 표현 가능한 값과 계산 결과가 다를 수 있습니다. str은 숫자처럼 보이는 글자도 문자열로 취급하고, bool은 조건을 판단할 때 핵심적으로 사용합니다. 나중에 list, dict 같은 새로운 타입을 배워도 ‘값마다 종류가 있고 종류에 따라 가능한 동작이 다르다’는 원리는 같습니다.";
            case "타입이 다르면 같은 +도 의미가 달라진다":
                return "연산자는 값의 타입을 보고 동작을 결정합니다. 숫자 + 숫자는 더하기, 문자열 + 문자열은 연결입니다. 따라서 오류가 났을 때 연산자만 보는 것이 아니라 왼쪽과 오른쪽 값의 타입을 함께 확인해야 합니다.";
            case "type()으로 값의 종류 확인하기":
                return "type()은 디버깅할 때도 유용합니다. 사용자가 입력한 값이나 계산 중간값이 예상한 타입인지 직접 출력해 확인할 수 있습니다. 추측보다 확인이 빠르다는 개발 습관을 배우는 첫 도구라고 생각하면 됩니다.";
            case "실습 A — 같은 모양, 다른 타입":
                return "문자열 \"10\"과 정수 10은 화면에 비슷하게 보이지만 가능한 연산이 다릅니다. 프로그램은 겉모양이 아니라 실제 타입에 따라 처리합니다. API나 파일에서 읽은 숫자가 문자열로 들어오는 경우가 흔하기 때문에 이 차이는 실제 개발에서도 자주 만납니다.";
            case "실습 B — 타입에 맞는 연산 고르기":
                return "TypeError가 나면 ‘연산자가 잘못됐나?’뿐 아니라 ‘값의 타입이 맞나?’를 확인해야 합니다. 특히 외부 입력을 받을 때는 값이 어떤 타입인지 확신하지 말고 확인하거나 변환하는 단계가 필요합니다.";
            case "확인 문제 — 타입 읽기":
                return "타입 문제를 풀 때는 값의 모양보다 문법을 봅니다. 따옴표가 있으면 문자열이고, 소수점이 있는 숫자 literal은 float이며, True/False는 bool입니다. 이런 기초 판별이 입력 처리와 조건문 학습의 바탕이 됩니다.";
            case "챕터 4 정리 — 값의 종류를 먼저 확인하자":
                return "타입은 프로그램의 약속입니다. 어떤 값이 들어오고 그 값에 어떤 동작을 할 수 있는지 결정합니다. 오류를 만났을 때 type()으로 확인하고 필요하면 변환하는 습관을 익히면 데이터 관련 문제를 훨씬 쉽게 해결할 수 있습니다.";

            case "프로그램은 입력을 받아 결과를 내놓는다":
                return "입력-처리-출력 구조는 계산기뿐 아니라 검색, 로그인, 주문, 사진 편집 같은 거의 모든 기능을 설명할 수 있습니다. 사용자가 무엇을 주는지, 프로그램이 내부에서 무엇을 하는지, 마지막에 무엇을 돌려주는지 세 단계로 나누면 복잡한 기능도 이해하기 쉬워집니다.";
            case "input()이 돌려주는 값은 문자열이다":
                return "input()이 문자열을 반환하는 이유는 키보드 입력이 기본적으로 글자 흐름으로 들어오기 때문입니다. 사용자가 123을 입력했더라도 프로그램이 그 값을 숫자로 사용할지 전화번호 같은 문자열로 사용할지는 개발자가 결정해야 합니다. 그래서 변환 과정이 따로 필요합니다.";
            case "int(), float(), str()로 타입 바꾸기":
                return "형 변환은 값을 사용할 목적에 맞게 준비하는 단계입니다. 계산하려면 숫자로, 화면 문장에 넣으려면 문자열로 바꾸기도 합니다. 변환은 아무 값에나 되는 것이 아니므로 입력값을 검증하는 개념으로 이어집니다.";
            case "입력받아 계산하는 프로그램":
                return "이 예제에는 작은 프로그램의 전체 흐름이 들어 있습니다. input으로 데이터를 받고, int로 준비하고, + 연산으로 처리하고, f-string과 print로 출력합니다. 앞으로 더 큰 프로그램을 만들어도 이런 단계들이 여러 개 연결되는 형태라고 볼 수 있습니다.";
            case "실습 A — 내 이름과 나이 입력받기":
                return "사용자 입력을 변수에 저장하면 프로그램이 실행할 때마다 다른 사람의 값을 다룰 수 있습니다. 코드 자체는 같지만 입력이 달라져 결과가 달라지는 것이 프로그램의 핵심 장점입니다.";
            case "실습 B — 두 숫자 더하기":
                return "두 숫자 계산기는 함수와 조건문이 없어도 입력 처리의 핵심을 연습할 수 있습니다. 특히 int와 float를 바꾸면서 어떤 입력을 허용할지 개발자가 결정한다는 점을 확인해 보세요.";
            case "확인 문제 — 입력과 형 변환":
                return "입력 처리 문제에서는 ‘지금 이 값이 어떤 타입인가?’와 ‘내가 다음에 하려는 연산에는 어떤 타입이 필요한가?’ 두 질문을 하면 됩니다. 이 두 질문으로 변환이 필요한 시점을 찾을 수 있습니다.";
            case "챕터 5 정리 — 입력은 타입까지 확인한다":
                return "외부에서 들어오는 값은 신뢰하지 않고 타입과 형식을 확인하는 습관이 중요합니다. 지금은 int 변환만 배우지만 나중에는 빈 값, 범위, 잘못된 형식까지 검사하게 됩니다. 이 과정이 입력 검증의 출발점입니다.";

            case "오류는 ‘틀렸다’가 아니라 단서다":
                return "디버깅은 개발 과정의 일부입니다. 오류 메시지를 없애는 것보다 왜 발생했는지 이해하고 같은 문제가 다시 생기지 않게 고치는 것이 중요합니다. 따라서 오류를 만나면 실패했다고 생각하기보다 프로그램이 제공한 추가 정보라고 보는 편이 좋습니다.";
            case "초보가 먼저 알아둘 오류 네 가지":
                return "오류 이름은 원인 범주를 빠르게 좁혀 줍니다. SyntaxError는 문법을, NameError는 이름을, TypeError는 값의 종류를, ValueError는 값의 형식을 먼저 살펴보라는 신호입니다. 검색할 때도 오류 이름을 그대로 포함하면 훨씬 정확한 자료를 찾을 수 있습니다.";
            case "traceback에서 무엇부터 볼까?":
                return "traceback은 함수가 여러 단계 호출되는 프로그램에서 오류가 어디를 거쳐 왔는지 보여 줍니다. 초보 단계에서는 마지막 줄의 오류 이름과 내 파일의 줄 번호를 우선 확인하세요. 이후 프로그램이 커지면 위쪽 호출 경로도 읽게 됩니다.";
            case "네 가지 오류를 구분해 보기":
                return "의도적으로 다른 오류를 만들어 보면 메시지 형식에 익숙해집니다. 오류를 볼 때 ‘이름’, ‘문제 줄’, ‘설명’을 구분해서 읽는 연습을 하세요. 실제 문제에서도 같은 형식이 반복되어 보입니다.";
            case "실습 A — 일부러 오류 만들고 복구하기":
                return "일부러 오류를 만든 뒤 한 부분씩 원상복구하면 원인과 결과의 관계를 명확히 볼 수 있습니다. 이 방식은 버그가 생겼을 때 최근 변경점을 되돌려 보는 실제 디버깅 방법과도 연결됩니다.";
            case "실습 B — 디버깅 4단계 습관":
                return "증상 확인, 원인 가설, 최소 수정, 재검증을 한 번의 디버깅 사이클로 생각하세요. 수정 후에는 반드시 다시 실행해 원래 문제가 해결됐는지 확인해야 합니다. 새로운 문제가 생기지 않았는지도 함께 보는 습관이 좋습니다.";
            case "확인 문제 — 오류 이름으로 범위 좁히기":
                return "오류 이름을 보는 목적은 정답을 외우는 것이 아니라 탐색 순서를 정하는 것입니다. NameError라면 이름 철자와 정의 위치를, ValueError라면 실제 값 형식을 먼저 보는 식으로 조사 범위를 좁힐 수 있습니다.";
            case "챕터 6 정리 — 오류를 읽는 순서":
                return "디버깅은 ‘오류를 읽는 기술’과 ‘작게 실험하는 습관’의 조합입니다. 메시지를 읽고, 가장 의심되는 한 부분만 바꾸고, 다시 실행해 확인하세요. 이 반복이 실제 개발에서 가장 자주 사용하는 문제 해결 방식 중 하나입니다.";

            case "코드는 실제 계산 도구가 될 수 있다":
                return "계산 규칙을 코드로 만들면 숫자만 바꿔 같은 작업을 반복할 수 있습니다. 사람이 매번 계산하는 대신 프로그램이 같은 규칙을 빠르고 일관되게 적용합니다. 회계, 통계, 게임 점수, 쇼핑 총액처럼 많은 기능이 결국 이런 계산의 조합입니다.";
            case "기본 산술 연산자":
                return "연산자는 expression을 만드는 핵심 도구입니다. +, -, *, / 외에도 나머지 %, 몫 //, 거듭제곱 **가 있으며 문제에 맞는 연산자를 선택합니다. 지금은 결과를 실행 전에 예상하고 실제 결과와 비교하는 습관을 함께 익히세요.";
            case "계산 순서와 괄호":
                return "우선순위 규칙을 모두 암기하기보다, 중요한 계산은 괄호로 의도를 명확히 표현하는 편이 안전합니다. 코드 리뷰에서도 괄호는 다른 사람이 계산 순서를 빠르게 이해하도록 도와 줍니다.";
            case "가격 × 개수 = 총액":
                return "price와 count를 분리하면 같은 계산식을 여러 상품에 재사용할 수 있습니다. total 같은 중간 변수는 계산 결과에 이름을 붙여 이후 코드에서 다시 사용하기 쉽게 만듭니다.";
            case "실습 A — 장보기 계산기":
                return "여러 상품 계산은 큰 계산을 작은 계산으로 나누는 연습입니다. 각 상품 금액을 따로 구한 뒤 전체를 더하면 오류가 났을 때 어느 부분이 잘못됐는지 찾기 쉽습니다.";
            case "실습 B — 입력받는 계산기로 확장하기":
                return "입력을 받는 순간 계산기는 특정 예제가 아니라 재사용 가능한 도구가 됩니다. 가격과 개수가 달라져도 코드를 수정하지 않고 입력만 바꾸면 되기 때문입니다. 이것이 데이터를 코드와 분리하는 가장 작은 예입니다.";
            case "확인 문제 — 연산과 계산 순서":
                return "연산 우선순위를 이해하면 복잡한 식을 정확히 읽을 수 있습니다. 헷갈릴 때는 식을 작은 부분으로 나눠 중간값을 계산하고, 필요하면 괄호를 추가해 의도를 분명하게 표현하세요.";
            case "챕터 7 정리 — 계산을 코드로 옮기는 법":
                return "계산 프로그램을 만들 때는 입력값에 이름을 붙이고, 필요한 식을 만들고, 중간 결과에도 의미 있는 이름을 붙인 뒤 출력합니다. 이 순서는 나중에 함수로 계산을 묶을 때도 그대로 이어집니다.";

            case "프로젝트는 코드를 쓰기 전에 요구사항부터 정한다":
                return "요구사항이 분명하면 무엇을 입력받고 어떤 결과를 내야 하는지 결정할 수 있습니다. 반대로 목표가 불분명하면 코드를 많이 작성해도 완성 기준을 알 수 없습니다. 작은 프로젝트라도 ‘누가 무엇을 입력하고 어떤 결과를 얻는가’를 먼저 적어 보세요.";
            case "입력 → 처리 → 출력으로 문제 나누기":
                return "알고리즘은 거창한 수학 공식만을 뜻하지 않습니다. 문제를 해결하기 위한 순서 자체가 알고리즘입니다. 지출 계산기에서는 금액을 받고, 더하고, 결과를 보여 주는 세 단계가 가장 단순한 알고리즘입니다.";
            case "테스트 케이스와 경계값":
                return "테스트 케이스는 프로그램이 맞는지 객관적으로 확인할 기준을 만듭니다. 정상값만 테스트하면 숨어 있는 오류를 놓칠 수 있으므로 0, 빈 값, 매우 큰 값, 잘못된 형식 같은 경계 상황도 확인합니다.";
            case "오늘 지출 계산기 기본 버전":
                return "프로젝트 코드를 읽을 때는 각 줄을 입력, 처리, 출력 중 어디에 속하는지 표시해 보세요. 이렇게 분류하면 나중에 기능을 추가할 때 어느 부분을 수정해야 하는지 쉽게 찾을 수 있습니다.";
            case "실습 A — 내 지출로 완성하기":
                return "기능을 하나 추가하려면 데이터 입력, 계산, 출력 중 어떤 부분이 함께 바뀌어야 하는지 살펴봐야 합니다. snack을 추가한다면 값을 입력받고 total에 더하고 필요하면 출력에도 표시해야 기능이 완성됩니다.";
            case "실습 B — 테스트 케이스 세 개 만들기":
                return "좋은 테스트는 단순히 여러 숫자를 넣는 것이 아니라 각각 왜 필요한지 목적이 있습니다. 0은 빈 지출 상황, 큰 값은 범위 문제, 잘못된 문자열은 입력 검증 문제를 확인합니다.";
            case "확인 문제 — 프로그램을 설계하는 순서":
                return "설계 문제에서는 코드 한 줄보다 문제를 어떤 단계로 나누는지가 중요합니다. 입력, 처리, 출력과 테스트 기준을 먼저 잡으면 코딩을 시작하기 전에 빠진 요구사항을 발견할 수 있습니다.";
            case "TRACK 01 완료 — 이제 무엇을 할 수 있나?":
                return "트랙 1에서 배운 용어들은 앞으로 검색하고 질문할 때 사용할 공통 언어입니다. 모르는 문제가 생겼을 때 ‘input 결과가 str인데 int로 바꾸다가 ValueError가 난다’처럼 구체적으로 말할 수 있으면 다른 사람이나 AI의 도움도 훨씬 정확하게 받을 수 있습니다.";
            default:
                return "";
        }
    }

    private static List<Page> vocabularyPages(int lesson, String lessonTitle) {
        List<Page> pages = new ArrayList<>();
        switch (lesson) {
            case 1:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — 프로그램·코드·언어",
                        "프로그램(program)\n"
                                + "컴퓨터가 어떤 목적을 수행하도록 만든 명령들의 묶음입니다. 계산기처럼 숫자를 계산하는 작은 것도 프로그램이고, 카카오톡이나 게임처럼 수많은 기능이 있는 앱도 프로그램입니다. 실제로는 입력을 받고, 정해진 규칙으로 처리하고, 결과를 보여 주는 구조를 가집니다. 우리가 앞으로 만드는 Python 파일도 아주 작은 프로그램입니다.\n\n"
                                + "프로그래밍(programming) / 코딩(coding)\n"
                                + "프로그램을 만들기 위해 문제를 나누고, 코드를 작성하고, 실행해 보고, 오류를 고치는 전체 작업을 뜻합니다. 일상에서는 programming과 coding을 비슷하게 쓰지만, programming은 설계와 테스트까지 포함하는 조금 더 넓은 말로 쓰이기도 합니다. ‘오늘 코딩한다’는 말은 실제로 코드를 작성하고 수정한다는 뜻으로 이해하면 됩니다.\n\n"
                                + "코드(code)\n"
                                + "컴퓨터에게 시킬 일을 프로그래밍 언어 규칙에 맞춰 적은 글입니다. 예를 들어 print(\"안녕\")도 한 줄의 코드입니다. 코드는 사람이 읽을 수 있도록 작성하지만, 실행 환경이 그 의미를 해석해 실제 동작으로 연결합니다.\n\n"
                                + "소스 코드(source code)\n"
                                + "사람이 직접 읽고 수정하는 프로그램 원본입니다. GitHub에서 ‘source’를 본다는 것은 보통 이 원본 코드를 본다는 뜻입니다. 버그를 고치거나 기능을 추가할 때 개발자는 소스 코드를 수정한 뒤 다시 실행하거나 빌드합니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — Python·인터프리터·실행 환경",
                        "프로그래밍 언어(programming language)\n"
                                + "코드를 적기 위한 문법과 규칙의 체계입니다. Python, JavaScript, Java, Kotlin 등이 각각 다른 프로그래밍 언어입니다. 언어마다 쓰는 기호와 문법은 다르지만 ‘값을 저장하고, 조건을 판단하고, 반복하고, 함수를 만든다’ 같은 기본 생각은 많이 공유합니다.\n\n"
                                + "Python\n"
                                + "읽기 쉬운 문법을 가진 프로그래밍 언어입니다. 자동화, 데이터 분석, 서버, AI 등 여러 분야에서 사용됩니다. 이 트랙에서는 Python을 이용해 코딩의 기본 개념을 익힙니다. 나중에 다른 언어를 배우더라도 변수·타입·조건 같은 생각은 그대로 도움이 됩니다.\n\n"
                                + "인터프리터(interpreter)\n"
                                + "Python 소스 코드를 읽고 실행해 주는 프로그램입니다. 우리가 python hello.py처럼 실행하면 Python 인터프리터가 파일을 읽고 각 명령을 처리합니다. ‘코드는 있는데 실행이 안 된다’는 상황에서 Python 인터프리터가 설치되어 있는지 확인하는 이유가 여기에 있습니다.\n\n"
                                + "실행 환경(runtime)\n"
                                + "코드가 실제로 동작할 수 있게 필요한 기능을 제공하는 환경을 넓게 부르는 말입니다. Python 프로그램이라면 Python 실행기와 관련 라이브러리 등이 포함될 수 있습니다. 인터넷에서 ‘runtime error’라는 말을 보면 프로그램을 실행하는 동안 문제가 생겼다는 뜻으로 이해하면 됩니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — run·execute·print·output·string",
                        "실행(run / execute)\n"
                                + "작성한 코드를 실제로 동작시키는 것입니다. 편집기에서 Run 버튼을 누르거나 터미널에서 python 파일이름.py를 입력하는 것이 실행의 예입니다. 코드를 수정한 뒤에는 다시 실행해 결과가 달라졌는지 확인합니다.\n\n"
                                + "print\n"
                                + "Python에서 값을 화면에 출력할 때 사용하는 함수입니다. print(\"안녕\")은 글자를 보여 주고, print(2 + 3)은 계산 결과 5를 보여 줍니다. 학습 중에는 변수 값이나 중간 계산 결과를 확인하는 디버깅 도구처럼도 자주 사용합니다.\n\n"
                                + "출력(output)\n"
                                + "프로그램이 처리한 뒤 밖으로 내보내는 결과입니다. 화면의 글자, 계산 결과, 파일 저장, 서버 응답 등 여러 형태가 output이 될 수 있습니다. 이 트랙에서는 주로 print로 화면에 보이는 결과를 output이라고 부릅니다.\n\n"
                                + "문자열(string / str)\n"
                                + "글자들의 묶음을 나타내는 데이터입니다. Python에서는 보통 \"안녕\"처럼 따옴표로 감쌉니다. 숫자처럼 보이는 \"123\"도 따옴표 안에 있으면 문자열입니다. 이름, 주소, 메시지처럼 글자로 다뤄야 하는 정보를 저장할 때 사용합니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — edit·result·prediction·quote·expression",
                        "수정(edit)\n"
                                + "이미 작성한 코드를 바꾸는 작업입니다. 글자 하나, 숫자 하나를 바꾸는 것도 edit입니다. 실제 개발에서는 수정한 뒤 반드시 다시 실행하거나 테스트해 영향이 원하는 대로인지 확인합니다.\n\n"
                                + "결과(result)\n"
                                + "어떤 실행이나 계산이 끝난 뒤 얻은 값이나 상태입니다. print로 보이는 출력도 result일 수 있고, 함수가 계산해서 돌려주는 값도 result라고 부를 수 있습니다. ‘expected result’는 미리 예상한 결과라는 뜻입니다.\n\n"
                                + "예측(prediction)\n"
                                + "실행하기 전에 어떤 결과가 나올지 미리 생각해 보는 것입니다. 코드를 읽는 실력을 키우려면 prediction을 한 뒤 실제 result와 비교하는 연습이 매우 효과적입니다.\n\n"
                                + "따옴표(quote)\n"
                                + "문자열의 시작과 끝을 표시하는 기호입니다. Python에서는 보통 \" \" 또는 ' '를 사용합니다. 따옴표가 있으면 2 + 3도 계산식이 아니라 글자로 취급될 수 있습니다.\n\n"
                                + "표현식(expression)\n"
                                + "계산되어 하나의 값이 되는 코드 조각입니다. 2 + 3, age + 1 등이 expression입니다. print 안에 expression을 넣으면 먼저 계산된 결과가 출력됩니다."));
                break;
            case 2:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — 실행 순서·흐름·순차 실행",
                        "실행 순서(execution order)\n"
                                + "코드가 실제로 어느 순서로 실행되는지를 뜻합니다. Python은 기본적으로 위에서 아래로 한 줄씩 진행합니다. 값을 만들기 전에 사용하면 문제가 생길 수 있으므로 ‘어느 줄이 먼저 실행되는가’를 보는 습관이 중요합니다.\n\n"
                                + "흐름(flow / control flow)\n"
                                + "프로그램의 실행이 어느 경로로 이동하는지 나타내는 말입니다. 지금은 위에서 아래로 단순하게 흐르지만, 나중에 if와 반복문을 배우면 흐름이 갈라지거나 되돌아옵니다. ‘control flow’라는 표현을 보면 실행 경로를 제어하는 구조라고 생각하면 됩니다.\n\n"
                                + "순차 실행(sequential execution)\n"
                                + "명령을 적힌 순서대로 하나씩 실행하는 방식입니다. print(\"A\"), print(\"B\") 순서라면 A가 먼저 나오고 B가 다음에 나옵니다. 가장 기본적인 실행 방식이며 이후 모든 제어 흐름의 기준점이 됩니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — statement·expression·value·evaluate",
                        "문장(statement)\n"
                                + "컴퓨터에게 한 가지 동작을 시키는 코드 단위입니다. 예를 들어 print(\"안녕\")은 화면에 값을 보여 주라는 statement입니다. assignment도 statement의 한 종류로 볼 수 있습니다. 긴 코드를 읽을 때 ‘이 줄은 어떤 일을 시키는 statement인가?’라고 물으면 구조가 보입니다.\n\n"
                                + "표현식(expression)\n"
                                + "계산되거나 평가되어 하나의 값이 되는 코드 조각입니다. 2 + 3은 5라는 값이 되고, age + 1도 현재 age에 따라 하나의 값이 됩니다. expression은 print 안이나 대입문의 오른쪽처럼 다른 코드 안에 들어가 자주 사용됩니다.\n\n"
                                + "값(value)\n"
                                + "프로그램이 실제로 다루는 데이터입니다. 10, 3.14, \"안녕\", True 등이 모두 값입니다. expression은 결국 이런 값을 만들어 내고, 변수는 값을 다시 사용할 수 있도록 이름을 붙여 줍니다.\n\n"
                                + "평가(evaluate / evaluation)\n"
                                + "expression을 계산해서 실제 값을 얻는 과정입니다. 2 + 3을 evaluate하면 5가 됩니다. ‘expression is evaluated first’라는 설명은 ‘이 식을 먼저 계산해 값으로 만든다’는 뜻입니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — comment·#·line",
                        "주석(comment)\n"
                                + "컴퓨터가 실행하지 않고 사람이 읽기 위해 남기는 메모입니다. 코드가 왜 필요한지, 어떤 주의점이 있는지 설명할 때 사용합니다. 실제 프로젝트에서는 팀원이 코드를 이해하거나 나중에 자신이 다시 볼 때 도움을 줍니다.\n\n"
                                + "# 기호\n"
                                + "Python에서 한 줄 주석을 시작할 때 쓰는 기호입니다. # 뒤의 내용은 그 줄에서 실행되지 않습니다. 예: # 가격은 원 단위. 코드를 잠시 실행하지 않게 실험할 때도 사용할 수 있지만, 오래된 코드를 무작정 주석으로 남겨 두는 것은 좋지 않을 수 있습니다.\n\n"
                                + "줄(line) / 줄 번호(line number)\n"
                                + "소스 코드에서 한 줄의 위치를 말합니다. 오류 메시지에서 ‘line 5’라고 나오면 다섯 번째 줄 근처를 먼저 확인합니다. 개발 도구에는 보통 줄 번호가 표시되어 있어 오류 위치를 찾는 데 사용됩니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — control flow·skip·order",
                        "제어 흐름(control flow)\n"
                                + "프로그램의 실행 순서를 결정하는 전체 흐름입니다. 지금은 위에서 아래로 순차 실행하지만, 다음 트랙의 if·반복문을 배우면 조건에 따라 다른 길로 가거나 같은 부분을 반복합니다. 인터넷 글에서 control flow를 보면 ‘코드가 어느 길로 실행되는가’라고 생각하면 됩니다.\n\n"
                                + "건너뛰기(skip)\n"
                                + "특정 코드가 실행되지 않고 넘어가는 상황을 설명할 때 자주 쓰는 일반 표현입니다. 주석은 항상 실행에서 skip되고, 나중에는 조건이 맞지 않을 때 코드 블록이 skip될 수도 있습니다.\n\n"
                                + "순서(order)\n"
                                + "코드나 작업이 배치된 앞뒤 관계입니다. execution order는 실제 실행 순서라는 뜻입니다. 값을 만든 뒤 사용하는 것처럼 코드에서는 order가 결과에 직접 영향을 줍니다."));
                break;
            case 3:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — variable·value·assignment",
                        "변수(variable)\n"
                                + "값을 다시 사용하기 위해 붙이는 이름입니다. price = 1200이라면 price라는 이름으로 1200을 다시 사용할 수 있습니다. 실제 프로그램에서는 사용자 이름, 점수, 총액처럼 계속 변할 수 있는 정보를 변수로 표현합니다.\n\n"
                                + "값(value)\n"
                                + "변수가 가리키는 실제 데이터입니다. variable과 value를 구분하면 코드를 읽기 쉬워집니다. name은 변수 이름이고 \"민수\"는 그 변수에 연결된 값입니다.\n\n"
                                + "대입(assignment)\n"
                                + "오른쪽에서 계산된 값을 왼쪽 변수 이름에 연결하는 동작입니다. age = 10을 실행하면 10이라는 값이 age에 대입됩니다. 이후 print(age)를 쓰면 10을 얻을 수 있습니다.\n\n"
                                + "대입 연산자(=)\n"
                                + "Python에서 assignment에 사용하는 기호입니다. 수학의 ‘양쪽이 같다’와 달리 오른쪽 결과를 왼쪽 이름에 넣는 방향성이 있습니다. total = price * count처럼 오른쪽 계산이 먼저 끝난 뒤 결과가 total에 대입됩니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — binding·reassignment·identifier",
                        "연결(binding)\n"
                                + "변수 이름이 어떤 값과 연결되어 있는지를 설명할 때 쓰는 말입니다. Python 설명서나 심화 글에서 ‘name is bound to a value’라는 문장을 만나면 ‘이 이름이 그 값을 가리킨다’고 이해하면 됩니다.\n\n"
                                + "재대입(reassignment)\n"
                                + "이미 사용 중인 변수에 새로운 값을 다시 대입하는 것입니다. age = 10 다음에 age = 11을 실행하면 이후 age는 11을 사용합니다. 프로그램의 상태가 바뀌는 가장 기본적인 모습입니다.\n\n"
                                + "식별자(identifier)\n"
                                + "변수, 함수 등 코드의 여러 대상을 구별하기 위해 붙이는 이름입니다. user_name, total_price 같은 이름이 identifier입니다. Python에는 숫자로 시작할 수 없다는 등 식별자 작성 규칙이 있습니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — variable name·snake_case·naming",
                        "변수명(variable name)\n"
                                + "변수에 붙인 실제 이름입니다. 같은 기능을 만들어도 변수명은 개발자가 정합니다. 의미가 분명한 이름을 사용하면 코드를 다시 읽을 때 ‘이 값이 무엇이었지?’를 추측할 필요가 줄어듭니다.\n\n"
                                + "snake_case\n"
                                + "여러 단어를 소문자와 밑줄로 연결하는 이름 표기법입니다. total_price, user_name처럼 씁니다. Python에서는 변수와 함수 이름에 snake_case를 사용하는 관례가 널리 쓰입니다.\n\n"
                                + "naming\n"
                                + "변수나 함수에 이름을 정하는 작업입니다. 좋은 naming은 짧기만 한 이름보다 역할을 정확히 드러냅니다. 예를 들어 a보다 item_count가 실제 프로그램에서는 이해하기 쉽습니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — state·current value·refactor",
                        "상태(state)\n"
                                + "프로그램이 현재 기억하고 있는 값들의 상황을 뜻합니다. score가 10에서 15로 바뀌면 프로그램의 상태도 바뀐 것입니다. 앱의 로그인 여부, 현재 화면, 장바구니 수량도 모두 state의 예가 될 수 있습니다.\n\n"
                                + "현재 값(current value)\n"
                                + "변수에 지금 연결되어 있는 최신 값을 뜻합니다. 재대입이 여러 번 있었으면 가장 최근 대입 이후의 값이 current value입니다. 디버깅할 때 print(variable)로 현재 값을 확인하는 경우가 많습니다.\n\n"
                                + "리팩터링(refactor)\n"
                                + "동작 결과는 유지하면서 코드 구조나 이름을 더 이해하기 좋게 고치는 작업입니다. city를 home_city로 더 명확하게 바꾸는 것처럼 이름 개선도 작은 refactor로 볼 수 있습니다."));
                break;
            case 4:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — data·type·int·float",
                        "데이터(data)\n"
                                + "프로그램이 저장하고 처리하는 정보입니다. 숫자, 글자, 사진, 날짜 등 모두 데이터가 될 수 있습니다. Python에서는 각각의 데이터가 어떤 종류인지 나타내는 타입을 가집니다.\n\n"
                                + "데이터 타입(data type) / 타입(type)\n"
                                + "값의 종류를 나타냅니다. 타입에 따라 가능한 동작이 달라집니다. 숫자는 계산할 수 있고 문자열은 글자를 이어 붙일 수 있습니다. 오류가 날 때 ‘이 값의 타입이 무엇인가?’를 확인하는 이유입니다.\n\n"
                                + "int(integer)\n"
                                + "소수점이 없는 정수 타입입니다. 0, 10, -3 등이 int입니다. 개수, 나이, 점수처럼 소수점이 필요 없는 값을 표현할 때 자주 사용합니다.\n\n"
                                + "float\n"
                                + "소수점을 표현할 수 있는 숫자 타입입니다. 3.14, 0.5 등이 float입니다. 비율이나 평균처럼 소수 계산이 필요한 값에 사용합니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — str·bool·operator",
                        "str(string)\n"
                                + "문자열 타입입니다. \"안녕하세요\"처럼 글자 묶음을 저장합니다. \"10\"처럼 숫자 모양이어도 따옴표 안에 있으면 str이므로 숫자 계산에 바로 사용할 수 없습니다. 이름, 주소, 메시지 등에 사용합니다.\n\n"
                                + "bool(boolean)\n"
                                + "참과 거짓을 나타내는 타입입니다. Python에서는 True와 False 두 값을 사용합니다. 지금은 단순한 값으로만 보지만, 다음 트랙에서 if 조건을 판단할 때 핵심적으로 사용합니다.\n\n"
                                + "연산자(operator)\n"
                                + "값에 계산이나 비교 같은 동작을 적용하는 기호입니다. +, -, *, / 등이 대표적입니다. 연산자는 값의 타입에 따라 가능한 동작이나 결과가 달라질 수 있습니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — type()·type mismatch·TypeError",
                        "type()\n"
                                + "Python에게 ‘이 값의 타입이 무엇이야?’라고 물어보는 함수입니다. type(10)은 int, type(\"10\")은 str을 알려 줍니다. 값이 예상과 다르게 움직일 때 디버깅용으로 자주 사용합니다.\n\n"
                                + "타입 불일치(type mismatch)\n"
                                + "서로 맞지 않는 타입을 함께 사용한 상황입니다. 예를 들어 숫자 10과 문자열 \"5\"를 +로 바로 더하려 하면 의도가 분명하지 않아 문제가 됩니다. 외부 입력을 받을 때 자주 만나는 개념입니다.\n\n"
                                + "TypeError\n"
                                + "현재 타입으로는 요청한 연산을 할 수 없을 때 흔히 발생하는 오류입니다. print(10 + \"5\")처럼 숫자와 문자열을 바로 더하려 할 때 볼 수 있습니다. 오류가 나오면 각 값의 type()을 확인해 보는 것이 좋은 첫 단계입니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — literal·concatenation·conversion",
                        "리터럴(literal)\n"
                                + "코드에 값을 직접 적어 놓은 표현입니다. 10, 3.14, \"안녕\", True가 각각 정수·실수·문자열·불리언 literal입니다. 변수와 달리 이름을 통해 가져오는 것이 아니라 코드에 값 자체가 적혀 있습니다.\n\n"
                                + "문자열 연결(concatenation)\n"
                                + "문자열 두 개를 이어 하나의 문자열로 만드는 작업입니다. \"안녕\" + \"하세요\"는 \"안녕하세요\"가 됩니다. 숫자 덧셈과 같은 + 기호를 쓰지만 타입이 문자열이면 의미가 달라집니다.\n\n"
                                + "변환(conversion)\n"
                                + "값을 다른 형태나 타입으로 바꾸는 작업을 넓게 부르는 말입니다. 다음 챕터에서 int(), float(), str()를 이용한 type conversion을 자세히 배웁니다."));
                break;
            case 5:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — input·process·output·I/O",
                        "입력(input)\n"
                                + "사용자, 파일, 센서, 인터넷 등 외부에서 프로그램 안으로 들어오는 값입니다. 키보드로 이름을 입력하는 것도 input입니다. 프로그램은 입력을 받아야 사용자마다 다른 결과를 만들 수 있습니다.\n\n"
                                + "처리(process)\n"
                                + "입력된 데이터를 계산하고, 비교하고, 변환하거나 저장하는 중간 작업입니다. 나이 입력에 1을 더해 내년 나이를 만드는 것이 처리의 예입니다.\n\n"
                                + "출력(output)\n"
                                + "처리가 끝난 뒤 프로그램이 밖으로 내놓는 결과입니다. 화면 글자, 파일, 소리, 네트워크 응답 등이 모두 출력이 될 수 있습니다.\n\n"
                                + "I/O(Input/Output)\n"
                                + "입력과 출력을 묶어 부르는 말입니다. ‘I/O 처리’라는 표현은 프로그램과 외부 세계 사이에서 데이터를 주고받는 작업을 뜻합니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — input()·return value·conversion",
                        "input()\n"
                                + "키보드 입력을 받아 문자열로 돌려주는 Python 함수입니다. name = input(\"이름: \")처럼 사용합니다. 사용자가 10을 입력해도 처음 결과는 문자열 \"10\"이라는 점이 중요합니다.\n\n"
                                + "반환값(return value)\n"
                                + "함수가 일을 한 뒤 호출한 곳으로 돌려주는 결과입니다. input()의 반환값은 사용자가 입력한 문자열입니다. 나중에 함수를 직접 만들 때도 ‘이 함수가 무엇을 return하는가?’가 중요한 질문이 됩니다.\n\n"
                                + "형 변환(type conversion)\n"
                                + "값을 한 타입에서 다른 타입으로 바꾸는 작업입니다. \"10\"을 숫자 10으로 바꾸면 계산할 수 있습니다. 외부 입력은 원하는 타입이 아닐 수 있어서 conversion이 자주 필요합니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — int()·float()·str()·f-string·ValueError",
                        "int() / float() / str()\n"
                                + "각각 값을 정수, 실수, 문자열로 변환할 때 사용하는 함수입니다. int(\"10\")은 10, float(\"3.5\")는 3.5, str(10)은 \"10\"을 만듭니다. 변환할 수 없는 형식이면 오류가 날 수 있습니다.\n\n"
                                + "f-string\n"
                                + "문자열 안에 변수나 계산 결과를 쉽게 넣는 Python 문법입니다. f\"총액은 {total}원\"처럼 사용합니다. 화면 메시지를 만들 때 매우 자주 쓰입니다.\n\n"
                                + "ValueError\n"
                                + "타입 자체는 변환 대상이 될 수 있지만 실제 값의 형식이 맞지 않을 때 발생하는 오류입니다. int(\"사과\")가 대표적인 예입니다. 사용자가 잘못된 값을 입력했을 때 자주 만나므로 나중에는 예외 처리로 다루게 됩니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — prompt·user input·validation",
                        "프롬프트(prompt)\n"
                                + "input()에서 사용자에게 무엇을 입력해야 하는지 보여 주는 안내 문구를 뜻하기도 합니다. input(\"나이: \")에서 \"나이: \"가 prompt입니다. AI에서 말하는 prompt와 문맥은 다르지만 ‘사용자에게 입력을 요청하는 문구’라는 공통점이 있습니다.\n\n"
                                + "사용자 입력(user input)\n"
                                + "사람이 프로그램에 직접 넣는 데이터입니다. 개발자는 user input이 항상 올바르다고 가정하면 안 됩니다. 숫자 칸에 글자를 넣거나 빈 값을 넣을 수 있기 때문입니다.\n\n"
                                + "입력 검증(validation)\n"
                                + "들어온 값이 허용된 형식과 범위인지 확인하는 과정입니다. 아직 본격적인 코드는 다음 단계에서 배우지만, int 변환이 실패하는 상황을 보면서 validation이 왜 필요한지 미리 이해할 수 있습니다."));
                break;
            case 6:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — error·exception·debugging",
                        "오류(error)\n"
                                + "프로그램이 의도대로 동작하지 않는 문제를 넓게 부르는 말입니다. 문법 오류, 잘못된 계산 결과, 실행 중 예외 등 여러 문제가 모두 error라고 불릴 수 있습니다. 중요한 것은 오류를 숨기는 것이 아니라 원인을 찾는 것입니다.\n\n"
                                + "예외(exception)\n"
                                + "프로그램 실행 중 정상적인 흐름을 깨뜨리는 문제를 나타내는 객체나 상황입니다. Python의 TypeError, ValueError 등이 exception 종류입니다. 나중에는 try/except를 사용해 일부 예외를 직접 처리할 수 있습니다.\n\n"
                                + "디버깅(debugging)\n"
                                + "문제의 원인을 찾고 수정한 뒤 다시 확인하는 과정입니다. 오류 메시지를 읽고, 값을 출력하고, 최근 변경을 확인하는 작업이 모두 debugging입니다. 실제 개발 시간의 큰 부분을 차지하는 중요한 기술입니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — traceback·line number·error message",
                        "traceback\n"
                                + "Python에서 예외가 발생했을 때 어떤 실행 경로를 거쳐 문제 지점에 도달했는지 보여 주는 정보입니다. 초보자는 마지막 오류 이름과 자기 코드의 줄 번호부터 보면 됩니다. 프로그램이 커지면 위쪽 호출 경로도 원인을 찾는 데 사용합니다.\n\n"
                                + "줄 번호(line number)\n"
                                + "소스 코드에서 문제와 관련된 위치를 알려 주는 번호입니다. traceback에 line 12라고 나오면 12번째 줄 주변을 먼저 확인합니다. 다만 실제 원인이 바로 이전 줄에 있을 수도 있으므로 주변도 함께 봅니다.\n\n"
                                + "오류 메시지(error / exception message)\n"
                                + "오류 종류 뒤에 붙어 구체적인 원인을 설명하는 문장입니다. 검색할 때 오류 이름과 메시지의 핵심 부분을 함께 넣으면 비슷한 사례를 찾기 쉽습니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — SyntaxError·NameError·TypeError·ValueError·reproduce·verify",
                        "SyntaxError\n문법 규칙이 깨졌을 때 발생합니다. 따옴표나 괄호가 닫히지 않은 경우가 대표적입니다. 코드 자체를 실행하기 전에 발견되기도 합니다.\n\n"
                                + "NameError\n정의하지 않은 변수나 이름을 사용했을 때 발생합니다. 철자가 틀렸거나 변수 생성 전 사용했는지 확인합니다.\n\n"
                                + "TypeError\n현재 값의 타입으로는 해당 연산을 할 수 없을 때 발생합니다. type()으로 실제 타입을 확인하는 것이 좋습니다.\n\n"
                                + "ValueError\n타입 변환 같은 작업에서 값의 형식이 적절하지 않을 때 발생합니다. int(\"천원\")이 예입니다.\n\n"
                                + "재현(reproduce) / 검증(verify)\n문제를 같은 조건에서 다시 발생시키는 것이 reproduce, 수정 후 실제로 해결됐는지 다시 실행해 확인하는 것이 verify입니다. 디버깅은 이 두 과정을 반복하면서 원인을 좁혀 갑니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — inspect·fix·debugging loop·hypothesis",
                        "검사(inspect)\n"
                                + "문제 원인을 찾기 위해 값, 타입, 실행 위치 등을 자세히 확인하는 작업입니다. print(type(value))처럼 프로그램 내부 상태를 확인하는 것도 inspect의 한 방법입니다.\n\n"
                                + "수정(fix)\n"
                                + "찾아낸 원인을 해결하도록 코드를 고치는 작업입니다. fix 뒤에는 반드시 verify가 필요합니다. 수정했다고 생각했지만 실제 문제는 남아 있을 수 있기 때문입니다.\n\n"
                                + "디버깅 반복(debugging loop)\n"
                                + "문제를 재현하고, 정보를 확인하고, 원인을 추측하고, 한 부분을 수정하고, 다시 검증하는 반복 과정을 말합니다. 한 번에 끝나지 않아도 정상입니다.\n\n"
                                + "가설(hypothesis)\n"
                                + "‘아마 price가 문자열이라 문제가 생겼을 것이다’처럼 원인에 대해 세우는 추측입니다. 좋은 디버깅은 가설을 작은 실험으로 확인하면서 진행합니다."));
                break;
            case 7:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — operator·arithmetic operator·expression",
                        "연산자(operator)\n"
                                + "값에 어떤 연산을 수행할지 나타내는 기호입니다. 산술, 비교, 논리 등 여러 종류가 있습니다. 이번 챕터에서는 숫자를 계산하는 산술 연산자를 중심으로 봅니다.\n\n"
                                + "산술 연산자(arithmetic operator)\n"
                                + "+ 더하기, - 빼기, * 곱하기, / 나누기처럼 숫자 계산에 사용하는 연산자입니다. Python에는 // 몫, % 나머지, ** 거듭제곱도 있습니다. 실제 계산기, 가격 계산, 점수 계산 등에 사용됩니다.\n\n"
                                + "표현식(expression)\n"
                                + "연산자와 값을 조합해 새로운 값을 만들어 내는 코드입니다. price * count는 두 변수 값을 곱해 하나의 숫자 값을 만듭니다. 그 결과를 total 같은 변수에 저장할 수 있습니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — precedence·parentheses·total",
                        "연산 우선순위(operator precedence)\n"
                                + "한 식에 여러 연산자가 있을 때 무엇을 먼저 계산할지 정하는 규칙입니다. 일반 수학처럼 곱셈·나눗셈이 덧셈·뺄셈보다 먼저입니다. 헷갈리는 식은 괄호로 의도를 명확히 표현하는 편이 안전합니다.\n\n"
                                + "괄호(parentheses)\n"
                                + "( ) 기호입니다. 계산 순서를 바꾸거나 함수 호출에 사용합니다. (2 + 3) * 4처럼 쓰면 괄호 안을 먼저 계산합니다. 코드를 읽는 사람에게 우선 계산 부분을 보여 주는 역할도 합니다.\n\n"
                                + "total\n"
                                + "‘전체 합계’를 뜻하는 영어 단어로 변수 이름에 자주 사용됩니다. total_price, total_count처럼 더 구체적인 이름으로 확장하기도 합니다. 특별한 Python 예약어가 아니라 개발자가 의미를 전달하려고 선택하는 이름입니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — discount·reuse·calculation",
                        "discount\n"
                                + "할인을 뜻하는 일반 영어 단어이며 예제 변수명에 자주 사용됩니다. discount_rate라면 할인 비율, discounted_price라면 할인된 가격처럼 이름을 만들 수 있습니다. 변수명을 통해 비즈니스 의미를 코드에 표현하는 예입니다.\n\n"
                                + "재사용(reuse)\n"
                                + "한 번 만든 코드나 계산 규칙을 값만 바꿔 여러 상황에 활용하는 것입니다. 가격과 개수를 input으로 받으면 같은 계산기 코드를 여러 상품에 사용할 수 있습니다. 나중에는 함수를 통해 재사용을 더 체계적으로 만듭니다.\n\n"
                                + "계산(calculation)\n"
                                + "값과 연산자를 이용해 새로운 결과를 만드는 작업입니다. 실제 프로그램에서는 계산식이 길어질 수 있으므로 중간 결과를 의미 있는 변수에 저장하면 이해와 디버깅이 쉬워집니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — //·%·**·rate·shipping",
                        "// 정수 나눗셈\n"
                                + "나눗셈 결과에서 소수 부분을 버린 몫을 구할 때 사용하는 연산자입니다. 7 // 2는 3입니다. 페이지 수 묶기나 개수 계산 등에서 사용할 수 있습니다.\n\n"
                                + "% 나머지 연산자\n"
                                + "나눗셈의 나머지를 구합니다. 7 % 2는 1입니다. 짝수·홀수 판단이나 주기적인 패턴을 만들 때 자주 사용합니다.\n\n"
                                + "** 거듭제곱 연산자\n"
                                + "2 ** 3은 2를 세 번 곱한 8입니다. 제곱 계산이나 수학식에서 사용합니다.\n\n"
                                + "비율(rate)\n"
                                + "할인율처럼 전체 중 어느 정도인지를 나타내는 값입니다. 10%는 계산할 때 0.10으로 표현할 수 있습니다. discount_rate 같은 변수 이름으로 자주 사용합니다.\n\n"
                                + "배송비(shipping)\n"
                                + "특별한 Python 용어는 아니지만 실제 쇼핑 계산 프로그램에서 자주 등장하는 데이터 이름입니다. shipping_fee처럼 의미가 분명한 변수명을 사용할 수 있습니다."));
                break;
            case 8:
                pages.add(vocab(lesson, lessonTitle, "용어집 1/4 — project·requirement·specification",
                        "프로젝트(project)\n"
                                + "하나의 목표를 가진 프로그램이나 기능을 실제로 완성하는 작업입니다. 학습 프로젝트는 배운 개념을 연결해 보는 연습이고, 실제 업무 프로젝트는 사용자 요구와 일정, 테스트까지 더 많은 요소를 포함합니다.\n\n"
                                + "요구사항(requirement)\n"
                                + "프로그램이 반드시 해야 하는 일을 적은 조건입니다. ‘이름과 지출 금액을 입력받아 총액을 보여 준다’가 요구사항의 예입니다. 코드를 쓰기 전에 요구사항을 정하면 무엇을 만들어야 완성인지 판단할 수 있습니다.\n\n"
                                + "명세(specification / spec)\n"
                                + "요구사항을 더 구체적으로 설명한 문서나 규칙입니다. 어떤 입력을 허용하는지, 결과 형식은 어떤지까지 적을 수 있습니다. 실제 개발에서는 사람마다 다르게 이해하지 않도록 기준을 맞추는 데 사용합니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 2/4 — algorithm·input-process-output·test",
                        "알고리즘(algorithm)\n"
                                + "문제를 해결하기 위한 단계적인 방법입니다. 꼭 어려운 수학일 필요는 없습니다. ‘세 금액을 입력받는다 → 더한다 → 총액을 출력한다’도 하나의 간단한 알고리즘입니다. 코드를 쓰기 전에 순서를 말로 적어 보는 것이 도움이 됩니다.\n\n"
                                + "입력-처리-출력(input-process-output)\n"
                                + "프로그램을 세 부분으로 나눠 생각하는 기본 모델입니다. 입력은 들어오는 데이터, 처리는 내부 계산, 출력은 결과입니다. 큰 문제를 이 세 칸으로 나누면 필요한 코드가 보이기 쉬워집니다.\n\n"
                                + "테스트(test)\n"
                                + "프로그램이 예상한 대로 동작하는지 확인하는 활동입니다. 단순히 한 번 실행해 보는 것보다 여러 입력과 예상 결과를 정해 확인하는 것이 더 좋은 테스트입니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 3/4 — test case·expected result·edge case·verify",
                        "테스트 케이스(test case)\n"
                                + "특정 입력과 그때 기대하는 결과를 한 묶음으로 정한 것입니다. coffee=0, lunch=9000, transport=1500이면 expected result=10500처럼 적을 수 있습니다. 오류를 고친 뒤 같은 테스트를 다시 실행해 확인하기도 합니다.\n\n"
                                + "기대 결과(expected result)\n"
                                + "테스트 전에 ‘이 결과가 나와야 맞다’고 정한 값입니다. 실제 결과(actual result)와 비교해 프로그램이 맞는지 판단합니다.\n\n"
                                + "경계값 / edge case\n"
                                + "0, 아주 큰 값, 빈 문자열, 잘못된 형식처럼 보통 상황의 가장자리나 특이한 입력입니다. 정상 입력만 테스트하면 놓칠 수 있는 문제를 찾기 위해 사용합니다.\n\n"
                                + "검증(verify) / 검산(check)\n"
                                + "수정한 코드나 계산 결과가 실제로 맞는지 다시 확인하는 과정입니다. 손계산, expected result, 반복 실행 등을 이용할 수 있습니다."));
                pages.add(vocab(lesson, lessonTitle, "용어집 4/4 — actual result·modify·regression·acceptance",
                        "실제 결과(actual result)\n"
                                + "프로그램을 실행했을 때 실제로 나온 값입니다. 테스트에서는 expected result와 actual result를 비교합니다. 둘이 다르면 코드 또는 기대값 중 무엇이 잘못됐는지 조사합니다.\n\n"
                                + "변형·수정(modify)\n"
                                + "기존 프로그램의 값이나 기능을 바꾸는 작업입니다. 학습에서는 예제에 snack 항목을 추가하는 것이 modify의 예입니다. 수정 후에는 기존 기능도 계속 정상인지 확인해야 합니다.\n\n"
                                + "회귀(regression)\n"
                                + "새로운 수정 때문에 예전에 잘 되던 기능이 다시 망가지는 문제를 뜻합니다. 지금 단계에서는 어려운 용어지만 ‘고친 뒤 다른 기능도 다시 확인하는 이유’와 연결해서 기억하면 됩니다.\n\n"
                                + "완료 기준(acceptance criteria)\n"
                                + "어떤 조건을 만족하면 기능이 완성됐다고 판단할지 정한 기준입니다. 예: ‘세 금액을 입력하면 정확한 총액이 나온다’. 요구사항을 테스트 가능한 형태로 바꾼 것이라고 이해하면 됩니다."));
                break;
            default:
                break;
        }
        return pages;
    }

    private static Page vocab(int lesson, String lessonTitle, String title, String body) {
        return new Page(
                lesson,
                lessonTitle,
                "용어집",
                title,
                body,
                "",
                null,
                null,
                null,
                null,
                "용어 뜻 → 어디에 쓰는지 → 실제 예 순서로 읽기"
        );
    }

    private static Page vocabularyPage(int lesson, String lessonTitle) {
        return new Page(
                lesson,
                lessonTitle,
                "용어집",
                "CHAPTER " + lesson + " 시작 전 용어집",
                vocabularyBody(lesson),
                "이 페이지의 단어를 전부 외우라는 뜻은 아닙니다. 먼저 한 번 읽어서 ‘이런 단어가 나오겠구나’ 정도만 알아두세요. 뒤에서 실제 코드와 함께 다시 만나면 훨씬 쉽게 기억됩니다. 모르는 단어를 인터넷이나 ChatGPT에 물어볼 때도 이 정확한 이름을 쓰면 원하는 설명을 찾기 쉬워집니다.",
                null,
                null,
                null,
                null,
                "용어는 외우기보다 실제 예에서 다시 만나며 익히기"
        );
    }

    private static String vocabularyBody(int lesson) {
        switch (lesson) {
            case 1:
                return "프로그램(program)\n컴퓨터가 해야 할 일을 순서대로 적어 놓은 것. 앱·게임·계산기도 모두 프로그램입니다.\n\n"
                        + "프로그래밍(programming) / 코딩(coding)\n프로그램을 만들기 위해 명령을 작성하고 고치는 일입니다.\n\n"
                        + "코드(code)\n컴퓨터에게 시킬 일을 프로그래밍 언어 규칙에 맞춰 적은 글입니다.\n\n"
                        + "소스 코드(source code)\n사람이 읽고 수정할 수 있는 프로그램 원문입니다.\n\n"
                        + "프로그래밍 언어(programming language)\nPython·JavaScript처럼 코드를 적는 규칙 체계입니다.\n\n"
                        + "Python\n이 트랙에서 사용할 프로그래밍 언어입니다. 읽기 쉬워 입문용으로 많이 사용됩니다.\n\n"
                        + "인터프리터(interpreter)\nPython 코드를 읽으면서 실행해 주는 프로그램이라고 이해하면 됩니다.\n\n"
                        + "실행 환경(runtime)\n코드가 실제로 돌아갈 수 있도록 필요한 기능을 제공하는 환경입니다.\n\n"
                        + "실행(run / execute)\n작성한 코드를 실제로 동작시키는 것입니다.\n\n"
                        + "print\n값이나 문장을 화면에 보여주는 Python 기능입니다.\n\n"
                        + "출력(output)\n프로그램이 계산하거나 처리한 뒤 밖으로 보여주는 결과입니다.\n\n"
                        + "문자열(string)\n글자들의 묶음입니다. Python에서는 보통 따옴표로 감쌉니다.";
            case 2:
                return "실행 순서(execution order)\n코드가 어떤 순서로 실행되는지를 뜻합니다. Python은 기본적으로 위에서 아래로 읽습니다.\n\n"
                        + "흐름(flow)\n프로그램의 실행이 어디에서 어디로 이동하는지를 말합니다.\n\n"
                        + "문장(statement)\n컴퓨터에게 한 가지 일을 시키는 코드 단위입니다. 예: print(\"안녕\").\n\n"
                        + "표현식(expression)\n계산되어 하나의 값이 되는 코드입니다. 예: 2 + 3.\n\n"
                        + "값(value)\n프로그램이 다루는 실제 데이터입니다. 5, \"안녕\", True 등이 값입니다.\n\n"
                        + "평가(evaluate)\n표현식을 계산해서 결과 값을 알아내는 과정입니다.\n\n"
                        + "주석(comment)\n컴퓨터가 실행하지 않고 사람이 읽기 위해 남기는 메모입니다. Python에서는 #을 사용합니다.\n\n"
                        + "순차 실행(sequential execution)\n코드를 적힌 순서대로 하나씩 실행하는 방식입니다.";
            case 3:
                return "변수(variable)\n값을 다시 사용하기 위해 붙이는 이름입니다.\n\n"
                        + "값(value)\n변수가 가리키거나 저장해서 사용하는 실제 데이터입니다.\n\n"
                        + "대입(assignment)\n오른쪽 값을 왼쪽 변수 이름에 연결하는 동작입니다. 예: age = 10.\n\n"
                        + "대입 연산자(=)\nPython에서 값을 변수에 넣을 때 사용하는 기호입니다. 수학의 ‘같다’와 역할이 다릅니다.\n\n"
                        + "연결(binding)\n변수 이름과 값이 연결되는 것을 설명할 때 쓰는 말입니다.\n\n"
                        + "재대입(reassignment)\n이미 있는 변수에 새 값을 다시 넣는 것입니다.\n\n"
                        + "식별자(identifier)\n변수·함수 등에 붙이는 이름을 뜻합니다.\n\n"
                        + "변수명(variable name)\n변수의 이름입니다. 무엇을 담았는지 알 수 있게 짓는 것이 좋습니다.\n\n"
                        + "snake_case\n단어 사이를 밑줄로 잇는 Python식 이름 표기입니다. 예: total_price.";
            case 4:
                return "데이터(data)\n프로그램이 저장하거나 계산하는 값입니다.\n\n"
                        + "데이터 타입(data type) / 타입(type)\n값의 종류입니다. 종류에 따라 가능한 연산이 달라집니다.\n\n"
                        + "int\n정수 타입입니다. 예: 3, -10, 100.\n\n"
                        + "float\n소수점이 있는 숫자 타입입니다. 예: 3.14.\n\n"
                        + "str(string)\n문자열 타입입니다. 예: \"안녕하세요\".\n\n"
                        + "bool(boolean)\n참 또는 거짓을 나타내는 타입입니다. True와 False 두 값이 있습니다.\n\n"
                        + "연산자(operator)\n값을 계산하거나 비교할 때 사용하는 기호입니다. 예: +, -, *.\n\n"
                        + "type()\n값의 타입을 확인하는 Python 기능입니다.\n\n"
                        + "타입 불일치(type mismatch)\n서로 맞지 않는 타입을 함께 사용한 상황입니다.\n\n"
                        + "TypeError\n타입 때문에 할 수 없는 연산을 시도했을 때 흔히 만나는 오류입니다.";
            case 5:
                return "입력(input)\n사용자나 다른 곳에서 프로그램 안으로 들어오는 값입니다.\n\n"
                        + "출력(output)\n프로그램이 처리한 뒤 밖으로 보여주는 결과입니다.\n\n"
                        + "처리(process)\n입력된 값을 계산·변환·저장하는 중간 작업입니다.\n\n"
                        + "I/O(Input/Output)\n입력과 출력을 함께 부르는 말입니다.\n\n"
                        + "input()\n키보드 입력을 받는 Python 기능입니다. 결과는 문자열입니다.\n\n"
                        + "반환값(return value)\n함수가 일을 한 뒤 돌려주는 결과 값입니다. input()도 입력받은 글자를 반환합니다.\n\n"
                        + "형 변환(type conversion)\n값을 다른 타입으로 바꾸는 작업입니다.\n\n"
                        + "int() / float() / str()\n각각 정수·실수·문자열로 바꾸는 데 사용하는 기능입니다.\n\n"
                        + "f-string\n문자열 안에 변수 값을 쉽게 넣는 Python 문법입니다.\n\n"
                        + "ValueError\n형식에 맞지 않는 값을 변환하려 할 때 흔히 나는 오류입니다.";
            case 6:
                return "오류(error)\n프로그램이 의도대로 실행되지 않는 문제를 넓게 부르는 말입니다.\n\n"
                        + "예외(exception)\n실행 중 발생해 정상 흐름을 깨뜨리는 문제를 말합니다.\n\n"
                        + "디버깅(debugging)\n오류 원인을 찾고 고치는 과정입니다.\n\n"
                        + "traceback\n오류가 어디를 거쳐 발생했는지 보여주는 Python의 실행 경로 정보입니다.\n\n"
                        + "line number\n오류가 관련된 코드 줄 번호입니다.\n\n"
                        + "SyntaxError\nPython 문법이 잘못됐을 때 나는 오류입니다.\n\n"
                        + "NameError\n정의되지 않은 이름을 사용했을 때 나는 오류입니다.\n\n"
                        + "TypeError\n타입이 맞지 않는 연산을 했을 때 나는 오류입니다.\n\n"
                        + "ValueError\n타입은 맞을 수 있지만 값의 형식이 맞지 않을 때 나는 오류입니다.\n\n"
                        + "재현(reproduce)\n같은 문제를 다시 발생시켜 원인을 조사할 수 있게 만드는 것입니다.\n\n"
                        + "검증(verify)\n수정 뒤 문제가 실제로 해결됐는지 다시 확인하는 것입니다.";
            case 7:
                return "연산자(operator)\n값을 계산하거나 비교할 때 쓰는 기호입니다.\n\n"
                        + "산술 연산자(arithmetic operator)\n숫자 계산용 연산자입니다. +, -, *, / 등이 있습니다.\n\n"
                        + "표현식(expression)\n계산되어 하나의 값이 되는 코드입니다.\n\n"
                        + "연산 우선순위(operator precedence)\n여러 연산이 있을 때 무엇을 먼저 계산할지 정하는 규칙입니다.\n\n"
                        + "괄호(parentheses)\n계산 순서를 분명하게 지정하거나 함수 호출에 사용하는 ( ) 기호입니다.\n\n"
                        + "total\n총합을 담는 변수 이름으로 자주 쓰는 영어 단어입니다.\n\n"
                        + "discount\n할인을 뜻합니다. 할인 계산 예제에서 자주 보게 됩니다.\n\n"
                        + "재사용(reuse)\n한 번 만든 코드를 값만 바꿔 여러 번 활용하는 것입니다.";
            case 8:
                return "프로젝트(project)\n하나의 목적을 가진 프로그램이나 기능을 실제로 완성해 보는 작업입니다.\n\n"
                        + "요구사항(requirement)\n프로그램이 무엇을 해야 하는지 적은 조건입니다.\n\n"
                        + "명세(specification / spec)\n프로그램의 동작 방식과 조건을 더 구체적으로 적은 설명입니다.\n\n"
                        + "알고리즘(algorithm)\n문제를 해결하기 위한 단계적인 방법입니다.\n\n"
                        + "입력-처리-출력(input-process-output)\n프로그램을 세 부분으로 나눠 생각하는 기본 모델입니다.\n\n"
                        + "테스트(test)\n프로그램이 예상대로 동작하는지 확인하는 활동입니다.\n\n"
                        + "테스트 케이스(test case)\n특정 입력과 그때 기대하는 결과의 한 묶음입니다.\n\n"
                        + "기대 결과(expected result)\n테스트에서 나와야 한다고 미리 정한 결과입니다.\n\n"
                        + "경계값 / edge case\n0, 아주 큰 값, 빈 값처럼 평범하지 않은 가장자리 상황입니다.\n\n"
                        + "검산(check)\n프로그램 결과가 손계산이나 예상과 맞는지 다시 확인하는 것입니다.";
            default:
                return "";
        }
    }

    private static String easyExplanationFor(Page page) {
        switch (page.title) {
            case "코딩은 컴퓨터에게 일을 시키는 방법이다":
                return "아주 쉽게 말하면 컴퓨터는 눈치가 없습니다. ‘알아서 해 줘’라고 말하면 아무것도 못 합니다. 그래서 사람이 ‘첫째 이것을 하고, 둘째 이것을 해’라고 아주 정확하게 적어 줘야 합니다. 그 지시문이 코드이고, 지시문을 여러 개 모아 실제 일을 하게 만든 것이 프로그램입니다. 리모컨 버튼을 누르면 TV가 정해진 일을 하는 것처럼, 코드도 컴퓨터가 할 일을 미리 정해 놓은 설명서라고 생각하면 됩니다.";
            case "소스 코드와 프로그래밍 언어":
                return "우리가 한글로 글을 쓰려면 한글 문법을 쓰듯이, 컴퓨터 프로그램도 정해진 언어 규칙으로 적습니다. Python은 그 규칙 중 하나입니다. ‘소스 코드’는 아직 사람이 읽고 고칠 수 있는 원본 글입니다. 인터넷에서 ‘소스 공개’, ‘소스 수정’, ‘코드 리뷰’ 같은 말을 보면 바로 이 원본 코드를 이야기하는 경우가 많습니다.";
            case "Python 코드는 누가 읽고 실행할까?":
                return "내가 Python으로 print(\"안녕\")이라고 적었다고 컴퓨터 부품이 그 글자를 바로 알아듣는 것은 아닙니다. 중간에서 Python 실행 프로그램이 이 글을 읽고 ‘아, 화면에 안녕을 보여주라는 뜻이구나’ 하고 실제 동작을 시킵니다. 이 중간 역할을 하는 것을 입문 단계에서는 interpreter라고 기억하면 됩니다. runtime은 ‘이 코드가 실제로 움직일 수 있게 마련된 실행 환경’ 정도로 생각하면 충분합니다.";
            case "첫 명령: print로 결과 보여주기":
                return "print는 ‘이 값을 화면에 보여 줘’라는 명령입니다. print(2 + 3)이라고 쓰면 먼저 2와 3을 더해 5를 만든 다음 5를 보여 줍니다. 반대로 print(\"2 + 3\")처럼 따옴표로 감싸면 계산하지 않고 글자 ‘2 + 3’을 그대로 보여 줍니다. 따옴표가 있느냐 없느냐가 왜 중요한지 여기서 처음 확인할 수 있습니다.";
            case "실습 A — 코드 한 글자 바꾸면 결과도 바뀐다":
                return "이 연습에서 중요한 것은 정답이 아닙니다. 내가 코드를 조금 바꾸면 컴퓨터 결과도 정확히 그에 맞춰 바뀐다는 감각을 만드는 것이 목적입니다. 코딩은 ‘읽기만 하는 공부’보다 ‘바꾸기 → 실행하기 → 결과 보기’를 계속 반복해야 빨리 익숙해집니다.";
            case "실습 B — 예상하고 실행하기":
                return "실행 전에 결과를 먼저 예상하면 머릿속에서 코드를 한 번 실행해 보는 연습이 됩니다. 실제 결과가 예상과 다르면 바로 ‘내가 어느 부분을 잘못 이해했지?’라는 좋은 질문이 생깁니다. 숙련된 개발자도 코드를 읽을 때 이런 식으로 결과를 계속 예상합니다.";
            case "확인 문제 — 코딩의 큰 그림":
                return "이 문제는 외우기 시험이 아닙니다. 나중에 인터넷에서 source code, programming language 같은 말을 봤을 때 ‘아, 저 말이 대충 무엇을 가리키는지 안다’ 정도가 목표입니다. 정답은 바로 다음 장에서 아주 쉽게 다시 풀어 설명합니다.";
            case "챕터 1 정리 — 지금 알아야 할 것":
                return "챕터 1에서 가장 중요한 한 문장은 ‘사람이 코드를 쓰고, Python 실행 환경이 그 코드를 읽어 실제 결과를 만든다’입니다. 아직 CPU가 내부에서 어떻게 움직이는지 몰라도 괜찮습니다. 지금 필요한 것은 코드가 원인이고 실행 결과가 결과라는 연결을 이해하는 것입니다.";

            case "여러 줄의 코드는 ‘순서’가 중요하다":
                return "요리 레시피에서 ‘라면을 끓인 뒤 물을 넣는다’처럼 순서가 뒤집히면 이상해집니다. 코드도 비슷합니다. 특별한 기능을 배우기 전에는 위에 적힌 일이 먼저, 아래에 적힌 일이 나중에 일어난다고 생각하면 됩니다.";
            case "문장(statement)과 표현식(expression)":
                return "statement는 ‘무엇을 해!’라고 시키는 한 덩어리이고, expression은 ‘계산하면 무엇이 되지?’에 해당하는 부분입니다. print(2 + 3)을 보면 print(...) 전체는 화면에 보여 주라는 명령이고, 2 + 3은 계산하면 5가 되는 표현식입니다. 이 둘을 구분하면 긴 코드를 볼 때도 구조가 조금씩 보입니다.";
            case "주석(comment)은 컴퓨터가 아닌 사람을 위한 메모":
                return "# 뒤의 글은 컴퓨터에게 하는 명령이 아니라 사람에게 남기는 메모입니다. ‘왜 이 코드를 넣었지?’를 나중에 기억하기 위한 포스트잇이라고 생각하면 됩니다. 코드 자체만 봐도 알 수 있는 말을 반복하기보다 이유를 남기는 주석이 더 도움이 됩니다.";
            case "실행 순서를 눈으로 확인하기":
                return "세 줄이 있으면 첫 줄 실행, 그다음 둘째 줄, 마지막 셋째 줄처럼 움직입니다. 줄 위치를 바꾸면 결과 순서도 바뀝니다. 아주 단순해 보여도 이후 조건문·반복문을 배울 때 ‘실행 흐름’의 기준점이 됩니다.";
            case "실습 A — 순서를 바꿔 결과를 통제하기":
                return "여기서는 명령의 내용보다 ‘어느 줄이 먼저 실행되느냐’를 직접 조절하는 연습입니다. 한 줄을 위로 올리거나 #으로 꺼 보면 실행 흐름을 내가 통제할 수 있다는 감각이 생깁니다.";
            case "실습 B — 표현식은 먼저 계산된다":
                return "print 안에 계산식이 있으면 컴퓨터는 먼저 계산해서 값을 만든 뒤 그 값을 print에게 넘깁니다. 그래서 print(3 * 2 + 1)은 글자 그대로를 보여 주는 것이 아니라 계산된 숫자를 보여 줍니다.";
            case "확인 문제 — 코드 흐름 읽기":
                return "코드를 위에서 아래로 읽고, 한 줄 안에서는 ‘먼저 계산될 부분’과 ‘그 결과로 할 일’을 구분할 수 있는지 확인하는 문제입니다. 다음 장에서 각 문제를 왜 그렇게 푸는지 한 단계씩 설명합니다.";
            case "챕터 2 정리 — 코드를 읽는 첫 번째 방법":
                return "긴 코드를 한꺼번에 이해하려 하지 말고 위에서 아래로 한 줄씩 보세요. ‘이 줄은 무슨 일을 시키지?’와 ‘이 줄 안에서 어떤 값이 계산되지?’ 두 질문만 해도 초보자가 코드를 읽는 힘이 크게 좋아집니다.";

            case "변수는 값을 다시 쓰기 위한 이름이다":
                return "책상에 물건이 여러 개 있을 때 ‘저것’이라고만 부르면 헷갈립니다. ‘연필통’, ‘공책’처럼 이름을 붙이면 다시 찾기 쉽습니다. 변수도 값에 이름을 붙여 나중에 다시 쓰기 위한 장치입니다.";
            case "‘상자’ 비유보다 조금 더 정확한 설명":
                return "처음에는 ‘변수라는 상자 안에 값이 들어 있다’고 생각해도 됩니다. 나중에 더 정확한 글을 읽으면 ‘이름이 값에 연결된다’라는 표현을 보게 됩니다. 둘 다 지금 단계에서는 같은 방향을 설명합니다. name이라는 이름을 보면 프로그램이 연결된 값을 찾아 쓰는 것입니다.";
            case "= 는 ‘같다’가 아니라 대입이다":
                return "수학에서 1 + 1 = 2의 =는 양쪽이 같다는 뜻입니다. 하지만 age = 37은 ‘오른쪽 37을 age라는 이름에 넣어 둬’라는 명령에 가깝습니다. 그래서 다음 줄에서 age = 38을 실행하면 이제 age를 사용했을 때 38이 나옵니다.";
            case "변수를 만들고 다시 사용하기":
                return "변수를 한 번 만들면 같은 값을 여기저기 다시 적지 않아도 됩니다. age라는 값이 바뀌면 age를 사용하는 계산도 새 값을 따라갑니다. 이것이 프로그램을 수정하기 쉽게 만드는 가장 기본적인 이유 중 하나입니다.";
            case "실습 A — 내 정보로 변수 바꾸기":
                return "예제 속 남의 값 대신 내 값으로 바꿔 보면 변수 이름과 실제 값이 따로라는 사실이 더 잘 보입니다. 변수 이름을 home_city처럼 바꾸면 그 이름을 사용하는 곳도 함께 바꿔야 합니다.";
            case "실습 B — 좋은 변수 이름 만들기":
                return "a, b처럼 짧은 이름은 간단한 수학 문제에는 쓸 수 있지만 실제 프로그램에서는 무엇인지 금방 잊습니다. total_price처럼 뜻이 보이는 이름을 쓰면 며칠 뒤 다시 봐도 이해하기 쉽습니다.";
            case "확인 문제 — 변수와 대입":
                return "변수는 ‘이름’, 값은 ‘그 이름이 가리키는 실제 데이터’, =는 ‘값을 연결하는 동작’이라는 세 가지를 구분할 수 있는지 보는 문제입니다. 다음 장의 풀이를 보고 자기 말로 다시 설명해 보세요.";
            case "챕터 3 정리 — 이름과 값의 관계":
                return "코드를 볼 때 name = \"민수\"가 나오면 ‘name이라는 이름에 민수라는 값을 연결했다’라고 읽으면 됩니다. 변수는 프로그램이 값을 기억하고 다시 사용하는 가장 기본적인 방법입니다.";

            case "값에는 종류가 있다":
                return "사람에게 ‘10’과 ‘열’이 비슷한 뜻일 수 있어도 컴퓨터는 값의 종류를 엄격히 구분합니다. 숫자 10은 계산할 수 있지만 글자 \"10\"은 처음에는 문자 데이터입니다. 이 차이를 알려 주는 것이 타입입니다.";
            case "처음 알아야 할 네 가지 타입":
                return "int는 소수점 없는 숫자, float는 소수점 숫자, str은 글자, bool은 맞다/아니다를 나타냅니다. 이 네 종류만 확실히 구분해도 초반 Python 오류의 상당수를 이해할 수 있습니다.";
            case "타입이 다르면 같은 +도 의미가 달라진다":
                return "+ 기호는 무조건 숫자 덧셈만 뜻하지 않습니다. 숫자 사이에서는 더하기지만 문자열 사이에서는 글자를 이어 붙입니다. 그래서 컴퓨터가 ‘이 값은 숫자인가 글자인가’를 아는 것이 중요합니다.";
            case "type()으로 값의 종류 확인하기":
                return "값의 종류가 헷갈릴 때 추측하지 말고 type()으로 물어보면 됩니다. 마치 물건 상자에 붙은 라벨을 확인하는 것처럼, Python에게 ‘이 값이 무슨 타입이야?’라고 직접 확인하는 도구입니다.";
            case "실습 A — 같은 모양, 다른 타입":
                return "10과 \"10\"은 화면에서 비슷해 보여도 하나는 숫자, 하나는 글자입니다. 직접 더하거나 이어 붙여 보면 두 값이 완전히 다르게 다뤄진다는 것을 알 수 있습니다.";
            case "실습 B — 타입에 맞는 연산 고르기":
                return "프로그램 오류가 났을 때 ‘이 값의 타입이 내가 생각한 것과 같은가?’를 확인하는 습관을 만드는 연습입니다. TypeError가 보이면 먼저 값 종류가 맞는지 의심해 볼 수 있습니다.";
            case "확인 문제 — 타입 읽기":
                return "따옴표가 있으면 문자열, 소수점이 있는 숫자는 float, True/False는 bool이라는 기본 구분을 확인합니다. 다음 장에서 왜 그런지 다시 아주 쉽게 설명합니다.";
            case "챕터 4 정리 — 값의 종류를 먼저 확인하자":
                return "코드에서 값이 이상하게 움직일 때는 먼저 타입을 확인하세요. 사람이 보기에는 비슷한 값도 컴퓨터에게는 전혀 다른 종류일 수 있습니다. type()은 이럴 때 바로 확인하는 도구입니다.";

            case "프로그램은 입력을 받아 결과를 내놓는다":
                return "자판기에 돈과 버튼 선택을 넣으면 음료가 나옵니다. 프로그램도 비슷합니다. 무엇인가 들어오고(input), 안에서 처리하고(process), 결과가 나옵니다(output). 이 세 칸으로 생각하면 복잡한 앱도 처음 구조를 잡기 쉬워집니다.";
            case "input()이 돌려주는 값은 문자열이다":
                return "키보드로 10을 쳐도 input()은 처음에 글자 \"10\"으로 받습니다. 그래서 10 + 5 같은 숫자 계산을 하려면 글자를 진짜 숫자로 바꿔 줘야 합니다. 초보자가 자주 막히는 지점이라 꼭 기억할 만합니다.";
            case "int(), float(), str()로 타입 바꾸기":
                return "형 변환은 옷을 갈아입히는 것과 비슷합니다. \"10\"이라는 문자열을 int()에 넣으면 계산할 수 있는 숫자 10으로 바뀝니다. 하지만 \"사과\"는 숫자 모양이 아니므로 int로 바꿀 수 없어 ValueError가 납니다.";
            case "입력받아 계산하는 프로그램":
                return "input으로 값을 받고, int로 숫자로 바꾸고, 계산하고, print로 보여 줍니다. 이 네 단계는 아주 많은 작은 프로그램의 기본 골격입니다. f-string은 계산 결과를 자연스러운 문장 속에 넣어 보여 주는 편한 방법입니다.";
            case "실습 A — 내 이름과 나이 입력받기":
                return "사용자가 입력한 데이터를 변수에 보관하고 다시 사용하는 연습입니다. 특히 나이는 숫자 계산을 해야 하므로 int 변환이 왜 필요한지 직접 확인하는 것이 핵심입니다.";
            case "실습 B — 두 숫자 더하기":
                return "숫자 두 개를 입력받아 계산하는 가장 단순한 계산기입니다. int를 빼면 문자열끼리 붙는 결과가 나올 수 있으므로 ‘입력값의 타입’을 의식하는 연습이 됩니다.";
            case "확인 문제 — 입력과 형 변환":
                return "input은 문자열을 준다는 사실과, 숫자로 계산하려면 변환이 필요하다는 사실을 확인합니다. 이 두 가지만 제대로 이해해도 입력 처리에서 생기는 초반 실수가 크게 줄어듭니다.";
            case "챕터 5 정리 — 입력은 타입까지 확인한다":
                return "사용자가 무엇을 입력했는지만 보지 말고 그 값이 어떤 타입으로 들어왔는지도 봐야 합니다. ‘입력 → 타입 확인/변환 → 처리 → 출력’ 순서로 생각하면 좋습니다.";

            case "오류는 ‘틀렸다’가 아니라 단서다":
                return "오류 메시지는 컴퓨터가 화내는 문장이 아닙니다. ‘여기에서 이런 이유로 더 못 가겠어’라고 알려 주는 힌트입니다. 오류를 없애려고 무작정 코드를 지우기보다 메시지를 읽고 범위를 좁히는 것이 디버깅입니다.";
            case "초보가 먼저 알아둘 오류 네 가지":
                return "오류 이름은 병원에서 증상 분류표를 보는 것과 비슷합니다. SyntaxError면 문법, NameError면 이름, TypeError면 타입, ValueError면 값 형식을 먼저 의심하면 됩니다. 오류 이름 하나만 알아도 검색이 훨씬 쉬워집니다.";
            case "traceback에서 무엇부터 볼까?":
                return "traceback 전체를 처음부터 완벽히 읽으려고 하지 마세요. 맨 마지막 오류 이름과 설명을 보고, 그 위에서 내 코드의 줄 번호를 찾으면 됩니다. 그리고 ‘내가 방금 무엇을 바꿨지?’를 확인하면 원인 후보가 크게 줄어듭니다.";
            case "네 가지 오류를 구분해 보기":
                return "오류도 직접 만나 봐야 익숙해집니다. 일부러 문법을 깨고, 없는 이름을 쓰고, 타입을 섞어 보면 오류 이름과 원인이 연결됩니다. 실제 문제가 생겼을 때 ‘전에 봤던 그 오류네’가 되는 것이 목표입니다.";
            case "실습 A — 일부러 오류 만들고 복구하기":
                return "일부러 고장 내고 다시 고치는 연습은 매우 좋은 공부입니다. 정상 코드와 고장 난 코드의 차이를 내가 직접 만들었기 때문에 오류 원인을 눈으로 비교하기 쉽습니다.";
            case "실습 B — 디버깅 4단계 습관":
                return "디버깅은 ‘증상을 본다 → 원인을 추측한다 → 한 부분만 고친다 → 다시 실행해 확인한다’의 반복입니다. 여러 곳을 한꺼번에 고치면 무엇 때문에 해결됐는지 알기 어려워집니다.";
            case "확인 문제 — 오류 이름으로 범위 좁히기":
                return "오류 이름을 외우는 것이 목적이 아니라, 오류를 봤을 때 어디부터 찾아야 할지 방향을 잡는 연습입니다. 다음 장에서 각각을 유치원생도 이해할 수 있는 말로 다시 풉니다.";
            case "챕터 6 정리 — 오류를 읽는 순서":
                return "오류가 나면 당황해서 처음부터 다시 만들지 마세요. 마지막 오류 이름 → 줄 번호 → 방금 바꾼 부분 순으로 보면 됩니다. 이 습관이 나중에 훨씬 큰 프로그램에서도 그대로 사용됩니다.";

            case "코드는 실제 계산 도구가 될 수 있다":
                return "변수에 값을 넣고 연산자를 사용하면 컴퓨터에게 반복 계산을 맡길 수 있습니다. 사람은 계산 규칙을 한 번 코드로 만들고, 컴퓨터는 다른 숫자가 들어와도 같은 규칙으로 계속 계산합니다.";
            case "기본 산술 연산자":
                return "+, -, *, /는 숫자 계산을 시키는 기호입니다. 코딩에서 *가 곱하기, /가 나누기라는 점만 익숙해지면 됩니다. //, %, **는 나중에 자주 보게 되지만 지금은 ‘이런 것도 있구나’ 정도면 충분합니다.";
            case "계산 순서와 괄호":
                return "컴퓨터도 계산 순서를 정해 두고 움직입니다. 2 + 3 * 4에서는 곱셈을 먼저 합니다. 내가 원하는 순서를 확실히 보여 주고 싶으면 괄호를 씁니다. 괄호는 컴퓨터뿐 아니라 코드를 읽는 사람에게도 ‘여기를 먼저 봐’라고 알려 줍니다.";
            case "가격 × 개수 = 총액":
                return "가격과 개수를 변수로 따로 두면 상품 가격이나 개수가 바뀌어도 같은 계산식을 그대로 쓸 수 있습니다. 이게 코드를 재사용한다는 아주 작은 예입니다.";
            case "실습 A — 장보기 계산기":
                return "현실에서 바로 이해할 수 있는 돈 계산으로 변수와 연산자를 연결합니다. 각 상품 총액을 따로 계산한 뒤 마지막에 더하면 복잡한 계산도 작은 단계로 나눌 수 있습니다.";
            case "실습 B — 입력받는 계산기로 확장하기":
                return "고정된 숫자를 코드에 박아 두는 대신 사용자가 입력하게 만들면 프로그램이 훨씬 쓸모 있어집니다. 같은 코드를 가격과 개수만 바꿔 여러 번 사용할 수 있기 때문입니다.";
            case "확인 문제 — 연산과 계산 순서":
                return "컴퓨터가 어떤 순서로 계산할지 미리 예상할 수 있는지 확인합니다. 실제 코드를 실행하기 전에 머릿속으로 결과를 계산하는 습관이 디버깅에도 도움이 됩니다.";
            case "챕터 7 정리 — 계산을 코드로 옮기는 법":
                return "‘숫자를 변수에 넣는다 → 연산자로 계산한다 → 결과를 새 변수에 넣는다 → 출력한다’가 작은 계산 프로그램의 기본 흐름입니다. 괄호를 쓰면 계산 의도를 더 정확히 전달할 수 있습니다.";

            case "프로젝트는 코드를 쓰기 전에 요구사항부터 정한다":
                return "코드를 먼저 쓰기 시작하면 중간에 ‘내가 뭘 만들려고 했지?’가 되기 쉽습니다. 먼저 한 문장으로 목표를 적으면 필요한 입력과 결과가 보입니다. 이것이 아주 작은 요구사항입니다.";
            case "입력 → 처리 → 출력으로 문제 나누기":
                return "큰 문제를 한 번에 풀려고 하지 말고 세 칸으로 나눕니다. 무엇을 받을지, 안에서 무엇을 계산할지, 마지막에 무엇을 보여 줄지 정하면 코드 줄이 자연스럽게 따라옵니다. 알고리즘은 이런 해결 순서를 단계로 정리한 것이라고 생각하면 됩니다.";
            case "테스트 케이스와 경계값":
                return "프로그램이 한 번 잘 됐다고 항상 맞는 것은 아닙니다. 미리 입력과 정답을 정해 놓고 여러 번 확인해야 합니다. 0원처럼 특이하지만 가능한 값도 넣어 보는 이유는 평범한 입력에서 숨겨진 문제를 찾기 위해서입니다.";
            case "오늘 지출 계산기 기본 버전":
                return "지금까지 배운 기능만 이어 붙여도 실제로 쓸 수 있는 작은 도구가 됩니다. 이름과 금액을 입력받고, 금액을 더하고, 결과를 문장으로 보여 줍니다. ‘새 문법을 많이 쓰는 것’보다 배운 것을 정확히 조합하는 것이 더 중요합니다.";
            case "실습 A — 내 지출로 완성하기":
                return "예제를 그대로 복사하는 단계에서 한 걸음 나아가 내 요구에 맞게 바꾸는 연습입니다. snack 같은 항목을 추가하면 입력·변수·계산·출력 네 군데가 어떻게 함께 연결되는지 볼 수 있습니다.";
            case "실습 B — 테스트 케이스 세 개 만들기":
                return "테스트 케이스는 ‘이 값을 넣으면 이 답이 나와야 해’라고 미리 약속하는 것입니다. 0원과 큰 금액, 잘못된 글자 입력을 넣어 보면 정상 상황뿐 아니라 이상한 상황에서도 프로그램이 어떻게 움직이는지 알 수 있습니다.";
            case "확인 문제 — 프로그램을 설계하는 순서":
                return "코드를 많이 아는 것보다 문제를 작은 단계로 나누는 습관이 중요합니다. 입력·처리·출력, 테스트 케이스, edge case 세 가지 말을 자기 말로 설명할 수 있는지 확인합니다.";
            case "TRACK 01 완료 — 이제 무엇을 할 수 있나?":
                return "트랙 1의 목표는 개발자가 되는 것이 아니라 ‘코딩 글을 읽다가 모르는 단어를 검색하고, 간단한 코드를 직접 바꾸고, 오류가 나면 메시지를 보고 질문할 수 있는 상태’가 되는 것입니다. 조건문·반복문·함수는 다음 트랙에서 차근차근 이어집니다.";
            default:
                return "";
        }
    }

    private static Page practiceAnswerPage(Page source) {
        String body;
        String code = null;

        switch (source.title) {
            case "실습 A — 코드 한 글자 바꾸면 결과도 바뀐다":
                body = "예시 답안\n1. print(\"승원\")처럼 따옴표 안 글자만 바꾸면 화면에 승원이 나옵니다.\n2. 10 + 7은 먼저 계산되어 17이 출력됩니다.\n3. print(100)을 추가하면 다음 줄에 100이 나옵니다.\n\n왜 이렇게 될까요? print는 괄호 안의 값을 화면에 보여 주고, 숫자 계산식은 먼저 계산되기 때문입니다. 중요한 것은 자기 이름이 꼭 승원일 필요가 없다는 점입니다. 내가 넣은 값에 맞게 결과가 바뀌면 정답입니다.";
                code = "print(\"승원\")\nprint(10 + 7)\nprint(100)";
                break;
            case "실습 B — 예상하고 실행하기":
                body = "예시 답안\n첫 줄 10 - 4는 6, 둘째 줄 3 * 5는 15입니다. 셋째 줄은 따옴표가 있으므로 계산하지 않고 글자 ‘3 + 5’를 그대로 보여 줍니다.\n\n마지막 줄의 따옴표를 지워 print(3 + 5)로 만들면 그때는 숫자 계산이 되어 8이 나옵니다. ‘따옴표 안은 글자, 따옴표 밖 숫자식은 계산’이라고 생각하면 쉽습니다.";
                code = "print(10 - 4)      # 6\nprint(3 * 5)       # 15\nprint(\"3 + 5\")   # 3 + 5\nprint(3 + 5)       # 8";
                break;
            case "실습 A — 순서를 바꿔 결과를 통제하기":
                body = "예시 답안\nC를 먼저 보고 싶다면 print(\"C\")를 맨 위에 둡니다. B → A → C 순서를 원하면 코드도 B, A, C 순서로 적습니다. #을 줄 앞에 붙이면 그 줄은 주석이 되어 실행되지 않습니다.\n\n여기서 핵심은 ‘Python은 기본적으로 위에서 아래’라는 규칙입니다.";
                code = "print(\"B\")\nprint(\"A\")\n# print(\"이 줄은 실행 안 됨\")\nprint(\"C\")";
                break;
            case "실습 B — 표현식은 먼저 계산된다":
                body = "예시 답안\n2 + 3은 5, 10 - 4는 6, 3 * 2 + 1은 7입니다. 세 번째는 곱셈 3 * 2가 먼저 6이 되고, 거기에 1을 더해 7이 됩니다.\n\n자기 계산식은 예를 들어 print(8 + 2), print(4 * 5)처럼 만들 수 있습니다.";
                code = "print(2 + 3)       # 5\nprint(10 - 4)      # 6\nprint(3 * 2 + 1)   # 7\nprint(8 + 2)       # 10\nprint(4 * 5)       # 20";
                break;
            case "실습 A — 내 정보로 변수 바꾸기":
                body = "예시 답안\n값은 자기 정보에 맞게 자유롭게 바꾸면 됩니다. age = age + 1은 현재 age 값에 1을 더해서 다시 age에 넣는 뜻입니다. city를 home_city로 이름을 바꿨다면 print(city)도 print(home_city)로 함께 고쳐야 합니다.";
                code = "name = \"승원\"\nage = 37\nhome_city = \"서울\"\n\nprint(name)\nprint(age)\nprint(home_city)\n\nage = age + 1\nprint(age)";
                break;
            case "실습 B — 좋은 변수 이름 만들기":
                body = "예시 답안\n오늘 걸음 수라면 today_steps, 사용자 이름이라면 user_name처럼 뜻이 보이는 이름이 좋습니다. 1name처럼 숫자로 시작하는 이름은 Python 변수 이름 규칙에 맞지 않아 SyntaxError가 납니다.\n\n정답은 이름 하나로 고정되지 않습니다. 다른 사람이 봤을 때 뜻을 알아보기 쉬우면 좋은 답입니다.";
                code = "today_steps = 8500\nuser_name = \"민수\"\ntotal_price = 1200 * 3";
                break;
            case "실습 A — 같은 모양, 다른 타입":
                body = "예시 답안\na + a는 20입니다. a는 숫자 10이기 때문입니다. b + b는 \"1010\"입니다. b는 글자 \"10\"이어서 글자를 이어 붙입니다. 3은 int, 3.0은 float, True는 bool, \"True\"는 str입니다.";
                code = "a = 10\nb = \"10\"\nprint(a + a)       # 20\nprint(b + b)       # 1010\nprint(type(3))     # int\nprint(type(3.0))   # float";
                break;
            case "실습 B — 타입에 맞는 연산 고르기":
                body = "예시 답안\n총 가격은 price * count로 계산합니다. 문자열 product는 product + product처럼 이어 붙일 수 있습니다. 하지만 price + product는 숫자와 문자열을 바로 더하려는 것이어서 TypeError가 납니다.\n\n오류가 나오면 ‘둘의 타입이 같은가?’를 먼저 확인해 보세요.";
                code = "price = 2500\ncount = 4\nproduct = \"사과\"\n\nprint(price * count)\nprint(product + product)";
                break;
            case "실습 A — 내 이름과 나이 입력받기":
                body = "예시 답안\n이름은 문자열 그대로 사용하고, 나이는 다음 해 나이를 계산해야 하므로 int로 바꿉니다. 나이에 ‘열살’ 같은 글자를 입력하면 숫자로 바꿀 수 없어서 ValueError가 납니다.";
                code = "name = input(\"이름: \")\nage = int(input(\"나이: \"))\n\nprint(f\"{name}님은 {age}살입니다.\")\nprint(f\"내년에는 {age + 1}살입니다.\")";
                break;
            case "실습 B — 두 숫자 더하기":
                body = "예시 답안\n10과 20을 입력하면 a와 b는 int로 변환되어 30이 나옵니다. 곱셈을 원하면 +를 *로 바꾸면 됩니다. 소수를 받으려면 float(input(...))을 사용합니다. a, b보다 first_number, second_number처럼 뜻이 보이는 이름이 더 친절합니다.";
                code = "first_number = float(input(\"첫 번째 숫자: \") )\nsecond_number = float(input(\"두 번째 숫자: \") )\nprint(first_number + second_number)";
                break;
            case "실습 A — 일부러 오류 만들고 복구하기":
                body = "예시 답안\n따옴표를 지우면 Python이 문자열 끝을 찾지 못해 SyntaxError가 납니다. name 대신 nam을 쓰면 그런 이름을 만든 적이 없어 NameError가 납니다. 10 + \"5\"는 숫자와 문자열을 바로 더할 수 없어 TypeError가 납니다.\n\n하나씩 원래 코드로 돌리면 다시 정상 실행됩니다.";
                code = "name = \"민수\"\nprint(name)\nprint(10 + 5)";
                break;
            case "실습 B — 디버깅 4단계 습관":
                body = "예시 답안\nprice = \"1200\"은 문자열입니다. 문자열에 * 3을 하면 ‘1200’을 세 번 이어 붙이는 동작이 되어 숫자 3600을 만들지 않습니다. type(price)를 출력하면 str이라고 확인할 수 있습니다. int(price)로 바꾼 뒤 계산하면 3600이 됩니다.";
                code = "price = \"1200\"\ncount = 3\nprint(type(price))\n\nprice = int(price)\nprint(price * count)  # 3600";
                break;
            case "실습 A — 장보기 계산기":
                body = "예시 답안\n사과는 1500 × 4 = 6000원, 바나나는 2000 × 2 = 4000원입니다. 전체는 10000원입니다. 10% 할인은 원래 금액의 90%를 내는 것이므로 10000 * 0.9 = 9000원입니다.";
                code = "apple_total = 1500 * 4\nbanana_total = 2000 * 2\ntotal = apple_total + banana_total\ndiscounted = total * 0.9\n\nprint(total)       # 10000\nprint(discounted)  # 9000.0";
                break;
            case "실습 B — 입력받는 계산기로 확장하기":
                body = "예시 답안\n가격 2500, 개수 4면 상품값은 10000원입니다. 배송비가 3000원이라면 최종은 13000원입니다. 소수 가격을 허용하려면 int 대신 float를 사용할 수 있습니다.";
                code = "price = int(input(\"가격: \") )\ncount = int(input(\"개수: \") )\nshipping = 3000\n\ntotal = (price * count) + shipping\nprint(f\"총액: {total}원\")";
                break;
            case "실습 A — 내 지출로 완성하기":
                body = "예시 답안\nsnack을 새로 입력받았다면 total 계산에도 snack을 더해야 합니다. 입력만 만들고 계산식에 넣지 않으면 총액에는 반영되지 않습니다. 마지막에 손계산 값과 프로그램 결과가 같은지 확인하면 됩니다.";
                code = "name = input(\"이름: \")\ncoffee = int(input(\"커피: \") )\nlunch = int(input(\"점심: \") )\ntransport = int(input(\"교통: \") )\nsnack = int(input(\"간식: \") )\n\ntotal = coffee + lunch + transport + snack\nprint(f\"{name}님의 오늘 지출은 {total}원입니다.\")";
                break;
            case "실습 B — 테스트 케이스 세 개 만들기":
                body = "예시 답안\n테스트 1은 0 + 9000 + 1500 = 10500, 테스트 2는 모두 0이므로 0입니다. 큰 금액도 Python 정수 계산에서는 정상적으로 더할 수 있습니다. 숫자 입력 칸에 ‘천원’을 넣으면 int(\"천원\")으로 바꿀 수 없어서 ValueError가 납니다.\n\n중요한 것은 입력하기 전에 기대값을 미리 적어 두고 실제 결과와 비교하는 것입니다.";
                code = null;
                break;
            default:
                body = "이 실습은 정답 하나를 외우는 문제가 아닙니다. 요구된 조건을 만족하는 여러 답이 있을 수 있습니다. 위 실습으로 돌아가 한 단계씩 실행하고, 결과가 설명과 같은지 확인하세요.";
                break;
        }

        body = body + "\n\n" + practiceApplicationFor(source);

        return new Page(
                source.lessonNumber,
                source.lessonTitle,
                "실습 정답·해설",
                source.title + " — 예시 정답과 아주 쉬운 풀이",
                body,
                "",
                code,
                null,
                null,
                null,
                "정답을 복사하기보다 왜 이렇게 되는지 한 줄씩 확인하기"
        );
    }

    private static String practiceApplicationFor(Page source) {
        switch (source.title) {
            case "실습 A — 코드 한 글자 바꾸면 결과도 바뀐다":
                return "한 단계 더 해보기\n"
                        + "이번에는 세 줄을 스스로 만들어 보세요. 첫 줄은 자기 이름, 둘째 줄은 8 + 9, 셋째 줄은 좋아하는 숫자를 출력합니다. 먼저 종이에 예상 결과를 적고 실행합니다.\n\n"
                        + "응용 예시\nprint(\"민수\")\nprint(8 + 9)\nprint(7)\n\n"
                        + "응용 해설\n첫 줄과 셋째 줄은 값을 그대로 보여 주고, 둘째 줄은 8 + 9라는 expression을 먼저 계산해 17을 만든 뒤 출력합니다. 이렇게 ‘글자 출력’과 ‘계산 결과 출력’을 한 프로그램 안에서 섞어 사용할 수 있습니다. 핵심은 코드를 바꾼 부분과 결과가 어떻게 연결되는지 설명할 수 있는 것입니다.";
            case "실습 B — 예상하고 실행하기":
                return "한 단계 더 해보기\n"
                        + "print(2 * 4 + 3)과 print(\"2 * 4 + 3\")의 결과를 실행 전에 예상해 보세요.\n\n"
                        + "응용 정답\n첫 번째는 11, 두 번째는 글자 2 * 4 + 3입니다.\n\n"
                        + "응용 해설\n따옴표가 없으면 Python은 숫자와 연산자를 expression으로 보고 계산합니다. 곱셈이 먼저라 2 * 4가 8이 되고 3을 더해 11이 됩니다. 따옴표 안에 들어가면 전체가 문자열이므로 계산하지 않습니다. 이 차이를 이해하면 나중에 input으로 받은 숫자가 왜 바로 계산되지 않는지도 쉽게 이해할 수 있습니다.";
            case "실습 A — 순서를 바꿔 결과를 통제하기":
                return "한 단계 더 해보기\n"
                        + "‘준비’, ‘실행’, ‘완료’ 세 문장이 정확한 순서로 나오게 만들고, 가운데 ‘실행’ 줄만 주석 처리했을 때 결과를 예상하세요.\n\n"
                        + "응용 예시\nprint(\"준비\")\n# print(\"실행\")\nprint(\"완료\")\n\n"
                        + "응용 해설\nPython은 위에서 아래로 진행하지만 주석 줄은 실행하지 않습니다. 그래서 준비 다음에 바로 완료가 나옵니다. 실제 프로그램에서도 특정 코드를 잠시 빼고 동작을 비교할 때 주석을 사용할 수 있지만, 장기간 필요 없는 코드는 삭제하거나 버전 관리로 남기는 편이 좋습니다.";
            case "실습 B — 표현식은 먼저 계산된다":
                return "한 단계 더 해보기\n"
                        + "print(5 + 2 * 3)과 print((5 + 2) * 3)의 결과가 왜 다른지 설명해 보세요.\n\n"
                        + "응용 정답\n첫 번째는 11, 두 번째는 21입니다.\n\n"
                        + "응용 해설\n첫 번째는 곱셈 2 * 3을 먼저 계산해 6을 만들고 5를 더합니다. 두 번째는 괄호가 있으므로 5 + 2를 먼저 계산해 7을 만든 뒤 3을 곱합니다. expression을 읽을 때는 값만 보지 말고 연산 순서까지 확인해야 합니다.";
            case "실습 A — 내 정보로 변수 바꾸기":
                return "한 단계 더 해보기\n"
                        + "name, age, home_city를 이용해 ‘민수는 서울에 살고 37살입니다’ 같은 문장을 출력해 보세요. 아직 f-string을 배우기 전이라 print에 여러 값을 쉼표로 넣어도 됩니다.\n\n"
                        + "응용 예시\nprint(name, \"는\", home_city, \"에 살고\", age, \"살입니다\")\n\n"
                        + "응용 해설\n변수를 사용하면 실제 값이 달라져도 출력 코드는 그대로 재사용할 수 있습니다. 이름이나 도시를 바꾸면 같은 문장 구조에서 새 값이 나옵니다. 이것이 ‘코드와 데이터가 분리된다’는 아주 작은 예입니다.";
            case "실습 B — 좋은 변수 이름 만들기":
                return "한 단계 더 해보기\n"
                        + "병원 방문 횟수, 오늘 매출, 사용자 전화번호를 저장할 변수 이름을 각각 만들어 보세요.\n\n"
                        + "응용 예시\nvisit_count, today_sales, user_phone_number\n\n"
                        + "응용 해설\n좋은 이름에는 값의 역할이 드러납니다. phone처럼 너무 넓은 이름보다 user_phone_number가 상황을 더 정확히 말해 줍니다. 다만 이름이 지나치게 길어지면 읽기 어려우므로 의미가 충분히 전달되는 범위에서 간결하게 정합니다.";
            case "실습 A — 같은 모양, 다른 타입":
                return "한 단계 더 해보기\n"
                        + "x = 5, y = \"5\"일 때 print(x * 2)와 print(y * 2)의 결과를 예상하세요.\n\n"
                        + "응용 정답\n첫 번째는 10, 두 번째는 55입니다.\n\n"
                        + "응용 해설\n숫자 5에 * 2를 하면 산술 곱셈입니다. 문자열 \"5\"에 * 2를 하면 문자열 반복이 되어 \"55\"가 됩니다. 같은 연산자라도 타입에 따라 의미가 달라질 수 있으므로 예상하지 못한 결과가 나오면 type()을 확인해야 합니다.";
            case "실습 B — 타입에 맞는 연산 고르기":
                return "한 단계 더 해보기\n"
                        + "price = \"2500\"이라는 문자열이 들어왔을 때 count = 4와 곱해 숫자 10000을 만들려면 어떻게 고쳐야 할까요?\n\n"
                        + "응용 정답\ntotal = int(price) * count\n\n"
                        + "응용 해설\nprice가 문자열이면 먼저 int(price)로 숫자 타입으로 바꿔야 합니다. 외부 파일이나 input에서 들어온 숫자가 문자열인 경우가 흔하므로 ‘형 변환 후 계산’ 패턴은 실제 코드에서도 매우 자주 사용됩니다.";
            case "실습 A — 내 이름과 나이 입력받기":
                return "한 단계 더 해보기\n"
                        + "출생연도를 입력받아 대략적인 나이를 계산하는 프로그램을 생각해 보세요. 여기서는 현재 연도를 2026이라고 가정합니다.\n\n"
                        + "응용 예시\nbirth_year = int(input(\"출생연도: \") )\nage = 2026 - birth_year\nprint(f\"대략 {age}살입니다.\")\n\n"
                        + "응용 해설\ninput 결과는 문자열이므로 뺄셈을 하려면 int로 바꿔야 합니다. 실제 나이는 생일이 지났는지에 따라 달라질 수 있으므로 이 코드는 ‘대략적인 계산’입니다. 프로그램을 만들 때 계산 규칙의 한계도 함께 생각해야 합니다.";
            case "실습 B — 두 숫자 더하기":
                return "한 단계 더 해보기\n"
                        + "세 숫자를 입력받아 평균을 계산해 보세요.\n\n"
                        + "응용 예시\na = float(input(\"첫 수: \") )\nb = float(input(\"둘째 수: \") )\nc = float(input(\"셋째 수: \") )\naverage = (a + b + c) / 3\nprint(average)\n\n"
                        + "응용 해설\n평균에는 소수 결과가 나올 수 있으므로 float를 사용했습니다. 괄호로 세 수를 먼저 더한다는 의도를 분명히 보여 줬고, 그 합을 3으로 나눠 평균을 만듭니다.";
            case "실습 A — 일부러 오류 만들고 복구하기":
                return "한 단계 더 해보기\n"
                        + "print(user_age)에서 NameError가 났다고 가정해 보세요. 어디부터 확인해야 할까요?\n\n"
                        + "응용 해설\n첫째 user_age라는 변수를 앞에서 만든 적이 있는지 봅니다. 둘째 철자가 user_age와 userage처럼 다르지 않은지 확인합니다. 셋째 변수를 만드는 줄보다 먼저 사용한 것은 아닌지 실행 순서를 봅니다. 오류 이름을 보고 조사 범위를 좁히는 것이 디버깅의 핵심입니다.";
            case "실습 B — 디버깅 4단계 습관":
                return "한 단계 더 해보기\n"
                        + "total = price + count에서 TypeError가 났다면 한 번에 모든 코드를 고치지 말고 어떤 확인을 할지 순서를 적어 보세요.\n\n"
                        + "응용 해설\n먼저 print(type(price)), print(type(count))로 실제 타입을 확인합니다. 문자열과 숫자가 섞였다면 어떤 값이 잘못 들어왔는지 찾습니다. 필요한 값만 int나 float로 변환한 뒤 다시 실행합니다. 마지막으로 예상 결과와 같은지 확인합니다. 이 순서가 ‘관찰 → 가설 → 최소 수정 → 검증’입니다.";
            case "실습 A — 장보기 계산기":
                return "한 단계 더 해보기\n"
                        + "할인 후 금액에 배송비 3000원을 더하는 계산을 만들어 보세요. 할인은 상품 금액에만 적용된다고 가정합니다.\n\n"
                        + "응용 예시\nproduct_total = 10000\ndiscounted = product_total * 0.9\nshipping = 3000\nfinal_total = discounted + shipping\n\n"
                        + "응용 해설\n무엇에 할인을 적용하는지가 중요합니다. 배송비까지 할인하면 계산 규칙이 달라집니다. 실제 프로그램에서는 이런 규칙을 요구사항으로 먼저 명확히 정해야 계산식이 정확해집니다.";
            case "실습 B — 입력받는 계산기로 확장하기":
                return "한 단계 더 해보기\n"
                        + "사용자가 할인율도 입력하도록 바꿔 보세요. 10을 입력하면 10% 할인이라고 해석합니다.\n\n"
                        + "응용 예시\ndiscount_percent = float(input(\"할인율(%): \") )\ndiscount_rate = discount_percent / 100\nfinal = (price * count) * (1 - discount_rate)\n\n"
                        + "응용 해설\n사람은 10%라고 입력하지만 계산에서는 0.10이 필요합니다. 그래서 100으로 나눠 비율을 만든 뒤 1 - discount_rate를 곱합니다. 사용자 표현과 내부 계산 표현이 다를 수 있다는 점을 보여 주는 예입니다.";
            case "실습 A — 내 지출로 완성하기":
                return "한 단계 더 해보기\n"
                        + "오늘 예산도 입력받고 ‘예산에서 얼마가 남았는지’를 출력해 보세요.\n\n"
                        + "응용 예시\nbudget = int(input(\"오늘 예산: \") )\nremaining = budget - total\nprint(f\"남은 예산: {remaining}원\")\n\n"
                        + "응용 해설\n기존 total을 다시 사용해 새로운 계산을 만들었습니다. 프로그램은 이전에 계산한 값을 다음 처리 단계의 입력처럼 사용할 수 있습니다. remaining이 음수가 나오면 예산을 초과했다는 뜻인데, 이 상황을 다루는 조건문은 다음 트랙에서 배웁니다.";
            case "실습 B — 테스트 케이스 세 개 만들기":
                return "한 단계 더 해보기\n"
                        + "테스트 케이스를 표처럼 생각해 보세요. 입력 coffee=1000, lunch=5000, transport=0이면 expected result는 6000입니다. 실제 결과가 6000이 아니면 코드에 문제가 있다는 신호입니다.\n\n"
                        + "응용 과제\n음수 -1000을 입력했을 때 프로그램은 어떻게 동작하는지 확인하세요. 기술적으로는 계산되지만 ‘지출 금액에 음수가 허용되는가?’는 요구사항 문제입니다. 나중에는 조건문으로 이런 잘못된 값을 막을 수 있습니다.";
            default:
                return "한 단계 더 해보기\n원래 실습의 숫자나 문자열 하나를 바꾼 새 문제를 직접 만들고, 실행 전에 결과를 예상한 뒤 실제 결과와 비교해 보세요. 정답을 그대로 복사하는 것보다 문제를 조금 변형해 보는 것이 이해 여부를 확인하는 가장 좋은 방법입니다.";
        }
    }

    private static String questionApplicationFor(Page source) {
        switch (source.lessonNumber) {
            case 1:
                return "응용 문제\nprint(\"2 + 3\")와 print(2 + 3)는 각각 무엇을 출력할까요?\n\n"
                        + "응용 정답·해설\n첫 번째는 글자 2 + 3, 두 번째는 숫자 5입니다. 따옴표 안은 문자열이라 계산하지 않고, 따옴표 밖의 숫자 expression은 먼저 계산합니다. 이 차이는 이후 input 값이 왜 문자열인지 이해할 때 다시 등장합니다.";
            case 2:
                return "응용 문제\nprint(1 + 2)\n# print(99)\nprint(3 + 4)\n이 코드는 어떤 순서로 무엇을 출력할까요?\n\n"
                        + "응용 정답·해설\n첫 줄 expression 1 + 2가 3이 되어 출력됩니다. 둘째 줄은 #으로 시작해 주석이므로 건너뜁니다. 마지막 줄 3 + 4가 7이 되어 출력됩니다. 따라서 결과는 3 다음 7입니다.";
            case 3:
                return "응용 문제\nscore = 10\nscore = score + 5\nprint(score)\n결과는 무엇일까요?\n\n"
                        + "응용 정답·해설\n15입니다. 두 번째 줄의 오른쪽 score는 기존 값 10을 사용해 10 + 5를 계산하고, 그 결과 15를 다시 score에 대입합니다. 같은 변수 이름이더라도 재대입 후에는 최신 값이 사용됩니다.";
            case 4:
                return "응용 문제\nprint(type(\"3.14\"))의 결과 타입은 float일까요 str일까요?\n\n"
                        + "응용 정답·해설\nstr입니다. 안쪽에 소수점 모양이 있어도 따옴표로 감싸져 있기 때문입니다. 실제 숫자 3.14로 계산하려면 float(\"3.14\")처럼 변환해야 합니다.";
            case 5:
                return "응용 문제\nx = input(\"숫자: \")에서 사용자가 7을 입력했습니다. print(x + x)는 무엇이 나올까요?\n\n"
                        + "응용 정답·해설\n77이 나옵니다. input의 반환값은 문자열이므로 x는 \"7\"입니다. 문자열 + 문자열은 연결입니다. 14를 원하면 x = int(input(...))처럼 숫자로 변환해야 합니다.";
            case 6:
                return "응용 문제\nprice = \"1000\"\nprint(price + 500)에서 어떤 오류를 예상할 수 있을까요?\n\n"
                        + "응용 정답·해설\nTypeError를 예상할 수 있습니다. price는 str이고 500은 int라 + 연산으로 바로 합칠 수 없습니다. type(price)를 확인하고 int(price)로 변환하면 숫자 1500을 만들 수 있습니다.";
            case 7:
                return "응용 문제\n10 - 2 * 3과 (10 - 2) * 3의 결과를 각각 계산하세요.\n\n"
                        + "응용 정답·해설\n첫 번째는 4입니다. 곱셈 2 * 3 = 6을 먼저 하고 10 - 6을 합니다. 두 번째는 괄호 10 - 2 = 8을 먼저 하고 8 * 3 = 24가 됩니다. 계산 순서를 분명히 하고 싶으면 괄호를 적극적으로 사용하세요.";
            case 8:
                return "응용 문제\n‘두 숫자를 입력받아 더 큰 숫자를 보여 주는 프로그램’을 만들기 전에 입력·처리·출력을 말로 나눠 보세요.\n\n"
                        + "응용 정답·해설\n입력은 두 숫자, 처리는 두 값을 비교해 어느 쪽이 큰지 판단하는 것, 출력은 더 큰 숫자입니다. 비교를 실제 코드로 만드는 if는 다음 트랙에서 배우지만, 아직 문법을 몰라도 요구사항과 알고리즘을 먼저 설계할 수 있습니다.";
            default:
                return "";
        }
    }

    private static String questionMistakeGuideFor(Page source) {
        switch (source.lessonNumber) {
            case 1:
                return "자주 틀리는 생각\n"
                        + "‘print 안에 있으면 모두 글자다’라고 생각하기 쉽지만 틀립니다. print(2 + 3)의 2 + 3은 계산식이고, print(\"2 + 3\")처럼 따옴표가 있어야 문자열입니다.\n\n"
                        + "스스로 확인하기\nprint(10)과 print(\"10\")은 화면에 비슷하게 보일 수 있습니다. 하지만 다음 챕터에서 타입을 배우면 두 값이 완전히 다르다는 것을 확인하게 됩니다. 지금은 ‘보이는 결과가 같아도 내부 값의 종류는 다를 수 있다’는 점만 기억하세요.";
            case 2:
                return "자주 틀리는 생각\n"
                        + "‘코드는 전부 위에서 아래로만 실행된다’고 영원히 생각하면 안 됩니다. 지금 챕터에서는 기본 순차 실행을 배우는 중입니다. 다음 트랙에서 if와 반복문을 배우면 실행 흐름이 갈라지거나 이전 줄로 돌아갈 수 있습니다.\n\n"
                        + "스스로 확인하기\n세 개의 print 줄 순서를 바꿔 결과가 바뀌는지 다시 확인하세요. 그리고 가운데 줄을 주석 처리해 ‘코드에 적혀 있어도 실행되지 않는 줄’이 있다는 것도 함께 확인하면 좋습니다.";
            case 3:
                return "자주 틀리는 생각\n"
                        + "x = 10을 수학식처럼 ‘x와 10은 같다’고만 읽으면 재대입에서 헷갈립니다. 프로그래밍에서는 ‘10을 x에 대입한다’고 읽는 것이 좋습니다.\n\n"
                        + "스스로 확인하기\nx = 10, x = x + 1, x = x + 1을 차례로 실행한 뒤 x가 12가 되는 과정을 한 줄씩 설명해 보세요. 오른쪽 x는 현재 값을 읽고, 계산한 새 값을 왼쪽 x에 다시 넣습니다.";
            case 4:
                return "자주 틀리는 생각\n"
                        + "값의 생김새만 보고 타입을 판단하면 실수합니다. \"100\"은 숫자처럼 보여도 문자열이고, \"True\"는 bool이 아니라 문자열입니다. 따옴표와 type() 결과를 함께 봐야 합니다.\n\n"
                        + "스스로 확인하기\n10, 10.0, \"10\", True 네 값을 각각 type()에 넣어 결과를 적어 보세요. 같은 화면 표시라도 타입이 다르면 가능한 연산이 달라진다는 점을 확인합니다.";
            case 5:
                return "자주 틀리는 생각\n"
                        + "input()에 숫자를 쳤으니 자동으로 숫자가 되겠지라고 생각하기 쉽습니다. 하지만 input()은 기본적으로 문자열을 반환합니다. 계산이 필요할 때 개발자가 int()나 float()를 선택합니다.\n\n"
                        + "스스로 확인하기\nx = input(\"숫자: \") 뒤에 print(type(x))를 추가하고 7을 입력해 보세요. 화면에는 7이 보이지만 타입은 str이라는 사실을 직접 확인할 수 있습니다.";
            case 6:
                return "자주 틀리는 생각\n"
                        + "오류 이름만 보고 바로 코드를 고치려는 것도 실수입니다. 같은 TypeError라도 원인은 여러 가지일 수 있습니다. 실제 값과 문제 줄을 확인한 뒤 가장 작은 수정부터 해야 합니다.\n\n"
                        + "스스로 확인하기\n오류 메시지를 복사해 검색할 때는 오류 이름만 넣기보다 ‘TypeError int str Python’처럼 관련 타입과 상황을 함께 넣으면 더 정확한 설명을 찾기 쉽습니다.";
            case 7:
                return "자주 틀리는 생각\n"
                        + "연산 순서를 머릿속으로만 복잡하게 외우려고 하면 실수하기 쉽습니다. 의도가 중요한 계산은 괄호로 직접 표시하는 편이 더 읽기 좋습니다.\n\n"
                        + "스스로 확인하기\n2 + 3 * 4, (2 + 3) * 4, 2 + (3 * 4)를 각각 실행하세요. 첫 번째와 세 번째가 같은 이유, 두 번째만 다른 이유를 설명하면 우선순위를 제대로 이해한 것입니다.";
            case 8:
                return "자주 틀리는 생각\n"
                        + "코딩을 시작해야 설계가 시작된다고 생각하기 쉽지만 반대입니다. 좋은 프로그램은 코드를 쓰기 전에 입력·처리·출력과 완료 기준을 먼저 정합니다.\n\n"
                        + "스스로 확인하기\n‘병원 방문 횟수를 입력받아 이번 달 총 방문 횟수를 보여 주는 프로그램’을 코드 없이 말로만 설계해 보세요. 어떤 입력이 필요하고, 무엇을 더하며, 어떤 결과를 보여 줄지 설명할 수 있다면 알고리즘의 시작을 이해한 것입니다.";
            default:
                return "";
        }
    }

    private static Page questionAnswerPage(Page source) {
        String extra;
        switch (source.lessonNumber) {
            case 1:
                extra = "‘소스 코드’는 우리가 직접 읽고 고치는 원본 글입니다. Python은 그 글을 쓰는 언어 이름입니다. print(2 + 3)에서는 2 + 3이 먼저 계산되어 5가 되고 print가 그 5를 보여 줍니다.";
                break;
            case 2:
                extra = "statement는 ‘일을 시키는 문장’, expression은 ‘계산하면 값이 되는 부분’이라고 생각하면 쉽습니다. # 뒤는 사람용 메모라 실행하지 않습니다. print(4 + 1)은 안쪽 계산 5를 만든 뒤 화면에 보여 줍니다.";
                break;
            case 3:
                extra = "x에 10을 넣었다가 다시 20을 넣으면 최신 값 20을 사용합니다. =는 오른쪽 값을 왼쪽 이름에 연결하는 대입입니다. total_price처럼 뜻이 보이는 이름은 나중에 코드를 다시 읽을 때 큰 도움이 됩니다.";
                break;
            case 4:
                extra = "따옴표로 감싼 \"25\"는 글자이므로 str입니다. 25.0처럼 소수점이 있으면 float입니다. True는 글자 \"True\"가 아니라 참을 나타내는 bool 값입니다.";
                break;
            case 5:
                extra = "input()은 사용자가 키보드로 친 내용을 문자열로 줍니다. 숫자 계산을 하려면 int나 float로 바꿉니다. hello는 숫자 모양이 아니므로 int로 바꾸려 하면 ValueError가 납니다.";
                break;
            case 6:
                extra = "없는 변수 이름을 쓰면 Python이 ‘그 이름이 뭔지 모르겠다’며 NameError를 냅니다. hello를 숫자로 바꾸려 하면 값 형식이 맞지 않아 ValueError가 납니다. 오류가 나면 전체를 지우지 말고 오류 이름과 줄 번호부터 봅니다.";
                break;
            case 7:
                extra = "곱셈은 덧셈보다 먼저이므로 2 + 3 * 4는 14입니다. 괄호가 있으면 괄호 안을 먼저 하므로 (2 + 3) * 4는 20입니다. 변수 계산도 똑같은 규칙을 따릅니다.";
                break;
            case 8:
                extra = "입력·처리·출력으로 나누면 큰 문제를 작은 세 덩어리로 볼 수 있습니다. 테스트 케이스는 ‘이 입력을 넣으면 이 결과가 나와야 한다’는 약속 한 세트입니다. edge case는 0이나 아주 큰 값처럼 평범하지 않은 가장자리 상황입니다.";
                break;
            default:
                extra = "";
        }

        String body = "정답\n" + (source.answer == null ? "" : source.answer)
                + "\n\n왜 이 답이 되는지 한 단계씩\n" + extra
                + "\n\n" + questionApplicationFor(source)
                + "\n\n" + questionMistakeGuideFor(source);

        return new Page(
                source.lessonNumber,
                source.lessonTitle,
                "문제 정답·해설",
                source.title + " — 정답 + 단계별 해설 + 응용",
                body,
                "",
                null,
                null,
                null,
                null,
                "문제를 다시 보고 자기 말로 한 번 설명하면 더 잘 기억됩니다."
        );
    }

    static int firstPageIndexOfLesson(List<Page> pages, int lessonNumber) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).lessonNumber == lessonNumber) return i;
        }
        return 0;
    }
}
