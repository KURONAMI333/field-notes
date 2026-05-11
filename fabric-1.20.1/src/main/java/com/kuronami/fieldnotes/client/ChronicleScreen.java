package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.data.ChronicleEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Field Notes — spread-page chronicle viewer.
 *
 * <p>Layout follows the Patchouli convention (the canonical reference for
 * modded book UIs): a 272×180 spread with two 116×156 pages and a 10-pixel
 * gutter. The texture itself ships under fieldnotes:textures/gui/book_spread.png
 * — a sepia parchment with central stitching, easily swappable by anyone
 * using the MineTexture recolor tool.
 *
 * <p>Per page: 2 entries (no. + 2-line title + coords + biome + datetime).
 * Per spread: 4 entries. Buttons sit outside the book on the lower edge
 * (Patchouli's pattern; keeps the page surface free of UI clutter).
 */
public final class ChronicleScreen extends Screen {

    // ── Spread geometry: Patchouli book_brown.png ─────────────────────
    // Patchouli's brown book texture is a proper spread (one image, two pages,
    // central stitching, curled corners). We use Patchouli's canonical layout
    // numbers so the text lines up with the artwork.
    private static final ResourceLocation BOOK_TEX =
            new ResourceLocation("patchouli", "textures/gui/book_brown.png");
    private static final int BOOK_WIDTH = 272;
    private static final int BOOK_HEIGHT = 180;
    private static final int TEX_SHEET = 512;
    private static final int TEX_SHEET_H = 256;
    private static final int LEFT_PAGE_X = 15;
    private static final int RIGHT_PAGE_X = 141;
    private static final int PAGE_WIDTH = 116;
    private static final int PAGE_HEIGHT = 156;
    private static final int PAGE_TOP_PADDING = 18;
    private static final int LINE_HEIGHT = 9;

    // ── Per-entry layout ──────────────────────────────────────────────
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int ENTRY_TEXT_X_OFFSET = ICON_SIZE + ICON_GAP;
    private static final int ENTRY_TEXT_WIDTH = PAGE_WIDTH - ENTRY_TEXT_X_OFFSET - 4;
    private static final int LINES_PER_ENTRY = 6;    // no. + title×2 + coords + biome + datetime
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int ENTRIES_PER_SPREAD = ENTRIES_PER_PAGE * 2;

    // ── Ink palette (sepia journal) ───────────────────────────────────
    private static final int INK_PRIMARY = 0xFF000000;
    private static final int INK_NUMBER = 0xFF8B6B3D;
    private static final int INK_SECONDARY = 0xFF5C4A2E;
    private static final int INK_TERTIARY = 0xFF8B7355;
    private static final int INK_RULE = 0x408B6B3D;

    /** Real-world wall clock — "2026.05.11 09:01". */
    private static final SimpleDateFormat WALL_CLOCK_FMT =
            new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.ROOT);

    private final List<ChronicleEntry> source;
    private List<ChronicleEntry> filtered;

    private int currentSpread = 0;
    private int totalSpreads = 1;
    private int bookLeft;
    private int bookTop;

    private EditBox searchBox;
    private Button frameFilterBtn;
    private PageButton prevBtn;
    private PageButton nextBtn;

    private FrameFilter frameFilter = FrameFilter.ALL;

    public ChronicleScreen(List<ChronicleEntry> entries) {
        super(Component.translatable("fieldnotes.screen.title"));
        this.source = new ArrayList<>(entries);
        this.source.sort((a, b) -> Long.compare(b.epochMillis(), a.epochMillis()));
        this.filtered = this.source;
        recomputePages();
    }

    private void recomputePages() {
        this.totalSpreads = Math.max(1,
                (filtered.size() + ENTRIES_PER_SPREAD - 1) / ENTRIES_PER_SPREAD);
        if (currentSpread >= totalSpreads) currentSpread = totalSpreads - 1;
    }

    @Override
    protected void init() {
        this.bookLeft = (this.width - BOOK_WIDTH) / 2;
        this.bookTop = 4;

        // Page-flip arrows — vanilla PageButton (the curled-arrow sprite from
        // written_book). Placed at the bottom corners of each page so a turn
        // gesture reads as "flip this page". Sprite is 23×13.
        int arrowY = bookTop + BOOK_HEIGHT - 22;
        this.prevBtn = new PageButton(
                bookLeft + LEFT_PAGE_X + 5, arrowY,
                false, b -> turnSpread(-1), true);
        this.nextBtn = new PageButton(
                bookLeft + RIGHT_PAGE_X + PAGE_WIDTH - 28, arrowY,
                true, b -> turnSpread(1), true);
        this.addRenderableWidget(this.prevBtn);
        this.addRenderableWidget(this.nextBtn);

        // Search + filter + Done strip below the spread
        int controlsY = bookTop + BOOK_HEIGHT + 12;
        this.searchBox = new EditBox(this.font,
                this.width / 2 - 120, controlsY,
                160, 18,
                Component.translatable("fieldnotes.screen.search"));
        this.searchBox.setHint(Component.translatable("fieldnotes.screen.search.hint"));
        this.searchBox.setResponder(s -> applyFilters());
        this.addRenderableWidget(this.searchBox);

        this.frameFilterBtn = Button.builder(
                Component.literal(frameFilter.label()),
                b -> {
                    frameFilter = frameFilter.next();
                    b.setMessage(Component.literal(frameFilter.label()));
                    applyFilters();
                })
                .bounds(this.width / 2 + 44, controlsY, 76, 18)
                .build();
        this.addRenderableWidget(this.frameFilterBtn);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> this.onClose())
                .bounds(this.width / 2 - 100, controlsY + 24, 200, 20)
                .build());

        this.setInitialFocus(this.searchBox);
        updatePageButtons();
    }

    private void turnSpread(int delta) {
        int next = currentSpread + delta;
        if (next < 0 || next >= totalSpreads) return;
        currentSpread = next;
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (prevBtn != null) prevBtn.visible = currentSpread > 0;
        if (nextBtn != null) nextBtn.visible = currentSpread < totalSpreads - 1;
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
        this.currentSpread = 0;
        recomputePages();
        updatePageButtons();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // 1. Spread background — single Patchouli book_brown.png blit
        g.blit(BOOK_TEX, bookLeft, bookTop, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, TEX_SHEET, TEX_SHEET_H);

        // 2. Headers — book title on the left page, spread indicator on the right
        Component bookTitle = Component.literal("Field Notes")
                .withStyle(Style.EMPTY.withItalic(true).withColor(INK_NUMBER));
        g.drawString(this.font, bookTitle,
                bookLeft + LEFT_PAGE_X, bookTop + 8,
                INK_PRIMARY, false);

        Component pageLabel = Component.translatable("fieldnotes.screen.page",
                currentSpread + 1, totalSpreads);
        int pageW = this.font.width(pageLabel);
        g.drawString(this.font, pageLabel,
                bookLeft + RIGHT_PAGE_X + PAGE_WIDTH - pageW,
                bookTop + 8,
                INK_PRIMARY, false);

        // 3. Body — render each page
        if (filtered.isEmpty()) {
            renderEmptyState(g);
        } else {
            int startIdx = currentSpread * ENTRIES_PER_SPREAD;
            // Left page (entries 0, 1 of this spread)
            renderPageEntries(g, startIdx, ENTRIES_PER_PAGE,
                    bookLeft + LEFT_PAGE_X, bookTop + PAGE_TOP_PADDING);
            // Right page (entries 2, 3 of this spread)
            renderPageEntries(g, startIdx + ENTRIES_PER_PAGE, ENTRIES_PER_PAGE,
                    bookLeft + RIGHT_PAGE_X, bookTop + PAGE_TOP_PADDING);
        }

        // 4. Widgets — arrows, search, filter, done — on top of the book
        super.render(g, mouseX, mouseY, partial);
    }

    private void renderEmptyState(GuiGraphics g) {
        Component empty = Component.translatable("fieldnotes.screen.empty");
        List<FormattedCharSequence> lines = this.font.split(empty, ENTRY_TEXT_WIDTH);
        int y = bookTop + PAGE_TOP_PADDING + PAGE_HEIGHT / 2 - lines.size() * LINE_HEIGHT / 2;
        for (FormattedCharSequence line : lines) {
            int lw = this.font.width(line);
            g.drawString(this.font, line,
                    bookLeft + LEFT_PAGE_X + (PAGE_WIDTH - lw) / 2,
                    y, INK_SECONDARY, false);
            y += LINE_HEIGHT;
        }
    }

    private void renderPageEntries(GuiGraphics g, int startIdx, int count,
                                   int pageX, int pageY) {
        int endIdx = Math.min(startIdx + count, filtered.size());
        int shown = endIdx - startIdx;
        if (shown <= 0) return;

        // Vertically center the entry block on the page so the bottom space
        // doesn't feel empty — and add an air-gap between entries.
        int entryHeight = LINES_PER_ENTRY * LINE_HEIGHT;
        int entryGap = 12;
        int blockHeight = shown * entryHeight + (shown - 1) * entryGap;
        // Available page-body height: PAGE_HEIGHT minus the bottom area
        // reserved for the page-flip arrow (about 22px).
        int bodyHeight = PAGE_HEIGHT - 24;
        int topPad = Math.max(0, (bodyHeight - blockHeight) / 2);
        int contentY = pageY + topPad;

        for (int i = startIdx; i < endIdx; i++) {
            ChronicleEntry e = filtered.get(i);
            int entryNumber = filtered.size() - i;
            int entryY = contentY + (i - startIdx) * (entryHeight + entryGap);
            renderEntry(g, this.font, e, entryNumber, pageX, entryY);

            // Divider between entries on the same page
            if (i < endIdx - 1) {
                drawRule(g, pageX,
                        entryY + entryHeight + entryGap / 2 - 1,
                        PAGE_WIDTH - 4);
            }
        }
    }

    private static void drawRule(GuiGraphics g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 1, INK_RULE);
    }

    private void renderEntry(GuiGraphics g, Font font, ChronicleEntry e, int entryNo,
                             int x, int y) {
        // ── 16×16 icon ────────────────────────────────────────────────
        ItemStack icon = resolveIcon(e.iconItemId());
        if (!icon.isEmpty()) {
            g.renderItem(icon, x, y);
        }

        int textX = x + ENTRY_TEXT_X_OFFSET;

        // ── Line 1: "No. 42" ──────────────────────────────────────────
        Component numberLine = Component.literal("No. " + entryNo)
                .withStyle(Style.EMPTY.withColor(INK_NUMBER).withItalic(true));
        g.drawString(font, numberLine, textX, y, INK_NUMBER, false);

        // ── Line 2-3: title (max 2 lines, bold, frame-colored) ────────
        int titleColor = switch (e.frameType()) {
            case "challenge" -> 0xFF8B2B6C;
            case "goal"      -> 0xFF2B5C8B;
            default          -> INK_PRIMARY;
        };
        MutableComponent titleOnly = Component.literal(e.title())
                .withStyle(Style.EMPTY.withColor(titleColor).withBold(true));
        List<FormattedCharSequence> wrapped = font.split(titleOnly, ENTRY_TEXT_WIDTH);
        int titleLinesShown = Math.min(2, Math.max(1, wrapped.size()));
        for (int i = 0; i < titleLinesShown; i++) {
            FormattedCharSequence seq = i < wrapped.size()
                    ? wrapped.get(i)
                    : titleOnly.getVisualOrderText();
            g.drawString(font, seq, textX, y + LINE_HEIGHT + i * LINE_HEIGHT,
                    INK_PRIMARY, false);
        }

        // Predictable meta baseline regardless of title length
        int metaY = y + LINE_HEIGHT + 2 * LINE_HEIGHT;

        // ── Line 4: coordinates ───────────────────────────────────────
        String coords = String.format(Locale.ROOT, "(%d, %d, %d)", e.x(), e.y(), e.z());
        g.drawString(font, coords, textX, metaY, INK_SECONDARY, false);

        // ── Line 5: biome (vanilla-translated) + day ──────────────────
        Component biomeLine = buildNarrative(e);
        FormattedCharSequence biomeSeq = font.split(biomeLine, ENTRY_TEXT_WIDTH).stream()
                .findFirst().orElse(biomeLine.getVisualOrderText());
        g.drawString(font, biomeSeq, textX, metaY + LINE_HEIGHT, INK_SECONDARY, false);

        // ── Line 6: real-world date & time ────────────────────────────
        String dateTime = WALL_CLOCK_FMT.format(new Date(e.epochMillis()));
        g.drawString(font, dateTime, textX, metaY + 2 * LINE_HEIGHT, INK_TERTIARY, false);
    }

    private Component buildNarrative(ChronicleEntry e) {
        MutableComponent line = Component.literal("Day " + e.worldDay());
        if (!e.biomeId().isEmpty()) {
            line.append(Component.literal(" · "))
                    .append(translatedRegistryName("biome", e.biomeId()));
        }
        if (!e.dimensionId().isEmpty() && !"minecraft:overworld".equals(e.dimensionId())) {
            line.append(Component.literal(" · "))
                    .append(translatedRegistryName("dimension", e.dimensionId()));
        }
        return line;
    }

    private static Component translatedRegistryName(String prefix, String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            String key = prefix + "." + rl.getNamespace() + "." + rl.getPath();
            return Component.translatable(key);
        } catch (Exception ignored) {
            return Component.literal(prettifyKey(id));
        }
    }

    private ItemStack resolveIcon(String iconItemId) {
        if (iconItemId == null || iconItemId.isEmpty()) return ItemStack.EMPTY;
        try {
            ResourceLocation rl = new ResourceLocation(iconItemId);
            Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String prettifyKey(String id) {
        int colon = id.indexOf(':');
        String body = colon >= 0 ? id.substring(colon + 1) : id;
        body = body.replace('_', ' ');
        StringBuilder out = new StringBuilder(body.length());
        boolean capNext = true;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == ' ') {
                out.append(' ');
                capNext = true;
            } else if (capNext) {
                out.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (scrollAmount > 0) turnSpread(-1);
        else if (scrollAmount < 0) turnSpread(1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            turnSpread(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            turnSpread(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Suppress the default pause-screen blur. 1.20.1 only takes one parameter
     * (the others were added in 1.20.6+); the override still no-ops.
     */
    @Override
    public void renderBackground(GuiGraphics g) {
        // intentionally empty
    }

    /** Cycle: ALL → MILESTONE → CHALLENGE → ALL */
    private enum FrameFilter {
        ALL("Filter: All", e -> true),
        MILESTONE("Filter: ◆✦", ChronicleEntry::isMilestone),
        CHALLENGE("Filter: ✦", e -> "challenge".equals(e.frameType()));

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
