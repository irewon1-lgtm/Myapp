package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V4DepthIntegrationTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate real textbook asset: $assetPath")
    }

    @Test
    fun allRealLessonsKeepOriginalBodyThenAddExpertDepthBeforeSelfCheckAnswers() {
        var lessonCount = 0

        V1TextbookCatalog.chapters.forEach { track ->
            val practice = requireNotNull(CurriculumDataRepository.lessonById(track.practiceLessonId))
            val markdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
            val sections = TextbookSectioner.split(track.id, TextbookMarkdownParser.parse(markdown))

            sections.forEach { section ->
                lessonCount += 1
                val concept = TextbookLearningFlow.buildConcepts(track.id, section, practice).single()
                val authoredStart = concept.blocks.indexOf(section.blocks.first())
                assertTrue("${section.id}: authored body must remain present", authoredStart >= 0)
                assertEquals(
                    "${section.id}: V3 authored blocks must stay contiguous and model-equal",
                    section.blocks,
                    concept.blocks.subList(authoredStart, authoredStart + section.blocks.size)
                )

                val depthBlocks = V4BookDepthLibrary.blocksFor(section.id)
                assertTrue("${section.id}: dedicated depth must exist", depthBlocks.isNotEmpty())
                val firstDepthHeading = depthBlocks.filterIsInstance<TextbookBlock.Heading>().first()
                val depthIndex = concept.blocks.indexOf(firstDepthHeading)
                val selfCheckIndex = concept.blocks.indexOfFirst { block ->
                    block is TextbookBlock.Heading && block.text.startsWith("자가점검 정답·해설")
                }
                val authoredEnd = authoredStart + section.blocks.size

                assertTrue("${section.id}: depth must follow the original lesson body", depthIndex >= authoredEnd)
                assertTrue("${section.id}: self-check answers must come after deeper reading", selfCheckIndex > depthIndex)
                assertTrue("${section.id}: recall prompt must include failure reasoning", concept.problem.prompt.contains("어디서 실패"))
            }
        }

        assertEquals("current curriculum contract is exactly 42 learner-facing lessons", 42, lessonCount)
    }

    @Test
    fun unicodeLessonNowTeachesNormalizationGraphemesAndByteDebuggingAsNormalBody() {
        val track = requireNotNull(V1TextbookCatalog.chapterById("V1-C01"))
        val practice = requireNotNull(CurriculumDataRepository.lessonById(track.practiceLessonId))
        val markdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
        val sections = TextbookSectioner.split(track.id, TextbookMarkdownParser.parse(markdown))
        val unicodeSection = sections[2]
        val concept = TextbookLearningFlow.buildConcepts(track.id, unicodeSection, practice).single()

        val tabletPages = TextbookPageComposer.paginate(
            concept.blocks,
            TextbookPageLayout(charsPerLine = 46, maxLines = 32)
        )
        val phonePages = TextbookPageComposer.paginate(
            concept.blocks,
            TextbookPageLayout(charsPerLine = 25, maxLines = 21)
        )
        val depthText = V4BookDepthLibrary.blocksFor(unicodeSection.id).joinToString("\n") { block ->
            when (block) {
                is TextbookBlock.Heading -> block.text
                is TextbookBlock.Paragraph -> block.text
                is TextbookBlock.BulletList -> block.items.joinToString("\n")
                is TextbookBlock.Code -> block.text
                is TextbookBlock.Table -> block.headers.joinToString(" ") + block.rows.flatten().joinToString(" ")
                TextbookBlock.Divider -> ""
            }
        }

        assertTrue("Unicode lesson should be substantial on tablet too", tabletPages.size >= 10)
        assertTrue("phone should need at least as many pages as tablet", phonePages.size >= tabletPages.size)
        assertTrue(depthText.contains("NFC"))
        assertTrue(depthText.contains("NFD"))
        assertTrue(depthText.contains("grapheme", ignoreCase = true))
        assertTrue(depthText.contains("BOM"))
        assertTrue(depthText.contains("hex", ignoreCase = true))
    }
}
