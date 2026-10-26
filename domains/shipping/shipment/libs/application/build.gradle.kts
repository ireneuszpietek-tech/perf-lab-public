plugins {
    kotlin("jvm")
    `java-library`
}
dependencies {
    api(project(":shipment-domain"))

//    implementation("ip.logistics.codec:public-id-codec:1.0.0")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.13")

    implementation("net.bytebuddy:byte-buddy:1.18.2")

}
