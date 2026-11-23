package com.example.data.network

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object RssParser {
    fun parse(xml: String, feedTitle: String, limit: Int = 8): List<RssArticle> {
        val articles = mutableListOf<RssArticle>()
        runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var insideEntry = false
            var title = ""
            var link = ""
            var summary = ""
            var pubDate = ""

            while (eventType != XmlPullParser.END_DOCUMENT && articles.size < limit) {
                val tag = parser.name?.lowercase()
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "item", "entry" -> {
                                insideEntry = true
                                title = ""
                                link = ""
                                summary = ""
                                pubDate = ""
                            }
                            "title" -> if (insideEntry) title = readSafeText(parser)
                            "link" -> if (insideEntry) {
                                val href = parser.getAttributeValue(null, "href")
                                link = if (!href.isNullOrBlank()) href else readSafeText(parser)
                            }
                            "description", "summary", "content" -> if (insideEntry && summary.isBlank()) {
                                summary = stripHtml(readSafeText(parser))
                            }
                            "pubdate", "published", "updated" -> if (insideEntry && pubDate.isBlank()) {
                                pubDate = readSafeText(parser)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ((tag == "item" || tag == "entry") && insideEntry) {
                            insideEntry = false
                            if (title.isNotBlank()) {
                                articles.add(
                                    RssArticle(
                                        title = title.trim(),
                                        link = link.trim(),
                                        summary = summary.trim().take(220),
                                        publishedAt = pubDate.trim(),
                                        feedTitle = feedTitle
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return articles
    }

    private fun readSafeText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }

    private fun stripHtml(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
