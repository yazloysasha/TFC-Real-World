package net.yazloysasha.tfcrealworld.mixin.client.screen;

import net.dries007.tfc.client.screen.CreateTFCWorldScreen;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreateTFCWorldScreen.class)
public interface CreateTFCWorldScreenAccessor {
  @Invoker("smallButton")
  AbstractWidget tfcrealworld$invokeSmallButton(OptionInstance<?> option);

  @Invoker("kmOption")
  static OptionInstance<Integer> tfcrealworld$invokeKmOption(
    String caption,
    int min,
    int max,
    int defaultValue
  ) {
    throw new AssertionError();
  }
}
