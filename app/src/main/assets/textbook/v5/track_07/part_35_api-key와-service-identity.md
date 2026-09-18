# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 03 · 인증·권한·신뢰 경계를 백엔드 흐름에 넣기

### LESSON 05 · API key와 service identity

## CHAPTER 01 · API key와 service identity — 쉬운 말에서 정확한 계약까지

**출발 개념.** 사람 사용자 외에도 서버·배치·외부 파트너 같은 program 주체를 식별할 credential이 필요하다.

**아주 쉬운 사건.** service key가 필요 이상의 scope를 가진다. 이 사건에서는 먼저 **caller identity와 least privilege를 본다**.

**왜 필요한가.** 정상 동작은 key 자체는 secret로 저장하고 서버에서는 hash/identifier로 lookup하며 scope와 rotation 상태를 함께 검사한다. 반대로 하나의 master key를 모든 서비스가 공유하거나 source code/log에 key 원문이 남는다.

**암기:** `API key와 service identity`의 역할 한 줄.

**직접 이해:** `API key와 service identity`의 입력·상태·결과 경계.

**AI 위임 가능:** `API key와 service identity` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `service key가 필요 이상의 scope를 가진다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 key id, scope, last_used, rotation/revocation 상태를 보되 secret 값은 관측 데이터에 넣지 않는다.

## CHAPTER 02 · API key와 service identity — 아주 쉬운 예를 한 단계씩 해석

T07-P35: `service key가 필요 이상의 scope를 가진다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | service key가 필요 이상의 scope를 가진다 | T07-P35 외부 입력 | T07-P35: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | caller identity와 least privilege를 본다 | T07-P35 판단 기준 | T07-P35/CH08 관측표와 대조 |
| 정상 경로 | T07-P35/CH03 M1→M5 | API key와 service identity: 완료 시점을 단계별로 분리 | T07-P35: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P35/CH06 하나의 master key를 모든 서비스가 공유하거나 source code/log에 key 원문이 남는다. | T07-P35: 깨진 계약 하나를 특정 | API key와 service identity: 증상과 원인을 분리 |
| 재검증 | T07-P35/CH10 직접 실행 | T07-P35: 예상값 T07-P035 5 기록 | API key와 service identity: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P35/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P35/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · API key와 service identity — 내부 메커니즘과 상태 전이

key 자체는 secret로 저장하고 서버에서는 hash/identifier로 lookup하며 scope와 rotation 상태를 함께 검사한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | service key가 필요 이상의 scope를 가진다 | source/actor/size를 보존 |
| M2 | 경계 판단 | caller identity와 least privilege를 본다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | key 자체는 secret로 저장하고 서버에서는 hash/identifier로 lookup하며 scope와 rotation 상태를 함께 검사한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | key id, scope, last_used, rotation/revocation 상태를 보되 secret 값은 관측 데이터에 넣지 않는다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | key metadata와 필요한 scope를 비교해 사용 가능 여부를 판정한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`API key와 service identity` 흐름을 framework 이름 없이 설명한다.

막히면 `service key가 필요 이상의 scope를 가진다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · API key와 service identity — 실전 경계 A

**경계 A.** API key는 보통 application/service caller를 식별하는 credential이지 그 요청 안의 최종 human user를 자동으로 증명하지 않으므로 두 identity를 섞지 않는다.

T07-P35/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P35에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P35/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P35/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P35): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P35/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P35-A1-73 | T07-P35 조건 | API key는 보통 application/service caller를 식별하는 credential이지 그 요청 안의 최종 human user를 자동으로 증명하지 않으므로 두 identity를 섞지 않는다. |
| T07-P35-A2-74 | T07-P35 변화점 | T07-P35/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P35-A3-75 | T07-P35 반례 | T07-P35/CH06 대표 실패와 A 위반을 구별 |
| T07-P35-A4-76 | T07-P35 근거 | T07-P35/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P35-A5-77 | T07-P35 재실험 | service key가 필요 이상의 scope를 가진다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · API key와 service identity — 실전 경계 B

**경계 B.** key마다 허용 scope·endpoint·환경을 좁혀 하나가 유출돼도 blast radius가 전체 시스템으로 커지지 않게 least privilege를 적용한다.

T07-P35/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P35에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P35/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P35/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P35): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P35/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P35-B1-104 | T07-P35 조건 | key마다 허용 scope·endpoint·환경을 좁혀 하나가 유출돼도 blast radius가 전체 시스템으로 커지지 않게 least privilege를 적용한다. |
| T07-P35-B2-105 | T07-P35 독립성 | T07-P35/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P35-B3-106 | T07-P35 상태 | T07-P35/CH03 before·after 위치를 다시 지정 |
| T07-P35-B4-107 | T07-P35 반증 | T07-P35/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P35-B5-108 | T07-P35 적용 | API key와 service identity의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · API key와 service identity — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **하나의 master key를 모든 서비스가 공유하거나 source code/log에 key 원문이 남는다.**

아래 여섯 사례는 T07-P35의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P35-F01 | T07-P35: 대표 실패 | T07-P35: T07-P35/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P35: 현상만 보고 원인을 확정 | T07-P35/CH08 evidence map에서 상태를 대조 |
| T07-P35-F02 | T07-P35: 경계 A 누락 | T07-P35: T07-P35/CH04 경계 A 위반 입력 | T07-P35: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P35/CH08 evidence map에서 상태를 대조 |
| T07-P35-F03 | T07-P35: 경계 B 누락 | T07-P35: T07-P35/CH05 경계 B 위반 입력 | T07-P35: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P35/CH08 evidence map에서 상태를 대조 |
| T07-P35-F04 | T07-P35: 복구 경계 C 누락 | T07-P35: T07-P35/CH07 경계 C 복구 조건 | T07-P35: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P35/CH08 evidence map에서 상태를 대조 |
| T07-P35-F05 | T07-P35: 운영 경계 D 누락 | T07-P35: T07-P35/CH09 경계 D 운영 조건 | T07-P35: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P35/CH08 evidence map에서 상태를 대조 |
| T07-P35-F06 | T07-P35: 증거 없는 결론 | T07-P35: T07-P35/CH02 첫 판단만 존재 | T07-P35: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P35/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P35/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · API key와 service identity — 복구 가능한 상태와 수명

**경계 C.** rotation 중에는 old/new key를 짧게 overlap시켜 client 전환을 허용할 수 있지만 사용량을 측정해 old key가 실제로 사라진 뒤 폐기한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P35에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P35/CH08 evidence map을 본다. 복구 후에는 T07-P35/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P35에서 이미 확정된 side effect는 T07-P35/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P35-R1-166 | T07-P35 중단 직전 | T07-P35/CH03에서 이미 확정된 상태만 표시 |
| T07-P35-R2-167 | T07-P35 재시작 직후 | rotation 중에는 old/new key를 짧게 overlap시켜 client 전환을 허용할 수 있지만 사용량을 측정해 old key가 실제로 사라진 뒤 폐기한다. |
| T07-P35-R3-168 | T07-P35 재검증 | T07-P35/CH08 근거로 중복·누락 여부 확인 |
| T07-P35-R4-169 | T07-P35 재실행 | T07-P35/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · API key와 service identity — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | service key가 필요 이상의 scope를 가진다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | caller identity와 least privilege를 본다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | key 자체는 secret로 저장하고 서버에서는 hash/identifier로 lookup하며 scope와 rotation 상태를 함께 검사한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 하나의 master key를 모든 서비스가 공유하거나 source code/log에 key 원문이 남는다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | key id, scope, last_used, rotation/revocation 상태를 보되 secret 값은 관측 데이터에 넣지 않는다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`API key와 service identity` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · API key와 service identity — 운영 한계와 종료 조건

**경계 D.** X-Service-Name 같은 일반 header를 caller identity로 믿지 않고 TLS/client credential/gateway 검증처럼 실제 신뢰 경로에서 설정된 identity만 사용한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P35/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P35/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P35 과제: 경계 D와 T07-P35/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P35-O1-228 | synthetic-load=88 | T07-P35/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P35-O2-229 | synthetic-budget=378ms | T07-P35 timeout과 unknown outcome을 분리 |
| T07-P35-O3-230 | T07-P35 종료 | X-Service-Name 같은 일반 header를 caller identity로 믿지 않고 TLS/client credential/gateway 검증처럼 실제 신뢰 경로에서 설정된 identity만 사용한다. |
| T07-P35-O4-231 | T07-P35 완화 | T07-P35/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · API key와 service identity — 직접 실행하는 작은 모델

`API key와 service identity` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P035 5`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P035";
const current = 4;
const incoming = [3, 4, 5];
const next = incoming.reduce((state, v) => v > state ? v : state, current);
console.log(marker, next);
```

기준 출력: `T07-P035 5`.

`API key와 service identity`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P35-L1-260 | constmarker="T07-P035"; | T07-P35 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P35-L2-261 | constcurrent=4; | T07-P35 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P35-L3-262 | constincoming=[3,4,5]; | T07-P35 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P35-L4-263 | constnext=incoming.reduce((state,v)=>v>state?v:state,current); | T07-P35 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P35-L5-264 | console.log(marker,next); | T07-P35 출력 관측점; 예상 `T07-P035 5`와 비교 |
| T07-P35-LX-349 | T07-P35 실행 기록 | T07-P35 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · API key와 service identity — 한 부분만 수정하고 다시 예측

수정 과제: **incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다**.

수정 전은 `T07-P035 5`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P35/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P35-D1-290 | 기준 `T07-P035 5` | T07-P35 수정 전 실행을 먼저 재현 |
| T07-P35-D2-291 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | T07-P35 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P35-D3-292 | T07-P35 새 예측 | T07-P35 실행 전에 출력·상태를 먼저 기록 |
| T07-P35-D4-293 | T07-P35 재실행 | T07-P35/CH10 실제값과 새 예측을 대조 |
| T07-P35-D5-294 | T07-P35 반례 | T07-P35/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P35-D6-295 | T07-P35 근거 | T07-P35/CH08 상태가 설명과 일치해야 완료 |
| T07-P35-D7-296 | T07-P35 이유 | T07-P35 변경 이유를 API key와 service identity 계약과 연결해 설명 |

## CHAPTER 12 · API key와 service identity — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: API key와 service identity | 사람 사용자 외에도 서버·배치·외부 파트너 같은 program 주체를 식별할 credential이 필요하다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P35/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | key 자체는 secret로 저장하고 서버에서는 hash/identifier로 lookup하며 scope와 rotation 상태를 함께 검사한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 하나의 master key를 모든 서비스가 공유하거나 source code/log에 key 원문이 남는다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | key id, scope, last_used, rotation/revocation 상태를 보되 secret 값은 관측 데이터에 넣지 않는다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P35/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P35/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P35/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P35/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `API key와 service identity` 실행 코드 수정 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | `API key와 service identity` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P35/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P35/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · API key와 service identity — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 사람 사용자 외에도 서버·배치·외부 파트너 같은 program 주체를 식별할 credential이 필요하다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P35/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P35/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P35/CH10 실행용 boilerplate·test 후보 | T07-P35: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P035 5` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P35/CH02의 판단 기준과 T07-P35/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P35-AI1-352 | T07-P35 사람 결정 | T07-P35 업무 의미·허용 위험·완료 기준 소유 |
| T07-P35-AI2-353 | T07-P35 AI 초안 | T07-P35/CH10 boilerplate·test 후보까지만 위임 |
| T07-P35-AI3-354 | T07-P35 검증 | T07-P35/CH06 반례와 T07-P35/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · API key와 service identity — 경계 조합 실험 8개

T07-P35: T07-P35/CH04~T07-P35/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P35의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-K01 | T07-P35: 경계 A | T07-P35: 경계 B | T07-P35: RECOVERY_SCOPE[selected=212; expected=15; backup=1; dry_run=1] / sample=47 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + RECOVERY_AUDIT |
| T07-P35-K02 | T07-P35: 경계 A | T07-P35: 경계 C | T07-P35: REPLAY[key=cmd-035-02; attempts=4; response_seen=0] / sample=54 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + IDEMPOTENCY_RECORD |
| T07-P35-K03 | T07-P35: 경계 A | T07-P35: 경계 D | T07-P35: OLD_SCHEMA[client=v3; server=v4; extra_field=1] / sample=61 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + CLIENT_VERSION |
| T07-P35-K04 | T07-P35: 경계 B | T07-P35: 경계 C | T07-P35: UNKNOWN_OUTCOME[timeout_ms=114; provider_state=UNKNOWN; lookup_id=p03504] / sample=68 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + PROVIDER_RESULT |
| T07-P35-K05 | T07-P35: 경계 B | T07-P35: 경계 D | T07-P35: OVERLOAD[rps=275; p99_ms=1029; queue=16] / sample=75 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + QUEUE_PRESSURE |
| T07-P35-K06 | T07-P35: 경계 C | T07-P35: 경계 D | T07-P35: SAMPLING[sample_rate=24%; trace_present=0; metric_present=1] / sample=82 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P35-K07 | T07-P35: 경계 A | T07-P35: 경계 B+C | T07-P35: DISCONNECT[disconnect_ms=30; commit_state=UNKNOWN; request=035-07] / sample=89 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + COMMIT_TIMELINE |
| T07-P35-K08 | T07-P35: 경계 B | T07-P35: 경계 C+D | T07-P35: RECURRENCE[occurrence=5; interval_s=88; mitigation_applied=1] / sample=96 | T07-P35: 먼저 깨지는 경계를 판정 | T07-P35: T07-P35/CH08 + RECURRENCE_TIMELINE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P35/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · API key와 service identity — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P35와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-B04 | T07-P35: object-level authorization과 BOLA/IDOR | T07-P35: user A가 user B의 id를 URL에 넣는다 | T07-P35: 조회 자체를 ownership scope로 제한한다 | T07-P35: T07-P35/CH08 증거와 형제 LESSON 증거를 분리 | T07-P35: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P35-B06 | T07-P35: CORS와 CSRF를 백엔드 경계에서 구분하기 | T07-P35: 다른 origin의 browser가 credential 요청을 보낸다 | T07-P35: CORS와 CSRF 질문을 분리한다 | T07-P35: T07-P35/CH08 증거와 형제 LESSON 증거를 분리 | T07-P35: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P35-B03 | T07-P35: authorization을 actor·action·resource로 생각하기 | T07-P35: editor가 다른 팀 document를 삭제하려 한다 | T07-P35: actor·action·resource로 판단한다 | T07-P35: T07-P35/CH08 증거와 형제 LESSON 증거를 분리 | T07-P35: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P35-B07 | T07-P35: secret와 configuration을 코드에서 분리하기 | T07-P35: API key가 config 파일과 로그에 보인다 | T07-P35: secret lifecycle과 redaction을 본다 | T07-P35: T07-P35/CH08 증거와 형제 LESSON 증거를 분리 | T07-P35: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P35-B02 | T07-P35: session·cookie·token의 역할 비교 | T07-P35: cookie session과 bearer token을 비교한다 | T07-P35: 보관 위치·revocation·수명을 비교한다 | T07-P35: T07-P35/CH08 증거와 형제 LESSON 증거를 분리 | T07-P35: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · API key와 service identity — 선택형 실패 주입 6개

T07-P35: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P35 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-X01 | T07-P35: 구버전 client | T07-P35: 한 단계 이전 schema가 요청됨; sample=105 | T07-P35: 호환 입력과 breaking change | T07-P35: T07-P35/CH02 판단과 별도 기록 | T07-P35: schema version·실제 client 분포·contract test |
| T07-P35-X02 | T07-P35: unknown outcome | T07-P35: dependency timeout 후 성공 여부 불명; sample=122 | T07-P35: 실패와 미확정 결과 | T07-P35: T07-P35/CH02 판단과 별도 기록 | T07-P35: provider id·조회 결과·retry history |
| T07-P35-X03 | T07-P35: 과부하 | T07-P35: traffic 세 배, p99 급증; sample=139 | T07-P35: 기능 실패와 saturation | T07-P35: T07-P35/CH02 판단과 별도 기록 | T07-P35: queue age·pool wait·CPU/event-loop·quota |
| T07-P35-X04 | T07-P35: sampling | T07-P35: 일부 log가 sampling으로 빠짐; sample=156 | T07-P35: 기록 부재와 사건 부재 | T07-P35: T07-P35/CH02 판단과 별도 기록 | T07-P35: metric·trace·durable state 교차 근거 |
| T07-P35-X05 | T07-P35: client disconnect | T07-P35: 응답 전에 연결이 끊김; sample=173 | T07-P35: 연결 종료와 server effect | T07-P35: T07-P35/CH02 판단과 별도 기록 | T07-P35: commit 시각·worker/outbox·request lifecycle |
| T07-P35-X06 | T07-P35: 재발 | T07-P35: 같은 오류가 잠시 뒤 다시 발생; sample=190 | T07-P35: 완화와 원인 제거 | T07-P35: T07-P35/CH02 판단과 별도 기록 | T07-P35: 재발 timeline·변경점·resource state |

## CHAPTER 17 · API key와 service identity — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P35에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-E01 | T07-P35: 대표 실패를 원인으로 착각 | T07-P35: T07-P35/CH06 실패 case를 다른 입력으로 재현 | T07-P35: 현상과 원인을 같은 것으로 봄 | T07-P35: T07-P35/CH06 대표 실패와 T07-P35/CH08 증거를 다시 대조 | T07-P35: T07-P35/CH08 |
| T07-P35-E02 | T07-P35: 경계 A 생략 | T07-P35: T07-P35/CH04의 조건 하나를 반대로 설정 | T07-P35: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P35: T07-P35/CH04를 새 입력에 적용 | T07-P35: T07-P35/CH08 |
| T07-P35-E03 | T07-P35: 경계 B 생략 | T07-P35: T07-P35/CH05의 조건 하나를 반대로 설정 | T07-P35: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P35: T07-P35/CH05를 새 입력에 적용 | T07-P35: T07-P35/CH08 |
| T07-P35-E04 | T07-P35: 복구 상태 혼동 | T07-P35: T07-P35/CH07에서 처리 중단을 주입 | T07-P35: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P35: T07-P35/CH07에서 수명 경계를 다시 표시 | T07-P35: T07-P35/CH08 |
| T07-P35-E05 | T07-P35: 운영 한계 누락 | T07-P35: T07-P35/CH09에서 부하 또는 drain 조건을 변경 | T07-P35: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P35: T07-P35/CH09의 종료 조건을 다시 작성 | T07-P35: T07-P35/CH08 |
| T07-P35-E06 | T07-P35: 증거 없는 성공 판정 | T07-P35: T07-P35/CH08에서 증거 하나를 숨김 | T07-P35: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P35: T07-P35/CH08에서 독립 증거 둘을 선택 | T07-P35: T07-P35/CH08 |

## CHAPTER 18 · API key와 service identity — synthetic 관측값 판독 6개

T07-P35: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P35의 숫자 하나만으로 원인을 단정하지 않고 T07-P35/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-O01 | T07-P35/auth_failures | 51 | T07-P35: 인증 실패 수 | T07-P35: 축=client disconnect; 원인 확정 금지 | T07-P35: COMMIT_TIMELINE + T07-P35/CH08 |
| T07-P35-O02 | T07-P35/policy_denials | 68 | T07-P35: 권한 거부 수 | T07-P35: 축=재발; 원인 확정 금지 | T07-P35: RECURRENCE_TIMELINE + T07-P35/CH08 |
| T07-P35-O03 | T07-P35/validation_rejects | 85 | T07-P35: 입력 거부 수 | T07-P35: 축=대형 입력; 원인 확정 금지 | T07-P35: SIZE_LIMIT + T07-P35/CH08 |
| T07-P35-O04 | T07-P35/rate_limited | 102 | T07-P35: rate-limit 적용 수 | T07-P35: 축=소유권 위조; 원인 확정 금지 | T07-P35: AUTHZ_POLICY + T07-P35/CH08 |
| T07-P35-O05 | T07-P35/redactions | 119 | T07-P35: 민감정보 마스킹 수 | T07-P35: 축=순서 역전; 원인 확정 금지 | T07-P35: SEQUENCE_STATE + T07-P35/CH08 |
| T07-P35-O06 | T07-P35/suspicious_requests | 136 | T07-P35: 의심 요청 수 | T07-P35: 축=복구 범위; 원인 확정 금지 | T07-P35: RECOVERY_AUDIT + T07-P35/CH08 |

## CHAPTER 19 · API key와 service identity — 선택형 코드 리뷰 6질문

T07-P35: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P35에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-R01 | T07-P35: 관측 | T07-P35: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P35: RECURRENCE[occurrence=2; interval_s=211; mitigation_applied=1] | T07-P35: T07-P35/CH04 | T07-P35: RECURRENCE_TIMELINE |
| T07-P35-R02 | T07-P35: 복구 | T07-P35: 재시작 뒤에도 필요한 상태가 남는가 | T07-P35: LARGE_INPUT[body_kb=2048; limit_kb=256; parsed=0] | T07-P35: T07-P35/CH05 | T07-P35: SIZE_LIMIT |
| T07-P35-R03 | T07-P35: 중복 | T07-P35: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P35: OWNER_SPOOF[actor=A0; payload_owner=B2; auth_owner=A0] | T07-P35: T07-P35/CH06 | T07-P35: AUTHZ_POLICY |
| T07-P35-R04 | T07-P35: timeout | T07-P35: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P35: REORDER[in_seq=6,4,5; applied_version=6] | T07-P35: T07-P35/CH07 | T07-P35: SEQUENCE_STATE |
| T07-P35-R05 | T07-P35: 관측 | T07-P35: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P35: RECOVERY_SCOPE[selected=34; expected=16; backup=1; dry_run=0] | T07-P35: T07-P35/CH04 | T07-P35: RECOVERY_AUDIT |
| T07-P35-R06 | T07-P35: 복구 | T07-P35: 재시작 뒤에도 필요한 상태가 남는가 | T07-P35: REPLAY[key=cmd-035-05; attempts=4; response_seen=0] | T07-P35: T07-P35/CH05 | T07-P35: IDEMPOTENCY_RECORD |

## CHAPTER 20 · API key와 service identity — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P35에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-I01 | T07-P35: 마지막 정상 | T07-P35: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P35: LARGE_INPUT[body_kb=1960; limit_kb=256; parsed=0] | T07-P35: T07-P35/CH08 + SIZE_LIMIT | T07-P35-incident-665 |
| T07-P35-I02 | T07-P35: 가설 검증 | T07-P35: 원인 후보 하나만 뒤집어 재현 | T07-P35: OWNER_SPOOF[actor=A0; payload_owner=B1; auth_owner=A0] | T07-P35: T07-P35/CH08 + AUTHZ_POLICY | T07-P35-incident-666 |
| T07-P35-I03 | T07-P35: 복구 확인 | T07-P35: durable state와 사용자 결과를 모두 확인 | T07-P35: REORDER[in_seq=5,3,4; applied_version=6] | T07-P35: T07-P35/CH08 + SEQUENCE_STATE | T07-P35-incident-667 |
| T07-P35-I04 | T07-P35: 재주입 | T07-P35: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P35: RECOVERY_SCOPE[selected=234; expected=22; backup=1; dry_run=1] | T07-P35: T07-P35/CH08 + RECOVERY_AUDIT | T07-P35-incident-668 |
| T07-P35-I05 | T07-P35: 회귀 고정 | T07-P35: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P35: REPLAY[key=cmd-035-04; attempts=3; response_seen=0] | T07-P35: T07-P35/CH08 + IDEMPOTENCY_RECORD | T07-P35-incident-669 |
| T07-P35-I06 | T07-P35: 영향 범위 | T07-P35: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P35: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P35: T07-P35/CH08 + CLIENT_VERSION | T07-P35-incident-670 |

## CHAPTER 21 · API key와 service identity — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P35/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P35/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P35/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P35/CH04~T07-P35/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P35/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P35/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P35/CH18 signal 두 개와 T07-P35/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P35/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P35/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · API key와 service identity — 통합 casebook 16문제

T07-P35 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P35 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P35-C01 | T07-P35: 경계 A | T07-P35: OWNER_SPOOF[actor=A0; payload_owner=B0; auth_owner=A0] | T07-P35: load=215, window=65s | T07-P35: review=권한 | T07-P35: 경계 A 위반 여부를 판정 | T07-P35: AUTHZ_POLICY + T07-P35/CH08 |
| T07-P35-C02 | T07-P35: 경계 B | T07-P35: REORDER[in_seq=4,2,3; applied_version=6] | T07-P35: load=238, window=84s | T07-P35: review=입력 경계 | T07-P35: 경계 B 위반 여부를 판정 | T07-P35: SEQUENCE_STATE + T07-P35/CH08 |
| T07-P35-C03 | T07-P35: 경계 C | T07-P35: RECOVERY_SCOPE[selected=223; expected=9; backup=1; dry_run=0] | T07-P35: load=261, window=13s | T07-P35: review=timeout | T07-P35: 경계 C 위반 여부를 판정 | T07-P35: RECOVERY_AUDIT + T07-P35/CH08 |
| T07-P35-C04 | T07-P35: 경계 D | T07-P35: REPLAY[key=cmd-035-03; attempts=2; response_seen=0] | T07-P35: load=284, window=32s | T07-P35: review=자원 | T07-P35: 경계 D 위반 여부를 판정 | T07-P35: IDEMPOTENCY_RECORD + T07-P35/CH08 |
| T07-P35-C05 | T07-P35: 경계 A | T07-P35: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P35: load=307, window=51s | T07-P35: review=순서 | T07-P35: 경계 A 위반 여부를 판정 | T07-P35: CLIENT_VERSION + T07-P35/CH08 |
| T07-P35-C06 | T07-P35: 경계 B | T07-P35: UNKNOWN_OUTCOME[timeout_ms=125; provider_state=UNKNOWN; lookup_id=p03505] | T07-P35: load=330, window=70s | T07-P35: review=관측 | T07-P35: 경계 B 위반 여부를 판정 | T07-P35: PROVIDER_RESULT + T07-P35/CH08 |
| T07-P35-C07 | T07-P35: 경계 C | T07-P35: OVERLOAD[rps=308; p99_ms=1146; queue=30] | T07-P35: load=353, window=89s | T07-P35: review=상태 변경 | T07-P35: 경계 C 위반 여부를 판정 | T07-P35: QUEUE_PRESSURE + T07-P35/CH08 |
| T07-P35-C08 | T07-P35: 경계 D | T07-P35: SAMPLING[sample_rate=20%; trace_present=1; metric_present=1] | T07-P35: load=376, window=18s | T07-P35: review=retry | T07-P35: 경계 D 위반 여부를 판정 | T07-P35: TRACE_METRIC_CROSSCHECK + T07-P35/CH08 |
| T07-P35-C09 | T07-P35: 경계 A | T07-P35: DISCONNECT[disconnect_ms=43; commit_state=UNKNOWN; request=035-08] | T07-P35: load=399, window=37s | T07-P35: review=복구 | T07-P35: 경계 A 위반 여부를 판정 | T07-P35: COMMIT_TIMELINE + T07-P35/CH08 |
| T07-P35-C10 | T07-P35: 경계 B | T07-P35: RECURRENCE[occurrence=6; interval_s=99; mitigation_applied=1] | T07-P35: load=422, window=56s | T07-P35: review=동시성 | T07-P35: 경계 B 위반 여부를 판정 | T07-P35: RECURRENCE_TIMELINE + T07-P35/CH08 |
| T07-P35-C11 | T07-P35: 경계 C | T07-P35: LARGE_INPUT[body_kb=1152; limit_kb=256; parsed=0] | T07-P35: load=445, window=75s | T07-P35: review=민감정보 | T07-P35: 경계 C 위반 여부를 판정 | T07-P35: SIZE_LIMIT + T07-P35/CH08 |
| T07-P35-C12 | T07-P35: 경계 D | T07-P35: OWNER_SPOOF[actor=A0; payload_owner=B1; auth_owner=A0] | T07-P35: load=468, window=94s | T07-P35: review=중복 | T07-P35: 경계 D 위반 여부를 판정 | T07-P35: AUTHZ_POLICY + T07-P35/CH08 |
| T07-P35-C13 | T07-P35: 경계 A | T07-P35: REORDER[in_seq=15,13,14; applied_version=6] | T07-P35: load=491, window=23s | T07-P35: review=권한 | T07-P35: 경계 A 위반 여부를 판정 | T07-P35: SEQUENCE_STATE + T07-P35/CH08 |
| T07-P35-C14 | T07-P35: 경계 B | T07-P35: RECOVERY_SCOPE[selected=133; expected=17; backup=1; dry_run=1] | T07-P35: load=514, window=42s | T07-P35: review=입력 경계 | T07-P35: 경계 B 위반 여부를 판정 | T07-P35: RECOVERY_AUDIT + T07-P35/CH08 |
| T07-P35-C15 | T07-P35: 경계 C | T07-P35: REPLAY[key=cmd-035-14; attempts=4; response_seen=0] | T07-P35: load=537, window=61s | T07-P35: review=timeout | T07-P35: 경계 C 위반 여부를 판정 | T07-P35: IDEMPOTENCY_RECORD + T07-P35/CH08 |
| T07-P35-C16 | T07-P35: 경계 D | T07-P35: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P35: load=560, window=80s | T07-P35: review=자원 | T07-P35: 경계 D 위반 여부를 판정 | T07-P35: CLIENT_VERSION + T07-P35/CH08 |

채점은 결론보다 근거를 본다. T07-P35/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · API key와 service identity — evidence 판독 문제 14개

T07-P35 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P35 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P35-V01 | T07-P35: latency=1455ms; queue=5; retry=0 | T07-P35: 순서 역전 | T07-P35: 축=순서 역전; 원인 확정은 보류 | T07-P35: SEQUENCE_STATE + T07-P35/CH08 | T07-P35: 피할 오판=오류 합치기 |
| T07-P35-V02 | T07-P35: latency=1522ms; queue=16; retry=3 | T07-P35: 복구 범위 | T07-P35: 축=복구 범위; 원인 확정은 보류 | T07-P35: RECOVERY_AUDIT + T07-P35/CH08 | T07-P35: 피할 오판=AI 과신 |
| T07-P35-V03 | T07-P35: latency=1589ms; queue=27; retry=6 | T07-P35: 재전송 | T07-P35: 축=재전송; 원인 확정은 보류 | T07-P35: IDEMPOTENCY_RECORD + T07-P35/CH08 | T07-P35: 피할 오판=복구 과잉 |
| T07-P35-V04 | T07-P35: latency=1656ms; queue=38; retry=2 | T07-P35: 구버전 client | T07-P35: 축=구버전 client; 원인 확정은 보류 | T07-P35: CLIENT_VERSION + T07-P35/CH08 | T07-P35: 피할 오판=잘못된 전제 |
| T07-P35-V05 | T07-P35: latency=1723ms; queue=49; retry=5 | T07-P35: unknown outcome | T07-P35: 축=unknown outcome; 원인 확정은 보류 | T07-P35: PROVIDER_RESULT + T07-P35/CH08 | T07-P35: 피할 오판=경계 누락 |
| T07-P35-V06 | T07-P35: latency=1790ms; queue=60; retry=1 | T07-P35: 과부하 | T07-P35: 축=과부하; 원인 확정은 보류 | T07-P35: QUEUE_PRESSURE + T07-P35/CH08 | T07-P35: 피할 오판=증거 혼동 |
| T07-P35-V07 | T07-P35: latency=57ms; queue=71; retry=4 | T07-P35: sampling | T07-P35: 축=sampling; 원인 확정은 보류 | T07-P35: TRACE_METRIC_CROSSCHECK + T07-P35/CH08 | T07-P35: 피할 오판=소유권 혼동 |
| T07-P35-V08 | T07-P35: latency=124ms; queue=2; retry=0 | T07-P35: client disconnect | T07-P35: 축=client disconnect; 원인 확정은 보류 | T07-P35: COMMIT_TIMELINE + T07-P35/CH08 | T07-P35: 피할 오판=운영 한계 누락 |
| T07-P35-V09 | T07-P35: latency=191ms; queue=13; retry=3 | T07-P35: 재발 | T07-P35: 축=재발; 원인 확정은 보류 | T07-P35: RECURRENCE_TIMELINE + T07-P35/CH08 | T07-P35: 피할 오판=오류 합치기 |
| T07-P35-V10 | T07-P35: latency=258ms; queue=24; retry=6 | T07-P35: 대형 입력 | T07-P35: 축=대형 입력; 원인 확정은 보류 | T07-P35: SIZE_LIMIT + T07-P35/CH08 | T07-P35: 피할 오판=AI 과신 |
| T07-P35-V11 | T07-P35: latency=325ms; queue=35; retry=2 | T07-P35: 소유권 위조 | T07-P35: 축=소유권 위조; 원인 확정은 보류 | T07-P35: AUTHZ_POLICY + T07-P35/CH08 | T07-P35: 피할 오판=복구 과잉 |
| T07-P35-V12 | T07-P35: latency=392ms; queue=46; retry=5 | T07-P35: 순서 역전 | T07-P35: 축=순서 역전; 원인 확정은 보류 | T07-P35: SEQUENCE_STATE + T07-P35/CH08 | T07-P35: 피할 오판=잘못된 전제 |
| T07-P35-V13 | T07-P35: latency=459ms; queue=57; retry=1 | T07-P35: 복구 범위 | T07-P35: 축=복구 범위; 원인 확정은 보류 | T07-P35: RECOVERY_AUDIT + T07-P35/CH08 | T07-P35: 피할 오판=경계 누락 |
| T07-P35-V14 | T07-P35: latency=526ms; queue=68; retry=4 | T07-P35: 재전송 | T07-P35: 축=재전송; 원인 확정은 보류 | T07-P35: IDEMPOTENCY_RECORD + T07-P35/CH08 | T07-P35: 피할 오판=증거 혼동 |

## CHAPTER 24 · API key와 service identity — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P35에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P35-D01 | T07-P35: 강제 종료 | T07-P35: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P35: in-flight request와 background work의 결과를 잃는지 | T07-P35: REPLAY[key=cmd-035-00; attempts=2; response_seen=0] | T07-P35: IDEMPOTENCY_RECORD + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P35-D02 | T07-P35: AI package 추가 | T07-P35: AI가 제안한 새 dependency를 도입한다 | T07-P35: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P35: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P35: CLIENT_VERSION + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P35-D03 | T07-P35: 비동기화 | T07-P35: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P35: durability·status API·worker retry 계약이 생기는지 | T07-P35: UNKNOWN_OUTCOME[timeout_ms=303; provider_state=UNKNOWN; lookup_id=p03502] | T07-P35: PROVIDER_RESULT + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P35-D04 | T07-P35: 권한 shortcut | T07-P35: payload의 owner/tenant id를 바로 사용한다 | T07-P35: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P35: OVERLOAD[rps=842; p99_ms=795; queue=28] | T07-P35: QUEUE_PRESSURE + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P35-D05 | T07-P35: 순서 병렬화 | T07-P35: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P35: 선후관계 invariant와 race를 깨지 않는지 | T07-P35: SAMPLING[sample_rate=78%; trace_present=0; metric_present=1] | T07-P35: TRACE_METRIC_CROSSCHECK + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P35-D06 | T07-P35: validation 이동 | T07-P35: validation을 business side effect 뒤로 옮긴다 | T07-P35: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P35: DISCONNECT[disconnect_ms=101; commit_state=UNKNOWN; request=035-05] | T07-P35: COMMIT_TIMELINE + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P35-D07 | T07-P35: batch 확대 | T07-P35: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P35: memory·deadline·부분 실패 범위가 커지는지 | T07-P35: RECURRENCE[occurrence=3; interval_s=66; mitigation_applied=1] | T07-P35: RECURRENCE_TIMELINE + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P35-D08 | T07-P35: fallback 추가 | T07-P35: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P35: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P35: LARGE_INPUT[body_kb=888; limit_kb=256; parsed=0] | T07-P35: SIZE_LIMIT + T07-P35/CH08 | T07-P35: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P35: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · API key와 service identity — 최종 contract와 evidence spine

**최종 계약:** 사람 사용자 외에도 서버·배치·외부 파트너 같은 program 주체를 식별할 credential이 필요하다.

**정상 메커니즘:** key 자체는 secret로 저장하고 서버에서는 hash/identifier로 lookup하며 scope와 rotation 상태를 함께 검사한다.

**대표 실패:** 하나의 master key를 모든 서비스가 공유하거나 source code/log에 key 원문이 남는다.

**검증 evidence:** key id, scope, last_used, rotation/revocation 상태를 보되 secret 값은 관측 데이터에 넣지 않는다.

**직접 행동:** key metadata와 필요한 scope를 비교해 사용 가능 여부를 판정한다.

**다음 연결:** `CORS와 CSRF를 백엔드 경계에서 구분하기`.

`API key와 service identity`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| OWASP-API | OWASP-API | API key와 service identity의 개념·실패·운영 판단 교차 확인 |
| OWASP-ASVS | OWASP-ASVS | API key와 service identity의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | API key와 service identity의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | API key와 service identity의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | API key와 service identity의 개념·실패·운영 판단 교차 확인 |

`API key와 service identity` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
