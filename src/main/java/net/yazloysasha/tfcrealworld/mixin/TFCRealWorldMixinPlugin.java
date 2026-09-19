package net.yazloysasha.tfcrealworld.mixin;

import java.util.List;
import java.util.Set;
import net.yazloysasha.tfcrealworld.compat.AurorasCompat;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Drops optional-integration mixins when their mod is absent so
 * stub-backed packages never resolve missing classes.
 */
public final class TFCRealWorldMixinPlugin implements IMixinConfigPlugin {

  @Override
  public void onLoad(String mixinPackage) {}

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(
    String targetClassName,
    String mixinClassName
  ) {
    return mixinDisableReason(mixinClassName) == null;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(
    String targetClassName,
    ClassNode targetClass,
    String mixinClassName,
    IMixinInfo mixinInfo
  ) {}

  @Override
  public void postApply(
    String targetClassName,
    ClassNode targetClass,
    String mixinClassName,
    IMixinInfo mixinInfo
  ) {}

  @Nullable
  private static String mixinDisableReason(String mixinClassName) {
    return AurorasCompat.mixinDisableReason(mixinClassName);
  }
}
