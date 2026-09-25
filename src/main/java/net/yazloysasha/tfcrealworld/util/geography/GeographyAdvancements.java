package net.yazloysasha.tfcrealworld.util.geography;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.item.ModItems;
import net.yazloysasha.tfcrealworld.trigger.ModTriggers;
import net.yazloysasha.tfcrealworld.trigger.VisitWaypointTrigger;

/**
 * Flat geography advancement tab: one root + one child per waypoint (no region tree).
 * Waypoint entries are injected after datapack load so they stay in sync with geography data.
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

  public static void injectInto(ServerAdvancementManager manager) {
    Map<ResourceLocation, AdvancementHolder> next = new HashMap<>(
      manager
        .getAllAdvancements()
        .stream()
        .collect(
          java.util.stream.Collectors.toMap(
            AdvancementHolder::id,
            h -> h,
            (a, b) -> a
          )
        )
    );

    // Ensure root exists (datapack JSON preferred; synthesize if missing).
    AdvancementHolder root = next.get(ROOT_ID);
    if (root == null) {
      root = buildRoot();
      next.put(ROOT_ID, root);
    }

    for (GeographyNode node : GeographyManager.allWaypoints()) {
      if (node.latitude() == null || node.longitude() == null) {
        continue;
      }
      ResourceLocation id = waypointId(node);
      next.put(id, buildWaypoint(node, id));
    }

    GeographyAdvancementAccessor accessor =
      (GeographyAdvancementAccessor) manager;
    accessor.tfcrealworld$setAdvancements(ImmutableMap.copyOf(next));

    AdvancementTree tree = new AdvancementTree();
    tree.addAll(next.values());
    for (var node : tree.roots()) {
      if (node.holder().value().display().isPresent()) {
        TreeNodePosition.run(node);
      }
    }
    accessor.tfcrealworld$setTree(tree);
    TFCRealWorld.LOGGER.info(
      "Injected {} geography waypoint advancements under {}",
      GeographyManager.allWaypoints().size(),
      ROOT_ID
    );
  }

  private static AdvancementHolder buildRoot() {
    DisplayInfo display = new DisplayInfo(
      new ItemStack(ModItems.GLOBE.get()),
      Component.translatable(
        "tfc_real_world.advancements.geography.root.title"
      ),
      Component.translatable(
        "tfc_real_world.advancements.geography.root.description"
      ),
      Optional.of(
        ResourceLocation.withDefaultNamespace(
          "textures/block/cartography_table_side3.png"
        )
      ),
      AdvancementType.TASK,
      false,
      false,
      false
    );
    Criterion<?> tick = PlayerTrigger.TriggerInstance.tick();
    Advancement adv = new Advancement(
      Optional.empty(),
      Optional.of(display),
      AdvancementRewards.EMPTY,
      Map.of("auto", tick),
      AdvancementRequirements.allOf(java.util.List.of("auto")),
      false
    );
    return new AdvancementHolder(ROOT_ID, adv);
  }

  private static AdvancementHolder buildWaypoint(
    GeographyNode node,
    ResourceLocation id
  ) {
    String titleKey = GeographyManager.langKey(node, "title");
    String descKey = GeographyManager.langKey(node, "description");
    DisplayInfo display = new DisplayInfo(
      new ItemStack(Items.COMPASS),
      Component.translatable(titleKey),
      Component.translatable(descKey),
      Optional.empty(),
      AdvancementType.TASK,
      true,
      true,
      false
    );
    VisitWaypointTrigger.TriggerInstance instance =
      new VisitWaypointTrigger.TriggerInstance(
        Optional.empty(),
        node.ref().toLowerCase(Locale.ROOT)
      );
    Criterion<?> criterion = ModTriggers.VISIT_WAYPOINT.get()
      .createCriterion(instance);
    Advancement adv = new Advancement(
      Optional.of(ROOT_ID),
      Optional.of(display),
      AdvancementRewards.EMPTY,
      Map.of("visited", criterion),
      AdvancementRequirements.allOf(java.util.List.of("visited")),
      false
    );
    return new AdvancementHolder(id, adv);
  }

  /**
   * Fire the visit trigger for a waypoint (root is already granted via tick / login sync).
   */
  public static void onWaypointVisited(
    ServerPlayer player,
    String waypointRef
  ) {
    if (waypointRef != null && !waypointRef.isEmpty()) {
      ModTriggers.VISIT_WAYPOINT.get().trigger(player, waypointRef);
    }
  }

  /**
   * Duck-typed setters implemented by mixin.
   */
  public interface GeographyAdvancementAccessor {
    void tfcrealworld$setAdvancements(
      Map<ResourceLocation, AdvancementHolder> advancements
    );

    void tfcrealworld$setTree(AdvancementTree tree);
  }
}
