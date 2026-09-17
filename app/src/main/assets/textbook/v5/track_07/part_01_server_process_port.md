# PART 01 · BLOCK 01 · LESSON 01 · 서버·프로세스·포트를 첫 요청으로 연결하기

휴대폰에서 버튼을 눌렀는데 다른 기기에서도 같은 결과를 보려면 화면 코드만으로는 부족하다. 요청을 받아 공통 상태를 읽거나 바꾸고, 결과를 다시 보내는 실행 주체가 필요하다. 이 LESSON은 `backend`, `server`, `process`, `host`, `port`, `listen`을 한 번에 암기하는 방식이 아니라 **실행 중인 프로그램이 네트워크 요청을 받을 수 있는 상태가 되는 과정**을 추적한다. 이후 모든 backend 문제를 이 실행 그림 위에 올릴 수 있어야 한다.

---

## CHAPTER 01 · backend는 화면 뒤의 코드가 아니라 서버 측 책임의 묶음이다

웹이나 모바일 앱에서 사용자가 직접 보는 버튼·목록·입력창은 보통 client 쪽에 있다. 그러나 로그인 사용자를 확인하고, 여러 기기가 공유하는 주문을 저장하고, 가격 규칙을 적용하고, 다른 시스템에 결제를 요청하는 일은 한 사용자의 화면에만 맡길 수 없다. 이런 서버 측 기능과 그 기능을 떠받치는 데이터 접근·외부 연동·운영 코드를 넓게 backend라고 부른다. backend라는 단어는 특정 언어나 framework를 뜻하지 않는다. Node.js로 작성할 수도 있고 Java, Kotlin, Python, Go 등 다른 환경을 쓸 수도 있다. 공통점은 **신뢰할 수 없는 외부 요청을 받아 서버가 책임지는 규칙을 수행하고 관찰 가능한 결과를 돌려준다**는 데 있다.

한 프로그램 안에서도 frontend와 backend의 경계는 배치 방식에 따라 달라질 수 있다. 서버가 HTML을 직접 만들 수도 있고, mobile app이 JSON API만 호출할 수도 있다. 따라서 `화면이 없으면 backend`, `JavaScript면 frontend`처럼 기술 이름으로 경계를 판단하면 틀리기 쉽다. 어떤 코드가 어느 실행 환경에서 누구의 권한으로 동작하고, 어떤 상태를 신뢰할 수 있게 관리하는지를 본다. 이 관점은 나중에 serverless function이나 background worker를 만났을 때도 그대로 적용된다.

직접 적어 볼 것은 자신이 쓰는 메모 앱의 동작 세 가지다. `메모 목록 보기`, `새 메모 저장`, `다른 기기에서 다시 보기`를 나누고, 각 단계에서 화면만으로 가능한 일과 중앙 서버가 맡아야 할 일을 표시한다. 이 연습은 framework 선택보다 먼저 **상태와 책임의 위치**를 결정하는 훈련이다.

---

## CHAPTER 02 · 저장된 프로그램 파일과 실행 중인 process는 다른 존재다

디스크에 `server.js` 또는 컴파일된 실행 파일이 존재한다고 해서 요청을 처리하는 서버가 생긴 것은 아니다. 운영체제가 프로그램을 실행하면 코드와 데이터가 메모리에 매핑되고, 실행 상태와 주소 공간, 열린 파일과 소켓 같은 자원을 가진 process가 만들어진다. process는 같은 프로그램 파일에서 여러 개 생길 수도 있다. 그래서 `코드는 배포되어 있다`와 `서버 process가 살아 있다`는 서로 다른 사실이다.

이 차이는 장애를 분류할 때 바로 쓰인다. 파일은 정상인데 process 시작이 실패할 수 있고, process는 떠 있지만 필요한 port에 bind하지 못했을 수 있으며, port까지 열었어도 route가 잘못돼 HTTP 오류가 날 수 있다. 반대로 사용자가 `서버가 안 된다`고 말해도 원인은 DNS, network, TLS, process, routing, dependency 등 여러 층일 수 있다. 진단은 추상적인 `서버` 한 단어가 아니라 **실행 단계가 어디까지 성공했는지**를 확인하는 방식으로 좁힌다.

터미널에서 server program을 실행한 뒤 다른 터미널에서 process 목록을 확인해 본다. 실행 전후를 비교하면 저장 파일은 그대로인데 process가 새로 생겼다는 사실을 볼 수 있다. process를 종료한 뒤 파일이 남아 있는지도 확인한다. 이 작은 실험을 통해 `코드 수정`, `build artifact`, `process 실행 상태`를 분리해서 말하는 습관을 만든다.

---

## CHAPTER 03 · server라는 단어는 기계·프로그램·역할을 문맥에 따라 가리킨다

현장에서 `서버를 재시작했다`, `서버가 죽었다`, `서버를 한 대 늘렸다`라는 표현은 서로 다른 대상을 가리킬 수 있다. 물리 또는 가상 machine을 뜻할 수도 있고, 그 안에서 실행되는 application process를 뜻할 수도 있으며, 여러 instance가 제공하는 논리적 service 전체를 뜻할 수도 있다. 이 모호함 때문에 장애 대화에서 `서버`만 반복하면 원인과 조치가 어긋난다.

예를 들어 한 virtual machine에서 application process가 두 개 동작하고 앞에 reverse proxy가 있을 수 있다. process 하나가 종료돼도 machine은 살아 있고 다른 process가 요청을 처리할 수 있다. 반대로 machine network가 끊기면 내부 process가 정상이어도 외부에서는 모두 사라진 것처럼 보인다. container 환경에서는 하나의 physical host 위에 여러 isolated process가 배치될 수 있어 기계와 application instance의 관계가 더 느슨해진다.

문제를 보고할 때는 `application process PID 1234가 종료됨`, `host A의 network reachability 실패`, `service의 healthy instance가 3개에서 1개로 감소`처럼 관찰 대상을 구체적으로 적는다. 이런 표현은 이후 health check와 load balancing을 배울 때도 이어진다. 프로그램 구조를 이해한다는 것은 용어를 많이 아는 일이 아니라 **어떤 자원이 실패했는지 정확히 지목할 수 있는 것**이다.

---

## CHAPTER 04 · host와 address는 요청이 도착할 기계를 찾는 데 쓰인다

client가 서버에 요청하려면 먼저 목적지가 필요하다. domain name은 DNS를 거쳐 IP address로 해석될 수 있고, 같은 machine에 여러 network interface와 여러 address가 존재할 수 있다. TRACK 06에서 이 network 경로를 배웠다면 backend에서는 그 요청을 **어느 interface에서 받아들일 것인지**가 중요해진다. server process가 `127.0.0.1`에만 bind하면 자기 machine 안에서만 접근 가능한 경우가 많고, 외부 interface에 bind해야 다른 device에서 접근할 수 있다.

`localhost`는 특별히 현재 machine 자신을 가리키는 이름으로 널리 쓰인다. 개발 PC에서 실행한 server를 휴대폰에서 테스트하면서 휴대폰 주소창에 `localhost:3000`을 입력하면 휴대폰 자신을 찾는다. PC를 찾는 마법 이름이 아니다. 같은 코드라도 어디에서 실행했는지에 따라 localhost의 대상이 바뀐다는 뜻이다.

실습에서는 PC의 server를 먼저 loopback에만 bind하고 같은 PC에서 접근한다. 그다음 LAN interface에 bind했을 때 같은 Wi-Fi의 다른 device에서 접근 가능한지 확인한다. 실제 성공 여부는 OS firewall과 network 설정에 따라 달라질 수 있다. 결과가 다르면 `코드가 틀렸다`고 단정하지 말고 bind address, listening socket, firewall rule, network path를 각각 증거로 확인한다.

---

## CHAPTER 05 · port는 같은 host 안에서 통신 대상을 구분하는 번호다

하나의 IP address에서 여러 network application이 동시에 동작하려면 어느 process의 endpoint로 데이터를 보낼지 구분해야 한다. TCP와 UDP는 port 번호를 사용해 이 구분을 만든다. web server가 3000, database가 5432, 다른 service가 8080을 사용할 수 있다. `IP만 맞으면 된다`가 아니라 transport protocol과 destination port까지 맞아야 해당 listening endpoint에 연결된다.

한 address와 port 조합을 이미 다른 process가 사용하고 있다면 새 process의 bind가 실패할 수 있다. 이때 application code가 정상이어도 시작 단계에서 `address already in use`와 같은 오류가 난다. 반대로 server가 4000에 listen하는데 client가 3000으로 연결하면 route handler까지 도달하지 않는다. HTTP 404를 찾기 전에 transport 연결부터 실패한다.

두 개의 작은 server를 각각 3000과 4000에 띄우고 다른 문자열을 반환하게 한다. browser 또는 curl에서 port만 바꾸어 결과를 비교한다. 그 뒤 두 번째 server도 3000을 쓰도록 바꿔 bind 충돌을 재현한다. 이 실험은 port가 단순 설정값이 아니라 **운영체제가 network endpoint ownership을 구분하는 실제 자원**임을 보여 준다.

---

## CHAPTER 06 · listen은 process가 요청을 받을 준비를 했다는 상태 전이다

Node.js에서 `server.listen(3000)`을 호출하면 application은 운영체제에 socket을 만들고 지정 endpoint에 bind한 뒤 incoming connection을 받을 준비를 하도록 요청한다. 세부 syscall 순서는 runtime과 설정에 따라 다르지만 개념적으로는 `program 실행 → network endpoint 확보 → 연결 대기`의 단계가 존재한다. process가 살아 있다는 사실만으로 listen이 성공했다고 가정하지 않는다.

아래 코드는 가장 작은 관찰점이다.

```ts
import http from "node:http";

const server = http.createServer((_req, res) => {
  res.statusCode = 200;
  res.end("hello backend");
});

server.listen(3000, "127.0.0.1", () => {
  console.log("listening on 127.0.0.1:3000");
});
```

`createServer`는 요청 처리 callback을 가진 server object를 만들고, `listen`이 endpoint 수신을 시작한다. callback의 log가 찍히기 전에 startup error가 발생할 수도 있다. production에서는 `process가 떴다`보다 **실제로 필요한 socket bind와 초기 dependency 준비가 끝났는지**를 readiness와 연결한다.

실행 후 OS 도구로 3000 port가 LISTEN 상태인지 확인한다. server를 종료하고 같은 명령을 다시 실행하면 entry가 사라져야 한다. 이 관찰은 application log만 믿지 않고 kernel이 가진 socket 상태로 사실을 교차검증하는 첫 예다.

---

## CHAPTER 07 · connection 실패와 HTTP 실패는 서로 다른 층에서 발생한다

client가 server와 TCP connection을 맺지 못하면 HTTP request 자체를 교환하지 못한다. destination에 listener가 없으면 connection refused가 날 수 있고, network path가 끊기면 timeout이 날 수 있다. 반면 connection과 HTTP parsing까지 성공한 뒤 요청한 resource를 찾지 못하면 404 같은 HTTP status가 돌아올 수 있다. 둘을 같은 `서버 에러`로 묶으면 잘못된 층을 고치게 된다.

진단 순서를 실제 경로대로 적으면 다음과 같다.

```text
목적지 이름/주소를 올바르게 찾았는가
→ transport endpoint까지 연결되는가
→ TLS가 있다면 handshake가 되는가
→ HTTP message가 서버에 도착하는가
→ method/path에 맞는 route가 있는가
→ handler와 dependency가 성공하는가
```

예를 들어 404가 왔다는 사실은 최소한 HTTP response를 만든 endpoint까지는 도달했다는 강한 단서다. 반대로 `ECONNREFUSED`가 발생했는데 route 코드를 수정하는 것은 근거 없는 조치다. 각 오류를 **어느 경계를 넘었고 어느 경계에서 멈췄는지**로 표현하면 디버깅 범위가 빠르게 줄어든다.

실습에서 server port를 4000으로 바꾸고 client만 3000을 유지해 연결 실패를 만든다. 다음에는 server를 다시 3000에 두되 존재하지 않는 `/missing` path를 요청해 HTTP 실패를 만든다. 두 실패의 client message, server log 유무, network 연결 여부를 표로 비교한다.

---

## CHAPTER 08 · 하나의 server process는 여러 요청을 시간상 겹쳐 처리할 수 있다

server는 `요청 하나를 다 끝내야 다음 요청을 받는 프로그램`이라고 단순화하면 실제 동작을 오해하게 된다. Node.js는 event loop와 비동기 I/O를 이용해 여러 connection과 request의 대기 시간을 겹쳐 다룰 수 있다. 다른 runtime은 thread pool, coroutine, process worker 등 다른 concurrency model을 사용할 수 있다. 공통점은 한 process가 동시에 여러 in-flight 작업을 책임질 수 있다는 것이다.

이 사실은 전역 mutable state를 사용할 때 바로 문제가 된다. 요청마다 `currentUser`라는 global 변수에 값을 넣으면 request A가 저장한 값이 request B에 덮일 수 있다. `현재 요청의 정보`는 process 전체 상태와 분리해야 한다. 뒤 LESSON에서 request context와 async propagation을 다루는 이유가 여기서 시작된다.

간단한 실험으로 `/slow?id=A`는 2초 뒤 응답하고 `/fast?id=B`는 즉시 응답하게 만든다. 두 요청을 거의 동시에 보내 결과 순서를 본다. runtime을 막지 않는 비동기 delay라면 slow가 기다리는 동안 fast가 처리될 수 있다. 그다음 CPU를 오래 점유하는 busy loop로 바꿔 차이를 관찰한다. 같은 `2초 작업`이라도 **I/O 대기와 CPU 점유가 다른 concurrency 효과**를 만든다.

---

## CHAPTER 09 · 여러 process 또는 instance가 뜨면 메모리 상태는 자동 공유되지 않는다

한 server process의 배열에 사용자를 저장하면 같은 process 안의 다음 요청에서는 보이지만 다른 process에는 자동으로 전달되지 않는다. production에서 같은 application을 여러 instance로 늘리면 각 process의 heap은 별개다. load balancer가 요청을 다른 instance로 보내면 memory-only session이나 cache가 갑자기 사라진 것처럼 보일 수 있다.

이 때문에 `개발 PC에서는 잘 된다`는 증거만으로 shared state 설계를 판단할 수 없다. 개발 환경에서는 instance가 하나이고 재시작도 적어서 global Map이 database처럼 보일 수 있다. 그러나 process crash, restart, horizontal scaling을 넣는 순간 durability와 consistency 요구가 드러난다. 영속 데이터는 database 같은 외부 저장소에 두고, local memory cache는 재생성 가능한 상태로 취급하는 설계가 흔하다.

두 process를 3001과 3002에 띄우고 각각 memory counter를 둔다. 3001에서 counter를 올린 뒤 3002를 조회하면 값이 다르다는 것을 확인한다. 그다음 둘 앞에 단순 proxy가 번갈아 요청을 보내면 사용자는 같은 service를 호출했는데 값이 흔들리는 현상을 볼 수 있다. 이 실험은 storage·session·cache 설계가 deployment topology와 분리될 수 없음을 보여 준다.

---

## CHAPTER 10 · 환경 설정은 process가 시작될 때 실행 계약의 일부가 된다

같은 program artifact라도 `PORT`, database endpoint, log level 같은 config가 다르면 다른 runtime behavior를 가진다. 그래서 실행 상태를 재현하려면 commit SHA만 기록해서는 부족하다. 어떤 artifact를 어떤 config와 secret reference로 어떤 runtime에서 시작했는지 알아야 한다. startup 단계에서 required config를 검증하면 잘못된 설정을 request 처리 중에 늦게 발견하는 일을 줄일 수 있다.

예를 들어 다음처럼 PORT를 숫자로 변환만 하고 검증하지 않으면 잘못된 값이 obscure한 startup error로 이어질 수 있다.

```ts
const port = Number(process.env.PORT ?? "3000");
if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error("PORT must be an integer from 1 to 65535");
}
```

설정과 secret은 모두 외부에서 주입될 수 있지만 위험도는 다르다. API key나 password는 log에 출력하지 않고 접근·rotation 정책이 필요하다. 이 LESSON에서는 secret 관리 자체보다 **process가 어떤 입력으로 시작됐는지 실행 계약으로 본다**는 관점을 잡는다.

실습에서는 PORT를 3000, 0, `abc`, 70000으로 바꿔 startup 결과를 확인한다. validation 없이 runtime에 맡긴 결과와 명시적으로 fail-fast한 결과의 error message가 얼마나 다른지 비교한다.

---

## CHAPTER 11 · process 종료는 단순히 화면이 꺼지는 일이 아니라 진행 중 책임을 정리하는 사건이다

server process가 종료되면 열린 connection, memory state, 진행 중 request, buffered log, background task가 영향을 받는다. 운영 환경의 deploy나 autoscaling은 process 종료를 정상적인 lifecycle로 만든다. 따라서 `시작만 잘하면 된다`가 아니라 종료 경로도 설계해야 한다. 운영체제 signal을 받았을 때 새 요청 수신을 멈추고, 이미 받은 작업을 제한 시간 안에 정리하고, connection pool과 telemetry를 닫는 절차가 필요할 수 있다.

즉시 `process.exit(0)`을 호출하면 cleanup callback이나 pending I/O가 완료될 기회를 잃을 수 있다. 반대로 종료를 무한정 기다리면 deploy가 멈추고 orchestrator가 결국 강제 종료할 수 있다. graceful shutdown은 `절대로 끊지 않기`가 아니라 **정해진 deadline 안에서 새 일을 차단하고 이미 맡은 일을 최대한 일관되게 끝내는 정책**이다.

3초 지연 route를 만든 뒤 request가 진행 중일 때 SIGTERM을 보내 본다. server가 바로 끊기는 구현과 `server.close()`로 새 connection을 막고 in-flight 처리를 기다리는 구현을 비교한다. 테스트에서는 성공 응답 여부뿐 아니라 종료까지 걸린 시간과 열린 connection이 남았는지도 관찰한다.

---

## CHAPTER 12 · health와 readiness는 process가 존재한다는 사실보다 더 구체적인 질문을 한다

PID가 존재하고 port가 열려 있어도 application이 실제 사용자 요청을 처리할 준비가 안 됐을 수 있다. startup migration이 끝나지 않았거나 필수 config가 잘못됐거나 dependency pool이 초기화되지 않았을 수 있다. production에서는 `process alive`와 `new traffic을 받아도 됨`을 분리하기 위해 liveness와 readiness 같은 health signal을 사용한다.

readiness가 false라면 load balancer나 orchestrator가 새 traffic을 보내지 않도록 할 수 있다. 종료할 때도 먼저 readiness를 내려 traffic을 빼고, 그 뒤 in-flight request를 drain하는 순서가 유용하다. 반대로 dependency가 잠깐 느리다고 모든 instance의 liveness를 동시에 실패시키면 재시작 폭주가 장애를 키울 수 있다. health check는 무엇을 검사하고 어떤 자동 조치를 일으키는지 함께 설계해야 한다.

개발용으로 `/ready` endpoint를 만들고 `ready=false`에서 503, 초기화가 끝난 뒤 200을 반환하도록 해 본다. 그 다음 application의 일반 route와 readiness를 동시에 호출하며 상태 전이를 기록한다. 이 실험은 health endpoint가 장식용 URL이 아니라 **traffic admission과 lifecycle을 연결하는 제어 신호**라는 점을 보여 준다.

---

## CHAPTER 13 · 관측 증거는 application log 한 줄보다 여러 층을 연결할 때 강해진다

server가 응답하지 않을 때 `console.log("started")` 한 줄만으로는 process가 지금도 살아 있는지, 원하는 port에 listen하는지, 요청이 실제 route까지 왔는지 알 수 없다. 최소한 process 상태, listening socket, request access log, error stack을 서로 연결해야 한다. production에서는 metrics와 traces까지 더해 어느 시간대에 어떤 request가 어느 dependency에서 멈췄는지 재구성한다.

증거에는 시점과 식별자가 필요하다. 여러 요청이 동시에 들어오면 `start`, `finish`만 찍힌 log가 서로 섞인다. request ID나 trace context를 함께 남기면 한 요청의 event를 묶을 수 있다. 다만 token이나 password 같은 민감정보를 correlation field로 쓰면 안 된다. 관측 가능성은 더 많이 출력하는 일이 아니라 **원인 추적에 필요한 최소 정보를 구조화해서 남기는 것**이다.

실습에서는 process PID, listen port, request ID, method, path, status, duration을 한 줄의 structured JSON으로 남긴다. 정상 호출과 잘못된 path를 보내고 log 차이를 본다. 다음으로 server를 종료한 상태에서 client가 실패했을 때 server log가 전혀 생기지 않는다는 사실을 확인한다. 이 차이만으로도 failure boundary를 크게 좁힐 수 있다.

---

## CHAPTER 14 · 첫 디버깅 절차는 추측보다 경계별 확인 순서를 고정한다

`API가 안 된다`는 보고를 받았을 때 코드부터 바꾸면 정상인 부분을 망가뜨릴 수 있다. 먼저 재현 조건과 목적 endpoint를 고정하고 경계를 따라 확인한다. client가 어느 URL을 사용했는지, DNS가 무엇을 반환했는지, connection이 성립하는지, TLS가 성공했는지, server가 request를 받았는지, route가 선택됐는지, handler가 어떤 dependency를 호출했는지 순서대로 증거를 모은다.

각 단계는 다음 단계의 전제다. 예를 들어 server access log에 request가 보이지 않는데 database query를 조사하는 것은 우선순위가 낮다. access log가 있고 404라면 process와 network보다 route table을 먼저 본다. 500이고 stack trace에 DB timeout이 있다면 application business rule을 무작정 고치는 대신 pool과 DB latency를 확인한다. 이런 방식은 복잡한 시스템에서도 동일하게 확장된다.

연습 문제로 세 상황을 분류한다. `A: curl이 connection refused`, `B: HTTP 404 + server access log 있음`, `C: HTTP 500 + DB timeout trace`. 각 경우 가장 먼저 확인할 증거 세 개를 적고, 왜 다른 층을 먼저 고치지 않는지 설명한다. 정답의 핵심은 명령어 암기가 아니라 **관찰된 사실이 배제하는 가능성을 이용해 탐색 공간을 줄이는 것**이다.

---

## CHAPTER 15 · 첫 실습은 실행·수정·실패·복구까지 한 묶음으로 끝낸다

아래 server를 직접 실행한다.

```ts
import http from "node:http";

const host = process.env.HOST ?? "127.0.0.1";
const port = Number(process.env.PORT ?? "3000");

if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error("invalid PORT");
}

const server = http.createServer((req, res) => {
  const started = Date.now();
  res.on("finish", () => {
    console.log(JSON.stringify({
      method: req.method,
      url: req.url,
      status: res.statusCode,
      durationMs: Date.now() - started
    }));
  });
  res.statusCode = 200;
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.end("backend alive");
});

server.listen(port, host, () => {
  console.log(`listening http://${host}:${port}`);
});
```

실행 후 네 가지 변경을 순서대로 한다. 첫째 port를 4000으로 바꾸고 client 주소를 일부러 3000에 둔다. 둘째 다시 3000으로 맞춘 뒤 `/missing` 요청을 보내 현재 code가 왜 200을 반환하는지 확인한다. 셋째 method와 path를 검사해 없는 route에는 404를 반환하도록 수정한다. 넷째 같은 port로 server를 두 개 실행해 bind 충돌을 재현한다. 각 단계에서 `client 증상`, `server log 존재 여부`, `listen 상태`, `원인 층`, `수정`을 기록한다.

이 실습에서 직접 유지해야 할 판단은 `파일이 존재함`, `process가 실행 중임`, `endpoint가 listen 중임`, `HTTP route가 존재함`을 서로 다른 사실로 구분하는 것이다. AI는 운영체제별 port 확인 명령이나 boilerplate를 생성할 수 있지만, 어떤 증거가 어느 경계를 통과했음을 의미하는지는 사람이 판단해야 한다. 다음 LESSON부터 request 내부 정보를 읽기 시작하지만, 그 전에 **요청이 handler까지 도착할 조건**을 이 PART에서 실제로 재현해 둔다.
