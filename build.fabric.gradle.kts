import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.jar.Manifest

plugins {
    // This plugin applies the correct loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
    id("slib-loader-conventions")
}

repositories {
    maven("https://maven.terraformersmc.com/releases") { name = "TerraformersMC" }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions
    loomx.applyMojangMappings()

    // Use `mod{dependency type}` even on 26.1+ - loom-back-compat converts them
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modCompileOnly("dev.isxander:yet-another-config-lib:${property("deps.yacl")}-fabric")
    modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}") { isTransitive = false }

    include("blue.endless:jankson:${property("deps.jankson")}")

    testRuntimeOnly(files(sourceSets.main.get().compileClasspath))
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run/${sc.current.project}")
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
    }
}

tasks {
    processResources {
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml", "pack.mcmeta")
    }

    named<Copy>("buildAndCollect") {
        // loomx.mod(Sources)Jar returns the jar task for the applied loom variant
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    }

    loomx.modJar.configure {
        doLast {
            val jarPath = (this as AbstractArchiveTask).archiveFile.get().asFile.toPath()
            FileSystems.newFileSystem(jarPath, mapOf("create" to "false")).use { fs ->
                val manifestPath = fs.getPath("META-INF/MANIFEST.MF")

                if (Files.exists(manifestPath)) {
                    val manifest = Files.newInputStream(manifestPath).use(::Manifest)
                    manifest.mainAttributes.putValue("Fabric-Loom-Version", "1.17.0")
                    Files.newOutputStream(manifestPath).use(manifest::write)
                }
            }
        }
    }
}

publishMods {
    file = loomx.modJar.flatMap { it.archiveFile }
}
