package net.yazloysasha.tfcrealworld.mixin.minecraft.client.resources.language;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.util.geography.GeographyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Geography translations live in geography data, not in lang files. Resolving
 * them here covers every place a key can reach the client untranslated, e.g.
 * advancement chat announcements built by a dedicated server.
 */
@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {

  @Unique
  private static final String GEOGRAPHY_KEY_PREFIX =
    TFCRealWorld.MOD_ID + ".geography.";

  @Inject(
    method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
    at = @At("HEAD"),
    cancellable = true
  )
  private void tfcrealworld$resolveGeographyKey(
    String key,
    String defaultValue,
    CallbackInfoReturnable<String> cir
  ) {
    if (key == null || !key.startsWith(GEOGRAPHY_KEY_PREFIX)) {
      return;
    }
    String lang = "en_us";
    Minecraft mc = Minecraft.getInstance();
    if (mc != null && mc.options != null) {
      lang = mc.options.languageCode;
    }
    String resolved = GeographyManager.resolveLang(key, lang);
    if (resolved != null && !resolved.isEmpty()) {
      cir.setReturnValue(resolved);
    }
  }
}
