import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinx.kover)
}

group = "io.rewynd"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins(libs.detekt.formatting)

    implementation(project(":rewynd-client-jvm"))
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.datetime)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgres)
    implementation(libs.sqlite)
    implementation(libs.hikari.cp)
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    implementation(libs.lettuce.core)

    implementation(libs.typesafe.config)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.reactor)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.mockk)

    implementation(libs.expiringmap)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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

tasks.build {
    dependsOn(tasks.koverHtmlReport)
}
