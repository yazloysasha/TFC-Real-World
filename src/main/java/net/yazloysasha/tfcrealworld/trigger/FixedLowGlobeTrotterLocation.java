package net.yazloysasha.tfcrealworld.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

public class FixedLowGlobeTrotterLocation
  extends SimpleCriterionTrigger<FixedLowGlobeTrotterLocation.TriggerInstance> {

  @Override
  public Codec<TriggerInstance> codec() {
    return TriggerInstance.CODEC;
  }

  public void trigger(ServerPlayer player) {
    this.trigger(player, instance -> instance.matches(player));
  }

  public static record TriggerInstance(Optional<ContextAwarePredicate> player)
    implements SimpleCriterionTrigger.SimpleInstance {
    public static final Codec<TriggerInstance> CODEC =
      RecordCodecBuilder.create(instance ->
        instance
          .group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf(
              "player"
            ).forGetter(TriggerInstance::player)
          )
          .apply(instance, TriggerInstance::new)
      );

    public boolean matches(ServerPlayer player) {
      return (
        player.blockPosition().getZ() <=
        -TFCRealWorldConfig.VERTICAL_SCALE.get()
      );
    }
  }
}
