package com.hfhub.android.ui

import com.hfhub.android.MainActivity
import com.hfhub.android.data.HfApi
import com.hfhub.android.data.RepoCard


class DatasetsFragment : BaseListFragment() {

    override fun kind() = "datasets"

    override fun sortOptions(): List<Pair<String, String>> = listOf(
        "downloads" to "下载最多",
        "likes" to "喜欢最多",
        "lastModified" to "最近更新"
    )

    override suspend fun fetch(skip: Int): List<RepoCard> =
        HfApi.listDatasets(query, currentSort, skip, activeFilters).map { d ->
            RepoCard(
                kind = "datasets",
                id = d.id,
                downloads = d.downloads,
                likes = d.likes,
                gated = d.gated,
                badge = null,
                desc = d.description?.trim()?.replace(Regex("\\s+"), " "),
                date = d.lastModified.orEmpty()
            )
        }

    override fun openDetail(id: String) {
        openDetail(kind(), id)
    }
}
