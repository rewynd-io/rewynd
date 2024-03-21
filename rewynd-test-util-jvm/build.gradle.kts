plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinter)
}

group = "io.rewynd"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":rewynd-common"))
    implementation(project(":rewynd-client-jvm"))
    implementation(libs.mockk)
    implementation(libs.kotest.property)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotest.extensions.property.datetime)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.lintKotlin { dependsOn(tasks.formatKotlin) }
