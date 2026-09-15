package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookRealAssetSectionTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate real textbook asset: $assetPath; cwd=${File(".").absolutePath}")
    }

    @Test
    fun allElevenRealChaptersSplitWithoutContentLossIntoFourToSevenMinuteSections() {
        V1TextbookCatalog.chapters.forEach { chapter ->
            val markdown = assetFile(chapter.assetPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(chapter.id, blocks)

            assertTrue("${chapter.id}: parsed blocks", blocks.isNotEmpty())
            assertTrue("${chapter.id}: multiple reading sections", sections.size >= 4)
            assertEquals("${chapter.id}: block order/content preserved", blocks, sections.flatMap { it.blocks })
            assertEquals("${chapter.id}: section ids unique", sections.size, sections.map { it.id }.toSet().size)
            assertTrue("${chapter.id}: every estimate 4-7 min", sections.all { it.estimatedMinutes in 4..7 })
            assertTrue("${chapter.id}: every section titled", sections.all { it.title.isNotBlank() })
            assertTrue("${chapter.id}: no empty section", sections.all { it.blocks.isNotEmpty() })
        }
    }
}
