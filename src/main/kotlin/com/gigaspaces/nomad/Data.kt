package com.gigaspaces.nomad

import com.google.gson.annotations.SerializedName
import java.math.BigInteger
import java.util.*


// https://stackoverflow.com/questions/44117970/kotlin-data-class-from-json-using-gson

@DslMarker
annotation class JobDCL


@JobDCL
class JobBuilder {
    var name: String? = null
    var id: String? = null

    private var datacenters: MutableList<String> = mutableListOf("dc1")
    private var groups: MutableList<TaskGroup> = mutableListOf()

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
        @SerializedName("Datacenters") var datacenters: List<String>,
        @SerializedName("TaskGroups") val taskGroups: List<TaskGroup>,
        @SerializedName("Stop") val stop: Boolean? = null,
        @SerializedName("Region") val region: String? = null,
        @SerializedName("Namespace") val namespace: String? = null,
        @SerializedName("ParentID") val parentId: String? = null,
        @SerializedName("Type") val type: String? = null,
        @SerializedName("Priority") val priority: Int? = null,
        @SerializedName("AllAtOnce") val allAtOnce: Boolean? = null,
        @SerializedName("Constraints") val constraints: List<Constraint>? = null,
        @SerializedName("Update") val update: UpdateStrategy? = null,
        @SerializedName("Periodic") val periodic: PeriodicConfig? = null,
        @SerializedName("ParameterizedJob") val parameterizedJob: ParameterizedJobConfig? = null,
        @SerializedName("Dispatched") val dispatched: Boolean = false,
        @Suppress("ArrayInDataClass") @SerializedName("Payload") val payload: ByteArray? = null,
        @SerializedName("Reschedule") val reschedule: ReschedulePolicy? = null,
        @SerializedName("Migrate") val migrate: MigrateStrategy? = null,
        @SerializedName("Meta") val meta: Map<String, String>? = null,
        @SerializedName("VaultToken") val vaultToken: String? = null,
        @SerializedName("Status") val status: String? = null,
        @SerializedName("StatusDescription") val statusDescription: String? = null,
        @SerializedName("Stable") val stable: Boolean? = null,
        @SerializedName("Version") val version: BigInteger? = null,
        @SerializedName("SubmitTime") val submitTime: Long? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger? = null,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger? = null,
        @SerializedName("JobModifyIndex") val jobModifyIndex: BigInteger? = null,
)

data class MigrateStrategy(
        @SerializedName("MaxParallel") val maxParallel: Int? = null,
        @SerializedName("HealthCheck") val healthCheck: String? = null,
        @SerializedName("MinHealthyTime") val minHealthyTime: Long? = null,
        @SerializedName("HealthyDeadline") val healthyDeadline: Long? = null

)

data class ReschedulePolicy(
        @SerializedName("Attempts") val attempts: Int? = null,
        @SerializedName("Interval") val interval: Long? = null,
        @SerializedName("Delay") val delay: Long? = null,
        @SerializedName("DelayFunction") val delayFunction: String? = null,
        @SerializedName("MaxDelay") val maxDelay: Long? = null,
        @SerializedName("Unlimited") val unlimited: Boolean? = null
)


data class ParameterizedJobConfig(
        @SerializedName("Payload") val payload: String? = null,
        @SerializedName("MetaRequired") val metaRequired: List<String>? = null,
        @SerializedName("MetaOptional") val metaOptional: List<String>? = null
)

data class PeriodicConfig(
        @SerializedName("Enabled") val enabled: Boolean? = null,
        @SerializedName("Spec") val spec: String? = null,
        @SerializedName("SpecType") val specType: String? = null,
        @SerializedName("ProhibitOverlap") val prohibitOverlap: Boolean? = null,
        @SerializedName("TimeZone") val timeZone: String? = null,
)

data class Constraint(
        @SerializedName("LTarget") var lTarget: String,
        @SerializedName("RTarget") val rTarget: String,
        @SerializedName("Operand") val operand: String
)

data class UpdateStrategy(
        @SerializedName("Stagger") val stagger: Long? = null,
        @SerializedName("MaxParallel") val maxParallel: Int? = null,
        @SerializedName("HealthCheck") val healthCheck: String? = null,
        @SerializedName("MinHealthyTime") val minHealthyTime: Long? = null,
        @SerializedName("HealthyDeadline") val healthyDeadline: Long? = null,
        @SerializedName("ProgressDeadline") val progressDeadline: Long? = null,
        @SerializedName("AutoRevert") val autoRevert: Boolean? = null,
        @SerializedName("Canary") val canary: Int? = null,

        )

@JobDCL
class GroupBuilder {
    private val tasks: MutableList<Task> = mutableListOf()
    private var restartPolicy: RestartPolicy? = null
    private var reschedulePolicy: ReschedulePolicy? = null
    var name: String? = null

    fun task(init: TaskBuilder.() -> Unit) {
        tasks.add(TaskBuilder().apply(init).build())
    }

    @Suppress("unused")
    fun restart(init: RestartPolicyBuilder.() -> Unit) {
        restartPolicy = RestartPolicyBuilder().apply(init).build()
    }

    @Suppress("unused")
    fun reschedule(init: ReschedulePolicyBuilder.() -> Unit) {
        reschedulePolicy = ReschedulePolicyBuilder().apply(init).build()
    }

    internal fun build(): TaskGroup {
        val name = this.name ?: throw IllegalArgumentException("group command is missing $this")
        return TaskGroup(name = name, tasks = tasks, restartPolicy = restartPolicy)
    }
}

data class TaskGroup(
        @SerializedName("Name") val name: String,
        @SerializedName("Tasks") val tasks: List<Task> = listOf(),
        @SerializedName("Count") val count: Int? = null,
        @SerializedName("Constraints") val constraints: List<Constraint>? = null,
        @SerializedName("RestartPolicy") val restartPolicy: RestartPolicy? = null,
        @SerializedName("ReschedulePolicy") val reschedulePolicy: ReschedulePolicy? = null,
        @SerializedName("EphemeralDisk") val ephemeralDisk: EphemeralDisk? = null,
        @SerializedName("Update") val update: UpdateStrategy? = null,
        @SerializedName("Migrate") val migrate: MigrateStrategy? = null,
        @SerializedName("Meta") val meta: Map<String, String>? = null,
)

@Suppress("MemberVisibilityCanBePrivate")
@JobDCL
class RestartPolicyBuilder {
    var interval: Long? = null
    var attempts: Int? = null
    var delay: Long? = null
    var mode: String? = null

    fun build(): RestartPolicy {
        return RestartPolicy(interval, attempts, delay, mode)
    }
}

@Suppress("MemberVisibilityCanBePrivate")
@JobDCL
class ReschedulePolicyBuilder {
    var attempts: Int? = null
    val interval: Long? = null
    var delay: Long? = null
    val delayFunction: String? = null
    val maxDelay: Long? = null
    val unlimited: Boolean? = null

    fun build(): ReschedulePolicy {
        return ReschedulePolicy(attempts, interval, delay, delayFunction, maxDelay, unlimited)
    }
}

data class RestartPolicy(
        @SerializedName("Interval") val interval: Long? = null,
        @SerializedName("Attempts") val attempts: Int? = null,
        @SerializedName("Delay") val delay: Long? = null,
        @SerializedName("Mode") val mode: String? = null
)

data class EphemeralDisk(
        @SerializedName("Sticky") val sticky: Boolean? = null,
        @SerializedName("Migrate") val migrate: Boolean? = null,
        @SerializedName("SizeMB") val sizeMb: Int? = null
)

@Suppress("MemberVisibilityCanBePrivate")
@JobDCL
class TaskBuilder {
    var name: String? = null
    private var driver: String? = null
    private var config: Any? = null
    private var resources: Resources? = null
    private var artifacts: List<TaskArtifact> = listOf()

    @Suppress("unused", "FunctionName")
    fun raw_exec(init: ConfigRawExecBuilder.() -> Unit) {
        config = ConfigRawExecBuilder().apply(init).build()
        driver = "raw_exec"
    }

    @Suppress("unused", "FunctionName")
    fun java_exec(init: ConfigJavaExecBuilder.() -> Unit) {
        config = ConfigJavaExecBuilder().apply(init).build()
        driver = "raw_exec"

    }

    @Suppress("unused")
    fun resource(init: ResourceBuilder.() -> Unit) {
        resources = ResourceBuilder().apply(init).build()
    }

    @Suppress("unused")
    fun artifact(init: ArtifactBuilder.() -> Unit) {
        artifacts = artifacts + ArtifactBuilder().apply(init).build()
    }

    internal fun build(): Task {
        val name = this.name ?: throw IllegalArgumentException("task name is missing $this")
        val driver = this.driver ?: throw IllegalArgumentException("task driver is missing $this")
        val config = this.config ?: throw IllegalArgumentException("task config is missing $this")
        return Task(name = name, driver = driver, config = config, resources = resources, artifacts = artifacts)
    }
}

data class Task(
        @SerializedName("Name") val name: String,
        @SerializedName("Driver") val driver: String,
        @SerializedName("Config") val config: Any? = null,
        @SerializedName("User") val user: String? = null,
        @SerializedName("Constraints") val constraints: List<Constraint>? = null,
        @SerializedName("Env") val env: Map<String, String>? = null,
        @SerializedName("Services") val services: List<Service>? = null,
        @SerializedName("Resources") val resources: Resources? = null,
        @SerializedName("Meta") val meta: Map<String, String>? = null,
        @SerializedName("KillTimeout") val killTimeout: Long? = null,
        @SerializedName("LogConfig") val logConfig: LogConfig? = null,
        @SerializedName("Artifacts") val artifacts: List<TaskArtifact>? = null,
        @SerializedName("Vault") val vault: Vault? = null,
        @SerializedName("Templates") val templates: List<Template>? = null,
        @SerializedName("DispatchPayload") val dispatchPayload: DispatchPayloadConfig? = null,
        @SerializedName("Leader") val leader: Boolean = false,
        @SerializedName("ShutdownDelay") val shutdownDelay: Long = 0,
        @SerializedName("KillSignal") val killSignal: String? = null
)

data class DispatchPayloadConfig(
        @SerializedName("File") var file: String? = null
)

data class Vault(
        @SerializedName("Policies") val policies: List<String>? = null,
        @SerializedName("Env") val env: Boolean? = null,
        @SerializedName("ChangeMode") val changeMode: String? = null,
        @SerializedName("ChangeSignal") val changeSignal: String? = null
)


data class LogConfig(
        @SerializedName("MaxFiles") val maxFiles: Int? = null,
        @SerializedName("MaxFileSizeMB") val maxFileSizeMb: Int? = null

)

@Suppress("MemberVisibilityCanBePrivate")
@JobDCL
class ArtifactBuilder {
    private var options: Map<String, String>? = null
    var source: String? = null
    var destination: String? = null
    var mode: String? = null

    @Suppress("unused")
    fun options(init: StringMapBuilder.() -> Unit) {
        options = StringMapBuilder().apply(init).build()
    }

    fun build(): TaskArtifact {
        return TaskArtifact(source, destination, options, mode)
    }
}

@Suppress("MemberVisibilityCanBePrivate")
@JobDCL
class StringMapBuilder {
    private var options = mapOf<String, String>()

    @Suppress("unused")
    fun op(key: String, value: String) {
        options = options + (key to value)
    }

    fun build(): Map<String, String> {
        return options
    }
}

data class TaskArtifact(
        @SerializedName("GetterSource") val source: String? = null,
        @SerializedName("GetterDestination") val destination: String? = null,
        @SerializedName("GetterOptions") val getterOptions: Map<String, String>? = null,
        @SerializedName("GetterMode") val mode: String? = null,
)

data class Template(
        @SerializedName("SourcePath") val sourcePath: String? = null,
        @Suppress("SpellCheckingInspection") @SerializedName("DestPath") val destPath: String? = null,
        @SerializedName("EmbeddedTmpl") val embeddedTmpl: String? = null,
        @SerializedName("ChangeMode") val changeMode: String? = null,
        @SerializedName("ChangeSignal") val changeSignal: String? = null,
        @SerializedName("Splay") val splay: Long? = null,
        @SerializedName("Perms") val perms: String? = null,
        @Suppress("SpellCheckingInspection") @SerializedName("LeftDelim") val leftDelim: String? = null,
        @Suppress("SpellCheckingInspection") @SerializedName("RightDelim") val rightDelim: String? = null,
        @Suppress("SpellCheckingInspection") @SerializedName("Envvars") val envvars: Boolean? = null,
        @SerializedName("VaultGrace") val vaultGrace: Long? = null
)


data class Service(
        @SerializedName("Id") val id: String? = null,
        @SerializedName("Name") val name: String? = null,
        @SerializedName("Tags") val tags: List<String>? = null,
        @SerializedName("CanaryTags") val canaryTags: List<String>? = null,
        @SerializedName("PortLabel") val portLabel: String? = null,
        @SerializedName("AddressMode") val addressMode: String? = null,
        @SerializedName("Checks") val checks: List<ServiceCheck>? = null,
        @SerializedName("CheckRestart") val checkRestart: CheckRestart? = null

)

data class ServiceCheck(
        @SerializedName("Id") val id: String? = null,
        @SerializedName("Name") val name: String? = null,
        @SerializedName("Type") val type: String? = null,
        @SerializedName("Command") val command: String? = null,
        @SerializedName("Args") val args: List<String>? = null,
        @SerializedName("Path") val path: String? = null,
        @SerializedName("Protocol") val protocol: String? = null,
        @SerializedName("PortLabel") val portLabel: String? = null,
        @SerializedName("AddressMode") val addressMode: String? = null,
        @SerializedName("Interval") val interval: Long = 0,
        @SerializedName("Timeout") val timeout: Long = 0,
        @SerializedName("InitialStatus") val initialStatus: String? = null,
        @SerializedName("TLSSkipVerify") val tlsSkipVerify: Boolean = false,
        @SerializedName("Header") val header: Map<String, List<String>>? = null,
        @SerializedName("Method") val method: String? = null,
        @SerializedName("CheckRestart") val checkRestart: CheckRestart? = null,
        @SerializedName("GRPCService") val grpcService: String? = null,
        @SerializedName("GRPCUseTLS") val grpcUseTls: Boolean = false
)

data class CheckRestart(
        @SerializedName("Limit") val limit: Int = 0,
        @SerializedName("Grace") val grace: Long? = null,
        @SerializedName("IgnoreWarnings") val ignoreWarnings: Boolean = false
)


@JobDCL
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

@JobDCL
class ConfigJavaExecBuilder {
    @Suppress("MemberVisibilityCanBePrivate")
    var jarPath: String? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var jvmOptions: List<String> = listOf()

    internal fun build(): ConfigJavaExec {
        val cmd = jarPath ?: throw IllegalArgumentException("config command is missing $this")
        return ConfigJavaExec(cmd, jvmOptions)
    }
}

data class ConfigJavaExec(
        @SerializedName("jar_path") val jarPath: String? = null,
        @SerializedName("jvm_options") val jvmOptions: List<String>? = null,
)

@JobDCL
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

@Suppress("unused")
data class JobDesc(
        @SerializedName("ID") val id: String,
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
        private val jobSummary: JobSummary,
        @SerializedName("CreateIndex") val createIndex: BigInteger? = null,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger,
        @SerializedName("JobModifyIndex") val jobModifyIndex: BigInteger? = null,
        @SerializedName("SubmitTime") val submitTime: Long = 0
)

data class JobSummary(
        @SerializedName("JobID") var jobId: String? = null,
        @SerializedName("Namespace") var namespace: String? = null,
        private val summary: Map<String, TaskGroupSummary>,
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
        @SerializedName("TaskStates") val taskStates: Map<String, TaskState> = emptyMap(),
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
        @SerializedName("TaskGroups") val taskGroups: Map<String, DeploymentState> = emptyMap(),
        @SerializedName("Status") val status: String? = null,
        @SerializedName("StatusDescription") val statusDescription: String? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger,
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

data class EvaluationResponse(
        @SerializedName("EvalCreateIndex") val createIndex: BigInteger?,
        @SerializedName("EvalID") val id: String,
        @SerializedName("Index") val index: BigInteger,
        @SerializedName("JobModifyIndex") val jobModifyIndex: BigInteger?,
        @SerializedName("KnownLeader") val knownLeader: Boolean?,
        @SerializedName("LastContact") val lastContact: BigInteger?,
        @SerializedName("Warnings") val warnings: String?
)

data class Evaluation(
        @SerializedName("ID") val id: String,
        @SerializedName("Priority") val priority: Int = 0,
        @SerializedName("Type") val type: String? = null,
        @SerializedName("TriggeredBy") val triggeredBy: String? = null,
        @SerializedName("Namespace") val namespace: String? = null,
        @SerializedName("JobID") val jobId: String? = null,
        @SerializedName("JobModifyIndex") val jobModifyIndex: BigInteger? = null,
        @SerializedName("NodeID") val nodeId: String? = null,
        @SerializedName("NodeModifyIndex") val nodeModifyIndex: BigInteger? = null,
        @SerializedName("DeploymentID") val deploymentId: String? = null,
        @SerializedName("Status") val status: String? = null,
        @SerializedName("StatusDescription") val statusDescription: String? = null,
        @SerializedName("Wait") val wait: Long = 0,
        @SerializedName("WaitUntil") val waitUntil: Date? = null,
        @SerializedName("NextEval") val nextEval: String? = null,
        @SerializedName("PreviousEval") val previousEval: String? = null,
        @SerializedName("BlockedEval") val blockedEval: String? = null,
        @Suppress("SpellCheckingInspection") @SerializedName("FailedTGAllocs") val failedTgAllocs: Map<String, AllocationMetric>? = null,
        @SerializedName("ClassEligibility") val classEligibility: Map<String, Boolean>? = null,
        @SerializedName("EscapedComputedClass") val escapedComputedClass: Boolean = false,
        @SerializedName("QuotaLimitReached") val quotaLimitReached: String? = null,
        @SerializedName("AnnotatePlan") val annotatePlan: Boolean = false,
        @SerializedName("QueuedAllocations") val queuedAllocations: Map<String, Int>? = null,
        @SerializedName("SnapshotIndex") val snapshotIndex: BigInteger? = null,
        @SerializedName("CreateIndex") val createIndex: BigInteger,
        @SerializedName("ModifyIndex") val modifyIndex: BigInteger? = null
)

data class AllocationMetric(
        @SerializedName("NodesEvaluated") val nodesEvaluated: Int = 0,
        @SerializedName("NodesFiltered") val nodesFiltered: Int = 0,
        @SerializedName("NodesAvailable") val nodesAvailable: Map<String, Int> = emptyMap(),
        @SerializedName("ClassFiltered") val classFiltered: Map<String, Int> = emptyMap(),
        @SerializedName("ConstraintFiltered") val constraintFiltered: Map<String, Int> = emptyMap(),
        @SerializedName("NodesExhausted") val nodesExhausted: Int = 0,
        @SerializedName("ClassExhausted") val classExhausted: Map<String, Int> = emptyMap(),
        @SerializedName("DimensionExhausted") val dimensionExhausted: Map<String, Int> = emptyMap(),
        @SerializedName("QuotaExhausted") val quotaExhausted: List<String> = emptyList(),
        @SerializedName("Scores") val scores: Map<String, Double>? = null,
        @SerializedName("AllocationTime") val allocationTime: Long = 0,
        @SerializedName("CoalescedFailures") val coalescedFailures: Int = 0
)
