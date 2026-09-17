# PART 45 · Function signature와 API evolution — 호출 문법을 호환성 계약으로 관리하기

함수 signature는 parameter 이름을 나열하는 문법 이상의 역할을 한다. 어떤 인자를 위치로 받을 수 있는지, 이름으로만 받아야 하는지, 생략 가능한지, 추가 인자를 허용하는지에 따라 호출자의 코드가 함수 구현과 결합되는 방식이 달라진다. 여러 module이나 외부 사용자가 공유하는 함수에서는 signature 자체가 public API가 되므로 구현을 바꾸지 않아도 parameter 구조를 바꾸는 것만으로 호환성이 깨질 수 있다.

---

## CHAPTER 01 · positional parameter는 순서를 public contract로 만든다

`connect(host, port, timeout)`처럼 위치 인자를 허용하면 호출자는 parameter 이름 없이 순서에 의존한다. 함수 내부에서 변수 이름을 바꾸는 것은 안전할 수 있지만 parameter 순서를 바꾸면 기존 호출의 의미가 달라진다. 특히 같은 타입의 값이 연속되면 프로그램이 즉시 실패하지 않고 잘못된 값으로 실행될 수 있어 더 위험하다. `rectangle(width, height)`에서 두 인자를 뒤집어도 둘 다 숫자라 type checker가 잡지 못하는 경우가 대표적이다.

위치 호출은 좌표나 수학 함수처럼 순서가 자연스럽고 인자가 적을 때 간결하다. 반면 boolean option, timeout, mode처럼 의미가 이름에 의존하는 값은 keyword 호출이 더 안전하다. API 설계는 작성하기 짧은가보다 호출자가 실수했을 때 얼마나 빨리 드러나는지를 기준으로 한다. 기존 공개 함수에 새 positional parameter를 중간에 끼워 넣는 변경은 과거 호출 전체의 의미를 흔들 수 있으므로 끝에 optional parameter를 추가하거나 keyword-only로 분리하는 방식이 호환성에 유리하다.

---

## CHAPTER 02 · positional-only는 parameter 이름을 구현 세부사항으로 남긴다

Python은 `/` 앞의 parameter를 positional-only로 선언할 수 있다. 이렇게 하면 호출자가 `name=value` 형태로 그 이름에 의존하지 못하므로 구현자가 parameter 이름을 바꾸더라도 source compatibility를 유지하기 쉽다. Built-in 함수처럼 이름보다 위치 자체가 의미 있고 오랫동안 유지되어야 하는 low-level API에서 유용하다.

반대로 domain API에서는 이름이 설명 역할을 한다. `transfer(source_account, destination_account, amount)`를 전부 positional-only로 만들면 호출 코드는 짧아지지만 source와 destination이 바뀌어도 눈에 잘 띄지 않는다. 어떤 부분을 public contract로 고정하고 어떤 부분을 숨길지 의도적으로 선택한다. Positional-only는 고급 문법이라서 사용하는 기능이 아니라 **호출자가 어떤 세부사항에 의존하도록 허용할지**를 정하는 도구다.

---

## CHAPTER 03 · keyword-only는 옵션 조합을 읽을 수 있는 호출로 만든다

`def export(data, *, compress=False, overwrite=False)`처럼 `*` 뒤의 parameter는 이름을 써야 호출할 수 있다. `export(rows, True, False)`보다 `export(rows, compress=True, overwrite=False)`가 어떤 정책을 선택했는지 분명하다. 같은 타입의 옵션이 많거나 기본값이 중요한 함수일수록 keyword-only가 잘못된 순서 전달을 구조적으로 막아 준다.

새 기능을 기존 API에 추가할 때도 keyword-only parameter는 변화 범위를 줄일 수 있다. 기존 호출자는 새 parameter를 생략하고 과거 behavior를 유지하며, 새 호출자만 이름을 통해 기능을 선택한다. 다만 default가 기존 semantics와 정확히 같아야 한다. Parameter가 optional하다는 사실과 생략 시 behavior compatibility가 보장된다는 사실은 다른 문제다. 새 default가 성능이나 보안 정책을 바꾼다면 source는 실행되어도 behavior compatibility는 깨진다.

---

## CHAPTER 04 · `*args`와 `**kwargs`는 유연성을 주는 대신 계약을 넓힌다

Decorator와 forwarding layer에서는 임의 인자를 받아 내부 callable에 전달해야 할 수 있다. 이때 `*args`, `**kwargs`는 원래 callable의 호출 surface를 보존하는 데 유용하다. 하지만 일반 업무 함수가 모든 입력을 가변 인자로 받으면 오타가 runtime 깊숙한 곳까지 전달되고 IDE와 type checker가 필요한 parameter를 알려 주기 어렵다.

Wrapper가 일부 keyword를 소비하고 나머지를 다음 layer로 넘긴다면 각 이름의 소유권을 정한다. 여러 decorator가 `timeout`이나 `retry` 같은 같은 이름을 서로 다른 의미로 해석하면 wrapper 순서에 따라 behavior가 바뀔 수 있다. 최종 경계에서는 예상하지 못한 keyword를 거부해 silent typo를 막는다. 유연한 forwarding은 외부 contract가 실제로 가변적일 때만 사용하고, domain function에는 가능한 한 구체적인 signature를 남긴다.

---

## CHAPTER 05 · default value는 생략 의미와 객체 lifetime을 동시에 가진다

Default parameter는 호출자가 인자를 생략했을 때 어떤 값을 사용할지 정한다. Immutable constant는 단순하지만 mutable default는 함수 정의 시점에 생성된 객체가 여러 호출에서 공유될 수 있어 state가 누적될 수 있다. 이 문제는 단순 Python 함정이 아니라 **signature가 object lifetime을 소유하는 사례**다.

`None`을 “인자가 제공되지 않음”의 sentinel로 자주 사용하지만 실제 domain에서 None이 유효한 값이면 상태를 구분할 수 없다. 예를 들어 cache update에서 `value=None`이 실제 저장 값이고 parameter 생략은 “변경하지 않음”이라면 별도의 고유 sentinel object가 필요하다. Default를 편의 문법으로 보지 않고 생략된 호출의 semantic state로 모델링하면 API가 더 정확해진다.

---

## CHAPTER 06 · 반환 타입과 exception도 signature 밖의 호출 계약이다

Source signature가 그대로여도 return shape가 `User`에서 `User | None`으로 바뀌면 호출자는 absence를 새로 처리해야 한다. Exception type이나 failure condition이 바뀌어도 behavior compatibility가 달라진다. Type annotation은 이 변화를 일부 표현하지만 side effect와 performance, idempotency까지 모두 담지는 못한다.

API evolution을 검토할 때 parameter 목록만 diff하지 않는다. 정상 반환, 실패 반환, exception, 입력 객체 mutation, 외부 I/O, latency expectation을 함께 비교한다. `save()`가 이전에는 idempotent했지만 새 version부터 호출마다 새 record를 만든다면 signature는 완전히 같아도 caller contract는 깨진다. Public API는 문법 표면과 실행 semantics를 합친 개념이다.

---

## CHAPTER 07 · overload는 하나의 구현에 여러 정적 호출 관계를 표현한다

같은 함수가 argument 형태에 따라 다른 return type을 제공할 수 있다면 overload annotation으로 caller 관점의 관계를 정밀하게 표현할 수 있다. 예를 들어 `open_item(id)`는 Item을, `open_item(ids)`는 list[Item]을 돌려주는 API를 모델링할 수 있다. 실제 runtime 구현은 하나일 수 있지만 static type checker는 호출별 결과를 더 정확히 추론한다.

Overload가 너무 많아지면 하나의 함수가 사실상 여러 책임을 가진 신호일 수 있다. `str`, bytes, path, file object, URL을 모두 한 함수에서 받고 return도 여러 형태라면 caller와 구현 모두 state space가 커진다. Overload는 복잡성을 숨기는 도구가 아니라 이미 존재하는 안정된 호출 관계를 문서화할 때 사용한다. 가능하면 서로 다른 의미의 operation은 이름도 분리해 runtime와 static model을 단순하게 유지한다.

---

## CHAPTER 08 · decorator는 원래 callable의 signature metadata를 보존해야 한다

Decorator가 `def wrapper(*args, **kwargs)`만 노출하면 runtime 호출은 가능해도 원래 함수의 이름, docstring, annotation, signature를 reflection tool이 잃을 수 있다. CLI generator, dependency injection framework, API documentation tool이 signature를 읽는다면 metadata 손실은 표시 문제가 아니라 실제 기능 오류가 된다.

`functools.wraps`는 runtime metadata를 보존하는 데 도움을 주고, typing의 ParamSpec 계열은 decorator가 받은 callable의 parameter 관계를 static하게 전달하는 데 사용될 수 있다. 두 층은 목적이 다르다. Decorator를 작성할 때 wrapper가 원래 callable의 입력·출력 contract를 실제로 유지하는지 확인하고, retry나 cache처럼 semantics를 바꾸는 경우 그 변화도 문서화한다.

---

## CHAPTER 09 · deprecation은 호환성 shim과 제거 조건을 함께 가진다

공개 함수나 parameter 이름을 바꿔야 한다면 즉시 삭제하는 대신 일정 기간 old form을 받아 warning을 내고 migration path를 제공할 수 있다. Warning에는 새 API와 제거 예정 version을 포함해 사용자가 언제까지 무엇을 바꿔야 하는지 알 수 있게 한다. Deprecated path가 남아 있는 동안에는 실제로 동작하는지 regression test도 필요하다.

Compatibility shim을 영구 유지하면 code path가 늘어나고 새 기능이 두 API를 모두 고려해야 해 복잡성이 쌓인다. Telemetry, repository search, support policy를 사용해 old API 사용량을 파악하고 제거 기준을 정한다. Deprecation은 삭제를 미루는 것이 아니라 **호환성을 깨지 않고 의존성을 이동시키는 상태 전이**다.

---

## CHAPTER 10 · 좋은 signature는 잘못된 호출을 어렵게 만든다

함수 사용자가 구현을 읽지 않아도 parameter 구조만 보고 필수 값, 선택 옵션, 서로 다른 domain value를 구분할 수 있으면 오류 공간이 줄어든다. 같은 primitive positional argument가 길게 이어지거나 boolean flag가 계속 추가된다면 signature 자체를 재설계할 신호다. Value object, keyword-only option, 별도 command object, 명확히 분리된 method가 더 안전할 수 있다.

Signature 설계는 syntax 취향이 아니라 API의 가능한 호출 상태를 제한하는 작업이다. Public 함수가 진화할 때는 source compatibility와 behavior compatibility를 따로 검토하고, static type·runtime validation·contract test를 겹쳐 사용한다. **잘 설계된 호출 surface는 올바른 사용을 쉽게 하고 잘못된 조합은 가능한 한 호출 시점 가까이에서 실패하게 한다.**