package cn.moze.catfight.client;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.entity.state.CatEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public final class CatJawFeatureRenderer extends FeatureRenderer<CatEntityRenderState, CatEntityModel> {
    private final CatJawModel jawModel;

    public CatJawFeatureRenderer(FeatureRendererContext<CatEntityRenderState, CatEntityModel> context) {
        super(context);
        this.jawModel = new CatJawModel();
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CatEntityRenderState state, float limbAngle, float limbDistance) {
        if (((FightRenderState) state).catfight$getFightState() != 0) {
            render(this.jawModel, state.texture, matrices, queue, light, state, -1, 2);
        }
    }
}
