plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.node.gradle)
}

group = "io.rewynd"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
}

node {
    version.set("18.17.0")
    download.set(true)
}

val buildNpm =
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildNpm") {
        args.addAll("run", "build")
        dependsOn.add(tasks.compileJava)
        dependsOn.add(tasks.npmInstall)

        inputs.files(
            fileTree("node_modules").exclude {
                it.relativePath.pathString.startsWith(".cache")
            },
        )
        inputs.files(fileTree("src"))
        inputs.file("package.json")

        outputs.dir("dist")
    }

val cleanNpm =
    tasks.register<Delete>("cleanNpm") {
        delete.add("$projectDir/dist")
        delete.add("$projectDir/node_modules")
        delete.add("$projectDir/.gradle/nodejs")
        delete.add("$projectDir/.gradle/npm")
    }

sourceSets.main {
    resources {
        srcDirs("src", "dist")
        include("package.json")
    }
}

tasks.jar {
    from(".").include("src/**", "dist/**", "package.json")
}

tasks.clean {
    dependsOn.add(cleanNpm)
}

tasks.processResources {
    dependsOn.add(buildNpm)
}

kotlin {
    jvmToolchain(17)
}
