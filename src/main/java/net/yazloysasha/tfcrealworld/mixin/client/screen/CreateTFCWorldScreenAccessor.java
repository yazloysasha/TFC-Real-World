package net.yazloysasha.tfcrealworld.mixin.client.screen;

import net.dries007.tfc.client.screen.CreateTFCWorldScreen;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CreateTFCWorldScreen.class, remap = false)
public interface CreateTFCWorldScreenAccessor {
  @Accessor("options")
  OptionsList tfcrealworld$getOptions();

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
