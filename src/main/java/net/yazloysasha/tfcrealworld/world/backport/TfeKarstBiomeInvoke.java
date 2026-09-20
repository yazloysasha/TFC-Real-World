package net.yazloysasha.tfcrealworld.world.backport;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

/**
 * Calls TFE {@code getBurrenBiome} / {@code getTowerKarstBiome} /
 * {@code getHotSpotBiome} merged onto {@link ChooseBiomes} after TFE applies
 * (no {@code @Invoker} on TFE mixin types).
 */
public final class TfeKarstBiomeInvoke {

  private static volatile MethodHandle burrenHandle;
  private static volatile MethodHandle towerHandle;
  private static volatile MethodHandle hotSpotHandle;
  private static volatile boolean burrenLookupFailed;
  private static volatile boolean towerLookupFailed;
  private static volatile boolean hotSpotLookupFailed;

  private TfeKarstBiomeInvoke() {}

  public static int burrenBiome(Object chooseBiomesReceiver, int biome) {
    return invoke(burrenHandle(), chooseBiomesReceiver, biome);
  }

  public static int towerKarstBiome(Object chooseBiomesReceiver, int biome) {
    return invoke(towerHandle(), chooseBiomesReceiver, biome);
  }

  public static int hotSpotBiome(Object chooseBiomesReceiver, int age) {
    return invoke(hotSpotHandle(), chooseBiomesReceiver, age);
  }

  private static MethodHandle burrenHandle() {
    return resolveHandle("getBurrenBiome", Slot.BURREN);
  }

  private static MethodHandle towerHandle() {
    return resolveHandle("getTowerKarstBiome", Slot.TOWER);
  }

  private static MethodHandle hotSpotHandle() {
    return resolveHandle("getHotSpotBiome", Slot.HOTSPOT);
  }

  private enum Slot {
    BURREN,
    TOWER,
    HOTSPOT,
  }

  private static MethodHandle resolveHandle(String methodName, Slot slot) {
    MethodHandle handle = handleOf(slot);
    if (handle != null) {
      return handle;
    }
    if (failedOf(slot)) {
      return null;
    }
    synchronized (TfeKarstBiomeInvoke.class) {
      handle = handleOf(slot);
      if (handle != null) {
        return handle;
      }
      try {
        final Method method =
          ChooseBiomes.class.getDeclaredMethod(methodName, int.class);
        method.setAccessible(true);
        handle = MethodHandles.lookup().unreflect(method);
        storeHandle(slot, handle);
        return handle;
      } catch (ReflectiveOperationException e) {
        storeFailed(slot);
        TFCRealWorld.LOGGER.warn(
          "ChooseBiomes.{} missing (TFE not merged?); using biome unchanged",
          methodName
        );
        return null;
      }
    }
  }

  private static MethodHandle handleOf(Slot slot) {
    return switch (slot) {
      case BURREN -> burrenHandle;
      case TOWER -> towerHandle;
      case HOTSPOT -> hotSpotHandle;
    };
  }

  private static boolean failedOf(Slot slot) {
    return switch (slot) {
      case BURREN -> burrenLookupFailed;
      case TOWER -> towerLookupFailed;
      case HOTSPOT -> hotSpotLookupFailed;
    };
  }

  private static void storeHandle(Slot slot, MethodHandle handle) {
    switch (slot) {
      case BURREN -> burrenHandle = handle;
      case TOWER -> towerHandle = handle;
      case HOTSPOT -> hotSpotHandle = handle;
    }
  }

  private static void storeFailed(Slot slot) {
    switch (slot) {
      case BURREN -> burrenLookupFailed = true;
      case TOWER -> towerLookupFailed = true;
      case HOTSPOT -> hotSpotLookupFailed = true;
    }
  }

  private static int invoke(MethodHandle handle, Object receiver, int biome) {
    if (handle == null) {
      return biome;
    }
    try {
      return (int) handle.invoke(receiver, biome);
    } catch (Throwable t) {
      throw new IllegalStateException("TFE karst helper invoke failed", t);
    }
  }
}
