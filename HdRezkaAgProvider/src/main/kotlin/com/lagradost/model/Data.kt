package com.lagradost.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Data(
    @JsonProperty("id") val id: String?,
    @JsonProperty("favs") val favs: String?,
    @JsonProperty("server") val server: List<Server>?,
    @JsonProperty("season") val season: String?,
    @JsonProperty("episode") val episode: String?,
    @JsonProperty("action") val action: String?,
    @JsonProperty("ref") val ref: String?,
)