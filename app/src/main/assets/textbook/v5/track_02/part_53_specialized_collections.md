# PART 53 · Specialized collections — 자료구조의 의미를 더 정확한 container로 표현하기

기본 `list`, `dict`, `set`만으로 대부분의 프로그램을 만들 수 있지만 workload와 불변조건이 더 구체해지면 specialized collection이 문제의 의미를 더 직접 표현한다. 양끝 queue, multiset, default-valued mapping, layered configuration, named record는 각각 다른 관계를 구조 자체에 담는다. 핵심은 편리한 method를 더 많이 쓰는 것이 아니라 **자주 수행하는 연산과 금지하고 싶은 잘못된 상태를 container 선택으로 드러내는 것**이다.

---

## CHAPTER 01 · `deque`는 양끝 삽입·삭제가 중심인 workload를 표현한다

일반 list는 끝에 append하고 index로 접근하는 데 강하지만 맨 앞에서 반복적으로 pop/insert하면 많은 요소 이동이 필요할 수 있다. `deque`는 양끝에서 값을 넣고 빼는 queue/stack workload를 위해 설계되어 sliding window, breadth-first traversal, producer-consumer buffer에 자연스럽다.

자료구조를 선택할 때 “속도가 빠르다”라는 일반론보다 어떤 연산이 hot path인지 본다. Random index access가 많다면 deque가 list를 자동 대체하지 않는다. 반대로 최근 N개 event를 앞뒤로 추가·제거하는 history라면 양끝 operation이 핵심이므로 deque가 의미와 비용을 동시에 맞출 수 있다.

Bounded `maxlen`을 사용하면 새 값이 들어올 때 오래된 값이 자동 제거되는 fixed-size history를 표현할 수 있다. 이때 eviction이 의도된 데이터 손실이라는 점을 contract로 남긴다. Audit log처럼 절대 잃으면 안 되는 데이터에는 bounded deque를 durable storage 대신 사용할 수 없다.

---

## CHAPTER 02 · `Counter`는 항목→빈도 관계를 multiset으로 표현한다

같은 값이 몇 번 등장했는지 세는 코드를 dict로 직접 만들 수 있지만 `Counter`는 value의 multiplicity를 자료구조 의미로 제공한다. 로그 level 빈도, token frequency, 재고 개수처럼 element 존재 여부가 아니라 **몇 개 존재하는가**가 중심인 문제에 적합하다.

Counter arithmetic은 multiset union/difference와 유사한 operation을 제공할 수 있지만 음수·0 count가 어떤 operation에서 유지되거나 제거되는지 정확한 semantics를 확인해야 한다. 재고처럼 음수가 금지된 domain은 Counter 자체에 모든 invariant를 맡기지 말고 update boundary에서 business validation을 추가한다.

`most_common` 같은 기능은 상위 빈도를 쉽게 찾지만 전체 데이터를 정렬/선택하는 비용을 고려한다. 대규모 streaming frequency에서 memory가 문제라면 모든 distinct key를 Counter에 보관하는 구조 자체가 적합한지 검토한다. Specialized collection도 무한 cardinality 문제를 해결하지 않는다.

---

## CHAPTER 03 · `defaultdict`는 missing key에서 value 생성 policy를 mapping에 넣는다

Grouping 코드는 `if key not in mapping: mapping[key] = []` 같은 boilerplate를 반복할 수 있다. `defaultdict(list)`는 없는 key를 읽을 때 새 list를 만드는 policy를 mapping 자체에 둔다. 따라서 caller는 grouping operation에 집중할 수 있다.

하지만 missing read가 mapping mutation을 일으킨다는 점이 중요하다. 단순 조회라고 생각한 코드가 존재하지 않는 key를 생성해 size와 serialization 결과를 바꿀 수 있다. `get()`과 indexing의 차이를 이해하고 read-only query에서 accidental creation이 일어나지 않게 한다.

Default factory가 mutable object를 매 key마다 새로 만들어야 하는지도 확인한다. 모든 key가 같은 shared list를 반환하는 잘못된 factory를 쓰면 서로 다른 group이 동일 object를 공유한다. Factory는 호출 시점마다 적절한 독립 state를 만들어야 한다.

---

## CHAPTER 04 · `ChainMap`은 여러 mapping layer를 복사 없이 우선순위로 겹친다

Built-in default, config file, environment, command-line override처럼 여러 mapping을 precedence 순서로 읽고 싶을 때 모두 merge해 새 dict를 만들 수도 있다. `ChainMap`은 여러 mapping을 순서대로 검색해 first-found value를 제공하므로 원본 layer를 보존한 채 layered view를 만들 수 있다.

이 구조에서는 read와 write semantics가 다를 수 있다. Lookup은 여러 mapping을 순회하지만 assignment는 특정 front mapping에만 기록될 수 있다. “겹쳐 보이는 하나의 dict”라고 생각하면 수정이 어느 layer에 들어가는지 오해할 수 있다. Configuration view처럼 underlying source를 유지해야 할 때는 이 차이가 유용하다.

원본 mapping이 나중에 변경되면 ChainMap view에도 즉시 반영될 수 있다. Immutable configuration snapshot이 필요한 경우에는 explicit merge/copy가 더 적합하다. Live layered view와 frozen snapshot 중 어떤 semantics가 필요한지 결정한다.

---

## CHAPTER 05 · named tuple은 위치 기반 record에 field 의미를 추가한다

`(x, y)`처럼 field 수가 작고 의미가 안정된 tuple은 간단하지만 항목이 늘어나면 `row[3]`가 무엇인지 기억하기 어렵다. Named tuple 계열은 tuple의 compact sequence 성질을 유지하면서 field 이름을 제공해 record 의미를 높일 수 있다.

읽기 전용 result record, 좌표, database row adapter처럼 behavior가 거의 없고 field 구성 자체가 중심인 값에 적합하다. Validation, 상태 전이, 복잡한 method가 필요해지면 dataclass나 domain class가 더 자연스러울 수 있다. Named record를 모든 object model의 대체품으로 쓰지 않는다.

Tuple semantics를 유지하므로 unpacking과 sequence operation이 가능하지만 이 유연성이 오히려 caller를 position에 결합시킬 수 있다. Public API가 장기적으로 field를 추가할 가능성이 있다면 positional unpacking compatibility도 고려한다.

---

## CHAPTER 06 · mapping의 ordering은 presentation과 identity에 어떤 의미가 있는지 분리한다

현대 Python dict는 insertion order와 관련된 언어 contract를 제공하지만 “mapping은 본질적으로 순서가 없다”던 오래된 직관과 실제 behavior를 구분해야 한다. 순서가 domain 의미인지 단지 현재 입력 순서를 보존하는 presentation detail인지 정한다.

JSON output이나 user-facing report가 insertion order에 의존한다면 data insertion path가 바뀌었을 때 output도 달라질 수 있다. Canonical serialization이 필요하면 명시적인 sort/order policy를 적용한다. 반대로 사용자가 등록한 순서를 보존해야 하는 UI에서는 insertion order 자체가 contract가 될 수 있다.

Equality와 ordering도 구분한다. 두 mapping이 같은 key/value를 가지면 insertion order가 달라도 값으로 같게 취급될 수 있지만 serialized bytes는 다를 수 있다. Cache key나 signature에 mapping representation을 사용할 때 canonicalization이 필요한 이유다.

---

## CHAPTER 07 · bounded history는 자료구조에 resource budget을 직접 넣는다

최근 1,000개의 latency sample, 마지막 100개 command history처럼 오래된 데이터를 자동 폐기해도 되는 경우 bounded deque나 ring-buffer 형태가 유용하다. “필요하면 나중에 지운다”는 cleanup보다 maximum count가 자료구조 contract에 들어가므로 unbounded memory growth를 구조적으로 막는다.

Count limit만으로 충분하지 않은 경우도 있다. Event 하나의 payload size가 크게 다르면 1,000개가 예상보다 많은 memory를 사용할 수 있다. Byte budget이나 time window를 추가할 수 있다. 오래된 항목이 제거되는 순간 외부 resource까지 release해야 하는 object라면 eviction callback과 ownership을 신중히 설계한다.

Bounded in-memory history는 observability sample이나 UI convenience에 적합하지만 durable audit와 혼동하지 않는다. Data retention 요구사항이 다른 두 목적을 하나의 container에 얹으면 의도하지 않은 손실이 생긴다.

---

## CHAPTER 08 · specialized collection 선택은 연산·불변조건·lifetime 세 축으로 결정한다

Deque, Counter, defaultdict, ChainMap, named record를 선택하는 기준은 API가 편리한가가 아니다. 어떤 연산이 자주 일어나는지, container 자체가 어떤 invariant를 표현하는지, 내부 데이터가 얼마나 오래 살아야 하는지를 본다. 같은 값 집합도 사용 패턴에 따라 적합한 구조가 달라진다.

특수 자료구조가 problem domain을 더 정확하게 표현하면 함수 body의 조건문과 bookkeeping이 줄어든다. 반대로 팀이 semantics를 이해하지 못하고 단지 짧은 코드 때문에 도입하면 hidden mutation이나 ordering rule을 오해할 수 있다. 중요한 구조는 작은 wrapper와 contract test로 behavior를 명시할 수 있다.

자료구조 설계의 핵심은 **데이터를 담을 수 있는 container를 고르는 것이 아니라 프로그램이 실제로 수행하는 연산과 유지해야 할 규칙을 가장 직접적으로 표현하는 container를 선택하는 것**이다.