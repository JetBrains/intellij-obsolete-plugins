/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.util.xml.*;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Presentation(icon = "StrutsApiIcons.DataSource")
public interface DataSource extends StrutsRootElement {

  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @NameValue
  @NotNull
  GenericAttributeValue<String> getKey();


  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @ExtendClass("javax.sql.DataSource")
  @Required
  @NotNull
  GenericAttributeValue<PsiClass> getType();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @ExtendClass("org.apache.struts.config.DataSourceConfig")
  @NotNull
  GenericAttributeValue<PsiClass> getClassName();

  List<SetProperty> getSetProperties();
}
