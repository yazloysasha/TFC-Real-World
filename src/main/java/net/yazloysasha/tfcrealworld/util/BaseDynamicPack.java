package net.yazloysasha.tfcrealworld.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public abstract class BaseDynamicPack implements PackResources {

  protected final PackLocationInfo locationInfo;
  protected static final String TFC_NAMESPACE = "tfc";

  protected BaseDynamicPack(PackLocationInfo locationInfo) {
    this.locationInfo = locationInfo;
  }

  protected int[] getWorldScaleValues() {
    int verticalWorldScale =
      net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig.getVerticalWorldScale();
    int halfScale = verticalWorldScale / 2;
    int negativeHalfScale = -halfScale;
    return new int[] { halfScale, negativeHalfScale };
  }

  @Override
  public @Nullable IoSupplier<InputStream> getRootResource(String... path) {
    return null;
  }

  @Override
  public void close() {}

  @Override
  public PackLocationInfo location() {
    return locationInfo;
  }

  @Override
  public <T> @Nullable T getMetadataSection(
    MetadataSectionSerializer<T> serializer
  ) {
    if (serializer == PackMetadataSection.TYPE) {
      @SuppressWarnings("unchecked")
      T result = (T) new PackMetadataSection(
        Component.literal(getPackDisplayName()),
        getPackFormat()
      );
      return result;
    }
    return null;
  }

  protected abstract String getPackDisplayName();

  protected abstract PackType getSupportedPackType();

  protected abstract int getPackFormat();

  @Override
  public Set<String> getNamespaces(PackType type) {
    if (type == getSupportedPackType()) {
      return Collections.singleton(TFC_NAMESPACE);
    }
    return Collections.emptySet();
  }

  protected String formatNumberWithCommas(int number) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    DecimalFormat formatter = new DecimalFormat("#,###", symbols);
    return formatter.format(number);
  }

  protected boolean isCorrectNamespace(String namespace) {
    return namespace.equals(TFC_NAMESPACE);
  }

  protected IoSupplier<InputStream> createJsonInputStream(String json) {
    return () ->
      new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
  }
}
