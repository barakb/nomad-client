package com.gigaspaces.nomad

import com.gigaspaces.http.HttpClient
import com.gigaspaces.http.HttpConfigBuilder
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.ssl.SSLContextBuilder
import java.io.Closeable
import java.math.BigInteger


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

    @Suppress("unused")
    val monitor = Monitor(this)

    override fun close() {
        httpClient.close()
    }

    private fun createHttpClient(config: NomadConfig): HttpClient {
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
                        if (config.address.toLowerCase().startsWith("https:")) {
                            setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            setSSLContext(SSLContextBuilder().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build())
                        }
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

    class Jobs(private val client: HttpClient) {
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


val job = JobBuilder().apply {
    id = "my_job_id"
    name = "my_job"
    repeat(4) { g ->
        group {
            name = "my_group_$g"
            repeat(2) {
                task {
                    raw_exec {
                        command = if (g != 0) "/home/barak/dev/kotlin/nomad-client/job.sh" else "/not_found.sh"
//                        command = "/home/barak/dev/kotlin/nomad-client/job.sh"
                    }
                    name = "myTask_${g}_$it"
                }
            }
//            restart {
//                attempts = 1
//                delay = 0
//                interval = 5_000_000_000
//                mode = "fail"
//            }
        }
    }
}.build()

//@Suppress("DEPRECATION")

//http://127.0.0.1:4646/ui/clients
fun main(): Unit = runBlocking {

    NomadClient {
        address = "http://127.0.0.1:4646"
        authToken = "my-fake-token"
    }.use { client ->
        val registrationResponse = client.jobs.create(job)
        logger.info("registrationResponse is $registrationResponse")
        client.monitor.watch(job.id)
//        val jobs = client.jobs.list()
//        logger.info("jobs are $jobs")
////        logger.info("read(foo) = ${client.jobs.read("foo")}")
//        logger.info("allocations(foo) = ${client.jobs.allocations(jobId = "foo", index = registrationResponse.index)}")
//        logger.info("deployments(foo) = ${client.jobs.deployments("foo")}")
//        logger.info("deployment(foo) = ${client.jobs.deployment("foo")}")
//        logger.info("summary(foo) = ${client.jobs.summary("foo")}")
//        val allocations = client.allocations.list()
//        allocations.forEach {
//            it.id?.let { id -> logger.info("client.allocation.read($id) = ${client.allocations.read(id)}") }
//        }
//        val nodes= client.nodes.list()
//        logger.info("nodes are $nodes")
    }
}