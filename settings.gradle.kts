import org.spongepowered.gradle.vanilla.repository.MinecraftRepositoryExtension

pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
        maven("https://maven.architectury.dev/")
    }
    val vanillaGradleVersion = "0.2.1-20241107.194040-91"
    plugins {
        id("org.spongepowered.gradle.vanilla") version vanillaGradleVersion
        id("implementation-structure")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.spongepowered.gradle.vanilla") {
                useModule("org.spongepowered:vanillagradle:$vanillaGradleVersion")
            }
        }
    }
}

plugins {
    id("org.spongepowered.gradle.vanilla")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT) // needed for arch-loom, unfortunately
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
        maven("https://maven.neoforged.net/releases/") {
            name = "neoforge"
        }
    }
    versionCatalogs {
        register("apiLibs") {
            from(files("SpongeAPI/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Sponge"

extensions.configure(MinecraftRepositoryExtension::class) {
    injectRepositories(false)
}

// Required projects

val apiPropsFile = file("SpongeAPI/gradle.properties")
if (!apiPropsFile.exists()) {
    throw InvalidUserDataException("""
        The SpongeAPI submodule required to build does not appear to be set up.

        To correct this, run
            git submodule update --init --recursive
        from the project's checkout directory.
    """.trimIndent())
}

includeBuild("build-logic")
includeBuild("SpongeAPI") {
    dependencySubstitution {
        substitute(module("org.spongepowered:spongeapi")).using(project(":"))
    }
}
include("modlauncher-transformers")
include("generator")

// Optional projects

var projects: List<String>
val projectsArg: String? = startParameter.projectProperties["projects"]
if (projectsArg == null) {
    val projectsPropsFile = file("projects.properties")
    if (!projectsPropsFile.exists()) {
        file("gradle/projects-default.properties").copyTo(projectsPropsFile)
    }

    val projectsProps = java.util.Properties()
    projectsPropsFile.bufferedReader(Charsets.UTF_8).use {
        projectsProps.load(it)
    }

    projects = projectsProps.entries.filter { it.value.equals("true") }.map { it.key.toString() }
    println("Projects selected in config file: " + projects.joinToString(","))
} else {
    projects = projectsArg.split(",")
    println("Projects selected in start parameter: " + projects.joinToString(","))
}

if (projects.contains("vanilla")) {
    include(":SpongeVanilla")
    project(":SpongeVanilla").projectDir = file("vanilla")
}

if (projects.contains("forge")) {
    include(":SpongeForge")
    project(":SpongeForge").projectDir = file("forge")
}

if (projects.contains("neoforge")) {
    include(":SpongeNeo")
    project(":SpongeNeo").projectDir = file("neoforge")
}

if (projects.contains("testplugins")) {
    include(":testplugins")
}

// API properties

val apiProps = java.util.Properties()
apiPropsFile.bufferedReader(Charsets.UTF_8).use {
    apiProps.load(it)
}

val extraProperties = mutableMapOf<String, String>()
apiProps.stringPropertyNames().forEach { key ->
    val value = apiProps.getProperty(key)
    if (value != null) {
        if (key.startsWith("api")) {
            extraProperties[key] = value
        } else {
            extraProperties["api${key.replaceFirstChar { it.uppercase() }}"] = value
        }
    }
}

gradle.beforeProject {
    val extraExt = project.extensions.extraProperties
    extraProperties.forEach { (k, v) -> extraExt.set(k, v) }
}
