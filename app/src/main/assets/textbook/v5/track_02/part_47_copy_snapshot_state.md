# PART 47 · Copy·snapshot·state transfer — 객체 그래프를 복제할 때 무엇을 보존할지 결정하기

복사는 단순히 “같은 값을 하나 더 만든다”는 operation이 아니다. 객체 그래프 안에서 어떤 node를 새로 만들고 어떤 node를 공유할지, identity를 유지해야 하는 resource는 무엇인지, 복사 시점의 state를 고정한 snapshot이 필요한지에 따라 의미가 달라진다. 얕은 복사와 깊은 복사를 문법으로만 구분하면 큰 그래프에서 비용과 correctness를 동시에 놓치게 된다. 핵심은 **복사하려는 대상의 소유권·identity·변경 가능성을 먼저 모델링하는 것**이다.

---

## CHAPTER 01 · copy는 값을 옮기는 것이 아니라 reference graph를 다시 구성한다

Python의 list, dict, class instance는 다른 객체를 reference할 수 있다. 어떤 object를 복사한다는 말은 결국 새 root object를 만들고 내부 edge를 원래 object로 유지할지 새 child로 연결할지 결정하는 작업이다. 따라서 복사 semantics는 단일 object가 아니라 graph 전체 관점에서 봐야 한다.

예를 들어 주문 객체가 immutable Money value와 mutable line-item list, shared Product catalog reference를 가진다고 하자. 주문 snapshot을 만들 때 line-item은 독립 복제가 필요할 수 있지만 Product catalog까지 모두 복제하면 identity와 memory가 불필요하게 늘어난다. 무엇을 복제해야 하는지는 타입 이름보다 ownership 관계가 결정한다.

`a = b`는 복사가 아니라 같은 object에 다른 이름을 바인딩한다. `copy.copy`나 container copy는 새로운 outer object를 만들 수 있지만 내부 reference를 공유할 수 있다. 이 차이를 이해하지 않으면 “복사했는데 왜 원본이 바뀌지?”라는 문제가 반복된다.

---

## CHAPTER 02 · shallow copy는 root container만 새로 만들고 내부 reference는 보존한다

Shallow copy된 list는 원본과 다른 list object지만 각 element가 mutable object라면 같은 element를 가리킬 수 있다. 따라서 top-level append/remove는 서로 독립적이지만 nested dict의 field를 바꾸면 양쪽에서 변화가 보일 수 있다.

```python
original = [{"name": "A"}]
clone = list(original)
clone.append({"name": "B"})
clone[0]["name"] = "X"
```

첫 번째 append는 clone의 outer structure만 바꾸므로 original length에는 영향이 없다. 두 번째 nested mutation은 shared dict object를 바꾸기 때문에 original에서도 `X`가 보인다. 복사 semantics를 읽을 때 “몇 단계 깊이까지 새 object가 만들어졌는가”를 확인한다.

Shallow copy가 잘못된 것은 아니다. 내부 element가 immutable하거나 의도적으로 shared entity라면 가장 적합할 수 있다. 비용이 낮고 identity를 보존한다는 장점도 있다. 문제는 호출자가 독립성을 기대하는데 공유가 숨어 있을 때다.

---

## CHAPTER 03 · deep copy는 모든 것을 독립적으로 만드는 만능 해법이 아니다

Deep copy는 reachable object graph를 재귀적으로 복제하려고 한다. 그러나 graph에 shared reference가 있으면 동일 child를 여러 번 복제해야 하는지 한 clone으로 공유해야 하는지 semantics가 필요하고, cycle도 처리해야 한다. Python의 deepcopy는 memo mechanism으로 일부 identity relation과 cycle을 관리하지만 application 의미를 자동으로 알 수는 없다.

File handle, socket, lock, database connection처럼 복제가 의미 없는 resource도 있다. External identity를 가진 User/Account entity를 deep-copy해 별도 object로 만든다고 해서 실제 외부 entity가 두 개 생긴 것은 아니다. Object memory와 domain identity를 혼동하면 update conflict가 생긴다.

대규모 graph에서 deep copy는 CPU와 memory peak를 크게 만들 수 있다. Request 하나를 처리할 때 전체 configuration tree를 매번 deepcopy하는 구조는 성능 문제를 만들 수 있다. Immutable shared data와 작은 mutable delta를 분리하면 복사 범위를 줄일 수 있다.

---

## CHAPTER 04 · 사용자 정의 copy protocol은 어떤 field를 새로 만들고 공유할지 명시한다

Class가 cache, lock, connection, parent link를 함께 가진다면 일반적인 shallow/deep copy가 domain contract와 맞지 않을 수 있다. 사용자 정의 copy behavior를 제공해 value field는 복제하고 shared service는 그대로 참조하며 transient cache는 비우는 식의 정책을 만들 수 있다.

복사본의 identity field를 유지할지 새 ID를 만들지도 중요하다. 문서 template 복제는 새로운 document ID가 필요할 수 있지만 read-only snapshot은 원래 entity ID를 보존하면서 version만 추가할 수 있다. `clone()`처럼 domain-specific method가 일반 copy보다 의도를 잘 표현하는 경우가 많다.

Copy protocol을 custom할 때는 constructor invariant를 우회하지 않도록 한다. Internal field를 직접 조립해 유효하지 않은 object를 만들기보다 검증된 factory를 재사용하거나 copy 후 invariant test를 수행한다.

---

## CHAPTER 05 · snapshot은 “현재 시점의 읽기 모델”을 고정하는 개념이다

Snapshot은 단순 복제보다 시간 semantics가 중요하다. Mutable object가 계속 변경되더라도 snapshot을 만든 시점의 상태는 이후 읽기에서 바뀌지 않아야 한다. Audit, undo, optimistic concurrency, report generation에서 이 성질이 필요하다.

Snapshot을 deep copy로 구현할 수도 있지만 더 효율적인 representation이 있을 수 있다. Immutable dataclass, serialized bytes, database MVCC snapshot처럼 상태를 고정하는 방법은 다양하다. 중요한 것은 snapshot consumer가 원본의 이후 mutation을 관찰하지 않는다는 계약이다.

Snapshot 시각과 version을 metadata로 기록하면 두 snapshot의 order와 source state를 비교할 수 있다. Wall-clock timestamp만으로 concurrency order를 완전히 판단할 수 없는 경우 version counter나 transaction ID가 더 적합할 수 있다.

---

## CHAPTER 06 · copy-on-write는 읽기 공유와 변경 시 복사를 결합한다

Copy-on-write(COW)는 처음에는 같은 underlying data를 공유하고 어느 쪽이 변경하려 할 때 해당 부분을 복제하는 전략이다. Operating system process memory와 일부 data structure에서 사용되며, 큰 read-mostly state의 초기 복사 비용을 줄일 수 있다.

COW가 안전하려면 공유 중인 state를 임의 mutation으로 바꿀 수 없게 write path를 통제해야 한다. 일반 mutable Python list를 여러 owner가 공유하면서 “필요하면 알아서 copy되겠지”라고 기대할 수는 없다. Library나 custom abstraction이 mutation을 가로채야 한다.

COW에서는 첫 write latency와 memory spike가 나중에 발생한다. Initial clone이 싸다고 전체 workload 비용이 사라진 것은 아니다. Write-heavy workload에서는 처음부터 독립 structure가 더 단순하고 빠를 수 있다.

---

## CHAPTER 07 · versioned state는 복사본 사이의 변경 충돌을 감지하게 한다

동일 entity의 snapshot 두 개를 각각 수정한 뒤 저장하려고 하면 마지막 writer가 앞선 변경을 덮어쓸 수 있다. Version field를 함께 저장하고 `expected_version`이 현재 저장소 version과 같을 때만 update하면 stale snapshot에서의 write를 감지하는 optimistic concurrency control을 만들 수 있다.

충돌을 발견한 뒤 자동 merge할지 사용자에게 재시도를 요구할지는 domain에 따라 다르다. 서로 다른 field 수정은 merge 가능할 수 있지만 같은 balance field를 두 요청이 바꾼 경우 business rule이 필요하다. Version check는 conflict를 발견하는 장치이지 conflict resolution 자체가 아니다.

Snapshot과 version은 undo/history에도 연결된다. 모든 전체 object를 저장하는 대신 event나 delta를 보관하고 특정 version state를 재구성할 수도 있다. Storage cost와 reconstruction complexity를 비교한다.

---

## CHAPTER 08 · serialization은 process 경계를 넘는 deep-copy처럼 보여도 의미가 다르다

Object를 JSON으로 serialize했다가 decode하면 보통 새로운 object graph가 생기므로 원본과 reference를 공유하지 않는다. 그래서 process boundary에서 자연스러운 state copy처럼 보일 수 있다. 그러나 serialization 과정에서 class identity, callable, private cache, exact numeric type 같은 정보가 사라질 수 있다.

Wire copy는 public schema에 포함한 의미만 보존한다. 따라서 “serialize→deserialize하면 원본 object와 완전히 동일”이 아니라 domain equality 또는 selected field equivalence를 요구하는 편이 정확할 수 있다. External resource handle을 serialize한다고 다른 process에서 같은 resource ownership이 자동 복제되는 것도 아니다.

Serialization format version이 달라지면 snapshot replay 결과가 달라질 수 있으므로 장기 snapshot은 schema version과 migration policy를 가져야 한다.

---

## CHAPTER 09 · copy test는 독립성·공유·identity를 각각 assertion한다

복사 test에서 `clone == original`만 확인하면 어떤 child가 공유되고 어떤 child가 독립인지 검증하지 못한다. 원본 nested mutable field를 바꿔 clone에 영향이 있는지, clone을 바꿔 원본이 유지되는지, 의도적으로 shared service의 identity는 같은지 별도 assertion이 필요하다.

Cycle과 shared child가 있는 graph를 test fixture로 사용하면 deep copy가 구조를 올바르게 보존하는지 확인할 수 있다. `a.left is a.right`였던 shared relation이 clone에서 두 개 별도 child로 깨지면 semantics가 달라질 수 있다.

Performance-sensitive clone은 representative graph size에서 time과 peak memory를 측정한다. Correctness는 통과하지만 request당 500MB를 복제하는 구현이라면 실제 계약을 만족하지 못한다.

---

## CHAPTER 10 · state transfer contract는 `무엇을 새로 만들고 무엇을 공유할지`를 명시한다

Copy, snapshot, serialization, cache snapshot은 모두 state를 다른 owner 또는 다른 시점으로 전달하는 작업이다. 설계할 때 네 가지를 정한다. 새 identity가 필요한가, nested mutable state를 공유해도 되는가, external resource는 누가 소유하는가, 원본 이후 변경을 복사본이 관찰해야 하는가.

이 답에 따라 shallow copy, deep copy, immutable value, snapshot DTO, versioned state 같은 도구를 선택한다. 무조건 deepcopy를 호출하는 것보다 domain relation을 먼저 정하는 편이 correctness와 performance 모두에 강하다.

State transfer의 핵심은 **메모리를 복제하는 행위가 아니라 object graph의 identity와 ownership을 새로운 경계에서 다시 정의하는 일**이다.