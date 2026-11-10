package com.chy.radiofrance.feature.stations

import com.chy.radiofrance.feature.stations.data.StationsRepositoryImpl
import com.chy.radiofrance.feature.stations.domain.GetStationSectionsUseCase
import com.chy.radiofrance.feature.stations.domain.StationsRepository
import org.koin.dsl.module

/** Koin bindings of the stations feature. The ViewModel comes with the screen (step 3). */
val stationsModule = module {
    single<StationsRepository> { StationsRepositoryImpl(remote = get()) }
    factory { GetStationSectionsUseCase(repository = get()) }
}
