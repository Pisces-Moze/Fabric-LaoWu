package cn.moze.catfight.bukkit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

final class RailFinder {
    private RailFinder() { }

    static Location nearestRail(Location center, int horizontal, int vertical) {
        Location best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int y = -vertical; y <= vertical; y++) {
            for (int x = -horizontal; x <= horizontal; x++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    Block block = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (!isRail(block.getType())) continue;
                    double distance = block.getLocation().distanceSquared(center);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = block.getLocation();
                    }
                }
            }
        }
        return best;
    }

    private static boolean isRail(Material material) {
        return material == Material.RAIL || material == Material.POWERED_RAIL
            || material == Material.DETECTOR_RAIL || material == Material.ACTIVATOR_RAIL;
    }
}
