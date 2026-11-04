import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.RunConfigurationContainer

plugins {
    `java-library`
    id("net.neoforged.moddev.legacyforge")
    id("neoforge-mutex")
    id("slib-loader-conventions")
}

dependencies {
    modCompileOnly("dev.isxander:yet-another-config-lib:${property("deps.yacl")}-forge")

    jarJar("blue.endless:jankson:${property("deps.jankson")}")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    testRuntimeOnly(files(sourceSets.main.get().runtimeClasspath))
    testRuntimeOnly("org.slf4j:slf4j-api:${property("deps.slf4j")}")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:${property("deps.log4j")}")
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
        register("client") { client() }
        register("server") { server() }

        configureEach {
            gameDirectory = rootProject.file("run/${sc.current.project}")
            disableIdeRun()
        }
    }
}

if (providers.systemProperty("idea.sync.active").orNull == "true") {
    val ideaSettings = (rootProject.the<IdeaModel>().project as ExtensionAware).the<ProjectSettings>() as ExtensionAware
    val nodePath = path
    legacyForge.runs.configureEach {
        val side = name.replaceFirstChar(Char::uppercase)
        ideaSettings.the<RunConfigurationContainer>().register<Gradle>("Minecraft $side ($nodePath)") {
            taskNames = listOf("$nodePath:run$side")
            projectPath = rootDir.invariantSeparatorsPath
        }
    }
}

mixin {
    add(sourceSets.main.get(), "s_lib.refmap.json")
    config("s-lib.mixins.json")
}

tasks {
    processResources {
        exclude("fabric.mod.json", "*.ct", "*.classtweaker", "META-INF/neoforge.mods.toml")

        val mixinConfig = destinationDir.resolve("s-lib.mixins.json")
        doLast {
            val config = JsonSlurper().parse(mixinConfig) as Map<*, *>
            mixinConfig.writeText(JsonOutput.toJson(config + ("refmap" to "s_lib.refmap.json")))
        }
    }

    named<Jar>("jar") {
        manifest.attributes("MixinConfigs" to "s-lib.mixins.json")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    named<Copy>("buildAndCollect") {
        from(named<Jar>("reobfJar").flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
    }
}
