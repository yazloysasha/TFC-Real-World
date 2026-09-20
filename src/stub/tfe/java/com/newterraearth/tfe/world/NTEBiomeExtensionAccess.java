package com.newterraearth.tfe.world;

import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;

public interface NTEBiomeExtensionAccess {
  NTECenteredFeatureBlendType tfe$getCenteredFeatureBlendType();

  void tfe$setCenteredFeatureBlendType(NTECenteredFeatureBlendType blendType);

  int tfe$getCenteredFeatureRarity();

  int tfe$getCenteredFeatureBaseHeight();

  int tfe$getCenteredFeatureScaleHeight();
}
