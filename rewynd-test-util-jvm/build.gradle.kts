import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
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
    implementation(libs.kielbasa.kotest.property)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
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
