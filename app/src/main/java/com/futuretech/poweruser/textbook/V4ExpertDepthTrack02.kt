package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack02 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C02-S01",
            depthTitle("문제 분해 다음 단계: 요구사항을 “상태·불변식·계약”으로 바꾼다"),
            depthParagraph("""
V3에서 목표→입력→처리→출력과 변수·자료형을 충분히 다뤘다면 더 이상 “문제를 쪼개라”를 반복할 필요가 없다. 실제 프로그램에서 중요한 것은 쪼갠 조각 사이의 계약이다. 어떤 입력을 허용하는지, 어떤 상태에서 시작해야 하는지, 성공하면 무엇이 보장되는지, 실패하면 상태가 어떻게 남는지를 명시해야 여러 함수와 사람이 같은 규칙을 공유할 수 있다.
"""),
            depthHeading("1. precondition과 postcondition으로 함수의 약속을 쓴다"),
            depthParagraph("""
transfer(from,to,amount)라는 함수가 있다면 precondition은 amount>0, from과 to가 존재함 같은 조건이고, postcondition은 성공 시 총 자산 합이 변하지 않고 A는 amount만큼 감소하며 B는 증가한다 같은 조건이다. 코드 줄보다 이 계약이 먼저 명확하면 테스트 케이스도 자연스럽게 나온다. AI에게 코드를 맡길 때도 “무엇을 구현해”보다 계약과 반례를 주는 편이 훨씬 정확하다.
"""),
            depthHeading("2. 상태 머신으로 “가능한 순서”를 모델링한다"),
            depthParagraph("""
주문 상태가 CREATED→PAID→SHIPPED→DELIVERED로 이동한다면 모든 문자열 조합을 허용하면 안 된다. DELIVERED에서 다시 CREATED로 돌아가거나 CANCELLED 주문을 SHIPPED로 만드는 전이를 막아야 한다. 상태 머신은 상태 이름보다 “어떤 전이가 어떤 조건에서 허용되는가”를 명시하는 도구다. UI 버튼 활성화, API validation, DB constraint가 이 규칙을 공유해야 한다.
"""),
            depthHeading("3. 값 객체로 의미를 타입에 담는다"),
            depthParagraph("""
price와 userId가 둘 다 숫자라고 해서 서로 바꿔 써도 되는 것은 아니다. 규모가 커지면 Money, Email, OrderId처럼 도메인 의미를 가진 타입이나 value object를 만들어 잘못된 조합을 컴파일 또는 생성 시점에 막을 수 있다. 단순 primitive만 넘기는 코드는 짧지만 “이 숫자가 무엇인지” 규칙이 호출자 머릿속에만 남기 쉽다.
"""),
            depthHeading("4. 불변성은 변경 자체를 금지하는 종교가 아니다"),
            depthParagraph("""
immutable data는 값이 바뀌지 않기 때문에 여러 함수가 같은 객체를 공유해도 누가 몰래 수정했는지 추적할 필요가 줄어든다. 상태 변화가 필요한 경우 새 값을 만들어 교체하면 이전 상태와 비교·undo·테스트가 쉬워진다. 성능이나 대용량 데이터 때문에 mutation이 필요한 곳에서는 변경 책임을 좁혀 통제한다.
"""),
            depthHeading("5. precision도 요구사항이다"),
            depthParagraph("""
가격, 세율, 포인트, 측정값은 “숫자” 하나로 끝나지 않는다. 허용 소수 자릿수, 반올림 시점, 단위, overflow 범위를 정해야 한다. 예를 들어 부가세를 품목별로 반올림할지 주문 총액에서 한 번 반올림할지에 따라 합계가 달라질 수 있다. 이런 규칙은 나중에 고치기 어렵기 때문에 데이터 모델 초기에 계약으로 남긴다.
"""),
            depthCode("""
주문 상태 계약 예

CREATED -> PAID      : 결제 성공일 때만
CREATED -> CANCELLED : 결제 전 취소 가능
PAID    -> SHIPPED   : 재고 확정 + 송장 생성 후
SHIPPED -> DELIVERED : 배송 완료 이벤트 후

금지
DELIVERED -> CREATED
CANCELLED -> SHIPPED

“status = 문자열”보다 허용 전이가 핵심이다.
"""),
            depthBullets(
                "함수명과 변수명만 예쁘게 바꾸는 것을 설계 개선으로 착각하지 않는다.",
                "상태를 저장하기 전에 허용 전이와 실패 후 상태를 정의한다.",
                "금액·시간·거리 같은 값은 단위와 정밀도 규칙을 타입/계약에 포함한다."
            )
        ),
        expertPack(
            sectionId = "V1-C02-S02",
            depthTitle("조건과 반복을 “문법”에서 “논리·불변식·종료 증명”으로 올린다"),
            depthHeading("조건식은 자연어 정책을 논리식으로 번역하는 작업이다"),
            depthParagraph("""
“성인이면서 회원이거나 관리자면 입장 가능” 같은 문장은 AND/OR 우선순위를 잘못 묶으면 전혀 다른 권한 규칙이 된다. 복잡한 조건은 truth table이나 작은 이름을 가진 predicate로 쪼개 실제 조합을 확인한다. 보안 규칙에서는 특히 부정 조건과 예외를 한 줄에 섞지 않고, deny-by-default 후 허용 조건을 명시하는 편이 안전하다.
"""),
            depthHeading("1. short-circuit는 실행되는 코드 자체를 바꾼다"),
            depthParagraph("""
A && B에서 A가 false면 B를 평가하지 않고, A || B에서 A가 true면 B를 평가하지 않는 언어가 많다. 그래서 obj != null && obj.value > 0처럼 안전한 접근을 만들 수 있지만, B 안에 side effect를 넣으면 조건 결과에 따라 호출 여부가 달라져 읽기 어려워진다. 조건식은 가능하면 “판단”에 집중하고 상태 변경은 분리한다.
"""),
            depthHeading("2. loop invariant로 반복문의 중간 상태를 설명한다"),
            depthParagraph("""
배열 앞에서부터 최댓값을 찾는 loop라면 i번째 반복이 끝날 때 “max는 0..i 구간의 최댓값”이라는 invariant를 세울 수 있다. 시작 전에 참이고, 한 번 반복해도 유지되고, 종료 시 전체 범위에 대해 참이면 알고리즘의 올바름을 설명할 수 있다. 단순히 실행 결과 몇 개를 맞춰 보는 것보다 강한 사고 도구다.
"""),
            depthHeading("3. 종료 조건은 variant가 줄어드는지까지 본다"),
            depthParagraph("""
while이 끝나려면 조건문이 언젠가 false가 되어야 한다. “남은 작업 수”, “탐색 구간 길이”처럼 매 반복마다 한 방향으로 감소하는 값을 variant로 잡으면 무한 루프 가능성을 분석하기 쉽다. binary search에서 low/high 업데이트를 잘못해 구간이 줄지 않으면 조건이 그럴듯해 보여도 끝나지 않을 수 있다.
"""),
            depthHeading("4. off-by-one은 경계 모델이 없어서 생긴다"),
            depthParagraph("""
0부터 n-1까지인지 1부터 n까지인지, end index를 포함하는지 제외하는지 혼용하면 마지막 항목 누락·배열 범위 오류가 생긴다. API와 함수마다 interval convention을 정하고, [start,end)처럼 반열린 구간을 사용하면 길이가 end-start가 되어 계산이 단순해진다. 반복문을 작성한 뒤 빈 범위, 원소 1개, 마지막 index를 손으로 추적한다.
"""),
            depthHeading("5. iterator는 반복 방법을 데이터 구조에서 분리한다"),
            depthParagraph("""
for-each가 배열뿐 아니라 set, map, generator에도 동작하는 이유는 “다음 값을 어떻게 얻는가”라는 iterator protocol을 구조가 제공하기 때문이다. 이 추상화를 이해하면 collection 전체를 메모리에 만들지 않고 값 하나씩 생성하는 lazy iteration과 generator로 넘어갈 수 있다.
"""),
            depthCode("""
binary search에서 구간을 [low, high)로 잡는 예

초기: low=0, high=n
반복 조건: low < high
mid = (low + high) // 2

arr[mid] < target 이면 low = mid + 1
그 외에는 high = mid

매 반복마다 high-low가 반드시 줄어드는지 확인한다.
"""),
            depthBullets(
                "복잡한 boolean 식은 진리표나 이름 붙인 predicate로 실제 조합을 확인한다.",
                "반복문은 시작값·invariant·변화값·종료 상태 네 가지로 설명할 수 있어야 한다.",
                "경계 테스트는 빈 입력, 원소 1개, 첫/마지막, 한도 바로 전/후를 포함한다."
            )
        ),
        expertPack(
            sectionId = "V1-C02-S03",
            depthTitle("자료구조와 함수를 연결하는 핵심: aliasing·hashability·lazy evaluation·call stack"),
            depthParagraph("""
V3에서 list·tuple·dict·set과 함수 기본을 이미 배웠다면 다음 문제는 “왜 같은 값처럼 보이는데 수정 결과가 달라지는가”다. 참조형 객체를 여러 변수가 동시에 가리키는 aliasing, 변경 가능한 값의 hash 사용 제한, 함수 호출이 stack을 만드는 방식까지 이해해야 Python 코드가 커졌을 때 예상 가능한 동작을 만든다.
"""),
            depthHeading("1. shallow copy는 중첩 객체까지 복제하지 않는다"),
            depthParagraph("""
리스트를 list(original)이나 슬라이스로 복사하면 바깥 리스트 컨테이너는 새로 생겨도 내부에 들어 있는 dict·list 객체는 같은 참조일 수 있다. 그래서 copy[0][\"name\"]을 바꿨는데 original도 바뀌는 일이 생긴다. deep copy는 모든 상황의 정답이 아니라 객체 그래프가 크거나 공유해야 할 객체가 있을 때 비용과 의미 문제가 있으므로 데이터 모델부터 공유 여부를 명확히 한다.
"""),
            depthHeading("2. dict key와 set item에는 안정적인 hash가 필요하다"),
            depthParagraph("""
hash table은 key의 hash 값으로 bucket 후보를 찾는다. 저장한 뒤 key의 동등성·hash가 바뀌면 원래 위치에서 찾을 수 없게 되므로 Python의 list 같은 mutable 객체는 일반적으로 dict key로 쓸 수 없다. tuple도 내부에 unhashable 값이 있으면 key가 될 수 없다. “왜 set에는 list를 넣지 못하지?”를 문법 제한이 아니라 자료구조 invariant로 이해한다.
"""),
            depthHeading("3. generator는 값을 미리 모두 만들지 않는다"),
            depthParagraph("""
백만 줄 파일을 list로 읽으면 메모리에 백만 항목을 만들지만 generator는 필요할 때 다음 값을 하나씩 생산할 수 있다. lazy evaluation은 메모리를 줄이고 pipeline을 스트리밍하게 만들지만, 한 번 소비한 iterator를 다시 쓸 수 없는 경우나 실제 오류가 반복 시점까지 늦게 발생하는 특성이 있다. eager와 lazy 중 어떤 의미가 필요한지 선택한다.
"""),
            depthHeading("4. 함수 호출은 stack frame을 만든다"),
            depthParagraph("""
함수를 호출하면 parameter, local variable, 돌아갈 위치 같은 실행 정보를 담는 frame이 call stack에 쌓인다. 재귀가 너무 깊으면 frame이 계속 쌓여 recursion limit/stack overflow 문제가 생긴다. 디버거에서 stack frame을 선택해 각 함수 시점의 지역 변수를 볼 수 있는 이유도 이 구조 때문이다.
"""),
            depthHeading("5. 순수 함수와 side effect를 분리하면 테스트가 단순해진다"),
            depthParagraph("""
같은 입력이면 항상 같은 출력을 내고 외부 상태를 바꾸지 않는 pure function은 테스트가 쉽다. 반면 파일 쓰기, DB 저장, 현재 시간 읽기, 로그 출력은 side effect다. 실제 프로그램에는 side effect가 필요하지만 계산 로직과 경계를 분리하면 핵심 규칙을 빠르게 검증하고 외부 실패를 따로 다룰 수 있다.
"""),
            depthCode("""
a = [{\"name\": \"Kim\"}]
b = list(a)          # 바깥 list만 새 객체

b[0][\"name\"] = \"Lee\"

# a[0]와 b[0]은 같은 dict를 가리킬 수 있다.
# 결과적으로 a에서도 \"Lee\"가 보인다.

질문은 “복사했나?”가 아니라
“객체 그래프 어느 수준까지 새 객체인가?”다.
""", "python"),
            depthBullets(
                "복사 문제는 object identity와 reference graph를 그려 본다.",
                "대용량 collection에서는 “전부 메모리에 올려야 하는가?”를 먼저 묻는다.",
                "함수 안 계산과 외부 I/O를 분리하면 실패 지점과 테스트 범위가 선명해진다."
            )
        ),
        expertPack(
            sectionId = "V1-C02-S04",
            depthTitle("모듈과 예외 다음 단계: 의존성 그래프·자원 수명·오류 의미를 설계한다"),
            depthHeading("import는 의존성 그래프를 만든다"),
            depthParagraph("""
파일을 나눴다고 결합도가 자동으로 낮아지는 것은 아니다. A가 B를 import하고 B가 C를 import하면 A는 간접적으로 C의 초기화와 버전에 영향을 받을 수 있다. 특히 import 시점에 DB 연결, 환경 변수 검사, 파일 쓰기 같은 side effect를 실행하면 테스트와 CLI 도구에서 예상치 못한 일이 생긴다. 모듈 top-level에는 가급적 정의를 두고 실제 실행은 명시적인 entry point에서 시작한다.
"""),
            depthHeading("1. 순환 의존은 초기화 순서 문제를 만든다"),
            depthParagraph("""
A가 B를 import하는 중 B가 다시 A의 아직 생성되지 않은 이름을 사용하면 부분 초기화 상태를 보게 된다. 임시로 import 위치를 옮겨 숨기기보다 두 모듈이 함께 의존하는 개념을 제3의 하위 모듈로 옮기거나 dependency 방향을 재설계하는 편이 낫다. dependency graph에 cycle이 생겼다는 것은 책임 경계가 꼬였다는 신호일 수 있다.
"""),
            depthHeading("2. exception type은 복구 전략을 전달한다"),
            depthParagraph("""
잘못된 사용자 입력, 파일 없음, 일시적 네트워크 timeout, 프로그래밍 버그를 모두 Exception 하나로 처리하면 무엇을 retry하고 무엇을 사용자에게 고쳐 달라고 해야 할지 알 수 없다. 오류를 의미 있는 종류로 분류하고, 처리할 수 있는 계층에서만 catch한다. 복구할 수 없다면 원인 정보를 보존한 채 상위로 전파하는 것이 낫다.
"""),
            depthHeading("3. context manager는 자원 수명을 코드 구조로 고정한다"),
            depthParagraph("""
파일·DB connection·lock은 성공 경로뿐 아니라 예외가 발생해도 반드시 닫거나 해제해야 한다. Python의 with 문은 enter/exit 경계를 통해 cleanup을 보장하는 패턴을 제공한다. try/finally를 매번 손으로 쓰는 것보다 “이 블록이 끝나면 자원이 정리된다”는 구조를 눈에 보이게 만든다.
"""),
            depthHeading("4. 로그에는 사건을 다시 연결할 식별자가 필요하다"),
            depthParagraph("""
“저장 실패”라는 문장 10만 줄보다 request_id, user_id(민감도 고려), operation, error_type, elapsed_ms가 구조화된 로그가 훨씬 검색 가능하다. password/token 같은 secret은 남기지 않고, trace/request id를 여러 계층에서 동일하게 전달하면 한 사용자 요청이 어디서 실패했는지 연결할 수 있다.
"""),
            depthHeading("5. 설정과 secret은 코드와 수명이 다르다"),
            depthParagraph("""
환경별 API endpoint, timeout, feature flag, DB credential을 source에 하드코딩하면 변경마다 새 build가 필요하고 secret 유출 위험이 커진다. 설정은 schema로 검증하고, secret은 전용 저장소나 OS/플랫폼 보안 기능을 사용한다. “환경 변수면 무조건 안전”한 것이 아니라 노출 범위와 권한을 함께 관리해야 한다.
"""),
            depthCode("""
요청 처리 중 DB timeout 발생

controller: 사용자 입력/HTTP 변환
service: 주문 규칙
repository: DB 호출 -> DbTimeoutError
service: 일시적 저장 실패로 의미 변환, 원인 보존
controller: 503 + request_id 반환
log: request_id, operation, error_type, elapsed_ms

어느 계층도 password나 전체 request body를 통째로 기록하지 않는다.
"""),
            depthBullets(
                "catch-all 후 빈 값 반환은 오류를 정상 데이터로 위장할 수 있다.",
                "자원 open과 close가 항상 한 구조 안에서 짝을 이루는지 확인한다.",
                "import graph의 cycle을 “문법 문제”가 아니라 설계 신호로 본다."
            )
        )
    )
}
