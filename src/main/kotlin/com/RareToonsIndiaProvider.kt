package com.raretoonsindia

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class RareToonsIndiaProvider : MainAPI() {
    override var mainUrl = "https://raretoonsindia.rtilinks.com"
    override var name = "Rare Toons India"
    override val supportedTypes = setOf(TvType.Cartoon, TvType.Anime)

    override var lang = "hi"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: ProviderData): HomePageResponse {
        val url = if (page <= 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(url).document
        val items = parsePosts(document)
        return newHomePageResponse(listOf(HomePageList("Latest Anime & Cartoons", items)), hasMore = true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return parsePosts(document)
    }

    private fun parsePosts(document: Document): List<SearchResponse> {
        val items = mutableListOf<SearchResponse>()
        document.select("article, .post-item, .entry-title").forEach { element ->
            val titleElement = element.selectFirst("a")
            val title = titleElement?.text()?.trim() ?: ""
            val href = titleElement?.attr("href") ?: ""
            val poster = element.selectFirst("img")?.attr("src") ?: element.selectFirst("img")?.attr("data-src")

            if (title.isNotEmpty() && href.isNotEmpty()) {
                items.add(newAnimeSearchResponse(title, href, TvType.Cartoon) {
                    this.posterUrl = poster
                })
            }
        }
        return items
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title, h1")?.text()?.trim() ?: "RareToons Episode"
        val poster = document.selectFirst(".entry-content img")?.attr("src")

        val episodeLinks = mutableListOf<Episode>()
        document.select(".entry-content a[href*='drive'], .entry-content a[href*='stream']").forEachIndexed { index, el ->
            val epName = el.text().ifEmpty { "Episode ${index + 1}" }
            val epUrl = el.attr("href")
            episodeLinks.add(newEpisode(epUrl) {
                this.name = epName
            })
        }

        return newAnimeLoadResponse(title, url, TvType.Cartoon) {
            this.posterUrl = poster
            this.addEpisodes(DubStatus.Subbed, episodeLinks)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.isNotEmpty()) {
            callback.invoke(
                ExtractorLink(
                    source = name,
                    name = "RareToons Server",
                    url = data,
                    referer = mainUrl,
                    quality = Qualities.P720.value,
                    isM3u8 = data.contains(".m3u8")
                )
            )
            return true
        }
        return false
    }
}
