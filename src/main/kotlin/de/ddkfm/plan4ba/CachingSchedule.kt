package de.ddkfm.plan4ba

import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.isWithinInterval
import java.time.LocalTime

class CachingSchedule : Runnable {
    override fun run() {
        val lectureCaller = LectureCaller.instance
        if(LocalTime.now() == config.timeSpanStart) {
            lectureCaller.fillOvernightQueue()
        }
        if(!lectureCaller.isWorking)
            lectureCaller.downloadLectures()
    }
}