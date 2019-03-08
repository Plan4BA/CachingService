package de.ddkfm.plan4ba

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.de.ddkfm.plan4ba.utils.toModel
import de.ddkfm.plan4ba.models.*
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

    Unirest.setHttpClient(makeHttpClient())
    port(8080)

    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate(CachingSchedule(), 5, 10, TimeUnit.SECONDS)

    post("/trigger", ::triggerCaching)
    get("/all") { _, _ -> LectureCaller.instance.fillOvernightQueue(); ""}
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
        e.printStackTrace()
    } catch (e: KeyStoreException) {
        e.printStackTrace()
    } catch (e: KeyManagementException) {
        e.printStackTrace()
    }


    return httpclient
}