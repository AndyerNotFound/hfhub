package com.hfhub.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.hfhub.android.data.HfApi
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors







object ImageLoader {

    private val mem = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val exec = Executors.newFixedThreadPool(3)

    private lateinit var dir: File
    private var appCtx: Context? = null

    
    private val failed = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val FAIL_TTL = 5 * 60_000L

    private const val DIR_MAX = 60L * 1024 * 1024
    private const val DIR_TRIM = 40L * 1024 * 1024

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        dir = File(ctx.cacheDir, "imgcache").apply { mkdirs() }
    }

    fun load(rawUrl: String, into: ImageView, circular: Boolean = false, placeholder: Int = 0x22000000) {
        if (!this::dir.isInitialized) init(into.context)
        val url = HfApi.cdnRewrite(rawUrl)
        val key = md5(url)

        mem.get(key)?.let {
            into.setImageDrawable(wrap(it, circular))
            return
        }
        failed[url]?.let { t -> if (System.currentTimeMillis() - t < FAIL_TTL) return }
        into.tag = key
        into.setImageDrawable(null)
        into.setBackgroundColor(placeholder)

        exec.execute {
            var bmp: Bitmap? = mem.get(key)
            if (bmp == null) bmp = readDisk(key)
            if (bmp == null) {
                bmp = download(url)
                if (bmp != null) writeDisk(key, bmp) else failed[url] = System.currentTimeMillis()
            }
            val result = bmp ?: return@execute
            mem.put(key, result)
            into.post {
                if (into.tag == key) {
                    into.setBackgroundColor(0x00000000)
                    into.setImageDrawable(wrap(result, circular))
                }
            }
        }
    }

    



    fun getSync(rawUrl: String): Bitmap? {
        if (!rawUrl.startsWith("http")) return null   
        val url = HfApi.cdnRewrite(rawUrl)
        val key = md5(url)
        mem.get(key)?.let { return it }
        failed[url]?.let { t -> if (System.currentTimeMillis() - t < FAIL_TTL) return null }
        var bmp = readDisk(key)
        if (bmp == null) {
            bmp = download(url)
            if (bmp != null) writeDisk(key, bmp) else failed[url] = System.currentTimeMillis()
        }
        bmp?.let { mem.put(key, it) }
        return bmp
    }

    private fun wrap(bmp: Bitmap, circular: Boolean): android.graphics.drawable.Drawable {
        val ctx = appCtx ?: return android.graphics.drawable.BitmapDrawable(bmp)
        return RoundedBitmapDrawableFactory.create(ctx.resources, bmp).apply {
            isCircular = circular
            setAntiAlias(true)
        }
    }

    private fun download(url: String): Bitmap? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", HfApi.UA)
        conn.connectTimeout = 8000
        conn.readTimeout = 12000
        BitmapFactory.decodeStream(conn.inputStream)
    } catch (_: Exception) {
        null
    }

    private fun cacheFile(key: String) = File(dir, key)

    private fun readDisk(key: String): Bitmap? = try {
        val f = cacheFile(key)
        if (f.exists() && f.length() > 0) {
            f.setLastModified(System.currentTimeMillis())
            BitmapFactory.decodeFile(f.absolutePath)
        } else null
    } catch (_: Exception) {
        null
    }

    private fun writeDisk(key: String, bmp: Bitmap) = try {
        cacheFile(key).outputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        trimIfNeeded()
    } catch (_: Exception) {
        
    }

    
    private fun trimIfNeeded() {
        try {
            val files = dir.listFiles() ?: return
            var total = files.sumOf { it.length() }
            if (total <= DIR_MAX) return
            files.sortedBy { it.lastModified() }.forEach { f ->
                if (total <= DIR_TRIM) return@forEach
                val len = f.length()
                if (f.delete()) total -= len
            }
        } catch (_: Exception) {
        }
    }

    private fun md5(s: String): String = try {
        MessageDigest.getInstance("MD5").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        s.hashCode().toString()
    }
}
