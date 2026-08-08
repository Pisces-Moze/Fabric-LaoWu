package cn.moze.catfight.client;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.entity.state.CatEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public final class CatJawFeatureRenderer extends FeatureRenderer<CatEntityRenderState, CatEntityModel> {
    private final CatJawModel adultJawModel;
    private final CatJawModel babyJawModel;

    public CatJawFeatureRenderer(FeatureRendererContext<CatEntityRenderState, CatEntityModel> context) {
        super(context);
        this.adultJawModel = new CatJawModel(false);
        this.babyJawModel = new CatJawModel(true);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CatEntityRenderState state, float limbAngle, float limbDistance) {
        if (((FightRenderState) state).catfight$getFightState() != 0) {
            CatJawModel model = state.baby ? this.babyJawModel : this.adultJawModel;
            render(model, state.texture, matrices, queue, light, state, -1, 2);
        }
    }
}
