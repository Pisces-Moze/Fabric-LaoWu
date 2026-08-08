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
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.entity.model.FelineEntityModel;
import net.minecraft.util.math.MathHelper;

/** A five-bone replacement for the vanilla cat's single rigid torso cuboid. */
public final class CatSegmentedBodyModel extends Model<CatEntityRenderState> {
    private final ModelPart spine1;
    private final ModelPart spine2;
    private final ModelPart spine3;
    private final ModelPart spine4;
    private final ModelPart spine5;

    public CatSegmentedBodyModel(boolean baby) {
        super(createRoot(baby), RenderLayers::entityCutoutNoCull);
        this.spine1 = this.root.getChild("spine1");
        this.spine2 = this.spine1.getChild("spine2");
        this.spine3 = this.spine2.getChild("spine3");
        this.spine4 = this.spine3.getChild("spine4");
        this.spine5 = this.spine4.getChild("spine5");
    }

    private static ModelPart createRoot(boolean baby) {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        Dilation dilation = new Dilation(0.0F);

        // Keep the vanilla 4 x 6 cross-section and 16-pixel body length. Each short
        // cuboid overlaps the next joint by two pixels. The overlap remains inside
        // the torso and prevents either the back or belly from opening while arched.
        ModelPartData spine1 = root.addChild(
            "spine1",
            ModelPartBuilder.create().uv(20, 0).cuboid(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 5.0F, dilation),
            ModelTransform.origin(0.0F, 17.0F, -7.0F)
        );
        ModelPartData spine2 = spine1.addChild(
            "spine2",
            ModelPartBuilder.create().uv(20, 3).cuboid(-2.0F, -3.0F, -1.0F, 4.0F, 6.0F, 5.0F, dilation),
            ModelTransform.origin(0.0F, 0.0F, 3.0F)
        );
        ModelPartData spine3 = spine2.addChild(
            "spine3",
            ModelPartBuilder.create().uv(20, 6).cuboid(-2.0F, -3.0F, -1.0F, 4.0F, 6.0F, 5.0F, dilation),
            ModelTransform.origin(0.0F, 0.0F, 3.0F)
        );
        ModelPartData spine4 = spine3.addChild(
            "spine4",
            ModelPartBuilder.create().uv(20, 9).cuboid(-2.0F, -3.0F, -1.0F, 4.0F, 6.0F, 5.0F, dilation),
            ModelTransform.origin(0.0F, 0.0F, 3.0F)
        );
        spine4.addChild(
            "spine5",
            ModelPartBuilder.create().uv(20, 12).cuboid(-2.0F, -3.0F, -1.0F, 4.0F, 6.0F, 5.0F, dilation),
            ModelTransform.origin(0.0F, 0.0F, 3.0F)
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
        if (!CatFightManager.isThreat(((FightRenderState) state).catfight$getFightState())) {
            return;
        }

        float breathing = MathHelper.sin(state.age * 0.34F) * 0.012F;
        // Small changes at five joints form the same arch without sharp, tearing
        // hinges. Cumulative pitches progress smoothly from +0.22 to -0.22 radians.
        this.spine1.pitch = 0.22F + breathing;
        this.spine2.pitch = -0.10F - breathing * 0.30F;
        this.spine3.pitch = -0.12F - breathing * 0.20F;
        this.spine4.pitch = -0.12F + breathing * 0.20F;
        this.spine5.pitch = -0.10F + breathing * 0.30F;
        this.spine1.roll = MathHelper.sin(state.age * 0.18F) * 0.008F;
    }
}
