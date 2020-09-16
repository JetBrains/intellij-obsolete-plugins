package com.intellij.javascript.testFramework;

import com.google.jstestdriver.idea.assertFramework.JstdTestMethodNameRefiner;
import com.google.jstestdriver.idea.assertFramework.jstd.JstdTestFileStructure;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestFileStructurePack {

  private final List<AbstractTestFileStructure> myTestFileStructures;

  public TestFileStructurePack(@NotNull List<AbstractTestFileStructure> testFileStructures) {
    myTestFileStructures = testFileStructures;
  }

  public boolean isEmpty() {
    for (AbstractTestFileStructure structure : myTestFileStructures) {
      if (!structure.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  public JstdRunElement getJstdRunElement(@NotNull PsiElement psiElement) {
    TextRange textRange = psiElement.getTextRange();
    for (AbstractTestFileStructure testFileStructure : myTestFileStructures) {
      if (testFileStructure instanceof JstdTestFileStructure) {
        JstdRunElement jstdRunElement = ((JstdTestFileStructure)testFileStructure).findJstdRunElement1(textRange);
        if (jstdRunElement != null) {
          return jstdRunElement;
        }
      }
    }
    return null;
  }

  @Nullable
  public PsiElement findPsiElement(@NotNull String testCaseName,
                                   @Nullable String testMethodName,
                                   @Nullable JstdTestMethodNameRefiner testMethodNameRefiner) {
    for (AbstractTestFileStructure testFileStructure : myTestFileStructures) {
      String refinedTestMethodName = testMethodName;
      if (testMethodNameRefiner != null && testMethodName != null) {
        refinedTestMethodName = testMethodNameRefiner.refine(testFileStructure, testMethodName);
      }
      PsiElement element = testFileStructure.findPsiElement(testCaseName, refinedTestMethodName);
      if (element != null) {
        return element;
      }
    }
    return null;
  }

  public boolean contains(@NotNull final String testCaseName,
                          @Nullable final String testMethodName,
                          @Nullable JstdTestMethodNameRefiner testMethodNameRefiner) {
    for (AbstractTestFileStructure testFileStructure : myTestFileStructures) {
      String refinedTestMethodName = testMethodName;
      if (testMethodNameRefiner != null && testMethodName != null) {
        refinedTestMethodName = testMethodNameRefiner.refine(testFileStructure, testMethodName);
      }
      boolean ok = testFileStructure.contains(testCaseName, refinedTestMethodName);
      if (ok) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public List<String> getTopLevelElements() {
    return Collections.emptyList();
  }

  @NotNull
  public List<String> getChildrenOf(String topLevelElementName) {
    List<String> out = new ArrayList<>();
    for (AbstractTestFileStructure structure : myTestFileStructures) {
      List<String> localChildren = structure.getChildrenOf(topLevelElementName);
      out.addAll(localChildren);
    }
    return out;
  }
}
