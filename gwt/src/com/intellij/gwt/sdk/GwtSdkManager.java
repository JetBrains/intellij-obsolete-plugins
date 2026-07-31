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

package com.intellij.gwt.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class GwtSdkManager {

  public static GwtSdkManager getInstance() {
    return ApplicationManager.getApplication().getService(GwtSdkManager.class);
  }

  public abstract @Nullable GwtSdkType detectSdkType(String homeDirectoryUrl);

  public abstract @NotNull GwtSdk getGwtSdk(@NotNull String sdkHomeUrl, @Nullable String sdkTypeId);

  public abstract @NotNull GwtSdk getGwtSdk(@NotNull String sdkHomeUrl);

  public abstract void registerGwtSdk(@NotNull GwtSdk gwtSdk);

  public abstract void moveToTop(@NotNull GwtSdk sdk);

  public abstract @Nullable GwtSdk suggestGwtSdk();

  public abstract List<@NlsSafe String> getAllSdkPaths();

  public abstract void removeInvalidSdk();
}
