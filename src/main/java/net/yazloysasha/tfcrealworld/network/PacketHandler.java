package net.yazloysasha.tfcrealworld.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

public class PacketHandler {

  private static final String PROTOCOL_VERSION = "1";
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
  }
}
