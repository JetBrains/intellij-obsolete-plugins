package com.jetbrains.plugins.compass;

import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.sass.extensions.SassExtension;

public class SassCompassExtensionStartupActivity implements StartupActivity.DumbAware {
  @Override
  public void runActivity(@NotNull Project project) {
    SassExtension.EXTENSION_POINT_NAME.addExtensionPointListener(new CompassExtensionPointListener(project), project);
    CompassSassExtension extension = CompassUtil.getCompassExtension();
    if (extension != null) {
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        extension.startActivity(module);
      }
    }
  }

  private static class CompassExtensionPointListener implements ExtensionPointListener<SassExtension> {
    private final Project myProject;

    private CompassExtensionPointListener(Project project) {
      myProject = project;
    }

    @Override
    public void extensionAdded(@NotNull SassExtension extension, @NotNull PluginDescriptor pluginDescriptor) {
      if (extension instanceof CompassSassExtension) {
        restartCompassExtensions(myProject);
      }
    }

    @Override
    public void extensionRemoved(@NotNull SassExtension extension, @NotNull PluginDescriptor pluginDescriptor) {
      if (extension instanceof CompassSassExtension) {
        restartCompassExtensions(myProject);
      }
    }
  }

  private static void restartCompassExtensions(@NotNull Project project) {
    CompassSassExtension currentExtension = CompassUtil.getCompassExtension();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    SassExtension.EXTENSION_POINT_NAME.extensions().filter(CompassSassExtension.class::isInstance).forEach(e -> {
      if (currentExtension != e) {
        for (Module module : modules) {
          ((CompassSassExtension)e).stopActivity(module);
          if (currentExtension != null) {
            currentExtension.startActivity(module);
          }
        }
      }
    });
  }

  public static class MyModuleListener implements ModuleListener {
    @Override
    public void moduleAdded(@NotNull Project project, @NotNull Module module) {
      CompassSassExtension extension = CompassUtil.getCompassExtension();
      if (extension != null) {
        extension.startActivity(module);
      }
    }

    @Override
    public void beforeModuleRemoved(@NotNull Project project, @NotNull Module module) {
      CompassSassExtension extension = CompassUtil.getCompassExtension();
      if (extension != null) {
        extension.stopActivity(module);
      }
    }
  }
}
