dependencies {
    implementation(apiLibs.gson)
}

tasks {
    jar {
        manifest {
            attributes("Automatic-Module-Name" to "sponge.library_manager")
        }
    }
}
