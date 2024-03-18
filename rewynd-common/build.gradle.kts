plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("org.jmailen.kotlinter") version "4.3.0"
}

group = "io.rewynd"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":rewynd-client-jvm"))
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.kotlinx.datetime)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgres)
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
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.lintKotlin { dependsOn(tasks.formatKotlin) }
