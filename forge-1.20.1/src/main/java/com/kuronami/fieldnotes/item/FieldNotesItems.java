package com.kuronami.fieldnotes.item;

import com.kuronami.fieldnotes.FieldNotes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registry for Field Notes (Forge 1.20.1).
 *
 * <p>Uses Forge's DeferredRegister + RegistryObject pattern. The Creative
 * tab hookup uses BuildCreativeModeTabContentsEvent on the mod bus.
 */
@Mod.EventBusSubscriber(modid = FieldNotes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FieldNotesItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FieldNotes.MODID);

    public static final RegistryObject<Item> JOURNAL = ITEMS.register("journal",
            () -> new JournalItem(new Item.Properties().stacksTo(1)));

    private FieldNotesItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    @SubscribeEvent
    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(JOURNAL.get());
        }
    }
}
