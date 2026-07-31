package org.jetbrains.jps.gwt.index.impl;

import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.gwt.index.JpsGwtModuleIndex;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JpsGwtModuleIndexImpl implements JpsGwtModuleIndex {
  private final MultiMap<JpsModule, JpsGwtModule> myModules;
  private final MultiMap<String, JpsGwtModule> myModulesByName;
  private final Map<JpsGwtModule, InheritedModulesInfo> myInheritanceMap;
  
  public JpsGwtModuleIndexImpl(JpsModel model, BuildDataPaths buildDataPaths) {
    myModules = new MultiMap<>();
    myModulesByName = new MultiMap<>();
    myInheritanceMap = new HashMap<>();
    List<JpsGwtModule> gwtModules = new JpsGwtModulesCache(buildDataPaths).loadGwtModules(model);
    for (JpsGwtModule gwtModule : gwtModules) {
      myModules.putValue(gwtModule.getModule(), gwtModule);
      myModulesByName.putValue(gwtModule.getQualifiedName(), gwtModule);
    }
    for (JpsGwtModule module : myModules.values()) {
      processInherited(module);
    }
  }

  private InheritedModulesInfo processInherited(JpsGwtModule module) {
    InheritedModulesInfo old = myInheritanceMap.get(module);
    if (old != null) return old;

    InheritedModulesInfo info = new InheritedModulesInfo();
    myInheritanceMap.put(module, info);
    for (String name : module.getInheritedNames()) {
      Collection<JpsGwtModule> modules = myModulesByName.get(name);
      if (modules.isEmpty()) {
        info.myMayHaveEntryPoints = true;
        continue;
      }
      for (JpsGwtModule inherited : modules) {
        InheritedModulesInfo inheritedInfo = processInherited(inherited);
        info.myMayHaveEntryPoints |= inheritedInfo.myMayHaveEntryPoints;
        info.myInheritedModules.add(inherited);
        info.myInheritedModules.addAll(inheritedInfo.myInheritedModules);
      }
    }
    return info;
  }

  @Override
  public Collection<JpsGwtModule> getModulesByName(String qualifiedName) {
    return myModulesByName.get(qualifiedName);
  }

  @Override
  public @NotNull Collection<JpsGwtModule> getModulesToCompile(@NotNull JpsGwtModuleExtension extension,
                                                               boolean includeTests) {
    return getModules(extension, includeTests, true);
  }

  @Override
  public @NotNull Collection<JpsGwtModule> getModules(@NotNull JpsGwtModuleExtension extension,
                                                      boolean includeTests, final boolean compilableOnly) {
    Collection<JpsGwtModule> modules = myModules.get(extension.getModule());
    List<JpsGwtModule> result = new ArrayList<>();
    for (JpsGwtModule module : modules) {
      if (!includeTests && module.isInTests()) continue;

      if (!compilableOnly || isCompilable(module, extension)) {
        result.add(module);
      }
    }
    return result;
  }

  @Override
  public boolean isCompilable(JpsGwtModule module, JpsGwtModuleExtension extension) {
    return !isLibraryModule(module) && extension.isModuleCompilationEnabled(module);
  }

  private boolean isLibraryModule(JpsGwtModule module) {
    if (module.hasEntryPoints()) {
      return false;
    }
    return !processInherited(module).myMayHaveEntryPoints;
  }

  @Override
  public @NotNull Collection<JpsGwtModule> getInheritedModules(@NotNull JpsGwtModule module) {
    return processInherited(module).myInheritedModules;
  }

  private static class InheritedModulesInfo {
    private boolean myMayHaveEntryPoints;
    private final Set<JpsGwtModule> myInheritedModules = new LinkedHashSet<>();
  }
}
