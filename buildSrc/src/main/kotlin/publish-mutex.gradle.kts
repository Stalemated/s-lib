import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal

// Prevents concurrent writes to maven-metadata-local.xml across parallel Stonecutter projects
interface PublishMutex : BuildService<BuildServiceParameters.None>

val publishMutex = gradle.sharedServices.registerIfAbsent("mavenLocalPublishMutex", PublishMutex::class.java) {
    maxParallelUsages.set(1)
}

tasks.withType<PublishToMavenLocal>().configureEach {
    usesService(publishMutex)
}
