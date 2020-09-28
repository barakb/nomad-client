package com.gigaspaces.http

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.concurrent.FutureCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun CloseableHttpAsyncClient.execute(request: SimpleHttpRequest): SimpleHttpResponse {
    return suspendCancellableCoroutine { continuation ->
        execute(request, object : FutureCallback<SimpleHttpResponse> {
            override fun cancelled() {
                continuation.cancel()
            }

            override fun completed(response: SimpleHttpResponse) {
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
