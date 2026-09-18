# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 05 · 관측 가능성과 장애 복구를 요청 단위로 익히기

### LESSON 07 · timeout·retry·circuit breaker·bulkhead 조합

## CHAPTER 01 · timeout·retry·circuit breaker·bulkhead 조합 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 복원력 패턴은 각각 다른 실패를 막으며 무조건 많이 붙인다고 안전해지는 것이 아니다.

**아주 쉬운 사건.** A와 B가 각각 3회 retry한다. 이 사건에서는 먼저 **retry amplification을 계산한다**.

**왜 필요한가.** 정상 동작은 timeout은 기다림 상한, retry는 일시 실패 재시도, circuit breaker는 실패 의존성 호출 억제, bulkhead는 자원 격리를 담당한다. 반대로 여러 계층 retry가 곱해져 요청 폭풍이 생기거나 breaker가 정상 traffic까지 오래 막는다.

**암기:** `timeout·retry·circuit breaker·bulkhead 조합`의 역할 한 줄.

**직접 이해:** `timeout·retry·circuit breaker·bulkhead 조합`의 입력·상태·결과 경계.

**AI 위임 가능:** `timeout·retry·circuit breaker·bulkhead 조합` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `A와 B가 각각 3회 retry한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 attempt 수, timeout budget, breaker state, pool/queue별 saturation을 본다.

## CHAPTER 02 · timeout·retry·circuit breaker·bulkhead 조합 — 아주 쉬운 예를 한 단계씩 해석

T07-P67: `A와 B가 각각 3회 retry한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | A와 B가 각각 3회 retry한다 | T07-P67 외부 입력 | T07-P67: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | retry amplification을 계산한다 | T07-P67 판단 기준 | T07-P67/CH08 관측표와 대조 |
| 정상 경로 | T07-P67/CH03 M1→M5 | timeout·retry·circuit breaker·bulkhead 조합: 완료 시점을 단계별로 분리 | T07-P67: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P67/CH06 여러 계층 retry가 곱해져 요청 폭풍이 생기거나 breaker가 정상 traffic까지 오래 막는다. | T07-P67: 깨진 계약 하나를 특정 | timeout·retry·circuit breaker·bulkhead 조합: 증상과 원인을 분리 |
| 재검증 | T07-P67/CH10 직접 실행 | T07-P67: 예상값 T07-P067 3/4 기록 | timeout·retry·circuit breaker·bulkhead 조합: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P67/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P67/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · timeout·retry·circuit breaker·bulkhead 조합 — 내부 메커니즘과 상태 전이

timeout은 기다림 상한, retry는 일시 실패 재시도, circuit breaker는 실패 의존성 호출 억제, bulkhead는 자원 격리를 담당한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | A와 B가 각각 3회 retry한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | retry amplification을 계산한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | timeout은 기다림 상한, retry는 일시 실패 재시도, circuit breaker는 실패 의존성 호출 억제, bulkhead는 자원 격리를 담당한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | attempt 수, timeout budget, breaker state, pool/queue별 saturation을 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 상태와 오류율로 breaker transition을 계산한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`timeout·retry·circuit breaker·bulkhead 조합` 흐름을 framework 이름 없이 설명한다.

막히면 `A와 B가 각각 3회 retry한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · timeout·retry·circuit breaker·bulkhead 조합 — 실전 경계 A

**경계 A.** timeout은 한 시도를 오래 붙잡지 않게 하고 retry는 새 시도를 만들며 circuit breaker는 실패가 많은 dependency 호출을 잠시 막고 bulkhead는 자원 pool을 분리해 장애 전파를 제한한다.

T07-P67/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P67에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P67/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P67/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P67): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P67/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P67-A1-749 | T07-P67 조건 | timeout은 한 시도를 오래 붙잡지 않게 하고 retry는 새 시도를 만들며 circuit breaker는 실패가 많은 dependency 호출을 잠시 막고 bulkhead는 자원 pool을 분리해 장애 전파를 제한한다. |
| T07-P67-A2-750 | T07-P67 변화점 | T07-P67/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P67-A3-751 | T07-P67 반례 | T07-P67/CH06 대표 실패와 A 위반을 구별 |
| T07-P67-A4-752 | T07-P67 근거 | T07-P67/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P67-A5-753 | T07-P67 재실험 | A와 B가 각각 3회 retry한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · timeout·retry·circuit breaker·bulkhead 조합 — 실전 경계 B

**경계 B.** client→service A→service B 각 계층이 3회 retry하면 하나의 사용자 요청이 최대 9회 이상 하위 호출로 증폭될 수 있어 retry ownership을 한두 계층으로 제한한다.

T07-P67/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P67에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P67/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P67/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P67): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P67/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P67-B1-780 | T07-P67 조건 | client→service A→service B 각 계층이 3회 retry하면 하나의 사용자 요청이 최대 9회 이상 하위 호출로 증폭될 수 있어 retry ownership을 한두 계층으로 제한한다. |
| T07-P67-B2-781 | T07-P67 독립성 | T07-P67/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P67-B3-782 | T07-P67 상태 | T07-P67/CH03 before·after 위치를 다시 지정 |
| T07-P67-B4-783 | T07-P67 반증 | T07-P67/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P67-B5-784 | T07-P67 적용 | timeout·retry·circuit breaker·bulkhead 조합의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · timeout·retry·circuit breaker·bulkhead 조합 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **여러 계층 retry가 곱해져 요청 폭풍이 생기거나 breaker가 정상 traffic까지 오래 막는다.**

아래 여섯 사례는 T07-P67의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P67-F01 | T07-P67: 대표 실패 | T07-P67: T07-P67/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P67: 현상만 보고 원인을 확정 | T07-P67/CH08 evidence map에서 상태를 대조 |
| T07-P67-F02 | T07-P67: 경계 A 누락 | T07-P67: T07-P67/CH04 경계 A 위반 입력 | T07-P67: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P67/CH08 evidence map에서 상태를 대조 |
| T07-P67-F03 | T07-P67: 경계 B 누락 | T07-P67: T07-P67/CH05 경계 B 위반 입력 | T07-P67: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P67/CH08 evidence map에서 상태를 대조 |
| T07-P67-F04 | T07-P67: 복구 경계 C 누락 | T07-P67: T07-P67/CH07 경계 C 복구 조건 | T07-P67: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P67/CH08 evidence map에서 상태를 대조 |
| T07-P67-F05 | T07-P67: 운영 경계 D 누락 | T07-P67: T07-P67/CH09 경계 D 운영 조건 | T07-P67: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P67/CH08 evidence map에서 상태를 대조 |
| T07-P67-F06 | T07-P67: 증거 없는 결론 | T07-P67: T07-P67/CH02 첫 판단만 존재 | T07-P67: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P67/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P67/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · timeout·retry·circuit breaker·bulkhead 조합 — 복구 가능한 상태와 수명

**경계 C.** circuit half-open에서는 소수 probe만 허용해 dependency가 회복됐는지 확인하고 성공 즉시 모든 traffic을 몰아 다시 쓰러뜨리지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P67에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P67/CH08 evidence map을 본다. 복구 후에는 T07-P67/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P67에서 이미 확정된 side effect는 T07-P67/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P67-R1-842 | T07-P67 중단 직전 | T07-P67/CH03에서 이미 확정된 상태만 표시 |
| T07-P67-R2-843 | T07-P67 재시작 직후 | circuit half-open에서는 소수 probe만 허용해 dependency가 회복됐는지 확인하고 성공 즉시 모든 traffic을 몰아 다시 쓰러뜨리지 않는다. |
| T07-P67-R3-844 | T07-P67 재검증 | T07-P67/CH08 근거로 중복·누락 여부 확인 |
| T07-P67-R4-845 | T07-P67 재실행 | T07-P67/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · timeout·retry·circuit breaker·bulkhead 조합 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | A와 B가 각각 3회 retry한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | retry amplification을 계산한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | timeout은 기다림 상한, retry는 일시 실패 재시도, circuit breaker는 실패 의존성 호출 억제, bulkhead는 자원 격리를 담당한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 여러 계층 retry가 곱해져 요청 폭풍이 생기거나 breaker가 정상 traffic까지 오래 막는다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | attempt 수, timeout budget, breaker state, pool/queue별 saturation을 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`timeout·retry·circuit breaker·bulkhead 조합` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · timeout·retry·circuit breaker·bulkhead 조합 — 운영 한계와 종료 조건

**경계 D.** fallback은 stale cache·기본값·기능 축소 중 업무상 안전한 결과만 사용하며 결제 성공 여부처럼 틀리면 위험한 값에 ‘임시 성공’을 만들지 않는다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P67/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P67/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P67 과제: 경계 D와 T07-P67/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P67-O1-904 | synthetic-load=44 | T07-P67/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P67-O2-905 | synthetic-budget=204ms | T07-P67 timeout과 unknown outcome을 분리 |
| T07-P67-O3-906 | T07-P67 종료 | fallback은 stale cache·기본값·기능 축소 중 업무상 안전한 결과만 사용하며 결제 성공 여부처럼 틀리면 위험한 값에 ‘임시 성공’을 만들지 않는다. |
| T07-P67-O4-907 | T07-P67 완화 | T07-P67/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · timeout·retry·circuit breaker·bulkhead 조합 — 직접 실행하는 작은 모델

`timeout·retry·circuit breaker·bulkhead 조합` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P067 3/4`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P067";
const checks = [true,false,true,true];
const passed = checks.filter(Boolean).length;
console.log(marker, `${passed}/${checks.length}`);
```

기준 출력: `T07-P067 3/4`.

`timeout·retry·circuit breaker·bulkhead 조합`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P67-L1-936 | constmarker="T07-P067"; | T07-P67 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P67-L2-937 | constchecks=[true,false,true,true]; | T07-P67 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P67-L3-938 | constpassed=checks.filter(Boolean).length; | T07-P67 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P67-L4-939 | console.log(marker,`${passed}/${checks.length}`); | T07-P67 출력 관측점; 예상 `T07-P067 3/4`와 비교 |
| T07-P67-LX-1025 | T07-P67 실행 기록 | T07-P67 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · timeout·retry·circuit breaker·bulkhead 조합 — 한 부분만 수정하고 다시 예측

수정 과제: **false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다**.

수정 전은 `T07-P067 3/4`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P67/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P67-D1-966 | 기준 `T07-P067 3/4` | T07-P67 수정 전 실행을 먼저 재현 |
| T07-P67-D2-967 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | T07-P67 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P67-D3-968 | T07-P67 새 예측 | T07-P67 실행 전에 출력·상태를 먼저 기록 |
| T07-P67-D4-969 | T07-P67 재실행 | T07-P67/CH10 실제값과 새 예측을 대조 |
| T07-P67-D5-970 | T07-P67 반례 | T07-P67/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P67-D6-971 | T07-P67 근거 | T07-P67/CH08 상태가 설명과 일치해야 완료 |
| T07-P67-D7-972 | T07-P67 이유 | T07-P67 변경 이유를 timeout·retry·circuit breaker·bulkhead 조합 계약과 연결해 설명 |

## CHAPTER 12 · timeout·retry·circuit breaker·bulkhead 조합 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: timeout·retry·circuit breaker·bulkhead 조합 | 복원력 패턴은 각각 다른 실패를 막으며 무조건 많이 붙인다고 안전해지는 것이 아니다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P67/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | timeout은 기다림 상한, retry는 일시 실패 재시도, circuit breaker는 실패 의존성 호출 억제, bulkhead는 자원 격리를 담당한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 여러 계층 retry가 곱해져 요청 폭풍이 생기거나 breaker가 정상 traffic까지 오래 막는다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | attempt 수, timeout budget, breaker state, pool/queue별 saturation을 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P67/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P67/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P67/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P67/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `timeout·retry·circuit breaker·bulkhead 조합` 실행 코드 수정 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | `timeout·retry·circuit breaker·bulkhead 조합` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P67/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P67/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · timeout·retry·circuit breaker·bulkhead 조합 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 복원력 패턴은 각각 다른 실패를 막으며 무조건 많이 붙인다고 안전해지는 것이 아니다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P67/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P67/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P67/CH10 실행용 boilerplate·test 후보 | T07-P67: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P067 3/4` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P67/CH02의 판단 기준과 T07-P67/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P67-AI1-31 | T07-P67 사람 결정 | T07-P67 업무 의미·허용 위험·완료 기준 소유 |
| T07-P67-AI2-32 | T07-P67 AI 초안 | T07-P67/CH10 boilerplate·test 후보까지만 위임 |
| T07-P67-AI3-33 | T07-P67 검증 | T07-P67/CH06 반례와 T07-P67/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · timeout·retry·circuit breaker·bulkhead 조합 — 경계 조합 실험 8개

T07-P67: T07-P67/CH04~T07-P67/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P67의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-K01 | T07-P67: 경계 A | T07-P67: 경계 B | T07-P67: DRAIN[ready=0; active=4; drain_deadline_s=6] / sample=43 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + DRAIN_STATE |
| T07-P67-K02 | T07-P67: 경계 A | T07-P67: 경계 C | T07-P67: RECOVERY_SCOPE[selected=96; expected=9; backup=1; dry_run=0] / sample=50 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + RECOVERY_AUDIT |
| T07-P67-K03 | T07-P67: 경계 A | T07-P67: 경계 D | T07-P67: UNKNOWN_OUTCOME[timeout_ms=187; provider_state=UNKNOWN; lookup_id=p06703] / sample=57 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + PROVIDER_RESULT |
| T07-P67-K04 | T07-P67: 경계 B | T07-P67: 경계 C | T07-P67: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] / sample=64 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + DURABLE_STATE |
| T07-P67-K05 | T07-P67: 경계 B | T07-P67: 경계 D | T07-P67: OVERLOAD[rps=527; p99_ms=687; queue=18] / sample=71 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + QUEUE_PRESSURE |
| T07-P67-K06 | T07-P67: 경계 C | T07-P67: 경계 D | T07-P67: SAMPLING[sample_rate=66%; trace_present=0; metric_present=1] / sample=78 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P67-K07 | T07-P67: 경계 A | T07-P67: 경계 B+C | T07-P67: DISCONNECT[disconnect_ms=89; commit_state=UNKNOWN; request=067-07] / sample=85 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + COMMIT_TIMELINE |
| T07-P67-K08 | T07-P67: 경계 B | T07-P67: 경계 C+D | T07-P67: RECURRENCE[occurrence=5; interval_s=172; mitigation_applied=1] / sample=92 | T07-P67: 먼저 깨지는 경계를 판정 | T07-P67: T07-P67/CH08 + RECURRENCE_TIMELINE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P67/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · timeout·retry·circuit breaker·bulkhead 조합 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P67와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-B06 | T07-P67: graceful shutdown과 draining | T07-P67: 배포 중 20개 요청이 처리 중이다 | T07-P67: draining deadline을 적용한다 | T07-P67: T07-P67/CH08 증거와 형제 LESSON 증거를 분리 | T07-P67: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P67-B08 | T07-P67: overload와 queueing을 latency에서 읽기 | T07-P67: CPU는 70%인데 p99가 급등한다 | T07-P67: queue와 saturation signal을 찾는다 | T07-P67: T07-P67/CH08 증거와 형제 LESSON 증거를 분리 | T07-P67: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P67-B05 | T07-P67: health·readiness·liveness를 분리하기 | T07-P67: DB가 잠깐 느린데 liveness도 실패한다 | T07-P67: readiness와 restart 조건을 분리한다 | T07-P67: T07-P67/CH08 증거와 형제 LESSON 증거를 분리 | T07-P67: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P67-B09 | T07-P67: memory leak과 resource leak | T07-P67: GC 뒤 heap 바닥선이 계속 오른다 | T07-P67: leak trend와 정상 cache를 구분한다 | T07-P67: T07-P67/CH08 증거와 형제 LESSON 증거를 분리 | T07-P67: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P67-B04 | T07-P67: SLI·SLO를 backend 계약으로 정하기 | T07-P67: 30일 SLO가 빠르게 소진된다 | T07-P67: error budget과 burn rate를 본다 | T07-P67: T07-P67/CH08 증거와 형제 LESSON 증거를 분리 | T07-P67: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · timeout·retry·circuit breaker·bulkhead 조합 — 선택형 실패 주입 6개

T07-P67: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P67 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-X01 | T07-P67: unknown outcome | T07-P67: dependency timeout 후 성공 여부 불명; sample=100 | T07-P67: 실패와 미확정 결과 | T07-P67: T07-P67/CH02 판단과 별도 기록 | T07-P67: provider id·조회 결과·retry history |
| T07-P67-X02 | T07-P67: 재시작 | T07-P67: side effect 직후 process가 재시작됨; sample=117 | T07-P67: durable state와 memory state | T07-P67: T07-P67/CH02 판단과 별도 기록 | T07-P67: commit·outbox·job id·restart 전후 상태 |
| T07-P67-X03 | T07-P67: 과부하 | T07-P67: traffic 세 배, p99 급증; sample=134 | T07-P67: 기능 실패와 saturation | T07-P67: T07-P67/CH02 판단과 별도 기록 | T07-P67: queue age·pool wait·CPU/event-loop·quota |
| T07-P67-X04 | T07-P67: sampling | T07-P67: 일부 log가 sampling으로 빠짐; sample=151 | T07-P67: 기록 부재와 사건 부재 | T07-P67: T07-P67/CH02 판단과 별도 기록 | T07-P67: metric·trace·durable state 교차 근거 |
| T07-P67-X05 | T07-P67: client disconnect | T07-P67: 응답 전에 연결이 끊김; sample=168 | T07-P67: 연결 종료와 server effect | T07-P67: T07-P67/CH02 판단과 별도 기록 | T07-P67: commit 시각·worker/outbox·request lifecycle |
| T07-P67-X06 | T07-P67: 재발 | T07-P67: 같은 오류가 잠시 뒤 다시 발생; sample=185 | T07-P67: 완화와 원인 제거 | T07-P67: T07-P67/CH02 판단과 별도 기록 | T07-P67: 재발 timeline·변경점·resource state |

## CHAPTER 17 · timeout·retry·circuit breaker·bulkhead 조합 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P67에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-E01 | T07-P67: 대표 실패를 원인으로 착각 | T07-P67: T07-P67/CH06 실패 case를 다른 입력으로 재현 | T07-P67: 현상과 원인을 같은 것으로 봄 | T07-P67: T07-P67/CH06 대표 실패와 T07-P67/CH08 증거를 다시 대조 | T07-P67: T07-P67/CH08 |
| T07-P67-E02 | T07-P67: 경계 A 생략 | T07-P67: T07-P67/CH04의 조건 하나를 반대로 설정 | T07-P67: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P67: T07-P67/CH04를 새 입력에 적용 | T07-P67: T07-P67/CH08 |
| T07-P67-E03 | T07-P67: 경계 B 생략 | T07-P67: T07-P67/CH05의 조건 하나를 반대로 설정 | T07-P67: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P67: T07-P67/CH05를 새 입력에 적용 | T07-P67: T07-P67/CH08 |
| T07-P67-E04 | T07-P67: 복구 상태 혼동 | T07-P67: T07-P67/CH07에서 처리 중단을 주입 | T07-P67: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P67: T07-P67/CH07에서 수명 경계를 다시 표시 | T07-P67: T07-P67/CH08 |
| T07-P67-E05 | T07-P67: 운영 한계 누락 | T07-P67: T07-P67/CH09에서 부하 또는 drain 조건을 변경 | T07-P67: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P67: T07-P67/CH09의 종료 조건을 다시 작성 | T07-P67: T07-P67/CH08 |
| T07-P67-E06 | T07-P67: 증거 없는 성공 판정 | T07-P67: T07-P67/CH08에서 증거 하나를 숨김 | T07-P67: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P67: T07-P67/CH08에서 독립 증거 둘을 선택 | T07-P67: T07-P67/CH08 |

## CHAPTER 18 · timeout·retry·circuit breaker·bulkhead 조합 — synthetic 관측값 판독 6개

T07-P67: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P67의 숫자 하나만으로 원인을 단정하지 않고 T07-P67/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-O01 | T07-P67/error_rate | 187 | T07-P67: 오류율 | T07-P67: 축=client disconnect; 원인 확정 금지 | T07-P67: COMMIT_TIMELINE + T07-P67/CH08 |
| T07-P67-O02 | T07-P67/p99_ms | 204 | T07-P67: p99 지연 | T07-P67: 축=재발; 원인 확정 금지 | T07-P67: RECURRENCE_TIMELINE + T07-P67/CH08 |
| T07-P67-O03 | T07-P67/queue_depth | 221 | T07-P67: 대기열 깊이 | T07-P67: 축=대형 입력; 원인 확정 금지 | T07-P67: SIZE_LIMIT + T07-P67/CH08 |
| T07-P67-O04 | T07-P67/heap_mb | 238 | T07-P67: heap 사용량 | T07-P67: 축=drain; 원인 확정 금지 | T07-P67: DRAIN_STATE + T07-P67/CH08 |
| T07-P67-O05 | T07-P67/event_loop_lag_ms | 255 | T07-P67: event-loop 지연 | T07-P67: 축=복구 범위; 원인 확정 금지 | T07-P67: RECOVERY_AUDIT + T07-P67/CH08 |
| T07-P67-O06 | T07-P67/dependency_errors | 272 | T07-P67: dependency 오류 수 | T07-P67: 축=unknown outcome; 원인 확정 금지 | T07-P67: PROVIDER_RESULT + T07-P67/CH08 |

## CHAPTER 19 · timeout·retry·circuit breaker·bulkhead 조합 — 선택형 코드 리뷰 6질문

T07-P67: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P67에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-R01 | T07-P67: 동시성 | T07-P67: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P67: RECURRENCE[occurrence=2; interval_s=84; mitigation_applied=1] | T07-P67: T07-P67/CH04 | T07-P67: RECURRENCE_TIMELINE |
| T07-P67-R02 | T07-P67: 권한 | T07-P67: actor·action·resource가 같은 판단 안에 있는가 | T07-P67: LARGE_INPUT[body_kb=1032; limit_kb=512; parsed=0] | T07-P67: T07-P67/CH05 | T07-P67: SIZE_LIMIT |
| T07-P67-R03 | T07-P67: 자원 | T07-P67: pool·queue·memory·connection 상한이 있는가 | T07-P67: DRAIN[ready=0; active=5; drain_deadline_s=7] | T07-P67: T07-P67/CH06 | T07-P67: DRAIN_STATE |
| T07-P67-R04 | T07-P67: 상태 변경 | T07-P67: side effect가 어느 줄에서 확정되는가 | T07-P67: RECOVERY_SCOPE[selected=107; expected=22; backup=1; dry_run=1] | T07-P67: T07-P67/CH07 | T07-P67: RECOVERY_AUDIT |
| T07-P67-R05 | T07-P67: 동시성 | T07-P67: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P67: UNKNOWN_OUTCOME[timeout_ms=198; provider_state=UNKNOWN; lookup_id=p06704] | T07-P67: T07-P67/CH04 | T07-P67: PROVIDER_RESULT |
| T07-P67-R06 | T07-P67: 권한 | T07-P67: actor·action·resource가 같은 판단 안에 있는가 | T07-P67: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P67: T07-P67/CH05 | T07-P67: DURABLE_STATE |

## CHAPTER 20 · timeout·retry·circuit breaker·bulkhead 조합 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P67에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-I01 | T07-P67: 마지막 정상 | T07-P67: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P67: LARGE_INPUT[body_kb=944; limit_kb=512; parsed=0] | T07-P67: T07-P67/CH08 + SIZE_LIMIT | T07-P67-incident-282 |
| T07-P67-I02 | T07-P67: 가설 검증 | T07-P67: 원인 후보 하나만 뒤집어 재현 | T07-P67: DRAIN[ready=0; active=4; drain_deadline_s=6] | T07-P67: T07-P67/CH08 + DRAIN_STATE | T07-P67-incident-283 |
| T07-P67-I03 | T07-P67: 복구 확인 | T07-P67: durable state와 사용자 결과를 모두 확인 | T07-P67: RECOVERY_SCOPE[selected=96; expected=9; backup=1; dry_run=0] | T07-P67: T07-P67/CH08 + RECOVERY_AUDIT | T07-P67-incident-284 |
| T07-P67-I04 | T07-P67: 재주입 | T07-P67: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P67: UNKNOWN_OUTCOME[timeout_ms=187; provider_state=UNKNOWN; lookup_id=p06703] | T07-P67: T07-P67/CH08 + PROVIDER_RESULT | T07-P67-incident-285 |
| T07-P67-I05 | T07-P67: 회귀 고정 | T07-P67: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P67: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P67: T07-P67/CH08 + DURABLE_STATE | T07-P67-incident-286 |
| T07-P67-I06 | T07-P67: 영향 범위 | T07-P67: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P67: OVERLOAD[rps=527; p99_ms=687; queue=18] | T07-P67: T07-P67/CH08 + QUEUE_PRESSURE | T07-P67-incident-287 |

## CHAPTER 21 · timeout·retry·circuit breaker·bulkhead 조합 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P67/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P67/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P67/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P67/CH04~T07-P67/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P67/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P67/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P67/CH18 signal 두 개와 T07-P67/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P67/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P67/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · timeout·retry·circuit breaker·bulkhead 조합 — 통합 casebook 16문제

T07-P67 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P67 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P67-C01 | T07-P67: 경계 A | T07-P67: DRAIN[ready=0; active=8; drain_deadline_s=5] | T07-P67: load=243, window=69s | T07-P67: review=복구 | T07-P67: 경계 A 위반 여부를 판정 | T07-P67: DRAIN_STATE + T07-P67/CH08 |
| T07-P67-C02 | T07-P67: 경계 B | T07-P67: RECOVERY_SCOPE[selected=85; expected=17; backup=1; dry_run=1] | T07-P67: load=266, window=88s | T07-P67: review=동시성 | T07-P67: 경계 B 위반 여부를 판정 | T07-P67: RECOVERY_AUDIT + T07-P67/CH08 |
| T07-P67-C03 | T07-P67: 경계 C | T07-P67: UNKNOWN_OUTCOME[timeout_ms=176; provider_state=UNKNOWN; lookup_id=p06702] | T07-P67: load=289, window=17s | T07-P67: review=민감정보 | T07-P67: 경계 C 위반 여부를 판정 | T07-P67: PROVIDER_RESULT + T07-P67/CH08 |
| T07-P67-C04 | T07-P67: 경계 D | T07-P67: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P67: load=312, window=36s | T07-P67: review=중복 | T07-P67: 경계 D 위반 여부를 판정 | T07-P67: DURABLE_STATE + T07-P67/CH08 |
| T07-P67-C05 | T07-P67: 경계 A | T07-P67: OVERLOAD[rps=494; p99_ms=570; queue=24] | T07-P67: load=335, window=55s | T07-P67: review=권한 | T07-P67: 경계 A 위반 여부를 판정 | T07-P67: QUEUE_PRESSURE + T07-P67/CH08 |
| T07-P67-C06 | T07-P67: 경계 B | T07-P67: SAMPLING[sample_rate=53%; trace_present=1; metric_present=1] | T07-P67: load=358, window=74s | T07-P67: review=입력 경계 | T07-P67: 경계 B 위반 여부를 판정 | T07-P67: TRACE_METRIC_CROSSCHECK + T07-P67/CH08 |
| T07-P67-C07 | T07-P67: 경계 C | T07-P67: DISCONNECT[disconnect_ms=76; commit_state=UNKNOWN; request=067-06] | T07-P67: load=381, window=93s | T07-P67: review=timeout | T07-P67: 경계 C 위반 여부를 판정 | T07-P67: COMMIT_TIMELINE + T07-P67/CH08 |
| T07-P67-C08 | T07-P67: 경계 D | T07-P67: RECURRENCE[occurrence=4; interval_s=161; mitigation_applied=1] | T07-P67: load=404, window=22s | T07-P67: review=자원 | T07-P67: 경계 D 위반 여부를 판정 | T07-P67: RECURRENCE_TIMELINE + T07-P67/CH08 |
| T07-P67-C09 | T07-P67: 경계 A | T07-P67: LARGE_INPUT[body_kb=1648; limit_kb=512; parsed=0] | T07-P67: load=427, window=41s | T07-P67: review=순서 | T07-P67: 경계 A 위반 여부를 판정 | T07-P67: SIZE_LIMIT + T07-P67/CH08 |
| T07-P67-C10 | T07-P67: 경계 B | T07-P67: DRAIN[ready=0; active=11; drain_deadline_s=6] | T07-P67: load=450, window=60s | T07-P67: review=관측 | T07-P67: 경계 B 위반 여부를 판정 | T07-P67: DRAIN_STATE + T07-P67/CH08 |
| T07-P67-C11 | T07-P67: 경계 C | T07-P67: RECOVERY_SCOPE[selected=184; expected=16; backup=1; dry_run=0] | T07-P67: load=473, window=79s | T07-P67: review=상태 변경 | T07-P67: 경계 C 위반 여부를 판정 | T07-P67: RECOVERY_AUDIT + T07-P67/CH08 |
| T07-P67-C12 | T07-P67: 경계 D | T07-P67: UNKNOWN_OUTCOME[timeout_ms=275; provider_state=UNKNOWN; lookup_id=p06711] | T07-P67: load=496, window=98s | T07-P67: review=retry | T07-P67: 경계 D 위반 여부를 판정 | T07-P67: PROVIDER_RESULT + T07-P67/CH08 |
| T07-P67-C13 | T07-P67: 경계 A | T07-P67: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P67: load=519, window=27s | T07-P67: review=복구 | T07-P67: 경계 A 위반 여부를 판정 | T07-P67: DURABLE_STATE + T07-P67/CH08 |
| T07-P67-C14 | T07-P67: 경계 B | T07-P67: OVERLOAD[rps=791; p99_ms=750; queue=33] | T07-P67: load=542, window=46s | T07-P67: review=동시성 | T07-P67: 경계 B 위반 여부를 판정 | T07-P67: QUEUE_PRESSURE + T07-P67/CH08 |
| T07-P67-C15 | T07-P67: 경계 C | T07-P67: SAMPLING[sample_rate=73%; trace_present=0; metric_present=1] | T07-P67: load=565, window=65s | T07-P67: review=민감정보 | T07-P67: 경계 C 위반 여부를 판정 | T07-P67: TRACE_METRIC_CROSSCHECK + T07-P67/CH08 |
| T07-P67-C16 | T07-P67: 경계 D | T07-P67: DISCONNECT[disconnect_ms=96; commit_state=UNKNOWN; request=067-15] | T07-P67: load=588, window=84s | T07-P67: review=중복 | T07-P67: 경계 D 위반 여부를 판정 | T07-P67: COMMIT_TIMELINE + T07-P67/CH08 |

채점은 결론보다 근거를 본다. T07-P67/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · timeout·retry·circuit breaker·bulkhead 조합 — evidence 판독 문제 14개

T07-P67 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P67 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P67-V01 | T07-P67: latency=967ms; queue=69; retry=4 | T07-P67: 복구 범위 | T07-P67: 축=복구 범위; 원인 확정은 보류 | T07-P67: RECOVERY_AUDIT + T07-P67/CH08 | T07-P67: 피할 오판=상태 수명 혼동 |
| T07-P67-V02 | T07-P67: latency=1034ms; queue=0; retry=0 | T07-P67: unknown outcome | T07-P67: 축=unknown outcome; 원인 확정은 보류 | T07-P67: PROVIDER_RESULT + T07-P67/CH08 | T07-P67: 피할 오판=운영 한계 누락 |
| T07-P67-V03 | T07-P67: latency=1101ms; queue=11; retry=3 | T07-P67: 재시작 | T07-P67: 축=재시작; 원인 확정은 보류 | T07-P67: DURABLE_STATE + T07-P67/CH08 | T07-P67: 피할 오판=오류 합치기 |
| T07-P67-V04 | T07-P67: latency=1168ms; queue=22; retry=6 | T07-P67: 과부하 | T07-P67: 축=과부하; 원인 확정은 보류 | T07-P67: QUEUE_PRESSURE + T07-P67/CH08 | T07-P67: 피할 오판=복구 과잉 |
| T07-P67-V05 | T07-P67: latency=1235ms; queue=33; retry=2 | T07-P67: sampling | T07-P67: 축=sampling; 원인 확정은 보류 | T07-P67: TRACE_METRIC_CROSSCHECK + T07-P67/CH08 | T07-P67: 피할 오판=경계 누락 |
| T07-P67-V06 | T07-P67: latency=1302ms; queue=44; retry=5 | T07-P67: client disconnect | T07-P67: 축=client disconnect; 원인 확정은 보류 | T07-P67: COMMIT_TIMELINE + T07-P67/CH08 | T07-P67: 피할 오판=증거 혼동 |
| T07-P67-V07 | T07-P67: latency=1369ms; queue=55; retry=1 | T07-P67: 재발 | T07-P67: 축=재발; 원인 확정은 보류 | T07-P67: RECURRENCE_TIMELINE + T07-P67/CH08 | T07-P67: 피할 오판=재시도 오판 |
| T07-P67-V08 | T07-P67: latency=1436ms; queue=66; retry=4 | T07-P67: 대형 입력 | T07-P67: 축=대형 입력; 원인 확정은 보류 | T07-P67: SIZE_LIMIT + T07-P67/CH08 | T07-P67: 피할 오판=동시성 무시 |
| T07-P67-V09 | T07-P67: latency=1503ms; queue=77; retry=0 | T07-P67: drain | T07-P67: 축=drain; 원인 확정은 보류 | T07-P67: DRAIN_STATE + T07-P67/CH08 | T07-P67: 피할 오판=순서 가정 |
| T07-P67-V10 | T07-P67: latency=1570ms; queue=8; retry=3 | T07-P67: 복구 범위 | T07-P67: 축=복구 범위; 원인 확정은 보류 | T07-P67: RECOVERY_AUDIT + T07-P67/CH08 | T07-P67: 피할 오판=상태 수명 혼동 |
| T07-P67-V11 | T07-P67: latency=1637ms; queue=19; retry=6 | T07-P67: unknown outcome | T07-P67: 축=unknown outcome; 원인 확정은 보류 | T07-P67: PROVIDER_RESULT + T07-P67/CH08 | T07-P67: 피할 오판=운영 한계 누락 |
| T07-P67-V12 | T07-P67: latency=1704ms; queue=30; retry=2 | T07-P67: 재시작 | T07-P67: 축=재시작; 원인 확정은 보류 | T07-P67: DURABLE_STATE + T07-P67/CH08 | T07-P67: 피할 오판=오류 합치기 |
| T07-P67-V13 | T07-P67: latency=1771ms; queue=41; retry=5 | T07-P67: 과부하 | T07-P67: 축=과부하; 원인 확정은 보류 | T07-P67: QUEUE_PRESSURE + T07-P67/CH08 | T07-P67: 피할 오판=복구 과잉 |
| T07-P67-V14 | T07-P67: latency=38ms; queue=52; retry=1 | T07-P67: sampling | T07-P67: 축=sampling; 원인 확정은 보류 | T07-P67: TRACE_METRIC_CROSSCHECK + T07-P67/CH08 | T07-P67: 피할 오판=경계 누락 |

## CHAPTER 24 · timeout·retry·circuit breaker·bulkhead 조합 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P67에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P67-D01 | T07-P67: AI package 추가 | T07-P67: AI가 제안한 새 dependency를 도입한다 | T07-P67: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P67: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P67: DURABLE_STATE + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P67-D02 | T07-P67: 비동기화 | T07-P67: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P67: durability·status API·worker retry 계약이 생기는지 | T07-P67: OVERLOAD[rps=395; p99_ms=1092; queue=19] | T07-P67: QUEUE_PRESSURE + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P67-D03 | T07-P67: 권한 shortcut | T07-P67: payload의 owner/tenant id를 바로 사용한다 | T07-P67: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P67: SAMPLING[sample_rate=14%; trace_present=0; metric_present=1] | T07-P67: TRACE_METRIC_CROSSCHECK + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P67-D04 | T07-P67: 순서 병렬화 | T07-P67: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P67: 선후관계 invariant와 race를 깨지 않는지 | T07-P67: DISCONNECT[disconnect_ms=37; commit_state=UNKNOWN; request=067-03] | T07-P67: COMMIT_TIMELINE + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P67-D05 | T07-P67: validation 이동 | T07-P67: validation을 business side effect 뒤로 옮긴다 | T07-P67: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P67: RECURRENCE[occurrence=6; interval_s=128; mitigation_applied=1] | T07-P67: RECURRENCE_TIMELINE + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P67-D06 | T07-P67: batch 확대 | T07-P67: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P67: memory·deadline·부분 실패 범위가 커지는지 | T07-P67: LARGE_INPUT[body_kb=1384; limit_kb=512; parsed=0] | T07-P67: SIZE_LIMIT + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P67-D07 | T07-P67: fallback 추가 | T07-P67: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P67: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P67: DRAIN[ready=0; active=6; drain_deadline_s=11] | T07-P67: DRAIN_STATE + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P67-D08 | T07-P67: 외부 호출 이동 | T07-P67: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P67: lock duration과 unknown outcome 경계가 달라지는지 | T07-P67: RECOVERY_SCOPE[selected=151; expected=17; backup=1; dry_run=1] | T07-P67: RECOVERY_AUDIT + T07-P67/CH08 | T07-P67: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P67: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · timeout·retry·circuit breaker·bulkhead 조합 — 최종 contract와 evidence spine

**최종 계약:** 복원력 패턴은 각각 다른 실패를 막으며 무조건 많이 붙인다고 안전해지는 것이 아니다.

**정상 메커니즘:** timeout은 기다림 상한, retry는 일시 실패 재시도, circuit breaker는 실패 의존성 호출 억제, bulkhead는 자원 격리를 담당한다.

**대표 실패:** 여러 계층 retry가 곱해져 요청 폭풍이 생기거나 breaker가 정상 traffic까지 오래 막는다.

**검증 evidence:** attempt 수, timeout budget, breaker state, pool/queue별 saturation을 본다.

**직접 행동:** 상태와 오류율로 breaker transition을 계산한다.

**다음 연결:** `overload와 queueing을 latency에서 읽기`.

`timeout·retry·circuit breaker·bulkhead 조합`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| SRE | SRE | timeout·retry·circuit breaker·bulkhead 조합의 개념·실패·운영 판단 교차 확인 |
| SRE-WORKBOOK | SRE-WORKBOOK | timeout·retry·circuit breaker·bulkhead 조합의 개념·실패·운영 판단 교차 확인 |
| OTEL-SEMCONV | OpenTelemetry Semantic Conventions | timeout·retry·circuit breaker·bulkhead 조합의 개념·실패·운영 판단 교차 확인 |
| NODE-DOCS | Node.js Documentation | timeout·retry·circuit breaker·bulkhead 조합의 개념·실패·운영 판단 교차 확인 |

`timeout·retry·circuit breaker·bulkhead 조합` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
