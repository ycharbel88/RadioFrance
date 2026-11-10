package com.chy.radiofrance.core.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.util.EventLogger

/**
 * Debug only. Media3's [EventLogger] (state, loading, discontinuities, errors under
 * the "EventLogger" tag) plus the two lines it doesn't print: when a load starts
 * and when one is cancelled.
 *
 * For a progressive stream, one load task is one HTTP connection. So after a resume,
 * `loadCanceled` (old task) then `loadStarted` with a new `task` id proves a new
 * connection to the live stream; no such pair means the old buffer is being played.
 */
@OptIn(UnstableApi::class)
internal class LoadEventLogger : EventLogger() {
    override fun onLoadStarted(
        eventTime: EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        retryCount: Int
    ) {
        logd(
            "loadStarted [task=${loadEventInfo.loadTaskId}, retry=$retryCount, ${loadEventInfo.uri}]"
        )
    }

    override fun onLoadCanceled(
        eventTime: EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        logd("loadCanceled [task=${loadEventInfo.loadTaskId}, bytes=${loadEventInfo.bytesLoaded}]")
    }
}
