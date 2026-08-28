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

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#EXTINF:")) {
                currentTitle = trimmedLine.substringAfter(",").trim()
            } else if (trimmedLine.startsWith("http://") || trimmedLine.startsWith("https://")) {
                if (currentTitle.isNotEmpty()) {
                    channels.add(
                        newLiveSearchResponse(
                            currentTitle,
                            trimmedLine,
                            TvType.Live
                        )
                    )
                }
            }
        }
        return newHomePageResponse(name, channels)
    }
}
