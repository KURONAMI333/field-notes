package com.kuronami.fieldnotes.network;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.data.ChronicleEntry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric 1.20.1 raw byte-buf networking.
 *
 * <p>Two channels: REQUEST (C2S, empty body) and OPEN (S2C, list of entries).
 */
public final class FieldNotesNetwork {

    public static final ResourceLocation REQUEST_ID =
            new ResourceLocation(FieldNotes.MODID, "request_chronicle");
    public static final ResourceLocation OPEN_ID =
            new ResourceLocation(FieldNotes.MODID, "open_chronicle");

    private FieldNotesNetwork() {}

    public static void sendChronicleToPlayer(ServerPlayer player, List<ChronicleEntry> entries) {
        FriendlyByteBuf buf = PacketByteBufs.create();
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
        ServerPlayNetworking.send(player, OPEN_ID, buf);
    }

    public static List<ChronicleEntry> readEntries(FriendlyByteBuf buf) {
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
        return list;
    }
}
