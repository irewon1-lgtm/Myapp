package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson08 {
    private Track2Lesson08() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 8;
        final String l = "Python 자동화 미니 프로젝트를 설계하고 완성하기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 자동화·스크립트·작업·파이프라인",
                "자동화(automation)\n"
                        + "사람이 반복해서 하던 규칙적인 작업을 프로그램이 대신 수행하게 만드는 것입니다. 파일의 여러 지출 기록을 읽어 합계를 계산하고 보고서를 만드는 일처럼 입력과 규칙이 비교적 명확한 작업이 좋은 자동화 대상입니다. 자동화는 ‘아무 판단이나 AI가 대신한다’가 아니라 반복 가능한 절차를 코드로 옮기는 것부터 시작합니다.\n\n"
                        + "스크립트(script)\n"
                        + "특정 작업을 수행하도록 작성한 비교적 작은 프로그램을 흔히 script라고 부릅니다. Python 파일 하나로 데이터 정리, 파일 이름 변경, 보고서 생성 등을 수행할 수 있습니다. 규모가 커지면 여러 함수와 파일로 나눌 수 있지만 기본 원리는 같습니다.\n\n"
                        + "작업(task)\n"
                        + "자동화하려는 한 단위의 일입니다. ‘지출 파일 읽기’, ‘한 줄 파싱하기’, ‘합계 계산하기’, ‘보고서 저장하기’처럼 큰 목표를 작은 task로 나누면 구현과 테스트가 쉬워집니다.\n\n"
                        + "파이프라인(pipeline)\n"
                        + "입력 데이터를 여러 처리 단계에 차례로 통과시켜 결과를 만드는 흐름입니다. 이번 프로젝트는 파일 읽기 → 문자열 정리 → 필드 분리 → 숫자 변환 → 합계 누적 → 보고서 저장 순서의 작은 pipeline입니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — 파싱·레코드·검증·건너뛰기",
                "파싱(parsing)\n"
                        + "문자열 같은 원본 데이터를 프로그램이 다루기 쉬운 구조로 해석하고 나누는 작업입니다. ‘식비,12000’을 쉼표로 split해 category와 amount로 나누는 것이 간단한 parsing입니다. 형식 약속을 알아야 올바르게 해석할 수 있습니다.\n\n"
                        + "레코드(record)\n"
                        + "관련된 여러 필드를 한 묶음으로 표현한 데이터 한 건입니다. 이번 프로젝트에서는 파일의 한 줄이 지출 record 한 건입니다. ‘식비,12000’에는 category와 amount 두 field가 있습니다.\n\n"
                        + "검증(validation)\n"
                        + "파싱한 데이터가 기대 규칙을 만족하는지 확인하는 작업입니다. 쉼표로 나눈 조각이 두 개인지, 금액이 숫자로 바뀔 수 있는지 확인하는 것이 예입니다. 잘못된 데이터를 계산에 넣기 전에 경계에서 확인해야 합니다.\n\n"
                        + "건너뛰기(skip)\n"
                        + "처리할 수 없는 record 하나 때문에 전체 자동화를 중단하지 않고 해당 record만 제외하는 전략입니다. 어떤 데이터를 왜 skip했는지 메시지나 로그로 남기면 나중에 원인을 확인할 수 있습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — 집계·합계·카테고리·보고서",
                "집계(aggregation)\n"
                        + "여러 record를 합치거나 세어서 의미 있는 요약값을 만드는 작업입니다. 전체 지출 합계, 카테고리별 합계, 건수 계산 등이 aggregation입니다. 반복문과 accumulator, dict를 함께 사용하는 경우가 많습니다.\n\n"
                        + "총합(grand total)\n"
                        + "모든 항목을 합친 전체 결과입니다. 이번 프로젝트에서는 각 지출 amount를 total에 누적해 만듭니다. category별 합계와 구분하기 위해 grand total이라는 말을 쓰기도 합니다.\n\n"
                        + "카테고리별 집계(grouping by category)\n"
                        + "같은 category에 속한 record의 값을 함께 합치는 작업입니다. totals dict에서 category를 key로, 누적 금액을 value로 두면 식비·교통·쇼핑별 합계를 만들 수 있습니다.\n\n"
                        + "보고서(report)\n"
                        + "처리한 결과를 사람이 읽을 수 있는 형태로 정리한 출력물입니다. 화면 출력일 수도 있고 summary.txt 같은 파일일 수도 있습니다. 자동화는 계산만 끝내는 것이 아니라 결과를 확인 가능한 형태로 남겨야 실제로 쓸 수 있습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — 로그·입력 파일·출력 파일·재실행 가능성",
                "로그(log)\n"
                        + "프로그램이 실행되면서 어떤 일이 있었는지 남기는 기록입니다. ‘3번째 줄 형식 오류로 건너뜀’처럼 처리 과정을 남기면 자동화가 왜 특정 결과를 만들었는지 조사하기 쉽습니다. 학습 단계에서는 print로 간단한 log를 대신할 수 있습니다.\n\n"
                        + "입력 파일(input file)\n"
                        + "자동화가 읽어서 처리하는 원본 파일입니다. 이번 프로젝트의 expenses.txt가 input file입니다. 원본을 보존해야 한다면 처리 중 실수로 w mode로 열지 않도록 주의해야 합니다.\n\n"
                        + "출력 파일(output file)\n"
                        + "자동화 결과를 저장하는 파일입니다. summary.txt처럼 input과 이름을 분리하면 원본 손상을 줄이고 결과를 비교하기 쉽습니다.\n\n"
                        + "재실행 가능성(re-runnable / repeatable)\n"
                        + "같은 입력으로 프로그램을 다시 실행했을 때 예측 가능한 결과를 만들 수 있는 성질입니다. 자동화는 한 번 우연히 성공하는 것보다 반복 실행해도 결과가 안정적이어야 합니다. output 파일을 매번 새로 만드는지 뒤에 추가하는지도 의도적으로 정해야 합니다."));

        out.add(Track2Content.page(lesson, l, "시작", "자동화는 큰 코드를 한 번에 쓰지 않고 작업을 작은 단계로 연결한다",
                "이번 챕터에서는 TRACK 02에서 배운 기능을 한 프로그램에 모읍니다. expenses.txt 파일에 ‘카테고리,금액’ 형식의 지출 기록이 여러 줄 있다고 가정합니다. 프로그램은 파일을 한 줄씩 읽고, 문자열을 정리하고, 데이터를 분리하고, 숫자로 변환하고, 전체 및 카테고리별 합계를 계산한 뒤 summary.txt에 보고서를 저장합니다.\n\n"
                        + "처음부터 전체 코드를 외우려고 하지 마세요. 자동화는 입력·처리·출력을 다시 쪼개고, 각 단계를 함수 하나씩 구현하고, 작은 테스트를 통과시킨 뒤 연결하는 방식으로 만드는 것이 안전합니다.",
                "공장에서 원재료가 여러 공정을 지나 완제품이 되는 모습을 생각하세요. 파일 한 줄도 읽기 → 정리 → 검사 → 계산 → 보고서라는 여러 작은 공정을 차례로 통과합니다.",
                null, null, null, null,
                "automation · script · task · pipeline · input-process-output"));

        out.add(Track2Content.page(lesson, l, "이론", "먼저 데이터 형식과 실패 규칙을 계약처럼 정한다",
                "코드를 쓰기 전에 input file의 형식을 정합니다. 이번 프로젝트의 정상 record는 한 줄에 ‘category,amount’이고 amount는 정수라고 약속합니다. 예: 식비,12000. 빈 줄은 무시하고, 쉼표로 나눈 조각이 두 개가 아니거나 amount가 정수로 바뀌지 않으면 해당 줄을 skip하면서 안내 메시지를 남깁니다.\n\n"
                        + "이렇게 형식과 failure rule을 먼저 정하면 구현 중 판단이 흔들리지 않습니다. ‘잘못된 한 줄 때문에 전체를 끝낼 것인가, 그 줄만 건너뛸 것인가?’ 같은 정책도 요구사항의 일부입니다. 실제 자동화에서는 이런 작은 규칙이 데이터 품질과 결과 신뢰도를 결정합니다.",
                "시험 채점 기준표를 먼저 만드는 것과 같습니다. 어떤 답안이 정상이고 어떤 답안은 제외할지 코드보다 먼저 정합니다.",
                "정상 예시:\n식비,12000\n교통,1500\n식비,8000\n\n비정상 예시:\n잘못된줄\n쇼핑,abc",
                null, null, null,
                "data format · record · validation · policy · skip"));

        out.add(Track2Content.page(lesson, l, "이론", "함수로 읽기·파싱·집계·보고서 생성을 분리한다",
                "하나의 긴 함수에 모든 일을 넣으면 오류가 났을 때 어느 단계가 문제인지 찾기 어렵습니다. read_records는 파일 줄을 읽고, parse_record는 한 줄을 category와 amount로 해석하고, aggregate는 여러 record를 합계 내며, save_report는 결과를 파일에 씁니다. 각 함수가 한 책임을 가지면 작은 단위로 테스트할 수 있습니다.\n\n"
                        + "모든 함수를 꼭 완벽하게 분리해야 한다는 뜻은 아닙니다. 중요한 것은 ‘입력 경계’, ‘변환’, ‘계산’, ‘출력’을 구분해 생각하는 습관입니다. 프로그램이 커질수록 이 구조가 수정 안전성을 높여 줍니다.",
                "한 사람이 주문 접수·요리·계산·청소를 모두 하는 것보다 역할을 나누면 어디서 문제가 생겼는지 찾기 쉬운 것과 비슷합니다.",
                "def parse_record(line):\n    clean = line.strip()\n    parts = clean.split(\",\")\n    if len(parts) != 2:\n        return None\n\n    category = parts[0].strip()\n    try:\n        amount = int(parts[1].strip())\n    except ValueError:\n        return None\n\n    return {\"category\": category, \"amount\": amount}",
                null, null, null,
                "function · single responsibility · parse · return · None"));

        out.add(Track2Content.page(lesson, l, "예제", "지출 파일을 읽어 전체 합계와 카테고리별 합계를 만들기",
                "아래 코드는 한 줄씩 파일을 읽고 parse_record를 호출합니다. None이 반환되면 처리할 수 없는 줄이므로 skip하고, 정상 dict라면 total과 category_totals에 값을 누적합니다. dict의 get(category, 0)을 사용하면 처음 보는 category도 0부터 합계를 시작할 수 있습니다.\n\n"
                        + "지금까지 배운 if, for, 함수, dict, 문자열, 파일, 예외 처리가 한 흐름 안에서 연결됩니다. 각각은 작은 문법이지만 조합하면 실제 업무에 가까운 자동화가 됩니다.",
                "각 줄이 검문소 parse_record를 통과한 뒤 정상 데이터만 계산 창구로 들어간다고 생각하세요.",
                "def parse_record(line):\n    parts = line.strip().split(\",\")\n    if len(parts) != 2:\n        return None\n    try:\n        return {\"category\": parts[0].strip(), \"amount\": int(parts[1].strip())}\n    except ValueError:\n        return None\n\ntotal = 0\ncategory_totals = {}\n\nwith open(\"expenses.txt\", \"r\", encoding=\"utf-8\") as file:\n    for line in file:\n        record = parse_record(line)\n        if record is None:\n            print(f\"건너뜀: {line.strip()}\")\n            continue\n        total += record[\"amount\"]\n        category = record[\"category\"]\n        category_totals[category] = category_totals.get(category, 0) + record[\"amount\"]\n\nprint(total)\nprint(category_totals)",
                null, null, null,
                "parse_record · None · continue · aggregation · dict.get"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — 카테고리별 보고서를 summary.txt로 저장하기",
                "예제에서 만든 total과 category_totals를 사람이 읽을 수 있는 report로 저장합니다. output file은 input file과 분리하고 w mode로 새 보고서를 만듭니다. dict.items()로 category와 amount를 반복하면서 한 줄씩 write하세요.\n\n"
                        + "보고서 순서는 이번 단계에서는 중요하지 않습니다. 먼저 정확한 값이 저장되는지 확인하고, 파일을 다시 r mode로 열어 실제 내용이 기대 결과와 같은지 검증하세요.",
                "계산 결과를 화면에서 보는 데서 끝내지 않고 사람이 나중에 열어 볼 수 있는 결과 파일로 전달하는 단계입니다.",
                "total = 21500\ncategory_totals = {\"식비\": 20000, \"교통\": 1500}\n\n# summary.txt를 작성하세요",
                "1. summary.txt를 w mode와 UTF-8로 열기\n2. 첫 줄에 ‘전체 합계: 21500원’ 저장하기\n3. category_totals.items()를 반복해 ‘식비: 20000원’ 같은 줄 저장하기\n4. 각 write 끝에 newline 넣기\n5. 저장 뒤 r mode로 다시 읽어 화면에 출력해 검증하기",
                null, null,
                "report · output file · items · write · verify"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — 카테고리별 보고서를 summary.txt로 저장하기",
                "예시 답안\n"
                        + "output file은 w mode로 매 실행 새 보고서를 만들도록 했습니다. 전체 합계를 먼저 쓰고 dict의 key-value pair를 반복하며 카테고리별 줄을 추가합니다. write는 자동 줄바꿈을 하지 않으므로 \\n을 직접 붙입니다.\n\n"
                        + "저장 뒤 다시 읽어 보는 것은 간단하지만 중요한 검증입니다. 코드가 오류 없이 끝났다는 사실만으로 파일 내용이 정확하다고 단정하면 안 됩니다.",
                "total = 21500\ncategory_totals = {\"식비\": 20000, \"교통\": 1500}\n\nwith open(\"summary.txt\", \"w\", encoding=\"utf-8\") as file:\n    file.write(f\"전체 합계: {total}원\\n\")\n    for category, amount in category_totals.items():\n        file.write(f\"{category}: {amount}원\\n\")\n\nwith open(\"summary.txt\", \"r\", encoding=\"utf-8\") as file:\n    print(file.read())"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — 잘못된 줄을 기록하고 정상 줄은 계속 처리하기",
                "자동화는 입력이 항상 완벽하다고 가정하면 쉽게 멈춥니다. 이번에는 invalid_count 변수를 만들어 skip한 줄 수를 세고, 어떤 줄이 잘못됐는지도 화면에 남깁니다. 정상 record 수 valid_count도 세어 마지막 report에 포함합니다.\n\n"
                        + "잘못된 데이터가 있어도 조용히 무시하지 말고 몇 건을 제외했는지 보이게 해야 결과를 신뢰할 수 있습니다. 입력 100건 중 40건이 skip됐는데 총합만 보여 주면 사용자는 결과가 불완전하다는 사실을 모를 수 있습니다.",
                "자동화 결과에는 ‘얼마가 나왔는가’뿐 아니라 ‘몇 건을 정상 처리했고 몇 건을 버렸는가’도 중요한 품질 정보입니다.",
                "valid_count = 0\ninvalid_count = 0\n\n# 파일 반복 안에서 정상/비정상 건수를 세어 보세요",
                "1. parse_record가 None이면 invalid_count += 1 하기\n2. 정상 record면 valid_count += 1 하기\n3. skip할 때 원본 줄도 ‘건너뜀: ...’으로 출력하기\n4. summary.txt 마지막에 정상 건수와 제외 건수 저장하기\n5. 일부러 잘못된 줄 두 개를 넣어 count가 맞는지 확인하기",
                null, null,
                "valid record · invalid record · count · log · data quality"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — 잘못된 줄을 기록하고 정상 줄은 계속 처리하기",
                "예시 답안\n"
                        + "record가 None인 branch에서 invalid_count를 증가시키고 continue로 다음 줄로 넘어갑니다. 정상 branch에서는 valid_count를 증가시킨 뒤 합계 계산을 계속합니다. 이렇게 하면 한 줄의 문제 때문에 전체 파일 처리가 중단되지 않으면서도 제외 규모를 확인할 수 있습니다.\n\n"
                        + "실제 시스템에서는 print 대신 log 파일이나 logging 도구를 사용할 수 있지만, 지금 단계에서는 실패 사실과 원본 줄을 확인할 수 있게 남기는 구조를 이해하면 충분합니다.",
                "valid_count = 0\ninvalid_count = 0\n\nwith open(\"expenses.txt\", \"r\", encoding=\"utf-8\") as file:\n    for line in file:\n        record = parse_record(line)\n        if record is None:\n            invalid_count += 1\n            print(f\"건너뜀: {line.strip()}\")\n            continue\n        valid_count += 1\n        # 여기서 정상 record를 집계합니다.\n\nprint(f\"정상 {valid_count}건 / 제외 {invalid_count}건\")"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — 자동화 파이프라인을 단계별로 설명하기",
                "프로젝트 마지막 확인은 문법 이름을 외웠는지가 아니라 프로그램 전체 흐름을 자기 말로 설명할 수 있는지 봅니다. expenses.txt 한 줄이 들어와 summary.txt 결과가 되기까지 어떤 단계를 통과하는지 순서대로 떠올려 보세요.",
                "‘읽기 → 정리 → 나누기 → 검증/변환 → 집계 → 보고서’ 여섯 단어로 먼저 말한 뒤 각 단계에 사용한 Python 기능을 붙여 보세요.",
                null, null,
                "1. ‘식비,12000’ 문자열을 category와 amount로 나누는 작업을 무엇이라고 부를 수 있나요?\n2. 잘못된 한 줄만 제외하고 다음 줄 처리를 계속할 때 사용할 수 있는 반복문 키워드는 무엇인가요?\n3. 카테고리 이름을 key, 누적 금액을 value로 저장하기 좋은 자료구조는 무엇인가요?\n4. 자동화 결과뿐 아니라 제외된 데이터 건수도 함께 남기는 이유는 무엇인가요?",
                "1. parsing\n2. continue\n3. dict\n4. 결과의 데이터 품질과 처리 상태를 확인하기 위해서입니다.",
                "parsing · continue · dict · aggregation · data quality"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — 자동화 파이프라인을 단계별로 설명하기",
                "1. parsing\n2. continue\n3. dict\n4. 처리 품질과 신뢰도를 확인하기 위해서입니다.",
                "문자열 record를 구조화된 field로 해석하는 과정이 parsing입니다. 처리할 수 없는 한 iteration만 건너뛰고 반복을 계속하려면 continue를 사용할 수 있습니다. category별 누적은 key-value 구조인 dict가 자연스럽습니다. 자동화는 숫자 결과만 맞아도 충분하지 않고 입력 중 몇 건을 정상 처리했는지 알아야 결과의 범위와 신뢰도를 판단할 수 있습니다.",
                "expenses.txt에 정상 3줄과 비정상 2줄을 직접 만들고 프로그램 실행 전에 예상 total, valid_count, invalid_count를 적어 보세요. 실행 결과가 모두 일치하는지 확인하면 작은 end-to-end test가 됩니다.",
                "전체 코드를 한 번에 작성해 어디서 틀렸는지 모르게 되는 것, 비정상 데이터를 조용히 버리는 것, input file을 실수로 w mode로 여는 것이 특히 주의할 실수입니다."));

        out.add(Track2Content.page(lesson, l, "완료", "TRACK 02 완료 — 반복 작업을 자동화하는 작은 Python 프로그램을 만들 수 있다",
                "이제 조건문으로 실행 경로를 나누고, for·while로 반복하고, 함수를 만들어 로직을 재사용하고, list·dict로 여러 데이터를 다루며, 문자열을 정리·분리하고, 파일을 읽고 쓰고, 예상 가능한 예외를 처리할 수 있습니다. 마지막 프로젝트에서는 이 기능들을 연결해 입력 파일을 읽고 검증하고 집계해 출력 파일을 만드는 자동화 흐름까지 만들었습니다.\n\n"
                        + "다음 단계에서 중요한 것은 더 많은 문법을 외우는 것이 아니라 문제를 작은 단계로 나누고 각 단계의 입력과 기대 결과를 테스트하는 습관입니다. TRACK 03에서는 바로 그 문제 해결 사고법, 의사코드, 자료구조와 테스트 케이스를 더 체계적으로 다룹니다.",
                "TRACK 02의 핵심은 ‘문법을 각각 아는 상태’에서 ‘여러 문법을 연결해 실제 작업 하나를 끝내는 상태’로 넘어온 것입니다. 완성된 예제를 그대로 외우지 말고 입력 형식이나 계산 규칙을 바꿔 다시 만들어 보세요.",
                null, null, null, null,
                "automation · condition · loop · function · list · dict · string · file · exception · pipeline · report"));
    }
}
