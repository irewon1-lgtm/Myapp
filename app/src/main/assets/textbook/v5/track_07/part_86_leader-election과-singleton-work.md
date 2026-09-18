# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 06 · 한 서버를 넘어 여러 구성요소가 협력하는 구조 이해하기

### LESSON 11 · leader election과 singleton work

## CHAPTER 01 · leader election과 singleton work — 쉬운 말에서 정확한 계약까지

**출발 개념.** 한 번만 실행해야 하는 coordinator 역할은 process 수와 별개로 현재 leader를 결정해야 한다.

**아주 쉬운 사건.** network partition 뒤 leader가 둘처럼 보인다. 이 사건에서는 먼저 **epoch와 idempotency를 적용한다**.

**왜 필요한가.** 정상 동작은 lease/consensus 기반 leader 상태와 term을 사용하고 leader 변경을 정상 사건으로 다룬다. 반대로 hostname 하나를 고정 leader로 지정해 장애 시 작업이 멈추거나 split-brain을 무시한다.

**암기:** `leader election과 singleton work`의 역할 한 줄.

**직접 이해:** `leader election과 singleton work`의 입력·상태·결과 경계.

**AI 위임 가능:** `leader election과 singleton work` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `network partition 뒤 leader가 둘처럼 보인다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 term, leader id, election 시간, 중복 실행 탐지를 본다.

## CHAPTER 02 · leader election과 singleton work — 아주 쉬운 예를 한 단계씩 해석

T07-P86: `network partition 뒤 leader가 둘처럼 보인다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | network partition 뒤 leader가 둘처럼 보인다 | T07-P86 외부 입력 | T07-P86: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | epoch와 idempotency를 적용한다 | T07-P86 판단 기준 | T07-P86/CH08 관측표와 대조 |
| 정상 경로 | T07-P86/CH03 M1→M5 | leader election과 singleton work: 완료 시점을 단계별로 분리 | T07-P86: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P86/CH06 hostname 하나를 고정 leader로 지정해 장애 시 작업이 멈추거나 split-brain을 무시한다. | T07-P86: 깨진 계약 하나를 특정 | leader election과 singleton work: 증상과 원인을 분리 |
| 재검증 | T07-P86/CH10 직접 실행 | T07-P86: 예상값 T07-P086 a → p86 → b 기록 | leader election과 singleton work: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P86/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P86/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · leader election과 singleton work — 내부 메커니즘과 상태 전이

lease/consensus 기반 leader 상태와 term을 사용하고 leader 변경을 정상 사건으로 다룬다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | network partition 뒤 leader가 둘처럼 보인다 | source/actor/size를 보존 |
| M2 | 경계 판단 | epoch와 idempotency를 적용한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | lease/consensus 기반 leader 상태와 term을 사용하고 leader 변경을 정상 사건으로 다룬다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | term, leader id, election 시간, 중복 실행 탐지를 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | term이 가장 높은 유효 후보를 leader로 선택한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`leader election과 singleton work` 흐름을 framework 이름 없이 설명한다.

막히면 `network partition 뒤 leader가 둘처럼 보인다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · leader election과 singleton work — 실전 경계 A

**경계 A.** leader election은 현재 coordinator를 한 명 고르는 과정이고 term/epoch를 함께 사용하면 이전 leader의 delayed message를 구분하는 데 도움이 된다.

T07-P86/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P86에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P86/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P86/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P86): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P86/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P86-A1-360 | T07-P86 조건 | leader election은 현재 coordinator를 한 명 고르는 과정이고 term/epoch를 함께 사용하면 이전 leader의 delayed message를 구분하는 데 도움이 된다. |
| T07-P86-A2-361 | T07-P86 변화점 | T07-P86/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P86-A3-362 | T07-P86 반례 | T07-P86/CH06 대표 실패와 A 위반을 구별 |
| T07-P86-A4-363 | T07-P86 근거 | T07-P86/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P86-A5-364 | T07-P86 재실험 | network partition 뒤 leader가 둘처럼 보인다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · leader election과 singleton work — 실전 경계 B

**경계 B.** network partition에서 서로 leader라고 믿는 split-brain 가능성을 fencing/consensus storage로 막지 않으면 singleton job도 두 번 실행될 수 있다.

T07-P86/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P86에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P86/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P86/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P86): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P86/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P86-B1-391 | T07-P86 조건 | network partition에서 서로 leader라고 믿는 split-brain 가능성을 fencing/consensus storage로 막지 않으면 singleton job도 두 번 실행될 수 있다. |
| T07-P86-B2-392 | T07-P86 독립성 | T07-P86/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P86-B3-393 | T07-P86 상태 | T07-P86/CH03 before·after 위치를 다시 지정 |
| T07-P86-B4-394 | T07-P86 반증 | T07-P86/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P86-B5-395 | T07-P86 적용 | leader election과 singleton work의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · leader election과 singleton work — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **hostname 하나를 고정 leader로 지정해 장애 시 작업이 멈추거나 split-brain을 무시한다.**

아래 여섯 사례는 T07-P86의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P86-F01 | T07-P86: 대표 실패 | T07-P86: T07-P86/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P86: 현상만 보고 원인을 확정 | T07-P86/CH08 evidence map에서 상태를 대조 |
| T07-P86-F02 | T07-P86: 경계 A 누락 | T07-P86: T07-P86/CH04 경계 A 위반 입력 | T07-P86: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P86/CH08 evidence map에서 상태를 대조 |
| T07-P86-F03 | T07-P86: 경계 B 누락 | T07-P86: T07-P86/CH05 경계 B 위반 입력 | T07-P86: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P86/CH08 evidence map에서 상태를 대조 |
| T07-P86-F04 | T07-P86: 복구 경계 C 누락 | T07-P86: T07-P86/CH07 경계 C 복구 조건 | T07-P86: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P86/CH08 evidence map에서 상태를 대조 |
| T07-P86-F05 | T07-P86: 운영 경계 D 누락 | T07-P86: T07-P86/CH09 경계 D 운영 조건 | T07-P86: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P86/CH08 evidence map에서 상태를 대조 |
| T07-P86-F06 | T07-P86: 증거 없는 결론 | T07-P86: T07-P86/CH02 첫 판단만 존재 | T07-P86: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P86/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P86/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · leader election과 singleton work — 복구 가능한 상태와 수명

**경계 C.** leader가 한 명이어도 failover 순간 같은 job이 재시작될 수 있어 실제 side effect는 idempotent해야 ‘leader election=exactly once’ 오해를 피한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P86에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P86/CH08 evidence map을 본다. 복구 후에는 T07-P86/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P86에서 이미 확정된 side effect는 T07-P86/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P86-R1-453 | T07-P86 중단 직전 | T07-P86/CH03에서 이미 확정된 상태만 표시 |
| T07-P86-R2-454 | T07-P86 재시작 직후 | leader가 한 명이어도 failover 순간 같은 job이 재시작될 수 있어 실제 side effect는 idempotent해야 ‘leader election=exactly once’ 오해를 피한다. |
| T07-P86-R3-455 | T07-P86 재검증 | T07-P86/CH08 근거로 중복·누락 여부 확인 |
| T07-P86-R4-456 | T07-P86 재실행 | T07-P86/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · leader election과 singleton work — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | network partition 뒤 leader가 둘처럼 보인다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | epoch와 idempotency를 적용한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | lease/consensus 기반 leader 상태와 term을 사용하고 leader 변경을 정상 사건으로 다룬다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | hostname 하나를 고정 leader로 지정해 장애 시 작업이 멈추거나 split-brain을 무시한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | term, leader id, election 시간, 중복 실행 탐지를 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`leader election과 singleton work` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · leader election과 singleton work — 운영 한계와 종료 조건

**경계 D.** leader 역할과 일반 request processing을 같은 process에 묶을 때 leader change가 user traffic에 어떤 영향을 주는지 readiness와 metric으로 관측한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P86/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P86/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P86 과제: 경계 D와 T07-P86/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P86-O1-515 | synthetic-load=195 | T07-P86/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P86-O2-516 | synthetic-budget=665ms | T07-P86 timeout과 unknown outcome을 분리 |
| T07-P86-O3-517 | T07-P86 종료 | leader 역할과 일반 request processing을 같은 process에 묶을 때 leader change가 user traffic에 어떤 영향을 주는지 readiness와 metric으로 관측한다. |
| T07-P86-O4-518 | T07-P86 완화 | T07-P86/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · leader election과 singleton work — 직접 실행하는 작은 모델

`leader election과 singleton work` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P086 a|p86|b`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P086";
const ids = ["a", "p86", "a", "p86", "b"];
const unique = [...new Set(ids)];
console.log(marker, unique.join("|"));
```

기준 출력: `T07-P086 a|p86|b`.

`leader election과 singleton work`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P86-L1-547 | constmarker="T07-P086"; | T07-P86 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P86-L2-548 | constids=["a","p86","a","p86","b"]; | T07-P86 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P86-L3-549 | constunique=[...newSet(ids)]; | T07-P86 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P86-L4-550 | console.log(marker,unique.join("→")); | T07-P86 출력 관측점; 예상 `T07-P086 a→p86→b`와 비교 |
| T07-P86-LX-636 | T07-P86 실행 기록 | T07-P86 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · leader election과 singleton work — 한 부분만 수정하고 다시 예측

수정 과제: **마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다**.

수정 전은 `T07-P086 a|p86|b`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P86/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P86-D1-577 | 기준 `T07-P086 a→p86→b` | T07-P86 수정 전 실행을 먼저 재현 |
| T07-P86-D2-578 | 마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다 | T07-P86 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P86-D3-579 | T07-P86 새 예측 | T07-P86 실행 전에 출력·상태를 먼저 기록 |
| T07-P86-D4-580 | T07-P86 재실행 | T07-P86/CH10 실제값과 새 예측을 대조 |
| T07-P86-D5-581 | T07-P86 반례 | T07-P86/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P86-D6-582 | T07-P86 근거 | T07-P86/CH08 상태가 설명과 일치해야 완료 |
| T07-P86-D7-583 | T07-P86 이유 | T07-P86 변경 이유를 leader election과 singleton work 계약과 연결해 설명 |

## CHAPTER 12 · leader election과 singleton work — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: leader election과 singleton work | 한 번만 실행해야 하는 coordinator 역할은 process 수와 별개로 현재 leader를 결정해야 한다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P86/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | lease/consensus 기반 leader 상태와 term을 사용하고 leader 변경을 정상 사건으로 다룬다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | hostname 하나를 고정 leader로 지정해 장애 시 작업이 멈추거나 split-brain을 무시한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | term, leader id, election 시간, 중복 실행 탐지를 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P86/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P86/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P86/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P86/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `leader election과 singleton work` 실행 코드 수정 | 마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다 | `leader election과 singleton work` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P86/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P86/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · leader election과 singleton work — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 한 번만 실행해야 하는 coordinator 역할은 process 수와 별개로 현재 leader를 결정해야 한다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P86/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P86/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P86/CH10 실행용 boilerplate·test 후보 | T07-P86: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P086 a → p86 → b` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P86/CH02의 판단 기준과 T07-P86/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P86-AI1-639 | T07-P86 사람 결정 | T07-P86 업무 의미·허용 위험·완료 기준 소유 |
| T07-P86-AI2-640 | T07-P86 AI 초안 | T07-P86/CH10 boilerplate·test 후보까지만 위임 |
| T07-P86-AI3-641 | T07-P86 검증 | T07-P86/CH06 반례와 T07-P86/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · leader election과 singleton work — 경계 조합 실험 8개

T07-P86: T07-P86/CH04~T07-P86/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P86의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-K01 | T07-P86: 경계 A | T07-P86: 경계 B | T07-P86: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=23] / sample=74 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + STATE_VERSION |
| T07-P86-K02 | T07-P86: 경계 A | T07-P86: 경계 C | T07-P86: UNKNOWN_OUTCOME[timeout_ms=305; provider_state=UNKNOWN; lookup_id=p08602] / sample=81 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + PROVIDER_RESULT |
| T07-P86-K03 | T07-P86: 경계 A | T07-P86: 경계 D | T07-P86: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] / sample=88 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + DURABLE_STATE |
| T07-P86-K04 | T07-P86: 경계 B | T07-P86: 경계 C | T07-P86: OVERLOAD[rps=248; p99_ms=858; queue=16] / sample=95 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + QUEUE_PRESSURE |
| T07-P86-K05 | T07-P86: 경계 B | T07-P86: 경계 D | T07-P86: SAMPLING[sample_rate=85%; trace_present=1; metric_present=1] / sample=13 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P86-K06 | T07-P86: 경계 C | T07-P86: 경계 D | T07-P86: DISCONNECT[disconnect_ms=108; commit_state=UNKNOWN; request=086-06] / sample=20 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + COMMIT_TIMELINE |
| T07-P86-K07 | T07-P86: 경계 A | T07-P86: 경계 B+C | T07-P86: RECURRENCE[occurrence=4; interval_s=79; mitigation_applied=1] / sample=27 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + RECURRENCE_TIMELINE |
| T07-P86-K08 | T07-P86: 경계 B | T07-P86: 경계 C+D | T07-P86: OWNER_SPOOF[actor=A2; payload_owner=B3; auth_owner=A2] / sample=34 | T07-P86: 먼저 깨지는 경계를 판정 | T07-P86: T07-P86/CH08 + AUTHZ_POLICY |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P86/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · leader election과 singleton work — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P86와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-B10 | T07-P86: distributed lock·lease·fencing token | T07-P86: lease가 끝난 old worker가 계속 write한다 | T07-P86: fencing token으로 stale writer를 막는다 | T07-P86: T07-P86/CH08 증거와 형제 LESSON 증거를 분리 | T07-P86: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P86-B12 | T07-P86: replication·failover를 backend 관점에서 다루기 | T07-P86: primary failover 뒤 일부 read가 늦다 | T07-P86: replica lag와 RPO/RTO를 구분한다 | T07-P86: T07-P86/CH08 증거와 형제 LESSON 증거를 분리 | T07-P86: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P86-B09 | T07-P86: CQRS를 읽기/쓰기 요구 차이에서 이해하기 | T07-P86: write model과 검색 화면 요구가 크게 다르다 | T07-P86: projection lag와 CQRS 필요성을 판단한다 | T07-P86: T07-P86/CH08 증거와 형제 LESSON 증거를 분리 | T07-P86: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P86-B13 | T07-P86: multi-tenant isolation | T07-P86: tenant A가 connection을 대부분 사용한다 | T07-P86: isolation과 noisy neighbor를 본다 | T07-P86: T07-P86/CH08 증거와 형제 LESSON 증거를 분리 | T07-P86: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P86-B08 | T07-P86: event schema와 evolution | T07-P86: producer가 새 field와 enum을 보낸다 | T07-P86: schema와 semantic compatibility를 구분한다 | T07-P86: T07-P86/CH08 증거와 형제 LESSON 증거를 분리 | T07-P86: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · leader election과 singleton work — 선택형 실패 주입 6개

T07-P86: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P86 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-X01 | T07-P86: 재시작 | T07-P86: side effect 직후 process가 재시작됨; sample=689 | T07-P86: durable state와 memory state | T07-P86: T07-P86/CH02 판단과 별도 기록 | T07-P86: commit·outbox·job id·restart 전후 상태 |
| T07-P86-X02 | T07-P86: 과부하 | T07-P86: traffic 세 배, p99 급증; sample=706 | T07-P86: 기능 실패와 saturation | T07-P86: T07-P86/CH02 판단과 별도 기록 | T07-P86: queue age·pool wait·CPU/event-loop·quota |
| T07-P86-X03 | T07-P86: sampling | T07-P86: 일부 log가 sampling으로 빠짐; sample=723 | T07-P86: 기록 부재와 사건 부재 | T07-P86: T07-P86/CH02 판단과 별도 기록 | T07-P86: metric·trace·durable state 교차 근거 |
| T07-P86-X04 | T07-P86: client disconnect | T07-P86: 응답 전에 연결이 끊김; sample=740 | T07-P86: 연결 종료와 server effect | T07-P86: T07-P86/CH02 판단과 별도 기록 | T07-P86: commit 시각·worker/outbox·request lifecycle |
| T07-P86-X05 | T07-P86: 재발 | T07-P86: 같은 오류가 잠시 뒤 다시 발생; sample=757 | T07-P86: 완화와 원인 제거 | T07-P86: T07-P86/CH02 판단과 별도 기록 | T07-P86: 재발 timeline·변경점·resource state |
| T07-P86-X06 | T07-P86: 소유권 위조 | T07-P86: tenant/owner가 payload에 포함됨; sample=774 | T07-P86: 식별 정보와 권한 근거 | T07-P86: T07-P86/CH02 판단과 별도 기록 | T07-P86: authenticated context·resource owner·policy result |

## CHAPTER 17 · leader election과 singleton work — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P86에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-E01 | T07-P86: 대표 실패를 원인으로 착각 | T07-P86: T07-P86/CH06 실패 case를 다른 입력으로 재현 | T07-P86: 현상과 원인을 같은 것으로 봄 | T07-P86: T07-P86/CH06 대표 실패와 T07-P86/CH08 증거를 다시 대조 | T07-P86: T07-P86/CH08 |
| T07-P86-E02 | T07-P86: 경계 A 생략 | T07-P86: T07-P86/CH04의 조건 하나를 반대로 설정 | T07-P86: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P86: T07-P86/CH04를 새 입력에 적용 | T07-P86: T07-P86/CH08 |
| T07-P86-E03 | T07-P86: 경계 B 생략 | T07-P86: T07-P86/CH05의 조건 하나를 반대로 설정 | T07-P86: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P86: T07-P86/CH05를 새 입력에 적용 | T07-P86: T07-P86/CH08 |
| T07-P86-E04 | T07-P86: 복구 상태 혼동 | T07-P86: T07-P86/CH07에서 처리 중단을 주입 | T07-P86: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P86: T07-P86/CH07에서 수명 경계를 다시 표시 | T07-P86: T07-P86/CH08 |
| T07-P86-E05 | T07-P86: 운영 한계 누락 | T07-P86: T07-P86/CH09에서 부하 또는 drain 조건을 변경 | T07-P86: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P86: T07-P86/CH09의 종료 조건을 다시 작성 | T07-P86: T07-P86/CH08 |
| T07-P86-E06 | T07-P86: 증거 없는 성공 판정 | T07-P86: T07-P86/CH08에서 증거 하나를 숨김 | T07-P86: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P86: T07-P86/CH08에서 독립 증거 둘을 선택 | T07-P86: T07-P86/CH08 |

## CHAPTER 18 · leader election과 singleton work — synthetic 관측값 판독 6개

T07-P86: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P86의 숫자 하나만으로 원인을 단정하지 않고 T07-P86/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-O01 | T07-P86/rpc_latency_ms | 399 | T07-P86: service 호출 지연 | T07-P86: 축=재발; 원인 확정 금지 | T07-P86: RECURRENCE_TIMELINE + T07-P86/CH08 |
| T07-P86-O02 | T07-P86/message_lag | 416 | T07-P86: message lag | T07-P86: 축=소유권 위조; 원인 확정 금지 | T07-P86: AUTHZ_POLICY + T07-P86/CH08 |
| T07-P86-O03 | T07-P86/replica_lag_ms | 433 | T07-P86: replica lag | T07-P86: 축=순서 역전; 원인 확정 금지 | T07-P86: SEQUENCE_STATE + T07-P86/CH08 |
| T07-P86-O04 | T07-P86/leader_changes | 450 | T07-P86: leader 변경 수 | T07-P86: 축=drain; 원인 확정 금지 | T07-P86: DRAIN_STATE + T07-P86/CH08 |
| T07-P86-O05 | T07-P86/conflicts | 467 | T07-P86: 분산 상태 충돌 수 | T07-P86: 축=복구 범위; 원인 확정 금지 | T07-P86: RECOVERY_AUDIT + T07-P86/CH08 |
| T07-P86-O06 | T07-P86/failover_ms | 484 | T07-P86: failover 시간 | T07-P86: 축=재전송; 원인 확정 금지 | T07-P86: IDEMPOTENCY_RECORD + T07-P86/CH08 |

## CHAPTER 19 · leader election과 singleton work — 선택형 코드 리뷰 6질문

T07-P86: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P86에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-R01 | T07-P86: 관측 | T07-P86: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P86: OWNER_SPOOF[actor=A2; payload_owner=B0; auth_owner=A2] | T07-P86: T07-P86/CH04 | T07-P86: AUTHZ_POLICY |
| T07-P86-R02 | T07-P86: 복구 | T07-P86: 재시작 뒤에도 필요한 상태가 남는가 | T07-P86: REORDER[in_seq=4,2,3; applied_version=3] | T07-P86: T07-P86/CH05 | T07-P86: SEQUENCE_STATE |
| T07-P86-R03 | T07-P86: 중복 | T07-P86: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P86: DRAIN[ready=0; active=3; drain_deadline_s=7] | T07-P86: T07-P86/CH06 | T07-P86: DRAIN_STATE |
| T07-P86-R04 | T07-P86: timeout | T07-P86: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P86: RECOVERY_SCOPE[selected=236; expected=16; backup=1; dry_run=1] | T07-P86: T07-P86/CH07 | T07-P86: RECOVERY_AUDIT |
| T07-P86-R05 | T07-P86: 관측 | T07-P86: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P86: REPLAY[key=cmd-086-04; attempts=3; response_seen=0] | T07-P86: T07-P86/CH04 | T07-P86: IDEMPOTENCY_RECORD |
| T07-P86-R06 | T07-P86: 복구 | T07-P86: 재시작 뒤에도 필요한 상태가 남는가 | T07-P86: OLD_SCHEMA[client=v4; server=v5; extra_field=1] | T07-P86: T07-P86/CH05 | T07-P86: CLIENT_VERSION |

## CHAPTER 20 · leader election과 singleton work — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P86에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-I01 | T07-P86: 재주입 | T07-P86: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P86: REORDER[in_seq=3,1,2; applied_version=3] | T07-P86: T07-P86/CH08 + SEQUENCE_STATE | T07-P86-incident-643 |
| T07-P86-I02 | T07-P86: 회귀 고정 | T07-P86: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P86: DRAIN[ready=0; active=7; drain_deadline_s=6] | T07-P86: T07-P86/CH08 + DRAIN_STATE | T07-P86-incident-644 |
| T07-P86-I03 | T07-P86: 영향 범위 | T07-P86: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P86: RECOVERY_SCOPE[selected=225; expected=22; backup=1; dry_run=0] | T07-P86: T07-P86/CH08 + RECOVERY_AUDIT | T07-P86-incident-645 |
| T07-P86-I04 | T07-P86: 변경 동결 | T07-P86: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P86: REPLAY[key=cmd-086-03; attempts=2; response_seen=0] | T07-P86: T07-P86/CH08 + IDEMPOTENCY_RECORD | T07-P86-incident-646 |
| T07-P86-I05 | T07-P86: correlation | T07-P86: 한 request/job/resource id를 시간축에 고정 | T07-P86: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P86: T07-P86/CH08 + CLIENT_VERSION | T07-P86-incident-647 |
| T07-P86-I06 | T07-P86: 마지막 정상 | T07-P86: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P86: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=75] | T07-P86: T07-P86/CH08 + STATE_VERSION | T07-P86-incident-648 |

## CHAPTER 21 · leader election과 singleton work — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P86/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P86/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P86/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P86/CH04~T07-P86/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P86/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P86/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P86/CH18 signal 두 개와 T07-P86/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P86/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P86/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · leader election과 singleton work — 통합 casebook 16문제

T07-P86 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P86 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P86-C01 | T07-P86: 경계 A | T07-P86: DRAIN[ready=0; active=11; drain_deadline_s=5] | T07-P86: load=794, window=32s | T07-P86: review=자원 | T07-P86: 경계 A 위반 여부를 판정 | T07-P86: DRAIN_STATE + T07-P86/CH08 |
| T07-P86-C02 | T07-P86: 경계 B | T07-P86: RECOVERY_SCOPE[selected=214; expected=9; backup=1; dry_run=1] | T07-P86: load=817, window=51s | T07-P86: review=순서 | T07-P86: 경계 B 위반 여부를 판정 | T07-P86: RECOVERY_AUDIT + T07-P86/CH08 |
| T07-P86-C03 | T07-P86: 경계 C | T07-P86: REPLAY[key=cmd-086-02; attempts=4; response_seen=0] | T07-P86: load=840, window=70s | T07-P86: review=관측 | T07-P86: 경계 C 위반 여부를 판정 | T07-P86: IDEMPOTENCY_RECORD + T07-P86/CH08 |
| T07-P86-C04 | T07-P86: 경계 D | T07-P86: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P86: load=863, window=89s | T07-P86: review=상태 변경 | T07-P86: 경계 D 위반 여부를 판정 | T07-P86: CLIENT_VERSION + T07-P86/CH08 |
| T07-P86-C05 | T07-P86: 경계 A | T07-P86: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=62] | T07-P86: load=886, window=18s | T07-P86: review=retry | T07-P86: 경계 A 위반 여부를 판정 | T07-P86: STATE_VERSION + T07-P86/CH08 |
| T07-P86-C06 | T07-P86: 경계 B | T07-P86: UNKNOWN_OUTCOME[timeout_ms=127; provider_state=UNKNOWN; lookup_id=p08605] | T07-P86: load=909, window=37s | T07-P86: review=복구 | T07-P86: 경계 B 위반 여부를 판정 | T07-P86: PROVIDER_RESULT + T07-P86/CH08 |
| T07-P86-C07 | T07-P86: 경계 C | T07-P86: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P86: load=932, window=56s | T07-P86: review=동시성 | T07-P86: 경계 C 위반 여부를 판정 | T07-P86: DURABLE_STATE + T07-P86/CH08 |
| T07-P86-C08 | T07-P86: 경계 D | T07-P86: OVERLOAD[rps=347; p99_ms=336; queue=21] | T07-P86: load=955, window=75s | T07-P86: review=민감정보 | T07-P86: 경계 D 위반 여부를 판정 | T07-P86: QUEUE_PRESSURE + T07-P86/CH08 |
| T07-P86-C09 | T07-P86: 경계 A | T07-P86: SAMPLING[sample_rate=27%; trace_present=0; metric_present=1] | T07-P86: load=978, window=94s | T07-P86: review=중복 | T07-P86: 경계 A 위반 여부를 판정 | T07-P86: TRACE_METRIC_CROSSCHECK + T07-P86/CH08 |
| T07-P86-C10 | T07-P86: 경계 B | T07-P86: DISCONNECT[disconnect_ms=50; commit_state=UNKNOWN; request=086-09] | T07-P86: load=101, window=23s | T07-P86: review=권한 | T07-P86: 경계 B 위반 여부를 판정 | T07-P86: COMMIT_TIMELINE + T07-P86/CH08 |
| T07-P86-C11 | T07-P86: 경계 C | T07-P86: RECURRENCE[occurrence=2; interval_s=112; mitigation_applied=1] | T07-P86: load=124, window=42s | T07-P86: review=입력 경계 | T07-P86: 경계 C 위반 여부를 판정 | T07-P86: RECURRENCE_TIMELINE + T07-P86/CH08 |
| T07-P86-C12 | T07-P86: 경계 D | T07-P86: OWNER_SPOOF[actor=A2; payload_owner=B1; auth_owner=A2] | T07-P86: load=147, window=61s | T07-P86: review=timeout | T07-P86: 경계 D 위반 여부를 판정 | T07-P86: AUTHZ_POLICY + T07-P86/CH08 |
| T07-P86-C13 | T07-P86: 경계 A | T07-P86: REORDER[in_seq=15,13,14; applied_version=3] | T07-P86: load=170, window=80s | T07-P86: review=자원 | T07-P86: 경계 A 위반 여부를 판정 | T07-P86: SEQUENCE_STATE + T07-P86/CH08 |
| T07-P86-C14 | T07-P86: 경계 B | T07-P86: DRAIN[ready=0; active=15; drain_deadline_s=10] | T07-P86: load=193, window=99s | T07-P86: review=순서 | T07-P86: 경계 B 위반 여부를 판정 | T07-P86: DRAIN_STATE + T07-P86/CH08 |
| T07-P86-C15 | T07-P86: 경계 C | T07-P86: RECOVERY_SCOPE[selected=146; expected=5; backup=1; dry_run=0] | T07-P86: load=216, window=28s | T07-P86: review=관측 | T07-P86: 경계 C 위반 여부를 판정 | T07-P86: RECOVERY_AUDIT + T07-P86/CH08 |
| T07-P86-C16 | T07-P86: 경계 D | T07-P86: REPLAY[key=cmd-086-15; attempts=2; response_seen=0] | T07-P86: load=239, window=47s | T07-P86: review=상태 변경 | T07-P86: 경계 D 위반 여부를 판정 | T07-P86: IDEMPOTENCY_RECORD + T07-P86/CH08 |

채점은 결론보다 근거를 본다. T07-P86/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · leader election과 singleton work — evidence 판독 문제 14개

T07-P86 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P86 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P86-V01 | T07-P86: latency=1746ms; queue=42; retry=2 | T07-P86: 복구 범위 | T07-P86: 축=복구 범위; 원인 확정은 보류 | T07-P86: RECOVERY_AUDIT + T07-P86/CH08 | T07-P86: 피할 오판=잘못된 전제 |
| T07-P86-V02 | T07-P86: latency=1813ms; queue=53; retry=5 | T07-P86: 재전송 | T07-P86: 축=재전송; 원인 확정은 보류 | T07-P86: IDEMPOTENCY_RECORD + T07-P86/CH08 | T07-P86: 피할 오판=경계 누락 |
| T07-P86-V03 | T07-P86: latency=80ms; queue=64; retry=1 | T07-P86: 구버전 client | T07-P86: 축=구버전 client; 원인 확정은 보류 | T07-P86: CLIENT_VERSION + T07-P86/CH08 | T07-P86: 피할 오판=증거 혼동 |
| T07-P86-V04 | T07-P86: latency=147ms; queue=75; retry=4 | T07-P86: 동시 변경 | T07-P86: 축=동시 변경; 원인 확정은 보류 | T07-P86: STATE_VERSION + T07-P86/CH08 | T07-P86: 피할 오판=재시도 오판 |
| T07-P86-V05 | T07-P86: latency=214ms; queue=6; retry=0 | T07-P86: unknown outcome | T07-P86: 축=unknown outcome; 원인 확정은 보류 | T07-P86: PROVIDER_RESULT + T07-P86/CH08 | T07-P86: 피할 오판=소유권 혼동 |
| T07-P86-V06 | T07-P86: latency=281ms; queue=17; retry=3 | T07-P86: 재시작 | T07-P86: 축=재시작; 원인 확정은 보류 | T07-P86: DURABLE_STATE + T07-P86/CH08 | T07-P86: 피할 오판=동시성 무시 |
| T07-P86-V07 | T07-P86: latency=348ms; queue=28; retry=6 | T07-P86: 과부하 | T07-P86: 축=과부하; 원인 확정은 보류 | T07-P86: QUEUE_PRESSURE + T07-P86/CH08 | T07-P86: 피할 오판=순서 가정 |
| T07-P86-V08 | T07-P86: latency=415ms; queue=39; retry=2 | T07-P86: sampling | T07-P86: 축=sampling; 원인 확정은 보류 | T07-P86: TRACE_METRIC_CROSSCHECK + T07-P86/CH08 | T07-P86: 피할 오판=상태 수명 혼동 |
| T07-P86-V09 | T07-P86: latency=482ms; queue=50; retry=5 | T07-P86: client disconnect | T07-P86: 축=client disconnect; 원인 확정은 보류 | T07-P86: COMMIT_TIMELINE + T07-P86/CH08 | T07-P86: 피할 오판=운영 한계 누락 |
| T07-P86-V10 | T07-P86: latency=549ms; queue=61; retry=1 | T07-P86: 재발 | T07-P86: 축=재발; 원인 확정은 보류 | T07-P86: RECURRENCE_TIMELINE + T07-P86/CH08 | T07-P86: 피할 오판=오류 합치기 |
| T07-P86-V11 | T07-P86: latency=616ms; queue=72; retry=4 | T07-P86: 소유권 위조 | T07-P86: 축=소유권 위조; 원인 확정은 보류 | T07-P86: AUTHZ_POLICY + T07-P86/CH08 | T07-P86: 피할 오판=복구 과잉 |
| T07-P86-V12 | T07-P86: latency=683ms; queue=3; retry=0 | T07-P86: 순서 역전 | T07-P86: 축=순서 역전; 원인 확정은 보류 | T07-P86: SEQUENCE_STATE + T07-P86/CH08 | T07-P86: 피할 오판=잘못된 전제 |
| T07-P86-V13 | T07-P86: latency=750ms; queue=14; retry=3 | T07-P86: drain | T07-P86: 축=drain; 원인 확정은 보류 | T07-P86: DRAIN_STATE + T07-P86/CH08 | T07-P86: 피할 오판=경계 누락 |
| T07-P86-V14 | T07-P86: latency=817ms; queue=25; retry=6 | T07-P86: 복구 범위 | T07-P86: 축=복구 범위; 원인 확정은 보류 | T07-P86: RECOVERY_AUDIT + T07-P86/CH08 | T07-P86: 피할 오판=증거 혼동 |

## CHAPTER 24 · leader election과 singleton work — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P86에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P86-D01 | T07-P86: 권한 shortcut | T07-P86: payload의 owner/tenant id를 바로 사용한다 | T07-P86: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P86: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P86: CLIENT_VERSION + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P86-D02 | T07-P86: 순서 병렬화 | T07-P86: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P86: 선후관계 invariant와 race를 깨지 않는지 | T07-P86: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=23] | T07-P86: STATE_VERSION + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P86-D03 | T07-P86: validation 이동 | T07-P86: validation을 business side effect 뒤로 옮긴다 | T07-P86: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P86: UNKNOWN_OUTCOME[timeout_ms=305; provider_state=UNKNOWN; lookup_id=p08602] | T07-P86: PROVIDER_RESULT + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P86-D04 | T07-P86: batch 확대 | T07-P86: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P86: memory·deadline·부분 실패 범위가 커지는지 | T07-P86: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P86: DURABLE_STATE + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P86-D05 | T07-P86: fallback 추가 | T07-P86: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P86: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P86: OVERLOAD[rps=248; p99_ms=858; queue=16] | T07-P86: QUEUE_PRESSURE + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P86-D06 | T07-P86: 외부 호출 이동 | T07-P86: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P86: lock duration과 unknown outcome 경계가 달라지는지 | T07-P86: SAMPLING[sample_rate=85%; trace_present=1; metric_present=1] | T07-P86: TRACE_METRIC_CROSSCHECK + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P86-D07 | T07-P86: cache 추가 | T07-P86: 현재 결과 앞에 cache layer를 추가한다 | T07-P86: stale·key·invalidation 책임이 새로 생기는지 | T07-P86: DISCONNECT[disconnect_ms=108; commit_state=UNKNOWN; request=086-06] | T07-P86: COMMIT_TIMELINE + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P86-D08 | T07-P86: pool 확대 | T07-P86: connection/worker pool 상한을 늘린다 | T07-P86: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P86: RECURRENCE[occurrence=4; interval_s=79; mitigation_applied=1] | T07-P86: RECURRENCE_TIMELINE + T07-P86/CH08 | T07-P86: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P86: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · leader election과 singleton work — 최종 contract와 evidence spine

**최종 계약:** 한 번만 실행해야 하는 coordinator 역할은 process 수와 별개로 현재 leader를 결정해야 한다.

**정상 메커니즘:** lease/consensus 기반 leader 상태와 term을 사용하고 leader 변경을 정상 사건으로 다룬다.

**대표 실패:** hostname 하나를 고정 leader로 지정해 장애 시 작업이 멈추거나 split-brain을 무시한다.

**검증 evidence:** term, leader id, election 시간, 중복 실행 탐지를 본다.

**직접 행동:** term이 가장 높은 유효 후보를 leader로 선택한다.

**다음 연결:** `replication·failover를 backend 관점에서 다루기`.

`leader election과 singleton work`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | leader election과 singleton work의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | leader election과 singleton work의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | leader election과 singleton work의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | leader election과 singleton work의 개념·실패·운영 판단 교차 확인 |

`leader election과 singleton work` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
