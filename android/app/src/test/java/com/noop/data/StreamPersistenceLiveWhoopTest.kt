package com.noop.data

import com.noop.protocol.Streams
import com.noop.protocol.HrSample
import com.noop.protocol.RrInterval
import com.noop.protocol.RrSourceChannel
import com.noop.protocol.WhoopEvent
import com.noop.protocol.BatterySample
import com.noop.protocol.Spo2Sample
import com.noop.protocol.SkinTempSample
import com.noop.protocol.RespSample
import org.junit.Assert.*
import org.junit.Test

class StreamPersistenceLiveWhoopTest {
    @Test fun liveProjectionDropsOnlyRrWithoutMutatingDecodedStreams() {
        val streams = Streams(
            hr = mutableListOf(HrSample(100, 70)),
            rr = mutableListOf(RrInterval(100, 860), RrInterval(101, 900, RrSourceChannel.WHOOP5_REALTIME)),
            events = mutableListOf(WhoopEvent(100, "event", mapOf("value" to 1))),
            battery = mutableListOf(BatterySample(100, 50.0, 3800, false)),
            spo2 = mutableListOf(Spo2Sample(100, 97, 1)),
            skinTemp = mutableListOf(SkinTempSample(100, 3300)),
            resp = mutableListOf(RespSample(100, 14000)),
        )
        val full = StreamPersistence.toBatch(streams)
        val live = StreamPersistence.toLiveWhoopBatch(streams)
        assertEquals(full.copy(rr = emptyList()), live)
        assertEquals(2, streams.rr.size)
        assertEquals(full, StreamPersistence.toBatch(streams))
        assertEquals(RrSourceChannel.WHOOP5_REALTIME, full.rr.last().srcChannel)
    }

    @Test fun rrOnlyLiveBatchIsEmptyButGeneralMappingStillPreservesRr() {
        val streams = Streams(rr = mutableListOf(RrInterval(100, 860, RrSourceChannel.GREEN_QUALITY)))
        assertTrue(StreamPersistence.toLiveWhoopBatch(streams).isEmpty)
        assertEquals(listOf(RrRow(100, 860, RrSourceChannel.GREEN_QUALITY)), StreamPersistence.toBatch(streams).rr)
        assertTrue(StreamPersistence.toLiveWhoopBatch(Streams()).isEmpty)
    }
}
