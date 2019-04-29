/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.java.JpsJavaModelSerializerExtension;

/**
 * @author nik
 */
public class J2MEModuleExtension extends ModuleExtension {
  @NonNls private static final String EXPLODED_TAG = JpsJavaModelSerializerExtension.EXPLODED_TAG;
  @NonNls private static final String EXCLUDE_EXPLODED_TAG = JpsJavaModelSerializerExtension.EXCLUDE_EXPLODED_TAG;
  @NonNls private static final String ATTRIBUTE_URL = JpsJavaModelSerializerExtension.URL_ATTRIBUTE;
  @Nullable VirtualFilePointer myExplodedDirectoryPointer;
  @Nullable private String myExplodedDirectory;
  private boolean myExcludeExploded;
  private J2MEModuleExtension mySource;

  public static J2MEModuleExtension getInstance(Module module) {
    return ModuleRootManager.getInstance(module).getModuleExtension(J2MEModuleExtension.class);
  }

  public J2MEModuleExtension() {
  }

  public J2MEModuleExtension(J2MEModuleExtension source) {
    mySource = source;
    myExcludeExploded = source.myExcludeExploded;
    myExplodedDirectory = source.myExplodedDirectory;
    final VirtualFilePointer pointer = source.myExplodedDirectoryPointer;
    myExplodedDirectoryPointer = pointer != null ? VirtualFilePointerManager.getInstance().duplicate(pointer, this, null) : null;
  }

  @NotNull
  @Override
  public ModuleExtension getModifiableModel(boolean writable) {
    return new J2MEModuleExtension(this);
  }

  @Override
  public void commit() {
    if (mySource != null) {
      mySource.setExcludeExplodedDirectory(myExcludeExploded);
      mySource.setExplodedDirectory(myExplodedDirectoryPointer != null ? myExplodedDirectoryPointer.getUrl() : null);
    }
  }

  @Override
  public boolean isChanged() {
    return myExcludeExploded != mySource.myExcludeExploded || !Comparing.equal(getExplodedDirectoryUrl(), mySource.getExplodedDirectoryUrl());
  }

  @Override
  public void dispose() {
    mySource = null;
    myExplodedDirectoryPointer = null;
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    myExcludeExploded = element.getChild(EXCLUDE_EXPLODED_TAG) != null;
    final Element outputPathChild = element.getChild(EXPLODED_TAG);
    myExplodedDirectory = outputPathChild != null ? outputPathChild.getAttributeValue(ATTRIBUTE_URL) : null;
    myExplodedDirectoryPointer = myExplodedDirectory != null ? VirtualFilePointerManager.getInstance().create(myExplodedDirectory, this, null) : null;
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    if (myExplodedDirectory != null) {
      final Element pathElement = new Element(EXPLODED_TAG);
      pathElement.setAttribute(ATTRIBUTE_URL, myExplodedDirectory);
      element.addContent(pathElement);
    }

    if (myExcludeExploded) {
      element.addContent(new Element(EXCLUDE_EXPLODED_TAG));
    }
  }

  @Nullable
  public VirtualFile getExplodedDirectory() {
    return myExplodedDirectoryPointer == null ? null : myExplodedDirectoryPointer.getFile();
  }

  @Nullable
  public VirtualFilePointer getExplodedDirectoryPointer() {
    return myExplodedDirectoryPointer;
  }

  public void setExplodedDirectory(@Nullable VirtualFile file) {
    setExplodedDirectory(file == null ? null : file.getUrl());
  }

  public void setExplodedDirectory(@Nullable String url) {
    myExplodedDirectory = url;
    myExplodedDirectoryPointer = url == null ? null : VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  public boolean isExcludeExplodedDirectory() {
    return myExcludeExploded;
  }

  public void setExcludeExplodedDirectory(boolean excludeExploded) {
    myExcludeExploded = excludeExploded;
  }

  @Nullable
  public String getExplodedDirectoryUrl() {
    return myExplodedDirectoryPointer == null ? null : myExplodedDirectoryPointer.getUrl();
  }

}
