package net.yazloysasha.tfcrealworld.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Serverbound: close any open container, then tell the client to open the
 * standalone Geography screen (mirrors BOOK → Patchouli large GUI flow).
 */
public final class OpenGeographyTabPacket {

  public static void encode(
    OpenGeographyTabPacket packet,
    FriendlyByteBuf buffer
  ) {}

  public static OpenGeographyTabPacket decode(FriendlyByteBuf buffer) {
    return new OpenGeographyTabPacket();
  }

  public static void handle(
    OpenGeographyTabPacket packet,
    Supplier<NetworkEvent.Context> context
  ) {
    context
      .get()
      .enqueueWork(() -> {
        final ServerPlayer player = context.get().getSender();
        if (player != null) {
          player.doCloseContainer();
          PacketHandler.sendToPlayer(player, new OpenGeographyScreenPacket());
        }
      });
    context.get().setPacketHandled(true);
  }
}
