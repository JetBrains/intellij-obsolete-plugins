// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

public abstract class ModuleCachedValue<R> extends GrailsCachedValue<Module, R> {

  protected ModuleCachedValue() {
  }

  protected ModuleCachedValue(Key dependency) {
    super(dependency);
  }

  @Override
  protected Project getProject(Module element) {
    return element.getProject();
  }

}
