# 9장 실전 훈련편 — Android 권한·알림·백그라운드·동기화

이 훈련편은 Android 용어를 다시 설명하지 않는다. 같은 앱이 **기기·권한·채널·token·백그라운드 상태**에 따라 다르게 동작하는 실제 상황을 분석한다.

---

## 훈련 A. 알림 전체 경로 그리기

다음 항목을 올바른 흐름으로 배치한다.

```text
notification channel
서버 이벤트
push provider
device token
기기 OS
앱 notification permission
화면 표시
```

한 가지 가능한 순서:

```text
서버 이벤트
→ provider 요청
→ device token 대상
→ 기기 OS 수신
→ permission/channel 정책
→ 화면 표시
```

실제 구현은 더 복잡할 수 있지만, 이 흐름을 기준으로 “어디까지 정상인지” 증거를 붙인다.

---

## 훈련 B. 한 기기만 실패하는 비교표

정상 기기와 실패 기기:

| 항목 | 엄마 | 배우자 |
|---|---|---|
| 앱 버전 | 1.5 | 1.5 |
| Android | 16 | 16 |
| notification permission | ON | ON |
| 일정 channel | ON | OFF |
| token 등록 시각 | 오늘 | 오늘 |
| provider accepted | 예 | 예 |

가장 강한 후보는 일정 channel이다.

서버를 재배포하기 전에 실패 기기의 channel을 확인한다.

---

## 훈련 C. token stale 판정

로그:

```text
wife current token = NEW123
server stored token = OLD999
provider response for OLD999 = invalid registration
```

원인 후보가 매우 강하다.

필요한 수정 방향:

- token 갱신 이벤트 처리
- 서버 등록 upsert
- invalid token 정리
- 재설치/재로그인 경로 테스트

---

## 훈련 D. 앱 재설치가 증거를 없애는 경우

알림 문제가 있을 때 바로 재설치하면:

- token이 바뀔 수 있음
- 앱 내부 설정이 초기화됨
- local DB가 사라질 수 있음
- permission 상태가 다시 설정될 수 있음

문제가 사라져도 **무엇이 원인이었는지 모를 수 있다.**

재설치 전 증거:

```text
앱 버전
현재 token
channel 상태
permission 상태
battery restriction
최근 provider response
계정/기기 mapping
```

---

## 훈련 E. runtime permission 거절 처리

기능: 사진 업로드.

상황:

```text
카메라 권한 거절
```

나쁜 앱:

```text
버튼 눌러도 아무 반응 없음
```

더 좋은 처리 후보:

- 왜 권한이 필요한지 설명
- 거절 시 대체 경로 제공
- 설정으로 이동할 필요가 있는 경우 안내
- 기능 자체가 crash하지 않게 처리

권한이 없다는 상태는 정상적인 사용자 선택일 수 있다.

---

## 훈련 F. “권한 ON”인데도 알림이 안 옴

가능한 후보를 최소 5개 적는다.

예:

```text
channel OFF
token stale
서버 recipient 누락
provider 오류
배터리/백그라운드 제한
앱 자체 notification 생성 오류
```

이 훈련의 목적은 권한 하나를 만능 원인으로 보지 않는 것이다.

---

## 훈련 G. WorkManager 작업이 정확히 09:00:00에 실행되지 않음

사용자 요구:

```text
매일 09:00 정확히 실행
```

실제 Android 백그라운드 제약에서는 지연 허용 작업과 정확한 알람 요구가 다르다.

질문:

- “대략 오전 중 동기화”인가?
- “09:00 정각 알림”인가?
- OS가 정확한 alarm 권한/제약을 요구하는가?
- 서버 스케줄 방식이 더 적절한가?

요구사항의 시간 정확도부터 분리한다.

---

## 훈련 H. 배터리 제한 비교 실험

실패 기기에서:

```text
앱 사용 중 수동 동기화 정상
화면 꺼진 뒤 예약 동기화만 실패
```

강한 후보:

- background execution 제한
- battery optimization
- scheduling 조건

다음 실험:

```text
같은 기기에서 화면 켠 상태/꺼진 상태 비교
충전 중/비충전 비교
Wi-Fi/모바일 데이터 비교
```

한 번에 하나의 조건만 바꾼다.

---

## 훈련 I. sync 충돌

기기 A:

```text
10:00 일정 제목 "병원"
```

기기 B 오프라인 수정:

```text
10:05 "프라임내과"
```

기기 A 수정:

```text
10:06 "점심 약속"
```

B가 10:10 온라인 복귀.

어떤 값을 남길지 정책이 필요하다.

가능한 전략:

- server version 기반
- last-write-wins
- 사용자 충돌 해결
- field-level merge

앱 성격에 따라 선택한다. “자동으로 알아서 맞춰짐”은 정책이 아니다.

---

## 훈련 J. sync는 정상인데 데이터 복구 실패

사용자 A가 일정을 삭제했고 서버와 모든 기기에 삭제가 정상 동기화됐다.

다음날 복구 요청.

동기화 시스템만 있다면 삭제 전 버전이 없을 수 있다.

필요한 별도 기능 후보:

- recycle bin
- version history
- backup snapshot
- audit log

`sync ≠ backup`을 실제 복구 사례로 확인한다.

---

## 훈련 K. process death 시나리오

사용자가 작성 중:

```text
병원 메모 10줄 입력
아직 저장 버튼 안 누름
```

앱이 백그라운드로 가고 프로세스가 종료됨.

다시 열면 입력이 사라졌다.

질문:

- 이 내용은 단순 UI state인가?
- 자동 draft 저장이 필요한가?
- 어디에 저장할까?
- 언제 삭제할까?

중요 사용자 입력은 메모리에만 의존하지 않는 설계를 고려한다.

---

## 훈련 L. lifecycle 중복 요청

화면이 resume될 때마다 API를 호출한다.

사용자가:

```text
화면 A → B → A → B → A
```

를 빠르게 반복했더니 같은 API가 5번 호출됐다.

가능한 개선:

- data freshness 확인
- ViewModel/state 보존
- inflight request dedupe
- cache

화면 lifecycle과 데이터 lifecycle을 같은 것으로 보지 않는다.

---

## 훈련 M. notification channel migration

앱 v1에는 channel ID `schedule`이 있었다.

v2에서 개발자가 이름을 바꾸면서 새 ID `calendar`를 만들었다.

사용자는 v1의 `schedule` channel을 꺼둔 상태였다.

v2에서 새 channel이 기본 ON으로 생기면 사용자 의도가 무시될 수 있다.

반대 상황도 가능하다.

채널 변경은 단순 UI 문자열 변경이 아니라 사용자 설정 migration 문제일 수 있다.

---

## 훈련 N. 여러 기기 같은 계정

사용자 한 명이 폰 2대를 쓴다.

서버 데이터 모델이:

```text
user → one token
```

이라면 두 번째 폰 등록 시 첫 번째 token이 덮어써질 수 있다.

더 적절한 구조 후보:

```text
user → devices[] → token per device
```

알림 수신 정책:

- 모든 기기
- 최근 활성 기기만
- 사용자 설정

요구사항을 명시한다.

---

## 훈련 O. 기기 시간에 의존한 버그

기기 시계가 15분 틀려 있다.

앱이 로컬 기기 시간만으로 token 만료나 일정 동기화 cutoff를 판단한다면 오작동할 수 있다.

서버 기준 시각, monotonic clock, timezone 정책이 필요한 지점을 구분한다.

---

## AI 답 검증 1

AI 답:

> 알림 권한이 켜져 있으므로 앱 코드와 서버만 보면 됩니다.

판정: 틀림.

channel, token, provider, battery/background, device mapping 등 다른 단계가 있다.

---

## AI 답 검증 2

AI 답:

> 앱이 백그라운드에서 정확히 매초 실행되도록 while loop를 만들면 됩니다.

판정: 잘못된 Android 운영 모델.

OS 스케줄러/foreground service/서버 trigger 등 적절한 메커니즘과 정책을 써야 한다.

---

## 디버깅 미션 1 — 엄마는 되고 배우자만 안 됨

증거:

```text
server recipients includes wife
wife token current
provider accepted
permission ON
channel OFF
```

root cause 후보와 수정 후 회귀 범위를 적는다.

회귀:

```text
wife channel ON → 수신
mom/dad 계속 수신
작성자 제외 유지
앱 재실행 후 유지
```

---

## 디버깅 미션 2 — 재부팅 후 예약 작업 사라짐

재부팅 전 정상, 재부팅 후 실패.

후보:

- schedule persistence
- boot event 이후 재등록 필요 여부
- OS job scheduler 사용 방식

앱 프로세스 메모리에만 timer를 둔 구조인지 확인한다.

---

## 독립 프로젝트 — 가족 일정 알림 설계서

가족 4명, 사용자마다 여러 기기가 가능하다고 하자.

### 데이터 모델

```text
users
families
memberships
devices
device_tokens
events
notifications
```

### 알림 규칙

```text
일정 추가/수정/삭제
작성자 제외
나머지 가족 구성원 모든 활성 기기 전송
invalid token 제거
중복 event id 방지
```

### 실패 상태

```text
provider reject
permission unknown
channel disabled
offline device
stale token
```

### 관찰 가능성

로그에 필요한 항목:

```text
eventId
recipientUserId
deviceId
token hash/provider id
provider result
sentAt
```

secret token 원문을 로그에 남기지 않는다.

---

# 9장 훈련 완료 기준

- Android 알림 경로를 서버부터 화면까지 단계로 그린다.
- 한 기기 실패에서 정상 비교군을 사용한다.
- token stale과 channel/permission 문제를 구분한다.
- background 실행이 OS 정책 영향을 받음을 안다.
- sync 충돌에는 명시적 정책이 필요함을 설명한다.
- sync와 backup을 실제 삭제 복구 사례로 구분한다.
- process death에 중요한 사용자 입력을 잃지 않도록 설계한다.
- 여러 기기 사용자를 one-token 모델로 처리하지 않는다.
