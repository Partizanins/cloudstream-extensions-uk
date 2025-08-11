package com.lagradost.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Sources(
    @JsonProperty("url") val url: String,
    @JsonProperty("subtitle") val subtitle: Any?,
    )