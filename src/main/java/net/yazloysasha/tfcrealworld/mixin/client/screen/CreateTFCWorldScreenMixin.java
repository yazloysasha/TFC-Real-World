package net.yazloysasha.tfcrealworld.mixin.client.screen;

import com.mojang.serialization.Codec;
import java.util.List;
import net.dries007.tfc.client.screen.CreateTFCWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.Component;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

  @Shadow(remap = false)
  private OptionInstance<Integer> spawnCenterX;

  @Shadow(remap = false)
  private OptionInstance<Integer> spawnCenterZ;

  @Shadow(remap = false)
  private OptionInstance<Integer> spawnDistance;

  @Unique
  private OptionInstance<Boolean> canyonsNotVolcanic;

  @Shadow(remap = false)
  private OptionInstance<Boolean> flatBedrock;

  @Shadow(remap = false)
  private OptionInstance<Double> continentalness;

  @Shadow(remap = false)
  private OptionInstance<Double> grassDensity;

  @Shadow(remap = false)
  private OptionInstance<Double> temperatureConstant;

  @Shadow(remap = false)
  private OptionInstance<Double> rainfallConstant;

  @Shadow(remap = false)
  private OptionInstance<Integer> temperatureScale;

  @Shadow(remap = false)
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
      (text, value) -> {
        MapProfile profile = ProfileManager.getProfile(value);
        String languageCode = Minecraft.getInstance().options.languageCode;
        String displayName = profile.getDisplayName(languageCode);
        return Component.literal(displayName);
      },
      enumValueSet,
      defaultValue,
      profileId -> applyProfileSpawnSettings(profileId)
    );
  }

  @Unique
  private void applyProfileSpawnSettings(String profileId) {
    MapProfile profile = ProfileManager.getProfile(profileId);

    spawnCenterLongitude.set(profile.spawnCenterLongitude());
    spawnCenterLatitude.set(profile.spawnCenterLatitude());
    spawnCenterX.set(profile.getSpawnCenterX());
    spawnCenterZ.set(profile.getSpawnCenterZ());
    horizontalTileSize.set(profile.horizontalTileSize());
    verticalTileSize.set(profile.verticalTileSize());

    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;
    OptionsList options = accessor.tfcrealworld$getOptions();
    if (options != null) {
      options.children().clear();
      addOptionsToList(options);
    }
  }

  @Unique
  private void addOptionsToList(OptionsList options) {
    options.children().clear();

    OptionInstance<Boolean> emptyOption = new OptionInstance<>(
      "",
      OptionInstance.noTooltip(),
      (text, value) -> Component.empty(),
      OptionInstance.BOOLEAN_VALUES,
      false,
      value -> {}
    );

    options.addSmall(mapProfile, spawnMode);
    options.addSmall(spawnCenterLongitude, spawnCenterLatitude);
    options.addSmall(spawnCenterX, spawnCenterZ);
    options.addSmall(spawnDistance, canyonsNotVolcanic);
    options.addSmall(flatBedrock, emptyOption);
    options.addSmall(continentalness, grassDensity);
    options.addSmall(temperatureConstant, rainfallConstant);
    options.addSmall(temperatureScale, rainfallScale);
    options.addSmall(horizontalTileSize, verticalTileSize);
    options.addSmall(continentFromMap, altitudeFromMap);
    options.addSmall(hotspotsFromMap, koppenFromMap);
  }

  @Inject(method = "init", at = @At("HEAD"))
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
    method = "init",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall(Lnet/minecraft/client/OptionInstance;Lnet/minecraft/client/OptionInstance;)V",
      ordinal = 4,
      shift = At.Shift.AFTER
    )
  )
  private void tfcrealworld$addAllOptions(CallbackInfo ci) {
    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;
    OptionsList options = accessor.tfcrealworld$getOptions();
    addOptionsToList(options);
  }

  @Inject(method = "applySettings", at = @At("TAIL"), remap = false)
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
    }
  }
}
