plugins {
    base
}

tasks.register("buildAll") {
    dependsOn(
        gradle.includedBuild("platform-spring-boot").task(":build"),
        gradle.includedBuild("public-id-codec").task(":build"),
        gradle.includedBuild("shipment").task(":build"),
        gradle.includedBuild("edge-ingestion").task(":build"),
        gradle.includedBuild("agent-runtime").task(":build"),
    )
}
