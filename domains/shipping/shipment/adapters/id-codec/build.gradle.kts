plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":shipment-application"))
    api(project(":shipment-share"))
    implementation("ip.logistics.codec:public-id-codec:1.0.0")

    // UUID lib
    implementation("com.fasterxml.uuid:java-uuid-generator:4.0.1")

    // @Component, @Configuration ect.
    implementation("org.springframework:spring-context:7.0.0")
}
