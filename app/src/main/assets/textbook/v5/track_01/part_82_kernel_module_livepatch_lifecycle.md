# PART 82 · Kernel Module and Livepatch Lifecycle — load, relocation, references, transition consistency, safe removal

Kernel code를 실행 중인 시스템에 추가하거나 교체하는 작업은 ordinary library loading보다 훨씬 강한 lifetime proof를 요구한다. Module code는 kernel privilege에서 interrupt·workqueue·RCU callback·device callback의 target이 될 수 있고, livepatch는 이미 stack 위에서 실행 중인 old function과 새 function이 일정 시간 공존하도록 만든다. 따라서 핵심은 `새 코드 주소를 등록했다`가 아니라 **어느 execution context가 어느 code generation을 실행할 수 있으며, old text/data를 언제 안전하게 제거할 수 있는가**다. Load·enable·transition·disable·remove를 하나의 state machine으로 보지 않으면 rare use-after-module, mixed semantic state, forced-transition corruption이 발생한다.

## CHAPTER 01 · Kernel module은 ELF file이 아니라 kernel address space에 들어오는 executable object다

Loadable module은 section·symbol·relocation metadata를 가진 object지만 kernel에 들어온 뒤에는 kernel text/data와 같은 privilege level에서 실행된다. Loader는 architecture-specific relocation을 적용하고 필요한 symbols를 resolve하며 executable/data permissions를 배치한 뒤 init routine을 호출한다. Userspace shared library failure가 한 process를 죽이는 수준이라면 module bug는 전체 kernel state를 손상시킬 수 있다. 따라서 parse·relocation·permission·version/signature validation은 convenience check가 아니라 **untrusted binary state가 kernel execution graph에 들어오기 전의 trust boundary**다.

## CHAPTER 02 · Module signature는 provenance를 검증하지만 runtime correctness를 증명하지 않는다

Kernel module signing facility는 module payload가 trusted key로 서명됐는지 kernel 내부에서 검증해 unauthorized code load를 막을 수 있다. Signature가 valid하다는 것은 binary가 승인된 signer에서 왔고 load 이후 변조되지 않았다는 뜻이지, 해당 module이 current kernel ABI와 semantic contract에 맞거나 race가 없다는 뜻은 아니다. Restrictive mode에서는 unsigned/unknown-key module을 거부하고 permissive mode에서는 taint와 함께 허용될 수 있다. 배포 정책은 **who may load code**와 **loaded code is correct**라는 두 검증을 분리해야 한다.

## CHAPTER 03 · Signed module은 signature 이후 binary mutation을 허용하지 않는 artifact contract를 가진다

Module signature는 ELF container 뒤에 append되는 payload까지 포함해 검증되므로 signing 후 strip이나 binary rewrite를 하면 signature mismatch가 발생할 수 있다. Build pipeline은 debug information 처리, compression, signing, packaging 순서를 명시해 release artifact bytes가 verifier가 본 bytes와 같게 해야 한다. 동일 private key를 여러 incompatible kernel configuration에 재사용한다면 module versioning/vermaging 같은 additional compatibility control이 필요하다. Artifact provenance는 source commit뿐 아니라 **exact final binary identity와 target kernel identity의 pair**로 관리해야 한다.

## CHAPTER 04 · Module loader의 symbol resolution은 exported kernel ABI에 의존한다

Module relocation이 참조하는 symbol은 current kernel이나 이미 load된 modules가 export한 namespace에서 찾아야 한다. Internal implementation symbol을 억지로 참조하면 kernel update에서 layout/name이 바뀌는 순간 binary compatibility가 깨진다. Symbol 이름이 존재해도 calling convention, structure layout, lifetime semantics가 달라졌다면 더 위험한 silent incompatibility가 된다. Out-of-tree module은 `컴파일됨`을 호환성 증거로 쓰지 말고 **target kernel build/config/version과 exported interface contract**를 함께 고정해야 한다.

## CHAPTER 05 · Relocation은 module의 link-time assumption을 runtime kernel address에 연결한다

Module text/data가 어느 virtual address에 배치될지는 load 시 결정될 수 있으므로 relocation entry는 symbol value와 instruction/data encoding 규칙을 이용해 runtime address를 채운다. Architecture에 따라 PC-relative range, trampoline, GOT-like data, special relocation type 제약이 달라질 수 있다. Relocation failure를 무시하거나 unsupported relocation을 잘못 적용하면 code가 load는 되지만 첫 실행에서 arbitrary address로 branch할 수 있다. Loader가 relocation을 검증한다는 것은 **binary control-flow/data-reference graph를 runtime kernel layout에 재구성하는 작업**이다.

## CHAPTER 06 · Module init 성공 전에는 external callers가 half-initialized state를 보면 안 된다

Module init routine은 data structures, workqueues, devices, sysfs/proc entries, callbacks를 등록하며 외부에서 module을 호출할 수 있는 entry points를 점진적으로 만든다. 초기화 중간에 failure가 나면 이미 등록된 앞 단계 resource를 역순으로 철회해야 한다. 더 중요한 문제는 callback을 publish한 뒤 callback이 사용하는 state가 아직 준비되지 않은 race다. 안전한 init은 **private state construction→all invariants ready→external publication** 순서를 유지하고, error path는 publication 여부를 기준으로 정확히 rollback해야 한다.

## CHAPTER 07 · Module reference count는 code text lifetime을 호출자 lifetime과 연결한다

Unloadable module의 code/data를 free하려면 어떤 CPU도 그 module code를 실행하거나 callback pointer를 들고 있지 않아야 한다. Module reference count는 device/file operation 같은 외부 object가 module implementation을 사용하는 동안 unload를 막는 대표 mechanism이다. `try_module_get()`은 removal이 시작된 module의 새 reference 획득을 거부할 수 있지만 caller가 module object 자체의 lifetime을 이미 안전하게 확보했다는 전제가 필요하다. Refcount는 단순 사용 통계가 아니라 **text/data reclaim 가능 시점을 결정하는 safety counter**다.

## CHAPTER 08 · Callback registration은 implicit module reference 또는 explicit teardown barrier를 요구한다

Timer, workqueue, notifier, IRQ handler, filesystem operation, network hook 등 callback pointer가 module text를 가리키면 callback registry가 module lifetime을 간접적으로 연장한다. Registry가 automatic module ref를 잡지 않는 경우 unload path가 먼저 unregister하고 in-flight callbacks가 완전히 끝날 때까지 synchronize해야 한다. `unregister()` return이 callback drain까지 보장하는지 API별로 다르므로 naming만 믿으면 안 된다. **Pointer를 더 이상 찾을 수 없음**과 **이미 pointer를 얻은 CPU가 실행을 끝냄**은 서로 다른 조건이다.

## CHAPTER 09 · Workqueue/timer teardown은 cancel과 running callback completion을 구분해야 한다

Module exit에서 queued work를 삭제했더라도 callback이 다른 CPU에서 이미 running이면 module text를 free할 수 없다. Synchronous cancel/flush primitive가 queued state와 executing state를 모두 drain하는지 확인하고, callback이 teardown thread가 보유한 lock/resource를 필요로 하는 cycle도 피해야 한다. Delayed work/timer가 callback 안에서 스스로 다시 queue하는 경우에는 먼저 requeue condition을 끄고 drain해야 한다. Teardown은 **new scheduling 차단→pending 제거→running completion→resource free** 순서를 가져야 한다.

## CHAPTER 10 · RCU callback과 read-side reference는 module unload를 grace period와 연결한다

Module callback pointer나 module-owned object가 RCU-protected structure에 publish되어 있다면 list에서 제거한 즉시 free할 수 없다. Existing readers가 old pointer를 이미 얻었을 수 있어 grace period 뒤에야 data 또는 code reference가 사라진다고 볼 수 있다. Callback function 자체가 module text에 있다면 queued RCU callback이 모두 끝나기 전 module unload는 use-after-text가 된다. Refcount와 RCU는 대체 관계가 아니라 **reference acquisition model과 deferred visibility를 각각 해결하는 lifetime layers**다.

## CHAPTER 11 · Force unload는 lifetime proof를 생략하는 debugging escape hatch일 뿐 정상 운영 계약이 아니다

Reference가 남아 있거나 module이 safe-unload 조건을 만족하지 않는데 강제로 제거하면 outstanding function pointer, in-flight work, device callback이 freed text/data를 실행할 수 있다. 즉 force는 `kernel이 알아서 정리해준다`가 아니라 proof를 우회해 system integrity를 포기하는 동작에 가깝다. Production automation에서 unload failure를 force로 재시도하면 intermittent crash를 확정적인 latent corruption으로 바꿀 수 있다. 안전한 정책은 blocker reference의 owner를 찾아 정상 quiesce한 뒤 제거하는 것이다.

## CHAPTER 12 · Livepatch는 old function을 덮어써 없애는 것이 아니라 old/new implementation을 일정 기간 공존시킨다

실행 중인 task stack에 old function이 올라가 있는데 text를 즉시 교체하면 return address·local invariant·lock protocol이 깨질 수 있다. Linux livepatch는 function entry redirection과 per-task transition state를 사용해 task가 safe point에 도달할 때 새 implementation으로 전환되도록 한다. Transition 동안 어떤 task는 old code, 다른 task는 new code를 실행할 수 있다. 따라서 patch correctness는 개별 function body뿐 아니라 **mixed-generation execution이 허용되는 semantic 범위**를 포함해야 한다.

## CHAPTER 13 · Self-contained fix와 cross-function semantic change는 livepatch 난도가 다르다

Bounds check 추가처럼 function의 external semantic을 바꾸지 않는 patch는 old/new task가 섞여도 비교적 안전할 수 있다. 반면 lock order, shared-structure meaning, state-machine invariant를 여러 function에서 동시에 바꾸는 fix는 한 task가 old helper와 new helper를 섞어 실행하면 incompatible state를 만들 수 있다. 이런 patch는 consistency unit을 더 크게 정의하고 transition point에서 관련 state가 neutral한지 확인해야 한다. `함수 단위 redirect 가능`은 `함수 단위 semantic transition 안전`과 같은 말이 아니다.

## CHAPTER 14 · Per-task consistency는 task별 code generation state를 추적한다

Livepatch transition에서 각 task는 patched/unpatched state를 가지며 safe하다고 판단되는 시점에 새 state로 이동한다. User task가 kernel/user boundary를 지나거나 sleeping task의 stack에 affected function이 없는 경우가 transition 기회가 될 수 있다. Fork된 child와 interrupt context의 state inheritance도 일관성 규칙에 포함된다. 따라서 system은 한 순간에 global bit 하나를 바꾸는 것이 아니라 **task population을 old generation에서 new generation으로 점진적으로 수렴시키는 distributed state transition**을 수행한다.

## CHAPTER 15 · Reliable stack trace는 affected function이 현재 실행 중인지 판정하기 위한 evidence다

Sleeping task를 patch state로 바꾸려면 그 task stack 어디에도 old affected function이 남아 있지 않다는 판단이 필요하다. Unwinder가 optimized frame, interrupt frame, architecture-specific stack을 정확히 복원하지 못하면 `safe하다`는 false negative가 old-code lifetime을 깨뜨릴 수 있다. 그래서 livepatch full support가 reliable stacktrace capability와 연결된다. Stack unwinding은 debugging convenience가 아니라 **runtime code replacement safety proof의 입력**이 될 수 있다.

## CHAPTER 16 · Kernel thread는 userspace boundary가 없으므로 explicit safe point가 필요할 수 있다

Ordinary user task는 syscall return 같은 kernel/user transition에서 patch state를 갱신할 수 있지만 영구 loop를 도는 kthread는 그런 boundary가 없을 수 있다. Workqueue/kthread worker처럼 generic loop가 well-defined idle point를 제공하면 그 위치가 safe transition point가 된다. Custom kthread loop는 locks를 풀고 transient shared state를 남기지 않은 지점을 직접 선정해야 한다. Patch transition이 끝나지 않는 kthread는 단순 관리 문제보다 **old semantic lifetime이 무기한 남아 있다는 correctness problem**이다.

## CHAPTER 17 · Transition sysfs state는 patch enable 요청과 실제 convergence 완료를 구분한다

Patch를 enable했다고 즉시 모든 task가 새 code를 실행하는 것은 아니다. `/sys/kernel/livepatch/<patch>/transition` 같은 state는 아직 old-state task가 남아 있는지를 보여주며, `/proc/<pid>/patch_state`는 blocker task 진단에 도움을 줄 수 있다. 운영 자동화는 module load 성공만 보고 rollout 완료를 선언하면 안 된다. **enabled, transition-in-progress, fully converged**를 서로 다른 deployment states로 기록해야 rollback과 다음 patch sequencing을 안전하게 할 수 있다.

## CHAPTER 18 · Stuck transition은 task를 깨우거나 signal해 safe point 도달을 유도해야 할 수 있다

Sleeping kthread나 장시간 kernel loop가 affected old function state를 유지하면 transition이 무기한 끝나지 않을 수 있다. 관리자는 blocker stack을 확인하고 task를 wake/signal해 safe point로 진행시키는 방법을 검토할 수 있다. 무조건 force하는 것은 old stack 존재 가능성을 숨기므로 먼저 workload-specific quiescence를 만드는 편이 안전하다. Transition latency는 patch framework 속도보다 **현재 task execution topology와 affected function residency**에 의해 결정된다.

## CHAPTER 19 · Force transition은 future patchability까지 손상시키는 비가역적 위험을 가진다

Livepatch force mechanism은 task state를 강제로 바꿀 수 있지만 old patched function 안에 sleeping task가 없는지 더 이상 보장할 수 없다. 그래서 patch module removal을 안전하게 증명할 수 없고 reference를 영구 유지해야 할 수 있으며, 이후 patch 적용도 위험해질 수 있다. Force는 `transition timeout 해결`이 아니라 system consistency proof를 의도적으로 깨는 emergency option이다. 사용했다면 reboot를 계획하고 추가 livepatch를 이어 적용하지 않는 보수적 운영이 필요하다.

## CHAPTER 20 · ftrace redirection은 function entry에서 implementation 선택을 수행한다

Livepatch는 dynamic ftrace 기반 handler를 이용해 patched function entry에서 old/new implementation으로 control flow를 redirect할 수 있다. 같은 function에 여러 cumulative patches가 쌓일 수 있어 handler는 function stack에서 현재 task state에 맞는 implementation을 선택한다. Entry redirection이 가능한 함수인지, compiler-generated prologue와 tracing instrumentation이 호환되는지 architecture/toolchain support가 중요하다. Runtime patch는 arbitrary instruction overwrite가 아니라 **instrumented function boundary를 이용한 controlled dispatch layer**다.

## CHAPTER 21 · Livepatch module의 special relocation은 original kernel의 non-exported context를 연결할 수 있다

Patch implementation이 original source file의 local symbol/data에 접근해야 할 때 ordinary module exported-symbol mechanism만으로는 충분하지 않을 수 있다. Livepatch ELF format은 target object와 symbol 위치를 표현하는 relocation metadata를 사용해 runtime에 주소를 해결한다. 동일 이름 symbol이 여러 개이면 object와 symbol position을 정확히 구분해야 한다. 잘못된 relocation은 patch code가 전혀 다른 kernel state를 읽게 만들므로 **symbol identity도 patch provenance의 일부**다.

## CHAPTER 22 · Patched module이 나중에 load되는 경우 livepatch object lifecycle이 다시 움직인다

Livepatch가 vmlinux뿐 아니라 특정 kernel module의 function을 patch할 수 있고, target module이 patch보다 나중에 load될 수도 있다. Framework는 target object가 나타날 때 symbol을 resolve하고 patch를 적용해야 하며, target module unload와도 lifetime을 맞춰야 한다. Patch state가 존재한다는 이유만으로 아직 존재하지 않는 module code address를 고정할 수 없다. **Patch object lifecycle과 target module lifecycle은 독립 state machine이면서 rendezvous point에서 결합**된다.

## CHAPTER 23 · Pre/post patch callbacks은 semantic state migration을 위한 transaction hook이다

Function body만 바꾸는 것으로 부족하고 shared data structure를 초기화하거나 state version을 바꿔야 하는 patch는 pre/post callbacks를 사용할 수 있다. Callback이 일부 object에서 성공한 뒤 다른 object에서 실패하면 rollback semantics가 명확해야 하며, transition 중 old/new task가 보는 state도 안전해야 한다. Callback에서 arbitrary long work나 unknown lock을 잡으면 patch control path 자체가 deadlock할 수 있다. Data migration은 code redirect와 **동일 transaction boundary 안에서 실패/복구 규칙**을 가져야 한다.

## CHAPTER 24 · Shadow variable은 old structure layout을 깨지 않고 patch-specific state를 붙인다

Live system에서 기존 object layout을 바꾸면 이미 allocation된 object와 old code가 새 field를 이해하지 못한다. Livepatch shadow variable mechanism은 original object pointer + id에 별도 data를 연결해 structure ABI를 즉시 변경하지 않고 new state를 추가할 수 있다. Shadow data도 allocation/lookup/destruction lifetime과 concurrent access ordering을 가져야 하며, 다음 cumulative patch가 state version을 이어받는 방법을 정의해야 한다. 이것은 **data schema migration을 side table로 수행하는 runtime compatibility technique**다.

## CHAPTER 25 · Cumulative patch는 active patch stack 복잡도를 줄이는 운영 모델이다

여러 independent patch를 계속 stacking하면 한 function에 여러 replacement와 state dependencies가 쌓여 어떤 combination이 현재 활성인지 추적하기 어려워진다. Cumulative patch는 이전 fixes를 포함한 새 patch가 old patches를 replace하도록 해 active semantic baseline을 단순화한다. Replace transition이 완전히 끝난 뒤에야 old patch function entries와 handlers를 제거할 수 있다. `latest patch 설치`와 `old patch safely reclaim` 사이에는 여전히 convergence barrier가 존재한다.

## CHAPTER 26 · Disable도 enable의 역방향 consistency transition이지 즉시 원복이 아니다

Patch를 disable하면 task population은 patched state에서 previous/original state로 다시 수렴해야 한다. New implementation stack frame이 살아 있는 task를 즉시 old function으로 돌리면 enable 때와 같은 mixed-semantic 문제를 만든다. Transition이 끝난 뒤 function stack entry와 ftrace handler를 제거하고 sysfs state를 정리할 수 있다. Rollback 버튼을 `즉시 old code`로 구현하면 livepatch consistency model을 무시하는 것이다.

## CHAPTER 27 · Patch module 제거는 모든 task가 해당 module text를 더 이상 실행하지 않는다는 proof가 필요하다

Disabled 상태라도 force가 사용됐거나 transition이 완전히 끝나지 않았다면 task가 patch module의 function 안에서 sleep 중일 가능성을 배제할 수 없다. Module text를 unload하면 그런 task의 return/instruction fetch가 freed address를 사용한다. Safe remove는 **transition completed + no live function users + callbacks/state cleanup completed**를 만족해야 한다. Livepatch에서 module refcount가 영구적으로 남는 상황은 leak라기보다 proof를 잃어서 reclaim을 금지한 안전조치일 수 있다.

## CHAPTER 28 · Observability는 patch version보다 task별 transition state와 blocker stack을 보여줘야 한다

운영 화면에 `patch v3 enabled`만 표시하면 실제로 수십 task가 old implementation을 쓰는 transition 상태를 숨길 수 있다. Patch enabled/transition flag, per-task patch state, blocker PID/kthread stack, affected function list, target module presence, cumulative replace graph를 함께 기록해야 한다. Transition duration distribution과 stuck reason을 release마다 비교하면 특정 kthread safe point나 unwind support 문제가 드러난다. **Runtime code generation이 둘 이상 공존하는 동안은 fleet version label 하나로 상태를 표현할 수 없다.**

## CHAPTER 29 · Livepatch 테스트는 function output뿐 아니라 transition interleaving을 검증해야 한다

Unit test가 new function 결과만 확인하면 old/new coexistence 동안 발생하는 lock/data invariant bug를 잡지 못한다. Stress test는 affected function 안에 task를 의도적으로 멈춰두고 patch enable/disable, fork, interrupt, kthread wake, target module load/unload를 교차시켜야 한다. Data migration patch는 old task와 new task가 동일 shared object를 순차/동시에 만질 때 invariant가 유지되는지 확인한다. Fault injection으로 relocation/callback/target-module failure가 생겼을 때 patch가 clean rollback되는지도 검증해야 한다.

## CHAPTER 30 · Runtime kernel code replacement는 code lifetime과 semantic generation을 함께 증명해야 한다

Module/livepatch 변경을 승인하려면 `누가 binary provenance를 검증했는가`, `symbols/relocations가 어느 exact kernel object에 묶였는가`, `external callbacks가 어떤 module reference를 잡는가`, `old/new function이 공존해도 data/lock semantics가 안전한가`, `task가 safe generation으로 전환됐음을 어떻게 아는가`, `disable/remove 전에 어떤 grace/drain 조건을 기다리는가`를 답할 수 있어야 한다. Code address를 바꾸는 기술보다 **old code가 더 이상 실행될 수 없음을 증명하는 기술**이 더 중요하다. Runtime patch correctness는 text modification이 아니라 generation-aware lifetime protocol이다.
