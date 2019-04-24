package de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.JsonNode
import com.mashape.unirest.request.GetRequest
import com.mashape.unirest.request.HttpRequestWithBody
import com.mashape.unirest.request.body.RequestBodyEntity
import io.swagger.annotations.ApiImplicitParam
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*


fun mapDataTypes(pair : Pair<ApiImplicitParam, String>) : Any {
    val returnValue = when(pair.first.dataType.toLowerCase()) {
        "integer" -> pair.second.toIntOrNull()
        "long" -> pair.second.toLongOrNull()
        "boolean" -> pair.second.toBoolean()
        "double" -> pair.second.toDoubleOrNull()
        else -> pair.second
    }
    return returnValue ?: getDefaultValue(pair.first.dataType.toLowerCase())
}

fun getDefaultValue(type : String) : Any {
    return when(type) {
        "integer", "long", "double" -> -1
        else -> ""
    }
}


fun getEnvOrDefault(key : String, default : String) : String {
    return System.getenv(key) ?: default
}

fun Long.toLocalDateTime() : LocalDateTime {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this),
            TimeZone.getDefault().toZoneId())
    return dateTime
}

fun LocalDateTime.toMillis() : Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Any.toJson() : String {
    return jacksonObjectMapper().writeValueAsString(this)
}
fun <T> JSONObject.toModel(type : Class<T>) : T {
    return jacksonObjectMapper().readValue(this.toString(), type)
}

fun HttpResponse<JsonNode>.mapModel(type : Class<*>) : Pair<Int, Any> {
    return this.status to when(this.status) {
        in 200..299 -> {
            val body = this.body
            if(body.isArray)
                body.array.map { (it as JSONObject).toModel(type) }
            else
                body.`object`.toModel(type)
        }
        else -> this.body.`object`
    }
}
fun GetRequest.toModel(type : Class<*>) : Pair<Int, Any> = this.asJson().mapModel(type)
fun HttpRequestWithBody.toModel(type : Class<*>) : Pair<Int, Any> = this.asJson().mapModel(type)
fun RequestBodyEntity.toModel(type : Class<*>) : Pair<Int, Any> = this.asJson().mapModel(type)

inline fun <reified T> String.toModel() : T? {
    return  jacksonObjectMapper().readValue(this, T::class.java)
}
inline fun <reified T> String.toListModel() : List<T>? {
    return  jacksonObjectMapper().readValue(this, jacksonObjectMapper().typeFactory.constructCollectionType(List::class.java, T::class.java))
}

fun String.encode() : String = URLEncoder.encode(this, "UTF-8")

fun LocalTime.isWithinInterval(start : LocalTime, end : LocalTime) : Boolean {
    return if (start.isAfter(end)) {
        this >= start || this < end
    } else {
        start <= this && this < end
    }
}