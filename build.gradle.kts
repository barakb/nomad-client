@file:Suppress("SpellCheckingInspection")

import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val coroutinesVersion = "1.3.9"
val jupiterVersion = "5.8.2"
val logbackVersion = "1.2.10"
val kotlinLoginVersion = "2.1.21"
val slf4jApiVersoion = "1.7.36"
val gsonVersion = "2.9.0"
val httpClientVersion = "5.1.3"

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
    application
    id("com.adarshr.test-logger") version "2.1.1"
    `maven-publish`
}
group = "com.github.barakb"
version = "1.0.11"

repositories {
    gradlePluginPortal()
    mavenCentral()
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

    dokkaJavadocPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.6.10")

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
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/barakb/nomad-client")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
