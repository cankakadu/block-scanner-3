package com.mobinspector.screen;

import com.mobinspector.config.MobInspectorConfig;
import com.mobinspector.input.MobInspectorKeybindings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final MobInspectorConfig cfg = MobInspectorConfig.get();

    private static final int SEG_GAP = 4, SEG_Y = 50;
    private final boolean showNotice;
    private long noticeAt;
    private final ButtonWidget[] modeButtons = new ButtonWidget[3];
    private int segLeft, segW;
    private float indicatorX;
    private long lastNanos = System.nanoTime();

    public SettingsScreen(Screen parent) { this(parent, false); }

    private SettingsScreen(Screen parent, boolean showNotice) {
        super(Text.translatable("screen.mobinspector.title"));
        this.parent = parent;
        this.showNotice = showNotice;
        this.noticeAt = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        int colW = 156, gap = 8;
        int left = width / 2 - colW - gap / 2, right = width / 2 + gap / 2;
        int y0 = 78, step = 22;

        // Segment-Schalter: Mob Detection Mode
        segLeft = left;
        segW = (colW * 2 + gap - 2 * SEG_GAP) / 3;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            modeButtons[i] = ButtonWidget.builder(Text.empty(), b -> {
                cfg.detectionMode = idx;
                touch();
                refreshModeLabels();
            }).dimensions(segLeft + i * (segW + SEG_GAP), SEG_Y, segW, 20).build();
            addDrawableChild(modeButtons[i]);
        }
        refreshModeLabels();
        indicatorX = segLeft + cfg.detectionMode * (segW + SEG_GAP);

        toggle(left, y0, colW, "enabled", () -> cfg.enabled, v -> cfg.enabled = v);
        toggle(left, y0 + step, colW, "target", () -> cfg.showTarget, v -> cfg.showTarget = v);
        toggle(left, y0 + step * 2, colW, "mobcount", () -> cfg.showMobCount, v -> cfg.showMobCount = v);
        toggle(left, y0 + step * 3, colW, "breakdown", () -> cfg.showBreakdown, v -> cfg.showBreakdown = v);
        toggle(left, y0 + step * 4, colW, "blockdata", () -> cfg.showBlockData, v -> cfg.showBlockData = v);
        cycle(left, y0 + step * 5, colW, "filter", MobInspectorConfig.FILTER_NAMES,
                () -> cfg.countFilter, v -> cfg.countFilter = v);
        cycle(left, y0 + step * 6, colW, "footprint", MobInspectorConfig.AREA_NAMES,
                () -> cfg.footprintMode ? 1 : 0, v -> cfg.footprintMode = v == 1);

        cycle(right, y0, colW, "accent", MobInspectorConfig.ACCENT_NAMES, () -> cfg.accent, v -> cfg.accent = v);
        cycle(right, y0 + step, colW, "anchor", MobInspectorConfig.ANCHOR_NAMES, () -> cfg.anchor, v -> cfg.anchor = v);
        cycle(right, y0 + step * 2, colW, "animation", MobInspectorConfig.ANIM_NAMES, () -> cfg.animation, v -> cfg.animation = v);
        addDrawableChild(new ValueSlider(right, y0 + step * 3, colW, "scale", 0.5, 2.0, cfg.scale, "x", false,
                v -> { cfg.scale = (float) v; touch(); }));
        addDrawableChild(new ValueSlider(right, y0 + step * 4, colW, "opacity", 10, 100, cfg.opacity, "%", true,
                v -> { cfg.opacity = (int) v; touch(); }));
        addDrawableChild(new ValueSlider(right, y0 + step * 5, colW, "offx", 0, 400, cfg.offsetX, "px", true,
                v -> { cfg.offsetX = (int) v; touch(); }));
        addDrawableChild(new ValueSlider(right, y0 + step * 6, colW, "offy", 0, 400, cfg.offsetY, "px", true,
                v -> { cfg.offsetY = (int) v; touch(); }));

        int bottomY = Math.min(height - 28, y0 + step * 7 + 2);
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset Settings").formatted(Formatting.RED),
                b -> confirmReset()).dimensions(left, bottomY, colW, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
                .dimensions(right, bottomY, colW, 20).build());
    }

    private void refreshModeLabels() {
        for (int i = 0; i < 3; i++) {
            Text t = Text.literal(MobInspectorConfig.MODE_NAMES[i]);
            modeButtons[i].setMessage(i == cfg.detectionMode ? t.copy().formatted(Formatting.BOLD)
                    : t.copy().formatted(Formatting.GRAY));
        }
    }

    private void confirmReset() {
        if (client == null) return;
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                cfg.resetToDefaults();   // setzt + speichert nur mobinspector.json
                MobInspectorKeybindings.resetToDefaults(client);
                client.setScreen(new SettingsScreen(parent, true)); // frisch aufbauen -> Widgets zeigen Defaults
            } else {
                client.setScreen(this);
            }
        }, Text.literal("Reset Settings"),
                Text.literal("Do you really want to reset all Mob Inspector settings to their default values?"),
                Text.literal("Reset"), Text.literal("Cancel")));
    }

    private void touch() { cfg.revision++; }

    private Text label(String key, Text value) {
        return Text.translatable("option.mobinspector." + key).append(": ").append(value);
    }

    private void toggle(int x, int y, int w, String key, BooleanSupplier get, Consumer<Boolean> set) {
        ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(key, get.getAsBoolean() ? ScreenTexts.ON : ScreenTexts.OFF));
            touch();
        }).dimensions(x, y, w, 20).build();
        btn.setMessage(label(key, get.getAsBoolean() ? ScreenTexts.ON : ScreenTexts.OFF));
        addDrawableChild(btn);
    }

    private void cycle(int x, int y, int w, String key, String[] names, IntSupplier get, IntConsumer set) {
        ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
            set.accept((get.getAsInt() + 1) % names.length);
            b.setMessage(label(key, Text.literal(names[get.getAsInt()])));
            touch();
        }).dimensions(x, y, w, 20).build();
        btn.setMessage(label(key, Text.literal(names[get.getAsInt()])));
        addDrawableChild(btn);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x55000000); // leichtes Abdunkeln ohne Blur: HUD bleibt als Live-Vorschau sichtbar
        int accent = 0xFF000000 | cfg.accentRgb();
        ctx.fill(width / 2 - 170, 8, width / 2 + 170, 32, 0xAA0B0F16);
        ctx.fill(width / 2 - 170, 31, width / 2 + 170, 32, accent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFFFF);
        ctx.drawText(textRenderer, Text.literal("MOB DETECTION MODE"), segLeft, SEG_Y - 11, 0xFF8B95A7, true);

        // Animierter Auswahl-Indikator ueber dem Segment-Schalter
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        float tx = segLeft + cfg.detectionMode * (segW + SEG_GAP);
        indicatorX = cfg.animation > 0 ? indicatorX + (tx - indicatorX) * (1f - (float) Math.exp(-dt * 18f)) : tx;
        int x = Math.round(indicatorX), rgb = cfg.accentRgb(), solid = 0xFF000000 | rgb;
        ctx.fill(x, SEG_Y, x + segW, SEG_Y + 20, 0x33000000 | rgb);
        ctx.fill(x, SEG_Y, x + segW, SEG_Y + 1, solid);
        ctx.fill(x, SEG_Y + 19, x + segW, SEG_Y + 20, solid);
        ctx.fill(x, SEG_Y, x + 1, SEG_Y + 20, solid);
        ctx.fill(x + segW - 1, SEG_Y, x + segW, SEG_Y + 20, solid);

        if (showNotice) {
            long age = System.currentTimeMillis() - noticeAt;
            if (age < 2500) {
                int a = (int) (255 * Math.min(1f, (2500 - age) / 600f));
                if (a >= 8) ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Settings reset successfully"),
                        width / 2, height - 12, (a << 24) | 0x3DDC97);
            }
        }
    }

    @Override
    public void removed() { cfg.save(); }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private final class ValueSlider extends SliderWidget {
        private final String key, suffix;
        private final double min, max;
        private final boolean integer;
        private final DoubleConsumer setter;

        ValueSlider(int x, int y, int w, String key, double min, double max, double cur,
                    String suffix, boolean integer, DoubleConsumer setter) {
            super(x, y, w, 20, Text.empty(), (cur - min) / (max - min));
            this.key = key; this.min = min; this.max = max;
            this.suffix = suffix; this.integer = integer; this.setter = setter;
            updateMessage();
        }

        private double current() {
            double v = min + value * (max - min);
            return integer ? Math.round(v) : Math.round(v * 20.0) / 20.0;
        }

        @Override
        protected void updateMessage() {
            if (key == null) return; // wird vom Super-Konstruktor aufgerufen, bevor Felder gesetzt sind
            double v = current();
            String s = integer ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.2f", v);
            setMessage(label(key, Text.literal(s + (suffix.equals("x") ? "x" : suffix))));
        }

        @Override
        protected void applyValue() { setter.accept(current()); }
    }
}
