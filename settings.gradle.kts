pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
// logistics-core-system (similar to inPost, amazon)

rootProject.name = "logistics-core-system"

includeBuild("libs/public-id-codec")
includeBuild("domains/shipping/shipment")
includeBuild("domains/shipping/edge-ingestion")
includeBuild("domains/ai/agent-runtime")
includeBuild("platform-spring-boot")
