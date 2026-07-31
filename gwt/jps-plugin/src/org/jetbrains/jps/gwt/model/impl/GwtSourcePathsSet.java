package org.jetbrains.jps.gwt.model.impl;

import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;

import java.util.HashSet;
import java.util.Set;

@Tag("gwt-sources")
public class GwtSourcePathsSet {
  @XCollection(propertyElementName = "list", elementName = "source")
  public Set<GwtSourcePath> mySourcePaths = new HashSet<>();

  @XCollection(propertyElementName = "super-sources-list", elementName = "source")
  public Set<GwtSourcePath> mySuperSourcePaths = new HashSet<>();
}
