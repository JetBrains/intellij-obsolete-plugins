package com.intellij.gwt;

import com.intellij.javaee.ResourceRegistrar;
import com.intellij.javaee.StandardResourceProvider;

final class GwtResourceProvider implements StandardResourceProvider {
  @Override
  public void registerResources(ResourceRegistrar registrar) {
    ClassLoader classLoader = getClass().getClassLoader();
    registrar.addStdResource("http://google-web-toolkit.googlecode.com/svn/releases/1.5/distro-source/core/src/gwt-module.dtd",
                             "schemas/gwt-module-1.5.dtd", classLoader);
    registrar.addStdResource("http://google-web-toolkit.googlecode.com/svn/releases/1.6/distro-source/core/src/gwt-module.dtd",
                             "schemas/gwt-module-1.6.dtd", classLoader);
    registrar.addStdResource("http://google-web-toolkit.googlecode.com/svn/releases/2.0/distro-source/core/src/gwt-module.dtd",
                             "schemas/gwt-module-2.0.dtd", classLoader);
    registrar.addStdResource("http://dl.google.com/gwt/DTD/xhtml.ent",
                             "schemas/xhtml.ent", classLoader);
  }
}
