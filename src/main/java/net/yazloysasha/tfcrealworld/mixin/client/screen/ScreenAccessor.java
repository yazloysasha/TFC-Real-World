package net.yazloysasha.tfcrealworld.mixin.client.screen;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
  @Invoker("removeWidget")
  void tfcrealworld$invokeRemoveWidget(GuiEventListener widget);

  @Invoker("addRenderableWidget")
  <T extends GuiEventListener> T tfcrealworld$invokeAddRenderableWidget(
    T widget
  );
}
