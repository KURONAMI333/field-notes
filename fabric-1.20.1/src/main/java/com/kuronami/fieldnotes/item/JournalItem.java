package com.kuronami.fieldnotes.item;

import com.kuronami.fieldnotes.client.ChronicleClientCache;
import com.kuronami.fieldnotes.network.FieldNotesNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Journal item (Fabric 1.20.1). Right-click -> open the chronicle viewer.
 * Uses 1.20.1's raw FriendlyByteBuf channel API.
 */
public final class JournalItem extends Item {

    public JournalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            openClient();
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Environment(EnvType.CLIENT)
    private static void openClient() {
        ChronicleClientCache.requestOpen();
        ClientPlayNetworking.send(FieldNotesNetwork.REQUEST_ID, PacketByteBufs.empty());
    }
}
