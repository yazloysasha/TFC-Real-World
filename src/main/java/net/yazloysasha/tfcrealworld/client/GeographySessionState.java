package net.yazloysasha.tfcrealworld.client;

import org.jetbrains.annotations.Nullable;

/**
 * Persists geography map camera + filters for the current client world session.
 * Cleared on disconnect / logout.
 * <p>
 * Camera uses <b>integer</b> zoom level and pan pixels so markers stay glued to
 * the map blit (no independent float UV rounding).
 */
public final class GeographySessionState {

  /**
   * 1 = fully zoomed out (contain-fit); higher = integer scale factor.
   */
  public static int zoomLevel = 1;

  /**
   * Top-left of the visible contain window in zoomed-map pixel space
   * ({@code drawX = contentLeft - panX}).
   */
  public static int panX = 0;
  public static int panY = 0;

  public static @Nullable String filterContinent = null;
  public static @Nullable String filterRegion = null;
  public static @Nullable String filterSubregion = null;

  private GeographySessionState() {}

  public static void clear() {
    zoomLevel = 1;
    panX = 0;
    panY = 0;
    filterContinent = null;
    filterRegion = null;
    filterSubregion = null;
  }
}
