package net.yazloysasha.tfcrealworld.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class VisitWaypointTrigger
  extends SimpleCriterionTrigger<VisitWaypointTrigger.TriggerInstance> {

  @Override
  public Codec<TriggerInstance> codec() {
    return TriggerInstance.CODEC;
  }

  public void trigger(ServerPlayer player, String waypointRef) {
    String normalized = waypointRef.toLowerCase(Locale.ROOT);
    this.trigger(player, instance -> instance.matches(normalized));
  }

  public record TriggerInstance(
    Optional<ContextAwarePredicate> player,
    String waypoint
  )
    implements SimpleCriterionTrigger.SimpleInstance {
    public static final Codec<TriggerInstance> CODEC =
      RecordCodecBuilder.create(instance ->
        instance
          .group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf(
              "player"
            ).forGetter(TriggerInstance::player),
            Codec.STRING.fieldOf("waypoint").forGetter(
              TriggerInstance::waypoint
            )
          )
          .apply(instance, TriggerInstance::new)
      );

    public boolean matches(String waypointRef) {
      return waypoint.toLowerCase(Locale.ROOT).equals(waypointRef);
    }
  }
}
