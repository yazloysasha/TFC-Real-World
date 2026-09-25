package net.yazloysasha.tfcrealworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

/**
 * Clientbound: open the standalone Geography screen (BOOK-like, not an inventory menu).
 * Handler is registered from the client entry so we never touch Screen classes on dedicated.
 */
public record OpenGeographyScreenPacket() implements CustomPacketPayload {
  public static final Type<OpenGeographyScreenPacket> TYPE = new Type<>(
    TFCRealWorld.id("open_geography_screen")
  );

  public static final StreamCodec<
    FriendlyByteBuf,
    OpenGeographyScreenPacket
  > STREAM_CODEC = StreamCodec.unit(new OpenGeographyScreenPacket());

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  /**
   * No-op placeholder; real client handler is wired in {@code TFCRealWorld}.
   */
  public static void handle(
    OpenGeographyScreenPacket packet,
    IPayloadContext context
  ) {
    // Client-side handler registered separately (see GeographyClientEvents).
  }
}
