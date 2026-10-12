package net.yazloysasha.tfcrealworld.client.tab;

import java.util.List;
import net.dries007.tfc.client.screen.CalendarScreen;
import net.dries007.tfc.client.screen.ClimateScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * TFC's inventory tabs as the loaded TFC draws and switches them. The TFC
 * bundled in TerraFirmaGreg 0.9.24+ reworked them, so each API lives in its
 * own class, loaded only when present.
 */
public interface InventoryTabs {
  InventoryTabs CURRENT =
    classOrNull("net.dries007.tfc.network.SwitchInventoryTabPacket$Type") !=
    null
      ? new Tfc3InventoryTabs()
      : new TfgInventoryTabs();

  /** TerraFirmaGreg's nutrition screen, which replaces TFC's. */
  @Nullable
  Class<?> TFG_NUTRITION_SCREEN = classOrNull(
    "su.terrafirmagreg.core.client.screen.TFGNutritionScreen"
  );

  /** Whether TFC hangs its tabs on this screen. */
  default boolean isTabScreen(Screen screen) {
    return (
      screen instanceof ClimateScreen ||
      screen instanceof CalendarScreen ||
      (TFG_NUTRITION_SCREEN != null && TFG_NUTRITION_SCREEN.isInstance(screen))
    );
  }

  /**
   * TFC's own tabs, inactive, hung on a screen with this corner. The
   * inventory tab runs {@code openInventory} before switching back.
   */
  List<Button> tfcTabs(int guiLeft, int guiTop, Runnable openInventory);

  /** How TFC draws a tab, open or not. */
  TabChrome chrome(boolean active);

  /**
   * A tab's place right of a 176-wide inventory, its width, its chrome in
   * TFC's GUI icons, and its icon's offset from the tab's left edge.
   */
  record TabChrome(int xIn, int width, int textureU, int textureV, int iconX) {}

  @Nullable
  private static Class<?> classOrNull(String name) {
    try {
      return Class.forName(name, false, InventoryTabs.class.getClassLoader());
    } catch (ClassNotFoundException | LinkageError ignored) {
      return null;
    }
  }
}
