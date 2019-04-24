package de.ddkfm.plan4ba

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toModel
import de.ddkfm.plan4ba.models.*
import io.sentry.event.Event
import org.apache.http.client.HttpClient
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContextBuilder
import org.json.JSONObject
import spark.Request
import spark.Response
import spark.Spark.*
import spark.kotlin.get
import java.net.InetAddress
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val config = Config()
fun main(args : Array<String>) {
    config.buildFromEnv()
    port(System.getenv("HTTP_PORT")?.toIntOrNull() ?: 8080)
    Unirest.setHttpClient(makeHttpClient())

    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate(CachingSchedule(), 5, 10, TimeUnit.SECONDS)

    post("/trigger", ::triggerCaching)
    get("/all") { _, _ -> LectureCaller.instance.fillOvernightQueue(); ""}

    val dsn = System.getenv("SENTRY_DSN")
    dsn?.let {
        println("DSN $dsn joined")
        SentryTurret.log {
            addTag("Service", "CachingService")
        }.event {
            withMessage("CachingService ${InetAddress.getLocalHost().hostName} joined")
            withLevel(Event.Level.INFO)
        }
    }
}

fun triggerCaching(req : Request, resp : Response) : String {
    resp.type("application/json")
    val job =req.body().toModel<LectureJob>()
        ?: return BadRequest("bad HTTP-body").toJson()
    val caller = LectureCaller.instance
    caller.addJob(job)
    return OK().toJson()
}

fun makeHttpClient() : HttpClient? {
    val builder = SSLContextBuilder()
    var httpclient: CloseableHttpClient? = null
    try {
        // builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        builder.loadTrustMaterial(null, object : TrustStrategy {
            @Throws(CertificateException::class)
            override fun isTrusted(chain: Array<X509Certificate>, authType: String): Boolean {
                return true
            }
        })
        val sslsf = SSLConnectionSocketFactory(
            builder.build())
        httpclient = HttpClients.custom().setSSLSocketFactory(
            sslsf).build()
        println("custom httpclient called")
        System.out.println(httpclient)

    } catch (e: NoSuchAlgorithmException) {
        SentryTurret.log {

        }.capture(e)
    }


    return httpclient
}