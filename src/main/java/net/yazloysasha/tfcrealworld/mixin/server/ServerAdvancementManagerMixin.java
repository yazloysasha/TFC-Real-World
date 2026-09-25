package net.yazloysasha.tfcrealworld.mixin.server;

import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.yazloysasha.tfcrealworld.util.geography.GeographyAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin
  implements GeographyAdvancements.GeographyAdvancementAccessor {

  @Shadow
  @Mutable
  private Map<ResourceLocation, AdvancementHolder> advancements;

  @Shadow
  @Mutable
  private AdvancementTree tree;

  @Inject(method = "apply", at = @At("RETURN"))
  private void tfcrealworld$injectGeographyAdvancements(
    Map<ResourceLocation, JsonElement> object,
    ResourceManager resourceManager,
    ProfilerFiller profiler,
    CallbackInfo ci
  ) {
    GeographyAdvancements.injectInto((ServerAdvancementManager) (Object) this);
  }

  @Override
  public void tfcrealworld$setAdvancements(
    Map<ResourceLocation, AdvancementHolder> advancements
  ) {
    this.advancements = advancements;
  }

  @Override
  public void tfcrealworld$setTree(AdvancementTree tree) {
    this.tree = tree;
  }
}
