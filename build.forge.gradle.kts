plugins {
    `maven-publish`
    id("java-library")
    id("net.neoforged.moddev.legacyforge")
    id("neoforge-mutex")
    id("publish-mutex")
}

group = "${property("mod.group")}"
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "s-lib-forge"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://maven.isxander.dev/releases") { name = "Isxander" }
    maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }

    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    // YACL
    modCompileOnly("dev.isxander:yet-another-config-lib:${property("deps.yacl")}-forge")

    // Jankson
    jarJar("blue.endless:jankson:${property("deps.jankson")}")
    api("blue.endless:jankson:${property("deps.jankson")}")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // JUnit Jupiter
    testImplementation("org.junit.jupiter:junit-jupiter:${property("deps.jupiter")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(files(sourceSets.main.get().compileClasspath))
    testRuntimeOnly(files(sourceSets.main.get().runtimeClasspath))
    testRuntimeOnly("org.slf4j:slf4j-api:${property("deps.slf4j")}")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:${property("deps.log4j")}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j" && requested.name == "log4j-core") {
            useVersion(property("deps.log4j") as String)
        }
    }
}

legacyForge {
    version = property("deps.forge_loader") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = file("../../run/")
            client()
        }

        register("server") {
            gameDirectory = file("../../run/")
            server()
        }
    }
}

mixin {
    add(sourceSets.main.get(), "s_lib.refmap.json")
    config("s-lib.mixins.json")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
        }

        filesMatching("META-INF/mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") {
            expand("java" to mixinJava)
            filter { line -> if (line.contains("\"package\"")) "    \"refmap\": \"s_lib.refmap.json\",\n$line" else line }
        }

        exclude("fabric.mod.json", "*.ct", "*.classtweaker", "META-INF/neoforge.mods.toml")
    }

    named<Jar>("jar") {
        manifest.attributes("MixinConfigs" to "s-lib.mixins.json")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // Includes the license file in the built mod
    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
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
