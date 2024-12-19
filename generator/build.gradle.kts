plugins {
    id("org.spongepowered.gradle.vanilla")
}

description = "Code generator for automatically producing API catalog classes based off of Vanilla MC data"

minecraft {
    rootProject.sourceSets["main"].resources
            .filter { it.name.endsWith(".accesswidener") }
            .files
            .forEach {
                accessWideners(it)
            }
}

configurations.configureEach {
    val dep = libs.log4j.slf4j2.get()
    exclude(group = dep.group, module = dep.name)
}

dependencies {
    implementation(libs.javapoet)
    implementation(libs.javaparser)
    implementation(libs.tinylog.api)
    runtimeOnly(libs.tinylog.impl)
    runtimeOnly(libs.tinylog.slf4j)
}

val apiBase = rootProject.file("SpongeAPI/src/main/java/")
val temporaryLicenseHeader = project.layout.buildDirectory.file("api-gen-license-header.txt")
tasks.register("generateApiData", JavaExec::class) {
    group = "sponge"
    description = "Generate API Catalog classes"
    javaLauncher.set(project.javaToolchains.launcherFor(java.toolchain))

    classpath(sourceSets.main.map { it.output }, sourceSets.main.map { it.runtimeClasspath })
    mainClass.set("org.spongepowered.vanilla.generator.GeneratorMain")
    args(apiBase.canonicalPath, temporaryLicenseHeader.get().asFile.canonicalPath)

    doFirst {
        // Write a template-expanded license header to the temporary file
        indraSpotlessLicenser.licenseHeaderFile().get().asReader().buffered().use { reader ->
            val template = groovy.text.GStringTemplateEngine().createTemplate(reader)

            val propertyMap = indraSpotlessLicenser.properties().get().toMutableMap()
            propertyMap["name"] = "SpongeAPI"
            val out = template.make(propertyMap)

            temporaryLicenseHeader.get().asFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                out.writeTo(writer)
            }
        }
    }
}
