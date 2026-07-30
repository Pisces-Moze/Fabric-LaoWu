package cn.moze.catfight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

public final class CatFightManager {
    public static final int IDLE = 0;
    public static final int THREAT = 1;
    public static final int TUSSLE = 2;
    public static final int THREAT_MIRRORED = 3;

    private static final int CHECK_INTERVAL = 10;
    private static final int THREAT_TICKS = 160;
    private static final int TUSSLE_TICKS = 100;
    private static final int APPROACH_TIMEOUT = 240;
    private static final int COOLDOWN_TICKS = 240;
    private static final double ACTIVATION_RANGE_SQUARED = 16.0 * 16.0;
    private static final double THREAT_DISTANCE_SQUARED = 2.35 * 2.35;
    private static final Map<ServerWorld, WorldState> STATES = new WeakHashMap<>();

    private CatFightManager() { }

    public static void tick(ServerWorld world) {
        WorldState state = STATES.computeIfAbsent(world, ignored -> new WorldState());
        long now = world.getTime();
        state.cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
        state.pairs.removeIf(pair -> tickPair(world, state, pair, now));

        if (now % CHECK_INTERVAL == 0) {
            makePairs(world, state, now);
        }
    }

    private static void makePairs(ServerWorld world, WorldState state, long now) {
        Set<UUID> busy = new HashSet<>();
        for (Pair pair : state.pairs) {
            busy.add(pair.first.getUuid());
            busy.add(pair.second.getUuid());
        }

        List<CatEntity> ready = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof CatEntity cat
                && cat.isAlive()
                && !cat.isBaby()
                && !cat.isSitting()
                && !busy.contains(cat.getUuid())
                && !state.cooldowns.containsKey(cat.getUuid())) {
                ready.add(cat);
            }
        }

        // The loaded, eligible cat population must be even. A lone odd cat never gets forced into a match.
        if (ready.size() < 2 || (ready.size() & 1) != 0) {
            return;
        }

        while (ready.size() >= 2) {
            PairCandidate closest = findClosestPair(ready);
            if (closest == null || closest.distanceSquared > ACTIVATION_RANGE_SQUARED) {
                break;
            }
            CatEntity first = ready.remove(closest.secondIndex);
            CatEntity second = ready.remove(closest.firstIndex);
            Pair pair = new Pair(second, first, Phase.APPROACH, now);
            state.pairs.add(pair);
            snapTogether(world, pair);
            enterThreat(world, pair, now);
        }
    }

    private static void snapTogether(ServerWorld world, Pair pair) {
        CatEntity a = pair.first;
        CatEntity b = pair.second;
        Vec3d delta = b.getEntityPos().subtract(a.getEntityPos());
        Vec3d horizontal = new Vec3d(delta.x, 0.0, delta.z);
        if (horizontal.lengthSquared() < 0.0001) {
            horizontal = new Vec3d(1.0, 0.0, 0.0);
        } else {
            horizontal = horizontal.normalize();
        }

        Vec3d midpoint = a.getEntityPos().add(b.getEntityPos()).multiply(0.5);
        Vec3d offset = horizontal.multiply(0.9);
        a.teleport(midpoint.x - offset.x, a.getY(), midpoint.z - offset.z, false);
        b.teleport(midpoint.x + offset.x, b.getY(), midpoint.z + offset.z, false);
        a.setVelocity(Vec3d.ZERO);
        b.setVelocity(Vec3d.ZERO);
        a.lookAtEntity(b, 90.0F, 90.0F);
        b.lookAtEntity(a, 90.0F, 90.0F);
        faceEachOther(a, b);
        world.spawnParticles(ParticleTypes.POOF, midpoint.x, midpoint.y + 0.3, midpoint.z, 8, 0.45, 0.22, 0.45, 0.025);
    }

    private static PairCandidate findClosestPair(List<CatEntity> cats) {
        List<PairCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < cats.size(); i++) {
            for (int j = i + 1; j < cats.size(); j++) {
                candidates.add(new PairCandidate(i, j, cats.get(i).squaredDistanceTo(cats.get(j))));
            }
        }
        return candidates.stream().min(Comparator.comparingDouble(candidate -> candidate.distanceSquared)).orElse(null);
    }

    private static boolean tickPair(ServerWorld world, WorldState state, Pair pair, long now) {
        CatEntity a = pair.first;
        CatEntity b = pair.second;
        if (!a.isAlive() || !b.isAlive() || a.getEntityWorld() != world || b.getEntityWorld() != world || a.isSitting() || b.isSitting()) {
            finishPair(state, pair, now);
            return true;
        }

        a.lookAtEntity(b, 90.0F, 90.0F);
        b.lookAtEntity(a, 90.0F, 90.0F);
        if (pair.phase == Phase.THREAT) {
            faceEachOther(a, b);
        }

        if (pair.phase == Phase.APPROACH) {
            if (a.squaredDistanceTo(b) <= THREAT_DISTANCE_SQUARED) {
                enterThreat(world, pair, now);
            } else if (now - pair.phaseStarted > APPROACH_TIMEOUT) {
                finishPair(state, pair, now);
                return true;
            } else {
                a.getNavigation().startMovingTo(b, 1.05);
                b.getNavigation().startMovingTo(a, 1.05);
            }
            return false;
        }

        a.getNavigation().stop();
        b.getNavigation().stop();
        a.setSprinting(false);
        b.setSprinting(false);

        if (pair.phase == Phase.THREAT) {
            holdStill(a);
            holdStill(b);
            a.setPose(EntityPose.CROUCHING);
            b.setPose(EntityPose.CROUCHING);
            long elapsed = now - pair.phaseStarted;
            if (elapsed == 32) {
                playPairSound(world, pair, CatFightMod.CAT_HISS, 0.9F);
            }
            if (elapsed >= THREAT_TICKS) {
                enterTussle(world, pair, now);
            }
            return false;
        }

        tussle(world, a, b, now, 1.0);
        tussle(world, b, a, now, -1.0);
        if ((now - pair.phaseStarted) % 8 == 0) {
            Vec3d middle = a.getEntityPos().add(b.getEntityPos()).multiply(0.5);
            world.spawnParticles(ParticleTypes.POOF, middle.x, middle.y + 0.35, middle.z, 3, 0.35, 0.2, 0.35, 0.025);
        }
        if (now - pair.phaseStarted >= TUSSLE_TICKS) {
            finishPair(state, pair, now);
            return true;
        }
        return false;
    }

    private static void enterThreat(ServerWorld world, Pair pair, long now) {
        pair.phase = Phase.THREAT;
        pair.phaseStarted = now;
        setState(pair.first, THREAT);
        setState(pair.second, THREAT_MIRRORED);
        pair.first.getNavigation().stop();
        pair.second.getNavigation().stop();
        playPairSound(world, pair, CatFightMod.CAT_HISS, 1.0F);
    }

    private static void enterTussle(ServerWorld world, Pair pair, long now) {
        pair.phase = Phase.TUSSLE;
        pair.phaseStarted = now;
        setState(pair.first, TUSSLE);
        setState(pair.second, TUSSLE);
        pair.first.setPose(EntityPose.STANDING);
        pair.second.setPose(EntityPose.STANDING);
        playPairSound(world, pair, CatFightMod.CAT_TUSSLE, 1.0F);
    }

    private static void tussle(ServerWorld world, CatEntity cat, CatEntity partner, long now, double side) {
        Vec3d toward = partner.getEntityPos().subtract(cat.getEntityPos());
        Vec3d horizontal = new Vec3d(toward.x, 0.0, toward.z);
        if (horizontal.lengthSquared() < 0.01) {
            horizontal = new Vec3d(1.0, 0.0, 0.0);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3d tangent = new Vec3d(-horizontal.z * side, 0.0, horizontal.x * side);
        double pulse = Math.sin(now * 0.55) * 0.07;
        double y = cat.isOnGround() && now % 12 == 0 ? 0.18 : cat.getVelocity().y;
        cat.setVelocity(horizontal.multiply(0.105 + pulse).add(tangent.multiply(0.13)).add(0.0, y, 0.0));
        cat.setPose(EntityPose.STANDING);
    }

    private static void holdStill(CatEntity cat) {
        Vec3d velocity = cat.getVelocity();
        cat.setVelocity(0.0, velocity.y, 0.0);
    }

    private static void faceEachOther(CatEntity a, CatEntity b) {
        float yawA = yawTo(a, b);
        float yawB = yawTo(b, a);
        lockYaw(a, yawA);
        lockYaw(b, yawB);
    }

    private static float yawTo(CatEntity from, CatEntity to) {
        Vec3d delta = to.getEntityPos().subtract(from.getEntityPos());
        return (float) (Math.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F;
    }

    private static void lockYaw(CatEntity cat, float yaw) {
        cat.setYaw(yaw);
        cat.setBodyYaw(yaw);
        cat.setHeadYaw(yaw);
    }

    private static void playPairSound(ServerWorld world, Pair pair, net.minecraft.sound.SoundEvent sound, float volume) {
        Vec3d middle = pair.first.getEntityPos().add(pair.second.getEntityPos()).multiply(0.5);
        world.playSound(null, middle.x, middle.y, middle.z, sound, SoundCategory.NEUTRAL, volume, 0.94F + world.getRandom().nextFloat() * 0.12F);
    }

    private static void finishPair(WorldState state, Pair pair, long now) {
        setState(pair.first, IDLE);
        setState(pair.second, IDLE);
        pair.first.setPose(EntityPose.STANDING);
        pair.second.setPose(EntityPose.STANDING);
        state.cooldowns.put(pair.first.getUuid(), now + COOLDOWN_TICKS);
        state.cooldowns.put(pair.second.getUuid(), now + COOLDOWN_TICKS);
    }

    private static void setState(CatEntity cat, int value) {
        ((CatFightAccess) cat).catfight$setState(value);
    }

    public static boolean isThreat(int state) {
        return state == THREAT || state == THREAT_MIRRORED;
    }

    private enum Phase { APPROACH, THREAT, TUSSLE }

    private static final class Pair {
        private final CatEntity first;
        private final CatEntity second;
        private Phase phase;
        private long phaseStarted;

        private Pair(CatEntity first, CatEntity second, Phase phase, long phaseStarted) {
            this.first = first;
            this.second = second;
            this.phase = phase;
            this.phaseStarted = phaseStarted;
        }
    }

    private record PairCandidate(int firstIndex, int secondIndex, double distanceSquared) { }

    private static final class WorldState {
        private final List<Pair> pairs = new ArrayList<>();
        private final Map<UUID, Long> cooldowns = new HashMap<>();
    }
}
