package cn.moze.catfight.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

final class CatVisualManager implements Runnable {
    private final CatFightPlugin plugin;
    private final NamespacedKey visualKey;
    private final NamespacedKey hiddenKey;
    private final NamespacedKey originalInvisibleKey;
    private final NamespacedKey originalAiKey;
    private final Map<UUID, Rig> rigs = new HashMap<>();
    private boolean enabled;
    private long tick;

    CatVisualManager(CatFightPlugin plugin) {
        this.plugin = plugin;
        this.visualKey = new NamespacedKey(plugin, "visual_part");
        this.hiddenKey = new NamespacedKey(plugin, "visual_hidden");
        this.originalInvisibleKey = new NamespacedKey(plugin, "original_invisible");
        this.originalAiKey = new NamespacedKey(plugin, "original_ai");
    }

    @Override
    public void run() {
        tick++;
        if (!enabled) return;
        if (tick % 20 == 1) discoverFlatCats();
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Rig> entry : rigs.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Cat) || entity.isDead()) {
                entry.getValue().remove();
                stale.add(entry.getKey());
                continue;
            }
            Cat cat = (Cat) entity;
            Rig rig = entry.getValue();
            if (plugin.states().isFlat(cat) && rig.mode != VisualMode.PAPER) {
                rig.remove();
                rig = createPaperRig(cat);
                entry.setValue(rig);
            }
            rig.update(cat, tick);
        }
        stale.forEach(rigs::remove);
    }

    void setFight(Cat cat, FightPhase phase, boolean mirrored) {
        if (!enabled) return;
        Rig old = rigs.remove(cat.getUniqueId());
        if (old != null) old.remove();
        hideOriginal(cat);
        rigs.put(cat.getUniqueId(), createFightRig(cat, phase, mirrored));
    }

    void refresh(Cat cat) {
        if (!enabled) return;
        Rig old = rigs.remove(cat.getUniqueId());
        if (old != null) old.remove();
        if (plugin.states().isFlat(cat)) {
            hideOriginal(cat);
            rigs.put(cat.getUniqueId(), createPaperRig(cat));
        }
    }

    void clear(Cat cat, boolean restoreOriginal) {
        Rig rig = rigs.remove(cat.getUniqueId());
        if (rig != null) rig.remove();
        if (restoreOriginal && !plugin.states().isFlat(cat)) restoreOriginal(cat);
    }

    void cleanupAll(boolean restoreCats) {
        for (Rig rig : rigs.values()) rig.remove();
        rigs.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (entity instanceof ArmorStand
                    && entity.getPersistentDataContainer().has(visualKey, PersistentDataType.BYTE)) {
                    entity.remove();
                } else if (restoreCats && entity instanceof Cat
                    && entity.getPersistentDataContainer().has(hiddenKey, PersistentDataType.BYTE)) {
                    Cat cat = (Cat) entity;
                    restoreOriginal(cat);
                }
            }
        }
    }

    void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (!enabled) {
            cleanupAll(true);
        } else {
            discoverFlatCats();
        }
    }

    private void discoverFlatCats() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Cat)) continue;
                Cat cat = (Cat) entity;
                if (plugin.states().isFlat(cat) && !rigs.containsKey(cat.getUniqueId())) refresh(cat);
            }
        }
    }

    private Rig createPaperRig(Cat cat) {
        int direction = plugin.states().projectionDirection(cat);
        int variant = variant(cat);
        ArmorStand paper = spawnPart(cat.getLocation(), ModelIds.paper(variant, direction));
        return new Rig(VisualMode.PAPER, false, new ArmorStand[] {paper});
    }

    private Rig createFightRig(Cat cat, FightPhase phase, boolean mirrored) {
        int variant = variant(cat);
        int[] ids = {
            ModelIds.HEAD, ModelIds.JAW,
            ModelIds.BODY_1, ModelIds.BODY_2, ModelIds.BODY_3, ModelIds.BODY_4,
            ModelIds.TAIL_1, ModelIds.TAIL_2,
            ModelIds.FRONT_LEFT, ModelIds.FRONT_RIGHT, ModelIds.HIND_LEFT, ModelIds.HIND_RIGHT
        };
        ArmorStand[] parts = new ArmorStand[ids.length];
        for (int index = 0; index < ids.length; index++) {
            parts[index] = spawnPart(cat.getLocation(), ModelIds.piece(variant, ids[index]));
        }
        return new Rig(phase == FightPhase.THREAT ? VisualMode.THREAT : VisualMode.TUSSLE, mirrored, parts);
    }

    private ArmorStand spawnPart(Location location, int customModelData) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setSilent(true);
        stand.setInvulnerable(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setCanPickupItems(false);
        stand.getPersistentDataContainer().set(visualKey, PersistentDataType.BYTE, (byte) 1);
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        stand.getEquipment().setHelmet(item);
        return stand;
    }

    private static int variant(Cat cat) {
        switch (cat.getCatType().name()) {
            case "TABBY": return 0;
            case "BLACK": return 1;
            case "RED": return 2;
            case "SIAMESE": return 3;
            case "BRITISH_SHORTHAIR": return 4;
            case "CALICO": return 5;
            case "PERSIAN": return 6;
            case "RAGDOLL": return 7;
            case "WHITE": return 8;
            case "JELLIE": return 9;
            case "ALL_BLACK": return 10;
            default: return 0;
        }
    }

    private void hideOriginal(Cat cat) {
        if (!cat.getPersistentDataContainer().has(hiddenKey, PersistentDataType.BYTE)) {
            cat.getPersistentDataContainer().set(originalInvisibleKey, PersistentDataType.BYTE,
                cat.isInvisible() ? (byte) 1 : (byte) 0);
            cat.getPersistentDataContainer().set(originalAiKey, PersistentDataType.BYTE,
                cat.hasAI() ? (byte) 1 : (byte) 0);
            cat.getPersistentDataContainer().set(hiddenKey, PersistentDataType.BYTE, (byte) 1);
        }
        cat.setInvisible(true);
    }

    private void restoreOriginal(Cat cat) {
        boolean invisible = cat.getPersistentDataContainer().getOrDefault(
            originalInvisibleKey, PersistentDataType.BYTE, (byte) 0) != 0;
        boolean ai = cat.getPersistentDataContainer().getOrDefault(
            originalAiKey, PersistentDataType.BYTE, (byte) 1) != 0;
        cat.setInvisible(invisible);
        cat.setAI(ai);
        cat.getPersistentDataContainer().remove(hiddenKey);
        cat.getPersistentDataContainer().remove(originalInvisibleKey);
        cat.getPersistentDataContainer().remove(originalAiKey);
    }

    private enum VisualMode { THREAT, TUSSLE, PAPER }

    private final class Rig {
        private final VisualMode mode;
        private final boolean mirrored;
        private final ArmorStand[] parts;

        private Rig(VisualMode mode, boolean mirrored, ArmorStand[] parts) {
            this.mode = mode;
            this.mirrored = mirrored;
            this.parts = parts;
        }

        private void update(Cat cat, long age) {
            if (mode == VisualMode.PAPER) {
                hideOriginal(cat);
                place(parts[0], cat.getLocation().clone().add(0, -0.67, 0), cat.getLocation().getYaw(), 0, 0, 0);
                return;
            }
            hideOriginal(cat);
            double chew = (Math.sin(age * 0.56) + 1.0) * 0.5;
            double headYaw = 0.24;
            double headRoll = 0.27;
            double tussleRoll = mode == VisualMode.TUSSLE ? Math.sin(age * 0.72) * 0.75 : 0.0;

            placeLocal(parts[0], cat, 0, 0.43, 0.47, -0.38, headYaw, headRoll + tussleRoll);
            placeLocal(parts[1], cat, 0, 0.37, 0.49, 0.04 + chew * 0.31, headYaw, headRoll + tussleRoll);

            double[] forward = {0.22, 0.02, -0.18, -0.38};
            double[] pitch = mode == VisualMode.THREAT
                ? new double[] {0.22, 0.07, -0.08, -0.23}
                : new double[] {0.0, 0.0, 0.0, 0.0};
            double[] lift = mode == VisualMode.THREAT
                ? new double[] {0.00, 0.055, 0.055, 0.00}
                : new double[] {0.0, 0.0, 0.0, 0.0};
            for (int i = 0; i < 4; i++) {
                placeLocal(parts[2 + i], cat, 0, 0.28 + lift[i], forward[i], pitch[i], 0, tussleRoll);
            }

            placeLocal(parts[6], cat, 0, 0.35, -0.57, 1.18, 0, tussleRoll);
            placeLocal(parts[7], cat, 0, 0.52, -0.72, 1.55 + Math.sin(age * 0.62) * 0.06, 0, tussleRoll);
            double scratch = age * 2.15;
            double frontPitch = mode == VisualMode.TUSSLE ? Math.sin(scratch) * 1.15 : -0.12;
            double oppositeFront = mode == VisualMode.TUSSLE ? Math.sin(scratch + Math.PI) * 1.15 : -0.12;
            double hindPitch = mode == VisualMode.TUSSLE ? Math.sin(scratch + 1.15) * 0.95 : 0.31;
            double oppositeHind = mode == VisualMode.TUSSLE ? Math.sin(scratch + 4.29) * 0.95 : 0.31;
            placeLocal(parts[8], cat, 0.15, 0.11, 0.26, frontPitch, 0, tussleRoll);
            placeLocal(parts[9], cat, -0.15, 0.11, 0.26, oppositeFront, 0, tussleRoll);
            placeLocal(parts[10], cat, 0.14, 0.11, -0.36, hindPitch, 0, tussleRoll);
            placeLocal(parts[11], cat, -0.14, 0.11, -0.36, oppositeHind, 0, tussleRoll);
        }

        private void remove() {
            for (ArmorStand part : parts) if (part != null && !part.isDead()) part.remove();
        }
    }

    private static void placeLocal(ArmorStand stand, Cat cat, double right, double up, double forward,
                                   double pitch, double yaw, double roll) {
        Location base = cat.getLocation();
        double radians = Math.toRadians(base.getYaw());
        Vector forwardVector = new Vector(-Math.sin(radians), 0, Math.cos(radians));
        Vector rightVector = new Vector(Math.cos(radians), 0, Math.sin(radians));
        Location target = base.clone().add(forwardVector.multiply(forward)).add(rightVector.multiply(right)).add(0, up - 0.72, 0);
        place(stand, target, base.getYaw(), pitch, yaw, roll);
    }

    private static void place(ArmorStand stand, Location target, float bodyYaw, double pitch, double yaw, double roll) {
        target.setYaw(bodyYaw);
        target.setPitch(0.0F);
        stand.teleport(target);
        stand.setHeadPose(new EulerAngle(pitch, yaw, roll));
    }
}
