package io.csum.segmented.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.csum.segmented.SegmentedMod;
import io.csum.segmented.client.gui.SegmentedHotbarRenderer;
import io.csum.segmented.config.SegmentedConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.geom.Point2D;

import static io.csum.segmented.client.util.Constants.*;
import static io.csum.segmented.client.util.Constants.TEXTURE_SIZE;

@Mixin(InGameHud.class)
@Environment(EnvType.CLIENT)
public abstract class InGameHudMixin extends DrawableHelper {

    private final Identifier SEGMENTED_WIDGETS_TEXTURE = new Identifier("segmented", "textures/gui/widgets.png");
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final SegmentedConfig config = AutoConfig.getConfigHolder(SegmentedConfig.class).getConfig();

    // rewrites the hotbar renderer so that it is segmented, and handles cancelling segment selection on a timer
    @Inject(method = "renderHotbar", at = @At(value = "HEAD"), cancellable = true, require = 0)
    private void renderSegmentedHotbar(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
        if (config.timerreset > 0) {
            if (SegmentedMod.cancelTimer > 0) {
                SegmentedMod.cancelTimer -= tickDelta;
            } else {
                SegmentedMod.cancelTimer = 0;
                SegmentedMod.selectedHotbarSegment = -1;
            }
        }

        SegmentedHotbarRenderer segmentedHotbarRenderer = new SegmentedHotbarRenderer();
        if (segmentedHotbarRenderer.renderSegmentedHotbar(matrices)) {
            ci.cancel();
        }
    }

    // "only selector" mode
    @Inject(method = "renderHotbar", at = @At(value = "TAIL"), require = 0)
    private void renderSegmentedSelectorOnly(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
        if (config.onlyselector) {

            SegmentedHotbarRenderer segmentedHotbarRenderer = new SegmentedHotbarRenderer();
            if (segmentedHotbarRenderer.isHidden() || !config.enabled) { return; }
            PlayerEntity player = (PlayerEntity)client.cameraEntity;
            if (player == null) { return; }

            // integration with raised
            if (FabricLoader.getInstance().getObjectShare().get("raised:hud") instanceof Integer hud) {
                SegmentedMod.raiseAmount = hud - 2;
            }

            int scaledWidthHalved = client.getWindow().getScaledWidth() / 2;
            int scaledHeight = client.getWindow().getScaledHeight();
            Point2D.Float hotbar = new Point2D.Float(scaledWidthHalved - 93, scaledHeight - 24 - (SegmentedMod.raiseAmount + 2));

            RenderSystem.setShaderTexture(0, SEGMENTED_WIDGETS_TEXTURE);

            // draw segment selection box if needed
            if (SegmentedMod.selectedHotbarSegment != -1) {
                drawTexture(matrices,
                        (int) hotbar.x + (SegmentedMod.selectedHotbarSegment * (SEGMENT_OFFSET - HOTBAR_GAP)),
                        (int) hotbar.y,
                        (float) SEGMENT_SELECTOR.x + (SegmentedMod.selectedHotbarSegment * (SEGMENT_SELECTOR.width + 1)),
                        (float) SEGMENT_SELECTOR.y,
                        SEGMENT_SELECTOR.width,
                        SEGMENT_SELECTOR.height,
                        TEXTURE_SIZE.width,
                        TEXTURE_SIZE.height);
            }
        }
    }

}

