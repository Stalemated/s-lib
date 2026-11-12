import dev.kikugie.stonecutter.build.StonecutterBuildExtension

plugins {
    id("me.modmuss50.mod-publish-plugin")
}

val sc = extensions.getByType<StonecutterBuildExtension>()
val loader = sc.current.project.substringAfter('-')

val loaderNames = mapOf("fabric" to "Fabric", "forge" to "Forge", "neoforge" to "NeoForge")
val loaderRequirements = mapOf("fabric" to listOf("fabric-api"))
val loaderOptionals = mapOf("fabric" to listOf("modmenu"))
val curseforgeSides = mapOf("both" to (true to true), "client" to (true to false), "server" to (false to true))

fun tomlList(vararg path: String): List<String>? = sc.properties.rawOrNull(*path)?.asList()?.map { it.toString() }

fun requiredTomlList(vararg path: String): List<String> = requireNotNull(tomlList(*path)) {
    "${path.joinToString(".")} is missing in stonecutter.properties.toml"
}

publishMods {
    val node = sc.current.project
    val minecraft = sc.current.version
    val modVersion = sc.properties.get<String>("mod.version")
    val loaderName = requireNotNull(loaderNames[loader]) {
        "loader must be one of ${loaderNames.keys.toList()}, got '$loader'"
    }
    val environment = sc.properties.get<String>("mod.environment")
    val (clientSide, serverSide) = requireNotNull(curseforgeSides[environment]) {
        "mod.environment must be one of ${curseforgeSides.keys.toList()}, got '$environment'"
    }
    val releaseTypes = mapOf("stable" to STABLE, "beta" to BETA, "alpha" to ALPHA)
    val releaseType = sc.properties.get<String>("publish.type")
    val requiredProjects = requiredTomlList("publish", "requires") + loaderRequirements[loader].orEmpty()
    val optionalProjects = requiredTomlList("publish", "optional") + loaderOptionals[loader].orEmpty()
    val mcReleases = tomlList("mod", "mc_releases")
    val modrinthId = sc.properties.get<String>("publish.modrinth_id")
    val curseforgeId = sc.properties.get<String>("publish.curseforge_id")
    val changelogFile = rootProject.file("Changelog.md")
    val javaVersion = providers.provider { the<JavaPluginExtension>().targetCompatibility }
    val releasedVersions = providers.provider {
        requireNotNull(mcReleases) { "mod.mc_releases is missing for node $node" }
    }

    type = requireNotNull(releaseTypes[releaseType]) {
        "publish.type must be one of ${releaseTypes.keys.toList()}, got '$releaseType'"
    }
    version = "$modVersion+$minecraft-$loader"
    displayName = "${sc.properties.get<String>("mod.name")} $modVersion for $loaderName $minecraft"
    changelog = providers.provider {
        val lines = changelogFile.readLines()
        val start = lines.indexOf("## $modVersion")
        require(start >= 0) { "Changelog.md has no '## $modVersion' section" }
        val next = lines.subList(start + 1, lines.size).indexOfFirst { it.startsWith("## ") }
        val end = if (next < 0) lines.size else start + 1 + next
        lines.subList(start + 1, end).joinToString("\n").trim()
    }
    modLoaders.add(loader)
    dryRun = flag("publishDryRun")

    modrinth {
        projectId = providers.provider {
            require(modrinthId.isNotEmpty()) {
                "publish.modrinth_id must be set in stonecutter.properties.toml to publish, got '$modrinthId'"
            }
            modrinthId
        }
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(releasedVersions)
        requires(*requiredProjects.toTypedArray())
        optional(*optionalProjects.toTypedArray())
    }

    curseforge {
        projectId = providers.provider {
            require(curseforgeId.isNotEmpty()) {
                "publish.curseforge_id must be set in stonecutter.properties.toml to publish, got '$curseforgeId'"
            }
            curseforgeId
        }
        accessToken = providers.environmentVariable("cf_token")
        minecraftVersions.addAll(releasedVersions)
        javaVersions.add(javaVersion)
        client = clientSide
        server = serverSide
        changelogType = "markdown"
        requires(*requiredProjects.toTypedArray())
        optional(*optionalProjects.toTypedArray())
    }
}
