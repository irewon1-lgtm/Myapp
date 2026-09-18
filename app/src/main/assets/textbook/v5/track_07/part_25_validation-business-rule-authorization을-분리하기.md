# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 02 · 업무 규칙과 상태 변화가 깨지지 않게 만들기

### LESSON 10 · validation·business rule·authorization을 분리하기

## CHAPTER 01 · validation·business rule·authorization을 분리하기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 문법적으로 올바른 값, 업무상 가능한 행동, 그 사용자가 할 수 있는 행동은 서로 다른 질문이다.

**아주 쉬운 사건.** 형식은 맞지만 품절이고 다른 사용자 주문이다. 이 사건에서는 먼저 **validation·rule·authorization을 나눈다**.

**왜 필요한가.** 정상 동작은 schema validation 뒤에 domain rule을 적용하고 별도로 actor-resource-action 권한을 확인한다. 반대로 권한 실패를 validation으로 숨기거나 business rule을 client가 보내 준 flag로 결정한다.

**암기:** `validation·business rule·authorization을 분리하기`의 역할 한 줄.

**직접 이해:** `validation·business rule·authorization을 분리하기`의 입력·상태·결과 경계.

**AI 위임 가능:** `validation·business rule·authorization을 분리하기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `형식은 맞지만 품절이고 다른 사용자 주문이다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 각 단계의 실패 종류와 공개 응답을 분리해 기록한다.

## CHAPTER 02 · validation·business rule·authorization을 분리하기 — 아주 쉬운 예를 한 단계씩 해석

T07-P25: `형식은 맞지만 품절이고 다른 사용자 주문이다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 형식은 맞지만 품절이고 다른 사용자 주문이다 | T07-P25 외부 입력 | T07-P25: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | validation·rule·authorization을 나눈다 | T07-P25 판단 기준 | T07-P25/CH08 관측표와 대조 |
| 정상 경로 | T07-P25/CH03 M1→M5 | validation·business rule·authorization을 분리하기: 완료 시점을 단계별로 분리 | T07-P25: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P25/CH06 권한 실패를 validation으로 숨기거나 business rule을 client가 보내 준 flag로 결정한다. | T07-P25: 깨진 계약 하나를 특정 | validation·business rule·authorization을 분리하기: 증상과 원인을 분리 |
| 재검증 | T07-P25/CH10 직접 실행 | T07-P25: 예상값 T07-P025 90 기록 | validation·business rule·authorization을 분리하기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P25/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P25/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · validation·business rule·authorization을 분리하기 — 내부 메커니즘과 상태 전이

schema validation 뒤에 domain rule을 적용하고 별도로 actor-resource-action 권한을 확인한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 형식은 맞지만 품절이고 다른 사용자 주문이다 | source/actor/size를 보존 |
| M2 | 경계 판단 | validation·rule·authorization을 나눈다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | schema validation 뒤에 domain rule을 적용하고 별도로 actor-resource-action 권한을 확인한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 각 단계의 실패 종류와 공개 응답을 분리해 기록한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 세 단계 판정 결과를 조합해 최초 거부 이유를 선택한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`validation·business rule·authorization을 분리하기` 흐름을 framework 이름 없이 설명한다.

막히면 `형식은 맞지만 품절이고 다른 사용자 주문이다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · validation·business rule·authorization을 분리하기 — 실전 경계 A

**경계 A.** schema validation은 ‘형식이 맞는가’, business rule은 ‘현재 상태에서 가능한가’, authorization은 ‘이 actor가 이 resource에 해도 되는가’를 묻는 서로 다른 gate다.

T07-P25/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P25에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P25/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P25/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P25): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P25/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P25-A1-275 | T07-P25 조건 | schema validation은 ‘형식이 맞는가’, business rule은 ‘현재 상태에서 가능한가’, authorization은 ‘이 actor가 이 resource에 해도 되는가’를 묻는 서로 다른 gate다. |
| T07-P25-A2-276 | T07-P25 변화점 | T07-P25/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P25-A3-277 | T07-P25 반례 | T07-P25/CH06 대표 실패와 A 위반을 구별 |
| T07-P25-A4-278 | T07-P25 근거 | T07-P25/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P25-A5-279 | T07-P25 재실험 | 형식은 맞지만 품절이고 다른 사용자 주문이다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · validation·business rule·authorization을 분리하기 — 실전 경계 B

**경계 B.** 어떤 gate를 먼저 평가하느냐가 resource 존재를 노출할 수 있어 보안상 민감한 endpoint는 404/403 정책과 오류 세부를 일관되게 정한다.

T07-P25/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P25에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P25/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P25/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P25): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P25/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P25-B1-306 | T07-P25 조건 | 어떤 gate를 먼저 평가하느냐가 resource 존재를 노출할 수 있어 보안상 민감한 endpoint는 404/403 정책과 오류 세부를 일관되게 정한다. |
| T07-P25-B2-307 | T07-P25 독립성 | T07-P25/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P25-B3-308 | T07-P25 상태 | T07-P25/CH03 before·after 위치를 다시 지정 |
| T07-P25-B4-309 | T07-P25 반증 | T07-P25/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P25-B5-310 | T07-P25 적용 | validation·business rule·authorization을 분리하기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · validation·business rule·authorization을 분리하기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **권한 실패를 validation으로 숨기거나 business rule을 client가 보내 준 flag로 결정한다.**

아래 여섯 사례는 T07-P25의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P25-F01 | T07-P25: 대표 실패 | T07-P25: T07-P25/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P25: 현상만 보고 원인을 확정 | T07-P25/CH08 evidence map에서 상태를 대조 |
| T07-P25-F02 | T07-P25: 경계 A 누락 | T07-P25: T07-P25/CH04 경계 A 위반 입력 | T07-P25: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P25/CH08 evidence map에서 상태를 대조 |
| T07-P25-F03 | T07-P25: 경계 B 누락 | T07-P25: T07-P25/CH05 경계 B 위반 입력 | T07-P25: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P25/CH08 evidence map에서 상태를 대조 |
| T07-P25-F04 | T07-P25: 복구 경계 C 누락 | T07-P25: T07-P25/CH07 경계 C 복구 조건 | T07-P25: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P25/CH08 evidence map에서 상태를 대조 |
| T07-P25-F05 | T07-P25: 운영 경계 D 누락 | T07-P25: T07-P25/CH09 경계 D 운영 조건 | T07-P25: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P25/CH08 evidence map에서 상태를 대조 |
| T07-P25-F06 | T07-P25: 증거 없는 결론 | T07-P25: T07-P25/CH02 첫 판단만 존재 | T07-P25: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P25/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P25/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · validation·business rule·authorization을 분리하기 — 복구 가능한 상태와 수명

**경계 C.** client가 admin=true나 price 같은 privileged field를 보냈다는 이유로 업무 권한·가격을 믿지 않고 server-owned source에서 다시 계산한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P25에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P25/CH08 evidence map을 본다. 복구 후에는 T07-P25/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P25에서 이미 확정된 side effect는 T07-P25/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P25-R1-368 | T07-P25 중단 직전 | T07-P25/CH03에서 이미 확정된 상태만 표시 |
| T07-P25-R2-369 | T07-P25 재시작 직후 | client가 admin=true나 price 같은 privileged field를 보냈다는 이유로 업무 권한·가격을 믿지 않고 server-owned source에서 다시 계산한다. |
| T07-P25-R3-370 | T07-P25 재검증 | T07-P25/CH08 근거로 중복·누락 여부 확인 |
| T07-P25-R4-371 | T07-P25 재실행 | T07-P25/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · validation·business rule·authorization을 분리하기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 형식은 맞지만 품절이고 다른 사용자 주문이다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | validation·rule·authorization을 나눈다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | schema validation 뒤에 domain rule을 적용하고 별도로 actor-resource-action 권한을 확인한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 권한 실패를 validation으로 숨기거나 business rule을 client가 보내 준 flag로 결정한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 각 단계의 실패 종류와 공개 응답을 분리해 기록한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`validation·business rule·authorization을 분리하기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · validation·business rule·authorization을 분리하기 — 운영 한계와 종료 조건

**경계 D.** HTTP API 외에 worker·admin job 같은 다른 entry point가 같은 use case를 호출한다면 business invariant와 authorization 책임이 어디서 적용되는지 경계를 명시한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P25/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P25/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P25 과제: 경계 D와 T07-P25/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P25-O1-430 | synthetic-load=110 | T07-P25/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P25-O2-431 | synthetic-budget=580ms | T07-P25 timeout과 unknown outcome을 분리 |
| T07-P25-O3-432 | T07-P25 종료 | HTTP API 외에 worker·admin job 같은 다른 entry point가 같은 use case를 호출한다면 business invariant와 authorization 책임이 어디서 적용되는지 경계를 명시한다. |
| T07-P25-O4-433 | T07-P25 완화 | T07-P25/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · validation·business rule·authorization을 분리하기 — 직접 실행하는 작은 모델

`validation·business rule·authorization을 분리하기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P025 90`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P025";
let remaining = 500;
for (const cost of [140, 180, 90]) remaining -= cost;
console.log(marker, remaining);
```

기준 출력: `T07-P025 90`.

`validation·business rule·authorization을 분리하기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P25-L1-462 | constmarker="T07-P025"; | T07-P25 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P25-L2-463 | letremaining=500; | T07-P25 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P25-L3-464 | for(constcostof[140,180,90])remaining-=cost; | T07-P25 계산 지점; 수정 전후 결과가 갈리는 위치 |
| T07-P25-L4-465 | console.log(marker,remaining); | T07-P25 출력 관측점; 예상 `T07-P025 90`와 비교 |
| T07-P25-LX-551 | T07-P25 실행 기록 | T07-P25 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · validation·business rule·authorization을 분리하기 — 한 부분만 수정하고 다시 예측

수정 과제: **두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다**.

수정 전은 `T07-P025 90`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P25/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P25-D1-492 | 기준 `T07-P025 90` | T07-P25 수정 전 실행을 먼저 재현 |
| T07-P25-D2-493 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | T07-P25 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P25-D3-494 | T07-P25 새 예측 | T07-P25 실행 전에 출력·상태를 먼저 기록 |
| T07-P25-D4-495 | T07-P25 재실행 | T07-P25/CH10 실제값과 새 예측을 대조 |
| T07-P25-D5-496 | T07-P25 반례 | T07-P25/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P25-D6-497 | T07-P25 근거 | T07-P25/CH08 상태가 설명과 일치해야 완료 |
| T07-P25-D7-498 | T07-P25 이유 | T07-P25 변경 이유를 validation·business rule·authorization을 분리하기 계약과 연결해 설명 |

## CHAPTER 12 · validation·business rule·authorization을 분리하기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: validation·business rule·authorization을 분리하기 | 문법적으로 올바른 값, 업무상 가능한 행동, 그 사용자가 할 수 있는 행동은 서로 다른 질문이다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P25/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | schema validation 뒤에 domain rule을 적용하고 별도로 actor-resource-action 권한을 확인한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 권한 실패를 validation으로 숨기거나 business rule을 client가 보내 준 flag로 결정한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 각 단계의 실패 종류와 공개 응답을 분리해 기록한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P25/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P25/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P25/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P25/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `validation·business rule·authorization을 분리하기` 실행 코드 수정 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | `validation·business rule·authorization을 분리하기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P25/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P25/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · validation·business rule·authorization을 분리하기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 문법적으로 올바른 값, 업무상 가능한 행동, 그 사용자가 할 수 있는 행동은 서로 다른 질문이다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P25/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P25/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P25/CH10 실행용 boilerplate·test 후보 | T07-P25: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P025 90` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P25/CH02의 판단 기준과 T07-P25/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P25-AI1-554 | T07-P25 사람 결정 | T07-P25 업무 의미·허용 위험·완료 기준 소유 |
| T07-P25-AI2-555 | T07-P25 AI 초안 | T07-P25/CH10 boilerplate·test 후보까지만 위임 |
| T07-P25-AI3-556 | T07-P25 검증 | T07-P25/CH06 반례와 T07-P25/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · validation·business rule·authorization을 분리하기 — 경계 조합 실험 8개

T07-P25: T07-P25/CH04~T07-P25/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P25의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-K01 | T07-P25: 경계 A | T07-P25: 경계 B | T07-P25: DISCONNECT[disconnect_ms=73; commit_state=UNKNOWN; request=025-01] / sample=26 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + COMMIT_TIMELINE |
| T07-P25-K02 | T07-P25: 경계 A | T07-P25: 경계 C | T07-P25: RECURRENCE[occurrence=4; interval_s=154; mitigation_applied=1] / sample=33 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + RECURRENCE_TIMELINE |
| T07-P25-K03 | T07-P25: 경계 A | T07-P25: 경계 D | T07-P25: LARGE_INPUT[body_kb=1592; limit_kb=256; parsed=0] / sample=40 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + SIZE_LIMIT |
| T07-P25-K04 | T07-P25: 경계 B | T07-P25: 경계 C | T07-P25: REORDER[in_seq=7,5,6; applied_version=2] / sample=47 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + SEQUENCE_STATE |
| T07-P25-K05 | T07-P25: 경계 B | T07-P25: 경계 D | T07-P25: RECOVERY_SCOPE[selected=177; expected=13; backup=1; dry_run=1] / sample=54 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + RECOVERY_AUDIT |
| T07-P25-K06 | T07-P25: 경계 C | T07-P25: 경계 D | T07-P25: REPLAY[key=cmd-025-06; attempts=2; response_seen=0] / sample=61 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + IDEMPOTENCY_RECORD |
| T07-P25-K07 | T07-P25: 경계 A | T07-P25: 경계 B+C | T07-P25: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=68 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + CLIENT_VERSION |
| T07-P25-K08 | T07-P25: 경계 B | T07-P25: 경계 C+D | T07-P25: CONCURRENT_WRITE[actors=2; base_version=8; writes=2; gap_ms=47] / sample=75 | T07-P25: 먼저 깨지는 경계를 판정 | T07-P25: T07-P25/CH08 + STATE_VERSION |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P25/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · validation·business rule·authorization을 분리하기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P25와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-B09 | T07-P25: 동시 수정에서 lost update를 막기 | T07-P25: 두 요청이 version=4를 읽고 수정한다 | T07-P25: compare-and-swap으로 충돌을 감지한다 | T07-P25: T07-P25/CH08 증거와 형제 LESSON 증거를 분리 | T07-P25: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P25-B11 | T07-P25: side effect와 outbox 사고방식 | T07-P25: DB commit 직후 process가 죽는다 | T07-P25: outbox가 발행 의도를 남기는지 본다 | T07-P25: T07-P25/CH08 증거와 형제 LESSON 증거를 분리 | T07-P25: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P25-B08 | T07-P25: transaction boundary를 use case에 맞추기 | T07-P25: 주문 row와 item row 중 하나만 저장된다 | T07-P25: transaction boundary를 업무 단위로 잡는다 | T07-P25: T07-P25/CH08 증거와 형제 LESSON 증거를 분리 | T07-P25: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P25-B12 | T07-P25: 외부 API를 adapter 뒤에 격리하기 | T07-P25: 외부 결제사가 새 error code를 보낸다 | T07-P25: adapter가 내부 오류로 번역한다 | T07-P25: T07-P25/CH08 증거와 형제 LESSON 증거를 분리 | T07-P25: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P25-B07 | T07-P25: 식별자와 public id를 설계하기 | T07-P25: public id는 랜덤이지만 다른 사용자 resource다 | T07-P25: 식별과 권한을 분리한다 | T07-P25: T07-P25/CH08 증거와 형제 LESSON 증거를 분리 | T07-P25: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · validation·business rule·authorization을 분리하기 — 선택형 실패 주입 6개

T07-P25: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P25 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-X01 | T07-P25: 대형 입력 | T07-P25: 입력 크기가 정상의 100배; sample=792 | T07-P25: 의미 검증과 resource limit | T07-P25: T07-P25/CH02 판단과 별도 기록 | T07-P25: body/batch size·parse time·memory·reject status |
| T07-P25-X02 | T07-P25: 순서 역전 | T07-P25: event가 원래 순서와 반대로 도착; sample=809 | T07-P25: 수신 순서와 업무 순서 | T07-P25: T07-P25/CH02 판단과 별도 기록 | T07-P25: version/sequence·dedupe id·applied state |
| T07-P25-X03 | T07-P25: 복구 범위 | T07-P25: 복구 script 대상이 예상보다 큼; sample=826 | T07-P25: 진단과 destructive recovery | T07-P25: T07-P25/CH02 판단과 별도 기록 | T07-P25: selected ids/count·backup·audit trail |
| T07-P25-X04 | T07-P25: 재전송 | T07-P25: 응답 유실 뒤 같은 command가 다시 도착함; sample=843 | T07-P25: 중복 side effect 여부 | T07-P25: T07-P25/CH02 판단과 별도 기록 | T07-P25: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P25-X05 | T07-P25: 구버전 client | T07-P25: 한 단계 이전 schema가 요청됨; sample=860 | T07-P25: 호환 입력과 breaking change | T07-P25: T07-P25/CH02 판단과 별도 기록 | T07-P25: schema version·실제 client 분포·contract test |
| T07-P25-X06 | T07-P25: 동시 변경 | T07-P25: 두 actor가 같은 resource를 수정함; sample=877 | T07-P25: lost update 또는 conflict | T07-P25: T07-P25/CH02 판단과 별도 기록 | T07-P25: version·affected rows·lock/wait 기록 |

## CHAPTER 17 · validation·business rule·authorization을 분리하기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P25에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-E01 | T07-P25: 대표 실패를 원인으로 착각 | T07-P25: T07-P25/CH06 실패 case를 다른 입력으로 재현 | T07-P25: 현상과 원인을 같은 것으로 봄 | T07-P25: T07-P25/CH06 대표 실패와 T07-P25/CH08 증거를 다시 대조 | T07-P25: T07-P25/CH08 |
| T07-P25-E02 | T07-P25: 경계 A 생략 | T07-P25: T07-P25/CH04의 조건 하나를 반대로 설정 | T07-P25: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P25: T07-P25/CH04를 새 입력에 적용 | T07-P25: T07-P25/CH08 |
| T07-P25-E03 | T07-P25: 경계 B 생략 | T07-P25: T07-P25/CH05의 조건 하나를 반대로 설정 | T07-P25: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P25: T07-P25/CH05를 새 입력에 적용 | T07-P25: T07-P25/CH08 |
| T07-P25-E04 | T07-P25: 복구 상태 혼동 | T07-P25: T07-P25/CH07에서 처리 중단을 주입 | T07-P25: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P25: T07-P25/CH07에서 수명 경계를 다시 표시 | T07-P25: T07-P25/CH08 |
| T07-P25-E05 | T07-P25: 운영 한계 누락 | T07-P25: T07-P25/CH09에서 부하 또는 drain 조건을 변경 | T07-P25: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P25: T07-P25/CH09의 종료 조건을 다시 작성 | T07-P25: T07-P25/CH08 |
| T07-P25-E06 | T07-P25: 증거 없는 성공 판정 | T07-P25: T07-P25/CH08에서 증거 하나를 숨김 | T07-P25: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P25: T07-P25/CH08에서 독립 증거 둘을 선택 | T07-P25: T07-P25/CH08 |

## CHAPTER 18 · validation·business rule·authorization을 분리하기 — synthetic 관측값 판독 6개

T07-P25: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P25의 숫자 하나만으로 원인을 단정하지 않고 T07-P25/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-O01 | T07-P25/commands_received | 568 | T07-P25: command 수신 수 | T07-P25: 축=구버전 client; 원인 확정 금지 | T07-P25: CLIENT_VERSION + T07-P25/CH08 |
| T07-P25-O02 | T07-P25/state_version | 585 | T07-P25: 상태 버전 | T07-P25: 축=동시 변경; 원인 확정 금지 | T07-P25: STATE_VERSION + T07-P25/CH08 |
| T07-P25-O03 | T07-P25/conflicts | 602 | T07-P25: 동시 수정 충돌 수 | T07-P25: 축=unknown outcome; 원인 확정 금지 | T07-P25: PROVIDER_RESULT + T07-P25/CH08 |
| T07-P25-O04 | T07-P25/retries | 619 | T07-P25: 재처리 수 | T07-P25: 축=재시작; 원인 확정 금지 | T07-P25: DURABLE_STATE + T07-P25/CH08 |
| T07-P25-O05 | T07-P25/side_effects | 636 | T07-P25: 외부 side effect 수 | T07-P25: 축=과부하; 원인 확정 금지 | T07-P25: QUEUE_PRESSURE + T07-P25/CH08 |
| T07-P25-O06 | T07-P25/invariant_violations | 653 | T07-P25: invariant 위반 수 | T07-P25: 축=sampling; 원인 확정 금지 | T07-P25: TRACE_METRIC_CROSSCHECK + T07-P25/CH08 |

## CHAPTER 19 · validation·business rule·authorization을 분리하기 — 선택형 코드 리뷰 6질문

T07-P25: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P25에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-R01 | T07-P25: 동시성 | T07-P25: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P25: CONCURRENT_WRITE[actors=2; base_version=8; writes=2; gap_ms=40] | T07-P25: T07-P25/CH04 | T07-P25: STATE_VERSION |
| T07-P25-R02 | T07-P25: 권한 | T07-P25: actor·action·resource가 같은 판단 안에 있는가 | T07-P25: UNKNOWN_OUTCOME[timeout_ms=213; provider_state=UNKNOWN; lookup_id=p02501] | T07-P25: T07-P25/CH05 | T07-P25: PROVIDER_RESULT |
| T07-P25-R03 | T07-P25: 자원 | T07-P25: pool·queue·memory·connection 상한이 있는가 | T07-P25: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P25: T07-P25/CH06 | T07-P25: DURABLE_STATE |
| T07-P25-R04 | T07-P25: 상태 변경 | T07-P25: side effect가 어느 줄에서 확정되는가 | T07-P25: OVERLOAD[rps=605; p99_ms=1011; queue=32] | T07-P25: T07-P25/CH07 | T07-P25: QUEUE_PRESSURE |
| T07-P25-R05 | T07-P25: 동시성 | T07-P25: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P25: SAMPLING[sample_rate=22%; trace_present=0; metric_present=1] | T07-P25: T07-P25/CH04 | T07-P25: TRACE_METRIC_CROSSCHECK |
| T07-P25-R06 | T07-P25: 권한 | T07-P25: actor·action·resource가 같은 판단 안에 있는가 | T07-P25: DISCONNECT[disconnect_ms=28; commit_state=UNKNOWN; request=025-05] | T07-P25: T07-P25/CH05 | T07-P25: COMMIT_TIMELINE |

## CHAPTER 20 · validation·business rule·authorization을 분리하기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P25에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-I01 | T07-P25: 변경 동결 | T07-P25: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P25: UNKNOWN_OUTCOME[timeout_ms=202; provider_state=UNKNOWN; lookup_id=p02500] | T07-P25: T07-P25/CH08 + PROVIDER_RESULT | T07-P25-incident-475 |
| T07-P25-I02 | T07-P25: correlation | T07-P25: 한 request/job/resource id를 시간축에 고정 | T07-P25: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P25: T07-P25/CH08 + DURABLE_STATE | T07-P25-incident-476 |
| T07-P25-I03 | T07-P25: 마지막 정상 | T07-P25: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P25: OVERLOAD[rps=572; p99_ms=894; queue=18] | T07-P25: T07-P25/CH08 + QUEUE_PRESSURE | T07-P25-incident-477 |
| T07-P25-I04 | T07-P25: 가설 검증 | T07-P25: 원인 후보 하나만 뒤집어 재현 | T07-P25: SAMPLING[sample_rate=89%; trace_present=1; metric_present=1] | T07-P25: T07-P25/CH08 + TRACE_METRIC_CROSSCHECK | T07-P25-incident-478 |
| T07-P25-I05 | T07-P25: 복구 확인 | T07-P25: durable state와 사용자 결과를 모두 확인 | T07-P25: DISCONNECT[disconnect_ms=112; commit_state=UNKNOWN; request=025-04] | T07-P25: T07-P25/CH08 + COMMIT_TIMELINE | T07-P25-incident-479 |
| T07-P25-I06 | T07-P25: 재주입 | T07-P25: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P25: RECURRENCE[occurrence=2; interval_s=187; mitigation_applied=1] | T07-P25: T07-P25/CH08 + RECURRENCE_TIMELINE | T07-P25-incident-480 |

## CHAPTER 21 · validation·business rule·authorization을 분리하기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P25/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P25/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P25/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P25/CH04~T07-P25/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P25/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P25/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P25/CH18 signal 두 개와 T07-P25/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P25/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P25/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · validation·business rule·authorization을 분리하기 — 통합 casebook 16문제

T07-P25 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P25 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P25-C01 | T07-P25: 경계 A | T07-P25: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P25: load=825, window=75s | T07-P25: review=timeout | T07-P25: 경계 A 위반 여부를 판정 | T07-P25: DURABLE_STATE + T07-P25/CH08 |
| T07-P25-C02 | T07-P25: 경계 B | T07-P25: OVERLOAD[rps=539; p99_ms=777; queue=24] | T07-P25: load=848, window=94s | T07-P25: review=자원 | T07-P25: 경계 B 위반 여부를 판정 | T07-P25: QUEUE_PRESSURE + T07-P25/CH08 |
| T07-P25-C03 | T07-P25: 경계 C | T07-P25: SAMPLING[sample_rate=76%; trace_present=0; metric_present=1] | T07-P25: load=871, window=23s | T07-P25: review=순서 | T07-P25: 경계 C 위반 여부를 판정 | T07-P25: TRACE_METRIC_CROSSCHECK + T07-P25/CH08 |
| T07-P25-C04 | T07-P25: 경계 D | T07-P25: DISCONNECT[disconnect_ms=99; commit_state=UNKNOWN; request=025-03] | T07-P25: load=894, window=42s | T07-P25: review=관측 | T07-P25: 경계 D 위반 여부를 판정 | T07-P25: COMMIT_TIMELINE + T07-P25/CH08 |
| T07-P25-C05 | T07-P25: 경계 A | T07-P25: RECURRENCE[occurrence=6; interval_s=176; mitigation_applied=1] | T07-P25: load=917, window=61s | T07-P25: review=상태 변경 | T07-P25: 경계 A 위반 여부를 판정 | T07-P25: RECURRENCE_TIMELINE + T07-P25/CH08 |
| T07-P25-C06 | T07-P25: 경계 B | T07-P25: LARGE_INPUT[body_kb=1768; limit_kb=256; parsed=0] | T07-P25: load=940, window=80s | T07-P25: review=retry | T07-P25: 경계 B 위반 여부를 판정 | T07-P25: SIZE_LIMIT + T07-P25/CH08 |
| T07-P25-C07 | T07-P25: 경계 C | T07-P25: REORDER[in_seq=9,7,8; applied_version=2] | T07-P25: load=963, window=99s | T07-P25: review=복구 | T07-P25: 경계 C 위반 여부를 판정 | T07-P25: SEQUENCE_STATE + T07-P25/CH08 |
| T07-P25-C08 | T07-P25: 경계 D | T07-P25: RECOVERY_SCOPE[selected=199; expected=20; backup=1; dry_run=1] | T07-P25: load=986, window=28s | T07-P25: review=동시성 | T07-P25: 경계 D 위반 여부를 판정 | T07-P25: RECOVERY_AUDIT + T07-P25/CH08 |
| T07-P25-C09 | T07-P25: 경계 A | T07-P25: REPLAY[key=cmd-025-08; attempts=4; response_seen=0] | T07-P25: load=109, window=47s | T07-P25: review=민감정보 | T07-P25: 경계 A 위반 여부를 판정 | T07-P25: IDEMPOTENCY_RECORD + T07-P25/CH08 |
| T07-P25-C10 | T07-P25: 경계 B | T07-P25: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P25: load=132, window=66s | T07-P25: review=중복 | T07-P25: 경계 B 위반 여부를 판정 | T07-P25: CLIENT_VERSION + T07-P25/CH08 |
| T07-P25-C11 | T07-P25: 경계 C | T07-P25: CONCURRENT_WRITE[actors=2; base_version=8; writes=2; gap_ms=73] | T07-P25: load=155, window=85s | T07-P25: review=권한 | T07-P25: 경계 C 위반 여부를 판정 | T07-P25: STATE_VERSION + T07-P25/CH08 |
| T07-P25-C12 | T07-P25: 경계 D | T07-P25: UNKNOWN_OUTCOME[timeout_ms=112; provider_state=UNKNOWN; lookup_id=p02511] | T07-P25: load=178, window=14s | T07-P25: review=입력 경계 | T07-P25: 경계 D 위반 여부를 판정 | T07-P25: PROVIDER_RESULT + T07-P25/CH08 |
| T07-P25-C13 | T07-P25: 경계 A | T07-P25: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P25: load=201, window=33s | T07-P25: review=timeout | T07-P25: 경계 A 위반 여부를 판정 | T07-P25: DURABLE_STATE + T07-P25/CH08 |
| T07-P25-C14 | T07-P25: 경계 B | T07-P25: OVERLOAD[rps=302; p99_ms=435; queue=38] | T07-P25: load=224, window=52s | T07-P25: review=자원 | T07-P25: 경계 B 위반 여부를 판정 | T07-P25: QUEUE_PRESSURE + T07-P25/CH08 |
| T07-P25-C15 | T07-P25: 경계 C | T07-P25: SAMPLING[sample_rate=38%; trace_present=0; metric_present=1] | T07-P25: load=247, window=71s | T07-P25: review=순서 | T07-P25: 경계 C 위반 여부를 판정 | T07-P25: TRACE_METRIC_CROSSCHECK + T07-P25/CH08 |
| T07-P25-C16 | T07-P25: 경계 D | T07-P25: DISCONNECT[disconnect_ms=61; commit_state=UNKNOWN; request=025-15] | T07-P25: load=270, window=90s | T07-P25: review=관측 | T07-P25: 경계 D 위반 여부를 판정 | T07-P25: COMMIT_TIMELINE + T07-P25/CH08 |

채점은 결론보다 근거를 본다. T07-P25/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · validation·business rule·authorization을 분리하기 — evidence 판독 문제 14개

T07-P25 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P25 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P25-V01 | T07-P25: latency=1045ms; queue=15; retry=4 | T07-P25: 과부하 | T07-P25: 축=과부하; 원인 확정은 보류 | T07-P25: QUEUE_PRESSURE + T07-P25/CH08 | T07-P25: 피할 오판=순서 가정 |
| T07-P25-V02 | T07-P25: latency=1112ms; queue=26; retry=0 | T07-P25: sampling | T07-P25: 축=sampling; 원인 확정은 보류 | T07-P25: TRACE_METRIC_CROSSCHECK + T07-P25/CH08 | T07-P25: 피할 오판=상태 수명 혼동 |
| T07-P25-V03 | T07-P25: latency=1179ms; queue=37; retry=3 | T07-P25: client disconnect | T07-P25: 축=client disconnect; 원인 확정은 보류 | T07-P25: COMMIT_TIMELINE + T07-P25/CH08 | T07-P25: 피할 오판=운영 한계 누락 |
| T07-P25-V04 | T07-P25: latency=1246ms; queue=48; retry=6 | T07-P25: 재발 | T07-P25: 축=재발; 원인 확정은 보류 | T07-P25: RECURRENCE_TIMELINE + T07-P25/CH08 | T07-P25: 피할 오판=오류 합치기 |
| T07-P25-V05 | T07-P25: latency=1313ms; queue=59; retry=2 | T07-P25: 대형 입력 | T07-P25: 축=대형 입력; 원인 확정은 보류 | T07-P25: SIZE_LIMIT + T07-P25/CH08 | T07-P25: 피할 오판=복구 과잉 |
| T07-P25-V06 | T07-P25: latency=1380ms; queue=70; retry=5 | T07-P25: 순서 역전 | T07-P25: 축=순서 역전; 원인 확정은 보류 | T07-P25: SEQUENCE_STATE + T07-P25/CH08 | T07-P25: 피할 오판=잘못된 전제 |
| T07-P25-V07 | T07-P25: latency=1447ms; queue=1; retry=1 | T07-P25: 복구 범위 | T07-P25: 축=복구 범위; 원인 확정은 보류 | T07-P25: RECOVERY_AUDIT + T07-P25/CH08 | T07-P25: 피할 오판=경계 누락 |
| T07-P25-V08 | T07-P25: latency=1514ms; queue=12; retry=4 | T07-P25: 재전송 | T07-P25: 축=재전송; 원인 확정은 보류 | T07-P25: IDEMPOTENCY_RECORD + T07-P25/CH08 | T07-P25: 피할 오판=증거 혼동 |
| T07-P25-V09 | T07-P25: latency=1581ms; queue=23; retry=0 | T07-P25: 구버전 client | T07-P25: 축=구버전 client; 원인 확정은 보류 | T07-P25: CLIENT_VERSION + T07-P25/CH08 | T07-P25: 피할 오판=재시도 오판 |
| T07-P25-V10 | T07-P25: latency=1648ms; queue=34; retry=3 | T07-P25: 동시 변경 | T07-P25: 축=동시 변경; 원인 확정은 보류 | T07-P25: STATE_VERSION + T07-P25/CH08 | T07-P25: 피할 오판=동시성 무시 |
| T07-P25-V11 | T07-P25: latency=1715ms; queue=45; retry=6 | T07-P25: unknown outcome | T07-P25: 축=unknown outcome; 원인 확정은 보류 | T07-P25: PROVIDER_RESULT + T07-P25/CH08 | T07-P25: 피할 오판=순서 가정 |
| T07-P25-V12 | T07-P25: latency=1782ms; queue=56; retry=2 | T07-P25: 재시작 | T07-P25: 축=재시작; 원인 확정은 보류 | T07-P25: DURABLE_STATE + T07-P25/CH08 | T07-P25: 피할 오판=상태 수명 혼동 |
| T07-P25-V13 | T07-P25: latency=49ms; queue=67; retry=5 | T07-P25: 과부하 | T07-P25: 축=과부하; 원인 확정은 보류 | T07-P25: QUEUE_PRESSURE + T07-P25/CH08 | T07-P25: 피할 오판=운영 한계 누락 |
| T07-P25-V14 | T07-P25: latency=116ms; queue=78; retry=1 | T07-P25: sampling | T07-P25: 축=sampling; 원인 확정은 보류 | T07-P25: TRACE_METRIC_CROSSCHECK + T07-P25/CH08 | T07-P25: 피할 오판=오류 합치기 |

## CHAPTER 24 · validation·business rule·authorization을 분리하기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P25에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P25-D01 | T07-P25: pool 확대 | T07-P25: connection/worker pool 상한을 늘린다 | T07-P25: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P25: DISCONNECT[disconnect_ms=60; commit_state=UNKNOWN; request=025-00] | T07-P25: COMMIT_TIMELINE + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P25-D02 | T07-P25: schema 변경 | T07-P25: 필드 이름·형식·required 조건을 바꾼다 | T07-P25: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P25: RECURRENCE[occurrence=3; interval_s=143; mitigation_applied=1] | T07-P25: RECURRENCE_TIMELINE + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P25-D03 | T07-P25: 결과 합치기 | T07-P25: 여러 오류를 하나의 status/error code로 합친다 | T07-P25: client 행동과 retry 가능성을 잃지 않는지 | T07-P25: LARGE_INPUT[body_kb=1504; limit_kb=256; parsed=0] | T07-P25: SIZE_LIMIT + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P25-D04 | T07-P25: retry 추가 | T07-P25: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P25: unknown outcome과 duplicate side effect를 구분하는지 | T07-P25: REORDER[in_seq=6,4,5; applied_version=2] | T07-P25: SEQUENCE_STATE + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P25-D05 | T07-P25: 로그 확대 | T07-P25: debug를 위해 payload와 context 기록을 늘린다 | T07-P25: secret·PII·cardinality 비용을 통제하는지 | T07-P25: RECOVERY_SCOPE[selected=166; expected=21; backup=1; dry_run=0] | T07-P25: RECOVERY_AUDIT + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P25-D06 | T07-P25: 강제 종료 | T07-P25: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P25: in-flight request와 background work의 결과를 잃는지 | T07-P25: REPLAY[key=cmd-025-05; attempts=4; response_seen=0] | T07-P25: IDEMPOTENCY_RECORD + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P25-D07 | T07-P25: AI package 추가 | T07-P25: AI가 제안한 새 dependency를 도입한다 | T07-P25: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P25: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P25: CLIENT_VERSION + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P25-D08 | T07-P25: 비동기화 | T07-P25: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P25: durability·status API·worker retry 계약이 생기는지 | T07-P25: CONCURRENT_WRITE[actors=2; base_version=8; writes=2; gap_ms=34] | T07-P25: STATE_VERSION + T07-P25/CH08 | T07-P25: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P25: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · validation·business rule·authorization을 분리하기 — 최종 contract와 evidence spine

**최종 계약:** 문법적으로 올바른 값, 업무상 가능한 행동, 그 사용자가 할 수 있는 행동은 서로 다른 질문이다.

**정상 메커니즘:** schema validation 뒤에 domain rule을 적용하고 별도로 actor-resource-action 권한을 확인한다.

**대표 실패:** 권한 실패를 validation으로 숨기거나 business rule을 client가 보내 준 flag로 결정한다.

**검증 evidence:** 각 단계의 실패 종류와 공개 응답을 분리해 기록한다.

**직접 행동:** 세 단계 판정 결과를 조합해 최초 거부 이유를 선택한다.

**다음 연결:** `side effect와 outbox 사고방식`.

`validation·business rule·authorization을 분리하기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | validation·business rule·authorization을 분리하기의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | validation·business rule·authorization을 분리하기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | validation·business rule·authorization을 분리하기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | validation·business rule·authorization을 분리하기의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | validation·business rule·authorization을 분리하기의 개념·실패·운영 판단 교차 확인 |

`validation·business rule·authorization을 분리하기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
