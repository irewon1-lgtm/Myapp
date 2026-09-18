# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 04 · cache·queue·background work로 느린 일을 분리하기

### LESSON 08 · at-least-once와 중복 처리

## CHAPTER 01 · at-least-once와 중복 처리 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 많은 queue는 message 유실을 줄이기 위해 실패 시 다시 전달할 수 있어 consumer가 같은 message를 여러 번 볼 수 있다.

**아주 쉬운 사건.** worker가 effect 후 ack 전에 죽는다. 이 사건에서는 먼저 **duplicate delivery를 정상 경로로 다룬다**.

**왜 필요한가.** 정상 동작은 event id와 처리 기록 또는 idempotent operation으로 중복 delivery가 중복 side effect가 되지 않게 한다. 반대로 delivery count=1을 가정해 이메일·결제·적립이 여러 번 실행된다.

**암기:** `at-least-once와 중복 처리`의 역할 한 줄.

**직접 이해:** `at-least-once와 중복 처리`의 입력·상태·결과 경계.

**AI 위임 가능:** `at-least-once와 중복 처리` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `worker가 effect 후 ack 전에 죽는다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 message id, delivery count, processing key, side-effect id를 연결한다.

## CHAPTER 02 · at-least-once와 중복 처리 — 아주 쉬운 예를 한 단계씩 해석

T07-P53: `worker가 effect 후 ack 전에 죽는다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | worker가 effect 후 ack 전에 죽는다 | T07-P53 외부 입력 | T07-P53: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | duplicate delivery를 정상 경로로 다룬다 | T07-P53 판단 기준 | T07-P53/CH08 관측표와 대조 |
| 정상 경로 | T07-P53/CH03 M1→M5 | at-least-once와 중복 처리: 완료 시점을 단계별로 분리 | T07-P53: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P53/CH06 delivery count=1을 가정해 이메일·결제·적립이 여러 번 실행된다. | T07-P53: 깨진 계약 하나를 특정 | at-least-once와 중복 처리: 증상과 원인을 분리 |
| 재검증 | T07-P53/CH10 직접 실행 | T07-P53: 예상값 T07-P053 RECEIVED->CHECKED->APPLIED 기록 | at-least-once와 중복 처리: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P53/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P53/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · at-least-once와 중복 처리 — 내부 메커니즘과 상태 전이

event id와 처리 기록 또는 idempotent operation으로 중복 delivery가 중복 side effect가 되지 않게 한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | worker가 effect 후 ack 전에 죽는다 | source/actor/size를 보존 |
| M2 | 경계 판단 | duplicate delivery를 정상 경로로 다룬다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | event id와 처리 기록 또는 idempotent operation으로 중복 delivery가 중복 side effect가 되지 않게 한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | message id, delivery count, processing key, side-effect id를 연결한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 중복 message id를 제거하되 최초 순서를 유지하는 코드를 실행한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`at-least-once와 중복 처리` 흐름을 framework 이름 없이 설명한다.

막히면 `worker가 effect 후 ack 전에 죽는다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · at-least-once와 중복 처리 — 실전 경계 A

**경계 A.** at-least-once broker에서는 message가 처리 중 consumer crash나 ack 손실로 다시 전달될 수 있으므로 handler가 duplicate를 정상 경로로 받아들여야 한다.

T07-P53/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P53에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P53/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P53/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P53): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P53/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P53-A1-591 | T07-P53 조건 | at-least-once broker에서는 message가 처리 중 consumer crash나 ack 손실로 다시 전달될 수 있으므로 handler가 duplicate를 정상 경로로 받아들여야 한다. |
| T07-P53-A2-592 | T07-P53 변화점 | T07-P53/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P53-A3-593 | T07-P53 반례 | T07-P53/CH06 대표 실패와 A 위반을 구별 |
| T07-P53-A4-594 | T07-P53 근거 | T07-P53/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P53-A5-595 | T07-P53 재실험 | worker가 effect 후 ack 전에 죽는다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · at-least-once와 중복 처리 — 실전 경계 B

**경계 B.** dedupe key를 side effect와 같은 durable transaction 경계에 기록하면 ‘effect는 했는데 dedupe 기록은 못 한’ 틈을 줄일 수 있다.

T07-P53/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P53에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P53/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P53/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P53): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P53/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P53-B1-622 | T07-P53 조건 | dedupe key를 side effect와 같은 durable transaction 경계에 기록하면 ‘effect는 했는데 dedupe 기록은 못 한’ 틈을 줄일 수 있다. |
| T07-P53-B2-623 | T07-P53 독립성 | T07-P53/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P53-B3-624 | T07-P53 상태 | T07-P53/CH03 before·after 위치를 다시 지정 |
| T07-P53-B4-625 | T07-P53 반증 | T07-P53/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P53-B5-626 | T07-P53 적용 | at-least-once와 중복 처리의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · at-least-once와 중복 처리 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **delivery count=1을 가정해 이메일·결제·적립이 여러 번 실행된다.**

아래 여섯 사례는 T07-P53의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P53-F01 | T07-P53: 대표 실패 | T07-P53: T07-P53/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P53: 현상만 보고 원인을 확정 | T07-P53/CH08 evidence map에서 상태를 대조 |
| T07-P53-F02 | T07-P53: 경계 A 누락 | T07-P53: T07-P53/CH04 경계 A 위반 입력 | T07-P53: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P53/CH08 evidence map에서 상태를 대조 |
| T07-P53-F03 | T07-P53: 경계 B 누락 | T07-P53: T07-P53/CH05 경계 B 위반 입력 | T07-P53: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P53/CH08 evidence map에서 상태를 대조 |
| T07-P53-F04 | T07-P53: 복구 경계 C 누락 | T07-P53: T07-P53/CH07 경계 C 복구 조건 | T07-P53: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P53/CH08 evidence map에서 상태를 대조 |
| T07-P53-F05 | T07-P53: 운영 경계 D 누락 | T07-P53: T07-P53/CH09 경계 D 운영 조건 | T07-P53: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P53/CH08 evidence map에서 상태를 대조 |
| T07-P53-F06 | T07-P53: 증거 없는 결론 | T07-P53: T07-P53/CH02 첫 판단만 존재 | T07-P53: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P53/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P53/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · at-least-once와 중복 처리 — 복구 가능한 상태와 수명

**경계 C.** exactly-once라는 표현은 broker delivery, DB write, 외부 API side effect 중 어느 boundary를 말하는지 정의하지 않으면 오해를 만든다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P53에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P53/CH08 evidence map을 본다. 복구 후에는 T07-P53/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P53에서 이미 확정된 side effect는 T07-P53/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P53-R1-684 | T07-P53 중단 직전 | T07-P53/CH03에서 이미 확정된 상태만 표시 |
| T07-P53-R2-685 | T07-P53 재시작 직후 | exactly-once라는 표현은 broker delivery, DB write, 외부 API side effect 중 어느 boundary를 말하는지 정의하지 않으면 오해를 만든다. |
| T07-P53-R3-686 | T07-P53 재검증 | T07-P53/CH08 근거로 중복·누락 여부 확인 |
| T07-P53-R4-687 | T07-P53 재실행 | T07-P53/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · at-least-once와 중복 처리 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | worker가 effect 후 ack 전에 죽는다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | duplicate delivery를 정상 경로로 다룬다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | event id와 처리 기록 또는 idempotent operation으로 중복 delivery가 중복 side effect가 되지 않게 한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | delivery count=1을 가정해 이메일·결제·적립이 여러 번 실행된다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | message id, delivery count, processing key, side-effect id를 연결한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`at-least-once와 중복 처리` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · at-least-once와 중복 처리 — 운영 한계와 종료 조건

**경계 D.** ack는 durable effect가 확정된 뒤 보내고, effect가 이미 존재하는 duplicate는 다시 실행하지 않고 성공적으로 소비 완료할 수 있게 설계한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P53/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P53/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P53 과제: 경계 D와 T07-P53/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P53-O1-746 | synthetic-load=66 | T07-P53/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P53-O2-747 | synthetic-budget=896ms | T07-P53 timeout과 unknown outcome을 분리 |
| T07-P53-O3-748 | T07-P53 종료 | ack는 durable effect가 확정된 뒤 보내고, effect가 이미 존재하는 duplicate는 다시 실행하지 않고 성공적으로 소비 완료할 수 있게 설계한다. |
| T07-P53-O4-749 | T07-P53 완화 | T07-P53/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · at-least-once와 중복 처리 — 직접 실행하는 작은 모델

`at-least-once와 중복 처리` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P053 RECEIVED->CHECKED->APPLIED`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P053";
const stages = ["RECEIVED", "CHECKED", "APPLIED", "DONE"];
const stopAt = 3;
const visited = stages.slice(0, stopAt);
console.log(marker, visited.join("->"));
```

기준 출력: `T07-P053 RECEIVED->CHECKED->APPLIED`.

`at-least-once와 중복 처리`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P53-L1-778 | constmarker="T07-P053"; | T07-P53 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P53-L2-779 | conststages=["RECEIVED","CHECKED","APPLIED","DONE"]; | T07-P53 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P53-L3-780 | conststopAt=3; | T07-P53 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P53-L4-781 | constvisited=stages.slice(0,stopAt); | T07-P53 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P53-L5-782 | console.log(marker,visited.join("->")); | T07-P53 출력 관측점; 예상 `T07-P053 RECEIVED->CHECKED->APPLIED`와 비교 |
| T07-P53-LX-867 | T07-P53 실행 기록 | T07-P53 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · at-least-once와 중복 처리 — 한 부분만 수정하고 다시 예측

수정 과제: **stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다**.

수정 전은 `T07-P053 RECEIVED->CHECKED->APPLIED`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P53/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P53-D1-808 | 기준 `T07-P053 RECEIVED->CHECKED->APPLIED` | T07-P53 수정 전 실행을 먼저 재현 |
| T07-P53-D2-809 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | T07-P53 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P53-D3-810 | T07-P53 새 예측 | T07-P53 실행 전에 출력·상태를 먼저 기록 |
| T07-P53-D4-811 | T07-P53 재실행 | T07-P53/CH10 실제값과 새 예측을 대조 |
| T07-P53-D5-812 | T07-P53 반례 | T07-P53/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P53-D6-813 | T07-P53 근거 | T07-P53/CH08 상태가 설명과 일치해야 완료 |
| T07-P53-D7-814 | T07-P53 이유 | T07-P53 변경 이유를 at-least-once와 중복 처리 계약과 연결해 설명 |

## CHAPTER 12 · at-least-once와 중복 처리 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: at-least-once와 중복 처리 | 많은 queue는 message 유실을 줄이기 위해 실패 시 다시 전달할 수 있어 consumer가 같은 message를 여러 번 볼 수 있다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P53/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | event id와 처리 기록 또는 idempotent operation으로 중복 delivery가 중복 side effect가 되지 않게 한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | delivery count=1을 가정해 이메일·결제·적립이 여러 번 실행된다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | message id, delivery count, processing key, side-effect id를 연결한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P53/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P53/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P53/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P53/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `at-least-once와 중복 처리` 실행 코드 수정 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | `at-least-once와 중복 처리` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P53/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P53/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · at-least-once와 중복 처리 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 많은 queue는 message 유실을 줄이기 위해 실패 시 다시 전달할 수 있어 consumer가 같은 message를 여러 번 볼 수 있다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P53/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P53/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P53/CH10 실행용 boilerplate·test 후보 | T07-P53: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P053 RECEIVED->CHECKED->APPLIED` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P53/CH02의 판단 기준과 T07-P53/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P53-AI1-870 | T07-P53 사람 결정 | T07-P53 업무 의미·허용 위험·완료 기준 소유 |
| T07-P53-AI2-871 | T07-P53 AI 초안 | T07-P53/CH10 boilerplate·test 후보까지만 위임 |
| T07-P53-AI3-872 | T07-P53 검증 | T07-P53/CH06 반례와 T07-P53/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · at-least-once와 중복 처리 — 경계 조합 실험 8개

T07-P53: T07-P53/CH04~T07-P53/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P53의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-K01 | T07-P53: 경계 A | T07-P53: 경계 B | T07-P53: RECURRENCE[occurrence=3; interval_s=111; mitigation_applied=1] / sample=67 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + RECURRENCE_TIMELINE |
| T07-P53-K02 | T07-P53: 경계 A | T07-P53: 경계 C | T07-P53: LARGE_INPUT[body_kb=1248; limit_kb=640; parsed=0] / sample=74 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + SIZE_LIMIT |
| T07-P53-K03 | T07-P53: 경계 A | T07-P53: 경계 D | T07-P53: REORDER[in_seq=6,4,5; applied_version=6] / sample=81 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + SEQUENCE_STATE |
| T07-P53-K04 | T07-P53: 경계 B | T07-P53: 경계 C | T07-P53: DRAIN[ready=0; active=16; drain_deadline_s=9] / sample=88 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + DRAIN_STATE |
| T07-P53-K05 | T07-P53: 경계 B | T07-P53: 경계 D | T07-P53: RECOVERY_SCOPE[selected=145; expected=6; backup=1; dry_run=1] / sample=95 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + RECOVERY_AUDIT |
| T07-P53-K06 | T07-P53: 경계 C | T07-P53: 경계 D | T07-P53: REPLAY[key=cmd-053-06; attempts=2; response_seen=0] / sample=13 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + IDEMPOTENCY_RECORD |
| T07-P53-K07 | T07-P53: 경계 A | T07-P53: 경계 B+C | T07-P53: CONCURRENT_WRITE[actors=2; base_version=9; writes=2; gap_ms=25] / sample=20 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + STATE_VERSION |
| T07-P53-K08 | T07-P53: 경계 B | T07-P53: 경계 C+D | T07-P53: UNKNOWN_OUTCOME[timeout_ms=258; provider_state=UNKNOWN; lookup_id=p05308] / sample=27 | T07-P53: 먼저 깨지는 경계를 판정 | T07-P53: T07-P53/CH08 + PROVIDER_RESULT |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P53/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · at-least-once와 중복 처리 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P53와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-B07 | T07-P53: queue는 일을 나중에 처리하도록 경계를 만든다 | T07-P53: report 생성 요청을 queue에 넣는다 | T07-P53: 접수와 완료를 분리한다 | T07-P53: T07-P53/CH08 증거와 형제 LESSON 증거를 분리 | T07-P53: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P53-B09 | T07-P53: retry·backoff·jitter·DLQ | T07-P53: dependency 장애에 모두 즉시 retry한다 | T07-P53: backoff·jitter·retry budget을 적용한다 | T07-P53: T07-P53/CH08 증거와 형제 LESSON 증거를 분리 | T07-P53: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P53-B06 | T07-P53: in-process cache와 distributed cache 선택 | T07-P53: instance 두 개가 서로 다른 local cache를 가진다 | T07-P53: 일관성과 network 비용을 비교한다 | T07-P53: T07-P53/CH08 증거와 형제 LESSON 증거를 분리 | T07-P53: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P53-B10 | T07-P53: ordering과 partition key | T07-P53: 한 tenant가 같은 partition을 독점한다 | T07-P53: ordering과 hot partition을 함께 본다 | T07-P53: T07-P53/CH08 증거와 형제 LESSON 증거를 분리 | T07-P53: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P53-B05 | T07-P53: HTTP cache와 ETag·conditional request | T07-P53: client가 ETag를 다시 보낸다 | T07-P53: 304와 representation version을 판단한다 | T07-P53: T07-P53/CH08 증거와 형제 LESSON 증거를 분리 | T07-P53: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · at-least-once와 중복 처리 — 선택형 실패 주입 6개

T07-P53: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P53 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-X01 | T07-P53: 순서 역전 | T07-P53: event가 원래 순서와 반대로 도착; sample=663 | T07-P53: 수신 순서와 업무 순서 | T07-P53: T07-P53/CH02 판단과 별도 기록 | T07-P53: version/sequence·dedupe id·applied state |
| T07-P53-X02 | T07-P53: drain | T07-P53: 배포 중 기존 요청이 처리 중; sample=680 | T07-P53: 새 traffic 차단과 in-flight 처리 | T07-P53: T07-P53/CH02 판단과 별도 기록 | T07-P53: readiness·active requests·deadline·final state |
| T07-P53-X03 | T07-P53: 복구 범위 | T07-P53: 복구 script 대상이 예상보다 큼; sample=697 | T07-P53: 진단과 destructive recovery | T07-P53: T07-P53/CH02 판단과 별도 기록 | T07-P53: selected ids/count·backup·audit trail |
| T07-P53-X04 | T07-P53: 재전송 | T07-P53: 응답 유실 뒤 같은 command가 다시 도착함; sample=714 | T07-P53: 중복 side effect 여부 | T07-P53: T07-P53/CH02 판단과 별도 기록 | T07-P53: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P53-X05 | T07-P53: 동시 변경 | T07-P53: 두 actor가 같은 resource를 수정함; sample=731 | T07-P53: lost update 또는 conflict | T07-P53: T07-P53/CH02 판단과 별도 기록 | T07-P53: version·affected rows·lock/wait 기록 |
| T07-P53-X06 | T07-P53: unknown outcome | T07-P53: dependency timeout 후 성공 여부 불명; sample=748 | T07-P53: 실패와 미확정 결과 | T07-P53: T07-P53/CH02 판단과 별도 기록 | T07-P53: provider id·조회 결과·retry history |

## CHAPTER 17 · at-least-once와 중복 처리 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P53에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-E01 | T07-P53: 대표 실패를 원인으로 착각 | T07-P53: T07-P53/CH06 실패 case를 다른 입력으로 재현 | T07-P53: 현상과 원인을 같은 것으로 봄 | T07-P53: T07-P53/CH06 대표 실패와 T07-P53/CH08 증거를 다시 대조 | T07-P53: T07-P53/CH08 |
| T07-P53-E02 | T07-P53: 경계 A 생략 | T07-P53: T07-P53/CH04의 조건 하나를 반대로 설정 | T07-P53: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P53: T07-P53/CH04를 새 입력에 적용 | T07-P53: T07-P53/CH08 |
| T07-P53-E03 | T07-P53: 경계 B 생략 | T07-P53: T07-P53/CH05의 조건 하나를 반대로 설정 | T07-P53: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P53: T07-P53/CH05를 새 입력에 적용 | T07-P53: T07-P53/CH08 |
| T07-P53-E04 | T07-P53: 복구 상태 혼동 | T07-P53: T07-P53/CH07에서 처리 중단을 주입 | T07-P53: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P53: T07-P53/CH07에서 수명 경계를 다시 표시 | T07-P53: T07-P53/CH08 |
| T07-P53-E05 | T07-P53: 운영 한계 누락 | T07-P53: T07-P53/CH09에서 부하 또는 drain 조건을 변경 | T07-P53: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P53: T07-P53/CH09의 종료 조건을 다시 작성 | T07-P53: T07-P53/CH08 |
| T07-P53-E06 | T07-P53: 증거 없는 성공 판정 | T07-P53: T07-P53/CH08에서 증거 하나를 숨김 | T07-P53: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P53: T07-P53/CH08에서 독립 증거 둘을 선택 | T07-P53: T07-P53/CH08 |

## CHAPTER 18 · at-least-once와 중복 처리 — synthetic 관측값 판독 6개

T07-P53: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P53의 숫자 하나만으로 원인을 단정하지 않고 T07-P53/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-O01 | T07-P53/cache_hit_ratio | 215 | T07-P53: cache hit 비율 | T07-P53: 축=동시 변경; 원인 확정 금지 | T07-P53: STATE_VERSION + T07-P53/CH08 |
| T07-P53-O02 | T07-P53/cache_age_s | 232 | T07-P53: cache 항목 나이 | T07-P53: 축=unknown outcome; 원인 확정 금지 | T07-P53: PROVIDER_RESULT + T07-P53/CH08 |
| T07-P53-O03 | T07-P53/queue_depth | 249 | T07-P53: 대기 job 수 | T07-P53: 축=재시작; 원인 확정 금지 | T07-P53: DURABLE_STATE + T07-P53/CH08 |
| T07-P53-O04 | T07-P53/job_attempt | 266 | T07-P53: job 실행 횟수 | T07-P53: 축=과부하; 원인 확정 금지 | T07-P53: QUEUE_PRESSURE + T07-P53/CH08 |
| T07-P53-O05 | T07-P53/dlq_count | 283 | T07-P53: DLQ 항목 수 | T07-P53: 축=sampling; 원인 확정 금지 | T07-P53: TRACE_METRIC_CROSSCHECK + T07-P53/CH08 |
| T07-P53-O06 | T07-P53/worker_latency_ms | 300 | T07-P53: worker 처리 지연 | T07-P53: 축=client disconnect; 원인 확정 금지 | T07-P53: COMMIT_TIMELINE + T07-P53/CH08 |

## CHAPTER 19 · at-least-once와 중복 처리 — 선택형 코드 리뷰 6질문

T07-P53: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P53에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-R01 | T07-P53: 관측 | T07-P53: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P53: UNKNOWN_OUTCOME[timeout_ms=170; provider_state=UNKNOWN; lookup_id=p05300] | T07-P53: T07-P53/CH04 | T07-P53: PROVIDER_RESULT |
| T07-P53-R02 | T07-P53: 복구 | T07-P53: 재시작 뒤에도 필요한 상태가 남는가 | T07-P53: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P53: T07-P53/CH05 | T07-P53: DURABLE_STATE |
| T07-P53-R03 | T07-P53: 중복 | T07-P53: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P53: OVERLOAD[rps=476; p99_ms=813; queue=29] | T07-P53: T07-P53/CH06 | T07-P53: QUEUE_PRESSURE |
| T07-P53-R04 | T07-P53: timeout | T07-P53: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P53: SAMPLING[sample_rate=80%; trace_present=1; metric_present=1] | T07-P53: T07-P53/CH07 | T07-P53: TRACE_METRIC_CROSSCHECK |
| T07-P53-R05 | T07-P53: 관측 | T07-P53: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P53: DISCONNECT[disconnect_ms=103; commit_state=UNKNOWN; request=053-04] | T07-P53: T07-P53/CH04 | T07-P53: COMMIT_TIMELINE |
| T07-P53-R06 | T07-P53: 복구 | T07-P53: 재시작 뒤에도 필요한 상태가 남는가 | T07-P53: RECURRENCE[occurrence=2; interval_s=155; mitigation_applied=1] | T07-P53: T07-P53/CH05 | T07-P53: RECURRENCE_TIMELINE |

## CHAPTER 20 · at-least-once와 중복 처리 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P53에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-I01 | T07-P53: 복구 확인 | T07-P53: durable state와 사용자 결과를 모두 확인 | T07-P53: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P53: T07-P53/CH08 + DURABLE_STATE | T07-P53-incident-016 |
| T07-P53-I02 | T07-P53: 재주입 | T07-P53: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P53: OVERLOAD[rps=443; p99_ms=696; queue=15] | T07-P53: T07-P53/CH08 + QUEUE_PRESSURE | T07-P53-incident-017 |
| T07-P53-I03 | T07-P53: 회귀 고정 | T07-P53: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P53: SAMPLING[sample_rate=67%; trace_present=0; metric_present=1] | T07-P53: T07-P53/CH08 + TRACE_METRIC_CROSSCHECK | T07-P53-incident-018 |
| T07-P53-I04 | T07-P53: 영향 범위 | T07-P53: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P53: DISCONNECT[disconnect_ms=90; commit_state=UNKNOWN; request=053-03] | T07-P53: T07-P53/CH08 + COMMIT_TIMELINE | T07-P53-incident-019 |
| T07-P53-I05 | T07-P53: 변경 동결 | T07-P53: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P53: RECURRENCE[occurrence=6; interval_s=144; mitigation_applied=1] | T07-P53: T07-P53/CH08 + RECURRENCE_TIMELINE | T07-P53-incident-020 |
| T07-P53-I06 | T07-P53: correlation | T07-P53: 한 request/job/resource id를 시간축에 고정 | T07-P53: LARGE_INPUT[body_kb=1512; limit_kb=640; parsed=0] | T07-P53: T07-P53/CH08 + SIZE_LIMIT | T07-P53-incident-021 |

## CHAPTER 21 · at-least-once와 중복 처리 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P53/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P53/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P53/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P53/CH04~T07-P53/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P53/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P53/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P53/CH18 signal 두 개와 T07-P53/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P53/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P53/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · at-least-once와 중복 처리 — 통합 casebook 16문제

T07-P53 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P53 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P53-C01 | T07-P53: 경계 A | T07-P53: OVERLOAD[rps=410; p99_ms=579; queue=21] | T07-P53: load=737, window=11s | T07-P53: review=상태 변경 | T07-P53: 경계 A 위반 여부를 판정 | T07-P53: QUEUE_PRESSURE + T07-P53/CH08 |
| T07-P53-C02 | T07-P53: 경계 B | T07-P53: SAMPLING[sample_rate=54%; trace_present=1; metric_present=1] | T07-P53: load=760, window=30s | T07-P53: review=retry | T07-P53: 경계 B 위반 여부를 판정 | T07-P53: TRACE_METRIC_CROSSCHECK + T07-P53/CH08 |
| T07-P53-C03 | T07-P53: 경계 C | T07-P53: DISCONNECT[disconnect_ms=77; commit_state=UNKNOWN; request=053-02] | T07-P53: load=783, window=49s | T07-P53: review=복구 | T07-P53: 경계 C 위반 여부를 판정 | T07-P53: COMMIT_TIMELINE + T07-P53/CH08 |
| T07-P53-C04 | T07-P53: 경계 D | T07-P53: RECURRENCE[occurrence=5; interval_s=133; mitigation_applied=1] | T07-P53: load=806, window=68s | T07-P53: review=동시성 | T07-P53: 경계 D 위반 여부를 판정 | T07-P53: RECURRENCE_TIMELINE + T07-P53/CH08 |
| T07-P53-C05 | T07-P53: 경계 A | T07-P53: LARGE_INPUT[body_kb=1424; limit_kb=640; parsed=0] | T07-P53: load=829, window=87s | T07-P53: review=민감정보 | T07-P53: 경계 A 위반 여부를 판정 | T07-P53: SIZE_LIMIT + T07-P53/CH08 |
| T07-P53-C06 | T07-P53: 경계 B | T07-P53: REORDER[in_seq=8,6,7; applied_version=6] | T07-P53: load=852, window=16s | T07-P53: review=중복 | T07-P53: 경계 B 위반 여부를 판정 | T07-P53: SEQUENCE_STATE + T07-P53/CH08 |
| T07-P53-C07 | T07-P53: 경계 C | T07-P53: DRAIN[ready=0; active=13; drain_deadline_s=11] | T07-P53: load=875, window=35s | T07-P53: review=권한 | T07-P53: 경계 C 위반 여부를 판정 | T07-P53: DRAIN_STATE + T07-P53/CH08 |
| T07-P53-C08 | T07-P53: 경계 D | T07-P53: RECOVERY_SCOPE[selected=167; expected=11; backup=1; dry_run=1] | T07-P53: load=898, window=54s | T07-P53: review=입력 경계 | T07-P53: 경계 D 위반 여부를 판정 | T07-P53: RECOVERY_AUDIT + T07-P53/CH08 |
| T07-P53-C09 | T07-P53: 경계 A | T07-P53: REPLAY[key=cmd-053-08; attempts=4; response_seen=0] | T07-P53: load=921, window=73s | T07-P53: review=timeout | T07-P53: 경계 A 위반 여부를 판정 | T07-P53: IDEMPOTENCY_RECORD + T07-P53/CH08 |
| T07-P53-C10 | T07-P53: 경계 B | T07-P53: CONCURRENT_WRITE[actors=2; base_version=9; writes=2; gap_ms=51] | T07-P53: load=944, window=92s | T07-P53: review=자원 | T07-P53: 경계 B 위반 여부를 판정 | T07-P53: STATE_VERSION + T07-P53/CH08 |
| T07-P53-C11 | T07-P53: 경계 C | T07-P53: UNKNOWN_OUTCOME[timeout_ms=280; provider_state=UNKNOWN; lookup_id=p05310] | T07-P53: load=967, window=21s | T07-P53: review=순서 | T07-P53: 경계 C 위반 여부를 판정 | T07-P53: PROVIDER_RESULT + T07-P53/CH08 |
| T07-P53-C12 | T07-P53: 경계 D | T07-P53: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P53: load=990, window=40s | T07-P53: review=관측 | T07-P53: 경계 D 위반 여부를 판정 | T07-P53: DURABLE_STATE + T07-P53/CH08 |
| T07-P53-C13 | T07-P53: 경계 A | T07-P53: OVERLOAD[rps=806; p99_ms=1110; queue=32] | T07-P53: load=113, window=59s | T07-P53: review=상태 변경 | T07-P53: 경계 A 위반 여부를 판정 | T07-P53: QUEUE_PRESSURE + T07-P53/CH08 |
| T07-P53-C14 | T07-P53: 경계 B | T07-P53: SAMPLING[sample_rate=16%; trace_present=1; metric_present=1] | T07-P53: load=136, window=78s | T07-P53: review=retry | T07-P53: 경계 B 위반 여부를 판정 | T07-P53: TRACE_METRIC_CROSSCHECK + T07-P53/CH08 |
| T07-P53-C15 | T07-P53: 경계 C | T07-P53: DISCONNECT[disconnect_ms=39; commit_state=UNKNOWN; request=053-14] | T07-P53: load=159, window=97s | T07-P53: review=복구 | T07-P53: 경계 C 위반 여부를 판정 | T07-P53: COMMIT_TIMELINE + T07-P53/CH08 |
| T07-P53-C16 | T07-P53: 경계 D | T07-P53: RECURRENCE[occurrence=2; interval_s=54; mitigation_applied=1] | T07-P53: load=182, window=26s | T07-P53: review=동시성 | T07-P53: 경계 D 위반 여부를 판정 | T07-P53: RECURRENCE_TIMELINE + T07-P53/CH08 |

채점은 결론보다 근거를 본다. T07-P53/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · at-least-once와 중복 처리 — evidence 판독 문제 14개

T07-P53 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P53 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P53-V01 | T07-P53: latency=393ms; queue=51; retry=4 | T07-P53: sampling | T07-P53: 축=sampling; 원인 확정은 보류 | T07-P53: TRACE_METRIC_CROSSCHECK + T07-P53/CH08 | T07-P53: 피할 오판=경계 누락 |
| T07-P53-V02 | T07-P53: latency=460ms; queue=62; retry=0 | T07-P53: client disconnect | T07-P53: 축=client disconnect; 원인 확정은 보류 | T07-P53: COMMIT_TIMELINE + T07-P53/CH08 | T07-P53: 피할 오판=증거 혼동 |
| T07-P53-V03 | T07-P53: latency=527ms; queue=73; retry=3 | T07-P53: 재발 | T07-P53: 축=재발; 원인 확정은 보류 | T07-P53: RECURRENCE_TIMELINE + T07-P53/CH08 | T07-P53: 피할 오판=재시도 오판 |
| T07-P53-V04 | T07-P53: latency=594ms; queue=4; retry=6 | T07-P53: 대형 입력 | T07-P53: 축=대형 입력; 원인 확정은 보류 | T07-P53: SIZE_LIMIT + T07-P53/CH08 | T07-P53: 피할 오판=동시성 무시 |
| T07-P53-V05 | T07-P53: latency=661ms; queue=15; retry=2 | T07-P53: 순서 역전 | T07-P53: 축=순서 역전; 원인 확정은 보류 | T07-P53: SEQUENCE_STATE + T07-P53/CH08 | T07-P53: 피할 오판=순서 가정 |
| T07-P53-V06 | T07-P53: latency=728ms; queue=26; retry=5 | T07-P53: drain | T07-P53: 축=drain; 원인 확정은 보류 | T07-P53: DRAIN_STATE + T07-P53/CH08 | T07-P53: 피할 오판=상태 수명 혼동 |
| T07-P53-V07 | T07-P53: latency=795ms; queue=37; retry=1 | T07-P53: 복구 범위 | T07-P53: 축=복구 범위; 원인 확정은 보류 | T07-P53: RECOVERY_AUDIT + T07-P53/CH08 | T07-P53: 피할 오판=운영 한계 누락 |
| T07-P53-V08 | T07-P53: latency=862ms; queue=48; retry=4 | T07-P53: 재전송 | T07-P53: 축=재전송; 원인 확정은 보류 | T07-P53: IDEMPOTENCY_RECORD + T07-P53/CH08 | T07-P53: 피할 오판=오류 합치기 |
| T07-P53-V09 | T07-P53: latency=929ms; queue=59; retry=0 | T07-P53: 동시 변경 | T07-P53: 축=동시 변경; 원인 확정은 보류 | T07-P53: STATE_VERSION + T07-P53/CH08 | T07-P53: 피할 오판=복구 과잉 |
| T07-P53-V10 | T07-P53: latency=996ms; queue=70; retry=3 | T07-P53: unknown outcome | T07-P53: 축=unknown outcome; 원인 확정은 보류 | T07-P53: PROVIDER_RESULT + T07-P53/CH08 | T07-P53: 피할 오판=잘못된 전제 |
| T07-P53-V11 | T07-P53: latency=1063ms; queue=1; retry=6 | T07-P53: 재시작 | T07-P53: 축=재시작; 원인 확정은 보류 | T07-P53: DURABLE_STATE + T07-P53/CH08 | T07-P53: 피할 오판=경계 누락 |
| T07-P53-V12 | T07-P53: latency=1130ms; queue=12; retry=2 | T07-P53: 과부하 | T07-P53: 축=과부하; 원인 확정은 보류 | T07-P53: QUEUE_PRESSURE + T07-P53/CH08 | T07-P53: 피할 오판=증거 혼동 |
| T07-P53-V13 | T07-P53: latency=1197ms; queue=23; retry=5 | T07-P53: sampling | T07-P53: 축=sampling; 원인 확정은 보류 | T07-P53: TRACE_METRIC_CROSSCHECK + T07-P53/CH08 | T07-P53: 피할 오판=재시도 오판 |
| T07-P53-V14 | T07-P53: latency=1264ms; queue=34; retry=1 | T07-P53: client disconnect | T07-P53: 축=client disconnect; 원인 확정은 보류 | T07-P53: COMMIT_TIMELINE + T07-P53/CH08 | T07-P53: 피할 오판=동시성 무시 |

## CHAPTER 24 · at-least-once와 중복 처리 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P53에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P53-D01 | T07-P53: cache 추가 | T07-P53: 현재 결과 앞에 cache layer를 추가한다 | T07-P53: stale·key·invalidation 책임이 새로 생기는지 | T07-P53: RECURRENCE[occurrence=2; interval_s=100; mitigation_applied=1] | T07-P53: RECURRENCE_TIMELINE + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P53-D02 | T07-P53: pool 확대 | T07-P53: connection/worker pool 상한을 늘린다 | T07-P53: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P53: LARGE_INPUT[body_kb=1160; limit_kb=640; parsed=0] | T07-P53: SIZE_LIMIT + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P53-D03 | T07-P53: schema 변경 | T07-P53: 필드 이름·형식·required 조건을 바꾼다 | T07-P53: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P53: REORDER[in_seq=5,3,4; applied_version=6] | T07-P53: SEQUENCE_STATE + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P53-D04 | T07-P53: 결과 합치기 | T07-P53: 여러 오류를 하나의 status/error code로 합친다 | T07-P53: client 행동과 retry 가능성을 잃지 않는지 | T07-P53: DRAIN[ready=0; active=3; drain_deadline_s=8] | T07-P53: DRAIN_STATE + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P53-D05 | T07-P53: retry 추가 | T07-P53: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P53: unknown outcome과 duplicate side effect를 구분하는지 | T07-P53: RECOVERY_SCOPE[selected=134; expected=12; backup=1; dry_run=0] | T07-P53: RECOVERY_AUDIT + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P53-D06 | T07-P53: 로그 확대 | T07-P53: debug를 위해 payload와 context 기록을 늘린다 | T07-P53: secret·PII·cardinality 비용을 통제하는지 | T07-P53: REPLAY[key=cmd-053-05; attempts=4; response_seen=0] | T07-P53: IDEMPOTENCY_RECORD + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P53-D07 | T07-P53: 강제 종료 | T07-P53: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P53: in-flight request와 background work의 결과를 잃는지 | T07-P53: CONCURRENT_WRITE[actors=2; base_version=9; writes=2; gap_ms=12] | T07-P53: STATE_VERSION + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P53-D08 | T07-P53: AI package 추가 | T07-P53: AI가 제안한 새 dependency를 도입한다 | T07-P53: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P53: UNKNOWN_OUTCOME[timeout_ms=247; provider_state=UNKNOWN; lookup_id=p05307] | T07-P53: PROVIDER_RESULT + T07-P53/CH08 | T07-P53: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P53: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · at-least-once와 중복 처리 — 최종 contract와 evidence spine

**최종 계약:** 많은 queue는 message 유실을 줄이기 위해 실패 시 다시 전달할 수 있어 consumer가 같은 message를 여러 번 볼 수 있다.

**정상 메커니즘:** event id와 처리 기록 또는 idempotent operation으로 중복 delivery가 중복 side effect가 되지 않게 한다.

**대표 실패:** delivery count=1을 가정해 이메일·결제·적립이 여러 번 실행된다.

**검증 evidence:** message id, delivery count, processing key, side-effect id를 연결한다.

**직접 행동:** 중복 message id를 제거하되 최초 순서를 유지하는 코드를 실행한다.

**다음 연결:** `retry·backoff·jitter·DLQ`.

`at-least-once와 중복 처리`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | at-least-once와 중복 처리의 개념·실패·운영 판단 교차 확인 |
| RFC9111 | HTTP Caching | at-least-once와 중복 처리의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | at-least-once와 중복 처리의 개념·실패·운영 판단 교차 확인 |
| SRE-WORKBOOK | SRE-WORKBOOK | at-least-once와 중복 처리의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | at-least-once와 중복 처리의 개념·실패·운영 판단 교차 확인 |

`at-least-once와 중복 처리` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
