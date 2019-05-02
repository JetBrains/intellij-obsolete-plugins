/*
 * Copyright 2000-2006 JetBrains s.r.o.
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
