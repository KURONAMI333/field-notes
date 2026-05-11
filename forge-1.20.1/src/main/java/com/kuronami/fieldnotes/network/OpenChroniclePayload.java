package com.kuronami.fieldnotes.network;

import com.kuronami.fieldnotes.data.ChronicleEntry;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record OpenChroniclePayload(List<ChronicleEntry> entries) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (ChronicleEntry e : entries) {
            buf.writeLong(e.epochMillis());
            buf.writeLong(e.worldDay());
            buf.writeUtf(e.dimensionId());
            buf.writeUtf(e.biomeId());
            buf.writeInt(e.x()); buf.writeInt(e.y()); buf.writeInt(e.z());
            buf.writeUtf(e.advancementId());
            buf.writeUtf(e.iconItemId());
            buf.writeUtf(e.title());
            buf.writeUtf(e.description());
            buf.writeUtf(e.frameType());
        }
    }

    public static OpenChroniclePayload decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ChronicleEntry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new ChronicleEntry(
                    buf.readLong(), buf.readLong(),
                    buf.readUtf(), buf.readUtf(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readUtf(), buf.readUtf(),
                    buf.readUtf(), buf.readUtf(), buf.readUtf()));
        }
        return new OpenChroniclePayload(list);
    }
}
