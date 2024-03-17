rootProject.name = "rewynd"
include("rewynd-android")
include("rewynd-api")
include("rewynd-client-jvm")
include("rewynd-client-typescript")
include("rewynd-common")
include("rewynd-spec")
include("rewynd-test-util-jvm")
include("rewynd-web")
include("rewynd-worker")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}
plugins {
}
