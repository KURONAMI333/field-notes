package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.network.FieldNotesNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FieldNotes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FieldNotesClient {

    public static final KeyMapping OPEN_CHRONICLE = new KeyMapping(
            "key.fieldnotes.open_chronicle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.fieldnotes");

    private FieldNotesClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CHRONICLE);
    }

    @Mod.EventBusSubscriber(modid = FieldNotes.MODID, value = Dist.CLIENT)
    public static final class TickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            while (OPEN_CHRONICLE.consumeClick()) {
                if (Minecraft.getInstance().player == null) return;
                ChronicleClientCache.requestOpen();
                FieldNotesNetwork.requestFromServer();
            }
        }
    }
}
