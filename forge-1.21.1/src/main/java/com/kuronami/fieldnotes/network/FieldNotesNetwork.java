package com.kuronami.fieldnotes.network;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.client.ChronicleClientCache;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * SimpleChannel-based networking (Forge 1.21.1).
 *
 * <p>Channel: {@code fieldnotes:main}, protocol version 1.
 * Two messages: 0=request (C2S), 1=open chronicle (S2C).
 */
public final class FieldNotesNetwork {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(FieldNotes.MODID, "main"))
            .networkProtocolVersion(1)
            .optional()
            .simpleChannel();

    private FieldNotesNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(RequestChroniclePayload.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> p.encode(buf))
                .decoder(RequestChroniclePayload::decode)
                .consumerMainThread(FieldNotesNetwork::handleRequest)
                .add();

        CHANNEL.messageBuilder(OpenChroniclePayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> p.encode(buf))
                .decoder(OpenChroniclePayload::decode)
                .consumerMainThread(FieldNotesNetwork::handleOpen)
                .add();
    }

    private static void handleRequest(RequestChroniclePayload payload, CustomPayloadEvent.Context ctx) {
        ServerPlayer sp = ctx.getSender();
        if (sp != null) {
            CHANNEL.send(new OpenChroniclePayload(ChronicleStorage.get(sp)),
                    PacketDistributor.PLAYER.with(sp));
        }
        ctx.setPacketHandled(true);
    }

    private static void handleOpen(OpenChroniclePayload payload, CustomPayloadEvent.Context ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ChronicleClientCache.update(payload.entries());
            ChronicleClientCache.openScreenWhenReady();
        });
        ctx.setPacketHandled(true);
    }

    public static void requestFromServer() {
        CHANNEL.send(RequestChroniclePayload.INSTANCE, PacketDistributor.SERVER.noArg());
    }
}
