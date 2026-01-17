package net.yazloysasha.tfcrealworld.config;

import java.util.function.Supplier;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigOption<T> implements Supplier<T> {

  private final ForgeConfigSpec.ConfigValue<T> configValue;
  private T serverValue;
  private boolean serverConfigActive = false;

  @SuppressWarnings("rawtypes")
  private Class<? extends Enum> enumClass;

  @SuppressWarnings("rawtypes")
  private ForgeConfigSpec.ConfigValue enumConfigValue;

  private T minValue;
  private T maxValue;

  public ConfigOption(
    ForgeConfigSpec.Builder builder,
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
    ForgeConfigSpec.Builder builder,
    String name,
    String comment,
    T defaultValue,
    T min,
    T max
  ) {
    this.minValue = min;
    this.maxValue = max;

    builder.comment("");
    builder.comment(comment);

    if (defaultValue instanceof Double) {
      this.configValue = (ForgeConfigSpec.ConfigValue<T>) builder.defineInRange(
        name,
        (Double) defaultValue,
        (Double) min,
        (Double) max
      );
    } else if (defaultValue instanceof Integer) {
      this.configValue = (ForgeConfigSpec.ConfigValue<T>) builder.defineInRange(
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
    ForgeConfigSpec.Builder builder,
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

  @SuppressWarnings("unchecked")
  public void set(T value) {
    if (enumClass != null && enumConfigValue != null) {
      ((ForgeConfigSpec.ConfigValue<T>) enumConfigValue).set(value);
    } else if (configValue != null) {
      configValue.set(value);
    } else {
      throw new IllegalStateException(
        "Cannot set value: ConfigOption is not properly initialized"
      );
    }
  }

  public T getMin() {
    return minValue;
  }

  public T getMax() {
    return maxValue;
  }
}
