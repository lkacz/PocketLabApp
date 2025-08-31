package com.lkacz.pola

import kotlin.system.measureNanoTime
import timber.log.Timber

/** Simple utility to measure and log execution time of a block. */
object PerfTimer {
    inline fun <T> track(label: String, block: () -> T): T {
        var result: T
        val elapsedNs = measureNanoTime { result = block() }
        val ms = elapsedNs / 1_000_000.0
        Timber.tag("Perf").d("%s took %.2f ms", label, ms)
        return result
    }
}
