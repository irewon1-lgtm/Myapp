# PART 76 · Kernel Failure Capture — watchdogs, panic, kdump, vmcore, pstore, evidence survival

Kernel 장애에서 가장 어려운 점은 원인이 아니라 **증거 수집기 자체가 같은 kernel·scheduler·filesystem·storage path에 의존한다는 사실**이다. CPU가 hard lockup되거나 memory allocator가 망가지고 filesystem이 deadlock되면 평소의 logger·daemon·disk write를 믿을 수 없다. 따라서 fatal path는 **progress detection, panic transition, reserved crash kernel, raw memory dump, persistent console fallback**을 정상 실행 경로와 독립적으로 설계해야 한다.

## CHAPTER 01 · Watchdog는 `느리다`가 아니라 progress invariant 위반을 감지한다

CPU utilization 100%는 정상일 수도 있지만 scheduler/interrupt progress가 일정 시간 전혀 없으면 kernel control flow가 stuck했을 가능성이 높다. Watchdog는 특정 heartbeat가 정해진 시간 안에 갱신되는지 보는 progress monitor다. Threshold를 너무 낮추면 정상 long critical section을 false positive로 잡고 너무 높이면 outage evidence를 늦게 얻는다. Workload worst-case nonpreemptible interval과 recovery SLO를 함께 기준으로 잡아야 한다.

## CHAPTER 02 · Soft lockup은 task scheduling progress가 장시간 멈춘 상태를 찾는다

Soft-lockup detector는 watchdog thread가 일정 시간 CPU에서 schedule되지 못했거나 scheduler tick 기반 progress가 멈춘 경우를 감지한다. CPU가 interrupts를 받고는 있지만 preemption-disabled loop, runaway kernel code, starvation 때문에 normal task scheduling이 진행되지 않을 수 있다. Stack trace가 찍히면 당시 CPU가 어떤 loop/lock 안에 있었는지 보되, symptom CPU와 root-cause lock owner CPU가 다를 수 있어 system-wide stacks를 함께 봐야 한다.

## CHAPTER 03 · Hard lockup은 ordinary interrupt path까지 진행하지 못하는 더 강한 failure다

Hard-lockup detector는 NMI처럼 normal interrupt masking의 영향을 덜 받는 mechanism을 사용해 CPU가 장시간 아무 progress도 하지 않는지 확인할 수 있다. Local interrupts disabled loop, hardware/firmware hang, catastrophic spin이 원인일 수 있다. Hard lockup 시 regular timer/logging이 작동하지 않을 가능성이 높아 NMI-safe stack capture와 panic policy가 중요하다.

## CHAPTER 04 · NMI watchdog 자체도 PMU resource와 overhead를 소비할 수 있다

일부 architectures에서 hard-lockup detector는 hardware performance counter를 NMI source로 사용한다. 그러면 perf profiling과 PMU slot을 경쟁할 수 있고 virtualization 환경에서 지원이 제한될 수 있다. `watchdog 켜짐`만 확인할 게 아니라 실제 detector가 어떤 hardware source를 쓰며 production PMU measurement와 충돌하지 않는지 확인해야 한다.

## CHAPTER 05 · Watchdog threshold는 single value가 아니라 workload context와 연결된다

Realtime workload, stop-machine operation, firmware call, large crash dump preparation은 평소보다 긴 nonpreemptible 구간을 만들 수 있다. Threshold를 workload에 맞추되 genuine deadlock detection을 무력화할 정도로 크게 만들면 안 된다. Alert와 panic threshold를 분리하거나 production에선 warning 후 evidence 수집, safety-critical system에선 automatic reset 같은 policy를 별도 결정할 수 있다.

## CHAPTER 06 · Panic-on-lockup은 diagnosis와 availability 사이의 선택이다

Lockup warning 후 system을 계속 살려두면 일부 service가 회복할 가능성이 있지만 corrupted/stuck state가 확대될 수도 있다. Panic으로 즉시 전환하면 kdump와 reboot를 통해 known state로 돌아갈 수 있지만 transient long stall에도 full outage가 된다. `panic_on_*` 정책은 crash dump availability, HA failover, expected recovery time을 포함한 운영 결정이다.

## CHAPTER 07 · Kernel panic은 ordinary process exit보다 훨씬 제한된 execution context다

Panic 시 locks가 이미 깨져 있거나 other CPUs가 inconsistent state일 수 있고 allocator/filesystem/network가 신뢰 불가능하다. Panic handler가 normal high-level subsystem을 다시 사용하면 secondary deadlock으로 최초 증거를 잃을 수 있다. Fatal path code는 allocation 최소화, recursion 방지, bounded execution, preallocated resources 같은 별도 규칙을 가져야 한다.

## CHAPTER 08 · Panic path에서 other CPUs를 stop하는 이유는 memory image를 더 이상 바꾸지 않게 하기 위해서다

Crash dump가 시작되는 동안 다른 CPU가 shared memory를 계속 수정하면 vmcore가 서로 다른 시간대 state를 섞을 수 있다. Panic flow는 가능한 범위에서 secondary CPUs를 정지시키고 crash kernel로 control을 넘긴다. 하지만 NMI/firmware hang 때문에 CPU가 stop 명령에 응답하지 않을 수 있어 `모든 CPU 정지 성공`도 guaranteed assumption이 아니다.

## CHAPTER 09 · kdump는 정상 kernel과 분리된 crash kernel memory를 미리 예약한다

Fatal crash 뒤 same corrupted kernel이 dump code를 실행하면 신뢰하기 어렵다. Kdump는 boot 시 crashkernel 영역을 예약해 별도 작은 kernel image를 load해두고 panic 시 kexec로 전환한다. Reserved memory는 정상 workload가 쓰지 못하므로 capacity cost가 있지만, 그 isolation이 allocator corruption과 kernel-state damage에서 dump path를 보호한다.

## CHAPTER 10 · Crash kernel은 최소 driver set과 dump destination만 필요하도록 단순화해야 한다

Crash kernel에 production kernel과 같은 모든 drivers/services를 넣으면 boot time과 failure surface가 커진다. Dump할 storage/network device와 filesystem/transport에 필요한 최소 stack으로 줄이고, encrypted/root storage 의존성도 검토해야 한다. Fatal path dependency graph가 짧을수록 dump 성공률이 높아진다.

## CHAPTER 11 · kexec handoff는 hardware를 완전히 power-cycle하지 않은 상태에서 새 kernel로 전환한다

Crash kernel은 firmware 초기화 전체를 다시 거치지 않고 현재 hardware state를 물려받을 수 있다. Device가 broken DMA/interrupt state를 남겼다면 crash kernel driver가 예상과 다른 initial state를 만날 수 있다. Crash path driver는 reset/reinitialize capability가 필요하고 dump device는 가능한 단순하고 독립적인 것이 유리하다.

## CHAPTER 12 · vmcore는 physical-memory snapshot이지 자동 root-cause report가 아니다

Kdump는 `/proc/vmcore` 같은 ELF core 형태로 crashed kernel의 memory를 노출한다. 분석자는 symbols/debug info와 kernel build identity를 사용해 task lists, stacks, locks, slab, page state를 복원한다. Dump만 저장하고 정확한 vmlinux/debuginfo/build config를 보존하지 않으면 address를 의미 있는 structure로 해석하기 어렵다.

## CHAPTER 13 · Build ID와 exact symbol artifact는 crash analysis의 필수 입력이다

같은 source commit이라도 compiler flags, KASLR layout, config, module versions가 다르면 structure layout과 code address가 달라질 수 있다. Production release는 kernel image hash/build ID, unstripped vmlinux, modules, BTF/debug info를 dump retention 기간 이상 보관해야 한다. Crash artifact와 symbol artifact가 1:1로 연결되지 않으면 stack trace가 misleading할 수 있다.

## CHAPTER 14 · KASLR은 runtime addresses와 static symbols 사이에 relocation 계산을 요구한다

Crash memory의 instruction/data address는 randomized kernel base를 반영한다. Analysis tool은 vmcore metadata에서 relocation offset을 얻어 static symbol table에 적용해야 한다. `0xffffffff...` address를 release binary에 그대로 lookup해 wrong function을 가리키면 root cause를 오판할 수 있다.

## CHAPTER 15 · Module address space는 load/unload history를 함께 알아야 한다

Crash 시 load된 kernel module은 별도 virtual range와 symbols를 가진다. Module이 unload/reload되면 같은 address가 다른 code generation에 재사용될 수 있다. Vmcore의 module list, build IDs, taint/out-of-tree provenance를 보존해야 `faulting IP가 어느 module code였는가`를 증명할 수 있다.

## CHAPTER 16 · makedumpfile류 filtering은 dump size와 forensic completeness를 교환한다

Physical RAM 전체를 저장하면 수백 GB가 될 수 있어 outage recovery가 느려진다. Free pages, cache pages 등을 제외해 dump를 줄일 수 있지만 나중에 필요했던 evidence를 제거할 수도 있다. Dump level은 예상 incident class와 storage/time budget을 근거로 정하고 full-dump escalation option을 준비해야 한다.

## CHAPTER 17 · Dump destination이 crashed subsystem과 같은 failure domain이면 성공률이 낮다

Root storage controller deadlock이 원인인데 같은 controller로 vmcore를 쓰려 하면 dump가 멈출 수 있다. 가능하면 independent local device, network target, reserved partition 등 failure domain을 분리한다. Network dump도 NIC/driver/switch path가 crash 원인일 수 있어 하나의 destination만 믿지 않는 것이 좋다.

## CHAPTER 18 · Panic timeout은 dump completion 전에 reboot를 끊을 수 있다

Automatic reboot timer가 너무 짧으면 large vmcore write가 끝나기 전에 reset되어 가장 중요한 evidence를 잃는다. 반대로 dump device가 hang하면 reboot가 영원히 지연될 수 있다. Dump watchdog과 timeout, maximum dump size, fallback destination을 함께 설계해야 availability와 forensic capture가 균형을 이룬다.

## CHAPTER 19 · pstore는 firmware/ram backend에 small persistent records를 남긴다

Pstore는 panic/oops/console messages를 EFI variable, ramoops reserved RAM 같은 backend에 저장해 reboot 뒤 userspace가 읽을 수 있게 한다. Full memory dump보다 정보량은 적지만 crash kernel이 실패하거나 storage가 unavailable한 상황에서 마지막 console evidence를 남길 수 있다. Kdump와 대체 관계가 아니라 fallback hierarchy다.

## CHAPTER 20 · ramoops는 reserved RAM 영역을 reboot 뒤에도 crash log로 사용한다

Platform이 power-cycle 없이 RAM 내용을 유지하는 조건에서 reserved region에 console/oops records를 기록하고 다음 boot에서 pstore로 노출할 수 있다. Region size가 작아 ring overwrite와 truncation이 발생할 수 있으므로 record size/count를 kernel message volume에 맞춰 설정해야 한다. Sensitive kernel logs가 reboot 뒤 남으므로 접근 권한/retention도 보안 정책에 포함한다.

## CHAPTER 21 · Persistent console은 first failure와 secondary panic을 구분해야 한다

Fatal path에서 dump code가 다시 fault하면 console에는 원래 panic과 crash-handler panic이 연속 기록될 수 있다. Timestamp/CPU/panic generation marker가 없으면 두 stack을 섞어 분석한다. Panic entry에서 once-only atomic flag와 reason chain을 기록해 recursive failure를 분리해야 한다.

## CHAPTER 22 · NMI-safe logging은 ordinary printk lock과 buffer dependency를 줄여야 한다

NMI context에서 normal console path가 same CPU/other CPU lock을 기다리면 deadlock할 수 있다. Modern logging subsystem은 atomic/NMI contexts를 고려한 buffering을 사용해야 한다. Watchdog stack dump가 `printk 때문에 다시 멈추는` 현상을 막으려면 fatal observability path의 lock dependencies를 별도로 분석해야 한다.

## CHAPTER 23 · Console flood는 crash detection과 dump handoff를 지연시킬 수 있다

Lockup 순간 모든 CPUs stack, locks, memory diagnostics를 synchronous slow serial console에 출력하면 seconds/minutes가 걸릴 수 있다. Evidence가 많다고 항상 좋은 것이 아니다. High-value minimal first record를 빠르게 남기고 full detail은 vmcore에서 분석하는 전략이 더 안정적일 수 있다.

## CHAPTER 24 · Panic notifier는 ordering과 bounded behavior를 요구한다

Subsystem은 panic notifier로 device LED, hypervisor notification, telemetry marker 등을 남길 수 있지만 callback이 sleep하거나 unknown lock을 잡으면 fatal path를 망가뜨린다. Notifier priority/order와 allowed operations를 제한하고 fault injection으로 callback hang을 시험해야 한다. Panic path extension은 production critical code다.

## CHAPTER 25 · Kdump self-test는 실제 panic 없이 예약/boot/dump path를 정기 검증해야 한다

Crashkernel parameter가 설정됐다고 dump가 되는 것은 아니다. Reserved memory 부족, image load failure, storage credential expiry, network route 변화, symbol retention 누락이 생길 수 있다. Maintenance test에서 crash kernel이 실제 boot하고 small test dump를 destination에 쓰며 analysis tool이 읽을 수 있는지 end-to-end 검증해야 한다.

## CHAPTER 26 · Fault injection은 lockup/panic path의 failure hierarchy를 검증한다

Synthetic panic, soft-lockup simulator, NMI watchdog test, forced sysrq crash 등을 controlled environment에서 사용해 watchdog detection→panic→kexec→dump→reboot→artifact upload chain을 확인한다. Production에서 최초 실제 kernel crash가 이 path의 첫 실행이 되어서는 안 된다.

## CHAPTER 27 · Crash dump에는 secrets와 user memory가 포함될 수 있다

Vmcore는 credentials, cryptographic material, application data, network packets를 포함할 수 있다. Storage encryption, access control, retention, redaction/export process를 security incident data와 같은 수준으로 관리해야 한다. Debug convenience 때문에 unrestricted object storage에 dump를 올리면 fatal crash가 data breach로 이어질 수 있다.

## CHAPTER 28 · Evidence retention은 binary retention과 같은 lifecycle을 가져야 한다

Vmcore만 남기고 해당 kernel/debuginfo를 삭제하거나 반대로 symbols만 있고 dump가 만료되면 분석 불가능하다. Release ID를 key로 vmcore, pstore, kernel config, vmlinux, module symbols, boot parameters, hardware firmware versions를 bundle/metadata로 연결한다. Incident retention period가 release artifact GC policy보다 길 수 있다.

## CHAPTER 29 · Lockup 관측은 panic 직전 CPU-wide state를 상관해야 한다

Watchdog가 CPU 3 stack만 보여줘도 실제 원인은 CPU 7이 lock을 잡고 interrupt-disabled loop에 빠진 것일 수 있다. All-CPU backtrace, lock owner metadata, scheduler runqueue, IRQ state, RCU stall info, PMU NMI status를 같은 timestamp/generation으로 수집해야 한다. Single stack은 symptom location이지 causal graph가 아니다.

## CHAPTER 30 · Fatal observability는 정상 subsystem이 모두 실패해도 증거가 남는 독립 경로여야 한다

신뢰할 수 있는 kernel failure pipeline은 **soft/hard lockup detection, panic policy, recursive-failure-safe logging, reserved crash kernel, hardware reinitialization, vmcore+symbols, independent dump destination, timeout/fallback, pstore, security/retention, periodic end-to-end self-test**를 하나로 설계한다. `로그가 남겠지`가 아니라 정상 scheduler·allocator·filesystem·storage가 이미 고장났다는 가정에서도 최소 하나의 evidence path가 살아 있어야 한다.
