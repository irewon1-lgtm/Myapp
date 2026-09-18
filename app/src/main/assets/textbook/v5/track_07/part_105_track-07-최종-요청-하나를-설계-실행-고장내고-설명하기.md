# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 07 · AI와 함께 백엔드를 만들되 사람이 계약과 검증을 통제하기

### LESSON 15 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기

## CHAPTER 01 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 최종 목표는 framework 이름 암기가 아니라 요청의 수명주기와 상태·신뢰·비동기·장애 경계를 스스로 설명하고 검증하는 것이다.

**아주 쉬운 사건.** 최종 request에 네 failure를 주입한다. 이 사건에서는 먼저 **사람 판단·AI 생성·자동 검증을 분리한다**.

**왜 필요한가.** 정상 동작은 한 요청을 route부터 response/background completion까지 추적하며 각 단계의 contract와 evidence를 연결한다. 반대로 AI가 만들어 준 코드를 실행 성공만 보고 믿거나, 반대로 모든 저수준 구현을 사람이 직접 작성하려 한다.

**암기:** `TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기`의 역할 한 줄.

**직접 이해:** `TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기`의 입력·상태·결과 경계.

**AI 위임 가능:** `TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `최종 request에 네 failure를 주입한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 정상·경계·실패·과부하 case의 실제 실행/관측 증거와 사람이 확인한 invariant를 최종 산출물로 남긴다.

## CHAPTER 02 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 아주 쉬운 예를 한 단계씩 해석

T07-P105: `최종 request에 네 failure를 주입한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 최종 request에 네 failure를 주입한다 | T07-P105 외부 입력 | T07-P105: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | 사람 판단·AI 생성·자동 검증을 분리한다 | T07-P105 판단 기준 | T07-P105/CH08 관측표와 대조 |
| 정상 경로 | T07-P105/CH03 M1→M5 | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기: 완료 시점을 단계별로 분리 | T07-P105: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P105/CH06 AI가 만들어 준 코드를 실행 성공만 보고 믿거나, 반대로 모든 저수준 구현을 사람이 직접 작성하려 한다. | T07-P105: 깨진 계약 하나를 특정 | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기: 증상과 원인을 분리 |
| 재검증 | T07-P105/CH10 직접 실행 | T07-P105: 예상값 T07-P105 RECEIVED 기록 | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P105/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P105/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 내부 메커니즘과 상태 전이

한 요청을 route부터 response/background completion까지 추적하며 각 단계의 contract와 evidence를 연결한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 최종 request에 네 failure를 주입한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | 사람 판단·AI 생성·자동 검증을 분리한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | 한 요청을 route부터 response/background completion까지 추적하며 각 단계의 contract와 evidence를 연결한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 정상·경계·실패·과부하 case의 실제 실행/관측 증거와 사람이 확인한 invariant를 최종 산출물로 남긴다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 종합 시나리오에서 어느 단계가 사람 판단, AI 생성, 자동 검증 대상인지 분류한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기` 흐름을 framework 이름 없이 설명한다.

막히면 `최종 request에 네 failure를 주입한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 실전 경계 A

**경계 A.** 최종 실습은 request 하나를 route→validation→authorization→domain decision→state commit→async side effect→response/status 조회까지 실제 순서로 설명하는 것이다.

T07-P105/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P105에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P105/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P105/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P105): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P105/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P105-A1-968 | T07-P105 조건 | 최종 실습은 request 하나를 route→validation→authorization→domain decision→state commit→async side effect→response/status 조회까지 실제 순서로 설명하는 것이다. |
| T07-P105-A2-969 | T07-P105 변화점 | T07-P105/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P105-A3-970 | T07-P105 반례 | T07-P105/CH06 대표 실패와 A 위반을 구별 |
| T07-P105-A4-971 | T07-P105 근거 | T07-P105/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P105-A5-972 | T07-P105 재실험 | 최종 request에 네 failure를 주입한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 실전 경계 B

**경계 B.** invalid input·duplicate request·dependency timeout·worker duplicate 같은 서로 다른 failure mode를 최소 네 가지 주입해 각각 어떤 invariant가 지켜져야 하는지 적는다.

T07-P105/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P105에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P105/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P105/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P105): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P105/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P105-B1-2 | T07-P105 조건 | invalid input·duplicate request·dependency timeout·worker duplicate 같은 서로 다른 failure mode를 최소 네 가지 주입해 각각 어떤 invariant가 지켜져야 하는지 적는다. |
| T07-P105-B2-3 | T07-P105 독립성 | T07-P105/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P105-B3-4 | T07-P105 상태 | T07-P105/CH03 before·after 위치를 다시 지정 |
| T07-P105-B4-5 | T07-P105 반증 | T07-P105/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P105-B5-6 | T07-P105 적용 | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **AI가 만들어 준 코드를 실행 성공만 보고 믿거나, 반대로 모든 저수준 구현을 사람이 직접 작성하려 한다.**

아래 여섯 사례는 T07-P105의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P105-F01 | T07-P105: 대표 실패 | T07-P105: T07-P105/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P105: 현상만 보고 원인을 확정 | T07-P105/CH08 evidence map에서 상태를 대조 |
| T07-P105-F02 | T07-P105: 경계 A 누락 | T07-P105: T07-P105/CH04 경계 A 위반 입력 | T07-P105: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P105/CH08 evidence map에서 상태를 대조 |
| T07-P105-F03 | T07-P105: 경계 B 누락 | T07-P105: T07-P105/CH05 경계 B 위반 입력 | T07-P105: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P105/CH08 evidence map에서 상태를 대조 |
| T07-P105-F04 | T07-P105: 복구 경계 C 누락 | T07-P105: T07-P105/CH07 경계 C 복구 조건 | T07-P105: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P105/CH08 evidence map에서 상태를 대조 |
| T07-P105-F05 | T07-P105: 운영 경계 D 누락 | T07-P105: T07-P105/CH09 경계 D 운영 조건 | T07-P105: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P105/CH08 evidence map에서 상태를 대조 |
| T07-P105-F06 | T07-P105: 증거 없는 결론 | T07-P105: T07-P105/CH02 첫 판단만 존재 | T07-P105: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P105/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P105/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 복구 가능한 상태와 수명

**경계 C.** 복구는 restart 한 번으로 끝내지 않고 retry·idempotency·compensation·reconciliation 중 어떤 수단이 왜 필요한지 상태 변화와 연결한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P105에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P105/CH08 evidence map을 본다. 복구 후에는 T07-P105/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P105에서 이미 확정된 side effect는 T07-P105/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P105-R1-64 | T07-P105 중단 직전 | T07-P105/CH03에서 이미 확정된 상태만 표시 |
| T07-P105-R2-65 | T07-P105 재시작 직후 | 복구는 restart 한 번으로 끝내지 않고 retry·idempotency·compensation·reconciliation 중 어떤 수단이 왜 필요한지 상태 변화와 연결한다. |
| T07-P105-R3-66 | T07-P105 재검증 | T07-P105/CH08 근거로 중복·누락 여부 확인 |
| T07-P105-R4-67 | T07-P105 재실행 | T07-P105/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 최종 request에 네 failure를 주입한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | 사람 판단·AI 생성·자동 검증을 분리한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | 한 요청을 route부터 response/background completion까지 추적하며 각 단계의 contract와 evidence를 연결한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | AI가 만들어 준 코드를 실행 성공만 보고 믿거나, 반대로 모든 저수준 구현을 사람이 직접 작성하려 한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 정상·경계·실패·과부하 case의 실제 실행/관측 증거와 사람이 확인한 invariant를 최종 산출물로 남긴다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 운영 한계와 종료 조건

**경계 D.** 사람은 업무 의미·risk·완료 증거를 결정하고, AI는 반복 구현 초안을 만들며, 자동 test/eval은 정의된 contract를 반복 검증하도록 세 역할을 명확히 분리한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P105/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P105/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P105 과제: 경계 D와 T07-P105/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P105-O1-126 | synthetic-load=166 | T07-P105/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P105-O2-127 | synthetic-budget=276ms | T07-P105 timeout과 unknown outcome을 분리 |
| T07-P105-O3-128 | T07-P105 종료 | 사람은 업무 의미·risk·완료 증거를 결정하고, AI는 반복 구현 초안을 만들며, 자동 test/eval은 정의된 contract를 반복 검증하도록 세 역할을 명확히 분리한다. |
| T07-P105-O4-129 | T07-P105 완화 | T07-P105/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 직접 실행하는 작은 모델

`TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P105 RECEIVED`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P105";
const stages = ["RECEIVED", "CHECKED", "APPLIED", "DONE"];
const stopAt = 1;
const visited = stages.slice(0, stopAt);
console.log(marker, visited.join("->"));
```

기준 출력: `T07-P105 RECEIVED`.

`TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P105-L1-158 | constmarker="T07-P105"; | T07-P105 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P105-L2-159 | conststages=["RECEIVED","CHECKED","APPLIED","DONE"]; | T07-P105 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P105-L3-160 | conststopAt=1; | T07-P105 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P105-L4-161 | constvisited=stages.slice(0,stopAt); | T07-P105 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P105-L5-162 | console.log(marker,visited.join("->")); | T07-P105 출력 관측점; 예상 `T07-P105 RECEIVED`와 비교 |
| T07-P105-LX-247 | T07-P105 실행 기록 | T07-P105 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 한 부분만 수정하고 다시 예측

수정 과제: **stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다**.

수정 전은 `T07-P105 RECEIVED`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P105/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P105-D1-188 | 기준 `T07-P105 RECEIVED` | T07-P105 수정 전 실행을 먼저 재현 |
| T07-P105-D2-189 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | T07-P105 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P105-D3-190 | T07-P105 새 예측 | T07-P105 실행 전에 출력·상태를 먼저 기록 |
| T07-P105-D4-191 | T07-P105 재실행 | T07-P105/CH10 실제값과 새 예측을 대조 |
| T07-P105-D5-192 | T07-P105 반례 | T07-P105/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P105-D6-193 | T07-P105 근거 | T07-P105/CH08 상태가 설명과 일치해야 완료 |
| T07-P105-D7-194 | T07-P105 이유 | T07-P105 변경 이유를 TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 계약과 연결해 설명 |

## CHAPTER 12 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 | 최종 목표는 framework 이름 암기가 아니라 요청의 수명주기와 상태·신뢰·비동기·장애 경계를 스스로 설명하고 검증하는 것이다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P105/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | 한 요청을 route부터 response/background completion까지 추적하며 각 단계의 contract와 evidence를 연결한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | AI가 만들어 준 코드를 실행 성공만 보고 믿거나, 반대로 모든 저수준 구현을 사람이 직접 작성하려 한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 정상·경계·실패·과부하 case의 실제 실행/관측 증거와 사람이 확인한 invariant를 최종 산출물로 남긴다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P105/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P105/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P105/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P105/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기` 실행 코드 수정 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | `TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P105/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P105/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 최종 목표는 framework 이름 암기가 아니라 요청의 수명주기와 상태·신뢰·비동기·장애 경계를 스스로 설명하고 검증하는 것이다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P105/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P105/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P105/CH10 실행용 boilerplate·test 후보 | T07-P105: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P105 RECEIVED` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P105/CH02의 판단 기준과 T07-P105/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P105-AI1-250 | T07-P105 사람 결정 | T07-P105 업무 의미·허용 위험·완료 기준 소유 |
| T07-P105-AI2-251 | T07-P105 AI 초안 | T07-P105/CH10 boilerplate·test 후보까지만 위임 |
| T07-P105-AI3-252 | T07-P105 검증 | T07-P105/CH06 반례와 T07-P105/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 경계 조합 실험 8개

T07-P105: T07-P105/CH04~T07-P105/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P105의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-K01 | T07-P105: 경계 A | T07-P105: 경계 B | T07-P105: RECURRENCE[occurrence=3; interval_s=142; mitigation_applied=1] / sample=16 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + RECURRENCE_TIMELINE |
| T07-P105-K02 | T07-P105: 경계 A | T07-P105: 경계 C | T07-P105: LARGE_INPUT[body_kb=1496; limit_kb=256; parsed=0] / sample=23 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + SIZE_LIMIT |
| T07-P105-K03 | T07-P105: 경계 A | T07-P105: 경계 D | T07-P105: OWNER_SPOOF[actor=A0; payload_owner=B3; auth_owner=A0] / sample=30 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + AUTHZ_POLICY |
| T07-P105-K04 | T07-P105: 경계 B | T07-P105: 경계 C | T07-P105: REORDER[in_seq=7,5,6; applied_version=4] / sample=37 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + SEQUENCE_STATE |
| T07-P105-K05 | T07-P105: 경계 B | T07-P105: 경계 D | T07-P105: RECOVERY_SCOPE[selected=176; expected=15; backup=1; dry_run=1] / sample=44 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + RECOVERY_AUDIT |
| T07-P105-K06 | T07-P105: 경계 C | T07-P105: 경계 D | T07-P105: REPLAY[key=cmd-105-06; attempts=2; response_seen=0] / sample=51 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + IDEMPOTENCY_RECORD |
| T07-P105-K07 | T07-P105: 경계 A | T07-P105: 경계 B+C | T07-P105: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=58 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + CLIENT_VERSION |
| T07-P105-K08 | T07-P105: 경계 B | T07-P105: 경계 C+D | T07-P105: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=49] / sample=65 | T07-P105: 먼저 깨지는 경계를 판정 | T07-P105: T07-P105/CH08 + STATE_VERSION |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P105/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P105와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-B14 | T07-P105: capstone: 명세에서 실제 backend 설계까지 | T07-P105: 주문 API를 spec부터 설계한다 | T07-P105: contract·state·async·evidence matrix를 만든다 | T07-P105: T07-P105/CH08 증거와 형제 LESSON 증거를 분리 | T07-P105: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P105-B13 | T07-P105: legacy backend를 안전하게 refactor하기 | T07-P105: legacy handler를 전부 다시 쓰려 한다 | T07-P105: characterization test와 작은 seam을 만든다 | T07-P105: T07-P105/CH08 증거와 형제 LESSON 증거를 분리 | T07-P105: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P105-B12 | T07-P105: dependency와 supply-chain 경계 검토 | T07-P105: AI가 새 package 6개를 설치했다 | T07-P105: lockfile·transitive·provenance를 검토한다 | T07-P105: T07-P105/CH08 증거와 형제 LESSON 증거를 분리 | T07-P105: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P105-B11 | T07-P105: cost와 resource budget을 backend 설계에 포함하기 | T07-P105: 외부 API 호출이 요청당 8회다 | T07-P105: resource unit과 cost budget을 계산한다 | T07-P105: T07-P105/CH08 증거와 형제 LESSON 증거를 분리 | T07-P105: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P105-B10 | T07-P105: observability readiness를 코드 리뷰에 포함하기 | T07-P105: dashboard는 있지만 request id가 없다 | T07-P105: instrumentation이 실제 연결되는지 확인한다 | T07-P105: T07-P105/CH08 증거와 형제 LESSON 증거를 분리 | T07-P105: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 선택형 실패 주입 6개

T07-P105: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P105 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-X01 | T07-P105: 소유권 위조 | T07-P105: tenant/owner가 payload에 포함됨; sample=281 | T07-P105: 식별 정보와 권한 근거 | T07-P105: T07-P105/CH02 판단과 별도 기록 | T07-P105: authenticated context·resource owner·policy result |
| T07-P105-X02 | T07-P105: 순서 역전 | T07-P105: event가 원래 순서와 반대로 도착; sample=298 | T07-P105: 수신 순서와 업무 순서 | T07-P105: T07-P105/CH02 판단과 별도 기록 | T07-P105: version/sequence·dedupe id·applied state |
| T07-P105-X03 | T07-P105: 복구 범위 | T07-P105: 복구 script 대상이 예상보다 큼; sample=315 | T07-P105: 진단과 destructive recovery | T07-P105: T07-P105/CH02 판단과 별도 기록 | T07-P105: selected ids/count·backup·audit trail |
| T07-P105-X04 | T07-P105: 재전송 | T07-P105: 응답 유실 뒤 같은 command가 다시 도착함; sample=332 | T07-P105: 중복 side effect 여부 | T07-P105: T07-P105/CH02 판단과 별도 기록 | T07-P105: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P105-X05 | T07-P105: 구버전 client | T07-P105: 한 단계 이전 schema가 요청됨; sample=349 | T07-P105: 호환 입력과 breaking change | T07-P105: T07-P105/CH02 판단과 별도 기록 | T07-P105: schema version·실제 client 분포·contract test |
| T07-P105-X06 | T07-P105: 동시 변경 | T07-P105: 두 actor가 같은 resource를 수정함; sample=366 | T07-P105: lost update 또는 conflict | T07-P105: T07-P105/CH02 판단과 별도 기록 | T07-P105: version·affected rows·lock/wait 기록 |

## CHAPTER 17 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P105에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-E01 | T07-P105: 대표 실패를 원인으로 착각 | T07-P105: T07-P105/CH06 실패 case를 다른 입력으로 재현 | T07-P105: 현상과 원인을 같은 것으로 봄 | T07-P105: T07-P105/CH06 대표 실패와 T07-P105/CH08 증거를 다시 대조 | T07-P105: T07-P105/CH08 |
| T07-P105-E02 | T07-P105: 경계 A 생략 | T07-P105: T07-P105/CH04의 조건 하나를 반대로 설정 | T07-P105: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P105: T07-P105/CH04를 새 입력에 적용 | T07-P105: T07-P105/CH08 |
| T07-P105-E03 | T07-P105: 경계 B 생략 | T07-P105: T07-P105/CH05의 조건 하나를 반대로 설정 | T07-P105: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P105: T07-P105/CH05를 새 입력에 적용 | T07-P105: T07-P105/CH08 |
| T07-P105-E04 | T07-P105: 복구 상태 혼동 | T07-P105: T07-P105/CH07에서 처리 중단을 주입 | T07-P105: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P105: T07-P105/CH07에서 수명 경계를 다시 표시 | T07-P105: T07-P105/CH08 |
| T07-P105-E05 | T07-P105: 운영 한계 누락 | T07-P105: T07-P105/CH09에서 부하 또는 drain 조건을 변경 | T07-P105: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P105: T07-P105/CH09의 종료 조건을 다시 작성 | T07-P105: T07-P105/CH08 |
| T07-P105-E06 | T07-P105: 증거 없는 성공 판정 | T07-P105: T07-P105/CH08에서 증거 하나를 숨김 | T07-P105: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P105: T07-P105/CH08에서 독립 증거 둘을 선택 | T07-P105: T07-P105/CH08 |

## CHAPTER 18 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — synthetic 관측값 판독 6개

T07-P105: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P105의 숫자 하나만으로 원인을 단정하지 않고 T07-P105/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-O01 | T07-P105/changed_files | 611 | T07-P105: 변경 파일 수 | T07-P105: 축=구버전 client; 원인 확정 금지 | T07-P105: CLIENT_VERSION + T07-P105/CH08 |
| T07-P105-O02 | T07-P105/dependency_count | 628 | T07-P105: 추가 dependency 수 | T07-P105: 축=동시 변경; 원인 확정 금지 | T07-P105: STATE_VERSION + T07-P105/CH08 |
| T07-P105-O03 | T07-P105/test_failures | 645 | T07-P105: 실패 test 수 | T07-P105: 축=unknown outcome; 원인 확정 금지 | T07-P105: PROVIDER_RESULT + T07-P105/CH08 |
| T07-P105-O04 | T07-P105/eval_passes | 662 | T07-P105: 통과 eval 수 | T07-P105: 축=과부하; 원인 확정 금지 | T07-P105: QUEUE_PRESSURE + T07-P105/CH08 |
| T07-P105-O05 | T07-P105/p99_ms | 679 | T07-P105: 변경 후 p99 | T07-P105: 축=sampling; 원인 확정 금지 | T07-P105: TRACE_METRIC_CROSSCHECK + T07-P105/CH08 |
| T07-P105-O06 | T07-P105/cost_units | 696 | T07-P105: resource/cost 단위 | T07-P105: 축=AI 복잡도; 원인 확정 금지 | T07-P105: DIFF_EVAL_ROLLBACK + T07-P105/CH08 |

## CHAPTER 19 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 선택형 코드 리뷰 6질문

T07-P105: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P105에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-R01 | T07-P105: 입력 경계 | T07-P105: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P105: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=42] | T07-P105: T07-P105/CH04 | T07-P105: STATE_VERSION |
| T07-P105-R02 | T07-P105: 순서 | T07-P105: old/new event 순서가 바뀌어도 안전한가 | T07-P105: UNKNOWN_OUTCOME[timeout_ms=212; provider_state=UNKNOWN; lookup_id=p10501] | T07-P105: T07-P105/CH05 | T07-P105: PROVIDER_RESULT |
| T07-P105-R03 | T07-P105: retry | T07-P105: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P105: OVERLOAD[rps=569; p99_ms=912; queue=20] | T07-P105: T07-P105/CH06 | T07-P105: QUEUE_PRESSURE |
| T07-P105-R04 | T07-P105: 민감정보 | T07-P105: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P105: SAMPLING[sample_rate=11%; trace_present=1; metric_present=1] | T07-P105: T07-P105/CH07 | T07-P105: TRACE_METRIC_CROSSCHECK |
| T07-P105-R05 | T07-P105: 입력 경계 | T07-P105: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P105: AI_COMPLEXITY[deps_added=1; cache_layer=1; rollback_plan=0] | T07-P105: T07-P105/CH04 | T07-P105: DIFF_EVAL_ROLLBACK |
| T07-P105-R06 | T07-P105: 순서 | T07-P105: old/new event 순서가 바뀌어도 안전한가 | T07-P105: RECURRENCE[occurrence=2; interval_s=186; mitigation_applied=1] | T07-P105: T07-P105/CH05 | T07-P105: RECURRENCE_TIMELINE |

## CHAPTER 20 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P105에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-I01 | T07-P105: 변경 동결 | T07-P105: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P105: UNKNOWN_OUTCOME[timeout_ms=201; provider_state=UNKNOWN; lookup_id=p10500] | T07-P105: T07-P105/CH08 + PROVIDER_RESULT | T07-P105-incident-013 |
| T07-P105-I02 | T07-P105: correlation | T07-P105: 한 request/job/resource id를 시간축에 고정 | T07-P105: OVERLOAD[rps=536; p99_ms=795; queue=26] | T07-P105: T07-P105/CH08 + QUEUE_PRESSURE | T07-P105-incident-014 |
| T07-P105-I03 | T07-P105: 마지막 정상 | T07-P105: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P105: SAMPLING[sample_rate=78%; trace_present=0; metric_present=1] | T07-P105: T07-P105/CH08 + TRACE_METRIC_CROSSCHECK | T07-P105-incident-015 |
| T07-P105-I04 | T07-P105: 가설 검증 | T07-P105: 원인 후보 하나만 뒤집어 재현 | T07-P105: AI_COMPLEXITY[deps_added=4; cache_layer=1; rollback_plan=1] | T07-P105: T07-P105/CH08 + DIFF_EVAL_ROLLBACK | T07-P105-incident-016 |
| T07-P105-I05 | T07-P105: 복구 확인 | T07-P105: durable state와 사용자 결과를 모두 확인 | T07-P105: RECURRENCE[occurrence=6; interval_s=175; mitigation_applied=1] | T07-P105: T07-P105/CH08 + RECURRENCE_TIMELINE | T07-P105-incident-017 |
| T07-P105-I06 | T07-P105: 재주입 | T07-P105: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P105: LARGE_INPUT[body_kb=1760; limit_kb=256; parsed=0] | T07-P105: T07-P105/CH08 + SIZE_LIMIT | T07-P105-incident-018 |

## CHAPTER 21 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P105/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P105/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P105/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P105/CH04~T07-P105/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P105/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P105/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P105/CH18 signal 두 개와 T07-P105/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P105/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P105/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 통합 casebook 16문제

T07-P105 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P105 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P105-C01 | T07-P105: 경계 A | T07-P105: OVERLOAD[rps=503; p99_ms=678; queue=12] | T07-P105: load=445, window=85s | T07-P105: review=민감정보 | T07-P105: 경계 A 위반 여부를 판정 | T07-P105: QUEUE_PRESSURE + T07-P105/CH08 |
| T07-P105-C02 | T07-P105: 경계 B | T07-P105: SAMPLING[sample_rate=65%; trace_present=1; metric_present=1] | T07-P105: load=468, window=14s | T07-P105: review=중복 | T07-P105: 경계 B 위반 여부를 판정 | T07-P105: TRACE_METRIC_CROSSCHECK + T07-P105/CH08 |
| T07-P105-C03 | T07-P105: 경계 C | T07-P105: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] | T07-P105: load=491, window=33s | T07-P105: review=권한 | T07-P105: 경계 C 위반 여부를 판정 | T07-P105: DIFF_EVAL_ROLLBACK + T07-P105/CH08 |
| T07-P105-C04 | T07-P105: 경계 D | T07-P105: RECURRENCE[occurrence=5; interval_s=164; mitigation_applied=1] | T07-P105: load=514, window=52s | T07-P105: review=입력 경계 | T07-P105: 경계 D 위반 여부를 판정 | T07-P105: RECURRENCE_TIMELINE + T07-P105/CH08 |
| T07-P105-C05 | T07-P105: 경계 A | T07-P105: LARGE_INPUT[body_kb=1672; limit_kb=256; parsed=0] | T07-P105: load=537, window=71s | T07-P105: review=timeout | T07-P105: 경계 A 위반 여부를 판정 | T07-P105: SIZE_LIMIT + T07-P105/CH08 |
| T07-P105-C06 | T07-P105: 경계 B | T07-P105: OWNER_SPOOF[actor=A0; payload_owner=B0; auth_owner=A0] | T07-P105: load=560, window=90s | T07-P105: review=자원 | T07-P105: 경계 B 위반 여부를 판정 | T07-P105: AUTHZ_POLICY + T07-P105/CH08 |
| T07-P105-C07 | T07-P105: 경계 C | T07-P105: REORDER[in_seq=9,7,8; applied_version=4] | T07-P105: load=583, window=19s | T07-P105: review=순서 | T07-P105: 경계 C 위반 여부를 판정 | T07-P105: SEQUENCE_STATE + T07-P105/CH08 |
| T07-P105-C08 | T07-P105: 경계 D | T07-P105: RECOVERY_SCOPE[selected=198; expected=22; backup=1; dry_run=1] | T07-P105: load=606, window=38s | T07-P105: review=관측 | T07-P105: 경계 D 위반 여부를 판정 | T07-P105: RECOVERY_AUDIT + T07-P105/CH08 |
| T07-P105-C09 | T07-P105: 경계 A | T07-P105: REPLAY[key=cmd-105-08; attempts=4; response_seen=0] | T07-P105: load=629, window=57s | T07-P105: review=상태 변경 | T07-P105: 경계 A 위반 여부를 판정 | T07-P105: IDEMPOTENCY_RECORD + T07-P105/CH08 |
| T07-P105-C10 | T07-P105: 경계 B | T07-P105: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P105: load=652, window=76s | T07-P105: review=retry | T07-P105: 경계 B 위반 여부를 판정 | T07-P105: CLIENT_VERSION + T07-P105/CH08 |
| T07-P105-C11 | T07-P105: 경계 C | T07-P105: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=75] | T07-P105: load=675, window=95s | T07-P105: review=복구 | T07-P105: 경계 C 위반 여부를 판정 | T07-P105: STATE_VERSION + T07-P105/CH08 |
| T07-P105-C12 | T07-P105: 경계 D | T07-P105: UNKNOWN_OUTCOME[timeout_ms=111; provider_state=UNKNOWN; lookup_id=p10511] | T07-P105: load=698, window=24s | T07-P105: review=동시성 | T07-P105: 경계 D 위반 여부를 판정 | T07-P105: PROVIDER_RESULT + T07-P105/CH08 |
| T07-P105-C13 | T07-P105: 경계 A | T07-P105: OVERLOAD[rps=266; p99_ms=336; queue=26] | T07-P105: load=721, window=43s | T07-P105: review=민감정보 | T07-P105: 경계 A 위반 여부를 판정 | T07-P105: QUEUE_PRESSURE + T07-P105/CH08 |
| T07-P105-C14 | T07-P105: 경계 B | T07-P105: SAMPLING[sample_rate=27%; trace_present=1; metric_present=1] | T07-P105: load=744, window=62s | T07-P105: review=중복 | T07-P105: 경계 B 위반 여부를 판정 | T07-P105: TRACE_METRIC_CROSSCHECK + T07-P105/CH08 |
| T07-P105-C15 | T07-P105: 경계 C | T07-P105: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] | T07-P105: load=767, window=81s | T07-P105: review=권한 | T07-P105: 경계 C 위반 여부를 판정 | T07-P105: DIFF_EVAL_ROLLBACK + T07-P105/CH08 |
| T07-P105-C16 | T07-P105: 경계 D | T07-P105: RECURRENCE[occurrence=2; interval_s=85; mitigation_applied=1] | T07-P105: load=790, window=10s | T07-P105: review=입력 경계 | T07-P105: 경계 D 위반 여부를 판정 | T07-P105: RECURRENCE_TIMELINE + T07-P105/CH08 |

채점은 결론보다 근거를 본다. T07-P105/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — evidence 판독 문제 14개

T07-P105 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P105 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P105-V01 | T07-P105: latency=725ms; queue=15; retry=0 | T07-P105: sampling | T07-P105: 축=sampling; 원인 확정은 보류 | T07-P105: TRACE_METRIC_CROSSCHECK + T07-P105/CH08 | T07-P105: 피할 오판=동시성 무시 |
| T07-P105-V02 | T07-P105: latency=792ms; queue=26; retry=3 | T07-P105: AI 복잡도 | T07-P105: 축=AI 복잡도; 원인 확정은 보류 | T07-P105: DIFF_EVAL_ROLLBACK + T07-P105/CH08 | T07-P105: 피할 오판=운영 한계 누락 |
| T07-P105-V03 | T07-P105: latency=859ms; queue=37; retry=6 | T07-P105: 재발 | T07-P105: 축=재발; 원인 확정은 보류 | T07-P105: RECURRENCE_TIMELINE + T07-P105/CH08 | T07-P105: 피할 오판=오류 합치기 |
| T07-P105-V04 | T07-P105: latency=926ms; queue=48; retry=2 | T07-P105: 대형 입력 | T07-P105: 축=대형 입력; 원인 확정은 보류 | T07-P105: SIZE_LIMIT + T07-P105/CH08 | T07-P105: 피할 오판=AI 과신 |
| T07-P105-V05 | T07-P105: latency=993ms; queue=59; retry=5 | T07-P105: 소유권 위조 | T07-P105: 축=소유권 위조; 원인 확정은 보류 | T07-P105: AUTHZ_POLICY + T07-P105/CH08 | T07-P105: 피할 오판=복구 과잉 |
| T07-P105-V06 | T07-P105: latency=1060ms; queue=70; retry=1 | T07-P105: 순서 역전 | T07-P105: 축=순서 역전; 원인 확정은 보류 | T07-P105: SEQUENCE_STATE + T07-P105/CH08 | T07-P105: 피할 오판=잘못된 전제 |
| T07-P105-V07 | T07-P105: latency=1127ms; queue=1; retry=4 | T07-P105: 복구 범위 | T07-P105: 축=복구 범위; 원인 확정은 보류 | T07-P105: RECOVERY_AUDIT + T07-P105/CH08 | T07-P105: 피할 오판=경계 누락 |
| T07-P105-V08 | T07-P105: latency=1194ms; queue=12; retry=0 | T07-P105: 재전송 | T07-P105: 축=재전송; 원인 확정은 보류 | T07-P105: IDEMPOTENCY_RECORD + T07-P105/CH08 | T07-P105: 피할 오판=증거 혼동 |
| T07-P105-V09 | T07-P105: latency=1261ms; queue=23; retry=3 | T07-P105: 구버전 client | T07-P105: 축=구버전 client; 원인 확정은 보류 | T07-P105: CLIENT_VERSION + T07-P105/CH08 | T07-P105: 피할 오판=재시도 오판 |
| T07-P105-V10 | T07-P105: latency=1328ms; queue=34; retry=6 | T07-P105: 동시 변경 | T07-P105: 축=동시 변경; 원인 확정은 보류 | T07-P105: STATE_VERSION + T07-P105/CH08 | T07-P105: 피할 오판=소유권 혼동 |
| T07-P105-V11 | T07-P105: latency=1395ms; queue=45; retry=2 | T07-P105: unknown outcome | T07-P105: 축=unknown outcome; 원인 확정은 보류 | T07-P105: PROVIDER_RESULT + T07-P105/CH08 | T07-P105: 피할 오판=동시성 무시 |
| T07-P105-V12 | T07-P105: latency=1462ms; queue=56; retry=5 | T07-P105: 과부하 | T07-P105: 축=과부하; 원인 확정은 보류 | T07-P105: QUEUE_PRESSURE + T07-P105/CH08 | T07-P105: 피할 오판=운영 한계 누락 |
| T07-P105-V13 | T07-P105: latency=1529ms; queue=67; retry=1 | T07-P105: sampling | T07-P105: 축=sampling; 원인 확정은 보류 | T07-P105: TRACE_METRIC_CROSSCHECK + T07-P105/CH08 | T07-P105: 피할 오판=오류 합치기 |
| T07-P105-V14 | T07-P105: latency=1596ms; queue=78; retry=4 | T07-P105: AI 복잡도 | T07-P105: 축=AI 복잡도; 원인 확정은 보류 | T07-P105: DIFF_EVAL_ROLLBACK + T07-P105/CH08 | T07-P105: 피할 오판=AI 과신 |

## CHAPTER 24 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P105에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P105-D01 | T07-P105: validation 이동 | T07-P105: validation을 business side effect 뒤로 옮긴다 | T07-P105: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P105: RECURRENCE[occurrence=2; interval_s=131; mitigation_applied=1] | T07-P105: RECURRENCE_TIMELINE + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P105-D02 | T07-P105: batch 확대 | T07-P105: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P105: memory·deadline·부분 실패 범위가 커지는지 | T07-P105: LARGE_INPUT[body_kb=1408; limit_kb=256; parsed=0] | T07-P105: SIZE_LIMIT + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P105-D03 | T07-P105: fallback 추가 | T07-P105: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P105: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P105: OWNER_SPOOF[actor=A0; payload_owner=B2; auth_owner=A0] | T07-P105: AUTHZ_POLICY + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P105-D04 | T07-P105: 외부 호출 이동 | T07-P105: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P105: lock duration과 unknown outcome 경계가 달라지는지 | T07-P105: REORDER[in_seq=6,4,5; applied_version=4] | T07-P105: SEQUENCE_STATE + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P105-D05 | T07-P105: cache 추가 | T07-P105: 현재 결과 앞에 cache layer를 추가한다 | T07-P105: stale·key·invalidation 책임이 새로 생기는지 | T07-P105: RECOVERY_SCOPE[selected=165; expected=23; backup=1; dry_run=0] | T07-P105: RECOVERY_AUDIT + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P105-D06 | T07-P105: pool 확대 | T07-P105: connection/worker pool 상한을 늘린다 | T07-P105: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P105: REPLAY[key=cmd-105-05; attempts=4; response_seen=0] | T07-P105: IDEMPOTENCY_RECORD + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P105-D07 | T07-P105: schema 변경 | T07-P105: 필드 이름·형식·required 조건을 바꾼다 | T07-P105: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P105: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P105: CLIENT_VERSION + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P105-D08 | T07-P105: 결과 합치기 | T07-P105: 여러 오류를 하나의 status/error code로 합친다 | T07-P105: client 행동과 retry 가능성을 잃지 않는지 | T07-P105: CONCURRENT_WRITE[actors=2; base_version=7; writes=2; gap_ms=36] | T07-P105: STATE_VERSION + T07-P105/CH08 | T07-P105: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P105: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기 — 최종 contract와 evidence spine

**최종 계약:** 최종 목표는 framework 이름 암기가 아니라 요청의 수명주기와 상태·신뢰·비동기·장애 경계를 스스로 설명하고 검증하는 것이다.

**정상 메커니즘:** 한 요청을 route부터 response/background completion까지 추적하며 각 단계의 contract와 evidence를 연결한다.

**대표 실패:** AI가 만들어 준 코드를 실행 성공만 보고 믿거나, 반대로 모든 저수준 구현을 사람이 직접 작성하려 한다.

**검증 evidence:** 정상·경계·실패·과부하 case의 실제 실행/관측 증거와 사람이 확인한 invariant를 최종 산출물로 남긴다.

**직접 행동:** 종합 시나리오에서 어느 단계가 사람 판단, AI 생성, 자동 검증 대상인지 분류한다.

**다음 연결:** `TRACK 07 종합 복습`.

`TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기의 개념·실패·운영 판단 교차 확인 |
| OWASP-API | OWASP-API | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기의 개념·실패·운영 판단 교차 확인 |

`TRACK 07 최종: 요청 하나를 설계·실행·고장내고 설명하기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
