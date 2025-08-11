package com.lagradost.model

import com.fasterxml.jackson.annotation.JsonProperty

data class LocalSources(
    @JsonProperty("streams") val streams: String,
    @JsonProperty("subtitle") val subtitle: Any?,
)