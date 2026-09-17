# PART 45 · Entropy and CSPRNG — noise, seeding, reseeding, fork safety, bias

컴퓨터의 `난수`는 하나의 개념이 아니다. 물리적·환경적 noise에서 예측하기 어려운 entropy를 수집하는 단계, raw noise의 품질을 검사·condition하는 단계, seed로부터 deterministic하게 많은 random bit를 생성하는 DRBG 단계가 분리된다. 보안용 random output은 단순히 값이 고르게 보이는지보다 **공격자가 내부 state와 미래·과거 output을 얼마나 예측할 수 있는가**가 핵심이다.

## CHAPTER 01 · statistical irregularity와 cryptographic unpredictability는 다른 요구다

숫자 분포가 uniform해 보이고 간단한 통계 test를 통과해도 공격자가 다음 값을 계산할 수 있다면 session token·key 생성에는 사용할 수 없다. Linear congruential generator처럼 simulation에는 충분한 generator가 state 일부 노출로 쉽게 예측될 수 있다. 반대로 CSPRNG는 deterministic algorithm이지만 secret internal state와 안전한 construction을 통해 next-output prediction을 어렵게 만든다. `random-looking`이라는 시각적 인상과 security property를 분리해야 한다. API 선택은 simulation reproducibility인지 cryptographic unpredictability인지 먼저 정한다.

## CHAPTER 02 · entropy는 byte 수가 아니라 attacker의 불확실성을 측정한다

128byte noise sample이 있다고 해서 1024bit entropy가 있다는 뜻은 아니다. Sample 사이 correlation이나 bias가 크면 가능한 raw bitstring 수보다 실제 uncertainty가 훨씬 작다. NIST SP 800-90B는 noise source의 min-entropy와 health testing을 별도 문제로 다룬다. Min-entropy는 가장 가능성 높은 결과를 공격자가 맞힐 확률과 연결되는 보수적 지표다. Entropy estimate를 단순 압축률이나 `0/1 비율이 절반`으로 대체하지 않는다.

## CHAPTER 03 · noise source는 deterministic software state와 다른 물리·환경 정보에서 온다

Hardware RNG, timing jitter, device interrupt timing 같은 source가 entropy input 후보가 될 수 있지만 source마다 failure mode와 adversarial influence 가능성이 다르다. Noise source가 고장 나 constant나 predictable pattern을 내도 software가 이를 `random`이라고 무조건 믿으면 전체 generator가 약해진다. Entropy source 설계는 raw sample acquisition, source assumptions, health test, conditioning을 하나의 검증 단위로 다룬다. 여러 source를 섞는다고 각 source failure가 자동으로 탐지되는 것도 아니다.

## CHAPTER 04 · entropy source health test는 catastrophic failure를 runtime에서 감시한다

Noise source가 stuck되어 같은 값만 반복하거나 분포가 갑자기 변하면 startup validation만으로는 잡지 못할 수 있다. NIST 800-90B는 continuous health testing 요구를 포함해 source가 operation 중 비정상 상태로 바뀌는지 감지한다. Health test threshold는 정상 random variation을 failure로 오인하지 않으면서 catastrophic degradation을 검출해야 한다. Health test 실패 시 output을 계속 제공할지, source를 격리할지, system initialization을 막을지 정책이 필요하다.

## CHAPTER 05 · conditioning은 raw noise를 균일하게 보이게 만드는 것과 entropy를 창조하는 것을 구분한다

Hash/cryptographic conditioning function은 biased/correlated raw input을 fixed-length bitstring으로 압축하고 여러 source를 mix할 수 있다. 그러나 입력에 존재하지 않는 entropy를 algorithm이 새로 만들 수는 없다. 32bit min-entropy input을 hash해 256bit output을 얻어도 256bit entropy가 생긴 것이 아니다. Conditioning output length와 assessed entropy를 분리한다. Security strength는 seed material에 실제 들어온 entropy upper bound를 넘을 수 없다.

## CHAPTER 06 · DRBG는 seed와 internal state에서 deterministic output stream을 만든다

NIST SP 800-90A의 deterministic random bit generator는 hash, HMAC, block cipher 기반 construction처럼 defined mechanism을 사용해 internal state를 갱신하면서 output을 만든다. 동일 algorithm과 동일 initial state라면 output은 deterministic하다. 보안성은 state가 충분한 entropy로 초기화되고 attacker에게 비밀이며 update rule이 cryptographically strong하다는 데서 온다. `DRBG=진짜 랜덤`이 아니라 entropy를 효율적으로 확장하는 state machine으로 이해한다.

## CHAPTER 07 · instantiate 단계는 entropy input·nonce·personalization을 state로 결합한다

DRBG instance 생성은 단순 `seed 변수에 정수 하나 저장`이 아니다. Construction이 요구하는 entropy input과 optional nonce/personalization을 받아 initial internal state를 만든다. Personalization string은 domain separation과 instance differentiation에 도움을 줄 수 있지만 entropy 부족을 보충하지 않는다. Seed material 구성 규칙을 임의로 줄이거나 predictable timestamp 하나만 넣으면 nominal state width와 실제 security strength가 달라진다.

## CHAPTER 08 · generate는 output을 내면서 internal state도 전진시켜야 한다

CSPRNG가 같은 state에서 매 호출 같은 block을 반복하면 catastrophic failure다. Secure construction은 output 생성 전후 state를 update해 이전 output만으로 현재 state를 역산하기 어렵게 설계한다. API caller는 generator 내부 state를 직접 serialization/logging하지 않는다. Crash dump, debug snapshot, VM snapshot이 state를 복제할 수 있다는 점도 threat model에 포함한다.

## CHAPTER 09 · reseed는 새로운 entropy를 기존 DRBG state에 주입한다

장시간 실행되는 generator는 일정 policy나 request에 따라 entropy source에서 새 input을 받아 state를 갱신할 수 있다. Reseed는 `entropy가 매 byte 소모되므로 계속 채워야 한다`는 단순 모델과 다르다. Initialized CSPRNG는 deterministic expansion으로 많은 output을 만들 수 있고 reseed는 state compromise recovery, long-lived assurance 같은 목적을 가진다. Construction별 reseed interval과 prediction-resistance option을 따른다.

## CHAPTER 10 · prediction resistance와 backtracking resistance는 시간 방향이 다르다

현재 internal state가 공격자에게 노출됐을 때 과거 output을 복원하기 어렵게 만드는 성질과, 새로운 entropy reseed 후 미래 output을 다시 예측하기 어렵게 만드는 성질은 구분된다. State compromise가 한 번 있었다고 영원히 모든 future output이 깨져야 하는 것은 아니며, 반대로 current state가 과거 output 전체를 재생 가능하게 만드는 design은 forensic exposure를 키운다. DRBG construction의 state update·reseed property를 threat model에 맞춰 평가한다.

## CHAPTER 11 · boot 초기 entropy는 persistent system에서 특별한 위험 구간이다

부팅 직후에는 interrupt/device activity가 아직 적고 clock/device state가 predictable할 수 있다. Kernel CSPRNG가 충분히 initialize되기 전에 cryptographic key나 long-lived token을 만들면 서로 다른 boot에서 output collision/predictability risk가 생긴다. Linux `getrandom()` 기본 동작은 urandom source가 초기화되지 않았을 때 기다릴 수 있어 이 경계를 API에 반영한다. Early-boot daemon이 nonblocking fallback로 weak random을 만들지 않는지 확인한다.

## CHAPTER 12 · getrandom()은 file path 없이 kernel CSPRNG output을 받는 명시적 interface다

Linux `getrandom()`은 기본적으로 urandom source를 사용하고 pool이 initialize되기 전에는 blocking할 수 있다. Small initialized requests는 signal interruption semantics도 명확하게 정의되어 있다. Caller는 return value를 확인해 partial result와 error를 처리한다. `/dev/urandom` file descriptor를 열고 관리하는 것보다 startup initialization race를 다루기 쉬운 interface가 될 수 있다. Cryptographic application은 platform에서 권장하는 high-level secure random API를 우선 사용한다.

## CHAPTER 13 · /dev/random과 /dev/urandom을 `강한/약한 난수`로 단순 분류하면 현재 Linux 동작을 오해한다

Kernel RNG가 초기화된 뒤 `/dev/urandom`은 CSPRNG output을 지속적으로 제공한다. `/dev/random`의 blocking semantics와 entropy accounting은 별도 interface contract다. 필요한 key security strength보다 수 MB raw entropy를 읽어야 안전하다는 생각은 잘못된 자원 모델이다. Application이 직접 device 선택을 하기에 앞서 `getrandom()` 또는 language crypto RNG API의 current platform guidance를 확인한다.

## CHAPTER 14 · hardware RNG output은 자동으로 trusted entropy가 아니다

Linux hardware RNG framework 문서는 `/dev/hwrng` raw data 자체가 fitness test로 검증되지 않을 수 있음을 명시한다. Hardware source는 malfunction, design flaw, compromise 가능성을 가진다. Kernel/system software는 여러 source를 mix하고 health/conditioning policy를 적용할 수 있다. Application이 `/dev/hwrng`를 직접 key material로 읽는 것보다 OS CSPRNG를 사용하는 이유가 이 trust layering에 있다.

## CHAPTER 15 · entropy mixing은 한 source의 bias를 숨기는 것과 failure를 탐지하는 것을 구분한다

여러 source를 cryptographic hash로 mix하면 하나의 strong unknown component가 전체 state unpredictability에 기여할 수 있다. 하지만 dead source가 계속 zero를 내는 사실은 hash output만 보면 보이지 않는다. Source별 health monitoring과 aggregate conditioning은 역할이 다르다. Audit에서는 어떤 source가 entropy credit을 받는지, failure 시 credit이 중지되는지, untrusted auxiliary input이 state를 약화시키지 않는지 본다.

## CHAPTER 16 · process fork는 user-space PRNG state를 byte-for-byte 복제할 수 있다

Parent process memory가 fork로 COW 복제되면 user-space PRNG internal state도 자식에 같은 값으로 시작할 수 있다. Parent와 child가 같은 generate sequence를 실행하면 동일 token/nonce를 만들 위험이 있다. Secure library는 fork detection, atfork reseed, kernel RNG direct request 같은 strategy를 사용해야 한다. VM/container process 생성 방식이 fork-like memory clone인지 확인한다. `PID가 다르니 random도 다르다`는 보장이 없다.

## CHAPTER 17 · VM snapshot/clone은 RNG state와 entropy history까지 복제할 수 있다

실행 중 VM을 snapshot한 뒤 여러 clone을 만들면 kernel/user DRBG state가 동일 snapshot 시점에서 출발할 수 있다. Host가 virtual RNG device로 fresh entropy를 공급하거나 guest가 clone identity 변화 후 reseed하는 mechanism이 필요할 수 있다. Deterministic test VM image를 production key generation에 그대로 복제하는 것은 위험하다. Virtualization P20의 snapshot consistency에 randomness uniqueness를 추가한다.

## CHAPTER 18 · thread safety와 cryptographic strength는 서로 다른 RNG property다

하나의 CSPRNG object를 여러 thread가 동시에 호출할 때 state update가 race하면 duplicate/corrupted output이 생길 수 있다. Lock/atomic serialization, thread-local generator, kernel API direct usage 등으로 concurrency를 관리한다. Thread-local state를 만들 때도 각 instance seed가 독립적이어야 한다. `thread-safe=true`는 output unpredictability를 보장하지 않고, `cryptographically secure=true`도 API object가 concurrency-safe하다는 뜻은 아니다.

## CHAPTER 19 · simulation PRNG는 재현성이 장점이고 CSPRNG와 목적이 반대일 수 있다

Monte Carlo, fuzz scheduling, game procedural generation은 같은 seed에서 같은 sequence를 재현해야 debugging이 가능하다. 이런 workload에는 빠른 non-crypto PRNG가 적합할 수 있다. Security token에는 seed를 공개해도 output 예측이 어려운 CSPRNG property가 필요하다. 하나의 `Random` API를 두 목적에 공유하지 않고 type/name/module 수준에서 분리하면 misuse를 줄일 수 있다.

## CHAPTER 20 · deterministic test seed는 실패 재현 artifact다

Randomized property test가 실패하면 사용한 seed와 generator version을 저장해야 같은 input sequence를 재현할 수 있다. Test harness는 production CSPRNG를 무조건 사용하지 않고 deterministic PRNG를 injection할 수 있다. 반대로 production path가 test fixed seed를 실수로 사용하지 못하게 build/config boundary를 둔다. Reproducibility와 security randomness를 명시적으로 분리한다.

## CHAPTER 21 · token은 충분한 entropy와 encoding length를 함께 계산해야 한다

Session/reset token이 128bit random source에서 생성돼도 hex/base64 encoding이 잘못 잘려 실제 후보 공간이 줄 수 있다. Token byte length, encoding alphabet, truncation, URL normalization을 끝까지 추적한다. Database에 hash만 저장할지 raw token을 저장할지도 breach model에 영향을 준다. User-readable short code는 collision/guessing rate가 높으므로 rate limit과 expiry를 함께 설계한다.

## CHAPTER 22 · UUID uniqueness requirement와 secret-token unpredictability는 다르다

일부 UUID version은 randomness 또는 timestamp/namespace 조합으로 collision avoidance를 목표로 하지만 모든 UUID가 secret credential로 적합한 것은 아니다. Identifier가 공개돼도 안전해야 하는 시스템과 identifier 자체가 bearer secret인 시스템을 구분한다. UUID library가 cryptographic RNG를 사용하는지 version semantics를 확인한다. `길고 랜덤처럼 보임`만으로 authentication token으로 승격하지 않는다.

## CHAPTER 23 · nonce는 종종 unpredictable보다 unique가 더 핵심인 cryptographic input이다

Encryption mode에 따라 nonce reuse가 catastrophic할 수 있지만 nonce가 공개되는 것은 정상일 수 있다. `random nonce`를 사용한다면 birthday collision probability와 RNG quality를 고려하고, counter/structured nonce가 더 강한 uniqueness guarantee를 줄 수도 있다. Key가 바뀌면 nonce namespace가 어떻게 reset되는지도 명시한다. Nonce requirement를 salt·IV·key와 같은 `랜덤 값`으로 뭉개지 않는다.

## CHAPTER 24 · salt는 password/hash collision precomputation을 막기 위한 공개 unique input이다

Password hashing salt는 user마다 충분히 unique하게 생성되어 같은 password가 같은 hash로 보이지 않게 하고 precomputed table 재사용을 어렵게 한다. Salt는 secret일 필요가 없고 database에 hash와 함께 저장할 수 있다. CSPRNG를 사용하면 collision risk를 낮출 수 있지만 목적은 encryption key entropy와 다르다. Salt length와 password KDF cost parameter를 별도 관리한다.

## CHAPTER 25 · key generation은 requested key size와 entropy strength를 혼동하지 않는다

3072-bit RSA private representation이 3072bit raw entropy를 요구한다는 뜻은 아니다. Algorithm security strength와 key generation procedure가 필요 entropy를 결정한다. OS CSPRNG에서 충분한 seed/output을 받고 validated crypto library keygen을 사용한다. Application이 entropy pool byte를 직접 모아 큰 integer를 만드는 custom key generator는 bias, range, primality, side-channel 문제가 추가된다.

## CHAPTER 26 · modulo reduction은 uniform random integer에 bias를 만들 수 있다

0..255 uniform byte를 `% 10`으로 줄이면 256이 10으로 나누어떨어지지 않아 일부 결과가 더 자주 나온다. Security-sensitive range sampling은 rejection sampling처럼 leftover range를 버려 uniformity를 유지하는 방법을 사용한다. Bias가 작아 보여도 signature nonce나 lottery-like selection에서는 누적 공격 가능성이 생길 수 있다. Standard library의 secure uniform-range API를 재구현하지 않는다.

## CHAPTER 27 · Fisher-Yates shuffle도 index sampling이 unbiased일 때만 uniform하다

Correct shuffle algorithm을 써도 각 step에서 random index를 biased modulo로 고르면 permutation distribution이 uniform하지 않다. 또한 같은 seed 재사용이 attacker에게 알려지면 shuffle 결과를 예측할 수 있다. Simulation shuffle은 deterministic seed가 유용하고 security-sensitive ordering은 CSPRNG를 요구할 수 있다. Algorithm proof와 RNG quality를 분리해서 검증한다.

## CHAPTER 28 · RNG health metric은 output 값 자체를 production log에 남기지 않는다

Entropy source health failure, pool initialization time, reseed count 같은 operational metric은 유용하지만 raw entropy sample, DRBG state, generated key/token을 로그로 남기면 security를 파괴한다. Diagnostic interface는 secret state를 노출하지 않고 status/counter만 제공한다. Crash dump에도 RNG internal state가 들어갈 수 있으므로 sensitive process dump access를 제한한다. Observability가 randomness secret을 유출하지 않게 한다.

## CHAPTER 29 · random failure는 duplicate output보다 초기화 실패·state clone·misuse로 나타날 수 있다

CSPRNG algorithm이 수학적으로 깨지지 않아도 early boot uninitialized state, fixed test seed, fork clone, VM snapshot, modulo bias, accidental non-crypto PRNG 사용으로 security incident가 생길 수 있다. Audit checklist는 generator name보다 seed source·initialization gate·fork handling·API selection·range conversion을 따라간다. Integration test에서 process fork와 VM/container clone scenario를 포함한다.

## CHAPTER 30 · randomness 계약은 entropy origin·generator state·consumer requirement를 분리한다

Production design은 entropy source가 무엇이고 health/conditioning을 누가 담당하는지, OS CSPRNG가 언제 initialized되는지, application이 어떤 secure API를 사용하는지 기록한다. Consumer마다 key·nonce·salt·token·simulation seed의 요구를 분리하고 type/API로 misuse를 줄인다. Fixed seed는 test에서만 허용하고 fork/snapshot 후 state duplication을 검증한다. Randomness는 `무작위처럼 보임`이 아니라 예측·복제·bias에 대한 명시적 security contract다.