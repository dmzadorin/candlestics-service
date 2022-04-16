plugins {
    idea
    kotlin("jvm") version "1.6.20"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "MainKt"
}

group = "org.traderepublic.candlesticks"
version = "1.1.3"

repositories {
    mavenCentral()
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}


object DependencyVersions {
    const val coroutines = "1.6.1"
    const val http4k = "4.25.9.0"
    const val jackson = "2.13.+"
    const val mockk = "1.12.0"
    const val exposed = "0.38.1"
    const val testcontainers = "1.16.3"
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))


    implementation(platform("org.http4k:http4k-bom:4.25.9.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-netty")
    implementation("org.http4k:http4k-client-websocket:${DependencyVersions.http4k}")
    implementation("org.http4k:http4k-format-jackson:${DependencyVersions.http4k}")

    implementation("org.jetbrains.exposed", "exposed-core", DependencyVersions.exposed)
    implementation("org.jetbrains.exposed", "exposed-dao", DependencyVersions.exposed)
    implementation("org.jetbrains.exposed", "exposed-jdbc", DependencyVersions.exposed)
    implementation("org.jetbrains.exposed", "exposed-java-time", DependencyVersions.exposed)
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.coroutines}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${DependencyVersions.jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${DependencyVersions.jackson}")
    testImplementation("io.mockk:mockk:${DependencyVersions.mockk}")
    testImplementation("org.http4k:http4k-testing-kotest:${DependencyVersions.http4k}")
    testImplementation("org.testcontainers:testcontainers:${DependencyVersions.testcontainers}")
    testImplementation("org.testcontainers:junit-jupiter:${DependencyVersions.testcontainers}")
    testImplementation("org.testcontainers:postgresql:${DependencyVersions.testcontainers}")
    testImplementation("org.awaitility:awaitility:4.2.0")
}

tasks.test {
    useJUnitPlatform()
}
