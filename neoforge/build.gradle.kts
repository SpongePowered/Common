import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI

buildscript {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public") {
            name = "sponge"
        }
        maven("https://maven.architectury.dev/")
    }
}

plugins {
    alias(libs.plugins.shadow)
    id("implementation-structure")
    alias(libs.plugins.blossom)
    id("dev.architectury.loom")
}

val commonProject = parent!!
val transformersProject = commonProject.project(":modlauncher-transformers")
val libraryManagerProject = commonProject.project(":library-manager")
val testPluginsProject: Project? = rootProject.subprojects.find { "testplugins" == it.name }

val apiVersion: String by project
val minecraftVersion: String by project
val neoForgeVersion: String by project
val recommendedVersion: String by project
val projectUrl: String by project

description = "The SpongeAPI implementation for NeoForge"
version = spongeImpl.generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion, neoForgeVersion)

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "sponge"
    }
    maven("https://maven.neoforged.net/releases/") {
        name = "neoforge"
    }
}

// SpongeNeo libraries
val serviceLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceLibraries")
val gameLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameLibraries")

val gameManagedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameManagedLibraries")

val serviceShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceShadedLibraries")
val gameShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameShadedLibraries")

configurations.named("forgeRuntimeLibrary") {
    extendsFrom(serviceLibrariesConfig.get())
}

// ModLauncher layers
val serviceLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceLayer") {
    extendsFrom(serviceLibrariesConfig.get())
    extendsFrom(configurations.getByName("forgeDependencies"))
}
val langLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("langLayer") {
    extendsFrom(configurations.getByName("forgeDependencies"))
}
val gameLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameLayer") {
    extendsFrom(serviceLayerConfig.get())
    extendsFrom(langLayerConfig.get())
    extendsFrom(gameLibrariesConfig.get())

    afterEvaluate {
        extendsFrom(configurations.getByName("minecraftNamedCompile"))
    }
}

// SpongeCommon source sets
val launchConfig: NamedDomainObjectProvider<Configuration> = commonProject.configurations.named("launch")
val accessors: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("accessors")
val launch: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("launch")
val applaunch: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("applaunch")
val mixins: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("mixins")
val main: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("main")

// SpongeNeo source sets
// Service layer
val forgeAppLaunch by sourceSets.register("applaunch") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(serviceLayerConfig.get())
    }
}

// Lang layer
val forgeLang by sourceSets.register("lang") {
    configurations.named(implementationConfigurationName) {
        extendsFrom(langLayerConfig.get())
    }
}

// Game layer
val forgeLaunch by sourceSets.register("launch") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(main.get(), this)
    spongeImpl.addDependencyToImplementation(forgeAppLaunch, this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val forgeAccessors by sourceSets.register("accessors") {
    spongeImpl.addDependencyToImplementation(accessors.get(), this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val forgeMixins by sourceSets.register("mixins") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(accessors.get(), this)
    spongeImpl.addDependencyToImplementation(mixins.get(), this)
    spongeImpl.addDependencyToImplementation(main.get(), this)
    spongeImpl.addDependencyToImplementation(forgeAppLaunch, this)
    spongeImpl.addDependencyToImplementation(forgeLaunch, this)
    spongeImpl.addDependencyToImplementation(forgeAccessors, this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val forgeMain by sourceSets.named("main") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(accessors.get(), this)
    spongeImpl.addDependencyToImplementation(main.get(), this)
    spongeImpl.addDependencyToImplementation(forgeAppLaunch, this)
    spongeImpl.addDependencyToImplementation(forgeLaunch, this)
    spongeImpl.addDependencyToImplementation(forgeAccessors, this)

    spongeImpl.addDependencyToImplementation(this, forgeMixins)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}

configurations.configureEach {
    exclude(group = "net.minecraft", module = "joined")
    if (name != "minecraft") { // awful terrible hack sssh
        exclude(group = "com.mojang", module = "minecraft")
    }
}

extensions.configure(LoomGradleExtensionAPI::class) {
    silentMojangMappingsLicense()
    accessWidenerPath.set(file("../src/main/resources/common.accesswidener"))

    mixin {
        useLegacyMixinAp.set(false)
    }

    mods {
        named("main") {
            sourceSet(forgeMixins)
            sourceSet(forgeAccessors)
            sourceSet(forgeLaunch)

            sourceSet(main.get(), commonProject)
            sourceSet(mixins.get(), commonProject)
            sourceSet(accessors.get(), commonProject)
            sourceSet(launch.get(), commonProject)

            configuration(gameManagedLibrariesConfig.get())
            configuration(gameShadedLibrariesConfig.get())
        }
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:${minecraftVersion}")
    "neoForge"("net.neoforged:neoforge:$neoForgeVersion")
    "mappings"(loom.layered {
        officialMojangMappings {
            nameSyntheticMembers = true
        }
    })

    val service = serviceLibrariesConfig.name
    service(apiLibs.pluginSpi)
    service(project(transformersProject.path)) {
        exclude(group = "cpw.mods", module = "modlauncher")
    }
    service(project(libraryManagerProject.path))
    service(platform(apiLibs.configurate.bom))
    service(apiLibs.configurate.core) {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    service(apiLibs.configurate.hocon) {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    service(libs.configurate.jackson) {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    val game = gameLibrariesConfig.name
    game("org.spongepowered:spongeapi:$apiVersion")
    game(libs.javaxInject)
    game(platform(apiLibs.adventure.bom))
    game(libs.adventure.serializerConfigurate4)

    val serviceShadedLibraries = serviceShadedLibrariesConfig.name
    serviceShadedLibraries(project(transformersProject.path)) { isTransitive = false }
    serviceShadedLibraries(project(libraryManagerProject.path)) { isTransitive = false }

    val gameShadedLibraries = gameShadedLibrariesConfig.name
    gameShadedLibraries("org.spongepowered:spongeapi:$apiVersion") { isTransitive = false }

    afterEvaluate {
        spongeImpl.copyModulesExcludingProvided(serviceLibrariesConfig.get(), configurations.getByName("forgeDependencies"), serviceShadedLibrariesConfig.get())
        spongeImpl.copyModulesExcludingProvided(gameLibrariesConfig.get(), serviceLayerConfig.get(), gameManagedLibrariesConfig.get())

        "forgeRuntimeLibrary"(files(tasks.named("forgeServicesJar"), tasks.named("forgeLangJar")))
    }
}

val forgeManifest = java.manifest {
    attributes(
            "Specification-Title" to "SpongeNeo",
            "Specification-Vendor" to "SpongePowered",
            "Specification-Version" to apiVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to spongeImpl.generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion, neoForgeVersion),
            "Implementation-Vendor" to "SpongePowered"
    )
    // These two are included by most CI's
    System.getenv()["GIT_COMMIT"]?.apply { attributes("Git-Commit" to this) }
    System.getenv()["GIT_BRANCH"]?.apply { attributes("Git-Branch" to this) }
}

val mixinConfigs: MutableSet<String> = spongeImpl.mixinConfigurations

tasks {
    jar {
        manifest.from(forgeManifest)
    }
    val forgeAppLaunchJar by registering(Jar::class) {
        archiveClassifier.set("applaunch")
        manifest.from(forgeManifest)
        from(forgeAppLaunch.output)
    }
    val forgeLaunchJar by registering(Jar::class) {
        archiveClassifier.set("launch")
        manifest.from(forgeManifest)
        from(forgeLaunch.output)
    }
    val forgeAccessorsJar by registering(Jar::class) {
        archiveClassifier.set("accessors")
        manifest.from(forgeManifest)
        from(forgeAccessors.output)
    }
    val forgeMixinsJar by registering(Jar::class) {
        archiveClassifier.set("mixins")
        manifest.from(forgeManifest)
        from(forgeMixins.output)
    }
    val forgeLangJar by registering(Jar::class) {
        archiveClassifier.set("lang")
        manifest {
            from(forgeManifest)
            attributes(
                "Automatic-Module-Name" to "spongeneo.lang",
                "FMLModType" to "LIBRARY"
            )
        }
        from(forgeLang.output)
    }

    val forgeServicesJar by registering(Jar::class) {
        archiveClassifier.set("services")

        manifest {
            from(forgeManifest)
            attributes("Automatic-Module-Name" to "spongeneo.services")
        }

        from(commonProject.sourceSets.named("applaunch").map { it.output })
        from(forgeAppLaunch.output)

        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    afterEvaluate {
        withType(net.fabricmc.loom.task.AbstractRunTask::class) {
            // Tip: don't set classpath here, it is ignored by bootstrap-launcher
            // Add boot jars into forgeRuntimeLibrary instead

            testPluginsProject?.also {
                val testPluginsOutput = it.sourceSets.getByName("main").output
                val dirs: MutableList<File> = mutableListOf()
                dirs.add(testPluginsOutput.resourcesDir!!)
                dirs.addAll(testPluginsOutput.classesDirs)
                environment["SPONGE_PLUGINS"] = dirs.joinToString("&")

                dependsOn(it.tasks.classes)
            }

            argumentProviders += CommandLineArgumentProvider {
                mixinConfigs.asSequence()
                        .flatMap { sequenceOf("--mixin.config", it) }
                        .toList()
            }

            // jvmArguments.add("-Dbsl.debug=true") // Uncomment to debug bootstrap classpath

            sourceSets.forEach {
                dependsOn(it.classesTaskName)
            }
        }
    }

    val installerResources = project.layout.buildDirectory.dir("generated/resources/installer")
    forgeAppLaunch.resources.srcDir(installerResources)

    val emitDependencies by registering(org.spongepowered.gradle.impl.OutputDependenciesToJson::class) {
        group = "sponge"
        this.dependencies("main", gameManagedLibrariesConfig)
        this.excludedDependencies(gameShadedLibrariesConfig)

        outputFile.set(installerResources.map { it.file("org/spongepowered/neoforge/applaunch/loading/moddiscovery/libraries.json") })
    }
    named(forgeAppLaunch.processResourcesTaskName).configure {
        dependsOn(emitDependencies)
    }

    val forgeServicesShadowJar by register("servicesShadowJar", ShadowJar::class) {
        group = "shadow"
        archiveClassifier.set("services")

        mergeServiceFiles()
        configurations = listOf(serviceShadedLibrariesConfig.get())
        exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "**/module-info.class")

        manifest {
            from(forgeManifest)
            attributes(
                "Automatic-Module-Name" to "spongeneo.services",
                "Multi-Release" to true
            )
        }

        from(commonProject.sourceSets.named("applaunch").map { it.output })
        from(forgeAppLaunch.output)
        // We need to exclude this as NeoForge ships jackson-core as a library
        // and we would be violating the packages
        dependencyFilter.exclude(dependencyFilter.dependency("com.fasterxml.jackson.core:jackson-core"))

        // Make sure to relocate access widener so that we don't conflict with other coremods
        relocate("net.fabricmc.accesswidener", "org.spongepowered.neoforge.libs.accesswidener")
    }

    shadowJar {
        group = "shadow"
        archiveClassifier.set("mod")

        mergeServiceFiles()
        configurations = listOf(gameShadedLibrariesConfig.get())

        manifest {
            attributes(
                "Access-Widener" to "common.accesswidener",
                "Superclass-Transformer" to "common.superclasschange,neoforge.superclasschange",
                "MixinConfigs" to mixinConfigs.joinToString(",")
            )
            from(forgeManifest)
        }

        from(commonProject.sourceSets.main.map { it.output })
        from(commonProject.sourceSets.named("mixins").map {it.output })
        from(commonProject.sourceSets.named("accessors").map {it.output })
        from(commonProject.sourceSets.named("launch").map {it.output })

        from(forgeLaunch.output)
        from(forgeAccessors.output)
        from(forgeMixins.output)
    }

    val universalJar = register("universalJar", Jar::class) {
        group = "build"
        archiveClassifier.set("universal")

        manifest.from(forgeServicesShadowJar.manifest)

        from(forgeServicesShadowJar.archiveFile.map { zipTree(it) })
        mustRunAfter(forgeServicesJar)

        into("jars") {
            from(shadowJar)
            rename("spongeneo-(.*)-mod.jar", "spongeneo-mod.jar")

            from(forgeLangJar)
            rename("spongeneo-(.*)-lang.jar", "spongeneo-lang.jar")
        }
    }

    assemble {
        dependsOn(universalJar)
    }
}

sourceSets {
    main {
        blossom.resources {
            property("version", project.provider { project.version.toString() })
            property("description", project.description.toString())
            property("neoForgeVersion", neoForgeVersion)
        }
    }
}

publishing {
    publications {
        register("sponge", MavenPublication::class) {
            artifact(tasks["universalJar"])

            artifact(tasks["jar"])
            artifact(tasks["sourcesJar"])

            artifact(tasks["forgeLangJar"])
            artifact(tasks["langSourcesJar"])

            artifact(tasks["forgeMixinsJar"])
            artifact(tasks["mixinsSourcesJar"])

            artifact(tasks["forgeAccessorsJar"])
            artifact(tasks["accessorsSourcesJar"])

            artifact(tasks["forgeLaunchJar"])
            artifact(tasks["launchSourcesJar"])

            artifact(tasks["forgeAppLaunchJar"])
            artifact(tasks["applaunchSourcesJar"])

            pom {
                artifactId = project.name.lowercase()
                this.name.set(project.name)
                this.description.set(project.description)
                this.url.set(projectUrl)

                licenses {
                    license {
                        this.name.set("MIT")
                        this.url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/SpongePowered/Sponge.git")
                    developerConnection.set("scm:git:ssh://github.com/SpongePowered/Sponge.git")
                    this.url.set(projectUrl)
                }
            }
        }
    }
}
