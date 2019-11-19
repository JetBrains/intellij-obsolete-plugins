package com.jetbrains.plugins.compass.ruby;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.sass.SASSFileType;
import org.junit.Test;

import java.io.File;

public class CompassFunctionExtensionsDocumentationTest extends SassExtensionsBaseTest {
  @Test
  public void testCompassFunction() throws Exception {
    doTest();
  }

  @Test
  public void testCustomCompassFunction() throws Exception {
    doTest();
  }

  private void doTest() throws Exception {
    String fullFileName = myFixture.getTestDataPath() + File.separator + getTestName(true) + ".txt";
    String[] content = FileUtil.loadFile(new File(fullFileName)).split("---");
    String fileContent = content[0].trim();
    assertTrue(content.length > 1);
    String expectedDoc = content[1].trim();

    myFixture.configureByText(SASSFileType.SASS, fileContent);
    final PsiFile file = myFixture.getFile();
    PsiElement originalElement = file.findElementAt(myFixture.getEditor().getCaretModel().getOffset());
    PsiElement element = DocumentationManager.getInstance(myFixture.getProject()).findTargetElement(myFixture.getEditor(), file);
    DocumentationProvider documentationProvider = DocumentationManager.getProviderFromElement(originalElement);

    final String actualDoc = documentationProvider.generateDoc(element, originalElement);
    assertNotNull(actualDoc);
    assertTrue("'" + actualDoc + "' doesn't contain '" + expectedDoc + "'", actualDoc.contains(expectedDoc));

    assert element instanceof PsiNamedElement;
    final String name = ((PsiNamedElement)element).getName();
    assert name != null;

    final LookupElement[] elements = myFixture.completeBasic();
    final LookupElement lookup = ContainerUtil.find(elements, element1 -> name.equals(element1.getLookupString()));
    assertNotNull(lookup);
    doTestLookupElement(lookup.getObject(), element, expectedDoc, documentationProvider);
  }

  private void doTestLookupElement(Object lookupObject, PsiElement context, String expectedDoc, DocumentationProvider documentationProvider) {
    PsiElement docElement = documentationProvider.getDocumentationElementForLookupItem(myFixture.getPsiManager(), lookupObject, context);
    String actualDoc = documentationProvider.generateDoc(docElement, context);
    assertNotNull(actualDoc);
    assertTrue("'" + actualDoc + "' doesn't contain '" + expectedDoc + "'", actualDoc.contains(expectedDoc));
  }

  @NotNull
  @Override
  protected String getTestDataRelativePath() {
    return "documentation";
  }
}
