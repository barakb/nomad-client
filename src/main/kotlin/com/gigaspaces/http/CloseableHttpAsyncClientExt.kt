package com.gigaspaces.http

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun CloseableHttpAsyncClient.execute(request: HttpUriRequest): HttpResponse {
    return suspendCancellableCoroutine { continuation ->
        execute(request, object : FutureCallback<HttpResponse> {
            override fun cancelled() {
                continuation.cancel()
            }

            override fun completed(response: HttpResponse) {
                continuation.resume(response)
            }

            override fun failed(exception: Exception) {
                if (exception is CancellationException) {
                    continuation.cancel()
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        })
    }
}
