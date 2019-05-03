/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom;

import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.MergingFileDescription;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
abstract class StrutsFileDescriptionBase<T extends DomElement> extends MergingFileDescription<T> {

  protected StrutsFileDescriptionBase(final Class<T> rootElementClass, @NonNls final String rootTagName) {
    super(rootElementClass, rootTagName);
  }

  @Override
  @NotNull
  public Set<?> getDependencyItems(final XmlFile file) {
    return Collections.singleton(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
  }
}

/**
 * Base class for plugin config files description.
 */
abstract class StrutsPluginDescriptorBase<T extends DomElement> extends StrutsFileDescriptionBase<T> {

  protected StrutsPluginDescriptorBase(final Class<T> rootElementClass, final String rootTagName) {
    super(rootElementClass, rootTagName);
  }
}