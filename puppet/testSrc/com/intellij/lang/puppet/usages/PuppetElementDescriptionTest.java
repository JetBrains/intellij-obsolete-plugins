package com.intellij.lang.puppet.usages;

import com.intellij.ide.util.DeleteNameDescriptionLocation;
import com.intellij.ide.util.DeleteTypeDescriptionLocation;
import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.NonCodeSearchDescriptionLocation;
import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.TestFrameworkUtil;
import com.intellij.usageView.UsageViewLongNameLocation;
import com.intellij.usageView.UsageViewNodeTextLocation;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import com.intellij.util.ThrowableRunnable;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class PuppetElementDescriptionTest extends PuppetTestCase {
  public void testType() {
    source("define blah::some::ty<caret>pe{}")
      .expectUsageShortName("blah::some::type")
      .expectUsageLongName("resource type definition 'blah::some::type'")
      .expectUsageType("resource type definition")
      .expectUsageNodeText("blah::some::type")
      .expectNonCodeLocation("type")
      .expectDeleteName("blah::some::type")
      .expectDeleteTypeSingular("resource type definition")
      .expectDeleteTypePlural("resource type definitions")
      .assertAll();
  }

  private void doTestNamespace(@NotNull String source) {
    source(source)
      .expectUsageShortName("blah::some")
      .expectUsageLongName("namespace 'blah::some'")
      .expectUsageType("namespace")
      .expectUsageNodeText("blah::some")
      .expectNonCodeLocation("some")
      .expectDeleteName("blah::some")
      .expectDeleteTypeSingular("namespace")
      .expectDeleteTypePlural("namespaces")
      .assertAll();
  }

  public void testNamespaceInType() {
    doTestNamespace("define blah::so<caret>me::type{}");
  }

  public void testClass() {
    source("class blah::some::ty<caret>pe{}")
      .expectUsageShortName("blah::some::type")
      .expectUsageLongName("class definition 'blah::some::type'")
      .expectUsageType("class definition")
      .expectUsageNodeText("blah::some::type")
      .expectNonCodeLocation("type")
      .expectDeleteName("blah::some::type")
      .expectDeleteTypeSingular("class definition")
      .expectDeleteTypePlural("class definitions")
      .assertAll();
  }

  public void testResourceInstance() {
    doTestResourceInstance("file{ some<caret>file: }");
  }

  private void doTestResourceInstance(@NotNull String source)
  {
    source(source)
      .expectUsageShortName("somefile")
      .expectUsageLongName("resource instance 'somefile'")
      .expectUsageType("resource instance")
      .expectUsageNodeText("somefile")
      .expectNonCodeLocation("somefile")
      .expectDeleteName("somefile")
      .expectDeleteTypeSingular("resource instance")
      .expectDeleteTypePlural("resource instances")
      .assertAll();
  }

  public void testResourceInstanceMultideclaration() {
    doTestResourceInstance("file{ [some<caret>file, otherfile]: }");
  }

  public void testNamespaceInClass() {
    doTestNamespace("class blah::so<caret>me::type{}");
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testFunction() {
    source("function blah::some::ty<caret>pe{}")
      .expectUsageShortName("blah::some::type")
      .expectUsageLongName("function definition 'blah::some::type'")
      .expectUsageType("function definition")
      .expectUsageNodeText("blah::some::type")
      .expectNonCodeLocation("type")
      .expectDeleteName("blah::some::type")
      .expectDeleteTypeSingular("function definition")
      .expectDeleteTypePlural("function definitions")
      .assertAll();
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testNamespaceInFunction() {
    doTestNamespace("function blah::so<caret>me::type{}");
  }

  public void testVariable() {
    source("$some<caret>var = 1")
      .expectUsageShortName("::somevar")
      .expectUsageLongName("variable '::somevar'")
      .expectUsageType("variable")
      .expectUsageNodeText("::somevar")
      .expectNonCodeLocation("somevar")
      .expectDeleteName("::somevar")
      .expectDeleteTypeSingular("variable")
      .expectDeleteTypePlural("variables")
      .assertAll();
  }

  public void testVariableInClass() {
    source("class someclass{ $some<caret>var = 1}")
      .expectUsageShortName("someclass::somevar")
      .expectUsageLongName("variable 'someclass::somevar'")
      .expectUsageType("variable")
      .expectUsageNodeText("someclass::somevar")
      .expectNonCodeLocation("somevar")
      .expectDeleteName("someclass::somevar")
      .expectDeleteTypeSingular("variable")
      .expectDeleteTypePlural("variables")
      .assertAll();
  }

  public void testVariableInNode() {
    source("node somenode{ $some<caret>var = 1}")
      .expectUsageShortName("::somevar")
      .expectUsageLongName("variable '::somevar'")
      .expectUsageType("variable")
      .expectUsageNodeText("::somevar")
      .expectNonCodeLocation("somevar")
      .expectDeleteName("::somevar")
      .expectDeleteTypeSingular("variable")
      .expectDeleteTypePlural("variables")
      .assertAll();
  }

  public void testClassParam() {
    source("class someclass($some<caret>var = 1){}")
      .expectUsageShortName("someclass::somevar")
      .expectUsageLongName("parameter 'someclass::somevar'")
      .expectUsageType("parameter")
      .expectUsageNodeText("someclass::somevar")
      .expectNonCodeLocation("somevar")
      .expectDeleteName("someclass::somevar")
      .expectDeleteTypeSingular("parameter")
      .expectDeleteTypePlural("parameters")
      .assertAll();
  }

  public void testTypeParam() {
    source("define sometype($some<caret>var = 1){}")
      .expectUsageShortName("somevar")
      .expectUsageLongName("parameter 'somevar'")
      .expectUsageType("parameter")
      .expectUsageNodeText("somevar")
      .expectNonCodeLocation("somevar")
      .expectDeleteName("somevar")
      .expectDeleteTypeSingular("parameter")
      .expectDeleteTypePlural("parameters")
      .assertAll();
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testFunctionParam() {
    source("function somefunc($some<caret>var = 1){}")
      .expectUsageShortName("somevar")
      .expectUsageLongName("parameter 'somevar'")
      .expectUsageType("parameter")
      .expectUsageNodeText("somevar")
      .expectNonCodeLocation("somevar")
      .expectDeleteName("somevar")
      .expectDeleteTypeSingular("parameter")
      .expectDeleteTypePlural("parameters")
      .assertAll();
  }

  private Subject source(@NotNull String source) {
    return new Subject(source);
  }

  private class Subject {
    private final PsiElement myElement;
    private final List<ThrowableRunnable<?>> myRunnables = new ArrayList<>();

    Subject(String source) {
      myFixture.configureByText(PuppetFileType.INSTANCE, source);
      myElement = myFixture.getElementAtCaret();
      assertNotNull(myElement);
    }

    public @NotNull Subject expectUsageShortName(@Nullable String expected) {
      assertDescriptionIs(UsageViewShortNameLocation.INSTANCE, expected);
      return this;
    }

    public @NotNull Subject expectUsageLongName(@Nullable String expected) {
      assertDescriptionIs(UsageViewLongNameLocation.INSTANCE, expected);
      return this;
    }

    public @NotNull Subject expectUsageType(@Nullable String expected) {
      assertDescriptionIs(UsageViewTypeLocation.INSTANCE, expected);
      return this;
    }

    public @NotNull Subject expectUsageNodeText(@Nullable String expected) {
      assertDescriptionIs(UsageViewNodeTextLocation.INSTANCE, expected);
      return this;
    }

    public @NotNull Subject expectNonCodeLocation(@Nullable String expected) {
      assertDescriptionIs(NonCodeSearchDescriptionLocation.STRINGS_AND_COMMENTS, expected);
      return this;
    }

    public @NotNull Subject expectDeleteName(@Nullable String expected) {
      assertDescriptionIs(DeleteNameDescriptionLocation.INSTANCE, expected);
      return this;
    }

    public @NotNull Subject expectDeleteTypeSingular(@Nullable String expected) {
      assertDescriptionIs(DeleteTypeDescriptionLocation.SINGULAR, expected);
      return this;
    }

    public @NotNull Subject expectDeleteTypePlural(@Nullable String expected) {
      assertDescriptionIs(DeleteTypeDescriptionLocation.PLURAL, expected);
      return this;
    }

    private void assertDescriptionIs(@NotNull ElementDescriptionLocation location, @Nullable String expected) {
      myRunnables.add(() -> {
        String actual = ElementDescriptionUtil.getElementDescription(myElement, location);
        if (!StringUtil.equals(expected, actual)) {
          fail(
            MessageFormat.format("{0} at {1}: <{2}> while expecting <{3}>", myElement, location.getClass().getSimpleName(), actual, expected)
          );
        }
      });
    }

    public void assertAll() {
      new  RunAll(myRunnables).run();
    }
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetElementDescriptionTest.class));
  }
}
