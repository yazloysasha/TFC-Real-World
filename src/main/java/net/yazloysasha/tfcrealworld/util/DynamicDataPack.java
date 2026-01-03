package net.yazloysasha.tfcrealworld.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public class DynamicDataPack extends BaseDynamicPack {

  private static final String ADVANCEMENT_PATH =
    "advancement/world/globe_trotter.json";
  private static final String ADVANCEMENT_RESOURCE_PATH =
    "/data/tfc/advancement/world/globe_trotter.json";

  private static String cachedAdvancementJson = null;

  public DynamicDataPack(PackLocationInfo locationInfo) {
    super(locationInfo);
  }

  private String getAdvancementJson() {
    if (cachedAdvancementJson != null) {
      return cachedAdvancementJson;
    }

    try (
      InputStream resourceStream =
        DynamicDataPack.class.getResourceAsStream(ADVANCEMENT_RESOURCE_PATH)
    ) {
      if (resourceStream == null) {
        throw new IllegalStateException(
          "Advancement template not found at: " + ADVANCEMENT_RESOURCE_PATH
        );
      }

      String template = new String(
        resourceStream.readAllBytes(),
        StandardCharsets.UTF_8
      );

      int[] scaleValues = getWorldScaleValues();
      int halfScale = scaleValues[0];
      int negativeHalfScale = scaleValues[1];

      String json = template
        .replace("\"{WORLD_SCALE_HALF}\"", String.valueOf(halfScale))
        .replace(
          "\"{WORLD_SCALE_NEGATIVE_HALF}\"",
          String.valueOf(negativeHalfScale)
        );

      cachedAdvancementJson = json;
      return json;
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to load advancement template: " + e.getMessage(),
        e
      );
    }
  }

  @Override
  protected String getPackDisplayName() {
    return "TFC Real World Data";
  }

  @Override
  protected PackType getSupportedPackType() {
    return PackType.SERVER_DATA;
  }

  @Override
  protected int getPackFormat() {
    return 48;
  }

  @Override
  public @Nullable IoSupplier<InputStream> getResource(
    PackType type,
    ResourceLocation location
  ) {
    if (
      type == PackType.SERVER_DATA &&
      isCorrectNamespace(location.getNamespace()) &&
      location.getPath().equals(ADVANCEMENT_PATH)
    ) {
      String json = getAdvancementJson();
      return createJsonInputStream(json);
    }

    return null;
  }

  @Override
  public void listResources(
    PackType type,
    String namespace,
    String path,
    ResourceOutput output
  ) {
    if (
      type == PackType.SERVER_DATA &&
      isCorrectNamespace(namespace) &&
      (path.equals("advancement") || path.equals("advancement/world"))
    ) {
      ResourceLocation advancementLocation =
        ResourceLocation.fromNamespaceAndPath(TFC_NAMESPACE, ADVANCEMENT_PATH);
      IoSupplier<InputStream> resource = getResource(type, advancementLocation);
      if (resource != null) {
        output.accept(advancementLocation, resource);
      }
    }
  }
}
