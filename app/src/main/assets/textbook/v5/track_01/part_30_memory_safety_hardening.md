# PART 30 · Memory Safety and Hardening — prevention, detection, mitigation

메모리 안전은 하나의 sanitizer나 compiler flag로 완성되지 않는다. spatial/temporal bug를 줄이는 language·API 선택, exploit을 어렵게 만드는 hardening, bug를 빨리 드러내는 sanitizer와 fuzzing, 정확한 crash artifact가 서로 다른 층을 담당한다. **prevention, detection, mitigation, diagnosis**를 분리해 설계해야 한다.

---

## CHAPTER 01 · prevention과 mitigation은 다른 목표를 가진다

prevention은 out-of-bounds, use-after-free 같은 bug 자체가 생길 가능성을 줄이고 mitigation은 bug가 존재해도 임의 코드 실행이나 privilege escalation으로 이어지기 어렵게 만든다. ASLR이나 canary가 memory bug를 제거하지 못하고, memory-safe language도 FFI/native boundary에서는 다시 unsafe state를 만날 수 있다.

한 mitigation이 켜졌다는 이유로 unsafe parser나 unchecked length를 방치하면 defense-in-depth가 단일 실패점으로 변한다. 반대로 모든 bug를 사전에 없앨 수 있다는 가정도 현실적이지 않다.

security review에서 각 control이 prevention/detection/mitigation 중 무엇을 제공하는지 표로 남긴다. 동일 failure mode를 독립 layer가 어떻게 막는지 확인해 공통 원인 하나로 모두 무력화되지 않게 한다.

---

## CHAPTER 02 · spatial과 temporal safety는 서로 다른 lifetime 오류다

spatial memory error는 유효 object boundary 밖을 접근하는 문제고 temporal error는 free된 object를 다시 사용하거나 lifetime 이전/이후에 접근하는 문제다. bounds check만 강화해도 use-after-free는 남고, allocator quarantine만 사용해도 buffer overflow는 남는다.

같은 crash address가 heap 주변이라고 두 종류를 구분할 수 있는 것은 아니다. allocation/free history와 object size가 필요하다.

sanitizer report와 allocator trace를 함께 보존한다. regression corpus는 overflow, underflow, UAF, double-free를 별도 testcase로 유지해 한 mitigation의 범위를 과대평가하지 않는다.

---

## CHAPTER 03 · stack canary는 return path의 특정 overwrite를 탐지한다

stack canary는 control data 근처에 예측하기 어려운 값을 두고 function return 전에 값이 유지됐는지 확인해 일부 stack overwrite를 탐지한다. canary 검사가 실패하면 이미 memory corruption이 발생했다는 뜻이므로 안전하게 중단하는 것이 일반적이다.

canary가 모든 stack corruption을 잡는 것은 아니다. canary를 건드리지 않는 field overwrite나 information leak으로 canary 값을 알아낸 공격은 다른 방어가 필요하다.

release artifact에서 실제 stack protector 적용 여부를 확인한다. crash report에는 canary failure와 build ID를 남겨 취약 함수 추적이 가능하게 한다.

---

## CHAPTER 04 · guard page는 경계를 넘는 접근을 즉시 fault로 바꾼다

stack이나 중요한 mapping 주변에 접근 불가능한 page를 두면 overflow가 조용히 인접 object를 훼손하기 전에 page fault로 드러날 수 있다. guard page는 page granularity이므로 작은 intra-object overflow를 모두 잡지는 못한다.

large stack frame이나 deep recursion은 정상 workload에서도 guard page에 닿을 수 있다. crash를 단순 invalid pointer로 처리하지 말고 stack growth와 mapping boundary를 확인해야 한다.

fault address와 `/proc` style mapping을 비교한다. stack usage report와 worst-case recursion test를 함께 유지해 detection과 capacity 문제를 구분한다.

---

## CHAPTER 05 · ASLR은 address predictability를 낮추는 mitigation이다

ASLR은 executable, library, stack, heap 등의 base address를 randomize해 공격자가 reusable code나 object address를 미리 알기 어렵게 한다. 하지만 pointer leak가 있으면 randomization entropy가 크게 약화될 수 있다.

ASLR은 buffer overflow를 막지 않는다. fixed-address region, low entropy, fork로 동일 layout이 반복되는 환경도 threat model에 포함한다.

release에서 PIE와 library randomization이 실제 적용됐는지 확인한다. crash symbolization은 absolute address 대신 module-relative offset과 build ID를 사용한다.

---

## CHAPTER 06 · PIE는 main executable도 random base에 load할 수 있게 한다

Position Independent Executable은 main binary의 code/data reference를 relocatable하게 만들어 ASLR이 executable base에도 적용될 수 있게 한다. compiler flag만 설정됐다고 final artifact가 PIE라는 보장은 없으므로 ELF type과 relocation을 확인해야 한다.

hand-written assembly나 absolute address assumption은 PIE 전환에서 깨질 수 있다. 성능 비용은 target architecture와 relocation model에 따라 다르므로 일반화하지 않는다.

CI에서 final binary property를 검사한다. startup과 code-size regression이 있다면 generated code와 relocation 수로 원인을 분석한다.

---

## CHAPTER 07 · RELRO는 relocation metadata의 writable window를 줄인다

dynamic loader가 relocation을 끝낸 뒤 GOT 같은 metadata 일부를 read-only로 전환하면 memory corruption이 control-flow target overwrite로 이어지는 surface를 줄일 수 있다. full/partial RELRO와 eager binding의 조합에 따라 protection 범위가 다르다.

flag 이름만 보고 보호됐다고 판단하지 않는다. custom linker script나 loader configuration이 final segment permission을 바꿀 수 있다.

program header와 dynamic flag를 release artifact에서 검증한다. protection 변경 후 startup latency와 symbol binding behavior도 함께 측정한다.

---

## CHAPTER 08 · W^X는 writable memory와 executable memory의 동시성을 제한한다

Write XOR Execute 원칙은 동일 mapping을 write와 execute 모두 가능하게 두지 않아 data corruption이 곧 injected code execution으로 이어지는 위험을 줄인다. JIT는 code generation 중 write, publication 후 execute로 state를 전환해야 한다.

RWX mapping을 편의상 상시 유지하면 security boundary가 약해진다. permission transition과 instruction-cache synchronization도 correctness에 필요하다.

mapping permission을 runtime에서 검사한다. JIT stress에서 concurrent execution 중 partially generated code가 노출되지 않는지 generation protocol을 검증한다.

---

## CHAPTER 09 · hardware memory protection은 privilege와 page permission을 강제한다

MMU page permission, execute-never, user/kernel separation 같은 hardware protection은 software가 잘못된 memory access를 architectural fault로 바꾸는 기본 boundary다. privileged kernel bug나 DMA는 이 boundary 밖의 별도 threat를 가진다.

page permission을 넓게 주고 application check만 믿으면 compromised code가 protection을 우회하기 쉽다. least privilege는 memory mapping에도 적용된다.

mapping audit에서 RWX, unexpected user-accessible region, device mapping을 확인한다. permission fault와 application access log를 correlation해 false positive가 아닌 실제 invariant violation을 찾는다.

---

## CHAPTER 10 · CFI는 indirect control-flow target의 집합을 제한한다

Control-Flow Integrity는 indirect call, virtual dispatch, return 같은 control transfer가 허용된 target 집합 밖으로 이동하지 못하게 검사해 pointer corruption의 exploitability를 줄인다. policy precision은 compiler와 whole-program visibility에 따라 달라질 수 있다.

너무 coarse한 CFI는 공격 가능한 target이 많이 남고, 잘못된 type/ABI annotation은 legitimate call을 차단할 수 있다. plugin과 JIT boundary는 별도 고려가 필요하다.

CFI violation report를 build ID와 symbol에 연결한다. release binary에서 instrumentation coverage와 excluded module을 inventory한다.

---

## CHAPTER 11 · shadow stack은 return address를 별도 보호 영역에 보존한다

shadow stack은 normal stack의 return address가 overwrite되더라도 trusted copy와 비교하거나 별도 hardware stack에서 return target을 관리해 ROP surface를 줄인다. call/return pairing이 깨지는 unusual control flow와 compatibility가 중요하다.

signal, setjmp/longjmp, exception unwinding이 shadow state와 일관되게 동작해야 한다. hand-written assembly가 protocol을 따르지 않으면 crash가 날 수 있다.

feature enable 여부와 violation telemetry를 확인한다. exception/signal-heavy workload를 포함해 compatibility test를 유지한다.

---

## CHAPTER 12 · pointer authentication은 pointer와 context를 cryptographically 묶는다

Pointer Authentication 계열은 pointer 일부에 authentication code를 저장해 return address나 function pointer가 예상 context에서 생성된 값인지 검증한다. memory corruption이 arbitrary pointer substitution으로 이어지는 비용을 높인다.

key 관리와 context 선택이 약하면 protection이 줄고, raw pointer serialization이나 bit manipulation과 compatibility 문제가 생길 수 있다. PAC 실패는 원래 corruption 지점보다 늦게 나타날 수 있다.

crash에는 authentication failure와 corrupted pointer chain을 보존한다. instrumentation이 원인 write를 찾을 수 있도록 sanitizer와 병행한다.

---

## CHAPTER 13 · memory tagging은 pointer와 allocation generation을 연결한다

Memory Tagging은 pointer와 memory allocation에 작은 tag를 부여해 접근 시 일치 여부를 검사하고 stale pointer나 일부 out-of-bounds access를 탐지한다. free 후 새 allocation이 다른 tag를 받으면 UAF detectability가 높아진다.

tag bit 수가 유한하므로 collision 가능성이 있고 모든 spatial overflow를 잡는 것은 아니다. allocator와 ABI가 tag-aware해야 한다.

allocation/free tag와 fault report를 연결한다. production sampling mode와 test synchronous mode의 detection coverage 차이를 명시한다.

---

## CHAPTER 14 · MTE mode는 detection timing과 overhead를 바꾼다

MTE는 synchronous 또는 asynchronous fault handling 등 여러 mode로 사용할 수 있어 bug detection precision과 performance cost가 달라진다. synchronous mode는 offending access와 fault를 가깝게 연결하지만 overhead가 더 클 수 있다.

async mode에서는 fault report가 실제 bad access보다 늦어져 stack만으로 원인을 찾기 어렵다. production rollout은 crash rate와 user impact를 고려해야 한다.

mode와 fault count를 release telemetry에 기록한다. 같은 corpus를 mode별로 실행해 detection coverage와 cost를 비교한다.

---

## CHAPTER 15 · ASan은 shadow memory와 redzone으로 invalid access를 찾는다

AddressSanitizer는 allocation 주변 redzone과 shadow metadata를 사용해 많은 heap/stack out-of-bounds와 UAF를 runtime에 탐지한다. instrumentation과 allocator replacement 때문에 memory와 CPU overhead가 크다.

ASan build에서 timing과 memory layout이 달라 race나 OOM behavior가 바뀔 수 있다. report 없음이 모든 memory bug 부재를 증명하지 않는다.

CI/fuzz에서 broad corpus를 실행하고 report artifact에 exact symbol과 input을 보존한다. 발견된 bug는 non-sanitized regression test도 추가한다.

---

## CHAPTER 16 · HWASan은 tagged pointer를 이용해 큰 address space에서 temporal bug를 잡는다

HWASan 계열은 pointer tag와 allocation tag를 비교해 UAF와 out-of-bounds를 탐지하며 traditional ASan과 다른 memory/architecture trade-off를 갖는다. Android native debugging에 특히 유용할 수 있다.

tag collision과 unsupported boundary가 남을 수 있고 instrumented/uninstrumented code가 섞이는 FFI에서 규칙을 이해해야 한다.

fault tag, allocation stack, free stack을 함께 보존한다. long-running workload에서 temporal bug reproduction rate를 비교한다.

---

## CHAPTER 17 · GWP-ASan은 일부 allocation만 선택해 production overhead를 낮춘다

GWP-ASan은 sampling된 allocation을 guard page와 special allocator로 보호해 낮은 overhead로 production UAF/OOB를 포착하려는 방식이다. 모든 allocation을 보호하지 않으므로 bug가 재현되어도 선택되지 않으면 report가 없을 수 있다.

sampling rate를 높이면 detection probability와 memory cost가 함께 증가한다. rollout population과 session 길이도 실제 coverage를 좌우한다.

sampled allocation 수와 crash report를 추적한다. production 발견 input을 full sanitizer lab test로 재현해 원인 write를 좁힌다.

---

## CHAPTER 18 · KASAN과 KFENCE는 kernel memory bug를 다른 비용 모델로 탐지한다

KASAN은 kernel memory access를 광범위하게 instrument해 높은 detection coverage를 제공하지만 overhead가 커 test/debug kernel에 적합하다. KFENCE는 sampling 기반으로 낮은 overhead에서 production-like 환경의 일부 heap bug를 찾는다.

user-space sanitizer report만으로 driver/kernel memory corruption을 배제할 수 없다. bug가 kernel allocation lifecycle에 있다면 다른 tool이 필요하다.

fault injection과 kernel selftest를 mode별로 수행한다. report가 어떤 allocator/object를 가리키는지 exact kernel build와 연결한다.

---

## CHAPTER 19 · UBSan은 undefined behavior를 runtime에 드러낸다

UndefinedBehaviorSanitizer는 signed overflow, invalid shift, alignment, type-related undefined behavior 같은 language semantic violation을 검사한다. 이런 UB는 debug에서는 우연히 동작하다 optimizer가 assumption을 사용할 때 release에서 miscompile처럼 나타날 수 있다.

모든 UB check를 production에 켜면 overhead와 failure policy가 달라질 수 있다. recover mode와 trap mode의 의미도 구분해야 한다.

report input을 최소화하고 compiler version을 보존한다. UB를 단순 sanitizer warning으로 무시하지 말고 source contract를 수정한다.

---

## CHAPTER 20 · MSan은 초기화되지 않은 값의 data flow를 추적한다

MemorySanitizer는 initialized 여부를 shadow state로 추적해 uninitialized value가 branch, syscall, output에 사용되는 문제를 탐지한다. 모든 dependency가 instrumented되지 않으면 false negative/compatibility 문제가 생길 수 있다.

uninitialized padding과 실제 semantic value를 구분해야 한다. native library boundary에서 origin 정보가 끊길 수 있다.

fully instrumented test environment를 준비한다. report에는 origin stack을 보존하고 initialization fix 뒤 같은 input을 재실행한다.

---

## CHAPTER 21 · TSan은 data race의 happens-before를 동적으로 추적한다

ThreadSanitizer는 memory access와 synchronization event를 instrument해 conflicting access 사이 happens-before가 없는 경우를 탐지한다. memory safety hardening에서 race는 object lifetime corruption의 원인이 될 수 있어 별도 class로 다뤄야 한다.

instrumentation overhead가 scheduling을 크게 바꾸고 custom atomic/lock primitive가 detector에 알려지지 않으면 report 품질이 달라질 수 있다.

report를 ownership model과 대조한다. 발견 race는 explicit synchronization과 deterministic regression test로 수정한다.

---

## CHAPTER 22 · quarantine은 freed memory의 즉시 재사용을 늦춘다

allocator quarantine은 free된 block을 잠시 재사용하지 않아 stale pointer가 다른 valid object를 조용히 수정하기보다 poison/guard에 닿을 가능성을 높인다. temporal bug detectability는 좋아지지만 memory footprint가 증가한다.

production에서 quarantine이 너무 크면 memory pressure가 새로운 failure를 만들 수 있다. security allocator와 sanitizer mode마다 정책이 다르다.

quarantine size, RSS, UAF detection rate를 비교한다. detection용 setting과 production capacity를 분리한다.

---

## CHAPTER 23 · memory poisoning은 freed/uninitialized state를 recognizable pattern으로 바꾼다

allocator나 kernel debug option이 free object와 padding을 poison pattern으로 채우면 stale read/write와 uninitialized access가 crash dump에서 더 쉽게 드러날 수 있다. poison 자체가 access를 막는 hardware protection은 아니다.

optimizer나 device DMA가 pattern을 예상과 다르게 만들 수 있고, production에서는 fill cost가 클 수 있다.

crash dump에서 poison signature를 allocator metadata와 함께 해석한다. pattern 발견을 원인으로 끝내지 말고 어떤 stale owner가 접근했는지 추적한다.

---

## CHAPTER 24 · checked arithmetic은 allocation size 계산의 overflow를 막는다

`count * element_size + header` 같은 size arithmetic이 integer overflow하면 작은 allocation을 만들고 이후 큰 copy가 buffer overflow로 이어질 수 있다. bounds check 전에 계산 자체가 wrap했는지 검증해야 한다.

signed/unsigned conversion과 platform width 차이가 boundary bug를 만든다. attacker-controlled length는 특히 중요하다.

checked multiply/add helper를 공통 사용한다. max-size, zero, near-overflow input을 fuzz corpus에 포함한다.

---

## CHAPTER 25 · FORTIFY는 known object size를 활용해 unsafe libc 사용을 탐지한다

fortify 계열은 compiler가 destination object size를 알 수 있을 때 memcpy/string operation의 size mismatch를 compile/runtime에 검사할 수 있다. optimization과 object-size analysis가 충분해야 효과가 크다.

pointer가 escape해 size 정보를 잃으면 protection 범위가 줄 수 있다. custom wrapper가 compiler builtin recognition을 막기도 한다.

build warning과 runtime fortify failure를 release gate에 포함한다. final binary의 enabled flags와 compiler optimization level을 보존한다.

---

## CHAPTER 26 · fuzzing과 sanitizer를 결합하면 crash를 빠르게 root cause로 연결할 수 있다

coverage-guided fuzzing은 parser와 state machine에 다양한 input을 생성하고 sanitizer는 invalid memory state가 발생한 순간을 자세히 보고한다. 단순 crash-only fuzz보다 detection depth가 높다.

corpus가 protocol state를 충분히 만들지 못하면 deep path에 도달하지 않는다. nondeterminism과 timeout도 coverage를 왜곡할 수 있다.

crash input을 최소화하고 deduplicate한다. fixed input을 permanent regression corpus에 추가해 동일 bug class의 재발을 막는다.

---

## CHAPTER 27 · memory crash report는 allocation과 binary identity를 함께 보존한다

native crash에서 fault address와 stack만으로는 UAF인지 OOB인지 알기 어려운 경우가 많다. allocator metadata, mapping, signal code, register, build ID, sanitizer tag/history가 함께 있으면 lifetime을 복원하기 쉽다.

잘못된 symbol file은 그럴듯한 오진을 만든다. ASLR 때문에 raw absolute address도 execution마다 달라진다.

module build ID와 symbol artifact를 content-addressed로 보존한다. crash pipeline은 report 생성 자체가 실패했을 때 raw tombstone을 남긴다.

---

## CHAPTER 28 · artifact hardening은 source flag가 아니라 final binary에서 검증한다

compiler command에 protector, PIE, RELRO, CFI flag가 있어도 link 단계나 특정 module exception 때문에 final artifact coverage가 다를 수 있다. release gate는 source config가 아니라 실제 ELF/loader property를 검사해야 한다.

third-party native library 하나가 hardening 없이 포함되면 application 전체 attack surface가 남는다. ABI별 artifact도 각각 확인한다.

build provenance와 hardening report를 release record에 저장한다. toolchain upgrade 시 property drift를 자동 diff한다.

---

## CHAPTER 29 · protection budget은 overhead와 coverage를 risk 기준으로 배분한다

모든 production process에 heavy sanitizer를 100% 켤 수 없다면 critical parser, privileged service, high-risk native component에 더 강한 protection과 sampling을 배분한다. overhead budget도 threat model과 연결해야 한다.

성능 문제 때문에 security control을 완전히 끄는 대신 sampling, hardware assist, staged rollout 같은 대안을 찾는다. control 간 중복과 gap을 함께 본다.

CPU/memory overhead와 detection event를 지속 측정한다. 제품 SLO와 security coverage가 동시에 유지되는 operating point를 선택한다.

---

## CHAPTER 30 · memory-safety strategy는 prevention→detection→mitigation→diagnosis를 연결한다

장기 전략은 memory-safe language와 safer API로 new bug를 줄이고, unsafe boundary를 좁히며, compiler/hardware hardening으로 exploitability를 낮추고, sanitizer·fuzzing·production sampling으로 남은 bug를 빠르게 찾는 구조다. crash artifact가 정확해야 발견된 문제를 실제 source fix로 되돌릴 수 있다.

하나의 tool을 '완전한 해결책'으로 선언하지 않는다. spatial, temporal, race, integer, control-flow failure마다 coverage가 다른 것을 인정하고 layered defense를 설계한다.

CLEAN 검증은 artifact hardening 검사, sanitizer corpus, fuzzing, fault/crash symbolization을 서로 다른 failure class로 실행한다. 최종 목표는 단순 crash 감소가 아니라 unsafe state가 외부 compromise나 silent corruption으로 이어지기 전에 예방·검출·격리되는 system을 만드는 것이다.
