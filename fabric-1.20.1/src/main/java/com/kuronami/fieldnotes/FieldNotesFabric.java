package com.kuronami.fieldnotes;

import com.kuronami.fieldnotes.data.ChronicleStorage;
import com.kuronami.fieldnotes.item.FieldNotesItems;
import com.kuronami.fieldnotes.network.FieldNotesNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Field Notes — Fabric 1.20.1 entry point.
 *
 * <p>Networking uses the older Fabric API (no PayloadTypeRegistry yet);
 * raw {@code FriendlyByteBuf} round-trips via {@link ServerPlayNetworking}.
 */
public final class FieldNotesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FieldNotesItems.init();

        ServerPlayNetworking.registerGlobalReceiver(
                FieldNotesNetwork.REQUEST_ID,
                (server, player, handler, buf, sender) -> {
                    server.execute(() -> FieldNotesNetwork.sendChronicleToPlayer(player,
                            ChronicleStorage.get(player)));
                });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChronicleStorage.clearCache());

        FieldNotes.LOGGER.info("Field Notes (Fabric 1.20.1) initialized.");
    }
}
