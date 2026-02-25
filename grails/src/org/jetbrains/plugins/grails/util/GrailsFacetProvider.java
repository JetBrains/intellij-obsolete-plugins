// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.util;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;

import java.util.Collection;

public interface GrailsFacetProvider {
  ExtensionPointName<GrailsFacetProvider> EP_NAME = ExtensionPointName.create("org.intellij.grails.facetProvider");

  void addFacets(Collection<Consumer<ModifiableFacetModel>> actions, Module module, Collection<VirtualFile> roots);

}
