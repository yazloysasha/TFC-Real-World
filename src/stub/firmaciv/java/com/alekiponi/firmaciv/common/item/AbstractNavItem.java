package com.alekiponi.firmaciv.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractNavItem extends Item {

  public AbstractNavItem(Properties properties) {
    super(properties);
  }

  public static double[] getNavLocation(Vec3 position) {
    throw new AssertionError("stub");
  }
}
