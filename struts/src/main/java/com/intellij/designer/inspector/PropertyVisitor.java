package com.intellij.designer.inspector;

/**
 * @author spleaner
 */
public interface PropertyVisitor {

  boolean visitProperty(Property property);

}
