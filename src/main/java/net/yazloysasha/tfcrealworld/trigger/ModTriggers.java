package net.yazloysasha.tfcrealworld.trigger;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

public class ModTriggers {

  public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
    DeferredRegister.create(Registries.TRIGGER_TYPE, TFCRealWorld.MOD_ID);

  public static final DeferredHolder<
    CriterionTrigger<?>,
    FixedHighGlobeTrotterLocation
  > FIXED_HIGH_GLOBE_TROTTER_LOCATION = TRIGGERS.register(
    "fixed_high_globe_trotter_location",
    FixedHighGlobeTrotterLocation::new
  );

  public static final DeferredHolder<
    CriterionTrigger<?>,
    FixedLowGlobeTrotterLocation
  > FIXED_LOW_GLOBE_TROTTER_LOCATION = TRIGGERS.register(
    "fixed_low_globe_trotter_location",
    FixedLowGlobeTrotterLocation::new
  );
}
