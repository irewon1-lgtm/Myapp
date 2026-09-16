# PART 12 · binary linking과 ABI — object identity에서 loader state까지

native binary correctness는 source code가 compile되는 순간 끝나지 않는다. object file의 symbol과 relocation, linker 배치, dynamic loader의 dependency resolution, ABI의 calling/layout 규칙이 모두 맞아야 process image가 성립한다. binary 문제는 source-level type만 보고 고칠 수 없다.

---

## CHAPTER 01 · relocatable object는 아직 완성되지 않은 machine-code graph다

separate compilation에서는 translation unit마다 code/data가 relocatable object에 저장된다. object에는 instruction bytes뿐 아니라 section metadata, symbol table, relocation record, debug/unwind information이 들어갈 수 있다. 다른 object가 제공할 symbol의 최종 주소를 아직 모르기 때문에 일부 instruction/data field는 linker가 나중에 보정한다.

object를 분석할 때 source file 이름보다 section, symbol definition/reference, relocation target을 본다. `컴파일 성공`은 frontend와 code generation이 해당 unit을 처리했다는 뜻일 뿐, program 전체의 모든 reference가 해결되었다는 뜻이 아니다. link failure를 source syntax problem으로 되돌려 조사하면 원인 공간이 불필요하게 넓어진다.

---

## CHAPTER 02 · symbol은 이름·binding·visibility·section을 묶은 binary identity다

linker symbol은 함수나 global object의 binary-level identity를 제공한다. defined/undefined 상태, local/global/weak binding, visibility, section index 같은 metadata가 resolution에 영향을 준다. 동일 source identifier라도 compiler-generated symbol이나 mangled name이 실제 linker identity가 될 수 있다.

undefined symbol은 반드시 `함수가 없다`는 뜻이 아니다. library가 link input에 빠졌거나, symbol visibility가 hidden이거나, versioned symbol이 맞지 않거나, C++ name mangling/ABI가 달라 lookup이 실패할 수 있다. symbol table과 dynamic dependency를 직접 확인해 어느 object가 definition을 제공해야 하는지 증명한다.

---

## CHAPTER 03 · binding rule은 여러 definition이 있을 때 어느 것이 선택되는지 결정한다

strong/weak symbol 규칙과 dynamic symbol interposition은 동일 이름의 여러 definition이 존재할 때 결과를 바꿀 수 있다. weak default implementation을 application이 override하는 패턴은 편리하지만 dependency upgrade에서 우연히 다른 symbol이 선택되는 위험을 만든다.

link order가 결과에 영향을 줄 수 있는 archive/static-library resolution도 있다. `-lA -lB`를 뒤집어 해결되는 현상을 단순 linker 버그로 보지 않고 archive member extraction rule과 unresolved set의 변화를 본다. link line 자체를 reproducible artifact로 보존해야 하는 이유다.

---

## CHAPTER 04 · section과 segment는 파일 조직과 runtime mapping이라는 다른 관점이다

section은 linker/debugger가 code, data, relocation, symbol 정보를 조직하는 논리 단위이고 segment는 loader가 process memory에 mapping할 범위를 기술하는 runtime 단위다. 여러 section이 하나의 loadable segment에 포함될 수 있다.

보안과 memory permission은 segment 관점이 중요하다. executable code가 writable segment에 섞이면 W^X 원칙을 약화시킬 수 있고, read-only relocation이 가능한 영역은 relocation 완료 후 protection을 강화할 수 있다. ELF를 볼 때 `.text` 이름만 확인하지 않고 program header와 page permission까지 본다.

---

## CHAPTER 05 · data section의 초기값과 zero-fill은 file size와 memory size를 다르게 만든다

initialized writable data는 file image에 실제 bytes가 필요하지만 zero-initialized global storage는 file에 모든 zero를 저장하지 않고 memory size metadata로 표현할 수 있다. 따라서 executable file 크기와 runtime writable memory 크기는 직접 비례하지 않는다.

binary size optimization에서 `.bss`와 file-backed data를 구분하지 않으면 효과를 잘못 평가한다. large lookup table, embedded resource, relocation-heavy data가 어느 section에 들어가는지 map file로 확인한다. memory footprint는 mapping 이후 COW/private dirty page까지 포함해 별도로 측정한다.

---

## CHAPTER 06 · relocation은 symbolic reference를 concrete address/offset 표현으로 고친다

compiler는 다른 symbol의 최종 위치를 모를 때 relocation entry를 남긴다. linker나 dynamic loader는 symbol value, relocation type, place address, addend를 이용해 instruction/data field를 수정한다. relocation type은 architecture마다 달라 instruction encoding과 직접 연결된다.

`relocation truncated to fit` 같은 오류는 이름 해석 문제가 아니라 relocation field가 표현할 수 있는 displacement/range를 초과했음을 의미할 수 있다. code model, section placement, PIC strategy를 검토해야 한다. source call을 다른 함수로 바꾸는 임시 수정은 binary layout 문제를 숨길 뿐이다.

---

## CHAPTER 07 · position-independent code는 load address 변화와 code sharing을 가능하게 한다

PIC/PIE는 absolute address를 code 곳곳에 박는 대신 PC-relative addressing이나 indirection table을 이용해 다양한 load address에서 동작하게 한다. 이는 ASLR과 shared-library page sharing에 유리하다.

PIC에는 GOT/PLT indirection, register usage 같은 비용이 생길 수 있지만 modern architecture/compiler에서는 비용 구조가 단순하지 않다. `PIC는 느리다`라는 일반론 대신 generated instruction과 relocation 수를 확인한다. security hardening과 performance의 실제 차이를 benchmark한다.

---

## CHAPTER 08 · static과 dynamic linking은 dependency resolution 시점과 update boundary가 다르다

static linking은 필요한 object code를 final binary에 포함해 runtime external dependency를 줄일 수 있지만 binary size와 update duplication이 증가할 수 있다. dynamic linking은 shared library를 runtime에 mapping하고 symbol을 resolve하므로 common code sharing과 independent update가 가능하지만 loader compatibility가 필요하다.

보안 업데이트 관점도 다르다. dynamic library 하나의 patch가 여러 binary에 적용될 수 있지만 ABI compatibility가 깨지면 전체 ecosystem이 영향을 받는다. static dependency는 application rebuild/redeploy가 필요하다. dependency policy는 size만이 아니라 patch distribution과 reproducibility까지 고려한다.

---

## CHAPTER 09 · dynamic loader는 dependency graph를 process mapping으로 구체화한다

process start에서 loader는 executable의 dynamic section을 읽고 필요한 shared object를 찾고 mapping하며 relocation과 initialization을 수행한다. dependency graph가 깊거나 relocation/symbol 수가 많으면 startup critical path가 길어질 수 있다.

loader failure는 file missing만이 아니다. incompatible ELF class/machine, missing symbol version, text relocation restriction, namespace policy, wrong architecture가 원인이 될 수 있다. loader diagnostic과 실제 mapped object list를 확인한다. 동일 library name의 다른 copy가 선택된 경우 path보다 inode/build ID로 식별한다.

---

## CHAPTER 10 · library search path는 dependency confusion attack surface가 될 수 있다

loader가 여러 directory에서 library를 검색할 때 untrusted writable directory가 우선순위에 들어가면 공격자가 같은 이름의 shared object를 주입할 수 있다. current-directory search, environment variable, rpath/runpath 정책을 production에서 엄격히 관리한다.

`LD_LIBRARY_PATH를 추가하면 해결`은 debugging 임시 수단일 수 있지만 production fix로 남기면 artifact identity가 environment에 의존하게 된다. absolute/controlled search root, signed package boundary, namespace restriction을 사용해 어떤 library가 선택되는지 결정적으로 만든다.

---

## CHAPTER 11 · GOT와 PLT는 dynamic symbol address를 code에서 분리한다

Global Offset Table은 dynamic object/data address indirection에 사용되고 Procedure Linkage Table은 external function call을 dynamic resolver와 연결하는 architecture/ABI mechanism에 사용될 수 있다. 구체 instruction sequence는 architecture와 linker에 따라 다르다.

GOT/PLT를 이해하면 `함수 call instruction이 직접 final function address를 가리키지 않는다`는 점을 설명할 수 있다. dynamic interposition, lazy binding, RELRO hardening이 이 table과 연결된다. disassembly에서 PLT stub를 실제 application function body로 오해하지 않는다.

---

## CHAPTER 12 · lazy binding은 startup 비용을 첫 호출로 미룬다

일부 dynamic linking mode는 external function address resolution을 처음 호출할 때 수행한다. startup relocation 비용을 줄일 수 있지만 first-call latency와 writable resolver state를 만든다. eager binding은 startup에서 모두 resolve해 이후 call path를 단순화할 수 있다.

보안 hardening에서는 full RELRO/now binding과 같은 조합이 writable relocation target을 줄일 수 있다. 성능 선택은 symbol 수, startup SLO, first-interaction latency를 모두 본다. loader option 이름만으로 정책을 선택하지 않는다.

---

## CHAPTER 13 · symbol interposition은 debugging hook이면서 optimization barrier가 될 수 있다

shared object의 external symbol이 runtime에 다른 definition으로 대체될 가능성이 있으면 compiler/linker가 call target을 고정하지 못할 수 있다. visibility를 hidden/local로 제한하면 interposition 가능성을 줄여 optimization과 startup resolution 비용을 개선할 수 있다.

LD_PRELOAD 같은 mechanism은 tracing/testing에 유용하지만 production behavior를 바꿀 수 있다. interposed allocator/network call이 recursion 또는 ABI mismatch를 만들 수 있으므로 debugging injection이 원래 failure를 재현하는지 확인한다.

---

## CHAPTER 14 · ABI는 source-compatible component가 binary-incompatible할 수 있게 만든다

ABI는 calling convention, register preservation, stack alignment, primitive size/alignment, object layout, exception/unwind convention 등을 정한다. header/source가 동일해도 compiler option이나 ABI version이 다르면 binary가 호환되지 않을 수 있다.

public native library는 API와 ABI compatibility를 별도 관리한다. struct에 field를 중간 삽입하거나 enum underlying size를 바꾸면 source recompilation 없이 기존 caller가 잘못된 offset을 사용할 수 있다. binary compatibility checker와 symbol/version policy를 release gate에 둔다.

---

## CHAPTER 15 · name mangling은 overload와 namespace 정보를 symbol identity에 인코딩한다

C++ compiler는 함수 이름만으로 overload를 구분할 수 없으므로 parameter type, namespace 등의 정보를 encoded symbol name에 포함한다. ABI별 mangling rule이 다르면 서로 다른 toolchain component가 동일 source function을 같은 symbol로 보지 않을 수 있다.

C interface를 외부 ABI 안정화 layer로 사용하는 이유 중 하나가 더 단순한 symbol/calling contract다. JNI/FFI boundary에서는 generated/native signature를 실제 symbol과 대조한다. manual spelling에 의존하면 package/class rename에서 runtime lookup이 깨질 수 있다.

---

## CHAPTER 16 · struct layout은 field order, alignment, padding의 결과다

compiler는 각 field의 alignment를 만족시키기 위해 padding을 넣고 struct 전체 alignment에 맞춰 tail padding을 둘 수 있다. 따라서 source field size 합이 `sizeof(struct)`와 다를 수 있다.

wire format, disk format, shared-memory protocol에 native struct bytes를 그대로 사용하면 compiler/architecture가 바뀔 때 layout이 깨진다. external format은 explicit field encoding을 사용한다. unavoidable shared ABI라면 static assertion으로 size/offset을 검증하고 version을 관리한다.

---

## CHAPTER 17 · stack alignment는 call boundary 전체가 지켜야 하는 invariant다

ABI는 function entry에서 stack pointer alignment를 요구할 수 있다. assembly/JIT/FFI stub 하나가 이를 깨뜨리면 downstream compiler-generated code가 aligned SIMD access를 가정해 crash할 수 있다.

crash가 library 내부 instruction에서 발생해도 실제 원인은 caller의 malformed frame일 수 있다. register/stack dump로 call boundary를 역추적한다. hand-written assembly와 signal trampoline은 unwind/alignment metadata까지 포함해 검증한다.

---

## CHAPTER 18 · unwind metadata는 optimized native stack을 복원하는 계약이다

frame pointer가 생략되고 code가 inline/reorder될 수 있는 optimized binary에서는 단순 stack memory scan으로 call chain을 안정적으로 복원할 수 없다. DWARF CFI나 architecture-specific unwind table이 register 복원 규칙을 제공한다.

release crash debugging을 위해 stripped runtime binary와 별도의 symbol/unwind artifact를 build ID로 연결해 보존한다. wrong-version symbol을 사용하면 그럴듯하지만 틀린 stack이 생성될 수 있다. symbol server는 content-addressed identity를 사용한다.

---

## CHAPTER 19 · ASLR은 address를 무작위화하지만 정보 leak과 code reuse 위험을 함께 본다

PIE와 shared library를 다양한 virtual address에 배치하면 공격자가 absolute address를 미리 아는 것을 어렵게 만든다. 그러나 pointer leak이 있으면 randomization entropy가 무력화될 수 있다.

ASLR은 memory safety bug를 제거하지 않는다. stack canary, CFI, DEP/W^X, hardened allocator와 함께 exploitation cost를 높이는 defense-in-depth layer다. crash reproduction에서는 address가 실행마다 바뀌므로 module-relative offset과 build ID를 사용한다.

---

## CHAPTER 20 · RELRO는 relocation 이후 writable metadata를 read-only로 전환한다

dynamic relocation을 위해 startup 중 writable해야 하는 table이 relocation 완료 뒤에도 writable하면 memory corruption이 control-flow target overwrite로 이어질 수 있다. RELRO는 가능한 영역을 read-only로 보호한다.

partial/full RELRO의 protection 범위와 binding mode를 구분한다. hardening flag가 build command에 있다고 실제 final ELF에 적용됐다는 보장은 없으므로 program header/dynamic flag를 release artifact에서 검사한다.

---

## CHAPTER 21 · PIE는 main executable에도 relocation-independent addressing을 적용한다

traditional fixed-address executable은 main image ASLR을 제한할 수 있다. PIE는 executable을 shared-object와 유사하게 relocatable하게 만들어 random base address에 load할 수 있게 한다.

PIE 적용 여부는 source code만 보고 알 수 없다. compiler와 linker flag, final ELF type/relocation을 검사한다. JIT-generated code나 native plugin 같은 다른 executable region의 W^X policy도 별도 검토한다.

---

## CHAPTER 22 · shared-library compatibility는 symbol 존재보다 semantics까지 포함한다

동일 symbol signature가 남아 있어도 ownership, error convention, thread-safety, struct meaning이 바뀌면 semantic ABI compatibility가 깨진다. binary는 load되지만 runtime corruption이 생겨 더 위험할 수 있다.

library versioning은 exported symbol set과 behavior contract를 함께 테스트한다. old consumer binary를 new library에 실제로 연결·실행하는 compatibility test를 유지한다. only-recompile test는 배포된 과거 consumer를 대표하지 못한다.

---

## CHAPTER 23 · Android native loader는 namespace와 APK packaging policy의 영향을 받는다

Android는 native library loading에 namespace와 public/private library policy를 적용한다. app이 platform private library에 우연히 의존하면 OS update에서 load failure가 생길 수 있다.

APK/AAB에 ABI별 library가 올바른 path로 포함되었는지, extract/load mode와 target SDK policy가 맞는지 확인한다. `System.loadLibrary` 실패 시 library name만 보지 않고 namespace, dependency chain, ABI, ELF class를 진단한다.

---

## CHAPTER 24 · JNI signature는 managed type과 native entry point 사이의 ABI adapter다

JNI는 object reference, primitive representation, exception state, thread attachment 규칙을 정의한다. native function이 managed object pointer를 raw process-lifetime pointer처럼 보관하면 GC/lifetime 규칙을 위반한다.

local/global/weak reference를 구분하고 long-running native thread는 VM attach/detach를 관리한다. pending exception 상태에서 허용되지 않은 JNI operation을 이어가면 추가 failure를 만든다. boundary wrapper에서 exception/ownership을 명시적으로 처리한다.

---

## CHAPTER 25 · reproducible build는 source revision과 binary identity 사이의 증거를 강화한다

동일 source와 declared build input에서 bit-identical output이 가능하면 artifact provenance를 검증하기 쉽다. timestamp, nondeterministic file order, absolute build path가 output에 섞이면 동일 source의 binary hash가 달라진다.

reproducibility는 supply-chain compromise 탐지와 rollback audit에 유용하지만 compiler 자체 신뢰 문제를 완전히 해결하지 않는다. toolchain digest, dependency lock, environment를 SBOM/provenance와 함께 기록한다.

---

## CHAPTER 26 · binary size는 code, data, symbol, relocation을 따로 최적화한다

APK/library 크기가 커진 원인을 `코드가 많다`로 끝내지 않는다. text/rodata/data, debug symbol, relocation, duplicate template instantiation, embedded resource 비중을 map/size tool로 분해한다.

on-disk compressed size, installed size, mapped RSS는 서로 다르다. size optimization 목표를 download, storage, startup I/O, memory 중 무엇인지 먼저 정한다. aggressive compression이 startup CPU를 늘릴 수도 있다.

---

## CHAPTER 27 · LTO는 translation-unit 경계를 넘어 optimization하지만 build contract를 넓힌다

Link-Time Optimization은 object 수준 IR을 linker 단계까지 유지해 cross-module inlining, dead-code elimination 같은 optimization을 가능하게 한다. 성능과 size가 좋아질 수 있지만 link memory/time과 debug complexity가 증가한다.

LTO가 ABI 문제를 고쳐 주지는 않는다. visibility와 undefined behavior가 더 공격적으로 최적화되어 latent bug가 드러날 수 있다. release build에서만 발생하는 failure는 LTO on/off differential을 evidence로 사용하되 원인은 source invariant까지 추적한다.

---

## CHAPTER 28 · symbol debugging은 build ID를 중심으로 artifact를 매칭한다

production crash address를 source line으로 변환하려면 exact executable/shared object와 debug symbol이 필요하다. filename/version string만으로는 동일 release name의 rebuild를 구분하기 어렵다.

build ID 또는 content hash를 crash report에 포함하고 CI artifact store에서 정확한 symbol을 찾는다. stripped binary와 debug companion의 ID 일치를 자동 검사한다. symbol mismatch는 잘못된 원인 분석을 낳는 silent corruption이다.

---

## CHAPTER 29 · linking과 loading도 startup critical path를 구성한다

shared object 수, relocation 수, symbol lookup, page fault가 application startup latency에 영향을 줄 수 있다. native-heavy application에서 code-level initialization을 줄였는데 startup이 개선되지 않으면 loader trace를 본다.

library consolidation은 relocation/search 비용을 줄일 수 있지만 incremental update와 component isolation을 약화할 수 있다. startup optimization은 binary layout, prefetch, relocation, class/runtime initialization을 하나의 timeline으로 측정한다.

---

## CHAPTER 30 · binary 문제는 source→object→link→load→call boundary를 순서대로 검증한다

`undefined symbol`이나 native crash를 만났을 때 source를 임의 수정하지 않는다. 먼저 target architecture/ABI, object symbol/relocation, final dependency graph, loader-selected library, runtime call boundary를 확인한다.

각 단계의 artifact가 다음 단계 input과 일치한다는 증거를 남기면 binary failure를 재현 가능하게 좁힐 수 있다. native system의 핵심은 기계어를 읽는 기술 자체가 아니라 **binary contract가 어느 경계에서 깨졌는지 식별하는 능력**이다.