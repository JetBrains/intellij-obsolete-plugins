/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package com.intellij.gwt.sdk.impl;

import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@State(name = "GwtSdkManager", storages = @Storage(value = "applicationLibraries.xml", roamingType = RoamingType.DISABLED))
public final class GwtSdkManagerImpl extends GwtSdkManager implements PersistentStateComponent<GwtSdkManagerImpl.GwtSdkList> {
  private final Map<String, GwtSdk> myGwtSdkMap = new HashMap<>();
  private GwtSdkList myGwtSdkList = new GwtSdkList();

  public GwtSdkManagerImpl() {
    GwtSdkType.EP_NAME.addChangeListener(myGwtSdkMap::clear, null);
  }

  @Override
  public @NotNull GwtSdk getGwtSdk(final @NotNull String sdkHomeUrl, @Nullable String sdkTypeId) {
    String key = sdkTypeId + ":" + sdkHomeUrl;
    GwtSdk gwtSdk = myGwtSdkMap.get(key);
    if (gwtSdk == null) {
      gwtSdk = createGwtSdk(sdkHomeUrl, sdkTypeId);
      myGwtSdkMap.put(key, gwtSdk);
    }
    return gwtSdk;
  }

  @Override
  public @NotNull GwtSdk getGwtSdk(@NotNull String sdkHomeUrl) {
    GwtSdkType sdkType = detectSdkType(sdkHomeUrl);
    return getGwtSdk(sdkHomeUrl, sdkType != null ? sdkType.getId() : null);
  }

  private static GwtSdk createGwtSdk(@NotNull String sdkHomeUrl, @Nullable String sdkTypeId) {
    final GwtSdkType type = GwtSdkType.findType(sdkTypeId);
    if (type != null) {
      return type.createSdk(sdkHomeUrl);
    }
    return new GwtSdkImpl(sdkHomeUrl);
  }

  @Override
  public void registerGwtSdk(final @NotNull GwtSdk gwtSdk) {
    final String url = gwtSdk.getHomeDirectoryUrl();
    if (gwtSdk.isValid() && !myGwtSdkList.getSdkInstallations().contains(url)) {
      myGwtSdkList.getSdkInstallations().add(0, StringUtil.trimEnd(url, "/"));
    }
  }

  @Override
  public void moveToTop(final @NotNull GwtSdk sdk) {
    if (sdk.isValid()) {
      String url = sdk.getHomeDirectoryUrl();
      List<String> list = myGwtSdkList.getSdkInstallations();
      list.remove(url);
      final String trimmedUrl = StringUtil.trimEnd(url, "/");
      list.remove(trimmedUrl);
      list.add(trimmedUrl);
    }
  }

  @Override
  public @Nullable GwtSdk suggestGwtSdk() {
    GwtSdk last = null;
    for (String url : myGwtSdkList.getSdkInstallations()) {
      GwtSdk sdk = getGwtSdk(url, null);
      if (sdk.isValid()) {
        last = sdk;
      }
    }
    return last;
  }

  @Override
  public List<String> getAllSdkPaths() {
    ArrayList<String> paths = new ArrayList<>();
    for (String url : myGwtSdkList.getSdkInstallations()) {
      paths.add(0, VfsUtilCore.urlToPath(url));
    }
    return paths;
  }

  @Override
  public void removeInvalidSdk() {
    final Iterator<String> iterator = myGwtSdkList.getSdkInstallations().iterator();
    while (iterator.hasNext()) {
      String url = iterator.next();
      final GwtSdk gwtSdk = getGwtSdk(url);
      if (!gwtSdk.isValid()) {
        iterator.remove();
      }
    }
  }

  @Override
  public GwtSdkList getState() {
    return myGwtSdkList;
  }

  @Override
  public void loadState(final @NotNull GwtSdkList state) {
    myGwtSdkList = state;
  }

  @Override
  public @Nullable GwtSdkType detectSdkType(final String homeDirectoryUrl) {
    for (GwtSdkType sdkType : GwtSdkType.EP_NAME.getExtensionList()) {
      if (sdkType.isValidSdkHomeDirectory(new File(VfsUtilCore.urlToPath(homeDirectoryUrl)))) {
        return sdkType;
      }
    }
    return null;
  }

  static final class GwtSdkList {
    private List<String> mySdkInstallations = new ArrayList<>();

    @XCollection(elementName = "gwt-sdk", valueAttributeName = "url", propertyElementName = "gwt-sdk-list")
    public List<String> getSdkInstallations() {
      return mySdkInstallations;
    }

    public void setSdkInstallations(List<String> sdkInstallations) {
      mySdkInstallations = sdkInstallations;
    }
  }

  static final class RootsChangeListener implements ModuleRootListener {
    @Override
    public void rootsChanged(@NotNull ModuleRootEvent event) {
      GwtSdkManager gwtSdkManager = ApplicationManager.getApplication().getServiceIfCreated(GwtSdkManager.class);
      if (gwtSdkManager != null) {
        for (GwtSdk gwtSdk : ((GwtSdkManagerImpl)gwtSdkManager).myGwtSdkMap.values()) {
          gwtSdk.clearCaches();
        }
      }
    }
  }
}
