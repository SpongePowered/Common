dependencies {
    // AccessWidener transformer
    implementation(libs.accessWidener) {
        exclude(group="org.apache.logging.log4j")
    }
    // ModLauncher inherited dependencies - strictly should be provided by
    //  the platform making use of this project
    compileOnly(libs.log4j.api)
    compileOnly(libs.neo.modlauncher) {
        exclude(group = "org.ow2.asm")
        exclude(group = "org.apache.logging.log4j")
    }

    compileOnly(libs.joptSimple)
    compileOnly(libs.asm.commons)
    // Configurate dependencies, also to be provided by the platform
    //  making use of this project
    compileOnly(platform(apiLibs.configurate.bom))
    compileOnly(apiLibs.configurate.core) {
        exclude(group = "org.checkerframework", module="checker-qual") // We use our own version
    }
    compileOnly(libs.configurate.jackson) {
        exclude(group="org.spongepowered", module="configurate-core")
        exclude(group="org.checkerframework", module="checker-qual") // We use our own version
    }

    // And finally, compile only annotations
    compileOnly(apiLibs.checkerQual)
}

tasks {
    jar {
        manifest {
            attributes("Automatic-Module-Name" to "sponge.modlauncher.transformers")
        }
    }
}
