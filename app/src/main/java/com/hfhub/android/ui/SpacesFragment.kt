package com.hfhub.android.ui

import com.hfhub.android.MainActivity
import com.hfhub.android.data.HfApi
import com.hfhub.android.data.RepoCard


class SpacesFragment : BaseListFragment() {

    override fun kind() = "spaces"

    override fun sortOptions(): List<Pair<String, String>> = listOf(
        "likes" to "喜欢最多",
        "lastModified" to "最近更新"
    )

    override suspend fun fetch(skip: Int): List<RepoCard> =
        HfApi.listSpaces(query, currentSort, skip).map { s ->
            RepoCard(
                kind = "spaces",
                id = s.id,
                downloads = 0,
                likes = s.likes,
                gated = false,
                badge = s.sdk,
                desc = null,
                date = s.createdAt.orEmpty()
            )
        }

    override fun openDetail(id: String) {
        openDetail(kind(), id)
    }
}
