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

package com.intellij.gwt.module.model;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.HyphenNameStrategy;
import com.intellij.util.xml.NameStrategyForAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@NameStrategyForAttributes(HyphenNameStrategy.class)
public interface GwtModule extends DomElement {
  GwtModule[] EMPTY_ARRAY = new GwtModule[0];

  GenericAttributeValue<String> getRenameTo();

  List<GwtEntryPoint> getEntryPoints();
  GwtEntryPoint addEntryPoint();

  List<GwtRelativePath> getSources();
  GwtRelativePath addSource();

  List<GwtRelativePath> getSuperSources();
  GwtRelativePath addSuperSource();

  List<GwtRelativePath> getPublics();
  GwtRelativePath addPublic();

  List<GwtServlet> getServlets();
  GwtServlet addServlet();

  List<GwtInheritsEntry> getInheritses();
  GwtInheritsEntry addInherits();

  List<GwtStylesheetRef> getStylesheets();

  @NlsSafe String getOutputName();

  @NlsSafe String getQualifiedName();

  VirtualFile getModuleFile();

  XmlFile getModuleXmlFile();

  @NlsSafe String getShortName();

  Collection<VirtualFile> getSourceRoots(final boolean includeTests);

  Collection<VirtualFile> getPublicRoots();

  Collection<VirtualFile> getPublicRoots(final boolean includeTests);

  VirtualFile getModuleDirectory();

  List<GwtModule> getInherited(final GlobalSearchScope scope);

  List<StylesheetFile> getStylesheetFiles();

  boolean isSourceFile(@NotNull VirtualFile file);

  Map<VirtualFile, GwtRelativePath> getSourceRoots();

  Map<VirtualFile, GwtRelativePath> getSuperSourceRoots();

  boolean isPublicFile(@NotNull VirtualFile file);

  Collection<VirtualFile> getSuperSourceRoots(final boolean includeTests);
}
