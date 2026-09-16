package com.futuretech.poweruser.textbook

/** Pure data/model layer for the tablet textbook. Kept Android-free so JVM tests can hit it hard. */
enum class TextbookLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED;

    companion object {
        fun fromWidthDp(widthDp: Int): TextbookLayoutMode = when {
            widthDp >= 1080 -> EXPANDED
            widthDp >= 720 -> MEDIUM
            else -> COMPACT
        }
    }
}

sealed interface TextbookBlock {
    data class Heading(val level: Int, val text: String) : TextbookBlock
    data class Paragraph(val text: String) : TextbookBlock
    /**
     * startNumber is meaningful only for ordered lists. It is explicit because a list may be split
     * across book pages; resetting a continuation page to 1 changes learner-visible meaning.
     */
    data class BulletList(
        val items: List<String>,
        val ordered: Boolean,
        val startNumber: Int = 1
    ) : TextbookBlock {
        init {
            require(startNumber >= 1) { "startNumber must be positive: $startNumber" }
        }
    }
    data class Code(val language: String, val text: String) : TextbookBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : TextbookBlock
    data object Divider : TextbookBlock
}

object TextbookMarkdownParser {
    private val orderedItem = Regex("^(\\d+)[.)]\\s+(.+)$")

    fun parse(markdown: String): List<TextbookBlock> {
        val lines = markdown.replace("\r\n", "\n").lines()
        val out = mutableListOf<TextbookBlock>()
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trimEnd()
            when {
                line.isBlank() -> i++
                line.trim() == "---" -> { out += TextbookBlock.Divider; i++ }
                line.startsWith("```") -> {
                    val language = line.removePrefix("```").trim(); i++
                    val code = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) { code += lines[i]; i++ }
                    if (i < lines.size) i++
                    out += TextbookBlock.Code(language, code.joinToString("\n").trimEnd())
                }
                line.startsWith("#### ") -> { out += TextbookBlock.Heading(4, cleanInline(line.removePrefix("#### "))); i++ }
                line.startsWith("### ") -> { out += TextbookBlock.Heading(3, cleanInline(line.removePrefix("### "))); i++ }
                line.startsWith("## ") -> { out += TextbookBlock.Heading(2, cleanInline(line.removePrefix("## "))); i++ }
                line.startsWith("# ") -> { out += TextbookBlock.Heading(1, cleanInline(line.removePrefix("# "))); i++ }
                isTableStart(lines, i) -> {
                    val headers = splitTableRow(lines[i]); i += 2
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) { rows += splitTableRow(lines[i]); i++ }
                    out += TextbookBlock.Table(headers, rows)
                }
                line.trimStart().startsWith("- ") -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith("- ")) { items += cleanInline(lines[i].trimStart().removePrefix("- ")); i++ }
                    out += TextbookBlock.BulletList(items, ordered = false)
                }
                orderedItem.matches(line.trimStart()) -> {
                    val items = mutableListOf<String>()
                    val firstMatch = requireNotNull(orderedItem.find(line.trimStart()))
                    val startNumber = firstMatch.groupValues[1].toInt()
                    var expected = startNumber
                    while (i < lines.size) {
                        val match = orderedItem.find(lines[i].trimStart()) ?: break
                        val actualNumber = match.groupValues[1].toInt()
                        // A numbering jump starts a new authored list instead of silently changing it.
                        if (actualNumber != expected) break
                        items += cleanInline(match.groupValues[2])
                        expected++
                        i++
                    }
                    out += TextbookBlock.BulletList(items, ordered = true, startNumber = startNumber)
                }
                else -> {
                    val paragraph = mutableListOf<String>()
                    while (i < lines.size && !isSpecialStart(lines, i)) { if (lines[i].isNotBlank()) paragraph += lines[i].trim(); i++ }
                    if (paragraph.isNotEmpty()) out += TextbookBlock.Paragraph(cleanInline(paragraph.joinToString(" "))) else i++
                }
            }
        }
        return out
    }

    private fun isSpecialStart(lines: List<String>, index: Int): Boolean {
        if (index >= lines.size) return true
        val s = lines[index].trimEnd()
        if (s.isBlank()) return true
        if (s.trim() == "---" || s.startsWith("```") || s.startsWith("#")) return true
        if (s.trimStart().startsWith("- ") || orderedItem.matches(s.trimStart())) return true
        return isTableStart(lines, index)
    }

    private fun isTableStart(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size) return false
        val first = lines[index].trim(); val second = lines[index + 1].trim()
        if (!first.startsWith("|") || !second.startsWith("|")) return false
        val delimiterCells = splitTableRow(second)
        return delimiterCells.isNotEmpty() && delimiterCells.all { cell -> val normalized = cell.replace(":", "").trim(); normalized.length >= 3 && normalized.all { it == '-' } }
    }

    private fun splitTableRow(line: String): List<String> = line.trim().trim('|').split('|').map { cleanInline(it.trim()) }
    private fun cleanInline(value: String): String = value.replace("**", "").replace("__", "").replace("`", "").trim()
}
