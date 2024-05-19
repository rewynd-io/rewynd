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

    detektPlugins(libs.detekt.formatting)
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

detekt {
    buildUponDefaultConfig = true
    autoCorrect = true
    config.setFrom(parent!!.file("detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
    }
    jvmTarget = "17"
}

tasks.build {
    dependsOn(tasks.koverHtmlReport)
}
