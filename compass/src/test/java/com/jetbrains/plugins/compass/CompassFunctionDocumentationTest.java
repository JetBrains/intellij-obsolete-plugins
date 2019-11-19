package com.jetbrains.plugins.compass;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.plugins.sass.SASSFileType;
import org.jetbrains.plugins.sass.extensions.SassExtensionFunctionInfo;
import org.jetbrains.plugins.sass.extensions.SassExtensionFunctionInfoImpl;

import java.io.File;

public class CompassFunctionDocumentationTest extends CompassTestCase {
  public void testExtensionFunction() throws Exception {
   doTest(true);
  }

  public void testCustomFunction() throws Exception {
    doTest(true);
  }

  public void testFunctionDefinedInSassFile() throws Exception {
    doTest(false);
  }

  private void doTest(boolean testLookup) throws Exception {
    String fullFileName = myFixture.getTestDataPath() + File.separator + getTestName(true) + ".txt";
    String[] content = FileUtil.loadFile(new File(fullFileName)).split("---");
    String fileContent = content[0].trim();
    String expectedDoc = content.length > 1 ? content[1].trim() : null;

    myFixture.configureByText(SASSFileType.SASS, fileContent);
    PsiElement originalElement = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
    PsiElement element = DocumentationManager.getInstance(myFixture.getProject()).findTargetElement(myFixture.getEditor(), myFixture.getFile());
    DocumentationProvider documentationProvider = DocumentationManager.getProviderFromElement(originalElement);

    assertEquals(expectedDoc, documentationProvider.generateDoc(element, originalElement));

    if (testLookup) {
      assert element instanceof PsiNamedElement;
      final String name = ((PsiNamedElement)element).getName();
      doTestLookupElement(new SassExtensionFunctionInfoImpl(StringUtil.notNullize(name), "", StringUtil.notNullize(expectedDoc), "", null),
                          element, expectedDoc, documentationProvider);
    }
  }

  private void doTestLookupElement(SassExtensionFunctionInfo lookupObject, PsiElement context, String expectedDoc, DocumentationProvider documentationProvider) {
    PsiElement docElement = documentationProvider.getDocumentationElementForLookupItem(myFixture.getPsiManager(), lookupObject, context);
    String inlineDoc = documentationProvider.generateDoc(docElement, context);
    assertEquals(expectedDoc, inlineDoc);
  }

  @Override
  protected String getTestDataSubdir() {
    return "documentation";
  }
}
