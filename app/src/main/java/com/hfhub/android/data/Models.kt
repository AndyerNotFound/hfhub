package com.hfhub.android.data

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


data class ModelInfo(
    val id: String = "",
    val downloads: Long = 0,
    val likes: Long = 0,
    val tags: List<String> = emptyList(),
    val pipeline_tag: String? = null,
    val library_name: String? = null,
    val createdAt: String? = null,
    val `private`: Boolean = false,
    val trendingScore: Double? = null,
    val gated: Boolean = false
)


data class DatasetInfo(
    val id: String = "",
    val author: String? = null,
    val downloads: Long = 0,
    val likes: Long = 0,
    val gated: Boolean = false,
    val `private`: Boolean = false,
    val lastModified: String? = null,
    val tags: List<String> = emptyList(),
    val description: String? = null
)


data class SpaceInfo(
    val id: String = "",
    val likes: Long = 0,
    val sdk: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String? = null,
    val `private`: Boolean = false
)


data class DailyPaper(
    val title: String? = null,
    val summary: String? = null,
    val publishedAt: String? = null,
    val numComments: Long = 0,
    val thumbnail: String? = null,
    val paper: PaperInfo? = null,
    val submittedBy: PaperSubmitter? = null,
    val organization: PaperOrg? = null
)

data class PaperInfo(
    val id: String? = null,
    val authors: List<PaperAuthor> = emptyList()
)

data class PaperAuthor(val name: String? = null)

data class PaperSubmitter(
    val fullname: String? = null,
    val avatarUrl: String? = null
)

data class PaperOrg(
    val name: String? = null,
    val fullname: String? = null
)


data class RepoDetail(
    val id: String = "",
    val downloads: Long = 0,
    val likes: Long = 0,
    val gated: Boolean = false,
    val `private`: Boolean = false,
    val lastModified: String? = null,
    val tags: List<String> = emptyList(),
    val library_name: String? = null,
    val pipeline_tag: String? = null,
    val sdk: String? = null,
    val author: String? = null,
    val description: String? = null,
    val safetensors: Safetensors? = null,
    val siblings: List<FileEntry> = emptyList(),
    val usedStorage: Long = 0,
    val cardData: JsonObject? = null
)

data class Safetensors(
    val total: Long = 0,
    val parameters: Map<String, Long> = emptyMap()
)


data class FileEntry(
    @SerializedName(value = "path", alternate = ["rfilename"])
    val path: String = "",
    val type: String = "file",
    val size: Long = 0,
    val oid: String? = null
)


data class RepoCard(
    val kind: String,
    val id: String,
    val downloads: Long,
    val likes: Long,
    val gated: Boolean,
    val badge: String?,
    val desc: String?,
    val date: String?
)


data class QuickSearch(
    val models: List<QuickItem> = emptyList(),
    val modelsCount: Long = 0,
    val datasets: List<QuickItem> = emptyList(),
    val datasetsCount: Long = 0,
    val spaces: List<QuickItem> = emptyList(),
    val spacesCount: Long = 0
)

data class QuickItem(val id: String = "")


data class OwnerInfo(
    val name: String = "",
    val fullname: String? = null,
    val avatarUrl: String? = null,
    val plan: String? = null,
    val numUsers: Long = 0,
    val numModels: Long = 0,
    val numDatasets: Long = 0,
    val numSpaces: Long = 0,
    val numPapers: Long = 0,
    val numFollowers: Long = 0
)
