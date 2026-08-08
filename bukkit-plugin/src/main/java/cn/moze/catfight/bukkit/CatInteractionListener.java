package cn.moze.catfight.bukkit;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.inventory.EquipmentSlot;

final class CatInteractionListener implements Listener {
    private final CatFightPlugin plugin;

    CatInteractionListener(CatFightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Cat)) return;
        Cat cat = (Cat) event.getRightClicked();
        Player player = event.getPlayer();
        CatStateService states = plugin.states();

        if (!states.isFlat(cat) && isShovel(player.getInventory().getItemInMainHand().getType())) {
            plugin.fights().cancel(cat);
            states.flatten(cat, player.getLocation().getYaw(), player.getLocation().getPitch());
            plugin.visuals().refresh(cat);
            player.swingMainHand();
            event.setCancelled(true);
            return;
        }
        if (states.isFlat(cat)) {
            boolean restored = states.restoreClick(cat, plugin.getConfig().getInt("paper-cat.restore-clicks", 3));
            if (restored) {
                plugin.visuals().clear(cat, true);
                cat.getWorld().spawnParticle(Particle.CLOUD, cat.getLocation().add(0, 0.25, 0), 8, 0.2, 0.08, 0.2, 0.01);
            }
            player.swingMainHand();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMinecartCollision(VehicleEntityCollisionEvent event) {
        Entity hit = event.getEntity();
        if (!(event.getVehicle() instanceof Minecart) || !(hit instanceof Cat)) return;
        Cat cat = (Cat) hit;
        if (plugin.states().isFlat(cat)) return;
        plugin.fights().cancel(cat);
        float randomYaw = (float) (Math.random() * 360.0 - 180.0);
        float randomPitch = (float) (Math.random() * 180.0 - 90.0);
        plugin.states().flatten(cat, cat.getLocation().getYaw() + randomYaw, randomPitch);
        plugin.visuals().refresh(cat);
    }

    private static boolean isShovel(Material material) {
        return material.name().endsWith("_SHOVEL");
    }
}
