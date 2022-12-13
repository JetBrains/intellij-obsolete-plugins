package com.intellij.dmserver.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.openapi.components.PersistentStateComponent;

public abstract class DMFacetConfigurationBase<T extends DMFacetConfigurationBase>
  implements FacetConfiguration, PersistentStateComponent<T> {

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DMFacetConfigurationBase)) {
      return false;
    }
    return true;
  }
}
