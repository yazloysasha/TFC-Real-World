package net.yazloysasha.tfcrealworld.trigger;

import com.google.gson.JsonObject;
import java.util.Locale;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

public class VisitWaypointTrigger
  extends SimpleCriterionTrigger<VisitWaypointTrigger.TriggerInstance>
{

  private static final ResourceLocation ID = TFCRealWorld.id("visit_waypoint");

  @Override
  public ResourceLocation getId() {
    return ID;
  }

  @Override
  protected TriggerInstance createInstance(
    JsonObject json,
    ContextAwarePredicate predicate,
    DeserializationContext context
  ) {
    return new TriggerInstance(
      predicate,
      GsonHelper.getAsString(json, "waypoint")
    );
  }

  public void trigger(ServerPlayer player, String waypointRef) {
    String normalized = waypointRef.toLowerCase(Locale.ROOT);
    this.trigger(player, instance -> instance.matches(normalized));
  }

  public static class TriggerInstance extends AbstractCriterionTriggerInstance {

    private final String waypoint;

    public TriggerInstance(ContextAwarePredicate player, String waypoint) {
      super(ID, player);
      this.waypoint = waypoint;
    }

    public boolean matches(String waypointRef) {
      return waypoint.toLowerCase(Locale.ROOT).equals(waypointRef);
    }

    @Override
    public JsonObject serializeToJson(SerializationContext context) {
      final JsonObject json = super.serializeToJson(context);
      json.addProperty("waypoint", waypoint);
      return json;
    }
  }
}
