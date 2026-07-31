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

package org.jetbrains.jps.gwt.model;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum GwtJavaScriptOutputStyle {
  OBFUSCATED("OBF", "Obfuscated", 1),
  PRETTY("PRETTY", "Pretty", 2),
  DETAILED("DETAILED", "Detailed", 3);
  private final @NonNls String myId;
  private final int myNumericId;
  private final String myPresentableName;

  GwtJavaScriptOutputStyle(final @NonNls String id, final String presentableName, int numericId) {
    myPresentableName = presentableName;
    myId = id;
    myNumericId = numericId;
  }

  public String getId() {
    return myId;
  }

  public int getNumericId() {
    return myNumericId;
  }

  public @NotNull String getPresentableName() {
    return myPresentableName;
  }

  @Override
  public String toString() {
    return myId;
  }

  public static @Nullable GwtJavaScriptOutputStyle byId(final int id) {
    for (GwtJavaScriptOutputStyle style : values()) {
      if (style.getNumericId() == id) {
        return style;
      }
    }
    return null;
  }

  public static @Nullable GwtJavaScriptOutputStyle byId(@Nullable String id) {
    for (GwtJavaScriptOutputStyle style : values()) {
      if (style.getId().equals(id)) {
        return style;
      }
    }
    return null;
  }
}
