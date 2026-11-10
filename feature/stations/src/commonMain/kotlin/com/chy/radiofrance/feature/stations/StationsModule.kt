package com.chy.radiofrance.feature.stations

import com.chy.radiofrance.feature.stations.data.StationsRepositoryImpl
import com.chy.radiofrance.feature.stations.domain.GetStationSectionsUseCase
import com.chy.radiofrance.feature.stations.domain.StationsRepository
import com.chy.radiofrance.feature.stations.presentation.StationsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the stations feature.
 *
 * Needs from other modules: `StationsRemoteDataSource` (core:network) and
 * `AudioPlayer` (core:player). The verify test lists them explicitly.
 */
val stationsModule = module {
    single<StationsRepository> { StationsRepositoryImpl(remote = get()) }
    factory { GetStationSectionsUseCase(repository = get()) }
    // viewModelOf: scoped to the screen's ViewModelStore (survives rotation) on
    // Android, and to the Compose navigation/owner on iOS.
    viewModelOf(::StationsViewModel)
}
