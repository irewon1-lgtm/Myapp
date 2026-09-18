# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 07 · AI와 함께 백엔드를 만들되 사람이 계약과 검증을 통제하기

### LESSON 05 · 생성된 인증·권한 코드 검토

## CHAPTER 01 · 생성된 인증·권한 코드 검토 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 보안 코드는 문법보다 모든 route/resource에서 정책이 빠짐없이 적용되는지가 중요하다.

**아주 쉬운 사건.** auth middleware는 있지만 BOLA test가 없다. 이 사건에서는 먼저 **route coverage와 object permission을 확인한다**.

**왜 필요한가.** 정상 동작은 authn principal 생성과 object authorization을 분리해 route matrix로 전수 확인한다. 반대로 공통 middleware 하나가 있으니 세부 객체 권한도 자동 해결됐다고 생각한다.

**암기:** `생성된 인증·권한 코드 검토`의 역할 한 줄.

**직접 이해:** `생성된 인증·권한 코드 검토`의 입력·상태·결과 경계.

**AI 위임 가능:** `생성된 인증·권한 코드 검토` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `auth middleware는 있지만 BOLA test가 없다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 route-policy matrix, negative test, 다른 tenant/object id 테스트를 본다.

## CHAPTER 02 · 생성된 인증·권한 코드 검토 — 아주 쉬운 예를 한 단계씩 해석

T07-P95: `auth middleware는 있지만 BOLA test가 없다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | auth middleware는 있지만 BOLA test가 없다 | T07-P95 외부 입력 | T07-P95: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | route coverage와 object permission을 확인한다 | T07-P95 판단 기준 | T07-P95/CH08 관측표와 대조 |
| 정상 경로 | T07-P95/CH03 M1→M5 | 생성된 인증·권한 코드 검토: 완료 시점을 단계별로 분리 | T07-P95: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P95/CH06 공통 middleware 하나가 있으니 세부 객체 권한도 자동 해결됐다고 생각한다. | T07-P95: 깨진 계약 하나를 특정 | 생성된 인증·권한 코드 검토: 증상과 원인을 분리 |
| 재검증 | T07-P95/CH10 직접 실행 | T07-P95: 예상값 T07-P095 5 기록 | 생성된 인증·권한 코드 검토: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P95/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P95/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · 생성된 인증·권한 코드 검토 — 내부 메커니즘과 상태 전이

authn principal 생성과 object authorization을 분리해 route matrix로 전수 확인한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | auth middleware는 있지만 BOLA test가 없다 | source/actor/size를 보존 |
| M2 | 경계 판단 | route coverage와 object permission을 확인한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | authn principal 생성과 object authorization을 분리해 route matrix로 전수 확인한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | route-policy matrix, negative test, 다른 tenant/object id 테스트를 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | route 목록과 policy map을 비교해 미보호 route를 찾는다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`생성된 인증·권한 코드 검토` 흐름을 framework 이름 없이 설명한다.

막히면 `auth middleware는 있지만 BOLA test가 없다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · 생성된 인증·권한 코드 검토 — 실전 경계 A

**경계 A.** AI가 auth middleware를 추가했다는 사실만 보지 않고 모든 route가 실제로 그 chain을 타는지 route table·integration test로 전수 확인한다.

T07-P95/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P95에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P95/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P95/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P95): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P95/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P95-A1-68 | T07-P95 조건 | AI가 auth middleware를 추가했다는 사실만 보지 않고 모든 route가 실제로 그 chain을 타는지 route table·integration test로 전수 확인한다. |
| T07-P95-A2-69 | T07-P95 변화점 | T07-P95/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P95-A3-70 | T07-P95 반례 | T07-P95/CH06 대표 실패와 A 위반을 구별 |
| T07-P95-A4-71 | T07-P95 근거 | T07-P95/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P95-A5-72 | T07-P95 재실험 | auth middleware는 있지만 BOLA test가 없다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · 생성된 인증·권한 코드 검토 — 실전 경계 B

**경계 B.** user A credential로 user B resource id를 넣는 object-level negative test를 만들어 middleware를 통과한 뒤에도 resource authorization이 적용되는지 본다.

T07-P95/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P95에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P95/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P95/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P95): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P95/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P95-B1-99 | T07-P95 조건 | user A credential로 user B resource id를 넣는 object-level negative test를 만들어 middleware를 통과한 뒤에도 resource authorization이 적용되는지 본다. |
| T07-P95-B2-100 | T07-P95 독립성 | T07-P95/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P95-B3-101 | T07-P95 상태 | T07-P95/CH03 before·after 위치를 다시 지정 |
| T07-P95-B4-102 | T07-P95 반증 | T07-P95/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P95-B5-103 | T07-P95 적용 | 생성된 인증·권한 코드 검토의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · 생성된 인증·권한 코드 검토 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **공통 middleware 하나가 있으니 세부 객체 권한도 자동 해결됐다고 생각한다.**

아래 여섯 사례는 T07-P95의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P95-F01 | T07-P95: 대표 실패 | T07-P95: T07-P95/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P95: 현상만 보고 원인을 확정 | T07-P95/CH08 evidence map에서 상태를 대조 |
| T07-P95-F02 | T07-P95: 경계 A 누락 | T07-P95: T07-P95/CH04 경계 A 위반 입력 | T07-P95: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P95/CH08 evidence map에서 상태를 대조 |
| T07-P95-F03 | T07-P95: 경계 B 누락 | T07-P95: T07-P95/CH05 경계 B 위반 입력 | T07-P95: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P95/CH08 evidence map에서 상태를 대조 |
| T07-P95-F04 | T07-P95: 복구 경계 C 누락 | T07-P95: T07-P95/CH07 경계 C 복구 조건 | T07-P95: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P95/CH08 evidence map에서 상태를 대조 |
| T07-P95-F05 | T07-P95: 운영 경계 D 누락 | T07-P95: T07-P95/CH09 경계 D 운영 조건 | T07-P95: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P95/CH08 evidence map에서 상태를 대조 |
| T07-P95-F06 | T07-P95: 증거 없는 결론 | T07-P95: T07-P95/CH02 첫 판단만 존재 | T07-P95: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P95/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P95/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · 생성된 인증·권한 코드 검토 — 복구 가능한 상태와 수명

**경계 C.** secret가 config file·log·error body·test fixture에 평문으로 들어갔는지 검색하고 rotation 가능한 injection 경로인지 확인한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P95에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P95/CH08 evidence map을 본다. 복구 후에는 T07-P95/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P95에서 이미 확정된 side effect는 T07-P95/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P95-R1-161 | T07-P95 중단 직전 | T07-P95/CH03에서 이미 확정된 상태만 표시 |
| T07-P95-R2-162 | T07-P95 재시작 직후 | secret가 config file·log·error body·test fixture에 평문으로 들어갔는지 검색하고 rotation 가능한 injection 경로인지 확인한다. |
| T07-P95-R3-163 | T07-P95 재검증 | T07-P95/CH08 근거로 중복·누락 여부 확인 |
| T07-P95-R4-164 | T07-P95 재실행 | T07-P95/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · 생성된 인증·권한 코드 검토 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | auth middleware는 있지만 BOLA test가 없다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | route coverage와 object permission을 확인한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | authn principal 생성과 object authorization을 분리해 route matrix로 전수 확인한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 공통 middleware 하나가 있으니 세부 객체 권한도 자동 해결됐다고 생각한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | route-policy matrix, negative test, 다른 tenant/object id 테스트를 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`생성된 인증·권한 코드 검토` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · 생성된 인증·권한 코드 검토 — 운영 한계와 종료 조건

**경계 D.** session/token expiration·refresh·logout·permission revocation을 정상 login과 별개 scenario로 실행해 오래된 credential이 계속 권한을 갖지 않는지 검증한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P95/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P95/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P95 과제: 경계 D와 T07-P95/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P95-O1-223 | synthetic-load=83 | T07-P95/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P95-O2-224 | synthetic-budget=373ms | T07-P95 timeout과 unknown outcome을 분리 |
| T07-P95-O3-225 | T07-P95 종료 | session/token expiration·refresh·logout·permission revocation을 정상 login과 별개 scenario로 실행해 오래된 credential이 계속 권한을 갖지 않는지 검증한다. |
| T07-P95-O4-226 | T07-P95 완화 | T07-P95/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · 생성된 인증·권한 코드 검토 — 직접 실행하는 작은 모델

`생성된 인증·권한 코드 검토` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P095 5`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P095";
const current = 4;
const incoming = [3, 4, 5];
const next = incoming.reduce((state, v) => v > state ? v : state, current);
console.log(marker, next);
```

기준 출력: `T07-P095 5`.

`생성된 인증·권한 코드 검토`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P95-L1-255 | constmarker="T07-P095"; | T07-P95 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P95-L2-256 | constcurrent=4; | T07-P95 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P95-L3-257 | constincoming=[3,4,5]; | T07-P95 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P95-L4-258 | constnext=incoming.reduce((state,v)=>v>state?v:state,current); | T07-P95 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P95-L5-259 | console.log(marker,next); | T07-P95 출력 관측점; 예상 `T07-P095 5`와 비교 |
| T07-P95-LX-344 | T07-P95 실행 기록 | T07-P95 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · 생성된 인증·권한 코드 검토 — 한 부분만 수정하고 다시 예측

수정 과제: **incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다**.

수정 전은 `T07-P095 5`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P95/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P95-D1-285 | 기준 `T07-P095 5` | T07-P95 수정 전 실행을 먼저 재현 |
| T07-P95-D2-286 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | T07-P95 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P95-D3-287 | T07-P95 새 예측 | T07-P95 실행 전에 출력·상태를 먼저 기록 |
| T07-P95-D4-288 | T07-P95 재실행 | T07-P95/CH10 실제값과 새 예측을 대조 |
| T07-P95-D5-289 | T07-P95 반례 | T07-P95/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P95-D6-290 | T07-P95 근거 | T07-P95/CH08 상태가 설명과 일치해야 완료 |
| T07-P95-D7-291 | T07-P95 이유 | T07-P95 변경 이유를 생성된 인증·권한 코드 검토 계약과 연결해 설명 |

## CHAPTER 12 · 생성된 인증·권한 코드 검토 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: 생성된 인증·권한 코드 검토 | 보안 코드는 문법보다 모든 route/resource에서 정책이 빠짐없이 적용되는지가 중요하다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P95/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | authn principal 생성과 object authorization을 분리해 route matrix로 전수 확인한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 공통 middleware 하나가 있으니 세부 객체 권한도 자동 해결됐다고 생각한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | route-policy matrix, negative test, 다른 tenant/object id 테스트를 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P95/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P95/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P95/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P95/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `생성된 인증·권한 코드 검토` 실행 코드 수정 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | `생성된 인증·권한 코드 검토` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P95/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P95/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · 생성된 인증·권한 코드 검토 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 보안 코드는 문법보다 모든 route/resource에서 정책이 빠짐없이 적용되는지가 중요하다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P95/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P95/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P95/CH10 실행용 boilerplate·test 후보 | T07-P95: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P095 5` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P95/CH02의 판단 기준과 T07-P95/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P95-AI1-347 | T07-P95 사람 결정 | T07-P95 업무 의미·허용 위험·완료 기준 소유 |
| T07-P95-AI2-348 | T07-P95 AI 초안 | T07-P95/CH10 boilerplate·test 후보까지만 위임 |
| T07-P95-AI3-349 | T07-P95 검증 | T07-P95/CH06 반례와 T07-P95/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · 생성된 인증·권한 코드 검토 — 경계 조합 실험 8개

T07-P95: T07-P95/CH04~T07-P95/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P95의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-K01 | T07-P95: 경계 A | T07-P95: 경계 B | T07-P95: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=84 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + CLIENT_VERSION |
| T07-P95-K02 | T07-P95: 경계 A | T07-P95: 경계 C | T07-P95: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=92] / sample=91 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + STATE_VERSION |
| T07-P95-K03 | T07-P95: 경계 A | T07-P95: 경계 D | T07-P95: UNKNOWN_OUTCOME[timeout_ms=155; provider_state=UNKNOWN; lookup_id=p09503] / sample=98 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + PROVIDER_RESULT |
| T07-P95-K04 | T07-P95: 경계 B | T07-P95: 경계 C | T07-P95: OVERLOAD[rps=398; p99_ms=489; queue=15] / sample=16 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + QUEUE_PRESSURE |
| T07-P95-K05 | T07-P95: 경계 B | T07-P95: 경계 D | T07-P95: SAMPLING[sample_rate=44%; trace_present=1; metric_present=1] / sample=23 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P95-K06 | T07-P95: 경계 C | T07-P95: 경계 D | T07-P95: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] / sample=30 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + DIFF_EVAL_ROLLBACK |
| T07-P95-K07 | T07-P95: 경계 A | T07-P95: 경계 B+C | T07-P95: RECURRENCE[occurrence=4; interval_s=129; mitigation_applied=1] / sample=37 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + RECURRENCE_TIMELINE |
| T07-P95-K08 | T07-P95: 경계 B | T07-P95: 경계 C+D | T07-P95: LARGE_INPUT[body_kb=1392; limit_kb=256; parsed=0] / sample=44 | T07-P95: 먼저 깨지는 경계를 판정 | T07-P95: T07-P95/CH08 + SIZE_LIMIT |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P95/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · 생성된 인증·권한 코드 검토 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P95와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-B04 | T07-P95: 생성된 persistence 코드와 migration 경계 검토 | T07-P95: AI migration이 column을 바로 삭제한다 | T07-P95: expand-migrate-contract로 위험을 줄인다 | T07-P95: T07-P95/CH08 증거와 형제 LESSON 증거를 분리 | T07-P95: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P95-B06 | T07-P95: 생성된 retry·async 코드를 검토 | T07-P95: AI가 모든 오류에 retry를 붙였다 | T07-P95: idempotency와 deadline을 먼저 검토한다 | T07-P95: T07-P95/CH08 증거와 형제 LESSON 증거를 분리 | T07-P95: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P95-B03 | T07-P95: 생성된 endpoint의 계약 검토 | T07-P95: 생성 endpoint가 200만 반환한다 | T07-P95: status·error·idempotency 계약을 검토한다 | T07-P95: T07-P95/CH08 증거와 형제 LESSON 증거를 분리 | T07-P95: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P95-B07 | T07-P95: eval과 test contract로 AI 변경을 반복 검증하기 | T07-P95: 변경 20개가 eval 95%를 통과했다 | T07-P95: failure type별 gate를 따로 본다 | T07-P95: T07-P95/CH08 증거와 형제 LESSON 증거를 분리 | T07-P95: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P95-B02 | T07-P95: AI에게 구현을 맡기기 전 spec 쓰기 | T07-P95: 구현 요청에 “알아서 안전하게”만 적혀 있다 | T07-P95: spec의 빈 결정을 명시한다 | T07-P95: T07-P95/CH08 증거와 형제 LESSON 증거를 분리 | T07-P95: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · 생성된 인증·권한 코드 검토 — 선택형 실패 주입 6개

T07-P95: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P95 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-X01 | T07-P95: unknown outcome | T07-P95: dependency timeout 후 성공 여부 불명; sample=968 | T07-P95: 실패와 미확정 결과 | T07-P95: T07-P95/CH02 판단과 별도 기록 | T07-P95: provider id·조회 결과·retry history |
| T07-P95-X02 | T07-P95: 과부하 | T07-P95: traffic 세 배, p99 급증; sample=985 | T07-P95: 기능 실패와 saturation | T07-P95: T07-P95/CH02 판단과 별도 기록 | T07-P95: queue age·pool wait·CPU/event-loop·quota |
| T07-P95-X03 | T07-P95: sampling | T07-P95: 일부 log가 sampling으로 빠짐; sample=5 | T07-P95: 기록 부재와 사건 부재 | T07-P95: T07-P95/CH02 판단과 별도 기록 | T07-P95: metric·trace·durable state 교차 근거 |
| T07-P95-X04 | T07-P95: AI 복잡도 | T07-P95: AI가 cache/package를 추가함; sample=22 | T07-P95: 실제 이득과 failure surface | T07-P95: T07-P95/CH02 판단과 별도 기록 | T07-P95: diff·dependency tree·load/eval·rollback |
| T07-P95-X05 | T07-P95: 재발 | T07-P95: 같은 오류가 잠시 뒤 다시 발생; sample=39 | T07-P95: 완화와 원인 제거 | T07-P95: T07-P95/CH02 판단과 별도 기록 | T07-P95: 재발 timeline·변경점·resource state |
| T07-P95-X06 | T07-P95: 대형 입력 | T07-P95: 입력 크기가 정상의 100배; sample=56 | T07-P95: 의미 검증과 resource limit | T07-P95: T07-P95/CH02 판단과 별도 기록 | T07-P95: body/batch size·parse time·memory·reject status |

## CHAPTER 17 · 생성된 인증·권한 코드 검토 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P95에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-E01 | T07-P95: 대표 실패를 원인으로 착각 | T07-P95: T07-P95/CH06 실패 case를 다른 입력으로 재현 | T07-P95: 현상과 원인을 같은 것으로 봄 | T07-P95: T07-P95/CH06 대표 실패와 T07-P95/CH08 증거를 다시 대조 | T07-P95: T07-P95/CH08 |
| T07-P95-E02 | T07-P95: 경계 A 생략 | T07-P95: T07-P95/CH04의 조건 하나를 반대로 설정 | T07-P95: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P95: T07-P95/CH04를 새 입력에 적용 | T07-P95: T07-P95/CH08 |
| T07-P95-E03 | T07-P95: 경계 B 생략 | T07-P95: T07-P95/CH05의 조건 하나를 반대로 설정 | T07-P95: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P95: T07-P95/CH05를 새 입력에 적용 | T07-P95: T07-P95/CH08 |
| T07-P95-E04 | T07-P95: 복구 상태 혼동 | T07-P95: T07-P95/CH07에서 처리 중단을 주입 | T07-P95: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P95: T07-P95/CH07에서 수명 경계를 다시 표시 | T07-P95: T07-P95/CH08 |
| T07-P95-E05 | T07-P95: 운영 한계 누락 | T07-P95: T07-P95/CH09에서 부하 또는 drain 조건을 변경 | T07-P95: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P95: T07-P95/CH09의 종료 조건을 다시 작성 | T07-P95: T07-P95/CH08 |
| T07-P95-E06 | T07-P95: 증거 없는 성공 판정 | T07-P95: T07-P95/CH08에서 증거 하나를 숨김 | T07-P95: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P95: T07-P95/CH08에서 독립 증거 둘을 선택 | T07-P95: T07-P95/CH08 |

## CHAPTER 18 · 생성된 인증·권한 코드 검토 — synthetic 관측값 판독 6개

T07-P95: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P95의 숫자 하나만으로 원인을 단정하지 않고 T07-P95/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-O01 | T07-P95/changed_files | 131 | T07-P95: 변경 파일 수 | T07-P95: 축=재발; 원인 확정 금지 | T07-P95: RECURRENCE_TIMELINE + T07-P95/CH08 |
| T07-P95-O02 | T07-P95/dependency_count | 148 | T07-P95: 추가 dependency 수 | T07-P95: 축=대형 입력; 원인 확정 금지 | T07-P95: SIZE_LIMIT + T07-P95/CH08 |
| T07-P95-O03 | T07-P95/test_failures | 165 | T07-P95: 실패 test 수 | T07-P95: 축=소유권 위조; 원인 확정 금지 | T07-P95: AUTHZ_POLICY + T07-P95/CH08 |
| T07-P95-O04 | T07-P95/eval_passes | 182 | T07-P95: 통과 eval 수 | T07-P95: 축=순서 역전; 원인 확정 금지 | T07-P95: SEQUENCE_STATE + T07-P95/CH08 |
| T07-P95-O05 | T07-P95/p99_ms | 199 | T07-P95: 변경 후 p99 | T07-P95: 축=복구 범위; 원인 확정 금지 | T07-P95: RECOVERY_AUDIT + T07-P95/CH08 |
| T07-P95-O06 | T07-P95/cost_units | 216 | T07-P95: resource/cost 단위 | T07-P95: 축=재전송; 원인 확정 금지 | T07-P95: IDEMPOTENCY_RECORD + T07-P95/CH08 |

## CHAPTER 19 · 생성된 인증·권한 코드 검토 — 선택형 코드 리뷰 6질문

T07-P95: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P95에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-R01 | T07-P95: 관측 | T07-P95: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P95: LARGE_INPUT[body_kb=688; limit_kb=256; parsed=0] | T07-P95: T07-P95/CH04 | T07-P95: SIZE_LIMIT |
| T07-P95-R02 | T07-P95: 복구 | T07-P95: 재시작 뒤에도 필요한 상태가 남는가 | T07-P95: OWNER_SPOOF[actor=A4; payload_owner=B1; auth_owner=A4] | T07-P95: T07-P95/CH05 | T07-P95: AUTHZ_POLICY |
| T07-P95-R03 | T07-P95: 중복 | T07-P95: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P95: REORDER[in_seq=5,3,4; applied_version=6] | T07-P95: T07-P95/CH06 | T07-P95: SEQUENCE_STATE |
| T07-P95-R04 | T07-P95: timeout | T07-P95: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P95: RECOVERY_SCOPE[selected=75; expected=13; backup=1; dry_run=1] | T07-P95: T07-P95/CH07 | T07-P95: RECOVERY_AUDIT |
| T07-P95-R05 | T07-P95: 관측 | T07-P95: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P95: REPLAY[key=cmd-095-04; attempts=3; response_seen=0] | T07-P95: T07-P95/CH04 | T07-P95: IDEMPOTENCY_RECORD |
| T07-P95-R06 | T07-P95: 복구 | T07-P95: 재시작 뒤에도 필요한 상태가 남는가 | T07-P95: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P95: T07-P95/CH05 | T07-P95: CLIENT_VERSION |

## CHAPTER 20 · 생성된 인증·권한 코드 검토 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P95에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-I01 | T07-P95: 회귀 고정 | T07-P95: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P95: OWNER_SPOOF[actor=A4; payload_owner=B0; auth_owner=A4] | T07-P95: T07-P95/CH08 + AUTHZ_POLICY | T07-P95-incident-814 |
| T07-P95-I02 | T07-P95: 영향 범위 | T07-P95: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P95: REORDER[in_seq=4,2,3; applied_version=6] | T07-P95: T07-P95/CH08 + SEQUENCE_STATE | T07-P95-incident-815 |
| T07-P95-I03 | T07-P95: 변경 동결 | T07-P95: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P95: RECOVERY_SCOPE[selected=64; expected=21; backup=1; dry_run=0] | T07-P95: T07-P95/CH08 + RECOVERY_AUDIT | T07-P95-incident-816 |
| T07-P95-I04 | T07-P95: correlation | T07-P95: 한 request/job/resource id를 시간축에 고정 | T07-P95: REPLAY[key=cmd-095-03; attempts=2; response_seen=0] | T07-P95: T07-P95/CH08 + IDEMPOTENCY_RECORD | T07-P95-incident-817 |
| T07-P95-I05 | T07-P95: 마지막 정상 | T07-P95: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P95: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P95: T07-P95/CH08 + CLIENT_VERSION | T07-P95-incident-818 |
| T07-P95-I06 | T07-P95: 가설 검증 | T07-P95: 원인 후보 하나만 뒤집어 재현 | T07-P95: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=34] | T07-P95: T07-P95/CH08 + STATE_VERSION | T07-P95-incident-819 |

## CHAPTER 21 · 생성된 인증·권한 코드 검토 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P95/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P95/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P95/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P95/CH04~T07-P95/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P95/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P95/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P95/CH18 signal 두 개와 T07-P95/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P95/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P95/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · 생성된 인증·권한 코드 검토 — 통합 casebook 16문제

T07-P95 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P95 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P95-C01 | T07-P95: 경계 A | T07-P95: REORDER[in_seq=3,1,2; applied_version=6] | T07-P95: load=155, window=95s | T07-P95: review=권한 | T07-P95: 경계 A 위반 여부를 판정 | T07-P95: SEQUENCE_STATE + T07-P95/CH08 |
| T07-P95-C02 | T07-P95: 경계 B | T07-P95: RECOVERY_SCOPE[selected=53; expected=8; backup=1; dry_run=1] | T07-P95: load=178, window=24s | T07-P95: review=입력 경계 | T07-P95: 경계 B 위반 여부를 판정 | T07-P95: RECOVERY_AUDIT + T07-P95/CH08 |
| T07-P95-C03 | T07-P95: 경계 C | T07-P95: REPLAY[key=cmd-095-02; attempts=4; response_seen=0] | T07-P95: load=201, window=43s | T07-P95: review=timeout | T07-P95: 경계 C 위반 여부를 판정 | T07-P95: IDEMPOTENCY_RECORD + T07-P95/CH08 |
| T07-P95-C04 | T07-P95: 경계 D | T07-P95: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P95: load=224, window=62s | T07-P95: review=자원 | T07-P95: 경계 D 위반 여부를 판정 | T07-P95: CLIENT_VERSION + T07-P95/CH08 |
| T07-P95-C05 | T07-P95: 경계 A | T07-P95: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=21] | T07-P95: load=247, window=81s | T07-P95: review=순서 | T07-P95: 경계 A 위반 여부를 판정 | T07-P95: STATE_VERSION + T07-P95/CH08 |
| T07-P95-C06 | T07-P95: 경계 B | T07-P95: UNKNOWN_OUTCOME[timeout_ms=177; provider_state=UNKNOWN; lookup_id=p09505] | T07-P95: load=270, window=10s | T07-P95: review=관측 | T07-P95: 경계 B 위반 여부를 판정 | T07-P95: PROVIDER_RESULT + T07-P95/CH08 |
| T07-P95-C07 | T07-P95: 경계 C | T07-P95: OVERLOAD[rps=464; p99_ms=723; queue=23] | T07-P95: load=293, window=29s | T07-P95: review=상태 변경 | T07-P95: 경계 C 위반 여부를 판정 | T07-P95: QUEUE_PRESSURE + T07-P95/CH08 |
| T07-P95-C08 | T07-P95: 경계 D | T07-P95: SAMPLING[sample_rate=70%; trace_present=1; metric_present=1] | T07-P95: load=316, window=48s | T07-P95: review=retry | T07-P95: 경계 D 위반 여부를 판정 | T07-P95: TRACE_METRIC_CROSSCHECK + T07-P95/CH08 |
| T07-P95-C09 | T07-P95: 경계 A | T07-P95: AI_COMPLEXITY[deps_added=1; cache_layer=1; rollback_plan=0] | T07-P95: load=339, window=67s | T07-P95: review=복구 | T07-P95: 경계 A 위반 여부를 판정 | T07-P95: DIFF_EVAL_ROLLBACK + T07-P95/CH08 |
| T07-P95-C10 | T07-P95: 경계 B | T07-P95: RECURRENCE[occurrence=6; interval_s=151; mitigation_applied=1] | T07-P95: load=362, window=86s | T07-P95: review=동시성 | T07-P95: 경계 B 위반 여부를 판정 | T07-P95: RECURRENCE_TIMELINE + T07-P95/CH08 |
| T07-P95-C11 | T07-P95: 경계 C | T07-P95: LARGE_INPUT[body_kb=1568; limit_kb=256; parsed=0] | T07-P95: load=385, window=15s | T07-P95: review=민감정보 | T07-P95: 경계 C 위반 여부를 판정 | T07-P95: SIZE_LIMIT + T07-P95/CH08 |
| T07-P95-C12 | T07-P95: 경계 D | T07-P95: OWNER_SPOOF[actor=A4; payload_owner=B1; auth_owner=A4] | T07-P95: load=408, window=34s | T07-P95: review=중복 | T07-P95: 경계 D 위반 여부를 판정 | T07-P95: AUTHZ_POLICY + T07-P95/CH08 |
| T07-P95-C13 | T07-P95: 경계 A | T07-P95: REORDER[in_seq=15,13,14; applied_version=6] | T07-P95: load=431, window=53s | T07-P95: review=권한 | T07-P95: 경계 A 위반 여부를 판정 | T07-P95: SEQUENCE_STATE + T07-P95/CH08 |
| T07-P95-C14 | T07-P95: 경계 B | T07-P95: RECOVERY_SCOPE[selected=185; expected=8; backup=1; dry_run=1] | T07-P95: load=454, window=72s | T07-P95: review=입력 경계 | T07-P95: 경계 B 위반 여부를 판정 | T07-P95: RECOVERY_AUDIT + T07-P95/CH08 |
| T07-P95-C15 | T07-P95: 경계 C | T07-P95: REPLAY[key=cmd-095-14; attempts=4; response_seen=0] | T07-P95: load=477, window=91s | T07-P95: review=timeout | T07-P95: 경계 C 위반 여부를 판정 | T07-P95: IDEMPOTENCY_RECORD + T07-P95/CH08 |
| T07-P95-C16 | T07-P95: 경계 D | T07-P95: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P95: load=500, window=20s | T07-P95: review=자원 | T07-P95: 경계 D 위반 여부를 판정 | T07-P95: CLIENT_VERSION + T07-P95/CH08 |

채점은 결론보다 근거를 본다. T07-P95/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · 생성된 인증·권한 코드 검토 — evidence 판독 문제 14개

T07-P95 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P95 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P95-V01 | T07-P95: latency=315ms; queue=25; retry=4 | T07-P95: 복구 범위 | T07-P95: 축=복구 범위; 원인 확정은 보류 | T07-P95: RECOVERY_AUDIT + T07-P95/CH08 | T07-P95: 피할 오판=동시성 무시 |
| T07-P95-V02 | T07-P95: latency=382ms; queue=36; retry=0 | T07-P95: 재전송 | T07-P95: 축=재전송; 원인 확정은 보류 | T07-P95: IDEMPOTENCY_RECORD + T07-P95/CH08 | T07-P95: 피할 오판=운영 한계 누락 |
| T07-P95-V03 | T07-P95: latency=449ms; queue=47; retry=3 | T07-P95: 구버전 client | T07-P95: 축=구버전 client; 원인 확정은 보류 | T07-P95: CLIENT_VERSION + T07-P95/CH08 | T07-P95: 피할 오판=오류 합치기 |
| T07-P95-V04 | T07-P95: latency=516ms; queue=58; retry=6 | T07-P95: 동시 변경 | T07-P95: 축=동시 변경; 원인 확정은 보류 | T07-P95: STATE_VERSION + T07-P95/CH08 | T07-P95: 피할 오판=AI 과신 |
| T07-P95-V05 | T07-P95: latency=583ms; queue=69; retry=2 | T07-P95: unknown outcome | T07-P95: 축=unknown outcome; 원인 확정은 보류 | T07-P95: PROVIDER_RESULT + T07-P95/CH08 | T07-P95: 피할 오판=복구 과잉 |
| T07-P95-V06 | T07-P95: latency=650ms; queue=0; retry=5 | T07-P95: 과부하 | T07-P95: 축=과부하; 원인 확정은 보류 | T07-P95: QUEUE_PRESSURE + T07-P95/CH08 | T07-P95: 피할 오판=잘못된 전제 |
| T07-P95-V07 | T07-P95: latency=717ms; queue=11; retry=1 | T07-P95: sampling | T07-P95: 축=sampling; 원인 확정은 보류 | T07-P95: TRACE_METRIC_CROSSCHECK + T07-P95/CH08 | T07-P95: 피할 오판=경계 누락 |
| T07-P95-V08 | T07-P95: latency=784ms; queue=22; retry=4 | T07-P95: AI 복잡도 | T07-P95: 축=AI 복잡도; 원인 확정은 보류 | T07-P95: DIFF_EVAL_ROLLBACK + T07-P95/CH08 | T07-P95: 피할 오판=증거 혼동 |
| T07-P95-V09 | T07-P95: latency=851ms; queue=33; retry=0 | T07-P95: 재발 | T07-P95: 축=재발; 원인 확정은 보류 | T07-P95: RECURRENCE_TIMELINE + T07-P95/CH08 | T07-P95: 피할 오판=재시도 오판 |
| T07-P95-V10 | T07-P95: latency=918ms; queue=44; retry=3 | T07-P95: 대형 입력 | T07-P95: 축=대형 입력; 원인 확정은 보류 | T07-P95: SIZE_LIMIT + T07-P95/CH08 | T07-P95: 피할 오판=소유권 혼동 |
| T07-P95-V11 | T07-P95: latency=985ms; queue=55; retry=6 | T07-P95: 소유권 위조 | T07-P95: 축=소유권 위조; 원인 확정은 보류 | T07-P95: AUTHZ_POLICY + T07-P95/CH08 | T07-P95: 피할 오판=동시성 무시 |
| T07-P95-V12 | T07-P95: latency=1052ms; queue=66; retry=2 | T07-P95: 순서 역전 | T07-P95: 축=순서 역전; 원인 확정은 보류 | T07-P95: SEQUENCE_STATE + T07-P95/CH08 | T07-P95: 피할 오판=운영 한계 누락 |
| T07-P95-V13 | T07-P95: latency=1119ms; queue=77; retry=5 | T07-P95: 복구 범위 | T07-P95: 축=복구 범위; 원인 확정은 보류 | T07-P95: RECOVERY_AUDIT + T07-P95/CH08 | T07-P95: 피할 오판=오류 합치기 |
| T07-P95-V14 | T07-P95: latency=1186ms; queue=8; retry=1 | T07-P95: 재전송 | T07-P95: 축=재전송; 원인 확정은 보류 | T07-P95: IDEMPOTENCY_RECORD + T07-P95/CH08 | T07-P95: 피할 오판=AI 과신 |

## CHAPTER 24 · 생성된 인증·권한 코드 검토 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P95에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P95-D01 | T07-P95: 강제 종료 | T07-P95: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P95: in-flight request와 background work의 결과를 잃는지 | T07-P95: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P95: CLIENT_VERSION + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P95-D02 | T07-P95: AI package 추가 | T07-P95: AI가 제안한 새 dependency를 도입한다 | T07-P95: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P95: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=79] | T07-P95: STATE_VERSION + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P95-D03 | T07-P95: 비동기화 | T07-P95: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P95: durability·status API·worker retry 계약이 생기는지 | T07-P95: UNKNOWN_OUTCOME[timeout_ms=144; provider_state=UNKNOWN; lookup_id=p09502] | T07-P95: PROVIDER_RESULT + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P95-D04 | T07-P95: 권한 shortcut | T07-P95: payload의 owner/tenant id를 바로 사용한다 | T07-P95: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P95: OVERLOAD[rps=365; p99_ms=372; queue=21] | T07-P95: QUEUE_PRESSURE + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P95-D05 | T07-P95: 순서 병렬화 | T07-P95: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P95: 선후관계 invariant와 race를 깨지 않는지 | T07-P95: SAMPLING[sample_rate=31%; trace_present=0; metric_present=1] | T07-P95: TRACE_METRIC_CROSSCHECK + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P95-D06 | T07-P95: validation 이동 | T07-P95: validation을 business side effect 뒤로 옮긴다 | T07-P95: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P95: AI_COMPLEXITY[deps_added=2; cache_layer=1; rollback_plan=1] | T07-P95: DIFF_EVAL_ROLLBACK + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P95-D07 | T07-P95: batch 확대 | T07-P95: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P95: memory·deadline·부분 실패 범위가 커지는지 | T07-P95: RECURRENCE[occurrence=3; interval_s=118; mitigation_applied=1] | T07-P95: RECURRENCE_TIMELINE + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P95-D08 | T07-P95: fallback 추가 | T07-P95: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P95: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P95: LARGE_INPUT[body_kb=1304; limit_kb=256; parsed=0] | T07-P95: SIZE_LIMIT + T07-P95/CH08 | T07-P95: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P95: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · 생성된 인증·권한 코드 검토 — 최종 contract와 evidence spine

**최종 계약:** 보안 코드는 문법보다 모든 route/resource에서 정책이 빠짐없이 적용되는지가 중요하다.

**정상 메커니즘:** authn principal 생성과 object authorization을 분리해 route matrix로 전수 확인한다.

**대표 실패:** 공통 middleware 하나가 있으니 세부 객체 권한도 자동 해결됐다고 생각한다.

**검증 evidence:** route-policy matrix, negative test, 다른 tenant/object id 테스트를 본다.

**직접 행동:** route 목록과 policy map을 비교해 미보호 route를 찾는다.

**다음 연결:** `생성된 retry·async 코드를 검토`.

`생성된 인증·권한 코드 검토`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | 생성된 인증·권한 코드 검토의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | 생성된 인증·권한 코드 검토의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | 생성된 인증·권한 코드 검토의 개념·실패·운영 판단 교차 확인 |
| OWASP-API | OWASP-API | 생성된 인증·권한 코드 검토의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | 생성된 인증·권한 코드 검토의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | 생성된 인증·권한 코드 검토의 개념·실패·운영 판단 교차 확인 |

`생성된 인증·권한 코드 검토` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
