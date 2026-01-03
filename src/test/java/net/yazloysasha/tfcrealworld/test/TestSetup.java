package net.yazloysasha.tfcrealworld.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.world.level.levelgen.RandomSupport;

/**
 * Bootstraps a number of useful pieces of data for unit tests. This is done using a hybrid of TFC and vanilla data generation,
 * plus a number of terrible hacks to get this to work... but... it does work. And it allows fast testing of complex mechanics
 * (molds, item/fluid heat components, heating, etc.)
 */
public interface TestSetup {
  AtomicBoolean LOADED = new AtomicBoolean(false);
  Object LOCK = new Object();

  Field TAG_BUILDERS = Helpers.uncheck(() -> {
    final var field = TagsProvider.class.getDeclaredField("builders");
    field.setAccessible(true);
    return field;
  });
  Method TAG_CONTENTS_PROVIDER = Helpers.uncheck(() -> {
    final var method =
      TagsProvider.class.getDeclaredMethod("createContentsProvider");
    method.setAccessible(true);
    return method;
  });

  record TagResolver<T>(
    Map<ResourceLocation, TagBuilder> builder,
    Registry<T> registry
  ) {
    Stream<Holder<T>> resolve(ResourceLocation id) {
      return Objects.requireNonNull(
        builder.get(id),
        () -> "No tag for " + id + " in registry " + registry.key().location()
      )
        .build()
        .stream()
        .flatMap(e ->
          e.isTag()
            ? e.isRequired() || builder.containsKey(e.getId())
              ? resolve(e.getId())
              : Stream.empty()
            : Stream.of(
              registry.wrapAsHolder(
                registry.getOrThrow(
                  ResourceKey.create(registry.key(), e.getId())
                )
              )
            )
        );
    }
  }

  class TagMap extends LinkedHashMap<ResourceLocation, TagBuilder> {

    @Override
    public void clear() {} // No-op
  }

  default long seed() {
    final long seed = RandomSupport.generateUniqueSeed();
    System.out.printf("Seed: %d\n", seed);
    return seed;
  }
}
