package net.yazloysasha.tfcrealworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.yazloysasha.tfcrealworld.client.screen.GeographyScreen;

public final class GeographyClientEvents {

  private GeographyClientEvents() {}

  public static void openGeographyScreen() {
    Minecraft.getInstance().setScreen(
      new GeographyScreen(
        Component.translatable("tfc_real_world.screen.geography")
      )
    );
  }
}
