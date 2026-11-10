package com.chy.radiofrance.core.network

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.core.network.graphql.StationsQuery

/**
 * API brands → flat list of playable stations, in API order.
 * - Something without a stream is not a station (the ICI brand only groups its locals).
 * - Null lists or items are treated as empty: the schema allows them.
 * - Blank descriptions become null (the API sends "" for most of them).
 */
internal fun StationsQuery.Data.toStations(): List<Station> =
    brands.orEmpty().filterNotNull().flatMap { brand ->
        val national =
            station(
                brand.id,
                brand.title,
                brand.description,
                brand.liveStream,
                StationKind.NATIONAL,
                brand
            )
        val webRadios = brand.webRadios.orEmpty().filterNotNull().map {
            station(it.id, it.title, it.description, it.liveStream, StationKind.WEBRADIO, brand)
        }
        val localRadios = brand.localRadios.orEmpty().filterNotNull().map {
            station(it.id, it.title, it.description, it.liveStream, StationKind.LOCAL, brand)
        }
        (listOf(national) + webRadios + localRadios).filterNotNull()
    }

private fun station(
    id: String,
    title: String,
    description: String?,
    liveStream: String?,
    kind: StationKind,
    brand: StationsQuery.Brand
): Station? {
    if (liveStream.isNullOrBlank()) return null
    return Station(
        id = id,
        name = title.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        streamUrl = liveStream,
        kind = kind,
        brandId = brand.id,
        brandName = brand.title.trim()
    )
}
