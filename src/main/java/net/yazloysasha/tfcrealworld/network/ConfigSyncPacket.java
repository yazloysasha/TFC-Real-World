package net.yazloysasha.tfcrealworld.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

public record ConfigSyncPacket(
  double continentalness,
  boolean finiteContinents,
  boolean flatBedrock,
  double grassDensity,
  int spawnCenterX,
  int spawnCenterZ,
  int spawnDistance,
  int temperatureScale,
  int rainfallScale,
  int verticalWorldScale,
  int horizontalWorldScale,
  boolean continentFromMap,
  boolean altitudeFromMap,
  boolean hotspotsFromMap,
  boolean koppenFromMap,
  int poleOffset,
  boolean poleLooping,
  boolean canyonsNotVolcanic
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
      buffer.writeDouble(packet.continentalness);
      buffer.writeBoolean(packet.finiteContinents);
      buffer.writeBoolean(packet.flatBedrock);
      buffer.writeDouble(packet.grassDensity);
      buffer.writeInt(packet.spawnCenterX);
      buffer.writeInt(packet.spawnCenterZ);
      buffer.writeInt(packet.spawnDistance);
      buffer.writeInt(packet.temperatureScale);
      buffer.writeInt(packet.rainfallScale);
      buffer.writeInt(packet.verticalWorldScale);
      buffer.writeInt(packet.horizontalWorldScale);
      buffer.writeBoolean(packet.continentFromMap);
      buffer.writeBoolean(packet.altitudeFromMap);
      buffer.writeBoolean(packet.hotspotsFromMap);
      buffer.writeBoolean(packet.koppenFromMap);
      buffer.writeInt(packet.poleOffset);
      buffer.writeBoolean(packet.poleLooping);
      buffer.writeBoolean(packet.canyonsNotVolcanic);
    },
    buffer ->
      new ConfigSyncPacket(
        buffer.readDouble(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readDouble(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readBoolean(),
        buffer.readInt(),
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
        packet.continentalness(),
        packet.finiteContinents(),
        packet.flatBedrock(),
        packet.grassDensity(),
        packet.spawnCenterX(),
        packet.spawnCenterZ(),
        packet.spawnDistance(),
        packet.temperatureScale(),
        packet.rainfallScale(),
        packet.verticalWorldScale(),
        packet.horizontalWorldScale(),
        packet.continentFromMap(),
        packet.altitudeFromMap(),
        packet.hotspotsFromMap(),
        packet.koppenFromMap(),
        packet.poleOffset(),
        packet.poleLooping(),
        packet.canyonsNotVolcanic()
      );
    });
  }
}
