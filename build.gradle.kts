plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.simbiri"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)

    // auth
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    // pass hash
    implementation(libs.argon2.jvm)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.ktor.serialization.kotlinx.json)

    // aws rds specific
    implementation(libs.postgres.driver)
    implementation(libs.hikari)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    // aws s3
    implementation(libs.aws.sdk.s3)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    exclude("**/*IntegrationTest*")
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs Docker-backed integration tests."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/*IntegrationTest*")
    shouldRunAfter(tasks.test)

    /*
     * Integration tests depend on an external Docker environment, so run
     * them every time this task is explicitly requested.
     */
    outputs.upToDateWhen { false }
}

tasks.register("fullTest") {
    description = "Runs unit and integration tests."
    group = "verification"
    dependsOn(tasks.test, integrationTest)
}