# PART 96 · Transient Execution and Speculation Security — Spectre, branch predictors, RSB/BHI/ITS, buffer sampling, VM boundaries

현대 CPU의 성능은 “조건이 확정될 때까지 가만히 기다리는” 방식으로 나오지 않는다. Branch predictor는 다음 제어 흐름을 추정하고, out-of-order engine은 아직 retire할 수 없는 instructions도 미리 실행하며, cache·TLB·predictor·internal buffer 상태를 바꾼다. 잘못 추측한 instructions는 architectural register/memory state에서는 취소되지만, 그동안 남긴 microarchitectural 흔적까지 완전히 되돌아가는 것은 아니다. Spectre 계열과 여러 transient-execution 취약점은 바로 이 간극을 이용한다. 따라서 보안 경계는 permission check가 architectural하게 맞는지만 확인해서 끝나지 않는다. **공격자가 speculation을 조종해 secret-dependent microarchitectural state를 만들고 그것을 timing으로 읽을 수 있는지**까지 포함해야 한다.

## CHAPTER 01 · Architectural state와 microarchitectural state를 분리해야 transient execution이 보인다

ISA 관점에서 프로그램의 결과는 registers, memory, exceptions처럼 software가 관찰하도록 정의된 architectural state로 설명된다. CPU는 그 결과를 빠르게 만들기 위해 reorder buffer, branch predictor, caches, TLB, load/store queues 같은 microarchitectural structures를 사용한다. 잘못 예측한 branch 뒤 instructions는 retire되지 않으므로 architectural 결과에서는 “실행되지 않은 것”처럼 사라질 수 있다. 그러나 그 instructions가 cache line을 불러오거나 predictor history를 바꿨다면 시간 측정으로 그 흔적을 추론할 여지가 생긴다. 이것이 transient execution을 단순한 “CPU가 틀렸다가 고친다” 수준으로 설명하면 안 되는 이유다. **보안 분석에서는 architectural rollback과 microarchitectural rollback이 동일하지 않다는 사실을 기본 전제**로 둬야 한다.

## CHAPTER 02 · Branch prediction은 control dependency의 latency를 숨기지만 공격자에게 speculation 방향을 조작할 표면을 준다

Pipeline이 branch 조건 계산을 기다릴 때마다 멈추면 modern wide CPU의 실행 자원을 충분히 활용하기 어렵다. 그래서 predictor는 branch outcome과 target을 추정하고 frontend가 다음 instructions를 미리 공급한다. Predictor는 이전 실행 history에서 학습하므로 반복적인 입력으로 특정 방향을 강하게 예측하도록 training할 수 있다. 정상 workload에서는 높은 prediction accuracy가 성능을 만든다. 공격 관점에서는 victim이 bounds check를 거의 항상 통과하도록 history를 만든 뒤, 드문 out-of-bounds 입력에서 CPU가 잠시 이전 패턴대로 speculatively 진행하도록 유도하는 식의 조작이 가능해진다. **Predictor는 프로그램의 논리적 권한 검사를 바꾸지 않지만 검사 결과가 확정되기 전에 어떤 code가 잠시 실행될지를 바꿀 수 있다.**

## CHAPTER 03 · Mis-speculated instruction은 retire되지 않아도 cache side effect를 남길 수 있다

CPU가 branch 예측이 틀렸음을 알게 되면 잘못된 path의 register writes와 stores는 architectural commit에서 버린다. 그래서 debugger로 최종 registers를 보면 공격 code가 secret을 읽은 흔적이 없을 수 있다. 문제는 transient load가 특정 cache line을 가져온 뒤 rollback되어도 cache residency가 자동으로 원래 상태로 돌아간다고 보장되지 않는다는 점이다. 공격자는 secret 값에 따라 서로 다른 probe array entry를 touch하게 만들고, speculation이 끝난 뒤 어떤 entry가 빠르게 읽히는지 재어 secret을 간접 복원할 수 있다. 핵심 채널은 “잘못된 값이 architectural memory에 써졌다”가 아니라 **secret-dependent timing state가 남았다**는 것이다. 따라서 memory safety check와 side-channel resistance는 별도 보안 속성이다.

## CHAPTER 04 · Cache timing은 transient data를 직접 읽지 않고도 값에 대한 정보를 복원한다

Flush+Reload, Prime+Probe 같은 cache side-channel 기법은 특정 address가 cache에 있었는지 latency 차이를 이용해 추론한다. Transient gadget은 secret byte를 직접 attacker buffer에 store하지 않고 `probe[secret * stride]` 같은 access pattern으로 cache set/line 선택에 영향을 주기만 해도 된다. 이후 architectural execution으로 돌아온 attacker가 probe 영역의 access times를 측정하면 어느 entry가 빠른지 통계적으로 구분할 수 있다. Noise, prefetcher, scheduler migration 때문에 한 번의 측정이 완벽하지 않아도 반복과 통계로 signal을 키울 수 있다. **정보 유출 channel의 payload는 memory write가 아니라 timing distribution**이므로 “secret을 destination에 복사하지 않았다”는 코드 리뷰만으로는 방어가 되지 않는다.

## CHAPTER 05 · Spectre v1은 bounds check 자체를 우회하지 않고 bounds check의 결과가 확정되기 전 window를 악용한다

대표적인 bounds-check-bypass 형태에서는 코드가 `if (x < len) value = array[x];`처럼 논리적으로 올바른 검사를 갖고 있어도 predictor가 반복적으로 taken 방향을 학습한 뒤 attacker-controlled 큰 `x`가 들어오면 CPU가 잠시 body를 speculatively 실행할 수 있다. Architectural하게는 check failure가 확정되면 결과가 폐기되지만, body 안에서 secret-dependent cache access가 일어나면 흔적이 남는다. 이것은 일반적인 buffer overflow와 다르다. Out-of-bounds value가 정상 architectural load 결과로 프로그램에 전달되는 것이 아니라 transient window 안의 data flow가 side channel로 인코딩된다. **v1 방어는 source-level bounds check 존재 여부가 아니라 speculation window에서 secret-dependent gadget이 성립하는지를 봐야 한다.**

## CHAPTER 06 · v1 mitigation은 speculation-safe data dependency나 barrier를 필요한 지점에 배치해야 한다

Bounds check 뒤 모든 code에 무조건 heavyweight barrier를 넣으면 성능을 크게 잃는다. Kernel과 compiler는 attacker-controlled index가 speculative path에서 unsafe address를 만들지 못하도록 index masking, nospec helpers, architecture-specific speculation barriers 같은 수단을 사용한다. 중요한 것은 단순히 branch 뒤에 instruction 하나를 추가했다는 사실이 아니라 해당 CPU와 compiler에서 **secret load보다 앞서 speculation을 실제로 차단하거나 address dependency를 안전한 값에 묶는가**다. Optimizer가 source dependency를 제거하거나 다른 형태로 바꿀 가능성도 검토해야 한다. Gadget remediation은 code pattern, compiler output, target architecture를 함께 확인해야 하며 source만 보고 “barrier가 있으니 안전”이라고 단정하면 안 된다.

## CHAPTER 07 · Spectre v2는 indirect branch target prediction을 오염시켜 victim을 원치 않는 gadget으로 speculatively 보낸다

Indirect call/jump는 runtime target이 여러 곳일 수 있어 branch target predictor가 성능에 중요하다. Spectre v2 계열은 attacker가 predictor state를 조작해 victim indirect branch가 architectural target과 다른 gadget으로 transient하게 향하도록 만들고, 그 gadget에서 secret-dependent side effect를 생성하려 한다. 여기서는 bounds check 한 줄보다 control-flow prediction domain이 핵심이다. Kernel entry, context switch, VM exit처럼 trust domain이 바뀌는 경계에서 이전 domain의 predictor history가 다음 domain에 영향을 줄 수 있는지가 공격 표면이 된다. **v2 mitigation은 한 함수의 local correctness가 아니라 indirect prediction state를 trust boundary 사이에서 어떻게 격리·제한하는가의 문제**다.

## CHAPTER 08 · Predictor state는 software address space와 동일한 isolation unit을 갖는다고 가정하면 안 된다

Page tables는 process마다 virtual-to-physical translation과 permission을 분리하지만 branch predictor의 내부 indexing/tagging은 software가 생각하는 PID나 VM identity와 정확히 일치하는 isolation을 자동 제공하지 않을 수 있다. 그래서 attacker와 victim이 시간적으로 같은 logical CPU를 사용하거나 SMT siblings로 물리 core를 공유할 때 predictor influence가 trust boundary를 넘는 공격이 문제된다. Hardware 세대에 따라 prediction domain control이 강화되지만 “새 CPU면 predictor state가 완전히 private”라고 일반화할 수 없다. Kernel mitigation은 CPU capability bits와 microcode behavior를 바탕으로 IBRS, IBPB, retpoline, BHB/RSB handling 등을 조합한다. **Protection unit은 process가 아니라 실제 hardware prediction domain과 context transition semantics로 판단**해야 한다.

## CHAPTER 09 · Retpoline은 indirect branch speculation을 return-based trap sequence 안에 가두는 software technique다

Retpoline은 indirect branch를 특수한 call/return sequence로 변환해 speculative execution이 attacker-chosen indirect target으로 흘러가지 않도록 설계된 software mitigation이다. Architectural execution은 실제 target으로 이동하지만 speculative path는 안전한 loop/thunk에 머물게 하는 발상이다. 그러나 retpoline 효과는 CPU의 return prediction behavior와 관련이 있어 모든 processor/새 변형에 영구 만능인 것은 아니다. RSB underflow나 Retbleed, BHI/ITS 같은 후속 연구는 return과 indirect prediction 사이 상호작용을 더 세밀하게 다루게 했다. **Mitigation primitive도 공격 모델의 일부이며 CPU capability와 최신 vulnerability class에 맞춰 유효성을 재평가**해야 한다.

## CHAPTER 10 · IBRS와 enhanced IBRS는 privilege/domain 전환에서 indirect prediction 사용을 hardware control로 제한한다

Indirect Branch Restricted Speculation 계열은 software가 predictor를 완전히 비우기보다 hardware에 현재 execution domain의 indirect prediction 제약을 요청하는 방향의 mitigation이다. Enhanced IBRS가 제공되는 CPU에서는 kernel이 이를 이용해 user-to-kernel, guest-to-host 같은 경계를 보호할 수 있고 retpoline과 선택 관계가 생길 수 있다. 하지만 “IBRS on = 모든 speculation attack 해결”은 틀리다. Intra-mode branch history, RSB behavior, v1 data speculation, MDS처럼 다른 microarchitectural buffer 문제는 별도 mitigation을 요구할 수 있다. **Mitigation status는 단일 boolean이 아니라 vulnerability class별 control의 조합**으로 읽어야 한다. Kernel sysfs status가 여러 취약점 파일로 분리되어 있는 이유도 이 때문이다.

## CHAPTER 11 · IBPB는 context boundary에서 이전 execution이 후속 indirect prediction을 지배하지 못하도록 barrier 역할을 한다

Indirect Branch Predictor Barrier는 동일 logical CPU에서 barrier 전 software가 만든 predictor influence가 barrier 뒤 software의 indirect targets를 제어하지 못하도록 하는 경계 primitive다. 모든 context switch에서 항상 실행하면 비용이 커질 수 있어 kernel은 trust relationship과 per-task control에 따라 conditional policy를 사용할 수 있다. User-to-user isolation이 필요한 sandbox나 untrusted tenant는 같은 uid/process group이라고 해서 predictor trust가 자동 성립하지 않을 수 있다. Scheduler와 security policy가 “어떤 tasks 사이에 IBPB가 필요한가”를 결정하는 이유다. **Context switch는 register/page-table switch만이 아니라 microarchitectural history handoff 경계**이며, barrier 비용과 threat model을 함께 설계해야 한다.

## CHAPTER 12 · STIBP는 SMT sibling이 indirect branch prediction에 영향을 주는 cross-thread 표면을 줄이기 위한 control이다

Simultaneous multithreading에서는 두 logical CPUs가 같은 physical core의 일부 microarchitectural resources를 공유한다. Single Thread Indirect Branch Predictors control은 한 sibling의 indirect prediction이 다른 sibling에 미치는 영향을 제한하는 데 사용될 수 있다. 그러나 SMT mitigation은 성능 비용이 workload에 따라 크고, 취약점마다 STIBP만으로 full protection이 되는 것도 아니다. 일부 classes는 sibling에서 공격자가 동시에 실행되지 않도록 SMT를 끄거나 core scheduling으로 trust group을 맞추는 추가 정책이 필요하다. **Logical CPU isolation과 physical core isolation을 동일하게 취급하면 cross-thread attack model을 놓친다.** Scheduler placement가 security control이 되는 이유다.

## CHAPTER 13 · Return Stack Buffer는 return target prediction을 빠르게 하지만 underflow와 cross-domain state가 공격 표면이 될 수 있다

CPU는 call/return pairing을 빠르게 예측하기 위해 Return Stack Buffer 같은 structure를 사용한다. 정상적으로 깊이가 맞으면 return target prediction이 효율적이지만 unbalanced returns, context transitions, underflow 상황에서 alternate predictor behavior가 나타날 수 있다. Modern kernel은 RSB stuffing이나 call-depth tracking 같은 기법으로 trust boundary에서 return predictions를 안전한 상태로 만들려 한다. Virtualization에서는 guest/host 전환도 RSB domain boundary가 된다. **Return instruction이 “stack에서 주소를 pop하니 branch target injection과 무관하다”는 단순 모델은 충분하지 않다.** Speculative target은 architectural stack read보다 먼저 predictor에 의해 선택될 수 있다.

## CHAPTER 14 · Retbleed는 retpoline이 의존한 return prediction assumptions까지 공격 모델에 포함시켰다

Retbleed 계열은 일부 CPUs에서 return prediction이 RSB에만 머물지 않고 다른 branch prediction machinery의 영향을 받을 수 있는 조건을 악용한다. 이는 “indirect branch를 return으로 바꾸면 항상 안전하다”는 초기 retpoline 직관을 CPU별로 재검토하게 만들었다. Kernel은 affected hardware에 맞춰 untraining, safe return thunk, call depth tracking 등 별도 mitigation path를 선택할 수 있다. 운영자가 과거 boot parameter를 영구 고정해두고 kernel/microcode를 업그레이드하지 않으면 새 attack model에 맞는 mitigation을 놓칠 수 있다. **Speculation security는 static checklist가 아니라 CPU model·microcode·kernel version이 함께 움직이는 compatibility problem**이다.

## CHAPTER 15 · Branch History Injection은 privileged target restriction이 있어도 branch history 자체가 gadget 선택에 영향을 줄 수 있음을 보여준다

Enhanced IBRS 같은 target-domain control이 있어도 branch history가 intra-domain prediction에 영향을 주는 방식이 남아 있다면 attacker가 history를 조작해 privileged domain 내부의 유용한 gadget으로 speculation을 유도할 가능성이 생긴다. BHI mitigation은 branch history buffer clearing이나 hardware controls, software sequences를 kernel entry에 배치하는 식으로 발전해 왔다. 이 문제는 “attacker가 kernel address를 predictor에 직접 넣는다”는 초기 v2 설명보다 더 복잡하다. **Target privilege restriction과 predictor history isolation은 서로 다른 보안 속성**이다. 최신 kernel status를 보는 것이 오래된 “eIBRS 지원” 한 줄만 보는 것보다 중요한 이유다.

## CHAPTER 16 · Indirect Target Selection은 최신 CPU에서도 indirect branch/return placement 자체가 새로운 predictor aliasing 표면을 만들 수 있음을 보여준다

최근 kernel documentation은 Indirect Target Selection 취약점을 별도 class로 다루며 affected returns/indirect branches를 safe aligned thunk로 옮기거나 retpoline/RSB stuffing과 결합하는 mitigation을 설명한다. eIBRS가 있다고 해서 모든 guest/host 또는 intra-mode case가 자동 해결되는 것은 아니며 VMM이 guest capability를 어떻게 expose하는지도 중요하다. Mitigation은 instruction placement와 i-cache/iTLB footprint를 바꿀 수 있어 성능 비용도 생긴다. **Binary layout은 단순 성능 최적화 대상이 아니라 predictor security property에 영향을 주는 요소**가 될 수 있다. Kernel/toolchain update가 security mitigation의 일부인 이유다.

## CHAPTER 17 · Speculative Store Bypass는 older load가 아직 확정되지 않은 store dependency를 잘못 추측하는 data-speculation 문제다

CPU는 load가 이전 store와 alias하지 않는다고 예측해 먼저 실행함으로써 memory dependency latency를 줄일 수 있다. 예측이 틀리면 architectural state는 복구되지만 transient load가 stale data를 사용해 side-channel gadget으로 이어질 수 있다. 이것은 branch target injection과 다른 speculation dimension이다. Linux는 일부 workloads에서 expensive mitigation을 per-task `PR_SET_SPECULATION_CTRL`로 선택할 수 있도록 한다. 모든 process에 강제로 비용을 부과하기보다 untrusted code를 실행하는 tasks에 제한하는 전략이 가능하지만 inheritance/exec semantics를 알아야 한다. **Speculation control은 branch뿐 아니라 memory dependency predictor까지 포함**한다.

## CHAPTER 18 · PR_SET_SPECULATION_CTRL은 일부 mitigation을 task 단위로 조절하지만 application security policy와 lifecycle을 함께 설계해야 한다

Kernel userspace API는 store bypass, indirect branch speculation, L1D flush 같은 controls 중 지원되는 항목을 `PR_GET_SPECULATION_CTRL`로 조회하고 `PR_SET_SPECULATION_CTRL`로 enable/disable/force-disable할 수 있게 한다. `PR_SPEC_FORCE_DISABLE`은 후속 enable을 막는 더 강한 상태이고, `PR_SPEC_DISABLE_NOEXEC`처럼 exec에서 상태를 바꾸는 mode도 있다. Library가 process 중간에서 control을 바꾸면 sibling threads나 exec child에 어떤 정책이 적용되는지 threat model과 맞춰야 한다. **Per-task knob은 feature flag가 아니라 trust-level transition**이다. Sandbox entry 전에 강화하고 untrusted code가 다시 완화할 수 없는지 확인해야 한다.

## CHAPTER 19 · MDS는 attacker가 정확한 target address를 선택하기보다 CPU internal buffers에서 transient data를 sampling하는 class다

Microarchitectural Data Sampling은 fill buffer, load port, store buffer 같은 CPU 내부 temporary structures의 data가 특정 fault/assist 상황에서 unrelated speculative load에 전달될 수 있는 문제를 묶는다. Architectural load 결과는 fault로 폐기되더라도 그 transient value가 disclosure gadget을 통해 cache timing에 인코딩될 수 있다. Spectre v1/v2처럼 victim branch를 정확히 조종하는 모델과 달리 sampling 후 통계적 postprocessing이 중요할 수 있다. Linux mitigation은 affected CPU에서 user return/guest entry 시 buffer clear를 수행하고 SMT 상태에 따라 full protection 조건이 달라진다. **Microarchitectural buffer도 trust domain 사이에 stale data를 운반할 수 있는 storage**로 봐야 한다.

## CHAPTER 20 · L1 Terminal Fault는 invalid page-table entry와 L1D contents의 상호작용이 privilege/VM 경계를 흔들 수 있음을 보여준다

L1TF는 terminal page-table fault가 architectural하게 access를 막더라도 transient execution이 L1 data cache의 잘못된 physical target과 연결될 수 있는 class로 알려졌다. Host/guest virtualization에서 특히 위험한 이유는 shared physical core와 L1D가 서로 다른 trust domains 사이 channel이 될 수 있기 때문이다. Mitigation에는 PTE sanitization, L1D flush, SMT policy 등이 CPU/virtualization scenario에 따라 결합된다. Guest가 자기 내부를 보호하는 문제와 host가 guest로부터 보호되는 문제도 동일하지 않다. **Page-table permission correctness만으로 speculative cache access가 모두 설명된다고 가정하면 안 된다.** Hardware fault path 자체가 transient window를 가질 수 있다.

## CHAPTER 21 · TAA와 유사 sampling 취약점은 mitigation primitive가 여러 vulnerability에 공유될 수 있음을 보여준다

TSX Asynchronous Abort 같은 취약점은 transactional execution abort window와 microarchitectural buffers를 이용한 data sampling 문제와 연결된다. 특정 processors에서는 MDS와 같은 buffer-clearing primitive가 mitigation에 사용되므로 kernel command-line options가 서로 독립 toggle처럼 보이면서 실제로는 같은 hardware sequence를 공유할 수 있다. 그래서 한 mitigation을 off로 요청해도 다른 vulnerability mitigation이 같은 primitive를 필요로 하면 완전히 사라지지 않을 수 있다. **Configuration surface와 underlying mitigation mechanism을 분리해서 이해**해야 운영자가 “옵션 하나 껐으니 비용이 0”이라고 잘못 판단하지 않는다. Kernel status 파일을 실제로 확인해야 한다.

## CHAPTER 22 · CPU vulnerability landscape는 한 번의 Spectre/Meltdown 목록으로 고정되지 않고 microarchitectural state class별로 계속 확장된다

Kernel hardware-vulnerability documentation은 Spectre, L1TF, MDS 외에도 MMIO stale data, GDS, RFDS, RSB-related issues, SRSO, ITS, VMSCAPE 등 다양한 classes를 별도 문서로 관리한다. 공통점은 architectural permission과 결과만 검사해서는 포착되지 않는 speculative/shared-state leakage가 존재할 수 있다는 점이지만 affected vendors와 mitigation은 서로 다르다. 따라서 product security 문서가 “Spectre mitigated” 한 줄로 끝나면 시간이 지나며 의미를 잃는다. **Asset inventory에는 CPU model, microcode revision, kernel mitigation status, SMT policy를 포함하고 vulnerability class별 exposure를 재평가**해야 한다. 새로운 class가 나오면 전체 threat model을 갱신할 수 있어야 한다.

## CHAPTER 23 · SMT는 두 trust domains가 같은 physical core의 microarchitectural resources를 동시에 사용하는 문제를 만든다

SMT siblings는 architectural register state는 분리되지만 execution units, caches, predictors, buffers 중 일부를 공유할 수 있다. MDS/L1TF처럼 cross-thread attack이 가능한 vulnerability에서는 sibling attacker가 victim과 동시에 실행하는 것이 핵심 vector가 될 수 있다. Full mitigation이 SMT disable을 요구하는 경우 성능/throughput 비용이 매우 클 수 있어 운영자는 trusted code만 실행하는지, multi-tenant isolation이 필요한지에 따라 정책을 달리한다. **Security scheduling unit을 logical CPU가 아니라 physical core로 올려야 하는 workload가 존재**한다. Kubernetes/VM scheduler 같은 상위 계층도 placement 정책을 알아야 한다.

## CHAPTER 24 · Core scheduling은 SMT를 유지하면서 trusted groups끼리만 sibling을 공유하게 하지만 모든 cross-thread 취약점의 만능 해법은 아니다

Linux core scheduling은 userspace가 tasks를 trust groups로 묶어 같은 physical core sibling에는 compatible group만 동시에 배치하도록 할 수 있다. 이는 SMT를 완전히 끄는 것보다 resource utilization을 보존하면서 일부 cross-HT attacks를 완화하는 데 도움이 된다. 하지만 kernel documentation도 모든 cross-thread vulnerability를 해결한다고 주장하지 않으며 hardware/mitigation class별 제약이 남는다. Scheduler policy가 깨지거나 group tagging이 잘못되면 isolation assumption도 깨진다. **Core scheduling은 trust labeling과 scheduler enforcement가 결합된 policy layer**이며, CPU hardware mitigation을 대체한다고 보면 안 된다.

## CHAPTER 25 · Virtualization에서는 user/kernel 외에 guest/host와 guest/guest prediction·buffer boundaries를 따로 모델링해야 한다

Hypervisor는 context switch보다 더 강한 privilege boundary를 제공하지만 CPU core의 predictor/cache/buffer state까지 VM별로 자동 분리되는 것은 아니다. VM entry/exit에서 필요한 IBRS/IBPB, buffer clear, L1D handling은 vulnerability와 hardware capability에 따라 달라진다. Host가 최신 microcode/kernel을 적용해도 guest 내부 user/kernel 보호는 guest OS가 자체 mitigation을 해야 할 수 있다. 반대로 guest에 capability bits가 숨겨지거나 가상화되어 보이면 guest가 불필요하거나 부족한 mitigation을 선택할 수도 있다. **Virtualization security는 host, VMM, guest kernel의 mitigation contract가 맞아야 완성**된다. Cloud 운영자는 tenant threat model에 맞는 vCPU/SMT placement도 고려해야 한다.

## CHAPTER 26 · `/sys/devices/system/cpu/vulnerabilities/*`는 현재 kernel이 인식한 affected/mitigation 상태를 운영 evidence로 제공한다

Linux는 spectre_v1, spectre_v2, mds 등 vulnerability별 sysfs files에서 `Not affected`, `Vulnerable`, `Mitigation: ...` 형태의 현재 상태를 노출한다. 이 문자열은 CPU capability, microcode, boot options, kernel mitigation 선택을 반영하므로 hardware 모델명만 보고 상태를 추정하는 것보다 낫다. 그러나 한 번 수집한 값이 영구 진실은 아니다. BIOS/microcode/kernel/boot option이 바뀌면 재확인이 필요하다. Fleet observability는 **host identity + kernel version + microcode + vulnerability status snapshot**을 함께 저장하고 drift를 탐지해야 한다. VM에서는 host 상태를 완전히 알 수 없는 항목도 명시적으로 unknown으로 다뤄야 한다.

## CHAPTER 27 · 최신 kernel attack-vector controls는 vulnerability 이름 대신 실제 trust boundary를 중심으로 mitigation policy를 구성한다

Current kernel documentation은 User-to-Kernel, User-to-User, Guest-to-Host, Guest-to-Guest, Cross-Thread 같은 attack-vector sets를 기준으로 관련 mitigations를 묶어 제어하는 방법을 제공한다. 새 vulnerability가 추가되면 해당 vector set에 포함될 수 있어 운영자가 취약점마다 수십 개 boot knobs를 수동 관리하는 부담을 줄인다. 반대로 특정 vector를 disable하면 그 경계에 대한 여러 mitigations가 함께 빠질 수 있으므로 “성능 때문에 하나 껐다”보다 큰 exposure가 생길 수 있다. **Mitigation policy를 CPU bug 이름이 아니라 실제로 신뢰하지 않는 주체들이 어떤 경계를 공유하는지에서 시작**하는 접근이 더 지속 가능하다.

## CHAPTER 28 · Mitigation 비용은 syscall, context switch, VM exit, branch-heavy code, SMT utilization에 서로 다르게 나타난다

Retpoline은 indirect branch path에 instruction overhead를 추가하고, IBPB는 context boundary 비용을 만들며, buffer clear와 L1D flush는 transition latency를 늘릴 수 있다. SMT disable은 코어당 runnable throughput을 직접 줄이고 core scheduling은 placement flexibility를 제한한다. 따라서 benchmark 하나의 평균 slowdown으로 “mitigation 비용은 5%”처럼 일반화하면 안 된다. Web server, HPC, database, VM host는 hotspot이 다르다. **Security configuration A/B는 throughput뿐 아니라 p99 syscall/VM-exit latency, context switches, branch misses, CPU utilization, tenant interference를 함께 측정**해야 한다. 비용 측정은 실제 threat model을 낮추는 근거가 아니라 선택의 결과를 이해하는 자료다.

## CHAPTER 29 · Compiler와 kernel source에서 speculation gadget은 ordinary data-flow review보다 넓은 범위를 본다

Architectural execution에서는 unreachable해 보이는 code path도 predictor가 transient하게 들어갈 수 있고, bounds check 뒤 load가 secret-dependent probe access로 이어지면 gadget이 된다. Compiler optimization은 branches를 conditional move로 바꾸거나 code layout을 재배치해 source-level 직관과 다른 speculation surface를 만들 수 있다. Kernel의 nospec helpers와 annotations는 이런 patterns를 architecture-aware하게 처리하려는 infrastructure다. **Gadget review는 source logic, generated assembly, attacker-controlled inputs, side-channel sink를 함께 추적**해야 한다. 단순 taint analysis에 “bounds check 있음” 규칙만 추가해서는 부족하다.

## CHAPTER 30 · Sandboxes와 multi-tenant runtimes는 같은 privilege level 내부의 user-to-user speculation boundary를 별도로 다뤄야 한다

서로 다른 untrusted plugins가 모두 user mode로 실행되더라도 process isolation, JIT sandbox, VM/container policy가 secret separation을 기대할 수 있다. Spectre_v2_user나 SSB처럼 user-to-user attack vector가 관련되면 kernel entry protection만으로 충분하지 않을 수 있다. Context switch predictor controls, per-task speculation settings, process isolation, timer precision policy가 함께 고려된다. Browser/JIT 환경은 attacker가 high-resolution timing과 code generation을 함께 사용할 수 있어 software sandbox 설계와 CPU mitigation이 상호의존적이다. **Ring level이 같다는 이유로 trust level도 같다고 가정하지 않는 것**이 현대 multi-tenant runtime의 핵심이다.

## CHAPTER 31 · JIT은 공격자가 predictor training과 disclosure gadget 모양을 더 정밀하게 만들 수 있는 환경이 될 수 있다

JIT runtime은 attacker-controlled high-level code를 native instruction sequences로 변환할 수 있고, array bounds checks와 typed memory access를 대량 생성한다. Sandbox는 architectural bounds를 정확히 강제해도 speculative window에서 secret-dependent access가 생기는지 별도 검토해야 한다. JIT compiler는 target CPU별 speculation barriers, index masking, code layout hardening을 적용할 수 있지만 지나친 barrier는 성능을 크게 떨어뜨린다. Tiered compilation/deoptimization 때문에 code shape가 runtime 중 바뀌는 점도 gadget auditing을 어렵게 한다. **JIT security는 language-level sandbox proof와 machine-level speculative data-flow proof를 연결**해야 한다. P43의 deoptimization/code generation lifetime과도 이어진다.

## CHAPTER 32 · Constant-time coding만으로 모든 transient-execution 위험이 자동 해결되지는 않는다

Cryptographic constant-time code는 secret에 따른 branch와 memory access 차이를 줄여 classic timing/cache side channel을 막는 데 중요하다. 그러나 주변 bounds checks, caller/callee gadgets, speculative misprediction으로 정상 architectural path에는 없는 secret-dependent access가 생기면 별도 문제가 된다. 반대로 speculation mitigation을 켰다고 algorithm-level timing leak이 사라지는 것도 아니다. **Constant-time discipline과 speculative-execution mitigation은 서로 보완적인 층**이다. Crypto library는 compiler output과 CPU target을 검증하고, host kernel mitigation 상태도 threat model에 포함해야 한다. 한 층을 다른 층의 대체재로 쓰면 안 된다.

## CHAPTER 33 · PMU와 tracing은 mitigation 비용과 branch behavior를 볼 수 있지만 secret leakage를 직접 증명하는 oracle은 아니다

`perf` counters로 branch misses, cycles, cache misses, context switches를 측정하면 mitigation 적용 전후 성능 변화를 분석할 수 있다. Tracepoints와 scheduler events를 함께 보면 IBPB-like context policy나 SMT placement가 workload latency와 어떻게 연결되는지 추론하는 데 도움이 된다. 하지만 counter가 정상이라고 side-channel safety가 증명되는 것은 아니다. PMU access 자체가 untrusted tenants에게 side-channel resolution을 제공할 수 있어 virtualization/container에서 권한 제한이 필요할 수 있다. **Observability 도구도 microarchitectural information channel의 일부**가 될 수 있으므로 누가 counters를 읽을 수 있는지와 어떤 data를 노출하는지 정책이 필요하다.

## CHAPTER 34 · Microcode, firmware, kernel, compiler가 같은 mitigation story를 공유해야 하며 한 층만 최신이라고 충분하지 않다

일부 controls와 buffer-clearing instructions는 microcode update가 capability bits와 mitigation primitive를 제공해야 kernel이 안전하게 사용할 수 있다. Kernel은 그 capabilities를 감지해 thunk/barrier/policy를 선택하고, compiler는 retpoline/nospec/code-layout support를 제공한다. Hypervisor는 guest에 올바른 virtual capabilities를 노출해야 한다. 그래서 fleet patching은 package version 하나가 아니라 BIOS/microcode/kernel/toolchain/container image의 조합 문제다. **Mitigation readiness를 component version matrix로 관리하고 reboot-required update가 실제 boot state에 반영됐는지 sysfs로 확인**해야 한다. “패키지 설치 성공”을 “CPU mitigation 활성”과 동일시하면 안 된다.

## CHAPTER 35 · Speculation-security fault injection은 실제 secret extraction exploit 대신 isolation assumptions와 mitigation state를 안전하게 검증할 수 있다

Production에서 공격 exploit을 그대로 돌리는 것이 유일한 검증 방법은 아니다. CI/lab에서는 vulnerability sysfs 상태, expected boot controls, SMT/core scheduling policy, per-task speculation-control inheritance를 자동 검사하고, synthetic branch-heavy benchmark로 성능 regression도 측정할 수 있다. JIT/nospec code는 compiler output에 required barriers/thunks가 존재하는지 binary audit를 수행한다. VM 환경에서는 host/guest capability exposure와 migration 후 상태를 확인한다. **CLEAN은 “취약점 scanner green” 한 줄이 아니라 configuration, generated code, scheduler placement, runtime status를 서로 다른 failure class로 검증**해야 한다. 실제 exploit 테스트가 필요한 경우 격리된 승인된 lab에서만 수행한다.

## CHAPTER 36 · Transient-execution security의 최종 proof는 trust boundary마다 architectural permission과 microarchitectural influence를 함께 닫는 것이다

안전한 시스템은 다음을 답할 수 있어야 한다. Attacker가 어떤 branch/data predictor를 training할 수 있는가. 잘못 추측한 path가 어떤 secret을 transient하게 touch할 수 있는가. Cache/buffer/predictor state로 정보가 남는가. User→kernel, user→user, guest→host, guest→guest, cross-thread 중 어떤 vectors가 실제 존재하는가. CPU/microcode/kernel이 어떤 mitigation을 활성화했는가. RSB/BHI/ITS처럼 새 classes가 추가됐을 때 policy가 갱신되는가. SMT/core scheduling은 trust model과 맞는가. Per-task controls가 exec/fork 뒤에도 의도한 상태인가. 성능 비용을 실제 workload에서 측정했는가. **Architectural access control이 맞다는 사실과 transient side channel이 닫혔다는 사실을 둘 다 증명해야 현대 CPU의 보안 경계가 완성된다.**
