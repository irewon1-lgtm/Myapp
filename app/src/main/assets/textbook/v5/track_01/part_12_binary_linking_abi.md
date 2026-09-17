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

원인을 확인할 때 final link map에서 해당 symbol이 **어느 object/archive member에서 선택됐는지**와 버려진 후보를 함께 본다. weak definition이 예상치 못하게 선택됐다면 symbol visibility와 archive extraction 순서를 재현한다. whole-archive나 group 옵션으로 증상이 사라지는 경우도 dependency cycle과 extraction rule을 숨길 수 있으므로, 왜 그 member가 원래 unresolved set에 들어오지 않았는지까지 추적해야 안정적인 수정이 된다.

---

## CHAPTER 04 · section과 segment는 파일 조직과 runtime mapping이라는 다른 관점이다

section은 linker/debugger가 code, data, relocation, symbol 정보를 조직하는 논리 단위이고 segment는 loader가 process memory에 mapping할 범위를 기술하는 runtime 단위다. 여러 section이 하나의 loadable segment에 포함될 수 있다.

보안과 memory permission은 segment 관점이 중요하다. executable code가 writable segment에 섞이면 W^X 원칙을 약화시킬 수 있고, read-only relocation이 가능한 영역은 relocation 완료 후 protection을 강화할 수 있다. ELF를 볼 때 `.text` 이름만 확인하지 않고 program header와 page permission까지 본다.

linker script나 section alignment가 바뀌면 예상하지 못한 section이 같은 load segment에 합쳐져 permission이 넓어질 수 있다. release gate에서는 section name이 아니라 program header의 file offset, virtual address, mem/file size, R/W/X flag를 검사한다. memory map과 ELF header를 대조하면 runtime에서 실제로 어떤 page가 executable+writable인지, large alignment hole이 RSS/startup I/O에 영향을 주는지까지 확인할 수 있다.

---

## CHAPTER 05 · data section의 초기값과 zero-fill은 file size와 memory size를 다르게 만든다

initialized writable data는 file image에 실제 bytes가 필요하지만 zero-initialized global storage는 file에 모든 zero를 저장하지 않고 memory size metadata로 표현할 수 있다. 따라서 executable file 크기와 runtime writable memory 크기는 직접 비례하지 않는다.

binary size optimization에서 `.bss`와 file-backed data를 구분하지 않으면 효과를 잘못 평가한다. large lookup table, embedded resource, relocation-heavy data가 어느 section에 들어가는지 map file로 확인한다. memory footprint는 mapping 이후 COW/private dirty page까지 포함해 별도로 측정한다.

`.bss`가 파일에는 거의 공간을 쓰지 않아도 startup 시 virtual address와 실제 faulted page를 소비할 수 있고, fork/zygote 계열 환경에서는 write pattern에 따라 private page가 증가한다. 반대로 큰 read-only table은 APK/file size를 키워도 여러 process가 clean page로 공유할 수 있다. size regression을 조사할 때 section별 on-disk bytes, mapped pages, private dirty/PSS를 나눠야 download 크기와 runtime memory 문제를 혼동하지 않는다.

---

## CHAPTER 06 · relocation은 symbolic reference를 concrete address/offset 표현으로 고친다

compiler는 다른 symbol의 최종 위치를 모를 때 relocation entry를 남긴다. linker나 dynamic loader는 symbol value, relocation type, place address, addend를 이용해 instruction/data field를 수정한다. relocation type은 architecture마다 달라 instruction encoding과 직접 연결된다.

`relocation truncated to fit` 같은 오류는 이름 해석 문제가 아니라 relocation field가 표현할 수 있는 displacement/range를 초과했음을 의미할 수 있다. code model, section placement, PIC strategy를 검토해야 한다. source call을 다른 함수로 바꾸는 임시 수정은 binary layout 문제를 숨길 뿐이다.

relocation 오류를 재현할 때 relocation entry의 type, source section/offset, target symbol value, computed displacement를 수치로 남기면 범위 초과인지 잘못된 architecture object 혼합인지 분리할 수 있다. linker relaxation이나 thunk/veneer 생성이 가능한 architecture에서는 최종 link가 어떤 우회 sequence를 만들었는지도 disassembly로 확인한다. build flag 변경으로 우연히 layout이 가까워져 성공했다고 해서 근본 contract가 해결된 것은 아니다.

---

## CHAPTER 07 · position-independent code는 load address 변화와 code sharing을 가능하게 한다

PIC/PIE는 absolute address를 code 곳곳에 박는 대신 PC-relative addressing이나 indirection table을 이용해 다양한 load address에서 동작하게 한다. 이는 ASLR과 shared-library page sharing에 유리하다.

PIC에는 GOT/PLT indirection, register usage 같은 비용이 생길 수 있지만 modern architecture/compiler에서는 비용 구조가 단순하지 않다. `PIC는 느리다`라는 일반론 대신 generated instruction과 relocation 수를 확인한다. security hardening과 performance의 실제 차이를 benchmark한다.

text relocation이 남으면 shared code page를 loader가 수정해야 해 page sharing과 W^X 정책에 불리할 수 있다. final ELF에서 dynamic relocation target이 writable data/GOT에 한정되는지 검사하고, architecture별 PC-relative range를 넘는 large model도 확인한다. PIC/non-PIC 비교는 동일 optimization level에서 startup relocation count, text private-dirty, steady CPU를 함께 측정해야 security 옵션의 실제 비용을 과장하지 않는다.

---

## CHAPTER 08 · static과 dynamic linking은 dependency resolution 시점과 update boundary가 다르다

static linking은 필요한 object code를 final binary에 포함해 runtime external dependency를 줄일 수 있지만 binary size와 update duplication이 증가할 수 있다. dynamic linking은 shared library를 runtime에 mapping하고 symbol을 resolve하므로 common code sharing과 independent update가 가능하지만 loader compatibility가 필요하다.

보안 업데이트 관점도 다르다. dynamic library 하나의 patch가 여러 binary에 적용될 수 있지만 ABI compatibility가 깨지면 전체 ecosystem이 영향을 받는다. static dependency는 application rebuild/redeploy가 필요하다. dependency policy는 size만이 아니라 patch distribution과 reproducibility까지 고려한다.

---

## CHAPTER 09 · dynamic loader는 dependency graph를 process mapping으로 구체화한다

process start에서 loader는 executable의 dynamic section을 읽고 필요한 shared object를 찾고 mapping하며 relocation과 initialization을 수행한다. dependency graph가 깊거나 relocation/symbol 수가 많으면 startup critical path가 길어질 수 있다.

loader failure는 file missing만이 아니다. incompatible ELF class/machine, missing symbol version, text relocation restriction, namespace policy, wrong architecture가 원인이 될 수 있다. loader diagnostic과 실제 mapped object list를 확인한다. 동일 library name의 다른 copy가 선택된 경우 path보다 inode/build ID로 식별한다.

loader startup을 분석할 때 dependency graph의 각 node에 resolved path, build ID, mapping address, relocation count, constructor 실행시간을 붙이면 어디서 실패하거나 지연됐는지 확인하기 쉽다. `DT_NEEDED`에는 직접 dependency만 보일 수 있고 transitive dependency에서 symbol/version failure가 발생할 수 있으므로 전체 graph를 수집한다. 동일 soname이 여러 namespace/path에 존재한다면 최종 선택된 object의 identity를 process map과 함께 보존한다.

---

## CHAPTER 10 · library search path는 dependency confusion attack surface가 될 수 있다

loader가 여러 directory에서 library를 검색할 때 untrusted writable directory가 우선순위에 들어가면 공격자가 같은 이름의 shared object를 주입할 수 있다. current-directory search, environment variable, rpath/runpath 정책을 production에서 엄격히 관리한다.

`LD_LIBRARY_PATH를 추가하면 해결`은 debugging 임시 수단일 수 있지만 production fix로 남기면 artifact identity가 environment에 의존하게 된다. absolute/controlled search root, signed package boundary, namespace restriction을 사용해 어떤 library가 선택되는지 결정적으로 만든다.

search-order 문제는 정상/공격 시나리오 모두에서 실제 resolver trace로 검증한다. executable의 RPATH/RUNPATH, environment, default path, namespace별 allowed path를 표로 만들고 writable directory가 후보에 들어가지 않는지 확인한다. deployment마다 동일 soname이 다른 build ID로 resolve되지 않도록 selected dependency manifest를 수집할 수 있다. temporary debug path를 production image에 남기지 않도록 release gate에서 dynamic tag와 environment override를 검사한다.

---

## CHAPTER 11 · GOT와 PLT는 dynamic symbol address를 code에서 분리한다

Global Offset Table은 dynamic object/data address indirection에 사용되고 Procedure Linkage Table은 external function call을 dynamic resolver와 연결하는 architecture/ABI mechanism에 사용될 수 있다. 구체 instruction sequence는 architecture와 linker에 따라 다르다.

GOT/PLT를 이해하면 `함수 call instruction이 직접 final function address를 가리키지 않는다`는 점을 설명할 수 있다. dynamic interposition, lazy binding, RELRO hardening이 이 table과 연결된다. disassembly에서 PLT stub를 실제 application function body로 오해하지 않는다.

성능/보안 분석에서는 call site→PLT→GOT slot→resolved symbol의 chain을 확인한다. direct/hidden binding으로 바뀌면 PLT hop이 제거될 수 있지만 interposition semantics도 달라질 수 있다. GOT slot이 언제 writable에서 read-only로 전환되는지와 lazy resolver가 어떤 slot을 갱신하는지도 RELRO/now 설정과 함께 본다. crash PC가 PLT 영역이라면 실제 target resolution과 corrupt slot을 먼저 확인한다.

---

## CHAPTER 12 · lazy binding은 startup 비용을 첫 호출로 미룬다

일부 dynamic linking mode는 external function address resolution을 처음 호출할 때 수행한다. startup relocation 비용을 줄일 수 있지만 first-call latency와 writable resolver state를 만든다. eager binding은 startup에서 모두 resolve해 이후 call path를 단순화할 수 있다.

보안 hardening에서는 full RELRO/now binding과 같은 조합이 writable relocation target을 줄일 수 있다. 성능 선택은 symbol 수, startup SLO, first-interaction latency를 모두 본다. loader option 이름만으로 정책을 선택하지 않는다.

lazy binding이 사용자 첫 interaction에서 발생하면 cold-start metric에는 안 보이면서 첫 기능 latency를 악화시킬 수 있다. loader trace로 resolve event와 target symbol을 기록하고, eager mode에서는 startup relocation 시간이 얼마나 늘어나는지 비교한다. multithreaded first call이 동시에 같은 symbol을 resolve하는 path와 resolver lock도 tail 원인이 될 수 있다. 보안 정책상 eager binding이 필수라면 startup budget 안에서 dependency/symbol 수를 줄이는 방향으로 최적화한다.

---

## CHAPTER 13 · symbol interposition은 debugging hook이면서 optimization barrier가 될 수 있다

shared object의 external symbol이 runtime에 다른 definition으로 대체될 가능성이 있으면 compiler/linker가 call target을 고정하지 못할 수 있다. visibility를 hidden/local로 제한하면 interposition 가능성을 줄여 optimization과 startup resolution 비용을 개선할 수 있다.

LD_PRELOAD 같은 mechanism은 tracing/testing에 유용하지만 production behavior를 바꿀 수 있다. interposed allocator/network call이 recursion 또는 ABI mismatch를 만들 수 있으므로 debugging injection이 원래 failure를 재현하는지 확인한다.

interposition을 사용하는 테스트에서는 원래 target과 replacement의 calling convention, errno/exception, allocation ownership을 동일하게 유지해야 한다. hook 내부에서 다시 같은 symbol을 호출해 recursion이 생기지 않도록 next-resolution semantics를 검증한다. production build에서는 export가 불필요한 symbol을 hidden으로 제한하고 final dynamic symbol table을 검사하면 의도하지 않은 ABI surface와 optimization barrier를 동시에 줄일 수 있다.

---

## CHAPTER 14 · ABI는 source-compatible component가 binary-incompatible할 수 있게 만든다

ABI는 calling convention, register preservation, stack alignment, primitive size/alignment, object layout, exception/unwind convention 등을 정한다. header/source가 동일해도 compiler option이나 ABI version이 다르면 binary가 호환되지 않을 수 있다.

public native library는 API와 ABI compatibility를 별도 관리한다. struct에 field를 중간 삽입하거나 enum underlying size를 바꾸면 source recompilation 없이 기존 caller가 잘못된 offset을 사용할 수 있다. binary compatibility checker와 symbol/version policy를 release gate에 둔다.

호환성 검증에는 새 header로 새 client를 compile하는 테스트뿐 아니라 **과거 binary client를 그대로 새 library에 실행**하는 테스트가 필요하다. compiler flag가 structure packing, exception model, visibility에 영향을 주면 version string이 같아도 ABI가 달라질 수 있다. exported symbol signature, type layout fingerprint, calling convention을 release artifact에서 비교하고 breaking change는 soname/versioned symbol 같은 명시적 boundary로 분리한다.

---

## CHAPTER 15 · name mangling은 overload와 namespace 정보를 symbol identity에 인코딩한다

C++ compiler는 함수 이름만으로 overload를 구분할 수 없으므로 parameter type, namespace 등의 정보를 encoded symbol name에 포함한다. ABI별 mangling rule이 다르면 서로 다른 toolchain component가 동일 source function을 같은 symbol로 보지 않을 수 있다.

C interface를 외부 ABI 안정화 layer로 사용하는 이유 중 하나가 더 단순한 symbol/calling contract다. JNI/FFI boundary에서는 generated/native signature를 실제 symbol과 대조한다. manual spelling에 의존하면 package/class rename에서 runtime lookup이 깨질 수 있다.

demangled 이름은 사람이 읽기 위한 표현일 뿐 loader가 실제로 찾는 raw symbol identity는 mangled form이다. undefined-symbol incident에서는 consumer object의 undefined raw symbol과 provider library의 exported raw symbol을 직접 비교한다. toolchain major change나 `_GLIBCXX_USE_CXX11_ABI`류 설정처럼 type encoding/standard-library ABI가 달라지는 경우 source 함수 이름이 같아도 symbol이 달라질 수 있다. external plugin ABI는 가능한 한 안정된 C surface나 versioned adapter로 격리한다.

---

## CHAPTER 16 · struct layout은 field order, alignment, padding의 결과다

compiler는 각 field의 alignment를 만족시키기 위해 padding을 넣고 struct 전체 alignment에 맞춰 tail padding을 둘 수 있다. 따라서 source field size 합이 `sizeof(struct)`와 다를 수 있다.

wire format, disk format, shared-memory protocol에 native struct bytes를 그대로 사용하면 compiler/architecture가 바뀔 때 layout이 깨진다. external format은 explicit field encoding을 사용한다. unavoidable shared ABI라면 static assertion으로 size/offset을 검증하고 version을 관리한다.

bitfield, packing pragma, flexible member, vector type은 layout portability를 더 어렵게 만든다. cross-language FFI에서는 양쪽 compiler가 같은 alignment/padding 규칙을 적용하는지 generated binding이나 layout test로 확인한다. `sizeof`만 같아도 각 field offset이 다를 수 있으므로 exported ABI type은 size, align, offsetof를 모두 golden contract로 검사한다. serialized data는 endian과 integer width도 명시해 native layout과 분리한다.

---

## CHAPTER 17 · stack alignment는 call boundary 전체가 지켜야 하는 invariant다

ABI는 function entry에서 stack pointer alignment를 요구할 수 있다. assembly/JIT/FFI stub 하나가 이를 깨뜨리면 downstream compiler-generated code가 aligned SIMD access를 가정해 crash할 수 있다.

crash가 library 내부 instruction에서 발생해도 실제 원인은 caller의 malformed frame일 수 있다. register/stack dump로 call boundary를 역추적한다. hand-written assembly와 signal trampoline은 unwind/alignment metadata까지 포함해 검증한다.

variable-size stack allocation이나 custom context switch는 alignment를 동적으로 깨뜨릴 수 있으므로 모든 exit/call path에서 stack pointer invariant를 확인한다. sanitizer/debug build가 prologue를 바꿔 문제를 숨길 수도 있어 release-like binary disassembly가 필요하다. faulting SIMD instruction 주소와 당시 SP modulo alignment를 dump에서 계산하면 library 내부 버그와 caller ABI violation을 분리할 수 있다. callback ABI도 정방향 호출과 동일하게 검증한다.

---

## CHAPTER 18 · unwind metadata는 optimized native stack을 복원하는 계약이다

frame pointer가 생략되고 code가 inline/reorder될 수 있는 optimized binary에서는 단순 stack memory scan으로 call chain을 안정적으로 복원할 수 없다. DWARF CFI나 architecture-specific unwind table이 register 복원 규칙을 제공한다.

release crash debugging을 위해 stripped runtime binary와 별도의 symbol/unwind artifact를 build ID로 연결해 보존한다. wrong-version symbol을 사용하면 그럴듯하지만 틀린 stack이 생성될 수 있다. symbol server는 content-addressed identity를 사용한다.

JIT/handwritten assembly가 unwind rule을 제공하지 않으면 stack이 그 frame에서 끊길 수 있다. exception unwinding과 profiler/crash unwinding이 요구하는 metadata가 같은지 platform ABI를 확인한다. build pipeline에서 stripped binary와 debug companion의 build ID 일치, unwind section 보존, symbol server 업로드를 자동 검사한다. stack corruption이 의심될 때는 unwind 결과가 불완전할 수 있음을 표시하고 register/stack memory와 함께 해석한다.

---

## CHAPTER 19 · ASLR은 address를 무작위화하지만 정보 leak과 code reuse 위험을 함께 본다

PIE와 shared library를 다양한 virtual address에 배치하면 공격자가 absolute address를 미리 아는 것을 어렵게 만든다. 그러나 pointer leak이 있으면 randomization entropy가 무력화될 수 있다.

ASLR은 memory safety bug를 제거하지 않는다. stack canary, CFI, DEP/W^X, hardened allocator와 함께 exploitation cost를 높이는 defense-in-depth layer다. crash reproduction에서는 address가 실행마다 바뀌므로 module-relative offset과 build ID를 사용한다.

실제 entropy는 architecture virtual-address width, alignment, loader policy, mapping 수에 제한되므로 단순 “ASLR enabled” boolean보다 여러 실행의 module base 분포를 확인할 수 있다. core dump/log에 raw pointer를 남기는 기능은 보안상 address disclosure가 될 수 있어 접근 통제가 필요하다. crash grouping은 absolute PC 대신 module build ID + relative offset을 사용하면 randomization을 유지하면서 동일 failure를 안정적으로 묶을 수 있다.

---

## CHAPTER 20 · RELRO는 relocation 이후 writable metadata를 read-only로 전환한다

dynamic relocation을 위해 startup 중 writable해야 하는 table이 relocation 완료 뒤에도 writable하면 memory corruption이 control-flow target overwrite로 이어질 수 있다. RELRO는 가능한 영역을 read-only로 보호한다.

partial/full RELRO의 protection 범위와 binding mode를 구분한다. hardening flag가 build command에 있다고 실제 final ELF에 적용됐다는 보장은 없으므로 program header/dynamic flag를 release artifact에서 검사한다.

runtime map에서 보호 대상 page가 실제 read-only로 바뀌었는지 확인하면 linker flag와 loader 적용을 모두 검증할 수 있다. lazy binding이 남아 있으면 일부 GOT entry가 이후에도 갱신 가능해야 하므로 full RELRO와 eager binding의 관계를 함께 본다. third-party native library가 다른 hardening 수준을 갖는 경우 main executable만 검사해서는 전체 attack surface를 놓친다. release package의 모든 ELF를 일괄 검사한다.

---

## CHAPTER 21 · PIE는 main executable에도 relocation-independent addressing을 적용한다

traditional fixed-address executable은 main image ASLR을 제한할 수 있다. PIE는 executable을 shared-object와 유사하게 relocatable하게 만들어 random base address에 load할 수 있게 한다.

PIE 적용 여부는 source code만 보고 알 수 없다. compiler와 linker flag, final ELF type/relocation을 검사한다. JIT-generated code나 native plugin 같은 다른 executable region의 W^X policy도 별도 검토한다.

PIE 전환 시 absolute-address assumption을 가진 assembly, serialization, plugin code가 숨어 있으면 runtime에서만 깨질 수 있다. 여러 실행에서 main image base가 실제로 변하는지와 relocation/textrel 여부를 검사한다. performance 영향은 branch/call sequence와 GOT usage가 architecture별로 다르므로 추정하지 않고 같은 binary workload로 측정한다. crash report는 PIE base를 제거한 relative PC로 symbolization해 재현성을 유지한다.

---

## CHAPTER 22 · shared-library compatibility는 symbol 존재보다 semantics까지 포함한다

동일 symbol signature가 남아 있어도 ownership, error convention, thread-safety, struct meaning이 바뀌면 semantic ABI compatibility가 깨진다. binary는 load되지만 runtime corruption이 생겨 더 위험할 수 있다.

library versioning은 exported symbol set과 behavior contract를 함께 테스트한다. old consumer binary를 new library에 실제로 연결·실행하는 compatibility test를 유지한다. only-recompile test는 배포된 과거 consumer를 대표하지 못한다.

예를 들어 caller가 반환 buffer를 free해야 하던 API가 library-owned lifetime으로 바뀌거나, success return 뒤 errno 의미가 바뀌면 symbol과 calling convention이 같아도 기존 client가 잘못 동작한다. compatibility suite에는 ownership, nullability, thread reentrancy, callback lifetime 같은 behavioral contract를 포함한다. new library + old binary, old library + new binary 중 실제 지원하는 방향을 명시해 rolling upgrade와 rollback 가능한 조합을 자동 검증한다.

---

## CHAPTER 23 · Android native loader는 namespace와 APK packaging policy의 영향을 받는다

Android는 native library loading에 namespace와 public/private library policy를 적용한다. app이 platform private library에 우연히 의존하면 OS update에서 load failure가 생길 수 있다.

APK/AAB에 ABI별 library가 올바른 path로 포함되었는지, extract/load mode와 target SDK policy가 맞는지 확인한다. `System.loadLibrary` 실패 시 library name만 보지 않고 namespace, dependency chain, ABI, ELF class를 진단한다.

App Bundle/split 설치에서는 build artifact에 모든 ABI가 있어도 특정 device에 전달된 split에 필요한 library가 없을 수 있다. installed package의 nativeLibraryDir/split 구성과 loader namespace를 실제 device에서 확인한다. platform private soname이 이전 OS에서 우연히 보였던 경우 target/OS update에서 차단될 수 있으므로 public NDK ABI만 의존하는지 dependency scan을 release gate에 둔다. load error에는 failing dependency soname과 selected ABI를 함께 수집한다.

---

## CHAPTER 24 · JNI signature는 managed type과 native entry point 사이의 ABI adapter다

JNI는 object reference, primitive representation, exception state, thread attachment 규칙을 정의한다. native function이 managed object pointer를 raw process-lifetime pointer처럼 보관하면 GC/lifetime 규칙을 위반한다.

local/global/weak reference를 구분하고 long-running native thread는 VM attach/detach를 관리한다. pending exception 상태에서 허용되지 않은 JNI operation을 이어가면 추가 failure를 만든다. boundary wrapper에서 exception/ownership을 명시적으로 처리한다.

array/string pinning이나 critical access를 오래 유지하면 collector와 runtime scheduling에 영향을 줄 수 있으므로 native work와 reference lifetime을 짧게 제한한다. native-created thread는 attach한 VM identity와 detach cleanup을 thread lifetime에 묶는다. JNI boundary test에서는 null/large input뿐 아니라 pending Java exception, process shutdown, callback after owner destruction을 넣어 use-after-free와 leaked global reference를 찾는다. generated signature와 actual exported symbol도 ABI별로 대조한다.

---

## CHAPTER 25 · reproducible build는 source revision과 binary identity 사이의 증거를 강화한다

동일 source와 declared build input에서 bit-identical output이 가능하면 artifact provenance를 검증하기 쉽다. timestamp, nondeterministic file order, absolute build path가 output에 섞이면 동일 source의 binary hash가 달라진다.

reproducibility는 supply-chain compromise 탐지와 rollback audit에 유용하지만 compiler 자체 신뢰 문제를 완전히 해결하지 않는다. toolchain digest, dependency lock, environment를 SBOM/provenance와 함께 기록한다.

두 independent build가 다르면 section별 hash와 archive member order, debug path, timestamp metadata를 비교해 nondeterminism source를 좁힌다. reproducible artifact가 확보되면 crash build ID와 source/toolchain provenance를 강하게 연결할 수 있고, release archive가 나중에 재생성된 다른 binary로 바뀌는 것을 탐지하기 쉽다. signing은 reproducible unsigned payload identity 위에 별도 provenance layer로 관리해 key operation과 build determinism을 혼동하지 않는다.

---

## CHAPTER 26 · binary size는 code, data, symbol, relocation을 따로 최적화한다

APK/library 크기가 커진 원인을 `코드가 많다`로 끝내지 않는다. text/rodata/data, debug symbol, relocation, duplicate template instantiation, embedded resource 비중을 map/size tool로 분해한다.

on-disk compressed size, installed size, mapped RSS는 서로 다르다. size optimization 목표를 download, storage, startup I/O, memory 중 무엇인지 먼저 정한다. aggressive compression이 startup CPU를 늘릴 수도 있다.

전후 map file을 symbol/section 단위로 diff하면 어떤 library/template/resource가 증가분을 만들었는지 찾을 수 있다. dead-code elimination과 LTO로 text가 줄어도 relocation table이나 unwind metadata가 늘 수 있으므로 total만 보지 않는다. APK 압축률이 높은 debug/symbol data 제거는 download에는 효과가 작을 수 있고, read-only code consolidation은 RSS sharing에 이득이 있을 수 있다. 목표 metric별로 bytes를 분리해 regression budget을 둔다.

---

## CHAPTER 27 · LTO는 translation-unit 경계를 넘어 optimization하지만 build contract를 넓힌다

Link-Time Optimization은 object 수준 IR을 linker 단계까지 유지해 cross-module inlining, dead-code elimination 같은 optimization을 가능하게 한다. 성능과 size가 좋아질 수 있지만 link memory/time과 debug complexity가 증가한다.

LTO가 ABI 문제를 고쳐 주지는 않는다. visibility와 undefined behavior가 더 공격적으로 최적화되어 latent bug가 드러날 수 있다. release build에서만 발생하는 failure는 LTO on/off differential을 evidence로 사용하되 원인은 source invariant까지 추적한다.

thin/full LTO는 build parallelism과 cache behavior가 달라 CI resource와 reproducibility에도 영향을 줄 수 있다. cross-module inlining으로 함수 boundary가 사라지면 profile/symbol stack이 달라져 debugging artifact 품질을 다시 확인해야 한다. LTO on/off 차이로 bug가 사라진다면 optimizer bug로 단정하기 전에 undefined behavior, ODR violation, visibility assumption을 sanitizer와 IR diff로 검사한다. 성능 이득도 final workload의 startup/code-size/CPU를 함께 측정한다.

---

## CHAPTER 28 · symbol debugging은 build ID를 중심으로 artifact를 매칭한다

production crash address를 source line으로 변환하려면 exact executable/shared object와 debug symbol이 필요하다. filename/version string만으로는 동일 release name의 rebuild를 구분하기 어렵다.

build ID 또는 content hash를 crash report에 포함하고 CI artifact store에서 정확한 symbol을 찾는다. stripped binary와 debug companion의 ID 일치를 자동 검사한다. symbol mismatch는 잘못된 원인 분석을 낳는 silent corruption이다.

symbol server lookup이 실패하면 임의의 “가장 가까운 버전” symbol을 사용하지 않고 unresolved frame으로 남기는 편이 안전하다. minidump에는 loaded module의 base, size, build ID를 저장하고 symbolization 결과에도 사용한 symbol artifact ID를 기록한다. CI에서 release binary가 생성될 때 debug companion 업로드와 retention을 atomic하게 처리하면 오래된 crash도 재분석할 수 있다. rebuild가 같은 version string을 재사용하지 못하도록 artifact identity를 immutable하게 관리한다.

---

## CHAPTER 29 · linking과 loading도 startup critical path를 구성한다

shared object 수, relocation 수, symbol lookup, page fault가 application startup latency에 영향을 줄 수 있다. native-heavy application에서 code-level initialization을 줄였는데 startup이 개선되지 않으면 loader trace를 본다.

library consolidation은 relocation/search 비용을 줄일 수 있지만 incremental update와 component isolation을 약화할 수 있다. startup optimization은 binary layout, prefetch, relocation, class/runtime initialization을 하나의 timeline으로 측정한다.

loader trace에서 open/map, relocation, symbol resolution, constructor를 분리하면 shared object 수 자체보다 실제 critical cost가 무엇인지 알 수 있다. cold start에서는 file page fault가 지배하고 warm start에서는 relocation/constructor가 더 두드러질 수 있다. library를 합친 뒤 total bytes가 커져 I/O가 늘거나 relro page sharing이 달라질 수도 있으므로 file count만 줄었다고 성공으로 보지 않는다. first-frame까지 critical dependency에 있는 native library만 우선 최적화한다.

---

## CHAPTER 30 · binary 문제는 source→object→link→load→call boundary를 순서대로 검증한다

`undefined symbol`이나 native crash를 만났을 때 source를 임의 수정하지 않는다. 먼저 target architecture/ABI, object symbol/relocation, final dependency graph, loader-selected library, runtime call boundary를 확인한다.

각 단계의 artifact가 다음 단계 input과 일치한다는 증거를 남기면 binary failure를 재현 가능하게 좁힐 수 있다. native system의 핵심은 기계어를 읽는 기술 자체가 아니라 **binary contract가 어느 경계에서 깨졌는지 식별하는 능력**이다.

실전 evidence chain에는 compiler command/toolchain digest, object build ID, link map, final ELF dynamic tags, loader-selected module build ID, crash register/stack을 순서대로 묶는다. object에는 symbol이 있는데 final binary에서 사라졌다면 link 단계, final binary에는 있는데 runtime에서 다른 library가 선택됐다면 load 단계, 올바른 target까지 도달했는데 stack/layout이 깨졌다면 call ABI 단계로 좁혀진다. 수정 후에는 동일 artifact chain과 과거 consumer binary까지 재검증해 boundary가 실제로 복구됐는지 확인한다.