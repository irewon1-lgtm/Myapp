# PART 95 · Digest와 content identity — hash·integrity·authenticity를 서로 다른 보장으로 구분하기

`hashlib`로 digest를 계산하면 file 비교, cache key, content-addressed storage 같은 기능을 만들 수 있다. 그러나 digest가 같다는 사실을 “누가 만들었는지 신뢰할 수 있다”는 인증과 동일시하면 안 된다. 또한 알고리즘 선택, canonical input, collision 요구 수준에 따라 같은 hash라는 단어의 의미가 달라진다. 이 절에서는 **입력 bytes → digest → 비교 목적 → 보안 보장 수준**을 분리해서 본다.

---

## CHAPTER 01 · digest는 임의 길이 입력을 고정 길이 식별값으로 투영한다

### 시작 전 용어집

#### 1. digest

- **뜻:** Digest는 원본 내용을 짧은 값으로 대표하지만 원본 자체가 아니다.
- **왜 중요한가:** 가능한 입력 공간이 digest 공간보다 크므로 이론적으로 collision은 존재한다.
- **예시:** import hashlib / digest = hashlib.sha256(data).hexdigest()

#### 2. hash

- **뜻:** Python의 일반 `hash()`와 cryptographic digest도 구분한다.
- **왜 중요한가:** `hash()`는 process 내부 dict/set 등에 적합한 hash semantics이고 안정된 파일 fingerprint나 외부 protocol identity로 사용할 목적이 아니다.
- **예시:** import hashlib / digest = hashlib.sha256(data).hexdigest()

```python
import hashlib

digest = hashlib.sha256(data).hexdigest()
```

  중요한 것은 사용 목적에서 collision을 찾기 충분히 어렵고 알고리즘이 현재 요구에 적합한가다.

 

---

**운영 검증 95-1 — CHAPTER 01 · digest는 임의 길이 입력을 고정 길이 식별값으로 투영한다**
CHAPTER 01 · digest는 임의 길이 입력을 고정 길이 식별값으로 투영한다을 검증할 때는 기대 상태를 먼저 적고 응답 지연을 일부러 만든다. PART 95 CHAPTER 1의 핵심 관찰값으로 체크포인트와 저장된 데이터의 위치를 맞춘다. P95-C01의 실패 주입 결과는 다음 정상 실행과 연결해서 본다. 같은 입력을 다시 처리할 때 P95-C01의 상태 머신이 이전 오류를 끌고 가지 않고 정해진 완료 상태로 돌아오는지 확인한다. P95-C01: 수정 전후는 처리 건수와 재시도 횟수를 분리해 비교한다. 같은 입력을 세 번 반복했을 때 최종 반영 수가 변하지 않고 실패 이유가 동일한 식별자로 남으면 검증을 통과한다.
## CHAPTER 02 · 큰 파일은 전체를 memory에 올리지 않고 streaming hash로 계산한다

### 시작 전 용어집

#### 1. memory

- **뜻:** Digest algorithm은 chunk 경계를 application message처럼 취급하지 않는다.
- **왜 중요한가:** 같은 byte sequence를 같은 순서로 update하면 chunk size가 달라도 같은 digest를 얻는다.
- **예시:** h = hashlib.sha256() / with open(path, "rb") as …

#### 2. stream

- **뜻:** 이 특성은 대용량 artifact 검증에 유용하다.
- **왜 중요한가:** 다만 hashing 중 file이 동시에 변경되면 읽은 byte sequence가 어느 한 시점의 완전한 snapshot이 아닐 수 있다.
- **예시:** h = hashlib.sha256() / with open(path, "rb") as …

#### 3. hash

- **뜻:** Stable file handle, size/metadata 재검사, immutable artifact storage를 함께 고려한다.
- **예시:** h = hashlib.sha256() / with open(path, "rb") as …

```python
h = hashlib.sha256()
with open(path, "rb") as f:
    while chunk := f.read(1024 * 1024):
        h.update(chunk)
print(h.hexdigest())
```

 

  

---

**운영 검증 95-2 — CHAPTER 02 · 큰 파일은 전체를 memory에 올리지 않고 streaming hash로 계산한다**
CHAPTER 02 · 큰 파일은 전체를 memory에 올리지 않고 streaming hash로 계산한다을 검증할 때는 부분 성공을 구분할 상태 값을 정하고 중복 요청을 한 번 넣는다. PART 95 CHAPTER 2의 핵심 관찰값으로 재시도 로그와 실제 반영 건수를 비교한다. P95-C02 장애 주입 뒤에는 같은 입력을 다시 넣어 P95-C02의 중복 처리 여부와 복구 시작점을 함께 비교한다. 재실행이 이전 성공분을 다시 반영하지 않고 남은 작업만 이어지면 상태 일관성이 유지된 것이다. P95-C02: 변경 효과는 성공 플래그보다 중복 반영 수와 미처리 항목 수로 판단한다. 재실행 뒤 누락이 줄고 이미 완료된 항목이 다시 처리되지 않으며 종료 상태가 명확해야 한다.
## CHAPTER 03 · collision resistance 요구는 checksum과 security-sensitive identity에서 다르다

### 시작 전 용어집

#### 1. collision resistance

- **뜻:** 전송 오류를 우연히 감지하려는 checksum과 공격자가 의도적으로 같은 digest를 만들지 못하게 해야 하는 cryptographic hash는 threat model이 다르다.
- **왜 중요한가:** Legacy algorithm이 빠르고 짧다는 이유로 security-sensitive content identity에 계속 사용하면 collision attack에 취약할 수 있다.
- **예시:** 전송 오류를 우연히 감지하려는 checksum과 공격자가 의도적으로 같은 …

#### 2. checksum

- **뜻:** 새로운 설계에서는 현재 권고되는 cryptographic algorithm을 선택하고 algorithm name을 metadata에 포함한다.
- **왜 중요한가:** Digest length를 임의로 짧게 잘라 UI ID로 사용할 때도 birthday collision 확률을 계산해야 한다.
- **예시:** 새로운 설계에서는 현재 권고되는 cryptographic algorithm을 선택하고 algorithm …

#### 3. security-sensitive identity

- **뜻:** 전체 digest의 보안 수준과 8글자 prefix의 uniqueness 수준은 같지 않다.
- **예시:** 전체 digest의 보안 수준과 8글자 prefix의 uniqueness 수준은 …

---

## CHAPTER 04 · digest는 무결성 검출 수단일 수 있지만 authenticity를 혼자 증명하지 못한다

### 시작 전 용어집

#### 1. digest

- **뜻:** 공격자가 파일을 바꿀 수 있고 digest 값도 같이 바꿀 수 있다면 검사는 아무 보호를 제공하지 못한다.
- **왜 중요한가:** Authenticity가 필요하면 신뢰된 channel, digital signature, MAC처럼 digest와 secret/key 기반 검증을 결합해야 한다.
- **예시:** 공격자가 파일을 바꿀 수 있고 digest 값도 같이 …

#### 2. authenticity

- **뜻:** 파일과 함께 SHA-256 값이 같은 서버에서 전달됐다고 하자.
- **왜 중요한가:** Digest는 “이 bytes가 기대한 bytes와 같은가”를 비교하는 재료이지 “이 bytes가 신뢰된 주체에게서 왔다”를 자동 증명하지 않는다.
- **예시:** 파일과 함께 SHA-256 값이 같은 서버에서 전달됐다고 하자.

#### 3. 검증

- **뜻:** 배포 artifact 검증에서는 digest가 어디서 왔고 그 reference value를 누가 신뢰하는지도 threat model에 포함한다.
- **예시:** 배포 artifact 검증에서는 digest가 어디서 왔고 그 reference …

---

**운영 검증 95-4 — CHAPTER 04 · digest는 무결성 검출 수단일 수 있지만 authenticity를 혼자 증명하지 못한다**
CHAPTER 04 · digest는 무결성 검출 수단일 수 있지만 authenticity를 혼자 증명하지 못한다을 검증할 때는 중복 여부를 판정할 키를 먼저 확인하고 복구 직후 같은 요청을 재전송한다. PART 95 CHAPTER 4의 핵심 관찰값으로 최종 상태와 처리 횟수를 함께 확인한다. P95-C04 실패 후 재처리는 멱등성 확인 단계다. P95-C04의 처리 횟수·최종 상태·재시도 로그를 함께 비교해 같은 입력이 여러 번 와도 데이터 의미가 변하지 않는지 검증한다. P95-C04: 회귀 확인에서는 정상 실행과 장애 실행의 상태 전이 수를 각각 기록한다. 재시도 상한 안에서 종료되고 남은 작업이 추적 가능하며 데이터 개수가 기준 실행과 같아야 한다.
## CHAPTER 05 · content-addressed identity는 내용이 같으면 같은 key가 된다는 성질을 사용한다

### 시작 전 용어집

#### 1. content-addressed identity

- **뜻:** Artifact cache나 blob store는 content digest를 key로 사용해 동일 bytes를 deduplicate할 수 있다.
- **왜 중요한가:** 이 구조에서는 blob이 digest key 아래 저장된 뒤 내용이 변하면 안 된다.
- **예시:** sha256:<digest> -> immutable blob

#### 2. key

- **뜻:** Mutable file path를 같은 content ID 아래 덮어쓰면 identity invariant가 깨진다.
- **왜 중요한가:** Metadata와 content를 분리하면 filename이나 upload time이 달라도 같은 blob을 공유할 수 있다.
- **예시:** sha256:<digest> -> immutable blob

#### 3. digest

- **뜻:** 반대로 content-equivalent하지만 serialization byte가 다른 JSON은 서로 다른 digest가 되므로 P94의 canonicalization 문제와 연결된다.
- **예시:** sha256:<digest> -> immutable blob

```text
sha256:<digest> -> immutable blob
```

 

 

---

**운영 검증 95-5 — CHAPTER 05 · content-addressed identity는 내용이 같으면 같은 key가 된다는 성질을 사용한다**
CHAPTER 05 · content-addressed identity는 내용이 같으면 같은 key가 된다는 성질을 사용한다을 검증할 때는 재시작 전후를 비교할 값을 고르고 부분 배치를 실패시킨다. PART 95 CHAPTER 5의 핵심 관찰값으로 실패 원인과 복구 결과가 같은 식별자로 이어지는지 확인한다. P95-C05 장애 테스트가 끝나면 초기 상태를 복원하지 않고 바로 재실행해 복구성을 확인한다. P95-C05이 남은 상태를 정확히 읽고 중복·누락 없이 종료되면 재시도 계약이 지켜진 것이다. P95-C05: 수정 전후 비교표에는 성공 건수, 실패 건수, 재시도 수, 최종 대기열 길이를 넣는다. 반복 실행에서 이 값들이 수렴하고 손실이나 무한 재시도가 나타나지 않아야 한다.
## CHAPTER 06 · secret-derived digest를 비교할 때는 timing behavior도 고려한다

### 시작 전 용어집

#### 1. secret-derived digest

- **뜻:** Authentication token이나 MAC처럼 secret과 관련된 값을 일반 문자열 `==`로 비교하면 일부 환경에서 비교 시간 차이가 정보 누출 surface가 될 수 있다.
- **왜 중요한가:** Python은 constant-time comparison을 위한 helper를 제공하는 영역이 있다.
- **예시:** Authentication token이나 MAC처럼 secret과 관련된 값을 일반 문자열 …

#### 2. timing behavior

- **뜻:** 하지만 timing-safe comparison 함수 하나를 썼다고 전체 authentication protocol이 안전해지는 것은 아니다.
- **왜 중요한가:** 입력 길이, error message, network latency, key management 등 다른 side channel이 남을 수 있다.
- **예시:** 하지만 timing-safe comparison 함수 하나를 썼다고 전체 authentication …

Security-sensitive comparison에서는 표준 cryptographic construction과 library API를 사용하고 직접 비교 protocol을 설계하지 않는다.

---

## CHAPTER 07 · algorithm과 representation version을 identity에 포함하면 migration이 가능해진다

### 시작 전 용어집

#### 1. algorithm

- **뜻:** ` 같은 digest 문자열만 저장하면 나중에 어떤 algorithm과 canonicalization을 사용했는지 알기 어렵다.
- **왜 중요한가:** 처럼 algorithm과 content-serialization version을 별도 metadata로 보존하면 새 algorithm이나 schema로 migration하기 쉽다.
- **예시:** sha256:v1:<hex>

#### 2. representation version

- **뜻:** Database unique key가 digest 하나에 묶여 있다면 algorithm 변경 시 dual-read/dual-write migration이 필요할 수 있다.
- **예시:** sha256:v1:<hex>

`abcdef...

```text
sha256:v1:<hex>
```


 현재 선택을 영구 불변으로 가정하지 않는다.

---

**운영 검증 95-7 — CHAPTER 07 · algorithm과 representation version을 identity에 포함하면 migration이 가능해진다**
CHAPTER 07 · algorithm과 representation version을 identity에 포함하면 migration이 가능해진다을 검증할 때는 성공 기준을 수치나 상태로 정하고 처리 중간에 프로세스를 중단한다. PART 95 CHAPTER 7의 핵심 관찰값으로 대기열 길이와 거부된 요청 수를 같이 본다. P95-C07 장애 주입 뒤에는 같은 입력을 다시 넣어 P95-C07의 중복 처리 여부와 복구 시작점을 함께 비교한다. 재실행이 이전 성공분을 다시 반영하지 않고 남은 작업만 이어지면 상태 일관성이 유지된 것이다. P95-C07: 통과 여부는 첫 성공이 아니라 반복 재현으로 결정한다. 같은 입력을 다시 처리해도 최종 결과가 동일하고 실패 항목의 원인이 로그와 상태 값으로 연결되어야 한다.
## CHAPTER 08 · digest contract는 입력 bytes와 기대 보장 수준을 먼저 고정한다

### 시작 전 용어집

#### 1. digest

- **뜻:** Digest API는 무엇을 hash하는지, canonicalization이 필요한지, algorithm/version이 무엇인지, collision이 발생했을 때 어떤 위험이 있는지, reference digest를 어디서 신뢰하는지 명시해야 한다.
- **왜 중요한가:** 테스트에서는 chunk size가 다른 streaming hash, file mutation, canonicalization version 변화, 잘못된 digest, shortened identifier collision budget을 확인한다.
- **예시:** Digest API는 무엇을 hash하는지, canonicalization이 필요한지, algorithm/version이 무엇인지, …

#### 2. bytes

- **뜻:** 이 PART의 핵심은 **digest를 만능 보안 도장으로 보지 않고, 정확히 정의된 byte sequence의 compact identity로 사용하면서 integrity·authenticity·uniqueness 요구를 별도 계층으로 설계하는 것**이다.
- **예시:** 이 PART의 핵심은 **digest를 만능 보안 도장으로 보지 …

---

## 실전 학습 루프 · digest·integrity·identity

### 1. 쉬운 예

파일을 SHA-256으로 hash해 같으면 동일한 byte sequence라고 강하게 추정할 수 있지만, digest는 “누가 만들었는가”를 인증하지 않는다. 무결성 식별자와 authentication/signature 역할을 분리해야 한다.

### 2. 한 줄 해석

digest는 내용의 byte identity를 요약하는 값이고, 신뢰 주체의 identity는 별도의 key·signature·access-control 계약이 필요하다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
import hashlib

data = b'payload-v1'
print(hashlib.sha256(data).hexdigest())
```

### 4. 수정 실습

1. 파일 이름 대신 content digest를 cache key로 사용할 때 collision·algorithm migration 정책을 적는다.
2. 공격자가 파일과 digest를 함께 바꿀 수 있는 상황에서 단순 checksum이 왜 신뢰 근거가 아닌지 설명한다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

SHA-256 digest가 일치하면 그 파일이 신뢰할 수 있는 제작자에게서 왔다고 증명할까?

### 6. 정답과 오답 설명

**정답:** 아니다. digest는 내용 동일성/무결성 확인에 쓰일 수 있지만 발신자 인증은 signature나 신뢰된 전달 경로가 필요하다.

**자주 나오는 오답:** hash, MAC, digital signature를 모두 “암호화”라고 부르면 보장하는 성질이 섞인다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.

## 현장 디버깅 체크 · digest·integrity identity

### 증상에서 시작한다

download 검증은 통과했는데 공격자가 바꾼 파일도 정상으로 인정되거나, algorithm 변경 뒤 cache key가 충돌한다. 먼저 재현 가능한 최소 payload와 operation id를 고정한다. 최종 상태만 고치면 중복·정밀도·복구 문제의 실제 발생 지점을 숨길 수 있다.

### 먼저 볼 증거

digest algorithm, raw byte length, expected digest의 신뢰 출처, signature/MAC 여부와 content provenance를 확인한다. 가능하면 이 값을 하나의 trace 또는 audit record로 묶어 시간 순서를 복원한다.

### 일부러 실패시켜 보기

파일과 checksum을 함께 변조하는 경우와 trusted manifest의 checksum만 고정한 경우를 비교한다. 이런 반례가 자동 테스트에 들어가야 정상 예제만 통과하는 구현을 걸러낼 수 있다.

### 통과 기준

내용 동일성 검증과 제작자/배포자 인증이 각각 독립된 신뢰 근거로 설명 가능해야 한다. 통과 기준은 “에러가 안 난다”가 아니라 **어떤 입력과 실패 순서에서도 허용된 상태 집합을 벗어나지 않는다**로 적는다.

