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
