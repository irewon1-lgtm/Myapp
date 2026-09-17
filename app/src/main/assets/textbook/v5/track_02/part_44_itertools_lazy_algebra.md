# PART 44 · itertools와 lazy algebra — 반복 흐름을 조합 가능한 작은 연산으로 만들기

반복문을 직접 쓰면 가장 자유롭지만 데이터 흐름이 `필터링`, `그룹화`, `슬라이싱`, `조합`, `연결`처럼 반복되는 패턴을 가질 때는 iterator 도구를 조합하는 편이 더 명확할 수 있다. `itertools` 계열은 값을 미리 모두 만들지 않고 반복 흐름 자체를 변환한다. 중요한 것은 함수 이름을 많이 외우는 것이 아니라 **각 연산이 입력을 얼마나 소비하고, 어떤 상태를 보관하며, 결과가 유한한지**를 이해하는 것이다.

---

## CHAPTER 01 · iterator algebra는 sequence를 값 목록보다 변환 가능한 흐름으로 본다

List를 받아 새 list를 만드는 대신 iterator를 받아 iterator를 반환하는 stage를 연결하면 source가 file이든 generator든 같은 consumer와 결합할 수 있다. `filter → map → take`처럼 각 단계는 다음 값을 요청할 때 upstream에서 필요한 만큼만 소비한다.

이 구조는 memory를 줄이는 것 외에도 early termination을 가능하게 한다. 첫 10개 결과만 필요하면 source 전체를 처리할 필요가 없다. 다만 upstream operation에 side effect가 있다면 소비하지 않은 항목의 effect도 실행되지 않는다.

Lazy pipeline을 함수 인자와 반환 타입에서 `Iterable`/`Iterator`로 표현하면 single-pass semantics가 보인다. Caller가 list를 기대해 index와 length를 사용하면 abstraction이 맞지 않는다.

Iterator algebra의 강점은 stage를 작은 pure-like transformation으로 만들 수 있다는 점이다. 실패와 resource lifetime은 각 stage boundary에서 별도 contract를 둔다.

---

## CHAPTER 02 · chain은 여러 iterable을 하나의 연속 stream처럼 소비한다

여러 file source나 batch를 순서대로 처리할 때 `chain`류 연산은 각 iterable의 element를 이어서 제공한다. 모든 source를 하나의 list로 합치지 않아도 되므로 큰 입력의 peak memory를 낮출 수 있다.

하지만 source boundary가 사라진다는 의미가 있다. Error report에서 어느 source에서 온 record인지 필요하면 element에 provenance를 함께 붙이거나 `(source_id, item)` 형태로 흐르게 해야 한다. 단순 flatten이 metadata까지 지우지 않게 한다.

Source 중 하나가 무한 iterable이면 뒤 source는 영원히 도달하지 못할 수 있다. 각 입력이 유한하다는 전제가 있는지 확인한다.

여러 source를 round-robin으로 공정하게 소비해야 한다면 단순 chain과 다른 scheduling problem이다. Operator 이름이 요구하는 fairness까지 제공한다고 가정하지 않는다.

---

## CHAPTER 03 · islice는 lazy range를 만들지만 upstream 소비 위치를 전진시킨다

`islice`류 도구는 iterator에서 특정 범위의 element만 lazy하게 가져올 수 있다. 큰 stream의 앞 100개를 preview하거나 pagination-like window를 만들 때 전체 list slice보다 memory 효율적일 수 있다.

Iterator는 stateful하므로 slice를 소비한 뒤 원본 iterator의 위치도 전진한다. 같은 iterator를 다른 consumer가 이어서 사용하면 이미 건너뛴 항목을 다시 볼 수 없다. Snapshot sequence의 slice와 stateful stream slice를 구분한다.

Start가 큰 값을 요구하면 그 위치까지 upstream element를 실제로 소비해야 할 수 있다. `islice(stream, 1_000_000, ...)`가 random access처럼 즉시 이동한다고 가정하지 않는다.

Page number가 커지는 API에서 매번 처음부터 skip하는 구조는 O(n) scan을 반복할 수 있다. Cursor/token 기반 pagination과 자료구조 index가 더 적합할 수 있다.

---

## CHAPTER 04 · takewhile과 dropwhile은 predicate가 처음 바뀌는 경계에서 동작한다

`takewhile`은 predicate가 참인 동안 값을 내보내다가 처음 거짓이 되는 순간 종료한다. 이후에 다시 참인 값이 나타나도 보지 않는다. 전체 stream에서 조건을 만족하는 모든 항목을 찾는 filter와 의미가 다르다.

정렬된 timestamp stream에서 deadline 전 record만 가져오는 경우처럼 monotonic한 predicate가 있을 때 유용하다. 입력 순서가 보장되지 않으면 원하는 결과와 다를 수 있다.

`dropwhile`은 처음 predicate가 거짓이 될 때까지 앞부분을 버리고 이후 모든 값을 전달한다. Header/comment prefix를 건너뛰는 데 사용할 수 있지만 중간에 다시 나타나는 comment를 제거하지 않는다.

Predicate가 expensive하거나 effectful하면 어디까지 호출되는지 semantics가 중요하다. Lazy operator를 선택할 때 “어떤 값에 몇 번 predicate가 실행되는가”를 읽는다.

---

## CHAPTER 05 · groupby는 전체 key 집합이 아니라 인접한 같은 key run을 묶는다

Iterator groupby를 SQL의 GROUP BY와 같은 것으로 생각하면 오류가 생긴다. 일반적으로 인접한 element 중 같은 key를 가진 연속 구간을 묶으므로 동일 key가 떨어져 있으면 여러 group으로 나뉠 수 있다. 전체 key group이 필요하면 입력을 먼저 key 기준으로 정렬하거나 다른 aggregation 구조를 사용해야 한다.

Group iterator는 원본 source와 같은 underlying iterator를 공유할 수 있어 outer loop가 다음 group으로 진행하면 이전 group을 나중에 다시 소비할 수 없을 수 있다. Group 내용을 보관해야 하면 해당 시점에 materialize한다.

정렬 비용과 memory를 감수할지, stream이 원래 key 순서로 들어오는지를 data contract에서 결정한다. Log가 user_id 순서로 정렬돼 있지 않은데 groupby를 사용하면 같은 user가 여러 번 나뉜다.

Groupby는 “인접 run compression” 도구로 이해하면 semantics가 선명해진다.

---

## CHAPTER 06 · tee는 iterator 복제를 흉내 내기 위해 내부 buffer를 만들 수 있다

Single-pass iterator를 두 consumer가 독립적으로 읽고 싶을 때 `tee`류 기능을 사용할 수 있지만 원본을 진짜 복제하는 것은 아니다. 한 consumer가 앞서 나가면 아직 느린 consumer가 읽지 않은 값을 내부 buffer에 저장해야 한다.

두 consumer 속도 차이가 매우 크거나 한쪽이 멈추면 buffer가 source 크기만큼 커질 수 있다. “lazy니까 memory가 작다”는 가정이 깨지는 대표 사례다.

두 consumer가 실제로 모두 전체 data를 필요로 한다면 처음부터 list로 materialize하는 것이 lifetime과 memory upper bound를 더 명확하게 만들 수 있다. 작은 bounded stream에서는 단순한 방법이 더 낫다.

Thread-safety도 API contract를 확인한다. 같은 tee iterator를 여러 thread가 동시에 소비한다고 자동으로 안전한 것은 아니다.

---

## CHAPTER 07 · product·permutations·combinations는 작은 입력도 결과 공간이 폭발할 수 있다

Cartesian product는 각 입력 크기의 곱만큼 결과를 만들고 permutation은 factorial 규모로 커질 수 있다. Iterator로 결과를 하나씩 생성하면 memory peak는 줄여도 총 CPU 작업량과 출력 cardinality는 줄지 않는다.

10개 항목의 permutation은 3,628,800개다. 입력 20개를 전부 순열 생성하는 것은 lazy iterator라도 현실적이지 않다. Combinatorial operator에는 maximum input size와 maximum output budget을 둔다.

Search problem에서 모든 조합을 생성하기 전에 pruning과 dynamic programming, specialized algorithm으로 상태 공간을 줄일 수 있는지 본다. Iterator는 algorithmic explosion을 숨기지 않는다.

Test data 생성에서도 모든 조합 대신 pairwise, equivalence partition 등 필요한 coverage 전략을 선택할 수 있다.

---

## CHAPTER 08 · accumulate는 prefix state를 stream으로 노출한다

누적합, running max처럼 이전 결과와 새 input을 결합해 중간 상태를 계속 내보내는 operation은 accumulate로 표현할 수 있다. 최종 reduce가 하나의 결과만 반환하는 것과 달리 모든 prefix 결과를 stream으로 제공한다.

Financial running balance처럼 각 시점의 intermediate value가 의미 있을 때 유용하다. 하지만 combine function이 associative하지 않거나 floating precision 문제가 있으면 순서가 결과에 영향을 준다.

초기값의 의미도 명확히 한다. Empty input에서 어떤 result stream이 나오는지, explicit initial state가 output에 포함되는지 API contract를 확인한다.

Stateful accumulation이 external mutable object를 수정하면 replay와 test가 어려워진다. 가능한 한 `(state, item) -> new_state` 형태의 pure transition으로 만든다.

---

## CHAPTER 09 · iterator pipeline debugging은 소비 지점과 source 위치를 함께 추적한다

Lazy pipeline은 생성 시 아무 error가 없고 실제 consumer가 `next`를 요청하는 지점에서 parser나 transform error가 발생할 수 있다. Stack trace만 보면 최종 consumer line이 눈에 띄므로 source record identity와 stage name을 exception context에 붙인다.

중간 stage를 list로 잠시 materialize해 inspect하면 debugging은 쉬워지지만 large production data에 그대로 적용하면 memory behavior가 달라진다. Bounded sample과 tracing stage를 사용한다.

각 stage의 input/output count metric을 기록하면 어느 단계에서 record가 줄거나 폭증하는지 알 수 있다. `parsed=1000`, `valid=970`, `expanded=2910`처럼 흐름을 보존한다.

Pipeline stage를 generator expression 한 줄로 모두 연결하기보다 중요한 business transition에는 이름을 붙여 observability와 test seam을 만든다.

---

## CHAPTER 10 · iterator 도구 선택은 lazy 여부보다 state·cardinality·replay를 기준으로 한다

Chain, slice, group, product, accumulate는 모두 iterator를 다루지만 memory와 state contract가 서로 다르다. 어떤 연산은 source를 한 번만 소비하고, 어떤 연산은 내부 buffer를 만들며, 어떤 연산은 결과 cardinality를 폭발시킨다.

API 설계에서 결과를 여러 번 읽어야 하는지, 순서를 보존해야 하는지, 전체 길이를 알아야 하는지에 따라 iterator와 materialized collection을 선택한다. Lazy가 항상 우월하지 않다.

Iterator algebra가 잘 맞는 경우에는 데이터 흐름을 작은 operator로 조합하면서도 bounded resource로 큰 입력을 처리할 수 있다. 잘못 사용하면 consumption state와 hidden buffer 때문에 오히려 어려워진다.

핵심은 **반복 도구의 이름을 암기하는 것이 아니라 각 operator가 upstream을 언제 얼마나 소비하고 어떤 상태를 보관하며 결과 공간을 얼마나 만드는지 계산하는 것**이다.