package cn.moze.catfight.bukkit;

import java.nio.file.Path;

/** Standalone pack generator used by release builds and server administrators. */
public final class ResourcePackBuildTool {
    private ResourcePackBuildTool() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ResourcePackBuildTool <output.zip> <pack-format>");
        }
        ResourcePackBuilder.build(Path.of(args[0]), Integer.parseInt(args[1]), ResourcePackBuildTool.class.getClassLoader());
    }
}
