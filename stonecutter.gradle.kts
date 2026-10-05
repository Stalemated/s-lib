plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.1-fabric"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Makes version- and loader-specific properties apply from `stoncutter.properties.toml`
    properties {
        tags(version, loader)
    }

    // Adds constants to Stonecutter comments (i.e. for `//? if fabric {...`)
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = properties.get<String>("mod.id") != "s_lib"
    dependencies["fapi"] = properties.getOrNull<String>("deps.fabric_api") ?: "0"
    val yaclVer = properties.getOrNull<String>("deps.yacl")?.substringBefore('+') ?: "0"
    dependencies["yacl"] = yaclVer
    dependencies["yet_another_config_lib_v3"] = yaclVer

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        string(current.parsed >= "26.1") {
            replace("classTweaker v2 named", "classTweaker v2 official")
        }
    }
}
