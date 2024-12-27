module org.spongepowered.boostrap.dev {
    requires net.minecraftforge.bootstrap.api;
    exports org.spongepowered.bootstrap.dev;

    provides net.minecraftforge.bootstrap.api.BootstrapClasspathModifier with org.spongepowered.bootstrap.dev.SpongeDevClasspathFixer;
}
