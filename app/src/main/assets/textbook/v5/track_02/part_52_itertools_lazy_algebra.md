# PART 52 · Iterator algebra — `itertools`로 lazy sequence를 조합하면서 소비 규칙을 보존하기

Iterator를 하나씩 직접 구현하지 않아도 작은 반복 연산을 조합해 큰 데이터 흐름을 만들 수 있다. `chain`, `islice`, `groupby`, `tee`, 조합 iterator는 각각 간단해 보이지만 **언제 input을 소비하고, 얼마나 buffer를 만들며, 순서를 어떤 전제로 해석하는지**가 다르다. Lazy pipeline의 품질은 함수 이름을 많이 아는 데서 나오지 않고 각 iterator가 가진 state와 소비 규칙을 정확히 연결하는 데서 나온다.

---

## CHAPTER 01 · iterator combinator는 sequence를 새로 저장하지 않고 소비 규칙을 바꾼다

List 연산은 결과 collection 전체를 즉시 만들기 쉽지만 iterator combinator는 upstream에서 값을 요청할 때 필요한 만큼만 가져오는 경우가 많다. 이 성질을 이용하면 대형 파일이나 무한 sequence를 일정한 memory로 처리할 수 있다. 그러나 lazy object를 만들었다는 사실만으로 실제 memory가 작아지는 것은 아니다. 어떤 combinator는 내부 상태와 buffer를 유지하고 downstream이 느리면 retention이 늘 수 있다.

Iterator algebra라는 표현은 `source → filter → map → take → group`처럼 작은 변환을 함수 합성처럼 연결한다는 뜻이다. 각 단계가 input order를 보존하는지, one-to-one인지 one-to-many인지, 끝을 어떻게 알리는지를 알면 전체 pipeline semantics를 계산할 수 있다. 반대로 각 helper를 독립 암기하면 소비가 한 번뿐인 iterator를 두 단계가 함께 읽는 문제를 발견하기 어렵다.

Pipeline을 설계할 때 intermediate list를 없애는 것만 목표로 두지 않는다. 오류가 어느 단계에서 발생하는지, resource가 얼마나 오래 살아 있는지, consumer가 중간에 멈추면 upstream이 얼마나 소비되었는지를 함께 본다. Lazy evaluation은 계산 시점을 뒤로 미루기 때문에 디버깅 위치와 lifetime도 바꾼다.

---

## CHAPTER 02 · `chain`은 여러 iterable을 하나의 순차 stream으로 연결한다

여러 source를 이어 붙일 때 list concatenation은 모든 내용을 새 collection에 복사할 수 있지만 `chain` 계열은 첫 iterable을 소진한 뒤 다음 iterable을 순서대로 소비할 수 있다. 각 source가 generator나 file iterator라면 전체 데이터를 메모리에 모으지 않고 하나의 stream처럼 읽을 수 있다.

이 연산은 interleaving이 아니라 concatenation이다. Source A가 매우 길거나 끝나지 않으면 Source B는 영원히 소비되지 않는다. 여러 source를 공정하게 섞어야 하는 문제에서는 round-robin 또는 async merge처럼 다른 scheduling semantics가 필요하다. 함수가 “합친다”는 표현만으로 ordering policy를 일반화하지 않는다.

Source 중 하나가 exception을 내면 chain 전체 iteration도 그 지점에서 실패한다. 다음 source로 자동 진행할지 여부는 기본 contract가 아니므로 fault-tolerant ingestion이 필요하면 source별 error boundary를 따로 둔다. 또한 각 source가 file이나 network resource를 소유한다면 앞 source가 끝날 때 resource가 닫히는지 확인한다.

---

## CHAPTER 03 · `islice`는 iterator를 복사하지 않고 지정 범위만큼 실제로 소비한다

`islice(source, start, stop, step)`는 sequence slicing과 비슷해 보이지만 source가 iterator라면 앞의 값을 건너뛰는 과정 자체가 소비다. `start=1000`이면 0..999 항목을 보관하지 않고 버릴 수 있지만 원본 iterator는 이미 1000개를 지난 상태가 된다. 이후 다른 코드가 같은 iterator를 이어 읽으면 그 값들은 다시 얻을 수 없다.

Random access 가능한 list slicing과 single-pass stream slicing을 구분해야 한다. 원격 pagination source에서 islice로 100번째 항목부터 받고 싶다고 해도 upstream API가 1~99페이지를 실제로 조회해야 할 수 있다. Syntax가 효율적인 seek를 보장하지 않는다.

`stop=None`인 slice는 source가 끝날 때까지 진행할 수 있고 무한 iterator와 결합하면 결과도 무한하다. `step`이 크더라도 skipped item을 upstream에서 소비해야 할 수 있다. Data source가 expensive한 경우 소비 비용을 별도로 계산한다.

---

## CHAPTER 04 · `tee`는 iterator 복제를 흉내 내지만 소비 속도 차이를 buffer로 지불한다

Single-pass iterator를 두 consumer가 각각 읽어야 할 때 `tee` 계열 도구는 논리적으로 독립된 iterator를 만들 수 있다. 하지만 원본 source에서 한 번 읽은 값을 느린 consumer가 아직 보지 않았다면 내부 buffer에 보관해야 한다. 두 consumer의 진행 위치 차이가 커질수록 memory 사용량도 늘 수 있다.

한쪽이 전체 stream을 먼저 끝내고 다른 쪽이 거의 읽지 않는다면 사실상 원본 전체가 buffer될 수 있다. 이 경우 처음부터 list로 materialize하는 것과 memory profile이 비슷하거나 더 복잡해질 수 있다. `tee`를 “무료 복제”로 이해하지 않는다.

Iterator를 여러 번 순회해야 하는 요구가 자주 반복된다면 API 자체가 replay 가능한 collection을 반환하는 편이 더 적합할 수 있다. 반대로 두 consumer가 거의 같은 속도로 짧은 구간을 읽는다면 tee가 유용하다. 선택 기준은 데이터 크기와 consumer drift다.

---

## CHAPTER 05 · `groupby`는 전체 동일 key가 아니라 연속된 동일 key run을 묶는다

`groupby`는 SQL의 `GROUP BY`처럼 input 전체에서 같은 key를 모두 모아 주는 operation이 아니다. 현재 iterator에서 인접한 항목들이 같은 key를 가지는 동안 하나의 group을 만든다. 따라서 동일 key의 모든 데이터를 하나로 묶고 싶다면 먼저 key 기준으로 정렬되어 있거나 원본이 이미 같은 key를 연속 제공한다는 invariant가 필요하다.

예를 들어 `A, B, A`를 groupby하면 A group, B group, 다시 A group이 생길 수 있다. 이 semantics를 모르면 aggregate 결과가 중복 group으로 나뉜다. Sort가 필요하면 전체 materialization과 O(n log n) 비용이 생길 수 있으므로 원본 source가 key order를 보장하는지 확인한다.

Group iterator 자체도 원본 stream을 공유한다. 다음 group으로 진행하면 이전 group의 아직 소비하지 않은 항목을 나중에 읽을 수 없을 수 있다. Group을 오래 보관해야 한다면 필요한 값을 그 시점에 materialize해야 한다. Nested lazy iterator의 ownership 문제다.

---

## CHAPTER 06 · 조합 iterator는 값 수가 폭발할 수 있으므로 결과 cardinality를 먼저 계산한다

`product`, `permutations`, `combinations`는 테스트 case 생성과 탐색에 강력하지만 input 크기가 조금만 커져도 결과 수가 매우 빠르게 증가한다. n개 항목의 permutation은 n!개가 될 수 있고 cartesian product는 각 차원의 크기를 곱한다. Lazy iterator라 해도 생성 결과 전체를 소비하면 CPU 시간은 사라지지 않는다.

입력 20개의 모든 permutation처럼 현실적으로 처리할 수 없는 작업을 “generator라 memory가 적으니 가능”하다고 오해하지 않는다. Lazy evaluation은 저장량을 줄일 수 있지만 algorithmic work count를 줄이지 않는다. 먼저 결과 cardinality와 budget을 계산한다.

테스트 조합 생성에서도 모든 boolean flag와 enum의 cartesian product를 무조건 실행하기보다 pairwise strategy나 중요한 boundary combination을 선택할 수 있다. Exhaustive enumeration이 정말 필요한 작은 state space와 sampling이 필요한 큰 state space를 구분한다.

---

## CHAPTER 07 · infinite iterator는 downstream termination contract가 반드시 필요하다

`count`, `cycle`, `repeat`처럼 끝나지 않는 iterator는 timer tick, ID sequence, repeated default 같은 개념을 표현할 수 있다. 하지만 downstream에서 `list()`나 전체 정렬처럼 끝을 요구하는 operation을 적용하면 종료하지 않거나 memory를 고갈시킨다. Infinite source를 사용할 때는 `islice`, `takewhile`, explicit cancellation처럼 유한 소비 조건을 함께 설계한다.

Termination predicate가 외부 state에 의존하면 언젠가 반드시 false가 되는지 검토한다. Remote API가 page token을 잘못 반복해 사실상 무한 pagination이 되는 경우에는 seen-token set과 maximum page budget 같은 defensive condition이 필요하다.

Infinite sequence는 library boundary에서도 표시할 필요가 있다. 단순 `Iterable[T]` 타입만 보면 caller가 finite collection이라고 생각해 전체 materialization할 수 있다. Documentation과 naming으로 stream lifetime을 드러낸다.

---

## CHAPTER 08 · lazy pipeline은 memory 최적화가 아니라 소비·오류·lifetime을 함께 재배치하는 설계다

Iterator combinator를 잘 연결하면 중간 list 없이 한 항목씩 처리할 수 있지만 각 stage가 언제 실행되는지 consumer에게 달려 있다. Pipeline을 만드는 함수는 즉시 성공해도 실제 parser error와 file read failure는 소비 시점에 나타날 수 있다. 따라서 error boundary와 resource scope를 consumption 위치까지 포함해 설계한다.

관측에서도 pipeline construction time과 full consumption time을 분리한다. Generator object를 빠르게 만들었다고 전체 작업이 빨라진 것이 아니다. Profiling과 benchmark는 sink가 모든 필요한 값을 실제로 소비하도록 해야 한다.

Iterator algebra의 핵심은 **작은 lazy operation을 많이 쓰는 것이 아니라 각 단계가 input을 얼마나 소비하고 어떤 상태를 보관하며 어떤 종료 조건을 요구하는지 연결해 전체 pipeline의 시간·공간·실패 semantics를 예측하는 것**이다.