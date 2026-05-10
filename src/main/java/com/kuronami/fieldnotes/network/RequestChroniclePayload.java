package com.kuronami.fieldnotes.network;

import com.kuronami.fieldnotes.FieldNotes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server: request that the server send my chronicle.
 *
 * <p>Empty payload — the server identifies the player from the connection.
 */
public record RequestChroniclePayload() implements CustomPacketPayload {

    public static final RequestChroniclePayload INSTANCE = new RequestChroniclePayload();

    public static final Type<RequestChroniclePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FieldNotes.MODID, "request_chronicle"));

    public static final StreamCodec<ByteBuf, RequestChroniclePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
