# PART 97 · UNIX Domain Socket Capability Transport — message boundaries, SCM_RIGHTS, credentials, ancillary framing, reference lifetime

UNIX domain socket은 “localhost TCP보다 빠른 소켓” 정도로 축약하면 핵심을 놓친다. AF_UNIX는 kernel 안에서 local processes를 연결하면서 stream/datagram/seqpacket semantics를 제공하고, `SCM_RIGHTS`로 open-file-description reference를 전달하며, `SCM_CREDENTIALS`·`SO_PEERCRED`로 process identity 정보를 교환할 수 있다. 이 순간 socket은 bytes channel을 넘어 capability transport가 된다. Control buffer가 작아 `MSG_CTRUNC`가 나면 fd가 일부 자동 close될 수 있고, receiver가 `MSG_CMSG_CLOEXEC`를 빼먹으면 exec race로 privileged descriptor가 새 program에 새어갈 수 있다. P97은 AF_UNIX를 **message framing, object reference transfer, credential snapshot, in-flight resource accounting**이 결합된 local security boundary로 다룬다.

## CHAPTER 01 · AF_UNIX는 IP routing을 거치지 않는 local IPC family지만 socket semantics는 그대로 유지한다

UNIX domain socket은 같은 host 안에서 processes 사이 통신을 위해 사용되며 IP address나 TCP/IP network stack의 routing identity 대신 pathname, abstract name, unnamed pair 같은 local address model을 사용한다. 그렇다고 pipe와 같은 단순 byte queue는 아니다. `socket()`, `bind()`, `listen()`, `connect()`, `accept()`, `sendmsg()`, `recvmsg()`와 readiness semantics를 그대로 갖고 stream/datagram/seqpacket type에 따라 framing과 connection model이 달라진다. 가장 큰 차이는 local kernel object를 ancillary data로 전달할 수 있다는 점이다. **AF_UNIX는 local transport + kernel object handoff protocol**로 이해해야 한다. Bytes만 주고받는 RPC와 fd/credential capability 전달을 같은 parser로 무심코 섞으면 lifetime과 security bug가 생긴다.

## CHAPTER 02 · SOCK_STREAM은 신뢰할 수 있는 byte stream이지 application message boundary를 보존하지 않는다

AF_UNIX `SOCK_STREAM`에서 sender가 `send()`를 세 번 호출했다고 receiver가 `recv()` 세 번으로 같은 조각을 받는 보장은 없다. 여러 writes가 합쳐지거나 한 write가 여러 reads로 나뉠 수 있다. 따라서 length-prefix, delimiter, fixed header처럼 application framing을 별도로 설계해야 한다. Local transport라 packet loss가 없다는 사실은 framing 문제를 해결하지 않는다. Nonblocking receiver는 partial header와 partial payload를 state machine으로 누적해야 하며 EOF가 frame 중간에 오면 protocol truncation으로 처리해야 한다. **Stream의 unit은 byte sequence이고 RPC message는 userspace가 만든다.** Ancillary data까지 함께 쓰면 어느 payload byte와 어느 control message가 논리적으로 연결되는지도 protocol에 명시해야 한다.

## CHAPTER 03 · SOCK_DGRAM은 message boundary를 보존하고 Linux AF_UNIX에서는 local datagram delivery 특성이 IP UDP와 다르다

UNIX domain datagram socket은 각 send 단위의 message boundary를 보존하며 Linux 문서는 이 domain의 datagrams가 reliable하고 reorder되지 않는다고 설명한다. 하지만 receiver buffer가 message보다 작으면 trailing data가 잘릴 수 있고 `MSG_TRUNC` 상태를 확인해야 한다. Datagram의 atomic message semantics는 RPC request 하나를 한 datagram에 넣기 편하지만 maximum practical size, socket send buffer, receiver queue pressure를 고려해야 한다. “local이니 무제한 message”는 아니다. **Reliability와 capacity는 다른 속성**이며 queue가 bounded라는 사실은 여전히 backpressure와 ENOBUFS/EAGAIN 계열 failure design을 요구한다. Message-based parser는 truncation을 정상 success로 오인하지 않아야 한다.

## CHAPTER 04 · SOCK_SEQPACKET은 connection-oriented lifetime과 record boundary를 동시에 원하는 local protocol에 적합하다

`SOCK_SEQPACKET`은 stream처럼 connection을 형성하면서 datagram처럼 message boundaries와 ordering을 보존한다. Request/response record를 직접 length-prefix로 재조립하고 싶지 않으면서 peer lifecycle과 connection admission이 필요한 local broker에 유용하다. `recvmsg()`의 `MSG_EOR` 같은 record completion 정보와 message truncation semantics를 고려할 수 있다. 그러나 library/ecosystem 지원이 stream보다 덜 흔할 수 있고 portability가 요구되면 target OS를 확인해야 한다. **Transport type 선택은 “성능”보다 framing contract와 peer lifetime model에서 시작**해야 한다. Stream framing bug를 피하려고 seqpacket을 선택할 수도 있고, protocol portability 때문에 stream을 선택할 수도 있다.

## CHAPTER 05 · socketpair()는 pathname 없이 이미 연결된 두 endpoint를 원자적으로 만들어 parent-child handoff에 강하다

`socketpair(AF_UNIX, type, 0, fds)`는 bind/listen/connect를 거치지 않고 서로 연결된 endpoints를 만든다. Fork 전 pair를 만들고 child와 parent가 불필요한 반대쪽 fd를 닫으면 control channel을 간단히 구성할 수 있다. Pathname race나 abstract-name collision이 없고, stream/datagram/seqpacket type을 선택할 수 있다. 하지만 fork 뒤 양쪽이 불필요한 descriptors를 닫지 않으면 EOF detection이 늦어진다. 한 process가 peer endpoint reference까지 계속 들고 있으면 상대가 종료해도 “모든 writers가 사라짐” 조건이 성립하지 않을 수 있다. **socketpair도 reference graph cleanup이 protocol 일부**다. CLOEXEC 설정 역시 creation 시점에 원자적으로 거는 편이 exec race를 줄인다.

## CHAPTER 06 · Pathname socket은 filesystem namespace와 permission model을 IPC admission에 사용한다

`bind()`로 `/run/myservice.sock` 같은 pathname socket을 만들면 filesystem에 socket inode가 나타나고 directory traversal permission, socket inode mode/owner가 access control에 영향을 줄 수 있다. Process umask가 생성 mode에 반영되며 `chmod/chown`으로 policy를 조정할 수 있다. 그러나 pathname이 존재한다는 것과 listener process가 살아 있다는 것은 다르다. Crash 뒤 stale socket path가 남으면 새 server의 bind가 EADDRINUSE로 실패할 수 있어 startup cleanup이 필요하다. 무조건 unlink하면 다른 live server의 path를 지울 위험이 있으므로 service ownership directory와 single-instance policy를 함께 설계해야 한다. **Pathname socket은 network endpoint이면서 filesystem object**이므로 VFS security와 process lifecycle 두 모델이 겹친다.

## CHAPTER 07 · sockaddr_un 길이는 문자열 길이와 구조체 전체 크기를 무심코 동일시하면 portability 문제가 생긴다

`struct sockaddr_un`의 `sun_path` 크기와 반환되는 address length는 구현에 따라 세부가 다를 수 있다. Linux pathname sockets에서도 null terminator 처리와 `addrlen` 계산을 명확히 해야 하며 abstract namespace는 애초에 C string이 아니다. 구조체 전체를 zero-initialize하고 실제 사용한 path 길이에 맞는 address length를 구성하는 방식이 안전하다. Kernel이 돌려준 address를 출력할 때도 `addrlen` 범위 밖 `sun_path`를 읽으면 안 된다. **Socket address는 fixed C string이 아니라 `(family, length, bytes)` 구조**로 다루는 편이 정확하다. 이 원칙은 abstract names의 embedded NUL과 unnamed sockets를 처리할 때 특히 중요하다.

## CHAPTER 08 · Abstract namespace는 filesystem path가 아니며 첫 NUL 이후 bytes 전체가 이름의 일부가 될 수 있다

Linux abstract AF_UNIX address는 `sun_path[0] == '\0'`으로 구분되고 뒤의 bytes는 지정된 `addrlen` 범위만큼 name을 이룬다. 내부 NUL도 특별한 terminator 의미를 갖지 않으므로 일반 문자열 함수로 비교/로그하면 identity를 잘못 표시할 수 있다. Filesystem inode가 없어서 umask/chmod/chown이 access control에 의미를 주지 않으며 Linux-specific extension이라 portability도 낮다. **Abstract name은 byte string이지 pathname이 아니다.** Security design이 filesystem directory permission에 의존한다면 abstract socket으로 바꾸는 순간 admission model이 달라진다. Credential check나 namespace isolation 같은 다른 controls를 명시해야 한다.

## CHAPTER 09 · Abstract socket lifetime은 pathname cleanup이 아니라 마지막 open reference lifetime에 묶인다

Pathname socket은 listener가 사라져도 directory entry가 남을 수 있어 unlink가 필요하지만 abstract socket name은 모든 open references가 닫히면 자동으로 사라진다. 이 차이는 crash recovery에는 편리하지만 name ownership을 filesystem에서 검사할 수 없다는 trade-off를 가진다. Same abstract name을 새 process가 다시 bind할 수 있는 시점도 old references의 lifetime과 연결된다. Fork/dup으로 listener fd가 예상치 못한 process에 남으면 name release가 늦어질 수 있다. **Abstract namespace의 cleanup correctness는 fd/reference cleanup correctness와 같다.** Process tree 전체의 inherited descriptors를 추적하지 않으면 “server는 죽었는데 address가 아직 사용 중” 같은 현상을 설명하기 어렵다.

## CHAPTER 10 · Autobind는 편리한 temporary identity를 만들지만 generated name을 authorization anchor로 쓰면 안 된다

Linux는 특정 bind form이나 unconnected socket의 `SO_PASSCRED` 설정 같은 조건에서 abstract namespace의 unique autobind address를 만들 수 있다. 이는 client가 explicit pathname을 관리하지 않고 local endpoint identity를 갖는 데 유용하다. 하지만 generated address는 authentication token이 아니며 finite namespace에서 생성되는 kernel-chosen locator다. Peer trust는 credentials, pre-established fd, higher-level secret 같은 별도 mechanism으로 확인해야 한다. **Address uniqueness와 peer authorization은 다른 속성**이다. Temporary endpoint name을 로그/metrics key로 쓸 때도 socket generation과 process identity를 함께 기록해야 reuse나 reconnect를 혼동하지 않는다.

## CHAPTER 11 · listen backlog는 local socket에도 admission queue가 bounded하다는 사실을 드러낸다

Pathname 또는 abstract stream/seqpacket listener가 `listen()`을 호출하면 pending connection admission이 kernel queue를 사용한다. Client가 많거나 server accept loop가 멈추면 backlog pressure가 생기고 connection attempts가 지연/실패할 수 있다. “같은 host니까 connect는 항상 즉시 성공”이라는 가정은 틀리다. Server는 accept rate, request worker capacity, per-peer auth cost를 분리해야 한다. Accept 직후 expensive authentication을 single thread에서 수행하면 queue를 비우는 속도가 줄어 overload를 확대한다. **Local IPC도 queueing system**이며 backlog와 accepted-socket limits를 observability에 넣어야 한다. DOS threat model에서는 untrusted local users가 connection slots를 고갈시킬 수 있는지도 본다.

## CHAPTER 12 · accept로 만들어진 connected socket은 listening pathname과 별도의 connection object lifetime을 가진다

Server가 pathname listener에서 connection을 accept한 뒤 listening pathname을 unlink하거나 listener를 restart해도 이미 accepted된 connection은 자체 socket object references를 따라 계속 살아 있을 수 있다. Client의 logical session은 “현재 `/run/x.sock`에 누가 bind되어 있는가”와 분리된다. Rolling restart에서 old listener generation과 new listener generation이 같은 service name을 순차 소유해도 old accepted connections가 drain될 수 있다. **Listener identity와 accepted-connection identity를 분리**해야 graceful handoff를 설계할 수 있다. Socket activation이나 fd passing으로 listener 자체를 새 process에 넘기는 경우에는 open-file-description reference가 더 직접적으로 generation을 이어준다.

## CHAPTER 13 · sendmsg/recvmsg의 msghdr는 payload와 ancillary control plane을 한 syscall에 함께 운반한다

`msghdr`는 destination/source address, iovec payload, control buffer, flags를 하나의 message operation에 결합한다. AF_UNIX에서 SCM_RIGHTS나 credentials를 전달하려면 control buffer에 `cmsghdr` records를 넣고 payload와 함께 `sendmsg()`를 사용한다. Receiver는 return byte count뿐 아니라 `msg_flags`, `msg_controllen`, 각 control header를 검사해야 한다. Payload parser가 성공했다고 control plane도 완전하다는 뜻이 아니다. **Data plane과 capability plane이 같은 syscall result 안에서 서로 다른 truncation/error semantics를 가진다.** Protocol은 “이 payload에는 fd가 정확히 몇 개 동반돼야 하는가” 같은 invariant를 명시해야 control truncation을 silent downgrade로 처리하지 않는다.

## CHAPTER 14 · cmsghdr sequence는 alignment가 있는 variable-length binary format이므로 CMSG macros로 순회해야 한다

Ancillary buffer는 `struct cmsghdr`와 그 뒤 data 영역이 alignment 규칙을 따라 연속 배치된다. `cmsg_len`만 수동으로 더해 next header를 찾으면 architecture별 padding을 놓칠 수 있으므로 `CMSG_FIRSTHDR`, `CMSG_NXTHDR`, `CMSG_DATA`, `CMSG_LEN`, `CMSG_SPACE` macros를 사용하는 것이 기본이다. Receive buffer를 `char[]` 하나로 선언할 때도 cmsghdr alignment가 보장되도록 union/적절한 storage를 쓰는 패턴이 흔하다. Malformed/unexpected control types도 parser가 bounds를 확인해야 한다. **Ancillary data는 kernel이 주는 작은 TLV-like binary stream**으로 취급하고, payload struct casting만큼 엄격하게 길이와 alignment를 검증해야 한다.

## CHAPTER 15 · SCM_RIGHTS는 “fd 정수 값”이 아니라 open file description에 대한 reference를 receiver table에 복제한다

Sender가 fd 7을 보냈다고 receiver도 fd 7을 얻는 것은 아니다. 전달되는 의미는 sender fd가 가리키던 open file description에 대한 reference이고 receiver는 자기 fd table의 빈 번호를 할당받는다. 따라서 shared file offset, status flags처럼 open-file-description에 속하는 state는 전달 후에도 sender/receiver가 같은 underlying description을 공유할 수 있다. 이것은 단순 filename 전달보다 훨씬 강하다. Already-open file, socket, pidfd, memfd 같은 capability를 namespace lookup 없이 직접 넘길 수 있다. **SCM_RIGHTS는 integer serialization이 아니라 kernel object capability duplication**이며, 받은 object의 type과 권한을 검증해야 한다.

## CHAPTER 16 · Received fd의 lifetime은 sender close와 독립적이어서 capability transfer는 실제 ownership handoff가 될 수 있다

`sendmsg()`가 SCM_RIGHTS reference를 queue에 넣은 뒤 sender가 원래 fd를 close해도 receiver가 정상적으로 reference를 받으면 underlying object는 receiver fd로 계속 살아 있을 수 있다. 완전 handoff를 원한다면 sender는 successful send 뒤 자신의 reference를 닫고 receiver가 protocol ACK로 acceptance를 확인하도록 설계할 수 있다. 반대로 둘 다 reference를 유지하면 shared ownership이다. Object가 socket/file/memfd인지에 따라 마지막-close effect가 다르므로 ownership mode를 message schema에 명시해야 한다. **Capability send 성공과 receiver application acceptance는 다른 milestone**이다. Queue에 들어갔지만 peer가 죽으면 delivery outcome을 상위 protocol에서 판단해야 한다.

## CHAPTER 17 · MSG_CMSG_CLOEXEC는 received descriptors의 exec leak window를 syscall 수준에서 닫는다

Receiver가 `recvmsg()`로 fd를 받은 뒤 별도 `fcntl(F_SETFD, FD_CLOEXEC)`를 호출하는 사이 다른 thread가 `execve()`를 수행하면 privileged fd가 새 program image로 유출될 수 있다. `MSG_CMSG_CLOEXEC`는 SCM_RIGHTS로 받은 descriptors에 close-on-exec를 receive 시점에 설정하여 이 race를 줄인다. Multi-threaded daemon에서 받은 fd의 기본 정책은 “명시적으로 상속할 이유가 없다면 CLOEXEC”가 안전하다. **CLOEXEC를 사후 설정이 아니라 descriptor creation/receive의 atomic property로 만드는 것**이 중요하다. P71의 descriptor inheritance 원칙이 capability reception에도 그대로 적용된다.

## CHAPTER 18 · MSG_CTRUNC는 control data 일부가 사라졌다는 신호이므로 payload를 정상 request로 처리하면 안 된다

Receiver가 제공한 `msg_control` buffer가 작거나 NULL이면 ancillary data가 truncate/discard될 수 있고 `MSG_CTRUNC`가 설정된다. SCM_RIGHTS의 경우 buffer에 담기지 못한 excess descriptors는 kernel이 receiver 쪽에서 자동으로 close할 수 있다. 그래서 payload bytes가 온전히 도착했더라도 protocol이 fd 2개를 요구하는 request라면 MSG_CTRUNC는 전체 message failure로 취급해야 한다. 그렇지 않으면 첫 fd만 사용해 권한/역할이 뒤바뀌거나 fallback path가 예상치 못한 file을 연다. **Control truncation은 optional metadata loss가 아니라 capability schema violation**일 수 있다. 충분한 CMSG_SPACE를 사전 계산하고 return flags를 반드시 검사해야 한다.

## CHAPTER 19 · MSG_TRUNC와 MSG_CTRUNC는 서로 다른 손실을 나타내므로 하나의 “truncated” boolean으로 합치지 않는다

Message-based sockets에서 payload buffer가 작으면 `MSG_TRUNC`가 normal data 손실을 나타낼 수 있고 control buffer가 작으면 `MSG_CTRUNC`가 ancillary 손실을 나타낸다. 두 flags는 동시에 또는 독립적으로 발생할 수 있다. Payload length를 재시도할 수 있는 protocol이라도 SCM_RIGHTS는 이미 일부가 자동 close됐을 수 있어 “같은 message를 다시 읽으면 된다”는 stream-like 복구가 성립하지 않는다. Datagram/seqpacket에서는 oversized message tail이 discard될 수 있다. **Data loss와 capability loss를 별도 error class로 기록**해야 한다. Sender가 idempotent retry할 수 있도록 message ID와 ownership ACK protocol을 설계하는 편이 안전하다.

## CHAPTER 20 · SCM_MAX_FD는 한 ancillary message에 무제한 capability를 넣지 못하게 하는 hard interface limit다

Linux는 한 SCM_RIGHTS control message에 전달할 수 있는 descriptors 수를 `SCM_MAX_FD`로 제한하며 현재 man-page는 253이라는 값을 설명한다. Application이 dynamic batch size를 계산하지 않고 수백/수천 fds를 한 번에 넘기려 하면 `sendmsg()`가 EINVAL로 실패할 수 있다. 더 큰 logical transfer가 필요하면 여러 messages로 나누고 sequence/transaction framing을 추가해야 한다. 일부 batch만 성공한 뒤 failure가 나면 sender와 receiver ownership이 갈릴 수 있으므로 all-or-nothing처럼 착각하면 안 된다. **Capability batch도 partial multi-message transaction**이며 각 batch의 accepted generation을 추적해야 한다.

## CHAPTER 21 · In-flight fd accounting은 send 후 recv 전 구간이 system resource라는 사실을 드러낸다

SCM_RIGHTS로 socket queue에 들어갔지만 아직 receiver `recvmsg()`가 받아 fd table에 설치하지 않은 descriptors는 in-flight state다. Linux는 unprivileged sender가 자신의 `RLIMIT_NOFILE`을 우회해 무한히 references를 queue에 쌓지 못하도록 in-flight descriptors를 제한하며 초과 시 `ETOOMANYREFS`가 발생할 수 있다. 과거 kernel에서는 이 accounting이 약해 exploit-like resource accumulation이 가능했다. **Sender의 local fd count만 세면 capability references 총량을 놓친다.** Broker는 queued/in-flight batch 수와 peer read progress를 bounded하게 관리하고 ETOOMANYREFS를 backpressure/peer-stall 신호로 관측해야 한다.

## CHAPTER 22 · Receiver RLIMIT_NOFILE 초과 시 excess received descriptors가 자동 close될 수 있어 count invariant를 확인해야 한다

Ancillary buffer는 충분해도 receiver가 새 descriptors를 설치하면 `RLIMIT_NOFILE`을 넘는 경우 excess fds가 자동으로 close될 수 있다. 따라서 sender가 20개를 보냈다는 사실만으로 receiver가 20개를 소유했다고 가정하면 안 된다. Receiver는 실제 cmsg data 길이와 protocol expected count를 검증하고 부족하면 message 전체를 reject/cleanup해야 한다. 성공적으로 설치된 일부 fds도 error path에서 명시적으로 close해야 leak이 없다. **Capability reception은 resource admission을 포함하는 operation**이며 process fd budget이 protocol correctness에 직접 영향을 준다. Low-fd fault injection을 반드시 테스트해야 한다.

## CHAPTER 23 · SCM_CREDENTIALS는 sender가 claims를 넣을 수 있지만 kernel이 권한에 따라 그 claims를 검증한다

`SCM_CREDENTIALS`는 `struct ucred`의 PID/UID/GID를 ancillary data로 전달한다. Sender가 자기 credentials와 다른 값을 임의로 사칭할 수 있는 것은 아니며 kernel은 PID/UID/GID가 허용되는 값인지 capabilities에 따라 검증한다. Privileged processes는 더 넓은 값을 지정할 수 있으므로 receiver가 “kernel-checked니까 언제나 actual caller identity와 동일”이라고 단순화해서도 안 된다. Authentication policy는 credential fields와 namespace/capability context를 함께 이해해야 한다. **Credential message는 cryptographic identity token이 아니라 local kernel credential assertion**이다. Connection/message freshness와 authorization database mapping은 application이 담당한다.

## CHAPTER 24 · SO_PASSCRED는 이후 received messages에 sender credentials를 자동으로 동반시키는 receiver-side policy다

Receiver가 `SO_PASSCRED`를 enable하면 subsequently received messages에 SCM_CREDENTIALS를 받을 수 있고 sender가 explicit credentials를 넣지 않았다면 kernel이 default sender PID/real UID/real GID 정보를 제공한다. Datagram broker처럼 message마다 sender identity를 확인해야 하는 protocol에 유용하다. Setting 자체가 peer authorization을 수행하는 것은 아니므로 every message에서 credential control data 존재와 예상 policy를 확인해야 한다. Socket이 아직 connected/bound되지 않은 상황에서는 Linux autobind behavior와도 연결될 수 있다. **Credential collection enable과 authorization decision을 분리**해야 한다. Credentials를 로그만 하고 access check를 payload의 self-declared user ID로 하면 의미가 없다.

## CHAPTER 25 · SO_PEERCRED는 connected peer의 credential snapshot을 connection setup 시점과 연결한다

Connected AF_UNIX stream/socketpair에서 `SO_PEERCRED`는 peer PID/UID/GID를 조회하는 간단한 authentication primitive다. Linux 문서는 returned credentials가 connect/listen/socketpair 관련 시점에 유효했던 값이라고 설명한다. Peer process가 이후 credential을 변경하거나 exec해도 “현재 호출 thread의 live credentials”를 매번 재평가하는 API로 보면 안 된다. Long-lived connection에서 privilege transition이 authorization에 중요하면 protocol-level reauthentication 또는 connection recreation이 필요할 수 있다. **SO_PEERCRED는 connection identity snapshot이지 continuous attestation이 아니다.** PID reuse를 장기 identity로 저장하는 문제도 별도로 고려해야 한다.

## CHAPTER 26 · User namespace가 있으면 numeric UID/PID를 host-global identity처럼 해석하면 authorization이 틀어진다

Container/user namespace 환경에서는 process가 보는 UID/PID와 initial namespace에서의 identity가 다를 수 있다. SCM_CREDENTIALS나 peer credential을 정책 database와 비교할 때 어느 namespace의 numeric identity인지 이해해야 한다. `uid 0`이라는 숫자가 곧 host root를 의미하지 않을 수 있고, credential mapping이 없는 value는 representation에서 특별 처리될 수 있다. Host broker가 containers를 service할 때는 namespace/container identity를 별도 metadata로 결합하는 편이 안전하다. **Local credential 숫자와 security principal을 동일시하지 않는 것**이 namespace 시대 AF_UNIX authentication의 핵심이다. Capability checks도 governing user namespace를 고려한다.

## CHAPTER 27 · SO_PASSSEC/SCM_SECURITY와 SO_PEERSEC는 MAC security label을 IPC authorization에 결합할 수 있다

SELinux 같은 LSM 환경에서는 peer socket의 security context를 ancillary data 또는 socket option으로 받아 policy decision과 audit에 사용할 수 있다. UID/GID가 같아도 domain label이 다른 services를 구분해야 하는 systems에서 유용하다. 하지만 security context string buffer sizing, null termination, policy reload, unsupported platform fallback을 처리해야 하며 label을 application이 임의 문자열로 생성해서는 안 된다. **Discretionary credential과 mandatory security label은 서로 다른 authorization dimensions**다. Portable program은 Linux/LSM-specific path와 일반 fallback policy를 명확히 분리해야 한다.

## CHAPTER 28 · File descriptor passing은 pathname-based access check를 건너뛰는 의도적인 capability transfer다

Receiver가 직접 `open("/secret/x")`할 permission이 없어도 privileged broker가 이미 연 fd를 SCM_RIGHTS로 넘기면 receiver는 그 open file description이 허용하는 operations를 수행할 수 있다. 이것이 capability architecture의 장점이지만 잘못된 broker validation은 privilege escalation이 된다. Broker는 request path를 다시 열어주는 API보다 prevalidated object capability를 최소 권한으로 전달할 수 있고, 받은 side는 arbitrary path traversal 권한이 필요 없다. **SCM_RIGHTS는 access-control 우회가 아니라 권한 위임 primitive**다. 어떤 object/flags/offset state를 위임하는지 security review와 audit가 필요하다.

## CHAPTER 29 · Open file status flags와 shared offset은 전달된 capability의 hidden mutable state가 될 수 있다

SCM_RIGHTS가 open file description reference를 공유하므로 sender와 receiver가 같은 file offset을 전진시키거나 `O_APPEND`, `O_NONBLOCK` 같은 description-level status flags의 영향을 공유할 수 있다. Receiver가 independent cursor를 기대하면 `dup`과 같은 semantics가 오히려 race를 만든다. Independent state가 필요하면 broker가 별도로 `open()`한 description을 전달하거나 receiver가 appropriate reopen mechanism을 사용해야 한다. Socket fd를 넘기면 queue/protocol state 자체가 공유되는 더 강한 handoff가 된다. **Capability type뿐 아니라 attached mutable open-description state를 contract에 포함**해야 한다. “read-only fd”라는 말만으로는 offset ownership이 설명되지 않는다.

## CHAPTER 30 · Privileged process가 preconfigured socket을 넘기면 receiver에 raw privilege 대신 제한된 capability만 위임할 수 있다

Privileged setup process가 raw/network socket을 만들고 필요한 filter를 설치한 뒤 `SO_LOCK_FILTER`로 filter 변경을 막고, 그 fd를 AF_UNIX SCM_RIGHTS로 unprivileged worker에게 넘기는 pattern이 가능하다. Worker는 socket object를 사용할 수 있지만 새 raw socket을 만들 전체 capability를 가질 필요가 없다. 이것은 least privilege를 object capability로 구현하는 좋은 예다. 그러나 filter가 실제 threat model을 충분히 제한하는지와 worker가 inherited options를 변경할 수 있는지를 검토해야 한다. **권한을 process-wide capability bit로 주는 대신 preconfigured kernel object reference를 전달**하면 compromise blast radius를 줄일 수 있다.

## CHAPTER 31 · Stream socket에서 ancillary data는 최소 하나의 real data byte와 함께 protocol framing해야 한다

Linux AF_UNIX stream socket에서 SCM_RIGHTS나 credentials 같은 ancillary data를 성공적으로 전달하려면 같은 `sendmsg()`에 적어도 하나의 nonancillary data byte를 포함해야 한다. Receiver도 그 byte와 control data의 logical association을 고려해야 한다. 여러 capabilities를 연속 전송하는 stream protocol에서 payload framing 없이 dummy byte만 쓰면 어느 control message가 어느 request와 연결되는지 code가 혼란스러워질 수 있다. Message type/header byte와 ancillary schema를 함께 정의하는 편이 낫다. Datagram은 이 요구가 다를 수 있으므로 transport type을 바꿀 때 protocol assumptions를 재검토한다. **Ancillary control은 stream 밖의 별도 channel이 아니라 stream message operation에 동반되는 metadata**다.

## CHAPTER 32 · recvmsg에서 control records를 처리하기 전에 payload만 dispatch하면 capability와 request가 분리될 수 있다

Event loop가 `recvmsg()` 결과를 받은 뒤 payload를 worker queue에 먼저 보내고 ancillary fds를 나중에 global table에 넣는 구조는 concurrency race를 만든다. Worker가 request를 처리할 때 required capability가 아직 등록되지 않았거나, 다음 message의 fd와 잘못 매칭될 수 있다. Receive thread는 한 syscall에서 얻은 payload, credentials, rights를 하나의 immutable message object로 묶고 schema validation을 끝낸 뒤 dispatch하는 편이 안전하다. Error 시 그 message에 포함된 모든 received fds를 close해야 한다. **Kernel message boundary에서 capability ownership을 application message ownership으로 원자적으로 승격**하는 단계가 필요하다. 특히 seqpacket/datagram은 이 구조가 자연스럽다.

## CHAPTER 33 · Peer shutdown과 EPIPE는 capability delivery와 business acceptance를 별도 상태로 남긴다

Stream peer가 닫힌 뒤 send하면 EPIPE와 SIGPIPE가 발생할 수 있고 `MSG_NOSIGNAL`로 signal side effect를 피할 수 있다. 하지만 과거 `sendmsg()`가 성공해 SCM_RIGHTS를 queue에 넣었다고 receiver application이 실제 capability를 사용하기 시작했다는 보장은 없다. Sender가 object ownership을 완전히 포기해야 하는 protocol이면 receiver ACK가 필요할 수 있다. Peer crash 시 in-flight references는 kernel cleanup을 거쳐 release되지만 business transaction이 commit됐는지는 별도다. **Socket send completion, fd queueing, recv installation, application acceptance를 서로 다른 milestone**로 추적해야 capability loss나 double-use를 막을 수 있다.

## CHAPTER 34 · AF_UNIX observability는 pathname보다 endpoint generation과 capability counts를 봐야 한다

`ss -x`, `/proc/net/unix`, fd tables는 local socket endpoints와 references를 조사하는 데 도움을 준다. Pathname/abstract name만 기록하면 rolling restart에서 old/new listener generation을 구분하기 어렵고 unnamed socketpair는 이름 자체가 없다. Service telemetry는 listener generation, peer credential, accepted connection count, queued/in-flight SCM_RIGHTS count, MSG_CTRUNC/ETOOMANYREFS/RLIMIT errors를 기록하는 편이 유용하다. Received fd type을 `fstat`, socket options, seals 등으로 검증한 결과도 audit에 연결할 수 있다. **Local IPC 장애는 network packet loss보다 reference leak, peer stall, credential mismatch, ancillary truncation으로 나타날 수 있으므로 관측 축도 달라야 한다.**

## CHAPTER 35 · Capability-transport fault injection은 low fd limit, truncated control buffer, exec race, peer crash를 실제로 흔들어야 한다

정상적으로 fd 하나 보내고 읽는 test만으로는 부족하다. Receiver control buffer를 작게 만들어 `MSG_CTRUNC`와 auto-close를 확인하고, RLIMIT_NOFILE을 낮춰 일부 capability admission failure를 만든다. Sender가 많은 fds를 recv 없이 밀어 ETOOMANYREFS backpressure를 확인하고, receive와 concurrent exec를 섞어 `MSG_CMSG_CLOEXEC` 없는 구현이 leak되는지 isolated child에서 검증한다. Abstract listener reference를 fork child에 남겨 lifetime retention도 확인한다. Credentials는 namespace/privilege 조건별로 test한다. **각 failure mode에서 leaked fd가 0이고 ownership ledger가 sender/receiver 중 정확히 한 쪽 또는 의도된 shared 상태로 수렴하는지** 확인해야 한다.

## CHAPTER 36 · UNIX-domain capability transport의 최종 proof는 bytes·identity·kernel-object ownership을 한 message generation으로 닫는 것이다

안전한 AF_UNIX protocol은 다음을 답할 수 있어야 한다. Stream인지 record-preserving transport인지 framing이 맞는가. Pathname과 abstract address의 permission/lifetime 차이를 아는가. SCM_RIGHTS가 fd number가 아니라 open-file-description reference를 전달한다는 것을 반영하는가. 받은 fds에 CLOEXEC가 원자적으로 설정되는가. MSG_CTRUNC/MSG_TRUNC를 구분하고 partial capabilities를 모두 cleanup하는가. In-flight/receiver fd limits가 bounded한가. Credential snapshot과 namespace를 올바르게 해석하는가. Capability가 최소 권한 object인지 검증하는가. Peer crash 뒤 delivery와 ownership을 구분하는가. **AF_UNIX correctness는 local bytes 전송 성공이 아니라 payload, peer identity, transferred references가 같은 protocol state machine에서 정확히 생성·수락·폐기된다는 증거**다.
