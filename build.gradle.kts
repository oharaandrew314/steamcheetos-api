plugins {
    kotlin("jvm") version "1.6.20"
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(platform("org.http4k:http4k-bom:4.25.6.0"))
    implementation(platform("software.amazon.awssdk:bom:2.17.160"))

    implementation(kotlin("stdlib"))
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    implementation("software.amazon.awssdk:kms")
    implementation("com.github.oharaandrew314:service-utils:0.7.1")
    implementation("com.github.oharaandrew314:dynamodb-kotlin-module:0.2.0")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha7")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-serverless-lambda")
    implementation("org.http4k:http4k-contract")
    implementation("org.http4k:http4k-format-jackson")
    implementation("com.github.oharaandrew314:openid4java:1.1.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.2.2")
    testImplementation("org.http4k:http4k-testing-kotest")
    testImplementation("com.github.oharaandrew314:mock-aws-java-sdk:1.0.0-beta.4")
}

tasks.test {
    useJUnitPlatform()
}

java {
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}