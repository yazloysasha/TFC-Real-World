package net.yazloysasha.tfcrealworld;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.yazloysasha.tfcrealworld.config.ConfigManager;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.network.ConfigSyncPacket;
import net.yazloysasha.tfcrealworld.util.pack.DynamicPackFinder;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenParameterCache;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import org.slf4j.Logger;

@Mod(TFCRealWorld.MOD_ID)
public final class TFCRealWorld {

  public static final String MOD_ID = "tfc_real_world";
  public static final String MOD_NAME = "TFC: Real World";
  public static final Logger LOGGER = LogUtils.getLogger();

  public TFCRealWorld(ModContainer container, IEventBus modEventBus) {
    ProfileManager.initialize();

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

    modEventBus.addListener(
      AddPackFindersEvent.class,
      DynamicPackFinder::registerPack
    );

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

  private void registerNetwork(RegisterPayloadHandlersEvent event) {
    final PayloadRegistrar registrar = event.registrar(TFCRealWorld.MOD_ID);
    registrar.playToClient(
      ConfigSyncPacket.TYPE,
      ConfigSyncPacket.STREAM_CODEC,
      ConfigSyncPacket::handle
    );
  }

  private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      ConfigManager.sendConfigToClient(serverPlayer);
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
    BasePNGNoise.clearImageCache();
  }
}
