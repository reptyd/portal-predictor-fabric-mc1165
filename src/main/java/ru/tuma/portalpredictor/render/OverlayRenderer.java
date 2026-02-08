package ru.tuma.portalpredictor.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.tuma.portalpredictor.config.PredictorConfig;
import ru.tuma.portalpredictor.logic.PortalPlan;
import ru.tuma.portalpredictor.logic.PortalPredictorEngine;
import ru.tuma.portalpredictor.logic.TargetManager;

import java.util.Optional;

public final class OverlayRenderer {
    private OverlayRenderer() {}

    public static void renderWorld(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        PredictorConfig cfg = PredictorConfig.get();
        if (!cfg.overlayEnabled) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.lineWidth(3.0f);

        MatrixStack matrices = new MatrixStack();
        Vec3d cam = context.camera().getPos();
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumerProvider consumers = context.consumers();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        Optional<BlockPos> targetA = TargetManager.get().getTargetA();
        Optional<BlockPos> targetB = TargetManager.get().getTargetB();
        if (client.world.getRegistryKey() == World.OVERWORLD) {
            if (targetA.isPresent()) {
                drawBlockBox(matrices, lines, targetA.get(), 0.05f, 0.2f, 0.9f, 0.9f,
                        alpha(cfg, 0.95f), fillAlpha(cfg, 0.25f));
            }
            if (targetB.isPresent()) {
                drawBlockBox(matrices, lines, targetB.get(), 0.05f, 0.95f, 0.85f, 0.2f,
                        alpha(cfg, 0.95f), fillAlpha(cfg, 0.25f));
            }
        }

        if (cfg.showScanBox && client.player != null) {
            int r = Math.max(2, Math.min(cfg.scanRadiusMax, cfg.scanRadius));
            BlockPos c = client.player.getBlockPos();
            Box scan = new Box(
                    c.getX() - r, c.getY() - r, c.getZ() - r,
                    c.getX() + r + 1, c.getY() + r + 1, c.getZ() + r + 1
            );
            WorldRenderer.drawBox(matrices, lines, scan, 0.25f, 0.7f, 0.95f, alpha(cfg, 0.35f));
        }

        PortalPredictorEngine.get().getCurrentPlan().ifPresent(plan -> {
            PortalPlan.PortalShape shape = null;
            if (client.world.getRegistryKey() == World.NETHER) {
                shape = plan.netherShape;
            } else if (client.world.getRegistryKey() == World.OVERWORLD) {
                shape = plan.overworldShape;
            }

            if (shape != null) {
                for (BlockPos p : shape.frameBlocks) {
                    drawBlockBox(matrices, lines, p, 0.05f, 0.95f, 0.55f, 0.15f,
                            alpha(cfg, 0.9f), fillAlpha(cfg, 0.18f));
                }
                for (BlockPos p : shape.portalBlocks) {
                    drawBlockBox(matrices, lines, p, 0.05f, 0.65f, 0.2f, 0.85f,
                            alpha(cfg, 0.8f), fillAlpha(cfg, 0.12f));
                }
            }

            if (client.world.getRegistryKey() == World.OVERWORLD || client.world.getRegistryKey() == World.NETHER) {
                for (BlockPos p : plan.blockers) {
                    drawBlockBox(matrices, lines, p, 0.05f, 0.9f, 0.2f, 0.2f,
                            alpha(cfg, 0.9f), fillAlpha(cfg, 0.35f));
                }
                for (BlockPos p : plan.primaryClear) {
                    drawBlockBox(matrices, lines, p, 0.05f, 0.95f, 0.85f, 0.2f,
                            alpha(cfg, 0.9f), fillAlpha(cfg, 0.35f));
                }
                for (BlockPos p : plan.primarySolid) {
                    drawBlockBox(matrices, lines, p, 0.05f, 0.2f, 0.55f, 0.95f,
                            alpha(cfg, 0.9f), fillAlpha(cfg, 0.35f));
                }
            }
        });

        matrices.pop();
        if (consumers instanceof VertexConsumerProvider.Immediate) {
            ((VertexConsumerProvider.Immediate) consumers).draw();
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawBlockBox(MatrixStack matrices, VertexConsumer lines, BlockPos pos,
                                     float expand, float r, float g, float b, float outline, float fill) {
        PredictorConfig cfg = PredictorConfig.get();
        Box box = new Box(pos).expand(expand);
        if (cfg.overlayInset > 0.0f) {
            double inset = cfg.overlayInset;
            box = new Box(box.minX + inset, box.minY + inset, box.minZ + inset,
                    box.maxX - inset, box.maxY - inset, box.maxZ - inset);
        }
        drawFilledBox(matrices, box, r, g, b, fill);
        WorldRenderer.drawBox(matrices, lines, box, r, g, b, outline);
    }

    private static float alpha(PredictorConfig cfg, float base) {
        return Math.min(1.0f, base * cfg.overlayAlpha);
    }

    private static float fillAlpha(PredictorConfig cfg, float base) {
        return Math.min(1.0f, base * cfg.overlayFillAlpha);
    }

    private static void drawFilledBox(MatrixStack matrices, Box box,
                                      float r, float g, float b, float a) {
        if (a <= 0.0f) {
            return;
        }
        Matrix4f m = matrices.peek().getModel();
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(7, VertexFormats.POSITION_COLOR);
        quad(buffer, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a);
        quad(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
        quad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        quad(buffer, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
        quad(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
        quad(buffer, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
        Tessellator.getInstance().draw();
        RenderSystem.enableTexture();
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a).next();
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a).next();
    }

    public static void renderHud(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PredictorConfig cfg = PredictorConfig.get();
        if (client.player == null) return;

        TextRenderer tr = client.textRenderer;
        int x = 6;
        int y = 6;

        Optional<BlockPos> targetA = TargetManager.get().getTargetA();
        Optional<BlockPos> targetB = TargetManager.get().getTargetB();
        if (!targetA.isPresent()) {
            tr.drawWithShadow(matrices, new TranslatableText("text.portalpredictor.no_target"), x, y, 0xFFFFFFFF);
        } else {
            BlockPos a = targetA.get();
            tr.drawWithShadow(matrices,
                    new TranslatableText("text.portalpredictor.target_a", a.getX() + " " + a.getY() + " " + a.getZ()),
                    x, y, 0xFFFFFFFF);
            y += 10;
            if (targetB.isPresent()) {
                BlockPos b = targetB.get();
                tr.drawWithShadow(matrices,
                        new TranslatableText("text.portalpredictor.target_b", b.getX() + " " + b.getY() + " " + b.getZ()),
                        x, y, 0xFFFFFFFF);
            } else {
                tr.drawWithShadow(matrices,
                        new TranslatableText("text.portalpredictor.target_b", "-"), x, y, 0xFFBBBBBB);
            }
        }
        y += 10;

        tr.drawWithShadow(matrices,
                new TranslatableText("text.portalpredictor.scan_radius", String.valueOf(cfg.scanRadius)), x, y, 0xFFBBBBBB);
        y += 10;

        tr.drawWithShadow(matrices,
                new TranslatableText("text.portalpredictor.local_scan", String.valueOf(cfg.nearbyScanRadius)), x, y, 0xFFBBBBBB);
        y += 10;

        tr.drawWithShadow(matrices,
                new TranslatableText("text.portalpredictor.surface_scan", String.valueOf(cfg.surfaceScanChunkRadius)), x, y, 0xFFBBBBBB);
        y += 10;

        tr.drawWithShadow(matrices,
                new TranslatableText("text.portalpredictor.scan_mode", cfg.scanThroughWalls ? "through" : "visible"),
                x, y, 0xFFBBBBBB);
        y += 10;

        tr.drawWithShadow(matrices,
                new TranslatableText(cfg.unknownAsSolid
                        ? "text.portalpredictor.unknown_mode_solid"
                        : "text.portalpredictor.unknown_mode_air"),
                x, y, 0xFFBBBBBB);
        y += 10;

        int scanCount = ru.tuma.portalpredictor.logic.ScanManager.get().getLastScanCount();
        tr.drawWithShadow(matrices,
                new TranslatableText("text.portalpredictor.scan_cells", String.valueOf(scanCount)), x, y, 0xFFBBBBBB);
        y += 10;

        Optional<PortalPlan> planOpt = PortalPredictorEngine.get().getCurrentPlan();
        if (planOpt.isPresent()) {
            PortalPlan plan = planOpt.get();
            tr.drawWithShadow(matrices,
                    new TranslatableText("text.portalpredictor.plan", plan.summary), x, y, 0xFFDDDDDD);
        } else {
            tr.drawWithShadow(matrices, new TranslatableText("text.portalpredictor.no_plan"), x, y, 0xFFAAAAAA);
        }
    }
}
