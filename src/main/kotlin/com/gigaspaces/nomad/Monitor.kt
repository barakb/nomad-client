package com.gigaspaces.nomad

import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Monitor(private var client: NomadClient) {

    private suspend fun ongoingEvaluations(job: Job): List<Evaluation> {
        return client.jobs.evaluations(job.id, job.modifyIndex).filter { it.status != "complete" }
    }

    private suspend fun getLasDeployment(jobId: String): Deployment? {
        return client.jobs.deployments(jobId = jobId).maxByOrNull { it.createIndex }
    }

    enum class MonitorResult {
        Evaluating,
        Deploying,
        Unhealthy,
        Healthy,
        DeploymentFailed
    }

    /**
     * If there is an incomplete evaluation it means that there should be allocation and deployment for this job
     * so we should wait.
     * otherwise check last deployment, if it successful check that all the tasks are healthy and we are done.
     */
    private suspend fun watchOnce(jobId: String): MonitorResult {
        val job = client.jobs.read(jobId)
        val ongoingEvaluations = ongoingEvaluations(job)
        if (ongoingEvaluations.isNotEmpty()) {
            ongoingEvaluations.forEach {
                logger.info("Evaluation ${it.id} triggered by ${it.triggeredBy} has status of ${it.status}")
                it.status
            }
            logger.info("There is on going evaluation related to this job, hence the job is not running")
            val associatedAllocations = ongoingEvaluations.flatMap { client.evaluations.allocations(it.id) }
            logger.info("This evaluation has ${associatedAllocations.size} allocations")
            associatedAllocations.forEach {
                logger.info("allocation ${it.id} related to ${it.evalId} $it")
            }
            return MonitorResult.Evaluating
        }

        val lastDeployment = getLasDeployment(jobId)
        if (lastDeployment != null) {
            logger.info("job has a last deployment is ${lastDeployment.id} of status ${lastDeployment.status} [${lastDeployment.statusDescription}]")
            lastDeployment.taskGroups.forEach { (groupName, deploymentState) ->
                logger.info("   last deployment group $groupName jobs are [healthy:${deploymentState.healthyAllocs}, unhealthy:${deploymentState.unhealthyAllocs}] }")
            }
            when (lastDeployment.status) {
                "running" -> return MonitorResult.Deploying
                "cancelled" -> return MonitorResult.Deploying
                "failed" -> return MonitorResult.DeploymentFailed
                "successful" -> return if (isHealthy(lastDeployment)) MonitorResult.Healthy else MonitorResult.Unhealthy

            }
        } else {
            logger.info("No deployment found for job ${job.id}")
            return MonitorResult.Deploying
        }
        return MonitorResult.Healthy
    }

    private fun isHealthy(deployment: Deployment): Boolean {
        return deployment.status == "successful" && deployment.taskGroups.values.all { it.unhealthyAllocs == 0L }
    }

    suspend fun watch(jobId: String): MonitorResult {
        while (true) {
            when (watchOnce(jobId)) {
                MonitorResult.Healthy -> return MonitorResult.Healthy
                MonitorResult.DeploymentFailed -> return MonitorResult.DeploymentFailed
                else -> {
                    delay(5000)
                    logger.info("\n\n\n\n")
                }
            }
        }
    }
}