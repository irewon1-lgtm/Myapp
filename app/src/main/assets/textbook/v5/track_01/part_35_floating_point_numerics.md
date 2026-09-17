# PART 35 · Floating-Point Numerics — representation, rounding, stability, reproducibility

실수 계산에서 가장 위험한 착각은 “수학식이 맞으면 프로그램 결과도 같은 값이 나온다”는 가정이다. finite bit pattern으로 값을 표현하는 순간 rounding, overflow, underflow, cancellation, operation ordering이 결과에 들어온다. 이 PART는 **표현 오차, 알고리즘 안정성, hardware/compiler 최적화, error budget**을 하나의 수치 계약으로 연결한다.

---

## CHAPTER 01 · floating-point representation은 실수 전체가 아니라 유한한 표본 집합이다

floating-point 값은 sign, exponent, significand를 조합해 넓은 magnitude 범위를 제한된 bit 수로 표현한다. 따라서 대부분의 실수는 정확히 저장되지 않고 가장 가까운 representable value로 반올림된다. 같은 decimal literal이라도 binary format에 정확한 점이 존재하지 않으면 처음 읽는 순간 이미 근사값이 된다.

정수처럼 bit 수가 늘면 단순히 범위만 커지는 것이 아니라 precision distribution도 exponent에 따라 달라진다. 큰 magnitude에서는 인접 representable value 간격이 넓어지고 작은 magnitude에서는 더 촘촘하다. absolute tolerance 하나를 모든 값에 적용하면 scale이 다른 데이터에서 잘못된 판정이 생긴다.

디버깅에서는 출력된 decimal 문자열보다 실제 bit pattern과 format을 확인한다. serialization·DB·GPU 경계를 지나며 precision이 바뀌는 경우 각 단계의 format을 기록해 첫 rounding 지점을 찾는다.

---

## CHAPTER 02 · normalized number는 leading significand 정보를 효율적으로 사용한다

normalized floating-point는 가능한 exponent 범위에서 significand의 유효 bit를 최대한 활용하도록 표현한다. binary format에서는 leading bit를 암묵적으로 다룰 수 있어 같은 storage로 더 많은 precision을 제공한다. 하지만 exponent가 극단으로 가면 normalized 영역을 벗어나 special handling이 필요하다.

machine number 사이 간격은 exponent가 커질수록 함께 커진다. 따라서 동일한 `+1`이 작은 값에는 변화를 만들지만 충분히 큰 값에는 rounding으로 사라질 수 있다. counter를 floating-point로 장기간 누적하거나 timestamp를 큰 absolute 값으로 저장하면 resolution 문제가 생길 수 있다.

테스트에서는 대표 값뿐 아니라 exponent boundary를 포함한다. numeric type을 선택할 때 maximum magnitude와 필요한 smallest delta를 동시에 적어야 한다.

---

## CHAPTER 03 · subnormal은 zero 근처에서 gradual underflow를 제공한다

normalized 최소값보다 작은 magnitude를 곧바로 zero로 만들면 zero 주변에 큰 gap이 생긴다. subnormal representation은 significand precision을 줄이는 대신 더 작은 값을 표현해 underflow를 점진적으로 만든다. 이는 작은 차이와 일부 numerical algorithm의 안정성에 영향을 준다.

일부 hardware나 fast mode는 성능을 위해 subnormal input/output을 zero처럼 처리할 수 있다. 같은 코드가 CPU와 GPU에서 tiny-value behavior가 달라질 수 있는 이유다. threshold 근처의 control logic이 subnormal 여부에 민감하면 portability 문제가 생긴다.

profile에서 subnormal 처리 비용을 의심할 때는 값 distribution을 먼저 확인한다. flush-to-zero 설정을 바꾸기 전 domain에서 tiny signal을 버려도 되는지 error budget으로 검증한다.

---

## CHAPTER 04 · infinity는 overflow와 극한 연산을 표현하지만 정상 숫자와 같은 의미는 아니다

floating-point infinity는 representable finite range를 넘어선 결과나 특정 divide operation에서 나타날 수 있다. arithmetic propagation 규칙이 있어 computation이 계속될 수 있지만 `+∞`가 business domain에서 허용 가능한 값이라는 뜻은 아니다. sensor, finance, probability처럼 finite value가 invariant인 영역에서는 즉시 boundary validation이 필요하다.

overflow가 infinity로 바뀌어 exception 없이 downstream으로 흐르면 훨씬 나중에 NaN이나 invalid decision으로 나타날 수 있다. exponential function, reciprocal, normalization scale이 큰 경로를 입력 범위와 함께 분석한다.

관측 metric에서 infinity를 일반 histogram bucket에 섞지 말고 별도 count로 추적한다. 첫 finite→infinite transition의 operation과 input magnitude를 보존하면 원인 추적이 쉬워진다.

---

## CHAPTER 05 · NaN은 invalid numerical state를 전파하지만 비교 규칙이 직관과 다르다

NaN은 정의되지 않은 연산이나 invalid input을 표현하는 special value다. 많은 arithmetic operation이 NaN을 전파해 오류가 조용히 downstream까지 이동할 수 있다. equality 비교와 ordering semantics도 일반 실수와 달라 `x == x` 같은 직관이 깨질 수 있다.

sorting, min/max, database key, cache equality에 NaN이 들어오면 library별 handling 차이가 시스템 behavior를 바꿀 수 있다. NaN payload나 signaling behavior에 의존하는 code는 portability 부담이 크다.

입력 경계에서 finite requirement를 명시하고 NaN 생성 수를 metric으로 둔다. crash 대신 NaN이 퍼지는 pipeline에서는 최초 생성 지점을 잡는 instrumentation이 필요하다.

---

## CHAPTER 06 · signed zero는 값 비교와 방향 정보가 분리될 수 있음을 보여준다

floating-point에는 +0과 -0이 존재할 수 있고 일반 equality에서는 같게 취급되더라도 reciprocal, complex function, branch-cut semantics 등에서 sign이 결과에 영향을 줄 수 있다. serialization이나 string formatting에서 둘을 합치면 일부 numerical information이 사라질 수 있다.

zero sign에 business 의미를 부여하는 것은 피하는 편이 좋지만 numerical library 내부에서는 directional limit을 보존하는 데 사용될 수 있다. canonicalization을 할 때 어떤 API가 sign of zero를 요구하는지 확인한다.

cross-platform test에서 bit-identical 결과를 요구한다면 signed zero 차이도 포함된다. tolerance 비교만으로는 bitwise reproducibility issue를 놓칠 수 있다.

---

## CHAPTER 07 · rounding mode는 representable grid에 결과를 매핑하는 규칙이다

정확한 실수 결과가 두 representable number 사이에 있을 때 어떤 값을 선택할지 rounding rule이 결정한다. nearest-even 같은 규칙은 누적 bias를 줄이는 데 유리하지만 directed rounding이 필요한 interval·financial calculation도 있다. operation마다 rounding이 개입하므로 긴 expression은 한 번만 반올림되는 것이 아니다.

compiler가 expression을 재배치하거나 FMA를 사용하면 rounding point 수가 바뀐다. 수학적으로 같은 식이 bitwise 다른 결과를 내는 이유다. global rounding mode에 의존하는 code는 library 호출과 thread context까지 고려해야 한다.

검증에서는 reference high-precision 계산과 target format 결과를 비교한다. rounding mode를 바꿔야 한다면 scope와 복원 규칙을 명시해 다른 계산에 누출되지 않게 한다.

---

## CHAPTER 08 · ULP는 해당 magnitude에서 인접 representable value의 간격을 측정한다

Units in the Last Place 관점은 floating-point error를 format의 실제 resolution과 연결한다. absolute error가 같아도 값의 magnitude에 따라 몇 ULP인지 달라진다. low-level math function이나 cross-platform regression에서는 ULP distance가 유용한 품질 지표가 될 수 있다.

그러나 ULP tolerance만으로 domain correctness를 설명할 수는 없다. zero 근처, subnormal, huge magnitude, discontinuity에서는 application이 허용하는 relative·absolute error와 별도로 판단해야 한다.

test helper는 NaN, infinity, signed zero를 명시적으로 처리한다. “몇 ULP 이내” 기준이 실제 business output 오차와 어떤 관계인지 문서화한다.

---

## CHAPTER 09 · machine epsilon은 모든 계산의 허용오차 상수가 아니다

machine epsilon은 1 근처에서 representable spacing과 관련된 format 속성이다. 이를 임의의 두 실수 비교에 그대로 사용하면 magnitude가 큰 값에서는 지나치게 엄격하고 zero 근처에서는 부적절할 수 있다. tolerance는 expected scale과 algorithm conditioning을 반영해야 한다.

`abs(a-b) < epsilon` 패턴을 복사하는 대신 absolute tolerance와 relative tolerance를 조합하는 방법이 일반적이다. 하지만 financial amount처럼 exact decimal semantics가 요구되면 floating tolerance 자체가 잘못된 모델일 수 있다.

API contract에 numeric tolerance와 unit을 명시한다. test마다 서로 다른 magic epsilon이 흩어지면 실제 error budget을 추적할 수 없게 된다.

---

## CHAPTER 10 · decimal fraction은 binary floating-point에 정확히 표현되지 않을 수 있다

0.1 같은 decimal fraction은 finite binary expansion이 아니므로 binary floating-point에 가장 가까운 근사값으로 저장된다. 출력 formatter가 다시 짧은 decimal을 보여 주기 때문에 사용자는 exact value라고 착각할 수 있다. 반복 합산이나 equality에서 차이가 드러나는 이유다.

화폐·세금처럼 decimal rounding rule이 규정된 domain에서 binary float를 사용하면 conversion point마다 정책을 정해야 한다. integer minor unit이나 decimal arithmetic이 더 적합할 수 있다.

입출력 pipeline의 decimal↔binary conversion 횟수를 줄인다. JSON·DB·UI가 서로 다른 rounding을 적용하면 같은 금액이 화면과 ledger에서 달라질 수 있다.

---

## CHAPTER 11 · decimal·fixed-point는 표현 정확성과 range·performance를 교환한다

decimal arithmetic이나 fixed-point는 특정 decimal fraction을 exact하게 표현할 수 있어 회계 규칙과 잘 맞는다. 대신 scale 관리, overflow, operation cost가 달라지고 division에서 다시 rounding policy가 필요하다. 타입만 바꾸면 모든 numerical issue가 사라지는 것은 아니다.

fixed-point에서는 unit과 scale이 type contract의 일부다. cents와 mills를 같은 integer로 혼용하면 type system이 막지 못할 수 있다. multiplication 후 scale normalization과 intermediate overflow도 확인해야 한다.

storage schema와 API에 scale을 명시하고 migration 시 변환 규칙을 테스트한다. 금융 계산은 final display rounding과 internal calculation rounding을 분리한다.

---

## CHAPTER 12 · 큰 수에 작은 수를 더하면 작은 변화가 표현 grid 아래로 사라질 수 있다

floating-point spacing은 magnitude에 따라 커지므로 큰 accumulator에 매우 작은 increment를 더할 때 결과가 같은 bit pattern으로 반올림될 수 있다. 많은 tiny update를 누적하는 simulation, statistic, timer에서 정보 손실이 점진적으로 쌓인다.

합산 순서를 바꾸거나 작은 값끼리 먼저 모으면 손실을 줄일 수 있다. 하지만 parallel reduction에서는 order가 scheduling과 partitioning에 따라 달라질 수 있어 deterministic result와 throughput 사이 trade-off가 생긴다.

실제 input magnitude distribution을 기록하고 naive sum과 high-precision reference의 drift를 비교한다. 평균 error뿐 아니라 worst-case sequence를 테스트한다.

---

## CHAPTER 13 · catastrophic cancellation은 비슷한 큰 값의 차에서 유효 digit를 잃는다

서로 가까운 근사값을 빼면 leading digit가 상쇄되고 이미 각 operand에 있던 rounding error가 결과에서 큰 상대 오차로 확대될 수 있다. quadratic formula, variance 계산, geometry distance 같은 곳에서 나타난다.

수학적으로 동치인 식을 algebraically 변형해 subtraction을 피하거나 더 안정적인 recurrence를 사용할 수 있다. 단순히 precision bit를 늘리는 방법은 비용을 높이면서 근본적인 conditioning 문제를 숨길 수 있다.

adversarial input을 만들어 cancellation 영역을 직접 테스트한다. normal input benchmark만으로 numerical stability를 판단하지 않는다.

---

## CHAPTER 14 · stable formula는 intermediate error가 output을 과도하게 증폭시키지 않게 설계한다

알고리즘의 numerical stability는 exact arithmetic에서 맞는지와 다른 질문이다. 같은 mathematical function을 계산해도 operation ordering과 intermediate magnitude에 따라 rounding error 증폭 정도가 달라진다. stable formulation은 가능한 한 well-scaled intermediate와 안전한 operation을 사용한다.

log-sum-exp, hypot 같은 재구성은 overflow·underflow를 피하는 대표적 사고 방식이다. library에 이미 stable primitive가 있다면 직접 naive 식을 구현하는 것보다 낫다.

reference oracle과 wide-range random input으로 relative error distribution을 측정한다. 성능 최적화가 stable transformation을 되돌리지 않는지 regression gate를 둔다.

---

## CHAPTER 15 · summation order는 floating-point reduction 결과의 일부다

실수 덧셈은 finite precision에서 일반적으로 associative하지 않으므로 `(a+b)+c`와 `a+(b+c)`가 다른 bit pattern을 낼 수 있다. parallel reduction tree, thread partition, compiler vectorization이 order를 바꾸면 run·platform 사이 결과 차이가 생긴다.

정확도 측면에서는 magnitude가 비슷한 값끼리 합치는 전략이 유리할 수 있고 deterministic tree는 reproducibility를 높인다. 하지만 strict ordering은 parallelism을 제한한다.

reduction algorithm과 partition count를 test artifact에 기록한다. bitwise reproducibility가 요구되는지 tolerance-based reproducibility면 충분한지 먼저 정한다.

---

## CHAPTER 16 · pairwise summation은 error growth를 줄이는 reduction tree를 만든다

값을 선형으로 하나씩 더하는 대신 비슷한 크기의 partial sum을 tree 구조로 합치면 rounding error가 누적되는 깊이를 줄일 수 있다. parallel reduction과도 잘 맞아 accuracy와 concurrency를 동시에 얻을 수 있는 경우가 많다.

partition이 불균형하거나 data magnitude가 극단적으로 치우치면 pairwise만으로 충분하지 않을 수 있다. tree shape가 worker count에 따라 바뀌면 bitwise result도 달라질 수 있다.

naive, pairwise, high-precision reference를 같은 dataset에서 비교한다. throughput과 maximum error를 함께 제시해 선택 근거를 남긴다.

---

## CHAPTER 17 · compensated summation은 잃어버린 low-order error를 별도 상태로 추적한다

Kahan 계열 방법은 각 addition에서 rounding으로 사라지는 작은 residual을 보정 변수에 누적해 naive summation보다 정확도를 높인다. 추가 arithmetic과 dependency가 생겨 vectorization·parallelization이 어려워질 수 있지만 정확도가 중요한 통계에서 가치가 있다.

보정 algorithm도 overflow나 pathological input 전체를 해결하지는 않는다. parallel version에서는 각 partition의 compensation과 final merge 전략을 별도 설계해야 한다.

정확도 개선을 decimal 출력 한두 자리로 판단하지 않고 reference와 ULP/relative error로 측정한다. 추가 CPU cost가 domain error budget에서 필요한지 확인한다.

---

## CHAPTER 18 · fused multiply-add는 두 operation을 한 rounding으로 계산한다

FMA는 `a*b+c`를 intermediate product를 별도로 rounding하지 않고 더 높은 내부 precision으로 계산한 뒤 한 번 rounding할 수 있다. 정확도와 throughput에 유리할 수 있지만 non-FMA target이나 contraction을 금지한 build와 bitwise 결과가 달라진다.

compiler가 자동 contraction을 허용하는지 optimization flag에 따라 달라질 수 있다. reference test가 특정 rounding sequence를 기대한다면 FMA 사용 여부가 contract다.

binary reproducibility를 요구할 때 generated instruction을 확인한다. numerical tolerance가 목적이라면 FMA가 오히려 더 정확한 결과를 만드는 점을 고려한다.

---

## CHAPTER 19 · extended precision은 intermediate value의 format을 source type보다 넓힐 수 있다

일부 execution environment는 register나 intermediate에서 선언된 storage type보다 넓은 precision을 사용해 계산하고 memory에 저장할 때 줄일 수 있다. spill 여부나 optimization에 따라 rounding point가 달라지면 같은 source가 build 설정에 따라 다른 마지막 bit를 낼 수 있다.

현대 platform에서도 vector unit, GPU, library path가 서로 다른 intermediate precision을 사용할 수 있다. exact bit result를 기대하면 operation format을 더 명시적으로 통제해야 한다.

cross-build test에서 register allocation 변화만으로 결과가 변하는지 확인한다. numerical correctness는 특정 compiler artifact에 우연히 의존하지 않아야 한다.

---

## CHAPTER 20 · fast-math는 algebraic freedom을 넓히는 대신 IEEE 보장을 일부 포기한다

compiler의 fast-math 계열 옵션은 NaN·infinity·signed zero·associativity 등에 대한 강한 가정을 허용해 reordering, vectorization, contraction을 늘릴 수 있다. 성능은 좋아질 수 있지만 exceptional value와 reproducibility behavior가 달라진다.

flag를 전체 프로젝트에 켜면 security·physics·finance처럼 strict semantics가 필요한 code에도 영향을 줄 수 있다. 함수별 범위나 kernel별 policy를 정하는 편이 안전하다.

fast-math on/off differential test를 유지하고 worst-case error와 performance를 함께 측정한다. 성능 regression을 막으려다 numerical contract를 암묵적으로 바꾸지 않는다.

---

## CHAPTER 21 · conditioning은 문제 자체가 input perturbation에 얼마나 민감한지 나타낸다

ill-conditioned problem은 input의 아주 작은 오차가 output에서 크게 증폭될 수 있다. 이런 경우 완벽히 stable한 algorithm을 사용해도 제한된 input precision 때문에 결과 불확실성이 크다. algorithm error와 problem conditioning을 구분해야 한다.

matrix solve, root finding, subtraction near singularity에서 condition number나 domain-specific sensitivity를 평가한다. precision만 높여도 input measurement error가 지배하면 의미가 없다.

error budget을 input uncertainty, algorithm rounding, approximation으로 분해한다. output tolerance가 왜 필요한지 수학적 sensitivity와 연결한다.

---

## CHAPTER 22 · stability는 algorithm이 exact problem의 작은 perturbation처럼 행동하는지를 본다

backward stable algorithm은 계산된 결과가 약간 perturb된 input에 대한 exact solution으로 해석될 수 있는 성질을 목표로 한다. 이는 단순 “오차가 작다”보다 더 구조적인 보장이다. conditioning과 결합해야 final forward error를 이해할 수 있다.

naive elimination, recurrence, polynomial evaluation은 식 선택에 따라 stability가 크게 달라진다. optimization이 operation order를 바꾸면 proof 전제가 달라질 수도 있다.

library 선택에서 benchmark 속도뿐 아니라 numerical method와 error guarantee를 확인한다. domain test는 ill-conditioned case를 따로 포함한다.

---

## CHAPTER 23 · pivoting은 elimination 과정의 error amplification을 줄이는 전략이다

linear system elimination에서 작은 pivot으로 나누면 rounding error가 크게 증폭될 수 있다. partial pivoting은 더 적절한 pivot을 선택해 안정성을 개선하는 대표 방법이다. pivot selection에는 비교와 row swap 비용이 있지만 correctness 비용과 비교해야 한다.

GPU에서 pivoting은 parallel data movement를 만들 수 있어 performance 최적화가 복잡하다. pivoting을 제거한 fast kernel이 특정 matrix에서만 실패할 수 있다.

random matrix뿐 아니라 near-singular·scaled matrix를 test한다. residual과 solution error를 분리해 측정한다.

---

## CHAPTER 24 · mixed precision은 계산 단계마다 필요한 정확도를 다르게 배분한다

일부 workload는 bulk multiply를 FP16/BF16으로 수행하고 accumulation이나 refinement를 FP32/FP64로 유지해 throughput과 accuracy를 절충할 수 있다. 성공 조건은 low-precision error가 refinement와 conditioning 범위 안에 있어야 한다는 것이다.

무작정 storage type을 줄이면 overflow·underflow와 quantization noise가 증가한다. loss scaling이나 iterative refinement 같은 보조 기법도 추가 state와 failure mode를 만든다.

각 단계의 format과 accumulator precision을 traceable config로 보존한다. model/library upgrade 뒤 accuracy drift가 format 변경 때문인지 구분 가능해야 한다.

---

## CHAPTER 25 · BF16과 FP16은 같은 16-bit라도 range와 precision trade-off가 다르다

FP16은 significand precision과 exponent 범위의 한 조합을 제공하고 BF16은 exponent range를 더 넓게 유지하는 대신 fraction precision을 줄이는 방향의 설계를 사용한다. 따라서 동일 model이라도 overflow 위험과 quantization error 특성이 다르다.

입력 activation과 gradient distribution에 따라 적합한 format이 달라진다. 단순 memory 절감률만 보고 선택하면 training instability나 inference drift가 생길 수 있다.

histogram으로 magnitude distribution을 관찰하고 saturation·underflow count를 측정한다. hardware가 어떤 accumulation format을 사용하는지도 확인한다.

---

## CHAPTER 26 · quantization은 continuous value를 제한된 discrete code로 매핑한다

정수 quantization은 scale과 zero-point 같은 parameter를 이용해 실수 범위를 제한된 code로 표현한다. range가 너무 넓으면 resolution이 나빠지고 outlier 때문에 대부분 값의 precision이 손해를 볼 수 있다. per-channel·per-tensor 같은 granularity 선택도 accuracy와 metadata cost를 바꾼다.

calibration dataset이 production distribution을 대표하지 않으면 deployment 뒤 saturation이 늘 수 있다. quantized operator coverage가 부족해 frequent dequantize/requantize가 생기면 성능 이점이 사라진다.

layer별 error와 end-to-end metric을 함께 본다. quantization 변경을 단순 file-size optimization으로 취급하지 않는다.

---

## CHAPTER 27 · cross-platform reproducibility는 ISA, library, compiler가 모두 관여한다

동일 source와 input이라도 FMA, vector width, math-library approximation, reduction order가 달라지면 마지막 bit가 달라질 수 있다. strict IEEE operation만으로 모든 transcendental function까지 bitwise 동일해지는 것은 아니다.

규제·scientific workflow처럼 exact reproducibility가 필요하면 approved library/version과 deterministic algorithm을 고정해야 한다. 일반 ML·graphics에서는 tolerance 기반 결과가 더 현실적일 수 있다.

reproducibility level을 bitwise, ULP, relative, domain metric 중 무엇으로 볼지 명시한다. CI device matrix에서 결과 drift를 자동 추적한다.

---

## CHAPTER 28 · numerical test는 nominal case보다 boundary와 adversarial scale이 중요하다

평균적인 입력만 사용하면 overflow, cancellation, subnormal, NaN propagation 같은 문제가 드러나지 않는다. test generator는 exponent range, near-zero, near-equal subtraction, huge/small mix를 의도적으로 만든다.

oracle 자체가 같은 precision을 사용하면 동일한 bug를 공유할 수 있다. higher precision library, algebraic invariant, independently derived implementation을 사용해 교차검증한다.

실패 input은 최소화해 regression corpus에 보존한다. random seed와 platform 정보를 같이 기록해 numerical failure를 재현 가능하게 만든다.

---

## CHAPTER 29 · error budget은 numerical drift를 system requirement와 연결한다

모든 last-bit 차이를 bug로 취급하면 최적화가 불가능하고, 반대로 “오차가 조금 있다”는 표현만으로는 correctness를 보장할 수 없다. 각 pipeline stage가 허용받는 absolute·relative·ULP·domain error를 배분해야 한다.

여러 stage의 error가 독립인지 같은 방향으로 누적되는지에 따라 합산 방식이 달라진다. upstream calibration error가 downstream threshold decision을 뒤집는지까지 본다.

metric dashboard에 accuracy distribution과 threshold violation count를 둔다. performance change는 error budget 안에 있다는 증거와 함께 승인한다.

---

## CHAPTER 30 · numerical contract는 format, operation freedom, tolerance를 함께 정의한다

수치 API는 단순히 `float`를 반환한다고 충분하지 않다. input range, finite-value requirement, internal precision, fast-math 허용 여부, deterministic reduction, output tolerance를 명시해야 한다. 이 계약이 있어야 CPU/GPU·compiler·library 변경을 안전하게 평가할 수 있다.

성능 optimization은 rounding point와 operation order를 바꿀 수 있으므로 numerical regression을 functional test와 별도 gate로 둔다. exact decimal이 필요한 domain은 애초에 binary floating-point를 선택하지 않는 것도 설계다.

최종 목표는 모든 계산을 가장 높은 precision으로 하는 것이 아니라 **domain decision을 바꾸지 않는 범위의 오차를 증명하면서 필요한 throughput과 portability를 확보하는 것**이다.
