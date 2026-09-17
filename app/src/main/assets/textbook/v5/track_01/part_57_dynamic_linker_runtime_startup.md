# PART 57 · Dynamic Linker Runtime Startup — dependency graph, symbol scope, relocation, TLS, constructors

Executable이 disk에 존재한다고 process image가 완성되는 것은 아니다. Dynamic executable은 **interpreter 지정, shared-object dependency discovery, memory mapping, relocation, symbol resolution, TLS setup, constructor ordering**을 거쳐야 entry point 이후의 program state가 유효해진다. 이 단계의 비용과 실패는 application main보다 앞에서 발생한다.

## CHAPTER 01 · ELF interpreter는 kernel과 userspace loader의 경계를 만든다

Dynamic ELF executable은 program interpreter를 지정할 수 있다. Kernel은 executable segment를 mapping하는 동시에 interpreter를 load하고 control을 넘겨 userspace dynamic loader가 나머지 shared objects와 relocations를 처리하게 한다. 따라서 `execve 성공`과 `main 진입 성공` 사이에는 별도 userspace runtime bootstrap이 존재한다.

## CHAPTER 02 · Dynamic section은 runtime loader가 읽는 dependency metadata다

`DT_NEEDED`, relocation table, string/symbol table pointer, init/fini array 같은 dynamic entries는 loader가 runtime image를 구성하는 입력이다. 이 metadata가 손상되면 machine code 자체가 멀쩡해도 startup이 실패할 수 있다. Loader는 offset·size·address 관계를 신뢰하기 전에 mapped object bounds와 consistency를 검증해야 한다.

## CHAPTER 03 · DT_NEEDED는 dependency graph의 edge다

하나의 object가 여러 shared library를 필요로 하고 각 library가 다시 다른 dependency를 필요로 하면 loader는 graph를 구성한다. 단순 tree가 아니라 동일 library 공유와 cycle 가능성을 고려해야 한다. Load order는 symbol lookup scope와 constructor order에도 영향을 주므로 dependency graph는 단순 파일 목록이 아니다.

## CHAPTER 04 · Library search path는 correctness와 security boundary다

필요한 SONAME을 어떤 파일로 resolve하는지는 embedded runpath, loader configuration, environment, default directories 같은 정책에 좌우된다. 예상과 다른 library가 먼저 선택되면 ABI mismatch나 symbol interposition이 발생한다. Privileged execution에서는 environment-controlled path를 제한하는 이유가 바로 loader search가 code selection boundary이기 때문이다.

## CHAPTER 05 · SONAME은 file name과 ABI identity 사이의 계약이다

Shared object의 실제 파일 이름과 dependency가 기록하는 logical SONAME은 다를 수 있다. Packaging은 compatible ABI family를 같은 SONAME으로 유지하고 incompatible ABI를 별도 identity로 분리할 수 있다. File 교체가 성공했다고 runtime compatibility가 보장되는 것이 아니며 symbol version과 data layout까지 함께 봐야 한다.

## CHAPTER 06 · Mapping은 segment permission과 relocation 가능성을 동시에 고려한다

Loader는 PT_LOAD segment를 file offset·virtual-address alignment에 맞춰 mapping하고 text/data permission을 설정한다. 일부 relocation은 writable mapping이 필요한 반면 최종 runtime에는 read-only로 잠가야 하는 영역이 있다. W^X와 RELRO는 startup 중 mutation과 steady-state protection을 단계적으로 분리하는 이유다.

## CHAPTER 07 · Relocation은 symbolic reference를 concrete runtime address에 결합한다

Object file이 build될 때 확정할 수 없던 address reference는 runtime load address와 symbol resolution 결과를 사용해 patch된다. Position-independent code도 모든 reference가 relocation-free인 것은 아니다. Global data, GOT entry, TLS, dynamic symbol reference는 architecture별 relocation type과 addend semantics를 따른다.

## CHAPTER 08 · Relative relocation은 symbol lookup 없이 base address로 계산할 수 있다

많은 position-dependent pointer는 load base+addend 형태로 해결할 수 있어 full symbol lookup보다 싸다. Startup optimization은 relocation 개수뿐 아니라 **어떤 relocation이 symbol hash lookup을 요구하는가**를 본다. Large binary에서 relocation table 구조와 prelinkable relative entries는 cold-start latency에 직접 영향을 준다.

## CHAPTER 09 · Symbol resolution은 global dictionary lookup이 아니다

Dynamic linker는 object load order, global/local visibility, symbol binding, version, namespace에 따라 lookup scope를 구성한다. 동일 이름 symbol이 여러 object에 있어도 `첫 번째 이름 일치` 이상의 규칙이 적용된다. Debugging 시 undefined symbol과 wrong-symbol binding을 구분하려면 lookup scope 자체를 재현해야 한다.

## CHAPTER 10 · Strong/weak binding은 fallback semantics를 만든다

Weak symbol은 definition이 없거나 strong definition과 함께 있을 때 다른 resolution behavior를 가질 수 있다. Weak reference를 optional capability처럼 쓰는 경우 build/link environment 변화가 runtime behavior를 바꿀 수 있다. `symbol exists`만 확인하지 말고 binding class와 chosen definition을 추적해야 한다.

## CHAPTER 11 · Symbol visibility는 export surface와 optimizer freedom을 바꾼다

Hidden/internal symbol은 다른 shared object에서 interpose되지 않는다는 강한 가정을 compiler/linker에 줄 수 있다. 이는 direct call, relocation reduction, devirtualization과 startup cost에 영향을 준다. 반대로 모든 symbol을 default visibility로 export하면 ABI surface와 interposition risk가 커진다.

## CHAPTER 12 · Symbol versioning은 이름 하나에 여러 ABI 세대를 공존시킨다

같은 symbol name이라도 version identifier로 ABI generation을 구분할 수 있다. Consumer가 특정 version을 요구하면 loader는 compatible definition을 선택하거나 startup을 실패시킨다. Symbol versioning은 semantic compatibility를 자동 보장하지 않으며 parameter layout과 side effect contract는 별도로 유지돼야 한다.

## CHAPTER 13 · PLT/GOT는 dynamic call indirection의 구현 요소다

Position-independent caller는 GOT/PLT 같은 table을 통해 runtime-resolved address로 branch할 수 있다. 이 indirection은 code text를 공유 가능하게 유지하면서 symbol address를 writable data table에 둘 수 있게 한다. Modern toolchain은 direct binding이나 alternate relocation을 사용해 비용을 줄일 수 있으므로 disassembly에서 항상 전통적 PLT shape를 가정하면 안 된다.

## CHAPTER 14 · Lazy binding은 startup 비용과 first-call latency를 교환한다

일부 loader는 function symbol resolution을 첫 호출까지 미룰 수 있다. Startup relocation work는 줄지만 첫 호출에 resolver path와 GOT update가 들어간다. Latency-sensitive function의 첫 invocation이 request critical path에 있으면 평균 startup 최적화가 p99 service latency를 악화시킬 수 있다.

## CHAPTER 15 · Eager binding은 failure를 앞당기고 runtime surprise를 줄인다

모든 required symbol을 startup에 resolve하면 missing symbol을 main 이전에 발견하고 steady-state first-call cost를 없앨 수 있다. 대신 사용하지 않는 library path까지 resolution 비용을 지불한다. Binding policy는 binary size, symbol count, cold-start SLO, fail-fast requirement를 함께 고려해야 한다.

## CHAPTER 16 · IFUNC와 resolver는 runtime CPU 특성에 따라 implementation을 선택할 수 있다

Indirect function resolver는 process startup 또는 symbol binding 시점에 hardware capability에 맞는 implementation address를 선택하는 데 사용될 수 있다. 잘못된 feature detection이나 resolver가 unsafe dependency를 사용하면 startup 자체가 깨진다. Resolver는 일반 application initialization보다 훨씬 제한된 runtime context에서 실행될 수 있다.

## CHAPTER 17 · Constructor는 main 이전에 실행되므로 hidden startup work가 된다

Shared object와 executable의 initialization arrays에 등록된 constructor는 dependency order와 loader policy에 따라 main 전 실행될 수 있다. Constructor가 file I/O, thread creation, heavy allocation을 수행하면 application code가 시작되기 전에 latency와 failure surface를 만든다. Cold-start profiling은 main timestamp보다 loader/constructor timeline을 앞에서부터 봐야 한다.

## CHAPTER 18 · Constructor ordering에 의존하면 cross-object initialization bug가 생긴다

한 translation unit이나 shared object의 global initialization이 다른 object의 global state가 이미 준비됐다고 가정하면 order 변화에 취약하다. Dependency edge가 initialization semantic dependency와 일치한다는 보장은 없다. Explicit initialization API나 function-local lazy state가 initialization-order coupling을 줄일 수 있다.

## CHAPTER 19 · Destructor/finalizer ordering도 process shutdown semantics다

`atexit`, fini array, shared-object unload destructor가 resource dependency와 반대 순서로 실행되지 않으면 shutdown use-after-free가 생길 수 있다. Abrupt process termination에서는 destructor가 전혀 실행되지 않을 수 있으므로 durability나 external cleanup correctness를 destructor에 의존해서는 안 된다.

## CHAPTER 20 · dlopen은 running process의 dependency graph를 동적으로 변경한다

Runtime에 shared object를 load하면 new mappings, relocations, symbol scope, TLS allocation, constructors가 실행될 수 있다. 이미 실행 중인 thread와 동시에 loader state를 수정해야 하므로 loader internal lock과 reentrancy 문제가 생긴다. Plugin architecture는 code extension 기능인 동시에 runtime linker concurrency surface다.

## CHAPTER 21 · dlclose는 library code가 즉시 안전하게 사라진다는 뜻이 아니다

다른 thread가 library function을 실행 중이거나 callback/function pointer를 보유하면 unmap 시 control-flow dangling reference가 생길 수 있다. TLS destructor나 object vtable도 library code address를 가리킬 수 있다. Safe unload는 reference/lifetime protocol 없이는 보장하기 어렵기 때문에 실제 시스템은 unload를 제한하거나 process lifetime까지 mapping을 유지하기도 한다.

## CHAPTER 22 · TLS relocation은 per-thread address를 runtime layout에 연결한다

Thread-local variable address는 process-global fixed address가 아니다. Loader는 module별 TLS block 배치와 thread pointer 기준 offset을 구성하고 relocation model에 맞춰 access sequence를 해결한다. dlopen으로 TLS module이 추가되면 기존 thread에도 dynamic TLS state가 필요할 수 있다.

## CHAPTER 23 · Static TLS budget은 plugin 확장성과 충돌할 수 있다

초기 load된 module의 fast TLS model은 thread creation 시 예약된 static area를 사용할 수 있다. Runtime에 많은 TLS-heavy modules가 추가되면 static allocation 가정이 깨지고 dynamic access path가 필요해질 수 있다. TLS performance를 평가할 때 variable access cost뿐 아니라 module lifecycle과 per-thread footprint를 함께 봐야 한다.

## CHAPTER 24 · Loader lock은 reentrant initialization에서 deadlock source가 된다

Dynamic linker가 global loader state를 보호하는 동안 constructor가 다시 `dlopen`, thread join, callback을 수행하면 lock-order cycle이 생길 수 있다. Library initialization code는 application-level lock graph와 loader-internal lock graph를 동시에 건드릴 수 있으므로 startup deadlock은 source mutex만 보고 찾기 어렵다.

## CHAPTER 25 · Symbol interposition은 디버깅 기능과 optimization barrier를 동시에 만든다

Preload/interposition은 allocator, I/O function 등을 감싸 instrumentation에 활용할 수 있지만 compiler가 call target이 고정이라는 가정을 못 하게 만들 수 있다. Interposed function이 original semantics·errno·thread-safety를 완전히 보존하지 않으면 관측 도구가 application behavior를 바꾼다.

## CHAPTER 26 · Loader namespace는 dependency 세계를 격리할 수 있다

일부 runtime은 별도 namespace나 classloader-like scope를 사용해 서로 다른 library version을 같은 process에서 분리할 수 있다. 격리는 symbol collision을 줄이지만 object를 namespace 경계로 넘길 때 ABI identity와 allocator ownership 문제가 생긴다. `같은 type name`이 같은 binary contract를 뜻하지 않는다.

## CHAPTER 27 · Startup relocation volume은 binary architecture의 결과다

Global object, exported symbol, template/code duplication, shared-library boundary가 많아지면 relocation과 symbol lookup 작업이 증가할 수 있다. Startup 최적화는 loader flag 하나보다 **visibility 축소, dead stripping, dependency graph 단순화, constructor 제거**가 더 큰 효과를 낼 수 있다.

## CHAPTER 28 · Loader diagnostics는 chosen path와 chosen symbol을 증거로 남겨야 한다

`library not found`만 기록하면 search path 어느 단계에서 실패했는지 알기 어렵다. 어떤 SONAME을 누가 요구했고 어떤 directory 후보를 검사했으며 어떤 symbol version이 필요했고 어떤 object가 선택됐는지 기록해야 reproducible diagnosis가 된다. Production에는 민감한 filesystem path 노출을 제한하는 정책도 필요하다.

## CHAPTER 29 · Wrong-library 문제는 성공적으로 load되기 때문에 더 위험하다

호환되지 않는 library가 같은 SONAME/symbol을 제공하면 startup은 통과하고 나중에 subtle data corruption이나 semantic mismatch로 실패할 수 있다. Build ID, ABI fingerprint, symbol-version requirement, package provenance를 검증하면 `찾았다`와 `올바른 binary를 찾았다`를 구분할 수 있다.

## CHAPTER 30 · Dynamic linking은 runtime code identity를 결정하는 공급망 경계다

최종 process가 실행하는 code는 executable 하나가 아니라 loader search policy로 선택된 shared objects와 runtime-injected modules의 합이다. 따라서 **dependency identity, load path, symbol version, relocation integrity, constructor behavior, unload lifetime**을 release evidence로 남겨야 한다. Runtime image가 무엇이었는지 증명할 수 없으면 같은 executable hash만으로 production behavior를 재현할 수 없다.
