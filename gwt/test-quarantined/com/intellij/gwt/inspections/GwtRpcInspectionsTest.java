package com.intellij.gwt.inspections;

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspectionBase;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/highlighting/rpc/")
public class GwtRpcInspectionsTest extends GwtCodeInsightTestCase {
  public void testSyncMethodsInSuperInterface() {
    myCodeInsightFixture.enableInspections(GwtInconsistentAsyncInterfaceInspection.class);
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true, "xxx/client/DataServiceBase.java", "xxx/client/DataService.java",
                                                  "xxx/client/DataServiceAsync.java");
  }

  public void testAsyncMethodsInSuperInterface() {
    myCodeInsightFixture.enableInspections(GwtInconsistentAsyncInterfaceInspection.class);
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true, "xxx/client/SampleService.java", "xxx/client/SampleServiceAsyncBase.java",
                                                  "xxx/client/SampleServiceAsync.java");
  }

  public void testGenericServiceInconsistentTypes() {
    myCodeInsightFixture.enableInspections(GwtInconsistentAsyncInterfaceInspection.class);
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.testHighlightingAllFiles(true, false, true, "xxx/client/GenericService.java", "xxx/client/GenericServiceInterface.java",
                                                  "xxx/client/GenericServiceAsync.java");
  }

  public void testServiceMethodNotUnused() {
    myCodeInsightFixture.enableInspections(new UnusedDeclarationInspectionBase(true));
    allowTreeAccessToGwtJreEmulationLibrary();
    myCodeInsightFixture.configureByFiles("xxx/client/MyService.java", "xxx/client/MyApp.java",
                                          "xxx/client/MyServiceAsync.java", "xxx/server/MyServiceImpl.java");
    myCodeInsightFixture.checkHighlighting();
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/rpc";
  }
}
