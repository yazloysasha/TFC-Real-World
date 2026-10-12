package net.yazloysasha.tfcrealworld.client.tab;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.client.screen.button.PlayerInventoryTabButton;
import net.dries007.tfc.compat.patchouli.PatchouliIntegration;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.SwitchInventoryTabPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraftforge.network.PacketDistributor;

/**
 * The tabs of the TFC bundled in TerraFirmaGreg 0.9.24+: each tab is a
 * {@code PlayerInventoryTabButton.Tab}, which TFC 3 lacks, so it is reached
 * reflectively.
 */
final class TfgInventoryTabs implements InventoryTabs {

  private static final TabChrome INACTIVE = new TabChrome(174, 24, 0, 16, 3);
  private static final TabChrome ACTIVE = new TabChrome(173, 24, 24, 16, 4);

  /** {@code (guiLeft, guiTop, active, detached, tab, onPress)}. */
  private final MethodHandle newButton;
  /** {@code (tab)}. */
  private final MethodHandle newPacket;
  private final Class<?> tabType;

  TfgInventoryTabs() {
    try {
      tabType = Class.forName(
        PlayerInventoryTabButton.class.getName() + "$Tab",
        true,
        PlayerInventoryTabButton.class.getClassLoader()
      );
      final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
      newButton = lookup.findConstructor(
        PlayerInventoryTabButton.class,
        MethodType.methodType(
          void.class,
          int.class,
          int.class,
          boolean.class,
          boolean.class,
          tabType,
          Button.OnPress.class
        )
      );
      newPacket = lookup.findConstructor(
        SwitchInventoryTabPacket.class,
        MethodType.methodType(void.class, tabType)
      );
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unknown TFC inventory tabs", e);
    }
  }

  @Override
  public List<Button> tfcTabs(int guiLeft, int guiTop, Runnable openInventory) {
    final List<Button> tabs = new ArrayList<>();
    tabs.add(
      tab(guiLeft, guiTop, "INVENTORY", button -> {
        openInventory.run();
        switchTo("INVENTORY");
      })
    );
    tabs.add(tab(guiLeft, guiTop, "CALENDAR"));
    tabs.add(tab(guiLeft, guiTop, "NUTRITION"));
    tabs.add(tab(guiLeft, guiTop, "CLIMATE"));
    PatchouliIntegration.ifEnabled(() ->
      tabs.add(tab(guiLeft, guiTop, "BOOK"))
    );
    return tabs;
  }

  @Override
  public TabChrome chrome(boolean active) {
    return active ? ACTIVE : INACTIVE;
  }

  private Button tab(int guiLeft, int guiTop, String tab) {
    return tab(guiLeft, guiTop, tab, button -> switchTo(tab));
  }

  private Button tab(
    int guiLeft,
    int guiTop,
    String tab,
    Button.OnPress onPress
  ) {
    try {
      return (Button) newButton.invoke(
        guiLeft,
        guiTop,
        false,
        false,
        tab(tab),
        onPress
      );
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }

  private void switchTo(String tab) {
    final Object packet;
    try {
      packet = newPacket.invoke(tab(tab));
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
    PacketHandler.send(PacketDistributor.SERVER.noArg(), packet);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object tab(String name) {
    return Enum.valueOf((Class) tabType, name);
  }
}
