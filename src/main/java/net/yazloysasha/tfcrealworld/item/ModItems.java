package net.yazloysasha.tfcrealworld.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

/**
 * Simple items used as UI / advancement icons (not intended as survival loot).
 */
public final class ModItems {

  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
    ForgeRegistries.ITEMS,
    TFCRealWorld.MOD_ID
  );

  /**
   * Globe icon for the geography advancement root and inventory tab texture twin.
   */
  public static final RegistryObject<Item> GLOBE = ITEMS.register("globe", () ->
    new Item(new Item.Properties().stacksTo(1))
  );

  private ModItems() {}
}
