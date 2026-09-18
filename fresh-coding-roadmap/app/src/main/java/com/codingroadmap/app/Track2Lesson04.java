package com.codingroadmap.app;

import java.util.List;

final class Track2Lesson04 {
    private Track2Lesson04() {}

    static void append(List<Track1Content.Page> out) {
        final int lesson = 4;
        final String l = "리스트와 딕셔너리로 여러 데이터를 다루기";

        out.add(Track2Content.vocab(lesson, l, "용어집 1/4 — 컬렉션·리스트·요소·인덱스",
                "컬렉션(collection)\n"
                        + "여러 값을 하나의 구조에 묶어 관리하는 데이터를 넓게 부르는 말입니다. 지금까지 변수 하나에 값 하나를 저장했다면, collection을 사용하면 여러 상품명이나 점수를 한 변수로 다룰 수 있습니다. Python의 list와 dict가 대표적인 collection입니다.\n\n"
                        + "리스트(list)\n"
                        + "여러 값을 순서대로 저장하는 Python 자료구조입니다. 대괄호 [ ]로 만들며 값 사이를 쉼표로 구분합니다. 예: scores = [90, 80, 100]. 순서가 유지되므로 ‘첫 번째, 두 번째’처럼 위치로 접근할 수 있습니다.\n\n"
                        + "요소(element / item)\n"
                        + "리스트 안에 들어 있는 각각의 값을 말합니다. [\"사과\", \"바나나\"]에는 두 element가 있습니다. 리스트 전체와 그 안의 element를 구분하면 반복문이나 수정 코드를 읽기 쉬워집니다.\n\n"
                        + "인덱스(index)\n"
                        + "리스트에서 element의 위치를 나타내는 번호입니다. Python은 첫 번째 위치를 0부터 셉니다. items[0]은 첫 번째 값, items[1]은 두 번째 값입니다. 존재하지 않는 위치를 읽으면 IndexError가 발생할 수 있습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 2/4 — append·len·슬라이스·멤버십",
                "append\n"
                        + "리스트의 맨 뒤에 새 element 하나를 추가하는 메서드입니다. items.append(\"우유\")처럼 사용합니다. 원래 list 자체가 수정됩니다. 사용자 입력을 차례로 모으거나 처리 결과를 저장할 때 자주 사용합니다.\n\n"
                        + "len\n"
                        + "리스트나 문자열처럼 길이가 있는 값의 개수를 알려 주는 Python 함수입니다. len([10, 20, 30])은 3입니다. 마지막 유효 index는 길이보다 1 작다는 점도 함께 기억하면 좋습니다.\n\n"
                        + "슬라이스(slice)\n"
                        + "리스트의 일부 구간을 잘라 새 값으로 얻는 문법입니다. items[1:3]은 index 1부터 3 직전까지 가져옵니다. range와 마찬가지로 끝 위치가 포함되지 않는 규칙이 있습니다.\n\n"
                        + "멤버십 검사(membership test)\n"
                        + "어떤 값이 collection 안에 있는지 in으로 확인하는 것입니다. \"사과\" in items는 결과가 True 또는 False입니다. 조건문과 함께 사용하면 특정 항목이 있는지 확인하기 좋습니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 3/4 — 딕셔너리·키·값·키-값 쌍",
                "딕셔너리(dictionary / dict)\n"
                        + "값을 순번이 아니라 이름표인 key와 연결해 저장하는 Python 자료구조입니다. 중괄호 { } 안에 key: value 형태로 적습니다. 예: user = {\"name\": \"민수\", \"age\": 20}. 서로 다른 속성을 한 묶음으로 표현할 때 유용합니다.\n\n"
                        + "키(key)\n"
                        + "dict에서 값을 찾기 위한 이름표입니다. user[\"name\"]처럼 key를 사용해 value를 꺼냅니다. 한 dict 안에서 같은 key는 하나의 현재 값만 가집니다.\n\n"
                        + "값(value)\n"
                        + "key에 연결되어 저장된 실제 데이터입니다. 같은 dict 안에서도 문자열, 숫자, bool 등 서로 다른 타입을 value로 넣을 수 있습니다.\n\n"
                        + "키-값 쌍(key-value pair)\n"
                        + "key 하나와 그 key에 연결된 value 하나를 한 묶음으로 부르는 말입니다. dict는 여러 key-value pair로 구성됩니다. JSON, 데이터베이스, API 응답을 배울 때도 이 구조와 비슷한 형태를 계속 만나게 됩니다."));

        out.add(Track2Content.vocab(lesson, l, "용어집 4/4 — get·items·수정·중첩 구조",
                "get 메서드\n"
                        + "dict에서 key를 안전하게 조회할 때 사용하는 메서드입니다. user.get(\"name\")처럼 사용합니다. 없는 key를 대괄호로 바로 읽으면 KeyError가 날 수 있지만 get은 기본적으로 None을 돌려주며, get(\"name\", \"없음\")처럼 기본값도 지정할 수 있습니다.\n\n"
                        + "items 메서드\n"
                        + "dict의 key와 value 쌍을 반복할 수 있게 제공합니다. for key, value in user.items():처럼 사용하면 각 key-value pair를 하나씩 처리할 수 있습니다.\n\n"
                        + "수정(mutation)\n"
                        + "이미 존재하는 list나 dict의 내용을 바꾸는 것입니다. items.append(...)나 user[\"age\"] = 21처럼 collection 자체를 변경하는 동작이 mutation입니다. 코드 여러 곳이 같은 collection을 사용할 때는 변경 시점을 주의해야 합니다.\n\n"
                        + "중첩(nested structure)\n"
                        + "collection 안에 다른 collection을 넣은 구조입니다. 예를 들어 여러 사용자를 list로 묶고 각 사용자를 dict로 표현할 수 있습니다. 처음에는 한 단계 구조를 정확히 읽은 뒤 나중에 중첩을 확장하는 편이 좋습니다."));

        out.add(Track2Content.page(lesson, l, "시작", "값이 많아지면 변수 하나씩보다 묶어서 다루는 편이 낫다",
                "상품이 세 개라면 item1, item2, item3 같은 변수를 만들 수도 있습니다. 하지만 상품이 100개라면 변수 100개를 만들고 같은 코드를 반복해야 합니다. list는 같은 종류의 여러 값을 순서대로 묶어 반복 처리하게 해 줍니다. dict는 ‘이름, 나이, 도시’처럼 서로 다른 의미의 값을 key라는 이름표로 묶어 줍니다.\n\n"
                        + "자료구조를 고를 때는 ‘순서가 중요한 여러 값인가?’와 ‘각 값에 이름표가 필요한가?’를 먼저 생각하세요. 순서 중심이면 list, 속성 이름 중심이면 dict가 자연스러운 경우가 많습니다.",
                "list는 번호가 붙은 서랍장이고 dict는 서랍마다 ‘이름’, ‘나이’, ‘주소’ 같은 라벨이 붙은 서랍장이라고 생각하면 쉽습니다.",
                null, null, null, null,
                "collection · list · dict · structure"));

        out.add(Track2Content.page(lesson, l, "이론", "list는 0부터 시작하는 index와 반복문으로 다룬다",
                "리스트를 만들면 대괄호와 index로 특정 element를 읽거나 바꿀 수 있습니다. Python index는 0부터 시작하므로 fruits[0]이 첫 번째 값입니다. len(fruits)가 3이면 유효한 index는 0, 1, 2입니다. fruits[3]은 네 번째 값을 요구하므로 존재하지 않아 오류가 납니다.\n\n"
                        + "리스트의 강점은 for와 연결할 때 드러납니다. for fruit in fruits:라고 쓰면 index를 직접 계산하지 않아도 각 값을 하나씩 처리할 수 있습니다. 반복 중 현재 값만 필요하다면 이런 방식이 읽기 쉽습니다.",
                "세 칸짜리 사물함 번호가 0, 1, 2라고 생각하세요. 칸은 세 개지만 3번 칸은 없습니다.",
                "fruits = [\"사과\", \"바나나\", \"포도\"]\nprint(fruits[0])\nprint(len(fruits))\n\nfruits.append(\"딸기\")\nfor fruit in fruits:\n    print(fruit)",
                null, null, null,
                "list · index · element · append · len · for"));

        out.add(Track2Content.page(lesson, l, "이론", "dict는 key를 사용해 의미 있는 값을 바로 찾는다",
                "dict에서는 위치 번호보다 key로 값을 찾습니다. product[\"price\"]를 보면 몇 번째 칸인지 몰라도 가격을 읽는 코드라는 뜻이 바로 보입니다. key를 새로 대입하면 항목이 추가되고 기존 key에 대입하면 value가 수정됩니다.\n\n"
                        + "외부 데이터는 항상 기대한 key를 가진다고 보장할 수 없으므로 get도 알아둘 필요가 있습니다. product.get(\"discount\", 0)은 discount key가 있으면 그 값을, 없으면 0을 돌려줍니다. 이런 기본값은 오류 없이 안전하게 처리하는 데 유용합니다.",
                "dict는 전화번호부와 비슷합니다. 몇 번째 줄인지 몰라도 사람 이름이라는 key를 알면 연결된 전화번호 value를 찾을 수 있습니다.",
                "product = {\n    \"name\": \"사과\",\n    \"price\": 1500,\n    \"stock\": 10\n}\n\nprint(product[\"name\"])\nprint(product.get(\"discount\", 0))\nproduct[\"stock\"] = 9",
                null, null, null,
                "dict · key · value · get · mutation"));

        out.add(Track2Content.page(lesson, l, "예제", "여러 주문 금액을 list에 모아 합계 계산하기",
                "아래 예제는 주문 금액 여러 개를 list에 저장하고 for로 하나씩 꺼내 total에 누적합니다. 숫자 개수가 바뀌어도 반복문 자체는 그대로 사용할 수 있습니다. 새 주문을 추가하려면 list에 값 하나만 append하면 됩니다.\n\n"
                        + "실제 Python에는 sum이라는 합계 함수도 있지만, 지금은 반복문과 list가 어떻게 연결되는지 보기 위해 직접 누적합니다. 이미 배운 개념을 합쳐 작은 데이터 처리를 만드는 것이 이번 챕터의 핵심입니다.",
                "돈 봉투 여러 개를 한 바구니 list에 넣고, 하나씩 꺼내 total 저금통에 더하는 모습입니다.",
                "orders = [12000, 8500, 23000]\norders.append(5000)\n\ntotal = 0\nfor amount in orders:\n    total += amount\n\nprint(f\"주문 건수: {len(orders)}\")\nprint(f\"총액: {total}원\")",
                null, null, null,
                "list · append · for · accumulator · len"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 A — 장보기 list를 추가·조회·반복하기",
                "문자열 여러 개를 list로 만들고 element를 읽고 추가하고 반복합니다. index를 사용하는 방식과 for로 값을 직접 꺼내는 방식의 차이를 확인하세요. 없는 index를 일부러 읽어 본 뒤 오류 이름도 관찰해 봅니다.\n\n"
                        + "실제 코드를 고칠 때는 list의 길이가 바뀔 수 있다는 점 때문에 고정 index를 많이 사용하는 것보다 반복문이 더 안전한 경우가 많습니다.",
                "리스트 길이가 3이면 마지막 index는 2입니다. len(items)와 마지막 index의 관계를 직접 확인해 보세요.",
                "shopping = [\"우유\", \"계란\", \"사과\"]",
                "1. 첫 번째와 세 번째 항목을 index로 출력하기\n2. append로 ‘빵’을 추가하고 len 출력하기\n3. for를 사용해 모든 항목 앞에 ‘살 것: ’을 붙여 출력하기\n4. shopping[10]을 실행해 어떤 오류가 나는지 확인한 뒤 원래대로 복구하기",
                null, null,
                "index · append · len · IndexError · for"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 A — 장보기 list를 추가·조회·반복하기",
                "예시 답안\n"
                        + "shopping[0]은 우유, shopping[2]는 사과입니다. 빵을 append하면 네 개가 되어 len은 4입니다. for에서는 각 element가 item에 차례로 들어가므로 index를 직접 계산하지 않아도 됩니다.\n\n"
                        + "shopping[10]은 실제 list 범위를 벗어나므로 IndexError가 납니다. 오류가 나면 len(shopping)으로 현재 길이를 확인하고 사용 가능한 index 범위를 계산하는 습관이 좋습니다.",
                "shopping = [\"우유\", \"계란\", \"사과\"]\nprint(shopping[0])\nprint(shopping[2])\n\nshopping.append(\"빵\")\nprint(len(shopping))\n\nfor item in shopping:\n    print(f\"살 것: {item}\")"));

        out.add(Track2Content.page(lesson, l, "실습", "실습 B — 상품 dict를 조회하고 수정하기",
                "한 상품의 여러 속성을 dict로 표현해 봅니다. name, price, stock이라는 key로 값을 저장한 뒤 가격 계산과 재고 수정에 사용합니다. 없는 key를 직접 읽는 방식과 get을 사용하는 방식도 비교합니다.\n\n"
                        + "dict를 사용할 때는 key 이름이 사실상 데이터의 작은 약속이 됩니다. \"price\"와 \"Price\"는 서로 다른 key이므로 철자와 대소문자를 일관되게 유지하세요.",
                "dict의 key는 서랍 라벨입니다. 라벨 철자를 다르게 쓰면 Python은 전혀 다른 서랍을 찾는다고 생각합니다.",
                "product = {\"name\": \"노트\", \"price\": 3000, \"stock\": 5}",
                "1. name과 price를 key로 읽어 출력하기\n2. stock에서 1을 빼 판매 후 재고로 수정하기\n3. category라는 새 key를 추가하기\n4. get(\"discount\", 0)으로 없는 값을 안전하게 조회하기\n5. for key, value in product.items()로 전체 속성 출력하기",
                null, null,
                "dict · key · value · get · items · mutation"));

        out.add(Track2Content.practiceAnswer(lesson, l, "실습 B — 상품 dict를 조회하고 수정하기",
                "예시 답안\n"
                        + "대괄호에 key를 넣으면 해당 value를 읽거나 수정할 수 있습니다. 기존 stock에 1을 뺀 값을 다시 같은 key에 대입하면 재고가 4가 됩니다. 존재하지 않던 category에 값을 대입하면 새 key-value pair가 추가됩니다.\n\n"
                        + "discount가 없어도 get에 기본값 0을 주면 오류 대신 0을 얻습니다. items()를 반복하면 각 key와 value를 동시에 받을 수 있어 데이터 확인이나 출력에 편리합니다.",
                "product = {\"name\": \"노트\", \"price\": 3000, \"stock\": 5}\nprint(product[\"name\"])\nprint(product[\"price\"])\n\nproduct[\"stock\"] -= 1\nproduct[\"category\"] = \"문구\"\nprint(product.get(\"discount\", 0))\n\nfor key, value in product.items():\n    print(key, value)"));

        out.add(Track2Content.page(lesson, l, "확인", "확인 문제 — list와 dict 중 알맞은 구조 고르기",
                "자료구조는 문법보다 ‘어떤 모양의 데이터를 표현하려는가’를 판단하는 것이 중요합니다. 순서대로 같은 종류의 값을 모으는지, 이름이 붙은 여러 속성을 한 묶음으로 표현하는지 생각해 보세요.",
                "‘첫 번째, 두 번째’가 중요하면 list를, ‘이름, 가격, 재고’처럼 필드 이름으로 찾고 싶으면 dict를 먼저 떠올리세요.",
                null, null,
                "1. colors = [\"red\", \"blue\"]에서 첫 번째 element를 읽는 코드는 무엇인가요?\n2. list에 새 element 하나를 맨 뒤에 추가하는 메서드는 무엇인가요?\n3. user = {\"name\": \"민수\"}에서 이름을 읽을 때 사용하는 key는 무엇인가요?\n4. 없는 dict key를 기본값과 함께 안전하게 읽을 때 유용한 메서드는 무엇인가요?",
                "1. colors[0]\n2. append\n3. \"name\"\n4. get",
                "list · index · append · dict · key · get"));

        out.add(Track2Content.questionAnswer(lesson, l, "확인 문제 — list와 dict 중 알맞은 구조 고르기",
                "1. colors[0]\n2. append\n3. \"name\"\n4. get",
                "list index는 0부터 시작하므로 첫 element는 [0]입니다. append는 list 맨 뒤에 값을 추가합니다. dict에서는 순번보다 key를 사용해 value를 찾고, get은 key가 없을 때 기본값을 지정할 수 있어 안전한 조회에 유용합니다.",
                "좋아하는 음식 세 개는 list로, 한 사람의 이름·나이·도시는 dict로 직접 표현해 보세요. 그리고 list는 for로, dict는 items()로 반복해서 내용을 출력해 보세요.",
                "첫 index를 1이라고 생각하는 것, list와 dict의 괄호 모양을 섞는 것, 존재하지 않는 key를 무조건 대괄호로 읽는 것이 흔한 실수입니다."));

        out.add(Track2Content.page(lesson, l, "정리", "챕터 4 정리 — 여러 값의 관계에 맞는 자료구조를 고른다",
                "list는 여러 값을 순서대로 저장하고 index 또는 반복문으로 다룹니다. dict는 key-value pair로 의미 있는 속성을 묶고 key로 조회합니다. append, len, get, items 같은 기본 도구를 익히면 실제 데이터 처리의 많은 부분을 만들 수 있습니다.\n\n"
                        + "자료구조를 외우기보다 데이터가 어떤 관계인지 먼저 그려 보세요. 다음 챕터에서는 list 안에서도 매우 자주 다루게 되는 문자열을 자르고, 정리하고, 분리하고, 다시 합치는 방법을 배웁니다.",
                "list는 ‘순서 있는 여러 값’, dict는 ‘이름표가 붙은 여러 속성’입니다. 이 한 문장을 기준으로 구조를 고르면 됩니다.",
                null, null, null, null,
                "collection · list · element · index · append · dict · key · value · get · items"));
    }
}
