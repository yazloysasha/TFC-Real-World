package net.yazloysasha.tfcrealworld.mixin.client.screen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.dries007.tfc.client.screen.CreateTFCWorldScreen;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.mixin.minecraft.client.gui.screens.ScreenAccessor;
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
  private OptionInstance<Integer> horizontalScale;

  @Unique
  private OptionInstance<Integer> verticalScale;

  @Unique
  private OptionInstance<Boolean> continentFromMap;

  @Unique
  private OptionInstance<Boolean> lakesFromMap;

  @Unique
  private OptionInstance<Boolean> tectonicsFromMap;

  @Unique
  private OptionInstance<Boolean> volcanoesFromMap;

  @Unique
  private OptionInstance<Boolean> riversFromMap;

  @Unique
  private OptionInstance<Boolean> climateFromMap;

  @Unique
  private AbstractWidget spawnCenterLongitudeWidget;

  @Unique
  private AbstractWidget spawnCenterLatitudeWidget;

  @Unique
  private AbstractWidget spawnCenterXWidget;

  @Unique
  private AbstractWidget spawnCenterZWidget;

  @Unique
  private AbstractWidget horizontalScaleWidget;

  @Unique
  private AbstractWidget verticalScaleWidget;

  @Unique
  private double scaleRatio = 2.0;

  @Unique
  private boolean updatingScales = false;

  @Unique
  private int tfcrealworld$optionsCount;

  @Unique
  private int tfcrealworld$kmOptionCount;

  @Unique
  private GridLayout.RowHelper tfcrealworld$rowHelper;

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
    spawnCenterX.set(profile.getSpawnCenterX());
    spawnCenterZ.set(profile.getSpawnCenterZ());

    int profileHorizontalScale = profile.horizontalScale();
    int profileVerticalScale = profile.verticalScale();
    scaleRatio = (double) profileHorizontalScale / profileVerticalScale;

    updatingScales = true;
    try {
      horizontalScale.set(profileHorizontalScale);
      verticalScale.set(profileVerticalScale);
    } finally {
      updatingScales = false;
    }

    updateWidgets();
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
    if (updatingScales) return;
    updatingScales = true;
    try {
      int newVerticalScale = (int) Math.round(newHorizontalScale / scaleRatio);
      int minVertical = TFCRealWorldConfig.VERTICAL_SCALE.getMin();
      int maxVertical = TFCRealWorldConfig.VERTICAL_SCALE.getMax();

      newVerticalScale = Math.max(
        minVertical,
        Math.min(maxVertical, newVerticalScale)
      );

      if (verticalScale.get() != newVerticalScale) {
        verticalScale.set(newVerticalScale);
        updateWidget(verticalScaleWidget, verticalScale, widget ->
          verticalScaleWidget = widget
        );
      }
    } finally {
      updatingScales = false;
    }
  }

  @Unique
  private void updateHorizontalScaleFromVertical(int newVerticalScale) {
    if (updatingScales) return;
    updatingScales = true;
    try {
      int newHorizontalScale = (int) Math.round(newVerticalScale * scaleRatio);
      int minHorizontal = TFCRealWorldConfig.HORIZONTAL_SCALE.getMin();
      int maxHorizontal = TFCRealWorldConfig.HORIZONTAL_SCALE.getMax();

      newHorizontalScale = Math.max(
        minHorizontal,
        Math.min(maxHorizontal, newHorizontalScale)
      );

      if (horizontalScale.get() != newHorizontalScale) {
        horizontalScale.set(newHorizontalScale);
        updateWidget(horizontalScaleWidget, horizontalScale, widget ->
          horizontalScaleWidget = widget
        );
      }
    } finally {
      updatingScales = false;
    }
  }

  @Unique
  private void updateWidget(
    AbstractWidget currentWidget,
    OptionInstance<?> option,
    Consumer<AbstractWidget> setter
  ) {
    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;
    final ScreenAccessor screenAccessor = (ScreenAccessor) (Object) this;

    AbstractWidget newWidget = accessor.tfcrealworld$invokeSmallButton(option);
    newWidget.setPosition(currentWidget.getX(), currentWidget.getY());
    newWidget.setWidth(currentWidget.getWidth());
    screenAccessor.tfcrealworld$invokeRemoveWidget(currentWidget);
    screenAccessor.tfcrealworld$invokeAddRenderableWidget(newWidget);
    setter.accept(newWidget);
  }

  @Unique
  private void updateWidgets() {
    updateWidget(spawnCenterLongitudeWidget, spawnCenterLongitude, widget ->
      spawnCenterLongitudeWidget = widget
    );
    updateWidget(spawnCenterLatitudeWidget, spawnCenterLatitude, widget ->
      spawnCenterLatitudeWidget = widget
    );
    updateWidget(spawnCenterXWidget, spawnCenterX, widget ->
      spawnCenterXWidget = widget
    );
    updateWidget(spawnCenterZWidget, spawnCenterZ, widget ->
      spawnCenterZWidget = widget
    );
    updateWidget(horizontalScaleWidget, horizontalScale, widget ->
      horizontalScaleWidget = widget
    );
    updateWidget(verticalScaleWidget, verticalScale, widget ->
      verticalScaleWidget = widget
    );
  }

  @Inject(method = "init()V", at = @At("HEAD"))
  private void tfcrealworld$initAdditionalOptions(CallbackInfo ci) {
    tfcrealworld$kmOptionCount = 0;
    tfcrealworld$optionsCount = 0;

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
    canyonsNotVolcanic = OptionInstance.createBoolean(
      getCaption("create_world.canyons_not_volcanic"),
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get(),
      value -> {}
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
    lakesFromMap = booleanOption(
      getCaption("create_world.lakes_from_map"),
      TFCRealWorldConfig.LAKES_FROM_MAP.get(),
      value -> {}
    );
    tectonicsFromMap = booleanOption(
      getCaption("create_world.tectonics_from_map"),
      TFCRealWorldConfig.TECTONICS_FROM_MAP.get(),
      value -> {}
    );
    volcanoesFromMap = booleanOption(
      getCaption("create_world.volcanoes_from_map"),
      TFCRealWorldConfig.VOLCANOES_FROM_MAP.get(),
      value -> {}
    );
    riversFromMap = booleanOption(
      getCaption("create_world.rivers_from_map"),
      TFCRealWorldConfig.RIVERS_FROM_MAP.get(),
      value -> {}
    );
    climateFromMap = booleanOption(
      getCaption("create_world.climate_from_map"),
      TFCRealWorldConfig.CLIMATE_FROM_MAP.get(),
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

    tfcrealworld$rowHelper = builder;

    builder.addChild(accessor.tfcrealworld$invokeSmallButton(mapProfile));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(spawnMode));
    spawnCenterLongitudeWidget = accessor.tfcrealworld$invokeSmallButton(
      spawnCenterLongitude
    );
    builder.addChild(spawnCenterLongitudeWidget);
    spawnCenterLatitudeWidget = accessor.tfcrealworld$invokeSmallButton(
      spawnCenterLatitude
    );
    builder.addChild(spawnCenterLatitudeWidget);
    spawnCenterXWidget = accessor.tfcrealworld$invokeSmallButton(spawnCenterX);
    builder.addChild(spawnCenterXWidget);
    spawnCenterZWidget = accessor.tfcrealworld$invokeSmallButton(spawnCenterZ);
    builder.addChild(spawnCenterZWidget);
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(spawnDistance));
    builder.addChild(
      accessor.tfcrealworld$invokeSmallButton(canyonsNotVolcanic)
    );
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(flatBedrock));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(finiteContinents));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(continentalness));
    builder.addChild(accessor.tfcrealworld$invokeSmallButton(grassDensity));
    builder.addChild(
      accessor.tfcrealworld$invokeSmallButton(temperatureConstant)
    );

    tfcrealworld$optionsCount = 0;
  }

  @Redirect(
    method = "init()V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/client/screen/CreateTFCWorldScreen;kmOption(Ljava/lang/String;III)Lnet/minecraft/client/OptionInstance;"
    )
  )
  private OptionInstance<Integer> tfcrealworld$redirectKmOption(
    String caption,
    int min,
    int max,
    int defaultValue
  ) {
    tfcrealworld$kmOptionCount++;

    switch (tfcrealworld$kmOptionCount) {
      case 1:
        return CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
          caption,
          TFCRealWorldConfig.SPAWN_DISTANCE.getMin(),
          TFCRealWorldConfig.SPAWN_DISTANCE.getMax(),
          TFCRealWorldConfig.SPAWN_DISTANCE.get()
        );
      case 2:
        return CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
          caption,
          TFCRealWorldConfig.SPAWN_CENTER_X.getMin(),
          TFCRealWorldConfig.SPAWN_CENTER_X.getMax(),
          TFCRealWorldConfig.SPAWN_CENTER_X.get()
        );
      case 3:
        return CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
          caption,
          TFCRealWorldConfig.SPAWN_CENTER_Z.getMin(),
          TFCRealWorldConfig.SPAWN_CENTER_Z.getMax(),
          TFCRealWorldConfig.SPAWN_CENTER_Z.get()
        );
      case 4:
        return CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
          caption,
          TFCRealWorldConfig.TEMPERATURE_SCALE.getMin(),
          TFCRealWorldConfig.TEMPERATURE_SCALE.getMax(),
          TFCRealWorldConfig.TEMPERATURE_SCALE.get()
        );
      case 5:
        return CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
          caption,
          TFCRealWorldConfig.RAINFALL_SCALE.getMin(),
          TFCRealWorldConfig.RAINFALL_SCALE.getMax(),
          TFCRealWorldConfig.RAINFALL_SCALE.get()
        );
      default:
        return CreateTFCWorldScreenAccessor.tfcrealworld$invokeKmOption(
          caption,
          min,
          max,
          defaultValue
        );
    }
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
    tfcrealworld$optionsCount++;

    final CreateTFCWorldScreenAccessor accessor =
      (CreateTFCWorldScreenAccessor) (Object) this;

    switch (tfcrealworld$optionsCount) {
      case 1:
        return accessor.tfcrealworld$invokeSmallButton(rainfallConstant);
      case 2:
        return accessor.tfcrealworld$invokeSmallButton(temperatureScale);
      case 3:
        return accessor.tfcrealworld$invokeSmallButton(rainfallScale);
      case 4:
        horizontalScaleWidget = accessor.tfcrealworld$invokeSmallButton(
          horizontalScale
        );
        return horizontalScaleWidget;
      case 5:
        verticalScaleWidget = accessor.tfcrealworld$invokeSmallButton(
          verticalScale
        );
        return verticalScaleWidget;
      case 6:
        return accessor.tfcrealworld$invokeSmallButton(continentFromMap);
      case 7:
        return accessor.tfcrealworld$invokeSmallButton(lakesFromMap);
      case 8:
        return accessor.tfcrealworld$invokeSmallButton(tectonicsFromMap);
      case 9:
        return accessor.tfcrealworld$invokeSmallButton(volcanoesFromMap);
      case 10:
        return accessor.tfcrealworld$invokeSmallButton(riversFromMap);
      case 11:
        return accessor.tfcrealworld$invokeSmallButton(climateFromMap);
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
    TFCRealWorldConfig.HORIZONTAL_SCALE.set(horizontalScale.get());
    TFCRealWorldConfig.VERTICAL_SCALE.set(verticalScale.get());
    TFCRealWorldConfig.CONTINENT_FROM_MAP.set(continentFromMap.get());
    TFCRealWorldConfig.LAKES_FROM_MAP.set(lakesFromMap.get());
    TFCRealWorldConfig.TECTONICS_FROM_MAP.set(tectonicsFromMap.get());
    TFCRealWorldConfig.VOLCANOES_FROM_MAP.set(volcanoesFromMap.get());
    TFCRealWorldConfig.RIVERS_FROM_MAP.set(riversFromMap.get());
    TFCRealWorldConfig.CLIMATE_FROM_MAP.set(climateFromMap.get());

    TFCRealWorldConfig.saveConfig();

    if (!newProfile.equals(previousProfile)) {
      BasePNGNoise.clearImageCache();
    }
  }
}
