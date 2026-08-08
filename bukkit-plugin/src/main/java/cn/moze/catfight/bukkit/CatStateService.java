package cn.moze.catfight.bukkit;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Cat;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

final class CatStateService {
    private final NamespacedKey flatKey;
    private final NamespacedKey restoreClicksKey;
    private final NamespacedKey projectionDirectionKey;
    private final NamespacedKey projectionPitchKey;

    CatStateService(CatFightPlugin plugin) {
        this.flatKey = new NamespacedKey(plugin, "flat");
        this.restoreClicksKey = new NamespacedKey(plugin, "restore_clicks");
        this.projectionDirectionKey = new NamespacedKey(plugin, "projection_direction");
        this.projectionPitchKey = new NamespacedKey(plugin, "projection_pitch");
    }

    boolean isFlat(Cat cat) {
        return cat.getPersistentDataContainer().getOrDefault(flatKey, PersistentDataType.BYTE, (byte) 0) != 0;
    }

    void flatten(Cat cat, float viewerYaw, float viewerPitch) {
        PersistentDataContainer data = cat.getPersistentDataContainer();
        float relativeYaw = normalizeDegrees(viewerYaw - cat.getLocation().getYaw());
        int direction = Math.floorMod(Math.round(relativeYaw / 22.5F), 16);
        data.set(flatKey, PersistentDataType.BYTE, (byte) 1);
        data.set(restoreClicksKey, PersistentDataType.INTEGER, 0);
        data.set(projectionDirectionKey, PersistentDataType.INTEGER, direction);
        data.set(projectionPitchKey, PersistentDataType.INTEGER, Math.round(viewerPitch));
    }

    boolean restoreClick(Cat cat, int requiredClicks) {
        PersistentDataContainer data = cat.getPersistentDataContainer();
        int clicks = data.getOrDefault(restoreClicksKey, PersistentDataType.INTEGER, 0) + 1;
        if (clicks >= requiredClicks) {
            clearFlat(cat);
            return true;
        }
        data.set(restoreClicksKey, PersistentDataType.INTEGER, clicks);
        return false;
    }

    void clearFlat(Cat cat) {
        PersistentDataContainer data = cat.getPersistentDataContainer();
        data.remove(flatKey);
        data.remove(restoreClicksKey);
        data.remove(projectionDirectionKey);
        data.remove(projectionPitchKey);
    }

    int projectionDirection(Cat cat) {
        return cat.getPersistentDataContainer().getOrDefault(projectionDirectionKey, PersistentDataType.INTEGER, 0);
    }

    private static float normalizeDegrees(float degrees) {
        float result = degrees % 360.0F;
        if (result >= 180.0F) result -= 360.0F;
        if (result < -180.0F) result += 360.0F;
        return result;
    }
}
