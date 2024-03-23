plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
}

repositories {
    mavenCentral()
}

group = "io.rewynd"
version = "0.0.1"

val apiSpec = configurations.create("apiSpec")

dependencies {
    apiSpec(project(":rewynd-spec"))
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.ktor.client.okhttp.jvm)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}

var generatedPath = "${layout.buildDirectory.asFile.get().path}/open-api-generated"
openApiGenerate {
    generatorName = "kotlin"
    inputSpec = "${project(":rewynd-spec").projectDir}/src/openapi.yaml"
    outputDir = generatedPath
    apiPackage = "io.rewynd.client"
    modelPackage = "io.rewynd.model"
    configOptions.put("library", "multiplatform")
    additionalProperties.put("dateLibrary", "kotlinx-datetime")
}

val copySources =
    tasks.register<Copy>("copySources") {
        from(generatedPath)
        into("${layout.buildDirectory.asFile.get().path}/generated-src")
        // Don"t ask me why - jsonBlock is just randomly added there.
        filter { line ->
            line.replace(
                "ApiClient\\(baseUrl, httpClientEngine, httpClientConfig, jsonBlock\\) \\{",
                "ApiClient(baseUrl, httpClientEngine, httpClientConfig) {",
            )
                .replace(
                    // ContentType being filtered from requests for some odd reason
                    "protected val UNSAFE_HEADERS = listOf\\(HttpHeaders\\.ContentType\\)",
                    "protected val UNSAFE_HEADERS = emptyList<String>()",
                )
                .replace(
                    // ContentType being filtered from requests for some odd reason
                    "protected val UNSAFE_HEADERS = listOf(HttpHeaders.ContentType)",
                    "protected val UNSAFE_HEADERS = emptyList<String>()",
                )
                .replace(
                    // Actually set the content type for json requests
                    "protected suspend fun \\<T: Any\\?\\> jsonRequest\\(requestConfig: RequestConfig\\<T\\>, body: Any\\? = null, authNames: kotlin.collections.List\\<String\\>\\): HttpResponse = request\\(requestConfig, body, authNames\\)",
                    "protected suspend fun <T: Any?> jsonRequest(requestConfig: RequestConfig<T>, body: Any? = null, authNames: kotlin.collections.List<String>): HttpResponse = request(requestConfig.apply { headers[HttpHeaders.ContentType] = \"application/json\" }, body, authNames)",
                )
                .replace(
                    // Expose the HttpClient so that we can extract cookies from it and store them
                    "private lateinit var client: HttpClient",
                    "lateinit var client: HttpClient",
                )
                .replace(
                    // Expose the baseUrl so that we can reference it easily
                    "private val baseUrl: String",
                    "val baseUrl: String",
                )
                .replace(
                    // Fix invalid return type for getImage
                    "getImage(imageId: kotlin.String): HttpResponse<org.openapitools.client.infrastructure.OctetByteArray>",
                    "getImage(imageId: kotlin.String): HttpResponse<io.ktor.utils.io.ByteReadChannel>",
                )
                .replace(
                    // TODO remove this and get gzip encoding working with empty responses
                    "val headers = requestConfig.headers",
                    "val headers = mapOf(\"Accept-Encoding\" to \"identity\") + requestConfig.headers",
                ).replace(
                    "val localVariableHeaders = mutableMapOf<String, String>()",
                    "val localVariableHeaders = mutableMapOf<String, String>(io.ktor.http.HttpHeaders.ContentType to io.ktor.http.ContentType.Application.Json.toString())",
                ).replace(
                    "    HttpResponse\\(this, TypedBodyProvider\\(typeInfo<T>\\(\\)\\)\\)",
                    """
    when(T::class.java) {
        Unit::class.java -> HttpResponse(this, UnitBodyProvider()) as HttpResponse<T>
        else -> HttpResponse(this, TypedBodyProvider(typeInfo<T>()))
    }
    class UnitBodyProvider() : BodyProvider<Unit> {
    @Suppress("UNCHECKED_CAST")
    override suspend fun body(response: io.ktor.client.statement.HttpResponse): Unit = Unit

    @Suppress("UNCHECKED_CAST")
    override suspend fun <V : Any> typedBody(response: io.ktor.client.statement.HttpResponse, type: TypeInfo): V =
        Unit as V
}
""",
                )
        }
    }

sourceSets["main"].kotlin.srcDir("${layout.buildDirectory.asFile.get().path}/generated-src/src/main/kotlin")

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

tasks.openApiGenerate {
    dependsOn.add(tasks.clean)
}
copySources {
    dependsOn.add(tasks.openApiGenerate)
}

tasks.compileKotlin {
    dependsOn(copySources)
}

tasks.jar {
    dependsOn(tasks.compileKotlin)
    dependsOn(copySources)
}

tasks.processResources {
    dependsOn.add(copySources)
}
tasks.build {
    dependsOn(tasks.koverHtmlReport)
}