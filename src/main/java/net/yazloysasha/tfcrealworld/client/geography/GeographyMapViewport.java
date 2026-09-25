package net.yazloysasha.tfcrealworld.client.geography;

import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.client.GeographyMapTexture;
import net.yazloysasha.tfcrealworld.client.GeographySessionState;
import net.yazloysasha.tfcrealworld.client.GeographyUI;

/**
 * Integer zoom/pan camera for the geography overview map.
 * <p>
 * Zoom 1 = contain-fit (margins are panel background). Zoom &gt; 1 expands the
 * viewport to the full map panel so side gutters disappear.
 */
public final class GeographyMapViewport {

  public record View(
    int mapLeft,
    int mapTop,
    int contentLeft,
    int contentTop,
    int baseW,
    int baseH,
    int viewLeft,
    int viewTop,
    int viewW,
    int viewH,
    int drawX,
    int drawY,
    int drawW,
    int drawH,
    int k
  ) {}

  private GeographyMapViewport() {}

  public static View compute(int leftPos, int topPos) {
    GeographyMapTexture.getOrCreate();
    float aspect = Math.max(0.01F, GeographyMapTexture.aspectRatio());
    int mapLeft = leftPos + GeographyUI.MAP_X;
    int mapTop = topPos + GeographyUI.MAP_Y;

    float contain = Math.min(
      GeographyUI.MAP_W / aspect,
      (float) GeographyUI.MAP_H
    );
    int baseW = Math.max(1, Math.round(aspect * contain));
    int baseH = Math.max(1, Math.round(contain));
    baseH = Math.max(1, Math.round(baseW / aspect));
    if (baseW > GeographyUI.MAP_W) {
      baseW = GeographyUI.MAP_W;
      baseH = Math.max(1, Math.round(baseW / aspect));
    }
    if (baseH > GeographyUI.MAP_H) {
      baseH = GeographyUI.MAP_H;
      baseW = Math.max(1, Math.round(baseH * aspect));
    }
    int contentLeft = mapLeft + (GeographyUI.MAP_W - baseW) / 2;
    int contentTop = mapTop + (GeographyUI.MAP_H - baseH) / 2;

    int k = Mth.clamp(
      GeographySessionState.zoomLevel,
      GeographyUI.MIN_ZOOM,
      GeographyUI.MAX_ZOOM
    );
    boolean fillPanel = k > GeographyUI.MIN_ZOOM;
    int viewLeft = fillPanel ? mapLeft : contentLeft;
    int viewTop = fillPanel ? mapTop : contentTop;
    int viewW = fillPanel ? GeographyUI.MAP_W : baseW;
    int viewH = fillPanel ? GeographyUI.MAP_H : baseH;

    int drawW = baseW * k;
    int drawH = baseH * k;
    int drawX = viewLeft - GeographySessionState.panX;
    int drawY = viewTop - GeographySessionState.panY;
    return new View(
      mapLeft,
      mapTop,
      contentLeft,
      contentTop,
      baseW,
      baseH,
      viewLeft,
      viewTop,
      viewW,
      viewH,
      drawX,
      drawY,
      drawW,
      drawH,
      k
    );
  }

  public static int[] toScreen(
    View view,
    int blockX,
    int blockZ,
    int worldMinX,
    int worldMaxX,
    int worldMinZ,
    int worldMaxZ
  ) {
    int spanX = Math.max(1, worldMaxX - worldMinX);
    int spanZ = Math.max(1, worldMaxZ - worldMinZ);
    int sx =
      view.drawX + (int) (((long) (blockX - worldMinX) * view.drawW) / spanX);
    int sy =
      view.drawY + (int) (((long) (blockZ - worldMinZ) * view.drawH) / spanZ);
    return new int[] { sx, sy };
  }

  public static boolean contains(View view, double mx, double my) {
    return (
      mx >= view.viewLeft &&
      mx < view.viewLeft + view.viewW &&
      my >= view.viewTop &&
      my < view.viewTop + view.viewH
    );
  }

  public static void clampPan(int leftPos, int topPos) {
    View view = compute(leftPos, topPos);
    int maxPanX = Math.max(0, view.drawW - view.viewW);
    int maxPanY = Math.max(0, view.drawH - view.viewH);
    GeographySessionState.panX = Mth.clamp(
      GeographySessionState.panX,
      0,
      maxPanX
    );
    GeographySessionState.panY = Mth.clamp(
      GeographySessionState.panY,
      0,
      maxPanY
    );
    if (view.k <= GeographyUI.MIN_ZOOM) {
      GeographySessionState.panX = 0;
      GeographySessionState.panY = 0;
    }
  }

  public static void zoomBy(
    int deltaLevels,
    double mouseX,
    double mouseY,
    int leftPos,
    int topPos
  ) {
    View before = compute(leftPos, topPos);
    int oldK = before.k;
    int newK = Mth.clamp(
      oldK + deltaLevels,
      GeographyUI.MIN_ZOOM,
      GeographyUI.MAX_ZOOM
    );
    if (newK == oldK) {
      return;
    }
    int focusX = Mth.clamp(
      (int) Math.floor(mouseX) - before.viewLeft,
      0,
      Math.max(0, before.viewW - 1)
    );
    int focusY = Mth.clamp(
      (int) Math.floor(mouseY) - before.viewTop,
      0,
      Math.max(0, before.viewH - 1)
    );
    int mapX = GeographySessionState.panX + focusX;
    int mapY = GeographySessionState.panY + focusY;
    GeographySessionState.zoomLevel = newK;
    View after = compute(leftPos, topPos);
    int newFocusX = Mth.clamp(
      (int) Math.floor(mouseX) - after.viewLeft,
      0,
      Math.max(0, after.viewW - 1)
    );
    int newFocusY = Mth.clamp(
      (int) Math.floor(mouseY) - after.viewTop,
      0,
      Math.max(0, after.viewH - 1)
    );
    GeographySessionState.panX =
      (int) Math.round((mapX * (double) newK) / (double) oldK) - newFocusX;
    GeographySessionState.panY =
      (int) Math.round((mapY * (double) newK) / (double) oldK) - newFocusY;
    clampPan(leftPos, topPos);
  }

  public static void panByPixels(
    double dx,
    double dy,
    int leftPos,
    int topPos
  ) {
    GeographySessionState.panX -= (int) Math.round(dx);
    GeographySessionState.panY -= (int) Math.round(dy);
    clampPan(leftPos, topPos);
  }
}
