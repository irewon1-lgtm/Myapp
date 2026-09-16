package com.futuretech.poweruser.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.TextbookBlock
import com.futuretech.poweruser.textbook.TextbookContentPage
import java.util.ArrayDeque
import kotlin.math.max

/**
 * Pagination driven by the same Compose text engine which renders the page.
 *
 * The old reader guessed characters per line and divided the viewport by a fixed 27dp line height.
 * That cannot know the real width of Hangul/Latin glyphs, heading wrapping, font scale, code, lists
 * or tables. This composer measures those units in pixels at the active density and width, then
 * slices only authored units that are safe to split.
 */
internal object MeasuredTextbookPageComposer {
    private val paragraphStyle = TextStyle(fontSize = 16.5.sp, lineHeight = 27.sp)
    private val bulletBodyStyle = TextStyle(fontSize = 15.sp, lineHeight = 23.sp)
    private val bulletMarkStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
    private val codeStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp)
    private val codeLabelStyle = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold)
    private val tableHeaderStyle = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold)
    private val tableValueStyle = TextStyle(fontSize = 13.sp, lineHeight = 19.sp)

    internal data class ResultPage(
        val content: TextbookContentPage,
        val usedHeightPx: Int,
        val availableHeightPx: Int
    ) {
        val utilization: Double
            get() = if (availableHeightPx <= 0) 1.0 else usedHeightPx.toDouble() / availableHeightPx.toDouble()
    }

    fun paginate(
        blocks: List<TextbookBlock>,
        contentWidthPx: Int,
        contentHeightPx: Int,
        density: Density,
        textMeasurer: TextMeasurer
    ): List<ResultPage> {
        require(contentWidthPx > 0) { "contentWidthPx must be positive" }
        require(contentHeightPx > 0) { "contentHeightPx must be positive" }
        if (blocks.isEmpty()) {
            return listOf(ResultPage(TextbookContentPage(emptyList(), 0), 0, contentHeightPx))
        }

        val blockGap = with(density) { 8.dp.roundToPx() }
        val oneBodyLine = measureText("가", paragraphStyle, contentWidthPx, textMeasurer).size.height
        val queue = ArrayDeque<TextbookBlock>()
        blocks.forEach(queue::addLast)
        val pages = mutableListOf<ResultPage>()
        var current = mutableListOf<TextbookBlock>()
        var used = 0

        fun gapBeforeNext(): Int = if (current.isEmpty()) 0 else blockGap

        fun flush() {
            if (current.isEmpty()) return
            pages += ResultPage(
                content = TextbookContentPage(current.toList(), estimatedLines = 0),
                usedHeightPx = used,
                availableHeightPx = contentHeightPx
            )
            current = mutableListOf()
            used = 0
        }

        fun add(block: TextbookBlock, measuredHeight: Int) {
            used += gapBeforeNext() + measuredHeight
            current += block
        }

        while (queue.isNotEmpty()) {
            val block = queue.removeFirst()
            val gap = gapBeforeNext()
            val remaining = contentHeightPx - used - gap
            val fullHeight = measureBlock(block, contentWidthPx, density, textMeasurer)

            if (block is TextbookBlock.Heading && current.isNotEmpty()) {
                val headingLayout = measureText(block.text, headingStyle(block.level), contentWidthPx, textMeasurer)
                // Extreme simulation showed that reserving two body lines after every heading creates
                // large artificial holes on compact pages. Keep a short one-line heading with one
                // real body line. A heading which already wraps to 2+ lines reserves no extra body
                // height; the heading itself is already a substantial measured unit.
                val bodyReserve = if (headingLayout.lineCount <= 1) oneBodyLine else 0
                if (remaining < fullHeight + bodyReserve) {
                    flush()
                    queue.addFirst(block)
                    continue
                }
            }

            if (fullHeight <= remaining) {
                add(block, fullHeight)
                continue
            }

            val split = splitForHeight(block, contentWidthPx, remaining, density, textMeasurer)
            if (split != null) {
                val headHeight = measureBlock(split.first, contentWidthPx, density, textMeasurer)
                add(split.first, headHeight)
                split.second?.let(queue::addFirst)
                flush()
                continue
            }

            if (current.isNotEmpty()) {
                flush()
                queue.addFirst(block)
            } else {
                // An authored atom which genuinely cannot be split is surfaced as over-budget rather
                // than silently dropped or rewritten. Runtime CLEAN must reject actual clipping.
                add(block, fullHeight)
                flush()
            }
        }
        flush()

        return pages.ifEmpty {
            listOf(ResultPage(TextbookContentPage(emptyList(), 0), 0, contentHeightPx))
        }
    }

    private fun headingStyle(level: Int): TextStyle {
        val size = when (level) {
            1 -> 24.sp
            2 -> 21.sp
            3 -> 18.sp
            4 -> 16.sp
            else -> 15.sp
        }
        return TextStyle(
            fontSize = size,
            lineHeight = (size.value + 7).sp,
            fontWeight = if (level <= 3) FontWeight.Bold else FontWeight.SemiBold
        )
    }

    private fun measureBlock(
        block: TextbookBlock,
        widthPx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Int = when (block) {
        is TextbookBlock.Heading -> {
            val topPad = with(density) { (if (block.level <= 3) 4.dp else 1.dp).roundToPx() }
            topPad + measureText(block.text, headingStyle(block.level), widthPx, measurer).size.height
        }
        is TextbookBlock.Paragraph -> measureText(block.text, paragraphStyle, widthPx, measurer).size.height
        is TextbookBlock.BulletList -> measureBulletList(block, widthPx, density, measurer)
        is TextbookBlock.Code -> measureCode(block, widthPx, density, measurer)
        is TextbookBlock.Table -> measureTable(block, widthPx, density, measurer)
        TextbookBlock.Divider -> with(density) { 7.dp.roundToPx() }
    }

    private fun measureBulletList(
        block: TextbookBlock.BulletList,
        widthPx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Int {
        val verticalPad = with(density) { 4.dp.roundToPx() }
        val itemGap = with(density) { 5.dp.roundToPx() }
        val markerWidth = with(density) { 28.dp.roundToPx() }
        val bodyWidth = (widthPx - markerWidth).coerceAtLeast(1)
        var height = verticalPad
        block.items.forEachIndexed { index, item ->
            if (index > 0) height += itemGap
            val marker = if (block.ordered) "${index + 1}." else "•"
            val markerHeight = measureText(marker, bulletMarkStyle, markerWidth, measurer).size.height
            val bodyHeight = measureText(item, bulletBodyStyle, bodyWidth, measurer).size.height
            height += max(markerHeight, bodyHeight)
        }
        return height
    }

    private fun measureCode(
        block: TextbookBlock.Code,
        widthPx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Int {
        val horizontalPad = with(density) { 24.dp.roundToPx() }
        val bodyWidth = (widthPx - horizontalPad).coerceAtLeast(1)
        val bodyPad = with(density) { 24.dp.roundToPx() }
        var height = bodyPad + measureText(block.text.ifBlank { " " }, codeStyle, bodyWidth, measurer).size.height
        if (block.language.isNotBlank()) {
            val labelVerticalPad = with(density) { 14.dp.roundToPx() }
            height += labelVerticalPad + measureText(block.language.uppercase(), codeLabelStyle, bodyWidth, measurer).size.height
            height += 1
        }
        return height
    }

    private fun measureTable(
        block: TextbookBlock.Table,
        widthPx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Int {
        val outerPad = with(density) { 24.dp.roundToPx() }
        val rowVerticalPad = with(density) { 12.dp.roundToPx() }
        val childGap = with(density) { 2.dp.roundToPx() }
        val innerWidth = (widthPx - with(density) { 24.dp.roundToPx() }).coerceAtLeast(1)
        var total = outerPad
        block.rows.forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) total += 1
            var children = 0
            var rowHeight = rowVerticalPad
            block.headers.forEachIndexed { columnIndex, header ->
                val value = row.getOrNull(columnIndex).orEmpty()
                if (value.isNotBlank()) {
                    if (children > 0) rowHeight += childGap
                    rowHeight += measureText(header, tableHeaderStyle, innerWidth, measurer).size.height
                    children++
                    rowHeight += childGap
                    rowHeight += measureText(value, tableValueStyle, innerWidth, measurer).size.height
                    children++
                }
            }
            total += rowHeight
        }
        return total
    }

    private fun splitForHeight(
        block: TextbookBlock,
        widthPx: Int,
        availablePx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Pair<TextbookBlock, TextbookBlock?>? {
        if (availablePx <= 0) return null
        return when (block) {
            is TextbookBlock.Paragraph -> splitParagraph(block, widthPx, availablePx, measurer)
            is TextbookBlock.BulletList -> splitList(block, widthPx, availablePx, density, measurer)
            is TextbookBlock.Code -> splitCode(block, widthPx, availablePx, density, measurer)
            is TextbookBlock.Table -> splitTable(block, widthPx, availablePx, density, measurer)
            is TextbookBlock.Heading, TextbookBlock.Divider -> null
        }
    }

    private fun splitParagraph(
        block: TextbookBlock.Paragraph,
        widthPx: Int,
        availablePx: Int,
        measurer: TextMeasurer
    ): Pair<TextbookBlock, TextbookBlock?>? {
        val layout = measureText(block.text, paragraphStyle, widthPx, measurer)
        if (layout.lineCount < 3) return null

        var lastFittingLine = -1
        for (line in 0 until layout.lineCount) {
            if (layout.getLineBottom(line) <= availablePx.toFloat()) lastFittingLine = line else break
        }
        if (lastFittingLine < 1 || lastFittingLine >= layout.lineCount - 1) return null

        // Do not leave a one-line widow in the paragraph tail when the head can give one line back.
        if (layout.lineCount - (lastFittingLine + 1) == 1 && lastFittingLine >= 2) {
            lastFittingLine--
        }
        val splitIndex = layout.getLineEnd(lastFittingLine, true).coerceIn(1, block.text.lastIndex)
        val head = block.text.substring(0, splitIndex).trimEnd()
        val tail = block.text.substring(splitIndex).trimStart()
        if (head.isBlank() || tail.isBlank()) return null
        return TextbookBlock.Paragraph(head) to TextbookBlock.Paragraph(tail)
    }

    private fun splitList(
        block: TextbookBlock.BulletList,
        widthPx: Int,
        availablePx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Pair<TextbookBlock, TextbookBlock?>? {
        if (block.items.size <= 1) return null
        var low = 1
        var high = block.items.lastIndex
        var best = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            val candidate = TextbookBlock.BulletList(block.items.take(mid), block.ordered)
            if (measureBulletList(candidate, widthPx, density, measurer) <= availablePx) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (best <= 0 || best >= block.items.size) return null
        return TextbookBlock.BulletList(block.items.take(best), block.ordered) to
            TextbookBlock.BulletList(block.items.drop(best), block.ordered)
    }

    private fun splitCode(
        block: TextbookBlock.Code,
        widthPx: Int,
        availablePx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Pair<TextbookBlock, TextbookBlock?>? {
        val lines = block.text.lines()
        if (lines.size <= 1) return null
        var low = 1
        var high = lines.lastIndex
        var best = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            val candidate = TextbookBlock.Code(block.language, lines.take(mid).joinToString("\n"))
            if (measureCode(candidate, widthPx, density, measurer) <= availablePx) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (best <= 0 || best >= lines.size) return null
        return TextbookBlock.Code(block.language, lines.take(best).joinToString("\n")) to
            TextbookBlock.Code(block.language, lines.drop(best).joinToString("\n"))
    }

    private fun splitTable(
        block: TextbookBlock.Table,
        widthPx: Int,
        availablePx: Int,
        density: Density,
        measurer: TextMeasurer
    ): Pair<TextbookBlock, TextbookBlock?>? {
        if (block.rows.size <= 1) return null
        var low = 1
        var high = block.rows.lastIndex
        var best = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            val candidate = TextbookBlock.Table(block.headers, block.rows.take(mid))
            if (measureTable(candidate, widthPx, density, measurer) <= availablePx) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (best <= 0 || best >= block.rows.size) return null
        return TextbookBlock.Table(block.headers, block.rows.take(best)) to
            TextbookBlock.Table(block.headers, block.rows.drop(best))
    }

    private fun measureText(
        text: String,
        style: TextStyle,
        widthPx: Int,
        measurer: TextMeasurer
    ): TextLayoutResult = measurer.measure(
        text = AnnotatedString(text),
        style = style,
        constraints = Constraints(maxWidth = widthPx.coerceAtLeast(1))
    )
}
