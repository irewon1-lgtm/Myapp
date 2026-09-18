# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 01 · 요청이 서버에 도착해 응답이 되기까지

### LESSON 14 · API version과 호환성은 기존 client를 깨지 않는 약속

## CHAPTER 01 · API version과 호환성은 기존 client를 깨지 않는 약속 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 서버 코드를 바꾸는 것과 이미 배포된 API 계약을 바꾸는 것은 다른 문제다.

**아주 쉬운 사건.** old client는 enum 두 값만 안다. 이 사건에서는 먼저 **새 enum이 호환성을 깨는지 본다**.

**왜 필요한가.** 정상 동작은 필드 추가·삭제·의미 변경·enum 확장 같은 변화가 기존 client의 가정을 깨는지 판단하고 versioning/deprecation 전략을 적용한다. 반대로 서버와 client를 동시에 배포할 수 있다고 가정해 기존 앱이 갑자기 실패한다.

**암기:** `API version과 호환성은 기존 client를 깨지 않는 약속`의 역할 한 줄.

**직접 이해:** `API version과 호환성은 기존 client를 깨지 않는 약속`의 입력·상태·결과 경계.

**AI 위임 가능:** `API version과 호환성은 기존 client를 깨지 않는 약속` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `old client는 enum 두 값만 안다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 실제 client version 분포, deprecated field 사용량, schema diff와 contract test 결과를 확인한다.

## CHAPTER 02 · API version과 호환성은 기존 client를 깨지 않는 약속 — 아주 쉬운 예를 한 단계씩 해석

T07-P14: `old client는 enum 두 값만 안다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | old client는 enum 두 값만 안다 | T07-P14 외부 입력 | T07-P14: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | 새 enum이 호환성을 깨는지 본다 | T07-P14 판단 기준 | T07-P14/CH08 관측표와 대조 |
| 정상 경로 | T07-P14/CH03 M1→M5 | API version과 호환성은 기존 client를 깨지 않는 약속: 완료 시점을 단계별로 분리 | T07-P14: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P14/CH06 서버와 client를 동시에 배포할 수 있다고 가정해 기존 앱이 갑자기 실패한다. | T07-P14: 깨진 계약 하나를 특정 | API version과 호환성은 기존 client를 깨지 않는 약속: 증상과 원인을 분리 |
| 재검증 | T07-P14/CH10 직접 실행 | T07-P14: 예상값 T07-P014 4/4 기록 | API version과 호환성은 기존 client를 깨지 않는 약속: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P14/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P14/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · API version과 호환성은 기존 client를 깨지 않는 약속 — 내부 메커니즘과 상태 전이

필드 추가·삭제·의미 변경·enum 확장 같은 변화가 기존 client의 가정을 깨는지 판단하고 versioning/deprecation 전략을 적용한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | old client는 enum 두 값만 안다 | source/actor/size를 보존 |
| M2 | 경계 판단 | 새 enum이 호환성을 깨는지 본다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | 필드 추가·삭제·의미 변경·enum 확장 같은 변화가 기존 client의 가정을 깨는지 판단하고 versioning/deprecation 전략을 적용한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 실제 client version 분포, deprecated field 사용량, schema diff와 contract test 결과를 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 이전 응답과 새 응답의 key 집합을 비교해 breaking change 후보를 찾는다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`API version과 호환성은 기존 client를 깨지 않는 약속` 흐름을 framework 이름 없이 설명한다.

막히면 `old client는 enum 두 값만 안다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · API version과 호환성은 기존 client를 깨지 않는 약속 — 실전 경계 A

**경계 A.** 기존 response에 optional field를 추가하는 변화는 보통 required field 추가보다 호환성이 높지만, 오래된 client가 unknown field에서 실패하는 구현인지 실제 contract test로 확인해야 한다.

T07-P14/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P14에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P14/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P14/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P14): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P14/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P14-A1-387 | T07-P14 조건 | 기존 response에 optional field를 추가하는 변화는 보통 required field 추가보다 호환성이 높지만, 오래된 client가 unknown field에서 실패하는 구현인지 실제 contract test로 확인해야 한다. |
| T07-P14-A2-388 | T07-P14 변화점 | T07-P14/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P14-A3-389 | T07-P14 반례 | T07-P14/CH06 대표 실패와 A 위반을 구별 |
| T07-P14-A4-390 | T07-P14 근거 | T07-P14/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P14-A5-391 | T07-P14 재실험 | old client는 enum 두 값만 안다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · API version과 호환성은 기존 client를 깨지 않는 약속 — 실전 경계 B

**경계 B.** enum에 새 값을 추가하면 타입 모양은 그대로여도 모든 값을 exhaustive switch로 가정한 client가 깨질 수 있어 open set인지 closed set인지 계약에 적는다.

T07-P14/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P14에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P14/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P14/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P14): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P14/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P14-B1-418 | T07-P14 조건 | enum에 새 값을 추가하면 타입 모양은 그대로여도 모든 값을 exhaustive switch로 가정한 client가 깨질 수 있어 open set인지 closed set인지 계약에 적는다. |
| T07-P14-B2-419 | T07-P14 독립성 | T07-P14/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P14-B3-420 | T07-P14 상태 | T07-P14/CH03 before·after 위치를 다시 지정 |
| T07-P14-B4-421 | T07-P14 반증 | T07-P14/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P14-B5-422 | T07-P14 적용 | API version과 호환성은 기존 client를 깨지 않는 약속의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · API version과 호환성은 기존 client를 깨지 않는 약속 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **서버와 client를 동시에 배포할 수 있다고 가정해 기존 앱이 갑자기 실패한다.**

아래 여섯 사례는 T07-P14의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P14-F01 | T07-P14: 대표 실패 | T07-P14: T07-P14/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P14: 현상만 보고 원인을 확정 | T07-P14/CH08 evidence map에서 상태를 대조 |
| T07-P14-F02 | T07-P14: 경계 A 누락 | T07-P14: T07-P14/CH04 경계 A 위반 입력 | T07-P14: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P14/CH08 evidence map에서 상태를 대조 |
| T07-P14-F03 | T07-P14: 경계 B 누락 | T07-P14: T07-P14/CH05 경계 B 위반 입력 | T07-P14: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P14/CH08 evidence map에서 상태를 대조 |
| T07-P14-F04 | T07-P14: 복구 경계 C 누락 | T07-P14: T07-P14/CH07 경계 C 복구 조건 | T07-P14: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P14/CH08 evidence map에서 상태를 대조 |
| T07-P14-F05 | T07-P14: 운영 경계 D 누락 | T07-P14: T07-P14/CH09 경계 D 운영 조건 | T07-P14: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P14/CH08 evidence map에서 상태를 대조 |
| T07-P14-F06 | T07-P14: 증거 없는 결론 | T07-P14: T07-P14/CH02 첫 판단만 존재 | T07-P14: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P14/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P14/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · API version과 호환성은 기존 client를 깨지 않는 약속 — 복구 가능한 상태와 수명

**경계 C.** string field를 유지하면서 단위·정렬 기준·null 의미만 바꾸는 semantic change도 breaking change가 될 수 있어 schema diff만으로 호환성을 판정하지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P14에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P14/CH08 evidence map을 본다. 복구 후에는 T07-P14/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P14에서 이미 확정된 side effect는 T07-P14/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P14-R1-480 | T07-P14 중단 직전 | T07-P14/CH03에서 이미 확정된 상태만 표시 |
| T07-P14-R2-481 | T07-P14 재시작 직후 | string field를 유지하면서 단위·정렬 기준·null 의미만 바꾸는 semantic change도 breaking change가 될 수 있어 schema diff만으로 호환성을 판정하지 않는다. |
| T07-P14-R3-482 | T07-P14 재검증 | T07-P14/CH08 근거로 중복·누락 여부 확인 |
| T07-P14-R4-483 | T07-P14 재실행 | T07-P14/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · API version과 호환성은 기존 client를 깨지 않는 약속 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | old client는 enum 두 값만 안다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | 새 enum이 호환성을 깨는지 본다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | 필드 추가·삭제·의미 변경·enum 확장 같은 변화가 기존 client의 가정을 깨는지 판단하고 versioning/deprecation 전략을 적용한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 서버와 client를 동시에 배포할 수 있다고 가정해 기존 앱이 갑자기 실패한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 실제 client version 분포, deprecated field 사용량, schema diff와 contract test 결과를 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`API version과 호환성은 기존 client를 깨지 않는 약속` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · API version과 호환성은 기존 client를 깨지 않는 약속 — 운영 한계와 종료 조건

**경계 D.** deprecated field 제거 전에 실제 traffic에서 old client 비율과 field 사용량이 0에 가까워졌는지 측정하고, client release cycle보다 충분한 overlap을 둔다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P14/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P14/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P14 과제: 경계 D와 T07-P14/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P14-O1-542 | synthetic-load=42 | T07-P14/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P14-O2-543 | synthetic-budget=692ms | T07-P14 timeout과 unknown outcome을 분리 |
| T07-P14-O3-544 | T07-P14 종료 | deprecated field 제거 전에 실제 traffic에서 old client 비율과 field 사용량이 0에 가까워졌는지 측정하고, client release cycle보다 충분한 overlap을 둔다. |
| T07-P14-O4-545 | T07-P14 완화 | T07-P14/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · API version과 호환성은 기존 client를 깨지 않는 약속 — 직접 실행하는 작은 모델

`API version과 호환성은 기존 client를 깨지 않는 약속` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P014 4/4`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P014";
const checks = [true,true,true,true];
const passed = checks.filter(Boolean).length;
console.log(marker, `${passed}/${checks.length}`);
```

기준 출력: `T07-P014 4/4`.

`API version과 호환성은 기존 client를 깨지 않는 약속`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P14-L1-574 | constmarker="T07-P014"; | T07-P14 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P14-L2-575 | constchecks=[true,true,true,true]; | T07-P14 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P14-L3-576 | constpassed=checks.filter(Boolean).length; | T07-P14 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P14-L4-577 | console.log(marker,`${passed}/${checks.length}`); | T07-P14 출력 관측점; 예상 `T07-P014 4/4`와 비교 |
| T07-P14-LX-663 | T07-P14 실행 기록 | T07-P14 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · API version과 호환성은 기존 client를 깨지 않는 약속 — 한 부분만 수정하고 다시 예측

수정 과제: **false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다**.

수정 전은 `T07-P014 4/4`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P14/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P14-D1-604 | 기준 `T07-P014 4/4` | T07-P14 수정 전 실행을 먼저 재현 |
| T07-P14-D2-605 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | T07-P14 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P14-D3-606 | T07-P14 새 예측 | T07-P14 실행 전에 출력·상태를 먼저 기록 |
| T07-P14-D4-607 | T07-P14 재실행 | T07-P14/CH10 실제값과 새 예측을 대조 |
| T07-P14-D5-608 | T07-P14 반례 | T07-P14/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P14-D6-609 | T07-P14 근거 | T07-P14/CH08 상태가 설명과 일치해야 완료 |
| T07-P14-D7-610 | T07-P14 이유 | T07-P14 변경 이유를 API version과 호환성은 기존 client를 깨지 않는 약속 계약과 연결해 설명 |

## CHAPTER 12 · API version과 호환성은 기존 client를 깨지 않는 약속 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: API version과 호환성은 기존 client를 깨지 않는 약속 | 서버 코드를 바꾸는 것과 이미 배포된 API 계약을 바꾸는 것은 다른 문제다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P14/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | 필드 추가·삭제·의미 변경·enum 확장 같은 변화가 기존 client의 가정을 깨는지 판단하고 versioning/deprecation 전략을 적용한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 서버와 client를 동시에 배포할 수 있다고 가정해 기존 앱이 갑자기 실패한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 실제 client version 분포, deprecated field 사용량, schema diff와 contract test 결과를 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P14/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P14/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P14/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P14/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `API version과 호환성은 기존 client를 깨지 않는 약속` 실행 코드 수정 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | `API version과 호환성은 기존 client를 깨지 않는 약속` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P14/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P14/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · API version과 호환성은 기존 client를 깨지 않는 약속 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 서버 코드를 바꾸는 것과 이미 배포된 API 계약을 바꾸는 것은 다른 문제다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P14/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P14/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P14/CH10 실행용 boilerplate·test 후보 | T07-P14: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P014 4/4` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P14/CH02의 판단 기준과 T07-P14/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P14-AI1-666 | T07-P14 사람 결정 | T07-P14 업무 의미·허용 위험·완료 기준 소유 |
| T07-P14-AI2-667 | T07-P14 AI 초안 | T07-P14/CH10 boilerplate·test 후보까지만 위임 |
| T07-P14-AI3-668 | T07-P14 검증 | T07-P14/CH06 반례와 T07-P14/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · API version과 호환성은 기존 client를 깨지 않는 약속 — 경계 조합 실험 8개

T07-P14: T07-P14/CH04~T07-P14/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P14의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-K01 | T07-P14: 경계 A | T07-P14: 경계 B | T07-P14: DISCONNECT[disconnect_ms=80; commit_state=UNKNOWN; request=014-01] / sample=83 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + COMMIT_TIMELINE |
| T07-P14-K02 | T07-P14: 경계 A | T07-P14: 경계 C | T07-P14: RECURRENCE[occurrence=4; interval_s=46; mitigation_applied=1] / sample=90 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + RECURRENCE_TIMELINE |
| T07-P14-K03 | T07-P14: 경계 A | T07-P14: 경계 D | T07-P14: LARGE_INPUT[body_kb=728; limit_kb=768; parsed=0] / sample=97 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + SIZE_LIMIT |
| T07-P14-K04 | T07-P14: 경계 B | T07-P14: 경계 C | T07-P14: DRAIN[ready=0; active=15; drain_deadline_s=9] / sample=15 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + DRAIN_STATE |
| T07-P14-K05 | T07-P14: 경계 B | T07-P14: 경계 D | T07-P14: REPLAY[key=cmd-014-05; attempts=4; response_seen=0] / sample=22 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + IDEMPOTENCY_RECORD |
| T07-P14-K06 | T07-P14: 경계 C | T07-P14: 경계 D | T07-P14: OLD_SCHEMA[client=v1; server=v2; extra_field=0] / sample=29 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + CLIENT_VERSION |
| T07-P14-K07 | T07-P14: 경계 A | T07-P14: 경계 B+C | T07-P14: UNKNOWN_OUTCOME[timeout_ms=171; provider_state=UNKNOWN; lookup_id=p01407] / sample=36 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + PROVIDER_RESULT |
| T07-P14-K08 | T07-P14: 경계 B | T07-P14: 경계 C+D | T07-P14: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] / sample=43 | T07-P14: 먼저 깨지는 경계를 판정 | T07-P14: T07-P14/CH08 + DURABLE_STATE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P14/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · API version과 호환성은 기존 client를 깨지 않는 약속 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P14와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-B13 | T07-P14: streaming과 backpressure로 큰 데이터를 다루기 | T07-P14: producer 8MB/s, consumer 3MB/s다 | T07-P14: buffer 증가와 backpressure 시점을 본다 | T07-P14: T07-P14/CH08 증거와 형제 LESSON 증거를 분리 | T07-P14: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P14-B15 | T07-P14: BLOCK 01 종합: 하나의 요청 수명주기를 끝까지 추적하기 | T07-P14: 한 주문 요청이 parse부터 response까지 간다 | T07-P14: 확정 지점과 실패 지점을 timeline에 표시한다 | T07-P14: T07-P14/CH08 증거와 형제 LESSON 증거를 분리 | T07-P14: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P14-B12 | T07-P14: connection·keep-alive·pool을 자원 관점으로 보기 | T07-P14: pool 4개에 동시 요청 7개가 들어온다 | T07-P14: active와 waiter를 따로 센다 | T07-P14: T07-P14/CH08 증거와 형제 LESSON 증거를 분리 | T07-P14: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P14-B11 | T07-P14: timeout·deadline·cancellation으로 기다림에 끝을 만들기 | T07-P14: 전체 budget 800ms인데 하위 호출 timeout이 2초다 | T07-P14: 남은 deadline을 계산한다 | T07-P14: T07-P14/CH08 증거와 형제 LESSON 증거를 분리 | T07-P14: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P14-B10 | T07-P14: async I/O와 event loop를 요청 처리에서 이해하기 | T07-P14: 두 Promise와 CPU loop가 같은 요청에 있다 | T07-P14: 기다림과 CPU 점유를 구분한다 | T07-P14: T07-P14/CH08 증거와 형제 LESSON 증거를 분리 | T07-P14: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · API version과 호환성은 기존 client를 깨지 않는 약속 — 선택형 실패 주입 6개

T07-P14: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P14 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-X01 | T07-P14: 대형 입력 | T07-P14: 입력 크기가 정상의 100배; sample=451 | T07-P14: 의미 검증과 resource limit | T07-P14: T07-P14/CH02 판단과 별도 기록 | T07-P14: body/batch size·parse time·memory·reject status |
| T07-P14-X02 | T07-P14: drain | T07-P14: 배포 중 기존 요청이 처리 중; sample=468 | T07-P14: 새 traffic 차단과 in-flight 처리 | T07-P14: T07-P14/CH02 판단과 별도 기록 | T07-P14: readiness·active requests·deadline·final state |
| T07-P14-X03 | T07-P14: 재전송 | T07-P14: 응답 유실 뒤 같은 command가 다시 도착함; sample=485 | T07-P14: 중복 side effect 여부 | T07-P14: T07-P14/CH02 판단과 별도 기록 | T07-P14: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P14-X04 | T07-P14: 구버전 client | T07-P14: 한 단계 이전 schema가 요청됨; sample=502 | T07-P14: 호환 입력과 breaking change | T07-P14: T07-P14/CH02 판단과 별도 기록 | T07-P14: schema version·실제 client 분포·contract test |
| T07-P14-X05 | T07-P14: unknown outcome | T07-P14: dependency timeout 후 성공 여부 불명; sample=519 | T07-P14: 실패와 미확정 결과 | T07-P14: T07-P14/CH02 판단과 별도 기록 | T07-P14: provider id·조회 결과·retry history |
| T07-P14-X06 | T07-P14: 재시작 | T07-P14: side effect 직후 process가 재시작됨; sample=536 | T07-P14: durable state와 memory state | T07-P14: T07-P14/CH02 판단과 별도 기록 | T07-P14: commit·outbox·job id·restart 전후 상태 |

## CHAPTER 17 · API version과 호환성은 기존 client를 깨지 않는 약속 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P14에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-E01 | T07-P14: 대표 실패를 원인으로 착각 | T07-P14: T07-P14/CH06 실패 case를 다른 입력으로 재현 | T07-P14: 현상과 원인을 같은 것으로 봄 | T07-P14: T07-P14/CH06 대표 실패와 T07-P14/CH08 증거를 다시 대조 | T07-P14: T07-P14/CH08 |
| T07-P14-E02 | T07-P14: 경계 A 생략 | T07-P14: T07-P14/CH04의 조건 하나를 반대로 설정 | T07-P14: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P14: T07-P14/CH04를 새 입력에 적용 | T07-P14: T07-P14/CH08 |
| T07-P14-E03 | T07-P14: 경계 B 생략 | T07-P14: T07-P14/CH05의 조건 하나를 반대로 설정 | T07-P14: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P14: T07-P14/CH05를 새 입력에 적용 | T07-P14: T07-P14/CH08 |
| T07-P14-E04 | T07-P14: 복구 상태 혼동 | T07-P14: T07-P14/CH07에서 처리 중단을 주입 | T07-P14: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P14: T07-P14/CH07에서 수명 경계를 다시 표시 | T07-P14: T07-P14/CH08 |
| T07-P14-E05 | T07-P14: 운영 한계 누락 | T07-P14: T07-P14/CH09에서 부하 또는 drain 조건을 변경 | T07-P14: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P14: T07-P14/CH09의 종료 조건을 다시 작성 | T07-P14: T07-P14/CH08 |
| T07-P14-E06 | T07-P14: 증거 없는 성공 판정 | T07-P14: T07-P14/CH08에서 증거 하나를 숨김 | T07-P14: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P14: T07-P14/CH08에서 독립 증거 둘을 선택 | T07-P14: T07-P14/CH08 |

## CHAPTER 18 · API version과 호환성은 기존 client를 깨지 않는 약속 — synthetic 관측값 판독 6개

T07-P14: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P14의 숫자 하나만으로 원인을 단정하지 않고 T07-P14/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-O01 | T07-P14/requests_received | 740 | T07-P14: 요청 수신 수 | T07-P14: 축=unknown outcome; 원인 확정 금지 | T07-P14: PROVIDER_RESULT + T07-P14/CH08 |
| T07-P14-O02 | T07-P14/handler_started | 757 | T07-P14: handler 진입 수 | T07-P14: 축=재시작; 원인 확정 금지 | T07-P14: DURABLE_STATE + T07-P14/CH08 |
| T07-P14-O03 | T07-P14/responses_completed | 774 | T07-P14: 응답 완료 수 | T07-P14: 축=과부하; 원인 확정 금지 | T07-P14: QUEUE_PRESSURE + T07-P14/CH08 |
| T07-P14-O04 | T07-P14/latency_ms | 791 | T07-P14: 요청 처리 지연 | T07-P14: 축=sampling; 원인 확정 금지 | T07-P14: TRACE_METRIC_CROSSCHECK + T07-P14/CH08 |
| T07-P14-O05 | T07-P14/body_kb | 808 | T07-P14: 입력 크기 | T07-P14: 축=client disconnect; 원인 확정 금지 | T07-P14: COMMIT_TIMELINE + T07-P14/CH08 |
| T07-P14-O06 | T07-P14/active_connections | 825 | T07-P14: 활성 연결 수 | T07-P14: 축=재발; 원인 확정 금지 | T07-P14: RECURRENCE_TIMELINE + T07-P14/CH08 |

## CHAPTER 19 · API version과 호환성은 기존 client를 깨지 않는 약속 — 선택형 코드 리뷰 6질문

T07-P14: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P14에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-R01 | T07-P14: 관측 | T07-P14: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P14: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P14: T07-P14/CH04 | T07-P14: DURABLE_STATE |
| T07-P14-R02 | T07-P14: 복구 | T07-P14: 재시작 뒤에도 필요한 상태가 남는가 | T07-P14: OVERLOAD[rps=848; p99_ms=840; queue=11] | T07-P14: T07-P14/CH05 | T07-P14: QUEUE_PRESSURE |
| T07-P14-R03 | T07-P14: 중복 | T07-P14: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P14: SAMPLING[sample_rate=83%; trace_present=0; metric_present=1] | T07-P14: T07-P14/CH06 | T07-P14: TRACE_METRIC_CROSSCHECK |
| T07-P14-R04 | T07-P14: timeout | T07-P14: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P14: DISCONNECT[disconnect_ms=106; commit_state=UNKNOWN; request=014-03] | T07-P14: T07-P14/CH07 | T07-P14: COMMIT_TIMELINE |
| T07-P14-R05 | T07-P14: 관측 | T07-P14: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P14: RECURRENCE[occurrence=6; interval_s=68; mitigation_applied=1] | T07-P14: T07-P14/CH04 | T07-P14: RECURRENCE_TIMELINE |
| T07-P14-R06 | T07-P14: 복구 | T07-P14: 재시작 뒤에도 필요한 상태가 남는가 | T07-P14: LARGE_INPUT[body_kb=904; limit_kb=768; parsed=0] | T07-P14: T07-P14/CH05 | T07-P14: SIZE_LIMIT |

## CHAPTER 20 · API version과 호환성은 기존 client를 깨지 않는 약속 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P14에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-I01 | T07-P14: 재주입 | T07-P14: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P14: OVERLOAD[rps=815; p99_ms=723; queue=17] | T07-P14: T07-P14/CH08 + QUEUE_PRESSURE | T07-P14-incident-266 |
| T07-P14-I02 | T07-P14: 회귀 고정 | T07-P14: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P14: SAMPLING[sample_rate=70%; trace_present=1; metric_present=1] | T07-P14: T07-P14/CH08 + TRACE_METRIC_CROSSCHECK | T07-P14-incident-267 |
| T07-P14-I03 | T07-P14: 영향 범위 | T07-P14: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P14: DISCONNECT[disconnect_ms=93; commit_state=UNKNOWN; request=014-02] | T07-P14: T07-P14/CH08 + COMMIT_TIMELINE | T07-P14-incident-268 |
| T07-P14-I04 | T07-P14: 변경 동결 | T07-P14: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P14: RECURRENCE[occurrence=5; interval_s=57; mitigation_applied=1] | T07-P14: T07-P14/CH08 + RECURRENCE_TIMELINE | T07-P14-incident-269 |
| T07-P14-I05 | T07-P14: correlation | T07-P14: 한 request/job/resource id를 시간축에 고정 | T07-P14: LARGE_INPUT[body_kb=816; limit_kb=768; parsed=0] | T07-P14: T07-P14/CH08 + SIZE_LIMIT | T07-P14-incident-270 |
| T07-P14-I06 | T07-P14: 마지막 정상 | T07-P14: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P14: DRAIN[ready=0; active=16; drain_deadline_s=10] | T07-P14: T07-P14/CH08 + DRAIN_STATE | T07-P14-incident-271 |

## CHAPTER 21 · API version과 호환성은 기존 client를 깨지 않는 약속 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P14/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P14/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P14/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P14/CH04~T07-P14/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P14/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P14/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P14/CH18 signal 두 개와 T07-P14/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P14/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P14/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · API version과 호환성은 기존 client를 깨지 않는 약속 — 통합 casebook 16문제

T07-P14 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P14 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P14-C01 | T07-P14: 경계 A | T07-P14: SAMPLING[sample_rate=57%; trace_present=0; metric_present=1] | T07-P14: load=506, window=68s | T07-P14: review=자원 | T07-P14: 경계 A 위반 여부를 판정 | T07-P14: TRACE_METRIC_CROSSCHECK + T07-P14/CH08 |
| T07-P14-C02 | T07-P14: 경계 B | T07-P14: DISCONNECT[disconnect_ms=80; commit_state=UNKNOWN; request=014-01] | T07-P14: load=529, window=87s | T07-P14: review=순서 | T07-P14: 경계 B 위반 여부를 판정 | T07-P14: COMMIT_TIMELINE + T07-P14/CH08 |
| T07-P14-C03 | T07-P14: 경계 C | T07-P14: RECURRENCE[occurrence=4; interval_s=46; mitigation_applied=1] | T07-P14: load=552, window=16s | T07-P14: review=관측 | T07-P14: 경계 C 위반 여부를 판정 | T07-P14: RECURRENCE_TIMELINE + T07-P14/CH08 |
| T07-P14-C04 | T07-P14: 경계 D | T07-P14: LARGE_INPUT[body_kb=728; limit_kb=768; parsed=0] | T07-P14: load=575, window=35s | T07-P14: review=상태 변경 | T07-P14: 경계 D 위반 여부를 판정 | T07-P14: SIZE_LIMIT + T07-P14/CH08 |
| T07-P14-C05 | T07-P14: 경계 A | T07-P14: DRAIN[ready=0; active=15; drain_deadline_s=9] | T07-P14: load=598, window=54s | T07-P14: review=retry | T07-P14: 경계 A 위반 여부를 판정 | T07-P14: DRAIN_STATE + T07-P14/CH08 |
| T07-P14-C06 | T07-P14: 경계 B | T07-P14: REPLAY[key=cmd-014-05; attempts=4; response_seen=0] | T07-P14: load=621, window=73s | T07-P14: review=복구 | T07-P14: 경계 B 위반 여부를 판정 | T07-P14: IDEMPOTENCY_RECORD + T07-P14/CH08 |
| T07-P14-C07 | T07-P14: 경계 C | T07-P14: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P14: load=644, window=92s | T07-P14: review=동시성 | T07-P14: 경계 C 위반 여부를 판정 | T07-P14: CLIENT_VERSION + T07-P14/CH08 |
| T07-P14-C08 | T07-P14: 경계 D | T07-P14: UNKNOWN_OUTCOME[timeout_ms=171; provider_state=UNKNOWN; lookup_id=p01407] | T07-P14: load=667, window=21s | T07-P14: review=민감정보 | T07-P14: 경계 D 위반 여부를 판정 | T07-P14: PROVIDER_RESULT + T07-P14/CH08 |
| T07-P14-C09 | T07-P14: 경계 A | T07-P14: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P14: load=690, window=40s | T07-P14: review=중복 | T07-P14: 경계 A 위반 여부를 판정 | T07-P14: DURABLE_STATE + T07-P14/CH08 |
| T07-P14-C10 | T07-P14: 경계 B | T07-P14: OVERLOAD[rps=479; p99_ms=903; queue=26] | T07-P14: load=713, window=59s | T07-P14: review=권한 | T07-P14: 경계 B 위반 여부를 판정 | T07-P14: QUEUE_PRESSURE + T07-P14/CH08 |
| T07-P14-C11 | T07-P14: 경계 C | T07-P14: SAMPLING[sample_rate=10%; trace_present=0; metric_present=1] | T07-P14: load=736, window=78s | T07-P14: review=입력 경계 | T07-P14: 경계 C 위반 여부를 판정 | T07-P14: TRACE_METRIC_CROSSCHECK + T07-P14/CH08 |
| T07-P14-C12 | T07-P14: 경계 D | T07-P14: DISCONNECT[disconnect_ms=113; commit_state=UNKNOWN; request=014-11] | T07-P14: load=759, window=97s | T07-P14: review=timeout | T07-P14: 경계 D 위반 여부를 판정 | T07-P14: COMMIT_TIMELINE + T07-P14/CH08 |
| T07-P14-C13 | T07-P14: 경계 A | T07-P14: RECURRENCE[occurrence=4; interval_s=156; mitigation_applied=1] | T07-P14: load=782, window=26s | T07-P14: review=자원 | T07-P14: 경계 A 위반 여부를 판정 | T07-P14: RECURRENCE_TIMELINE + T07-P14/CH08 |
| T07-P14-C14 | T07-P14: 경계 B | T07-P14: LARGE_INPUT[body_kb=1608; limit_kb=768; parsed=0] | T07-P14: load=805, window=45s | T07-P14: review=순서 | T07-P14: 경계 B 위반 여부를 판정 | T07-P14: SIZE_LIMIT + T07-P14/CH08 |
| T07-P14-C15 | T07-P14: 경계 C | T07-P14: DRAIN[ready=0; active=2; drain_deadline_s=11] | T07-P14: load=828, window=64s | T07-P14: review=관측 | T07-P14: 경계 C 위반 여부를 판정 | T07-P14: DRAIN_STATE + T07-P14/CH08 |
| T07-P14-C16 | T07-P14: 경계 D | T07-P14: REPLAY[key=cmd-014-15; attempts=2; response_seen=0] | T07-P14: load=851, window=83s | T07-P14: review=상태 변경 | T07-P14: 경계 D 위반 여부를 판정 | T07-P14: IDEMPOTENCY_RECORD + T07-P14/CH08 |

채점은 결론보다 근거를 본다. T07-P14/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · API version과 호환성은 기존 client를 깨지 않는 약속 — evidence 판독 문제 14개

T07-P14 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P14 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P14-V01 | T07-P14: latency=594ms; queue=18; retry=0 | T07-P14: client disconnect | T07-P14: 축=client disconnect; 원인 확정은 보류 | T07-P14: COMMIT_TIMELINE + T07-P14/CH08 | T07-P14: 피할 오판=동시성 무시 |
| T07-P14-V02 | T07-P14: latency=661ms; queue=29; retry=3 | T07-P14: 재발 | T07-P14: 축=재발; 원인 확정은 보류 | T07-P14: RECURRENCE_TIMELINE + T07-P14/CH08 | T07-P14: 피할 오판=상태 수명 혼동 |
| T07-P14-V03 | T07-P14: latency=728ms; queue=40; retry=6 | T07-P14: 대형 입력 | T07-P14: 축=대형 입력; 원인 확정은 보류 | T07-P14: SIZE_LIMIT + T07-P14/CH08 | T07-P14: 피할 오판=운영 한계 누락 |
| T07-P14-V04 | T07-P14: latency=795ms; queue=51; retry=2 | T07-P14: drain | T07-P14: 축=drain; 원인 확정은 보류 | T07-P14: DRAIN_STATE + T07-P14/CH08 | T07-P14: 피할 오판=오류 합치기 |
| T07-P14-V05 | T07-P14: latency=862ms; queue=62; retry=5 | T07-P14: 재전송 | T07-P14: 축=재전송; 원인 확정은 보류 | T07-P14: IDEMPOTENCY_RECORD + T07-P14/CH08 | T07-P14: 피할 오판=복구 과잉 |
| T07-P14-V06 | T07-P14: latency=929ms; queue=73; retry=1 | T07-P14: 구버전 client | T07-P14: 축=구버전 client; 원인 확정은 보류 | T07-P14: CLIENT_VERSION + T07-P14/CH08 | T07-P14: 피할 오판=잘못된 전제 |
| T07-P14-V07 | T07-P14: latency=996ms; queue=4; retry=4 | T07-P14: unknown outcome | T07-P14: 축=unknown outcome; 원인 확정은 보류 | T07-P14: PROVIDER_RESULT + T07-P14/CH08 | T07-P14: 피할 오판=경계 누락 |
| T07-P14-V08 | T07-P14: latency=1063ms; queue=15; retry=0 | T07-P14: 재시작 | T07-P14: 축=재시작; 원인 확정은 보류 | T07-P14: DURABLE_STATE + T07-P14/CH08 | T07-P14: 피할 오판=증거 혼동 |
| T07-P14-V09 | T07-P14: latency=1130ms; queue=26; retry=3 | T07-P14: 과부하 | T07-P14: 축=과부하; 원인 확정은 보류 | T07-P14: QUEUE_PRESSURE + T07-P14/CH08 | T07-P14: 피할 오판=동시성 무시 |
| T07-P14-V10 | T07-P14: latency=1197ms; queue=37; retry=6 | T07-P14: sampling | T07-P14: 축=sampling; 원인 확정은 보류 | T07-P14: TRACE_METRIC_CROSSCHECK + T07-P14/CH08 | T07-P14: 피할 오판=상태 수명 혼동 |
| T07-P14-V11 | T07-P14: latency=1264ms; queue=48; retry=2 | T07-P14: client disconnect | T07-P14: 축=client disconnect; 원인 확정은 보류 | T07-P14: COMMIT_TIMELINE + T07-P14/CH08 | T07-P14: 피할 오판=운영 한계 누락 |
| T07-P14-V12 | T07-P14: latency=1331ms; queue=59; retry=5 | T07-P14: 재발 | T07-P14: 축=재발; 원인 확정은 보류 | T07-P14: RECURRENCE_TIMELINE + T07-P14/CH08 | T07-P14: 피할 오판=오류 합치기 |
| T07-P14-V13 | T07-P14: latency=1398ms; queue=70; retry=1 | T07-P14: 대형 입력 | T07-P14: 축=대형 입력; 원인 확정은 보류 | T07-P14: SIZE_LIMIT + T07-P14/CH08 | T07-P14: 피할 오판=복구 과잉 |
| T07-P14-V14 | T07-P14: latency=1465ms; queue=1; retry=4 | T07-P14: drain | T07-P14: 축=drain; 원인 확정은 보류 | T07-P14: DRAIN_STATE + T07-P14/CH08 | T07-P14: 피할 오판=잘못된 전제 |

## CHAPTER 24 · API version과 호환성은 기존 client를 깨지 않는 약속 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P14에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P14-D01 | T07-P14: 결과 합치기 | T07-P14: 여러 오류를 하나의 status/error code로 합친다 | T07-P14: client 행동과 retry 가능성을 잃지 않는지 | T07-P14: LARGE_INPUT[body_kb=2152; limit_kb=768; parsed=0] | T07-P14: SIZE_LIMIT + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P14-D02 | T07-P14: retry 추가 | T07-P14: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P14: unknown outcome과 duplicate side effect를 구분하는지 | T07-P14: DRAIN[ready=0; active=10; drain_deadline_s=6] | T07-P14: DRAIN_STATE + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P14-D03 | T07-P14: 로그 확대 | T07-P14: debug를 위해 payload와 context 기록을 늘린다 | T07-P14: secret·PII·cardinality 비용을 통제하는지 | T07-P14: REPLAY[key=cmd-014-02; attempts=4; response_seen=0] | T07-P14: IDEMPOTENCY_RECORD + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P14-D04 | T07-P14: 강제 종료 | T07-P14: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P14: in-flight request와 background work의 결과를 잃는지 | T07-P14: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P14: CLIENT_VERSION + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P14-D05 | T07-P14: AI package 추가 | T07-P14: AI가 제안한 새 dependency를 도입한다 | T07-P14: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P14: UNKNOWN_OUTCOME[timeout_ms=138; provider_state=UNKNOWN; lookup_id=p01404] | T07-P14: PROVIDER_RESULT + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P14-D06 | T07-P14: 비동기화 | T07-P14: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P14: durability·status API·worker retry 계약이 생기는지 | T07-P14: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P14: DURABLE_STATE + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P14-D07 | T07-P14: 권한 shortcut | T07-P14: payload의 owner/tenant id를 바로 사용한다 | T07-P14: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P14: OVERLOAD[rps=380; p99_ms=552; queue=24] | T07-P14: QUEUE_PRESSURE + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P14-D08 | T07-P14: 순서 병렬화 | T07-P14: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P14: 선후관계 invariant와 race를 깨지 않는지 | T07-P14: SAMPLING[sample_rate=51%; trace_present=1; metric_present=1] | T07-P14: TRACE_METRIC_CROSSCHECK + T07-P14/CH08 | T07-P14: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P14: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · API version과 호환성은 기존 client를 깨지 않는 약속 — 최종 contract와 evidence spine

**최종 계약:** 서버 코드를 바꾸는 것과 이미 배포된 API 계약을 바꾸는 것은 다른 문제다.

**정상 메커니즘:** 필드 추가·삭제·의미 변경·enum 확장 같은 변화가 기존 client의 가정을 깨는지 판단하고 versioning/deprecation 전략을 적용한다.

**대표 실패:** 서버와 client를 동시에 배포할 수 있다고 가정해 기존 앱이 갑자기 실패한다.

**검증 evidence:** 실제 client version 분포, deprecated field 사용량, schema diff와 contract test 결과를 확인한다.

**직접 행동:** 이전 응답과 새 응답의 key 집합을 비교해 breaking change 후보를 찾는다.

**다음 연결:** `BLOCK 01 종합: 하나의 요청 수명주기를 끝까지 추적하기`.

`API version과 호환성은 기존 client를 깨지 않는 약속`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| NODE-DOCS | Node.js Documentation | API version과 호환성은 기존 client를 깨지 않는 약속의 개념·실패·운영 판단 교차 확인 |
| EXPRESS5 | Express 5 Documentation | API version과 호환성은 기존 client를 깨지 않는 약속의 개념·실패·운영 판단 교차 확인 |
| RFC9110 | RFC9110 | API version과 호환성은 기존 client를 깨지 않는 약속의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | API version과 호환성은 기존 client를 깨지 않는 약속의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | API version과 호환성은 기존 client를 깨지 않는 약속의 개념·실패·운영 판단 교차 확인 |
| RFC9457 | Problem Details for HTTP APIs | API version과 호환성은 기존 client를 깨지 않는 약속의 개념·실패·운영 판단 교차 확인 |

`API version과 호환성은 기존 client를 깨지 않는 약속` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
