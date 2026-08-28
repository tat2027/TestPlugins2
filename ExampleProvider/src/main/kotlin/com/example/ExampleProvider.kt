package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://raw.githubusercontent.com"
    override var name = "My IPTV List"
    override var supportedTypes = setOf(TvType.Live)
    override var lang = "ur"
    override val hasMainPage = true

    private val rawPlaylistUrl = "https://raw.githubusercontent.com/tat2027/TV1/main/INDIA.m3u"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = app.get(rawPlaylistUrl).text
        val channels = mutableListOf<SearchResponse>()
        val lines = response.lines()

        var currentTitle = ""
        var currentPoster = ""

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#EXTINF:")) {
                currentTitle = trimmedLine.substringAfter(",").trim()
                currentPoster = if (trimmedLine.contains("tvg-logo=\"")) {
                    trimmedLine.substringAfter("tvg-logo=\"").substringBefore("\"")
                } else {
                    ""
                }
            } else if (trimmedLine.startsWith("http://") || trimmedLine.startsWith("https://")) {
                if (currentTitle.isNotEmpty()) {
                    val liveResponse = newLiveSearchResponse(
                        name = currentTitle,
                        url = trimmedLine,
                        type = TvType.Live
                    ) {
                        this.posterUrl = currentPoster.ifEmpty { null }
                    }
                    channels.add(liveResponse)
                    
                    currentTitle = ""
                    currentPoster = ""
                }
            }
        }
        return newHomePageResponse(name, channels)
    }

    override suspend fun load(url: String): LoadResponse {
        return newLiveStreamLoadResponse(
            name = name,
            url = url,
            type = TvType.Live
        ) {
            this.dataUrl = url
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback(
            newExtractorLink(
                name = name,
                source = name,
                url = data
            )
        )
        return true
    }
}
