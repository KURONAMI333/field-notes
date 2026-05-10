package com.kuronami.fieldnotes;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Field Notes — auto-record your achievements with timestamp, location, and context.
 *
 * Listens to {@code PlayerAdvancementEarnEvent} and persists structured entries
 * (time / coords / dimension / biome / advancement details) to per-world data.
 * Players review the entries via a custom Screen opened by hotkey.
 *
 * <p>NeoForge 1.21.1 entry point. Sub-projects (Forge / Fabric × 1.20.1 / 1.21.1)
 * mirror this with loader-specific event APIs but share the persistence + UI layer.
 */
@Mod(FieldNotes.MODID)
public final class FieldNotes {

    public static final String MODID = "fieldnotes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FieldNotes(IEventBus modEventBus) {
        // mod lifecycle bus: register data attachments / capabilities here
        modEventBus.addListener(FieldNotes::onCommonSetup);

        // game event bus: register advancement listener here (Phase 1.5)
        // NeoForge.EVENT_BUS.register(AdvancementListener.class);

        LOGGER.info("Field Notes loaded — listening for adventures.");
    }

    private static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Field Notes common setup complete.");
    }
}
