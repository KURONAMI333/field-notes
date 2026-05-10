package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.network.RequestChroniclePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Client setup: keybinding to open the Field Notes screen.
 *
 * <p>Default key: J (mnemonic for "Journal"). User can rebind in Controls menu.
 *
 * <p>The keypress sends {@link RequestChroniclePayload} to the server; on
 * receipt the server replies with the chronicle and the screen opens via
 * {@link ChronicleClientCache#openScreenWhenReady()}.
 */
@EventBusSubscriber(modid = FieldNotes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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

    /** Game-bus tick handler runs on EventBus.GAME, separate class needed. */
    @EventBusSubscriber(modid = FieldNotes.MODID, value = Dist.CLIENT)
    public static final class TickHandler {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_CHRONICLE.consumeClick()) {
                openChronicle();
            }
        }

        private static void openChronicle() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            ChronicleClientCache.requestOpen();
            PacketDistributor.sendToServer(RequestChroniclePayload.INSTANCE);
        }
    }
}
