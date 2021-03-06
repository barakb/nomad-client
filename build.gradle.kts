@file:Suppress("SpellCheckingInspection")

import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val coroutinesVersion = "1.3.9"
val jupiterVersion = "5.6.2"
val logbackVersion = "1.2.3"
val kotlinLoginVersion = "1.8.3"
val slf4jApiVersoion = "1.7.30"
val gsonVersion = "2.8.6"
val httpClientVersion = "5.0.1"

plugins {
    kotlin("jvm") version "1.4.10"
    id("org.jetbrains.dokka") version "1.4.10"
    application
    id("com.adarshr.test-logger") version "2.1.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}
group = "com.github.barakb"
version = "1.0.10"

repositories {
    gradlePluginPortal()
    mavenCentral()
    jcenter()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jApiVersoion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoginVersion")
    implementation("com.github.barakb:mini-rest-client:1.0.2")

    dokkaJavadocPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.10")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    implementation(kotlin("stdlib-jdk8"))
}
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        @Suppress("SpellCheckingInspection")
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn" + "-Xjsr305=strict"
    }
}

tasks.test {
    useJUnitPlatform()
}

testlogger {
    showStandardStreams = true
}

application {
    applicationDefaultJvmArgs = listOf("-Dkotlinx.coroutines.debug")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}


val artifactName = project.name
val artifactGroup = project.group.toString()
val artifactVersion = project.version.toString()

val pomUrl = "https://github.com/barakb/nomad-client"
val pomScmUrl = "https://github.com/barakb/nomad-client"
val pomIssueUrl = "https://github.com/barakb/nomad-client/issues"
val pomDesc = "A Kotlin Nomad client"

val githubReadme = "Readme.md"

val pomLicenseName = "The Apache Software License, Version 2.0"
val pomLicenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"
val pomLicenseDist = "repo"

val pomDeveloperId = "barakb"
val pomDeveloperName = "Barak Bar Orion"

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

publishing {
    publications {
        create<MavenPublication>("nomad-client") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            from(components["java"])

            artifact(sourcesJar)
            artifact(dokkaJar)

            pom.withXml {
                asNode().apply {
                    appendNode("description", pomDesc)
                    appendNode("name", rootProject.name)
                    appendNode("url", pomUrl)
                    appendNode("licenses").appendNode("license").apply {
                        appendNode("name", pomLicenseName)
                        appendNode("url", pomLicenseUrl)
                        appendNode("distribution", pomLicenseDist)
                    }
                    appendNode("developers").appendNode("developer").apply {
                        appendNode("id", pomDeveloperId)
                        appendNode("name", pomDeveloperName)
                    }
                    appendNode("scm").apply {
                        appendNode("url", pomScmUrl)
                    }
                }
            }
        }
    }
}


bintray {
    user = project.findProperty("bintrayUser").toString()
    key = project.findProperty("bintrayKey").toString()
    publish = true

    setPublications("nomad-client")

    pkg.apply {
        repo = "maven"
        name = artifactName
        userOrg = "barakb"
        githubRepo = "barakb/nomad-client"
        vcsUrl = pomScmUrl
        description = "A Kotlin Nomad client"
        setLabels("kotlin", "Nomad", "REST")
        setLicenses("Apache-2.0")
        desc = description
        websiteUrl = pomUrl
        issueTrackerUrl = pomIssueUrl
        githubReleaseNotesFile = githubReadme
        version.apply {
            name = artifactVersion
            desc = pomDesc
            released = Date().toString()
            vcsTag = artifactVersion
            gpg.sign = true
            mavenCentralSync.apply {
                sync = true
                user = project.findProperty("sonatypeUser").toString()
                password = project.findProperty("sonatypePassword").toString()
            }
        }
    }
}