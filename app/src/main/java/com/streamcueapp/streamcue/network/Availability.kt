package com.streamcueapp.streamcue.network

/** Mirrors iOS's Availability struct. */
data class Availability(
    val subscription: List<String> = emptyList(),
    val free: List<String> = emptyList(),
    val rentOrBuy: List<String> = emptyList(),
    val link: String? = null,
)

/** Mirrors iOS's TMDBClient.providers(path:region:) narrowing to one region. */
fun TmdbWatchProviderResponse.availability(region: String = "US"): Availability {
    val regionProviders = results[region] ?: return Availability()
    return Availability(
        subscription = (regionProviders.flatrate ?: emptyList()).map { it.providerName },
        free = ((regionProviders.free ?: emptyList()) + (regionProviders.ads ?: emptyList()))
            .map { it.providerName },
        rentOrBuy = ((regionProviders.rent ?: emptyList()) + (regionProviders.buy ?: emptyList()))
            .map { it.providerName },
        link = regionProviders.link,
    )
}
