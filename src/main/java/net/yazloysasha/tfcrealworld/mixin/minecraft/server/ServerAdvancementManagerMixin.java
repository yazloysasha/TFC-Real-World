package net.yazloysasha.tfcrealworld.mixin.minecraft.server;

import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.yazloysasha.tfcrealworld.util.geography.GeographyAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin {

  @ModifyArg(
    method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/advancements/AdvancementList;add(Ljava/util/Map;)V"
    )
  )
  private Map<
    ResourceLocation,
    Advancement.Builder
  > tfcrealworld$addGeographyAdvancements(
    Map<ResourceLocation, Advancement.Builder> advancements
  ) {
    GeographyAdvancements.addTo(advancements);
    return advancements;
  }
}
