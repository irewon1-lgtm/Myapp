# PART 36 · Refactoring과 change safety — behavior를 유지한 채 구조를 바꾸는 방법

리팩터링은 코드를 예쁘게 정리하는 작업이 아니라 외부 behavior contract를 유지하면서 내부 구조를 변경하는 작업이다. 이름 변경, 함수 추출, 자료구조 교체, dependency 방향 수정은 모두 “같은 기능을 더 안전하게 변경할 수 있는 형태로 만든다”는 목적을 가진다. 기능 변경과 구조 변경을 섞으면 실패했을 때 원인을 좁히기 어려워진다. 핵심은 **현재 behavior를 증거로 고정하고 작은 구조 변화를 반복적으로 검증하는 것**이다.

---

## CHAPTER 01 · refactoring의 첫 조건은 무엇을 유지해야 하는지 아는 것이다

“동작을 바꾸지 않는다”는 말은 public output만 같으면 된다는 뜻이 아닐 수 있다. Exception type, timing, side effect 순서, persistence format, logging event가 caller에게 contract일 수 있다. Refactoring 전에 실제로 유지해야 하는 observable behavior를 식별한다.

Legacy code에 test가 부족하면 characterization test를 먼저 작성해 현재 behavior를 기록할 수 있다. 현재 behavior가 이상해 보여도 먼저 무엇이 실제로 일어나는지 고정한 뒤 bug fix를 별도 change로 수행하면 구조 변경과 기능 변경을 분리할 수 있다.

모든 implementation detail을 test로 고정하면 리팩터링이 불가능해진다. Private helper 호출 순서보다 외부 contract와 중요한 invariant를 중심으로 보호한다. Internal structure는 바꾸기 위해 존재하는 자유 공간이다.

성능도 contract일 수 있다. O(n) lookup을 O(n²)로 바꾸면서 결과만 같으면 large input에서 사실상 behavior가 달라진다. 중요한 resource budget은 benchmark나 performance test로 함께 보호한다.

---

## CHAPTER 02 · rename은 작은 변경처럼 보여도 public surface와 serialization을 건드릴 수 있다

Local variable 이름 변경은 대체로 안전하지만 function parameter가 keyword argument로 public하게 사용된다면 이름 변경이 caller를 깨뜨릴 수 있다. Dataclass field 이름이 JSON serializer와 연결되어 있다면 storage/wire schema까지 변할 수 있다.

IDE rename 도구는 syntax reference를 넓게 찾지만 dynamic `getattr(obj, "name")`, string-based registry, template, configuration까지 모두 알 수는 없다. Search와 contract test를 함께 사용한다.

Public 이름을 바꿔야 한다면 일정 기간 old alias를 유지하고 deprecation warning을 제공할 수 있다. 하지만 영구 alias는 API surface를 계속 늘리므로 제거 version을 계획한다.

이름 변경은 개념 정리의 강한 도구다. `data`, `process`, `handle` 같은 모호한 이름을 domain vocabulary로 바꾸면 이후 함수 분해와 module 경계가 더 명확해질 수 있다.

---

## CHAPTER 03 · Extract Function은 줄 수보다 의미 단위를 분리할 때 효과가 있다

긴 함수에서 어떤 계산 블록이 하나의 명확한 목적과 input/output을 가진다면 별도 함수로 추출해 이름을 붙일 수 있다. 이때 local variable가 너무 많이 필요하다면 원래 함수 안에 여러 책임과 state가 얽혀 있다는 신호일 수 있다.

추출한 함수가 global state를 여전히 많이 읽으면 표면만 나뉜 것이다. 필요한 값을 parameter로 전달하고 return으로 결과를 돌려주면 data dependency가 드러난다. Pure calculation을 먼저 추출하면 test하기 쉽다.

반대로 한 줄 helper를 수십 개 만들어 caller가 구현 흐름을 따라 여러 파일을 이동하게 만드는 것은 개선이 아닐 수 있다. 추상화 수준과 reuse보다 개념 경계를 우선한다.

Extract Function 후 test가 그대로 통과하면 behavior preservation 증거가 된다. Performance-sensitive hot loop에서 호출 수가 늘었다면 profiler로 비용을 확인하고 필요하면 다른 경계를 선택한다.

---

## CHAPTER 04 · Replace Conditional with Polymorphism은 상태와 behavior 변화 축이 맞을 때만 쓴다

`if type == A`, `elif type == B`가 여러 함수에 반복된다면 variant별 behavior를 class method로 옮겨 polymorphism으로 dispatch할 수 있다. 새로운 variant를 추가할 때 여러 switch를 수정하는 대신 새 implementation 한 곳에 behavior를 모을 수 있다.

하지만 operation 종류가 자주 늘고 variant 종류는 안정적이라면 pattern matching이나 visitor가 더 적합할 수 있다. 모든 branch를 class hierarchy로 바꾸는 것은 코드 수를 늘리고 data transformation을 어렵게 만들 수 있다.

Refactoring pattern은 자동 정답이 아니라 change axis를 맞추는 선택이다. Variant와 operation 중 어느 쪽이 더 자주 바뀌는지 관찰한다.

전환 과정에서는 old branch와 new dispatch를 동시에 유지해 결과를 비교하는 characterization mode를 사용할 수 있다. 동일 input에서 output/invariant가 같은지 확인한 뒤 old path를 제거한다.

---

## CHAPTER 05 · data structure 교체는 API semantic 차이를 먼저 찾는다

List membership이 병목이라 set으로 바꾸면 average lookup은 빨라질 수 있지만 order와 duplicate semantics가 달라진다. Dict로 바꾸면 key uniqueness가 생기고 iteration contract가 달라질 수 있다. Performance refactor가 domain behavior를 바꾸지 않는지 확인한다.

Internal representation을 직접 caller에 반환하고 있었다면 교체가 어려워진다. `list` 자체보다 iterable/read-only view를 public contract로 제공했으면 내부 set/index를 도입하기 쉽다.

Migration 중 old/new representation을 동시에 유지하면 synchronization invariant가 추가된다. 가능한 한 conversion boundary를 만들고 한 representation을 source of truth로 유지한다.

Large persistent data의 schema 변경은 code refactoring과 다르게 migration과 rollback을 요구한다. In-memory representation 교체와 storage format 변경을 같은 commit에서 처리하지 않으면 위험을 줄일 수 있다.

---

## CHAPTER 06 · dependency inversion refactor는 concrete import를 capability contract로 바꾼다

Core service가 특정 database client나 HTTP library를 직접 생성하면 test와 교체가 어렵다. 필요한 operation을 protocol/interface로 추출하고 composition root가 concrete implementation을 제공하면 dependency 방향을 뒤집을 수 있다.

첫 단계에서 거대한 generic repository interface를 만들지 않는다. Existing caller가 실제 사용하는 method만 추출해 작은 capability를 만든다. Interface가 implementation의 모든 method를 그대로 복사하면 abstraction 가치가 낮다.

Seam을 만든 뒤 fake implementation으로 core behavior를 test하고 실제 adapter는 integration test로 검증한다. 이 두 test가 다른 failure class를 다루는지 확인한다.

Dependency inversion은 layer 수를 늘리는 목표가 아니다. 외부 기술 변화가 domain logic까지 전파되는 범위를 줄이는 것이 목적이다.

---

## CHAPTER 07 · parallel change는 큰 breaking migration을 여러 호환 단계로 나눈다

Public API나 schema를 한 번에 바꾸기 어려울 때 새 interface를 추가하고 old/new를 동시에 지원한 뒤 caller를 순차적으로 이동시키고 마지막에 old path를 제거할 수 있다. 이 방식은 배포 순서가 엇갈리는 system에서 특히 유용하다.

Database column rename도 새 column 추가→dual read/write 또는 backfill→caller 전환→old column 제거 같은 expand/contract 단계로 나눌 수 있다. 각 단계가 old/new application version과 어떤 조합에서 안전한지 matrix를 만든다.

Compatibility 기간에는 code가 더 복잡해진다. Temporary bridge가 영구 legacy가 되지 않도록 제거 조건과 metric을 정의한다.

Feature flag를 사용해 new implementation을 일부 traffic에서 비교할 수 있지만 두 path 모두 유지하는 기간만큼 test burden이 늘어난다. Rollout 완료 후 dead path를 삭제한다.

---

## CHAPTER 08 · branch by abstraction은 implementation 교체를 호출자 변경과 분리한다

큰 component를 교체해야 할 때 caller가 concrete implementation을 직접 사용하면 모든 caller와 구현을 동시에 바꿔야 한다. 먼저 abstraction을 넣어 기존 implementation을 그 뒤에 배치하고, 새 implementation을 같은 contract로 추가한 뒤 wiring만 전환한다.

이 패턴은 storage engine, external API client, rule evaluator 교체처럼 risk가 큰 change에서 유용하다. Shadow mode로 old/new 결과를 비교해 semantic difference를 발견할 수 있다.

하지만 abstraction을 너무 일찍 만들어 실제 변화가 없는 곳에 넣으면 indirection만 늘어난다. 구체적인 교체 요구가 있을 때 seam을 만든다.

Performance와 side effect를 비교할 때 output equality만 보지 않는다. Query 수, timeout, retry, write ordering 같은 operational behavior도 contract일 수 있다.

---

## CHAPTER 09 · refactoring commit을 작게 유지하면 실패 원인과 review 범위가 줄어든다

Rename, function move, behavior change, formatting을 한 commit에 모두 섞으면 diff가 커져 reviewer가 의미 있는 변화와 기계적 변화를 구분하기 어렵다. 가능하면 mechanical refactor와 semantic change를 분리한다.

각 단계에서 test를 실행하고 compile/type check를 통과시키면 어느 단계에서 깨졌는지 쉽게 찾을 수 있다. Git bisect도 각 commit이 buildable/testable할수록 유용하다.

자동 formatter 결과를 별도 commit으로 분리하면 logic diff가 선명해진다. Generated file과 authored code를 같은 review 방식으로 보지 않는다.

작은 commit은 revert 단위도 작게 만든다. Production incident에서 기능 전체를 되돌리지 않고 위험한 구조 변경만 분리해 판단할 수 있다.

---

## CHAPTER 10 · refactoring은 change cost를 낮추는 투자이며 종료 조건이 필요하다

구조 개선은 끝없이 할 수 있다. 현재 기능 변경에 필요한 risk를 줄였는지, duplicate rule을 한 곳으로 모았는지, test seam을 확보했는지처럼 구체적인 목적을 정한다. “더 깨끗해질 때까지”는 종료 기준이 아니다.

미래 가능성을 상상해 abstraction을 미리 많이 만들면 현재 이해해야 할 layer가 늘어난다. 실제 반복되는 변화와 incident에서 드러난 coupling을 evidence로 사용한다.

Refactoring 후에는 code metric보다 다음 change가 쉬워졌는지 본다. 새 payment provider를 한 adapter 추가로 넣을 수 있는지, state rule 변경이 한 module에 국한되는지, regression test가 빠르게 원인을 알려 주는지가 실용적인 결과다.

리팩터링의 핵심은 **기능을 바꾸지 않는 작은 구조 변화와 실제 기능 변화를 분리하고, 매 단계 behavior evidence를 유지하면서 코드의 변화 축과 경계를 맞춰 가는 것**이다.