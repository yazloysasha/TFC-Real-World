package net.yazloysasha.tfcrealworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import net.yazloysasha.tfcrealworld.types.SpawnMode;

public record ConfigSyncPacket(
  SpawnMode spawnMode,
  Double spawnCenterLongtitude,
  Double spawnCenterLatitude,
  int spawnCenterX,
  int spawnCenterZ,
  int spawnDistance,
  boolean flatBedrock,
  boolean finiteContinents,
  double continentalness,
  double grassDensity,
  int temperatureScale,
  int rainfallScale,
  int horizontalTileSize,
  int verticalTileSize,
  boolean continentFromMap,
  boolean altitudeFromMap,
  boolean hotspotsFromMap,
  boolean koppenFromMap,
  boolean canyonsNotVolcanic,
  Double westEdgeLongtitude,
  Double eastEdgeLongtitude,
  Double southEdgeLatitude,
  Double northEdgeLatitude,
  MapProjection mapProjection
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
      buffer.writeEnum(packet.spawnMode);
      buffer.writeDouble(packet.spawnCenterLongtitude);
      buffer.writeDouble(packet.spawnCenterLatitude);
      buffer.writeInt(packet.spawnCenterX);
      buffer.writeInt(packet.spawnCenterZ);
      buffer.writeInt(packet.spawnDistance);
      buffer.writeBoolean(packet.flatBedrock);
      buffer.writeBoolean(packet.finiteContinents);
      buffer.writeDouble(packet.continentalness);
      buffer.writeDouble(packet.grassDensity);
      buffer.writeInt(packet.temperatureScale);
      buffer.writeInt(packet.rainfallScale);
      buffer.writeInt(packet.horizontalTileSize);
      buffer.writeInt(packet.verticalTileSize);
      buffer.writeBoolean(packet.continentFromMap);
      buffer.writeBoolean(packet.altitudeFromMap);
      buffer.writeBoolean(packet.hotspotsFromMap);
      buffer.writeBoolean(packet.koppenFromMap);
      buffer.writeBoolean(packet.canyonsNotVolcanic);
      buffer.writeDouble(packet.westEdgeLongtitude);
      buffer.writeDouble(packet.eastEdgeLongtitude);
      buffer.writeDouble(packet.southEdgeLatitude);
      buffer.writeDouble(packet.northEdgeLatitude);
      buffer.writeEnum(packet.mapProjection);
    },
    buffer ->
      new ConfigSyncPacket(
        buffer.readEnum(SpawnMode.class),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readDouble(),
        buffer.readEnum(MapProjection.class)
      )
  );

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(ConfigSyncPacket packet, IPayloadContext context) {
    context.enqueueWork(() -> {
      TFCRealWorldConfig.setServerConfig(
        packet.spawnMode(),
        packet.spawnCenterLongtitude(),
        packet.spawnCenterLatitude(),
        packet.spawnCenterX(),
        packet.spawnCenterZ(),
        packet.spawnDistance(),
        packet.flatBedrock(),
        packet.finiteContinents(),
        packet.continentalness(),
        packet.grassDensity(),
        packet.temperatureScale(),
        packet.rainfallScale(),
        packet.horizontalTileSize(),
        packet.verticalTileSize(),
        packet.continentFromMap(),
        packet.altitudeFromMap(),
        packet.hotspotsFromMap(),
        packet.koppenFromMap(),
        packet.canyonsNotVolcanic(),
        packet.westEdgeLongtitude(),
        packet.eastEdgeLongtitude(),
        packet.southEdgeLatitude(),
        packet.northEdgeLatitude(),
        packet.mapProjection()
      );
    });
  }
}
