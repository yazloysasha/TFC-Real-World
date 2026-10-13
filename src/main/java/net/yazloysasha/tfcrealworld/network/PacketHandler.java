package net.yazloysasha.tfcrealworld.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

public class PacketHandler {

  private static final String PROTOCOL_VERSION = "4";

  public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
    new ResourceLocation(TFCRealWorld.MOD_ID, "main"),
    () -> PROTOCOL_VERSION,
    PROTOCOL_VERSION::equals,
    PROTOCOL_VERSION::equals
  );

  private static int id = 0;

  public static void register() {
    INSTANCE.registerMessage(
      id++,
      ConfigSyncPacket.class,
      ConfigSyncPacket::encode,
      ConfigSyncPacket::decode,
      ConfigSyncPacket::handle
    );
    INSTANCE.registerMessage(
      id++,
      OpenGeographyTabPacket.class,
      OpenGeographyTabPacket::encode,
      OpenGeographyTabPacket::decode,
      OpenGeographyTabPacket::handle
    );
    INSTANCE.registerMessage(
      id++,
      OpenGeographyScreenPacket.class,
      OpenGeographyScreenPacket::encode,
      OpenGeographyScreenPacket::decode,
      OpenGeographyScreenPacket::handle
    );
    INSTANCE.registerMessage(
      id++,
      VisitedWaypointsSyncPacket.class,
      VisitedWaypointsSyncPacket::encode,
      VisitedWaypointsSyncPacket::decode,
      VisitedWaypointsSyncPacket::handle
    );
    INSTANCE.registerMessage(
      id++,
      VisitedWaypointUpdatePacket.class,
      VisitedWaypointUpdatePacket::encode,
      VisitedWaypointUpdatePacket::decode,
      VisitedWaypointUpdatePacket::handle
    );
  }

  public static void sendToPlayer(ServerPlayer player, Object packet) {
    INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
  }
}
