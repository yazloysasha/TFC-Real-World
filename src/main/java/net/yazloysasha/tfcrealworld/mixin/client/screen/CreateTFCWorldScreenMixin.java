package net.yazloysasha.tfcrealworld.mixin.client.screen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.dries007.tfc.client.screen.CreateTFCWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.compat.TfeCompat;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.SpawnMode;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenParameterCache;
import net.yazloysasha.tfcrealworld.world.noise.koppen.SmoothedKoppenParameterMaps;
import net.yazloysasha.tfcrealworld.world.noise.koppen.TfeKoppenParameterCache;
import net.yazloysasha.tfcrealworld.world.noise.koppen.TfeSmoothedKoppenParameterMaps;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
  private OptionInstance<Integer> horizontalScale;

  @Unique
  private OptionInstance<Integer> verticalScale;

  @Unique
  private OptionInstance<Boolean> continentFromMap;

  @Unique
  private OptionInstance<Boolean> altitudeFromMap;

  @Unique
  private OptionInstance<Boolean> hotspotsFromMap;

  @Unique
  private OptionInstance<Boolean> koppenFromMap;

  @Unique
  private OptionInstance<Boolean> tectonicsFromMap;

  @Unique
  private OptionInstance<Boolean> canyonsNotVolcanic;

  @Unique
  private OptionInstance<Boolean> finiteContinents;

  @Unique
  private double scaleRatio = 2.0;

  @Unique
  private boolean updatingScales = false;

  @Unique
  private int tfcrealworld$kmOptionCount;

  @Unique
  private static String getCaption(String suffix) {
    return TFCRealWorld.MOD_ID + "." + suffix;
  }

  @Unique
  private static OptionInstance<Boolean> booleanOption(
    String caption,
    boolean defaultValue,
    Consumer<Boolean> onChange
  ) {
    return new OptionInstance<>(
      caption,
      OptionInstance.cachedConstantTooltip(
        Component.translatable(caption + ".tooltip")
      ),
      (text, value) ->
        value
          ? Component.translatable("options.on")
          : Component.translatable("options.off"),
      OptionInstance.BOOLEAN_VALUES,
      defaultValue,
      onChange
    );
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
  private static OptionInstance<Integer> kmOptionWithCallback(
    String caption,
    int min,
    int max,
    int defaultValue,
    Consumer<Integer> callback
  ) {
    return new OptionInstance<>(
      caption,
      OptionInstance.cachedConstantTooltip(
        Component.translatable(caption + ".tooltip")
      ),
      (text, value) ->
        Options.genericValueLabel(
          text,
          Component.translatable(
            "tfc.settings.km",
            String.format("%.1f", value / 1000.0)
          )
        ),
      new OptionInstance.IntRange(min, max),
      defaultValue,
      callback
    );
  }

  @Unique
  private void updateVerticalScaleFromHorizontal(int newHorizontalScale) {
    if (updatingScales || verticalScale == null) {
      return;
    }
    updatingScales = true;
    try {
      int newVerticalScale = Mth.clamp(
        (int) Math.round(newHorizontalScale / scaleRatio),
        TFCRealWorldConfig.VERTICAL_SCALE.getMin(),
        TFCRealWorldConfig.VERTICAL_SCALE.getMax()
      );
      if (verticalScale.get() != newVerticalScale) {
        verticalScale.set(newVerticalScale);
      }
    } finally {
      updatingScales = false;
    }
  }

  @Unique
  private void updateHorizontalScaleFromVertical(int newVerticalScale) {
    if (updatingScales || horizontalScale == null) {
      return;
    }
    updatingScales = true;
    try {
      int newHorizontalScale = Mth.clamp(
        (int) Math.round(newVerticalScale * scaleRatio),
        TFCRealWorldConfig.HORIZONTAL_SCALE.getMin(),
        TFCRealWorldConfig.HORIZONTAL_SCALE.getMax()
      );
      if (horizontalScale.get() != newHorizontalScale) {
        horizontalScale.set(newHorizontalScale);
      }
    } finally {
      updatingScales = false;
    }
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

    double westEdge = profile.westEdgeLongitude();
    double eastEdge = profile.eastEdgeLongitude();
    double southEdge = profile.southEdgeLatitude();
    double northEdge = profile.northEdgeLatitude();

    spawnCenterLongitude = doubleOption(
      getCaption("create_world.spawn_center_longitude"),
      profile.spawnCenterLongitude(),
      westEdge,
      eastEdge
    );
    spawnCenterLatitude = doubleOption(
      getCaption("create_world.spawn_center_latitude"),
      profile.spawnCenterLatitude(),
      southEdge,
      northEdge
    );
    if (spawnCenterX != null) {
      spawnCenterX.set(profile.getSpawnCenterX());
    }
    if (spawnCenterZ != null) {
      spawnCenterZ.set(profile.getSpawnCenterZ());
    }

    int profileHorizontalScale = profile.horizontalScale();
    int profileVerticalScale = profile.verticalScale();
    scaleRatio = (double) profileHorizontalScale / profileVerticalScale;

    updatingScales = true;
    try {
      if (horizontalScale != null) {
        horizontalScale.set(profileHorizontalScale);
      }
      if (verticalScale != null) {
        verticalScale.set(profileVerticalScale);
      }
    } finally {
      updatingScales = false;
    }

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

    options.addSmall(mapProfile, spawnMode);
    options.addSmall(spawnCenterLongitude, spawnCenterLatitude);
    options.addSmall(spawnCenterX, spawnCenterZ);
    if (TfeCompat.isModPresent()) {
      options.addSmall(spawnDistance, canyonsNotVolcanic);
      options.addSmall(flatBedrock, finiteContinents);
    } else {
      options.addSmall(spawnDistance, flatBedrock);
    }
    options.addSmall(continentalness, grassDensity);
    options.addSmall(temperatureConstant, rainfallConstant);
    options.addSmall(temperatureScale, rainfallScale);
    options.addSmall(horizontalScale, verticalScale);
    options.addSmall(continentFromMap, altitudeFromMap);
    if (TfeCompat.isModPresent()) {
      options.addSmall(hotspotsFromMap, tectonicsFromMap);
      options.addBig(koppenFromMap);
    } else {
      options.addSmall(hotspotsFromMap, koppenFromMap);
    }
  }

  @Redirect(
    method = "init()V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/client/screen/CreateTFCWorldScreen;kmOption(Ljava/lang/String;III)Lnet/minecraft/client/OptionInstance;",
      remap = false
    )
  )
  private OptionInstance<Integer> tfcrealworld$expandTfcKmOptionRange(
    String caption,
    int min,
    int max,
    int defaultValue
  ) {
    tfcrealworld$kmOptionCount++;
    return switch (tfcrealworld$kmOptionCount) {
      case 1 -> CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
        caption,
        TFCRealWorldConfig.SPAWN_DISTANCE.getMin(),
        TFCRealWorldConfig.SPAWN_DISTANCE.getMax(),
        TFCRealWorldConfig.SPAWN_DISTANCE.get()
      );
      case 2 -> CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
        caption,
        TFCRealWorldConfig.SPAWN_CENTER_X.getMin(),
        TFCRealWorldConfig.SPAWN_CENTER_X.getMax(),
        TFCRealWorldConfig.SPAWN_CENTER_X.get()
      );
      case 3 -> CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
        caption,
        TFCRealWorldConfig.SPAWN_CENTER_Z.getMin(),
        TFCRealWorldConfig.SPAWN_CENTER_Z.getMax(),
        TFCRealWorldConfig.SPAWN_CENTER_Z.get()
      );
      case 4 -> CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
        caption,
        TFCRealWorldConfig.TEMPERATURE_SCALE.getMin(),
        TFCRealWorldConfig.TEMPERATURE_SCALE.getMax(),
        TFCRealWorldConfig.TEMPERATURE_SCALE.get()
      );
      case 5 -> CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
        caption,
        TFCRealWorldConfig.RAINFALL_SCALE.getMin(),
        TFCRealWorldConfig.RAINFALL_SCALE.getMax(),
        TFCRealWorldConfig.RAINFALL_SCALE.get()
      );
      default -> CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
        caption,
        min,
        max,
        defaultValue
      );
    };
  }

  @Inject(method = "init()V", at = @At("HEAD"))
  private void tfcrealworld$initAdditionalOptions(CallbackInfo ci) {
    tfcrealworld$kmOptionCount = 0;

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
      TFCRealWorldConfig.getWestEdgeLongitude(),
      TFCRealWorldConfig.getEastEdgeLongitude()
    );
    spawnCenterLatitude = doubleOption(
      getCaption("create_world.spawn_center_latitude"),
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.get(),
      TFCRealWorldConfig.getSouthEdgeLatitude(),
      TFCRealWorldConfig.getNorthEdgeLatitude()
    );

    int initialHorizontalScale = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    int initialVerticalScale = TFCRealWorldConfig.VERTICAL_SCALE.get();
    scaleRatio = (double) initialHorizontalScale / initialVerticalScale;

    horizontalScale = kmOptionWithCallback(
      getCaption("create_world.horizontal_scale"),
      TFCRealWorldConfig.HORIZONTAL_SCALE.getMin(),
      TFCRealWorldConfig.HORIZONTAL_SCALE.getMax(),
      initialHorizontalScale,
      this::updateVerticalScaleFromHorizontal
    );
    verticalScale = kmOptionWithCallback(
      getCaption("create_world.vertical_scale"),
      TFCRealWorldConfig.VERTICAL_SCALE.getMin(),
      TFCRealWorldConfig.VERTICAL_SCALE.getMax(),
      initialVerticalScale,
      this::updateHorizontalScaleFromVertical
    );
    continentFromMap = booleanOption(
      getCaption("create_world.continent_from_map"),
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get(),
      value -> {}
    );
    altitudeFromMap = booleanOption(
      getCaption("create_world.altitude_from_map"),
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get(),
      value -> {}
    );
    hotspotsFromMap = booleanOption(
      getCaption("create_world.hotspots_from_map"),
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get(),
      value -> {}
    );
    koppenFromMap = booleanOption(
      getCaption("create_world.koppen_from_map"),
      TFCRealWorldConfig.KOPPEN_FROM_MAP.get(),
      value -> {}
    );
    if (TfeCompat.isModPresent()) {
      tectonicsFromMap = booleanOption(
        getCaption("create_world.rifts_from_map"),
        TFCRealWorldConfig.TECTONICS_FROM_MAP.get(),
        value -> {}
      );
    }
    canyonsNotVolcanic = OptionInstance.createBoolean(
      getCaption("create_world.canyons_not_volcanic"),
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get(),
      value -> {}
    );
    finiteContinents = new OptionInstance<>(
      "tfc.create_world.finite_continents",
      OptionInstance.cachedConstantTooltip(
        Component.translatable("tfc.create_world.finite_continents.tooltip")
      ),
      (text, value) ->
        value
          ? Component.translatable("options.on")
          : Component.translatable("options.off"),
      OptionInstance.BOOLEAN_VALUES,
      TFCRealWorldConfig.FINITE_CONTINENTS.get(),
      value -> {}
    );
  }

  @Inject(
    method = "init()V",
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
    TFCRealWorldConfig.HORIZONTAL_SCALE.set(horizontalScale.get());
    TFCRealWorldConfig.VERTICAL_SCALE.set(verticalScale.get());
    TFCRealWorldConfig.CONTINENT_FROM_MAP.set(continentFromMap.get());
    TFCRealWorldConfig.ALTITUDE_FROM_MAP.set(altitudeFromMap.get());
    TFCRealWorldConfig.HOTSPOTS_FROM_MAP.set(hotspotsFromMap.get());
    TFCRealWorldConfig.KOPPEN_FROM_MAP.set(koppenFromMap.get());
    if (tectonicsFromMap != null) {
      TFCRealWorldConfig.TECTONICS_FROM_MAP.set(tectonicsFromMap.get());
    }

    TFCRealWorldConfig.saveConfig();

    if (!newProfile.equals(previousProfile)) {
      BasePNGNoise.clearImageCache();
      KoppenParameterCache.clear();
      SmoothedKoppenParameterMaps.clear();
      TfeKoppenParameterCache.clear();
      TfeSmoothedKoppenParameterMaps.clear();
    }
  }
}
