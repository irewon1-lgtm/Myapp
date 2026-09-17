# PART 04 · 함수와 추상화 — 호출 계약, scope, closure, 재귀, 부작용을 한 모델로 연결하기

함수는 코드를 여러 줄 묶어 이름을 붙이는 기능보다 훨씬 큰 역할을 한다. 입력을 어떤 방식으로 받는지, 지역 상태가 어디까지 살아 있는지, 결과와 실패가 어떻게 전달되는지, 바깥 상태를 변경하는지에 따라 프로그램의 의존성 구조가 달라진다. 함수 호출을 정확히 이해하면 작은 script에서 시작해 모듈과 객체, 비동기 코드, 테스트 가능한 설계로 자연스럽게 확장할 수 있다.

---

## CHAPTER 01 · 함수 정의는 재사용보다 먼저 호출 계약을 만든다

함수는 호출자가 내부 구현을 모두 알지 않아도 사용할 수 있게 하는 경계다. 좋은 함수 경계에는 어떤 인자를 허용하는지, 반환값이 무엇을 뜻하는지, 실패하면 어떤 방식으로 알리는지, 외부 상태를 변경하는지에 대한 계약이 있다. 함수 이름과 parameter 목록은 그 계약의 압축된 표현이다.

`calculate_total(lines, tax_policy)`라는 함수가 있다면 `lines`가 비어도 되는지, 이미 검증된 품목만 들어오는지, `tax_policy`가 어떤 callable 형태인지, 결과가 원 단위 정수인지 소수인지가 실제 의미를 결정한다. 함수 body가 짧아도 이런 계약이 모호하면 호출자가 매번 소스 코드를 읽어야 한다.

반대로 너무 많은 상황을 하나의 함수가 처리하려고 optional parameter와 flag를 늘리면 호출 계약이 조합 폭발을 일으킨다. `save(data, validate=True, notify=False, async_mode=False, overwrite=None)`처럼 여러 flag가 독립적으로 조합되면 실제로 몇 종류의 함수를 한 인터페이스에 숨긴 셈이 될 수 있다. 동작의 이유가 다르면 별도 함수나 객체로 나누는 편이 의미가 선명하다.

함수의 추상화 수준도 맞춰야 한다. 하나의 함수 안에서 “주문을 승인한다” 같은 업무 단계와 “문자열에서 세 번째 문자를 자른다” 같은 세부 구현이 뒤섞이면 읽는 사람이 여러 수준을 동시에 오가야 한다. 상위 함수는 하위 작업의 의도를 조합하고, 세부 변환은 별도 경계로 내려 보내면 제어 흐름이 설명문처럼 읽힌다.

---

## CHAPTER 02 · parameter binding은 값을 복사하는 규칙이 아니라 객체 참조를 연결하는 규칙이다

Python 함수 호출에서 argument expression은 호출 전에 평가되고 그 결과 객체가 함수의 parameter 이름에 바인딩된다. “값으로 전달”이나 “참조로 전달” 같은 다른 언어의 용어를 그대로 가져오면 mutable object에서 혼동이 생길 수 있다. Python 모델에서는 **객체가 전달되고 새로운 지역 이름이 그 객체에 바인딩된다**고 보는 편이 정확하다.

```python
def rebind(xs):
    xs = [99]

def mutate(xs):
    xs.append(99)
```

첫 함수는 지역 이름 `xs`를 새 리스트에 다시 바인딩하므로 호출자의 이름이 가리키는 객체는 변하지 않는다. 두 번째 함수는 parameter와 호출자가 공유하는 기존 리스트 객체를 수정하므로 바깥에서도 변경이 보인다. 같은 인자 전달 방식에서 재바인딩과 mutation의 차이 때문에 결과가 달라진다.

이 모델은 API 설계와도 연결된다. 함수 이름만 보고 입력 객체를 수정하는지 새 값을 반환하는지 알 수 있어야 한다. `sort()`와 `sorted()`처럼 in-place와 new-value 동작을 구분하는 이름은 호출자가 aliasing 효과를 예측하는 데 도움을 준다. 함수가 전달받은 collection을 몰래 수정하면 나중에 같은 객체를 사용하는 다른 코드가 영향을 받을 수 있다.

parameter binding을 이해하면 nested mutable default, callback, closure 같은 주제도 설명하기 쉬워진다. 중요한 것은 “변수 안에 값이 들어간다”는 그림보다 어느 이름들이 동일 객체를 공유하고 어떤 코드가 그 객체를 수정할 수 있는지를 추적하는 것이다.

---

## CHAPTER 03 · 기본 인자는 함수가 호출될 때마다 새로 만들어진다고 가정하지 않는다

Python의 default argument expression은 함수 정의가 실행될 때 평가되어 함수 객체에 연결된다. 따라서 mutable object를 기본값으로 두면 여러 호출이 같은 객체를 공유할 수 있다. 이 semantics를 모르면 “이번 호출에서 만든 리스트가 왜 다음 호출에도 남아 있지?” 같은 버그가 발생한다.

```python
def collect(value, bucket=[]):
    bucket.append(value)
    return bucket
```

이 함수의 `bucket`은 호출마다 새 리스트가 아니다. 기본 인자를 사용한 호출들이 같은 리스트를 계속 수정한다. 일반적으로 호출마다 새 mutable object가 필요하다면 `None` 같은 sentinel을 기본값으로 두고 함수 안에서 새 객체를 만든다.

```python
def collect(value, bucket=None):
    if bucket is None:
        bucket = []
    bucket.append(value)
    return bucket
```

반대로 정의 시점에 값을 고정하고 싶은 경우에는 이 semantics가 유용할 수도 있다. 중요한 것은 우연이 아니라 의도다. default argument가 언제 평가되는지 알고, shared state가 필요한지 호출별 state가 필요한지 선택해야 한다.

기본값은 API의 호환성에도 영향을 준다. parameter를 추가하면서 안전한 default를 제공하면 기존 호출을 유지할 수 있지만, default가 도메인 의미를 숨기면 새로운 요구를 잘못 처리할 수 있다. “값을 생략했음”과 “명시적으로 None을 전달함”을 구분해야 하는 API라면 별도 sentinel이 필요할 수 있다.

---

## CHAPTER 04 · scope는 이름을 찾는 규칙이고 객체 lifetime과 같은 개념이 아니다

함수 안에서 이름을 사용할 때 Python은 정해진 namespace 규칙에 따라 binding을 찾는다. 지역 이름, 둘러싼 함수의 이름, module global, built-in 같은 범위가 서로 다른 역할을 한다. scope를 이해하지 못하면 같은 이름이 여러 곳에 있을 때 어떤 값을 읽는지, 대입이 어느 binding을 만드는지 예측하기 어렵다.

지역 변수는 함수 호출 frame과 연결되지만 객체의 lifetime은 그 객체에 대한 참조가 남아 있는지에 따라 더 길어질 수 있다. 함수가 종료되어 지역 이름이 사라져도 반환된 객체가 내부 객체를 참조하거나 closure가 지역 값을 붙잡고 있으면 데이터는 계속 살아 있을 수 있다. scope와 memory lifetime을 동일시하면 closure와 generator에서 혼동이 생긴다.

global state는 접근이 쉬워 보이지만 함수의 숨은 입력과 출력을 만든다. parameter에 없는 설정값을 읽고 global collection을 수정하는 함수는 호출 signature만 보고 동작을 이해하기 어렵다. 테스트는 실행 순서에 영향을 받고 병렬 실행이나 재사용도 어려워질 수 있다. 필요한 값은 가능한 한 명시적으로 전달하고, 공유 상태가 필요한 경우 책임이 분명한 경계에 둔다.

`global`과 `nonlocal`은 특정 binding을 재바인딩하겠다는 선언이지 객체 mutation을 허용하는 특별 권한이 아니다. outer scope의 list를 `append`하는 것과 그 이름 자체에 새 list를 대입하는 것은 다른 문제다. 다시 한 번 재바인딩과 mutation을 분리하면 scope 규칙이 정리된다.

---

## CHAPTER 05 · 함수는 값이기 때문에 저장·전달·반환할 수 있다

Python에서 함수 객체는 변수에 바인딩하고 collection에 넣으며 다른 함수의 argument로 전달하거나 반환할 수 있다. 이 성질은 behavior를 데이터처럼 조합하게 만든다. 정렬 기준, validation policy, retry condition, 이벤트 handler처럼 “어떤 동작을 할지”를 호출자에게 주입할 수 있다.

```python
def apply_discount(amount, policy):
    discount = policy(amount)
    return amount - discount
```

이 함수는 특정 할인 규칙에 고정되지 않는다. `policy`가 callable이라는 계약만 만족하면 서로 다른 전략을 전달할 수 있다. 이 구조는 거대한 `if type == ...` 분기를 줄이고 정책을 독립적으로 테스트하게 한다.

고차 함수는 강력하지만 지나치게 많은 익명 함수와 동적 조합은 실제 호출 경로를 숨길 수 있다. callback이 어디서 왔는지 찾기 어렵고 exception stack이 복잡해질 수도 있다. 동작을 값으로 다룰 때도 이름, 타입 힌트, 책임 경계를 분명히 해 실행 흐름을 추적 가능하게 유지한다.

함수가 값을 캡처하는 closure와 결합되면 설정된 behavior를 만드는 factory 패턴도 가능하다. 예를 들어 세율을 받아 계산 함수를 반환할 수 있다. 이때 capture되는 값이 immutable인지 mutable인지에 따라 이후 상태 변화가 결과에 영향을 줄 수 있으므로 closure는 단순 문법 축약이 아니라 state ownership 문제와 연결된다.

---

## CHAPTER 06 · closure는 코드와 lexical environment를 함께 보존한다

안쪽 함수가 바깥 함수의 지역 이름을 참조하고 그 안쪽 함수가 바깥으로 반환되면, 바깥 함수 호출이 끝난 뒤에도 필요한 binding이 closure를 통해 유지될 수 있다. 이는 함수가 정의된 lexical environment를 함께 기억하기 때문이다. closure는 private state, callback configuration, decorator 구현 등에 사용된다.

```python
def make_multiplier(factor):
    def multiply(value):
        return value * factor
    return multiply

times3 = make_multiplier(3)
```

`make_multiplier` 호출 frame 자체는 끝나지만 `times3`가 사용하는 `factor` binding은 필요한 방식으로 보존된다. 이를 “지역 변수는 함수 끝나면 무조건 사라진다”는 규칙으로 설명할 수 없다. lifetime은 참조 관계와 실행 모델을 함께 봐야 한다.

loop 안에서 closure를 여러 개 만들 때 late binding 문제가 나타날 수 있다. closure가 각 반복 시점의 값을 복사한다고 가정했지만 실제로는 동일 binding을 나중에 읽어 모두 같은 최종값을 보게 되는 패턴이다. 캡처 semantics를 이해하고 default argument나 별도 factory를 사용해 호출별 값을 고정할 수 있다.

closure가 mutable state를 감추면 편리하지만 상태 변화 경로가 숨을 수도 있다. 여러 함수가 같은 enclosed object를 수정하면 작은 객체 시스템과 비슷한 복잡성이 생긴다. 읽기 전용 configuration capture와 변경 가능한 private state를 구분하고, 후자의 경우 동시성·테스트·reset 정책까지 고려한다.

---

## CHAPTER 07 · 재귀는 함수가 자기 자신을 부르는 문법이 아니라 문제 구조를 자기 유사하게 표현한다

재귀는 큰 문제를 같은 형태의 더 작은 문제로 바꾸고 base case에서 멈추는 방식이다. 트리 순회, 중첩 자료구조, divide-and-conquer처럼 데이터 구조 자체가 재귀적인 경우 자연스럽게 모델링된다. 재귀의 정확성을 보려면 base case, reduction step, 종료 measure 세 요소를 확인한다.

팩토리얼 예제보다 실용적인 관점은 파일 트리나 AST처럼 각 node가 child node를 포함하는 구조다. `visit(node)`가 현재 node를 처리하고 각 child에 대해 같은 함수를 호출하면 데이터 구조와 알고리즘 모양이 맞는다. 다만 깊이가 매우 큰 입력에서는 call stack이 많이 쌓이고 Python의 recursion limit에 도달할 수 있다.

재귀 호출마다 새 frame이 생기므로 local variable, return point, parameter가 중첩된다. tail-recursive 형태라고 해도 Python이 일반적으로 이를 반복문처럼 자동 최적화한다고 기대해서는 안 된다. 입력 깊이가 외부에서 통제되거나 매우 커질 수 있다면 명시적 stack과 iteration으로 바꾸는 설계를 고려한다.

재귀를 사용할지 반복을 사용할지는 코드 길이보다 데이터 구조, 최대 깊이, 실패 모드, 디버깅 가능성으로 결정한다. 재귀는 우아함 자체가 목적이 아니라 문제의 구조와 제어 구조를 맞추는 한 가지 도구다.

---

## CHAPTER 08 · pure function과 side effect를 분리하면 테스트 공간이 줄어든다

같은 입력에 같은 출력을 반환하고 외부에서 관찰 가능한 상태를 바꾸지 않는 함수는 reasoning이 단순하다. 입력과 출력만 비교하면 되고 실행 순서가 결과에 미치는 영향도 적다. 반면 파일 쓰기, 데이터베이스 변경, 네트워크 요청, 현재 시각 읽기, random 사용은 함수 결과가 외부 환경에 의존하거나 환경을 변경하게 한다.

실제 프로그램에서 side effect를 제거할 수는 없다. 목표는 계산 핵심과 effect boundary를 분리하는 것이다. 예를 들어 `calculate_invoice(order, tax_table)`는 순수 계산으로 만들고, `load_tax_table()`과 `save_invoice()`를 바깥에서 수행할 수 있다. 그러면 계산 규칙을 빠르게 테스트하고 외부 시스템 오류는 별도 integration test에서 다룰 수 있다.

현재 시각도 숨은 입력이다. 함수 안에서 직접 `now()`를 읽으면 특정 시각 경계 테스트가 어려워진다. 필요한 시각을 parameter로 전달하거나 clock abstraction을 사용하면 동일 입력을 재현할 수 있다. random, environment variable, global configuration도 같은 원리로 명시적 의존성 후보가 된다.

순수성과 비순수성을 도덕적 구분으로 보지 않는다. 네트워크 요청 함수는 본질적으로 effectful하다. 중요한 것은 어떤 함수가 어떤 효과를 일으키는지 인터페이스에서 예측할 수 있고, 계산 로직이 불필요하게 그 효과에 묶이지 않는 구조다.

---

## CHAPTER 09 · 함수 크기는 줄 수보다 응집도와 추상화 수준으로 판단한다

“함수는 몇 줄 이하여야 한다” 같은 규칙은 기계적 지표일 뿐 좋은 경계를 보장하지 않는다. 40줄이지만 하나의 명확한 parser step을 수행하는 함수가 5줄짜리 함수 열 개로 조각난 코드보다 이해하기 쉬울 수 있다. 판단 기준은 함수 안의 문장들이 하나의 책임을 설명하고 같은 수준의 추상화에 있는가다.

함수 이름이 body의 모든 주요 단계를 자연스럽게 설명하면 응집도가 높을 가능성이 있다. 반대로 이름에 `and`, `then`, `handle_everything` 같은 표현이 필요하거나 서로 다른 외부 시스템을 여러 개 직접 다룬다면 분리 후보가 된다. 지역 변수의 묶음이 서로 다른 그룹으로 나뉘는 것도 여러 책임이 섞였다는 신호일 수 있다.

과도하게 작은 함수는 navigation cost를 만든다. 값을 한 번 전달하기 위해 파일 여러 곳을 오가고, 이름만으로는 실제 동작을 알 수 없으며, 함수 호출 계층이 깊어질 수 있다. 추출은 중복 제거보다 개념 경계를 만드는 데 사용한다. 재사용되지 않더라도 복잡한 계산에 이름을 붙여 상위 흐름을 명확하게 만든다면 가치가 있다.

함수 경계를 리팩터링할 때는 기존 behavior를 보호하는 테스트가 중요하다. 구조를 바꾸는 작업과 기능을 바꾸는 작업을 분리하면 문제가 생겼을 때 원인을 좁힐 수 있다. 함수 설계는 처음부터 완벽하게 결정하는 것이 아니라 코드가 드러내는 변화 축을 보며 개선하는 과정이다.

---

## CHAPTER 10 · 함수 계약은 AI 생성 코드의 검증 단위가 된다

AI에게 “이 기능 만들어 줘”라고만 지시하면 구현 선택의 자유도가 매우 크다. 함수 단위로 input type, invariant, side effect, error behavior, performance boundary를 제시하면 생성 결과를 비교할 기준이 생긴다. 이는 prompt 기술보다 software contract의 문제다.

예를 들어 `parse_quantity(text)`에 대해 “공백 허용, 십진 정수만 허용, 1..999 범위, 실패 시 ValueError, 외부 상태 변경 없음”이라고 정의하면 정상 예제와 반례를 자동으로 만들 수 있다. 생성된 코드가 `float`를 경유해 `"1.5"`를 1로 받아들이거나 음수를 절댓값으로 바꾼다면 계약 위반을 즉시 찾을 수 있다.

AI가 만든 함수가 지나치게 많은 역할을 갖는 경우에도 계약 분해가 유용하다. parsing, validation, DB 저장, UI 메시지를 한 함수에 넣었다면 각 단계의 계약을 별도 함수로 나누도록 요구할 수 있다. 사람이 모든 코드를 직접 쓰지 않더라도 구조와 검증 기준을 소유하면 생성 결과를 통제할 수 있다.

함수는 그래서 학습 단위이자 위임 단위다. parameter와 return만 보는 수준을 넘어 state ownership, failure semantics, complexity, effect boundary까지 명시하면 코드 작성자가 사람인지 AI인지에 관계없이 더 예측 가능한 프로그램을 만들 수 있다.