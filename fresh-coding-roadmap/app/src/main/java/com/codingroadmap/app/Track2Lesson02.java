package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson02 {
    private Track2Lesson02() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 2;
        final String l = "반복문 for·while로 같은 작업을 반복하기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 반복·루프·한 번의 반복",
                "반복(repetition)\n"
                        + "같은 종류의 작업을 여러 번 수행하는 것입니다. 이름을 10번 출력하거나 1부터 100까지 더하는 일을 코드 100줄로 복사하지 않고 반복문으로 표현할 수 있습니다. 반복을 사용하면 코드가 짧아질 뿐 아니라 횟수나 규칙을 바꾸기도 쉬워집니다.\n\n"
                        + "루프(loop) / 반복문(loop statement)\n"
                        + "정해진 규칙에 따라 코드 블록을 여러 번 실행하는 구조입니다. Python의 대표적인 반복문은 for와 while입니다. 둘 다 같은 블록을 반복하지만, for는 ‘대상을 하나씩 꺼내며 반복’, while은 ‘조건이 True인 동안 반복’이라는 차이가 있습니다.\n\n"
                        + "반복 1회(iteration)\n"
                        + "루프의 본문이 한 번 실행되는 것을 iteration이라고 합니다. for가 다섯 번 돌면 iteration도 다섯 번입니다. 오류를 찾을 때 ‘세 번째 iteration에서 값이 어떻게 됐지?’처럼 각 반복을 따로 추적하면 이해가 쉽습니다.\n\n"
                        + "루프 본문(loop body)\n"
                        + "반복될 코드 블록입니다. if와 마찬가지로 Python에서는 들여쓰기로 소속을 표시합니다. 들여쓰기된 여러 줄은 한 iteration마다 위에서 아래로 함께 실행됩니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — for·range·반복 변수",
                "for 문(for loop)\n"
                        + "여러 값이나 정해진 범위를 하나씩 꺼내며 반복할 때 사용하는 문법입니다. for number in range(5):라고 쓰면 range가 만드는 값을 number에 하나씩 넣으면서 본문을 반복합니다. 반복 횟수가 비교적 분명할 때 자주 사용합니다.\n\n"
                        + "range\n"
                        + "연속된 정수 범위를 만들어 반복에 사용할 수 있게 해 주는 Python 기능입니다. range(5)는 0,1,2,3,4 다섯 값을 차례로 제공합니다. range(1, 6)은 1부터 5까지입니다. 끝 숫자는 포함되지 않는다는 점이 중요합니다.\n\n"
                        + "반복 변수(loop variable)\n"
                        + "for가 현재 꺼낸 값을 잠시 담는 변수입니다. for i in range(3)에서 i는 0, 1, 2로 바뀝니다. 이름은 i가 아니어도 되고, 실제 의미가 있다면 number, day, item처럼 짓는 편이 읽기 좋습니다.\n\n"
                        + "in\n"
                        + "for에서는 ‘이 대상 안의 값을 하나씩 사용한다’는 뜻으로 읽습니다. 나중에 리스트를 배우면 for item in items처럼 같은 문법으로 리스트 안의 항목도 하나씩 처리할 수 있습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — while·조건·카운터",
                "while 문(while loop)\n"
                        + "조건이 True인 동안 같은 블록을 계속 실행합니다. 횟수보다 ‘언제까지 계속할지’가 중요한 반복에 적합합니다. 예를 들어 비밀번호를 맞힐 때까지 다시 입력받거나 잔액이 목표 금액보다 작을 동안 저축을 반복하는 상황에 사용할 수 있습니다.\n\n"
                        + "반복 조건(loop condition)\n"
                        + "while이 매 iteration 시작 전에 확인하는 bool 식입니다. 조건이 True면 본문을 실행하고 다시 조건을 확인합니다. False가 되는 순간 루프를 빠져나옵니다. 따라서 언젠가 False가 되도록 값을 바꾸는 코드가 보통 필요합니다.\n\n"
                        + "카운터(counter)\n"
                        + "반복 횟수나 현재 순서를 세기 위해 사용하는 변수입니다. count = 0으로 시작해 count = count + 1 또는 count += 1로 증가시키는 패턴이 흔합니다. counter를 바꾸지 않으면 while이 끝나지 않을 수 있습니다.\n\n"
                        + "+= 복합 대입\n"
                        + "count = count + 1을 count += 1처럼 짧게 쓰는 문법입니다. 오른쪽 값을 더한 결과를 다시 같은 변수에 저장합니다. 반복문 안에서 합계나 횟수를 누적할 때 자주 보게 됩니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — break·continue·무한 루프·누적",
                "break\n"
                        + "반복 중간에서 루프 전체를 즉시 끝내는 문법입니다. while True처럼 계속 도는 루프를 만든 뒤 특정 입력을 받으면 break로 빠져나오는 패턴이 있습니다. 다만 처음에는 종료 조건이 명확한 while을 먼저 익히는 편이 안전합니다.\n\n"
                        + "continue\n"
                        + "현재 iteration의 남은 코드를 건너뛰고 다음 iteration으로 바로 넘어갑니다. 잘못된 값만 건너뛰고 나머지 데이터를 계속 처리할 때 유용합니다. continue 아래 코드는 그 iteration에서는 실행되지 않습니다.\n\n"
                        + "무한 루프(infinite loop)\n"
                        + "종료 조건이 영원히 False가 되지 않아 계속 반복되는 상태입니다. while count < 5인데 count를 증가시키지 않으면 count가 계속 0이라 끝나지 않습니다. 프로그램이 멈춘 것처럼 보일 수 있으므로 while에서는 값이 어떻게 바뀌는지 꼭 확인해야 합니다.\n\n"
                        + "누적(accumulation) / 누적 변수(accumulator)\n"
                        + "반복하면서 결과를 계속 더하거나 이어 붙이는 패턴입니다. total = 0으로 시작해 total += number를 반복하면 여러 숫자의 합계를 만들 수 있습니다. 반복문에서 매우 자주 쓰는 기본 패턴입니다."));

        out.add(Track2Content.page(lesson, l, "시작", "같은 코드를 복사하지 말고 규칙을 반복한다",
                "print를 열 번 실행하려고 같은 줄을 열 번 복사할 수도 있지만, 횟수가 100번이나 10,000번이 되면 현실적인 방법이 아닙니다. 더 큰 문제는 규칙을 바꿀 때 복사한 모든 줄을 수정해야 한다는 점입니다. 반복문은 ‘이 블록을 어떤 규칙에 따라 다시 실행하라’고 컴퓨터에게 맡기는 방법입니다.\n\n"
                        + "반복문을 읽을 때는 네 가지를 찾으세요. 무엇을 반복하는가, 현재 iteration의 값은 무엇인가, 반복할 때 어떤 값이 바뀌는가, 언제 끝나는가입니다. 이 네 가지를 추적하면 for와 while이 길어져도 흐름을 잃지 않습니다.",
                "매일 같은 알람을 30개 따로 만드는 대신 ‘매일 오전 7시에 울려라’라는 규칙 하나를 만드는 것과 비슷합니다. 반복문은 반복되는 일을 규칙으로 압축합니다.",
                null, null, null, null,
                "repetition · loop · iteration · loop body"));

        out.add(Track2Content.page(lesson, l, "이론", "for는 값을 하나씩 꺼내며 정해진 횟수를 반복한다",
                "for는 반복할 대상이 있을 때 가장 자연스럽습니다. 아직 리스트를 배우기 전이므로 range를 사용해 숫자 범위를 만들어 보겠습니다. range(3)은 0, 1, 2를 차례로 제공하므로 본문은 세 번 실행됩니다. range(1, 4)는 1, 2, 3을 제공합니다. 마지막 숫자는 포함되지 않습니다.\n\n"
                        + "각 iteration마다 반복 변수의 값이 달라집니다. for number in range(1, 4):에서 첫 번째 iteration의 number는 1, 두 번째는 2, 세 번째는 3입니다. print(number)를 본문 안에 두면 그 변화를 직접 확인할 수 있습니다. 반복 횟수를 계산할 때는 range의 시작과 끝을 정확히 읽는 습관이 중요합니다.",
                "for는 컨베이어 벨트에서 물건을 하나씩 받는 것과 같습니다. 매번 다음 값을 number에 올려 주고, 그 값으로 같은 작업을 실행합니다.",
                "for number in range(1, 4):\n    print(number)\n    print(number * 10)",
                null, null, null,
                "for · range · loop variable · iteration"));

        out.add(Track2Content.page(lesson, l, "이론", "while은 조건이 True인 동안 반복하므로 종료 변화를 확인한다",
                "while은 ‘몇 번’보다 ‘어떤 조건이 유지되는 동안’ 반복할 때 사용합니다. count = 1에서 시작해 while count <= 3:을 실행하면 count가 1, 2, 3일 때 본문이 실행됩니다. 본문에서 count += 1을 수행해 결국 4가 되면 조건이 False가 되어 끝납니다.\n\n"
                        + "while을 읽을 때는 조건에 사용된 변수가 본문 안에서 어떻게 바뀌는지 반드시 찾으세요. 그 변화가 없다면 infinite loop 가능성이 있습니다. 반대로 한 번에 2씩 증가한다면 예상보다 적은 횟수만 실행될 수도 있습니다. 실행 전에 표처럼 count 값을 1→2→3→4로 적으면 흐름이 선명해집니다.",
                "while은 ‘문이 열려 있는 동안 계속 들어간다’고 생각할 수 있습니다. 중요한 것은 누군가 결국 문을 닫아야 한다는 점입니다. count를 바꾸는 코드가 그 역할을 합니다.",
                "count = 1\n\nwhile count <= 3:\n    print(f\"{count}번째 실행\")\n    count += 1\n\nprint(\"반복 끝\")",
                null, null, null,
                "while · condition · counter · += · infinite loop"));

        out.add(Track2Content.page(lesson, l, "예제", "1부터 5까지 더하며 누적값을 관찰하기",
                "반복문은 단순 출력보다 계산을 누적할 때 더 유용해집니다. 아래 코드는 total을 0에서 시작하고, number가 1부터 5까지 바뀔 때마다 total에 현재 number를 더합니다. 첫 iteration 뒤 total은 1, 다음은 3, 그다음은 6, 10, 마지막은 15가 됩니다.\n\n"
                        + "이 패턴은 매출 합계, 지출 합계, 점수 합계처럼 실제 프로그램에서 계속 사용됩니다. 핵심은 누적 변수 total을 루프 밖에서 한 번 초기화하고, 루프 안에서 이전 값을 이용해 새 값을 만드는 것입니다. print를 루프 안에 넣으면 중간 상태를 관찰할 수 있고 밖에 넣으면 최종 결과만 볼 수 있습니다.",
                "저금통을 0원으로 시작하고 매일 돈을 넣는 모습과 같습니다. total은 지금까지 모인 돈, number는 오늘 넣는 돈입니다.",
                "total = 0\n\nfor number in range(1, 6):\n    total += number\n    print(f\"number={number}, total={total}\")\n\nprint(f\"최종 합계: {total}\")",
                null, null, null,
                "accumulator · total · initialization · intermediate value"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — for로 구구단 한 줄과 합계 만들기",
                "for의 반복 변수와 range를 직접 바꾸면서 횟수와 값의 관계를 익힙니다. 실행 전에 각 iteration에서 number가 어떤 값인지 적어 보세요. 특히 range의 끝값이 포함되지 않는다는 규칙을 확인합니다.\n\n"
                        + "두 번째 과제에서는 합계 누적 패턴을 사용합니다. total을 어디에서 0으로 만드는지에 따라 결과가 달라질 수 있으므로 초기화 위치에도 주의하세요.",
                "range(1, 10)은 1부터 9까지입니다. 10을 포함하려면 끝값을 11로 적어야 합니다.",
                "dan = 3\n\nfor number in range(1, 10):\n    print(dan * number)",
                "1. 각 출력 앞에 ‘3 x 1 = 3’처럼 곱하는 두 수까지 보이게 만들기\n2. dan을 7로 바꿔 같은 코드로 7단 실행하기\n3. 새 total 변수를 만들어 1부터 10까지의 합 구하기\n4. range(1, 10)과 range(1, 11)의 차이를 실제 결과로 설명하기",
                null, null,
                "for · range · multiplication · accumulator"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — for로 구구단 한 줄과 합계 만들기",
                "예시 답안\n"
                        + "구구단에서는 number가 1부터 9까지 바뀌고 dan은 그대로 유지됩니다. f-string을 사용하면 계산식의 구성과 결과를 한 줄에서 함께 볼 수 있습니다. 1부터 10까지의 합은 total = 0을 반복문 밖에 두고 매 iteration마다 number를 더합니다.\n\n"
                        + "range(1, 10)은 끝값 10을 포함하지 않아 1~9이고, range(1, 11)은 1~10입니다. Python에서 range의 끝값이 제외된다는 규칙은 이후 인덱스와 반복 범위를 다룰 때도 계속 중요합니다.",
                "dan = 7\nfor number in range(1, 10):\n    print(f\"{dan} x {number} = {dan * number}\")\n\ntotal = 0\nfor number in range(1, 11):\n    total += number\nprint(f\"1~10 합계: {total}\")"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — while로 목표 금액까지 저축하기",
                "while은 반복 횟수를 직접 정하기보다 상태가 목표에 도달할 때까지 반복하는 예제로 익혀 보겠습니다. balance는 현재 모인 돈, goal은 목표 금액, monthly는 한 번 반복할 때 추가되는 금액입니다. 각 iteration에서 balance가 바뀌고, 언젠가 balance < goal이 False가 되어야 합니다.\n\n"
                        + "monthly를 0으로 바꿨을 때 왜 문제가 생기는지도 생각해 보세요. while은 종료 조건을 설계하는 연습이 핵심입니다.",
                "현재 잔액이 목표보다 작은 동안만 저축을 계속합니다. 매달 돈이 늘지 않는다면 목표에 영원히 도달하지 못해 반복도 끝나지 않습니다.",
                "balance = 0\ngoal = 50000\nmonthly = 12000\nmonth = 0\n\nwhile balance < goal:\n    balance += monthly\n    month += 1\n    print(month, balance)",
                "1. 목표 금액에 도달한 뒤 총 몇 개월 걸렸는지 출력하기\n2. monthly를 20000으로 바꿔 반복 횟수 비교하기\n3. balance가 매번 어떻게 변하는지 표처럼 적기\n4. monthly = 0이면 왜 infinite loop가 되는지 자기 말로 설명하기",
                null, null,
                "while · goal · state change · counter · infinite loop"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — while로 목표 금액까지 저축하기",
                "예시 답안\n"
                        + "12,000원씩 모으면 1개월 12,000원, 2개월 24,000원, 3개월 36,000원, 4개월 48,000원, 5개월 60,000원이 되어 다섯 번 반복한 뒤 끝납니다. while 조건은 다음 반복 전에 다시 확인되므로 60,000원에서 60,000 < 50,000이 False가 되어 종료됩니다.\n\n"
                        + "monthly가 0이면 balance가 계속 0이라 balance < goal이 영원히 True입니다. 따라서 while에서는 조건에 영향을 주는 값이 실제로 변하는지 확인해야 합니다.",
                "balance = 0\ngoal = 50000\nmonthly = 12000\nmonth = 0\n\nwhile balance < goal:\n    balance += monthly\n    month += 1\n\nprint(f\"{month}개월 후 {balance}원, 목표 달성\")"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — 반복 횟수와 종료 조건 읽기",
                "반복문은 눈으로만 보면 횟수를 착각하기 쉽습니다. 각 문제에서 반복 변수나 counter의 값을 한 iteration씩 적어 보세요. for에서는 range가 제공하는 값의 개수를, while에서는 조건이 언제 False가 되는지를 확인하면 됩니다.",
                "코드 전체를 한 번에 보지 말고 ‘첫 번째 반복 전 값 → 첫 반복 후 값 → 다음 조건 검사’ 순서로 적으면 정확해집니다.",
                null, null,
                "1. for i in range(3)은 몇 번 실행되고 i 값은 차례로 무엇인가요?\n2. range(2, 5)가 제공하는 값은 무엇인가요?\n3. count = 0이고 while count < 2 안에서 count += 1을 하면 본문은 몇 번 실행되나요?\n4. while에서 종료 조건에 영향을 주는 값이 전혀 바뀌지 않으면 어떤 문제가 생길 수 있나요?",
                "1. 3번, 0·1·2\n2. 2·3·4\n3. 2번\n4. 무한 루프가 생길 수 있습니다.",
                "range · iteration · counter · termination · infinite loop"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — 반복 횟수와 종료 조건 읽기",
                "1. 3번, i는 0·1·2\n2. 2·3·4\n3. 2번\n4. infinite loop 가능",
                "range(3)은 끝값 3을 제외한 0,1,2를 제공합니다. range(2,5)는 시작 2부터 5 직전인 4까지입니다. while 예제에서는 count가 0일 때 첫 실행 후 1, 다시 조건이 True라 두 번째 실행 후 2가 되고, 2 < 2가 False가 되어 끝납니다. 종료 조건을 바꾸는 코드가 없다면 True가 계속 유지될 수 있습니다.",
                "range(1, 6), range(0, 10, 2)를 직접 실행해 어떤 값을 만드는지 확인해 보세요. while count < 5에서 count += 2를 사용하면 count가 어떤 순서로 바뀌는지도 적어 보세요.",
                "range의 끝값을 포함한다고 착각하거나, while 본문에서 counter 증가를 빼먹거나, 누적 변수를 반복문 안에서 매번 0으로 다시 만드는 것이 흔한 실수입니다."));

        out.add(Track2Content.page(lesson, l, "정리", "챕터 2 정리 — 반복할 대상과 종료 조건을 먼저 찾는다",
                "for는 값의 모음이나 range를 하나씩 꺼내며 반복하고, while은 조건이 True인 동안 반복합니다. 반복문을 읽을 때는 현재 iteration의 값, 바뀌는 변수, 종료 조건을 추적하세요. total += number 같은 누적 패턴은 여러 데이터를 합칠 때 매우 자주 사용됩니다.\n\n"
                        + "반복을 무조건 줄이는 것이 목표가 아니라, 같은 규칙을 한 곳에 표현해 수정과 검증을 쉽게 만드는 것이 핵심입니다. 다음 챕터에서는 반복해서 쓰는 코드 덩어리 자체에 이름을 붙여 다시 호출할 수 있게 만드는 함수를 배웁니다.",
                "for는 ‘하나씩 꺼내기’, while은 ‘조건이 맞는 동안 계속하기’입니다. 둘 다 반복 본문이 몇 번, 어떤 값으로 실행되는지 추적할 수 있어야 합니다.",
                null, null, null, null,
                "for · while · range · iteration · counter · accumulator · break · continue · infinite loop"));
    }
}
