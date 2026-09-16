package com.futuretech.poweruser.textbook

import java.util.ArrayDeque

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
 *
 * The old paginator split a large block against a whole-page budget first and then moved that
 * already-split block to the next page whenever it did not fit. That left large empty bottoms.
 * This version always looks at the *remaining* capacity of the current page and slices paragraphs,
 * lists, code and tables to use that space before turning the page. Source code lines and original
 * list items are never rewritten or reordered.
 */
object TextbookPageComposer {
    private const val MIN_USEFUL_REMAINDER = 4

    fun paginate(blocks: List<TextbookBlock>, layout: TextbookPageLayout): List<TextbookContentPage> {
        if (blocks.isEmpty()) return listOf(TextbookContentPage(emptyList(), 0))

        val queue = ArrayDeque<TextbookBlock>()
        blocks.forEach(queue::addLast)

        val pages = mutableListOf<TextbookContentPage>()
        var current = mutableListOf<TextbookBlock>()
        var usedLines = 0

        fun flush() {
            if (current.isEmpty()) return
            pages += TextbookContentPage(current.toList(), usedLines)
            current = mutableListOf()
            usedLines = 0
        }

        fun add(block: TextbookBlock) {
            current += block
            usedLines += estimateLines(block, layout.charsPerLine).coerceAtLeast(1)
        }

        while (queue.isNotEmpty()) {
            val block = queue.removeFirst()
            var remaining = layout.maxLines - usedLines
            val blockLines = estimateLines(block, layout.charsPerLine).coerceAtLeast(1)

            // Do not leave a heading stranded at the page bottom without explanation below it.
            if (block is TextbookBlock.Heading && current.isNotEmpty() && remaining < blockLines + 2) {
                flush()
                remaining = layout.maxLines
            }

            if (blockLines <= remaining) {
                add(block)
                if (usedLines >= layout.maxLines) flush()
                continue
            }

            if (current.isNotEmpty() && remaining < MIN_USEFUL_REMAINDER) {
                flush()
                queue.addFirst(block)
                continue
            }

            val slice = sliceForCapacity(block, layout.charsPerLine, remaining)
            if (slice != null) {
                add(slice.first)
                slice.second?.let(queue::addFirst)
                // A sliced block intentionally finishes this page after consuming the available tail.
                flush()
                continue
            }

            if (current.isNotEmpty()) {
                flush()
                queue.addFirst(block)
            } else {
                // Unsplittable authored units stay intact. Exposing a rare over-budget page is safer
                // than rewriting a source-code line or turning one authored bullet into fake items.
                add(block)
                flush()
            }
        }
        flush()

        return pages.ifEmpty { listOf(TextbookContentPage(emptyList(), 0)) }
    }

    fun estimateLines(block: TextbookBlock, charsPerLine: Int): Int = when (block) {
        is TextbookBlock.Heading -> {
            val width = when (block.level) {
                1 -> (charsPerLine * 0.74f).toInt()
                2 -> (charsPerLine * 0.80f).toInt()
                else -> (charsPerLine * 0.88f).toInt()
            }.coerceAtLeast(8)
            wrappedLines(block.text, width) + 1
        }
        is TextbookBlock.Paragraph -> wrappedLines(block.text, charsPerLine) + 1
        is TextbookBlock.BulletList -> {
            val contentLines = block.items.sumOf { item ->
                wrappedLines(item, (charsPerLine - 4).coerceAtLeast(10))
            }
            contentLines + ((block.items.size + 2) / 3) + 1
        }
        is TextbookBlock.Code -> block.text.lines().sumOf { line ->
            wrappedLines(line.ifEmpty { " " }, (charsPerLine * 0.76f).toInt().coerceAtLeast(10))
        } + 3
        is TextbookBlock.Table -> {
            val columns = block.headers.size.coerceAtLeast(1)
            val cellWidth = (charsPerLine / columns).coerceAtLeast(8)
            val rows = block.rows.sumOf { row -> row.maxOfOrNull { wrappedLines(it, cellWidth) } ?: 1 }
            rows + 3
        }
        TextbookBlock.Divider -> 1
    }

    private fun sliceForCapacity(
        block: TextbookBlock,
        charsPerLine: Int,
        availableLines: Int
    ): Pair<TextbookBlock, TextbookBlock?>? {
        if (availableLines < MIN_USEFUL_REMAINDER) return null
        return when (block) {
            is TextbookBlock.Paragraph -> sliceParagraph(block, charsPerLine, availableLines)
            is TextbookBlock.BulletList -> sliceBulletList(block, charsPerLine, availableLines)
            is TextbookBlock.Code -> sliceCode(block, charsPerLine, availableLines)
            is TextbookBlock.Table -> sliceTable(block, charsPerLine, availableLines)
            is TextbookBlock.Heading, TextbookBlock.Divider -> null
        }
    }

    private fun sliceParagraph(
        block: TextbookBlock.Paragraph,
        charsPerLine: Int,
        availableLines: Int
    ): Pair<TextbookBlock, TextbookBlock?>? {
        val textLines = (availableLines - 1).coerceAtLeast(1)
        val maxChars = (charsPerLine * textLines).coerceAtLeast(charsPerLine)
        val (head, tail) = splitTextOnce(block.text, maxChars) ?: return null
        val first = TextbookBlock.Paragraph(head)
        val rest = tail?.takeIf { it.isNotBlank() }?.let(TextbookBlock::Paragraph)
        return first to rest
    }

    private fun sliceBulletList(
        block: TextbookBlock.BulletList,
        charsPerLine: Int,
        availableLines: Int
    ): Pair<TextbookBlock, TextbookBlock?>? {
        val itemWidth = (charsPerLine - 4).coerceAtLeast(10)
        val head = mutableListOf<String>()
        var estimated = 1

        for (item in block.items) {
            val nextItems = head.size + 1
            val nextEstimate = estimated + wrappedLines(item, itemWidth) + if (nextItems % 3 == 0) 1 else 0
            if (nextEstimate > availableLines) break
            head += item
            estimated = nextEstimate
        }
        if (head.isEmpty() || head.size == block.items.size) return null
        return TextbookBlock.BulletList(head, block.ordered) to
            TextbookBlock.BulletList(block.items.drop(head.size), block.ordered)
    }

    private fun sliceCode(
        block: TextbookBlock.Code,
        charsPerLine: Int,
        availableLines: Int
    ): Pair<TextbookBlock, TextbookBlock?>? {
        val source = block.text.lines()
        if (source.size <= 1) return null
        val width = (charsPerLine * 0.76f).toInt().coerceAtLeast(10)
        val maxContentLines = (availableLines - 3).coerceAtLeast(1)
        val head = mutableListOf<String>()
        var used = 0

        for (line in source) {
            val need = wrappedLines(line.ifEmpty { " " }, width)
            if (used + need > maxContentLines) break
            head += line
            used += need
        }
        if (head.isEmpty() || head.size == source.size) return null
        return TextbookBlock.Code(block.language, head.joinToString("\n")) to
            TextbookBlock.Code(block.language, source.drop(head.size).joinToString("\n"))
    }

    private fun sliceTable(
        block: TextbookBlock.Table,
        charsPerLine: Int,
        availableLines: Int
    ): Pair<TextbookBlock, TextbookBlock?>? {
        if (block.rows.size <= 1) return null
        val columns = block.headers.size.coerceAtLeast(1)
        val width = (charsPerLine / columns).coerceAtLeast(8)
        val maxRowLines = (availableLines - 3).coerceAtLeast(1)
        val rows = mutableListOf<List<String>>()
        var used = 0

        for (row in block.rows) {
            val need = row.maxOfOrNull { wrappedLines(it, width) } ?: 1
            if (used + need > maxRowLines) break
            rows += row
            used += need
        }
        if (rows.isEmpty() || rows.size == block.rows.size) return null
        return TextbookBlock.Table(block.headers, rows) to
            TextbookBlock.Table(block.headers, block.rows.drop(rows.size))
    }

    private fun splitTextOnce(text: String, maxChars: Int): Pair<String, String?>? {
        val normalized = text.trim().replace(Regex("\\s+"), " ")
        if (normalized.length <= maxChars) return null

        var split = normalized.lastIndexOf(' ', startIndex = maxChars.coerceAtMost(normalized.lastIndex))
        if (split <= 0) split = normalized.indexOf(' ', startIndex = maxChars.coerceAtMost(normalized.lastIndex))
        if (split <= 0 || split >= normalized.lastIndex) return null

        val head = normalized.substring(0, split).trim()
        val tail = normalized.substring(split + 1).trim()
        if (head.isBlank() || tail.isBlank()) return null
        return head to tail
    }

    private fun wrappedLines(text: String, charsPerLine: Int): Int {
        if (text.isEmpty()) return 1
        return text.lines().sumOf { line ->
            val length = line.length.coerceAtLeast(1)
            ((length + charsPerLine - 1) / charsPerLine).coerceAtLeast(1)
        }
    }
}
