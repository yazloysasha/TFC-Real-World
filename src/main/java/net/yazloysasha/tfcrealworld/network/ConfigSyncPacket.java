package net.yazloysasha.tfcrealworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.SpawnMode;

public record ConfigSyncPacket(
  String mapProfile,
  SpawnMode spawnMode,
  double spawnCenterLongitude,
  double spawnCenterLatitude,
  int spawnCenterX,
  int spawnCenterZ,
  int spawnDistance,
  boolean canyonsNotVolcanic,
  boolean flatBedrock,
  boolean finiteContinents,
  double continentalness,
  double grassDensity,
  double temperatureConstant,
  double rainfallConstant,
  int temperatureScale,
  int rainfallScale,
  int horizontalTileSize,
  int verticalTileSize,
  boolean continentFromMap,
  boolean altitudeFromMap,
  boolean hotspotsFromMap,
  boolean koppenFromMap
)
  implements CustomPacketPayload {
  public static final Type<ConfigSyncPacket> TYPE = new Type<>(
    ResourceLocation.fromNamespaceAndPath(TFCRealWorld.MOD_ID, "config_sync")
  );

  public static final StreamCodec<
    FriendlyByteBuf,
    ConfigSyncPacket
  > STREAM_CODEC = StreamCodec.of(
    (buffer, packet) -> {
      buffer.writeUtf(packet.mapProfile);
      buffer.writeEnum(packet.spawnMode);
      buffer.writeDouble(packet.spawnCenterLongitude);
      buffer.writeDouble(packet.spawnCenterLatitude);
      buffer.writeInt(packet.spawnCenterX);
      buffer.writeInt(packet.spawnCenterZ);
      buffer.writeInt(packet.spawnDistance);
      buffer.writeBoolean(packet.canyonsNotVolcanic);
      buffer.writeBoolean(packet.flatBedrock);
      buffer.writeBoolean(packet.finiteContinents);
      buffer.writeDouble(packet.continentalness);
      buffer.writeDouble(packet.grassDensity);
      buffer.writeDouble(packet.temperatureConstant);
      buffer.writeDouble(packet.rainfallConstant);
      buffer.writeInt(packet.temperatureScale);
      buffer.writeInt(packet.rainfallScale);
      buffer.writeInt(packet.horizontalTileSize);
      buffer.writeInt(packet.verticalTileSize);
      buffer.writeBoolean(packet.continentFromMap);
      buffer.writeBoolean(packet.altitudeFromMap);
      buffer.writeBoolean(packet.hotspotsFromMap);
      buffer.writeBoolean(packet.koppenFromMap);
    },
    buffer ->
      new ConfigSyncPacket(
        buffer.readUtf(),
        buffer.readEnum(SpawnMode.class),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean()
      )
  );

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(ConfigSyncPacket packet, IPayloadContext context) {
    context.enqueueWork(() -> {
      TFCRealWorldConfig.setServerConfig(
        packet.mapProfile(),
        packet.spawnMode(),
        packet.spawnCenterLongitude(),
        packet.spawnCenterLatitude(),
        packet.spawnCenterX(),
        packet.spawnCenterZ(),
        packet.spawnDistance(),
        packet.canyonsNotVolcanic(),
        packet.flatBedrock(),
        packet.finiteContinents(),
        packet.continentalness(),
        packet.grassDensity(),
        packet.temperatureConstant(),
        packet.rainfallConstant(),
        packet.temperatureScale(),
        packet.rainfallScale(),
        packet.horizontalTileSize(),
        packet.verticalTileSize(),
        packet.continentFromMap(),
        packet.altitudeFromMap(),
        packet.hotspotsFromMap(),
        packet.koppenFromMap()
      );
    });
  }
}
