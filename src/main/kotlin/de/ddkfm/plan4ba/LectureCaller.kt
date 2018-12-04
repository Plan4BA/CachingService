package de.ddkfm.plan4ba

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.isWithinInterval
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toModel
import de.ddkfm.plan4ba.models.Lecture
import de.ddkfm.plan4ba.models.LectureJob
import de.ddkfm.plan4ba.models.User
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.util.*
import kotlin.concurrent.thread

data class LectureCaller(
        var isWorking : Boolean = false,
        private val justInTimeQueue : Queue<LectureJob> = LinkedList(),
        private val overnightQueue : Queue<LectureJob> = LinkedList()
) {
    companion object {
        val instance = LectureCaller()
    }

    fun downloadLectures() {
        val now = LocalTime.now()
        if(now.isWithinInterval(config.timeSpanStart, config.timeSpanEnd)){
            //only working on overnightQueue
            workOnQueue(this.overnightQueue)
        }
        workOnQueue(this.justInTimeQueue)

    }
    fun addJob(job : LectureJob) {
        this.justInTimeQueue.offer(job)
    }

    fun workOnQueue(queue : Queue<LectureJob>) {
        //remove depllicate entries
        val copyList = queue.distinctBy { it.hashCode() }
        queue.clear()
        queue.addAll(copyList)
        while(!queue.isEmpty()) {
            val wholeStartTime = System.currentTimeMillis()
            val threads = (1..config.maxParallelJobs)
                    .map threadExecute@{
                        if(queue.isEmpty())
                            null
                        else {
                            val job = queue.remove()
                            val thread = thread {
                                val start = System.currentTimeMillis()
                                try {
                                    val resp = Unirest.get("https://selfservice.campus-dual.de/room/json?userid=${job.matriculationNumber}&hash=${job.hash}")
                                        .asJson()
                                    val lectures = resp.body.array.mapToLectureModel(job.userId)
                                            .filter { it.start >= (System.currentTimeMillis() / 1000) - 3 * 31 * 24 * 3600 }
                                    updateLectures(job.userId, lectures)
                                } catch (e : Exception) {
                                    println("Campus Dual request for ${job.matriculationNumber} throw exception: ")
                                    e.printStackTrace()

                                }
                                println("Thread take ${(System.currentTimeMillis() - start) / 1000.0} s")
                            }
                            thread
                        }
                    }
            threads.forEach { it?.join() }
            println("whole time: ${(System.currentTimeMillis() - wholeStartTime) / 1000.0} s")
        }
    }

    fun fillOvernightQueue() {
        val users = (Unirest.get("${config.dbServiceEndpoint}/users")
                .toModel(User::class.java).second as List<User>)
        users.map { user ->
                if(!user.userHash.isNullOrEmpty())
                    LectureJob(
                            userId = user.id,
                            matriculationNumber = user.matriculationNumber,
                            hash = user.userHash!!
                    )
                else
                    null
            }
                .filterNotNull()
                .forEach { this.overnightQueue.offer(it)}
    }

    fun updateLectures(userId : Int, lectures : List<Lecture>) {
        val oldLectures = Unirest.get("${config.dbServiceEndpoint}/lectures?userId=$userId")
                .asJson()
                .body.array
                .map { (it as JSONObject).toModel(Lecture::class.java) }
        var changed = false
        lectures.forEach { changed = changed || !oldLectures.contains(it) }
        println("Calendar for User $userId ${if(changed) "changed" else "not changed"}")
        if(changed) {
            val resp = Unirest.delete("${config.dbServiceEndpoint}/lectures?userId=$userId")
                    .asJson()
            println("lectures for $userId deleted")
            lectures.forEach{ lecture ->
                val (status, lectureResp) = Unirest.put("${config.dbServiceEndpoint}/lectures")
                        .body(lecture.toJson())
                        .toModel(Lecture::class.java)
            }

            val user = (Unirest.get("${config.dbServiceEndpoint}/users/$userId")
                    .toModel(User::class.java).second) as User
            user.lastLecturePolling = System.currentTimeMillis()
            Unirest.post("${config.dbServiceEndpoint}/users/$userId")
                    .body(user.toJson())
                    .asJson()
        }
    }
}
fun JSONArray.mapToLectureModel(userId : Int) : List<Lecture> {
    return this.map { obj ->
        val lectureJson = obj as JSONObject
        val title = obj.getString("title")
        val seed = title.hashCode()
        val hexColor = Random(seed.toLong()).randomHexString()

        Lecture(
                id = 0,
                userId = userId,
                title = obj.getString("title"),
                sroom = obj.getString("sroom"),
                room = obj.getString("room"),
                remarks = obj.getString("remarks"),
                instructor = obj.getString("instructor"),
                color = hexColor,
                allDay = obj.getBoolean("allDay"),
                description = obj.getString("description"),
                start = obj.getLong("start"),
                end = obj.getLong("end"),
                exam = obj.getString("remarks").startsWith("Prüfung")
        )
    }
}
fun Random.randomHexString() : String {
    return (0..2)
            .map{ this.nextInt(256) }
            .joinToString(separator = "", prefix = "#") { "%02x".format(it) }
}