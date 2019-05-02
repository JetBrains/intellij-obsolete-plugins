/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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