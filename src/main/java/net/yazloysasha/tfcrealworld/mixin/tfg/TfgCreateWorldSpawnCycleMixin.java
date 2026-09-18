package net.yazloysasha.tfcrealworld.mixin.tfg;

import net.minecraft.client.gui.components.CycleButton;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.config.TFGConfig;
import su.terrafirmagreg.core.utils.CustomSpawnHelper;
import su.terrafirmagreg.core.utils.CustomSpawnHelper.CustomSpawnCondition;

/**
 * Selects {@code tfc_real_world} on the create-world spawn cycle when TFG still has {@code default}.
 */
@Mixin(value = CustomSpawnHelper.CreateWorldSpawnCycle.class, remap = false)
public class TfgCreateWorldSpawnCycleMixin {

  @Inject(method = "register", at = @At("TAIL"))
  private static void tfcrealworld$selectRealWorldByDefault(
    CycleButton<CustomSpawnCondition> button,
    CallbackInfo ci
  ) {
    if (!tfcrealworld$shouldApplyRealWorldDefault()) {
      return;
    }

    CustomSpawnCondition ours = CustomSpawnHelper.CUSTOM_SPAWN_CONDITIONS.get(
      TFCRealWorld.MOD_ID
    );
    if (ours == null) {
      return;
    }

    TFGConfig.COMMON.NEW_WORLD_SPAWN.set(ours.id());
    button.setValue(ours);
    button.setTooltip(
      CustomSpawnHelper.CreateWorldSpawnCycle.createTooltip(ours)
    );
  }

  @Unique
  private static boolean tfcrealworld$shouldApplyRealWorldDefault() {
    String selected = TFGConfig.COMMON.NEW_WORLD_SPAWN.get();
    return (
      !CustomSpawnHelper.VIEWER_SPAWN_ID.equals(selected) &&
      CustomSpawnHelper.DEFAULT_SPAWN.id().equals(selected)
    );
  }
}
