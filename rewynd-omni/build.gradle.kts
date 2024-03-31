import java.net.URI

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.kotlinx.kover)
    application
}
apply(plugin = "kotlin")
group = "io.rewynd"
version = "0.0.1"

application {
    mainClass.set("io.rewynd.omni.MainKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":rewynd-common"))
    implementation(project(":rewynd-client-jvm"))
    implementation(project(":rewynd-api"))
    implementation(project(":rewynd-worker"))

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.ktor.server.cio.jvm)

    implementation(libs.kotlin.logging)
    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)
}

kotlin {
    jvmToolchain(17)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "io.rewynd.omni.MainKt"
    }
    archiveVersion.set("")
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.lintKotlin { dependsOn(tasks.formatKotlin) }
tasks.build {
    dependsOn(tasks.koverHtmlReport)
}