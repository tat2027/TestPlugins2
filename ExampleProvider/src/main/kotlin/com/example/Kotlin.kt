package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class ExampleProvider : MainProvider() {
    override var mainUrl = "https://raw.githubusercontent.com"
    override var name = "My IPTV List"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "ur"
    override val hasMainPage = true

    // 1. Apni Raw JSON ya M3U file ka Raw Link yahan daalein
    private val rawPlaylistUrl = "https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/playlist.m3u"

    override async fun getMainPage(page: Int, request: ProviderRequest): HomePageResponse? {
        // Raw Link se data fetch karna
        val response = app.get(rawPlaylistUrl).text
        
        // Basic M3U Parsing Logic
        val channels = mutableListOf<SearchResponse>()
        val lines = response.lines()
        var currentTitle = ""

        for (line in lines) {
            if (line.startsWith("#EXTINF:")) {
                currentTitle = line.substringAfterLast(",")
            } else if (line.startsWith("http")) {
                val streamUrl = line.trim()
                channels.add(
                    LiveSearchResponse(
                        name = if (currentTitle.isNotEmpty()) currentTitle else "Live Stream",
                        url = streamUrl,
                        apiName = this.name,
                        type = TvType.Live,
                        posterUrl = "https://via.placeholder.com/500"
                    )
                )
                currentTitle = ""
            }
        }

        return newHomePageResponse(listOf(HomePageList("IPTV Channels", channels)), hasNext = false)
    }

    override async fun search(query: String): List<SearchResponse> {
        return emptyList()
    }

    override async fun load(url: String): LoadResponse {
        return LiveStreamLoadResponse(
            name = "Live Channel",
            url = url,
            apiName = this.name,
            dataUrl = url
        )
    }

    override async fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Stream Direct Play karna
        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = "Direct Stream",
                url = data, // Target stream link
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8")
            )
        )
        return true
    }
}
