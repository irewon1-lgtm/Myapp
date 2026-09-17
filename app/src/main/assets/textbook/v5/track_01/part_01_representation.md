# PART 01 · 표현 계약 — 비트열에서 의미가 생기는 조건

메모리와 저장장치에는 값의 뜻이 저장되지 않는다. 저장되는 것은 비트 패턴이고, 뜻은 **폭(width), 부호성, 바이트 순서, 인코딩, 스키마, 단위, 버전, 허용 연산**이 결정한다. 따라서 표현 버그는 데이터가 훼손되지 않아도 발생한다. 송신자와 수신자가 같은 바이트에 다른 계약을 적용하면 서로 다른 값을 얻는다.

---

## CHAPTER 01 · 표현은 `bytes + contract`다

값을 외부 경계로 내보낼 때 최소 계약은 다음 요소를 가져야 한다.

```text
field width
signedness / numeric model
endianness
encoding
schema and field identity
unit / scale
null or absence semantics
version / compatibility rule
valid range and invariants
```

`decode(bytes)`라는 함수는 실제로는 충분하지 않다. 더 정확한 모델은 `decode(bytes, schema, version, policy)`다. 여기서 `policy`에는 invalid input 처리, unknown field 처리, overflow 처리, normalization 규칙처럼 구현 선택이 들어간다.

같은 raw bytes를 서로 다른 타입으로 reinterpret할 수 있다는 사실은 type safety의 출발점이다. 타입은 메모리 위에 붙는 이름이 아니라 **허용되는 값 집합과 연산, 불변조건을 제한하는 계약**이다. 도메인 타입이 강할수록 `문법적으로 가능한 값`과 `업무적으로 가능한 값`의 간격이 줄어든다.

외부 데이터는 세 단계를 통과해야 한다.

```text
bytes/text가 형식적으로 파싱 가능한가
→ schema/type 제약을 만족하는가
→ 도메인 불변조건을 만족하는가
```

이 세 단계를 하나로 합치면 실패 위치가 흐려진다. parser가 성공했다는 사실은 데이터가 유효하다는 증거가 아니다.

계약은 문서에만 적혀 있어서는 부족하다. producer의 encoder, consumer의 decoder, validation, 저장 스키마가 서로 다른 기본값을 가지면 배포 시점이 엇갈리는 순간 동일한 payload가 다른 의미로 해석될 수 있다. 그래서 경계마다 **어떤 버전의 계약으로 어떤 bytes를 해석했는지**가 관측 가능해야 하며, 호환성 테스트는 정상값뿐 아니라 unknown field, 범위 초과, 누락값, 오래된 version을 포함해야 한다. 표현 계약의 실패는 대개 byte 손상보다 해석 규칙의 불일치에서 시작한다.

---

## CHAPTER 02 · 고정 폭 정수는 수학의 정수가 아니다

`n`비트 unsigned 정수는 `2^n`개의 패턴을 표현하고 산술은 고정 폭이라는 경계 안에서 일어난다. signed two's-complement 역시 같은 패턴 수를 사용하지만 해석 범위가 다르다. 비트 패턴 자체에는 signed/unsigned 정보가 없다.

중요한 구분은 다음 네 가지다.

```text
mathematical integer
fixed-width machine integer
language-level integer semantics
serialized integer field
```

이 네 층은 overflow에서 갈라진다. 어떤 언어는 wrapping을 정의하고, 어떤 연산은 checked exception을 발생시키며, 어떤 언어의 일반 정수는 필요한 만큼 확장된다. C/C++의 signed overflow처럼 최적화와 직접 연결되는 규칙도 있다. 따라서 overflow를 CPU 동작 하나로 일반화하지 않는다.

자원 크기를 계산하는 산술은 반드시 overflow 검증 대상이다.

```text
bytes = count * elementSize + headerSize
```

외부 입력이 `count`에 영향을 줄 수 있는데 곱셈·덧셈이 wrap되면 실제 필요량보다 작은 buffer가 할당될 수 있다. 이후 copy loop가 원래 논리적 개수대로 쓰면 memory boundary가 깨진다. 안전한 순서는 **범위 검증 → checked arithmetic → allocation → 실제 처리량 재검증**이다.

바이트 순서는 multi-byte field 계약의 일부다. 값 `V`가 같아도 memory 또는 wire에 놓이는 byte significance 순서는 달라질 수 있다. host-native struct를 wire format으로 사용하면 endianness뿐 아니라 alignment와 padding까지 ABI에 종속된다. 외부 포맷은 native layout과 분리한다.

```text
native representation  ≠  wire representation
```

외부 포맷에는 width, signedness, endian, alignment 여부, 단위, sentinel, versioning을 명시한다. “32-bit integer”만으로는 충분하지 않다.

---

## CHAPTER 03 · 부동소수점은 근사 표현이며 비교 규칙도 다르다

IEEE 754 계열 부동소수점은 부호·지수·유효숫자 구조를 사용해 매우 넓은 범위를 제한된 비트로 표현한다. 모든 실수를 정확히 나타낼 수 없으므로 많은 값은 가장 가까운 representable value로 반올림된다.

문제의 핵심은 `0.1` 같은 유명한 예제가 아니다. 다음 성질들이 시스템 계약에 영향을 준다.

- 연산 순서를 바꾸면 rounding 결과가 달라질 수 있다.
- `NaN`은 일반 값과 같은 equality 규칙을 따르지 않는다.
- `+0`과 `-0`은 비교에서는 같게 보일 수 있지만 일부 연산 결과에서 차이가 드러난다.
- infinity와 subnormal을 허용할지 validation 정책이 필요할 수 있다.
- compiler/runtime의 optimization mode가 strict reproducibility와 충돌할 수 있다.

분산 계산이나 재현 가능한 테스트에서 `같은 수학식이면 같은 bit pattern`이라고 가정하면 안 된다. 재현성이 요구되면 precision, rounding mode, operation order, library/runtime 조건을 계약에 포함한다.

금액·정산·세금처럼 십진 단위 정확성이 핵심인 데이터는 binary floating point를 그대로 업무 단위로 쓰는 설계가 부적절할 수 있다. 가능한 선택은 **최소 화폐 단위 정수**, decimal/fixed-point 타입, 명시적 rounding rule이다. 판단 기준은 타입 이름이 아니라 **어느 단계에서 어떤 반올림이 허용되는가**를 규칙으로 고정하는 것이다.

---

## CHAPTER 04 · Unicode에서 `문자열 길이`는 하나의 개념이 아니다

문자열 시스템에는 적어도 다음 단위가 존재한다.

```text
encoded bytes
code units
Unicode code points / scalar values
extended grapheme clusters
rendered glyphs
```

API의 `length`가 무엇을 세는지 확인하지 않으면 buffer size, cursor 이동, truncation, DB 제한, UI 입력 제한이 서로 다른 단위를 사용하게 된다. 특히 UTF-16 기반 API에서는 supplementary code point가 두 code unit을 사용할 수 있고, 사용자가 한 글자로 인식하는 grapheme cluster는 여러 code point로 구성될 수 있다.

Unicode normalization은 화면상 동등한 텍스트가 서로 다른 code-point sequence로 표현될 수 있다는 문제를 다룬다. NFC/NFD 같은 normalization form은 **동등성 판단의 한 도구**이지 모든 도메인에 무조건 적용할 정답이 아니다. identifier, 파일명, 검색어, 비밀번호, cryptographic input은 서로 다른 정책을 요구할 수 있다.

보안에서는 canonicalization 순서가 중요하다.

```text
raw input
→ decoding
→ normalization / canonicalization
→ validation
→ authorization / lookup
```

서로 다른 계층이 다른 normalization을 적용하면 한 계층에서 허용한 식별자가 다음 계층에서 다른 값으로 해석될 수 있다. Unicode UTS #39가 다루는 confusable·mixed-script 문제도 같은 종류의 경계 위험이다. 식별자 정책은 단순 `Unicode 허용`이 아니라 허용 script, normalization, case policy, confusable 대응까지 설계해야 한다.

invalid UTF-8 처리도 계약이다. decoder가 reject하는지 replacement character로 치환하는지에 따라 후속 validation 결과가 달라질 수 있다. 보안 경계에서는 **같은 raw bytes가 모든 계층에서 같은 문자열로 해석되는지**를 검증해야 한다.

---

## CHAPTER 05 · serialization format과 schema evolution은 분리해서 설계한다

JSON은 구조화 데이터를 표현하는 텍스트 형식이지만 schema 자체를 제공하지 않는다. JSON `number`를 각 언어의 `int64`, `double`, arbitrary precision decimal 중 무엇으로 받을지는 구현 계약이다. 특히 서로 다른 언어를 연결할 때 큰 정수·소수 정밀도·duplicate object member 처리처럼 구현 차이가 interoperability 문제를 만든다.

Protocol Buffers 같은 schema 기반 binary format에서는 wire의 field number와 wire type이 호환성의 핵심이다. field name은 source-level 의미에 가깝고 wire identity는 number에 묶인다. 삭제한 field number를 다른 의미로 재사용하면 과거 데이터나 구버전 peer가 새 의미로 오해할 수 있다. schema evolution은 **추가 가능성**보다 더 엄격하게 다음을 관리해야 한다.

```text
field identity stability
presence semantics
required/optional/default semantics
unknown-field behavior
numeric range changes
oneof/union evolution
removed-field reservation
old-reader / new-writer compatibility
new-reader / old-writer compatibility
```

CBOR처럼 같은 의미 값을 둘 이상의 byte sequence로 표현할 수 있는 형식에서는 `semantic equality`와 `byte equality`를 구분해야 한다. cryptographic signing, hashing, cache key에 raw bytes를 사용한다면 deterministic/canonical encoding 규칙이 별도로 필요하다.

JSON, CBOR, Protobuf 어느 형식을 선택하든 핵심 문제는 동일하다.

```text
내부 객체 모델
→ 외부 schema
→ wire encoding
→ version compatibility
→ validation
```

형식 자체가 호환성을 보장하지 않는다. 호환성은 **field semantics와 evolution discipline**이 보장한다.

---

## CHAPTER 06 · binary parser는 길이와 offset을 신뢰하지 않는다

untrusted binary input을 파싱할 때 가장 먼저 지켜야 할 불변조건은 `모든 read 범위가 실제 buffer 범위 안에 있다`는 것이다. length-prefixed structure를 읽는 전형적인 검증은 다음 순서를 갖는다.

```text
header를 읽을 최소 byte가 존재하는가
→ length field 자체를 안전하게 decode했는가
→ length가 protocol 최대값 이하인가
→ offset + length 계산이 overflow하지 않는가
→ offset + length <= buffer.size 인가
→ nested structure의 총 budget을 넘지 않는가
```

`if (offset + length <= size)`만 두면 `offset + length` 자체가 overflow하는 타입에서는 검사가 우회될 수 있다. 안전한 형태는 subtraction 기반 경계 검사 또는 checked addition을 사용한다.

stream parser는 message 전체가 한 번에 도착한다고 가정해서도 안 된다. 네트워크·file channel은 header의 일부만 반환할 수 있다. parser state는 최소한 다음을 명시적으로 표현한다.

```text
need fixed header
need variable header
need payload N bytes
complete
invalid
```

부분 입력을 `invalid`로 오인하면 정상 stream을 끊고, 반대로 invalid 구조를 `need more bytes`로 계속 유지하면 resource exhaustion이 가능하다. 최대 frame 크기, nesting depth, element count, allocation budget을 protocol policy로 둔다.

zero-copy parsing은 copy를 줄이는 대신 lifetime coupling을 만든다. parsed object가 원본 buffer slice를 참조한다면 buffer를 재사용하거나 해제하는 시점이 object lifetime보다 늦어야 한다. 성능 최적화가 memory safety·ownership 계약을 추가한다는 뜻이다.

---

## CHAPTER 07 · canonicalization 차이는 보안 취약점이 된다

하나의 입력을 여러 계층이 서로 다르게 해석하면 validation과 실제 사용 사이에 틈이 생긴다. 대표적인 범주는 다음과 같다.

- Unicode normalization/case-folding 차이
- path separator와 `.`/`..` 처리 차이
- percent-decoding을 몇 번 수행하는지의 차이
- duplicate JSON member를 first-wins/last-wins/error로 처리하는 차이
- 숫자 문자열의 leading sign·exponent·overflow 처리 차이
- hostname 또는 identifier의 canonical form 차이

안전한 설계는 **parse once, canonicalize once, validate canonical representation, 이후 동일 representation을 사용**하는 방향을 선호한다. 같은 raw input을 각 계층에서 제각각 다시 parse하면 parser differential이 공격면이 된다.

cryptographic signature는 특히 byte identity에 민감하다. 구조적으로 같은 JSON object라도 whitespace, member order, numeric spelling이 달라 raw bytes는 달라질 수 있다. semantic object를 서명하려면 canonical serialization 규칙을 정하거나 애초에 canonical encoding이 명확한 포맷/프로파일을 사용해야 한다. “JSON을 stringify해서 서명” 같은 규칙은 구현·버전 차이에 취약할 수 있다.

identifier security에서는 normalization만으로 충분하지 않다. 서로 다른 code point가 시각적으로 매우 유사할 수 있고 script 혼합이 spoofing에 사용될 수 있다. 사용자에게 표시되는 identity와 내부 canonical key를 분리하고, 고위험 식별자에는 confusable·mixed-script 정책을 적용한다.

---

## CHAPTER 08 · 표현 문제는 raw evidence에서 역추적한다

표현 버그를 조사할 때 UI에 보이는 최종 문자열이나 숫자부터 수정하지 않는다. 가장 이른 신뢰 가능한 경계의 raw representation을 확보한다.

```text
raw bytes / original payload
schema + schema version
parser/decoder version
field offset and declared length
decoded intermediate value
normalization/canonicalization result
validated domain value
```

binary protocol이면 실패 field의 offset을 포함한 hexdump를 보존하고, 길이·endianness·signedness·scale을 문서와 대조한다. 문자열이면 raw bytes, decode charset, Unicode code-point sequence, normalization form을 비교한다. 숫자면 source lexical form과 target numeric type을 함께 기록한다.

강한 회귀검증은 값 하나의 expected output보다 invariant를 검사한다.

```text
encode(decode(validBytes))가 허용된 canonical form을 만드는가
round-trip 후 의미가 보존되는가
unknown field가 호환성 정책대로 처리되는가
invalid length/nesting/overflow가 allocation 전에 거부되는가
서로 다른 parser 구현이 동일 payload를 동일 의미로 해석하는가
```

property-based test와 fuzzing은 representation layer에 특히 강하다. 경계값, malformed UTF-8, integer extrema, 중첩 깊이, duplicate key, truncated frame, oversized length를 자동 생성하면 사람이 만든 정상 예제로 찾기 어려운 parser 차이를 드러낼 수 있다.

표현 계층의 최종 판단 기준은 간단하다. **의미를 주장하려면 그 의미를 만드는 계약과 raw evidence를 함께 제시할 수 있어야 한다.**