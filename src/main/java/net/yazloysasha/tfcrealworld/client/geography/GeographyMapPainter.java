package net.yazloysasha.tfcrealworld.client.geography;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.yazloysasha.tfcrealworld.client.ClientVisitedWaypoints;
import net.yazloysasha.tfcrealworld.client.GeographyMapTexture;
import net.yazloysasha.tfcrealworld.client.GeographyUI;

/**
 * Blits the overview map, waypoint orbs, and player marker into a viewport.
 */
public final class GeographyMapPainter {

  private static final ResourceLocation XP_ORB =
    ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
  private static final int ORB_U = 48;
  private static final int ORB_V = 0;

  private GeographyMapPainter() {}

  /**
   * Pixel size of the drawn orb — also used by hit-testing.
   */
  public static int orbSize(boolean capital) {
    return capital ? GeographyUI.ORB_SIZE_CAPITAL : GeographyUI.ORB_SIZE_NORMAL;
  }

  public static void render(
    GuiGraphics graphics,
    GeographyMapViewport.View view,
    List<PlottedWaypoint> waypoints,
    int worldMinX,
    int worldMaxX,
    int worldMinZ,
    int worldMaxZ,
    Minecraft minecraft
  ) {
    graphics.fill(
      view.mapLeft(),
      view.mapTop(),
      view.mapLeft() + GeographyUI.MAP_W,
      view.mapTop() + GeographyUI.MAP_H,
      GeographyUI.PANEL_BG_ARGB
    );

    graphics.enableScissor(
      view.viewLeft(),
      view.viewTop(),
      view.viewLeft() + view.viewW(),
      view.viewTop() + view.viewH()
    );

    ResourceLocation map = GeographyMapTexture.getOrCreate();
    int tw = GeographyMapTexture.width();
    int th = GeographyMapTexture.height();
    graphics.blit(
      map,
      view.drawX(),
      view.drawY(),
      view.drawW(),
      view.drawH(),
      0,
      0,
      tw,
      th,
      tw,
      th
    );

    for (PlottedWaypoint w : waypoints) {
      int[] screen = GeographyMapViewport.toScreen(
        view,
        w.x(),
        w.z(),
        worldMinX,
        worldMaxX,
        worldMinZ,
        worldMaxZ
      );
      if (
        screen[0] < view.viewLeft() - 8 ||
        screen[0] > view.viewLeft() + view.viewW() + 8 ||
        screen[1] < view.viewTop() - 8 ||
        screen[1] > view.viewTop() + view.viewH() + 8
      ) {
        continue;
      }
      drawOrb(graphics, w, screen[0], screen[1]);
    }

    drawPlayerMarker(
      graphics,
      view,
      worldMinX,
      worldMaxX,
      worldMinZ,
      worldMaxZ,
      minecraft
    );

    graphics.disableScissor();
    graphics.renderOutline(
      view.mapLeft(),
      view.mapTop(),
      GeographyUI.MAP_W,
      GeographyUI.MAP_H,
      GeographyUI.PANEL_OUTLINE_ARGB
    );
  }

  private static void drawOrb(
    GuiGraphics graphics,
    PlottedWaypoint w,
    int cx,
    int cy
  ) {
    boolean visited = ClientVisitedWaypoints.isVisited(w.node().ref());
    boolean capital = w.node().capital();
    int size = orbSize(capital);
    float r;
    float g;
    float b;
    if (visited) {
      r = capital ? 0.55F : 0.35F;
      g = capital ? 1.0F : 0.95F;
      b = capital ? 0.35F : 0.40F;
    } else {
      r = capital ? 0.22F : 0.12F;
      g = capital ? 0.20F : 0.12F;
      b = capital ? 0.18F : 0.14F;
    }
    graphics.setColor(r, g, b, 1.0F);
    graphics.blit(
      XP_ORB,
      cx - size / 2,
      cy - size / 2,
      size,
      size,
      ORB_U,
      ORB_V,
      16,
      16,
      64,
      64
    );
    graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
  }

  private static void drawPlayerMarker(
    GuiGraphics graphics,
    GeographyMapViewport.View view,
    int worldMinX,
    int worldMaxX,
    int worldMinZ,
    int worldMaxZ,
    Minecraft minecraft
  ) {
    if (minecraft == null || minecraft.player == null) {
      return;
    }
    double px = minecraft.player.getX();
    double pz = minecraft.player.getZ();
    if (px < worldMinX || px > worldMaxX || pz < worldMinZ || pz > worldMaxZ) {
      return;
    }
    int[] screen = GeographyMapViewport.toScreen(
      view,
      (int) Math.round(px),
      (int) Math.round(pz),
      worldMinX,
      worldMaxX,
      worldMinZ,
      worldMaxZ
    );
    if (
      screen[0] < view.viewLeft() ||
      screen[0] >= view.viewLeft() + view.viewW() ||
      screen[1] < view.viewTop() ||
      screen[1] >= view.viewTop() + view.viewH()
    ) {
      return;
    }

    float yaw = minecraft.player.getYRot();
    MapDecoration decoration = new MapDecoration(
      MapDecorationTypes.PLAYER,
      (byte) 0,
      (byte) 0,
      (byte) 0,
      Optional.empty()
    );
    TextureAtlasSprite sprite = minecraft
      .getMapDecorationTextures()
      .get(decoration);

    int size = 8;
    graphics.pose().pushPose();
    graphics.pose().translate(screen[0], screen[1], 0);
    graphics
      .pose()
      .mulPose(com.mojang.math.Axis.ZP.rotationDegrees(yaw + 180.0F));
    graphics.blit(-size / 2, -size / 2, 0, size, size, sprite);
    graphics.pose().popPose();
  }
}
