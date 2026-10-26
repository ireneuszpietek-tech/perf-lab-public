plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    `java-library`
}

group = "ip.logistics.platform"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}


dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.0"))
    implementation("org.springframework.boot:spring-boot-starter-security")

    // OAuth2 resource server (JWT validation)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")


    // --- Spring Boot ---
    implementation("org.springframework.boot:spring-boot-starter-web")
}
