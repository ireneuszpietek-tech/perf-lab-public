plugins {
    kotlin("jvm") version "2.2.0"
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

group = "ip.logistics.codec"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-codec:commons-codec:1.20.0")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.13")
}
