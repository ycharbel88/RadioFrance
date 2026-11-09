/*
 * core:player: the `AudioPlayer` interface and its platform implementations
 * (Media3 on Android; for now an `UnavailableAudioPlayer` stub on iOS).
 *
 * Why a module: playback outlives the screen (background, notification, lock
 * screen) and has to be testable with a fake. The UI only ever sees the
 * interface.
 *
 * Rule: knows nothing about UI or Compose. Depends on core:model only.
 */
package com.chy.radiofrance.core.player
