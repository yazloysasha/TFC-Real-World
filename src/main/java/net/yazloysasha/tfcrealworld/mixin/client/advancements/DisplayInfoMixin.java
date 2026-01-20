package net.yazloysasha.tfcrealworld.mixin.client.advancements;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DisplayInfo.class)
public class DisplayInfoMixin {

  @Unique
  private static final String GLOBE_TROTTER_DESCRIPTION_KEY =
    "tfc_real_world.advancements.world.globe_trotter.description";

  @Inject(
    method = "getDescription()Lnet/minecraft/network/chat/Component;",
    at = @At("RETURN"),
    cancellable = true
  )
  private void modifyDescription(CallbackInfoReturnable<Component> cir) {
    Component original = cir.getReturnValue();
    if (original instanceof TranslatableComponent translatable) {
      String key = translatable.getKey();
      if (GLOBE_TROTTER_DESCRIPTION_KEY.equals(key)) {
        Component modified = createGlobeTrotterComponent(key);
        cir.setReturnValue(modified);
      }
    }
  }

  @Unique
  private Component createGlobeTrotterComponent(String key) {
    int hemisphereScale = TFCRealWorldConfig.VERTICAL_TILE_SIZE.get() / 2;
    Component formattedScale = new net.minecraft.network.chat.TextComponent(
      formatNumberWithCommas(hemisphereScale)
    );

    return new TranslatableComponent(key, formattedScale, formattedScale);
  }

  @Unique
  private String formatNumberWithCommas(int number) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    DecimalFormat formatter = new DecimalFormat("#,###", symbols);
    return formatter.format(number);
  }
}
