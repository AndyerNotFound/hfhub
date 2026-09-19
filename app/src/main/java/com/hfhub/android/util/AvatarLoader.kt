package com.hfhub.android.util

import android.widget.ImageView
import com.hfhub.android.data.HfApi
import kotlinx.coroutines.runBlocking
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors







object AvatarLoader {

    private val urlCache = ConcurrentHashMap<String, String>()
    private val missing: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val exec = Executors.newFixedThreadPool(2)

    fun load(author: String, into: ImageView) {
        if (author.isBlank()) return
        into.tag = "av:$author"
        urlCache[author]?.let { url ->
            ImageLoader.load(url, into, circular = true)
            return
        }
        if (missing.contains(author)) return
        exec.execute {
            val url = try {
                runBlocking { HfApi.avatarUrl(author) }
            } catch (_: Exception) {
                null
            }
            if (url != null) {
                urlCache[author] = url
                into.post {
                    if (into.tag == "av:$author") ImageLoader.load(url, into, circular = true)
                }
            } else {
                missing.add(author)
            }
        }
    }
}
