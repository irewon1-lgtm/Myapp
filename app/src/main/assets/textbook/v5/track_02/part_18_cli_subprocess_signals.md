# PART 18 · CLI·subprocess·signal — 프로그램을 운영체제의 process 경계와 연결하기

함수와 module 안에서만 코드를 보다가 실제 프로그램을 실행하면 argument, exit code, standard input/output, environment variable, child process, signal이라는 운영체제 경계를 만나게 된다. CLI 프로그램은 화면 없는 작은 앱이 아니라 **process가 외부 세계와 맺는 계약을 가장 직접적으로 보여 주는 인터페이스**다. 이 경계를 정확히 이해하면 자동화 script와 build tool, worker, server startup을 더 안전하게 다룰 수 있다.

---

## CHAPTER 01 · command-line argument는 문자열 배열이며 parsing과 의미 검증을 분리한다

Shell에서 `app --limit 10 input.txt`처럼 실행한 명령은 process에 argument sequence로 전달된다. 프로그램은 먼저 option 이름과 value를 구조적으로 parse하고, 그 뒤 `limit`이 실제로 허용 범위인지, input path가 업무 규칙에 맞는지 검증한다. 문자열을 수동 index로 해석하기보다 표준 argument parser를 사용하면 help, required option, type conversion, error message를 일관되게 처리할 수 있다.

Option 설계는 public API 설계와 같다. Flag 이름을 바꾸거나 default를 바꾸면 automation script가 깨질 수 있다. `--force`처럼 boolean flag가 정확히 어떤 safety check를 건너뛰는지 문서화해야 하며, 위험한 operation은 explicit confirmation이나 dry-run과 결합할 수 있다.

Position argument와 option의 관계도 명확하게 한다. File이 여러 개인지 하나인지, `-`가 stdin을 뜻하는지, path와 pattern 순서가 고정인지 정한다. Ambiguous parser behavior를 줄일수록 shell script에서 예측 가능하다.

Parsing 성공은 실행 가능성을 보장하지 않는다. `--workers 0`, 존재하지 않는 config profile처럼 domain validation은 별도 단계에서 실패시켜야 한다. Startup 초기에 모든 configuration을 검증하면 긴 작업 중간의 늦은 실패를 줄인다.

---

## CHAPTER 02 · exit code는 사람이 아닌 호출 process와 맺는 결과 계약이다

CLI가 화면에 “실패”라고 출력해도 exit status가 0이면 shell, scheduler, CI는 성공으로 판단할 수 있다. 반대로 정상 결과를 출력한 뒤 non-zero로 종료하면 자동화가 실패로 처리한다. Exit code는 stdout message와 별개의 machine-readable contract다.

일반적으로 0은 성공, non-zero는 실패 범주를 나타내지만 application이 여러 code를 정의할 수도 있다. 너무 세분화하면 호출자가 모든 숫자를 알아야 하고, 너무 단순하면 retry 가능한 실패와 사용자 입력 오류를 구분하기 어렵다. 외부 automation이 실제로 필요한 수준의 범주를 제공한다.

Exception이 top level까지 올라가 interpreter 기본 traceback과 non-zero exit로 끝나는 것은 개발 중 유용하지만 production CLI에서는 expected error를 깔끔한 message와 안정된 code로 번역할 수 있다. Unexpected defect는 diagnostic trace를 보존하면서 안전하게 실패시킨다.

`sys.exit` 같은 호출을 domain logic 깊숙한 곳에서 사용하면 library로 재사용하기 어렵다. Core 함수는 결과나 exception으로 의미를 반환하고 entry point가 이를 exit code로 변환하는 구조가 더 유연하다.

---

## CHAPTER 03 · stdout과 stderr는 서로 다른 데이터 채널이다

CLI가 생성한 실제 데이터는 stdout에 쓰고 progress, warning, diagnostic은 stderr에 보내면 shell pipeline이 결과만 다른 command로 전달할 수 있다. 모든 메시지를 stdout에 섞으면 `app | parser` 같은 조합에서 설명 문구가 데이터로 들어가 오류를 만든다.

Structured output mode가 필요하다면 JSON Lines나 CSV처럼 format을 명시하고 사람이 읽는 decoration을 끈다. Human output과 machine output을 같은 문자열 layout으로 억지로 맞추면 둘 다 불편해질 수 있다. `--json` 같은 mode를 public contract로 제공할 수 있다.

Output encoding과 terminal capability도 고려한다. Redirect된 file과 interactive terminal은 같은 device가 아니다. Color escape sequence와 progress animation은 TTY에서만 사용하고 pipe/file에서는 plain output을 선택할 수 있다.

대용량 stdout은 consumer가 느리면 pipe buffer가 가득 차 producer가 block될 수 있다. Output도 stream이므로 backpressure가 있다. Child process를 제어할 때 stdout/stderr를 둘 다 pipe로 잡고 한쪽만 읽으면 다른 buffer가 가득 차 deadlock이 생길 수 있다.

---

## CHAPTER 04 · stdin은 interactive input과 pipeline input을 모두 받을 수 있다

`input()`으로 사람에게 묻는 방식은 interactive terminal에서는 편리하지만 automation에서는 대답할 사람이 없다. CLI가 stdin을 data stream으로도 사용할 수 있다면 prompt와 data 채널이 충돌하지 않게 설계한다. Non-interactive mode에서는 필요한 값이 없으면 즉시 실패시키는 편이 안전하다.

Password나 secret을 받을 때 command-line argument로 넘기면 process list와 shell history에 노출될 수 있다. Environment variable도 완전한 secret store는 아니며 process dump나 child inheritance에 영향을 받을 수 있다. 가능한 경우 dedicated secret input, file descriptor, platform credential mechanism을 사용한다.

Stream input은 EOF가 정상 종료 신호일 수 있다. 사용자 키보드 입력과 pipe input에서 EOF 발생 방식이 다르지만 parser는 같은 stream contract로 처리할 수 있다. 한 번에 전체 stdin을 읽을지 line/chunk로 streaming할지는 데이터 크기에 맞춰 결정한다.

Interactive confirmation을 안전 장치로 사용하는 command도 `--yes` 같은 automation override를 제공할 수 있지만 위험 범위를 명확히 한다. 사람이 없는 환경과 사람이 있는 환경의 UX를 분리한다.

---

## CHAPTER 05 · subprocess 실행은 shell command 문자열과 argv 실행을 구분해야 한다

다른 프로그램을 실행할 때 가장 중요한 security 경계 중 하나는 shell을 거치는지 여부다. 사용자 입력을 포함한 문자열을 shell에 넘기면 `;`, pipe, redirection 같은 shell syntax가 code로 해석될 수 있어 injection 위험이 생긴다. 가능하면 executable과 argument를 별도 sequence로 전달해 shell parsing을 건너뛴다.

Shell 기능이 실제로 필요한 경우에도 trusted template과 untrusted data를 분리하고 platform-specific quoting을 정확히 이해해야 한다. 문자열 escape를 직접 조합하는 것보다 shell을 사용하지 않는 구조가 훨씬 단순하다.

Child process의 current directory, environment, stdin/out/err, timeout을 명시하면 실행 재현성이 높아진다. Parent environment 전체를 무조건 상속하면 secret과 설정이 의도치 않게 child로 전달될 수 있다.

Executable resolution도 security와 재현성에 영향을 준다. PATH에서 같은 이름의 다른 binary가 먼저 선택될 수 있다. 중요한 tool은 검증된 absolute path나 controlled environment를 사용할 수 있다.

---

## CHAPTER 06 · child process 결과는 exit code·stdout·stderr·timeout을 함께 본다

Subprocess가 끝났다는 사실만으로 성공이라고 판단하지 않는다. Exit code를 확인하고, 필요한 stdout을 parse하며, stderr는 diagnostic으로 보존한다. 어떤 tool은 warning을 stderr에 출력하면서 성공 code를 반환할 수도 있으므로 channel 이름만으로 성공/실패를 결정하지 않는다.

Timeout이 발생하면 child를 terminate한 뒤 실제 종료를 기다리고 pipe와 resource를 정리해야 한다. Parent만 timeout exception을 던지고 child가 background에 남으면 orphan process와 file lock이 쌓일 수 있다. Process tree 전체를 중단해야 하는 command에서는 child가 다시 child를 만든 경우도 고려한다.

Output capture에 크기 제한이 필요할 수 있다. 악성 또는 잘못된 child가 무한 stdout을 내보내면 parent memory가 고갈될 수 있다. File이나 streaming consumer로 보내거나 최대 크기를 둔다.

실패 report에는 command의 secret argument를 그대로 포함하지 않는다. Reproduction에 필요한 executable/version과 safe argument만 남기고 credential은 redact한다.

---

## CHAPTER 07 · signal은 process lifetime에 비동기적으로 전달되는 제어 사건이다

운영체제 signal은 종료 요청, interrupt, child 상태 변화 같은 사건을 process에 전달하는 mechanism이다. `Ctrl+C`가 보통 interrupt signal과 연결되는 것처럼 사용자는 정상 shutdown을 요청할 수 있다. Signal handler는 application의 shutdown state machine에 사건을 전달하는 역할로 제한하는 편이 안전하다.

Signal은 ordinary function call과 다른 시점에 도착할 수 있으므로 handler 안에서 복잡한 I/O나 lock acquisition을 수행하는 것은 위험할 수 있다. Runtime과 platform이 허용하는 안전한 operation 범위를 확인하고, 일반 실행 흐름이 cleanup을 수행하게 flag/event만 설정할 수 있다.

첫 종료 signal에서는 graceful shutdown을 시작하고 일정 시간 뒤 강제 종료를 허용하는 정책을 둘 수 있다. Server는 신규 request 수락을 중단하고 in-flight 작업을 마치며 queue acknowledgement와 log flush를 처리한다.

Signal semantics는 platform 차이가 크다. Portable application은 특정 Unix signal을 모든 환경에 있다고 가정하지 않고 target deployment environment에 맞춰 구현한다.

---

## CHAPTER 08 · graceful shutdown은 새로운 작업 차단→진행 작업 정리→resource 해제 순서로 본다

Shutdown을 단순 `exit()`로 처리하면 진행 중인 transaction, temporary file, message acknowledgement가 중간 상태에 남을 수 있다. 먼저 신규 작업을 받지 않게 하고, 이미 시작한 작업이 완료하거나 cancellation-safe 지점에서 중단되도록 한 뒤 resource를 닫는 순서를 설계한다.

모든 작업을 무한히 기다릴 수는 없으므로 graceful deadline을 둔다. Deadline 안에 끝나지 않는 task는 강제 종료할 수 있지만 그 뒤 replay/recovery가 가능하도록 idempotency와 durable state가 필요하다.

Worker queue에서는 가져온 메시지를 언제 ack하는지가 중요하다. 처리 전에 ack하면 crash 시 작업이 유실될 수 있고, 처리 후 ack하면 retry 시 duplicate가 생길 수 있다. Business operation의 idempotency와 queue semantics를 함께 설계한다.

Shutdown test도 필요하다. 정상 path만 테스트하면 실제 deploy나 machine restart에서만 data loss가 나타날 수 있다. 작업 중 signal, network wait 중 cancellation, lock 보유 중 종료 같은 failure injection을 검증한다.

---

## CHAPTER 09 · environment variable은 문자열 namespace이며 configuration source 중 하나다

Environment variable은 deployment마다 다른 설정을 process에 전달하기 편하지만 타입이 없는 문자열이다. Startup에서 required 여부, parsing, range를 검증해 typed configuration으로 바꾼다. Boolean을 `"false"`라는 문자열이 truthy라는 이유로 true로 처리하는 식의 오류를 피한다.

변수 이름 충돌과 prefix도 고려한다. 여러 application이 같은 host에서 실행될 때 generic `PORT`, `DEBUG`가 어느 component용인지 모호할 수 있다. 명확한 prefix와 schema를 갖고 unknown configuration을 경고하거나 거부할 수 있다.

Environment를 log로 전체 dump하면 secret이 노출될 수 있다. Config diagnostic은 key 목록과 redacted value, source 정도만 안전하게 출력한다. Child process에 environment를 전달할 때도 필요한 값만 선택한다.

Runtime 중 environment variable을 바꾼다고 이미 읽어 만들어진 configuration object가 자동 갱신되지는 않는다. Startup snapshot인지 dynamic read인지 정책을 고정해 같은 setting이 호출마다 다르게 보이지 않게 한다.

---

## CHAPTER 10 · CLI는 작지만 완전한 application architecture를 연습할 수 있다

잘 설계된 CLI entry point는 argument parsing과 configuration loading을 하고, core service를 구성한 뒤 operation을 호출하고, 결과를 stdout/stderr와 exit code로 변환한다. Domain function은 shell과 process를 모른다. 이 구조는 web API의 request parsing→service→response mapping과 본질적으로 같다.

Subprocess, signal, file path, environment를 boundary adapter로 두면 core logic을 unit test하기 쉽다. Integration test에서는 실제 CLI를 실행해 exit code와 output을 검증할 수 있다. 작은 command 하나가 parsing, dependency injection, error boundary, observability를 모두 연습하게 한다.

Automation-friendly CLI는 deterministic output, stable exit code, non-interactive mode, timeout, dry-run을 제공할 수 있다. 사람을 위한 color와 progress는 machine mode와 분리한다.

Process 경계를 이해하면 Python 프로그램이 운영체제에서 어떻게 시작되고 다른 프로그램과 어떻게 연결되며 어떻게 종료되는지 보인다. 함수의 return에서 끝났던 사고를 **프로세스의 입력·출력·lifetime 계약**까지 확장하는 단계다.