# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 03 · 인증·권한·신뢰 경계를 백엔드 흐름에 넣기

### LESSON 10 · login abuse와 rate limit

## CHAPTER 01 · login abuse와 rate limit — 쉬운 말에서 정확한 계약까지

**출발 개념.** 정상 로그인 실패와 공격자의 대량 추측을 구분하기 어렵기 때문에 계정과 출발지의 행동 패턴을 함께 제한한다.

**아주 쉬운 사건.** 같은 계정으로 100개 IP가 로그인 시도한다. 이 사건에서는 먼저 **rate signal과 lockout DoS를 함께 본다**.

**왜 필요한가.** 정상 동작은 IP·계정·device 등 여러 차원의 rate/velocity를 보고 점진적 delay, challenge, lock 정책을 적용한다. 반대로 공격자가 victim 계정을 일부러 잠글 수 있는 영구 lock이나 IP 하나만 보는 정책을 사용한다.

**암기:** `login abuse와 rate limit`의 역할 한 줄.

**직접 이해:** `login abuse와 rate limit`의 입력·상태·결과 경계.

**AI 위임 가능:** `login abuse와 rate limit` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `같은 계정으로 100개 IP가 로그인 시도한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 실패 분포, 계정별 velocity, source diversity, lock/challenge 결과를 본다.

## CHAPTER 02 · login abuse와 rate limit — 아주 쉬운 예를 한 단계씩 해석

T07-P40: `같은 계정으로 100개 IP가 로그인 시도한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 같은 계정으로 100개 IP가 로그인 시도한다 | T07-P40 외부 입력 | T07-P40: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | rate signal과 lockout DoS를 함께 본다 | T07-P40 판단 기준 | T07-P40/CH08 관측표와 대조 |
| 정상 경로 | T07-P40/CH03 M1→M5 | login abuse와 rate limit: 완료 시점을 단계별로 분리 | T07-P40: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P40/CH06 공격자가 victim 계정을 일부러 잠글 수 있는 영구 lock이나 IP 하나만 보는 정책을 사용한다. | T07-P40: 깨진 계약 하나를 특정 | login abuse와 rate limit: 증상과 원인을 분리 |
| 재검증 | T07-P40/CH10 직접 실행 | T07-P40: 예상값 T07-P040 90 기록 | login abuse와 rate limit: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P40/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P40/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · login abuse와 rate limit — 내부 메커니즘과 상태 전이

IP·계정·device 등 여러 차원의 rate/velocity를 보고 점진적 delay, challenge, lock 정책을 적용한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 같은 계정으로 100개 IP가 로그인 시도한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | rate signal과 lockout DoS를 함께 본다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | IP·계정·device 등 여러 차원의 rate/velocity를 보고 점진적 delay, challenge, lock 정책을 적용한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 실패 분포, 계정별 velocity, source diversity, lock/challenge 결과를 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 시간창 내 실패 횟수로 단계별 대응을 계산한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`login abuse와 rate limit` 흐름을 framework 이름 없이 설명한다.

막히면 `같은 계정으로 100개 IP가 로그인 시도한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · login abuse와 rate limit — 실전 경계 A

**경계 A.** login rate limit은 IP 하나만 보면 NAT 사용자들을 함께 막고 account 하나만 보면 공격자가 여러 IP를 쓸 수 있어 IP·account·device signal을 조합하는 trade-off가 있다.

T07-P40/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P40에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P40/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P40/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P40): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P40/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P40-A1-523 | T07-P40 조건 | login rate limit은 IP 하나만 보면 NAT 사용자들을 함께 막고 account 하나만 보면 공격자가 여러 IP를 쓸 수 있어 IP·account·device signal을 조합하는 trade-off가 있다. |
| T07-P40-A2-524 | T07-P40 변화점 | T07-P40/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P40-A3-525 | T07-P40 반례 | T07-P40/CH06 대표 실패와 A 위반을 구별 |
| T07-P40-A4-526 | T07-P40 근거 | T07-P40/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P40-A5-527 | T07-P40 재실험 | 같은 계정으로 100개 IP가 로그인 시도한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · login abuse와 rate limit — 실전 경계 B

**경계 B.** 무조건 계정을 잠그는 정책은 공격자가 타인의 계정을 반복 시도해 정상 사용자를 잠그는 denial-of-service 수단이 될 수 있다.

T07-P40/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P40에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P40/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P40/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P40): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P40/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P40-B1-554 | T07-P40 조건 | 무조건 계정을 잠그는 정책은 공격자가 타인의 계정을 반복 시도해 정상 사용자를 잠그는 denial-of-service 수단이 될 수 있다. |
| T07-P40-B2-555 | T07-P40 독립성 | T07-P40/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P40-B3-556 | T07-P40 상태 | T07-P40/CH03 before·after 위치를 다시 지정 |
| T07-P40-B4-557 | T07-P40 반증 | T07-P40/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P40-B5-558 | T07-P40 적용 | login abuse와 rate limit의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · login abuse와 rate limit — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **공격자가 victim 계정을 일부러 잠글 수 있는 영구 lock이나 IP 하나만 보는 정책을 사용한다.**

아래 여섯 사례는 T07-P40의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P40-F01 | T07-P40: 대표 실패 | T07-P40: T07-P40/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P40: 현상만 보고 원인을 확정 | T07-P40/CH08 evidence map에서 상태를 대조 |
| T07-P40-F02 | T07-P40: 경계 A 누락 | T07-P40: T07-P40/CH04 경계 A 위반 입력 | T07-P40: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P40/CH08 evidence map에서 상태를 대조 |
| T07-P40-F03 | T07-P40: 경계 B 누락 | T07-P40: T07-P40/CH05 경계 B 위반 입력 | T07-P40: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P40/CH08 evidence map에서 상태를 대조 |
| T07-P40-F04 | T07-P40: 복구 경계 C 누락 | T07-P40: T07-P40/CH07 경계 C 복구 조건 | T07-P40: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P40/CH08 evidence map에서 상태를 대조 |
| T07-P40-F05 | T07-P40: 운영 경계 D 누락 | T07-P40: T07-P40/CH09 경계 D 운영 조건 | T07-P40: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P40/CH08 evidence map에서 상태를 대조 |
| T07-P40-F06 | T07-P40: 증거 없는 결론 | T07-P40: T07-P40/CH02 첫 판단만 존재 | T07-P40: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P40/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P40/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · login abuse와 rate limit — 복구 가능한 상태와 수명

**경계 C.** credential stuffing은 다른 서비스에서 유출된 credential 재사용을 대량 시험하는 공격이라 단순 password length 정책만으로 막을 수 없고 anomaly/rate/MFA가 함께 필요하다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P40에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P40/CH08 evidence map을 본다. 복구 후에는 T07-P40/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P40에서 이미 확정된 side effect는 T07-P40/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P40-R1-616 | T07-P40 중단 직전 | T07-P40/CH03에서 이미 확정된 상태만 표시 |
| T07-P40-R2-617 | T07-P40 재시작 직후 | credential stuffing은 다른 서비스에서 유출된 credential 재사용을 대량 시험하는 공격이라 단순 password length 정책만으로 막을 수 없고 anomaly/rate/MFA가 함께 필요하다. |
| T07-P40-R3-618 | T07-P40 재검증 | T07-P40/CH08 근거로 중복·누락 여부 확인 |
| T07-P40-R4-619 | T07-P40 재실행 | T07-P40/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · login abuse와 rate limit — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 같은 계정으로 100개 IP가 로그인 시도한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | rate signal과 lockout DoS를 함께 본다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | IP·계정·device 등 여러 차원의 rate/velocity를 보고 점진적 delay, challenge, lock 정책을 적용한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 공격자가 victim 계정을 일부러 잠글 수 있는 영구 lock이나 IP 하나만 보는 정책을 사용한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 실패 분포, 계정별 velocity, source diversity, lock/challenge 결과를 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`login abuse와 rate limit` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · login abuse와 rate limit — 운영 한계와 종료 조건

**경계 D.** 로그인 실패율·challenge 발생·MFA 실패·recovery 사용량을 관측하되 password나 OTP 자체는 기록하지 않는다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P40/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P40/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P40 과제: 경계 D와 T07-P40/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P40-O1-678 | synthetic-load=178 | T07-P40/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P40-O2-679 | synthetic-budget=828ms | T07-P40 timeout과 unknown outcome을 분리 |
| T07-P40-O3-680 | T07-P40 종료 | 로그인 실패율·challenge 발생·MFA 실패·recovery 사용량을 관측하되 password나 OTP 자체는 기록하지 않는다. |
| T07-P40-O4-681 | T07-P40 완화 | T07-P40/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · login abuse와 rate limit — 직접 실행하는 작은 모델

`login abuse와 rate limit` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P040 90`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P040";
let remaining = 500;
for (const cost of [140, 180, 90]) remaining -= cost;
console.log(marker, remaining);
```

기준 출력: `T07-P040 90`.

`login abuse와 rate limit`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P40-L1-710 | constmarker="T07-P040"; | T07-P40 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P40-L2-711 | letremaining=500; | T07-P40 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P40-L3-712 | for(constcostof[140,180,90])remaining-=cost; | T07-P40 계산 지점; 수정 전후 결과가 갈리는 위치 |
| T07-P40-L4-713 | console.log(marker,remaining); | T07-P40 출력 관측점; 예상 `T07-P040 90`와 비교 |
| T07-P40-LX-799 | T07-P40 실행 기록 | T07-P40 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · login abuse와 rate limit — 한 부분만 수정하고 다시 예측

수정 과제: **두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다**.

수정 전은 `T07-P040 90`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P40/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P40-D1-740 | 기준 `T07-P040 90` | T07-P40 수정 전 실행을 먼저 재현 |
| T07-P40-D2-741 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | T07-P40 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P40-D3-742 | T07-P40 새 예측 | T07-P40 실행 전에 출력·상태를 먼저 기록 |
| T07-P40-D4-743 | T07-P40 재실행 | T07-P40/CH10 실제값과 새 예측을 대조 |
| T07-P40-D5-744 | T07-P40 반례 | T07-P40/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P40-D6-745 | T07-P40 근거 | T07-P40/CH08 상태가 설명과 일치해야 완료 |
| T07-P40-D7-746 | T07-P40 이유 | T07-P40 변경 이유를 login abuse와 rate limit 계약과 연결해 설명 |

## CHAPTER 12 · login abuse와 rate limit — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: login abuse와 rate limit | 정상 로그인 실패와 공격자의 대량 추측을 구분하기 어렵기 때문에 계정과 출발지의 행동 패턴을 함께 제한한다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P40/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | IP·계정·device 등 여러 차원의 rate/velocity를 보고 점진적 delay, challenge, lock 정책을 적용한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 공격자가 victim 계정을 일부러 잠글 수 있는 영구 lock이나 IP 하나만 보는 정책을 사용한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 실패 분포, 계정별 velocity, source diversity, lock/challenge 결과를 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P40/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P40/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P40/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P40/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `login abuse와 rate limit` 실행 코드 수정 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | `login abuse와 rate limit` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P40/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P40/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · login abuse와 rate limit — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 정상 로그인 실패와 공격자의 대량 추측을 구분하기 어렵기 때문에 계정과 출발지의 행동 패턴을 함께 제한한다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P40/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P40/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P40/CH10 실행용 boilerplate·test 후보 | T07-P40: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P040 90` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P40/CH02의 판단 기준과 T07-P40/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P40-AI1-802 | T07-P40 사람 결정 | T07-P40 업무 의미·허용 위험·완료 기준 소유 |
| T07-P40-AI2-803 | T07-P40 AI 초안 | T07-P40/CH10 boilerplate·test 후보까지만 위임 |
| T07-P40-AI3-804 | T07-P40 검증 | T07-P40/CH06 반례와 T07-P40/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · login abuse와 rate limit — 경계 조합 실험 8개

T07-P40: T07-P40/CH04~T07-P40/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P40의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-K01 | T07-P40: 경계 A | T07-P40: 경계 B | T07-P40: OVERLOAD[rps=578; p99_ms=453; queue=28] / sample=13 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + QUEUE_PRESSURE |
| T07-P40-K02 | T07-P40: 경계 A | T07-P40: 경계 C | T07-P40: SAMPLING[sample_rate=40%; trace_present=0; metric_present=1] / sample=20 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P40-K03 | T07-P40: 경계 A | T07-P40: 경계 D | T07-P40: DISCONNECT[disconnect_ms=63; commit_state=UNKNOWN; request=040-03] / sample=27 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + COMMIT_TIMELINE |
| T07-P40-K04 | T07-P40: 경계 B | T07-P40: 경계 C | T07-P40: RECURRENCE[occurrence=6; interval_s=189; mitigation_applied=1] / sample=34 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + RECURRENCE_TIMELINE |
| T07-P40-K05 | T07-P40: 경계 B | T07-P40: 경계 D | T07-P40: LARGE_INPUT[body_kb=1872; limit_kb=256; parsed=0] / sample=41 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + SIZE_LIMIT |
| T07-P40-K06 | T07-P40: 경계 C | T07-P40: 경계 D | T07-P40: OWNER_SPOOF[actor=A5; payload_owner=B1; auth_owner=A5] / sample=48 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + AUTHZ_POLICY |
| T07-P40-K07 | T07-P40: 경계 A | T07-P40: 경계 B+C | T07-P40: REORDER[in_seq=10,8,9; applied_version=5] / sample=55 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + SEQUENCE_STATE |
| T07-P40-K08 | T07-P40: 경계 B | T07-P40: 경계 C+D | T07-P40: RECOVERY_SCOPE[selected=223; expected=16; backup=1; dry_run=0] / sample=62 | T07-P40: 먼저 깨지는 경계를 판정 | T07-P40: T07-P40/CH08 + RECOVERY_AUDIT |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P40/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · login abuse와 rate limit — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P40와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-B09 | T07-P40: 민감정보를 로그와 오류에서 제거하기 | T07-P40: error log에 Authorization header가 남는다 | T07-P40: 관측성과 민감정보 최소화를 함께 지킨다 | T07-P40: T07-P40/CH08 증거와 형제 LESSON 증거를 분리 | T07-P40: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P40-B11 | T07-P40: URL fetch와 SSRF trust boundary | T07-P40: 사용자가 입력한 URL을 server가 fetch한다 | T07-P40: scheme·host·redirect·resolved IP를 검증한다 | T07-P40: T07-P40/CH08 증거와 형제 LESSON 증거를 분리 | T07-P40: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P40-B08 | T07-P40: allowlist로 입력·출력 필드를 좁히기 | T07-P40: update payload에 role=admin이 섞인다 | T07-P40: allowlist DTO로 server-owned field를 막는다 | T07-P40: T07-P40/CH08 증거와 형제 LESSON 증거를 분리 | T07-P40: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P40-B12 | T07-P40: file upload를 데이터와 실행물 사이 경계로 보기 | T07-P40: jpg 확장자 파일이 거대한 압축 payload다 | T07-P40: 형식·크기·격리 상태를 분리한다 | T07-P40: T07-P40/CH08 증거와 형제 LESSON 증거를 분리 | T07-P40: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P40-B07 | T07-P40: secret와 configuration을 코드에서 분리하기 | T07-P40: API key가 config 파일과 로그에 보인다 | T07-P40: secret lifecycle과 redaction을 본다 | T07-P40: T07-P40/CH08 증거와 형제 LESSON 증거를 분리 | T07-P40: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · login abuse와 rate limit — 선택형 실패 주입 6개

T07-P40: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P40 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-X01 | T07-P40: client disconnect | T07-P40: 응답 전에 연결이 끊김; sample=260 | T07-P40: 연결 종료와 server effect | T07-P40: T07-P40/CH02 판단과 별도 기록 | T07-P40: commit 시각·worker/outbox·request lifecycle |
| T07-P40-X02 | T07-P40: 재발 | T07-P40: 같은 오류가 잠시 뒤 다시 발생; sample=277 | T07-P40: 완화와 원인 제거 | T07-P40: T07-P40/CH02 판단과 별도 기록 | T07-P40: 재발 timeline·변경점·resource state |
| T07-P40-X03 | T07-P40: 대형 입력 | T07-P40: 입력 크기가 정상의 100배; sample=294 | T07-P40: 의미 검증과 resource limit | T07-P40: T07-P40/CH02 판단과 별도 기록 | T07-P40: body/batch size·parse time·memory·reject status |
| T07-P40-X04 | T07-P40: 소유권 위조 | T07-P40: tenant/owner가 payload에 포함됨; sample=311 | T07-P40: 식별 정보와 권한 근거 | T07-P40: T07-P40/CH02 판단과 별도 기록 | T07-P40: authenticated context·resource owner·policy result |
| T07-P40-X05 | T07-P40: 순서 역전 | T07-P40: event가 원래 순서와 반대로 도착; sample=328 | T07-P40: 수신 순서와 업무 순서 | T07-P40: T07-P40/CH02 판단과 별도 기록 | T07-P40: version/sequence·dedupe id·applied state |
| T07-P40-X06 | T07-P40: 복구 범위 | T07-P40: 복구 script 대상이 예상보다 큼; sample=345 | T07-P40: 진단과 destructive recovery | T07-P40: T07-P40/CH02 판단과 별도 기록 | T07-P40: selected ids/count·backup·audit trail |

## CHAPTER 17 · login abuse와 rate limit — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P40에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-E01 | T07-P40: 대표 실패를 원인으로 착각 | T07-P40: T07-P40/CH06 실패 case를 다른 입력으로 재현 | T07-P40: 현상과 원인을 같은 것으로 봄 | T07-P40: T07-P40/CH06 대표 실패와 T07-P40/CH08 증거를 다시 대조 | T07-P40: T07-P40/CH08 |
| T07-P40-E02 | T07-P40: 경계 A 생략 | T07-P40: T07-P40/CH04의 조건 하나를 반대로 설정 | T07-P40: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P40: T07-P40/CH04를 새 입력에 적용 | T07-P40: T07-P40/CH08 |
| T07-P40-E03 | T07-P40: 경계 B 생략 | T07-P40: T07-P40/CH05의 조건 하나를 반대로 설정 | T07-P40: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P40: T07-P40/CH05를 새 입력에 적용 | T07-P40: T07-P40/CH08 |
| T07-P40-E04 | T07-P40: 복구 상태 혼동 | T07-P40: T07-P40/CH07에서 처리 중단을 주입 | T07-P40: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P40: T07-P40/CH07에서 수명 경계를 다시 표시 | T07-P40: T07-P40/CH08 |
| T07-P40-E05 | T07-P40: 운영 한계 누락 | T07-P40: T07-P40/CH09에서 부하 또는 drain 조건을 변경 | T07-P40: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P40: T07-P40/CH09의 종료 조건을 다시 작성 | T07-P40: T07-P40/CH08 |
| T07-P40-E06 | T07-P40: 증거 없는 성공 판정 | T07-P40: T07-P40/CH08에서 증거 하나를 숨김 | T07-P40: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P40: T07-P40/CH08에서 독립 증거 둘을 선택 | T07-P40: T07-P40/CH08 |

## CHAPTER 18 · login abuse와 rate limit — synthetic 관측값 판독 6개

T07-P40: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P40의 숫자 하나만으로 원인을 단정하지 않고 T07-P40/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-O01 | T07-P40/auth_failures | 588 | T07-P40: 인증 실패 수 | T07-P40: 축=순서 역전; 원인 확정 금지 | T07-P40: SEQUENCE_STATE + T07-P40/CH08 |
| T07-P40-O02 | T07-P40/policy_denials | 605 | T07-P40: 권한 거부 수 | T07-P40: 축=복구 범위; 원인 확정 금지 | T07-P40: RECOVERY_AUDIT + T07-P40/CH08 |
| T07-P40-O03 | T07-P40/validation_rejects | 622 | T07-P40: 입력 거부 수 | T07-P40: 축=재전송; 원인 확정 금지 | T07-P40: IDEMPOTENCY_RECORD + T07-P40/CH08 |
| T07-P40-O04 | T07-P40/rate_limited | 639 | T07-P40: rate-limit 적용 수 | T07-P40: 축=구버전 client; 원인 확정 금지 | T07-P40: CLIENT_VERSION + T07-P40/CH08 |
| T07-P40-O05 | T07-P40/redactions | 656 | T07-P40: 민감정보 마스킹 수 | T07-P40: 축=unknown outcome; 원인 확정 금지 | T07-P40: PROVIDER_RESULT + T07-P40/CH08 |
| T07-P40-O06 | T07-P40/suspicious_requests | 673 | T07-P40: 의심 요청 수 | T07-P40: 축=과부하; 원인 확정 금지 | T07-P40: QUEUE_PRESSURE + T07-P40/CH08 |

## CHAPTER 19 · login abuse와 rate limit — 선택형 코드 리뷰 6질문

T07-P40: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P40에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-R01 | T07-P40: 동시성 | T07-P40: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P40: RECOVERY_SCOPE[selected=135; expected=9; backup=1; dry_run=0] | T07-P40: T07-P40/CH04 | T07-P40: RECOVERY_AUDIT |
| T07-P40-R02 | T07-P40: 권한 | T07-P40: actor·action·resource가 같은 판단 안에 있는가 | T07-P40: REPLAY[key=cmd-040-01; attempts=3; response_seen=0] | T07-P40: T07-P40/CH05 | T07-P40: IDEMPOTENCY_RECORD |
| T07-P40-R03 | T07-P40: 자원 | T07-P40: pool·queue·memory·connection 상한이 있는가 | T07-P40: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P40: T07-P40/CH06 | T07-P40: CLIENT_VERSION |
| T07-P40-R04 | T07-P40: 상태 변경 | T07-P40: side effect가 어느 줄에서 확정되는가 | T07-P40: UNKNOWN_OUTCOME[timeout_ms=248; provider_state=UNKNOWN; lookup_id=p04003] | T07-P40: T07-P40/CH07 | T07-P40: PROVIDER_RESULT |
| T07-P40-R05 | T07-P40: 동시성 | T07-P40: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P40: OVERLOAD[rps=677; p99_ms=804; queue=30] | T07-P40: T07-P40/CH04 | T07-P40: QUEUE_PRESSURE |
| T07-P40-R06 | T07-P40: 권한 | T07-P40: actor·action·resource가 같은 판단 안에 있는가 | T07-P40: SAMPLING[sample_rate=79%; trace_present=1; metric_present=1] | T07-P40: T07-P40/CH05 | T07-P40: TRACE_METRIC_CROSSCHECK |

## CHAPTER 20 · login abuse와 rate limit — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P40에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-I01 | T07-P40: 영향 범위 | T07-P40: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P40: REPLAY[key=cmd-040-00; attempts=2; response_seen=0] | T07-P40: T07-P40/CH08 + IDEMPOTENCY_RECORD | T07-P40-incident-760 |
| T07-P40-I02 | T07-P40: 변경 동결 | T07-P40: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P40: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P40: T07-P40/CH08 + CLIENT_VERSION | T07-P40-incident-761 |
| T07-P40-I03 | T07-P40: correlation | T07-P40: 한 request/job/resource id를 시간축에 고정 | T07-P40: UNKNOWN_OUTCOME[timeout_ms=237; provider_state=UNKNOWN; lookup_id=p04002] | T07-P40: T07-P40/CH08 + PROVIDER_RESULT | T07-P40-incident-762 |
| T07-P40-I04 | T07-P40: 마지막 정상 | T07-P40: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P40: OVERLOAD[rps=644; p99_ms=687; queue=16] | T07-P40: T07-P40/CH08 + QUEUE_PRESSURE | T07-P40-incident-763 |
| T07-P40-I05 | T07-P40: 가설 검증 | T07-P40: 원인 후보 하나만 뒤집어 재현 | T07-P40: SAMPLING[sample_rate=66%; trace_present=0; metric_present=1] | T07-P40: T07-P40/CH08 + TRACE_METRIC_CROSSCHECK | T07-P40-incident-764 |
| T07-P40-I06 | T07-P40: 복구 확인 | T07-P40: durable state와 사용자 결과를 모두 확인 | T07-P40: DISCONNECT[disconnect_ms=89; commit_state=UNKNOWN; request=040-05] | T07-P40: T07-P40/CH08 + COMMIT_TIMELINE | T07-P40-incident-765 |

## CHAPTER 21 · login abuse와 rate limit — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P40/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P40/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P40/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P40/CH04~T07-P40/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P40/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P40/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P40/CH18 signal 두 개와 T07-P40/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P40/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P40/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · login abuse와 rate limit — 통합 casebook 16문제

T07-P40 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P40 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P40-C01 | T07-P40: 경계 A | T07-P40: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P40: load=360, window=60s | T07-P40: review=관측 | T07-P40: 경계 A 위반 여부를 판정 | T07-P40: CLIENT_VERSION + T07-P40/CH08 |
| T07-P40-C02 | T07-P40: 경계 B | T07-P40: UNKNOWN_OUTCOME[timeout_ms=226; provider_state=UNKNOWN; lookup_id=p04001] | T07-P40: load=383, window=79s | T07-P40: review=상태 변경 | T07-P40: 경계 B 위반 여부를 판정 | T07-P40: PROVIDER_RESULT + T07-P40/CH08 |
| T07-P40-C03 | T07-P40: 경계 C | T07-P40: OVERLOAD[rps=611; p99_ms=570; queue=22] | T07-P40: load=406, window=98s | T07-P40: review=retry | T07-P40: 경계 C 위반 여부를 판정 | T07-P40: QUEUE_PRESSURE + T07-P40/CH08 |
| T07-P40-C04 | T07-P40: 경계 D | T07-P40: SAMPLING[sample_rate=53%; trace_present=1; metric_present=1] | T07-P40: load=429, window=27s | T07-P40: review=복구 | T07-P40: 경계 D 위반 여부를 판정 | T07-P40: TRACE_METRIC_CROSSCHECK + T07-P40/CH08 |
| T07-P40-C05 | T07-P40: 경계 A | T07-P40: DISCONNECT[disconnect_ms=76; commit_state=UNKNOWN; request=040-04] | T07-P40: load=452, window=46s | T07-P40: review=동시성 | T07-P40: 경계 A 위반 여부를 판정 | T07-P40: COMMIT_TIMELINE + T07-P40/CH08 |
| T07-P40-C06 | T07-P40: 경계 B | T07-P40: RECURRENCE[occurrence=2; interval_s=200; mitigation_applied=1] | T07-P40: load=475, window=65s | T07-P40: review=민감정보 | T07-P40: 경계 B 위반 여부를 판정 | T07-P40: RECURRENCE_TIMELINE + T07-P40/CH08 |
| T07-P40-C07 | T07-P40: 경계 C | T07-P40: LARGE_INPUT[body_kb=1960; limit_kb=256; parsed=0] | T07-P40: load=498, window=84s | T07-P40: review=중복 | T07-P40: 경계 C 위반 여부를 판정 | T07-P40: SIZE_LIMIT + T07-P40/CH08 |
| T07-P40-C08 | T07-P40: 경계 D | T07-P40: OWNER_SPOOF[actor=A5; payload_owner=B2; auth_owner=A5] | T07-P40: load=521, window=13s | T07-P40: review=권한 | T07-P40: 경계 D 위반 여부를 판정 | T07-P40: AUTHZ_POLICY + T07-P40/CH08 |
| T07-P40-C09 | T07-P40: 경계 A | T07-P40: REORDER[in_seq=11,9,10; applied_version=5] | T07-P40: load=544, window=32s | T07-P40: review=입력 경계 | T07-P40: 경계 A 위반 여부를 판정 | T07-P40: SEQUENCE_STATE + T07-P40/CH08 |
| T07-P40-C10 | T07-P40: 경계 B | T07-P40: RECOVERY_SCOPE[selected=234; expected=10; backup=1; dry_run=1] | T07-P40: load=567, window=51s | T07-P40: review=timeout | T07-P40: 경계 B 위반 여부를 판정 | T07-P40: RECOVERY_AUDIT + T07-P40/CH08 |
| T07-P40-C11 | T07-P40: 경계 C | T07-P40: REPLAY[key=cmd-040-10; attempts=3; response_seen=0] | T07-P40: load=590, window=70s | T07-P40: review=자원 | T07-P40: 경계 C 위반 여부를 판정 | T07-P40: IDEMPOTENCY_RECORD + T07-P40/CH08 |
| T07-P40-C12 | T07-P40: 경계 D | T07-P40: OLD_SCHEMA[client=v4; server=v5; extra_field=1] | T07-P40: load=613, window=89s | T07-P40: review=순서 | T07-P40: 경계 D 위반 여부를 판정 | T07-P40: CLIENT_VERSION + T07-P40/CH08 |
| T07-P40-C13 | T07-P40: 경계 A | T07-P40: UNKNOWN_OUTCOME[timeout_ms=136; provider_state=UNKNOWN; lookup_id=p04012] | T07-P40: load=636, window=18s | T07-P40: review=관측 | T07-P40: 경계 A 위반 여부를 판정 | T07-P40: PROVIDER_RESULT + T07-P40/CH08 |
| T07-P40-C14 | T07-P40: 경계 B | T07-P40: OVERLOAD[rps=341; p99_ms=984; queue=39] | T07-P40: load=659, window=37s | T07-P40: review=상태 변경 | T07-P40: 경계 B 위반 여부를 판정 | T07-P40: QUEUE_PRESSURE + T07-P40/CH08 |
| T07-P40-C15 | T07-P40: 경계 C | T07-P40: SAMPLING[sample_rate=19%; trace_present=0; metric_present=1] | T07-P40: load=682, window=56s | T07-P40: review=retry | T07-P40: 경계 C 위반 여부를 판정 | T07-P40: TRACE_METRIC_CROSSCHECK + T07-P40/CH08 |
| T07-P40-C16 | T07-P40: 경계 D | T07-P40: DISCONNECT[disconnect_ms=25; commit_state=UNKNOWN; request=040-15] | T07-P40: load=705, window=75s | T07-P40: review=복구 | T07-P40: 경계 D 위반 여부를 판정 | T07-P40: COMMIT_TIMELINE + T07-P40/CH08 |

채점은 결론보다 근거를 본다. T07-P40/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · login abuse와 rate limit — evidence 판독 문제 14개

T07-P40 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P40 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P40-V01 | T07-P40: latency=1660ms; queue=40; retry=5 | T07-P40: unknown outcome | T07-P40: 축=unknown outcome; 원인 확정은 보류 | T07-P40: PROVIDER_RESULT + T07-P40/CH08 | T07-P40: 피할 오판=복구 과잉 |
| T07-P40-V02 | T07-P40: latency=1727ms; queue=51; retry=1 | T07-P40: 과부하 | T07-P40: 축=과부하; 원인 확정은 보류 | T07-P40: QUEUE_PRESSURE + T07-P40/CH08 | T07-P40: 피할 오판=잘못된 전제 |
| T07-P40-V03 | T07-P40: latency=1794ms; queue=62; retry=4 | T07-P40: sampling | T07-P40: 축=sampling; 원인 확정은 보류 | T07-P40: TRACE_METRIC_CROSSCHECK + T07-P40/CH08 | T07-P40: 피할 오판=경계 누락 |
| T07-P40-V04 | T07-P40: latency=61ms; queue=73; retry=0 | T07-P40: client disconnect | T07-P40: 축=client disconnect; 원인 확정은 보류 | T07-P40: COMMIT_TIMELINE + T07-P40/CH08 | T07-P40: 피할 오판=증거 혼동 |
| T07-P40-V05 | T07-P40: latency=128ms; queue=4; retry=3 | T07-P40: 재발 | T07-P40: 축=재발; 원인 확정은 보류 | T07-P40: RECURRENCE_TIMELINE + T07-P40/CH08 | T07-P40: 피할 오판=소유권 혼동 |
| T07-P40-V06 | T07-P40: latency=195ms; queue=15; retry=6 | T07-P40: 대형 입력 | T07-P40: 축=대형 입력; 원인 확정은 보류 | T07-P40: SIZE_LIMIT + T07-P40/CH08 | T07-P40: 피할 오판=운영 한계 누락 |
| T07-P40-V07 | T07-P40: latency=262ms; queue=26; retry=2 | T07-P40: 소유권 위조 | T07-P40: 축=소유권 위조; 원인 확정은 보류 | T07-P40: AUTHZ_POLICY + T07-P40/CH08 | T07-P40: 피할 오판=오류 합치기 |
| T07-P40-V08 | T07-P40: latency=329ms; queue=37; retry=5 | T07-P40: 순서 역전 | T07-P40: 축=순서 역전; 원인 확정은 보류 | T07-P40: SEQUENCE_STATE + T07-P40/CH08 | T07-P40: 피할 오판=AI 과신 |
| T07-P40-V09 | T07-P40: latency=396ms; queue=48; retry=1 | T07-P40: 복구 범위 | T07-P40: 축=복구 범위; 원인 확정은 보류 | T07-P40: RECOVERY_AUDIT + T07-P40/CH08 | T07-P40: 피할 오판=복구 과잉 |
| T07-P40-V10 | T07-P40: latency=463ms; queue=59; retry=4 | T07-P40: 재전송 | T07-P40: 축=재전송; 원인 확정은 보류 | T07-P40: IDEMPOTENCY_RECORD + T07-P40/CH08 | T07-P40: 피할 오판=잘못된 전제 |
| T07-P40-V11 | T07-P40: latency=530ms; queue=70; retry=0 | T07-P40: 구버전 client | T07-P40: 축=구버전 client; 원인 확정은 보류 | T07-P40: CLIENT_VERSION + T07-P40/CH08 | T07-P40: 피할 오판=경계 누락 |
| T07-P40-V12 | T07-P40: latency=597ms; queue=1; retry=3 | T07-P40: unknown outcome | T07-P40: 축=unknown outcome; 원인 확정은 보류 | T07-P40: PROVIDER_RESULT + T07-P40/CH08 | T07-P40: 피할 오판=증거 혼동 |
| T07-P40-V13 | T07-P40: latency=664ms; queue=12; retry=6 | T07-P40: 과부하 | T07-P40: 축=과부하; 원인 확정은 보류 | T07-P40: QUEUE_PRESSURE + T07-P40/CH08 | T07-P40: 피할 오판=소유권 혼동 |
| T07-P40-V14 | T07-P40: latency=731ms; queue=23; retry=2 | T07-P40: sampling | T07-P40: 축=sampling; 원인 확정은 보류 | T07-P40: TRACE_METRIC_CROSSCHECK + T07-P40/CH08 | T07-P40: 피할 오판=운영 한계 누락 |

## CHAPTER 24 · login abuse와 rate limit — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P40에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P40-D01 | T07-P40: pool 확대 | T07-P40: connection/worker pool 상한을 늘린다 | T07-P40: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P40: SAMPLING[sample_rate=14%; trace_present=0; metric_present=1] | T07-P40: TRACE_METRIC_CROSSCHECK + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P40-D02 | T07-P40: schema 변경 | T07-P40: 필드 이름·형식·required 조건을 바꾼다 | T07-P40: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P40: DISCONNECT[disconnect_ms=37; commit_state=UNKNOWN; request=040-01] | T07-P40: COMMIT_TIMELINE + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P40-D03 | T07-P40: 결과 합치기 | T07-P40: 여러 오류를 하나의 status/error code로 합친다 | T07-P40: client 행동과 retry 가능성을 잃지 않는지 | T07-P40: RECURRENCE[occurrence=4; interval_s=167; mitigation_applied=1] | T07-P40: RECURRENCE_TIMELINE + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P40-D04 | T07-P40: retry 추가 | T07-P40: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P40: unknown outcome과 duplicate side effect를 구분하는지 | T07-P40: LARGE_INPUT[body_kb=1696; limit_kb=256; parsed=0] | T07-P40: SIZE_LIMIT + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P40-D05 | T07-P40: 로그 확대 | T07-P40: debug를 위해 payload와 context 기록을 늘린다 | T07-P40: secret·PII·cardinality 비용을 통제하는지 | T07-P40: OWNER_SPOOF[actor=A5; payload_owner=B4; auth_owner=A5] | T07-P40: AUTHZ_POLICY + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P40-D06 | T07-P40: 강제 종료 | T07-P40: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P40: in-flight request와 background work의 결과를 잃는지 | T07-P40: REORDER[in_seq=8,6,7; applied_version=5] | T07-P40: SEQUENCE_STATE + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P40-D07 | T07-P40: AI package 추가 | T07-P40: AI가 제안한 새 dependency를 도입한다 | T07-P40: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P40: RECOVERY_SCOPE[selected=201; expected=11; backup=1; dry_run=0] | T07-P40: RECOVERY_AUDIT + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P40-D08 | T07-P40: 비동기화 | T07-P40: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P40: durability·status API·worker retry 계약이 생기는지 | T07-P40: REPLAY[key=cmd-040-07; attempts=3; response_seen=0] | T07-P40: IDEMPOTENCY_RECORD + T07-P40/CH08 | T07-P40: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P40: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · login abuse와 rate limit — 최종 contract와 evidence spine

**최종 계약:** 정상 로그인 실패와 공격자의 대량 추측을 구분하기 어렵기 때문에 계정과 출발지의 행동 패턴을 함께 제한한다.

**정상 메커니즘:** IP·계정·device 등 여러 차원의 rate/velocity를 보고 점진적 delay, challenge, lock 정책을 적용한다.

**대표 실패:** 공격자가 victim 계정을 일부러 잠글 수 있는 영구 lock이나 IP 하나만 보는 정책을 사용한다.

**검증 evidence:** 실패 분포, 계정별 velocity, source diversity, lock/challenge 결과를 본다.

**직접 행동:** 시간창 내 실패 횟수로 단계별 대응을 계산한다.

**다음 연결:** `URL fetch와 SSRF trust boundary`.

`login abuse와 rate limit`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| OWASP-API | OWASP-API | login abuse와 rate limit의 개념·실패·운영 판단 교차 확인 |
| OWASP-ASVS | OWASP-ASVS | login abuse와 rate limit의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | login abuse와 rate limit의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | login abuse와 rate limit의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | login abuse와 rate limit의 개념·실패·운영 판단 교차 확인 |

`login abuse와 rate limit` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
