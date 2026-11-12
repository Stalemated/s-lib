import dev.kikugie.stonecutter.build.StonecutterBuildExtension

plugins {
    `java-library`
    `maven-publish`
    id("publish-mutex")
    id("mod-publishing")
}

val sc = extensions.getByType<StonecutterBuildExtension>()
val modId = sc.properties.get<String>("mod.id")
val modVersion = sc.properties.get<String>("mod.version")

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

group = sc.properties.get<String>("mod.group")
version = "$modVersion+${sc.current.version}"
base.archivesName = "s-lib-${sc.current.project.substringAfter('-')}"

repositories {
    mavenCentral()
    maven("https://maven.isxander.dev/releases") { name = "Isxander" }
}

dependencies {
    api("blue.endless:jankson:${sc.properties.get<String>("deps.jankson")}")
    testImplementation("org.junit.jupiter:junit-jupiter:${sc.properties.get<String>("deps.jupiter")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(files(sourceSets.main.get().compileClasspath))
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    processResources {
        val fabricLoader = sc.properties.get<String>("deps.fabric_loader")
        val modMetadata = mapOf(
            "id" to modId,
            "name" to sc.properties.get<String>("mod.name"),
            "version" to modVersion,
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "java_version" to requiredJava.majorVersion,
            "mixin_compatibility" to "JAVA_${requiredJava.majorVersion}",
            "fabric_loader" to requireNotNull(Regex("\\d+\\.\\d+").find(fabricLoader)) {
                "deps.fabric_loader must start with major.minor, got $fabricLoader"
            }.value,
            "neoforge" to sc.properties.get<String>("mod.neoforge_compat"),
            "icon_key" to if (sc.current.parsed >= "26.2") "iconFile" else "logoFile",
        )

        inputs.properties(modMetadata)
        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
            expand(modMetadata)
        }
    }

    withType<Jar>().configureEach {
        val licenseName = "LICENSE-$modId"
        inputs.property("mod_id", modId)
        from(rootProject.file("LICENSE")) { rename { licenseName } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"
        inputs.property("version", modVersion)
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
