/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom.tiles;

import com.intellij.javaee.model.xml.SecurityRole;
import com.intellij.struts.SecurityRoleScopeProvider;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Yann C&eacute;bron
 */
public interface PutListAttribute extends DomElement {

  @NameValue
  @Required
  @NotNull
  GenericAttributeValue<String> getName();

  @NotNull
  @Scope(SecurityRoleScopeProvider.class)
  GenericAttributeValue<SecurityRole> getRole();

  List<Add> getAddAttributes();

  Add addAddAttribute();

  List<Item> getItems();

  Item addItem();


  List<Bean> getBeans();

  Bean addBean();

  GenericAttributeValue<Boolean> getInherit();

  GenericAttributeValue<Boolean> getCascade();
}
