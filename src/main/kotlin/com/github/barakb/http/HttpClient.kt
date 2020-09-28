package com.github.barakb.http

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.io.CloseMode
import java.io.Closeable
import java.net.URI


private val logger = KotlinLogging.logger {}

class HttpClient(configBuilder: HttpConfigBuilder) : Closeable {
    val config: Config = configBuilder.create()

    constructor(init: HttpConfigBuilder.() -> Unit) : this(HttpConfigBuilder().apply(init))


    val httpClient: CloseableHttpAsyncClient = config.builder.build().apply { start() }


    @Suppress("unused")
    suspend inline fun <reified T> get(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.GET)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> put(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.PUT)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> post(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.POST)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> head(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.HEAD)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> delete(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.DELETE)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> options(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.OPTIONS)(init)
    }

    @Suppress("unused")
    suspend inline fun <reified T> trace(noinline init: RequestBuilder.() -> Unit): T {
        return execute<T>(RequestType.TRACE)(init)
    }

    override fun close() {
        httpClient.close(CloseMode.GRACEFUL)
    }

    inline fun <reified T> execute(requestType: RequestType): suspend (init: RequestBuilder.() -> Unit) -> T = { init ->
        val request = config.defaultRequest.copy().apply(init).build(requestType)
        val httpRequest = prepareHttpUriRequest(request)
        val response = httpClient.execute(httpRequest)
        extractResponse(response)
    }


    inline fun <reified T> extractResponse(response: SimpleHttpResponse): T {
        val typeInfo = typeInfo<T>()
        val status = response.code
        if (status == 404) {
            if (typeInfo.kotlinType?.isMarkedNullable == true) {
                return null as T
            }
        }
        return when (T::class) {
            HttpResponse::class -> response as T
            String::class -> response.bodyText as T
            else -> config.gson.fromJson(response.bodyText, typeInfo.reifiedType)
        }
    }

    fun prepareHttpUriRequest(request: Request): SimpleHttpRequest {
        val httpRequest = httpRequest(request)
        httpRequest.uri = URI.create(request.url)
        request.body?.let {
            val content = config.gson.toJson(it)
            logger.debug("${httpRequest.method} ${httpRequest.uri}: body = $content")
            httpRequest.setBody(content.toByteArray(), ContentType.APPLICATION_JSON)
        }
        request.headers.forEach {
            httpRequest.setHeader(it.first, it.second)
        }
        logger.debug("sending $httpRequest")
        return httpRequest
    }

    private fun httpRequest(request: Request): SimpleHttpRequest {
        return when (request.type) {
            RequestType.GET -> SimpleHttpRequests.get(request.url)
            RequestType.PUT -> SimpleHttpRequests.put(request.url)
            RequestType.POST -> SimpleHttpRequests.post(request.url)
            RequestType.DELETE -> SimpleHttpRequests.delete(request.url)
            RequestType.HEAD -> SimpleHttpRequests.head(request.url)
            RequestType.TRACE -> SimpleHttpRequests.trace(request.url)
            RequestType.OPTIONS -> SimpleHttpRequests.options(request.url)
        }
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

enum class RequestType {
    GET,
    PUT,
    POST,
    DELETE,
    HEAD,
    TRACE,
    OPTIONS
}

data class Request(val type: RequestType, val url: String, var body: Any? = null, val headers: Set<Pair<String, String>>)

@DslHttpClient
class RequestBuilder(private val headers: MutableSet<Pair<String, String>> = mutableSetOf(),
                     private val queries: MutableSet<Pair<String, String>> = mutableSetOf(),
                     var url: String? = null,
                     var body: Any? = null,
                     var path: String = ""
) {

    fun copy(): RequestBuilder {
        return RequestBuilder(headers.toMutableSet(), queries.toMutableSet(), url, body, path)
    }

    fun build(type: RequestType): Request {
        val combined = url?.let { combineUrl(it, path) }
                ?: throw java.lang.IllegalArgumentException("Bad request, url is not set")
        val withQueries = appendQueries(combined, queries)
        return Request(type, withQueries, body, headers)
    }

    @Suppress("unused")
    fun header(name: String, value: String) {
        headers.add(name to value)
    }

    @Suppress("unused")
    fun param(name: String, value: Any?) {
        value?.let { queries.add(name to it.toString()) }
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

// https://proandroiddev.com/writing-dsls-in-kotlin-part-2-cd9dcd0c4715
// https://kotlinlang.org/docs/reference/type-safe-builders.html


fun main(): Unit = runBlocking {
    HttpClient {
        defaultRequest {
            url = "http://httpbin.org/"
            header("name", "value")
            param("v", "f")
        }
    }.use { client ->
        val headers = client.get<JsonObject> {
            path = "headers"
        }
        println("headers: $headers")
    }
}