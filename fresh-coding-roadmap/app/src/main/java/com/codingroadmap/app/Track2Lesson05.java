package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson05 {
    private Track2Lesson05() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 5;
        final String l = "문자열을 자르고 바꾸고 정리하기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 문자열·문자·인덱스·슬라이스",
                "문자열(string / str)\n"
                        + "글자들의 순서 있는 묶음입니다. 이름, 주소, 메시지, 파일 한 줄처럼 텍스트로 다루는 데이터가 문자열입니다. Python에서는 보통 따옴표로 만들며, 문자열도 순서가 있기 때문에 list처럼 index와 slice 개념을 사용할 수 있습니다.\n\n"
                        + "문자(character)\n"
                        + "문자열을 이루는 각각의 글자 하나를 뜻합니다. Python에는 별도의 char 타입이 없어서 text[0]으로 한 글자를 꺼내도 결과 타입은 길이가 1인 str입니다.\n\n"
                        + "문자열 인덱스(string index)\n"
                        + "각 문자의 위치 번호입니다. list와 똑같이 0부터 시작합니다. text = \"Python\"이면 text[0]은 \"P\", text[1]은 \"y\"입니다. 음수 index -1은 마지막 글자를 가리키는 문법도 있습니다.\n\n"
                        + "문자열 슬라이스(string slice)\n"
                        + "문자열의 일부 구간을 잘라 새 문자열을 얻는 문법입니다. text[0:3]은 index 0부터 3 직전까지 가져옵니다. 원본 문자열을 직접 변경하는 것이 아니라 잘라낸 새 문자열을 만듭니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — 메서드·strip·lower·upper·replace",
                "메서드(method)\n"
                        + "특정 값과 연결되어 그 값을 다루는 기능입니다. text.strip(), text.lower()처럼 점(.) 뒤에 이름을 붙여 호출합니다. 지금 단계에서는 ‘문자열이 제공하는 전용 함수’라고 이해해도 충분합니다.\n\n"
                        + "strip\n"
                        + "문자열 앞뒤의 불필요한 공백이나 줄바꿈을 제거한 새 문자열을 돌려줍니다. 사용자 입력이나 파일에서 읽은 줄을 정리할 때 매우 자주 사용합니다. 원본 변수 자체가 자동으로 바뀌는 것은 아니므로 text = text.strip()처럼 결과를 다시 저장할 수 있습니다.\n\n"
                        + "lower / upper\n"
                        + "영문 문자열을 각각 소문자 또는 대문자로 바꾼 새 문자열을 반환합니다. 이메일이나 명령어처럼 대소문자 차이를 줄여 비교하고 싶을 때 lower를 자주 사용합니다.\n\n"
                        + "replace\n"
                        + "문자열 안의 특정 부분을 다른 문자열로 바꾼 새 값을 반환합니다. phone.replace(\"-\", \"\")는 전화번호에서 하이픈을 제거할 수 있습니다. 원본을 직접 수정하지 않는다는 점이 중요합니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — split·join·구분자·토큰",
                "split\n"
                        + "하나의 문자열을 기준 문자로 나눠 list로 만드는 메서드입니다. \"사과,바나나,포도\".split(\",\")은 [\"사과\", \"바나나\", \"포도\"]를 만듭니다. 파일 한 줄이나 CSV 비슷한 텍스트를 항목별로 나눌 때 자주 사용합니다.\n\n"
                        + "join\n"
                        + "여러 문자열을 하나의 문자열로 합치는 메서드입니다. \", \".join(items)는 list의 문자열 사이에 쉼표와 공백을 넣어 연결합니다. split과 반대 방향의 작업이라고 생각하면 쉽습니다.\n\n"
                        + "구분자(delimiter / separator)\n"
                        + "문자열을 어디에서 나눌지 표시하는 기준 문자입니다. 쉼표, 탭, 공백, | 같은 문자가 delimiter가 될 수 있습니다. 데이터의 실제 내용에 같은 문자가 들어갈 수 있는지까지 고려해야 안전한 형식을 만들 수 있습니다.\n\n"
                        + "토큰(token)\n"
                        + "문자열을 일정 기준으로 나눈 각각의 조각을 넓게 부르는 말입니다. split으로 나눈 단어 하나를 token이라고 부르기도 합니다. 이후 텍스트 처리나 AI 관련 설명에서도 자주 만나게 됩니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — f-string·정규화·불변성·검증",
                "f-string\n"
                        + "문자열 안에 변수나 계산 결과를 쉽게 넣는 Python 문법입니다. f\"총액: {total}원\"처럼 문자열 앞에 f를 붙이고 중괄호 안에 값을 적습니다. 여러 값을 읽기 좋은 문장으로 조립할 때 편리합니다.\n\n"
                        + "정규화(normalization)\n"
                        + "같은 의미의 입력을 일정한 형식으로 정리하는 작업입니다. 이메일 앞뒤 공백을 지우고 소문자로 바꾸거나 전화번호의 하이픈을 제거하는 것이 예입니다. 데이터를 비교하거나 저장하기 전에 normalization을 하면 오류가 줄어듭니다.\n\n"
                        + "불변(immutable)\n"
                        + "문자열 객체 자체의 글자를 제자리에서 바꿀 수 없다는 성질입니다. lower, replace 같은 메서드는 원본을 바꾸는 대신 새 문자열을 반환합니다. 그래서 결과를 계속 쓰려면 변수에 다시 대입해야 합니다.\n\n"
                        + "검증(validation)\n"
                        + "입력 문자열이 기대한 형식인지 확인하는 작업입니다. 빈 문자열인지, 필요한 구분자가 있는지, 숫자로 바꿀 부분이 실제 숫자 형태인지 등을 확인할 수 있습니다. 문자열 처리와 예외 처리는 실제 프로그램에서 자주 함께 사용됩니다."));

        out.add(Track2Content.page(lesson, l, "시작", "현실의 데이터는 대부분 깨끗한 문자열로 들어오지 않는다",
                "사용자는 이름 앞에 공백을 넣을 수 있고, 이메일에 대문자를 섞을 수 있으며, 전화번호는 010-1234-5678 또는 01012345678처럼 여러 형식으로 입력할 수 있습니다. 파일에서 읽은 줄에는 줄바꿈 문자가 붙어 있기도 합니다. 프로그램은 이런 문자열을 그대로 믿기보다 필요한 형태로 정리하고 분리해야 합니다.\n\n"
                        + "문자열 처리에서 중요한 질문은 세 가지입니다. 원본이 어떤 형식인가, 어떤 기준으로 자를 것인가, 최종적으로 어떤 형식이 필요한가입니다. 메서드 이름을 외우기보다 입력과 원하는 출력 사이의 변환 과정을 단계로 나누세요.",
                "택배 주소를 적을 때 띄어쓰기나 대소문자가 제각각이면 먼저 형식을 정리하는 것과 같습니다. 컴퓨터도 데이터를 비교하기 전에 모양을 맞추면 훨씬 안전합니다.",
                null, null, null, null,
                "string · text · normalization · validation"));

        out.add(Track2Content.page(lesson, l, "이론", "문자열도 순서가 있어 index와 slice로 일부를 읽을 수 있다",
                "문자열은 문자들이 순서대로 이어진 값이므로 list처럼 index가 있습니다. text[0]은 첫 글자이고 text[-1]은 마지막 글자입니다. slice를 사용하면 특정 구간을 가져올 수 있습니다. code = \"KR-2026-001\"에서 code[0:2]는 \"KR\"입니다.\n\n"
                        + "하지만 문자열은 immutable이므로 text[0] = \"A\"처럼 한 글자를 직접 교체할 수 없습니다. 대신 필요한 부분을 잘라 새 문자열을 만들거나 replace를 사용합니다. ‘조회는 index로 가능하지만 직접 수정은 새 문자열을 만들어야 한다’고 기억하세요.",
                "문자열은 글자가 적힌 종이 테이프와 비슷합니다. 몇 번째 글자를 읽거나 일부를 잘라 복사할 수 있지만 원본 테이프의 한 칸만 Python 방식으로 바로 갈아 끼우지는 못합니다.",
                "text = \"Python\"\nprint(text[0])\nprint(text[-1])\nprint(text[0:3])\nprint(len(text))",
                null, null, null,
                "index · negative index · slice · immutable"));

        out.add(Track2Content.page(lesson, l, "이론", "문자열 메서드는 새 값을 반환하므로 변환 결과를 이어서 사용한다",
                "strip, lower, upper, replace 같은 메서드는 정리된 새 문자열을 반환합니다. raw = \"  TEST@EMAIL.COM  \"에서 raw.strip().lower()를 실행하면 앞뒤 공백을 제거한 뒤 소문자로 바꾼 결과를 얻을 수 있습니다. 여러 메서드를 연속으로 호출하는 것을 method chaining이라고 부르기도 합니다.\n\n"
                        + "변환 후 원본과 결과를 둘 다 print해 보세요. raw는 그대로이고 clean에만 새 값이 저장된다는 것을 확인할 수 있습니다. 이 차이를 이해하면 ‘메서드를 호출했는데 왜 변수가 안 바뀌었지?’라는 혼란을 피할 수 있습니다.",
                "사진 편집본을 새 파일로 만드는 것과 비슷합니다. 원본 사진은 남아 있고 strip이나 lower가 만든 결과를 새 변수에 받습니다.",
                "raw = \"  TEST@EMAIL.COM  \"\nclean = raw.strip().lower()\n\nprint(raw)\nprint(clean)\n\nphone = \"010-1234-5678\"\nprint(phone.replace(\"-\", \"\"))",
                null, null, null,
                "method · strip · lower · replace · chaining"));

        out.add(Track2Content.page(lesson, l, "예제", "쉼표로 된 한 줄을 split해 각 필드로 나누기",
                "프로그램은 텍스트 한 줄 안에 여러 정보를 담은 데이터를 자주 만납니다. \"사과,3,1500\"은 사람 눈에는 상품명, 수량, 가격처럼 보이지만 Python에게는 처음에는 문자열 하나입니다. split(\",\")을 사용하면 세 조각의 list가 되고, 각 위치를 변수에 연결한 뒤 숫자 필드는 int로 변환할 수 있습니다.\n\n"
                        + "이때 데이터 형식을 미리 약속하는 것이 중요합니다. 항상 ‘이름,개수,가격’ 순서라고 정했기 때문에 index 0,1,2의 의미를 알 수 있습니다. 실제 시스템에서는 형식이 깨진 줄에 대한 검증도 필요하며 챕터 7에서 예외 처리와 함께 다룹니다.",
                "영수증 한 줄에 쉼표가 칸막이 역할을 한다고 생각하세요. split은 칸막이를 기준으로 세 칸을 분리합니다.",
                "line = \"사과,3,1500\"\nparts = line.split(\",\")\n\nname = parts[0]\ncount = int(parts[1])\nprice = int(parts[2])\n\nprint(name)\nprint(count * price)",
                null, null, null,
                "split · delimiter · list · int · field"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — 이메일 입력을 일정한 형식으로 정리하기",
                "사용자가 입력한 이메일에는 앞뒤 공백과 대문자가 섞일 수 있습니다. 비교나 저장 전에 strip과 lower를 사용해 normalization해 봅니다. 변환 단계마다 값을 출력하면 각 메서드가 무엇을 바꾸는지 분명히 볼 수 있습니다.\n\n"
                        + "정규화는 데이터의 의미를 바꾸면 안 됩니다. 이메일의 일반적인 비교에서는 소문자화가 학습 예제로 적절하지만, 모든 종류의 문자열을 무조건 lower 처리해야 하는 것은 아닙니다. 필요한 규칙만 적용하세요.",
                "‘  USER@Example.COM  ’과 ‘user@example.com’을 같은 형식으로 만들면 이후 비교가 쉬워집니다.",
                "email = input(\"이메일: \")\n# clean_email을 만들어 보세요",
                "1. strip으로 앞뒤 공백 제거하기\n2. lower로 영문을 소문자로 만들기\n3. clean_email 변수에 최종 결과 저장하기\n4. 원본 email과 clean_email을 모두 출력해 차이 확인하기\n5. clean_email 안에 ‘@’가 있는지 in으로 검사하기",
                null, null,
                "strip · lower · normalization · in · clean data"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — 이메일 입력을 일정한 형식으로 정리하기",
                "예시 답안\n"
                        + "strip은 문자열 양끝의 공백을 제거하고 lower는 영문을 소문자로 바꾼 새 문자열을 반환합니다. clean_email에 두 결과를 이어서 저장하면 이후 저장이나 비교에 사용할 일정한 형식을 얻습니다. 원본 email은 그대로 남아 있으므로 둘을 출력하면 immutable 성질도 확인할 수 있습니다.\n\n"
                        + "‘@’ in clean_email은 아주 간단한 형식 확인일 뿐 완전한 이메일 검증은 아닙니다. 실제 서비스의 검증은 더 복잡할 수 있지만, 지금은 문자열 조건을 조합하는 연습으로 충분합니다.",
                "email = input(\"이메일: \")\nclean_email = email.strip().lower()\n\nprint(f\"원본: [{email}]\")\nprint(f\"정리: [{clean_email}]\")\nprint(f\"@ 포함: {'@' in clean_email}\")"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — 상품 한 줄을 split하고 다시 join하기",
                "‘상품명,수량,가격’ 형식 문자열을 list로 나누고 각 필드를 정리한 뒤 계산합니다. 그 다음 사람이 읽기 좋은 문자열 list를 만들어 join으로 한 줄에 합쳐 봅니다. split이 문자열→list, join이 여러 문자열→문자열 방향이라는 점을 몸으로 익히는 실습입니다.\n\n"
                        + "숫자 계산을 하려면 split 결과의 숫자 조각도 처음에는 str이라는 점을 잊지 마세요. int 변환이 필요합니다.",
                "split한 뒤 나오는 ‘3’은 여전히 글자입니다. 3개라는 숫자로 계산하려면 int('3')을 거쳐야 합니다.",
                "line = \"노트,4,2500\"\nparts = line.split(\",\")",
                "1. name, count, price 변수로 각각 꺼내기\n2. count와 price를 int로 바꿔 total 계산하기\n3. 결과 문자열 list [name, ‘4개’, ‘10000원’] 만들기\n4. ‘ | ’.join(...)으로 한 줄 출력하기\n5. 입력 줄 앞뒤에 공백이 있을 때 strip을 어느 단계에 넣을지 생각하기",
                null, null,
                "split · int · total · join · delimiter"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — 상품 한 줄을 split하고 다시 join하기",
                "예시 답안\n"
                        + "split 결과는 모두 문자열이므로 수량과 가격을 int로 바꾼 뒤 곱합니다. join은 문자열들만 연결할 수 있으므로 숫자를 그대로 넣기보다 f-string으로 ‘4개’, ‘10000원’ 같은 문자열을 만들어 list에 넣습니다.\n\n"
                        + "원본 한 줄 전체의 앞뒤 공백은 split 전에 strip할 수 있고, 각 필드 자체에도 공백이 들어올 가능성이 있다면 parts의 각 문자열을 추가로 strip해야 합니다. 데이터 형식에 맞춰 정리 위치를 선택하세요.",
                "line = \"노트,4,2500\".strip()\nparts = line.split(\",\")\n\nname = parts[0].strip()\ncount = int(parts[1].strip())\nprice = int(parts[2].strip())\ntotal = count * price\n\nresult = [name, f\"{count}개\", f\"{total}원\"]\nprint(\" | \".join(result))"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — 문자열 변환의 입력과 결과 타입 읽기",
                "문자열 메서드는 이름만 외우기보다 무엇을 입력으로 보고 어떤 타입의 결과를 만드는지 이해해야 합니다. 특히 split은 list를 만들고 join은 문자열을 만든다는 방향을 구분하세요.",
                "화살표로 적어 보세요. ‘문자열 --split--> list’, ‘문자열 list --join--> 문자열’처럼 결과 타입을 함께 적으면 덜 헷갈립니다.",
                null, null,
                "1. \"  hello  \".strip()의 결과는 무엇인가요?\n2. \"ABC\".lower()의 결과는 무엇인가요?\n3. \"a,b,c\".split(\",\")의 결과 타입은 str과 list 중 무엇인가요?\n4. 문자열 메서드를 호출했는데 원래 변수도 자동으로 바뀐다고 생각해도 될까요?",
                "1. \"hello\"\n2. \"abc\"\n3. list\n4. 아닙니다. 문자열은 immutable이며 메서드는 보통 새 문자열을 반환합니다.",
                "strip · lower · split · list · immutable"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — 문자열 변환의 입력과 결과 타입 읽기",
                "1. \"hello\"\n2. \"abc\"\n3. list\n4. 자동으로 바뀌지 않습니다.",
                "strip은 양끝 공백을 제거한 새 문자열을, lower는 소문자로 바꾼 새 문자열을 반환합니다. split은 하나의 문자열을 여러 조각으로 나누므로 결과가 list입니다. 문자열은 immutable이라 원본 객체를 제자리에서 바꾸지 않으므로 결과를 변수에 다시 저장해야 계속 사용할 수 있습니다.",
                "raw = \" A-B-C \"를 strip → lower → replace 순서로 변환해 ‘abc’를 만들어 보세요. 각 단계 결과를 별도 변수에 저장해서 어떤 변화가 생기는지도 확인하세요.",
                "split 결과가 문자열이라고 착각하거나, 숫자 모양 문자열을 int 변환 없이 계산하거나, 메서드 결과를 다시 저장하지 않고 원본이 변했다고 생각하는 것이 흔한 실수입니다."));

        out.add(Track2Content.page(lesson, l, "정리", "챕터 5 정리 — 문자열은 정리하고 나누고 다시 조립한다",
                "문자열은 index와 slice로 일부를 읽을 수 있고, strip·lower·replace 같은 메서드로 새 문자열을 만들어 정리할 수 있습니다. split은 문자열을 list로 나누고 join은 문자열 list를 다시 하나로 합칩니다. f-string은 여러 값을 사람이 읽기 좋은 문장으로 조립할 때 유용합니다.\n\n"
                        + "실제 프로그램에서는 입력 문자열을 그대로 쓰지 않고 normalization과 validation을 거치는 경우가 많습니다. 다음 챕터에서는 이렇게 정리한 데이터를 프로그램이 끝난 뒤에도 남길 수 있도록 파일에 읽고 쓰는 방법을 배웁니다.",
                "문자열 처리의 큰 흐름은 ‘원본 확인 → 정리 → 분리/변환 → 필요한 형식으로 조립’입니다.",
                null, null, null, null,
                "string · index · slice · method · strip · replace · split · join · f-string · normalization"));
    }
}
