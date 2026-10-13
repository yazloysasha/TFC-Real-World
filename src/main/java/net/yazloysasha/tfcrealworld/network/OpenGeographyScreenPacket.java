package net.yazloysasha.tfcrealworld.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.yazloysasha.tfcrealworld.client.GeographyClientEvents;

/**
 * Clientbound: open the standalone Geography screen (BOOK-like, not an
 * inventory menu). The screen is only touched on the client.
 */
public final class OpenGeographyScreenPacket {

  public static void encode(
    OpenGeographyScreenPacket packet,
    FriendlyByteBuf buffer
  ) {}

  public static OpenGeographyScreenPacket decode(FriendlyByteBuf buffer) {
    return new OpenGeographyScreenPacket();
  }

  public static void handle(
    OpenGeographyScreenPacket packet,
    Supplier<NetworkEvent.Context> context
  ) {
    context
      .get()
      .enqueueWork(() ->
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
          GeographyClientEvents::openGeographyScreen
        )
      );
    context.get().setPacketHandled(true);
  }
}
