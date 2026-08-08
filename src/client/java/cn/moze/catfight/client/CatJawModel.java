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
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.entity.model.FelineEntityModel;
import net.minecraft.client.render.entity.state.CatEntityRenderState;
import net.minecraft.util.math.MathHelper;

public final class CatJawModel extends Model<CatEntityRenderState> {
    private final ModelPart headJoint;
    private final ModelPart jaw;

    public CatJawModel(boolean baby) {
        super(createRoot(baby), RenderLayers::entityCutoutNoCull);
        this.headJoint = this.root.getChild("head");
        this.jaw = this.headJoint.getChild("jaw");
    }

    private static ModelPart createRoot(boolean baby) {
        ModelData data = new ModelData();
        Dilation dilation = new Dilation(0.0F);
        ModelPartData headJoint = data.getRoot().addChild(
            "head",
            ModelPartBuilder.create()
                .cuboid("main", -2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 5.0F, dilation)
                .cuboid("upper_nose", -1.5F, -0.001F, -4.0F, 3, 1, 2, dilation, 0, 24)
                .cuboid("ear1", -2.0F, -3.0F, 0.0F, 1, 1, 2, dilation, 0, 10)
                .cuboid("ear2", 1.0F, -3.0F, 0.0F, 1, 1, 2, dilation, 6, 10),
            // Raw vanilla feline head pivot. Adult/baby and cat transforms are
            // applied below in exactly the same order as the built-in model layer.
            ModelTransform.origin(0.0F, 15.0F, -9.0F)
        );
        headJoint.addChild(
            "jaw",
            ModelPartBuilder.create().cuboid("lower_nose", -1.5F, 0.0F, -2.0F, 3, 1, 2, dilation, 0, 25),
            ModelTransform.origin(0.0F, 1.0F, -2.0F)
        );
        TexturedModelData textured = TexturedModelData.of(data, 64, 32);
        if (baby) {
            textured = textured.transform(FelineEntityModel.BABY_TRANSFORMER);
        }
        return textured.transform(CatEntityModel.CAT_TRANSFORMER).createModel();
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

        // The adult/baby geometry already uses the same transformer chain as the
        // selected vanilla cat model. Only apply animation offsets here; applying a
        // second scale at runtime made some threat poses appear abnormally enlarged.
        this.headJoint.originY -= 0.35F;
        this.headJoint.originZ += 0.25F;
        this.headJoint.pitch = -0.38F + MathHelper.sin(state.age * 1.91F) * 0.035F;
        // The opponents face 180 degrees apart, so the same model-local turn becomes
        // opposite in world space. Using opposite local signs made both heads appear
        // to lean toward the same world side when the cats faced one another.
        float threatTurn = CatFightManager.isThreat(fightState) ? 1.0F : 0.0F;
        this.headJoint.yaw = state.relativeHeadYaw * (float) (Math.PI / 180.0) + threatTurn * 0.24F;
        this.headJoint.roll = threatTurn * 0.27F;
        this.jaw.pitch = open;
        this.jaw.originY = 1.0F + chew * 0.12F;
    }
}
