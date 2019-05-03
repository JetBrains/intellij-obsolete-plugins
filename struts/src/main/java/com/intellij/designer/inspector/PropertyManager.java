package com.intellij.designer.inspector;

/**
 * @author spleaner
 */
public interface PropertyManager<P extends Property> {

  boolean canCreateProperties();
  P createProperty(Property parent);

  boolean isRemovable(P property);
  void removeProperty(P property);

}
