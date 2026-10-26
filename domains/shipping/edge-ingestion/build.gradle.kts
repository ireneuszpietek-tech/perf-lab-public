plugins {
    base
}

subprojects {

    plugins.withId("java") {
        the<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }
}
