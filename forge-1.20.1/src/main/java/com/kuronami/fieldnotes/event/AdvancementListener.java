package com.kuronami.fieldnotes.event;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.data.ChronicleEntry;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge 1.20.1: advancement granted listener.
 *
 * <p>API differences from 1.21.1:
 * <ul>
 *   <li>{@link AdvancementEvent.AdvancementEarnEvent#getAdvancement()} returns
 *       {@link Advancement} (not AdvancementHolder)</li>
 *   <li>{@link DisplayInfo#getFrame()} (1.20) vs getType() (1.21+)</li>
 *   <li>{@code FrameType.getName()} returns the serialized name</li>
 * </ul>
 */
public final class AdvancementListener {

    private AdvancementListener() {}

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Advancement adv = event.getAdvancement();
        DisplayInfo display = adv.getDisplay();
        if (display == null) return;

        BlockPos pos = player.blockPosition();
        String biomeId = "";
        try {
            Holder<Biome> biomeHolder = player.level().getBiome(pos);
            biomeId = biomeHolder.unwrapKey().map(k -> k.location().toString()).orElse("");
        } catch (Exception ex) {
            FieldNotes.LOGGER.debug("Biome resolution skipped at {}: {}", pos, ex.toString());
        }

        String iconItemId = "";
        try {
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(display.getIcon().getItem());
            if (itemKey != null) iconItemId = itemKey.toString();
        } catch (Exception ignored) { /* best-effort */ }

        ChronicleEntry entry = new ChronicleEntry(
                System.currentTimeMillis(),
                player.level().getDayTime() / 24000L,
                player.level().dimension().location().toString(),
                biomeId,
                pos.getX(), pos.getY(), pos.getZ(),
                adv.getId().toString(),
                iconItemId,
                display.getTitle().getString(),
                display.getDescription().getString(),
                display.getFrame().getName()
        );

        FieldNotes.LOGGER.info("Chronicle entry recorded: {} ({}) at {}",
                display.getTitle().getString(), adv.getId(), pos);
        ChronicleStorage.append(player, entry);
    }

    /**
     * Drop the in-memory cache on integrated-server stop so leaving a world
     * and entering another doesn't show stale entries from the previous one.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ChronicleStorage.clearCache();
    }
}
