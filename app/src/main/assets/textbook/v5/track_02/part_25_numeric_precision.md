# PART 25 · Numeric precision — 정수·부동소수점·Decimal을 값의 의미에 맞게 선택하기

숫자는 모두 같은 종류의 값처럼 보이지만 계산 모델은 다르다. 정수는 정확한 개수와 식별자를 표현하는 데 강하고, binary floating point는 넓은 범위의 근사 계산에 효율적이며, decimal arithmetic은 십진 규칙이 계약인 금액과 회계 계산에서 의미가 있다. 숫자 오류를 줄이려면 자료형 이름보다 **어떤 오차가 허용되고 어떤 반올림 규칙을 따라야 하는가**를 먼저 정해야 한다.

---

## CHAPTER 01 · 정수는 정확하지만 크기와 외부 경계 비용이 사라지는 것은 아니다

### 시작 전 용어집

#### 1. 경계

- **뜻:** 다른 언어의 truncation rule을 그대로 기대하면 경계값에서 다른 결과가 날 수 있다.
- **왜 중요한가:** Python의 일반 정수는 값이 커질 때 고정 폭 overflow 대신 필요한 크기에 맞춰 확장되는 semantics를 제공한다.
- **예시:** 다른 언어의 truncation rule을 그대로 기대하면 경계값에서 다른 …

#### 2. protocol

- **뜻:** 하지만 정수가 커질수록 memory와 연산 비용이 늘고, database나 network protocol의 fixed-width integer와 교환할 때 범위 제한이 다시 등장한다.
- **왜 중요한가:** 외부 API가 signed 64-bit를 요구한다면 Python 안에서 더 큰 값이 만들어진다고 그대로 전송할 수 있는 것은 아니다.
- **예시:** 하지만 정수가 커질수록 memory와 연산 비용이 늘고, database나 …

#### 3. API

- **뜻:** 그래서 32-bit 정수 최대값을 넘는 계산이 즉시 wraparound하지 않을 수 있다.
- **왜 중요한가:** Serialization boundary에서 range를 검증한다.
- **예시:** 그래서 32-bit 정수 최대값을 넘는 계산이 즉시 wraparound하지 …

#### 4. serialization

- **뜻:** C extension이나 binary file format을 사용할 때도 같은 문제가 생긴다.
- **왜 중요한가:** 식별자가 숫자 문자로만 구성되어 있다고 산술 타입으로 만들 필요는 없다.
- **예시:** C extension이나 binary file format을 사용할 때도 같은 …

우편번호와 계정번호처럼 leading zero가 의미 있고 더하기/빼기를 하지 않는 값은 문자열 또는 domain type이 더 정확하다. 숫자 모양과 숫자 의미를 구분한다.

정수 division에서도 `/`와 floor division의 결과 타입과 음수에서의 rounding direction을 확인한다. 

---

## CHAPTER 02 · binary floating point는 십진수 대부분을 유한하게 정확히 표현할 수 없다

### 시작 전 용어집

#### 1. binary floating

- **뜻:** 1 같은 단순 십진 fraction도 2진수로는 무한 반복 표현이 될 수 있어 finite floating representation에서는 가장 가까운 근삿값으로 저장된다.
- **왜 중요한가:** 화면에 출력할 때 formatting이 반올림해 0.
- **예시:** 1 같은 단순 십진 fraction도 2진수로는 무한 반복 …

#### 2. point

- **뜻:** 3처럼 보여도 내부 equality가 같다는 뜻은 아니다.
- **왜 중요한가:** 계산의 중간 단계에서 작은 오차가 누적될 수 있고 뺄셈 cancellation으로 상대 오차가 커질 수 있다.
- **예시:** 3처럼 보여도 내부 equality가 같다는 뜻은 아니다.

#### 3. 반복

- **뜻:** Scientific 계산에서는 이 근사 모델이 표준적이고 효율적일 수 있다.
- **왜 중요한가:** 중요한 것은 exact equality가 필요한지, 어느 tolerance까지 허용할지를 알고 비교하는 것이다.
- **예시:** Scientific 계산에서는 이 근사 모델이 표준적이고 효율적일 수 …

#### 4. 오류

- **뜻:** 모든 float를 Decimal로 바꾸는 것이 자동으로 더 정확하고 빠른 해법은 아니다.
- **왜 중요한가:** 숫자 algorithm을 설계할 때 input scale과 condition을 확인한다.
- **예시:** 모든 float를 Decimal로 바꾸는 것이 자동으로 더 정확하고 …

0. `0.1 + 0.2` 결과가 정확한 0.3과 내부적으로 같지 않을 수 있는 이유다. 이는 계산기 오류가 아니라 표현 모델의 특성이다.

 

  

 매우 큰 값과 매우 작은 값을 섞는 합계, 거의 같은 두 값을 빼는 계산은 precision loss가 두드러질 수 있다.

---

## CHAPTER 03 · float equality는 tolerance의 의미를 문제 영역에서 정한다

### 시작 전 용어집

#### 1. float equality

- **뜻:** 두 float가 “충분히 같은가”를 판단할 때 절대 오차와 상대 오차를 함께 고려할 수 있다.
- **왜 중요한가:** 값이 1e9 규모일 때 1e-9 절대 tolerance는 지나치게 엄격하고, 0 근처에서는 상대 tolerance만 사용하면 의미가 없을 수 있다.
- **예시:** 두 float가 “충분히 같은가”를 판단할 때 절대 오차와 …

#### 2. tolerance

- **뜻:** 1인 값과 numerical algorithm 검증은 다른 tolerance를 가진다.
- **왜 중요한가:** Money처럼 1원 단위까지 정확히 같아야 하는 domain에서는 float close comparison 자체가 잘못된 모델일 수 있다.
- **예시:** 1인 값과 numerical algorithm 검증은 다른 tolerance를 가진다.

#### 3. 함수

- **뜻:** Library의 close comparison 함수는 이런 기준을 구현하지만 parameter를 무조건 default로 쓰면 domain 요구와 어긋날 수 있다.
- **왜 중요한가:** Integer minor unit이나 Decimal을 사용해 exact business rule을 표현한다.
- **예시:** Library의 close comparison 함수는 이런 기준을 구현하지만 parameter를 …

#### 4. parameter

- **뜻:** Test에서도 expected float를 literal equality로 비교하기 전에 algorithm의 허용 오차를 정의한다.
- **왜 중요한가:** Tolerance를 너무 넓게 주면 큰 오류까지 통과하므로 physical/business 의미에서 최대 허용 오차를 도출한다.
- **예시:** Test에서도 expected float를 literal equality로 비교하기 전에 algorithm의 …

센서 오차가 ±0.

 

 

---

## CHAPTER 04 · Decimal은 십진 표현과 rounding context를 명시하지만 자동 회계 규칙은 아니다

### 시작 전 용어집

#### 1. decimal

- **뜻:** Decimal 타입을 사용한다고 세법과 회계 규칙이 자동 적용되는 것은 아니다.
- **왜 중요한가:** Decimal을 float에서 만들면 이미 float approximation을 십진으로 옮길 수 있다.
- **예시:** Decimal 타입을 사용한다고 세법과 회계 규칙이 자동 적용되는 …

#### 2. rounding context

- **뜻:** 1 같은 십진 fraction을 십진 arithmetic으로 표현할 수 있어 금액과 세율 계산에 유용하다.
- **왜 중요한가:** 그러나 precision, rounding mode, quantization을 어떻게 설정할지는 여전히 application contract다.
- **예시:** 1 같은 십진 fraction을 십진 arithmetic으로 표현할 수 …

#### 3. precision

- **뜻:** 통화별 최소 단위가 다르고 일부 금융 계산은 중간 precision과 최종 rounding 시점을 별도로 규정한다.
- **왜 중요한가:** Rounding은 마지막 출력 한 번만의 문제가 아닐 수 있다.
- **예시:** 통화별 최소 단위가 다르고 일부 금융 계산은 중간 …

#### 4. 타입

- **뜻:** 1"`을 정확히 처리하려면 문자열이나 정수 representation에서 직접 Decimal을 만든다.
- **왜 중요한가:** 0`이 값으로 같은지 scale이 의미 있는지 domain마다 다를 수 있다.
- **예시:** 1"`을 정확히 처리하려면 문자열이나 정수 representation에서 직접 Decimal을 …

Decimal 계열은 0.  

 외부 금액 문자열 `"0. Boundary conversion이 중요하다.

`10.00`과 `10. 

 각 line item에서 반올림한 합계와 전체 합계 후 한 번 반올림한 값이 다를 수 있다. 어느 방식이 계약인지 테스트로 고정한다.

---

## CHAPTER 05 · rounding mode는 .5에서 무엇을 할지 이상의 정책이다

### 시작 전 용어집

#### 1. rounding mode

- **뜻:** 사람에게 익숙한 “5 이상 올림” 외에도 ties-to-even, toward zero, floor, ceiling 같은 rounding mode가 있다.
- **왜 중요한가:** Binary float의 `round()` 결과를 십진 직관으로만 보면 표현 오차와 tie rule 때문에 예상이 달라질 수 있다.
- **예시:** 사람에게 익숙한 “5 이상 올림” 외에도 ties-to-even, toward …

#### 2. 함수

- **뜻:** 개발자가 임의로 가장 익숙한 함수를 사용하면 규정 차이만큼 돈이 틀린다.
- **왜 중요한가:** 요구사항에 rounding rule과 단위를 명시한다.
- **예시:** 개발자가 임의로 가장 익숙한 함수를 사용하면 규정 차이만큼 …

#### 3. 요구사항

- **뜻:** 세금과 금융 domain은 특정 법규나 기관 규칙에 따라 반올림 위치와 mode를 정할 수 있다.
- **왜 중요한가:** Negative number에서 floor와 truncation은 결과가 다르다.
- **예시:** 세금과 금융 domain은 특정 법규나 기관 규칙에 따라 …

#### 4. 경계

- **뜻:** Rounding test는 정확히 경계 주변 값을 포함한다.
- **왜 중요한가:** 0051처럼 tie 직전·tie·직후와 양수/음수를 함께 확인한다.
- **예시:** Rounding test는 정확히 경계 주변 값을 포함한다.

`-1.2`를 floor하면 더 작은 -2 방향이고 truncation toward zero는 -1이 될 수 있다. Discount/refund처럼 음수가 가능한 계산에서 중요하다.

 0.0049, 0.0050, 0.

---

## CHAPTER 06 · money는 숫자 하나가 아니라 amount와 currency의 쌍이다

### 시작 전 용어집

#### 1. money

- **뜻:** Money type은 amount와 currency를 함께 가져야 하며 서로 다른 currency를 단순 더하는 operation을 금지할 수 있다.
- **왜 중요한가:** Exchange rate conversion은 별도 operation이다.
- **예시:** `rate=1300` 숫자만 저장하면 방향과 freshness를 알 수 없다.

#### 2. amount

- **뜻:** 100이라는 amount만으로는 100원인지 100달러 센트인지 알 수 없다.
- **왜 중요한가:** Minor unit 정수 representation을 사용하면 일부 통화에서는 정확한 계산이 쉽지만 모든 통화가 항상 2 decimal digit이라는 가정은 틀릴 수 있다.
- **예시:** `rate=1300` 숫자만 저장하면 방향과 freshness를 알 수 없다.

#### 3. currency

- **뜻:** 통화별 exponent를 metadata로 관리한다.
- **왜 중요한가:** 금융상품에는 일반 cash minor unit보다 더 높은 precision이 필요할 수도 있다.
- **예시:** `rate=1300` 숫자만 저장하면 방향과 freshness를 알 수 없다.

#### 4. decimal

- **뜻:** Primitive decimal을 application 전체에 흩어놓는 것보다 오류 공간이 줄어든다.
- **왜 중요한가:** Exchange rate에는 기준 통화와 quote 통화, 시각, source가 의미 있다.
- **예시:** `rate=1300` 숫자만 저장하면 방향과 freshness를 알 수 없다.

`rate=1300` 숫자만 저장하면 방향과 freshness를 알 수 없다. Rate application과 rounding sequence를 contract로 정의한다.

Money object가 arithmetic invariant를 소유하면 `USD + KRW` 같은 잘못된 operation을 runtime에서 즉시 거부할 수 있다. 

---

## CHAPTER 07 · overflow가 사라져도 algorithmic growth는 남는다

### 시작 전 용어집

#### 1. overflow

- **뜻:** Python bigint는 overflow를 피하지만 공격자가 수백만 자리 integer를 입력하면 parsing과 arithmetic에 큰 CPU/memory를 사용할 수 있다.
- **왜 중요한가:** “숫자는 무제한”을 untrusted input contract로 허용하면 resource exhaustion이 가능하다.
- **예시:** Python bigint는 overflow를 피하지만 공격자가 수백만 자리 integer를 …

#### 2. algorithmic growth

- **뜻:** 입력 digit 수와 numeric range를 제한한다.
- **왜 중요한가:** Exponentiation은 작은 입력 표기로 매우 큰 결과를 만들 수 있다.
- **예시:** 입력 digit 수와 numeric range를 제한한다.

#### 3. 입력

- **뜻:** Calculator API가 `10**100000000`을 허용하면 결과 object 자체가 거대하다.
- **왜 중요한가:** Evaluation engine에는 operation budget과 result-size limit이 필요하다.
- **예시:** Calculator API가 `10**100000000`을 허용하면 결과 object 자체가 거대하다.

#### 4. resource

- **뜻:** Validation range는 business rule뿐 아니라 resource budget을 반영한다.
- **왜 중요한가:** 암호학과 modular arithmetic처럼 큰 정수를 정상적으로 사용하는 domain에서는 algorithm 선택이 성능과 side-channel에 영향을 줄 수 있다.
- **예시:** Validation range는 business rule뿐 아니라 resource budget을 반영한다.

직접 구현보다 검증된 cryptographic library를 사용한다.

자료형이 표현할 수 있는 범위와 application이 안전하게 처리할 수 있는 범위는 다르다. 

---

## CHAPTER 08 · aggregate 계산은 순서와 algorithm에 따라 오차가 달라질 수 있다

### 시작 전 용어집

#### 1. aggregate

- **뜻:** 많은 float를 합할 때 작은 값을 큰 누적합에 계속 더하면 작은 contribution이 precision 아래로 사라질 수 있다.
- **왜 중요한가:** 입력 순서를 바꾸거나 compensated summation algorithm을 사용하면 오차 특성이 달라질 수 있다.
- **예시:** 많은 float를 합할 때 작은 값을 큰 누적합에 …

#### 2. algorithm

- **뜻:** Standard library와 numerical library가 제공하는 안정된 algorithm을 우선 사용한다.
- **왜 중요한가:** Regression test가 bit exact 결과를 요구해야 하는지 tolerance를 허용해야 하는지는 domain에 따라 다르다.
- **예시:** Standard library와 numerical library가 제공하는 안정된 algorithm을 우선 …

#### 3. precision

- **뜻:** 통계 평균도 먼저 합계를 구해 나누는 단순 구현이 overflow/precision 문제를 가질 수 있는 다른 언어와 environment가 있다.
- **왜 중요한가:** 금융 ledger와 scientific model은 다른 reproducibility 기준을 가진다.
- **예시:** 통계 평균도 먼저 합계를 구해 나누는 단순 구현이 …

#### 4. 입력

- **뜻:** 병렬 aggregation은 데이터를 partition해 부분 합을 만든 뒤 결합하므로 순차 합과 bit-for-bit 동일하지 않을 수 있다.
- **왜 중요한가:** Floating addition은 수학적 실수 덧셈처럼 완전히 associative하지 않기 때문이다.
- **예시:** 병렬 aggregation은 데이터를 partition해 부분 합을 만든 뒤 …

Distributed analytics에서 재현성 요구와 performance를 함께 고려한다.

 

 

---

## CHAPTER 09 · NaN과 infinity는 ordinary number 직관을 깨뜨리는 특수 상태다

### 시작 전 용어집

#### 1. NaN

- **뜻:** IEEE floating point에는 NaN과 positive/negative infinity 같은 특수값이 존재할 수 있다.
- **왜 중요한가:** NaN은 자기 자신과 equality가 false가 되는 등 일반 숫자와 다른 비교 semantics를 가진다.
- **예시:** Sorting, set membership, validation에서 예상하지 못한 behavior를 만들 …

#### 2. infinity

- **뜻:** 외부 JSON이나 database가 NaN/Infinity를 지원하는지 표준과 구현이 다를 수 있다.
- **왜 중요한가:** Serialization boundary에서 허용 여부를 정한다.
- **예시:** Sorting, set membership, validation에서 예상하지 못한 behavior를 만들 …

#### 3. ordinary number

- **뜻:** Sorting, set membership, validation에서 예상하지 못한 behavior를 만들 수 있다.
- **왜 중요한가:** Metric pipeline에서는 missing data를 NaN으로 표현하는 관례가 있을 수 있지만 business amount에서는 거부해야 할 가능성이 높다.
- **예시:** `x >= 0` 검사 하나만으로 NaN을 걸러내지 못할 …

#### 4. 상태

- **뜻:** `x >= 0` 검사 하나만으로 NaN을 걸러내지 못할 수 있다.
- **왜 중요한가:** 숫자가 finite해야 한다는 contract가 있다면 명시적 finite check를 한다.
- **예시:** `x >= 0` 검사 하나만으로 NaN을 걸러내지 못할 …

Infinity도 range comparison에서 통과/실패 방식이 일반 큰 수와 다르다.

특수값은 오류라기보다 특정 계산 모델의 상태다. 허용할 domain과 금지할 domain을 구분한다.

---

## CHAPTER 10 · 수치 타입 선택은 정확도·범위·성능·상호운용성의 균형이다

### 시작 전 용어집

#### 1. 타입

- **뜻:** 하나의 “가장 정확한 타입”이 모든 문제에 적합하지 않다.
- **왜 중요한가:** 외부 system과 data exchange할 때는 상대 시스템의 숫자 범위와 precision을 고려한다.
- **예시:** 하나의 “가장 정확한 타입”이 모든 문제에 적합하지 않다.

#### 2. 계약

- **뜻:** 정확한 개수와 ID에는 integer, 일반적인 과학·그래픽 계산에는 float, 십진 규칙이 계약인 금액에는 Decimal이 적합할 수 있다.
- **왜 중요한가:** Fraction은 정확한 유리수 계산을 제공하지만 numerator/denominator가 커져 비용이 증가할 수 있다.
- **예시:** 정확한 개수와 ID에는 integer, 일반적인 과학·그래픽 계산에는 float, …

#### 3. decimal

- **뜻:** JSON number를 JavaScript consumer가 읽는다면 큰 integer ID를 string으로 보낼지 policy를 정한다.
- **왜 중요한가:** 성능 최적화로 타입을 바꾸기 전에 correctness budget을 고정한다.
- **예시:** JSON number를 JavaScript consumer가 읽는다면 큰 integer ID를 …

#### 4. precision

- **뜻:** Float32로 memory를 줄이는 대신 허용 오차를 넘지 않는지 실제 workload에서 측정한다.
- **왜 중요한가:** Vectorized library는 scalar Python과 다른 dtype overflow semantics를 가질 수 있다.
- **예시:** Float32로 memory를 줄이는 대신 허용 오차를 넘지 않는지 …

수치 프로그래밍의 핵심은 **숫자 모양을 보고 타입을 정하는 것이 아니라 값의 의미, 허용 오차, rounding, 범위, 외부 계약을 먼저 정하고 그 계약을 가장 잘 보존하는 표현을 선택하는 것**이다.
