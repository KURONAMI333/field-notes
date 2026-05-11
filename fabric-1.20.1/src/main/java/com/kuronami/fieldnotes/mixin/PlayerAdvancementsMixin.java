package com.kuronami.fieldnotes.mixin;

import com.kuronami.fieldnotes.FieldNotes;
import com.kuronami.fieldnotes.data.ChronicleEntry;
import com.kuronami.fieldnotes.data.ChronicleStorage;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric 1.20.1: hook PlayerAdvancements#award to detect advancement grant. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow private ServerPlayer player;

    @Shadow public abstract AdvancementProgress getOrStartProgress(Advancement advancement);

    @Inject(method = "award", at = @At("RETURN"))
    private void fieldnotes$onAward(Advancement advancement, String criterion,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        AdvancementProgress prog = getOrStartProgress(advancement);
        if (!prog.isDone()) return;

        DisplayInfo display = advancement.getDisplay();
        if (display == null) return;

        ServerPlayer sp = this.player;
        if (sp == null) return;

        BlockPos pos = sp.blockPosition();
        String biomeId = "";
        try {
            Holder<Biome> biomeHolder = sp.level().getBiome(pos);
            biomeId = biomeHolder.unwrapKey().map(k -> k.location().toString()).orElse("");
        } catch (Exception ex) {
            FieldNotes.LOGGER.debug("Biome resolution skipped: {}", ex.toString());
        }

        String iconItemId = "";
        try {
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(display.getIcon().getItem());
            if (itemKey != null) iconItemId = itemKey.toString();
        } catch (Exception ignored) { /* best-effort */ }

        ChronicleEntry entry = new ChronicleEntry(
                System.currentTimeMillis(),
                sp.level().getDayTime() / 24000L,
                sp.level().dimension().location().toString(),
                biomeId,
                pos.getX(), pos.getY(), pos.getZ(),
                advancement.getId().toString(),
                iconItemId,
                display.getTitle().getString(),
                display.getDescription().getString(),
                display.getFrame().getName()
        );

        FieldNotes.LOGGER.info("Chronicle entry recorded: {} ({}) at {}",
                display.getTitle().getString(), advancement.getId(), pos);
        ChronicleStorage.append(sp, entry);
    }
}
