package ru.tuma.portalpredictor.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import ru.tuma.portalpredictor.config.PredictorConfig;
import ru.tuma.portalpredictor.logic.PortalPredictorEngine;
import ru.tuma.portalpredictor.logic.ScanManager;
import ru.tuma.portalpredictor.logic.TargetManager;
import ru.tuma.portalpredictor.render.OverlayRenderer;
import ru.tuma.portalpredictor.ui.PredictorScreen;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class PortalPredictorClient implements ClientModInitializer {
    public static final String MOD_ID = "portalpredictor";
    public static final String KEY_CATEGORY = "key.categories.portalpredictor";

    private static KeyBinding KEY_SET_TARGET;
    private static KeyBinding KEY_CLEAR_TARGET;
    private static KeyBinding KEY_RESCAN;
    private static KeyBinding KEY_RESCAN_NETHER;
    private static KeyBinding KEY_CLEAR_SCAN;
    private static KeyBinding KEY_PLAN;
    private static KeyBinding KEY_TOGGLE_OVERLAY;
    private static KeyBinding KEY_OPEN_UI;

    @Override
    public void onInitializeClient() {
        PredictorConfig.load();

        KEY_SET_TARGET = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.set_target",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KEY_CATEGORY
        ));
        KEY_CLEAR_TARGET = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.clear_target",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));
        KEY_RESCAN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.rescan",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KEY_CATEGORY
        ));
        KEY_RESCAN_NETHER = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.rescan_nether",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                KEY_CATEGORY
        ));
        KEY_CLEAR_SCAN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.clear_scan",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));
        KEY_PLAN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.plan",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KEY_CATEGORY
        ));
        KEY_TOGGLE_OVERLAY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.toggle_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KEY_CATEGORY
        ));
        KEY_OPEN_UI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.portalpredictor.open_ui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(PortalPredictorClient::onClientTick);
        WorldRenderEvents.LAST.register(OverlayRenderer::renderWorld);
        HudRenderCallback.EVENT.register(OverlayRenderer::renderHud);

        ClientCommandManager.DISPATCHER.register(literal("portalpredictor")
                .then(literal("target")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    TargetManager.get().setTargetA(new BlockPos(x, y, z));
                                                    TargetManager.get().clearTargetB();
                                                    ctx.getSource().sendFeedback(new LiteralText("Target A set to " + x + " " + y + " " + z));
                                                    return 1;
                                                })))))
                .then(literal("targetb")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    TargetManager.get().setTargetB(new BlockPos(x, y, z));
                                                    ctx.getSource().sendFeedback(new LiteralText("Target B set to " + x + " " + y + " " + z));
                                                    return 1;
                                                })))))
                .then(literal("clear").executes(ctx -> {
                    TargetManager.get().clear();
                    PortalPredictorEngine.get().clearPlan();
                    ctx.getSource().sendFeedback(new LiteralText("Target cleared"));
                    return 1;
                }))
                .then(literal("scan").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null && client.world != null) {
                        ScanManager.get().rescanAroundPlayer(client.world, client.player);
                        ctx.getSource().sendFeedback(new LiteralText("Scan complete"));
                    }
                    return 1;
                }))
                .then(literal("scannether").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null && client.world != null) {
                        if (client.world.getRegistryKey() == net.minecraft.world.World.NETHER) {
                            TargetManager.get().setNetherAnchorOverride(client.player.getBlockPos());
                        }
                        ScanManager.get().rescanAroundPlayer(client.world, client.player, false);
                        ctx.getSource().sendFeedback(new LiteralText("Nether scan complete"));
                    }
                    return 1;
                }))
                .then(literal("clearscan").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.world != null) {
                        ScanManager.get().clearDimension(client.world);
                        ctx.getSource().sendFeedback(new LiteralText("Scan cleared"));
                    }
                    return 1;
                }))
                .then(literal("plan").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    PortalPredictorEngine.get().recompute(client);
                    ctx.getSource().sendFeedback(new LiteralText("Plan recomputed"));
                    return 1;
                }))
        );
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        while (KEY_SET_TARGET.wasPressed()) {
            TargetManager.get().cycleTargetFromCrosshair(client);
        }
        while (KEY_CLEAR_TARGET.wasPressed()) {
            TargetManager.get().clear();
            PortalPredictorEngine.get().clearPlan();
        }
        while (KEY_RESCAN.wasPressed()) {
            ScanManager.get().rescanAroundPlayer(client.world, client.player);
        }
        while (KEY_RESCAN_NETHER.wasPressed()) {
            if (client.world.getRegistryKey() == net.minecraft.world.World.NETHER) {
                TargetManager.get().setNetherAnchorOverride(client.player.getBlockPos());
            }
            ScanManager.get().rescanAroundPlayer(client.world, client.player, false);
        }
        while (KEY_CLEAR_SCAN.wasPressed()) {
            ScanManager.get().clearDimension(client.world);
        }
        while (KEY_PLAN.wasPressed()) {
            PortalPredictorEngine.get().recompute(client);
        }
        while (KEY_TOGGLE_OVERLAY.wasPressed()) {
            PredictorConfig.get().toggleOverlay();
        }
        while (KEY_OPEN_UI.wasPressed()) {
            client.openScreen(new PredictorScreen(client.currentScreen));
        }
    }
}
