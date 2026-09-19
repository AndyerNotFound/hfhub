package com.hfhub.android.util









object Markdown {

    private const val MAX_LINES = 6000
    private const val MAX_OUT = 400_000

    data class Table(val header: List<String>, val rows: List<List<String>>)

    data class Result(val html: String, val tables: List<Table>)

    fun render(md: String): Result {
        if (md.isBlank()) return Result("", emptyList())
        val sb = StringBuilder(md.length + 1024)
        val tables = mutableListOf<Table>()
        
        val folded = md.replace(Regex("<[^>\\n]*\\n[^>\\n]*>")) { it.value.replace('\n', ' ') }
        val lines = folded.lineSequence().take(MAX_LINES).toList()
        var i = 0
        
        if (lines.isNotEmpty() && lines[0].trim() == "---") {
            val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (end in 0..200) i = end + 2
        }
        var inCode = false
        var inUl = false
        var inOl = false
        var paragraph = StringBuilder()

        fun closeLists() {
            if (inUl) { sb.append("</ul>\n"); inUl = false }
            if (inOl) { sb.append("</ol>\n"); inOl = false }
        }

        fun flushPara(closeList: Boolean = true) {
            if (paragraph.isNotEmpty()) {
                sb.append("<p>").append(paragraph).append("</p>\n")
                paragraph = StringBuilder()
            }
            if (closeList) closeLists()
        }

        while (i < lines.size) {
            val line = lines[i]
            if (sb.length > MAX_OUT) break

            
            if (line.trimStart().startsWith("```")) {
                flushPara()
                if (!inCode) {
                    inCode = true
                    sb.append("<pre><code>")
                } else {
                    inCode = false
                    sb.append("</code></pre>\n")
                }
                i++
                continue
            }
            if (inCode) {
                sb.append(escape(line)).append('\n')
                i++
                continue
            }

            val t = preprocessHtml(line).trim()

            
            if (t.startsWith("|") && t.endsWith("|") &&
                i + 1 < lines.size && isTableSeparator(preprocessHtml(lines[i + 1]).trim())
            ) {
                flushPara()
                val rows = mutableListOf<List<String>>()
                var j = i
                while (j < lines.size) {
                    val tj = preprocessHtml(lines[j]).trim()
                    if (!tj.startsWith("|") || !tj.endsWith("|")) break
                    if (!isTableSeparator(tj)) rows.add(parseRow(tj))
                    j++
                }
                if (rows.isNotEmpty()) {
                    tables.add(Table(rows.first(), rows.drop(1)))
                    sb.append('\u0001').append('T').append(tables.size - 1).append('\u0001')
                }
                i = j
                continue
            }

            when {
                t.isEmpty() -> { flushPara(); i++ }
                t.startsWith("#### ") -> { flushPara(); sb.append("<h4>").append(inline(t.drop(5))).append("</h4>\n"); i++ }
                t.startsWith("### ") -> { flushPara(); sb.append("<h3>").append(inline(t.drop(4))).append("</h3>\n"); i++ }
                t.startsWith("## ") -> { flushPara(); sb.append("<h2>").append(inline(t.drop(3))).append("</h2>\n"); i++ }
                t.startsWith("# ") -> { flushPara(); sb.append("<h1>").append(inline(t.drop(2))).append("</h1>\n"); i++ }
                t == "---" || t == "***" || t == "___" -> { flushPara(); sb.append("<hr/>\n"); i++ }
                t.startsWith("> ") -> { flushPara(); sb.append("<blockquote>").append(inline(t.drop(2))).append("</blockquote>\n"); i++ }
                t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ") -> {
                    flushPara(closeList = false)
                    if (inOl) { sb.append("</ol>\n"); inOl = false }
                    if (!inUl) { sb.append("<ul>\n"); inUl = true }
                    sb.append("<li>").append(taskMarker(t.drop(2))).append("</li>\n")
                    i++
                }
                Regex("^\\d+\\.\\s").containsMatchIn(t) -> {
                    flushPara(closeList = false)
                    if (inUl) { sb.append("</ul>\n"); inUl = false }
                    if (!inOl) { sb.append("<ol>\n"); inOl = true }
                    sb.append("<li>").append(inline(t.substringAfter('.').trim())).append("</li>\n")
                    i++
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(inline(t))
                    i++
                }
            }
        }
        if (inCode) sb.append("</code></pre>\n")
        flushPara()
        return Result(sb.toString().take(MAX_OUT), tables)
    }

    
    private fun taskMarker(s: String): String = when {
        s.startsWith("[x] ") || s.startsWith("[X] ") -> "☑ " + inline(s.drop(4))
        s.startsWith("[ ] ") -> "☐ " + inline(s.drop(4))
        else -> inline(s)
    }

    private fun isTableSeparator(t: String): Boolean =
        t.startsWith("|") && t.endsWith("|") && t.count { it == '-' } >= 2 &&
            t.all { it == '|' || it == '-' || it == ':' || it == ' ' }

    private fun parseRow(t: String): List<String> =
        t.trim('|').split('|').map { it.trim() }

    
    private fun inline(s: String): String {
        var r = escape(s.trim())
        
        val codes = mutableListOf<String>()
        r = Regex("`([^`]*)`").replace(r) { m ->
            codes.add(m.groupValues[1])
            "\u0000${codes.size - 1}\u0000"
        }
        
        r = Regex("!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)").replace(r) { m ->
            "<img src=\"${m.groupValues[2].replace("&amp;", "&").replace("\"", "%22")}\" alt=\"${m.groupValues[1]}\">"
        }
        r = Regex("\\[([^\\]]+)\\]\\(([^)\\s]+)\\)").replace(r) {
            "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>"
        }
        r = Regex("\\*\\*(.+?)\\*\\*").replace(r) { "<b>${it.groupValues[1]}</b>" }
        r = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)").replace(r) { "<i>${it.groupValues[1]}</i>" }
        r = Regex("~~(.+?)~~").replace(r) { "<s>${it.groupValues[1]}</s>" }
        codes.forEachIndexed { idx, c ->
            r = r.replace("\u0000$idx\u0000", "<code>${escape(c)}</code>")
        }
        return r
    }

    



    private fun preprocessHtml(s: String): String {
        var r = s
        r = r.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        r = r.replace(
            Regex("<a[^>]*href=['\"]([^'\"]*)['\"][^>]*>(.*?)</a>", RegexOption.IGNORE_CASE)
        ) { m ->
            val text = m.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
            val href = m.groupValues[1].trim()
            if (href.isEmpty()) text else "[" + text.ifEmpty { href } + "](" + href + ")"
        }
        r = r.replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE)) { m ->
            val src = Regex("src=['\"]([^'\"]*)['\"]").find(m.value)?.groupValues?.get(1).orEmpty()
            val alt = Regex("alt=['\"]([^'\"]*)['\"]").find(m.value)?.groupValues?.get(1).orEmpty()
            if (src.isNotEmpty()) "![" + alt + "](" + src + ")" else alt
        }
        r = r.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
        r = r.replace(Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE), "")
        r = r.replace(Regex("<[^>]+>"), "")
        return r
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
