package com.futuretech.poweruser.textbook

/** Current platform, standards and specialist sources audited independently from the stable book spine. */
object V5BookSupplementalSources {
    private fun s(
        id: String,
        title: String,
        owner: String,
        type: V5BookSource.SourceType,
        version: String? = null,
        url: String
    ) = V5BookSource(id, title, owner, version, url, type)

    val sources: List<V5BookSource> = listOf(
        s("AOSP-ART", "Android runtime and Dalvik", "Android Open Source Project", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://source.android.com/docs/core/runtime"),
        s("AOSP-ART-JIT", "Implement ART just-in-time compiler", "Android Open Source Project", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://source.android.com/docs/core/runtime/jit-compiler"),
        s("AOSP-ART-CONFIG", "Configure ART", "Android Open Source Project", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://source.android.com/docs/core/runtime/configure"),
        s("AOSP-ZYGOTE", "About the Zygote processes", "Android Open Source Project", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://source.android.com/docs/core/runtime/zygote"),
        s("AOSP-BOOTLOADER", "Bootloader overview", "Android Open Source Project", V5BookSource.SourceType.OFFICIAL_DOC, "audited 2026-09", "https://source.android.com/docs/core/architecture/bootloader"),
        s("AOSP-AVB", "Android Verified Boot", "Android Open Source Project", V5BookSource.SourceType.OFFICIAL_DOC, "AVB / Verified Boot 2.0", "https://source.android.com/docs/security/features/verifiedboot/avb"),
        s("AOSP-BOOT-FLOW", "Verified Boot flow", "Android Open Source Project", V5BookSource.SourceType.OFFICIAL_DOC, "audited 2026-09", "https://source.android.com/docs/security/features/verifiedboot/boot-flow"),
        s("ANDROID-MEMORY", "Understanding and troubleshooting Android memory", "Android Developers", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://developer.android.com/topic/performance/memory/guide"),
        s("ANDROID-PROCESSES", "Processes and threads overview", "Android Developers", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://developer.android.com/guide/components/processes-and-threads"),
        s("ANDROID-COMPOSE-PERF", "Jetpack Compose performance", "Android Developers", V5BookSource.SourceType.OFFICIAL_DOC, "audited 2026-09", "https://developer.android.com/develop/ui/compose/performance"),
        s("ANDROID-COMPOSE-PHASES", "Jetpack Compose phases", "Android Developers", V5BookSource.SourceType.OFFICIAL_DOC, "audited 2026-09", "https://developer.android.com/develop/ui/compose/phases"),
        s("LINUX-MAN", "Linux man-pages project", "Linux man-pages project", V5BookSource.SourceType.OFFICIAL_DOC, "man-pages 6.19 family audited 2026-09", "https://www.kernel.org/doc/man-pages/"),
        s("LINUX-CGROUP2", "Control Group v2", "Linux kernel documentation", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://www.kernel.org/doc/html/latest/admin-guide/cgroup-v2.html"),
        s("LINUX-SCHED", "Linux scheduler documentation", "Linux kernel documentation", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://www.kernel.org/doc/html/latest/scheduler/"),
        s("LINUX-BOOTCONFIG", "Boot Configuration", "Linux kernel documentation", V5BookSource.SourceType.OFFICIAL_DOC, "audited 2026-09", "https://www.kernel.org/doc/html/latest/admin-guide/bootconfig.html"),
        s("LINUX-TIMEKEEPING", "Clock sources, clock events, sched_clock and delay timers", "Linux kernel documentation", V5BookSource.SourceType.OFFICIAL_DOC, "Linux 7.2-rc2 docs audited 2026-09", "https://www.kernel.org/doc/html/latest/timers/timekeeping.html"),
        s("LINUX-MM", "Memory Management Documentation", "Linux kernel documentation", V5BookSource.SourceType.OFFICIAL_DOC, "latest audited 2026-09", "https://www.kernel.org/doc/html/latest/mm/"),
        s("LINUX-SLAB", "Slab Allocation", "Linux kernel documentation", V5BookSource.SourceType.OFFICIAL_DOC, "latest audited 2026-09", "https://www.kernel.org/doc/html/latest/mm/slab.html"),
        s("LINUX-KVM", "KVM documentation", "Linux kernel documentation", V5BookSource.SourceType.OFFICIAL_DOC, "latest audited 2026-09", "https://docs.kernel.org/virt/kvm/index.html"),
        s("SQLITE-ATOMIC", "Atomic Commit In SQLite", "SQLite project", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://sqlite.org/atomiccommit.html"),
        s("SQLITE-ISOLATION", "Isolation In SQLite", "SQLite project", V5BookSource.SourceType.OFFICIAL_DOC, url = "https://sqlite.org/isolation.html"),
        s("RFC8259", "The JavaScript Object Notation (JSON) Data Interchange Format", "IETF / RFC Editor", V5BookSource.SourceType.STANDARD, "RFC 8259 / STD 90", "https://www.rfc-editor.org/rfc/rfc8259"),
        s("RFC8949", "Concise Binary Object Representation (CBOR)", "IETF / RFC Editor", V5BookSource.SourceType.STANDARD, "RFC 8949 / STD 94", "https://www.rfc-editor.org/rfc/rfc8949"),
        s("RFC5905", "Network Time Protocol Version 4", "IETF / RFC Editor", V5BookSource.SourceType.STANDARD, "RFC 5905 with current updates audited 2026-09", "https://www.rfc-editor.org/rfc/rfc5905"),
        s("RFC9769", "NTP Interleaved Modes", "IETF / RFC Editor", V5BookSource.SourceType.STANDARD, "RFC 9769 (updates RFC 5905)", "https://www.rfc-editor.org/rfc/rfc9769"),
        s("PROTOBUF-WIRE", "Protocol Buffers Encoding", "Google", V5BookSource.SourceType.OFFICIAL_DOC, "current official encoding guide", "https://protobuf.dev/programming-guides/encoding/"),
        s("UNICODE-UTS39", "Unicode Security Mechanisms", "Unicode Consortium", V5BookSource.SourceType.STANDARD, "UTS #39", "https://www.unicode.org/reports/tr39/"),
        s("LLVM-LANGREF", "LLVM Language Reference Manual", "LLVM Project", V5BookSource.SourceType.OFFICIAL_DOC, "current LangRef", "https://llvm.org/docs/LangRef.html"),
        s("ARM-ELF-ABI", "ELF for the Arm Architecture", "Arm ABI project", V5BookSource.SourceType.STANDARD, "current abi-aa", "https://github.com/ARM-software/abi-aa/blob/main/aaelf32/aaelf32.rst"),
        s("GC-HANDBOOK2", "The Garbage Collection Handbook: The Art of Automatic Memory Management", "Richard Jones; Antony Hosking; Eliot Moss", V5BookSource.SourceType.ENGINEERING_BOOK, "2nd edition, 2023", "https://gchandbook.org/")
    )

    val byId: Map<String, V5BookSource> = sources.associateBy(V5BookSource::id)

    init {
        require(byId.size == sources.size) { "Duplicate supplemental V5 source ids" }
        val collisions = byId.keys.intersect(V5BookSourceRegistry.byId.keys)
        require(collisions.isEmpty()) { "Supplemental V5 source ids collide with core registry: $collisions" }
    }
}

object V5BookAllSources {
    val byId: Map<String, V5BookSource> = V5BookSourceRegistry.byId + V5BookSupplementalSources.byId
}
