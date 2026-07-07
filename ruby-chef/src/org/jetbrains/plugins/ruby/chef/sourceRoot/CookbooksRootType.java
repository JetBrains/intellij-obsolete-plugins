package org.jetbrains.plugins.ruby.chef.sourceRoot;

import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.ex.JpsElementTypeWithDummyProperties;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

public final class CookbooksRootType extends JpsElementTypeWithDummyProperties implements JpsModuleSourceRootType<JpsDummyElement> {
  public static final CookbooksRootType COOKBOOKS = new CookbooksRootType();

  private CookbooksRootType() { }
}