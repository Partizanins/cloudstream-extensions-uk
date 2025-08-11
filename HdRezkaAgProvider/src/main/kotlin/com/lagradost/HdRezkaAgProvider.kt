package com.lagradost

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.lagradost.api.Log
import com.lagradost.cloudstream3.Actor
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
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.model.Data
import com.lagradost.model.LocalSources
import com.lagradost.model.Sources
import com.lagradost.nicehttp.NiceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Date
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
    private val movieSelector = "div.b-content__inline_items > div"
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
            getDocument("$mainUrl/search/?do=search&subaction=search&q=$query").document
        return response.select(movieSelector).map {
            it.toSearchResponse()
        }

    }

    override suspend fun load(url: String): LoadResponse {
        Log.d("load", "Url: $url")
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
        val episodes = getEpisodes(document, url)
        val title = getPageTitle(document)
        val engTitle = getPageEngTitle(document)
        val posterUrl = getPagePosterUrl(document)
        val year = getYear(document)
        val description = getDescription(document)
        val rating = getRating(document)
        val duration = getDuration(document)
        val trailerUrl = getTrailerUrL(url)
        val tags = getTags(document)
        val actors = getActors(document)

        val newTvSeriesLoadResponse = newTvSeriesLoadResponse(title, url, tvType, episodes) {
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
        Log.d("getNewTvSeriesLoadResponse", newTvSeriesLoadResponse.toString())
        return newTvSeriesLoadResponse

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

    private fun getActors(document: Document): List<Actor> {
        return document.select("table.b-post__info > tbody > tr:last-child span.item").mapNotNull {
            Actor(
                it.selectFirst("span[itemprop=name]")?.text() ?: return@mapNotNull null,
                it.selectFirst("span[itemprop=actor]")?.attr("data-photo")
            )
        }
    }

    private fun getTags(document: Document): List<String> {
        return document.select("table.b-post__info > tbody > tr:contains(Жанр) span[itemprop=genre]")
            .map { it.text() }
    }

    private suspend fun getTrailerUrL(url: String): String {
        val id = getId(url)
        val post = app.post(
            "https://rezka.ag/engine/ajax/gettrailervideo.php",
            data = mapOf("id" to id),
            referer = url
        )
        val readTree = ObjectMapper().readTree(post.text)
        val iframe = readTree.get("code")
        val parse = try {
            Jsoup.parse(iframe.textValue())
        } catch (e: Exception) {
            System.err.println(e)
            return ""
        }
        return parse.select("iframe").attr("src")

    }

    private fun getId(url: String): String {
        return url.split("/").last().split("-").first()
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

    private fun getEpisodes(document: Document, url: String): List<Episode> {
        val player = document.select("div#player.b-player")
        if (player.isEmpty()) {
            return emptyList()
        }

        val episodes = player.select("div#simple-episodes-tabs").select("ul").flatMap { aSeason ->
            getEpisodesFromSeason(aSeason, url)
        }

        for (episode in episodes) {
            val url = episode.data
            episode.data = getEpisodeData(url, document, episode)
        }

        return episodes
    }

    private fun getEpisodesFromSeason(
        document: Element,
        url: String
    ): List<Episode> {

        val select = document.select("a")
        if (select.isNotEmpty()) {
            return select.map { element ->
                val data = element.attr("href").ifEmpty {
                    url
                }

                val episodeName = element.text()
                val season = element.attr("data-season_id").toInt()
                val episode = element.attr("data-episode_id").toInt()
                Episode(
                    data,
                    episodeName,
                    season,
                    episode,
                    "",
                    -1,
                    "",
                    -1
                )
            }
        }

        return document.select("li").map { element ->
            val data = element.attr("href").ifEmpty {
                url
            }
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
        Log.d("loadLinks", "Data: $data")

        tryParseJson<Data>(data)?.let { res ->
            if (res.server?.isEmpty() == true) {
                val document = getDocument(res.ref ?: return@let).document
                document.select("script").map { script ->
                    if (script.data().contains("sof.tv.initCDNSeriesEvents(")) {
                        val dataJson =
                            script.data().substringAfter("false, {").substringBefore("});")
                        tryParseJson<LocalSources>("{$dataJson}")?.let { sources ->
                            invokeSources(
                                this.name,
                                sources.streams,
                                sources.subtitle.toString(),
                                subtitleCallback,
                                callback
                            )
                        }
                    }
                }
            } else {
                res.server?.apmap { server ->
                    app.post(
                        url = "$mainUrl/ajax/get_cdn_series/?t=${Date().time}",
                        data = mapOf(
                            "id" to res.id,
                            "translator_id" to server.translator_id,
                            "favs" to res.favs,
                            "is_camrip" to server.camrip,
                            "is_ads" to server.ads,
                            "is_director" to server.director,
                            "season" to res.season,
                            "episode" to res.episode,
                            "action" to res.action,
                        ).filterValues { it != null }.mapValues { it.value as String },
                        referer = res.ref
                    ).parsedSafe<Sources>()?.let { source ->
                        invokeSources(
                            server.translator_name.toString(),
                            source.url,
                            source.subtitle.toString(),
                            subtitleCallback,
                            callback
                        )
                    }
                }
            }
        }

        return true
    }

    private fun Element.toSearchResponse(): MovieSearchResponse {
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

    private fun getEpisodeData(url: String, document: Document, episode: Episode): String {
        val server = ArrayList<Map<String, String>>()
        val data = HashMap<String, Any>()

        document.select("ul#translators-list a").map { res ->
            server.add(
                mapOf(
                    "translator_name" to res.text(),
                    "translator_id" to res.attr("data-translator_id"),
                )
            )
        }

        data["season"] = "${episode.season}"
        data["episode"] = "${episode.episode}"
        data["server"] = server
        data["action"] = "get_stream"
        data["id"] = getId(url)
        data["favs"] = document.selectFirst("input#ctrl_favs")?.attr("value").toString()
        data["ref"] = url

        return data.toJson()
    }

    private suspend fun cleanCallback(
        source: String,
        url: String,
        quality: String,
        isM3u8: Boolean,
        sourceCallback: (ExtractorLink) -> Unit
    ) {
        println("source:$source, url:$url, quality:$quality, isM3u8:$isM3u8, sourceCallback:$sourceCallback")
        sourceCallback.invoke(
            M3u8Helper.generateM3u8(
                source = "dubName",
                streamUrl = url,
                referer = "$mainUrl/"
            ).first()
            // Call with the main arguments...
//            newExtractorLink(
//                source = source,
//                name = source,
//                url = url
//            ) {
//                // ...and then use the initializer block to set the other properties.
//                referer = "$mainUrl/"
//                this.quality = getQuality(quality)
//                this.isM3u8 = isM3u8
//                headers = mapOf(
//                    "Origin" to mainUrl
//                )
//            }
        )
    }

    private fun decryptStreamUrl(data: String): String {

        fun getTrash(arr: List<String>, item: Int): List<String> {
            val trash = ArrayList<List<String>>()
            for (i in 1..item) {
                trash.add(arr)
            }
            return trash.reduce { acc, list ->
                val temp = ArrayList<String>()
                acc.forEach { ac ->
                    list.forEach { li ->
                        temp.add(ac.plus(li))
                    }
                }
                return@reduce temp
            }
        }

        val trashList = listOf("@", "#", "!", "^", "$")
        val trashSet = getTrash(trashList, 2) + getTrash(trashList, 3)
        var trashString = data.replace("#h", "").split("//_//").joinToString("")

        trashSet.forEach {
            val temp = base64Encode(it.toByteArray())
            trashString = trashString.replace(temp, "")
        }

        return base64Decode(trashString)

    }


    private suspend fun invokeSources(
        source: String,
        url: String,
        subtitle: String,
        subCallback: (SubtitleFile) -> Unit,
        sourceCallback: (ExtractorLink) -> Unit
    ) {
        decryptStreamUrl(url).split(",").map { links ->
            val quality =
                Regex("\\[([0-9]{3,4}p\\s?\\w*?)]").find(links)?.groupValues?.getOrNull(1)
                    ?.trim() ?: return@map null
            links.replace("[$quality]", "").split(" or ")
                .map {
                    val link = it.trim()
                    val type = if (link.contains(".m3u8")) "(Main)" else "(Backup)"
                    cleanCallback(
                        "$source $type",
                        link,
                        quality,
                        link.contains(".m3u8"),
                        sourceCallback,
                    )
                }
        }

        subtitle.split(",").map { sub ->
            val language =
                Regex("\\[(.*)]").find(sub)?.groupValues?.getOrNull(1) ?: return@map null
            val link = sub.replace("[$language]", "").trim()
            subCallback.invoke(
                SubtitleFile(
                    getLanguage(language),
                    link
                )
            )
        }
    }


    private fun getLanguage(str: String): String {
        return when (str) {
            "Русский" -> "Russian"
            "Українська" -> "Ukrainian"
            else -> str
        }
    }

    private fun getQuality(str: String): Int {
        return when (str) {
            "360p" -> Qualities.P240.value
            "480p" -> Qualities.P360.value
            "720p" -> Qualities.P480.value
            "1080p" -> Qualities.P720.value
            "1080p Ultra" -> Qualities.P1080.value
            else -> getQualityFromName(str)
        }
    }

}