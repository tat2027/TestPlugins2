package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class MoviPk : MainAPI() {
    override var mainUrl = "https://www.movi.pk"
    override var name = "Movi.pk"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "hi"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val homeRows = mutableListOf<HomePageList>()

        // Homepage se movie items select karna
        val list = document.select("div.items.row div.item, div.col-lg-2, div.col-md-3").mapNotNull { el ->
            toResult(el)
        }

        if (list.isNotEmpty()) {
            homeRows.add(HomePageList("Featured Movies", list))
        }

        return HomePageResponse(homeRows)
    }

    private fun toResult(element: Element): SearchResponse? {
        val title = element.selectFirst("a")?.attr("title") ?: element.selectFirst(".title, h3, h2")?.text() ?: return null
        val url = fixUrl(element.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(element.selectFirst("img")?.attr("data-src") ?: element.selectFirst("img")?.attr("src") ?: "")

        return newMovieSearchResponse(title, url, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=$query"
        val document = app.get(searchUrl).document

        return document.select("div.item, div.col-lg-2").mapNotNull { el ->
            toResult(el)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1, .movie-title")?.text() ?: "No Title"
        val poster = fixUrl(document.selectFirst(".poster img, .movie-poster img")?.attr("src") ?: "")
        val description = document.selectFirst(".description, .synopsis, .entry-content")?.text()
        val year = document.selectFirst(".year")?.text()?.toIntOrNull()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Watch page se iframe ya video links nikalna
        document.select("iframe, source").forEach { element ->
            val link = element.attr("src").ifEmpty { element.attr("data-src") }
            if (link.isNotEmpty()) {
                loadExtractor(link, data, subtitleCallback, callback)
            }
        }
        
        return true
    }
}
