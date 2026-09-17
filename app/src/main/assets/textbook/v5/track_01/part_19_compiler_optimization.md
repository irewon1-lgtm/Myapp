# PART 19 · Compiler Optimization — IR, dataflow, alias, loop transformation

컴파일러 최적화은 IR·control-flow·value relation의 상태 변화로 이해해야 한다. 각 장은 개별 메커니즘의 semantics, failure boundary, evidence를 분리해 다룬다. 성능 개선보다 의미 보존과 복구 가능성을 먼저 확인한다.


---

## CHAPTER 01 · IR

IR은 source syntax를 지운 뒤 type, control flow, memory operation을 명시적으로 남겨 후속 analysis와 transformation의 공통 기반이 된다. IR의 의미는 결과값보다 state transition과 invariant에서 드러난다. IR 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

IR 오류는 평균지표에서 숨는 경우가 많다. invalid-assumption·miscompile는 IR의 tail에서 먼저 나타난다. IR에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 IR 분석의 중심증거다. IR의 IR-diff·remark·differential-run를 application symptom과 매칭한다. IR 변경 후 adversarial case도 다시 실행한다. IR 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 IR 코드와 문서에 같이 남긴다.

IR 안전성은 암묵state를 줄일수록 높아진다. IR owner·generation·limit·ordering을 API에 드러낸다. IR global-flag 의존은 restart에서 취약해질 수 있다. IR state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 02 · well-formedness

well-formed IR은 parser가 읽을 수 있다는 뜻보다 강하며, type 일치·SSA dominance·terminator·phi predecessor 관계 같은 구조 invariant를 만족해야 한다. well-formedness의 의미는 결과값보다 state transition과 invariant에서 드러난다. well-formedness 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

well-formedness trade-off는 빠름과 느림만의 문제가 아니다. well-formedness이 implicit-assumption붕괴를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. well-formedness 변경은 recovery 가능성까지 포함해 평가한다.

well-formedness 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. before/after-state·timeline가 well-formedness 전후에 어떻게 달라졌는지 본다. well-formedness 가설과 관측이 다르면 원인을 다시 세운다. well-formedness 설정값을 workload 밖 magic-number로 남기지 않는다. well-formedness 운영기준은 semanticequivalence와passordering과 연결한다.

well-formedness 안전성은 암묵state를 줄일수록 높아진다. well-formedness owner·generation·limit·ordering을 API에 드러낸다. well-formedness global-flag 의존은 restart에서 취약해질 수 있다. well-formedness state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 03 · CFG

CFG는 basic block과 successor edge로 분기 구조를 표현해 reachability, loop, dominance 같은 analysis의 그래프 입력이 된다. CFG의 의미는 결과값보다 state transition과 invariant에서 드러난다. CFG 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

CFG 오류는 평균지표에서 숨는 경우가 많다. invalid-assumption·miscompile는 CFG의 tail에서 먼저 나타난다. CFG에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

CFG 진단에는 최적화전후IR,optimizationremark,generatedcode,differentialtest가 필요하다. CFG 관련 IR-diff·remark·differential-run를 같은 시간축에 연결한다. CFG 수정이 맞으면 원인metric도 함께 변해야 한다. CFG 회귀검사는 boundary·failure·concurrency를 포함한다. semanticequivalence와passordering이 CFG의 최종 설계기준이다.

CFG 안전성은 암묵state를 줄일수록 높아진다. CFG owner·generation·limit·ordering을 API에 드러낸다. CFG global-flag 의존은 restart에서 취약해질 수 있다. CFG state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 04 · dominance

dominator relation은 entry에서 특정 block으로 가는 모든 경로가 선행 block을 통과하는지 나타내며 SSA definition의 유효 범위를 증명한다. dominance의 의미는 결과값보다 state transition과 invariant에서 드러난다. dominance 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

dominance trade-off는 빠름과 느림만의 문제가 아니다. dominance이 implicit-assumption붕괴를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. dominance 변경은 recovery 가능성까지 포함해 평가한다.

dominance incident에서 최적화전후IR,optimizationremark,generatedcode,differentialtest를 먼저 보존한다. before/after-state·timeline의 최초 divergence가 dominance 원인후보다. dominance 수정은 가장 작은 state-boundary에 적용한다. dominance 인접경로가 유지되는지도 재검증한다. semanticequivalence와passordering이 dominance의 회귀방지 기준이다.

dominance 안전성은 암묵state를 줄일수록 높아진다. dominance owner·generation·limit·ordering을 API에 드러낸다. dominance global-flag 의존은 restart에서 취약해질 수 있다. dominance state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 05 · SSA

SSA는 register-like value에 하나의 definition을 부여해 def-use chain을 명확히 하고 상수전파·DCE·value numbering의 근거를 만든다. SSA의 의미는 결과값보다 state transition과 invariant에서 드러난다. SSA 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

SSA 오류는 평균지표에서 숨는 경우가 많다. invalid-assumption·miscompile는 SSA의 tail에서 먼저 나타난다. SSA에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 SSA 분석의 중심증거다. SSA의 IR-diff·remark·differential-run를 application symptom과 매칭한다. SSA 변경 후 adversarial case도 다시 실행한다. SSA 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 SSA 코드와 문서에 같이 남긴다.

SSA 재현에는 exact build와 runtime-version이 필요하다. SSA 환경차이는 같은 증상을 다른 원인으로 만들 수 있다. SSA 비교실험은 변경변수 하나만 움직이는 편이 강하다. SSA negative-result도 원인후보를 제거하는 증거다.

---

## CHAPTER 06 · PHI

phi node는 여러 predecessor에서 들어온 값을 merge block에서 edge별로 선택하며 loop recurrence와 branch merge를 SSA 안에 표현한다. PHI의 의미는 결과값보다 state transition과 invariant에서 드러난다. PHI 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

PHI trade-off는 빠름과 느림만의 문제가 아니다. PHI이 invalid-assumption·miscompile를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. PHI 변경은 recovery 가능성까지 포함해 평가한다.

PHI 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. IR-diff·remark·differential-run가 PHI 전후에 어떻게 달라졌는지 본다. PHI 가설과 관측이 다르면 원인을 다시 세운다. PHI 설정값을 workload 밖 magic-number로 남기지 않는다. PHI 운영기준은 semanticequivalence와passordering과 연결한다.

PHI 변경은 rollback 경계도 함께 정의해야 한다. PHI state-format이 바뀌면 binary rollback만으로 부족하다. PHI compatibility는 중간실패 상태에서도 확인해야 한다. PHI migration과 rollback testcase를 분리해 유지한다.

---

## CHAPTER 07 · mem2reg

mem2reg는 주소가 escape하지 않는 local alloca를 SSA value로 승격해 불필요한 load/store와 alias uncertainty를 줄인다. mem2reg의 의미는 결과값보다 state transition과 invariant에서 드러난다. mem2reg 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

컴파일러·최적화 장애에서 mem2reg만 단독원인으로 보지 않는다. mem2reg과 downstream의 state 불일치가 더 중요할 수 있다. invalid-assumption·miscompile가 mem2reg의 정상응답처럼 보일 수도 있다. mem2reg 주변의 timeout·retry·rollback을 함께 검증한다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 mem2reg 분석의 중심증거다. mem2reg의 IR-diff·remark·differential-run를 application symptom과 매칭한다. mem2reg 변경 후 adversarial case도 다시 실행한다. mem2reg 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 mem2reg 코드와 문서에 같이 남긴다.

mem2reg은 평균보다 distribution으로 보는 편이 안전하다. mem2reg p95·p99에서 wait·retry·fault를 분리한다. mem2reg 비용이 다른 resource로 이동했는지도 확인한다. mem2reg optimization은 end-to-end metric으로 판정한다.

---

## CHAPTER 08 · dataflow

dataflow analysis는 CFG를 따라 fact를 전달하고 fixed point를 계산하며 forward/backward, may/must 성질에 따라 lattice와 meet 연산이 달라진다. dataflow의 의미는 결과값보다 state transition과 invariant에서 드러난다. dataflow 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

dataflow trade-off는 빠름과 느림만의 문제가 아니다. dataflow이 invalid-assumption·miscompile를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. dataflow 변경은 recovery 가능성까지 포함해 평가한다.

dataflow incident에서 최적화전후IR,optimizationremark,generatedcode,differentialtest를 먼저 보존한다. IR-diff·remark·differential-run의 최초 divergence가 dataflow 원인후보다. dataflow 수정은 가장 작은 state-boundary에 적용한다. dataflow 인접경로가 유지되는지도 재검증한다. semanticequivalence와passordering이 dataflow의 회귀방지 기준이다.

dataflow 안전성은 암묵state를 줄일수록 높아진다. dataflow owner·generation·limit·ordering을 API에 드러낸다. dataflow global-flag 의존은 restart에서 취약해질 수 있다. dataflow state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 09 · liveness

liveness는 program point 이후 값이 다시 사용될 가능성을 계산해 register allocation, spill 판단, dead-code 분석에 사용된다. liveness의 의미는 결과값보다 state transition과 invariant에서 드러난다. liveness 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

liveness 오류는 평균지표에서 숨는 경우가 많다. implicit-assumption붕괴는 liveness의 tail에서 먼저 나타난다. liveness에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

liveness 진단에는 최적화전후IR,optimizationremark,generatedcode,differentialtest가 필요하다. liveness 관련 before/after-state·timeline를 같은 시간축에 연결한다. liveness 수정이 맞으면 원인metric도 함께 변해야 한다. liveness 회귀검사는 boundary·failure·concurrency를 포함한다. semanticequivalence와passordering이 liveness의 최종 설계기준이다.

liveness은 작은 workload에서 안정적으로 보일 수 있다. liveness metadata비용은 scale 임계점에서 지배적이 된다. liveness benchmark에는 input-size와 concurrency를 기록한다. liveness scale-curve가 비선형이면 숨은 queue를 의심한다.

---

## CHAPTER 10 · constant · propagation

constant propagation은 compile-time known value를 전파해 arithmetic을 접고 branch를 확정하며 unreachable path 제거 기회를 만든다. constant·propagation의 의미는 결과값보다 state transition과 invariant에서 드러난다. constant·propagation 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

constant·propagation trade-off는 빠름과 느림만의 문제가 아니다. constant·propagation이 state·boundary·contract를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. constant·propagation 변경은 recovery 가능성까지 포함해 평가한다.

constant·propagation 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. before/after-state·timeline가 constant·propagation 전후에 어떻게 달라졌는지 본다. constant·propagation 가설과 관측이 다르면 원인을 다시 세운다. constant·propagation 설정값을 workload 밖 magic-number로 남기지 않는다. constant·propagation 운영기준은 semanticequivalence와passordering과 연결한다.

constant·propagation 재현에는 exact build와 runtime-version이 필요하다. constant·propagation 환경차이는 같은 증상을 다른 원인으로 만들 수 있다. constant·propagation 비교실험은 변경변수 하나만 움직이는 편이 강하다. constant·propagation negative-result도 원인후보를 제거하는 증거다.

---

## CHAPTER 11 · value · numbering

value numbering은 서로 다른 expression이 같은 semantic value를 만든다는 사실을 번호로 추적해 중복 계산을 합친다. value·numbering의 의미는 결과값보다 state transition과 invariant에서 드러난다. value·numbering 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

컴파일러·최적화 장애에서 value·numbering만 단독원인으로 보지 않는다. value·numbering과 downstream의 state 불일치가 더 중요할 수 있다. implicit-assumption붕괴가 value·numbering의 정상응답처럼 보일 수도 있다. value·numbering 주변의 timeout·retry·rollback을 함께 검증한다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 value·numbering 분석의 중심증거다. value·numbering의 before/after-state·timeline를 application symptom과 매칭한다. value·numbering 변경 후 adversarial case도 다시 실행한다. value·numbering 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 value·numbering 코드와 문서에 같이 남긴다.

value·numbering 변경은 rollback 경계도 함께 정의해야 한다. value·numbering state-format이 바뀌면 binary rollback만으로 부족하다. value·numbering compatibility는 중간실패 상태에서도 확인해야 한다. value·numbering migration과 rollback testcase를 분리해 유지한다.

---

## CHAPTER 12 · dead-code elimination

DCE는 결과가 쓰이지 않고 observable side effect가 없는 instruction만 제거하며 volatile·atomic·I/O 같은 effect는 보존해야 한다. dead-code elimination의 의미는 결과값보다 state transition과 invariant에서 드러난다. dead-code elimination 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

dead-code elimination trade-off는 빠름과 느림만의 문제가 아니다. dead-code elimination이 state·boundary·contract를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. dead-code elimination 변경은 recovery 가능성까지 포함해 평가한다.

dead-code elimination incident에서 최적화전후IR,optimizationremark,generatedcode,differentialtest를 먼저 보존한다. before/after-state·timeline의 최초 divergence가 dead-code elimination 원인후보다. dead-code elimination 수정은 가장 작은 state-boundary에 적용한다. dead-code elimination 인접경로가 유지되는지도 재검증한다. semanticequivalence와passordering이 dead-code elimination의 회귀방지 기준이다.

dead-code elimination은 평균보다 distribution으로 보는 편이 안전하다. dead-code elimination p95·p99에서 wait·retry·fault를 분리한다. dead-code elimination 비용이 다른 resource로 이동했는지도 확인한다. dead-code elimination optimization은 end-to-end metric으로 판정한다.

---

## CHAPTER 13 · alias · analysis

alias analysis는 두 memory reference가 같은 object를 가리킬 가능성을 분류해 load/store 재배치, vectorization, LICM의 안전 범위를 결정한다. alias·analysis의 의미는 결과값보다 state transition과 invariant에서 드러난다. alias·analysis 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

alias·analysis 오류는 평균지표에서 숨는 경우가 많다. invalid-assumption·miscompile는 alias·analysis의 tail에서 먼저 나타난다. alias·analysis에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

alias·analysis 진단에는 최적화전후IR,optimizationremark,generatedcode,differentialtest가 필요하다. alias·analysis 관련 IR-diff·remark·differential-run를 같은 시간축에 연결한다. alias·analysis 수정이 맞으면 원인metric도 함께 변해야 한다. alias·analysis 회귀검사는 boundary·failure·concurrency를 포함한다. semanticequivalence와passordering이 alias·analysis의 최종 설계기준이다.

alias·analysis 안전성은 암묵state를 줄일수록 높아진다. alias·analysis owner·generation·limit·ordering을 API에 드러낸다. alias·analysis global-flag 의존은 restart에서 취약해질 수 있다. alias·analysis state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 14 · escape · analysis

escape analysis는 object reference가 현재 scope나 thread 밖에서 관찰될 수 있는지 추론해 stack allocation·scalar replacement 같은 최적화 가능성을 넓힌다. escape·analysis의 의미는 결과값보다 state transition과 invariant에서 드러난다. escape·analysis 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

escape·analysis trade-off는 빠름과 느림만의 문제가 아니다. escape·analysis이 invalid-assumption·miscompile를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. escape·analysis 변경은 recovery 가능성까지 포함해 평가한다.

escape·analysis 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. IR-diff·remark·differential-run가 escape·analysis 전후에 어떻게 달라졌는지 본다. escape·analysis 가설과 관측이 다르면 원인을 다시 세운다. escape·analysis 설정값을 workload 밖 magic-number로 남기지 않는다. escape·analysis 운영기준은 semanticequivalence와passordering과 연결한다.

escape·analysis 재현에는 exact build와 runtime-version이 필요하다. escape·analysis 환경차이는 같은 증상을 다른 원인으로 만들 수 있다. escape·analysis 비교실험은 변경변수 하나만 움직이는 편이 강하다. escape·analysis negative-result도 원인후보를 제거하는 증거다.

---

## CHAPTER 15 · scalar · replacement

scalar replacement는 aggregate object의 field를 독립 SSA value로 분해해 object materialization과 load/store를 제거할 수 있다. scalar·replacement의 의미는 결과값보다 state transition과 invariant에서 드러난다. scalar·replacement 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

컴파일러·최적화 장애에서 scalar·replacement만 단독원인으로 보지 않는다. scalar·replacement과 downstream의 state 불일치가 더 중요할 수 있다. implicit-assumption붕괴가 scalar·replacement의 정상응답처럼 보일 수도 있다. scalar·replacement 주변의 timeout·retry·rollback을 함께 검증한다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 scalar·replacement 분석의 중심증거다. scalar·replacement의 before/after-state·timeline를 application symptom과 매칭한다. scalar·replacement 변경 후 adversarial case도 다시 실행한다. scalar·replacement 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 scalar·replacement 코드와 문서에 같이 남긴다.

scalar·replacement 변경은 rollback 경계도 함께 정의해야 한다. scalar·replacement state-format이 바뀌면 binary rollback만으로 부족하다. scalar·replacement compatibility는 중간실패 상태에서도 확인해야 한다. scalar·replacement migration과 rollback testcase를 분리해 유지한다.

---

## CHAPTER 16 · inlining

inlining은 call overhead 제거보다 caller의 constant·type·alias context를 callee에 노출해 추가 최적화를 가능하게 하는 효과가 더 크다. inlining의 의미는 결과값보다 state transition과 invariant에서 드러난다. inlining 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

inlining trade-off는 빠름과 느림만의 문제가 아니다. inlining이 invalid-assumption·miscompile를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. inlining 변경은 recovery 가능성까지 포함해 평가한다.

inlining incident에서 최적화전후IR,optimizationremark,generatedcode,differentialtest를 먼저 보존한다. IR-diff·remark·differential-run의 최초 divergence가 inlining 원인후보다. inlining 수정은 가장 작은 state-boundary에 적용한다. inlining 인접경로가 유지되는지도 재검증한다. semanticequivalence와passordering이 inlining의 회귀방지 기준이다.

inlining은 평균보다 distribution으로 보는 편이 안전하다. inlining p95·p99에서 wait·retry·fault를 분리한다. inlining 비용이 다른 resource로 이동했는지도 확인한다. inlining optimization은 end-to-end metric으로 판정한다.

---

## CHAPTER 17 · interprocedural · analysis

IPA는 function 경계를 넘어 call graph, global state, side effect를 분석하며 LTO 환경에서 더 많은 cross-module 최적화 기회를 얻는다. interprocedural·analysis의 의미는 결과값보다 state transition과 invariant에서 드러난다. interprocedural·analysis 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

interprocedural·analysis 오류는 평균지표에서 숨는 경우가 많다. invalid-assumption·miscompile는 interprocedural·analysis의 tail에서 먼저 나타난다. interprocedural·analysis에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

interprocedural·analysis 진단에는 최적화전후IR,optimizationremark,generatedcode,differentialtest가 필요하다. interprocedural·analysis 관련 IR-diff·remark·differential-run를 같은 시간축에 연결한다. interprocedural·analysis 수정이 맞으면 원인metric도 함께 변해야 한다. interprocedural·analysis 회귀검사는 boundary·failure·concurrency를 포함한다. semanticequivalence와passordering이 interprocedural·analysis의 최종 설계기준이다.

interprocedural·analysis 안전성은 암묵state를 줄일수록 높아진다. interprocedural·analysis owner·generation·limit·ordering을 API에 드러낸다. interprocedural·analysis global-flag 의존은 restart에서 취약해질 수 있다. interprocedural·analysis state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 18 · devirtualization

devirtualization은 receiver type을 하나 또는 작은 집합으로 좁혀 indirect call을 direct call로 바꾸고 이후 inlining을 가능하게 한다. devirtualization의 의미는 결과값보다 state transition과 invariant에서 드러난다. devirtualization 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

devirtualization trade-off는 빠름과 느림만의 문제가 아니다. devirtualization이 invalid-assumption·miscompile를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. devirtualization 변경은 recovery 가능성까지 포함해 평가한다.

devirtualization 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. IR-diff·remark·differential-run가 devirtualization 전후에 어떻게 달라졌는지 본다. devirtualization 가설과 관측이 다르면 원인을 다시 세운다. devirtualization 설정값을 workload 밖 magic-number로 남기지 않는다. devirtualization 운영기준은 semanticequivalence와passordering과 연결한다.

devirtualization 재현에는 exact build와 runtime-version이 필요하다. devirtualization 환경차이는 같은 증상을 다른 원인으로 만들 수 있다. devirtualization 비교실험은 변경변수 하나만 움직이는 편이 강하다. devirtualization negative-result도 원인후보를 제거하는 증거다.

---

## CHAPTER 19 · loop · canonicalization

loop canonicalization은 preheader, latch, dedicated exit 같은 정규 형태를 만들어 후속 loop analysis가 같은 구조를 기대할 수 있게 한다. loop·canonicalization의 의미는 결과값보다 state transition과 invariant에서 드러난다. loop·canonicalization 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

컴파일러·최적화 장애에서 loop·canonicalization만 단독원인으로 보지 않는다. loop·canonicalization과 downstream의 state 불일치가 더 중요할 수 있다. invalid-assumption·miscompile가 loop·canonicalization의 정상응답처럼 보일 수도 있다. loop·canonicalization 주변의 timeout·retry·rollback을 함께 검증한다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 loop·canonicalization 분석의 중심증거다. loop·canonicalization의 IR-diff·remark·differential-run를 application symptom과 매칭한다. loop·canonicalization 변경 후 adversarial case도 다시 실행한다. loop·canonicalization 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 loop·canonicalization 코드와 문서에 같이 남긴다.

loop·canonicalization 변경은 rollback 경계도 함께 정의해야 한다. loop·canonicalization state-format이 바뀌면 binary rollback만으로 부족하다. loop·canonicalization compatibility는 중간실패 상태에서도 확인해야 한다. loop·canonicalization migration과 rollback testcase를 분리해 유지한다.

---

## CHAPTER 20 · LICM

LICM은 iteration마다 변하지 않는 계산을 loop 밖으로 이동하지만 memory alias와 side-effect ordering이 안전하다는 증명이 먼저 필요하다. LICM의 의미는 결과값보다 state transition과 invariant에서 드러난다. LICM 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

LICM trade-off는 빠름과 느림만의 문제가 아니다. LICM이 invalid-assumption·miscompile를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. LICM 변경은 recovery 가능성까지 포함해 평가한다.

LICM incident에서 최적화전후IR,optimizationremark,generatedcode,differentialtest를 먼저 보존한다. IR-diff·remark·differential-run의 최초 divergence가 LICM 원인후보다. LICM 수정은 가장 작은 state-boundary에 적용한다. LICM 인접경로가 유지되는지도 재검증한다. semanticequivalence와passordering이 LICM의 회귀방지 기준이다.

LICM은 평균보다 distribution으로 보는 편이 안전하다. LICM p95·p99에서 wait·retry·fault를 분리한다. LICM 비용이 다른 resource로 이동했는지도 확인한다. LICM optimization은 end-to-end metric으로 판정한다.

---

## CHAPTER 21 · induction

induction-variable analysis는 반복마다 일정 규칙으로 변하는 값을 recurrence로 표현해 trip count, range, bounds, vectorization 정보를 얻는다. induction의 의미는 결과값보다 state transition과 invariant에서 드러난다. induction 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

induction 오류는 평균지표에서 숨는 경우가 많다. implicit-assumption붕괴는 induction의 tail에서 먼저 나타난다. induction에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

induction 진단에는 최적화전후IR,optimizationremark,generatedcode,differentialtest가 필요하다. induction 관련 before/after-state·timeline를 같은 시간축에 연결한다. induction 수정이 맞으면 원인metric도 함께 변해야 한다. induction 회귀검사는 boundary·failure·concurrency를 포함한다. semanticequivalence와passordering이 induction의 최종 설계기준이다.

induction 안전성은 암묵state를 줄일수록 높아진다. induction owner·generation·limit·ordering을 API에 드러낸다. induction global-flag 의존은 restart에서 취약해질 수 있다. induction state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 22 · unrolling

loop unrolling은 여러 iteration body를 복제해 branch overhead와 dependency 간격을 줄이지만 code size와 register pressure를 늘린다. unrolling의 의미는 결과값보다 state transition과 invariant에서 드러난다. unrolling 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

unrolling trade-off는 빠름과 느림만의 문제가 아니다. unrolling이 state·boundary·contract를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. unrolling 변경은 recovery 가능성까지 포함해 평가한다.

unrolling 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. before/after-state·timeline가 unrolling 전후에 어떻게 달라졌는지 본다. unrolling 가설과 관측이 다르면 원인을 다시 세운다. unrolling 설정값을 workload 밖 magic-number로 남기지 않는다. unrolling 운영기준은 semanticequivalence와passordering과 연결한다.

unrolling 재현에는 exact build와 runtime-version이 필요하다. unrolling 환경차이는 같은 증상을 다른 원인으로 만들 수 있다. unrolling 비교실험은 변경변수 하나만 움직이는 편이 강하다. unrolling negative-result도 원인후보를 제거하는 증거다.

---

## CHAPTER 23 · loop · vectorizer

loop vectorizer는 loop-carried dependency와 alias 조건을 분석해 여러 iteration을 vector lane으로 묶을 수 있는지 판단한다. loop·vectorizer의 의미는 결과값보다 state transition과 invariant에서 드러난다. loop·vectorizer 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

컴파일러·최적화 장애에서 loop·vectorizer만 단독원인으로 보지 않는다. loop·vectorizer과 downstream의 state 불일치가 더 중요할 수 있다. invalid-assumption·miscompile가 loop·vectorizer의 정상응답처럼 보일 수도 있다. loop·vectorizer 주변의 timeout·retry·rollback을 함께 검증한다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 loop·vectorizer 분석의 중심증거다. loop·vectorizer의 IR-diff·remark·differential-run를 application symptom과 매칭한다. loop·vectorizer 변경 후 adversarial case도 다시 실행한다. loop·vectorizer 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 loop·vectorizer 코드와 문서에 같이 남긴다.

loop·vectorizer 변경은 rollback 경계도 함께 정의해야 한다. loop·vectorizer state-format이 바뀌면 binary rollback만으로 부족하다. loop·vectorizer compatibility는 중간실패 상태에서도 확인해야 한다. loop·vectorizer migration과 rollback testcase를 분리해 유지한다.

---

## CHAPTER 24 · SLP

SLP vectorization은 서로 독립적인 scalar operation 묶음을 같은 vector instruction으로 재구성해 basic block 내부 병렬성을 활용한다. SLP의 의미는 결과값보다 state transition과 invariant에서 드러난다. SLP 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

SLP trade-off는 빠름과 느림만의 문제가 아니다. SLP이 state·boundary·contract를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. SLP 변경은 recovery 가능성까지 포함해 평가한다.

SLP incident에서 최적화전후IR,optimizationremark,generatedcode,differentialtest를 먼저 보존한다. before/after-state·timeline의 최초 divergence가 SLP 원인후보다. SLP 수정은 가장 작은 state-boundary에 적용한다. SLP 인접경로가 유지되는지도 재검증한다. semanticequivalence와passordering이 SLP의 회귀방지 기준이다.

SLP은 평균보다 distribution으로 보는 편이 안전하다. SLP p95·p99에서 wait·retry·fault를 분리한다. SLP 비용이 다른 resource로 이동했는지도 확인한다. SLP optimization은 end-to-end metric으로 판정한다.

---

## CHAPTER 25 · strength · reduction

strength reduction은 비싼 연산을 더 싼 연산이나 recurrence로 바꾸되 overflow와 target cost model이 semantic equivalence를 유지해야 한다. strength·reduction의 의미는 결과값보다 state transition과 invariant에서 드러난다. strength·reduction 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

strength·reduction 오류는 평균지표에서 숨는 경우가 많다. state·boundary·contract는 strength·reduction의 tail에서 먼저 나타난다. strength·reduction에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

strength·reduction 진단에는 최적화전후IR,optimizationremark,generatedcode,differentialtest가 필요하다. strength·reduction 관련 before/after-state·timeline를 같은 시간축에 연결한다. strength·reduction 수정이 맞으면 원인metric도 함께 변해야 한다. strength·reduction 회귀검사는 boundary·failure·concurrency를 포함한다. semanticequivalence와passordering이 strength·reduction의 최종 설계기준이다.

strength·reduction 안전성은 암묵state를 줄일수록 높아진다. strength·reduction owner·generation·limit·ordering을 API에 드러낸다. strength·reduction global-flag 의존은 restart에서 취약해질 수 있다. strength·reduction state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 26 · if · conversion

if-conversion은 branch를 predicated/select 형태로 바꿔 misprediction을 줄일 수 있지만 양쪽 연산 비용과 side effect를 함께 고려해야 한다. if·conversion의 의미는 결과값보다 state transition과 invariant에서 드러난다. if·conversion 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

if·conversion trade-off는 빠름과 느림만의 문제가 아니다. if·conversion이 state·boundary·contract를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. if·conversion 변경은 recovery 가능성까지 포함해 평가한다.

if·conversion 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. before/after-state·timeline가 if·conversion 전후에 어떻게 달라졌는지 본다. if·conversion 가설과 관측이 다르면 원인을 다시 세운다. if·conversion 설정값을 workload 밖 magic-number로 남기지 않는다. if·conversion 운영기준은 semanticequivalence와passordering과 연결한다.

if·conversion 재현에는 exact build와 runtime-version이 필요하다. if·conversion 환경차이는 같은 증상을 다른 원인으로 만들 수 있다. if·conversion 비교실험은 변경변수 하나만 움직이는 편이 강하다. if·conversion negative-result도 원인후보를 제거하는 증거다.

---

## CHAPTER 27 · pass · manager

pass manager는 analysis의 생성·보존·invalidated 상태와 pass ordering을 관리하며 한 transformation이 다음 pass의 전제조건을 만들기도 한다. pass·manager의 의미는 결과값보다 state transition과 invariant에서 드러난다. pass·manager 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

컴파일러·최적화 장애에서 pass·manager만 단독원인으로 보지 않는다. pass·manager과 downstream의 state 불일치가 더 중요할 수 있다. invalid-assumption·miscompile가 pass·manager의 정상응답처럼 보일 수도 있다. pass·manager 주변의 timeout·retry·rollback을 함께 검증한다.

최적화전후IR,optimizationremark,generatedcode,differentialtest가 pass·manager 분석의 중심증거다. pass·manager의 IR-diff·remark·differential-run를 application symptom과 매칭한다. pass·manager 변경 후 adversarial case도 다시 실행한다. pass·manager 평균값 하나만 좋아지면 완료로 보지 않는다. semanticequivalence와passordering을 pass·manager 코드와 문서에 같이 남긴다.

pass·manager 변경은 rollback 경계도 함께 정의해야 한다. pass·manager state-format이 바뀌면 binary rollback만으로 부족하다. pass·manager compatibility는 중간실패 상태에서도 확인해야 한다. pass·manager migration과 rollback testcase를 분리해 유지한다.

---

## CHAPTER 28 · PGO

PGO는 실제 workload의 branch·call hotness를 compiler에 제공해 inlining, layout, code generation 선택을 대표 사용 패턴에 맞춘다. PGO의 의미는 결과값보다 state transition과 invariant에서 드러난다. PGO 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

PGO trade-off는 빠름과 느림만의 문제가 아니다. PGO이 loss·observer-effect를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. PGO 변경은 recovery 가능성까지 포함해 평가한다.

PGO incident에서 최적화전후IR,optimizationremark,generatedcode,differentialtest를 먼저 보존한다. loss-counter·clock·build-ID의 최초 divergence가 PGO 원인후보다. PGO 수정은 가장 작은 state-boundary에 적용한다. PGO 인접경로가 유지되는지도 재검증한다. semanticequivalence와passordering이 PGO의 회귀방지 기준이다.

PGO은 평균보다 distribution으로 보는 편이 안전하다. PGO p95·p99에서 wait·retry·fault를 분리한다. PGO 비용이 다른 resource로 이동했는지도 확인한다. PGO optimization은 end-to-end metric으로 판정한다.

---

## CHAPTER 29 · JIT · deoptimization

JIT deoptimization은 speculative optimization의 전제가 깨질 때 optimized frame을 interpreter/baseline state로 복원해 correctness를 유지한다. JIT·deoptimization의 의미는 결과값보다 state transition과 invariant에서 드러난다. JIT·deoptimization 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

JIT·deoptimization 오류는 평균지표에서 숨는 경우가 많다. old/new-generation혼선는 JIT·deoptimization의 tail에서 먼저 나타난다. JIT·deoptimization에 retry를 더하면 load가 증폭될 수도 있다. 잘못된전제에서의미를바꾸는miscompile를 막으려면 최초 invariant 위반을 찾는다.

JIT·deoptimization 진단에는 최적화전후IR,optimizationremark,generatedcode,differentialtest가 필요하다. JIT·deoptimization 관련 transition·generation·timeline를 같은 시간축에 연결한다. JIT·deoptimization 수정이 맞으면 원인metric도 함께 변해야 한다. JIT·deoptimization 회귀검사는 boundary·failure·concurrency를 포함한다. semanticequivalence와passordering이 JIT·deoptimization의 최종 설계기준이다.

JIT·deoptimization 안전성은 암묵state를 줄일수록 높아진다. JIT·deoptimization owner·generation·limit·ordering을 API에 드러낸다. JIT·deoptimization global-flag 의존은 restart에서 취약해질 수 있다. JIT·deoptimization state-machine을 test fixture와 공유하면 재현이 쉽다.

---

## CHAPTER 30 · miscompile

miscompile 조사는 source만 보지 않고 optimization level·IR diff·pass bisect·generated code·differential execution으로 의미가 처음 바뀐 지점을 좁힌다. miscompile의 의미는 결과값보다 state transition과 invariant에서 드러난다. miscompile 전후의 입력·출력 조건을 분리하면 최적화와 correctness를 같은 기준으로 비교할 수 있다.

miscompile trade-off는 빠름과 느림만의 문제가 아니다. miscompile이 invalid-assumption·miscompile를 줄여도 다른 비용이 늘 수 있다. 컴파일러·최적화의 잘못된전제에서의미를바꾸는miscompile를 별도 failure-domain으로 본다. miscompile 변경은 recovery 가능성까지 포함해 평가한다.

miscompile 검증은 최적화전후IR,optimizationremark,generatedcode,differentialtest에서 시작한다. IR-diff·remark·differential-run가 miscompile 전후에 어떻게 달라졌는지 본다. miscompile 가설과 관측이 다르면 원인을 다시 세운다. miscompile 설정값을 workload 밖 magic-number로 남기지 않는다. miscompile 운영기준은 semanticequivalence와passordering과 연결한다.

miscompile 재현에는 exact build와 runtime-version이 필요하다. miscompile 환경차이는 같은 증상을 다른 원인으로 만들 수 있다. miscompile 비교실험은 변경변수 하나만 움직이는 편이 강하다. miscompile negative-result도 원인후보를 제거하는 증거다.
