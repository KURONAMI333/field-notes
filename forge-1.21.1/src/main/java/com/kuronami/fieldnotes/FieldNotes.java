package com.kuronami.fieldnotes;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Field Notes — auto-record advancements with timestamp/coords/biome/dimension.
 * Forge 1.21.1 entry point.
 */
@Mod(FieldNotes.MODID)
public final class FieldNotes {

    public static final String MODID = "fieldnotes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FieldNotes() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Mod-bus events (network registration, key bindings)
        modEventBus.addListener(this::commonSetup);

        // Game-bus event subscribers register via @EventBusSubscriber annotations
        // (AdvancementListener, FieldNotesClient.TickHandler).
        MinecraftForge.EVENT_BUS.register(com.kuronami.fieldnotes.event.AdvancementListener.class);

        LOGGER.info("Field Notes (Forge 1.21.1) loaded.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.kuronami.fieldnotes.network.FieldNotesNetwork::register);
    }
}
