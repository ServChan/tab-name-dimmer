package org.lts.tabnamedimmer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
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
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.lts.tabnamedimmer.mixin.accessor.RenderSetupAccessor;
import org.lts.tabnamedimmer.mixin.accessor.RenderTypeAccessor;
import org.lts.tabnamedimmer.mixin.accessor.TextureBindingAccessor;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

public class TranslucentSubmitNodeCollector implements SubmitNodeCollector {
    private static final int MAX_RENDER_TYPE_CACHE_SIZE = 256;
    private static final Map<RenderType, RenderType> TRANSLUCENT_TYPE_CACHE = new IdentityHashMap<>();
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
        synchronized (TRANSLUCENT_TYPE_CACHE) {
            if (TRANSLUCENT_TYPE_CACHE.containsKey(type)) {
                return TRANSLUCENT_TYPE_CACHE.get(type);
            }
        }
        Identifier texture = extractTexture(type);
        RenderType converted = type;
        if (texture != null) {
            String name = type.toString().toLowerCase(Locale.ROOT);
            if (name.contains("item")) {
                converted = RenderTypes.itemTranslucent(texture);
            } else {
                converted = RenderTypes.entityTranslucent(texture);
            }
        }
        synchronized (TRANSLUCENT_TYPE_CACHE) {
            if (TRANSLUCENT_TYPE_CACHE.size() >= MAX_RENDER_TYPE_CACHE_SIZE) {
                TRANSLUCENT_TYPE_CACHE.clear();
            }
            TRANSLUCENT_TYPE_CACHE.put(type, converted);
        }
        return converted;
    }

    private static Identifier extractTexture(RenderType type) {
        if (type == null) return null;
        Map<String, Object> textures = ((RenderSetupAccessor) (Object)
                ((RenderTypeAccessor) (Object) type).tabNameDimmer$getState()).tabNameDimmer$getTextures();
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        Object binding = textures.get("Sampler0");
        if (binding == null) {
            binding = textures.values().iterator().next();
        }
        return binding instanceof TextureBindingAccessor accessor
                ? accessor.tabNameDimmer$getLocation() : null;
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
                                 int packedLight, int packedOverlay, int tint, UvMapping uvMapping,
                                 int outlineColor) {
        int newTint = modifyColor(tint);
        RenderType translucentType = convertToTranslucent(renderType);
        delegate.submitModel(model, state, poseStack, translucentType, packedLight, packedOverlay, newTint, uvMapping, outlineColor);
    }

    @Override
    public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                           int packedLight, int packedOverlay, int tint,
                                           ModelFeatureRenderer.CrumblingOverlay crumbling) {
        delegate.submitCrumblingOverlay(model, state, poseStack, renderType, packedLight, packedOverlay, tint, crumbling);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int light, int overlay,
                           int outlineColor, int[] tint, ItemQuads quads, ItemStackRenderState.FoilType foilType) {
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

    @Override public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(poseStack, radius, pieces);
    }

    @Override public void submitNameTag(PoseStack poseStack, Vec3 pos, int i, Component text, boolean b, int j, CameraRenderState camera) {
        delegate.submitNameTag(poseStack, pos, i, text, b, j, camera);
    }

    @Override public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean b, Font.DisplayMode mode, int i, int j, int k, int l) {
        delegate.submitText(poseStack, x, y, text, b, mode, i, j, k, l);
    }

    @Override public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode mode, int light) {
        delegate.submitTextBackground(poseStack, x0, y0, x1, y1, color, mode, light);
    }

    @Override public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rot) {
        delegate.submitFlame(poseStack, state, rot);
    }

    @Override public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) {
        delegate.submitLeash(poseStack, leash);
    }

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

    @Override public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int i, boolean b) {
        delegate.submitBreakingBlockModel(poseStack, parts, i, b);
    }

    @Override public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer custom) {
        delegate.submitCustomGeometry(poseStack, convertToTranslucent(renderType), custom);
    }

    @Override public void submitQuadParticleGroup(QuadParticleRenderState group) {
        delegate.submitQuadParticleGroup(group);
    }

    @Override public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean b) {
        delegate.submitGizmoPrimitives(group, camera, b);
    }

    @Override public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float alpha, boolean b) {
        delegate.submitShapeOutline(poseStack, shape, renderType, color, alpha, b);
    }
}
