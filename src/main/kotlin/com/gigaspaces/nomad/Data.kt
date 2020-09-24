package com.gigaspaces.nomad

import com.google.gson.annotations.SerializedName
import java.math.BigInteger
import java.util.*


// https://stackoverflow.com/questions/44117970/kotlin-data-class-from-json-using-gson
data class EvaluationResponse(
        @SerializedName("EvalCreateIndex") val createIndex: BigInteger?,
        @SerializedName("EvalID") val id: String,
        @SerializedName("Index") val index: BigInteger,
        @SerializedName("JobModifyIndex") val jobModifyIndex: BigInteger?,
        @SerializedName("KnownLeader") val knownLeader: Boolean?,
        @SerializedName("LastContact") val lastContact: BigInteger?,
        @SerializedName("Warnings") val warnings: String?
)


class JobBuilder {
    var name: String? = null
    var id: String? = null

    private var datacenters: MutableList<String> = mutableListOf("dc1")
    private var groups: MutableList<Group> = mutableListOf()

    @Suppress("unused")
    fun datacenter(name: String) {
        datacenters.add(name)
    }

    @Suppress("unused")
    fun group(init: GroupBuilder.() -> Unit) {
        groups.add(GroupBuilder().apply(init).build())
    }

    internal fun build(): Job {
        val n = name ?: throw IllegalArgumentException("job name is missing $this")
        val i = id ?: throw IllegalArgumentException("job name is missing $this")
        return Job(n, i, datacenters, groups)
    }
}

data class Job(
        @SerializedName("Name") var name: String,
        @SerializedName("ID") var id: String,
        @SerializedName("Datacenters") var datacenters: List<String> = listOf(),
        @SerializedName("TaskGroups") var groups: List<Group> = listOf(),
)

class GroupBuilder {
    private val tasks: MutableList<Task> = mutableListOf()
    var name: String? = null

    fun task(init: TaskBuilder.() -> Unit) {
        tasks.add(TaskBuilder().apply(init).build())
    }

    internal fun build(): Group {
        val name = this.name ?: throw IllegalArgumentException("group command is missing $this")
        return Group(name, tasks)
    }
}

data class Group(
        @SerializedName("Name") val name: String,
        @SerializedName("Tasks") val tasks: List<Task> = listOf(),
)

class TaskBuilder {
    var name: String? = null
    private var driver: String? = null
    private var config: Any? = null
    private var resources: Resources? = null

    @Suppress("unused", "FunctionName")
    fun raw_exec(init: ConfigRawExecBuilder.() -> Unit) {
        config = ConfigRawExecBuilder().apply(init).build()
        if (config != ConfigRawExec()) {
            driver = "raw_exec"
        }
    }

    @Suppress("unused")
    fun resource(init: ResourceBuilder.() -> Unit) {
        resources = ResourceBuilder().apply(init).build()
    }

    internal fun build(): Task {
        val name = this.name ?: throw IllegalArgumentException("task name is missing $this")
        val driver = this.driver ?: throw IllegalArgumentException("task driver is missing $this")
        val config = this.config ?: throw IllegalArgumentException("task config is missing $this")
        return Task(name, driver, config, resources)
    }

}

data class Task(
        @SerializedName("Name") val name: String,
        @SerializedName("Driver") val driver: String,
        @SerializedName("Config") val config: Any,
        @SerializedName("Resources") val resources: Resources? = null,
)

class ConfigRawExecBuilder {
    @Suppress("MemberVisibilityCanBePrivate")
    var command: String? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var args: List<String> = listOf()

    internal fun build(): ConfigRawExec {
        val cmd = command ?: throw IllegalArgumentException("config command is missing $this")
        return ConfigRawExec(cmd, args)
    }
}

data class ConfigRawExec(
        val command: String? = null,
        val args: List<String> = listOf(),
)

class ResourceBuilder {
    @Suppress("MemberVisibilityCanBePrivate")
    var cpu: Int? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var memory: Int? = null

    internal fun build(): Resources? {
        return if (cpu != null || memory != null) {
            Resources(cpu, memory)
        } else {
            null
        }
    }
}

data class JobDesc(
        @SerializedName("ID") val id: String? = null,
        @SerializedName("ParentID") val parentId: String? = null,
        @SerializedName("Name") val name: String? = null,
        val datacenters: List<String>? = null,
        @SerializedName("Type") val type: String? = null,
        @SerializedName("Priority") val priority: Int = 0,
        @SerializedName("Periodic")
        val periodic: Boolean = false,
        @SerializedName("ParameterizedJob") val parameterizedJob: Boolean = false,
        @SerializedName("Stop") val stop: Boolean = false,
        @SerializedName("Status") val status: String? = null,
        @SerializedName("StatusDescription") val statusDescription: String? = null,
        private val jobSummary: JobSummary? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger? = null,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger? = null,
        @SerializedName("JobModifyIndex") val jobModifyIndex: BigInteger? = null,
        @SerializedName("SubmitTime") val submitTime: Long = 0
)

data class JobSummary(
        @SerializedName("JobID") var jobId: String? = null,
        @SerializedName("Namespace") var namespace: String? = null,
        private val summary: Map<String, TaskGroupSummary>? = null,
        private val children: JobChildrenSummary? = null,
        @SerializedName("CreateIndex") var createIndex: BigInteger? = null,
        @SerializedName("ModifyIndex") var modifyIndex: BigInteger? = null,
)

data class TaskGroupSummary(
        @SerializedName("Queued") val queued: Long = 0,
        @SerializedName("Complete") val complete: Long = 0,
        @SerializedName("Failed") val failed: Long = 0,
        @SerializedName("Running") val running: Long = 0,
        @SerializedName("Starting") val starting: Long = 0,
        @SerializedName("Lost") val lost: Long = 0
)

data class JobChildrenSummary(
        @SerializedName("Pending") val pending: Long = 0,
        @SerializedName("Running") val running: Long = 0,
        @SerializedName("Dead") val dead: Long = 0
)

data class Allocation(
        @SerializedName("ID") val id: String? = null,
        @SerializedName("EvalID") val evalId: String? = null,
        @SerializedName("Name") val name: String? = null,
        @SerializedName("Namespace") val namespace: String? = null,
        @SerializedName("NodeID") val nodeId: String? = null,
        @SerializedName("NodeName") val nodeName: String? = null,
        @SerializedName("JobID") val jobId: String? = null,
        @SerializedName("JobType") val jobType: String? = null,
        @SerializedName("JobVersion") val jobVersion: BigInteger? = null,
        @SerializedName("TaskGroup") val taskGroup: String? = null,
        @SerializedName("DesiredStatus") val desiredStatus: String? = null,
        @SerializedName("DesiredDescription") val desiredDescription: String? = null,
        @SerializedName("ClientStatus") val clientStatus: String? = null,
        @SerializedName("ClientDescription") val clientDescription: String? = null,
        @SerializedName("TaskStates") val taskStates: Map<String, TaskState>? = null,
        @SerializedName("DeploymentStatus") val deploymentStatus: AllocDeploymentStatus? = null,
        @SerializedName("FollowupEvalID") val followupEvalId: String? = null,
        @SerializedName("RescheduleTracker") val rescheduleTracker: RescheduleTracker? = null,
        @SerializedName("PreemptedAllocations") val preemptedAllocations: List<String>? = null,
        @SerializedName("PreemptedByAllocation") val preemptedByAllocation: String? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger? = null,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger? = null,
        @SerializedName("CreateTime") val createTime: Long = 0,
        @SerializedName("ModifyTime") val modifyTime: Long = 0
)

data class TaskState(
        @SerializedName("State") val state: String? = null,
        @SerializedName("Failed") val failed: Boolean = false,
        @SerializedName("Restarts") val restarts: BigInteger? = null,
        @SerializedName("LastRestart") val lastRestart: Date? = null,
        @SerializedName("StartedAt") val startedAt: Date? = null,
        @SerializedName("FinishedAt") val finishedAt: Date? = null,
        @SerializedName("Events") val events: List<TaskEvent>? = null,
)

data class TaskEvent(
        @SerializedName("Type") val type: String? = null,
        @SerializedName("Time") val time: Long = 0,
        @SerializedName("DisplayMessage") val displayMessage: String? = null,
        @SerializedName("Details") val details: Map<String, String>? = null,
        @SerializedName("FailsTask") val failsTask: Boolean = false,
        @SerializedName("RestartReason") val restartReason: String? = null,
        @SerializedName("SetupError") val setupError: String? = null,
        @SerializedName("DriverError") val driverError: String? = null,
        @SerializedName("DriverMessage") val driverMessage: String? = null,
        @SerializedName("ExitCode") val exitCode: Long = 0,
        @SerializedName("Signal") val signal: Long = 0,
        @SerializedName("Message") val message: String? = null,
        @SerializedName("KillReason") val killReason: String? = null,
        @SerializedName("KillTimeout") val killTimeout: Long = 0,
        @SerializedName("KillError") val killError: String? = null,
        @SerializedName("StartDelay") val startDelay: Long = 0,
        @SerializedName("DownloadError") val downloadError: String? = null,
        @SerializedName("ValidationError") val validationError: String? = null,
        @SerializedName("DiskLimit") val diskLimit: Long = 0,
        @SerializedName("DiskSize") val diskSize: Long = 0,
        @SerializedName("FailedSibling") val failedSibling: String? = null,
        @SerializedName("VaultError") val vaultError: String? = null,
        @SerializedName("TaskSignalReason") val taskSignalReason: String? = null,
        @SerializedName("TaskSignal") val taskSignal: String? = null,
        @SerializedName("GenericSource") val genericSource: String? = null
)

data class AllocDeploymentStatus(
        @SerializedName("Healthy") val healthy: Boolean? = null,
        @SerializedName("Timestamp") val timestamp: Date? = null,
        @SerializedName("Canary") val canary: Boolean = false,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger? = null
)

data class RescheduleTracker(
        @SerializedName("Events") val events: List<RescheduleEvent?>? = null
)

data class RescheduleEvent(
        @SerializedName("RescheduleTime") val rescheduleTime: Long = 0,
        @SerializedName("PrevAllocID") val prevAllocId: String? = null,
        @SerializedName("PrevNodeID") val prevNodeId: String? = null
)

data class Deployment(
        @SerializedName("ID") val id: String? = null,
        @SerializedName("Namespace") val namespace: String? = null,
        @SerializedName("JobID") val jobId: String? = null,
        @SerializedName("JobVersion") val jobVersion: BigInteger? = null,
        @SerializedName("JobModifyIndex") val jobModifyIndex: BigInteger? = null,
        @SerializedName("JobSpecModifyIndex") val jobSpecModifyIndex: BigInteger? = null,
        @SerializedName("JobCreateIndex") val jobCreateIndex: BigInteger? = null,
        @SerializedName("TaskGroups") val taskGroups: Map<String, DeploymentState>? = null,
        @SerializedName("Status") val status: String? = null,
        @SerializedName("StatusDescription") val statusDescription: String? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger? = null,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger? = null
)

data class DeploymentState(
        @SerializedName("PlacedCanaries") val placedCanaries: List<String>? = null,
        @SerializedName("AutoRevert") val autoRevert: Boolean = false,
        @SerializedName("ProgressDeadline") val progressDeadline: Long = 0,
        @SerializedName("RequireProgressBy") val requireProgressBy: Date? = null,
        @SerializedName("Promoted") val promoted: Boolean = false,
        @SerializedName("DesiredCanaries") val desiredCanaries: Long = 0,
        @SerializedName("DesiredTotal") val desiredTotal: Long = 0,
        @Suppress("SpellCheckingInspection") @SerializedName("PlacedAllocs") val placedAllocs: Long = 0,
        @Suppress("SpellCheckingInspection") @SerializedName("HealthyAllocs") val healthyAllocs: Long = 0,
        @Suppress("SpellCheckingInspection") @SerializedName("UnhealthyAllocs") val unhealthyAllocs: Long = 0,
)

data class Node(
        @SerializedName("ID") val id: String? = null,
        @SerializedName("Datacenter") val datacenter: String? = null,
        @SerializedName("Name") val name: String? = null,
        @Suppress("SpellCheckingInspection") @SerializedName("HTTPAddr") val httpAddr: String? = null,
        @SerializedName("TLSEnabled") val tlsEnabled: Boolean = false,
        @SerializedName("Attributes") val attributes: Map<String, String>? = null,
        @SerializedName("Resources") val resources: Resources? = null,
        @SerializedName("Reserved") val reserved: Resources? = null,
        @SerializedName("Links") val links: Map<String, String>? = null,
        @SerializedName("Meta") val meta: Map<String, String>? = null,
        @SerializedName("NodeClass") val nodeClass: String? = null,
        @SerializedName("Drain") val drain: Boolean = false,
        @SerializedName("DrainStrategy") val drainStrategy: DrainStrategy? = null,
        @SerializedName("SchedulingEligibility") val schedulingEligibility: String? = null,
        @SerializedName("Status") val status: String? = null,
        @SerializedName("StatusDescription") val statusDescription: String? = null,
        @SerializedName("StatusUpdatedAt") val statusUpdatedAt: Long = 0,
        @SerializedName("Events") val events: List<NodeEvent>? = null,
        @SerializedName("Drivers") val drivers: Map<String, DriverInfo>? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger? = null,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger? = null,
)

data class Resources(
        @SerializedName("CPU") val cpu: Int? = null,
        @SerializedName("MemoryMB") val memoryMb: Int? = null,
        @SerializedName("DiskMB") val diskMb: Int? = null,
        @Suppress("SpellCheckingInspection") @SerializedName("IOPS") val iops: Int? = null,
        @SerializedName("Networks") val networks: List<NetworkResource>? = null,
)

data class NetworkResource(
        @SerializedName("Device") var device: String? = null,
        @SerializedName("CIDR") val cidr: String? = null,
        @SerializedName("IP") val ip: String? = null,
        @SerializedName("MBits") val mBits: Int? = null,
        @SerializedName("ReservedPorts") val reservedPorts: List<Port>? = null,
        @SerializedName("DynamicPorts") val dynamicPorts: List<Port>? = null,
)

data class Port(
        @SerializedName("Label") val label: String? = null,
        @SerializedName("Value") val value: Long = 0,
)

data class DrainStrategy(
        @SerializedName("DrainSpec") var drainSpec: DrainSpec? = null,
        @SerializedName("ForceDeadline") val forceDeadline: Date? = null,
)

data class DrainSpec(
        @SerializedName("Deadline") var deadline: Long = 0,
        @SerializedName("IgnoreSystemJobs") val ignoreSystemJobs: Boolean = false
)

data class NodeEvent(
        @SerializedName("Message") var message: String? = null,
        @SerializedName("Subsystem") val subsystem: String? = null,
        @SerializedName("Details") val details: Map<String, String>? = null,
        @SerializedName("Timestamp") val timestamp: Date? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger? = null
)

data class DriverInfo(
        @SerializedName("Attributes") var attributes: Map<String?, String?>? = null,
        @SerializedName("Detected") val detected: Boolean = false,
        @SerializedName("Healthy") val healthy: Boolean = false,
        @SerializedName("HealthDescription") val healthDescription: String? = null,
        @SerializedName("UpdateTime") val updateTime: Date? = null

)