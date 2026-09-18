# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 01 · 요청이 서버에 도착해 응답이 되기까지

### LESSON 08 · dependency injection은 외부 의존성을 밖에서 넣는다

## CHAPTER 01 · dependency injection은 외부 의존성을 밖에서 넣는다 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 함수나 객체가 DB·clock·mailer를 직접 만들어 버리지 않고 필요한 기능을 입력으로 받게 하면 교체와 검증이 쉬워진다.

**아주 쉬운 사건.** 현재 시각을 함수 안에서 직접 읽는다. 이 사건에서는 먼저 **clock dependency를 외부에서 넣는다**.

**왜 필요한가.** 정상 동작은 composition root에서 실제 구현을 조립하고 내부 코드는 인터페이스나 함수 계약만 사용한다. 반대로 모든 것을 전역 singleton으로 두어 테스트 간 상태가 섞이거나 지나친 추상화로 흐름이 보이지 않는다.

**암기:** `dependency injection은 외부 의존성을 밖에서 넣는다`의 역할 한 줄.

**직접 이해:** `dependency injection은 외부 의존성을 밖에서 넣는다`의 입력·상태·결과 경계.

**AI 위임 가능:** `dependency injection은 외부 의존성을 밖에서 넣는다` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `현재 시각을 함수 안에서 직접 읽는다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 객체 생성 위치, 의존 그래프, 테스트에서 대체되는 경계를 확인한다.

## CHAPTER 02 · dependency injection은 외부 의존성을 밖에서 넣는다 — 아주 쉬운 예를 한 단계씩 해석

T07-P08: `현재 시각을 함수 안에서 직접 읽는다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 현재 시각을 함수 안에서 직접 읽는다 | T07-P08 외부 입력 | T07-P08: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | clock dependency를 외부에서 넣는다 | T07-P08 판단 기준 | T07-P08/CH08 관측표와 대조 |
| 정상 경로 | T07-P08/CH03 M1→M5 | dependency injection은 외부 의존성을 밖에서 넣는다: 완료 시점을 단계별로 분리 | T07-P08: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P08/CH06 모든 것을 전역 singleton으로 두어 테스트 간 상태가 섞이거나 지나친 추상화로 흐름이 보이지 않는다. | T07-P08: 깨진 계약 하나를 특정 | dependency injection은 외부 의존성을 밖에서 넣는다: 증상과 원인을 분리 |
| 재검증 | T07-P08/CH10 직접 실행 | T07-P08: 예상값 T07-P008 RECEIVED->CHECKED->APPLIED 기록 | dependency injection은 외부 의존성을 밖에서 넣는다: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P08/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P08/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · dependency injection은 외부 의존성을 밖에서 넣는다 — 내부 메커니즘과 상태 전이

composition root에서 실제 구현을 조립하고 내부 코드는 인터페이스나 함수 계약만 사용한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 현재 시각을 함수 안에서 직접 읽는다 | source/actor/size를 보존 |
| M2 | 경계 판단 | clock dependency를 외부에서 넣는다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | composition root에서 실제 구현을 조립하고 내부 코드는 인터페이스나 함수 계약만 사용한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 객체 생성 위치, 의존 그래프, 테스트에서 대체되는 경계를 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | clock 함수를 인자로 받아 시간이 고정된 상태에서도 같은 결과가 나오는 코드를 실행한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`dependency injection은 외부 의존성을 밖에서 넣는다` 흐름을 framework 이름 없이 설명한다.

막히면 `현재 시각을 함수 안에서 직접 읽는다`의 M1→M5를 다시 추적한다.

### 전이 체크포인트
| case | 위치 | 확인할 차이 |
| --- | --- | --- |
| T07-P08-M1-813 | T07-P08/CH02 입력 | T07-P08 읽기 전용 단계 |
| T07-P08-M2-814 | T07-P08/CH03 판단 | T07-P08 상태 변경 직전 경계 |
| T07-P08-M3-815 | T07-P08/CH08 검증 | T07-P08 변경 뒤 실제 상태 대조 |
| T07-P08-M4-816 | T07-P08/CH06 반례 | T07-P08 성공 문자열과 상태 성공을 분리 |

## CHAPTER 04 · dependency injection은 외부 의존성을 밖에서 넣는다 — 실전 경계 A

**경계 A.** composition root는 실제 DB client·clock·mailer 같은 구현을 한곳에서 조립하고 핵심 로직에는 필요한 능력만 넘기는 위치다. 객체 생성이 곳곳에 퍼지면 어떤 구현이 쓰이는지 추적하기 어렵다.

T07-P08/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P08에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P08/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P08/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P08): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P08/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P08-A1-844 | T07-P08 조건 | composition root는 실제 DB client·clock·mailer 같은 구현을 한곳에서 조립하고 핵심 로직에는 필요한 능력만 넘기는 위치다. 객체 생성이 곳곳에 퍼지면 어떤 구현이 쓰이는지 추적하기 어렵다. |
| T07-P08-A2-845 | T07-P08 변화점 | T07-P08/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P08-A3-846 | T07-P08 반례 | T07-P08/CH06 대표 실패와 A 위반을 구별 |
| T07-P08-A4-847 | T07-P08 근거 | T07-P08/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P08-A5-848 | T07-P08 재실험 | 현재 시각을 함수 안에서 직접 읽는다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · dependency injection은 외부 의존성을 밖에서 넣는다 — 실전 경계 B

**경계 B.** dependency lifetime을 singleton·request-scoped·transient로 나눠 생각한다. request별 identity를 singleton 객체의 mutable field에 넣으면 다른 요청에 값이 섞이는 심각한 버그가 생길 수 있다.

T07-P08/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P08에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P08/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P08/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P08): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P08/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P08-B1-875 | T07-P08 조건 | dependency lifetime을 singleton·request-scoped·transient로 나눠 생각한다. request별 identity를 singleton 객체의 mutable field에 넣으면 다른 요청에 값이 섞이는 심각한 버그가 생길 수 있다. |
| T07-P08-B2-876 | T07-P08 독립성 | T07-P08/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P08-B3-877 | T07-P08 상태 | T07-P08/CH03 before·after 위치를 다시 지정 |
| T07-P08-B4-878 | T07-P08 반증 | T07-P08/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P08-B5-879 | T07-P08 적용 | dependency injection은 외부 의존성을 밖에서 넣는다의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · dependency injection은 외부 의존성을 밖에서 넣는다 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **모든 것을 전역 singleton으로 두어 테스트 간 상태가 섞이거나 지나친 추상화로 흐름이 보이지 않는다.**

아래 여섯 사례는 T07-P08의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P08-F01 | T07-P08: 대표 실패 | T07-P08: T07-P08/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P08: 현상만 보고 원인을 확정 | T07-P08/CH08 evidence map에서 상태를 대조 |
| T07-P08-F02 | T07-P08: 경계 A 누락 | T07-P08: T07-P08/CH04 경계 A 위반 입력 | T07-P08: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P08/CH08 evidence map에서 상태를 대조 |
| T07-P08-F03 | T07-P08: 경계 B 누락 | T07-P08: T07-P08/CH05 경계 B 위반 입력 | T07-P08: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P08/CH08 evidence map에서 상태를 대조 |
| T07-P08-F04 | T07-P08: 복구 경계 C 누락 | T07-P08: T07-P08/CH07 경계 C 복구 조건 | T07-P08: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P08/CH08 evidence map에서 상태를 대조 |
| T07-P08-F05 | T07-P08: 운영 경계 D 누락 | T07-P08: T07-P08/CH09 경계 D 운영 조건 | T07-P08: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P08/CH08 evidence map에서 상태를 대조 |
| T07-P08-F06 | T07-P08: 증거 없는 결론 | T07-P08: T07-P08/CH02 첫 판단만 존재 | T07-P08: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P08/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P08/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · dependency injection은 외부 의존성을 밖에서 넣는다 — 복구 가능한 상태와 수명

**경계 C.** clock·random·ID generator를 주입하면 테스트가 현재 시각과 우연한 난수에 흔들리지 않고 동일 입력에서 동일 조건을 검증할 수 있다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P08에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P08/CH08 evidence map을 본다. 복구 후에는 T07-P08/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P08에서 이미 확정된 side effect는 T07-P08/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P08-R1-937 | T07-P08 중단 직전 | T07-P08/CH03에서 이미 확정된 상태만 표시 |
| T07-P08-R2-938 | T07-P08 재시작 직후 | clock·random·ID generator를 주입하면 테스트가 현재 시각과 우연한 난수에 흔들리지 않고 동일 입력에서 동일 조건을 검증할 수 있다. |
| T07-P08-R3-939 | T07-P08 재검증 | T07-P08/CH08 근거로 중복·누락 여부 확인 |
| T07-P08-R4-940 | T07-P08 재실행 | T07-P08/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · dependency injection은 외부 의존성을 밖에서 넣는다 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 현재 시각을 함수 안에서 직접 읽는다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | clock dependency를 외부에서 넣는다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | composition root에서 실제 구현을 조립하고 내부 코드는 인터페이스나 함수 계약만 사용한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 모든 것을 전역 singleton으로 두어 테스트 간 상태가 섞이거나 지나친 추상화로 흐름이 보이지 않는다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 객체 생성 위치, 의존 그래프, 테스트에서 대체되는 경계를 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`dependency injection은 외부 의존성을 밖에서 넣는다` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · dependency injection은 외부 의존성을 밖에서 넣는다 — 운영 한계와 종료 조건

**경계 D.** service locator처럼 내부에서 전역 container를 꺼내 쓰면 함수 signature만 봐서는 의존성을 알 수 없다. 필요한 dependency를 명시적으로 전달해 호출 계약을 읽을 수 있게 한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P08/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P08/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P08 과제: 경계 D와 T07-P08/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P08-O1-2 | synthetic-load=42 | T07-P08/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P08-O2-3 | synthetic-budget=152ms | T07-P08 timeout과 unknown outcome을 분리 |
| T07-P08-O3-4 | T07-P08 종료 | service locator처럼 내부에서 전역 container를 꺼내 쓰면 함수 signature만 봐서는 의존성을 알 수 없다. 필요한 dependency를 명시적으로 전달해 호출 계약을 읽을 수 있게 한다. |
| T07-P08-O4-5 | T07-P08 완화 | T07-P08/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · dependency injection은 외부 의존성을 밖에서 넣는다 — 직접 실행하는 작은 모델

`dependency injection은 외부 의존성을 밖에서 넣는다` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P008 RECEIVED->CHECKED->APPLIED`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P008";
const stages = ["RECEIVED", "CHECKED", "APPLIED", "DONE"];
const stopAt = 3;
const visited = stages.slice(0, stopAt);
console.log(marker, visited.join("->"));
```

기준 출력: `T07-P008 RECEIVED->CHECKED->APPLIED`.

`dependency injection은 외부 의존성을 밖에서 넣는다`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P08-L1-34 | constmarker="T07-P008"; | T07-P08 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P08-L2-35 | conststages=["RECEIVED","CHECKED","APPLIED","DONE"]; | T07-P08 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P08-L3-36 | conststopAt=3; | T07-P08 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P08-L4-37 | constvisited=stages.slice(0,stopAt); | T07-P08 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P08-L5-38 | console.log(marker,visited.join("->")); | T07-P08 출력 관측점; 예상 `T07-P008 RECEIVED->CHECKED->APPLIED`와 비교 |
| T07-P08-LX-123 | T07-P08 실행 기록 | T07-P08 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · dependency injection은 외부 의존성을 밖에서 넣는다 — 한 부분만 수정하고 다시 예측

수정 과제: **stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다**.

수정 전은 `T07-P008 RECEIVED->CHECKED->APPLIED`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P08/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P08-D1-64 | 기준 `T07-P008 RECEIVED->CHECKED->APPLIED` | T07-P08 수정 전 실행을 먼저 재현 |
| T07-P08-D2-65 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | T07-P08 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P08-D3-66 | T07-P08 새 예측 | T07-P08 실행 전에 출력·상태를 먼저 기록 |
| T07-P08-D4-67 | T07-P08 재실행 | T07-P08/CH10 실제값과 새 예측을 대조 |
| T07-P08-D5-68 | T07-P08 반례 | T07-P08/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P08-D6-69 | T07-P08 근거 | T07-P08/CH08 상태가 설명과 일치해야 완료 |
| T07-P08-D7-70 | T07-P08 이유 | T07-P08 변경 이유를 dependency injection은 외부 의존성을 밖에서 넣는다 계약과 연결해 설명 |

## CHAPTER 12 · dependency injection은 외부 의존성을 밖에서 넣는다 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: dependency injection은 외부 의존성을 밖에서 넣는다 | 함수나 객체가 DB·clock·mailer를 직접 만들어 버리지 않고 필요한 기능을 입력으로 받게 하면 교체와 검증이 쉬워진다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P08/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | composition root에서 실제 구현을 조립하고 내부 코드는 인터페이스나 함수 계약만 사용한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 모든 것을 전역 singleton으로 두어 테스트 간 상태가 섞이거나 지나친 추상화로 흐름이 보이지 않는다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 객체 생성 위치, 의존 그래프, 테스트에서 대체되는 경계를 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P08/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P08/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P08/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P08/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `dependency injection은 외부 의존성을 밖에서 넣는다` 실행 코드 수정 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | `dependency injection은 외부 의존성을 밖에서 넣는다` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P08/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P08/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · dependency injection은 외부 의존성을 밖에서 넣는다 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 함수나 객체가 DB·clock·mailer를 직접 만들어 버리지 않고 필요한 기능을 입력으로 받게 하면 교체와 검증이 쉬워진다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P08/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P08/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P08/CH10 실행용 boilerplate·test 후보 | T07-P08: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P008 RECEIVED->CHECKED->APPLIED` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P08/CH02의 판단 기준과 T07-P08/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P08-AI1-126 | T07-P08 사람 결정 | T07-P08 업무 의미·허용 위험·완료 기준 소유 |
| T07-P08-AI2-127 | T07-P08 AI 초안 | T07-P08/CH10 boilerplate·test 후보까지만 위임 |
| T07-P08-AI3-128 | T07-P08 검증 | T07-P08/CH06 반례와 T07-P08/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · dependency injection은 외부 의존성을 밖에서 넣는다 — 경계 조합 실험 8개

T07-P08: T07-P08/CH04~T07-P08/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P08의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-K01 | T07-P08: 경계 A | T07-P08: 경계 B | T07-P08: LARGE_INPUT[body_kb=848; limit_kb=640; parsed=0] / sample=17 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + SIZE_LIMIT |
| T07-P08-K02 | T07-P08: 경계 A | T07-P08: 경계 C | T07-P08: DRAIN[ready=0; active=1; drain_deadline_s=7] / sample=24 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + DRAIN_STATE |
| T07-P08-K03 | T07-P08: 경계 A | T07-P08: 경계 D | T07-P08: REPLAY[key=cmd-008-03; attempts=2; response_seen=0] / sample=31 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + IDEMPOTENCY_RECORD |
| T07-P08-K04 | T07-P08: 경계 B | T07-P08: 경계 C | T07-P08: OLD_SCHEMA[client=v1; server=v2; extra_field=0] / sample=38 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + CLIENT_VERSION |
| T07-P08-K05 | T07-P08: 경계 B | T07-P08: 경계 D | T07-P08: UNKNOWN_OUTCOME[timeout_ms=186; provider_state=UNKNOWN; lookup_id=p00805] / sample=45 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + PROVIDER_RESULT |
| T07-P08-K06 | T07-P08: 경계 C | T07-P08: 경계 D | T07-P08: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] / sample=52 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + DURABLE_STATE |
| T07-P08-K07 | T07-P08: 경계 A | T07-P08: 경계 B+C | T07-P08: OVERLOAD[rps=524; p99_ms=624; queue=33] / sample=59 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + QUEUE_PRESSURE |
| T07-P08-K08 | T07-P08: 경계 B | T07-P08: 경계 C+D | T07-P08: SAMPLING[sample_rate=59%; trace_present=0; metric_present=1] / sample=66 | T07-P08: 먼저 깨지는 경계를 판정 | T07-P08: T07-P08/CH08 + TRACE_METRIC_CROSSCHECK |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P08/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · dependency injection은 외부 의존성을 밖에서 넣는다 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P08와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-B07 | T07-P08: controller·service·repository로 책임을 나누는 이유 | T07-P08: handler가 DB와 mailer까지 직접 부른다 | T07-P08: transport와 use-case 책임을 나눈다 | T07-P08: T07-P08/CH08 증거와 형제 LESSON 증거를 분리 | T07-P08: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P08-B09 | T07-P08: 오류를 종류별로 나누고 응답 계약으로 바꾸기 | T07-P08: 없는 resource와 권한 없는 resource가 섞인다 | T07-P08: public error와 internal cause를 분리한다 | T07-P08: T07-P08/CH08 증거와 형제 LESSON 증거를 분리 | T07-P08: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P08-B06 | T07-P08: validation은 외부 입력을 내부 계약으로 바꾸는 문 | T07-P08: age="20", admin=true가 입력으로 온다 | T07-P08: coercion과 허용 field를 따로 결정한다 | T07-P08: T07-P08/CH08 증거와 형제 LESSON 증거를 분리 | T07-P08: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P08-B10 | T07-P08: async I/O와 event loop를 요청 처리에서 이해하기 | T07-P08: 두 Promise와 CPU loop가 같은 요청에 있다 | T07-P08: 기다림과 CPU 점유를 구분한다 | T07-P08: T07-P08/CH08 증거와 형제 LESSON 증거를 분리 | T07-P08: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P08-B05 | T07-P08: middleware는 공통 단계를 순서대로 연결한다 | T07-P08: auth middleware가 parser보다 먼저/뒤에 놓인다 | T07-P08: middleware 순서를 화살표로 그린다 | T07-P08: T07-P08/CH08 증거와 형제 LESSON 증거를 분리 | T07-P08: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · dependency injection은 외부 의존성을 밖에서 넣는다 — 선택형 실패 주입 6개

T07-P08: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P08 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-X01 | T07-P08: 재전송 | T07-P08: 응답 유실 뒤 같은 command가 다시 도착함; sample=265 | T07-P08: 중복 side effect 여부 | T07-P08: T07-P08/CH02 판단과 별도 기록 | T07-P08: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P08-X02 | T07-P08: 구버전 client | T07-P08: 한 단계 이전 schema가 요청됨; sample=282 | T07-P08: 호환 입력과 breaking change | T07-P08: T07-P08/CH02 판단과 별도 기록 | T07-P08: schema version·실제 client 분포·contract test |
| T07-P08-X03 | T07-P08: unknown outcome | T07-P08: dependency timeout 후 성공 여부 불명; sample=299 | T07-P08: 실패와 미확정 결과 | T07-P08: T07-P08/CH02 판단과 별도 기록 | T07-P08: provider id·조회 결과·retry history |
| T07-P08-X04 | T07-P08: 재시작 | T07-P08: side effect 직후 process가 재시작됨; sample=316 | T07-P08: durable state와 memory state | T07-P08: T07-P08/CH02 판단과 별도 기록 | T07-P08: commit·outbox·job id·restart 전후 상태 |
| T07-P08-X05 | T07-P08: 과부하 | T07-P08: traffic 세 배, p99 급증; sample=333 | T07-P08: 기능 실패와 saturation | T07-P08: T07-P08/CH02 판단과 별도 기록 | T07-P08: queue age·pool wait·CPU/event-loop·quota |
| T07-P08-X06 | T07-P08: sampling | T07-P08: 일부 log가 sampling으로 빠짐; sample=350 | T07-P08: 기록 부재와 사건 부재 | T07-P08: T07-P08/CH02 판단과 별도 기록 | T07-P08: metric·trace·durable state 교차 근거 |

## CHAPTER 17 · dependency injection은 외부 의존성을 밖에서 넣는다 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P08에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-E01 | T07-P08: 대표 실패를 원인으로 착각 | T07-P08: T07-P08/CH06 실패 case를 다른 입력으로 재현 | T07-P08: 현상과 원인을 같은 것으로 봄 | T07-P08: T07-P08/CH06 대표 실패와 T07-P08/CH08 증거를 다시 대조 | T07-P08: T07-P08/CH08 |
| T07-P08-E02 | T07-P08: 경계 A 생략 | T07-P08: T07-P08/CH04의 조건 하나를 반대로 설정 | T07-P08: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P08: T07-P08/CH04를 새 입력에 적용 | T07-P08: T07-P08/CH08 |
| T07-P08-E03 | T07-P08: 경계 B 생략 | T07-P08: T07-P08/CH05의 조건 하나를 반대로 설정 | T07-P08: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P08: T07-P08/CH05를 새 입력에 적용 | T07-P08: T07-P08/CH08 |
| T07-P08-E04 | T07-P08: 복구 상태 혼동 | T07-P08: T07-P08/CH07에서 처리 중단을 주입 | T07-P08: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P08: T07-P08/CH07에서 수명 경계를 다시 표시 | T07-P08: T07-P08/CH08 |
| T07-P08-E05 | T07-P08: 운영 한계 누락 | T07-P08: T07-P08/CH09에서 부하 또는 drain 조건을 변경 | T07-P08: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P08: T07-P08/CH09의 종료 조건을 다시 작성 | T07-P08: T07-P08/CH08 |
| T07-P08-E06 | T07-P08: 증거 없는 성공 판정 | T07-P08: T07-P08/CH08에서 증거 하나를 숨김 | T07-P08: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P08: T07-P08/CH08에서 독립 증거 둘을 선택 | T07-P08: T07-P08/CH08 |

## CHAPTER 18 · dependency injection은 외부 의존성을 밖에서 넣는다 — synthetic 관측값 판독 6개

T07-P08: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P08의 숫자 하나만으로 원인을 단정하지 않고 T07-P08/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-O01 | T07-P08/requests_received | 452 | T07-P08: 요청 수신 수 | T07-P08: 축=과부하; 원인 확정 금지 | T07-P08: QUEUE_PRESSURE + T07-P08/CH08 |
| T07-P08-O02 | T07-P08/handler_started | 469 | T07-P08: handler 진입 수 | T07-P08: 축=sampling; 원인 확정 금지 | T07-P08: TRACE_METRIC_CROSSCHECK + T07-P08/CH08 |
| T07-P08-O03 | T07-P08/responses_completed | 486 | T07-P08: 응답 완료 수 | T07-P08: 축=client disconnect; 원인 확정 금지 | T07-P08: COMMIT_TIMELINE + T07-P08/CH08 |
| T07-P08-O04 | T07-P08/latency_ms | 503 | T07-P08: 요청 처리 지연 | T07-P08: 축=재발; 원인 확정 금지 | T07-P08: RECURRENCE_TIMELINE + T07-P08/CH08 |
| T07-P08-O05 | T07-P08/body_kb | 520 | T07-P08: 입력 크기 | T07-P08: 축=대형 입력; 원인 확정 금지 | T07-P08: SIZE_LIMIT + T07-P08/CH08 |
| T07-P08-O06 | T07-P08/active_connections | 537 | T07-P08: 활성 연결 수 | T07-P08: 축=drain; 원인 확정 금지 | T07-P08: DRAIN_STATE + T07-P08/CH08 |

## CHAPTER 19 · dependency injection은 외부 의존성을 밖에서 넣는다 — 선택형 코드 리뷰 6질문

T07-P08: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P08에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-R01 | T07-P08: 관측 | T07-P08: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P08: SAMPLING[sample_rate=52%; trace_present=0; metric_present=1] | T07-P08: T07-P08/CH04 | T07-P08: TRACE_METRIC_CROSSCHECK |
| T07-P08-R02 | T07-P08: 복구 | T07-P08: 재시작 뒤에도 필요한 상태가 남는가 | T07-P08: DISCONNECT[disconnect_ms=75; commit_state=UNKNOWN; request=008-01] | T07-P08: T07-P08/CH05 | T07-P08: COMMIT_TIMELINE |
| T07-P08-R03 | T07-P08: 중복 | T07-P08: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P08: RECURRENCE[occurrence=4; interval_s=83; mitigation_applied=1] | T07-P08: T07-P08/CH06 | T07-P08: RECURRENCE_TIMELINE |
| T07-P08-R04 | T07-P08: timeout | T07-P08: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P08: LARGE_INPUT[body_kb=1024; limit_kb=640; parsed=0] | T07-P08: T07-P08/CH07 | T07-P08: SIZE_LIMIT |
| T07-P08-R05 | T07-P08: 관측 | T07-P08: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P08: DRAIN[ready=0; active=10; drain_deadline_s=9] | T07-P08: T07-P08/CH04 | T07-P08: DRAIN_STATE |
| T07-P08-R06 | T07-P08: 복구 | T07-P08: 재시작 뒤에도 필요한 상태가 남는가 | T07-P08: REPLAY[key=cmd-008-05; attempts=4; response_seen=0] | T07-P08: T07-P08/CH05 | T07-P08: IDEMPOTENCY_RECORD |

## CHAPTER 20 · dependency injection은 외부 의존성을 밖에서 넣는다 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P08에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-I01 | T07-P08: 영향 범위 | T07-P08: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P08: DISCONNECT[disconnect_ms=62; commit_state=UNKNOWN; request=008-00] | T07-P08: T07-P08/CH08 + COMMIT_TIMELINE | T07-P08-incident-152 |
| T07-P08-I02 | T07-P08: 변경 동결 | T07-P08: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P08: RECURRENCE[occurrence=3; interval_s=72; mitigation_applied=1] | T07-P08: T07-P08/CH08 + RECURRENCE_TIMELINE | T07-P08-incident-153 |
| T07-P08-I03 | T07-P08: correlation | T07-P08: 한 request/job/resource id를 시간축에 고정 | T07-P08: LARGE_INPUT[body_kb=936; limit_kb=640; parsed=0] | T07-P08: T07-P08/CH08 + SIZE_LIMIT | T07-P08-incident-154 |
| T07-P08-I04 | T07-P08: 마지막 정상 | T07-P08: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P08: DRAIN[ready=0; active=14; drain_deadline_s=8] | T07-P08: T07-P08/CH08 + DRAIN_STATE | T07-P08-incident-155 |
| T07-P08-I05 | T07-P08: 가설 검증 | T07-P08: 원인 후보 하나만 뒤집어 재현 | T07-P08: REPLAY[key=cmd-008-04; attempts=3; response_seen=0] | T07-P08: T07-P08/CH08 + IDEMPOTENCY_RECORD | T07-P08-incident-156 |
| T07-P08-I06 | T07-P08: 복구 확인 | T07-P08: durable state와 사용자 결과를 모두 확인 | T07-P08: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P08: T07-P08/CH08 + CLIENT_VERSION | T07-P08-incident-157 |

## CHAPTER 21 · dependency injection은 외부 의존성을 밖에서 넣는다 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P08/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P08/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P08/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P08/CH04~T07-P08/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P08/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P08/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P08/CH18 signal 두 개와 T07-P08/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P08/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P08/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · dependency injection은 외부 의존성을 밖에서 넣는다 — 통합 casebook 16문제

T07-P08 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P08 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P08-C01 | T07-P08: 경계 A | T07-P08: RECURRENCE[occurrence=2; interval_s=61; mitigation_applied=1] | T07-P08: load=332, window=56s | T07-P08: review=동시성 | T07-P08: 경계 A 위반 여부를 판정 | T07-P08: RECURRENCE_TIMELINE + T07-P08/CH08 |
| T07-P08-C02 | T07-P08: 경계 B | T07-P08: LARGE_INPUT[body_kb=848; limit_kb=640; parsed=0] | T07-P08: load=355, window=75s | T07-P08: review=민감정보 | T07-P08: 경계 B 위반 여부를 판정 | T07-P08: SIZE_LIMIT + T07-P08/CH08 |
| T07-P08-C03 | T07-P08: 경계 C | T07-P08: DRAIN[ready=0; active=1; drain_deadline_s=7] | T07-P08: load=378, window=94s | T07-P08: review=중복 | T07-P08: 경계 C 위반 여부를 판정 | T07-P08: DRAIN_STATE + T07-P08/CH08 |
| T07-P08-C04 | T07-P08: 경계 D | T07-P08: REPLAY[key=cmd-008-03; attempts=2; response_seen=0] | T07-P08: load=401, window=23s | T07-P08: review=권한 | T07-P08: 경계 D 위반 여부를 판정 | T07-P08: IDEMPOTENCY_RECORD + T07-P08/CH08 |
| T07-P08-C05 | T07-P08: 경계 A | T07-P08: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P08: load=424, window=42s | T07-P08: review=입력 경계 | T07-P08: 경계 A 위반 여부를 판정 | T07-P08: CLIENT_VERSION + T07-P08/CH08 |
| T07-P08-C06 | T07-P08: 경계 B | T07-P08: UNKNOWN_OUTCOME[timeout_ms=186; provider_state=UNKNOWN; lookup_id=p00805] | T07-P08: load=447, window=61s | T07-P08: review=timeout | T07-P08: 경계 B 위반 여부를 판정 | T07-P08: PROVIDER_RESULT + T07-P08/CH08 |
| T07-P08-C07 | T07-P08: 경계 C | T07-P08: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P08: load=470, window=80s | T07-P08: review=자원 | T07-P08: 경계 C 위반 여부를 판정 | T07-P08: DURABLE_STATE + T07-P08/CH08 |
| T07-P08-C08 | T07-P08: 경계 D | T07-P08: OVERLOAD[rps=524; p99_ms=624; queue=33] | T07-P08: load=493, window=99s | T07-P08: review=순서 | T07-P08: 경계 D 위반 여부를 판정 | T07-P08: QUEUE_PRESSURE + T07-P08/CH08 |
| T07-P08-C09 | T07-P08: 경계 A | T07-P08: SAMPLING[sample_rate=59%; trace_present=0; metric_present=1] | T07-P08: load=516, window=28s | T07-P08: review=관측 | T07-P08: 경계 A 위반 여부를 판정 | T07-P08: TRACE_METRIC_CROSSCHECK + T07-P08/CH08 |
| T07-P08-C10 | T07-P08: 경계 B | T07-P08: DISCONNECT[disconnect_ms=82; commit_state=UNKNOWN; request=008-09] | T07-P08: load=539, window=47s | T07-P08: review=상태 변경 | T07-P08: 경계 B 위반 여부를 판정 | T07-P08: COMMIT_TIMELINE + T07-P08/CH08 |
| T07-P08-C11 | T07-P08: 경계 C | T07-P08: RECURRENCE[occurrence=2; interval_s=171; mitigation_applied=1] | T07-P08: load=562, window=66s | T07-P08: review=retry | T07-P08: 경계 C 위반 여부를 판정 | T07-P08: RECURRENCE_TIMELINE + T07-P08/CH08 |
| T07-P08-C12 | T07-P08: 경계 D | T07-P08: LARGE_INPUT[body_kb=1728; limit_kb=640; parsed=0] | T07-P08: load=585, window=85s | T07-P08: review=복구 | T07-P08: 경계 D 위반 여부를 판정 | T07-P08: SIZE_LIMIT + T07-P08/CH08 |
| T07-P08-C13 | T07-P08: 경계 A | T07-P08: DRAIN[ready=0; active=5; drain_deadline_s=9] | T07-P08: load=608, window=14s | T07-P08: review=동시성 | T07-P08: 경계 A 위반 여부를 판정 | T07-P08: DRAIN_STATE + T07-P08/CH08 |
| T07-P08-C14 | T07-P08: 경계 B | T07-P08: REPLAY[key=cmd-008-13; attempts=3; response_seen=0] | T07-P08: load=631, window=33s | T07-P08: review=민감정보 | T07-P08: 경계 B 위반 여부를 판정 | T07-P08: IDEMPOTENCY_RECORD + T07-P08/CH08 |
| T07-P08-C15 | T07-P08: 경계 C | T07-P08: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P08: load=654, window=52s | T07-P08: review=중복 | T07-P08: 경계 C 위반 여부를 판정 | T07-P08: CLIENT_VERSION + T07-P08/CH08 |
| T07-P08-C16 | T07-P08: 경계 D | T07-P08: UNKNOWN_OUTCOME[timeout_ms=296; provider_state=UNKNOWN; lookup_id=p00815] | T07-P08: load=677, window=71s | T07-P08: review=권한 | T07-P08: 경계 D 위반 여부를 판정 | T07-P08: PROVIDER_RESULT + T07-P08/CH08 |

채점은 결론보다 근거를 본다. T07-P08/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · dependency injection은 외부 의존성을 밖에서 넣는다 — evidence 판독 문제 14개

T07-P08 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P08 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P08-V01 | T07-P08: latency=348ms; queue=56; retry=1 | T07-P08: 대형 입력 | T07-P08: 축=대형 입력; 원인 확정은 보류 | T07-P08: SIZE_LIMIT + T07-P08/CH08 | T07-P08: 피할 오판=복구 과잉 |
| T07-P08-V02 | T07-P08: latency=415ms; queue=67; retry=4 | T07-P08: drain | T07-P08: 축=drain; 원인 확정은 보류 | T07-P08: DRAIN_STATE + T07-P08/CH08 | T07-P08: 피할 오판=잘못된 전제 |
| T07-P08-V03 | T07-P08: latency=482ms; queue=78; retry=0 | T07-P08: 재전송 | T07-P08: 축=재전송; 원인 확정은 보류 | T07-P08: IDEMPOTENCY_RECORD + T07-P08/CH08 | T07-P08: 피할 오판=경계 누락 |
| T07-P08-V04 | T07-P08: latency=549ms; queue=9; retry=3 | T07-P08: 구버전 client | T07-P08: 축=구버전 client; 원인 확정은 보류 | T07-P08: CLIENT_VERSION + T07-P08/CH08 | T07-P08: 피할 오판=증거 혼동 |
| T07-P08-V05 | T07-P08: latency=616ms; queue=20; retry=6 | T07-P08: unknown outcome | T07-P08: 축=unknown outcome; 원인 확정은 보류 | T07-P08: PROVIDER_RESULT + T07-P08/CH08 | T07-P08: 피할 오판=동시성 무시 |
| T07-P08-V06 | T07-P08: latency=683ms; queue=31; retry=2 | T07-P08: 재시작 | T07-P08: 축=재시작; 원인 확정은 보류 | T07-P08: DURABLE_STATE + T07-P08/CH08 | T07-P08: 피할 오판=상태 수명 혼동 |
| T07-P08-V07 | T07-P08: latency=750ms; queue=42; retry=5 | T07-P08: 과부하 | T07-P08: 축=과부하; 원인 확정은 보류 | T07-P08: QUEUE_PRESSURE + T07-P08/CH08 | T07-P08: 피할 오판=운영 한계 누락 |
| T07-P08-V08 | T07-P08: latency=817ms; queue=53; retry=1 | T07-P08: sampling | T07-P08: 축=sampling; 원인 확정은 보류 | T07-P08: TRACE_METRIC_CROSSCHECK + T07-P08/CH08 | T07-P08: 피할 오판=오류 합치기 |
| T07-P08-V09 | T07-P08: latency=884ms; queue=64; retry=4 | T07-P08: client disconnect | T07-P08: 축=client disconnect; 원인 확정은 보류 | T07-P08: COMMIT_TIMELINE + T07-P08/CH08 | T07-P08: 피할 오판=복구 과잉 |
| T07-P08-V10 | T07-P08: latency=951ms; queue=75; retry=0 | T07-P08: 재발 | T07-P08: 축=재발; 원인 확정은 보류 | T07-P08: RECURRENCE_TIMELINE + T07-P08/CH08 | T07-P08: 피할 오판=잘못된 전제 |
| T07-P08-V11 | T07-P08: latency=1018ms; queue=6; retry=3 | T07-P08: 대형 입력 | T07-P08: 축=대형 입력; 원인 확정은 보류 | T07-P08: SIZE_LIMIT + T07-P08/CH08 | T07-P08: 피할 오판=경계 누락 |
| T07-P08-V12 | T07-P08: latency=1085ms; queue=17; retry=6 | T07-P08: drain | T07-P08: 축=drain; 원인 확정은 보류 | T07-P08: DRAIN_STATE + T07-P08/CH08 | T07-P08: 피할 오판=증거 혼동 |
| T07-P08-V13 | T07-P08: latency=1152ms; queue=28; retry=2 | T07-P08: 재전송 | T07-P08: 축=재전송; 원인 확정은 보류 | T07-P08: IDEMPOTENCY_RECORD + T07-P08/CH08 | T07-P08: 피할 오판=동시성 무시 |
| T07-P08-V14 | T07-P08: latency=1219ms; queue=39; retry=5 | T07-P08: 구버전 client | T07-P08: 축=구버전 client; 원인 확정은 보류 | T07-P08: CLIENT_VERSION + T07-P08/CH08 | T07-P08: 피할 오판=상태 수명 혼동 |

## CHAPTER 24 · dependency injection은 외부 의존성을 밖에서 넣는다 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P08에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P08-D01 | T07-P08: cache 추가 | T07-P08: 현재 결과 앞에 cache layer를 추가한다 | T07-P08: stale·key·invalidation 책임이 새로 생기는지 | T07-P08: REPLAY[key=cmd-008-00; attempts=2; response_seen=0] | T07-P08: IDEMPOTENCY_RECORD + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P08-D02 | T07-P08: pool 확대 | T07-P08: connection/worker pool 상한을 늘린다 | T07-P08: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P08: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P08: CLIENT_VERSION + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P08-D03 | T07-P08: schema 변경 | T07-P08: 필드 이름·형식·required 조건을 바꾼다 | T07-P08: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P08: UNKNOWN_OUTCOME[timeout_ms=153; provider_state=UNKNOWN; lookup_id=p00802] | T07-P08: PROVIDER_RESULT + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P08-D04 | T07-P08: 결과 합치기 | T07-P08: 여러 오류를 하나의 status/error code로 합친다 | T07-P08: client 행동과 retry 가능성을 잃지 않는지 | T07-P08: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P08: DURABLE_STATE + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P08-D05 | T07-P08: retry 추가 | T07-P08: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P08: unknown outcome과 duplicate side effect를 구분하는지 | T07-P08: OVERLOAD[rps=425; p99_ms=1146; queue=28] | T07-P08: QUEUE_PRESSURE + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P08-D06 | T07-P08: 로그 확대 | T07-P08: debug를 위해 payload와 context 기록을 늘린다 | T07-P08: secret·PII·cardinality 비용을 통제하는지 | T07-P08: SAMPLING[sample_rate=20%; trace_present=1; metric_present=1] | T07-P08: TRACE_METRIC_CROSSCHECK + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P08-D07 | T07-P08: 강제 종료 | T07-P08: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P08: in-flight request와 background work의 결과를 잃는지 | T07-P08: DISCONNECT[disconnect_ms=43; commit_state=UNKNOWN; request=008-06] | T07-P08: COMMIT_TIMELINE + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P08-D08 | T07-P08: AI package 추가 | T07-P08: AI가 제안한 새 dependency를 도입한다 | T07-P08: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P08: RECURRENCE[occurrence=4; interval_s=138; mitigation_applied=1] | T07-P08: RECURRENCE_TIMELINE + T07-P08/CH08 | T07-P08: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P08: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · dependency injection은 외부 의존성을 밖에서 넣는다 — 최종 contract와 evidence spine

**최종 계약:** 함수나 객체가 DB·clock·mailer를 직접 만들어 버리지 않고 필요한 기능을 입력으로 받게 하면 교체와 검증이 쉬워진다.

**정상 메커니즘:** composition root에서 실제 구현을 조립하고 내부 코드는 인터페이스나 함수 계약만 사용한다.

**대표 실패:** 모든 것을 전역 singleton으로 두어 테스트 간 상태가 섞이거나 지나친 추상화로 흐름이 보이지 않는다.

**검증 evidence:** 객체 생성 위치, 의존 그래프, 테스트에서 대체되는 경계를 확인한다.

**직접 행동:** clock 함수를 인자로 받아 시간이 고정된 상태에서도 같은 결과가 나오는 코드를 실행한다.

**다음 연결:** `오류를 종류별로 나누고 응답 계약으로 바꾸기`.

`dependency injection은 외부 의존성을 밖에서 넣는다`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| NODE-DOCS | Node.js Documentation | dependency injection은 외부 의존성을 밖에서 넣는다의 개념·실패·운영 판단 교차 확인 |
| EXPRESS5 | Express 5 Documentation | dependency injection은 외부 의존성을 밖에서 넣는다의 개념·실패·운영 판단 교차 확인 |
| RFC9110 | RFC9110 | dependency injection은 외부 의존성을 밖에서 넣는다의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | dependency injection은 외부 의존성을 밖에서 넣는다의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | dependency injection은 외부 의존성을 밖에서 넣는다의 개념·실패·운영 판단 교차 확인 |
| RFC9457 | Problem Details for HTTP APIs | dependency injection은 외부 의존성을 밖에서 넣는다의 개념·실패·운영 판단 교차 확인 |

`dependency injection은 외부 의존성을 밖에서 넣는다` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
