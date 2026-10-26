plugins {
    java
    application
    id("com.gradleup.shadow") version "9.6.1"
}

val avroTools = configurations.create("avroTools")

val generatedAvroDirectory =
    layout.buildDirectory.dir("generated/sources/avro/main")

val avroSchemas = fileTree("src/main/avro") {
    include("**/*.avsc")
}

dependencies {
    // Required by generated SpecificRecord classes at application runtime.
    implementation("org.apache.avro:avro:1.12.1")

    // Build-time tooling must not leak into the application runtime classpath.
    avroTools("org.apache.avro:avro-tools:1.12.1") {
        /*
         * avro-tools declares dependencies on Trevni test artifacts that are not
         * available in Maven Central for this version.
         *
         * Trevni is unrelated to Avro schema compilation, which is the only
         * avro-tools command used by this project.
         */
        exclude(group = "org.apache.avro", module = "trevni-avro")
        exclude(group = "org.apache.avro", module = "trevni-core")
    }

}

val generateAvroJava = tasks.register<JavaExec>("generateAvroJava") {
    group = "build"
    description = "Generates Java SpecificRecord classes from Avro schemas."

    classpath = avroTools
    mainClass.set("org.apache.avro.tool.Main")

    inputs.files(avroSchemas)
    outputs.dir(generatedAvroDirectory)

    doFirst {
        val schemaFiles = avroSchemas.files
            .sortedBy { it.absolutePath }

        require(schemaFiles.isNotEmpty()) {
            "No Avro schemas found in src/main/avro"
        }

        /*
         * avro-tools expects:
         *
         * compile schema <schema-file>... <output-directory>
         */
        setArgs(
            listOf("compile", "schema") +
                    schemaFiles.map { it.absolutePath } +
                    generatedAvroDirectory.get().asFile.absolutePath
        )
    }
}

sourceSets {
    named("main") {
        // Generated files belong to build output, not to version-controlled sources.
        java.srcDir(generatedAvroDirectory)
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateAvroJava)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs = listOf("-Duser.timezone=UTC")
}

application {
    mainClass.set(
        "ip.logistics.shipping.edgeingestion.service.IngestionServiceApplication"
    )
}