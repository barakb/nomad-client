package com.gigaspaces.http

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.http.HttpResponse
import org.apache.http.client.methods.*
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.client.HttpAsyncClients
import java.io.Closeable
import java.net.URI


private val logger = KotlinLogging.logger {}

class HttpClient(configBuilder: HttpConfigBuilder) : Closeable {
    val config: Config = configBuilder.create()

    constructor(init: HttpConfigBuilder.() -> Unit) : this(HttpConfigBuilder().apply(init))


    val httpClient: CloseableHttpAsyncClient = config.builder.build().apply { start() }


    @Suppress("unused")
    suspend inline fun <reified T> get(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(HttpGet())(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> put(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(HttpPut())(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> post(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(HttpPost())(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> head(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(HttpHead())(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> delete(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(HttpDelete())(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> options(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(HttpOptions())(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> trace(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(HttpTrace())(init)
    }

    override fun close() {
        httpClient.close()
    }

    inline fun <reified T> execute(req: HttpRequestBase): suspend (init: RequestBuilder.() -> Unit) -> T = { init ->
        val request = config.defaultRequest.copy().apply(init).build()
        prepareHttpUriRequest(request, req)
        val response = withRetry(request.retry) { httpClient.execute(req) }
        extractResponse(response)
    }


    inline fun <reified T> extractResponse(response: HttpResponse): T {
        try {
            val typeInfo = typeInfo<T>()
            return when (T::class) {
                HttpResponse::class -> response as T
                String::class -> BasicResponseHandler().handleResponse(response) as T
                else -> BasicResponseHandler().handleResponse(response).reader().use { config.gson.fromJson(it, typeInfo.reifiedType) }
            }
        } catch (e: Exception) {
            println("got exception response is: $response")
            throw e
        }
    }

    fun prepareHttpUriRequest(request: Request, httpRequest: HttpRequestBase) {
        httpRequest.uri = URI.create(request.url)
        if (httpRequest is HttpEntityEnclosingRequestBase) {
            request.body?.let {
                val content = config.gson.toJson(it)
                logger.debug("${httpRequest.method} ${httpRequest.uri}: body = $content")
                httpRequest.entity = ByteArrayEntity(content.toByteArray())
            }
        }
        request.headers.forEach {
            httpRequest.setHeader(it.first, it.second)
        }
        logger.debug("sending $httpRequest")
    }
}

@DslMarker
annotation class DslHttpClient

class Config(val builder: HttpAsyncClientBuilder, val defaultRequest: RequestBuilder = RequestBuilder(), val gson: Gson = Gson())

@DslHttpClient
class HttpConfigBuilder {
    private var defaultRequest: RequestBuilder = RequestBuilder()
    private var gson = GsonBuilder()
    private var builder = HttpAsyncClients.custom()


    @Suppress("unused")
    fun defaultRequest(init: RequestBuilder.() -> Unit) {
        defaultRequest.apply(init)
    }

    @Suppress("unused")
    fun gson(init: GsonBuilder.() -> Unit) {
        gson = gson.apply(init)
    }

    @Suppress("unused")
    fun client(init: HttpAsyncClientBuilder.() -> Unit) {
        builder.apply(init)
    }

    internal fun create(): Config {
        return Config(builder, defaultRequest, gson.create())
    }
}


data class Request(val url: String, var body: Any? = null, val headers: Set<Pair<String, String>>, val retry: Retry)

@DslHttpClient
data class RequestBuilder(private val headers: MutableSet<Pair<String, String>> = mutableSetOf(),
                          private val queries: MutableSet<Pair<String, String>> = mutableSetOf(),
                          private var retry: Retry = RetryBuilder().build(),
                          var url: String? = null,
                          var body: Any? = null,
                          var path: String = ""
) {

    @Suppress("unused")
    fun header(name: String, value: String) {
        headers.add(name to value)
    }

    @Suppress("unused")
    fun param(name: String, value: Any?) {
        value?.let { queries.add(name to it.toString()) }
    }

    @Suppress("unused")
    fun retry(init: RetryBuilder.() -> Unit) {
        retry = RetryBuilder().apply(init).build()
    }

    fun build(): Request {
        val combined = url?.let { combineUrl(it, path) }
                ?: throw java.lang.IllegalArgumentException("Bad request, url is not set")
        val withQueries = appendQueries(combined, queries)
        return Request(withQueries, body, headers, retry)
    }

    private fun appendQueries(url: String, queries: Set<Pair<String, String>>): String {
        return queries.foldIndexed(url, { i, acc, query ->
            val connector = if (i == 0) "?" else "&"
            "$acc$connector${query.first}=${query.second}"
        })
    }

    private fun combineUrl(url: String, path: String): String {
        if (path.isEmpty()) return url
        return if (url.endsWith("/") || path.endsWith("/")) {
            "$url$path"
        } else {
            "$url/$path"
        }
    }
}

data class Retry(val times: Int, val initialDelay: Long, val maxDelay: Long)

@DslHttpClient
class RetryBuilder {
    var times: Int = 1
    var initialDelay: Long = 1000
    var maxDelay: Long = 30_000

    fun build(): Retry {
        return Retry(times, initialDelay, maxDelay)
    }
}

// https://proandroiddev.com/writing-dsls-in-kotlin-part-2-cd9dcd0c4715
// https://kotlinlang.org/docs/reference/type-safe-builders.html


fun main(): Unit = runBlocking {
    HttpClient {
        defaultRequest {
            url = "http://httpbin.org/"
            header("name", "value")
            param("v", "f")
            retry { times = 1 }
        }
    }.use { client ->
        val headers = client.get<JsonObject> {
            path = "headers"
        }
        println("headers: $headers")
    }
}