# V5 CONTENT RESEARCH MAP — 11 TRACKS = 11 BOOKS

This is an authoring map, not learner-facing filler and not a completion report. Parts may split further when the prerequisite graph or source evidence requires it. No part exists merely to hit a count.

## TRACK 01 — 컴퓨터와 프로그래밍의 언어

Primary spine: CSAPP3, OSTEP, TLPI, UNICODE, ANDROID-FUNDAMENTALS.

1. Information as physical state: bit/byte, binary/hex, signed and unsigned interpretation, overflow, endian, representation vs meaning.
2. Text is data: code point, encoding, UTF-8, grapheme, normalization, malformed input and why byte length != character count.
3. From source text to running program: parser/compiler/interpreter, object code, linker, loader, process creation and startup.
4. CPU execution model: instruction, register, program counter, call/return, stack frame, branch, exception; trace tiny programs at instruction/state level without turning the book into an assembly manual.
5. Memory hierarchy: register/cache/RAM/storage, locality, latency vs bandwidth, cache effects and why performance surprises happen.
6. Processes and virtual memory: address space, isolation, page, mapping, stack/heap, allocation, faults and out-of-memory failure modes.
7. Files and system I/O: descriptor/handle, open/read/write/close, buffering, durability, atomicity boundaries, path resolution and permissions.
8. Operating-system boundary: syscall, user/kernel mode, scheduling, blocking, concurrency and observable process state.
9. Android as a real system: APK, manifest, process, Linux UID/sandbox, component lifecycle, main thread, background work and process death.
10. Evidence-first diagnosis: CPU, memory, storage, process, file and Android examples traced from symptom to mechanism.

## TRACK 02 — 프로그래밍 사고와 문법

Primary spine: SICP, PYTHON-REF, CSAPP3, LEARNING-CLT-COMPUTING.

1. Expression, value, type and evaluation order before syntax memorization.
2. Names, binding, assignment and state; mutation vs rebinding; identity vs equality.
3. Conditionals as decision boundaries; truth values, short-circuit behavior, boundary conditions and state tables.
4. Iteration: invariants, termination, accumulation, traversal and off-by-one failures.
5. Functions: arguments, return values, call stack, local/global scope, closures and responsibility boundaries.
6. Collections and data modeling: sequence, mapping, set, record/object; choose representation from operations and invariants.
7. Errors: syntax/runtime/domain failure; exception propagation, contracts, validation and useful error messages.
8. Modules, packages, environments and dependency boundaries; import resolution and reproducibility.
9. Iteration protocols, resource lifetime/context management and asynchronous execution as control flow.
10. From worked trace to independent program: design, test, debug and audit AI-generated code without guessing.

## TRACK 03 — 자료구조와 알고리즘

Primary spine: CLRS4, ODS, CSAPP3, LEARNING-CLT-COMPUTING.

1. Cost model first: input size, operation count, asymptotic notation, constants, memory and real-machine caveats.
2. Arrays/dynamic arrays/linked structures: layout, access, insertion, deletion, resizing and cache locality.
3. Stack/queue/deque: invariants, ring buffers, worklists, scheduling and bounded queues.
4. Hash tables: hash function, bucket, collision, load factor, resizing, adversarial cases and equality contracts.
5. Trees: traversal, BST invariant, balance, heap/priority queue and why tree shape controls cost.
6. Graphs: representation, BFS/DFS, visited state, cycle, topological order, shortest paths and weighted edges.
7. Search and sorting: linear/binary search preconditions; comparison sorting; stability; partitioning; external/system implications.
8. Recursion and divide-and-conquer: call tree, base case, stack cost, merge/partition reasoning.
9. Dynamic programming and greedy reasoning: state definition, recurrence, proof obligations and counterexamples.
10. Engineering algorithms: Unicode/text, large data, streaming, approximate/online choices, profiling before optimization.

## TRACK 04 — 웹 화면과 브라우저

Primary spine: WHATWG-HTML, CSS-CASCADE, WCAG22, MDN-WEB.

1. Browser navigation and document lifecycle: URL to response to parsed document.
2. HTML semantics: document structure, links/media/tables, semantic elements and accessibility tree consequences.
3. Forms and user input: labels, validation, keyboard use, autofill and submission boundaries.
4. CSS cascade: origin, importance, specificity, inheritance, computed values and debugging a winning declaration.
5. Box model and formatting: normal flow, containing blocks, overflow, sizing and coordinate systems.
6. Flexbox/Grid/responsive layout: intrinsic sizing, min/max constraints, breakpoints and failure on small screens.
7. Browser rendering pipeline: DOM/CSSOM, style, layout, paint, compositing; distinguish reflow/repaint myths from evidence.
8. DOM/events: event target/path, capture/bubble, default actions, focus and event delegation.
9. Accessibility as engineering: semantic structure, focus order, contrast, touch targets, screen readers and WCAG checks.
10. DevTools-driven capstone: reproduce a broken page, isolate HTML/CSS/event/performance causes, fix and regression-test.

## TRACK 05 — JavaScript와 TypeScript 깊게

Primary spine: ECMA262, TS-HANDBOOK, MDN-WEB.

1. ECMAScript execution model: source, execution context, lexical environment and completion values.
2. Values and coercion: primitive/reference behavior, Number/BigInt, equality, conversion and edge cases.
3. Scope, closures and lifetime: lexical capture, callbacks, memory retention and stale state.
4. Objects: property lookup, descriptor, prototype chain, class syntax and composition trade-offs.
5. Functions as values: callbacks, higher-order operations, this/binding and API design.
6. Modules: import/export, graph evaluation, cycles, bundling boundaries and side effects.
7. Asynchrony: task, microtask, Promise reaction, async/await desugared mental model and event-loop traces.
8. Failure/cancellation: rejected Promise, timeout, AbortController, cleanup and race conditions.
9. TypeScript: structural typing, narrowing, generics, unions, mapped/conditional types and soundness boundaries.
10. Boundary design: parse unknown external data, establish runtime contracts, then let static types carry the guarantee inward.

## TRACK 06 — 인터넷·네트워크·API

Primary spine: RFC1034, RFC1035, RFC9110, RFC8446, RFC9000, OPENAPI31.

1. Network path and layer boundaries: host/interface/router, packet/frame, address and route; what each layer can and cannot promise.
2. IP and routing: subnet intuition, default gateway, NAT, MTU, fragmentation/PMTUD and observable failure modes.
3. DNS: resolver, recursive/authoritative roles, records, TTL/cache, negative answers and split-horizon pitfalls.
4. Transport: TCP connection/reliability/flow/congestion vs UDP message service; ports and connection identity.
5. TLS 1.3: authentication, key agreement, certificate validation, confidentiality/integrity and common trust failures.
6. HTTP semantics: method, target, status, fields, representation, content negotiation, safe/idempotent meaning.
7. HTTP performance: persistent connections, multiplexing, caching, validators, proxies/CDNs and HTTP/3/QUIC boundaries.
8. API contracts: resource/data model, pagination/filtering, error schema, OpenAPI and compatibility.
9. Identity at the web boundary: cookies/session/token/OAuth concepts, CORS/same-origin/CSRF distinctions.
10. Reliability: timeout, retry, exponential backoff, jitter, idempotency key, rate limit and retry storms.
11. Streaming/realtime: SSE, WebSocket, long-lived connections and backpressure.
12. Packet-to-app debugging: DNS/TLS/HTTP/body failures reproduced with concrete evidence rather than guessed from UI symptoms.

## TRACK 07 — 서버와 백엔드

Primary spine: SRE, SRE-WORKBOOK, DDIA, OPENAPI31, OWASP-API.

1. Server process and request lifecycle from accept to response.
2. Routing/middleware/dependency boundaries and context propagation.
3. Parsing, validation, canonicalization and domain invariants.
4. Authentication vs authorization; object-level access and multi-tenant boundaries.
5. Database interaction and transaction ownership; connection pools and N+1 behavior.
6. Cache: key, freshness, invalidation, stampede, eviction and consistency trade-offs.
7. Queue/background work: delivery semantics, acknowledgement, retry, poison messages and dead-letter handling.
8. Idempotency/concurrency: duplicate request, optimistic/pessimistic control, outbox and race conditions.
9. Observability: structured logs, metrics, traces, correlation and actionable alerts.
10. Capacity/reliability: saturation, load shedding, circuit breaking, bulkheads, SLO/error budget.
11. Deployment/runtime: configuration, secret, health/readiness, graceful shutdown and rollback.
12. Production capstone: one request traced through edge, app, DB, queue and observability evidence.

## TRACK 08 — 데이터베이스

Primary spine: POSTGRES, DBSC7, DDIA.

1. Data model and invariants before SQL syntax.
2. Relational model: relation/tuple/attribute, key and foreign key, NULL and three-valued logic.
3. Query semantics: SELECT/FROM/WHERE/order/limit and why logical order differs from surface syntax.
4. JOIN: cardinality, duplicate multiplication, outer joins, anti/semi patterns and wrong-result diagnosis.
5. Normalization and deliberate denormalization: dependency, anomalies, boundaries and read/write trade-offs.
6. Constraints and data ownership: unique/check/FK, generated values and safe mutation.
7. Transactions: atomicity/consistency/isolation/durability, anomalies, isolation levels, MVCC and locks.
8. Deadlocks/contention: wait graphs, lock ordering, retry boundaries and transaction sizing.
9. Index internals: B-tree intuition, composite ordering, selectivity, covering/index-only behavior and write cost.
10. Query planner: statistics, cost estimates, EXPLAIN/ANALYZE and why an index can be ignored.
11. WAL/recovery/durability: commit path, crash recovery, checkpoints and backup consistency.
12. Replication/migration/backup: lag, failover, schema evolution, online change and restore drills.

## TRACK 09 — 보안과 데이터 보호

Primary spine: OWASP-ASVS, OWASP-API, NIST-SSDF, RFC8446, ANDROID-SECURITY.

1. Asset, threat, vulnerability, risk and trust boundary; draw attack paths before naming controls.
2. Authentication lifecycle: enrollment, password/passkey/MFA, session, recovery and account takeover paths.
3. Authorization: deny-by-default, object/function/property access, tenancy and confused-deputy problems.
4. Cryptographic primitives by purpose: randomness, hash/MAC, KDF, encryption, AEAD, signature and certificate.
5. Secrets and keys: generation, storage, rotation, scope and logging/exfiltration failures.
6. Injection/canonicalization: SQL/command/template/path contexts and context-specific encoding.
7. Browser attacks: XSS, CSRF, clickjacking, same-origin/CORS and cookie attributes.
8. Server-side attack surfaces: SSRF, upload, path traversal, deserialization and resource exhaustion.
9. Mobile security/privacy: sandbox, storage, IPC, network security and data minimization.
10. Supply chain: dependency trust, artifact provenance, signing, compromised updates and secure defaults.
11. Detection/response: security logging without secret leakage, incident containment and evidence preservation.
12. AI/agent security: prompt injection as untrusted input, tool authority, data exfiltration and least privilege.

## TRACK 10 — 오류·테스트·Git·빌드·배포

Primary spine: PROGIT2, GIT-DOCS, SRE, SRE-WORKBOOK, NIST-SSDF.

1. Debugging method: reproduce, observe, narrow, hypothesize, falsify, minimally change, regression-test.
2. Evidence: logs, debugger, trace, metrics, dumps and how instrumentation can mislead.
3. Unit tests: behavior boundary, deterministic inputs, assertions and test doubles.
4. Integration/E2E/contract tests: what each catches, cost, flakiness and environment control.
5. Property/fuzz/mutation thinking: generate cases, invariants and test-the-test weaknesses.
6. Git object model: blob/tree/commit/ref, working tree/index/HEAD and why commands behave as they do.
7. Branch/merge/rebase/conflict: graph transformations, safe recovery, revert/reset/restore distinctions.
8. Dependencies/build: lockfiles, reproducibility, compiler/toolchain/config inputs and cache correctness.
9. CI/artifacts/provenance: one source revision to one verifiable artifact; signing and supply-chain controls.
10. Deployment: migration compatibility, progressive rollout, health checks, rollback/roll-forward.
11. Incident practice: detection, triage, mitigation, communication, postmortem and learning loop.
12. CLEAN methodology: never equate unexecuted checks, simulations or stale artifacts with PASS.

## TRACK 11 — 소프트웨어 설계와 종합 프로젝트

Primary spine: DDIA, REFACTORING2, DDD, GOF, SRE.

1. Requirements as observable behavior, constraints, invariants and non-functional goals.
2. Decomposition: responsibility, cohesion, coupling and information hiding.
3. Interfaces/contracts: data ownership, error model, versioning and dependency direction.
4. State/data flow: source of truth, derived state, synchronization and temporal coupling.
5. Layer/module architecture: boundary placement, dependency inversion and when layers become ceremony.
6. Reuse/patterns: strategy/observer/adapter/factory and recognizing when a pattern is not justified.
7. Domain modeling: ubiquitous language, entities/value objects/aggregates and bounded contexts without cargo culting DDD.
8. Refactoring: preserve behavior while changing structure; characterize legacy behavior before surgery.
9. Distributed-system realities: partial failure, retries, ordering, consistency, replication and idempotency.
10. Performance/capacity: workload model, latency distribution, caching, queueing and measurement-driven optimization.
11. Reliability/operations: SLO, observability, graceful degradation, migration and incident readiness as design inputs.
12. Final system: specification -> architecture -> implementation -> tests -> security review -> build artifact -> deployment plan -> incident/rollback drill -> evidence-backed audit.

## Cross-book authoring rules

- A technical term that is necessary to understand a sentence is introduced before that sentence unless the current page gives a short definition at first use.
- Repeated definitions do not count as depth. Later books link back and add a new mechanism, constraint or consequence.
- Examples are original and small enough to trace completely. A second example exists only if it exposes a different failure mode or abstraction.
- Every command says what it changes before the learner runs it. Destructive commands are never default exercises.
- Code is paired with expected state/output and a failure interpretation where useful.
- Claims derived from a source are paraphrased. Copyrighted prose is not reproduced as chapter text.
- Source references are evidence anchors. The learner-facing text should read as one coherent Korean book, not a pile of citations.
