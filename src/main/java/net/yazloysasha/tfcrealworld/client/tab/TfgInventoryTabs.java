package net.yazloysasha.tfcrealworld.client.tab;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.client.screen.button.PlayerInventoryTabButton;
import net.dries007.tfc.compat.patchouli.PatchouliIntegration;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.SwitchInventoryTabPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The tabs of the TFC bundled in TerraFirmaGreg 0.9.24+: each tab is a
 * {@code PlayerInventoryTabButton.Tab}, which TFC 3 lacks, so it is reached
 * reflectively. This TFC also hangs them beside the field guide.
 */
final class TfgInventoryTabs implements InventoryTabs {

  private static final TabChrome INACTIVE = new TabChrome(174, 0, 24, 0, 16, 3);
  private static final TabChrome ACTIVE = new TabChrome(173, 0, 24, 24, 16, 4);
  private static final TabChrome DETACHED = new TabChrome(
    286,
    5,
    24,
    48,
    16,
    4
  );
  private static final TabChrome DETACHED_ACTIVE = new TabChrome(
    286,
    5,
    24,
    72,
    16,
    4
  );

  /** {@code (guiLeft, guiTop, active, detached, tab, onPress)}. */
  private final MethodHandle newButton;
  /** {@code (tab)}. */
  private final MethodHandle newPacket;
  private final Class<?> tabType;

  /** Patchouli's book screen and its corner, if Patchouli is loaded. */
  @Nullable
  private final Class<?> guiBook;

  @Nullable
  private final Field bookLeft;

  @Nullable
  private final Field bookTop;

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
    Class<?> book;
    Field left;
    Field top;
    try {
      book = Class.forName(
        "vazkii.patchouli.client.book.gui.GuiBook",
        false,
        TfgInventoryTabs.class.getClassLoader()
      );
      left = book.getField("bookLeft");
      top = book.getField("bookTop");
    } catch (ReflectiveOperationException | LinkageError ignored) {
      book = null;
      left = null;
      top = null;
    }
    guiBook = book;
    bookLeft = left;
    bookTop = top;
  }

  /** On a book-sized panel, the tabs hang as beside the field guide. */
  @Override
  public Anchor panelAnchor(int left, int top, int width) {
    return new Anchor(left, top, true);
  }

  @Override
  @Nullable
  public Anchor bookAnchor(
    Screen screen,
    List<? extends GuiEventListener> widgets
  ) {
    if (
      guiBook == null ||
      !guiBook.isInstance(screen) ||
      widgets.stream().noneMatch(PlayerInventoryTabButton.class::isInstance)
    ) {
      return null;
    }
    try {
      return new Anchor(bookLeft.getInt(screen), bookTop.getInt(screen), true);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public List<Button> tfcTabs(Anchor anchor, Runnable openInventory) {
    final List<Button> tabs = new ArrayList<>();
    tabs.add(
      tab(anchor, "INVENTORY", button -> {
        openInventory.run();
        switchTo("INVENTORY");
      })
    );
    tabs.add(tab(anchor, "CALENDAR"));
    tabs.add(tab(anchor, "NUTRITION"));
    tabs.add(tab(anchor, "CLIMATE"));
    PatchouliIntegration.ifEnabled(() -> tabs.add(tab(anchor, "BOOK")));
    return tabs;
  }

  @Override
  public TabChrome chrome(boolean active, boolean detached) {
    if (detached) {
      return active ? DETACHED_ACTIVE : DETACHED;
    }
    return active ? ACTIVE : INACTIVE;
  }

  private Button tab(Anchor anchor, String tab) {
    return tab(anchor, tab, button -> switchTo(tab));
  }

  private Button tab(Anchor anchor, String tab, Button.OnPress onPress) {
    try {
      return (Button) newButton.invoke(
        anchor.guiLeft(),
        anchor.guiTop(),
        false,
        anchor.detached(),
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
