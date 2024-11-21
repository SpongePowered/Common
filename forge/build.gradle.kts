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
    id("dev.architectury.loom") version "1.6.411"
}

val commonProject = parent!!
val transformersProject = commonProject.project(":modlauncher-transformers")
val libraryManagerProject = commonProject.project(":library-manager")
val testPluginsProject: Project? = rootProject.subprojects.find { "testplugins" == it.name }

val apiVersion: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val recommendedVersion: String by project
val organization: String by project
val projectUrl: String by project

description = "The SpongeAPI implementation for MinecraftForge"
version = spongeImpl.generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion, forgeVersion)

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "sponge"
    }
}

// SpongeForge libraries
val serviceLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceLibraries")
val gameLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameLibraries")

val gameManagedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameManagedLibraries")

val serviceShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceShadedLibraries")
val gameShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameShadedLibraries")

val runTaskOnlyConfig: NamedDomainObjectProvider<Configuration> = configurations.register("runTaskOnly")

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

// SpongeForge source sets
val forgeMain by sourceSets.named("main") {
    // implementation (compile) dependencies
    spongeImpl.applyNamedDependencyOnOutput(commonProject, accessors.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, launch.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, applaunch.get(), this, project, this.implementationConfigurationName)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val forgeLaunch by sourceSets.register("launch") {
    // implementation (compile) dependencies
    spongeImpl.applyNamedDependencyOnOutput(commonProject, launch.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, applaunch.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, main.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(project, this, forgeMain, project, forgeMain.implementationConfigurationName)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val forgeAccessors by sourceSets.register("accessors") {
    spongeImpl.applyNamedDependencyOnOutput(commonProject, mixins.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, accessors.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(project, this, forgeLaunch, project, forgeLaunch.implementationConfigurationName)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val forgeMixins by sourceSets.register("mixins") {
    // implementation (compile) dependencies
    spongeImpl.applyNamedDependencyOnOutput(commonProject, mixins.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, accessors.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, launch.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, applaunch.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(commonProject, main.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(project, forgeMain, this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(project, forgeAccessors, this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(project, forgeLaunch, this, project, this.implementationConfigurationName)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val forgeLang by sourceSets.register("lang") {
    configurations.named(implementationConfigurationName) {
        extendsFrom(langLayerConfig.get())
    }
}
val forgeAppLaunch by sourceSets.register("applaunch") {
    // implementation (compile) dependencies
    spongeImpl.applyNamedDependencyOnOutput(commonProject, applaunch.get(), this, project, this.implementationConfigurationName)
    spongeImpl.applyNamedDependencyOnOutput(project, this, forgeLaunch, project, forgeLaunch.implementationConfigurationName)

    configurations.named(implementationConfigurationName) {
        extendsFrom(serviceLayerConfig.get())
    }
}

configurations.configureEach {
    exclude(group = "net.minecraft", module = "joined")
    if (name != "minecraft") { // awful terrible hack sssh
        exclude(group = "com.mojang", module = "minecraft")
    }

    // Fix that can be found in Forge MDK too
    resolutionStrategy {
        force("net.sf.jopt-simple:jopt-simple:5.0.4")
    }
}

extensions.configure(LoomGradleExtensionAPI::class) {
    silentMojangMappingsLicense()
    accessWidenerPath.set(file("../src/main/resources/common.accesswidener"))

    mixin {
        useLegacyMixinAp.set(false)
    }

    forge {
        useCustomMixin.set(false)
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

    // Arch-loom bug, skip broken union-relauncher
    runs.forEach {
        it.mainClass.set("net.minecraftforge.bootstrap.ForgeBootstrap")
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:${minecraftVersion}")
    "forge"("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    "mappings"(loom.layered {
        officialMojangMappings {
            nameSyntheticMembers = true
        }
    })

    api(project(":", configuration = "launch")) {
        exclude(group = "org.spongepowered", module = "mixin")
    }
    implementation(project(":", configuration = "accessors")) {
        exclude(group = "org.spongepowered", module = "mixin")
    }
    implementation(project(commonProject.path)) {
        exclude(group = "org.spongepowered", module = "mixin")
    }

    forgeMixins.implementationConfigurationName(project(commonProject.path))

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
    }

    val runTaskOnly = runTaskOnlyConfig.name
    // Arch-loom bug, fix support of MOD_CLASSES
    runTaskOnly("net.minecraftforge:bootstrap-dev:2.1.3")
}

val forgeManifest = java.manifest {
    attributes(
            "Specification-Title" to "SpongeForge",
            "Specification-Vendor" to "SpongePowered",
            "Specification-Version" to apiVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to spongeImpl.generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion, forgeVersion),
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
                "Automatic-Module-Name" to "spongeforge.lang",
                "FMLModType" to "LANGPROVIDER"
            )
        }
        from(forgeLang.output)
    }

    val forgeServicesJar by registering(Jar::class) {
        archiveClassifier.set("services")

        manifest {
            from(forgeManifest)
            attributes("Automatic-Module-Name" to "spongeforge.services")
        }

        from(commonProject.sourceSets.named("applaunch").map { it.output })
        from(forgeAppLaunch.output)

        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    afterEvaluate {
        withType(net.fabricmc.loom.task.AbstractRunTask::class) {
            // Default classpath is a mess, we better start a new one from scratch
            classpath = files(
                    configurations.getByName("forgeRuntimeLibrary"),
                    forgeServicesJar, forgeLangJar, runTaskOnlyConfig
            )

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

        outputFile.set(installerResources.map { it.file("org/spongepowered/forge/applaunch/loading/moddiscovery/libraries.json") })
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
                "Automatic-Module-Name" to "spongeforge.services",
                "Multi-Release" to true
            )
        }

        from(commonProject.sourceSets.named("applaunch").map { it.output })
        from(forgeAppLaunch.output)

        // Make sure to relocate access widener so that we don't conflict with other coremods
        relocate("net.fabricmc.accesswidener", "org.spongepowered.forge.libs.accesswidener")
    }

    shadowJar {
        group = "shadow"
        archiveClassifier.set("mod")

        mergeServiceFiles()
        configurations = listOf(gameShadedLibrariesConfig.get())

        manifest {
            attributes(
                "Access-Widener" to "common.accesswidener",
                "Superclass-Transformer" to "common.superclasschange,forge.superclasschange",
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

        into("jars") {
            from(shadowJar)
            rename("spongeforge-(.*)-mod.jar", "spongeforge-mod.jar")

            from(forgeLangJar)
            rename("spongeforge-(.*)-lang.jar", "spongeforge-lang.jar")
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
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("HEADER.txt"))

    property("name", "Sponge")
    property("organization", organization)
    property("url", projectUrl)
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
