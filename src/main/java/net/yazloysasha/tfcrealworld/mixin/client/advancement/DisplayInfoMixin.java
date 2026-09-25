package net.yazloysasha.tfcrealworld.mixin.client.advancement;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.geography.GeographyManager;
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
    method = "getTitle()Lnet/minecraft/network/chat/Component;",
    at = @At("RETURN"),
    cancellable = true
  )
  private void tfcrealworld$resolveGeographyTitle(
    CallbackInfoReturnable<Component> cir
  ) {
    Component resolved = tfcrealworld$resolveGeographyComponent(
      cir.getReturnValue()
    );
    if (resolved != null) {
      cir.setReturnValue(resolved);
    }
  }

  @Inject(
    method = "getDescription()Lnet/minecraft/network/chat/Component;",
    at = @At("RETURN"),
    cancellable = true
  )
  private void tfcrealworld$modifyDescription(
    CallbackInfoReturnable<Component> cir
  ) {
    Component original = cir.getReturnValue();
    if (
      original != null &&
      original.getContents() instanceof TranslatableContents translatable
    ) {
      String key = translatable.getKey();
      if (GLOBE_TROTTER_DESCRIPTION_KEY.equals(key)) {
        cir.setReturnValue(tfcrealworld$createGlobeTrotterComponent(key));
        return;
      }
    }
    Component resolved = tfcrealworld$resolveGeographyComponent(original);
    if (resolved != null) {
      cir.setReturnValue(resolved);
    }
  }

  @Unique
  private Component tfcrealworld$resolveGeographyComponent(Component original) {
    if (
      original == null ||
      !(original.getContents() instanceof TranslatableContents translatable)
    ) {
      return null;
    }
    String key = translatable.getKey();
    if (!key.startsWith("tfc_real_world.geography.")) {
      return null;
    }
    String lang = "en_us";
    try {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.options != null) {
        lang = mc.options.languageCode;
      }
    } catch (Throwable ignored) {}
    String resolved = GeographyManager.resolveLang(key, lang);
    if (resolved == null || resolved.isEmpty()) {
      return null;
    }
    return Component.literal(resolved);
  }

  @Unique
  private Component tfcrealworld$createGlobeTrotterComponent(String key) {
    int hemisphereScale = TFCRealWorldConfig.VERTICAL_SCALE.get();
    Component formattedScale = Component.literal(
      tfcrealworld$formatNumberWithCommas(hemisphereScale)
    );
    return Component.translatable(key, formattedScale, formattedScale);
  }

  @Unique
  private String tfcrealworld$formatNumberWithCommas(int number) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    DecimalFormat formatter = new DecimalFormat("#,###", symbols);
    return formatter.format(number);
  }
}
