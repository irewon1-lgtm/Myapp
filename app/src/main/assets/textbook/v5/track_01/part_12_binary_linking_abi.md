# PART 12 · 실행 파일은 코드 덩어리가 아니다 — object, ELF, relocation, linker, ABI

소스 코드를 compile했다고 바로 실행 가능한 process가 생기는 것은 아니다. 여러 translation unit에서 나온 machine code와 data를 object file에 담고, symbol reference를 해결하고, address를 배치하고, shared library와 runtime loader가 협력해 process address space를 구성해야 한다.

이 PART의 목표는 linker 옵션을 외우는 것이 아니라 **`undefined symbol`, `cannot locate shared library`, ABI mismatch, duplicate symbol, relocation failure` 같은 오류를 binary 구조로 해석하는 것**이다.

---

## CHAPTER 01 · translation unit과 object file을 구분한다

C/C++ 계열을 예로 들면 source file 하나가 preprocessing/compilation/assembly를 거쳐 relocatable object를 만들 수 있다.

```text
foo.c
↓ preprocess/compile/assemble
foo.o
```

object file에는 완성된 machine code만 있는 것이 아니다.

```text
machine instructions
data sections
symbol table
relocation records
debug metadata
section metadata
```

같은 정보가 담길 수 있다.

### 아직 주소가 결정되지 않은 reference

`foo.o`가 다른 object의 `bar()`를 호출할 수 있다.

compile 시점에는 `bar`의 최종 virtual address를 모를 수 있다.

그래서 symbol reference와 relocation 정보를 남겨 linker가 나중에 해결한다.

---

## CHAPTER 02 · symbol은 이름과 storage/code 위치를 연결하는 linker 단위다

예:

```c
int global_count;
void process(void) { ... }
```

compiler/linker 관점에서는 `global_count`, `process`가 symbol로 나타날 수 있다.

### defined와 undefined

object A:

```text
DEFINED: process
UNDEFINED: log_error
```

object B가 `log_error`를 정의하면 linker가 연결할 수 있다.

정의가 없으면:

```text
undefined reference / unresolved symbol
```

류의 link error가 난다.

### compile success와 link success가 다른 이유

source 문법과 type check가 맞아도 다른 module의 실제 symbol definition이 없으면 compile은 성공하고 link가 실패할 수 있다.

build pipeline 단계를 분리해서 읽어야 한다.

---

## CHAPTER 03 · strong/weak symbol과 duplicate definition

linker는 같은 이름의 여러 symbol definition이 나타났을 때 binding 규칙을 적용한다.

일부 object format/toolchain에서는 strong/weak binding을 지원한다.

잘못된 상황:

```text
A.o: strong foo
B.o: strong foo
```

→ multiple definition error가 날 수 있다.

weak symbol은 default implementation/override pattern 등에 쓰일 수 있지만 toolchain 규칙을 정확히 알아야 한다.

`같은 함수 이름이 두 파일에 있어도 namespace가 알아서 구분하겠지`라는 가정은 언어 linkage 규칙에 따라 틀릴 수 있다.

---

## CHAPTER 04 · section과 segment는 같은 단어가 아니다

ELF 계열 object/executable은 **section**과 **program segment**라는 서로 다른 관점을 가진다.

### section

link/edit/debug 관점의 논리 구획:

```text
.text
.rodata
.data
.bss
.symtab
.rela.*
```

### segment

loader가 process memory mapping을 만들 때 사용하는 실행 관점의 범위다.

여러 section이 하나의 loadable segment에 들어갈 수 있다.

```text
ELF sections
↓ linker arrangement
program headers
↓ loader
memory segments/pages
```

`ELF section = process memory page`로 1:1 대응시키면 안 된다.

---

## CHAPTER 05 · .text, .rodata, .data, .bss의 역할

전형적인 개념:

```text
.text    executable instructions
.rodata  read-only constants
.data    initialized writable globals/statics
.bss     zero-initialized/uninitialized writable data
```

실제 compiler/linker는 더 많은 section을 만든다.

### BSS가 file에서 크게 공간을 차지할 필요가 없는 이유

```c
static char buffer[100 * 1024 * 1024];
```

zero-initialized data라면 executable file에 100MB의 zero byte를 그대로 저장하는 대신 `memory size는 100MB지만 file-backed data는 없음`으로 기술할 수 있다.

loader/kernel은 mapping 시 zero-filled memory를 제공한다.

그래서:

```text
binary file size
≠ process virtual memory size
```

이다.

---

## CHAPTER 06 · relocation은 `이 reference가 실제로 어디를 가리켜야 하는가`를 고친다

object code 안에 다른 symbol address가 필요한 instruction/data가 있으면 relocation record가 위치와 종류를 설명한다.

linker는 final layout을 정한 뒤 값을 패치한다.

```text
call ???
relocation: offset X refers symbol foo
↓ linker decides foo address
call resolved-target
```

### relocation type

absolute address, PC-relative offset 등 architecture마다 여러 relocation type이 있다.

잘못된 relocation type/범위를 만나면 `relocation truncated`, unsupported relocation 같은 error가 날 수 있다.

---

## CHAPTER 07 · position-independent code는 load address에 덜 의존한다

shared library와 ASLR에서는 code가 매번 같은 virtual address에 load된다고 가정하기 어렵다.

Position Independent Code(PIC)는 instruction/data reference를 상대 addressing이나 indirection으로 구성해 arbitrary base address에서 동작하기 쉽게 한다.

### 왜 모든 absolute address를 runtime에 다시 고치지 않는가

text page를 process마다 수정하면 shared read-only code page를 공유하기 어렵고 startup relocation 비용도 커질 수 있다.

PIC와 GOT/PLT 같은 mechanism은 code sharing과 dynamic linking을 지원한다.

---

## CHAPTER 08 · static linking과 dynamic linking

### static

필요한 library code 일부/전체를 link time에 executable 안에 포함할 수 있다.

장점 후보:

```text
runtime library dependency 감소
배포 단순화 가능
```

대가:

```text
binary size 증가
shared library security fix가 자동 반영되지 않을 수 있음
```

### dynamic

shared object를 runtime loader가 process에 mapping하고 symbol을 연결한다.

```text
app executable
+
libA.so
+
libB.so
↓ dynamic loader
process image
```

배포/업데이트/ABI compatibility 문제가 추가된다.

---

## CHAPTER 09 · dynamic loader가 process 시작에서 하는 일

Linux/ELF 환경을 단순화하면 kernel이 executable header를 보고 필요한 interpreter/dynamic loader를 사용하게 구성할 수 있다.

loader는:

```text
needed shared objects 찾기
mapping
relocation
symbol resolution
initialization routines
control transfer
```

등을 수행한다.

### main() 이전에도 code가 실행될 수 있다

runtime startup code, dynamic initializer, constructor, language runtime initialization이 `main` 전에 실행될 수 있다.

`main 첫 줄 이전 crash`가 가능한 이유다.

---

## CHAPTER 10 · shared library search path는 security와 correctness 문제다

loader가 `libfoo.so`를 찾을 때 여러 search path/rpath/environment 설정이 관여할 수 있다.

의도하지 않은 directory가 앞에 오면 다른 version/library를 load할 수 있다.

### dependency confusion at runtime

library 이름만 맞으면 된다는 가정은 위험하다.

```text
expected libfoo v2
actual libfoo v1 loaded
```

이면 symbol/version mismatch나 subtle behavior 차이가 생긴다.

보안 환경에서는 untrusted path가 search에 들어가지 않도록 해야 한다.

---

## CHAPTER 11 · GOT와 PLT를 indirection으로 이해한다

정확한 ELF implementation 세부를 전부 외우지 않고 목적을 이해한다.

### GOT

Global Offset Table은 runtime address가 필요한 global/reference를 data table indirection으로 접근하게 할 수 있다.

### PLT

Procedure Linkage Table은 external function call을 dynamic symbol resolution path와 연결할 수 있다.

```text
caller
↓ PLT entry
↓ GOT/resolver
actual shared-library function
```

lazy binding configuration에서는 first call 시 resolution이 일어날 수 있다.

---

## CHAPTER 12 · lazy binding은 startup과 first-call latency를 교환한다

모든 function symbol을 startup에서 resolve하면 startup cost가 늘지만 이후 call은 바로 target을 알 수 있다.

lazy binding은 실제 처음 호출될 때 resolve해 startup work를 줄일 수 있다.

대가:

```text
first call latency
runtime resolver complexity
security hardening considerations
```

system/toolchain 설정에 따라 eager binding을 선택할 수 있다.

---

## CHAPTER 13 · symbol interposition은 예상한 함수가 아닌 함수를 호출하게 할 수 있다

ELF dynamic linking 환경에서는 symbol resolution order와 preload mechanism 등을 이용해 symbol을 가로챌 수 있다.

이는 debugging/instrumentation에 유용할 수 있지만 correctness/security complexity를 만든다.

```text
original malloc
↓ interposed wrapper
custom logging
↓ real malloc
```

### optimizer와 interposition

compiler가 symbol이 override될 수 있다고 가정하면 일부 optimization을 제한할 수 있다.

linkage semantics가 performance에도 영향을 준다.

---

## CHAPTER 14 · ABI는 binary끼리 대화하는 계약이다

API가 source-level function 이름/parameter contract라면 ABI는 binary-level convention까지 포함한다.

예:

```text
argument register/order
return value location
callee/caller-saved registers
stack alignment
object layout
name mangling
exception/unwind convention
binary format
```

### source compatible인데 binary incompatible할 수 있다

header는 같은데 struct layout이나 calling convention이 바뀌면 old binary와 new library가 깨질 수 있다.

`컴파일만 다시 하면 됨`과 `기존 binary도 그대로 실행됨`은 다른 compatibility 목표다.

---

## CHAPTER 15 · C++ name mangling과 symbol 이름

C++는 function overload, namespace, class method 정보를 linker symbol에 encode하는 name mangling을 사용한다.

source:

```cpp
void foo(int);
void foo(double);
```

binary symbol 이름은 서로 달라져야 한다.

compiler/ABI version이 달라 mangling이나 object ABI가 호환되지 않으면 library가 있어도 symbol을 못 찾을 수 있다.

`extern "C"`는 C linkage 이름 규칙을 요청할 때 사용한다.

---

## CHAPTER 16 · structure padding과 alignment도 ABI다

```c
struct Example {
    char a;
    int b;
};
```

memory layout이 단순히 1+4=5 byte라고 가정하면 안 된다.

alignment 요구 때문에 padding이 들어갈 수 있다.

```text
[a][padding...][b b b b]
```

### binary serialization에 struct memory를 그대로 쓰면 위험한 이유

compiler/architecture/endianness/padding이 다르면 file/network format이 달라진다.

portable serialization은 field encoding을 명시해야 한다.

PART 01 representation과 ABI가 연결된다.

---

## CHAPTER 17 · stack alignment가 깨지면 instruction 수준에서 실패할 수 있다

ABI는 function entry에서 stack pointer alignment를 요구할 수 있다.

hand-written assembly/JIT/FFI code가 이를 깨뜨리면 vector instruction이나 callee assumption에서 crash할 수 있다.

고수준 언어 compiler가 자동으로 지켜 주던 규칙을 low-level boundary에서는 직접 책임져야 한다.

---

## CHAPTER 18 · unwind metadata는 crash stack과 exception 처리에 중요하다

optimized native code에서 frame pointer가 항상 존재한다고 가정할 수 없다.

unwind table/debug metadata를 이용해 caller frame을 복원할 수 있다.

### symbol stripping

release binary에서 debug symbol을 분리/strip하면 package는 작아질 수 있지만 crash 주소를 source로 해석하려면 별도 symbol artifact를 보존해야 한다.

정확한 build symbol 보관이 observability pipeline의 일부다.

---

## CHAPTER 19 · ASLR은 load address 예측을 어렵게 한다

Address Space Layout Randomization은 executable/library/stack/heap 등의 base address를 실행마다 변화시켜 공격자가 address를 예측하기 어렵게 만든다.

### crash address 비교

ASLR 때문에 두 process crash address가 절대주소로 다를 수 있다.

module base와 offset, build ID를 사용해 symbolization해야 한다.

```text
PC absolute address
- module load base
= relative offset
```

---

## CHAPTER 20 · RELRO와 read-only relocation 영역

일부 dynamic relocation table은 startup에 loader가 수정해야 하지만 resolution 뒤 더 이상 writable일 필요가 없다.

RELRO 같은 hardening은 relocation-related data 영역을 read-only로 바꿔 공격자가 GOT 등을 덮어쓰기 어렵게 한다.

security feature는 performance/startup trade-off와 함께 toolchain에 구성된다.

---

## CHAPTER 21 · PIE는 executable에도 position independence를 확장한다

Position Independent Executable은 main executable을 임의 base에 배치하기 쉽게 해 ASLR 효과를 높인다.

shared library만 PIC이고 main executable이 fixed address라면 address randomization 범위가 줄 수 있다.

현대 mobile/server toolchain은 PIE를 기본 요구/사용하는 경우가 많다.

---

## CHAPTER 22 · dynamic library update는 ABI contract를 지켜야 한다

library v1:

```text
foo(int) returns int
```

v2가 ABI를 깨뜨리면 old application binary가 재compile 없이 동작하지 않을 수 있다.

versioned symbol, soname, compatibility policy를 사용해 breaking change를 관리한다.

### semver와 ABI는 자동 연결되지 않는다

version number를 올렸다고 loader가 binary compatibility를 자동 보장하는 것은 아니다.

실제 exported symbol/layout contract를 검증해야 한다.

---

## CHAPTER 23 · Android native library loading

APK/AAB에 ABI별 `.so`가 포함될 수 있다.

runtime linker가 app namespace와 permitted search path에서 library를 load한다.

`System.loadLibrary("foo")`가 성공하려면 target ABI에 맞는 library가 package/device 환경에서 발견되고 dependency도 해결돼야 한다.

### UnsatisfiedLinkError를 분해한다

가능한 원인:

```text
.so 없음
ABI mismatch
transitive dependency 없음
symbol 없음
namespace/search restriction
library corrupted
min API / symbol availability mismatch
```

error text와 ELF dependency를 확인한다.

---

## CHAPTER 24 · JNI signature도 binary boundary다

managed method와 native function의 type/signature가 맞아야 한다.

JNI가 primitive/object reference를 전달하는 규칙, local/global reference lifetime, thread attachment 규칙이 있다.

### wrong ownership

native code가 Java object reference를 call 이후 오래 보관하려면 local reference를 그대로 저장해서는 안 될 수 있다.

GC와 lifetime contract를 따라 global reference 등을 사용해야 한다.

---

## CHAPTER 25 · reproducible build와 binary provenance

같은 source commit에서 나온 binary인지 확인하려면 compiler version, dependency, build flag, timestamp/input 등 environment가 영향을 준다.

reproducible build는 같은 input에서 byte-identical output을 목표로 해 공급망 검증과 debugging에 도움을 줄 수 있다.

### build ID

binary에 build identifier를 넣으면 crash artifact가 어느 exact build인지 symbol server와 연결하기 쉬워진다.

---

## CHAPTER 26 · binary size를 section별로 본다

APK/native binary가 커졌을 때 전체 크기만 보지 않는다.

```text
.text increase
.rodata increase
debug info accidentally packaged
large resource
duplicate native ABI
static library duplication
```

section/resource contribution을 비교한다.

### template/generic code bloat

C++ template나 monomorphization 계열은 type별 code specialization이 많아지면 text size가 증가할 수 있다.

성능 이점과 instruction-cache/binary-size 비용을 함께 본다.

---

## CHAPTER 27 · link-time optimization은 module 경계를 넘어 최적화한다

일반 compile에서는 각 translation unit을 독립적으로 최적화해 다른 object 내부 정보를 제한적으로 본다.

LTO는 link 단계까지 intermediate representation을 유지해 cross-module inline/dead-code elimination 같은 optimization을 가능하게 한다.

대가:

```text
build time/memory 증가
linker/toolchain complexity
profile/debug 변화
```

실제 성능/size를 측정한다.

---

## CHAPTER 28 · undefined symbol을 증거로 디버깅한다

상황:

```text
app: symbol lookup error: foo_v2
```

순서:

```text
1. 어느 binary가 foo_v2를 요구하는가?
2. 그 binary의 DT_NEEDED/shared dependencies는?
3. 실제 load된 library path는?
4. library symbol table에 foo_v2가 exported됐는가?
5. symbol version이 맞는가?
6. architecture/ABI가 맞는가?
7. runtime search path가 예상과 같은가?
```

source repository에서 함수가 `존재한다`는 사실만으로 runtime symbol이 존재한다고 결론 내리지 않는다.

---

## CHAPTER 29 · startup slow를 dynamic linking으로 분해한다

native library 수가 매우 많고 relocation/constructor가 무거우면 process startup 일부를 차지할 수 있다.

trace에서:

```text
dlopen
relocation
JNI_OnLoad
static constructors
```

시간을 본다.

불필요한 library를 지연 load하거나 initialization을 줄일 수 있다.

---

## CHAPTER 30 · binary 사고 모델

source function 하나가 실행되기까지:

```text
source declaration
↓ compiler
object symbol + relocation
↓ linker
executable/shared object
↓ package/storage
loader mapping
↓ relocation/symbol resolution
ABI call boundary
↓
CPU instructions
```

각 경계마다 다른 failure class가 있다.

```text
compile error
link error
load error
symbol resolution error
ABI mismatch
runtime crash
```

이제 error가 나온 stage를 먼저 식별한다.

---

## PART 12 종료 점검

1. object file이 완성된 executable과 다른 이유는 무엇인가?
2. undefined symbol이 compile이 아니라 link/runtime에서 나타날 수 있는 이유는 무엇인가?
3. ELF section과 load segment는 무엇이 다른가?
4. BSS가 process memory에는 크지만 file size에는 작을 수 있는 이유는 무엇인가?
5. relocation이 해결하는 문제는 무엇인가?
6. PIC와 ASLR/dynamic library는 어떻게 연결되는가?
7. static linking과 dynamic linking의 trade-off는 무엇인가?
8. GOT/PLT가 dynamic symbol resolution에서 어떤 indirection을 제공하는가?
9. ABI가 API보다 낮은 층에서 어떤 것을 규정하는가?
10. struct padding을 binary serialization로 그대로 쓰면 왜 위험한가?
11. ASLR 환경에서 native crash address를 어떻게 symbolization하는가?
12. Android UnsatisfiedLinkError를 어떤 원인 범주로 나눌 수 있는가?
13. LTO가 최적화 범위와 build cost를 어떻게 바꾸는가?
14. source에 함수가 존재해도 runtime symbol이 없을 수 있는 이유는 무엇인가?

이제 `컴파일됐다`와 `실행할 수 있다` 사이를 비워 두지 않는다. **object → symbol → relocation → link → loader → ABI → instruction**을 연결해 설명할 수 있어야 한다.