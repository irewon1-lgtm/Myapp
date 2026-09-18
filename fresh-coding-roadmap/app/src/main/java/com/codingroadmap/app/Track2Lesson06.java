package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson06 {
    private Track2Lesson06() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 6;
        final String l = "파일을 읽고 써서 데이터를 남기기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 파일·경로·폴더·현재 작업 위치",
                "파일(file)\n"
                        + "프로그램이 끝난 뒤에도 저장장치에 남겨 둘 수 있는 데이터 단위입니다. 메모.txt, settings.json, report.csv처럼 이름과 내용을 가집니다. 변수는 프로그램이 종료되면 사라질 수 있지만 파일에 저장하면 다음 실행에서도 다시 읽을 수 있습니다.\n\n"
                        + "경로(path)\n"
                        + "파일이나 폴더가 어디에 있는지 나타내는 주소입니다. 예: data/memo.txt. 코드에서 파일을 찾지 못하는 문제는 내용보다 path가 틀려서 생기는 경우도 많습니다. 절대 경로와 상대 경로가 있지만 여기서는 프로젝트 현재 위치를 기준으로 한 상대 경로를 주로 사용합니다.\n\n"
                        + "폴더(directory / folder)\n"
                        + "파일과 다른 폴더를 묶어 정리하는 공간입니다. 개발 문서에서는 directory라는 말을 자주 사용합니다. data 폴더 안의 memo.txt라면 path를 data/memo.txt처럼 표현할 수 있습니다.\n\n"
                        + "현재 작업 디렉터리(current working directory)\n"
                        + "상대 경로를 해석할 때 기준이 되는 현재 위치입니다. 같은 Python 파일이라도 어디에서 실행했는지에 따라 상대 경로가 가리키는 곳이 달라질 수 있습니다. 파일이 없다고 나올 때 먼저 실제 실행 위치와 path를 확인하는 이유입니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — open·모드 r·w·a",
                "open\n"
                        + "Python에서 파일을 열기 위해 사용하는 함수입니다. open(\"memo.txt\", \"r\", encoding=\"utf-8\")처럼 파일 이름, mode, encoding을 지정할 수 있습니다. 파일을 열면 읽거나 쓸 수 있는 file object를 얻습니다.\n\n"
                        + "읽기 모드(r / read mode)\n"
                        + "기존 파일 내용을 읽기 위한 mode입니다. 없는 파일을 r로 열면 FileNotFoundError가 날 수 있습니다. 원본 내용을 바꾸지 않고 조회할 때 사용합니다.\n\n"
                        + "쓰기 모드(w / write mode)\n"
                        + "파일에 새 내용을 쓰는 mode입니다. 파일이 없으면 만들 수 있지만, 이미 있는 파일을 w로 열면 기존 내용이 비워지고 새 내용으로 덮어써질 수 있습니다. 중요한 파일에서는 특히 주의해야 합니다.\n\n"
                        + "추가 모드(a / append mode)\n"
                        + "기존 파일 끝에 새 내용을 이어 붙이는 mode입니다. 일기나 로그처럼 기존 기록을 유지하면서 한 줄씩 추가할 때 유용합니다. w와 a의 차이를 이해하지 못하면 기존 데이터를 잃을 수 있습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — with·context manager·close·encoding",
                "with 문\n"
                        + "파일처럼 사용 후 정리가 필요한 자원을 안전하게 다루는 Python 문법입니다. with open(...) as file: 안에서 파일을 사용하면 블록을 벗어날 때 자동으로 닫아 줍니다. 초보 단계에서는 파일을 열 때 기본 패턴으로 익혀 두는 것이 좋습니다.\n\n"
                        + "context manager\n"
                        + "with와 함께 사용되어 자원의 시작과 정리를 관리하는 객체를 말합니다. 파일에서는 열기와 닫기를 안전하게 처리해 줍니다. 나중에 네트워크나 다른 자원을 다룰 때도 같은 개념을 만날 수 있습니다.\n\n"
                        + "close\n"
                        + "열어 둔 파일 사용을 끝내는 동작입니다. 직접 open만 사용했다면 close()를 호출해야 하지만 with를 사용하면 보통 자동 처리됩니다. 파일을 제대로 닫지 않으면 쓰기가 완전히 반영되지 않거나 자원이 불필요하게 남을 수 있습니다.\n\n"
                        + "인코딩(encoding)\n"
                        + "글자를 파일의 바이트로 저장하고 다시 글자로 읽는 규칙입니다. 한글 텍스트에서는 UTF-8을 명시하면 환경 차이로 인한 깨짐을 줄일 수 있습니다. encoding=\"utf-8\"을 파일 코드에 함께 적는 습관이 좋습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — read·write·줄바꿈·텍스트 파일",
                "read\n"
                        + "파일 전체 내용을 하나의 문자열로 읽는 메서드입니다. 작은 학습 파일에서는 편리하지만 매우 큰 파일을 한꺼번에 읽으면 메모리를 많이 사용할 수 있습니다. 지금은 동작 원리를 익히는 데 사용합니다.\n\n"
                        + "readline / 반복 읽기\n"
                        + "readline은 한 줄씩 읽는 메서드이고, file object 자체를 for line in file처럼 반복하면 줄을 하나씩 처리할 수 있습니다. 여러 줄 데이터를 처리할 때 유용합니다.\n\n"
                        + "write\n"
                        + "문자열을 파일에 쓰는 메서드입니다. write는 자동으로 줄바꿈을 붙이지 않으므로 여러 줄을 기록하려면 \n을 직접 넣어야 합니다. 숫자는 str 또는 f-string으로 문자열로 바꿔 써야 합니다.\n\n"
                        + "줄바꿈(newline, \\n)\n"
                        + "다음 줄로 이동한다는 특별한 문자입니다. 파일에서 한 줄을 읽으면 끝에 newline이 포함될 수 있어 strip으로 제거하는 경우가 많습니다. 저장할 때 record + \"\\n\"처럼 명시적으로 붙이는 패턴도 자주 사용합니다."));

        out.add(Track2Content.page(lesson, l, "시작", "변수는 실행 중 기억이고 파일은 실행 뒤에도 남는 기록이다",
                "프로그램에서 name이나 total 같은 변수는 실행 중에는 유용하지만 프로그램이 종료되면 그 상태를 잃을 수 있습니다. 다음 날 다시 실행해도 어제 기록이 필요하다면 데이터를 파일이나 데이터베이스 같은 영구 저장소에 남겨야 합니다. 이번 챕터에서는 가장 단순한 텍스트 파일을 이용해 ‘저장 → 종료 → 다시 읽기’의 흐름을 익힙니다.\n\n"
                        + "파일 처리는 코드만 맞는다고 끝나지 않습니다. 어떤 path인지, 읽기인지 쓰기인지, 기존 파일을 덮어써도 되는지, 글자 encoding은 무엇인지 같은 실행 환경 조건도 함께 생각해야 합니다.",
                "메모장 앱을 닫아도 글이 남는 이유는 화면 변수에만 두는 것이 아니라 저장장치에 기록하기 때문입니다. 파일은 프로그램 밖에 남는 작은 기록장입니다.",
                null, null, null, null,
                "file · persistence · path · storage"));

        out.add(Track2Content.page(lesson, l, "이론", "파일 mode는 읽기·덮어쓰기·추가의 의미를 결정한다",
                "open에서 지정하는 mode는 매우 중요합니다. r은 기존 내용을 읽고, w는 새 내용을 쓰면서 기존 내용을 덮어쓸 수 있으며, a는 기존 내용 뒤에 추가합니다. ‘파일에 쓰기’라는 같은 목적처럼 보여도 w와 a는 데이터 보존 측면에서 완전히 다릅니다.\n\n"
                        + "실제 작업에서는 파일을 열기 전에 ‘기존 내용이 보존되어야 하는가?’를 먼저 결정하세요. 메모 전체를 새 버전으로 교체한다면 w, 매일 기록을 한 줄씩 쌓는다면 a가 자연스럽습니다. 실수로 w를 사용해 기존 기록을 지우는 위험을 줄이려면 작은 테스트 파일로 먼저 연습하는 습관도 좋습니다.",
                "화이트보드 전체를 지우고 새로 쓰는 것이 w, 기존 글 아래에 한 줄 더 적는 것이 a입니다. r은 보기만 하는 상태입니다.",
                "with open(\"memo.txt\", \"w\", encoding=\"utf-8\") as file:\n    file.write(\"첫 메모\\n\")\n\nwith open(\"memo.txt\", \"a\", encoding=\"utf-8\") as file:\n    file.write(\"두 번째 메모\\n\")",
                null, null, null,
                "r · w · a · overwrite · append"));

        out.add(Track2Content.page(lesson, l, "이론", "with open 패턴은 파일을 사용한 뒤 자동으로 닫아 준다",
                "파일을 직접 open하면 사용이 끝난 뒤 close해야 합니다. with open(...) as file: 패턴을 쓰면 들여쓰기 블록 안에서 파일을 사용하고, 정상 종료든 예외 발생이든 블록을 벗어나면서 정리 작업을 해 줍니다. 그래서 Python 파일 처리에서 매우 흔한 기본 형태입니다.\n\n"
                        + "as file의 file은 열린 파일 객체를 가리키는 변수 이름입니다. f처럼 짧게 써도 되지만 초보 단계에서는 file처럼 의미가 드러나는 이름이 읽기 쉽습니다. 블록 밖에서는 이미 닫힌 파일을 다시 사용하지 말고 필요하면 새 with로 다시 여세요.",
                "문을 열고 방에 들어가 작업한 뒤 나올 때 자동으로 문을 닫아 주는 관리자가 with라고 생각하면 됩니다.",
                "with open(\"memo.txt\", \"r\", encoding=\"utf-8\") as file:\n    content = file.read()\n    print(content)\n\nprint(\"파일 읽기 끝\")",
                null, null, null,
                "with · context manager · close · file object · encoding"));

        out.add(Track2Content.page(lesson, l, "예제", "메모를 저장한 뒤 다시 읽어 같은 내용인지 확인하기",
                "아래 예제는 문자열 한 줄을 memo.txt에 저장하고 다시 r mode로 열어 내용을 읽습니다. 쓰기 단계와 읽기 단계를 분리하면 데이터가 실제 파일을 거쳐 다시 프로그램으로 들어오는 흐름을 볼 수 있습니다.\n\n"
                        + "write는 문자열을 요구하므로 숫자나 계산 결과를 저장할 때는 f-string이나 str로 변환합니다. 읽어 온 content는 다시 str입니다. 파일은 타입 정보를 자동으로 기억해 주는 것이 아니므로 숫자로 저장한 텍스트를 계산하려면 나중에 int 등으로 다시 변환해야 합니다.",
                "종이에 ‘12000’을 적었다가 다시 읽으면 눈에 보이는 것은 숫자 모양의 글자입니다. 텍스트 파일도 마찬가지라 읽어 오면 문자열입니다.",
                "message = \"오늘 Python 파일 저장을 연습했다.\"\n\nwith open(\"memo.txt\", \"w\", encoding=\"utf-8\") as file:\n    file.write(message + \"\\n\")\n\nwith open(\"memo.txt\", \"r\", encoding=\"utf-8\") as file:\n    saved = file.read()\n\nprint(saved)",
                null, null, null,
                "write · read · text file · str · persistence"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — 하루 메모를 a mode로 한 줄씩 추가하기",
                "기존 기록을 유지하면서 새로운 메모를 계속 추가하는 작은 로그 파일을 만듭니다. input으로 한 줄을 받고 notes.txt를 a mode로 열어 기록하세요. write가 자동 줄바꿈을 하지 않는다는 점을 확인하기 위해 newline을 직접 붙입니다.\n\n"
                        + "프로그램을 여러 번 실행해 파일에 줄이 누적되는지 확인하세요. 그 다음 일부러 w로 한 번 바꿨을 때 어떤 차이가 생기는지 테스트용 파일에서만 관찰하고 다시 a로 복구하세요.",
                "a mode는 노트의 마지막 줄 아래에 계속 적습니다. \n을 빼면 다음 실행의 메모가 같은 줄에 붙을 수 있습니다.",
                "note = input(\"오늘 메모: \")\n\n# notes.txt에 한 줄 추가하세요",
                "1. with open과 a mode로 notes.txt 열기\n2. note + ‘\\n’을 write하기\n3. 프로그램을 세 번 실행해 세 줄이 남는지 확인하기\n4. r mode로 전체 내용을 읽어 화면에 출력하기\n5. 테스트 복사본에서 w와 a의 차이를 직접 확인하기",
                null, null,
                "append mode · write · newline · persistence"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — 하루 메모를 a mode로 한 줄씩 추가하기",
                "예시 답안\n"
                        + "a mode로 열면 기존 notes.txt의 끝에 새 문자열이 추가됩니다. note 뒤에 \\n을 붙여 한 실행이 한 줄이 되게 만듭니다. 다시 r mode로 열고 read하면 지금까지 쌓인 모든 줄을 하나의 문자열로 확인할 수 있습니다.\n\n"
                        + "w mode는 기존 내용을 덮어쓸 수 있으므로 누적 기록 목적에는 맞지 않습니다. 실제 중요한 파일로 실험하지 말고 학습용 파일에서 차이를 확인하는 습관을 들이세요.",
                "note = input(\"오늘 메모: \")\n\nwith open(\"notes.txt\", \"a\", encoding=\"utf-8\") as file:\n    file.write(note + \"\\n\")\n\nwith open(\"notes.txt\", \"r\", encoding=\"utf-8\") as file:\n    content = file.read()\n\nprint(content)"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — 파일을 한 줄씩 읽고 빈 줄을 제외해 개수 세기",
                "파일 전체를 한꺼번에 읽는 대신 for line in file로 한 줄씩 처리해 봅니다. 각 line에는 줄바꿈이나 공백이 들어 있을 수 있으므로 strip한 결과가 빈 문자열인지 확인하고, 내용이 있는 줄만 count를 증가시킵니다.\n\n"
                        + "이 실습은 파일 읽기, 문자열 정리, 조건문, 반복문을 한 번에 결합합니다. 지금까지 배운 기능들이 실제 작업 흐름으로 연결되는 지점을 관찰하세요.",
                "파일을 종이 뭉치라고 보면 한 장씩 넘기며 빈 종이는 건너뛰고 내용 있는 종이만 세는 작업입니다.",
                "count = 0\n\nwith open(\"notes.txt\", \"r\", encoding=\"utf-8\") as file:\n    for line in file:\n        # 빈 줄이 아닌 경우만 count를 늘리세요\n        pass\n\nprint(count)",
                "1. clean = line.strip() 만들기\n2. clean이 빈 문자열이 아닐 때만 count += 1 하기\n3. 내용 있는 줄도 함께 출력하기\n4. notes.txt에 빈 줄을 하나 넣고 결과가 달라지는지 확인하기",
                null, null,
                "for line in file · strip · empty string · count"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — 파일을 한 줄씩 읽고 빈 줄을 제외해 개수 세기",
                "예시 답안\n"
                        + "for line in file은 파일의 각 줄을 차례로 문자열로 제공합니다. strip 후 아무 글자도 없으면 clean == \"\"이므로 조건에서 제외할 수 있습니다. 내용 있는 줄에서만 count를 증가시키면 실제 기록 수를 셀 수 있습니다.\n\n"
                        + "큰 파일에서는 한 줄씩 처리하는 방식이 전체 read보다 메모리 사용 측면에서 유리할 수 있습니다. 지금은 성능보다 ‘stream처럼 한 줄씩 처리할 수 있다’는 구조만 이해하면 충분합니다.",
                "count = 0\n\nwith open(\"notes.txt\", \"r\", encoding=\"utf-8\") as file:\n    for line in file:\n        clean = line.strip()\n        if clean != \"\":\n            count += 1\n            print(clean)\n\nprint(f\"내용 있는 줄: {count}\")"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — 파일 mode와 읽기·쓰기 흐름 구분하기",
                "파일 코드는 작은 철자보다 mode와 데이터 보존 의미를 이해하는 것이 중요합니다. 각 문제에서 ‘기존 파일을 어떻게 다루는가?’와 ‘프로그램으로 들어오는 값의 타입은 무엇인가?’를 생각해 보세요.",
                "r, w, a를 각각 ‘읽기’, ‘새로 쓰기/덮어쓰기 주의’, ‘뒤에 추가’라는 한 문장으로 연결해 두면 좋습니다.",
                null, null,
                "1. 기존 내용을 읽을 때 주로 사용하는 mode는 무엇인가요?\n2. 기존 내용을 유지한 채 끝에 새 기록을 붙일 때 사용하는 mode는 무엇인가요?\n3. with open을 사용하면 파일 사용이 끝난 뒤 어떤 정리 작업을 자동으로 처리해 주나요?\n4. 한글 텍스트 파일에서 명시해 두면 좋은 대표적인 encoding은 무엇인가요?",
                "1. r\n2. a\n3. 파일 close\n4. UTF-8",
                "r · a · with · close · UTF-8"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — 파일 mode와 읽기·쓰기 흐름 구분하기",
                "1. r\n2. a\n3. close\n4. UTF-8",
                "r은 읽기, a는 기존 파일 끝에 추가하는 mode입니다. with는 블록이 끝날 때 file을 자동으로 닫아 자원 정리를 돕습니다. UTF-8은 한글을 포함한 다양한 문자를 널리 표현하는 encoding이며 코드에 명시하면 환경 차이를 줄이는 데 도움이 됩니다.",
                "같은 test.txt를 w로 ‘A\\n’을 쓴 뒤 a로 ‘B\\n’을 추가하고, 마지막에 r로 읽어 결과를 확인해 보세요. 각 단계에서 파일 내용이 어떻게 변하는지 미리 적은 뒤 실행하세요.",
                "w와 a를 혼동해 기존 데이터를 지우는 것, newline을 빼서 여러 기록이 붙는 것, 상대 path가 실제 실행 위치와 다르다는 점을 놓치는 것이 흔한 실수입니다."));

        out.add(Track2Content.page(lesson, l, "정리", "챕터 6 정리 — 파일은 path·mode·encoding까지 함께 생각한다",
                "텍스트 파일을 사용하면 프로그램 실행이 끝난 뒤에도 데이터를 남길 수 있습니다. open에서 r·w·a mode의 차이를 이해하고, with를 사용해 파일을 안전하게 닫으며, 한글 텍스트는 UTF-8 encoding을 명시하는 습관을 들이세요. read와 write, 한 줄 반복을 이용하면 작은 기록 프로그램을 만들 수 있습니다.\n\n"
                        + "파일은 현실 환경과 연결되므로 ‘파일이 없다’, ‘권한이 없다’, ‘내용 형식이 깨졌다’ 같은 문제가 생길 수 있습니다. 다음 챕터에서는 이런 실행 중 오류를 프로그램이 예상하고 처리하는 예외 처리를 배웁니다.",
                "파일 처리는 ‘어디(path) → 어떻게 열기(mode) → 무엇을 읽고/쓰기 → 안전하게 닫기’ 순서로 생각하면 됩니다.",
                null, null, null, null,
                "file · path · open · r · w · a · with · encoding · read · write · newline"));
    }
}
