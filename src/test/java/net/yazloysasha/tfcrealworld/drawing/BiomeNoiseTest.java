package net.yazloysasha.tfcrealworld.drawing;

import static net.dries007.tfc.world.TFCChunkGenerator.*;

import java.awt.Color;
import java.util.function.DoubleFunction;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.Artist;
import net.yazloysasha.tfcrealworld.TestHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class BiomeNoiseTest extends TestHelper {

  private final DoubleFunction<Color> green = Artist.Colors.linearGradient(
    new Color(0, 90, 0),
    new Color(90, 240, 90)
  );
  private final DoubleFunction<Color> blue = Artist.Colors.linearGradient(
    new Color(90, 170, 240),
    new Color(10, 80, 140)
  );
  private final Artist.Noise<Noise2D> terrain = Artist.<Noise2D>forNoise(
    instance -> Artist.NoisePixel.coerceFloat(instance::noise)
  )
    .scale((value, min, max) ->
      value > SEA_LEVEL_Y
        ? Mth.clampedMap((int) value, SEA_LEVEL_Y, max, 0, 1)
        : Mth.clampedMap((int) value, SEA_LEVEL_Y, min, 0, -1)
    )
    .color(x -> x < 0 ? blue.apply(-x) : green.apply(x))
    .dimensions(400)
    .size(400);

  @Test
  public void testRollingHills() {
    terrain.draw("noise_rolling_hills", BiomeNoise.hills(seed(), -5, 28));
  }

  @Test
  public void testLowCanyons() {
    terrain.draw("noise_low_canyons", BiomeNoise.canyons(seed(), -8, 21));
  }

  @Test
  public void testCanyons() {
    terrain.draw("noise_canyons", BiomeNoise.canyons(seed(), -2, 40));
  }

  @Test
  public void testSharpHills() {
    terrain.draw("noise_sharp_hills", BiomeNoise.sharpHills(seed()));
  }

  @Test
  public void testLakes() {
    terrain.draw("noise_lakes", BiomeNoise.lake(seed()));
  }
}
