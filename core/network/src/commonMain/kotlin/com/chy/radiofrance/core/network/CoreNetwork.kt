/*
 * core:network: the Radio France Open API client (Apollo GraphQL, from step 2).
 *
 * Why a module: it is the only place that knows about GraphQL, Apollo, Ktor
 * and the API key. Generated Apollo code stays `internal`, and only core:model
 * types leave this module, so the transport can change without touching features.
 *
 * Rule: depends on core:model only.
 */
package com.chy.radiofrance.core.network
