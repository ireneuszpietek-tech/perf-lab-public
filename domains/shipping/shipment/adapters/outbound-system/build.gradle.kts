plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // implements ports from application
    api(project(":shipment-application"))

    // UUID lib
    implementation("com.fasterxml.uuid:java-uuid-generator:4.0.1")

    // for @Component, @Configuration, etc.
    implementation("org.springframework:spring-context:7.0.0")
}
