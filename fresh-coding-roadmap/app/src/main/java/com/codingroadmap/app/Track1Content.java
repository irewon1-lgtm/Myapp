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
                expanded.add(vocabularyPage(source.lessonNumber, source.lessonTitle));
                lastLesson = source.lessonNumber;
            }

            Page explained = new Page(
                    source.lessonNumber,
                    source.lessonTitle,
                    source.kind,
                    source.title,
                    source.body,
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
                + "\n\n유치원생도 이해하게 다시 설명하면\n" + extra;

        return new Page(
                source.lessonNumber,
                source.lessonTitle,
                "문제 정답·해설",
                source.title + " — 정답과 아주 쉬운 해설",
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
