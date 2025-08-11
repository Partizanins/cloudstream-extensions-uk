package com.lagradost.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Server(
    @JsonProperty("translator_name") val translator_name: String?,
    @JsonProperty("translator_id") val translator_id: String?,
    @JsonProperty("camrip") val camrip: String?,
    @JsonProperty("ads") val ads: String?,
    @JsonProperty("director") val director: String?,
)