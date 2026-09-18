# PART 12 · Test와 correctness — 예제가 맞는지보다 계약이 깨지는 위치를 찾기

테스트는 코드를 한 번 실행해 보는 활동과 다르다. 프로그램이 약속한 계약을 어떤 입력 공간에서 검증할지 설계하고, 실패했을 때 원인을 좁힐 수 있는 증거를 남기는 체계다. 정상 예제만 반복하면 이미 예상한 경로만 확인하게 된다. 강한 테스트는 경계값, 상태 전이, 실패 원자성, 외부 의존성, 성능 한계를 서로 다른 층에서 검증한다.

---

## CHAPTER 01 · test case는 입력과 기대값이 아니라 계약의 한 주장이다

### 시작 전 용어집

#### 1. test case

- **뜻:** `assert add(2, 3) == 5`는 단순 계산 예제처럼 보이지만 실제로는 특정 계약의 한 점을 검증한다.
- **왜 중요한가:** 함수가 commutative인지, overflow가 없는지, 잘못된 입력을 어떻게 거부하는지 같은 다른 속성은 이 한 사례로 증명되지 않는다.
- **예시:** Test를 설계할 때 먼저 “이 테스트가 어떤 규칙을 …

#### 2. 입력

- **뜻:** 어떤 입력을 통제하고 무엇을 관측하는지 선명할수록 실패가 정보가 된다.
- **왜 중요한가:** Test를 설계할 때 먼저 “이 테스트가 어떤 규칙을 깨뜨리면 실패해야 하는가”를 문장으로 정의하면 중복되는 예제 수를 줄이고 coverage의 의미를 높일 수 있다.
- **예시:** 어떤 입력을 통제하고 무엇을 관측하는지 선명할수록 실패가 정보가 …

#### 3. 계약

- **뜻:** Test는 “코드가 실행된다”가 아니라 계약의 특정 속성을 관찰하는 실험이다.
- **왜 중요한가:** 좋은 test name은 실행 절차보다 behavior를 설명한다.
- **예시:** Test는 “코드가 실행된다”가 아니라 계약의 특정 속성을 관찰하는 …

#### 4. 검증

- **뜻:** 복잡한 알고리즘은 서로 다른 구현이나 invariant로 교차 검증할 수 있다.
- **왜 중요한가:** `test_checkout_1`보다 `test_checkout_rejects_expired_coupon_without_changing_total`이 실패 이유를 더 빠르게 알려 준다.
- **예시:** 복잡한 알고리즘은 서로 다른 구현이나 invariant로 교차 검증할 …

테스트를 문서처럼 읽을 수 있으면 요구사항과 구현 사이의 추적 가능성도 높아진다.

기대값은 구현 코드의 계산식을 그대로 복사해 만들면 같은 오류를 두 곳에서 반복할 수 있다. 계산 규칙이 단순하면 독립적으로 손으로 정한 값, domain table, known reference를 사용한다. 

 

---

## CHAPTER 02 · equivalence partition과 boundary value로 입력 공간을 압축한다

### 시작 전 용어집

#### 1. equivalence partition

- **뜻:** 가능한 입력을 모두 테스트하는 것은 대부분 불가능하다.
- **왜 중요한가:** 대신 같은 규칙이 적용될 것으로 기대되는 입력들을 equivalence class로 묶고 각 class에서 대표값을 고른다.
- **예시:** 같은 branch를 통과하는 100개 정상 예제보다 서로 다른 …

#### 2. boundary value

- **뜻:** 64, 65 이상으로 나뉜다면 각 구간의 아무 값만 고르는 것보다 경계 바로 전·정확한 경계·직후를 함께 선택하면 비교 연산 오류를 잘 잡을 수 있다.
- **왜 중요한가:** 범위 `[1, 999]`라면 `0,1,2,998,999,1000` 같은 값이 핵심이다.
- **예시:** 같은 branch를 통과하는 100개 정상 예제보다 서로 다른 …

#### 3. 입력

- **뜻:** 입력 공간을 구조적으로 나누면 test 수가 늘어나는 것이 아니라 필요한 차이를 보존하면서 불필요한 반복을 줄인다.
- **왜 중요한가:** 같은 branch를 통과하는 100개 정상 예제보다 서로 다른 규칙 경계 10개가 더 높은 오류 탐지력을 가질 수 있다.
- **예시:** 입력 공간을 구조적으로 나누면 test 수가 늘어나는 것이 …

#### 4. class

- **뜻:** Collection이라면 empty, one element, many elements, duplicate, already sorted, reverse order가 서로 다른 behavior class가 될 수 있다.
- **왜 중요한가:** 문자열에서는 empty, whitespace only, Unicode, invalid encoding boundary가 중요할 수 있다.
- **예시:** Collection이라면 empty, one element, many elements, duplicate, already …

나이 규칙이 0..18, 19..

  

경계 테스트는 숫자에만 해당하지 않는다. State machine에서는 허용 전이와 금지 전이의 경계, permission에서는 owner/non-owner, resource에서는 quota 직전과 초과, timeout에서는 deadline 직전과 직후가 경계다. Domain rule의 분기점이 곧 test boundary다.

 

---

## CHAPTER 03 · Arrange–Act–Assert는 원인과 관측을 분리한다

### 시작 전 용어집

#### 1. Arrange

- **뜻:** 많은 unit test는 준비(Arrange), 대상 동작 실행(Act), 결과 검증(Assert)의 세 구간으로 설명할 수 있다.
- **왜 중요한가:** 이 구조의 목적은 형식적으로 빈 줄을 넣는 것이 아니라 실패했을 때 “입력 구성 문제인지, 대상 동작인지, 결과 해석인지”를 좁히는 것이다.
- **예시:** 한 test에서 create→edit→delete→recreate를 모두 수행하면 중간 어느 단계가 …

#### 2. Act

- **뜻:** Factory와 builder를 사용하더라도 default가 test의 핵심 조건을 숨기지 않게 한다.
- **왜 중요한가:** Act는 가능하면 핵심 operation 하나에 집중한다.
- **예시:** 한 test에서 create→edit→delete→recreate를 모두 수행하면 중간 어느 단계가 …

#### 3. Assert

- **뜻:** Assert에서는 최종 return뿐 아니라 필요한 side effect와 금지된 변화도 본다.
- **왜 중요한가:** 결제 실패 후 balance가 그대로인지, exception type과 state rollback이 모두 맞는지 확인해야 실패 원자성을 검증한 것이다.
- **예시:** 한 test에서 create→edit→delete→recreate를 모두 수행하면 중간 어느 단계가 …

#### 4. 검증

- **뜻:** 반면 state transition sequence 자체가 계약이라면 여러 동작을 하나의 시나리오로 검증할 이유가 있다.
- **왜 중요한가:** 분리 여부는 테스트가 주장하는 behavior 단위로 결정한다.
- **예시:** 한 test에서 create→edit→delete→recreate를 모두 수행하면 중간 어느 단계가 …

Arrange 단계가 너무 길고 복잡하면 test fixture 자체에 버그가 생길 수 있다. 거대한 실제 application context를 만들기보다 해당 unit이 정말 필요한 dependency와 상태만 준비한다. 

 한 test에서 create→edit→delete→recreate를 모두 수행하면 중간 어느 단계가 잘못됐는지 찾기 어렵다.  

 

---

## CHAPTER 04 · unit test는 작은 함수가 아니라 작은 behavior boundary를 격리한다

### 시작 전 용어집

#### 1. unit test

- **뜻:** “함수 하나당 unit test 하나”는 unit의 좋은 정의가 아니다.
- **왜 중요한가:** Unit은 외부 dependency와 분리해 빠르고 결정적으로 검증할 수 있는 behavior 경계다.
- **예시:** “함수 하나당 unit test 하나”는 unit의 좋은 정의가 …

#### 2. 함수

- **뜻:** 여러 작은 helper가 함께 하나의 가격 정책을 구현한다면 public policy function을 통해 검증하는 편이 내부 구조 변경에 덜 취약할 수 있다.
- **왜 중요한가:** Private helper를 직접 test하면 리팩터링 때 behavior는 그대로인데 test가 대량으로 깨질 수 있다.
- **예시:** 여러 작은 helper가 함께 하나의 가격 정책을 구현한다면 …

#### 3. behavior boundary

- **뜻:** Public contract를 중심으로 검증하고 복잡한 내부 algorithm만 별도 unit으로 추출할 가치가 있을 때 직접 다룬다.
- **왜 중요한가:** Test가 구현 구조를 복제할수록 변경 비용이 커진다.
- **예시:** Public contract를 중심으로 검증하고 복잡한 내부 algorithm만 별도 …

#### 4. dependency

- **뜻:** 외부 clock, random, network, filesystem은 unit을 비결정적으로 만들 수 있으므로 dependency를 명시적으로 주입하거나 fake를 사용한다.
- **왜 중요한가:** 중요한 것은 모든 것을 mock으로 바꾸는 것이 아니라 테스트하려는 규칙 외의 변동 요인을 통제하는 것이다.
- **예시:** 외부 clock, random, network, filesystem은 unit을 비결정적으로 만들 …

Unit test suite는 빠르게 반복할 수 있어야 개발 중 자주 실행된다. 느린 database와 network test를 같은 층에 모두 넣으면 feedback 시간이 길어져 실제 실행 빈도가 줄어든다. 검증 속도 자체가 품질 시스템의 설계 요소다.

---

## CHAPTER 05 · fake, stub, mock은 목적이 다르며 interaction test를 남용하지 않는다

### 시작 전 용어집

#### 1. fake

- **뜻:** Stub은 미리 정한 응답을 제공하고, fake는 단순하지만 실제 동작하는 구현을 가질 수 있으며, mock은 호출 횟수와 argument 같은 interaction을 검증하는 데 쓰인다.
- **왜 중요한가:** 용어보다 중요한 것은 **무엇을 관측하려고 대체했는가**다.
- **예시:** Stub은 미리 정한 응답을 제공하고, fake는 단순하지만 실제 …

#### 2. stub

- **뜻:** Test double은 실제 dependency를 대체하지만 모두 같은 역할은 아니다.
- **왜 중요한가:** 결과 behavior가 중요하다면 fake repository를 사용해 최종 상태를 검증하는 것이 구현 호출 순서를 하나하나 mock으로 고정하는 것보다 리팩터링에 강할 수 있다.
- **예시:** Test double은 실제 dependency를 대체하지만 모두 같은 역할은 …

#### 3. mock

- **뜻:** Mock이 너무 많아지면 test가 production object graph의 복제본이 된다.
- **왜 중요한가:** Method 하나 이름이 바뀔 때 behavior와 상관없는 수십 test가 깨질 수 있다.
- **예시:** Mock이 너무 많아지면 test가 production object graph의 복제본이 …

#### 4. interaction test

- **뜻:** 반대로 “결제 승인 실패 시 배송 API를 절대 호출하지 않는다”처럼 호출 자체가 계약인 경우 interaction verification이 의미 있다.
- **왜 중요한가:** Dependency interface를 작게 만들고 public outcome을 우선 검증하면 이런 coupling을 줄일 수 있다.
- **예시:** 반대로 “결제 승인 실패 시 배송 API를 절대 …

Fake도 실제 시스템과 의미가 달라질 수 있다. In-memory repository가 transaction, unique constraint, concurrency를 재현하지 못한다면 integration test에서 실제 database behavior를 별도로 확인해야 한다. Test double은 특정 failure class를 격리하는 도구이지 현실 전체를 대신하지 않는다.

---

## CHAPTER 06 · integration test는 경계 사이의 계약 불일치를 찾는다

### 시작 전 용어집

#### 1. integration test

- **뜻:** Integration test는 여러 component가 실제 경계를 통해 연결될 때 계약이 일치하는지 검증한다.
- **왜 중요한가:** Unit test보다 느리고 setup이 크지만 다른 종류의 오류를 잡는다.
- **예시:** Integration test는 여러 component가 실제 경계를 통해 연결될 …

#### 2. 경계

- **뜻:** 모든 production service를 매번 띄울 필요는 없지만 위험한 경계는 실제 구현을 사용한다.
- **왜 중요한가:** 한 test가 남긴 row가 다음 test 결과를 바꾸면 실행 순서에 민감해진다.
- **예시:** 모든 production service를 매번 띄울 필요는 없지만 위험한 …

#### 3. 계약

- **뜻:** 각 unit이 독립적으로 맞아도 serializer field name, database schema, HTTP status, timezone, transaction 설정이 맞지 않으면 실제 system은 실패한다.
- **왜 중요한가:** Database integration에서는 실제 migration을 적용한 schema에서 query와 constraint를 확인한다.
- **예시:** 각 unit이 독립적으로 맞아도 serializer field name, database …

#### 4. HTTP

- **뜻:** HTTP integration에서는 framework routing, serialization, authentication middleware가 함께 동작하는지 본다.
- **왜 중요한가:** File test에서는 실제 encoding, path, permission behavior가 중요할 수 있다.
- **예시:** HTTP integration에서는 framework routing, serialization, authentication middleware가 함께 …

Integration environment가 production과 너무 다르면 false confidence가 생긴다. Embedded database가 production DB와 SQL semantics가 다르다면 중요한 query는 실제 engine과 호환되는 환경에서 검증해야 한다. 

Test data isolation도 중요하다.  Transaction rollback, unique namespace, disposable resource 같은 전략으로 각 test의 초기 상태를 통제한다.

---

## CHAPTER 07 · property-based testing은 예제보다 invariant를 생성된 입력에 적용한다

### 시작 전 용어집

#### 1. property-based testing

- **뜻:** 어떤 함수의 중요한 성질이 많은 입력에 대해 동일하다면 개별 expected value를 수십 개 적는 대신 property를 정의할 수 있다.
- **왜 중요한가:** 정렬 결과는 입력과 같은 multiset을 가지며 non-decreasing order여야 한다, encode 후 decode하면 원래 값과 동등해야 한다, normalization을 두 번 적용해도 한 번 적용한 결과와 같아야 한다 같은 속성이다.
- **예시:** 예제 테스트와 property test는 경쟁하지 않는다.

#### 2. invariant

- **뜻:** Property-based 도구는 다양한 입력을 생성해 invariant를 깨는 counterexample을 찾는다.
- **왜 중요한가:** 사람이 생각하지 못한 빈 문자열, 매우 큰 수, 특수 Unicode, 이상한 조합을 발견할 가능성이 높다.
- **예시:** 예제 테스트와 property test는 경쟁하지 않는다.

#### 3. 입력

- **뜻:** 실패 입력을 더 작은 사례로 shrink해 원인을 보여 주는 기능도 강점이다.
- **왜 중요한가:** Property 자체가 잘못되면 test도 잘못된다.
- **예시:** 예제 테스트와 property test는 경쟁하지 않는다.

#### 4. 함수

- **뜻:** 모든 serialize/deserialize가 byte-for-byte 원본을 복구해야 하는지, semantic equality면 되는지 계약을 먼저 정한다.
- **왜 중요한가:** Random generation만 많이 한다고 품질이 생기는 것이 아니라 어떤 invariant를 검사하는지가 핵심이다.
- **예시:** 예제 테스트와 property test는 경쟁하지 않는다.

예제 테스트와 property test는 경쟁하지 않는다. 업무적으로 중요한 대표 사례는 읽기 쉬운 example test로 남기고, 넓은 값 공간의 수학적 속성은 property test로 보완한다.

---

## CHAPTER 08 · stateful testing은 operation sequence 전체의 invariant를 검증한다

### 시작 전 용어집

#### 1. stateful testing

- **뜻:** 단일 함수 입력만으로는 주문, 계좌, cache처럼 상태가 누적되는 시스템의 오류를 충분히 찾기 어렵다.
- **왜 중요한가:** `create → pay → cancel`, `put → get → delete`, 여러 deposit/withdraw sequence처럼 operation 순서가 behavior를 결정한다.
- **예시:** Operation sequence를 자동 생성하면 사람이 예상하지 못한 순서에서 …

#### 2. operation sequence

- **뜻:** Stateful test는 상태 모델과 실제 implementation을 같은 operation sequence로 움직여 invariant를 비교할 수 있다.
- **왜 중요한가:** 상태 머신에서 허용되지 않은 transition이 거부되는지, 거부 뒤 state가 유지되는지, 같은 idempotency key를 반복했을 때 결과가 중복되지 않는지 확인한다.
- **예시:** Operation sequence를 자동 생성하면 사람이 예상하지 못한 순서에서 …

#### 3. invariant

- **뜻:** 모든 interleaving을 현실적으로 검사하기 어렵지만 critical section과 atomicity invariant를 중심으로 stress test와 model-based test를 설계할 수 있다.
- **왜 중요한가:** Stateful test의 reference model은 production 구현보다 단순해야 한다.
- **예시:** Operation sequence를 자동 생성하면 사람이 예상하지 못한 순서에서 …

#### 4. 검증

- **뜻:** Operation sequence를 자동 생성하면 사람이 예상하지 못한 순서에서 버그를 찾을 수 있다.
- **왜 중요한가:** 동시성 시스템에서는 같은 logical operations도 interleaving에 따라 결과가 달라질 수 있다.
- **예시:** Operation sequence를 자동 생성하면 사람이 예상하지 못한 순서에서 …

같은 algorithm을 그대로 복제하면 동일한 오류를 공유한다. 느리더라도 명확한 model을 사용해 구현의 최적화 결과와 비교하는 방식이 더 강하다.

---

## CHAPTER 09 · flaky test는 제품 오류와 검증 시스템 오류를 구분하지 못하게 한다

### 시작 전 용어집

#### 1. flaky test

- **뜻:** Flaky test를 단순히 재실행해 통과시키는 습관은 실제 race나 운영 문제를 숨길 수 있다.
- **왜 중요한가:** 시간 기반 test에서는 real sleep보다 controllable clock을 사용하고, random test는 seed를 기록해 재현 가능하게 한다.
- **예시:** Flaky test를 단순히 재실행해 통과시키는 습관은 실제 race나 …

#### 2. 오류

- **뜻:** 같은 commit과 같은 입력에서 test가 때로 성공하고 때로 실패하면 실패 신호의 신뢰도가 낮아진다.
- **왜 중요한가:** 원인은 현재 시각, random seed, thread scheduling, network, test 간 공유 상태, timeout margin, resource leak 등 다양하다.
- **예시:** 같은 commit과 같은 입력에서 test가 때로 성공하고 때로 …

#### 3. 검증

- **뜻:** 검증 시스템이 신뢰를 잃으면 실제 regression 탐지력도 같이 떨어진다.
- **왜 중요한가:** Async test는 “조금 기다리면 되겠지”라는 sleep 대신 실제 completion signal을 기다린다.
- **예시:** 검증 시스템이 신뢰를 잃으면 실제 regression 탐지력도 같이 …

#### 4. 입력

- **뜻:** Network dependency는 integration 목적이 아니라면 deterministic fake로 격리한다.
- **왜 중요한가:** Order-dependent failure를 찾으려면 test 순서를 바꾸어 실행하거나 개별 실행과 suite 실행을 비교한다.
- **예시:** Network dependency는 integration 목적이 아니라면 deterministic fake로 격리한다.

Global cache, singleton, environment variable을 test가 원래 상태로 복구하지 않는 경우가 흔하다. Resource cleanup을 fixture lifetime과 함께 관리한다.

Flaky test도 하나의 defect다. 실패율이 낮다는 이유로 방치하면 나중에 모든 실패를 “또 flaky겠지”라고 무시하게 된다. 

---

## CHAPTER 10 · coverage 숫자는 실행 경로의 양을 말할 뿐 correctness를 증명하지 않는다

### 시작 전 용어집

#### 1. coverage

- **뜻:** Line coverage 100%는 모든 줄이 한 번 실행됐다는 뜻에 가깝고 모든 조건 조합과 업무 invariant가 검증됐다는 뜻은 아니다.
- **왜 중요한가:** `if a and b:` 한 줄을 한 번 통과해도 `a=false`, `b=false`, short-circuit path가 모두 검증된 것은 아니다.
- **예시:** Line coverage 100%는 모든 줄이 한 번 실행됐다는 …

#### 2. correctness

- **뜻:** Branch coverage가 더 많은 경로를 보여 주지만 여전히 assertion 품질을 대신하지 못한다.
- **왜 중요한가:** Coverage는 “아무 테스트도 지나가지 않는 중요한 코드”를 찾는 탐색 도구로 유용하다.
- **예시:** Branch coverage가 더 많은 경로를 보여 주지만 여전히 …

#### 3. 조건

- **뜻:** Mutation testing처럼 코드의 조건이나 연산을 일부 바꿨을 때 suite가 실제로 실패하는지 보는 접근은 assertion 강도를 평가하는 데 도움이 된다.
- **왜 중요한가:** 테스트가 실행은 되지만 결과를 거의 확인하지 않는 경우 mutation이 살아남는다.
- **예시:** Mutation testing처럼 코드의 조건이나 연산을 일부 바꿨을 때 …

#### 4. 검증

- **뜻:** 중요한 것은 coverage보다 **결함이 들어왔을 때 검증층이 그것을 감지하는가**다.
- **왜 중요한가:** 검증 전략은 unit, integration, property/stateful, 정적 분석, runtime observability를 서로 다른 failure class에 배치한다.
- **예시:** 중요한 것은 coverage보다 **결함이 들어왔을 때 검증층이 그것을 …

돈, 권한, migration, recovery 같은 위험한 코드가 전혀 실행되지 않았다면 테스트 설계를 보강해야 한다. 반대로 trivial getter까지 숫자를 올리기 위해 test를 쓰는 것은 유지 비용만 늘릴 수 있다.

  

 같은 test를 반복하는 대신 각각이 다른 종류의 오류를 잡도록 설계하는 것이 전체 시스템의 CLEAN PASS 원리와도 연결된다.
