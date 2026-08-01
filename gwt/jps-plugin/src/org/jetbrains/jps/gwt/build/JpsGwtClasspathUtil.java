package org.jetbrains.jps.gwt.build;

import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleDependency;
import org.jetbrains.jps.model.module.JpsModuleSourceDependency;

import java.io.File;
import java.util.Collection;

public final class JpsGwtClasspathUtil {
  public static Collection<File> getSourceRootsOfGwtModules(JpsModule module, boolean productionOnly) {
    JpsJavaDependenciesEnumerator enumerator = JpsJavaExtensionService.dependencies(module);
    if (productionOnly) {
      enumerator = enumerator.productionOnly();
    }
    return enumerator.withoutSdk().recursively().satisfying(dependencyElement -> {
      if (dependencyElement instanceof JpsModuleDependency) return true;
      if (dependencyElement instanceof JpsModuleSourceDependency) {
        return JpsGwtExtensionService.getInstance().getExtension(dependencyElement.getContainingModule()) != null;
      }
      return false;
    }).sources().getRoots();
  }
}
