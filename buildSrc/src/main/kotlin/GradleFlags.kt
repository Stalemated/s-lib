import org.gradle.api.Project

private val flagValues = mapOf("" to true, "true" to true, "false" to false)

fun Project.flag(name: String): Boolean {
    val value = providers.gradleProperty(name).orNull ?: return false
    return requireNotNull(flagValues[value]) { "$name must be true or false, got '$value'" }
}
