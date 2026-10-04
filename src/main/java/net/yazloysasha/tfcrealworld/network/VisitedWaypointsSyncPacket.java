package net.yazloysasha.tfcrealworld.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.yazloysasha.tfcrealworld.client.ClientVisitedWaypoints;

public record VisitedWaypointsSyncPacket(Map<String, Long> visited) {
  public static void encode(
    VisitedWaypointsSyncPacket packet,
    FriendlyByteBuf buffer
  ) {
    buffer.writeVarInt(packet.visited.size());
    for (Map.Entry<String, Long> e : packet.visited.entrySet()) {
      buffer.writeUtf(e.getKey());
      buffer.writeLong(e.getValue());
    }
  }

  public static VisitedWaypointsSyncPacket decode(FriendlyByteBuf buffer) {
    int n = buffer.readVarInt();
    Map<String, Long> map = new HashMap<>(n);
    for (int i = 0; i < n; i++) {
      map.put(buffer.readUtf(), buffer.readLong());
    }
    return new VisitedWaypointsSyncPacket(map);
  }

  public static void handle(
    VisitedWaypointsSyncPacket packet,
    Supplier<NetworkEvent.Context> context
  ) {
    context
      .get()
      .enqueueWork(() ->
        DistExecutor.unsafeRunWhenOn(
          Dist.CLIENT,
          () -> () -> ClientVisitedWaypoints.replaceAll(packet.visited())
        )
      );
    context.get().setPacketHandled(true);
  }
}
