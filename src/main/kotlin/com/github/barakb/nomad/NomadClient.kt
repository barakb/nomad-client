package com.github.barakb.nomad

import com.github.barakb.http.HttpClient
import com.github.barakb.http.HttpConfigBuilder
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy
import org.apache.hc.core5.http.ssl.TLS
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.pool.PoolConcurrencyPolicy
import org.apache.hc.core5.pool.PoolReusePolicy
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import java.io.Closeable
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


private val logger = KotlinLogging.logger {}

class NomadClient(init: NomadConfigBuilder.() -> Unit) : Closeable {
    private val config = NomadConfigBuilder().apply(init).build()
    private val httpClient = createHttpClient(config)

    @Suppress("unused")
    val jobs = Jobs(httpClient)

    @Suppress("unused")
    val allocations = Allocations(httpClient)

    @Suppress("unused")
    val nodes = Nodes(httpClient)

    @Suppress("unused")
    val evaluations = Evaluations(httpClient)

    @Suppress("unused")
    val deployments = Deployments(httpClient)

    override fun close() {
        httpClient.close()
    }

    private fun createHttpClient(config: NomadConfig): HttpClient {
        val connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(ClientTlsStrategyBuilder.create()
                        .setSslContext(SSLContexts.createSystemDefault())
                        .setTlsVersions(TLS.V_1_3, TLS.V_1_2)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .setSslContext(SSLContextBuilder().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build())
                        .build())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setConnectionTimeToLive(TimeValue.ofMinutes(1L))
                .build()

        val httpClientBuilder = HttpConfigBuilder()
                .apply {
                    defaultRequest {
                        url = if (config.address.endsWith("/")) "${config.address}v1/" else "${config.address}/v1/"
                        config.authToken?.let { header("X-Nomad-Token", it) }
                        param("region", config.region)
                        header("Content-Type", "application/json")
                    }
                }.apply {
                    client {
                        setConnectionManager(connectionManager)
                        setDefaultRequestConfig(RequestConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .setResponseTimeout(Timeout.ofSeconds(5))
                                .setCookieSpec(StandardCookieSpec.STRICT)
                                .build())
                        setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
                    }
                }
        return HttpClient(httpClientBuilder)
    }

    class Nodes(private val client: HttpClient) {
        @Suppress("unused")
        suspend fun list(prefix: String? = null): List<Node> {
            return client.get {
                path = "nodes"
                param("prefix", prefix)
            }
        }
    }

    class Deployments(private val client: HttpClient) {
        @Suppress("unused")
        suspend fun list(index: BigInteger? = null, prefix: String? = null, wait: String? = null): List<Deployment> {
            return client.get {
                path = "deployments"
                param("index", index)
                param("prefix", prefix)
                param("wait", wait)
            }
        }

        @Suppress("unused")
        suspend fun read(id: String, index: BigInteger? = null, wait: String? = null): Deployment? {
            return client.get {
                path = "deployment/$id"
                param("index", index)
                param("wait", wait)
            }
        }

        @Suppress("unused")
        suspend fun fail(id: String): EvaluationResponse {
            return client.post {
                path = "deployment/fail/$id"
            }
        }
    }

    class Evaluations(private val client: HttpClient) {
        @Suppress("unused")
        suspend fun read(id: String, index: BigInteger? = null, wait: String? = null): Evaluation {
            return client.get {
                path = "evaluation/$id"
                param("index", index)
                param("wait", wait)
            }
        }

        @Suppress("unused")
        suspend fun allocations(id: String, index: BigInteger? = null, wait: String? = null): List<Allocation> {
            return client.get {
                path = "evaluation/$id/allocations"
                param("index", index)
                param("wait", wait)
            }
        }
    }

    class Allocations(private val client: HttpClient) {

        @Suppress("unused")
        suspend fun list(prefix: String? = null): List<Allocation> {
            return client.get {
                path = "allocations"
                param("prefix", prefix)
            }
        }

        @Suppress("unused")
        suspend fun read(id: String): Allocation {
            return client.get {
                path = "allocation/$id"
            }
        }

        @Suppress("unused")
        suspend fun stop(id: String): EvaluationResponse {
            return client.post {
                path = "allocation/$id/stop"
            }
        }
    }

    inner class Jobs(private val client: HttpClient) {
        @Suppress("unused")
        suspend fun create(init: JobBuilder.() -> Unit): EvaluationResponse {
            return create(JobBuilder().apply(init).build())
        }

        @Suppress("unused")
        suspend fun create(job: Job): EvaluationResponse {
            return client.post {
                path = "jobs"
                body = hashMapOf(("Job" to job))
            }
        }

        @Suppress("unused")
        suspend fun stop(jobId: String, purge: Boolean? = null): EvaluationResponse {
            return client.delete {
                path = "job/$jobId"
                param("purge", purge)

            }
        }

        @Suppress("unused")
        suspend fun update(job: Job, enforceIndex: Boolean? = null, jobModifyIndex: Int = 0, policyOverride: Boolean = false): EvaluationResponse {
            return client.post {
                path = "job/${job.id}"
                body = hashMapOf(("Job" to job),
                        ("EnforceIndex" to enforceIndex),
                        ("JobModifyIndex" to jobModifyIndex),
                        ("PolicyOverride" to policyOverride))
            }
        }

        @Suppress("unused")
        suspend fun list(prefix: String? = null): List<Job> {
            return client.get {
                path = "jobs"
                param("prefix", prefix)
            }
        }

        @Suppress("unused")
        suspend fun read(jobId: String): Job {
            return client.get {
                path = "job/$jobId"
            }
        }

        @Suppress("unused")
        suspend fun plan(job: Job, diff: Boolean?, policyOverride: Boolean): JobPlanResponse {
            return client.post {
                path = "job/${job.id}/plan"
                body = JobPlanRequest(job, diff, policyOverride)
            }
        }

        @Suppress("unused")
        suspend fun allocations(jobId: String, all: Boolean? = null, index: BigInteger? = null, wait: String? = null): List<Allocation> {
            return client.get {
                path = "job/$jobId/allocations"
                param("all", all)
                param("index", index)
                param("wait", wait)
            }
        }

        @Suppress("unused")
        suspend fun evaluations(jobId: String, index: BigInteger? = null, wait: String? = null): List<Evaluation> {
            return client.get {
                path = "job/$jobId/evaluations"
                param("index", index)
                param("wait", wait)
            }
        }

        @Suppress("unused")
        suspend fun deployments(jobId: String, all: Boolean? = null, index: BigInteger? = null, wait: String? = null): List<Deployment> {
            return client.get {
                path = "job/$jobId/deployments"
                param("all", all)
                param("index", index)
                param("wait", wait)
            }
        }

        @Suppress("unused")
        suspend fun deployment(jobId: String): Deployment {
            return client.get {
                path = "job/$jobId/deployment"
            }
        }

        @Suppress("unused")
        suspend fun summary(jobId: String): JobSummary {
            return client.get {
                path = "job/$jobId/summary"
            }
        }

        private fun Deployment.isHealthy(): Boolean {
            return status == "successful" && taskGroups.values.all { it.unhealthyAllocs == 0L }
        }

        private suspend fun getLastDeployment(jobId: String, jobModifyIndex: BigInteger): Deployment? {
            val deployment = deployment(jobId)
            return if (jobModifyIndex == deployment.jobSpecModifyIndex) deployment else null
        }

        @Suppress("unused")
        private suspend fun getLastDeploymentIfHealthy(jobId: String): Deployment? {
            val job = jobs.read(jobId)
            val deployment = getLastDeployment(job.id, job.jobModifyIndex!!)
            if (deployment != null) {
                if (deployment.isHealthy()) {
                    logger.debug("job [$jobId] deployment - Healthy ${deployment.id} ${deployment.status} [${deployment.statusDescription}]")
                    return deployment
                } else {
                    val unHealthy = deployment.taskGroups.values.map { it.unhealthyAllocs }.sum()
                    logger.debug("job [$jobId] deployment - Un-Healthy ${deployment.id} ${deployment.status}:($unHealthy) [${deployment.statusDescription}]")
                }
            } else {
                logger.debug("there is no active deployment for job $jobId")
            }
            return null
        }

        @ExperimentalTime
        suspend fun getLastDeploymentIfHealthy(jobId: String, wait: Duration = 0.seconds): Deployment? {
            var remaining = wait.inMilliseconds.toLong()
            val delay = 5000L
            while (true) {
                while (true) {
                    val deployment = getLastDeploymentIfHealthy(jobId)
                    if (deployment != null) {
                        return deployment
                    }
                    if (0 < remaining) {
                        val d = delay.coerceAtMost(remaining)
                        delay(d)
                        remaining -= d
                    } else {
                        return null
                    }
                }
            }
        }
    }
}

@DslMarker
annotation class DslNomadClient

@Suppress("MemberVisibilityCanBePrivate")
@DslNomadClient
class NomadConfigBuilder {
    var address: String? = null
    var region: String? = null
    var authToken: String? = null

    fun build(): NomadConfig {
        val address = address ?: throw IllegalArgumentException("address must be set")
        return NomadConfig(address, region, authToken)
    }
}

data class NomadConfig(
        var address: String,
        val region: String?,
        val authToken: String? = null,
)


