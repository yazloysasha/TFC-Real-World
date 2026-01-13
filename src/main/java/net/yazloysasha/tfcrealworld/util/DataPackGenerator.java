package net.yazloysasha.tfcrealworld.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

public class DataPackGenerator {

  private static final Gson GSON = new Gson();
  private static final String DATAPACK_NAME = "tfc_real_world.zip";
  private static final String ADVANCEMENT_PATH =
    "data/tfc/advancement/world/globe_trotter.json";
  private static final String PACK_MCMETA_PATH = "pack.mcmeta";
  private static final ResourceLocation TFC_ADVANCEMENT_LOCATION =
    ResourceLocation.fromNamespaceAndPath(
      "tfc",
      "advancement/world/globe_trotter.json"
    );

  public static void generateDataPack(MinecraftServer server) {
    try {
      Path worldPath = server.getWorldPath(LevelResource.ROOT);
      Path datapacksDir = worldPath.resolve("datapacks");
      Path datapackFile = datapacksDir.resolve(DATAPACK_NAME);

      if (!Files.exists(datapacksDir)) {
        Files.createDirectories(datapacksDir);
      }

      int hemisphereScale = TFCRealWorldConfig.VERTICAL_TILE_SIZE.get() / 2;

      ResourceManager resourceManager = server.getResourceManager();
      Optional<Resource> resourceOpt = resourceManager.getResource(
        TFC_ADVANCEMENT_LOCATION
      );

      if (resourceOpt.isEmpty()) {
        TFCRealWorld.LOGGER.error(
          "TFC advancement not found: {}",
          TFC_ADVANCEMENT_LOCATION
        );
        return;
      }

      Resource resource = resourceOpt.get();
      try (
        InputStream resourceStream = resource.open();
        ZipOutputStream zos = new ZipOutputStream(
          Files.newOutputStream(datapackFile)
        )
      ) {
        JsonObject json;
        try {
          json = GSON.fromJson(
            new InputStreamReader(resourceStream, StandardCharsets.UTF_8),
            JsonObject.class
          );
        } catch (Exception e) {
          TFCRealWorld.LOGGER.error(
            "Failed to parse TFC advancement JSON from {}: {}",
            TFC_ADVANCEMENT_LOCATION,
            e.getMessage(),
            e
          );
          return;
        }

        if (json == null) {
          TFCRealWorld.LOGGER.error(
            "TFC advancement JSON is null from: {}",
            TFC_ADVANCEMENT_LOCATION
          );
          return;
        }

        JsonObject criteria = json.getAsJsonObject("criteria");
        JsonObject high = criteria.getAsJsonObject("high");
        JsonObject highConditions = high.getAsJsonObject("conditions");
        JsonObject highPlayer = highConditions
          .getAsJsonArray("player")
          .get(0)
          .getAsJsonObject();
        JsonObject highPredicate = highPlayer.getAsJsonObject("predicate");
        JsonObject highLocation = highPredicate.getAsJsonObject("location");
        JsonObject highPosition = highLocation.getAsJsonObject("position");
        JsonObject highZ = highPosition.getAsJsonObject("z");
        highZ.addProperty("min", hemisphereScale);

        JsonObject low = criteria.getAsJsonObject("low");
        JsonObject lowConditions = low.getAsJsonObject("conditions");
        JsonObject lowPlayer = lowConditions
          .getAsJsonArray("player")
          .get(0)
          .getAsJsonObject();
        JsonObject lowPredicate = lowPlayer.getAsJsonObject("predicate");
        JsonObject lowLocation = lowPredicate.getAsJsonObject("location");
        JsonObject lowPosition = lowLocation.getAsJsonObject("position");
        JsonObject lowZ = lowPosition.getAsJsonObject("z");
        lowZ.addProperty("max", -hemisphereScale);

        String minifiedJson = GSON.toJson(json);

        ZipEntry advancementEntry = new ZipEntry(ADVANCEMENT_PATH);
        zos.putNextEntry(advancementEntry);
        zos.write(minifiedJson.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        JsonObject packMeta = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 9);
        pack.addProperty("description", TFCRealWorld.MOD_ID + "_data");
        packMeta.add("pack", pack);
        String minifiedPackMeta = GSON.toJson(packMeta);

        ZipEntry packMetaEntry = new ZipEntry(PACK_MCMETA_PATH);
        zos.putNextEntry(packMetaEntry);
        zos.write(minifiedPackMeta.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        TFCRealWorld.LOGGER.info(
          "Generated datapack {} with hemisphere scale: {}",
          datapackFile,
          hemisphereScale
        );
      }
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error("Failed to generate datapack", e);
    }
  }
}
