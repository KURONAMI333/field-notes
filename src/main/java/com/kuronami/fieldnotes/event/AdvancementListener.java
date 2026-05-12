package com.kuronami.fieldnotes.event;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.data.ChronicleEntry;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

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

        // Resolve the icon item's registry id so we can render it later without
        // serializing the whole ItemStack (icons rarely carry NBT for advancements).
        String iconItemId = "";
        try {
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(display.getIcon().getItem());
            if (itemKey != null) iconItemId = itemKey.toString();
        } catch (Exception ignored) { /* item lookup is best-effort */ }

        ChronicleEntry entry = new ChronicleEntry(
                System.currentTimeMillis(),
                player.level().getDayTime() / 24000L,
                player.level().dimension().location().toString(),
                biomeId,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                advId.toString(),
                iconItemId,
                display.getTitle().getString(),
                display.getDescription().getString(),
                display.getType().getSerializedName()
        );

        FieldNotes.LOGGER.info("Chronicle entry recorded: {} ({}) at {}",
                display.getTitle().getString(), advId, pos);
        ChronicleStorage.append(player, entry);
    }

    /**
     * Drop the in-memory cache when the integrated server stops. Without this
     * the cache (keyed by player UUID) bleeds entries from the previous world
     * into the next, so leaving a world and opening another shows the wrong
     * chronicle until the player relogs.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ChronicleStorage.clearCache();
    }
}
