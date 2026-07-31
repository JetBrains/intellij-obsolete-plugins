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


public class Gwt15InspectionsTest extends GwtInspectionsTestCase {
  @Override
  protected GwtVersionImpl getVersion() {
    return GwtVersionImpl.VERSION_1_5;
  }

  public void testNonSerializableMethodParameters15() {
    doTest(new GwtNonSerializableRemoteServiceMethodParametersInspection());
  }

  public void testInconsistentSerializable15() {
    doTest(new GwtInconsistentSerializableClassInspection());
  }

  public void testObsoleteTypeArgsTag15() {
    doTest(new GwtObsoleteTypeArgsJavadocTagInspection());
  }

  public void testRawAsyncCallback15() {
    doTest(new GwtRawAsyncCallbackInspection());
  }

  public void testDeprecatedKeyTag() {
    doTest(new GwtDeprecatedPropertyKeyJavadocTagInspection());
  }

  public void testRedundantSetServicePathCalls() {
    doTest(new RedundantSetServiceEntryPointMethodCallsInspection());
  }
}
