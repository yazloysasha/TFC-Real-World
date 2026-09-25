package net.yazloysasha.tfcrealworld.attachment;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

public final class ModAttachments {

  public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
    DeferredRegister.create(
      NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
      TFCRealWorld.MOD_ID
    );

  public static final Supplier<
    AttachmentType<VisitedWaypoints>
  > VISITED_WAYPOINTS = ATTACHMENT_TYPES.register("visited_waypoints", () ->
    AttachmentType.serializable(VisitedWaypoints::new).copyOnDeath().build()
  );

  private ModAttachments() {}
}
