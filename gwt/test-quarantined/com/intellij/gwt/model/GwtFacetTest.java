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

package com.intellij.gwt.model;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.psi.GwtSourcePathsRefresher;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IndexingTestUtil;

import java.io.FileWriter;
import java.util.List;

import static com.intellij.gwt.references.GwtFixtureTestCase.waitForGwtSourcePathsRefresher;

public class GwtFacetTest extends GwtTestCase {

  public void testFacet() throws Exception {
    VirtualFile dir = addGwtModule("model/complexModule");
    VirtualFile clientFile = dir.findFileByRelativePath("pack/client1/client2/ClientFile.java");
    assertNotNull(clientFile);

    GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(myProject, clientFile);
    assertNotNull(gwtFacet);

    GwtSdk gwtSdk = gwtFacet.getConfiguration().getSdk();
    assertSame(GwtVersionImpl.VERSION_1_4, gwtSdk.getVersion());

    GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(myProject);
    List<GwtModule> gwtModules = gwtModulesManager.findGwtModulesByClientSourceFile(clientFile);
    assertNotEmpty(gwtModules);

    assertTrue(gwtSdk.containsJreEmulationClass(gwtModules, String.class.getName()));
    assertFalse(gwtSdk.containsJreEmulationClass(gwtModules, FileWriter.class.getName()));

    GwtSourcePathsRefresher sourcePathsRefresher = GwtSourcePathsRefresher.getInstance(getProject());
    sourcePathsRefresher.enqueueRefreshSourcePathsTask(false);
    waitForGwtSourcePathsRefresher(getProject());
    assertTrue(gwtSdk.containsJreEmulationClass(gwtModules, Thread.class.getName()));
    assertTrue(gwtSdk.containsJreEmulationClass(gwtModules, Thread.class.getName()));

    WriteAction.run(() -> dir.findFileByRelativePath("pack/super1/java/lang/Thread.java").delete(this));
    assertFalse(gwtSdk.containsJreEmulationClass(gwtModules, Thread.class.getName()));
    assertFalse(gwtSdk.containsJreEmulationClass(gwtModules, Thread.class.getName()));
  }

}
