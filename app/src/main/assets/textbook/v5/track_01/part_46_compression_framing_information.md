# PART 46 · Compression and Framing — redundancy, dictionary, entropy code, resource bounds

압축은 데이터를 단순히 작게 만드는 기능이 아니라 **반복과 확률 구조를 더 짧은 표현으로 바꾸고, 제한된 자원 안에서 원본을 복원하는 프로토콜**이다. 실제 시스템에서는 압축률만 보면 설계를 잘못하기 쉽다. encoder 탐색 비용, decoder 메모리, frame 경계, random access, checksum, streaming backpressure, 공격자가 만든 입력의 확장률까지 함께 봐야 한다. 이 PART는 source model에서 시작해 LZ·entropy coding·framing을 거쳐 운영상의 resource contract까지 연결한다.

---

## CHAPTER 01 · redundancy는 압축 알고리즘이 소비하는 원재료다

Lossless compression이 이득을 내는 이유는 입력에 반복 문자열, 치우친 symbol frequency, 규칙적인 schema처럼 **예측 가능한 구조**가 있기 때문이다. 모든 가능한 byte sequence를 동시에 더 짧게 표현할 수는 없으므로, 어떤 데이터에서 이득을 얻으면 다른 데이터에서는 header나 code overhead 때문에 길어질 수 있다. 따라서 “텍스트는 잘 압축된다” 같은 분류보다 실제 corpus의 반복 거리와 symbol 분포를 보는 편이 정확하다.

Source model은 다음 값이 무엇일 가능성이 높은지에 대한 가정이다. JSON key가 반복되는 로그, 동일 prefix를 가진 telemetry, 같은 column type이 연속되는 table은 서로 다른 형태의 redundancy를 제공한다. 이미 잘 압축된 미디어나 암호문은 이 구조가 상당 부분 제거되어 추가 이득이 작다.

압축 정책을 정할 때는 원본 크기, 압축 후 크기뿐 아니라 데이터 유형별 ratio 분포를 기록한다. 평균 ratio 하나만 남기면 incompressible tail을 놓친다. 모델이 잘 맞는 집합과 맞지 않는 집합을 분리해야 level과 dictionary 선택이 의미를 가진다.

## CHAPTER 02 · lossless와 lossy는 보존해야 하는 invariant부터 다르다

Lossless codec의 핵심 invariant는 `decode(encode(x)) = x`가 byte 단위로 성립하는 것이다. source code, 실행 파일, database backup처럼 한 bit의 차이도 의미를 바꿀 수 있는 데이터는 이 계약이 필요하다. 반면 lossy codec은 사람이 인지하기 어려운 정보나 application이 허용한 오차를 버려 더 큰 크기 절감을 얻는다.

따라서 두 방식을 압축률 숫자로만 비교하면 안 된다. Lossy에서는 distortion metric, quality setting, downstream tolerance가 correctness 정의의 일부가 된다. 예를 들어 이미지 분석 pipeline은 사람이 보기 좋은 결과와 모델 입력에 안정적인 결과가 다를 수 있다.

검증 방식도 달라진다. Lossless는 원본 hash나 byte comparison으로 직접 확인할 수 있지만 lossy는 domain metric과 허용 범위가 필요하다. 저장 계층이 어떤 종류의 보존 계약을 제공하는지 metadata에 명시해야 복원 시점의 오해를 막을 수 있다.

## CHAPTER 03 · model과 coder를 분리하면 압축 파이프라인의 병목이 보인다

압축기는 입력의 구조를 찾는 **modeling 단계**와 그 결과 symbol을 짧은 bit 표현으로 바꾸는 **coding 단계**로 나누어 볼 수 있다. LZ 계열은 반복 substring을 literal 또는 length-distance 형태의 token으로 바꾸고, entropy coder는 token frequency에 따라 평균 code length를 줄인다. 좋은 coder가 나쁜 model을 구제하지 못하고, 좋은 match도 비싼 metadata로 상쇄될 수 있다.

이 분리는 성능 분석에도 유용하다. CPU profile에서 hash-chain 탐색이 시간을 쓰는지, entropy table build가 비싼지, bit packing이 병목인지 구분할 수 있기 때문이다. 같은 압축률 저하라도 원인이 match 부족인지 code assignment 문제인지에 따라 튜닝 방향이 달라진다.

Format을 설계할 때도 model token과 serialized representation을 분리해 문서화한다. Decoder는 “무슨 의미의 token인가”와 “어떤 bit pattern으로 저장되었는가”를 모두 검증해야 한다. 두 계층이 뒤섞이면 corruption 처리와 버전 진화가 어려워진다.

## CHAPTER 04 · LZ back-reference는 이미 복원한 history를 데이터로 재사용한다

LZ 계열은 현재 위치의 byte sequence가 앞선 output에 존재하면 literal을 반복 저장하지 않고 `distance + length` 형태의 참조로 표현한다. Decoder는 이전에 복원한 history를 읽어 다음 output을 만든다. 그래서 back-reference는 단순한 압축 기법이 아니라 **과거 output을 주소 공간처럼 사용하는 명령**에 가깝다.

Overlapping copy도 중요하다. 짧은 pattern을 자기 자신을 확장하듯 복사하면 작은 reference 하나로 긴 반복을 만들 수 있다. 구현은 format이 정의한 copy semantics를 따라야 하며 일반 `memcpy`와 같은 동작을 가정하면 안 된다. distance가 0이거나 아직 생성되지 않은 영역을 가리키는 malformed stream도 거부해야 한다.

Decoder 보안 검토에서는 distance 범위, length 합산 overflow, output limit을 함께 본다. 압축 해제기는 공격자가 만든 명령열을 실행하는 parser이므로, “정상 encoder가 이런 값을 만들지 않는다”는 검증 생략의 근거가 되지 않는다.

## CHAPTER 05 · window 크기는 ratio와 decoder memory를 맞바꾸는 명시적 계약이다

Sliding window가 크면 멀리 떨어진 반복을 참조할 수 있어 ratio가 좋아질 가능성이 커진다. 대신 decoder는 그 거리만큼의 history를 유지해야 하므로 최소 메모리 요구량이 증가한다. Frame이 window 크기를 선언하는 format에서는 그 metadata가 곧 resource request가 된다.

서버가 untrusted client의 압축 stream을 받는다면 선언된 window를 그대로 allocate하면 안 된다. 서비스가 허용하는 최대 window, 동시 decoder 수, container memory limit을 함께 고려해 상한을 둬야 한다. 단일 요청은 합리적이어도 수천 연결이 동시에 큰 window를 요청하면 전체 memory pressure가 커진다.

Encoder 측에서는 무조건 최대 window를 쓰지 않는다. 실제 반복 거리 histogram을 측정해 window 증가가 ratio를 얼마나 개선하는지 확인한다. 1 MiB 밖의 reference가 거의 없다면 64 MiB window는 메모리만 늘릴 수 있다.

## CHAPTER 06 · match finder는 압축률보다 먼저 encoder의 계산량을 결정할 수 있다

현재 위치에서 과거와 일치하는 문자열을 찾으려면 hash table, chain, tree 같은 index에서 candidate를 탐색한다. 더 많은 후보를 비교할수록 긴 match를 찾을 가능성이 높아지지만 memory access와 branch가 늘어난다. 높은 compression level이 느린 주된 이유 중 하나가 이 search budget 증가다.

Match finder는 데이터 분포에도 민감하다. 반복이 많은 입력에서는 후보가 지나치게 많아질 수 있고, 거의 random한 입력에서는 search가 실패를 확인하는 데 CPU만 쓸 수 있다. 따라서 incompressible detection이나 search depth 제한이 tail latency를 안정시키는 데 중요하다.

성능 측정에서는 encoder 전체 시간만 보지 말고 bytes searched, candidates tested, match length distribution을 같이 수집한다. 같은 5% ratio 개선이라도 candidate가 2배 늘어난 결과인지, 더 좋은 indexing으로 얻은 결과인지에 따라 운영 가치가 다르다.

## CHAPTER 07 · parse strategy는 지금의 긴 match와 뒤의 더 싼 경로를 비교한다

Greedy parser는 현재 위치에서 가장 좋아 보이는 match를 바로 선택하지만 그 선택이 다음 위치의 훨씬 긴 match를 가릴 수 있다. Lazy matching은 한두 위치 앞을 보고 결정을 미루고, 더 복잡한 optimal parser는 literal과 match의 예상 bit cost를 누적해 전체 경로를 비교한다.

중요한 것은 “가장 긴 match”가 항상 “가장 싼 encoding”은 아니라는 점이다. distance symbol이 비싸거나 특정 length가 entropy table에서 불리하면 조금 짧은 match와 literal 조합이 더 적은 bit를 쓸 수 있다. Parser가 entropy model의 cost를 알고 있으면 선택 품질이 높아진다.

Archival workload는 parse search에 큰 CPU budget을 줄 수 있지만 RPC payload는 그렇지 못하다. 알고리즘 이름보다 허용 가능한 encode latency와 ratio 목표를 먼저 정하고, 그 범위에서 parse depth를 선택해야 한다.

## CHAPTER 08 · Huffman coding은 frequency를 prefix-free bit length로 변환한다

Huffman code는 자주 나오는 symbol에 짧은 codeword를, 드문 symbol에 긴 codeword를 배정한다. Prefix-free 조건 때문에 decoder는 bitstream을 왼쪽부터 읽으며 한 codeword가 끝나는 지점을 모호함 없이 판단할 수 있다. 다만 frequency가 거의 균일하면 fixed-width와 큰 차이가 없을 수 있다.

실제 format에서는 code tree 자체를 그대로 저장하기보다 code length를 canonical 규칙으로 재구성하는 방식을 많이 쓴다. 이렇게 하면 table metadata를 줄이고 decoder implementation도 단순화할 수 있다. 하지만 malformed length set이 oversubscribed 또는 incomplete한 경우를 검증해야 한다.

작은 block에서는 table을 전달하는 비용이 payload 절감보다 클 수 있다. 그래서 predefined table, previous table reuse, raw block 같은 선택지가 존재한다. Huffman 효율은 symbol 통계뿐 아니라 table 전송 비용까지 포함해 계산해야 한다.

## CHAPTER 09 · entropy coder의 state는 bitstream을 해석하는 실행 상태다

현대 codec은 Huffman뿐 아니라 finite-state 기반 entropy coding을 사용해 평균 bit 수를 더 촘촘하게 줄이기도 한다. 이때 decoder는 normalized frequency table과 현재 state를 이용해 symbol을 복원하고 다음 state로 이동한다. 즉 bitstream은 단순한 codeword 나열이 아니라 **상태 전이 규칙과 결합된 데이터**다.

Corruption으로 state가 허용 범위를 벗어나거나 table 합계가 틀리면 이후 bit 소비 위치가 연쇄적으로 깨진다. 구현은 table build 시 범위와 합계를 검증하고, decode loop에서도 input/output boundary를 보장해야 한다. 한 번의 unchecked state index가 arbitrary memory access로 이어질 수 있다.

성능에서는 state table 크기와 cache locality가 중요하다. 더 세밀한 probability 표현이 ratio를 조금 개선해도 decode table이 cache를 압박하면 throughput이 떨어질 수 있다. format 선택은 bit efficiency와 구현 비용을 함께 평가해야 한다.

## CHAPTER 10 · substream 분리는 서로 다른 통계를 이용하지만 동기화 부담을 만든다

Literal byte, match length, offset은 서로 다른 확률 분포를 가진다. 이들을 별도 substream으로 분리하면 각 stream에 적합한 entropy table을 적용할 수 있다. Decoder는 각 stream을 독립적으로 읽다가 sequence metadata에 따라 다시 하나의 output으로 결합한다.

문제는 경계가 하나 틀렸을 때다. literal count나 bitstream length가 손상되면 다음 substream의 시작 위치까지 밀릴 수 있다. 따라서 section size, symbol count, compressed length를 교차 검증하고 parent frame boundary를 절대 넘어가지 않게 해야 한다.

병렬 decode를 설계할 때도 substream이 곧 독립 작업이라는 보장은 없다. 한 stream의 symbol이 다른 stream에서 몇 byte를 소비할지 결정할 수 있기 때문이다. dependency graph를 확인한 뒤 실제 parallel unit을 정한다.

## CHAPTER 11 · frame은 decoder가 다시 동기화할 수 있는 독립 경계를 제공한다

Frame은 magic, header, optional size·dictionary·checksum 정보, compressed block을 묶어 하나의 해석 단위로 만든다. 여러 frame을 이어 붙일 수 있는 format이라면 각 frame은 decoder state를 새로 시작하는 recovery point가 된다. Application message와 frame 경계가 일치하는지는 별도 계약이다.

Header가 전달하는 값은 모두 decoder 동작을 바꾼다. window 크기, dictionary id, content size를 읽기 전에 길이와 허용 범위를 확인해야 한다. unknown flag를 무시할지 거부할지도 버전 호환성 규칙에 따라 정한다.

운영 관점에서는 frame boundary가 corruption 격리와 random access granularity를 결정한다. 한 거대한 frame은 ratio에는 유리할 수 있지만 일부 손상이 전체 object 복원을 막을 수 있다. 저장·전송 실패 모델과 함께 크기를 설계한다.

## CHAPTER 12 · block size는 context reuse와 latency 사이의 조절점이다

큰 block은 더 많은 sample에서 symbol 통계를 얻고 header 비중을 줄일 수 있지만, encoder가 입력을 모으는 시간이 길어져 first-byte latency가 늘 수 있다. 작은 block은 빠르게 flush하고 parallel processing하기 쉽지만 table·block header overhead가 상대적으로 커진다.

Streaming service에서 `flush()`를 호출하는 빈도도 사실상 block 정책이다. 작은 application message마다 강제 flush하면 compressor가 가진 context와 batching 이득을 버릴 수 있다. 반대로 무한히 기다리면 interactive latency 요구를 위반한다.

평균 throughput만 측정하지 말고 payload size별 compression ratio와 flush-to-first-output latency를 본다. block 크기를 정할 때 p99 message size와 backpressure 조건을 함께 포함해야 한다.

## CHAPTER 13 · streaming decode는 전체 파일 크기와 무관한 bounded state를 목표로 한다

좋은 streaming API는 입력 전체를 메모리에 올리지 않고 일정한 window와 parser state만 유지하며 decode할 수 있다. 하지만 application이 편의를 위해 `read all → decompress all → parse all` 구조를 쓰면 format의 bounded-memory 성질을 잃는다. 큰 upload에서는 이 차이가 OOM 여부를 결정한다.

Decoder는 input이 조각나 도착하는 경우도 처리해야 한다. Header 하나가 여러 network chunk에 걸칠 수 있고, output sink가 느리면 decompressor가 backpressure를 받아 멈춰야 한다. “현재까지 몇 byte를 소비했고 몇 byte를 만들었는가”를 API state로 명확히 표현해야 한다.

Cancellation도 중요하다. 요청 취소 시 dictionary buffer와 output reservation을 즉시 회수하고 partial object를 완료 상태로 노출하지 않는다. Streaming은 단순한 성능 기능이 아니라 lifetime 관리 문제다.

## CHAPTER 14 · pretrained dictionary는 독립된 작은 payload에도 공유 history를 제공한다

수백 byte짜리 record를 각각 독립 frame으로 압축하면 이전 message의 반복을 활용할 수 없다. 비슷한 schema와 문자열을 가진 corpus에서 dictionary를 학습하면 첫 byte부터 공통 pattern을 reference할 수 있어 small payload의 header·startup 손실을 줄일 수 있다.

Dictionary는 sender와 receiver가 동일한 artifact를 사용해야 한다. id만 맞고 내용이 다르면 silent corruption이 아니라 decode 실패로 처리되어야 하므로 dictionary 자체의 version과 cryptographic hash를 배포 체계에 포함하는 편이 안전하다.

운영에서는 dictionary rollout이 codec rollout과 별개라는 점을 기억한다. 새 dictionary를 먼저 배포하고 구버전 reader가 존재하는 동안 dual support를 유지한 뒤 writer를 전환한다. hit rate와 fallback 비율을 telemetry로 확인한다.

## CHAPTER 15 · random access를 원하면 dependency chain의 길이를 제한해야 한다

Sequential compressor가 이전 history를 계속 참조하면 중간 offset의 block만 가져와서는 decode할 수 없다. Point lookup이 필요한 저장 형식은 독립 frame, restart point, chunk index를 두어 특정 구간을 복원하는 데 필요한 앞선 데이터 양을 제한한다.

Restart를 자주 할수록 dictionary history가 끊기므로 ratio가 나빠질 수 있다. 반대로 restart 간격이 너무 크면 작은 record 하나를 읽으려고 수십 MiB를 먼저 decode해야 한다. 이 trade-off는 압축 algorithm보다 access pattern에 의해 결정된다.

Index에도 correctness 문제가 있다. Offset은 compressed byte 기준인지 uncompressed position 기준인지 명확해야 하고, append나 corruption 후 index와 data의 generation이 일치하는지 검증한다. Random access는 framing과 metadata consistency를 함께 요구한다.

## CHAPTER 16 · checksum은 accidental corruption을 찾지만 authenticity를 증명하지 않는다

Frame checksum은 저장 장치나 전송 중 발생한 bit error를 검출하는 데 유용하다. 하지만 공격자가 payload와 checksum을 함께 다시 계산할 수 있다면 악의적 변조를 막지 못한다. CRC나 비암호학적 checksum을 MAC 또는 signature와 혼동하면 안 된다.

Checksum 범위도 중요하다. Compressed bytes를 보호하는지, decompressed content를 보호하는지에 따라 검출 가능한 오류가 다르다. Metadata가 범위 밖이면 length나 dictionary id 변조가 별도로 남을 수 있다. Format spec의 정확한 coverage를 확인한다.

Backup 같은 장기 보존에서는 frame checksum 외에 object-level hash를 둘 가치가 있다. 복구 시 decode 성공만 기록하지 말고 expected hash와 비교해 end-to-end integrity를 확인한다.

## CHAPTER 17 · size metadata는 allocation 최적화 정보이지 신뢰 가능한 약속이 아니다

Frame이 uncompressed content size를 제공하면 output buffer를 한 번에 예약할 수 있어 편리하다. 그러나 untrusted input이 거대한 값을 선언하면 decode를 시작하기도 전에 memory exhaustion을 만들 수 있다. Metadata를 읽는 순간 resource policy와 비교해야 한다.

Size가 없거나 unknown인 format도 있으므로 decoder는 incremental growth를 지원해야 한다. Growth factor만 두고 상한이 없으면 작은 compressed stream이 계속 memory를 확장시킬 수 있다. Application별 maximum output bytes와 allocator failure 경로를 명시한다.

Declared size와 실제 produced size가 다르면 corruption 또는 format violation으로 처리한다. 이 교차 검증은 truncation과 trailing data를 찾는 데도 유용하다. Metadata는 믿는 값이 아니라 검증할 주장이다.

## CHAPTER 18 · compression bomb은 byte 비율이 아니라 자원 비대칭을 공격한다

아주 반복적인 데이터는 몇 KiB의 compressed input에서 GiB 단위 output을 만들 수 있다. 공격자는 network 비용은 작게 지불하면서 서버의 memory, disk, CPU를 크게 소비시킬 수 있다. 따라서 upload compressed size 제한만으로는 방어가 되지 않는다.

안전한 decoder는 total decompressed bytes, expansion ratio, dictionary/window memory, decode time을 독립적으로 제한한다. Archive라면 entry count와 path depth도 추가된다. Limit은 request 단위뿐 아니라 tenant·process 전체 예산과 연결해야 한다.

Limit 도달 시 처리도 설계 대상이다. Partial extraction을 최종 위치에 남기거나 metadata만 먼저 commit하면 후속 코드가 불완전한 데이터를 정상으로 볼 수 있다. 임시 영역에서 작업하고 성공 후 원자적으로 publish하는 방식이 안전하다.

## CHAPTER 19 · nested compression에서는 각 layer의 합리적인 제한이 곱셈 폭발을 만들 수 있다

HTTP body가 압축되어 있고 그 안의 archive가 다시 압축 파일을 포함하면 여러 decoder가 연쇄된다. 각 layer가 “최대 100배”를 허용해도 두 단계면 이론상 10,000배까지 커질 수 있다. 각 decoder가 자기 입력만 보는 구조는 전체 resource amplification을 통제하지 못한다.

Pipeline은 cumulative budget을 공유해야 한다. 현재까지 생성된 logical bytes, nesting depth, 남은 CPU budget을 context로 전달하고 새 decoder 생성 전에 차감한다. 단순히 각 library의 default limit에 의존하지 않는다.

관측도 layer별로 분리한다. 어느 단계에서 expansion이 발생했는지, 어떤 content-encoding 또는 archive entry가 원인인지 남겨야 incident에서 정책을 조정할 수 있다. Payload 내용 자체를 민감하게 로그하지 않는 것도 중요하다.

## CHAPTER 20 · decompressor는 성능 라이브러리가 아니라 공격 표면을 가진 binary parser다

Compressed format에는 length, offset, table count, state index처럼 memory access를 결정하는 숫자가 많다. Integer overflow, 잘못된 back-reference, 과도한 allocation, 무한 loop는 모두 보안 취약점으로 이어질 수 있다. Decoder는 정상 encoder가 생성하는 범위가 아니라 spec이 허용하는 전체 입력 공간을 방어해야 한다.

구현에서는 모든 산술을 boundary check와 함께 수행하고 output pointer가 frame limit을 넘지 않도록 단일 invariant로 관리한다. Error path도 success path와 같은 수준으로 lifetime을 정리해야 double-free나 use-after-free를 피할 수 있다.

검증은 known-good corpus만으로 충분하지 않다. Truncated header, invalid code table, 최대값 경계, random mutation을 fuzzing과 sanitizer로 지속 실행한다. Library 업데이트 시 security fix가 ABI와 성능에 미치는 영향도 함께 확인한다.

## CHAPTER 21 · compression level은 ratio·CPU·memory를 묶은 preset이다

Level 숫자는 보통 match search depth, strategy, window, entropy optimization 같은 여러 knob를 함께 조정한다. 높은 level이 더 작은 output을 만들 가능성은 있지만 CPU 사용량이 비선형으로 증가할 수 있고 peak memory도 커질 수 있다. 숫자 자체를 품질 등급처럼 해석하면 안 된다.

Encode-once/decode-many archive는 높은 encode 비용을 감수할 수 있지만 실시간 API는 p99 encode latency가 더 중요하다. Mobile에서는 CPU 시간이 곧 energy와 thermal 상태에 영향을 준다. 같은 codec이라도 workload마다 적절한 level이 다르다.

실험에서는 level별 bytes saved, encode MB/s, decode MB/s, peak RSS를 한 표로 본다. 한 지표만 최적화하지 않고 서비스의 비용 함수에 맞는 지점을 선택한다.

## CHAPTER 22 · compression은 CPU 시간을 소비해 I/O bytes를 줄이는 교환이다

네트워크나 저장장치가 병목이면 압축에 CPU를 써도 total latency가 줄 수 있다. 반대로 CPU가 이미 포화되고 local NVMe가 충분히 빠르면 압축 때문에 더 느려질 수 있다. End-to-end 시간은 `encode + transfer + decode` 세 구간으로 분해해서 봐야 한다.

Throughput만 보면 queueing effect를 놓칠 수 있다. Encoder worker가 포화되면 작은 CPU 증가가 대기 시간을 크게 늘릴 수 있고, network byte 절감의 이득을 상쇄한다. Compression을 켠 뒤 CPU utilization과 request latency distribution을 함께 본다.

비용 모델에는 cloud egress·storage 비용도 들어갈 수 있다. CPU 1초의 가격과 전송 bytes 가격이 다르면 최적 level이 달라진다. 기술적인 ratio뿐 아니라 운영 단가까지 포함한 실제 workload 실험이 필요하다.

## CHAPTER 23 · 작은 payload는 header와 table 비용 때문에 오히려 커질 수 있다

수십~수백 byte 메시지는 반복을 찾을 표본이 적고 frame header, checksum, entropy table 비용의 비중이 크다. 압축 후 크기가 원본보다 커지는 것은 bug가 아니라 format overhead가 이득을 넘은 결과일 수 있다.

따라서 size threshold 아래에서는 raw 전송을 선택하거나 shared dictionary를 사용할 수 있다. 단일 고정 threshold보다 content type과 실제 ratio 분포를 이용하면 더 정확하다. 특히 이미 base64·compressed binary를 담은 작은 메시지는 이득이 거의 없다.

정책 전환에는 hysteresis나 stable rule을 둬야 작은 크기 차이로 압축 여부가 계속 흔들리지 않는다. Protocol peer가 encoding을 지원하는지와 cache key가 encoding별로 분리되는지도 확인한다.

## CHAPTER 24 · incompressible input에서도 encoder의 실패 비용은 제한되어야 한다

암호문, 압축 미디어, random-like 데이터는 유용한 match나 symbol bias가 거의 없다. 이런 입력에서 compressor가 끝까지 비싼 search를 수행하면 output 이득은 없고 CPU만 소모한다. 공격자가 일부러 incompressible payload를 보내 CPU amplification을 만들 수도 있다.

Encoder는 일정 구간에서 ratio가 나쁘면 search depth를 낮추거나 raw block으로 전환하는 전략을 가질 수 있다. 하지만 heuristic이 너무 공격적이면 뒤쪽에 compressible 구간이 나타나는 데이터에서 기회를 놓친다. Sampling window와 전환 기준을 workload로 검증한다.

관측에서는 compressed size뿐 아니라 `saved_bytes / cpu_time` 같은 효율 지표를 볼 수 있다. Negative saving이 지속되는 content type은 compression allowlist에서 제외하는 것이 단순하고 안전할 수 있다.

## CHAPTER 25 · encryption 뒤의 compression은 거의 이득이 없고 순서 자체가 보안 의미를 가진다

안전한 암호문은 평문 구조를 숨겨 random-like하게 보이도록 설계되므로 암호화 후 압축은 대체로 이득이 없다. 보통은 압축 후 암호화 순서를 사용하지만, 공격자가 선택한 입력과 secret이 같은 compression context를 공유하고 compressed length를 관찰할 수 있으면 길이 side channel이 생길 수 있다.

문제는 암호화 primitive가 아니라 **secret-dependent compression ratio가 외부에 노출되는 구조**다. Cookie나 token 같은 secret과 attacker-controlled text를 같은 context에서 압축하는 protocol은 별도 threat model이 필요하다.

대응은 secret과 attacker input의 context 분리, compression 비활성화, padding 등 protocol 수준에서 결정한다. 단순히 강한 cipher를 사용한다고 길이 leakage가 사라지지 않는다.

## CHAPTER 26 · HTTP Content-Encoding은 representation에 적용되는 protocol 계약이다

HTTP에서 compression은 connection 내부 구현 세부사항이 아니라 representation의 content coding으로 전달된다. Sender가 어떤 encoding을 적용했는지 metadata로 알리고 receiver는 그 순서에 맞춰 decode한다. Cache와 proxy도 encoding variant를 올바르게 구분해야 한다.

Negotiation에서는 client가 지원한다고 광고한 coding과 server가 실제 선택한 coding이 일치해야 한다. 지원하지 않는 encoding을 보내거나 header와 body가 다르면 interoperability 문제가 된다. 중간 proxy가 재압축할 경우 checksum·ETag semantics도 검토한다.

Security policy는 protocol negotiation과 별개로 상한을 둔다. Peer가 coding을 지원한다고 해서 무제한 window나 decompressed size를 허용한다는 뜻은 아니다. HTTP parser와 decompressor의 resource budget을 하나의 request budget으로 연결한다.

## CHAPTER 27 · transport chunk 경계와 compression frame 경계를 같은 것으로 가정하지 않는다

Network는 compressed stream을 임의의 크기로 잘라 전달할 수 있다. Frame header가 두 read에 나뉘거나 하나의 read에 여러 frame이 들어올 수 있으므로 `read()` 반환 단위를 logical message 경계로 사용하면 안 된다. Decoder는 incremental input state를 유지해야 한다.

반대로 application이 chunk별로 compressor를 reset하면 transport fragmentation이 압축 ratio를 바꾸게 된다. Chunked transfer, TLS record, socket read size는 각각 다른 계층의 경계다. 어떤 경계에서 dictionary state를 유지할지 명시해야 한다.

테스트에서는 header 1 byte씩 전달, block 중간 분할, 여러 frame 한 번에 전달 같은 fragmentation case를 포함한다. 정상적인 큰 buffer 테스트만으로 streaming correctness를 증명할 수 없다.

## CHAPTER 28 · columnar layout은 같은 종류의 값을 모아 압축 모델을 더 쉽게 만든다

Row-oriented data는 서로 다른 type과 분포의 값이 교대로 나타나지만 columnar layout은 같은 field 값이 연속된다. 정수 column은 delta·bit packing, 반복 string은 dictionary encoding처럼 domain-specific representation을 적용하기 쉬워진다. 압축 전에 layout이 redundancy를 드러내는 셈이다.

이때 compression ratio는 codec만의 성능이 아니다. Sort order, row-group 크기, null pattern도 결과에 큰 영향을 준다. 같은 zstd level이라도 data organization이 다르면 전혀 다른 ratio가 나올 수 있다.

Query engine에서는 읽지 않는 column을 건너뛸 수 있어 I/O 절감과 압축 이득이 결합된다. 다만 작은 point update에는 rewrite amplification이 생길 수 있으므로 저장 workload와 access pattern을 함께 본다.

## CHAPTER 29 · compression benchmark는 ratio와 자원 사용을 같은 corpus에서 재야 한다

대표성 없는 한 파일로 codec을 비교하면 결론이 쉽게 뒤집힌다. Production의 size distribution, content type, compressibility를 반영한 corpus를 고정하고 동일한 hardware·thread 수·warmup 조건에서 측정해야 한다. Training dictionary를 썼다면 train/test corpus도 분리한다.

측정값은 compressed bytes, encode/decode throughput, p50·p99 latency, peak memory를 최소 단위로 둔다. Multi-thread codec은 worker 수와 queueing을 고정하고, storage benchmark라면 cache state도 통제한다. 결과 artifact에는 codec version과 parameter를 기록한다.

Regression threshold는 측정 noise보다 충분히 커야 한다. 작은 ratio 차이를 성능 개선으로 선언하기 전에 반복 run 분산을 확인하고, CPU와 I/O가 실제 서비스에서 어느 쪽 병목인지 연결해 해석한다.

## CHAPTER 30 · 압축의 최종 contract는 bytes saved가 아니라 bounded reconstruction이다

실전 압축 계층은 세 가지를 동시에 보장해야 한다. 첫째, 정상 입력은 정의된 format과 dictionary로 원본을 정확히 복원한다. 둘째, corrupted·malicious input은 memory와 CPU 상한을 넘기기 전에 실패한다. 셋째, frame·version·checksum metadata가 저장과 전송 계층의 recovery 규칙과 일치한다.

따라서 API에는 codec/level, maximum window, maximum output, dictionary identity, checksum policy, streaming behavior를 명시적으로 포함하는 편이 좋다. 숨은 default가 많을수록 다른 서비스나 버전에서 의미가 달라진다.

운영에서는 ratio만 dashboard에 두지 않는다. decode failure, resource-limit rejection, dictionary mismatch, compression CPU, expansion ratio tail을 함께 본다. 이 지표들이 있어야 압축을 성능 최적화가 아니라 안전한 데이터 변환 계약으로 관리할 수 있다.
