package net.yazloysasha.tfcrealworld.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.yazloysasha.tfcrealworld.client.ClientVisitedWaypoints;

public record VisitedWaypointUpdatePacket(String ref, long visitedAt) {
  public static void encode(
    VisitedWaypointUpdatePacket packet,
    FriendlyByteBuf buffer
  ) {
    buffer.writeUtf(packet.ref);
    buffer.writeLong(packet.visitedAt);
  }

  public static VisitedWaypointUpdatePacket decode(FriendlyByteBuf buffer) {
    return new VisitedWaypointUpdatePacket(buffer.readUtf(), buffer.readLong());
  }

  public static void handle(
    VisitedWaypointUpdatePacket packet,
    Supplier<NetworkEvent.Context> context
  ) {
    context
      .get()
      .enqueueWork(() ->
        DistExecutor.unsafeRunWhenOn(
          Dist.CLIENT,
          () ->
            () -> ClientVisitedWaypoints.put(packet.ref(), packet.visitedAt())
        )
      );
    context.get().setPacketHandled(true);
  }
}
