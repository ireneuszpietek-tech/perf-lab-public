rootProject.name = "shipment" // execution unit / aggregate

pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
    plugins {
        kotlin("jvm") version "2.2.0"
        kotlin("plugin.spring") version "2.0.21"
        id("org.springframework.boot") version "4.0.0"
        id("io.spring.dependency-management") version "1.1.6"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

// Leaf modules (technical)
include("shipment-domain")
project(":shipment-domain").projectDir = file("libs/domain")

include("shipment-application")
project(":shipment-application").projectDir = file("libs/application")

include("shipment-inbound-web")
project(":shipment-inbound-web").projectDir = file("adapters/inbound-web")

include("shipment-outbound-jpa")
project(":shipment-outbound-jpa").projectDir = file("adapters/outbound-jpa")

include("shipment-outbound-system")
project(":shipment-outbound-system").projectDir = file("adapters/outbound-system")

include("shipment-id-codec")
project(":shipment-id-codec").projectDir = file("adapters/id-codec")

include("shipment-share")
project(":shipment-share").projectDir = file("adapters/share")

include("shipment-service")
project(":shipment-service").projectDir = file("services/shipment-service")
