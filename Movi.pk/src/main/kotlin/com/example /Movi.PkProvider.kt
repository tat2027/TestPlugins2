package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URLEncoder

class MoviPkProvider : MainAPI() {
    override var mainUrl = "https://movi.pk"
    override var name = "Movi.pk"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = mutableListOf<HomePageList>()
        
        val elements = document.select("div.item")
        val list = elements.mapNotNull { it.toSearchResult() }
        
        if (list.isNotEmpty()) {
            home.add(HomePageList("Trending", list))
        }
        
        // Fixed deprecated HomePageResponse constructor using newHomePageResponse
        return newHomePageResponse(home, false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "$mainUrl/?s=$encodedQuery"
        val document = app.get(searchUrl).document
        
        return document.select("div.item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: "No Title"
        val poster = fixUrlNull(document.selectFirst("img.poster")?.attr("src"))
        val description = document.selectFirst("div.desc")?.text()
        
        // Fixed newEpisode syntax matching current Cloudstream version
        val episodes = listOf(
            newEpisode(url) {
                this.name = "Episode 1"
                this.description = description
            }
        )

        return newMovieLoadResponse(title, url, TvType.Movie, episodes) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Add your link extraction logic here
        return true
    }
}
