package cn.moze.catfight.client;

import cn.moze.catfight.CatFightManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.entity.state.CatEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public final class CatSegmentedBodyFeatureRenderer extends FeatureRenderer<CatEntityRenderState, CatEntityModel> {
    private final CatSegmentedBodyModel adultBodyModel = new CatSegmentedBodyModel(false);
    private final CatSegmentedBodyModel babyBodyModel = new CatSegmentedBodyModel(true);

    public CatSegmentedBodyFeatureRenderer(FeatureRendererContext<CatEntityRenderState, CatEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CatEntityRenderState state, float limbAngle, float limbDistance) {
        if (CatFightManager.isThreat(((FightRenderState) state).catfight$getFightState())) {
            CatSegmentedBodyModel model = state.baby ? this.babyBodyModel : this.adultBodyModel;
            render(model, state.texture, matrices, queue, light, state, -1, 1);
        }
    }
}
