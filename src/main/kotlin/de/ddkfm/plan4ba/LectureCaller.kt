package de.ddkfm.plan4ba

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.isWithinInterval
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toMillis
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import io.sentry.event.Event
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
                    .map threadExecute@ {
                        if(queue.isEmpty())
                            null
                        else {
                            val job = queue.remove()
                            val user = DBService.get<User>(job.userId).maybe ?: return@threadExecute null
                            val thread = thread {
                                val start = System.currentTimeMillis()
                                try {
                                    val start = (System.currentTimeMillis() / 1000) - 3 * 31 * 24 * 3600
                                    val end = (System.currentTimeMillis() / 1000) + 3 * 31 * 24 * 3600
                                    val timestamp = System.currentTimeMillis()
                                    val resp = Unirest.get("https://selfservice.campus-dual.de/room/json?userid=${job.matriculationNumber}" +
                                            "&hash=${job.hash}&start=$start&end=$end&_=$timestamp")
                                        .asJson()
                                    val lectures = resp.body.array.mapToLectureModel(job.userId)
                                            .filter { it.start >= (System.currentTimeMillis() / 1000) - 3 * 31 * 24 * 3600 }
                                    updateLectures(job.userId, lectures)
                                    if(user.storeReminders)
                                        downloadReminders(user)
                                    if(user.storeExamsStats)
                                        downloadExamStats(user)
                                } catch (e : Exception) {
                                    SentryTurret.log {
                                        user(username = job.matriculationNumber)
                                    }.capture(e)
                                }
                                SentryTurret.log {
                                    addTag("section", "Caching")
                                }.event {
                                    withLevel(Event.Level.INFO)
                                    withMessage("Thread take ${(System.currentTimeMillis() - start) / 1000.0} s (userId: ${job.userId})")
                                }
                                println("Thread take ${(System.currentTimeMillis() - start) / 1000.0} s")
                            }
                            return@threadExecute thread
                        }
                    }
            threads.forEach { it?.join() }
            println("whole time: ${(System.currentTimeMillis() - wholeStartTime) / 1000.0} s")
            SentryTurret.log {
                addTag("section", "Caching")
            }.event {
                withLevel(Event.Level.INFO)
                withMessage("whole time: ${(System.currentTimeMillis() - wholeStartTime) / 1000.0} s")
            }
        }
    }

    private fun downloadExamStats(user: User) {
        val examStatResp = Unirest.get("https://selfservice.campus-dual.de/dash/getexamstats?user=${user.matriculationNumber}&hash=${user.userHash}")
            .asJson().body.`object`
        val creditPointsResp = Unirest.get("https://selfservice.campus-dual.de/dash/getcp?user=${user.matriculationNumber}&hash=${user.userHash}")
            .asString().body.toIntOrNull()
        val existingExamStat = DBService.all<ExamStat>("userId" to user.id).maybe?.firstOrNull()
        if(existingExamStat != null)
            DBService.delete<ExamStat>(existingExamStat.id)
        val newExamStat = ExamStat(
            id = 0,
            exams = examStatResp.getInt("EXAMS"),
            success = examStatResp.getInt("SUCCESS"),
            modules = examStatResp.getInt("MODULES"),
            failure = examStatResp.getInt("FAILURE"),
            booked = examStatResp.getInt("BOOKED"),
            mbooked = examStatResp.getInt("MBOOKED"),
            userId = user.id,
            creditpoints = creditPointsResp ?: 0
        )
        val createResp = DBService.create(newExamStat)
        if(createResp.maybe == null)
            throw java.lang.Exception("Examstat for user ${user.id} could not created")


    }

    private fun downloadReminders(user: User) {
        val reminderResp = Unirest.get("https://selfservice.campus-dual.de/dash/getreminders?user=${user.matriculationNumber}&hash=${user.userHash}")
            .asJson().body.`object`
        val existingReminder = DBService.all<Reminder>("userId" to user.id).maybe?.firstOrNull()
        if(existingReminder != null) {
            val ok = DBService.delete<Reminder>(existingReminder.id).getOrThrow()
        }
        var reminder = Reminder(id = 0, userId = user.id,
            semester = reminderResp.getInt("SEMESTER"),
            exams = reminderResp.getInt("EXAMS"),
            electives = reminderResp.getInt("ELECTIVES"))
        reminder = DBService.create(reminder).maybe ?: throw Exception("Reminder coult not be created")
        val upcomingResp = reminderResp.getJSONArray("UPCOMING")
        val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss")
        val upcomings = upcomingResp.map { jsonObject ->
            jsonObject as JSONObject
            val beginString = jsonObject.getString("EVDAT") + " " + jsonObject.getString("BEGUZ")
            val endString = jsonObject.getString("EVDAT") + " " + jsonObject.getString("ENDUZ")
            Upcoming(
                id = 0,
                comment = jsonObject.getString("COMMENT"),
                instructor = jsonObject.getString("INSTRUCTOR"),
                room = jsonObject.getString("ROOM"),
                title = jsonObject.getString("SM_STEXT"),
                shortTitle = jsonObject.getString("SM_SHORT"),
                begin = LocalDateTime.parse(beginString, dateFormat).toMillis(),
                end = LocalDateTime.parse(endString, dateFormat).toMillis(),
                reminderId = reminder.id
            )
        }.map { DBService.create(it).maybe }
        val latestResp = reminderResp.getJSONArray("LATEST")
        val latestExamResult = latestResp.map { jsonObject ->
            jsonObject as JSONObject
            LatestExamResult(
                id = 0,
                reminderId = reminder.id,
                shortTitle = jsonObject.getString("AWOBJECT_SHORT"),
                title = jsonObject.getString("AWOBJECT"),
                grade = jsonObject.getString("GRADESYMBOL").replace(",", ".").toDoubleOrNull() ?: 0.0,
                type = jsonObject.getString("AGRTYPE"),
                agrDate = LocalDateTime.parse(jsonObject.getString("AGRDATE") + " 000000",dateFormat).toMillis(),
                status = jsonObject.getString("AWSTATUS")
            )
        }.map { DBService.create(it).maybe }
    }

    fun fillOvernightQueue() {
        val users = DBService.all<User>().maybe ?: return
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
        val oldLectures = DBService.all<Lecture>("userId" to userId).maybe?.toMutableList() ?: return
        val mutableLectures = lectures.toMutableList()
        val updatedLectures = mutableMapOf<Lecture, Lecture>()
        val deletedLectures = mutableListOf<Lecture>()
        oldLectures.filter { !it.deprecated }.forEach { oldLecture ->
            val found = mutableLectures.find { it.title == oldLecture.title && it.start == oldLecture.start
                && it.end == oldLecture.end && it.exam == oldLecture.exam}
            if(found == null) {
                deletedLectures.add(oldLecture)
            } else {
                found.id = oldLecture.id
                if(found.hashCode() != oldLecture.hashCode()) {
                    updatedLectures.put(oldLecture, found)
                }
                mutableLectures.remove(found)
            }
        }
        val createdLectures = mutableLectures

        var changed = createdLectures.isNotEmpty() || deletedLectures.isNotEmpty() || updatedLectures.isNotEmpty()

        println("Calendar for User $userId ${if(changed) "changed" else "not changed"}")
        val user = DBService.get<User>(userId).maybe ?: return
        if(changed) {
            val notification = DBService.create(Notification(0, userId = user.id, type = "lectureChanged", versionId = null, timestamp = System.currentTimeMillis())).maybe
            if(notification == null)
                throw java.lang.Exception("could not create a notification")
            val changes = mutableListOf<LectureChange>()
            deletedLectures.forEach { deletedLecture ->
                deletedLecture.deprecated = true
                DBService.update(deletedLecture) { it.id }
                changes.add(LectureChange(0, notification.id, deletedLecture.id, null))
            }
            updatedLectures.forEach { updatedLecture ->
                val old = updatedLecture.key
                old.deprecated = true
                DBService.update(old) { it.id }
                val new = DBService.create(updatedLecture.value).maybe
                changes.add(LectureChange(0, notification.id, old.id, new?.id))
            }
            createdLectures.forEach { createdLecture ->
                val new = DBService.create(createdLecture).maybe
                changes.add(LectureChange(0, notification.id, null, new?.id))
            }
            changes.forEach { DBService.create(it) }
        }
        user.lastLecturePolling = System.currentTimeMillis()
        DBService.update(user) { it.id }
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