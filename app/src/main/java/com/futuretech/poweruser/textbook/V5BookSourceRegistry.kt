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
 * Stable bibliography/source IDs for the book-scale rewrite.
 *
 * The authored text must synthesize and explain; these references are evidence anchors, not a license
 * to copy source prose. Track mappings are intentionally plural so a major mechanism is not learned
 * from one author's wording alone when an independent primary/reference source exists.
 */
object V5BookSourceRegistry {
    val sources: List<V5BookSource> = listOf(
        V5BookSource(
            "CSAPP3",
            "Computer Systems: A Programmer's Perspective",
            "Randal E. Bryant; David R. O'Hallaron",
            "3rd edition",
            "https://www.pearson.com/en-us/subject-catalog/p/computer-systems-a-programmer-s-perspective/P200000003479/9780134092669",
            V5BookSource.SourceType.TEXTBOOK
        ),
        V5BookSource(
            "OSTEP",
            "Operating Systems: Three Easy Pieces",
            "Remzi H. Arpaci-Dusseau; Andrea C. Arpaci-Dusseau",
            "version 1.10",
            "https://pages.cs.wisc.edu/~remzi/OSTEP/",
            V5BookSource.SourceType.TEXTBOOK
        ),
        V5BookSource(
            "TLPI",
            "The Linux Programming Interface",
            "Michael Kerrisk",
            "1st edition",
            "https://www.man7.org/tlpi/",
            V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "UNICODE",
            "The Unicode Standard",
            "The Unicode Consortium",
            canonicalUrl = "https://www.unicode.org/standard/standard.html",
            sourceType = V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "ANDROID-FUNDAMENTALS",
            "Application fundamentals",
            "Android Developers",
            canonicalUrl = "https://developer.android.com/guide/components/fundamentals",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "SICP",
            "Structure and Interpretation of Computer Programs",
            "Harold Abelson; Gerald Jay Sussman; Julie Sussman",
            "2nd edition",
            "https://mitpress.mit.edu/9780262510875/structure-and-interpretation-of-computer-programs/",
            V5BookSource.SourceType.TEXTBOOK
        ),
        V5BookSource(
            "PYTHON-REF",
            "The Python Language Reference",
            "Python Software Foundation",
            canonicalUrl = "https://docs.python.org/3/reference/",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "CLRS4",
            "Introduction to Algorithms",
            "Thomas H. Cormen; Charles E. Leiserson; Ronald L. Rivest; Clifford Stein",
            "4th edition",
            "https://mitpress.mit.edu/9780262046305/introduction-to-algorithms/",
            V5BookSource.SourceType.TEXTBOOK
        ),
        V5BookSource(
            "ODS",
            "Open Data Structures",
            "Pat Morin",
            canonicalUrl = "https://opendatastructures.org/",
            sourceType = V5BookSource.SourceType.TEXTBOOK
        ),
        V5BookSource(
            "WHATWG-HTML",
            "HTML Living Standard",
            "WHATWG",
            canonicalUrl = "https://html.spec.whatwg.org/",
            sourceType = V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "CSS-CASCADE",
            "CSS Cascading and Inheritance",
            "W3C CSS Working Group",
            canonicalUrl = "https://www.w3.org/TR/css-cascade-6/",
            sourceType = V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "WCAG22",
            "Web Content Accessibility Guidelines (WCAG) 2.2",
            "W3C",
            "2.2",
            "https://www.w3.org/TR/WCAG22/",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "MDN-WEB",
            "MDN Web Docs",
            "Mozilla contributors",
            canonicalUrl = "https://developer.mozilla.org/",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "ECMA262",
            "ECMAScript Language Specification",
            "Ecma International / TC39",
            canonicalUrl = "https://tc39.es/ecma262/",
            sourceType = V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "TS-HANDBOOK",
            "TypeScript Handbook",
            "Microsoft",
            canonicalUrl = "https://www.typescriptlang.org/docs/handbook/intro.html",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "RFC1034",
            "Domain Names - Concepts and Facilities",
            "IETF",
            "RFC 1034",
            "https://www.rfc-editor.org/rfc/rfc1034",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "RFC1035",
            "Domain Names - Implementation and Specification",
            "IETF",
            "RFC 1035",
            "https://www.rfc-editor.org/rfc/rfc1035",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "RFC9110",
            "HTTP Semantics",
            "IETF",
            "RFC 9110",
            "https://www.rfc-editor.org/rfc/rfc9110",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "RFC8446",
            "The Transport Layer Security (TLS) Protocol Version 1.3",
            "IETF",
            "RFC 8446",
            "https://www.rfc-editor.org/rfc/rfc8446",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "RFC9000",
            "QUIC: A UDP-Based Multiplexed and Secure Transport",
            "IETF",
            "RFC 9000",
            "https://www.rfc-editor.org/rfc/rfc9000",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "OPENAPI31",
            "OpenAPI Specification",
            "OpenAPI Initiative",
            "3.1",
            "https://spec.openapis.org/oas/v3.1.0",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "SRE",
            "Site Reliability Engineering",
            "Google",
            canonicalUrl = "https://sre.google/sre-book/table-of-contents/",
            sourceType = V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "SRE-WORKBOOK",
            "The Site Reliability Workbook",
            "Google",
            canonicalUrl = "https://sre.google/workbook/table-of-contents/",
            sourceType = V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "DDIA",
            "Designing Data-Intensive Applications",
            "Martin Kleppmann",
            "1st edition",
            "https://www.oreilly.com/library/view/designing-data-intensive-applications/9781491903063/",
            V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "POSTGRES",
            "PostgreSQL Documentation",
            "PostgreSQL Global Development Group",
            canonicalUrl = "https://www.postgresql.org/docs/current/",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "DBSC7",
            "Database System Concepts",
            "Abraham Silberschatz; Henry F. Korth; S. Sudarshan",
            "7th edition",
            "https://www.db-book.com/",
            V5BookSource.SourceType.TEXTBOOK
        ),
        V5BookSource(
            "OWASP-ASVS",
            "Application Security Verification Standard",
            "OWASP Foundation",
            canonicalUrl = "https://owasp.org/www-project-application-security-verification-standard/",
            sourceType = V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "OWASP-API",
            "OWASP API Security Top 10",
            "OWASP Foundation",
            canonicalUrl = "https://owasp.org/API-Security/",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "NIST-SSDF",
            "Secure Software Development Framework (SSDF)",
            "NIST",
            "SP 800-218",
            "https://csrc.nist.gov/pubs/sp/800/218/final",
            V5BookSource.SourceType.STANDARD
        ),
        V5BookSource(
            "ANDROID-SECURITY",
            "Security best practices",
            "Android Developers",
            canonicalUrl = "https://developer.android.com/privacy-and-security/security-best-practices",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "PROGIT2",
            "Pro Git",
            "Scott Chacon; Ben Straub",
            "2nd edition",
            "https://git-scm.com/book/en/v2",
            V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "GIT-DOCS",
            "Git Reference Documentation",
            "Git project",
            canonicalUrl = "https://git-scm.com/docs",
            sourceType = V5BookSource.SourceType.OFFICIAL_DOC
        ),
        V5BookSource(
            "REFACTORING2",
            "Refactoring: Improving the Design of Existing Code",
            "Martin Fowler",
            "2nd edition",
            "https://martinfowler.com/books/refactoring.html",
            V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "DDD",
            "Domain-Driven Design: Tackling Complexity in the Heart of Software",
            "Eric Evans",
            "1st edition",
            "https://www.domainlanguage.com/ddd/",
            V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "GOF",
            "Design Patterns: Elements of Reusable Object-Oriented Software",
            "Erich Gamma; Richard Helm; Ralph Johnson; John Vlissides",
            "1st edition",
            sourceType = V5BookSource.SourceType.ENGINEERING_BOOK
        ),
        V5BookSource(
            "LEARNING-SPACING-RETRIEVAL",
            "The science of effective learning with spacing and retrieval practice",
            "Shana K. Carpenter; Steven C. Pan; Andrew C. Butler",
            "Nature Reviews Psychology (2022)",
            "https://doi.org/10.1038/s44159-022-00089-1",
            V5BookSource.SourceType.RESEARCH
        ),
        V5BookSource(
            "LEARNING-CLT-COMPUTING",
            "Cognitive Load Theory in Computing Education Research: A Review",
            "ACM Transactions on Computing Education",
            "2022",
            "https://doi.org/10.1145/3483843",
            V5BookSource.SourceType.RESEARCH
        )
    )

    val byId: Map<String, V5BookSource> = sources.associateBy(V5BookSource::id)

    val trackSourceSpines: Map<Int, Set<String>> = mapOf(
        1 to setOf("CSAPP3", "OSTEP", "TLPI", "UNICODE", "ANDROID-FUNDAMENTALS"),
        2 to setOf("SICP", "PYTHON-REF", "CSAPP3", "LEARNING-CLT-COMPUTING"),
        3 to setOf("CLRS4", "ODS", "CSAPP3", "LEARNING-CLT-COMPUTING"),
        4 to setOf("WHATWG-HTML", "CSS-CASCADE", "WCAG22", "MDN-WEB"),
        5 to setOf("ECMA262", "TS-HANDBOOK", "MDN-WEB"),
        6 to setOf("RFC1034", "RFC1035", "RFC9110", "RFC8446", "RFC9000", "OPENAPI31"),
        7 to setOf("SRE", "SRE-WORKBOOK", "DDIA", "OPENAPI31", "OWASP-API"),
        8 to setOf("POSTGRES", "DBSC7", "DDIA"),
        9 to setOf("OWASP-ASVS", "OWASP-API", "NIST-SSDF", "RFC8446", "ANDROID-SECURITY"),
        10 to setOf("PROGIT2", "GIT-DOCS", "SRE", "SRE-WORKBOOK", "NIST-SSDF"),
        11 to setOf("DDIA", "REFACTORING2", "DDD", "GOF", "SRE")
    )

    init {
        require(byId.size == sources.size) { "Duplicate V5 source ids" }
        require(trackSourceSpines.keys == (1..11).toSet()) { "Every track must own a source spine" }
        val missing = trackSourceSpines.values.flatten().filterNot(byId::containsKey).toSet()
        require(missing.isEmpty()) { "Unknown source ids in track source spines: $missing" }
    }
}
