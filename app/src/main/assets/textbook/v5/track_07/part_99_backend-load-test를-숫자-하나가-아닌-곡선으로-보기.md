# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 07 · AI와 함께 백엔드를 만들되 사람이 계약과 검증을 통제하기

### LESSON 09 · backend load test를 숫자 하나가 아닌 곡선으로 보기

## CHAPTER 01 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 한 번의 RPS 기록보다 concurrency를 올릴 때 throughput·latency·error·resource가 어떻게 변하는지가 중요하다.

**아주 쉬운 사건.** 100 RPS에서만 load test했다. 이 사건에서는 먼저 **concurrency sweep과 saturation knee를 찾는다**.

**왜 필요한가.** 정상 동작은 warm-up 후 단계적으로 load를 올리고 saturation point와 p95/p99를 찾으며 실제 traffic shape를 반영한다. 반대로 평균 latency와 최대 RPS만 보고 queue buildup이나 error 증가를 무시한다.

**암기:** `backend load test를 숫자 하나가 아닌 곡선으로 보기`의 역할 한 줄.

**직접 이해:** `backend load test를 숫자 하나가 아닌 곡선으로 보기`의 입력·상태·결과 경계.

**AI 위임 가능:** `backend load test를 숫자 하나가 아닌 곡선으로 보기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `100 RPS에서만 load test했다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 concurrency별 throughput/p99/error/saturation을 같은 표로 본다.

## CHAPTER 02 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 아주 쉬운 예를 한 단계씩 해석

T07-P99: `100 RPS에서만 load test했다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 100 RPS에서만 load test했다 | T07-P99 외부 입력 | T07-P99: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | concurrency sweep과 saturation knee를 찾는다 | T07-P99 판단 기준 | T07-P99/CH08 관측표와 대조 |
| 정상 경로 | T07-P99/CH03 M1→M5 | backend load test를 숫자 하나가 아닌 곡선으로 보기: 완료 시점을 단계별로 분리 | T07-P99: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P99/CH06 평균 latency와 최대 RPS만 보고 queue buildup이나 error 증가를 무시한다. | T07-P99: 깨진 계약 하나를 특정 | backend load test를 숫자 하나가 아닌 곡선으로 보기: 증상과 원인을 분리 |
| 재검증 | T07-P99/CH10 직접 실행 | T07-P99: 예상값 T07-P099 3,2,2,1 기록 | backend load test를 숫자 하나가 아닌 곡선으로 보기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P99/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P99/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 내부 메커니즘과 상태 전이

warm-up 후 단계적으로 load를 올리고 saturation point와 p95/p99를 찾으며 실제 traffic shape를 반영한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 100 RPS에서만 load test했다 | source/actor/size를 보존 |
| M2 | 경계 판단 | concurrency sweep과 saturation knee를 찾는다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | warm-up 후 단계적으로 load를 올리고 saturation point와 p95/p99를 찾으며 실제 traffic shape를 반영한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | concurrency별 throughput/p99/error/saturation을 같은 표로 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | load 단계 데이터에서 첫 SLO 위반 지점을 찾는다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`backend load test를 숫자 하나가 아닌 곡선으로 보기` 흐름을 framework 이름 없이 설명한다.

막히면 `100 RPS에서만 load test했다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 실전 경계 A

**경계 A.** load test는 100 RPS 한 점만 재지 않고 concurrency/RPS를 단계적으로 올려 throughput이 평탄해지고 p95/p99가 급격히 늘어나는 saturation knee를 찾는다.

T07-P99/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P99에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P99/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P99/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P99): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P99/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P99-A1-428 | T07-P99 조건 | load test는 100 RPS 한 점만 재지 않고 concurrency/RPS를 단계적으로 올려 throughput이 평탄해지고 p95/p99가 급격히 늘어나는 saturation knee를 찾는다. |
| T07-P99-A2-429 | T07-P99 변화점 | T07-P99/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P99-A3-430 | T07-P99 반례 | T07-P99/CH06 대표 실패와 A 위반을 구별 |
| T07-P99-A4-431 | T07-P99 근거 | T07-P99/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P99-A5-432 | T07-P99 재실험 | 100 RPS에서만 load test했다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 실전 경계 B

**경계 B.** warm-up·steady-state·cool-down을 구분해 JIT/cache 초기화나 connection setup 시간을 지속 성능과 섞지 않는다.

T07-P99/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P99에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P99/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P99/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P99): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P99/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P99-B1-459 | T07-P99 조건 | warm-up·steady-state·cool-down을 구분해 JIT/cache 초기화나 connection setup 시간을 지속 성능과 섞지 않는다. |
| T07-P99-B2-460 | T07-P99 독립성 | T07-P99/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P99-B3-461 | T07-P99 상태 | T07-P99/CH03 before·after 위치를 다시 지정 |
| T07-P99-B4-462 | T07-P99 반증 | T07-P99/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P99-B5-463 | T07-P99 적용 | backend load test를 숫자 하나가 아닌 곡선으로 보기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **평균 latency와 최대 RPS만 보고 queue buildup이나 error 증가를 무시한다.**

아래 여섯 사례는 T07-P99의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P99-F01 | T07-P99: 대표 실패 | T07-P99: T07-P99/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P99: 현상만 보고 원인을 확정 | T07-P99/CH08 evidence map에서 상태를 대조 |
| T07-P99-F02 | T07-P99: 경계 A 누락 | T07-P99: T07-P99/CH04 경계 A 위반 입력 | T07-P99: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P99/CH08 evidence map에서 상태를 대조 |
| T07-P99-F03 | T07-P99: 경계 B 누락 | T07-P99: T07-P99/CH05 경계 B 위반 입력 | T07-P99: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P99/CH08 evidence map에서 상태를 대조 |
| T07-P99-F04 | T07-P99: 복구 경계 C 누락 | T07-P99: T07-P99/CH07 경계 C 복구 조건 | T07-P99: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P99/CH08 evidence map에서 상태를 대조 |
| T07-P99-F05 | T07-P99: 운영 경계 D 누락 | T07-P99: T07-P99/CH09 경계 D 운영 조건 | T07-P99: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P99/CH08 evidence map에서 상태를 대조 |
| T07-P99-F06 | T07-P99: 증거 없는 결론 | T07-P99: T07-P99/CH02 첫 판단만 존재 | T07-P99: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P99/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P99/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 복구 가능한 상태와 수명

**경계 C.** test data cardinality와 dependency mock/real 여부를 기록해 cache hit 100%인 synthetic test를 production capacity처럼 해석하지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P99에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P99/CH08 evidence map을 본다. 복구 후에는 T07-P99/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P99에서 이미 확정된 side effect는 T07-P99/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P99-R1-521 | T07-P99 중단 직전 | T07-P99/CH03에서 이미 확정된 상태만 표시 |
| T07-P99-R2-522 | T07-P99 재시작 직후 | test data cardinality와 dependency mock/real 여부를 기록해 cache hit 100%인 synthetic test를 production capacity처럼 해석하지 않는다. |
| T07-P99-R3-523 | T07-P99 재검증 | T07-P99/CH08 근거로 중복·누락 여부 확인 |
| T07-P99-R4-524 | T07-P99 재실행 | T07-P99/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 100 RPS에서만 load test했다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | concurrency sweep과 saturation knee를 찾는다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | warm-up 후 단계적으로 load를 올리고 saturation point와 p95/p99를 찾으며 실제 traffic shape를 반영한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 평균 latency와 최대 RPS만 보고 queue buildup이나 error 증가를 무시한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | concurrency별 throughput/p99/error/saturation을 같은 표로 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`backend load test를 숫자 하나가 아닌 곡선으로 보기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 운영 한계와 종료 조건

**경계 D.** CPU·memory·pool·queue·downstream quota를 함께 기록해 latency 상승이 application code인지 외부 bottleneck인지 증거로 좁힌다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P99/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P99/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P99 과제: 경계 D와 T07-P99/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P99-O1-583 | synthetic-load=83 | T07-P99/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P99-O2-584 | synthetic-budget=733ms | T07-P99 timeout과 unknown outcome을 분리 |
| T07-P99-O3-585 | T07-P99 종료 | CPU·memory·pool·queue·downstream quota를 함께 기록해 latency 상승이 application code인지 외부 bottleneck인지 증거로 좁힌다. |
| T07-P99-O4-586 | T07-P99 완화 | T07-P99/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 직접 실행하는 작은 모델

`backend load test를 숫자 하나가 아닌 곡선으로 보기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P099 3,2,2,1`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P099";
const limit = 7;
const input = [3, 2, 2, 1];
const accepted = input.filter(value => value <= limit);
console.log(marker, accepted.join(","));
```

기준 출력: `T07-P099 3,2,2,1`.

`backend load test를 숫자 하나가 아닌 곡선으로 보기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P99-L1-615 | constmarker="T07-P099"; | T07-P99 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P99-L2-616 | constlimit=7; | T07-P99 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P99-L3-617 | constinput=[3,2,2,1]; | T07-P99 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P99-L4-618 | constaccepted=input.filter(value=>value<=limit); | T07-P99 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P99-L5-619 | console.log(marker,accepted.join(",")); | T07-P99 출력 관측점; 예상 `T07-P099 3,2,2,1`와 비교 |
| T07-P99-LX-704 | T07-P99 실행 기록 | T07-P99 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 한 부분만 수정하고 다시 예측

수정 과제: **limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다**.

수정 전은 `T07-P099 3,2,2,1`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P99/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P99-D1-645 | 기준 `T07-P099 3,2,2,1` | T07-P99 수정 전 실행을 먼저 재현 |
| T07-P99-D2-646 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | T07-P99 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P99-D3-647 | T07-P99 새 예측 | T07-P99 실행 전에 출력·상태를 먼저 기록 |
| T07-P99-D4-648 | T07-P99 재실행 | T07-P99/CH10 실제값과 새 예측을 대조 |
| T07-P99-D5-649 | T07-P99 반례 | T07-P99/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P99-D6-650 | T07-P99 근거 | T07-P99/CH08 상태가 설명과 일치해야 완료 |
| T07-P99-D7-651 | T07-P99 이유 | T07-P99 변경 이유를 backend load test를 숫자 하나가 아닌 곡선으로 보기 계약과 연결해 설명 |

## CHAPTER 12 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: backend load test를 숫자 하나가 아닌 곡선으로 보기 | 한 번의 RPS 기록보다 concurrency를 올릴 때 throughput·latency·error·resource가 어떻게 변하는지가 중요하다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P99/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | warm-up 후 단계적으로 load를 올리고 saturation point와 p95/p99를 찾으며 실제 traffic shape를 반영한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 평균 latency와 최대 RPS만 보고 queue buildup이나 error 증가를 무시한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | concurrency별 throughput/p99/error/saturation을 같은 표로 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P99/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P99/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P99/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P99/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `backend load test를 숫자 하나가 아닌 곡선으로 보기` 실행 코드 수정 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | `backend load test를 숫자 하나가 아닌 곡선으로 보기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P99/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P99/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 한 번의 RPS 기록보다 concurrency를 올릴 때 throughput·latency·error·resource가 어떻게 변하는지가 중요하다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P99/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P99/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P99/CH10 실행용 boilerplate·test 후보 | T07-P99: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P099 3,2,2,1` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P99/CH02의 판단 기준과 T07-P99/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P99-AI1-707 | T07-P99 사람 결정 | T07-P99 업무 의미·허용 위험·완료 기준 소유 |
| T07-P99-AI2-708 | T07-P99 AI 초안 | T07-P99/CH10 boilerplate·test 후보까지만 위임 |
| T07-P99-AI3-709 | T07-P99 검증 | T07-P99/CH06 반례와 T07-P99/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 경계 조합 실험 8개

T07-P99: T07-P99/CH04~T07-P99/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P99의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-K01 | T07-P99: 경계 A | T07-P99: 경계 B | T07-P99: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=39 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + CLIENT_VERSION |
| T07-P99-K02 | T07-P99: 경계 A | T07-P99: 경계 C | T07-P99: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=63] / sample=46 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + STATE_VERSION |
| T07-P99-K03 | T07-P99: 경계 A | T07-P99: 경계 D | T07-P99: UNKNOWN_OUTCOME[timeout_ms=271; provider_state=UNKNOWN; lookup_id=p09903] / sample=53 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + PROVIDER_RESULT |
| T07-P99-K04 | T07-P99: 경계 B | T07-P99: 경계 C | T07-P99: OVERLOAD[rps=746; p99_ms=1101; queue=23] / sample=60 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + QUEUE_PRESSURE |
| T07-P99-K05 | T07-P99: 경계 B | T07-P99: 경계 D | T07-P99: SAMPLING[sample_rate=15%; trace_present=1; metric_present=1] / sample=67 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P99-K06 | T07-P99: 경계 C | T07-P99: 경계 D | T07-P99: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] / sample=74 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + DIFF_EVAL_ROLLBACK |
| T07-P99-K07 | T07-P99: 경계 A | T07-P99: 경계 B+C | T07-P99: RECURRENCE[occurrence=4; interval_s=245; mitigation_applied=1] / sample=81 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + RECURRENCE_TIMELINE |
| T07-P99-K08 | T07-P99: 경계 B | T07-P99: 경계 C+D | T07-P99: LARGE_INPUT[body_kb=632; limit_kb=768; parsed=0] / sample=88 | T07-P99: 먼저 깨지는 경계를 판정 | T07-P99: T07-P99/CH08 + SIZE_LIMIT |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P99/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P99와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-B08 | T07-P99: OpenAPI를 실행 가능한 API 계약으로 쓰기 | T07-P99: OpenAPI와 실제 response가 다르다 | T07-P99: contract test로 drift를 찾는다 | T07-P99: T07-P99/CH08 증거와 형제 LESSON 증거를 분리 | T07-P99: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P99-B10 | T07-P99: observability readiness를 코드 리뷰에 포함하기 | T07-P99: dashboard는 있지만 request id가 없다 | T07-P99: instrumentation이 실제 연결되는지 확인한다 | T07-P99: T07-P99/CH08 증거와 형제 LESSON 증거를 분리 | T07-P99: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P99-B07 | T07-P99: eval과 test contract로 AI 변경을 반복 검증하기 | T07-P99: 변경 20개가 eval 95%를 통과했다 | T07-P99: failure type별 gate를 따로 본다 | T07-P99: T07-P99/CH08 증거와 형제 LESSON 증거를 분리 | T07-P99: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P99-B11 | T07-P99: cost와 resource budget을 backend 설계에 포함하기 | T07-P99: 외부 API 호출이 요청당 8회다 | T07-P99: resource unit과 cost budget을 계산한다 | T07-P99: T07-P99/CH08 증거와 형제 LESSON 증거를 분리 | T07-P99: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P99-B06 | T07-P99: 생성된 retry·async 코드를 검토 | T07-P99: AI가 모든 오류에 retry를 붙였다 | T07-P99: idempotency와 deadline을 먼저 검토한다 | T07-P99: T07-P99/CH08 증거와 형제 LESSON 증거를 분리 | T07-P99: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 선택형 실패 주입 6개

T07-P99: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P99 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-X01 | T07-P99: unknown outcome | T07-P99: dependency timeout 후 성공 여부 불명; sample=95 | T07-P99: 실패와 미확정 결과 | T07-P99: T07-P99/CH02 판단과 별도 기록 | T07-P99: provider id·조회 결과·retry history |
| T07-P99-X02 | T07-P99: 과부하 | T07-P99: traffic 세 배, p99 급증; sample=112 | T07-P99: 기능 실패와 saturation | T07-P99: T07-P99/CH02 판단과 별도 기록 | T07-P99: queue age·pool wait·CPU/event-loop·quota |
| T07-P99-X03 | T07-P99: sampling | T07-P99: 일부 log가 sampling으로 빠짐; sample=129 | T07-P99: 기록 부재와 사건 부재 | T07-P99: T07-P99/CH02 판단과 별도 기록 | T07-P99: metric·trace·durable state 교차 근거 |
| T07-P99-X04 | T07-P99: AI 복잡도 | T07-P99: AI가 cache/package를 추가함; sample=146 | T07-P99: 실제 이득과 failure surface | T07-P99: T07-P99/CH02 판단과 별도 기록 | T07-P99: diff·dependency tree·load/eval·rollback |
| T07-P99-X05 | T07-P99: 재발 | T07-P99: 같은 오류가 잠시 뒤 다시 발생; sample=163 | T07-P99: 완화와 원인 제거 | T07-P99: T07-P99/CH02 판단과 별도 기록 | T07-P99: 재발 timeline·변경점·resource state |
| T07-P99-X06 | T07-P99: 대형 입력 | T07-P99: 입력 크기가 정상의 100배; sample=180 | T07-P99: 의미 검증과 resource limit | T07-P99: T07-P99/CH02 판단과 별도 기록 | T07-P99: body/batch size·parse time·memory·reject status |

## CHAPTER 17 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P99에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-E01 | T07-P99: 대표 실패를 원인으로 착각 | T07-P99: T07-P99/CH06 실패 case를 다른 입력으로 재현 | T07-P99: 현상과 원인을 같은 것으로 봄 | T07-P99: T07-P99/CH06 대표 실패와 T07-P99/CH08 증거를 다시 대조 | T07-P99: T07-P99/CH08 |
| T07-P99-E02 | T07-P99: 경계 A 생략 | T07-P99: T07-P99/CH04의 조건 하나를 반대로 설정 | T07-P99: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P99: T07-P99/CH04를 새 입력에 적용 | T07-P99: T07-P99/CH08 |
| T07-P99-E03 | T07-P99: 경계 B 생략 | T07-P99: T07-P99/CH05의 조건 하나를 반대로 설정 | T07-P99: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P99: T07-P99/CH05를 새 입력에 적용 | T07-P99: T07-P99/CH08 |
| T07-P99-E04 | T07-P99: 복구 상태 혼동 | T07-P99: T07-P99/CH07에서 처리 중단을 주입 | T07-P99: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P99: T07-P99/CH07에서 수명 경계를 다시 표시 | T07-P99: T07-P99/CH08 |
| T07-P99-E05 | T07-P99: 운영 한계 누락 | T07-P99: T07-P99/CH09에서 부하 또는 drain 조건을 변경 | T07-P99: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P99: T07-P99/CH09의 종료 조건을 다시 작성 | T07-P99: T07-P99/CH08 |
| T07-P99-E06 | T07-P99: 증거 없는 성공 판정 | T07-P99: T07-P99/CH08에서 증거 하나를 숨김 | T07-P99: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P99: T07-P99/CH08에서 독립 증거 둘을 선택 | T07-P99: T07-P99/CH08 |

## CHAPTER 18 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — synthetic 관측값 판독 6개

T07-P99: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P99의 숫자 하나만으로 원인을 단정하지 않고 T07-P99/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-O01 | T07-P99/changed_files | 323 | T07-P99: 변경 파일 수 | T07-P99: 축=재발; 원인 확정 금지 | T07-P99: RECURRENCE_TIMELINE + T07-P99/CH08 |
| T07-P99-O02 | T07-P99/dependency_count | 340 | T07-P99: 추가 dependency 수 | T07-P99: 축=대형 입력; 원인 확정 금지 | T07-P99: SIZE_LIMIT + T07-P99/CH08 |
| T07-P99-O03 | T07-P99/test_failures | 357 | T07-P99: 실패 test 수 | T07-P99: 축=소유권 위조; 원인 확정 금지 | T07-P99: AUTHZ_POLICY + T07-P99/CH08 |
| T07-P99-O04 | T07-P99/eval_passes | 374 | T07-P99: 통과 eval 수 | T07-P99: 축=순서 역전; 원인 확정 금지 | T07-P99: SEQUENCE_STATE + T07-P99/CH08 |
| T07-P99-O05 | T07-P99/p99_ms | 391 | T07-P99: 변경 후 p99 | T07-P99: 축=복구 범위; 원인 확정 금지 | T07-P99: RECOVERY_AUDIT + T07-P99/CH08 |
| T07-P99-O06 | T07-P99/cost_units | 408 | T07-P99: resource/cost 단위 | T07-P99: 축=재전송; 원인 확정 금지 | T07-P99: IDEMPOTENCY_RECORD + T07-P99/CH08 |

## CHAPTER 19 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 선택형 코드 리뷰 6질문

T07-P99: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P99에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-R01 | T07-P99: 입력 경계 | T07-P99: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P99: LARGE_INPUT[body_kb=1616; limit_kb=768; parsed=0] | T07-P99: T07-P99/CH04 | T07-P99: SIZE_LIMIT |
| T07-P99-R02 | T07-P99: 순서 | T07-P99: old/new event 순서가 바뀌어도 안전한가 | T07-P99: OWNER_SPOOF[actor=A1; payload_owner=B1; auth_owner=A1] | T07-P99: T07-P99/CH05 | T07-P99: AUTHZ_POLICY |
| T07-P99-R03 | T07-P99: retry | T07-P99: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P99: REORDER[in_seq=5,3,4; applied_version=4] | T07-P99: T07-P99/CH06 | T07-P99: SEQUENCE_STATE |
| T07-P99-R04 | T07-P99: 민감정보 | T07-P99: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P99: RECOVERY_SCOPE[selected=191; expected=5; backup=1; dry_run=1] | T07-P99: T07-P99/CH07 | T07-P99: RECOVERY_AUDIT |
| T07-P99-R05 | T07-P99: 입력 경계 | T07-P99: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P99: REPLAY[key=cmd-099-04; attempts=3; response_seen=0] | T07-P99: T07-P99/CH04 | T07-P99: IDEMPOTENCY_RECORD |
| T07-P99-R06 | T07-P99: 순서 | T07-P99: old/new event 순서가 바뀌어도 안전한가 | T07-P99: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P99: T07-P99/CH05 | T07-P99: CLIENT_VERSION |

## CHAPTER 20 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P99에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-I01 | T07-P99: 마지막 정상 | T07-P99: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P99: OWNER_SPOOF[actor=A1; payload_owner=B0; auth_owner=A1] | T07-P99: T07-P99/CH08 + AUTHZ_POLICY | T07-P99-incident-890 |
| T07-P99-I02 | T07-P99: 가설 검증 | T07-P99: 원인 후보 하나만 뒤집어 재현 | T07-P99: REORDER[in_seq=4,2,3; applied_version=4] | T07-P99: T07-P99/CH08 + SEQUENCE_STATE | T07-P99-incident-891 |
| T07-P99-I03 | T07-P99: 복구 확인 | T07-P99: durable state와 사용자 결과를 모두 확인 | T07-P99: RECOVERY_SCOPE[selected=180; expected=11; backup=1; dry_run=0] | T07-P99: T07-P99/CH08 + RECOVERY_AUDIT | T07-P99-incident-892 |
| T07-P99-I04 | T07-P99: 재주입 | T07-P99: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P99: REPLAY[key=cmd-099-03; attempts=2; response_seen=0] | T07-P99: T07-P99/CH08 + IDEMPOTENCY_RECORD | T07-P99-incident-893 |
| T07-P99-I05 | T07-P99: 회귀 고정 | T07-P99: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P99: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P99: T07-P99/CH08 + CLIENT_VERSION | T07-P99-incident-894 |
| T07-P99-I06 | T07-P99: 영향 범위 | T07-P99: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P99: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=5] | T07-P99: T07-P99/CH08 + STATE_VERSION | T07-P99-incident-895 |

## CHAPTER 21 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P99/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P99/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P99/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P99/CH04~T07-P99/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P99/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P99/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P99/CH18 signal 두 개와 T07-P99/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P99/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P99/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 통합 casebook 16문제

T07-P99 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P99 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P99-C01 | T07-P99: 경계 A | T07-P99: REORDER[in_seq=3,1,2; applied_version=4] | T07-P99: load=271, window=73s | T07-P99: review=순서 | T07-P99: 경계 A 위반 여부를 판정 | T07-P99: SEQUENCE_STATE + T07-P99/CH08 |
| T07-P99-C02 | T07-P99: 경계 B | T07-P99: RECOVERY_SCOPE[selected=169; expected=17; backup=1; dry_run=1] | T07-P99: load=294, window=92s | T07-P99: review=관측 | T07-P99: 경계 B 위반 여부를 판정 | T07-P99: RECOVERY_AUDIT + T07-P99/CH08 |
| T07-P99-C03 | T07-P99: 경계 C | T07-P99: REPLAY[key=cmd-099-02; attempts=4; response_seen=0] | T07-P99: load=317, window=21s | T07-P99: review=상태 변경 | T07-P99: 경계 C 위반 여부를 판정 | T07-P99: IDEMPOTENCY_RECORD + T07-P99/CH08 |
| T07-P99-C04 | T07-P99: 경계 D | T07-P99: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P99: load=340, window=40s | T07-P99: review=retry | T07-P99: 경계 D 위반 여부를 판정 | T07-P99: CLIENT_VERSION + T07-P99/CH08 |
| T07-P99-C05 | T07-P99: 경계 A | T07-P99: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=89] | T07-P99: load=363, window=59s | T07-P99: review=복구 | T07-P99: 경계 A 위반 여부를 판정 | T07-P99: STATE_VERSION + T07-P99/CH08 |
| T07-P99-C06 | T07-P99: 경계 B | T07-P99: UNKNOWN_OUTCOME[timeout_ms=293; provider_state=UNKNOWN; lookup_id=p09905] | T07-P99: load=386, window=78s | T07-P99: review=동시성 | T07-P99: 경계 B 위반 여부를 판정 | T07-P99: PROVIDER_RESULT + T07-P99/CH08 |
| T07-P99-C07 | T07-P99: 경계 C | T07-P99: OVERLOAD[rps=812; p99_ms=462; queue=34] | T07-P99: load=409, window=97s | T07-P99: review=민감정보 | T07-P99: 경계 C 위반 여부를 판정 | T07-P99: QUEUE_PRESSURE + T07-P99/CH08 |
| T07-P99-C08 | T07-P99: 경계 D | T07-P99: SAMPLING[sample_rate=41%; trace_present=1; metric_present=1] | T07-P99: load=432, window=26s | T07-P99: review=중복 | T07-P99: 경계 D 위반 여부를 판정 | T07-P99: TRACE_METRIC_CROSSCHECK + T07-P99/CH08 |
| T07-P99-C09 | T07-P99: 경계 A | T07-P99: AI_COMPLEXITY[deps_added=1; cache_layer=1; rollback_plan=0] | T07-P99: load=455, window=45s | T07-P99: review=권한 | T07-P99: 경계 A 위반 여부를 판정 | T07-P99: DIFF_EVAL_ROLLBACK + T07-P99/CH08 |
| T07-P99-C10 | T07-P99: 경계 B | T07-P99: RECURRENCE[occurrence=6; interval_s=56; mitigation_applied=1] | T07-P99: load=478, window=64s | T07-P99: review=입력 경계 | T07-P99: 경계 B 위반 여부를 판정 | T07-P99: RECURRENCE_TIMELINE + T07-P99/CH08 |
| T07-P99-C11 | T07-P99: 경계 C | T07-P99: LARGE_INPUT[body_kb=808; limit_kb=768; parsed=0] | T07-P99: load=501, window=83s | T07-P99: review=timeout | T07-P99: 경계 C 위반 여부를 판정 | T07-P99: SIZE_LIMIT + T07-P99/CH08 |
| T07-P99-C12 | T07-P99: 경계 D | T07-P99: OWNER_SPOOF[actor=A1; payload_owner=B1; auth_owner=A1] | T07-P99: load=524, window=12s | T07-P99: review=자원 | T07-P99: 경계 D 위반 여부를 판정 | T07-P99: AUTHZ_POLICY + T07-P99/CH08 |
| T07-P99-C13 | T07-P99: 경계 A | T07-P99: REORDER[in_seq=15,13,14; applied_version=4] | T07-P99: load=547, window=31s | T07-P99: review=순서 | T07-P99: 경계 A 위반 여부를 판정 | T07-P99: SEQUENCE_STATE + T07-P99/CH08 |
| T07-P99-C14 | T07-P99: 경계 B | T07-P99: RECOVERY_SCOPE[selected=90; expected=17; backup=1; dry_run=1] | T07-P99: load=570, window=50s | T07-P99: review=관측 | T07-P99: 경계 B 위반 여부를 판정 | T07-P99: RECOVERY_AUDIT + T07-P99/CH08 |
| T07-P99-C15 | T07-P99: 경계 C | T07-P99: REPLAY[key=cmd-099-14; attempts=4; response_seen=0] | T07-P99: load=593, window=69s | T07-P99: review=상태 변경 | T07-P99: 경계 C 위반 여부를 판정 | T07-P99: IDEMPOTENCY_RECORD + T07-P99/CH08 |
| T07-P99-C16 | T07-P99: 경계 D | T07-P99: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P99: load=616, window=88s | T07-P99: review=retry | T07-P99: 경계 D 위반 여부를 판정 | T07-P99: CLIENT_VERSION + T07-P99/CH08 |

채점은 결론보다 근거를 본다. T07-P99/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — evidence 판독 문제 14개

T07-P99 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P99 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P99-V01 | T07-P99: latency=479ms; queue=53; retry=1 | T07-P99: 복구 범위 | T07-P99: 축=복구 범위; 원인 확정은 보류 | T07-P99: RECOVERY_AUDIT + T07-P99/CH08 | T07-P99: 피할 오판=재시도 오판 |
| T07-P99-V02 | T07-P99: latency=546ms; queue=64; retry=4 | T07-P99: 재전송 | T07-P99: 축=재전송; 원인 확정은 보류 | T07-P99: IDEMPOTENCY_RECORD + T07-P99/CH08 | T07-P99: 피할 오판=소유권 혼동 |
| T07-P99-V03 | T07-P99: latency=613ms; queue=75; retry=0 | T07-P99: 구버전 client | T07-P99: 축=구버전 client; 원인 확정은 보류 | T07-P99: CLIENT_VERSION + T07-P99/CH08 | T07-P99: 피할 오판=동시성 무시 |
| T07-P99-V04 | T07-P99: latency=680ms; queue=6; retry=3 | T07-P99: 동시 변경 | T07-P99: 축=동시 변경; 원인 확정은 보류 | T07-P99: STATE_VERSION + T07-P99/CH08 | T07-P99: 피할 오판=운영 한계 누락 |
| T07-P99-V05 | T07-P99: latency=747ms; queue=17; retry=6 | T07-P99: unknown outcome | T07-P99: 축=unknown outcome; 원인 확정은 보류 | T07-P99: PROVIDER_RESULT + T07-P99/CH08 | T07-P99: 피할 오판=오류 합치기 |
| T07-P99-V06 | T07-P99: latency=814ms; queue=28; retry=2 | T07-P99: 과부하 | T07-P99: 축=과부하; 원인 확정은 보류 | T07-P99: QUEUE_PRESSURE + T07-P99/CH08 | T07-P99: 피할 오판=AI 과신 |
| T07-P99-V07 | T07-P99: latency=881ms; queue=39; retry=5 | T07-P99: sampling | T07-P99: 축=sampling; 원인 확정은 보류 | T07-P99: TRACE_METRIC_CROSSCHECK + T07-P99/CH08 | T07-P99: 피할 오판=복구 과잉 |
| T07-P99-V08 | T07-P99: latency=948ms; queue=50; retry=1 | T07-P99: AI 복잡도 | T07-P99: 축=AI 복잡도; 원인 확정은 보류 | T07-P99: DIFF_EVAL_ROLLBACK + T07-P99/CH08 | T07-P99: 피할 오판=잘못된 전제 |
| T07-P99-V09 | T07-P99: latency=1015ms; queue=61; retry=4 | T07-P99: 재발 | T07-P99: 축=재발; 원인 확정은 보류 | T07-P99: RECURRENCE_TIMELINE + T07-P99/CH08 | T07-P99: 피할 오판=경계 누락 |
| T07-P99-V10 | T07-P99: latency=1082ms; queue=72; retry=0 | T07-P99: 대형 입력 | T07-P99: 축=대형 입력; 원인 확정은 보류 | T07-P99: SIZE_LIMIT + T07-P99/CH08 | T07-P99: 피할 오판=증거 혼동 |
| T07-P99-V11 | T07-P99: latency=1149ms; queue=3; retry=3 | T07-P99: 소유권 위조 | T07-P99: 축=소유권 위조; 원인 확정은 보류 | T07-P99: AUTHZ_POLICY + T07-P99/CH08 | T07-P99: 피할 오판=재시도 오판 |
| T07-P99-V12 | T07-P99: latency=1216ms; queue=14; retry=6 | T07-P99: 순서 역전 | T07-P99: 축=순서 역전; 원인 확정은 보류 | T07-P99: SEQUENCE_STATE + T07-P99/CH08 | T07-P99: 피할 오판=소유권 혼동 |
| T07-P99-V13 | T07-P99: latency=1283ms; queue=25; retry=2 | T07-P99: 복구 범위 | T07-P99: 축=복구 범위; 원인 확정은 보류 | T07-P99: RECOVERY_AUDIT + T07-P99/CH08 | T07-P99: 피할 오판=동시성 무시 |
| T07-P99-V14 | T07-P99: latency=1350ms; queue=36; retry=5 | T07-P99: 재전송 | T07-P99: 축=재전송; 원인 확정은 보류 | T07-P99: IDEMPOTENCY_RECORD + T07-P99/CH08 | T07-P99: 피할 오판=운영 한계 누락 |

## CHAPTER 24 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P99에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P99-D01 | T07-P99: 비동기화 | T07-P99: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P99: durability·status API·worker retry 계약이 생기는지 | T07-P99: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P99: CLIENT_VERSION + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P99-D02 | T07-P99: 권한 shortcut | T07-P99: payload의 owner/tenant id를 바로 사용한다 | T07-P99: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P99: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=50] | T07-P99: STATE_VERSION + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P99-D03 | T07-P99: 순서 병렬화 | T07-P99: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P99: 선후관계 invariant와 race를 깨지 않는지 | T07-P99: UNKNOWN_OUTCOME[timeout_ms=260; provider_state=UNKNOWN; lookup_id=p09902] | T07-P99: PROVIDER_RESULT + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P99-D04 | T07-P99: validation 이동 | T07-P99: validation을 business side effect 뒤로 옮긴다 | T07-P99: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P99: OVERLOAD[rps=713; p99_ms=984; queue=29] | T07-P99: QUEUE_PRESSURE + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P99-D05 | T07-P99: batch 확대 | T07-P99: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P99: memory·deadline·부분 실패 범위가 커지는지 | T07-P99: SAMPLING[sample_rate=19%; trace_present=0; metric_present=1] | T07-P99: TRACE_METRIC_CROSSCHECK + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P99-D06 | T07-P99: fallback 추가 | T07-P99: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P99: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P99: AI_COMPLEXITY[deps_added=2; cache_layer=1; rollback_plan=1] | T07-P99: DIFF_EVAL_ROLLBACK + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P99-D07 | T07-P99: 외부 호출 이동 | T07-P99: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P99: lock duration과 unknown outcome 경계가 달라지는지 | T07-P99: RECURRENCE[occurrence=3; interval_s=234; mitigation_applied=1] | T07-P99: RECURRENCE_TIMELINE + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P99-D08 | T07-P99: cache 추가 | T07-P99: 현재 결과 앞에 cache layer를 추가한다 | T07-P99: stale·key·invalidation 책임이 새로 생기는지 | T07-P99: LARGE_INPUT[body_kb=2232; limit_kb=768; parsed=0] | T07-P99: SIZE_LIMIT + T07-P99/CH08 | T07-P99: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P99: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · backend load test를 숫자 하나가 아닌 곡선으로 보기 — 최종 contract와 evidence spine

**최종 계약:** 한 번의 RPS 기록보다 concurrency를 올릴 때 throughput·latency·error·resource가 어떻게 변하는지가 중요하다.

**정상 메커니즘:** warm-up 후 단계적으로 load를 올리고 saturation point와 p95/p99를 찾으며 실제 traffic shape를 반영한다.

**대표 실패:** 평균 latency와 최대 RPS만 보고 queue buildup이나 error 증가를 무시한다.

**검증 evidence:** concurrency별 throughput/p99/error/saturation을 같은 표로 본다.

**직접 행동:** load 단계 데이터에서 첫 SLO 위반 지점을 찾는다.

**다음 연결:** `observability readiness를 코드 리뷰에 포함하기`.

`backend load test를 숫자 하나가 아닌 곡선으로 보기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | backend load test를 숫자 하나가 아닌 곡선으로 보기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | backend load test를 숫자 하나가 아닌 곡선으로 보기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | backend load test를 숫자 하나가 아닌 곡선으로 보기의 개념·실패·운영 판단 교차 확인 |
| OWASP-API | OWASP-API | backend load test를 숫자 하나가 아닌 곡선으로 보기의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | backend load test를 숫자 하나가 아닌 곡선으로 보기의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | backend load test를 숫자 하나가 아닌 곡선으로 보기의 개념·실패·운영 판단 교차 확인 |

`backend load test를 숫자 하나가 아닌 곡선으로 보기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
