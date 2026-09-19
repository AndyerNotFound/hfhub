package com.hfhub.android.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit









object HfApi {

    const val UA = "LesbianHub/0.1 (Android)"
    const val DEFAULT_BASE = "https://hf-mirror.com"
    const val OFFICIAL_BASE = "https://huggingface.co"
    const val PAGE_LIMIT = 20

    val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun base(): String = SettingsStore.baseUrl.ifBlank { DEFAULT_BASE }

    
    private fun seg(kind: String): String = when (kind) {
        "models", "datasets", "spaces" -> kind
        else -> "models"
    }

    




    private fun webSeg(kind: String): String =
        if (kind == "models") "" else "/${seg(kind)}"

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(base() + path)
            .header("User-Agent", UA)
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${body.take(120)}")
            }
            body
        }
    }

    private suspend inline fun <reified T> getJson(path: String): T {
        val body = get(path)
        val t = object : TypeToken<T>() {}.type
        return gson.fromJson(body, t)
    }

    private suspend inline fun <reified T> getJsonList(path: String): List<T> {
        val body = get(path)
        val t = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(body, t)
    }

    

    private fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8")

    private fun queryOf(search: String, filters: List<String>, author: String? = null): String {
        val sb = StringBuilder()
        if (search.isNotBlank()) sb.append("&search=").append(enc(search.trim()))
        if (!author.isNullOrBlank()) sb.append("&author=").append(enc(author.trim()))
        filters.forEach { f ->
            if (f.isBlank()) return@forEach
            val v = f.trim()
            
            if (v.startsWith("params:")) {
                sb.append("&num_parameters=").append(enc(v.removePrefix("params:")))
            } else {
                sb.append("&filter=").append(enc(v))
            }
        }
        return sb.toString()
    }

    suspend fun listModels(search: String, sort: String, skip: Int, filters: List<String> = emptyList(), author: String? = null): List<ModelInfo> =
        getJsonList("/api/models?limit=$PAGE_LIMIT&skip=$skip&sort=$sort" + queryOf(search, filters, author))

    suspend fun listDatasets(search: String, sort: String, skip: Int, filters: List<String> = emptyList(), author: String? = null): List<DatasetInfo> =
        getJsonList("/api/datasets?limit=$PAGE_LIMIT&skip=$skip&sort=$sort" + queryOf(search, filters, author))

    suspend fun listSpaces(search: String, sort: String, skip: Int, author: String? = null): List<SpaceInfo> =
        getJsonList("/api/spaces?limit=$PAGE_LIMIT&skip=$skip&sort=$sort" + queryOf(search, emptyList(), author))

    
    suspend fun owner(name: String): OwnerInfo? {
        for (prefix in listOf("/api/organizations/", "/api/users/")) {
            try {
                val body = get(prefix + name.trim() + "/overview")
                val t = body.trimStart()
                if (t.startsWith("{") && !t.contains("\"error\"")) {
                    val info = gson.fromJson(body, OwnerInfo::class.java)
                    if (info != null) {
                        return info.copy(avatarUrl = info.avatarUrl?.let { cdnRewrite(it) })
                    }
                }
            } catch (_: Exception) {
                
            }
        }
        return null
    }

    suspend fun papers(): List<DailyPaper> = getJsonList("/api/daily_papers")

    

    suspend fun detail(kind: String, id: String): RepoDetail {
        val body = get("/api/${seg(kind)}/$id")
        
        if (!body.trimStart().startsWith("{")) {
            throw IOException("该数据源不支持此详情接口（返回了非 JSON 内容）")
        }
        return gson.fromJson(body, RepoDetail::class.java)
    }

    
    suspend fun detailOrNull(kind: String, id: String): RepoDetail? =
        try { detail(kind, id) } catch (e: Exception) { null }

    suspend fun tree(kind: String, id: String, subdir: String = ""): List<FileEntry> {
        val sub = if (subdir.isBlank()) "" else "/" + subdir.trim('/')
        return getJsonList("/api/${seg(kind)}/$id/tree/main$sub")
    }

    
    suspend fun readme(kind: String, id: String): String = try {
        get("${webSeg(kind)}/$id/raw/main/README.md")
    } catch (e: IOException) {
        
        ""
    }

    
    fun resolveUrl(kind: String, id: String, path: String): String =
        "${base()}/${seg(kind)}/$id/resolve/main/${path.trimStart('/')}"


    

    



    suspend fun quicksearch(q: String): QuickSearch {
        val body = get("/api/quicksearch?q=" + enc(q) + "&limit=5")
        return gson.fromJson(body, QuickSearch::class.java) ?: QuickSearch()
    }

    

    


    fun cdnRewrite(url: String): String {
        if (!base().contains("hf-mirror")) return url
        return url
            .replace("cdn-avatars.huggingface.co", "cdn-avatars.hf-mirror.com")
            .replace("cdn-thumbnails.huggingface.co", "cdn-thumbnails.hf-mirror.com")
            .replace("cdn-uploads.huggingface.co", "cdn-uploads.hf-mirror.com")
    }

    



    suspend fun avatarUrl(author: String): String? {
        if (author.isBlank()) return null
        for (prefix in listOf("/api/organizations/", "/api/users/")) {
            try {
                val body = get(prefix + author.trim() + "/avatar")
                if (body.trimStart().startsWith("{")) {
                    val obj = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                    val url = obj?.get("avatarUrl")?.asString
                    if (!url.isNullOrBlank()) return cdnRewrite(url)
                }
            } catch (_: Exception) {
                
            }
        }
        return null
    }

    
    fun webUrl(kind: String, id: String): String =
        "${base()}/${seg(kind)}/$id"

    
    fun paperUrl(paperId: String): String = "${base()}/papers/$paperId"
}
