plugins {
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.kotlinx.kover).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.kotlin.android.parcelize).apply(false)
    alias(libs.plugins.android.app).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
}

repositories {
    mavenCentral()
}

dependencies {
}
