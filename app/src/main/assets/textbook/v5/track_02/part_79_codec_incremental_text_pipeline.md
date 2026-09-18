# PART 79 · Codec와 incremental text pipeline — bytes/text 경계를 stream 상태와 함께 다루기

Text 처리 오류의 상당수는 문자열 자체가 아니라 bytes와 text가 만나는 경계에서 생긴다. 한 번에 전체 payload를 decode하면 단순하지만 network stream이나 대용량 file에서는 multibyte character가 chunk 사이에 나뉠 수 있다. Python codec model을 이해하면 encoding 이름, incremental decoder state, error handler를 분리해 볼 수 있다. 핵심은 **decode를 한 번의 함수 호출이 아니라 입력 stream을 text state로 바꾸는 protocol**로 보는 것이다.

---

## CHAPTER 01 · text와 bytes는 의미 층이 다르므로 경계를 명시한다

### 시작 전 용어집

#### 1. text

- **뜻:** Python `str`은 text를 나타내고 `bytes`는 0~255 정수의 sequence다.
- **왜 중요한가:** Encoding 이름을 생략해 환경 default에 의존하면 다른 machine이나 file format에서 결과가 달라질 수 있다.
- **예시:** text = "한글" / raw = text.encode("utf-8")

#### 2. bytes

- **뜻:** Protocol, file format, API contract가 encoding을 규정한다면 code에도 명시한다.
- **왜 중요한가:** Debug representation과 실제 text conversion을 구분한다.
- **예시:** text = "한글" / raw = text.encode("utf-8")

Encoding은 이 둘 사이의 규칙이다.

```python
text = "한글"
raw = text.encode("utf-8")
restored = raw.decode("utf-8")
```

 

`str(raw)`는 decode가 아니다. 

---

## CHAPTER 02 · codec lookup은 encoding alias를 실제 encoder/decoder implementation과 연결한다

### 시작 전 용어집

#### 1. codec

- **뜻:** Encoding name은 사람이 쓰는 label이고 codec registry는 이를 실제 implementation과 연결한다.
- **왜 중요한가:** `utf-8`, alias spelling 등은 normalization을 거칠 수 있다.
- **예시:** Encoding name은 사람이 쓰는 label이고 codec registry는 이를 …

#### 2. encoding alias

- **뜻:** Application에서 user가 임의 codec 이름을 선택하게 한다면 허용 범위를 제한할 필요가 있을 수 있다.
- **왜 중요한가:** 모든 codec이 같은 error behavior나 security property를 갖는 것은 아니다.
- **예시:** Application에서 user가 임의 codec 이름을 선택하게 한다면 허용 …

#### 3. encoder

- **뜻:** 내부 data format이 항상 UTF-8이라면 configuration option으로 불필요하게 codec surface를 넓히지 않는다.
- **예시:** 내부 data format이 항상 UTF-8이라면 configuration option으로 불필요하게 …

필요한 variability만 API로 노출한다.

---

## CHAPTER 03 · incremental decoder는 chunk boundary를 문자 boundary로 착각하지 않는다

### 시작 전 용어집

#### 1. incremental decoder

- **뜻:** Incremental decoder는 남은 partial bytes를 내부 state에 보존했다가 다음 chunk와 합쳐 처리한다.
- **왜 중요한가:** Stream parser는 network read boundary와 logical character boundary가 다르다는 사실을 전제로 해야 한다.
- **예시:** import codecs / decoder = codecs.getincrementaldecoder("utf-8")()

#### 2. chunk boundary

- **뜻:** UTF-8 같은 variable-length encoding에서는 한 character의 byte sequence가 두 chunk로 나뉠 수 있다.
- **왜 중요한가:** 각 chunk를 독립적으로 `decode()`하면 첫 chunk가 incomplete sequence로 끝날 수 있다.
- **예시:** import codecs / decoder = codecs.getincrementaldecoder("utf-8")()

#### 3. bytes

- **뜻:** 같은 원리는 line framing과 message framing에서도 반복된다.
- **예시:** import codecs / decoder = codecs.getincrementaldecoder("utf-8")()

```python
import codecs

decoder = codecs.getincrementaldecoder("utf-8")()
text1 = decoder.decode(chunk1)
text2 = decoder.decode(chunk2)
last = decoder.decode(b"", final=True)
```

 

---

## CHAPTER 04 · error handler는 잘못된 byte sequence를 어떻게 해석할지 결정한다

### 시작 전 용어집

#### 1. error handler

- **뜻:** Decode error policy에는 strict failure, replacement, ignore 등 서로 다른 선택이 있다.
- **왜 중요한가:** `errors="ignore"`는 문제 byte를 조용히 버려 data loss를 만들 수 있다.
- **예시:** Decode error policy에는 strict failure, replacement, ignore 등 …

#### 2. byte sequence

- **뜻:** 후자에서 replacement나 ignore를 쓰면 서로 다른 byte sequence가 같은 text처럼 보일 수 있어 위험하다.
- **왜 중요한가:** 오류 정책은 편의 옵션이 아니라 data integrity 계약이다.
- **예시:** 후자에서 replacement나 ignore를 쓰면 서로 다른 byte sequence가 …

#### 3. 오류

- **뜻:** 사용자 표시용 best-effort text와 cryptographic signature input, identifier parsing은 요구가 다르다.
- **왜 중요한가:** Strict가 기본이고 loss가 허용되는 boundary에서만 명시적으로 완화한다.
- **예시:** 사용자 표시용 best-effort text와 cryptographic signature input, identifier …

---

## CHAPTER 05 · stream wrapper는 buffering과 decoding을 함께 가져갈 수 있다

### 시작 전 용어집

#### 1. stream

- **뜻:** Text file object는 underlying byte stream 위에 buffering과 codec state를 결합한다.
- **왜 중요한가:** `open(path, encoding="utf-8")` 같은 API가 편리한 이유다.
- **예시:** Wrapper를 여러 겹 만들면 buffer가 앞서 읽은 bytes …

#### 2. buffer

- **뜻:** Wrapper를 여러 겹 만들면 buffer가 앞서 읽은 bytes 때문에 예상 위치가 달라질 수 있다.
- **왜 중요한가:** File position도 text mode에서 byte offset과 단순히 같은 의미가 아닐 수 있다.
- **예시:** Wrapper를 여러 겹 만들면 buffer가 앞서 읽은 bytes …

#### 3. decoding

- **뜻:** 하지만 binary protocol header를 먼저 읽고 나머지만 text로 해석해야 한다면 raw byte stream과 text wrapper의 위치를 신중히 정한다.
- **왜 중요한가:** Random seek가 필요한 binary format에서는 byte layer에서 boundary를 관리한다.
- **예시:** 하지만 binary protocol header를 먼저 읽고 나머지만 text로 …

---

## CHAPTER 06 · BOM과 decoder state는 stream 시작 부분의 특별한 해석을 만든다

### 시작 전 용어집

#### 1. BOM

- **뜻:** 일부 encoding은 BOM(Byte Order Mark)을 사용하거나 파일 시작에서 encoding variant를 식별할 수 있다.
- **왜 중요한가:** UTF-8 BOM을 일반 character로 처리할지 signature로 소비할지는 codec 선택에 따라 달라질 수 있다.
- **예시:** 일부 encoding은 BOM(Byte Order Mark)을 사용하거나 파일 시작에서 …

#### 2. decoder state

- **뜻:** CSV header 첫 field 앞에 보이지 않는 BOM character가 붙어 key match가 실패하는 문제는 흔하다.
- **왜 중요한가:** Data source format을 확인하고 적절한 codec을 선택한다.
- **예시:** CSV header 첫 field 앞에 보이지 않는 BOM …

#### 3. stream

- **뜻:** Chunked stream에서 BOM detection을 직접 구현한다면 첫 chunk가 매우 짧을 수 있다는 것도 고려한다.
- **왜 중요한가:** 시작 marker를 완전히 받을 때까지 state를 유지해야 한다.
- **예시:** Chunked stream에서 BOM detection을 직접 구현한다면 첫 chunk가 …

---

## CHAPTER 07 · lossy recovery는 복구된 text와 원본 bytes의 관계를 기록해야 한다

### 시작 전 용어집

#### 1. lossy recovery

- **뜻:** Lossy recovery 후 값을 key나 signature input으로 사용하지 않는다.
- **왜 중요한가:** 사람에게 보여주는 diagnostic view와 authoritative data를 분리한다.
- **예시:** Lossy recovery 후 값을 key나 signature input으로 사용하지 …

#### 2. text

- **뜻:** Data migration에서는 복구 text와 raw input을 별도 field로 두는 것도 방법이다.
- **왜 중요한가:** 깨진 log나 외부 legacy file을 어떻게든 읽어야 할 때 replacement character를 사용한 복구가 필요할 수 있다.
- **예시:** Data migration에서는 복구 text와 raw input을 별도 field로 …

#### 3. bytes

- **뜻:** 가능하면 decode error count, byte offset, source ID를 함께 기록하고 원본 bytes를 보존한다.
- **왜 중요한가:** 이때 복구된 문자열을 “원본을 정확히 decode했다”고 취급하면 안 된다.
- **예시:** 가능하면 decode error count, byte offset, source ID를 …

---

## CHAPTER 08 · codec contract는 encoding·chunk state·error policy를 하나로 고정한다

### 시작 전 용어집

#### 1. codec

- **뜻:** Text pipeline을 설계할 때는 encoding 이름 하나만 정하는 것으로 끝나지 않는다.
- **왜 중요한가:** Input이 complete buffer인지 stream인지, chunk boundary를 누가 관리하는지, malformed sequence에서 실패할지 복구할지, final flush를 언제 호출할지 정의한다.
- **예시:** Text pipeline을 설계할 때는 encoding 이름 하나만 정하는 …

#### 2. encoding

- **뜻:** 이 PART의 핵심은 **encoding을 단순한 문자열 변환 옵션으로 보지 않고, bytes stream에서 text를 복원하는 stateful protocol로 이해해 chunk와 오류 경계를 명시하는 것**이다.
- **왜 중요한가:** 테스트에는 multibyte character를 모든 가능한 byte 위치에서 쪼갠 chunk, invalid byte, BOM, empty final chunk를 포함한다.
- **예시:** 이 PART의 핵심은 **encoding을 단순한 문자열 변환 옵션으로 …

#### 3. chunk state

- **뜻:** Stream decoder는 정상 문장 하나보다 boundary fuzz test가 더 강하다.
- **예시:** Stream decoder는 정상 문장 하나보다 boundary fuzz test가 …

---

## 실전 학습 루프 · incremental codec pipeline

### 1. 쉬운 예

네트워크에서 UTF-8 문자가 byte 경계 중간에서 끊겨 도착할 수 있다. chunk마다 독립적으로 decode하면 멀쩡한 문자가 오류가 될 수 있으므로 incremental decoder는 미완성 byte를 다음 chunk까지 보존한다.

### 2. 한 줄 해석

stream text 처리는 “각 chunk decode”가 아니라 decoder state를 포함한 연속 변환이다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
import codecs

d = codecs.getincrementaldecoder('utf-8')()
raw = '한'.encode('utf-8')
print(d.decode(raw[:1]))
print(d.decode(raw[1:], final=True))
```

### 4. 수정 실습

1. `final=True`를 빼면 stream 종료 시 남은 불완전 byte가 어떻게 처리되는지 확인한다.
2. errors='replace'와 strict 정책이 데이터 품질에 미치는 차이를 비교한다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

UTF-8 byte를 임의 위치에서 자른 뒤 각 조각을 따로 decode해도 항상 안전할까?

### 6. 정답과 오답 설명

**정답:** 아니다. multi-byte sequence가 chunk 사이에 걸칠 수 있어 decoder state가 필요하다.

**자주 나오는 오답:** chunk 경계와 문자 경계를 동일시하면 정상 입력을 invalid로 오판할 수 있다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · incremental codec

### 증상에서 시작한다

정상 UTF-8 stream인데 chunk 크기에 따라 decode 오류가 나거나 마지막 문자가 사라진다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

각 chunk raw hex, decoder 내부 pending bytes, final flag, error policy를 기록한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

한글·이모지 byte를 모든 가능한 위치에서 둘로 잘라 동일한 최종 문자열이 나오는지 검사한다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

chunk 경계와 무관하게 같은 byte stream은 같은 text를 만들고, 종료 시 미완성 sequence는 정책대로 명시적으로 처리돼야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

