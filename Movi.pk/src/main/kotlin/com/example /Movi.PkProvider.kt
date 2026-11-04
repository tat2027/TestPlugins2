package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class MoviPk : MainAPI() {
    override var mainUrl = "https://movi.pk"
    override var name = "Movi.pk"
    override val hasMainPage = true
    override var lang = "ur"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama
    )

    // Homepage categories
    override val mainPage = mainPageOf(
        "$mainUrl/category/pakistani-movies/page/" to "Pakistani Movies",
        "$mainUrl/category/bollywood-movies/page/" to "Bollywood Movies",
        "$mainUrl/category/hollywood-movies/page/" to "Hollywood Movies",
        "$mainUrl/category/punjabi-movies/page/" to "Punjabi Movies",
        "$mainUrl/page/" to "Latest Releases"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "${request.data}$page"
        val document = app.get(url).document
        
        // Match items with common WordPress/streaming selectors
        val home = document.select("article.item, div.movie-card, .post-item, .item-video").mapNotNull {
            it.toSearchResult()
        }
        
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title, h2.entry-title, .movie-title, .entry-header h2")?.text()?.trim() 
            ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.getImageUrl())

        val quality = this.selectFirst(".quality, .badge-quality, .status")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            addQuality(quality)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=${query.encodeUrl()}"
        val document = app.get(searchUrl).document

        return document.select("article.item, div.movie-card, .search-item, .post-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title, .movie-info h1, h1")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:title]")?.attr("content")
            ?: "Unknown Title"

        val poster = fixUrlNull(
            document.selectFirst("meta[property=og:image]")?.attr("content")
                ?: document.selectFirst(".poster img, .movie-thumbnail img, .entry-content img")?.getImageUrl()
        )

        val plot = document.selectFirst(".entry-content p, .overview, .movie-description, #synopsis")?.text()?.trim()
        val year = document.selectFirst(".release-year, .year, .date")?.text()?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
        val tags = document.select(".genres a, .categories a, .s-genres a").map { it.text().trim() }

        // Check for episodic structure (TV series / dramas)
        val episodes = document.select(".episodes-list a, .episode-item, .episodios a").mapIndexed { index, ep ->
            val epHref = fixUrlNull(ep.attr("href")) ?: ""
            val epName = ep.text().trim().ifBlank { "Episode ${index + 1}" }
            Episode(
                data = epHref,
                name = epName,
                episode = index + 1
            )
        }

        if (episodes.isNotEmpty()) {
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
                this.tags = tags
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
            this.year = year
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val links = mutableListOf<String>()

        // 1. Collect embed iframes
        document.select("iframe[src]").forEach { iframe ->
            val src = fixUrlNull(iframe.attr("src"))
            if (!src.isNullOrBlank() && !src.contains("recaptcha") && !src.contains("facebook")) {
                links.add(src)
            }
        }

        // 2. Collect download / direct watch buttons
        document.select(".download-links a, .stream-links a, a.download-btn, a[href*='stream'], a[href*='embed']").forEach { a ->
            val href = fixUrlNull(a.attr("href"))
            if (!href.isNullOrBlank()) {
                links.add(href)
            }
        }

        // 3. Resolve through Cloudstream universal extractor registry
        links.distinct().forEach { link ->
            loadExtractor(link, mainUrl, subtitleCallback, callback)
        }

        return true
    }

    private fun Element.getImageUrl(): String? {
        return this.attr("data-src").ifBlank {
            this.attr("data-lazy-src").ifBlank {
                this.attr("srcset").substringBefore(" ").ifBlank {
                    this.attr("src")
                }
            }
        }
    }
}

