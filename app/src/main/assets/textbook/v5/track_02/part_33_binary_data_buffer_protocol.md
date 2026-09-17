# PART 33 · Binary data와 buffer protocol — bytes·bytearray·memoryview로 복사와 소유권 다루기

텍스트를 넘어 이미지, 압축 데이터, 파일 header, network frame을 다루면 문자열이 아니라 byte sequence가 중심이 된다. Python은 immutable `bytes`, mutable `bytearray`, zero-copy view를 만들 수 있는 `memoryview` 같은 도구를 제공한다. 중요한 것은 함수 이름을 외우는 것이 아니라 **buffer가 누구의 메모리를 참조하고, 어떤 width와 endian으로 해석하며, 언제 복사가 발생하는지**를 추적하는 것이다.

---

## CHAPTER 01 · bytes는 숫자 0..255의 immutable sequence로 본다

`bytes`를 출력하면 ASCII처럼 보이는 부분이 문자로 표시될 수 있지만 본질은 byte value의 sequence다. Indexing 결과가 한 글자 string이 아니라 integer byte value가 될 수 있는 이유도 이 모델에서 이해할 수 있다. Text 의미가 필요할 때만 encoding을 선택해 decode한다.

Immutable이라는 성질은 hash key로 사용할 수 있고 여러 caller가 공유해도 내용이 바뀌지 않는 장점이 있다. 대신 일부 slice와 concatenation은 새로운 bytes object를 만들어 data를 복사할 수 있다. 큰 payload를 반복적으로 잘라 붙이는 hot path에서는 allocation 비용이 커질 수 있다.

Binary protocol에서 delimiter byte를 찾거나 fixed header를 비교하는 작업은 text normalization을 적용하지 않는다. Raw bytes identity가 contract이므로 Unicode layer를 끼우면 의미가 바뀔 수 있다.

Log에 arbitrary bytes를 그대로 text로 decode해 넣지 않는다. Invalid encoding이 있거나 binary secret이 포함될 수 있다. 제한된 hex preview와 length처럼 진단에 필요한 정보만 기록한다.

---

## CHAPTER 02 · bytearray는 같은 byte model에 mutation을 허용한다

`bytearray`는 byte sequence를 제자리에서 수정할 수 있어 incremental parser buffer, packet assembly, binary transformation에 유용하다. Mutable object이므로 여러 이름이 같은 bytearray를 공유하면 한쪽 수정이 다른 쪽에서 보인다. 일반 list와 같은 aliasing 문제가 binary data에서도 발생한다.

Buffer를 재사용하면 allocation을 줄일 수 있지만 parser가 이전 slice를 계속 참조하고 있다면 새로운 data를 덮어써 잘못된 값이 보일 수 있다. Ownership과 lifetime을 명시한다. Consumer가 data를 오래 보관해야 하면 immutable bytes snapshot으로 복사하는 편이 안전할 수 있다.

Length를 변경하는 mutation과 같은 크기 안에서 byte만 덮어쓰는 operation은 exported view가 있을 때 제약이 다를 수 있다. Buffer protocol을 통해 다른 object가 memory를 보고 있다면 resize가 허용되지 않는 경우가 있다.

Mutation은 성능 도구이면서 새로운 invariant를 만든다. `valid_length <= capacity`, read/write offset 범위를 checked arithmetic으로 관리한다.

---

## CHAPTER 03 · memoryview는 원본 buffer를 복사하지 않고 다른 해석 창을 만든다

큰 bytes나 bytearray의 일부분을 함수에 전달할 때 slice가 data를 복사하면 memory와 CPU 비용이 생긴다. `memoryview`는 buffer를 참조하는 view를 만들어 같은 underlying memory를 다른 범위에서 볼 수 있게 한다. Zero-copy parser와 I/O pipeline에서 유용하다.

Zero-copy의 대가는 lifetime coupling이다. View가 살아 있는 동안 원본 buffer가 유효해야 하고 mutable 원본이면 내용이 바뀔 수 있다. Consumer가 stable snapshot을 기대한다면 view를 반환하는 API는 위험할 수 있다.

View 자체를 slice하면 또 다른 view를 만들 수 있어 큰 원본의 작은 부분만 필요해도 원본 전체 lifetime이 연장될 수 있다. Memory leak처럼 보이는 retention이 생길 수 있으므로 장기 저장 전에 필요한 작은 bytes로 복사할지 판단한다.

Performance 최적화에서 “copy 0회”가 항상 목표는 아니다. 짧은 data를 한 번 복사해 resource lifetime을 끊는 것이 전체 system에서는 더 효율적일 수 있다.

---

## CHAPTER 04 · binary field는 width·signedness·endianness를 함께 정의한다

4 bytes를 integer로 읽는다고 해도 signed/unsigned와 byte order가 정해져야 값이 결정된다. `01 00 00 00`은 little-endian 32-bit에서는 1일 수 있지만 big-endian에서는 전혀 다른 값이다. Protocol schema는 width와 endian을 생략하지 않는다.

Host machine native order를 wire format으로 그대로 사용하면 다른 architecture와 interoperability가 깨질 수 있다. Network/protocol format은 고정된 byte order를 정하고 boundary에서 변환한다.

Integer range를 검증하지 않고 작은 width로 pack하려 하면 exception이나 truncation이 발생할 수 있다. API가 어떤 behavior를 갖는지 확인하고 serialization 전에 domain range를 검사한다.

Signed two's-complement representation과 float bit representation처럼 같은 bytes가 type에 따라 전혀 다른 의미를 가진다. Buffer 자체에는 type tag가 없으므로 schema가 해석을 결정한다.

---

## CHAPTER 05 · struct-style packing은 binary schema를 format string으로 압축한다

고정된 binary record를 pack/unpack하는 API는 field width와 endian, alignment rule을 compact한 format으로 표현한다. 이 문자열은 사실 작은 schema이므로 source에 magic string으로 흩어놓기보다 이름을 붙이고 expected size를 검증한다.

Native alignment를 사용하는 format과 standard packed layout을 사용하는 format은 field offset이 달라질 수 있다. 파일·network format은 platform-independent layout을 명시한다. C struct와 Python format이 같다고 가정하지 말고 실제 ABI/padding을 확인한다.

Unpack 전에 buffer length가 충분한지 검사한다. Header 안의 payload length를 읽은 뒤 `offset + length` overflow/bounds를 확인하고 protocol maximum을 적용한다. Parser는 valid input보다 malicious/truncated input에서 더 많은 경계 검사가 필요하다.

Version이 바뀌어 field가 추가되면 fixed struct size도 달라질 수 있다. Header에 version/length를 넣거나 version별 decoder를 유지해 old data를 새로운 layout으로 잘못 읽지 않게 한다.

---

## CHAPTER 06 · incremental parser는 “아직 부족함”과 “잘못됨”을 다른 상태로 표현한다

Stream에서 header 8 bytes가 필요하지만 현재 5 bytes만 도착한 것은 invalid packet이 아니라 incomplete input이다. Parser가 `need_more(3)`와 `invalid(reason)`을 구분하면 network fragmentation을 정상 처리할 수 있다.

State machine은 `READ_HEADER → READ_BODY(n) → COMPLETE`처럼 현재 필요한 정보와 남은 byte 수를 가진다. Header가 선언한 body length가 maximum frame size를 넘으면 body가 도착하기를 기다리지 않고 즉시 거부한다.

Buffer compaction도 고려한다. 처리한 앞부분을 매번 삭제해 전체 bytearray를 이동하면 큰 stream에서 복사 비용이 누적될 수 있다. Read offset을 이동하고 일정 threshold에서 한 번 compact하는 전략을 사용할 수 있다.

Parser error에는 absolute stream offset과 field context를 남기면 malformed data를 조사하기 쉽다. Raw payload 전체를 log에 남겨 secret과 대용량 data를 노출하지 않는다.

---

## CHAPTER 07 · buffer protocol은 서로 다른 라이브러리가 같은 메모리를 공유하는 계약이다

Python의 일부 객체는 자신의 raw memory를 다른 consumer가 접근할 수 있도록 buffer protocol을 제공한다. `memoryview`, binary I/O, numerical library가 data copy 없이 연결될 수 있는 기반이다. Shape, item size, format, strides 같은 metadata가 multi-dimensional data 해석에 필요할 수 있다.

Consumer가 writable buffer를 요구하는지 read-only buffer도 허용하는지 확인한다. Immutable bytes에 쓰려고 하면 실패하고 mutable memory를 공유하면 외부 library가 원본을 변경할 수 있다.

Native extension과 zero-copy interop에서는 원본 object lifetime을 Python reference가 보호해야 한다. C code가 pointer만 보관하고 Python object가 수집되면 use-after-free 위험이 생길 수 있으므로 extension contract가 lifetime을 명시해야 한다.

Application code에서 buffer protocol 세부 구현을 자주 다룰 필요는 없지만 data science/image/network library 사이에 “왜 copy가 생기는가”를 이해하는 데 중요한 모델이다.

---

## CHAPTER 08 · binary equality와 cryptographic comparison은 목적이 다르다

두 bytes가 같은지 일반 equality로 비교하는 것은 데이터 값 비교에는 충분할 수 있다. 그러나 secret token이나 MAC처럼 공격자가 timing을 관찰할 수 있는 값의 비교는 early-exit equality가 정보 leakage를 만들 수 있어 constant-time comparison API가 필요할 수 있다.

Hash digest를 비교할 때도 text hex string과 raw digest bytes를 구분한다. Hex는 raw bytes의 printable encoding이므로 길이가 두 배가 되고 대소문자 representation 차이가 있을 수 있다. Protocol이 어느 형태를 요구하는지 명시한다.

Checksum과 cryptographic hash는 목적이 다르다. CRC는 transmission corruption detection에 유용하지만 attacker가 조작하는 data의 authenticity를 보장하지 않는다. Security boundary에서는 keyed MAC이나 signature 같은 적절한 primitive를 사용한다.

직접 cryptographic compare나 hash construction을 구현하지 않고 검증된 library를 사용한다. Binary programming의 작은 실수가 security property를 깨뜨릴 수 있다.

---

## CHAPTER 09 · compression은 input size보다 output budget을 함께 검증한다

작은 compressed file이 압축 해제 후 매우 큰 data가 되는 compression bomb이 가능하다. Upload 크기만 제한하고 decompressed size를 제한하지 않으면 memory/disk exhaustion이 발생할 수 있다. Streaming decompressor에도 총 output byte budget을 적용한다.

Compressed stream이 중간에 손상되면 일부 output이 이미 생성되었을 수 있다. 전체 object가 valid해야 저장할 수 있는 domain에서는 temporary output에 쓰고 integrity check 후 commit한다.

Compression format에는 checksum과 metadata가 있을 수 있지만 그 의미가 authenticity를 보장하는 것은 아니다. File extension만 보고 decoder를 선택하기보다 magic/header와 허용 format policy를 확인한다.

압축 여부는 CPU와 I/O trade-off다. Network bandwidth를 줄이는 대신 compression CPU와 latency가 늘어난다. Payload 크기와 workload를 측정해 threshold를 선택한다.

---

## CHAPTER 10 · binary boundary는 schema·ownership·budget 세 축으로 검증한다

Binary data를 안전하게 다루려면 먼저 bytes의 의미를 정하는 schema가 있어야 한다. 그 다음 buffer를 누가 소유하고 view가 얼마나 오래 살아 있는지 lifetime을 정하며, 마지막으로 frame size와 allocation, decompression output 같은 resource budget을 둔다.

Zero-copy와 buffer reuse는 성능을 높일 수 있지만 aliasing과 lifetime을 복잡하게 한다. Copy는 비용이지만 ownership을 끊어 contract를 단순하게 만드는 도구이기도 하다. 어느 쪽이 전체 system에서 더 나은지 profiler와 memory measurement로 결정한다.

Text와 binary 경계를 명시하면 encoding 오류와 protocol 오류를 분리할 수 있다. Wire bytes는 parser가 읽고 domain value로 변환된 뒤 내부 logic은 raw offset을 모르게 만든다.

Binary programming의 핵심은 **byte 배열을 빠르게 조작하는 것이 아니라 같은 memory를 누가 어떤 schema로 언제까지 해석하는지, 외부 입력이 자원 한계를 넘지 않는지 통제하는 것**이다.