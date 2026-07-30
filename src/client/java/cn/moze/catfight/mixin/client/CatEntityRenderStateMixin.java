package cn.moze.catfight.mixin.client;

import cn.moze.catfight.client.FightRenderState;
import net.minecraft.client.render.entity.state.CatEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CatEntityRenderState.class)
public final class CatEntityRenderStateMixin implements FightRenderState {
    @Unique private int catfight$fightState;
    @Unique private boolean catfight$flat;
    @Unique private int catfight$projectionYaw;
    @Unique private int catfight$projectionPitch = 90;

    @Override
    public int catfight$getFightState() {
        return this.catfight$fightState;
    }

    @Override
    public void catfight$setFightState(int state) {
        this.catfight$fightState = state;
    }

    @Override public boolean catfight$isFlat() { return this.catfight$flat; }
    @Override public void catfight$setFlat(boolean flat) { this.catfight$flat = flat; }
    @Override public int catfight$getProjectionYaw() { return this.catfight$projectionYaw; }
    @Override public void catfight$setProjectionYaw(int yaw) { this.catfight$projectionYaw = yaw; }
    @Override public int catfight$getProjectionPitch() { return this.catfight$projectionPitch; }
    @Override public void catfight$setProjectionPitch(int pitch) { this.catfight$projectionPitch = pitch; }
}
