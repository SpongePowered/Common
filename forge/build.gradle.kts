import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.userdev.UserDevExtension
import org.spongepowered.gradle.impl.AWToAT

buildscript {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public") {
            name = "sponge"
        }
        maven("https://maven.minecraftforge.net/")
    }
}

plugins {
    alias(libs.plugins.shadow)
    id("implementation-structure")
    alias(libs.plugins.blossom)
    id("net.minecraftforge.gradle") version "[6.0.24,6.2)"
}

val commonProject = parent!!
val bootstrapDevProject = commonProject.project(":bootstrap-dev")
val transformersProject = commonProject.project(":modlauncher-transformers")
val libraryManagerProject = commonProject.project(":library-manager")
val testPluginsProject: Project? = rootProject.subprojects.find { "testplugins" == it.name }

val apiVersion: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val recommendedVersion: String by project
val projectUrl: String by project

description = "The SpongeAPI implementation for MinecraftForge"
version = spongeImpl.generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion, forgeVersion)

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "sponge"
    }
}

// SpongeForge libraries
val bootLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("bootLibraries") {
    // Ideally we would filter minecraft itself and only keep its dependencies for this layer,
    // but I couldn't find a way to do it without breaking ForgeGradle.
    extendsFrom(configurations.minecraft.get())
}
val serviceLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceLibraries")
val gameLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameLibraries")

val gameManagedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameManagedLibraries")

val serviceShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceShadedLibraries")
val gameShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameShadedLibraries")

// ModLauncher layers
val bootLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("bootLayer") {
    extendsFrom(bootLibrariesConfig.get())
}
val serviceLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("serviceLayer") {
    extendsFrom(bootLayerConfig.get())
    extendsFrom(serviceLibrariesConfig.get())
}
val langLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("langLayer") {
    extendsFrom(bootLayerConfig.get())
}
val gameLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameLayer") {
    extendsFrom(serviceLayerConfig.get())
    extendsFrom(langLayerConfig.get())
    extendsFrom(gameLibrariesConfig.get())
}

// SpongeCommon source sets
val launchConfig: NamedDomainObjectProvider<Configuration> = commonProject.configurations.named("launch")
val accessors: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("accessors")
val launch: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("launch")
val applaunch: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("applaunch")
val mixins: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("mixins")
val main: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("main")

// SpongeForge source sets
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

    // The rest of the project because we want everything in the initial classpath
    spongeImpl.addDependencyToRuntimeOnly(mixins.get(), this)
    spongeImpl.addDependencyToRuntimeOnly(forgeMixins, this)
    spongeImpl.addDependencyToRuntimeOnly(forgeLang, this)
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

dependencies {
    "minecraft"("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

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
        spongeImpl.copyModulesExcludingProvided(serviceLibrariesConfig.get(), bootLayerConfig.get(), serviceShadedLibrariesConfig.get())
        spongeImpl.copyModulesExcludingProvided(gameLibrariesConfig.get(), serviceLayerConfig.get(), gameManagedLibrariesConfig.get())
    }

    runtimeOnly(project(bootstrapDevProject.path))
    testPluginsProject?.also {
        runtimeOnly(project(it.path))
    }
}

val awFiles: Set<File> = files(main.get().resources, forgeMain.resources).filter { it.name.endsWith(".accesswidener") }.files
val atFile = project.layout.buildDirectory.file("generated/resources/at.cfg").get().asFile
AWToAT.convert(awFiles, atFile)

val mixinConfigs: MutableSet<String> = spongeImpl.mixinConfigurations

extensions.configure(UserDevExtension::class) {
    mappings("official", "1.21.3")
    accessTransformers.from(atFile)
    reobf = false

    runs {
        configureEach {
            ideaModule("Sponge.SpongeForge.main")

            // property("forge.logging.console.level", "debug")
            // jvmArgs("-Dbsl.debug=true") // Uncomment to debug bootstrap classpath

            args(mixinConfigs.flatMap { sequenceOf("--mixin.config", it) })
            environment("MOD_CLASSES", "nop")
        }

        create("client")

        create("server") {
            args("--nogui")
        }
    }
}

afterEvaluate {
    extensions.configure(UserDevExtension::class) {
        // Configure bootstrap-dev
        val bootFileNames = spongeImpl.buildRuntimeFileNames(serviceLayerConfig.get()) // service in boot during dev
        val gameShadedFileNames = spongeImpl.buildRuntimeFileNames(gameShadedLibrariesConfig.get())
        runs.configureEach {
            jvmArgs("-Dsponge.dev.root=" + project.rootDir)
            jvmArgs("-Dsponge.dev.boot=$bootFileNames")
            jvmArgs("-Dsponge.dev.gameShaded=$gameShadedFileNames")
        }
    }
}

val forgeManifest = java.manifest {
    attributes(
            "Specification-Title" to "SpongeForge",
            "Specification-Vendor" to "SpongePowered",
            "Specification-Version" to apiVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to version,
            "Implementation-Vendor" to "SpongePowered"
    )
    // These two are included by most CI's
    System.getenv()["GIT_COMMIT"]?.apply { attributes("Git-Commit" to this) }
    System.getenv()["GIT_BRANCH"]?.apply { attributes("Git-Branch" to this) }
}

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
            property("apiVersion", apiVersion)
            property("version", version.toString())
            property("description", description.toString())
            property("forgeVersion", forgeVersion)
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
