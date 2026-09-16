package com.futuretech.poweruser.textbook

import kotlin.math.ceil

internal data class BookDepthTopic(
    val title: String,
    val explanation: String
)

internal data class BookDepthPack(
    val sectionId: String,
    val topics: List<BookDepthTopic>,
    val workedExample: String,
    val mistakes: List<String>,
    val questions: List<String>
)

internal fun depthTopic(title: String, explanation: String) = BookDepthTopic(title, explanation)

internal fun depthPack(
    sectionId: String,
    topics: List<BookDepthTopic>,
    workedExample: String,
    mistakes: List<String>,
    questions: List<String>
) = BookDepthPack(sectionId, topics, workedExample, mistakes, questions)

/**
 * Extra authored reading for every learner-facing V3 LESSON.
 *
 * This layer is intentionally additive: the original V3 markdown stays intact. The goal is to
 * answer the questions a true beginner asks after the first explanation: why this exists, what is
 * happening underneath, where it breaks in real programs, and what neighboring idea to learn next.
 */
object V4BookDepthLibrary {
    private val packs: Map<String, BookDepthPack> = (
        V4BookDepthTrack01To03.packs +
            V4BookDepthTrack04To07.packs +
            V4BookDepthTrack08To11.packs
        ).associateBy { it.sectionId }

    fun blocksFor(sectionId: String): List<TextbookBlock> {
        val pack = packs[sectionId] ?: return emptyList()
        return buildList {
            add(TextbookBlock.Divider)
            add(TextbookBlock.Heading(3, "더 깊게 파보기 · 여기서 멈추지 않는다"))
            add(
                TextbookBlock.Paragraph(
                    "앞의 본문은 첫 이해를 만드는 설명이다. 아래에서는 같은 개념을 다른 각도에서 다시 보고, 실제 프로그램에서 어떤 문제가 생기는지까지 연결한다. 처음 읽을 때 전부 외우지 말고 ‘왜 그런지’를 따라가는 데 집중한다."
                )
            )
            pack.topics.forEachIndexed { index, topic ->
                add(TextbookBlock.Heading(4, "${index + 1}. ${topic.title}"))
                add(TextbookBlock.Paragraph(topic.explanation))
            }
            add(TextbookBlock.Heading(4, "손으로 따라가는 실전 흐름"))
            add(TextbookBlock.Code("text", pack.workedExample.trimIndent()))
            add(TextbookBlock.Heading(4, "초보자가 실제로 많이 틀리는 지점"))
            add(TextbookBlock.BulletList(pack.mistakes, ordered = false))
            add(TextbookBlock.Heading(4, "여기까지 이해했다면 다음 질문도 생각해 본다"))
            add(TextbookBlock.BulletList(pack.questions, ordered = false))
        }
    }

    fun hasDepthFor(sectionId: String): Boolean = sectionId in packs

    fun sectionIds(): Set<String> = packs.keys

    fun extraCharacterCount(sectionId: String): Int = blocksFor(sectionId).sumOf { block ->
        when (block) {
            is TextbookBlock.Heading -> block.text.length
            is TextbookBlock.Paragraph -> block.text.length
            is TextbookBlock.BulletList -> block.items.sumOf { it.length }
            is TextbookBlock.Code -> block.text.length
            is TextbookBlock.Table -> block.headers.sumOf { it.length } + block.rows.flatten().sumOf { it.length }
            TextbookBlock.Divider -> 0
        }
    }

    fun extraEstimatedMinutes(sectionId: String): Int =
        ceil(extraCharacterCount(sectionId) / 520.0).toInt().coerceAtLeast(0)
}
