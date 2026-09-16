package com.futuretech.poweruser.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.futuretech.poweruser.textbook.TextbookBlock
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Real Compose text-layout contract. Unlike tools/v5_reader_extreme_sim.py this uses the device or
 * emulator font resolver and TextMeasurer. It is PREPARED until an instrumented run is actually
 * executed; the repository must never report this class as a PASS merely because it compiles.
 */
class MeasuredPaginationDeviceContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun koreanLatinCodeAndTableFitMeasuredPhonePagesWithoutDroppingContent() {
        val result = AtomicReference<List<MeasuredTextbookPageComposer.ResultPage>>()
        val sourceBlocks = mixedStressBlocks()

        composeRule.setContent {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer(cacheSize = 128)
            val widthPx = with(density) { 328.dp.roundToPx() }
            val heightPx = with(density) { 682.dp.roundToPx() }
            val pages = MeasuredTextbookPageComposer.paginate(
                blocks = sourceBlocks,
                contentWidthPx = widthPx,
                contentHeightPx = heightPx,
                density = density,
                textMeasurer = measurer
            )
            SideEffect { result.set(pages) }
        }

        composeRule.waitForIdle()
        val pages = result.get()
        assertTrue(pages.isNotEmpty())
        assertTrue("Measured pages must not overflow", pages.all { it.usedHeightPx <= it.availableHeightPx })

        val nonFinal = pages.dropLast(1)
        val lowDensity = nonFinal.count { it.utilization < 0.82 }
        val ratio = if (nonFinal.isEmpty()) 0.0 else lowDensity.toDouble() / nonFinal.size.toDouble()
        assertTrue("Too many sparse measured pages: low=$lowDensity pages=${nonFinal.size} ratio=$ratio", ratio <= 0.05)

        val reconstructed = pages.flatMap { it.content.blocks }
        assertSemanticContentPreserved(sourceBlocks, reconstructed)
    }

    @Test
    fun extremelyLongSplittableBlocksNeverProduceNonAtomicOverflow() {
        val result = AtomicReference<List<MeasuredTextbookPageComposer.ResultPage>>()
        val longKorean = buildString {
            repeat(260) { index ->
                append("문장 $index 에서는 같은 용어를 반복하는 대신 주소 변환과 상태 변화의 인과관계를 한 단계씩 추적한다. ")
            }
        }
        val longCode = (1..180).joinToString("\n") { "val step$it = previous + $it // measured code line" }
        val blocks = listOf(
            TextbookBlock.Paragraph(longKorean),
            TextbookBlock.Code("kotlin", longCode),
            TextbookBlock.BulletList((1..80).map { "항목 $it: 독립된 실패 조건과 관측 증거를 확인한다." }, false)
        )

        composeRule.setContent {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer(cacheSize = 256)
            val pages = MeasuredTextbookPageComposer.paginate(
                blocks = blocks,
                contentWidthPx = with(density) { 288.dp.roundToPx() },
                contentHeightPx = with(density) { 470.dp.roundToPx() },
                density = density,
                textMeasurer = measurer
            )
            SideEffect { result.set(pages) }
        }

        composeRule.waitForIdle()
        val pages = result.get()
        assertTrue(pages.size > 5)
        assertTrue("Splittable stress blocks must remain within measured page height", pages.all { it.usedHeightPx <= it.availableHeightPx })
        assertSemanticContentPreserved(blocks, pages.flatMap { it.content.blocks })
    }

    @Test
    fun orderedListContinuationKeepsAuthoredNumberingAcrossMeasuredPages() {
        val result = AtomicReference<List<MeasuredTextbookPageComposer.ResultPage>>()
        val items = (0 until 36).map { index ->
            "단계 ${7 + index}: 이 항목은 페이지가 나뉘어도 작성자가 부여한 순서를 유지해야 한다."
        }
        val block = TextbookBlock.BulletList(items = items, ordered = true, startNumber = 7)

        composeRule.setContent {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer(cacheSize = 64)
            val pages = MeasuredTextbookPageComposer.paginate(
                blocks = listOf(block),
                contentWidthPx = with(density) { 300.dp.roundToPx() },
                contentHeightPx = with(density) { 330.dp.roundToPx() },
                density = density,
                textMeasurer = measurer
            )
            SideEffect { result.set(pages) }
        }

        composeRule.waitForIdle()
        val listFragments = result.get().flatMap { page ->
            page.content.blocks.filterIsInstance<TextbookBlock.BulletList>()
        }
        assertTrue("Test must force an actual ordered-list page split", listFragments.size >= 2)

        var expectedStart = 7
        val reconstructedItems = mutableListOf<String>()
        listFragments.forEach { fragment ->
            assertTrue(fragment.ordered)
            assertEquals(expectedStart, fragment.startNumber)
            reconstructedItems += fragment.items
            expectedStart += fragment.items.size
        }
        assertEquals(items, reconstructedItems)
        assertEquals(43, expectedStart)
    }

    private fun mixedStressBlocks(): List<TextbookBlock> = listOf(
        TextbookBlock.Heading(2, "주소가 보인다고 물리 메모리 주소인 것은 아니다"),
        TextbookBlock.Paragraph(
            "프로세스가 사용하는 주소는 일반적으로 가상 주소다. 페이지 테이블과 TLB를 거쳐 실제 메모리 접근으로 연결되며, " +
                "이 구분을 놓치면 page fault와 segmentation fault, resident memory를 하나의 현상으로 오해하게 된다. " +
                "Korean 한글과 Latin text가 같은 문단에서 섞여도 줄바꿈은 글자 수 추정이 아니라 실제 glyph 측정으로 결정되어야 한다."
        ),
        TextbookBlock.BulletList(
            listOf(
                "virtual address와 physical frame을 구분한다.",
                "TLB miss와 page fault를 같은 사건으로 부르지 않는다.",
                "관측값이 RSS인지 virtual size인지 먼저 확인한다.",
                "실패 원인은 실제 trace와 counter로 좁힌다."
            ),
            false
        ),
        TextbookBlock.Code(
            "kotlin",
            """val encoded = text.toByteArray(Charsets.UTF_8)
val byteCount = encoded.size
require(byteCount <= limit) { "payload too large" }
println(encoded.joinToString(" ") { "%02x".format(it) })"""
        ),
        TextbookBlock.Table(
            headers = listOf("관측", "뜻"),
            rows = listOf(
                listOf("TLB miss", "주소 변환 cache에서 원하는 translation을 찾지 못함"),
                listOf("minor fault", "storage I/O 없이 kernel 처리가 가능한 page fault 계열"),
                listOf("major fault", "필요한 page를 가져오는 더 비싼 경로와 연결될 수 있음"),
                listOf("RSS", "현재 resident한 process memory를 보는 한 종류의 관측값")
            )
        ),
        TextbookBlock.Heading(3, "긴 토큰과 실제 줄바꿈"),
        TextbookBlock.Paragraph(
            "unbroken_token_0123456789_abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ_끝 " +
                "같은 긴 토큰이 있어도 페이지 계산이 임의의 25자 단위 추정으로 돌아가서는 안 된다."
        )
    )

    /**
     * Splitting is allowed, reordering or dropping is not. Repeated table headers on continuation
     * pages are presentation metadata, so row payload is compared independently while every
     * continuation table must keep one of the original authored header sets.
     */
    private fun assertSemanticContentPreserved(
        original: List<TextbookBlock>,
        reconstructed: List<TextbookBlock>
    ) {
        val originalTableHeaders = original.filterIsInstance<TextbookBlock.Table>().map { it.headers }.toSet()
        val reconstructedTables = reconstructed.filterIsInstance<TextbookBlock.Table>()
        assertTrue(
            "Every continued table must preserve an authored header",
            reconstructedTables.all { it.headers in originalTableHeaders }
        )

        fun payload(blocks: List<TextbookBlock>): String = blocks.joinToString(" ") { block ->
            when (block) {
                is TextbookBlock.Heading -> block.text
                is TextbookBlock.Paragraph -> block.text
                is TextbookBlock.BulletList -> block.items.joinToString(" ")
                is TextbookBlock.Code -> block.text
                is TextbookBlock.Table -> block.rows.flatten().joinToString(" ")
                TextbookBlock.Divider -> ""
            }
        }.replace(Regex("\\s+"), " ").trim()

        assertTrue(payload(original).isNotBlank())
        assertEquals(payload(original), payload(reconstructed))
    }
}
