package ru.tuma.portalpredictor.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import ru.tuma.portalpredictor.config.PredictorConfig;
import ru.tuma.portalpredictor.logic.PortalPredictorEngine;
import ru.tuma.portalpredictor.logic.ScanManager;
import ru.tuma.portalpredictor.logic.TargetManager;

import java.util.Locale;

public class PredictorScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget xFieldA;
    private TextFieldWidget yFieldA;
    private TextFieldWidget zFieldA;
    private TextFieldWidget xFieldB;
    private TextFieldWidget yFieldB;
    private TextFieldWidget zFieldB;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int rowStart;
    private int rowStep;
    private int labelTargetsY;
    private int labelScanY;
    private int labelOffsetsY;
    private int labelOverlayY;
    private int labelTogglesY;

    public PredictorScreen(Screen parent) {
        super(new LiteralText("Portal Predictor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(380, this.width - 20);
        panelHeight = Math.min(480, this.height - 20);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        rowStart = panelY + 28;
        rowStep = 24;
        PredictorConfig cfg = PredictorConfig.get();

        int fieldWidth = 66;
        int fieldGap = 8;
        int fieldsTotal = fieldWidth * 3 + fieldGap * 2;
        int fieldsX = panelX + (panelWidth - fieldsTotal) / 2;
        int row0 = rowStart;
        int row1 = rowStart + rowStep;
        xFieldA = new TextFieldWidget(textRenderer, fieldsX, row0, fieldWidth, 18, new LiteralText("X"));
        yFieldA = new TextFieldWidget(textRenderer, fieldsX + fieldWidth + fieldGap, row0, fieldWidth, 18, new LiteralText("Y"));
        zFieldA = new TextFieldWidget(textRenderer, fieldsX + (fieldWidth + fieldGap) * 2, row0, fieldWidth, 18, new LiteralText("Z"));
        xFieldB = new TextFieldWidget(textRenderer, fieldsX, row1, fieldWidth, 18, new LiteralText("X"));
        yFieldB = new TextFieldWidget(textRenderer, fieldsX + fieldWidth + fieldGap, row1, fieldWidth, 18, new LiteralText("Y"));
        zFieldB = new TextFieldWidget(textRenderer, fieldsX + (fieldWidth + fieldGap) * 2, row1, fieldWidth, 18, new LiteralText("Z"));

        TargetManager.get().getTargetA().ifPresent(t -> {
            xFieldA.setText(String.valueOf(t.getX()));
            yFieldA.setText(String.valueOf(t.getY()));
            zFieldA.setText(String.valueOf(t.getZ()));
        });
        TargetManager.get().getTargetB().ifPresent(t -> {
            xFieldB.setText(String.valueOf(t.getX()));
            yFieldB.setText(String.valueOf(t.getY()));
            zFieldB.setText(String.valueOf(t.getZ()));
        });

        this.addChild(xFieldA);
        this.addChild(yFieldA);
        this.addChild(zFieldA);
        this.addChild(xFieldB);
        this.addChild(yFieldB);
        this.addChild(zFieldB);

        int colGap = 8;
        int colWidth = (panelWidth - 20 - colGap) / 2;
        int leftCol = panelX + 10;
        int rightCol = leftCol + colWidth + colGap;
        int row2 = rowStart + rowStep * 2;
        int row3 = rowStart + rowStep * 3;
        int row4 = rowStart + rowStep * 4;
        int row5 = rowStart + rowStep * 5;
        int row6 = rowStart + rowStep * 6;
        int row7 = rowStart + rowStep * 7;
        int row8 = rowStart + rowStep * 8;
        int row9 = rowStart + rowStep * 9;
        int row10 = rowStart + rowStep * 10;
        int row11 = rowStart + rowStep * 11;
        int row12 = rowStart + rowStep * 12;
        int row13 = rowStart + rowStep * 13;
        int row14 = rowStart + rowStep * 14;
        int row15 = rowStart + rowStep * 15;
        int row16 = rowStart + rowStep * 16;
        int row17 = rowStart + rowStep * 17;

        ButtonWidget setBtnA = new ButtonWidget(leftCol, row2, colWidth, 20, new LiteralText("Set A"), btn -> setTargetFromFields(true));
        ButtonWidget setBtnB = new ButtonWidget(rightCol, row2, colWidth, 20, new LiteralText("Set B"), btn -> setTargetFromFields(false));
        ButtonWidget scanBtn = new ButtonWidget(leftCol, row3, colWidth, 20, new LiteralText("Scan around"), btn -> rescan());
        ButtonWidget scanNetherBtn = new ButtonWidget(rightCol, row3, colWidth, 20, new LiteralText("Scan nether"), btn -> rescanNether());
        ButtonWidget planBtn = new ButtonWidget(leftCol, row4, colWidth, 20, new LiteralText("Recompute plan"), btn -> plan());
        ButtonWidget clearScanBtn = new ButtonWidget(rightCol, row4, colWidth, 20, new LiteralText("Clear scan"), btn -> clearScan());
        ButtonWidget radiusMinus = new ButtonWidget(leftCol, row5, colWidth, 20, new LiteralText("- radius"), btn -> {
            cfg.scanRadius = Math.max(2, cfg.scanRadius - 8);
        });
        ButtonWidget radiusPlus = new ButtonWidget(rightCol, row5, colWidth, 20, new LiteralText("+ radius"), btn -> {
            cfg.scanRadius = Math.min(cfg.scanRadiusMax, cfg.scanRadius + 8);
        });
        SliderWidget localScanSlider = new SliderWidget(leftCol, row6, panelWidth - 20, 20,
                new LiteralText(localScanText(cfg)), cfg.nearbyScanRadius / (double) cfg.nearbyScanRadiusMax) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(localScanText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.nearbyScanRadius = (int) Math.round(this.value * cfg.nearbyScanRadiusMax);
            }
        };
        SliderWidget surfaceSlider = new SliderWidget(leftCol, row7, panelWidth - 20, 20,
                new LiteralText(surfaceText(cfg)), cfg.surfaceScanChunkRadius / (double) cfg.surfaceScanChunkRadiusMax) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(surfaceText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.surfaceScanChunkRadius = (int) Math.round(this.value * cfg.surfaceScanChunkRadiusMax);
            }
        };
        SliderWidget netherAdjustSlider = new SliderWidget(leftCol, row8, panelWidth - 20, 20,
                new LiteralText(netherAdjustText(cfg)), cfg.netherAdjustRadius / (double) cfg.netherAdjustRadiusMax) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(netherAdjustText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.netherAdjustRadius = (int) Math.round(this.value * cfg.netherAdjustRadiusMax);
            }
        };
        ButtonWidget primaryBtn = new ButtonWidget(leftCol, row15, colWidth, 20, new LiteralText(primaryText(cfg)), btn -> {
            cfg.requirePrimary = !cfg.requirePrimary;
            btn.setMessage(new LiteralText(primaryText(cfg)));
        });
        ButtonWidget throughWallsBtn = new ButtonWidget(rightCol, row15, colWidth, 20, new LiteralText(throughWallsText(cfg)), btn -> {
            cfg.scanThroughWalls = !cfg.scanThroughWalls;
            btn.setMessage(new LiteralText(throughWallsText(cfg)));
        });
        ButtonWidget lockNetherBtn = new ButtonWidget(leftCol, row16, colWidth, 20, new LiteralText(lockNetherText(cfg)), btn -> {
            cfg.lockNetherOrigin = !cfg.lockNetherOrigin;
            btn.setMessage(new LiteralText(lockNetherText(cfg)));
        });
        ButtonWidget trustBtn = new ButtonWidget(rightCol, row16, colWidth, 20, new LiteralText(trustText(cfg)), btn -> {
            cfg.assumeSkyVisibleTrusted = !cfg.assumeSkyVisibleTrusted;
            btn.setMessage(new LiteralText(trustText(cfg)));
        });
        ButtonWidget unknownBtn = new ButtonWidget(leftCol, row17, colWidth, 20, new LiteralText(unknownText(cfg)), btn -> {
            cfg.unknownAsSolid = !cfg.unknownAsSolid;
            btn.setMessage(new LiteralText(unknownText(cfg)));
        });
        ButtonWidget scanBoxBtn = new ButtonWidget(rightCol, row17, colWidth, 20, new LiteralText(scanBoxText(cfg)), btn -> {
            cfg.showScanBox = !cfg.showScanBox;
            btn.setMessage(new LiteralText(scanBoxText(cfg)));
        });
        SliderWidget offsetXSlider = new SliderWidget(leftCol, row9, panelWidth - 20, 20,
                new LiteralText(offsetXText(cfg)), offsetToValue(cfg.netherOffsetX)) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(offsetXText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.netherOffsetX = valueToOffset(this.value);
            }
        };
        SliderWidget offsetYSlider = new SliderWidget(leftCol, row10, panelWidth - 20, 20,
                new LiteralText(offsetYText(cfg)), offsetToValue(cfg.netherOffsetY)) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(offsetYText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.netherOffsetY = valueToOffset(this.value);
            }
        };
        SliderWidget offsetZSlider = new SliderWidget(leftCol, row11, panelWidth - 20, 20,
                new LiteralText(offsetZText(cfg)), offsetToValue(cfg.netherOffsetZ)) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(offsetZText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.netherOffsetZ = valueToOffset(this.value);
            }
        };
        SliderWidget overlaySlider = new SliderWidget(leftCol, row12, panelWidth - 20, 20,
                new LiteralText(overlayText(cfg)), cfg.overlayAlpha) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(overlayText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.overlayAlpha = (float) this.value;
            }
        };
        SliderWidget fillSlider = new SliderWidget(leftCol, row13, panelWidth - 20, 20,
                new LiteralText(fillText(cfg)), cfg.overlayFillAlpha) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(fillText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.overlayFillAlpha = (float) this.value;
            }
        };
        SliderWidget insetSlider = new SliderWidget(leftCol, row14, panelWidth - 20, 20,
                new LiteralText(insetText(cfg)), cfg.overlayInset / overlayInsetMax()) {
            @Override
            protected void updateMessage() {
                setMessage(new LiteralText(insetText(cfg)));
            }

            @Override
            protected void applyValue() {
                cfg.overlayInset = (float) (this.value * overlayInsetMax());
            }
        };
        ButtonWidget closeBtn = new ButtonWidget(rightCol, row17 + rowStep, colWidth, 20, new LiteralText("Close"), btn -> onClose());

        this.addButton(setBtnA);
        this.addButton(setBtnB);
        this.addButton(scanBtn);
        this.addButton(scanNetherBtn);
        this.addButton(planBtn);
        this.addButton(clearScanBtn);
        this.addButton(radiusMinus);
        this.addButton(radiusPlus);
        this.addButton(localScanSlider);
        this.addButton(surfaceSlider);
        this.addButton(netherAdjustSlider);
        this.addButton(primaryBtn);
        this.addButton(throughWallsBtn);
        this.addButton(lockNetherBtn);
        this.addButton(trustBtn);
        this.addButton(unknownBtn);
        this.addButton(scanBoxBtn);
        this.addButton(offsetXSlider);
        this.addButton(offsetYSlider);
        this.addButton(offsetZSlider);
        this.addButton(overlaySlider);
        this.addButton(fillSlider);
        this.addButton(insetSlider);
        this.addButton(closeBtn);

        labelTargetsY = rowStart - 12;
        labelScanY = rowStart + rowStep * 2 - 12;
        labelOffsetsY = rowStart + rowStep * 8 - 12;
        labelOverlayY = rowStart + rowStep * 12 - 12;
        labelTogglesY = rowStart + rowStep * 15 - 12;
    }

    private void setTargetFromFields(boolean isA) {
        TextFieldWidget xf = isA ? xFieldA : xFieldB;
        TextFieldWidget yf = isA ? yFieldA : yFieldB;
        TextFieldWidget zf = isA ? zFieldA : zFieldB;
        Integer x = parseInt(xf.getText());
        Integer y = parseInt(yf.getText());
        Integer z = parseInt(zf.getText());
        if (x == null || y == null || z == null) return;
        if (isA) {
            TargetManager.get().setTargetA(new BlockPos(x, y, z));
        } else {
            TargetManager.get().setTargetB(new BlockPos(x, y, z));
        }
    }

    private void rescan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        ScanManager.get().rescanAroundPlayer(client.world, client.player);
    }

    private void rescanNether() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        ScanManager.get().rescanAroundPlayer(client.world, client.player, false);
    }

    private void clearScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        ScanManager.get().clearDimension(client.world);
    }

    private void plan() {
        PortalPredictorEngine.get().recompute(MinecraftClient.getInstance());
    }

    @Override
    public void onClose() {
        MinecraftClient.getInstance().openScreen(parent);
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (xFieldA.keyPressed(keyCode, scanCode, modifiers) || yFieldA.keyPressed(keyCode, scanCode, modifiers)
                || zFieldA.keyPressed(keyCode, scanCode, modifiers)
                || xFieldB.keyPressed(keyCode, scanCode, modifiers) || yFieldB.keyPressed(keyCode, scanCode, modifiers)
                || zFieldB.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (xFieldA.charTyped(chr, modifiers) || yFieldA.charTyped(chr, modifiers) || zFieldA.charTyped(chr, modifiers)
                || xFieldB.charTyped(chr, modifiers) || yFieldB.charTyped(chr, modifiers)
                || zFieldB.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        fill(matrices, panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC0E0E12);
        fill(matrices, panelX, panelY, panelX + panelWidth, panelY + 22, 0xFF1B1B22);
        textRenderer.drawWithShadow(matrices, new LiteralText("Portal Predictor"), panelX + 10, panelY + 7, 0xFFEFEFEF);
        textRenderer.drawWithShadow(matrices, new LiteralText("Targets"), panelX + 10, labelTargetsY, 0xFF9AD0FF);
        textRenderer.drawWithShadow(matrices, new LiteralText("Scan"), panelX + 10, labelScanY, 0xFF9AD0FF);
        textRenderer.drawWithShadow(matrices, new LiteralText("Nether offsets"), panelX + 10, labelOffsetsY, 0xFF9AD0FF);
        textRenderer.drawWithShadow(matrices, new LiteralText("Overlay"), panelX + 10, labelOverlayY, 0xFF9AD0FF);
        textRenderer.drawWithShadow(matrices, new LiteralText("Toggles"), panelX + 10, labelTogglesY, 0xFF9AD0FF);
        super.render(matrices, mouseX, mouseY, delta);
        PredictorConfig cfg = PredictorConfig.get();
        textRenderer.drawWithShadow(matrices, new LiteralText("Target A (bottom frame)"), panelX + 10, panelY + 46, 0xFFBBBBBB);
        textRenderer.drawWithShadow(matrices, new LiteralText("Target B (optional)"), panelX + 10, panelY + 70, 0xFF888888);
        xFieldA.render(matrices, mouseX, mouseY, delta);
        yFieldA.render(matrices, mouseX, mouseY, delta);
        zFieldA.render(matrices, mouseX, mouseY, delta);
        xFieldB.render(matrices, mouseX, mouseY, delta);
        yFieldB.render(matrices, mouseX, mouseY, delta);
        zFieldB.render(matrices, mouseX, mouseY, delta);
    }

    private static String throughWallsText(PredictorConfig cfg) {
        return "Scan through walls: " + (cfg.scanThroughWalls ? "ON" : "OFF");
    }

    private static String lockNetherText(PredictorConfig cfg) {
        return "Lock nether: " + (cfg.lockNetherOrigin ? "ON" : "OFF");
    }

    private static String primaryText(PredictorConfig cfg) {
        return "Require primary: " + (cfg.requirePrimary ? "ON" : "OFF");
    }

    private static String trustText(PredictorConfig cfg) {
        return "Trust sky-visible: " + (cfg.assumeSkyVisibleTrusted ? "ON" : "OFF");
    }

    private static String scanBoxText(PredictorConfig cfg) {
        return "Show scan box: " + (cfg.showScanBox ? "ON" : "OFF");
    }

    private static String unknownText(PredictorConfig cfg) {
        return "Unknown as solid: " + (cfg.unknownAsSolid ? "ON" : "OFF");
    }

    private static String localScanText(PredictorConfig cfg) {
        return "Local scan radius: " + cfg.nearbyScanRadius;
    }

    private static String surfaceText(PredictorConfig cfg) {
        return "Surface scan chunks: " + cfg.surfaceScanChunkRadius;
    }

    private static String netherAdjustText(PredictorConfig cfg) {
        return "Nether adjust: " + cfg.netherAdjustRadius;
    }

    private static String offsetXText(PredictorConfig cfg) {
        return "Nether offset X: " + cfg.netherOffsetX;
    }

    private static String offsetYText(PredictorConfig cfg) {
        return "Nether offset Y: " + cfg.netherOffsetY;
    }

    private static String offsetZText(PredictorConfig cfg) {
        return "Nether offset Z: " + cfg.netherOffsetZ;
    }

    private static String fillText(PredictorConfig cfg) {
        return "Fill alpha: " + String.format(Locale.ROOT, "%.2f", cfg.overlayFillAlpha);
    }

    private static String overlayText(PredictorConfig cfg) {
        return "Overlay alpha: " + String.format(Locale.ROOT, "%.2f", cfg.overlayAlpha);
    }

    private static String insetText(PredictorConfig cfg) {
        return "Fill inset: " + String.format(Locale.ROOT, "%.2f", cfg.overlayInset);
    }

    private static double overlayInsetMax() {
        return 0.35;
    }

    private static double offsetToValue(int offset) {
        return (offset + offsetMax()) / (double) (offsetMax() * 2);
    }

    private static int valueToOffset(double value) {
        int offset = (int) Math.round(value * (offsetMax() * 2) - offsetMax());
        return Math.max(-offsetMax(), Math.min(offsetMax(), offset));
    }

    private static int offsetMax() {
        return 8;
    }
}
