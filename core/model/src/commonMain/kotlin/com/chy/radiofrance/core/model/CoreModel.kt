/*
 * core:model: the domain types every layer shares (for example `Station`).
 *
 * Why a module: network, player and features all talk about the same concepts.
 * Putting them here lets those modules share the vocabulary without depending
 * on each other, and without pulling in Apollo.
 *
 * Rule: no dependencies at all. No Apollo, no Compose, no Koin.
 */
package com.chy.radiofrance.core.model
