package com.kuronami.fieldnotes.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client → Server: empty marker, server identifies player from connection. */
public record RequestChroniclePayload() {
    public static final RequestChroniclePayload INSTANCE = new RequestChroniclePayload();
    public void encode(FriendlyByteBuf buf) {}
    public static RequestChroniclePayload decode(FriendlyByteBuf buf) { return INSTANCE; }
}
