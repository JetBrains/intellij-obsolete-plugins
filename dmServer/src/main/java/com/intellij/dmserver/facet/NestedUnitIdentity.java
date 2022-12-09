package com.intellij.dmserver.facet;

import com.intellij.dmserver.util.VersionUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Transient;

/**
 * @author michael.golubev
 */
public class NestedUnitIdentity implements Comparable<NestedUnitIdentity> {

  private static final Logger LOG = Logger.getInstance(NestedUnitIdentity.class);

  private String myVersionRange;

  private String myModuleName;

  private ModulePointer myModulePointer;

  public NestedUnitIdentity() {

  }

  public NestedUnitIdentity(Module module) {
    setModule(module);
    setVersionRange(VersionUtils.emptyRange.toString());
  }

  @Transient
  public void init(Project project) {
    if (myModulePointer == null) {
      if (myModuleName == null) {
        LOG.error("Either module name or module pointer should be set before calling the init");
        return;
      }
      myModulePointer = ModulePointerManager.getInstance(project).create(myModuleName);
    }
  }

  public String getModuleName() {
    return myModulePointer == null ? myModuleName : myModulePointer.getModuleName();
  }

  public void setModuleName(String moduleName) {
    myModuleName = moduleName;
  }

  public String getVersionRange() {
    return myVersionRange == null ? VersionUtils.emptyRange.toString() : myVersionRange;
  }

  public void setVersionRange(String versionRange) {
    myVersionRange = versionRange;
  }

  @Override
  public int compareTo(NestedUnitIdentity o) {
    return getModuleName().compareTo(o.getModuleName());
  }

  @Transient
  public Module getModule() {
    return myModulePointer.getModule();
  }

  @Transient
  public void setModule(Module module) {
    myModulePointer = ModulePointerManager.getInstance(module.getProject()).create(module);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof NestedUnitIdentity)) {
      return false;
    }
    NestedUnitIdentity asInstance = (NestedUnitIdentity)obj;
    return getModule() == asInstance.getModule() //
           && getVersionRange().equals(asInstance.getVersionRange());
  }

  @Override
  public NestedUnitIdentity clone() {
    NestedUnitIdentity result = new NestedUnitIdentity();
    result.setModule(getModule());
    result.setVersionRange(getVersionRange());
    return result;
  }
}
