package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1WorkbookQualityTest {
    private fun assetFile(path: String): File {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate V1 workbook asset: $path")
    }

    @Test
    fun allElevenWorkbooksExistAndAddPracticeRatherThanPadding() {
        val workbookFiles = (1..11).map { number ->
            assetFile("textbook/v1/workbook_${number.toString().padStart(2, '0')}.md")
        }
        assertEquals(11, workbookFiles.size)

        workbookFiles.forEachIndexed { index, file ->
            val chapterNumber = index + 1
            val text = file.readText(Charsets.UTF_8)
            val trainingHeadings = text.lineSequence().count {
                it.startsWith("## 훈련") ||
                    it.startsWith("## 프로젝트") ||
                    it.startsWith("# 프로젝트") ||
                    it.startsWith("# 종합 시나리오")
            }

            assertTrue("workbook $chapterNumber: accidental truncation", text.length >= 5_500)
            assertTrue("workbook $chapterNumber: needs hands-on tasks", trainingHeadings >= 8)
            assertTrue("workbook $chapterNumber: needs code/data examples", text.contains("```"))
            assertTrue(
                "workbook $chapterNumber: must include direct learner action",
                listOf("직접", "수정", "판정", "실행", "디버깅", "프로젝트", "시나리오").any(text::contains)
            )
            assertTrue(
                "workbook $chapterNumber: placeholder text found",
                listOf("TODO", "TBD", "LOREM", "나중에 작성", "준비중")
                    .none { text.contains(it, ignoreCase = true) }
            )

            val paragraphs = text
                .split(Regex("\\n\\s*\\n"))
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { it.length >= 100 && !it.startsWith("```") && !it.startsWith("|") }
            val duplicates = paragraphs.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue("workbook $chapterNumber: repeated explanatory paragraph", duplicates.isEmpty())
        }
    }

    @Test
    fun readerSourceRequiresWorkbookAfterChapterText() {
        val candidates = listOf(
            File("src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt"),
            File("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
        )
        val source = candidates.firstOrNull { it.isFile }?.readText(Charsets.UTF_8)
            ?: error("Cannot locate V1TextbookScreen.kt")

        assertTrue(source.contains("workbook_${'$'}{selected.number.toString().padStart(2, '0')}.md"))
        assertTrue(source.contains("val chapterText ="))
        assertTrue(source.contains("val workbookText ="))
        assertTrue(source.contains("${'$'}chapterText\\n\\n---\\n\\n${'$'}workbookText"))
    }

    @Test
    fun everyChapterAndWorkbookPairParsesWithoutContentLoss() {
        V1TextbookCatalog.chapters.forEach { chapter ->
            val chapterText = assetFile(chapter.assetPath).readText(Charsets.UTF_8)
            val workbookPath = "textbook/v1/workbook_${chapter.number.toString().padStart(2, '0')}.md"
            val workbookText = assetFile(workbookPath).readText(Charsets.UTF_8)
            val combined = "$chapterText\n\n---\n\n$workbookText"

            val blocks = TextbookMarkdownParser.parse(combined)
            val sections = TextbookSectioner.split(chapter.id, blocks)

            assertTrue("${chapter.id}: combined blocks empty", blocks.isNotEmpty())
            assertTrue("${chapter.id}: workbook title missing after parse", blocks.any {
                it is TextbookBlock.Heading && it.text.contains("실전 훈련")
            })
            assertEquals("${chapter.id}: combined content loss", blocks, sections.flatMap { it.blocks })
            assertTrue("${chapter.id}: combined reader needs multiple sections", sections.size >= 8)
        }
    }
}
