plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `java-library`
}

dependencies {
    api(project(":shipment-application"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.0"))
//    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    // + flyway/liquibase in future
}
