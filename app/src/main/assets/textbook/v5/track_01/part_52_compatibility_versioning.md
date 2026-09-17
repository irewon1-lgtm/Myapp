# PART 52 · Compatibility and Versioning — ABI, API, schema, rolling upgrades

소프트웨어 변경의 위험은 새 버전이 단독으로 정상 동작하는지보다 **서로 다른 버전이 동시에 존재할 때 같은 byte·symbol·field·state를 같은 의미로 해석하는지**에서 결정된다. Source compatibility, binary compatibility, wire compatibility, persistent-data compatibility는 서로 다른 계약이다. 한 계층의 호환성을 유지했다고 다른 계층까지 자동으로 안전해지지 않는다. 배포·롤백·점진적 migration은 이 계약들이 겹치는 시간 구간을 설계하는 작업이다.

## CHAPTER 01 · source·binary·wire·data compatibility는 서로 독립된 축이다

Source-compatible 변경은 기존 source code가 새 library/header/compiler에서 다시 build될 수 있다는 의미에 가깝다. 이미 build된 binary가 새 library와 그대로 link/load되는지는 binary ABI 문제이며, network message를 old/new peer가 함께 읽는지는 wire compatibility 문제다. 저장된 row/file/snapshot을 old/new executable이 읽을 수 있는지는 persistent-data compatibility다. Function parameter를 추가했지만 default argument로 source compile은 유지되어도 symbol mangling이나 calling convention이 바뀌면 binary는 깨질 수 있다. JSON field를 추가해 wire는 유지되어도 database schema migration이 rollback 불가능하면 운영 호환성은 깨진다. Change review는 네 축을 각각 표로 판정해야 한다.

## CHAPTER 02 · backward와 forward compatibility는 읽는 방향이 반대다

Backward compatibility는 보통 새 consumer가 old producer/data를 처리할 수 있는 성질이고, forward compatibility는 old consumer가 미래 producer/data의 일부를 견딜 수 있는 성질이다. Rolling deployment에서는 두 방향이 동시에 필요할 수 있다. 새 server가 old request를 받아야 하고 old server도 새 client가 보내는 message를 일정 기간 처리해야 한다. Data store도 migration 후 새 row를 old binary가 읽을 가능성이 있다. `하위 호환`이라는 한 단어로 끝내면 어느 방향을 보장하는지 모호해진다. Producer version×consumer version matrix를 작성하고 각 cell의 허용 결과를 명시해야 mixed-version window를 안전하게 설계할 수 있다.

## CHAPTER 03 · syntactic compatibility와 semantic compatibility를 분리한다

Parser가 message를 성공적으로 읽는다고 업무 의미까지 호환되는 것은 아니다. Field type이 그대로 integer여도 단위가 seconds에서 milliseconds로 바뀌면 syntax는 같지만 의미는 깨진다. Enum 값 `ACTIVE`의 정책을 바꾸거나 `0`을 unknown에서 valid state로 재정의하는 것도 같은 문제다. Schema diff tool은 field 존재·type 변화는 잡지만 invariant·unit·ordering·authorization 의미 변화를 자동으로 증명하지 못한다. Compatibility review에는 value domain, unit, default, nullability, idempotency, ordering, error semantics 같은 의미 계약이 포함되어야 한다. Semantic change는 이름이 그대로여도 versioned behavior가 필요할 수 있다.

## CHAPTER 04 · additive change도 old reader가 unknown field를 어떻게 처리하는지에 달려 있다

Field 추가는 흔히 안전하다고 말하지만 format이 unknown field를 무시·보존·거부하는지에 따라 결과가 다르다. Strict parser가 정의되지 않은 key를 error로 처리하면 JSON field 하나 추가도 old client를 깨뜨린다. Unknown field를 읽었다가 old component가 object를 다시 serialize할 때 제거해 버리면 round-trip 중 미래 정보가 유실될 수 있다. Protocol Buffers처럼 unknown field preservation semantics가 있는 format도 language/runtime version에 따라 세부를 확인해야 한다. Additive change를 안전하게 만들려면 old reader behavior, proxy/read-modify-write path, signature/canonicalization 영향을 실제 mixed-version test로 확인한다.

## CHAPTER 05 · field removal은 reader보다 writer migration을 먼저 끝내야 한다

Field를 더 이상 사용하지 않는 새 code가 나왔다고 즉시 storage/wire field를 삭제하면 아직 old binary가 그 field를 요구하는 동안 rollback과 rolling deployment가 깨진다. 먼저 모든 writer가 새 representation도 함께 쓰거나 old field 의존을 제거하고, 모든 reader가 old/new 양쪽을 처리하도록 배포한 뒤, 관측을 통해 old consumer가 사라졌음을 확인하고 마지막에 physical removal을 수행한다. 이 순서를 expand-contract pattern으로 볼 수 있다. Cleanup commit은 기능 개발보다 늦게 들어가며 deployment fleet version과 background job/versioned client까지 확인해야 한다. `코드 검색에서 참조 0`은 외부 consumer 부재의 증명이 아니다.

## CHAPTER 06 · rename은 add+copy+deprecate+remove의 여러 단계 변경으로 취급한다

Field/column 이름을 한 번에 바꾸면 old writer는 old name에 쓰고 new reader는 new name만 보는 split-brain이 생길 수 있다. 안전한 rename은 새 name을 추가하고 read fallback 또는 dual-write로 값을 동기화하고, 모든 producer/consumer migration 후 old name을 제거하는 단계가 필요하다. 두 field가 동시에 존재하는 기간에는 어느 값이 source of truth인지, 둘이 다르면 어떤 것을 선택할지 명시해야 한다. Automatic sync trigger나 application dual-write도 partial failure가 가능하다. Rename은 cosmetic refactor가 아니라 temporal data-consistency protocol이다.

## CHAPTER 07 · enum evolution은 unknown value를 first-class state로 다뤄야 한다

새 producer가 enum value를 추가했을 때 old consumer가 parse 자체를 실패하면 forward compatibility가 없다. Unknown value를 generic `UNRECOGNIZED`로 보존하거나 raw numeric/string value를 유지하면 old code가 안전한 fallback을 선택할 수 있다. 하지만 authorization/security enum에서 unknown을 permissive default로 매핑하면 취약점이 된다. Exhaustive switch는 compile-time safety를 주지만 remote enum은 미래 값을 받을 수 있다는 사실을 표현해야 한다. Persisted numeric enum의 번호 재사용은 과거 data를 새 의미로 해석하게 만들므로 삭제된 번호를 reserved 상태로 유지한다.

## CHAPTER 08 · numeric width와 signedness 변경은 overflow·wire encoding·ABI를 동시에 건드린다

32-bit count를 64-bit로 넓히면 source-level로는 자연스러워 보여도 binary struct layout, database column, JSON consumer, protobuf field type, FFI를 모두 확인해야 한다. Old reader가 64-bit 값을 32-bit로 truncation하면 작은 테스트 값에서는 정상처럼 보이다 production 규모에서 깨진다. Signed→unsigned 변화는 negative sentinel 의미를 없애고 comparison rule을 바꾼다. Wire format이 varint라 byte encoding이 호환돼 보여도 valid value domain은 달라질 수 있다. Migration 기간에는 old range를 넘는 값 생성을 늦추거나 capability/version gate로 제한해야 한다.

## CHAPTER 09 · default value는 field absence와 명시적 값이 같은지 결정한다

Schema에서 field가 없을 때 `false/0/empty`로 해석하는 정책은 과거 message와 새 message 의미를 연결한다. 나중에 default를 바꾸면 old persisted message가 재해석되어 behavior가 달라질 수 있다. `field absent`, `field present with zero`, `field explicitly null`을 구분하는 format도 있고 하나로 합치는 format도 있다. Feature flag나 security policy에서 default 변화는 매우 위험하다. 새 semantics가 필요하면 별도 field/version을 두고 old absence의 의미를 영구히 유지하는 편이 안전할 수 있다. Compatibility test corpus에는 field omitted case를 반드시 포함한다.

## CHAPTER 10 · optional→required 변경은 이미 저장된 과거 data 때문에 쉽게 깨진다

현재 모든 producer가 field를 채운다고 확인해도 수년 전 persisted row/event에는 field가 없을 수 있다. Consumer가 required invariant를 가정하면 replay, backup restore, migration, analytics job에서 실패한다. Required로 강화하려면 먼저 historical backfill을 수행하고 validation으로 missing count를 0으로 만들고, old writer가 사라졌음을 확인한 뒤 enforcement를 켠다. 반대 방향 required→optional도 downstream이 null을 처리할 준비가 필요하다. Nullability 변화는 type annotation 한 줄이 아니라 historical dataset 전체와 writer fleet의 동시 조건이다.

## CHAPTER 11 · Protocol Buffers field number는 이름보다 강한 wire identity다

Protobuf에서 encoded field는 textual name이 아니라 numeric field number와 wire type에 의해 식별된다. Field를 삭제한 뒤 같은 번호를 새로운 의미에 재사용하면 old serialized data가 새 field로 해석될 수 있다. 삭제 번호와 이름을 reserved로 남기는 이유다. Compatible type change도 wire type과 semantic range를 확인해야 하며 `int32↔int64` 같은 일부 변화가 parser 수준에서 가능해도 old code range 문제가 남는다. Unknown field preservation은 proxy가 미래 field를 잃지 않게 할 수 있지만 JSON 변환 등 다른 representation 경계를 지나면 보존 여부가 달라질 수 있다.

## CHAPTER 12 · JSON의 느슨함은 schema-free가 아니라 hidden compatibility rule을 만든다

JSON 자체는 object key와 number/string/boolean/null syntax만 정의하며 application field 의미는 별도 계약이다. Number precision, duplicate key 처리, unknown field 허용, null과 absence 구분, timestamp format을 각 구현이 다르게 처리할 수 있다. `JSON이라 호환된다`는 말은 근거가 없다. OpenAPI/JSON Schema류 명세와 consumer contract test로 실제 field domain을 고정해야 한다. Parser를 지나치게 permissive하게 만들어 invalid input을 silent coercion하면 미래 compatibility는 좋아지는 대신 data-quality/security 문제가 생길 수 있다. 허용할 자유도와 거부할 ambiguity를 명확히 정한다.

## CHAPTER 13 · explicit version field는 negotiation을 돕지만 모든 change를 version switch로 해결하지 않는다

Message에 `version: 3`을 넣으면 parser가 schema generation을 선택하기 쉽지만 모든 minor additive change마다 전체 format version을 올리면 branch와 migration matrix가 폭발한다. Version field는 incompatible semantic epoch를 구분하고, compatible additive evolution은 field-level rule로 처리하는 조합이 일반적이다. Version 값 자체가 없던 v0 data도 해석 규칙이 필요하다. Writer는 자신이 생성하는 version을 명시하고 reader는 지원 범위 밖 version을 fail-closed하거나 upgrade path를 안내한다. `최신 version으로 강제 변환` 전에 unknown 정보 손실을 고려한다.

## CHAPTER 14 · capability negotiation은 version 숫자보다 실제 기능 조합을 표현할 수 있다

Peer A v5와 B v6라는 숫자만으로 어떤 compression, auth, feature를 공통 지원하는지 알 수 없을 때 capability set을 교환해 intersection을 선택할 수 있다. TLS/HTTP extension처럼 기능별 negotiation은 independent feature rollout에 유리하다. 그러나 capability 값이 많아지면 조합 테스트가 폭발하고 downgrade attack 가능성도 생긴다. Security-sensitive capability는 negotiation transcript와 policy를 인증해야 할 수 있다. Version은 protocol epoch, capability는 선택 가능한 기능이라는 역할 분리를 유지하면 mixed fleet reasoning이 쉬워진다.

## CHAPTER 15 · API endpoint versioning은 URL 이름보다 behavior lifetime 관리 문제다

`/v1`, `/v2`를 동시에 제공하면 routing은 쉬워지지만 business logic 두 버전을 실제로 얼마나 오래 유지할지 비용이 생긴다. Header/media-type versioning도 같은 maintenance problem을 다른 표면으로 표현한다. Breaking change를 새 endpoint로 분리한 뒤 old version usage telemetry와 sunset policy가 없으면 legacy path가 영구히 남는다. Client upgrade 속도, mobile release lag, external partner SLA를 고려해 support window를 정한다. Server 내부 implementation은 공유하더라도 response/error semantics는 version contract를 독립적으로 테스트한다.

## CHAPTER 16 · binary ABI는 struct layout·alignment·calling convention·symbol까지 묶인 계약이다

Header source가 동일하게 compile된다고 이미 배포된 plugin/shared library binary가 새 host와 동작하는 것은 아니다. Struct field 추가가 tail padding을 바꾸고 vtable slot 변화가 virtual call target을 바꾸며 exception/unwind ABI나 standard-library type layout 변화가 경계를 깨뜨릴 수 있다. Public ABI에는 opaque handle과 size/version field를 사용해 internal layout을 숨기는 방식이 유리하다. Cross-language FFI는 language object를 직접 ABI로 노출하지 않고 C-compatible fixed-width representation을 좁게 설계한다. ABI test는 실제 old binary를 rebuild하지 않고 새 library에 load해 봐야 한다.

## CHAPTER 17 · symbol visibility와 versioning은 같은 이름의 여러 ABI generation을 관리한다

Shared library가 exported symbol을 무제한 공개하면 internal refactor도 ABI commitment가 된다. Visibility를 제한하고 intended API만 export하면 evolution surface를 줄일 수 있다. 일부 ELF environment는 symbol versioning으로 동일 symbol name의 여러 ABI revision을 구분할 수 있다. SONAME/version policy와 package dependency가 함께 맞아야 loader가 incompatible library를 거부할 수 있다. `symbol이 존재함`만으로 signature/layout semantics가 호환된다는 뜻은 아니다. ABI checker와 old-client smoke test를 함께 사용한다.

## CHAPTER 18 · plugin ABI는 host와 plugin의 독립 release cadence를 견뎌야 한다

Plugin이 host internal class·allocator·exception type을 직접 공유하면 compiler/runtime upgrade마다 ABI가 깨질 수 있다. Stable plugin boundary는 versioned function table, opaque handle, explicit ownership, size-prefixed struct 같은 pattern으로 확장 가능성을 둔다. Host는 plugin이 요구하는 ABI version/capability를 load 전에 확인하고 unsupported plugin을 process crash가 아니라 명시적 rejection으로 처리한다. Callback table도 tail extension과 null capability 규칙을 정의해야 한다. Hot reload에서는 old plugin object가 모두 소멸한 뒤 library unload가 가능한지 lifetime contract를 검증한다.

## CHAPTER 19 · database migration은 schema state와 application version의 시간적 overlap을 설계한다

Single transaction으로 schema를 바꾸고 모든 process를 동시에 교체할 수 없는 production에서는 old/new application이 같은 DB를 일정 기간 공유한다. Column add는 비교적 쉬워도 rename/drop/type rewrite는 mixed-version writer를 깨뜨린다. Migration은 lock duration, table rewrite, replication lag, index build 비용까지 고려해야 한다. Application deploy보다 schema deploy를 먼저/나중에 어느 순서로 할지 change별 dependency graph를 만든다. Migration script 자체도 idempotent/restartable하고 현재 schema version을 검증해야 한다.

## CHAPTER 20 · expand-contract는 destructive change를 두 개의 compatibility window로 나눈다

Expand 단계에서는 new schema/API를 추가하지만 old path를 유지해 old/new binary가 모두 동작하게 만든다. Data backfill과 dual read/write로 새 representation을 채운 뒤 fleet가 모두 전환되면 contract 단계에서 old field/index/path를 제거한다. 두 단계를 한 release에 묶으면 rollback window를 잃는다. Cleanup 이전에 usage telemetry와 invariant query로 old dependency가 0인지 확인한다. Expand resource overhead와 dual-write consistency cost를 temporary migration budget으로 관리한다.

## CHAPTER 21 · dual-write는 호환성을 얻는 대신 두 저장소를 원자적으로 맞추는 문제를 만든다

Old/new column 또는 old/new service에 동시에 쓰면 어느 한쪽만 성공하는 partial failure가 생긴다. Single DB transaction 안이면 atomic하게 묶을 수 있지만 cross-service dual-write는 distributed transaction/outbox/reconciliation 같은 추가 mechanism이 필요하다. Migration 중 `둘 다 쓴다`는 지시만으로 일관성이 보장되지 않는다. Source of truth를 명확히 하고 mismatch metric, repair job, cutover condition을 정의한다. Backfill writer와 live writer가 race할 때 최신 value를 덮어쓰지 않도록 ordering/version을 사용한다.

## CHAPTER 22 · rolling upgrade에서는 N과 N+1이 양방향으로 통신하는 시간을 가정한다

Instance를 순차 교체하면 load balancer/queue를 통해 old producer→new consumer, new producer→old consumer 조합이 모두 생길 수 있다. 새 code가 배포되자마자 old code가 이해 못 하는 message를 생성하면 rollout 중 실패한다. `read old+new, write old` 단계로 reader compatibility를 먼저 배포하고, old fleet가 사라진 뒤 `write new`를 활성화하는 방식이 필요하다. Queue에 오래 남는 message와 offline client는 fleet보다 더 오래 old version을 유지할 수 있어 retention window를 포함한다.

## CHAPTER 23 · feature flag는 code deployment와 semantic activation을 분리한다

새 parser/field 처리 code를 모든 instance에 먼저 배포한 뒤 feature flag로 new producer behavior를 켜면 mixed-version risk를 줄일 수 있다. 하지만 flag state 자체가 distributed configuration이며 stale cache, partial propagation, rollback semantics를 가진다. Flag off가 old behavior를 완전히 복원하는지, migration이 이미 irreversible state를 만들었는지 확인한다. Long-lived flag는 branching complexity를 키우므로 compatibility window 종료 후 제거한다. Security behavior를 client-controlled flag에 맡기지 않는다.

## CHAPTER 24 · persistent file format은 수년 뒤 reader가 존재할 수 있다는 장기 계약이다

Application process는 몇 분마다 배포돼도 backup/document/index file은 수년간 남는다. File header에 magic, format version, endian, feature flag, section length, checksum을 두면 reader가 unsupported data를 명시적으로 거부할 수 있다. In-place structure memory dump를 format으로 사용하면 compiler/ABI 변화에 취약하다. Unknown optional section은 length를 이용해 skip 가능하게 설계하고 required feature bit가 unknown이면 fail한다. Migration tool은 original artifact를 보존하고 변환 후 semantic/hash verification을 수행한다.

## CHAPTER 25 · cache serialization은 persistent DB보다 짧아도 rolling compatibility가 필요하다

Distributed cache entry가 TTL 24시간이면 deploy 후 하루 동안 old serialized object가 남을 수 있다. New code가 class layout/serializer를 바꾸면 cache deserialize failure storm이 생길 수 있다. Cache miss로 안전하게 fallback 가능한지, bad entry를 evict할 수 있는지, cache key에 schema version을 넣을지 결정한다. Large cache를 동시에 version-bump하면 backend origin thundering herd가 생길 수 있어 staged warming이 필요하다. `캐시는 버려도 됨`은 fallback capacity가 있을 때만 안전하다.

## CHAPTER 26 · rollback compatibility는 forward migration보다 더 엄격할 수 있다

새 binary가 새 field를 쓰기 시작한 뒤 old binary로 rollback하면 old code가 unknown data를 거부하거나 더 위험하게 잘못 해석할 수 있다. Destructive DB migration, one-way encryption/key format change는 binary rollback을 불가능하게 만들 수 있다. Release 전에 `N+1 data를 N reader가 처리 가능한가`를 test한다. 불가능하면 rollback 대신 roll-forward recovery strategy를 준비하고 deployment UI/Runbook에 명시한다. `코드 revert 가능`과 `서비스 상태 rollback 가능`을 구분한다.

## CHAPTER 27 · deprecation은 사용 중단의 telemetry와 deadline을 가진 lifecycle이다

API를 deprecated annotation으로 표시해도 binary/external client가 계속 사용하면 제거할 수 없다. Call count, client version, owner를 관측해 migration progress를 추적한다. Deprecated path가 security patch를 계속 받아야 하는 support burden도 계산한다. Removal date와 replacement contract를 공개하고 build-time warning을 error로 강화하는 단계적 policy를 사용할 수 있다. Internal API도 monorepo 밖 artifact/plugin이 사용할 가능성을 inventory한다. Deprecation 완료는 source 검색이 아니라 runtime usage 0과 support window 종료로 판정한다.

## CHAPTER 28 · compatibility test matrix는 old/new artifact를 실제 조합해 실행한다

Unit test가 current schema만 검사하면 version evolution을 증명하지 못한다. Golden old messages/files, N/N+1 binaries, old/new DB snapshots를 fixture로 보존하고 reader/writer cross matrix를 자동 실행한다. ABI는 old prebuilt client binary를 새 library와 load하고, wire protocol은 old client/new server와 반대 조합을 모두 호출한다. Historical fixture를 current code로 재생성하면 과거 representation을 잃으므로 immutable artifact로 보관한다. Matrix 크기는 support window와 실제 deployed versions에 맞춰 제한한다.

## CHAPTER 29 · version provenance telemetry가 있어야 mixed fleet incident를 분석할 수 있다

Request/message/file에 무조건 version field를 넣을 필요는 없지만 observability에는 producer build, schema/protocol version, consumer build를 추적할 방법이 필요하다. Error rate가 특정 client 3.7에서만 높다면 빠르게 isolate할 수 있다. Database row마다 version을 저장하는지 event envelope에 schema id를 넣는지는 system design에 따라 다르다. Log에 sensitive payload를 남기지 않고 identifier/version metadata만 기록한다. Compatibility alert는 unknown version/field decode failure와 downgrade fallback rate를 별도 metric으로 둔다.

## CHAPTER 30 · evolution 계약은 변화 자체보다 overlap 기간의 허용 조합을 증명한다

변경마다 source/binary/wire/persistent-data 호환성을 분리하고 producer×consumer×storage version matrix를 작성한다. Destructive change는 expand→backfill/dual-read-write→fleet cutover→contract 단계로 분리하며 각 단계의 rollback 조건을 정의한다. Unknown enum/field, absence/default, numeric overflow, old binary restore를 fixture로 테스트한다. Cleanup은 telemetry가 old usage 0임을 증명한 뒤 수행한다. 호환성은 `새 버전이 정상`이라는 사실이 아니라 버전이 섞인 실제 배포 시간 동안 semantic invariant가 유지된다는 증거다.