[![Build Status](https://travis-ci.org/barakb/nomad-client.svg?branch=master)](https://travis-ci.org/barakb/nomad-client)
### Yet another Nomad client for the JVM
I had 3 goals in mind when starting this work.

1. Fully async code no blocking threads.
2. Easy to compose both sequentially and concurrently
3. Minimum dependencies.

- Choosing the underlining http client to be Apache HttpAsyncClient **satisfy the first and third requirements**.
- Extending `CloseableHttpAsyncClient.execute` as a suspend function (in the file CloseableHttpAsyncClientExt.kt)
  enable easy composition of the result client sequentially and concurrently, hence **satisfy the second requirement**. 
     
Currently, the compiled transitive dependencies are:

````bash

[INFO]    ch.qos.logback:logback-classic:jar:1.2.3:compile
[INFO]    ch.qos.logback:logback-core:jar:1.2.3:compile
[INFO]    com.google.code.gson:gson:jar:2.8.6:compile
[INFO]    commons-codec:commons-codec:jar:1.10:compile
[INFO]    commons-logging:commons-logging:jar:1.2:compile
[INFO]    io.github.microutils:kotlin-logging:jar:1.11.5:compile
[INFO]    org.apache.httpcomponents:httpasyncclient:jar:4.1.4:compile
[INFO]    org.apache.httpcomponents:httpclient:jar:4.5.6:compile
[INFO]    org.apache.httpcomponents:httpcore-nio:jar:4.4.10:compile
[INFO]    org.apache.httpcomponents:httpcore:jar:4.4.10:compile
[INFO]    org.jetbrains.kotlin:kotlin-reflect:jar:1.4.10:compile
[INFO]    org.jetbrains.kotlin:kotlin-stdlib-common:jar:1.4.10:compile
[INFO]    org.jetbrains.kotlin:kotlin-stdlib-jdk7:jar:1.4.10:compile
[INFO]    org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.4.10:compile
[INFO]    org.jetbrains.kotlin:kotlin-stdlib:jar:1.4.10:compile
[INFO]    org.jetbrains.kotlinx:kotlinx-coroutines-core:jar:1.3.9:compile
[INFO]    org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:jar:1.3.9:compile
[INFO]    org.jetbrains:annotations:jar:13.0:compile
[INFO]    org.slf4j:slf4j-api:jar:1.7.30:compile

Here is the dependency tree:
com.gigaspaces:nomad-client:jar:1.0-SNAPSHOT
[INFO] +- org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.4.10:compile
[INFO] |  +- org.jetbrains.kotlin:kotlin-stdlib:jar:1.4.10:compile
[INFO] |  |  +- org.jetbrains.kotlin:kotlin-stdlib-common:jar:1.4.10:compile
[INFO] |  |  \- org.jetbrains:annotations:jar:13.0:compile
[INFO] |  \- org.jetbrains.kotlin:kotlin-stdlib-jdk7:jar:1.4.10:compile
[INFO] +- org.jetbrains.kotlin:kotlin-reflect:jar:1.4.10:compile
[INFO] +- org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:jar:1.3.9:compile
[INFO] |  \- org.jetbrains.kotlinx:kotlinx-coroutines-core:jar:1.3.9:compile
[INFO] +- ch.qos.logback:logback-classic:jar:1.2.3:compile
[INFO] |  \- ch.qos.logback:logback-core:jar:1.2.3:compile
[INFO] +- io.github.microutils:kotlin-logging:jar:1.11.5:compile
[INFO] +- org.apache.httpcomponents:httpasyncclient:jar:4.1.4:compile
[INFO] |  +- org.apache.httpcomponents:httpcore:jar:4.4.10:compile
[INFO] |  +- org.apache.httpcomponents:httpcore-nio:jar:4.4.10:compile
[INFO] |  +- org.apache.httpcomponents:httpclient:jar:4.5.6:compile
[INFO] |  |  \- commons-codec:commons-codec:jar:1.10:compile
[INFO] |  \- commons-logging:commons-logging:jar:1.2:compile
[INFO] +- com.google.code.gson:gson:jar:2.8.6:compile
[INFO] +- org.slf4j:slf4j-api:jar:1.7.30:compile

````

In addition, there is a builder for Nomad job that mimic HCL, 
that makes the creation of a job from Kotlin code extremely readable.

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

Not to mention the endless options to add some Kotlin code inside, for example create Group with 10 tasks

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


Of course, it can combine with the create function

```Kotlin
client.jobs.create{
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

The nomad client itself can be configured using kotlin DSL

```Kotlin
    val client = NomadClient {
        httpConfig {
            defaultRequest {
                url = "http://127.0.0.1:4646/v1/"
            }
        }
        nomadToken("my-token")
    }
```   