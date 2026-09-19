package com.codingroadmap.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class GlossaryEntry(
    val term: String,
    val description: String,
    val usage: String,
    val example: String
)

data class ExtraSection(
    val title: String,
    val body: String
)

data class ContentPage(
    val kind: String,
    val eyebrow: String,
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList(),
    val extras: List<ExtraSection> = emptyList(),
    val visibleExtras: Int = Int.MAX_VALUE,
    val closingPrompt: String? = null,
    val code: String? = null,
    val codeNote: String? = null,
    val calloutTitle: String? = null,
    val calloutBody: String? = null,
    val practicePrompt: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val mistake: String? = null,
    val visual: String? = null
)

data class ChapterContent(
    val chapter: Int,
    val title: String,
    val pages: List<ContentPage>
)

object Track1ContentLoader {
    fun load(context: Context): List<ChapterContent> =
        (1..8).map { chapter ->
            val path = "content/track01/ch%02d.json".format(chapter)
            val raw = context.assets.open(path).bufferedReader().use { it.readText() }
            parseChapter(JSONObject(raw))
        }

    private fun parseChapter(o: JSONObject): ChapterContent {
        val pagesJson = o.getJSONArray("pages")
        val pages = buildList {
            for (i in 0 until pagesJson.length()) {
                add(parsePage(pagesJson.getJSONObject(i)))
            }
        }
        return ChapterContent(
            chapter = o.getInt("chapter"),
            title = o.getString("title"),
            pages = pages
        )
    }

    private fun parsePage(o: JSONObject): ContentPage = ContentPage(
        kind = o.optString("kind"),
        eyebrow = o.optString("eyebrow"),
        title = o.optString("title"),
        paragraphs = o.optStringList("paragraphs"),
        keywords = o.optStringList("keywords"),
        bullets = o.optStringList("bullets"),
        glossary = o.optObjectList("glossary") { g ->
            GlossaryEntry(
                term = g.getString("term"),
                description = g.getString("description"),
                usage = g.getString("usage"),
                example = g.getString("example")
            )
        },
        extras = o.optObjectList("extras") { e ->
            ExtraSection(
                title = e.getString("title"),
                body = e.getString("body")
            )
        },
        visibleExtras = o.optInt("visibleExtras", Int.MAX_VALUE),
        closingPrompt = o.optNullableString("closingPrompt"),
        code = o.optNullableString("code"),
        codeNote = o.optNullableString("codeNote"),
        calloutTitle = o.optNullableString("calloutTitle"),
        calloutBody = o.optNullableString("calloutBody"),
        practicePrompt = o.optNullableString("practicePrompt"),
        question = o.optNullableString("question"),
        answer = o.optNullableString("answer"),
        mistake = o.optNullableString("mistake"),
        visual = o.optNullableString("visual")
    )

    private fun JSONObject.optStringList(key: String): List<String> {
        val arr: JSONArray = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
    }

    private fun <T> JSONObject.optObjectList(
        key: String,
        map: (JSONObject) -> T
    ): List<T> {
        val arr: JSONArray = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) add(map(arr.getJSONObject(i)))
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
