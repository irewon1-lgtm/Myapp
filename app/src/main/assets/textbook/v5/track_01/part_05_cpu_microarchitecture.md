# PART 05 · CPU는 명령을 한 줄씩 천천히 읽는 상자가 아니다

소스 코드를 machine instruction으로 바꿨다고 해서 CPU 내부가 단순해지는 것은 아니다. 현대 processor는 instruction을 가져오고 해석하고 실행하는 여러 단계를 겹치고, branch의 미래를 예측하고, dependency가 없는 작업을 먼저 실행하며, 여러 core의 cache가 같은 memory를 볼 때 coherence를 유지한다.

이 PART는 특정 CPU 제품의 회로도를 외우려는 것이 아니다. **왜 똑같은 Big-O 코드가 실제 hardware에서 크게 다른 성능을 내는지, profiler의 cycle·stall·branch miss·cache miss가 어떤 실행 메커니즘과 연결되는지** 이해하는 것이 목표다.

---

## CHAPTER 01 · ISA와 microarchitecture를 구분한다

### ISA는 software와 processor 사이의 계약이다

Instruction Set Architecture, ISA는 software가 processor에 기대할 수 있는 명령 형식과 programmer-visible state의 계약이다.

예를 들면 다음과 같은 범주가 있다.

```text
registers
instruction encoding
load/store behavior
arithmetic/logic operations
control flow
privilege-related architectural state
exception model
```

같은 ISA를 구현하는 CPU라도 내부 구조는 크게 다를 수 있다.

### microarchitecture는 그 계약을 실제로 구현하는 방법이다

동일한 instruction sequence를 실행하면서도 CPU A와 B는 pipeline 깊이, cache 크기, execution unit 수, branch predictor, reorder buffer 등에서 다를 수 있다.

```text
same ISA program
   ↓
CPU A microarchitecture → 1.0 ms
CPU B microarchitecture → 0.6 ms
```

따라서 `Arm이면 속도가 이렇다`, `x86이면 반드시 저렇다` 같은 단순화는 위험하다.

ISA는 semantic contract이고 microarchitecture는 성능과 전력 trade-off를 담는 implementation이다.

---

## CHAPTER 02 · instruction cycle을 fetch-decode-execute 한 문장으로 끝내지 않는다

### 가장 단순한 모델

처음에는 다음 정도로 생각할 수 있다.

```text
fetch instruction
↓
decode
↓
read operands
↓
execute
↓
write result
```

하지만 현대 CPU는 한 instruction이 모든 단계를 끝낸 뒤 다음 instruction을 시작하는 방식만 사용하지 않는다.

### pipeline은 여러 instruction의 단계를 겹친다

세탁 공정을 생각하지 말고 실제 시간축으로 본다.

```text
cycle:    1   2   3   4   5   6
I1:       F   D   E   M   W
I2:           F   D   E   M   W
I3:               F   D   E   M   W
```

각 글자는 단순화한 pipeline stage다.

pipeline이 충분히 채워지면 하나의 instruction latency가 여러 cycle이어도 일정 조건에서는 매 cycle 새로운 instruction 완료를 목표로 할 수 있다.

이때 latency와 throughput을 구분해야 한다.

```text
latency
= 특정 instruction 하나가 시작부터 결과까지 걸리는 시간

throughput
= 단위 시간에 처리 가능한 instruction/operation 수
```

둘은 같은 숫자가 아니다.

---

## CHAPTER 03 · dependency가 pipeline을 멈출 수 있다

### data dependency

```text
x = a + b
y = x * c
```

두 번째 연산은 첫 번째 결과 `x`가 필요하다.

instruction-level로도 producer 결과가 준비되기 전 consumer가 필요한 값을 사용할 수 없다.

CPU는 forwarding/bypassing 같은 기법으로 일부 기다림을 줄일 수 있지만 dependency 자체가 사라지는 것은 아니다.

### dependency chain이 긴 code

```text
x = (((a * b) + c) * d + e) * f
```

각 단계가 이전 결과를 기다리면 많은 execution unit이 있어도 병렬 실행 여지가 작다.

반면 independent accumulator가 여러 개 있으면 hardware가 동시에 진행할 여지가 생길 수 있다.

```text
s0 += a[i]
s1 += a[i+1]
s2 += a[i+2]
s3 += a[i+3]
```

compiler가 이런 변환을 안전하게 할 수 있는지, floating-point 연산 순서가 결과에 영향을 주는지 등 추가 조건은 따로 확인해야 한다.

---

## CHAPTER 04 · branch는 미래 instruction stream을 불확실하게 만든다

### branch target을 늦게 알면 pipeline이 굶는다

```text
if condition:
    path A
else:
    path B
```

CPU가 condition 결과가 확정될 때까지 다음 instruction fetch를 전부 멈추면 pipeline 활용도가 크게 떨어질 수 있다.

그래서 branch predictor가 다음 path를 예측하고 speculative execution을 진행할 수 있다.

### prediction이 맞으면 시간을 숨긴다

```text
predict A
↓
A path fetch/decode/execute 준비
↓
actual = A
↓
계속 진행
```

### prediction이 틀리면 speculative work를 버린다

```text
predict A
↓
A path 진행
↓
actual = B
↓
잘못된 speculative state 폐기
↓
B path에서 다시 진행
```

이때 pipeline depth와 implementation에 따라 branch misprediction penalty가 커질 수 있다.

### branchless가 항상 더 빠른 것은 아니다

조건문을 arithmetic이나 mask로 없애면 branch miss는 줄 수 있지만 더 많은 instruction을 실행하거나 vectorization을 방해할 수 있다.

따라서:

```text
branch 있음 = 느림
branch 없음 = 빠름
```

은 규칙이 아니다.

실제 data distribution과 predictor behavior를 측정해야 한다.

---

## CHAPTER 05 · out-of-order execution은 source 순서를 마음대로 깨는 것이 아니다

### independent instruction은 먼저 실행될 수 있다

다음 instruction stream을 보자.

```text
1. load A   // cache miss로 오래 걸림
2. B = C+D  // A와 독립
3. E = A*2  // A 필요
```

CPU가 1번 완료를 기다리며 모든 것을 멈추는 대신 2번처럼 dependency가 없는 instruction을 먼저 실행해 latency를 숨길 수 있다.

### architectural result는 program order의 의미를 지켜야 한다

out-of-order execution은 program semantics를 무시하겠다는 뜻이 아니다.

CPU 내부에서 execution 시점은 재배치될 수 있어도 exception과 visible architectural state가 ISA가 요구하는 결과와 맞도록 retirement/commit mechanism을 둔다.

### reorder buffer를 직관적으로 본다

정확한 구조는 CPU마다 다르지만 개념적으로 speculative result를 임시로 추적하고 older instruction부터 architectural state에 확정하는 buffer가 있을 수 있다.

```text
issue out of order
↓
execute
↓
results wait
↓
retire in architectural order
```

이 모델은 `CPU가 source line 순서대로 한 줄씩 실행한다`는 생각을 깨 준다.

---

## CHAPTER 06 · superscalar는 한 cycle에 여러 operation을 처리하려 한다

### execution unit이 여러 종류일 수 있다

processor에는 integer ALU, floating-point/vector unit, load/store unit, branch unit 등 여러 execution resource가 있을 수 있다.

동시에 처리 가능한 instruction 수는 dependency뿐 아니라 필요한 execution port/unit 경쟁에도 영향을 받는다.

### instruction mix가 성능에 영향을 준다

두 code가 instruction count는 비슷해도 특정 resource 하나에 몰리면 throughput이 제한될 수 있다.

```text
workload A
integer + load + branch가 균형

workload B
특정 expensive unit만 연속 사용
```

이 차이는 source line 수로 알기 어렵다.

그래서 low-level profiling에서는 instruction count뿐 아니라 cycles, IPC, stalls 같은 지표를 같이 본다.

---

## CHAPTER 07 · register는 가장 가까운 working storage다

### 모든 변수마다 RAM read가 필요한 것은 아니다

compiler는 local value를 register에 유지할 수 있다.

```text
for (...) {
    sum += x;
}
```

`sum`이 매 iteration마다 memory에 저장됐다 다시 읽히는지, register에 살아 있는지는 optimization과 aliasing 등 조건에 따라 달라진다.

### register pressure

동시에 살아 있어야 하는 값이 available register보다 많으면 일부 값을 stack memory로 spill해야 할 수 있다.

```text
many live values
↓
register shortage
↓
spill store/load
↓
extra memory traffic
```

그래서 거대한 함수와 복잡한 expression이 무조건 빠른 code가 되는 것은 아니다.

compiler가 최적화하기 좋은 lifetime과 data flow를 만드는 것도 중요하다.

---

## CHAPTER 08 · function call에는 호출 규약이 있다

### caller와 callee가 register와 stack을 어떻게 나눌지 약속한다

machine-level function call에서는 argument 전달, return value, preserved register, stack alignment 등에 ABI 규약이 있다.

일반적인 개념:

```text
caller prepares arguments
↓
control transfers to callee
↓
callee saves required state
↓
body executes
↓
return value prepared
↓
state restored
↓
control returns
```

정확한 register 이름과 규칙은 ABI/architecture 문서를 확인한다.

### stack frame은 source block과 같은 것이 아니다

함수 호출마다 local state, saved return address, spilled register 등이 stack에 배치될 수 있다.

compiler optimization으로 frame이 줄거나 사라질 수도 있다.

따라서 debugger stack trace의 frame과 source code 중괄호 block을 동일시하지 않는다.

---

## CHAPTER 09 · recursion은 call stack이라는 실제 resource를 사용한다

### recursive depth가 무한할 수 없는 이유

```text
f()
 -> f()
   -> f()
     -> ...
```

각 active call이 stack state를 요구하면 depth가 커질수록 stack memory를 소비한다.

언어/runtime에 따라 recursion limit을 별도로 두거나 stack overflow가 발생할 수 있다.

### tail call optimization을 보편 규칙으로 가정하지 않는다

어떤 language/compiler는 특정 tail call을 frame reuse로 최적화할 수 있지만 모든 환경이 이를 보장하지 않는다.

그래서 `꼬리 재귀니까 stack을 안 쓴다`고 플랫폼 계약 없이 단정하면 안 된다.

---

## CHAPTER 10 · SIMD는 같은 종류의 연산을 여러 lane에 적용한다

### scalar와 vector

scalar:

```text
1 + 2
```

vector/SIMD:

```text
[a0 a1 a2 a3]
+
[b0 b1 b2 b3]
=
[c0 c1 c2 c3]
```

image pixel, audio sample, numeric array처럼 같은 연산을 반복하는 workload에서 SIMD가 큰 throughput 향상을 줄 수 있다.

### compiler auto-vectorization

개발자가 vector intrinsic을 직접 쓰지 않아도 compiler가 loop dependency를 분석해 vector instruction으로 변환할 수 있다.

하지만 aliasing, branch, dependency, alignment, loop trip count 등이 vectorization을 방해할 수 있다.

### SIMD가 algorithm을 대신하지 않는다

O(n²) algorithm을 SIMD로 최적화해도 더 나은 O(n log n) algorithm이 훨씬 빠를 수 있다.

순서는 보통:

```text
correct algorithm/data structure
↓
measure bottleneck
↓
cache/access pattern
↓
vectorization/micro-optimization
```

이다.

---

## CHAPTER 11 · cache coherence는 여러 core가 공유 memory를 다루는 규칙이다

### core마다 private cache가 있을 수 있다

```text
Core 0 cache ─┐
              ├─ shared memory view
Core 1 cache ─┘
```

두 core가 같은 address를 읽고 쓰면 각 cache copy가 무작정 독립적으로 남아서는 안 된다.

cache coherence protocol은 같은 cache line에 대한 여러 copy의 상태를 조정해 write가 적절히 전파되도록 한다.

### coherence와 consistency/memory ordering은 구분한다

coherence는 같은 memory location의 cache copies가 어떤 순서로 일관되게 보이는가와 관련된다.

memory consistency model은 서로 다른 location의 load/store ordering까지 더 넓은 규칙을 다룬다.

둘을 `캐시 동기화` 하나로 묶으면 concurrent algorithm을 이해하기 어렵다.

---

## CHAPTER 12 · false sharing은 서로 다른 변수인데 같은 cache line 때문에 싸우는 문제다

### 논리적으로 독립이어도 물리적으로 가까울 수 있다

```text
cache line:
[counterA][counterB][other data]

Core 0 repeatedly writes counterA
Core 1 repeatedly writes counterB
```

A와 B는 서로 다른 변수라 lock이 필요 없어 보인다.

하지만 같은 cache line에 있다면 한 core의 write가 다른 core cache의 line state를 계속 바꾸게 해 coherence traffic이 증가할 수 있다.

### padding을 무조건 넣는 것도 답이 아니다

false sharing을 피하려고 모든 variable을 cache line 크기로 띄우면 memory footprint가 커지고 cache locality가 나빠질 수 있다.

profiler와 hardware counter로 실제 contention을 확인한 뒤 data layout을 조정한다.

---

## CHAPTER 13 · memory ordering은 multi-core에서 실제 bug가 된다

### 두 variable의 write/read 순서를 source만 보고 가정하지 않는다

Thread A:

```text
data = 42
ready = true
```

Thread B:

```text
if ready:
    print(data)
```

synchronization이 없다면 B가 `ready=true`를 관찰했을 때 반드시 최신 `data=42`도 보인다는 보장을 language/platform memory model 없이 만들 수 없다.

### release/acquire 같은 ordering primitive

atomic operation과 memory barrier는 특정 operation 전후 memory visibility 순서를 만드는 데 사용된다.

정확한 이름과 semantics는 C++, Java, Kotlin/JVM, Rust, CPU ISA마다 다르다.

따라서 low-level lock-free code를 작성할 때 다른 언어의 memory model을 복사해 오면 안 된다.

### lock이 쉬운 이유

잘 구현된 mutex는 mutual exclusion뿐 아니라 필요한 memory visibility/order 관계를 함께 제공한다.

그래서 lock-free가 항상 `고급이고 빠른 해답`이 아니다.

correctness proof와 maintenance cost까지 포함해야 한다.

---

## CHAPTER 14 · CPU cache와 compiler optimization 사이에 aliasing 문제가 있다

### 두 pointer가 같은 memory를 가리킬 수 있는가

compiler가 다음 두 pointer가 절대 겹치지 않는다고 알면 load/store를 더 공격적으로 재배치하거나 vectorize할 수 있다.

반대로 alias 가능성이 있으면 `앞의 write가 뒤 read에 영향을 줄 수 있다`고 보수적으로 가정해야 한다.

```text
*p = ...
x = *q
```

`p`와 `q`가 같은 address일 수 있는지에 따라 optimization 자유도가 달라진다.

### high-level language도 alias 문제가 사라지지 않는다

mutable object reference 여러 개가 같은 object를 가리키면 logical alias가 된다.

```text
val a = user
val b = user
b.name = "new"
// a.name도 바뀐 object를 본다
```

PART 01의 `bit interpretation`처럼 여기서는 `reference identity`를 정확히 이해해야 한다.

---

## CHAPTER 15 · exception과 interrupt는 normal instruction flow를 바꾼다

### synchronous exception

현재 instruction 때문에 발생하는 사건을 생각할 수 있다.

```text
invalid instruction
page fault
divide error
system call trap
```

CPU는 정해진 handler 경로로 control을 넘기고 privileged software가 원인을 처리한다.

### asynchronous interrupt

device completion이나 timer처럼 현재 instruction의 논리 결과와 직접 연결되지 않은 외부 사건이 interrupt를 일으킬 수 있다.

```text
CPU running user code
↓
device interrupt
↓
kernel handler
↓
return/reschedule
```

이 구조 덕분에 CPU가 device가 끝났는지 매 instruction마다 직접 polling하지 않고도 사건을 받을 수 있다.

### interrupt가 있다고 context switch가 반드시 일어나는 것은 아니다

interrupt 처리 뒤 같은 thread로 돌아갈 수도 있고 scheduler가 다른 runnable task를 선택할 수도 있다.

`interrupt = process switch`로 외우지 않는다.

---

## CHAPTER 16 · privilege level은 하드웨어 보호의 일부다

### user mode에서 아무 instruction이나 실행하지 못한다

운영체제는 application이 page table, device control, interrupt 설정 같은 critical resource를 직접 바꾸지 못하도록 CPU privilege mechanism을 사용한다.

user code가 privileged operation이 필요하면 system call boundary를 통해 kernel에 요청한다.

```text
user mode
↓ syscall/trap
kernel mode
↓ validation + privileged operation
return
↓
user mode
```

### privilege는 보안 boundary다

application bug 하나가 전체 system memory를 마음대로 덮어쓰지 못하게 하는 데 process isolation과 privilege mode가 함께 작동한다.

하지만 kernel bug나 driver bug는 더 높은 privilege 때문에 영향 범위가 커질 수 있다.

---

## CHAPTER 17 · performance counter는 CPU 내부 추측을 evidence로 바꾼다

### wall-clock time 하나만으로 원인을 모른다

프로그램 A가 2초 걸리고 B가 1초 걸린다는 사실은 결과다.

왜 다른지는 추가 evidence가 필요하다.

hardware performance counter로 가능한 경우 다음 지표를 볼 수 있다.

```text
cycles
instructions retired
branches
branch misses
cache references/misses
stalled cycles
```

정확한 counter 이름과 신뢰도는 CPU/OS/profiler에 따라 다르다.

### IPC/CPI를 맥락 없이 순위표로 쓰지 않는다

IPC는 instructions per cycle, CPI는 cycles per instruction 관점이다.

하지만 instruction complexity, vector instruction, compiler transformation, memory latency가 달라서 `IPC가 높은 프로그램이 무조건 더 좋다`는 결론은 틀리다.

같은 workload/결과를 비교하는 보조 지표로 사용한다.

---

## CHAPTER 18 · microbenchmark는 너무 쉽게 거짓말한다

### compiler가 계산 자체를 없앨 수 있다

결과가 사용되지 않는 code는 dead-code elimination 대상이 될 수 있다.

```text
start timer
compute value nobody uses
stop timer
```

release optimization에서 `compute`가 사라지면 엄청 빠른 benchmark가 나온다.

### warm-up과 JIT

runtime이 JIT를 사용하면 처음 실행과 충분히 warm-up된 steady state가 다를 수 있다.

### cache warm/cold 상태

같은 data를 두 번째 실행하면 cache hit로 빨라질 수 있다.

### frequency와 thermal

mobile CPU는 thermal/power condition에 따라 frequency가 바뀔 수 있다.

따라서 작은 성능 차이를 측정할 때 device 온도와 반복 순서를 통제해야 한다.

---

## CHAPTER 19 · Android 성능도 CPU 모델과 연결된다

### main thread frame deadline

UI rendering은 일정 frame cadence 안에 input, layout, draw 관련 작업을 끝내야 부드럽다.

main thread에서 CPU-heavy loop가 길어지면 network가 없어도 frame을 놓칠 수 있다.

### background thread가 많아도 CPU는 한정돼 있다

worker를 100개 만든다고 mobile SoC의 CPU core가 100개가 되는 것은 아니다.

많은 thread가 runnable이 되면 서로 CPU를 차지하고 foreground UI task와 경쟁할 수 있다.

### big/little heterogeneous core

mobile SoC에는 성능 특성과 전력 특성이 다른 core cluster가 있을 수 있다.

scheduler가 workload를 어떤 core에 배치하는지는 system policy의 영역이다.

앱은 `특정 thread는 항상 최고성능 core에서 돈다` 같은 가정을 피하고 실제 trace를 본다.

---

## CHAPTER 20 · 최적화는 층위를 지킨다

가장 위험한 순서는 다음이다.

```text
느리다
→ branch를 없앤다
→ inline한다
→ bit trick을 쓴다
→ readability가 무너진다
→ 실제 bottleneck은 DB/network였음
```

더 안전한 순서:

```text
1. 사용자에게 느린 구간을 정의한다.
2. end-to-end trace를 측정한다.
3. CPU/I/O/wait 중 어디가 큰지 나눈다.
4. algorithm/data structure를 확인한다.
5. memory/cache behavior를 확인한다.
6. 그 후 필요한 microarchitecture 최적화를 검토한다.
7. 같은 workload로 다시 측정한다.
```

### 계산 예제

API 응답이 900ms다.

```text
network RTT + remote service: 650ms
local DB: 120ms
JSON parsing: 40ms
UI transform: 30ms
other: 60ms
```

여기서 JSON parsing을 SIMD로 40ms → 20ms 줄여도 전체는 약 880ms다.

가장 큰 병목 650ms를 무시한 micro-optimization은 기술적으로 멋져도 사용자 효과가 작다.

---

## PART 05 종료 점검

1. ISA와 microarchitecture는 무엇이 다른가?
2. pipeline이 latency와 throughput을 어떻게 분리시키는가?
3. data dependency가 instruction-level parallelism을 왜 제한하는가?
4. branch prediction 실패 시 왜 비용이 생기는가?
5. out-of-order execution이 program semantics를 무시한다는 뜻이 아닌 이유는 무엇인가?
6. register pressure와 spill은 어떻게 연결되는가?
7. SIMD가 모든 algorithm을 빠르게 만들지 못하는 이유는 무엇인가?
8. cache coherence와 memory ordering은 무엇이 다른가?
9. false sharing은 서로 다른 variable 사이에서도 왜 생길 수 있는가?
10. user mode와 kernel mode가 protection boundary를 만드는 방식은 무엇인가?
11. performance counter가 wall-clock time보다 어떤 원인 증거를 추가하는가?
12. microbenchmark에서 compiler/JIT/cache/thermal을 왜 통제해야 하는가?

이 PART를 다 읽고도 `CPU는 명령을 처리한다`만 말할 수 있다면 아직 충분하지 않다. **pipeline, dependency, speculation, cache, coherence, ordering, privilege, measurement**를 실제 코드 성능과 연결할 수 있어야 한다.