package fr.outadoc.quickhass.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Service(
    @Json(name = "domain")
    val domain: String,
    @Json(name = "services")
    val services: List<String>
)