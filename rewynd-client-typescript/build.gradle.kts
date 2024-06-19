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
    compileOnly(project(":rewynd-spec"))
}

val buildNpm =
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildNpm") {
        dependsOn.add(tasks.compileJava)
        dependsOn.add(tasks.npmInstall)
        args.addAll("run", "build")

        inputs.files(
            fileTree("node_modules").exclude {
                with(it.relativePath.pathString) {
                    startsWith(".cache") || equals("@openapitools/openapi-generator-cli/versions")
                }
            },
        )
        inputs.files(fileTree("binSrc"))
        inputs.file("package.json")
        inputs.file("babel.config.js")
        inputs.file("openapitools.json")
        inputs.file("tsconfig.json")

        outputs.dir("dist")
        outputs.dir("lib")
        outputs.dir("bin")
    }

val cleanNpm =
    tasks.register<Delete>("cleanNpm") {
        delete.add("$projectDir/dist")
        delete.add("$projectDir/lib")
        delete.add("$projectDir/bin")
        delete.add("$projectDir/node_modules")
        delete.add("$projectDir/.gradle/nodejs")
        delete.add("$projectDir/.gradle/npm")
    }

sourceSets {
    main {
        resources {
            srcDir("./lib")
            srcDir("./dist")
            include("./package.json")
            include("./package-lock.json")
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
