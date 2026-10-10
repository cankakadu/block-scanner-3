package com.mobinspector.hud;

import com.mobinspector.config.MobInspectorConfig;
import com.mobinspector.core.Inspection;
import com.mobinspector.core.InspectionService;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MobInspectorHud implements HudElement {
    private static final int HEADER = 0, LABEL = 1, VALUE = 2, BIG = 3, MUTED = 4, WARN = 5, DIVIDER = 6;
    private static final int PAD = 8, RADIUS = 4, LINE_H = 9;

    private record Line(String text, int kind, float scale, int gapBefore) {}

    private final List<Line> lines = new ArrayList<>();
    private Inspection last;       // zuletzt angezeigter Inhalt (fuer Fade-out)
    private Inspection built;      // Inhalt, fuer den das Layout gebaut wurde
    private int builtRevision = -1;
    private int boxW, boxH;

    private float alpha, slide, flash;
    private int lastCount = -1;
    private long lastNanos = System.nanoTime();

    @Override
    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MobInspectorConfig cfg = MobInspectorConfig.get();
        if (!cfg.enabled || mc.options.hudHidden || mc.world == null) { alpha = 0; return; }

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        Inspection cur = InspectionService.current();
        boolean animated = cfg.animation > 0;
        if (cur != null) {
            if (last == null || !last.pos().equals(cur.pos())) slide = 1f;
            if (animated && lastCount >= 0 && sig(cur) != lastCount && last != null
                    && last.pos().equals(cur.pos())) flash = 1f;
            if (animated && last != null && last.pos().equals(cur.pos()) && last.mode() != cur.mode())
                alpha = Math.min(alpha, 0.35f); // sanftes Einblenden beim Moduswechsel
            lastCount = sig(cur);
            last = cur;
        } else {
            lastCount = -1;
        }

        float target = cur != null ? 1f : 0f;
        if (animated) {
            float k = 1f - (float) Math.exp(-dt * 16f);
            alpha += (target - alpha) * k;
            slide = Math.max(0f, slide - dt * 6f);
            flash = Math.max(0f, flash - dt * 3.5f);
            if (Math.abs(target - alpha) < 0.01f) alpha = target;
        } else {
            alpha = target; slide = 0f; flash = 0f;
        }
        if (last == null || alpha <= 0.01f) return;

        TextRenderer tr = mc.textRenderer;
        if (built != last || builtRevision != cfg.revision) rebuild(last, cfg, tr);

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        float sc = cfg.scale;
        float pxW = boxW * sc, pxH = boxH * sc;
        boolean right = cfg.anchor == 1 || cfg.anchor == 3;
        boolean bottom = cfg.anchor == 2 || cfg.anchor == 3;
        float x = right ? sw - cfg.offsetX - pxW : cfg.offsetX;
        float y = bottom ? sh - cfg.offsetY - pxH : cfg.offsetY;
        x = Math.max(0, Math.min(sw - pxW, x));
        y = Math.max(0, Math.min(sh - pxH, y));
        float slideAmt = cfg.animation == 2 ? 14f : cfg.animation == 1 ? 6f : 0f;
        y += (bottom ? 1 : -1) * slide * slideAmt * (1f);

        int accent = cfg.accentRgb();
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(sc, sc);

        // Panel
        float bgA = cfg.opacity / 100f * alpha;
        roundedRect(ctx, 0, 0, boxW, boxH, RADIUS, argb(accent, 0.38f * alpha));
        roundedRect(ctx, 1, 1, boxW - 2, boxH - 2, RADIUS - 1, argb(0x0B0F16, bgA));
        ctx.fillGradient(PAD / 2 - 1, RADIUS + 1, PAD / 2 + 1, boxH - RADIUS - 1,
                argb(accent, 0.95f * alpha), argb(accent, 0.05f * alpha));

        // Inhalt
        int cy = PAD;
        for (Line l : lines) {
            cy += l.gapBefore();
            int color = colorFor(l, accent);
            if (l.kind() == DIVIDER) {
                ctx.fill(PAD + 3, cy, boxW - PAD, cy + 1, argb(0xFFFFFF, 0.10f * alpha));
            } else if ((color >>> 24) >= 6) {
                if (l.scale() == 1f) {
                    ctx.drawText(tr, textFor(l), PAD + 3, cy, color, l.kind() != LABEL);
                } else {
                    ctx.getMatrices().pushMatrix();
                    ctx.getMatrices().translate(PAD + 3, cy);
                    ctx.getMatrices().scale(l.scale(), l.scale());
                    ctx.drawText(tr, textFor(l), 0, 0, color, l.kind() != LABEL);
                    ctx.getMatrices().popMatrix();
                }
            }
            cy += Math.round(LINE_H * l.scale());
        }
        ctx.getMatrices().popMatrix();
    }

    private static int sig(Inspection i) { return i.mobCount() + 1000 * i.insideCount(); }

    private Text textFor(Line l) {
        Text t = Text.literal(l.text());
        return (l.kind() == HEADER || l.kind() == BIG) ? t.copy().formatted(Formatting.BOLD) : t;
    }

    private int colorFor(Line l, int accent) {
        return switch (l.kind()) {
            case HEADER -> argb(accent, alpha);
            case LABEL -> argb(0x8B95A7, alpha);
            case VALUE -> argb(0xF1F5F9, alpha);
            case BIG -> argb(lerp(accent, 0xFFFFFF, flash), alpha);
            case WARN -> argb(0xFBBF24, alpha);
            default -> argb(0x6B7587, alpha);
        };
    }

    private void rebuild(Inspection in, MobInspectorConfig cfg, TextRenderer tr) {
        lines.clear();
        lines.add(new Line("MOB INSPECTOR", HEADER, 1f, 0));
        if (cfg.showTarget) {
            lines.add(new Line("TARGET", LABEL, 0.75f, 7));
            lines.add(new Line(in.blockName().toUpperCase(Locale.ROOT), VALUE, 1f, 1));
        }
        boolean onBlock = in.mode() != 1 && cfg.showMobCount && in.mobCount() >= 0;
        boolean insideEnt = in.mode() != 0 && cfg.showMobCount && in.insideCount() >= 0;
        boolean spawnerData = in.mode() != 0 && cfg.showBlockData;
        boolean inBlock = insideEnt || spawnerData;
        if (onBlock) {
            addCountSection(in.mobCount(), in.breakdown(), "MOBS ON BLOCK", 7, in, cfg);
        }
        if (onBlock && inBlock) lines.add(new Line("", DIVIDER, 0.12f, 8));
        if (inBlock) {
            int gap = onBlock ? 6 : 7;
            if (insideEnt) {
                addCountSection(in.insideCount(), in.insideBreakdown(), "ENTITIES INSIDE BLOCK", gap, in, cfg);
                gap = 8;
            }
            Inspection.BlockMobData d = in.blockData();
            if (spawnerData) {
                if (d == null) {
                    lines.add(new Line("BLOCK DATA", LABEL, 0.75f, gap));
                    lines.add(new Line("No Mob Data", MUTED, 0.85f, 1));
                } else {
                    lines.add(new Line(d.kind().toUpperCase(Locale.ROOT), VALUE, 1f, gap));
                    lines.add(new Line("SPAWN ENTITY", LABEL, 0.75f, 5));
                    lines.add(d.spawnEntity() != null
                            ? new Line(d.spawnEntity(), VALUE, 1f, 1)
                            : new Line("Unknown (not synced)", WARN, 0.85f, 1));
                    lines.add(new Line("STORED MOBS", LABEL, 0.75f, 5));
                    lines.add(new Line("N/A", MUTED, 1f, 1));
                    for (Inspection.Field f : d.fields())
                        lines.add(new Line(f.label() + ": " + f.value(), MUTED, 0.75f, 2));
                }
            }
        }

        int w = 112, h = PAD;
        for (Line l : lines) {
            w = Math.max(w, Math.round(tr.getWidth(l.text()) * l.scale()) + PAD * 2 + 3);
            h += l.gapBefore() + Math.round(LINE_H * l.scale());
        }
        boxW = w;
        boxH = h + PAD;
        built = in;
        builtRevision = cfg.revision;
    }

    private void addCountSection(int count, List<Inspection.Entry> breakdown, String label, int gap,
                                 Inspection in, MobInspectorConfig cfg) {
        lines.add(new Line(label, LABEL, 0.75f, gap));
        lines.add(new Line(String.format(Locale.ROOT, "%02d", count), BIG, 2f, 1));
        if (cfg.showBreakdown && !breakdown.isEmpty()) {
            int shown = Math.min(4, breakdown.size());
            for (int i = 0; i < shown; i++) {
                Inspection.Entry e = breakdown.get(i);
                lines.add(new Line(e.name() + "  \u00d7" + e.count(), VALUE, 0.85f, i == 0 ? 3 : 1));
            }
            if (breakdown.size() > shown)
                lines.add(new Line("+" + (breakdown.size() - shown) + " more", MUTED, 0.75f, 1));
        }
        if (in.multiplayer())
            lines.add(new Line("Client-loaded entities only", MUTED, 0.7f, 4));
    }

    // ---- Zeichen-Helfer ----
    private static void roundedRect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    private static int argb(int rgb, float a) {
        int ai = Math.max(0, Math.min(255, (int) (a * 255f)));
        return (ai << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
