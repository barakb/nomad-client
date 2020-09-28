package com.github.barakb.nomad

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private val logger = KotlinLogging.logger("main")

val validJob = JobBuilder().apply {
    id = "my_job_id"
    name = "my_job"
    repeat(4) { g ->
        group {
            name = "my_group_$g"
            repeat(2) {
                task {
                    name = "myTask_${g}_$it"

                    raw_exec {
                        command = "/home/barak/dev/kotlin/nomad-client/job.sh"
                    }
                }
            }
        }
    }
}.build()

val invalidJob = JobBuilder().apply {
    id = "my_job_id"
    name = "my_job"
    repeat(4) { g ->
        group {
            name = "my_group_$g"
            repeat(2) {
                task {
                    name = "myTask_${g}_$it"

                    raw_exec {
                        command = if (g != 0) "/home/barak/dev/kotlin/nomad-client/job.sh" else "/not_found.sh"
                    }
                }
            }
        }
    }
}.build()


//http://127.0.0.1:4646/ui/clients
@ExperimentalTime
fun main(): Unit = runBlocking {
    NomadClient {
        address = "http://127.0.0.1:4646"
        authToken = "my-fake-token"
    }.use { client ->
        client.jobs.create(invalidJob)
        val deployment = client.jobs.getLastDeploymentIfHealthy(invalidJob.id, 30.seconds)
        if (deployment == null) {
            logger.info("reverting to a known valid job setup")
            client.jobs.create(validJob)
            val revertTo = client.jobs.getLastDeploymentIfHealthy(validJob.id, 30.seconds)
            logger.info("got deployment: $revertTo")
            val active = client.jobs.getLastDeploymentIfHealthy(validJob.id)
            logger.info("getLastDeploymentIfHealthy with zero waitTime returns $active")
        }
    }
}