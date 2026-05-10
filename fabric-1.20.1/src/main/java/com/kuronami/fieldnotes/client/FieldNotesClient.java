package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.network.FieldNotesNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric 1.20.1 client init.
 */
public final class FieldNotesClient implements ClientModInitializer {

    public static final KeyMapping OPEN_CHRONICLE = new KeyMapping(
            "key.fieldnotes.open_chronicle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.fieldnotes");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_CHRONICLE);

        // S2C handler: receive chronicle list and open screen
        ClientPlayNetworking.registerGlobalReceiver(FieldNotesNetwork.OPEN_ID,
                (client, handler, buf, sender) -> {
                    var entries = FieldNotesNetwork.readEntries(buf);
                    client.execute(() -> {
                        ChronicleClientCache.update(entries);
                        ChronicleClientCache.openScreenWhenReady();
                    });
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_CHRONICLE.consumeClick()) {
                if (client.player == null) return;
                ChronicleClientCache.requestOpen();
                ClientPlayNetworking.send(FieldNotesNetwork.REQUEST_ID, PacketByteBufs.empty());
            }
        });

        FieldNotes.LOGGER.info("Field Notes client (Fabric 1.20.1) initialized.");
    }
}
