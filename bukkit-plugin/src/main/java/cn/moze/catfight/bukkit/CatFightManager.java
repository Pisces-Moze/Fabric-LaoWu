package cn.moze.catfight.bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

final class CatFightManager implements Runnable {
    private final CatFightPlugin plugin;
    private final List<FightPair> pairs = new ArrayList<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private long tick;

    CatFightManager(CatFightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tick++;
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= tick);
        Iterator<FightPair> iterator = pairs.iterator();
        while (iterator.hasNext()) {
            FightPair pair = iterator.next();
            if (tickPair(pair)) iterator.remove();
        }
        if (tick % 10 == 0) {
            for (World world : Bukkit.getWorlds()) makePairs(world);
        }
        if (tick % 20 == 0 && plugin.getConfig().getBoolean("paper-cat.seek-rails", true)) {
            seekRails();
        }
    }

    void cancel(Cat cat) {
        Iterator<FightPair> iterator = pairs.iterator();
        while (iterator.hasNext()) {
            FightPair pair = iterator.next();
            if (pair.first.equals(cat.getUniqueId()) || pair.second.equals(cat.getUniqueId())) {
                finish(pair);
                iterator.remove();
            }
        }
    }

    void shutdown() {
        for (FightPair pair : new ArrayList<>(pairs)) finish(pair);
        pairs.clear();
    }

    private void makePairs(World world) {
        Set<UUID> busy = new HashSet<>();
        for (FightPair pair : pairs) {
            busy.add(pair.first);
            busy.add(pair.second);
        }
        List<Cat> ready = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Cat)) continue;
            Cat cat = (Cat) entity;
            if (!cat.isDead() && cat.isAdult() && !cat.isSitting()
                && !plugin.states().isFlat(cat) && !busy.contains(cat.getUniqueId())
                && !cooldowns.containsKey(cat.getUniqueId())) {
                ready.add(cat);
            }
        }
        if (ready.size() < 2 || (ready.size() & 1) != 0) return;

        double rangeSquared = Math.pow(plugin.getConfig().getDouble("pairing.range", 16.0), 2.0);
        while (ready.size() >= 2) {
            Candidate closest = closest(ready);
            if (closest == null || closest.distanceSquared > rangeSquared) return;
            Cat second = ready.remove(closest.secondIndex);
            Cat first = ready.remove(closest.firstIndex);
            start(first, second);
        }
    }

    private Candidate closest(List<Cat> cats) {
        List<Candidate> candidates = new ArrayList<>();
        for (int first = 0; first < cats.size(); first++) {
            for (int second = first + 1; second < cats.size(); second++) {
                candidates.add(new Candidate(first, second, cats.get(first).getLocation().distanceSquared(cats.get(second).getLocation())));
            }
        }
        return candidates.stream().min(Comparator.comparingDouble(candidate -> candidate.distanceSquared)).orElse(null);
    }

    private void start(Cat first, Cat second) {
        FightPair pair = new FightPair(first.getUniqueId(), second.getUniqueId(), FightPhase.THREAT, tick);
        snap(pair, first, second);
        pairs.add(pair);
        plugin.visuals().setFight(first, FightPhase.THREAT, false);
        plugin.visuals().setFight(second, FightPhase.THREAT, true);
        first.setAI(false);
        second.setAI(false);
        playSound(first.getLocation().clone().add(second.getLocation()).multiply(0.5), "catfight:cat_hiss", 1.0F);
    }

    private void snap(FightPair pair, Cat first, Cat second) {
        Vector horizontal = second.getLocation().toVector().subtract(first.getLocation().toVector()).setY(0);
        if (horizontal.lengthSquared() < 0.0001) horizontal = new Vector(1, 0, 0);
        horizontal.normalize();
        Location middle = first.getLocation().clone().add(second.getLocation()).multiply(0.5);
        double half = plugin.getConfig().getDouble("pairing.separation", 1.8) * 0.5;
        pair.firstAnchor = middle.clone().subtract(horizontal.clone().multiply(half));
        pair.secondAnchor = middle.clone().add(horizontal.clone().multiply(half));
        faceAndTeleport(first, pair.firstAnchor, pair.secondAnchor);
        faceAndTeleport(second, pair.secondAnchor, pair.firstAnchor);
        if (plugin.getConfig().getBoolean("visuals.particles", true)) {
            middle.getWorld().spawnParticle(Particle.CLOUD, middle.clone().add(0, 0.3, 0), 8, 0.45, 0.22, 0.45, 0.025);
        }
    }

    private boolean tickPair(FightPair pair) {
        Cat first = cat(pair.first);
        Cat second = cat(pair.second);
        if (!valid(first, second)) {
            finish(pair);
            return true;
        }
        if (pair.phase == FightPhase.THREAT) {
            faceAndTeleport(first, pair.firstAnchor, pair.secondAnchor);
            faceAndTeleport(second, pair.secondAnchor, pair.firstAnchor);
            long duration = Math.round(plugin.getConfig().getDouble("pairing.threat-seconds", 8.0) * 20.0);
            if (tick - pair.phaseStarted >= duration) enterTussle(pair, first, second);
            return false;
        }

        animateTussle(pair, first, second);
        long duration = Math.round(plugin.getConfig().getDouble("pairing.tussle-seconds", 5.0) * 20.0);
        if (tick - pair.phaseStarted >= duration) {
            finish(pair);
            return true;
        }
        return false;
    }

    private void enterTussle(FightPair pair, Cat first, Cat second) {
        pair.phase = FightPhase.TUSSLE;
        pair.phaseStarted = tick;
        plugin.visuals().setFight(first, FightPhase.TUSSLE, false);
        plugin.visuals().setFight(second, FightPhase.TUSSLE, true);
        playSound(first.getLocation().clone().add(second.getLocation()).multiply(0.5), "catfight:cat_tussle", 1.0F);
    }

    private void animateTussle(FightPair pair, Cat first, Cat second) {
        Location middle = pair.firstAnchor.clone().add(pair.secondAnchor).multiply(0.5);
        double time = (tick - pair.phaseStarted) * 0.34;
        Vector axis = pair.secondAnchor.toVector().subtract(pair.firstAnchor.toVector()).setY(0).normalize();
        Vector tangent = new Vector(-axis.getZ(), 0, axis.getX());
        Vector orbit = axis.clone().multiply(Math.cos(time) * 0.34).add(tangent.multiply(Math.sin(time) * 0.34));
        Location firstPosition = middle.clone().add(orbit).add(0, Math.abs(Math.sin(time * 1.4)) * 0.13, 0);
        Location secondPosition = middle.clone().subtract(orbit).add(0, Math.abs(Math.cos(time * 1.4)) * 0.13, 0);
        faceAndTeleport(first, firstPosition, secondPosition);
        faceAndTeleport(second, secondPosition, firstPosition);
        if (tick % 8 == 0 && plugin.getConfig().getBoolean("visuals.particles", true)) {
            middle.getWorld().spawnParticle(Particle.CLOUD, middle.clone().add(0, 0.35, 0), 3, 0.3, 0.18, 0.3, 0.02);
        }
    }

    private void finish(FightPair pair) {
        Cat first = cat(pair.first);
        Cat second = cat(pair.second);
        long cooldown = Math.round(plugin.getConfig().getDouble("pairing.cooldown-seconds", 12.0) * 20.0);
        if (first != null) restore(first, cooldown);
        if (second != null) restore(second, cooldown);
    }

    private void restore(Cat cat, long cooldown) {
        cat.setAI(true);
        plugin.visuals().clear(cat, true);
        cooldowns.put(cat.getUniqueId(), tick + cooldown);
    }

    private boolean valid(Cat first, Cat second) {
        return first != null && second != null && !first.isDead() && !second.isDead()
            && first.getWorld().equals(second.getWorld()) && !first.isSitting() && !second.isSitting()
            && !plugin.states().isFlat(first) && !plugin.states().isFlat(second);
    }

    private Cat cat(UUID id) {
        Entity entity = Bukkit.getEntity(id);
        return entity instanceof Cat ? (Cat) entity : null;
    }

    private static void faceAndTeleport(Cat cat, Location position, Location target) {
        Location result = position.clone();
        Vector direction = target.toVector().subtract(position.toVector()).setY(0);
        if (direction.lengthSquared() > 0.0001) result.setDirection(direction);
        result.setPitch(0.0F);
        cat.teleport(result);
        cat.setVelocity(new Vector());
    }

    private static void playSound(Location location, String sound, float volume) {
        if (location.getWorld() != null) location.getWorld().playSound(location, sound, volume, 0.95F + (float) Math.random() * 0.1F);
    }

    private void seekRails() {
        int range = plugin.getConfig().getInt("paper-cat.rail-range", 10);
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Cat)) continue;
                Cat cat = (Cat) entity;
                if (cat.isDead() || cat.isSitting() || plugin.states().isFlat(cat) || isBusy(cat.getUniqueId())) continue;
                Location rail = RailFinder.nearestRail(cat.getLocation(), range, 3);
                if (rail != null) {
                    Vector velocity = rail.toVector().add(new Vector(0.5, 0, 0.5)).subtract(cat.getLocation().toVector()).setY(0);
                    if (velocity.lengthSquared() > 0.1) cat.setVelocity(velocity.normalize().multiply(0.16));
                }
            }
        }
    }

    private boolean isBusy(UUID id) {
        for (FightPair pair : pairs) if (pair.first.equals(id) || pair.second.equals(id)) return true;
        return false;
    }

    private static final class Candidate {
        final int firstIndex;
        final int secondIndex;
        final double distanceSquared;

        Candidate(int firstIndex, int secondIndex, double distanceSquared) {
            this.firstIndex = firstIndex;
            this.secondIndex = secondIndex;
            this.distanceSquared = distanceSquared;
        }
    }
}
