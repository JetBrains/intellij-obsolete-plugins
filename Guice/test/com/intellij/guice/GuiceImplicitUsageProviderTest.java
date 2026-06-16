package com.intellij.guice;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class GuiceImplicitUsageProviderTest extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addClass("""
      package com.google.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
      public @interface Inject {}
      """);
    myFixture.addClass("""
      package com.google.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.METHOD)
      public @interface Provides {}
      """);
    myFixture.addClass("""
      package javax.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
      public @interface Inject {}
      """);
    myFixture.addClass("""
      package jakarta.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
      public @interface Inject {}
      """);
    myFixture.addClass("""
      package com.google.inject.throwingproviders;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
      public @interface ThrowingInject {}
      """);
    myFixture.addClass("""
      package com.google.inject.throwingproviders;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.METHOD)
      public @interface CheckedProvides {}
      """);
  }

  // -------------------------------------------------------------------------
  // Helper template functions to generate mock classes/fields/methods
  // -------------------------------------------------------------------------

  private PsiField createJavaInjectField(String annotationClass, String fieldName) {
    String simpleName = annotationClass.substring(annotationClass.lastIndexOf('.') + 1);
    PsiClass aClass = myFixture.addClass(String.format("""
      import %s;
      class MyClass {
        @%s private String %s;
      }
      """, annotationClass, simpleName, fieldName));
    PsiField field = aClass.findFieldByName(fieldName, false);
    assertNotNull(field);
    return field;
  }

  private PsiField createKotlinInjectField(String annotationClass, String fieldName) {
    String simpleName = annotationClass.substring(annotationClass.lastIndexOf('.') + 1);
    myFixture.configureByText("MyKotlinClass.kt", String.format("""
      import %s
      class MyKotlinClass {
        @field:%s private val %s: String = ""
      }
      """, annotationClass, simpleName, fieldName));
    PsiClass aClass = JavaPsiFacade.getInstance(getProject()).findClass("MyKotlinClass", GlobalSearchScope.projectScope(getProject()));
    assertNotNull(aClass);
    PsiField field = aClass.findFieldByName("myField", false);
    assertNotNull(field);
    return field;
  }

  private PsiMethod createJavaProvidesMethod(String annotationClass, String methodName) {
    String simpleName = annotationClass.substring(annotationClass.lastIndexOf('.') + 1);
    PsiClass aClass = myFixture.addClass(String.format("""
      import %s;
      class MyClass {
        @%s private String %s() { return ""; }
      }
      """, annotationClass, simpleName, methodName));
    PsiMethod[] methods = aClass.findMethodsByName(methodName, false);
    assertTrue(methods.length > 0);
    PsiMethod method = methods[0];
    assertNotNull(method);
    return method;
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  public void testInjectFieldIsImplicitUsage() {
    PsiField field = createJavaInjectField("com.google.inject.Inject", "myField");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(field));
    assertTrue(provider.isImplicitRead(field));
    assertTrue(provider.isImplicitWrite(field));
  }

  public void testProvidesMethodIsImplicitUsage() {
    PsiMethod method = createJavaProvidesMethod("com.google.inject.Provides", "provideString");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(method));
    assertTrue(provider.isImplicitRead(method));
    assertFalse(provider.isImplicitWrite(method));
  }

  public void testKotlinInjectFieldIsImplicitUsage() {
    PsiField field = createKotlinInjectField("com.google.inject.Inject", "myField");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(field));
    assertTrue(provider.isImplicitRead(field));
    assertTrue(provider.isImplicitWrite(field));
  }

  public void testJavaxInjectFieldIsImplicitUsage() {
    PsiField field = createJavaInjectField("javax.inject.Inject", "myField");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(field));
    assertTrue(provider.isImplicitRead(field));
    assertTrue(provider.isImplicitWrite(field));
  }

  public void testJakartaInjectFieldIsImplicitUsage() {
    PsiField field = createJavaInjectField("jakarta.inject.Inject", "myField");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(field));
    assertTrue(provider.isImplicitRead(field));
    assertTrue(provider.isImplicitWrite(field));
  }

  public void testThrowingInjectFieldIsImplicitUsage() {
    PsiField field = createJavaInjectField("com.google.inject.throwingproviders.ThrowingInject", "myField");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(field));
    assertTrue(provider.isImplicitRead(field));
    assertTrue(provider.isImplicitWrite(field));
  }

  public void testCheckedProvidesMethodIsImplicitUsage() {
    PsiMethod method = createJavaProvidesMethod("com.google.inject.throwingproviders.CheckedProvides", "provideString");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(method));
    assertTrue(provider.isImplicitRead(method));
    assertFalse(provider.isImplicitWrite(method));
  }

  public void testKotlinJavaxInjectFieldIsImplicitUsage() {
    PsiField field = createKotlinInjectField("javax.inject.Inject", "myField");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(field));
    assertTrue(provider.isImplicitRead(field));
    assertTrue(provider.isImplicitWrite(field));
  }

  public void testKotlinJakartaInjectFieldIsImplicitUsage() {
    PsiField field = createKotlinInjectField("jakarta.inject.Inject", "myField");
    GuiceImplicitUsageProvider provider = new GuiceImplicitUsageProvider();
    assertTrue(provider.isImplicitUsage(field));
    assertTrue(provider.isImplicitRead(field));
    assertTrue(provider.isImplicitWrite(field));
  }
}
