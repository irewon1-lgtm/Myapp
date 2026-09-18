# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 06 · 한 서버를 넘어 여러 구성요소가 협력하는 구조 이해하기

### LESSON 12 · replication·failover를 backend 관점에서 다루기

## CHAPTER 01 · replication·failover를 backend 관점에서 다루기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 복제는 가용성과 읽기 확장을 도울 수 있지만 failover 순간의 lag와 consistency를 고려해야 한다.

**아주 쉬운 사건.** primary failover 뒤 일부 read가 늦다. 이 사건에서는 먼저 **replica lag와 RPO/RTO를 구분한다**.

**왜 필요한가.** 정상 동작은 primary/replica 역할, replication position, promotion 조건과 client reconnect 정책을 계약으로 둔다. 반대로 replica가 있으니 데이터 손실이 0이라고 가정하거나 오래된 connection이 옛 primary로 계속 write한다.

**암기:** `replication·failover를 backend 관점에서 다루기`의 역할 한 줄.

**직접 이해:** `replication·failover를 backend 관점에서 다루기`의 입력·상태·결과 경계.

**AI 위임 가능:** `replication·failover를 backend 관점에서 다루기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `primary failover 뒤 일부 read가 늦다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 replication lag/position, role change, write error와 reconnect를 본다.

## CHAPTER 02 · replication·failover를 backend 관점에서 다루기 — 아주 쉬운 예를 한 단계씩 해석

T07-P87: `primary failover 뒤 일부 read가 늦다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | primary failover 뒤 일부 read가 늦다 | T07-P87 외부 입력 | T07-P87: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | replica lag와 RPO/RTO를 구분한다 | T07-P87 판단 기준 | T07-P87/CH08 관측표와 대조 |
| 정상 경로 | T07-P87/CH03 M1→M5 | replication·failover를 backend 관점에서 다루기: 완료 시점을 단계별로 분리 | T07-P87: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P87/CH06 replica가 있으니 데이터 손실이 0이라고 가정하거나 오래된 connection이 옛 primary로 계속 write한다. | T07-P87: 깨진 계약 하나를 특정 | replication·failover를 backend 관점에서 다루기: 증상과 원인을 분리 |
| 재검증 | T07-P87/CH10 직접 실행 | T07-P87: 예상값 T07-P087 5 기록 | replication·failover를 backend 관점에서 다루기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P87/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P87/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · replication·failover를 backend 관점에서 다루기 — 내부 메커니즘과 상태 전이

primary/replica 역할, replication position, promotion 조건과 client reconnect 정책을 계약으로 둔다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | primary failover 뒤 일부 read가 늦다 | source/actor/size를 보존 |
| M2 | 경계 판단 | replica lag와 RPO/RTO를 구분한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | primary/replica 역할, replication position, promotion 조건과 client reconnect 정책을 계약으로 둔다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | replication lag/position, role change, write error와 reconnect를 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | replica lag가 허용 기준 이하인지 판정한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`replication·failover를 backend 관점에서 다루기` 흐름을 framework 이름 없이 설명한다.

막히면 `primary failover 뒤 일부 read가 늦다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · replication·failover를 backend 관점에서 다루기 — 실전 경계 A

**경계 A.** replica는 read scale과 fault tolerance를 높일 수 있지만 replication lag 때문에 방금 쓴 값을 secondary에서 즉시 못 볼 수 있다.

T07-P87/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P87에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P87/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P87/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P87): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P87/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P87-A1-450 | T07-P87 조건 | replica는 read scale과 fault tolerance를 높일 수 있지만 replication lag 때문에 방금 쓴 값을 secondary에서 즉시 못 볼 수 있다. |
| T07-P87-A2-451 | T07-P87 변화점 | T07-P87/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P87-A3-452 | T07-P87 반례 | T07-P87/CH06 대표 실패와 A 위반을 구별 |
| T07-P87-A4-453 | T07-P87 근거 | T07-P87/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P87-A5-454 | T07-P87 재실험 | primary failover 뒤 일부 read가 늦다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · replication·failover를 backend 관점에서 다루기 — 실전 경계 B

**경계 B.** failover는 endpoint/DNS/role이 바뀌는 시간과 client connection이 old primary에 남는 시간을 포함하므로 ‘replica 있음’만으로 recovery time이 0이 되지 않는다.

T07-P87/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P87에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P87/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P87/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P87): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P87/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P87-B1-481 | T07-P87 조건 | failover는 endpoint/DNS/role이 바뀌는 시간과 client connection이 old primary에 남는 시간을 포함하므로 ‘replica 있음’만으로 recovery time이 0이 되지 않는다. |
| T07-P87-B2-482 | T07-P87 독립성 | T07-P87/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P87-B3-483 | T07-P87 상태 | T07-P87/CH03 before·after 위치를 다시 지정 |
| T07-P87-B4-484 | T07-P87 반증 | T07-P87/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P87-B5-485 | T07-P87 적용 | replication·failover를 backend 관점에서 다루기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · replication·failover를 backend 관점에서 다루기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **replica가 있으니 데이터 손실이 0이라고 가정하거나 오래된 connection이 옛 primary로 계속 write한다.**

아래 여섯 사례는 T07-P87의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P87-F01 | T07-P87: 대표 실패 | T07-P87: T07-P87/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P87: 현상만 보고 원인을 확정 | T07-P87/CH08 evidence map에서 상태를 대조 |
| T07-P87-F02 | T07-P87: 경계 A 누락 | T07-P87: T07-P87/CH04 경계 A 위반 입력 | T07-P87: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P87/CH08 evidence map에서 상태를 대조 |
| T07-P87-F03 | T07-P87: 경계 B 누락 | T07-P87: T07-P87/CH05 경계 B 위반 입력 | T07-P87: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P87/CH08 evidence map에서 상태를 대조 |
| T07-P87-F04 | T07-P87: 복구 경계 C 누락 | T07-P87: T07-P87/CH07 경계 C 복구 조건 | T07-P87: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P87/CH08 evidence map에서 상태를 대조 |
| T07-P87-F05 | T07-P87: 운영 경계 D 누락 | T07-P87: T07-P87/CH09 경계 D 운영 조건 | T07-P87: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P87/CH08 evidence map에서 상태를 대조 |
| T07-P87-F06 | T07-P87: 증거 없는 결론 | T07-P87: T07-P87/CH02 첫 판단만 존재 | T07-P87: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P87/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P87/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · replication·failover를 backend 관점에서 다루기 — 복구 가능한 상태와 수명

**경계 C.** read consistency를 primary-only, stale-allowed replica, version-aware routing 등 use case별로 정해 모든 조회를 같은 정책으로 처리하지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P87에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P87/CH08 evidence map을 본다. 복구 후에는 T07-P87/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P87에서 이미 확정된 side effect는 T07-P87/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P87-R1-543 | T07-P87 중단 직전 | T07-P87/CH03에서 이미 확정된 상태만 표시 |
| T07-P87-R2-544 | T07-P87 재시작 직후 | read consistency를 primary-only, stale-allowed replica, version-aware routing 등 use case별로 정해 모든 조회를 같은 정책으로 처리하지 않는다. |
| T07-P87-R3-545 | T07-P87 재검증 | T07-P87/CH08 근거로 중복·누락 여부 확인 |
| T07-P87-R4-546 | T07-P87 재실행 | T07-P87/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · replication·failover를 backend 관점에서 다루기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | primary failover 뒤 일부 read가 늦다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | replica lag와 RPO/RTO를 구분한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | primary/replica 역할, replication position, promotion 조건과 client reconnect 정책을 계약으로 둔다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | replica가 있으니 데이터 손실이 0이라고 가정하거나 오래된 connection이 옛 primary로 계속 write한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | replication lag/position, role change, write error와 reconnect를 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`replication·failover를 backend 관점에서 다루기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · replication·failover를 backend 관점에서 다루기 — 운영 한계와 종료 조건

**경계 D.** RPO는 얼마나 최근 data까지 잃을 수 있는지, RTO는 얼마나 빨리 복구해야 하는지의 목표로 backup/replication/failover 설계를 같은 언어로 비교한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P87/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P87/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P87 과제: 경계 D와 T07-P87/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P87-O1-605 | synthetic-load=105 | T07-P87/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P87-O2-606 | synthetic-budget=755ms | T07-P87 timeout과 unknown outcome을 분리 |
| T07-P87-O3-607 | T07-P87 종료 | RPO는 얼마나 최근 data까지 잃을 수 있는지, RTO는 얼마나 빨리 복구해야 하는지의 목표로 backup/replication/failover 설계를 같은 언어로 비교한다. |
| T07-P87-O4-608 | T07-P87 완화 | T07-P87/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · replication·failover를 backend 관점에서 다루기 — 직접 실행하는 작은 모델

`replication·failover를 backend 관점에서 다루기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P087 5`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P087";
const current = 4;
const incoming = [3, 4, 5];
const next = incoming.reduce((state, v) => v > state ? v : state, current);
console.log(marker, next);
```

기준 출력: `T07-P087 5`.

`replication·failover를 backend 관점에서 다루기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P87-L1-637 | constmarker="T07-P087"; | T07-P87 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P87-L2-638 | constcurrent=4; | T07-P87 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P87-L3-639 | constincoming=[3,4,5]; | T07-P87 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P87-L4-640 | constnext=incoming.reduce((state,v)=>v>state?v:state,current); | T07-P87 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P87-L5-641 | console.log(marker,next); | T07-P87 출력 관측점; 예상 `T07-P087 5`와 비교 |
| T07-P87-LX-726 | T07-P87 실행 기록 | T07-P87 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · replication·failover를 backend 관점에서 다루기 — 한 부분만 수정하고 다시 예측

수정 과제: **incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다**.

수정 전은 `T07-P087 5`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P87/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P87-D1-667 | 기준 `T07-P087 5` | T07-P87 수정 전 실행을 먼저 재현 |
| T07-P87-D2-668 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | T07-P87 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P87-D3-669 | T07-P87 새 예측 | T07-P87 실행 전에 출력·상태를 먼저 기록 |
| T07-P87-D4-670 | T07-P87 재실행 | T07-P87/CH10 실제값과 새 예측을 대조 |
| T07-P87-D5-671 | T07-P87 반례 | T07-P87/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P87-D6-672 | T07-P87 근거 | T07-P87/CH08 상태가 설명과 일치해야 완료 |
| T07-P87-D7-673 | T07-P87 이유 | T07-P87 변경 이유를 replication·failover를 backend 관점에서 다루기 계약과 연결해 설명 |

## CHAPTER 12 · replication·failover를 backend 관점에서 다루기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: replication·failover를 backend 관점에서 다루기 | 복제는 가용성과 읽기 확장을 도울 수 있지만 failover 순간의 lag와 consistency를 고려해야 한다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P87/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | primary/replica 역할, replication position, promotion 조건과 client reconnect 정책을 계약으로 둔다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | replica가 있으니 데이터 손실이 0이라고 가정하거나 오래된 connection이 옛 primary로 계속 write한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | replication lag/position, role change, write error와 reconnect를 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P87/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P87/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P87/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P87/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `replication·failover를 backend 관점에서 다루기` 실행 코드 수정 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | `replication·failover를 backend 관점에서 다루기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P87/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P87/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · replication·failover를 backend 관점에서 다루기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 복제는 가용성과 읽기 확장을 도울 수 있지만 failover 순간의 lag와 consistency를 고려해야 한다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P87/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P87/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P87/CH10 실행용 boilerplate·test 후보 | T07-P87: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P087 5` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P87/CH02의 판단 기준과 T07-P87/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P87-AI1-729 | T07-P87 사람 결정 | T07-P87 업무 의미·허용 위험·완료 기준 소유 |
| T07-P87-AI2-730 | T07-P87 AI 초안 | T07-P87/CH10 boilerplate·test 후보까지만 위임 |
| T07-P87-AI3-731 | T07-P87 검증 | T07-P87/CH06 반례와 T07-P87/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · replication·failover를 backend 관점에서 다루기 — 경계 조합 실험 8개

T07-P87: T07-P87/CH04~T07-P87/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P87의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-K01 | T07-P87: 경계 A | T07-P87: 경계 B | T07-P87: OVERLOAD[rps=236; p99_ms=660; queue=11] / sample=85 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + QUEUE_PRESSURE |
| T07-P87-K02 | T07-P87: 경계 A | T07-P87: 경계 C | T07-P87: SAMPLING[sample_rate=63%; trace_present=0; metric_present=1] / sample=92 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P87-K03 | T07-P87: 경계 A | T07-P87: 경계 D | T07-P87: DISCONNECT[disconnect_ms=86; commit_state=UNKNOWN; request=087-03] / sample=99 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + COMMIT_TIMELINE |
| T07-P87-K04 | T07-P87: 경계 B | T07-P87: 경계 C | T07-P87: RECURRENCE[occurrence=6; interval_s=75; mitigation_applied=1] / sample=17 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + RECURRENCE_TIMELINE |
| T07-P87-K05 | T07-P87: 경계 B | T07-P87: 경계 D | T07-P87: OWNER_SPOOF[actor=A3; payload_owner=B0; auth_owner=A3] / sample=24 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + AUTHZ_POLICY |
| T07-P87-K06 | T07-P87: 경계 C | T07-P87: 경계 D | T07-P87: REORDER[in_seq=9,7,8; applied_version=4] / sample=31 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + SEQUENCE_STATE |
| T07-P87-K07 | T07-P87: 경계 A | T07-P87: 경계 B+C | T07-P87: DRAIN[ready=0; active=5; drain_deadline_s=12] / sample=38 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + DRAIN_STATE |
| T07-P87-K08 | T07-P87: 경계 B | T07-P87: 경계 C+D | T07-P87: RECOVERY_SCOPE[selected=109; expected=20; backup=1; dry_run=0] / sample=45 | T07-P87: 먼저 깨지는 경계를 판정 | T07-P87: T07-P87/CH08 + RECOVERY_AUDIT |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P87/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · replication·failover를 backend 관점에서 다루기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P87와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-B11 | T07-P87: leader election과 singleton work | T07-P87: network partition 뒤 leader가 둘처럼 보인다 | T07-P87: epoch와 idempotency를 적용한다 | T07-P87: T07-P87/CH08 증거와 형제 LESSON 증거를 분리 | T07-P87: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P87-B13 | T07-P87: multi-tenant isolation | T07-P87: tenant A가 connection을 대부분 사용한다 | T07-P87: isolation과 noisy neighbor를 본다 | T07-P87: T07-P87/CH08 증거와 형제 LESSON 증거를 분리 | T07-P87: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P87-B10 | T07-P87: distributed lock·lease·fencing token | T07-P87: lease가 끝난 old worker가 계속 write한다 | T07-P87: fencing token으로 stale writer를 막는다 | T07-P87: T07-P87/CH08 증거와 형제 LESSON 증거를 분리 | T07-P87: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P87-B14 | T07-P87: serverless와 long-running backend 실행 모델 비교 | T07-P87: function이 scale-out하며 DB connection이 급증한다 | T07-P87: serverless lifecycle과 global capacity를 본다 | T07-P87: T07-P87/CH08 증거와 형제 LESSON 증거를 분리 | T07-P87: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P87-B09 | T07-P87: CQRS를 읽기/쓰기 요구 차이에서 이해하기 | T07-P87: write model과 검색 화면 요구가 크게 다르다 | T07-P87: projection lag와 CQRS 필요성을 판단한다 | T07-P87: T07-P87/CH08 증거와 형제 LESSON 증거를 분리 | T07-P87: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · replication·failover를 backend 관점에서 다루기 — 선택형 실패 주입 6개

T07-P87: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P87 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-X01 | T07-P87: client disconnect | T07-P87: 응답 전에 연결이 끊김; sample=720 | T07-P87: 연결 종료와 server effect | T07-P87: T07-P87/CH02 판단과 별도 기록 | T07-P87: commit 시각·worker/outbox·request lifecycle |
| T07-P87-X02 | T07-P87: 재발 | T07-P87: 같은 오류가 잠시 뒤 다시 발생; sample=737 | T07-P87: 완화와 원인 제거 | T07-P87: T07-P87/CH02 판단과 별도 기록 | T07-P87: 재발 timeline·변경점·resource state |
| T07-P87-X03 | T07-P87: 소유권 위조 | T07-P87: tenant/owner가 payload에 포함됨; sample=754 | T07-P87: 식별 정보와 권한 근거 | T07-P87: T07-P87/CH02 판단과 별도 기록 | T07-P87: authenticated context·resource owner·policy result |
| T07-P87-X04 | T07-P87: 순서 역전 | T07-P87: event가 원래 순서와 반대로 도착; sample=771 | T07-P87: 수신 순서와 업무 순서 | T07-P87: T07-P87/CH02 판단과 별도 기록 | T07-P87: version/sequence·dedupe id·applied state |
| T07-P87-X05 | T07-P87: drain | T07-P87: 배포 중 기존 요청이 처리 중; sample=788 | T07-P87: 새 traffic 차단과 in-flight 처리 | T07-P87: T07-P87/CH02 판단과 별도 기록 | T07-P87: readiness·active requests·deadline·final state |
| T07-P87-X06 | T07-P87: 복구 범위 | T07-P87: 복구 script 대상이 예상보다 큼; sample=805 | T07-P87: 진단과 destructive recovery | T07-P87: T07-P87/CH02 판단과 별도 기록 | T07-P87: selected ids/count·backup·audit trail |

## CHAPTER 17 · replication·failover를 backend 관점에서 다루기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P87에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-E01 | T07-P87: 대표 실패를 원인으로 착각 | T07-P87: T07-P87/CH06 실패 case를 다른 입력으로 재현 | T07-P87: 현상과 원인을 같은 것으로 봄 | T07-P87: T07-P87/CH06 대표 실패와 T07-P87/CH08 증거를 다시 대조 | T07-P87: T07-P87/CH08 |
| T07-P87-E02 | T07-P87: 경계 A 생략 | T07-P87: T07-P87/CH04의 조건 하나를 반대로 설정 | T07-P87: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P87: T07-P87/CH04를 새 입력에 적용 | T07-P87: T07-P87/CH08 |
| T07-P87-E03 | T07-P87: 경계 B 생략 | T07-P87: T07-P87/CH05의 조건 하나를 반대로 설정 | T07-P87: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P87: T07-P87/CH05를 새 입력에 적용 | T07-P87: T07-P87/CH08 |
| T07-P87-E04 | T07-P87: 복구 상태 혼동 | T07-P87: T07-P87/CH07에서 처리 중단을 주입 | T07-P87: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P87: T07-P87/CH07에서 수명 경계를 다시 표시 | T07-P87: T07-P87/CH08 |
| T07-P87-E05 | T07-P87: 운영 한계 누락 | T07-P87: T07-P87/CH09에서 부하 또는 drain 조건을 변경 | T07-P87: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P87: T07-P87/CH09의 종료 조건을 다시 작성 | T07-P87: T07-P87/CH08 |
| T07-P87-E06 | T07-P87: 증거 없는 성공 판정 | T07-P87: T07-P87/CH08에서 증거 하나를 숨김 | T07-P87: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P87: T07-P87/CH08에서 독립 증거 둘을 선택 | T07-P87: T07-P87/CH08 |

## CHAPTER 18 · replication·failover를 backend 관점에서 다루기 — synthetic 관측값 판독 6개

T07-P87: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P87의 숫자 하나만으로 원인을 단정하지 않고 T07-P87/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-O01 | T07-P87/rpc_latency_ms | 447 | T07-P87: service 호출 지연 | T07-P87: 축=drain; 원인 확정 금지 | T07-P87: DRAIN_STATE + T07-P87/CH08 |
| T07-P87-O02 | T07-P87/message_lag | 464 | T07-P87: message lag | T07-P87: 축=복구 범위; 원인 확정 금지 | T07-P87: RECOVERY_AUDIT + T07-P87/CH08 |
| T07-P87-O03 | T07-P87/replica_lag_ms | 481 | T07-P87: replica lag | T07-P87: 축=재전송; 원인 확정 금지 | T07-P87: IDEMPOTENCY_RECORD + T07-P87/CH08 |
| T07-P87-O04 | T07-P87/leader_changes | 498 | T07-P87: leader 변경 수 | T07-P87: 축=구버전 client; 원인 확정 금지 | T07-P87: CLIENT_VERSION + T07-P87/CH08 |
| T07-P87-O05 | T07-P87/conflicts | 515 | T07-P87: 분산 상태 충돌 수 | T07-P87: 축=동시 변경; 원인 확정 금지 | T07-P87: STATE_VERSION + T07-P87/CH08 |
| T07-P87-O06 | T07-P87/failover_ms | 532 | T07-P87: failover 시간 | T07-P87: 축=unknown outcome; 원인 확정 금지 | T07-P87: PROVIDER_RESULT + T07-P87/CH08 |

## CHAPTER 19 · replication·failover를 backend 관점에서 다루기 — 선택형 코드 리뷰 6질문

T07-P87: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P87에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-R01 | T07-P87: 입력 경계 | T07-P87: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P87: RECOVERY_SCOPE[selected=232; expected=13; backup=1; dry_run=0] | T07-P87: T07-P87/CH04 | T07-P87: RECOVERY_AUDIT |
| T07-P87-R02 | T07-P87: 순서 | T07-P87: old/new event 순서가 바뀌어도 안전한가 | T07-P87: REPLAY[key=cmd-087-01; attempts=3; response_seen=0] | T07-P87: T07-P87/CH05 | T07-P87: IDEMPOTENCY_RECORD |
| T07-P87-R03 | T07-P87: retry | T07-P87: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P87: OLD_SCHEMA[client=v2; server=v3; extra_field=0] | T07-P87: T07-P87/CH06 | T07-P87: CLIENT_VERSION |
| T07-P87-R04 | T07-P87: 민감정보 | T07-P87: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P87: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=66] | T07-P87: T07-P87/CH07 | T07-P87: STATE_VERSION |
| T07-P87-R05 | T07-P87: 입력 경계 | T07-P87: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P87: UNKNOWN_OUTCOME[timeout_ms=145; provider_state=UNKNOWN; lookup_id=p08704] | T07-P87: T07-P87/CH04 | T07-P87: PROVIDER_RESULT |
| T07-P87-R06 | T07-P87: 순서 | T07-P87: old/new event 순서가 바뀌어도 안전한가 | T07-P87: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P87: T07-P87/CH05 | T07-P87: DURABLE_STATE |

## CHAPTER 20 · replication·failover를 backend 관점에서 다루기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P87에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-I01 | T07-P87: 회귀 고정 | T07-P87: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P87: REPLAY[key=cmd-087-00; attempts=2; response_seen=0] | T07-P87: T07-P87/CH08 + IDEMPOTENCY_RECORD | T07-P87-incident-662 |
| T07-P87-I02 | T07-P87: 영향 범위 | T07-P87: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P87: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P87: T07-P87/CH08 + CLIENT_VERSION | T07-P87-incident-663 |
| T07-P87-I03 | T07-P87: 변경 동결 | T07-P87: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P87: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=53] | T07-P87: T07-P87/CH08 + STATE_VERSION | T07-P87-incident-664 |
| T07-P87-I04 | T07-P87: correlation | T07-P87: 한 request/job/resource id를 시간축에 고정 | T07-P87: UNKNOWN_OUTCOME[timeout_ms=134; provider_state=UNKNOWN; lookup_id=p08703] | T07-P87: T07-P87/CH08 + PROVIDER_RESULT | T07-P87-incident-665 |
| T07-P87-I05 | T07-P87: 마지막 정상 | T07-P87: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P87: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P87: T07-P87/CH08 + DURABLE_STATE | T07-P87-incident-666 |
| T07-P87-I06 | T07-P87: 가설 검증 | T07-P87: 원인 후보 하나만 뒤집어 재현 | T07-P87: OVERLOAD[rps=368; p99_ms=1128; queue=27] | T07-P87: T07-P87/CH08 + QUEUE_PRESSURE | T07-P87-incident-667 |

## CHAPTER 21 · replication·failover를 backend 관점에서 다루기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P87/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P87/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P87/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P87/CH04~T07-P87/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P87/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P87/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P87/CH18 signal 두 개와 T07-P87/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P87/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P87/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · replication·failover를 backend 관점에서 다루기 — 통합 casebook 16문제

T07-P87 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P87 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P87-C01 | T07-P87: 경계 A | T07-P87: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P87: load=823, window=49s | T07-P87: review=순서 | T07-P87: 경계 A 위반 여부를 판정 | T07-P87: CLIENT_VERSION + T07-P87/CH08 |
| T07-P87-C02 | T07-P87: 경계 B | T07-P87: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=40] | T07-P87: load=846, window=68s | T07-P87: review=관측 | T07-P87: 경계 B 위반 여부를 판정 | T07-P87: STATE_VERSION + T07-P87/CH08 |
| T07-P87-C03 | T07-P87: 경계 C | T07-P87: UNKNOWN_OUTCOME[timeout_ms=123; provider_state=UNKNOWN; lookup_id=p08702] | T07-P87: load=869, window=87s | T07-P87: review=상태 변경 | T07-P87: 경계 C 위반 여부를 판정 | T07-P87: PROVIDER_RESULT + T07-P87/CH08 |
| T07-P87-C04 | T07-P87: 경계 D | T07-P87: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P87: load=892, window=16s | T07-P87: review=retry | T07-P87: 경계 D 위반 여부를 판정 | T07-P87: DURABLE_STATE + T07-P87/CH08 |
| T07-P87-C05 | T07-P87: 경계 A | T07-P87: OVERLOAD[rps=335; p99_ms=1011; queue=33] | T07-P87: load=915, window=35s | T07-P87: review=복구 | T07-P87: 경계 A 위반 여부를 판정 | T07-P87: QUEUE_PRESSURE + T07-P87/CH08 |
| T07-P87-C06 | T07-P87: 경계 B | T07-P87: SAMPLING[sample_rate=22%; trace_present=1; metric_present=1] | T07-P87: load=938, window=54s | T07-P87: review=동시성 | T07-P87: 경계 B 위반 여부를 판정 | T07-P87: TRACE_METRIC_CROSSCHECK + T07-P87/CH08 |
| T07-P87-C07 | T07-P87: 경계 C | T07-P87: DISCONNECT[disconnect_ms=28; commit_state=UNKNOWN; request=087-06] | T07-P87: load=961, window=73s | T07-P87: review=민감정보 | T07-P87: 경계 C 위반 여부를 판정 | T07-P87: COMMIT_TIMELINE + T07-P87/CH08 |
| T07-P87-C08 | T07-P87: 경계 D | T07-P87: RECURRENCE[occurrence=4; interval_s=108; mitigation_applied=1] | T07-P87: load=984, window=92s | T07-P87: review=중복 | T07-P87: 경계 D 위반 여부를 판정 | T07-P87: RECURRENCE_TIMELINE + T07-P87/CH08 |
| T07-P87-C09 | T07-P87: 경계 A | T07-P87: OWNER_SPOOF[actor=A3; payload_owner=B3; auth_owner=A3] | T07-P87: load=107, window=21s | T07-P87: review=권한 | T07-P87: 경계 A 위반 여부를 판정 | T07-P87: AUTHZ_POLICY + T07-P87/CH08 |
| T07-P87-C10 | T07-P87: 경계 B | T07-P87: REORDER[in_seq=12,10,11; applied_version=4] | T07-P87: load=130, window=40s | T07-P87: review=입력 경계 | T07-P87: 경계 B 위반 여부를 판정 | T07-P87: SEQUENCE_STATE + T07-P87/CH08 |
| T07-P87-C11 | T07-P87: 경계 C | T07-P87: DRAIN[ready=0; active=10; drain_deadline_s=7] | T07-P87: load=153, window=59s | T07-P87: review=timeout | T07-P87: 경계 C 위반 여부를 판정 | T07-P87: DRAIN_STATE + T07-P87/CH08 |
| T07-P87-C12 | T07-P87: 경계 D | T07-P87: RECOVERY_SCOPE[selected=142; expected=21; backup=1; dry_run=1] | T07-P87: load=176, window=78s | T07-P87: review=자원 | T07-P87: 경계 D 위반 여부를 판정 | T07-P87: RECOVERY_AUDIT + T07-P87/CH08 |
| T07-P87-C13 | T07-P87: 경계 A | T07-P87: REPLAY[key=cmd-087-12; attempts=2; response_seen=0] | T07-P87: load=199, window=97s | T07-P87: review=순서 | T07-P87: 경계 A 위반 여부를 판정 | T07-P87: IDEMPOTENCY_RECORD + T07-P87/CH08 |
| T07-P87-C14 | T07-P87: 경계 B | T07-P87: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P87: load=222, window=26s | T07-P87: review=관측 | T07-P87: 경계 B 위반 여부를 판정 | T07-P87: CLIENT_VERSION + T07-P87/CH08 |
| T07-P87-C15 | T07-P87: 경계 C | T07-P87: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=15] | T07-P87: load=245, window=45s | T07-P87: review=상태 변경 | T07-P87: 경계 C 위반 여부를 판정 | T07-P87: STATE_VERSION + T07-P87/CH08 |
| T07-P87-C16 | T07-P87: 경계 D | T07-P87: UNKNOWN_OUTCOME[timeout_ms=266; provider_state=UNKNOWN; lookup_id=p08715] | T07-P87: load=268, window=64s | T07-P87: review=retry | T07-P87: 경계 D 위반 여부를 판정 | T07-P87: PROVIDER_RESULT + T07-P87/CH08 |

채점은 결론보다 근거를 본다. T07-P87/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · replication·failover를 backend 관점에서 다루기 — evidence 판독 문제 14개

T07-P87 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P87 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P87-V01 | T07-P87: latency=1787ms; queue=49; retry=3 | T07-P87: 동시 변경 | T07-P87: 축=동시 변경; 원인 확정은 보류 | T07-P87: STATE_VERSION + T07-P87/CH08 | T07-P87: 피할 오판=증거 혼동 |
| T07-P87-V02 | T07-P87: latency=54ms; queue=60; retry=6 | T07-P87: unknown outcome | T07-P87: 축=unknown outcome; 원인 확정은 보류 | T07-P87: PROVIDER_RESULT + T07-P87/CH08 | T07-P87: 피할 오판=재시도 오판 |
| T07-P87-V03 | T07-P87: latency=121ms; queue=71; retry=2 | T07-P87: 재시작 | T07-P87: 축=재시작; 원인 확정은 보류 | T07-P87: DURABLE_STATE + T07-P87/CH08 | T07-P87: 피할 오판=소유권 혼동 |
| T07-P87-V04 | T07-P87: latency=188ms; queue=2; retry=5 | T07-P87: 과부하 | T07-P87: 축=과부하; 원인 확정은 보류 | T07-P87: QUEUE_PRESSURE + T07-P87/CH08 | T07-P87: 피할 오판=동시성 무시 |
| T07-P87-V05 | T07-P87: latency=255ms; queue=13; retry=1 | T07-P87: sampling | T07-P87: 축=sampling; 원인 확정은 보류 | T07-P87: TRACE_METRIC_CROSSCHECK + T07-P87/CH08 | T07-P87: 피할 오판=순서 가정 |
| T07-P87-V06 | T07-P87: latency=322ms; queue=24; retry=4 | T07-P87: client disconnect | T07-P87: 축=client disconnect; 원인 확정은 보류 | T07-P87: COMMIT_TIMELINE + T07-P87/CH08 | T07-P87: 피할 오판=상태 수명 혼동 |
| T07-P87-V07 | T07-P87: latency=389ms; queue=35; retry=0 | T07-P87: 재발 | T07-P87: 축=재발; 원인 확정은 보류 | T07-P87: RECURRENCE_TIMELINE + T07-P87/CH08 | T07-P87: 피할 오판=운영 한계 누락 |
| T07-P87-V08 | T07-P87: latency=456ms; queue=46; retry=3 | T07-P87: 소유권 위조 | T07-P87: 축=소유권 위조; 원인 확정은 보류 | T07-P87: AUTHZ_POLICY + T07-P87/CH08 | T07-P87: 피할 오판=오류 합치기 |
| T07-P87-V09 | T07-P87: latency=523ms; queue=57; retry=6 | T07-P87: 순서 역전 | T07-P87: 축=순서 역전; 원인 확정은 보류 | T07-P87: SEQUENCE_STATE + T07-P87/CH08 | T07-P87: 피할 오판=복구 과잉 |
| T07-P87-V10 | T07-P87: latency=590ms; queue=68; retry=2 | T07-P87: drain | T07-P87: 축=drain; 원인 확정은 보류 | T07-P87: DRAIN_STATE + T07-P87/CH08 | T07-P87: 피할 오판=잘못된 전제 |
| T07-P87-V11 | T07-P87: latency=657ms; queue=79; retry=5 | T07-P87: 복구 범위 | T07-P87: 축=복구 범위; 원인 확정은 보류 | T07-P87: RECOVERY_AUDIT + T07-P87/CH08 | T07-P87: 피할 오판=경계 누락 |
| T07-P87-V12 | T07-P87: latency=724ms; queue=10; retry=1 | T07-P87: 재전송 | T07-P87: 축=재전송; 원인 확정은 보류 | T07-P87: IDEMPOTENCY_RECORD + T07-P87/CH08 | T07-P87: 피할 오판=증거 혼동 |
| T07-P87-V13 | T07-P87: latency=791ms; queue=21; retry=4 | T07-P87: 구버전 client | T07-P87: 축=구버전 client; 원인 확정은 보류 | T07-P87: CLIENT_VERSION + T07-P87/CH08 | T07-P87: 피할 오판=재시도 오판 |
| T07-P87-V14 | T07-P87: latency=858ms; queue=32; retry=0 | T07-P87: 동시 변경 | T07-P87: 축=동시 변경; 원인 확정은 보류 | T07-P87: STATE_VERSION + T07-P87/CH08 | T07-P87: 피할 오판=소유권 혼동 |

## CHAPTER 24 · replication·failover를 backend 관점에서 다루기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P87에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P87-D01 | T07-P87: schema 변경 | T07-P87: 필드 이름·형식·required 조건을 바꾼다 | T07-P87: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P87: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P87: DURABLE_STATE + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P87-D02 | T07-P87: 결과 합치기 | T07-P87: 여러 오류를 하나의 status/error code로 합친다 | T07-P87: client 행동과 retry 가능성을 잃지 않는지 | T07-P87: OVERLOAD[rps=236; p99_ms=660; queue=11] | T07-P87: QUEUE_PRESSURE + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P87-D03 | T07-P87: retry 추가 | T07-P87: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P87: unknown outcome과 duplicate side effect를 구분하는지 | T07-P87: SAMPLING[sample_rate=63%; trace_present=0; metric_present=1] | T07-P87: TRACE_METRIC_CROSSCHECK + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P87-D04 | T07-P87: 로그 확대 | T07-P87: debug를 위해 payload와 context 기록을 늘린다 | T07-P87: secret·PII·cardinality 비용을 통제하는지 | T07-P87: DISCONNECT[disconnect_ms=86; commit_state=UNKNOWN; request=087-03] | T07-P87: COMMIT_TIMELINE + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P87-D05 | T07-P87: 강제 종료 | T07-P87: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P87: in-flight request와 background work의 결과를 잃는지 | T07-P87: RECURRENCE[occurrence=6; interval_s=75; mitigation_applied=1] | T07-P87: RECURRENCE_TIMELINE + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P87-D06 | T07-P87: AI package 추가 | T07-P87: AI가 제안한 새 dependency를 도입한다 | T07-P87: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P87: OWNER_SPOOF[actor=A3; payload_owner=B0; auth_owner=A3] | T07-P87: AUTHZ_POLICY + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P87-D07 | T07-P87: 비동기화 | T07-P87: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P87: durability·status API·worker retry 계약이 생기는지 | T07-P87: REORDER[in_seq=9,7,8; applied_version=4] | T07-P87: SEQUENCE_STATE + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P87-D08 | T07-P87: 권한 shortcut | T07-P87: payload의 owner/tenant id를 바로 사용한다 | T07-P87: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P87: DRAIN[ready=0; active=5; drain_deadline_s=12] | T07-P87: DRAIN_STATE + T07-P87/CH08 | T07-P87: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P87: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · replication·failover를 backend 관점에서 다루기 — 최종 contract와 evidence spine

**최종 계약:** 복제는 가용성과 읽기 확장을 도울 수 있지만 failover 순간의 lag와 consistency를 고려해야 한다.

**정상 메커니즘:** primary/replica 역할, replication position, promotion 조건과 client reconnect 정책을 계약으로 둔다.

**대표 실패:** replica가 있으니 데이터 손실이 0이라고 가정하거나 오래된 connection이 옛 primary로 계속 write한다.

**검증 evidence:** replication lag/position, role change, write error와 reconnect를 본다.

**직접 행동:** replica lag가 허용 기준 이하인지 판정한다.

**다음 연결:** `multi-tenant isolation`.

`replication·failover를 backend 관점에서 다루기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | replication·failover를 backend 관점에서 다루기의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | replication·failover를 backend 관점에서 다루기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | replication·failover를 backend 관점에서 다루기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | replication·failover를 backend 관점에서 다루기의 개념·실패·운영 판단 교차 확인 |

`replication·failover를 backend 관점에서 다루기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
