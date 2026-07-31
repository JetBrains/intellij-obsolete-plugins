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

public class GwtInspectionsTest extends GwtInspectionsTestCase {
  @Override
  protected GwtVersionImpl getVersion() {
    return null;
  }

  public void testI18n() {
    doTest(new GwtInconsistentLocalizableInterfaceInspection());
  }

  public void testInconsistentAsync() {
    doTest(new GwtInconsistentAsyncInterfaceInspection());
  }

  public void testMethodWithParametersInConstantsInterface() {
    doTest(new GwtMethodWithParametersInConstantsInterfaceInspection());
  }

  public void testObsoleteTypeArgsTag14() {
    doTest(new GwtObsoleteTypeArgsJavadocTagInspection());
  }


  public void testRawAsyncCallback14() {
    doTest(new GwtRawAsyncCallbackInspection());
  }

  public void testUnresolvedCssReferences() {
    doTest(new GwtToCssClassReferencesInspection());
  }

  public void testDefaultPackageNotRegistered() {
    doTest(new GwtDefaultPackageNotRegisteredInspection());
  }

  public void testUnresolvedUiXmlReferences() {
    doTest(new UiXmlUnresolvedReferencesInspection());
  }

  public void testMenuItemInUiBinder() {
    doTest(new UiXmlUnresolvedReferencesInspection());
  }

  public void testOverlayTypeRestrictions() {
    doTest(new GwtOverlayTypeRestrictionsInspection());
  }

  public void testCreateMethodCall() {
    doTest(new GwtCreateMethodCallInspection());
  }
}
