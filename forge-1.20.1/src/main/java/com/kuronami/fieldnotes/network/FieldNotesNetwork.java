package com.kuronami.fieldnotes.network;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.client.ChronicleClientCache;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * SimpleChannel networking for Forge 1.20.1.
 *
 * <p>Differs from 1.21.1: uses old NetworkRegistry.newSimpleChannel + Supplier&lt;NetworkEvent.Context&gt;
 * handler signature (1.21+ moved to CustomPayloadEvent.Context direct param).
 */
public final class FieldNotesNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldNotes.MODID, "main"),
            () -> PROTOCOL_VERSION,
            v -> true,
            v -> true
    );

    private FieldNotesNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(
                0,
                RequestChroniclePayload.class,
                (p, buf) -> p.encode(buf),
                RequestChroniclePayload::decode,
                FieldNotesNetwork::handleRequest,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                1,
                OpenChroniclePayload.class,
                (p, buf) -> p.encode(buf),
                OpenChroniclePayload::decode,
                FieldNotesNetwork::handleOpen,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    private static void handleRequest(RequestChroniclePayload payload, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new OpenChroniclePayload(ChronicleStorage.get(sp)));
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleOpen(OpenChroniclePayload payload, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ChronicleClientCache.update(payload.entries());
            ChronicleClientCache.openScreenWhenReady();
        }));
        ctx.setPacketHandled(true);
    }

    public static void requestFromServer() {
        CHANNEL.sendToServer(RequestChroniclePayload.INSTANCE);
    }
}
