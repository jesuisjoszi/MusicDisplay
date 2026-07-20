plugins {
    id("hytale-mod") version "0.+"
}

group = "com.joszza"
version = "0.1.3"
val javaVersion = 25

repositories {
    mavenCentral()
    maven("https://maven.hytale-modding.info/releases") {
        name = "HytaleModdingReleases"
    }
}

val modEmbed: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    val gson = "com.google.code.gson:gson:2.11.0"
    implementation(gson)
    modEmbed(gson)
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("org.jspecify:jspecify:1.0.0")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        modEmbed
            .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
            .map { zipTree(it) }
    }) {
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    from(sourceSets.main.get().output.resourcesDir)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to findProperty("plugin_name"),
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),
        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),
        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author" to findProperty("plugin_author")
    )
    filesMatching("manifest.json") {
        expand(replaceProperties)
    }
    inputs.properties(replaceProperties)
}
