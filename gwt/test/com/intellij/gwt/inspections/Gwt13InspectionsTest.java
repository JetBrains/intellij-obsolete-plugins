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

package com.intellij.gwt.inspections;

import com.intellij.gwt.sdk.impl.GwtVersionImpl;

public class Gwt13InspectionsTest extends GwtInspectionsTestCase {

  @Override
  protected GwtVersionImpl getVersion() {
    return GwtVersionImpl.VERSION_FROM_1_1_TO_1_3;
  }

  public void testNonSerializableMethodParameters() {
    doTest(new GwtNonSerializableRemoteServiceMethodParametersInspection());
  }

  public void testInconsistentSerializable() {
    doTest(new GwtInconsistentSerializableClassInspection());
  }
}
