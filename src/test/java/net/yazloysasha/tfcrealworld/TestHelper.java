package net.yazloysasha.tfcrealworld;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import net.dries007.tfc.common.recipes.ingredients.TFCIngredients;
import net.dries007.tfc.common.recipes.outputs.ItemStackModifiers;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.GameData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;

/**
 * Ensure that we are bootstrapped before each test runs, to prevent errors from uncertain loading order.
 */
public class TestHelper {

  private static final AtomicBoolean BOOTSTRAP = new AtomicBoolean(false);

  @BeforeAll
  public static void setup() {
    bootstrap();
  }

  public static synchronized void bootstrap() {
    if (!BOOTSTRAP.get()) {
      BOOTSTRAP.set(true);

      try {
        final Field field =
          SharedConstants.class.getDeclaredField("CURRENT_VERSION");
        field.setAccessible(true);
        field.set(null, DetectedVersion.BUILT_IN);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        fail("Unable to set SharedConstants#CURRENT_VERSION", e);
      }

      try {
        final Class<?> modListClass = ModList.class;
        Field instanceField = null;
        for (Field f : modListClass.getDeclaredFields()) {
          if (f.getName().equals("INSTANCE") || f.getType() == modListClass) {
            instanceField = f;
            break;
          }
        }

        if (instanceField != null) {
          instanceField.setAccessible(true);
          java.lang.reflect.Constructor<?> constructor =
            modListClass.getDeclaredConstructor(
              java.util.List.class,
              java.util.List.class
            );
          constructor.setAccessible(true);
          Object mockInstance = constructor.newInstance(
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList()
          );

          Field indexedModsField = modListClass.getDeclaredField("indexedMods");
          indexedModsField.setAccessible(true);
          indexedModsField.set(mockInstance, new java.util.HashMap<>());

          instanceField.set(null, mockInstance);
        }
      } catch (Exception e) {
        fail("Unable to mock ModList", e);
      }

      Bootstrap.bootStrap();
      GameData.unfreezeData();

      ItemStackModifiers.registerItemStackModifierTypes();
      TFCIngredients.registerIngredientTypes();
    }
  }

  public static long seed() {
    final long seed = new Random().nextLong();
    System.out.printf("Seed: %d\n", seed);
    return seed;
  }

  public static CraftingContainer mock(int width, int height) {
    return new TransientCraftingContainer(
      new AbstractContainerMenu(null, 0) {
        @NotNull
        @Override
        public ItemStack quickMoveStack(@NotNull Player player, int index) {
          return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
          return true;
        }

        @Override
        public void slotsChanged(@NotNull Container container) {}
      },
      width,
      height
    );
  }
}
