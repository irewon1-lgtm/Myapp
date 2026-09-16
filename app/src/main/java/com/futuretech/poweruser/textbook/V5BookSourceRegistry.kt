package com.futuretech.poweruser.textbook

data class V5BookSource(
    val id: String,
    val title: String,
    val authorOrOwner: String,
    val editionOrVersion: String? = null,
    val canonicalUrl: String? = null,
    val sourceType: SourceType
) {
    enum class SourceType { STANDARD, OFFICIAL_DOC, TEXTBOOK, ENGINEERING_BOOK, RESEARCH }
}

/**
 * Evidence anchors for the book-scale rewrite. Learner prose must synthesize and explain rather
 * than reproduce source wording. Versions are pinned when a stable edition matters; living/current
 * documentation uses its canonical current URL and is re-audited before release.
 */
object V5BookSourceRegistry {
    private fun s(
        id: String,
        title: String,
        owner: String,
        type: V5BookSource.SourceType,
        version: String? = null,
        url: String? = null
    ) = V5BookSource(id, title, owner, version, url, type)

    val sources: List<V5BookSource> = listOf(
        s(
            "CSAPP3",
            "Computer Systems: A Programmer's Perspective",
            "Randal E. Bryant; David R. O'Hallaron",
            V5BookSource.SourceType.TEXTBOOK,
            "3rd edition",
            "https://www.pearson.com/en-us/subject-catalog/p/computer-systems-a-programmer-s-perspective/P200000003479/9780134092669"
        ),
        s(
            "OSTEP",
            "Operating Systems: Three Easy Pieces",
            "Remzi H. Arpaci-Dusseau; Andrea C. Arpaci-Dusseau",
            V5BookSource.SourceType.TEXTBOOK,
            "version 1.10",
            "https://pages.cs.wisc.edu/~remzi/OSTEP/"
        ),
        s(
            "TLPI",
            "The Linux Programming Interface",
            "Michael Kerrisk",
            V5BookSource.SourceType.ENGINEERING_BOOK,
            "1st edition",
            "https://www.man7.org/tlpi/"
        ),
        s(
            "UNICODE",
            "The Unicode Standard",
            "The Unicode Consortium",
            V5BookSource.SourceType.STANDARD,
            url = "https://www.unicode.org/standard/standard.html"
        ),
        s(
            "ANDROID-FUNDAMENTALS",
            "Application fundamentals",
            "Android Developers",
            V5BookSource.SourceType.OFFICIAL_DOC,
            url = "https://developer.android.com/guide/components/fundamentals"
        ),
        s(
            "SICP",
            "Structure and Interpretation of Computer Programs",
            "Harold Abelson; Gerald Jay Sussman; Julie Sussman",
            V5BookSource.SourceType.TEXTBOOK,
            "2nd edition",
            "https://mitpress.mit.edu/9780262510875/structure-and-interpretation-of-computer-programs/"
        ),
        s(
            "PYTHON-REF",
            "The Python Language Reference",
            "Python Software Foundation",
            V5BookSource.SourceType.OFFICIAL_DOC,
            url = "https://docs.python.org/3/reference/"
        ),
        s(
            "CLRS4",
            "Introduction to Algorithms",
            "Thomas H. Cormen; Charles E. Leiserson; Ronald L. Rivest; Clifford Stein",
            V5BookSource.SourceType.TEXTBOOK,
            "4th edition",
            "https://mitpress.mit.edu/9780262046305/introduction-to-algorithms/"
        ),
        s(
            "ODS",
            "Open Data Structures",
            "Pat Morin",
            V5BookSource.SourceType.TEXTBOOK,
            url = "https://opendatastructures.org/"
        ),
        s(
            "WHATWG-HTML",
            "HTML Living Standard",
            "WHATWG",
            V5BookSource.SourceType.STANDARD,
            "Living Standard",
            "https://html.spec.whatwg.org/"
        ),
        s(
            "CSS-CASCADE",
            "CSS Cascading and Inheritance Level 5",
            "W3C CSS Working Group",
            V5BookSource.SourceType.STANDARD,
            "Candidate Recommendation Snapshot",
            "https://www.w3.org/TR/css-cascade-5/"
        ),
        s(
            "WCAG22",
            "Web Content Accessibility Guidelines (WCAG) 2.2",
            "W3C",
            V5BookSource.SourceType.STANDARD,
            "W3C Recommendation 2.2",
            "https://www.w3.org/TR/WCAG22/"
        ),
        s(
            "MDN-WEB",
            "MDN Web Docs",
            "Mozilla contributors",
            V5BookSource.SourceType.OFFICIAL_DOC,
            url = "https://developer.mozilla.org/"
        ),
        s(
            "ECMA262",
            "ECMAScript Language Specification",
            "Ecma International / TC39",
            V5BookSource.SourceType.STANDARD,
            "living specification",
            "https://tc39.es/ecma262/"
        ),
        s(
            "TS-HANDBOOK",
            "TypeScript Handbook",
            "Microsoft",
            V5BookSource.SourceType.OFFICIAL_DOC,
            url = "https://www.typescriptlang.org/docs/handbook/intro.html"
        ),
        s("RFC1034", "Domain Names - Concepts and Facilities", "IETF", V5BookSource.SourceType.STANDARD, "RFC 1034", "https://www.rfc-editor.org/rfc/rfc1034"),
        s("RFC1035", "Domain Names - Implementation and Specification", "IETF", V5BookSource.SourceType.STANDARD, "RFC 1035", "https://www.rfc-editor.org/rfc/rfc1035"),
        s("RFC9110", "HTTP Semantics", "IETF", V5BookSource.SourceType.STANDARD, "RFC 9110 / STD 97", "https://www.rfc-editor.org/rfc/rfc9110"),
        s(
            "RFC9846",
            "The Transport Layer Security (TLS) Protocol Version 1.3",
            "IETF",
            V5BookSource.SourceType.STANDARD,
            "RFC 9846 (obsoletes RFC 8446; July 2026)",
            "https://www.rfc-editor.org/rfc/rfc9846"
        ),
        s("RFC9000", "QUIC: A UDP-Based Multiplexed and Secure Transport", "IETF", V5BookSource.SourceType.STANDARD, "RFC 9000", "https://www.rfc-editor.org/rfc/rfc9000"),
        s(
            "OPENAPI31",
            "OpenAPI Specification",
            "OpenAPI Initiative",
            V5BookSource.SourceType.STANDARD,
            "3.1.2",
            "https://spec.openapis.org/oas/v3.1.2"
        ),
        s("SRE", "Site Reliability Engineering", "Google", V5BookSource.SourceType.ENGINEERING_BOOK, url = "https://sre.google/sre-book/table-of-contents/"),
        s("SRE-WORKBOOK", "The Site Reliability Workbook", "Google", V5BookSource.SourceType.ENGINEERING_BOOK, url = "https://sre.google/workbook/table-of-contents/"),
        s(
            "DDIA",
            "Designing Data-Intensive Applications",
            "Martin Kleppmann; Chris Riccomini",
            V5BookSource.SourceType.ENGINEERING_BOOK,
            "2nd edition, February 2026",
            "https://www.oreilly.com/library/view/designing-data-intensive-applications/9781098119058/"
        ),
        s(
            "POSTGRES",
            "PostgreSQL Documentation",
            "PostgreSQL Global Development Group",
            V5BookSource.SourceType.OFFICIAL_DOC,
            "current stable documentation",
            "https://www.postgresql.org/docs/current/"
        ),
        s(
            "DBSC7",
            "Database System Concepts",
            "Abraham Silberschatz; Henry F. Korth; S. Sudarshan",
            V5BookSource.SourceType.TEXTBOOK,
            "7th edition",
            "https://www.db-book.com/"
        ),
        s(
            "OWASP-ASVS",
            "Application Security Verification Standard",
            "OWASP Foundation",
            V5BookSource.SourceType.STANDARD,
            "5.0.0",
            "https://owasp.org/projects/asvs"
        ),
        s(
            "OWASP-API",
            "OWASP API Security Top 10",
            "OWASP Foundation",
            V5BookSource.SourceType.OFFICIAL_DOC,
            "2023 edition",
            "https://api-security.owasp.org/"
        ),
        s(
            "NIST-SSDF",
            "Secure Software Development Framework (SSDF)",
            "NIST",
            V5BookSource.SourceType.STANDARD,
            "SP 800-218 v1.1 (final)",
            "https://csrc.nist.gov/pubs/sp/800/218/final"
        ),
        s(
            "ANDROID-SECURITY",
            "Security best practices",
            "Android Developers",
            V5BookSource.SourceType.OFFICIAL_DOC,
            url = "https://developer.android.com/privacy-and-security/security-best-practices"
        ),
        s(
            "PROGIT2",
            "Pro Git",
            "Scott Chacon; Ben Straub",
            V5BookSource.SourceType.ENGINEERING_BOOK,
            "2nd edition",
            "https://git-scm.com/book/en/v2"
        ),
        s("GIT-DOCS", "Git Reference Documentation", "Git project", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://git-scm.com/docs"),
        s(
            "REFACTORING2",
            "Refactoring: Improving the Design of Existing Code",
            "Martin Fowler",
            V5BookSource.SourceType.ENGINEERING_BOOK,
            "2nd edition",
            "https://martinfowler.com/books/refactoring.html"
        ),
        s(
            "DDD",
            "Domain-Driven Design: Tackling Complexity in the Heart of Software",
            "Eric Evans",
            V5BookSource.SourceType.ENGINEERING_BOOK,
            "1st edition",
            "https://www.domainlanguage.com/ddd/"
        ),
        s(
            "GOF",
            "Design Patterns: Elements of Reusable Object-Oriented Software",
            "Erich Gamma; Richard Helm; Ralph Johnson; John Vlissides",
            V5BookSource.SourceType.ENGINEERING_BOOK,
            "1st edition"
        ),
        s(
            "LEARNING-SPACING-RETRIEVAL",
            "The science of effective learning with spacing and retrieval practice",
            "Shana K. Carpenter; Steven C. Pan; Andrew C. Butler",
            V5BookSource.SourceType.RESEARCH,
            "Nature Reviews Psychology (2022)",
            "https://doi.org/10.1038/s44159-022-00089-1"
        ),
        s(
            "LEARNING-CLT-COMPUTING",
            "Cognitive Load Theory in Computing Education Research: A Review",
            "ACM Transactions on Computing Education",
            V5BookSource.SourceType.RESEARCH,
            "2022",
            "https://doi.org/10.1145/3483843"
        )
    )

    val byId: Map<String, V5BookSource> = sources.associateBy(V5BookSource::id)

    val trackSourceSpines: Map<Int, Set<String>> = mapOf(
        1 to setOf("CSAPP3", "OSTEP", "TLPI", "UNICODE", "ANDROID-FUNDAMENTALS"),
        2 to setOf("SICP", "PYTHON-REF", "CSAPP3", "LEARNING-CLT-COMPUTING"),
        3 to setOf("CLRS4", "ODS", "CSAPP3", "LEARNING-CLT-COMPUTING"),
        4 to setOf("WHATWG-HTML", "CSS-CASCADE", "WCAG22", "MDN-WEB"),
        5 to setOf("ECMA262", "TS-HANDBOOK", "MDN-WEB"),
        6 to setOf("RFC1034", "RFC1035", "RFC9110", "RFC9846", "RFC9000", "OPENAPI31", "SRE"),
        7 to setOf("SRE", "SRE-WORKBOOK", "DDIA", "OPENAPI31", "OWASP-API"),
        8 to setOf("POSTGRES", "DBSC7", "DDIA"),
        9 to setOf("OWASP-ASVS", "OWASP-API", "NIST-SSDF", "RFC9846", "ANDROID-SECURITY"),
        10 to setOf("PROGIT2", "GIT-DOCS", "SRE", "SRE-WORKBOOK", "NIST-SSDF"),
        11 to setOf("DDIA", "REFACTORING2", "DDD", "GOF", "SRE", "OPENAPI31")
    )

    init {
        require(byId.size == sources.size) { "Duplicate V5 source ids" }
        require(trackSourceSpines.keys == (1..11).toSet()) { "Every track must own a source spine" }
        val missing = trackSourceSpines.values.flatten().filterNot(byId::containsKey).toSet()
        require(missing.isEmpty()) { "Unknown source ids in track source spines: $missing" }
        require("RFC8446" !in byId) { "Obsolete RFC 8446 must not be used after RFC 9846" }
    }
}
