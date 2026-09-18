# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 03 · 인증·권한·신뢰 경계를 백엔드 흐름에 넣기

### LESSON 14 · backend threat modeling을 요청 흐름에 붙이기

## CHAPTER 01 · backend threat modeling을 요청 흐름에 붙이기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 위협 모델링은 공격 목록 암기가 아니라 데이터가 신뢰 경계를 지나는 지점을 찾아 잘못된 가정을 검토하는 과정이다.

**아주 쉬운 사건.** 계정 변경 API의 data flow를 그린다. 이 사건에서는 먼저 **asset·entry point·trust boundary를 표시한다**.

**왜 필요한가.** 정상 동작은 entry point, asset, actor, trust boundary, privilege, side effect를 diagram과 표로 연결한다. 반대로 시스템 전체를 한 박스로 그려 경계가 사라지거나 보안 장치를 나열하고 실제 자산과 연결하지 않는다.

**암기:** `backend threat modeling을 요청 흐름에 붙이기`의 역할 한 줄.

**직접 이해:** `backend threat modeling을 요청 흐름에 붙이기`의 입력·상태·결과 경계.

**AI 위임 가능:** `backend threat modeling을 요청 흐름에 붙이기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `계정 변경 API의 data flow를 그린다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 각 경계의 input, authentication/authorization, validation, logging, failure handling을 확인한다.

## CHAPTER 02 · backend threat modeling을 요청 흐름에 붙이기 — 아주 쉬운 예를 한 단계씩 해석

T07-P44: `계정 변경 API의 data flow를 그린다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 계정 변경 API의 data flow를 그린다 | T07-P44 외부 입력 | T07-P44: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | asset·entry point·trust boundary를 표시한다 | T07-P44 판단 기준 | T07-P44/CH08 관측표와 대조 |
| 정상 경로 | T07-P44/CH03 M1→M5 | backend threat modeling을 요청 흐름에 붙이기: 완료 시점을 단계별로 분리 | T07-P44: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P44/CH06 시스템 전체를 한 박스로 그려 경계가 사라지거나 보안 장치를 나열하고 실제 자산과 연결하지 않는다. | T07-P44: 깨진 계약 하나를 특정 | backend threat modeling을 요청 흐름에 붙이기: 증상과 원인을 분리 |
| 재검증 | T07-P44/CH10 직접 실행 | T07-P44: 예상값 T07-P044 4/4 기록 | backend threat modeling을 요청 흐름에 붙이기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P44/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P44/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · backend threat modeling을 요청 흐름에 붙이기 — 내부 메커니즘과 상태 전이

entry point, asset, actor, trust boundary, privilege, side effect를 diagram과 표로 연결한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 계정 변경 API의 data flow를 그린다 | source/actor/size를 보존 |
| M2 | 경계 판단 | asset·entry point·trust boundary를 표시한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | entry point, asset, actor, trust boundary, privilege, side effect를 diagram과 표로 연결한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 각 경계의 input, authentication/authorization, validation, logging, failure handling을 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 간단한 flow 노드 목록에서 외부→내부 경계를 자동 표시한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`backend threat modeling을 요청 흐름에 붙이기` 흐름을 framework 이름 없이 설명한다.

막히면 `계정 변경 API의 data flow를 그린다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · backend threat modeling을 요청 흐름에 붙이기 — 실전 경계 A

**경계 A.** threat modeling은 공격 이름을 외우는 작업보다 asset·entry point·trust boundary·privileged action을 데이터 흐름 그림에 표시하는 것부터 시작한다.

T07-P44/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P44에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P44/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P44/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P44): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P44/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P44-A1-883 | T07-P44 조건 | threat modeling은 공격 이름을 외우는 작업보다 asset·entry point·trust boundary·privileged action을 데이터 흐름 그림에 표시하는 것부터 시작한다. |
| T07-P44-A2-884 | T07-P44 변화점 | T07-P44/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P44-A3-885 | T07-P44 반례 | T07-P44/CH06 대표 실패와 A 위반을 구별 |
| T07-P44-A4-886 | T07-P44 근거 | T07-P44/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P44-A5-887 | T07-P44 재실험 | 계정 변경 API의 data flow를 그린다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · backend threat modeling을 요청 흐름에 붙이기 — 실전 경계 B

**경계 B.** 각 boundary에서 spoofing·tampering·information disclosure·resource abuse 같은 abuse case를 질문해 실제 control이 어느 코드/infra에 있는지 연결한다.

T07-P44/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P44에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P44/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P44/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P44): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P44/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P44-B1-914 | T07-P44 조건 | 각 boundary에서 spoofing·tampering·information disclosure·resource abuse 같은 abuse case를 질문해 실제 control이 어느 코드/infra에 있는지 연결한다. |
| T07-P44-B2-915 | T07-P44 독립성 | T07-P44/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P44-B3-916 | T07-P44 상태 | T07-P44/CH03 before·after 위치를 다시 지정 |
| T07-P44-B4-917 | T07-P44 반증 | T07-P44/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P44-B5-918 | T07-P44 적용 | backend threat modeling을 요청 흐름에 붙이기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · backend threat modeling을 요청 흐름에 붙이기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **시스템 전체를 한 박스로 그려 경계가 사라지거나 보안 장치를 나열하고 실제 자산과 연결하지 않는다.**

아래 여섯 사례는 T07-P44의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P44-F01 | T07-P44: 대표 실패 | T07-P44: T07-P44/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P44: 현상만 보고 원인을 확정 | T07-P44/CH08 evidence map에서 상태를 대조 |
| T07-P44-F02 | T07-P44: 경계 A 누락 | T07-P44: T07-P44/CH04 경계 A 위반 입력 | T07-P44: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P44/CH08 evidence map에서 상태를 대조 |
| T07-P44-F03 | T07-P44: 경계 B 누락 | T07-P44: T07-P44/CH05 경계 B 위반 입력 | T07-P44: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P44/CH08 evidence map에서 상태를 대조 |
| T07-P44-F04 | T07-P44: 복구 경계 C 누락 | T07-P44: T07-P44/CH07 경계 C 복구 조건 | T07-P44: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P44/CH08 evidence map에서 상태를 대조 |
| T07-P44-F05 | T07-P44: 운영 경계 D 누락 | T07-P44: T07-P44/CH09 경계 D 운영 조건 | T07-P44: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P44/CH08 evidence map에서 상태를 대조 |
| T07-P44-F06 | T07-P44: 증거 없는 결론 | T07-P44: T07-P44/CH02 첫 판단만 존재 | T07-P44: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P44/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P44/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · backend threat modeling을 요청 흐름에 붙이기 — 복구 가능한 상태와 수명

**경계 C.** ‘인증 middleware 있음’ 같은 추상 통제보다 cross-tenant id를 넣었을 때 거부되는 test처럼 검증 가능한 control evidence를 남긴다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P44에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P44/CH08 evidence map을 본다. 복구 후에는 T07-P44/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P44에서 이미 확정된 side effect는 T07-P44/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P44-R1-976 | T07-P44 중단 직전 | T07-P44/CH03에서 이미 확정된 상태만 표시 |
| T07-P44-R2-977 | T07-P44 재시작 직후 | ‘인증 middleware 있음’ 같은 추상 통제보다 cross-tenant id를 넣었을 때 거부되는 test처럼 검증 가능한 control evidence를 남긴다. |
| T07-P44-R3-978 | T07-P44 재검증 | T07-P44/CH08 근거로 중복·누락 여부 확인 |
| T07-P44-R4-979 | T07-P44 재실행 | T07-P44/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · backend threat modeling을 요청 흐름에 붙이기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 계정 변경 API의 data flow를 그린다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | asset·entry point·trust boundary를 표시한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | entry point, asset, actor, trust boundary, privilege, side effect를 diagram과 표로 연결한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 시스템 전체를 한 박스로 그려 경계가 사라지거나 보안 장치를 나열하고 실제 자산과 연결하지 않는다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 각 경계의 input, authentication/authorization, validation, logging, failure handling을 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`backend threat modeling을 요청 흐름에 붙이기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · backend threat modeling을 요청 흐름에 붙이기 — 운영 한계와 종료 조건

**경계 D.** 모든 risk를 0으로 만들 수 없으므로 residual risk·owner·monitoring·review date를 남겨 설계 당시 판단이 시간이 지나도 추적되게 한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P44/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P44/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P44 과제: 경계 D와 T07-P44/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P44-O1-41 | synthetic-load=81 | T07-P44/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P44-O2-42 | synthetic-budget=191ms | T07-P44 timeout과 unknown outcome을 분리 |
| T07-P44-O3-43 | T07-P44 종료 | 모든 risk를 0으로 만들 수 없으므로 residual risk·owner·monitoring·review date를 남겨 설계 당시 판단이 시간이 지나도 추적되게 한다. |
| T07-P44-O4-44 | T07-P44 완화 | T07-P44/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · backend threat modeling을 요청 흐름에 붙이기 — 직접 실행하는 작은 모델

`backend threat modeling을 요청 흐름에 붙이기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P044 4/4`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P044";
const checks = [true,true,true,true];
const passed = checks.filter(Boolean).length;
console.log(marker, `${passed}/${checks.length}`);
```

기준 출력: `T07-P044 4/4`.

`backend threat modeling을 요청 흐름에 붙이기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P44-L1-73 | constmarker="T07-P044"; | T07-P44 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P44-L2-74 | constchecks=[true,true,true,true]; | T07-P44 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P44-L3-75 | constpassed=checks.filter(Boolean).length; | T07-P44 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P44-L4-76 | console.log(marker,`${passed}/${checks.length}`); | T07-P44 출력 관측점; 예상 `T07-P044 4/4`와 비교 |
| T07-P44-LX-162 | T07-P44 실행 기록 | T07-P44 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · backend threat modeling을 요청 흐름에 붙이기 — 한 부분만 수정하고 다시 예측

수정 과제: **false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다**.

수정 전은 `T07-P044 4/4`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P44/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P44-D1-103 | 기준 `T07-P044 4/4` | T07-P44 수정 전 실행을 먼저 재현 |
| T07-P44-D2-104 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | T07-P44 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P44-D3-105 | T07-P44 새 예측 | T07-P44 실행 전에 출력·상태를 먼저 기록 |
| T07-P44-D4-106 | T07-P44 재실행 | T07-P44/CH10 실제값과 새 예측을 대조 |
| T07-P44-D5-107 | T07-P44 반례 | T07-P44/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P44-D6-108 | T07-P44 근거 | T07-P44/CH08 상태가 설명과 일치해야 완료 |
| T07-P44-D7-109 | T07-P44 이유 | T07-P44 변경 이유를 backend threat modeling을 요청 흐름에 붙이기 계약과 연결해 설명 |

## CHAPTER 12 · backend threat modeling을 요청 흐름에 붙이기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: backend threat modeling을 요청 흐름에 붙이기 | 위협 모델링은 공격 목록 암기가 아니라 데이터가 신뢰 경계를 지나는 지점을 찾아 잘못된 가정을 검토하는 과정이다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P44/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | entry point, asset, actor, trust boundary, privilege, side effect를 diagram과 표로 연결한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 시스템 전체를 한 박스로 그려 경계가 사라지거나 보안 장치를 나열하고 실제 자산과 연결하지 않는다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 각 경계의 input, authentication/authorization, validation, logging, failure handling을 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P44/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P44/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P44/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P44/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `backend threat modeling을 요청 흐름에 붙이기` 실행 코드 수정 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | `backend threat modeling을 요청 흐름에 붙이기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P44/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P44/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · backend threat modeling을 요청 흐름에 붙이기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 위협 모델링은 공격 목록 암기가 아니라 데이터가 신뢰 경계를 지나는 지점을 찾아 잘못된 가정을 검토하는 과정이다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P44/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P44/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P44/CH10 실행용 boilerplate·test 후보 | T07-P44: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P044 4/4` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P44/CH02의 판단 기준과 T07-P44/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P44-AI1-165 | T07-P44 사람 결정 | T07-P44 업무 의미·허용 위험·완료 기준 소유 |
| T07-P44-AI2-166 | T07-P44 AI 초안 | T07-P44/CH10 boilerplate·test 후보까지만 위임 |
| T07-P44-AI3-167 | T07-P44 검증 | T07-P44/CH06 반례와 T07-P44/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · backend threat modeling을 요청 흐름에 붙이기 — 경계 조합 실험 8개

T07-P44: T07-P44/CH04~T07-P44/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P44의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-K01 | T07-P44: 경계 A | T07-P44: 경계 B | T07-P44: SAMPLING[sample_rate=15%; trace_present=1; metric_present=1] / sample=57 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P44-K02 | T07-P44: 경계 A | T07-P44: 경계 C | T07-P44: DISCONNECT[disconnect_ms=118; commit_state=UNKNOWN; request=044-02] / sample=64 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + COMMIT_TIMELINE |
| T07-P44-K03 | T07-P44: 경계 A | T07-P44: 경계 D | T07-P44: RECURRENCE[occurrence=5; interval_s=83; mitigation_applied=1] / sample=71 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + RECURRENCE_TIMELINE |
| T07-P44-K04 | T07-P44: 경계 B | T07-P44: 경계 C | T07-P44: LARGE_INPUT[body_kb=1024; limit_kb=768; parsed=0] / sample=78 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + SIZE_LIMIT |
| T07-P44-K05 | T07-P44: 경계 B | T07-P44: 경계 D | T07-P44: OWNER_SPOOF[actor=A2; payload_owner=B0; auth_owner=A2] / sample=85 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + AUTHZ_POLICY |
| T07-P44-K06 | T07-P44: 경계 C | T07-P44: 경계 D | T07-P44: REORDER[in_seq=9,7,8; applied_version=3] / sample=92 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + SEQUENCE_STATE |
| T07-P44-K07 | T07-P44: 경계 A | T07-P44: 경계 B+C | T07-P44: RECOVERY_SCOPE[selected=117; expected=14; backup=1; dry_run=1] / sample=99 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + RECOVERY_AUDIT |
| T07-P44-K08 | T07-P44: 경계 B | T07-P44: 경계 C+D | T07-P44: REPLAY[key=cmd-044-08; attempts=4; response_seen=0] / sample=17 | T07-P44: 먼저 깨지는 경계를 판정 | T07-P44: T07-P44/CH08 + IDEMPOTENCY_RECORD |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P44/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · backend threat modeling을 요청 흐름에 붙이기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P44와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-B13 | T07-P44: 외부 응답도 신뢰하지 않고 validation하기 | T07-P44: trusted provider가 새 enum과 긴 배열을 보낸다 | T07-P44: 외부 응답도 schema와 size를 검증한다 | T07-P44: T07-P44/CH08 증거와 형제 LESSON 증거를 분리 | T07-P44: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P44-B15 | T07-P44: BLOCK 03 종합: 계정 API의 신뢰 경계 감사 | T07-P44: 로그인부터 audit까지 한 요청을 따라간다 | T07-P44: tenant와 object permission을 끝까지 유지한다 | T07-P44: T07-P44/CH08 증거와 형제 LESSON 증거를 분리 | T07-P44: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P44-B12 | T07-P44: file upload를 데이터와 실행물 사이 경계로 보기 | T07-P44: jpg 확장자 파일이 거대한 압축 payload다 | T07-P44: 형식·크기·격리 상태를 분리한다 | T07-P44: T07-P44/CH08 증거와 형제 LESSON 증거를 분리 | T07-P44: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P44-B11 | T07-P44: URL fetch와 SSRF trust boundary | T07-P44: 사용자가 입력한 URL을 server가 fetch한다 | T07-P44: scheme·host·redirect·resolved IP를 검증한다 | T07-P44: T07-P44/CH08 증거와 형제 LESSON 증거를 분리 | T07-P44: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P44-B10 | T07-P44: login abuse와 rate limit | T07-P44: 같은 계정으로 100개 IP가 로그인 시도한다 | T07-P44: rate signal과 lockout DoS를 함께 본다 | T07-P44: T07-P44/CH08 증거와 형제 LESSON 증거를 분리 | T07-P44: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · backend threat modeling을 요청 흐름에 붙이기 — 선택형 실패 주입 6개

T07-P44: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P44 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-X01 | T07-P44: 재발 | T07-P44: 같은 오류가 잠시 뒤 다시 발생; sample=384 | T07-P44: 완화와 원인 제거 | T07-P44: T07-P44/CH02 판단과 별도 기록 | T07-P44: 재발 timeline·변경점·resource state |
| T07-P44-X02 | T07-P44: 대형 입력 | T07-P44: 입력 크기가 정상의 100배; sample=401 | T07-P44: 의미 검증과 resource limit | T07-P44: T07-P44/CH02 판단과 별도 기록 | T07-P44: body/batch size·parse time·memory·reject status |
| T07-P44-X03 | T07-P44: 소유권 위조 | T07-P44: tenant/owner가 payload에 포함됨; sample=418 | T07-P44: 식별 정보와 권한 근거 | T07-P44: T07-P44/CH02 판단과 별도 기록 | T07-P44: authenticated context·resource owner·policy result |
| T07-P44-X04 | T07-P44: 순서 역전 | T07-P44: event가 원래 순서와 반대로 도착; sample=435 | T07-P44: 수신 순서와 업무 순서 | T07-P44: T07-P44/CH02 판단과 별도 기록 | T07-P44: version/sequence·dedupe id·applied state |
| T07-P44-X05 | T07-P44: 복구 범위 | T07-P44: 복구 script 대상이 예상보다 큼; sample=452 | T07-P44: 진단과 destructive recovery | T07-P44: T07-P44/CH02 판단과 별도 기록 | T07-P44: selected ids/count·backup·audit trail |
| T07-P44-X06 | T07-P44: 재전송 | T07-P44: 응답 유실 뒤 같은 command가 다시 도착함; sample=469 | T07-P44: 중복 side effect 여부 | T07-P44: T07-P44/CH02 판단과 별도 기록 | T07-P44: 처리 식별자·이전 결과·idempotency 기록 |

## CHAPTER 17 · backend threat modeling을 요청 흐름에 붙이기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P44에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-E01 | T07-P44: 대표 실패를 원인으로 착각 | T07-P44: T07-P44/CH06 실패 case를 다른 입력으로 재현 | T07-P44: 현상과 원인을 같은 것으로 봄 | T07-P44: T07-P44/CH06 대표 실패와 T07-P44/CH08 증거를 다시 대조 | T07-P44: T07-P44/CH08 |
| T07-P44-E02 | T07-P44: 경계 A 생략 | T07-P44: T07-P44/CH04의 조건 하나를 반대로 설정 | T07-P44: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P44: T07-P44/CH04를 새 입력에 적용 | T07-P44: T07-P44/CH08 |
| T07-P44-E03 | T07-P44: 경계 B 생략 | T07-P44: T07-P44/CH05의 조건 하나를 반대로 설정 | T07-P44: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P44: T07-P44/CH05를 새 입력에 적용 | T07-P44: T07-P44/CH08 |
| T07-P44-E04 | T07-P44: 복구 상태 혼동 | T07-P44: T07-P44/CH07에서 처리 중단을 주입 | T07-P44: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P44: T07-P44/CH07에서 수명 경계를 다시 표시 | T07-P44: T07-P44/CH08 |
| T07-P44-E05 | T07-P44: 운영 한계 누락 | T07-P44: T07-P44/CH09에서 부하 또는 drain 조건을 변경 | T07-P44: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P44: T07-P44/CH09의 종료 조건을 다시 작성 | T07-P44: T07-P44/CH08 |
| T07-P44-E06 | T07-P44: 증거 없는 성공 판정 | T07-P44: T07-P44/CH08에서 증거 하나를 숨김 | T07-P44: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P44: T07-P44/CH08에서 독립 증거 둘을 선택 | T07-P44: T07-P44/CH08 |

## CHAPTER 18 · backend threat modeling을 요청 흐름에 붙이기 — synthetic 관측값 판독 6개

T07-P44: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P44의 숫자 하나만으로 원인을 단정하지 않고 T07-P44/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-O01 | T07-P44/auth_failures | 780 | T07-P44: 인증 실패 수 | T07-P44: 축=복구 범위; 원인 확정 금지 | T07-P44: RECOVERY_AUDIT + T07-P44/CH08 |
| T07-P44-O02 | T07-P44/policy_denials | 797 | T07-P44: 권한 거부 수 | T07-P44: 축=재전송; 원인 확정 금지 | T07-P44: IDEMPOTENCY_RECORD + T07-P44/CH08 |
| T07-P44-O03 | T07-P44/validation_rejects | 814 | T07-P44: 입력 거부 수 | T07-P44: 축=구버전 client; 원인 확정 금지 | T07-P44: CLIENT_VERSION + T07-P44/CH08 |
| T07-P44-O04 | T07-P44/rate_limited | 831 | T07-P44: rate-limit 적용 수 | T07-P44: 축=unknown outcome; 원인 확정 금지 | T07-P44: PROVIDER_RESULT + T07-P44/CH08 |
| T07-P44-O05 | T07-P44/redactions | 848 | T07-P44: 민감정보 마스킹 수 | T07-P44: 축=과부하; 원인 확정 금지 | T07-P44: QUEUE_PRESSURE + T07-P44/CH08 |
| T07-P44-O06 | T07-P44/suspicious_requests | 865 | T07-P44: 의심 요청 수 | T07-P44: 축=sampling; 원인 확정 금지 | T07-P44: TRACE_METRIC_CROSSCHECK + T07-P44/CH08 |

## CHAPTER 19 · backend threat modeling을 요청 흐름에 붙이기 — 선택형 코드 리뷰 6질문

T07-P44: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P44에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-R01 | T07-P44: 관측 | T07-P44: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P44: REPLAY[key=cmd-044-00; attempts=2; response_seen=0] | T07-P44: T07-P44/CH04 | T07-P44: IDEMPOTENCY_RECORD |
| T07-P44-R02 | T07-P44: 복구 | T07-P44: 재시작 뒤에도 필요한 상태가 남는가 | T07-P44: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P44: T07-P44/CH05 | T07-P44: CLIENT_VERSION |
| T07-P44-R03 | T07-P44: 중복 | T07-P44: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P44: UNKNOWN_OUTCOME[timeout_ms=142; provider_state=UNKNOWN; lookup_id=p04402] | T07-P44: T07-P44/CH06 | T07-P44: PROVIDER_RESULT |
| T07-P44-R04 | T07-P44: timeout | T07-P44: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P44: OVERLOAD[rps=359; p99_ms=426; queue=27] | T07-P44: T07-P44/CH07 | T07-P44: QUEUE_PRESSURE |
| T07-P44-R05 | T07-P44: 관측 | T07-P44: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P44: SAMPLING[sample_rate=37%; trace_present=0; metric_present=1] | T07-P44: T07-P44/CH04 | T07-P44: TRACE_METRIC_CROSSCHECK |
| T07-P44-R06 | T07-P44: 복구 | T07-P44: 재시작 뒤에도 필요한 상태가 남는가 | T07-P44: DISCONNECT[disconnect_ms=60; commit_state=UNKNOWN; request=044-05] | T07-P44: T07-P44/CH05 | T07-P44: COMMIT_TIMELINE |

## CHAPTER 20 · backend threat modeling을 요청 흐름에 붙이기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P44에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-I01 | T07-P44: 가설 검증 | T07-P44: 원인 후보 하나만 뒤집어 재현 | T07-P44: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P44: T07-P44/CH08 + CLIENT_VERSION | T07-P44-incident-836 |
| T07-P44-I02 | T07-P44: 복구 확인 | T07-P44: durable state와 사용자 결과를 모두 확인 | T07-P44: UNKNOWN_OUTCOME[timeout_ms=131; provider_state=UNKNOWN; lookup_id=p04401] | T07-P44: T07-P44/CH08 + PROVIDER_RESULT | T07-P44-incident-837 |
| T07-P44-I03 | T07-P44: 재주입 | T07-P44: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P44: OVERLOAD[rps=326; p99_ms=1182; queue=30] | T07-P44: T07-P44/CH08 + QUEUE_PRESSURE | T07-P44-incident-838 |
| T07-P44-I04 | T07-P44: 회귀 고정 | T07-P44: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P44: SAMPLING[sample_rate=24%; trace_present=1; metric_present=1] | T07-P44: T07-P44/CH08 + TRACE_METRIC_CROSSCHECK | T07-P44-incident-839 |
| T07-P44-I05 | T07-P44: 영향 범위 | T07-P44: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P44: DISCONNECT[disconnect_ms=47; commit_state=UNKNOWN; request=044-04] | T07-P44: T07-P44/CH08 + COMMIT_TIMELINE | T07-P44-incident-840 |
| T07-P44-I06 | T07-P44: 변경 동결 | T07-P44: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P44: RECURRENCE[occurrence=2; interval_s=105; mitigation_applied=1] | T07-P44: T07-P44/CH08 + RECURRENCE_TIMELINE | T07-P44-incident-841 |

## CHAPTER 21 · backend threat modeling을 요청 흐름에 붙이기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P44/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P44/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P44/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P44/CH04~T07-P44/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P44/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P44/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P44/CH18 signal 두 개와 T07-P44/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P44/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P44/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · backend threat modeling을 요청 흐름에 붙이기 — 통합 casebook 16문제

T07-P44 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P44 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P44-C01 | T07-P44: 경계 A | T07-P44: UNKNOWN_OUTCOME[timeout_ms=120; provider_state=UNKNOWN; lookup_id=p04400] | T07-P44: load=476, window=38s | T07-P44: review=동시성 | T07-P44: 경계 A 위반 여부를 판정 | T07-P44: PROVIDER_RESULT + T07-P44/CH08 |
| T07-P44-C02 | T07-P44: 경계 B | T07-P44: OVERLOAD[rps=293; p99_ms=1065; queue=16] | T07-P44: load=499, window=57s | T07-P44: review=민감정보 | T07-P44: 경계 B 위반 여부를 판정 | T07-P44: QUEUE_PRESSURE + T07-P44/CH08 |
| T07-P44-C03 | T07-P44: 경계 C | T07-P44: SAMPLING[sample_rate=28%; trace_present=0; metric_present=1] | T07-P44: load=522, window=76s | T07-P44: review=중복 | T07-P44: 경계 C 위반 여부를 판정 | T07-P44: TRACE_METRIC_CROSSCHECK + T07-P44/CH08 |
| T07-P44-C04 | T07-P44: 경계 D | T07-P44: DISCONNECT[disconnect_ms=34; commit_state=UNKNOWN; request=044-03] | T07-P44: load=545, window=95s | T07-P44: review=권한 | T07-P44: 경계 D 위반 여부를 판정 | T07-P44: COMMIT_TIMELINE + T07-P44/CH08 |
| T07-P44-C05 | T07-P44: 경계 A | T07-P44: RECURRENCE[occurrence=6; interval_s=94; mitigation_applied=1] | T07-P44: load=568, window=24s | T07-P44: review=입력 경계 | T07-P44: 경계 A 위반 여부를 판정 | T07-P44: RECURRENCE_TIMELINE + T07-P44/CH08 |
| T07-P44-C06 | T07-P44: 경계 B | T07-P44: LARGE_INPUT[body_kb=1112; limit_kb=768; parsed=0] | T07-P44: load=591, window=43s | T07-P44: review=timeout | T07-P44: 경계 B 위반 여부를 판정 | T07-P44: SIZE_LIMIT + T07-P44/CH08 |
| T07-P44-C07 | T07-P44: 경계 C | T07-P44: OWNER_SPOOF[actor=A2; payload_owner=B1; auth_owner=A2] | T07-P44: load=614, window=62s | T07-P44: review=자원 | T07-P44: 경계 C 위반 여부를 판정 | T07-P44: AUTHZ_POLICY + T07-P44/CH08 |
| T07-P44-C08 | T07-P44: 경계 D | T07-P44: REORDER[in_seq=10,8,9; applied_version=3] | T07-P44: load=637, window=81s | T07-P44: review=순서 | T07-P44: 경계 D 위반 여부를 판정 | T07-P44: SEQUENCE_STATE + T07-P44/CH08 |
| T07-P44-C09 | T07-P44: 경계 A | T07-P44: RECOVERY_SCOPE[selected=128; expected=8; backup=1; dry_run=0] | T07-P44: load=660, window=10s | T07-P44: review=관측 | T07-P44: 경계 A 위반 여부를 판정 | T07-P44: RECOVERY_AUDIT + T07-P44/CH08 |
| T07-P44-C10 | T07-P44: 경계 B | T07-P44: REPLAY[key=cmd-044-09; attempts=2; response_seen=0] | T07-P44: load=683, window=29s | T07-P44: review=상태 변경 | T07-P44: 경계 B 위반 여부를 판정 | T07-P44: IDEMPOTENCY_RECORD + T07-P44/CH08 |
| T07-P44-C11 | T07-P44: 경계 C | T07-P44: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P44: load=706, window=48s | T07-P44: review=retry | T07-P44: 경계 C 위반 여부를 판정 | T07-P44: CLIENT_VERSION + T07-P44/CH08 |
| T07-P44-C12 | T07-P44: 경계 D | T07-P44: UNKNOWN_OUTCOME[timeout_ms=241; provider_state=UNKNOWN; lookup_id=p04411] | T07-P44: load=729, window=67s | T07-P44: review=복구 | T07-P44: 경계 D 위반 여부를 판정 | T07-P44: PROVIDER_RESULT + T07-P44/CH08 |
| T07-P44-C13 | T07-P44: 경계 A | T07-P44: OVERLOAD[rps=656; p99_ms=606; queue=36] | T07-P44: load=752, window=86s | T07-P44: review=동시성 | T07-P44: 경계 A 위반 여부를 판정 | T07-P44: QUEUE_PRESSURE + T07-P44/CH08 |
| T07-P44-C14 | T07-P44: 경계 B | T07-P44: SAMPLING[sample_rate=57%; trace_present=1; metric_present=1] | T07-P44: load=775, window=15s | T07-P44: review=민감정보 | T07-P44: 경계 B 위반 여부를 판정 | T07-P44: TRACE_METRIC_CROSSCHECK + T07-P44/CH08 |
| T07-P44-C15 | T07-P44: 경계 C | T07-P44: DISCONNECT[disconnect_ms=80; commit_state=UNKNOWN; request=044-14] | T07-P44: load=798, window=34s | T07-P44: review=중복 | T07-P44: 경계 C 위반 여부를 판정 | T07-P44: COMMIT_TIMELINE + T07-P44/CH08 |
| T07-P44-C16 | T07-P44: 경계 D | T07-P44: RECURRENCE[occurrence=2; interval_s=215; mitigation_applied=1] | T07-P44: load=821, window=53s | T07-P44: review=권한 | T07-P44: 경계 D 위반 여부를 판정 | T07-P44: RECURRENCE_TIMELINE + T07-P44/CH08 |

채점은 결론보다 근거를 본다. T07-P44/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · backend threat modeling을 요청 흐름에 붙이기 — evidence 판독 문제 14개

T07-P44 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P44 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P44-V01 | T07-P44: latency=24ms; queue=68; retry=2 | T07-P44: 과부하 | T07-P44: 축=과부하; 원인 확정은 보류 | T07-P44: QUEUE_PRESSURE + T07-P44/CH08 | T07-P44: 피할 오판=복구 과잉 |
| T07-P44-V02 | T07-P44: latency=91ms; queue=79; retry=5 | T07-P44: sampling | T07-P44: 축=sampling; 원인 확정은 보류 | T07-P44: TRACE_METRIC_CROSSCHECK + T07-P44/CH08 | T07-P44: 피할 오판=잘못된 전제 |
| T07-P44-V03 | T07-P44: latency=158ms; queue=10; retry=1 | T07-P44: client disconnect | T07-P44: 축=client disconnect; 원인 확정은 보류 | T07-P44: COMMIT_TIMELINE + T07-P44/CH08 | T07-P44: 피할 오판=경계 누락 |
| T07-P44-V04 | T07-P44: latency=225ms; queue=21; retry=4 | T07-P44: 재발 | T07-P44: 축=재발; 원인 확정은 보류 | T07-P44: RECURRENCE_TIMELINE + T07-P44/CH08 | T07-P44: 피할 오판=증거 혼동 |
| T07-P44-V05 | T07-P44: latency=292ms; queue=32; retry=0 | T07-P44: 대형 입력 | T07-P44: 축=대형 입력; 원인 확정은 보류 | T07-P44: SIZE_LIMIT + T07-P44/CH08 | T07-P44: 피할 오판=소유권 혼동 |
| T07-P44-V06 | T07-P44: latency=359ms; queue=43; retry=3 | T07-P44: 소유권 위조 | T07-P44: 축=소유권 위조; 원인 확정은 보류 | T07-P44: AUTHZ_POLICY + T07-P44/CH08 | T07-P44: 피할 오판=운영 한계 누락 |
| T07-P44-V07 | T07-P44: latency=426ms; queue=54; retry=6 | T07-P44: 순서 역전 | T07-P44: 축=순서 역전; 원인 확정은 보류 | T07-P44: SEQUENCE_STATE + T07-P44/CH08 | T07-P44: 피할 오판=오류 합치기 |
| T07-P44-V08 | T07-P44: latency=493ms; queue=65; retry=2 | T07-P44: 복구 범위 | T07-P44: 축=복구 범위; 원인 확정은 보류 | T07-P44: RECOVERY_AUDIT + T07-P44/CH08 | T07-P44: 피할 오판=AI 과신 |
| T07-P44-V09 | T07-P44: latency=560ms; queue=76; retry=5 | T07-P44: 재전송 | T07-P44: 축=재전송; 원인 확정은 보류 | T07-P44: IDEMPOTENCY_RECORD + T07-P44/CH08 | T07-P44: 피할 오판=복구 과잉 |
| T07-P44-V10 | T07-P44: latency=627ms; queue=7; retry=1 | T07-P44: 구버전 client | T07-P44: 축=구버전 client; 원인 확정은 보류 | T07-P44: CLIENT_VERSION + T07-P44/CH08 | T07-P44: 피할 오판=잘못된 전제 |
| T07-P44-V11 | T07-P44: latency=694ms; queue=18; retry=4 | T07-P44: unknown outcome | T07-P44: 축=unknown outcome; 원인 확정은 보류 | T07-P44: PROVIDER_RESULT + T07-P44/CH08 | T07-P44: 피할 오판=경계 누락 |
| T07-P44-V12 | T07-P44: latency=761ms; queue=29; retry=0 | T07-P44: 과부하 | T07-P44: 축=과부하; 원인 확정은 보류 | T07-P44: QUEUE_PRESSURE + T07-P44/CH08 | T07-P44: 피할 오판=증거 혼동 |
| T07-P44-V13 | T07-P44: latency=828ms; queue=40; retry=3 | T07-P44: sampling | T07-P44: 축=sampling; 원인 확정은 보류 | T07-P44: TRACE_METRIC_CROSSCHECK + T07-P44/CH08 | T07-P44: 피할 오판=소유권 혼동 |
| T07-P44-V14 | T07-P44: latency=895ms; queue=51; retry=6 | T07-P44: client disconnect | T07-P44: 축=client disconnect; 원인 확정은 보류 | T07-P44: COMMIT_TIMELINE + T07-P44/CH08 | T07-P44: 피할 오판=운영 한계 누락 |

## CHAPTER 24 · backend threat modeling을 요청 흐름에 붙이기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P44에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P44-D01 | T07-P44: 결과 합치기 | T07-P44: 여러 오류를 하나의 status/error code로 합친다 | T07-P44: client 행동과 retry 가능성을 잃지 않는지 | T07-P44: DISCONNECT[disconnect_ms=92; commit_state=UNKNOWN; request=044-00] | T07-P44: COMMIT_TIMELINE + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P44-D02 | T07-P44: retry 추가 | T07-P44: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P44: unknown outcome과 duplicate side effect를 구분하는지 | T07-P44: RECURRENCE[occurrence=3; interval_s=61; mitigation_applied=1] | T07-P44: RECURRENCE_TIMELINE + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P44-D03 | T07-P44: 로그 확대 | T07-P44: debug를 위해 payload와 context 기록을 늘린다 | T07-P44: secret·PII·cardinality 비용을 통제하는지 | T07-P44: LARGE_INPUT[body_kb=848; limit_kb=768; parsed=0] | T07-P44: SIZE_LIMIT + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P44-D04 | T07-P44: 강제 종료 | T07-P44: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P44: in-flight request와 background work의 결과를 잃는지 | T07-P44: OWNER_SPOOF[actor=A2; payload_owner=B3; auth_owner=A2] | T07-P44: AUTHZ_POLICY + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P44-D05 | T07-P44: AI package 추가 | T07-P44: AI가 제안한 새 dependency를 도입한다 | T07-P44: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P44: REORDER[in_seq=7,5,6; applied_version=3] | T07-P44: SEQUENCE_STATE + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P44-D06 | T07-P44: 비동기화 | T07-P44: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P44: durability·status API·worker retry 계약이 생기는지 | T07-P44: RECOVERY_SCOPE[selected=95; expected=7; backup=1; dry_run=1] | T07-P44: RECOVERY_AUDIT + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P44-D07 | T07-P44: 권한 shortcut | T07-P44: payload의 owner/tenant id를 바로 사용한다 | T07-P44: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P44: REPLAY[key=cmd-044-06; attempts=2; response_seen=0] | T07-P44: IDEMPOTENCY_RECORD + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P44-D08 | T07-P44: 순서 병렬화 | T07-P44: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P44: 선후관계 invariant와 race를 깨지 않는지 | T07-P44: OLD_SCHEMA[client=v4; server=v5; extra_field=1] | T07-P44: CLIENT_VERSION + T07-P44/CH08 | T07-P44: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P44: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · backend threat modeling을 요청 흐름에 붙이기 — 최종 contract와 evidence spine

**최종 계약:** 위협 모델링은 공격 목록 암기가 아니라 데이터가 신뢰 경계를 지나는 지점을 찾아 잘못된 가정을 검토하는 과정이다.

**정상 메커니즘:** entry point, asset, actor, trust boundary, privilege, side effect를 diagram과 표로 연결한다.

**대표 실패:** 시스템 전체를 한 박스로 그려 경계가 사라지거나 보안 장치를 나열하고 실제 자산과 연결하지 않는다.

**검증 evidence:** 각 경계의 input, authentication/authorization, validation, logging, failure handling을 확인한다.

**직접 행동:** 간단한 flow 노드 목록에서 외부→내부 경계를 자동 표시한다.

**다음 연결:** `BLOCK 03 종합: 계정 API의 신뢰 경계 감사`.

`backend threat modeling을 요청 흐름에 붙이기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| OWASP-API | OWASP-API | backend threat modeling을 요청 흐름에 붙이기의 개념·실패·운영 판단 교차 확인 |
| OWASP-ASVS | OWASP-ASVS | backend threat modeling을 요청 흐름에 붙이기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | backend threat modeling을 요청 흐름에 붙이기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | backend threat modeling을 요청 흐름에 붙이기의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | backend threat modeling을 요청 흐름에 붙이기의 개념·실패·운영 판단 교차 확인 |

`backend threat modeling을 요청 흐름에 붙이기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
