package net.yazloysasha.tfcrealworld.mixin.tfg;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.compat.TfgSpawn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.config.TFGConfig;
import su.terrafirmagreg.core.utils.CustomSpawnHelper;
import su.terrafirmagreg.core.utils.CustomSpawnHelper.CustomSpawnCondition;

/**
 * Registers the Real World spawn preset and remaps Core-Modern spawn search.
 */
@Mixin(value = CustomSpawnHelper.class, remap = false)
public class TfgCustomSpawnHelperMixin {

  @Shadow
  public static TreeMap<String, CustomSpawnCondition> CUSTOM_SPAWN_CONDITIONS;

  @Shadow
  @Final
  @Mutable
  public static List<CustomSpawnCondition> CREATE_WORLD_SPAWN_CYCLE_VALUES;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void tfcrealworld$registerSpawnOption(CallbackInfo ci) {
    CustomSpawnCondition ours = TfgSpawn.createCondition();
    CUSTOM_SPAWN_CONDITIONS.put(ours.id(), ours);
    CREATE_WORLD_SPAWN_CYCLE_VALUES = List.copyOf(
      Stream.concat(
        Stream.of(ours),
        CREATE_WORLD_SPAWN_CYCLE_VALUES.stream()
      ).toList()
    );
  }

  @Inject(method = "resetConfigValue", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$resetToRealWorld(CallbackInfo ci) {
    TFGConfig.COMMON.NEW_WORLD_SPAWN.set(TFCRealWorld.MOD_ID);
    TfgSpawn.clearClimateCenter();
    ci.cancel();
  }

  @Inject(method = "testWithinRanges", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$skipClimateForRealWorld(
    float temperature,
    float rainfall,
    CustomSpawnCondition condition,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (TfgSpawn.isRealWorld(condition)) {
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "findSpawnBiome", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$remapSpawnSearch(
    int spawnCenterX,
    int spawnCenterZ,
    int spawnRadius,
    RandomSource random,
    ChunkGeneratorExtension extension,
    CallbackInfoReturnable<BlockPos> cir
  ) {
    BlockPos remapped = TfgSpawn.remapFindSpawnBiome(
      spawnCenterX,
      spawnCenterZ,
      spawnRadius,
      random,
      extension
    );
    if (remapped != null) {
      cir.setReturnValue(remapped);
    }
  }

  @Inject(
    method = "createWorldSpawnCycleLabel",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$label(
    CustomSpawnCondition condition,
    CallbackInfoReturnable<Component> cir
  ) {
    tfcrealworld$realWorldText(
      condition,
      cir,
      "tfc_real_world.create_world.spawn_location"
    );
  }

  @Inject(
    method = "createWorldSpawnTooltipText",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$tooltip(
    CustomSpawnCondition condition,
    CallbackInfoReturnable<Component> cir
  ) {
    tfcrealworld$realWorldText(
      condition,
      cir,
      "tfc_real_world.create_world.spawn_location.tooltip"
    );
  }

  @Unique
  private static void tfcrealworld$realWorldText(
    CustomSpawnCondition condition,
    CallbackInfoReturnable<Component> cir,
    String translationKey
  ) {
    if (TfgSpawn.isRealWorld(condition)) {
      cir.setReturnValue(Component.translatable(translationKey));
    }
  }
}
