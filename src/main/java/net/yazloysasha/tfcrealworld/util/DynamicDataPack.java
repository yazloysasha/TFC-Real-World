package net.yazloysasha.tfcrealworld.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.jetbrains.annotations.Nullable;

public class DynamicDataPack implements PackResources {

  protected final PackLocationInfo locationInfo;
  protected static final String TFC_NAMESPACE = "tfc";

  private static final String ADVANCEMENT_PATH =
    "advancement/world/globe_trotter.json";
  private static final String ADVANCEMENT_RESOURCE_PATH =
    "/data/tfc/advancement/world/globe_trotter.json";

  private static String cachedAdvancementJson = null;
  private static int cachedHemisphereScale = -1;

  public DynamicDataPack(PackLocationInfo locationInfo) {
    this.locationInfo = locationInfo;
  }

  @Override
  public @Nullable IoSupplier<InputStream> getRootResource(String... path) {
    return null;
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
      return () ->
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
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

  @Override
  public Set<String> getNamespaces(PackType type) {
    if (type == PackType.SERVER_DATA) {
      return Collections.singleton(TFC_NAMESPACE);
    }
    return Collections.emptySet();
  }

  @Override
  public <T> @Nullable T getMetadataSection(
    MetadataSectionSerializer<T> serializer
  ) {
    if (serializer == PackMetadataSection.TYPE) {
      @SuppressWarnings("unchecked")
      T result = (T) new PackMetadataSection(
        Component.literal("TFC Real World Data"),
        48
      );
      return result;
    }
    return null;
  }

  @Override
  public PackLocationInfo location() {
    return locationInfo;
  }

  @Override
  public void close() {}

  private boolean isCorrectNamespace(String namespace) {
    return namespace.equals(TFC_NAMESPACE);
  }

  private String getAdvancementJson() {
    int currentHemisphereScale =
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get() / 2;

    if (
      cachedAdvancementJson != null &&
      cachedHemisphereScale == currentHemisphereScale
    ) {
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

      String json = template
        .replace(
          "\"{HEMISPHERE_SCALE}\"",
          String.valueOf(currentHemisphereScale)
        )
        .replace(
          "\"{HEMISPHERE_SCALE_NEGATIVE}\"",
          String.valueOf(-currentHemisphereScale)
        );

      cachedAdvancementJson = json;
      cachedHemisphereScale = currentHemisphereScale;
      return json;
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to load advancement template: " + e.getMessage(),
        e
      );
    }
  }

  public static void invalidateCache() {
    cachedAdvancementJson = null;
    cachedHemisphereScale = -1;
  }

  public static void registerPack(AddPackFindersEvent event) {
    if (event.getPackType() == PackType.SERVER_DATA) {
      String packId = TFCRealWorld.MOD_ID + "_data";
      Component packName = Component.literal("TFC: Real World - Data");
      Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
        @Override
        public PackResources openPrimary(PackLocationInfo location) {
          return new DynamicDataPack(location);
        }

        @Override
        public PackResources openFull(
          PackLocationInfo location,
          Pack.Metadata metadata
        ) {
          return new DynamicDataPack(location);
        }
      };

      PackLocationInfo locationInfo = new PackLocationInfo(
        packId,
        packName,
        PackSource.BUILT_IN,
        Optional.empty()
      );

      event.addRepositorySource(consumer -> {
        try {
          PackSelectionConfig selectionConfig = new PackSelectionConfig(
            true,
            Pack.Position.TOP,
            true
          );

          Pack pack = Pack.readMetaAndCreate(
            locationInfo,
            resourcesSupplier,
            event.getPackType(),
            selectionConfig
          );

          if (pack != null) {
            consumer.accept(pack);
          } else {
            TFCRealWorld.LOGGER.warn(
              "Failed to create Pack: readMetaAndCreate returned null for type {}",
              event.getPackType()
            );
          }
        } catch (Exception e) {
          TFCRealWorld.LOGGER.error(
            "Error creating dynamic data pack: {}",
            e.getMessage(),
            e
          );
        }
      });
    }
  }
}
