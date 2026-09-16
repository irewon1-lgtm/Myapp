# PART 02 · 소스 코드가 실행 중인 프로세스가 되기까지

`print("hello")` 한 줄을 실행한다고 해서 CPU가 `print`라는 영어 단어를 이해하는 것은 아니다. 사람에게 읽기 좋은 소스 코드는 여러 해석·변환 계층을 지나고, 운영체제가 실행에 필요한 주소 공간과 자원을 준비하고, CPU가 기계 명령을 반복해서 수행해야 실제 동작이 일어난다.

이 PART에서는 `코드를 쓴다 → 실행 버튼을 누른다 → 결과가 나온다` 사이의 검은 상자를 연다. 목표는 컴파일러 이론 전체를 배우는 것이 아니라, **오류가 어느 계층에서 생겼는지 구분하고 실행 중인 프로그램의 상태를 추적할 수 있는 모델**을 만드는 것이다.

---

## CHAPTER 01 · 소스 코드는 실행 그 자체가 아니다

### 소스 파일은 우선 문자 데이터다

다음 Python 파일을 생각하자.

```python
price = 10000
quantity = 3
print(price * quantity)
```

디스크에 저장된 `order.py`는 처음에는 UTF-8 같은 인코딩으로 저장된 **바이트열**이다. 편집기는 그 바이트를 문자로 decode해서 우리에게 소스 텍스트로 보여 준다.

즉 첫 단계부터 이미 PART 01의 표현 계층이 적용된다.

```text
파일의 bytes
→ text encoding으로 decode
→ source characters
```

파일 인코딩이 깨지면 parser가 프로그램 문법을 보기 전에 이미 잘못된 문자를 받을 수 있다. 반대로 화면에는 멀쩡해 보여도 눈에 잘 구별되지 않는 Unicode 문자가 식별자나 문자열 안에 들어갈 수 있다. `코드`라고 부르는 순간에도 결국 표현 규칙 위에 서 있다.

### lexer와 parser의 목적을 문법 용어보다 먼저 이해한다

프로그래밍 언어 구현은 소스 텍스트를 단순한 문자열 덩어리로 계속 다루지 않는다. 언어 문법에 따라 의미 있는 구조를 찾아야 한다.

예를 들어:

```python
price * quantity
```

를 읽을 때 사람은 대략 `price라는 이름`, `곱셈 연산자`, `quantity라는 이름`을 구분한다. 구현 세부는 언어마다 다르지만 일반적인 컴파일러/인터프리터 설명에서는 소스를 token으로 나누고, 문법에 맞춰 더 구조적인 표현으로 만든다.

개념적으로는 다음과 같이 볼 수 있다.

```text
문자열
price * quantity

↓ tokenization / lexical analysis

IDENT(price)
STAR(*)
IDENT(quantity)

↓ parsing

Multiply
├─ Name(price)
└─ Name(quantity)
```

이런 트리 형태를 흔히 abstract syntax tree, AST라고 부른다.

여기서 중요한 점은 `AST라는 단어`가 아니다. **프로그램이 소스 글자를 그대로 하나씩 실행하는 것이 아니라, 언어 규칙에 맞는 구조로 해석한 뒤 그 구조를 이용한다**는 점이다.

### syntax error는 실행 중 오류와 다른 계층이다

다음 코드를 보자.

```python
if price > 10000
    print(price)
```

언어 문법상 필요한 구분자가 빠졌다면 parser 단계에서 프로그램 구조를 완성할 수 없다. 이런 오류는 실행이 시작된 뒤 특정 사용자 입력 때문에 생긴 것이 아니다.

반면 다음 코드는 문법적으로는 올바를 수 있다.

```python
quantity = 0
print(100 / quantity)
```

문법 구조는 만들 수 있지만 실제 연산 중 0으로 나누는 문제가 생긴다.

두 오류를 구분해야 디버깅 질문도 달라진다.

```text
syntax/parse 문제
→ 언어 문법상 프로그램 구조를 만들 수 있는가?

runtime 문제
→ 구조는 유효하지만 특정 실행 상태에서 연산을 계속할 수 있는가?
```

`에러가 났다`로 하나로 묶으면 조사 범위가 넓어진다.

---

## CHAPTER 02 · 컴파일, 해석, 가상 머신은 한 줄짜리 구호보다 다양하다

### compiled와 interpreted를 이분법으로만 외우면 금방 틀린다

초보 설명에서는 흔히 다음처럼 나눈다.

```text
C = 컴파일 언어
Python = 인터프리터 언어
```

첫 구분에는 도움이 되지만 실제 구현을 설명하기에는 너무 거칠다.

어떤 언어 구현은 소스를 기계어로 미리 컴파일한다. 어떤 구현은 bytecode 같은 중간 표현으로 바꾼 뒤 virtual machine이 실행한다. 어떤 runtime은 자주 실행되는 부분을 실행 중에 native code로 JIT compile하기도 한다. 같은 언어도 구현과 플랫폼에 따라 전략이 달라질 수 있다.

따라서 더 안전한 질문은 이것이다.

```text
내가 쓰는 이 구현은 소스를 어떤 중간 표현으로 바꾸는가?
실제 CPU가 수행할 native instruction은 언제 만들어지는가?
실행에 어떤 runtime이 필요한가?
```

### native machine code는 CPU가 정의한 명령 형식을 따른다

CPU는 특정 instruction set architecture, ISA가 정한 명령 형식을 이해한다. x86-64, Arm 계열처럼 서로 다른 ISA가 있고 같은 소스 프로그램도 target ISA에 따라 다른 machine code로 만들어진다.

그래서 `Windows용 exe면 아무 CPU에서나 동일하게 실행된다`고 생각하면 안 된다. 운영체제 파일 형식뿐 아니라 architecture도 맞아야 한다.

Android 앱 배포에서도 native library가 있다면 ABI별 빌드가 중요한 이유가 여기에 있다. Java/Kotlin 코드가 Android Runtime 위에서 실행되는 부분과 C/C++ native library가 CPU ABI에 직접 묶이는 부분은 배포 특성이 다르다.

### 중간 표현은 목적이 있다

소스를 바로 모든 CPU용 machine code로 만드는 것만이 유일한 설계는 아니다. 중간 표현을 두면 언어 front-end와 target back-end 사이를 분리하거나, 최적화하기 쉬운 구조를 만들거나, portable runtime을 설계할 수 있다.

개념적으로:

```text
source language
↓
intermediate representation
↓
target-specific code
```

같은 구조를 가질 수 있다.

이 분리는 나중에 `frontend`, `backend`라는 말을 다른 분야에서 만날 때도 좋은 사고 모델이 된다. 한 시스템이 입력 의미를 분석하는 부분과 특정 실행 환경에 맞게 결과를 만드는 부분을 분리하는 것이다.

---

## CHAPTER 03 · 실행 파일은 코드만 담긴 한 덩어리가 아니다

### 프로그램에는 명령 외에도 데이터와 메타데이터가 필요하다

native executable을 단순히 `기계어 파일`이라고만 부르면 부족하다. 실제 실행 파일 형식에는 loader가 프로그램을 메모리에 배치하고 시작하기 위해 필요한 여러 정보가 들어간다.

운영체제와 executable format에 따라 이름과 구조는 다르지만 다음과 같은 범주를 생각할 수 있다.

```text
실행할 코드
초기화된 데이터
초기값이 0인 데이터에 대한 정보
symbol / relocation 관련 정보
동적 라이브러리 의존 정보
entry point
segment/section 배치 정보
권한 정보
```

`파일을 CPU가 통째로 읽어서 실행한다`가 아니다. 운영체제 loader가 파일 형식을 해석하고 실행에 필요한 메모리 mapping을 만든 뒤 정해진 시작점으로 제어를 넘긴다.

### object file과 executable 사이에 linker가 있다

C/C++ 같은 전형적인 ahead-of-time native build를 단순화하면 여러 source file을 각각 compile해 object file을 만들고, linker가 이들을 연결해 executable이나 library를 만든다.

```text
main.c     math.c
  ↓          ↓
main.o     math.o
   \        /
     linker
       ↓
    executable
```

`main.c`가 `calculate_total()`을 호출하지만 그 함수의 구현은 `math.c`에 있을 수 있다. 각 object file을 만들 때 모든 최종 주소가 정해지지 않았다면 linker가 symbol reference를 해결하고 최종 배치를 반영해야 한다.

### undefined reference는 parser 오류와 다르다

소스 문법이 맞고 compiler가 각 파일을 object file로 만들 수 있어도 필요한 symbol의 구현을 link 단계에서 찾지 못하면 실패할 수 있다.

```text
컴파일 성공
≠
링크 성공
≠
프로그램 실행 성공
```

이 세 단계는 서로 다른 증거다.

예를 들어 header declaration만 있고 실제 implementation object/library가 link input에 없다면 compile은 통과하고 link가 실패할 수 있다. 반대로 link까지 성공해도 실행 시 동적 라이브러리를 찾지 못하거나 환경 설정이 잘못되어 시작에 실패할 수 있다.

### static linking과 dynamic linking은 의존 코드를 가져오는 시점이 다르다

정적으로 연결하면 필요한 코드 일부가 최종 바이너리에 포함될 수 있다. 동적 연결에서는 실행 파일이 shared library를 사용하도록 기록하고 실행 시 loader/dynamic linker가 이를 mapping하고 symbol을 연결한다.

두 방식에는 바이너리 크기, 업데이트, 메모리 공유, 배포 호환성 등 여러 trade-off가 있다. `static이 무조건 안전`, `dynamic이 무조건 가볍다`처럼 하나의 문장으로 결론내리면 안 된다.

실무에서 중요한 디버깅 질문은 다음처럼 구체적이다.

```text
이 symbol은 어느 binary/library가 제공해야 하는가?
실제로 어떤 library version이 load되었는가?
search path는 무엇인가?
build 시점 의존성과 실행 시점 의존성이 같은가?
ABI가 호환되는가?
```

---

## CHAPTER 04 · 프로그램을 실행하면 파일이 아니라 프로세스를 다루게 된다

### program과 process를 구분한다

디스크의 executable은 정적인 파일이다. 실행되면 운영체제는 그 프로그램의 실행 인스턴스를 관리한다. 이를 process라고 부른다.

같은 프로그램을 두 번 실행하면 같은 파일에서 시작해도 서로 다른 process가 생길 수 있다.

```text
program file: app

실행 1 → process A
실행 2 → process B
```

두 process는 각각 자신의 실행 상태를 가진다.

- register 상태
- virtual address space
- stack
- heap
- open file descriptor/handle
- thread
- scheduling state
- 권한/credentials

`프로그램이 RAM에 올라간다`는 초보 표현을 더 정확하게 확장하면, 운영체제가 **프로세스라는 실행 컨텍스트와 virtual address space를 만들고 필요한 code/data mapping과 runtime 상태를 준비한다**고 보는 편이 좋다.

### virtual address는 물리 RAM 번호와 같지 않다

프로세스가 보는 pointer 주소를 `RAM 칩의 실제 위치 번호`라고 생각하면 곧 막힌다. 현대 OS는 virtual memory를 사용해 각 프로세스에 virtual address space를 제공한다.

프로세스는 자신에게 주어진 virtual address를 사용하고, page table 등의 메커니즘을 통해 실제 physical memory frame 또는 다른 backing과 연결된다.

이 추상화가 주는 중요한 효과는 다음과 같다.

- 서로 다른 프로세스의 주소 공간을 격리할 수 있다.
- 각 프로세스는 비교적 일관된 주소 모델을 사용할 수 있다.
- 파일을 메모리 주소 공간에 mapping할 수 있다.
- 실제 RAM보다 큰 virtual space를 관리할 수 있다.
- page 단위 보호 권한을 적용할 수 있다.

`virtual`이라는 말이 `가짜라서 느리다`는 뜻은 아니다. **프로그램이 사용하는 주소 모델과 물리 저장장치 배치를 분리하는 핵심 추상화**다.

### 주소 공간을 code/stack/heap 세 칸으로만 외우지 않는다

교육용 그림에서는 흔히 다음 영역을 그린다.

```text
code
static/global data
heap
...
stack
```

이 그림은 시작점이지만 실제 process memory map에는 executable segment, shared library, memory-mapped file, anonymous mapping, thread stack 등 다양한 mapping이 존재할 수 있다.

중요한 것은 `heap은 아래에서 위`, `stack은 위에서 아래` 같은 그림을 모든 시스템의 절대 법칙처럼 외우는 것이 아니다. 다음 질문이 더 중요하다.

```text
이 주소 구간은 무엇이 mapping한 것인가?
read/write/execute 권한은 무엇인가?
private인가 shared인가?
파일 backing이 있는가 anonymous memory인가?
```

디버거와 `/proc` 계열 정보, platform profiler가 memory map을 보여 주는 이유가 여기에 있다.

---

## CHAPTER 05 · stack과 heap은 수명과 관리 방식이 다르다

### 함수 호출은 return할 위치와 지역 상태를 관리해야 한다

다음 함수를 보자.

```c
int add(int a, int b) {
    int result = a + b;
    return result;
}
```

실제 machine-level 구현은 compiler/ABI/optimization에 따라 달라지지만 함수 호출에는 최소한 `어디에서 호출했는지`, `인자를 어디서 받는지`, `결과를 어떻게 돌려주는지`, `호출 중 필요한 임시 상태를 어디에 보관하는지` 같은 규칙이 필요하다.

이 규칙을 calling convention이라고 부르는 체계가 정한다. 일부 값은 register로 전달되고, 필요한 경우 stack memory가 사용된다.

따라서 stack을 `지역변수 넣는 곳` 한 문장으로만 외우면 부족하다. **호출 관계와 제한된 수명의 실행 상태를 관리하는 데 핵심적으로 쓰이는 영역**이라고 이해한다.

### stack frame은 함수마다 항상 똑같이 생기지 않는다

최적화된 compiler는 지역변수를 register에만 둘 수 있고, 작은 함수를 inline해서 실제 호출 자체를 없앨 수도 있다. frame pointer를 생략할 수도 있다.

그래서 source-level 그림과 machine-level 실제 layout은 다를 수 있다.

이 차이는 디버깅에서 중요하다. 최적화된 release build를 debugger로 볼 때 source variable이 `optimized out`되거나 실행 순서가 소스와 직관적으로 맞지 않을 수 있다.

### recursion이 stack 문제와 연결되는 이유

재귀 함수는 자기 자신을 다시 호출한다.

```python
def countdown(n):
    if n == 0:
        return
    countdown(n - 1)
```

각 호출이 반환되기 전까지 호출 상태가 누적되면 제한된 stack 자원을 소모한다. 언어/runtime은 recursion limit을 둘 수도 있고, 너무 깊은 호출은 stack overflow로 이어질 수 있다.

`재귀는 느리다`가 핵심이 아니라 **호출 깊이가 자원 사용량에 어떤 영향을 주는지**를 이해해야 한다.

### heap은 동적 수명의 객체를 관리하는 일반적인 공간이다

함수 호출 범위를 넘어 살아야 하거나 크기를 실행 중 결정하는 데이터는 동적 메모리 관리가 필요하다. 저수준 언어에서는 allocator에게 memory block을 요청하고 직접 해제할 수 있고, garbage-collected runtime에서는 객체 reachability 등을 기준으로 자동 회수한다.

하지만 `GC가 있으니 메모리 누수는 없다`고 생각하면 안 된다. 더 이상 필요 없는 객체라도 프로그램이 reference를 계속 보관하면 collector 입장에서는 아직 살아 있는 객체다.

예를 들어 전역 cache가 모든 요청 결과를 영원히 보관한다면 reference가 계속 남아 memory usage가 증가할 수 있다.

```text
객체를 free하지 않았다
```

만이 leak의 유일한 형태가 아니다.

```text
논리적으로는 필요 없지만 reachable reference가 남아 있다
```

도 managed runtime에서 중요한 memory retention 문제다.

---

## CHAPTER 06 · CPU는 명령을 수행하며 상태를 바꾼다

### CPU 실행을 '계산한다'보다 구체적으로 본다

CPU를 일하는 사람이라고 비유하는 단계에서 이제 내려온다. 프로그램 실행은 instruction sequence가 CPU architecture가 정의한 방식으로 **machine state를 변화시키는 과정**으로 볼 수 있다.

machine state에는 다음과 같은 것들이 포함된다.

- general-purpose register 값
- program counter / instruction pointer
- condition flags
- vector/SIMD register
- memory에 대한 읽기/쓰기 결과

CPU는 다음에 실행할 instruction 위치를 추적하고, instruction을 가져와 decode하고, 필요한 연산을 수행하고, 다음 상태로 진행한다. 현대 CPU 내부는 pipeline, out-of-order execution, branch prediction, cache hierarchy 등으로 훨씬 복잡하지만 programmer-visible 의미는 ISA가 제공하는 규칙을 만족하도록 유지된다.

### program counter가 순서 실행과 branch를 연결한다

아주 단순화한 pseudo instruction을 보자.

```text
100: LOAD  R1, [price]
104: LOAD  R2, [quantity]
108: MUL   R1, R2
112: STORE [total], R1
116: ...
```

program counter가 100을 가리키고 첫 명령이 끝나면 다음 명령 주소로 진행한다. branch나 function call 같은 명령은 다음에 실행할 위치를 바꾼다.

조건문이 machine level에서 마법처럼 `if`를 이해하는 것이 아니다. 비교 결과를 만들고 조건에 따라 control flow가 다른 instruction address로 이동하도록 구현할 수 있다.

### source 한 줄이 instruction 하나라는 보장은 없다

```python
result = price * quantity
```

같은 source line은 언어/runtime에 따라 수많은 내부 동작을 포함할 수 있다. 반대로 compiler optimization은 여러 source operation을 결합하거나 제거할 수 있다.

따라서 performance를 source line 수로만 판단하지 않는다.

```text
코드가 한 줄이다
≠
CPU instruction 하나다
≠
항상 빠르다
```

특히 데이터베이스 query, network call, filesystem I/O처럼 외부 시스템 경계를 넘는 한 줄은 CPU arithmetic 수백만 번보다 오래 걸릴 수 있다.

### register는 아주 빠른 작은 저장 위치지만 '변수 하나당 하나'가 아니다

compiler는 register allocation을 통해 실행 중 값을 register에 배치하려 한다. register 수는 제한되어 있고 모든 source variable이 고정된 한 register를 영구히 가지는 것은 아니다.

필요한 값이 많으면 stack memory에 spill할 수 있고, optimization 과정에서 source variable 자체가 사라지거나 여러 값이 같은 register를 서로 다른 시점에 사용할 수 있다.

이 모델을 알면 debugger에서 register와 source variable의 관계가 왜 단순하지 않은지 이해할 수 있다.

---

## CHAPTER 07 · memory access는 CPU와 RAM 사이 한 번의 왕복이 아니다

### cache hierarchy가 필요한 이유는 속도 차이다

CPU core의 연산 속도와 main memory 접근 지연에는 큰 차이가 있다. 현대 시스템은 여러 단계의 cache를 두어 최근 또는 가까운 데이터를 더 빠른 저장 계층에서 재사용한다.

일반적인 그림은 다음처럼 생각할 수 있다.

```text
register
↓
L1 cache
↓
L2 cache
↓
L3 / last-level cache
↓
main memory
↓
storage
```

각 계층의 정확한 크기와 구조는 CPU마다 다르다. 중요한 원리는 **빠른 계층은 작고 비싸며, 큰 계층은 상대적으로 느리다**는 trade-off다.

### locality는 데이터 구조 성능과 직접 연결된다

배열의 연속된 원소를 순서대로 읽는 코드는 memory locality가 좋을 수 있다. 반면 멀리 흩어진 node를 pointer로 따라가는 구조는 cache miss가 더 많이 발생할 수 있다.

그래서 알고리즘의 Big-O가 같더라도 실제 성능이 다를 수 있다.

```text
O(n) 순회 A
O(n) 순회 B
```

둘 다 n에 비례하지만 memory access pattern, branch behavior, object allocation 등 실제 machine cost가 다르다.

이것이 뒤의 자료구조 TRACK에서 `LinkedList가 중간 삽입 O(1)이니까 항상 빠르다` 같은 단순 결론을 경계해야 하는 이유다.

### cache가 있다고 memory consistency 문제가 사라지는 것은 아니다

여러 core와 thread가 shared memory를 읽고 쓸 때는 각 core의 cache, compiler reordering, CPU memory model, synchronization primitive를 함께 고려해야 한다.

이 PART에서 모든 memory model을 배우지는 않지만 다음 원칙은 남긴다.

```text
한 thread에서 순서대로 썼다
→ 다른 thread가 같은 순서와 시점으로 자동 관찰한다
```

를 아무 synchronization 없이 가정하면 안 된다.

`volatile`, atomic, lock, mutex 같은 도구는 단순히 `동시에 못 들어오게 한다` 이상의 memory-order 의미를 가질 수 있다. 정확한 규칙은 언어와 플랫폼 memory model을 확인해야 한다.

---

## CHAPTER 08 · 사용자 프로그램은 운영체제에 일을 요청한다

### user mode와 kernel mode를 나누는 이유

일반 애플리케이션이 CPU의 모든 권한을 직접 가진다면 한 프로그램의 버그가 임의 메모리를 읽거나 장치 상태를 망가뜨리고 다른 프로그램까지 파괴할 수 있다.

운영체제는 CPU protection mechanism을 이용해 일반 프로그램이 제한된 권한에서 실행되게 하고, privileged operation은 kernel이 통제한다.

그래서 파일 열기, network socket, process 생성처럼 시스템 자원이 필요한 작업은 운영체제에 요청하는 경계를 지난다.

이 경계를 system call, syscall이라고 부른다.

### library function과 syscall은 같은 단어가 아니다

C에서 `printf()`를 호출했다고 해서 CPU가 `printf syscall`을 실행하는 것은 아니다. `printf`는 C library function이고 formatting과 buffering을 수행한 뒤 필요할 때 더 낮은 I/O 함수를 통해 kernel에 write 요청을 할 수 있다.

개념적인 흐름을 단순화하면:

```text
printf("hello")
↓
C library formatting/buffering
↓
write 계열 system call
↓
kernel
↓
file/terminal/device 처리
```

따라서 profiler나 tracer에서 library call과 syscall을 구분해야 한다.

### syscall은 단순 함수 호출보다 경계 비용이 있다

user mode에서 kernel mode로 들어가면 권한 수준과 실행 컨텍스트를 전환해야 하고 kernel이 argument와 resource 상태를 검증해야 한다. 그래서 작은 I/O를 지나치게 많이 syscall로 보내는 것보다 buffering이 도움이 되는 경우가 있다.

하지만 `syscall은 무조건 느리니 줄이면 된다`도 지나친 일반화다. 병목이 실제 어디인지 measurement로 확인해야 한다.

### blocking I/O는 CPU가 멈춘다는 뜻이 아니다

한 thread가 disk/network I/O 완료를 기다리며 block될 수 있다. 그렇다고 CPU 전체가 멈추는 것은 아니다. scheduler는 실행 가능한 다른 thread/process를 core에 배치할 수 있다.

```text
thread A: read 요청 → wait
CPU core: thread B 실행 가능
I/O 완료
thread A: runnable로 복귀
```

여기서 concurrency와 scheduling 개념이 시작된다.

`async`를 배우기 전에 blocking 상태와 runnable 상태를 이해해야 하는 이유가 있다. 비동기 API는 기다림 자체를 없애는 마법이 아니라, **기다리는 동안 thread/runtime가 다른 일을 할 수 있도록 control flow와 notification 방식을 바꾸는 설계**다.

---

## CHAPTER 09 · 파일 I/O를 한 번 추적하면 시스템 경계가 보인다

### open은 '파일 내용을 읽는다'가 아니다

POSIX 계열 모델을 단순화하면 `open()`은 path를 해석하고 접근 권한을 확인한 뒤, 프로세스가 이후 I/O에 사용할 file descriptor를 돌려준다.

```c
int fd = open("data.txt", O_RDONLY);
```

`fd`는 파일 내용 그 자체가 아니다. 프로세스의 descriptor table에서 열린 파일 상태를 참조하는 작은 정수 handle이다.

그 다음 `read(fd, buffer, size)`가 데이터를 요청한다.

```text
path string
↓ open
file descriptor
↓ read
bytes copied/read into process buffer
```

이 두 단계를 구분하면 오류도 다르게 볼 수 있다.

- path가 없거나 권한이 없으면 open이 실패할 수 있다.
- open은 성공했지만 read 중 I/O error가 날 수 있다.
- read는 요청한 size보다 적은 byte를 반환할 수 있다.
- end-of-file을 별도 결과로 확인해야 한다.

### read가 한 번에 전부 준다고 가정하지 않는다

stream/socket/file API는 요청한 길이보다 적은 byte를 반환할 수 있다. 특히 network code에서 `read 한 번 = message 하나`라고 가정하면 packet/stream 경계를 혼동한다.

안전한 코드는 API 계약을 읽고 partial read/write 가능성을 처리한다.

```text
원하는 총 길이
- 지금까지 처리한 길이
= 남은 길이
```

를 추적하며 반복해야 할 수 있다.

### close는 단순 정리 문법이 아니라 resource lifetime의 끝이다

open file descriptor는 프로세스가 가진 제한된 resource다. 반복해서 열고 닫지 않으면 descriptor exhaustion이 발생할 수 있다.

managed language도 마찬가지다. 객체가 garbage-collected 된다고 OS handle이 원하는 시점에 즉시 닫힌다는 보장은 없다. 그래서 `with`, `try-with-resources`, `use` 같은 구조로 resource lifetime을 명시적으로 관리한다.

Kotlin에서는 예를 들어:

```kotlin
context.assets.open("config.json").bufferedReader().use { reader ->
    val text = reader.readText()
}
```

`use` block을 벗어날 때 close되는 구조를 만든다.

### write 성공과 durable persistence는 같은 말이 아니다

프로그램이 `write()`를 호출해 성공했다고 해서 저장장치의 비휘발성 매체에 모든 데이터가 이미 안전하게 기록되었다고 단정할 수 없다. OS page cache와 storage controller cache 같은 계층이 존재할 수 있다.

데이터베이스가 WAL, fsync, checkpoint 같은 개념을 중요하게 다루는 이유가 여기에 연결된다.

```text
application buffer
→ kernel/page cache
→ storage device/cache
→ durable media
```

각 단계가 언제 완료되는지, 어떤 API가 어떤 durability guarantee를 주는지 확인해야 한다.

`저장 버튼 눌렀으니 절대 안 날아간다`는 제품 요구사항은 실제로 매우 강한 시스템 계약이다.

---

## CHAPTER 10 · process와 thread는 실행 단위를 다르게 나눈다

### process isolation과 thread sharing을 구분한다

같은 프로세스의 여러 thread는 일반적으로 같은 virtual address space와 process resource를 공유하면서 각자의 register 상태와 stack을 가진다.

개념적으로:

```text
PROCESS
- virtual address space
- open files
- code/data/heap mappings

THREAD A
- registers
- stack

THREAD B
- registers
- stack
```

공유 메모리 때문에 thread끼리 데이터를 전달하기 쉽지만, 동시에 같은 mutable state를 건드릴 수 있으므로 race condition이 생길 수 있다.

프로세스는 기본적으로 주소 공간이 분리되어 있어 격리가 강하지만 통신에는 IPC 같은 명시적인 경계가 필요하다.

### race condition은 '두 thread가 동시에 실행'보다 정확한 개념이다

다음 의사코드를 두 thread가 동시에 실행한다고 하자.

```text
counter = counter + 1
```

source에서는 한 줄이지만 machine-level로는 다음과 같은 read-modify-write sequence가 될 수 있다.

```text
값 읽기
+1 계산
값 쓰기
```

초기 counter가 0일 때:

```text
A: 0 읽음
B: 0 읽음
A: 1 계산
B: 1 계산
A: 1 씀
B: 1 씀
```

두 번 증가했지만 결과는 1이다.

문제는 thread 개수 그 자체가 아니라 **결과가 timing/interleaving에 의존하게 되었는데 필요한 synchronization이 없다는 것**이다.

### lock은 코드를 느리게 만드는 장치가 아니라 invariant를 보호하는 경계다

mutex/lock을 `한 번에 한 thread만 들어오게 한다`고 배울 수 있다. 더 깊게는 **여러 연산을 하나의 논리적 critical section으로 묶어 shared invariant가 깨지는 중간 상태를 다른 thread가 관찰하지 못하게 한다**고 생각한다.

예를 들어 은행 transfer가:

```text
A 계좌 차감
B 계좌 증가
```

두 단계라면 다른 thread가 중간 상태를 보지 않아야 할 수 있다.

어떤 synchronization이 맞는지는 shared state의 범위, contention, ordering, failure semantics에 달려 있다.

---

## CHAPTER 11 · Android 앱도 결국 Linux 프로세스와 thread 위에서 실행된다

### APK는 설치 가능한 패키지이고 실행 중인 앱 그 자체가 아니다

Android 앱을 build하면 APK 또는 App Bundle을 통해 배포된다. 설치된 package의 코드와 resource가 있다고 해서 process가 항상 살아 있는 것은 아니다.

사용자가 component를 실행하거나 시스템이 필요로 할 때 Android가 app process를 만들 수 있고, memory pressure와 component 상태에 따라 더 이상 필요하지 않은 process를 종료할 수 있다.

따라서 모바일 앱은 `사용자가 화면을 보고 있으니 process가 영원히 유지된다`는 전제로 상태를 보관하면 안 된다.

### Android는 앱마다 Linux UID 기반 sandbox를 사용한다

Android platform은 일반적으로 각 앱에 고유한 Linux UID를 부여하고 process isolation과 file permission을 이용해 앱 데이터 경계를 만든다. 앱이 다른 앱의 private file을 마음대로 읽지 못하는 기본 sandbox가 여기서 나온다.

이것은 `Android가 Java니까 안전하다`와 다른 층의 이야기다. runtime language safety와 OS-level process/UID isolation은 별개의 방어 계층이다.

### main thread는 UI와 component callback을 처리하는 중요한 경계다

Android 앱 process가 시작되면 기본적으로 main thread가 만들어지고 UI 작업과 여러 component callback이 이 thread에서 실행된다.

main thread에서 긴 disk/network/computation을 blocking으로 수행하면 화면 event를 처리하지 못해 UI가 멈춘 것처럼 보이고 ANR 위험이 생긴다.

```text
터치 event 대기
↓
main thread가 8초짜리 작업에 막힘
↓
새 UI event 처리 못함
↓
사용자는 앱이 멈췄다고 느낌
```

그래서 background thread/coroutine을 쓰는 목적은 `코드를 멋있게 비동기로 보이게` 만드는 것이 아니라 **main thread의 응답성이라는 시스템 invariant를 지키는 것**이다.

### process death를 고려하면 저장 위치의 의미가 달라진다

변수를 memory에만 저장하면 process가 죽을 때 사라진다. 화면 회전 같은 configuration change, activity 재생성, process death 등 Android lifecycle 이벤트를 고려하면 상태를 어디에 보관할지 의식해야 한다.

```text
일시 UI state
process lifetime state
persistent user data
remote source-of-truth
```

를 구분해야 한다.

모든 것을 DB에 넣을 필요도 없고, 모든 것을 `remember`에 넣어도 안 된다. **필요한 수명과 복구 요구사항에 맞춰 저장 계층을 선택**해야 한다.

---

## CHAPTER 12 · 한 번의 실행을 계층별 증거로 추적한다

다음 프로그램이 파일에서 가격을 읽어 총액을 계산하고 화면에 표시한다고 하자.

```text
앱 시작
→ 설정 파일 open/read
→ 문자열 decode
→ 숫자 parse
→ quantity와 곱셈
→ UI state 갱신
→ 화면 render
```

문제가 `가격이 0으로 나온다`면 무작정 계산 코드부터 바꾸지 않는다.

### 계층 1 · 파일 존재와 접근

```text
path가 맞는가?
open은 성공했는가?
권한은 있는가?
실제 읽은 byte 수는 얼마인가?
```

### 계층 2 · 표현

```text
raw bytes는 무엇인가?
encoding은 무엇인가?
decode 결과 문자열은 무엇인가?
newline/BOM/공백이 있는가?
```

### 계층 3 · parsing과 domain validation

```text
문자열을 숫자로 parse했는가?
parse 실패를 0으로 바꾸고 있지는 않은가?
허용 범위 안인가?
```

### 계층 4 · 실행 상태

```text
price register/variable 값은?
quantity 값은?
분기 조건이 다른 path로 갔는가?
overflow 가능성은?
```

### 계층 5 · UI 전달

```text
계산된 total은 state source-of-truth에 반영됐는가?
다른 thread가 옛 값을 덮었는가?
render는 최신 state를 읽는가?
```

이렇게 조사하면 `CPU 문제`, `RAM 문제`, `앱 문제` 같은 넓은 말 대신 실패 계층을 좁힐 수 있다.

---

## CHAPTER 13 · 성능 문제도 상태 변화와 기다림을 분리해서 본다

### CPU-bound와 waiting-bound를 구분한다

프로그램이 느리다고 해서 CPU가 느린 것은 아니다.

```text
CPU-bound
→ 실제 연산을 계속 수행하느라 core 사용률이 높음

I/O-bound / wait-heavy
→ disk/network/lock/timer 등 외부 사건을 기다리는 시간이 큼
```

둘의 해결 방법은 다르다.

CPU-bound라면 algorithm, data locality, vectorization, parallelism, compiler optimization을 조사할 수 있다. I/O wait가 크다면 request 횟수, batching, caching, concurrency, network latency를 봐야 한다.

### 평균만 보면 tail latency가 숨는다

100번 요청 중 99번이 20ms이고 1번이 5초라면 평균은 사용자 체감 문제를 충분히 표현하지 못할 수 있다. percentile과 latency distribution을 보는 이유다.

성능 문제를 이해하려면 `빠르다/느리다` 대신 다음 증거를 모은다.

```text
CPU utilization
run queue
allocation/GC
I/O wait
disk/network latency
lock contention
request p50/p95/p99
```

어느 계층이 시간을 소비하는지 확인한 뒤 최적화한다.

### cache miss와 page fault는 같은 cache라는 말로 뭉개지 않는다

CPU cache miss는 hardware cache hierarchy에서 원하는 cache line을 찾지 못한 상황이다. page fault는 virtual memory translation/permission/backing과 관련해 OS가 처리해야 하는 사건이다. 둘 다 느려질 수 있지만 전혀 다른 계층이다.

정확한 이름을 붙이는 이유는 멋있게 말하기 위해서가 아니다. **원인 계층에 맞는 관측 도구와 해결책을 선택하기 위해서**다.

---

## CHAPTER 14 · 이 PART에서 반드시 연결되어야 하는 것

소스 코드와 실행을 다음 한 줄로 압축해서 기억하지 않는다.

```text
코드 → CPU
```

대신 실제 조사의 기준이 되는 연결을 남긴다.

```text
source bytes
→ text decoding
→ lexical/syntax structure
→ compiler/interpreter/runtime transformation
→ executable/bytecode/intermediate form
→ loader/runtime startup
→ process + virtual address space
→ thread execution state
→ machine instructions / runtime operations
→ syscall when OS resource is needed
→ device/file/network and back
```

모든 언어가 정확히 같은 단계를 같은 이름으로 거치는 것은 아니다. 이 그림의 목적은 **언어 구현이 달라도 조사할 경계를 찾는 것**이다.

오류가 나면 다음을 묻는다.

1. source를 읽고 parse하는 단계인가?
2. compile/link/build 단계인가?
3. loader/runtime startup 단계인가?
4. 실행 중 값과 control flow 문제인가?
5. memory/resource lifetime 문제인가?
6. OS syscall/I/O 경계 문제인가?
7. thread/concurrency 문제인가?
8. Android lifecycle/main-thread/process 문제인가?

이 질문만 제대로 해도 `에러가 났으니 코드를 전부 다시 써 달라`는 방식에서 벗어나 실제 개발자가 하는 원인 격리로 들어간다.

다음 PART에서는 CPU·메모리·저장장치를 각각 따로 설명하는 대신 **cache hierarchy, virtual memory, allocation, page fault, file cache, durability가 하나의 실행에서 어떻게 연결되는지** 더 깊게 파고든다.
