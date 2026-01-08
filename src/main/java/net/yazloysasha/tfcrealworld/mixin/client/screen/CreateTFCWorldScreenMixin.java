package net.yazloysasha.tfcrealworld.mixin.client.screen;

import net.dries007.tfc.client.screen.CreateTFCWorldScreen;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.layouts.GridLayout;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.SpawnMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CreateTFCWorldScreen.class)
public class CreateTFCWorldScreenMixin {

  // Spawn settings
  @Unique
  private OptionInstance<SpawnMode> spawnMode;

  @Unique
  private OptionInstance<Double> spawnCenterLongitude;

  @Unique
  private OptionInstance<Double> spawnCenterLatitude;

  // World generation settings
  @Shadow
  private OptionInstance<Integer> spawnCenterX;

  @Shadow
  private OptionInstance<Integer> spawnCenterZ;

  @Shadow
  private OptionInstance<Integer> spawnDistance;

  @Shadow
  private OptionInstance<Boolean> flatBedrock;

  @Shadow
  private OptionInstance<Boolean> finiteContinents;

  @Shadow
  private OptionInstance<Double> continentalness;

  @Shadow
  private OptionInstance<Double> grassDensity;

  @Shadow
  private OptionInstance<Double> temperatureConstant;

  @Shadow
  private OptionInstance<Double> rainfallConstant;

  @Shadow
  private OptionInstance<Integer> temperatureScale;

  @Shadow
  private OptionInstance<Integer> rainfallScale;

  // Generation mode settings
  @Unique
  private OptionInstance<Integer> horizontalTileSize;

  @Unique
  private OptionInstance<Integer> verticalTileSize;

  @Unique
  private OptionInstance<Boolean> continentFromMap;

  @Unique
  private OptionInstance<Boolean> altitudeFromMap;

  @Unique
  private OptionInstance<Boolean> hotspotsFromMap;

  @Unique
  private OptionInstance<Boolean> koppenFromMap;

  // Biome modifications settings
  @Unique
  private OptionInstance<Boolean> canyonsNotVolcanic;

  @Unique
  private static OptionInstance<Double> doubleOption(
    String caption,
    double defaultValue,
    double min,
    double max
  ) {
    double range = max - min;
    return new OptionInstance<>(
      caption,
      OptionInstance.cachedConstantTooltip(
        net.minecraft.network.chat.Component.translatable(caption + ".tooltip")
      ),
      (text, value) ->
        net.minecraft.client.Options.genericValueLabel(
          text,
          net.minecraft.network.chat.Component.literal(
            String.format("%.2f", value)
          )
        ),
      OptionInstance.UnitDouble.INSTANCE.xmap(
        sliderValue -> min + sliderValue * range,
        value -> (value - min) / range
      ),
      Math.max(min, Math.min(max, defaultValue)),
      value -> {}
    );
  }

  @Unique
  private static <E extends Enum<E>> OptionInstance<E> enumOption(
    String caption,
    Class<E> enumClass,
    E defaultValue
  ) {
    java.util.List<E> values = java.util.List.of(enumClass.getEnumConstants());

    com.mojang.serialization.Codec<E> codec =
      com.mojang.serialization.Codec.STRING.xmap(
        name -> {
          try {
            return Enum.valueOf(enumClass, name.toUpperCase());
          } catch (IllegalArgumentException e) {
            return defaultValue;
          }
        },
        value -> value.name().toLowerCase()
      );

    OptionInstance.Enum<E> enumValueSet = new OptionInstance.Enum<>(
      values,
      codec
    );

    return new OptionInstance<>(
      caption,
      OptionInstance.cachedConstantTooltip(
        net.minecraft.network.chat.Component.translatable(caption + ".tooltip")
      ),
      (text, value) ->
        net.minecraft.network.chat.Component.translatable(
          caption + "." + value.name().toLowerCase()
        ),
      enumValueSet,
      defaultValue,
      v -> {}
    );
  }

  @Inject(method = "init()V", at = @At("HEAD"))
  private void tfcrealworld$initAdditionalOptions(CallbackInfo ci) {
    spawnMode = enumOption(
      "tfc_real_world.create_world.spawn_mode",
      SpawnMode.class,
      TFCRealWorldConfig.SPAWN_MODE.get()
    );
    spawnCenterLongitude = doubleOption(
      "tfc_real_world.create_world.spawn_center_longitude",
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get(),
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.getMin(),
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.getMax()
    );
    spawnCenterLatitude = doubleOption(
      "tfc_real_world.create_world.spawn_center_latitude",
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.get(),
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.getMin(),
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.getMax()
    );
    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;
    horizontalTileSize = accessor.tfcrealworld$invokeKmOption(
      "tfc_real_world.create_world.horizontal_tile_size",
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.getMin(),
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.getMax(),
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get()
    );
    verticalTileSize = accessor.tfcrealworld$invokeKmOption(
      "tfc_real_world.create_world.vertical_tile_size",
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.getMin(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.getMax(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get()
    );
    continentFromMap = OptionInstance.createBoolean(
      "tfc_real_world.create_world.continent_from_map",
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get(),
      value -> {}
    );
    altitudeFromMap = OptionInstance.createBoolean(
      "tfc_real_world.create_world.altitude_from_map",
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get(),
      value -> {}
    );
    hotspotsFromMap = OptionInstance.createBoolean(
      "tfc_real_world.create_world.hotspots_from_map",
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get(),
      value -> {}
    );
    koppenFromMap = OptionInstance.createBoolean(
      "tfc_real_world.create_world.koppen_from_map",
      TFCRealWorldConfig.KOPPEN_FROM_MAP.get(),
      value -> {}
    );
    canyonsNotVolcanic = OptionInstance.createBoolean(
      "tfc_real_world.create_world.canyons_not_volcanic",
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get(),
      value -> {}
    );
  }

  @Inject(
    method = "init()V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
      ordinal = 0,
      shift = At.Shift.BEFORE
    ),
    locals = LocalCapture.CAPTURE_FAILHARD
  )
  private void tfcrealworld$addAdditionalOptions(
    CallbackInfo ci,
    net.minecraft.world.level.chunk.ChunkGenerator generator,
    net.dries007.tfc.world.settings.Settings settings,
    GridLayout grid,
    GridLayout.RowHelper builder
  ) {
    if (spawnMode != null) {
      final CreateTFCWorldScreenAccessor accessor =
        (CreateTFCWorldScreenAccessor) (Object) this;
      builder.addChild(accessor.tfcrealworld$invokeSmallButton(spawnMode));
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(spawnCenterLongitude)
      );
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(spawnCenterLatitude)
      );
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(horizontalTileSize)
      );
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(verticalTileSize)
      );
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(continentFromMap)
      );
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(altitudeFromMap)
      );
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(hotspotsFromMap)
      );
      builder.addChild(accessor.tfcrealworld$invokeSmallButton(koppenFromMap));
      builder.addChild(
        accessor.tfcrealworld$invokeSmallButton(canyonsNotVolcanic)
      );
    }
  }

  @Inject(method = "applySettings()V", at = @At("TAIL"))
  private void tfcrealworld$applyAdditionalSettings(CallbackInfo ci) {
    if (spawnMode != null) {
      TFCRealWorldConfig.SPAWN_MODE.set(spawnMode.get());
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.set(spawnCenterLongitude.get());
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.set(spawnCenterLatitude.get());
      TFCRealWorldConfig.SPAWN_CENTER_X.set(spawnCenterX.get());
      TFCRealWorldConfig.SPAWN_CENTER_Z.set(spawnCenterZ.get());
      TFCRealWorldConfig.SPAWN_DISTANCE.set(spawnDistance.get());
      TFCRealWorldConfig.FLAT_BEDROCK.set(flatBedrock.get());
      TFCRealWorldConfig.FINITE_CONTINENTS.set(finiteContinents.get());
      TFCRealWorldConfig.CONTINENTALNESS.set(continentalness.get());
      TFCRealWorldConfig.GRASS_DENSITY.set(grassDensity.get());
      TFCRealWorldConfig.TEMPERATURE_CONSTANT.set(temperatureConstant.get());
      TFCRealWorldConfig.RAINFALL_CONSTANT.set(rainfallConstant.get());
      TFCRealWorldConfig.TEMPERATURE_SCALE.set(temperatureScale.get());
      TFCRealWorldConfig.RAINFALL_SCALE.set(rainfallScale.get());
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.set(horizontalTileSize.get());
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.set(verticalTileSize.get());
      TFCRealWorldConfig.CONTINENT_FROM_MAP.set(continentFromMap.get());
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.set(altitudeFromMap.get());
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.set(hotspotsFromMap.get());
      TFCRealWorldConfig.KOPPEN_FROM_MAP.set(koppenFromMap.get());
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.set(canyonsNotVolcanic.get());
    }
  }
}
