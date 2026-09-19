package com.codingroadmap.app.data

import android.content.Context
import org.json.JSONObject

object CatalogLoader {
    fun load(context: Context): Catalog {
        val raw = context.assets.open("content/catalog.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val arr = root.getJSONArray("tracks")
        val tracks = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val chaptersJson = o.getJSONArray("chapters")
                val chapters = buildList {
                    for (j in 0 until chaptersJson.length()) add(chaptersJson.getString(j))
                }
                add(
                    TrackMeta(
                        id = o.getInt("id"),
                        title = o.getString("title"),
                        subtitle = o.getString("subtitle"),
                        available = o.getBoolean("available"),
                        chapters = chapters
                    )
                )
            }
        }
        return Catalog(tracks)
    }
}
