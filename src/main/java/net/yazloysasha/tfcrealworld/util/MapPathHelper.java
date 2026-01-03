package net.yazloysasha.tfcrealworld.util;

import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

public class MapPathHelper {

  private static final String MAPS_DIR = "config/tfc_real_world/maps";

  public static Path getMapsDirectory() {
    return FMLPaths.GAMEDIR.get().resolve(MAPS_DIR);
  }

  public static Path getMapPath(String mapName) {
    return getMapsDirectory().resolve(mapName + ".png");
  }
}
