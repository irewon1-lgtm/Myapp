# PART 46 · Compression and Framing — redundancy, dictionary, entropy code, resource bounds

Lossless compression은 데이터를 `작게 만드는 함수`가 아니라 입력에 존재하는 반복·통계적 편향을 더 짧은 표현으로 바꾸고 decoder가 원본 byte sequence를 정확히 복원하게 하는 양방향 계약이다. 실제 시스템에서는 compression ratio뿐 아니라 encoder CPU, decoder memory, streaming latency, frame recovery, checksum, random access, untrusted-input resource bound가 함께 중요하다. Format과 algorithm을 분리하면 gzip/zstd 같은 container가 어떤 metadata와 safety boundary를 추가하는지 이해할 수 있다.

## CHAPTER 01 · 압축 가능성은 데이터의 redundancy와 source model에 달려 있다

모든 byte sequence를 항상 더 짧게 lossless 표현하는 algorithm은 존재할 수 없다. 어떤 입력 집합을 짧게 만들면 다른 입력에는 format/header overhead나 더 긴 code가 필요하다. Compression은 반복 substring, symbol frequency, context correlation 같은 structure를 model이 포착할 때 이득을 얻는다. 이미 암호화되거나 잘 압축된 data는 높은 entropy처럼 보여 추가 compression 이득이 작을 수 있다. `파일 종류`보다 실제 byte distribution과 model 적합성을 본다.

## CHAPTER 02 · lossless와 lossy는 decoder가 보존해야 할 invariant가 다르다

Lossless format은 decoded byte가 원본과 정확히 일치해야 하며 checksum/differential test로 bit-exact verification이 가능하다. Lossy codec은 인간 perception이나 application tolerance에 따라 일부 정보를 버리고 distortion metric·quality parameter를 사용한다. Source code, database backup, executable에는 bit-exact lossless가 필요하지만 image/audio/video는 domain-specific lossy가 적합할 수 있다. 두 종류를 `압축률이 높은/낮은 방식`으로만 비교하면 요구사항 자체를 놓친다.

## CHAPTER 03 · compression pipeline은 model과 code assignment를 분리해 생각할 수 있다

입력에서 어떤 pattern이 자주 나오는지 찾는 modeling 단계와, model output을 실제 bit sequence로 encode하는 entropy-coding 단계를 분리하면 algorithm을 분석하기 쉽다. Dictionary/LZ 계열은 반복 substring을 distance+length 같은 symbol로 바꾸고, Huffman/ANS류 coder는 symbol probability 차이를 bit length에 반영할 수 있다. Model이 나쁘면 entropy coder가 아무리 효율적이어도 ratio가 낮다. 반대로 좋은 match를 찾아도 metadata code가 비싸면 전체 이득이 줄어든다.

## CHAPTER 04 · LZ 계열 dictionary는 이전 byte sequence를 back-reference로 재사용한다

과거 window 안에 현재 위치와 같은 substring이 있으면 literal bytes를 다시 쓰는 대신 `얼마나 뒤에 있는가(distance), 몇 byte가 같은가(length)`를 기록할 수 있다. Decoder는 이미 복원한 output을 참조해 다음 byte를 재생한다. 따라서 compressed stream의 correctness는 distance가 현재 history 범위 안인지, overlapping copy semantics가 정의되어 있는지에 달려 있다. Malformed distance를 검증하지 않으면 out-of-bounds read/write가 될 수 있어 decoder가 untrusted parser라는 사실을 잊지 않는다.

## CHAPTER 05 · sliding window 크기는 compression ratio와 decoder memory를 직접 교환한다

더 큰 history window는 멀리 떨어진 반복을 찾을 기회를 늘리지만 decoder가 back-reference를 해결하기 위해 더 많은 history를 유지해야 한다. Zstandard format의 Window_Size가 decoder minimum buffer와 연결되는 이유다. HTTP zstd context에서는 RFC 9659가 interoperability와 resource control을 위해 8MB window 지원/생성 제한을 명시한다. Protocol에서 compression parameter를 허용할 때 peer가 선언한 거대한 window를 그대로 allocate하지 않고 policy limit를 적용한다.

## CHAPTER 06 · match finder가 encoder CPU와 ratio의 주요 cost center가 될 수 있다

현재 position에서 가장 긴 이전 match를 찾으려면 hash chain, binary tree, suffix-like structure 등 candidate search 전략이 필요하다. 더 많은 candidate를 탐색하면 좋은 match를 찾을 가능성이 높아지지만 encoder CPU와 memory access가 증가한다. Compression level은 흔히 search depth, strategy, window, parser policy를 조정하는 묶음이다. `level 19가 level 3보다 항상 좋다`가 아니라 saved bytes가 추가 CPU·latency 비용을 정당화하는지 workload별로 판단한다.

## CHAPTER 07 · greedy parse와 optimal parse는 같은 match set에서도 결과가 다를 수 있다

현재 위치에서 가장 긴 match를 즉시 선택하는 greedy strategy가 뒤의 더 좋은 sequence를 막을 수 있다. Lazy matching이나 dynamic-programming-like parser는 여러 선택의 future cost를 비교해 더 낮은 total bit cost를 찾을 수 있다. 그러나 parse search 자체가 비싸다. Encoder는 estimated entropy-code cost까지 포함해 literal과 match를 선택할 수 있다. Ratio-sensitive archival과 latency-sensitive RPC는 같은 parser budget이 적합하지 않다.

## CHAPTER 08 · Huffman code는 더 자주 나오는 symbol에 더 짧은 prefix code를 배정한다

Prefix-free code는 어떤 codeword도 다른 codeword의 prefix가 아니어서 decoder가 bitstream을 ambiguity 없이 읽을 수 있다. Symbol frequency가 크게 치우치면 fixed-width code보다 평균 bit 수를 줄일 수 있다. Code length table 자체도 stream에 전달하거나 predefined table을 써야 하므로 작은 block에서는 metadata overhead가 크다. Canonical Huffman은 code length만으로 deterministic code assignment를 재구성해 table representation을 줄이는 방식이다.

## CHAPTER 09 · ANS/FSE류 entropy coder는 probability state를 정수 state machine으로 표현한다

Modern format은 Huffman만 사용하지 않고 Asymmetric Numeral Systems 계열처럼 fractional-bit에 가까운 평균 code length를 효율적으로 구현하는 방법을 사용할 수 있다. Encoder/decoder는 normalized frequency table과 finite state transition을 공유한다. Table build cost와 decode table memory가 존재하며 small alphabet/block에서는 다른 coder가 나을 수 있다. Format spec을 구현할 때 algorithm 이름보다 serialized table constraint와 state transition validation을 정확히 따른다.

## CHAPTER 10 · literal stream과 match sequence를 분리하면 서로 다른 통계를 활용할 수 있다

Dictionary compressor는 match되지 않은 literal byte와 length/distance symbol이 서로 다른 distribution을 가진다. 각각 별도 entropy table로 coding하면 효율이 높아질 수 있다. Decoder는 여러 substream을 올바른 순서로 소비해 output position을 재구성한다. Corrupt count 하나가 stream boundary를 밀어 전체 parser state를 깨뜨릴 수 있으므로 각 section size와 symbol count를 range-check한다. `compressed payload 하나`가 내부적으로 여러 synchronized stream일 수 있다.

## CHAPTER 11 · frame은 compressed block을 독립적인 transport/storage 단위로 만든다

RFC 8878의 zstd data는 하나 이상의 frame으로 구성되고 frame은 다른 frame과 독립적으로 decompress할 수 있다. Frame header는 content size, dictionary id, window parameter, checksum flag 같은 decoding contract를 전달할 수 있다. Block는 frame 내부 processing unit이며 raw/RLE/compressed 같은 mode를 가질 수 있다. Frame boundary를 알면 concatenation, recovery, parallel processing policy를 설계할 수 있다. Application protocol은 frame과 message boundary가 같은지 별도로 정의한다.

## CHAPTER 12 · block size는 streaming latency와 coding context를 조절한다

큰 block은 frequency estimate와 match opportunity를 늘릴 수 있지만 encoder가 더 많은 input을 모아야 하고 error recovery granularity가 커질 수 있다. 작은 block은 first-byte latency와 parallelism에 유리할 수 있지만 header/table overhead 비중이 높다. Network streaming에서는 producer flush policy가 compression ratio와 latency를 직접 바꾼다. 매 작은 message마다 compressor를 flush하면 dictionary/context reuse가 끊길 수 있다.

## CHAPTER 13 · streaming decoder는 bounded memory로 무한 input을 처리할 수 있어야 한다

Format이 sequential stream을 지원한다면 decoder는 전체 compressed/uncompressed file을 memory에 올리지 않고 window와 current block state만 유지해 처리할 수 있다. RFC 8878은 bounded intermediate storage로 stream 처리 가능하도록 설계된다. API가 convenience 때문에 `readAllBytes()`를 사용하면 format의 bounded-memory 성질을 application이 다시 깨뜨릴 수 있다. Large upload/download는 incremental parse와 output backpressure를 연결한다.

## CHAPTER 14 · dictionary training은 small repeated records에서 startup overhead를 줄일 수 있다

많은 small payload가 유사한 schema/header/string을 공유하지만 각각 독립 frame이면 과거 message history를 사용할 수 없다. Pretrained dictionary에 자주 나타나는 pattern과 entropy table 정보를 넣어 첫 byte부터 reference하게 할 수 있다. Sender와 receiver가 정확히 같은 dictionary version/id를 공유해야 하며 mismatch는 decode failure다. Dictionary rollout은 schema/config artifact처럼 version·checksum·fallback을 관리한다.

## CHAPTER 15 · random access는 sequential compression과 자연스럽게 충돌한다

한 block을 decode하려면 이전 window history가 필요하면 file 중간으로 바로 seek해 복원할 수 없다. Random access가 필요하면 independent chunk/frame, periodic restart point, external index를 두어 dependency chain을 제한해야 한다. 더 자주 restart하면 compression ratio는 낮아질 수 있다. Log/archive format은 read pattern이 sequential scan인지 point lookup인지 먼저 정하고 compression layout을 선택한다.

## CHAPTER 16 · checksum은 corruption detection이고 cryptographic authenticity가 아니다

Compressed frame checksum은 storage/network bit corruption을 검출하는 데 유용하지만 attacker가 의도적으로 payload와 checksum을 함께 바꾸는 것을 막는 cryptographic MAC/signature와 목적이 다르다. Checksum이 optional이면 application integrity requirement가 format default보다 강할 수 있다. Decode 성공이 원본 authenticity를 증명하지 않는다. Backup artifact는 compression checksum 외에 cryptographic hash/signature를 별도 계층에 둘 수 있다.

## CHAPTER 17 · uncompressed size metadata는 allocation hint이지 무조건 신뢰할 값이 아니다

Frame이 decompressed content size를 선언할 수 있어 output buffer preallocation에 유용하지만 untrusted input이 비정상적으로 큰 size를 선언할 수 있다. Decoder가 size만큼 즉시 allocate하면 memory exhaustion 공격이 가능하다. Application-level maximum decompressed size와 expansion ratio limit를 적용하고 streaming sink를 사용한다. Unknown content size도 지원해야 하는 format에서는 output grow policy를 bounded하게 설계한다.

## CHAPTER 18 · compression bomb은 작은 input이 거대한 output/work를 유발하는 asymmetric resource 문제다

매우 반복적인 data나 nested archive는 compressed byte 수에 비해 decompressed byte·file count·CPU를 극단적으로 키울 수 있다. `upload는 10MB 이하` 같은 compressed-size 제한만으로 안전하지 않다. Decompressed total bytes, nesting depth, entry count, window size, CPU/time budget을 함께 제한한다. Parser가 limit에 도달했을 때 partial file을 commit하지 않도록 transactional extraction policy를 둔다.

## CHAPTER 19 · nested compression은 각 layer의 expansion limit를 곱할 수 있다

HTTP content encoding으로 압축된 archive 안에 다시 압축 file이 들어있고 각 file이 내부 compressed payload를 포함할 수 있다. Layer마다 개별 ratio는 합리적이어도 전체 expansion이 곱셈적으로 커질 수 있다. Pipeline은 cumulative output budget을 공유해야 한다. 각 decoder가 자기 input size만 기준으로 limit를 잡으면 전체 memory/disk 사용량을 통제하지 못한다.

## CHAPTER 20 · decompressor는 untrusted binary parser로 취급한다

Length, offset, table count, dictionary id, bitstream state는 모두 공격자가 조작할 수 있는 input이다. Integer overflow, out-of-bounds back-reference, oversized allocation, infinite loop를 방지하도록 spec range를 검증한다. Mature library를 사용하고 fuzzing·sanitizer corpus로 malformed frame을 지속 검증한다. Compression library CVE가 application business logic과 무관해 보여도 remote payload boundary라면 attack surface다.

## CHAPTER 21 · compression level은 ratio 하나가 아니라 time·memory parameter set이다

Higher level은 match search와 table optimization을 늘려 encoder CPU/memory를 크게 증가시킬 수 있지만 decoder cost는 비슷하거나 일부 parameter에서 증가할 수 있다. Archive는 encode-once/decode-many 특성 때문에 높은 encode cost를 감수할 수 있고 real-time API는 낮은 latency가 우선이다. Level별 compressed bytes, encode throughput, decode throughput, peak memory를 함께 benchmark한다. Default level을 근거 없이 최댓값으로 올리지 않는다.

## CHAPTER 22 · compression은 CPU를 써서 I/O byte를 줄이는 교환이다

Network/storage가 병목이면 compression CPU를 추가해 total latency와 비용을 줄일 수 있다. 반대로 이미 CPU-bound이고 local NVMe/network가 빠르면 compression이 오히려 느릴 수 있다. End-to-end time은 `compress CPU + transfer compressed bytes + decompress CPU`로 분해한다. Energy-limited mobile에서는 radio active time 감소와 CPU energy 증가를 함께 측정한다. P10 queueing과 P21 power model을 결합한다.

## CHAPTER 23 · 작은 payload에서는 frame/header/table overhead가 압축 이득보다 클 수 있다

수십 byte message는 repeated pattern을 학습할 sample이 적고 frame header와 entropy table metadata가 상대적으로 크다. 압축 후 payload가 더 커질 수도 있다. Threshold 이하 message는 raw 전송하거나 shared dictionary/context를 사용할 수 있다. Threshold를 평균 message 하나로 고정하지 말고 content type과 distribution을 반영한다. `Content-Encoding` 선택이 per-message latency에 미치는 영향을 실측한다.

## CHAPTER 24 · 이미 압축·암호화된 byte는 추가 compression 이득이 거의 없을 수 있다

JPEG/AV1/zip 같은 compressed data와 ciphertext는 low-redundancy byte distribution을 갖도록 설계되어 일반 lossless compressor가 반복을 찾기 어렵다. 다시 압축하면 header와 CPU만 늘 수 있다. File extension만 믿지 말고 magic/content type과 sample compressibility를 볼 수 있다. Mixed multipart payload는 compressible text와 incompressible media를 분리하는 것이 나을 수 있다.

## CHAPTER 25 · encrypt-then-compress와 compress-then-encrypt는 security semantics가 다르다

Ciphertext를 압축하기 어려우므로 실무에서는 plaintext compression 후 encryption이 자연스럽지만, attacker가 일부 plaintext를 제어하고 compressed length를 관찰할 수 있는 protocol에서는 length side channel이 생길 수 있다. Secret과 attacker-controlled text를 같은 compression context에 섞는지 threat model을 검토한다. 암호화 protocol이 compression을 금지하거나 context를 분리하는 이유가 있다. 성능을 위해 보안 경계를 임의로 합치지 않는다.

## CHAPTER 26 · HTTP Content-Encoding은 representation과 transport framing을 구분한다

HTTP `Content-Encoding: zstd`는 representation에 zstd content coding이 적용됐음을 의미하며 transfer framing과 같은 개념이 아니다. RFC 9659는 HTTP zstd의 Window_Size를 8MB 이하로 제한해 interoperability와 decoder memory bound를 강화한다. Client/server가 지원하는 encoding을 negotiation하고 cache key가 encoding variant를 올바르게 구분해야 한다. Unsupported parameter를 silent corruption으로 처리하지 않고 명확한 decode failure로 만든다.

## CHAPTER 27 · chunk boundary와 compression frame boundary는 반드시 같지 않다

Network transport가 데이터를 여러 packet/chunk로 나눠도 compression decoder는 bitstream state를 chunk 사이에 유지할 수 있다. 한 compressed frame이 여러 network read에 걸쳐 올 수 있고 한 read에 여러 frame이 들어올 수도 있다. Parser는 read() 호출 경계를 message boundary로 가정하지 않는다. Streaming state machine은 partial header, partial block, output backpressure를 안전하게 처리한다.

## CHAPTER 28 · columnar/repetitive layout은 compression model에 유리한 symbol locality를 만들 수 있다

같은 type/field의 값이 연속되면 delta, run-length, dictionary encoding과 entropy coding이 distribution을 더 잘 활용할 수 있다. Row-oriented object bytes를 그대로 압축하는 것보다 logical schema를 알고 전처리한 format이 더 높은 ratio와 query efficiency를 만들 수 있다. 이는 database/storage format에서 중요한 이유다. Generic compressor와 domain-specific encoding을 계층적으로 조합하되 중복 CPU와 random-access requirement를 고려한다.

## CHAPTER 29 · compression benchmark는 ratio·throughput·latency·memory를 같은 dataset에서 측정한다

Synthetic repeated string 하나로 algorithm을 비교하면 match finder에 극단적으로 유리한 결과가 나온다. Production data의 size/content distribution, cold/warm cache, core count, compression level, dictionary를 고정하고 p50/p99 encode/decode latency를 측정한다. Throughput만 보고 single-message tail을 놓치지 않고, ratio만 보고 peak memory를 놓치지 않는다. Version upgrade마다 format compatibility와 performance를 둘 다 regression test한다.

## CHAPTER 30 · compression 계약은 decoder resource upper bound와 exact recovery를 동시에 보장한다

Production compression format은 지원 frame/version/dictionary, 최대 window, 최대 decompressed bytes, nesting depth, checksum policy를 명시한다. Encoder 선택은 end-to-end CPU·network/storage 비용으로 결정하고 decoder는 malformed/unbounded input을 fail-closed한다. Random access가 필요하면 independent frame/index를 설계하고 streaming이면 bounded buffer와 backpressure를 사용한다. 성공 조건은 `압축률이 높다`가 아니라 원본 정확성·resource bound·latency·interoperability가 모두 검증되는 것이다.