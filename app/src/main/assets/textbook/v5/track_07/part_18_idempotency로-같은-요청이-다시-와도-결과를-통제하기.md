# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 02 · 업무 규칙과 상태 변화가 깨지지 않게 만들기

### LESSON 03 · idempotency로 같은 요청이 다시 와도 결과를 통제하기

## CHAPTER 01 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 네트워크에서는 client가 응답을 못 받아 같은 요청을 다시 보낼 수 있으므로 중복 실행을 별도 문제로 다뤄야 한다.

**아주 쉬운 사건.** 같은 결제 요청이 두 번 도착한다. 이 사건에서는 먼저 **NEW·REPLAY·CONFLICT를 나눈다**.

**왜 필요한가.** 정상 동작은 idempotency key와 요청 fingerprint를 저장해 같은 의도의 재시도는 기존 결과를 재사용하고 다른 payload 충돌은 거절한다. 반대로 결제·주문이 중복 생성되거나 key만 같으면 다른 body까지 같은 요청으로 오인한다.

**암기:** `idempotency로 같은 요청이 다시 와도 결과를 통제하기`의 역할 한 줄.

**직접 이해:** `idempotency로 같은 요청이 다시 와도 결과를 통제하기`의 입력·상태·결과 경계.

**AI 위임 가능:** `idempotency로 같은 요청이 다시 와도 결과를 통제하기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `같은 결제 요청이 두 번 도착한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 key, fingerprint, 최초 처리 상태, 재시도 횟수, 반환한 기존 결과 id를 확인한다.

## CHAPTER 02 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 아주 쉬운 예를 한 단계씩 해석

T07-P18: `같은 결제 요청이 두 번 도착한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 같은 결제 요청이 두 번 도착한다 | T07-P18 외부 입력 | T07-P18: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | NEW·REPLAY·CONFLICT를 나눈다 | T07-P18 판단 기준 | T07-P18/CH08 관측표와 대조 |
| 정상 경로 | T07-P18/CH03 M1→M5 | idempotency로 같은 요청이 다시 와도 결과를 통제하기: 완료 시점을 단계별로 분리 | T07-P18: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P18/CH06 결제·주문이 중복 생성되거나 key만 같으면 다른 body까지 같은 요청으로 오인한다. | T07-P18: 깨진 계약 하나를 특정 | idempotency로 같은 요청이 다시 와도 결과를 통제하기: 증상과 원인을 분리 |
| 재검증 | T07-P18/CH10 직접 실행 | T07-P18: 예상값 T07-P018 410 기록 | idempotency로 같은 요청이 다시 와도 결과를 통제하기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P18/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P18/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 내부 메커니즘과 상태 전이

idempotency key와 요청 fingerprint를 저장해 같은 의도의 재시도는 기존 결과를 재사용하고 다른 payload 충돌은 거절한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 같은 결제 요청이 두 번 도착한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | NEW·REPLAY·CONFLICT를 나눈다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | idempotency key와 요청 fingerprint를 저장해 같은 의도의 재시도는 기존 결과를 재사용하고 다른 payload 충돌은 거절한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | key, fingerprint, 최초 처리 상태, 재시도 횟수, 반환한 기존 결과 id를 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | key와 payload hash를 비교해 NEW/REPLAY/CONFLICT를 판정한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`idempotency로 같은 요청이 다시 와도 결과를 통제하기` 흐름을 framework 이름 없이 설명한다.

막히면 `같은 결제 요청이 두 번 도착한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 실전 경계 A

**경계 A.** idempotency key는 모든 사용자에게 전역 한 칸이 아니라 actor·operation 같은 scope와 함께 저장해야 우연히 같은 문자열을 쓴 다른 요청이 충돌하지 않는다.

T07-P18/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P18에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P18/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P18/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P18): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P18/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P18-A1-642 | T07-P18 조건 | idempotency key는 모든 사용자에게 전역 한 칸이 아니라 actor·operation 같은 scope와 함께 저장해야 우연히 같은 문자열을 쓴 다른 요청이 충돌하지 않는다. |
| T07-P18-A2-643 | T07-P18 변화점 | T07-P18/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P18-A3-644 | T07-P18 반례 | T07-P18/CH06 대표 실패와 A 위반을 구별 |
| T07-P18-A4-645 | T07-P18 근거 | T07-P18/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P18-A5-646 | T07-P18 재실험 | 같은 결제 요청이 두 번 도착한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 실전 경계 B

**경계 B.** 같은 key라도 payload가 달라졌는지 canonical request fingerprint를 비교해 동일 재시도와 key 오용을 구분한다.

T07-P18/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P18에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P18/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P18/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P18): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P18/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P18-B1-673 | T07-P18 조건 | 같은 key라도 payload가 달라졌는지 canonical request fingerprint를 비교해 동일 재시도와 key 오용을 구분한다. |
| T07-P18-B2-674 | T07-P18 독립성 | T07-P18/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P18-B3-675 | T07-P18 상태 | T07-P18/CH03 before·after 위치를 다시 지정 |
| T07-P18-B4-676 | T07-P18 반증 | T07-P18/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P18-B5-677 | T07-P18 적용 | idempotency로 같은 요청이 다시 와도 결과를 통제하기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **결제·주문이 중복 생성되거나 key만 같으면 다른 body까지 같은 요청으로 오인한다.**

아래 여섯 사례는 T07-P18의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P18-F01 | T07-P18: 대표 실패 | T07-P18: T07-P18/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P18: 현상만 보고 원인을 확정 | T07-P18/CH08 evidence map에서 상태를 대조 |
| T07-P18-F02 | T07-P18: 경계 A 누락 | T07-P18: T07-P18/CH04 경계 A 위반 입력 | T07-P18: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P18/CH08 evidence map에서 상태를 대조 |
| T07-P18-F03 | T07-P18: 경계 B 누락 | T07-P18: T07-P18/CH05 경계 B 위반 입력 | T07-P18: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P18/CH08 evidence map에서 상태를 대조 |
| T07-P18-F04 | T07-P18: 복구 경계 C 누락 | T07-P18: T07-P18/CH07 경계 C 복구 조건 | T07-P18: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P18/CH08 evidence map에서 상태를 대조 |
| T07-P18-F05 | T07-P18: 운영 경계 D 누락 | T07-P18: T07-P18/CH09 경계 D 운영 조건 | T07-P18: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P18/CH08 evidence map에서 상태를 대조 |
| T07-P18-F06 | T07-P18: 증거 없는 결론 | T07-P18: T07-P18/CH02 첫 판단만 존재 | T07-P18: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P18/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P18/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 복구 가능한 상태와 수명

**경계 C.** 처리 중 PENDING, 완료 COMPLETED, 확정 실패 FAILED 같은 상태를 두면 첫 요청이 아직 끝나지 않았는데 두 번째 요청이 들어온 race를 명시적으로 다룰 수 있다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P18에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P18/CH08 evidence map을 본다. 복구 후에는 T07-P18/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P18에서 이미 확정된 side effect는 T07-P18/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P18-R1-735 | T07-P18 중단 직전 | T07-P18/CH03에서 이미 확정된 상태만 표시 |
| T07-P18-R2-736 | T07-P18 재시작 직후 | 처리 중 PENDING, 완료 COMPLETED, 확정 실패 FAILED 같은 상태를 두면 첫 요청이 아직 끝나지 않았는데 두 번째 요청이 들어온 race를 명시적으로 다룰 수 있다. |
| T07-P18-R3-737 | T07-P18 재검증 | T07-P18/CH08 근거로 중복·누락 여부 확인 |
| T07-P18-R4-738 | T07-P18 재실행 | T07-P18/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 같은 결제 요청이 두 번 도착한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | NEW·REPLAY·CONFLICT를 나눈다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | idempotency key와 요청 fingerprint를 저장해 같은 의도의 재시도는 기존 결과를 재사용하고 다른 payload 충돌은 거절한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 결제·주문이 중복 생성되거나 key만 같으면 다른 body까지 같은 요청으로 오인한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | key, fingerprint, 최초 처리 상태, 재시도 횟수, 반환한 기존 결과 id를 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`idempotency로 같은 요청이 다시 와도 결과를 통제하기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 운영 한계와 종료 조건

**경계 D.** key를 영원히 보관할 수 없으므로 보존 기간 이후 재사용 규칙을 정하고, 외부 결제처럼 중복 비용이 큰 작업은 provider idempotency와 내부 기록을 함께 설계한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P18/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P18/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P18 과제: 경계 D와 T07-P18/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P18-O1-797 | synthetic-load=117 | T07-P18/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P18-O2-798 | synthetic-budget=947ms | T07-P18 timeout과 unknown outcome을 분리 |
| T07-P18-O3-799 | T07-P18 종료 | key를 영원히 보관할 수 없으므로 보존 기간 이후 재사용 규칙을 정하고, 외부 결제처럼 중복 비용이 큰 작업은 provider idempotency와 내부 기록을 함께 설계한다. |
| T07-P18-O4-800 | T07-P18 완화 | T07-P18/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 직접 실행하는 작은 모델

`idempotency로 같은 요청이 다시 와도 결과를 통제하기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P018 410`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P018";
let remaining = 800;
for (const cost of [120, 180, 90]) remaining -= cost;
console.log(marker, remaining);
```

기준 출력: `T07-P018 410`.

`idempotency로 같은 요청이 다시 와도 결과를 통제하기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P18-L1-829 | constmarker="T07-P018"; | T07-P18 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P18-L2-830 | letremaining=800; | T07-P18 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P18-L3-831 | for(constcostof[120,180,90])remaining-=cost; | T07-P18 계산 지점; 수정 전후 결과가 갈리는 위치 |
| T07-P18-L4-832 | console.log(marker,remaining); | T07-P18 출력 관측점; 예상 `T07-P018 410`와 비교 |
| T07-P18-LX-918 | T07-P18 실행 기록 | T07-P18 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 한 부분만 수정하고 다시 예측

수정 과제: **두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다**.

수정 전은 `T07-P018 410`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P18/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P18-D1-859 | 기준 `T07-P018 410` | T07-P18 수정 전 실행을 먼저 재현 |
| T07-P18-D2-860 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | T07-P18 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P18-D3-861 | T07-P18 새 예측 | T07-P18 실행 전에 출력·상태를 먼저 기록 |
| T07-P18-D4-862 | T07-P18 재실행 | T07-P18/CH10 실제값과 새 예측을 대조 |
| T07-P18-D5-863 | T07-P18 반례 | T07-P18/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P18-D6-864 | T07-P18 근거 | T07-P18/CH08 상태가 설명과 일치해야 완료 |
| T07-P18-D7-865 | T07-P18 이유 | T07-P18 변경 이유를 idempotency로 같은 요청이 다시 와도 결과를 통제하기 계약과 연결해 설명 |

## CHAPTER 12 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: idempotency로 같은 요청이 다시 와도 결과를 통제하기 | 네트워크에서는 client가 응답을 못 받아 같은 요청을 다시 보낼 수 있으므로 중복 실행을 별도 문제로 다뤄야 한다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P18/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | idempotency key와 요청 fingerprint를 저장해 같은 의도의 재시도는 기존 결과를 재사용하고 다른 payload 충돌은 거절한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 결제·주문이 중복 생성되거나 key만 같으면 다른 body까지 같은 요청으로 오인한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | key, fingerprint, 최초 처리 상태, 재시도 횟수, 반환한 기존 결과 id를 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P18/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P18/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P18/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P18/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `idempotency로 같은 요청이 다시 와도 결과를 통제하기` 실행 코드 수정 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | `idempotency로 같은 요청이 다시 와도 결과를 통제하기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P18/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P18/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 네트워크에서는 client가 응답을 못 받아 같은 요청을 다시 보낼 수 있으므로 중복 실행을 별도 문제로 다뤄야 한다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P18/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P18/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P18/CH10 실행용 boilerplate·test 후보 | T07-P18: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P018 410` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P18/CH02의 판단 기준과 T07-P18/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P18-AI1-921 | T07-P18 사람 결정 | T07-P18 업무 의미·허용 위험·완료 기준 소유 |
| T07-P18-AI2-922 | T07-P18 AI 초안 | T07-P18/CH10 boilerplate·test 후보까지만 위임 |
| T07-P18-AI3-923 | T07-P18 검증 | T07-P18/CH06 반례와 T07-P18/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 경계 조합 실험 8개

T07-P18: T07-P18/CH04~T07-P18/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P18의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-K01 | T07-P18: 경계 A | T07-P18: 경계 B | T07-P18: REORDER[in_seq=4,2,3; applied_version=1] / sample=38 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + SEQUENCE_STATE |
| T07-P18-K02 | T07-P18: 경계 A | T07-P18: 경계 C | T07-P18: RECOVERY_SCOPE[selected=152; expected=11; backup=1; dry_run=0] / sample=45 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + RECOVERY_AUDIT |
| T07-P18-K03 | T07-P18: 경계 A | T07-P18: 경계 D | T07-P18: REPLAY[key=cmd-018-03; attempts=2; response_seen=0] / sample=52 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + IDEMPOTENCY_RECORD |
| T07-P18-K04 | T07-P18: 경계 B | T07-P18: 경계 C | T07-P18: OLD_SCHEMA[client=v3; server=v4; extra_field=0] / sample=59 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + CLIENT_VERSION |
| T07-P18-K05 | T07-P18: 경계 B | T07-P18: 경계 D | T07-P18: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=83] / sample=66 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + STATE_VERSION |
| T07-P18-K06 | T07-P18: 경계 C | T07-P18: 경계 D | T07-P18: UNKNOWN_OUTCOME[timeout_ms=276; provider_state=UNKNOWN; lookup_id=p01806] / sample=73 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + PROVIDER_RESULT |
| T07-P18-K07 | T07-P18: 경계 A | T07-P18: 경계 B+C | T07-P18: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] / sample=80 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + DURABLE_STATE |
| T07-P18-K08 | T07-P18: 경계 B | T07-P18: 경계 C+D | T07-P18: OVERLOAD[rps=794; p99_ms=525; queue=23] / sample=87 | T07-P18: 먼저 깨지는 경계를 판정 | T07-P18: T07-P18/CH08 + QUEUE_PRESSURE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P18/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P18와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-B02 | T07-P18: command와 query를 구분해 use case를 선명하게 만들기 | T07-P18: 조회 endpoint가 lastSeen을 갱신한다 | T07-P18: query와 업무 side effect를 구분한다 | T07-P18: T07-P18/CH08 증거와 형제 LESSON 증거를 분리 | T07-P18: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P18-B04 | T07-P18: state machine으로 가능한 상태 전이를 제한하기 | T07-P18: PAID 주문에 PAY event가 다시 온다 | T07-P18: state transition table로 거부한다 | T07-P18: T07-P18/CH08 증거와 형제 LESSON 증거를 분리 | T07-P18: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P18-B01 | T07-P18: domain model과 invariant | T07-P18: 재고가 1인데 2개를 빼려 한다 | T07-P18: invariant가 invalid state를 막아야 한다 | T07-P18: T07-P18/CH08 증거와 형제 LESSON 증거를 분리 | T07-P18: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P18-B05 | T07-P18: 돈·수량·비율에서 정밀도 규칙 정하기 | T07-P18: 10% 할인과 세금 반올림 순서가 다르다 | T07-P18: minor unit과 rounding policy를 고정한다 | T07-P18: T07-P18/CH08 증거와 형제 LESSON 증거를 분리 | T07-P18: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P18-B06 | T07-P18: 시간·timezone·deadline을 업무 데이터로 다루기 | T07-P18: 자정 경계와 다른 timezone이 만난다 | T07-P18: instant와 local date를 분리한다 | T07-P18: T07-P18/CH08 증거와 형제 LESSON 증거를 분리 | T07-P18: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 선택형 실패 주입 6개

T07-P18: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P18 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-X01 | T07-P18: 재전송 | T07-P18: 응답 유실 뒤 같은 command가 다시 도착함; sample=575 | T07-P18: 중복 side effect 여부 | T07-P18: T07-P18/CH02 판단과 별도 기록 | T07-P18: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P18-X02 | T07-P18: 구버전 client | T07-P18: 한 단계 이전 schema가 요청됨; sample=592 | T07-P18: 호환 입력과 breaking change | T07-P18: T07-P18/CH02 판단과 별도 기록 | T07-P18: schema version·실제 client 분포·contract test |
| T07-P18-X03 | T07-P18: 동시 변경 | T07-P18: 두 actor가 같은 resource를 수정함; sample=609 | T07-P18: lost update 또는 conflict | T07-P18: T07-P18/CH02 판단과 별도 기록 | T07-P18: version·affected rows·lock/wait 기록 |
| T07-P18-X04 | T07-P18: unknown outcome | T07-P18: dependency timeout 후 성공 여부 불명; sample=626 | T07-P18: 실패와 미확정 결과 | T07-P18: T07-P18/CH02 판단과 별도 기록 | T07-P18: provider id·조회 결과·retry history |
| T07-P18-X05 | T07-P18: 재시작 | T07-P18: side effect 직후 process가 재시작됨; sample=643 | T07-P18: durable state와 memory state | T07-P18: T07-P18/CH02 판단과 별도 기록 | T07-P18: commit·outbox·job id·restart 전후 상태 |
| T07-P18-X06 | T07-P18: 과부하 | T07-P18: traffic 세 배, p99 급증; sample=660 | T07-P18: 기능 실패와 saturation | T07-P18: T07-P18/CH02 판단과 별도 기록 | T07-P18: queue age·pool wait·CPU/event-loop·quota |

## CHAPTER 17 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P18에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-E01 | T07-P18: 대표 실패를 원인으로 착각 | T07-P18: T07-P18/CH06 실패 case를 다른 입력으로 재현 | T07-P18: 현상과 원인을 같은 것으로 봄 | T07-P18: T07-P18/CH06 대표 실패와 T07-P18/CH08 증거를 다시 대조 | T07-P18: T07-P18/CH08 |
| T07-P18-E02 | T07-P18: 경계 A 생략 | T07-P18: T07-P18/CH04의 조건 하나를 반대로 설정 | T07-P18: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P18: T07-P18/CH04를 새 입력에 적용 | T07-P18: T07-P18/CH08 |
| T07-P18-E03 | T07-P18: 경계 B 생략 | T07-P18: T07-P18/CH05의 조건 하나를 반대로 설정 | T07-P18: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P18: T07-P18/CH05를 새 입력에 적용 | T07-P18: T07-P18/CH08 |
| T07-P18-E04 | T07-P18: 복구 상태 혼동 | T07-P18: T07-P18/CH07에서 처리 중단을 주입 | T07-P18: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P18: T07-P18/CH07에서 수명 경계를 다시 표시 | T07-P18: T07-P18/CH08 |
| T07-P18-E05 | T07-P18: 운영 한계 누락 | T07-P18: T07-P18/CH09에서 부하 또는 drain 조건을 변경 | T07-P18: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P18: T07-P18/CH09의 종료 조건을 다시 작성 | T07-P18: T07-P18/CH08 |
| T07-P18-E06 | T07-P18: 증거 없는 성공 판정 | T07-P18: T07-P18/CH08에서 증거 하나를 숨김 | T07-P18: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P18: T07-P18/CH08에서 독립 증거 둘을 선택 | T07-P18: T07-P18/CH08 |

## CHAPTER 18 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — synthetic 관측값 판독 6개

T07-P18: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P18의 숫자 하나만으로 원인을 단정하지 않고 T07-P18/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-O01 | T07-P18/commands_received | 932 | T07-P18: command 수신 수 | T07-P18: 축=재시작; 원인 확정 금지 | T07-P18: DURABLE_STATE + T07-P18/CH08 |
| T07-P18-O02 | T07-P18/state_version | 949 | T07-P18: 상태 버전 | T07-P18: 축=과부하; 원인 확정 금지 | T07-P18: QUEUE_PRESSURE + T07-P18/CH08 |
| T07-P18-O03 | T07-P18/conflicts | 966 | T07-P18: 동시 수정 충돌 수 | T07-P18: 축=sampling; 원인 확정 금지 | T07-P18: TRACE_METRIC_CROSSCHECK + T07-P18/CH08 |
| T07-P18-O04 | T07-P18/retries | 983 | T07-P18: 재처리 수 | T07-P18: 축=client disconnect; 원인 확정 금지 | T07-P18: COMMIT_TIMELINE + T07-P18/CH08 |
| T07-P18-O05 | T07-P18/side_effects | 3 | T07-P18: 외부 side effect 수 | T07-P18: 축=재발; 원인 확정 금지 | T07-P18: RECURRENCE_TIMELINE + T07-P18/CH08 |
| T07-P18-O06 | T07-P18/invariant_violations | 20 | T07-P18: invariant 위반 수 | T07-P18: 축=대형 입력; 원인 확정 금지 | T07-P18: SIZE_LIMIT + T07-P18/CH08 |

## CHAPTER 19 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 선택형 코드 리뷰 6질문

T07-P18: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P18에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-R01 | T07-P18: 입력 경계 | T07-P18: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P18: OVERLOAD[rps=530; p99_ms=462; queue=28] | T07-P18: T07-P18/CH04 | T07-P18: QUEUE_PRESSURE |
| T07-P18-R02 | T07-P18: 순서 | T07-P18: old/new event 순서가 바뀌어도 안전한가 | T07-P18: SAMPLING[sample_rate=41%; trace_present=1; metric_present=1] | T07-P18: T07-P18/CH05 | T07-P18: TRACE_METRIC_CROSSCHECK |
| T07-P18-R03 | T07-P18: retry | T07-P18: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P18: DISCONNECT[disconnect_ms=64; commit_state=UNKNOWN; request=018-02] | T07-P18: T07-P18/CH06 | T07-P18: COMMIT_TIMELINE |
| T07-P18-R04 | T07-P18: 민감정보 | T07-P18: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P18: RECURRENCE[occurrence=5; interval_s=173; mitigation_applied=1] | T07-P18: T07-P18/CH07 | T07-P18: RECURRENCE_TIMELINE |
| T07-P18-R05 | T07-P18: 입력 경계 | T07-P18: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P18: LARGE_INPUT[body_kb=1744; limit_kb=640; parsed=0] | T07-P18: T07-P18/CH04 | T07-P18: SIZE_LIMIT |
| T07-P18-R06 | T07-P18: 순서 | T07-P18: old/new event 순서가 바뀌어도 안전한가 | T07-P18: REORDER[in_seq=8,6,7; applied_version=1] | T07-P18: T07-P18/CH05 | T07-P18: SEQUENCE_STATE |

## CHAPTER 20 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P18에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-I01 | T07-P18: correlation | T07-P18: 한 request/job/resource id를 시간축에 고정 | T07-P18: SAMPLING[sample_rate=28%; trace_present=0; metric_present=1] | T07-P18: T07-P18/CH08 + TRACE_METRIC_CROSSCHECK | T07-P18-incident-342 |
| T07-P18-I02 | T07-P18: 마지막 정상 | T07-P18: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P18: DISCONNECT[disconnect_ms=51; commit_state=UNKNOWN; request=018-01] | T07-P18: T07-P18/CH08 + COMMIT_TIMELINE | T07-P18-incident-343 |
| T07-P18-I03 | T07-P18: 가설 검증 | T07-P18: 원인 후보 하나만 뒤집어 재현 | T07-P18: RECURRENCE[occurrence=4; interval_s=162; mitigation_applied=1] | T07-P18: T07-P18/CH08 + RECURRENCE_TIMELINE | T07-P18-incident-344 |
| T07-P18-I04 | T07-P18: 복구 확인 | T07-P18: durable state와 사용자 결과를 모두 확인 | T07-P18: LARGE_INPUT[body_kb=1656; limit_kb=640; parsed=0] | T07-P18: T07-P18/CH08 + SIZE_LIMIT | T07-P18-incident-345 |
| T07-P18-I05 | T07-P18: 재주입 | T07-P18: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P18: REORDER[in_seq=7,5,6; applied_version=1] | T07-P18: T07-P18/CH08 + SEQUENCE_STATE | T07-P18-incident-346 |
| T07-P18-I06 | T07-P18: 회귀 고정 | T07-P18: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P18: RECOVERY_SCOPE[selected=185; expected=12; backup=1; dry_run=1] | T07-P18: T07-P18/CH08 + RECOVERY_AUDIT | T07-P18-incident-347 |

## CHAPTER 21 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P18/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P18/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P18/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P18/CH04~T07-P18/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P18/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P18/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P18/CH18 signal 두 개와 T07-P18/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P18/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P18/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 통합 casebook 16문제

T07-P18 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P18 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P18-C01 | T07-P18: 경계 A | T07-P18: DISCONNECT[disconnect_ms=38; commit_state=UNKNOWN; request=018-00] | T07-P18: load=622, window=46s | T07-P18: review=retry | T07-P18: 경계 A 위반 여부를 판정 | T07-P18: COMMIT_TIMELINE + T07-P18/CH08 |
| T07-P18-C02 | T07-P18: 경계 B | T07-P18: RECURRENCE[occurrence=3; interval_s=151; mitigation_applied=1] | T07-P18: load=645, window=65s | T07-P18: review=복구 | T07-P18: 경계 B 위반 여부를 판정 | T07-P18: RECURRENCE_TIMELINE + T07-P18/CH08 |
| T07-P18-C03 | T07-P18: 경계 C | T07-P18: LARGE_INPUT[body_kb=1568; limit_kb=640; parsed=0] | T07-P18: load=668, window=84s | T07-P18: review=동시성 | T07-P18: 경계 C 위반 여부를 판정 | T07-P18: SIZE_LIMIT + T07-P18/CH08 |
| T07-P18-C04 | T07-P18: 경계 D | T07-P18: REORDER[in_seq=6,4,5; applied_version=1] | T07-P18: load=691, window=13s | T07-P18: review=민감정보 | T07-P18: 경계 D 위반 여부를 판정 | T07-P18: SEQUENCE_STATE + T07-P18/CH08 |
| T07-P18-C05 | T07-P18: 경계 A | T07-P18: RECOVERY_SCOPE[selected=174; expected=18; backup=1; dry_run=0] | T07-P18: load=714, window=32s | T07-P18: review=중복 | T07-P18: 경계 A 위반 여부를 판정 | T07-P18: RECOVERY_AUDIT + T07-P18/CH08 |
| T07-P18-C06 | T07-P18: 경계 B | T07-P18: REPLAY[key=cmd-018-05; attempts=4; response_seen=0] | T07-P18: load=737, window=51s | T07-P18: review=권한 | T07-P18: 경계 B 위반 여부를 판정 | T07-P18: IDEMPOTENCY_RECORD + T07-P18/CH08 |
| T07-P18-C07 | T07-P18: 경계 C | T07-P18: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P18: load=760, window=70s | T07-P18: review=입력 경계 | T07-P18: 경계 C 위반 여부를 판정 | T07-P18: CLIENT_VERSION + T07-P18/CH08 |
| T07-P18-C08 | T07-P18: 경계 D | T07-P18: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=12] | T07-P18: load=783, window=89s | T07-P18: review=timeout | T07-P18: 경계 D 위반 여부를 판정 | T07-P18: STATE_VERSION + T07-P18/CH08 |
| T07-P18-C09 | T07-P18: 경계 A | T07-P18: UNKNOWN_OUTCOME[timeout_ms=298; provider_state=UNKNOWN; lookup_id=p01808] | T07-P18: load=806, window=18s | T07-P18: review=자원 | T07-P18: 경계 A 위반 여부를 판정 | T07-P18: PROVIDER_RESULT + T07-P18/CH08 |
| T07-P18-C10 | T07-P18: 경계 B | T07-P18: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P18: load=829, window=37s | T07-P18: review=순서 | T07-P18: 경계 B 위반 여부를 판정 | T07-P18: DURABLE_STATE + T07-P18/CH08 |
| T07-P18-C11 | T07-P18: 경계 C | T07-P18: OVERLOAD[rps=860; p99_ms=759; queue=31] | T07-P18: load=852, window=56s | T07-P18: review=관측 | T07-P18: 경계 C 위반 여부를 판정 | T07-P18: QUEUE_PRESSURE + T07-P18/CH08 |
| T07-P18-C12 | T07-P18: 경계 D | T07-P18: SAMPLING[sample_rate=74%; trace_present=1; metric_present=1] | T07-P18: load=875, window=75s | T07-P18: review=상태 변경 | T07-P18: 경계 D 위반 여부를 판정 | T07-P18: TRACE_METRIC_CROSSCHECK + T07-P18/CH08 |
| T07-P18-C13 | T07-P18: 경계 A | T07-P18: DISCONNECT[disconnect_ms=97; commit_state=UNKNOWN; request=018-12] | T07-P18: load=898, window=94s | T07-P18: review=retry | T07-P18: 경계 A 위반 여부를 판정 | T07-P18: COMMIT_TIMELINE + T07-P18/CH08 |
| T07-P18-C14 | T07-P18: 경계 B | T07-P18: RECURRENCE[occurrence=5; interval_s=72; mitigation_applied=1] | T07-P18: load=921, window=23s | T07-P18: review=복구 | T07-P18: 경계 B 위반 여부를 판정 | T07-P18: RECURRENCE_TIMELINE + T07-P18/CH08 |
| T07-P18-C15 | T07-P18: 경계 C | T07-P18: LARGE_INPUT[body_kb=936; limit_kb=640; parsed=0] | T07-P18: load=944, window=42s | T07-P18: review=동시성 | T07-P18: 경계 C 위반 여부를 판정 | T07-P18: SIZE_LIMIT + T07-P18/CH08 |
| T07-P18-C16 | T07-P18: 경계 D | T07-P18: REORDER[in_seq=18,16,17; applied_version=1] | T07-P18: load=967, window=61s | T07-P18: review=민감정보 | T07-P18: 경계 D 위반 여부를 판정 | T07-P18: SEQUENCE_STATE + T07-P18/CH08 |

채점은 결론보다 근거를 본다. T07-P18/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — evidence 판독 문제 14개

T07-P18 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P18 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P18-V01 | T07-P18: latency=758ms; queue=46; retry=4 | T07-P18: 재발 | T07-P18: 축=재발; 원인 확정은 보류 | T07-P18: RECURRENCE_TIMELINE + T07-P18/CH08 | T07-P18: 피할 오판=경계 누락 |
| T07-P18-V02 | T07-P18: latency=825ms; queue=57; retry=0 | T07-P18: 대형 입력 | T07-P18: 축=대형 입력; 원인 확정은 보류 | T07-P18: SIZE_LIMIT + T07-P18/CH08 | T07-P18: 피할 오판=증거 혼동 |
| T07-P18-V03 | T07-P18: latency=892ms; queue=68; retry=3 | T07-P18: 순서 역전 | T07-P18: 축=순서 역전; 원인 확정은 보류 | T07-P18: SEQUENCE_STATE + T07-P18/CH08 | T07-P18: 피할 오판=재시도 오판 |
| T07-P18-V04 | T07-P18: latency=959ms; queue=79; retry=6 | T07-P18: 복구 범위 | T07-P18: 축=복구 범위; 원인 확정은 보류 | T07-P18: RECOVERY_AUDIT + T07-P18/CH08 | T07-P18: 피할 오판=동시성 무시 |
| T07-P18-V05 | T07-P18: latency=1026ms; queue=10; retry=2 | T07-P18: 재전송 | T07-P18: 축=재전송; 원인 확정은 보류 | T07-P18: IDEMPOTENCY_RECORD + T07-P18/CH08 | T07-P18: 피할 오판=순서 가정 |
| T07-P18-V06 | T07-P18: latency=1093ms; queue=21; retry=5 | T07-P18: 구버전 client | T07-P18: 축=구버전 client; 원인 확정은 보류 | T07-P18: CLIENT_VERSION + T07-P18/CH08 | T07-P18: 피할 오판=상태 수명 혼동 |
| T07-P18-V07 | T07-P18: latency=1160ms; queue=32; retry=1 | T07-P18: 동시 변경 | T07-P18: 축=동시 변경; 원인 확정은 보류 | T07-P18: STATE_VERSION + T07-P18/CH08 | T07-P18: 피할 오판=운영 한계 누락 |
| T07-P18-V08 | T07-P18: latency=1227ms; queue=43; retry=4 | T07-P18: unknown outcome | T07-P18: 축=unknown outcome; 원인 확정은 보류 | T07-P18: PROVIDER_RESULT + T07-P18/CH08 | T07-P18: 피할 오판=오류 합치기 |
| T07-P18-V09 | T07-P18: latency=1294ms; queue=54; retry=0 | T07-P18: 재시작 | T07-P18: 축=재시작; 원인 확정은 보류 | T07-P18: DURABLE_STATE + T07-P18/CH08 | T07-P18: 피할 오판=복구 과잉 |
| T07-P18-V10 | T07-P18: latency=1361ms; queue=65; retry=3 | T07-P18: 과부하 | T07-P18: 축=과부하; 원인 확정은 보류 | T07-P18: QUEUE_PRESSURE + T07-P18/CH08 | T07-P18: 피할 오판=잘못된 전제 |
| T07-P18-V11 | T07-P18: latency=1428ms; queue=76; retry=6 | T07-P18: sampling | T07-P18: 축=sampling; 원인 확정은 보류 | T07-P18: TRACE_METRIC_CROSSCHECK + T07-P18/CH08 | T07-P18: 피할 오판=경계 누락 |
| T07-P18-V12 | T07-P18: latency=1495ms; queue=7; retry=2 | T07-P18: client disconnect | T07-P18: 축=client disconnect; 원인 확정은 보류 | T07-P18: COMMIT_TIMELINE + T07-P18/CH08 | T07-P18: 피할 오판=증거 혼동 |
| T07-P18-V13 | T07-P18: latency=1562ms; queue=18; retry=5 | T07-P18: 재발 | T07-P18: 축=재발; 원인 확정은 보류 | T07-P18: RECURRENCE_TIMELINE + T07-P18/CH08 | T07-P18: 피할 오판=재시도 오판 |
| T07-P18-V14 | T07-P18: latency=1629ms; queue=29; retry=1 | T07-P18: 대형 입력 | T07-P18: 축=대형 입력; 원인 확정은 보류 | T07-P18: SIZE_LIMIT + T07-P18/CH08 | T07-P18: 피할 오판=동시성 무시 |

## CHAPTER 24 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P18에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P18-D01 | T07-P18: 로그 확대 | T07-P18: debug를 위해 payload와 context 기록을 늘린다 | T07-P18: secret·PII·cardinality 비용을 통제하는지 | T07-P18: REORDER[in_seq=3,1,2; applied_version=1] | T07-P18: SEQUENCE_STATE + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P18-D02 | T07-P18: 강제 종료 | T07-P18: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P18: in-flight request와 background work의 결과를 잃는지 | T07-P18: RECOVERY_SCOPE[selected=141; expected=17; backup=1; dry_run=1] | T07-P18: RECOVERY_AUDIT + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P18-D03 | T07-P18: AI package 추가 | T07-P18: AI가 제안한 새 dependency를 도입한다 | T07-P18: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P18: REPLAY[key=cmd-018-02; attempts=4; response_seen=0] | T07-P18: IDEMPOTENCY_RECORD + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P18-D04 | T07-P18: 비동기화 | T07-P18: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P18: durability·status API·worker retry 계약이 생기는지 | T07-P18: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P18: CLIENT_VERSION + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P18-D05 | T07-P18: 권한 shortcut | T07-P18: payload의 owner/tenant id를 바로 사용한다 | T07-P18: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P18: CONCURRENT_WRITE[actors=2; base_version=1; writes=2; gap_ms=70] | T07-P18: STATE_VERSION + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P18-D06 | T07-P18: 순서 병렬화 | T07-P18: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P18: 선후관계 invariant와 race를 깨지 않는지 | T07-P18: UNKNOWN_OUTCOME[timeout_ms=265; provider_state=UNKNOWN; lookup_id=p01805] | T07-P18: PROVIDER_RESULT + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P18-D07 | T07-P18: validation 이동 | T07-P18: validation을 business side effect 뒤로 옮긴다 | T07-P18: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P18: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P18: DURABLE_STATE + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P18-D08 | T07-P18: batch 확대 | T07-P18: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P18: memory·deadline·부분 실패 범위가 커지는지 | T07-P18: OVERLOAD[rps=761; p99_ms=408; queue=29] | T07-P18: QUEUE_PRESSURE + T07-P18/CH08 | T07-P18: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P18: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · idempotency로 같은 요청이 다시 와도 결과를 통제하기 — 최종 contract와 evidence spine

**최종 계약:** 네트워크에서는 client가 응답을 못 받아 같은 요청을 다시 보낼 수 있으므로 중복 실행을 별도 문제로 다뤄야 한다.

**정상 메커니즘:** idempotency key와 요청 fingerprint를 저장해 같은 의도의 재시도는 기존 결과를 재사용하고 다른 payload 충돌은 거절한다.

**대표 실패:** 결제·주문이 중복 생성되거나 key만 같으면 다른 body까지 같은 요청으로 오인한다.

**검증 evidence:** key, fingerprint, 최초 처리 상태, 재시도 횟수, 반환한 기존 결과 id를 확인한다.

**직접 행동:** key와 payload hash를 비교해 NEW/REPLAY/CONFLICT를 판정한다.

**다음 연결:** `state machine으로 가능한 상태 전이를 제한하기`.

`idempotency로 같은 요청이 다시 와도 결과를 통제하기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | idempotency로 같은 요청이 다시 와도 결과를 통제하기의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | idempotency로 같은 요청이 다시 와도 결과를 통제하기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | idempotency로 같은 요청이 다시 와도 결과를 통제하기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | idempotency로 같은 요청이 다시 와도 결과를 통제하기의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | idempotency로 같은 요청이 다시 와도 결과를 통제하기의 개념·실패·운영 판단 교차 확인 |

`idempotency로 같은 요청이 다시 와도 결과를 통제하기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
