# PART 22 · Pattern matching과 variant model — 가능한 상태를 분기 구조에 그대로 표현하기

프로그램은 값 하나만 처리하는 것이 아니라 “성공/실패”, “현금/카드/계좌”, “텍스트/이미지/파일”처럼 서로 다른 형태의 variant를 다룬다. 조건문을 끝없이 늘리는 대신 variant를 명시적인 데이터 모델로 만들고 pattern matching으로 구조를 분해하면 상태 공간을 더 직접적으로 표현할 수 있다. 핵심은 문법의 화려함이 아니라 **어떤 경우가 가능한지와 각 경우에 필요한 데이터가 무엇인지**를 코드에 드러내는 것이다.

---

## CHAPTER 01 · variant는 같은 개념의 서로 다른 유효 형태다

결제 결과를 생각하면 성공에는 transaction ID가 필요하고 거절에는 reason이 필요하며 pending에는 polling token이 필요할 수 있다. 이 세 상태를 하나의 dict에 optional field 여러 개로 넣으면 `status=SUCCESS`인데 transaction ID가 없거나 decline reason이 함께 존재하는 잘못된 조합이 가능해진다.

각 상태를 별도 class/dataclass로 표현하면 생성 가능한 state가 줄어든다. `PaymentSuccess(transaction_id)`, `PaymentDeclined(reason)`, `PaymentPending(token)`처럼 variant 자체가 필요한 필드를 소유한다. 호출자는 현재 variant를 확인한 뒤 그 상태에 존재하는 data만 사용한다.

이 모델은 type union과 잘 맞는다. Return type을 세 variant의 union으로 선언하면 static analyzer가 branch마다 type을 좁힐 수 있다. Optional field 네 개를 가진 거대한 object보다 contract가 선명하다.

Variant 설계의 기준은 “class를 많이 만들고 싶어서”가 아니라 서로 배타적인 상태가 다른 data와 behavior를 요구하는가다. 단순 boolean 하나면 충분한 상태를 과도하게 타입으로 쪼갤 필요는 없다.

---

## CHAPTER 02 · structural pattern matching은 값의 타입과 내부 구조를 함께 검사할 수 있다

Python의 pattern matching은 단순 switch-case 숫자 비교보다 구조적이다. Sequence의 모양, mapping key, class field, literal을 기준으로 pattern을 선택하고 내부 값을 이름에 bind할 수 있다. Nested data parser나 command dispatch에서 if/elif보다 구조가 명확해질 수 있다.

Pattern은 위에서부터 시도되므로 더 넓은 pattern을 앞에 두면 뒤의 구체 pattern이 도달하지 못할 수 있다. Branch ordering은 조건문과 마찬가지로 policy다. Catch-all pattern을 너무 일찍 두면 새로운 variant가 추가되어도 조용히 default path로 들어가 exhaustiveness 문제를 숨길 수 있다.

Pattern matching은 object를 변환하지 않고 현재 structure를 해석하는 데 집중한다. Pattern 안에서 복잡한 side effect를 수행하기보다 branch body에서 필요한 operation을 명시한다. Guard를 사용할 수 있지만 guard가 너무 복잡해지면 별도 predicate로 추출한다.

Framework payload를 pattern matching으로 바로 다루기 전에 schema validation을 통과시킨다. Untrusted raw mapping은 key 누락과 타입 오류가 많으므로 pattern이 validation 전체를 대신하게 하지 않는다.

---

## CHAPTER 03 · literal pattern과 capture name을 혼동하면 예상보다 넓게 매칭된다

Pattern 문법에서 어떤 이름은 기존 변수와 비교하는 것이 아니라 새 capture binding을 만드는 의미를 가질 수 있다. 따라서 “이 상수와 같은지 검사한다”고 생각했는데 실제로는 어떤 값이든 이름에 bind해 branch가 항상 맞는 오류가 생길 수 있다. Enum이나 qualified constant를 사용해 literal/value pattern 의도를 명확히 한다.

Capture는 branch 내부에서 새로운 이름을 사용할 수 있게 해 data extraction을 간결하게 한다. 하지만 같은 pattern에서 이름이 언제 bound되는지와 실패한 alternative의 binding semantics를 정확히 이해해야 한다. 복잡한 OR pattern에서는 각 alternative가 호환되는 binding을 제공해야 할 수 있다.

Pattern을 읽을 때는 “이 위치는 비교인가 bind인가”를 표시해 보는 습관이 좋다. 일반 expression과 pattern language는 유사해 보여도 semantics가 다르다.

코드 리뷰에서는 match branch가 너무 자유로운 capture를 사용해 malformed input까지 정상 상태로 받아들이지 않는지 확인한다. Pattern은 구조 검사의 도구이지만 업무 validation rule을 자동으로 보장하지 않는다.

---

## CHAPTER 04 · sequence pattern은 길이와 위치의 계약을 표현한다

`[command, argument]` 형태의 pattern은 두 요소를 가진 sequence structure를 검사하고 각 위치 값을 bind할 수 있다. Command parser, token stream, recursive tree representation에서 자연스럽다. Star pattern을 사용하면 head/tail 구조도 표현할 수 있다.

그러나 문자열도 sequence처럼 보일 수 있는 일반 개념과 pattern matching에서 실제 어떤 타입이 sequence pattern 대상인지 언어 규칙을 확인해야 한다. 사용자가 기대하는 모든 iterable이 동일하게 destructure되는 것은 아니다.

Position-based structure가 오래 유지되기 어렵다면 named class/mapping variant가 더 낫다. `[id, name, age, role, status]`처럼 위치 의미가 많아지면 index 순서를 기억해야 한다. Sequence pattern은 작은 고정 구조에서 가장 읽기 좋다.

Star capture는 나머지 요소를 새 collection으로 만들 수 있어 대용량 입력에서 allocation을 고려한다. Stream 전체를 pattern에 넣기보다 parser가 필요한 작은 token window만 제공하는 것이 적합할 수 있다.

---

## CHAPTER 05 · mapping pattern은 필요한 key를 선택하지만 unknown field 정책은 별도다

JSON-like mapping에서 `{"type": "user", "id": id}` 같은 pattern은 필요한 key와 literal discriminator를 동시에 검사할 수 있다. 다른 추가 key가 있어도 match되는지 여부 등 mapping pattern의 semantics를 이해하고 strict schema 요구가 있다면 별도 unknown-field validation을 수행한다.

Discriminator field는 variant decoding에 유용하다. `type=card`, `type=bank`처럼 명시적 tag가 있으면 각 payload schema를 선택할 수 있다. Tag 없이 field 존재 여부만으로 variant를 추측하면 두 schema가 겹칠 때 ambiguity가 생길 수 있다.

Mapping pattern 안에서 nested pattern을 사용해 내부 구조를 한 번에 검사할 수 있지만 너무 깊어지면 error가 왜 발생했는지 사용자에게 설명하기 어렵다. Public API validation은 schema tool로 field-specific error를 만들고, 이미 validated된 internal dict를 dispatch하는 데 pattern matching을 쓰는 식으로 역할을 나눌 수 있다.

Key 이름을 protocol version에서 바꾸면 match code가 모두 영향을 받는다. Wire DTO를 domain variant로 변환하는 adapter 한 곳에 pattern을 집중시키면 변화 범위를 줄인다.

---

## CHAPTER 06 · class pattern은 object interface와 pattern interface를 연결한다

Class instance도 지정된 attribute 구조를 기준으로 pattern matching할 수 있다. Dataclass나 named model에서 `case Point(x=0, y=y):` 같은 형태는 타입 확인과 field extraction을 동시에 표현한다. Position pattern을 지원하는 class에서는 field 순서가 public pattern API가 될 수 있으므로 함부로 바꾸지 않는다.

Class pattern은 constructor를 다시 호출하는 것이 아니라 existing object를 inspect한다. Property나 descriptor가 pattern access 중 실행될 수 있다면 비용과 side effect가 발생할 가능성을 고려한다. Matching을 pure structural inspection으로 기대하는 호출자에게 surprise를 줄 수 있다.

Domain variant class와 match는 잘 맞지만 behavior가 variant object 안에 자연스럽게 들어갈 수 있다면 polymorphism이 더 나을 수도 있다. Caller가 모든 variant를 알아야 하는지, 각 object가 스스로 operation을 수행해야 하는지 책임을 비교한다.

Pattern matching과 polymorphism은 경쟁하는 유행이 아니다. Data transformation/serialization처럼 variant별 구조가 중요하면 matching이 자연스럽고, operation이 안정적이며 variant가 behavior를 소유하면 method dispatch가 강할 수 있다.

---

## CHAPTER 07 · guard는 구조가 맞은 뒤 추가 predicate를 적용한다

Pattern만으로 표현하기 어려운 range와 domain predicate는 guard를 사용할 수 있다. 예를 들어 integer field를 bind한 뒤 `if amount > 0`을 붙여 양수 variant를 처리한다. 구조 검사와 값 규칙이 순서대로 드러난다.

Guard가 network 조회나 state mutation을 수행하면 branch 선택 자체가 effectful해지고 재실행이 어려워진다. Guard는 가능한 한 pure predicate로 유지하고 외부 operation은 branch body로 이동한다.

여러 branch가 같은 구조 pattern에 서로 다른 guard만 가진다면 범위가 겹치는지와 순서를 검사한다. `score >= 60`이 `score >= 90`보다 앞에 있으면 높은 점수도 첫 branch에 잡힌다. Pattern matching도 기존 조건 분기의 경계 문제를 그대로 가진다.

Guard 실패 후 다음 case가 시도될 수 있으므로 capture와 side effect를 섞지 않는다. Branch selection은 선언적 판정으로 유지하는 편이 예측 가능하다.

---

## CHAPTER 08 · exhaustive handling은 새 variant가 추가될 때 누락을 발견하게 한다

Variant model의 장점 중 하나는 가능한 상태 집합을 명시할 수 있다는 점이다. 그러나 마지막에 무조건 catch-all default를 두면 새 variant가 추가되어도 기존 code가 조용히 default 동작을 할 수 있다. 그 동작이 안전한지 검토해야 한다.

정적 타입 도구와 `assert_never` 계열 기법을 사용하면 union의 모든 variant를 처리했는지 검사할 수 있는 경우가 있다. 새 타입이 union에 추가되면 처리하지 않은 branch에서 type check가 실패하도록 만드는 것이다. Runtime default와 compile-time-like exhaustiveness 역할을 구분한다.

External protocol은 미래에 unknown variant가 들어올 수 있으므로 완전 거부, ignore, forward compatibility 중 정책이 필요하다. Internal closed union과 open-world wire enum은 다른 contract다.

Payment status처럼 unknown 값을 잘못 성공으로 처리하면 위험한 곳에서는 fail-closed가 적합할 수 있다. Telemetry event처럼 모르는 type을 저장만 하고 계속 처리할 수 있는 곳은 다른 정책을 가질 수 있다.

---

## CHAPTER 09 · pattern matching은 parser의 한 층이지 grammar 전체를 대신하지 않는다

작은 token list와 command structure는 pattern으로 읽기 쉽지만 복잡한 nested language를 match case 수백 개로 만들면 grammar와 precedence가 숨는다. 표현식 언어, config DSL처럼 문법이 커지면 tokenizer/parser 구조와 AST를 명시하는 것이 더 적합하다.

Pattern matching은 parser가 만든 AST node를 해석하거나 transform하는 단계에서 강하다. `Binary(op, left, right)`, `Literal(value)`, `Call(name,args)` 같은 node variant를 match해 evaluator를 구현할 수 있다. Data structure가 grammar 구조를 직접 반영한다.

새 AST variant가 추가될 때 evaluator, formatter, type checker가 각각 branch를 추가해야 할 수 있다. Operation 수가 많은 구조에서는 visitor/polymorphism 같은 다른 dispatch 전략도 비교한다.

문법 처리에서도 입력 syntax error와 semantic error를 분리한다. Pattern이 맞지 않는다고 모두 같은 invalid input으로 만들지 않고 token 위치와 expected structure를 보존하면 debugging과 사용자 경험이 좋아진다.

---

## CHAPTER 10 · 상태 공간을 데이터로 모델링하면 분기 복잡도를 통제할 수 있다

복잡한 조건문을 리팩터링할 때 먼저 branch를 줄이려고 하지 않는다. 실제로 가능한 상태가 무엇인지 enum, variant class, state machine으로 명명한다. 그 뒤 각 상태에 필요한 data를 붙이면 잘못된 조합이 줄고 match/polymorphism 중 적합한 dispatch를 선택할 수 있다.

Boolean flag 세 개는 이론상 8개 조합을 만들지만 실제 유효 상태가 4개뿐이라면 네 variant로 모델링하는 편이 강하다. `is_paid`, `is_cancelled`, `is_shipped`가 서로 모순될 수 있는 object보다 `Created|Paid|Shipped|Cancelled`가 state space를 직접 표현한다.

Pattern matching은 이 state model을 읽는 한 방법이다. 모든 상태를 한 함수가 알아야 하는 transformation에서는 명확하지만 객체가 자신의 behavior를 소유해야 하는 operation은 method가 더 자연스러울 수 있다.

중요한 것은 `match` 문법을 쓰는 것이 아니라 **가능한 상태를 명시하고 불가능한 상태가 만들어지는 공간을 줄이며 새 상태가 추가됐을 때 누락을 발견할 수 있는 구조**를 만드는 것이다.