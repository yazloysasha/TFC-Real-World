package net.yazloysasha.tfcrealworld.mixin.client.screen;

import com.mojang.serialization.Codec;
import java.util.List;
import net.dries007.tfc.client.screen.CreateTFCWorldScreen;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.SpawnMode;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CreateTFCWorldScreen.class)
public class CreateTFCWorldScreenMixin {

  @Unique
  private OptionInstance<String> mapProfile;

  @Unique
  private OptionInstance<SpawnMode> spawnMode;

  @Unique
  private OptionInstance<Double> spawnCenterLongitude;

  @Unique
  private OptionInstance<Double> spawnCenterLatitude;

  @Shadow
  private OptionInstance<Integer> spawnCenterX;

  @Shadow
  private OptionInstance<Integer> spawnCenterZ;

  @Shadow
  private OptionInstance<Integer> spawnDistance;

  @Unique
  private OptionInstance<Boolean> canyonsNotVolcanic;

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

  @Unique
  private static int optionsCount = 0;

  @Unique
  private static String getCaption(String suffix) {
    return TFCRealWorld.MOD_ID + "." + suffix;
  }

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
        Component.translatable(caption + ".tooltip")
      ),
      (text, value) ->
        Options.genericValueLabel(
          text,
          Component.literal(String.format("%.2f", value))
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
    List<E> values = List.of(enumClass.getEnumConstants());

    Codec<E> codec = Codec.STRING.xmap(
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
        Component.translatable(caption + ".tooltip")
      ),
      (text, value) ->
        Component.translatable(caption + "." + value.name().toLowerCase()),
      enumValueSet,
      defaultValue,
      v -> {}
    );
  }

  @Unique
  private static OptionInstance<String> stringOption(
    String caption,
    List<String> values,
    String defaultValue
  ) {
    Codec<String> codec = Codec.STRING.xmap(
      name -> values.contains(name) ? name : defaultValue,
      value -> value
    );

    OptionInstance.Enum<String> enumValueSet = new OptionInstance.Enum<>(
      values,
      codec
    );

    return new OptionInstance<>(
      caption,
      OptionInstance.cachedConstantTooltip(
        Component.translatable(caption + ".tooltip")
      ),
      (text, value) -> Component.translatable(caption + "." + value),
      enumValueSet,
      defaultValue,
      v -> {}
    );
  }

  @Unique
  private OptionInstance<String> stringOptionWithProfileCallback(
    String caption,
    List<String> values,
    String defaultValue
  ) {
    Codec<String> codec = Codec.STRING.xmap(
      name -> values.contains(name) ? name : defaultValue,
      value -> value
    );

    OptionInstance.Enum<String> enumValueSet = new OptionInstance.Enum<>(
      values,
      codec
    );

    return new OptionInstance<>(
      caption,
      OptionInstance.cachedConstantTooltip(
        Component.translatable(caption + ".tooltip")
      ),
      (text, value) ->
        Component.translatable(caption + "." + value.toLowerCase()),
      enumValueSet,
      defaultValue,
      profileId -> applyProfileSpawnSettings(profileId)
    );
  }

  @Unique
  private void applyProfileSpawnSettings(String profileId) {
    if (
      spawnCenterLongitude == null ||
      spawnCenterLatitude == null ||
      spawnCenterX == null ||
      spawnCenterZ == null
    ) {
      return;
    }

    MapProfile profile = ProfileManager.getProfile(profileId);
    double convertedLongitude = profile.getSpawnCenterLongitude();
    double convertedLatitude = profile.getSpawnCenterLatitude();

    spawnCenterLongitude.set(convertedLongitude);
    spawnCenterLatitude.set(convertedLatitude);
    spawnCenterX.set(profile.spawnCenterX());
    spawnCenterZ.set(profile.spawnCenterZ());

    TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.set(convertedLongitude);
    TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.set(convertedLatitude);
    TFCRealWorldConfig.SPAWN_CENTER_X.set(profile.spawnCenterX());
    TFCRealWorldConfig.SPAWN_CENTER_Z.set(profile.spawnCenterZ());
  }

  @Inject(method = "init()V", at = @At("HEAD"))
  private void tfcrealworld$initAdditionalOptions(CallbackInfo ci) {
    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;

    List<String> availableProfiles = ProfileManager.discoverProfiles();
    String defaultProfile = TFCRealWorldConfig.MAP_PROFILE.get();
    if (!availableProfiles.contains(defaultProfile)) {
      defaultProfile = TFCRealWorldConfig.DEFAULT_MAP_PROFILE;
    }

    mapProfile = stringOptionWithProfileCallback(
      getCaption("create_world.map_profile"),
      availableProfiles,
      defaultProfile
    );
    spawnMode = enumOption(
      getCaption("create_world.spawn_mode"),
      SpawnMode.class,
      TFCRealWorldConfig.SPAWN_MODE.get()
    );
    spawnCenterLongitude = doubleOption(
      getCaption("create_world.spawn_center_longitude"),
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get(),
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.getMin(),
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.getMax()
    );
    spawnCenterLatitude = doubleOption(
      getCaption("create_world.spawn_center_latitude"),
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.get(),
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.getMin(),
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.getMax()
    );
    canyonsNotVolcanic = OptionInstance.createBoolean(
      getCaption("create_world.canyons_not_volcanic"),
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get(),
      value -> {}
    );
    horizontalTileSize = accessor.tfcrealworld$invokeKmOption(
      getCaption("create_world.horizontal_tile_size"),
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.getMin(),
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.getMax(),
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get()
    );
    verticalTileSize = accessor.tfcrealworld$invokeKmOption(
      getCaption("create_world.vertical_tile_size"),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.getMin(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.getMax(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get()
    );
    continentFromMap = OptionInstance.createBoolean(
      getCaption("create_world.continent_from_map"),
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get(),
      value -> {}
    );
    altitudeFromMap = OptionInstance.createBoolean(
      getCaption("create_world.altitude_from_map"),
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get(),
      value -> {}
    );
    hotspotsFromMap = OptionInstance.createBoolean(
      getCaption("create_world.hotspots_from_map"),
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get(),
      value -> {}
    );
    koppenFromMap = OptionInstance.createBoolean(
      getCaption("create_world.koppen_from_map"),
      TFCRealWorldConfig.KOPPEN_FROM_MAP.get(),
      value -> {}
    );
  }

  @Inject(
    method = "init()V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/client/screen/CreateTFCWorldScreen;smallButton(Lnet/minecraft/client/OptionInstance;)Lnet/minecraft/client/gui/components/AbstractWidget;",
      ordinal = 0,
      shift = At.Shift.BEFORE
    ),
    locals = LocalCapture.CAPTURE_FAILHARD
  )
  private void tfcrealworld$addAllOptionsInOrder(
    CallbackInfo ci,
    ChunkGenerator generator,
    Settings settings,
    GridLayout grid,
    GridLayout.RowHelper builder
  ) {
    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;

    builder.addChild(accessor.tfcrealworld$invokeSmallButton(mapProfile));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(spawnMode));
    builder.addChild(
      accessor.tfcrealworld$invokeSmallButton(spawnCenterLongitude)
    );
    builder.addChild(
      accessor.tfcrealworld$invokeSmallButton(spawnCenterLatitude)
    );
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(spawnCenterX));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(spawnCenterZ));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(spawnDistance));
    builder.addChild(
      accessor.tfcrealworld$invokeSmallButton(canyonsNotVolcanic)
    );
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(flatBedrock));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(finiteContinents));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(continentalness));

    optionsCount = 0;
  }

  @Redirect(
    method = "init()V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/client/screen/CreateTFCWorldScreen;smallButton(Lnet/minecraft/client/OptionInstance;)Lnet/minecraft/client/gui/components/AbstractWidget;"
    )
  )
  private AbstractWidget tfcrealworld$cancelOriginalSmallButton(
    CreateTFCWorldScreen instance,
    OptionInstance<?> option
  ) {
    optionsCount++;

    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;

    switch (optionsCount) {
      case 1:
        return accessor.tfcrealworld$invokeSmallButton(grassDensity);
      case 2:
        return accessor.tfcrealworld$invokeSmallButton(temperatureConstant);
      case 3:
        return accessor.tfcrealworld$invokeSmallButton(rainfallConstant);
      case 4:
        return accessor.tfcrealworld$invokeSmallButton(temperatureScale);
      case 5:
        return accessor.tfcrealworld$invokeSmallButton(rainfallScale);
      case 6:
        return accessor.tfcrealworld$invokeSmallButton(horizontalTileSize);
      case 7:
        return accessor.tfcrealworld$invokeSmallButton(verticalTileSize);
      case 8:
        return accessor.tfcrealworld$invokeSmallButton(continentFromMap);
      case 9:
        return accessor.tfcrealworld$invokeSmallButton(altitudeFromMap);
      case 10:
        return accessor.tfcrealworld$invokeSmallButton(hotspotsFromMap);
      case 11:
        return accessor.tfcrealworld$invokeSmallButton(koppenFromMap);
      default:
        return accessor.tfcrealworld$invokeSmallButton(option);
    }
  }

  @Inject(method = "applySettings()V", at = @At("TAIL"))
  private void tfcrealworld$applyAdditionalSettings(CallbackInfo ci) {
    String previousProfile = TFCRealWorldConfig.MAP_PROFILE.get();
    String newProfile = mapProfile.get();

    TFCRealWorldConfig.MAP_PROFILE.set(newProfile);
    TFCRealWorldConfig.SPAWN_MODE.set(spawnMode.get());
    TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.set(spawnCenterLongitude.get());
    TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.set(spawnCenterLatitude.get());
    TFCRealWorldConfig.SPAWN_CENTER_X.set(spawnCenterX.get());
    TFCRealWorldConfig.SPAWN_CENTER_Z.set(spawnCenterZ.get());
    TFCRealWorldConfig.SPAWN_DISTANCE.set(spawnDistance.get());
    TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.set(canyonsNotVolcanic.get());
    TFCRealWorldConfig.FLAT_BEDROCK.set(flatBedrock.get());
    TFCRealWorldConfig.FINITE_CONTINENTS.set(finiteContinents.get());
    TFCRealWorldConfig.CONTINENTALNESS.set(continentalness.get());
    TFCRealWorldConfig.GRASS_DENSITY.set(grassDensity.get());
    TFCRealWorldConfig.TEMPERATURE_CONSTANT.set(
      temperatureConstant.get() * 2.0 - 1.0
    );
    TFCRealWorldConfig.RAINFALL_CONSTANT.set(
      rainfallConstant.get() * 2.0 - 1.0
    );
    TFCRealWorldConfig.TEMPERATURE_SCALE.set(temperatureScale.get());
    TFCRealWorldConfig.RAINFALL_SCALE.set(rainfallScale.get());
    TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.set(horizontalTileSize.get());
    TFCRealWorldConfig.VERTICAL_TILE_SIZE.set(verticalTileSize.get());
    TFCRealWorldConfig.CONTINENT_FROM_MAP.set(continentFromMap.get());
    TFCRealWorldConfig.ALTITUDE_FROM_MAP.set(altitudeFromMap.get());
    TFCRealWorldConfig.HOTSPOTS_FROM_MAP.set(hotspotsFromMap.get());
    TFCRealWorldConfig.KOPPEN_FROM_MAP.set(koppenFromMap.get());

    TFCRealWorldConfig.saveConfig();

    if (!newProfile.equals(previousProfile)) {
      BasePNGNoise.clearImageCache();
      ProfileManager.clearCache();
    }
  }
}
