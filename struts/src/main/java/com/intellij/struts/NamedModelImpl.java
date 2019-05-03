/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.model.impl.DomModelImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class NamedModelImpl<T extends DomElement> extends DomModelImpl<T> implements NamedDomModel<T> {

  private final String myName;

  public NamedModelImpl(@NotNull final Set<XmlFile> configFiles,
                        @NotNull final DomFileElement<T> mergedModel,
                        @NotNull final String name) {
    super(mergedModel, configFiles);

    myName = name;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }
}
