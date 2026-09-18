# PART 95 · Digest와 content identity — hash·integrity·authenticity를 서로 다른 보장으로 구분하기

`hashlib`로 digest를 계산하면 file 비교, cache key, content-addressed storage 같은 기능을 만들 수 있다. 그러나 digest가 같다는 사실을 “누가 만들었는지 신뢰할 수 있다”는 인증과 동일시하면 안 된다. 또한 알고리즘 선택, canonical input, collision 요구 수준에 따라 같은 hash라는 단어의 의미가 달라진다. 이 PART에서는 **입력 bytes → digest → 비교 목적 → 보안 보장 수준**을 분리해서 본다.

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

## CHAPTER 08 · digest contract는 입력 bytes와 기대 보장 수준을 먼저 고정한다

### 시작 전 용어집

#### 1. digest

- **뜻:** Digest API는 무엇을 hash하는지, canonicalization이 필요한지, algorithm/version이 무엇인지, collision이 발생했을 때 어떤 위험이 있는지, reference digest를 어디서 신뢰하는지 명시해야 한다.
- **왜 중요한가:** 테스트에서는 chunk size가 다른 streaming hash, file mutation, canonicalization version 변화, 잘못된 digest, shortened identifier collision budget을 확인한다.
- **예시:** Digest API는 무엇을 hash하는지, canonicalization이 필요한지, algorithm/version이 무엇인지, …

#### 2. bytes

- **뜻:** 이 PART의 핵심은 **digest를 만능 보안 도장으로 보지 않고, 정확히 정의된 byte sequence의 compact identity로 사용하면서 integrity·authenticity·uniqueness 요구를 별도 계층으로 설계하는 것**이다.
- **예시:** 이 PART의 핵심은 **digest를 만능 보안 도장으로 보지 …
