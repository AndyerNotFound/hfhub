package com.hfhub.android.ui

import com.hfhub.android.MainActivity
import com.hfhub.android.data.HfApi
import com.hfhub.android.data.RepoCard


class ModelsFragment : BaseListFragment() {

    override fun kind() = "models"

    override fun sortOptions(): List<Pair<String, String>> = listOf(
        "downloads" to "下载最多",
        "likes" to "喜欢最多",
        "trendingScore" to "本周趋势",
        "lastModified" to "最近更新"
    )

    override suspend fun fetch(skip: Int): List<RepoCard> =
        HfApi.listModels(query, currentSort, skip, activeFilters).map { m ->
            RepoCard(
                kind = "models",
                id = m.id,
                downloads = m.downloads,
                likes = m.likes,
                gated = m.gated,
                badge = m.pipeline_tag ?: m.library_name,
                desc = null,
                date = m.createdAt.orEmpty()
            )
        }

    override fun openDetail(id: String) {
        openDetail(kind(), id)
    }
}
