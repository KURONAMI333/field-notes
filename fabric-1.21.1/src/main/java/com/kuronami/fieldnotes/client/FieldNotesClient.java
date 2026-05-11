package com.kuronami.fieldnotes.client;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.network.OpenChroniclePayload;
import com.kuronami.fieldnotes.network.RequestChroniclePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric 1.21.1 client init: keybinding + S2C payload receiver.
 */
public final class FieldNotesClient implements ClientModInitializer {

    public static final KeyMapping OPEN_CHRONICLE = new KeyMapping(
            "key.fieldnotes.open_chronicle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,  // K — J would collide with JourneyMap's full-map binding
            "key.categories.fieldnotes");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_CHRONICLE);

        // Server replied → cache + open screen
        ClientPlayNetworking.registerGlobalReceiver(OpenChroniclePayload.TYPE, (payload, ctx) -> {
            ChronicleClientCache.update(payload.entries());
            ChronicleClientCache.openScreenWhenReady();
        });

        // Per-tick keypress check
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_CHRONICLE.consumeClick()) {
                if (client.player == null) return;
                ChronicleClientCache.requestOpen();
                ClientPlayNetworking.send(RequestChroniclePayload.INSTANCE);
            }
        });

        FieldNotes.LOGGER.info("Field Notes client (Fabric 1.21.1) initialized.");
    }
}
