# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 05 · 관측 가능성과 장애 복구를 요청 단위로 익히기

### LESSON 12 · feature flag와 runtime configuration

## CHAPTER 01 · feature flag와 runtime configuration — 쉬운 말에서 정확한 계약까지

**출발 개념.** 코드 배포 없이 동작을 바꾸는 flag/config는 편리하지만 또 하나의 상태 시스템이다.

**아주 쉬운 사건.** flag를 1% 사용자에게 먼저 켠다. 이 사건에서는 먼저 **점진 적용과 kill switch를 본다**.

**왜 필요한가.** 정상 동작은 flag owner, scope, default, rollout, expiry를 명시하고 요청마다 어떤 variation이 적용됐는지 관측한다. 반대로 오래된 flag가 중첩돼 같은 code path를 아무도 이해하지 못하거나 설정 오타가 전체 traffic에 퍼진다.

**암기:** `feature flag와 runtime configuration`의 역할 한 줄.

**직접 이해:** `feature flag와 runtime configuration`의 입력·상태·결과 경계.

**AI 위임 가능:** `feature flag와 runtime configuration` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `flag를 1% 사용자에게 먼저 켠다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 flag version/variation, config load 실패, fallback 사용량을 본다.

## CHAPTER 02 · feature flag와 runtime configuration — 아주 쉬운 예를 한 단계씩 해석

T07-P72: `flag를 1% 사용자에게 먼저 켠다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | flag를 1% 사용자에게 먼저 켠다 | T07-P72 외부 입력 | T07-P72: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | 점진 적용과 kill switch를 본다 | T07-P72 판단 기준 | T07-P72/CH08 관측표와 대조 |
| 정상 경로 | T07-P72/CH03 M1→M5 | feature flag와 runtime configuration: 완료 시점을 단계별로 분리 | T07-P72: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P72/CH06 오래된 flag가 중첩돼 같은 code path를 아무도 이해하지 못하거나 설정 오타가 전체 traffic에 퍼진다. | T07-P72: 깨진 계약 하나를 특정 | feature flag와 runtime configuration: 증상과 원인을 분리 |
| 재검증 | T07-P72/CH10 직접 실행 | T07-P72: 예상값 T07-P072 2 기록 | feature flag와 runtime configuration: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P72/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P72/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · feature flag와 runtime configuration — 내부 메커니즘과 상태 전이

flag owner, scope, default, rollout, expiry를 명시하고 요청마다 어떤 variation이 적용됐는지 관측한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | flag를 1% 사용자에게 먼저 켠다 | source/actor/size를 보존 |
| M2 | 경계 판단 | 점진 적용과 kill switch를 본다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | flag owner, scope, default, rollout, expiry를 명시하고 요청마다 어떤 variation이 적용됐는지 관측한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | flag version/variation, config load 실패, fallback 사용량을 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 사용자 bucket과 rollout percent로 variation을 결정한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`feature flag와 runtime configuration` 흐름을 framework 이름 없이 설명한다.

막히면 `flag를 1% 사용자에게 먼저 켠다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · feature flag와 runtime configuration — 실전 경계 A

**경계 A.** feature flag는 code deploy와 behavior enable 시점을 분리하지만 flag default가 잘못되거나 config service가 unavailable할 때 어떤 값으로 동작할지 정해야 한다.

T07-P72/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P72에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P72/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P72/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P72): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P72/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P72-A1-202 | T07-P72 조건 | feature flag는 code deploy와 behavior enable 시점을 분리하지만 flag default가 잘못되거나 config service가 unavailable할 때 어떤 값으로 동작할지 정해야 한다. |
| T07-P72-A2-203 | T07-P72 변화점 | T07-P72/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P72-A3-204 | T07-P72 반례 | T07-P72/CH06 대표 실패와 A 위반을 구별 |
| T07-P72-A4-205 | T07-P72 근거 | T07-P72/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P72-A5-206 | T07-P72 재실험 | flag를 1% 사용자에게 먼저 켠다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · feature flag와 runtime configuration — 실전 경계 B

**경계 B.** runtime config는 apply 전에 type·range·cross-field validation을 거쳐 invalid timeout·pool size가 production에 즉시 퍼지지 않게 한다.

T07-P72/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P72에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P72/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P72/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P72): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P72/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P72-B1-233 | T07-P72 조건 | runtime config는 apply 전에 type·range·cross-field validation을 거쳐 invalid timeout·pool size가 production에 즉시 퍼지지 않게 한다. |
| T07-P72-B2-234 | T07-P72 독립성 | T07-P72/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P72-B3-235 | T07-P72 상태 | T07-P72/CH03 before·after 위치를 다시 지정 |
| T07-P72-B4-236 | T07-P72 반증 | T07-P72/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P72-B5-237 | T07-P72 적용 | feature flag와 runtime configuration의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · feature flag와 runtime configuration — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **오래된 flag가 중첩돼 같은 code path를 아무도 이해하지 못하거나 설정 오타가 전체 traffic에 퍼진다.**

아래 여섯 사례는 T07-P72의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P72-F01 | T07-P72: 대표 실패 | T07-P72: T07-P72/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P72: 현상만 보고 원인을 확정 | T07-P72/CH08 evidence map에서 상태를 대조 |
| T07-P72-F02 | T07-P72: 경계 A 누락 | T07-P72: T07-P72/CH04 경계 A 위반 입력 | T07-P72: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P72/CH08 evidence map에서 상태를 대조 |
| T07-P72-F03 | T07-P72: 경계 B 누락 | T07-P72: T07-P72/CH05 경계 B 위반 입력 | T07-P72: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P72/CH08 evidence map에서 상태를 대조 |
| T07-P72-F04 | T07-P72: 복구 경계 C 누락 | T07-P72: T07-P72/CH07 경계 C 복구 조건 | T07-P72: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P72/CH08 evidence map에서 상태를 대조 |
| T07-P72-F05 | T07-P72: 운영 경계 D 누락 | T07-P72: T07-P72/CH09 경계 D 운영 조건 | T07-P72: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P72/CH08 evidence map에서 상태를 대조 |
| T07-P72-F06 | T07-P72: 증거 없는 결론 | T07-P72: T07-P72/CH02 첫 판단만 존재 | T07-P72: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P72/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P72/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · feature flag와 runtime configuration — 복구 가능한 상태와 수명

**경계 C.** 점진적으로 1%→10%→100%처럼 enable하고 SLI·error를 비교하면 모든 사용자에게 동시에 위험을 노출하지 않고 rollback/kill switch를 사용할 수 있다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P72에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P72/CH08 evidence map을 본다. 복구 후에는 T07-P72/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P72에서 이미 확정된 side effect는 T07-P72/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P72-R1-295 | T07-P72 중단 직전 | T07-P72/CH03에서 이미 확정된 상태만 표시 |
| T07-P72-R2-296 | T07-P72 재시작 직후 | 점진적으로 1%→10%→100%처럼 enable하고 SLI·error를 비교하면 모든 사용자에게 동시에 위험을 노출하지 않고 rollback/kill switch를 사용할 수 있다. |
| T07-P72-R3-297 | T07-P72 재검증 | T07-P72/CH08 근거로 중복·누락 여부 확인 |
| T07-P72-R4-298 | T07-P72 재실행 | T07-P72/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · feature flag와 runtime configuration — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | flag를 1% 사용자에게 먼저 켠다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | 점진 적용과 kill switch를 본다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | flag owner, scope, default, rollout, expiry를 명시하고 요청마다 어떤 variation이 적용됐는지 관측한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 오래된 flag가 중첩돼 같은 code path를 아무도 이해하지 못하거나 설정 오타가 전체 traffic에 퍼진다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | flag version/variation, config load 실패, fallback 사용량을 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`feature flag와 runtime configuration` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · feature flag와 runtime configuration — 운영 한계와 종료 조건

**경계 D.** stale config가 허용되는 시간과 last-known-good fallback을 명시해 config backend 장애가 application 전체 장애로 증폭되지 않게 한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P72/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P72/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P72 과제: 경계 D와 T07-P72/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P72-O1-357 | synthetic-load=217 | T07-P72/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P72-O2-358 | synthetic-budget=507ms | T07-P72 timeout과 unknown outcome을 분리 |
| T07-P72-O3-359 | T07-P72 종료 | stale config가 허용되는 시간과 last-known-good fallback을 명시해 config backend 장애가 application 전체 장애로 증폭되지 않게 한다. |
| T07-P72-O4-360 | T07-P72 완화 | T07-P72/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · feature flag와 runtime configuration — 직접 실행하는 작은 모델

`feature flag와 runtime configuration` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P072 2`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P072";
const current = 1;
const incoming = [0, 1, 2];
const next = incoming.reduce((state, v) => v > state ? v : state, current);
console.log(marker, next);
```

기준 출력: `T07-P072 2`.

`feature flag와 runtime configuration`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P72-L1-389 | constmarker="T07-P072"; | T07-P72 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P72-L2-390 | constcurrent=1; | T07-P72 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P72-L3-391 | constincoming=[0,1,2]; | T07-P72 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P72-L4-392 | constnext=incoming.reduce((state,v)=>v>state?v:state,current); | T07-P72 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P72-L5-393 | console.log(marker,next); | T07-P72 출력 관측점; 예상 `T07-P072 2`와 비교 |
| T07-P72-LX-478 | T07-P72 실행 기록 | T07-P72 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · feature flag와 runtime configuration — 한 부분만 수정하고 다시 예측

수정 과제: **incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다**.

수정 전은 `T07-P072 2`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P72/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P72-D1-419 | 기준 `T07-P072 2` | T07-P72 수정 전 실행을 먼저 재현 |
| T07-P72-D2-420 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | T07-P72 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P72-D3-421 | T07-P72 새 예측 | T07-P72 실행 전에 출력·상태를 먼저 기록 |
| T07-P72-D4-422 | T07-P72 재실행 | T07-P72/CH10 실제값과 새 예측을 대조 |
| T07-P72-D5-423 | T07-P72 반례 | T07-P72/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P72-D6-424 | T07-P72 근거 | T07-P72/CH08 상태가 설명과 일치해야 완료 |
| T07-P72-D7-425 | T07-P72 이유 | T07-P72 변경 이유를 feature flag와 runtime configuration 계약과 연결해 설명 |

## CHAPTER 12 · feature flag와 runtime configuration — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: feature flag와 runtime configuration | 코드 배포 없이 동작을 바꾸는 flag/config는 편리하지만 또 하나의 상태 시스템이다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P72/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | flag owner, scope, default, rollout, expiry를 명시하고 요청마다 어떤 variation이 적용됐는지 관측한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 오래된 flag가 중첩돼 같은 code path를 아무도 이해하지 못하거나 설정 오타가 전체 traffic에 퍼진다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | flag version/variation, config load 실패, fallback 사용량을 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P72/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P72/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P72/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P72/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `feature flag와 runtime configuration` 실행 코드 수정 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | `feature flag와 runtime configuration` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P72/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P72/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · feature flag와 runtime configuration — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 코드 배포 없이 동작을 바꾸는 flag/config는 편리하지만 또 하나의 상태 시스템이다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P72/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P72/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P72/CH10 실행용 boilerplate·test 후보 | T07-P72: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P072 2` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P72/CH02의 판단 기준과 T07-P72/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P72-AI1-481 | T07-P72 사람 결정 | T07-P72 업무 의미·허용 위험·완료 기준 소유 |
| T07-P72-AI2-482 | T07-P72 AI 초안 | T07-P72/CH10 boilerplate·test 후보까지만 위임 |
| T07-P72-AI3-483 | T07-P72 검증 | T07-P72/CH06 반례와 T07-P72/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · feature flag와 runtime configuration — 경계 조합 실험 8개

T07-P72: T07-P72/CH04~T07-P72/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P72의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-K01 | T07-P72: 경계 A | T07-P72: 경계 B | T07-P72: DISCONNECT[disconnect_ms=96; commit_state=UNKNOWN; request=072-01] / sample=98 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + COMMIT_TIMELINE |
| T07-P72-K02 | T07-P72: 경계 A | T07-P72: 경계 C | T07-P72: RECURRENCE[occurrence=4; interval_s=40; mitigation_applied=1] / sample=16 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + RECURRENCE_TIMELINE |
| T07-P72-K03 | T07-P72: 경계 A | T07-P72: 경계 D | T07-P72: LARGE_INPUT[body_kb=680; limit_kb=512; parsed=0] / sample=23 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + SIZE_LIMIT |
| T07-P72-K04 | T07-P72: 경계 B | T07-P72: 경계 C | T07-P72: DRAIN[ready=0; active=2; drain_deadline_s=9] / sample=30 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + DRAIN_STATE |
| T07-P72-K05 | T07-P72: 경계 B | T07-P72: 경계 D | T07-P72: RECOVERY_SCOPE[selected=63; expected=17; backup=1; dry_run=1] / sample=37 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + RECOVERY_AUDIT |
| T07-P72-K06 | T07-P72: 경계 C | T07-P72: 경계 D | T07-P72: UNKNOWN_OUTCOME[timeout_ms=154; provider_state=UNKNOWN; lookup_id=p07206] / sample=44 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + PROVIDER_RESULT |
| T07-P72-K07 | T07-P72: 경계 A | T07-P72: 경계 B+C | T07-P72: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] / sample=51 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + DURABLE_STATE |
| T07-P72-K08 | T07-P72: 경계 B | T07-P72: 경계 C+D | T07-P72: OVERLOAD[rps=428; p99_ms=930; queue=28] / sample=58 | T07-P72: 먼저 깨지는 경계를 판정 | T07-P72: T07-P72/CH08 + QUEUE_PRESSURE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P72/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · feature flag와 runtime configuration — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P72와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-B11 | T07-P72: incident를 증거로 좁히는 요청 경로 디버깅 | T07-P72: request id 하나가 500으로 끝난다 | T07-P72: timeline으로 첫 실패 지점을 찾는다 | T07-P72: T07-P72/CH08 증거와 형제 LESSON 증거를 분리 | T07-P72: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P72-B13 | T07-P72: dependency outage와 fallback·stale data | T07-P72: dependency가 5분 동안 unavailable이다 | T07-P72: stale/fail-closed와 breaker를 고른다 | T07-P72: T07-P72/CH08 증거와 형제 LESSON 증거를 분리 | T07-P72: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P72-B10 | T07-P72: event-loop stall과 CPU-bound work | T07-P72: CPU loop 하나가 모든 요청을 늦춘다 | T07-P72: event-loop lag와 profile을 연결한다 | T07-P72: T07-P72/CH08 증거와 형제 LESSON 증거를 분리 | T07-P72: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P72-B14 | T07-P72: 데이터 무결성 이상을 runtime에서 감지하기 | T07-P72: 완료 job인데 result가 없다 | T07-P72: runtime invariant와 reconciliation을 실행한다 | T07-P72: T07-P72/CH08 증거와 형제 LESSON 증거를 분리 | T07-P72: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P72-B09 | T07-P72: memory leak과 resource leak | T07-P72: GC 뒤 heap 바닥선이 계속 오른다 | T07-P72: leak trend와 정상 cache를 구분한다 | T07-P72: T07-P72/CH08 증거와 형제 LESSON 증거를 분리 | T07-P72: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · feature flag와 runtime configuration — 선택형 실패 주입 6개

T07-P72: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P72 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-X01 | T07-P72: 대형 입력 | T07-P72: 입력 크기가 정상의 100배; sample=255 | T07-P72: 의미 검증과 resource limit | T07-P72: T07-P72/CH02 판단과 별도 기록 | T07-P72: body/batch size·parse time·memory·reject status |
| T07-P72-X02 | T07-P72: drain | T07-P72: 배포 중 기존 요청이 처리 중; sample=272 | T07-P72: 새 traffic 차단과 in-flight 처리 | T07-P72: T07-P72/CH02 판단과 별도 기록 | T07-P72: readiness·active requests·deadline·final state |
| T07-P72-X03 | T07-P72: 복구 범위 | T07-P72: 복구 script 대상이 예상보다 큼; sample=289 | T07-P72: 진단과 destructive recovery | T07-P72: T07-P72/CH02 판단과 별도 기록 | T07-P72: selected ids/count·backup·audit trail |
| T07-P72-X04 | T07-P72: unknown outcome | T07-P72: dependency timeout 후 성공 여부 불명; sample=306 | T07-P72: 실패와 미확정 결과 | T07-P72: T07-P72/CH02 판단과 별도 기록 | T07-P72: provider id·조회 결과·retry history |
| T07-P72-X05 | T07-P72: 재시작 | T07-P72: side effect 직후 process가 재시작됨; sample=323 | T07-P72: durable state와 memory state | T07-P72: T07-P72/CH02 판단과 별도 기록 | T07-P72: commit·outbox·job id·restart 전후 상태 |
| T07-P72-X06 | T07-P72: 과부하 | T07-P72: traffic 세 배, p99 급증; sample=340 | T07-P72: 기능 실패와 saturation | T07-P72: T07-P72/CH02 판단과 별도 기록 | T07-P72: queue age·pool wait·CPU/event-loop·quota |

## CHAPTER 17 · feature flag와 runtime configuration — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P72에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-E01 | T07-P72: 대표 실패를 원인으로 착각 | T07-P72: T07-P72/CH06 실패 case를 다른 입력으로 재현 | T07-P72: 현상과 원인을 같은 것으로 봄 | T07-P72: T07-P72/CH06 대표 실패와 T07-P72/CH08 증거를 다시 대조 | T07-P72: T07-P72/CH08 |
| T07-P72-E02 | T07-P72: 경계 A 생략 | T07-P72: T07-P72/CH04의 조건 하나를 반대로 설정 | T07-P72: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P72: T07-P72/CH04를 새 입력에 적용 | T07-P72: T07-P72/CH08 |
| T07-P72-E03 | T07-P72: 경계 B 생략 | T07-P72: T07-P72/CH05의 조건 하나를 반대로 설정 | T07-P72: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P72: T07-P72/CH05를 새 입력에 적용 | T07-P72: T07-P72/CH08 |
| T07-P72-E04 | T07-P72: 복구 상태 혼동 | T07-P72: T07-P72/CH07에서 처리 중단을 주입 | T07-P72: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P72: T07-P72/CH07에서 수명 경계를 다시 표시 | T07-P72: T07-P72/CH08 |
| T07-P72-E05 | T07-P72: 운영 한계 누락 | T07-P72: T07-P72/CH09에서 부하 또는 drain 조건을 변경 | T07-P72: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P72: T07-P72/CH09의 종료 조건을 다시 작성 | T07-P72: T07-P72/CH08 |
| T07-P72-E06 | T07-P72: 증거 없는 성공 판정 | T07-P72: T07-P72/CH08에서 증거 하나를 숨김 | T07-P72: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P72: T07-P72/CH08에서 독립 증거 둘을 선택 | T07-P72: T07-P72/CH08 |

## CHAPTER 18 · feature flag와 runtime configuration — synthetic 관측값 판독 6개

T07-P72: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P72의 숫자 하나만으로 원인을 단정하지 않고 T07-P72/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-O01 | T07-P72/error_rate | 427 | T07-P72: 오류율 | T07-P72: 축=재시작; 원인 확정 금지 | T07-P72: DURABLE_STATE + T07-P72/CH08 |
| T07-P72-O02 | T07-P72/p99_ms | 444 | T07-P72: p99 지연 | T07-P72: 축=과부하; 원인 확정 금지 | T07-P72: QUEUE_PRESSURE + T07-P72/CH08 |
| T07-P72-O03 | T07-P72/queue_depth | 461 | T07-P72: 대기열 깊이 | T07-P72: 축=sampling; 원인 확정 금지 | T07-P72: TRACE_METRIC_CROSSCHECK + T07-P72/CH08 |
| T07-P72-O04 | T07-P72/heap_mb | 478 | T07-P72: heap 사용량 | T07-P72: 축=client disconnect; 원인 확정 금지 | T07-P72: COMMIT_TIMELINE + T07-P72/CH08 |
| T07-P72-O05 | T07-P72/event_loop_lag_ms | 495 | T07-P72: event-loop 지연 | T07-P72: 축=재발; 원인 확정 금지 | T07-P72: RECURRENCE_TIMELINE + T07-P72/CH08 |
| T07-P72-O06 | T07-P72/dependency_errors | 512 | T07-P72: dependency 오류 수 | T07-P72: 축=대형 입력; 원인 확정 금지 | T07-P72: SIZE_LIMIT + T07-P72/CH08 |

## CHAPTER 19 · feature flag와 runtime configuration — 선택형 코드 리뷰 6질문

T07-P72: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P72에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-R01 | T07-P72: 입력 경계 | T07-P72: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P72: OVERLOAD[rps=797; p99_ms=867; queue=13] | T07-P72: T07-P72/CH04 | T07-P72: QUEUE_PRESSURE |
| T07-P72-R02 | T07-P72: 순서 | T07-P72: old/new event 순서가 바뀌어도 안전한가 | T07-P72: SAMPLING[sample_rate=86%; trace_present=1; metric_present=1] | T07-P72: T07-P72/CH05 | T07-P72: TRACE_METRIC_CROSSCHECK |
| T07-P72-R03 | T07-P72: retry | T07-P72: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P72: DISCONNECT[disconnect_ms=109; commit_state=UNKNOWN; request=072-02] | T07-P72: T07-P72/CH06 | T07-P72: COMMIT_TIMELINE |
| T07-P72-R04 | T07-P72: 민감정보 | T07-P72: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P72: RECURRENCE[occurrence=5; interval_s=51; mitigation_applied=1] | T07-P72: T07-P72/CH07 | T07-P72: RECURRENCE_TIMELINE |
| T07-P72-R05 | T07-P72: 입력 경계 | T07-P72: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P72: LARGE_INPUT[body_kb=768; limit_kb=512; parsed=0] | T07-P72: T07-P72/CH04 | T07-P72: SIZE_LIMIT |
| T07-P72-R06 | T07-P72: 순서 | T07-P72: old/new event 순서가 바뀌어도 안전한가 | T07-P72: DRAIN[ready=0; active=15; drain_deadline_s=10] | T07-P72: T07-P72/CH05 | T07-P72: DRAIN_STATE |

## CHAPTER 20 · feature flag와 runtime configuration — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P72에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-I01 | T07-P72: 영향 범위 | T07-P72: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P72: SAMPLING[sample_rate=73%; trace_present=0; metric_present=1] | T07-P72: T07-P72/CH08 + TRACE_METRIC_CROSSCHECK | T07-P72-incident-377 |
| T07-P72-I02 | T07-P72: 변경 동결 | T07-P72: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P72: DISCONNECT[disconnect_ms=96; commit_state=UNKNOWN; request=072-01] | T07-P72: T07-P72/CH08 + COMMIT_TIMELINE | T07-P72-incident-378 |
| T07-P72-I03 | T07-P72: correlation | T07-P72: 한 request/job/resource id를 시간축에 고정 | T07-P72: RECURRENCE[occurrence=4; interval_s=40; mitigation_applied=1] | T07-P72: T07-P72/CH08 + RECURRENCE_TIMELINE | T07-P72-incident-379 |
| T07-P72-I04 | T07-P72: 마지막 정상 | T07-P72: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P72: LARGE_INPUT[body_kb=680; limit_kb=512; parsed=0] | T07-P72: T07-P72/CH08 + SIZE_LIMIT | T07-P72-incident-380 |
| T07-P72-I05 | T07-P72: 가설 검증 | T07-P72: 원인 후보 하나만 뒤집어 재현 | T07-P72: DRAIN[ready=0; active=2; drain_deadline_s=9] | T07-P72: T07-P72/CH08 + DRAIN_STATE | T07-P72-incident-381 |
| T07-P72-I06 | T07-P72: 복구 확인 | T07-P72: durable state와 사용자 결과를 모두 확인 | T07-P72: RECOVERY_SCOPE[selected=63; expected=17; backup=1; dry_run=1] | T07-P72: T07-P72/CH08 + RECOVERY_AUDIT | T07-P72-incident-382 |

## CHAPTER 21 · feature flag와 runtime configuration — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P72/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P72/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P72/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P72/CH04~T07-P72/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P72/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P72/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P72/CH18 signal 두 개와 T07-P72/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P72/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P72/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · feature flag와 runtime configuration — 통합 casebook 16문제

T07-P72 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P72 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P72-C01 | T07-P72: 경계 A | T07-P72: DISCONNECT[disconnect_ms=83; commit_state=UNKNOWN; request=072-00] | T07-P72: load=388, window=64s | T07-P72: review=입력 경계 | T07-P72: 경계 A 위반 여부를 판정 | T07-P72: COMMIT_TIMELINE + T07-P72/CH08 |
| T07-P72-C02 | T07-P72: 경계 B | T07-P72: RECURRENCE[occurrence=3; interval_s=240; mitigation_applied=1] | T07-P72: load=411, window=83s | T07-P72: review=timeout | T07-P72: 경계 B 위반 여부를 판정 | T07-P72: RECURRENCE_TIMELINE + T07-P72/CH08 |
| T07-P72-C03 | T07-P72: 경계 C | T07-P72: LARGE_INPUT[body_kb=592; limit_kb=512; parsed=0] | T07-P72: load=434, window=12s | T07-P72: review=자원 | T07-P72: 경계 C 위반 여부를 판정 | T07-P72: SIZE_LIMIT + T07-P72/CH08 |
| T07-P72-C04 | T07-P72: 경계 D | T07-P72: DRAIN[ready=0; active=6; drain_deadline_s=8] | T07-P72: load=457, window=31s | T07-P72: review=순서 | T07-P72: 경계 D 위반 여부를 판정 | T07-P72: DRAIN_STATE + T07-P72/CH08 |
| T07-P72-C05 | T07-P72: 경계 A | T07-P72: RECOVERY_SCOPE[selected=52; expected=23; backup=1; dry_run=0] | T07-P72: load=480, window=50s | T07-P72: review=관측 | T07-P72: 경계 A 위반 여부를 판정 | T07-P72: RECOVERY_AUDIT + T07-P72/CH08 |
| T07-P72-C06 | T07-P72: 경계 B | T07-P72: UNKNOWN_OUTCOME[timeout_ms=143; provider_state=UNKNOWN; lookup_id=p07205] | T07-P72: load=503, window=69s | T07-P72: review=상태 변경 | T07-P72: 경계 B 위반 여부를 판정 | T07-P72: PROVIDER_RESULT + T07-P72/CH08 |
| T07-P72-C07 | T07-P72: 경계 C | T07-P72: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P72: load=526, window=88s | T07-P72: review=retry | T07-P72: 경계 C 위반 여부를 판정 | T07-P72: DURABLE_STATE + T07-P72/CH08 |
| T07-P72-C08 | T07-P72: 경계 D | T07-P72: OVERLOAD[rps=395; p99_ms=813; queue=34] | T07-P72: load=549, window=17s | T07-P72: review=복구 | T07-P72: 경계 D 위반 여부를 판정 | T07-P72: QUEUE_PRESSURE + T07-P72/CH08 |
| T07-P72-C09 | T07-P72: 경계 A | T07-P72: SAMPLING[sample_rate=80%; trace_present=0; metric_present=1] | T07-P72: load=572, window=36s | T07-P72: review=동시성 | T07-P72: 경계 A 위반 여부를 판정 | T07-P72: TRACE_METRIC_CROSSCHECK + T07-P72/CH08 |
| T07-P72-C10 | T07-P72: 경계 B | T07-P72: DISCONNECT[disconnect_ms=103; commit_state=UNKNOWN; request=072-09] | T07-P72: load=595, window=55s | T07-P72: review=민감정보 | T07-P72: 경계 B 위반 여부를 판정 | T07-P72: COMMIT_TIMELINE + T07-P72/CH08 |
| T07-P72-C11 | T07-P72: 경계 C | T07-P72: RECURRENCE[occurrence=2; interval_s=128; mitigation_applied=1] | T07-P72: load=618, window=74s | T07-P72: review=중복 | T07-P72: 경계 C 위반 여부를 판정 | T07-P72: RECURRENCE_TIMELINE + T07-P72/CH08 |
| T07-P72-C12 | T07-P72: 경계 D | T07-P72: LARGE_INPUT[body_kb=1384; limit_kb=512; parsed=0] | T07-P72: load=641, window=93s | T07-P72: review=권한 | T07-P72: 경계 D 위반 여부를 판정 | T07-P72: SIZE_LIMIT + T07-P72/CH08 |
| T07-P72-C13 | T07-P72: 경계 A | T07-P72: DRAIN[ready=0; active=9; drain_deadline_s=9] | T07-P72: load=664, window=22s | T07-P72: review=입력 경계 | T07-P72: 경계 A 위반 여부를 판정 | T07-P72: DRAIN_STATE + T07-P72/CH08 |
| T07-P72-C14 | T07-P72: 경계 B | T07-P72: RECOVERY_SCOPE[selected=151; expected=5; backup=1; dry_run=1] | T07-P72: load=687, window=41s | T07-P72: review=timeout | T07-P72: 경계 B 위반 여부를 판정 | T07-P72: RECOVERY_AUDIT + T07-P72/CH08 |
| T07-P72-C15 | T07-P72: 경계 C | T07-P72: UNKNOWN_OUTCOME[timeout_ms=242; provider_state=UNKNOWN; lookup_id=p07214] | T07-P72: load=710, window=60s | T07-P72: review=자원 | T07-P72: 경계 C 위반 여부를 판정 | T07-P72: PROVIDER_RESULT + T07-P72/CH08 |
| T07-P72-C16 | T07-P72: 경계 D | T07-P72: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P72: load=733, window=79s | T07-P72: review=순서 | T07-P72: 경계 D 위반 여부를 판정 | T07-P72: DURABLE_STATE + T07-P72/CH08 |

채점은 결론보다 근거를 본다. T07-P72/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · feature flag와 runtime configuration — evidence 판독 문제 14개

T07-P72 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P72 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P72-V01 | T07-P72: latency=1172ms; queue=24; retry=2 | T07-P72: 재발 | T07-P72: 축=재발; 원인 확정은 보류 | T07-P72: RECURRENCE_TIMELINE + T07-P72/CH08 | T07-P72: 피할 오판=운영 한계 누락 |
| T07-P72-V02 | T07-P72: latency=1239ms; queue=35; retry=5 | T07-P72: 대형 입력 | T07-P72: 축=대형 입력; 원인 확정은 보류 | T07-P72: SIZE_LIMIT + T07-P72/CH08 | T07-P72: 피할 오판=오류 합치기 |
| T07-P72-V03 | T07-P72: latency=1306ms; queue=46; retry=1 | T07-P72: drain | T07-P72: 축=drain; 원인 확정은 보류 | T07-P72: DRAIN_STATE + T07-P72/CH08 | T07-P72: 피할 오판=복구 과잉 |
| T07-P72-V04 | T07-P72: latency=1373ms; queue=57; retry=4 | T07-P72: 복구 범위 | T07-P72: 축=복구 범위; 원인 확정은 보류 | T07-P72: RECOVERY_AUDIT + T07-P72/CH08 | T07-P72: 피할 오판=경계 누락 |
| T07-P72-V05 | T07-P72: latency=1440ms; queue=68; retry=0 | T07-P72: unknown outcome | T07-P72: 축=unknown outcome; 원인 확정은 보류 | T07-P72: PROVIDER_RESULT + T07-P72/CH08 | T07-P72: 피할 오판=증거 혼동 |
| T07-P72-V06 | T07-P72: latency=1507ms; queue=79; retry=3 | T07-P72: 재시작 | T07-P72: 축=재시작; 원인 확정은 보류 | T07-P72: DURABLE_STATE + T07-P72/CH08 | T07-P72: 피할 오판=재시도 오판 |
| T07-P72-V07 | T07-P72: latency=1574ms; queue=10; retry=6 | T07-P72: 과부하 | T07-P72: 축=과부하; 원인 확정은 보류 | T07-P72: QUEUE_PRESSURE + T07-P72/CH08 | T07-P72: 피할 오판=동시성 무시 |
| T07-P72-V08 | T07-P72: latency=1641ms; queue=21; retry=2 | T07-P72: sampling | T07-P72: 축=sampling; 원인 확정은 보류 | T07-P72: TRACE_METRIC_CROSSCHECK + T07-P72/CH08 | T07-P72: 피할 오판=순서 가정 |
| T07-P72-V09 | T07-P72: latency=1708ms; queue=32; retry=5 | T07-P72: client disconnect | T07-P72: 축=client disconnect; 원인 확정은 보류 | T07-P72: COMMIT_TIMELINE + T07-P72/CH08 | T07-P72: 피할 오판=상태 수명 혼동 |
| T07-P72-V10 | T07-P72: latency=1775ms; queue=43; retry=1 | T07-P72: 재발 | T07-P72: 축=재발; 원인 확정은 보류 | T07-P72: RECURRENCE_TIMELINE + T07-P72/CH08 | T07-P72: 피할 오판=운영 한계 누락 |
| T07-P72-V11 | T07-P72: latency=42ms; queue=54; retry=4 | T07-P72: 대형 입력 | T07-P72: 축=대형 입력; 원인 확정은 보류 | T07-P72: SIZE_LIMIT + T07-P72/CH08 | T07-P72: 피할 오판=오류 합치기 |
| T07-P72-V12 | T07-P72: latency=109ms; queue=65; retry=0 | T07-P72: drain | T07-P72: 축=drain; 원인 확정은 보류 | T07-P72: DRAIN_STATE + T07-P72/CH08 | T07-P72: 피할 오판=복구 과잉 |
| T07-P72-V13 | T07-P72: latency=176ms; queue=76; retry=3 | T07-P72: 복구 범위 | T07-P72: 축=복구 범위; 원인 확정은 보류 | T07-P72: RECOVERY_AUDIT + T07-P72/CH08 | T07-P72: 피할 오판=경계 누락 |
| T07-P72-V14 | T07-P72: latency=243ms; queue=7; retry=6 | T07-P72: unknown outcome | T07-P72: 축=unknown outcome; 원인 확정은 보류 | T07-P72: PROVIDER_RESULT + T07-P72/CH08 | T07-P72: 피할 오판=증거 혼동 |

## CHAPTER 24 · feature flag와 runtime configuration — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P72에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P72-D01 | T07-P72: schema 변경 | T07-P72: 필드 이름·형식·required 조건을 바꾼다 | T07-P72: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P72: DRAIN[ready=0; active=13; drain_deadline_s=5] | T07-P72: DRAIN_STATE + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P72-D02 | T07-P72: 결과 합치기 | T07-P72: 여러 오류를 하나의 status/error code로 합친다 | T07-P72: client 행동과 retry 가능성을 잃지 않는지 | T07-P72: RECOVERY_SCOPE[selected=230; expected=5; backup=1; dry_run=1] | T07-P72: RECOVERY_AUDIT + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P72-D03 | T07-P72: retry 추가 | T07-P72: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P72: unknown outcome과 duplicate side effect를 구분하는지 | T07-P72: UNKNOWN_OUTCOME[timeout_ms=110; provider_state=UNKNOWN; lookup_id=p07202] | T07-P72: PROVIDER_RESULT + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P72-D04 | T07-P72: 로그 확대 | T07-P72: debug를 위해 payload와 context 기록을 늘린다 | T07-P72: secret·PII·cardinality 비용을 통제하는지 | T07-P72: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P72: DURABLE_STATE + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P72-D05 | T07-P72: 강제 종료 | T07-P72: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P72: in-flight request와 background work의 결과를 잃는지 | T07-P72: OVERLOAD[rps=296; p99_ms=462; queue=32] | T07-P72: QUEUE_PRESSURE + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P72-D06 | T07-P72: AI package 추가 | T07-P72: AI가 제안한 새 dependency를 도입한다 | T07-P72: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P72: SAMPLING[sample_rate=41%; trace_present=1; metric_present=1] | T07-P72: TRACE_METRIC_CROSSCHECK + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P72-D07 | T07-P72: 비동기화 | T07-P72: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P72: durability·status API·worker retry 계약이 생기는지 | T07-P72: DISCONNECT[disconnect_ms=64; commit_state=UNKNOWN; request=072-06] | T07-P72: COMMIT_TIMELINE + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P72-D08 | T07-P72: 권한 shortcut | T07-P72: payload의 owner/tenant id를 바로 사용한다 | T07-P72: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P72: RECURRENCE[occurrence=4; interval_s=95; mitigation_applied=1] | T07-P72: RECURRENCE_TIMELINE + T07-P72/CH08 | T07-P72: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P72: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · feature flag와 runtime configuration — 최종 contract와 evidence spine

**최종 계약:** 코드 배포 없이 동작을 바꾸는 flag/config는 편리하지만 또 하나의 상태 시스템이다.

**정상 메커니즘:** flag owner, scope, default, rollout, expiry를 명시하고 요청마다 어떤 variation이 적용됐는지 관측한다.

**대표 실패:** 오래된 flag가 중첩돼 같은 code path를 아무도 이해하지 못하거나 설정 오타가 전체 traffic에 퍼진다.

**검증 evidence:** flag version/variation, config load 실패, fallback 사용량을 본다.

**직접 행동:** 사용자 bucket과 rollout percent로 variation을 결정한다.

**다음 연결:** `dependency outage와 fallback·stale data`.

`feature flag와 runtime configuration`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| SRE | SRE | feature flag와 runtime configuration의 개념·실패·운영 판단 교차 확인 |
| SRE-WORKBOOK | SRE-WORKBOOK | feature flag와 runtime configuration의 개념·실패·운영 판단 교차 확인 |
| OTEL-SEMCONV | OpenTelemetry Semantic Conventions | feature flag와 runtime configuration의 개념·실패·운영 판단 교차 확인 |
| NODE-DOCS | Node.js Documentation | feature flag와 runtime configuration의 개념·실패·운영 판단 교차 확인 |

`feature flag와 runtime configuration` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
