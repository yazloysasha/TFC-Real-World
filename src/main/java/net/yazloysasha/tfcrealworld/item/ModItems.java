package net.yazloysasha.tfcrealworld.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

/**
 * Simple items used as UI / advancement icons (not intended as survival loot).
 */
public final class ModItems {

  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
    Registries.ITEM,
    TFCRealWorld.MOD_ID
  );

  /**
   * Globe icon for the geography advancement root and inventory tab texture twin.
   */
  public static final DeferredHolder<Item, Item> GLOBE = ITEMS.register(
    "globe",
    () -> new Item(new Item.Properties().stacksTo(1))
  );

  private ModItems() {}
}
