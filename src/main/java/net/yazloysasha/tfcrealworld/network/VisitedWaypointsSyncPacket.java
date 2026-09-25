package net.yazloysasha.tfcrealworld.network;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.client.ClientVisitedWaypoints;

public record VisitedWaypointsSyncPacket(Map<String, Long> visited)
  implements CustomPacketPayload {
  public static final Type<VisitedWaypointsSyncPacket> TYPE = new Type<>(
    TFCRealWorld.id("visited_waypoints_sync")
  );

  public static final StreamCodec<
    FriendlyByteBuf,
    VisitedWaypointsSyncPacket
  > STREAM_CODEC = StreamCodec.of(
    (buf, packet) -> {
      buf.writeVarInt(packet.visited.size());
      for (Map.Entry<String, Long> e : packet.visited.entrySet()) {
        buf.writeUtf(e.getKey());
        buf.writeLong(e.getValue());
      }
    },
    buf -> {
      int n = buf.readVarInt();
      Map<String, Long> map = new HashMap<>(n);
      for (int i = 0; i < n; i++) {
        map.put(buf.readUtf(), buf.readLong());
      }
      return new VisitedWaypointsSyncPacket(map);
    }
  );

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
    VisitedWaypointsSyncPacket packet,
    IPayloadContext context
  ) {
    context.enqueueWork(() ->
      ClientVisitedWaypoints.replaceAll(packet.visited())
    );
  }
}
