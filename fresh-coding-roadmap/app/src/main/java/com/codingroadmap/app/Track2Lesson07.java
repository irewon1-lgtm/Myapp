package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson07 {
    private Track2Lesson07() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 7;
        final String l = "예외 처리로 오류가 나도 프로그램을 이어가기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 예외·try·except·처리",
                "예외(exception)\n"
                        + "프로그램 실행 중 정상 흐름을 계속할 수 없게 만드는 문제를 Python이 객체로 표현한 것입니다. 잘못된 숫자 변환의 ValueError, 0으로 나눌 때의 ZeroDivisionError, 없는 파일의 FileNotFoundError 등이 예입니다. 모든 오류를 숨기는 것이 아니라 예상 가능한 문제를 구분해 대응하는 것이 중요합니다.\n\n"
                        + "try\n"
                        + "예외가 발생할 가능성이 있는 코드를 넣는 블록입니다. Python은 try 블록을 정상 실행하다 예외가 발생하면 남은 try 코드를 건너뛰고 맞는 except를 찾습니다. ‘일단 시도해 보고 문제가 생기면 정해 둔 처리로 간다’는 구조입니다.\n\n"
                        + "except\n"
                        + "특정 예외가 발생했을 때 실행할 처리 블록입니다. except ValueError:처럼 예외 종류를 명시하면 어떤 실패를 예상했는지 코드에 드러납니다. 원인을 모르는 모든 오류를 무조건 잡는 것보다 가능한 한 구체적인 예외를 처리하는 것이 좋습니다.\n\n"
                        + "예외 처리(exception handling)\n"
                        + "예외가 발생했을 때 프로그램을 바로 끝내는 대신 메시지를 보여 주거나 다시 입력받거나 기본값을 사용하는 등 안전한 다음 행동을 정하는 것입니다. 예외를 없애는 것이 아니라 실패를 의도적으로 다루는 설계입니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — ValueError·ZeroDivisionError·FileNotFoundError",
                "ValueError\n"
                        + "타입 자체는 맞는 변환을 시도했지만 값의 내용이 기대 형식에 맞지 않을 때 흔히 발생합니다. int(\"abc\")처럼 문자열을 정수로 바꾸려고 했지만 숫자 형태가 아니면 ValueError가 납니다. 사용자 숫자 입력 처리에서 자주 만납니다.\n\n"
                        + "ZeroDivisionError\n"
                        + "숫자를 0으로 나누려고 할 때 발생합니다. division 계산에서는 입력 타입이 숫자여도 분모가 0인지 별도 검사가 필요하다는 뜻입니다.\n\n"
                        + "FileNotFoundError\n"
                        + "읽으려는 파일이나 경로를 찾을 수 없을 때 발생합니다. 파일 이름 오타, 다른 current working directory, 아직 생성되지 않은 파일 등이 원인일 수 있습니다. 파일 처리에서 자주 대비하는 예외입니다.\n\n"
                        + "예외 타입(exception type)\n"
                        + "어떤 종류의 실패인지 나타내는 이름입니다. traceback의 마지막 줄에서 ValueError 같은 type을 확인하면 조사 범위를 좁힐 수 있습니다. 서로 다른 exception type마다 대응 방법도 달라질 수 있습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — else·finally·raise·메시지",
                "try의 else\n"
                        + "try 블록에서 예외가 발생하지 않았을 때만 실행되는 선택 블록입니다. 위험한 작업과 성공 후 작업을 분리하고 싶을 때 사용할 수 있습니다. 처음에는 try/except를 먼저 익히고 필요할 때 else를 추가하면 됩니다.\n\n"
                        + "finally\n"
                        + "예외 발생 여부와 관계없이 마지막에 실행되는 블록입니다. 반드시 정리해야 할 자원이 있을 때 유용합니다. 파일은 with가 정리를 도와주므로 무조건 finally를 써야 하는 것은 아닙니다.\n\n"
                        + "raise\n"
                        + "코드에서 의도적으로 예외를 발생시키는 키워드입니다. 함수가 허용할 수 없는 값이나 상태를 받았을 때 ‘이 입력은 잘못됐다’고 명확히 알릴 수 있습니다. 초보 단계에서는 읽을 수 있는 정도로 익혀 둡니다.\n\n"
                        + "예외 메시지(exception message)\n"
                        + "예외 type 뒤에 나오는 구체적인 설명입니다. 같은 ValueError라도 어떤 값 때문에 발생했는지 힌트를 줄 수 있습니다. 디버깅할 때 type과 message를 함께 읽어야 합니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — 복구·폴백·실패 경로·너무 넓은 except",
                "복구(recovery)\n"
                        + "예외가 발생한 뒤 프로그램이 안전한 상태로 돌아가 다음 행동을 할 수 있게 만드는 것입니다. 다시 입력받기, 해당 데이터만 건너뛰기, 기본값 사용 등이 recovery 전략이 될 수 있습니다.\n\n"
                        + "폴백(fallback)\n"
                        + "원래 방법이 실패했을 때 대신 사용할 대체 방법이나 기본값입니다. 설정 파일이 없으면 기본 설정을 사용하는 것이 예입니다. fallback은 편리하지만 실패 사실을 완전히 숨기면 원인 발견이 어려울 수 있습니다.\n\n"
                        + "실패 경로(failure path)\n"
                        + "정상 성공 경로가 아니라 문제가 생겼을 때 프로그램이 따라가는 실행 흐름입니다. 좋은 프로그램은 성공 path뿐 아니라 예상 가능한 failure path도 설계합니다.\n\n"
                        + "너무 넓은 except(broad exception catch)\n"
                        + "except: 또는 너무 일반적인 Exception으로 모든 문제를 한꺼번에 잡으면 실제 버그까지 숨길 수 있습니다. 학습 초기에도 ValueError, FileNotFoundError처럼 예상하는 exception type을 구체적으로 적는 습관이 좋습니다."));

        out.add(Track2Content.page(lesson, l, "시작", "오류를 없애는 것과 예상 가능한 실패를 처리하는 것은 다르다",
                "잘 만든 프로그램도 사용자가 숫자 대신 글자를 입력하거나, 필요한 파일이 없거나, 네트워크가 끊기는 등 외부 상황 때문에 실패할 수 있습니다. 이런 상황까지 모두 ‘버그니까 절대 일어나면 안 된다’고 볼 수는 없습니다. 예외 처리는 예상 가능한 실패가 생겼을 때 프로그램이 어떤 메시지와 다음 행동을 선택할지 정하는 방법입니다.\n\n"
                        + "중요한 원칙은 오류를 무조건 숨기지 않는 것입니다. 예상한 예외만 구체적으로 처리하고, 개발 중인 진짜 버그는 traceback을 통해 발견할 수 있게 해야 합니다. ‘프로그램이 안 죽으면 좋은 코드’가 아니라 ‘어떤 실패를 왜 처리하는지 분명한 코드’가 목표입니다.",
                "식당에서 원하는 메뉴가 품절됐을 때 가게 전체를 닫는 대신 ‘이 메뉴는 품절입니다. 다른 메뉴를 선택해 주세요’라고 대응하는 것이 exception handling과 비슷합니다.",
                null, null, null, null,
                "exception · failure path · handling · recovery"));

        out.add(Track2Content.page(lesson, l, "이론", "try에서 실패하면 맞는 except로 실행 흐름이 이동한다",
                "Python은 try 블록을 위에서부터 실행합니다. 중간에 ValueError가 발생하면 그 아래 try 줄은 실행하지 않고 except ValueError 블록을 찾습니다. 일치하는 except가 있으면 그 코드를 실행한 뒤 try/except 구조 다음으로 이동할 수 있습니다.\n\n"
                        + "예외가 전혀 발생하지 않으면 except 블록은 실행하지 않습니다. 따라서 try 안에는 실제로 실패할 가능성이 있는 최소한의 코드만 두는 편이 원인을 이해하기 쉽습니다. 너무 많은 코드를 한 try에 넣으면 어느 작업이 실패했는지 흐려질 수 있습니다.",
                "try는 위험할 수 있는 작업 구역이고 except는 특정 사고가 났을 때 사용하는 비상 통로입니다. 사고 종류에 맞는 통로를 골라야 합니다.",
                "text = input(\"숫자: \")\n\ntry:\n    number = int(text)\n    print(number * 2)\nexcept ValueError:\n    print(\"숫자 형태로 입력해 주세요.\")\n\nprint(\"프로그램 계속\")",
                null, null, null,
                "try · except · ValueError · control flow"));

        out.add(Track2Content.page(lesson, l, "이론", "예외 타입마다 원인과 복구 방법이 다르므로 구체적으로 나눈다",
                "숫자 변환 실패와 0으로 나누는 실패는 사용자에게 설명할 내용과 고칠 방법이 다릅니다. 그래서 except ValueError와 except ZeroDivisionError를 따로 둘 수 있습니다. 여러 예외를 구분하면 프로그램의 failure path가 문서처럼 드러납니다.\n\n"
                        + "예외 처리 전에 if로 예방할 수 있는 조건도 있습니다. 분모가 0인지 if로 검사할 수도 있고 ZeroDivisionError를 처리할 수도 있습니다. 어느 방식이든 의도가 분명해야 하며, 같은 문제를 이중으로 복잡하게 처리할 필요는 없습니다. 처음에는 입력 변환처럼 예외가 자연스러운 경계에서 try/except를 사용하세요.",
                "‘숫자가 아님’과 ‘0으로 나눔’은 둘 다 계산 실패지만 이유가 다릅니다. 병원에서 증상에 따라 다른 처치를 하듯 exception type별로 대응합니다.",
                "try:\n    a = int(input(\"첫 숫자: \"))\n    b = int(input(\"둘째 숫자: \"))\n    print(a / b)\nexcept ValueError:\n    print(\"정수를 입력해 주세요.\")\nexcept ZeroDivisionError:\n    print(\"0으로 나눌 수 없습니다.\")",
                null, null, null,
                "specific exception · ValueError · ZeroDivisionError · recovery"));

        out.add(Track2Content.page(lesson, l, "예제", "사용자가 올바른 정수를 입력할 때까지 다시 묻기",
                "while과 try/except를 결합하면 잘못된 입력을 받은 뒤 프로그램을 끝내지 않고 다시 입력받을 수 있습니다. while True로 반복하고 int 변환이 성공하면 break로 빠져나옵니다. ValueError가 나면 안내만 하고 다음 iteration에서 다시 입력받습니다.\n\n"
                        + "이 패턴에서 break가 성공 경로의 종료 조건 역할을 합니다. 무한 반복처럼 보이는 while True라도 성공 시 반드시 break가 실행되는 구조가 명확해야 합니다. 실제 프로그램에서는 반복 횟수 제한이 필요한 경우도 있지만 여기서는 흐름 이해에 집중합니다.",
                "잘못된 비밀번호를 입력하면 앱이 바로 종료되지 않고 다시 입력창을 보여 주는 것과 비슷합니다.",
                "while True:\n    try:\n        age = int(input(\"나이: \"))\n        break\n    except ValueError:\n        print(\"숫자로 입력해 주세요.\")\n\nprint(f\"입력된 나이: {age}\")",
                null, null, null,
                "while True · break · retry · ValueError"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — 안전한 나눗셈 프로그램 만들기",
                "두 값을 입력받아 나누는 프로그램에 두 종류의 failure path를 만듭니다. 숫자가 아닌 입력과 0으로 나누는 경우를 서로 다른 메시지로 처리하세요. 정상 계산일 때는 결과를 보여 주고 마지막에는 프로그램이 끝났다는 문구를 출력합니다.\n\n"
                        + "각 예외를 실제로 한 번씩 발생시켜 테스트해야 합니다. 정상 입력만 테스트하면 exception handling 코드가 실제로 작동하는지 알 수 없습니다.",
                "테스트 입력을 세 세트 준비하세요. ‘10과 2’, ‘hello와 2’, ‘10과 0’이면 성공·변환 실패·0 나누기를 모두 확인할 수 있습니다.",
                "try:\n    a = int(input(\"a: \") )\n    b = int(input(\"b: \") )\n    # 나눗셈과 출력\nexcept ValueError:\n    pass\nexcept ZeroDivisionError:\n    pass",
                "1. 정상일 때 a / b 결과 출력하기\n2. ValueError에는 ‘숫자를 입력하세요’ 출력하기\n3. ZeroDivisionError에는 ‘0으로 나눌 수 없습니다’ 출력하기\n4. 세 테스트 케이스를 실행해 각각 다른 경로 확인하기\n5. try/except 다음에 ‘종료’ 문구를 넣고 모든 경우 출력되는지 확인하기",
                null, null,
                "try · ValueError · ZeroDivisionError · test case"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — 안전한 나눗셈 프로그램 만들기",
                "예시 답안\n"
                        + "10과 2에서는 예외가 없어 5.0이 출력되고 except는 건너뜁니다. hello를 int로 바꾸려 하면 ValueError가 발생해 첫 except로 이동합니다. b가 0이면 변환은 성공하지만 나눗셈에서 ZeroDivisionError가 발생해 두 번째 except로 갑니다.\n\n"
                        + "세 경우 모두 try/except 구조가 끝난 뒤의 ‘프로그램 종료’는 실행됩니다. 예외별 테스트 케이스를 따로 만드는 것이 중요합니다.",
                "try:\n    a = int(input(\"a: \") )\n    b = int(input(\"b: \") )\n    print(a / b)\nexcept ValueError:\n    print(\"숫자를 입력하세요.\")\nexcept ZeroDivisionError:\n    print(\"0으로 나눌 수 없습니다.\")\n\nprint(\"프로그램 종료\")"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — 없는 파일에는 안내하고 기본 내용을 사용하기",
                "파일을 r mode로 열 때 파일이 없으면 FileNotFoundError가 날 수 있습니다. 이 예외를 처리해 프로그램이 중단되지 않게 하고, fallback 문자열을 사용해 다음 코드를 계속 실행해 봅니다. 그 다음 실제 파일을 만들었을 때 정상 경로로 바뀌는지도 확인합니다.\n\n"
                        + "fallback을 사용하더라도 실패 사실을 사용자나 로그에 알리는 것이 좋습니다. 조용히 기본값만 사용하면 파일 이름 오타 같은 진짜 문제를 놓칠 수 있습니다.",
                "책을 찾으러 갔는데 없으면 ‘책이 없습니다’라고 알리고 빈 종이를 대신 사용하는 구조입니다. 없다는 사실을 숨기지는 않습니다.",
                "try:\n    with open(\"settings.txt\", \"r\", encoding=\"utf-8\") as file:\n        settings = file.read()\nexcept FileNotFoundError:\n    # 안내 후 기본값을 넣으세요\n    pass\n\nprint(settings)",
                "1. FileNotFoundError에서 ‘설정 파일 없음’ 출력하기\n2. settings = ‘default’라는 fallback 넣기\n3. 파일이 없을 때 프로그램이 계속되는지 확인하기\n4. settings.txt를 만든 뒤 파일 내용이 사용되는지 다시 확인하기",
                null, null,
                "FileNotFoundError · fallback · file · recovery"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — 없는 파일에는 안내하고 기본 내용을 사용하기",
                "예시 답안\n"
                        + "파일이 없으면 open 단계에서 FileNotFoundError가 발생하고 try의 아래 코드는 건너뛴 뒤 except가 실행됩니다. 여기서 settings에 default를 넣었기 때문에 try/except 다음의 print도 안전하게 실행됩니다. 실제 파일이 있으면 except는 실행되지 않고 파일 내용이 settings에 저장됩니다.\n\n"
                        + "중요한 점은 except가 모든 오류를 숨기는 장치가 아니라 ‘파일 없음은 예상 가능한 상황’이라고 명시적으로 처리하는 코드라는 것입니다.",
                "try:\n    with open(\"settings.txt\", \"r\", encoding=\"utf-8\") as file:\n        settings = file.read().strip()\nexcept FileNotFoundError:\n    print(\"설정 파일 없음 — 기본값 사용\")\n    settings = \"default\"\n\nprint(f\"설정: {settings}\")"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — 어떤 예외를 어디서 처리할지 구분하기",
                "예외 처리는 문법 하나보다 ‘어떤 작업이 어떤 방식으로 실패할 수 있는가’를 연결하는 능력이 중요합니다. 아래 상황마다 가장 관련 있는 exception type을 떠올려 보세요.",
                "오류 이름을 외우기 어려우면 실제로 한 번 발생시켜 traceback 마지막 줄을 읽어 보세요. 이름과 원인이 함께 기억됩니다.",
                null, null,
                "1. int(\"hello\")에서 대표적으로 발생하는 예외는 무엇인가요?\n2. 10 / 0에서 발생하는 예외는 무엇인가요?\n3. 없는 파일을 r mode로 열 때 발생할 수 있는 예외는 무엇인가요?\n4. except:로 모든 오류를 무조건 숨기는 것보다 구체적인 exception type을 적는 편이 좋은 이유는 무엇인가요?",
                "1. ValueError\n2. ZeroDivisionError\n3. FileNotFoundError\n4. 예상한 실패만 처리하고 다른 버그를 숨기지 않기 위해서입니다.",
                "ValueError · ZeroDivisionError · FileNotFoundError · specific exception"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — 어떤 예외를 어디서 처리할지 구분하기",
                "1. ValueError\n2. ZeroDivisionError\n3. FileNotFoundError\n4. 다른 버그를 숨기지 않기 위해 구체적으로 처리합니다.",
                "각 exception type은 실패 원인을 좁혀 줍니다. 숫자 형식 변환 실패는 ValueError, 0 나누기는 ZeroDivisionError, 파일 부재는 FileNotFoundError입니다. 구체적인 except를 사용하면 코드가 어떤 failure path를 예상하는지 분명하고, 예상하지 못한 버그는 traceback으로 발견할 수 있습니다.",
                "세 예외를 각각 일부러 한 번 발생시킨 뒤 traceback의 마지막 줄에서 type과 message를 찾아 적어 보세요. 그 다음 해당 줄만 try에 넣어 처리해 보세요.",
                "try 범위를 지나치게 크게 잡는 것, 모든 예외를 하나의 broad except로 숨기는 것, 예외 메시지를 사용자에게 그대로 노출해도 항상 안전하다고 생각하는 것이 흔한 실수입니다."));

        out.add(Track2Content.page(lesson, l, "정리", "챕터 7 정리 — 성공 경로와 실패 경로를 함께 설계한다",
                "try는 실패할 수 있는 작업을 시도하고 except는 특정 exception type의 failure path를 처리합니다. ValueError, ZeroDivisionError, FileNotFoundError처럼 원인에 맞는 구체적인 예외를 처리하세요. 필요에 따라 else, finally, fallback, retry 같은 패턴도 사용할 수 있습니다.\n\n"
                        + "예외 처리의 목적은 오류를 숨기는 것이 아니라 예상 가능한 실패에서 사용자와 데이터를 안전하게 보호하는 것입니다. 이제 TRACK 02의 마지막 챕터에서는 조건문, 반복문, 함수, list·dict, 문자열, 파일, 예외 처리를 하나의 Python 자동화 미니 프로젝트로 결합합니다.",
                "좋은 exception handling은 ‘실패해도 모른 척’이 아니라 ‘이 실패를 예상했고 이렇게 안전하게 대응한다’는 코드입니다.",
                null, null, null, null,
                "exception · try · except · ValueError · FileNotFoundError · recovery · fallback · failure path"));
    }
}
