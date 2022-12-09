package com.intellij.dmserver.facet;

import com.intellij.dmserver.artifacts.ManifestManager;
import com.intellij.dmserver.util.VersionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author michael.golubev
 */
public final class DMUnitDescriptorProvider extends DMFacetsSwitch<DMUnitDescriptor> {
  private static final Map<DMCompositeType, DMUnitType> ourComposite2UnitType;

  static {
    ourComposite2UnitType = new HashMap<>();
    ourComposite2UnitType.put(DMCompositeType.PAR, DMUnitType.PAR);
    ourComposite2UnitType.put(DMCompositeType.PLAN, DMUnitType.PLAN);
  }

  private static final DMUnitDescriptorProvider ourInstance = new DMUnitDescriptorProvider();

  public static DMUnitDescriptorProvider getInstance() {
    return ourInstance;
  }

  private DMUnitDescriptorProvider() {

  }

  @Override
  protected DMUnitDescriptor doProcessBundleFacet(DMBundleFacet bundleFacet) {
    ManifestManager.FileWrapper manifestFileWrapper = ManifestManager.getBundleInstance().findManifest(bundleFacet.getModule());
    if (manifestFileWrapper == null) {
      return null;
    }
    return new DMUnitDescriptor(DMUnitType.BUNDLE, manifestFileWrapper.getSymbolicName(), manifestFileWrapper.getVersion());
  }

  @Override
  protected DMUnitDescriptor doProcessCompositeFacet(DMCompositeFacet compositeFacet) {
    DMCompositeFacetConfiguration configuration = compositeFacet.getConfigurationImpl();
    return new DMUnitDescriptor(ourComposite2UnitType.get(configuration.getCompositeType()),
                                configuration.getName(compositeFacet.getModule()),
                                configuration.getVersion());
  }

  @Override
  protected DMUnitDescriptor doProcessConfigFacet(DMConfigFacet configFacet) {
    DMConfigFacetConfiguration configuration = configFacet.getConfigurationImpl();
    return new DMUnitDescriptor(DMUnitType.CONFIG, configuration.getName(configFacet.getModule()), VersionUtils.emptyRange.toString());
  }
}
