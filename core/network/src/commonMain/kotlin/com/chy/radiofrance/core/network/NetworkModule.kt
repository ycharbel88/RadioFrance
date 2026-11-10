package com.chy.radiofrance.core.network

import org.koin.dsl.module

/**
 * Koin bindings of core:network. Only [StationsRemoteDataSource] is meant to be
 * used outside; the Apollo client is an implementation detail.
 */
val networkModule = module {
    single { buildApolloClient(apiKey = BuildKonfig.RADIOFRANCE_API_KEY) }
    single<StationsRemoteDataSource> {
        ApolloStationsRemoteDataSource(
            apolloClient = get(),
            apiKey = BuildKonfig.RADIOFRANCE_API_KEY
        )
    }
}
