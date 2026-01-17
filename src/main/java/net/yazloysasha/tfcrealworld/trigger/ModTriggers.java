package net.yazloysasha.tfcrealworld.trigger;

import net.minecraft.advancements.CriteriaTriggers;

public class ModTriggers {

  public static final FixedHighGlobeTrotterLocation FIXED_HIGH_GLOBE_TROTTER_LOCATION =
    CriteriaTriggers.register(new FixedHighGlobeTrotterLocation());

  public static final FixedLowGlobeTrotterLocation FIXED_LOW_GLOBE_TROTTER_LOCATION =
    CriteriaTriggers.register(new FixedLowGlobeTrotterLocation());

  public static void init() {}
}
