package net.yazloysasha.tfcrealworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.client.ClientVisitedWaypoints;

public record VisitedWaypointUpdatePacket(String ref, long visitedAt)
  implements CustomPacketPayload {
  public static final Type<VisitedWaypointUpdatePacket> TYPE = new Type<>(
    TFCRealWorld.id("visited_waypoint_update")
  );

  public static final StreamCodec<
    FriendlyByteBuf,
    VisitedWaypointUpdatePacket
  > STREAM_CODEC = StreamCodec.of(
    (buf, packet) -> {
      buf.writeUtf(packet.ref);
      buf.writeLong(packet.visitedAt);
    },
    buf -> new VisitedWaypointUpdatePacket(buf.readUtf(), buf.readLong())
  );

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
    VisitedWaypointUpdatePacket packet,
    IPayloadContext context
  ) {
    context.enqueueWork(() ->
      ClientVisitedWaypoints.put(packet.ref(), packet.visitedAt())
    );
  }
}
