import java.net.URI

val kotlinVersion: String by project
val kotlinxSerializationVersion: String by project
val logbackVersion: String by project
val lettuceVersion: String by project
val slf4jVersion: String by project
val postgresDriverVersion: String by project
val kotlinxCoroutinesVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jmailen.kotlinter") version "4.2.0"
    application
}
apply(plugin = "kotlin")
group = "io.rewynd"
version = "0.0.1"
application {
    mainClass.set("io.rewynd.worker.MainKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = URI.create("https://jitpack.io") }
}

dependencies {
    implementation(project(":rewynd-common"))
    implementation(project(":rewynd-client-jvm"))

    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)
    implementation(libs.lettuce.core)
    implementation(libs.kotlin.logging.jvm)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgres)

    implementation(libs.kotlinx.datetime)

    // NFO parsing
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.typesafe.config)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    // Searching
    implementation(libs.lucene.core)
    implementation(libs.lucene.queries)
    implementation(libs.lucene.queryparser)
    implementation(libs.lucene.analysis.common)
    implementation(libs.lucene.analysis.icu)
    implementation(libs.lucene.suggest)
    implementation(libs.lucene.memory)
    implementation(libs.lucene.backward.codecs)

    // Scheduling
    implementation(libs.quartz)

    // Mp4 fragmenting
    implementation(libs.margarita)
    implementation(libs.okio)
}

kotlin {
    jvmToolchain(17)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.rewynd.worker.MainKt"
    }
    archiveVersion.set("")
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.lintKotlin { dependsOn(tasks.formatKotlin) }
