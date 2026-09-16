# PART 07 · kernel boundary, 권한, 격리 — 프로그램이 시스템 전체를 마음대로 못 하는 이유

프로그램은 CPU instruction을 실행하지만 아무 memory나 읽고 아무 device나 제어하고 아무 process나 죽일 수 있는 권한을 자동으로 얻지 않는다. 운영체제는 **process credentials, virtual memory protection, system call validation, file permission, capability, namespace, resource limit** 같은 여러 층을 이용해 실행을 통제한다.

이 PART의 목표는 `관리자 권한`이라는 한 단어를 외우는 것이 아니다. **누가 어떤 resource에 어떤 operation을 요청했고, kernel이 어떤 identity와 policy를 근거로 허용/거부했는지** 추적하는 모델을 만드는 것이다.

---

## CHAPTER 01 · system call은 library function과 같은 층이 아니다

### application API가 곧 kernel entry는 아니다

고수준에서 파일을 연다고 하자.

```text
language API
openFile(...)
↓
runtime/library
↓
system call wrapper
↓
kernel
↓
filesystem / driver
```

어떤 API는 user space에서 전부 처리될 수 있고, 어떤 API는 여러 system call을 조합할 수 있다.

따라서 profiler에서 function call 수와 system call 수를 동일하게 세면 안 된다.

### system call boundary에서 validation이 필요하다

user process가 kernel에 pointer와 length를 넘긴다고 하자.

kernel은 user가 준 pointer를 그대로 신뢰하면 안 된다.

```text
address valid?
user-accessible range?
length overflow?
permission okay?
resource limit okay?
operation allowed for credentials?
```

kernel bug가 위험한 이유는 이 검증 경계가 높은 privilege에서 동작하기 때문이다.

---

## CHAPTER 02 · syscall ABI는 함수 이름보다 낮은 수준의 계약이다

### user와 kernel은 register/number 규약으로 만난다

실제 syscall entry에서는 architecture가 정한 방식으로 syscall number와 argument를 register 등에 배치하고 trap instruction을 사용한다.

C library가 이를 감싸 평범한 함수처럼 보이게 한다.

```text
read(fd, buf, n)
```

source에서 함수 호출처럼 보이지만 내부에는 privilege transition이 있다.

### syscall 번호를 hard-code하면 portability가 깨진다

architecture/OS에 따라 syscall number와 ABI가 다를 수 있다.

그래서 application은 보통 libc/runtime wrapper를 사용한다.

low-level sandbox나 tracer를 만들 때만 ABI 세부를 직접 다루는 경우가 많다.

---

## CHAPTER 03 · credentials는 process가 누구인지 kernel에 설명한다

### UID/GID를 단순 login name으로만 보지 않는다

Unix 계열에서 process는 user/group identity와 관련된 credential을 가진다.

filesystem permission, signal, process control 등 여러 kernel operation에서 이 credential이 사용된다.

실제 Linux에는 real/effective/saved ID 등 여러 종류가 있고 supplementary group도 있다.

초보 단계에서 중요한 것은:

> permission check는 화면에 표시된 사용자 이름이 아니라 kernel이 관리하는 process credential을 기준으로 수행된다.

### effective identity

set-user-ID 같은 mechanism을 사용하면 실행 파일 owner 권한과 process effective identity가 상호작용할 수 있다.

이 기능은 필요한 privileged operation만 수행하고 권한을 줄이는 설계에 쓰일 수 있지만 잘못 사용하면 privilege escalation 위험이 커진다.

---

## CHAPTER 04 · root 하나로 모든 권한을 설명하면 너무 거칠다

전통적인 Unix 모델에서는 UID 0 process가 광범위한 privilege를 가진다.

Linux는 root privilege를 여러 **capability**로 나누어 특정 권한만 부여할 수 있다.

예를 들면 network raw operation, UID 변경, system administration 등 서로 다른 권한 단위가 있다.

### least privilege

프로세스가 필요한 operation보다 훨씬 큰 privilege를 가지면 bug가 침해로 이어졌을 때 피해 범위가 커진다.

```text
필요: 낮은 port bind만
잘못된 설계: full root
더 작은 권한: 필요한 capability만
```

실제 service 설계에서는 가능한 한 작은 privilege set을 사용한다.

### capability가 있다고 모든 check를 bypass하는 것은 아니다

각 capability가 허용하는 operation 범위가 정해져 있고 namespace와 다른 security mechanism의 영향을 받을 수 있다.

`CAP 하나 = 관리자 전체 권한`으로 이해하면 안 된다.

---

## CHAPTER 05 · filesystem permission은 path lookup 단계마다 관여한다

### 파일 하나의 mode bit만 보는 것으로 부족하다

```text
/home/user/private/data.txt
```

파일을 열려면 path의 directory를 탐색해야 한다.

상위 directory에 필요한 search/execute permission이 없으면 파일 자체가 readable이어도 접근이 막힐 수 있다.

### read/write/execute 의미는 file type에 따라 다르게 해석될 수 있다

regular file의 execute와 directory의 execute/search 의미는 다르다.

따라서 `chmod 777이면 된다`는 식으로 문제를 해결하면 권한을 과도하게 넓히고 보안 원인을 숨긴다.

### permission denied 디버깅

```text
어떤 process credential인가?
어떤 exact path인가?
각 directory permission은?
ACL/SELinux 같은 추가 policy가 있는가?
mount가 read-only인가?
sandbox path인가?
```

error message 하나보다 실제 check chain을 따라간다.

---

## CHAPTER 06 · file descriptor도 resource limit을 가진다

### 무한히 open할 수 없다

process가 open file/socket/pipe descriptor를 계속 만들고 닫지 않으면 limit에 도달할 수 있다.

```text
accept connection
fd created
handler error path
close skipped
```

이런 leak은 memory leak과 비슷한 패턴을 가진다.

### 증상

```text
too many open files
새 connection 실패
log file open 실패
DNS/network library가 간접 실패
```

root cause가 file descriptor leak인데 상위에서는 `network 장애`처럼 보일 수 있다.

### limit은 방어 장치이기도 하다

resource limit은 한 process가 system-wide resource를 무한 소비하지 못하게 한다.

limit을 무작정 높이면 leak 증상을 늦출 뿐 원인을 고치지 못한다.

---

## CHAPTER 07 · memory limit과 OOM은 여러 층에서 발생할 수 있다

`메모리 부족`도 하나의 limit이 아니다.

```text
language/runtime heap limit
process virtual address constraints
cgroup/container memory limit
system-wide physical memory pressure
Android process management
```

어느 층이 먼저 제한을 걸었는지 확인해야 한다.

container memory limit 안에서 죽은 process를 host의 free RAM만 보고 이해할 수 없는 이유다.

---

## CHAPTER 08 · cgroup은 process 묶음에 resource policy를 적용한다

### process hierarchy와 resource controller

Linux cgroup은 process를 group hierarchy에 배치하고 CPU, memory 등 resource 사용을 제한/계측하는 mechanism을 제공한다.

개념:

```text
root cgroup
├─ service A
│  ├─ worker 1
│  └─ worker 2
└─ service B
```

service A 전체에 memory/CPU policy를 적용할 수 있다.

### container가 자기 machine처럼 보여도 host resource를 공유한다

container isolation은 VM과 같지 않다.

process들은 host kernel을 공유할 수 있고 cgroup/resource policy와 namespace를 조합해 격리된 environment를 만든다.

따라서 container에서 CPU 2개가 보이거나 memory limit 1GB가 설정돼 있어도 물리 host 구조와 동일하지 않을 수 있다.

---

## CHAPTER 09 · namespace는 process가 보는 시스템 view를 격리한다

Linux namespace는 process가 보는 특정 global resource view를 분리할 수 있다.

종류 예:

```text
PID namespace
mount namespace
network namespace
IPC namespace
UTS namespace
user namespace
cgroup namespace
```

### PID namespace

container 안에서 PID 1처럼 보이는 process가 host에서는 다른 PID를 가질 수 있다.

그래서 container log의 PID와 host diagnostic tool의 PID를 연결할 때 namespace를 고려해야 한다.

### network namespace

interface, route, socket view를 분리할 수 있다.

`localhost`는 항상 host 전체를 뜻하지 않는다.

현재 process가 속한 network namespace 안의 loopback이다.

이 사실을 모르면 container에서 `127.0.0.1` 연결 오류를 이해하기 어렵다.

---

## CHAPTER 10 · user namespace의 root는 host root와 같지 않을 수 있다

user namespace는 UID/GID mapping을 격리할 수 있다.

process가 namespace 내부에서는 UID 0처럼 보여도 외부 host에서는 unprivileged UID에 mapping될 수 있다.

```text
inside container/user ns: uid 0
outside host: uid 100000
```

이 구조는 `root`라는 숫자 하나보다 **어느 namespace에서 어떤 capability를 가지는가**가 중요하다는 것을 보여 준다.

---

## CHAPTER 11 · chroot와 namespace와 sandbox를 같은 것으로 보지 않는다

### chroot

process가 보는 filesystem root를 바꾸는 mechanism이다.

하지만 이것 하나만으로 완전한 security container가 되는 것은 아니다.

### namespace

여러 global resource view를 격리한다.

### sandbox

더 넓은 개념이다.

permission, syscall filtering, namespace, UID isolation, MAC policy, broker process 등 다양한 mechanism을 결합할 수 있다.

`chroot했으니 안전` 같은 단일 기능 신뢰는 위험하다.

---

## CHAPTER 12 · mandatory access control은 owner permission 외의 policy를 추가한다

Unix mode bit만으로 모든 access control을 설명하기 어려운 환경에서는 SELinux 같은 MAC system이 추가 policy를 적용할 수 있다.

```text
DAC permission allows
BUT
MAC policy denies
```

그래서 `chmod 777인데 왜 안 되지?`라는 상황이 생길 수 있다.

권한 문제를 해결할 때 security framework를 끄는 것은 원인을 제거하는 게 아니라 보호 장치를 제거하는 것이다.

---

## CHAPTER 13 · Android app sandbox는 Linux identity와 platform policy를 사용한다

Android는 일반적으로 앱마다 고유한 Linux UID를 부여해 app data와 process를 격리한다.

앱 A가 앱 B의 private data directory를 임의로 읽지 못하도록 filesystem permission과 platform security mechanism이 협력한다.

### permission API는 sandbox 위에 추가 capability를 준다

camera/location 같은 protected operation은 manifest/runtime permission과 system service policy를 통해 접근이 제어된다.

permission 승인을 받았다고 다른 앱 private memory까지 접근할 수 있게 되는 것은 아니다.

### app signing도 identity 일부다

Android package signing key는 update trust와 package identity에 중요하다.

같은 applicationId 문자열만 복사한다고 기존 app의 update가 될 수 있는 것이 아니다.

---

## CHAPTER 14 · Binder는 Android process boundary를 넘는 RPC infrastructure다

Android framework의 많은 service call은 다른 process로 넘어갈 수 있다.

개념적으로:

```text
app process
proxy call
↓
Binder driver/kernel mediation
↓
service process thread
actual method execution
↓
reply
```

### local function처럼 보여도 remote failure가 있다

다른 process가 죽거나 timeout/transaction size 문제가 생기면 평범한 in-process call과 다른 실패가 가능하다.

그래서 IPC interface에는 serialization, version compatibility, identity, thread-safety를 고려해야 한다.

### Binder thread pool

service-side method가 main thread 하나에서만 호출된다고 가정하면 안 되는 API가 있다.

여러 Binder thread에서 동시에 들어올 수 있으므로 shared state는 thread-safe해야 한다.

---

## CHAPTER 15 · process identity와 request identity를 분리한다

server가 privileged process라고 해서 caller도 privileged인 것은 아니다.

IPC/RPC service는 **누가 요청했는지**를 확인하고 operation authorization을 수행해야 한다.

```text
service process privilege
≠ caller permission
```

confused deputy 문제가 여기서 나온다.

높은 권한을 가진 service가 낮은 권한 caller의 요청을 검증 없이 대신 실행하면 privilege가 우회된다.

---

## CHAPTER 16 · TOCTOU는 check와 use 사이에 상태가 바뀌는 race다

Time Of Check To Time Of Use.

```text
check: path is safe
--- attacker changes path/symlink ---
use: open path
```

check 결과와 실제 use 대상이 달라질 수 있다.

### name-based validation의 한계

path 문자열을 먼저 검사하고 나중에 다시 open하면 두 시점 사이 filesystem namespace가 바뀔 수 있다.

가능하면 file descriptor 기반 operation, atomic API, `openat` 계열 context 같은 mechanism으로 check/use gap을 줄이는 설계를 사용한다.

정확한 API는 platform 문서를 확인한다.

---

## CHAPTER 17 · resource ownership은 privilege와 lifetime을 동시에 관리한다

file descriptor를 연 함수와 닫는 함수가 멀리 떨어져 있으면 leak 가능성이 커진다.

권한도 마찬가지다.

```text
acquire privileged resource
↓
perform narrow operation
↓
release/drop privilege
```

오랫동안 privilege를 유지하면 attack surface가 커진다.

RAII/context manager 같은 pattern은 resource lifetime을 lexical scope와 묶어 cleanup 누락을 줄인다.

---

## CHAPTER 18 · signal은 process control channel이지만 평범한 callback이 아니다

Unix signal handler는 임의 instruction 사이에 비동기적으로 실행될 수 있다.

모든 library function이 signal handler 안에서 안전하지 않다.

multithread process에서는 어떤 thread가 signal을 받는지도 규칙이 복잡하다.

따라서 signal handler에서 많은 일을 하지 않고 flag/eventfd/pipe 등 안전한 mechanism으로 main loop에 전달하는 pattern이 사용된다.

`Ctrl+C를 처리하는 함수` 정도로만 이해하면 production daemon의 shutdown bug를 만들 수 있다.

---

## CHAPTER 19 · graceful shutdown은 resource contract의 마지막 단계다

process 종료에도 순서가 있다.

```text
new request 수신 중단
↓
in-flight work 완료/취소 정책
↓
queue drain 또는 persist
↓
DB transaction 마무리
↓
network connection close
↓
file/log flush policy
↓
process exit
```

강제 kill과 graceful stop을 같은 것으로 보면 data loss와 duplicate job이 생길 수 있다.

### shutdown deadline

무한정 기다릴 수도 없다.

orchestrator가 termination grace period를 주는 환경에서는 deadline 안에 cleanup이 끝나도록 설계해야 한다.

못 끝낸 job을 durable queue에 되돌리는 정책이 필요할 수 있다.

---

## CHAPTER 20 · 권한 오류를 구조적으로 디버깅한다

상황: 앱이 특정 파일/서비스에 접근하지 못한다.

확인 순서:

```text
1. 실제 실패 syscall/API는 무엇인가?
2. errno/exception은 무엇인가?
3. process UID/GID/capability는?
4. namespace 내부/외부 중 어디인가?
5. path 각 component permission은?
6. mount flags/read-only 여부는?
7. SELinux/MAC denial이 있는가?
8. container/cgroup resource limit인가?
9. Android permission/service policy인가?
10. IPC caller identity check가 거절했는가?
```

`권한 줬는데 안 됨`이 아니라 어느 security gate가 거부했는지 찾는다.

---

## PART 07 종료 점검

1. library call과 system call이 왜 같은 개념이 아닌가?
2. kernel이 user pointer를 검증해야 하는 이유는 무엇인가?
3. process credential은 permission check에 어떻게 쓰이는가?
4. Linux capability가 root privilege를 더 작은 단위로 나누는 이유는 무엇인가?
5. file descriptor limit이 network error처럼 보일 수 있는 이유는 무엇인가?
6. cgroup과 namespace는 각각 무엇을 격리/제어하는가?
7. container의 PID 1과 host PID가 다를 수 있는 이유는 무엇인가?
8. namespace 안의 root가 host root와 다를 수 있는 이유는 무엇인가?
9. Android app sandbox가 앱 사이 private data를 어떻게 분리하는가?
10. Binder call이 local function처럼 보여도 remote failure를 고려해야 하는 이유는 무엇인가?
11. confused deputy는 어떤 authorization 실수인가?
12. TOCTOU가 path validation에서 어떻게 발생하는가?
13. graceful shutdown이 단순 process exit보다 복잡한 이유는 무엇인가?

권한과 격리를 `관리자냐 아니냐`로 설명하지 말고 **identity, namespace, capability, policy, resource limit, lifetime**으로 나눠 설명할 수 있어야 한다.