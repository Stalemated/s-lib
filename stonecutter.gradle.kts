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

    dependencies["yacl"] = properties.getOrNull<String>("deps.yacl")?.substringBefore('+') ?: "0"

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        string(current.parsed >= "26.1") {
            replace("classTweaker v2 named", "classTweaker v2 official")
        }
    }
}

tasks.register(DRY_PUBLISH_TASK) {
    group = "publishing"
    description = "Runs publishMods on every node as a dry run: nothing is uploaded, the files and details land in build/publishMods"
    val requestedTasks = gradle.startParameter.taskNames.toList()
    if (dryPublishRequested()) dependsOn(subprojects.map { "${it.path}:publishMods" })
    else doFirst { throw GradleException("Run '$DRY_PUBLISH_TASK' by its full name so the dry run is on, got the task names $requestedTasks") }
}
