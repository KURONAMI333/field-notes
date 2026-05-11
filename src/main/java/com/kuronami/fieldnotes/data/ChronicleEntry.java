package com.kuronami.fieldnotes.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Immutable record of a single advancement event.
 *
 * <p>One entry is created the moment a player earns an advancement, capturing
 * the contextual state at that moment. The set is intentionally minimal:
 * everything here is data the game already exposes. Field Notes does not
 * compute or invent anything — it just lays out what was already there.
 *
 * @param epochMillis    real-time wall clock when the advancement was granted
 * @param worldDay       in-world day count (0 = world creation)
 * @param dimensionId    e.g. {@code "minecraft:overworld"}
 * @param biomeId        e.g. {@code "minecraft:plains"}, may be empty if not resolvable
 * @param x              player x at the moment of grant
 * @param y              player y
 * @param z              player z
 * @param advancementId  e.g. {@code "minecraft:adventure/kill_a_mob"}
 * @param iconItemId     e.g. {@code "minecraft:diamond_pickaxe"} — registry id of the
 *                       advancement's display icon. Stored as a string (not the live
 *                       ItemStack) so the entry stays portable across worlds that
 *                       don't have the source advancement registered.
 * @param title          the advancement's display title (translated at capture time)
 * @param description    the advancement's display description
 * @param frameType      "task" / "goal" / "challenge" — determines tier-of-importance
 */
public record ChronicleEntry(
        long epochMillis,
        long worldDay,
        String dimensionId,
        String biomeId,
        int x,
        int y,
        int z,
        String advancementId,
        String iconItemId,
        String title,
        String description,
        String frameType
) {

    /**
     * Codec for NBT / JSON persistence. Field order matches the record's component order.
     * {@code iconItemId} is optional for back-compat with v0.1.0 entries that pre-date it.
     */
    public static final Codec<ChronicleEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("epochMillis").forGetter(ChronicleEntry::epochMillis),
            Codec.LONG.fieldOf("worldDay").forGetter(ChronicleEntry::worldDay),
            Codec.STRING.fieldOf("dimensionId").forGetter(ChronicleEntry::dimensionId),
            Codec.STRING.fieldOf("biomeId").forGetter(ChronicleEntry::biomeId),
            Codec.INT.fieldOf("x").forGetter(ChronicleEntry::x),
            Codec.INT.fieldOf("y").forGetter(ChronicleEntry::y),
            Codec.INT.fieldOf("z").forGetter(ChronicleEntry::z),
            Codec.STRING.fieldOf("advancementId").forGetter(ChronicleEntry::advancementId),
            Codec.STRING.optionalFieldOf("iconItemId", "").forGetter(ChronicleEntry::iconItemId),
            Codec.STRING.fieldOf("title").forGetter(ChronicleEntry::title),
            Codec.STRING.fieldOf("description").forGetter(ChronicleEntry::description),
            Codec.STRING.fieldOf("frameType").forGetter(ChronicleEntry::frameType)
    ).apply(instance, ChronicleEntry::new));

    /** Returns true for "important" entries (goal/challenge), used for filter/JM-waypoint hints. */
    public boolean isMilestone() {
        return "goal".equals(frameType) || "challenge".equals(frameType);
    }
}
