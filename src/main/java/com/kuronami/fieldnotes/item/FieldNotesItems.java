package com.kuronami.fieldnotes.item;

import com.kuronami.fieldnotes.FieldNotes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry for Field Notes.
 *
 * <p>Currently a single entry: the {@link JournalItem journal} that opens the
 * chronicle viewer when right-clicked. Kept as its own class so the registration
 * surface stays compact and {@link FieldNotes} only knows to forward a bus.
 */
public final class FieldNotesItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(FieldNotes.MODID);

    /**
     * The Journal — right-click to open the chronicle screen. Stacks to 1 because
     * each one is conceptually "your" notebook; duplicates would just be confusing.
     */
    public static final DeferredItem<JournalItem> JOURNAL = ITEMS.register("journal",
            () -> new JournalItem(new Item.Properties().stacksTo(1)));

    private FieldNotesItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
