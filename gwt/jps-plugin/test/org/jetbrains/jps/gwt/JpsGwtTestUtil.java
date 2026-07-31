package org.jetbrains.jps.gwt;

import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.gwt.model.impl.GwtModuleExtensionProperties;
import org.jetbrains.jps.gwt.model.impl.JpsGwtModuleExtensionImpl;
import org.jetbrains.jps.model.module.JpsModule;

public final class JpsGwtTestUtil {
  public static JpsGwtModuleExtension addGwtExtension(JpsModule appModule, final GwtModuleExtensionProperties properties) {
    JpsGwtModuleExtension extension = new JpsGwtModuleExtensionImpl(properties);
    JpsGwtExtensionService.getInstance().setExtension(appModule, extension);
    return extension;
  }

  public static JpsGwtModuleExtension addGwtExtension(JpsModule module) {
    return addGwtExtension(module, new GwtModuleExtensionProperties());
  }
}
