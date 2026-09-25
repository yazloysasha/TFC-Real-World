package net.yazloysasha.tfcrealworld.client.geography;

import net.yazloysasha.tfcrealworld.util.geography.GeographyNode;
import org.jetbrains.annotations.Nullable;

/**
 * Waypoint plotted into classic block XZ for the geography map.
 */
public record PlottedWaypoint(
  GeographyNode node,
  int x,
  int z,
  @Nullable String continentRef,
  @Nullable String regionRef,
  @Nullable String subregionRef
) {}
