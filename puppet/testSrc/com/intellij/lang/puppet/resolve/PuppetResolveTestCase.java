package com.intellij.lang.puppet.resolve;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetResolveTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.NullableConsumer;
import junit.framework.AssertionFailedError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.ALL;

public abstract class PuppetResolveTestCase extends PuppetTestCase {
  private final List<NullableConsumer<PsiPolyVariantReference>> myReferenceCheckers = new ArrayList<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    clearReferenceCheckers();
    setUpLibraries();
  }

  protected void clearReferenceCheckers() {
    myReferenceCheckers.clear();
  }

  protected void addReferenceChecker(@Nullable NullableConsumer<PsiPolyVariantReference> consumer) {
    myReferenceCheckers.add(consumer);
  }

  @Override
  protected String getBasePath() {
    return "resolve";
  }

  protected void doTest() {
    doTest(ALL);
  }

  protected void doTest(TestType testType) {
    doTest(testType, ResultType.SERIALIZE, PsiPolyVariantReference.class);
  }

  protected void doTest(TestType testType, ResultType resultType, Class<? extends PsiPolyVariantReference> referenceClassToTest) {
    doTest(testType, resultType, referenceClassToTest, getTestFileName(), getAnswersFileName());
  }

  protected String getTestFileName() {
    String testName = getTestName(true);
    return testName + ".code";
  }

  protected String getAnswersFileName() {
    return getAnswersFileName(getTestName(true));
  }

  protected String getAnswersFileName(@NotNull String baseName) {
    return "answers/" + baseName + ".txt";
  }

  protected void doTest(@NotNull TestType testType,
                        @NotNull ResultType resultType,
                        @NotNull Class<? extends PsiPolyVariantReference> referenceClassToTest,
                        @NotNull String testFileName,
                        @Nullable String testAnswerFileName) {
    if (testType == TestType.CARET) {
      PsiReference reference = getReferenceFromCaret(testFileName, referenceClassToTest);
      ResolveResult[] resolveResults = ((PsiPolyVariantReference)reference).multiResolve(false);
      switch (resultType) {
        case ZERO -> assertEquals(0, resolveResults.length);
        case ONE -> assertEquals(1, resolveResults.length);
        case SERIALIZE -> {
          String resolveData = serializeResolveResults(reference, resolveResults);
          assertSameLinesWithFile(getTestDataPath() + "/" + testAnswerFileName, resolveData);
        }
      }
    }
    else {
      assertEquals(ResultType.SERIALIZE, resultType);
      configureByManifest(testFileName);
      compareSerializedReferencesWithEditor(referenceClassToTest, testAnswerFileName);
    }
  }

  protected void compareSerializedReferencesWithEditor(@NotNull String answersFileName) {
    compareSerializedReferencesWithEditor(PsiPolyVariantReference.class, answersFileName);
  }

  protected void compareSerializedReferencesWithEditor(@NotNull Class<? extends PsiPolyVariantReference> referenceClassToTest,
                                                       @Nullable String testAnswerFileName) {
    assertSameLinesWithFile(getTestDataPath() + "/" + testAnswerFileName, getSerializedResolveResultsFromAll(referenceClassToTest));
  }

  private String getSerializedResolveResultsFromAll(Class<? extends PsiPolyVariantReference> referenceClassToTest) {
    final List<PsiPolyVariantReference> referencesList =
      PuppetResolveTestUtil.getReferencesInFile(myFixture.getFile(), referenceClassToTest);

    StringBuilder sb = new StringBuilder();
    for (PsiPolyVariantReference reference : referencesList) {
      for (NullableConsumer<PsiPolyVariantReference> checker : myReferenceCheckers) {
        checker.consume(reference);
      }

      ResolveResult[] results = reference.multiResolve(false);
      sb.append(serializeResolveResults(reference, results)).append("\n");
    }
    return sb.toString();
  }

  // fixme suppose there is a default function for this
  private PsiReference getReferenceFromCaret(String testFileName, Class<? extends PsiPolyVariantReference> referenceClassToTest) {
    final PsiReference reference = PuppetResolveTestUtil.getReferenceOfType(getReferenceAtCaretPosition(testFileName),
                                                                            referenceClassToTest);
    assertNotNull(reference);
    assertInstanceOf(reference, referenceClassToTest);

    return reference;
  }

  private PsiReference getReferenceAtCaretPosition(@NotNull String testFileName) {
    configureByManifest(testFileName);
    return myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
  }

  private static String serializeResolveResults(PsiReference reference, ResolveResult[] results) {
    StringBuilder sb = new StringBuilder();
    PsiElement sourceElement = reference.getElement();
    TextRange referenceRange = reference.getRangeInElement();
    String sourceElementText = sourceElement.getText();
    int sourceElementOffset = sourceElement.getNode().getStartOffset();

    sb
      .append("'")
      .append(referenceRange.subSequence(sourceElementText))
      .append("'")
      .append(" at ")
      .append(referenceRange.shiftRight(sourceElementOffset))
      .append(" => ")
      .append(results.length)
      .append(" results:")
      .append('\n');

    for (ResolveResult result : results) {
      if (!result.isValidResult()) {
        throw new AssertionFailedError("Invalid resolve result");
      }

      PsiElement targetElement = result.getElement();
      assertNotNull(targetElement);

      sb.append('\t');

      ASTNode targetElementNode = targetElement.getNode();

      if (targetElementNode == null) {
        sb.append(FileUtil.toSystemIndependentName(targetElement.toString()));
      }
      else {
        sb.append(PsiUtilCore.getElementType(targetElementNode))
          .append(" at ")
          .append(targetElementNode.getStartOffset())
          .append(" in ")
          .append(targetElement.getContainingFile().getName())
        ;
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  protected enum TestType {CARET, ALL}

  protected enum ResultType {ZERO, ONE, SERIALIZE}
}
