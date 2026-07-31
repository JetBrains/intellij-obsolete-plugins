package com.intellij.gwt.references;

import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.TestDataPath;

import java.util.List;

@GwtTestOptions(version = GwtVersionImpl.VERSION_2_7)
@TestDataPath("$CONTENT_ROOT/../testData/references/uiRenderer/")
public class UiRendererXmlFileCorrespondenceInUiBinderMappingServiceTest extends GwtCodeInsightTestCase {

  private UiBinderMappingService myMappingService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myMappingService = UiBinderMappingService.getInstance(myCodeInsightFixture.getModule());
  }

  @Override
  protected void tearDown() throws Exception {
    myMappingService = null;
    super.tearDown();
  }

  public void testSimple() {
    runTest("ppp.client.uiTemplate.MyComponent", "MyComponent.ui.xml");
  }

  public void testAnnotationValueWithShortUiTemplate() {
    runTest("ppp.client.uiTemplate.MyComponentShort", "MyComponent.ui.xml");
  }

  public void testAnnotationValueWithFullUiTemplate() {
    runTest("ppp.client.uiTemplate.MyComponentFull", "MyComponent.ui.xml");
  }

  public void testOuterAnnotationValueWithShortUiTemplate() {
    runTest("ppp.client.uiTemplate.MyComponentOuterShort", "MyComponent.ui.xml");
  }

  public void testOuterAnnotationValueWithFullUiTemplate() {
    runTest("ppp.client.uiTemplate.MyComponentOuterFull", "MyComponent.ui.xml");
  }

  public void testNested() {
    runTest("ppp/client/uiTemplate/MyComponentNested.java",
            "ppp.client.uiTemplate.MyComponentNested.MyComponent", "MyComponent.ui.xml");
  }

  public void testAnnotationValueWithShortDollarUiTemplate() {
    runTest("ppp.client.uiTemplate.MyComponentAnotherShort", "MyComponent.Another.ui.xml");
  }

  public void testAnnotationValueWithFullDollarUiTemplate() {
    runTest("ppp.client.uiTemplate.MyComponentAnotherFull", "MyComponent.Another.ui.xml");
  }

  public void testWithoutRenderer() {
    runTest("ppp.client.uiTemplate.MyComponentWithoutRenderer", "");
  }

  public void testWithSubDirectoryUiTemplate() {
    runTest("ppp.client.uiTemplate.MyComponentSub", "sub/MyComponentSub.ui.xml");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/uiRenderer";
  }

  private void runTest(String className, String xmlFileName) {
    runTest(className.replace('.', '/') + ".java", className, xmlFileName);
  }

  private void runTest(String fileName, String className, String xmlFileName) {
    myCodeInsightFixture.configureByFiles(fileName);

    PsiClass myComponentClass = findPsiClass(className);
    List<XmlFile> xmlFiles = myMappingService.getUiXmlFiles(myComponentClass);

    if (StringUtil.isNotEmpty(xmlFileName)) {
      assertEquals("Cannot find unique *.ui.xml file", 1, xmlFiles.size());

      XmlFile xmlFile = xmlFiles.get(0);
      assertEquals("Found file doesn't match the expected one", StringUtil.getShortName(xmlFileName, '/'), xmlFile.getName());
    } else {
      assertEquals("Unexpected *.ui.xml file found", 0, xmlFiles.size());
    }
  }

  private PsiClass findPsiClass(String className) {
    Module module = myCodeInsightFixture.getModule();
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(module.getProject());
    GlobalSearchScope searchScope = GlobalSearchScope.moduleScope(module);
    return psiFacade.findClass(className, searchScope);
  }
}
