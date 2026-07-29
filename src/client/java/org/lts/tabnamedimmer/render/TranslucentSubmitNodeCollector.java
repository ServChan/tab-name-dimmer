package org.lts.tabnamedimmer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wraps a SubmitNodeCollector to scale alpha AND convert cutout/solid render types to translucent
 * for all submitted player body models, armor pieces, capes, elytra, and held items.
 */
public class TranslucentSubmitNodeCollector implements SubmitNodeCollector {
    private final OrderedSubmitNodeCollector delegate;
    private final float opacity;

    public TranslucentSubmitNodeCollector(OrderedSubmitNodeCollector delegate, float opacity) {
        this.delegate = delegate;
        this.opacity = opacity;
    }

    private int modifyColor(int color) {
        int currentAlpha = ARGB.alpha(color);
        if (currentAlpha == 0) {
            currentAlpha = 255;
        }
        int newAlpha = (int) (currentAlpha * opacity);
        newAlpha = Math.max(0, Math.min(255, newAlpha));
        return ARGB.color(newAlpha, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }

    private RenderType convertToTranslucent(RenderType type) {
        if (type == null || type.hasBlending()) {
            return type;
        }
        Identifier texture = extractTexture(type);
        if (texture == null) {
            return type;
        }
        String name = type.toString().toLowerCase(Locale.ROOT);
        if (name.contains("armor")) {
            return RenderTypes.armorTranslucent(texture);
        } else if (name.contains("item")) {
            return RenderTypes.itemTranslucent(texture);
        } else {
            return RenderTypes.entityTranslucent(texture);
        }
    }

    private static Identifier extractTexture(RenderType type) {
        if (type == null) return null;
        try {
            Field stateField = RenderType.class.getDeclaredField("state");
            stateField.setAccessible(true);
            Object setup = stateField.get(type);

            Field texturesField = setup.getClass().getDeclaredField("textures");
            texturesField.setAccessible(true);
            Map<?, ?> textures = (Map<?, ?>) texturesField.get(setup);

            if (textures != null && !textures.isEmpty()) {
                Object binding = textures.get("Sampler0");
                if (binding == null) {
                    binding = textures.values().iterator().next();
                }
                Method locationMethod = binding.getClass().getMethod("location");
                locationMethod.setAccessible(true);
                return (Identifier) locationMethod.invoke(binding);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public OrderedSubmitNodeCollector order(int i) {
        if (delegate instanceof SubmitNodeCollector collector) {
            return new TranslucentSubmitNodeCollector(collector.order(i), opacity);
        }
        return this;
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                 int packedLight, int packedOverlay, int tint, TextureAtlasSprite sprite,
                                 int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        int newTint = modifyColor(tint);
        RenderType translucentType = convertToTranslucent(renderType);
        delegate.submitModel(model, state, poseStack, translucentType, packedLight, packedOverlay, newTint, sprite, outlineColor, crumbling);
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType,
                                int packedLight, int packedOverlay, TextureAtlasSprite sprite,
                                boolean doubleSided, boolean outline, int tint,
                                ModelFeatureRenderer.CrumblingOverlay crumbling, int order) {
        int newTint = modifyColor(tint);
        RenderType translucentType = convertToTranslucent(renderType);
        delegate.submitModelPart(part, poseStack, translucentType, packedLight, packedOverlay, sprite, doubleSided, outline, newTint, crumbling, order);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int light, int overlay,
                           int outlineColor, int[] tint, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        int[] newTint;
        if (tint != null && tint.length > 0) {
            newTint = new int[tint.length];
            for (int i = 0; i < tint.length; i++) {
                newTint[i] = modifyColor(tint[i]);
            }
        } else {
            newTint = new int[]{ modifyColor(-1) };
        }
        delegate.submitItem(poseStack, displayContext, light, overlay, outlineColor, newTint, quads, foilType);
    }

    @Override public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) { delegate.submitShadow(poseStack, radius, pieces); }
    @Override public void submitNameTag(PoseStack poseStack, Vec3 pos, int i, Component text, boolean b, int j, double d, CameraRenderState camera) { delegate.submitNameTag(poseStack, pos, i, text, b, j, d, camera); }
    @Override public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean b, Font.DisplayMode mode, int i, int j, int k, int l) { delegate.submitText(poseStack, x, y, text, b, mode, i, j, k, l); }
    @Override public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rot) { delegate.submitFlame(poseStack, state, rot); }
    @Override public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) { delegate.submitLeash(poseStack, leash); }
    @Override public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState block) { delegate.submitMovingBlock(poseStack, block); }
    @Override public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tint, int i, int j, int k) {
        int[] newTint = null;
        if (tint != null) {
            newTint = new int[tint.length];
            for (int idx = 0; idx < tint.length; idx++) newTint[idx] = modifyColor(tint[idx]);
        }
        delegate.submitBlockModel(poseStack, convertToTranslucent(renderType), parts, newTint, i, j, k);
    }
    @Override public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long l, int i) { delegate.submitBreakingBlockModel(poseStack, model, l, i); }
    @Override public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer custom) { delegate.submitCustomGeometry(poseStack, convertToTranslucent(renderType), custom); }
    @Override public void submitParticleGroup(ParticleGroupRenderer group) { delegate.submitParticleGroup(group); }
}
