# PART 41 · Equality·hash·ordering — 객체의 동일성 규칙을 collection과 정렬 계약으로 연결하기

객체를 비교한다는 말에는 여러 의미가 섞여 있다. 같은 object인지, 같은 값을 나타내는지, 같은 key로 취급해도 되는지, 어느 쪽이 먼저 와야 하는지는 서로 다른 계약이다. Python에서는 `is`, `==`, hash, ordering protocol이 이 차이를 표현한다. 이 규칙을 임의로 정의하면 dict·set·sort가 서로 모순된 세계관을 가지므로 **identity·equality·hash·order가 어떤 domain 기준을 공유하는지**를 먼저 고정해야 한다.

---

## CHAPTER 01 · identity와 equality는 질문 자체가 다르다

`a is b`는 두 이름이 같은 객체를 가리키는지 묻고 `a == b`는 타입이 정의한 값 동등성 규칙을 적용한다. 서로 다른 두 `Money(1000, "KRW")` 객체가 값으로 같을 수 있지만 identity는 다르다. 반대로 같은 mutable list를 두 이름이 가리키면 identity는 같고 내용도 현재는 같지만 이후 mutation이 두 이름 모두에서 보인다.

Domain entity에서는 equality를 어떤 기준으로 정의할지 신중해야 한다. Database ID가 같은 두 User snapshot을 같은 entity로 볼지, 모든 field가 같아야 값으로 같다고 볼지에 따라 cache와 test 의미가 달라진다. Entity identity와 value object equality를 같은 규칙으로 만들지 않는다.

`None`처럼 singleton semantics가 중요한 값은 identity 검사에 적합하지만 문자열·정수 값 비교를 object reuse 같은 구현 우연에 의존해서는 안 된다. 언어가 보장하는 관계와 runtime optimization을 구분한다.

---

## CHAPTER 02 · `__eq__`는 symmetric하고 예측 가능한 동등성 관계를 목표로 한다

Equality는 일반적으로 reflexive, symmetric, transitive한 관계를 기대하지만 floating NaN처럼 예외적인 수학 모델도 존재한다. 사용자 정의 object는 가능한 한 호출자가 기대하는 동등성 규칙을 유지해야 collection과 test가 예측 가능하다. `a == b`와 `b == a`가 서로 다른 결과를 내는 type은 조합하기 어렵다.

다른 타입과 비교할 수 없을 때 무조건 `False`를 반환할지 `NotImplemented`를 사용해 상대 operand의 비교 기회를 줄지 Python data model을 따라야 한다. 이 distinction은 mixed-type comparison과 subclass behavior에 영향을 준다.

Equality method 안에서 network나 database 조회 같은 side effect를 수행하면 set membership과 list comparison처럼 평범한 연산이 I/O를 일으킬 수 있다. 동등성은 빠르고 안정적인 value relation으로 유지하는 편이 좋다.

---

## CHAPTER 03 · equality가 같으면 hash도 같은 결과를 가져야 한다

Hash table은 hash로 후보 bucket을 찾고 equality로 실제 key인지 확인한다. 따라서 `a == b`가 참인데 `hash(a) != hash(b)`라면 같은 logical key가 서로 다른 위치로 가 dict/set contract가 깨진다. 사용자 정의 hash는 equality에 참여하는 immutable field에서 계산한다.

Mutable field가 hash에 들어가면 object를 dict key에 넣은 뒤 field를 바꿨을 때 원래 bucket에서 찾지 못할 수 있다. 그래서 mutable value object에 hash를 제공하지 않거나 key identity를 바뀌지 않는 ID로 제한한다.

Hash collision은 서로 다른 key에서도 정상적으로 일어날 수 있다. Hash가 같다고 equality까지 같다는 뜻은 아니다. Application에서 hash value를 unique ID처럼 사용하면 collision 때문에 잘못된 identity가 생길 수 있다.

---

## CHAPTER 04 · dataclass의 자동 equality와 hash는 domain semantics와 맞는지 확인한다

Dataclass는 field 기반 equality를 자동 생성할 수 있어 value object에 편리하다. 하지만 모든 field가 domain equality에 참여해야 하는 것은 아니다. Cache field, last_updated metadata처럼 object behavior에는 필요하지만 logical value identity와 무관한 field를 비교에 넣으면 같은 값이 다르게 취급될 수 있다.

Frozen dataclass는 hashable value object를 만들기 쉬워 보이지만 nested mutable field가 있으면 deep immutability가 자동 보장되는 것은 아니다. Tuple field 안에 mutable object가 들어 있거나 external object를 reference하면 lifetime과 mutation을 별도로 봐야 한다.

Entity는 자동 field equality보다 explicit ID equality가 적합할 수 있다. Dataclass option을 선택할 때 생성되는 method가 실제 domain contract와 맞는지 검토한다.

---

## CHAPTER 05 · total ordering은 일부 비교만 정의해도 전체 순서를 요구할 수 있다

정렬 가능한 type은 `<`, `<=`, `>`, `>=` 관계가 서로 모순되지 않아야 한다. Price object를 amount만으로 비교하는데 currency가 다르면 순서 자체가 의미 없을 수 있다. 이때 임의로 currency code까지 비교해 total order를 만드는 것보다 mixed-currency comparison을 거부하는 것이 domain에 더 정확할 수 있다.

`functools.total_ordering` 같은 helper는 일부 method에서 나머지를 생성할 수 있지만 잘못된 핵심 비교 규칙을 고쳐 주지는 않는다. Equality와 less-than이 일관되어야 정렬과 bisect가 예상대로 동작한다.

Partial order인 문제를 total order처럼 강제하면 business 의미가 왜곡될 수 있다. Dependency graph의 node나 set inclusion처럼 일부 pair만 비교 가능한 관계도 있다.

---

## CHAPTER 06 · 정렬 key는 객체 자체의 ordering과 presentation ordering을 분리한다

모든 User object에 하나의 자연스러운 `<`를 정의할 필요는 없다. 화면에서는 이름순, 보고서에서는 가입일순, admin 도구에서는 ID순으로 정렬할 수 있다. `sorted(users, key=...)`는 object의 intrinsic comparison과 use-case-specific ordering policy를 분리한다.

Key function은 각 element에서 비교 가능한 값 하나를 추출하므로 expensive operation을 넣으면 정렬 비용이 커진다. Python sort가 key를 한 번 계산해 보관하는 방식의 장점을 이용하되 key 계산에 I/O를 넣지 않는다.

복합 key tuple은 여러 기준을 lexicographic order로 표현할 수 있다. Null/None placement와 descending field가 섞이면 명시적인 normalized key를 만들어 정책을 읽을 수 있게 한다.

---

## CHAPTER 07 · stable sort는 동일 key 항목의 이전 상대 순서를 보존한다

Stable sort는 같은 key를 가진 항목끼리 원래 순서를 유지한다. 이 성질을 이용하면 secondary key로 먼저 정렬한 뒤 primary key로 정렬하는 multi-pass 전략이 가능하다. 그러나 한 번의 composite key가 더 직접적인 경우도 있으므로 의도를 기준으로 선택한다.

Stability는 UI에서 사용자가 이전 ordering을 유지한 채 새로운 group 기준을 적용할 때도 의미가 있다. 같은 점수 사용자가 입력 순서를 유지해야 하는지 random tie-break가 필요한지 정책을 정한다.

Stable이라고 해서 결과가 deterministic하다는 뜻은 아니다. 원래 input order가 set이나 external concurrent source에서 비결정적이면 동일 key 항목 순서도 달라질 수 있다.

---

## CHAPTER 08 · dict/set key contract는 serialization과 cache identity에도 연결된다

같은 object를 dict key와 cache key, serialized ID로 사용할 때 각 identity 규칙이 다르면 문제가 생긴다. Python hash는 process 간 영구 식별자로 쓰기 위한 값이 아니며 문자열 hash randomization 같은 동작도 있을 수 있다. Persistent cache나 wire key에는 stable canonical representation이 필요하다.

Set에 object를 넣는 것은 “이 equality/hash 규칙으로 unique membership을 판단하겠다”는 결정이다. Domain에서 중복 기준이 다른 여러 view가 필요하면 object 자체 hash를 바꾸기보다 별도 key selector를 사용한다.

Database unique constraint와 Python set uniqueness도 같은 field를 기준으로 하는지 확인한다. Application에서 중복이 아니라고 판단했는데 DB가 거부하면 identity rule이 계층마다 다르다는 증거다.

---

## CHAPTER 09 · 비교 protocol은 예상보다 많은 library operation에서 호출된다

`in`, set operation, dict lookup, sorting, heap, bisect, dataclass comparison이 equality나 ordering protocol을 간접적으로 호출할 수 있다. 비교 method 안에 logging이나 mutation을 넣으면 library algorithm이 몇 번 비교하는지에 따라 side effect 횟수가 달라진다.

Comparator 호출 횟수에 correctness를 의존하지 않는다. Sort algorithm implementation이 바뀌거나 input distribution이 달라지면 비교 횟수와 순서가 달라질 수 있다. Comparison은 가능한 한 pure relation으로 설계한다.

성능 issue가 있을 때 `__eq__`가 큰 nested structure 전체를 매번 비교하는지 profile한다. Stable ID가 있다면 빠른 early check를 사용할 수 있지만 hash collision이나 ID reuse semantics를 조심한다.

---

## CHAPTER 10 · equality 설계는 “같음”을 어디까지 공유할지 정하는 모델링 작업이다

객체를 만들 때 equality와 hash를 자동으로 추가하기 전에 domain에서 같은 것의 의미를 정의한다. Value object는 값 전체, entity는 stable identity, view model은 presentation key처럼 서로 다른 기준을 가질 수 있다. 하나의 class가 모든 의미를 떠맡으면 collection과 persistence에서 모순이 생긴다.

Contract test는 equality symmetry, equal→same hash, mutation 후 key stability, sort ordering을 검증할 수 있다. Boundary value와 mixed-type comparison도 포함한다.

Equality·hash·ordering의 핵심은 operator를 구현하는 법이 아니라 **동일성과 순서를 하나의 일관된 모델로 만들어 Python collection과 domain rule이 같은 세계를 보게 하는 것**이다.