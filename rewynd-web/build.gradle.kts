plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.node.gradle)
}

val compileNode: Configuration by configurations.creating
configurations {
    compileNode.extendsFrom(implementation.get())
    compileNode.isCanBeResolved = true
}

group = "io.rewynd"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":rewynd-spec"))
    compileOnly(project(":rewynd-client-typescript"))
}

val buildNpm =
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildNpm") {
        dependsOn.add(tasks.npmInstall)
        args.addAll("run", "build")

        inputs.files(
            fileTree("node_modules").exclude {
                it.relativePath.pathString.startsWith(".cache")
            },
        )
        inputs.files(fileTree("lib"))
        inputs.files(fileTree("public"))
        inputs.file("package.json")
        inputs.file("babel.config.js")
        inputs.file("tsconfig.json")

        outputs.dir("dist")
        outputs.dir("webpack")
    }

val cleanNpm =
    tasks.register<Delete>("cleanNpm") {
        delete.add("$projectDir/dist")
        delete.add("$projectDir/node_modules")
        delete.add("$projectDir/webpack")
        delete.add("$projectDir/.gradle/nodejs")
        delete.add("$projectDir/.gradle/npm")
    }

sourceSets {
    main {
        resources {
            srcDir("./webpack")
        }
    }
}

tasks.processResources {
    dependsOn.add(buildNpm)
}

tasks.clean {
    dependsOn.add(cleanNpm)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

node {
    version.set(libs.versions.node.get())
    download.set(true)
}
