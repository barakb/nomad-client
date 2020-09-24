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
        val request: RequestBuilder = config.defaultRequest.copy().apply(init)
        prepareHttpUriRequest(request, req)
        val response = httpClient.execute(req)
        extractResponse(response)
    }


    inline fun <reified T> extractResponse(response: HttpResponse): T {
        val typeInfo = typeInfo<T>()
        return when (T::class) {
            HttpResponse::class -> response as T
            String::class -> BasicResponseHandler().handleResponse(response) as T
            else -> BasicResponseHandler().handleResponse(response).reader().use { config.gson.fromJson(it, typeInfo.reifiedType) }
        }
    }

    fun prepareHttpUriRequest(request: RequestBuilder, httpRequest: HttpRequestBase) {
        request.url?.let {
            httpRequest.uri = URI.create(it + request.path)
        } ?: throw IllegalArgumentException("request $request url can not be null")
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
    }
}

open class HttpConfigBuilder {
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

@Suppress("ClassName")
class Config(val builder: HttpAsyncClientBuilder, val defaultRequest: RequestBuilder = RequestBuilder(), val gson: Gson = Gson())


data class RequestBuilder(var url: String? = null,
                          var body: Any? = null,
                          val headers: MutableSet<Pair<String, String>> = mutableSetOf(),
                          var path: String = "") {
    fun header(name: String, value: String) {
        headers.add(name to value)
    }
}


fun main(): Unit = runBlocking {
    HttpClient {
        defaultRequest {
            url = "http://httpbin.org/"
            header("name", "value")
        }
    }.use { client ->
        val headers = client.get<JsonObject> {
            path = "headers"
        }
        println("headers: $headers")
    }
}