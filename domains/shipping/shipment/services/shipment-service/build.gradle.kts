plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    application
}

dependencies {
    implementation(project(":shipment-application"))
    implementation(project(":shipment-inbound-web"))
    implementation(project(":shipment-outbound-jpa"))
    implementation(project(":shipment-outbound-system"))
    implementation(project(":shipment-share"))
    implementation(project(":shipment-id-codec"))
    implementation("ip.logistics.platform:platform-spring-boot:1.0.0")
    implementation("ip.logistics.codec:public-id-codec:1.0.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("-Duser.timezone=UTC")
}
application {
    mainClass.set(
        "ip.logistics.shipping.shipment.service.command.ShipmentServiceApplicationKt"
    )
}