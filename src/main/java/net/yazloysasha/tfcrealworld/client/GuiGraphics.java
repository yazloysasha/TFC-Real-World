package net.yazloysasha.tfcrealworld.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The drawing calls of Minecraft 1.20's GuiGraphics, on a pose stack. */
public final class GuiGraphics {

  private final PoseStack pose;

  public GuiGraphics(PoseStack pose) {
    this.pose = pose;
  }

  public PoseStack pose() {
    return pose;
  }

  public void fill(int minX, int minY, int maxX, int maxY, int color) {
    GuiComponent.fill(pose, minX, minY, maxX, maxY, color);
  }

  public void renderOutline(int x, int y, int width, int height, int color) {
    fill(x, y, x + width, y + 1, color);
    fill(x, y + height - 1, x + width, y + height, color);
    fill(x, y + 1, x + 1, y + height - 1, color);
    fill(x + width - 1, y + 1, x + width, y + height - 1, color);
  }

  public int drawString(
    Font font,
    Component text,
    int x,
    int y,
    int color,
    boolean shadow
  ) {
    return shadow
      ? font.drawShadow(pose, text, x, y, color)
      : font.draw(pose, text, x, y, color);
  }

  public int drawString(
    Font font,
    String text,
    int x,
    int y,
    int color,
    boolean shadow
  ) {
    return shadow
      ? font.drawShadow(pose, text, x, y, color)
      : font.draw(pose, text, x, y, color);
  }

  public void blit(
    ResourceLocation texture,
    int x,
    int y,
    int width,
    int height,
    float u,
    float v,
    int uWidth,
    int vHeight,
    int textureWidth,
    int textureHeight
  ) {
    RenderSystem.setShaderTexture(0, texture);
    GuiComponent.blit(
      pose,
      x,
      y,
      width,
      height,
      u,
      v,
      uWidth,
      vHeight,
      textureWidth,
      textureHeight
    );
  }

  public void blit(
    ResourceLocation texture,
    int x,
    int y,
    float u,
    float v,
    int width,
    int height,
    int textureWidth,
    int textureHeight
  ) {
    RenderSystem.setShaderTexture(0, texture);
    GuiComponent.blit(
      pose,
      x,
      y,
      u,
      v,
      width,
      height,
      textureWidth,
      textureHeight
    );
  }

  public void setColor(float red, float green, float blue, float alpha) {
    RenderSystem.setShaderColor(red, green, blue, alpha);
  }

  /** Scissor in GUI coordinates, untransformed by the pose. */
  public void enableScissor(int minX, int minY, int maxX, int maxY) {
    final Window window = Minecraft.getInstance().getWindow();
    final double scale = window.getGuiScale();
    RenderSystem.enableScissor(
      (int) (minX * scale),
      (int) (window.getHeight() - maxY * scale),
      Math.max(0, (int) ((maxX - minX) * scale)),
      Math.max(0, (int) ((maxY - minY) * scale))
    );
  }

  public void disableScissor() {
    RenderSystem.disableScissor();
  }

  public void renderComponentTooltip(
    Font font,
    List<Component> lines,
    int mouseX,
    int mouseY
  ) {
    final Screen screen = Minecraft.getInstance().screen;
    if (screen != null) {
      screen.renderTooltip(pose, lines, Optional.empty(), mouseX, mouseY, font);
    }
  }
}
