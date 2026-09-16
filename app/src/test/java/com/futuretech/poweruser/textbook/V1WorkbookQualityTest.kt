package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V1WorkbookQualityTest {
    private fun assetFile(path: String): File {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate legacy workbook asset: $path")
    }

    @Test
    fun allElevenLegacyWorkbooksRemainIntactAsStandalonePractice() {
        val workbookFiles = (1..11).map { number ->
            assetFile("textbook/v1/workbook_${number.toString().padStart(2, '0')}.md")
        }
        assertEquals(11, workbookFiles.size)

        workbookFiles.forEachIndexed { index, file ->
            val number = index + 1
            val text = file.readText(Charsets.UTF_8)
            val trainingHeadings = text.lineSequence().count {
                it.startsWith("## 훈련") ||
                    it.startsWith("## 프로젝트") ||
                    it.startsWith("# 프로젝트") ||
                    it.startsWith("# 종합 시나리오")
            }

            assertTrue("workbook $number: accidental truncation", text.length >= 3_000)
            assertTrue("workbook $number: needs hands-on tasks", trainingHeadings >= 8)
            assertTrue("workbook $number: needs code/data examples", text.contains("```"))
            assertTrue(
                "workbook $number: must include direct learner action",
                listOf("직접", "수정", "판정", "실행", "디버깅", "프로젝트", "시나리오").any(text::contains)
            )
            assertTrue(
                "workbook $number: placeholder text found",
                listOf("TODO", "TBD", "LOREM", "나중에 작성", "준비중")
                    .none { text.contains(it, ignoreCase = true) }
            )

            val paragraphs = text
                .split(Regex("\\n\\s*\\n"))
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { it.length >= 100 && !it.startsWith("```") && !it.startsWith("|") }
            val duplicates = paragraphs.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue("workbook $number: repeated explanatory paragraph", duplicates.isEmpty())
        }
    }

    @Test
    fun v2ReaderDoesNotAppendLegacyWorkbookIntoTrackBody() {
        val candidates = listOf(
            File("src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt"),
            File("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
        )
        val source = candidates.firstOrNull { it.isFile }?.readText(Charsets.UTF_8)
            ?: error("Cannot locate V1TextbookScreen.kt")

        assertTrue(source.contains("context.assets.open(selected.assetPath)"))
        assertFalse(source.contains("workbook_${'$'}{selected.number.toString().padStart(2, '0')}.md"))
        assertFalse(source.contains("val workbookText ="))
        assertFalse(source.contains("${'$'}chapterText\\n\\n---\\n\\n${'$'}workbookText"))
    }

    @Test
    fun everyLegacyWorkbookParsesIndependentlyWithoutContentLoss() {
        (1..11).forEach { number ->
            val workbookPath = "textbook/v1/workbook_${number.toString().padStart(2, '0')}.md"
            val workbookText = assetFile(workbookPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(workbookText)
            val sections = TextbookSectioner.split("LEGACY-WB-${number.toString().padStart(2, '0')}", blocks)

            assertTrue("workbook $number: blocks empty", blocks.isNotEmpty())
            assertEquals("workbook $number: content loss", blocks, sections.flatMap { it.blocks })
            assertTrue("workbook $number: needs at least one reader section", sections.isNotEmpty())
        }
    }
}
