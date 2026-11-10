package com.chy.radiofrance.feature.stations

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind

internal fun station(id: String, kind: StationKind, brandId: String, brandName: String = brandId) =
    Station(
        id = id,
        name = id,
        description = null,
        streamUrl = "https://icecast.radiofrance.fr/$id.mp3",
        kind = kind,
        brandId = brandId,
        brandName = brandName
    )
