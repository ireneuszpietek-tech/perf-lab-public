plugins {
    kotlin("jvm") version "2.2.20"
    application
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("ip.ai.agent.MainKt")
}

kotlin {
    jvmToolchain(21)
}