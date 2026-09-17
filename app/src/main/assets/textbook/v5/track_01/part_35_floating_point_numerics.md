# PART 35 · Floating-Point and Numerical Correctness — rounding, stability, reproducibility

부동소수점은 `소수점 오차가 있다`는 경고문으로 끝나는 주제가 아니다. 표현 가능한 값의 간격이 magnitude에 따라 달라지고, 각 연산은 rounding rule 아래 exact result를 representable value로 투영한다. Algorithm이 같은 수학식을 계산해도 evaluation order·precision·FMA·parallel reduction에 따라 결과가 달라질 수 있다. Numerical engineering은 **표현 오차, conditioning, algorithmic stability, reproducibility requirement를 분리해 관리하는 일**이다.

---

## CHAPTER 01 · Floating-point value는 sign·significand·exponent의 유한 조합이다

Binary floating-point는 값의 scale을 exponent로 이동시키고 limited significand bits로 precision을 유지한다. Fixed integer와 달리 representable spacing이 전체 number line에서 일정하지 않다. Magnitude가 커질수록 adjacent representable value 사이 간격도 커진다.

`double은 소수점 15자리` 같은 구호보다 특정 magnitude에서 ULP 간격이 얼마인지 보는 편이 정확하다.

---

## CHAPTER 02 · Normalized representation은 leading significand pattern을 이용해 precision을 효율적으로 쓴다

Normal number는 significand를 정규화해 leading bit를 implicit하게 표현할 수 있다. Exponent field는 bias를 사용해 signed scale을 encoding한다. Bit pattern 해석은 integer와 완전히 다른 계약이다.

Binary dump를 볼 때 raw hex를 decimal integer로 읽지 않고 sign/exponent/fraction field를 분리한다.

---

## CHAPTER 03 · Subnormal은 zero 근처의 abrupt underflow를 완화한다

Normal exponent 최솟값보다 작은 magnitude를 subnormal format으로 표현하면 precision을 점진적으로 줄이면서 zero에 접근할 수 있다. 이를 gradual underflow라 한다. Subnormal 지원은 numerical behavior를 부드럽게 하지만 일부 hardware에서는 performance cost나 flush-to-zero mode가 존재할 수 있다.

Realtime/GPU code에서 FTZ/DAZ 설정을 바꾸면 작은 값의 algorithm semantics도 달라진다.

---

## CHAPTER 04 · Infinity는 overflow와 division semantics를 표현하는 값이다

IEEE-style arithmetic은 ±infinity를 사용해 finite range를 넘은 result나 division-by-zero 일부를 represent할 수 있다. Infinity와 finite value 연산은 정의된 propagation rule을 따른다.

Overflow를 exception/crash로 기대하는 code는 actual language/runtime behavior를 확인해야 한다. Infinity가 database/JSON/API boundary로 나갈 수 있는지도 별도 문제다.

---

## CHAPTER 05 · NaN은 하나의 숫자가 아니라 invalid/undefined result category다

0/0, invalid sqrt 같은 operation은 NaN을 만들 수 있고 대부분의 arithmetic에서 NaN이 전파된다. NaN comparison은 ordinary total ordering과 다르며 `x == x`가 false일 수 있다.

Sort/key/cache logic은 NaN policy를 명시해야 한다. Payload/signaling distinction을 지원하는 environment에서는 diagnostic metadata가 있을 수도 있지만 portable business logic에 의존하지 않는다.

---

## CHAPTER 06 · Signed zero는 비교는 같아도 일부 operation에서 의미가 다르다

+0와 -0는 equality comparison에서 같게 취급될 수 있지만 reciprocal, copysign, branch cut이 있는 complex function에서 차이가 드러날 수 있다. Underflow 방향 정보를 보존하는 데도 사용된다.

Serialization/canonicalization이 zero sign을 제거할 때 numerical semantics가 필요한지 검토한다.

---

## CHAPTER 07 · Rounding은 exact real result를 representable set으로 mapping한다

연산 hardware는 infinite-precision real result를 그대로 저장할 수 없으므로 rounding mode에 따라 nearby representable value를 선택한다. Round-to-nearest ties-to-even은 bias를 줄이는 기본 mode로 널리 사용된다. Directed rounding은 interval arithmetic과 bounds computation에 유용할 수 있다.

Rounding mode가 process/thread state로 변경 가능한 environment에서는 library call이 implicit global state에 의존할 수 있다.

---

## CHAPTER 08 · ULP는 local spacing을 표현한다

Unit in the Last Place는 특정 floating-point value 근처 representable spacing과 연결된다. Absolute error가 같아도 magnitude에 따라 ULP error는 다르다. Numerical library accuracy를 `소수점 몇 자리 맞음`보다 ulp bound로 표현하는 이유다.

Tolerance를 global constant epsilon으로 두기 전에 scale/units를 고려한다.

---

## CHAPTER 09 · Machine epsilon은 모든 비교에 사용할 만능 tolerance가 아니다

Machine epsilon은 1 근처 representable spacing과 관련된 상수다. 데이터가 1e12 규모이거나 1e-12 규모일 때 `abs(a-b) < epsilon`은 의미가 달라진다. Relative tolerance와 absolute floor를 domain scale에 맞춰 조합해야 한다.

Physical measurement tolerance는 floating-point representation tolerance보다 훨씬 클 수 있다. Domain uncertainty와 numerical error를 분리한다.

---

## CHAPTER 10 · Decimal fraction이 binary에서 무한 반복될 수 있다

10진 0.1처럼 denominator에 factor 5가 포함된 rational은 base-2 finite fraction으로 정확히 표현되지 않는다. 저장된 value는 가장 가까운 binary approximation이다. Python 공식 문서가 `0.1` representation error를 설명하는 핵심도 이것이다. citeturn267086search3turn267086search4

이 현상을 `컴퓨터가 계산을 틀림`이 아니라 radix representation limit으로 이해한다.

---

## CHAPTER 11 · Decimal floating-point/fixed-point는 다른 representation contract를 선택한다

금융처럼 decimal round rule과 cent 단위 exactness가 중요하면 binary float 대신 scaled integer 또는 decimal arithmetic을 사용할 수 있다. 그러나 decimal도 finite precision과 rounding context를 가진다.

Representation 선택은 `정확/부정확` 이분법이 아니라 어떤 값 집합과 operation rule을 exact하게 보존해야 하는가의 문제다.

---

## CHAPTER 12 · Addition은 magnitude 차이가 크면 작은 operand 정보를 잃을 수 있다

큰 value와 매우 작은 value를 더할 때 exponent alignment 과정에서 작은 significand bit가 rounding으로 사라질 수 있다. 반복적으로 tiny increment를 large accumulator에 더하면 increment가 반영되지 않는 구간이 생길 수 있다.

Accumulation order와 accumulator precision이 long-running counter/integration result에 중요하다.

---

## CHAPTER 13 · Catastrophic cancellation은 비슷한 수의 subtraction에서 relative error를 증폭한다

두 큰 approximate value가 거의 같을 때 subtraction 결과는 작은 차이인데 input의 low-order uncertainty가 결과 대부분을 차지할 수 있다. 수학적으로 동치인 formula를 cancellation이 적은 형태로 재작성하면 정확도가 개선된다.

Numerical instability는 hardware precision 부족과 algorithm formulation을 구분해 조사한다.

---

## CHAPTER 14 · Quadratic formula도 algebraically equivalent transformation이 stability를 바꾼다

`-b ± sqrt(b²-4ac)`에서 b와 sqrt term이 비슷하면 한 root 계산에서 cancellation이 심해질 수 있다. Alternative formulation으로 한 root를 안정적으로 계산한 뒤 product relation을 이용해 다른 root를 구할 수 있다.

Symbolic equivalence가 floating-point execution equivalence를 보장하지 않는다.

---

## CHAPTER 15 · Summation order가 result를 바꾸므로 naive reduction은 nondeterministic할 수 있다

Floating-point addition은 exact real arithmetic처럼 associative하지 않다. `(a+b)+c`와 `a+(b+c)`가 다른 rounded result를 낼 수 있다. Parallel reduction은 scheduler/tree shape에 따라 addition order가 달라진다.

Distributed/GPU aggregation에서 bitwise reproducibility가 요구되면 fixed reduction tree나 reproducible summation algorithm을 사용한다.

---

## CHAPTER 16 · Pairwise summation은 error growth를 줄일 수 있다

Sequential left-to-right sum은 large list에서 rounding error가 accumulation될 수 있다. Balanced pairwise tree는 similar scale partial sum을 결합해 error bound를 개선할 수 있고 parallelization에도 적합하다.

Algorithm complexity가 모두 O(n)이어도 numerical error behavior는 다르다.

---

## CHAPTER 17 · Kahan compensation은 잃어버린 low-order contribution을 별도 state로 추적한다

Compensated summation은 이전 rounding에서 사라진 small error estimate를 다음 addition에 보정해 naive accumulation보다 정확도를 높일 수 있다. Extra operations/state가 필요하고 compiler fast-math transformation이 compensation identity를 깨뜨릴 수 있다.

Accuracy target과 throughput을 benchmark해 선택한다.

---

## CHAPTER 18 · Fused Multiply-Add는 한 번의 rounding으로 a*b+c를 계산한다

FMA는 multiplication과 addition 사이 intermediate rounding을 제거해 accuracy와 performance를 개선할 수 있다. 그러나 compiler가 separate mul/add를 FMA로 contract하면 이전 binary result와 달라질 수 있다.

Strict reproducibility가 필요한 code는 FMA contraction policy와 target architecture를 고정해야 한다. IEEE 754는 fused operation 의미를 명시한다. citeturn267086search2

---

## CHAPTER 19 · Extended precision과 register allocation이 과거 platform 차이를 만들 수 있다

일부 architecture/runtime는 intermediate expression을 nominal type보다 높은 precision register에 유지했다가 memory store 시 rounding할 수 있다. Optimization과 spill 여부에 따라 result가 달라지는 문제가 생길 수 있다.

Modern compiler/ABI behavior를 확인하고 `같은 source면 bit-identical`이라고 가정하지 않는다.

---

## CHAPTER 20 · Fast-math는 algebraic transformation 허용 범위를 넓힌다

Compiler fast-math option은 NaN/Inf/sign-zero/associativity에 대한 strict assumption을 완화해 vectorization/reassociation을 허용할 수 있다. 성능은 좋아지지만 source-level IEEE semantics와 bitwise result가 달라질 수 있다.

Library/kernel마다 허용 error model을 문서화하고 global fast-math를 무작정 켜지 않는다.

---

## CHAPTER 21 · Conditioning은 problem 자체가 input error를 얼마나 증폭하는지 나타낸다

Well-conditioned problem은 작은 input perturbation이 작은 output change를 만들고 ill-conditioned problem은 작은 error가 큰 output change를 만들 수 있다. Algorithm이 완벽해도 problem conditioning 때문에 높은 precision이 필요할 수 있다.

Numerical bug 분석은 representation error와 condition number를 분리한다.

---

## CHAPTER 22 · Stability는 algorithm이 이상적 문제의 근처 solution을 계산하는지 묻는다

Backward-stable algorithm은 계산된 output이 slightly perturbed input의 exact solution으로 해석될 수 있는 특성을 목표로 한다. Condition이 좋으면 backward stability가 forward accuracy로 이어질 수 있다.

`double 사용`이 algorithm stability를 보장하지 않는다. Formula와 pivoting/scaling strategy가 중요하다.

---

## CHAPTER 23 · Matrix computation은 scaling과 pivoting이 핵심이다

Gaussian elimination에서 pivot가 매우 작으면 division이 error를 증폭할 수 있어 partial pivoting을 사용한다. Feature scale이 크게 다르면 condition이 나빠질 수 있다. Linear algebra library는 단순 nested loop 이상의 numerical policy를 포함한다.

ML preprocessing의 normalization도 optimization뿐 아니라 conditioning 관점에서 볼 수 있다.

---

## CHAPTER 24 · Mixed precision은 낮은 precision throughput과 높은 precision accumulation을 결합한다

GPU/accelerator는 FP16/BF16/TF32류 낮은 precision에서 높은 throughput을 제공할 수 있다. Dot product의 multiply는 low precision으로 수행하고 accumulation을 FP32로 유지하면 speed와 accuracy를 절충할 수 있다.

Loss scaling, master weights, iterative refinement처럼 error를 제어하는 algorithm이 필요하다. Type만 바꿔 성능을 얻는 문제가 아니다.

---

## CHAPTER 25 · BF16과 FP16은 exponent/significand trade-off가 다르다

두 format은 모두 16-bit 계열이지만 exponent 범위와 significand precision 배분이 달라 overflow/underflow와 relative precision 특성이 다르다. Training/inference workload는 dynamic range와 accuracy requirement에 따라 format을 선택한다.

`16-bit니까 정확도 절반` 같은 직관은 틀리다.

---

## CHAPTER 26 · Quantization은 floating-point precision 문제와 다른 approximation layer다

Integer/low-bit quantization은 scale/zero-point 등으로 real range를 discrete integer에 mapping한다. Calibration range가 잘못되면 clipping과 quantization noise가 증가한다. Floating-point rounding error와 quantization model error를 분리한다.

Model quality metric과 kernel throughput을 함께 검증한다.

---

## CHAPTER 27 · Cross-platform reproducibility는 compiler·ISA·library·thread schedule을 포함한다

동일 source와 input이어도 FMA 사용, vector width, libm implementation, fast-math, parallel reduction order가 다르면 result bit pattern이 달라질 수 있다. 테스트가 exact float equality를 요구하면 platform matrix에서 불필요하게 깨질 수 있다.

Requirement가 bitwise reproducibility인지 domain tolerance인지 먼저 정한다.

---

## CHAPTER 28 · Floating-point test는 exact expected value보다 property와 error bound를 사용할 수 있다

Known exact case, monotonicity, symmetry, relative/absolute error bound, ulp distance를 조합해 numerical function을 검증할 수 있다. NaN/Inf/subnormal/overflow boundary도 별도 test set으로 둔다.

Random property test는 ordinary range만 생성하지 말고 exponent extreme과 adjacent representable value를 포함한다.

---

## CHAPTER 29 · Numerical incident는 error budget을 단계별로 추적한다

Input measurement error, representation rounding, algorithm truncation/iteration error, parallel reduction nondeterminism을 분리해 total error에 기여하는 항목을 추적한다. 마지막 output 차이만 보고 `float precision`으로 뭉개지 않는다.

Reference high-precision implementation과 differential test를 사용하면 어느 단계에서 error가 커지는지 찾을 수 있다.

---

## CHAPTER 30 · Numerical 설계의 최종 계약은 range·precision·rounding·stability·reproducibility다

1. **Range** — overflow/underflow 없이 필요한 magnitude를 표현하는가.
2. **Precision** — local ULP/error가 domain tolerance를 만족하는가.
3. **Rounding** — operation order와 mode가 요구 semantics와 맞는가.
4. **Stability** — algorithm이 input/rounding error를 과도하게 증폭하지 않는가.
5. **Reproducibility** — target이 statistical tolerance인지 bitwise identity인지 명시했는가.

부동소수점 correctness는 `double 쓰면 안전`이 아니라 이 다섯 조건을 algorithm과 platform 전체에서 관리하는 것이다.
