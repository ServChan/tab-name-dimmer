package org.lts.tabnamedimmer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wraps a SubmitNodeCollector to scale alpha AND convert cutout/solid render types to translucent
 * for all submitted player body models, armor pieces, capes, elytra, and held items.
 *
 * <p>This is the MC 26.2-compatible variant. OrderedSubmitNodeCollector changed in 26.2:
 * {@code submitNameTag} lost the {@code double} distance parameter, {@code submitMovingBlock}
 * gained an {@code int} argument, {@code submitBreakingBlockModel} and particle methods
 * changed signatures, and new abstract methods were added.
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

    // submitModelPart — all overloads became default in 26.2; let them delegate through submitModel.

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

    // --- Delegated methods (no alpha modification needed) ---

    @Override public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(poseStack, radius, pieces);
    }

    /** 26.2: removed the {@code double} distance parameter present in 26.1.2. */
    @Override public void submitNameTag(PoseStack poseStack, Vec3 pos, int i, Component text, boolean b, int j, CameraRenderState camera) {
        delegate.submitNameTag(poseStack, pos, i, text, b, j, camera);
    }

    @Override public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean b, Font.DisplayMode mode, int i, int j, int k, int l) {
        delegate.submitText(poseStack, x, y, text, b, mode, i, j, k, l);
    }

    @Override public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rot) {
        delegate.submitFlame(poseStack, state, rot);
    }

    @Override public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) {
        delegate.submitLeash(poseStack, leash);
    }

    /** 26.2: gained an extra {@code int} argument compared to 26.1.2. */
    @Override public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState block, int i) {
        delegate.submitMovingBlock(poseStack, block, i);
    }

    @Override public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tint, int i, int j, int k) {
        int[] newTint = null;
        if (tint != null) {
            newTint = new int[tint.length];
            for (int idx = 0; idx < tint.length; idx++) newTint[idx] = modifyColor(tint[idx]);
        }
        delegate.submitBlockModel(poseStack, convertToTranslucent(renderType), parts, newTint, i, j, k);
    }

    /** 26.2: signature changed from (PoseStack, BlockStateModel, long, int) to (PoseStack, List<BlockStateModelPart>, int). */
    @Override public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int i) {
        delegate.submitBreakingBlockModel(poseStack, parts, i);
    }

    @Override public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer custom) {
        delegate.submitCustomGeometry(poseStack, convertToTranslucent(renderType), custom);
    }

    /** 26.2: renamed from submitParticleGroup(ParticleGroupRenderer), now uses QuadParticleRenderState. */
    @Override public void submitQuadParticleGroup(QuadParticleRenderState group) {
        delegate.submitQuadParticleGroup(group);
    }

    /** 26.2: new abstract method; delegate as-is. */
    @Override public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean b) {
        delegate.submitGizmoPrimitives(group, camera, b);
    }

    /** 26.2: new abstract method; delegate as-is. */
    @Override public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float alpha, boolean b) {
        delegate.submitShapeOutline(poseStack, shape, renderType, color, alpha, b);
    }
}
