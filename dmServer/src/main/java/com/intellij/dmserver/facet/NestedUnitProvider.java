package com.intellij.dmserver.facet;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Ref;

import java.util.ArrayList;
import java.util.List;

public class NestedUnitProvider {

  private final Module myConfiguredModule;
  private final ModulesProvider myModulesProvider;

  public NestedUnitProvider(Module configuredModule, ModulesProvider modulesProvider) {
    myConfiguredModule = configuredModule;
    myModulesProvider = modulesProvider;
  }

  public List<Module> getPossibleNestedModules(DMCompositeFacetConfiguration configuration) {
    List<Module> result = new ArrayList<>();

    for (Module module : myModulesProvider.getModules()) {
      if (module.equals(myConfiguredModule)) {
        continue;
      }

      if (isPossibleNestedModule(module, configuration)) {
        result.add(module);
      }
    }
    return result;
  }

  public static boolean isPossibleNestedModule(Module module, final DMCompositeFacetConfiguration configuration) {
     return isPossibleNestedModule(module, configuration, new Ref<>());
  }

  public static boolean isPossibleNestedModule(Module module,
                                               final DMCompositeFacetConfiguration configuration,
                                               final Ref<? super String> errorRef) {
    Boolean result = new DMFacetsSwitch<Boolean>() {

      @Override
      protected Boolean doProcessBundleFacet(DMBundleFacet bundleFacet) {
        return true;
      }

      @Override
      protected Boolean doProcessCompositeFacet(DMCompositeFacet compositeFacet) {
        if (configuration.getCompositeType() == DMCompositeType.PAR) {
          errorRef.set(DmServerBundle.message("NestedUnitProvider.error.par.inside.par"));
          return false;
        } else {
            DMCompositeFacetConfiguration compositeFacetConfiguration = compositeFacet.getConfigurationImpl();
            boolean nestedScope = configuration.getScoped()
                                  && (compositeFacetConfiguration.getCompositeType() == DMCompositeType.PAR
                                      || compositeFacetConfiguration.getScoped());
            if (nestedScope) {
              errorRef.set(DmServerBundle.message("NestedUnitProvider.error.nested.scopes"));
            }
            return !nestedScope;
        }
      }

      @Override
      protected Boolean doProcessConfigFacet(DMConfigFacet configFacet) {
        return true;
      }
    }.processModule(module);

    if (result == null) {
      errorRef.set(DmServerBundle.message("NestedUnitProvider.error.not.single.dm.facet"));
      return false;
    }
    else {
      return result;
    }
  }
}
