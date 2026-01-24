package net.yazloysasha.tfcrealworld.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.SpawnMode;

public class ConfigSyncPacket {

  private final String mapProfile;
  private final SpawnMode spawnMode;
  private final double spawnCenterLongitude;
  private final double spawnCenterLatitude;
  private final int spawnCenterX;
  private final int spawnCenterZ;
  private final int spawnDistance;
  private final boolean flatBedrock;
  private final double continentalness;
  private final int temperatureScale;
  private final int rainfallScale;
  private final int horizontalScale;
  private final int verticalScale;
  private final boolean continentFromMap;
  private final boolean altitudeFromMap;
  private final boolean hotspotsFromMap;
  private final boolean koppenFromMap;

  public ConfigSyncPacket(
    String mapProfile,
    SpawnMode spawnMode,
    double spawnCenterLongitude,
    double spawnCenterLatitude,
    int spawnCenterX,
    int spawnCenterZ,
    int spawnDistance,
    boolean flatBedrock,
    double continentalness,
    int temperatureScale,
    int rainfallScale,
    int horizontalScale,
    int verticalScale,
    boolean continentFromMap,
    boolean altitudeFromMap,
    boolean hotspotsFromMap,
    boolean koppenFromMap
  ) {
    this.mapProfile = mapProfile;
    this.spawnMode = spawnMode;
    this.spawnCenterLongitude = spawnCenterLongitude;
    this.spawnCenterLatitude = spawnCenterLatitude;
    this.spawnCenterX = spawnCenterX;
    this.spawnCenterZ = spawnCenterZ;
    this.spawnDistance = spawnDistance;
    this.flatBedrock = flatBedrock;
    this.continentalness = continentalness;
    this.temperatureScale = temperatureScale;
    this.rainfallScale = rainfallScale;
    this.horizontalScale = horizontalScale;
    this.verticalScale = verticalScale;
    this.continentFromMap = continentFromMap;
    this.altitudeFromMap = altitudeFromMap;
    this.hotspotsFromMap = hotspotsFromMap;
    this.koppenFromMap = koppenFromMap;
  }

  public static void encode(ConfigSyncPacket packet, FriendlyByteBuf buffer) {
    buffer.writeUtf(packet.mapProfile);
    buffer.writeEnum(packet.spawnMode);
    buffer.writeDouble(packet.spawnCenterLongitude);
    buffer.writeDouble(packet.spawnCenterLatitude);
    buffer.writeInt(packet.spawnCenterX);
    buffer.writeInt(packet.spawnCenterZ);
    buffer.writeInt(packet.spawnDistance);
    buffer.writeBoolean(packet.flatBedrock);
    buffer.writeDouble(packet.continentalness);
    buffer.writeInt(packet.temperatureScale);
    buffer.writeInt(packet.rainfallScale);
    buffer.writeInt(packet.horizontalScale);
    buffer.writeInt(packet.verticalScale);
    buffer.writeBoolean(packet.continentFromMap);
    buffer.writeBoolean(packet.altitudeFromMap);
    buffer.writeBoolean(packet.hotspotsFromMap);
    buffer.writeBoolean(packet.koppenFromMap);
  }

  public static ConfigSyncPacket decode(FriendlyByteBuf buffer) {
    return new ConfigSyncPacket(
      buffer.readUtf(),
      buffer.readEnum(SpawnMode.class),
      buffer.readDouble(),
      buffer.readDouble(),
      buffer.readInt(),
      buffer.readInt(),
      buffer.readInt(),
      buffer.readBoolean(),
      buffer.readDouble(),
      buffer.readInt(),
      buffer.readInt(),
      buffer.readInt(),
      buffer.readInt(),
      buffer.readBoolean(),
      buffer.readBoolean(),
      buffer.readBoolean(),
      buffer.readBoolean()
    );
  }

  public static void handle(
    ConfigSyncPacket packet,
    Supplier<NetworkEvent.Context> ctx
  ) {
    ctx
      .get()
      .enqueueWork(() -> {
        TFCRealWorldConfig.setServerConfig(
          packet.mapProfile,
          packet.spawnMode,
          packet.spawnCenterLongitude,
          packet.spawnCenterLatitude,
          packet.spawnCenterX,
          packet.spawnCenterZ,
          packet.spawnDistance,
          packet.flatBedrock,
          packet.continentalness,
          packet.temperatureScale,
          packet.rainfallScale,
          packet.horizontalScale,
          packet.verticalScale,
          packet.continentFromMap,
          packet.altitudeFromMap,
          packet.hotspotsFromMap,
          packet.koppenFromMap
        );
      });
    ctx.get().setPacketHandled(true);
  }
}
