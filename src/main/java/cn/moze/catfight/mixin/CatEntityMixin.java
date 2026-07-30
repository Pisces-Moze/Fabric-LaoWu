package cn.moze.catfight.mixin;

import cn.moze.catfight.CatFightAccess;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CatEntity.class)
public abstract class CatEntityMixin implements CatFightAccess {
    @Unique
    private static final TrackedData<Integer> CATFIGHT_STATE =
        DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Boolean> CATFIGHT_FLAT =
        DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private static final TrackedData<Integer> CATFIGHT_FLAT_POSE =
        DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> CATFIGHT_RESTORE_CLICKS =
        DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> CATFIGHT_PROJECTION_YAW =
        DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique private static final TrackedData<Integer> CATFIGHT_PROJECTION_PITCH =
        DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.INTEGER);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void catfight$initDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(CATFIGHT_STATE, 0);
        builder.add(CATFIGHT_FLAT, false);
        builder.add(CATFIGHT_FLAT_POSE, 0);
        builder.add(CATFIGHT_RESTORE_CLICKS, 0);
        builder.add(CATFIGHT_PROJECTION_YAW, 0);
        builder.add(CATFIGHT_PROJECTION_PITCH, 90);
    }

    @Override
    public int catfight$getState() {
        return ((CatEntity) (Object) this).getDataTracker().get(CATFIGHT_STATE);
    }

    @Override
    public void catfight$setState(int state) {
        ((CatEntity) (Object) this).getDataTracker().set(CATFIGHT_STATE, state);
    }

    @Override public boolean catfight$isFlat() { return ((CatEntity) (Object) this).getDataTracker().get(CATFIGHT_FLAT); }
    @Override public void catfight$setFlat(boolean flat) { ((CatEntity) (Object) this).getDataTracker().set(CATFIGHT_FLAT, flat); }
    @Override public int catfight$getFlatPose() { return ((CatEntity) (Object) this).getDataTracker().get(CATFIGHT_FLAT_POSE); }
    @Override public void catfight$setFlatPose(int pose) { ((CatEntity) (Object) this).getDataTracker().set(CATFIGHT_FLAT_POSE, pose); }
    @Override public int catfight$getRestoreClicks() { return ((CatEntity) (Object) this).getDataTracker().get(CATFIGHT_RESTORE_CLICKS); }
    @Override public void catfight$setRestoreClicks(int clicks) { ((CatEntity) (Object) this).getDataTracker().set(CATFIGHT_RESTORE_CLICKS, clicks); }
    @Override public int catfight$getProjectionYaw() { return ((CatEntity) (Object) this).getDataTracker().get(CATFIGHT_PROJECTION_YAW); }
    @Override public void catfight$setProjectionYaw(int yaw) { ((CatEntity) (Object) this).getDataTracker().set(CATFIGHT_PROJECTION_YAW, yaw); }
    @Override public int catfight$getProjectionPitch() { return ((CatEntity) (Object) this).getDataTracker().get(CATFIGHT_PROJECTION_PITCH); }
    @Override public void catfight$setProjectionPitch(int pitch) { ((CatEntity) (Object) this).getDataTracker().set(CATFIGHT_PROJECTION_PITCH, pitch); }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void catfight$flattenOrRestore(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        CatEntity cat = (CatEntity) (Object) this;
        boolean shovel = player.getStackInHand(hand).isIn(ItemTags.SHOVELS);
        if (!this.catfight$isFlat() && shovel) {
            if (!cat.getEntityWorld().isClient()) {
                // Store the captured view relative to the cat, so the paper image does
                // not change later when the still-mobile cat turns while pathfinding.
                int relativeYaw = Math.round(player.getYaw() - cat.getBodyYaw());
                this.catfight$flatten(relativeYaw, Math.round(player.getPitch()));
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        } else if (this.catfight$isFlat()) {
            if (!cat.getEntityWorld().isClient()) {
                int clicks = this.catfight$getRestoreClicks() + 1;
                if (clicks >= 3) {
                    this.catfight$setFlat(false);
                    this.catfight$setRestoreClicks(0);
                } else {
                    this.catfight$setRestoreClicks(clicks);
                }
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void catfight$seekRailsAndCheckMinecarts(CallbackInfo ci) {
        CatEntity cat = (CatEntity) (Object) this;
        if (!(cat.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        Box hitBox = cat.getBoundingBox().expand(0.18);
        if (!world.getEntitiesByClass(AbstractMinecartEntity.class, hitBox, Entity -> Entity.isAlive()).isEmpty()) {
            if (!this.catfight$isFlat()) {
                int randomYaw = cat.getRandom().nextInt(360);
                int randomPitch = 15 + cat.getRandom().nextInt(66);
                this.catfight$flatten(randomYaw, randomPitch);
            }
        }

        if (cat.age % 20 == 0 && this.catfight$getState() == 0 && !cat.isSitting()) {
            BlockPos rail = catfight$findNearestRail(world, cat.getBlockPos(), 10, 3);
            if (rail != null) {
                cat.getNavigation().startMovingTo(rail.getX() + 0.5, rail.getY(), rail.getZ() + 0.5, 1.0);
            }
        }
    }

    @Unique
    private static BlockPos catfight$findNearestRail(ServerWorld world, BlockPos center, int horizontalRadius, int verticalRadius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (AbstractRailBlock.isRail(world, mutable)) {
                        double distance = x * x + y * y + z * z;
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = mutable.toImmutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    @Unique
    private void catfight$flatten(int projectionYaw, int projectionPitch) {
        this.catfight$setFlatPose(this.catfight$getState());
        this.catfight$setProjectionYaw(projectionYaw);
        this.catfight$setProjectionPitch(Math.max(-90, Math.min(90, projectionPitch)));
        this.catfight$setFlat(true);
        this.catfight$setRestoreClicks(0);
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void catfight$writeFlatData(WriteView view, CallbackInfo ci) {
        view.putBoolean("CatFightFlat", this.catfight$isFlat());
        view.putInt("CatFightFlatPose", this.catfight$getFlatPose());
        view.putInt("CatFightRestoreClicks", this.catfight$getRestoreClicks());
        view.putInt("CatFightProjectionYaw", this.catfight$getProjectionYaw());
        view.putInt("CatFightProjectionPitch", this.catfight$getProjectionPitch());
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void catfight$readFlatData(ReadView view, CallbackInfo ci) {
        this.catfight$setFlat(view.getBoolean("CatFightFlat", false));
        this.catfight$setFlatPose(view.getInt("CatFightFlatPose", 0));
        this.catfight$setRestoreClicks(view.getInt("CatFightRestoreClicks", 0));
        this.catfight$setProjectionYaw(view.getInt("CatFightProjectionYaw", 0));
        this.catfight$setProjectionPitch(view.getInt("CatFightProjectionPitch", 90));
    }
}
