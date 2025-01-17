import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.spongepowered.gradle.vanilla")
    alias(libs.plugins.shadow)
    id("implementation-structure")
    alias(libs.plugins.blossom)
    eclipse
}

val commonProject = parent!!
val transformersProject = commonProject.project(":modlauncher-transformers")
val libraryManagerProject = commonProject.project(":library-manager")
val testPluginsProject: Project? = rootProject.subprojects.find { "testplugins" == it.name }

val apiVersion: String by project
val apiJavaTarget: String by project
val minecraftVersion: String by project
val recommendedVersion: String by project
val projectUrl: String by project

description = "The SpongeAPI implementation for Vanilla Minecraft"
version = spongeImpl.generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion)

// SpongeVanilla libraries
val devlaunchLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("devlaunchLibraries")
val installerLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("installerLibraries")
val initLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("initLibraries") // JVM initial classpath
val bootLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("bootLibraries")
val gameLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameLibraries") {
    extendsFrom(configurations.minecraft.get())
}

val gameManagedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameManagedLibraries")

val bootShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("bootShadedLibraries")
val gameShadedLibrariesConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameShadedLibraries")

// ModLauncher layers
val bootLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("bootLayer") {
    extendsFrom(initLibrariesConfig.get())
    extendsFrom(bootLibrariesConfig.get())
}
val gameLayerConfig: NamedDomainObjectProvider<Configuration> = configurations.register("gameLayer") {
    extendsFrom(bootLayerConfig.get())
    extendsFrom(gameLibrariesConfig.get())
}

// SpongeCommon source sets
val accessors: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("accessors")
val launch: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("launch")
val applaunch: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("applaunch")
val mixins: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("mixins")
val main: NamedDomainObjectProvider<SourceSet> = commonProject.sourceSets.named("main")

// SpongeVanilla source sets
// Dev launch
val vanillaDevLaunch by sourceSets.register("devlaunch") {
    configurations.named(implementationConfigurationName) {
        extendsFrom(devlaunchLibrariesConfig.get())
    }
}

// Prod launch
val vanillaInstaller by sourceSets.register("installer") {
    configurations.named(implementationConfigurationName) {
        extendsFrom(installerLibrariesConfig.get())
    }
}

// Boot layer
val vanillaAppLaunch by sourceSets.register("applaunch") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(bootLayerConfig.get())
    }
}

// Game layer
val vanillaLaunch by sourceSets.register("launch") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(main.get(), this)
    spongeImpl.addDependencyToImplementation(vanillaAppLaunch, this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val vanillaAccessors by sourceSets.register("accessors") {
    spongeImpl.addDependencyToImplementation(accessors.get(), this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val vanillaMixins by sourceSets.register("mixins") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(accessors.get(), this)
    spongeImpl.addDependencyToImplementation(mixins.get(), this)
    spongeImpl.addDependencyToImplementation(main.get(), this)
    spongeImpl.addDependencyToImplementation(vanillaAppLaunch, this)
    spongeImpl.addDependencyToImplementation(vanillaLaunch, this)
    spongeImpl.addDependencyToImplementation(vanillaAccessors, this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }
}
val vanillaMain by sourceSets.named("main") {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(accessors.get(), this)
    spongeImpl.addDependencyToImplementation(main.get(), this)
    spongeImpl.addDependencyToImplementation(vanillaAppLaunch, this)
    spongeImpl.addDependencyToImplementation(vanillaLaunch, this)
    spongeImpl.addDependencyToImplementation(vanillaAccessors, this)

    spongeImpl.addDependencyToImplementation(this, vanillaMixins)

    configurations.named(implementationConfigurationName) {
        extendsFrom(gameLayerConfig.get())
    }

    // The rest of the project because we want everything in the initial classpath
    spongeImpl.addDependencyToRuntimeOnly(mixins.get(), this)
    spongeImpl.addDependencyToRuntimeOnly(vanillaDevLaunch, this)
    spongeImpl.addDependencyToRuntimeOnly(vanillaMixins, this)

    configurations.named(runtimeOnlyConfigurationName) {
        extendsFrom(devlaunchLibrariesConfig.get())
    }
}

val superclassConfigs = spongeImpl.getNamedConfigurations("superClassChanges")
val mixinConfigs = spongeImpl.mixinConfigurations

minecraft {
    main.get().resources
            .filter { it.name.endsWith(".accesswidener") }
            .files
            .forEach { accessWideners(it) }

    vanillaMain.resources
            .filter { it.name.endsWith(".accesswidener") }
            .files
            .forEach { accessWideners(it) }
}

configurations.configureEach {
    // Force jopt-simple to be exactly 5.0.4 because Mojang ships that version, but some transitive dependencies request 6.0+
    resolutionStrategy {
        force("net.sf.jopt-simple:jopt-simple:5.0.4")
    }
}

dependencies {
    val devlaunch = devlaunchLibrariesConfig.name
    devlaunch(libs.bootstrap.api)

    val installer = installerLibrariesConfig.name
    installer(apiLibs.gson)
    installer(platform(apiLibs.configurate.bom))
    installer(apiLibs.configurate.hocon)
    installer(apiLibs.configurate.core)
    installer(libs.joptSimple)
    installer(libs.tinylog.api)
    installer(libs.tinylog.impl)

    installer(libs.asm.commons)
    installer(libs.asm.tree)
    installer(libs.forgeAutoRenamingTool) {
        exclude(group = "net.sf.jopt-simple")
        exclude(group = "org.ow2.asm")
    }

    installer(project(libraryManagerProject.path))

    val init = initLibrariesConfig.name
    init(libs.securemodules)
    init(libs.asm.commons)
    init(libs.asm.util)
    init(libs.jarjar.fs)

    val boot = bootLibrariesConfig.name
    boot(libs.securemodules)
    boot(libs.asm.commons)
    boot(libs.asm.util)
    boot(libs.bootstrap)

    boot(libs.modlauncher) {
        exclude(group = "org.apache.logging.log4j")
    }
    boot(apiLibs.pluginSpi) {
        exclude(group = "org.checkerframework", module = "checker-qual")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    boot(libs.lmaxDisruptor)
    boot(apiLibs.checkerQual)

    boot(libs.terminalConsoleAppender) {
        exclude(group = "org.jline", module = "jline-reader")
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
    }
    boot(libs.jline.terminal)
    boot(libs.jline.reader)
    boot(libs.jline.terminalJansi)

    boot(libs.log4j.jpl)
    boot(libs.log4j.api)
    boot(libs.log4j.core)
    boot(libs.log4j.slf4j2)

    boot(platform(apiLibs.configurate.bom))
    boot(apiLibs.configurate.core) {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    boot(apiLibs.configurate.hocon) {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    boot(libs.configurate.jackson) {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    boot(libs.mixin)
    boot(libs.mixinextras.common)
    boot(libs.asm.tree)
    boot(libs.guava) {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    // All minecraft deps except itself
    configurations.minecraft.get().resolvedConfiguration.resolvedArtifacts
        .map {
            var id = it.id.componentIdentifier.toString()
            if (it.classifier != null) {
                id += ":" + it.classifier
            }
            id
        }
        .filter { !it.startsWith("net.minecraft:joined") }
        .forEach { boot(it) { isTransitive = false } }

    boot(project(transformersProject.path)) {
        exclude(group = "cpw.mods", module = "modlauncher")
    }

    val game = gameLibrariesConfig.name
    game("org.spongepowered:spongeapi:$apiVersion")
    game(platform(apiLibs.adventure.bom)) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    game(libs.adventure.serializerConfigurate4) {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    game(libs.javaxInject)
    game(libs.adventure.serializerAnsi) {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    val bootShadedLibraries = bootShadedLibrariesConfig.name
    bootShadedLibraries(project(transformersProject.path)) { isTransitive = false }

    val gameShadedLibraries = gameShadedLibrariesConfig.name
    gameShadedLibraries("org.spongepowered:spongeapi:$apiVersion") { isTransitive = false }

    spongeImpl.copyModulesExcludingProvided(gameLibrariesConfig.get(), bootLayerConfig.get(), gameManagedLibrariesConfig.get())

    val runOnly = vanillaMain.runtimeOnlyConfigurationName
    testPluginsProject?.also {
        runOnly(project(it.path))
    }
}

minecraft {
    runs {
        // Full development environment
        server("runServer") {
            args("--nogui", "--launchTarget", "sponge_server_dev")
        }
        client("runClient") {
            args("--launchTarget", "sponge_client_dev")
        }

        // Lightweight integration tests
        server("integrationTestServer") {
            args("--launchTarget", "sponge_server_it")
        }
        client("integrationTestClient") {
            args("--launchTarget", "sponge_client_it")
        }

        configureEach {
            targetVersion(apiJavaTarget.toInt())
            workingDirectory(project.file("run/"))

            if (org.spongepowered.gradle.vanilla.internal.util.IdeConfigurer.isIdeaImport()) { // todo(zml): promote to API... eventually
                // IntelliJ does not properly report its compatibility
                jvmArgs("-Dterminal.ansi=true", "-Djansi.mode=force")
            }

            jvmArgs(
                "-Dlog4j.configurationFile=log4j2_dev.xml",
                "-Dmixin.dumpTargetOnFailure=true",
                "-Dmixin.debug.verbose=true",
                "-Dmixin.debug.countInjections=true",
                "-Dmixin.debug.strict=true",
                "-Dmixin.debug.strict.unique=false"
            )

            allArgumentProviders += CommandLineArgumentProvider {
                mixinConfigs.asSequence()
                    .flatMap { sequenceOf("--mixin.config", it) }
                    .toList()
            }
            allArgumentProviders += CommandLineArgumentProvider {
                superclassConfigs.asSequence()
                    .flatMap { sequenceOf("--superclass_change.config", it) }
                    .toList()
            }

            // ModLauncher
            // jvmArgs("-Dbsl.debug=true") // Uncomment to debug bootstrap classpath
            mainClass("net.minecraftforge.bootstrap.ForgeBootstrap")

            // Configure resources
            jvmArgs("-Dsponge.dev.root=" + project.rootDir)
            jvmArgs("-Dsponge.dev.boot=" + bootLayerConfig.get().resolvedConfiguration.resolvedArtifacts.joinToString(";") { it.file.name })
            jvmArgs("-Dsponge.dev.gameShaded=" + gameShadedLibrariesConfig.get().resolvedConfiguration.resolvedArtifacts.joinToString(";") { it.file.name })
        }
    }
}

val vanillaManifest = java.manifest {
    attributes(
        "Specification-Title" to "SpongeVanilla",
        "Specification-Vendor" to "SpongePowered",
        "Specification-Version" to apiVersion,
        "Implementation-Title" to project.name,
        "Implementation-Version" to spongeImpl.generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion),
        "Implementation-Vendor" to "SpongePowered"
    )
    // These two are included by most CI's
    System.getenv()["GIT_COMMIT"]?.apply { attributes("Git-Commit" to this) }
    System.getenv()["GIT_BRANCH"]?.apply { attributes("Git-Branch" to this) }
}

vanillaAppLaunch.apply {
    blossom.resources {
        property("minecraftVersion", minecraftVersion)
    }
}
vanillaLaunch.apply {
    blossom.resources {
        property("apiVersion", apiVersion)
        property("minecraftVersion", minecraftVersion)
        property("version", provider { project.version.toString() })
    }
}
vanillaInstaller.apply {
    blossom.javaSources {
        property("minecraftVersion", minecraftVersion)
    }
}

tasks {
    jar {
        manifest.from(vanillaManifest)
    }

    val vanillaInstallerJar by registering(Jar::class) {
        archiveClassifier.set("installer")
        manifest{
            from(vanillaManifest)
            attributes(
                "Main-Class" to "org.spongepowered.vanilla.installer.InstallerMain",
                "Multi-Release" to true
            )
        }
        from(vanillaInstaller.output)
    }
    val vanillaAppLaunchJar by registering(Jar::class) {
        archiveClassifier.set("applaunch")
        manifest.from(vanillaManifest)
        from(vanillaAppLaunch.output)
    }
    val vanillaLaunchJar by registering(Jar::class) {
        archiveClassifier.set("launch")
        manifest.from(vanillaManifest)
        from(vanillaLaunch.output)
    }
    val vanillaAccessorsJar by registering(Jar::class) {
        archiveClassifier.set("accessors")
        manifest.from(vanillaManifest)
        from(vanillaAccessors.output)
    }
    val vanillaMixinsJar by registering(Jar::class) {
        archiveClassifier.set("mixins")
        manifest.from(vanillaManifest)
        from(vanillaMixins.output)
    }

    val integrationTest by registering {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn("integrationTestServer", "integrationTestClient")
    }

    val installerResources = project.layout.buildDirectory.dir("generated/resources/installer")
    vanillaInstaller.resources.srcDir(installerResources)

    val emitDependencies by registering(org.spongepowered.gradle.impl.OutputDependenciesToJson::class) {
        group = "sponge"
        this.dependencies("bootstrap", bootLibrariesConfig)
        this.dependencies("main", gameManagedLibrariesConfig)
        this.excludeDependencies(configurations.minecraft)
        this.excludeDependencies(gameShadedLibrariesConfig)

        outputFile.set(installerResources.map { it.file("sponge-libraries.json") })
    }
    named(vanillaInstaller.processResourcesTaskName).configure {
        dependsOn(emitDependencies)
    }

    val vanillaBootShadowJar by register("bootShadowJar", ShadowJar::class) {
        group = "shadow"
        archiveClassifier.set("boot")

        mergeServiceFiles()
        configurations = listOf(bootShadedLibrariesConfig.get())

        manifest {
            from(vanillaManifest)
            attributes("Automatic-Module-Name" to "spongevanilla.boot")
        }

        from(commonProject.sourceSets.named("applaunch").map { it.output })
        from(vanillaAppLaunch.output)
    }

    val installerShadowJar by register("installerShadowJar", ShadowJar::class) {
        group = "shadow"
        archiveClassifier.set("installer-shadow")

        mergeServiceFiles()
        configurations = listOf(installerLibrariesConfig.get(), initLibrariesConfig.get())
        exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "**/module-info.class")

        manifest {
            from(vanillaManifest)
            attributes(
                "Premain-Class" to "org.spongepowered.vanilla.installer.Agent",
                "Main-Class" to "org.spongepowered.vanilla.installer.InstallerMain",
                "Automatic-Module-Name" to "spongevanilla.installer",
                "Launch-Target" to "sponge_server_prod",
                "Multi-Release" to true
            )
            attributes(mapOf("Implementation-Version" to libs.versions.asm.get()), "org/objectweb/asm/")
        }

        from(vanillaInstaller.output)
    }

    shadowJar {
        group = "shadow"
        archiveClassifier.set("mod")

        mergeServiceFiles()
        configurations = listOf(gameShadedLibrariesConfig.get())

        manifest {
            from(vanillaManifest)
            attributes(
                "Superclass-Transformer" to "common.superclasschange,vanilla.superclasschange",
                "Access-Widener" to "common.accesswidener",
                "MixinConfigs" to mixinConfigs.joinToString(","),
                "Multi-Release" to true
            )
        }

        from(commonProject.sourceSets.main.map { it.output })
        from(commonProject.sourceSets.named("mixins").map { it.output })
        from(commonProject.sourceSets.named("accessors").map { it.output })
        from(commonProject.sourceSets.named("launch").map { it.output })

        from(vanillaLaunch.output)
        from(vanillaAccessors.output)
        from(vanillaMixins.output)
    }

    val universalJar = register("universalJar", Jar::class) {
        group = "build"
        archiveClassifier.set("universal")

        manifest.from(installerShadowJar.manifest)

        from(installerShadowJar.archiveFile.map { zipTree(it) })

        into("jars") {
            from(shadowJar)
            rename("spongevanilla-(.*)-mod.jar", "spongevanilla-mod.jar")

            from(vanillaBootShadowJar)
            rename("spongevanilla-(.*)-boot.jar", "spongevanilla-boot.jar")
        }
    }

    assemble {
        dependsOn(universalJar)
    }
}

val universalJar by tasks.existing
val vanillaInstallerJar by tasks.existing
val vanillaAppLaunchJar by tasks.existing
val vanillaLaunchJar by tasks.existing
val vanillaAccessorsJar by tasks.existing
val vanillaMixinsJar by tasks.existing

publishing {
    publications {
        register("sponge", MavenPublication::class) {
            artifact(tasks["universalJar"])

            artifact(tasks["jar"])
            artifact(tasks["sourcesJar"])

            artifact(tasks["vanillaInstallerJar"])
            artifact(tasks["installerSourcesJar"])

            artifact(tasks["vanillaMixinsJar"])
            artifact(tasks["mixinsSourcesJar"])

            artifact(tasks["vanillaAccessorsJar"])
            artifact(tasks["accessorsSourcesJar"])

            artifact(tasks["vanillaLaunchJar"])
            artifact(tasks["launchSourcesJar"])

            artifact(tasks["vanillaAppLaunchJar"])
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
