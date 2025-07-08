package com.lagradost

import com.fasterxml.jackson.databind.json.JsonMapper
import com.lagradost.api.Log
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element

class HdRezkaCoProvider : MainAPI() {

    private val movieSelector = "div.b-content__inline_items"
    private val titleSelector = "div.b-content__inline_item-link > a"
    private val posterUrlSelector = "div.b-content__inline_item  img"

    private val cloudflareKiller = CloudflareKiller()
    private val interceptor = CloudflareInterceptor(cloudflareKiller)
    private val mapper = JsonMapper.builder()
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .build()

    // Basic Info
    override var mainUrl = "https://hdrezka.co"
    override var name = "HdRezkaCo"
    override val hasMainPage = true
    override var lang = "uk"

    override val hasDownloadSupport = false

    override val supportedTypes = setOf(
        TvType.TvSeries,
        TvType.Cartoon,
        TvType.Movie,
        TvType.Anime
    )

    // Sections
    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Новинки",
        "$mainUrl/films/" to "Фільми",
        "$mainUrl/series/" to "Серіали",
        "$mainUrl/cartoons/" to "Мультфільми",
        "$mainUrl/animation/" to "Аніме"
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + page, interceptor = interceptor).document
        val first = document.select(movieSelector)
            .firstOrNull()


        return if (first == null) {
            newHomePageResponse(request.name, emptyList())
        } else {
            val mainPage = first.children().map {
                it.toSearchResponse()
            }.filter { !it.posterUrl.isNullOrEmpty() }
            newHomePageResponse(request.name, mainPage)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        Log.d("search","query: $query")
        val response =
            app.get("$mainUrl/search/?do=search&subaction=search&q=$query", interceptor = interceptor).document
        val map = response.select(movieSelector).map {
            it.toSearchResponse()
        }
        return map
    }

    override suspend fun load(url: String): LoadResponse {
        Log.d("DEBUG load", "Url: $url")

        return newMovieLoadResponse("title", url, TvType.Movie, url)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("DEBUG loadLinks", "Data: $data")

        return false
    }

    private fun Element.toSearchResponse(): MovieSearchResponse {
        val tittleNode = this.select(titleSelector)
        val title = tittleNode.text()
        val url = tittleNode.attr("href")
        val posterUrl = fixUrl(
            this.select(posterUrlSelector)
                .attr("src")
        )

        return newMovieSearchResponse(title, url, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }
}