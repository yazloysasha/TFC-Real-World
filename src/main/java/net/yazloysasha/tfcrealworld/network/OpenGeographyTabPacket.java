package net.yazloysasha.tfcrealworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

/**
 * Serverbound: close any open container, then tell the client to open the
 * standalone Geography screen (mirrors BOOK → Patchouli large GUI flow).
 */
public record OpenGeographyTabPacket() implements CustomPacketPayload {
  public static final Type<OpenGeographyTabPacket> TYPE = new Type<>(
    TFCRealWorld.id("open_geography_tab")
  );

  public static final StreamCodec<
    FriendlyByteBuf,
    OpenGeographyTabPacket
  > STREAM_CODEC = StreamCodec.unit(new OpenGeographyTabPacket());

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
    OpenGeographyTabPacket packet,
    IPayloadContext context
  ) {
    context.enqueueWork(() -> {
      if (context.player() instanceof ServerPlayer player) {
        player.doCloseContainer();
        PacketDistributor.sendToPlayer(player, new OpenGeographyScreenPacket());
      }
    });
  }
}
