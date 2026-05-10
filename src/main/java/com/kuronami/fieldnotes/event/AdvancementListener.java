package com.kuronami.fieldnotes.event;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.data.ChronicleEntry;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/**
 * Listens for the moment a player earns an advancement and records a
 * {@link ChronicleEntry} with the contextual state.
 *
 * <p>Server-side only. Each grant produces exactly one entry; the listener
 * never blocks or modifies the advancement flow.
 */
@EventBusSubscriber(modid = FieldNotes.MODID)
public final class AdvancementListener {

    private AdvancementListener() {}

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AdvancementHolder holder = event.getAdvancement();
        var displayOpt = holder.value().display();
        // Skip advancements without display info (purely internal/parent nodes)
        if (displayOpt.isEmpty()) return;
        DisplayInfo display = displayOpt.get();

        ResourceLocation advId = holder.id();
        BlockPos pos = player.blockPosition();

        String biomeId = "";
        try {
            Holder<Biome> biomeHolder = player.level().getBiome(pos);
            biomeId = biomeHolder.unwrapKey()
                    .map(k -> k.location().toString())
                    .orElse("");
        } catch (Exception ex) {
            // Biome resolution can fail in edge cases (chunk unloaded mid-event); best-effort.
            FieldNotes.LOGGER.debug("Biome resolution skipped at {}: {}", pos, ex.toString());
        }

        ChronicleEntry entry = new ChronicleEntry(
                System.currentTimeMillis(),
                player.level().getDayTime() / 24000L,
                player.level().dimension().location().toString(),
                biomeId,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                advId.toString(),
                display.getTitle().getString(),
                display.getDescription().getString(),
                display.getType().getSerializedName()
        );

        ChronicleStorage.append(player, entry);
    }
}
