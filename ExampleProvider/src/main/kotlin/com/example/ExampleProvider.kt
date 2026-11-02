package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://www.movi.pk"
    override var name = "Movi.pk"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)
    override var lang = "hi"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Featured Movies",
        "$mainUrl/genre/action/" to "Action Movies",
        "$mainUrl/genre/crime/" to "Crime Movies",
        "$mainUrl/genre/indian-movies/" to "Indian Movies",
        "$mainUrl/genre/hollywood-hindi-dubbed-movies/" to "Hollywood Hindi Dubbed",
        "$mainUrl/genre/hollywood-movies/" to "Hollywood Movies",
        "$mainUrl/genre/south-hindi/" to "South Hindi",
        "$mainUrl/genre/south-indian-hindi-dubbed-movies/" to "South Indian Hindi Dubbed",
        "$mainUrl/genre/tv-series/" to "TV Series",
        "$mainUrl/genre/web-series-hindi-dubbed/" to "Web Series Hindi Dubbed",
        "$mainUrl/genre/punjabi/" to "Punjabi Movies",
        "$mainUrl/genre/lollywood-movies/" to "Lollywood Movies",
        "$mainUrl/genre/anime/" to "Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document

        val list = document.select("div.movies-list.movies-list-full div.item, div.items.row div.item, div.col-lg-2").mapNotNull { el ->
            toResult(el)
        }

        return newHomePageResponse(request.name, list)
    }

    private fun toResult(element: Element): SearchResponse? {
        val title = element.selectFirst("a")?.attr("title") ?: element.selectFirst(".title, h3, h2, .ml-item-title")?.text() ?: return null
        val url = fixUrl(element.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(element.selectFirst("img")?.attr("data-src") ?: element.selectFirst("img")?.attr("src") ?: "")
        
        val isAnime = url.contains("anime", ignoreCase = true) || title.contains("anime", ignoreCase = true)
        val isSeries = isAnime || url.contains("series") || title.contains("Season", ignoreCase = true) || url.contains("web-series") || title.contains("Complete", ignoreCase = true)
        
        val tvType = if (isAnime) TvType.Anime else if (isSeries) TvType.TvSeries else TvType.Movie

        return when (tvType) {
            TvType.Anime -> newAnimeSearchResponse(title, url, tvType) { this.posterUrl = posterUrl }
            TvType.TvSeries -> newTvSeriesSearchResponse(title, url, tvType) { this.posterUrl = posterUrl }
            else -> newMovieSearchResponse(title, url, tvType) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=$query"
        val document = app.get(searchUrl).document

        return document.select("div.movies-list.movies-list-full div.item, div.item, div.col-lg-2").mapNotNull { el ->
            toResult(el)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1, .movie-title")?.text() ?: "No Title"
        val poster = fixUrl(document.selectFirst(".poster img, .movie-poster img, .mvi-cover img")?.attr("src") ?: "")
        val description = document.selectFirst(".description, .synopsis, .entry-content, .mvi-content")?.text()
        val year = document.selectFirst(".year")?.text()?.toIntOrNull()

        val isAnime = url.contains("anime", ignoreCase = true)
        val isSeries = isAnime || url.contains("series") || title.contains("Season", ignoreCase = true) || url.contains("web-series") || title.contains("Complete", ignoreCase = true)

        val episodes = mutableListOf<Episode>()
        document.select("div.les-content a, div.episode-list a").forEachIndexed { index, el ->
            val epUrl = fixUrl(el.attr("href"))
            val epTitle = el.text().ifEmpty { "Episode ${index + 1}" }
            episodes.add(newEpisode(epUrl) {
                this.name = epTitle
                this.episode = index + 1
            })
        }

        return if (isAnime) {
            newAnimeLoadResponse(title, url, TvType.Anime, false) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.episodes = mutableMapOf(DubStatus.Dub to episodes)
            }
        } else if (isSeries) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, false) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.episodes = mutableMapOf(DubStatus.Dub to episodes)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("iframe, source, embed").forEach { element ->
            val link = element.attr("src").ifEmpty { element.attr("data-src") }
            if (link.isNotEmpty()) {
                loadExtractor(link, data, subtitleCallback, callback)
            }
        }
        
        return true
    }
}
