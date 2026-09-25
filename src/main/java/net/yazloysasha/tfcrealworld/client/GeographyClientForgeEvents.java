package net.yazloysasha.tfcrealworld.client;

import net.dries007.tfc.client.screen.CalendarScreen;
import net.dries007.tfc.client.screen.ClimateScreen;
import net.dries007.tfc.client.screen.NutritionScreen;
import net.dries007.tfc.util.Helpers;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.client.widget.GeographyInventoryTabButton;
import vazkii.patchouli.client.book.gui.GuiBook;

@EventBusSubscriber(modid = TFCRealWorld.MOD_ID, value = Dist.CLIENT)
public final class GeographyClientForgeEvents {

  private GeographyClientForgeEvents() {}

  @SubscribeEvent
  public static void onScreenInit(ScreenEvent.Init.Post event) {
    Screen screen = event.getScreen();
    if (screen instanceof InventoryScreen inventoryScreen) {
      int left = inventoryScreen.getGuiLeft();
      int top = inventoryScreen.getGuiTop();
      event.addListener(
        new GeographyInventoryTabButton(
          left,
          top,
          false,
          false
        ).setRecipeBookCallback(inventoryScreen)
      );
      return;
    }
    if (
      screen instanceof ClimateScreen ||
      screen instanceof NutritionScreen ||
      screen instanceof CalendarScreen
    ) {
      AbstractContainerScreen<?> container = (AbstractContainerScreen<
          ?
        >) screen;
      event.addListener(
        new GeographyInventoryTabButton(
          container.getGuiLeft(),
          container.getGuiTop(),
          false,
          false
        )
      );
      return;
    }
    // Field-guide book screen: TFC injects 5 tabs; add our 6th globe.
    if (screen instanceof GuiBook guiBook) {
      if (
        guiBook.book != null &&
        guiBook.book.id.equals(Helpers.resourceLocation("tfc", "field_guide"))
      ) {
        event.addListener(
          new GeographyInventoryTabButton(
            guiBook.bookLeft,
            guiBook.bookTop,
            false,
            true
          )
        );
      }
    }
  }

  @SubscribeEvent
  public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
    ClientVisitedWaypoints.clear();
    GeographyMapTexture.clear();
    GeographySessionState.clear();
  }
}
