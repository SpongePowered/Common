import org.spongepowered.gradle.vanilla.task.DecompileJarTask
import java.util.Locale

plugins {
    `maven-publish`
    `java-library`
    eclipse
    id("org.spongepowered.gradle.vanilla")
    alias(libs.plugins.shadow)
    alias(apiLibs.plugins.spongeGradle.convention) apply false // for version json generation
    alias(libs.plugins.indra.licenserSpotless) version apiLibs.versions.indra.get()
    id("implementation-structure")
    id(apiLibs.plugins.ideaExt.get().pluginId)
    alias(libs.plugins.versions)
}

val commonProject = project
val apiVersion: String by project
val apiJavaTarget: String by project
val minecraftVersion: String by project
val recommendedVersion: String by project
val organization: String by project
val projectUrl: String by project

version = spongeImpl.generateImplementationVersionString(apiVersion, minecraftVersion, recommendedVersion)

val commonManifest = java.manifest {
    attributes(
        "Specification-Title" to "Sponge",
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

// SpongeCommon configurations
val applaunchConfig by configurations.register("applaunch")

val launchConfig by configurations.register("launch") {
    extendsFrom(configurations.minecraft.get())
    extendsFrom(applaunchConfig)
}
val accessorsConfig by configurations.register("accessors") {
    extendsFrom(launchConfig)
}
val mixinsConfig by configurations.register("mixins") {
    extendsFrom(applaunchConfig)
    extendsFrom(launchConfig)
}

// SpongeCommon source sets
val main by sourceSets

val applaunch by sourceSets.registering {
    spongeImpl.addDependencyToImplementation(this, main)

    configurations.named(implementationConfigurationName) {
        extendsFrom(applaunchConfig)
    }
}
val launch by sourceSets.registering {
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(this, main)

    configurations.named(implementationConfigurationName) {
        extendsFrom(launchConfig)
    }
}
val accessors by sourceSets.registering {
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(this, main)

    configurations.named(implementationConfigurationName) {
        extendsFrom(accessorsConfig)
    }
}
val mixins by sourceSets.registering {
    spongeImpl.addDependencyToImplementation(launch.get(), this)
    spongeImpl.addDependencyToImplementation(applaunch.get(), this)
    spongeImpl.addDependencyToImplementation(accessors.get(), this)
    spongeImpl.addDependencyToImplementation(main, this)

    configurations.named(implementationConfigurationName) {
        extendsFrom(mixinsConfig)
    }
}

dependencies {
    // api
    api("org.spongepowered:spongeapi:$apiVersion")

    implementation(libs.javaxInject)

    // ASM - required for generating event listeners
    implementation(libs.asm.util)
    implementation(libs.asm.tree)

    // Implementation-only Adventure
    implementation(platform(apiLibs.adventure.bom)) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    implementation(libs.adventure.serializerConfigurate4) {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    implementation(libs.adventure.serializerAnsi) {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    // Launch Dependencies - Needed to bootstrap the engine(s)
    launchConfig("org.spongepowered:spongeapi:$apiVersion")
    launchConfig(apiLibs.pluginSpi) {
        exclude(group = "org.checkerframework", module = "checker-qual")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }
    launchConfig(libs.mixin)
    launchConfig(apiLibs.checkerQual)
    launchConfig(libs.guava) {
        exclude(group = "com.google.code.findbugs", module = "jsr305") // We don't want to use jsr305, use checkerframework
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "org.codehaus.mojo", module = "animal-sniffer-annotations")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }
    launchConfig(apiLibs.gson)
    launchConfig(libs.asm.tree)
    launchConfig(libs.asm.util)

    // Applaunch -- initialization that needs to occur without game access
    applaunchConfig(apiLibs.checkerQual)
    applaunchConfig(libs.log4j.api)
    applaunchConfig(libs.guava) {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    applaunchConfig(platform(apiLibs.configurate.bom))
    applaunchConfig(apiLibs.configurate.core) {
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
    }
    applaunchConfig(apiLibs.configurate.hocon) {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
    }
    applaunchConfig(libs.configurate.jackson) {
        exclude(group = "org.spongepowered", module = "configurate-core")
        exclude(group = "org.checkerframework", module = "checker-qual") // We use our own version
    }
    applaunchConfig(libs.log4j.core)
    applaunchConfig(libs.log4j.jpl)
    applaunchConfig(apiLibs.pluginSpi) {
        exclude(group = "org.checkerframework", module = "checker-qual")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }

    add(mixins.get().implementationConfigurationName, "org.spongepowered:spongeapi:$apiVersion")

    // Tests
    testImplementation(platform(apiLibs.junit.bom))
    testImplementation(apiLibs.junit.api)
    testImplementation(apiLibs.junit.params)
    testRuntimeOnly(apiLibs.junit.engine)
    testRuntimeOnly(apiLibs.junit.launcher)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junitJupiter) {
        exclude(group = "org.junit.jupiter", module = "junit-jupiter-api")
    }
}

minecraft {
    accessWideners(main.resources.filter { it.name.endsWith(".accesswidener") })
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
    apply(plugin = "net.kyori.indra.licenser.spotless")

    base {
        archivesName = name.lowercase(Locale.ENGLISH)
    }

    plugins.withId("org.spongepowered.gradle.vanilla") {
        minecraft {
            version(minecraftVersion)
            injectRepositories(false)
        }

        dependencies {
            decompiler(libs.vineflower)
        }

        tasks.named("decompile", DecompileJarTask::class) {
            extraFernFlowerArgs.put("win", "0")
        }
    }

    idea {
        if (project != null) {
            (project as ExtensionAware).extensions["settings"].run {
                (this as ExtensionAware).extensions.getByType(org.jetbrains.gradle.ext.ActionDelegationConfig::class).run {
                    delegateBuildRunToGradle = false
                    testRunner = org.jetbrains.gradle.ext.ActionDelegationConfig.TestRunner.PLATFORM
                }
                extensions.getByType(org.jetbrains.gradle.ext.IdeaCompilerConfiguration::class).run {
                    addNotNullAssertions = false
                    useReleaseOption = JavaVersion.current().isJava10Compatible
                    parallelCompilation = true
                }
            }
        }
    }

    java {
        val targetJavaVersion = JavaVersion.toVersion(apiJavaTarget.toInt())
        sourceCompatibility = targetJavaVersion
        targetCompatibility = targetJavaVersion
        if (JavaVersion.current() < targetJavaVersion) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(apiJavaTarget.toInt()))
            }
        }
    }

    tasks.withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    spotless {
        java {
            toggleOffOn("@formatter:off", "@formatter:on")
            endWithNewline()
            indentWithSpaces(4)
            trimTrailingWhitespace()
            removeUnusedImports()
            importOrderFile(rootProject.file("SpongeAPI/extra/eclipse/sponge_eclipse.importorder"))
            targetExclude("build/generated/**/*") // exclude generated content
        }
        kotlinGradle {
            endWithNewline()
            indentWithSpaces(4)
            trimTrailingWhitespace()
        }
    }

    indraSpotlessLicenser {
        licenseHeaderFile(rootProject.file("HEADER.txt"))

        property("name", "Sponge")
        property("organization", organization)
        property("url", projectUrl)
    }

    val spongeSnapshotRepo: String? by project
    val spongeReleaseRepo: String? by project
    tasks {
        val emptyAnnotationProcessors = objects.fileCollection()
        withType(JavaCompile::class).configureEach {
            options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
            options.encoding = "UTF-8"
            options.release.set(apiJavaTarget.toInt())
            if (project.name != "testplugins" && System.getProperty("idea.sync.active") != null) {
                options.annotationProcessorPath = emptyAnnotationProcessors // hack so IntelliJ doesn't try to run Mixin AP
            }
        }

        withType(PublishToMavenRepository::class).configureEach {
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
        val isMain = "main" == sourceSet.name

        val sourcesJarName: String = if (isMain) "sourcesJar" else (sourceSet.name + "SourcesJar")
        tasks.register(sourcesJarName, Jar::class.java) {
            group = "build"
            val classifier = if (isMain) "sources" else (sourceSet.name + "-sources")
            archiveClassifier.set(classifier)
            from(sourceSet.allJava)
        }
    }

    tasks.register("printConfigsHierarchy") {
        group = "debug"
        doLast {
            configurations.forEach { conf: Configuration  ->
                val seen = mutableSetOf<Configuration>()
                println("Parents of ${conf.name}:")
                printParents(conf, "", seen)
            }
        }
    }

    tasks.register("printConfigsResolution") {
        group = "debug"
        doLast {
            configurations.forEach { conf: Configuration  ->
                println()
                println("Artifacts of ${conf.name}:")
                if (conf.isCanBeResolved) {
                    try {
                        conf.forEach {
                            println(it)
                        }
                    } catch (e: Exception) {
                        println("error")
                    }
                } else {
                    println("not resolved")
                }
            }
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

fun printParents(conf: Configuration, indent: String, seen: MutableSet<Configuration>) {
    for (parent in conf.extendsFrom) {
        if (parent in seen) {
            continue
        }
        seen.add(parent)
        println("$indent - ${parent.name}")
        printParents(parent, indent + "  ", seen)
    }
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

    val jar by existing
    val sourcesJar by existing

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
        from(sourcesJar)
        from(mixinsJar)
        from(accessorsJar)
        from(launchJar)
        from(applaunchJar)

        dependencies {
            include(project(":"))
        }
    }

    test {
        useJUnitPlatform()
    }

    check {
        dependsOn(gradle.includedBuild("SpongeAPI").task(":check"))
    }

    prepareWorkspace {
        dependsOn(gradle.includedBuild("SpongeAPI").task(":genEventImpl"))
    }
}

publishing {
    publications {
        register("sponge", MavenPublication::class) {
            from(components["java"])
            artifact(tasks["sourcesJar"])

            artifact(tasks["mixinsJar"])
            artifact(tasks["mixinsSourcesJar"])

            artifact(tasks["accessorsJar"])
            artifact(tasks["accessorsSourcesJar"])

            artifact(tasks["launchJar"])
            artifact(tasks["launchSourcesJar"])

            artifact(tasks["applaunchJar"])
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
