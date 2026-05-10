package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.data.ChronicleEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side cached snapshot of the player's chronicle, populated by
 * server response.
 */
public final class ChronicleClientCache {

    private static volatile List<ChronicleEntry> entries = new ArrayList<>();
    private static volatile boolean pendingOpen = false;

    private ChronicleClientCache() {}

    public static void update(List<ChronicleEntry> newEntries) {
        entries = new ArrayList<>(newEntries);
    }

    public static List<ChronicleEntry> getEntries() {
        return entries;
    }

    /** Mark that we want to open the screen as soon as data arrives. */
    public static void requestOpen() {
        pendingOpen = true;
    }

    /** Server responded — open the screen if we were waiting for data. */
    public static void openScreenWhenReady() {
        if (!pendingOpen) return;
        pendingOpen = false;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            // Avoid replacing a real in-game screen if user moved on
            Screen current = mc.screen;
            if (current == null || current instanceof ChronicleScreen) {
                mc.setScreen(new ChronicleScreen(entries));
            }
        });
    }
}
