package com.kuronami.fieldnotes.network;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.data.ChronicleEntry;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server → Client payload carrying the player's full chronicle.
 *
 * <p>Sent on demand when the client requests to open the Field Notes screen.
 * Re-sent on each open (chronicle is small enough — usually &lt; 200 entries —
 * that incremental sync isn't worth the complexity).
 */
public record OpenChroniclePayload(List<ChronicleEntry> entries) implements CustomPacketPayload {

    public static final Type<OpenChroniclePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FieldNotes.MODID, "open_chronicle"));

    public static final StreamCodec<ByteBuf, OpenChroniclePayload> STREAM_CODEC = ByteBufCodecs
            .fromCodec(ChronicleEntry.CODEC.listOf())
            .map(OpenChroniclePayload::new, OpenChroniclePayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
