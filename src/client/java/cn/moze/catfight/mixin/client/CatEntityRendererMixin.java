package cn.moze.catfight.mixin.client;

import cn.moze.catfight.CatFightAccess;
import cn.moze.catfight.CatFightManager;
import cn.moze.catfight.client.CatJawFeatureRenderer;
import cn.moze.catfight.client.CatSegmentedBodyFeatureRenderer;
import cn.moze.catfight.client.FightRenderState;
import net.minecraft.client.render.entity.CatEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.entity.state.CatEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CatEntityRenderer.class)
public abstract class CatEntityRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void catfight$addJaw(EntityRendererFactory.Context context, CallbackInfo ci) {
        FeatureRendererContext<CatEntityRenderState, CatEntityModel> featureContext =
            (FeatureRendererContext<CatEntityRenderState, CatEntityModel>) (Object) this;
        ((LivingEntityRendererInvoker<CatEntityRenderState, CatEntityModel>) this)
            .catfight$addFeature(new CatSegmentedBodyFeatureRenderer(featureContext));
        ((LivingEntityRendererInvoker<CatEntityRenderState, CatEntityModel>) this)
            .catfight$addFeature(new CatJawFeatureRenderer(featureContext));
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void catfight$copyState(CatEntity cat, CatEntityRenderState state, float tickProgress, CallbackInfo ci) {
        CatFightAccess access = (CatFightAccess) cat;
        FightRenderState renderState = (FightRenderState) state;
        renderState.catfight$setFlat(access.catfight$isFlat());
        renderState.catfight$setProjectionYaw(access.catfight$getProjectionYaw());
        renderState.catfight$setProjectionPitch(access.catfight$getProjectionPitch());
        renderState.catfight$setFightState(access.catfight$isFlat() ? access.catfight$getFlatPose() : access.catfight$getState());
    }

    @Inject(method = "setupTransforms", at = @At("TAIL"))
    private void catfight$poseWholeCat(CatEntityRenderState state, MatrixStack matrices, float bodyYaw, float baseScale, CallbackInfo ci) {
        int fightState = ((FightRenderState) state).catfight$getFightState();
        boolean flat = ((FightRenderState) state).catfight$isFlat();
        if (fightState == CatFightManager.TUSSLE && !flat) {
            matrices.translate(0.0F, 0.12F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(state.age * 0.72F) * 62.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.sin(state.age * 0.41F) * 18.0F));
        }
        if (flat) {
            FightRenderState projection = (FightRenderState) state;
            float yawDelta = projection.catfight$getProjectionYaw();
            float viewPitch = MathHelper.clamp(Math.abs(projection.catfight$getProjectionPitch()), 0.0F, 90.0F);
            float groundTilt = 90.0F - viewPitch;
            // Matrix operations affect vertices in reverse order: capture the cat from
            // the selected yaw/pitch first, then collapse the resulting image along
            // world Y. The final paper plane therefore always stays flush with ground.
            // Lift is applied outside the flattened coordinate system, preventing the
            // projected legs, belly and tail from becoming coplanar with terrain.
            matrices.translate(0.0F, 0.035F, 0.0F);
            matrices.scale(1.0F, 0.06F, 1.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(groundTilt));
            // Matrix application is reversed: yaw is now applied to the model first,
            // selecting front/side/back content before pitch aligns it for projection.
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDelta));
        }
    }
}
