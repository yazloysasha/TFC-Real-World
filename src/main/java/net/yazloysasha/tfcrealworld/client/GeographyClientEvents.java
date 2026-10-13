package net.yazloysasha.tfcrealworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.yazloysasha.tfcrealworld.client.screen.GeographyScreen;

public final class GeographyClientEvents {

  private GeographyClientEvents() {}

  public static void openGeographyScreen() {
    Minecraft.getInstance().setScreen(
      new GeographyScreen(
        new TranslatableComponent("tfc_real_world.screen.geography")
      )
    );
  }
}
