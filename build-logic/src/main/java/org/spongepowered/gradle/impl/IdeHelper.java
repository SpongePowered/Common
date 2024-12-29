package org.spongepowered.gradle.impl;

public class IdeHelper {

    public static boolean isIdeaActive() {
        return Boolean.getBoolean("idea.active");
    }

    public static boolean isIdeaSync() {
        return Boolean.getBoolean("idea.sync.active");
    }
}
