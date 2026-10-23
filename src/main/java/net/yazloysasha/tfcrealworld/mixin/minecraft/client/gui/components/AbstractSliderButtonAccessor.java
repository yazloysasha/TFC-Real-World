package net.yazloysasha.tfcrealworld.mixin.minecraft.client.gui.components;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Moves a slider to a value set from elsewhere, without applying it. */
@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
  @Accessor("value")
  void tfcrealworld$setValue(double value);

  @Invoker("updateMessage")
  void tfcrealworld$invokeUpdateMessage();
}
