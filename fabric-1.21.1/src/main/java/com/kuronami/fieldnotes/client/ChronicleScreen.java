package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.data.ChronicleEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom Screen displaying the player's chronicle entries.
 *
 * <p>Layout:
 * <ul>
 *   <li>Top: title + entry count + filter input + frame-type filter button</li>
 *   <li>Middle: scrollable list of entries (newest first)</li>
 *   <li>Bottom: close button</li>
 * </ul>
 *
 * <p>This is the MVP — no per-entry detail panel yet, just the chronological list.
 */
public final class ChronicleScreen extends Screen {

    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);

    private static final int ROW_HEIGHT = 36;
    private static final int LIST_PADDING = 8;

    private final List<ChronicleEntry> source;
    private List<ChronicleEntry> filtered;

    private EditBox searchBox;
    private Button frameFilterBtn;
    private FrameFilter frameFilter = FrameFilter.ALL;

    private int scroll = 0;

    public ChronicleScreen(List<ChronicleEntry> entries) {
        super(Component.literal("Field Notes"));
        this.source = new ArrayList<>(entries);
        // newest first
        this.source.sort((a, b) -> Long.compare(b.epochMillis(), a.epochMillis()));
        this.filtered = this.source;
    }

    @Override
    protected void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 30, 200, 18,
                Component.literal("search"));
        this.searchBox.setHint(Component.literal("Search title / advancement..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        this.frameFilterBtn = Button.builder(
                Component.literal("Filter: " + frameFilter.label()),
                b -> {
                    frameFilter = frameFilter.next();
                    b.setMessage(Component.literal("Filter: " + frameFilter.label()));
                    applyFilters();
                })
                .bounds(this.width / 2 + 110, 28, 110, 22)
                .build();
        this.addRenderableWidget(this.frameFilterBtn);

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 22)
                .build());
    }

    private void onSearchChanged(String s) {
        applyFilters();
    }

    private void applyFilters() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";
        List<ChronicleEntry> out = new ArrayList<>();
        for (ChronicleEntry e : source) {
            if (!frameFilter.accept(e)) continue;
            if (!q.isEmpty()) {
                boolean hit = e.title().toLowerCase(Locale.ROOT).contains(q)
                        || e.advancementId().toLowerCase(Locale.ROOT).contains(q)
                        || e.description().toLowerCase(Locale.ROOT).contains(q);
                if (!hit) continue;
            }
            out.add(e);
        }
        this.filtered = out;
        this.scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // background
        g.fill(0, 0, this.width, this.height, 0xC0101018);

        super.render(g, mouseX, mouseY, partial);

        // title
        String title = "Field Notes  —  " + filtered.size() + " / " + source.size();
        g.drawCenteredString(this.font, title, this.width / 2, 12, 0xFFFFFFFF);

        // list area
        int listTop = 60;
        int listBottom = this.height - 36;
        g.fill(this.width / 2 - 200, listTop, this.width / 2 + 200, listBottom, 0x80000000);

        int visible = (listBottom - listTop) / ROW_HEIGHT;
        int startIdx = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - visible)));

        for (int i = 0; i < visible && startIdx + i < filtered.size(); i++) {
            ChronicleEntry e = filtered.get(startIdx + i);
            int y = listTop + LIST_PADDING + i * ROW_HEIGHT;
            renderRow(g, e, this.width / 2 - 195, y, 390);
        }

        if (filtered.isEmpty()) {
            g.drawCenteredString(this.font,
                    "(no entries) — earn an advancement to begin chronicling",
                    this.width / 2, listTop + 20, 0xFF888888);
        }
    }

    private void renderRow(GuiGraphics g, ChronicleEntry e, int x, int y, int w) {
        // title line
        ChatFormatting frameColor = switch (e.frameType()) {
            case "challenge" -> ChatFormatting.LIGHT_PURPLE;
            case "goal"      -> ChatFormatting.AQUA;
            default          -> ChatFormatting.WHITE;
        };
        g.drawString(this.font,
                Component.literal("[" + e.frameType().toUpperCase(Locale.ROOT) + "] ")
                        .withStyle(frameColor)
                        .append(Component.literal(e.title()).withStyle(ChatFormatting.WHITE)),
                x, y, 0xFFFFFFFF);

        // meta line: timestamp, dimension, biome, coords
        String when = TS_FMT.format(new Date(e.epochMillis()));
        String biome = e.biomeId().isEmpty() ? "?" : shortKey(e.biomeId());
        String dim = shortKey(e.dimensionId());
        String meta = String.format(Locale.ROOT, "%s · %s · %s · (%d, %d, %d)",
                when, dim, biome, e.x(), e.y(), e.z());
        g.drawString(this.font, meta, x, y + 12, 0xFFAAAAAA);

        // separator
        g.fill(x, y + 28, x + w, y + 29, 0x40FFFFFF);
    }

    /** "minecraft:plains" -> "plains" */
    private static String shortKey(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int newScroll = scroll - (int) Math.signum(scrollY);
        scroll = Math.max(0, Math.min(filtered.size() - 1, newScroll));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Cycle: ALL → MILESTONE → CHALLENGE → ALL */
    private enum FrameFilter {
        ALL("All", e -> true),
        MILESTONE("Goals & Challenges", ChronicleEntry::isMilestone),
        CHALLENGE("Challenges only", e -> "challenge".equals(e.frameType()));

        final String label;
        final java.util.function.Predicate<ChronicleEntry> pred;

        FrameFilter(String label, java.util.function.Predicate<ChronicleEntry> pred) {
            this.label = label;
            this.pred = pred;
        }

        boolean accept(ChronicleEntry e) { return pred.test(e); }
        String label()                  { return label; }

        FrameFilter next() {
            FrameFilter[] vs = values();
            return vs[(this.ordinal() + 1) % vs.length];
        }
    }
}
