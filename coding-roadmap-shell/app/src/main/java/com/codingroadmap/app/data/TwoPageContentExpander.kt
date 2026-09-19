package com.codingroadmap.app.data

/**
 * Converts every authored reading page into a fixed two-page reading pair.
 *
 * Page 1 keeps the authored explanation, code, diagram and question in the normal reader.
 * Page 2 receives every optional "extra" section that used to be folded into a popup, plus
 * compact contextual deepening prompts. No authored extra is discarded.
 */
object TwoPageContentExpander {
    private val trailingHalfMarker = Regex("""\s+[12]/2$""")

    fun expand(chapter: ChapterContent): ChapterContent =
        chapter.copy(pages = chapter.pages.flatMap(::expandPage))

    private fun expandPage(source: ContentPage): List<ContentPage> {
        val first = source.copy(
            eyebrow = pairEyebrow(source.eyebrow, 1),
            extras = emptyList(),
            visibleExtras = 0,
            closingPrompt = null,
            answer = null,
            mistake = null
        )

        val secondSections = buildSecondPageSections(source)
        val second = ContentPage(
            kind = "depth",
            eyebrow = pairEyebrow(source.eyebrow, 2),
            title = "${source.title} · 더 깊게",
            paragraphs = secondPageIntro(source),
            keywords = source.keywords.take(6),
            extras = secondSections,
            visibleExtras = Int.MAX_VALUE,
            closingPrompt = source.closingPrompt ?: defaultClosingPrompt(source)
        )

        return listOf(first, second)
    }

    private fun pairEyebrow(raw: String, part: Int): String {
        val cleaned = raw.trim().replace(trailingHalfMarker, "").trim()
        val base = cleaned.ifBlank { "LEARNING PAGE" }
        return "$base · $part/2"
    }

    private fun secondPageIntro(source: ContentPage): List<String> {
        val focus = when {
            source.keywords.isNotEmpty() -> source.keywords.take(3).joinToString(" · ")
            source.glossary.isNotEmpty() -> source.glossary.take(3).joinToString(" · ") { it.term }
            else -> source.title
        }
        val first = "첫째 장에서 본 ‘${source.title}’을 여기서는 한 단계 더 연결합니다. 핵심은 내용을 더 많이 외우는 것이 아니라, 왜 그렇게 되는지와 실제 코드·실행·오류에서 어떻게 확인하는지를 함께 이해하는 것입니다."
        val second = "특히 $focus 를 따로 떼어 외우기보다 서로 어떤 관계인지 설명해 보세요. 정의 → 실제 모습 → 결과 확인 순서로 연결하면 처음 보는 코드에서도 같은 원리를 다시 찾기 쉬워집니다."
        return listOf(first, second)
    }

    private fun buildSecondPageSections(source: ContentPage): List<ExtraSection> {
        val sections = mutableListOf<ExtraSection>()

        source.answer?.takeIf { it.isNotBlank() }?.let {
            sections += ExtraSection("정답·해설", it)
        }
        source.mistake?.takeIf { it.isNotBlank() }?.let {
            sections += ExtraSection("자주 틀리는 이유", it)
        }
        sections += source.extras

        if (sections.size < 4) {
            sections += generatedSections(source).take(4 - sections.size)
        }

        return compactForTablet(sections)
    }

    private fun generatedSections(source: ContentPage): List<ExtraSection> {
        val readingLens = when {
            source.code != null -> ExtraSection(
                "코드를 더 깊게 읽는 순서",
                "코드를 바로 실행하기 전에 위에서 아래로 한 줄씩 읽으면서 ‘지금 어떤 값이 만들어졌는가’, ‘무엇이 바뀌었는가’, ‘무엇이 출력되는가’를 구분해 보세요. 실행 뒤에는 예상과 실제가 처음 달라진 줄부터 확인하면 원인을 훨씬 빨리 좁힐 수 있습니다."
            )
            source.visual != null -> ExtraSection(
                "도식을 더 깊게 읽는 순서",
                "그림을 한 번에 보지 말고 시작점 → 중간 단계 → 결과의 순서로 따라가세요. 각 화살표마다 ‘무엇이 들어오고 무엇이 달라졌는가’를 한 문장으로 말할 수 있으면 도식이 실제 동작 흐름으로 바뀝니다."
            )
            source.glossary.isNotEmpty() -> ExtraSection(
                "용어를 실제 상황에 연결하기",
                "용어는 뜻만 외우면 금방 잊기 쉽습니다. 각 단어를 ‘언제 쓰는 말인지’와 ‘코드나 화면에서 어떤 모습으로 만나는지’까지 한 묶음으로 기억하세요. 모르는 용어를 다시 만났을 때도 문맥으로 의미를 복원하기 쉬워집니다."
            )
            source.question != null -> ExtraSection(
                "답보다 근거를 먼저 보기",
                "확인 문제에서는 정답을 맞히는 것보다 근거를 설명하는 것이 중요합니다. 어떤 문법·값·실행 순서를 보고 그 답을 골랐는지 말해 보고, 틀렸다면 잘못 잡은 전제를 한 문장으로 기록하세요."
            )
            else -> ExtraSection(
                "원인과 결과를 연결하기",
                "이 개념을 ‘무엇이다’에서 끝내지 말고 ‘그래서 무엇이 달라지는가’까지 이어 설명해 보세요. 입력이나 상태가 바뀌었을 때 결과가 어떻게 달라지는지 생각하면 개념이 실제 동작과 연결됩니다."
            )
        }

        val verification = ExtraSection(
            "실제로 확인할 기준",
            "새 예제를 볼 때는 입력 → 처리 → 결과 순서로 나누고, 실행 전 예상과 실행 후 결과를 비교하세요. 예상과 다르면 한 번에 여러 곳을 고치지 말고 가장 먼저 달라진 지점 하나만 바꾼 뒤 다시 실행하는 것이 좋습니다."
        )

        val teachBack = ExtraSection(
            "내 말로 다시 설명하기",
            "이 페이지를 보지 않고 ‘무엇인지 → 왜 필요한지 → 아주 짧은 예시’의 세 단계로 설명해 보세요. 설명이 막히는 부분이 바로 다시 읽어야 할 지점입니다. 완벽한 문장을 만드는 것보다 핵심 관계를 빠뜨리지 않는 것이 더 중요합니다."
        )

        return listOf(readingLens, verification, teachBack)
    }

    private fun compactForTablet(sections: List<ExtraSection>): List<ExtraSection> {
        if (sections.size <= 6) return sections

        val head = sections.take(5)
        val tail = sections.drop(5)
        val mergedTail = ExtraSection(
            title = tail.joinToString(" · ") { it.title },
            body = tail.joinToString("\\n\\n") { "• ${it.title}: ${it.body}" }
        )
        return head + mergedTail
    }

    private fun defaultClosingPrompt(source: ContentPage): String =
        "‘${source.title}’을 처음 듣는 사람에게 두세 문장으로 설명해 보세요. 무엇인지, 왜 필요한지, 실제 코드나 실행에서 어디를 보면 확인할 수 있는지까지 이어 말하면 됩니다."
}
