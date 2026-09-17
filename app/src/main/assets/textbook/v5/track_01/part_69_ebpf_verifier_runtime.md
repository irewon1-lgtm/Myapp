# PART 69 · eBPF Verifier and Runtime — type state, bounds, helpers, maps, references, JIT

eBPF의 핵심은 `kernel에서 작은 프로그램을 실행한다`가 아니다. Untrusted program을 kernel context에서 실행하려면 load 시점에 **control flow, register type, pointer provenance, bounds, stack initialization, helper contract, resource lifetime**을 증명해야 한다. Verifier가 받아들인 program만 interpreter/JIT가 실행한다. 따라서 BPF programming model은 ISA보다 verifier가 이해할 수 있는 proof language에 가깝다.

## CHAPTER 01 · Verification은 syntax check가 아니라 abstract execution이다

Verifier는 instruction stream을 따라 가능한 execution paths를 탐색하면서 각 register와 stack slot의 abstract state를 갱신한다. 실제 값을 모두 실행하는 것이 아니라 `이 register는 context pointer`, `이 scalar는 0..127 범위` 같은 facts를 추적한다. Safety proof는 이 facts가 모든 reachable path에서 memory access와 helper precondition을 만족하는지 확인하는 과정이다.

## CHAPTER 02 · Uninitialized register read는 load 시점에 거부된다

Machine CPU는 임의 register bit를 읽을 수 있지만 BPF verifier는 definition-before-use를 강제한다. Exit return register R0도 모든 exit path에서 initialized되어야 한다. 이 규칙은 uninitialized kernel data leak과 undefined control behavior를 load 전에 차단한다.

## CHAPTER 03 · Register type은 bit pattern보다 pointer provenance를 표현한다

64-bit 값이 주소처럼 보여도 verifier가 `SCALAR_VALUE`로 알고 있으면 arbitrary memory dereference에 사용할 수 없다. `PTR_TO_CTX`, map value pointer, stack pointer 등 base provenance가 있는 값만 해당 access rule 안에서 dereference할 수 있다. Pointer cast를 통한 kernel address forgery를 제한하는 핵심이다.

## CHAPTER 04 · Pointer arithmetic은 base type과 allowed offset 범위를 유지해야 한다

Known-safe pointer에 bounded scalar offset을 더하면 verifier가 resulting range를 추적할 수 있지만 arbitrary pointer+pointer 연산은 provenance를 잃는다. Arithmetic이 base object bounds를 벗어날 가능성이 있으면 load/store를 거부한다. C source상 합법처럼 보여도 verifier proof가 부족하면 load되지 않는다.

## CHAPTER 05 · Scalar range analysis가 bounds check를 proof로 바꾼다

Verifier는 branch condition을 통해 register의 signed/unsigned min/max와 bit knowledge를 좁힌다. `if (idx < len)` 이후 true branch에서 idx 범위가 충분히 좁아지면 packet/map array access가 허용될 수 있다. Runtime bounds check 코드는 verifier에게도 이해 가능한 control-flow shape여야 한다.

## CHAPTER 06 · 32-bit와 64-bit arithmetic facts는 구분해야 한다

ALU32 operation은 upper bits를 다르게 처리할 수 있고 signed extension/zero extension에 따라 verifier range가 달라진다. Source에서 `int`와 `long`을 무심코 섞으면 verifier가 pointer offset bound를 증명하지 못할 수 있다. BPF에서 integer width는 performance뿐 아니라 proof precision에 영향을 준다.

## CHAPTER 07 · Stack access는 fixed frame boundary와 initialization state를 추적한다

R10 frame pointer 기준 negative offset 범위 안에서만 stack slot을 접근할 수 있고 read 전에 해당 bytes가 initialized돼 있어야 한다. Struct padding을 helper에 넘길 때 일부 byte가 uninitialized라면 verifier가 reject할 수 있다. Stack memory도 byte-level initialization proof 대상이다.

## CHAPTER 08 · Spill된 pointer는 stack에서도 pointer type으로 추적된다

Callee-saved register가 부족해 pointer를 stack에 spill했다가 load할 때 verifier는 단순 64-bit scalar가 아니라 원래 pointer type과 id를 복원할 수 있다. 그러나 pointer slot 일부를 scalar store로 덮으면 provenance가 깨진다. Stack slot type-state가 dataflow analysis에 포함되는 이유다.

## CHAPTER 09 · CFG 검증은 unreachable/invalid control flow를 먼저 제거한다

Verifier는 instruction boundary와 jump target, function call graph를 검증하고 불가능하거나 허용되지 않은 control structure를 제거한다. 현대 BPF는 bounded loop를 지원하지만 termination proof가 필요하다. Arbitrary unbounded loop는 kernel CPU monopolization risk 때문에 받아들일 수 없다.

## CHAPTER 10 · Loop termination proof는 iteration bound를 abstract state로 계산한다

Loop variable의 initial value, update, exit condition이 verifier가 이해 가능한 범위면 bounded loop가 허용될 수 있다. Bound가 매우 크거나 state explosion을 만들면 program이 이론상 finite여도 complexity limit에 걸릴 수 있다. Algorithm complexity와 verifier complexity는 다른 제약이다.

## CHAPTER 11 · State pruning은 같은 program point의 equivalent facts를 병합한다

모든 branch path를 끝까지 개별 탐색하면 exponential explosion이 생긴다. Verifier는 이미 방문한 instruction에서 현재 abstract state가 기존 state에 포함되거나 equivalent하면 재탐색을 줄인다. Code shape가 불필요하게 많은 distinct states를 만들면 verification time이 증가한다.

## CHAPTER 12 · Helper function은 typed kernel capability다

BPF program은 arbitrary kernel function을 호출하지 않고 program type에 허용된 helpers를 통해 kernel capability를 사용한다. Verifier는 helper prototype의 argument type/size/nullability를 확인하고 return type을 register state에 반영한다. Helper surface는 BPF sandbox의 syscall table과 비슷한 역할을 한다.

## CHAPTER 13 · Helper call 뒤 scratch register state는 ABI 규칙에 따라 사라진다

BPF calling convention에서 R1-R5는 arguments/scratch, R6-R9는 callee-saved, R0는 return value다. Helper call 뒤 R1-R5 값을 계속 신뢰하면 verifier가 reject한다. Long-lived pointer/state는 callee-saved register나 stack에 안전하게 보존해야 한다.

## CHAPTER 14 · kfunc는 BTF type information으로 더 세밀한 contract를 표현한다

Kernel function을 BPF에 노출할 때 BTF 기반 annotation으로 trusted pointer, nullable return, reference acquisition/release, map identity 같은 semantics를 verifier에 제공할 수 있다. Function이 C signature만 맞는다고 안전한 것이 아니다. Verifier-visible lifetime/provenance contract가 함께 필요하다.

## CHAPTER 15 · Reference-acquiring kfunc/helper는 모든 exit path에서 release proof가 필요하다

Socket/object reference나 ring-buffer record를 획득한 뒤 program path가 종료될 때 release/submit/discard하지 않으면 resource leak이 된다. Verifier는 reference ID를 추적해 unmatched lifetime을 reject할 수 있다. Resource ownership이 compile/load-time linearity proof로 바뀐다.

## CHAPTER 16 · BPF map은 program과 userspace가 공유하는 state object다

Hash/array/per-CPU/LRU/ringbuf 등 map type마다 lookup/update/concurrency semantics가 다르다. Map FD와 program lifetime은 reference counted kernel objects로 관리된다. `map=딕셔너리`라고만 이해하면 per-CPU value, preallocation, LRU eviction, mmapable array 같은 성능/ownership 차이를 놓친다.

## CHAPTER 17 · Map value pointer lifetime은 map operation과 synchronization에 의존한다

Lookup helper가 반환한 pointer를 arbitrary long-lived global pointer처럼 저장할 수 있는 것은 아니다. Map type과 program context에 따라 value address stability와 concurrent update semantics가 다르다. Verifier가 허용한 scope 밖으로 pointer를 escape시키는 방법은 제한된다.

## CHAPTER 18 · Per-CPU map은 global atomic을 shard memory footprint로 교환한다

CPU별 value copy를 두면 hot counter update에서 cross-core contention을 줄일 수 있다. Userspace가 global total을 읽으려면 모든 CPU values를 aggregate해야 하고 CPU count에 비례해 memory가 늘어난다. P66의 per-CPU fast-path 설계와 같은 partitioning trade-off가 kernel-managed map에도 존재한다.

## CHAPTER 19 · Ring buffer reserve/submit은 variable-length event의 ownership protocol이다

Producer는 ring space를 reserve하고 record를 채운 뒤 submit 또는 discard한다. Verifier reference tracking은 reserved record가 exit path에서 방치되지 않도록 검사한다. Reserve success와 consumer visibility는 submit commit으로 분리된다. Multi-record atomicity는 discard/commit policy와 연결된다.

## CHAPTER 20 · Ring buffer는 ordering과 wakeup policy를 함께 설계한다

Per-CPU buffers는 scalability가 좋지만 cross-CPU event ordering을 복원하기 어렵다. Shared BPF ring buffer는 multiple producers의 global order를 더 잘 보존할 수 있다. Wakeup suppression/force flags는 notification rate와 consumer latency를 교환한다.

## CHAPTER 21 · Tail call은 program graph를 runtime dispatch로 확장한다

BPF tail call은 map을 통해 다음 program으로 control을 넘겨 modular policy pipeline을 만들 수 있다. Return stack을 계속 쌓는 ordinary function call과 다르며 recursion/loop를 막기 위한 depth limit이 있다. Program graph update가 live traffic behavior를 즉시 바꿀 수 있어 versioning이 필요하다.

## CHAPTER 22 · Program-local BPF function은 verifier call graph와 stack depth에 포함된다

Subprogram call은 code reuse를 가능하게 하지만 각 frame stack usage와 call depth를 합산해야 한다. Inlining 여부와 local function split이 verifier complexity, JIT code layout, stack bound에 영향을 준다. Ordinary compiler recursion을 그대로 허용하지 않는다.

## CHAPTER 23 · Program type은 context pointer가 무엇을 가리키는지 결정한다

XDP, tracing, cgroup, socket filter 등 attachment point마다 R1 context structure와 allowed fields/helpers가 다르다. 같은 C helper wrapper가 다른 program type에서는 verifier에서 reject될 수 있다. Attachment point는 event source뿐 아니라 capability/security model이다.

## CHAPTER 24 · Attach lifetime은 program/map lifetime과 별도다

Program FD가 존재해도 link가 detach되면 event path에서 실행되지 않고, pinned object가 filesystem namespace에 남으면 creating process 종료 뒤에도 kernel object가 유지될 수 있다. Program, link, map, pin reference graph를 명시해야 cleanup leak을 막는다.

## CHAPTER 25 · Pinning은 transient FD object를 persistent control-plane object로 바꾼다

bpffs pin은 map/program/link를 path로 다시 열 수 있게 해 daemon restart 후 state를 재사용할 수 있다. 그러나 upgrade 시 old pinned maps의 key/value layout이 새 program 기대와 다르면 subtle corruption이 생긴다. Pin path에도 schema/version metadata가 필요하다.

## CHAPTER 26 · JIT은 verified BPF를 native code로 바꾸지만 safety proof를 다시 정의하지 않는다

Verifier가 safe semantics를 증명한 뒤 JIT compiler가 architecture machine code를 생성한다. JIT bug가 verifier assumptions를 깨뜨리면 kernel privilege에서 실행되므로 매우 위험하다. Interpreter/JIT differential test와 constant blinding/hardening 같은 별도 방어가 필요하다.

## CHAPTER 27 · JIT code memory는 W^X와 code lifetime 규칙을 가져야 한다

Generated code page는 compilation 동안 writable, execution 동안 executable state를 적절히 전환해야 하고 freed program의 stale function pointer가 남지 않아야 한다. BPF program update/unload는 RCU 같은 deferred lifetime protocol과 결합될 수 있다.

## CHAPTER 28 · Verifier rejection log는 proof failure를 읽는 디버깅 자료다

`invalid access` 한 줄만 보는 대신 해당 instruction에서 register type, known bounds, null state, reference set이 무엇이었는지 읽어야 한다. Source-level check가 있는데도 verifier가 못 증명하면 code를 verifier가 추론 가능한 control-flow 형태로 재구성해야 한다. 안전성 기준을 낮추는 cast는 해결이 아니다.

## CHAPTER 29 · Verifier/JIT performance도 control-plane SLO다

매 request마다 BPF program을 load하는 구조는 verifier path exploration과 JIT compile 비용을 user latency에 넣는다. Production은 program load를 deploy/control-plane 단계로 분리하고 verified artifact identity를 관리해야 한다. Large generated policy는 verification state explosion을 benchmark해야 한다.

## CHAPTER 30 · eBPF runtime은 kernel capability를 proof-carrying code로 제한한다

안전한 BPF system은 **CFG/termination, register/stack initialization, pointer provenance, scalar bounds, helper/kfunc contracts, reference tracking, map lifetime, attachment/pinning versioning, ring-buffer ownership, JIT code lifetime**을 모두 통과해야 한다. 핵심은 kernel에서 code를 실행하는 유연성이 아니라 그 code가 허용된 memory와 capability만 사용한다는 증명을 load 시점에 강제하는 데 있다.
