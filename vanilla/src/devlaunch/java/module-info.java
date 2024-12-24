module org.spongepowered.vanilla.devlaunch {
    requires net.minecraftforge.bootstrap.api;
    exports org.spongepowered.vanilla.devlaunch;

    provides net.minecraftforge.bootstrap.api.BootstrapClasspathModifier with org.spongepowered.vanilla.devlaunch.SpongeDevClasspathFixer;
}
