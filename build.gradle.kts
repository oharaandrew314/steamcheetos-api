plugins {
    kotlin("jvm") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(platform("org.http4k:http4k-bom:4.27.0.0"))
    implementation(platform("org.http4k:http4k-connect-bom:3.18.1.3"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.http4k:http4k-connect-amazon-dynamodb")
    implementation("org.http4k:http4k-connect-amazon-kms")
    implementation("com.github.oharaandrew314:service-utils:0.7.1")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha7")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-serverless-lambda")
    implementation("org.http4k:http4k-contract")
    implementation("org.http4k:http4k-format-moshi")
    implementation("org.http4k:http4k-format-gson")
    implementation("com.github.oharaandrew314:openid4java:1.1.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.3.1")
    testImplementation("org.http4k:http4k-testing-kotest")
    testImplementation("org.http4k:http4k-connect-amazon-dynamodb-fake")
    testImplementation("org.http4k:http4k-connect-amazon-kms-fake")
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