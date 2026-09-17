# PART 98 · Terminal, PTY and Job Control — sessions, foreground groups, termios, hangup, remote shell lifetime

터미널은 화면에 글자를 그리는 UI와 동일한 것이 아니다. Linux의 TTY 계층은 process session, process group, controlling terminal, line discipline, signal generation, input/output transformation을 묶는 kernel interface이고, pseudoterminal은 이 semantics를 master/slave device pair로 가상화한다. Shell의 `Ctrl-C`, `Ctrl-Z`, background job, SSH 세션, terminal emulator, `tmux`, container console이 서로 비슷하게 보이는 이유는 결국 같은 terminal contract를 다른 경계에서 재현하기 때문이다. 정확한 모델은 **어떤 session이 어떤 terminal을 controlling terminal로 소유하는지, 어느 process group이 foreground인지, line discipline이 bytes를 어떻게 바꾸는지, master/slave references가 언제 hangup을 만들고 signal을 누구에게 전달하는지**를 함께 추적해야 한다.

## CHAPTER 01 · Terminal endpoint와 terminal emulator는 서로 다른 층이다

사용자가 보는 terminal emulator 창은 글꼴, 색상, scrollback, escape sequence 해석을 담당하는 userspace 프로그램이고, kernel TTY는 character-device semantics와 job-control state를 제공한다. Terminal emulator는 흔히 pseudoterminal master를 잡고, shell은 대응하는 slave를 stdin/stdout/stderr와 controlling terminal로 사용한다. Shell이 `echo hi`를 실행하면 출력 bytes는 slave에 쓰이고 master를 통해 emulator가 읽은 뒤 화면에 렌더링한다. 반대로 키 입력은 emulator가 master에 쓰고 line discipline을 거쳐 foreground process가 slave에서 읽는다. **화면 표시 상태와 TTY input/output queue state는 다른 객체**이므로 scrollback에 문자가 보인다는 사실만으로 application이 그 bytes를 읽었다거나, application write가 peer terminal에 영구 반영됐다고 판단하면 안 된다.

## CHAPTER 02 · TTY는 단순 character device가 아니라 line discipline과 job-control metadata를 가진 kernel object다

일반 file descriptor처럼 TTY fd에도 `read`, `write`, `poll`, `ioctl`을 사용할 수 있지만 내부에는 terminal attributes, foreground process-group ID, window size, line discipline, input/output queues 같은 상태가 추가된다. 같은 terminal을 여러 processes가 열어도 foreground/background 규칙은 fd 하나가 아니라 terminal과 session 관계에 의해 적용된다. `isatty()`가 true라는 사실은 단지 fd가 terminal device를 가리킨다는 단서이고, 그것이 caller의 controlling terminal인지 또는 foreground 권한을 가진다는 뜻은 아니다. Debugging에서는 fd number만 기록하지 말고 device identity, session ID, process group ID, foreground PGID를 같이 확인해야 한다. **TTY correctness는 byte stream과 process hierarchy state를 동시에 다루는 문제**이며, 일반 pipe 모델만으로는 SIGTTIN·SIGTTOU·SIGHUP 같은 동작을 설명할 수 없다.

## CHAPTER 03 · Session과 process group은 shell job control의 두 단계 hierarchy를 만든다

Linux process는 하나의 process group에 속하고 process group은 하나의 session에 속한다. Pipeline `a | b | c`를 하나의 job으로 취급하려면 shell은 관련 children을 같은 process group에 넣고, session의 controlling terminal이 그 group을 foreground로 볼지 background로 볼지 전환한다. PID 하나가 foreground가 되는 것이 아니라 **process group 전체가 terminal foreground unit**이 된다. 이 구조 덕분에 `Ctrl-C`가 pipeline의 한 process만 죽이지 않고 foreground group 전체에 SIGINT를 전달할 수 있다. Process group과 session ID는 fork로 상속되고 exec 뒤에도 유지되므로 command image가 바뀌어도 job-control identity가 이어진다. Supervisor가 raw PID 하나만 보고 job lifetime을 관리하면 pipeline sibling과 group signal semantics를 놓칠 수 있다.

## CHAPTER 04 · setsid()는 새 session·새 process group을 만들고 controlling terminal 없이 시작한다

`setsid()`가 성공하면 caller는 새 session의 leader가 되고, 동시에 PID와 같은 PGID를 가진 새 process group leader가 된다. 새 session은 처음에는 controlling terminal이 없다. 이미 process-group leader인 process가 `setsid()`를 호출하면 hierarchy를 깨뜨릴 수 있으므로 실패한다. 그래서 daemonization이나 PTY child setup에서는 흔히 fork 후 child가 `setsid()`를 호출하는 구조를 사용한다. 핵심은 “setsid를 호출하면 terminal이 생긴다”가 아니라 정반대로 **기존 terminal 관계를 끊고 terminal 없는 새 session을 만든다**는 점이다. 그 뒤 특정 terminal slave를 controlling terminal로 취득할지, 아예 없는 상태로 운영할지를 별도 단계에서 결정한다. Session creation과 terminal acquisition을 하나의 operation으로 생각하면 setup race를 이해하기 어렵다.

## CHAPTER 05 · Controlling terminal은 session 단위 관계이며 session leader의 acquisition 규칙을 따른다

Session은 최대 하나의 controlling terminal을 가질 수 있고 terminal도 한 session의 controlling terminal이 된다. Session leader가 아직 controlling terminal이 없는 상태에서 terminal을 적절히 열면 controlling terminal을 취득할 수 있으며 `O_NOCTTY`는 이 자동 acquisition을 막는 데 사용된다. Linux에서는 `TIOCSCTTY` ioctl로 명시적 설정도 가능하지만 caller가 session leader이고 기존 controlling terminal이 없어야 하는 등의 조건이 있다. 이미 다른 session이 소유한 terminal을 빼앗는 동작은 privilege까지 관련된다. **Terminal fd를 열었다는 사실과 controlling-terminal 관계가 생겼다는 사실을 분리**해야 한다. Daemon·container runtime·PTY launcher는 의도하지 않은 terminal acquisition을 막기 위해 open flags와 session state를 명확하게 구성해야 한다.

## CHAPTER 06 · /dev/tty는 특정 device path가 아니라 caller의 controlling terminal을 가리키는 특수 handle이다

`/dev/tty`를 열면 process가 가진 controlling terminal을 가리키는 fd를 얻을 수 있고, controlling terminal이 없다면 open은 실패한다. stdin이 pipe나 regular file로 redirect되어 있어도 process가 session의 controlling terminal을 유지하고 있다면 `/dev/tty`를 통해 사용자 terminal과 별도로 통신할 수 있다. Password prompt나 interactive diagnostic이 redirected standard input과 무관하게 terminal을 찾는 이유가 여기에 있다. 반대로 daemon처럼 controlling terminal을 끊은 process에서 `/dev/tty`를 사용 가능하다고 가정하면 오류가 난다. **standard fd 0/1/2와 controlling terminal은 같은 개념이 아니다.** Shell redirection은 descriptors를 바꾸지만 session-level terminal association은 별도 상태로 남을 수 있으므로 보안 prompt나 background process에서 차이를 반드시 고려해야 한다.

## CHAPTER 07 · Foreground process group은 terminal 입력과 terminal-generated signals의 주 수신 단위다

Controlling terminal은 같은 session 안에서 한 번에 하나의 process group만 foreground로 지정한다. `tcgetpgrp()`는 현재 foreground PGID를 읽고 `tcsetpgrp()`는 같은 session의 nonempty process group을 foreground로 바꾼다. Interactive shell은 command를 시작할 때 child job group을 foreground로 넘기고, job이 stop되거나 끝나면 terminal foreground를 shell group으로 되돌린다. 이 handoff가 틀리면 shell이 사용자의 다음 키 입력을 못 읽거나 child와 shell이 동시에 terminal을 놓고 경쟁한다. `tcsetpgrp()` 자체도 background caller에게 SIGTTOU를 만들 수 있어 shell은 signal disposition과 순서를 조심해야 한다. **Job-control 핵심 transaction은 process 생성보다 terminal foreground ownership handoff**이며, 실패 시 원래 shell state로 복구하는 경로가 필요하다.

## CHAPTER 08 · Background read는 blocking read가 아니라 SIGTTIN이라는 job-control stop으로 바뀔 수 있다

Controlling terminal에서 background process group이 read를 시도하면 kernel은 단순 EAGAIN이나 queue wait를 제공하는 대신 해당 process group에 SIGTTIN을 보내 job을 stop시키는 job-control semantics를 적용할 수 있다. 그래서 shell에서 background command가 terminal input을 읽으려다 `Stopped (tty input)` 상태가 되는 것은 application parser bug가 아니라 terminal foreground rule의 결과다. Signal을 block/ignore한 경우와 orphaned group 등 세부 예외는 별도 처리가 필요하지만 기본 모델은 명확하다. **Terminal input 권한은 file permission이 아니라 foreground-group state에도 의존**한다. Event-driven code가 `poll()`에서 readable을 보았더라도 실제 read 시점의 job-control state가 바뀌면 예상과 다른 signal behavior를 만날 수 있으므로 session/foreground 전이를 함께 추적해야 한다.

## CHAPTER 09 · Background write는 TOSTOP 설정 여부에 따라 허용되거나 SIGTTOU로 stop될 수 있다

기본 terminal 설정에서는 background job의 출력이 terminal에 섞일 수 있지만 `TOSTOP` flag가 활성화되면 background process group의 terminal write가 SIGTTOU를 유발해 group을 stop시킬 수 있다. `tcsetpgrp()`처럼 terminal state를 바꾸는 일부 operation도 background caller에게 SIGTTOU가 적용된다. 따라서 “background는 input만 금지되고 output은 항상 된다”는 규칙은 정확하지 않다. Interactive shell과 debugger는 terminal settings를 변경할 때 자기 group이 foreground인지 확인하고, 필요한 signal mask/ignore policy를 일시적으로 적용한 뒤 복원한다. **Read/write syscall permission과 job-control permission은 별도 층**이다. TOSTOP 정책을 모르는 logging process가 controlling terminal에 직접 출력하면 production에서 갑자기 stop되는 사고도 가능하다.

## CHAPTER 10 · ISIG와 special characters는 입력 byte를 foreground group signal로 변환한다

Terminal local mode에서 `ISIG`가 켜져 있으면 VINTR, VQUIT, VSUSP 같은 special input characters가 일반 payload byte로 application에 전달되는 대신 SIGINT, SIGQUIT, SIGTSTP 같은 signal을 foreground process group에 생성할 수 있다. 흔한 `Ctrl-C`가 ASCII ETX byte를 application parser가 직접 처리해서 종료하는 것이 아닌 경우가 많은 이유다. Raw terminal mode를 만들면서 ISIG를 끄면 같은 key sequence가 ordinary byte로 들어와 application이 직접 의미를 정해야 한다. Signal-generating key는 terminal emulator→PTY master→line discipline 경로에서 해석된다. **Key press, byte value, generated signal을 같은 사건으로 합치면 안 된다.** Remote shell은 local UI에서 받은 bytes를 remote PTY master로 전달해 remote kernel의 line discipline이 job-control signal을 만들도록 하는 구조를 이해해야 한다.

## CHAPTER 11 · Canonical mode는 line discipline이 line editing과 record completion을 수행한다

`ICANON`이 켜진 canonical mode에서는 terminal driver가 입력을 line 단위로 조립하고 erase/kill 같은 editing characters를 처리한 뒤 newline·EOF 같은 조건에 따라 reader에게 record를 제공한다. Application의 `read()`가 매 key press마다 한 byte씩 즉시 돌아온다고 기대하면 안 된다. Shell command line이나 전통적인 line-oriented tool이 특별한 input editor 없이도 어느 정도 편집 동작을 얻을 수 있는 이유가 line discipline이다. Canonical input queue가 한계에 도달하면 추가 input 처리에도 제약이 생길 수 있다. **사용자가 타이핑한 bytes와 application이 읽는 bytes 사이에는 kernel transformation 단계**가 있다. Terminal program이 자체 editor를 구현하려면 canonical processing을 끄고 raw/noncanonical mode에서 필요한 editing과 UTF-8 cursor semantics를 직접 구현해야 한다.

## CHAPTER 12 · Noncanonical VMIN/VTIME은 단순 timeout 옵션이 아니라 read completion state machine을 만든다

`ICANON`을 끄면 `VMIN`과 `VTIME` 조합이 `read()`가 언제 반환되는지를 결정한다. `VMIN>0, VTIME=0`은 최소 byte 수를 기다리는 형태이고, `VMIN=0, VTIME>0`은 전체 read timeout처럼 동작할 수 있다. 둘 다 0이면 즉시 available bytes를 반환하는 polling-like behavior가 되고, 둘 다 양수면 첫 byte 이후 inter-byte timer semantics가 적용된다. 이 규칙을 socket timeout처럼 막연히 이해하면 keyboard latency나 partial escape-sequence parsing이 이상해진다. **Noncanonical read의 completion contract는 termios state의 일부**이므로 library가 VMIN/VTIME을 바꾸면 같은 fd를 읽는 다른 code의 behavior도 바뀐다. Event loop와 blocking read를 섞는 terminal application은 O_NONBLOCK과 VMIN/VTIME의 상호작용도 실제 target에서 검증해야 한다.

## CHAPTER 13 · Echo는 입력을 다시 보이는 기능이지만 secret handling에서는 보안 경계가 된다

`ECHO`와 관련 flags는 사용자가 입력한 characters를 terminal output 쪽에 반영하는 방식을 제어한다. Password prompt가 echo를 끄는 이유는 secret bytes가 화면·scrollback·recording layer로 유출되지 않게 하기 위해서다. 중요한 것은 변경 후 반드시 원래 termios를 복원해야 한다는 점이다. Prompt 중 signal/crash가 나서 echo-off 상태가 남으면 shell이 “키가 안 보이는” 상태가 되고, 반대로 restore race가 있으면 secret 일부가 노출될 수 있다. Robust program은 원본 `struct termios` snapshot을 보관하고 cleanup path에서 복원하며, signal handling에서는 async-signal-safe 제약까지 고려한다. **Terminal mode 변경은 process-local UI 설정이 아니라 shared terminal object mutation**이므로 같은 terminal을 사용하는 다른 process와의 lifetime도 생각해야 한다.

## CHAPTER 14 · cfmakeraw()는 여러 input/output/local flags를 묶어 끄는 편의 함수이지 모든 terminal policy를 없애는 마법이 아니다

Raw mode는 canonical processing, echo, signal-generation, 여러 input/output translations를 비활성화해 application이 bytes를 더 직접적으로 다루게 한다. `cfmakeraw()`는 전형적인 flag 조합을 설정하지만 baud rate, window size, foreground group, controlling-terminal relation, arbitrary ioctl state까지 초기화하지는 않는다. Raw mode에서도 PTY queue capacity와 hangup, fd blocking mode는 그대로 존재한다. Terminal UI library가 raw mode를 켠 뒤 child process를 exec하면 child가 의도하지 않은 raw state를 상속할 수 있으므로 shell/launcher는 exec 전후 termios ownership을 명확히 해야 한다. **Raw는 “kernel을 우회”하는 것이 아니라 line-discipline transformation을 줄이는 mode**다. 따라서 byte framing 책임이 application으로 이동할 뿐 resource lifetime과 job-control semantics는 사라지지 않는다.

## CHAPTER 15 · Input translation flags는 carriage return과 newline을 application bytes로 바꾸기 전에 변환할 수 있다

Terminal input processing에는 `ICRNL`, `INLCR`, `IGNCR`처럼 CR/LF 처리에 영향을 주는 flags가 있어 physical key나 remote stream에서 온 byte와 application read 결과가 달라질 수 있다. Output 쪽에도 `OPOST`와 newline-related processing이 존재해 application이 쓴 `\n`이 device 쪽에서 추가 변환될 수 있다. Network protocol parser를 TTY 위에 얹으면서 “write한 bytes가 그대로 반대편 read에 나온다”고 가정하면 이런 transformations 때문에 binary framing이 깨질 수 있다. 그래서 serial protocol이나 PTY tunnel은 필요한 flags를 명시적으로 설정한다. **Terminal은 기본적으로 transparent byte pipe가 아니다.** 어떤 transformations를 유지하고 어떤 것을 끌지 termios snapshot과 함께 protocol contract로 기록해야 한다.

## CHAPTER 16 · tcsetattr 적용 시점은 queued output과 unread input을 어떻게 다룰지 결정한다

`tcsetattr()`의 action으로 `TCSANOW`, `TCSADRAIN`, `TCSAFLUSH` 등을 선택하면 attribute 변경을 즉시 적용할지, queued output이 drain된 뒤 적용할지, input queue를 flush하면서 전환할지 behavior가 달라진다. Terminal mode를 바꾸는 순간 boundary에 old-mode bytes와 new-mode bytes가 섞이지 않도록 적절한 action을 선택해야 한다. Interactive protocol에서 canonical→raw 전환 전에 사용자가 미리 타이핑한 bytes가 queue에 남아 있으면 application이 예상하지 못한 command로 처리할 수도 있다. 반대로 무조건 flush하면 사용자의 legitimate input을 잃는다. **Termios 변경도 queue state를 포함하는 transition**이며, 단순 struct assignment처럼 생각하면 안 된다. 변경 전후에 어느 bytes가 보존·discard되는지 명시적으로 결정해야 한다.

## CHAPTER 17 · Software flow control은 ^S/^Q를 일반 data가 아니라 terminal queue 제어로 해석할 수 있다

`IXON`, `IXOFF` 같은 flags가 활성화된 환경에서는 start/stop characters가 software flow control에 사용되어 output 진행을 멈추거나 재개할 수 있다. 사용자가 실수로 `Ctrl-S`를 눌러 terminal이 “멈춘 것처럼” 보이고 `Ctrl-Q`로 풀리는 현상은 application deadlock이 아니라 이 layer일 수 있다. PTY packet mode는 flow-control state changes도 control event로 관찰할 수 있다. Binary protocol이 모든 byte 값을 payload로 써야 한다면 이런 processing을 끄지 않으면 특정 byte가 사라지거나 side effect가 생긴다. **Queue progress가 멈춘 원인을 application backpressure와 terminal flow control로 구분**해야 한다. Observability에는 fd writable 여부뿐 아니라 termios flow-control state와 packet-mode events가 도움이 된다.

## CHAPTER 18 · TIOCSTI는 input queue injection 기능이어서 현대 시스템에서는 privilege boundary 관점으로 다뤄야 한다

`TIOCSTI`는 주어진 byte를 terminal input queue에 삽입해 사용자가 입력한 것처럼 처리하게 만들 수 있다. 이런 capability는 terminal을 공유하는 privilege boundary에서 command injection에 악용될 수 있기 때문에 Linux 6.2 이후에는 `dev.tty.legacy_tiocsti` 설정이 false인 경우 CAP_SYS_ADMIN을 요구할 수 있다. 따라서 terminal automation이 오래된 `TIOCSTI` behavior를 당연한 portability contract로 기대하면 최신 hardening 환경에서 실패할 수 있다. 키 입력 자동화가 필요하면 PTY master에 data를 쓰는 명시적 endpoint 구조가 더 자연스럽다. **Input injection은 편의 기능이 아니라 authority**이므로 caller privilege, target session, kernel policy를 포함해 검토해야 한다. Hardening 정책을 배포할 때는 실제 sysctl 값과 호출 실패 errno를 기록해 legacy 허용 상태가 fleet마다 달라지지 않는지도 확인해야 한다.

## CHAPTER 19 · PTY는 master와 slave 두 virtual character devices가 terminal semantics를 사이에 두고 연결된 구조다

Pseudoterminal의 slave는 classical terminal처럼 application에 보이고 master는 terminal emulator나 SSH daemon 같은 controller가 잡는다. Master에 쓴 bytes는 slave 측 input으로 들어가 line discipline 영향을 받고, slave에 쓴 output은 master reader가 받는다. 이 구조 덕분에 physical UART 없이도 interactive shell이 job control과 termios를 그대로 사용할 수 있다. 하지만 master/slave는 단순 socket pair와 다르다. Slave에는 controlling-terminal 관계, foreground group, special-character signal generation이 붙을 수 있다. **PTY master는 remote/user-agent boundary이고 slave는 process가 terminal이라고 믿는 endpoint**다. Proxy를 구현할 때 어느 side에서 transformations와 window-size state를 적용하는지 명확히 해야 한다.

## CHAPTER 20 · UNIX 98 PTY allocation은 master open → slave permission/unlock → slave open의 lifetime protocol이다

현대 Linux에서는 `posix_openpt()` 또는 `/dev/ptmx` open으로 unused master를 얻고, 대응 slave pathname이 `/dev/pts/<n>` 형태로 생긴다. Portable sequence에서는 `grantpt()`로 slave 접근 관련 setup을 하고 `unlockpt()`로 slave lock을 해제한 뒤 `ptsname()` 계열로 이름을 얻어 slave를 연다. Master가 닫히면 대응 slave pathname lifetime도 영향을 받는다. 단계 하나를 건너뛰거나 error cleanup에서 master를 leak하면 PTY resource가 남을 수 있다. **PTY allocation은 fd 하나 생성이 아니라 pair generation 생성**으로 취급해야 한다. Master/slave identity, permissions, CLOEXEC, failure cleanup을 같은 object record에 보관하면 session setup 중간 실패를 정확히 회수할 수 있다.

## CHAPTER 21 · TIOCGPTPEER는 slave pathname lookup을 건너뛰어 namespace와 pathname race를 줄이는 Linux-specific primitive다

전통적인 sequence는 `ptsname()`으로 `/dev/pts/N` 문자열을 얻고 다시 `open()`한다. Mount namespace가 달라 해당 pathname이 보이지 않거나 lookup 경로가 security policy와 충돌할 수 있다. Linux `TIOCGPTPEER`는 master fd로부터 직접 peer slave fd를 열어 반환할 수 있고, 문서도 namespace를 고려하는 security-conscious program에서 이 방법을 권할 수 있음을 설명한다. 이 방식은 open flags도 함께 지정할 수 있어 `O_CLOEXEC` 같은 policy를 원자적으로 적용하기 좋다. **이미 확보한 master capability에서 peer capability를 파생**하면 global pathname을 다시 신뢰할 필요가 줄어든다. 다만 Linux-specific이므로 portability 요구가 있으면 fallback sequence와 차이를 분리해야 한다. Pathname fallback을 사용할 때는 ptsname 결과와 현재 devpts mount generation을 함께 검증해 잘못된 namespace의 slave를 여는 실수를 막아야 한다.

## CHAPTER 22 · TIOCPKT packet mode는 master reader에게 data뿐 아니라 terminal control-state 변화도 전달한다

PTY master에서 `TIOCPKT`를 켜면 read 결과의 첫 byte가 data/control 구분 역할을 하고, flush, output stop/start, software flow-control policy 변경 같은 terminal events를 별도 control bits로 관찰할 수 있다. Remote-login 구현이 local/remote terminal state를 맞추는 데 이런 기능을 활용해왔다. Packet mode를 켠 뒤 첫 byte를 ordinary payload로 처리하면 application data가 한 byte씩 오염된다. `poll()`의 POLLPRI 같은 exceptional readiness도 control status와 연결될 수 있다. **PTY tunnel은 bytes만 복사하는 프로그램보다 terminal control plane을 같이 전달해야 하는 경우가 있다.** 필요한 controls를 무시할지, protocol로 변환할지 명시해야 한다. Control byte를 무시하는 구현이라도 최소한 packet mode 활성 여부를 protocol metadata에 남겨 decoder가 data offset을 잘못 해석하지 않게 해야 한다.

## CHAPTER 23 · Window size는 kernel에 저장되는 terminal metadata이고 변경 시 foreground group에 SIGWINCH가 간다

`TIOCGWINSZ`와 `TIOCSWINSZ`는 rows/columns 등의 window metadata를 읽고 쓴다. Linux kernel은 일반 PTY에서 이 크기를 직접 layout에 사용하지 않지만 값이 바뀌면 foreground process group에 SIGWINCH를 보낸다. Full-screen application은 signal을 받은 뒤 새 크기를 조회하고 layout을 다시 계산한다. SSH에서는 local emulator resize를 remote protocol로 보내고 remote daemon이 remote PTY window size를 갱신해야 한다. Signal은 “새 크기 자체”를 payload로 담는 것이 아니므로 handler가 state를 다시 읽어야 한다. **Resize correctness는 UI event 전달 + kernel metadata update + foreground notification** 세 단계를 하나의 generation으로 묶는 문제다. Resize가 연속으로 들어오면 signal 횟수보다 마지막 kernel window-size state가 authoritative하므로 모든 중간 크기를 처리했다고 가정하지 않는다.

## CHAPTER 24 · Master/slave close와 terminal hangup은 EOF 하나로 축약되지 않는 lifecycle event다

PTY master가 닫히거나 physical-terminal 성격의 hangup이 발생하면 slave-side processes는 read/write error, EOF-like behavior, SIGHUP 등 여러 terminal semantics를 경험할 수 있다. 정확한 결과는 endpoint, session leadership, terminal state에 따라 달라질 수 있으므로 generic pipe EOF 모델만 적용하면 안 된다. Session leader termination 자체도 controlling terminal의 foreground group에 SIGHUP을 보낼 수 있다. Long-running daemon이 terminal에서 독립되어야 한다면 stdio fd를 redirect하는 것만으로 부족하고 session/controlling-terminal relation을 정리해야 한다. **Fd close, terminal hangup, job-control signal, business-session 종료는 서로 다른 milestone**이다. Remote shell은 master close를 child process cleanup과 연결하되, signal delivery와 reap까지 별도로 추적해야 한다.

## CHAPTER 25 · Orphaned stopped process group에는 SIGHUP과 SIGCONT가 전달되어 영구 정지된 job을 회수할 기회를 만든다

Process termination이나 reparenting으로 process group이 orphaned 상태가 되고 그 group에 stopped member가 있으면 POSIX/Linux job-control 규칙에 따라 members에 SIGHUP 뒤 SIGCONT가 전달될 수 있다. 이는 parent shell을 잃은 stopped job이 아무 관리 주체 없이 영원히 정지 상태로 남는 것을 줄이기 위한 semantics다. “부모가 죽으면 모든 children도 즉시 죽는다”는 단순 모델과 다르다. Child가 SIGHUP을 처리하거나 무시하면 계속 실행할 수도 있다. **Parent-child 관계와 process-group/session 관계는 독립 축**이며, supervisor가 terminal jobs를 관리할 때 둘을 함께 봐야 한다. P92의 reparent/reap 모델만으로 terminal stop/resume behavior를 설명하기 어려운 이유다. 상태 변화는 job generation에 묶어 기록해야 restart 뒤 오래된 stop/continue event를 새 job과 혼동하지 않는다.

## CHAPTER 26 · Interactive shell은 fork/exec보다 setpgid와 tcsetpgrp의 race를 더 조심해야 한다

Shell은 pipeline children을 생성하면서 모두 같은 PGID로 묶고, foreground job이라면 terminal foreground를 그 PGID로 넘긴 뒤 exec된 program들이 terminal을 사용하게 해야 한다. Parent와 child가 병렬로 실행되므로 어느 쪽이 먼저 `setpgid()`를 수행하는지 race가 생길 수 있어 robust shell은 허용된 transition을 양쪽에서 조정한다. Child가 너무 빨리 exec하거나 exit하면 parent의 group setup이 실패할 수 있고, shell이 terminal을 넘긴 뒤 setup error가 나면 foreground ownership을 자신에게 되돌려야 한다. **Job launch는 process graph + process group + terminal foreground를 함께 commit하는 multi-object transaction**이다. “fork 성공”만으로 launch 성공을 선언하면 terminal state가 망가질 수 있다.

## CHAPTER 27 · Remote shell은 network stream과 PTY semantics 사이를 중계하는 protocol translator다

SSH 같은 remote-login service는 단순히 client TCP bytes를 shell stdin으로 복사하지 않는다. Interactive session에서는 server 쪽 PTY pair를 만들고 child shell을 slave controlling terminal에 연결하며, daemon은 master와 network 사이를 중계한다. Client terminal size 변화, signal-like user actions, encoding/control sequence, flow control을 remote PTY semantics로 연결해야 한다. Network backpressure가 master read/write와 결합되면 child output이 terminal queue에서 막힐 수도 있다. **Remote shell의 두 flow-control domain은 network socket queue와 PTY queue**이며 서로의 stall을 bounded하게 전파해야 한다. Terminal state negotiation과 transport encryption은 또 다른 층이므로 bytes 전달 성공만으로 interactive semantics가 맞다고 볼 수 없다.

## CHAPTER 28 · Terminal emulator와 shell이 crash하면 termios restore ownership을 누가 갖는지가 사용자 경험을 결정한다

Full-screen editor나 REPL이 raw mode를 설정한 뒤 정상 종료하면 saved termios를 복원할 수 있지만 SIGKILL이나 emulator crash에서는 application cleanup이 실행되지 않는다. Shell이 같은 slave를 계속 사용한다면 terminal은 raw/no-echo 상태로 남을 수 있다. 많은 shell/tools가 `stty sane` 같은 복구 경로를 제공하는 이유다. 더 안전한 launcher는 child에게 terminal state ownership을 넘기기 전 baseline을 알고, child stop/exit 시 필요한 restore를 수행한다. 그러나 application-specific mode를 무조건 덮어쓰면 정당한 설정을 잃을 수도 있다. **Termios는 shared mutable state이므로 owner generation과 restore policy가 필요**하다. Crash recovery도 heap cleanup처럼 자동으로 kernel이 원래 값으로 되돌려주지 않는다.

## CHAPTER 29 · PTY fd inheritance와 CLOEXEC 누락은 hangup과 resource release를 예상보다 늦춘다

Master 또는 slave fd가 `fork()`/`exec()`를 거쳐 의도치 않은 helper에 남으면 원래 owner가 close해도 last-reference 조건이 성립하지 않는다. 그 결과 peer가 기대한 hangup/EOF를 받지 못하고 session shutdown이 멈출 수 있다. Socketpair나 pipe에서 본 reference-leak 문제가 PTY에서도 동일하게 나타나지만 terminal signal semantics까지 얹혀 더 복잡하다. Creation 직후부터 `O_CLOEXEC`를 사용하는 편이 race를 줄이고, child launch 시 필요한 descriptors만 명시적으로 dup해 0/1/2에 배치한 뒤 나머지를 닫아야 한다. **PTY shutdown bug는 signal bug처럼 보여도 실제 원인이 stray fd reference일 수 있다.** `/proc/<pid>/fd`와 process tree를 함께 보면 holder를 찾는 데 도움이 된다. Shutdown 진단에서는 master·slave의 예상 holder set을 기록하고 실제 fd table과 diff하면 hangup을 막는 stray reference를 빠르게 찾을 수 있다.

## CHAPTER 30 · devpts newinstance는 container마다 PTY index와 ptmx view를 분리하는 mount-level isolation이다

Linux `devpts`는 `/dev/pts` 아래 UNIX 98 slave devices를 제공한다. `newinstance` mount option을 사용하면 기존 global instance와 독립된 PTY indices를 가진 private instance를 만들 수 있어 container console isolation에 유용하다. 이때 instance별 `ptmx` node와 `/dev/ptmx` link 구성이 맞아야 container 내부 `posix_openpt()`가 의도한 instance를 사용한다. `uid`, `gid`, `mode`, `ptmxmode` options도 slave/ptmx 접근권한에 영향을 준다. **PTY namespace isolation은 PID namespace만으로 생기지 않고 devpts mount topology가 필요**하다. Container runtime이 host `/dev/pts`를 잘못 공유하면 device identity와 permission assumptions가 달라질 수 있다.

## CHAPTER 31 · TTY state는 PID namespace와 mount namespace를 가로질러 보일 수 있어 integer IDs만으로 identity를 해석하면 위험하다

Terminal foreground PGID, session ID, process IDs는 process namespace 관점에 따라 표현이 달라질 수 있고 PTY pathname은 mount namespace의 devpts instance에 따라 보이거나 안 보일 수 있다. Host supervisor와 container 내부 shell이 같은 terminal event를 서로 다른 PID/pathname으로 기록할 수 있다. TIOCGPTPEER가 mount namespace pathname visibility와 무관하게 master에서 slave fd를 얻을 수 있다는 점도 object identity가 pathname보다 강한 예다. **Terminal incident metadata는 host/container identity, namespace-relative IDs, device identity를 같이 보관**해야 한다. `/dev/pts/3`이라는 문자열만 로그에 남기면 다른 devpts instance의 `/dev/pts/3`과 혼동될 수 있다.

## CHAPTER 32 · Terminal proxy의 backpressure는 master read와 slave writer 사이에 bounded queue를 요구한다

Terminal emulator, SSH daemon, recording proxy는 PTY master에서 읽은 output을 UI/network/storage로 넘긴다. Downstream이 느린데 proxy가 무제한 userspace buffer를 쌓으면 memory pressure가 생기고, 반대로 master를 계속 읽지 않으면 slave writer가 kernel queue에서 block되어 foreground application latency가 늘어난다. Input 방향도 마찬가지로 network/UI burst를 master write capacity에 맞춰 제한해야 한다. Terminal은 interactive latency가 중요하므로 giant buffering은 throughput이 좋아 보여도 keystroke echo 지연을 키울 수 있다. **PTY proxy는 양방향 bounded queueing system**이다. Occupancy, blocked duration, bytes in/out, flow-control stop/start events를 관측해야 application hang과 proxy backpressure를 구분할 수 있다.

## CHAPTER 33 · TTY observability는 PID보다 SID·PGID·TPGID와 terminal device를 함께 봐야 한다

`ps`의 SID, PGID, TTY, TPGID 같은 필드와 `/proc/<pid>/stat`의 terminal 관련 값은 job-control 관계를 진단하는 데 유용하다. Shell이 foreground를 잃은 사고라면 단순 CPU stack보다 “caller PGID가 terminal TPGID와 같은가”, “session leader가 누구인가”, “controlling terminal이 존재하는가”가 직접적인 증거가 된다. `stty -a`는 termios state를 확인하고 `/proc/<pid>/fd`는 stray PTY references를 찾는 데 도움이 된다. **Terminal 문제는 process hierarchy graph + device state snapshot으로 진단**해야 한다. High-frequency tracing이 필요하면 signal delivery, read/write blocking, ioctl transitions를 연결하되 사용자 입력 payload 자체를 무분별하게 기록해 secret을 유출하지 않도록 한다.

## CHAPTER 34 · Terminal security review는 input injection, echo, device permission, inherited capability를 따로 검사해야 한다

Terminal은 사용자가 명령을 입력하고 때로는 password나 token을 입력하는 boundary라 보안 영향이 크다. TIOCSTI 같은 input injection authority, pathname/device permissions, devpts mount options, inherited master/slave fds, echo 설정, terminal recording 정책을 각각 검토해야 한다. Root-like process와 untrusted process가 같은 controlling terminal을 공유하는 구조는 keystroke injection과 observation 위험을 키울 수 있다. PTY master fd 자체가 강한 capability이므로 SCM_RIGHTS로 전달할 때 receiver 권한을 검증해야 한다. **Terminal 접근권한은 단순 `/dev/pts/N` mode bit 하나가 아니라 master capability와 session relation까지 포함**한다. Audit log도 raw keystrokes 대신 lifecycle metadata 중심으로 구성하는 편이 안전하다.

## CHAPTER 35 · PTY/job-control fault injection은 stop, resize, hangup, inherited fd, raw-mode crash를 각각 실제로 흔들어야 한다

검증은 shell 한 번 실행해 prompt가 보이는 것으로 끝내면 안 된다. Background child가 read해 SIGTTIN으로 stop되는지, TOSTOP 상태에서 write가 SIGTTOU를 만드는지, foreground handoff 중 child exit race를 처리하는지 확인한다. Raw/no-echo program을 강제 kill해 restore strategy를 검증하고, master fd를 의도적으로 helper에 leak해 hangup 지연 detector가 잡는지도 본다. Window resize burst로 SIGWINCH coalescing을 견디는지, devpts private instance에서 PTY path/permissions가 맞는지, TIOCSTI hardening 상태에서 fallback이 안전한지 별도 test한다. **각 failure class가 terminal state와 process groups를 baseline으로 되돌리는지 실제로 확인해야 CLEAN이라 부를 수 있다.**

## CHAPTER 36 · Terminal correctness는 bytes, process hierarchy, terminal state, reference lifetime을 한 세션 generation으로 닫는 proof다

안전한 interactive-session 구현은 다음을 답할 수 있어야 한다. Session과 process groups가 의도대로 만들어지는가. Controlling terminal owner가 명확한가. Foreground PGID handoff와 복구가 race-safe한가. Canonical/raw/echo/flow-control settings의 owner와 restore path가 있는가. SIGINT·SIGTSTP·SIGTTIN·SIGTTOU·SIGHUP·SIGWINCH가 올바른 group에 전달되는가. PTY master/slave references가 exec/fork 뒤 leak되지 않는가. Remote proxy backpressure가 bounded한가. devpts namespace가 container boundary와 맞는가. Crash/fault injection 뒤 stray jobs, stale foreground ownership, unreleased PTYs가 0으로 수렴하는가. **Terminal session은 화면 창이 아니라 kernel job-control state와 PTY references가 함께 살아 있는 resource generation이며, 그 전체 lifetime을 닫아야 correctness가 성립한다.**
