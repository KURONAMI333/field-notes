package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.data.ChronicleEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Field Notes — vanilla-book-styled chronicle viewer.
 *
 * <p>Visual baseline: vanilla {@code minecraft:textures/gui/book.png} (192×192
 * book region rendered onto the standard 256×256 GUI texture). Each page shows
 * up to {@link #ENTRIES_PER_PAGE} chronicle entries; readers paginate with
 * vanilla {@link PageButton}s, scroll wheel, or left/right arrow keys.
 *
 * <p>Top strip hosts a thin search box and a frame-type filter button so the
 * search affordance stays inside the book aesthetic without dominating the
 * page surface.
 */
public final class ChronicleScreen extends Screen {

    // ── vanilla book GUI geometry (matches BookViewScreen) ────────────
    private static final ResourceLocation BOOK_TEX =
            ResourceLocation.parse("minecraft:textures/gui/book.png");
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final int TEXT_LEFT_PAD = 36;     // distance from book.left to text start
    private static final int TEXT_TOP_PAD = 30;      // distance from book.top to first text line
    private static final int TEXT_WIDTH = 114;       // safe text width
    private static final int LINE_HEIGHT = 9;

    private static final int ENTRIES_PER_PAGE = 4;
    private static final int LINES_PER_ENTRY = 4;    // title + 2 meta + spacer

    private static final SimpleDateFormat TS_FMT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);

    private final List<ChronicleEntry> source;
    private List<ChronicleEntry> filtered;

    private int currentPage = 0;
    private int totalPages = 1;
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
        // newest first → reads like a journal opened to the latest entry on top
        this.source.sort((a, b) -> Long.compare(b.epochMillis(), a.epochMillis()));
        this.filtered = this.source;
        recomputePages();
    }

    private void recomputePages() {
        this.totalPages = Math.max(1, (filtered.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        if (currentPage >= totalPages) currentPage = totalPages - 1;
    }

    @Override
    protected void init() {
        this.bookLeft = (this.width - BOOK_WIDTH) / 2;
        this.bookTop = 2;

        // search box: thin strip above the book — does not occlude the page
        int searchY = Math.max(2, bookTop - 16);
        this.searchBox = new EditBox(this.font, this.width / 2 - 80, searchY, 130, 12,
                Component.translatable("fieldnotes.screen.search"));
        this.searchBox.setHint(Component.translatable("fieldnotes.screen.search.hint"));
        this.searchBox.setBordered(true);
        this.searchBox.setResponder(s -> applyFilters());
        this.addRenderableWidget(this.searchBox);

        // filter button next to search
        this.frameFilterBtn = Button.builder(
                Component.literal(frameFilter.label()),
                b -> {
                    frameFilter = frameFilter.next();
                    b.setMessage(Component.literal(frameFilter.label()));
                    applyFilters();
                })
                .bounds(this.width / 2 + 54, searchY - 2, 60, 16)
                .build();
        this.addRenderableWidget(this.frameFilterBtn);

        // vanilla PageButton arrows — same look as written_book
        int arrowY = bookTop + BOOK_HEIGHT - 36;
        this.prevBtn = new PageButton(bookLeft + 38, arrowY, false, b -> turnPage(-1), true);
        this.nextBtn = new PageButton(bookLeft + 116, arrowY, true, b -> turnPage(1), true);
        this.addRenderableWidget(this.prevBtn);
        this.addRenderableWidget(this.nextBtn);

        updatePageButtons();
    }

    private void turnPage(int delta) {
        int next = currentPage + delta;
        if (next < 0 || next >= totalPages) return;
        currentPage = next;
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (prevBtn != null) prevBtn.visible = currentPage > 0;
        if (nextBtn != null) nextBtn.visible = currentPage < totalPages - 1;
    }

    private void applyFilters() {
        String q = searchBox != null
                ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";
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
        this.currentPage = 0;
        recomputePages();
        updatePageButtons();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);

        // book background (vanilla texture, 192×192 region from 256×256 sheet)
        g.blit(BOOK_TEX, bookLeft, bookTop, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, 256, 256);

        // page header — "Field Notes" + page number
        Component header = Component.translatable("fieldnotes.screen.title");
        int headerW = this.font.width(header);
        g.drawString(this.font,
                header,
                bookLeft + (BOOK_WIDTH - headerW) / 2,
                bookTop + 14,
                0xFF4A3A28,
                false);

        Component pageLabel = Component.translatable("fieldnotes.screen.page",
                currentPage + 1, totalPages);
        int pageW = this.font.width(pageLabel);
        g.drawString(this.font,
                pageLabel,
                bookLeft + BOOK_WIDTH - pageW - 32,
                bookTop + 14,
                0xFF6B5B43,
                false);

        // page body
        if (filtered.isEmpty()) {
            Component empty = Component.translatable("fieldnotes.screen.empty");
            int ew = this.font.width(empty);
            g.drawString(this.font, empty,
                    bookLeft + (BOOK_WIDTH - ew) / 2,
                    bookTop + 80,
                    0xFF8B7355,
                    false);
            return;
        }

        int x = bookLeft + TEXT_LEFT_PAD;
        int y = bookTop + TEXT_TOP_PAD;
        int startIdx = currentPage * ENTRIES_PER_PAGE;
        int endIdx = Math.min(startIdx + ENTRIES_PER_PAGE, filtered.size());

        for (int i = startIdx; i < endIdx; i++) {
            ChronicleEntry e = filtered.get(i);
            int lineY = y + (i - startIdx) * (LINES_PER_ENTRY * LINE_HEIGHT);
            renderEntry(g, this.font, e, x, lineY);
        }
    }

    private void renderEntry(GuiGraphics g, Font font, ChronicleEntry e, int x, int y) {
        // Line 1: frame-marker + title (truncated/wrapped to TEXT_WIDTH)
        int frameColor = switch (e.frameType()) {
            case "challenge" -> 0xFF8B2B6C; // dark magenta
            case "goal"      -> 0xFF2B5C8B; // dark cyan
            default          -> 0xFF4A3A28; // sepia ink
        };
        String mark = switch (e.frameType()) {
            case "challenge" -> "✦";
            case "goal"      -> "◆";
            default          -> "•";
        };
        Component titleLine = Component.literal(mark + " ")
                .append(Component.literal(e.title()));
        // Trim title to fit one line
        FormattedCharSequence trimmed = font.split(titleLine, TEXT_WIDTH).isEmpty()
                ? titleLine.getVisualOrderText()
                : font.split(titleLine, TEXT_WIDTH).get(0);
        g.drawString(font, trimmed, x, y, frameColor, false);

        // Line 2: timestamp + dimension/biome short
        String when = TS_FMT.format(new Date(e.epochMillis()));
        String dim = shortKey(e.dimensionId());
        String biome = e.biomeId().isEmpty() ? "?" : shortKey(e.biomeId());
        String meta1 = when + " · " + dim;
        if (!biome.equals("?")) meta1 += " · " + biome;
        g.drawString(font, meta1, x, y + LINE_HEIGHT, 0xFF6B5B43, false);

        // Line 3: coordinates
        String coords = String.format(Locale.ROOT, "(%d, %d, %d)", e.x(), e.y(), e.z());
        g.drawString(font, coords, x, y + 2 * LINE_HEIGHT, 0xFF8B7355, false);
    }

    private static String shortKey(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) turnPage(-1);
        else if (scrollY < 0) turnPage(1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            turnPage(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            turnPage(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Cycle: ALL → GOALS+CHALLENGES → CHALLENGES → ALL */
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
