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
    fun allElevenRealTracksSplitWithoutContentLossAndKeepExplicitLessonBoundaries() {
        V1TextbookCatalog.chapters.forEach { track ->
            val markdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(track.id, blocks)
            val authoredLessonTitles = blocks
                .filterIsInstance<TextbookBlock.Heading>()
                .filter { it.level == 3 }
                .map { it.text }
            val lessonSections = sections.filter { section ->
                section.blocks.filterIsInstance<TextbookBlock.Heading>().any { it.level == 3 }
            }

            assertTrue("${track.id}: parsed blocks", blocks.isNotEmpty())
            assertTrue("${track.id}: book-scale reading sections", sections.size >= 20)
            assertEquals("${track.id}: block order/content preserved", blocks, sections.flatMap { it.blocks })
            assertEquals("${track.id}: section ids unique", sections.size, sections.map { it.id }.toSet().size)
            assertTrue("${track.id}: every estimate 4-7 min", sections.all { it.estimatedMinutes in 4..7 })
            assertTrue("${track.id}: every section titled", sections.all { it.title.isNotBlank() })
            assertTrue("${track.id}: no empty section", sections.all { it.blocks.isNotEmpty() })
            assertEquals("${track.id}: one reader page per authored LESSON", authoredLessonTitles.size, lessonSections.size)
            assertEquals("${track.id}: reader titles follow H3 LESSON titles", authoredLessonTitles, lessonSections.map { it.title })
        }
    }
}
