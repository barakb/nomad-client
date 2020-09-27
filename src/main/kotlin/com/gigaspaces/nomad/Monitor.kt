package com.gigaspaces.nomad

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Monitor(private var client: NomadClient) {


    private suspend fun getJobEvaluation(job: Job): Evaluation? {
        val modifyIndex = job.jobModifyIndex!!
        return client.jobs.evaluations(job.id).filter {
            modifyIndex == it.jobModifyIndex
        }.maxByOrNull { it.createIndex }
    }

    private fun Deployment.isHealthy(): Boolean {
        return status == "successful" && taskGroups.values.all { it.unhealthyAllocs == 0L }
    }

    suspend fun watchOnce(jobId: String) {
        val job = client.jobs.read(jobId)
        val jobEvaluation = getJobEvaluation(job)
        if (jobEvaluation != null) {
            logger.info("found job evaluation status=${jobEvaluation.status} " +
                    "[${jobEvaluation.statusDescription}] triggeredBy=${jobEvaluation.triggeredBy} deploymentId=${jobEvaluation.deploymentId} ")
            if (jobEvaluation.deploymentId != null) {
                val deployment = client.deployments.read(jobEvaluation.deploymentId)
                if (deployment != null) {
                    if (deployment.isHealthy()) {
                        logger.info("Healthy deployment ${deployment.status} [${deployment.statusDescription}]")
                    } else {
                        logger.info("un Healthy ${deployment.status} [${deployment.statusDescription}]")
                    }
                } else {
                    logger.info("there is no active deployment for job $jobId")
                }
            }
        }
    }

    suspend fun watch(jobId: String): Nothing {
        while (true) {
            while (true) {
                watchOnce(jobId)
                delay(5000)
                logger.info("\n\n\n\n")

            }
        }
    }
}

fun main(): Unit = runBlocking {
    NomadClient {
        address = "http://127.0.0.1:4646"
        authToken = "my-fake-token"
    }.use { client ->
        val m = Monitor(client)
        m.watchOnce("my_job_id")
    }
}
