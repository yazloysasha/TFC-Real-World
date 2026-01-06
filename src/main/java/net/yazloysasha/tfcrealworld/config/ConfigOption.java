package net.yazloysasha.tfcrealworld.config;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigOption<T> implements Supplier<T> {

  private final ModConfigSpec.ConfigValue<T> configValue;
  private T serverValue;
  private boolean serverConfigActive = false;

  @SuppressWarnings("rawtypes")
  private Class<? extends Enum> enumClass;

  @SuppressWarnings("rawtypes")
  private ModConfigSpec.ConfigValue enumConfigValue;

  public ConfigOption(
    ModConfigSpec.Builder builder,
    String name,
    String comment,
    T defaultValue
  ) {
    builder.comment("");
    builder.comment(comment);
    this.configValue = builder.define(name, defaultValue);
  }

  @SuppressWarnings("unchecked")
  public ConfigOption(
    ModConfigSpec.Builder builder,
    String name,
    String comment,
    T defaultValue,
    T min,
    T max
  ) {
    builder.comment("");
    builder.comment(comment);
    if (defaultValue instanceof Double) {
      this.configValue = (ModConfigSpec.ConfigValue<T>) builder.defineInRange(
        name,
        (Double) defaultValue,
        (Double) min,
        (Double) max
      );
    } else if (defaultValue instanceof Integer) {
      this.configValue = (ModConfigSpec.ConfigValue<T>) builder.defineInRange(
        name,
        (Integer) defaultValue,
        (Integer) min,
        (Integer) max
      );
    } else {
      throw new IllegalArgumentException(
        "Unsupported type for range: " + defaultValue.getClass()
      );
    }
  }

  @SuppressWarnings("rawtypes")
  public <E extends Enum<E>> ConfigOption(
    ModConfigSpec.Builder builder,
    String name,
    String comment,
    E defaultValue,
    Class<E> enumClass
  ) {
    builder.comment("");
    builder.comment(comment);
    this.enumClass = (Class<? extends Enum>) enumClass;
    this.enumConfigValue = builder.defineEnum(name, defaultValue);
    this.configValue = null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get() {
    if (serverConfigActive && serverValue != null) {
      return serverValue;
    }

    if (enumClass != null && enumConfigValue != null) {
      return (T) enumConfigValue.get();
    }

    return configValue.get();
  }

  public void setServerValue(T value) {
    this.serverValue = value;
    this.serverConfigActive = true;
  }

  public void clearServerValue() {
    this.serverValue = null;
    this.serverConfigActive = false;
  }
}
