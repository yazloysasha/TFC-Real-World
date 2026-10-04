package net.yazloysasha.tfcrealworld.util.geography;

import java.util.Locale;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.item.ModItems;
import net.yazloysasha.tfcrealworld.trigger.ModTriggers;
import net.yazloysasha.tfcrealworld.trigger.VisitWaypointTrigger;

/**
 * Flat geography advancement tab: one root + one child per waypoint (no region tree).
 * Waypoint entries are added to the datapack's advancements as they load, so
 * they stay in sync with geography data.
 */
public final class GeographyAdvancements {

  public static final ResourceLocation ROOT_ID = TFCRealWorld.id(
    "geography/root"
  );

  private GeographyAdvancements() {}

  public static ResourceLocation waypointId(GeographyNode node) {
    return TFCRealWorld.id(
      "geography/waypoint/" + node.namespace() + "/" + node.slug()
    );
  }

  /** Adds the geography advancements to those read from the datapacks. */
  public static void addTo(
    Map<ResourceLocation, Advancement.Builder> advancements
  ) {
    // Ensure root exists (datapack JSON preferred; synthesize if missing).
    advancements.computeIfAbsent(ROOT_ID, id -> buildRoot());
    for (GeographyNode node : GeographyManager.allWaypoints()) {
      if (node.latitude() == null || node.longitude() == null) {
        continue;
      }
      advancements.put(waypointId(node), buildWaypoint(node));
    }
    TFCRealWorld.LOGGER.info(
      "Injected {} geography waypoint advancements under {}",
      GeographyManager.allWaypoints().size(),
      ROOT_ID
    );
  }

  private static Advancement.Builder buildRoot() {
    return Advancement.Builder.advancement()
      .display(
        new ItemStack(ModItems.GLOBE.get()),
        Component.translatable(
          "tfc_real_world.advancements.geography.root.title"
        ),
        Component.translatable(
          "tfc_real_world.advancements.geography.root.description"
        ),
        new ResourceLocation("textures/block/cartography_table_side3.png"),
        FrameType.TASK,
        false,
        false,
        false
      )
      .addCriterion("auto", PlayerTrigger.TriggerInstance.tick());
  }

  private static Advancement.Builder buildWaypoint(GeographyNode node) {
    return Advancement.Builder.advancement()
      .parent(ROOT_ID)
      .display(
        new ItemStack(Items.COMPASS),
        Component.translatable(GeographyManager.langKey(node, "title")),
        Component.translatable(GeographyManager.langKey(node, "description")),
        null,
        FrameType.TASK,
        true,
        true,
        false
      )
      .addCriterion(
        "visited",
        new VisitWaypointTrigger.TriggerInstance(
          ContextAwarePredicate.ANY,
          node.ref().toLowerCase(Locale.ROOT)
        )
      );
  }

  /**
   * Fire the visit trigger for a waypoint (root is already granted via tick / login sync).
   */
  public static void onWaypointVisited(
    ServerPlayer player,
    String waypointRef
  ) {
    if (waypointRef != null && !waypointRef.isEmpty()) {
      ModTriggers.VISIT_WAYPOINT.trigger(player, waypointRef);
    }
  }
}
