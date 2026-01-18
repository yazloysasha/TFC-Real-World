package net.yazloysasha.tfcrealworld;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.yazloysasha.tfcrealworld.config.ConfigManager;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.network.PacketHandler;
import net.yazloysasha.tfcrealworld.trigger.ModTriggers;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import org.slf4j.Logger;

@Mod(TFCRealWorld.MOD_ID)
public final class TFCRealWorld {

  public static final String MOD_ID = "tfc_real_world";
  public static final String MOD_NAME = "TFC: Real World";
  public static final Logger LOGGER = LogUtils.getLogger();

  public TFCRealWorld() {
    ProfileManager.initialize();

    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    ModLoadingContext.get()
      .registerConfig(
        ModConfig.Type.COMMON,
        TFCRealWorldConfig.SPEC,
        MOD_ID + "/common.toml"
      );

    modEventBus.addListener(this::onModConfigLoading);

    MinecraftForge.EVENT_BUS.register(ConfigManager.class);

    ModTriggers.init();

    MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);

    PacketHandler.register();

    MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);

    MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);

    MinecraftForge.EVENT_BUS.addListener(this::onClientLoggingIn);

    MinecraftForge.EVENT_BUS.addListener(this::onClientLoggingOut);

    MinecraftForge.EVENT_BUS.addListener(this::onLevelUnload);
  }

  private void onModConfigLoading(ModConfigEvent.Loading event) {
    if (event.getConfig().getModId().equals(MOD_ID)) {
      TFCRealWorldConfig.setModConfig(event.getConfig());
    }
  }

  private void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (
      event.phase == TickEvent.Phase.END &&
      event.player instanceof ServerPlayer serverPlayer
    ) {
      if (serverPlayer.tickCount % 20 == 0) {
        ModTriggers.FIXED_HIGH_GLOBE_TROTTER_LOCATION.trigger(serverPlayer);
        ModTriggers.FIXED_LOW_GLOBE_TROTTER_LOCATION.trigger(serverPlayer);
      }
    }
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

  private void onClientLoggingIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (!event.getEntity().level().isClientSide) {
      return;
    }

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

  private void onClientLoggingOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!event.getEntity().level().isClientSide) {
      return;
    }

    TFCRealWorldConfig.clearServerConfig();
  }

  private void clearCaches() {
    GlobalOceanDistanceCache.clear();
    BasePNGNoise.clearImageCache();
  }
}
