# PART 27 · Randomness와 reproducibility — 난수를 데이터·보안·테스트 목적에 맞게 분리하기

난수는 하나의 기능처럼 보이지만 simulation, sampling, 게임 규칙, test data, password token은 서로 다른 요구를 가진다. 어떤 작업은 같은 seed에서 같은 sequence가 재현되어야 하고, 어떤 작업은 공격자가 다음 값을 예측할 수 없어야 한다. 난수 API를 “랜덤한 숫자를 주는 함수”로만 보면 통계적 편향과 보안 결함을 동시에 만들 수 있다. 핵심은 **난수의 목적, 상태, entropy source, 재현성 계약을 먼저 구분하는 것**이다.

---

## CHAPTER 01 · pseudo-random generator는 내부 상태에서 결정적으로 다음 값을 만든다

일반적인 PRNG는 이전 내부 상태를 바탕으로 다음 상태와 출력을 계산한다. 겉으로 불규칙해 보여도 동일한 algorithm과 동일한 초기 state를 사용하면 같은 sequence가 다시 나온다. 이 결정성은 simulation과 test에서는 장점이다. 실패한 실험의 seed를 기록하면 같은 입력 순서를 재현할 수 있기 때문이다.

반대로 보안 token처럼 예측 불가능성이 필요한 곳에서는 일반 PRNG의 결정성이 약점이 될 수 있다. 공격자가 state 일부나 seed를 추론하면 다음 값을 예측할 가능성이 생긴다. “분포가 랜덤해 보인다”와 “공격자가 예측하기 어렵다”는 별개의 성질이다.

Generator를 global singleton처럼 공유하면 서로 다른 module의 호출 순서가 sequence에 영향을 준다. Test A가 난수를 몇 번 더 사용하면 Test B의 결과가 달라지는 식의 coupling이 생길 수 있다. 재현성이 중요하면 generator instance를 명시적으로 생성해 소유권을 좁히고 seed를 기록한다.

PRNG의 period와 statistical quality는 algorithm마다 다르다. 일반 application은 검증된 library implementation을 사용하고 자체 난수 algorithm을 만들지 않는다.

---

## CHAPTER 02 · seed는 “랜덤성의 양”이 아니라 재현 가능한 초기 상태를 정하는 입력이다

Seed를 현재 시각으로 주면 실행마다 다른 sequence가 나올 수 있지만 그것이 cryptographic security를 보장하지 않는다. 현재 시각은 가능한 후보 범위가 좁고 공격자가 대략적인 실행 시점을 알 수 있기 때문이다. Seed의 역할은 PRNG state를 초기화하는 것이고, security strength는 entropy source와 generator 설계에 달려 있다.

Simulation에서는 seed를 experiment metadata에 남긴다. Algorithm version, input data version과 함께 seed가 있어야 결과를 다시 만들 수 있다. 여러 worker가 parallel simulation을 수행한다면 모두 같은 seed를 사용해 동일 sequence를 중복 소비하지 않도록 independent stream 전략을 정한다.

Test에서 seed를 항상 고정하면 deterministic하지만 값 공간의 다양성이 줄 수 있다. Property-based test처럼 여러 seed를 사용하되 실패 시 seed와 최소 counterexample을 기록하면 다양성과 재현성을 함께 얻을 수 있다.

Seed를 API의 숨은 global setting으로 두기보다 함수나 generator construction boundary에서 명시하면 dependency가 보인다. 같은 함수가 어떤 환경에서는 deterministic하고 어떤 환경에서는 아닐지 모르는 상태를 줄인다.

---

## CHAPTER 03 · cryptographic randomness는 운영체제의 안전한 entropy source를 사용한다

Password reset token, session secret, API key, nonce처럼 공격자가 예측하면 보안이 깨지는 값은 cryptographically secure random source가 필요하다. Python에서는 이런 목적을 위해 일반 `random` 계열과 별도로 보안용 API가 제공된다. 운영체제가 제공하는 entropy source와 검증된 library를 사용한다.

Token 길이는 문자 수가 아니라 실제 entropy bit 수로 평가한다. 6자리 숫자는 가능한 값이 100만 개뿐이므로 online rate limit이 없다면 brute force에 약할 수 있다. URL-safe token을 생성할 때도 encoding 후 문자 길이와 원래 random byte 수를 구분한다.

Secret을 생성한 뒤 log에 출력하거나 exception context에 넣으면 좋은 randomness가 무의미해진다. 생성, 전달, 저장, 비교, 폐기 전 과정이 보안 contract다. Token은 가능한 한 최소 lifetime과 최소 exposure를 가진다.

보안 난수를 test에서 재현하려고 production generator를 deterministic seed로 바꾸는 것은 위험하다. Test double을 dependency로 주입하고 production 구성은 secure source를 유지한다.

---

## CHAPTER 04 · uniform sampling은 index 선택과 범위 경계를 정확히 처리해야 한다

N개 항목에서 하나를 균등하게 뽑는다면 각 항목의 확률이 정확히 `1/N`이 되도록 해야 한다. Random integer API의 upper bound가 inclusive인지 exclusive인지 혼동하면 마지막 항목이 제외되거나 범위 밖 index가 생긴다. Python API마다 range semantics를 확인한다.

큰 random integer를 특정 범위로 만들 때 단순 `% N`을 사용하면 source 범위가 N으로 정확히 나누어떨어지지 않을 경우 modulo bias가 생길 수 있다. 일반 application은 library의 range sampling 함수를 사용해 이런 세부 문제를 맡긴다.

Weighted sampling에서는 weight가 확률인지 상대 가중치인지 구분하고 음수·NaN·모두 0인 경우를 validation한다. Float weight 누적에서 precision 문제가 생길 수 있으므로 library contract를 확인한다.

Sampling without replacement는 한 항목을 두 번 뽑지 않는 계약이고 with replacement는 독립 draw를 반복한다. 이름이 비슷하지만 통계적 의미가 다르므로 분석과 A/B assignment에서 명시한다.

---

## CHAPTER 05 · shuffle은 permutation을 만들며 원본 mutation 여부가 API 계약이다

목록을 섞는 operation은 모든 permutation이 동일 확률이어야 하는 요구를 가질 수 있다. 잘못된 swap algorithm은 특정 순열을 더 자주 만들 수 있다. 검증된 Fisher–Yates 계열 implementation을 제공하는 표준 library를 사용하고 직접 “적당히 swap”하지 않는다.

In-place shuffle은 원본 list를 변경한다. 같은 list를 다른 코드가 공유한다면 그 순서도 바뀐다. 재현 가능한 데이터 pipeline에서 원본 order를 보존해야 한다면 copy를 만든 뒤 shuffle하거나 shuffled view를 반환하는 함수를 사용한다.

Seed를 고정해 shuffle 결과를 test할 때는 특정 Python version의 exact permutation을 public contract로 의존해야 하는지 신중히 본다. Algorithm 구현이 version에 따라 바뀔 가능성이 있다면 “같은 seed → 특정 순서”가 장기 호환 요구인지 문서를 확인한다.

데이터 split에서 먼저 shuffle한 뒤 train/test를 나누는 경우 동일 entity가 여러 row로 존재하면 정보 누수가 생길 수 있다. Randomness는 domain grouping rule을 대신하지 않는다.

---

## CHAPTER 06 · random sample은 population 정의가 틀리면 통계적으로 정확해도 잘못된 결론을 만든다

100개를 균등 추출하는 code가 완벽해도 모집단이 이미 편향돼 있다면 sample도 원하는 현실을 대표하지 않는다. 로그에 기록된 사용자만 sampling하면서 전체 사용자 behavior를 추론하거나, 생존한 데이터만 대상으로 실패율을 계산하면 selection bias가 생길 수 있다.

Sampling unit도 정의한다. User 단위로 뽑을지 event 단위로 뽑을지에 따라 활동량이 많은 user가 과대표집될 수 있다. Experiment assignment는 보통 같은 user가 반복 방문해도 동일 group에 남도록 stable identifier 기반 hashing을 사용할 수 있다.

Random split 전에 time leakage를 고려해야 하는 데이터도 있다. 미래 record가 training에 들어가 과거를 예측하는 test에 영향을 주면 실제 배포 성능을 과대평가한다. 이 경우 chronological split이 필요하다.

난수 함수는 selection mechanism만 제공한다. 어떤 population과 unit, time window를 대상으로 할지는 분석 계약의 문제다.

---

## CHAPTER 07 · simulation은 generator state와 model assumption을 모두 기록해야 재현된다

Monte Carlo simulation은 random input을 반복 생성해 결과 분포를 추정한다. Seed만 기록해도 입력 sequence는 재현할 수 있지만 model parameter, algorithm version, stopping condition이 다르면 결과는 달라진다. Experiment manifest에 모든 의미 있는 입력을 기록한다.

표본 수가 늘수록 추정 오차가 일반적으로 줄어들 수 있지만 계산 비용이 증가한다. 단순히 “백만 번 돌리면 정확하다”가 아니라 confidence interval이나 variance를 보고 충분한 sample size를 결정한다.

Parallel simulation에서 worker마다 독립 random stream이 필요한 경우 seed를 단순 `base_seed + worker_id`로 만드는 것이 generator 특성상 충분한지 확인한다. Library가 제공하는 stream splitting이나 seed sequence mechanism을 사용하는 편이 안전하다.

결과를 평균 하나로만 저장하지 않고 분포, quantile, 실패 빈도를 남기면 rare event와 tail risk를 볼 수 있다. Randomness는 결과를 하나의 정답이 아니라 분포로 바꾸는 계산 모델이다.

---

## CHAPTER 08 · deterministic test와 randomized test는 서로 다른 실패를 찾는다

Unit test는 보통 동일 입력에서 동일 결과가 나와야 빠르게 회귀를 판단할 수 있다. Random input을 매번 바꾸면서 실패 seed를 기록하지 않으면 CI에서만 가끔 깨지는 flaky test가 된다. 기본 suite는 deterministic하게 유지하고 randomized 탐색은 재현 정보를 반드시 남긴다.

Fuzzing과 property-based testing은 예측하지 못한 입력을 찾는 데 강하다. 하지만 발견한 실패를 작은 deterministic regression test로 고정해야 같은 bug가 다시 들어오는 것을 확실히 막을 수 있다.

Randomized concurrency stress test도 timing에 따라 rare race를 찾을 수 있지만 “1000번 통과했으니 race가 없다”는 증명은 아니다. Synchronization invariant와 deterministic interleaving test를 함께 사용한다.

Test generator 자체가 invalid input만 만들거나 특정 branch를 거의 방문하지 않는지 distribution을 확인한다. Random test도 input model의 품질에 좌우된다.

---

## CHAPTER 09 · stable hashing은 random assignment와 process-randomized hash를 구분해야 한다

사용자를 A/B group에 안정적으로 배정하기 위해 `hash(user_id) % 2`를 쓰고 싶을 수 있다. 하지만 language runtime의 일반 hash는 process마다 달라지거나 version별 안정성을 보장하지 않을 수 있다. Persistent assignment에는 명시적으로 stable hash algorithm과 salt/version을 선택한다.

Hash 결과를 random-like bucket으로 사용할 때 input distribution과 hash quality를 확인한다. Sequential ID라도 좋은 non-cryptographic hash는 bucket을 고르게 만들 수 있지만 공격자가 bucket을 조작할 수 있는 보안 경계에서는 keyed cryptographic hash가 필요할 수 있다.

Experiment version을 salt에 포함하면 새 실험에서 과거 assignment와 독립적인 group을 만들 수 있다. 반대로 동일 cohort를 유지해야 하면 version 변경이 assignment를 바꾸지 않도록 한다.

Stable assignment는 truly random draw가 아니라 deterministic pseudo-random mapping이다. 같은 input은 언제나 같은 output을 얻는다는 성질이 핵심이다.

---

## CHAPTER 10 · 난수 dependency를 명시하면 보안과 재현성을 동시에 통제할 수 있다

Domain 함수 안에서 global random API를 직접 호출하면 test가 어렵고 caller가 randomness source를 선택할 수 없다. 필요한 경우 `rng`나 `token_source`를 dependency로 전달해 simulation에서는 seeded generator, production secret에서는 secure generator를 구성한다.

모든 함수에 generator를 parameter로 넣을 필요는 없다. Randomness가 실제 behavior의 일부인 service 경계에서 소유하고 하위 pure function에는 이미 선택된 값을 넘길 수 있다. Dependency를 필요한 범위만큼만 노출한다.

Logging에는 secret token 값을 남기지 않지만 simulation seed는 재현을 위해 남길 수 있다. 같은 “random input”이라도 관측 정책이 다르다. Security-sensitive source와 reproducible source를 타입이나 interface 이름으로 구분하면 실수로 교체하는 위험을 줄인다.

난수 프로그래밍의 핵심은 **랜덤해 보이는 출력을 만드는 것이 아니라 예측 불가능성이 필요한 곳과 재현 가능성이 필요한 곳을 분리하고, sampling 의미와 state ownership을 계약으로 만드는 것**이다.