package net.yazloysasha.tfcrealworld.util;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

public class DynamicPackFinder {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void registerPack(AddPackFindersEvent event) {
    if (
      event.getPackType() == PackType.CLIENT_RESOURCES ||
      event.getPackType() == PackType.SERVER_DATA
    ) {
      String packId;
      Component packName;
      Pack.ResourcesSupplier resourcesSupplier;

      if (event.getPackType() == PackType.CLIENT_RESOURCES) {
        packId = "tfc_real_world_assets";
        packName = Component.literal("TFC: Real World - Assets");
        resourcesSupplier = new Pack.ResourcesSupplier() {
          @Override
          public PackResources openPrimary(PackLocationInfo location) {
            return new DynamicResourcePack(location);
          }

          @Override
          public PackResources openFull(
            PackLocationInfo location,
            Pack.Metadata metadata
          ) {
            return new DynamicResourcePack(location);
          }
        };
      } else {
        packId = "tfc_real_world_data";
        packName = Component.literal("TFC: Real World - Data");
        resourcesSupplier = new Pack.ResourcesSupplier() {
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
      }

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
            LOGGER.info(
              "Successfully registered dynamic pack: {} (type: {})",
              pack.getId(),
              event.getPackType()
            );
            consumer.accept(pack);
          } else {
            LOGGER.warn(
              "Failed to create Pack: readMetaAndCreate returned null for type {}",
              event.getPackType()
            );
          }
        } catch (Exception e) {
          LOGGER.error(
            "Error creating dynamic resource pack: {}",
            e.getMessage(),
            e
          );
        }
      });
    }
  }
}
