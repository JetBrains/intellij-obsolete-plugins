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

import com.intellij.gwt.clientBundle.CssResourceClassErrorsInspection;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;

public class Gwt2InspectionsTest extends GwtInspectionsTestCase {
  @Override
  protected GwtVersionImpl getVersion() {
    return GwtVersionImpl.VERSION_2_0;
  }

  public void testNonJREEmulationClasses() {
    doTest(new NonJREEmulationClassesInClientCodeInspection());
  }

  public void testClientClassFromNonInheritedModule() {
    doTest(new ClientClassFromNonInheritedModuleUsageInspection());
  }

  public void testConsistentAsyncWithGenericSuperclass() {
    doTest(new GwtInconsistentAsyncInterfaceInspection());
  }

  public void testConsistentSyncWithGenericSuperclass() {
    doTest(new GwtInconsistentAsyncInterfaceInspection());
  }

  public void testUnregisteredService() {
    doTest(new GwtServiceNotRegisteredInspection());
  }

  public void testIncorrectRemoteServiceUrlMapping() {
    doTest(new GwtServiceNotRegisteredInspection());
  }

  public void testUiFieldErrors() {
    doTest(new GwtUiFieldErrorsInspection());
  }

  public void testUiHandlerErrors() {
    doTest(new GwtUiHandlerErrorsInspection());
  }

  public void testCssResource() {
    doTest(new CssResourceClassErrorsInspection());
  }

  public void testCssResourceInUiBinder() {
    doTest(new CssResourceClassErrorsInspection());
  }

  public void testUiFieldAssignment() {
    doTest(new GwtUiFieldAssignmentInspection());
  }
}
