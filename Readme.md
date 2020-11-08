[![Build Status](https://travis-ci.org/barakb/nomad-client.svg?branch=master)](https://travis-ci.org/barakb/nomad-client)
[![Download](https://api.bintray.com/packages/barakb/maven/nomad-client/images/download.svg)](https://bintray.com/barakb/maven/nomad-client/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.barakb/nomad-client.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.barakb%22%20AND%20a:%22nomad-client%22)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
### A Kotlin Nomad client

I had 3 goals in mind when starting this work.

1. Fully async code no blocking threads.
2. Easy to compose both sequentially and concurrently
3. Minimum dependencies.
4. Small

This project depends on [min-rest-client](https://github.com/barakb/mini-rest-client) that is both small and fully non-blocking rest client using suspendable methods to allow easy composition. 
     
To consume this project using maven add the following to your pom.xml

````Xml
<dependency>
     <groupId>com.github.barakb</groupId>
     <artifactId>nomad-client</artifactId>
     <version>1.0.10</version>
</dependency>
````

Or gradle

````kotlin

implementation("com.github.barakb:nomad-client:1.0.10")
````


##### Usage:
To create a Nomad client Kotlin DSL can be used.
```Kotlin
    val client = NomadClient {
        address = "http://127.0.0.1:4646"
        // optional authToken
        authToken = "my-fake-token" 
    }
```   
Https address can be used as well.
The authToken is optional.

The client has public getter for each sub api (jobs, allocations etc), for example this is how a job is submitted.

```Kotlin
client.jobs.create(job)
```   
 
A job can be created directly using the data objects (same as in Java) 
or using the JobBuilder that give a nicer Kotlin DCL that mimic HCL. 

````Kotlin
val job = JobBuilder().apply {
    id = "foo"
    name = "foo"
    group {
        name = "bar"
        task {
            raw_exec {
                command = "/home/barak/dev/kotlin/nomad-client/job.sh"
            }
            name = "myTask"
        }
    }
}.build()

client.jobs.create(job)

````

Of course, you can use arbitrary Kotlin code in the builder. 
For example, this is how you can create a Group with 10 tasks.

````Kotlin
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

client.jobs.create(job)

````

You can also create the Job while sending the request to Nomad.

```Kotlin
client.jobs.create {
            id = "foo"
            name = "foo"
            group {
                name = "bar"
                task {
                    raw_exec {
                        command = "/home/barak/dev/kotlin/nomad-client/job.sh"
                    }
                    name = "myTask"
                }
            }
        }
```
A docker task can executed using the docker_exec DSL

````Kotlin
fun main(): Unit = runBlocking {
    NomadClient {
        address = "http://127.0.0.1:4646"
        authToken = "my-fake-token"
    }.use { client ->
        // https://learn.hashicorp.com/tutorials/nomad/jobs-submit
        client.jobs.create {
            id = "my_job_id"
            name = "my_job"
            group {
                name = "example"
                task {
                    name = "server"
                    docker_exec {
                        image = "hashicorp/http-echo"
                        args = listOf("-listen", ":5678", "-text", "hello world")
                    }
                    resource {
                        network {
                            mBits = 10
                            port("http") {
                                static = 5678
                            }
                        }
                    }
                }
            }
        }
    }
}

````

There are a few utility functions built around Nomad API, for example calling `client.jobs.getLastDeploymentIfHealthy(validJob.id, 30.seconds)`
will return the healthy deployment of that last version of this job or null if no such exists, wait is an optional parameter.

This is how you can use this function to revert changes to a job after unsuccessful change

````Kotlin
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
        }
    }
}
````


The following API are implemented
- jobs
- allocations
- nodes
- evaluations
- deployments
- aclTokens
- aclPolicies
- agent
- operator
- search
- volume

