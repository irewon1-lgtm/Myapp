# PART 52 · Compatibility and Versioning — ABI, API, schema, rolling upgrades

Compatibility는 “새 버전이 옛 버전과 같이 동작한다”는 한 문장으로 끝나지 않는다. Reader와 writer 중 누가 새 버전인지, wire schema·database·binary ABI·plugin interface·cache key 중 어느 경계인지에 따라 backward/forward 의미가 달라진다. Rolling upgrade에서는 여러 버전이 동시에 살아 있으므로 **변경 자체보다 혼재 기간의 protocol**이 더 중요하다. 이 PART는 additive change부터 expand/contract migration과 rollback까지 버전 진화를 구조적으로 다룬다.

---

## CHAPTER 01 · compatibility는 wire·semantic·operational 차원을 분리해야 한다

두 버전이 같은 bytes를 읽을 수 있다고 해서 같은 의미로 처리한다는 보장은 없다. Wire compatibility는 parser가 message를 해석할 수 있는지, semantic compatibility는 같은 field가 같은 의미를 가지는지, operational compatibility는 mixed-version deployment가 안전한지를 묻는다. 세 차원을 한 단어로 합치면 위험한 변경이 “호환됨”으로 표시될 수 있다.

예를 들어 새 field를 추가해 old reader가 무시할 수 있어도 그 field가 authorization 결정에 필수라면 semantic하게 안전하지 않다. 반대로 wire format이 달라도 gateway 변환으로 operational migration이 가능할 수 있다.

변경 리뷰에서는 어떤 compatibility dimension을 보장하는지 명시한다. “backward compatible” 대신 `new reader reads old data`, `old reader safely ignores field X`처럼 방향을 적는다.

## CHAPTER 02 · backward와 forward compatibility는 reader/writer 방향을 뒤집어 생각한다

Backward compatibility는 보통 새 reader가 old data를 처리하는 능력을 말하고, forward compatibility는 old reader가 new writer의 data를 어느 정도 처리하는 능력을 말한다. 용어 사용이 조직마다 혼동될 수 있어 실제 matrix를 쓰는 편이 안전하다.

Rolling upgrade에서는 `old writer→new reader`, `new writer→old reader`, `old-old`, `new-new` 네 조합이 일정 기간 존재할 수 있다. 새 binary가 혼자 잘 동작하는지만 테스트하면 절반의 경계를 놓친다.

Compatibility test suite는 version pair를 parameter로 실행한다. 최소 지원 버전과 최신 버전 사이의 실제 혼재 조합을 지속적으로 검증한다.

## CHAPTER 03 · semantic compatibility는 같은 field 이름보다 의미 변화에 더 민감하다

API field `timeout`이 과거에는 seconds였는데 새 버전에서 milliseconds가 되면 wire type과 이름은 같아도 의미가 깨진다. Default 값, null 해석, ordering, retry semantics 같은 behavior 변화도 semantic incompatibility다.

이런 변경은 schema diff tool만으로 잡기 어렵다. Contract test와 domain invariant가 필요하다. Consumer가 어떤 assumption을 가지고 있는지 inventory를 유지하면 위험을 줄일 수 있다.

의미 변경이 필요하면 새 field나 versioned behavior를 도입하고 migration 기간을 둔다. 기존 field의 의미를 조용히 재정의하는 것을 피한다.

## CHAPTER 04 · additive change는 가장 안전한 진화 패턴이지만 조건이 있다

새 optional field를 추가하면 old reader가 unknown field를 무시하는 format에서는 forward compatibility를 유지하기 쉽다. 하지만 consumer가 strict validation으로 unknown field를 거부하거나 JSON object를 exact equality로 비교한다면 additive change도 깨질 수 있다.

Writer가 새 field를 반드시 필요로 하는 logic을 바로 사용하면 old reader가 무시하는 동안 semantic mismatch가 생길 수 있다. 먼저 reader를 배포하고 usage를 관찰한 뒤 writer behavior를 전환하는 단계가 필요하다.

Additive라는 형태만 보고 안전하다고 결론내리지 말고 unknown-field policy와 business requirement를 함께 테스트한다.

## CHAPTER 05 · field removal은 writer 중단보다 reader 의존성 제거가 먼저다

Field를 없애려면 consumer가 더 이상 읽지 않는다는 증거가 필요하다. Writer에서 즉시 제거하면 오래된 reader가 default를 사용하거나 기능을 잃을 수 있다. 먼저 새 reader가 field 없이 동작하도록 배포하고 telemetry로 read usage를 줄인 뒤 writer를 중단한다.

Persisted data에는 과거 field가 오래 남을 수 있다. Parser는 제거된 field를 일정 기간 무시할 수 있어야 하고 migration job이 필요할 수도 있다.

Schema registry나 code search만으로 모든 consumer를 알 수 없는 환경에서는 access telemetry와 deprecation window를 둔다. 제거는 구현 작업보다 dependency discovery가 더 어려울 수 있다.

## CHAPTER 06 · rename은 실제로는 add + copy + deprecate + remove다

Field나 column 이름을 한 번에 바꾸면 old/new 버전 중 하나가 깨진다. 안전한 rename은 새 이름을 추가하고, 일정 기간 두 이름을 읽고/쓸 수 있게 하며, 모든 consumer가 전환된 뒤 old 이름을 제거하는 여러 단계 migration이다.

Database column rename도 application rolling upgrade와 결합하면 같은 문제가 있다. Old binary가 old column을 찾는 동안 schema가 먼저 바뀌면 startup부터 실패할 수 있다.

단순 refactoring처럼 보여도 external contract가 있는 이름은 versioned state다. IDE rename으로 끝내지 말고 migration sequence를 설계한다.

## CHAPTER 07 · enum 진화는 unknown value 처리 정책이 핵심이다

새 enum value를 writer가 보내면 old reader의 exhaustive switch가 default 없이 crash하거나 validation error를 낼 수 있다. Wire type이 integer/string이라 읽을 수 있어도 application behavior는 깨진다.

Reader는 미래 value를 unknown bucket으로 처리하거나 capability negotiation으로 writer가 old peer에게 보내지 않게 할 수 있다. Security-sensitive enum에서는 unknown을 permissive default로 매핑하면 안 된다.

Test는 known values만 돌리지 말고 임의의 future value를 넣는다. Generated code가 unknown enum을 보존하는지 drop하는지도 format별로 확인한다.

## CHAPTER 08 · numeric width 변경은 range와 representation을 동시에 고려해야 한다

32-bit field를 64-bit로 넓히면 새 reader는 old 값을 처리하기 쉽지만 old reader가 큰 새 값을 받으면 overflow나 truncation이 생길 수 있다. Signedness 변경은 같은 bit pattern의 의미를 바꿀 수 있다.

Wire format이 varint인지 fixed-width인지에 따라 binary compatibility도 달라진다. Database column type과 language type mapping이 모두 같은 범위를 지원하는지 확인한다.

Migration 동안 writer는 old peer가 처리할 수 있는 range를 넘지 않도록 guard할 수 있다. Capacity가 실제로 커지기 전 compatibility phase를 분리한다.

## CHAPTER 09 · default semantics는 field 부재와 명시적 값의 차이를 보존해야 한다

일부 serialization에서는 field가 없을 때 language-level default와 구분되지 않을 수 있다. 예를 들어 `false`가 명시된 것인지 old writer가 field 자체를 모르는지 모호해진다. 새 behavior를 default 변경으로 도입하면 old data 의미가 바뀔 수 있다.

Presence bit, nullable wrapper, explicit version을 사용해 필요한 경우 부재를 표현한다. Database default도 migration 중 old/new writer가 서로 다른 값을 생성할 수 있어 주의한다.

Compatibility test는 field omitted, explicit default, explicit non-default 세 case를 구분한다. Happy path 한 값만으로는 default 변화 bug를 찾기 어렵다.

## CHAPTER 10 · required와 optional은 schema 문법보다 rollout 순서 문제다

새 required field를 즉시 추가하면 old writer가 값을 보내지 못해 new reader가 거부한다. 안전한 절차는 먼저 optional로 추가해 reader가 default/absence를 처리하도록 배포하고, 모든 writer가 값을 보내는 것이 확인된 뒤 requirement를 강화하는 것이다.

반대로 required field를 optional로 완화하면 downstream code가 null을 처리하지 못할 수 있다. Schema validation만 느슨하게 만든다고 application이 안전해지지 않는다.

Requiredness 변화는 telemetry로 실제 population을 확인한다. “모든 새 request는 있다”가 persisted old record에도 적용되는지 따로 본다.

## CHAPTER 11 · Protobuf field number는 이름보다 강한 wire identity다

Protobuf 계열에서 field number는 wire format의 핵심 식별자다. 이름을 바꿔도 번호를 유지하면 wire compatibility를 유지할 수 있지만, 제거한 번호를 새 의미에 재사용하면 오래된 bytes가 완전히 다른 field로 해석될 수 있다.

삭제한 number와 name을 reserved로 남기는 이유다. Schema lint가 재사용을 막도록 자동화한다.

Migration review에서는 source diff보다 descriptor diff를 본다. Generated code 이름이 깨끗해 보여도 wire identity가 바뀌면 위험하다.

## CHAPTER 12 · JSON은 느슨한 format이지만 consumer behavior는 오히려 더 제각각일 수 있다

JSON parser 자체는 unknown field를 허용할 수 있지만 application validator, typed binding, signature canonicalization이 strict할 수 있다. Number precision도 language마다 달라 큰 integer가 손실될 수 있다.

Key order를 의미 있게 사용하거나 duplicate key를 다르게 처리하는 consumer가 있으면 interoperability 문제가 생긴다. RFC-level syntax만 맞는다고 ecosystem compatibility가 보장되지 않는다.

Contract test는 실제 client library와 serializer를 사용한다. Generic JSON tool 하나의 성공으로 모든 consumer를 대표하지 않는다.

## CHAPTER 13 · version field는 자동으로 호환성을 해결하지 않고 분기점을 명시할 뿐이다

Payload에 `version: 2`를 넣으면 reader가 어떤 규칙을 적용할지 선택할 수 있지만, version마다 전체 parser를 복제하면 조합이 폭발할 수 있다. Field-level evolution으로 해결 가능한 변경까지 global version으로 나누면 maintenance가 어려워진다.

반대로 의미가 근본적으로 달라졌다면 명시적 major version이 더 안전하다. “버전 필드는 나쁘다/좋다”가 아니라 변화의 범위에 맞춰 사용한다.

Version 값이 unknown일 때 fail-closed할지 best-effort할지도 정의한다. Silent fallback으로 v1 parser를 쓰면 잘못된 interpretation이 생길 수 있다.

## CHAPTER 14 · capability negotiation은 version 숫자보다 실제 기능 교집합을 찾는다

Peer가 version 3이라고 해서 모든 optional feature를 지원한다고 가정하면 vendor extension이나 partial implementation에서 깨질 수 있다. Capability bit/set으로 지원 기능을 명시하고 교집합을 선택하면 더 유연하다.

하지만 negotiation 자체도 downgrade attack이나 config drift의 대상이 될 수 있다. Security feature는 minimum capability를 강제하고 negotiation 결과를 audit할 필요가 있다.

운영에서는 negotiated feature distribution을 metric으로 남긴다. 오래된 capability가 얼마나 남아 있는지 알아야 deprecation 시점을 결정할 수 있다.

## CHAPTER 15 · API versioning은 URL보다 behavior contract를 어떻게 분리하는지가 핵심이다

`/v1`, header, content type 등 여러 versioning 방식이 있지만 중요한 것은 old contract를 얼마나 오래 유지하고 routing·docs·observability가 version을 구분하는지다. Path만 v2로 바꾸고 내부 behavior를 공유하면 hidden coupling이 남을 수 있다.

Major version을 늘리는 비용이 크다고 old endpoint에서 breaking change를 몰래 넣으면 consumer 신뢰를 잃는다. Deprecation 기간과 migration guide를 제공한다.

Traffic을 version별로 측정해 실제 old-client 비중을 확인한다. Calendar date만 보고 종료하지 않는다.

## CHAPTER 16 · binary ABI는 source code가 같아도 layout과 calling convention이 달라지면 깨진다

함수 symbol 이름이 남아 있어도 parameter passing, structure layout, alignment, vtable, exception ABI가 바뀌면 old binary와 new library가 함께 동작하지 않을 수 있다. Source compatibility와 binary compatibility를 분리해야 한다.

Compiler option, architecture, standard library version도 ABI에 영향을 줄 수 있다. Plugin이나 system library는 header만 재컴파일 테스트해서 충분하지 않다.

ABI checker와 old binary integration test를 사용한다. 실제 배포 artifact를 link/load해 runtime 호출까지 검증하는 것이 가장 직접적이다.

## CHAPTER 17 · symbol versioning은 같은 이름의 여러 ABI 세대를 library 안에서 관리할 수 있다

Dynamic linker의 symbol version mechanism을 사용하면 old binary가 옛 symbol implementation을, new binary가 새 version을 요구하도록 만들 수 있다. 이를 통해 library가 일부 ABI 진화를 지원할 수 있다.

하지만 version graph와 default symbol을 잘못 설정하면 새 build가 의도치 않게 old implementation을 사용하거나 반대가 될 수 있다. Export map을 source control에서 관리한다.

Symbol version은 semantic compatibility를 자동 보장하지 않는다. Function signature가 맞아도 side effect가 바뀌면 여전히 application bug가 될 수 있다.

## CHAPTER 18 · plugin ABI는 host와 독립 배포되는 code의 경계를 더 엄격하게 만든다

Plugin은 host와 release cadence가 다를 수 있어 structure size, function table, ownership 규칙을 안정적으로 정의해야 한다. C ABI처럼 단순한 boundary를 두거나 versioned function table을 사용하면 compiler-specific layout 의존성을 줄일 수 있다.

Host가 새 callback을 추가할 때 table size/version을 확인해 old plugin이 없는 field를 읽지 않게 한다. Memory allocation과 free를 어느 side에서 하는지도 ABI contract에 포함한다.

Plugin discovery 시 incompatible version을 명확히 거부하고 reason을 기록한다. Crash로 compatibility를 판별하지 않는다.

## CHAPTER 19 · database migration은 schema와 application version이 동시에 움직이는 distributed change다

Table에 column을 추가하는 DDL 하나도 old/new application이 동시에 접근하면 rollout sequence가 필요하다. Long-running transaction과 replica lag 때문에 schema change가 모든 node에 즉시 동일하게 보이지 않을 수도 있다.

Migration은 lock duration, rewrite cost, index build를 고려해야 한다. Data volume이 크면 schema compatibility와 operational capacity가 연결된다.

Deploy plan에는 `schema expand → reader deploy → writer deploy → backfill → old reader retire → contract`처럼 단계와 rollback point를 명시한다.

## CHAPTER 20 · expand/contract는 mixed-version 기간을 안전하게 통과하는 기본 패턴이다

Expand 단계에서는 old/new 버전이 모두 동작할 수 있도록 새 field·column·endpoint를 추가한다. 새 version이 완전히 배포되고 data migration이 끝난 뒤 contract 단계에서 old 요소를 제거한다. Breaking change를 두 개의 compatible change로 분해하는 방식이다.

중간 상태가 며칠 지속될 수 있으므로 임시 dual read/write logic도 production quality가 필요하다. “잠깐만 쓸 코드”라고 test를 줄이면 실제 rollout 중 가장 위험한 부분이 된다.

각 단계 완료 조건을 telemetry로 정의한다. 배포가 끝났다는 사실과 old behavior 사용이 0이라는 사실은 다르다.

## CHAPTER 21 · dual write는 migration을 돕지만 두 destination의 불일치를 새 문제로 만든다

Old/new field나 database에 동시에 쓰면 점진적 전환이 가능하지만 한쪽 write만 성공하는 partial failure가 생긴다. Transaction으로 묶을 수 없는 system에서는 reconciliation과 source-of-truth 정책이 필요하다.

읽기 전환 전에 두 representation을 shadow compare해 divergence를 측정한다. 단순 success count보다 value-level mismatch가 중요하다.

Dual write 기간을 가능한 짧게 하고 종료 조건을 명확히 한다. 영구적으로 두 path를 유지하면 복잡성과 failure surface가 늘어난다.

## CHAPTER 22 · rolling upgrade는 cluster 안에 여러 version이 공존하는 정상 상태를 만든다

Node를 하나씩 교체하면 old와 new가 서로 RPC하고 shared storage를 읽는다. 따라서 upgrade compatibility는 단일 binary test가 아니라 mixed-version topology test다. Leader election이나 shard movement로 어떤 version이 어떤 역할을 맡을지도 변한다.

새 feature를 code deploy와 동시에 켜지 않고 feature gate로 나중에 활성화하면 mixed period를 단순화할 수 있다. 모든 node가 새 reader를 가진 후 writer format을 전환한다.

Rollout 중 version distribution과 inter-version error를 관찰한다. Canary만 성공해도 cluster interaction이 안전하다는 보장은 없다.

## CHAPTER 23 · feature flag는 code 배포와 behavior 활성화를 분리한다

새 code를 먼저 모든 node에 배포하고 flag를 단계적으로 켜면 rollback이 빠르고 mixed-version 위험을 줄일 수 있다. 하지만 flag 자체가 configuration compatibility와 cleanup 책임을 만든다.

Flag가 schema write를 활성화한다면 old binary로 rollback할 수 있는지 확인한다. 일단 irreversible data를 쓰기 시작하면 flag off만으로 완전한 rollback이 되지 않을 수 있다.

Flag owner, expiry, default를 관리한다. 오래된 flag가 누적되면 가능한 state 조합이 폭발해 test coverage가 떨어진다.

## CHAPTER 24 · file format은 수년 뒤 old artifact를 읽어야 하므로 장기 compatibility가 필요하다

Network API는 client를 업데이트할 수 있지만 backup, document, database file은 오래 저장된다. Reader는 old generation을 지속 지원하거나 명시적 migration tool을 제공해야 한다.

Format header에 magic, version, feature flag, checksum을 넣어 interpretation을 self-describing하게 한다. Unknown mandatory feature가 있으면 추측해서 읽지 않는다.

Golden corpus에 과거 release가 만든 실제 파일을 보존하고 최신 reader로 계속 테스트한다. Source code가 사라진 old writer를 재현하지 않아도 compatibility를 검증할 수 있다.

## CHAPTER 25 · cache key도 schema version을 포함하지 않으면 stale interpretation이 생길 수 있다

Serialized object를 cache에 저장한 뒤 application schema가 바뀌면 새 reader가 old bytes를 읽지 못하거나, 읽더라도 default 때문에 의미가 달라질 수 있다. Cache를 ephemeral이라고 무시해도 rolling deploy 동안 old entry는 충분히 오래 존재할 수 있다.

Key namespace에 schema generation을 넣으면 새 version이 자연스럽게 cold cache로 시작할 수 있다. 다만 cache stampede와 memory doubling을 고려한다.

Cross-version shared cache를 유지하려면 serialization compatibility test를 둔다. Cache TTL만 믿고 migration 시간을 추정하지 않는다.

## CHAPTER 26 · rollback 가능성은 새 version이 남긴 state가 old version에 읽히는지에 달려 있다

Binary를 이전 버전으로 되돌리는 것은 쉽지만 새 schema·file·message가 이미 persist되었다면 old binary가 시작하지 못할 수 있다. 그래서 deploy 전 rollback compatibility를 별도 질문으로 다룬다.

Irreversible migration 전에는 backup과 forward-fix plan을 준비한다. Feature flag를 끄는 것과 data rollback은 다르다.

Rollback drill에서 실제 old artifact를 실행해 새 version이 만든 state를 읽는지 확인한다. 문서상 “rollback supported”보다 직접적인 증거다.

## CHAPTER 27 · deprecation은 제거 날짜가 아니라 consumer migration process다

Old API를 deprecated 표시만 해두면 사용자는 계속 남을 수 있다. Usage telemetry, warning, migration guide, owner contact를 통해 실제 의존성을 줄여야 한다.

Library에서는 compile warning이 도움이 되지만 runtime API는 access log가 더 중요할 수 있다. Internal consumer도 예외가 아니다.

Removal gate는 deprecation 기간 경과뿐 아니라 remaining usage가 허용 수준인지 확인한다. 예외 consumer가 있다면 명시적으로 연장하거나 adapter를 둔다.

## CHAPTER 28 · compatibility matrix는 지원 범위를 표로 만들어 숨은 조합을 드러낸다

Version N reader가 N-2 writer data를 읽는지, N writer가 N-1 peer와 통신하는지 등을 matrix로 관리하면 지원 policy가 명확해진다. 모든 조합을 무한히 지원할 수 없으므로 minimum supported version을 정한다.

Test automation은 matrix의 supported cell을 실제로 실행하고 unsupported cell이 명확한 error로 실패하는지도 본다. Silent misinterpretation이 가장 위험하다.

Matrix는 release마다 업데이트한다. 문서와 test가 다른 지원 범위를 말하지 않게 하나의 source of truth로 관리한다.

## CHAPTER 29 · version telemetry가 있어야 언제 old path를 제거할지 판단할 수 있다

Request header, schema generation, client version, file format version을 privacy를 침해하지 않는 범위에서 집계한다. Distribution을 보면 old version이 자연히 사라지는지, 특정 tenant에 고정되어 있는지 알 수 있다.

Telemetry 자체가 version spoofing에 취약할 수 있으므로 security decision의 유일한 근거로 쓰지 않는다. 하지만 deprecation planning과 rollout 관찰에는 강력하다.

Unknown/newer version error rate도 별도 metric으로 둔다. 호환성 문제가 발생했을 때 배포와 즉시 상관관계를 찾을 수 있다.

## CHAPTER 30 · evolution contract는 변경 전후가 아니라 혼재 기간 전체를 설계한다

안전한 version evolution은 새 schema가 예쁘게 정의됐는지보다 old/new reader와 writer가 동시에 존재할 때 모든 path가 어떤 의미를 갖는지에 달려 있다. Additive change, expand/contract, capability negotiation은 이 mixed state를 관리하는 도구다.

계획에는 deploy 순서, telemetry gate, rollback point, irreversible step을 명시한다. Binary ABI, database, file format, cache도 같은 원리로 다룰 수 있다.

최종 검증은 compatibility matrix의 실제 version pair를 실행하는 것이다. 문법적 schema diff와 unit test만으로는 semantic·operational compatibility를 증명할 수 없다.
