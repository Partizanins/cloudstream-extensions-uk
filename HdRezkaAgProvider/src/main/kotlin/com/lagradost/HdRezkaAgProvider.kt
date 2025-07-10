package com.lagradost

import com.fasterxml.jackson.databind.json.JsonMapper
import com.lagradost.api.Log
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.nicehttp.NiceResponse
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.absoluteValue

class HdRezkaAgProvider : MainAPI() {

    private val headers =
        mapOf("user-agent" to "Mozilla/5.0 (Windows NT 10.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36")


    private val dubList =
        listOf(
            "украинский (1+1)",
            "оригинал (+субтитры)",
            "украинский",
            "украинский многоголосый",
            "amanogawa",
            "glass moon",
            "fanvoxua",
            "украинский дубляж",
            "украинский двухголосый",
            "artymko",
            "украинский (amc)"
        )
    private val movieSelector = "div.b-content__inline_items"
    private val titleSelector = "div.b-content__inline_item-link > a"
    private val posterUrlSelector = "div.b-content__inline_item  img"

    //    private val interceptor = CloudflareInterceptor(cloudflareKiller)
    private val mapper = JsonMapper.builder()
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .build()

    // Basic Info
    override var mainUrl = "https://rezka.ag"
    override var name = "HdRezkaAg"
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
        "$mainUrl/films/page/" to "Фільми",
        "$mainUrl/series/page/" to "Серіали",
        "$mainUrl/cartoons/page/" to "Мультфільми",
        "$mainUrl/animation/page/" to "Аніме"
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        Log.d("getMainPage", "request.data${request.data}")
        val document = getDocument(request.data + page).document
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

    private suspend fun getDocument(
        url: String,
    ): NiceResponse = app.get(url, headers)

    override suspend fun search(query: String): List<SearchResponse> {
        Log.d("search", "query: $query")
        val response =
            app.get("$mainUrl/search/?do=search&subaction=search&q=$query").document
        val map = response.select(movieSelector).map {
            it.toSearchResponse()
        }
        return map
    }

    override suspend fun load(url: String): LoadResponse {
        Log.d("DEBUG load", "Url: $url")
        val document = getDocument(url).document
        val tvType = getTvType(url)

        return when (tvType) {
            TvType.Movie -> {
                getNewMovieLoadResponse(document, tvType, url)
            }

            TvType.Anime -> {
                getNewAnimeLoadResponse(document, tvType, url)
            }

            TvType.Cartoon -> {
                getNewCartoonLoadResponse(document, tvType, url)
            }

            else -> {//series
                getNewTvSeriesLoadResponse(document, tvType, url)
            }
        }
    }

    private fun getNewMovieLoadResponse(
        document: Document,
        tvType: TvType,
        url: String
    ): LoadResponse {
        TODO("Not yet implemented")
    }

    private fun getNewAnimeLoadResponse(
        document: Document,
        tvType: TvType,
        url: String
    ): LoadResponse {
        TODO("Not yet implemented")
    }

    private fun getNewCartoonLoadResponse(
        document: Document,
        tvType: TvType,
        url: String
    ): LoadResponse {
        TODO("Not yet implemented")
    }

    private suspend fun getNewTvSeriesLoadResponse(
        document: Document,
        tvType: TvType,
        url: String
    ): LoadResponse {
        val episodes = getEpisodes(document)
        val title = getPageTitle(document)
        val engTitle = getPageEngTitle(document)
        val posterUrl = getPagePosterUrl(document)
        val year = getYear(document)
        val description = getDescription(document)
        val rating = getRating(document)
        val duration = getDuration(document)
        val trailerUrl = getTrailerUrL(document)
        val tags = getTags(document)
        val actors = getActors(document)

        return newTvSeriesLoadResponse(title, url, tvType, episodes) {
            this.posterUrl = posterUrl
            this.plot = description
            this.tags = tags
            this.year = year
            this.rating = rating
            this.name = "$title ($engTitle)"
            addActors(actors)
            addTrailer(trailerUrl)
            this.duration = duration
        }

    }

    fun contentHasUaDub(document: Document): List<String> {
        val dubs =
            document.select("div.b-translators__block > ul#translators-list.b-translators__list")
                .select("a").filter { element ->
                    val title = element.select("title").text().trim()
                    val img = element.select("img").attr("src")

                    dubList.contains(title.lowercase()) || img.endsWith("ua.png")
                }.map { element ->
                    element.attr("href")
                }
        return dubs
    }

    private fun getActors(document: Document): List<String> {
        return document.select("div.persons-list-holder").component2()
            .select("span.item")
            .map { it.select("span[itemprop=\"name\"]").text() }
    }

    private fun getTags(document: Document): List<String> {
        return document.select("table.b-post__info")
            .select("span[itemprop=\"genre\"]").map { it.text() }
    }

    private fun getTrailerUrL(document: Document): String {
//        return document.select("").text()
        return ""
    }

    private fun getDuration(document: Document): Int {
        return document.select("table.b-post__info")
            .select("td[itemprop=\"duration\"]").text().filter { it.isDigit() }.toInt()
    }

    private fun getRating(document: Document): Int {
        return document.select("table.b-post__info")
            .select("td > span.b-post__info_rates.imdb")
            .select("span.bold").text().trim().toDoubleOrNull()
            ?.absoluteValue?.times(1000f)?.toInt() ?: 0
    }

    private fun getDescription(document: Document): String {
        return document.select("div.b-post__description_text").text()
    }

    private fun getYear(document: Document): Int {
        val text = document.select("table.b-post__info").select("td")
            .first { it.text().contains("Дата выхода") }.parent()?.select("td")?.component2()
            ?.text()

        if (text.isNullOrEmpty()) {
            return -1
        }
        return text.filter { it.isDigit() }.reversed().take(4).reversed().toInt()

    }

    private fun getPagePosterUrl(document: Document): String {
        return document.select("div.b-post__infotable_left").select("a").attr("href")
    }

    private fun getPageEngTitle(document: Document): String {
        return document.select("div.b-post__origtitle").text()
    }

    private fun getPageTitle(document: Document): String {
        return document.select("div.b-post__title").text()
    }

    private fun getEpisodes(document: Document): List<Episode> {
        val player = document.select("div#player.b-player")
        if (player.isEmpty()) {
            return emptyList()
        }

        return player.select("div#simple-episodes-tabs").select("ul").flatMap { aSeason ->
            getEpisodes(aSeason)
        }
    }

    private fun getEpisodes(document: Element): List<Episode> {

        return document.select("a")
            .map { element ->
                val data = element.attr("href")
                val episodeName = element.text()
                val season = element.attr("data-season_id").toInt()
                val episode = element.attr("data-episode_id").toInt()
                Episode(data, episodeName, season, episode, "", -1, "", -1)
            }
    }

    private fun getTvType(url: String): TvType {
        return when {
            url.contains("series") -> TvType.TvSeries
            url.contains("cartoon/series") -> TvType.TvSeries
            url.contains("cartoons") -> TvType.Cartoon
            url.contains("animation") -> TvType.Anime
            else -> TvType.Movie
        }
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

    private suspend fun Element.toSearchResponse(): MovieSearchResponse {
        val tittleNode = this.select(titleSelector)
        val url = tittleNode.attr("href")

        if (url.isEmpty()) {
            return newMovieSearchResponse("", "", TvType.Movie)
        }

        val title = tittleNode.text()
        val posterUrl = fixUrl(
            this.select(posterUrlSelector)
                .attr("src")
        )

//        val contentHasUaDub = contentHasUaDub(app.get(url).document)
//        if (contentHasUaDub.isEmpty()) { // filter only ua dub
//            return newMovieSearchResponse("", "", TvType.Movie)
//        }//todo filter only ua dub

        return newMovieSearchResponse(title, url, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }
}