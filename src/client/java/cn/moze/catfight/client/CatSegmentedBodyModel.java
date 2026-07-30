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
import net.minecraft.util.math.MathHelper;

/** A three-bone replacement for the vanilla cat's single rigid torso cuboid. */
public final class CatSegmentedBodyModel extends Model<CatEntityRenderState> {
    private final ModelPart chest;
    private final ModelPart middle;
    private final ModelPart rump;

    public CatSegmentedBodyModel() {
        super(createRoot(), RenderLayers::entityCutoutNoCull);
        this.chest = this.root.getChild("chest");
        this.middle = this.chest.getChild("middle");
        this.rump = this.middle.getChild("rump");
    }

    private static ModelPart createRoot() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        Dilation dilation = new Dilation(0.0F);

        // Cuboids extend towards +Z. Child origins sit slightly inside the preceding
        // segment so that bending never exposes a visible gap in the cat's back.
        ModelPartData chest = root.addChild(
            "chest",
            ModelPartBuilder.create().uv(20, 0).cuboid(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 5.5F, dilation),
            ModelTransform.origin(0.0F, 17.0F, -7.0F)
        );
        ModelPartData middle = chest.addChild(
            "middle",
            ModelPartBuilder.create().uv(20, 6).cuboid(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 5.5F, dilation),
            ModelTransform.origin(0.0F, 0.0F, 5.2F)
        );
        middle.addChild(
            "rump",
            ModelPartBuilder.create().uv(20, 12).cuboid(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 5.6F, dilation),
            ModelTransform.origin(0.0F, 0.0F, 5.2F)
        );
        return TexturedModelData.of(data, 64, 32).transform(CatEntityModel.CAT_TRANSFORMER).createModel();
    }

    @Override
    public void setAngles(CatEntityRenderState state) {
        super.setAngles(state);
        if (!CatFightManager.isThreat(((FightRenderState) state).catfight$getFightState())) {
            return;
        }

        float breathing = MathHelper.sin(state.age * 0.34F) * 0.018F;
        // Accumulated angles are +0.31, +0.02, -0.29 radians: a continuous arch.
        this.chest.pitch = 0.31F + breathing;
        this.middle.pitch = -0.29F - breathing * 0.45F;
        this.rump.pitch = -0.31F + breathing * 0.25F;
        this.chest.roll = MathHelper.sin(state.age * 0.18F) * 0.012F;
    }
}
