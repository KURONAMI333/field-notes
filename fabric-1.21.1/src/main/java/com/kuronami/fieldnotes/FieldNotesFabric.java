package com.kuronami.fieldnotes;

import com.kuronami.fieldnotes.data.ChronicleStorage;
import com.kuronami.fieldnotes.item.FieldNotesItems;
import com.kuronami.fieldnotes.network.OpenChroniclePayload;
import com.kuronami.fieldnotes.network.RequestChroniclePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Field Notes — Fabric 1.21.1 entry point.
 *
 * <p>Fabric has no first-class advancement-grant event, so we use a Mixin
 * (see {@code mixin/PlayerAdvancementsMixin}) to capture the moment.
 *
 * <p>Networking uses the Fabric API's PayloadTypeRegistry / ServerPlayNetworking.
 */
public final class FieldNotesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Force item-registration class load + Creative tab hookup
        FieldNotesItems.init();

        // Register payloads (both directions)
        PayloadTypeRegistry.playC2S().register(RequestChroniclePayload.TYPE, RequestChroniclePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenChroniclePayload.TYPE, OpenChroniclePayload.STREAM_CODEC);

        // Server-side: respond to client request with player's chronicle
        ServerPlayNetworking.registerGlobalReceiver(RequestChroniclePayload.TYPE, (payload, ctx) -> {
            var player = ctx.player();
            ServerPlayNetworking.send(player, new OpenChroniclePayload(ChronicleStorage.get(player)));
        });

        FieldNotes.LOGGER.info("Field Notes (Fabric 1.21.1) initialized.");
    }
}
