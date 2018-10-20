package de.ddkfm.plan4ba

import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toModel
import de.ddkfm.plan4ba.models.*
import org.json.JSONObject
import spark.Request
import spark.Response
import spark.Spark.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val config = Config()
fun main(args : Array<String>) {
    config.buildFromEnv()

    port(8080)

    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate(CachingSchedule(), 5, 10, TimeUnit.SECONDS)

    post("/trigger", ::triggerCaching)
    get("/all") {req, resp -> LectureCaller.instance.fillOvernightQueue(); ""}
}

fun triggerCaching(req : Request, resp : Response) : String {
    resp.type("application/json")
    try {
        val job = JSONObject(req.body()).toModel(LectureJob::class.java)
        val caller = LectureCaller.instance
        caller.addJob(job)
        return OK().toJson()
    } catch (e : Exception) {
        return BadRequest("bad HTTP-body").toJson()
    }
}