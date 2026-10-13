package net.yazloysasha.tfcrealworld.world.tectonics;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.LandRelief;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Volcanism;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.jetbrains.annotations.Nullable;

/**
 * What kind of place each region cell is, as TFC can build it. TFC has no
 * hotspots, so a hotspot of the map that raises land is described by what
 * it is: a volcanic mountain, risen from the sea where it stands in it, and
 * coastal where the sea of the map is at hand. Every task reads the
 * place from here, so none of them knows about hotspots.
 */
public final class TectonicPlaces {

  /** Age of an ancient hotspot, the only one that raises no land. */
  private static final byte ANCIENT_HOTSPOT = 4;

  private final TectonicsMap map;

  /** The map's hotspots. */
  @Nullable
  private final MapHotspotLayout volcanoes;

  @Nullable
  private final PNGContinentNoise continent;

  private TectonicPlaces(
    TectonicsMap map,
    @Nullable MapHotspotLayout volcanoes,
    @Nullable PNGContinentNoise continent
  ) {
    this.map = map;
    this.volcanoes = volcanoes;
    this.continent = continent;
  }

  /** @return the places of the generator's map, or null without tectonics */
  @Nullable
  public static TectonicPlaces of(RegionGenerator generator) {
    final TectonicsMap map = TectonicsRegistry.get(generator);
    if (map == null) {
      return null;
    }
    return new TectonicPlaces(
      map,
      TectonicsRegistry.hotspotLayout(),
      ContinentNoiseRegistry.get(generator)
    );
  }

  public TectonicsMap map() {
    return map;
  }

  public TectonicClass at(int gridX, int gridZ) {
    final TectonicClass place = map.classAtGrid(gridX, gridZ);
    final MapHotspotLayout.Center volcano = volcanoAt(gridX, gridZ);
    if (volcano == null) {
      return place;
    }
    return new TectonicClass(
      place.boundary(),
      LandRelief.MOUNTAIN,
      place.water(),
      place.volcanism() == Volcanism.NONE
        ? Volcanism.INTRAPLATE
        : place.volcanism(),
      touchesTheSea(gridX, gridZ),
      place.atolls(),
      volcano.age()
    );
  }

  /**
   * Whether the cell lies on the sea of continent.png or beside it: the
   * part of a volcano that is a coastal mountain.
   */
  private boolean touchesTheSea(int gridX, int gridZ) {
    if (continent == null) {
      return false;
    }
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        if (continent.isOceanAtGridHard(gridX + dx + 0.5, gridZ + dz + 0.5)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Whether the cell is land whatever continent.png has there. */
  public boolean risesFromTheSea(int gridX, int gridZ) {
    return volcanoAt(gridX, gridZ) != null;
  }

  @Nullable
  private MapHotspotLayout.Center volcanoAt(int gridX, int gridZ) {
    if (volcanoes == null) {
      return null;
    }
    final MapHotspotLayout.Center hotspot = volcanoes.nearestCenterAtGrid(
      gridX,
      gridZ
    );
    return hotspot != null &&
      hotspot.age() > 0 &&
      hotspot.age() != ANCIENT_HOTSPOT
      ? hotspot
      : null;
  }
}
