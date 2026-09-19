package com.hfhub.android.util








object Links {

    
    data class Target(val type: String, val kind: String, val id: String)

    
    private val RESERVED = setOf(
        "models", "datasets", "spaces", "papers", "docs", "blog", "join", "login",
        "logout", "settings", "organizations", "users", "tasks", "languages",
        "licenses", "pricing", "enterprise", "new", "search", "notifications",
        "following", "followers", "collections", "api", "audit", "billing",
        "charts", "oauth", "privacy", "terms", "trust", "posts", "highlights",
        "discussions", "assistants", "benchmarks", "keras", "course", "learn"
    )

    fun parse(text: String): Target? {
        val t = text.trim()
        val m = Regex("^https?://(?:www\\.)?(?:huggingface\\.co|hf-mirror\\.com)/(.+)$", RegexOption.IGNORE_CASE)
            .find(t) ?: return null
        var path = m.groupValues[1]
        path = path.substringBefore('?').substringBefore('#').trim('/')
        if (path.isEmpty()) return null
        var kind = "models"
        when {
            path.startsWith("datasets/") -> { kind = "datasets"; path = path.removePrefix("datasets/") }
            path.startsWith("spaces/") -> { kind = "spaces"; path = path.removePrefix("spaces/") }
            path.startsWith("models/") -> { kind = "models"; path = path.removePrefix("models/") }
        }
        val parts = path.split("/").filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        if (parts[0].lowercase() in RESERVED) return null
        return if (parts.size == 1) {
            Target("user", "", parts[0])
        } else {
            Target("repo", kind, parts[0] + "/" + parts[1])
        }
    }
}
