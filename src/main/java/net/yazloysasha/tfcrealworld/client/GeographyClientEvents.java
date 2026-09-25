package net.yazloysasha.tfcrealworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.client.screen.GeographyScreen;
import net.yazloysasha.tfcrealworld.network.OpenGeographyScreenPacket;

public final class GeographyClientEvents {

  private GeographyClientEvents() {}

  public static void handleOpenGeographyScreen(
    OpenGeographyScreenPacket packet,
    IPayloadContext context
  ) {
    context.enqueueWork(() ->
      Minecraft.getInstance()
        .setScreen(
          new GeographyScreen(
            Component.translatable("tfc_real_world.screen.geography")
          )
        )
    );
  }
}
