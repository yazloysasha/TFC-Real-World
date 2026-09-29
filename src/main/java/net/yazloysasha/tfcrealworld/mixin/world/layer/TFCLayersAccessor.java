package net.yazloysasha.tfcrealworld.mixin.world.layer;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TFCLayers.class, remap = false)
public interface TFCLayersAccessor {
  @Accessor("BIOME_LAYERS")
  static BiomeExtension[] tfcrealworld$getBiomeLayers() {
    throw new AssertionError();
  }
}
