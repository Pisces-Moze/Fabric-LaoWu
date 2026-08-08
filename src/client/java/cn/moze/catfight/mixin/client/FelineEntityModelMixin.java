package cn.moze.catfight.mixin.client;

import cn.moze.catfight.CatFightManager;
import cn.moze.catfight.client.FightRenderState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.FelineEntityModel;
import net.minecraft.client.render.entity.state.FelineEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FelineEntityModel.class)
public abstract class FelineEntityModelMixin {
    @Shadow protected ModelPart leftHindLeg;
    @Shadow protected ModelPart rightHindLeg;
    @Shadow protected ModelPart leftFrontLeg;
    @Shadow protected ModelPart rightFrontLeg;
    @Shadow protected ModelPart upperTail;
    @Shadow protected ModelPart lowerTail;
    @Shadow protected ModelPart head;
    @Shadow protected ModelPart body;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void catfight$animate(FelineEntityRenderState state, CallbackInfo ci) {
        // hidden is not part of ModelTransform, so reset it explicitly every frame.
        this.body.hidden = false;
        this.head.hidden = false;
        if (!(state instanceof FightRenderState fightState)) {
            return;
        }
        int phase = fightState.catfight$getFightState();
        if (CatFightManager.isThreat(phase)) {
            float twitch = MathHelper.sin(state.age * 1.91F) * 0.035F;
            this.body.hidden = true;
            this.head.hidden = true;
            this.head.originY -= 0.35F;
            this.head.originZ += 0.25F;
            this.head.pitch = -0.38F + twitch;
            // Both opponents use the same local roll; their opposite body yaws turn
            // it into a mirrored left/right lean in world space.
            this.head.roll = 0.27F;
            this.upperTail.pitch = 1.18F;
            // CAT_TRANSFORMER makes the eight-pixel upper tail 6.4 pixels long.
            // Start the lower tail just inside that endpoint so rounding and motion
            // can never reveal a bright line between the two independent root parts.
            float upperLength = 6.22F;
            this.lowerTail.originX = this.upperTail.originX;
            this.lowerTail.originY = this.upperTail.originY + MathHelper.cos(this.upperTail.pitch) * upperLength;
            this.lowerTail.originZ = this.upperTail.originZ + MathHelper.sin(this.upperTail.pitch) * upperLength;
            this.lowerTail.pitch = 1.55F + MathHelper.sin(state.age * 0.62F) * 0.06F;
            this.leftHindLeg.originY -= 0.25F;
            this.rightHindLeg.originY -= 0.25F;
            this.leftHindLeg.pitch = 0.31F;
            this.rightHindLeg.pitch = 0.31F;
            this.leftFrontLeg.pitch = -0.12F;
            this.rightFrontLeg.pitch = -0.12F;
        } else if (phase == CatFightManager.TUSSLE) {
            this.head.hidden = true;
            float scratch = state.age * 2.15F;
            float roll = MathHelper.sin(state.age * 0.72F) * 0.42F;
            this.body.roll += roll;
            this.head.roll -= roll * 0.7F;
            this.head.pitch += MathHelper.sin(state.age * 1.35F) * 0.20F;
            this.leftFrontLeg.pitch = MathHelper.sin(scratch) * 1.15F;
            this.rightFrontLeg.pitch = MathHelper.sin(scratch + (float) Math.PI) * 1.15F;
            this.leftHindLeg.pitch = MathHelper.sin(scratch + 1.15F) * 0.95F;
            this.rightHindLeg.pitch = MathHelper.sin(scratch + 4.29F) * 0.95F;
            this.upperTail.pitch = 2.4F + MathHelper.sin(state.age * 0.9F) * 0.6F;
            this.lowerTail.pitch = 2.5F + MathHelper.sin(state.age * 1.2F) * 0.8F;
        }
    }
}
