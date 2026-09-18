# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 04 · cache·queue·background work로 느린 일을 분리하기

### LESSON 13 · 비동기 작업 상태 API 설계

## CHAPTER 01 · 비동기 작업 상태 API 설계 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 오래 걸리는 작업은 POST 하나에서 끝날 때까지 기다리기보다 job id와 상태 조회 계약을 제공할 수 있다.

**아주 쉬운 사건.** job은 RUNNING인데 HTTP 요청은 끝났다. 이 사건에서는 먼저 **202와 status resource를 설계한다**.

**왜 필요한가.** 정상 동작은 생성 요청은 job resource를 만들고 client는 status/result/error를 조회하거나 callback을 받는다. 반대로 202 응답 후 job이 실제로 저장되지 않거나 status가 모호해 영원히 RUNNING으로 남는다.

**암기:** `비동기 작업 상태 API 설계`의 역할 한 줄.

**직접 이해:** `비동기 작업 상태 API 설계`의 입력·상태·결과 경계.

**AI 위임 가능:** `비동기 작업 상태 API 설계` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `job은 RUNNING인데 HTTP 요청은 끝났다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 job state transition, heartbeat/progress, terminal reason, retention을 본다.

## CHAPTER 02 · 비동기 작업 상태 API 설계 — 아주 쉬운 예를 한 단계씩 해석

T07-P58: `job은 RUNNING인데 HTTP 요청은 끝났다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | job은 RUNNING인데 HTTP 요청은 끝났다 | T07-P58 외부 입력 | T07-P58: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | 202와 status resource를 설계한다 | T07-P58 판단 기준 | T07-P58/CH08 관측표와 대조 |
| 정상 경로 | T07-P58/CH03 M1→M5 | 비동기 작업 상태 API 설계: 완료 시점을 단계별로 분리 | T07-P58: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P58/CH06 202 응답 후 job이 실제로 저장되지 않거나 status가 모호해 영원히 RUNNING으로 남는다. | T07-P58: 깨진 계약 하나를 특정 | 비동기 작업 상태 API 설계: 증상과 원인을 분리 |
| 재검증 | T07-P58/CH10 직접 실행 | T07-P58: 예상값 T07-P058 0 기록 | 비동기 작업 상태 API 설계: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P58/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P58/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · 비동기 작업 상태 API 설계 — 내부 메커니즘과 상태 전이

생성 요청은 job resource를 만들고 client는 status/result/error를 조회하거나 callback을 받는다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | job은 RUNNING인데 HTTP 요청은 끝났다 | source/actor/size를 보존 |
| M2 | 경계 판단 | 202와 status resource를 설계한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | 생성 요청은 job resource를 만들고 client는 status/result/error를 조회하거나 callback을 받는다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | job state transition, heartbeat/progress, terminal reason, retention을 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | job event 목록으로 최종 상태와 progress를 계산한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`비동기 작업 상태 API 설계` 흐름을 framework 이름 없이 설명한다.

막히면 `job은 RUNNING인데 HTTP 요청은 끝났다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · 비동기 작업 상태 API 설계 — 실전 경계 A

**경계 A.** 오래 걸리는 작업은 POST가 202 Accepted와 job id를 반환하고 GET /jobs/{id} 같은 상태 resource로 completion을 조회하는 계약을 만들 수 있다.

T07-P58/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P58에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P58/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P58/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P58): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P58/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P58-A1-44 | T07-P58 조건 | 오래 걸리는 작업은 POST가 202 Accepted와 job id를 반환하고 GET /jobs/{id} 같은 상태 resource로 completion을 조회하는 계약을 만들 수 있다. |
| T07-P58-A2-45 | T07-P58 변화점 | T07-P58/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P58-A3-46 | T07-P58 반례 | T07-P58/CH06 대표 실패와 A 위반을 구별 |
| T07-P58-A4-47 | T07-P58 근거 | T07-P58/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P58-A5-48 | T07-P58 재실험 | job은 RUNNING인데 HTTP 요청은 끝났다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · 비동기 작업 상태 API 설계 — 실전 경계 B

**경계 B.** QUEUED·RUNNING·SUCCEEDED·FAILED·CANCELLED처럼 상태를 명시하면 client가 HTTP connection 하나를 오래 붙잡지 않고도 진행을 이해할 수 있다.

T07-P58/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P58에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P58/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P58/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P58): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P58/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P58-B1-75 | T07-P58 조건 | QUEUED·RUNNING·SUCCEEDED·FAILED·CANCELLED처럼 상태를 명시하면 client가 HTTP connection 하나를 오래 붙잡지 않고도 진행을 이해할 수 있다. |
| T07-P58-B2-76 | T07-P58 독립성 | T07-P58/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P58-B3-77 | T07-P58 상태 | T07-P58/CH03 before·after 위치를 다시 지정 |
| T07-P58-B4-78 | T07-P58 반증 | T07-P58/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P58-B5-79 | T07-P58 적용 | 비동기 작업 상태 API 설계의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · 비동기 작업 상태 API 설계 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **202 응답 후 job이 실제로 저장되지 않거나 status가 모호해 영원히 RUNNING으로 남는다.**

아래 여섯 사례는 T07-P58의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P58-F01 | T07-P58: 대표 실패 | T07-P58: T07-P58/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P58: 현상만 보고 원인을 확정 | T07-P58/CH08 evidence map에서 상태를 대조 |
| T07-P58-F02 | T07-P58: 경계 A 누락 | T07-P58: T07-P58/CH04 경계 A 위반 입력 | T07-P58: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P58/CH08 evidence map에서 상태를 대조 |
| T07-P58-F03 | T07-P58: 경계 B 누락 | T07-P58: T07-P58/CH05 경계 B 위반 입력 | T07-P58: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P58/CH08 evidence map에서 상태를 대조 |
| T07-P58-F04 | T07-P58: 복구 경계 C 누락 | T07-P58: T07-P58/CH07 경계 C 복구 조건 | T07-P58: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P58/CH08 evidence map에서 상태를 대조 |
| T07-P58-F05 | T07-P58: 운영 경계 D 누락 | T07-P58: T07-P58/CH09 경계 D 운영 조건 | T07-P58: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P58/CH08 evidence map에서 상태를 대조 |
| T07-P58-F06 | T07-P58: 증거 없는 결론 | T07-P58: T07-P58/CH02 첫 판단만 존재 | T07-P58: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P58/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P58/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · 비동기 작업 상태 API 설계 — 복구 가능한 상태와 수명

**경계 C.** progress 73% 같은 숫자는 알고리즘상 정확히 계산할 수 있는지 아니면 estimate인지 표시해 완료 시각 보장처럼 오해시키지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P58에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P58/CH08 evidence map을 본다. 복구 후에는 T07-P58/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P58에서 이미 확정된 side effect는 T07-P58/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P58-R1-137 | T07-P58 중단 직전 | T07-P58/CH03에서 이미 확정된 상태만 표시 |
| T07-P58-R2-138 | T07-P58 재시작 직후 | progress 73% 같은 숫자는 알고리즘상 정확히 계산할 수 있는지 아니면 estimate인지 표시해 완료 시각 보장처럼 오해시키지 않는다. |
| T07-P58-R3-139 | T07-P58 재검증 | T07-P58/CH08 근거로 중복·누락 여부 확인 |
| T07-P58-R4-140 | T07-P58 재실행 | T07-P58/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · 비동기 작업 상태 API 설계 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | job은 RUNNING인데 HTTP 요청은 끝났다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | 202와 status resource를 설계한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | 생성 요청은 job resource를 만들고 client는 status/result/error를 조회하거나 callback을 받는다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 202 응답 후 job이 실제로 저장되지 않거나 status가 모호해 영원히 RUNNING으로 남는다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | job state transition, heartbeat/progress, terminal reason, retention을 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`비동기 작업 상태 API 설계` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · 비동기 작업 상태 API 설계 — 운영 한계와 종료 조건

**경계 D.** 완료 result와 error detail을 언제까지 보관할지, polling을 너무 자주 해 overload시키지 않도록 minimum interval/backoff를 어떻게 안내할지도 API contract다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P58/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P58/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P58 과제: 경계 D와 T07-P58/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P58-O1-199 | synthetic-load=59 | T07-P58/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P58-O2-200 | synthetic-budget=349ms | T07-P58 timeout과 unknown outcome을 분리 |
| T07-P58-O3-201 | T07-P58 종료 | 완료 result와 error detail을 언제까지 보관할지, polling을 너무 자주 해 overload시키지 않도록 minimum interval/backoff를 어떻게 안내할지도 API contract다. |
| T07-P58-O4-202 | T07-P58 완화 | T07-P58/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · 비동기 작업 상태 API 설계 — 직접 실행하는 작은 모델

`비동기 작업 상태 API 설계` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P058 0`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P058";
const capacity = 6;
const requests = 6;
const waiting = Math.max(0, requests - capacity);
console.log(marker, waiting);
```

기준 출력: `T07-P058 0`.

`비동기 작업 상태 API 설계`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P58-L1-231 | constmarker="T07-P058"; | T07-P58 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P58-L2-232 | constcapacity=6; | T07-P58 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P58-L3-233 | constrequests=6; | T07-P58 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P58-L4-234 | constwaiting=Math.max(0,requests-capacity); | T07-P58 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P58-L5-235 | console.log(marker,waiting); | T07-P58 출력 관측점; 예상 `T07-P058 0`와 비교 |
| T07-P58-LX-320 | T07-P58 실행 기록 | T07-P58 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · 비동기 작업 상태 API 설계 — 한 부분만 수정하고 다시 예측

수정 과제: **capacity를 1 줄여 waiter가 얼마나 늘어나는지 계산한다**.

수정 전은 `T07-P058 0`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P58/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P58-D1-261 | 기준 `T07-P058 0` | T07-P58 수정 전 실행을 먼저 재현 |
| T07-P58-D2-262 | capacity를 1 줄여 waiter가 얼마나 늘어나는지 계산한다 | T07-P58 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P58-D3-263 | T07-P58 새 예측 | T07-P58 실행 전에 출력·상태를 먼저 기록 |
| T07-P58-D4-264 | T07-P58 재실행 | T07-P58/CH10 실제값과 새 예측을 대조 |
| T07-P58-D5-265 | T07-P58 반례 | T07-P58/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P58-D6-266 | T07-P58 근거 | T07-P58/CH08 상태가 설명과 일치해야 완료 |
| T07-P58-D7-267 | T07-P58 이유 | T07-P58 변경 이유를 비동기 작업 상태 API 설계 계약과 연결해 설명 |

## CHAPTER 12 · 비동기 작업 상태 API 설계 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: 비동기 작업 상태 API 설계 | 오래 걸리는 작업은 POST 하나에서 끝날 때까지 기다리기보다 job id와 상태 조회 계약을 제공할 수 있다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P58/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | 생성 요청은 job resource를 만들고 client는 status/result/error를 조회하거나 callback을 받는다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 202 응답 후 job이 실제로 저장되지 않거나 status가 모호해 영원히 RUNNING으로 남는다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | job state transition, heartbeat/progress, terminal reason, retention을 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P58/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P58/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P58/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P58/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `비동기 작업 상태 API 설계` 실행 코드 수정 | capacity를 1 줄여 waiter가 얼마나 늘어나는지 계산한다 | `비동기 작업 상태 API 설계` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P58/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P58/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · 비동기 작업 상태 API 설계 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 오래 걸리는 작업은 POST 하나에서 끝날 때까지 기다리기보다 job id와 상태 조회 계약을 제공할 수 있다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P58/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P58/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P58/CH10 실행용 boilerplate·test 후보 | T07-P58: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P058 0` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P58/CH02의 판단 기준과 T07-P58/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P58-AI1-323 | T07-P58 사람 결정 | T07-P58 업무 의미·허용 위험·완료 기준 소유 |
| T07-P58-AI2-324 | T07-P58 AI 초안 | T07-P58/CH10 boilerplate·test 후보까지만 위임 |
| T07-P58-AI3-325 | T07-P58 검증 | T07-P58/CH06 반례와 T07-P58/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · 비동기 작업 상태 API 설계 — 경계 조합 실험 8개

T07-P58: T07-P58/CH04~T07-P58/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P58의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-K01 | T07-P58: 경계 A | T07-P58: 경계 B | T07-P58: DRAIN[ready=0; active=16; drain_deadline_s=6] / sample=33 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + DRAIN_STATE |
| T07-P58-K02 | T07-P58: 경계 A | T07-P58: 경계 C | T07-P58: RECOVERY_SCOPE[selected=46; expected=12; backup=1; dry_run=0] / sample=40 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + RECOVERY_AUDIT |
| T07-P58-K03 | T07-P58: 경계 A | T07-P58: 경계 D | T07-P58: REPLAY[key=cmd-058-03; attempts=2; response_seen=0] / sample=47 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + IDEMPOTENCY_RECORD |
| T07-P58-K04 | T07-P58: 경계 B | T07-P58: 경계 C | T07-P58: CONCURRENT_WRITE[actors=2; base_version=5; writes=2; gap_ms=71] / sample=54 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + STATE_VERSION |
| T07-P58-K05 | T07-P58: 경계 B | T07-P58: 경계 D | T07-P58: UNKNOWN_OUTCOME[timeout_ms=159; provider_state=UNKNOWN; lookup_id=p05805] / sample=61 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + PROVIDER_RESULT |
| T07-P58-K06 | T07-P58: 경계 C | T07-P58: 경계 D | T07-P58: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] / sample=68 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + DURABLE_STATE |
| T07-P58-K07 | T07-P58: 경계 A | T07-P58: 경계 B+C | T07-P58: OVERLOAD[rps=443; p99_ms=417; queue=30] / sample=75 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + QUEUE_PRESSURE |
| T07-P58-K08 | T07-P58: 경계 B | T07-P58: 경계 C+D | T07-P58: SAMPLING[sample_rate=36%; trace_present=0; metric_present=1] / sample=82 | T07-P58: 먼저 깨지는 경계를 판정 | T07-P58: T07-P58/CH08 + TRACE_METRIC_CROSSCHECK |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P58/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · 비동기 작업 상태 API 설계 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P58와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-B12 | T07-P58: backpressure와 load shedding | T07-P58: worker queue가 계속 증가한다 | T07-P58: backpressure와 load shedding 시점을 정한다 | T07-P58: T07-P58/CH08 증거와 형제 LESSON 증거를 분리 | T07-P58: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P58-B14 | T07-P58: batch와 chunking으로 큰 일을 재시작 가능하게 만들기 | T07-P58: 1GB 파일을 100개 chunk로 처리한다 | T07-P58: checkpoint와 partial retry를 만든다 | T07-P58: T07-P58/CH08 증거와 형제 LESSON 증거를 분리 | T07-P58: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P58-B11 | T07-P58: delayed job과 예약 실행 | T07-P58: 09:00 예약 job이 09:07에 실행된다 | T07-P58: schedule time과 execution time을 구분한다 | T07-P58: T07-P58/CH08 증거와 형제 LESSON 증거를 분리 | T07-P58: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P58-B15 | T07-P58: BLOCK 04 종합: 이미지·보고서 처리 pipeline | T07-P58: upload→queue→worker→result 전체를 잇는다 | T07-P58: pipeline의 owner와 failure state를 표시한다 | T07-P58: T07-P58/CH08 증거와 형제 LESSON 증거를 분리 | T07-P58: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P58-B10 | T07-P58: ordering과 partition key | T07-P58: 한 tenant가 같은 partition을 독점한다 | T07-P58: ordering과 hot partition을 함께 본다 | T07-P58: T07-P58/CH08 증거와 형제 LESSON 증거를 분리 | T07-P58: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · 비동기 작업 상태 API 설계 — 선택형 실패 주입 6개

T07-P58: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P58 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-X01 | T07-P58: 재전송 | T07-P58: 응답 유실 뒤 같은 command가 다시 도착함; sample=818 | T07-P58: 중복 side effect 여부 | T07-P58: T07-P58/CH02 판단과 별도 기록 | T07-P58: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P58-X02 | T07-P58: 동시 변경 | T07-P58: 두 actor가 같은 resource를 수정함; sample=835 | T07-P58: lost update 또는 conflict | T07-P58: T07-P58/CH02 판단과 별도 기록 | T07-P58: version·affected rows·lock/wait 기록 |
| T07-P58-X03 | T07-P58: unknown outcome | T07-P58: dependency timeout 후 성공 여부 불명; sample=852 | T07-P58: 실패와 미확정 결과 | T07-P58: T07-P58/CH02 판단과 별도 기록 | T07-P58: provider id·조회 결과·retry history |
| T07-P58-X04 | T07-P58: 재시작 | T07-P58: side effect 직후 process가 재시작됨; sample=869 | T07-P58: durable state와 memory state | T07-P58: T07-P58/CH02 판단과 별도 기록 | T07-P58: commit·outbox·job id·restart 전후 상태 |
| T07-P58-X05 | T07-P58: 과부하 | T07-P58: traffic 세 배, p99 급증; sample=886 | T07-P58: 기능 실패와 saturation | T07-P58: T07-P58/CH02 판단과 별도 기록 | T07-P58: queue age·pool wait·CPU/event-loop·quota |
| T07-P58-X06 | T07-P58: sampling | T07-P58: 일부 log가 sampling으로 빠짐; sample=903 | T07-P58: 기록 부재와 사건 부재 | T07-P58: T07-P58/CH02 판단과 별도 기록 | T07-P58: metric·trace·durable state 교차 근거 |

## CHAPTER 17 · 비동기 작업 상태 API 설계 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P58에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-E01 | T07-P58: 대표 실패를 원인으로 착각 | T07-P58: T07-P58/CH06 실패 case를 다른 입력으로 재현 | T07-P58: 현상과 원인을 같은 것으로 봄 | T07-P58: T07-P58/CH06 대표 실패와 T07-P58/CH08 증거를 다시 대조 | T07-P58: T07-P58/CH08 |
| T07-P58-E02 | T07-P58: 경계 A 생략 | T07-P58: T07-P58/CH04의 조건 하나를 반대로 설정 | T07-P58: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P58: T07-P58/CH04를 새 입력에 적용 | T07-P58: T07-P58/CH08 |
| T07-P58-E03 | T07-P58: 경계 B 생략 | T07-P58: T07-P58/CH05의 조건 하나를 반대로 설정 | T07-P58: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P58: T07-P58/CH05를 새 입력에 적용 | T07-P58: T07-P58/CH08 |
| T07-P58-E04 | T07-P58: 복구 상태 혼동 | T07-P58: T07-P58/CH07에서 처리 중단을 주입 | T07-P58: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P58: T07-P58/CH07에서 수명 경계를 다시 표시 | T07-P58: T07-P58/CH08 |
| T07-P58-E05 | T07-P58: 운영 한계 누락 | T07-P58: T07-P58/CH09에서 부하 또는 drain 조건을 변경 | T07-P58: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P58: T07-P58/CH09의 종료 조건을 다시 작성 | T07-P58: T07-P58/CH08 |
| T07-P58-E06 | T07-P58: 증거 없는 성공 판정 | T07-P58: T07-P58/CH08에서 증거 하나를 숨김 | T07-P58: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P58: T07-P58/CH08에서 독립 증거 둘을 선택 | T07-P58: T07-P58/CH08 |

## CHAPTER 18 · 비동기 작업 상태 API 설계 — synthetic 관측값 판독 6개

T07-P58: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P58의 숫자 하나만으로 원인을 단정하지 않고 T07-P58/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-O01 | T07-P58/cache_hit_ratio | 752 | T07-P58: cache hit 비율 | T07-P58: 축=과부하; 원인 확정 금지 | T07-P58: QUEUE_PRESSURE + T07-P58/CH08 |
| T07-P58-O02 | T07-P58/cache_age_s | 769 | T07-P58: cache 항목 나이 | T07-P58: 축=sampling; 원인 확정 금지 | T07-P58: TRACE_METRIC_CROSSCHECK + T07-P58/CH08 |
| T07-P58-O03 | T07-P58/queue_depth | 786 | T07-P58: 대기 job 수 | T07-P58: 축=client disconnect; 원인 확정 금지 | T07-P58: COMMIT_TIMELINE + T07-P58/CH08 |
| T07-P58-O04 | T07-P58/job_attempt | 803 | T07-P58: job 실행 횟수 | T07-P58: 축=재발; 원인 확정 금지 | T07-P58: RECURRENCE_TIMELINE + T07-P58/CH08 |
| T07-P58-O05 | T07-P58/dlq_count | 820 | T07-P58: DLQ 항목 수 | T07-P58: 축=대형 입력; 원인 확정 금지 | T07-P58: SIZE_LIMIT + T07-P58/CH08 |
| T07-P58-O06 | T07-P58/worker_latency_ms | 837 | T07-P58: worker 처리 지연 | T07-P58: 축=순서 역전; 원인 확정 금지 | T07-P58: SEQUENCE_STATE + T07-P58/CH08 |

## CHAPTER 19 · 비동기 작업 상태 API 설계 — 선택형 코드 리뷰 6질문

T07-P58: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P58에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-R01 | T07-P58: 동시성 | T07-P58: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P58: SAMPLING[sample_rate=29%; trace_present=0; metric_present=1] | T07-P58: T07-P58/CH04 | T07-P58: TRACE_METRIC_CROSSCHECK |
| T07-P58-R02 | T07-P58: 권한 | T07-P58: actor·action·resource가 같은 판단 안에 있는가 | T07-P58: DISCONNECT[disconnect_ms=52; commit_state=UNKNOWN; request=058-01] | T07-P58: T07-P58/CH05 | T07-P58: COMMIT_TIMELINE |
| T07-P58-R03 | T07-P58: 자원 | T07-P58: pool·queue·memory·connection 상한이 있는가 | T07-P58: RECURRENCE[occurrence=4; interval_s=56; mitigation_applied=1] | T07-P58: T07-P58/CH06 | T07-P58: RECURRENCE_TIMELINE |
| T07-P58-R04 | T07-P58: 상태 변경 | T07-P58: side effect가 어느 줄에서 확정되는가 | T07-P58: LARGE_INPUT[body_kb=808; limit_kb=640; parsed=0] | T07-P58: T07-P58/CH07 | T07-P58: SIZE_LIMIT |
| T07-P58-R05 | T07-P58: 동시성 | T07-P58: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P58: REORDER[in_seq=7,5,6; applied_version=5] | T07-P58: T07-P58/CH04 | T07-P58: SEQUENCE_STATE |
| T07-P58-R06 | T07-P58: 권한 | T07-P58: actor·action·resource가 같은 판단 안에 있는가 | T07-P58: DRAIN[ready=0; active=17; drain_deadline_s=10] | T07-P58: T07-P58/CH05 | T07-P58: DRAIN_STATE |

## CHAPTER 20 · 비동기 작업 상태 API 설계 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P58에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-I01 | T07-P58: correlation | T07-P58: 한 request/job/resource id를 시간축에 고정 | T07-P58: DISCONNECT[disconnect_ms=39; commit_state=UNKNOWN; request=058-00] | T07-P58: T07-P58/CH08 + COMMIT_TIMELINE | T07-P58-incident-111 |
| T07-P58-I02 | T07-P58: 마지막 정상 | T07-P58: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P58: RECURRENCE[occurrence=3; interval_s=45; mitigation_applied=1] | T07-P58: T07-P58/CH08 + RECURRENCE_TIMELINE | T07-P58-incident-112 |
| T07-P58-I03 | T07-P58: 가설 검증 | T07-P58: 원인 후보 하나만 뒤집어 재현 | T07-P58: LARGE_INPUT[body_kb=720; limit_kb=640; parsed=0] | T07-P58: T07-P58/CH08 + SIZE_LIMIT | T07-P58-incident-113 |
| T07-P58-I04 | T07-P58: 복구 확인 | T07-P58: durable state와 사용자 결과를 모두 확인 | T07-P58: REORDER[in_seq=6,4,5; applied_version=5] | T07-P58: T07-P58/CH08 + SEQUENCE_STATE | T07-P58-incident-114 |
| T07-P58-I05 | T07-P58: 재주입 | T07-P58: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P58: DRAIN[ready=0; active=4; drain_deadline_s=9] | T07-P58: T07-P58/CH08 + DRAIN_STATE | T07-P58-incident-115 |
| T07-P58-I06 | T07-P58: 회귀 고정 | T07-P58: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P58: RECOVERY_SCOPE[selected=79; expected=13; backup=1; dry_run=1] | T07-P58: T07-P58/CH08 + RECOVERY_AUDIT | T07-P58-incident-116 |

## CHAPTER 21 · 비동기 작업 상태 API 설계 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P58/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P58/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P58/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P58/CH04~T07-P58/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P58/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P58/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P58/CH18 signal 두 개와 T07-P58/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P58/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P58/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · 비동기 작업 상태 API 설계 — 통합 casebook 16문제

T07-P58 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P58 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P58-C01 | T07-P58: 경계 A | T07-P58: RECURRENCE[occurrence=2; interval_s=245; mitigation_applied=1] | T07-P58: load=882, window=96s | T07-P58: review=중복 | T07-P58: 경계 A 위반 여부를 판정 | T07-P58: RECURRENCE_TIMELINE + T07-P58/CH08 |
| T07-P58-C02 | T07-P58: 경계 B | T07-P58: LARGE_INPUT[body_kb=632; limit_kb=640; parsed=0] | T07-P58: load=905, window=25s | T07-P58: review=권한 | T07-P58: 경계 B 위반 여부를 판정 | T07-P58: SIZE_LIMIT + T07-P58/CH08 |
| T07-P58-C03 | T07-P58: 경계 C | T07-P58: REORDER[in_seq=5,3,4; applied_version=5] | T07-P58: load=928, window=44s | T07-P58: review=입력 경계 | T07-P58: 경계 C 위반 여부를 판정 | T07-P58: SEQUENCE_STATE + T07-P58/CH08 |
| T07-P58-C04 | T07-P58: 경계 D | T07-P58: DRAIN[ready=0; active=8; drain_deadline_s=8] | T07-P58: load=951, window=63s | T07-P58: review=timeout | T07-P58: 경계 D 위반 여부를 판정 | T07-P58: DRAIN_STATE + T07-P58/CH08 |
| T07-P58-C05 | T07-P58: 경계 A | T07-P58: RECOVERY_SCOPE[selected=68; expected=19; backup=1; dry_run=0] | T07-P58: load=974, window=82s | T07-P58: review=자원 | T07-P58: 경계 A 위반 여부를 판정 | T07-P58: RECOVERY_AUDIT + T07-P58/CH08 |
| T07-P58-C06 | T07-P58: 경계 B | T07-P58: REPLAY[key=cmd-058-05; attempts=4; response_seen=0] | T07-P58: load=997, window=11s | T07-P58: review=순서 | T07-P58: 경계 B 위반 여부를 판정 | T07-P58: IDEMPOTENCY_RECORD + T07-P58/CH08 |
| T07-P58-C07 | T07-P58: 경계 C | T07-P58: CONCURRENT_WRITE[actors=2; base_version=5; writes=2; gap_ms=97] | T07-P58: load=120, window=30s | T07-P58: review=관측 | T07-P58: 경계 C 위반 여부를 판정 | T07-P58: STATE_VERSION + T07-P58/CH08 |
| T07-P58-C08 | T07-P58: 경계 D | T07-P58: UNKNOWN_OUTCOME[timeout_ms=181; provider_state=UNKNOWN; lookup_id=p05807] | T07-P58: load=143, window=49s | T07-P58: review=상태 변경 | T07-P58: 경계 D 위반 여부를 판정 | T07-P58: PROVIDER_RESULT + T07-P58/CH08 |
| T07-P58-C09 | T07-P58: 경계 A | T07-P58: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P58: load=166, window=68s | T07-P58: review=retry | T07-P58: 경계 A 위반 여부를 판정 | T07-P58: DURABLE_STATE + T07-P58/CH08 |
| T07-P58-C10 | T07-P58: 경계 B | T07-P58: OVERLOAD[rps=509; p99_ms=651; queue=38] | T07-P58: load=189, window=87s | T07-P58: review=복구 | T07-P58: 경계 B 위반 여부를 판정 | T07-P58: QUEUE_PRESSURE + T07-P58/CH08 |
| T07-P58-C11 | T07-P58: 경계 C | T07-P58: SAMPLING[sample_rate=62%; trace_present=0; metric_present=1] | T07-P58: load=212, window=16s | T07-P58: review=동시성 | T07-P58: 경계 C 위반 여부를 판정 | T07-P58: TRACE_METRIC_CROSSCHECK + T07-P58/CH08 |
| T07-P58-C12 | T07-P58: 경계 D | T07-P58: DISCONNECT[disconnect_ms=85; commit_state=UNKNOWN; request=058-11] | T07-P58: load=235, window=35s | T07-P58: review=민감정보 | T07-P58: 경계 D 위반 여부를 판정 | T07-P58: COMMIT_TIMELINE + T07-P58/CH08 |
| T07-P58-C13 | T07-P58: 경계 A | T07-P58: RECURRENCE[occurrence=4; interval_s=166; mitigation_applied=1] | T07-P58: load=258, window=54s | T07-P58: review=중복 | T07-P58: 경계 A 위반 여부를 판정 | T07-P58: RECURRENCE_TIMELINE + T07-P58/CH08 |
| T07-P58-C14 | T07-P58: 경계 B | T07-P58: LARGE_INPUT[body_kb=1688; limit_kb=640; parsed=0] | T07-P58: load=281, window=73s | T07-P58: review=권한 | T07-P58: 경계 B 위반 여부를 판정 | T07-P58: SIZE_LIMIT + T07-P58/CH08 |
| T07-P58-C15 | T07-P58: 경계 C | T07-P58: REORDER[in_seq=17,15,16; applied_version=5] | T07-P58: load=304, window=92s | T07-P58: review=입력 경계 | T07-P58: 경계 C 위반 여부를 판정 | T07-P58: SEQUENCE_STATE + T07-P58/CH08 |
| T07-P58-C16 | T07-P58: 경계 D | T07-P58: DRAIN[ready=0; active=4; drain_deadline_s=12] | T07-P58: load=327, window=21s | T07-P58: review=timeout | T07-P58: 경계 D 위반 여부를 판정 | T07-P58: DRAIN_STATE + T07-P58/CH08 |

채점은 결론보다 근거를 본다. T07-P58/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · 비동기 작업 상태 API 설계 — evidence 판독 문제 14개

T07-P58 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P58 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P58-V01 | T07-P58: latency=598ms; queue=6; retry=2 | T07-P58: 대형 입력 | T07-P58: 축=대형 입력; 원인 확정은 보류 | T07-P58: SIZE_LIMIT + T07-P58/CH08 | T07-P58: 피할 오판=경계 누락 |
| T07-P58-V02 | T07-P58: latency=665ms; queue=17; retry=5 | T07-P58: 순서 역전 | T07-P58: 축=순서 역전; 원인 확정은 보류 | T07-P58: SEQUENCE_STATE + T07-P58/CH08 | T07-P58: 피할 오판=증거 혼동 |
| T07-P58-V03 | T07-P58: latency=732ms; queue=28; retry=1 | T07-P58: drain | T07-P58: 축=drain; 원인 확정은 보류 | T07-P58: DRAIN_STATE + T07-P58/CH08 | T07-P58: 피할 오판=재시도 오판 |
| T07-P58-V04 | T07-P58: latency=799ms; queue=39; retry=4 | T07-P58: 복구 범위 | T07-P58: 축=복구 범위; 원인 확정은 보류 | T07-P58: RECOVERY_AUDIT + T07-P58/CH08 | T07-P58: 피할 오판=동시성 무시 |
| T07-P58-V05 | T07-P58: latency=866ms; queue=50; retry=0 | T07-P58: 재전송 | T07-P58: 축=재전송; 원인 확정은 보류 | T07-P58: IDEMPOTENCY_RECORD + T07-P58/CH08 | T07-P58: 피할 오판=순서 가정 |
| T07-P58-V06 | T07-P58: latency=933ms; queue=61; retry=3 | T07-P58: 동시 변경 | T07-P58: 축=동시 변경; 원인 확정은 보류 | T07-P58: STATE_VERSION + T07-P58/CH08 | T07-P58: 피할 오판=상태 수명 혼동 |
| T07-P58-V07 | T07-P58: latency=1000ms; queue=72; retry=6 | T07-P58: unknown outcome | T07-P58: 축=unknown outcome; 원인 확정은 보류 | T07-P58: PROVIDER_RESULT + T07-P58/CH08 | T07-P58: 피할 오판=운영 한계 누락 |
| T07-P58-V08 | T07-P58: latency=1067ms; queue=3; retry=2 | T07-P58: 재시작 | T07-P58: 축=재시작; 원인 확정은 보류 | T07-P58: DURABLE_STATE + T07-P58/CH08 | T07-P58: 피할 오판=오류 합치기 |
| T07-P58-V09 | T07-P58: latency=1134ms; queue=14; retry=5 | T07-P58: 과부하 | T07-P58: 축=과부하; 원인 확정은 보류 | T07-P58: QUEUE_PRESSURE + T07-P58/CH08 | T07-P58: 피할 오판=복구 과잉 |
| T07-P58-V10 | T07-P58: latency=1201ms; queue=25; retry=1 | T07-P58: sampling | T07-P58: 축=sampling; 원인 확정은 보류 | T07-P58: TRACE_METRIC_CROSSCHECK + T07-P58/CH08 | T07-P58: 피할 오판=잘못된 전제 |
| T07-P58-V11 | T07-P58: latency=1268ms; queue=36; retry=4 | T07-P58: client disconnect | T07-P58: 축=client disconnect; 원인 확정은 보류 | T07-P58: COMMIT_TIMELINE + T07-P58/CH08 | T07-P58: 피할 오판=경계 누락 |
| T07-P58-V12 | T07-P58: latency=1335ms; queue=47; retry=0 | T07-P58: 재발 | T07-P58: 축=재발; 원인 확정은 보류 | T07-P58: RECURRENCE_TIMELINE + T07-P58/CH08 | T07-P58: 피할 오판=증거 혼동 |
| T07-P58-V13 | T07-P58: latency=1402ms; queue=58; retry=3 | T07-P58: 대형 입력 | T07-P58: 축=대형 입력; 원인 확정은 보류 | T07-P58: SIZE_LIMIT + T07-P58/CH08 | T07-P58: 피할 오판=재시도 오판 |
| T07-P58-V14 | T07-P58: latency=1469ms; queue=69; retry=6 | T07-P58: 순서 역전 | T07-P58: 축=순서 역전; 원인 확정은 보류 | T07-P58: SEQUENCE_STATE + T07-P58/CH08 | T07-P58: 피할 오판=동시성 무시 |

## CHAPTER 24 · 비동기 작업 상태 API 설계 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P58에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P58-D01 | T07-P58: 순서 병렬화 | T07-P58: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P58: 선후관계 invariant와 race를 깨지 않는지 | T07-P58: DRAIN[ready=0; active=3; drain_deadline_s=5] | T07-P58: DRAIN_STATE + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P58-D02 | T07-P58: validation 이동 | T07-P58: validation을 business side effect 뒤로 옮긴다 | T07-P58: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P58: RECOVERY_SCOPE[selected=35; expected=18; backup=1; dry_run=1] | T07-P58: RECOVERY_AUDIT + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P58-D03 | T07-P58: batch 확대 | T07-P58: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P58: memory·deadline·부분 실패 범위가 커지는지 | T07-P58: REPLAY[key=cmd-058-02; attempts=4; response_seen=0] | T07-P58: IDEMPOTENCY_RECORD + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P58-D04 | T07-P58: fallback 추가 | T07-P58: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P58: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P58: CONCURRENT_WRITE[actors=2; base_version=5; writes=2; gap_ms=58] | T07-P58: STATE_VERSION + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P58-D05 | T07-P58: 외부 호출 이동 | T07-P58: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P58: lock duration과 unknown outcome 경계가 달라지는지 | T07-P58: UNKNOWN_OUTCOME[timeout_ms=148; provider_state=UNKNOWN; lookup_id=p05804] | T07-P58: PROVIDER_RESULT + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P58-D06 | T07-P58: cache 추가 | T07-P58: 현재 결과 앞에 cache layer를 추가한다 | T07-P58: stale·key·invalidation 책임이 새로 생기는지 | T07-P58: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P58: DURABLE_STATE + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P58-D07 | T07-P58: pool 확대 | T07-P58: connection/worker pool 상한을 늘린다 | T07-P58: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P58: OVERLOAD[rps=410; p99_ms=1173; queue=33] | T07-P58: QUEUE_PRESSURE + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P58-D08 | T07-P58: schema 변경 | T07-P58: 필드 이름·형식·required 조건을 바꾼다 | T07-P58: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P58: SAMPLING[sample_rate=23%; trace_present=1; metric_present=1] | T07-P58: TRACE_METRIC_CROSSCHECK + T07-P58/CH08 | T07-P58: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P58: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · 비동기 작업 상태 API 설계 — 최종 contract와 evidence spine

**최종 계약:** 오래 걸리는 작업은 POST 하나에서 끝날 때까지 기다리기보다 job id와 상태 조회 계약을 제공할 수 있다.

**정상 메커니즘:** 생성 요청은 job resource를 만들고 client는 status/result/error를 조회하거나 callback을 받는다.

**대표 실패:** 202 응답 후 job이 실제로 저장되지 않거나 status가 모호해 영원히 RUNNING으로 남는다.

**검증 evidence:** job state transition, heartbeat/progress, terminal reason, retention을 본다.

**직접 행동:** job event 목록으로 최종 상태와 progress를 계산한다.

**다음 연결:** `batch와 chunking으로 큰 일을 재시작 가능하게 만들기`.

`비동기 작업 상태 API 설계`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | 비동기 작업 상태 API 설계의 개념·실패·운영 판단 교차 확인 |
| RFC9111 | HTTP Caching | 비동기 작업 상태 API 설계의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | 비동기 작업 상태 API 설계의 개념·실패·운영 판단 교차 확인 |
| SRE-WORKBOOK | SRE-WORKBOOK | 비동기 작업 상태 API 설계의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | 비동기 작업 상태 API 설계의 개념·실패·운영 판단 교차 확인 |

`비동기 작업 상태 API 설계` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
