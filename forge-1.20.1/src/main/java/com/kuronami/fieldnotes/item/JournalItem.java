package com.kuronami.fieldnotes.item;

import com.kuronami.fieldnotes.client.ChronicleClientCache;
import com.kuronami.fieldnotes.network.FieldNotesNetwork;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Journal item (Forge 1.20.1). Right-click → open the chronicle viewer.
 * Uses the 1.20.1 SimpleChannel for the request payload.
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
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    /** Split so dedicated server doesn't load client-only code. */
    private static void openClient() {
        ChronicleClientCache.requestOpen();
        FieldNotesNetwork.requestFromServer();
    }
}
