package com.kuronami.fieldnotes.item;

import com.kuronami.fieldnotes.client.ChronicleClientCache;
import com.kuronami.fieldnotes.network.RequestChroniclePayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Journal — right-click to open the Field Notes chronicle viewer.
 *
 * <p>Mirrors the keybinding flow exactly: the client side primes
 * {@link ChronicleClientCache} for an "open when ready" handshake, then sends
 * a {@link RequestChroniclePayload} so the server can reply with the player's
 * chronicle. We don't have to do anything server-side here; the request
 * payload's handler already serves the chronicle back.
 *
 * <p>Stacks to 1, no durability, no special tooltip beyond the localized name.
 * The journal is intentionally minimal so it doesn't compete with the screen
 * it opens — the chronicle is the experience, the item is just the door.
 */
public final class JournalItem extends Item {

    public JournalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            openClient();
        }
        // Both sides return success so the swing animation plays uniformly.
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    /** Split into its own method so the client-only call sites don't load on a dedicated server. */
    private static void openClient() {
        ChronicleClientCache.requestOpen();
        PacketDistributor.sendToServer(RequestChroniclePayload.INSTANCE);
    }
}
