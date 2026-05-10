package com.kuronami.fieldnotes.network;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.client.ChronicleClientCache;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the Field Notes packet types and routes incoming payloads to handlers.
 *
 * <p>Server → Client: {@link OpenChroniclePayload} carries the chronicle list,
 * client caches it and opens the screen.
 *
 * <p>Client → Server: {@link RequestChroniclePayload} asks the server to send
 * the player's chronicle.
 */
@EventBusSubscriber(modid = FieldNotes.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FieldNotesNetwork {

    /** Bumped when the wire format changes. */
    private static final String VERSION = "1";

    private FieldNotesNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToClient(
                OpenChroniclePayload.TYPE,
                OpenChroniclePayload.STREAM_CODEC,
                FieldNotesNetwork::handleOpenChronicleClient);

        registrar.playToServer(
                RequestChroniclePayload.TYPE,
                RequestChroniclePayload.STREAM_CODEC,
                FieldNotesNetwork::handleRequestChronicleServer);
    }

    /** Server-side: client asked for chronicle → reply with their entries. */
    private static void handleRequestChronicleServer(RequestChroniclePayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        var entries = ChronicleStorage.get(sp);
        ctx.reply(new OpenChroniclePayload(entries));
    }

    /** Client-side: received chronicle from server → cache + open screen. */
    private static void handleOpenChronicleClient(OpenChroniclePayload payload, IPayloadContext ctx) {
        ChronicleClientCache.update(payload.entries());
        ChronicleClientCache.openScreenWhenReady();
    }
}
