package com.gigaspaces.nomad

import com.gigaspaces.http.HttpClient
import com.gigaspaces.http.HttpConfigBuilder
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.ssl.SSLContexts
import java.io.Closeable


class NomadConfigBuilder {
    @Suppress("MemberVisibilityCanBePrivate")
    var httpConfigBuilder = HttpConfigBuilder()

    @Suppress("unused")
    fun nomadToken(token: String) {
        httpConfigBuilder.defaultRequest { header("X-Nomad-Token", token) }
    }

    @Suppress("unused")
    fun httpConfig(init: HttpConfigBuilder.() -> Unit) {
        httpConfigBuilder.apply(init)
    }
}

private val logger = KotlinLogging.logger {}

class NomadClient(init: NomadConfigBuilder.() -> Unit) : Closeable {
    private val configBuilder = NomadConfigBuilder().apply(init)
    private val httpClient = HttpClient(configBuilder.httpConfigBuilder)

    @Suppress("unused")
    val jobs = Jobs(httpClient)

    @Suppress("unused")
    val allocations = Allocations(httpClient)

    @Suppress("unused")
    val nodes = Nodes(httpClient)

    override fun close() {
        httpClient.close()
    }
}


class Nodes(private val client: HttpClient) {
    @Suppress("unused")
    suspend fun list(prefix: String? = null): List<Node> {
        val queryParam = prefix?.let { "?prefix=$it" } ?: ""
        return client.get {
            path = "nodes$queryParam"
        }
    }
}

class Allocations(private val client: HttpClient) {

    @Suppress("unused")
    suspend fun list(prefix: String? = null): List<Allocation> {
        val queryParam = prefix?.let { "?prefix=$it" } ?: ""
        return client.get {
            path = "allocations$queryParam"
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
    suspend fun stop(jobId: String, purge: Boolean = false): EvaluationResponse {
        return client.delete {
            path = "job/$jobId/?purge=$purge"
        }
    }

    @Suppress("unused")
    suspend fun update(job: Job, enforceIndex: Boolean = false, jobModifyIndex: Int = 0, policyOverride: Boolean = false): EvaluationResponse {
        return client.post {
            path = "job/${job.id}"
            body = hashMapOf(("Job" to job),
                    ("EnforceIndex" to enforceIndex),
                    ("JobModifyIndex" to jobModifyIndex),
                    ("PolicyOverride" to policyOverride))
        }
    }

    @Suppress("unused")
    suspend fun list(): List<JobDesc> {
        return client.get {
            path = "jobs"
        }
    }

    @Suppress("unused")
    suspend fun read(jobId: String): Job {
        return client.get {
            path = "job/$jobId"
        }
    }

    @Suppress("unused")
    suspend fun allocations(jobId: String, all: Boolean = false): List<Allocation> {
        return client.get {
            path = "job/$jobId/allocations?all=${all}"
        }
    }

    @Suppress("unused")
    suspend fun deployments(jobId: String, all: Boolean = false): List<Deployment> {
        return client.get {
            path = "job/$jobId/deployments?all=${all}"
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

val job = JobBuilder().apply {
    id = "foo"
    name = "foo"
    group {
        name = "bar"
        repeat(10) {
            task {
                raw_exec {
                    command = "/home/barak/dev/kotlin/nomad-client/job.sh"
                }
                name = "myTask$it"
            }
        }
    }
}.build()


//http://127.0.0.1:4646/ui/clients
fun main(): Unit = runBlocking {
    NomadClient {
        httpConfig {
            client {
                @Suppress("DEPRECATION")
                setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                        .setSSLContext(SSLContexts.custom().loadTrustMaterial(null, TrustStrategy { _, _ -> true }).build())
            }
            defaultRequest {
                url = "http://127.0.0.1:4646/v1/"
            }
        }
//      nomadToken("fake-token")
    }.use { client ->
        val registrationResponse = client.jobs.create(job)
        logger.info("registrationResponse is $registrationResponse")
        val jobs = client.jobs.list()
        logger.info("jobs are $jobs")
//        logger.info("read(foo) = ${client.jobs.read("foo")}")
//        logger.info("allocations(foo) = ${client.jobs.allocations("foo")}")
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