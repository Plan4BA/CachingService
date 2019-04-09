package de.ddkfm.plan4ba

import io.sentry.SentryClient
import io.sentry.SentryClientFactory
import io.sentry.context.Context
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.EventBuilder
import io.sentry.event.UserBuilder

fun Context.user(username : String = "generic", email : String = "CachingService@plan4ba.ba-leipzig.de", data : Map<String, Any?> = emptyMap()) {
    this.user = UserBuilder()
        .setUsername(username)
        .setEmail(email)
        .setData(data)
        .build()
}

fun Context.breadcrumb(lambda : BreadcrumbBuilder.() -> Unit) {
    val builder = BreadcrumbBuilder()
    builder.lambda()
    this.recordBreadcrumb(builder.build())
}
fun SentryClient.capture(e : Throwable) {
    e.printStackTrace()
    this.sendException(e)
}
fun SentryClient.event(lambda : EventBuilder.() -> Unit) {
    val builder = EventBuilder()
    builder.lambda()
    this.sendEvent(builder.build())
}
object SentryTurret {
    fun log(lambda : Context.() -> Unit) : SentryClient {
        val client = SentryClientFactory.sentryClient()
        client.context.lambda()
        return client
    }
}