package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack09 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C09-S01",
            depthTitle("보안을 공격명 암기에서 threat model·session lifecycle·auditability로 올린다"),
            depthHeading("threat model은 “무엇을 지키나” 다음에 attacker capability를 구체화한다"),
            depthParagraph("""
같은 파일 업로드 기능도 익명 인터넷 사용자, 로그인 사용자, 내부 직원, 탈취된 service credential이 할 수 있는 일이 다르다. asset·entry point·trust boundary를 그린 뒤 attacker가 어떤 입력을 통제하고 어떤 권한을 얻으려 하는지 적는다. STRIDE 같은 분류는 빠진 질문을 찾는 체크리스트로 쓸 수 있지만, 이름을 채우는 것이 목적은 아니다.
"""),
            depthHeading("1. session fixation과 rotation"),
            depthParagraph("""
로그인 전 session id를 공격자가 알 수 있고 로그인 후에도 같은 id를 그대로 쓰면 공격자가 그 id로 로그인 session을 탈취할 수 있는 fixation 위험이 있다. 인증 수준이 올라갈 때 session identifier를 rotate하고, logout/password reset/MFA change 같은 보안 이벤트에서 기존 session을 revoke하는 정책이 필요하다.
"""),
            depthHeading("2. refresh token rotation과 reuse detection"),
            depthParagraph("""
긴 수명의 refresh token을 탈취당하면 access token이 짧아도 계속 재발급할 수 있다. refresh할 때 새 token을 발급하고 이전 token을 폐기하며, 이미 폐기된 token이 다시 사용되면 token family 전체를 revoke하는 reuse detection 전략을 사용할 수 있다. 여러 기기를 지원하면 device/session 단위 revoke 모델도 필요하다.
"""),
            depthHeading("3. MFA는 모든 공격을 없애지 않는다"),
            depthParagraph("""
TOTP나 security key는 password 탈취 위험을 줄이지만 phishing-resistant 수준과 recovery 절차가 방식마다 다르다. SMS는 SIM swap 위험이 있고, TOTP도 real-time phishing에 속을 수 있다. 중요한 관리자 작업에는 WebAuthn/FIDO2 같은 강한 factor와 step-up authentication을 고려한다.
"""),
            depthHeading("4. least privilege와 blast radius"),
            depthParagraph("""
thumbnail worker가 image bucket read/write만 필요하다면 전체 production DB admin 권한을 주지 않는다. credential이 유출됐을 때 공격자가 접근 가능한 resource 범위가 곧 blast radius다. service account마다 필요한 API/action/resource를 최소화하고 unused permission을 주기적으로 제거한다.
"""),
            depthHeading("5. audit log는 일반 debug log와 목적이 다르다"),
            depthParagraph("""
관리자가 권한을 부여하거나 돈이 움직이는 사건은 누가, 언제, 무엇을, 어떤 이전/이후 상태로 바꿨는지 변조하기 어렵게 기록해야 한다. debug log처럼 자유롭게 삭제/샘플링하면 조사 근거가 사라질 수 있다. audit log에는 secret 자체를 남기지 않으면서 actor, target, action, result, request id를 남긴다.
"""),
            depthCode("""
관리자 권한 변경 audit event
actor_user_id: U10
target_user_id: U44
action: ROLE_CHANGED
before: USER
after: ADMIN
request_id: Rabc
result: SUCCESS
timestamp: ...

password, token, full cookie는 기록하지 않는다.
"""),
            depthBullets(
                "로그인 성공만 확인하지 않고 session 발급·rotation·revoke 전체 생명주기를 그린다.",
                "MFA recovery가 가장 약한 우회 경로가 되지 않는지 threat model에 포함한다.",
                "service credential 권한은 기능별 최소 resource/action으로 제한한다."
            )
        ),
        expertPack(
            sectionId = "V1-C09-S02",
            depthTitle("암호화 기초 다음 단계: pepper·KDF·AEAD·envelope encryption·key rotation"),
            depthParagraph("""
V3가 hash, encryption, signature의 차이를 이미 설명했다면 이제 운영 가능한 key management로 간다. 암호 알고리즘 자체보다 key를 어디에 보관하고 어떻게 교체하며 어떤 context에서 재사용하지 않는지가 실제 사고를 좌우한다. 직접 새 암호를 만들지 않고 검증된 library와 platform KMS/HSM을 사용하는 원칙은 그대로 유지한다.
"""),
            depthHeading("1. password hash의 salt와 pepper는 역할이 다르다"),
            depthParagraph("""
salt는 사용자별 random 값으로 hash 옆에 저장해 같은 password가 같은 hash가 되는 것을 막는다. pepper는 모든/일부 password에 추가하는 비밀 값으로 DB와 별도 secret store에 보관해 DB dump만 탈취한 공격 비용을 높일 수 있다. pepper가 유출되면 rotation/rehash 전략이 필요하고, 잃어버리면 기존 password 검증이 불가능할 수 있다.
"""),
            depthHeading("2. KDF는 password에서 바로 AES key를 만들지 않게 한다"),
            depthParagraph("""
사람 password는 entropy가 낮기 때문에 단순 SHA-256 한 번으로 key를 만들면 brute force가 빠르다. Argon2id, scrypt, PBKDF2 같은 password KDF는 time/memory cost와 salt로 공격을 비싸게 만든다. parameter는 하드웨어가 발전하면 조정하고 로그인 시 old parameter hash를 새 설정으로 점진적으로 rehash할 수 있다.
"""),
            depthHeading("3. AEAD는 암호화와 변조 검증을 함께 한다"),
            depthParagraph("""
AES-GCM, ChaCha20-Poly1305 같은 AEAD는 plaintext를 숨길 뿐 아니라 authentication tag로 ciphertext/associated data 변조를 검출한다. nonce/IV 재사용 규칙을 어기면 보안이 크게 깨질 수 있으므로 library가 요구하는 nonce 길이와 unique 조건을 따른다. encryption만 하고 MAC을 빼먹는 식의 조합을 직접 만들지 않는다.
"""),
            depthHeading("4. envelope encryption은 data key와 master key를 분리한다"),
            depthParagraph("""
대용량 데이터를 KMS master key로 직접 매번 암호화하기보다 random data encryption key(DEK)로 데이터를 암호화하고, DEK를 KMS key(KEK)로 감싸 함께 저장한다. KMS는 작은 key wrap/unwrap만 수행한다. master key rotation 때 모든 대용량 ciphertext를 다시 암호화하지 않고 wrapped DEK만 재암호화할 수 있는 장점이 있다.
"""),
            depthHeading("5. key rotation은 versioning 문제다"),
            depthParagraph("""
ciphertext와 함께 key version/id를 저장해야 어떤 key로 복호화할지 알 수 있다. 새 write는 K2로 암호화하고 old data는 K1으로 읽되 background re-encryption으로 K2로 이동한 뒤 K1을 폐기한다. “key를 바꿨다” 한 줄로 끝나지 않고 dual-read 기간, failure recovery, backup에 남은 old key까지 생명주기를 관리한다.
"""),
            depthHeading("6. signature와 HMAC"),
            depthParagraph("""
HMAC은 shared secret을 아는 양쪽이 message integrity/authenticity를 검증하는 데 적합하고, digital signature는 private key 소유자만 서명하고 public key를 가진 누구나 검증할 수 있다. webhook provider와 receiver가 secret을 공유한다면 HMAC이 단순할 수 있고, 공개 검증이 필요하면 asymmetric signature가 필요할 수 있다.
"""),
            depthCode("""
envelope encryption
plaintext
 -> random DEK로 AEAD encryption -> ciphertext
DEK
 -> KMS/KEK로 wrap -> encrypted_DEK

저장:
key_version + nonce + encrypted_DEK + ciphertext + tag

rotation 시 새 write는 새 KEK 사용,
old wrapped DEK는 점진적으로 re-wrap
"""),
            depthBullets(
                "nonce/IV reuse 금지 조건은 암호 library 문서를 그대로 따른다.",
                "key id/version을 ciphertext metadata에 남겨 rotation 가능성을 확보한다.",
                "pepper·KMS credential·private key는 source repository와 DB dump에서 분리한다."
            )
        ),
        expertPack(
            sectionId = "V1-C09-S03",
            depthTitle("웹 공격을 이름에서 실제 parser·context·network 경계로 내려간다"),
            depthHeading("SQL parameterization으로도 식별자와 query 구조는 따로 처리해야 한다"),
            depthParagraph("""
prepared parameter는 value를 안전하게 전달하지만 table/column 이름이나 ORDER BY 방향을 parameter placeholder로 처리할 수 없는 경우가 많다. 사용자에게 sort field를 받으면 raw string을 SQL에 붙이지 말고 허용 목록을 내부 column name에 mapping한다. dynamic query builder도 결국 “코드가 구조를, 입력이 값만” 결정하도록 해야 한다.
"""),
            depthHeading("1. XSS escaping은 출력 context별로 다르다"),
            depthParagraph("""
HTML text, attribute, URL, JavaScript string context는 안전한 encoding 규칙이 다르다. HTML escaping한 문자열을 script context에 그대로 넣으면 안전하지 않을 수 있다. framework template의 auto-escape를 유지하고 raw HTML이 필요하면 well-reviewed sanitizer를 사용한다. CSP nonce/hash는 exploit 가능성을 줄이는 defense-in-depth이지 sanitizer 대체물이 아니다.
"""),
            depthHeading("2. CSRF token과 SameSite의 한계"),
            depthParagraph("""
SameSite cookie는 많은 cross-site request를 줄이지만 browser 호환/redirect/login flow와 subdomain 관계를 이해해야 한다. state-changing request에는 unpredictable CSRF token 또는 same-origin custom header 전략을 사용할 수 있다. XSS가 있으면 attacker가 page 안에서 token을 읽거나 request를 만들 수 있으므로 CSRF와 XSS는 별도 방어가 필요하다.
"""),
            depthHeading("3. SSRF에서 URL allowlist만으로 끝나지 않는 이유"),
            depthParagraph("""
evil.example이 처음 DNS lookup에서는 public IP를 반환하고 뒤이어 private IP로 바꾸는 DNS rebinding, redirect가 내부 주소로 향하는 경우, IPv6/decimal/hex IP 표기 우회가 있을 수 있다. resolve 후 IP range 검증, redirect마다 재검증, egress firewall/proxy, metadata service 보호를 함께 둔다.
"""),
            depthHeading("4. path traversal과 archive extraction"),
            depthParagraph("""
../만 제거해도 URL encoding, alternate separator, symlink 때문에 우회될 수 있다. 최종 canonical path가 허용 root 안에 있는지 확인한다. ZIP을 푸는 기능은 archive entry 이름이 ../../outside를 포함하는 Zip Slip, 압축 해제 크기가 폭발하는 zip bomb을 막기 위해 경로와 total expanded size를 제한한다.
"""),
            depthHeading("5. secret을 Git에 commit한 사고는 삭제 commit으로 끝나지 않는다"),
            depthParagraph("""
Git history, fork, CI log, cache, artifact에 secret이 남을 수 있다. 먼저 credential을 revoke/rotate하고 사용 흔적을 조사한 뒤 history cleanup은 노출 범위를 줄이는 후속 조치로 한다. 새 secret을 발급하기 전에 leak root cause(환경변수 출력, debug log, .env commit)를 막지 않으면 반복된다.
"""),
            depthHeading("6. dependency 취약점과 supply chain"),
            depthParagraph("""
직접 작성한 코드가 안전해도 package dependency가 악성 update나 알려진 vulnerability를 포함할 수 있다. lockfile/version pinning, dependency review, SBOM, automated advisory scan, 최소 install script 권한을 사용한다. CVE 숫자만 보고 즉시 upgrade하기보다 실제 사용 경로와 exploitability를 확인하되 critical remote code execution은 빠르게 대응한다.
"""),
            depthCode("""
SSRF 방어 흐름 예
1) scheme allowlist: https only
2) hostname syntax 검증
3) DNS resolve
4) resolved IP가 private/loopback/link-local/metadata range인지 차단
5) connect 시 검증된 IP 사용 또는 egress proxy 강제
6) redirect가 나오면 새 URL을 1~5 다시 검증
7) response size/time 제한
"""),
            depthBullets(
                "입력 문자열 blacklist 하나로 injection/traversal을 막으려 하지 않는다.",
                "secret leak 발견 시 첫 행동은 history 편집이 아니라 credential revoke/rotation이다.",
                "security control은 parser, browser, DNS, redirect 같은 실제 해석 단계를 기준으로 검증한다."
            )
        )
    )
}
