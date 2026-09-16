package com.futuretech.poweruser.textbook

import kotlin.math.ceil

/**
 * Android-free pagination engine used by the ebook reader.
 *
 * The budget is deliberately conservative: the UI keeps extra headroom for real font metrics,
 * device font scaling, and spacing that a simple character estimator cannot know exactly.
 */
data class TextbookPageMetrics(
    val widthDp: Int,
    val heightDp: Int
) {
    val charsPerLine: Int = ((widthDp - 24) / 15).coerceIn(18, 58)
    val lineBudget: Int = (heightDp / 28).coerceIn(12, 30)
    val fragmentBudget: Int = (lineBudget - 8).coerceIn(4, 22)
}

data class TextbookPage(
    val blocks: List<TextbookBlock>,
    val estimatedLines: Int
)

object TextbookPaginator {
    fun paginate(blocks: List<TextbookBlock>, metrics: TextbookPageMetrics): List<TextbookPage> {
        if (blocks.isEmpty()) return listOf(TextbookPage(emptyList(), 0))
        val fragments = blocks.flatMap { splitBlock(it, metrics) }
        if (fragments.isEmpty()) return listOf(TextbookPage(emptyList(), 0))

        val pages = mutableListOf<TextbookPage>()
        val current = mutableListOf<TextbookBlock>()
        var used = 0

        fun flush() {
            if (current.isNotEmpty()) {
                pages += TextbookPage(current.toList(), used)
                current.clear()
                used = 0
            }
        }

        var i = 0
        while (i < fragments.size) {
            if (fragments[i] is TextbookBlock.Heading) {
                val group = mutableListOf<TextbookBlock>()
                var j = i
                while (j < fragments.size && fragments[j] is TextbookBlock.Heading) {
                    group += fragments[j]
                    j++
                }
                if (j < fragments.size) group += fragments[j]
                val groupCost = group.sumOf { estimateLines(it, metrics.charsPerLine) }
                if (groupCost <= metrics.lineBudget) {
                    if (current.isNotEmpty() && used + groupCost > metrics.lineBudget) flush()
                    current += group
                    used += groupCost
                    if (used >= metrics.lineBudget) flush()
                    i = j + 1
                    continue
                }
            }

            val block = fragments[i]
            val cost = estimateLines(block, metrics.charsPerLine)
            if (current.isNotEmpty() && used + cost > metrics.lineBudget) flush()
            current += block
            used += cost
            if (used >= metrics.lineBudget) flush()
            i++
        }
        flush()
        return pages.ifEmpty { listOf(TextbookPage(emptyList(), 0)) }
    }

    fun estimateLines(block: TextbookBlock, charsPerLine: Int): Int = when (block) {
        is TextbookBlock.Heading -> 2 + wrappedLines(block.text, charsPerLine)
        is TextbookBlock.Paragraph -> 1 + wrappedLines(block.text, charsPerLine)
        is TextbookBlock.BulletList -> 1 + block.items.sumOf {
            1 + wrappedLines(it, (charsPerLine - 3).coerceAtLeast(12))
        }
        is TextbookBlock.Code -> 2 + block.text.lines().sumOf {
            wrappedLines(it.ifBlank { " " }, (charsPerLine - 2).coerceAtLeast(12))
        }
        is TextbookBlock.Table -> 2 + block.rows.sumOf { row ->
            1 + row.sumOf { cell -> wrappedLines(cell, (charsPerLine / 2).coerceAtLeast(10)) }
        }
        TextbookBlock.Divider -> 1
    }.coerceAtLeast(1)

    private fun splitBlock(block: TextbookBlock, metrics: TextbookPageMetrics): List<TextbookBlock> {
        val cost = estimateLines(block, metrics.charsPerLine)
        if (cost <= metrics.fragmentBudget || block is TextbookBlock.Heading || block is TextbookBlock.Divider) {
            return listOf(block)
        }
        return when (block) {
            is TextbookBlock.Paragraph -> splitParagraph(block, metrics)
            is TextbookBlock.BulletList -> splitBulletList(block, metrics)
            is TextbookBlock.Code -> splitCode(block, metrics)
            is TextbookBlock.Table -> splitTable(block, metrics)
            is TextbookBlock.Heading,
            TextbookBlock.Divider -> listOf(block)
        }
    }

    private fun splitParagraph(block: TextbookBlock.Paragraph, metrics: TextbookPageMetrics): List<TextbookBlock> {
        val maxWrappedLines = (metrics.fragmentBudget - 1).coerceAtLeast(3)
        val words = block.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf(TextbookBlock.Paragraph(""))
        val out = mutableListOf<TextbookBlock>()
        val current = mutableListOf<String>()
        words.forEach { word ->
            val candidate = (current + word).joinToString(" ")
            if (current.isNotEmpty() && wrappedLines(candidate, metrics.charsPerLine) > maxWrappedLines) {
                out += TextbookBlock.Paragraph(current.joinToString(" "))
                current.clear()
            }
            current += word
        }
        if (current.isNotEmpty()) out += TextbookBlock.Paragraph(current.joinToString(" "))
        return out
    }

    private fun splitBulletList(block: TextbookBlock.BulletList, metrics: TextbookPageMetrics): List<TextbookBlock> {
        val out = mutableListOf<TextbookBlock>()
        val current = mutableListOf<String>()
        var used = 1
        block.items.forEach { item ->
            val itemCost = 1 + wrappedLines(item, (metrics.charsPerLine - 3).coerceAtLeast(12))
            if (current.isNotEmpty() && used + itemCost > metrics.fragmentBudget) {
                out += TextbookBlock.BulletList(current.toList(), block.ordered)
                current.clear()
                used = 1
            }
            current += item
            used += itemCost
        }
        if (current.isNotEmpty()) out += TextbookBlock.BulletList(current.toList(), block.ordered)
        return out
    }

    private fun splitCode(block: TextbookBlock.Code, metrics: TextbookPageMetrics): List<TextbookBlock> {
        val out = mutableListOf<TextbookBlock>()
        val current = mutableListOf<String>()
        var used = 2
        block.text.lines().forEach { line ->
            val lineCost = wrappedLines(line.ifBlank { " " }, (metrics.charsPerLine - 2).coerceAtLeast(12))
            if (current.isNotEmpty() && used + lineCost > metrics.fragmentBudget) {
                out += TextbookBlock.Code(block.language, current.joinToString("\n"))
                current.clear()
                used = 2
            }
            current += line
            used += lineCost
        }
        if (current.isNotEmpty()) out += TextbookBlock.Code(block.language, current.joinToString("\n"))
        return out
    }

    private fun splitTable(block: TextbookBlock.Table, metrics: TextbookPageMetrics): List<TextbookBlock> {
        if (block.rows.isEmpty()) return listOf(block)
        val out = mutableListOf<TextbookBlock>()
        val current = mutableListOf<List<String>>()
        var used = 2
        block.rows.forEach { row ->
            val rowCost = 1 + row.sumOf { wrappedLines(it, (metrics.charsPerLine / 2).coerceAtLeast(10)) }
            if (current.isNotEmpty() && used + rowCost > metrics.fragmentBudget) {
                out += TextbookBlock.Table(block.headers, current.toList())
                current.clear()
                used = 2
            }
            current += row
            used += rowCost
        }
        if (current.isNotEmpty()) out += TextbookBlock.Table(block.headers, current.toList())
        return out
    }

    private fun wrappedLines(text: String, charsPerLine: Int): Int {
        val safeWidth = charsPerLine.coerceAtLeast(1)
        return text.split('\n').sumOf { line ->
            ceil(line.length.coerceAtLeast(1).toDouble() / safeWidth.toDouble()).toInt().coerceAtLeast(1)
        }
    }
}
