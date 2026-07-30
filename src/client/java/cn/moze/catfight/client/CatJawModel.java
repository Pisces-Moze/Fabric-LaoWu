package cn.moze.catfight.client;

import cn.moze.catfight.CatFightManager;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.CatEntityRenderState;
import net.minecraft.util.math.MathHelper;

public final class CatJawModel extends Model<CatEntityRenderState> {
    private final ModelPart headJoint;
    private final ModelPart jaw;

    public CatJawModel() {
        super(createRoot(), RenderLayers::entityCutoutNoCull);
        this.headJoint = this.root.getChild("head_joint");
        this.jaw = this.headJoint.getChild("jaw");
    }

    private static ModelPart createRoot() {
        ModelData data = new ModelData();
        Dilation dilation = new Dilation(0.0F);
        ModelPartData headJoint = data.getRoot().addChild(
            "head_joint",
            ModelPartBuilder.create()
                .cuboid("main", -2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 5.0F, dilation)
                .cuboid("upper_nose", -1.5F, -0.001F, -4.0F, 3, 1, 2, dilation, 0, 24)
                .cuboid("ear1", -2.0F, -3.0F, 0.0F, 1, 1, 2, dilation, 0, 10)
                .cuboid("ear2", 1.0F, -3.0F, 0.0F, 1, 1, 2, dilation, 6, 10),
            ModelTransform.origin(0.0F, 16.8032F, -7.2F)
        );
        headJoint.addChild(
            "jaw",
            ModelPartBuilder.create().cuboid("lower_nose", -1.5F, 0.0F, -2.0F, 3, 1, 2, dilation, 0, 25),
            ModelTransform.origin(0.0F, 1.0F, -2.0F)
        );
        return TexturedModelData.of(data, 64, 32).createModel();
    }

    @Override
    public void setAngles(CatEntityRenderState state) {
        super.setAngles(state);
        int fightState = ((FightRenderState) state).catfight$getFightState();
        float chew;
        if (CatFightManager.isThreat(fightState)) {
            // Smoothstep produces a slow opening and a visibly quicker gum-chewing close.
            float cycle = (MathHelper.sin(state.age * 0.56F) + 1.0F) * 0.5F;
            chew = cycle * cycle * (3.0F - 2.0F * cycle);
            chew = MathHelper.clamp(chew + MathHelper.sin(state.age * 1.73F) * 0.06F, 0.0F, 1.0F);
        } else {
            chew = (MathHelper.sin(state.age * 1.38F) + 1.0F) * 0.5F;
        }
        float open = 0.035F + chew * 0.31F;

        // Reproduce CAT_TRANSFORMER on one parent joint. Both skull and hinged jaw then
        // share exactly the same coordinate system instead of drifting in render space.
        this.headJoint.xScale = 0.8F;
        this.headJoint.yScale = 0.8F;
        this.headJoint.zScale = 0.8F;
        this.headJoint.originY = 16.8032F - 0.35F;
        this.headJoint.originZ = -7.2F + 0.25F;
        this.headJoint.pitch = -0.38F + MathHelper.sin(state.age * 1.91F) * 0.035F;
        this.headJoint.yaw = state.relativeHeadYaw * (float) (Math.PI / 180.0);
        this.headJoint.roll = fightState == CatFightManager.THREAT ? 0.27F : fightState == CatFightManager.THREAT_MIRRORED ? -0.27F : 0.0F;
        this.jaw.pitch = open;
        this.jaw.originY = 1.0F + chew * 0.12F;
    }
}
