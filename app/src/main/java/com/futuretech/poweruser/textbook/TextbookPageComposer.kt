package com.futuretech.poweruser.textbook

data class TextbookPageLayout(
    val charsPerLine: Int,
    val maxLines: Int
) {
    init {
        require(charsPerLine >= 12) { "charsPerLine must be >= 12" }
        require(maxLines >= 6) { "maxLines must be >= 6" }
    }
}

data class TextbookContentPage(
    val blocks: List<TextbookBlock>,
    val estimatedLines: Int
)

/**
 * Deterministically converts authored textbook blocks into reader pages.
 * Pagination may group/split containers, but it must never rewrite code or create fake list items.
 */
object TextbookPageComposer {
    fun paginate(blocks: List<TextbookBlock>, layout: TextbookPageLayout): List<TextbookContentPage> {
        if (blocks.isEmpty()) return listOf(TextbookContentPage(emptyList(), 0))

        val pieces = blocks.flatMap { splitToFit(it, layout) }
        val pages = mutableListOf<TextbookContentPage>()
        var current = mutableListOf<TextbookBlock>()
        var usedLines = 0

        fun flush() {
            if (current.isEmpty()) return
            pages += TextbookContentPage(current.toList(), usedLines.coerceAtMost(layout.maxLines))
            current = mutableListOf()
            usedLines = 0
        }

        pieces.forEach { piece ->
            val lines = estimateLines(piece, layout.charsPerLine).coerceAtLeast(1)
            if (current.isNotEmpty() && usedLines + lines > layout.maxLines) flush()
            current += piece
            usedLines += lines
            if (usedLines >= layout.maxLines) flush()
        }
        flush()

        return pages.ifEmpty { listOf(TextbookContentPage(emptyList(), 0)) }
    }

    fun estimateLines(block: TextbookBlock, charsPerLine: Int): Int = when (block) {
        is TextbookBlock.Heading -> {
            val width = when (block.level) {
                1 -> (charsPerLine * 0.72f).toInt()
                2 -> (charsPerLine * 0.78f).toInt()
                else -> (charsPerLine * 0.84f).toInt()
            }.coerceAtLeast(8)
            wrappedLines(block.text, width) + if (block.level <= 2) 2 else 1
        }
        is TextbookBlock.Paragraph -> wrappedLines(block.text, charsPerLine) + 1
        is TextbookBlock.BulletList -> block.items.sumOf { item ->
            wrappedLines(item, (charsPerLine - 3).coerceAtLeast(10)) + 1
        } + 1
        is TextbookBlock.Code -> block.text.lines().sumOf { line ->
            wrappedLines(line.ifEmpty { " " }, (charsPerLine * 0.72f).toInt().coerceAtLeast(10))
        } + 2
        is TextbookBlock.Table -> {
            val cellWidth = (charsPerLine - 4).coerceAtLeast(10)
            block.rows.sumOf { row ->
                row.sumOf { value -> wrappedLines(value, cellWidth) } + block.headers.size + 1
            } + 1
        }
        TextbookBlock.Divider -> 1
    }

    private fun splitToFit(block: TextbookBlock, layout: TextbookPageLayout): List<TextbookBlock> {
        val maxBlockLines = (layout.maxLines - 2).coerceAtLeast(4)
        if (estimateLines(block, layout.charsPerLine) <= maxBlockLines) return listOf(block)
        return when (block) {
            is TextbookBlock.Paragraph -> splitParagraph(block, layout, maxBlockLines)
            is TextbookBlock.BulletList -> splitBulletList(block, layout, maxBlockLines)
            is TextbookBlock.Code -> splitCode(block, layout, maxBlockLines)
            is TextbookBlock.Table -> splitTable(block, layout, maxBlockLines)
            is TextbookBlock.Heading, TextbookBlock.Divider -> listOf(block)
        }
    }

    private fun splitParagraph(
        block: TextbookBlock.Paragraph,
        layout: TextbookPageLayout,
        maxBlockLines: Int
    ): List<TextbookBlock> {
        val maxChars = (layout.charsPerLine * (maxBlockLines - 1)).coerceAtLeast(layout.charsPerLine)
        return splitText(block.text, maxChars).map { TextbookBlock.Paragraph(it) }
    }

    private fun splitBulletList(
        block: TextbookBlock.BulletList,
        layout: TextbookPageLayout,
        maxBlockLines: Int
    ): List<TextbookBlock> {
        val out = mutableListOf<TextbookBlock>()
        var current = mutableListOf<String>()
        var used = 1
        val itemWidth = (layout.charsPerLine - 3).coerceAtLeast(10)

        fun flush() {
            if (current.isNotEmpty()) {
                out += TextbookBlock.BulletList(current.toList(), block.ordered)
                current = mutableListOf()
                used = 1
            }
        }

        block.items.forEach { item ->
            val lines = wrappedLines(item, itemWidth) + 1
            if (current.isNotEmpty() && used + lines > maxBlockLines) flush()
            current += item
            used += lines
            if (used >= maxBlockLines) flush()
        }
        flush()
        return out.ifEmpty { listOf(block) }
    }

    private fun splitCode(
        block: TextbookBlock.Code,
        layout: TextbookPageLayout,
        maxBlockLines: Int
    ): List<TextbookBlock> {
        val codeWidth = (layout.charsPerLine * 0.72f).toInt().coerceAtLeast(10)
        val maxContentLines = (maxBlockLines - 2).coerceAtLeast(2)
        val out = mutableListOf<TextbookBlock>()
        var current = mutableListOf<String>()
        var used = 0

        fun flush() {
            if (current.isNotEmpty()) {
                out += TextbookBlock.Code(block.language, current.joinToString("\n"))
                current = mutableListOf()
                used = 0
            }
        }

        block.text.lines().forEach { sourceLine ->
            val lines = wrappedLines(sourceLine.ifEmpty { " " }, codeWidth)
            if (current.isNotEmpty() && used + lines > maxContentLines) flush()
            current += sourceLine
            used += lines
            if (used >= maxContentLines) flush()
        }
        flush()
        return out.ifEmpty { listOf(block) }
    }

    private fun splitTable(
        block: TextbookBlock.Table,
        layout: TextbookPageLayout,
        maxBlockLines: Int
    ): List<TextbookBlock> {
        if (block.rows.isEmpty()) return listOf(block)
        val out = mutableListOf<TextbookBlock>()
        var rows = mutableListOf<List<String>>()
        var used = 1
        val cellWidth = (layout.charsPerLine - 4).coerceAtLeast(10)

        fun rowLines(row: List<String>): Int =
            row.sumOf { value -> wrappedLines(value, cellWidth) } + block.headers.size + 1

        fun flush() {
            if (rows.isNotEmpty()) {
                out += TextbookBlock.Table(block.headers, rows.toList())
                rows = mutableListOf()
                used = 1
            }
        }

        block.rows.forEach { row ->
            val lines = rowLines(row)
            if (rows.isNotEmpty() && used + lines > maxBlockLines) flush()
            rows += row
            used += lines
            if (used >= maxBlockLines) flush()
        }
        flush()
        return out.ifEmpty { listOf(block) }
    }

    private fun splitText(text: String, maxChars: Int): List<String> {
        val normalized = text.trim()
        if (normalized.length <= maxChars) return listOf(normalized)
        val words = normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                out += current.toString().trim()
                current.clear()
            }
        }

        words.forEach { word ->
            if (word.length > maxChars) {
                flush()
                out += word.chunked(maxChars)
            } else if (current.isEmpty()) {
                current.append(word)
            } else if (current.length + 1 + word.length <= maxChars) {
                current.append(' ').append(word)
            } else {
                flush()
                current.append(word)
            }
        }
        flush()
        return out.ifEmpty { listOf(normalized) }
    }

    private fun wrappedLines(text: String, charsPerLine: Int): Int {
        if (text.isEmpty()) return 1
        return text.lines().sumOf { line ->
            val length = line.length.coerceAtLeast(1)
            ((length + charsPerLine - 1) / charsPerLine).coerceAtLeast(1)
        }
    }
}
