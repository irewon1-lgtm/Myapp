# PART 57 · Dynamic Linker Runtime Startup — dependencies, symbols, relocations, TLS

동적 링커는 실행 파일의 `main()`보다 먼저 움직인다. ELF interpreter를 통해 시작해 shared object dependency graph를 확장하고, segment를 mapping하고, symbol scope를 구성하며, relocation과 TLS를 해결하고, constructor를 호출한 뒤에야 application으로 control을 넘긴다. `dlopen` 이후에는 같은 작업이 runtime 중 다시 일어난다. 이 PART는 **disk의 ELF image가 실제 process address space와 callable symbol graph로 바뀌는 과정**을 추적한다.

---

## CHAPTER 01 · ELF interpreter가 kernel exec와 userspace dynamic loader 사이의 handoff를 만든다

동적 실행 파일에는 실행에 필요한 program interpreter가 지정될 수 있다. Kernel은 executable segment만 mapping하고 끝나는 것이 아니라 interpreter도 mapping한 뒤 entry를 loader 쪽으로 넘긴다. Loader는 kernel이 전달한 auxiliary vector, executable metadata, environment를 이용해 나머지 runtime image를 구성한다. 따라서 `main()` 진입 전에도 이미 복잡한 userspace code가 실행된다.

Startup crash를 application 초기화 문제로 단정하면 안 된다. 잘못된 interpreter path, architecture 불일치, loader가 읽지 못하는 dynamic metadata는 user code 한 줄 실행 전에 process를 종료시킬 수 있다. `execve` 성공 여부와 application entry 도달 여부는 다른 단계다.

진단에서는 executable의 interpreter, ELF class/machine, loader message를 먼저 확인한다. Container/chroot에서 host와 다른 loader path를 갖는 경우도 있어 file 존재와 ABI compatibility를 같이 본다.

## CHAPTER 02 · dynamic section은 runtime loader에게 필요한 dependency·relocation·symbol table 위치를 알려준다

ELF의 dynamic section에는 필요한 shared object 이름, string/symbol table, relocation table, init/fini entry 등 runtime linking에 필요한 pointer와 tag가 들어간다. Loader는 이 metadata를 신뢰해 address 계산을 수행하므로 malformed size와 pointer를 bounded하게 검증해야 한다. 정상 compiler output만 들어온다는 가정은 분석 tool이나 untrusted plugin 환경에서 위험하다.

각 pointer는 file virtual address인지 runtime relocated address인지 해석 규칙이 다르다. PIE와 shared object는 load bias가 적용되므로 raw ELF 값과 process address를 혼동하면 잘못된 patch가 발생한다.

Loader diagnostics를 읽을 때 `dynamic section missing` 같은 오류는 symbol 이름 문제가 아니라 훨씬 앞단 image 구조 문제다. 단계별로 원인을 좁힌다.

## CHAPTER 03 · DT_NEEDED graph는 단순 목록이 아니라 transitive dependency와 load order를 만든다

Executable이 직접 참조하는 shared object만 load되는 것이 아니다. 각 library의 dependency가 다시 확장되어 전체 graph를 이룬다. 같은 SONAME이 여러 경로에서 나타날 때 이미 load된 object 재사용, namespace, symbol scope 규칙이 실제 graph를 결정한다. 따라서 `ldd` 한 줄의 순서만으로 모든 lookup semantics를 설명할 수 없다.

Dependency cycle이 있어도 object identity와 visitation 상태로 무한 recursion을 피해야 한다. Missing indirect dependency는 application이 직접 그 library symbol을 호출하지 않아도 startup을 실패시킬 수 있다.

배포 artifact에는 직접 dependency뿐 아니라 transitive graph 변화도 추적한다. 작은 package update가 전혀 예상하지 못한 library version을 runtime image에 넣을 수 있기 때문이다.

## CHAPTER 04 · library search path는 correctness와 supply-chain security를 동시에 결정한다

Loader가 필요한 SONAME을 찾을 때 embedded RUNPATH/RPATH, environment, system cache/default path 등 여러 source를 사용할 수 있다. 정확한 우선순위와 secure-execution mode의 예외는 platform 규칙을 따라야 한다. 검색 경로에 writable directory가 앞서면 공격자가 동일 이름의 library를 주입할 수 있다.

Development 환경의 `LD_LIBRARY_PATH`가 production에서도 암묵적으로 필요하면 배포 재현성이 떨어진다. Executable이 어느 library를 선택했는지 path와 build identity를 기록해야 “내 컴퓨터에서는 된다” 문제를 줄일 수 있다.

보안상 privileged process는 일부 environment override를 무시할 수 있다. 따라서 환경변수로 강제한 test와 실제 privileged startup 결과가 다를 수 있음을 확인한다.

## CHAPTER 05 · SONAME은 파일 이름과 별개로 ABI 세대를 가리키는 runtime identity다

Shared library 파일이 `libx.so.3.7`처럼 세부 버전을 포함해도 dependent object는 보통 SONAME `libx.so.3`을 요구한다. 이 이름은 호환 가능한 ABI major를 나타내는 계약 역할을 한다. Symlink만 새 파일로 바꿔도 ABI가 깨지면 loader는 성공하고 application이 더 늦게 오작동할 수 있다.

SONAME 변경은 dependency graph 전체에 영향을 주므로 major ABI 변경과 함께 관리한다. 반대로 ABI를 깨뜨렸는데 SONAME을 유지하면 old binary가 새 library를 잘못 load할 수 있다.

Package validation에서 실제 SONAME, exported symbols, ABI checker를 함께 본다. File version string만으로 compatibility를 판정하지 않는다.

## CHAPTER 06 · segment mapping은 ELF permission과 page alignment를 process virtual memory로 구체화한다

Loader는 PT_LOAD segment를 적절한 virtual address에 mapping하고 file-backed 영역과 zero-filled BSS를 구성한다. Code는 executable/read-only, data는 writable처럼 permission이 달라야 하며 W^X, RELRO 같은 hardening도 이후 단계와 연결된다. File offset과 virtual address alignment 조건이 맞지 않으면 mapping 자체가 실패할 수 있다.

PIE와 ASLR에서는 object마다 load bias가 생긴다. Relocation은 이 runtime base를 포함해 계산된다. Debugger나 symbolizer도 같은 mapping 정보를 사용하므로 runtime map snapshot이 중요하다.

Executable writable mapping이 예상보다 많다면 linker flags나 relocation policy를 점검한다. Loader startup은 security memory layout을 결정하는 지점이기도 하다.

## CHAPTER 07 · relocation은 link time에 확정하지 못한 주소를 runtime 배치에 맞춰 patch한다

Shared object는 실제 load address를 미리 알 수 없으므로 code/data 안의 일부 address 계산을 relocation entry로 남긴다. Loader는 relocation type, symbol value, addend, load bias를 이용해 지정 위치를 수정한다. Type마다 overflow와 signedness 규칙이 달라 generic integer addition으로 처리할 수 없다.

Relocation target이 read-only로 보호되기 전 잠깐 writable해야 할 수도 있다. RELRO는 relocation 완료 후 GOT 일부를 read-only로 전환해 공격 surface를 줄인다.

Startup cost 분석에서는 relocation count와 symbol lookup cost를 분리한다. 수만 개 relocation은 cold start와 page dirtying을 크게 늘릴 수 있다.

## CHAPTER 08 · relative relocation은 external symbol lookup 없이 load bias만으로 해결할 수 있어 빠르다

많은 pointer는 같은 object 내부 address를 가리키므로 symbol table 검색 없이 `base + addend` 형태로 patch할 수 있다. Relative relocation은 이런 경우를 빠르게 처리해 startup cost를 줄인다. Modern linker가 relative relocation packing 같은 최적화를 사용하는 이유도 수가 매우 많기 때문이다.

하지만 relocation stream을 압축하거나 묶는 format은 decoder robustness가 중요하다. 잘못된 count나 delta가 mapping 밖 target을 가리키지 않게 검증해야 한다.

Binary size 최적화가 startup speed와 같은 방향일 수도 있지만 항상 그런 것은 아니다. Relocation representation을 바꾼 뒤 actual loader CPU와 dirty page를 측정한다.

## CHAPTER 09 · symbol scope는 동일 이름이 여러 object에 있을 때 어느 정의를 선택할지 결정한다

Dynamic symbol lookup은 단순 hash table 검색이 아니라 현재 namespace와 load order가 만든 scope list를 따라간다. Main executable, dependencies, `dlopen` flags에 따라 같은 이름의 symbol이 다른 object에서 선택될 수 있다. 이 규칙이 symbol interposition의 기반이다.

Library가 내부 helper까지 default visibility로 export하면 외부 object의 같은 이름에 의해 의도치 않게 interpose될 수 있다. Hidden visibility를 사용하면 내부 binding을 더 안정적으로 만들 수 있다.

Wrong-symbol incident에서는 symbol 이름만 확인하지 말고 lookup scope와 실제 resolved object path를 수집한다. 예상 library가 load돼 있어도 다른 정의가 먼저 선택될 수 있다.

## CHAPTER 10 · weak binding은 symbol 부재를 허용하거나 strong 정의에 양보하는 선택 규칙을 만든다

Weak symbol은 strong definition이 존재하면 그것에 밀리고, 특정 상황에서는 정의가 없어도 link/load가 계속될 수 있다. Optional hook이나 default implementation에 쓰이지만 runtime behavior가 deployment에 따라 달라질 수 있다. 동일 binary가 plugin 유무에 따라 다른 function을 호출하는 구조가 된다.

Weak undefined symbol을 null/zero semantics로 처리하는 architecture와 relocation 종류를 정확히 이해해야 한다. 무조건 callable address라고 가정하면 crash가 난다.

Compatibility 테스트에서는 optional provider가 있는 경우와 없는 경우를 모두 실행한다. Weak binding을 숨은 feature flag처럼 사용하지 않는다.

## CHAPTER 11 · symbol visibility는 export surface와 optimization 가능성을 동시에 바꾼다

Default visibility symbol은 다른 object에서 참조하거나 interpose할 수 있어 compiler/linker가 binding을 고정하기 어렵다. Hidden/internal visibility는 object 밖에서 보이지 않게 해 direct call과 relocation 감소를 가능하게 할 수 있다. Export surface를 최소화하면 ABI 관리와 보안 측면도 좋아진다.

하지만 외부 plugin이 실제로 필요한 symbol을 hidden으로 바꾸면 runtime load가 실패한다. Public ABI 목록을 명시적으로 관리해야 한다.

Build에서 exported symbol diff를 artifact로 남긴다. Source header만으로 실제 ELF export가 예상과 일치하는지 보장되지 않는다.

## CHAPTER 12 · symbol versioning은 동일 symbol 이름 아래 여러 ABI generation을 공존시킨다

Library는 symbol version 정보를 사용해 old binary가 과거 ABI 구현을 요구하고 new binary가 최신 구현을 사용하도록 할 수 있다. Loader는 이름뿐 아니라 required version을 맞춰 definition을 선택한다. 버전이 없거나 맞지 않으면 library 자체는 존재해도 startup symbol error가 발생한다.

Default version 설정이 잘못되면 새 link가 오래된 implementation을 참조할 수 있다. Version script와 exported ABI를 source control로 관리한다.

Incident에서는 `undefined symbol` 문자열 뒤의 version suffix까지 확인한다. 같은 symbol 이름이 보인다는 사실만으로 호환된다고 판단하지 않는다.

## CHAPTER 13 · PLT와 GOT는 position-independent code가 external function address를 간접 참조하게 한다

Procedure Linkage Table과 Global Offset Table은 code text를 매 process마다 직접 수정하지 않고 external function/data address를 indirection으로 해결하는 전형적인 구조다. Call site는 PLT entry를 거쳐 GOT에 저장된 resolved address로 이동할 수 있다. 이 layout은 lazy binding과 RELRO hardening에 연결된다.

GOT가 writable한 기간은 공격자가 control-flow target을 바꾸는 surface가 될 수 있어 full RELRO와 eager binding 정책이 사용되기도 한다.

프로파일에서 PLT trampoline frame이 보일 수 있으므로 symbolizer가 실제 target과 구분해야 한다. 작은 indirect call overhead보다 startup/security trade-off를 함께 본다.

## CHAPTER 14 · lazy binding은 첫 호출까지 symbol resolution 비용을 미룬다

Lazy binding에서는 external function의 GOT entry가 처음에는 resolver trampoline을 가리키고, 최초 호출 때 loader가 symbol을 찾은 뒤 실제 address를 patch한다. Startup은 빨라질 수 있지만 첫 request 중 resolution latency가 나타나고 loader lock contention이 생길 수 있다.

사용되지 않는 function은 끝까지 resolve하지 않아 비용을 아낀다. 반대로 latency-sensitive service에서는 warmup되지 않은 rare path가 갑자기 loader work를 수행할 수 있다.

첫-call latency와 startup time을 분리 측정한다. Lazy 여부가 security hardening과 충돌하는지도 확인한다.

## CHAPTER 15 · eager binding은 startup에서 relocation을 끝내 runtime 중 surprise를 줄인다

Eager binding은 필요한 function symbol을 process 시작 또는 `dlopen` 시 모두 resolve한다. Startup CPU는 증가하지만 missing symbol을 일찍 발견하고, runtime first-call jitter를 줄이며 GOT를 read-only로 보호하기 쉬워진다.

대규모 application에서는 사용하지 않을 plugin symbol까지 resolve해 불필요한 비용이 생길 수 있다. 따라서 startup SLA와 runtime latency 목표를 비교해야 한다.

`BIND_NOW`류 설정 변경 전후에 relocation count, startup wall time, first-request p99를 같이 측정한다. 보안 설정과 성능을 한 지표로 단순화하지 않는다.

## CHAPTER 16 · IFUNC은 runtime CPU 특성에 따라 symbol implementation을 선택한다

Indirect function mechanism은 loader가 resolver를 실행해 CPU feature나 platform 상태에 맞는 implementation 주소를 선택하게 할 수 있다. 같은 binary가 AVX 지원 여부 등에 따라 다른 code path를 사용한다. 이 resolver는 매우 이른 startup 단계에서 실행될 수 있어 사용할 수 있는 runtime 서비스가 제한적일 수 있다.

Resolver에서 allocation, loader reentry 같은 복잡한 작업을 하면 deadlock이나 초기화 순서 문제가 생길 수 있다. Deterministic하고 작은 함수로 유지하는 편이 안전하다.

성능 incident에서 동일 binary인데 machine별 결과가 다르면 실제 resolved implementation을 확인한다. CPU flag만 추측하지 않는다.

## CHAPTER 17 · constructors는 main 이전에 global state와 dependency를 초기화한다

Shared object와 executable은 constructor 목록을 통해 application entry 전에 code를 실행할 수 있다. Logging, allocator, thread 생성까지 constructor에서 수행하면 startup dependency가 숨겨지고 실패 지점이 복잡해진다. Constructor 순서에 암묵적으로 의존하는 global object는 fragile하다.

가능하면 명시적 application initialization으로 옮기고 constructor는 local invariant만 준비한다. 외부 I/O나 long-running 작업을 피하면 loader lock과 startup tail을 줄일 수 있다.

Startup trace에서 constructor별 duration을 측정하면 `main` 이전 시간이 어디에 쓰이는지 알 수 있다. 전체 cold start만 보면 원인을 놓친다.

## CHAPTER 18 · initialization order는 dependency graph와 language 규칙이 섞여 예상보다 복잡하다

Loader는 dependency가 준비된 뒤 dependent constructor를 실행해야 하지만 동일 level의 object와 language static initialization 순서는 제한적 보장만 가질 수 있다. 서로 다른 translation unit의 global constructor가 상대 순서를 가정하면 build/link 순서 변화로 bug가 나타날 수 있다.

Constructor A가 B의 아직 초기화되지 않은 global을 읽는 문제는 테스트 순서에 따라 드물게 보일 수 있다. Lazy function-local initialization이나 explicit dependency injection이 더 안전하다.

Linker upgrade나 LTO를 적용한 뒤 startup regression test를 돌린다. Binary layout 변화가 init order의 숨은 가정을 드러낼 수 있다.

## CHAPTER 19 · destructors는 process 종료와 dlclose에서 실행될 수 있지만 모든 종료 경로를 보장하지 않는다

Normal exit에서는 fini/destructor가 호출될 수 있지만 `_exit`, fatal signal, SIGKILL에서는 실행되지 않는다. Persistent correctness나 critical flush를 destructor에만 의존하면 안 된다. `dlclose` 시 module destructor는 다른 thread가 module code를 사용 중인지와도 충돌할 수 있다.

Destructor ordering은 constructor의 단순 역순처럼 보일 수 있지만 dependency와 atexit registration이 섞여 복잡하다. 다른 library global을 참조하는 cleanup은 특히 취약하다.

정상 shutdown test와 abrupt crash test를 분리한다. Destructor가 실행된다는 사실을 durability guarantee로 확대하지 않는다.

## CHAPTER 20 · dlopen은 runtime 중 dependency graph와 symbol scope를 확장한다

Plugin load는 shared object file mapping, dependency resolution, relocation, TLS registration, constructor 실행을 현재 multi-thread process 안에서 수행한다. Startup loader보다 concurrency가 훨씬 복잡한 이유다. `RTLD_LOCAL/GLOBAL` 같은 flag가 이후 symbol lookup scope를 바꾼다.

Untrusted plugin path는 code execution boundary이므로 signature/allowlist와 directory permission을 관리한다. 단순 “파일 열기” API가 아니다.

Load 실패는 partial dependency mapping과 constructor side effect를 남기지 않도록 library contract를 확인한다. Application은 error string뿐 아니라 requested path와 namespace를 기록한다.

## CHAPTER 21 · dlclose는 reference count를 줄이지만 즉시 안전한 code unload를 의미하지 않을 수 있다

다른 object가 reference를 유지하거나 runtime policy가 unload를 지연하면 `dlclose` 후 mapping이 남을 수 있다. 반대로 실제 unmap이 되면 다른 thread의 function pointer, callback, TLS destructor가 stale code address를 가리킬 위험이 있다.

Plugin API는 outstanding object와 callback이 모두 종료된 후 unload하도록 lifetime protocol을 둔다. Raw function pointer를 장기 cache하지 않는다.

Stress test에서 load→use→concurrent callback→close를 반복해 race를 찾는다. Single-thread load/unload 성공만으로 안전하다고 판단하지 않는다.

## CHAPTER 22 · TLS relocation은 module별 thread-local object 주소를 runtime TLS layout과 연결한다

Shared library의 TLS symbol은 일반 data symbol과 다른 relocation/model을 사용한다. Loader는 module ID와 offset을 등록하고 이미 존재하는 thread와 이후 생성되는 thread가 해당 storage에 접근할 수 있게 한다. Dynamic load가 많으면 static TLS 여유와 dynamic TLS 경로가 중요해진다.

잘못된 TLS model을 사용한 binary는 `dlopen` 환경에서 load failure나 address corruption을 만들 수 있다. Compiler flags와 link model이 deployment 방식과 맞는지 확인한다.

TLS access 문제는 P47의 thread-local semantics와 loader module lifecycle을 함께 봐야 한다. 단순 symbol resolution 문제가 아니다.

## CHAPTER 23 · static TLS capacity는 빠른 access와 late-loaded module 유연성 사이의 제한된 자원이다

Initial executable과 early library의 TLS는 thread pointer 기준 고정 offset으로 접근할 수 있는 static block에 배치된다. Loader는 late-loaded module을 위해 일부 surplus를 둘 수 있지만 무한하지 않다. 많은 plugin이 특정 TLS model을 요구하면 static TLS allocation 실패가 나타날 수 있다.

이 failure는 thread 수나 heap 여유와 무관하게 발생할 수 있어 진단이 어렵다. Loader error와 module TLS size를 확인한다.

Plugin design에서는 큰 thread-local buffer를 피하고, 필요하면 heap-backed per-thread state로 분리한다. ABI-level static TLS를 scarce resource로 본다.

## CHAPTER 24 · loader lock은 graph와 symbol table mutation을 직렬화하는 전역 병목이 될 수 있다

`dlopen`, lazy binding, constructor 실행 중 loader 내부 shared state를 보호하기 위해 lock이 사용될 수 있다. Constructor가 다시 loader operation을 호출하거나 다른 lock을 잡은 상태에서 `dlopen`하면 lock-order deadlock이 생길 수 있다. Multi-thread startup/runtime에서 rare hang의 원인이 된다.

Lazy binding이 request 처리 중 처음 발생하면 많은 thread가 같은 loader lock에 몰릴 수도 있다. Eager warmup이 tail을 줄이는 경우가 있다.

Hang dump에서 모든 thread가 loader frame 주변에 blocked되어 있는지 확인한다. Application mutex만 추적하면 root lock을 놓칠 수 있다.

## CHAPTER 25 · symbol interposition은 강력한 override mechanism이지만 내부 최적화와 의미를 불안정하게 만든다

Preload library나 earlier scope의 symbol이 같은 이름을 제공하면 원래 library 호출이 다른 implementation으로 연결될 수 있다. Debugging/profiling wrapper에는 유용하지만 security와 correctness에 큰 영향이 있다.

Compiler가 symbol이 interposable하다고 가정하면 direct call/inlining을 제한할 수 있다. Hidden visibility나 `-Bsymbolic`류 선택은 동작과 최적화 가능성을 바꾼다.

Production에서 preload/override 환경을 inventory로 남긴다. 예상과 다른 allocator나 libc wrapper가 들어가면 performance와 crash signature가 달라질 수 있다.

## CHAPTER 26 · loader namespace는 같은 process 안에 서로 다른 dependency universe를 격리할 수 있다

일부 loader는 별도 namespace에 object graph를 load해 동일 SONAME의 다른 version을 공존시키거나 symbol scope를 분리할 수 있다. Plugin conflict 완화에 유용하지만 object identity와 type/allocator crossing이 더 복잡해진다.

Namespace A의 library object를 B의 함수가 해제하는 식으로 ABI boundary를 넘으면 서로 다른 runtime instance가 충돌할 수 있다. Shared singleton도 namespace마다 복제될 수 있다.

Isolation을 쓴다면 cross-namespace API를 단순 C ABI와 explicit ownership으로 제한한다. “같은 이름 library”가 같은 global state라고 가정하지 않는다.

## CHAPTER 27 · relocation volume은 startup CPU와 dirty private pages를 동시에 늘린다

각 relocation은 loader가 metadata를 읽고 target memory를 patch해야 하며, read-only share 가능했던 page를 private dirty로 만들 수도 있다. 많은 global pointer와 exported symbol은 binary size 이상의 runtime memory cost를 낳는다.

PIC-friendly code, relative relocation, hidden visibility, section layout으로 count와 dirty footprint를 줄일 수 있다. 하지만 source readability를 희생하기 전에 profile로 실제 startup hotspot인지 확인한다.

CI에서 relocation count와 `.data.rel.ro` 크기를 추적하면 큰 regression을 조기에 잡을 수 있다. Cold-start metric과 상관관계를 본다.

## CHAPTER 28 · loader diagnostics는 실제 search·resolution 결정을 증거로 남겨야 한다

“library not found”를 해결하려고 임의 symlink를 만들기 전에 loader가 어떤 path를 검색했고 어느 dependency에서 요구됐는지 확인한다. Debug environment option, trace, process maps를 사용하면 실제 선택 경로를 볼 수 있다.

Diagnostics 자체가 startup timing을 바꾸고 민감한 environment/path를 노출할 수 있어 production 상시 활성화는 주의한다. 필요한 범위만 capture한다.

Incident artifact에는 executable build ID, loaded object path, SONAME/version을 함께 남긴다. 파일 이름 하나만으로는 wrong-library 문제를 재현하기 어렵다.

## CHAPTER 29 · wrong-library 문제는 ‘파일이 있다’보다 ‘예상 ABI의 정확한 object가 선택됐다’가 핵심이다

System에 같은 SONAME을 가진 여러 library가 있거나 container mount가 달라지면 loader가 예상하지 않은 copy를 선택할 수 있다. Symbol이 모두 존재하면 startup은 성공하지만 미묘한 semantic/ABI mismatch가 runtime crash로 나타날 수 있다.

Resolved absolute path와 build ID를 known-good manifest와 비교하면 이런 drift를 잡을 수 있다. Package manager database만 믿지 말고 running process map을 본다.

Security-sensitive deployment는 writable search path를 제거하고 immutable image로 dependency set을 고정한다. 재현 가능한 runtime image가 진단 비용을 크게 줄인다.

## CHAPTER 30 · runtime image contract는 dependency graph·binding·initialization 결과가 결정적이어야 한다

동적 링킹의 최종 산출물은 단순히 모든 `.so`가 열린 상태가 아니다. 어떤 object generation이 mapping됐고, 각 external symbol이 어디에 binding됐으며, TLS와 constructor가 어떤 순서로 준비됐는지가 process 실행 의미를 결정한다.

Build와 배포는 SONAME/ABI, search path, exported symbol, relocation, hardening 설정을 함께 관리해야 한다. Runtime에서는 loaded object build ID와 loader failure를 관측한다.

검증은 production artifact를 실제 loader로 시작하고 `dlopen`/TLS/constructor 경계까지 실행하는 것이다. Static link 성공이나 파일 존재 확인만으로 runtime image가 올바르다고 PASS라고 부를 수 없다.
