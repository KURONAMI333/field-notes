package com.kuronami.fieldnotes.item;

import com.kuronami.fieldnotes.FieldNotes;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Item registration for Field Notes (Fabric 1.21.1).
 *
 * <p>Mirrors the NeoForge registration but uses Fabric's direct registry calls
 * + ItemGroupEvents for the Creative tab. Same item id, same texture, same
 * stack-size-1 properties, same right-click behavior — the differences are
 * entirely in the registration plumbing.
 */
public final class FieldNotesItems {

    public static final Item JOURNAL = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(FieldNotes.MODID, "journal"),
            new JournalItem(new Item.Properties().stacksTo(1))
    );

    private FieldNotesItems() {}

    /** Called from the ModInitializer; forces class load + tab registration. */
    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(content -> content.accept(JOURNAL));
    }
}
