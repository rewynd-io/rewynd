import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinx.kover)
}

group = "io.rewynd"
version = "0.0.1"
application {
    mainClass.set("io.rewynd.api.MainKt")

    val isDevelopment: Boolean = project.ext.has("development")

    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":rewynd-spec"))
    implementation(project(":rewynd-client-jvm"))
    implementation(project(":rewynd-common"))
    implementation(project(":rewynd-test-util-jvm"))
    implementation(project(":rewynd-web"))

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.sessions.jvm)
    implementation(libs.ktor.server.host.common.jvm)
    implementation(libs.ktor.server.status.pages.jvm)
    implementation(libs.ktor.server.compression.jvm)
    implementation(libs.ktor.server.partial.content.jvm)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.default.headers.jvm)
    implementation(libs.ktor.server.call.logging.jvm)
    implementation(libs.ktor.server.call.id.jvm)
    implementation(libs.ktor.server.metrics.jvm)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    implementation(libs.ktor.server.cio.jvm)
    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.coroutines.reactive)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgres)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.mockk)
    testImplementation(libs.kielbasa.kotest.property)
    implementation(libs.kotlin.logging)

    // Scheduling
    implementation(libs.quartz)
    detektPlugins(libs.detekt.formatting)
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

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.rewynd.api.MainKt"
    }
    archiveVersion.set("")
    mergeServiceFiles()
    append("reference.conf") // Join reference files. This might not be the best way of handling it, but it works...
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build {
    dependsOn(tasks.koverHtmlReport)
}
