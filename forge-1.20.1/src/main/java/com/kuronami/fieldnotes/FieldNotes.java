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
 * Forge 1.20.1 entry point.
 *
 * <p>Differs from Forge 1.21.1: uses old {@code DisplayInfo.getFrame()} API,
 * old {@code new ResourceLocation()} ctor, NBT-style data persistence still works
 * with the same Codec since Codecs are version-stable.
 */
@Mod(FieldNotes.MODID)
public class FieldNotes {

    public static final String MODID = "fieldnotes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FieldNotes() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Item registry hookup (DeferredRegister + Creative tab event)
        com.kuronami.fieldnotes.item.FieldNotesItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(com.kuronami.fieldnotes.event.AdvancementListener.class);

        LOGGER.info("Field Notes (Forge 1.20.1) loaded.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.kuronami.fieldnotes.network.FieldNotesNetwork::register);
    }
}
