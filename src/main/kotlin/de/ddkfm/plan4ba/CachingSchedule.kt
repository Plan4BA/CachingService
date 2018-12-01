package de.ddkfm.plan4ba

import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.isWithinInterval
import java.time.LocalDateTime
import java.time.LocalTime

class CachingSchedule : Runnable {

    override fun run() {
        val lectureCaller = LectureCaller.instance
        val now = LocalTime.now()
        if(now in config.timeSpanStart..config.timeSpanStart.plusSeconds(10)) {
            lectureCaller.fillOvernightQueue()
        }
        if(!lectureCaller.isWorking)
            lectureCaller.downloadLectures()
    }
}