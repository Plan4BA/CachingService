package de.ddkfm.plan4ba.utils

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.config
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toListModel
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toModel
import de.ddkfm.plan4ba.models.*
import org.json.JSONObject
import java.lang.Exception

object DBService {
    inline fun <reified T> all(vararg filter : Pair<String, Any>) : Maybe<List<T>> {
        val path = getPath(T::class.java)
        val filterString = if(filter.isNotEmpty())
            filter.joinToString(separator = "&", prefix = "?") { "${it.first}=${it.second}"}
        else ""
        val response = Unirest.get("$path$filterString").asString()
        if(response.status in listOf(200, 201)) {
            return Maybe.of(response.body.toListModel()!!)
        } else {
            return Maybe.ofError(HttpStatus(response.status, response.body))
        }
    }

    inline fun <reified T> get(id : Int) : Maybe<T> {
        val path = getPath(T::class.java, "$id")
        val response = Unirest.get(path).asString()
        if(response.status in listOf(200, 201)) {
            return Maybe.of(response.body.toModel()!!)
        } else {
            return Maybe.ofError(HttpStatus(response.status, response.body))
        }
    }

    inline fun <reified T> get(id : String) : Maybe<T> {
        val path = getPath(T::class.java, id)
        val response = Unirest.get(path).asString()
        if(response.status in listOf(200, 201)) {
            return Maybe.of(response.body.toModel()!!)
        } else {
            return Maybe.ofError(HttpStatus(response.status, response.body))
        }
    }

    inline fun <reified T : Any> create(entity : T) : Maybe<T> {
        val path = getPath(entity::class.java)
        val response = Unirest.put(path)
            .body(entity.toJson())
            .asString()
        if(response.status in listOf(200, 201)) {
            return Maybe.of(response.body.toModel()!!)
        } else {
            return Maybe.ofError(HttpStatus(response.status, response.body))
        }
    }

    inline fun <reified T : Any> update(entity : T, idLambda : (T) -> Int) : Maybe<T> {
        val path = getPath(entity::class.java, idLambda(entity).toString())
        val response = Unirest.post(path)
            .body(entity.toJson())
            .asString()
        if(response.status in listOf(200, 201)) {
            return Maybe.of(response.body.toModel()!!)
        } else {
            return Maybe.ofError(HttpStatus(response.status, response.body))
        }
    }

    inline fun <reified T : Any> delete(id : Int) : Maybe<String> {
        val path = getPath(T::class.java, id.toString())
        val response = Unirest.delete(path)
            .asString()
        if(response.status in listOf(200, 201)) {
            return Maybe.of(response.body)
        } else {
            return Maybe.ofError(HttpStatus(response.status, response.body))
        }
    }

    fun getPath(clazz : Class<*>, id : String? = null) : String {
        var path = "${config.dbServiceEndpoint}/"
        path += when(clazz) {
            Infotext::class.java -> "info"
            Lecture::class.java -> "lectures"
            Notification::class.java -> "notifications"
            Token::class.java -> "tokens"
            University::class.java -> "universities"
            User::class.java -> "users"
            UserGroup::class.java -> "groups"
            AppVersion::class.java -> "app"
            ExamStat::class.java -> "examstats"
            LatestExamResult::class.java -> "latest"
            LectureChange::class.java -> "changes"
            Reminder::class.java -> "reminders"
            Upcoming::class.java -> "upcoming"
            NotificationTranslation::class.java -> "translations"
            else -> ""
        }
        path += if(id != null) "/$id" else ""
        return path
    }

}

fun User.authenticate(password : String) : Boolean {
    val response = Unirest.post("${config.dbServiceEndpoint}/users/${this.id}/authenticate")
        .body(JSONObject("{ \"password\" : \"$password\"}"))
        .asString()
    return response.status == 200
}