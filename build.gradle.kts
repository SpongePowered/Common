import java.util.Locale

plugins {
    id("org.spongepowered.gradle.vanilla") version "0.2-SNAPSHOT"
    `maven-publish`
    `java-library`
    eclipse
    id("org.cadixdev.licenser") version "0.5.1"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.spongepowered.gradle.sponge.dev") version "1.0.2" apply false // for version json generation
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.0"
}

val commonProject = project
val apiVersion: String by project
val minecraftVersion: String by project
val recommendedVersion: String by project

val asmVersion: String by project
val modlauncherVersion: String by project
val mixinVersion: String by project
val pluginSpiVersion: String by project
val guavaVersion: String by project
val junitVersion: String by project

minecraft {
    version(minecraftVersion)
    injectRepositories(false)
    project.sourceSets["main"].resources
            .filter { it.name.endsWith(".accesswidener") }
            .files
            .forEach {
                accessWidener(it)
                parent?.minecraft?.accessWidener(it)
            }
}

val commonManifest = the<JavaPluginConvention>().manifest {
    attributes(
        "Specification-Title" to "Sponge",
        "Specification-Vendor" to "SpongePowered",
        "Specification-Version" to apiVersion,
        "Implementation-Title" to project.name,
        "Implementation-Version" to generateImplementationVersionString(apiVersion, minecraftVersion, recommendedVersion),
        "Implementation-Vendor" to "SpongePowered"
    )
}

tasks {
    jar {
        manifest.from(commonManifest)
    }
    val mixinsJar by registering(Jar::class) {
        archiveClassifier.set("mixins")
        manifest.from(commonManifest)
        from(mixins.map { it.output })
    }
    val accessorsJar by registering(Jar::class) {
        archiveClassifier.set("accessors")
        manifest.from(commonManifest)
        from(accessors.map { it.output })
    }
    val launchJar by registering(Jar::class) {
        archiveClassifier.set("launch")
        manifest.from(commonManifest)
        from(launch.map { it.output })
    }
    val applaunchJar by registering(Jar::class) {
        archiveClassifier.set("applaunch")
        manifest.from(commonManifest)
        from(applaunch.map { it.output })
    }

    test {
        useJUnitPlatform()
    }

}

version = generateImplementationVersionString(apiVersion, minecraftVersion, recommendedVersion)

// Configurations
val applaunchConfig by configurations.register("applaunch")

val launchConfig by configurations.register("launch") {
    extendsFrom(applaunchConfig)
}
val accessorsConfig by configurations.register("accessors") {
    extendsFrom(launchConfig)
}
val mixinsConfig by configurations.register("mixins") {
    extendsFrom(applaunchConfig)
    extendsFrom(launchConfig)
}

// create the sourcesets
val main by sourceSets

val applaunch by sourceSets.registering {
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = this, targetSource = main, implProject = project, dependencyConfigName = main.implementationConfigurationName)
    project.dependencies {
        mixinsConfig(this@registering.output)
    }
    configurations.named(implementationConfigurationName) {
        extendsFrom(applaunchConfig)
    }
}
val launch by sourceSets.registering {
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = applaunch.get(), targetSource = this, implProject = project, dependencyConfigName = this.implementationConfigurationName)
    project.dependencies {
        mixinsConfig(this@registering.output)
    }
    project.dependencies {
        implementation(this@registering.output)
    }

    configurations.named(implementationConfigurationName) {
        extendsFrom(launchConfig)
    }
}

val accessors by sourceSets.registering {
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = launch.get(), targetSource = this, implProject = project, dependencyConfigName = this.implementationConfigurationName)
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = this, targetSource = main, implProject = project, dependencyConfigName = main.implementationConfigurationName)
    configurations.named(implementationConfigurationName) {
        extendsFrom(accessorsConfig)
    }
}
val mixins by sourceSets.registering {
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = launch.get(), targetSource = this, implProject = project, dependencyConfigName = this.implementationConfigurationName)
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = applaunch.get(), targetSource = this, implProject = project, dependencyConfigName = this.implementationConfigurationName)
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = accessors.get(), targetSource = this, implProject = project, dependencyConfigName = this.implementationConfigurationName)
    applyNamedDependencyOnOutput(originProject = project, sourceAdding = main, targetSource = this, implProject = project, dependencyConfigName = this.implementationConfigurationName)
    configurations.named(implementationConfigurationName) {
        extendsFrom(mixinsConfig)
    }
}

dependencies {
    // api
    api("org.spongepowered:spongeapi:$apiVersion")

    // Database stuffs... likely needs to be looked at
    implementation("com.zaxxer:HikariCP:2.6.3")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.0.3")
    implementation("com.h2database:h2:1.4.196")
    implementation("org.xerial:sqlite-jdbc:3.20.0")
    implementation("javax.inject:javax.inject:1")

    // ASM - required for generating event listeners
    implementation("org.ow2.asm:asm-util:$asmVersion")
    implementation("org.ow2.asm:asm-tree:$asmVersion")

    // Implementation-only Adventure
    implementation(platform("net.kyori:adventure-bom:4.7.0"))
    implementation("net.kyori:adventure-serializer-configurate4")

    // Launch Dependencies - Needed to bootstrap the engine(s)
    launchConfig("org.spongepowered:spongeapi:$apiVersion")
    launchConfig(minecraft.minecraftDependency())
    launchConfig("org.spongepowered:plugin-spi:$pluginSpiVersion")
    launchConfig("org.spongepowered:mixin:$mixinVersion")
    launchConfig("org.checkerframework:checker-qual:3.4.1")
    launchConfig("com.google.guava:guava:$guavaVersion") {
        exclude(group = "com.google.code.findbugs", module = "jsr305") // We don't want to use jsr305, use checkerframework
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "org.codehaus.mojo", module = "animal-sniffer-annotations")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }
    launchConfig("com.google.code.gson:gson:2.8.0")
    launchConfig("org.ow2.asm:asm-tree:$asmVersion")
    launchConfig("org.ow2.asm:asm-util:$asmVersion")

    // Applaunch -- initialization that needs to occur without game access
    applaunchConfig("org.spongepowered:plugin-spi:$pluginSpiVersion")
    applaunchConfig("org.apache.logging.log4j:log4j-api:2.11.2")
    applaunchConfig("com.google.guava:guava:$guavaVersion")
    applaunchConfig(platform("org.spongepowered:configurate-bom:4.0.0"))
    applaunchConfig("org.spongepowered:configurate-core") {
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
    }
    applaunchConfig("org.spongepowered:configurate-hocon") {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
    }
    applaunchConfig("org.spongepowered:configurate-jackson") {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
    }
    applaunchConfig("org.apache.logging.log4j:log4j-core:2.11.2")

    mixinsConfig(sourceSets.named("main").map { it.output })
    add(mixins.get().implementationConfigurationName, "org.spongepowered:spongeapi:$apiVersion")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

fun debug(logger: Logger, messsage: String) {
    println(message = messsage)
    if (System.getProperty("sponge.gradleDebug", "false")!!.toBoolean()) {
        logger.lifecycle(messsage)
    }
}
fun applyNamedDependencyOnOutput(originProject: Project, sourceAdding: SourceSet, targetSource: SourceSet, implProject: Project, dependencyConfigName: String) {
    debug(implProject.logger, "[${implProject.name}] Adding ${originProject.path}(${sourceAdding.name}) to ${implProject.path}(${targetSource.name}).$dependencyConfigName")
    implProject.dependencies.add(dependencyConfigName, sourceAdding.output)
}

fun generateImplementationVersionString(apiVersion: String, minecraftVersion: String, implRecommendedVersion: String, addedVersionInfo: String? = null): String {
    val apiSplit = apiVersion.replace("-SNAPSHOT", "").split(".")
    val minor = if (apiSplit.size > 1) apiSplit[1] else (if (apiSplit.size > 0) apiSplit.last() else "-1")
    val apiReleaseVersion = "${apiSplit[0]}.$minor"
    return listOfNotNull(minecraftVersion, addedVersionInfo, "$apiReleaseVersion.$implRecommendedVersion").joinToString("-")
}
fun generatePlatformBuildVersionString(apiVersion: String, minecraftVersion: String, implRecommendedVersion: String, addedVersionInfo: String? = null): String {
    val isRelease = !implRecommendedVersion.endsWith("-SNAPSHOT")
    println("Detected Implementation Version $implRecommendedVersion as ${if (isRelease) "Release" else "Snapshot"}")
    val apiSplit = apiVersion.replace("-SNAPSHOT", "").split(".")
    val minor = if (apiSplit.size > 1) apiSplit[1] else (if (apiSplit.size > 0) apiSplit.last() else "-1")
    val apiReleaseVersion = "${apiSplit[0]}.$minor"
    val buildNumber = Integer.parseInt(System.getenv("BUILD_NUMBER") ?: "0")
    val implVersionAsReleaseCandidateOrRecommended: String = if (isRelease) {
        "$apiReleaseVersion.$implRecommendedVersion"
    } else {
        "$apiReleaseVersion.${implRecommendedVersion.replace("-SNAPSHOT", "")}-RC$buildNumber"
    }
    return listOfNotNull(minecraftVersion, addedVersionInfo, implVersionAsReleaseCandidateOrRecommended).joinToString("-")
}

val organization: String by project
val projectUrl: String by project
license {
    (this as ExtensionAware).extra.apply {
        this["name"] = "Sponge"
        this["organization"] = organization
        this["url"] = projectUrl
    }
    header = rootProject.file("HEADER.txt")

    include("**/*.java")
    newLine = false
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            // https://github.com/zml2008/guice/tree/backport/5.0.1
            substitute(module("com.google.inject:guice:5.0.1"))
                    .because("We need to run against Guava 21")
                    .using(module("ca.stellardrift.guice-backport:guice:5.0.1"))
        }
    }

    apply(plugin = "org.jetbrains.gradle.plugin.idea-ext")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    base {
        archivesBaseName = name.toLowerCase(Locale.ENGLISH)
    }

    idea {
        if (project != null) {
            (project as ExtensionAware).extensions["settings"].run {
                /* (this as ExtensionAware).extensions.getByType(org.jetbrains.gradle.ext.ActionDelegationConfig::class).run {
                    delegateBuildRunToGradle = false
                    testRunner = org.jetbrains.gradle.ext.ActionDelegationConfig.TestRunner.PLATFORM
                } */ // TODO: Make this default once we can silence the Mixin AP
                (this as ExtensionAware).extensions.getByType(org.jetbrains.gradle.ext.IdeaCompilerConfiguration::class).run {
                    addNotNullAssertions = false
                    useReleaseOption = JavaVersion.current().isJava10Compatible
                    parallelCompilation = true
                }
            }
        }
    }

    tasks {
        withType(JavaCompile::class).configureEach {
            options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
            options.encoding = "UTF-8"
            if (JavaVersion.current().isJava10Compatible) {
                options.release.set(8)
            }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    tasks.withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    val spongeSnapshotRepo: String? by project
    val spongeReleaseRepo: String? by project
    tasks {

        withType<PublishToMavenRepository>().configureEach {
            onlyIf {
                (repository == publishing.repositories["GitHubPackages"] &&
                        !(rootProject.version as String).endsWith("-SNAPSHOT")) ||
                        (!spongeSnapshotRepo.isNullOrBlank()
                                && !spongeReleaseRepo.isNullOrBlank()
                                && repository == publishing.repositories["spongeRepo"]
                                && publication == publishing.publications["sponge"])

            }
        }
    }
    sourceSets.configureEach {
        val sourceSet = this
        val sourceJarName: String = if ("main".equals(this.name)) "sourceJar" else "${this.name}SourceJar"
        tasks.register(sourceJarName, Jar::class.java) {
            group = "build"
            val classifier = if ("main".equals(sourceSet.name)) "sources" else "${sourceSet.name}sources"
            archiveClassifier.set(classifier)
            from(sourceSet.allJava)
        }
    }
    afterEvaluate {
        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    this.url = uri("https://maven.pkg.github.com/SpongePowered/${rootProject.name}")
                    credentials {
                        username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                        password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                    }
                }
                // Set by the build server
                maven {
                    name = "spongeRepo"
                    val repoUrl = if ((version as String).endsWith("-SNAPSHOT")) spongeSnapshotRepo else spongeReleaseRepo
                    repoUrl?.apply {
                        url = uri(this)
                    }
                    val spongeUsername: String? by project
                    val spongePassword: String? by project
                    credentials {
                        username = spongeUsername ?: ""
                        password = spongePassword ?: ""
                    }
                }
            }
        }
    }
}

tasks {
    val jar by existing
    val sourceJar by existing
    val mixinsJar by existing
    val accessorsJar by existing
    val launchJar by existing
    val applaunchJar by existing
    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("dev")
        manifest {
            attributes(mapOf(
                    "Access-Widener" to "common.accesswidener",
                    "Multi-Release" to true
            ))
            from(commonManifest)
        }
        from(jar)
        from(sourceJar)
        from(mixinsJar)
        from(accessorsJar)
        from(launchJar)
        from(applaunchJar)
        dependencies {
            include(project(":"))
        }
    }
}
publishing {
    publications {
        register("sponge", MavenPublication::class) {
            from(components["java"])

            artifact(tasks["sourceJar"])
            artifact(tasks["mixinsJar"])
            artifact(tasks["accessorsJar"])
            artifact(tasks["launchJar"])
            artifact(tasks["applaunchJar"])
            artifact(tasks["applaunchSourceJar"])
            artifact(tasks["launchSourceJar"])
            artifact(tasks["mixinsSourceJar"])
            artifact(tasks["accessorsSourceJar"])
            pom {
                artifactId = project.name.toLowerCase()
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
data class ProjectDep(val group: String, val module: String, val version: String)


val testplugins: Project? = subprojects.find { "testplugins".equals(it.name) }
if (testplugins != null) {
    project("testplugins") {
        apply {
            plugin("java-library")
            plugin("eclipse")
            plugin("org.cadixdev.licenser")
        }

        dependencies {
            annotationProcessor(implementation("org.spongepowered:spongeapi:$apiVersion")!!)
        }

        tasks.jar {
            manifest {
                attributes("Loader" to "java_plain")
            }
        }
        license {
            (this as ExtensionAware).extra.apply {
                this["name"] = "Sponge"
                this["organization"] = organization
                this["url"] = projectUrl
            }
            header = rootProject.file("HEADER.txt")

            include("**/*.java")
            newLine = false
        }
    }
}

project("SpongeVanilla") {
    val vanillaProject = this
    apply {
        plugin("org.spongepowered.gradle.vanilla")
        plugin("java-library")
        plugin("maven-publish")
        plugin("org.cadixdev.licenser")
        plugin("com.github.johnrengelman.shadow")
    }

    description = "The SpongeAPI implementation for Vanilla Minecraft"
    version = generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion)
    println("SpongeVanilla Version $version")

    val vanillaLibrariesConfig by configurations.register("libraries") {
    }
    val vanillaAppLaunchConfig by configurations.register("applaunch") {
        extendsFrom(vanillaLibrariesConfig)
    }
    val vanillaInstallerConfig by configurations.register("installer") {
    }

    val vanillaInstaller by sourceSets.register("installer") {
    }

    val vanillaInstallerJava9 by sourceSets.register("installerJava9") {
        this.java.setSrcDirs(setOf("src/installer/java9"))
        compileClasspath += vanillaInstaller.compileClasspath
        compileClasspath += vanillaInstaller.runtimeClasspath

        tasks.named(compileJavaTaskName, JavaCompile::class) {
            options.release.set(9)
            if (JavaVersion.current() < JavaVersion.VERSION_11) {
                javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(11)) })
            }
        }

        dependencies.add(implementationConfigurationName, objects.fileCollection().from(vanillaInstaller.output.classesDirs))
    }

    val vanillaMain by sourceSets.named("main") {
        // implementation (compile) dependencies
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = accessors.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = launch.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = applaunch.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        configurations.named(implementationConfigurationName) {
            extendsFrom(vanillaLibrariesConfig)
        }
    }
    val vanillaLaunch by sourceSets.register("launch") {
        // implementation (compile) dependencies
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = launch.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = applaunch.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = main, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = this, targetSource = vanillaMain, implProject = vanillaProject, dependencyConfigName = vanillaMain.implementationConfigurationName)

        configurations.named(implementationConfigurationName) {
            extendsFrom(vanillaAppLaunchConfig)
        }
    }
    val vanillaMixins by sourceSets.register("mixins") {
        // implementation (compile) dependencies
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = mixins.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = accessors.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = launch.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = applaunch.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = main, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = vanillaMain, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = vanillaLaunch, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
    }
    val vanillaAppLaunch by sourceSets.register("applaunch") {
        // implementation (compile) dependencies
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = applaunch.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = launch.get(), targetSource = vanillaLaunch, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = vanillaInstaller, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.implementationConfigurationName)
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = this, targetSource = vanillaLaunch, implProject = vanillaProject, dependencyConfigName = vanillaLaunch.implementationConfigurationName)
        // runtime dependencies - literally add the rest of the project, because we want to launch the game
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = vanillaMixins, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.runtimeOnlyConfigurationName)
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = vanillaLaunch, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.runtimeOnlyConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = mixins.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.runtimeOnlyConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = main, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.runtimeOnlyConfigurationName)
        applyNamedDependencyOnOutput(originProject = commonProject, sourceAdding = accessors.get(), targetSource = this, implProject = vanillaProject, dependencyConfigName = this.runtimeOnlyConfigurationName)
        applyNamedDependencyOnOutput(originProject = vanillaProject, sourceAdding = vanillaMain, targetSource = this, implProject = vanillaProject, dependencyConfigName = this.runtimeOnlyConfigurationName)
    }
    val vanillaMixinsImplementation by configurations.named(vanillaMixins.implementationConfigurationName) {
        extendsFrom(vanillaAppLaunchConfig)
    }
    configurations.named(vanillaInstaller.implementationConfigurationName) {
        extendsFrom(vanillaInstallerConfig)
    }
    configurations.named(vanillaAppLaunch.implementationConfigurationName) {
        extendsFrom(vanillaAppLaunchConfig)
        extendsFrom(launchConfig)
    }
    val vanillaAppLaunchRuntime by configurations.named(vanillaAppLaunch.runtimeOnlyConfigurationName)

    minecraft {
        version(minecraftVersion)
        injectRepositories().set(false)
        runs {
            // Full development environment
            sequenceOf(8, 11, 16).forEach {
                server("runJava${it}Server") {
                    args("--nogui", "--launchTarget", "sponge_server_dev")
                }
                client("runJava${it}Client") {
                    args("--launchTarget", "sponge_client_dev")
                }
                tasks.named("runJava${it}Server", JavaExec::class).configure {
                    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(it)) })
                }
                tasks.named("runJava${it}Client", JavaExec::class).configure {
                    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(it)) })
                }
            }

            // Lightweight integration tests
            server("integrationTestServer") {
                args("--launchTarget", "sponge_server_it")
            }
            client("integrationTestClient") {
                args("--launchTarget", "sponge_client_it")
            }

            configureEach {
                workingDirectory().set(vanillaProject.file("run/"))
                if (System.getProperty("idea.active")?.toBoolean() == true) {
                    // IntelliJ does not properly report its compatibility
                    jvmArgs("-Dterminal.ansi=true", "-Djansi.mode=force")
                }
                jvmArgs("-Dlog4j.configurationFile=log4j2_dev.xml")
                allJvmArgumentProviders() += CommandLineArgumentProvider {
                    // Resolve the Mixin artifact for use as a reload agent
                    val mixinJar = vanillaAppLaunchConfig.resolvedConfiguration
                            .getFiles { it.name == "mixin" && it.group == "org.spongepowered" }
                            .firstOrNull()

                    val base = if (mixinJar != null) {
                        listOf("-javaagent:$mixinJar")
                    } else {
                        emptyList()
                    }

                    // Then add necessary module cracks
                    if (!this.name.contains("Java8")) {
                        base + listOf(
                                "--illegal-access=deny", // enable strict mode in prep for Java 16
                                "--add-exports=java.base/sun.security.util=ALL-UNNAMED", // ModLauncher
                                "--add-opens=java.base/java.util.jar=ALL-UNNAMED" // ModLauncher
                        )
                    } else {
                        base
                    }
                }
                mainClass().set("org.spongepowered.vanilla.applaunch.Main")
                classpath().from(vanillaAppLaunch.runtimeClasspath, vanillaAppLaunch.output)
                ideaRunSourceSet().set(vanillaAppLaunch)
            }
        }
        commonProject.sourceSets["main"].resources
                .filter { it.name.endsWith(".accesswidener") }
                .files
                .forEach {
                    accessWidener(it)
                }

        vanillaProject.sourceSets["main"].resources
                .filter { it.name.endsWith(".accesswidener") }
                .files
                .forEach { accessWidener(it) }
    }

    dependencies {
        val jlineVersion: String by project
        api(launch.map { it.output })
        implementation(accessors.map { it.output })
        implementation(project(commonProject.path))

        vanillaMixinsImplementation(project(commonProject.path))
        add(vanillaLaunch.implementationConfigurationName, "org.spongepowered:spongeapi:$apiVersion")
        add(vanillaAppLaunch.implementationConfigurationName, vanillaProject.minecraft.minecraftDependency())
        add(vanillaLaunch.implementationConfigurationName, vanillaProject.minecraft.minecraftDependency())
        add(vanillaMixins.implementationConfigurationName, vanillaProject.minecraft.minecraftDependency())

        vanillaInstallerConfig("com.google.code.gson:gson:2.8.0")
        vanillaInstallerConfig("org.spongepowered:configurate-hocon:4.0.0")
        vanillaInstallerConfig("org.spongepowered:configurate-core:4.0.0")
        vanillaInstallerConfig("net.sf.jopt-simple:jopt-simple:5.0.3")
        vanillaInstallerConfig("org.tinylog:tinylog-api:2.2.1")
        vanillaInstallerConfig("org.tinylog:tinylog-impl:2.2.1")
        // Override ASM versions, and explicitly declare dependencies so ASM is excluded from the manifest.
        val asmExclusions = sequenceOf("-commons", "-tree", "-analysis", "")
                .map { "asm$it" }
                .onEach {
            vanillaInstallerConfig("org.ow2.asm:$it:$asmVersion")
        }.toSet()
        vanillaInstallerConfig("org.cadixdev:atlas:0.2.1") {
            asmExclusions.forEach { exclude(group = "org.ow2.asm", module = it) } // Use our own ASM version
        }
        vanillaInstallerConfig("org.cadixdev:lorenz-asm:0.5.6") {
            asmExclusions.forEach { exclude(group = "org.ow2.asm", module = it) } // Use our own ASM version
        }
        vanillaInstallerConfig("org.cadixdev:lorenz-io-proguard:0.5.6")

        vanillaAppLaunchConfig("org.spongepowered:spongeapi:$apiVersion")
        // vanillaAppLaunchConfig(vanillaProject.minecraft.minecraftDependency())
        vanillaAppLaunchConfig(platform("net.kyori:adventure-bom:4.7.0"))
        vanillaAppLaunchConfig("net.kyori:adventure-serializer-configurate4")
        vanillaAppLaunchConfig("org.spongepowered:mixin:$mixinVersion")
        vanillaAppLaunchConfig("org.ow2.asm:asm-util:$asmVersion")
        vanillaAppLaunchConfig("org.ow2.asm:asm-tree:$asmVersion")
        vanillaAppLaunchConfig("com.google.guava:guava:$guavaVersion")
        vanillaAppLaunchConfig("org.spongepowered:plugin-spi:$pluginSpiVersion")
        vanillaAppLaunchConfig("javax.inject:javax.inject:1")
        vanillaAppLaunchConfig("org.apache.logging.log4j:log4j-api:2.11.2")
        vanillaAppLaunchConfig("org.apache.logging.log4j:log4j-core:2.11.2")
        vanillaAppLaunchConfig("com.lmax:disruptor:3.4.2")
        vanillaAppLaunchConfig("com.zaxxer:HikariCP:2.6.3")
        vanillaAppLaunchConfig("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
        vanillaAppLaunchConfig(platform("org.spongepowered:configurate-bom:4.0.0"))
        vanillaAppLaunchConfig("org.spongepowered:configurate-core") {
            exclude(group = "org.checkerframework", module = "checker-qual")
        }
        vanillaAppLaunchConfig("org.spongepowered:configurate-hocon") {
            exclude(group = "org.spongepowered", module = "configurate-core")
            exclude(group = "org.checkerframework", module = "checker-qual")
        }
        vanillaAppLaunchConfig("org.spongepowered:configurate-jackson") {
            exclude(group = "org.spongepowered", module = "configurate-core")
            exclude(group = "org.checkerframework", module = "checker-qual")
        }

        vanillaLibrariesConfig("net.minecrell:terminalconsoleappender:1.2.0")
        vanillaLibrariesConfig("org.jline:jline-terminal:$jlineVersion")
        vanillaLibrariesConfig("org.jline:jline-reader:$jlineVersion")
        vanillaLibrariesConfig("org.jline:jline-terminal-jansi:$jlineVersion") {
            exclude("org.fusesource.jansi") // Use our own JAnsi
        }
        // A newer version is required to make log4j happy
        vanillaLibrariesConfig("org.fusesource.jansi:jansi:2.3.1")

        // Launch Dependencies - Needed to bootstrap the engine(s)
        // The ModLauncher compatibility launch layer
        vanillaAppLaunchConfig("cpw.mods:modlauncher:$modlauncherVersion") {
            exclude(group = "org.apache.logging.log4j")
        }
        vanillaAppLaunchConfig("org.ow2.asm:asm-commons:$asmVersion")
        vanillaAppLaunchConfig("cpw.mods:grossjava9hacks:1.3.3") {
            exclude(group = "org.apache.logging.log4j")
        }
        vanillaAppLaunchConfig("net.fabricmc:access-widener:1.0.2") {
            exclude(group = "org.apache.logging.log4j")
        }

        testplugins?.apply {
            vanillaAppLaunchRuntime(project(testplugins.path)) {
                exclude(group = "org.spongepowered")
            }
        }
    }

    val vanillaManifest = the<JavaPluginConvention>().manifest {
        attributes(
            "Specification-Title" to "SpongeVanilla",
            "Specification-Vendor" to "SpongePowered",
            "Specification-Version" to apiVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to generatePlatformBuildVersionString(apiVersion, minecraftVersion, recommendedVersion),
            "Implementation-Vendor" to "SpongePowered"
        )
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
                    "Premain-Class" to "org.spongepowered.vanilla.installer.Agent",
                    "Agent-Class" to "org.spongepowered.vanilla.installer.Agent",
                    "Launcher-Agent-Class" to "org.spongepowered.vanilla.installer.Agent",
                    "Multi-Release" to true
                )
            }
            from(vanillaInstaller.output)
            into("META-INF/versions/9/") {
                from(vanillaInstallerJava9.output)
            }
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
        val vanillaMixinsJar by registering(Jar::class) {
            archiveClassifier.set("mixins")
            manifest.from(vanillaManifest)
            from(vanillaMixins.output)
        }

        val integrationTest by registering {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            dependsOn("integrationTestServer", "integrationTestClient")
        }

        val installerTemplateSource = vanillaProject.file("src/installer/templates")
        val installerTemplateDest = vanillaProject.layout.buildDirectory.dir("generated/sources/installerTemplates")
        val generateInstallerTemplates by registering(Copy::class) {
            group = "sponge"
            description = "Generate classes from templates for the SpongeVanilla installer"
            val properties = mutableMapOf(
                "minecraftVersion" to minecraftVersion
            )
            inputs.properties(properties)

            // Copy template
            from(installerTemplateSource)
            into(installerTemplateDest)
            expand(properties)
        }
        vanillaInstaller.java.srcDir(generateInstallerTemplates.map { it.outputs })

        val installerResources = vanillaProject.layout.buildDirectory.dir("generated/resources/installer")
        vanillaInstaller.resources.srcDir(installerResources)
        val emitDependencies by registering(org.spongepowered.gradle.convention.task.OutputDependenciesToJson::class) {
            group = "sponge"
            // everything in applaunch
            configuration.set(vanillaAppLaunchConfig)
            // except what we're providing through the installer
            excludeConfiguration.set(vanillaInstallerConfig)
            // for accesstransformers
            allowedClassifiers.add("service")

            outputFile.set(installerResources.map { it.file("libraries.json") })
        }
        named(vanillaInstaller.processResourcesTaskName).configure {
            dependsOn(emitDependencies)
        }

        shadowJar {
            mergeServiceFiles()

            archiveClassifier.set("universal")
            manifest {
                attributes(mapOf(
                    "Access-Widener" to "common.accesswidener",
                    "Main-Class" to "org.spongepowered.vanilla.installer.InstallerMain",
                    "Launch-Target" to "sponge_server_prod",
                    "Multi-Release" to true,
                    "Premain-Class" to "org.spongepowered.vanilla.installer.Agent",
                    "Agent-Class" to "org.spongepowered.vanilla.installer.Agent",
                    "Launcher-Agent-Class" to "org.spongepowered.vanilla.installer.Agent"
                ))
                from(vanillaManifest)
            }
            from(commonProject.tasks.jar)
            from(commonProject.tasks.named("mixinsJar"))
            from(commonProject.tasks.named("accessorsJar"))
            from(commonProject.tasks.named("launchJar"))
            from(commonProject.tasks.named("applaunchJar"))
            from(jar)
            from(vanillaInstallerJar)
            from(vanillaAppLaunchJar)
            from(vanillaLaunchJar)
            from(vanillaMixinsJar)
            from(vanillaInstallerConfig)
            dependencies {
                include(project(":"))
                include("org.spongepowered:spongeapi:$apiVersion")
            }

            // We cannot have modules in a shaded jar
            exclude("META-INF/versions/*/module-info.class")
            exclude("module-info.class")
        }
        assemble {
            dependsOn(shadowJar)
        }
    }

    license {
        (this as ExtensionAware).extra.apply {
            this["name"] = "Sponge"
            this["organization"] = organization
            this["url"] = projectUrl
        }
        header = rootProject.file("HEADER.txt")

        include("**/*.java")
        newLine = false
    }

    val shadowJar by tasks.existing
    val vanillaInstallerJar by tasks.existing
    val vanillaAppLaunchJar by tasks.existing
    val vanillaLaunchJar by tasks.existing
    val vanillaMixinsJar by tasks.existing

    publishing {
        publications {
            register("sponge", MavenPublication::class) {

                artifact(shadowJar.get())
                artifact(vanillaInstallerJar.get())
                artifact(vanillaAppLaunchJar.get())
                artifact(vanillaLaunchJar.get())
                artifact(vanillaMixinsJar.get())
                artifact(tasks["applaunchSourceJar"])
                artifact(tasks["launchSourceJar"])
                artifact(tasks["mixinsSourceJar"])
                pom {
                    artifactId = project.name.toLowerCase()
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
}