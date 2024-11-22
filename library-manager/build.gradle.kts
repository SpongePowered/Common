val organization: String by project
val projectUrl: String by project

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("HEADER.txt"))

    property("name", "Sponge")
    property("organization", organization)
    property("url", projectUrl)
}

dependencies {
    implementation(apiLibs.gson)
}
