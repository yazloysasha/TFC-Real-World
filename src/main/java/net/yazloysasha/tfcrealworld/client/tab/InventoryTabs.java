package net.yazloysasha.tfcrealworld.client.tab;

import java.util.List;
import net.dries007.tfc.client.screen.CalendarScreen;
import net.dries007.tfc.client.screen.ClimateScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
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

  /** Where TFC's tabs hang on a book-sized panel with this corner. */
  Anchor panelAnchor(int left, int top, int width);

  /**
   * Where TFC's tabs hang on this screen if it is a field guide that shows
   * them, given the screen's widgets.
   */
  @Nullable
  default Anchor bookAnchor(
    Screen screen,
    List<? extends GuiEventListener> widgets
  ) {
    return null;
  }

  /**
   * TFC's own tabs, inactive. The inventory tab runs {@code openInventory}
   * before switching back.
   */
  List<Button> tfcTabs(Anchor anchor, Runnable openInventory);

  /** How TFC draws a tab, open or not, beside a screen or apart from it. */
  TabChrome chrome(boolean active, boolean detached);

  /**
   * The corner TFC hangs its tabs from, as if of a 176-wide inventory, and
   * whether they hang apart from the screen, as beside a book.
   */
  record Anchor(int guiLeft, int guiTop, boolean detached) {}

  /**
   * A tab's offset from its anchor, its width, its chrome in TFC's GUI
   * icons, and its icon's offset from the tab's left edge.
   */
  record TabChrome(
    int xIn,
    int yIn,
    int width,
    int textureU,
    int textureV,
    int iconX
  ) {}

  @Nullable
  private static Class<?> classOrNull(String name) {
    try {
      return Class.forName(name, false, InventoryTabs.class.getClassLoader());
    } catch (ClassNotFoundException | LinkageError ignored) {
      return null;
    }
  }
}
