package com.intellij.gwt.jsni;

import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/jsni/completion/injected/")
public class CompletionInInjectedTest extends GwtCodeInsightTestCase {
  public void testMethodOfWndParameter() {
    myCodeInsightFixture.testCompletionVariants("MethodOfWndParameter.java", "alert");
  }

  public void testSyntheticParameter() {
    myCodeInsightFixture.testCompletionVariants("SyntheticParameter.java", "$doc", "$entry", "$wnd");
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_7)
  public void testShortJavaClassReference() {
    myCodeInsightFixture.testCompletion("pack/JavaClassReference.java", "pack/JavaClassReference_after_short.java");
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_6)
  public void testShortenClassReference() {
    //it would be better to add import statement when class name is completed in JSNI method, but it isn't simple to implement
    String[] before = {"pack/NotImportedClassReference.java", "notImported/ClassFromAnotherPackage.java"};
    myCodeInsightFixture.testCompletion(before, "pack/NotImportedClassReference_after.java");
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_5)
  public void testQualifiedJavaClassReference() {
    myCodeInsightFixture.testCompletion("pack/JavaClassReference.java", "pack/JavaClassReference_after_qualified.java");
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_6)
  public void testMethodReferenceForOverloaded() {
    myCodeInsightFixture.testCompletionVariants("MethodReferenceForOverloaded.java", "m(I)", "m(II)");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "jsni/completion/injected";
  }
}
