plugins {
    alias(libs.plugins.blossom)
}

val apiVersion: String by project

dependencies {
    annotationProcessor(implementation("org.spongepowered:spongeapi:$apiVersion")!!)
}

sourceSets {
    main {
        blossom.resources {
            property("apiVersion", apiVersion.replace("-SNAPSHOT", ""))
            property("description", description.toString())
        }
    }
}
