import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinx.kover)
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
}

dependencies {
    detektPlugins(libs.detekt.formatting)

    implementation(project(":rewynd-common"))
    implementation(project(":rewynd-client-jvm"))

    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)
    implementation(libs.lettuce.core)
    implementation(libs.kotlin.logging)

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

    testImplementation(project(":rewynd-test-util-jvm"))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kielbasa.kotest.property)
}


tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.rewynd.worker.MainKt"
    }
    archiveVersion.set("")
    mergeServiceFiles()
    append("reference.conf") // Join reference files. This might not be the best way of handling it, but it works...
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

detekt {
    buildUponDefaultConfig = true
    autoCorrect = true
    config.setFrom(parent!!.file("detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
    }
    jvmTarget = libs.versions.jvm.get()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.build {
    dependsOn(tasks.koverHtmlReport)
}
