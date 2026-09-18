# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 02 · 업무 규칙과 상태 변화가 깨지지 않게 만들기

### LESSON 12 · 외부 API를 adapter 뒤에 격리하기

## CHAPTER 01 · 외부 API를 adapter 뒤에 격리하기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 외부 결제·메일·지도 API의 세부 형식을 domain code에 퍼뜨리지 않고 adapter 경계에 모은다.

**아주 쉬운 사건.** 외부 결제사가 새 error code를 보낸다. 이 사건에서는 먼저 **adapter가 내부 오류로 번역한다**.

**왜 필요한가.** 정상 동작은 내부 요청 모델을 외부 payload로 변환하고 외부 오류를 내부 오류 종류로 번역한다. 반대로 vendor 필드명이 서비스 전체에 퍼져 교체가 어렵고 provider 오류 문자열에 업무 로직이 의존한다.

**암기:** `외부 API를 adapter 뒤에 격리하기`의 역할 한 줄.

**직접 이해:** `외부 API를 adapter 뒤에 격리하기`의 입력·상태·결과 경계.

**AI 위임 가능:** `외부 API를 adapter 뒤에 격리하기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `외부 결제사가 새 error code를 보낸다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 adapter 입력/출력, provider status, retry 가능 여부와 mapping version을 본다.

## CHAPTER 02 · 외부 API를 adapter 뒤에 격리하기 — 아주 쉬운 예를 한 단계씩 해석

T07-P27: `외부 결제사가 새 error code를 보낸다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 외부 결제사가 새 error code를 보낸다 | T07-P27 외부 입력 | T07-P27: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | adapter가 내부 오류로 번역한다 | T07-P27 판단 기준 | T07-P27/CH08 관측표와 대조 |
| 정상 경로 | T07-P27/CH03 M1→M5 | 외부 API를 adapter 뒤에 격리하기: 완료 시점을 단계별로 분리 | T07-P27: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P27/CH06 vendor 필드명이 서비스 전체에 퍼져 교체가 어렵고 provider 오류 문자열에 업무 로직이 의존한다. | T07-P27: 깨진 계약 하나를 특정 | 외부 API를 adapter 뒤에 격리하기: 증상과 원인을 분리 |
| 재검증 | T07-P27/CH10 직접 실행 | T07-P27: 예상값 T07-P027 5 기록 | 외부 API를 adapter 뒤에 격리하기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P27/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P27/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · 외부 API를 adapter 뒤에 격리하기 — 내부 메커니즘과 상태 전이

내부 요청 모델을 외부 payload로 변환하고 외부 오류를 내부 오류 종류로 번역한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 외부 결제사가 새 error code를 보낸다 | source/actor/size를 보존 |
| M2 | 경계 판단 | adapter가 내부 오류로 번역한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | 내부 요청 모델을 외부 payload로 변환하고 외부 오류를 내부 오류 종류로 번역한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | adapter 입력/출력, provider status, retry 가능 여부와 mapping version을 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | provider별 응답을 공통 Result 모델로 변환한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`외부 API를 adapter 뒤에 격리하기` 흐름을 framework 이름 없이 설명한다.

막히면 `외부 결제사가 새 error code를 보낸다`의 M1→M5를 다시 추적한다.

### 전이 체크포인트
| case | 위치 | 확인할 차이 |
| --- | --- | --- |
| T07-P27-M1-424 | T07-P27/CH02 입력 | T07-P27 읽기 전용 단계 |
| T07-P27-M2-425 | T07-P27/CH03 판단 | T07-P27 상태 변경 직전 경계 |
| T07-P27-M3-426 | T07-P27/CH08 검증 | T07-P27 변경 뒤 실제 상태 대조 |
| T07-P27-M4-427 | T07-P27/CH06 반례 | T07-P27 성공 문자열과 상태 성공을 분리 |

## CHAPTER 04 · 외부 API를 adapter 뒤에 격리하기 — 실전 경계 A

**경계 A.** adapter는 내부 domain model을 vendor payload로 번역하고 vendor response를 내부 결과로 되돌려 provider field명과 error code가 core service에 퍼지는 것을 막는다.

T07-P27/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P27에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P27/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P27/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P27): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P27/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P27-A1-455 | T07-P27 조건 | adapter는 내부 domain model을 vendor payload로 번역하고 vendor response를 내부 결과로 되돌려 provider field명과 error code가 core service에 퍼지는 것을 막는다. |
| T07-P27-A2-456 | T07-P27 변화점 | T07-P27/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P27-A3-457 | T07-P27 반례 | T07-P27/CH06 대표 실패와 A 위반을 구별 |
| T07-P27-A4-458 | T07-P27 근거 | T07-P27/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P27-A5-459 | T07-P27 재실험 | 외부 결제사가 새 error code를 보낸다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · 외부 API를 adapter 뒤에 격리하기 — 실전 경계 B

**경계 B.** provider의 timeout·rate-limit·temporary error를 내부 RetryableDependencyError 같은 제한된 분류로 바꾸면 상위 use case가 vendor 문자열 parsing에 의존하지 않는다.

T07-P27/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P27에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P27/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P27/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P27): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P27/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P27-B1-486 | T07-P27 조건 | provider의 timeout·rate-limit·temporary error를 내부 RetryableDependencyError 같은 제한된 분류로 바꾸면 상위 use case가 vendor 문자열 parsing에 의존하지 않는다. |
| T07-P27-B2-487 | T07-P27 독립성 | T07-P27/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P27-B3-488 | T07-P27 상태 | T07-P27/CH03 before·after 위치를 다시 지정 |
| T07-P27-B4-489 | T07-P27 반증 | T07-P27/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P27-B5-490 | T07-P27 적용 | 외부 API를 adapter 뒤에 격리하기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · 외부 API를 adapter 뒤에 격리하기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **vendor 필드명이 서비스 전체에 퍼져 교체가 어렵고 provider 오류 문자열에 업무 로직이 의존한다.**

아래 여섯 사례는 T07-P27의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P27-F01 | T07-P27: 대표 실패 | T07-P27: T07-P27/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P27: 현상만 보고 원인을 확정 | T07-P27/CH08 evidence map에서 상태를 대조 |
| T07-P27-F02 | T07-P27: 경계 A 누락 | T07-P27: T07-P27/CH04 경계 A 위반 입력 | T07-P27: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P27/CH08 evidence map에서 상태를 대조 |
| T07-P27-F03 | T07-P27: 경계 B 누락 | T07-P27: T07-P27/CH05 경계 B 위반 입력 | T07-P27: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P27/CH08 evidence map에서 상태를 대조 |
| T07-P27-F04 | T07-P27: 복구 경계 C 누락 | T07-P27: T07-P27/CH07 경계 C 복구 조건 | T07-P27: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P27/CH08 evidence map에서 상태를 대조 |
| T07-P27-F05 | T07-P27: 운영 경계 D 누락 | T07-P27: T07-P27/CH09 경계 D 운영 조건 | T07-P27: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P27/CH08 evidence map에서 상태를 대조 |
| T07-P27-F06 | T07-P27: 증거 없는 결론 | T07-P27: T07-P27/CH02 첫 판단만 존재 | T07-P27: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P27/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P27/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · 외부 API를 adapter 뒤에 격리하기 — 복구 가능한 상태와 수명

**경계 C.** retry·timeout·circuit 정책은 integration 특성을 가장 잘 아는 adapter 근처에서 정하되 상위 전체 deadline과 idempotency contract를 넘지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P27에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P27/CH08 evidence map을 본다. 복구 후에는 T07-P27/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P27에서 이미 확정된 side effect는 T07-P27/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P27-R1-548 | T07-P27 중단 직전 | T07-P27/CH03에서 이미 확정된 상태만 표시 |
| T07-P27-R2-549 | T07-P27 재시작 직후 | retry·timeout·circuit 정책은 integration 특성을 가장 잘 아는 adapter 근처에서 정하되 상위 전체 deadline과 idempotency contract를 넘지 않는다. |
| T07-P27-R3-550 | T07-P27 재검증 | T07-P27/CH08 근거로 중복·누락 여부 확인 |
| T07-P27-R4-551 | T07-P27 재실행 | T07-P27/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · 외부 API를 adapter 뒤에 격리하기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 외부 결제사가 새 error code를 보낸다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | adapter가 내부 오류로 번역한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | 내부 요청 모델을 외부 payload로 변환하고 외부 오류를 내부 오류 종류로 번역한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | vendor 필드명이 서비스 전체에 퍼져 교체가 어렵고 provider 오류 문자열에 업무 로직이 의존한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | adapter 입력/출력, provider status, retry 가능 여부와 mapping version을 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`외부 API를 adapter 뒤에 격리하기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · 외부 API를 adapter 뒤에 격리하기 — 운영 한계와 종료 조건

**경계 D.** provider sandbox만 믿지 말고 대표 실제 response shape·누락 field·새 enum 값을 fixture로 보존해 adapter contract test를 돌리면 외부 변화에 더 빨리 대응할 수 있다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P27/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P27/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P27 과제: 경계 D와 T07-P27/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P27-O1-610 | synthetic-load=110 | T07-P27/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P27-O2-611 | synthetic-budget=760ms | T07-P27 timeout과 unknown outcome을 분리 |
| T07-P27-O3-612 | T07-P27 종료 | provider sandbox만 믿지 말고 대표 실제 response shape·누락 field·새 enum 값을 fixture로 보존해 adapter contract test를 돌리면 외부 변화에 더 빨리 대응할 수 있다. |
| T07-P27-O4-613 | T07-P27 완화 | T07-P27/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · 외부 API를 adapter 뒤에 격리하기 — 직접 실행하는 작은 모델

`외부 API를 adapter 뒤에 격리하기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P027 5`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P027";
const current = 4;
const incoming = [3, 4, 5];
const next = incoming.reduce((state, v) => v > state ? v : state, current);
console.log(marker, next);
```

기준 출력: `T07-P027 5`.

`외부 API를 adapter 뒤에 격리하기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P27-L1-642 | constmarker="T07-P027"; | T07-P27 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P27-L2-643 | constcurrent=4; | T07-P27 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P27-L3-644 | constincoming=[3,4,5]; | T07-P27 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P27-L4-645 | constnext=incoming.reduce((state,v)=>v>state?v:state,current); | T07-P27 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P27-L5-646 | console.log(marker,next); | T07-P27 출력 관측점; 예상 `T07-P027 5`와 비교 |
| T07-P27-LX-731 | T07-P27 실행 기록 | T07-P27 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · 외부 API를 adapter 뒤에 격리하기 — 한 부분만 수정하고 다시 예측

수정 과제: **incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다**.

수정 전은 `T07-P027 5`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P27/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P27-D1-672 | 기준 `T07-P027 5` | T07-P27 수정 전 실행을 먼저 재현 |
| T07-P27-D2-673 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | T07-P27 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P27-D3-674 | T07-P27 새 예측 | T07-P27 실행 전에 출력·상태를 먼저 기록 |
| T07-P27-D4-675 | T07-P27 재실행 | T07-P27/CH10 실제값과 새 예측을 대조 |
| T07-P27-D5-676 | T07-P27 반례 | T07-P27/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P27-D6-677 | T07-P27 근거 | T07-P27/CH08 상태가 설명과 일치해야 완료 |
| T07-P27-D7-678 | T07-P27 이유 | T07-P27 변경 이유를 외부 API를 adapter 뒤에 격리하기 계약과 연결해 설명 |

## CHAPTER 12 · 외부 API를 adapter 뒤에 격리하기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: 외부 API를 adapter 뒤에 격리하기 | 외부 결제·메일·지도 API의 세부 형식을 domain code에 퍼뜨리지 않고 adapter 경계에 모은다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P27/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | 내부 요청 모델을 외부 payload로 변환하고 외부 오류를 내부 오류 종류로 번역한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | vendor 필드명이 서비스 전체에 퍼져 교체가 어렵고 provider 오류 문자열에 업무 로직이 의존한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | adapter 입력/출력, provider status, retry 가능 여부와 mapping version을 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P27/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P27/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P27/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P27/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `외부 API를 adapter 뒤에 격리하기` 실행 코드 수정 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | `외부 API를 adapter 뒤에 격리하기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P27/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P27/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · 외부 API를 adapter 뒤에 격리하기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 외부 결제·메일·지도 API의 세부 형식을 domain code에 퍼뜨리지 않고 adapter 경계에 모은다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P27/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P27/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P27/CH10 실행용 boilerplate·test 후보 | T07-P27: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P027 5` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P27/CH02의 판단 기준과 T07-P27/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P27-AI1-734 | T07-P27 사람 결정 | T07-P27 업무 의미·허용 위험·완료 기준 소유 |
| T07-P27-AI2-735 | T07-P27 AI 초안 | T07-P27/CH10 boilerplate·test 후보까지만 위임 |
| T07-P27-AI3-736 | T07-P27 검증 | T07-P27/CH06 반례와 T07-P27/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · 외부 API를 adapter 뒤에 격리하기 — 경계 조합 실험 8개

T07-P27: T07-P27/CH04~T07-P27/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P27의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-K01 | T07-P27: 경계 A | T07-P27: 경계 B | T07-P27: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=48 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + CLIENT_VERSION |
| T07-P27-K02 | T07-P27: 경계 A | T07-P27: 경계 C | T07-P27: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=3] / sample=55 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + STATE_VERSION |
| T07-P27-K03 | T07-P27: 경계 A | T07-P27: 경계 D | T07-P27: UNKNOWN_OUTCOME[timeout_ms=293; provider_state=UNKNOWN; lookup_id=p02703] / sample=62 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + PROVIDER_RESULT |
| T07-P27-K04 | T07-P27: 경계 B | T07-P27: 경계 C | T07-P27: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] / sample=69 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + DURABLE_STATE |
| T07-P27-K05 | T07-P27: 경계 B | T07-P27: 경계 D | T07-P27: OVERLOAD[rps=845; p99_ms=678; queue=17] / sample=76 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + QUEUE_PRESSURE |
| T07-P27-K06 | T07-P27: 경계 C | T07-P27: 경계 D | T07-P27: SAMPLING[sample_rate=65%; trace_present=0; metric_present=1] / sample=83 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P27-K07 | T07-P27: 경계 A | T07-P27: 경계 B+C | T07-P27: DISCONNECT[disconnect_ms=88; commit_state=UNKNOWN; request=027-07] / sample=90 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + COMMIT_TIMELINE |
| T07-P27-K08 | T07-P27: 경계 B | T07-P27: 경계 C+D | T07-P27: RECURRENCE[occurrence=5; interval_s=67; mitigation_applied=1] / sample=97 | T07-P27: 먼저 깨지는 경계를 판정 | T07-P27: T07-P27/CH08 + RECURRENCE_TIMELINE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P27/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · 외부 API를 adapter 뒤에 격리하기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P27와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-B11 | T07-P27: side effect와 outbox 사고방식 | T07-P27: DB commit 직후 process가 죽는다 | T07-P27: outbox가 발행 의도를 남기는지 본다 | T07-P27: T07-P27/CH08 증거와 형제 LESSON 증거를 분리 | T07-P27: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P27-B13 | T07-P27: scheduled job과 cron을 안전한 업무로 만들기 | T07-P27: 10:00 job이 10:05까지 끝나지 않는다 | T07-P27: overlap과 occurrence id를 결정한다 | T07-P27: T07-P27/CH08 증거와 형제 LESSON 증거를 분리 | T07-P27: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P27-B10 | T07-P27: validation·business rule·authorization을 분리하기 | T07-P27: 형식은 맞지만 품절이고 다른 사용자 주문이다 | T07-P27: validation·rule·authorization을 나눈다 | T07-P27: T07-P27/CH08 증거와 형제 LESSON 증거를 분리 | T07-P27: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P27-B14 | T07-P27: webhook을 받는 쪽의 상태 변화 설계 | T07-P27: webhook 42가 두 번, 41이 늦게 온다 | T07-P27: dedupe와 version 순서를 함께 본다 | T07-P27: T07-P27/CH08 증거와 형제 LESSON 증거를 분리 | T07-P27: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P27-B09 | T07-P27: 동시 수정에서 lost update를 막기 | T07-P27: 두 요청이 version=4를 읽고 수정한다 | T07-P27: compare-and-swap으로 충돌을 감지한다 | T07-P27: T07-P27/CH08 증거와 형제 LESSON 증거를 분리 | T07-P27: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · 외부 API를 adapter 뒤에 격리하기 — 선택형 실패 주입 6개

T07-P27: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P27 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-X01 | T07-P27: unknown outcome | T07-P27: dependency timeout 후 성공 여부 불명; sample=854 | T07-P27: 실패와 미확정 결과 | T07-P27: T07-P27/CH02 판단과 별도 기록 | T07-P27: provider id·조회 결과·retry history |
| T07-P27-X02 | T07-P27: 재시작 | T07-P27: side effect 직후 process가 재시작됨; sample=871 | T07-P27: durable state와 memory state | T07-P27: T07-P27/CH02 판단과 별도 기록 | T07-P27: commit·outbox·job id·restart 전후 상태 |
| T07-P27-X03 | T07-P27: 과부하 | T07-P27: traffic 세 배, p99 급증; sample=888 | T07-P27: 기능 실패와 saturation | T07-P27: T07-P27/CH02 판단과 별도 기록 | T07-P27: queue age·pool wait·CPU/event-loop·quota |
| T07-P27-X04 | T07-P27: sampling | T07-P27: 일부 log가 sampling으로 빠짐; sample=905 | T07-P27: 기록 부재와 사건 부재 | T07-P27: T07-P27/CH02 판단과 별도 기록 | T07-P27: metric·trace·durable state 교차 근거 |
| T07-P27-X05 | T07-P27: client disconnect | T07-P27: 응답 전에 연결이 끊김; sample=922 | T07-P27: 연결 종료와 server effect | T07-P27: T07-P27/CH02 판단과 별도 기록 | T07-P27: commit 시각·worker/outbox·request lifecycle |
| T07-P27-X06 | T07-P27: 재발 | T07-P27: 같은 오류가 잠시 뒤 다시 발생; sample=939 | T07-P27: 완화와 원인 제거 | T07-P27: T07-P27/CH02 판단과 별도 기록 | T07-P27: 재발 timeline·변경점·resource state |

## CHAPTER 17 · 외부 API를 adapter 뒤에 격리하기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P27에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-E01 | T07-P27: 대표 실패를 원인으로 착각 | T07-P27: T07-P27/CH06 실패 case를 다른 입력으로 재현 | T07-P27: 현상과 원인을 같은 것으로 봄 | T07-P27: T07-P27/CH06 대표 실패와 T07-P27/CH08 증거를 다시 대조 | T07-P27: T07-P27/CH08 |
| T07-P27-E02 | T07-P27: 경계 A 생략 | T07-P27: T07-P27/CH04의 조건 하나를 반대로 설정 | T07-P27: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P27: T07-P27/CH04를 새 입력에 적용 | T07-P27: T07-P27/CH08 |
| T07-P27-E03 | T07-P27: 경계 B 생략 | T07-P27: T07-P27/CH05의 조건 하나를 반대로 설정 | T07-P27: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P27: T07-P27/CH05를 새 입력에 적용 | T07-P27: T07-P27/CH08 |
| T07-P27-E04 | T07-P27: 복구 상태 혼동 | T07-P27: T07-P27/CH07에서 처리 중단을 주입 | T07-P27: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P27: T07-P27/CH07에서 수명 경계를 다시 표시 | T07-P27: T07-P27/CH08 |
| T07-P27-E05 | T07-P27: 운영 한계 누락 | T07-P27: T07-P27/CH09에서 부하 또는 drain 조건을 변경 | T07-P27: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P27: T07-P27/CH09의 종료 조건을 다시 작성 | T07-P27: T07-P27/CH08 |
| T07-P27-E06 | T07-P27: 증거 없는 성공 판정 | T07-P27: T07-P27/CH08에서 증거 하나를 숨김 | T07-P27: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P27: T07-P27/CH08에서 독립 증거 둘을 선택 | T07-P27: T07-P27/CH08 |

## CHAPTER 18 · 외부 API를 adapter 뒤에 격리하기 — synthetic 관측값 판독 6개

T07-P27: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P27의 숫자 하나만으로 원인을 단정하지 않고 T07-P27/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-O01 | T07-P27/commands_received | 664 | T07-P27: command 수신 수 | T07-P27: 축=client disconnect; 원인 확정 금지 | T07-P27: COMMIT_TIMELINE + T07-P27/CH08 |
| T07-P27-O02 | T07-P27/state_version | 681 | T07-P27: 상태 버전 | T07-P27: 축=재발; 원인 확정 금지 | T07-P27: RECURRENCE_TIMELINE + T07-P27/CH08 |
| T07-P27-O03 | T07-P27/conflicts | 698 | T07-P27: 동시 수정 충돌 수 | T07-P27: 축=대형 입력; 원인 확정 금지 | T07-P27: SIZE_LIMIT + T07-P27/CH08 |
| T07-P27-O04 | T07-P27/retries | 715 | T07-P27: 재처리 수 | T07-P27: 축=순서 역전; 원인 확정 금지 | T07-P27: SEQUENCE_STATE + T07-P27/CH08 |
| T07-P27-O05 | T07-P27/side_effects | 732 | T07-P27: 외부 side effect 수 | T07-P27: 축=복구 범위; 원인 확정 금지 | T07-P27: RECOVERY_AUDIT + T07-P27/CH08 |
| T07-P27-O06 | T07-P27/invariant_violations | 749 | T07-P27: invariant 위반 수 | T07-P27: 축=재전송; 원인 확정 금지 | T07-P27: IDEMPOTENCY_RECORD + T07-P27/CH08 |

## CHAPTER 19 · 외부 API를 adapter 뒤에 격리하기 — 선택형 코드 리뷰 6질문

T07-P27: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P27에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-R01 | T07-P27: 입력 경계 | T07-P27: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P27: RECURRENCE[occurrence=2; interval_s=190; mitigation_applied=1] | T07-P27: T07-P27/CH04 | T07-P27: RECURRENCE_TIMELINE |
| T07-P27-R02 | T07-P27: 순서 | T07-P27: old/new event 순서가 바뀌어도 안전한가 | T07-P27: LARGE_INPUT[body_kb=1880; limit_kb=512; parsed=0] | T07-P27: T07-P27/CH05 | T07-P27: SIZE_LIMIT |
| T07-P27-R03 | T07-P27: retry | T07-P27: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P27: REORDER[in_seq=5,3,4; applied_version=4] | T07-P27: T07-P27/CH06 | T07-P27: SEQUENCE_STATE |
| T07-P27-R04 | T07-P27: 민감정보 | T07-P27: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P27: RECOVERY_SCOPE[selected=213; expected=21; backup=1; dry_run=1] | T07-P27: T07-P27/CH07 | T07-P27: RECOVERY_AUDIT |
| T07-P27-R05 | T07-P27: 입력 경계 | T07-P27: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P27: REPLAY[key=cmd-027-04; attempts=3; response_seen=0] | T07-P27: T07-P27/CH04 | T07-P27: IDEMPOTENCY_RECORD |
| T07-P27-R06 | T07-P27: 순서 | T07-P27: old/new event 순서가 바뀌어도 안전한가 | T07-P27: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P27: T07-P27/CH05 | T07-P27: CLIENT_VERSION |

## CHAPTER 20 · 외부 API를 adapter 뒤에 격리하기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P27에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-I01 | T07-P27: 마지막 정상 | T07-P27: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P27: LARGE_INPUT[body_kb=1792; limit_kb=512; parsed=0] | T07-P27: T07-P27/CH08 + SIZE_LIMIT | T07-P27-incident-513 |
| T07-P27-I02 | T07-P27: 가설 검증 | T07-P27: 원인 후보 하나만 뒤집어 재현 | T07-P27: REORDER[in_seq=4,2,3; applied_version=4] | T07-P27: T07-P27/CH08 + SEQUENCE_STATE | T07-P27-incident-514 |
| T07-P27-I03 | T07-P27: 복구 확인 | T07-P27: durable state와 사용자 결과를 모두 확인 | T07-P27: RECOVERY_SCOPE[selected=202; expected=8; backup=1; dry_run=0] | T07-P27: T07-P27/CH08 + RECOVERY_AUDIT | T07-P27-incident-515 |
| T07-P27-I04 | T07-P27: 재주입 | T07-P27: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P27: REPLAY[key=cmd-027-03; attempts=2; response_seen=0] | T07-P27: T07-P27/CH08 + IDEMPOTENCY_RECORD | T07-P27-incident-516 |
| T07-P27-I05 | T07-P27: 회귀 고정 | T07-P27: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P27: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P27: T07-P27/CH08 + CLIENT_VERSION | T07-P27-incident-517 |
| T07-P27-I06 | T07-P27: 영향 범위 | T07-P27: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P27: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=42] | T07-P27: T07-P27/CH08 + STATE_VERSION | T07-P27-incident-518 |

## CHAPTER 21 · 외부 API를 adapter 뒤에 격리하기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P27/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P27/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P27/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P27/CH04~T07-P27/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P27/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P27/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P27/CH18 signal 두 개와 T07-P27/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P27/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P27/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · 외부 API를 adapter 뒤에 격리하기 — 통합 casebook 16문제

T07-P27 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P27 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P27-C01 | T07-P27: 경계 A | T07-P27: REORDER[in_seq=3,1,2; applied_version=4] | T07-P27: load=883, window=19s | T07-P27: review=순서 | T07-P27: 경계 A 위반 여부를 판정 | T07-P27: SEQUENCE_STATE + T07-P27/CH08 |
| T07-P27-C02 | T07-P27: 경계 B | T07-P27: RECOVERY_SCOPE[selected=191; expected=16; backup=1; dry_run=1] | T07-P27: load=906, window=38s | T07-P27: review=관측 | T07-P27: 경계 B 위반 여부를 판정 | T07-P27: RECOVERY_AUDIT + T07-P27/CH08 |
| T07-P27-C03 | T07-P27: 경계 C | T07-P27: REPLAY[key=cmd-027-02; attempts=4; response_seen=0] | T07-P27: load=929, window=57s | T07-P27: review=상태 변경 | T07-P27: 경계 C 위반 여부를 판정 | T07-P27: IDEMPOTENCY_RECORD + T07-P27/CH08 |
| T07-P27-C04 | T07-P27: 경계 D | T07-P27: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P27: load=952, window=76s | T07-P27: review=retry | T07-P27: 경계 D 위반 여부를 판정 | T07-P27: CLIENT_VERSION + T07-P27/CH08 |
| T07-P27-C05 | T07-P27: 경계 A | T07-P27: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=29] | T07-P27: load=975, window=95s | T07-P27: review=복구 | T07-P27: 경계 A 위반 여부를 판정 | T07-P27: STATE_VERSION + T07-P27/CH08 |
| T07-P27-C06 | T07-P27: 경계 B | T07-P27: UNKNOWN_OUTCOME[timeout_ms=315; provider_state=UNKNOWN; lookup_id=p02705] | T07-P27: load=998, window=24s | T07-P27: review=동시성 | T07-P27: 경계 B 위반 여부를 판정 | T07-P27: PROVIDER_RESULT + T07-P27/CH08 |
| T07-P27-C07 | T07-P27: 경계 C | T07-P27: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P27: load=121, window=43s | T07-P27: review=민감정보 | T07-P27: 경계 C 위반 여부를 판정 | T07-P27: DURABLE_STATE + T07-P27/CH08 |
| T07-P27-C08 | T07-P27: 경계 D | T07-P27: OVERLOAD[rps=278; p99_ms=912; queue=25] | T07-P27: load=144, window=62s | T07-P27: review=중복 | T07-P27: 경계 D 위반 여부를 판정 | T07-P27: QUEUE_PRESSURE + T07-P27/CH08 |
| T07-P27-C09 | T07-P27: 경계 A | T07-P27: SAMPLING[sample_rate=11%; trace_present=0; metric_present=1] | T07-P27: load=167, window=81s | T07-P27: review=권한 | T07-P27: 경계 A 위반 여부를 판정 | T07-P27: TRACE_METRIC_CROSSCHECK + T07-P27/CH08 |
| T07-P27-C10 | T07-P27: 경계 B | T07-P27: DISCONNECT[disconnect_ms=114; commit_state=UNKNOWN; request=027-09] | T07-P27: load=190, window=10s | T07-P27: review=입력 경계 | T07-P27: 경계 B 위반 여부를 판정 | T07-P27: COMMIT_TIMELINE + T07-P27/CH08 |
| T07-P27-C11 | T07-P27: 경계 C | T07-P27: RECURRENCE[occurrence=2; interval_s=89; mitigation_applied=1] | T07-P27: load=213, window=29s | T07-P27: review=timeout | T07-P27: 경계 C 위반 여부를 판정 | T07-P27: RECURRENCE_TIMELINE + T07-P27/CH08 |
| T07-P27-C12 | T07-P27: 경계 D | T07-P27: LARGE_INPUT[body_kb=1072; limit_kb=512; parsed=0] | T07-P27: load=236, window=48s | T07-P27: review=자원 | T07-P27: 경계 D 위반 여부를 판정 | T07-P27: SIZE_LIMIT + T07-P27/CH08 |
| T07-P27-C13 | T07-P27: 경계 A | T07-P27: REORDER[in_seq=15,13,14; applied_version=4] | T07-P27: load=259, window=67s | T07-P27: review=순서 | T07-P27: 경계 A 위반 여부를 판정 | T07-P27: SEQUENCE_STATE + T07-P27/CH08 |
| T07-P27-C14 | T07-P27: 경계 B | T07-P27: RECOVERY_SCOPE[selected=112; expected=16; backup=1; dry_run=1] | T07-P27: load=282, window=86s | T07-P27: review=관측 | T07-P27: 경계 B 위반 여부를 판정 | T07-P27: RECOVERY_AUDIT + T07-P27/CH08 |
| T07-P27-C15 | T07-P27: 경계 C | T07-P27: REPLAY[key=cmd-027-14; attempts=4; response_seen=0] | T07-P27: load=305, window=15s | T07-P27: review=상태 변경 | T07-P27: 경계 C 위반 여부를 판정 | T07-P27: IDEMPOTENCY_RECORD + T07-P27/CH08 |
| T07-P27-C16 | T07-P27: 경계 D | T07-P27: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P27: load=328, window=34s | T07-P27: review=retry | T07-P27: 경계 D 위반 여부를 판정 | T07-P27: CLIENT_VERSION + T07-P27/CH08 |

채점은 결론보다 근거를 본다. T07-P27/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · 외부 API를 adapter 뒤에 격리하기 — evidence 판독 문제 14개

T07-P27 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P27 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P27-V01 | T07-P27: latency=1127ms; queue=29; retry=6 | T07-P27: 복구 범위 | T07-P27: 축=복구 범위; 원인 확정은 보류 | T07-P27: RECOVERY_AUDIT + T07-P27/CH08 | T07-P27: 피할 오판=복구 과잉 |
| T07-P27-V02 | T07-P27: latency=1194ms; queue=40; retry=2 | T07-P27: 재전송 | T07-P27: 축=재전송; 원인 확정은 보류 | T07-P27: IDEMPOTENCY_RECORD + T07-P27/CH08 | T07-P27: 피할 오판=잘못된 전제 |
| T07-P27-V03 | T07-P27: latency=1261ms; queue=51; retry=5 | T07-P27: 구버전 client | T07-P27: 축=구버전 client; 원인 확정은 보류 | T07-P27: CLIENT_VERSION + T07-P27/CH08 | T07-P27: 피할 오판=경계 누락 |
| T07-P27-V04 | T07-P27: latency=1328ms; queue=62; retry=1 | T07-P27: 동시 변경 | T07-P27: 축=동시 변경; 원인 확정은 보류 | T07-P27: STATE_VERSION + T07-P27/CH08 | T07-P27: 피할 오판=증거 혼동 |
| T07-P27-V05 | T07-P27: latency=1395ms; queue=73; retry=4 | T07-P27: unknown outcome | T07-P27: 축=unknown outcome; 원인 확정은 보류 | T07-P27: PROVIDER_RESULT + T07-P27/CH08 | T07-P27: 피할 오판=재시도 오판 |
| T07-P27-V06 | T07-P27: latency=1462ms; queue=4; retry=0 | T07-P27: 재시작 | T07-P27: 축=재시작; 원인 확정은 보류 | T07-P27: DURABLE_STATE + T07-P27/CH08 | T07-P27: 피할 오판=동시성 무시 |
| T07-P27-V07 | T07-P27: latency=1529ms; queue=15; retry=3 | T07-P27: 과부하 | T07-P27: 축=과부하; 원인 확정은 보류 | T07-P27: QUEUE_PRESSURE + T07-P27/CH08 | T07-P27: 피할 오판=순서 가정 |
| T07-P27-V08 | T07-P27: latency=1596ms; queue=26; retry=6 | T07-P27: sampling | T07-P27: 축=sampling; 원인 확정은 보류 | T07-P27: TRACE_METRIC_CROSSCHECK + T07-P27/CH08 | T07-P27: 피할 오판=상태 수명 혼동 |
| T07-P27-V09 | T07-P27: latency=1663ms; queue=37; retry=2 | T07-P27: client disconnect | T07-P27: 축=client disconnect; 원인 확정은 보류 | T07-P27: COMMIT_TIMELINE + T07-P27/CH08 | T07-P27: 피할 오판=운영 한계 누락 |
| T07-P27-V10 | T07-P27: latency=1730ms; queue=48; retry=5 | T07-P27: 재발 | T07-P27: 축=재발; 원인 확정은 보류 | T07-P27: RECURRENCE_TIMELINE + T07-P27/CH08 | T07-P27: 피할 오판=오류 합치기 |
| T07-P27-V11 | T07-P27: latency=1797ms; queue=59; retry=1 | T07-P27: 대형 입력 | T07-P27: 축=대형 입력; 원인 확정은 보류 | T07-P27: SIZE_LIMIT + T07-P27/CH08 | T07-P27: 피할 오판=복구 과잉 |
| T07-P27-V12 | T07-P27: latency=64ms; queue=70; retry=4 | T07-P27: 순서 역전 | T07-P27: 축=순서 역전; 원인 확정은 보류 | T07-P27: SEQUENCE_STATE + T07-P27/CH08 | T07-P27: 피할 오판=잘못된 전제 |
| T07-P27-V13 | T07-P27: latency=131ms; queue=1; retry=0 | T07-P27: 복구 범위 | T07-P27: 축=복구 범위; 원인 확정은 보류 | T07-P27: RECOVERY_AUDIT + T07-P27/CH08 | T07-P27: 피할 오판=경계 누락 |
| T07-P27-V14 | T07-P27: latency=198ms; queue=12; retry=3 | T07-P27: 재전송 | T07-P27: 축=재전송; 원인 확정은 보류 | T07-P27: IDEMPOTENCY_RECORD + T07-P27/CH08 | T07-P27: 피할 오판=증거 혼동 |

## CHAPTER 24 · 외부 API를 adapter 뒤에 격리하기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P27에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P27-D01 | T07-P27: schema 변경 | T07-P27: 필드 이름·형식·required 조건을 바꾼다 | T07-P27: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P27: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P27: CLIENT_VERSION + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P27-D02 | T07-P27: 결과 합치기 | T07-P27: 여러 오류를 하나의 status/error code로 합친다 | T07-P27: client 행동과 retry 가능성을 잃지 않는지 | T07-P27: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=87] | T07-P27: STATE_VERSION + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P27-D03 | T07-P27: retry 추가 | T07-P27: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P27: unknown outcome과 duplicate side effect를 구분하는지 | T07-P27: UNKNOWN_OUTCOME[timeout_ms=282; provider_state=UNKNOWN; lookup_id=p02702] | T07-P27: PROVIDER_RESULT + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P27-D04 | T07-P27: 로그 확대 | T07-P27: debug를 위해 payload와 context 기록을 늘린다 | T07-P27: secret·PII·cardinality 비용을 통제하는지 | T07-P27: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P27: DURABLE_STATE + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P27-D05 | T07-P27: 강제 종료 | T07-P27: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P27: in-flight request와 background work의 결과를 잃는지 | T07-P27: OVERLOAD[rps=812; p99_ms=561; queue=23] | T07-P27: QUEUE_PRESSURE + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P27-D06 | T07-P27: AI package 추가 | T07-P27: AI가 제안한 새 dependency를 도입한다 | T07-P27: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P27: SAMPLING[sample_rate=52%; trace_present=1; metric_present=1] | T07-P27: TRACE_METRIC_CROSSCHECK + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P27-D07 | T07-P27: 비동기화 | T07-P27: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P27: durability·status API·worker retry 계약이 생기는지 | T07-P27: DISCONNECT[disconnect_ms=75; commit_state=UNKNOWN; request=027-06] | T07-P27: COMMIT_TIMELINE + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P27-D08 | T07-P27: 권한 shortcut | T07-P27: payload의 owner/tenant id를 바로 사용한다 | T07-P27: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P27: RECURRENCE[occurrence=4; interval_s=56; mitigation_applied=1] | T07-P27: RECURRENCE_TIMELINE + T07-P27/CH08 | T07-P27: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P27: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · 외부 API를 adapter 뒤에 격리하기 — 최종 contract와 evidence spine

**최종 계약:** 외부 결제·메일·지도 API의 세부 형식을 domain code에 퍼뜨리지 않고 adapter 경계에 모은다.

**정상 메커니즘:** 내부 요청 모델을 외부 payload로 변환하고 외부 오류를 내부 오류 종류로 번역한다.

**대표 실패:** vendor 필드명이 서비스 전체에 퍼져 교체가 어렵고 provider 오류 문자열에 업무 로직이 의존한다.

**검증 evidence:** adapter 입력/출력, provider status, retry 가능 여부와 mapping version을 본다.

**직접 행동:** provider별 응답을 공통 Result 모델로 변환한다.

**다음 연결:** `scheduled job과 cron을 안전한 업무로 만들기`.

`외부 API를 adapter 뒤에 격리하기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | 외부 API를 adapter 뒤에 격리하기의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | 외부 API를 adapter 뒤에 격리하기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | 외부 API를 adapter 뒤에 격리하기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | 외부 API를 adapter 뒤에 격리하기의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | 외부 API를 adapter 뒤에 격리하기의 개념·실패·운영 판단 교차 확인 |

`외부 API를 adapter 뒤에 격리하기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
