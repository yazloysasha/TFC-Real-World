package net.yazloysasha.tfcrealworld;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.yazloysasha.tfcrealworld.attachment.ModAttachments;
import net.yazloysasha.tfcrealworld.attachment.VisitedWaypoints;
import net.yazloysasha.tfcrealworld.config.ConfigManager;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.item.ModItems;
import net.yazloysasha.tfcrealworld.network.ConfigSyncPacket;
import net.yazloysasha.tfcrealworld.network.OpenGeographyScreenPacket;
import net.yazloysasha.tfcrealworld.network.OpenGeographyTabPacket;
import net.yazloysasha.tfcrealworld.network.VisitedWaypointUpdatePacket;
import net.yazloysasha.tfcrealworld.network.VisitedWaypointsSyncPacket;
import net.yazloysasha.tfcrealworld.trigger.ModTriggers;
import net.yazloysasha.tfcrealworld.util.geography.GeographyAdvancements;
import net.yazloysasha.tfcrealworld.util.geography.GeographyManager;
import net.yazloysasha.tfcrealworld.util.geography.WaypointVisitTracker;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenParameterCache;
import net.yazloysasha.tfcrealworld.world.noise.koppen.SmoothedKoppenParameterMaps;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import org.slf4j.Logger;

@Mod(TFCRealWorld.MOD_ID)
public final class TFCRealWorld {

  public static final String MOD_ID = "tfc_real_world";
  public static final String MOD_NAME = "TFC: Real World";
  public static final Logger LOGGER = LogUtils.getLogger();

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
  }

  public TFCRealWorld(ModContainer container, IEventBus modEventBus) {
    ProfileManager.initialize();
    GeographyManager.initialize();

    container.registerConfig(
      ModConfig.Type.COMMON,
      TFCRealWorldConfig.SPEC,
      MOD_ID + "/common.toml"
    );

    modEventBus.addListener(ModConfigEvent.Loading.class, event -> {
      if (event.getConfig().getModId().equals(MOD_ID)) {
        TFCRealWorldConfig.setModConfig(event.getConfig());
      }
    });

    NeoForge.EVENT_BUS.register(ConfigManager.class);

    ModTriggers.TRIGGERS.register(modEventBus);
    ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
    ModItems.ITEMS.register(modEventBus);

    NeoForge.EVENT_BUS.addListener(this::onPlayerTick);

    modEventBus.addListener(
      RegisterPayloadHandlersEvent.class,
      this::registerNetwork
    );

    NeoForge.EVENT_BUS.addListener(
      PlayerEvent.PlayerLoggedInEvent.class,
      this::onPlayerLoggedIn
    );

    NeoForge.EVENT_BUS.addListener(
      PlayerEvent.PlayerLoggedOutEvent.class,
      this::onPlayerLoggedOut
    );

    NeoForge.EVENT_BUS.addListener(
      ClientPlayerNetworkEvent.LoggingIn.class,
      this::onClientLoggingIn
    );

    NeoForge.EVENT_BUS.addListener(
      ClientPlayerNetworkEvent.LoggingOut.class,
      this::onClientLoggingOut
    );

    NeoForge.EVENT_BUS.addListener(
      LevelEvent.Unload.class,
      this::onLevelUnload
    );
  }

  private void onPlayerTick(PlayerTickEvent.Post event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      if (serverPlayer.tickCount % 20 == 0) {
        ModTriggers.FIXED_HIGH_GLOBE_TROTTER_LOCATION.get()
          .trigger(serverPlayer);
        ModTriggers.FIXED_LOW_GLOBE_TROTTER_LOCATION.get()
          .trigger(serverPlayer);
        WaypointVisitTracker.tickPlayer(serverPlayer);
      }
    }
  }

  private void registerNetwork(RegisterPayloadHandlersEvent event) {
    final PayloadRegistrar registrar = event.registrar(TFCRealWorld.MOD_ID);
    registrar.playToClient(
      ConfigSyncPacket.TYPE,
      ConfigSyncPacket.STREAM_CODEC,
      ConfigSyncPacket::handle
    );
    registrar.playToServer(
      OpenGeographyTabPacket.TYPE,
      OpenGeographyTabPacket.STREAM_CODEC,
      OpenGeographyTabPacket::handle
    );
    if (FMLEnvironment.dist == Dist.CLIENT) {
      registrar.playToClient(
        OpenGeographyScreenPacket.TYPE,
        OpenGeographyScreenPacket.STREAM_CODEC,
        net.yazloysasha.tfcrealworld.client.GeographyClientEvents::handleOpenGeographyScreen
      );
    } else {
      registrar.playToClient(
        OpenGeographyScreenPacket.TYPE,
        OpenGeographyScreenPacket.STREAM_CODEC,
        OpenGeographyScreenPacket::handle
      );
    }
    registrar.playToClient(
      VisitedWaypointsSyncPacket.TYPE,
      VisitedWaypointsSyncPacket.STREAM_CODEC,
      VisitedWaypointsSyncPacket::handle
    );
    registrar.playToClient(
      VisitedWaypointUpdatePacket.TYPE,
      VisitedWaypointUpdatePacket.STREAM_CODEC,
      VisitedWaypointUpdatePacket::handle
    );
  }

  private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      ConfigManager.sendConfigToClient(serverPlayer);
      VisitedWaypoints data = serverPlayer.getData(
        ModAttachments.VISITED_WAYPOINTS
      );
      PacketDistributor.sendToPlayer(
        serverPlayer,
        new VisitedWaypointsSyncPacket(data.asMap())
      );
      GeographyAdvancements.syncFromVisited(serverPlayer, data.asMap());
      WaypointVisitTracker.rebuildCache();
    }
  }

  private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!event.getEntity().level().isClientSide) {
      return;
    }

    TFCRealWorldConfig.clearServerConfig();
  }

  private void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel().isClientSide()) {
      TFCRealWorldConfig.clearServerConfig();
    }
  }

  private void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
    try {
      Minecraft mc = Minecraft.getInstance();
      boolean isSingleplayer =
        mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null;

      if (!isSingleplayer) {
        clearCaches();
      }
    } catch (Exception e) {
      clearCaches();
    }
  }

  private void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    TFCRealWorldConfig.clearServerConfig();
  }

  private void clearCaches() {
    GlobalOceanDistanceCache.clear();
    GlobalWestCoastDistanceCache.clear();
    KoppenParameterCache.clear();
    SmoothedKoppenParameterMaps.clear();
    BasePNGNoise.clearImageCache();
  }
}
