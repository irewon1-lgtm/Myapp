# PART 44 · Randomness와 reproducibility — 난수를 상태·분포·보안 계약으로 다루기

난수는 “매번 다른 숫자”를 만드는 기능처럼 보이지만 실제 프로그램에서는 세 가지 전혀 다른 목적이 섞인다. 테스트와 simulation에서는 다시 재현 가능한 pseudo-random sequence가 필요하고, sampling에서는 원하는 확률분포를 보존해야 하며, token·password reset처럼 공격자가 결과를 예측하면 안 되는 영역에서는 cryptographic randomness가 필요하다. 이 목적들을 하나의 `random()` 호출로 통합하면 correctness와 security가 동시에 깨질 수 있다. 핵심은 **난수 source의 상태·분포·재현성·위협 모델을 사용 목적에 맞게 고정하는 것**이다.

---

## CHAPTER 01 · randomness는 값 하나가 아니라 생성 규칙의 속성이다

난수 API가 반환한 값 `0.314...` 하나만 보고 그 값이 “좋은 난수”인지 판단할 수 없다. 중요한 것은 여러 호출에서 결과가 어떤 분포를 따르고, 이전 상태에서 다음 상태가 어떻게 결정되며, 외부 관찰자가 그 sequence를 예측할 수 있는지다. 따라서 randomness를 평가할 때 개별 숫자의 모양보다 generator와 사용 방식의 contract를 본다.

Pseudo-random number generator(PRNG)는 내부 상태를 deterministic하게 갱신하면서 겉보기에는 불규칙한 sequence를 만든다. 같은 algorithm과 같은 초기 상태라면 같은 sequence를 다시 만들 수 있다. 이 특성은 simulation과 test 재현성에 유용하지만 security token처럼 예측 불가능성이 필요한 곳에서는 오히려 위험할 수 있다.

진짜 물리적 entropy source와 software PRNG의 관계도 구분한다. Operating system은 hardware event와 system state에서 entropy를 수집해 secure random interface를 제공할 수 있고 application은 이를 직접 구현하기보다 검증된 API를 사용한다. “무작위처럼 보인다”와 “공격자가 예측하기 어렵다”는 전혀 다른 보장이다.

---

## CHAPTER 02 · PRNG는 현재 상태가 다음 sequence 전체를 결정할 수 있다

일반적인 PRNG는 hidden state를 가지고 있고 호출할 때마다 state transition을 수행한 뒤 output을 만든다. 따라서 프로그램 어디에서 난수를 한 번 더 소비하면 뒤의 모든 sequence가 달라질 수 있다. 같은 seed를 사용했는데 test 결과가 달라졌다면 seed만 볼 것이 아니라 호출 순서와 generator 공유 여부까지 확인해야 한다.

Global generator 하나를 application 전체가 공유하면 unrelated feature가 추가한 random call 때문에 simulation 결과가 바뀔 수 있다. 중요한 실험은 dedicated generator instance를 만들고 ownership을 명시적으로 전달하는 편이 재현성이 좋다. Test A와 Test B가 같은 global state를 건드리면 실행 순서에 따라 서로 영향을 줄 수도 있다.

Generator state snapshot을 저장하면 중간 지점부터 같은 sequence를 재개할 수 있는 경우도 있다. 하지만 algorithm/version compatibility가 장기적으로 보장되는지는 별도 문제다. 장기 artifact에는 seed뿐 아니라 runtime/library version과 algorithm identity도 기록해야 완전한 재현 가능성이 높아진다.

---

## CHAPTER 03 · seed는 재현성을 위한 입력이지 random 품질을 높이는 주문이 아니다

Seed는 PRNG의 초기 상태를 정하는 입력이다. 같은 환경에서 동일 seed를 사용하면 같은 random stream을 재현할 수 있어 flaky test와 simulation bug를 조사하기 쉽다. 실패한 property test에서 seed를 기록하면 개발자가 같은 생성 sequence를 다시 실행해 원인을 좁힐 수 있다.

Seed를 현재 시각으로 매번 바꾸면 실행마다 다른 sequence를 얻을 수 있지만 실패 재현이 어려워진다. Test에서는 기본적으로 deterministic seed를 쓰고, fuzz run에서 임의 seed를 선택하더라도 실패 report에 정확한 seed를 남기는 전략이 강하다. “랜덤 테스트라 다시 안 난다”는 검증 품질이 아니다.

반대로 cryptographic secret을 seed 하나로 만들거나 작은 seed space에서 secure token을 생성하면 공격자가 가능한 seed를 탐색해 결과를 예측할 수 있다. Security 영역에서는 OS가 제공하는 CSPRNG를 사용하고 일반 PRNG seed 기법과 분리한다.

---

## CHAPTER 04 · 보안 난수는 예측 저항성과 entropy source가 핵심이다

Password reset token, session ID, API key처럼 값 자체가 권한을 가진다면 공격자가 다음 값을 추측하기 어려워야 한다. 일반 simulation용 PRNG는 통계적으로 그럴듯해 보여도 내부 state 일부가 노출되거나 output을 충분히 관찰하면 미래 값을 예측할 수 있는 algorithm일 수 있다. 이런 영역에서는 Python의 security-oriented random API와 OS entropy source를 사용한다.

Secure token의 bit 수와 encoding도 threat model에 맞춰야 한다. 6자리 숫자는 사용성이 좋지만 가능한 경우의 수가 백만 개뿐이므로 online rate limit 없이 강한 secret으로 쓸 수 없다. 충분한 entropy를 가진 random bytes를 URL-safe encoding으로 표현하는 방식은 훨씬 큰 search space를 만들 수 있다.

Token을 log나 analytics에 그대로 남기면 난수 quality가 아무리 좋아도 secret이 노출된다. 생성·전달·저장·비교·lifetime까지 전체 secret lifecycle을 설계해야 한다. Randomness는 security control의 한 층일 뿐 authorization과 rate limiting을 대신하지 않는다.

---

## CHAPTER 05 · sampling은 uniformity와 bias를 알고리즘 수준에서 검증한다

1..10 사이 숫자를 뽑는다고 할 때 source random integer를 `% 10`으로 줄이는 방식은 source range가 10의 배수가 아니면 일부 결과가 더 자주 나오는 modulo bias를 만들 수 있다. 검증된 standard library sampling API는 이런 세부사항을 처리하므로 직접 변환 알고리즘을 만들기 전에 제공 기능을 사용한다.

Weighted sampling에서는 weight normalization과 floating precision, zero/negative weight 정책을 정한다. A=1, B=2라면 장기적으로 B가 약 두 배 선택되는 분포를 기대하지만 작은 sample에서 정확히 2:1이 나와야 하는 것은 아니다. Statistical behavior와 deterministic assertion을 혼동하지 않는다.

Sampling test는 개별 run의 exact count보다 명백한 bias를 탐지하는 통계적 test나 deterministic reference algorithm을 사용할 수 있다. 너무 좁은 tolerance는 정상 random variation 때문에 flaky하고 너무 넓은 tolerance는 실제 bias를 놓친다. 가능한 경우 random algorithm 자체보다 standard library contract에 의존한다.

---

## CHAPTER 06 · shuffle은 permutation contract와 state consumption을 함께 가진다

Shuffle의 핵심은 입력 element를 잃거나 추가하지 않고 순서만 바꾸는 permutation을 만드는 것이다. Test에서는 output의 multiset이 input과 같고 동일 seed에서 같은 ordering이 재현되는지 확인할 수 있다. “매번 원래 순서와 달라야 한다”는 assertion은 잘못이다. Random shuffle도 우연히 원래 순서를 반환할 수 있다.

Mutable list를 in-place로 shuffle하면 호출자가 가진 원본 ordering도 바뀐다. Immutable input을 유지해야 한다면 copy 후 shuffle하거나 새 sequence를 반환하는 helper를 만든다. Random behavior와 mutation behavior를 한 함수 이름에서 분명하게 드러낸다.

Dataset split에서 먼저 shuffle한 뒤 train/test를 나눌 때 동일 entity가 여러 row로 존재하면 information leakage가 생길 수 있다. 단순 row-level randomness보다 group-level sampling이 필요할 수 있다. Sampling unit 자체가 domain contract다.

---

## CHAPTER 07 · property-based test의 random generation은 실패를 축소하고 재현해야 한다

Property-based testing은 많은 입력을 생성해 invariant를 깨는 counterexample을 찾는다. 여기서 randomness는 목적이 아니라 input space 탐색 도구다. 좋은 framework는 실패한 generated case와 seed를 기록하고 더 작은 반례로 shrink해 사람이 이해할 수 있게 만든다.

Generator가 너무 제한된 범위만 만든다면 test가 많은 횟수로 실행돼도 중요한 경계를 방문하지 못한다. Empty collection, 매우 큰 값, Unicode, duplicate, invalid combination처럼 domain boundary를 generator 전략에 반영한다. Pure random distribution보다 boundary-biased generation이 결함 탐지에 더 강할 수 있다.

Test 실패를 “한 번 더 돌려서 통과하면 괜찮음”으로 처리하지 않는다. 동일 seed와 최소 counterexample로 재현하고 defect를 수정한 뒤 그 case가 회귀 test에 남도록 한다. Randomized verification도 deterministic debugging path를 가져야 한다.

---

## CHAPTER 08 · concurrency에서 random stream 공유는 schedule과 state를 얽히게 한다

여러 thread/task가 동일 PRNG instance를 공유하면 호출 interleaving에 따라 각 task가 받는 random value sequence가 달라질 수 있다. Generator 자체가 thread-safe하더라도 reproducibility가 깨질 수 있다. Simulation worker마다 독립 stream을 만들거나 deterministic하게 seed를 분할하는 전략이 필요하다.

Worker ID만 seed에 더하는 단순 방법은 parallelism level이 바뀌었을 때 결과가 달라질 수 있다. 실험 재현성이 매우 중요하다면 logical task identity에서 seed를 derive하거나 splittable stream을 지원하는 library를 사용한다. “8 worker로 돌렸을 때만 재현됨”은 운영상 불편한 contract다.

Secure randomness는 보통 OS-level API가 concurrency를 안전하게 처리하지만 performance와 blocking semantics는 platform에 따라 확인해야 한다. Application-level CSPRNG state를 직접 공유·fork하는 위험한 설계를 피한다.

---

## CHAPTER 09 · simulation 결과에는 seed뿐 아니라 model version과 환경을 함께 기록한다

Simulation이 random input을 사용하면 seed는 재현에 필수지만 충분하지 않을 수 있다. Model code, data snapshot, Python version, dependency, floating-point environment가 달라지면 같은 seed에서도 결과가 달라질 수 있다. 실험 artifact에 이런 provenance를 함께 남긴다.

Monte Carlo 결과는 단일 run의 숫자보다 confidence interval과 sample count, convergence를 함께 해석해야 한다. Seed를 여러 개 바꿔 결과 분산을 확인하면 특정 random stream에 우연히 의존하는 결론을 줄일 수 있다. 그러나 seed를 많이 돌렸다는 이유로 잘못된 model assumption이 해결되는 것은 아니다.

성능 benchmark에서도 random input generation 시간을 측정 구간에 포함했는지 구분한다. Algorithm cost를 비교하고 싶다면 동일한 input dataset을 재사용해 randomness가 measurement noise를 키우지 않게 할 수 있다.

---

## CHAPTER 10 · randomness contract는 목적·generator·state·reproduction 네 항목으로 고정한다

난수를 사용하는 지점마다 먼저 목적을 분류한다. Security secret인지, simulation인지, test data인지, UI sampling인지가 첫 번째다. 다음으로 그 목적에 맞는 generator를 선택하고 state ownership을 정하며, 실패나 결과를 다시 만들어야 한다면 seed와 environment를 기록한다.

API signature에도 random source를 dependency로 받을 수 있다. `choose_candidate(items, rng)`처럼 generator를 주입하면 test에서는 fixed/deterministic source를 사용하고 production에서는 적절한 source를 연결할 수 있다. Global random state에 숨은 의존성을 줄이는 방식이다.

Randomness 프로그래밍의 핵심은 **결과를 예측할 수 없게 만드는 것이 아니라 어떤 경우에는 예측 불가능성을, 어떤 경우에는 정확한 재현성을 의도적으로 선택하고 그 선택을 코드와 검증에 고정하는 것**이다.