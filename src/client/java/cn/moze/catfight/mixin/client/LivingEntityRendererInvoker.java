package cn.moze.catfight.mixin.client;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker<S extends EntityRenderState, M extends EntityModel<? super S>> {
    @Invoker("addFeature")
    boolean catfight$addFeature(FeatureRenderer<S, M> feature);
}
