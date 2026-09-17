# PART 45 · Entropy and CSPRNG — noise, seeding, reseeding, fork safety, bias

보안 난수의 품질은 “숫자가 골고루 보인다”로 판단할 수 없다. 예측 불가능한 entropy source를 수집하고, 건강성을 평가하고, DRBG state를 올바르게 instantiate/reseed하며, fork·VM clone·thread 환경에서도 state 중복을 막아야 한다. 이 PART는 **entropy provenance, CSPRNG state lifecycle, application token/nonce 사용과 bias**를 하나의 보안 계약으로 다룬다.

---

## CHAPTER 01 · unpredictability는 distribution 모양과 다른 보안 속성이다

난수열이 통계적으로 균등해 보여도 공격자가 내부 state나 seed를 알고 다음 값을 예측할 수 있다면 cryptographic 용도로 안전하지 않다. 반대로 CSPRNG 출력은 deterministic algorithm에서 나오지만 충분히 비밀인 seed와 안전한 state transition 덕분에 예측 저항성을 제공한다.

`random()` API 이름이나 histogram만으로 보안성을 판단하지 않는다. simulation용 PRNG와 security token generator는 요구사항이 다르다.

threat model에서 공격자가 어떤 output과 process state를 관찰할 수 있는지 적는다. token/key 생성에는 검증된 OS CSPRNG를 사용하고 자체 algorithm을 설계하지 않는다.

---

## CHAPTER 02 · min-entropy는 가장 가능성 높은 outcome을 기준으로 보수적으로 평가한다

entropy source 품질을 평균적인 다양성만으로 보면 특정 값이 과도하게 자주 나오는 bias를 놓칠 수 있다. min-entropy는 공격자가 가장 유리한 예측을 할 때 남는 불확실성을 보수적으로 표현하는 관점이다.

raw sensor bit 수가 곧 entropy bit 수가 아니다. 256-bit sample에 noise가 있어도 실제 독립 entropy가 몇 bit인지 별도 평가가 필요하다.

source qualification에서 raw sample distribution과 environmental condition을 보존한다. nominal lab 조건뿐 아니라 temperature·boot state를 포함한다.

---

## CHAPTER 03 · noise source는 deterministic system 밖의 불확실성을 수집한다

hardware timing jitter, physical noise 같은 source가 entropy pool의 입력이 될 수 있다. source마다 failure mode와 attacker influence 가능성이 다르므로 하나의 센서 값에 무조건 의존하지 않는다.

noise source output은 bias와 correlation을 가질 수 있어 raw bytes를 직접 key로 사용하는 것이 안전하지 않다. source driver와 OS pool이 conditioning과 health monitoring을 담당한다.

boot log에는 source availability와 failure를 민감한 raw entropy 없이 기록한다. entropy bytes 자체를 diagnostic에 남기지 않는다.

---

## CHAPTER 04 · health test는 entropy source가 stuck하거나 품질이 급락한 상태를 탐지한다

연속 동일 값, 비정상 repetition 같은 failure를 runtime health test로 탐지할 수 있다. health test는 source가 cryptographically perfect하다는 증명이 아니라 known failure mode를 빠르게 차단하는 안전장치다.

threshold를 느슨하게 잡으면 broken source를 통과시키고 너무 엄격하면 정상 noise를 false alarm으로 차단한다. source characterization과 연결해야 한다.

health failure count와 device/boot condition을 monitor한다. fail-open할지 fail-closed할지는 key generation 등 사용 목적에 따라 명시한다.

---

## CHAPTER 05 · conditioning은 biased raw input을 fixed-size seed material로 정리한다

hash·approved conditioning function은 여러 noisy sample을 압축해 output bit에 entropy를 분산시킬 수 있다. 하지만 input에 실제 entropy가 없으면 hash가 entropy를 새로 만들지는 못한다.

conditioner output 길이가 256bit라고 256bit entropy가 자동 보장되는 것이 아니다. source min-entropy와 conditioning construction을 함께 평가한다.

source→conditioner→DRBG seed chain을 문서화한다. 어느 layer가 실패하면 어떤 error가 application에 보이는지 정한다.

---

## CHAPTER 06 · DRBG는 secret internal state에서 예측 저항성 있는 stream을 생성한다

Deterministic Random Bit Generator는 entropy-rich seed로 state를 만들고 반복 generate마다 cryptographic state transition을 수행한다. output을 많이 만들기 위해 매번 physical noise를 직접 읽을 필요를 줄인다.

검증된 construction도 state가 노출되면 미래 또는 과거 output 보장에 영향이 생길 수 있다. reseed와 update policy가 compromise recovery 성질을 결정한다.

application은 DRBG internals보다 OS API를 사용하는 편이 안전하다. custom wrapper가 state serialization을 하지 않게 한다.

---

## CHAPTER 07 · instantiate는 entropy, nonce, personalization을 초기 state로 결합한다

DRBG 초기화는 단순 정수 seed 하나를 넣는 것보다 entropy input과 추가 context를 규칙에 따라 결합한다. 동일 entropy가 여러 instance에 반복되지 않도록 OS와 library가 instance separation을 관리해야 한다.

hard-coded seed, current timestamp, PID만으로 security RNG를 초기화하면 state space가 공격자에게 너무 작다. personalization은 entropy 대체물이 아니다.

startup test에서 low-entropy fallback이 없는지 확인한다. seed material을 로그나 crash dump에 노출하지 않는다.

---

## CHAPTER 08 · generate는 output과 internal state update를 함께 수행한다

secure generator는 단순히 state를 읽어 output만 내는 것이 아니라 generation 뒤 state를 변경해 이전 output만으로 future state를 복원하기 어렵게 한다. 구현이 state update를 누락하면 repeated output이나 compromise 확대가 생길 수 있다.

concurrent thread가 같은 state를 race로 갱신하면 duplicate block을 생성할 수 있으므로 locking 또는 per-thread instance policy가 필요하다.

output collision 통계보다 implementation thread-safety와 state lifecycle을 검증한다. generator state를 clone/copy 가능한 일반 object로 노출하지 않는다.

---

## CHAPTER 09 · reseed는 새로운 entropy로 오래된 internal state를 갱신한다

DRBG가 장기간 많은 output을 만들면 정책에 따라 fresh entropy를 받아 state를 갱신할 수 있다. reseed는 physical source cost와 security freshness 사이 trade-off를 가진다.

reseed 실패 시 silent weak fallback을 사용하면 안 된다. VM resume, entropy-source recovery 같은 lifecycle event가 reseed trigger가 될 수 있다.

reseed count와 error만 telemetry로 남기고 seed value는 절대 기록하지 않는다. rate spike가 source failure를 의미하는지 확인한다.

---

## CHAPTER 10 · compromise resistance는 state 노출 전후 어느 output이 보호되는지 구분한다

공격자가 현재 DRBG state를 획득했다고 가정할 때 과거 output을 역산할 수 있는지, fresh reseed 후 future prediction을 다시 막을 수 있는지가 중요한 property다. 모든 PRNG가 같은 backward/forward security를 제공하는 것은 아니다.

process memory dump와 crash report에 RNG state가 포함될 수 있어 diagnostic 접근도 threat model에 들어간다.

key/token lifetime과 RNG compromise recovery를 연결한다. state exposure가 의심되면 단순 process restart가 충분한지 fresh entropy 확보까지 확인한다.

---

## CHAPTER 11 · boot entropy는 system 초기화 순서 때문에 특별히 취약할 수 있다

부팅 초기는 disk/network/user event가 적고 hardware source가 아직 준비되지 않았을 수 있다. embedded device가 매 boot 같은 deterministic event만 사용하면 early key가 반복될 위험이 있다.

OS CSPRNG가 sufficiently initialized되기 전 blocking/ready semantics를 제공하는 이유다. application이 non-blocking fallback으로 timestamp seed를 만들면 이 보호를 우회한다.

early-boot key generation을 테스트하고 RNG-ready event 이전 호출 behavior를 확인한다. cloned image가 unique device entropy를 갖는지도 검증한다.

---

## CHAPTER 12 · getrandom 계열 API는 OS RNG readiness와 직접 연결된다

OS가 제공하는 random syscall은 파일 descriptor lifecycle 없이 kernel CSPRNG에서 bytes를 받을 수 있고 initialization 상태와 flag semantics를 정의한다. error/retry를 임의 local PRNG fallback으로 바꾸면 security가 약해진다.

large request는 interruption이나 partial result semantics를 확인해야 한다. signal handling과 boot blocking을 고려한다.

wrapper는 exact requested length를 채웠는지 검사한다. API failure를 telemetry에 남기되 generated bytes는 남기지 않는다.

---

## CHAPTER 13 · /dev/random과 /dev/urandom 이름만으로 현대 semantics를 추측하지 않는다

OS 버전에 따라 random device의 blocking과 entropy-accounting semantics가 달라질 수 있다. 오래된 조언을 그대로 적용해 unnecessary blocking이나 insecure fallback을 만들 수 있다.

application은 current platform 공식 API와 권장 CSPRNG를 사용한다. random bytes를 많이 소비한다고 pool entropy가 물리적으로 같은 양만큼 “고갈”된다는 단순 모델도 피한다.

platform/version별 behavior를 integration test한다. portable library는 OS adapter로 차이를 감싼다.

---

## CHAPTER 14 · hardware RNG는 entropy source 중 하나이지 무조건 신뢰할 root가 아니다

CPU/SoC hardware random instruction이나 HWRNG device는 높은 rate의 noise를 제공할 수 있지만 firmware bug, hardware failure, supply-chain trust를 고려해야 한다. OS는 여러 source를 mixing하고 health check를 수행할 수 있다.

application이 hardware instruction을 직접 key로 사용하면 OS의 conditioning과 source diversity를 우회한다. virtual machine에서는 instruction이 host implementation에 의존할 수 있다.

source failure를 simulation해 OS fallback이 안전한지 검증한다. hardware source raw output은 보안 log에 저장하지 않는다.

---

## CHAPTER 15 · source mixing은 한 source failure가 전체 RNG를 결정하지 않게 한다

여러 entropy source를 cryptographic pool에 결합하면 적어도 하나의 충분히 unknown source가 있을 때 robustness를 높일 수 있다. 단순 XOR도 context에 따라 성질이 있지만 검증된 OS mixing construction을 사용해야 한다.

attacker-controlled source를 추가한다고 existing secret entropy가 자동 파괴되어서는 안 된다. pool update와 estimate accounting을 분리한다.

source enable/disable 상태를 monitor하되 entropy content는 노출하지 않는다. degraded source diversity를 health signal로 본다.

---

## CHAPTER 16 · fork는 process memory와 RNG state를 복제할 수 있다

fork 순간 parent DRBG state가 child에 그대로 복사되면 두 process가 같은 state에서 같은 sequence를 생성할 위험이 있다. secure library는 fork detection과 reseed, OS RNG direct call로 이를 방지해야 한다.

PID를 output에 섞는 임시처리는 cryptographic proof가 아니다. fork 직후 key/token generation이 많은 server에서 특히 중요하다.

fork regression test에서 parent와 child sequence가 독립적인지 확인한다. library upgrade 시 fork hook behavior를 재검증한다.

---

## CHAPTER 17 · VM clone과 snapshot은 fork보다 더 큰 runtime state를 복제한다

가상 machine snapshot은 memory, kernel RNG state, application DRBG를 함께 복제할 수 있다. 같은 image를 여러 instance로 시작하면 clone마다 fresh entropy가 주입되지 않을 경우 token/key sequence가 겹칠 수 있다.

host가 virtual HWRNG나 unique boot entropy를 제공하고 guest OS가 clone event를 적절히 처리해야 한다. snapshot resume 뒤 reseed policy도 확인한다.

clone lab test에서 simultaneous instance가 같은 output prefix를 만들지 않는지 검증한다. machine identity만 바꿔 RNG state를 그대로 두지 않는다.

---

## CHAPTER 18 · thread safety는 generator state update를 직렬화하거나 분리해야 한다

하나의 DRBG object를 여러 thread가 lock 없이 사용하면 state transition이 lost update되어 duplicate output이 생길 수 있다. library가 내부 synchronization을 제공하는지 확인하거나 per-thread generator를 안전하게 seed한다.

per-thread instance를 같은 deterministic seed에서 만들면 race는 없어도 sequence가 중복된다. instance diversification이 필요하다.

concurrency stress로 duplicate block을 검사한다. performance를 위해 unsafe shared state를 허용하지 않는다.

---

## CHAPTER 19 · simulation PRNG는 reproducibility가 목표라 security RNG와 반대 요구를 가질 수 있다

Monte Carlo, game test에서는 fixed seed로 동일 sequence를 재현하는 것이 장점이다. cryptographic token에서는 같은 성질이 치명적이다. API와 type 이름을 분리해 서로 혼용하지 않게 한다.

simulation generator는 algorithm/version이 바뀌면 동일 seed의 sequence도 달라질 수 있다. scientific reproducibility가 필요하면 generator 종류와 version을 기록한다.

security code review에서 non-CSPRNG import를 자동 탐지한다. simulation seed를 production secret 생성에 재사용하지 않는다.

---

## CHAPTER 20 · test seed는 실패 재현성을 위해 기록하되 production entropy와 분리한다

property/fuzz test는 random input을 생성하지만 실패 시 seed를 남겨 같은 case를 재현해야 한다. 이 seed는 test determinism용이며 secret이 아니다.

production path에 test seed injection hook이 남아 있으면 predictable token을 만들 수 있다. dependency injection scope를 build/test 환경으로 제한한다.

CI failure report에는 generator type과 seed를 남긴다. production binary에서 deterministic override가 비활성인지 release gate로 검사한다.

---

## CHAPTER 21 · security token은 충분한 random bit와 secret handling을 동시에 요구한다

session ID, password reset token은 attacker가 guess하기 어려운 bit 수를 가져야 하며 URL/encoding으로 표현할 때 entropy를 잃지 않아야 한다. token이 random해도 log, referer, analytics에 노출되면 보안이 깨진다.

database에는 raw token 대신 hash를 저장하는 설계가 가능한지 검토한다. expiration과 one-time use도 guessing protection과 함께 필요하다.

collision뿐 아니라 brute-force budget으로 bit length를 정한다. token 값을 application log에 남기지 않는다.

---

## CHAPTER 22 · UUID는 version별 생성 semantics가 달라 security token과 동일하지 않다

UUID는 uniqueness를 위한 identifier 표준이며 모든 version이 cryptographic unpredictability를 목표로 하지 않는다. random 기반 variant도 bit 일부가 version/variant로 고정되어 실제 random bit 수를 계산해야 한다.

DB key에는 충분해도 password reset secret으로 적합한지는 threat model에 따라 판단한다. time-based identifier는 생성 시각과 node 정보를 노출할 수 있다.

identifier 요구를 uniqueness와 secrecy로 분리한다. UUID를 “랜덤해 보인다”는 이유로 auth secret으로 쓰지 않는다.

---

## CHAPTER 23 · nonce는 context에 따라 uniqueness가 secrecy보다 더 중요할 수 있다

암호 mode의 nonce는 key 아래 재사용 금지가 핵심인 경우가 많다. random nonce를 사용해도 collision probability를 관리해야 하고 counter nonce는 state persistence가 필요하다. secret일 필요가 없는 nonce를 숨기는 것보다 uniqueness proof가 중요하다.

process restart나 multi-node deployment에서 counter range가 겹칠 수 있다. random choice는 충분한 bit space와 CSPRNG가 필요하다.

key ID와 nonce allocation policy를 함께 관리한다. reuse detection을 telemetry로 두되 sensitive payload는 노출하지 않는다.

---

## CHAPTER 24 · salt는 password hash의 precomputation 재사용을 막기 위한 public random value다

salt는 user마다 충분히 unique하게 생성해 동일 password도 다른 hash를 만들게 한다. 일반적으로 secret일 필요는 없지만 weak predictable short salt는 collision이 많아 precomputation 방어가 약해진다.

salt를 global constant로 두거나 username만 deterministic하게 사용하면 목적을 잃는다. password hashing algorithm과 cost parameter도 함께 저장한다.

account migration 시 old salt format을 명시적으로 versioning한다. salt와 encryption key 역할을 혼동하지 않는다.

---

## CHAPTER 25 · key generation은 RNG 품질과 key algorithm requirement가 직접 연결된다

symmetric key나 private key는 required entropy와 format 제약을 만족해야 한다. 어떤 algorithm은 단순 random bytes가 아니라 rejection 또는 structured generation이 필요하다.

weak boot entropy나 deterministic test seed가 key generation path에 들어가면 암호 algorithm이 강해도 전체 security가 무너진다. key material은 memory/log/dump handling도 엄격해야 한다.

key-generation API는 vetted crypto library를 사용한다. integration test에서 key bytes를 출력하지 않고 format과 error만 검증한다.

---

## CHAPTER 26 · modulo reduction은 uniform RNG를 작은 range로 바꿀 때 bias를 만들 수 있다

0..255 균등 byte를 `% 10`으로 줄이면 256이 10으로 나누어 떨어지지 않아 일부 결과가 더 자주 나온다. security token alphabet selection, shuffle index에서 이런 작은 bias가 누적될 수 있다.

rejection sampling은 range가 고르게 나뉘는 구간만 사용해 bias를 제거할 수 있다. standard library의 uniform-range API를 우선 사용한다.

통계 test는 implementation error를 찾는 보조 수단이지 CSPRNG security proof가 아니다. range mapping algorithm을 code review한다.

---

## CHAPTER 27 · secure shuffle은 각 permutation이 동일 probability를 갖도록 index를 선택한다

Fisher-Yates 계열은 단계별로 올바른 range에서 uniform index를 선택해야 permutation이 균등해진다. 잘못된 `%` bias나 매 단계 전체 range를 선택하는 구현은 특정 permutation을 더 자주 만든다.

카드·lottery처럼 fairness가 보안 요구라면 CSPRNG와 unbiased mapping이 모두 필요하다. simulation shuffle은 reproducible PRNG가 더 적합할 수 있다.

small n에서 모든 permutation frequency를 exhaustive test해 algorithm bug를 찾는다. production secret shuffle seed는 기록하지 않는다.

---

## CHAPTER 28 · RNG observability는 상태를 노출하지 않고 health만 보여야 한다

운영자는 entropy source failure, reseed error, RNG unavailable 상태를 알아야 하지만 seed와 internal state, generated secret을 log하면 안 된다. metric은 count와 readiness, source status만 제공한다.

debugging 편의를 위해 random bytes를 dump하는 기능은 incident 때 가장 위험하다. crash dump 접근 정책도 RNG state threat model에 포함한다.

telemetry schema를 security review한다. support bundle에서 memory/secret이 자동 포함되지 않게 한다.

---

## CHAPTER 29 · RNG integration failure는 algorithm보다 lifecycle과 fallback에서 자주 발생한다

검증된 CSPRNG를 사용해도 boot 전 호출, fork state duplication, VM clone, insecure fallback, modulo bias 때문에 application security가 깨질 수 있다. RNG subsystem만 unit test하고 integration lifecycle을 테스트하지 않으면 놓친다.

error가 날 때 `Math.random()` 같은 대체 경로로 조용히 내려가는 코드는 최악의 패턴이다. security operation은 fail closed가 적합한 경우가 많다.

fork/clone/low-entropy/failure fault를 integration test에 넣는다. generated output 자체가 아니라 uniqueness와 error behavior를 검사한다.

---

## CHAPTER 30 · entropy contract는 source provenance에서 application mapping까지 이어진다

안전한 random pipeline은 어떤 noise source를 OS가 수집하고, CSPRNG가 언제 ready가 되며, fork·clone에서 state를 어떻게 분리하고, application이 token·nonce·range로 어떻게 변환하는지 연결해 설명할 수 있어야 한다.

자체 PRNG, timestamp seed, silent fallback을 금지하고 security random과 reproducible simulation random API를 분리한다. telemetry는 health만 노출하고 secret state는 보호한다.

최종 검증은 boot, fork, VM clone, concurrency, range mapping을 실제로 실행해 **동일 state 재사용과 predictable fallback이 없고 요구한 entropy/uniqueness가 end-to-end로 유지되는지** 확인하는 것이다.
