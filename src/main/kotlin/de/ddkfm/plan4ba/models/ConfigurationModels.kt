package de.ddkfm.plan4ba.models

import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.getEnvOrDefault
import java.time.LocalTime

data class Config(
        var dbServiceEndpoint : String = "http://localhost:8080",
        var timeSpanStart : LocalTime = LocalTime.of(23, 0),
        var timeSpanEnd : LocalTime = LocalTime.of(2, 0),
        var maxParallelJobs : Int = 5
) {
    fun buildFromEnv() {
        this.dbServiceEndpoint = getEnvOrDefault("DBSERVICE_ENDPOINT", this.dbServiceEndpoint)
        this.maxParallelJobs = getEnvOrDefault("MAX_PARALLEL_JOBS", "$maxParallelJobs").toInt()

        val start = getEnvOrDefault("TIMESPAN_START", timeSpanStart.toString())
        try {
            this.timeSpanStart = LocalTime.parse(start)
        } catch (e : Exception) {
            println("timespan start $start does not match the pattern HH:mm. $timeSpanStart is used")
        }
        val end = getEnvOrDefault("TIMESPAN_END", timeSpanEnd.toString())
        try {
            this.timeSpanEnd = LocalTime.parse(end)
        } catch (e : Exception) {
            println("timespan end $end does not match the pattern HH:mm. $timeSpanEnd is used")
        }
    }
}