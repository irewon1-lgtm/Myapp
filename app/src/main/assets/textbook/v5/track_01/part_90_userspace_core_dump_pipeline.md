# PART 90 · Userspace Core Dump Pipeline — fatal signal, thread-group stop, ELF notes, mapping policy, collector lifetime

Userspace core dump는 process memory를 무조건 통째로 파일에 복사하는 기능이 아니다. Fatal signal이 process를 dumpable state로 전환하면 kernel은 같은 address space를 공유하는 threads를 crash snapshot에 맞춰 정지시키고, register/thread metadata를 ELF notes로 직렬화하며, VMA별 dump policy와 resource limit를 적용해 memory segments를 내보낸다. Output은 직접 file일 수도 있고 `core_pattern` pipe collector로 stream될 수도 있다. 정확한 postmortem은 **process가 죽는 순간의 execution state를 얼마나 일관되게 freeze했는가, 어떤 mappings를 의도적으로 제외했는가, collector가 crash process metadata를 언제까지 볼 수 있는가, dump가 secret을 노출하지 않는가**를 함께 증명해야 한다.

## CHAPTER 01 · Core dump 진입점은 fatal signal semantics와 dumpability policy의 교차점이다

SIGSEGV·SIGABRT처럼 default action이 core dump인 signal이 발생해도 항상 dump가 생성되는 것은 아니다. Process dumpable state, RLIMIT_CORE, filesystem/pipe policy, kernel configuration, permission/security restrictions가 모두 통과해야 실제 snapshot이 시작된다. 따라서 `process died with SIGSEGV but core missing`은 signal handling bug가 아니라 policy gate 중 하나에서 막힌 것일 수 있다. Incident tooling은 **signal cause와 dump-generation eligibility를 별도 상태로 기록**해야 원인을 빠르게 좁힐 수 있다.

## CHAPTER 02 · Dumpability는 privileged execution의 memory disclosure risk를 제한한다

Setuid/setgid, file capabilities, credential transition을 거친 process memory에는 다른 사용자가 읽어서는 안 되는 secrets와 privileged state가 포함될 수 있다. `PR_SET_DUMPABLE`과 `fs.suid_dumpable`은 이런 process의 core creation과 ownership/path policy를 제한한다. Debug convenience 때문에 dumpability를 전역 완화하면 private keys, tokens, user data가 crash artifact로 노출될 수 있다. **Crash evidence availability와 credential-bound confidentiality는 서로 반대 방향의 requirements**라 environment별 policy가 필요하다.

## CHAPTER 03 · Fatal thread 하나의 crash는 thread group 전체의 snapshot coordination을 요구한다

멀티스레드 process에서 한 thread가 fatal fault를 일으켜도 다른 threads는 같은 address space를 수정하고 있을 수 있다. Core image가 registers는 crash 시점인데 heap은 수 ms 뒤 다른 thread가 바꾼 값이라면 postmortem causal analysis가 왜곡된다. Kernel은 coredump coordination을 통해 address-space participants를 더 이상 정상 execution하지 못하게 하고 snapshot writer를 하나로 정리해야 한다. **Crash snapshot consistency는 thread-group execution을 quiesce하는 barrier에서 시작**한다.

## CHAPTER 04 · Thread-group stop은 normal cooperative shutdown과 다른 emergency quiescence다

Ordinary process shutdown은 workers에게 flag를 보내고 locks를 풀며 queues를 drain할 수 있지만 fatal core path는 arbitrary instruction/lock state에서 시작한다. 다른 thread가 uninterruptible kernel wait에 있거나 kernel critical section을 실행 중일 수 있어 즉시 완벽한 userspace-level quiescence가 불가능할 수도 있다. Core path는 가능한 execution state를 안정화하면서 dead process cleanup과 snapshot generation을 조정해야 한다. Crash dump를 **application-consistent checkpoint와 동일한 것으로 취급하면 안 된다.**

## CHAPTER 05 · ELF core는 executable image가 아니라 crash state를 담는 container format이다

ELF core file은 loadable program처럼 실행하기 위한 object가 아니라 program headers와 PT_NOTE records, memory ranges를 이용해 debugger가 process virtual address space와 thread register state를 재구성하도록 한다. Original executable/shared libraries의 file contents를 모두 포함하지 않을 수 있어 debugger는 matching binaries와 symbols를 외부에서 찾아야 한다. Core file 하나만 archive하고 exact executable/build-id를 잃으면 stack symbolization과 object layout 해석이 깨진다. **Crash evidence unit은 core + exact binaries + symbols + environment metadata의 묶음**이다.

## CHAPTER 06 · PT_NOTE는 threads와 process metadata를 memory bytes와 분리해 전달한다

General-purpose registers, floating/SIMD state, signal info, process status, auxiliary vectors 같은 metadata는 ordinary virtual-memory segments와 의미가 다르므로 ELF notes로 표현된다. Thread별 register note를 통해 debugger가 crashing thread뿐 아니라 sibling threads의 call stacks와 lock waits를 재구성한다. Architecture/toolchain이 추가 register set을 사용하면 corresponding notes를 보존해야 vector/extended state corruption을 분석할 수 있다. **Bytes와 execution context는 서로 다른 serialization channels**이다.

## CHAPTER 07 · Crashing thread identification은 많은 thread stacks 중 causal starting point를 찾는 기준이다

수백 threads가 모두 core에 포함되면 postmortem 첫 단계는 fatal signal과 register state를 가진 thread를 식별하는 것이다. 다른 threads가 mutex/futex/epoll에서 기다리는 stack만 보고 deadlock을 원인으로 오해할 수 있다. Signal number, siginfo fault address, instruction pointer, thread id를 동일 record로 연결해야 한다. 이후 shared-data corruption 가능성을 보기 위해 sibling stacks를 확장한다. **Primary fault thread와 collateral blocked threads를 분리하는 것이 core triage의 첫 invariant**다.

## CHAPTER 08 · coredump_filter는 artifact size와 forensic completeness를 mapping type별로 교환한다

`/proc/<pid>/coredump_filter` bitmask는 anonymous private/shared, file-backed private/shared, ELF headers, HugeTLB, DAX mappings 같은 종류별 포함 여부를 결정한다. Large shared caches/file mappings를 제외하면 artifact를 크게 줄일 수 있지만 bug가 그 mapping의 modified private bytes나 shared state에 있으면 evidence를 잃는다. Filter는 child가 inherit하고 exec 뒤에도 유지되므로 service launcher에서 policy를 고정할 수 있다. **Dump 크기 제한은 random truncation보다 semantic mapping selection으로 하는 편이 진단 가능성을 높인다.**

## CHAPTER 09 · MADV_DONTDUMP는 application이 특정 VMA를 crash artifact에서 명시적으로 제외한다

Huge cache, cryptographic material, replicated datasets처럼 core에 남길 가치보다 크기/기밀성 위험이 큰 region은 `MADV_DONTDUMP`로 제외할 수 있다. 그러나 pointer/object graph가 excluded region을 가리키면 debugger가 object 내용을 못 읽어 root cause 분석이 어려워질 수 있다. Security-sensitive mappings를 제외한다면 최소한 metadata/hash/state summary를 별도 crash telemetry에 남길지 설계해야 한다. **Evidence minimization은 visibility를 없애는 게 아니라 민감 bytes 대신 안전한 metadata를 남기는 문제**다.

## CHAPTER 10 · MMIO와 vDSO는 일반 coredump_filter 규칙과 다른 특별 취급을 받는다

Device framebuffer 같은 MMIO pages를 dump하려고 읽으면 side effect, bus error, huge artifact가 생길 수 있어 core image에서 제외된다. 반대로 vDSO mapping은 userspace call stack/unwind와 ABI 해석에 필요해 filter와 무관하게 포함될 수 있다. VMA가 address space에 존재한다는 이유만으로 `읽어서 저장 가능한 memory`라고 가정하면 안 된다. Core writer도 **mapping backing type과 safe-read semantics를 구분**한다.

## CHAPTER 11 · RLIMIT_CORE는 core artifact의 최대 output budget을 process policy로 제공한다

Direct file dump에서는 RLIMIT_CORE가 dump size를 제한해 runaway multi-GB process가 filesystem을 채우는 것을 막을 수 있다. 하지만 limit에 걸린 core는 뒤쪽 mappings가 누락되어 debugger가 일부 pointer를 읽지 못하는 partial artifact가 될 수 있다. Collector/policy는 `core 파일이 존재한다`만 보고 완전하다고 판단하지 말고 expected size/filter와 truncation 상태를 기록해야 한다. **Artifact existence와 forensic completeness는 별도 상태**다.

## CHAPTER 12 · core_pattern은 crash artifact naming을 process identity metadata와 연결한다

`core_pattern`의 `%p/%P/%i/%I/%u/%g/%s/%t/%h/%e/%E/%c/%C/%F` 같은 expansion은 PID namespace, signal, executable, time, CPU 등을 filename/helper arguments에 넣을 수 있다. Containerized system에서 local PID와 init-namespace PID가 다르므로 `%p` 하나만 저장하면 host-side incident와 process를 매칭하기 어려울 수 있다. Filename은 convenience가 아니라 **crash artifact를 exact process generation과 연결하는 indexing metadata**다.

## CHAPTER 13 · Pipe core_pattern은 kernel writer와 userspace collector를 streaming producer-consumer로 연결한다

Pattern이 `|`로 시작하면 kernel은 core bytes를 file에 직접 쓰지 않고 helper process의 stdin으로 stream한다. Collector는 compression, encryption, upload, indexing을 수행할 수 있지만 crash path가 userspace service availability와 storage/network dependency에 연결된다. Collector가 막히면 crashing process cleanup도 늦어질 수 있다. **Crash evidence pipeline은 kernel→pipe→collector→persistent store 전체를 하나의 backpressure chain**으로 관리해야 한다.

## CHAPTER 14 · core_pipe_limit은 crash storm에서 collector concurrency와 /proc lifetime을 제한한다

여러 processes가 동시에 crash하면 unlimited helper를 생성하는 것은 fork/memory/storage storm을 만들어 장애를 확대할 수 있다. `core_pipe_limit`은 concurrent pipe collectors를 제한하고, kernel이 helper가 종료될 때까지 기다리도록 해 collector가 `/proc/<crashing-PID>` metadata를 읽을 수 있게 할 수 있다. Limit 초과 dump는 skip될 수 있으므로 core absence가 application bug가 없었다는 뜻이 아니다. **Collector capacity 자체가 incident evidence loss policy**를 결정한다.

## CHAPTER 15 · core_pipe_limit=0의 unlimited 모드는 /proc access lifetime을 보장하지 않는다

특수값 0은 병렬 collector 수를 제한하지 않지만 kernel이 helper 종료를 기다려 crash process의 `/proc` entries를 유지해준다는 보장이 없다. Collector가 core stream과 함께 cmdline/maps/status를 읽으려 하면 process metadata가 먼저 사라질 수 있다. Metadata가 반드시 필요하면 nonzero limit와 충분한 collector throughput을 설계해야 한다. **Concurrency 무제한과 metadata consistency를 동시에 얻을 수 있다고 가정하면 안 된다.**

## CHAPTER 16 · core collector가 느리면 dead process resource release가 늦어진다

Pipe handler가 compression/upload에서 오래 block되면 kernel이 coredump completion을 기다리는 동안 crashing task/thread-group의 mm와 process resources가 더 오래 유지될 수 있다. Crash storm에서는 이미 memory pressure가 원인인데 cores가 수십 GB stream되며 pressure가 악화될 수 있다. Collector는 streaming compression, size/filter policy, timeout/fallback storage를 가져야 한다. **Postmortem 품질을 높이는 기능이 incident recovery capacity를 고갈시키지 않도록 bounded해야 한다.**

## CHAPTER 17 · Core dump writer는 page fault/read failure를 만나도 전체 artifact policy를 결정해야 한다

Crash process VMA를 읽는 동안 swapped page, hwpoison, userfaultfd, inaccessible mapping처럼 normal page access가 실패할 수 있다. Core generation이 한 page 오류 때문에 전체 dump를 버릴지, zero/skip/error metadata를 남기고 계속할지는 artifact usefulness에 큰 영향을 준다. Memory corruption incident에서는 바로 문제 page가 읽히지 않을 수 있으므로 `perfect memory copy`를 전제로 debugger를 설계하면 안 된다. **Partial observability를 명시적으로 표시하는 format/collector metadata가 필요**하다.

## CHAPTER 18 · HugeTLB와 DAX mappings는 artifact 크기와 backing semantics 때문에 별도 filter bits를 가진다

Huge private mapping 하나가 수 GB artifact를 만들 수 있고 DAX는 persistent storage를 직접 mapping하므로 ordinary anonymous/file-backed 분류와 cost가 다르다. Default policy가 모든 huge/shared/DAX memory를 포함하지 않는 이유는 crash dump가 storage capacity를 압도할 수 있기 때문이다. 그러나 persistent-memory corruption이나 hugepage allocator bug를 조사할 때는 이 mappings가 핵심 evidence일 수 있다. **Workload별 incident class에 맞춰 dump filter profile을 사전에 준비**해야 한다.

## CHAPTER 19 · fork/exec inheritance는 coredump_filter를 deployment policy로 만든다

Coredump filter는 child process에 inherit되고 exec 이후에도 유지되므로 shell/service manager/container runtime이 application 실행 전에 dump policy를 세팅할 수 있다. Application source를 수정하지 않고도 fleet별 evidence level을 통제할 수 있다는 장점이 있다. 반대로 parent environment에서 우연히 바뀐 filter가 child 모두에 퍼져 예상보다 민감 data를 dump하거나 중요한 mappings를 제외할 수 있다. **Process-launch boundary에서 filter를 explicit configuration으로 고정**하는 편이 재현성 있다.

## CHAPTER 20 · PID namespace에서는 crash process의 local PID와 collector가 보는 global PID를 구분해야 한다

Container 안 PID 1인 process가 host에서는 PID 48231일 수 있다. Core filename, `/proc` lookup, orchestration metadata가 서로 다른 namespace PID를 쓰면 artifact를 wrong container/process에 연결할 수 있다. `%p/%P`, `%i/%I`, pidfd-related metadata를 함께 저장하고 namespace/container identity를 collector가 추가해야 한다. **Integer PID 하나는 globally stable process identity가 아니다.**

## CHAPTER 21 · User namespace와 credential mapping은 dump ownership/privacy policy를 복잡하게 만든다

Container user namespace의 uid/gid와 initial namespace credential이 다를 수 있고 core_pattern helper는 crashing process와 다른 privilege context에서 실행될 수 있다. Artifact file owner/permission을 단순 process uid에 맞추면 host filesystem에서 예상치 못한 접근권한을 만들 수 있다. Collector는 initial credential, container identity, dumpability mode를 확인해 storage ACL과 encryption key를 결정해야 한다. **Crash artifact는 process memory 전체를 포함할 수 있는 high-sensitivity object**로 취급해야 한다.

## CHAPTER 22 · Core dump에는 secret이 평문으로 존재할 가능성이 매우 높다

Heap/stack에는 access token, private key, password, user payload, database row가 application lifetime 동안 평문으로 존재할 수 있다. Core artifact를 일반 log와 같은 bucket, retention, support-ticket attachment로 취급하면 정상 production security boundary를 우회한다. Encryption at rest/in transit, least-privilege access, short retention, audit log, regional/data-classification policy가 필요하다. **디버깅 가치가 높을수록 artifact의 공격 가치도 높다.**

## CHAPTER 23 · Symbolization은 core와 exact executable/shared libraries의 identity를 결합해야 한다

ASLR 때문에 runtime address는 binary-relative address와 달라지고 stripped production binary에는 source symbols가 없을 수 있다. Core notes/mappings와 exact build-id의 executable, shared libraries, debug symbols를 결합해야 stack frame과 variables를 정확히 복원할 수 있다. `같은 version string`이라도 compiler flags/LTO/build timestamp가 달라 binary layout이 바뀔 수 있다. **Build-id/content hash를 crash artifact metadata에 저장**해야 wrong symbol로 그럴듯한 거짓 stack을 만드는 것을 막는다.

## CHAPTER 24 · JIT runtime은 native ELF core만으로 code identity를 복원하기 어려울 수 있다

JIT code cache는 process heap에 동적으로 생성되고 code address가 runtime profile/deoptimization에 따라 바뀐다. Core에는 bytes가 포함돼도 method name/source mapping metadata가 별도 runtime structure에 있을 수 있다. Runtime crash support는 JIT symbol map, code-cache metadata, managed stack frames를 core와 같이 보존하거나 debugger plugin이 heap structures를 해석할 수 있어야 한다. **Dynamic code generation은 artifact에 code provenance를 추가로 요구**한다.

## CHAPTER 25 · Core snapshot은 application transaction consistency를 보장하지 않는다

Thread-group execution을 멈췄더라도 crash는 arbitrary instruction 중간에 발생했으므로 in-memory data structure가 lock transition, journal append, partial state update 한가운데일 수 있다. Debugger가 pointer invariant가 깨졌다고 해서 그것이 crash 원인인지 crash 시점의 정상 transient state인지 구분해야 한다. Lock owner/thread stacks와 state-machine phase를 함께 보면 transient 여부를 판단할 수 있다. **Core는 crash-consistent memory snapshot이지 business-transaction-consistent checkpoint가 아니다.**

## CHAPTER 26 · gcore/ptrace 기반 live snapshot은 fatal core와 consistency semantics가 다르다

Debugger가 running process에 attach해 `gcore`를 생성할 수 있지만 process를 어떻게 stop하고 threads를 동결하는지, kernel I/O가 계속 진행하는지에 따라 fatal crash core와 다른 시점 semantics를 가진다. Production에서 `hang`을 조사할 때 live core는 valuable하지만 snapshot 생성 자체가 process pause latency와 memory/storage pressure를 만든다. Live core와 fatal core를 같은 artifact type으로 저장하더라도 **capture trigger와 quiescence method를 metadata에 구분**해야 한다.

## CHAPTER 27 · Disk-full/collector failure는 crash artifact 자체에 failure telemetry가 필요하다

Process가 이미 죽었는데 core write가 ENOSPC, quota, permission, pipe helper failure로 중단되면 application log만으로는 `core를 왜 못 찾는지` 알 수 없다. Kernel/collector는 dump attempted, skipped reason, bytes written, truncation, helper exit status, storage destination을 별도 reliable telemetry에 남겨야 한다. Core pipeline failure가 원 incident와 동일 disk-full/memory-pressure failure domain을 공유할 수 있다. **Evidence system의 실패를 관측하는 second-order observability**가 필요하다.

## CHAPTER 28 · Crash storm에서는 모든 core를 보존하는 것보다 representative evidence를 bounded하게 남기는 편이 안전할 수 있다

동일 binary bug로 수천 replicas가 동시에 crash하면 full core를 전부 수집하는 것은 storage/network/collector를 압도해 recovery를 늦춘다. First-N full core + remaining metadata/signature, sampling, hash-based deduplication 같은 policy가 더 높은 fleet visibility를 줄 수 있다. 단, distinct crash signatures를 과도하게 합치면 rare second bug를 잃을 수 있다. **Evidence admission control도 cardinality와 novelty를 판단하는 resource scheduler**다.

## CHAPTER 29 · Core pipeline 테스트는 crash type뿐 아니라 collector resource failure까지 포함해야 한다

SIGSEGV/SIGABRT만 발생시켜 file 하나 생성됐는지 보는 테스트로는 부족하다. Multithread mutation 중 crash, huge mapping, DONTDUMP secret region, RLIMIT_CORE truncation, pipe collector saturation, core_pipe_limit 초과, disk full, namespace PID, privileged dump policy를 조합해 artifact와 metadata가 의도대로 남는지 확인해야 한다. Exact symbols로 stack을 실제 unwind하는 단계까지 자동 검증해야 한다. **Dump 생성 success가 아니라 postmortem 질문에 답할 수 있는 artifact인지 검증**해야 한다.

## CHAPTER 30 · Userspace core correctness는 crash execution generation을 재현 가능한 evidence bundle로 봉인하는 proof다

한 core pipeline을 승인하려면 `fatal signal과 crashing thread를 식별할 수 있는가`, `sibling execution이 snapshot 중 어떻게 정지되는가`, `어떤 VMA가 왜 포함/제외됐는가`, `truncation/partial read가 표시되는가`, `collector가 namespace/credential/process metadata를 안정적으로 확보하는가`, `exact binaries/symbols가 연결되는가`, `secret protection과 retention이 충분한가`, `crash storm에서도 bounded evidence가 남는가`를 답해야 한다. **Core dump는 죽은 process의 memory file이 아니라 한 execution generation을 분석 가능한 형태로 보존하는 forensic transaction**이다.
