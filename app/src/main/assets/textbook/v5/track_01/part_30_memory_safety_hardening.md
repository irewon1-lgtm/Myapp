# PART 30 · Memory Safety and Hardening — canary, ASLR, CFI, tagging, sanitizers

Memory-safety engineering은 취약점 이름을 외우는 작업이 아니다. Out-of-bounds, use-after-free, uninitialized read, invalid control-flow가 발생하지 않게 language/runtime/API를 선택하고, 남은 오류가 exploitation이나 silent corruption으로 이어지기 전에 **compile-time check, runtime instrumentation, hardware tag, memory permission, control-flow validation**으로 검출·격리하는 defense-in-depth 문제다.

---

## CHAPTER 01 · Prevention과 mitigation을 구분한다

Bounds-safe language/runtime가 invalid access 자체를 막는 것은 prevention에 가깝고 ASLR·canary처럼 bug가 존재해도 exploitation을 어렵게 하거나 detect하는 것은 mitigation이다. Mitigation이 있다고 bug가 사라진 것이 아니며, prevention만 믿어 unsafe FFI/native path를 무시해서도 안 된다.

Security review는 각 bug class에 대해 `발생 방지`, `검출`, `악용 제한`, `복구` 층을 따로 기록한다.

---

## CHAPTER 02 · Spatial safety와 temporal safety는 다른 문제다

Spatial memory safety는 object 경계를 넘어선 access를 막고 temporal safety는 object lifetime 종료 후 access를 막는다. Buffer overflow는 spatial violation이고 use-after-free는 temporal violation이다. Bounds check만으로 freed pointer 재사용을 막을 수 없다.

Tool 선택도 달라진다. Redzone/guard는 spatial error를 잘 잡지만 temporal error에는 quarantine/tagging/generation tracking이 필요할 수 있다.

---

## CHAPTER 03 · Stack canary는 return-address 주변 corruption을 탐지하는 sentinel이다

Compiler는 vulnerable stack frame에서 local buffer와 control data 사이에 unpredictable canary value를 배치하고 return 전 값이 변했는지 검사할 수 있다. Canary mismatch는 stack overwrite가 이미 발생했음을 알려 process를 중단한다.

Canary는 arbitrary memory corruption을 모두 막지 않으며 information leak으로 secret value가 노출될 수 있다. Per-process/thread randomness와 fail-fast path가 중요하다.

---

## CHAPTER 04 · Guard page는 invalid range를 unmapped page로 만들어 즉시 fault시킨다

Stack 끝이나 large allocation 주변에 inaccessible page를 두면 overflow가 다음 object를 조용히 덮기 전에 page fault로 중단될 수 있다. Guard page는 page granularity라 small overrun을 모두 잡지 못하고 virtual address space를 소비한다.

Rare but catastrophic overflow를 deterministic crash로 바꾸는 것이 debugging value다.

---

## CHAPTER 05 · ASLR은 address predictability를 낮추는 probabilistic defense다

Executable, shared library, stack, heap 등의 base address를 randomize하면 memory address를 미리 알아야 하는 공격의 reliability를 낮춘다. 그러나 information leak이 address를 노출하면 protection이 약해질 수 있다.

ASLR entropy와 relocation capability가 중요하며 non-PIE executable은 code base randomization 범위를 제한할 수 있다.

---

## CHAPTER 06 · PIE는 main executable도 relocatable하게 만든다

Position-Independent Executable은 main binary를 arbitrary base에 load할 수 있게 해 ASLR이 executable code에도 적용되게 한다. PIC/relocation/GOT model은 PART 12와 연결된다.

PIE는 performance와 relocation cost를 가질 수 있지만 modern toolchain에서는 일반적인 hardening default가 됐다. Build artifact에서 실제 ELF type과 flags를 확인한다.

---

## CHAPTER 07 · RELRO는 relocation 이후 writable metadata를 read-only로 전환한다

Dynamic linker가 startup relocation을 완료한 뒤 GOT 등 일부 region을 read-only로 만들면 memory corruption이 function resolution metadata를 덮는 공격 surface를 줄일 수 있다. Partial/Full RELRO는 eager/lazy binding과 trade-off가 있다.

Protection은 linker option 문자열이 아니라 final program header/memory permission에서 확인한다.

---

## CHAPTER 08 · W^X는 writable memory와 executable memory를 분리한다

Code page를 write+execute로 동시에 허용하면 data corruption이 executable code injection으로 이어질 위험이 커진다. W^X policy는 executable region을 read+execute, writable data를 read+write로 분리한다.

JIT runtime은 code generation 동안 writable, execution 동안 executable state를 안전하게 전환해야 한다. 동시에 W+X 상태가 길게 유지되지 않도록 lifecycle을 관리한다.

---

## CHAPTER 09 · NX/PXN/PAN/SMEP/SMAP류는 privilege-domain memory use를 제한한다

Hardware는 page execute-disable, privileged execute-never, kernel의 userspace execute/access restriction 같은 mechanism을 제공할 수 있다. Kernel이 user-controlled memory를 code/data pointer로 무제한 사용하지 못하게 해 exploitation path를 줄인다.

Architecture마다 이름과 exact semantics가 다르므로 capability를 확인한다. 하나의 protection 이름을 모든 CPU에 일반화하지 않는다.

---

## CHAPTER 10 · Control-Flow Integrity는 indirect branch target 집합을 제한한다

Function pointer, vtable, return address가 corruption돼도 임의 code address로 branch하지 못하도록 allowed control-flow graph를 검증하는 CFI 기법이 있다. Type-based CFI, forward-edge/return-edge protection은 서로 다른 edge를 보호한다.

CFI precision이 낮으면 valid target set이 커져 protection이 약하고 너무 강하면 dynamic language/FFI compatibility가 깨질 수 있다.

---

## CHAPTER 11 · Shadow stack은 return address를 별도 protected storage와 대조한다

Normal stack의 saved return address가 overwrite될 수 있으므로 hardware/software shadow stack은 return target을 별도 protected stack에 저장하고 함수 return 때 검증할 수 있다. Stack canary가 frame corruption을 sentinel로 탐지한다면 shadow stack은 return control-flow data 자체를 이중화한다.

Exception/unwind/context-switch도 shadow state와 일관되게 동기화해야 한다.

---

## CHAPTER 12 · Pointer Authentication은 pointer에 cryptographic tag를 결합한다

일부 ARM architecture는 pointer value와 context를 이용한 authentication code를 pointer spare bits에 저장하고 use 전에 검증하는 PAC mechanism을 제공한다. Return address/function pointer corruption의 성공 가능성을 낮출 수 있다.

PAC key management, discriminator/context 선택, pointer stripping이 ABI/runtime와 결합된다. 모든 memory corruption을 막는 bounds checker는 아니다.

---

## CHAPTER 13 · Memory tagging은 pointer와 allocation에 tag를 부여해 mismatch를 fault로 만든다

MTE/HWASan류는 pointer tag와 memory granule tag를 비교해 invalid object access를 탐지한다. Freed allocation에 새 tag를 부여하면 stale pointer가 같은 address를 가리켜도 tag mismatch로 temporal bug를 잡을 확률이 높아진다.

Tag space가 유한하므로 probabilistic temporal detection일 수 있고 granule 내부 small overflow는 놓칠 수 있다. Detection mode와 overhead를 이해한다.

---

## CHAPTER 14 · Synchronous와 asynchronous MTE fault mode는 진단 정밀도가 다르다

Synchronous checking은 offending access 시점에 fault해 정확한 PC를 제공하지만 overhead가 클 수 있다. Asynchronous mode는 error를 누적한 뒤 나중에 report해 production overhead를 줄일 수 있지만 정확한 faulting instruction을 찾기 어렵다.

Test/debug build와 production canary deployment는 서로 다른 mode를 선택할 수 있다. Error rate와 report quality를 함께 평가한다.

---

## CHAPTER 15 · ASan은 shadow memory와 redzone으로 invalid access를 검출한다

AddressSanitizer는 application memory 일부를 shadow memory에 mapping하고 allocation 주변 redzone과 freed-region poison state를 유지해 instrumented load/store 전 validity를 검사한다. Detection coverage가 높지만 memory/CPU overhead가 커 production 전체 배포에는 부담이 있다.

CI fuzz test와 integration test에서 강력한 oracle로 사용한다.

---

## CHAPTER 16 · HWASan은 software tag와 tagged pointer를 이용해 lower-overhead temporal/spatial detection을 제공한다

HWASan은 address tagging을 활용해 allocation tag와 pointer tag를 비교한다. ASan의 dense shadow/redzone model과 trade-off가 다르며 64-bit address-space/tagging capability를 활용한다.

Report에는 pointer/memory tag, allocation/free stack, nearby tags가 포함될 수 있어 UAF/overflow root cause를 좁히는 데 유용하다.

---

## CHAPTER 17 · GWP-ASan류 sampled allocator는 production에서 일부 allocation만 감시한다

모든 allocation에 heavy sanitizer를 적용하기 어려우면 일부 allocation을 guard page 기반 special pool에 sample해 rare memory bug를 production에서 포착할 수 있다. Sampling이므로 재현 확률은 낮지만 fleet scale에서는 가치가 있다.

Report rate를 bug absence와 동일시하지 않는다. Sampling fraction과 traffic volume을 함께 본다.

---

## CHAPTER 18 · KASAN/KFENCE는 kernel memory bug에 맞는 instrumentation을 제공한다

Kernel memory access는 userspace ASan과 별도 build/runtime constraint를 가진다. KASAN은 kernel address sanitizer 계열이고 KFENCE는 low-overhead sampling/guard page 방식으로 production-like environment에서 bug detection을 돕는다.

Kernel bug는 system-wide impact가 있으므로 test kernel과 production sampling 전략을 분리한다.

---

## CHAPTER 19 · UBSan은 memory access 이전의 undefined operation을 잡을 수 있다

Integer overflow class, invalid shift, misaligned access, invalid enum/cast 같은 undefined behavior는 optimizer가 source programmer의 직관과 다른 code를 만들게 할 수 있다. UndefinedBehaviorSanitizer는 instrumentation으로 일부 UB를 runtime에 검출한다.

ASan과 UBSan은 다른 bug class를 다루므로 하나의 sanitizer PASS로 전체 native correctness를 주장하지 않는다.

---

## CHAPTER 20 · MSan은 uninitialized read의 dataflow를 추적한다

초기화되지 않은 byte가 branch, pointer, output에 사용되면 behavior가 input/history에 따라 비결정적으로 변할 수 있다. MemorySanitizer는 value shadow/origin을 추적해 uninitialized data use를 발견한다.

모든 dependency/library가 compatible instrumentation을 필요로 할 수 있어 deployment 범위를 계획해야 한다.

---

## CHAPTER 21 · TSan은 data race를 happens-before model로 검출한다

ThreadSanitizer는 memory access와 synchronization event를 instrument해 conflicting access가 happens-before로 ordered되지 않았는지 추적한다. Race가 발생한 두 stack을 제공해 shared-state bug를 좁힌다.

Lock-free custom synchronization이나 unsupported atomics는 annotation/false-positive 문제가 생길 수 있다. PART 27의 formal memory-order proof를 대체하지 않는다.

---

## CHAPTER 22 · Allocator quarantine는 freed address 재사용을 늦춰 UAF detection window를 늘린다

Freed block을 즉시 same-size allocation에 재사용하면 stale pointer가 새 valid object를 우연히 가리켜 bug가 숨을 수 있다. Quarantine은 일정 시간/용량 동안 freed block을 재사용하지 않아 stale access가 poisoned region을 hit할 확률을 높인다.

Detection에는 유리하지만 memory footprint를 증가시킨다.

---

## CHAPTER 23 · Memory poisoning은 stale/uninitialized state를 recognizable pattern으로 만든다

Allocation/free 시 known pattern을 채우거나 metadata poison bit를 설정하면 invalid access가 deterministic failure나 recognizable corruption으로 나타날 수 있다. Production에서 항상 가능한 것은 아니지만 debug allocator/kernel hardening에 유용하다.

Poison pattern 자체가 secret이 아니며 attacker-resistant integrity mechanism으로 오해하지 않는다.

---

## CHAPTER 24 · Integer overflow check는 memory-safety boundary 앞에 위치한다

`count * elementSize`, `header + payload` 계산이 overflow하면 allocator는 너무 작은 buffer를 만들고 뒤 copy가 bounds를 넘을 수 있다. Checked arithmetic은 allocation/copy 전에 size expression이 representable한지 검증한다.

Length field validation은 type range뿐 아니라 multiplication/addition sequence 전체를 확인한다.

---

## CHAPTER 25 · Fortified library는 known object size를 이용해 dangerous copy를 조기에 막을 수 있다

Compiler가 destination object size를 알 수 있을 때 memcpy/strcpy류 호출을 fortified variant로 바꿔 runtime/compile-time bounds violation을 검출할 수 있다. Optimization과 object-size inference에 따라 coverage가 달라질 수 있다.

Safe wrapper를 썼다는 이름보다 final binary에 protection이 적용됐는지 build flag와 disassembly/report로 확인한다.

---

## CHAPTER 26 · Fuzzing과 sanitizer를 결합하면 invalid state를 자동 탐색하고 강한 oracle로 판정한다

Coverage-guided fuzzer가 parser/API input space를 탐색하고 sanitizer가 OOB/UAF/UB를 즉시 failure로 만들면 사람이 예상하지 못한 path를 효율적으로 찾을 수 있다. Crash input은 minimization 후 regression test로 고정한다.

Fuzzer corpus diversity와 sanitizer configuration을 artifact로 보존해 coverage regression을 추적한다.

---

## CHAPTER 27 · Crash report는 fault address만이 아니라 allocation lifetime을 보여줘야 한다

Memory bug root cause는 crash PC보다 object allocation/free site가 더 중요할 수 있다. Sanitizer report, allocator stack trace, tag dump, thread list를 함께 수집해 `누가 만들고 누가 해제했고 누가 나중에 접근했는가`를 복원한다.

Symbolization에는 exact build ID/debug symbol이 필요하다. Wrong symbols는 false root cause를 만든다.

---

## CHAPTER 28 · Hardening option은 release artifact에서 검증해야 한다

Build configuration에 PIE/CFI/stack protector flag가 있어도 LTO/link step에서 빠지거나 특정 library가 다르게 빌드될 수 있다. Release binary의 ELF headers, program permissions, symbol metadata를 검사하는 CI gate를 둔다.

Source config PASS와 artifact protection PASS를 분리한다.

---

## CHAPTER 29 · Performance budget 때문에 보호를 끌 때는 risk delta를 명시한다

Sanitizer full instrumentation은 production에 비쌀 수 있지만 canary cohort, sampling allocator, hardware tagging처럼 lower-overhead option이 있다. Protection을 제거하기 전에 실제 overhead를 측정하고 어떤 bug class detection이 사라지는지 기록한다.

`느려서 껐다`가 아니라 latency/CPU/memory 변화와 residual risk를 함께 승인한다.

---

## CHAPTER 30 · Memory-safety strategy는 prevent·detect·contain·diagnose의 네 층으로 평가한다

1. **Prevent** — memory-safe language, bounds-checked API, checked arithmetic으로 invalid operation 발생을 줄인다.
2. **Detect** — sanitizer, tag, canary, CFI가 오류를 빠르게 드러낸다.
3. **Contain** — W^X, ASLR, privilege isolation이 memory bug의 blast radius를 제한한다.
4. **Diagnose** — exact build symbol, allocation lifetime, tag/sanitizer report로 root cause를 재현한다.

한 mitigation이 네 층을 모두 대체하지 않는다. Senior-level memory safety review는 `보안 flag 목록`이 아니라 **각 bug class가 어느 층에서 차단되며 escape path가 무엇인지**를 증명한다.
