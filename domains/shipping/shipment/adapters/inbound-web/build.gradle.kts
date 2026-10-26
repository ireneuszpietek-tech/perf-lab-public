plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `java-library`
}

dependencies {
    api(project(":shipment-application"))

    implementation("ip.logistics.platform:platform-spring-boot:1.0.0")

    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.0"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    //kotlin
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // OAuth2 resource server (JWT validation)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("org.springframework.boot:spring-boot-starter-security")
}
