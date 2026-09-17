# PART 14 · BLOCK 01 · LESSON 14 · graceful shutdown으로 새 요청과 진행 중 책임을 분리하기

server process는 영원히 실행되지 않는다. 배포, autoscaling, machine maintenance, crash 때문에 종료된다. 정상 종료 경로를 설계하지 않으면 request 중간에 connection이 끊기고 DB transaction이나 background job이 애매한 상태로 남을 수 있다. 반대로 모든 작업을 무한정 기다리면 deploy가 멈추고 결국 강제 종료된다. graceful shutdown은 **새 작업 수락을 중단하고 이미 맡은 작업을 제한된 시간 안에 일관되게 정리하는 lifecycle protocol**이다.

---

## CHAPTER 01 · signal은 process에 종료 의도를 전달하지만 강제 crash와 의미가 다르다

Unix 계열 환경에서 orchestrator는 SIGTERM 같은 signal을 보내 정상 종료 기회를 줄 수 있고 개발자는 Ctrl+C로 SIGINT를 보낼 수 있다. application은 signal handler에서 shutdown sequence를 시작한다. SIGKILL이나 process crash처럼 handler를 실행할 수 없는 종료도 존재하므로 graceful path만 믿고 durability를 설계하면 안 된다.

signal handler에서 무거운 synchronous work를 바로 수행하기보다 idempotent shutdown function을 호출하고 중복 signal에도 한 번만 sequence가 실행되도록 한다. exit code는 정상/실패 종료를 운영 도구에 전달한다.

실습에서 SIGTERM/SIGINT를 각각 보내 shutdown log와 exit code를 기록한다. 두 signal이 연속으로 와도 cleanup이 두 번 실행되지 않는지 test한다.

---

## CHAPTER 02 · 첫 단계는 readiness를 내려 새 traffic의 admission을 멈추는 것이다

load balancer가 계속 request를 보내는 동안 server socket부터 닫으면 connection error가 늘 수 있다. orchestrated environment에서는 먼저 readiness를 false로 바꾸고 endpoint 목록에서 빠질 시간을 준 뒤 server accept를 중단하는 순서를 사용할 수 있다. 정확한 propagation delay는 platform에 따라 다르므로 측정한다.

readiness false는 process가 죽었다는 뜻이 아니다. 이미 받은 request를 처리할 수 있고 metrics endpoint가 살아 있을 수 있다. liveness와 readiness를 구분해야 rolling deployment가 안정적이다.

실습에서 shutdown 시작 시 `/ready`가 503으로 바뀌지만 진행 중 slow request는 계속 처리되는지 확인한다.

---

## CHAPTER 03 · server.close는 새 connection 수락과 기존 connection 처리 규칙을 확인해야 한다

Node HTTP server의 close API는 새 connection을 막고 기존 connection이 끝나길 기다리는 데 사용되지만 runtime version에 따라 idle keep-alive connection 처리 detail이 다를 수 있다. official API contract를 확인하고 필요한 경우 idle/all connection close API를 사용한다.

`close callback이 호출됨`과 `모든 business task가 durable하게 끝남`은 다르다. server socket 외에 DB pool, queue consumer, timer, telemetry exporter가 별도로 존재한다.

실습에서 keep-alive client를 연결한 뒤 shutdown을 시작해 callback까지 시간을 측정한다. idle connection이 deadline을 지연하는지 확인한다.

---

## CHAPTER 04 · in-flight request counter는 drain 진행 상태를 관찰하게 한다

request 시작 시 counter를 증가시키고 finish/close에서 감소시키면 shutdown 중 남은 request 수를 볼 수 있다. 0이 되면 HTTP work가 drain됐다는 strong signal이 된다. counter decrement가 exception/abort path에서 빠지지 않도록 lifecycle hook을 사용한다.

counter 자체로 모든 async work를 알 수는 없다. handler가 fire-and-forget task를 시작했다면 response 뒤에도 일이 남는다. 그런 작업은 queue owner로 넘기거나 별도 task registry를 관리한다.

실습에서 concurrent slow request 20개 중 shutdown을 시작하고 counter가 20→0으로 줄어드는 timeline을 기록한다.

---

## CHAPTER 05 · keep-alive와 WebSocket처럼 장수 connection은 별도 drain 정책이 필요하다

HTTP keep-alive idle socket과 WebSocket/SSE처럼 오래 유지되는 session은 일반 request가 끝나는 것을 기다리는 것만으로 종료되지 않을 수 있다. shutdown에서 client에게 reconnect signal을 보내고 일정 grace 후 connection을 close할 수 있다. load balancer connection draining 설정과 application policy를 맞춘다.

실시간 connection이 끊겨도 client가 다른 instance로 재연결할 수 있게 session/state를 process memory에만 두지 않는 설계가 중요하다. reconnect token과 last event cursor를 사용할 수 있다.

실습에서 long-lived connection fake를 만들어 graceful notification 후 client reconnect 시나리오를 작성한다.

---

## CHAPTER 06 · DB pool은 새로운 query가 끝난 뒤 닫아야 한다

HTTP server를 닫자마자 DB pool을 먼저 close하면 아직 처리 중인 handler가 query를 실행하려다 실패한다. 일반적인 순서는 new traffic 차단 → in-flight drain → pool close다. 반대로 drain 중 request가 무한 query를 시작하지 않게 deadline/cancellation도 적용한다.

transaction 진행 중 process가 죽는 경우 DB가 connection loss를 감지해 rollback할 수 있지만 외부 side effect는 자동 rollback되지 않는다. durability는 transaction/outbox와 별도로 보장한다.

실습에서 2초 DB fake를 가진 request와 shutdown을 겹치고 pool close 순서를 잘못 배치한 failure를 재현한다. 올바른 순서에서 query가 완료되는지 확인한다.

---

## CHAPTER 07 · background timer와 interval은 shutdown에서 새 작업을 생성하지 않게 멈춘다

process 안의 setInterval scheduler가 shutdown 중에도 새 batch job을 시작하면 drain이 끝나지 않을 수 있다. shutdown 시작에서 scheduler admission을 중단하고 현재 run만 정책에 따라 완료/중단한다. 다음 실행 시 다른 instance가 이어받을 수 있게 durable checkpoint를 사용한다.

schedule tick을 놓치면 다음 startup에서 catch-up할지 skip할지 business semantics를 정의한다. process-local timer는 multi-instance에서 duplicate 실행될 수 있다는 한계도 있다.

실습에서 1초 interval job이 shutdown 후 추가 run을 시작하지 않는지 counter로 확인한다.

---

## CHAPTER 08 · queue worker는 fetch를 멈추고 현재 lease를 완료하거나 반환한다

worker process는 HTTP server와 다른 drain protocol을 가진다. 먼저 broker에서 새 message fetch를 중단하고, 현재 처리 중 message가 deadline 안에 끝나면 ack한다. 끝내지 못하면 lease가 expire되어 다른 worker가 재처리할 수 있으므로 consumer idempotency가 필요하다.

job 처리 중 side effect가 끝났지만 ack 전 종료될 수 있다. graceful shutdown만으로 duplicate delivery를 없앨 수 없고 at-least-once model을 정상 failure로 취급한다.

실습에서 5초 job과 3초 grace를 비교해 완료/lease-return policy를 test한다. duplicate 재처리에도 결과가 중복되지 않는지 별도 idempotency test를 둔다.

---

## CHAPTER 09 · shutdown deadline은 drain이 영원히 끝나지 않는 상황을 제한한다

한 request가 bug로 hang하면 in-flight counter가 0이 되지 않는다. orchestrator에는 termination grace period가 있고 그 이후 강제 kill할 수 있다. application은 그보다 짧은 internal deadline을 두어 telemetry flush와 final cleanup 시간을 남긴다.

예: 전체 grace 30초 중 20초 request drain, 5초 resource close, 5초 buffer를 둘 수 있다. 숫자는 workload p99와 platform limit을 근거로 정한다. 모든 request를 절대 완료시키겠다는 목표보다 user SLO와 deploy availability를 함께 본다.

실습에서 never-resolving request를 주입해 deadline 후 abort/force close path가 실행되는지 확인한다.

---

## CHAPTER 10 · shutdown 중 status와 error는 정상 종료 사건과 실제 장애를 구분한다

drain 때문에 새 request가 503을 받는 것을 server bug와 같은 incident로 세면 rolling deploy마다 error rate가 튄다. load balancer가 readiness false instance에 새 traffic을 보내지 않게 하고, 내부 shutdown metric을 별도 event로 기록한다.

반대로 grace deadline을 초과해 강제 kill이 반복되면 정상 deploy로 숨기지 않는다. `shutdown_duration`, `forced_termination_count`, `inflight_at_force`를 측정해 leak/hang를 찾는다.

실습으로 정상 drain과 forced shutdown을 metric에서 구분한다.

---

## CHAPTER 11 · rolling deployment에서는 old와 new version이 동시에 traffic을 처리한다

instance를 하나씩 교체하면 일정 시간 두 version이 공존한다. DB schema와 message contract, API response가 cross-version compatible해야 한다. new code가 old schema를 깨는 destructive migration을 먼저 적용하면 old instance가 실패할 수 있다.

expand-and-contract migration처럼 먼저 compatible schema를 추가하고 code rollout 후 old field를 제거하는 전략이 필요하다. 자세한 migration은 TRACK 08/10과 연결되지만 backend developer는 shutdown/deploy window의 version overlap을 이해해야 한다.

연습으로 v1/v2 instance가 동시에 존재할 때 request와 message가 서로 호환되는지 matrix를 만든다.

---

## CHAPTER 12 · crash recovery는 graceful shutdown과 다른 방어층이다

OOM, kernel kill, machine power loss에서는 cleanup callback이 실행되지 않는다. 중요한 상태를 memory-only cleanup에 의존하면 데이터가 남거나 사라진다. DB transaction, idempotency record, queue lease, object-storage lifecycle처럼 process crash에도 복구 가능한 durable mechanism을 사용한다.

`finally` block이 있다는 사실은 crash safety를 보장하지 않는다. process가 정상 control flow를 실행할 수 있어야 finally가 동작한다. critical consistency는 external durable state와 recovery worker에 맡긴다.

실습 설계에서 `SIGTERM` 정상 종료와 `kill -9` 강제 종료를 별도 failure class로 두고 예상 차이를 적는다.

---

## CHAPTER 13 · telemetry exporter도 shutdown에서 flush할 시간을 가진다

log와 trace exporter가 batch buffer를 사용하면 process 종료 직전 incident event가 아직 외부 backend로 전송되지 않았을 수 있다. shutdown sequence에서 제한된 flush를 수행하면 last errors를 보존할 수 있다. 하지만 telemetry outage 때문에 server가 무한히 종료를 못 하는 일은 막아야 한다.

file logger는 buffer fsync semantics가 다르고 remote exporter는 network timeout이 있다. application의 business cleanup보다 telemetry를 먼저 닫으면 후속 cleanup log를 잃는다. 순서를 명시한다.

실습에서 buffered fake exporter에 final event를 넣고 flush success/timeout 두 case의 shutdown duration을 test한다.

---

## CHAPTER 14 · shutdown function은 중복 호출되어도 안전하게 한 번의 state transition을 수행한다

SIGTERM 두 번, admin shutdown endpoint, fatal condition이 동시에 shutdown을 시작할 수 있다. `RUNNING → DRAINING → CLOSING → STOPPED` state machine을 두고 이미 진행 중이면 같은 Promise를 반환하게 할 수 있다. resource close를 두 번 호출해 error가 나는 것을 막는다.

state transition log에 reason과 timestamp를 남기면 누가 종료를 시작했는지 알 수 있다. shutdown 중 신규 fatal error가 생겨도 state와 exit code를 업데이트할 policy를 둔다.

실습에서 shutdown 함수를 10번 동시에 호출하고 server.close/pool.close가 각각 한 번만 실행되는지 spy로 검증한다.

---

## CHAPTER 15 · shutdown 실습은 graceful과 forced termination을 모두 재현한다

테스트 server에 fast route, 3초 slow route, keep-alive connection, fake DB pool, interval job, buffered telemetry를 붙인다. SIGTERM에서 readiness false → new admission stop → in-flight drain → timer stop → pool close → telemetry flush → exit 순서를 구현한다. 전체 grace는 테스트용으로 5초를 사용한다.

첫 시나리오는 3초 request가 정상 완료되어 exit 0이 되는지 본다. 두 번째는 never-ending request로 5초 deadline을 넘겨 force-close와 nonzero diagnostic metric이 남는지 확인한다. 세 번째는 SIGTERM 두 번을 보내 cleanup이 중복되지 않는지 본다. 네 번째는 강제 process kill에서는 cleanup log가 없다는 차이를 문서화한다.

AI는 signal handler와 framework close API 사용법을 생성할 수 있다. 사람은 어느 resource를 어떤 순서로 닫아야 하는지, 얼마나 기다릴지, crash에서도 남아야 할 state가 무엇인지 결정한다. 종료 경로를 실제 test하면 배포가 **정상 운영 사건인지 데이터 손실 위험인지** 구분할 수 있다.
