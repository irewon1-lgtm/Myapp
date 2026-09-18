# PART 23 · Regex와 text parsing — 문자열 검색을 작은 언어 처리로 확장하기

문자열 안에서 숫자, 날짜, 식별자 같은 패턴을 찾을 때 regular expression은 강력하지만 모든 parsing 문제의 정답은 아니다. Regex는 문자 sequence의 형태를 기술하는 작은 언어이고, 복잡도가 커질수록 escaping, backtracking, Unicode, validation semantics를 함께 고려해야 한다. 목표는 패턴을 짧게 쓰는 것이 아니라 **무엇을 허용하고 무엇을 거부하는지 검증 가능한 언어로 표현하는 것**이다.

---

## CHAPTER 01 · 검색과 전체 검증은 다른 operation이다

### 시작 전 용어집

#### 1. 검증

- **뜻:** 문자열 어딘가에 패턴이 존재하는지 찾는 것과 문자열 전체가 패턴으로 구성되었는지 검증하는 것은 다르다.
- **왜 중요한가:** 전화번호 validation을 search로 구현하면 앞뒤에 임의 문자가 붙어 있어도 내부 일부가 맞아서 성공할 수 있다.
- **예시:** 문자열 어딘가에 패턴이 존재하는지 찾는 것과 문자열 전체가 …

#### 2. operation

- **뜻:** API가 search, match, full-match 중 어떤 semantics를 제공하는지 구분한다.
- **왜 중요한가:** 정규식 pattern에는 문자 literal, character class, repetition, grouping, alternation이 결합된다.
- **예시:** API가 search, match, full-match 중 어떤 semantics를 제공하는지 …

#### 3. validation

- **뜻:** Format validation과 semantic validation도 분리한다.
- **왜 중요한가:** `2026-99-99`가 날짜 모양 regex를 통과해도 실제 날짜는 아니다.
- **예시:** Format validation과 semantic validation도 분리한다.

#### 4. API

- **뜻:** Literal 검색이 목적이면 escape API를 사용한다.
- **왜 중요한가:** Pattern source와 data source를 구분하는 것은 SQL parameterization과 비슷한 경계 원리다.
- **예시:** Literal 검색이 목적이면 escape API를 사용한다.

이를 한 번에 외우기보다 입력 language를 작은 rule로 나누고 각각의 범위를 확인한다. “숫자 4자리-숫자 2자리-숫자 2자리”처럼 format structure를 먼저 쓰고 regex로 옮긴다.

  Regex는 lexical shape를 확인하고 날짜 parser가 calendar rule을 검사하도록 역할을 나눈다.

사용자 입력을 regex에 직접 삽입하면 그 입력의 metacharacter가 pattern syntax로 해석될 수 있다.  

---

## CHAPTER 02 · raw string은 Python 문자열 escape와 regex escape의 두 층을 분리한다

### 시작 전 용어집

#### 1. raw string

- **뜻:** Raw string literal은 많은 경우 첫 번째 escape 층을 줄여 pattern을 regex 표기에 가깝게 만든다.
- **왜 중요한가:** Raw string이라고 backslash 규칙이 모두 사라지는 것은 아니다.
- **예시:** Raw string literal은 많은 경우 첫 번째 escape …

#### 2. Python

- **뜻:** Regex pattern은 Python string literal 안에 작성되기 때문에 backslash가 Python parser와 regex engine 두 번의 의미 층을 거친다.
- **왜 중요한가:** `\d`, `\b` 같은 regex escape를 일반 문자열에 쓰면 Python escape와 충돌하거나 읽기 어려워질 수 있다.
- **예시:** Regex pattern은 Python string literal 안에 작성되기 때문에 …

#### 3. escape

- **뜻:** Literal 자체의 종료 quote와 마지막 backslash 같은 제약이 있으며 regex engine은 여전히 자신의 escape를 해석한다.
- **왜 중요한가:** Windows path를 regex와 함께 다룰 때는 path separator backslash가 또 다른 의미 층이 된다.
- **예시:** Literal 자체의 종료 quote와 마지막 backslash 같은 제약이 …

#### 4. regex

- **뜻:** Path를 string concatenation으로 pattern에 넣기보다 path API와 regex escape를 각각 사용해 경계를 분리한다.
- **왜 중요한가:** Debugging할 때 source literal, Python이 만든 실제 string value, regex parser가 해석한 pattern을 세 단계로 본다.
- **예시:** Path를 string concatenation으로 pattern에 넣기보다 path API와 regex …

`r"..."`를 마법적 안전 모드로 생각하지 않는다.

 

 `repr()`은 실제 backslash가 몇 개인지 확인하는 데 도움이 된다.

---

## CHAPTER 03 · greedy와 lazy quantifier는 가능한 match 중 어느 범위를 선택할지 바꾼다

### 시작 전 용어집

#### 1. greedy

- **뜻:** HTML-like text에서 첫 `<`부터 마지막 `>`까지 한 번에 잡히는 현상을 greedy semantics로 설명할 수 있다.
- **왜 중요한가:** 하지만 lazy로 바꾼다고 arbitrary nested markup parser가 되는 것은 아니다.
- **예시:** HTML-like text에서 첫 `<`부터 마지막 `>`까지 한 번에 …

#### 2. lazy quantifier

- **뜻:** `처럼 최소한으로 소비하는 lazy form도 있다.
- **왜 중요한가:** Quantifier가 nested되거나 alternation과 결합되면 engine이 여러 후보를 시도하며 backtracking할 수 있다.
- **예시:** `처럼 최소한으로 소비하는 lazy form도 있다.

#### 3. match

- **뜻:** 특정 pattern과 input 조합에서는 시도 횟수가 폭발해 CPU를 오래 사용할 수 있다.
- **왜 중요한가:** Untrusted input에 복잡한 regex를 적용할 때 ReDoS risk를 고려한다.
- **예시:** 특정 pattern과 input 조합에서는 시도 횟수가 폭발해 CPU를 …

#### 4. 반복

- **뜻:** 반복 가능한 token이 서로 겹치고 실패가 문자열 끝에서 늦게 결정되는 구조가 특히 위험할 수 있다.
- **왜 중요한가:** 가능한 경우 ambiguous repetition을 줄이고 delimiter와 upper bound를 명시한다.
- **예시:** 반복 가능한 token이 서로 겹치고 실패가 문자열 끝에서 …

`.*` 같은 반복은 가능한 한 많이 소비하는 greedy behavior를 가질 수 있고 `.*?  

  

성능 문제는 pattern 길이만으로 판단할 수 없다.  

Regex engine마다 지원 기능과 실행 algorithm이 다를 수 있으므로 성능 보장을 일반화하지 않는다. 중요한 validation pattern은 최악 입력 크기에서 benchmark하고 request size limit도 둔다.

---

## CHAPTER 04 · group은 capture와 precedence 두 역할을 가질 수 있다

### 시작 전 용어집

#### 1. group

- **뜻:** 괄호 grouping은 alternation과 repetition의 범위를 묶으면서 동시에 capture group을 만들 수 있다.
- **왜 중요한가:** 단순 precedence만 필요하다면 non-capturing group을 사용해 결과 group 번호가 불필요하게 바뀌는 것을 줄일 수 있다.
- **예시:** Pattern을 수정할 때 group index가 이동해 parser가 다른 …

#### 2. capture

- **뜻:** Backreference는 이전 capture와 같은 text를 요구하는 등 더 강한 constraint를 표현할 수 있지만 pattern complexity와 performance를 높인다.
- **왜 중요한가:** 단순 format validation이 목적이면 가능한 최소 기능을 사용한다.
- **예시:** Pattern을 수정할 때 group index가 이동해 parser가 다른 …

#### 3. precedence

- **뜻:** Pattern을 수정할 때 group index가 이동해 parser가 다른 값을 읽는 bug를 예방한다.
- **왜 중요한가:** Named group은 날짜의 year/month/day처럼 의미 있는 이름으로 값을 추출하게 해 position coupling을 줄인다.
- **예시:** Pattern을 수정할 때 group index가 이동해 parser가 다른 …

#### 4. regex

- **뜻:** Regex match 결과를 domain parser로 넘길 때 이름이 schema 역할을 한다.
- **왜 중요한가:** Optional group은 실제로 존재하지 않을 수 있으므로 반환값의 absence를 처리한다.
- **예시:** Regex match 결과를 domain parser로 넘길 때 이름이 …

여러 alternative가 서로 다른 group을 채우는 pattern은 결과 shape가 복잡해져 typed variant parser가 더 나을 수 있다.

 

---

## CHAPTER 05 · Unicode text에서 `\w`, 대소문자, 경계의 의미를 ASCII로 단정하지 않는다

### 시작 전 용어집

#### 1. Unicode text

- **뜻:** Regex character class와 word boundary가 Unicode에서 어떤 문자를 포함하는지는 engine flag와 language semantics에 따라 달라질 수 있다.
- **왜 중요한가:** `\w`가 `[A-Za-z0-9_]`와 완전히 같다고 가정하면 국제화된 사용자 이름에서 오류가 생길 수 있다.
- **예시:** Regex character class와 word boundary가 Unicode에서 어떤 문자를 …

#### 2. 경계

- **뜻:** Case-insensitive matching도 단순 ASCII lowercasing보다 복잡하다.
- **왜 중요한가:** Unicode case mapping에는 한 문자와 여러 문자의 관계, locale-sensitive rule이 존재할 수 있다.
- **예시:** Case-insensitive matching도 단순 ASCII lowercasing보다 복잡하다.

#### 3. ASCII

- **뜻:** Identifier security가 중요하면 어떤 script와 normalization을 허용할지 별도 정책이 필요하다.
- **왜 중요한가:** Grapheme cluster와 code point가 다르기 때문에 `.
- **예시:** Identifier security가 중요하면 어떤 script와 normalization을 허용할지 별도 …

#### 4. regex

- **뜻:** UI 글자 수 제한을 regex repetition으로 처리할 때 이 차이가 중요해진다.
- **왜 중요한가:** Text를 regex에 넣기 전 normalization할지 여부도 domain contract다.
- **예시:** UI 글자 수 제한을 regex repetition으로 처리할 때 …

` 한 번이 사용자가 보는 “한 글자”와 항상 같지 않을 수 있다. 

 서로 canonically equivalent한 Unicode sequence를 같은 값으로 볼지, raw bytes identity가 중요한지에 따라 달라진다.

---

## CHAPTER 06 · lookaround는 context를 검사하지만 소비하지 않는 조건을 표현한다

### 시작 전 용어집

#### 1. lookaround

- **뜻:** Password policy나 token boundary에서 유용해 보이지만 여러 lookaround를 겹치면 pattern이 선언적 validation language처럼 복잡해질 수 있다.
- **왜 중요한가:** Password strength를 “대문자 하나, 숫자 하나, 특수문자 하나”라는 regex 하나로 강제하는 것이 실제 security 목표와 맞는지도 별도 문제다.
- **예시:** Password policy나 token boundary에서 유용해 보이지만 여러 lookaround를 …

#### 2. context

- **뜻:** Lookahead와 lookbehind는 현재 위치 주변에 특정 pattern이 있는지 검사하면서 match 결과에 그 context를 포함시키지 않는 조건을 만들 수 있다.
- **왜 중요한가:** 길이와 compromised-password screening, rate limiting 같은 정책이 더 중요할 수 있다.
- **예시:** Lookahead와 lookbehind는 현재 위치 주변에 특정 pattern이 있는지 …

#### 3. 조건

- **뜻:** Regex가 표현할 수 있다는 이유로 domain policy 전체를 넣지 않는다.
- **왜 중요한가:** Lookbehind에는 engine별로 길이 제약이 있을 수 있다.
- **예시:** Regex가 표현할 수 있다는 이유로 domain policy 전체를 …

#### 4. validation

- **뜻:** Positive/negative boundary table을 만들고 어떤 condition 때문에 거부됐는지 explainable validation이 필요한 경우 일반 코드로 분리한다.
- **왜 중요한가:** 다른 language/runtime로 pattern을 옮길 계획이 있다면 portability를 확인한다.
- **예시:** Positive/negative boundary table을 만들고 어떤 condition 때문에 거부됐는지 …

복잡한 lookaround가 여러 개인 pattern은 sample만 보고 correctness를 판단하기 어렵다. 

---

## CHAPTER 07 · tokenization은 raw text를 parser가 다루기 쉬운 단위로 바꾼다

### 시작 전 용어집

#### 1. tokenization

- **뜻:** 간단한 표현식 `12 + 3 * x`를 parse하려면 문자 하나씩 직접 판단하는 대신 number, plus, star, identifier 같은 token으로 먼저 분리할 수 있다.
- **왜 중요한가:** Tokenizer는 whitespace와 lexical rule을 처리하고 parser는 token sequence의 grammar를 다룬다.
- **예시:** 간단한 표현식 `12 + 3 * x`를 parse하려면 …

#### 2. raw text

- **뜻:** Regex는 tokenizer 구현에 유용할 수 있다.
- **왜 중요한가:** 여러 token pattern을 조합하고 현재 position에서 어떤 token이 시작되는지 검사한다.
- **예시:** Regex는 tokenizer 구현에 유용할 수 있다.

#### 3. parser

- **뜻:** 각 token에 source offset이나 line/column을 보존하면 parser error가 사용자 입력의 어느 위치인지 설명할 수 있다.
- **왜 중요한가:** Text를 여러 단계로 변환하면서 위치 정보를 잃으면 최종 `syntax error`가 어디서 왔는지 찾기 어렵다.
- **예시:** 각 token에 source offset이나 line/column을 보존하면 parser error가 …

#### 4. regex

- **뜻:** 인식되지 않는 문자가 있으면 건너뛰기보다 위치와 함께 lexical error를 반환한다.
- **왜 중요한가:** Tokenizer와 parser를 분리하면 숫자 literal 규칙을 바꿀 때 grammar 전체를 고치지 않아도 된다.
- **예시:** 인식되지 않는 문자가 있으면 건너뛰기보다 위치와 함께 lexical …

작은 language에서도 boundary가 변화 이유를 분리한다.

---

## CHAPTER 08 · parser는 precedence와 associativity를 명시해 구조를 만든다

### 시작 전 용어집

#### 1. parser

- **뜻:** Parser는 token sequence를 AST로 바꾸면서 operator precedence와 associativity를 구조에 반영해야 한다.
- **왜 중요한가:** `+`보다 `*`가 더 강하게 묶이고 exponent가 right-associative인지 같은 rule이다.
- **예시:** Parser는 token sequence를 AST로 바꾸면서 operator precedence와 associativity를 …

#### 2. precedence

- **뜻:** `1 + 2 * 3`을 단순 왼쪽부터 계산하면 일반 산술 precedence와 다른 결과가 나온다.
- **왜 중요한가:** Recursive descent는 grammar rule을 함수로 대응시키는 한 방법이다.
- **예시:** `1 + 2 * 3`을 단순 왼쪽부터 계산하면 …

#### 3. associativity

- **뜻:** 간단한 expression grammar에서 `parse_expression`, `parse_term`, `parse_primary`처럼 precedence level을 분리할 수 있다.
- **왜 중요한가:** Left recursion 같은 grammar form은 그대로 구현하면 무한 recursion이 될 수 있어 변환이 필요하다.
- **예시:** 간단한 expression grammar에서 `parse_expression`, `parse_term`, `parse_primary`처럼 precedence level을 …

#### 4. AST

- **뜻:** AST를 만든 뒤 evaluator는 syntax parsing과 별도 단계에서 계산한다.
- **왜 중요한가:** 이렇게 하면 formatter, static checker, optimizer도 같은 AST를 사용할 수 있다.
- **예시:** AST를 만든 뒤 evaluator는 syntax parsing과 별도 단계에서 …

Raw string에서 바로 계산하는 코드보다 단계가 많아지지만 책임이 명확해진다.

User input language가 커질수록 hand-written parser보다 parser generator나 검증된 library가 적합할 수 있다. Error reporting과 grammar maintenance 비용을 비교한다.

---

## CHAPTER 09 · validation regex와 extraction regex는 계약을 분리하는 편이 낫다

### 시작 전 용어집

#### 1. validation

- **뜻:** 먼저 전체 structure가 맞는지 확인하고 필요한 부분을 parse하거나, parser가 structure와 extraction을 한 번에 책임지되 domain validation을 별도로 둔다.
- **왜 중요한가:** 로그 line에서 timestamp와 level, message를 추출하는 pattern은 extraction 중심이다.
- **예시:** 먼저 전체 structure가 맞는지 확인하고 필요한 부분을 parse하거나, …

#### 2. regex

- **뜻:** Pattern source에 comment와 verbose mode를 사용하면 복잡한 regex를 여러 줄로 설명할 수 있다.
- **왜 중요한가:** 하지만 설명이 길어질수록 일반 parser code가 더 읽기 쉬운지 다시 평가한다.
- **예시:** Pattern source에 comment와 verbose mode를 사용하면 복잡한 regex를 …

#### 3. 계약

- **뜻:** 하나의 거대한 pattern으로 형식 검증, 모든 field capture, business rule을 동시에 처리하면 작은 변경이 전체 pattern을 흔든다.
- **왜 중요한가:** 사용자 계정 ID 허용 문자 rule은 validation 중심이다.
- **예시:** 하나의 거대한 pattern으로 형식 검증, 모든 field capture, …

#### 4. 검증

- **뜻:** 로그 parser는 일부 malformed line을 quarantine할 수 있지만 ID validation은 요청을 즉시 거부할 수 있다.
- **왜 중요한가:** Compiled pattern을 반복 재사용하면 parsing overhead를 줄일 수 있으나 global registry가 필요 이상으로 커지지 않게 한다.
- **예시:** 로그 parser는 일부 malformed line을 quarantine할 수 있지만 …

목적이 다르면 실패 처리도 다르다. 

 많은 동적 pattern을 cache하면 memory와 eviction 문제가 생긴다.

 

---

## CHAPTER 10 · regex를 떠나야 하는 시점을 grammar와 failure semantics로 판단한다

### 시작 전 용어집

#### 1. regex

- **뜻:** Nested 괄호, recursive structure, 복잡한 quoting, context-sensitive rule이 늘어나면 regex 하나로 해결하려는 시도가 유지보수와 성능 문제를 만든다.
- **왜 중요한가:** 언어의 구조가 tree를 요구한다면 tokenizer와 parser, AST가 더 자연스럽다.
- **예시:** 또한 사용자가 어떤 부분을 잘못 입력했는지 상세히 알려 …

#### 2. grammar

- **뜻:** Text parsing의 핵심은 **문자열을 하나의 덩어리로 보지 않고 lexical shape, grammar structure, semantic rule을 다른 층으로 분리하고 각 층에 맞는 도구를 선택하는 것**이다.
- **왜 중요한가:** 또한 사용자가 어떤 부분을 잘못 입력했는지 상세히 알려 줘야 한다면 거대한 full-match 실패 하나보다 parser가 예상 token과 위치를 제공하는 편이 좋다.
- **예시:** Text parsing의 핵심은 **문자열을 하나의 덩어리로 보지 않고 …

#### 3. failure semantics

- **뜻:** Error reporting requirement 자체가 도구 선택에 영향을 준다.
- **왜 중요한가:** 로그 검색, 단순 identifier validation, format extraction, text replacement처럼 local pattern이 명확한 문제에서는 간결하고 검증된 도구다.
- **예시:** Error reporting requirement 자체가 도구 선택에 영향을 준다.

#### 4. AST

- **뜻:** 무조건 피하거나 모든 것을 regex로 해결하는 극단을 피한다.
- **예시:** 무조건 피하거나 모든 것을 regex로 해결하는 극단을 피한다.

Regex가 적합한 영역도 넓다.
