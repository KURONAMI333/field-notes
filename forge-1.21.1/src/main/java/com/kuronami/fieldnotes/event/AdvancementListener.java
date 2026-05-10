package com.kuronami.fieldnotes.event;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.data.ChronicleEntry;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge game-bus listener: advancement granted -> append chronicle entry. */
public final class AdvancementListener {

    private AdvancementListener() {}

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AdvancementHolder holder = event.getAdvancement();
        var displayOpt = holder.value().display();
        if (displayOpt.isEmpty()) return;
        DisplayInfo display = displayOpt.get();

        BlockPos pos = player.blockPosition();
        String biomeId = "";
        try {
            Holder<Biome> biomeHolder = player.level().getBiome(pos);
            biomeId = biomeHolder.unwrapKey().map(k -> k.location().toString()).orElse("");
        } catch (Exception ex) {
            FieldNotes.LOGGER.debug("Biome resolution skipped at {}: {}", pos, ex.toString());
        }

        ChronicleEntry entry = new ChronicleEntry(
                System.currentTimeMillis(),
                player.level().getDayTime() / 24000L,
                player.level().dimension().location().toString(),
                biomeId,
                pos.getX(), pos.getY(), pos.getZ(),
                holder.id().toString(),
                display.getTitle().getString(),
                display.getDescription().getString(),
                display.getType().getSerializedName()
        );

        ChronicleStorage.append(player, entry);
    }
}
