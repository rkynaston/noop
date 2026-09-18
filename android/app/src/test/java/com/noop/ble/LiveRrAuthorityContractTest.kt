package com.noop.ble

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Caller wiring guards: the Android BLE singleton cannot run without platform services.
 * Complements the executable projection/census tests; this does not claim device validation. */
class LiveRrAuthorityContractTest {
    private fun source(name: String): String {
        val root = File(System.getProperty("user.dir") ?: ".").absoluteFile
        return generateSequence(root) { it.parentFile }
            .map { File(it, "android/app/src/main/java/com/noop/ble/$name.kt") }
            .first { it.isFile }.readText()
    }

    @Test fun whoopFlushPersistsProjectionButCensusesDecodedRr() {
        val body = source("WhoopBleClient").substringAfter("private suspend fun flushLive()")
            .substringBefore("private var lastStdRrCensusSec")
        assertTrue(body.contains("val batch = StreamPersistence.toLiveWhoopBatch(streams)"))
        assertTrue(body.contains("repository.insert(batch, deviceId)"))
        assertTrue(body.contains("RrEmissionStats.compute(streams.rr.map"))
        assertTrue(body.contains("rr source=r10r11 liveReceived="))
        assertTrue(body.contains("persisted=0 mode=ui-only"))
        assertFalse(body.contains("RrEmissionStats.compute(batch.rr"))
    }

    @Test fun standardFlushRetriesHrAndContactButNeverRr() {
        val body = source("WhoopBleClient").substringAfter("private suspend fun flushStandardHr()")
            .substringBefore("// MARK: Historical offload")
        assertTrue(body.contains("stdRr.clear()"))
        assertTrue(body.contains("repository.insert(StreamBatch(hr = hr, events = contact), deviceId)"))
        assertFalse(body.contains("rr = rr"))
        val retry = body.substringAfter("catch (t: Throwable)")
        assertTrue(retry.contains("stdHr.addAll(0, hr)"))
        assertTrue(retry.contains("stdContact.addAll(0, contact)"))
        assertFalse(retry.contains("stdRr.addAll"))
        assertTrue(retry.contains("rrFrames = 0"))
        assertTrue(body.contains("RrEmissionStats.compute(rr.map"))
        assertTrue(body.contains("rr source=standard liveReceived="))
        assertTrue(body.contains("persisted=0 mode=ui-only"))
    }

    @Test fun standaloneStandardHrSourceAlsoExcludesRrFromPersistenceAndRetry() {
        val body = source("StandardHrSource").substringAfter("private fun flush(")
            .substringBefore("override fun onScanResult")
        assertTrue(body.contains("persist(StreamBatch(hr = hrRows, events = contactEvents)"))
        assertFalse(body.contains("rr = rrRows"))
        assertTrue(body.contains("snapshot.map { it.copy(rr = emptyList()) }"))
        assertTrue(body.contains("rr source=standard liveReceived="))
        assertTrue(body.contains("persisted=0 mode=ui-only"))
        assertTrue(body.contains("rrFrames = 0"))
    }

    @Test fun historicalDiagnosticUsesDecodedSessionAndActualInsertedCount() {
        val body = source("Backfiller").substringAfter("fun sessionRrEmissionLine()")
            .substringBefore("companion object")
        assertTrue(body.contains("RrEmissionStats.historicalAuthorityLine("))
        assertTrue(body.contains("received = sessionRrOffered, accepted = sessionRrOffered"))
        assertTrue(body.contains("persisted = sessionRrInserted, rejected = 0"))
    }
}
