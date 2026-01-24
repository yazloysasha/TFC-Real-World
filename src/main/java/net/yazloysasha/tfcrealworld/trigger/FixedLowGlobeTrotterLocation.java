package net.yazloysasha.tfcrealworld.trigger;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

public class FixedLowGlobeTrotterLocation
  extends SimpleCriterionTrigger<FixedLowGlobeTrotterLocation.TriggerInstance> {

  private static final ResourceLocation ID = new ResourceLocation(
    TFCRealWorld.MOD_ID,
    "fixed_low_globe_trotter_location"
  );

  @Override
  public ResourceLocation getId() {
    return ID;
  }

  @Override
  protected TriggerInstance createInstance(
    JsonObject json,
    EntityPredicate.Composite predicate,
    DeserializationContext context
  ) {
    return new TriggerInstance(predicate);
  }

  public void trigger(ServerPlayer player) {
    this.trigger(player, instance -> instance.matches(player));
  }

  public static class TriggerInstance extends AbstractCriterionTriggerInstance {

    public TriggerInstance(EntityPredicate.Composite predicate) {
      super(ID, predicate);
    }

    public boolean matches(ServerPlayer player) {
      return (
        player.blockPosition().getZ() <=
        -TFCRealWorldConfig.VERTICAL_SCALE.get() / 2
      );
    }
  }
}
